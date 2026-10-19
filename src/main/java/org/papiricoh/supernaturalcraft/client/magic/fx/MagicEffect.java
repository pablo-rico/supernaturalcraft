package org.papiricoh.supernaturalcraft.client.magic.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.magic.school.School;

/**
 * One live moment of magic on the client (v0.21): ticked by {@link MagicFxManager} (where it spawns its particles) and drawn in the
 * world stage (where it draws its geometry). Its age counts client ticks; it ends when the age reaches its life.
 */
public abstract class MagicEffect {

    protected final Vec3 at;
    protected final School school;
    protected final int life;
    /** The school's main colour and its accent (always bright enough to read as light). */
    protected final int primary, accent;
    protected int age;

    protected MagicEffect(Vec3 at, School school, int life) {
        this.at = at;
        this.school = school;
        this.life = Math.max(1, life);
        this.primary = FxColor.primary(school);
        this.accent = FxColor.accent(school);
    }

    /** Called once per client tick before the age advances: spawn particles, play sounds. */
    protected void tick(ClientLevel level) {
    }

    /** Draws the effect. */
    protected abstract void render(FxDraw d);

    protected boolean done() {
        return age >= life;
    }

    /** How far from {@link #anchor} the effect reaches (blocks): used to cull it by distance. */
    protected double reach() {
        return 6;
    }

    /** Where the effect is now (it may follow an entity). */
    protected Vec3 anchor() {
        return at;
    }

    private double floorY = Double.NaN;
    private int floorAge = Integer.MIN_VALUE;

    /**
     * The floor under the effect (world y; NaN where there is none within reach), under which its glows fade out
     * ({@link FxDraw#floor}). Found under {@link #anchor} and refreshed every few ticks for effects that follow something; an effect
     * that knows better overrides it.
     */
    protected double floor(ClientLevel level) {
        if (age - floorAge >= 5) {
            floorAge = age;
            floorY = FxWorld.floorOrNaN(level, anchor(), 6);
        }
        return floorY;
    }

    /** Ticks since it began, with the partial tick. */
    protected final float time(FxDraw d) {
        return age + d.partial;
    }

    /** 0 → 1 over its life. */
    protected final float progress(FxDraw d) {
        return Mth.clamp((age + d.partial) / life, 0f, 1f);
    }

    /** Fades in over {@code in} ticks and out over the last {@code out}. */
    protected final float fade(float t, float in, float out) {
        float a = in <= 0 ? 1 : t / in;
        float b = out <= 0 ? 1 : (life - t) / out;
        return Mth.clamp(Math.min(a, b), 0f, 1f);
    }

    protected static @Nullable Entity entity(int id) {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null || id < 0 ? null : mc.level.getEntity(id);
    }

    /** The point an entity's centre is at this frame, or {@code fallback}. */
    protected static Vec3 centre(@Nullable Entity e, float partial, Vec3 fallback) {
        return e == null || e.isRemoved() ? fallback : e.getPosition(partial).add(0, e.getBbHeight() * 0.5, 0);
    }

    /** Back-out easing: overshoots a little past 1 before settling. */
    protected static float easeOutBack(float k) {
        k = Mth.clamp(k, 0, 1) - 1;
        return 1 + k * k * (2.7f * k + 1.7f);
    }

    protected static float easeOut(float k) {
        k = Mth.clamp(k, 0, 1);
        return 1 - (1 - k) * (1 - k) * (1 - k);
    }
}
