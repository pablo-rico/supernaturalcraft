package org.papiricoh.supernaturalcraft.bowl.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.BowlSpells;
import org.papiricoh.supernaturalcraft.bowl.Recitation;
import org.papiricoh.supernaturalcraft.bowl.SpellBowlBlock;
import org.papiricoh.supernaturalcraft.network.OpenRecitationPayload;
import org.papiricoh.supernaturalcraft.network.RecitationResultPayload;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.List;

/**
 * Speaking the incantation over a lit bowl. The Latin is shown on a parchment as written, accents
 * and all; the letters light up gold as they are typed, the next one is underlined, the rest are
 * faded ink. A bar shows the time left; it shakes and flashes red at each typo, which costs time.
 * Finishing, running out of time or Esc (abandoning, a failure) reports to the server, which has
 * the last word. The game keeps running behind it.
 */
public class RecitationScreen extends Screen {

    private static final ResourceLocation BG = SupernaturalCraft.asResource("textures/gui/journal.png");
    private static final int W = 256, H = 196;
    private static final int INK = 0xFF3B2A1A, FADED = 0xFFA8977A, GOLD = 0xFFE8B030, GLOW = 0x40FFD24A, RED = 0xFFB02020;
    private static final float TEXT_SCALE = 1.5f;
    /** Ticks the screen lingers once the words are spoken or have failed. */
    private static final int LINGER = 12;
    /** Ticks before a bowl that is not (yet) lit closes the screen: the block update can trail this payload. */
    private static final int SETTLE = 20;

    private final OpenRecitationPayload payload;
    private final Recitation recitation;
    private final Component spellName;
    private final List<Glyph> glyphs = new ArrayList<>();
    private int left, top;
    private int ticks;
    private int shake;
    private int lingering = -1;
    private boolean sent;

    private record Glyph(int index, String text, float x, float y) {
    }

    public RecitationScreen(OpenRecitationPayload payload) {
        super(Component.translatable("screen.supernaturalcraft.recitation"));
        this.payload = payload;
        this.recitation = new Recitation(payload.incantation(), payload.allowedTicks(), payload.penaltyTicks());
        this.spellName = Component.translatable(BowlSpells.nameKey(payload.spell()));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        layout();
    }

    /** Word-wraps the incantation into glyph positions (unscaled units, relative to the text box). */
    private void layout() {
        glyphs.clear();
        String text = recitation.original();
        float maxWidth = (W - 36) / TEXT_SCALE;
        float x = 0, y = 0;
        int i = 0;
        while (i < text.length()) {
            int end = i;
            while (end < text.length() && text.charAt(end) != ' ') end++;
            float wordWidth = font.width(text.substring(i, end));
            if (x > 0 && x + wordWidth > maxWidth) {
                x = 0;
                y += font.lineHeight + 3;
            }
            for (int k = i; k < end; k++) {
                String ch = String.valueOf(text.charAt(k));
                glyphs.add(new Glyph(k, ch, x, y));
                x += font.width(ch);
            }
            if (end < text.length()) {
                glyphs.add(new Glyph(end, " ", x, y));
                x += font.width(" ");
            }
            i = end + 1;
        }
    }

    @Override
    public void tick() {
        ticks++;
        if (shake > 0) shake--;
        if (lingering >= 0) {
            if (++lingering >= LINGER) closeQuietly();
            return;
        }
        if (minecraft != null && minecraft.level != null && ticks > SETTLE) {
            BlockState state = minecraft.level.getBlockState(payload.pos());
            if (!(state.getBlock() instanceof SpellBowlBlock) || !state.getValue(SpellBowlBlock.LIT)) {
                // Broken, or already settled by the server: nothing to report.
                sent = true;
                closeQuietly();
                return;
            }
        }
        recitation.tick(1);
        if (recitation.expired()) {
            send(false);
            lingering = 0;
        }
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (lingering >= 0) return true;
        Recitation.Result result = recitation.type(c);
        switch (result) {
            case LETTER -> play(AllSounds.RECITE_LETTER.get(), 0.9f + (recitation.cursor() % 6) * 0.04f, 0.6f);
            case DONE -> {
                play(AllSounds.RECITE_LETTER.get(), 1.3f, 0.8f);
                send(true);
                lingering = 0;
            }
            case TYPO -> {
                play(AllSounds.RECITE_TYPO.get(), 0.9f + minecraftRandom() * 0.2f, 0.8f);
                shake = 8;
                if (recitation.expired()) {
                    send(false);
                    lingering = 0;
                }
            }
            case IGNORED -> {
            }
        }
        return true;
    }

    private float minecraftRandom() {
        return minecraft == null || minecraft.level == null ? 0.5f : minecraft.level.random.nextFloat();
    }

