package org.papiricoh.supernaturalcraft.client.book.chain;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.papiricoh.supernaturalcraft.allegiance.Ranks;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.ChainNames;
import org.papiricoh.supernaturalcraft.chain.ChainRank;
import org.papiricoh.supernaturalcraft.client.book.BookAtlas.Sprite;
import org.papiricoh.supernaturalcraft.client.book.BookSection;
import org.papiricoh.supernaturalcraft.client.book.BookStyle;
import org.papiricoh.supernaturalcraft.client.book.chain.ChainTreeLayout.Node;
import org.papiricoh.supernaturalcraft.client.book.chain.ChainTreeLayout.Place;
import org.papiricoh.supernaturalcraft.client.chain.ClientChain;
import org.papiricoh.supernaturalcraft.client.chain.OfficerPuppets;
import org.papiricoh.supernaturalcraft.client.legacy.table.CompassMath;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Entry;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.History;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Chain of Command tab: one faction's living officers as a tree across both pages, each in a medallion with its own mob
 * inside, chained (the Pit) or corded with light (the Host) to the one it answers to. Drag to move, scroll to zoom, click an
 * officer for its record; the scroll button opens the chain's recent events and who may yet return. Everything is everyone's
 * to read: the chain is shared by the whole world.
 */
public class ChainSection extends BookSection {

    private static final int X0 = BookStyle.SPREAD_X, Y0 = BookStyle.SPREAD_Y;
    private static final int X1 = X0 + BookStyle.SPREAD_W, Y1 = Y0 + BookStyle.SPREAD_H;
    private static final float CX = X0 + BookStyle.SPREAD_W / 2f, CY = Y0 + BookStyle.SPREAD_H / 2f + 8;
    private static final float MIN_ZOOM = 0.45f, MAX_ZOOM = 3f;
    /** Below this zoom the medallions show their numeral instead of their mob; below the other, no names. */
    private static final float MODEL_ZOOM = 0.55f, LABEL_ZOOM = 0.55f;
    /** Half a medallion and half its window, in map units. */
    private static final int HALF = ChainAtlas.MEDALLION / 2, WINDOW = 12;
    /** Depths: the mobs stand around z 50; frames, the vignette, the furniture and the cards go over them. */
    private static final float Z_FRAME = 110, Z_VIGNETTE = 150, Z_FURNITURE = 170, Z_CARD = 260;
    private static final int BUTTON_Y = Y0 + 8, CARD_W = 152;
    private static final String K = "screen.supernaturalcraft.book.chain.";

    private enum Card { NONE, OFFICER, EVENTS }

    // --- the view, kept across openings ------------------------------------------------------------
    private static ChainFaction faction = ChainFaction.DEMON;
    private static final Map<ChainFaction, float[]> VIEWS = new EnumMap<>(ChainFaction.class);
    private static float panX, panY, zoom = 1;
    private static boolean placed;
    private static boolean gliding;
    private static float goalX, goalY, goalZoom;
    private static long lastFrame;
    private static int selected = -1;
    private static Card card = Card.NONE;

    // --- this frame ---------------------------------------------------------------------------------
    private ChainTreeLayout.Result tree = ChainTreeLayout.Result.EMPTY;
    private final Map<Integer, Entry> entries = new java.util.HashMap<>();
    private int cardX, cardY, cardH;

    // --- input --------------------------------------------------------------------------------------
    private boolean dragging, moved;
    private double pressX, pressY;
    private long lastRelease;

    // --- the chain ----------------------------------------------------------------------------------

    private void refresh() {
        entries.clear();
        List<Node> nodes = new ArrayList<>();
        for (Entry e : ClientChain.officers) {
            if (!e.faction().equals(faction.id())) continue;
            entries.put(e.id(), e);
            if (alive(e)) nodes.add(new Node(e.id(), e.superior(), ChainRank.valueOf(e.rank()).ordinal(), e.level()));
        }
        tree = ChainTreeLayout.layout(nodes);
        if (!placed && !tree.places().isEmpty()) fitNow();
        if (selected >= 0 && !entries.containsKey(selected)) {
            selected = -1;
            if (card == Card.OFFICER) card = Card.NONE;
        }
    }

    /** The officer chosen in the book (the war table marks it), or null. */
    public static Entry selectedOfficer() {
        if (selected < 0) return null;
        for (Entry e : ClientChain.officers) if (e.id() == selected && e.faction().equals(faction.id())) return e;
        return null;
    }

