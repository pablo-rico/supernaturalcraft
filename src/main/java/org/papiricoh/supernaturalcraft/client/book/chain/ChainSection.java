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
 * officer for its record ({@link OfficerModal}); the scroll button opens the chain's recent events and who may yet return, the
 * links button draws the officers' notable bonds over the tree, and the banner opens the front ({@link WarPanel}). A post a
 * hunter holds shows their face, the reader's own ringed in sky-blue. Everything is everyone's to read: the chain is shared by
 * the whole world.
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

    private enum Card { NONE, EVENTS }

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
    /** Whether the bonds are drawn over the tree. */
    private static boolean bondsShown;
    /** The record or the front, open over the spread, or null. */
    private ChainPanel panel;

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
        // A guild that has dissolved (its slot freed) is no longer there to look at.
        if (faction.guild() && ClientChain.guild(faction.id()) == null) {
            showFaction(ChainFaction.DEMON);
            return;
        }
        if (!placed && !tree.places().isEmpty()) fitNow();
        if (selected >= 0 && !entries.containsKey(selected)) selected = -1;
        if (panel != null && panel.stale()) panel = null;
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
        Entry e = entries.get(id);
        if (e == null) return;
        openOfficer(e);
    }

    /** Opens an officer's record (of either chain) over the spread. */
    public void openOfficer(Entry e) {
        card = Card.NONE;
        if (panel instanceof OfficerModal m) m.show(e);
        else panel = new OfficerModal(this, book, e);
        reveal(e);
    }

    /** The tree follows the record: an officer of the open chain is chosen and the view glides to it. */
    void reveal(Entry e) {
        if (!e.faction().equals(faction.id())) {
            selected = -1;
            return;
        }
        selected = e.id();
        Place p = tree.places().get(e.id());
        if (p != null) glide(p.x(), p.y(), Mth.clamp(Math.max(zoom, 1.2f), MIN_ZOOM, MAX_ZOOM));
    }

    /** The open record, or null. */
    public OfficerModal.Tab modalTab(OfficerModal.Tab t, boolean ordering) {
        if (panel instanceof OfficerModal m) m.preview(t, ordering);
        return t;
    }

    public void openWar() {
        card = Card.NONE;
        panel = new WarPanel(this, book, faction);
    }

    /** Opens a guild's record ({@link GuildPanel}) over the spread. */
    public void openGuild(String slot) {
        if (ClientChain.guild(slot) == null) return;
        card = Card.NONE;
        panel = new GuildPanel(this, book, slot);
    }

    /** For previews: the open guild record, or null. */
    public @org.jetbrains.annotations.Nullable GuildPanel guildPanel() {
        return panel instanceof GuildPanel p ? p : null;
    }

    /** Hands the keyboard to the open panel's text field (the book only sends typed characters to a focused widget). */
    void takeKeyboard(boolean on) {
        if (sink != null) book.setFocused(on ? sink : null);
    }

    /** The invisible widget that holds the book's keyboard focus while a panel's field is being written in. */
    private KeySink sink;

    @Override
    public void init() {
        sink = book.add(new KeySink());
    }

    private final class KeySink extends net.minecraft.client.gui.components.AbstractWidget {
        KeySink() {
            super(-100, -100, 0, 0, Component.empty());
        }

        @Override
        public boolean charTyped(char c, int modifiers) {
            return panel != null && panel.charTyped(c);
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput out) {
        }
    }

    public void showBonds(boolean on) {
        bondsShown = on;
    }

    void closePanel() {
        panel = null;
        selected = -1;
        takeKeyboard(false);
    }

    public boolean panelOpen() {
        return panel != null;
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
        panel = null;
        sink = null;
    }

    // --- drawing ------------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        refresh();
        tickGlide();
        book.scissor(g, X0, Y0, X1, Y1);
        drawPaper(g);
        Place hovered = panel != null || overFurniture(mx, my) || overCard(mx, my) ? null : hovered(mx, my);

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
        for (Place p : tree.places().values()) if (p.parent() >= 0) drawLink(g, tree.places().get(p.parent()), p);
        if (bondsShown) drawBonds(g);
        pose.popPose();

        for (Place p : tree.places().values()) drawOfficer(g, p, p == hovered, mx, my, partial);
        if (zoom >= LABEL_ZOOM) for (Place p : tree.places().values()) drawLabel(g, p);

        pose.pushPose();
        pose.translate(0, 0, Z_VIGNETTE);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        ChainAtlas.tint(g, ChainAtlas.side(faction));
        g.blit(ChainAtlas.frame(faction), X0, Y0, 0, 0, ChainAtlas.FRAME_W, ChainAtlas.FRAME_H,
                ChainAtlas.FRAME_SHEET_W, ChainAtlas.FRAME_SHEET_H);
        ChainAtlas.untint(g, ChainAtlas.side(faction));
        pose.translate(0, 0, Z_FURNITURE - Z_VIGNETTE);
        drawFurniture(g, mx, my);
        pose.popPose();
        g.disableScissor();

        if (hovered != null && card == Card.NONE) {
            Entry e = entries.get(hovered.node().id());
            if (e != null) {
                List<Component> tip = new ArrayList<>(List.of(title(e), name(e)));
                if (bondsShown) {
                    for (var b : ClientChain.bondsOf(ClientChain.actor(e))) {
                        if (!b.to().equals(ClientChain.me())) continue;
                        tip.add(Component.translatable(K + "bond.toward_you", ClientChain.kindName(ClientChain.kind(b)))
                                .withStyle(net.minecraft.ChatFormatting.RED));
                    }
                }
                book.tooltip(tip);
            }
        }
    }

    private void drawPaper(GuiGraphics g) {
        int p = ChainAtlas.PAPER;
        int ox = Math.floorMod(Math.round(-panX * zoom), p), oy = Math.floorMod(Math.round(-panY * zoom), p);
        int side = ChainAtlas.side(faction);
        ChainAtlas.tintPale(g, side);
        for (int x = X0 - p + ox; x < X1; x += p) {
            for (int y = Y0 - p + oy; y < Y1; y += p) {
                g.blit(ChainAtlas.paper(faction), x, y, 0, 0, p, p, p, p);
            }
        }
        ChainAtlas.untint(g, side);
    }

    /** An elbow from the superior down to the officer: down, across, down. A Fallen angel's last link is broken. */
    private void drawLink(GuiGraphics g, Place from, Place to) {
        Entry e = entries.get(to.node().id());
        boolean fallen = e != null && "FALLEN".equals(e.branch());
        boolean cord = faction == ChainFaction.ANGEL && !fallen;
        Sprite h = cord ? ChainAtlas.CORD_H : ChainAtlas.CHAIN_H, v = cord ? ChainAtlas.CORD_V : ChainAtlas.CHAIN_V;
        if (faction == ChainFaction.LEVIATHAN || faction.guild()) {
            // SucroCorp's strands of goo (a guild's chain, in its colour), in the chain's slots of its own sheet.
            var sheet = ChainAtlas.widgets(ChainAtlas.side(faction));
            h = new Sprite(sheet, h.u(), h.v(), h.w(), h.h(), 256);
            v = new Sprite(sheet, v.u(), v.v(), v.w(), v.h(), 256);
        }
        int side = ChainAtlas.side(faction);
        ChainAtlas.tint(g, side);
        int fx = Math.round(from.x()), fy = Math.round(from.y()), tx = Math.round(to.x()), ty = Math.round(to.y());
        // The drop leaves from under the superior's name and the bar runs just above the row below it; a link that skips
        // rows goes on down at the subordinate's own column (its subtree's), never through anyone else's family.
        int mid = Math.min(ty, fy + Math.round(ChainTreeLayout.GY)) - HALF - 8;
        vline(g, v, fx, fy + HALF + labelGap(), mid);
        hline(g, h, Math.min(fx, tx), Math.max(fx, tx), mid);
        vline(g, v, tx, mid, ty - HALF + 2);
        ChainAtlas.untint(g, side);
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
        ChainAtlas.draw(g, kind, back, -back.w() / 2, -back.h() / 2);
        pose.popPose();

        boolean hunter = !e.player().isEmpty();
        if (hunter) drawFace(g, e, bx, by);
        else if (zoom >= MODEL_ZOOM) drawModel(g, e, bx, by, hovered, mx, my, partial);

        pose.pushPose();
        pose.translate(bx, by, Z_FRAME);
        pose.scale(zoom, zoom, 1);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        ChainAtlas.draw(g, kind, ChainAtlas.medallion(kind, rank), -HALF, -HALF);
        if (zoom < MODEL_ZOOM && !hunter && ChainAtlas.guild(kind)) {
            ChainAtlas.emblem(g, ClientChain.emblem(e.faction()), -8, -8, 16, 1f);
        } else if (zoom < MODEL_ZOOM && !hunter) {
            String numeral = Ranks.roman(rank + 1);
            g.drawString(font, numeral, -font.width(numeral) / 2, -4, rank == ChainRank.LORD.ordinal() ? BookStyle.GOLD : 0xFFE8D8B0, false);
        }
        if (hunter) (("player:" + e.player()).equals(ClientChain.me()) ? ChainAtlas.MY_RIM : ChainAtlas.HUNTER_RIM).draw(g, -HALF, -HALF);
        if (p.node().id() == selected) ChainAtlas.SELECTED.draw(g, -HALF, -HALF);
        else if (hovered) ChainAtlas.HOVER.draw(g, -HALF, -HALF);
        if (bondsShown) {
            // What it thinks of the reader, at its shoulder.
            for (var b : ClientChain.bondsOf(ClientChain.actor(e))) {
                if (!b.to().equals(ClientChain.me())) continue;
                Sprite icon = ChainAtlas.bond(ClientChain.kind(b));
                if (icon != null) icon.draw(g, HALF - 10, -HALF + 1);
                break;
            }
        }
        pose.popPose();
    }

    /**
     * The notable bonds between officers of this tree as bowed strokes (one per pair, the stronger kind): rivals red,
     * grudges crimson and thicker, the loyal gold, a pact gold with a link at its middle.
     */
    private void drawBonds(GuiGraphics g) {
        Map<String, org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Bond> pairs = new java.util.HashMap<>();
        for (var b : ClientChain.bonds) {
            Entry a = ClientChain.entry(b.from()), c = ClientChain.entry(b.to());
            if (a == null || c == null || !a.faction().equals(faction.id()) || !c.faction().equals(faction.id())) continue;
            if (!tree.places().containsKey(a.id()) || !tree.places().containsKey(c.id())) continue;
            if (ClientChain.kind(b) == org.papiricoh.supernaturalcraft.chain.Bonds.Kind.NONE) continue;
            String key = Math.min(a.id(), c.id()) + ":" + Math.max(a.id(), c.id());
            var old = pairs.get(key);
            if (old == null || strength(b) > strength(old)) pairs.put(key, b);
        }
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        List<float[]> links = new ArrayList<>();
        for (var b : pairs.values()) {
            Place p = tree.places().get(ClientChain.entry(b.from()).id()), q = tree.places().get(ClientChain.entry(b.to()).id());
            var kind = ClientChain.kind(b);
            int colour = switch (kind) {
                case GRUDGE -> 0xD8700A12;
                case RIVAL -> 0xC8C4352F;
                default -> 0xC8D8A930;
            };
            float width = kind == org.papiricoh.supernaturalcraft.chain.Bonds.Kind.GRUDGE ? 3f : 1.6f;
            // Bow away from the tree's own links: always to the same hand of the pair.
            float bend = p.x() <= q.x() ? -0.18f : 0.18f;
            ChainDraw.curve(g, p.x(), p.y(), q.x(), q.y(), bend, width + 1.4f, 0x50000000);
            ChainDraw.curve(g, p.x(), p.y(), q.x(), q.y(), bend, width, colour);
            if (kind == org.papiricoh.supernaturalcraft.chain.Bonds.Kind.PACT) links.add(ChainDraw.curveMid(p.x(), p.y(), q.x(), q.y(), bend));
        }
        for (float[] m : links) ChainAtlas.PACT_LINK.draw(g, Math.round(m[0]) - 4, Math.round(m[1]) - 3);
    }

    private static int strength(org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Bond b) {
        return (b.pact() ? 1000 : 0) + Math.abs(b.regard());
    }

    /** A post a hunter holds: their face fills the window. */
    private void drawFace(GuiGraphics g, Entry e, float bx, float by) {
        float r = WINDOW * zoom;
        g.enableScissor(book.toScreenX(bx - r), book.toScreenY(by - r), book.toScreenX(bx + r), book.toScreenY(by + r));
        var pose = g.pose();
        pose.pushPose();
        pose.translate(bx, by, 40);
        pose.scale(zoom, zoom, 1);
        ChainDraw.face(g, e.player(), -9, -10, 18);
        pose.popPose();
        g.disableScissor();
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
                .append(Component.literal(shortName(e)).withColor(e.kills() > 0 ? BookStyle.BLOOD : BookStyle.INK));
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

    /** A faction's button this frame: the world's own as wide as fits, the guilds' compact with their emblem. */
    private record FactionButton(ChainFaction f, int x, int w) {
    }

    private static final int GUILD_W = 22;

    /** The faction buttons, left to right in faction order, squeezed so up to seven fit before the front's button. */
    private static List<FactionButton> factionButtons() {
        List<ChainFaction> fixed = new ArrayList<>(), guilds = new ArrayList<>();
        for (ChainFaction f : ChainFaction.values()) if (listed(f)) (f.guild() ? guilds : fixed).add(f);
        int room = frontX() - 6 - (X0 + 12) - guilds.size() * (GUILD_W + 3);
        int w = Mth.clamp(room / Math.max(1, fixed.size()) - 4, 24, 64);
        List<FactionButton> out = new ArrayList<>();
        int x = X0 + 12;
        for (ChainFaction f : fixed) {
            out.add(new FactionButton(f, x, w));
            x += w + 4;
        }
        for (ChainFaction f : guilds) {
            out.add(new FactionButton(f, x, GUILD_W));
            x += GUILD_W + 3;
        }
        return out;
    }

    /** A faction's button shows once its chain has woken (the first two always: the book was written for them); a guild's once founded. */
    private static boolean listed(ChainFaction f) {
        if (f.guild()) return ClientChain.guild(f.id()) != null;
        return f.side() != ChainFaction.Side.NONE || ClientChain.officers.stream().anyMatch(e -> f.id().equals(e.faction()));
    }

    private static int eventsX() {
        return X1 - 12 - 20;
    }

    private static int fitX() {
        return X1 - 12 - 20 - 4 - 20;
    }

    private static int bondsX() {
        return fitX() - 24;
    }

    private static int frontX() {
        return fitX() - 48;
    }

    private void drawFurniture(GuiGraphics g, int mx, int my) {
        for (FactionButton b : factionButtons()) {
            ChainFaction f = b.f();
            boolean host = f == ChainFaction.ANGEL, on = f == faction;
            int x = b.x();
            boolean over = inside(mx, my, x, BUTTON_Y, b.w(), 16);
            Sprite plate = ChainAtlas.factionButton(f, on || over);
            if (f.guild()) {
                // A guild's plate in its colour, darkened, its emblem lit on it; the name in a tooltip.
                ChainAtlas.colour(g, ClientChain.mix(ClientChain.guildTint(f.id()), 0x000000, on ? 0.2f : 0.45f));
                ChainDraw.strip(g, plate.texture(), 256, 256, plate.u(), plate.v(), plate.w(), plate.h(), 6, x, BUTTON_Y, b.w());
                g.setColor(1, 1, 1, 1);
                ChainAtlas.emblem(g, ClientChain.emblem(f.id()), x + 3, BUTTON_Y, 16, on || over ? 1f : 0.8f);
                if (over && panel == null) book.tooltip(guildTip(f.id()));
                continue;
            }
            ChainDraw.strip(g, plate.texture(), 256, 256, plate.u(), plate.v(), plate.w(), plate.h(), 6, x, BUTTON_Y, b.w());
            Component t = Component.translatable(K + f.id());
            // Pale ink on the Pit's dark leather and SucroCorp's goo, dark ink on the Host's ivory.
            int col = host ? (on ? BookStyle.INK : BookStyle.FADED) : f == ChainFaction.LEVIATHAN ? (on ? 0xFFF2D58A : 0xFFB8C8C0)
                    : (on ? 0xFFF2C9A0 : 0xFFB8A890);
            int tw = font.width(t);
            if (tw <= b.w() - 6) {
                g.drawString(font, t, x + (b.w() - tw) / 2, BUTTON_Y + 4, col, !host);
            } else {
                // Squeezed beside the guilds' buttons: the label shrinks to its plate.
                float k = (b.w() - 6) / (float) tw;
                g.pose().pushPose();
                g.pose().translate(x + 3, BUTTON_Y + 4 + (1 - k) * 4, 0);
                g.pose().scale(k, k, 1);
                g.drawString(font, t, 0, 0, col, !host);
                g.pose().popPose();
            }
        }
        boolean overEvents = inside(mx, my, eventsX(), BUTTON_Y, 20, 16), overFit = inside(mx, my, fitX(), BUTTON_Y, 20, 16);
        ChainAtlas.eventsButton(card == Card.EVENTS || overEvents).draw(g, eventsX(), BUTTON_Y);
        ChainAtlas.recenterButton(overFit).draw(g, fitX(), BUTTON_Y);
        boolean overBonds = inside(mx, my, bondsX(), BUTTON_Y, 20, 16), overFront = inside(mx, my, frontX(), BUTTON_Y, 20, 16);
        ChainAtlas.bondsButton(bondsShown || overBonds).draw(g, bondsX(), BUTTON_Y);
        ChainAtlas.frontButton(panel instanceof WarPanel || overFront).draw(g, frontX(), BUTTON_Y);
        if (ClientChain.atWar()) {
            // At war: the banner's button smoulders.
            float pulse = 0.5f + 0.5f * Mth.sin(Util.getMillis() / 300f);
            g.fill(frontX() + 1, BUTTON_Y + 14, frontX() + 19, BUTTON_Y + 15, ChainDraw.argb(0.5f + 0.5f * pulse, 0xE8473A));
        }
        if (panel != null) return;
        if (overEvents) book.tooltip(List.of(Component.translatable(K + "history")));
        if (overFit) book.tooltip(List.of(Component.translatable(K + "recenter")));
        if (overBonds) book.tooltip(List.of(Component.translatable(K + (bondsShown ? "bonds.hide" : "bonds.show"))));
        if (overFront && !ClientChain.sectors()) book.tooltip(List.of(Component.translatable(K + "dominions"),
                (ClientChain.realmWars().isEmpty() ? Component.translatable(K + "war.peace")
                        : Component.translatable(K + "realm.state", ClientChain.realmWars().size())).withStyle(net.minecraft.ChatFormatting.GRAY)));
        else if (overFront) book.tooltip(List.of(Component.translatable(K + "front"),
                Component.translatable(K + (ClientChain.atWar() ? "war.until" : "war.peace"), ClientChain.war.until()).withStyle(net.minecraft.ChatFormatting.GRAY)));
    }

    private boolean overFurniture(double mx, double my) {
        for (FactionButton b : factionButtons()) if (inside(mx, my, b.x(), BUTTON_Y, b.w(), 16)) return true;
        return inside(mx, my, frontX(), BUTTON_Y, 92, 16) || overSpot(mx, my);
    }

    /** A guild's button tooltip: its name, its founder and how many it counts. */
    private static List<Component> guildTip(String slot) {
        var guild = ClientChain.guild(slot);
        List<Component> tip = new ArrayList<>();
        tip.add(ClientChain.factionName(slot).copy().withStyle(net.minecraft.ChatFormatting.BOLD));
        if (guild != null) tip.add(Component.translatable(K + "guild.founder", guild.founderName()).withStyle(net.minecraft.ChatFormatting.GRAY));
        tip.add(Component.translatable(K + "guild.members", ClientChain.members(slot).size()).withStyle(net.minecraft.ChatFormatting.GRAY));
        return tip;
    }

    // --- the foot of the page: the guild's record and the reader's invitations (v0.29) -------------

    /** Something clickable drawn over the tree outside a panel: the guild plate, an invitation's buttons. */
    private record Spot(int x, int y, int w, int h, @org.jetbrains.annotations.Nullable Runnable click, @org.jetbrains.annotations.Nullable List<Component> tip) {
    }

    private final List<Spot> spots = new ArrayList<>();

    private boolean overSpot(double mx, double my) {
        for (Spot s : spots) if (inside(mx, my, s.x(), s.y(), s.w(), s.h())) return true;
        return false;
    }

    /** The guild whose record the plate at the foot of the page opens: the one shown, else the reader's own; null for none. */
    private @org.jetbrains.annotations.Nullable String plateGuild() {
        if (faction.guild()) return faction.id();
        var mine = ClientChain.myGuild();
        return mine == null ? null : mine.slot();
    }

    /** The plate opening a guild's record (bottom right), and over it the reader's invitations while they have no guild. */
    private void drawFoot(GuiGraphics g, int mx, int my) {
        spots.clear();
        int y = Y1 - 30;
        String slot = plateGuild();
        if (slot != null) {
            Component label = faction.guild() ? Component.translatable(K + "guild.record")
                    : Component.translatable(K + "guild.mine", GuildPanel.inkName(slot));
            int w = 20 + font.width(label) + 10, x = X1 - 24 - w;
            boolean over = panel == null && inside(mx, my, x, y, w, 18);
            nine(g, ChainAtlas.CARD, x, y, w, 18, ChainAtlas.CARD_CORNER);
            ChainAtlas.emblem(g, ClientChain.emblem(slot), x + 5, y + 1, 16, 1f);
            int col = over ? BookStyle.BLOOD : BookStyle.INK;
            g.drawString(font, label, x + 23, y + 5, col, false);
            if (over) g.fill(x + 23, y + 14, x + 23 + font.width(label), y + 15, col);
            spots.add(new Spot(x, y, w, 18, () -> {
                click(SoundEvents.BOOK_PAGE_TURN);
                openGuild(slot);
            }, List.of(Component.translatable(K + "guild.open", ClientChain.factionName(slot)))));
            y -= 22;
        }
        if (ClientChain.myGuild() != null) return;
        int n = 0;
        for (var invite : ClientChain.invites) {
            if (n++ >= 3) break;
            Component text = Component.translatable(K + "guild.invited", GuildPanel.inkName(invite.slot()));
            Component yes = Component.translatable(K + "guild.accept"), no = Component.translatable(K + "guild.decline");
            int yw = font.width(yes) + 8, nw = font.width(no) + 8;
            int w = 20 + font.width(text) + 6 + yw + 2 + nw + 6, x = X1 - 24 - w;
            nine(g, ChainAtlas.CARD, x, y, w, 18, ChainAtlas.CARD_CORNER);
            ChainAtlas.emblem(g, ClientChain.emblem(invite.slot()), x + 5, y + 1, 16, 1f);
            g.drawString(font, text, x + 23, y + 5, BookStyle.INK, false);
            List<Component> tip = List.of(Component.translatable(K + "guild.invited.until", invite.until()));
            spots.add(new Spot(x, y, 20 + font.width(text), 18, null, tip));
            int bx = x + 23 + font.width(text) + 6;
            int side = ChainAtlas.side(ChainFaction.byId(invite.slot()));
            for (int i = 0; i < 2; i++) {
                int bw = i == 0 ? yw : nw;
                boolean hot = panel == null && inside(mx, my, bx, y + 2, bw, 14);
                ChainAtlas.button(g, side, hot ? 1 : 0, bx, y + 2, bw);
                g.drawString(font, i == 0 ? yes : no, bx + 4, y + 5, 0xFFF6F2EC, false);
                var action = i == 0 ? org.papiricoh.supernaturalcraft.network.GuildActionPayload.Action.ACCEPT_INVITE
                        : org.papiricoh.supernaturalcraft.network.GuildActionPayload.Action.DECLINE_INVITE;
                spots.add(new Spot(bx, y + 2, bw, 14, () -> {
                    click(SoundEvents.UI_BUTTON_CLICK.value());
                    ClientChain.act(action, invite.slot(), "", 0);
                }, List.of(i == 0 ? yes : no)));
                bx += bw + 2;
            }
            y -= 22;
        }
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
        drawFoot(g, mx, my);
        pose.popPose();
        if (panel == null && card == Card.NONE)
            for (Spot sp : spots) if (sp.tip() != null && inside(mx, my, sp.x(), sp.y(), sp.w(), sp.h())) book.tooltip(sp.tip());
        if (panel != null) {
            panel.render(g, mx, my, partial);
            return;
        }
        if (card == Card.NONE) return;
        pose.pushPose();
        pose.translate(0, 0, Z_CARD);
        List<Line> lines = eventLines();
        if (lines.isEmpty()) {
            pose.popPose();
            return;
        }
        int h = 12;
        for (Line l : lines) h += l.height();
        cardH = Math.min(h, BookStyle.SPREAD_H - 40);
        cardX = X1 - CARD_W - 14;
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

    static final String OVERWORLD = "minecraft:overworld";
    /** Rounding of the book's distances: a region, not a place (the war table gives the place). */
    private static final int REGION = 100;

    /** "about 1200 blocks north-east", "close by", or "in another realm" when the hunter is not in that dimension. */
    static Component region(String dimension, int x, int z) {
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
            wrap(out, OfficerModal.dated(h), BookStyle.INK);
            out.add(new Line(FormattedCharSequence.EMPTY, 0, 1, 3));
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
        if (panel != null) return panel.mouseClicked(mx, my, button);
        if (button != 0) return false;
        for (FactionButton b : factionButtons()) {
            if (inside(mx, my, b.x(), BUTTON_Y, b.w(), 16)) {
                if (b.f() != faction) click(SoundEvents.BOOK_PAGE_TURN);
                showFaction(b.f());
                return true;
            }
        }
        for (int i = spots.size() - 1; i >= 0; i--) {
            Spot sp = spots.get(i);
            if (inside(mx, my, sp.x(), sp.y(), sp.w(), sp.h())) {
                if (sp.click() != null) sp.click().run();
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
        if (inside(mx, my, bondsX(), BUTTON_Y, 20, 16)) {
            click(SoundEvents.UI_BUTTON_CLICK.value());
            bondsShown = !bondsShown;
            return true;
        }
        if (inside(mx, my, frontX(), BUTTON_Y, 20, 16)) {
            click(SoundEvents.BOOK_PAGE_TURN);
            openWar();
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
        if (panel != null) return true;
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
        if (panel != null) {
            dragging = false;
            return true;
        }
        if (!dragging || button != 0) return false;
        dragging = false;
        if (moved) return true;
        Place p = hovered((int) Math.floor(mx), (int) Math.floor(my));
        if (p != null) {
            click(SoundEvents.BOOK_PAGE_TURN);
            select(p.node().id());
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
        if (panel != null) return panel.mouseScrolled(mx, my, sy);
        if (overCard(mx, my) || !onCanvas(mx, my) || sy == 0) return false;
        zoomAt(mx, my, zoom * (float) Math.pow(1.15, sy));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (panel != null) return panel.keyPressed(key);
        if (key == GLFW.GLFW_KEY_ESCAPE) takeKeyboard(false);
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

    static boolean alive(Entry e) {
        return "ALIVE".equals(e.status());
    }

    /** 0 the Pit, 1 the Host, 2 a Fallen angel, 3 SucroCorp, a guild's side (its hired officers too). */
    static int kind(Entry e) {
        if ("demon".equals(e.faction())) return 0;
        if (ClientChain.isGuild(e.faction())) return ChainAtlas.side(ChainFaction.byId(e.faction()));
        if ("leviathan".equals(e.faction())) return ChainAtlas.LEVIATHAN;
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
        return ClientChain.name(e);
    }

    /** The tree's label: a name of one word as it is; of two (SucroCorp's people), the surname, so labels side by side stay apart (a boss keeps his). */
    private static String shortName(Entry e) {
        String n = name(e).getString();
        int space = n.lastIndexOf(' ');
        boolean whole = space < 0 || !e.player().isEmpty() || org.papiricoh.supernaturalcraft.chain.Officer.BOSS_TYPE.equals(e.entityType());
        return whole ? n : n.substring(space + 1);
    }

    /** "Captain of the Pit": the rank and the epithet. */
    static Component title(Entry e) {
        ChainFaction f = ChainFaction.byId(e.faction());
        ChainFaction.Branch branch = "FALLEN".equals(e.branch()) ? ChainFaction.Branch.FALLEN : ChainFaction.Branch.LOYAL;
        ChainRank rank = ChainRank.valueOf(e.rank());
        return Component.translatable(org.papiricoh.supernaturalcraft.chain.ChainRank.titleKey(e.faction(), e.rank())).append(" ")
                .append(ClientChain.epithet(e));
    }

    private static int rankColour(Entry e) {
        if (!alive(e)) return BookStyle.FADED;
        if ("FALLEN".equals(e.branch())) return 0xFF5A5450;
        return ChainRank.valueOf(e.rank()) == ChainRank.LORD ? BookStyle.GOLD
                : ClientChain.isGuild(e.faction()) ? 0xFF000000 | ClientChain.guildInk(e.faction()) : "angel".equals(e.faction()) ? BookStyle.MANA : "leviathan".equals(e.faction()) ? LEVIATHAN_INK : BookStyle.BLOOD;
    }

    /** SucroCorp's colour in the book's text (petrol). */
    static final int LEVIATHAN_INK = 0xFF24504A;

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
