package org.papiricoh.supernaturalcraft.arena;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * One boss arena ("the Cage"): a cylinder around the summoning altar.
 *
 * <p><b>Terrain is copy-on-write.</b> Every block the fight changes goes through {@link #mutate},
 * which remembers the original the first time a position is touched. Restoration puts originals
 * back only where the arena's own block is still in place, so anything a player built afterwards
 * survives. Block entities and {@code #arena_immune} blocks are never touched. Changes use flag 2
 * only (no neighbour updates), so a torch on a replaced block doesn't pop off.
 */
public class ArenaController {

    public enum Status { ACTIVE, RESTORING, CLOSED }

    /** Default vertical bounds; a theme may ask for more ({@link ArenaTheme}). */
    public static final int DEPTH = 8, HEIGHT = 24, EDGE_MARGIN = 3, RESTORE_BATCH = 256;
    private static final int SET_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private final UUID id;
    private final BlockPos center;
    private final int radius;
    private @Nullable UUID bossId;
    private final Set<UUID> participants = new HashSet<>();
    private final Map<BlockPos, BlockState> originals = new LinkedHashMap<>();
    private final Map<BlockPos, BlockState> placed = new LinkedHashMap<>();
    private final Map<BlockPos, Long> timedReverts = new LinkedHashMap<>();
    private final LongSet forcedChunks = new LongOpenHashSet();
    /** Positions the fight must never change (bell pedestals, the columns that give shade). */
    private final LongSet protectedBlocks = new LongOpenHashSet();
    private Status status = Status.ACTIVE;
    private int phase = 1;
    /** Which fight the arena holds; see {@link ArenaTheme}. */
    private int theme;
    public static final int CAGE = ArenaTheme.CAGE, DARKNESS = ArenaTheme.DARKNESS, CHORUS = ArenaTheme.CHORUS;
    /** How far from the centre the floor still reaches; shrinks as a breaking platform falls away. */
    private int floorRadius = -1;
    /** The fight holds the weather in a storm, which must be let go however the arena closes. */
    private boolean forcedStorm;
    private long emptySince = -1;
    private boolean victory;
    /** What the summoning put up for this fight (v0.19), handed back unless it is won. */
    private @Nullable Stake stake;
    /** Whether its end has been settled this session: the stake, and what it held open in the world (not saved: idempotent). */
    private boolean settled;

    public ArenaController(UUID id, BlockPos center, int radius) {
        this.id = id;
        this.center = center;
        this.radius = radius;
    }

    public UUID id() {
        return id;
    }

    public BlockPos center() {
        return center;
    }

    public int radius() {
        return radius;
    }

    public Status status() {
        return status;
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public int phase() {
        return phase;
    }

    public void setPhase(int phase) {
        this.phase = phase;
    }

    public int theme() {
        return theme;
    }

    public void setTheme(int theme) {
        this.theme = theme;
    }

    public int depth() {
        return ArenaTheme.depth(theme);
    }

    public int height() {
        return ArenaTheme.height(theme);
    }

    public int floorRadius() {
        return floorRadius < 0 ? radius : floorRadius;
    }

    public void setFloorRadius(int floorRadius) {
        this.floorRadius = floorRadius;
    }

    public boolean forcedStorm() {
        return forcedStorm;
    }

    public void setForcedStorm(boolean forcedStorm) {
        this.forcedStorm = forcedStorm;
    }

    /** Marks a position the fight may never change, even though it is not {@code #arena_immune}. */
    public void protect(BlockPos pos) {
        protectedBlocks.add(pos.asLong());
    }

    public boolean isProtected(BlockPos pos) {
        return protectedBlocks.contains(pos.asLong());
    }

    public @Nullable UUID bossId() {
        return bossId;
    }

    public void setBoss(UUID bossId) {
        this.bossId = bossId;
    }

    public Set<UUID> participants() {
        return participants;
    }

    public @Nullable Stake stake() {
        return stake;
    }

    public void setStake(@Nullable Stake stake) {
        this.stake = stake;
    }

    /** The stake, taken (it is settled once). */
    @Nullable
    Stake takeStake() {
        Stake s = stake;
        stake = null;
        return s;
    }

    /** The stake is only shown to the boss, not given: it comes back after a victory too. */
    public void lendStake() {
        if (stake != null) stake = stake.lent();
    }

    public boolean victory() {
        return victory;
    }

    // --- geometry -------------------------------------------------------------------------

    public double horizontalDistance(Vec3 p) {
        double dx = p.x - (center.getX() + 0.5), dz = p.z - (center.getZ() + 0.5);
        return Math.sqrt(dx * dx + dz * dz);
    }

    public boolean contains(Vec3 p) {
        return horizontalDistance(p) <= radius && p.y >= center.getY() - depth() && p.y <= center.getY() + height();
    }

    public boolean contains(BlockPos pos) {
        return contains(Vec3.atCenterOf(pos));
    }

    public Vec3 centerVec() {
        return Vec3.atBottomCenterOf(center);
    }

    // --- terrain --------------------------------------------------------------------------

    /**
     * Changes a block for the fight. {@code revertAfter} &gt; 0 schedules it to go back on its own
     * after that many ticks (ice cages, pillars); otherwise it stays until the arena closes.
     *
     * @return false if the position is protected, outside the arena or the snapshot is full
     */
    public boolean mutate(ServerLevel level, BlockPos pos, BlockState state, int revertAfter) {
        if (status != Status.ACTIVE || !contains(pos) || isProtected(pos)) return false;
        pos = pos.immutable();
        BlockState current = level.getBlockState(pos);
        if (level.getBlockEntity(pos) != null || current.is(AllTags.Blocks.ARENA_IMMUNE)) return false;
        if (!originals.containsKey(pos)) {
            if (originals.size() >= Math.max(SNConfig.MAX_SNAPSHOT.get(), ArenaTheme.minSnapshot(theme))) return false;
            originals.put(pos, current);
        }
        level.setBlock(pos, state, SET_FLAGS);
        placed.put(pos, state);
        if (revertAfter > 0) timedReverts.put(pos, level.getGameTime() + revertAfter);
        return true;
    }

    /** Puts one position back the way it was, if the arena's block is still the one there. */
    public void revert(ServerLevel level, BlockPos pos) {
        BlockState original = originals.get(pos);
        BlockState mine = placed.get(pos);
        if (original == null) return;
        BlockState now = level.getBlockState(pos);
        if (now == mine || now.isAir()) {
            level.setBlock(pos, original, SET_FLAGS);
        }
        originals.remove(pos);
        placed.remove(pos);
        timedReverts.remove(pos);
    }

    /**
     * Puts back up to {@code max} of the blocks it changed, oldest first, without copying the rest (v0.19: a 90 000-block
     * snapshot restores in linear time).
     *
     * @return how many were put back
     */
    public int revertSome(ServerLevel level, int max) {
        BlockPos[] batch = new BlockPos[Math.min(max, originals.size())];
        Iterator<BlockPos> it = originals.keySet().iterator();
        int n = 0;
        while (n < batch.length && it.hasNext()) batch[n++] = it.next();
        for (int i = 0; i < n; i++) revert(level, batch[i]);
        return n;
    }

    public Map<BlockPos, BlockState> placedBlocks() {
        return placed;
    }

    public int snapshotSize() {
        return originals.size();
    }

    // --- lifecycle ------------------------------------------------------------------------

    public void forceChunks(ServerLevel level) {
        int r = radius + 2;
        for (int cx = (center.getX() - r) >> 4; cx <= (center.getX() + r) >> 4; cx++) {
            for (int cz = (center.getZ() - r) >> 4; cz <= (center.getZ() + r) >> 4; cz++) {
                if (level.setChunkForced(cx, cz, true)) forcedChunks.add(ChunkPos.asLong(cx, cz));
            }
        }
    }

    private void releaseChunks(ServerLevel level) {
        for (long c : forcedChunks) {
            level.setChunkForced(ChunkPos.getX(c), ChunkPos.getZ(c), false);
        }
        forcedChunks.clear();
    }

    public void beginRestore(boolean victory) {
        if (status == Status.ACTIVE) {
            this.victory = victory;
            status = Status.RESTORING;
        }
    }

    /** One server tick of housekeeping: timed reverts while active, batched restore afterwards. */
    public void tick(ServerLevel level) {
        if (status == Status.ACTIVE) {
            long now = level.getGameTime();
            List<BlockPos> due = new ArrayList<>();
            timedReverts.forEach((pos, at) -> {
                if (at <= now) due.add(pos);
            });
            due.forEach(p -> revert(level, p));
        } else if (status == Status.RESTORING) {
            if (!settled) {
                settled = true;
                if (stake != null) Stakes.settle(level.getServer(), this, victory, false);
                ArenaEvents.released(level, this, false);
            }
            org.papiricoh.supernaturalcraft.weather.StormLock.release(level, this);
            revertSome(level, RESTORE_BATCH);
            if (originals.isEmpty()) {
                releaseChunks(level);
                status = Status.CLOSED;
            }
        }
    }

    /** Synchronous full restore (previews, tests). */
    public void restoreNow(ServerLevel level) {
        restoreNow(level, false);
    }

    /** Synchronous full restore; on server shutdown ({@code stopping}) a stake is owed, not handed over. */
    public void restoreNow(ServerLevel level, boolean stopping) {
        if (stake != null) Stakes.settle(level.getServer(), this, status == Status.RESTORING && victory, stopping);
        if (!settled) {
            settled = true;
            ArenaEvents.released(level, this, stopping);
        }
        status = Status.RESTORING;
        org.papiricoh.supernaturalcraft.weather.StormLock.release(level, this);
        for (BlockPos pos : new ArrayList<>(originals.keySet())) revert(level, pos);
        releaseChunks(level);
        status = Status.CLOSED;
    }

    // --- participants ---------------------------------------------------------------------

    public void join(ServerPlayer player) {
        participants.add(player.getUUID());
    }

    /** Living participants currently inside the arena (or close to its edge). */
    public List<ServerPlayer> livingParticipants(ServerLevel level) {
        List<ServerPlayer> out = new ArrayList<>();
        for (UUID uuid : participants) {
            if (org.papiricoh.supernaturalcraft.util.Players.byUuid(level, uuid) instanceof ServerPlayer p && p.isAlive() && !p.isSpectator()
                    && horizontalDistance(p.position()) <= radius + EDGE_MARGIN) {
                out.add(p);
            }
        }
        return out;
    }

    /** @return true once nobody has been left fighting for {@code failureTicks} */
    public boolean checkAbandoned(ServerLevel level, int failureTicks) {
        if (!livingParticipants(level).isEmpty()) {
            emptySince = -1;
            return false;
        }
        if (emptySince < 0) emptySince = level.getGameTime();
        return level.getGameTime() - emptySince >= failureTicks;
    }

    public boolean isParticipant(Entity e) {
        return participants.contains(e.getUUID());
    }

    // --- persistence ----------------------------------------------------------------------

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (stake != null) {
            Stake.CODEC.encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), stake).result()
                    .ifPresent(t -> tag.put("Stake", t));
        }
        tag.putUUID("Id", id);
        tag.put("Center", NbtUtils.writeBlockPos(center));
        tag.putInt("Radius", radius);
        if (bossId != null) tag.putUUID("Boss", bossId);
        tag.putString("Status", status.name());
        tag.putInt("Phase", phase);
        tag.putInt("Theme", theme);
        tag.putInt("FloorRadius", floorRadius);
        tag.putBoolean("ForcedStorm", forcedStorm);
        tag.putLongArray("Protected", protectedBlocks.toLongArray());
        tag.putBoolean("Victory", victory);
        ListTag ps = new ListTag();
        participants.forEach(u -> ps.add(NbtUtils.createUUID(u)));
        tag.put("Participants", ps);
        ListTag blocks = new ListTag();
        originals.forEach((pos, state) -> {
            CompoundTag b = new CompoundTag();
            b.putLong("Pos", pos.asLong());
            b.put("Original", NbtUtils.writeBlockState(state));
            BlockState mine = placed.get(pos);
            if (mine != null) b.put("Placed", NbtUtils.writeBlockState(mine));
            Long revert = timedReverts.get(pos);
            if (revert != null) b.putLong("RevertAt", revert);
            blocks.add(b);
        });
        tag.put("Blocks", blocks);
        tag.putLongArray("Chunks", forcedChunks.toLongArray());
        return tag;
    }

    public static ArenaController load(CompoundTag tag, HolderGetter<Block> blocks, HolderLookup.Provider registries) {
        BlockPos center = NbtUtils.readBlockPos(tag, "Center").orElse(BlockPos.ZERO);
        ArenaController a = new ArenaController(tag.getUUID("Id"), center, tag.getInt("Radius"));
        if (tag.contains("Stake")) {
            a.stake = Stake.CODEC.parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag.get("Stake")).result().orElse(null);
        }
        if (tag.hasUUID("Boss")) a.bossId = tag.getUUID("Boss");
        a.status = Status.valueOf(tag.getString("Status"));
        a.phase = tag.getInt("Phase");
        a.theme = tag.getInt("Theme");
        a.floorRadius = tag.contains("FloorRadius") ? tag.getInt("FloorRadius") : -1;
        a.forcedStorm = tag.getBoolean("ForcedStorm");
        for (long l : tag.getLongArray("Protected")) a.protectedBlocks.add(l);
        a.victory = tag.getBoolean("Victory");
        for (Tag t : tag.getList("Participants", Tag.TAG_INT_ARRAY)) a.participants.add(NbtUtils.loadUUID(t));
        for (Tag t : tag.getList("Blocks", Tag.TAG_COMPOUND)) {
            CompoundTag b = (CompoundTag) t;
            BlockPos pos = BlockPos.of(b.getLong("Pos"));
            a.originals.put(pos, NbtUtils.readBlockState(blocks, b.getCompound("Original")));
            if (b.contains("Placed")) a.placed.put(pos, NbtUtils.readBlockState(blocks, b.getCompound("Placed")));
            if (b.contains("RevertAt")) a.timedReverts.put(pos, b.getLong("RevertAt"));
        }
        for (long c : tag.getLongArray("Chunks")) a.forcedChunks.add(c);
        return a;
    }

    public static HolderGetter<Block> blockLookup(ServerLevel level) {
        return level.holderLookup(Registries.BLOCK);
    }
}
