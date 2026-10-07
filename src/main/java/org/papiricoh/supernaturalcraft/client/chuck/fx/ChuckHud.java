package org.papiricoh.supernaturalcraft.client.chuck.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The fourth wall, on the HUD. While the Author rewrites it ({@code HUD_REWRITE}): the held item's name is struck
 * out and an absurd one typed over it, the hotbar's items crossed through, the hearts covered over and redrawn by
 * hand in ink. Always, for his boss bar (a name key under {@code entity.supernaturalcraft.chuck.bar}): a bar of ink
 * that glitches, and a name typed, deleted and typed again. Visual only: nothing here changes what the game knows.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ChuckHud {

    /** How many absurd item names there are ({@code fourth_wall.supernaturalcraft.hud.item.<n>}). */
    public static final int ITEM_NAMES = 16;
    public static final String BAR_PREFIX = "entity.supernaturalcraft.chuck.bar";

    private static int rewrite, rewriteLife;
    private static long seed;
    private static int heartsY = -1;
    private static ItemStack lastSelected = ItemStack.EMPTY;
    private static ChuckText.Typist absurd;
    private static int absurdAge;

    private static final class Bar {
        String name = "";
        int age;
    }

    private static final Map<UUID, Bar> BARS = new HashMap<>();

    private ChuckHud() {
    }

    public static void rewrite(int duration) {
        rewriteLife = Math.max(40, duration);
        rewrite = rewriteLife;
        seed = new java.util.Random().nextLong();
        absurd = null;
    }

    public static boolean rewriting() {
        return rewrite > 0;
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            rewrite = 0;
            BARS.clear();
            return;
        }
        if (rewrite > 0) rewrite--;
        for (Bar b : BARS.values()) b.age++;
        if (absurd != null) absurd.tick(++absurdAge);
    }

    private static float alpha() {
        return Mth.clamp(Math.min((rewriteLife - rewrite) / 6f, rewrite / 15f), 0, 1);
    }

    /** The absurd name of an item, the same for the whole rewrite. */
    static String absurdName(ItemStack stack) {
        int h = BuiltInRegistries.ITEM.getKey(stack.getItem()).hashCode() ^ (int) seed;
        return Component.translatable("fourth_wall.supernaturalcraft.hud.item." + Math.floorMod(h, ITEM_NAMES)).getString();
    }

    @SubscribeEvent
    public static void onLayerPre(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)) heartsY = event.getGuiGraphics().guiHeight() - Minecraft.getInstance().gui.leftHeight;
        if (!rewriting()) return;
        if (event.getName().equals(VanillaGuiLayers.SELECTED_ITEM_NAME)) {
            event.setCanceled(true);
            selectedName(event.getGuiGraphics());
        }
    }

    @SubscribeEvent
    public static void onLayerPost(RenderGuiLayerEvent.Post event) {
        if (!rewriting()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        GuiGraphics g = event.getGuiGraphics();
        if (event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH) && heartsY >= 0 && mc.gameMode != null && mc.gameMode.canHurtPlayer()) {
            inkHearts(g, mc, heartsY);
        } else if (event.getName().equals(VanillaGuiLayers.HOTBAR) && !mc.player.isSpectator()) {
            float a = alpha();
            int x0 = g.guiWidth() / 2 - 91, y = g.guiHeight() - 22;
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getItem(i).isEmpty() || i == mc.player.getInventory().selected) continue;
                int x = x0 + 3 + i * 20;
                GuiDraw.line(g, x + 1, y + 16, x + 16, y + 4, 1.3f, ChuckText.argb(a * 0.85f, ChuckText.INK_BLUE));
            }
        }
    }

    /** The held item: its real name struck through, and above it the one he gives it, typed. */
    private static void selectedName(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ItemStack stack = mc.player.getMainHandItem();
        if (!ItemStack.isSameItem(stack, lastSelected) || absurd == null) {
            lastSelected = stack.copy();
            absurdAge = 0;
            absurd = new ChuckText.Typist(stack.isEmpty() ? Component.translatable("fourth_wall.supernaturalcraft.hud.empty").getString()
                    : absurdName(stack), 1.2f, 4, false);
        }
        float a = alpha();
        int w = g.guiWidth();
        int y = g.guiHeight() - 59 - (mc.gameMode != null && !mc.gameMode.canHurtPlayer() ? -14 : 0);
        if (!stack.isEmpty()) {
            Component real = stack.getHoverName();
            int rw = mc.font.width(real);
            g.drawString(mc.font, real, (w - rw) / 2, y, ChuckText.argb(a * 0.6f, 0xBFBFBF), true);
            GuiDraw.line(g, (w - rw) / 2f - 2, y + 4, (w + rw) / 2f + 2, y + 3.5f, 1.2f, ChuckText.argb(a, 0xC8261E));
        }
        Component full = ChuckText.typedItalic(absurd.text);
        float x = w / 2f - mc.font.width(full) / 2f;
        GuiDraw.text(g, ChuckText.typedItalic(absurd.visible(absurdAge)), x, y - 12, 1f, ChuckText.argb(a, 0xF2E8D0), true);
    }

    /** The hearts: papered over, struck out, and drawn again by hand in ink. */
    private static void inkHearts(GuiGraphics g, Minecraft mc, int y) {
        float a = alpha();
        int x0 = g.guiWidth() / 2 - 91;
        float max = Math.max(1, mc.player.getMaxHealth());
        int hearts = Math.min(10, Mth.ceil(max / 2));
        int full = Mth.ceil(mc.player.getHealth() / 2);
        g.fill(x0 - 1, y - 1, x0 + hearts * 8 + 2, y + 10, ChuckText.argb(a * 0.82f, ChuckText.PAPER));
        GuiDraw.line(g, x0 - 2, y + 6, x0 + hearts * 8 + 3, y + 3, 1.2f, ChuckText.argb(a, 0xC8261E));
        long tick = mc.level.getGameTime();
        for (int i = 0; i < hearts; i++) {
            long k = (tick / 4) * 31 + i * 17 + seed;
            int jx = (int) ((k >>> 3) % 3) - 1, jy = (int) ((k >>> 7) % 3) - 1;
            String glyph = i < full ? "♥" : "♡";
            g.drawString(mc.font, Component.literal(glyph), x0 + i * 8 + jx, y + 1 + jy, ChuckText.argb(a, ChuckText.INK), false);
        }
    }

    // --- the boss bar ------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        if (!(boss.getName().getContents() instanceof TranslatableContents tc) || !tc.getKey().startsWith(BAR_PREFIX)) return;
        event.setCanceled(true);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        GuiGraphics g = event.getGuiGraphics();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Bar bar = BARS.computeIfAbsent(boss.getId(), id -> new Bar());
        String name = boss.getName().getString();
        if (!name.equals(bar.name)) {
            bar.name = name;
            bar.age = 0;
        }
        int x = event.getX(), y = event.getY();
        double t = mc.level.getGameTime() + partial;
        drawBar(g, x, y, boss.getProgress(), t, boss.getId().hashCode());
        // The name: typed, held, backspaced, typed again.
        int cycle = 140, age = bar.age % cycle;
        int typed = Math.min(name.length(), (int) (age * 0.7f));
        int deleting = age - 105;
        if (deleting > 0) typed = Math.max(0, name.length() - deleting);
        String shown = name.substring(0, Math.max(0, Math.min(typed, name.length())));
        long k = (long) (t / 2) * 131 + boss.getId().getLeastSignificantBits();
        int jx = (int) Math.floorMod(k >>> 5, 3L) - 1;
        Component text = ChuckText.typed(shown + (((int) t / 5) % 2 == 0 ? "_" : " "));
        Component full = ChuckText.typed(name + "_");
        int tx = g.guiWidth() / 2 - mc.font.width(full) / 2 + jx;
        if (flick(t, 3) > 0.9f) g.drawString(mc.font, text, tx + 2, y - 9, 0xAAC8261E, false);
        g.drawString(mc.font, text, tx, y - 9, 0xFFF2E8D0, true);
    }

    private static void drawBar(GuiGraphics g, int x, int y, float progress, double t, int salt) {
        int w = 182;
        g.fill(x - 1, y - 1, x + w + 1, y + 6, 0xCC050407);
        g.fill(x, y, x + w, y + 5, 0xFFE9DFC6);
        boolean glitch = flick(t, salt) > 0.86f;
        int fill = Math.round(w * Mth.clamp(progress, 0, 1));
        // Ink, its right edge ragged and wet.
        for (int i = 0; i < fill; i++) {
            int top = y + (i > fill - 4 ? (int) (flick(t + i * 7, salt + i) * 2) : 0);
            int col = ((i * 13 + (int) (t / 3)) % 29 == 0) ? 0xFF3D3772 : 0xFF14112A;
            g.fill(x + i, top, x + i + 1, y + 5, col);
        }
        if (glitch) {
            // A slice of the bar slips sideways, and a red ghost of the fill flashes ahead of it.
            int sx = x + (int) (flick(t, salt + 9) * (w - 30));
            int off = (int) ((flick(t, salt + 11) - 0.5f) * 14);
            g.fill(sx + off, y + 1, sx + off + 26, y + 3, 0xFF14112A);
            g.fill(x + fill, y, Math.min(x + w, x + fill + 4 + (int) (flick(t, salt + 5) * 30)), y + 5, 0x99C8261E);
        }
    }

    private static float flick(double t, int salt) {
        long k = (long) (t / 2) * 0x9E3779B97F4A7C15L + salt * 0xBF58476D1CE4E5B9L;
        k ^= k >>> 31;
        k *= 0x94D049BB133111EBL;
        k ^= k >>> 29;
        return ((k >>> 40) & 0xFFFF) / 65535f;
    }
}
