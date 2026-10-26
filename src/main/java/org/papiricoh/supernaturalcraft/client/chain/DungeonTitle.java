package org.papiricoh.supernaturalcraft.client.chain;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Entry;

import static org.papiricoh.supernaturalcraft.client.chain.ChainOverlay.*;

/**
 * A stronghold's title card: the first time the player comes within {@link #ARRIVE} blocks of a built court's origin (in the
 * Overworld, measured flat), the band of its side ({@code court_<side>.png}) opens from the middle with its name, "The Pit
 * Castle" or "The Monastery", and under it whose court it is ("Court of <the Lord>") or that the throne stands vacant. It
 * arms again once the player is {@link #REARM} blocks away. It waits for an officer's title card to finish, and is let
 * through camera sequences.
 */
public final class DungeonTitle implements LayeredDraw.Layer {

    public static final ResourceLocation LAYER = SupernaturalCraft.asResource("chain_court_title");
    static final double ARRIVE = 40, REARM = 120;
    private static final int LIFE = 120, IN = 16, OUT = 26;
    private static final String[] FACTIONS = {"demon", "angel"};

    /** Per faction (demon, angel): whether the next arrival shows the card. */
    private static final boolean[] ARMED = {true, true};
    private static Title title;

    private record Title(int side, Component name, Component subtitle, int[] age) {
    }

    private DungeonTitle() {
    }

    public static DungeonTitle layer() {
        return new DungeonTitle();
    }

    static void tick(Minecraft mc) {
        Player player = mc.player;
        if (mc.level == null || player == null) {
            title = null;
            return;
        }
        Title t = title;
        if (t != null) {
            // It waits while an officer's card is up, then plays out.
            if (!(t.age[0] == 0 && OfficerCard.active()) && ++t.age[0] >= LIFE) title = null;
        }
        if (player.tickCount % 10 != 0 || mc.level.dimension() != Level.OVERWORLD) return;
        for (int i = 0; i < FACTIONS.length; i++) {
            ChainSyncPayload.Court court = ClientChain.court(FACTIONS[i]);
            if (court == null || !court.built()) continue;
            double dx = player.getX() - (court.x() + 0.5), dz = player.getZ() - (court.z() + 0.5);
            double flat = Math.sqrt(dx * dx + dz * dz);
            if (flat > REARM) {
                ARMED[i] = true;
            } else if (ARMED[i] && flat <= ARRIVE && Math.abs(player.getY() - court.y()) <= 64) {
                ARMED[i] = false;
                show(i, court);
            }
        }
    }

    private static void show(int faction, ChainSyncPayload.Court court) {
        String id = FACTIONS[faction];
        Entry lord = lordOf(id);
        Component subtitle = court.vacantUntil() >= 0 || lord == null
                ? Component.translatable("gui.supernaturalcraft.chain.vacant")
                : Component.translatable("gui.supernaturalcraft.chain.court_of", name(lord));
        title = new Title(faction == 0 ? PIT : HOST, Component.translatable("gui.supernaturalcraft.chain.court." + id), subtitle,
                new int[1]);
    }

    static void clear() {
        title = null;
        ARMED[0] = ARMED[1] = true;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Title t = title;
        Minecraft mc = Minecraft.getInstance();
        if (t == null || mc.player == null || mc.options.hideGui || t.age[0] == 0 && OfficerCard.active()) return;
        float time = t.age[0] + delta.getGameTimeDeltaPartialTick(false);
        float in = smooth(time / IN), out = smooth((LIFE - time) / OUT);
        float a = Math.min(in, out);
        if (a <= 0.02f) return;
        int w = g.guiWidth(), h = g.guiHeight();
        float scale = Math.min(1.25f, (w - 12) / (float) COURT_W);
        float top = Math.max(6, Math.min(h * 0.18f, h / 2f - 28 - COURT_H * scale));
        top = Math.max(top, OfficerBar.bossBottom() + 2);
        int side = t.side;
        int host = side == HOST ? 1 : 0;
        g.pose().pushPose();
        g.pose().translate(w / 2f - COURT_W * scale / 2f, top, 0);
        g.pose().scale(scale, scale, 1);
        // The band opens from the middle.
        float open = smooth(time / 22f) * COURT_W / 2f;
        int x0 = Math.round(COURT_W / 2f - open), x1 = Math.round(COURT_W / 2f + open);
        if (x1 > x0) {
            if (has(COURT[host])) {
                blit(g, COURT[host], x0, 0, x1 - x0, COURT_H, x0, 0, x1 - x0, COURT_H, COURT_W, COURT_H, a);
            } else {
                g.fill(x0, 20, x1, 60, argb(a * 0.7f, PLATE[side]));
                g.fill(x0, 18, x1, 19, argb(a, ACCENT[side]));
                g.fill(x0, 61, x1, 62, argb(a, ACCENT[side]));
            }
        }
        // The name, then whose court it is.
        float ns = Math.min(2.25f, 228f / Math.max(1, GuiDraw.width(t.name, 1f)));
        float nameA = a * smooth((time - 6) / 14f);
        centred(g, t.name, COURT_W / 2f, 23 + (18 - 8 * ns) / 2f, ns, nameA, INK[side]);
        float sub = a * smooth((time - 16) / 14f);
        float ss = Math.min(1f, 228f / Math.max(1, GuiDraw.width(t.subtitle, 1f)));
        centred(g, t.subtitle, COURT_W / 2f, 46, ss, sub, ACCENT[side]);
        g.pose().popPose();
    }
}
