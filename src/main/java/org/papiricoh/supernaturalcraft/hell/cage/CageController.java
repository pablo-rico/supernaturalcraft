package org.papiricoh.supernaturalcraft.hell.cage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/**
 * Whether the Cage stands open. The summoning rite opens it (the iris in its floor draws back ring by
 * ring, chains groaning, the Pit shaking) and it shuts again when the fight ends, whichever way. While
 * shut, Lucifer waits inside it in chains.
 */
public class CageController extends SavedData {

    public enum State { CLOSED, OPENING, OPEN, CLOSING }

    /** Ticks between the iris's rings moving. */
    public static final int STEP_TICKS = 12;
    private static final String NAME = "supernaturalcraft_cage";

    private State state = State.CLOSED;
    private int step;
    private int timer;

    /** Levels whose Cage record has been opened this session (Hell, or wherever a command or test built one). */
    private static final java.util.Set<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>> KNOWN =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static CageController get(ServerLevel level) {
        KNOWN.add(level.dimension());
        return level.getDataStorage().computeIfAbsent(new Factory<>(CageController::new, CageController::load), NAME);
    }

    private static CageController load(CompoundTag tag, HolderLookup.Provider registries) {
        CageController c = new CageController();
        c.state = State.valueOf(tag.getString("State").isEmpty() ? "CLOSED" : tag.getString("State"));
        c.step = tag.getInt("Step");
        c.timer = tag.getInt("Timer");
        return c;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("State", state.name());
        tag.putInt("Step", step);
        tag.putInt("Timer", timer);
        return tag;
    }

    /** Whether this level's Cage record is in use this session (cheap: no disk lookup every tick). */
    public static boolean has(ServerLevel level) {
        return KNOWN.contains(level.dimension());
    }

    public State state() {
        return state;
    }

    public boolean isClosed() {
        return state == State.CLOSED;
    }

    public boolean isOpen() {
        return state == State.OPEN;
    }

    /** Begins to open the Cage. Lucifer in his chains is gone the moment it starts. */
    public void open(ServerLevel level) {
        if (state == State.OPEN || state == State.OPENING) return;
        state = State.OPENING;
        timer = STEP_TICKS;
        setDirty();
        for (CagedLuciferEntity e : level.getEntitiesOfClass(CagedLuciferEntity.class, new AABB(CageLayout.THRONE).inflate(16))) e.discard();
        Vec3 c = Vec3.atCenterOf(CageLayout.THRONE);
        level.playSound(null, CageLayout.THRONE, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 4.0f, 0.5f);
        level.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 2, 0, 0, 0, 0);
        shake(level, 80, 0.8f, 0xB3121A);
    }

    public void close(ServerLevel level) {
        if (state == State.CLOSED || state == State.CLOSING) return;
        state = State.CLOSING;
        timer = STEP_TICKS;
        setDirty();
        level.playSound(null, CageLayout.THRONE, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 4.0f, 0.4f);
        shake(level, 50, 0.5f, 0x000000);
    }

    /** Opens or shuts it at once (commands, tests). */
    public void set(ServerLevel level, boolean open) {
        state = open ? State.OPEN : State.CLOSED;
        step = open ? CageLayout.IRIS_STEPS : 0;
        CageBuilder.iris(level, CageBuilder.extent(), step);
        if (open) {
            for (CagedLuciferEntity e : level.getEntitiesOfClass(CagedLuciferEntity.class, new AABB(CageLayout.THRONE).inflate(16))) e.discard();
        }
        setDirty();
    }

    public void tick(ServerLevel level) {
        if (state == State.OPENING || state == State.CLOSING) {
            if (--timer > 0) return;
            timer = STEP_TICKS;
            step += state == State.OPENING ? 1 : -1;
            CageBuilder.iris(level, CageBuilder.extent(), step);
            Vec3 c = Vec3.atCenterOf(new BlockPos(0, CageLayout.CAGE_FLOOR, 0));
            level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y, c.z, 40, 2.5, 0.3, 2.5, 0.03);
            level.sendParticles(AllParticles.HELLFIRE.get(), c.x, c.y - 0.5, c.z, 30, 2.5, 0.2, 2.5, 0.05);
            level.playSound(null, BlockPos.containing(c), SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 3.0f, 0.5f);
            level.playSound(null, BlockPos.containing(c), SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 3.0f, 0.4f + step * 0.1f);
            if (state == State.OPENING && step >= CageLayout.IRIS_STEPS) state = State.OPEN;
            if (state == State.CLOSING && step <= 0) state = State.CLOSED;
            setDirty();
        } else if (state == State.CLOSED && level.getGameTime() % 100 == 0 && level.isLoaded(CageLayout.THRONE)
                && level.getEntitiesOfClass(CagedLuciferEntity.class, new AABB(CageLayout.THRONE).inflate(16)).isEmpty()
                && !level.getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class, new AABB(CageLayout.THRONE).inflate(128)).isEmpty()) {
            CagedLuciferEntity.place(level, AllEntities.CAGED_LUCIFER.get());
        }
    }

    private static void shake(ServerLevel level, int ticks, float strength, int flash) {
        CinematicPayload payload = new CinematicPayload(ticks, strength, flash, flash == 0 ? 0f : 0.25f, false, "", "");
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(0, CageLayout.ISLAND_Y, 0) < 160 * 160) PacketDistributor.sendToPlayer(p, payload);
        }
    }

}
