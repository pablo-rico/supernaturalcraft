package org.papiricoh.supernaturalcraft.entity.boss.lilith;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.uncaged.UncagedAttacks;
import org.papiricoh.supernaturalcraft.entity.magic.WardEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.entity.projectile.BossShard;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.*;

/**
 * Lilith's attacks: her contracts and her white light (the fight's two mechanics), shards and rings of
 * light, a clawing rush, and once she is pressed, Lucifer Uncaged's hounds and Lucifer's own lanes of
 * judgement.
 */
public final class LilithAttacks {

    private LilithAttacks() {
    }

    private static AttackScheduler.Option<LuciferEntity> opt(Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(
            opt(Contract::new, 3), opt(LightShards::new, 3), opt(Smother::new, 2), opt(Rend::new, 2));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(
            opt(Contract::new, 3), opt(LightShards::new, 2.5f), opt(Smother::new, 2), opt(Rend::new, 2),
            opt(UncagedAttacks.HoundPack::new, 1.5f), opt(BlindingFlash::new, 2));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(
            opt(Contract::new, 3), opt(LightShards::new, 2.5f), opt(Smother::new, 1.5f), opt(Rend::new, 2),
            opt(UncagedAttacks.HoundPack::new, 1), opt(BlindingFlash::new, 2), opt(LuciferAttacks.Judgement::new, 1.5f));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            default -> P3;
        };
    }

    private static LilithEntity lilith(LuciferEntity boss) {
        return (LilithEntity) boss;
    }

    private static boolean isFoe(LuciferEntity boss, LivingEntity e) {
        return e != boss && e.isAlive() && !(e instanceof ArmorStand) && !boss.minions().contains(e.getUUID());
    }

    private static List<LivingEntity> foes(LuciferEntity boss, AABB box) {
        return level(boss).getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(boss, e));
    }

    // --- the contract -----------------------------------------------------------------------

    /** She points, and a name is written in fire: the contract comes due unless they wound her enough in time. */
    public static class Contract extends BossAttack<LuciferEntity> {
        private final List<ServerPlayer> marked = new ArrayList<>();

        public Contract() {
            super("contract", "contract", 22, 4, 14);
        }

        private static List<ServerPlayer> candidates(LuciferEntity boss) {
            LilithEntity l = lilith(boss);
            List<ServerPlayer> out = new ArrayList<>();
            for (ServerPlayer p : boss.challengers()) if (!l.contracts().has(p.getUUID())) out.add(p);
            out.sort(Comparator.comparingDouble((ServerPlayer p) -> p.getHealth()).reversed());
            return out;
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            int room = LilithBalance.contractsAtOnce(boss.phase()) - lilith(boss).contracts().size();
            return room > 0 && !candidates(boss).isEmpty() ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            int room = LilithBalance.contractsAtOnce(boss.phase()) - lilith(boss).contracts().size();
            for (ServerPlayer p : candidates(boss)) {
                if (marked.size() >= room) break;
                marked.add(p);
                TelegraphMarker.ring(level(boss), p.position(), 1.4f, TelegraphMarker.WHITE, windup + 4);
            }
            sound(boss, AllSounds.CONTRACT_SIGN.get(), 2f, 0.6f);
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            if (t % 3 != 0) return;
            for (ServerPlayer p : marked) {
                level(boss).sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 2.2, p.getZ(), 2, 0.2, 0.05, 0.2, 0.0);
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (ServerPlayer p : marked) if (p.isAlive()) lilith(boss).signContract(p);
            sound(boss, AllSounds.LILITH_LAUGH.get(), 1.5f, 1.0f);
        }
    }

    // --- the white light ----------------------------------------------------------------------

    /**
     * The white light that burned out a room full of people: it charges, then falls on everyone she can
     * see. A headstone between her and a hunter takes it instead, and cracks. In her last phase it comes
     * twice. Afterwards she is spent.
     */
    public static class WhiteLight extends BossAttack<LuciferEntity> {
        private boolean second;

        public WhiteLight() {
            super("white_light", "white_light", 50, LilithBalance.SECOND_BURST_AFTER + 2, LilithBalance.EMPTY_TICKS);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return 0;  // only ever forced
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            TelegraphMarker.ring(level(boss), boss.position(), 3.5f, TelegraphMarker.WHITE, windup);
            sound(boss, AllSounds.LILITH_LIGHT_CHARGE.get(), 3f, 0.7f);
            for (ServerPlayer p : boss.challengers()) {
                p.displayClientMessage(Component.translatable("message.supernaturalcraft.lilith.light_warning")
                        .withStyle(net.minecraft.ChatFormatting.WHITE), true);
            }
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            ServerLevel level = level(boss);
            double r = 6 - t * 0.1;
            for (int i = 0; i < 4; i++) {
                double a = boss.getRandom().nextDouble() * Math.PI * 2;
                level.sendParticles(AllParticles.WHITE_LIGHT.get(), boss.getX() + Math.cos(a) * r, boss.getY() + 1.2 + boss.getRandom().nextDouble(),
                        boss.getZ() + Math.sin(a) * r, 0, -Math.cos(a), 0, -Math.sin(a), 0.15);
            }
            if (t % 10 == 0) sound(boss, AllSounds.LILITH_LIGHT_CHARGE.get(), 1.5f, 0.8f + t * 0.015f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            burst(lilith(boss));
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (boss.phase() < 3 || second) return;
            if (t >= LilithBalance.SECOND_BURST_AFTER - 12 && t % 3 == 0) {
                level(boss).sendParticles(AllParticles.WHITE_LIGHT.get(), boss.getX(), boss.getY() + 1.4, boss.getZ(), 10, 0.5, 0.6, 0.5, 0.05);
            }
            if (t >= LilithBalance.SECOND_BURST_AFTER) {
                second = true;
                burst(lilith(boss));
            }
        }

        @Override
        public boolean endEarly(LuciferEntity boss, LivingEntity target) {
            return boss.phase() < 3 || second;
        }

        /** Where the light from {@code lilith}'s eyes to {@code victim}'s is stopped, or null if it reaches them. */
        public static @Nullable BlockHitResult blocked(LuciferEntity lilith, LivingEntity victim) {
            BlockHitResult clip = level(lilith).clip(new ClipContext(lilith.getEyePosition(), victim.getEyePosition(),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, lilith));
            return clip.getType() == HitResult.Type.BLOCK ? clip : null;
        }

        /** One burst: everyone in her sight is burned; every headstone that stood in the way cracks. */
        public static void burst(LilithEntity lilith) {
            ServerLevel level = level(lilith);
            float damage = SNConfig.LILITH_WHITE_LIGHT_DAMAGE.get().floatValue();
            Set<BlockPos> cracked = new HashSet<>();
            for (ServerPlayer p : lilith.challengers()) {
                if (WardEntity.isSheltered(p)) continue;
                BlockHitResult cover = blocked(lilith, p);
                if (cover != null) {
                    BlockPos at = cover.getBlockPos();
                    if (level.getBlockState(at).is(AllBlocks.CRACKED_HEADSTONE.get()) && cracked.add(at)) lilith.crackHeadstone(level, at);
                    continue;
                }
                hit(lilith, p, AllDamageTypes.WHITE_LIGHT, damage);
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), lilith);
            }
            level.sendParticles(ParticleTypes.FLASH, lilith.getX(), lilith.getY() + 1.4, lilith.getZ(), 2, 0, 0, 0, 0);
            level.sendParticles(AllParticles.WHITE_LIGHT.get(), lilith.getX(), lilith.getY() + 1.2, lilith.getZ(), 120, 0.4, 0.6, 0.4, 0.6);
            sound(lilith, AllSounds.LILITH_LIGHT_BURST.get(), 4f, 1.0f);
            lilith.spend();
        }
    }

    // --- light ------------------------------------------------------------------------------

    /** A fan of shards of light, turning a little after their mark. */
    public static class LightShards extends BossAttack<LuciferEntity> {
        public LightShards() {
            super("light_shards", "shards", 20, 4, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.hasLineOfSight(target) ? 1 : 0.2f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            TelegraphMarker.cone(level(boss), boss.position(), yawTo(boss.position(), target.position()), 8, TelegraphMarker.WHITE, windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            Vec3 from = boss.getEyePosition();
            Vec3 dir = target.getEyePosition().subtract(from).normalize();
            double base = Math.atan2(dir.z, dir.x);
            int n = boss.phase() >= 3 ? 7 : 5;
            for (int i = 0; i < n; i++) {
                double a = base + Math.toRadians((i - (n - 1) / 2.0) * 10);
                BossShard s = new BossShard(level, boss, BossShard.Kind.FEATHER, 5f * boss.attackDamageMultiplier(), 0.02f, target);
                s.setPos(from.x, from.y, from.z);
                s.shoot(Math.cos(a), dir.y, Math.sin(a), 1.15f, 1f);
                level.addFreshEntity(s);
            }
            sound(boss, SoundEvents.AMETHYST_BLOCK_CHIME, 2f, 1.4f);
        }
    }

    /** A ring of light closes on the ground where her target stands. */
    public static class Smother extends BossAttack<LuciferEntity> {
        private static final float RADIUS = 2.6f;
        private Vec3 spot = Vec3.ZERO;

        public Smother() {
            super("smother", "smother", 24, 6, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            spot = target.position();
            TelegraphMarker.circle(level(boss), spot, RADIUS, TelegraphMarker.WHITE, windup + 2);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            column(level, spot, AllParticles.WHITE_LIGHT.get(), 4, 16);
            for (LivingEntity e : foes(boss, new AABB(spot, spot).inflate(RADIUS, 3, RADIUS))) {
                if (!inCircle(e, spot, RADIUS)) continue;
                hit(boss, e, AllDamageTypes.SPELL, 8);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2), boss);
            }
            sound(boss, SoundEvents.BEACON_DEACTIVATE, 2f, 1.5f);
        }
    }

    /** She closes the distance in a rush and rakes twice with hands of light. */
    public static class Rend extends BossAttack<LuciferEntity> {
        private Vec3 dash = Vec3.ZERO;
        private final Set<UUID> struck = new HashSet<>();

        public Rend() {
            super("rend", "rend", 14, 20, 18);
        }

        @Override
        public boolean movesBoss() {
            return true;
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < 12 * 12 ? 1 : 0.2f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            float yaw = yawTo(boss.position(), target.position());
            dash = dirOf(yaw);
            TelegraphMarker.line(level(boss), boss.position(), yaw, 2f, (float) Math.min(12, boss.distanceTo(target) + 2), TelegraphMarker.WHITE, windup + 6);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t < 6) {
                boss.setDeltaMovement(dash.x * 1.0, boss.getDeltaMovement().y, dash.z * 1.0);
                level(boss).sendParticles(AllParticles.WHITE_LIGHT.get(), boss.getX(), boss.getY() + 1, boss.getZ(), 3, 0.3, 0.5, 0.3, 0.01);
            } else {
                boss.setDeltaMovement(0, boss.getDeltaMovement().y, 0);
                boss.getLookControl().setLookAt(target);
            }
            if (t == 8 || t == 15) {
                struck.clear();
                Vec3 facing = dirOf(boss.getYRot());
                for (LivingEntity e : foes(boss, boss.getBoundingBox().inflate(3.2))) {
                    Vec3 to = e.position().subtract(boss.position()).multiply(1, 0, 1);
                    if (to.length() > 3.2 || (to.length() > 0.5 && to.normalize().dot(facing) < 0.3)) continue;
                    if (struck.add(e.getUUID())) {
                        hit(boss, e, AllDamageTypes.SPELL, 7);
                        e.knockback(0.5, -facing.x, -facing.z);
                    }
                }
                sound(boss, SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 1.3f);
            }
        }
    }

    /** Light pours out of her eyes: everyone in front is blinded and weakened. */
    public static class BlindingFlash extends BossAttack<LuciferEntity> {
        private static final double RADIUS = 9;
        private float yaw;

        public BlindingFlash() {
            super("blinding_flash", "flash", 22, 4, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            yaw = yawTo(boss.position(), target.position());
            TelegraphMarker.cone(level(boss), boss.position(), yaw, (float) RADIUS, TelegraphMarker.WHITE, windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 dir = dirOf(yaw);
            for (LivingEntity e : foes(boss, boss.getBoundingBox().inflate(RADIUS, 4, RADIUS))) {
                Vec3 to = e.position().subtract(boss.position()).multiply(1, 0, 1);
                if (to.length() > RADIUS || (to.length() > 0.8 && to.normalize().dot(dir) < 0.5)) continue;
                hit(boss, e, AllDamageTypes.WHITE_LIGHT, 4);
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), boss);
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), boss);
            }
            level(boss).sendParticles(ParticleTypes.FLASH, boss.getX(), boss.getEyeY(), boss.getZ(), 1, 0, 0, 0, 0);
            sound(boss, AllSounds.LILITH_LIGHT_BURST.get(), 1.5f, 1.6f);
        }
    }
}
