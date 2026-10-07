package org.papiricoh.supernaturalcraft.grave;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.ghost.GhostBalance;
import org.papiricoh.supernaturalcraft.entity.ghost.GhostEntity;
import org.papiricoh.supernaturalcraft.registry.AllBlockEntities;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.UUID;

/**
 * Remembers the ghost these bones raise. At night, with a player near, it raises the ghost again
 * if it is not about (never once the bones are at rest).
 */
public class GraveBonesBlockEntity extends BlockEntity {

    @Nullable
    private UUID ghost;
    private long nextRaise;

    public GraveBonesBlockEntity(BlockPos pos, BlockState state) {
        super(AllBlockEntities.GRAVE_BONES.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GraveBonesBlockEntity be) {
        if (!(level instanceof ServerLevel server) || state.getValue(GraveBonesBlock.RESTED)) return;
        if ((server.getGameTime() + pos.asLong()) % GhostBalance.RAISE_CHECK != 0) return;
        if (!nightIn(server) || server.getGameTime() < be.nextRaise) return;
        if (server.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, GhostBalance.RAISE_RANGE,
                EntitySelector.NO_CREATIVE_OR_SPECTATOR) == null) return;
        if (be.ghost(server) == null) be.raiseGhost(server);
    }

    /** Night here: never in a dimension with a fixed time (the Nether, Hell). */
    public static boolean nightIn(ServerLevel level) {
        return !level.dimensionType().hasFixedTime() && GhostBalance.isNight(level.getDayTime());
    }

    /** The ghost these bones raised, if it is loaded and still about. */
    @Nullable
    public GhostEntity ghost(ServerLevel level) {
        if (ghost == null) return null;
        return level.getEntity(ghost) instanceof GhostEntity g && g.isAlive() && !g.isRemoved() ? g : null;
    }

    @Nullable
    public UUID ghostId() {
        return ghost;
    }

    /** Raises the ghost over the grave now (whatever the hour). Null if the bones are at rest. */
    @Nullable
    public GhostEntity raiseGhost(ServerLevel level) {
        if (getBlockState().getValue(GraveBonesBlock.RESTED)) return null;
        GhostEntity old = ghost(level);
        if (old != null) old.discard();
        GhostEntity g = AllEntities.GHOST.get().create(level);
        if (g == null) return null;
        BlockPos at = worldPosition.above(GraveLayout.DEPTH + 1);
        g.moveTo(at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5, level.random.nextFloat() * 360f, 0);
        g.setBones(worldPosition);
        level.addFreshEntity(g);
        ghost = g.getUUID();
        nextRaise = level.getGameTime() + GhostBalance.RAISE_COOLDOWN;
        setChanged();
        level.sendParticles(ParticleTypes.SOUL, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 12, 0.3, 0.6, 0.3, 0.02);
        level.playSound(null, at, AllSounds.GHOST_WHISPER.get(), SoundSource.HOSTILE, 1.0f, 0.8f);
        return g;
    }

    /** Whether {@code g} is the ghost these bones answer for. */
    public boolean owns(GhostEntity g) {
        return g.getUUID().equals(ghost);
    }

    /** The spirit is free: the bones are only bones now. */
    public void markRested() {
        ghost = null;
        setChanged();
        if (level != null) {
            BlockState s = getBlockState();
            if (!s.getValue(GraveBonesBlock.RESTED)) level.setBlock(worldPosition, s.setValue(GraveBonesBlock.RESTED, true), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (ghost != null) tag.putUUID("Ghost", ghost);
        tag.putLong("NextRaise", nextRaise);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ghost = tag.hasUUID("Ghost") ? tag.getUUID("Ghost") : null;
        nextRaise = tag.getLong("NextRaise");
    }
}
