package org.papiricoh.supernaturalcraft.client.book.roadmap;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.papiricoh.supernaturalcraft.client.ClientHunterLog;
import org.papiricoh.supernaturalcraft.client.book.BookAtlas;
import org.papiricoh.supernaturalcraft.client.book.BookButton;
import org.papiricoh.supernaturalcraft.client.book.BookData;
import org.papiricoh.supernaturalcraft.client.book.BookSection;
import org.papiricoh.supernaturalcraft.client.book.BookStyle;
import org.papiricoh.supernaturalcraft.client.book.HunterBookScreen;
import org.papiricoh.supernaturalcraft.journal.JournalEntry;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.journal.Roadmap;
import org.papiricoh.supernaturalcraft.journal.RoadmapNode;
import org.papiricoh.supernaturalcraft.journal.RoadmapState;
import org.papiricoh.supernaturalcraft.journal.RoadmapState.Status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The roadmap: an old hunter's map spread over both pages, one road at a time (the road to the Cage,
 * the spell bowl, the crossroads...; the cartouche unrolls into the list of roads). Every step of the
 * road ({@link BookData#roadmap(ResourceLocation)}) sits on a grid of {@link #GX}×{@link #GY} map units, joined to the
 * steps it follows by inked elbows (gold once the way is walked). Drag to move, scroll to zoom
 * around the mouse, double-click or press "Next" to glide to the next objective. Where the reader
 * left the map is remembered for as long as the game runs.
 */
public class RoadmapSection extends BookSection {

    /** Map units between columns and rows at zoom 1 (one unit is one book pixel there). */
    static final int GX = 56, GY = 46;
    private static final float MIN_ZOOM = 0.5f, MAX_ZOOM = 2f;
    /** Below this zoom only the bosses keep their names. */
    private static final float LABEL_ZOOM = 0.75f;

    private static final int X0 = BookStyle.SPREAD_X, Y0 = BookStyle.SPREAD_Y;
    private static final int X1 = X0 + BookStyle.SPREAD_W, Y1 = Y0 + BookStyle.SPREAD_H;
    private static final float CX = X0 + BookStyle.SPREAD_W / 2f, CY = Y0 + BookStyle.SPREAD_H / 2f;
    private static final int PAPER = 256;
    private static final String K = "screen.supernaturalcraft.book.roadmap.";

    // --- the view, kept across openings --------------------------------------------------------
    /** The road on the map. */
    private static ResourceLocation road = Roadmap.CAGE;
    /** Where each road was left: pan x, pan y, zoom. */
    private static final Map<ResourceLocation, float[]> VIEWS = new HashMap<>();
    /** The map point at the centre of the canvas, and the zoom. */
    private static float panX, panY, zoom = 1;
    private static boolean placed;
    /** Whether the cartouche is unrolled into the list of roads. */
    private boolean menuOpen;
    /** The list's rows this frame (top, road) and its extent. */
    private final List<ResourceLocation> menuRoads = new ArrayList<>();
    private int menuX, menuY, menuW, menuRowH = 22;
    /** An eased glide towards a goal (after "Next" or a focus from the dashboard). */
    private static boolean gliding;
    private static float goalX, goalY, goalZoom;
    private static long lastFrame;
    /** A node to centre on once the roadmap has loaded. */
    private static String pendingFocus;
    /** {@code SN_PREVIEW=book}: shows this node's tooltip as if hovered. */
    private static String forcedHover;

    // --- this frame's road ------------------------------------------------------------------------
    private List<RoadmapNode> nodes = List.of();
    private final Map<String, RoadmapNode> byId = new HashMap<>();
    private final Map<ResourceLocation, ItemStack> icons = new HashMap<>();
    private Map<String, Status> state = Map.of();
    private RoadmapNode next;
    private int minX, maxX, minY, maxY;

    // --- input ------------------------------------------------------------------------------------
    private boolean dragging, moved;
    private double pressX, pressY;
    private long lastPress;
    private BookButton nextButton, fitButton;
    /** Where the cartouche and the legend end (they swallow clicks). */
    private int titleRight = X0 + 120, legendRight = X0 + 90;

    // --- the road -----------------------------------------------------------------------------------

    private void refresh() {
        if (!BookData.roads().containsKey(road) && !BookData.roads().isEmpty()) {
            road = BookData.roads().containsKey(Roadmap.CAGE) ? Roadmap.CAGE : BookData.roads().keySet().iterator().next();
        }
        List<RoadmapNode> now = BookData.roadmap(road);
        if (now != nodes) {
            nodes = now;
            byId.clear();
            minX = minY = Integer.MAX_VALUE;
            maxX = maxY = Integer.MIN_VALUE;
            for (RoadmapNode n : nodes) {
                byId.put(n.id(), n);
                minX = Math.min(minX, x(n));
                maxX = Math.max(maxX, x(n));
                minY = Math.min(minY, y(n));
                maxY = Math.max(maxY, y(n));
            }
            if (nodes.isEmpty()) minX = maxX = minY = maxY = 0;
        }
        state = RoadmapState.of(nodes, ClientHunterLog.PROGRESS);
        next = RoadmapState.next(nodes, state);
        if (pendingFocus != null && byId.containsKey(pendingFocus)) {
            String id = pendingFocus;
            pendingFocus = null;
            focus(id);
        }
        if (!placed && !nodes.isEmpty()) {
            if (next != null) jump(x(next), y(next), 1);
            else fitNow();
        }
    }

    private static int x(RoadmapNode n) {
        return n.col() * GX;
    }

    private static int y(RoadmapNode n) {
        return n.row() * GY;
    }

    private static int half(RoadmapNode n) {
        return n.boss() ? 16 : 12;
    }

    private Status status(RoadmapNode n) {
        return state.getOrDefault(n.id(), Status.LOCKED);
    }

    private ItemStack icon(RoadmapNode n) {
        return icons.computeIfAbsent(n.icon(), id -> new ItemStack(BuiltInRegistries.ITEM.get(id)));
    }

    /** The journal entry a node opens, if the hunter can read it yet. */
    private ResourceLocation readable(RoadmapNode n) {
        if (n.entry().isEmpty()) return null;
        JournalEntry e = BookData.entries().get(n.entry().get());
        return e != null && e.unlock().test(ClientHunterLog.PROGRESS) ? n.entry().get() : null;
    }

    // --- the view -----------------------------------------------------------------------------------

    /** Centres the canvas on a node (gliding there if the map is already open somewhere). */
    public void focus(String nodeId) {
        ResourceLocation home = BookData.roadOf(nodeId);
        if (home != null && !home.equals(road)) select(home);
        RoadmapNode n = byId.get(nodeId);
        if (n == null) {
            if (home == null) return;
            pendingFocus = nodeId;
            return;
        }
        if (!placed) jump(x(n), y(n), Math.max(zoom, 1));
        else glide(x(n), y(n), Math.max(zoom, 1));
    }

    /** Puts another road on the map, where it was last left (or on its next step). */
    public void select(ResourceLocation id) {
        if (id.equals(road)) return;
        VIEWS.put(road, new float[]{panX, panY, zoom});
        road = id;
        menuOpen = false;
        gliding = false;
        float[] view = VIEWS.get(id);
        placed = view != null;
        if (view != null) {
            panX = view[0];
            panY = view[1];
            zoom = view[2];
        }
        nodes = List.of();
        refresh();
    }

    public static ResourceLocation road() {
        return road;
    }

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

    private void centreOnNext() {
        if (next != null) glide(x(next), y(next), Math.max(zoom, 1));
    }

    /** The zoom that shows the whole road. */
    private float fitZoom() {
        float w = maxX - minX + GX + 8, h = maxY - minY + GY + 24;
        return Mth.clamp(Math.min((BookStyle.SPREAD_W - 16) / w, (BookStyle.SPREAD_H - 40) / h), MIN_ZOOM, MAX_ZOOM);
    }

    private void fit() {
        glide((minX + maxX) / 2f, (minY + maxY) / 2f + 6, fitZoom());
    }

    private void fitNow() {
        jump((minX + maxX) / 2f, (minY + maxY) / 2f + 6, fitZoom());
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

    /** The centre may go no further than the outermost steps. */
    private void clampPan() {
        panX = Mth.clamp(panX, minX, maxX);
        panY = Mth.clamp(panY, minY, maxY);
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

    // --- widgets ------------------------------------------------------------------------------------

    @Override
    public boolean coversPages() {
        return true;
    }

    @Override
    public void init() {
        refresh();
        Component nextText = Component.translatable(K + "next"), fitText = Component.translatable(K + "fit");
        int nw = font.width(nextText) + 14, fw = font.width(fitText) + 14, h = 14, y = Y1 - 8 - h;
        nextButton = book.add(new BookButton(X1 - 8 - nw, y, nw, h, nextText, this::centreOnNext));
        fitButton = book.add(new BookButton(X1 - 8 - nw - 3 - fw, y, fw, h, fitText, this::fit));
    }

    @Override
    public void hidden() {
        menuOpen = false;
        dragging = false;
        forcedHover = null;
    }

    // --- drawing ------------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        refresh();
        tickGlide();
        if (nextButton != null) nextButton.active = next != null;

        book.scissor(g, X0, Y0, X1, Y1);
        drawPaper(g);
        RoadmapNode hovered = hovered(mx, my);
        var pose = g.pose();
        pose.pushPose();
        pose.translate(CX, CY, 0);
        pose.scale(zoom, zoom, 1);
        pose.translate(-panX, -panY, 0);
        g.drawManaged(() -> {
            for (RoadmapNode n : nodes) for (String p : n.parents()) {
                RoadmapNode parent = byId.get(p);
                if (parent != null) drawEdge(g, parent, n, status(parent) == Status.DONE);
            }
        });
        for (RoadmapNode n : nodes) if (visible(n)) drawNode(g, n, n == hovered);
        pose.popPose();
        for (RoadmapNode n : nodes) if (visible(n)) drawLabel(g, n);
        drawShade(g);
        g.disableScissor();
        // Everything after this sits above the canvas, items included.
        g.flush();
        RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

        drawTitle(g);
        drawLegend(g);
        if (nodes.isEmpty()) {
            Component empty = Component.translatable(K + "empty");
            g.drawString(font, empty, (int) CX - font.width(empty) / 2, (int) CY - 4, BookStyle.FADED, false);
        }
        if (menuOpen) drawMenu(g, mx, my);
        else if (onTitle(mx, my)) book.tooltip(List.of(Component.translatable(K + "roads.tooltip")));
        else if (hovered != null) tooltip(hovered);
        else if (nextButton != null && nextButton.isHovered()) book.tooltip(List.of(Component.translatable(K + "next.tooltip")));
        else if (fitButton != null && fitButton.isHovered()) book.tooltip(List.of(Component.translatable(K + "fit.tooltip")));
    }

    /** Old map paper, tiled at its own size so it stays crisp, sliding with the map. */
    private void drawPaper(GuiGraphics g) {
        int ox = Math.floorMod(Math.round(-panX * zoom), PAPER), oy = Math.floorMod(Math.round(-panY * zoom), PAPER);
        for (int x = X0 - PAPER + ox; x < X1; x += PAPER) {
            for (int y = Y0 - PAPER + oy; y < Y1; y += PAPER) {
                g.blit(BookAtlas.ROADMAP_PAPER, x, y, 0, 0, PAPER, PAPER, PAPER, PAPER);
            }
        }
    }

    /** The spine's shadow down the middle, the paper browning towards its edges, and a fine ink rule. */
    private void drawShade(GuiGraphics g) {
        int mid = (X0 + X1) / 2;
        for (int i = 0; i < 10; i++) {
            int a = (10 - i) * (10 - i) * 52 / 100;
            g.fill(mid - i - 1, Y0, mid - i, Y1, ink(a));
            g.fill(mid + i, Y0, mid + i + 1, Y1, ink(a));
        }
        for (int i = 0; i < 12; i++) {
            int a = (12 - i) * (12 - i) * 70 / 144;
            g.fill(X0 + i, Y0, X0 + i + 1, Y1, ink(a));
            g.fill(X1 - i - 1, Y0, X1 - i, Y1, ink(a));
            g.fill(X0, Y0 + i, X1, Y0 + i + 1, ink(a));
            g.fill(X0, Y1 - i - 1, X1, Y1 - i, ink(a));
        }
        int rule = ink(0x90);
        g.fill(X0, Y0, X1, Y0 + 1, rule);
        g.fill(X0, Y1 - 1, X1, Y1, rule);
        g.fill(X0, Y0, X0 + 1, Y1, rule);
        g.fill(X1 - 1, Y0, X1, Y1, rule);
    }

    private static int ink(int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (BookStyle.INK & 0xFFFFFF);
    }

    private boolean visible(RoadmapNode n) {
        float bx = bookX(x(n)), by = bookY(y(n)), r = (half(n) + 40) * Math.max(zoom, 1);
        return bx + r > X0 && bx - r < X1 && by + r > Y0 && by - r < Y1;
    }

    // --- edges --------------------------------------------------------------------------------------

    /**
     * An elbow from parent to child: along the parent's row, down or up between the columns, then
     * into the child. If a node stands in the way on the parent's row, the turn comes first and the
     * line runs along the child's row instead.
     */
    private void drawEdge(GuiGraphics g, RoadmapNode p, RoadmapNode c, boolean walked) {
        int px = x(p), py = y(p), cx = x(c), cy = y(c);
        int color = walked ? BookStyle.GOLD : (0xA0 << 24) | (BookStyle.FADED & 0xFFFFFF);
        if (py == cy) {
            segment(g, px, py, cx, cy, color, !walked, 0);
            return;
        }
        int ex = blocked(p, c) ? px + GX / 2 : cx - GX / 2;
        int phase = segment(g, px, py, ex, py, color, !walked, 0);
        phase = segment(g, ex, py, ex, cy, color, !walked, phase);
        segment(g, ex, cy, cx, cy, color, !walked, phase);
    }

    private boolean blocked(RoadmapNode p, RoadmapNode c) {
        for (RoadmapNode n : nodes) {
            if (n.row() == p.row() && n.col() > p.col() && n.col() < c.col()) return true;
        }
        return false;
    }

    /** A 2-unit line (dotted: 4 on, 3 off, carrying the dash phase round corners); returns the phase. */
    private static int segment(GuiGraphics g, int x0, int y0, int x1, int y1, int color, boolean dotted, int phase) {
        int len = Math.abs(x1 - x0) + Math.abs(y1 - y0);
        int dx = Integer.signum(x1 - x0), dy = Integer.signum(y1 - y0);
        if (!dotted) {
            if (color == BookStyle.GOLD) line(g, x0 + 1, y0 + 1, x1 + 1, y1 + 1, ink(0x50));
            line(g, x0, y0, x1, y1, color);
            return 0;
        }
        for (int i = 0; i <= len; i++) {
            if ((phase + i) % 7 < 4) {
                int x = x0 + dx * i, y = y0 + dy * i;
                g.fill(x - 1, y - 1, x + 1, y + 1, color);
            }
        }
        return (phase + len + 1) % 7;
    }

    private static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        g.fill(Math.min(x0, x1) - 1, Math.min(y0, y1) - 1, Math.max(x0, x1) + 1, Math.max(y0, y1) + 1, color);
    }

    // --- nodes --------------------------------------------------------------------------------------

    private void drawNode(GuiGraphics g, RoadmapNode n, boolean hovered) {
        int x = x(n), y = y(n), h = half(n), size = 2 * h;
        Status s = status(n);
        float pulse = 0.5f + 0.5f * Mth.sin((Util.getMillis() % 1800) / 1800f * Mth.TWO_PI);
        if (n == next) {
            // The next objective: a red ring of ink that keeps spreading.
            float t = (Util.getMillis() % 1600) / 1600f;
            int r = Math.round(size + 4 + t * 14);
            tint(g, BookAtlas.FX_RING, x, y, r, BookStyle.BLOOD, 0.85f * (1 - t));
        }
        if (s == Status.AVAILABLE || hovered) {
            int r = Math.round(size + 12 + 5 * pulse);
            tint(g, BookAtlas.FX_GLOW, x, y, r, BookStyle.GOLD, hovered ? 0.9f : 0.35f + 0.35f * pulse);
        }
        crisp(g, n.boss() ? BookAtlas.bossFrame(frame(s)) : BookAtlas.stepFrame(frame(s)), x - h, y - h);

        var pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        if (n.boss()) pose.scale(1.25f, 1.25f, 1.25f);
        switch (s) {
            case DONE -> g.renderItem(icon(n), -8, -8);
            // Not yet done: the picture is only an inked shape.
            case AVAILABLE -> silhouette(g, icon(n), 0.27f, 0.19f, 0.12f);
            case LOCKED -> silhouette(g, icon(n), 0.10f, 0.08f, 0.06f);
            // A side the hunter swore against: the picture in pencil grey.
            case FORSAKEN -> silhouette(g, icon(n), 0.42f, 0.41f, 0.39f);
        }
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, 300);
        if (s == Status.DONE) crisp(g, BookAtlas.SEAL, x + h - 11, y + h - 12);
        else if (s == Status.LOCKED) crisp(g, BookAtlas.LOCK, x + h - 10, y + h - 13);
        else if (s == Status.FORSAKEN) {
            // Washed over in grey, and struck through: this road is closed to the side the hunter chose.
            g.fill(x - h + 1, y - h + 1, x + h - 1, y + h - 1, 0x8C9C968C);
            for (int i = -h + 3; i < h - 3; i++) g.fill(x + i, y - i - 1, x + i + 1, y - i + 1, 0x995A544C);
        }
        pose.popPose();
    }

    /**
     * Draws a sprite at its own size with its texture coordinates pulled in a tenth of a texel: at
     * the canvas's fractional zooms a plain blit can sample the neighbouring sprite along an edge.
     */
    private static void crisp(GuiGraphics g, BookAtlas.Sprite s, int x, int y) {
        float sheet = s.sheet(), e = 0.1f;
        RenderSystem.setShaderTexture(0, s.texture());
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Matrix4f m = g.pose().last().pose();
        float u0 = (s.u() + e) / sheet, u1 = (s.u() + s.w() - e) / sheet, v0 = (s.v() + e) / sheet, v1 = (s.v() + s.h() - e) / sheet;
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        b.addVertex(m, x, y, 0).setUv(u0, v0);
        b.addVertex(m, x, y + s.h(), 0).setUv(u0, v1);
        b.addVertex(m, x + s.w(), y + s.h(), 0).setUv(u1, v1);
        b.addVertex(m, x + s.w(), y, 0).setUv(u1, v0);
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.disableBlend();
    }

    private static void silhouette(GuiGraphics g, ItemStack stack, float r, float gr, float b) {
        RenderSystem.setShaderColor(r, gr, b, 1);
        g.renderItem(stack, -8, -8);
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    /** A greyscale sprite tinted with an ink colour, centred on (x, y) at a size. */
    private static void tint(GuiGraphics g, BookAtlas.Sprite sprite, int x, int y, int size, int rgb, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, alpha);
        sprite.draw(g, x - size / 2, y - size / 2, size, size);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    /** Names in small ink under each node, drawn in book space so they stay crisp at any zoom. */
    /** The atlas has a frame for done, available and locked; a forsaken step wears the locked one. */
    private static int frame(Status s) {
        return Math.min(s.ordinal(), Status.LOCKED.ordinal());
    }

    private void drawLabel(GuiGraphics g, RoadmapNode n) {
        if (zoom < LABEL_ZOOM && !n.boss()) return;
        Status s = status(n);
        Component text = s == Status.LOCKED ? Component.translatable(K + "unknown") : Component.translatable(n.nameKey());
        float ls = labelScale();
        int width = (int) ((Math.max(GX * zoom, 44) - 4) / ls);
        List<FormattedCharSequence> lines = font.split(text, width);
        int color = switch (s) {
            case DONE -> BookStyle.INK;
            case AVAILABLE -> BookStyle.BLOOD;
            case LOCKED, FORSAKEN -> BookStyle.FADED;
        };
        float bx = bookX(x(n)), top = bookY(y(n)) + (half(n) + 1) * zoom + 2;
        var pose = g.pose();
        for (int i = 0; i < lines.size(); i++) {
            pose.pushPose();
            pose.translate(bx, top + i * 9 * ls, 0);
            pose.scale(ls, ls, 1);
            g.drawString(font, lines.get(i), -font.width(lines.get(i)) / 2, 0, color, false);
            pose.popPose();
        }
    }

    /**
     * About three quarters of the book's pixel, rounded to whole screen pixels per font pixel so the
     * small lettering stays sharp.
     */
    private float labelScale() {
        double perBookPixel = book.scale() * mc.getWindow().getGuiScale();
        long n = Math.max(1, Math.round(0.75 * perBookPixel - 0.01));
        return (float) (n / perBookPixel);
    }

    // --- the cartouche and the legend --------------------------------------------------------------

    private void drawTitle(GuiGraphics g) {
        Component title = Component.translatable(Roadmap.titleKey(road)).append(menuOpen ? "  \u25B4" : "  \u25BE");
        long done = nodes.stream().filter(n -> status(n) == Status.DONE).count();
        Component count = next == null && !nodes.isEmpty() ? Component.translatable(K + "the_end")
                : Component.literal(done + " / " + nodes.size());
        float ls = labelScale();
        int w = Math.max(font.width(title), (int) Math.ceil(font.width(count) * ls)) + 16, x = X0 + 7, y = Y0 + 7;
        BookAtlas.panel(g, BookAtlas.CARD, x, y, w, 25);
        titleRight = x + w;
        g.drawString(font, title, x + 8, y + 5, BookStyle.INK, false);
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x + 8, y + 15, 0);
        pose.scale(ls, ls, 1);
        g.drawString(font, count, 0, 0, BookStyle.FADED, false);
        pose.popPose();
    }

    private void drawLegend(GuiGraphics g) {
        // A fourth row on a road with branches the hunter swore against (v0.13).
        int rows = legendRows();
        Component[] names = {Component.translatable(K + "legend.done"), Component.translatable(K + "legend.next"),
                Component.translatable(K + "legend.locked"), Component.translatable(K + "legend.forsaken")};
        float ls = labelScale();
        int textW = 0;
        for (int i = 0; i < rows; i++) textW = Math.max(textW, (int) Math.ceil(font.width(names[i]) * ls));
        int w = 8 + 12 + 4 + textW + 8, h = 8 + rows * 13 + 4, x = X0 + 7, y = Y1 - 7 - h;
        BookAtlas.panel(g, BookAtlas.CARD, x, y, w, h);
        legendRight = x + w;
        var pose = g.pose();
        for (int i = 0; i < rows; i++) {
            if (i == 3) {
                int ry = y + 6 + i * 13;
                pose.pushPose();
                pose.translate(x + 8, ry, 0);
                pose.scale(0.5f, 0.5f, 1);
                BookAtlas.stepFrame(2).draw(g, 0, 0);
                pose.popPose();
                g.fill(x + 9, ry + 1, x + 19, ry + 11, 0x8C9C968C);
                pose.pushPose();
                pose.translate(x + 8 + 12 + 4, ry + 6 - 4 * ls, 0);
                pose.scale(ls, ls, 1);
                g.drawString(font, names[i], 0, 0, BookStyle.FADED, false);
                pose.popPose();
                continue;
            }
            int ry = y + 6 + i * 13;
            pose.pushPose();
            pose.translate(x + 8, ry, 0);
            pose.scale(0.5f, 0.5f, 1);
            BookAtlas.stepFrame(i).draw(g, 0, 0);
            if (i == 0) BookAtlas.SEAL.draw(g, 12, 12);
            if (i == 2) BookAtlas.LOCK.draw(g, 13, 11);
            pose.popPose();
            pose.pushPose();
            pose.translate(x + 8 + 12 + 4, ry + 6 - 4 * ls, 0);
            pose.scale(ls, ls, 1);
            int color = i == 0 ? BookStyle.INK : i == 1 ? BookStyle.BLOOD : BookStyle.FADED;
            g.drawString(font, names[i], 0, 0, color, false);
            pose.popPose();
        }
    }

    private int legendTop() {
        return Y1 - 7 - (8 + legendRows() * 13 + 4);
    }

    private int legendRows() {
        return state.containsValue(Status.FORSAKEN) ? 4 : 3;
    }

    private boolean onTitle(double mx, double my) {
        return mx >= X0 && mx < titleRight && my >= Y0 && my < Y0 + 32;
    }

    /** The unrolled list of roads under the cartouche: icon, title, steps done. */
    private void drawMenu(GuiGraphics g, int mx, int my) {
        menuRoads.clear();
        menuRoads.addAll(BookData.roads().keySet());
        float ls = labelScale();
        int textW = 0;
        for (ResourceLocation id : menuRoads) {
            textW = Math.max(textW, font.width(Component.translatable(Roadmap.titleKey(id))) + 4 + (int) Math.ceil(font.width("00 / 00") * ls));
        }
        menuX = X0 + 7;
        menuY = Y0 + 7 + 25 - 2;
        menuW = Math.max(titleRight - menuX, 8 + 18 + textW + 10);
        int h = 6 + menuRoads.size() * menuRowH;
        var pose = g.pose();
        BookAtlas.panel(g, BookAtlas.CARD, menuX, menuY, menuW, h);
        for (int i = 0; i < menuRoads.size(); i++) {
            ResourceLocation id = menuRoads.get(i);
            Roadmap r = BookData.roads().get(id);
            int ry = menuY + 3 + i * menuRowH;
            boolean current = id.equals(road), over = mx >= menuX && mx < menuX + menuW && my >= ry && my < ry + menuRowH;
            if (over) g.fill(menuX + 3, ry, menuX + menuW - 3, ry + menuRowH, BookStyle.HOVER);
            if (current) g.fill(menuX + 3, ry + 2, menuX + 5, ry + menuRowH - 2, BookStyle.GOLD);
            g.renderItem(icons.computeIfAbsent(r.icon(), k -> new ItemStack(BuiltInRegistries.ITEM.get(k))), menuX + 8, ry + 3);
            Map<String, Status> st = RoadmapState.of(r.nodes(), ClientHunterLog.PROGRESS);
            long done = r.nodes().stream().filter(n -> st.get(n.id()) == Status.DONE).count();
            g.drawString(font, Component.translatable(Roadmap.titleKey(id)), menuX + 28, ry + 7, current ? BookStyle.INK : BookStyle.FADED, false);
            String count = done + " / " + r.nodes().size();
            pose.pushPose();
            pose.translate(menuX + menuW - 8 - font.width(count) * ls, ry + 8, 0);
            pose.scale(ls, ls, 1);
            g.drawString(font, count, 0, 0, done == r.nodes().size() ? BookStyle.GOLD : BookStyle.FADED, false);
            pose.popPose();
        }
    }

    private boolean onMenu(double mx, double my) {
        return menuOpen && mx >= menuX && mx < menuX + menuW && my >= menuY && my < menuY + 6 + menuRoads.size() * menuRowH;
    }

    /** Whether a point is on the canvas but under the cartouche, the legend or the buttons. */
    private boolean onFurniture(double mx, double my) {
        if (onMenu(mx, my)) return true;
        if (mx < titleRight && my < Y0 + 32) return true;
        if (mx < legendRight && my >= legendTop()) return true;
        return fitButton != null && my >= fitButton.getY() && mx >= fitButton.getX();
    }

    // --- hover and tooltips ------------------------------------------------------------------------

    private RoadmapNode hovered(int mx, int my) {
        if (forcedHover != null) return byId.get(forcedHover);
        if (menuOpen) return null;
        if (dragging && moved || !onCanvas(mx, my) || onFurniture(mx, my)) return null;
        float wx = worldX(mx), wy = worldY(my);
        for (RoadmapNode n : nodes) {
            int h = half(n) + 1;
            if (Math.abs(wx - x(n)) <= h && Math.abs(wy - y(n)) <= h) return n;
        }
        return null;
    }

    private void tooltip(RoadmapNode n) {
        List<Component> lines = new ArrayList<>();
        Status s = status(n);
        switch (s) {
            case DONE -> {
                lines.add(name(n));
                lines.add(Component.translatable(K + "done").withStyle(ChatFormatting.DARK_GREEN));
            }
            case AVAILABLE -> {
                lines.add(name(n));
                lines.add(Component.translatable(n.hintKey()).withStyle(ChatFormatting.GRAY));
            }
            case LOCKED -> {
                lines.add(Component.translatable(K + "unknown").withStyle(ChatFormatting.DARK_GRAY));
                MutableComponent needs = Component.empty();
                boolean first = true;
                for (String p : n.parents()) {
                    RoadmapNode parent = byId.get(p);
                    if (parent == null || status(parent) == Status.DONE) continue;
                    if (!first) needs.append(", ");
                    first = false;
                    needs.append(status(parent) == Status.LOCKED ? Component.translatable(K + "unknown")
                            : Component.translatable(parent.nameKey()));
                }
                lines.add(Component.translatable(K + "requires", needs).withStyle(ChatFormatting.GRAY));
            }
            case FORSAKEN -> {
                lines.add(Component.translatable(n.nameKey()).withStyle(ChatFormatting.DARK_GRAY));
                String sworn = ClientHunterLog.PROGRESS.allegiance();
                lines.add(Component.translatable(K + "forsaken." + (sworn.isEmpty() ? "angel" : sworn)).withStyle(ChatFormatting.GRAY,
                        ChatFormatting.ITALIC));
            }
        }
        if (s != Status.LOCKED && s != Status.FORSAKEN && readable(n) != null) {
            lines.add(Component.translatable(K + "read").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        }
        book.tooltip(lines);
    }

    private static Component name(RoadmapNode n) {
        return Component.translatable(n.nameKey()).withStyle(n.boss() ? ChatFormatting.GOLD : ChatFormatting.WHITE);
    }

    // --- input --------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && onMenu(mx, my)) {
            int i = (int) ((my - menuY - 3) / menuRowH);
            if (i >= 0 && i < menuRoads.size()) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
                ResourceLocation pick = menuRoads.get(i);
                menuOpen = false;
                select(pick);
            }
            return true;
        }
        if (button == 0 && onTitle(mx, my)) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.6f));
            menuOpen = !menuOpen;
            return true;
        }
        if (menuOpen) {
            // A click anywhere else rolls the list back up.
            menuOpen = false;
            return true;
        }
        if (button != 0 || !onCanvas(mx, my) || onFurniture(mx, my)) return false;
        forcedHover = null;
        long now = Util.getMillis();
        if (now - lastPress < 300 && hovered((int) Math.floor(mx), (int) Math.floor(my)) == null) {
            lastPress = 0;
            centreOnNext();
            return true;
        }
        lastPress = now;
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
        RoadmapNode n = hovered((int) Math.floor(mx), (int) Math.floor(my));
        ResourceLocation entry = n == null || status(n) == Status.LOCKED ? null : readable(n);
        if (entry != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
            book.show(HunterBookScreen.Tab.JOURNAL);
            book.journal().openEntry(entry);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (!onCanvas(mx, my) || sy == 0) return false;
        zoomAt(mx, my, zoom * (float) Math.pow(1.15, sy));
        return true;
    }

    // --- preview ------------------------------------------------------------------------------------

    @Override
    public List<PreviewShot> previewShots() {
        return List.of(
                new PreviewShot("open", b -> {
                    b.roadmap().select(Roadmap.CAGE);
                    forcedHover = null;
                }),
                new PreviewShot("overview", b -> {
                    RoadmapSection r = b.roadmap();
                    r.refresh();
                    r.fitNow();
                    forcedHover = null;
                }),
                new PreviewShot("zoomed", b -> {
                    RoadmapSection r = b.roadmap();
                    r.refresh();
                    if (r.next != null) r.jump(x(r.next), y(r.next), 1.5f);
                    forcedHover = null;
                }),
                new PreviewShot("roads", b -> {
                    RoadmapSection r = b.roadmap();
                    r.select(Roadmap.CAGE);
                    r.menuOpen = true;
                    forcedHover = null;
                }),
                new PreviewShot("bowl", b -> {
                    RoadmapSection r = b.roadmap();
                    r.select(SupernaturalCraft.asResource("the_spell_bowl"));
                    r.refresh();
                    r.fitNow();
                    forcedHover = null;
                }),
                new PreviewShot("crossroads", b -> {
                    RoadmapSection r = b.roadmap();
                    r.select(SupernaturalCraft.asResource("the_crossroads"));
                    r.refresh();
                    r.fitNow();
                    forcedHover = null;
                }),
                new PreviewShot("allegiance", b -> {
                    RoadmapSection r = b.roadmap();
                    r.select(SupernaturalCraft.asResource("heaven_hell_free_will"));
                    r.refresh();
                    r.fitNow();
                    forcedHover = null;
                }),
                new PreviewShot("forsaken", b -> {
                    RoadmapSection r = b.roadmap();
                    r.select(SupernaturalCraft.asResource("heaven_hell_free_will"));
                    r.refresh();
                    RoadmapNode n = r.byId.get("angel_2");
                    if (n != null) {
                        r.jump(x(n) - (BookStyle.W / 2f - CX) / 1.25f, y(n) - (BookStyle.H / 2f - CY) / 1.25f, 1.25f);
                        forcedHover = n.id();
                    }
                }),
                new PreviewShot("hover", b -> {
                    RoadmapSection r = b.roadmap();
                    r.refresh();
                    if (r.next != null) {
                        // The node under the middle of the window, where the preview's mouse rests.
                        r.jump(x(r.next) - (BookStyle.W / 2f - CX) / 1.25f, y(r.next) - (BookStyle.H / 2f - CY) / 1.25f, 1.25f);
                        forcedHover = r.next.id();
                    }
                }));
    }
}
