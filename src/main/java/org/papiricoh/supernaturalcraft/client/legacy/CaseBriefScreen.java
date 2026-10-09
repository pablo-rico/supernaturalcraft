package org.papiricoh.supernaturalcraft.client.legacy;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseFile;

/**
 * A case file opened (v0.17): a manila folder with a typed brief -- the case's name, what it looks like (the scenario), the
 * suspect with a photograph pinned to it, what else is known (the twist), how dangerous, roughly where it is from here, and a
 * stamp with its state.
 */
public class CaseBriefScreen extends Screen {

    private static final int W = 280, H = 186;
    private static final int INK = 0xFF2A2018, FADED = 0xFF7A6A4A, MANILA = 0xFFDFC67A, MANILA_DARK = 0xFFB39447, PAPER = 0xFFF5EEDB;
    private final CaseFile file;
    private LivingEntity photo;

    public CaseBriefScreen(CaseFile file) {
        super(Component.translatable("legacy.supernaturalcraft.case.title", file.index() + 1));
        this.file = file;
    }

    public CaseFile file() {
        return file;
    }

    @Override
    protected void init() {
        if (photo == null && minecraft != null && minecraft.level != null) {
            photo = BuiltInRegistries.ENTITY_TYPE.getOptional(file.monster()).map(t -> t.create(minecraft.level))
                    .filter(e -> e instanceof LivingEntity).map(e -> (LivingEntity) e).orElse(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        int x = (width - W) / 2, y = (height - H) / 2;
        // The folder, its tab, the typed sheet on it.
        g.fill(x + 6, y - 10, x + 80, y + 2, MANILA_DARK);
        g.fill(x, y, x + W, y + H, MANILA_DARK);
        g.fill(x + 1, y + 1, x + W - 1, y + H - 1, MANILA);
        g.drawString(font, Component.translatable("screen.supernaturalcraft.case.tab"), x + 10, y - 8, INK, false);
        int px = x + 10, py = y + 8, pw = W - 20, ph = H - 16;
        g.fill(px + 2, py + 2, px + pw + 2, py + ph + 2, 0x40000000);
        g.fill(px, py, px + pw, py + ph, PAPER);
        g.drawString(font, title, px + 8, py + 8, INK, false);
        g.fill(px + 8, py + 18, px + pw - 8, py + 19, 0xFF8A2A20);
        int ty = py + 24;
        Component scenario = Component.translatable("legacy.supernaturalcraft.case.scenario." + file.scenario());
        g.drawString(font, scenario, px + 8, ty, INK, false);
        ty += 14;
        ty = field(g, px + 8, ty, pw - 90, "screen.supernaturalcraft.case.suspect", LegacyText.entityName(file.monster().toString()));
        String twist = file.twist().isEmpty() ? "none" : file.twist();
        ty = field(g, px + 8, ty, pw - 90, "screen.supernaturalcraft.case.known",
                Component.translatable("legacy.supernaturalcraft.case.twist." + twist));
        StringBuilder skulls = new StringBuilder();
        for (int i = 1; i <= 5; i++) skulls.append(i <= file.tier() ? '☠' : '·');
        ty = field(g, px + 8, ty, pw - 90, "screen.supernaturalcraft.case.danger", Component.literal(skulls.toString()));
        field(g, px + 8, ty, pw - 16, "screen.supernaturalcraft.case.where", where());
        // The photograph, pinned at the right.
        int fx = px + pw - 76, fy = py + 26, fw = 66, fh = 74;
        g.fill(fx - 1, fy - 1, fx + fw + 1, fy + fh + 1, 0xFF3A3020);
        g.fill(fx, fy, fx + fw, fy + fh, 0xFFE8E2D2);
        g.fill(fx + 4, fy + 4, fx + fw - 4, fy + fh - 14, 0xFF2A2A2E);
        if (photo != null) {
            g.enableScissor(fx + 4, fy + 4, fx + fw - 4, fy + fh - 14);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, fx + 4, fy + 4, fx + fw - 4, fy + fh - 14, 22, 0.0625f, mx, my, photo);
            g.disableScissor();
        }
        g.fill(fx + fw / 2 - 2, fy - 3, fx + fw / 2 + 2, fy + 1, 0xFFC02A20);
        // The stamp.
        int colour = switch (file.state()) {
            case CaseFile.SOLVED -> 0xC02E7A3A;
            case CaseFile.LOST -> 0xC0A02020;
            default -> 0xC06A4A20;
        };
        Component stamp = Component.translatable("screen.supernaturalcraft.case.state." + file.state());
        g.pose().pushPose();
        g.pose().translate(px + pw - 60, py + ph - 26, 0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-12));
        g.pose().scale(1.4f, 1.4f, 1);
        int sw = font.width(stamp);
        g.fill(-sw / 2 - 3, -3, sw / 2 + 3, 10, colour & 0x30FFFFFF);
        g.renderOutline(-sw / 2 - 3, -3, sw + 6, 13, colour);
        g.drawString(font, stamp, -sw / 2, 0, colour, false);
        g.pose().popPose();
    }

    private int field(GuiGraphics g, int x, int y, int w, String label, Component value) {
        Component l = Component.translatable(label);
        g.drawString(font, l, x, y, FADED, false);
        y += 10;
        for (FormattedCharSequence line : font.split(value, w)) {
            g.drawString(font, line, x + 4, y, INK, false);
            y += 10;
        }
        return y + 4;
    }

    /** "Roughly 800 blocks north-east of here, near X 1250, Z -400." */
    private Component where() {
        BlockPos site = file.site();
        if (minecraft == null || minecraft.player == null) return Component.empty();
        double dx = site.getX() + 0.5 - minecraft.player.getX(), dz = site.getZ() + 0.5 - minecraft.player.getZ();
        int dist = (int) (Math.round(Math.sqrt(dx * dx + dz * dz) / 50.0) * 50);
        if (dist < 50) return Component.translatable("screen.supernaturalcraft.case.here");
        String[] dirs = {"south", "south_west", "west", "north_west", "north", "north_east", "east", "south_east"};
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        int i = Math.floorMod((int) Math.round(angle / 45.0), 8);
        int rx = Math.round(site.getX() / 50f) * 50, rz = Math.round(site.getZ() / 50f) * 50;
        return Component.translatable("screen.supernaturalcraft.case.distance", dist,
                Component.translatable("screen.supernaturalcraft.case.dir." + dirs[i]), rx, rz);
    }
}
