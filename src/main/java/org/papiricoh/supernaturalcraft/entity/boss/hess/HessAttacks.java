package org.papiricoh.supernaturalcraft.entity.boss.hess;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.british.BossKit;
import org.papiricoh.supernaturalcraft.british.BritishFxPayload;
import org.papiricoh.supernaturalcraft.british.BritishRegistry;
import org.papiricoh.supernaturalcraft.british.agent.BritishAgentEntity;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;

import java.util.ArrayList;
import java.util.List;

import static org.papiricoh.supernaturalcraft.british.BossKit.face;
import static org.papiricoh.supernaturalcraft.british.BossKit.inLane;
import static org.papiricoh.supernaturalcraft.british.BossKit.inRing;
import static org.papiricoh.supernaturalcraft.british.BossKit.opt;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.dirOf;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.floorAt;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.hit;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.inCircle;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.level;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.randomArenaPoint;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.sound;

/**
 * Hess's spells (v0.41), numbers in {@link HessBalance}. Violet for her magic, red rings where her agents come in, green for
 * the one place the folio's curse spares. Each is shown in its windup and lands only in ACTIVE.
 */
public final class HessAttacks {

    private HessAttacks() {
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(opt(Bolt::new, 3), opt(HexBag::new, 2), opt(CallAgents::new, 1.5f));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(opt(Bolt::new, 2.5f), opt(HexBag::new, 2),
            opt(CallAgents::new, 1.2f), opt(Ward::new, 2f));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(opt(Bolt::new, 2.5f), opt(HexBag::new, 1.5f),
            opt(CallAgents::new, 1f), opt(Ward::new, 1.5f), opt(Curse::new, 2f));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            default -> P3;
        };
    }

    static HessEntity hess(LuciferEntity boss) {
        return (HessEntity) boss;
    }

    abstract static class Cued extends BossKit.Cued {
        Cued(String id, String clip, int windup, int active, int recover) {
            super(id, clip, HessBalance.cue(clip), windup, active, recover);
        }
    }

    /** A bolt from her wand down a violet lane. */
    public static class Bolt extends Cued {
        private float yaw;
        private Vec3 from = Vec3.ZERO;

        public Bolt() {
            super("bolt", "wand_cast", HessBalance.BOLT_WINDUP, 2, 12);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            yaw = face(boss, target.position());
            from = boss.position();
            TelegraphMarker.line(level(boss), from, yaw, HessBalance.BOLT_WIDTH, HessBalance.BOLT_LENGTH, TelegraphMarker.VIOLET, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inLane(boss, from, dirOf(yaw), HessBalance.BOLT_LENGTH, HessBalance.BOLT_WIDTH / 2)) {
                hit(boss, e, AllDamageTypes.SPELL, HessBalance.BOLT_DAMAGE);
            }
            BritishFxPayload.send(level(boss), boss.getId(), BritishFxPayload.LANE, Math.round(yaw),
                    Math.round(HessBalance.BOLT_LENGTH * 10) + BritishFxPayload.HEX * 10000, from, 8);
            sound(boss, BritishRegistry.sound("hess.cast"), 1.6f, 1f);
        }
    }

    /** A hex bag tossed under a hunter: a violet circle. */
    public static class HexBag extends Cued {
        private Vec3 at = Vec3.ZERO;

        public HexBag() {
            super("hex_bag", "hex_throw", HessBalance.HEX_WINDUP, 2, 14);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            at = floorAt(boss, target.position());
            TelegraphMarker.circle(level(boss), at, HessBalance.HEX_RADIUS, TelegraphMarker.VIOLET, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inRing(boss, at, HessBalance.HEX_RADIUS)) {
                hit(boss, e, AllDamageTypes.SPELL, HessBalance.HEX_DAMAGE);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, HessBalance.HEX_TICKS, 1));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, HessBalance.HEX_TICKS, 0));
            }
            BritishFxPayload.send(level(boss), boss.getId(), BritishFxPayload.CLOUD, Math.round(HessBalance.HEX_RADIUS * 10), BritishFxPayload.HEX, at, 30);
            sound(boss, BritishRegistry.sound("hess.hex"), 1.6f, 1f);
        }
    }

    /** Two fingers raised: her agents come in at the doors (red rings). */
    public static class CallAgents extends Cued {
        private final List<Vec3> spots = new ArrayList<>();

        public CallAgents() {
            super("call_agents", "summon_agents", HessBalance.SUMMON_WINDUP, 2, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return hess(boss).agents() < HessBalance.MAX_AGENTS ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            spots.clear();
            for (Vec3 s : boss.minionSpots(hess(boss).agentSpots())) {
                spots.add(s);
                TelegraphMarker.ring(level(boss), s, 1.2f, TelegraphMarker.RED, windupLength(boss) + 4);
                if (spots.size() >= boss.minionCount(HessBalance.AGENTS_PER_CALL)) break;
            }
            sound(boss, BritishRegistry.sound("hess.command"), 1.6f, 1f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            int room = HessBalance.MAX_AGENTS - hess(boss).agents();
            for (Vec3 s : spots) {
                if (room-- <= 0) break;
                BritishAgentEntity a = hess(boss).callAgent(level, s);
                if (a != null) a.setTarget(target);
            }
        }
    }

    /** A ward drawn round her (violet): whoever stands in it is thrown out; while it holds she takes less. */
    public static class Ward extends Cued {
        public Ward() {
            super("ward", "ward_raise", HessBalance.WARD_WINDUP, 2, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return hess(boss).warded() ? 0 : 1;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            TelegraphMarker.circle(level(boss), boss.position(), HessBalance.WARD_RADIUS, TelegraphMarker.VIOLET, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inRing(boss, boss.position(), HessBalance.WARD_RADIUS)) {
                hit(boss, e, AllDamageTypes.SPELL, HessBalance.WARD_DAMAGE);
                Vec3 out = e.position().subtract(boss.position()).multiply(1, 0, 1);
                if (out.lengthSqr() < 0.01) out = new Vec3(1, 0, 0);
                out = out.normalize().scale(HessBalance.WARD_PUSH);
                e.setDeltaMovement(out.x, 0.45, out.z);
                e.hurtMarked = true;
            }
            hess(boss).raiseWard(HessBalance.WARD_TICKS);
            sound(boss, BritishRegistry.sound("hess.ward"), 1.8f, 1f);
        }
    }

    /** The folio read aloud: a curse over the room (a wide violet ring), all but a small green circle. */
    public static class Curse extends Cued {
        private Vec3 safe = Vec3.ZERO;

        public Curse() {
            super("curse", "folio_read", HessBalance.CURSE_WINDUP, 2, 20);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            Vec3 pick = boss.position();
            for (int i = 0; i < 8; i++) {
                pick = randomArenaPoint(boss, 0.6);
                if (pick.distanceTo(boss.position()) >= HessBalance.SAFE_MIN_DISTANCE) break;
            }
            safe = pick;
            int life = windupLength(boss) + 4;
            TelegraphMarker.ring(level(boss), boss.position(), HessBalance.CURSE_RADIUS, TelegraphMarker.VIOLET, life);
            TelegraphMarker.circle(level(boss), safe, HessBalance.SAFE_RADIUS, TelegraphMarker.SAFE, life);
            hess(boss).tell("message.supernaturalcraft.hess.folio");
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inRing(boss, boss.position(), HessBalance.CURSE_RADIUS)) {
                if (inCircle(e, safe, HessBalance.SAFE_RADIUS)) continue;
                hit(boss, e, AllDamageTypes.SPELL, HessBalance.CURSE_DAMAGE);
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, HessBalance.CURSE_WEAKNESS, 1));
            }
            level(boss).sendParticles(ParticleTypes.ENCHANT, boss.getX(), boss.getY() + 1.5, boss.getZ(), 120, 6, 1, 6, 1.0);
            BritishFxPayload.send(level(boss), boss.getId(), BritishFxPayload.RING, Math.round(HessBalance.CURSE_RADIUS * 10), BritishFxPayload.HEX, boss.position(), 20);
            sound(boss, BritishRegistry.sound("hess.cast"), 2.2f, 0.7f);
        }
    }
}
