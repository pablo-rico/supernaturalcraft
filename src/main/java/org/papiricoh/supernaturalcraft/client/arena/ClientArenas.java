package org.papiricoh.supernaturalcraft.client.arena;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.network.ArenaStatePayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Arenas the server has told us about: draws their domes and plays the fight music inside. */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ClientArenas {

    private static final ResourceLocation DOME = SupernaturalCraft.asResource("textures/effect/arena_dome.png");
    private static final int SEGMENTS = 72;

    private record Known(ArenaStatePayload state, long lastSeen) {
    }

    private static final Map<UUID, Known> ARENAS = new HashMap<>();
    private static ArenaMusic music;
    private static long clientTicks;

    private ClientArenas() {
    }

    public static void update(ArenaStatePayload p) {
        if (p.active()) ARENAS.put(p.id(), new Known(p, clientTicks));
        else ARENAS.remove(p.id());
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        clientTicks++;
        ARENAS.values().removeIf(k -> clientTicks - k.lastSeen > 100);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            ARENAS.clear();
            return;
        }
        Known near = ARENAS.values().stream().filter(k -> distance(k.state, mc.player.position()) < k.state.radius() + 12)
                .findFirst().orElse(null);
        boolean inside = near != null;
        if (inside && (music == null || music.isStopped())) {
            mc.getMusicManager().stopPlaying();
            music = new ArenaMusic(ArenaStyles.music(near.state.theme()));
            mc.getSoundManager().play(music);
        }
        if (music != null) music.wanted = inside;
    }

    private static double distance(ArenaStatePayload s, Vec3 p) {
        double dx = p.x - (s.center().getX() + 0.5), dz = p.z - (s.center().getZ() + 0.5);
        return Math.sqrt(dx * dx + dz * dz);
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || ARENAS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType type = RenderType.entityTranslucentEmissive(DOME);
        VertexConsumer vc = buffers.getBuffer(type);
        PoseStack pose = event.getPoseStack();
        float time = (clientTicks + event.getPartialTick().getGameTimeDeltaPartialTick(false)) / 20f;
        for (Known k : ARENAS.values()) {
            ArenaStatePayload s = k.state;
            int color = ArenaStyles.color(s.theme(), s.phase());
            double near = s.radius() - distance(s, mc.player == null ? cam : mc.player.position());
            float alpha = 0.28f + 0.5f * (1 - Mth.clamp((float) near / 5f, 0, 1));
            pose.pushPose();
            pose.translate(s.center().getX() + 0.5 - cam.x, s.center().getY() - cam.y, s.center().getZ() + 0.5 - cam.z);
            wall(vc, pose.last().pose(), pose.last(), s.radius(), -ArenaTheme.depth(s.theme()) / 2f, ArenaTheme.height(s.theme()), color, alpha, time);
            pose.popPose();
        }
        buffers.endBatch(type);
    }

    private static void wall(VertexConsumer vc, Matrix4f m, PoseStack.Pose p, float r, float y0, float y1, int rgb, float alpha,
                             float time) {
        int cr = (rgb >> 16) & 0xFF, cg = (rgb >> 8) & 0xFF, cb = rgb & 0xFF, a = (int) (alpha * 255);
        float scroll = time * 0.05f;
        float uScale = 2 * Mth.PI * r / 8f;  // the texture repeats every 8 blocks around the wall
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = Mth.TWO_PI * i / SEGMENTS, a1 = Mth.TWO_PI * (i + 1) / SEGMENTS;
            float x0 = Mth.cos(a0) * r, z0 = Mth.sin(a0) * r, x1 = Mth.cos(a1) * r, z1 = Mth.sin(a1) * r;
            float u0 = uScale * i / SEGMENTS + scroll, u1 = uScale * (i + 1) / SEGMENTS + scroll;
            v(vc, m, p, x0, y0, z0, u0, 1, cr, cg, cb, a);
            v(vc, m, p, x0, y1, z0, u0, 0, cr, cg, cb, 0);
            v(vc, m, p, x1, y1, z1, u1, 0, cr, cg, cb, 0);
            v(vc, m, p, x1, y0, z1, u1, 1, cr, cg, cb, a);
        }
    }

    private static void v(VertexConsumer vc, Matrix4f m, PoseStack.Pose p, float x, float y, float z, float u, float vv,
                          int r, int g, int b, int a) {
        vc.addVertex(m, x, y, z).setColor(r, g, b, a).setUv(u, vv).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(p, 0, 1, 0);
    }

    /** The fight theme, faded in and out as you cross the dome. */
    private static class ArenaMusic extends AbstractTickableSoundInstance {
        boolean wanted = true;

        ArenaMusic(net.minecraft.sounds.SoundEvent theme) {
            super(theme, SoundSource.MUSIC, net.minecraft.client.resources.sounds.SoundInstance.createUnseededRandom());
            looping = true;
            relative = true;
            volume = 0.01f;
            attenuation = Attenuation.NONE;
        }

        @Override
        public void tick() {
            volume = Mth.clamp(volume + (wanted ? 0.02f : -0.02f), 0f, 1f);
            if (!wanted && volume <= 0f) stop();
        }
    }
}
