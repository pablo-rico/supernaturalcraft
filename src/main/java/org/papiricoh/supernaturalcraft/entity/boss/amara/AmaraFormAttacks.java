package org.papiricoh.supernaturalcraft.entity.boss.amara;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.darkness.DarknessRegistry;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.network.AmaraFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

import static org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAttacks.floorAt;
import static org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAttacks.flat;
import static org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAttacks.hit;
import static org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAttacks.level;
import static org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAttacks.victims;
import static org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAttacks.yawTo;

/**
 * v0.42: what she does in her later bodies (phases II-VI), the girl, the woman and the Void. Same rules as {@link AmaraAttacks}:
 * every attack warns on the ground in its windup (violet her darkness, green a safe place, gold the sun) and hurts only once it is
 * active. Numbers are her base damage before {@code attackDamageMultiplier}.
 */
public final class AmaraFormAttacks {

    private AmaraFormAttacks() {
    }

    private static void sound(AmaraEntity boss, String id, float volume, float pitch) {
        boss.level().playSound(null, boss.blockPosition(), DarknessRegistry.amara(id), net.minecraft.sounds.SoundSource.HOSTILE, volume, pitch);
    }

    private static Vec3 chest(AmaraEntity boss) {
        return boss.position().add(0, boss.getBbHeight() * 0.7, 0);
    }

    // --- phase II: the girl ----------------------------------------------------------------------------------------------

    /** She sinks into her own shadow and comes up behind her target with a slap of darkness. */
    public static class ShadowStep extends BossAttack<AmaraEntity> {
        public static final float DAMAGE = 9, RADIUS = 1.6f;
        Vec3 behind;

        public ShadowStep() {
            super("shadow_step", "giggle_step", 14, 4, 18);
        }

        @Override
        public boolean movesBoss() {
            return true;
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            Vec3 back = target.getLookAngle().multiply(1, 0, 1);
            if (back.lengthSqr() < 0.01) back = new Vec3(0, 0, 1);
            behind = floorAt(boss, target.position().subtract(back.normalize().scale(1.6)));
            TelegraphMarker.circle(level(boss), behind, RADIUS, TelegraphMarker.VIOLET, windup + active);
            AmaraFx.send(boss, AmaraFxPayload.SHADOW_STEP, boss.position(), yawTo(boss.position(), behind), (float) flat(boss.position(), behind), windup);
            sound(boss, "giggle", 2f, 1.1f + boss.getRandom().nextFloat() * 0.2f);
        }

        @Override
        public void onActive(AmaraEntity boss, LivingEntity target) {
            boss.glideTo(behind, true);
            for (LivingEntity e : victims(boss, new AABB(behind, behind).inflate(RADIUS, 2, RADIUS))) {
                if (flat(e.position(), behind) <= RADIUS + 0.6) hit(boss, e, DAMAGE);
            }
        }
    }

    /**
     * Hide and seek: she curls into smoke and two of her shades take her shape somewhere in the town. Whoever finds the real one
     * (strikes her) startles her out: she is open for a while. Leave her be and she pops up with a ring of darkness ("found you").
     */
    public static class HideAndSeek extends BossAttack<AmaraEntity> {
        public static final float RING = 7f, DAMAGE = 12;
        public static final int DECOYS = 2;
        Vec3 spot;

        public HideAndSeek() {
            super("hide_and_seek", "hide", 16, 120, 20);
        }

        @Override
        public float weight(AmaraEntity boss, LivingEntity target) {
            return boss.hidden() ? 0f : 1f;
        }

        @Override
        public boolean movesBoss() {
            return true;
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            sound(boss, "giggle", 3f, 1.0f);
            AmaraFx.send(boss, AmaraFxPayload.HIDE, boss.position(), 0, 1.5f, windup + active);
        }

