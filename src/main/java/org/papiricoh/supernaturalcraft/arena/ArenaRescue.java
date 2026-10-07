package org.papiricoh.supernaturalcraft.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * On a platform that breaks away, falling is not death: a hand of light catches the challenger
 * and sets them back down on what is left of the floor — bruised and weakened, but still in the fight.
 */
public final class ArenaRescue {

    /** How far below the arena centre a challenger must fall before the hand comes for them. */
    public static final int FALL_DEPTH = 6;
    public static final float DAMAGE = 6.0f;
    public static final int WEAKNESS_TICKS = 200;
    private static final int COOLDOWN = 40;

    private static final Map<UUID, Long> lastRescue = new HashMap<>();

    private ArenaRescue() {
    }

    /** @return true if the player had fallen and was carried back */
    public static boolean check(ServerLevel level, ArenaController arena, ServerPlayer p) {
        if (!ArenaTheme.rescuesFallers(arena.theme()) || p.isSpectator() || p.isCreative() || !p.isAlive()) return false;
        if (p.getY() >= arena.center().getY() - FALL_DEPTH) return false;
        long now = level.getGameTime();
        Long last = lastRescue.get(p.getUUID());
        if (last != null && now - last < COOLDOWN && now >= last) return false;
        lastRescue.put(p.getUUID(), now);

        Vec3 from = p.position();
        BlockPos land = landing(level, arena, level.getRandom());
        p.fallDistance = 0;
        // moveTo for the server's own view (test players have no connection to teleport through), teleportTo for the client.
        p.moveTo(land.getX() + 0.5, land.getY() + 1, land.getZ() + 0.5);
        p.teleportTo(land.getX() + 0.5, land.getY() + 1, land.getZ() + 0.5);
        p.setDeltaMovement(Vec3.ZERO);
        p.hurtMarked = true;
        p.hurt(AllDamageTypes.source(level, AllDamageTypes.JUDGMENT, null), DAMAGE);
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_TICKS, 0));
        hand(level, from, p.position());
        return true;
    }

    /** A random standing spot on the remaining floor, away from the very edge. */
    public static BlockPos landing(ServerLevel level, ArenaController arena, RandomSource random) {
        int r = Math.max(2, arena.floorRadius() - 2);
        BlockPos c = arena.center();
        for (int tries = 0; tries < 24; tries++) {
            double a = random.nextDouble() * Math.PI * 2, d = 2 + random.nextDouble() * (r - 2);
            BlockPos s = ArenaTerrain.surface(level, arena, c.getX() + (int) Math.round(Math.cos(a) * d),
                    c.getZ() + (int) Math.round(Math.sin(a) * d));
            if (s != null && standable(level, s)) return s;
        }
        @Nullable BlockPos s = ArenaTerrain.surface(level, arena, c.getX() + 2, c.getZ());
        return s != null ? s : c;
    }

    private static boolean standable(ServerLevel level, BlockPos s) {
        BlockState ground = level.getBlockState(s);
        return !ground.is(AllTags.Blocks.ARENA_IMMUNE) && level.getBlockState(s.above(2)).getCollisionShape(level, s.above(2)).isEmpty();
    }

    /** A streak of light from where they fell to where they land. */
    private static void hand(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 step = to.subtract(from);
        for (int i = 0; i <= 24; i++) {
            Vec3 p = from.add(step.scale(i / 24.0));
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 1, p.z, 2, 0.25, 0.25, 0.25, 0.01);
        }
        level.sendParticles(ParticleTypes.FLASH, to.x, to.y + 1, to.z, 1, 0, 0, 0, 0);
        level.playSound(null, to.x, to.y, to.z, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.0f, 1.6f);
        level.playSound(null, to.x, to.y, to.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1.2f, 0.8f);
    }

    public static void forget(UUID player) {
        lastRescue.remove(player);
    }
}
