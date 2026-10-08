package org.papiricoh.supernaturalcraft.client.allegiance;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.network.AllegianceChoicePayload;
import org.papiricoh.supernaturalcraft.network.AllegianceDialoguePayload;

import java.util.ArrayList;
import java.util.List;

import static org.papiricoh.supernaturalcraft.client.allegiance.AllegianceGui.*;

/**
 * The shared allegiance dialogue (Heaven's messenger, Lucifer's offer…): a panel at the foot of the screen with the
 * speaker's name, their line revealed letter by letter (a click or a key finishes it) and the answers to click or pick
 * with 1–9. Light for an angel, embers for anything of Hell. The answer goes to the server as an
 * {@link AllegianceChoicePayload}; walking away, closing or letting the time run out sends the last option (silence).
 */
public class AllegianceDialogueScreen extends Screen {

    private static final int W = 340, PAD = 12, ROW = 10, OPT = 13;
    private static final float SPEED = 1.8f;

    private AllegianceDialoguePayload page;
    private List<FormattedCharSequence> lines = List.of();
    private final List<Component> options = new ArrayList<>();
    private int age, shownAt, left, top, h;
    private boolean answered;

    public AllegianceDialogueScreen(AllegianceDialoguePayload page) {
        super(Component.translatable("screen.supernaturalcraft.allegiance.dialogue"));
        this.page = page;
    }

    public AllegianceDialoguePayload page() {
        return page;
    }

    /** The next line of the same talk: the panel turns to it. */
    public void next(AllegianceDialoguePayload p) {
        page = p;
        answered = false;
        shownAt = age;
        layout();
    }

    @Override
    protected void init() {
        layout();
    }

    private void layout() {
        int textW = W - PAD * 2;
        lines = font.split(Component.translatable(page.lineKey()), textW);
        options.clear();
        for (int i = 0; i < page.options().size(); i++) {
            options.add(Component.literal((i + 1) + ". ").append(Component.translatable(page.optionKey(page.options().get(i)))));
        }
        h = PAD + 12 + lines.size() * ROW + 8 + options.size() * OPT + PAD;
        left = (width - W) / 2;
        // Low on the screen, over the hotbar: the speaker stays in view above it.
        top = height - h - 6;
    }

    private Entity speaker() {
        return minecraft == null || minecraft.level == null ? null : minecraft.level.getEntity(page.entity());
    }

    private boolean holy() {
        if ("messenger".equals(page.dialogue())) return true;
        if ("lucifer_offer".equals(page.dialogue())) return false;
        Entity e = speaker();
        return e != null && Kin.isAngel(e);
    }

    private int letters() {
        int n = 0;
        for (FormattedCharSequence l : lines) n += length(l);
        return n;
    }

    private static int length(FormattedCharSequence l) {
        int[] n = {0};
        l.accept((i, style, cp) -> {
            n[0]++;
            return true;
        });
        return n[0];
    }

    private int shown(float partial) {
        return (int) ((age - shownAt + partial) * SPEED);
    }

    private boolean typing() {
        return shown(0) < letters();
    }

    /** For previews: the whole line at once. */
    public void finishTyping() {
        shownAt = age - (int) Math.ceil(letters() / SPEED) - 1;
    }

    @Override
    public void tick() {
        age++;
        Entity e = speaker();
        boolean gone = e == null || !e.isAlive() || minecraft.player == null || minecraft.player.distanceTo(e) > 16;
        boolean late = page.timeout() > 0 && age - shownAt >= page.timeout();
        if (!answered && (gone || late)) silence();
    }

    private void silence() {
        if (!page.options().isEmpty()) answer(page.options().size() - 1);
        else onClose();
    }

    private void answer(int i) {
        if (answered || i < 0 || i >= page.options().size()) return;
        answered = true;
        PacketDistributor.sendToServer(new AllegianceChoicePayload(page.entity(), page.dialogue(), page.node(), page.options().get(i)));
        // The server answers with the next line, or nothing more: close unless a new page arrives at once.
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public void onClose() {
        if (!answered && !page.options().isEmpty()) {
            answer(page.options().size() - 1);
            return;
        }
        super.onClose();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9 && !typing()) {
            answer(key - GLFW.GLFW_KEY_1);
            return true;
        }
        if ((key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER) && typing()) {
            finishTyping();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        if (typing()) {
            finishTyping();
            return true;
        }
        int i = optionAt(mx, my);
        if (i >= 0) {
            answer(i);
            return true;
        }
        return false;
    }

    private int optionsTop() {
        return top + PAD + 12 + lines.size() * ROW + 8;
    }

    private int optionAt(double mx, double my) {
        if (typing()) return -1;
        int oy = optionsTop();
        if (mx < left + PAD || mx > left + W - PAD || my < oy - 2) return -1;
        int i = (int) Math.floor((my - oy + 2) / OPT);
        return i >= 0 && i < options.size() ? i : -1;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        g.fillGradient(0, height / 2, width, height, 0x00000000, 0x90000000);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        boolean light = holy();
        int edge = light ? GRACE : CORRUPTION, deep = light ? GRACE_DEEP : CORRUPTION_DEEP;
        int paper = light ? 0xF6F0E2 : 0x1A0E10, ink = light ? 0x3A2A12 : 0xE8D8C8;
        g.fill(left - 2, top - 2, left + W + 2, top + h + 2, argb(0.95f, deep));
        g.fill(left - 1, top - 1, left + W + 1, top + h + 1, argb(1, edge));
        g.fillGradient(left, top, left + W, top + h, argb(0.96f, paper), argb(0.96f, light ? 0xE8EEF6 : 0x0C0608));
        // The speaker.
        Entity e = speaker();
        Component who = e != null ? e.getDisplayName() : Component.translatable("screen.supernaturalcraft.allegiance.dialogue");
        g.drawString(font, who, left + PAD, top + PAD - 2, argb(1, light ? GRACE_DEEP : 0xE0503A), false);
        // The line, letter by letter.
        int budget = shown(partial), y = top + PAD + 12;
        for (FormattedCharSequence l : lines) {
            if (budget <= 0) break;
            int n = length(l);
            FormattedCharSequence part = budget >= n ? l : cut(l, budget);
            g.drawString(font, part, left + PAD, y, argb(1, ink), false);
            budget -= n;
            y += ROW;
        }
        if (typing()) return;
        int hover = optionAt(mx, my), oy = optionsTop();
        for (int i = 0; i < options.size(); i++) {
            boolean hot = i == hover;
            int yy = oy + i * OPT;
            if (hot) g.fill(left + PAD - 4, yy - 2, left + W - PAD + 4, yy + OPT - 3, argb(0.25f, edge));
            g.drawString(font, options.get(i), left + PAD, yy, argb(1, hot ? (light ? 0x7A4E10 : 0xFF7A5A) : ink), false);
        }
        if (page.timeout() > 0) {
            float share = Mth.clamp(1 - (age - shownAt + partial) / page.timeout(), 0, 1);
            g.fill(left, top + h - 2, left + Math.round(W * share), top + h, argb(0.9f, edge));
        }
    }

    private static FormattedCharSequence cut(FormattedCharSequence l, int n) {
        return sink -> {
            int[] i = {0};
            return l.accept((pos, style, cp) -> i[0]++ < n && sink.accept(pos, style, cp));
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