        @Override
        public void onActive(AmaraEntity boss, LivingEntity target) {
            var arena = boss.arena();
            Vec3 c = arena != null ? arena.centerVec() : boss.position();
            double a = boss.getRandom().nextDouble() * Math.PI * 2;
            spot = floorAt(boss, c.add(Math.cos(a) * 12, 0, Math.sin(a) * 12));
            boss.glideTo(spot, true);
            boss.setHidden(true);
            ServerLevel level = level(boss);
            for (int i = 0; i < boss.minionCount(DECOYS); i++) {
                double b = a + (i + 1) * Math.PI * 2 / (DECOYS + 1);
                Vec3 at = floorAt(boss, c.add(Math.cos(b) * 12, 0, Math.sin(b) * 12));
                AmaraShade decoy = AllEntities.AMARA_SHADE.get().create(level);
                if (decoy == null) continue;
                decoy.moveTo(at.x, at.y, at.z, 0, 0);
                decoy.makeDecoy();
                level.addFreshEntity(decoy);
                AmaraFx.send(boss, AmaraFxPayload.HIDE, at, 0, 1.2f, active);
            }
        }

        @Override
        public boolean endEarly(AmaraEntity boss, LivingEntity target) {
            return !boss.hidden();
        }

        @Override
        public void tickActive(AmaraEntity boss, LivingEntity target, int t) {
            if (t == active - 20) TelegraphMarker.ring(level(boss), floorAt(boss, boss.position()), RING, TelegraphMarker.VIOLET, 20);
            if (t == active - 1 && boss.hidden()) {
                boss.setHidden(false);
                AmaraFx.send(boss, AmaraFxPayload.FOUND, boss.position(), 0, RING, 20);
                boss.voice(2);
                sound(boss, "child_scream", 2.5f, 1.2f);
                for (LivingEntity e : victims(boss, boss.getBoundingBox().inflate(RING, 3, RING))) {
                    if (flat(e.position(), boss.position()) <= RING) hit(boss, e, DAMAGE);
                }
            }
        }

        @Override
        public void onEnd(AmaraEntity boss) {
            boss.setHidden(false);
            for (AmaraShade s : boss.level().getEntitiesOfClass(AmaraShade.class, boss.getBoundingBox().inflate(48), AmaraShade::decoy)) s.unmake();
        }
    }

    /** She throws a ball of darkness underhand, as a child throws a ball; it bursts where it lands. */
    public static class Toss extends BossAttack<AmaraEntity> {
        public static final float RADIUS = 2.5f, DAMAGE = 8;
        Vec3 landing;

        public Toss() {
            super("toss", "toss", 16, 14, 16);
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            landing = floorAt(boss, target.position().add(target.getDeltaMovement().scale(10)));
            TelegraphMarker.circle(level(boss), landing, RADIUS, TelegraphMarker.VIOLET, windup + active);
        }

        @Override
        public void tickActive(AmaraEntity boss, LivingEntity target, int t) {
            ServerLevel level = level(boss);
            double k = (t + 1) / (double) active;
            Vec3 from = chest(boss);
            Vec3 at = from.lerp(landing, k).add(0, Math.sin(k * Math.PI) * 4, 0);
            level.sendParticles(AllParticles.VOID_MOTE.get(), at.x, at.y, at.z, 4, 0.15, 0.15, 0.15, 0.01);
            if (t == active - 1) {
                level.sendParticles(ParticleTypes.SQUID_INK, landing.x, landing.y + 0.3, landing.z, 30, RADIUS * 0.4, 0.3, RADIUS * 0.4, 0.05);
                for (LivingEntity e : victims(boss, new AABB(landing, landing).inflate(RADIUS, 2, RADIUS))) {
                    if (flat(e.position(), landing) <= RADIUS) {
                        hit(boss, e, DAMAGE);
                        e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
                    }
                }
            }
        }
    }

    /** She stamps her foot: a ring of darkness rushes outward (jump it). */
    public static class Tantrum extends BossAttack<AmaraEntity> {
        public static final float RADIUS = 10, DAMAGE = 10;

        public Tantrum() {
            super("tantrum", "tantrum", 24, 16, 24);
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            TelegraphMarker.ring(level(boss), floorAt(boss, boss.position()), RADIUS, TelegraphMarker.VIOLET, windup + active);
            sound(boss, "child_scream", 3f, 1.0f);
            boss.voice(3);
        }

        @Override
        public void onActive(AmaraEntity boss, LivingEntity target) {
            AmaraFx.send(boss, AmaraFxPayload.RING_OUT, boss.position(), 0, RADIUS, active);
        }

        @Override
        public void tickActive(AmaraEntity boss, LivingEntity target, int t) {
            double front = RADIUS * (t + 1) / active;
            for (LivingEntity e : victims(boss, boss.getBoundingBox().inflate(RADIUS, 2, RADIUS))) {
                double d = flat(e.position(), boss.position());
                if (Math.abs(d - front) < 1.0 && e.onGround()) hit(boss, e, DAMAGE);
            }
        }
    }

