package org.papiricoh.supernaturalcraft.client.legacy.table;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.legacy.CaseBriefScreen;
import org.papiricoh.supernaturalcraft.client.legacy.ClientLegacy;
import org.papiricoh.supernaturalcraft.client.legacy.LegacyText;
import org.papiricoh.supernaturalcraft.legacy.Legacy;
import org.papiricoh.supernaturalcraft.legacy.LegacySchedule;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseFile;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseOffice;
import org.papiricoh.supernaturalcraft.legacy.cases.table.TablePin;
import org.papiricoh.supernaturalcraft.legacy.cases.table.TerrainTile;
import org.papiricoh.supernaturalcraft.legacy.cases.table.WarTable;
import org.papiricoh.supernaturalcraft.legacy.cases.table.WarTableBlockEntity;
import org.papiricoh.supernaturalcraft.legacy.cases.table.WarTableServerHandlers;
import org.papiricoh.supernaturalcraft.network.WarTableActionPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * The war table opened (v0.19): the hologram of the land around the table, dragged and zoomed, with the table, the hunter's
 * cases, the table's pins and the hunter on it; beside it, the open case (brief, abandon) or a button to ask for one, and the
 * table's pins (double-click the map to stick one, rename, take out). The hunter's Letters compass is pointed from here: at a
 * pin, or back at the case.
 */
public class WarTableScreen extends Screen {

    static final ResourceLocation ICONS = SupernaturalCraft.asResource("textures/gui/war_table_icons.png");
    static final int ICON_TABLE = 0, ICON_CASE = 1, ICON_SOLVED = 2, ICON_LOST = 3, ICON_PIN = 4, ICON_PLAYER = 5, ICON_EDGE = 6, ICON_RING = 7,
            ICON_OFFICER = 8, ICON_SEAT = 9;
    /** Icons in the sheet, 16 px each in a row ({@code war_icons} in {@code tools/artgen/legacy_items_art.py}). */
    static final int ICON_COUNT = 10;
    private static final int PANEL = 156, PAD = 8, ROW = 14;
    static final int CYAN = 0xFF5AE6FF, CYAN_DIM = 0xFF1E6F84, AMBER = 0xFFFFC266, TEXT = 0xFFD8F6FF, MUTED = 0xFF7FA9B8, DEEP = 0xFF040D14;
    private static final int[] GRID = {64, 128, 256, 512, 1024};

    private final BlockPos centre;
    private final MapView view;
    private double pressX, pressY;
    private boolean dragging, pressedOnMap;
    private long lastClick;
    private double lastClickX, lastClickY;
    private long abandonUntil;
    private int pinScroll;
    private int renaming = -1;
    private @Nullable EditBox rename;
    private String state = "";

    public WarTableScreen(BlockPos centre) {
        super(Component.translatable("gui.supernaturalcraft.war_table.title"));
        this.centre = centre;
        this.view = new MapView(centre.getX() + 0.5, centre.getZ() + 0.5, 1024);
    }

    public BlockPos centre() {
        return centre;
    }

