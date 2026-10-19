package org.papiricoh.supernaturalcraft.client.witch;

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.papiricoh.supernaturalcraft.client.allegiance.render.AllegianceGeo;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * What the coven's renderers share (v0.21): which clip a controller plays and for how long it has played (the server says
 * nothing about "casting", so the clip on the {@code action} controller stands for it), a fading strength per entity for the
 * glowmasks, and the moments a rig is hidden while smoke covers a teleport ({@link #vanish}).
 */
public final class WitchGeo {

    /** How long a model stays hidden after its TELEPORT smoke arrives (the blink's slide is covered). */
    public static final int VANISH_TICKS = 6;

    private static final Map<Entity, Map<String, Track>> TRACKS = new WeakHashMap<>();
    private static final Map<Entity, float[]> FADES = new WeakHashMap<>();
    private static final Int2LongOpenHashMap VANISHED = new Int2LongOpenHashMap();

    private WitchGeo() {
    }

    private static final class Track {
        String clip = "";
        float start;
    }

    /** The clip {@code controller} plays now ("" for none). */
    public static String clip(Entity entity, String controller) {
        return entity instanceof GeoAnimatable g ? AllegianceGeo.clip(g, entity.getId(), controller) : "";
    }

    private static float now(float partial) {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0 : mc.level.getGameTime() + partial;
    }

    /**
     * The clip {@code controller} plays and how long it has played (ticks), noticed the first frame it is drawn: an entity never
     * drawn has no clock (age 0).
     */
    public static float clipAge(Entity entity, String controller, float partial) {
        String clip = clip(entity, controller);
        Track t = TRACKS.computeIfAbsent(entity, e -> new java.util.HashMap<>(2)).computeIfAbsent(controller, c -> new Track());
        float now = now(partial);
        if (!clip.equals(t.clip)) {
            t.clip = clip;
            t.start = now;
        }
        return now - t.start;
    }

    /**
     * Eases a per-entity strength toward {@code target} (0-1): up over {@code in} ticks, down over {@code out}, by the time between
     * frames. @return the strength this frame
     */
    public static float fade(Entity entity, String key, float target, float in, float out, float partial) {
        float[] f = FADES.computeIfAbsent(entity, e -> new float[]{0, Float.NaN});
        float now = now(partial);
        float dt = Float.isNaN(f[1]) ? 0 : Mth.clamp(now - f[1], 0, 10);
        f[1] = now;
        float v = f[0];
        if (target > v) v = Math.min(target, v + dt / Math.max(0.01f, in));
        else v = Math.max(target, v - dt / Math.max(0.01f, out));
        f[0] = v;
        return v;
    }

    // --- vanishing in smoke --------------------------------------------------------------------------------------------------

    /** Hides entity {@code id} for {@code ticks} (its smoke covers it). */
    public static void vanish(int id, int ticks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || id < 0) return;
        if (VANISHED.size() > 64) VANISHED.clear();
        VANISHED.put(id, mc.level.getGameTime() + ticks);
    }

    public static boolean vanished(Entity e) {
        long until = VANISHED.getOrDefault(e.getId(), Long.MIN_VALUE);
        if (until == Long.MIN_VALUE) return false;
        if (e.level().getGameTime() < until) return true;
        VANISHED.remove(e.getId());
        return false;
    }

    /**
     * Whether a boss's blink clip is at the moment its smoke covers it: from a tick before {@code hit} until
     * {@link #VANISH_TICKS} after.
     */
    public static boolean inBlink(Entity e, String clip, int hit, float partial) {
        if (!clip.equals(clip(e, "action"))) return false;
        float age = clipAge(e, "action", partial);
        return age >= hit - 1 && age < hit + VANISH_TICKS;
    }

    // --- rising out of the floor ---------------------------------------------------------------------------------------------

    /** How long a summoned body takes to claw its way up out of the floor (Rowena's hexed victims). */
    public static final int RISE_TICKS = 22;
    private static final Int2LongOpenHashMap RISING = new Int2LongOpenHashMap();

    /** Entity {@code id} rises out of the floor from now. */
    public static void rise(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || id < 0) return;
        if (RISING.size() > 64) RISING.clear();
        RISING.put(id, mc.level.getGameTime());
    }

    /** How far {@code e} has risen (0 under the floor, 1 standing; 1 when it was never told to rise). */
    public static float risen(Entity e, float partial) {
        long at = RISING.getOrDefault(e.getId(), Long.MIN_VALUE);
        if (at == Long.MIN_VALUE) return 1;
        float k = (e.level().getGameTime() - at + partial) / RISE_TICKS;
        if (k >= 1) {
            RISING.remove(e.getId());
            return 1;
        }
        return Mth.clamp(k, 0, 1);
    }

    public static void show(GeoModel<?> model, String bone, boolean visible) {
        AllegianceGeo.show(model, bone, visible);
    }
}