    // --- phase III: the woman --------------------------------------------------------------------------------------------

    /** Three blows at arm's length, each a step closer. */
    public static class Combo extends BossAttack<AmaraEntity> {
        public static final float DAMAGE = 7, REACH = 3.2f;

        public Combo() {
            super("combo", "combo", 10, 30, 22);
        }

        @Override
        public float weight(AmaraEntity boss, LivingEntity target) {
            return boss.distanceTo(target) < 8 ? 3f : 0.3f;
        }

        @Override
        public boolean movesBoss() {
            return true;
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            TelegraphMarker.cone(level(boss), floorAt(boss, boss.position()), yawTo(boss.position(), target.position()), REACH + 1,
                    TelegraphMarker.VIOLET, windup + active);
        }

        @Override
        public void tickActive(AmaraEntity boss, LivingEntity target, int t) {
            if (t % 10 != 0) return;
            Vec3 to = target.position().subtract(boss.position()).multiply(1, 0, 1);
            if (to.length() > 1.2) boss.glideTo(boss.position().add(to.normalize().scale(Math.min(2, to.length() - 1.2))), false);
            sound(boss, "strike", 1.5f, 0.9f + t * 0.01f);
            for (LivingEntity e : victims(boss, boss.getBoundingBox().inflate(REACH, 2, REACH))) {
                if (boss.distanceTo(e) <= REACH) {
                    hit(boss, e, DAMAGE);
                    if (e instanceof ServerPlayer p) AmaraFx.send(boss, AmaraFxPayload.CHROMA_HIT, p.position(), 0, 0.6f, 10);
                }
            }
        }
    }

    /**
     * Telekinesis: the street's debris and the hunters near her rise into the air, hang there, and are flung away from her.
     */
    public static class Telekinesis extends BossAttack<AmaraEntity> {
        public static final float RADIUS = 9, DAMAGE = 11;

        public Telekinesis() {
            super("telekinesis", "tk_lift", 20, 40, 26);
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            TelegraphMarker.circle(level(boss), floorAt(boss, boss.position()), RADIUS, TelegraphMarker.VIOLET, windup + 20);
            AmaraFx.send(boss, AmaraFxPayload.TELEKINESIS, boss.position(), yawTo(boss.position(), target.position()), RADIUS, windup + active);
            sound(boss, "tk", 3f, 0.8f);
            boss.voice(6);
        }

        @Override
        public void tickActive(AmaraEntity boss, LivingEntity target, int t) {
            for (LivingEntity e : victims(boss, boss.getBoundingBox().inflate(RADIUS, 4, RADIUS))) {
                if (flat(e.position(), boss.position()) > RADIUS) continue;
                if (t < 20) {
                    e.setDeltaMovement(e.getDeltaMovement().x * 0.5, t < 8 ? 0.35 : 0.02, e.getDeltaMovement().z * 0.5);
                } else if (t == 20) {
                    boss.triggerAnim("action", "tk_throw");
                    Vec3 away = e.position().subtract(boss.position()).multiply(1, 0, 1);
                    if (away.lengthSqr() < 0.01) away = new Vec3(1, 0, 0);
                    away = away.normalize().scale(2.2);
                    e.setDeltaMovement(away.x, 0.4, away.z);
                    hit(boss, e, DAMAGE);
                }
                e.fallDistance = 0;
                e.hurtMarked = true;
            }
        }
    }

    /**
     * She draws the soul out of the hunter nearest her: a ribbon of light from their chest to her mouth that hurts them and heals
     * her. Strong light (or breaking her line of sight) snaps it; so does hurting her while she feeds.
     */
    public static class SoulDrain extends BossAttack<AmaraEntity> {
        public static final float RANGE = 14, DAMAGE_PER_PULSE = 2.5f;
        /** What each pulse gives back to her, as a share of her vanilla max health. */
        public static final float HEAL_SHARE = 0.002f;
        @Nullable LivingEntity prey;
        int broken;

        public SoulDrain() {
            super("soul_drain", "drain", 20, 80, 30);
        }

