package org.papiricoh.supernaturalcraft.hell.cage;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Brings the Cage of a world generated before {@link CagePlan} up to date (v0.30). The Cage's record
 * ({@link CageController#layout}) says which builder made it; an old world is told apart by a block the
 * old builder put where the plan does not ({@link #looksOld}). The rebuild goes one chunk per tick: in
 * each it clears what the old builder placed ({@link CageLegacy#erasable}) and writes the plan, so a
 * floor never vanishes for more than a tick. It waits while the Cage is open or a fight holds Hell,
 * and resumes after a restart from the saved cursor.
 */
public final class CageWorld {

    /** The layout this version builds (0 = not yet checked, {@link #OLD} = the first builder's, being rebuilt). */
    public static final int CURRENT = 2, OLD = 1;
    /** One of the old Cage's corner posts: the old builder put frame here, the plan does not. */
    public static final BlockPos PROBE = new BlockPos(CageLayout.CAGE_HALF, 130, CageLayout.CAGE_DIAG - CageLayout.CAGE_HALF);

    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static volatile List<ChunkPos> chunks;
    private static volatile Set<Block> oldBlocks;

    private CageWorld() {
    }

    /** Whether the Cage standing in {@code level} is the old builder's. */
    public static boolean looksOld(ServerLevel level) {
        return level.getBlockState(PROBE).is(AllBlocks.CAGE_FRAME.get());
    }

    /** Called every tick from {@link CageController#tick}: checks the layout once, then advances a rebuild. */
    static void tick(ServerLevel level, CageController cage) {
        if (cage.layout() == 0) {
            if (!level.isLoaded(PROBE)) return;
            cage.setLayout(looksOld(level) ? OLD : CURRENT, 0);
            if (cage.layout() == OLD) announce(level);
        }
        if (cage.layout() != OLD || !cage.isClosed() || ArenaSavedData.get(level).hasActive()) return;
        List<ChunkPos> all = chunks();
        int i = cage.cursor();
        if (i < all.size()) {
            rebuild(level, all.get(i));
            cage.setLayout(OLD, i + 1);
            return;
        }
        cage.set(level, false);
        for (CagedLuciferEntity e : level.getEntitiesOfClass(CagedLuciferEntity.class, new AABB(CageLayout.THRONE).inflate(16))) e.discard();
        cage.setLayout(CURRENT, 0);
        for (ServerPlayer p : level.players()) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.cage.rebuilt").withStyle(ChatFormatting.DARK_RED), false);
        }
    }

    /** Starts the rebuild over (the command): whatever stands is treated as the old Cage. */
    public static void restart(ServerLevel level) {
        CageController.get(level).setLayout(OLD, 0);
        announce(level);
    }

    /** Whether a rebuild is pending or under way (the summoning rite waits for it). */
    public static boolean rebuilding(ServerLevel level) {
        return CageController.get(level).layout() == OLD;
    }

    private static void announce(ServerLevel level) {
        for (ServerPlayer p : level.players()) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.cage.rebuilding").withStyle(ChatFormatting.DARK_RED), false);
        }
    }

    /** Clears the old builder's blocks from one chunk and writes the plan's cells there, in the same tick. */
    public static void rebuild(ServerLevel level, ChunkPos chunk) {
        level.getChunk(chunk.x, chunk.z);
        Set<Block> old = oldBlocks();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                for (int y = CageLegacy.OLD_MIN_Y; y <= CageLegacy.OLD_MAX_Y; y++) {
                    if (!CageLegacy.inOldExtent(x, y, z)) continue;
                    p.set(x, y, z);
                    BlockState s = level.getBlockState(p);
                    if (s.isAir()) continue;
                    boolean erase = old.contains(s.getBlock())
                            || s.is(AllBlocks.HELLSTONE_BRICKS.get()) && CageLegacy.oldGate(x, y, z);
                    if (erase) level.setBlock(p, air, FLAGS);
                }
            }
        }
        CageBuilder.writeChunk(level, chunk.x, chunk.z);
    }

    /** Every chunk the old Cage or the plan touches, in a fixed order (the saved cursor indexes it). */
    static List<ChunkPos> chunks() {
        List<ChunkPos> c = chunks;
        if (c == null) {
            Set<ChunkPos> set = new LinkedHashSet<>();
            int r = CageLegacy.OLD_REACH >> 4;
            for (int x = -r - 1; x <= r; x++) {
                for (int z = -r - 1; z <= r; z++) set.add(new ChunkPos(x, z));
            }
            set.addAll(CageBuilder.chunks());
            c = new ArrayList<>(set);
            c.sort(Comparator.comparingInt((ChunkPos k) -> k.x).thenComparingInt(k -> k.z));
            chunks = c = List.copyOf(c);
        }
        return c;
    }

    private static Set<Block> oldBlocks() {
        Set<Block> s = oldBlocks;
        if (s == null) {
            Set<Block> out = new java.util.HashSet<>();
            for (String id : CageLegacy.OLD_IDS) {
                out.add(BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse(id)));
            }
            oldBlocks = s = Set.copyOf(out);
        }
        return s;
    }
}