    public MapView view() {
        return view;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // --- layout ------------------------------------------------------------------------------------------------------------

    private int mapLeft() {
        return PAD;
    }

    private int mapTop() {
        return PAD + 14;
    }

    private int mapRight() {
        return width - PANEL - PAD * 2;
    }

    private int mapBottom() {
        return height - PAD - 12;
    }

    private int panelLeft() {
        return width - PANEL - PAD;
    }

    @Override
    protected void init() {
        view.frame(mapLeft(), mapTop(), mapRight() - mapLeft(), mapBottom() - mapTop());
        int x = panelLeft(), y = PAD + 14 + 74;
        Legacy legacy = ClientLegacy.legacy();
        CaseFile open = CaseOffice.open(legacy);
        if (open != null) {
            addRenderableWidget(Button.builder(Component.translatable("gui.supernaturalcraft.war_table.brief"),
                    b -> openBrief(open)).bounds(x, y, PANEL / 2 - 2, 18).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.supernaturalcraft.war_table.compass_case"),
                    b -> ClientWarTable.act(centre, WarTableActionPayload.COMPASS, -1, 0, 0, ""))
                    .tooltip(Tooltip.create(Component.translatable("gui.supernaturalcraft.war_table.compass_case.tip")))
                    .bounds(x + PANEL / 2 + 2, y, PANEL / 2 - 2, 18).build());
            boolean confirming = Util.getMillis() < abandonUntil;
            addRenderableWidget(Button.builder(Component.translatable("gui.supernaturalcraft.war_table.abandon" + (confirming ? "_confirm" : "")),
                    b -> abandon()).bounds(x, y + 22, PANEL, 18).build());
        } else {
            Button request = addRenderableWidget(Button.builder(Component.translatable("gui.supernaturalcraft.war_table.request"),
                    b -> ClientWarTable.act(centre, WarTableActionPayload.REQUEST, 0, 0, 0, "")).bounds(x, y, PANEL, 18).build());
            request.active = !waiting();
        }
        List<TablePin> pins = pins();
        int listTop = pinListTop(), rows = pinRows();
        pinScroll = Math.max(0, Math.min(pinScroll, Math.max(0, pins.size() - rows)));
        for (int r = 0; r < rows && pinScroll + r < pins.size(); r++) {
            int i = pinScroll + r, ry = listTop + r * ROW;
            TablePin pin = pins.get(i);
            if (i == renaming) {
                rename = new EditBox(font, x + 1, ry, PANEL - 52, ROW - 2, Component.translatable("gui.supernaturalcraft.war_table.rename"));
                rename.setMaxLength(TablePin.MAX_NAME);
                rename.setValue(pin.name());
                addRenderableWidget(rename);
                setInitialFocus(rename);
            } else {
                addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth(pin.name(), PANEL - 60)),
                        b -> view.centreOn(pin.x(), pin.z())).bounds(x, ry, PANEL - 50, ROW - 1).build());
            }
            addRenderableWidget(Button.builder(Component.literal("➶"), b -> ClientWarTable.act(centre, WarTableActionPayload.COMPASS, i, 0, 0, ""))
                    .tooltip(Tooltip.create(Component.translatable("gui.supernaturalcraft.war_table.compass_pin")))
                    .bounds(x + PANEL - 48, ry, 15, ROW - 1).build());
            addRenderableWidget(Button.builder(Component.literal(i == renaming ? "✔" : "✎"), b -> rename(i)).bounds(x + PANEL - 32, ry, 15, ROW - 1).build());
            addRenderableWidget(Button.builder(Component.literal("✕"), b -> {
                renaming = -1;
                ClientWarTable.act(centre, WarTableActionPayload.PIN_REMOVE, i, 0, 0, "");
            }).bounds(x + PANEL - 15, ry, 15, ROW - 1).build());
        }
        state = state();
    }

    private int pinListTop() {
        return PAD + 14 + 74 + 50 + 14;
    }

    private int pinRows() {
        return Math.max(1, (mapBottom() - 12 - pinListTop()) / ROW);
    }

    /** What the widgets depend on: when it changes, they are laid out again. */
    private String state() {
        Legacy l = ClientLegacy.legacy();
        CaseFile open = CaseOffice.open(l);
        return (open == null ? "-" : open.index() + ":" + open.state()) + "|" + waiting() + "|"
                + (Util.getMillis() < abandonUntil) + "|" + pins().hashCode() + "|" + renaming + "|" + pinScroll;
    }

    private boolean waiting() {
        return minecraft != null && minecraft.level != null && !LegacySchedule.mayRequest(ClientLegacy.legacy(), minecraft.level.getDayTime());
    }

    private List<TablePin> pins() {
        WarTableBlockEntity be = ClientWarTable.entity(centre);
        return be == null ? List.of() : be.pins();
    }

    @Override
    public void tick() {
        if (minecraft == null || minecraft.player == null || minecraft.level == null) return;
        double reach = WarTableServerHandlers.REACH + 1;
        if (minecraft.player.distanceToSqr(centre.getX() + 0.5, centre.getY() + 0.5, centre.getZ() + 0.5) > reach * reach
                || !WarTable.intact(minecraft.level, centre)) {
            onClose();
            return;
        }
        if (renaming < 0 && !state.equals(state())) rebuildWidgets();
    }

    // --- actions -----------------------------------------------------------------------------------------------------------

    private void openBrief(CaseFile c) {
        if (minecraft != null) minecraft.setScreen(new CaseBriefScreen(c, this));
    }

    private void abandon() {
        if (Util.getMillis() < abandonUntil) {
            abandonUntil = 0;
            ClientWarTable.act(centre, WarTableActionPayload.ABANDON, 0, 0, 0, "");
        } else {
            abandonUntil = Util.getMillis() + 3000;
        }
        rebuildWidgets();
    }

    private void rename(int index) {
        if (renaming == index && rename != null) {
            ClientWarTable.act(centre, WarTableActionPayload.PIN_RENAME, index, 0, 0, rename.getValue());
            renaming = -1;
        } else {
            renaming = index;
        }
        rename = null;
        rebuildWidgets();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (renaming >= 0 && rename != null) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                rename(renaming);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                renaming = -1;
                rename = null;
                rebuildWidgets();
                return true;
            }
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button == 0 && view.contains(mx, my)) {
            pressX = mx;
            pressY = my;
            pressedOnMap = true;
            dragging = false;
            long now = Util.getMillis();
            if (now - lastClick < 300 && Math.hypot(mx - lastClickX, my - lastClickY) < 4) {
                lastClick = 0;
                if (pins().size() < TablePin.MAX) {
                    ClientWarTable.act(centre, WarTableActionPayload.PIN_ADD, 0, (int) Math.floor(view.worldX(mx)), (int) Math.floor(view.worldZ(my)), "");
                }
            } else {
                lastClick = now;
                lastClickX = mx;
                lastClickY = my;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0 && pressedOnMap) {
            if (!dragging && Math.hypot(mx - pressX, my - pressY) > 3) dragging = true;
            if (dragging) view.pan(dx, dy);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        pressedOnMap = false;
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (view.contains(mx, my)) {
            view.zoomAt(mx, my, view.width() * Math.pow(1.2, -sy));
            return true;
        }
        if (mx >= panelLeft() && my >= pinListTop() && renaming < 0) {
            pinScroll = Math.max(0, pinScroll - (int) Math.signum(sy));
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    // --- drawing -----------------------------------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        g.drawString(font, title, PAD, PAD + 2, CYAN, false);
        Component where = Component.translatable("gui.supernaturalcraft.war_table.coords", centre.getX(), centre.getZ());
        g.drawString(font, where, mapRight() - font.width(where), PAD + 2, MUTED, false);
        drawMap(g, mx, my);
        drawPanel(g);
        g.drawString(font, Component.translatable("gui.supernaturalcraft.war_table.hint"), PAD, mapBottom() + 3, MUTED, false);
        if (view.contains(mx, my)) {
            Component at = Component.translatable("gui.supernaturalcraft.war_table.coords", (int) Math.floor(view.worldX(mx)), (int) Math.floor(view.worldZ(my)));
            g.drawString(font, at, mapRight() - font.width(at), mapBottom() + 3, TEXT, false);
            ClientWarTable.Marker hover = hovered(mx, my);
            if (hover != null) g.renderComponentTooltip(font, tooltip(hover), mx, my);
        }
    }

    private void drawMap(GuiGraphics g, int mx, int my) {
        int l = mapLeft(), t = mapTop(), r = mapRight(), b = mapBottom();
        g.fill(l - 1, t - 1, r + 1, b + 1, CYAN_DIM);
        g.fill(l, t, r, b, DEEP);
        g.enableScissor(l, t, r, b);
        double[] box = view.bounds();
        int lod = TerrainTile.pickLod(1 / view.scale());
        boolean any = drawTiles(g, TerrainTile.FAR, box);
        if (lod == TerrainTile.NEAR) any |= drawTiles(g, TerrainTile.NEAR, box);
        drawGrid(g, box);
        long ms = Util.getMillis();
        int scan = t + (int) ((ms % 4000) / 4000.0 * (b - t));
        g.fill(l, scan, r, scan + 1, 0x7050F0FF);
        g.fill(l, scan - 6, r, scan, 0x1850F0FF);
        if (!any) {
            Component s = Component.translatable("gui.supernaturalcraft.war_table.surveying");
            g.drawString(font, s, (l + r - font.width(s)) / 2, (t + b) / 2, (ms / 400) % 2 == 0 ? CYAN : CYAN_DIM, false);
        }
        List<ClientWarTable.Marker> markers = ClientWarTable.markers(centre);
        ClientWarTable.Marker chosen = null, sighting = null;
        for (ClientWarTable.Marker m : markers) {
            if (m.kind() == ClientWarTable.Kind.CASE_OPEN) dashed(g, view.screenX(centre.getX() + 0.5), view.screenY(centre.getZ() + 0.5), view.screenX(m.x()), view.screenY(m.z()));
            if (m.kind() == ClientWarTable.Kind.SIGHTING) sighting = m;
            else if (m.kind() != ClientWarTable.Kind.TABLE && chain(m) && ClientWarTable.highlighted(m)) chosen = m;
        }
        // From the chosen officer's territory (or seat) to where it was last seen.
        if (chosen != null && sighting != null)
            dashed(g, view.screenX(chosen.x() + 0.5), view.screenY(chosen.z() + 0.5), view.screenX(sighting.x() + 0.5), view.screenY(sighting.z() + 0.5));
        for (int i = markers.size() - 1; i >= 0; i--) drawMarker(g, markers.get(i), ms);
        drawPlayer(g);
        ClientWarTable.Marker hover = view.contains(mx, my) ? hovered(mx, my) : null;
        if (hover != null) icon(g, ICON_RING, view.screenX(hover.x()), view.screenY(hover.z()), 0, CYAN);
        g.disableScissor();
    }

    /** Draws the known tiles at {@code lod} covering the view, asking for the rest; whether any was drawn. */
    private boolean drawTiles(GuiGraphics g, int lod, double[] box) {
        long[] keys = TerrainTile.covering(lod, (int) Math.floor(box[0]), (int) Math.floor(box[1]), (int) Math.ceil(box[2]), (int) Math.ceil(box[3]));
        ClientWarTable.want(centre, keys);
        boolean any = false;
        int span = TerrainTile.span(lod);
        RenderSystem.enableBlend();
        for (long key : keys) {
            ResourceLocation tex = ClientWarTable.texture(key);
            if (tex == null) continue;
            any = true;
            double sx = view.screenX(TerrainTile.origin(lod, TerrainTile.tx(key))), sy = view.screenY(TerrainTile.origin(lod, TerrainTile.tz(key)));
            float size = (float) (span * view.scale());
            g.pose().pushPose();
            g.pose().translate(sx, sy, 0);
            g.pose().scale(size / ClientWarTable.TEXTURE, size / ClientWarTable.TEXTURE, 1);
            g.blit(tex, 0, 0, 0, 0, ClientWarTable.TEXTURE, ClientWarTable.TEXTURE, ClientWarTable.TEXTURE, ClientWarTable.TEXTURE);
            g.pose().popPose();
        }
        RenderSystem.disableBlend();
        return any;
    }

    private void drawGrid(GuiGraphics g, double[] box) {
        int step = GRID[GRID.length - 1];
        for (int s : GRID) {
            if (s * view.scale() >= 48) {
                step = s;
                break;
            }
        }
        for (long x = (long) Math.ceil(box[0] / step) * step; x <= box[2]; x += step) {
            int sx = (int) view.screenX(x);
            g.fill(sx, mapTop(), sx + 1, mapBottom(), 0x2240E0FF);
        }
        for (long z = (long) Math.ceil(box[1] / step) * step; z <= box[3]; z += step) {
            int sy = (int) view.screenY(z);
            g.fill(mapLeft(), sy, mapRight(), sy + 1, 0x2240E0FF);
        }
    }

    private void dashed(GuiGraphics g, double x0, double y0, double x1, double y1) {
        double len = Math.hypot(x1 - x0, y1 - y0);
        int steps = (int) (len / 6);
        for (int i = 0; i < steps; i += 1) {
            if (i % 2 == 1) continue;
            double f = i / (double) steps;
            int x = (int) (x0 + (x1 - x0) * f), y = (int) (y0 + (y1 - y0) * f);
            g.fill(x, y, x + 2, y + 2, 0xC0FF4A3A);
        }
    }

    private void drawMarker(GuiGraphics g, ClientWarTable.Marker m, long ms) {
        double sx = view.screenX(m.x() + 0.5), sy = view.screenY(m.z() + 0.5);
        int l = mapLeft() + 6, t = mapTop() + 6, r = mapRight() - 6, b = mapBottom() - 6;
        int colour = ClientWarTable.colour(m.kind());
        boolean chosen = chain(m) && ClientWarTable.highlighted(m);
        if (sx < l || sx > r || sy < t || sy > b) {
            if (m.kind() != ClientWarTable.Kind.CASE_OPEN && m.kind() != ClientWarTable.Kind.TABLE && (!chosen || m.kind() == ClientWarTable.Kind.SIGHTING)) return;
            double cx = (l + r) / 2.0, cy = (t + b) / 2.0;
            float angle = (float) Math.toDegrees(Math.atan2(sx - cx, -(sy - cy)));
            icon(g, ICON_EDGE, Math.max(l, Math.min(r, sx)), Math.max(t, Math.min(b, sy)), angle, colour);
            return;
        }
        int icon = switch (m.kind()) {
            case TABLE -> ICON_TABLE;
            case CASE_OPEN -> ICON_CASE;
            case CASE_SOLVED -> ICON_SOLVED;
            case CASE_LOST -> ICON_LOST;
            case PIN -> ICON_PIN;
            case OFFICER_PIT, OFFICER_HOST, OFFICER_BOARD, SIGHTING -> ICON_OFFICER;
            case SEAT_PIT, SEAT_HOST, SEAT_BOARD -> ICON_SEAT;
        };
        if (chain(m)) {
            // The chain's marks are tinted by their side; the chosen officer pulses and is named, the rest stay faint.
            if (chosen && m.kind() != ClientWarTable.Kind.SIGHTING) {
                float pulse = (ms % 1500) / 1500f;
                g.pose().pushPose();
                g.pose().translate(sx, sy, 0);
                g.pose().scale(1 + pulse * 1.5f, 1 + pulse * 1.5f, 1);
                icon(g, ICON_RING, 0, 0, 0, ((int) ((1 - pulse) * 200) << 24) | (colour & 0xFFFFFF));
                g.pose().popPose();
            }
            icon(g, icon, sx, sy, 0, chosen && m.kind() != ClientWarTable.Kind.SIGHTING ? colour : (0x90 << 24) | (colour & 0xFFFFFF));
            if (chosen && view.scale() > 0.05) g.drawString(font, m.label(), (int) sx + 7, (int) sy - 3, colour, true);
            return;
        }
        if (m.kind() == ClientWarTable.Kind.CASE_OPEN) {
            float pulse = (ms % 1500) / 1500f;
            g.pose().pushPose();
            g.pose().translate(sx, sy, 0);
            g.pose().scale(1 + pulse * 1.5f, 1 + pulse * 1.5f, 1);
            icon(g, ICON_RING, 0, 0, 0, ((int) ((1 - pulse) * 200) << 24) | (colour & 0xFFFFFF));
            g.pose().popPose();
        }
        icon(g, icon, sx, sy, 0, 0xFFFFFFFF);
        if (m.kind() == ClientWarTable.Kind.PIN && view.scale() > 0.15) {
            g.drawString(font, m.label(), (int) sx + 6, (int) sy - 3, AMBER, true);
        }
    }

    private void drawPlayer(GuiGraphics g) {
        if (minecraft == null || minecraft.player == null) return;
        double sx = view.screenX(minecraft.player.getX()), sy = view.screenY(minecraft.player.getZ());
        icon(g, ICON_PLAYER, sx, sy, (float) CompassMath.heading(minecraft.player.getYRot()), 0xFFFFFFFF);
    }

    /** One 16×16 icon of the sheet, centred on (x, y), turned {@code angle} degrees clockwise, tinted. */
    static void icon(GuiGraphics g, int icon, double x, double y, float angle, int argb) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        if (angle != 0) g.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        RenderSystem.enableBlend();
        g.setColor(((argb >> 16) & 0xFF) / 255f, ((argb >> 8) & 0xFF) / 255f, (argb & 0xFF) / 255f, ((argb >>> 24) & 0xFF) / 255f);
        g.blit(ICONS, -8, -8, icon * 16, 0, 16, 16, ICON_COUNT * 16, 16);
        g.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
        g.pose().popPose();
    }

    private void drawPanel(GuiGraphics g) {
        int x = panelLeft(), y = mapTop();
        g.fill(x - 4, y - 1, x + PANEL + 4, mapBottom() + 1, 0xC0061620);
        g.renderOutline(x - 4, y - 1, PANEL + 8, mapBottom() - y + 2, CYAN_DIM);
        CaseFile open = CaseOffice.open(ClientLegacy.legacy());
        g.drawString(font, Component.translatable("gui.supernaturalcraft.war_table.case_file"), x, y + 4, CYAN, false);
        int ty = y + 18;
        if (open == null) {
            for (FormattedCharSequence line : font.split(Component.translatable("gui.supernaturalcraft.war_table." + (waiting() ? "waiting" : "no_case")), PANEL)) {
                g.drawString(font, line, x, ty, MUTED, false);
                ty += 10;
            }
        } else {
            g.drawString(font, ClientWarTable.caseLabel(open), x, ty, TEXT, false);
            ty += 11;
            g.drawString(font, LegacyText.entityName(open.monster().toString()), x, ty, AMBER, false);
            ty += 11;
            double bearing = CompassMath.bearing(centre.getX(), centre.getZ(), open.site().getX(), open.site().getZ());
            int dist = (int) Math.hypot(open.site().getX() - centre.getX(), open.site().getZ() - centre.getZ());
            g.drawString(font, Component.translatable("gui.supernaturalcraft.war_table.distance", dist,
                    Component.translatable("screen.supernaturalcraft.case.dir." + CompassMath.point(bearing))), x, ty, MUTED, false);
            ty += 11;
            StringBuilder skulls = new StringBuilder();
            for (int i = 1; i <= 5; i++) skulls.append(i <= open.tier() ? '☠' : '·');
            g.drawString(font, Component.translatable("screen.supernaturalcraft.case.state." + open.state()).append("  " + skulls), x, ty, MUTED, false);
        }
        int py = pinListTop() - 12;
        List<TablePin> pins = pins();
        g.drawString(font, Component.translatable("gui.supernaturalcraft.war_table.pins", pins.size(), TablePin.MAX), x, py, CYAN, false);
        if (pins.isEmpty()) {
            int ly = pinListTop();
            for (FormattedCharSequence line : font.split(Component.translatable("gui.supernaturalcraft.war_table.no_pins"), PANEL)) {
                g.drawString(font, line, x, ly, MUTED, false);
                ly += 10;
            }
        }
    }

    /** Whether a mark belongs to the Chain of Command. */
    static boolean chain(ClientWarTable.Marker m) {
        return switch (m.kind()) {
            case OFFICER_PIT, OFFICER_HOST, OFFICER_BOARD, SEAT_PIT, SEAT_HOST, SEAT_BOARD, SIGHTING -> true;
            default -> false;
        };
    }

    private @Nullable ClientWarTable.Marker hovered(double mx, double my) {
        ClientWarTable.Marker best = null;
        double bestD = 7 * 7;
        for (ClientWarTable.Marker m : ClientWarTable.markers(centre)) {
            double dx = view.screenX(m.x() + 0.5) - mx, dy = view.screenY(m.z() + 0.5) - my;
            double d = dx * dx + dy * dy;
            if (d < bestD) {
                bestD = d;
                best = m;
            }
        }
        return best;
    }

    private List<Component> tooltip(ClientWarTable.Marker m) {
        List<Component> out = new ArrayList<>();
        out.add(m.label().copy().withColor(ClientWarTable.colour(m.kind()) & 0xFFFFFF));
        if (m.kind() == ClientWarTable.Kind.SEAT_PIT || m.kind() == ClientWarTable.Kind.SEAT_HOST || m.kind() == ClientWarTable.Kind.SEAT_BOARD)
            for (Component c : ClientWarTable.court(m.kind())) out.add(Component.literal("  ").append(c).withColor(TEXT & 0xFFFFFF));
        if (m.kind() != ClientWarTable.Kind.TABLE) {
            double bearing = CompassMath.bearing(centre.getX(), centre.getZ(), m.x(), m.z());
            int dist = (int) Math.hypot(m.x() - centre.getX(), m.z() - centre.getZ());
            out.add(Component.translatable("gui.supernaturalcraft.war_table.distance", dist,
                    Component.translatable("screen.supernaturalcraft.case.dir." + CompassMath.point(bearing))).withColor(MUTED & 0xFFFFFF));
        }
        if (m.kind() == ClientWarTable.Kind.CASE_SOLVED || m.kind() == ClientWarTable.Kind.CASE_LOST || m.kind() == ClientWarTable.Kind.CASE_OPEN) {
            CaseFile c = ClientLegacy.caseFile(m.index());
            if (c != null) out.add(Component.translatable("screen.supernaturalcraft.case.state." + c.state()).withColor(MUTED & 0xFFFFFF));
        }
        out.add(Component.translatable("gui.supernaturalcraft.war_table.coords", m.x(), m.z()).withColor(MUTED & 0xFFFFFF));
        return out;
    }
}