        @Override
        public float weight(AmaraEntity boss, LivingEntity target) {
            return boss.distanceTo(target) < RANGE ? 1.5f : 0f;
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            prey = target;
            TelegraphMarker.line(level(boss), floorAt(boss, boss.position()), yawTo(boss.position(), target.position()), 1f,
                    boss.distanceTo(target), TelegraphMarker.VIOLET, windup);
            sound(boss, "drain", 3f, 0.8f);
        }

        @Override
        public void onActive(AmaraEntity boss, LivingEntity target) {
            if (prey != null) AmaraFx.send(boss, AmaraFxPayload.SOUL_DRAIN, prey.position(), 0, 0, active);
            boss.voice(5);
            broken = boss.hurtCounter();
        }

        @Override
        public boolean endEarly(AmaraEntity boss, LivingEntity target) {
            if (prey == null || !prey.isAlive() || boss.distanceTo(prey) > RANGE + 2 || !boss.hasLineOfSight(prey)) return true;
            if (AmaraAttacks.blockLight(level(boss), prey.getEyePosition()) >= AmaraBalance.LANCE_PROOF) return true;
            return boss.hurtCounter() - broken >= 3;
        }

        @Override
        public void tickActive(AmaraEntity boss, LivingEntity target, int t) {
            if (prey == null || t % 10 != 0) return;
            hit(boss, prey, DAMAGE_PER_PULSE);
            boss.healShare(HEAL_SHARE);
            Vec3 a = prey.getEyePosition().subtract(0, 0.4, 0), b = chest(boss).add(0, 0.4, 0);
            for (int i = 0; i < 6; i++) {
                Vec3 p = a.lerp(b, i / 6.0);
                level(boss).sendParticles(ParticleTypes.SOUL, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
            }
        }
    }

    /** A shove of bent air in a cone ahead of her. */
    public static class Push extends BossAttack<AmaraEntity> {
        public static final float RADIUS = 7, DAMAGE = 8;
        float yaw;

        public Push() {
            super("push", "push", 14, 4, 18);
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            yaw = yawTo(boss.position(), target.position());
            TelegraphMarker.cone(level(boss), floorAt(boss, boss.position()), yaw, RADIUS, TelegraphMarker.VIOLET, windup + active);
        }

        @Override
        public void onActive(AmaraEntity boss, LivingEntity target) {
            Vec3 dir = new Vec3(-Mth.sin(yaw * Mth.DEG_TO_RAD), 0, Mth.cos(yaw * Mth.DEG_TO_RAD));
            for (LivingEntity e : victims(boss, boss.getBoundingBox().inflate(RADIUS, 3, RADIUS))) {
                Vec3 to = e.position().subtract(boss.position()).multiply(1, 0, 1);
                if (to.length() > RADIUS || to.lengthSqr() < 0.01 || to.normalize().dot(dir) < Math.cos(Math.toRadians(35))) continue;
                hit(boss, e, DAMAGE);
                e.setDeltaMovement(dir.x * 1.8, 0.5, dir.z * 1.8);
                e.hurtMarked = true;
            }
        }
    }

    // --- phase IV: the siblings ------------------------------------------------------------------------------------------

    /**
     * She closes her fist on her brother: smoke chains run from her to Chuck and tear at his light. Hurting her while she holds
     * them ({@link #BREAK_HITS} blows) breaks her grip.
     */
    public static class WoundGod extends BossAttack<AmaraEntity> {
        public static final int BREAK_HITS = 4;
        /** How much of Chuck's light each pulse tears away. */
        public static final float TEAR = 0.04f;
        int hitsAtStart;

        public WoundGod() {
            super("wound_god", "wound", 20, 100, 30);
        }

        @Override
        public float weight(AmaraEntity boss, LivingEntity target) {
            return boss.chuck() != null && boss.chuckLight() > 0 ? 2.5f : 0f;
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            var chuck = boss.chuck();
            if (chuck == null) return;
            TelegraphMarker.line(level(boss), floorAt(boss, boss.position()), yawTo(boss.position(), chuck.position()), 1.2f,
                    boss.distanceTo(chuck), TelegraphMarker.GOLD, windup + active);
            sound(boss, "wound", 3f, 0.7f);
        }

