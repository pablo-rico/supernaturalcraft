package org.papiricoh.supernaturalcraft.client.abaddon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxParticles;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonBalance;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonEntity;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonHeraldEntity;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonKnightEntity;
import org.papiricoh.supernaturalcraft.network.AbaddonFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.util.ClientPresence;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Abaddon's moments on the client (v0.21): each {@link AbaddonFxPayload} kind played out — the title cards ({@link AbaddonOverlay})
 * and the fight in the world ({@link AbaddonWorldFx}: the herald's mark and its strike, the hellfire sweep, the retinue rising, the
 * smoke out of her vessel and into a knight, each way of casting her out, the pin, the Brand, the devil's trap holding her and
 * breaking, the cleaver's wave, her death).
 *
 * <p>What lasts is read off the synced flags instead: a possessed knight leaks black smoke at its joints (here) and burns crimson
 * ({@link AbaddonRenderers}). Her knights' last places are remembered while she is about, since they are gone from the client
 * before her death's card arrives (they crumble to ash where they stood).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ClientAbaddonFx {

    /** Where a knight or herald last stood: feet, body yaw, height, and the client tick it was seen. */
    record Seen(Vec3 pos, float yaw, float height, boolean herald, long tick) {
    }

    private static final Map<Integer, Seen> RETINUE = new HashMap<>();
    /** Retinue rising out of the floor: entity id → the client tick it began. */
    private static final Map<Integer, Long> RISING = new HashMap<>();
    /** Abaddon dying: entity id → the client tick her death began. */
    private static final Map<Integer, Long> DYING = new HashMap<>();
    /** Possession beginning: knight id → the client tick the smoke went in (the visor's flare). */
    private static final Map<Integer, Long> TAKEN = new HashMap<>();
    /** Abaddon reforming out of smoke after leaving a knight: entity id → the client tick it began. */
    private static final Map<Integer, Long> REFORM = new HashMap<>();
    /** Ticks a summoned knight takes to rise out of the floor. */
    static final int RISE_TICKS = 18;
    private static long ticks;

    private ClientAbaddonFx() {
    }

    public static void handle(AbaddonFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        switch (p.kind()) {
            case AbaddonFxPayload.TITLE -> {
                boolean fall = p.arg2() == 1;
                AbaddonOverlay.title(p.arg(), fall, p.duration());
                if (fall) death(p.entity(), p.point());
                else if (p.arg() >= AbaddonBalance.PHASES && mc.level.getEntity(p.entity()) instanceof AbaddonEntity a) {
                    // The last phase's camera opens on her palm: the Brand slides out of it as the card comes.
                    a.triggerAnim("action", "brand");
                }
            }
            case AbaddonFxPayload.HERALD_MARK -> AbaddonWorldFx.heraldMark(p.entity(), p.arg(), p.arg2() / 10f, p.point(), p.duration());
            case AbaddonFxPayload.MARK_STRIKE -> AbaddonWorldFx.markStrike(p.point(), p.arg() / 10f, p.arg2() == 1, p.duration());
            case AbaddonFxPayload.SWEEP -> AbaddonWorldFx.sweep(p.point(), p.arg(), p.arg2(), p.duration());
            case AbaddonFxPayload.SUMMON -> {
                RISING.put(p.arg(), ticks);
                AbaddonWorldFx.summon(p.arg(), p.arg2() == 1, p.point(), p.duration());
            }
            case AbaddonFxPayload.SMOKE_OUT -> AbaddonWorldFx.smokeOut(p.entity(), p.arg(), p.point(), p.duration());
            case AbaddonFxPayload.POSSESS -> {
                if (p.arg2() == 1) {
                    TAKEN.put(p.arg(), ticks);
                    AbaddonWorldFx.possess(p.arg(), p.point(), p.duration());
                } else {
                    AbaddonWorldFx.release(p.entity(), p.arg(), knightAt(p.arg(), p.point()), p.point());
                }
            }
            case AbaddonFxPayload.EXPEL -> AbaddonWorldFx.expel(p.entity(), p.arg(), knightAt(p.arg(), p.point()), p.arg2(), p.point(),
                    p.duration());
            case AbaddonFxPayload.PIN -> AbaddonWorldFx.pin(p.entity(), p.arg(), p.point(), p.duration());
            case AbaddonFxPayload.BRAND -> AbaddonWorldFx.brand(p.entity(), p.arg(), p.arg2(), p.point(), p.duration());
            case AbaddonFxPayload.TRAPPED -> AbaddonWorldFx.trapped(p.entity(), p.point(), p.duration());
            case AbaddonFxPayload.TRAP_FLARE -> AbaddonWorldFx.trapFlare(p.entity(), p.point(), p.duration());
            case AbaddonFxPayload.EXPELLED_BY -> AbaddonWorldFx.expelledBy(p.entity(), p.arg2(), p.point());
            case AbaddonFxPayload.CLEAVE -> AbaddonWorldFx.cleave(p.entity(), p.arg(), p.arg2(), p.point(), p.duration());
            default -> {
            }
        }
    }

    /** Her death: the knights crumble where they last stood, she burns, goes to smoke and down; the Book is left glowing. */
    private static void death(int abaddon, Vec3 at) {
        DYING.put(abaddon, ticks);
        Vec3 centre = Minecraft.getInstance().level.getEntity(abaddon) instanceof AbaddonEntity a ? a.position() : at;
        for (Map.Entry<Integer, Seen> e : RETINUE.entrySet()) {
            Seen s = e.getValue();
            // Only those standing until now (her death takes them with her; one fallen earlier is already gone).
            if (ticks - s.tick > 4 || s.pos.distanceToSqr(centre) > 64 * 64) continue;
            AbaddonWorldFx.crumble(s.pos, s.yaw, s.height, s.herald);
        }
        RETINUE.clear();
        AbaddonWorldFx.death(abaddon, centre, AbaddonBalance.DEATH_TICKS);
    }

    /** Where knight {@code id} is (or last stood), else {@code fallback}. */
    static Vec3 knightAt(int id, Vec3 fallback) {
        Minecraft mc = Minecraft.getInstance();
        Entity e = mc.level == null || id < 0 ? null : mc.level.getEntity(id);
        if (e != null && !e.isRemoved()) return e.position();
        Seen s = RETINUE.get(id);
        return s != null ? s.pos : fallback;
    }

    /** How far a summoned knight has risen out of the floor (0 under it, 1 standing). */
    public static float risen(int id, float partial) {
        Long at = RISING.get(id);
        if (at == null) return 1;
        return Mth.clamp((ticks - at + partial) / RISE_TICKS, 0, 1);
    }

    /** Ticks since Abaddon {@code id} began to die (-1 if she is not dying). */
    public static float dying(int id, float partial) {
        Long at = DYING.get(id);
        return at == null ? -1 : ticks - at + partial;
    }

    static void reformed(int abaddon) {
        REFORM.put(abaddon, ticks);
    }

    /** Ticks since Abaddon {@code id} began to reform out of smoke (-1 if not lately). */
    public static float reforming(int id, float partial) {
        Long at = REFORM.get(id);
        return at == null ? -1 : ticks - at + partial;
    }

    /** Ticks since the smoke went into knight {@code id} (-1 if not lately). */
    public static float taken(int id, float partial) {
        Long at = TAKEN.get(id);
        return at == null ? -1 : ticks - at + partial;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        AbaddonOverlay.tick();
        if (mc.isPaused()) return;
        ticks++;
        RISING.values().removeIf(t -> ticks - t > RISE_TICKS + 2);
        TAKEN.values().removeIf(t -> ticks - t > 60);
        REFORM.values().removeIf(t -> ticks - t > 60);
        DYING.values().removeIf(t -> ticks - t > AbaddonBalance.DEATH_TICKS + 40);
        for (AbaddonEntity a : ClientPresence.live(AbaddonEntity.class, level)) {
            if (a.distanceToSqr(mc.player) > 96 * 96) continue;
            for (Entity e : level.getEntities(a, new AABB(a.blockPosition()).inflate(48, 16, 48),
                    x -> x instanceof AbaddonKnightEntity || x instanceof AbaddonHeraldEntity)) {
                LivingEntity l = (LivingEntity) e;
                if (!l.isAlive()) continue;
                RETINUE.put(e.getId(), new Seen(e.position(), l.yBodyRot, e.getBbHeight(), e instanceof AbaddonHeraldEntity, ticks));
                if (e instanceof AbaddonKnightEntity k && k.possessed()) leak(level, k);
            }
        }
        for (Iterator<Seen> it = RETINUE.values().iterator(); it.hasNext(); ) {
            if (ticks - it.next().tick > 100) it.remove();
        }
    }

    /** A possessed knight: thin wisps of black smoke seeping from the joints of its plate, crimson glints in it. */
    private static void leak(ClientLevel level, AbaddonKnightEntity k) {
        RandomSource r = level.random;
        float yaw = k.yBodyRot * Mth.DEG_TO_RAD;
        // Right of its facing is (-cos, -sin); the joints from its rig (shoulders, elbows, hips, knees, the neck).
        double rx = -Mth.cos(yaw), rz = -Mth.sin(yaw);
        float[][] joints = {{0.39f, 1.56f}, {-0.39f, 1.56f}, {0.42f, 1.2f}, {-0.42f, 1.2f}, {0.14f, 0.8f}, {-0.14f, 0.8f}, {0.14f, 0.42f},
                {-0.14f, 0.42f}, {0, 1.66f}};
        for (int i = FxParticles.rate(r, 0.5f); i > 0; i--) {
            float[] j = joints[r.nextInt(joints.length)];
            double x = k.getX() + rx * j[0] + FxParticles.j(r) * 0.06, y = k.getY() + j[1], z = k.getZ() + rz * j[0] + FxParticles.j(r) * 0.06;
            FxParticles.plain(level, AllParticles.DEMON_SMOKE.get(), x, y, z, rx * j[0] * 0.05 + FxParticles.j(r) * 0.01, 0.02,
                    rz * j[0] * 0.05 + FxParticles.j(r) * 0.01);
        }
        if (r.nextFloat() < 0.12f && FxParticles.n(1) > 0) {
            FxParticles.plain(level, ParticleTypes.SMOKE, k.getX() + FxParticles.j(r) * 0.3, k.getY() + 0.3 + r.nextDouble() * 1.6,
                    k.getZ() + FxParticles.j(r) * 0.3, 0, 0.03, 0);
        }
        if (r.nextFloat() < 0.4f) {
            FxParticles.ember(level, r.nextBoolean() ? AbaddonWorldFx.CRIMSON : AbaddonWorldFx.CRIMSON_HOT, 0.4f, 16,
                    k.getX() + FxParticles.j(r) * 0.35, k.getY() + 0.5 + r.nextDouble() * 1.4, k.getZ() + FxParticles.j(r) * 0.35, 0, 0.03, 0);
        }
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    /** Forget everything (left the world, or a preview between scenes). The world effects live in the magic's manager. */
    public static void clear() {
        AbaddonOverlay.clear();
        AbaddonWorldFx.clear();
        RETINUE.clear();
        RISING.clear();
        DYING.clear();
        TAKEN.clear();
        REFORM.clear();
    }
}