    /** Opens the other chain where it was left (or whole). */
    public void showFaction(ChainFaction f) {
        if (f == faction) return;
        VIEWS.put(faction, new float[]{panX, panY, zoom});
        faction = f;
        selected = -1;
        if (card == Card.OFFICER) card = Card.NONE;
        float[] view = VIEWS.get(f);
        placed = view != null;
        if (view != null) {
            panX = view[0];
            panY = view[1];
            zoom = view[2];
        }
        gliding = false;
        refresh();
    }

    /** Chooses an officer of the open chain, glides to it and opens its record. */
    public void select(int id) {
        Place p = tree.places().get(id);
        if (p == null) return;
        selected = id;
        card = Card.OFFICER;
        // The officer glides to the middle of the page it is on; its card takes the other page.
        float z = Mth.clamp(Math.max(zoom, 1.6f), MIN_ZOOM, MAX_ZOOM);
        float pageX = bookX(p.x()) < CX ? X0 + BookStyle.SPREAD_W / 4f : X1 - BookStyle.SPREAD_W / 4f;
        glide(p.x() - (pageX - CX) / z, p.y(), z);
    }

    public void openEvents() {
        card = card == Card.EVENTS ? Card.NONE : Card.EVENTS;
    }

    // --- the camera (as the roadmap's) --------------------------------------------------------------

    private void jump(float x, float y, float z) {
        zoom = Mth.clamp(z, MIN_ZOOM, MAX_ZOOM);
        panX = x;
        panY = y;
        clampPan();
        gliding = false;
        placed = true;
    }

    private void glide(float x, float y, float z) {
        goalX = x;
        goalY = y;
        goalZoom = Mth.clamp(z, MIN_ZOOM, MAX_ZOOM);
        gliding = true;
        placed = true;
        lastFrame = Util.getMillis();
    }

    private float fitZoom() {
        float w = tree.maxX() - tree.minX() + ChainTreeLayout.GX + 8, h = tree.maxY() - tree.minY() + ChainTreeLayout.GY + 24;
        return Mth.clamp(Math.min((BookStyle.SPREAD_W - 24) / w, (BookStyle.SPREAD_H - 56) / h), MIN_ZOOM, 1.6f);
    }

    public void fit() {
        glide((tree.minX() + tree.maxX()) / 2f, (tree.minY() + tree.maxY()) / 2f, fitZoom());
    }

    public void fitNow() {
        jump((tree.minX() + tree.maxX()) / 2f, (tree.minY() + tree.maxY()) / 2f, fitZoom());
    }

    private void zoomAt(double mx, double my, float to) {
        to = Mth.clamp(to, MIN_ZOOM, MAX_ZOOM);
        float wx = worldX(mx), wy = worldY(my);
        zoom = to;
        panX = (float) (wx - (mx - CX) / zoom);
        panY = (float) (wy - (my - CY) / zoom);
        clampPan();
        gliding = false;
    }

    private void clampPan() {
        panX = Mth.clamp(panX, tree.minX() - 40, tree.maxX() + 40);
        panY = Mth.clamp(panY, tree.minY() - 40, tree.maxY() + 40);
    }

    private void tickGlide() {
        if (!gliding) return;
        long now = Util.getMillis();
        float dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        float k = 1 - (float) Math.exp(-dt * 9);
        panX += (goalX - panX) * k;
        panY += (goalY - panY) * k;
        zoom += (goalZoom - zoom) * k;
        if (Math.abs(goalX - panX) < 0.3f && Math.abs(goalY - panY) < 0.3f && Math.abs(goalZoom - zoom) < 0.003f) {
            panX = goalX;
            panY = goalY;
            zoom = goalZoom;
            gliding = false;
        }
        clampPan();
    }

    private static float worldX(double bookX) {
        return (float) (panX + (bookX - CX) / zoom);
    }

    private static float worldY(double bookY) {
        return (float) (panY + (bookY - CY) / zoom);
    }

    private static float bookX(float worldX) {
        return CX + (worldX - panX) * zoom;
    }

    private static float bookY(float worldY) {
        return CY + (worldY - panY) * zoom;
    }

    private static boolean onCanvas(double mx, double my) {
        return mx >= X0 && mx < X1 && my >= Y0 && my < Y1;
    }

    @Override
    public boolean coversPages() {
        return true;
    }

    @Override
    public void hidden() {
        dragging = false;
    }