        @Override
        public void onActive(AmaraEntity boss, LivingEntity target) {
            var chuck = boss.chuck();
            if (chuck != null) AmaraFx.send(boss, AmaraFxPayload.CHUCK_WOUND, chuck.position(), 0, 0, active);
            boss.voice(9);
            hitsAtStart = boss.hurtCounter();
        }

        @Override
        public boolean endEarly(AmaraEntity boss, LivingEntity target) {
            return boss.chuck() == null || boss.hurtCounter() - hitsAtStart >= BREAK_HITS;
        }

        @Override
        public void tickActive(AmaraEntity boss, LivingEntity target, int t) {
            if (t % 10 == 0) boss.tearChuck(TEAR);
        }

        @Override
        public void onEnd(AmaraEntity boss) {
            if (boss.hurtCounter() - hitsAtStart >= BREAK_HITS) boss.chainsBroken();
        }
    }

    /** A wall of smoke sweeps across the street: it burns whoever is caught in it, and stands a while. */
    public static class SmokeWall extends BossAttack<AmaraEntity> {
        public static final float LENGTH = 24, HALF_WIDTH = 1.2f, DAMAGE = 9;
        Vec3 through;
        float yaw;

        public SmokeWall() {
            super("smoke_wall", "wall", 22, 80, 20);
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            through = floorAt(boss, target.position());
            yaw = yawTo(boss.position(), target.position()) + 90f;
            Vec3 dir = new Vec3(-Mth.sin(yaw * Mth.DEG_TO_RAD), 0, Mth.cos(yaw * Mth.DEG_TO_RAD));
            TelegraphMarker.line(level(boss), through.subtract(dir.scale(LENGTH / 2)), yaw, HALF_WIDTH * 2, LENGTH, TelegraphMarker.VIOLET, windup + active);
            sound(boss, "wall", 3f, 0.8f);
        }

        @Override
        public void onActive(AmaraEntity boss, LivingEntity target) {
            AmaraFx.send(boss, AmaraFxPayload.SMOKE_WALL, through, yaw, LENGTH, active);
        }

        @Override
        public void tickActive(AmaraEntity boss, LivingEntity target, int t) {
            if (t % 10 != 0) return;
            Vec3 dir = new Vec3(-Mth.sin(yaw * Mth.DEG_TO_RAD), 0, Mth.cos(yaw * Mth.DEG_TO_RAD));
            for (LivingEntity e : victims(boss, new AABB(through, through).inflate(LENGTH / 2, 3, LENGTH / 2))) {
                Vec3 rel = e.position().subtract(through).multiply(1, 0, 1);
                double along = rel.dot(dir), across = rel.subtract(dir.scale(along)).length();
                if (Math.abs(along) <= LENGTH / 2 && across <= HALF_WIDTH + 0.4) hit(boss, e, DAMAGE);
            }
        }
    }

    // --- phase V: the void -----------------------------------------------------------------------------------------------

    /** Everything is drawn up toward the black sun: hold on, or be lifted off the islands. */
    public static class BlackSunPull extends BossAttack<AmaraEntity> {
        public static final float RADIUS = 30;

        public BlackSunPull() {
            super("black_sun_pull", "pull", 30, 60, 30);
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            AmaraFx.send(boss, AmaraFxPayload.GRAVITY, boss.position(), 0, RADIUS, windup + active);
            sound(boss, "pull", 4f, 0.6f);
            boss.voice(13);
            var arena = boss.arena();
            if (arena != null) {
                // The heart island is where nothing pulls you off.
                TelegraphMarker.circle(level(boss), floorAt(boss, arena.centerVec()), 4f, TelegraphMarker.SAFE, windup + active);
            }
        }

        @Override
        public void tickActive(AmaraEntity boss, LivingEntity target, int t) {
            var arena = boss.arena();
            Vec3 heart = arena != null ? arena.centerVec() : boss.position();
            for (LivingEntity e : victims(boss, boss.getBoundingBox().inflate(RADIUS, 20, RADIUS))) {
                if (flat(e.position(), heart) < 4) continue;
                Vec3 up = boss.position().add(0, 2, 0).subtract(e.position()).normalize();
                e.setDeltaMovement(e.getDeltaMovement().add(up.x * 0.03, 0.06, up.z * 0.03));
                e.fallDistance = 0;
                e.hurtMarked = true;
            }
        }
    }

