package org.papiricoh.supernaturalcraft.client.amara;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraLook;

import java.util.HashMap;
import java.util.Map;

/**
 * The Darkness as this client sees her (v0.42): the nearest {@link AmaraLook} entity, her sky's stage eased over the
 * {@code SKY} payload's duration, short-lived per-boss flags the FX set (Chuck's chains, the gravity pull, the gutter pulse) and
 * a preview override ({@link #fake}) the dev scenes use to show any phase without a fight. Everything client-side reads her
 * through {@link #look(Entity)} / {@link #current()}, never through the entity's own fields.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ClientAmara {

    /** How far her sky, her post-processing and her HUD reach. */
    public static final double RANGE = 160;
    /** Default blend for a sky change the server did not announce. */
    private static final int SKY_BLEND = 60;

    /** A staged look (previews): every field fixed. */
    public record Fake(int form, int phase, int skyStage, float chuckLight, float countdown, boolean enraged) implements AmaraLook {
        public Fake withSky(int stage) {
            return new Fake(form, phase, stage, chuckLight, countdown, enraged);
        }
    }

    private static @Nullable Fake fake;
    private static @Nullable Entity nearest;
    private static int scanCooldown;

    private static int skyFrom, skyTo = 0;
    private static int skyAge = 1, skyLife = 1;
    private static boolean skyAnnounced;

    private static final Map<Integer, Long> CHAINS_UNTIL = new HashMap<>();
    private static long ticks, gravityUntil;
    private static float gutter, gutterO;
    private static @Nullable Vec3 gravityAt;

    private ClientAmara() {
    }

    // --- preview override ------------------------------------------------------------------------------------------------

    /** The dev scenes' staged look (null to go back to the real one). */
    public static void fake(@Nullable Fake look) {
        fake = look;
        if (look != null) {
            skyFrom = skyTo = look.skyStage();
            skyAge = skyLife = 1;
        }
    }

    public static @Nullable Fake fake() {
        return fake;
    }

    private static final Map<Integer, Fake> FAKES = new HashMap<>();

    /** A staged look for one entity only (the previews' line-ups); null clears it. */
    public static void fake(int entityId, @Nullable Fake look) {
        if (look == null) FAKES.remove(entityId);
        else FAKES.put(entityId, look);
    }

    public static void clearFakes() {
        FAKES.clear();
        fake(null);
    }

    // --- who she is ------------------------------------------------------------------------------------------------------

    /** What to draw {@code e} as: the staged look if one is set, else the entity's own. */
    public static @Nullable AmaraLook look(@Nullable Entity e) {
        if (e != null && !FAKES.isEmpty()) {
            Fake own = FAKES.get(e.getId());
            if (own != null) return own;
        }
        if (fake != null) return fake;
        return e instanceof AmaraLook l ? l : null;
    }

    /** The nearest Amara (or the staged look), or null. */
    public static @Nullable AmaraLook current() {
        if (fake != null) return fake;
        return nearest instanceof AmaraLook l && !nearest.isRemoved() ? l : null;
    }

    public static @Nullable Entity nearestEntity() {
        return nearest != null && !nearest.isRemoved() ? nearest : null;
    }

    /** Whether her fight colours this client's world now. */
    public static boolean present() {
        return current() != null;
    }

    // --- the sky ---------------------------------------------------------------------------------------------------------

    /** The {@code SKY} payload: blend to {@code stage} over {@code duration} ticks. */
    public static void sky(int stage, int duration) {
        if (stage == skyTo && skyAge >= skyLife) return;
        skyFrom = skyBlend() < 0.5f ? skyFrom : skyTo;
        skyTo = Mth.clamp(stage, 0, 5);
        skyAge = 0;
        skyLife = Math.max(1, duration);
        skyAnnounced = true;
    }

    private static float skyBlend() {
        return Mth.clamp(skyAge / (float) skyLife, 0, 1);
    }

    /** How much of sky {@code stage} shows now (0..1): the stages being blended share the weight. */
    public static float skyWeight(int stage, float partial) {
        float k = Mth.clamp((skyAge + partial) / (float) skyLife, 0, 1);
        k = k * k * (3 - 2 * k);
        float w = 0;
        if (stage == skyTo) w += k;
        if (stage == skyFrom) w += 1 - k;
        return w;
    }

    public static int skyTarget() {
        return skyTo;
    }

    // --- per-boss flags --------------------------------------------------------------------------------------------------

    /** Her smoke chains show on boss {@code id} for {@code duration} ticks (phase IV's wound). */
    public static void chains(int id, int duration) {
        CHAINS_UNTIL.put(id, ticks + Math.max(1, duration));
    }

    public static boolean chainsShown(int id) {
        Long until = CHAINS_UNTIL.get(id);
        return until != null && until > ticks;
    }

    /** Phase V's pull toward the black sun, for {@code duration} ticks. */
    public static void gravity(Vec3 at, int duration) {
        gravityAt = at;
        gravityUntil = ticks + Math.max(1, duration);
    }

    /** 0..1: how strongly the pull shows (eases in and out). */
    public static float gravity(float partial) {
        if (ticks >= gravityUntil) return 0;
        float left = gravityUntil - ticks - partial;
        return Mth.clamp(Math.min(left / 20f, 1f), 0, 1);
    }

    public static @Nullable Vec3 gravityAt() {
        return gravityAt;
    }

    /** Phase VI's gutter: a pulse of dimming over the sky and the screen. */
    public static void gutterPulse(float strength) {
        gutter = Math.max(gutter, Mth.clamp(strength, 0, 1));
    }

    public static float gutter(float partial) {
        return Mth.lerp(partial, gutterO, gutter);
    }

    public static long ticks() {
        return ticks;
    }

    // --- ticking ---------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ticks++;
        gutterO = gutter;
        gutter = Math.max(0, gutter - 0.025f);
        if (skyAge < skyLife) skyAge++;
        if (mc.level == null || mc.player == null) {
            nearest = null;
            return;
        }
        if (nearest != null && (nearest.isRemoved() || nearest.level() != mc.level
                || nearest.distanceToSqr(mc.player) > RANGE * RANGE)) nearest = null;
        if (--scanCooldown <= 0) {
            scanCooldown = nearest == null ? 20 : 60;
            double best = RANGE * RANGE;
            Entity found = null;
            for (Entity e : mc.level.entitiesForRendering()) {
                if (!(e instanceof AmaraLook) || e.isRemoved()) continue;
                double d = e.distanceToSqr(mc.player);
                if (d < best) {
                    best = d;
                    found = e;
                }
            }
            nearest = found;
        }
        // A sky change the server did not announce (a joining player, a reload) still blends in.
        AmaraLook look = current();
        int want = look == null ? 0 : Mth.clamp(look.skyStage(), 0, 5);
        if (fake == null && want != skyTo && (skyAge >= skyLife || !skyAnnounced)) {
            sky(want, SKY_BLEND);
            skyAnnounced = false;
        }
        CHAINS_UNTIL.values().removeIf(until -> until <= ticks);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    public static void clear() {
        nearest = null;
        skyFrom = skyTo = 0;
        skyAge = skyLife = 1;
        CHAINS_UNTIL.clear();
        gravityUntil = 0;
        gutter = gutterO = 0;
    }
}
