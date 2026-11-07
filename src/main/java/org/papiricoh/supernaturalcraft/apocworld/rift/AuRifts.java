package org.papiricoh.supernaturalcraft.apocworld.rift;

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
import org.papiricoh.supernaturalcraft.apocworld.ApocWorldRegistry;
import org.papiricoh.supernaturalcraft.apocworld.ApocalypseWorld;
import org.papiricoh.supernaturalcraft.hell.rift.RiftShape;

import java.util.List;
import java.util.UUID;

/**
 * Opening, crossing and closing the rifts to and from the Apocalypse World (v0.41). The geometry is Hell's ({@link RiftShape});
 * where a rift leads is worked out for each hunter as they cross: {@link AuRift.Kind#INTO} to the landing in the burned world
 * (only for a hunter on the road far enough: Jack has been born to them, {@link ApocalypseWorld#mayEnter}), with the way back
 * remembered; {@link AuRift.Kind#HOME} back to where that hunter came from.
 */
public final class AuRifts {

    /** Ticks a hunter stands in a rift before it takes them. */
    public static final int CROSSING_TICKS = 40;
    public static final int WARNING_TICKS = 600;

    private AuRifts() {
    }

    /** Opens a rift on {@code anchor} for {@code ticks}; whatever stands in its way is cleared (not block entities, not bedrock). */
    public static AuRift open(ServerLevel level, BlockPos anchor, Direction.Axis axis, AuRift.Kind kind, int ticks) {
        AuRift rift = new AuRift(UUID.randomUUID(), anchor, axis, kind, level.getGameTime() + ticks);
        BlockState state = ApocWorldRegistry.AU_RIFT.get().defaultBlockState().setValue(AuRiftBlock.AXIS, axis);
        for (BlockPos p : rift.blocks()) {
            BlockState here = level.getBlockState(p);
            if (level.getBlockEntity(p) != null || here.getDestroySpeed(level, p) < 0 && !here.is(ApocWorldRegistry.AU_RIFT.get())) continue;
            level.setBlock(p, state, Block.UPDATE_CLIENTS);
        }
        AuRiftSavedData.get(level).add(rift);
        Vec3 c = Vec3.atCenterOf(anchor.above(2));
        level.sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, 50, 0.6, 1.6, 0.6, 0.04);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y, c.z, 40, 0.6, 1.6, 0.6, 0.02);
        level.sendParticles(ParticleTypes.ASH, c.x, c.y, c.z, 80, 1.0, 2.0, 1.0, 0.02);
        level.playSound(null, anchor, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.2f, 0.6f);
        return rift;
    }

    /** The first standing spot at or near {@code y} in this column (or {@code at} itself). */
    public static BlockPos groundNear(ServerLevel level, BlockPos at, int y) {
        for (int dy = 0; dy <= 6; dy++) {
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

    /** The open rift nearest {@code at} within {@code range} blocks, or null. */
    public static @Nullable AuRift near(ServerLevel level, BlockPos at, double range, @Nullable AuRift.Kind kind) {
        AuRift best = null;
        double bestD = range * range;
        for (AuRift r : AuRiftSavedData.get(level).all()) {
            if (kind != null && r.kind() != kind) continue;
            double d = r.anchor().distSqr(at);
            if (d <= bestD) {
                bestD = d;
                best = r;
            }
        }
        return best;
    }

    // --- crossing -------------------------------------------------------------------------------

    /** Where the rift at {@code pos} takes {@code entity}; null if nowhere (not a hunter, or not yet on the road to it). */
    public static @Nullable DimensionTransition destination(ServerLevel from, Entity entity, BlockPos pos) {
        AuRift rift = AuRiftSavedData.get(from).at(pos);
        if (rift == null || !(entity instanceof ServerPlayer player)) return null;
        return destination(from, player, rift);
    }

    /** As above, for a known rift (tests). */
    public static @Nullable DimensionTransition destination(ServerLevel from, ServerPlayer player, AuRift rift) {
        if (rift.kind() == AuRift.Kind.INTO) {
            if (!ApocalypseWorld.mayEnter(player)) {
                if (player.tickCount % 40 == 0) {
                    player.displayClientMessage(Component.translatable("message.supernaturalcraft.apocworld.barred")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                }
                return null;
            }
            ServerLevel to = ApocalypseWorld.level(from.getServer());
            Vec3 landing = ApocalypseWorld.landing(to);
            Vec3 back = Vec3.atBottomCenterOf(rift.anchor()).add(Vec3.atLowerCornerOf(sideOf(rift).getNormal()).scale(2));
            ApocalypseWorld.rememberWayBack(player, from, back);
            return new DimensionTransition(to, landing, Vec3.ZERO, player.getYRot(), player.getXRot(),
                    DimensionTransition.PLAY_PORTAL_SOUND.then(e -> {
                        if (e instanceof ServerPlayer p) ApocalypseWorld.arrived(p, rift.expiresAt() - from.getGameTime());
                    }));
        }
        ApocalypseWorld.Home home = ApocalypseWorld.home(from.getServer(), player);
        return new DimensionTransition(home.level(), home.pos(), Vec3.ZERO, player.getYRot(), player.getXRot(),
                DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET));
    }

    /** The side a hunter steps out of a rift on (the face toward +x or +z). */
    public static Direction sideOf(AuRift rift) {
        return Direction.fromAxisAndDirection(rift.axis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X, Direction.AxisDirection.POSITIVE);
    }

    // --- closing --------------------------------------------------------------------------------

    /** Once a second per dimension: warn about rifts about to close and close the ones whose time is up. */
    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 20 != 11) return;
        AuRiftSavedData data = AuRiftSavedData.get(level);
        if (data.all().isEmpty()) return;
        long now = level.getGameTime();
        for (AuRift rift : List.copyOf(data.all())) {
            if (!level.isLoaded(rift.anchor())) {
                if (now >= rift.expiresAt()) data.remove(rift);
                continue;
            }
            if (now >= rift.expiresAt()) {
                collapse(level, rift);
                data.remove(rift);
            } else if (!rift.warned() && rift.expiresAt() - now <= WARNING_TICKS) {
                rift.setWarned();
                data.setDirty();
                for (ServerPlayer p : level.players()) {
                    if (p.blockPosition().closerThan(rift.anchor(), 48)) {
                        p.displayClientMessage(Component.translatable("message.supernaturalcraft.rift.closing")
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
                    }
                }
            }
        }
    }

    /** Seals a rift at once. */
    public static void collapse(ServerLevel level, AuRift rift) {
        for (BlockPos p : rift.blocks()) {
            if (level.getBlockState(p).is(ApocWorldRegistry.AU_RIFT.get())) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        Vec3 c = Vec3.atCenterOf(rift.anchor().above(2));
        level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y, c.z, 40, 0.5, 1.5, 0.5, 0.06);
        level.playSound(null, rift.anchor(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.2f, 0.8f);
    }

    /** Seals every rift in {@code level} (commands, tests). @return how many */
    public static int collapseAll(ServerLevel level) {
        AuRiftSavedData data = AuRiftSavedData.get(level);
        int n = 0;
        for (AuRift r : List.copyOf(data.all())) {
            collapse(level, r);
            data.remove(r);
            n++;
        }
        return n;
    }
}
