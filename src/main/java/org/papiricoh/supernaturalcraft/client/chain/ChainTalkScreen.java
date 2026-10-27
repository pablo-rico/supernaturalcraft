package org.papiricoh.supernaturalcraft.client.chain;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.book.chain.ChainDraw;
import org.papiricoh.supernaturalcraft.network.AllegianceChoicePayload;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Entry;
import org.papiricoh.supernaturalcraft.network.ChainTalkPayload;

import java.util.ArrayList;
import java.util.List;

import static org.papiricoh.supernaturalcraft.client.chain.ChainOverlay.*;

/**
 * A conversation with an officer ({@link ChainTalkPayload}), low on the screen so the officer stays in view: a plate in its
 * side's metal ({@code talk.png}: the Pit's iron and blood, the Host's gold and night, the Fallen's ash) with its portrait in a
 * ring breaking the top edge, its name, rank and temperament, its line written out letter by letter (a click or Space
 * finishes it), and the hunter's answers, numbered (keys 1–9 answer too). A countdown runs along the foot when the officer
 * will not wait; silence then counts as the last answer. Closing walks away when walking away is an answer.
 */
public class ChainTalkScreen extends Screen {

    private static final ResourceLocation TALK = SupernaturalCraft.asResource("textures/gui/chain/talk.png");
    private static final int PAD = 12, ROW = 10, OPT = 17, PORTRAIT = 64, MAX_W = 380;
    private static final float SPEED = 1.8f;

    private final ChainTalkPayload talk;
    private List<FormattedCharSequence> lines = List.of();
    private final List<Component> options = new ArrayList<>();
    private int age, left, top, w, h, textX;
    private boolean answered;

    protected ChainTalkScreen(ChainTalkPayload talk) {
        super(Component.translatable("screen.supernaturalcraft.chain.talk"));
        this.talk = talk;
    }

    public static void open(ChainTalkPayload p) {
        Minecraft.getInstance().setScreen(new ChainTalkScreen(p));
    }

    /**
     * Who speaks when no officer stands behind the words (v0.29: a wild mob a guild would hire): the mob's own name, else a
     * stranger.
     */
    private Component stranger() {
        if (minecraft != null && minecraft.level != null && talk.entity() >= 0) {
            Entity mob = minecraft.level.getEntity(talk.entity());
            if (mob != null) return mob.getDisplayName();
        }
        return Component.translatable("screen.supernaturalcraft.chain.talk.stranger");
    }

    private @Nullable Entry entry() {
        return ClientChain.entry(talk.faction(), talk.officerId());
    }

    private int side() {
        Entry e = entry();
        int side = e != null ? ChainOverlay.side(e) : "angel".equals(talk.faction()) ? HOST
                : ClientChain.isGuild(talk.faction()) ? FALLEN : PIT;
        // SucroCorp never talks; its sheet row would not exist. A guild's officer speaks from the Fallen's plain ash.
        return side == LEVIATHAN ? PIT : guild(side) ? FALLEN : side;
    }

    @Override
    protected void init() {
        w = Math.min(MAX_W, width - 16);
        textX = PORTRAIT + 10;
        int textW = w - textX - PAD;
        lines = font.split(Component.translatable(talk.lineKey(), talk.args().toArray()), textW);
        options.clear();
        for (String o : talk.options()) options.add(Component.translatable("chain.supernaturalcraft.talk.option." + o));
        int body = 30 + lines.size() * ROW + 6;
        int answers = options.isEmpty() ? 14 : options.size() * OPT + 2;
        h = Math.max(PORTRAIT + 8, body + answers + PAD);
        left = (width - w) / 2;
        top = height - h - 8;
    }

    // --- answering -------------------------------------------------------------------------------

