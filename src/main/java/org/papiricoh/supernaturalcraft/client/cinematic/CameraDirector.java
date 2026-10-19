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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Takes the camera for a scripted sequence: a client-only marker entity becomes the camera and is
 * moved every frame along the sequence's path; the player's input is held and the HUD hidden
 * (letterbox and titles from {@link ClientCinematics} stay, and every layer let through with {@link #showDuringSequences}). Holding the skip key, dying, leaving
 * or changing dimension ends it, and every exit restores the camera.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class CameraDirector {

    public static final ResourceLocation SKIP_LAYER = SupernaturalCraft.asResource("camera_skip");
    /** The GUI layers that still draw during a sequence. */
    private static final Set<ResourceLocation> SHOWN = ConcurrentHashMap.newKeySet();

    static {
        SHOWN.add(SKIP_LAYER);
        SHOWN.add(SupernaturalCraft.asResource("cinematic"));
    }
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

    /**
     * Lets a GUI layer draw while the camera is taken. Only for layers that draw nothing but cinematic moments (a boss's
     * title cards, flashes): titles are sent with the camera shots, and a hidden one is never seen.
     */
    public static void showDuringSequences(ResourceLocation layer) {
        SHOWN.add(layer);
    }

    /** Whether {@code layer} draws while the camera is taken. */
    public static boolean shownDuringSequences(ResourceLocation layer) {
        return SHOWN.contains(layer);
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
        reach = 1;
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
        pos = clear(look, pos);
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

    /** How far along the line from the look point the camera may go this frame (eased back out after a wall is passed). */
    private static float reach = 1;

    /**
     * Keeps the camera out of walls (v0.21: fights indoors, Rowena's house): a shot is anchored on the boss and turned with her, so
     * where it lands in a room is not known when it is written. If a block stands between the look point and the camera, the camera
     * is drawn in toward the look point (stopping short of the wall), and it eases back out when the way is clear again. A look
     * point inside a block, or a clear line, leaves the shot as written.
     */
    private static Vec3 clear(Vec3 look, Vec3 pos) {
        double dist = look.distanceTo(pos);
        if (dist < 0.6 || level == null) {
            reach = 1;
            return pos;
        }
        net.minecraft.core.BlockPos lp = net.minecraft.core.BlockPos.containing(look);
        if (!level.getBlockState(lp).getCollisionShape(level, lp).isEmpty()) return pos;
        var hit = level.clip(new net.minecraft.world.level.ClipContext(look, pos, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
        float want = 1;
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            double free = look.distanceTo(hit.getLocation()) - 0.3;
            want = (float) Mth.clamp(Math.max(0.5, free) / dist, 0, 1);
        }
        // In at once (never a frame inside the wall), out gently.
        reach = want < reach ? want : Math.min(want, reach + 0.04f);
        return reach >= 0.999f ? pos : look.add(pos.subtract(look).scale(reach));
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
        if (skipHeld >= SNClientConfig.SKIP_HOLD_TICKS.get()) {
            // Skipped: the server lets go of the lock that kept us untouchable while we could not see (v0.19).
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(org.papiricoh.supernaturalcraft.network.CinematicSkipPayload.INSTANCE);
            stop();
        } else if (++ticks >= length()) {
            stop();
        }
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

    /** Only the letterbox/title layer, the skip hint and the layers let through draw during a sequence. */
    @SubscribeEvent
    public static void onGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (active() && !SHOWN.contains(event.getName())) event.setCanceled(true);
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
