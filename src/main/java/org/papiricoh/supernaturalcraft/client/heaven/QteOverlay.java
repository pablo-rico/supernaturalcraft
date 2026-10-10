package org.papiricoh.supernaturalcraft.client.heaven;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;
import org.papiricoh.supernaturalcraft.network.ChairStrugglePayload;

/**
 * Strapped into Naomi's chair (v0.18): not a screen (the fight goes on around you), a strap across the bottom of the HUD with
 * how far the buckles have given and how long before the drill comes down. Every press of jump counts; the presses go to the
 * server every {@link #SEND_EVERY} ticks ({@link ChairStrugglePayload}), which validates them and answers with the count it
 * accepted ({@code HeavenFxPayload.QTE_PROGRESS}).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class QteOverlay {

    public static final int SEND_EVERY = 5;

    private static int chair = -1, needed = 1, counted, unsent, sentSinceAnswer, left, total = 1, sinceSend, shake, age;

    private QteOverlay() {
    }

    /** Strapped in: {@code chair} = the chair entity, {@code presses} to break free, {@code ticks} before the drill. */
    public static void start(int chairId, int presses, int ticks) {
        chair = chairId;
        needed = Math.max(1, presses);
        counted = unsent = sentSinceAnswer = 0;
        total = Math.max(1, ticks);
        left = total;
        sinceSend = 0;
        age = 0;
    }

    /** The server's count. */
    public static void progress(int pressesCounted, int pressesNeeded) {
        counted = Math.max(0, pressesCounted);
        if (pressesNeeded > 0) needed = pressesNeeded;
        sentSinceAnswer = 0;
    }

    /** Out of the chair: broke free (or a friend cut the straps), or the drill. */
    public static void end(boolean free) {
        if (chair < 0) return;
        chair = -1;
        HeavenOverlay.add(new HeavenOverlay.Notice(Component.translatable(free ? "hud.supernaturalcraft.heaven.qte_free"
                : "hud.supernaturalcraft.heaven.qte_drilled"), free ? HeavenGui.CYAN : HeavenGui.ALARM, 50));
        if (!free) HeavenOverlay.add(new HeavenOverlay.Wash(HeavenGui.ALARM, 0.35f, 1, 2, 16));
    }

    public static boolean active() {
        return chair >= 0;
    }

    public static void clear() {
        chair = -1;
    }

    /** 0-1: how far the straps have given, counting presses not yet answered for. */
    public static float fraction() {
        return Mth.clamp((counted + sentSinceAnswer + unsent) / (float) needed, 0, 1);
    }

    static void tick() {
        if (chair < 0) return;
        age++;
        if (left > 0) left--;
        if (shake > 0) shake--;
        if (++sinceSend >= SEND_EVERY) {
            sinceSend = 0;
            if (unsent > 0) {
                PacketDistributor.sendToServer(new ChairStrugglePayload(chair, unsent));
                sentSinceAnswer += unsent;
                unsent = 0;
            }
        }
        // A struggle the server never answered (the chair gone): let go after a grace period.
        if (left <= 0 && age > total + 60) chair = -1;
    }

    private static void press() {
        if (chair < 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        unsent++;
        shake = 4;
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (chair < 0 || event.getAction() != GLFW.GLFW_PRESS) return;
        if (Minecraft.getInstance().options.keyJump.matches(event.getKey(), event.getScanCode())) press();
    }

    @SubscribeEvent
    public static void onMouse(InputEvent.MouseButton.Pre event) {
        if (chair < 0 || event.getAction() != GLFW.GLFW_PRESS) return;
        if (Minecraft.getInstance().options.keyJump.matchesMouse(event.getButton())) press();
    }

    static void render(GuiGraphics g, float partial, int w, int h) {
        if (chair < 0) return;
        float a = Mth.clamp((age + partial) / 5f, 0, 1);
        float time = Mth.clamp((left - partial) / total, 0, 1);
        // The drill coming: the screen's edge reddens as time runs out.
        float danger = 1 - time;
        if (danger > 0.4f) {
            float k = (danger - 0.4f) / 0.6f;
            for (int i = 0; i < 6; i++) {
                int e = i * 4;
                float al = k * 0.12f * (6 - i) / 6f;
                g.fill(0, e, w, e + 4, HeavenGui.argb(al, HeavenGui.ALARM));
                g.fill(0, h - e - 4, w, h - e, HeavenGui.argb(al, HeavenGui.ALARM));
            }
        }
        int bw = Math.min(220, w - 40), bh = 34, x0 = (w - bw) / 2, y0 = h - 92;
        float sx = shake > 0 ? (shake % 2 == 0 ? 1.5f : -1.5f) : 0;
        g.pose().pushPose();
        g.pose().translate(sx, 0, 0);
        HeavenGui.panel(g, HeavenGuiAtlas.QTE_FRAME, x0, y0, x0 + bw, y0 + bh, HeavenGui.Style.CLINIC, a);
        Minecraft mc = Minecraft.getInstance();
        Component title = Component.translatable("hud.supernaturalcraft.heaven.qte", mc.options.keyJump.getTranslatedKeyMessage());
        GuiDraw.centred(g, title, w / 2f, y0 + 4, 0.9f, HeavenGui.argb(a, 0x1E3A44), false);
        // The straps: the bar the presses fill.
        int bx0 = x0 + 8, bx1 = x0 + bw - 8, by0 = y0 + 15, by1 = y0 + 22;
        g.fill(bx0 - 1, by0 - 1, bx1 + 1, by1 + 1, HeavenGui.argb(a, 0x3A2A1E));
        g.fill(bx0, by0, bx1, by1, HeavenGui.argb(a, 0x6B4A30));
        // Buckle marks along the strap.
        for (int i = 1; i < 6; i++) {
            int x = bx0 + (bx1 - bx0) * i / 6;
            g.fill(x, by0, x + 1, by1, HeavenGui.argb(a * 0.6f, 0xC9B79A));
        }
        HeavenGui.bar(g, HeavenGuiAtlas.QTE_FILL, bx0, by0, bx1, by1, fraction(), 0xF2F8FA, a);
        // The drill's time: a thin cyan line under it, red at the end.
        HeavenGui.bar(g, time < 0.3f ? HeavenGuiAtlas.QTE_DANGER : HeavenGuiAtlas.QTE_FILL, bx0, y0 + 26, bx1, y0 + 29, time,
                time < 0.3f ? HeavenGui.ALARM : HeavenGui.CYAN, a);
        g.pose().popPose();
    }
}