    private void answer(String option) {
        if (answered) return;
        answered = true;
        PacketDistributor.sendToServer(new AllegianceChoicePayload(talk.entity(), ChainTalkPayload.DIALOGUE, talk.node(), option));
        ClientChain.talk = null;
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void onClose() {
        // Closing without answering counts as walking away, when walking away is an answer.
        if (!answered && talk.options().contains("leave")) answer("leave");
        else {
            ClientChain.talk = null;
            super.onClose();
        }
    }

    @Override
    public void tick() {
        age++;
        if (answered) return;
        if (talk.timeout() > 0 && age >= talk.timeout()) {
            if (talk.options().isEmpty()) onClose();
            else answer(talk.options().getLast());
            return;
        }
        // An officer in the world who is gone or left behind ends the talk.
        if (talk.entity() >= 0 && minecraft != null && minecraft.level != null && minecraft.player != null) {
            Entity e = minecraft.level.getEntity(talk.entity());
            if (e == null || !e.isAlive() || minecraft.player.distanceTo(e) > 24) onClose();
        }
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

    private int shownAt;

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
    public boolean keyPressed(int key, int scan, int mods) {
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {
            int i = key - GLFW.GLFW_KEY_1;
            if (typing()) finishTyping();
            else if (i < talk.options().size()) answer(talk.options().get(i));
            return true;
        }
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER) {
            if (typing()) finishTyping();
            else if (talk.options().isEmpty()) onClose();
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
        if (talk.options().isEmpty()) {
            onClose();
            return true;
        }
        int i = optionAt(mx, my);
        if (i >= 0) answer(talk.options().get(i));
        return true;
    }

    private int optionsTop() {
        return top + 30 + lines.size() * ROW + 6;
    }

    private int optionAt(double mx, double my) {
        if (typing()) return -1;
        int oy = optionsTop();
        if (mx < left + textX - 2 || mx > left + w - PAD || my < oy) return -1;
        int i = (int) Math.floor((my - oy) / OPT);
        return i >= 0 && i < options.size() && my - oy - i * OPT < 15 ? i : -1;
    }

    // --- drawing ---------------------------------------------------------------------------------

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        g.fillGradient(0, height / 2, width, height, 0x00000000, 0xA0000000);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        int side = side();
        Entry e = entry();
        RenderSystem.enableBlend();
        ChainDraw.nine(g, TALK, 256, 192, 0, side * 64, 64, 64, 16, left, top, w, h);
        // A little depth: the plate darkens toward its foot.
        g.fillGradient(left + 5, top + h / 2, left + w - 5, top + h - 5, 0x00000000, 0x40000000);
        drawPortrait(g, e, side, mx, my, partial);

        int tx = left + textX, y = top + 9;
        Component name = e != null ? ClientChain.name(e) : stranger();
        g.drawString(font, name, tx, y, 0xFF000000 | (e != null && lord(e) ? ACCENT[HOST] : INK[side]), true);
        if (e != null) {
            Component rank = rankName(e.faction(), e.rank()).copy().append(" ").append(epithet(e));
            int nx = tx + font.width(name) + 6;
            int room = left + w - PAD - nx;
            Component temper = e.player().isEmpty() ? ClientChain.temperamentName(e) : Component.empty();
            int tw = font.width(temper);
            if (font.width(rank) + tw + 8 <= room) {
                g.drawString(font, rank, nx, y, 0xFF000000 | ACCENT[side], false);
                g.drawString(font, temper, left + w - PAD - tw, y, argb(0.7f, INK[side]), false);
            } else {
                g.drawString(font, net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(rank, Math.max(0, room))), nx, y, 0xFF000000 | ACCENT[side], false);
            }
            ChainOverlay.pips(g, side, e.level(), left + w - PAD, y + 11, 1, false);
        }
        // A rule under the heading.
        g.fill(tx, top + 22, left + w - PAD, top + 23, argb(0.45f, ACCENT[side]));

        int budget = shown(partial);
        y = top + 30;
        int ink = 0xFF000000 | INK[side];
        for (FormattedCharSequence l : lines) {
            if (budget <= 0) break;
            int n = length(l);
            g.drawString(font, budget >= n ? l : cut(l, budget), tx, y, ink, false);
            budget -= n;
            y += ROW;
        }
        if (!typing()) drawOptions(g, side, mx, my);
        if (talk.timeout() > 0) {
            float share = Mth.clamp(1 - (age + partial) / talk.timeout(), 0, 1);
            int x0 = left + 8, x1 = left + w - 8, by = top + h - 7;
            g.fill(x0, by, x1, by + 2, argb(0.5f, 0x000000));
            int end = x0 + Math.round((x1 - x0) * share);
            g.fillGradient(x0, by, end, by + 2, 0xFF000000 | FILL_HI[side], 0xFF000000 | FILL_LO[side]);
        }
    }