    /** Stars fall on the islands (gold: the light she cannot stand turned against them). */
    public static class Starfall extends BossAttack<AmaraEntity> {
        public static final float RADIUS = 2.2f, DAMAGE = 10;
        public static final int STARS = 7;
        final java.util.List<Vec3> spots = new java.util.ArrayList<>();

        public Starfall() {
            super("starfall", "starfall", 30, 20, 26);
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            spots.clear();
            spots.add(floorAt(boss, target.position()));
            for (int i = 1; i < STARS; i++) {
                double a = boss.getRandom().nextDouble() * Math.PI * 2, d = 3 + boss.getRandom().nextDouble() * 10;
                spots.add(floorAt(boss, target.position().add(Math.cos(a) * d, 0, Math.sin(a) * d)));
            }
            for (Vec3 s : spots) {
                TelegraphMarker.circle(level(boss), s, RADIUS, TelegraphMarker.GOLD, windup + active);
                AmaraFx.send(boss, AmaraFxPayload.STARFALL, s, 0, RADIUS, windup + active);
            }
            sound(boss, "starfall", 3f, 0.8f);
            if (boss.getRandom().nextInt(3) == 0) boss.voice(14);
        }

        @Override
        public void tickActive(AmaraEntity boss, LivingEntity target, int t) {
            if (t != active - 1) return;
            for (Vec3 s : spots) {
                level(boss).sendParticles(ParticleTypes.FLASH, s.x, s.y + 0.4, s.z, 1, 0, 0, 0, 0);
                for (LivingEntity e : victims(boss, new AABB(s, s).inflate(RADIUS, 2, RADIUS))) {
                    if (flat(e.position(), s) <= RADIUS) hit(boss, e, DAMAGE);
                }
            }
        }
    }

    /** She points at an island and it sinks a step (and anyone on it is shaken). */
    public static class SinkIsland extends BossAttack<AmaraEntity> {
        int island = -1;

        public SinkIsland() {
            super("sink_island", "crumble", 26, 4, 20);
        }

        @Override
        public float weight(AmaraEntity boss, LivingEntity target) {
            return boss.ground() != null && boss.ground().sinkableUnder(target) >= 0 ? 2f : 0.2f;
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            var ground = boss.ground();
            island = ground != null ? ground.sinkableUnder(target) : -1;
            if (ground != null && island >= 0) {
                Vec3 c = ground.islandCentre(island);
                var is = org.papiricoh.supernaturalcraft.entity.boss.amara.arena.VoidIslands.ISLANDS.get(island);
                TelegraphMarker.circle(level(boss), floorAt(boss, c), is.radius() + 0.5f, TelegraphMarker.VIOLET, windup + active);
            }
            sound(boss, "island", 3f, 0.7f);
        }

        @Override
        public void onActive(AmaraEntity boss, LivingEntity target) {
            var ground = boss.ground();
            if (ground != null && island >= 0) ground.sink(level(boss), boss.arena(), island);
        }
    }

    // --- phase VI: the dying sun ------------------------------------------------------------------------------------------

    /** The sun gutters: a pulse of dark that hurts whoever stands outside the light (her brother's, or a lit well's). */
    public static class Gutter extends BossAttack<AmaraEntity> {
        public static final float DAMAGE = 12;

        public Gutter() {
            super("gutter", "countdown", 40, 4, 30);
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            var chuck = boss.chuck();
            if (chuck != null) TelegraphMarker.circle(level(boss), floorAt(boss, chuck.position()), 4f, TelegraphMarker.SAFE, windup + active);
            for (var w : boss.wells()) {
                TelegraphMarker.circle(level(boss), new Vec3(w.getX() + 0.5, w.getY(), w.getZ() + 0.5), 3f, TelegraphMarker.SAFE, windup + active);
            }
            AmaraFx.send(boss, AmaraFxPayload.SUN_DIE, boss.position(), 0, boss.countdown(), windup);
            sound(boss, "countdown", 4f, 0.6f);
        }

        @Override
        public void onActive(AmaraEntity boss, LivingEntity target) {
            var chuck = boss.chuck();
            for (LivingEntity e : victims(boss, boss.getBoundingBox().inflate(40, 20, 40))) {
                boolean safe = (chuck != null && flat(e.position(), chuck.position()) <= 4.5) || boss.nearLitWell(e.position(), 3.5);
                if (!safe) hit(boss, e, DAMAGE);
            }
        }
    }
}
