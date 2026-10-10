package org.papiricoh.supernaturalcraft.client.heaven;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.network.AshChoicePayload;

import java.util.List;

/**
 * Ash's menu at the Roadhouse (v0.18, from {@code AshMenuPayload}): on the left what he has to say (a hint about what comes
 * next, by the hunter's progress), on the right the Heavens he can open the way to, and the three things he does for the
 * hunter's own (take them home, welcome or turn away visitors, another word of advice). Every choice goes back as an
 * {@link AshChoicePayload}, validated by the server.
 */
public class AshMenuScreen extends Screen {

    static final int W = 300, H = 180, ROWS = 5;

    private final int ash;
    private AshMenu menu;
    private int scroll, left, top, age;

    public AshMenuScreen(int ash, AshMenu menu) {
        super(Component.translatable("screen.supernaturalcraft.ash"));
        this.ash = ash;
        this.menu = menu;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int rx = left + W / 2 + 6, rw = W / 2 - 14;
        List<AshMenu.Heaven> heavens = menu.visitsEnabled() ? menu.heavens() : List.of();
        int max = Math.max(0, heavens.size() - ROWS);
        scroll = Mth.clamp(scroll, 0, max);
        for (int i = 0; i < Math.min(ROWS, heavens.size()); i++) {
            AshMenu.Heaven h = heavens.get(scroll + i);
            addRenderableWidget(new BarButton(rx, top + 34 + i * 18, rw, 16, Component.literal(h.name()),
                    () -> choose(AshChoicePayload.VISIT, h.uuid())));
        }
        int by = top + H - 52;
        if (menu.home()) {
            addRenderableWidget(new BarButton(rx, by, rw, 16, Component.translatable("screen.supernaturalcraft.ash.home"),
                    () -> choose(AshChoicePayload.GO_HOME, "")));
        }
        if (menu.visitsEnabled()) addRenderableWidget(new BarButton(rx, by + 18, rw, 16, Component.translatable(menu.welcome()
                ? "screen.supernaturalcraft.ash.visitors_on" : "screen.supernaturalcraft.ash.visitors_off"), () -> {
            PacketDistributor.sendToServer(new AshChoicePayload(ash, AshChoicePayload.TOGGLE_VISITORS, ""));
            // Shown at once; the server's word follows if it disagrees.
            menu = menu.withWelcome(!menu.welcome());
            rebuildWidgets();
        }));
        int lx = left + 10, lw = W / 2 - 16;
        addRenderableWidget(new BarButton(lx, by + 18, lw / 2 - 2, 16, Component.translatable("screen.supernaturalcraft.ash.hint"),
                () -> PacketDistributor.sendToServer(new AshChoicePayload(ash, AshChoicePayload.HINT, ""))));
        addRenderableWidget(new BarButton(lx + lw / 2 + 2, by + 18, lw / 2 - 2, 16, Component.translatable("screen.supernaturalcraft.ash.bye"),
                this::onClose));
    }

    private void choose(byte action, String arg) {
        PacketDistributor.sendToServer(new AshChoicePayload(ash, action, arg));
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public void onClose() {
        PacketDistributor.sendToServer(new AshChoicePayload(ash, AshChoicePayload.CLOSE, ""));
        super.onClose();
    }

    @Override
    public void tick() {
        age++;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int max = Math.max(0, menu.heavens().size() - ROWS);
        int next = Mth.clamp(scroll - (int) Math.signum(sy), 0, max);
        if (next != scroll) {
            scroll = next;
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.renderBackground(g, mouseX, mouseY, partial);
        HeavenGui.panel(g, HeavenGuiAtlas.ASH_PANEL, left, top, left + W, top + H, HeavenGui.Style.BAR, 1f);
        // Neon over the bar: the Roadhouse's sign, flickering now and then.
        float flicker = (age / 7) % 23 == 0 ? 0.5f : 1f;
        Component sign = Component.translatable("screen.supernaturalcraft.ash.title");
        g.drawCenteredString(font, sign, left + W / 2, top + 8, HeavenGui.argb(flicker, 0xFF6FB0));
        g.fill(left + 10, top + 20, left + W - 10, top + 21, HeavenGui.argb(0.7f, HeavenGui.BRASS));
        g.fill(left + W / 2, top + 26, left + W / 2 + 1, top + H - 12, HeavenGui.argb(0.4f, HeavenGui.BRASS));
        // Ash's word: the current hint, wrapped.
        int lx = left + 10, lw = W / 2 - 16;
        g.drawString(font, Component.translatable("screen.supernaturalcraft.ash.says"), lx, top + 26, HeavenGui.argb(1, HeavenGui.BRASS), false);
        List<Component> said = menu.lines().isEmpty() ? List.of(Component.translatable("screen.supernaturalcraft.ash.nothing")) : menu.lines();
        int y = top + 40;
        for (Component text : said) {
            for (FormattedCharSequence line : font.split(text.copy().withStyle(s -> s.withItalic(true)), lw)) {
                if (y > top + H - 62) break;
                g.drawString(font, line, lx, y, 0xFFF0E2C0, false);
                y += 10;
            }
            y += 4;
        }
        int rx = left + W / 2 + 6;
        g.drawString(font, Component.translatable("screen.supernaturalcraft.ash.visit"), rx, top + 26, HeavenGui.argb(1, HeavenGui.BRASS), false);
        if (!menu.visitsEnabled()) {
            g.drawString(font, Component.translatable("screen.supernaturalcraft.ash.visits_off"), rx, top + 40, 0xFF9A8A70, false);
        } else if (menu.heavens().isEmpty()) {
            g.drawString(font, Component.translatable("screen.supernaturalcraft.ash.no_heavens"), rx, top + 40, 0xFF9A8A70, false);
        } else if (menu.heavens().size() > ROWS) {
            g.drawString(font, (scroll + 1) + "-" + Math.min(menu.heavens().size(), scroll + ROWS) + "/" + menu.heavens().size(),
                    left + W - 40, top + 26, 0xFF9A8A70, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** A button of the bar: dark wood, brass edge, warm text; brighter under the mouse. */
    static final class BarButton extends AbstractButton {
        private final Runnable action;

        BarButton(int x, int y, int w, int h, Component text, Runnable action) {
            super(x, y, w, h, text);
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
            boolean hot = isHoveredOrFocused();
            int x0 = getX(), y0 = getY(), x1 = x0 + width, y1 = y0 + height;
            g.fill(x0, y0, x1, y1, hot ? 0xFF5A3E26 : 0xFF3A2616);
            g.fill(x0, y0, x1, y0 + 1, hot ? 0xFFE8C46A : 0xFF8A6A30);
            g.fill(x0, y1 - 1, x1, y1, 0xFF1A100A);
            var font = net.minecraft.client.Minecraft.getInstance().font;
            g.drawCenteredString(font, org.papiricoh.supernaturalcraft.client.book.journal.Ink.fit(font, getMessage(), width - 6),
                    x0 + width / 2, y0 + (height - 8) / 2, hot ? 0xFFFFF0C8 : 0xFFE0CCA0);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
            defaultButtonNarrationText(out);
        }
    }
}
