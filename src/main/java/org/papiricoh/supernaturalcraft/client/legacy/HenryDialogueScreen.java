package org.papiricoh.supernaturalcraft.client.legacy;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.papiricoh.supernaturalcraft.legacy.HenryDialogue;
import org.papiricoh.supernaturalcraft.network.LegacyChoicePayload;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;

/**
 * Henry Winchester talking (v0.17): his figure on the left, a card of typewritten lines on the right, typed out a letter at a
 * time; click (or space) for the next line. On the last line the stage's answers ({@link HenryDialogue.Stage#choices()})
 * appear; each sends a {@link LegacyChoicePayload}. The server answers with the next stage or closes the screen.
 */
public class HenryDialogueScreen extends Screen {

    private static final int W = 300, H = 150, CHARS_PER_TICK = 2;
    private final int henry;
    private HenryDialogue.Stage stage;
    private int line = 1, age;
    private boolean answered;

    public HenryDialogueScreen(int henry, HenryDialogue.Stage stage) {
        super(Component.translatable("screen.supernaturalcraft.henry"));
        this.henry = henry;
        this.stage = stage;
    }

    public int henry() {
        return henry;
    }

    /** The server moved the talk on. */
    public void stage(HenryDialogue.Stage next) {
        stage = next;
        line = 1;
        age = 0;
        answered = false;
    }

    @Override
    public void tick() {
        age++;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Component text() {
        return Component.translatable(HenryDialogue.line(stage, line));
    }

    private boolean typed() {
        return age * CHARS_PER_TICK >= text().getString().length();
    }

    private boolean last() {
        return line >= stage.lines;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        int x = (width - W) / 2, y = height - H - 16;
        // Henry, standing at the left of the card.
        Entity e = minecraft != null && minecraft.level != null ? minecraft.level.getEntity(henry) : null;
        if (e instanceof LivingEntity living) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, x - 4, y - 40, x + 84, y + H - 4, 42, 0.0625f, mx, my, living);
        }
        int cx = x + 84, cw = W - 84;
        g.fill(cx - 2, y - 2, cx + cw + 2, y + H + 2, 0xFF1A0F08);
        g.fill(cx, y, cx + cw, y + H, 0xFFEDE3C6);
        for (int ly = y + 22; ly < y + H - 4; ly += 10) g.fill(cx + 4, ly, cx + cw - 4, ly + 1, 0x223B6A8A);
        g.fill(cx + 14, y, cx + 15, y + H, 0x40B02020);
        g.drawString(font, Component.translatable("screen.supernaturalcraft.henry.name"), cx + 20, y + 8, 0xFF5A2A10, false);
        // The line, typed out.
        String full = text().getString();
        int shown = Math.min(full.length(), (int) ((age + partial) * CHARS_PER_TICK));
        int ty = y + 23;
        for (FormattedCharSequence l : font.split(Component.literal(full.substring(0, shown)), cw - 28)) {
            g.drawString(font, l, cx + 20, ty, 0xFF2A2018, false);
            ty += 10;
        }
        Component page = Component.literal(line + "/" + stage.lines);
        g.drawString(font, page, cx + cw - 8 - font.width(page), y + 8, 0xFF8A7A5A, false);
        if (!typed()) return;
        if (!last()) {
            Component more = Component.translatable("screen.supernaturalcraft.henry.more");
            g.drawString(font, more, cx + cw - 8 - font.width(more), y + H - 12, (age / 10) % 2 == 0 ? 0xFF5A2A10 : 0xFF8A6A40, false);
            return;
        }
        List<Byte> choices = stage.choices();
        for (int i = 0; i < choices.size(); i++) {
            int[] b = choiceBox(i, choices.size());
            boolean over = mx >= b[0] && my >= b[1] && mx < b[0] + b[2] && my < b[1] + b[3];
            g.fill(b[0], b[1], b[0] + b[2], b[1] + b[3], over ? 0xFFB08B36 : 0xFF634816);
            g.fill(b[0] + 1, b[1] + 1, b[0] + b[2] - 1, b[1] + b[3] - 1, over ? 0xFF2E5A36 : 0xFF15281B);
            Component label = Component.translatable(HenryDialogue.choiceKey(choices.get(i)));
            g.drawString(font, label, b[0] + (b[2] - font.width(label)) / 2, b[1] + 4, 0xFFF3E7C8, false);
        }
    }

    /** {x, y, w, h} of answer {@code i} of {@code n}, along the card's foot. */
    private int[] choiceBox(int i, int n) {
        int x = (width - W) / 2 + 84, cw = W - 84, y = height - 16 - 18;
        int bw = (cw - 12 - (n - 1) * 6) / n;
        return new int[]{x + 6 + i * (bw + 6), y, bw, 15};
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!typed()) {
            age = 10_000;
            return true;
        }
        if (!last()) {
            next();
            return true;
        }
        List<Byte> choices = stage.choices();
        for (int i = 0; i < choices.size(); i++) {
            int[] b = choiceBox(i, choices.size());
            if (mx >= b[0] && my >= b[1] && mx < b[0] + b[2] && my < b[1] + b[3]) {
                answer(choices.get(i));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER) {
            if (!typed()) age = 10_000;
            else if (!last()) next();
            else if (stage.choices().size() == 1) answer(stage.choices().get(0));
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private void next() {
        line++;
        age = 0;
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
    }

    private void answer(byte choice) {
        if (answered) return;
        answered = true;
        PacketDistributor.sendToServer(new LegacyChoicePayload(henry, choice));
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(AllSounds.LEGACY_TYPEWRITER.get(), 1.0f, 0.4f));
        if (choice == HenryDialogue.CLOSE) onClose();
    }

    @Override
    public void onClose() {
        if (!answered && stage.offers(HenryDialogue.CLOSE)) {
            answered = true;
            PacketDistributor.sendToServer(new LegacyChoicePayload(henry, HenryDialogue.CLOSE));
        }
        super.onClose();
    }

    /** For previews: jump to a line, typed out. */
    public void previewLine(int n) {
        line = Math.max(1, Math.min(stage.lines, n));
        age = 10_000;
    }
}
