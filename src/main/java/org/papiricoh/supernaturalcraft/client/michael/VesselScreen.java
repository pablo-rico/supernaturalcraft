package org.papiricoh.supernaturalcraft.client.michael;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;
import org.papiricoh.supernaturalcraft.network.VesselAnswerPayload;

import static org.papiricoh.supernaturalcraft.client.michael.MichaelGui.*;

/**
 * "Michael needs your yes": a panel of light with his words, Yes and No, and the seconds left running down. The answer
 * goes to the server ({@link VesselAnswerPayload}), which checks it was asked and is in time; walking away or letting the
 * time run out is a no.
 */
public class VesselScreen extends Screen {

    private static final int W = 256, H = 160;

    private final int michael, life;
    private int age, left, top;
    private boolean answered;

    public VesselScreen(int michael, int life) {
        super(Component.translatable("screen.supernaturalcraft.michael.ask"));
        this.michael = michael;
        this.life = Math.max(20, life);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.translatable("screen.supernaturalcraft.michael.yes"), b -> answer(true))
                .bounds(left + 28, top + H - 32, 90, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.supernaturalcraft.michael.no"), b -> answer(false))
                .bounds(left + W - 118, top + H - 32, 90, 20).build());
    }

    private void answer(boolean yes) {
        if (answered) return;
        answered = true;
        PacketDistributor.sendToServer(new VesselAnswerPayload(michael, yes));
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public void tick() {
        super.tick();
        age++;
        // Silence is an answer too (the server counts it as no when his time runs out).
        if (age >= life || minecraft == null || minecraft.level == null || minecraft.level.getEntity(michael) == null) {
            answered = true;
            onClose();
        }
    }

    @Override
    public void onClose() {
        if (!answered) {
            answered = true;
            PacketDistributor.sendToServer(new VesselAnswerPayload(michael, false));
        }
        super.onClose();
    }

    /** Seconds left, for the countdown. */
    public int secondsLeft() {
        return Math.max(0, Mth.ceil((life - age) / 20f));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        // The world stays visible behind, washed in light.
        g.fill(0, 0, width, height, argb(0.35f, LIGHT));
        if (has(YES_PANEL)) {
            blit(g, YES_PANEL, left, top, W, H, 0, 0, W, H, W, H, 1, 0xFFFFFF);
        } else {
            g.fill(left - 2, top - 2, left + W + 2, top + H + 2, argb(1, GOLD_DEEP));
            g.fill(left - 1, top - 1, left + W + 1, top + H + 1, argb(1, GOLD));
            g.fillGradient(left, top, left + W, top + H, argb(0.96f, 0xFFFDF4), argb(0.96f, 0xE6F2FF));
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        GuiDraw.centred(g, title, width / 2f, top + 14, 1.4f, argb(1, INK), false);
        int y = top + 38;
        Component body = Component.translatable("screen.supernaturalcraft.michael.ask.body").withStyle(s -> s.withItalic(true));
        for (FormattedCharSequence line : font.split(body, W - 40)) {
            g.drawString(font, line, left + 20, y, argb(1, 0x4A3A20), false);
            y += 11;
        }
        // The seconds, and a bar of light running out.
        float share = Mth.clamp((life - age - partial) / life, 0, 1);
        int bx = left + 28, bw = W - 56, by = top + H - 46;
        g.fill(bx, by, bx + bw, by + 3, argb(0.4f, GOLD_DEEP));
        g.fill(bx, by, bx + Math.round(bw * share), by + 3, argb(1, GOLD));
        Component secs = Component.literal(String.valueOf(secondsLeft()));
        GuiDraw.centred(g, secs, width / 2f, by - 14, 1.6f, argb(1, GOLD_DEEP), false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
