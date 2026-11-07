package org.papiricoh.supernaturalcraft.purgatory.rift;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.hell.rift.RiftShape;
import org.papiricoh.supernaturalcraft.purgatory.PurgatoryWorld;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;
import java.util.UUID;

/**
 * Opening, crossing and closing Purgatory's rifts (v0.39). The geometry is Hell's ({@link RiftShape}); where a rift leads is not
 * fixed but worked out for each hunter as they cross: {@link PurgatoryRift.Kind#INTO} to the landing near Purgatory's heart (and
 * the hunter's way back remembered), {@link PurgatoryRift.Kind#HOME} back to where that hunter came from, and only for one Purgatory
 * lets go ({@link PurgatoryWorld#mayLeave}: Eve has fallen to them).
 */
public final class PurgatoryRifts {

    /** Ticks a hunter stands in a rift before it takes them. */
    public static final int CROSSING_TICKS = 40;
    /** How long a rite's rift stays open, and the one that opens in Eve's glade when she falls. */
    public static final int RITE_TICKS = 5 * 60 * 20, GLADE_TICKS = 10 * 60 * 20;
    public static final int WARNING_TICKS = 600;

    private PurgatoryRifts() {
    }

    /** Opens a rift on {@code anchor} for {@code ticks}; whatever stands in its way is cleared (not block entities, not bedrock). */
    public static PurgatoryRift open(ServerLevel level, BlockPos anchor, Direction.Axis axis, PurgatoryRift.Kind kind, int ticks) {
        PurgatoryRift rift = new PurgatoryRift(UUID.randomUUID(), anchor, axis, kind, level.getGameTime() + ticks);
        BlockState state = AllBlocks.PURGATORY_RIFT.get().defaultBlockState().setValue(PurgatoryRiftBlock.AXIS, axis);
        for (BlockPos p : rift.blocks()) {
            BlockState here = level.getBlockState(p);
            if (level.getBlockEntity(p) != null || here.getDestroySpeed(level, p) < 0 && !here.is(AllBlocks.PURGATORY_RIFT.get())) continue;
            level.setBlock(p, state, Block.UPDATE_CLIENTS);
        }
        PurgatoryRiftSavedData.get(level).add(rift);
        Vec3 c = Vec3.atCenterOf(anchor.above(2));
        level.sendParticles(ParticleTypes.SOUL, c.x, c.y, c.z, 60, 0.6, 1.6, 0.6, 0.04);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y, c.z, 30, 0.6, 1.6, 0.6, 0.02);
        level.playSound(null, anchor, AllSounds.purgatory("rift_open"), SoundSource.BLOCKS, 1.6f, 1.0f);
        return rift;
    }

    /** A rite's rift: six blocks behind the altar as the ritualist sees it, facing them. */
    public static PurgatoryRift openAtAltar(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist, PurgatoryRift.Kind kind) {
        Direction facing = Direction.SOUTH;
        if (ritualist != null) {
            Vec3 d = Vec3.atCenterOf(altar).subtract(ritualist.position());
            if (d.horizontalDistanceSqr() > 0.01) facing = Direction.getNearest(d.x, 0, d.z);
        }
        BlockPos anchor = groundNear(level, altar.relative(facing, 6), altar.getY());
        return open(level, anchor, RiftShape.axisFacing(facing), kind, RITE_TICKS);
    }

    /** The first standing spot at or near {@code y} in this column (or {@code at} itself). */
    public static BlockPos groundNear(ServerLevel level, BlockPos at, int y) {
        for (int dy = 0; dy <= 4; dy++) {
            for (int sign : new int[]{1, -1}) {
                BlockPos p = new BlockPos(at.getX(), y + dy * sign, at.getZ());
                if (level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)
                        && level.getBlockState(p).canBeReplaced() && level.getBlockState(p.above()).canBeReplaced()) {
                    return p;
                }
            }
        }
        return new BlockPos(at.getX(), y, at.getZ());
    }

    // --- crossing -------------------------------------------------------------------------------

    /** Where the rift at {@code pos} takes {@code entity}; null if nowhere (not a hunter, or Purgatory will not let them go). */
    public static @Nullable DimensionTransition destination(ServerLevel from, Entity entity, BlockPos pos) {
        PurgatoryRift rift = PurgatoryRiftSavedData.get(from).at(pos);
        if (rift == null || !(entity instanceof ServerPlayer player)) return null;
        return destination(from, player, rift);
    }

    /** As above, for a known rift (tests). */
    public static @Nullable DimensionTransition destination(ServerLevel from, ServerPlayer player, PurgatoryRift rift) {
        if (rift.kind() == PurgatoryRift.Kind.INTO) {
            ServerLevel to = PurgatoryWorld.level(from.getServer());
            Vec3 landing = PurgatoryWorld.landing(to);
            Vec3 back = Vec3.atBottomCenterOf(rift.anchor()).add(Vec3.atLowerCornerOf(sideOf(rift).getNormal()).scale(2));
            PurgatoryWorld.rememberWayBack(player, from, back);
            return new DimensionTransition(to, landing, Vec3.ZERO, player.getYRot(), player.getXRot(),
                    DimensionTransition.PLAY_PORTAL_SOUND.then(e -> {
                        if (e instanceof ServerPlayer p) PurgatoryWorld.arrived(p);
                    }));
        }
        if (!PurgatoryWorld.mayLeave(player)) {
            if (player.tickCount % 40 == 0) {
                player.displayClientMessage(Component.translatable("message.supernaturalcraft.purgatory.held")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            }
            return null;
        }
        PurgatoryWorld.Home home = PurgatoryWorld.home(from.getServer(), player);
        return new DimensionTransition(home.level(), home.pos(), Vec3.ZERO, player.getYRot(), player.getXRot(),
                DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET));
    }

    /** The side a hunter steps out of a rift on (the face toward +x or +z). */
    private static Direction sideOf(PurgatoryRift rift) {
        return Direction.fromAxisAndDirection(rift.axis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X, Direction.AxisDirection.POSITIVE);
    }

    // --- closing --------------------------------------------------------------------------------

    /** Once a second per dimension: warn about rifts about to close and close the ones whose time is up. */
    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 20 != 7) return;
        PurgatoryRiftSavedData data = PurgatoryRiftSavedData.get(level);
        if (data.all().isEmpty()) return;
        long now = level.getGameTime();
        for (PurgatoryRift rift : List.copyOf(data.all())) {
            if (!level.isLoaded(rift.anchor())) continue;
            if (now >= rift.expiresAt()) {
                collapse(level, rift);
                data.remove(rift);
            } else if (!rift.warned() && rift.expiresAt() - now <= WARNING_TICKS) {
                rift.setWarned();
                data.setDirty();
                for (ServerPlayer p : level.players()) {
                    if (p.blockPosition().closerThan(rift.anchor(), 48)) {
                        p.displayClientMessage(Component.translatable("message.supernaturalcraft.rift.closing")
                                .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC), false);
                    }
                }
            }
        }
    }

    /** Seals a rift at once. */
    public static void collapse(ServerLevel level, PurgatoryRift rift) {
        for (BlockPos p : rift.blocks()) {
            if (level.getBlockState(p).is(AllBlocks.PURGATORY_RIFT.get())) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        Vec3 c = Vec3.atCenterOf(rift.anchor().above(2));
        level.sendParticles(ParticleTypes.SOUL, c.x, c.y, c.z, 40, 0.5, 1.5, 0.5, 0.06);
        level.playSound(null, rift.anchor(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.2f, 0.8f);
    }
}
