package org.papiricoh.supernaturalcraft.entity.boss.gabriel;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTerrain;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Baiting the Trickster (v0.14): TV Land opens round the altar, a ring of holy oil catches fire round it, the static flashes,
 * a snap of the fingers and "Welcome to TV Land!": Gabriel steps out facing whoever laid the bait, and the sitcom's set
 * writes itself round them ({@link GabrielEntity}'s first ticks).
 */
public final class GabrielSummoning {

    public static final double DISTANCE = 6;
    /** The ring of holy oil round the altar. */
    public static final int RING_RADIUS = 3;
    /** The ring burns through his entrance and goes out soon after, so it never hides the sitcom's floor. */
    public static final int RING_TICKS = 160;

    private GabrielSummoning() {
    }

    /** @return Gabriel, or null (and a message to the ritualist) if another fight already holds this world */
    public static @Nullable GabrielEntity summon(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        ArenaController arena = LuciferSummoning.openArena(level, altar, SNConfig.GABRIEL_ARENA_RADIUS.get(), ArenaTheme.TV_LAND);
        if (arena == null) {
            if (ritualist != null) {
                ritualist.displayClientMessage(Component.translatable("message.supernaturalcraft.gabriel.busy")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return null;
        }
        GabrielEntity boss = AllEntities.GABRIEL.get().create(level);
        if (boss == null) return null;
        ringOfFire(level, arena, altar);
        Vec3 dir = ritualist != null ? ritualist.position().subtract(Vec3.atCenterOf(altar)).multiply(1, 0, 1) : new Vec3(0, 0, 1);
        if (dir.lengthSqr() < 0.01) dir = new Vec3(0, 0, 1);
        dir = dir.normalize();
        double x = altar.getX() + 0.5 - dir.x * DISTANCE, z = altar.getZ() + 0.5 - dir.z * DISTANCE;
        BlockPos floor = ArenaTerrain.surface(level, arena, (int) Math.floor(x), (int) Math.floor(z));
        double y = floor != null ? floor.getY() + 1 : altar.getY();
        float yaw = (float) Math.toDegrees(Math.atan2(dir.x, -dir.z));
        if (ritualist != null) yaw = (float) Math.toDegrees(Math.atan2(-(ritualist.getX() - x), ritualist.getZ() - z));
        boss.moveTo(x, y, z, yaw, 0);
        boss.setYHeadRot(yaw);
        boss.setYBodyRot(yaw);
        boss.bindArena(arena);
        level.addFreshEntity(boss);
        boss.beginEmergence();
        // The static is the client's (on the CHANNEL payload Gabriel sends as he emerges).
        level.playSound(null, altar, AllSounds.GABRIEL_SNAP.get(), SoundSource.HOSTILE, 3.0f, 1.0f);
        return boss;
    }

    /** Lights holy oil in a ring round the altar (through the arena, so the ground comes back). @return the fires lit */
    public static Set<BlockPos> ringOfFire(ServerLevel level, ArenaController arena, BlockPos altar) {
        Set<BlockPos> ring = new LinkedHashSet<>();
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2 / 24;
            ring.add(altar.offset((int) Math.round(Math.cos(a) * RING_RADIUS), 0, (int) Math.round(Math.sin(a) * RING_RADIUS)));
        }
        BlockState fire = AllBlocks.HOLY_OIL_FIRE.get().defaultBlockState();
        Set<BlockPos> lit = new LinkedHashSet<>();
        for (BlockPos pos : ring) {
            BlockState here = level.getBlockState(pos);
            if (!(here.isAir() || here.canBeReplaced()) || !here.getFluidState().isEmpty()) continue;
            if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) continue;
            if (arena.mutate(level, pos, fire, RING_TICKS)) lit.add(pos);
        }
        return lit;
    }
}