    // --- drawing ------------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        refresh();
        tickGlide();
        boolean host = faction == ChainFaction.ANGEL;
        book.scissor(g, X0, Y0, X1, Y1);
        drawPaper(g, host);
        Place hovered = overFurniture(mx, my) || overCard(mx, my) ? null : hovered(mx, my);

        if (tree.places().isEmpty()) {
            Component empty = Component.translatable(K + "empty");
            for (FormattedCharSequence line : font.split(empty, 220)) {
                g.drawString(font, line, (int) CX - font.width(line) / 2, (int) CY - 4, BookStyle.FADED, false);
            }
        }
        var pose = g.pose();
        pose.pushPose();
        pose.translate(CX, CY, 0);
        pose.scale(zoom, zoom, 1);
        pose.translate(-panX, -panY, 0);
        for (Place p : tree.places().values()) if (p.parent() >= 0) drawLink(g, tree.places().get(p.parent()), p, host);
        pose.popPose();

        for (Place p : tree.places().values()) drawOfficer(g, p, p == hovered, mx, my, partial);
        if (zoom >= LABEL_ZOOM) for (Place p : tree.places().values()) drawLabel(g, p);

        pose.pushPose();
        pose.translate(0, 0, Z_VIGNETTE);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.blit(host ? ChainAtlas.FRAME_HOST : ChainAtlas.FRAME_PIT, X0, Y0, 0, 0, ChainAtlas.FRAME_W, ChainAtlas.FRAME_H,
                ChainAtlas.FRAME_SHEET_W, ChainAtlas.FRAME_SHEET_H);
        pose.translate(0, 0, Z_FURNITURE - Z_VIGNETTE);
        drawFurniture(g, mx, my);
        pose.popPose();
        g.disableScissor();

