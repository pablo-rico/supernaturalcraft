package org.papiricoh.supernaturalcraft.client.chain;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.client.book.chain.ChainDraw;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;
import org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector;
import org.papiricoh.supernaturalcraft.network.ChainDuelPayload;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Entry;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;

import java.util.Comparator;
import java.util.List;

import static org.papiricoh.supernaturalcraft.client.chain.ChainOverlay.*;

/**
 * The duel the local hunter is near ({@link ChainDuelPayload}): a banner at the top of the screen, "A ⚔ B", with each
 * duellist's medallion (or face), name and health draining toward the crest (crimson to the death, gold until one yields), and
 * in the world a ring of runes on the ground with a low wall of light where the duel is fought. When it ends the banner holds a
 * moment on the winner, then fades. It sits under the boss bars and the officers' bars, and hides with the GUI and in camera
 * sequences.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class DuelHud implements LayeredDraw.Layer {
    private DuelHud() {}

    public static final ResourceLocation LAYER = SupernaturalCraft.asResource("chain_duel");
    private static final ResourceLocation DUEL = SupernaturalCraft.asResource("textures/gui/chain/duel.png");
    private static final ResourceLocation DUEL_LEVIATHAN = SupernaturalCraft.asResource("textures/gui/chain/duel_leviathan.png");
    private static final ResourceLocation RING = SupernaturalCraft.asResource("textures/gui/chain/duel_ring.png");
    private static final int HALF_W = 112, HALF_H = 24, CREST = 32, OUTRO = 70, SEGMENTS = 64;
    /** Lethal crimson, yielding gold. */
    private static final int LETHAL = 0xD8352A, YIELD = 0xF2D257;

    private static @Nullable ChainDuelPayload duel;
    /** The duel just ended, held on screen a moment ({@link #OUTRO} ticks), and its winner. */
    private static @Nullable ChainDuelPayload ended;
    private static int winner = -1, outro, age;
    private static float alpha;
    private static final Side A = new Side(), B = new Side();

    private static final class Side {
        float health = -1, max = 1, trail;
        int hurtAt = -100;

        void reset() {
            health = -1;
            max = 1;
            trail = 0;
            hurtAt = -100;
        }

        void track(@Nullable Entity e) {
            if (!(e instanceof LivingEntity le)) return;
            float h = Math.max(0, le.isAlive() ? le.getHealth() : 0);
            max = Math.max(1, le.getMaxHealth());
            if (health >= 0 && h < health - 0.01f) {
                trail = Math.max(trail, health);
                hurtAt = age;
            }
            health = h;
            if (age - hurtAt > 10) trail = Math.max(health, trail - max * 0.02f);
        }
    }

    public static void set(ChainDuelPayload p) {
        if (p.ended()) {
            if (duel != null && duel.id() == p.id()) {
                ended = duel;
                winner = p.winner();
                outro = OUTRO;
                duel = null;
            }
        } else {
            if (duel == null || duel.id() != p.id()) {
                A.reset();
                B.reset();
            }
            duel = p;
            ended = null;
        }
    }

    public static @Nullable ChainDuelPayload current() {
        return duel;
    }

    /** Whether an entity is one of the duellists on screen (the officers' bars may leave them to the banner). */
    public static boolean involves(int entityId) {
        ChainDuelPayload d = shown();
        return d != null && (d.a() == entityId || d.b() == entityId);
    }

    public static void clear() {
        duel = null;
        ended = null;
        outro = 0;
        alpha = 0;
    }

    private static @Nullable ChainDuelPayload shown() {
        return duel != null ? duel : outro > 0 ? ended : null;
    }

    public static DuelHud layer() {
        return new DuelHud();
    }

    static void tick(Minecraft mc) {
        age++;
        if (mc.level == null) {
            clear();
            return;
        }
        if (outro > 0 && --outro == 0) ended = null;
        ChainDuelPayload d = shown();
        alpha = Mth.clamp(alpha + (duel != null || outro > 12 ? 0.15f : -0.1f), 0, 1);
        if (d == null) return;
        A.track(mc.level.getEntity(d.a()));
        B.track(mc.level.getEntity(d.b()));
    }

    // --- the banner ------------------------------------------------------------------------------

    /** A duellist's side: an officer's own, a hunter's by allegiance (a plain human in ash). */
    private static int sideOf(@Nullable Entity e) {
        if (e == null) return FALLEN;
        Entry o = ClientChain.entryFor(e);
        if (o != null) return side(o);
        if (e instanceof Player p) {
            Faction f = p.getData(AllAttachments.ALLEGIANCE).faction();
            return f == Faction.DEMON ? PIT : f == Faction.ANGEL ? HOST : FALLEN;
        }
        return FALLEN;
    }

    /** How far down the boss bars and the officers' bars reach (the duellists have no officer bar of their own). */
    private static float below(Minecraft mc) {
        float y = OfficerBar.bossBottom() > 0 ? OfficerBar.bossBottom() + 2 : 3;
        Player player = mc.player;
        if (player == null || mc.level == null || ClientChain.officers.isEmpty()) return y;
        List<LivingEntity> near = mc.level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(OfficerBar.RANGE),
                e -> e != player && e.isAlive() && e.distanceToSqr(player) <= OfficerBar.RANGE * OfficerBar.RANGE && ClientChain.entryFor(e) != null
                        && !involves(e.getId()));
        near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        for (int i = 0; i < Math.min(2, near.size()); i++) y += (lord(ClientChain.entryFor(near.get(i))) ? LORD_H : BAR_H) + 1;
        return y;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        ChainDuelPayload d = shown();
        if (d == null || alpha <= 0.02f || mc.options.hideGui || CameraDirector.active() || mc.level == null) return;
        float a = smooth(alpha);
        int total = HALF_W * 2 + CREST - 12;
        float scale = Math.min(1f, (g.guiWidth() - 8) / (float) total);
        float y = below(mc) + 2;
        float cx = g.guiWidth() / 2f;
        Entity ea = mc.level.getEntity(d.a()), eb = mc.level.getEntity(d.b());
        int sa = sideOf(ea), sb = sideOf(eb);
        boolean over = d == ended;
        float dimA = over && winner >= 0 && winner != d.a() ? 0.45f : 1, dimB = over && winner >= 0 && winner != d.b() ? 0.45f : 1;
        g.pose().pushPose();
        g.pose().translate(cx, y + 4, 0);
        g.pose().scale(scale, scale, 1);
        RenderSystem.enableBlend();
        half(g, d, ea, sa, A, -CREST / 2f + 6 - HALF_W, true, a * dimA, d.nameA());
        half(g, d, eb, sb, B, CREST / 2f - 6, false, a * dimB, d.nameB());
        // The crest over both, breathing while the duel lasts.
        float breathe = duel != null ? 0.85f + 0.15f * Mth.sin((age + delta.getGameTimeDeltaPartialTick(false)) * 0.2f) : 1;
        g.pose().pushPose();
        g.pose().translate(0, 12, 0);
        g.pose().scale(breathe * 0.1f + 0.9f, breathe * 0.1f + 0.9f, 1);
        blit(g, DUEL, -CREST / 2f, -16, CREST, CREST, d.lethal() ? 0 : 32, 72, CREST, CREST, 256, 128, a);
        g.pose().popPose();
        Component sub;
        if (over) {
            sub = winner == d.a() ? Component.translatable("screen.supernaturalcraft.chain.duel.won", d.nameA())
                    : winner == d.b() ? Component.translatable("screen.supernaturalcraft.chain.duel.won", d.nameB())
                    : Component.translatable("screen.supernaturalcraft.chain.duel.over");
        } else {
            sub = Component.translatable("screen.supernaturalcraft.chain.duel." + (d.lethal() ? "lethal" : "yield"));
        }
        centred(g, sub, 0, CREST - 1, over ? 1f : 0.75f, a, d.lethal() ? 0xF08070 : YIELD);
        g.pose().popPose();
    }

    /** One duellist: the plate (mirrored for the right), its medallion or face, its name and its health. */
    private static void half(GuiGraphics g, ChainDuelPayload d, @Nullable Entity e, int side, Side s, float x, boolean left,
                             float a, String name) {
        blit(g, sheet(DUEL, DUEL_LEVIATHAN, side), x, 0, HALF_W, HALF_H, left ? 0 : HALF_W, row(side) * HALF_H, HALF_W, HALF_H, 256, 128, a);
        float socket = left ? x + 3 : x + HALF_W - 21;
        Entry o = e == null ? null : ClientChain.entryFor(e);
        if (o != null) medallion(g, side, rank(o.rank()), socket, 3, 18, a);
        else if (e instanceof Player p) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 1);
            if (a > 0.5f) ChainDraw.face(g, p.getUUID().toString(), Math.round(socket) + 2, 5, 14);
            g.pose().popPose();
        }
        Component n = Component.literal(name);
        float room = 74, w = GuiDraw.width(n, 1f), sc = Math.min(1f, room / Math.max(1, w));
        float nx = left ? x + 26 : x + HALF_W - 26 - w * sc;
        text(g, n, nx, 5 + (1 - sc) * 4, sc, a, INK[side]);
        // Health drains toward the crest.
        int tx = Math.round(left ? x + 26 : x + 7), tw = 79, ty = 15, th = 4;
        float hp = s.health < 0 ? 1 : Mth.clamp(s.health / s.max, 0, 1), trail = Mth.clamp(s.trail / s.max, 0, 1);
        int fill = Math.round(tw * hp), tr = Math.round(tw * trail);
        if (tr > fill) {
            float flash = Mth.clamp(1 - (age - s.hurtAt) / 14f, 0.45f, 1);
            if (left) g.fill(tx + tw - tr, ty, tx + tw - fill, ty + th, argb(a * flash, 0xFFF4EC));
            else g.fill(tx + fill, ty, tx + tr, ty + th, argb(a * flash, 0xFFF4EC));
        }
        if (fill > 0) {
            int x0 = left ? tx + tw - fill : tx, x1 = left ? tx + tw : tx + fill;
            int hi = d.lethal() ? FILL_HI[side] : 0xFFF4C8, lo = d.lethal() ? FILL_LO[side] : 0xD9A92B;
            g.fillGradient(x0, ty, x1, ty + th, argb(a, hi), argb(a, lo));
            g.fill(x0, ty, x1, ty + 1, argb(a * 0.35f, 0xFFFFFF));
        }
        // The yielding mark: below it, a duel that is not to the death is lost.
        if (!d.lethal()) {
            int mark = Math.round(tw * org.papiricoh.supernaturalcraft.chain.Challenge.YIELD_FRACTION);
            int mx = left ? tx + tw - mark : tx + mark;
            g.fill(mx, ty - 1, mx + 1, ty + th + 1, argb(a * 0.8f, 0xFFFFFF));
        }
    }

    // --- the ring in the world -------------------------------------------------------------------

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        ChainDuelPayload d = shown();
        if (d == null || alpha <= 0.02f || d.radius() <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = event.getCamera().getPosition();
        if (cam.distanceToSqr(d.x(), d.y(), d.z()) > 128 * 128) return;
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType type = RenderType.entityTranslucentEmissive(RING);
        VertexConsumer vc = buffers.getBuffer(type);
        PoseStack pose = event.getPoseStack();
        float time = age + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        int rgb = d.lethal() ? LETHAL : YIELD;
        float a = smooth(alpha) * (0.75f + 0.25f * Mth.sin(time * 0.12f));
        pose.pushPose();
        pose.translate(d.x() - cam.x, d.y() - cam.y, d.z() - cam.z);
        Matrix4f m = pose.last().pose();
        PoseStack.Pose p = pose.last();
        float r = d.radius(), scroll = time * 0.01f, uScale = 2 * Mth.PI * r / 1.5f;
        int cr = (rgb >> 16) & 0xFF, cg = (rgb >> 8) & 0xFF, cb = rgb & 0xFF;
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = Mth.TWO_PI * i / SEGMENTS, a1 = Mth.TWO_PI * (i + 1) / SEGMENTS;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float u0 = uScale * i / SEGMENTS + scroll, u1 = uScale * (i + 1) / SEGMENTS + scroll;
            int wall = (int) (a * 110), ground = (int) (a * 200);
            // The wall: bright at the ground, gone at 1.6 blocks.
            quad(vc, m, p, c0 * r, 0.02f, s0 * r, u0, 1, c0 * r, 1.6f, s0 * r, u0, 0, c1 * r, 1.6f, s1 * r, u1, 0,
                    c1 * r, 0.02f, s1 * r, u1, 1, cr, cg, cb, wall);
            // The band of runes on the ground.
            float ri = r - 0.25f, ro = r + 0.25f;
            quad(vc, m, p, c0 * ri, 0.03f, s0 * ri, u0, 0.6f, c0 * ro, 0.03f, s0 * ro, u0, 0.78f, c1 * ro, 0.03f, s1 * ro, u1, 0.78f,
                    c1 * ri, 0.03f, s1 * ri, u1, 0.6f, cr, cg, cb, ground);
        }
        pose.popPose();
        buffers.endBatch(type);
    }

    /** A quad seen from both faces. */
    private static void quad(VertexConsumer vc, Matrix4f m, PoseStack.Pose p, float x0, float y0, float z0, float u0, float v0,
                             float x1, float y1, float z1, float u1, float v1, float x2, float y2, float z2, float u2, float v2,
                             float x3, float y3, float z3, float u3, float v3, int r, int g, int b, int a) {
        v(vc, m, p, x0, y0, z0, u0, v0, r, g, b, a);
        v(vc, m, p, x1, y1, z1, u1, v1, r, g, b, a);
        v(vc, m, p, x2, y2, z2, u2, v2, r, g, b, a);
        v(vc, m, p, x3, y3, z3, u3, v3, r, g, b, a);
        v(vc, m, p, x3, y3, z3, u3, v3, r, g, b, a);
        v(vc, m, p, x2, y2, z2, u2, v2, r, g, b, a);
        v(vc, m, p, x1, y1, z1, u1, v1, r, g, b, a);
        v(vc, m, p, x0, y0, z0, u0, v0, r, g, b, a);
    }

    private static void v(VertexConsumer vc, Matrix4f m, PoseStack.Pose p, float x, float y, float z, float u, float vv,
                          int r, int g, int b, int a) {
        vc.addVertex(m, x, y, z).setColor(r, g, b, a).setUv(u, vv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(p, 0, 1, 0);
    }
}