    private void drawOptions(GuiGraphics g, int side, int mx, int my) {
        int oy = optionsTop(), x0 = left + textX - 2, x1 = left + w - PAD;
        if (options.isEmpty()) {
            Component hint = Component.translatable("screen.supernaturalcraft.chain.talk.close");
            g.drawString(font, hint, x1 - font.width(hint), oy + 2, argb(0.6f + 0.25f * Mth.sin(age * 0.15f), INK[side]), false);
            return;
        }
        int hover = optionAt(mx, my);
        for (int i = 0; i < options.size(); i++) {
            boolean hot = i == hover;
            int y = oy + i * OPT;
            ChainDraw.strip(g, TALK, 256, 192, 192, side * 64 + (hot ? 14 : 0), 32, 14, 4, x0 + 8, y, x1 - x0 - 8);
            g.blit(TALK, x0, y + 1, 224, side * 64 + (hot ? 12 : 0), 12, 12, 256, 192);
            String n = String.valueOf(i + 1);
            g.drawString(font, n, x0 + 6 - font.width(n) / 2 + 1, y + 3, 0xFF000000 | (hot ? 0xFFFFFF : INK[side]), false);
            g.drawString(font, options.get(i), x0 + 18, y + 3, 0xFF000000 | (hot ? FILL_HI[side] : INK[side]), false);
        }
    }

    private void drawPortrait(GuiGraphics g, @Nullable Entry e, int side, int mx, int my, float partial) {
        int px = left + 4, py = top - 22, cx = px + 32, cy = py + 34;
        RenderSystem.enableBlend();
        g.blit(TALK, px, py, 128, side * 64, 64, 64, 256, 192);
        LivingEntity puppet = e == null ? null : OfficerPuppets.of(e);
        if (puppet != null) {
            g.enableScissor(cx - 20, cy - 26, cx + 20, cy + 20);
            float h = puppet.getBbHeight();
            float scale = 50 / (0.55f * h);
            float time = puppet.tickCount + partial;
            float yaw = Mth.clamp((cx - mx) / 60f, -1, 1) * 30 + Mth.sin(time * 0.03f) * 6;
            float pitch = -Mth.clamp((cy - my) / 80f, -1, 1) * 10;
            g.pose().pushPose();
            ChainDraw.puppet(g, puppet, cx, cy - 22 + 0.22f * h * scale, scale, 0.78f, yaw, pitch);
            g.pose().popPose();
            g.disableScissor();
        } else if (e != null && !e.player().isEmpty()) {
            ChainDraw.face(g, e.player(), cx - 16, cy - 16, 32);
        } else if (minecraft != null && minecraft.level != null && minecraft.level.getEntity(talk.entity()) instanceof LivingEntity live) {
            g.enableScissor(cx - 20, cy - 26, cx + 20, cy + 20);
            float scale = 50 / (0.55f * live.getBbHeight());
            net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventory(g, cx, cy + 4, scale,
                    new org.joml.Vector3f(0, live.getBbHeight() * 0.78f, 0), new org.joml.Quaternionf().rotateZ((float) Math.PI), null, live);
            g.disableScissor();
        }
        if (e != null && e.player().isEmpty()) {
            // What it thinks of the hunter, under the portrait.
            int favor = e.favor();
            org.papiricoh.supernaturalcraft.chain.Bonds.Kind kind = org.papiricoh.supernaturalcraft.chain.Bonds.kind(favor, false);
            Component word = kind != org.papiricoh.supernaturalcraft.chain.Bonds.Kind.NONE ? ClientChain.kindName(kind)
                    : Component.translatable("screen.supernaturalcraft.book.chain.regard." + (favor >= 15 ? "warm" : favor <= -15 ? "cold" : "indifferent"));
            int col = favor <= org.papiricoh.supernaturalcraft.chain.Bonds.RIVAL ? 0xF06A50 : favor >= org.papiricoh.supernaturalcraft.chain.Bonds.LOYAL ? 0xF2D257 : INK[side];
            if (top + h - 12 > py + 66) centred(g, word, cx, py + 68, 1f, 0.9f, col);
        }
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        RenderSystem.enableBlend();
        g.blit(TALK, px, py, 64, side * 64, 64, 64, 256, 192);
        if (e != null) medallion(g, side, rank(e.rank()), cx + 14, cy + 14, 18, 1);
        g.pose().popPose();
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