        if (hovered != null && card == Card.NONE) {
            Entry e = entries.get(hovered.node().id());
            if (e != null) book.tooltip(List.of(title(e), name(e)));
        }
    }

    private void drawPaper(GuiGraphics g, boolean host) {
        int p = ChainAtlas.PAPER;
        int ox = Math.floorMod(Math.round(-panX * zoom), p), oy = Math.floorMod(Math.round(-panY * zoom), p);
        for (int x = X0 - p + ox; x < X1; x += p) {
            for (int y = Y0 - p + oy; y < Y1; y += p) {
                g.blit(host ? ChainAtlas.PAPER_HOST : ChainAtlas.PAPER_PIT, x, y, 0, 0, p, p, p, p);
            }
        }
    }

    /** An elbow from the superior down to the officer: down, across, down. A Fallen angel's last link is broken. */
    private void drawLink(GuiGraphics g, Place from, Place to, boolean host) {
        Entry e = entries.get(to.node().id());
        boolean fallen = e != null && "FALLEN".equals(e.branch());
        boolean cord = host && !fallen;
        Sprite h = cord ? ChainAtlas.CORD_H : ChainAtlas.CHAIN_H, v = cord ? ChainAtlas.CORD_V : ChainAtlas.CHAIN_V;
        int fx = Math.round(from.x()), fy = Math.round(from.y()), tx = Math.round(to.x()), ty = Math.round(to.y());
        // The drop leaves from under the superior's name, and the bar runs just above the subordinates.
        int mid = ty - HALF - 8;
        vline(g, v, fx, fy + HALF + labelGap(), mid);
        hline(g, h, Math.min(fx, tx), Math.max(fx, tx), mid);
        vline(g, v, tx, mid, ty - HALF + 2);
        if (fallen) {
            Sprite b = ChainAtlas.BROKEN;
            b.draw(g, tx - b.w() / 2, (mid + ty - HALF) / 2 - b.h() / 2);
        }
    }

    /** Map units the name line takes under a medallion (none when names are hidden). */
    private static int labelGap() {
        return zoom < LABEL_ZOOM ? -2 : Math.round(10 * labelScale() / zoom) + 2;
    }

    private static float labelScale() {
        return Mth.clamp(zoom * 0.75f, 0.55f, 1f);
    }

    private static void hline(GuiGraphics g, Sprite s, int x0, int x1, int y) {
        int top = y - s.h() / 2;
        for (int x = x0; x < x1; x += s.w()) {
            g.blit(s.texture(), x, top, s.u(), s.v(), Math.min(s.w(), x1 - x), s.h(), s.sheet(), s.sheet());
        }
    }

    private static void vline(GuiGraphics g, Sprite s, int x, int y0, int y1) {
        int left = x - s.w() / 2;
        for (int y = y0; y < y1; y += s.h()) {
            g.blit(s.texture(), left, y, s.u(), s.v(), s.w(), Math.min(s.h(), y1 - y), s.sheet(), s.sheet());
        }
    }

    /** The backdrop, the officer's mob and the frame over both, in book space so the mob is lit and clipped like an icon. */
    private void drawOfficer(GuiGraphics g, Place p, boolean hovered, int mx, int my, float partial) {
        Entry e = entries.get(p.node().id());
        if (e == null) return;
        float bx = bookX(p.x()), by = bookY(p.y()), half = HALF * zoom;
        if (bx + half < X0 || bx - half > X1 || by + half + 20 < Y0 || by - half > Y1) return;
        int kind = kind(e), rank = ChainRank.valueOf(e.rank()).ordinal();
        var pose = g.pose();
        pose.pushPose();
        pose.translate(bx, by, 0);
        pose.scale(zoom, zoom, 1);
        Sprite back = ChainAtlas.backdrop(kind);
        back.draw(g, -back.w() / 2, -back.h() / 2);
        pose.popPose();

        if (zoom >= MODEL_ZOOM) drawModel(g, e, bx, by, hovered, mx, my, partial);

        pose.pushPose();
        pose.translate(bx, by, Z_FRAME);
        pose.scale(zoom, zoom, 1);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        ChainAtlas.medallion(kind, rank).draw(g, -HALF, -HALF);
        if (zoom < MODEL_ZOOM) {
            String numeral = Ranks.roman(rank + 1);
            g.drawString(font, numeral, -font.width(numeral) / 2, -4, rank == ChainRank.LORD.ordinal() ? BookStyle.GOLD : 0xFFE8D8B0, false);
        }
        if (p.node().id() == selected) ChainAtlas.SELECTED.draw(g, -HALF, -HALF);
        else if (hovered) ChainAtlas.HOVER.draw(g, -HALF, -HALF);
        pose.popPose();
    }

    private void drawModel(GuiGraphics g, Entry e, float bx, float by, boolean hovered, int mx, int my, float partial) {
        LivingEntity puppet = OfficerPuppets.of(e);
        if (puppet == null) return;
        float r = WINDOW * zoom;
        g.enableScissor(book.toScreenX(bx - r), book.toScreenY(by - r), book.toScreenX(bx + r), book.toScreenY(by + r));
        float height = puppet.getBbHeight();
        // A bust: the head and shoulders fill the window.
        float scale = 2 * WINDOW * zoom / (0.6f * height);
        float time = puppet.tickCount + partial;
        float yaw, pitch;
        if (hovered) {
            yaw = Mth.clamp((bx - mx) / (24 * zoom), -1, 1) * 40;
            pitch = -Mth.clamp((by - my) / (24 * zoom), -1, 1) * 15;
        } else {
            yaw = Mth.sin(time * 0.035f + e.id() * 1.7f) * 22;
            pitch = 0;
        }
        puppet.yBodyRot = puppet.yBodyRotO = 180 + yaw * 0.5f;
        puppet.setYRot(180 + yaw);
        puppet.yRotO = puppet.getYRot();
        puppet.yHeadRot = puppet.yHeadRotO = 180 + yaw;
        puppet.setXRot(pitch);
        puppet.xRotO = pitch;
        InventoryScreen.renderEntityInInventory(g, bx, by, scale, new Vector3f(0, height * 0.76f, 0),
                new Quaternionf().rotateZ((float) Math.PI), null, puppet);
        g.disableScissor();
    }

    /** The officer's numeral and name under its medallion, and how many times it has killed the reader. */
    private void drawLabel(GuiGraphics g, Place p) {
        Entry e = entries.get(p.node().id());
        if (e == null) return;
        float bx = bookX(p.x()), top = bookY(p.y()) + (HALF + 1) * zoom + 1;
        if (bx < X0 - 60 || bx > X1 + 60 || top > Y1 || top < Y0 - 20) return;
        float ls = labelScale();
        Component label = Component.literal(Ranks.roman(ChainRank.valueOf(e.rank()).ordinal() + 1) + " ").withColor(rankColour(e))
                .append(name(e).copy().withColor(e.kills() > 0 ? BookStyle.BLOOD : BookStyle.INK));
        var pose = g.pose();
        pose.pushPose();
        pose.translate(bx, top, Z_FRAME);
        pose.scale(ls, ls, 1);
        // One line, so the chain below can leave from under it: numeral, name and, if it has killed the reader, a tally.
        Sprite k = ChainAtlas.KILL;
        String n = e.kills() > 0 ? "×" + e.kills() : "";
        int w = font.width(label) + (n.isEmpty() ? 0 : 3 + k.w() + font.width(n));
        int x = -w / 2;
        g.drawString(font, label, x, 0, BookStyle.INK, false);
        if (!n.isEmpty()) {
            x += font.width(label) + 3;
            k.draw(g, x, -1);
            g.drawString(font, n, x + k.w(), 0, BookStyle.BLOOD, false);
        }
        pose.popPose();
    }

    // --- furniture: the faction switch, the events scroll and the whole-chain button ---------------

    private static int pitX() {
        return X0 + 12;
    }

    private static int hostX() {
        return X0 + 12 + 64 + 4;
    }

    private static int eventsX() {
        return X1 - 12 - 20;
    }

    private static int fitX() {
        return X1 - 12 - 20 - 4 - 20;
    }

    private void drawFurniture(GuiGraphics g, int mx, int my) {
        for (ChainFaction f : ChainFaction.values()) {
            boolean host = f == ChainFaction.ANGEL, on = f == faction;
            int x = host ? hostX() : pitX();
            boolean over = inside(mx, my, x, BUTTON_Y, 64, 16);
            ChainAtlas.factionButton(host, on || over).draw(g, x, BUTTON_Y);
            Component t = Component.translatable(K + f.id());
            // Pale ink on the Pit's dark leather, dark ink on the Host's ivory.
            int col = host ? (on ? BookStyle.INK : BookStyle.FADED) : (on ? 0xFFF2C9A0 : 0xFFB8A890);
            g.drawString(font, t, x + (64 - font.width(t)) / 2, BUTTON_Y + 4, col, !host);
        }
        boolean overEvents = inside(mx, my, eventsX(), BUTTON_Y, 20, 16), overFit = inside(mx, my, fitX(), BUTTON_Y, 20, 16);
        ChainAtlas.eventsButton(card == Card.EVENTS || overEvents).draw(g, eventsX(), BUTTON_Y);
        ChainAtlas.recenterButton(overFit).draw(g, fitX(), BUTTON_Y);
        if (overEvents) book.tooltip(List.of(Component.translatable(K + "history")));
        if (overFit) book.tooltip(List.of(Component.translatable(K + "recenter")));
    }

    private boolean overFurniture(double mx, double my) {
        return inside(mx, my, pitX(), BUTTON_Y, 132, 16) || inside(mx, my, fitX(), BUTTON_Y, 44, 16);
    }

    // --- cards --------------------------------------------------------------------------------------

    @Override
    public void renderOverlay(GuiGraphics g, int mx, int my, float partial) {
        var pose = g.pose();
        // The vacancies on a strip of parchment, so no chain runs over them.
        pose.pushPose();
        pose.translate(0, 0, Z_CARD - 20);
        Component vacant = vacancies();
        int vw = font.width(vacant) + 14;
        nine(g, ChainAtlas.CARD, X0 + 24, Y1 - 30, vw, 18, ChainAtlas.CARD_CORNER);
        g.drawString(font, vacant, X0 + 31, Y1 - 25, BookStyle.FADED, false);
        pose.popPose();
        if (card == Card.NONE) return;
        pose.pushPose();
        pose.translate(0, 0, Z_CARD);
        List<Line> lines = card == Card.OFFICER ? officerLines() : eventLines();
        if (lines.isEmpty()) {
            pose.popPose();
            return;
        }
        int h = 12;
        for (Line l : lines) h += l.height();
        cardH = Math.min(h, BookStyle.SPREAD_H - 40);
        // The card takes the page the officer is not on.
        Place p = card == Card.OFFICER ? tree.places().get(selected) : null;
        boolean right = p == null || (gliding ? CX + (p.x() - goalX) * goalZoom : bookX(p.x())) < CX;
        cardX = right ? X1 - CARD_W - 14 : X0 + 14;
        cardY = Y0 + 30;
        nine(g, ChainAtlas.CARD, cardX, cardY, CARD_W, cardH, ChainAtlas.CARD_CORNER);
        book.scissor(g, cardX, cardY, cardX + CARD_W, cardY + cardH - 4);
        int y = cardY + 8;
        for (Line l : lines) {
            if (l.scale() != 1) {
                pose.pushPose();
                pose.translate(cardX + 10, y, 0);
                pose.scale(l.scale(), l.scale(), 1);
                g.drawString(font, l.text(), 0, 0, l.colour(), false);
                pose.popPose();
            } else {
                g.drawString(font, l.text(), cardX + 10, y, l.colour(), false);
            }
            y += l.height();
        }
        g.disableScissor();
        pose.popPose();
    }

    private record Line(FormattedCharSequence text, int colour, float scale, int height) {
    }

    private void wrap(List<Line> out, Component c, int colour) {
        for (FormattedCharSequence s : font.split(c, CARD_W - 20)) out.add(new Line(s, colour, 1, 10));
    }

    private List<Line> officerLines() {
        Entry e = entries.get(selected);
        List<Line> out = new ArrayList<>();
        if (e == null) return out;
        ChainFaction f = ChainFaction.byId(e.faction());
        boolean fallen = "FALLEN".equals(e.branch());
        out.add(new Line(name(e).getVisualOrderText(), rankColour(e), 1.5f, 15));
        wrap(out, title(e), BookStyle.FADED);
        out.add(new Line(FormattedCharSequence.EMPTY, 0, 1, 4));
        wrap(out, Component.translatable(K + "level", e.level()), BookStyle.INK);
        Entry sup = entries.get(e.superior());
        if (sup != null) wrap(out, Component.translatable(K + "answers_to", name(sup)), BookStyle.INK);
        int under = 0;
        for (Entry o : entries.values()) if (o.superior() == e.id() && alive(o)) under++;
        if (under > 0) wrap(out, Component.translatable(K + "subordinates", under), BookStyle.INK);
        if (alive(e)) {
            boolean seated = ChainRank.valueOf(e.rank()).seated();
            wrap(out, Component.translatable(K + (seated ? "seat" : "territory"), region(OVERWORLD, e.homeX(), e.homeZ())), BookStyle.INK);
        }
        if (e.seenDay() >= 0) wrap(out, Component.translatable(K + "seen", e.seenDay(), region(e.seenDimension(), e.seenX(), e.seenZ())), BookStyle.FADED);
        if (!alive(e)) wrap(out, Component.translatable(K + "status." + e.status().toLowerCase(Locale.ROOT)), BookStyle.FADED);
        if (fallen) wrap(out, Component.translatable(K + "fallen"), 0xFF5A5450);
        if (e.scars() > 0) wrap(out, Component.translatable(K + "scars", e.scars()), BookStyle.INK);
        if (e.kills() > 0) wrap(out, Component.translatable(K + "kills", e.kills()), BookStyle.BLOOD);
        List<History> mine = new ArrayList<>();
        for (History h : ClientChain.history) if (h.faction().equals(f.id()) && h.officerId() == e.id()) mine.add(h);
        if (!mine.isEmpty()) {
            out.add(new Line(FormattedCharSequence.EMPTY, 0, 1, 4));
            wrap(out, Component.translatable(K + "history"), BookStyle.GOLD);
            for (int i = mine.size() - 1; i >= 0 && i >= mine.size() - 5; i--) wrap(out, event(mine.get(i)), BookStyle.INK);
        }
        return out;
    }

    private static final String OVERWORLD = "minecraft:overworld";
    /** Rounding of the book's distances: a region, not a place (the war table gives the place). */
    private static final int REGION = 100;

    /** "about 1200 blocks north-east", "close by", or "in another realm" when the hunter is not in that dimension. */
    private static Component region(String dimension, int x, int z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.level().dimension().location().toString().equals(dimension))
            return Component.translatable(K + "elsewhere");
        double dist = Math.hypot(x - mc.player.getX(), z - mc.player.getZ());
        if (dist < REGION) return Component.translatable(K + "near");
        return Component.translatable(K + "away", Math.round(dist / REGION) * REGION, Component.translatable("screen.supernaturalcraft.case.dir."
                + CompassMath.point(CompassMath.bearing(mc.player.getX(), mc.player.getZ(), x, z))));
    }

    private List<Line> eventLines() {
        List<Line> out = new ArrayList<>();
        var court = ClientChain.court(faction.id());
        if (court != null && court.vacantUntil() >= 0) {
            wrap(out, Component.translatable(K + "interregnum", court.vacantUntil()), BookStyle.BLOOD);
            out.add(new Line(FormattedCharSequence.EMPTY, 0, 1, 4));
        }
        wrap(out, Component.translatable(K + "history"), BookStyle.GOLD);
        int n = 0;
        for (int i = ClientChain.history.size() - 1; i >= 0 && n < 9; i--) {
            History h = ClientChain.history.get(i);
            if (!h.faction().equals(faction.id())) continue;
            wrap(out, event(h), BookStyle.INK);
            out.add(new Line(FormattedCharSequence.EMPTY, 0, 1, 2));
            n++;
        }
        List<Entry> away = new ArrayList<>();
        for (Entry e : entries.values()) if ("RETURNING".equals(e.status())) away.add(e);
        if (!away.isEmpty()) {
            out.add(new Line(FormattedCharSequence.EMPTY, 0, 1, 4));
            wrap(out, Component.translatable(K + "absent"), BookStyle.GOLD);
            away.sort(java.util.Comparator.comparingInt((Entry e) -> -ChainRank.valueOf(e.rank()).ordinal()).thenComparingInt(Entry::id));
            for (Entry e : away) wrap(out, title(e).copy().append(" ").append(name(e)), BookStyle.FADED);
        }
        return out;
    }

    private Component event(History h) {
        Entry who = entries.get(h.officerId());
        return Component.translatable(K + "day", h.day()).append(": ").append(Component.translatable(
                "chain.supernaturalcraft.event." + h.key(), who == null ? Component.literal("?") : name(who)));
    }

    private static void nine(GuiGraphics g, Sprite s, int x, int y, int w, int h, int b) {
        var t = s.texture();
        int u = s.u(), v = s.v(), sh = s.sheet(), mw = s.w() - 2 * b, mh = s.h() - 2 * b, iw = w - 2 * b, ih = h - 2 * b;
        g.blit(t, x, y, u, v, b, b, sh, sh);
        g.blit(t, x + w - b, y, u + s.w() - b, v, b, b, sh, sh);
        g.blit(t, x, y + h - b, u, v + s.h() - b, b, b, sh, sh);
        g.blit(t, x + w - b, y + h - b, u + s.w() - b, v + s.h() - b, b, b, sh, sh);
        g.blit(t, x + b, y, iw, b, u + b, v, mw, b, sh, sh);
        g.blit(t, x + b, y + h - b, iw, b, u + b, v + s.h() - b, mw, b, sh, sh);
        g.blit(t, x, y + b, b, ih, u, v + b, b, mh, sh, sh);
        g.blit(t, x + w - b, y + b, b, ih, u + s.w() - b, v + b, b, mh, sh, sh);
        g.blit(t, x + b, y + b, iw, ih, u + b, v + b, mw, mh, sh, sh);
    }

    private boolean overCard(double mx, double my) {
        return card != Card.NONE && inside(mx, my, cardX, cardY, CARD_W, cardH);
    }

    // --- input --------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        for (ChainFaction f : ChainFaction.values()) {
            if (inside(mx, my, f == ChainFaction.ANGEL ? hostX() : pitX(), BUTTON_Y, 64, 16)) {
                if (f != faction) click(SoundEvents.BOOK_PAGE_TURN);
                showFaction(f);
                return true;
            }
        }
        if (inside(mx, my, eventsX(), BUTTON_Y, 20, 16)) {
            click(SoundEvents.UI_BUTTON_CLICK.value());
            openEvents();
            return true;
        }
        if (inside(mx, my, fitX(), BUTTON_Y, 20, 16)) {
            click(SoundEvents.UI_BUTTON_CLICK.value());
            fit();
            return true;
        }
        if (overCard(mx, my)) return true;
        if (!onCanvas(mx, my)) return false;
        dragging = true;
        moved = false;
        pressX = mx;
        pressY = my;
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (!dragging || button != 0) return false;
        if (!moved && Math.abs(mx - pressX) + Math.abs(my - pressY) > 3) moved = true;
        if (moved) {
            panX -= (float) (dx / zoom);
            panY -= (float) (dy / zoom);
            clampPan();
            gliding = false;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (!dragging || button != 0) return false;
        dragging = false;
        if (moved) return true;
        Place p = hovered((int) Math.floor(mx), (int) Math.floor(my));
        if (p != null) {
            click(SoundEvents.BOOK_PAGE_TURN);
            if (p.node().id() == selected && card == Card.OFFICER) {
                selected = -1;
                card = Card.NONE;
            } else {
                select(p.node().id());
            }
        } else if (card != Card.NONE) {
            card = Card.NONE;
            selected = -1;
        } else if (Util.getMillis() - lastRelease < 300) {
            fit();
        }
        lastRelease = Util.getMillis();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (overCard(mx, my) || !onCanvas(mx, my) || sy == 0) return false;
        zoomAt(mx, my, zoom * (float) Math.pow(1.15, sy));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE && card != Card.NONE) {
            card = Card.NONE;
            selected = -1;
            return true;
        }
        return false;
    }

    private void click(net.minecraft.sounds.SoundEvent sound) {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0f));
    }

    private Place hovered(int mx, int my) {
        if (!onCanvas(mx, my) || dragging && moved) return null;
        float wx = worldX(mx), wy = worldY(my);
        for (Place p : tree.places().values()) {
            if (Math.abs(wx - p.x()) <= HALF && Math.abs(wy - p.y()) <= HALF) return p;
        }
        return null;
    }

    // --- data -----------------------------------------------------------------------------------

    private static boolean alive(Entry e) {
        return "ALIVE".equals(e.status());
    }

    /** 0 the Pit, 1 the Host, 2 a Fallen angel. */
    private static int kind(Entry e) {
        if ("demon".equals(e.faction())) return 0;
        return "FALLEN".equals(e.branch()) ? 2 : 1;
    }

    private Component vacancies() {
        int[] alive = new int[ChainRank.values().length];
        for (Entry e : entries.values()) if (alive(e)) alive[ChainRank.valueOf(e.rank()).ordinal()]++;
        Component out = Component.translatable(K + "vacant").append(":");
        for (ChainRank r : ChainRank.values()) {
            out = out.copy().append("  " + Ranks.roman(r.ordinal() + 1) + " " + Math.max(0, r.cap() - alive[r.ordinal()]));
        }
        return out;
    }

    private static Component name(Entry e) {
        return Component.literal(ChainNames.name(ChainFaction.byId(e.faction()), e.nameSeed()));
    }

    /** "Captain of the Pit": the rank and the epithet. */
    private static Component title(Entry e) {
        ChainFaction f = ChainFaction.byId(e.faction());
        ChainFaction.Branch branch = "FALLEN".equals(e.branch()) ? ChainFaction.Branch.FALLEN : ChainFaction.Branch.LOYAL;
        ChainRank rank = ChainRank.valueOf(e.rank());
        return Component.translatable("chain.supernaturalcraft.rank." + e.rank().toLowerCase(Locale.ROOT)).append(" ")
                .append(Component.translatable("chain.supernaturalcraft.epithet." + ChainNames.epithet(f, branch, rank)));
    }

    private static int rankColour(Entry e) {
        if (!alive(e)) return BookStyle.FADED;
        if ("FALLEN".equals(e.branch())) return 0xFF5A5450;
        return ChainRank.valueOf(e.rank()) == ChainRank.LORD ? BookStyle.GOLD
                : "angel".equals(e.faction()) ? BookStyle.MANA : BookStyle.BLOOD;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // --- preview ------------------------------------------------------------------------------------

    @Override
    public List<PreviewShot> previewShots() {
        return List.of(
                new PreviewShot("pit", b -> {
                    ChainSection s = (ChainSection) b.section();
                    s.showFaction(ChainFaction.DEMON);
                    card = Card.NONE;
                    s.refresh();
                    s.fitNow();
                }),
                new PreviewShot("host", b -> {
                    ChainSection s = (ChainSection) b.section();
                    s.showFaction(ChainFaction.ANGEL);
                    card = Card.NONE;
                    s.refresh();
                    s.fitNow();
                }));
    }

    /** {@code SN_PREVIEW=chain}: the officer of the open chain at this rank (the first found), or -1. */
    public int firstOfRank(ChainRank rank) {
        refresh();
        for (Place p : tree.places().values()) if (p.node().rank() == rank.ordinal()) return p.node().id();
        return -1;
    }

    /** {@code SN_PREVIEW=chain}: jumps (no glide) to an officer at a zoom. */
    public void jumpTo(int id, float z) {
        Place p = tree.places().get(id);
        if (p != null) jump(p.x(), p.y(), z);
    }

    /** {@code SN_PREVIEW=chain}: finishes any glide at once. */
    public void settle() {
        if (gliding) jump(goalX, goalY, goalZoom);
    }
}
