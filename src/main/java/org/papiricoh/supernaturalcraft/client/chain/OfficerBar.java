package org.papiricoh.supernaturalcraft.client.chain;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;
import org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Entry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.papiricoh.supernaturalcraft.client.chain.ChainOverlay.*;

/**
 * The bars of the officers at hand: while a materialized officer is alive within {@link #RANGE} blocks, a compact bar at the
 * top of the screen ({@code bar_<side>.png}) with its medallion, name, rank, health and level pips; the nearest two, nearest
 * first. A Lord's bar is the larger, crested {@code lord_<side>.png}. They sit under any boss bars that are up (measured
 * from vanilla's own layout each frame) and drop whatever would pass a third of the screen; they give way to a title card
 * ({@link OfficerCard#fade}), and hide during camera sequences and with the GUI hidden. An officer with a bar loses its
 * vanilla nameplate (the bar names it).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class OfficerBar implements LayeredDraw.Layer {

    public static final ResourceLocation LAYER = SupernaturalCraft.asResource("chain_officer_bars");
    static final double RANGE = 24;
    private static final int MAX = 2;

    /** Bar geometry (see the art script): medallion, text line, track and the pips' right edge. */
    private static final int BAR_MED = 18, BAR_MED_X = 3, BAR_MED_Y = 3, BAR_TEXT_X = 26, BAR_TEXT_Y = 5, BAR_TRACK_X = 26,
            BAR_TRACK_Y = 15, BAR_TRACK_W = 145, BAR_TRACK_H = 4, BAR_PIPS_R = 171, BAR_PIPS_Y = 6;
    private static final int LORD_MED_X = 2, LORD_MED_Y = 6, LORD_TEXT_X = 46, LORD_TEXT_Y = 18, LORD_TRACK_X = 44,
            LORD_TRACK_Y = 30, LORD_TRACK_W = 182, LORD_TRACK_H = 6, LORD_PIPS_R = 226, LORD_PIPS_Y = 19;

    private static final Map<Integer, Tracked> TRACKED = new HashMap<>();
    /** The lowest point the boss bars reached this frame, and the frame it was measured in. */
    private static int frame, bossFrame = -1;
    private static float bossBottom;

    private static final class Tracked {
        final int id;
        Entry entry;
        boolean near;
        double distance;
        float alpha, health = -1, trail, maxHealth = 1;
        int hurtAt = Integer.MIN_VALUE / 2, age;

        Tracked(int id) {
            this.id = id;
        }
    }

    private OfficerBar() {
    }

    public static OfficerBar layer() {
        return new OfficerBar();
    }

    // --- tracking ------------------------------------------------------------------------------------------------

    static void tick(Minecraft mc) {
        Player player = mc.player;
        if (mc.level == null || player == null) {
            TRACKED.clear();
            return;
        }
        for (Tracked t : TRACKED.values()) t.near = false;
        if (!ClientChain.officers.isEmpty()) {
            List<LivingEntity> near = mc.level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RANGE),
                    e -> e != player && e.isAlive() && e.distanceToSqr(player) <= RANGE * RANGE && ClientChain.entryFor(e) != null
                            && !DuelHud.involves(e.getId()));
            // Higher ranks first (a Lord always has its bar), then the nearest.
            near.sort(Comparator.<LivingEntity>comparingInt(e -> -rankOf(ClientChain.entryFor(e))).thenComparingDouble(e -> e.distanceToSqr(player)));
            for (int i = 0; i < Math.min(MAX, near.size()); i++) {
                LivingEntity e = near.get(i);
                Tracked t = TRACKED.computeIfAbsent(e.getId(), Tracked::new);
                t.entry = ClientChain.entryFor(e);
                t.near = true;
                t.distance = e.distanceToSqr(player);
                float health = Math.max(0, e.getHealth());
                t.maxHealth = Math.max(1, e.getMaxHealth());
                if (t.health >= 0 && health < t.health - 0.01f) {
                    t.trail = Math.max(t.trail, t.health);
                    t.hurtAt = t.age;
                }
                t.health = health;
            }
        }
        TRACKED.values().removeIf(t -> {
            t.age++;
            // A dead officer's bar empties and fades; a far one fades.
            if (!t.near) {
                Entity e = mc.level.getEntity(t.id);
                if (e instanceof LivingEntity le && !le.isAlive()) t.health = 0;
            }
            t.alpha = Mth.clamp(t.alpha + (t.near ? 0.2f : -0.12f), 0, 1);
            // The trail of a blow holds a moment, then drains.
            if (t.age - t.hurtAt > 10) t.trail = Math.max(t.health, t.trail - t.maxHealth * 0.02f);
            return !t.near && t.alpha <= 0;
        });
    }

    static void clear() {
        TRACKED.clear();
    }

    /** Whether the entity's nameplate gives way to its bar. */
    static boolean barred(Entity e) {
        Tracked t = TRACKED.get(e.getId());
        return t != null && t.near;
    }

    @SubscribeEvent
    public static void onNameTag(RenderNameTagEvent event) {
        if (barred(event.getEntity())) event.setCanRender(TriState.FALSE);
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        frame++;
    }

    /** Vanilla lays the boss bars out one under another; this notes how far down they went (after every mod has had its say). */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        float bottom = event.getY() + event.getIncrement() - 9;
        if (bossFrame != frame) {
            bossFrame = frame;
            bossBottom = bottom;
        } else {
            bossBottom = Math.max(bossBottom, bottom);
        }
    }

    /** How far down the boss bars reach this frame (0 when none is up): the title cards keep below them. */
    static float bossBottom() {
        return bossFrame == frame ? bossBottom : 0;
    }

    // --- drawing -------------------------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (TRACKED.isEmpty() || mc.player == null || mc.options.hideGui || CameraDirector.active()) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        float give = 1 - OfficerCard.fade(partial);
        if (give <= 0.02f) return;
        List<Tracked> shown = new ArrayList<>(TRACKED.values());
        shown.sort(Comparator.<Tracked, Boolean>comparing(t -> !t.near).thenComparingInt(t -> -rankOf(t.entry)).thenComparingDouble(t -> t.distance));
        int w = g.guiWidth();
        float y = bossFrame == frame ? bossBottom + 2 : 3;
        float limit = g.guiHeight() / 3f;
        int drawn = 0;
        for (Tracked t : shown) {
            if (drawn >= MAX) break;
            if (t.entry == null) continue;
            boolean lord = lord(t.entry);
            int bw = lord ? LORD_W : BAR_W, bh = lord ? LORD_H : BAR_H;
            float scale = Math.min(1f, (w - 8) / (float) bw);
            if (y + bh * scale > limit && drawn > 0) break;
            float a = smooth(t.alpha) * give;
            g.pose().pushPose();
            g.pose().translate(w / 2f - bw * scale / 2f, y, 0);
            g.pose().scale(scale, scale, 1);
            if (lord) drawLord(g, t, a);
            else drawBar(g, t, a);
            g.pose().popPose();
            y += (bh + 1) * scale;
            drawn++;
        }
    }

    private static void drawBar(GuiGraphics g, Tracked t, float a) {
        Entry e = t.entry;
        int side = side(e);
        if (has(BAR[side])) blit(g, BAR[side], 0, 0, BAR_W, BAR_H, 0, 0, BAR_W, BAR_H, BAR_W, BAR_H, a);
        else plate(g, 10, 3, BAR_W, BAR_H - 3, side, a);
        medallion(g, side, rank(e.rank()), BAR_MED_X, BAR_MED_Y, BAR_MED, a);
        float pipsLeft = pips(g, side, e.level(), BAR_PIPS_R, BAR_PIPS_Y, a, true);
        line(g, e, side, BAR_TEXT_X, BAR_TEXT_Y, pipsLeft - 4 - BAR_TEXT_X, a, false);
        track(g, t, side, BAR_TRACK_X, BAR_TRACK_Y, BAR_TRACK_W, BAR_TRACK_H, a, has(BAR[side]));
    }

    private static void drawLord(GuiGraphics g, Tracked t, float a) {
        Entry e = t.entry;
        int side = side(e);
        if (has(LORD[side])) blit(g, LORD[side], 0, 0, LORD_W, LORD_H, 0, 0, LORD_W, LORD_H, LORD_W, LORD_H, a);
        else plate(g, 18, 10, 232, 40, side, a);
        medallion(g, side, rank(e.rank()), LORD_MED_X, LORD_MED_Y, MEDALLION, a);
        float pipsLeft = pips(g, side, e.level(), LORD_PIPS_R, LORD_PIPS_Y, a, true);
        line(g, e, side, LORD_TEXT_X, LORD_TEXT_Y, pipsLeft - 4 - LORD_TEXT_X, a, true);
        track(g, t, side, LORD_TRACK_X, LORD_TRACK_Y, LORD_TRACK_W, LORD_TRACK_H, a, has(LORD[side]));
    }

    /** "Name  Rank" (the Lord's "Name  Lord of the Pit"), shrunk if it would run into the pips. */
    private static void line(GuiGraphics g, Entry e, int side, float x, float y, float room, float a, boolean lord) {
        Component name = ChainOverlay.name(e);
        Component title = lord ? rankName(e.faction(), e.rank()).copy().append(" ").append(epithet(e)) : rankName(e.faction(), e.rank());
        float nw = GuiDraw.width(name, 1f), gap = 5, tw = GuiDraw.width(title, 1f);
        float scale = Mth.clamp(room / Math.max(1, nw + gap + tw), 0.6f, 1f);
        text(g, name, x, y + (1 - scale) * 4, scale, a, INK[side]);
        text(g, title, x + (nw + gap) * scale, y + (1 - scale) * 4, scale, a * 0.9f, ACCENT[side]);
    }

    private static void track(GuiGraphics g, Tracked t, int side, int x, int y, int w, int h, float a, boolean art) {
        if (!art) g.fill(x, y, x + w, y + h, argb(a * 0.9f, 0x070506));
        float max = Math.max(1, t.maxHealth);
        float hp = Mth.clamp(t.health / max, 0, 1), trail = Mth.clamp(t.trail / max, 0, 1);
        int fill = Math.round(w * hp);
        if (trail > hp) {
            float flash = Mth.clamp(1 - (t.age - t.hurtAt) / 14f, 0.45f, 1);
            g.fill(x + fill, y, x + Math.round(w * trail), y + h, argb(a * flash, 0xFFF4EC));
        }
        if (fill > 0) {
            g.fillGradient(x, y, x + fill, y + h, argb(a, FILL_HI[side]), argb(a, FILL_LO[side]));
            g.fill(x, y, x + fill, y + 1, argb(a * 0.35f, 0xFFFFFF));
        }
        // A tick every quarter.
        for (int i = 1; i < 4; i++) g.fill(x + w * i / 4, y, x + w * i / 4 + 1, y + h, argb(a * 0.35f, 0x000000));
    }

    private static int rankOf(@org.jetbrains.annotations.Nullable org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Entry e) {
        return e == null ? -1 : org.papiricoh.supernaturalcraft.chain.ChainRank.valueOf(e.rank()).ordinal();
    }
}