    private void play(net.minecraft.sounds.SoundEvent sound, float pitch, float volume) {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    private void send(boolean success) {
        if (sent) return;
        sent = true;
        PacketDistributor.sendToServer(new RecitationResultPayload(payload.pos(), success, recitation.typos()));
    }

    /** Esc abandons the recitation: a failure. */
    @Override
    public void onClose() {
        send(false);
        super.onClose();
    }

    private void closeQuietly() {
        if (minecraft != null && minecraft.screen == this) minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // --- drawing --------------------------------------------------------------------------------

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        // No blur: the lit bowl smoking behind the page is part of the moment.
        g.fillGradient(0, 0, width, height, 0x50000000, 0x90000000);
        g.blit(BG, left, top, 0, 0, W, H, 256, 256);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        g.drawString(font, spellName, left + (W - font.width(spellName)) / 2, top + 12, INK, false);
        Component sub = Component.translatable("screen.supernaturalcraft.recitation.subtitle");
        g.drawString(font, sub, left + (W - font.width(sub)) / 2, top + 24, FADED, false);
        renderIncantation(g, partial);
        renderTimeBar(g, partial);
        Component hint = Component.translatable(recitation.done() ? "screen.supernaturalcraft.recitation.spoken"
                : recitation.expired() || (sent && !recitation.done()) ? "screen.supernaturalcraft.recitation.failed"
                : "screen.supernaturalcraft.recitation.hint");
        g.drawString(font, hint, left + (W - font.width(hint)) / 2, top + 150, recitation.done() ? 0xFFB07A10 : FADED, false);
    }

    private void renderIncantation(GuiGraphics g, float partial) {
        float time = ticks + partial;
        g.pose().pushPose();
        g.pose().translate(left + 18, top + 44, 0);
        g.pose().scale(TEXT_SCALE, TEXT_SCALE, 1);
        for (Glyph glyph : glyphs) {
            int i = glyph.index();
            boolean letters = recitation.lettersAt(i) > 0;
            boolean typed = letters ? recitation.typedAt(i) : recitation.displayIndex(i) <= recitation.cursor() && recitation.cursor() > 0;
            boolean current = letters && recitation.currentAt(i);
            int ix = Mth.floor(glyph.x()), iy = Mth.floor(glyph.y());
            if (typed && letters) {
                // A soft glow behind each spoken letter, pulsing slightly.
                float pulse = 0.5f + 0.5f * Mth.sin(time * 0.25f + i * 0.4f);
                int glow = ((int) (0x30 + 0x30 * pulse) << 24) | (GLOW & 0xFFFFFF);
                for (int[] d : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
                    g.drawString(font, glyph.text(), ix + d[0], iy + d[1], glow, false);
                }
                g.drawString(font, glyph.text(), ix, iy, GOLD, false);
            } else if (current) {
                g.drawString(font, glyph.text(), ix, iy, INK, false);
                if ((ticks / 6) % 2 == 0 || shake > 0) {
                    int w = Math.max(2, font.width(glyph.text()));
                    g.fill(ix, iy + font.lineHeight - 1, ix + w, iy + font.lineHeight, shake > 0 ? RED : INK);
                }
            } else {
                g.drawString(font, glyph.text(), ix, iy, typed ? GOLD : FADED, false);
            }
        }
        g.pose().popPose();
    }

    private void renderTimeBar(GuiGraphics g, float partial) {
        int barW = W - 48, barH = 6;
        int x = left + 24, y = top + 166;
        if (shake > 0 && minecraft != null && minecraft.level != null) {
            x += (int) ((minecraft.level.random.nextFloat() - 0.5f) * shake);
            y += (int) ((minecraft.level.random.nextFloat() - 0.5f) * shake * 0.5f);
        }
        float allowed = Math.max(1, recitation.allowedTicks());
        float left01 = Mth.clamp((recitation.remaining() - (recitation.over() ? 0 : partial)) / allowed, 0, 1);
        g.fill(x - 1, y - 1, x + barW + 1, y + barH + 1, 0xFF2A1A0A);
        g.fill(x, y, x + barW, y + barH, 0xFF6B5A40);
        int fill = Math.round(barW * left01);
        int color = left01 > 0.3f ? lerpColor(0xFFC8962E, 0xFFE8C060, (left01 - 0.3f) / 0.7f) : lerpColor(RED, 0xFFC8962E, left01 / 0.3f);
        if (fill > 0) g.fill(x, y, x + fill, y + barH, color);
        if (shake > 0) g.fill(x, y, x + barW, y + barH, ((int) (0x90 * shake / 8f) << 24) | (RED & 0xFFFFFF));
    }

    private static int lerpColor(int a, int b, float t) {
        t = Mth.clamp(t, 0, 1);
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return 0xFF000000 | (Math.round(ar + (br - ar) * t) << 16) | (Math.round(ag + (bg - ag) * t) << 8) | Math.round(ab + (bb - ab) * t);
    }
}
