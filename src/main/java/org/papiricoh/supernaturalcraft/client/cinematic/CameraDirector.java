package org.papiricoh.supernaturalcraft.client.cinematic;

import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.papiricoh.supernaturalcraft.SNClientConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cinematic.CameraSequence;
import org.papiricoh.supernaturalcraft.client.SNKeys;
import org.papiricoh.supernaturalcraft.network.CameraSequencePayload;

/**
 * Takes the camera for a scripted sequence: a client-only marker entity becomes the camera and is
 * moved every frame along the sequence's path; the player's input is held and the HUD hidden
 * (letterbox and titles from {@link ClientCinematics} stay). Holding the skip key, dying, leaving
 * or changing dimension ends it, and every exit restores the camera.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class CameraDirector {

    public static final ResourceLocation SKIP_LAYER = SupernaturalCraft.asResource("camera_skip");
    private static CameraSequence sequence;
    private static CameraSequencePayload playing;
    private static Marker camera;
    private static ClientLevel level;
    private static CameraType previousType;
    private static int ticks, skipHeld;
    private static CameraSequence.Frame frame;
    private static float yaw, pitch;
    private static Vec3 lastAnchor;

    private CameraDirector() {
    }

    public static boolean active() {
        return playing != null;
    }

    public static void play(CameraSequencePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!SNClientConfig.CINEMATICS.get() || mc.level == null || mc.player == null) return;
        CameraSequence seq = CinematicSequences.get(payload.sequence());
        if (seq == null) {
            SupernaturalCraft.LOGGER.warn("No cinematic {}", payload.sequence());
            return;
        }
        if (active()) stop();
        sequence = seq;
        playing = payload;
        level = mc.level;
        lastAnchor = payload.anchor();
        ticks = 0;
        skipHeld = 0;
        camera = new Marker(EntityType.MARKER, mc.level);
        previousType = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        place(0f);
        mc.setCameraEntity(camera);
    }

    public static void stop() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.getCameraEntity() == camera) mc.setCameraEntity(mc.player);
        if (previousType != null) mc.options.setCameraType(previousType);
        playing = null;
        sequence = null;
        camera = null;
        previousType = null;
        frame = null;
    }

    private static int length() {
        return Math.min(playing.ticks(), sequence.duration());
    }

    /** Moves the camera marker to the sequence's frame at {@code ticks + partial}. */
    private static void place(float partial) {
        frame = sequence.sample(ticks + partial);
        // Follow the anchor while it lives (bosses rise, fly and fall); its facing stays as it was.
        var anchor = level.getEntity(playing.anchorEntity());
        if (anchor != null) lastAnchor = anchor.getPosition(partial);
        Vec3 pos = CameraSequence.toWorld(frame.pos(), lastAnchor, playing.yaw());
        Vec3 look = CameraSequence.toWorld(frame.look(), lastAnchor, playing.yaw());
        Vec3 d = look.subtract(pos);
        yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        // Same old and new values: the renderer's own interpolation must not fight ours.
        camera.setPos(pos);
        camera.xo = camera.xOld = pos.x;
        camera.yo = camera.yOld = pos.y;
        camera.zo = camera.zOld = pos.z;
        camera.setYRot(yaw);
        camera.setXRot(pitch);
        camera.yRotO = yaw;
        camera.xRotO = pitch;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!active()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != level || mc.player == null || mc.player.isDeadOrDying()) {
            stop();
            return;
        }
        if (mc.isPaused()) return;
        skipHeld = SNKeys.SKIP_CINEMATIC.isDown() ? skipHeld + 1 : 0;
        if (++ticks >= length() || skipHeld >= SNClientConfig.SKIP_HOLD_TICKS.get()) stop();
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        if (active()) place(event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!active() || frame == null) return;
        float t = ticks + (float) event.getPartialTick();
        float s = frame.shake();
        event.setYaw(yaw + s * 1.2f * Mth.sin(t * 1.9f));
        event.setPitch(pitch + s * 0.9f * Mth.sin(t * 2.7f + 1f));
        event.setRoll(frame.roll());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (active() && frame != null) event.setFOV(frame.fov());
    }

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent event) {
        if (!active()) return;
        var in = event.getInput();
        in.forwardImpulse = 0;
        in.leftImpulse = 0;
        in.up = in.down = in.left = in.right = false;
        in.jumping = false;
        in.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onInteract(InputEvent.InteractionKeyMappingTriggered event) {
        if (!active()) return;
        event.setCanceled(true);
        event.setSwingHand(false);
    }

    @SubscribeEvent
    public static void onHand(RenderHandEvent event) {
        if (active()) event.setCanceled(true);
    }

    /** Only the letterbox/title layer and the skip hint draw during a sequence. */
    @SubscribeEvent
    public static void onGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (!active()) return;
        ResourceLocation name = event.getName();
        if (!name.equals(SKIP_LAYER) && !name.equals(SupernaturalCraft.asResource("cinematic"))) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (active()) stop();
    }

    /** "Hold [key] to skip", with a bar that fills while it is held. */
    public static class SkipHint implements LayeredDraw.Layer {
        @Override
        public void render(GuiGraphics g, DeltaTracker delta) {
            if (!active() || ticks < 20) return;
            var font = Minecraft.getInstance().font;
            Component text = Component.translatable("cinematic.supernaturalcraft.skip", SNKeys.SKIP_CINEMATIC.getTranslatedKeyMessage());
            int w = font.width(text), x = g.guiWidth() - w - 8, y = g.guiHeight() - 14;
            g.drawString(font, text, x, y, 0xA0FFFFFF, true);
            float held = Math.min(1f, skipHeld / (float) SNClientConfig.SKIP_HOLD_TICKS.get());
            if (held > 0) g.fill(x, y + 10, x + Math.round(w * held), y + 11, 0xFFF2E6B0);
        }
    }
}
