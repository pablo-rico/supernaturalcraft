package org.papiricoh.supernaturalcraft.client.magic.fx;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.SNClientConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Every live magic effect on the client (v0.21): ticks them, draws them, and keeps them within a budget set by
 * {@code magicFxIntensity} (0 minimal, 1 reduced, 2 full): a cap on live effects (the oldest give way), a particle budget per tick
 * ({@link FxParticles}) and a distance past which nothing is drawn or even started.
 *
 * <p>They are drawn after the weather, the last world stage: the clouds come after the translucent blocks, and as light writes no
 * depth a cloud behind a pillar would otherwise be painted over it (dark bands across the sky). With Fabulous graphics the clouds
 * have their own target, so there they keep the translucent stage. The sigil bolts are drawn here too ({@link SigilBoltRenderer}).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class MagicFxManager {

    /** Effects further than this from the camera (plus their own reach) are neither started nor drawn. */
    public static final double VIEW_RANGE = 96;

    private static final List<MagicEffect> LIVE = new ArrayList<>();
    /** The effects' clock: client ticks while unpaused. */
    private static long clock;

    private MagicFxManager() {
    }

    /** 0 minimal, 1 reduced, 2 full. */
    public static int detail() {
        try {
            return SNClientConfig.MAGIC_FX_INTENSITY.get();
        } catch (IllegalStateException e) {
            return 2;
        }
    }

    private static int cap() {
        return switch (detail()) {
            case 0 -> 24;
            case 1 -> 64;
            default -> 160;
        };
    }

    public static long clock() {
        return clock;
    }

    /** Starts an effect (if anyone could see it). */
    public static void add(MagicEffect e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !visible(mc, e.anchor(), e.reach() + 16)) return;
        if (LIVE.size() >= cap()) LIVE.removeFirst();
        LIVE.add(e);
    }

    /** Whether a point is within the view range of the camera. */
    public static boolean visible(Minecraft mc, Vec3 at, double extra) {
        if (mc.gameRenderer == null) return true;
        double r = VIEW_RANGE + extra;
        return mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(at) < r * r;
    }

    /**
     * How bright the day is where the camera stands (0 at night, underground or in a skyless dimension; 1 at open noon): the weight
     * of the painted pass that keeps the light readable by day ({@link FxDraw#day}).
     */
    public static float daylight(float partial) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.gameRenderer == null || !level.dimensionType().hasSkyLight()) return 0;
        float sun = Mth.clamp((level.getSkyDarken(partial) - 0.25f) / 0.7f, 0, 1);
        if (sun <= 0) return 0;
        var cam = net.minecraft.core.BlockPos.containing(mc.gameRenderer.getMainCamera().getPosition());
        float sky = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, cam) / 15f;
        return sun * sky;
    }

    /** Whether anything is showing (previews). */
    public static boolean busy() {
        return !LIVE.isEmpty() || RitualChargeFx.any();
    }

    public static void clear() {
        LIVE.clear();
        RitualChargeFx.clear();
        BoltTrails.clear();
        RecitationFx.clear();
        MagicOverlay.reset();
        MagicShake.reset();
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            if (!LIVE.isEmpty() || RitualChargeFx.any()) clear();
            return;
        }
        if (mc.isPaused()) return;
        clock++;
        FxParticles.refill();
        MagicShake.tick();
        MagicOverlay.tick();
        // Effects may start others while ticking (a seal that lands, a pop after an implosion): those join the list's end.
        for (int i = 0; i < LIVE.size(); i++) {
            MagicEffect e = LIVE.get(i);
            e.tick(level);
            e.age++;
        }
        LIVE.removeIf(MagicEffect::done);
        RitualChargeFx.tickAll(level);
        BoltTrails.tick(level);
        RecitationFx.tick(level);
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            // The additive particle sheet leaves its blend function behind: restore the default for whatever draws next.
            RenderSystem.defaultBlendFunc();
            return;
        }
        RenderLevelStageEvent.Stage stage = Minecraft.useShaderTransparency() ? RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                : RenderLevelStageEvent.Stage.AFTER_WEATHER;
        if (event.getStage() != stage) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !busy() && !BoltTrails.any() && !CurseAuras.any(mc) && !ShieldBubbles.any()) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var camera = event.getCamera();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        FxDraw d = new FxDraw(event.getPoseStack(), buffers, camera, camera.getPosition(), partial, clock + partial);
        for (MagicEffect e : LIVE) {
            if (!visible(mc, e.anchor(), e.reach())) continue;
            d.floor(e.floor(mc.level));
            e.render(d);
        }
        d.floor(Double.NaN);
        RitualChargeFx.renderAll(d);
        SigilBoltRenderer.renderAll(d, mc);
        CurseAuras.renderAll(d, mc);
        ShieldBubbles.renderAll(d, mc);
        d.floor(Double.NaN);
        buffers.endBatch();
    }

    /**
     * The local player's cast circles carried by the grimoire in their own first-person hand: called by its renderer right
     * after the book, with the open spread's centre in view space, so the circle sits on the book whatever the FOV or bobbing.
     */
    public static void renderAtBook(MultiBufferSource buffers, Vector3f spread, HumanoidArm arm) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || LIVE.isEmpty()) return;
        float partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
        FxDraw d = FxDraw.view(new PoseStack(), buffers, mc.gameRenderer.getMainCamera(), partial, clock + partial);
        for (MagicEffect e : LIVE) {
            if (e instanceof CastCircleFx c) c.renderAtBook(d, spread, arm);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }
}
