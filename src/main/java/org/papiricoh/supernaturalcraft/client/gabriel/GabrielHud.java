package org.papiricoh.supernaturalcraft.client.gabriel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.Channel;

import static org.papiricoh.supernaturalcraft.client.gabriel.GabrielGui.*;

/**
 * Gabriel's boss bar as a programme banner (v0.14), for his bar keys ({@code entity.supernaturalcraft.gabriel.bar.<channel>}):
 * the banner ({@code bar.png}) where vanilla's bar would be, near the top; the programme's name in its band; his health as
 * the strip under it, in the channel's colour, with a white flash where a blow took a bite; and a red ON AIR lamp that
 * blinks. A plain stand-in while the texture is missing.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class GabrielHud {

    public static final String BAR_PREFIX = "entity.supernaturalcraft.gabriel.bar";
    /** The progress track inside the banner, and the name band (see the requests to the art). */
    static final int TRACK_X = 8, TRACK_Y = 24, TRACK_W = 240, TRACK_H = 6, NAME_Y = 6, NAME_H = 12;

    private static float lastProgress = -1, bitten;
    private static long biteAt = Long.MIN_VALUE / 2, drawnAt = Long.MIN_VALUE / 2;
    private static float bottom;

    private GabrielHud() {
    }

    /** The y under his bar (where the signs and the quiz go), or near the top if no bar of his is up. */
    public static float bottom() {
        return ClientGabriel.ticks - drawnAt <= 2 ? bottom : 6;
    }

    /** The channel a bar key names (its last part), the sitcom if unknown. */
    static Channel channelOf(String key) {
        String id = key.substring(key.lastIndexOf('.') + 1);
        for (Channel c : Channel.values()) if (c.id().equals(id)) return c;
        return Channel.SITCOM;
    }

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        if (!(boss.getName().getContents() instanceof TranslatableContents tc) || !tc.getKey().startsWith(BAR_PREFIX)) return;
        event.setCanceled(true);
        event.setIncrement(BAR_H + 6);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        GuiGraphics g = event.getGuiGraphics();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float now = ClientGabriel.ticks + partial;
        Channel channel = channelOf(tc.getKey());
        int colour = channelColour(channel);
        float progress = Mth.clamp(boss.getProgress(), 0, 1);
        if (lastProgress >= 0 && progress < lastProgress - 0.0005f) {
            bitten = Math.max(bitten, lastProgress);
            biteAt = ClientGabriel.ticks;
        }
        if (progress > lastProgress + 0.05f) bitten = 0;
        lastProgress = progress;
        // As high as vanilla's own bar: its top band where vanilla writes the name.
        int x0 = g.guiWidth() / 2 - BAR_W / 2, y0 = Math.max(1, event.getY() - 11);
        bottom = y0 + BAR_H;
        drawnAt = ClientGabriel.ticks;
        boolean art = has(BAR);
        if (!art) {
            g.fill(x0 + 2, y0 + 2, x0 + BAR_W - 2, y0 + BAR_H - 1, argb(0.85f, 0x14101C));
            g.fill(x0 + 2, y0 + 2, x0 + BAR_W - 2, y0 + 3, argb(1, colour));
            g.fill(x0 + 2, y0 + 21, x0 + BAR_W - 2, y0 + 22, argb(0.6f, colour));
        }
        // The banner first (its track is painted dark), then the track, the bite (white, fading), the health over it.
        if (art) blit(g, BAR, x0, y0, BAR_W, BAR_H, 0, 0, BAR_W, BAR_H, BAR_W, BAR_H, 1, 0xFFFFFF);
        // The track, the bite (white, fading), the health.
        int tx = x0 + TRACK_X, ty = y0 + TRACK_Y;
        if (!art) g.fill(tx, ty, tx + TRACK_W, ty + TRACK_H, argb(0.9f, 0x0A0A0E));
        float bite = Mth.clamp(1 - (now - biteAt) / 20f, 0, 1);
        if (bite > 0 && bitten > progress) {
            g.fill(tx + Math.round(TRACK_W * progress), ty, tx + Math.round(TRACK_W * bitten), ty + TRACK_H, argb(bite, 0xFFFFFF));
        } else {
            bitten = 0;
        }
        int fill = Math.round(TRACK_W * progress);
        g.fillGradient(tx, ty, tx + fill, ty + TRACK_H, argb(1, lighten(colour)), argb(1, colour));
        // Little ticks every tenth, as on a programme's running bar.
        for (int i = 1; i < 10; i++) g.fill(tx + TRACK_W * i / 10, ty, tx + TRACK_W * i / 10 + 1, ty + 2, argb(0.5f, 0x000000));
        // ON AIR: a red lamp that blinks once a second, its word beside it.
        boolean on = ((int) (now / 10)) % 2 == 0;
        int lx = x0 + 7, ly = y0 + 7;
        g.fill(lx, ly, lx + 10, ly + 10, argb(1, 0x2A0806));
        g.fill(lx + 1, ly + 1, lx + 9, ly + 9, argb(1, on ? 0xFF3B30 : 0x5A1410));
        if (on) g.fill(lx - 2, ly - 2, lx + 12, ly + 12, argb(0.25f, 0xFF3B30));
        GuiDraw.text(g, Component.translatable("hud.supernaturalcraft.gabriel.on_air"), lx + 13, ly + 2, 0.6f,
                argb(on ? 1 : 0.5f, 0xFF6A5E), false);
        // The programme's name, in its band.
        Component name = Component.literal(boss.getName().getString());
        float scale = Math.min(1f, (BAR_W - 70) / (float) Math.max(1, GuiDraw.width(name, 1f)));
        GuiDraw.centred(g, name, x0 + BAR_W / 2f + 10, y0 + NAME_Y + (NAME_H - 8 * scale) / 2f, scale, argb(1, CREAM), true);
    }

    private static int lighten(int rgb) {
        int r = Math.min(255, ((rgb >> 16) & 255) + 70), gg = Math.min(255, ((rgb >> 8) & 255) + 70), b = Math.min(255, (rgb & 255) + 70);
        return r << 16 | gg << 8 | b;
    }
}
