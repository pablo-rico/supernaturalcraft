package org.papiricoh.supernaturalcraft.client.book.chain;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.chain.Bonds;
import org.papiricoh.supernaturalcraft.chain.ChainRank;
import org.papiricoh.supernaturalcraft.chain.talk.TalkOption;
import org.papiricoh.supernaturalcraft.client.book.BookAtlas.Sprite;
import org.papiricoh.supernaturalcraft.client.book.BookStyle;
import org.papiricoh.supernaturalcraft.client.book.HunterBookScreen;
import org.papiricoh.supernaturalcraft.client.chain.ClientChain;
import org.papiricoh.supernaturalcraft.client.chain.OfficerPuppets;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Bond;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Challenge;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Entry;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.History;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * An officer's record (v0.27), opened by clicking it in the chain's tree: on the left its portrait in the side's arch, its
 * name, title, rank, level, temperament, its regard for the reader and what it has done to them, then its past and where it
 * is; on the right a page with four tabs: its chronicle, its bonds (a name opens that officer's record), its duels and
 * challenges, and the messages the reader may send it.
 */
public final class OfficerModal extends ChainPanel {

    public enum Tab { CHRONICLE, BONDS, DUELS, ACTIONS }

    /** Left column, the portrait's arch and the text beside it; the page to the right. */
    private static final int LX = X0 + 12, LW = 152, ARCH_Y = Y0 + 12, TX = LX + 68, TW = LX + LW - TX;
    private static final int PX0 = LX + LW + 8, PX1 = X1 - 12, TAB_Y = Y0 + 10, PY0 = TAB_Y + 15, PY1 = Y1 - 12;
    /** Chronicle keys the Duels tab gathers. */
    private static final Set<String> DUEL_KEYS = Set.of("challenge", "duel_won", "duel_yield", "duel_death", "challenge_hunter",
            "coward", "hunter_duel_won", "hunter_duel_lost", "throne_duel", "champion_won", "vow");
    /** Messages sent today, so a button is not pressed twice: {@code faction:id:option} → day. */
    private static final Map<String, Long> SENT = new HashMap<>();

    private String faction;
    private int id;
    private Tab tab = Tab.CHRONICLE;
    private boolean ordering;
    private @Nullable Component notice;
    private final Column left = new Column(), page = new Column();

    OfficerModal(ChainSection section, HunterBookScreen book, Entry e) {
        super(section, book);
        show(e);
    }

    void show(Entry e) {
        faction = e.faction();
        id = e.id();
        ordering = false;
        notice = null;
        left.scroll = 0;
        page.scroll = 0;
    }

    void tab(Tab t) {
        tab = t;
        page.scroll = 0;
        ordering = false;
    }

    @Nullable
    Entry entry() {
        return ClientChain.entry(faction, id);
    }

    @Override
    boolean stale() {
        return entry() == null;
    }

    @Override
    int side() {
        Entry e = entry();
        return e == null ? 0 : ChainSection.kind(e);
    }

    // --- drawing ---------------------------------------------------------------------------------

    @Override
    void draw(GuiGraphics g, int mx, int my, float partial) {
        Entry e = entry();
        if (e == null) return;
        int side = side();
        drawPortrait(g, e, side, mx, my, partial);
        drawHeader(g, e, side);
        int bodyY = drawRegard(g, e, side, mx, my);
        left.place(LX, bodyY, LW, PY1 - bodyY);
        left.draw(g, leftRows(e, side), mx, my, side, false);
        drawTabs(g, side, mx, my);
        ChainAtlas.page(g, side, PX0, PY0, PX1 - PX0, PY1 - PY0 + 2);
        // The open tab sits on the page's top edge.
        drawTab(g, side, tab, true, mx, my);
        page.place(PX0 + 8, PY0 + 7, PX1 - PX0 - 12, PY1 - PY0 - 10);
        List<Row> rows = switch (tab) {
            case CHRONICLE -> chronicleRows(e);
            case BONDS -> bondRows(e);
            case DUELS -> duelRows(e);
            case ACTIONS -> actionRows(e, side);
        };
        page.draw(g, rows, mx, my, side, true);
    }

    private void drawPortrait(GuiGraphics g, Entry e, int side, int mx, int my, float partial) {
        int ax = LX, ay = ARCH_Y;
        RenderSystem.enableBlend();
        ChainAtlas.portraitBack(side).draw(g, ax, ay);
        if (!e.player().isEmpty()) {
            ChainDraw.face(g, e.player(), ax + 12, ay + 28, 40);
        } else {
            LivingEntity puppet = OfficerPuppets.of(e);
            if (puppet != null) {
                book.scissor(g, ax + 11, ay + 22, ax + 54, ay + 72);
                float h = puppet.getBbHeight();
                float scale = 56 / (0.62f * h);
                float time = puppet.tickCount + partial;
                float cx = ax + 32, cy = ay + 24 + 0.3f * h * scale;
                float yaw = inside(mx, my, ax, ay, 64, 80) ? Mth.clamp((cx - mx) / 30f, -1, 1) * 35 : Mth.sin(time * 0.03f) * 14;
                float pitch = inside(mx, my, ax, ay, 64, 80) ? -Mth.clamp((ay + 34 - my) / 30f, -1, 1) * 12 : 0;
                g.pose().pushPose();
                ChainDraw.puppet(g, puppet, cx, cy, scale, 0.7f, yaw, pitch);
                g.pose().popPose();
                g.disableScissor();
            }
        }
        g.pose().pushPose();
        g.pose().translate(0, 0, 90);
        RenderSystem.enableBlend();
        ChainAtlas.portraitFrame(side).draw(g, ax, ay);
        if (e.kills() > 0) {
            // The tally of the reader's deaths on the plinth.
            Sprite k = ChainAtlas.KILL;
            String n = "×" + e.kills();
            int w = k.w() + font.width(n);
            k.draw(g, ax + 32 - w / 2, ay + 70);
            g.drawString(font, n, ax + 32 - w / 2 + k.w(), ay + 71, 0xFFF08070, true);
        }
        g.pose().popPose();
        hit(ax, ay, 64, 80, null, portraitTip(e));
    }

    private List<Component> portraitTip(Entry e) {
        List<Component> tip = new ArrayList<>();
        tip.add(ClientChain.name(e).copy().withStyle(ChatFormatting.BOLD));
        tip.add(ChainSection.title(e).copy().withStyle(ChatFormatting.GRAY));
        if (e.kills() > 0) tip.add(Component.translatable(K + "kills", e.kills()).withStyle(ChatFormatting.RED));
        return tip;
    }

    private void drawHeader(GuiGraphics g, Entry e, int side) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 90);
        Component name = ClientChain.name(e);
        int nw = font.width(name);
        float s = Math.min(1.25f, TW / (float) Math.max(1, nw));
        g.pose().pushPose();
        g.pose().translate(TX, ARCH_Y + 4, 0);
        g.pose().scale(s, s, 1);
        g.drawString(font, name, 0, 0, nameColour(e, side), false);
        g.pose().popPose();
        int y = ARCH_Y + 6 + Math.round(9 * s) + 2;
        var title = font.split(ChainSection.title(e), TW);
        for (int i = 0; i < Math.min(2, title.size()); i++) {
            g.drawString(font, title.get(i), TX, y, FADED[side], false);
            y += 9;
        }
        y += 4;
        int rank = ChainRank.valueOf(e.rank()).ordinal();
        ChainDraw.medallion(g, side, rank, TX, y, 20);
        fit(g, Component.translatable(K + "level", e.level()), TX + 24, y + 2, TW - 24, INK[side]);
        fit(g, Component.translatable(org.papiricoh.supernaturalcraft.chain.ChainRank.titleKey(e.faction(), e.rank())), TX + 24, y + 11,
                TW - 24, FADED[side]);
        hit(TX, y, 20, 20, null, List.of(Component.translatable(org.papiricoh.supernaturalcraft.chain.ChainRank.titleKey(e.faction(), e.rank()))));
        y += 26;
        Component temper = e.player().isEmpty() ? ClientChain.temperamentName(e) : Component.translatable(K + "hunter_post");
        for (var line : font.split(temper, TW)) {
            g.drawString(font, line, TX, y, ACCENT[side], false);
            y += 9;
        }
        if (e.player().isEmpty())
            hit(TX, y - 9, TW, 9, null, List.of(ClientChain.temperamentName(e),
                    Component.translatable("chain.supernaturalcraft.temperament." + ClientChain.temperament(e).id() + ".desc")
                            .withStyle(ChatFormatting.GRAY)));
        g.pose().popPose();
    }

    /** The officer's regard for the reader as a bar from −100 to 100. @return where the body starts */
    private int drawRegard(GuiGraphics g, Entry e, int side, int mx, int my) {
        int y = ARCH_Y + 84;
        if (!e.player().isEmpty()) return y;
        g.pose().pushPose();
        g.pose().translate(0, 0, 90);
        Component label = Component.translatable(K + "regard"), word = regardWord(e);
        g.drawString(font, label, LX, y, FADED[side], false);
        // The word goes after the label when both fit on the line, else on its own line under it.
        if (font.width(label) + 6 + font.width(word) > LW) {
            y += 10;
            fit(g, word, LX, y, LW, regardColour(e.favor(), side));
        } else {
            g.drawString(font, word, LX + LW - font.width(word), y, regardColour(e.favor(), side), false);
        }
        int by = y + 11, x0 = LX, x1 = LX + LW, mid = (x0 + x1) / 2;
        g.fill(x0 - 1, by - 1, x1 + 1, by + 6, 0xFF000000 | (side == 1 ? 0x8A6420 : 0x0A0606));
        g.fill(x0, by, x1, by + 5, side == 1 ? 0xFFE5D8B9 : 0xFF1E1414);
        float v = Mth.clamp(e.favor() / 100f, -1, 1);
        int end = mid + Math.round(v * (x1 - mid));
        if (v < 0) g.fillGradient(end, by, mid, by + 5, 0xFFE8473A, 0xFF7A1012);
        else if (v > 0) g.fillGradient(mid, by, end, by + 5, 0xFFF4DC8A, 0xFFB48A34);
        for (int t : new int[]{Bonds.GRUDGE, Bonds.RIVAL, Bonds.LOYAL}) {
            int tx = mid + Math.round(t / 100f * (x1 - mid));
            g.fill(tx, by, tx + 1, by + 5, 0x60000000);
        }
        g.fill(mid, by - 1, mid + 1, by + 6, side == 1 ? 0xFF5A3C10 : 0xFFE8D8B0);
        g.fill(end - 1, by - 2, end + 1, by + 7, side == 1 ? BookStyle.INK : 0xFFFFF1CF);
        hit(x0, y, x1 - x0, 18, null, List.of(Component.translatable(K + "regard.value", e.favor()),
                Component.translatable(K + "regard.met", e.met()).withStyle(ChatFormatting.GRAY)));
        g.pose().popPose();
        return by + 12;
    }

    /** A line of text no wider than {@code w}: shrunk to fit if it must. */
    private void fit(GuiGraphics g, Component text, int x, int y, int w, int colour) {
        int tw = font.width(text);
        if (tw <= w) {
            g.drawString(font, text, x, y, colour, false);
            return;
        }
        float s = w / (float) tw;
        g.pose().pushPose();
        g.pose().translate(x, y + (1 - s) * 4, 0);
        g.pose().scale(s, s, 1);
        g.drawString(font, text, 0, 0, colour, false);
        g.pose().popPose();
    }

    private static Component regardWord(Entry e) {
        if (e.met() == 0 && e.favor() == 0) return Component.translatable(K + "regard.unmet");
        Bonds.Kind kind = Bonds.kind(e.favor(), false);
        if (kind != Bonds.Kind.NONE) return ClientChain.kindName(kind);
        return Component.translatable(K + (e.favor() >= 15 ? "regard.warm" : e.favor() <= -15 ? "regard.cold" : "regard.indifferent"));
    }

    private static int regardColour(int favor, int side) {
        if (favor <= Bonds.RIVAL) return side == 1 ? BookStyle.BLOOD : 0xFFF06A50;
        if (favor >= Bonds.LOYAL) return side == 1 ? BookStyle.GOLD : 0xFFF2D257;
        return INK[side];
    }

    static int nameColour(Entry e, int side) {
        if (!"ALIVE".equals(e.status())) return FADED[side];
        if (ChainRank.valueOf(e.rank()) == ChainRank.LORD) return side == 1 ? BookStyle.GOLD : 0xFFF2C960;
        return switch (side) {
            case 0 -> 0xFFF2836A;
            case 1 -> BookStyle.MANA;
            default -> 0xFFDCD4CC;
        };
    }

    // --- the left column -------------------------------------------------------------------------

    private List<Row> leftRows(Entry e, int side) {
        List<Row> rows = new ArrayList<>();
        int w = LW - 4;
        if (e.player().isEmpty()) {
            text(rows, Component.translatable("chain.supernaturalcraft.temperament." + ClientChain.temperament(e).id() + ".desc")
                    .withStyle(ChatFormatting.ITALIC), FADED[side], w, 0);
            gap(rows, 3);
            for (Component c : ClientChain.backstory(e)) {
                text(rows, c, INK[side], w, 0);
                gap(rows, 3);
            }
        } else {
            text(rows, Component.translatable(K + "hunter_post.desc", ClientChain.name(e)).withStyle(ChatFormatting.ITALIC), FADED[side], w, 0);
            gap(rows, 3);
        }
        rule(rows);
        Entry sup = ClientChain.entry(e.faction(), e.superior());
        if (sup != null && e.superior() != e.id()) {
            link(rows, null, Component.translatable(K + "answers_to", ClientChain.name(sup)), INK[side], w, 0, () -> open(sup),
                    List.of(Component.translatable(K + "open", ClientChain.name(sup))));
        }
        int under = 0;
        for (Entry o : ClientChain.officers) if (o.faction().equals(e.faction()) && o.superior() == e.id() && o.id() != e.id() && ChainSection.alive(o)) under++;
        if (under > 0) text(rows, Component.translatable(K + "subordinates", under), INK[side], w, 0);
        if (ChainSection.alive(e)) {
            boolean seated = ChainRank.valueOf(e.rank()).seated();
            text(rows, Component.translatable(K + (seated ? "seat" : "territory"),
                    ChainSection.region(ChainSection.OVERWORLD, e.homeX(), e.homeZ())), INK[side], w, 0);
        }
        if (e.seenDay() >= 0)
            text(rows, Component.translatable(K + "seen", e.seenDay(), ChainSection.region(e.seenDimension(), e.seenX(), e.seenZ())), FADED[side], w, 0);
        if (!ChainSection.alive(e)) text(rows, Component.translatable(K + "status." + e.status().toLowerCase(Locale.ROOT)), ACCENT[side], w, 0);
        if ("FALLEN".equals(e.branch())) text(rows, Component.translatable(K + "fallen"), ACCENT[side], w, 0);
        if (e.scars() > 0) text(rows, Component.translatable(K + "scars", e.scars()), INK[side], w, 0);
        if (e.kills() > 0) text(rows, Component.translatable(K + "kills", e.kills()), side == 1 ? BookStyle.BLOOD : 0xFFF08070, w, 0);
        return rows;
    }

    private void open(Entry other) {
        click(net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN);
        section.reveal(other);
        show(other);
    }

    // --- tabs ------------------------------------------------------------------------------------

    private static final int[] TAB_ICON = {0, 1, 2, 3};

    private int tabWidth(Tab t, boolean open) {
        return open ? 22 + font.width(tabName(t)) : 22;
    }

    private static Component tabName(Tab t) {
        return Component.translatable(K + "tab." + t.name().toLowerCase(Locale.ROOT));
    }

    private int tabX(Tab t) {
        int x = PX0 + 4;
        for (Tab o : Tab.values()) {
            if (o == t) return x;
            x += tabWidth(o, o == tab) + 2;
        }
        return x;
    }

    private void drawTabs(GuiGraphics g, int side, int mx, int my) {
        for (Tab t : Tab.values()) if (t != tab) drawTab(g, side, t, false, mx, my);
    }

    private void drawTab(GuiGraphics g, int side, Tab t, boolean open, int mx, int my) {
        int x = tabX(t), w = tabWidth(t, open), y = open ? TAB_Y : TAB_Y + 2;
        boolean over = inside(mx, my, x, TAB_Y, w, 15);
        ChainAtlas.tab(g, side, open, x, y, w);
        ChainAtlas.icon(TAB_ICON[t.ordinal()]).draw(g, x + 6, y + 3);
        if (open) g.drawString(font, tabName(t), x + 18, y + 4, BookStyle.INK, false);
        else if (over) g.fill(x + 3, y + 13, x + w - 3, y + 14, BookStyle.GOLD);
        hit(x, TAB_Y, w, 15, open ? null : () -> tab(t), open ? null : List.of(tabName(t)));
    }

    @Override
    boolean mouseScrolled(double mx, double my, double sy) {
        if (left.over(mx, my)) left.scroll(sy);
        else if (page.over(mx, my)) page.scroll(sy);
        return true;
    }

    // --- the page --------------------------------------------------------------------------------

    private int pageW() {
        return page.w - 4;
    }

    static Component dated(History h) {
        return Component.empty().append(Component.translatable(K + "day", h.day()).append(": ").withColor(BookStyle.GOLD))
                .append(ClientChain.chronicle(h).copy().withColor(BookStyle.INK));
    }

    private List<Row> chronicleRows(Entry e) {
        List<Row> rows = new ArrayList<>();
        List<History> mine = ClientChain.chronicleOf(e);
        if (mine.isEmpty()) {
            text(rows, Component.translatable(K + "chronicle.empty"), BookStyle.FADED, pageW(), 0);
            return rows;
        }
        for (int i = mine.size() - 1; i >= 0; i--) {
            text(rows, dated(mine.get(i)), BookStyle.INK, pageW(), 0);
            gap(rows, 4);
        }
        return rows;
    }

    private static final Bonds.Kind[] ORDER = {Bonds.Kind.GRUDGE, Bonds.Kind.RIVAL, Bonds.Kind.PACT, Bonds.Kind.LOYAL, Bonds.Kind.NONE};

    private List<Row> bondRows(Entry e) {
        List<Row> rows = new ArrayList<>();
        String actor = ClientChain.actor(e);
        List<Bond> of = ClientChain.bondsOf(actor), to = ClientChain.bondsTo(actor);
        if (of.isEmpty() && to.isEmpty()) {
            text(rows, Component.translatable(K + "bonds.none"), BookStyle.FADED, pageW(), 0);
            return rows;
        }
        if (!of.isEmpty()) bondGroup(rows, e, Component.translatable(K + "bonds.of", ClientChain.name(e)), of, true);
        if (!to.isEmpty()) {
            if (!of.isEmpty()) gap(rows, 4);
            bondGroup(rows, e, Component.translatable(K + "bonds.to", ClientChain.name(e)), to, false);
        }
        return rows;
    }

    private void bondGroup(List<Row> rows, Entry e, Component title, List<Bond> bonds, boolean outward) {
        text(rows, title, BookStyle.GOLD, pageW(), 0);
        rule(rows);
        for (Bonds.Kind kind : ORDER) {
            List<Bond> group = bonds.stream().filter(b -> ClientChain.kind(b) == kind).toList();
            if (group.isEmpty()) continue;
            Component label = kind == Bonds.Kind.NONE ? Component.translatable(K + "bond.mild") : ClientChain.kindName(kind);
            link(rows, ChainAtlas.bond(kind), label.copy().withStyle(ChatFormatting.BOLD), kindColour(kind), pageW(), 0, null, null);
            for (Bond b : group) {
                String other = outward ? b.to() : b.from();
                Entry o = ClientChain.entry(other);
                boolean me = other.equals(ClientChain.me());
                Component name = me ? Component.translatable(K + "you") : ClientChain.name(other);
                int colour = me ? BookStyle.INK : actorColour(other, e.faction());
                List<Component> tip = new ArrayList<>();
                tip.add(Component.translatable(K + "bond.regard", b.regard()));
                if (o != null) tip.add(ChainSection.title(o).copy().withStyle(ChatFormatting.GRAY));
                if (o != null && !me) tip.add(Component.translatable(K + "open", name).withStyle(ChatFormatting.DARK_GRAY));
                Entry target = o;
                link(rows, null, Component.literal("· ").append(name), colour, pageW(), 12, target != null && !me ? () -> open(target) : null, tip);
            }
        }
    }

    static int kindColour(Bonds.Kind k) {
        return switch (k) {
            case GRUDGE -> 0xFF6E0A10;
            case RIVAL -> BookStyle.BLOOD;
            case LOYAL, PACT -> BookStyle.GOLD;
            case NONE -> BookStyle.FADED;
        };
    }

    /** Names of the other side in that side's colour; a hunter in ink. */
    static int actorColour(String actor, String ownFaction) {
        Entry o = ClientChain.entry(actor);
        if (o == null || !o.player().isEmpty() && actor.startsWith("player:")) return BookStyle.INK;
        boolean fallen = "FALLEN".equals(o.branch());
        if (o.faction().equals(ownFaction) && !fallen) return BookStyle.INK;
        if (fallen) return 0xFF5A5450;
        return "angel".equals(o.faction()) ? BookStyle.MANA : "leviathan".equals(o.faction()) ? ChainSection.LEVIATHAN_INK : BookStyle.BLOOD;
    }

    private List<Row> duelRows(Entry e) {
        List<Row> rows = new ArrayList<>();
        String actor = ClientChain.actor(e), me = ClientChain.me();
        List<Challenge> open = ClientChain.challengesOf(actor);
        text(rows, Component.translatable(K + "duels.open"), BookStyle.GOLD, pageW(), 0);
        rule(rows);
        if (open.isEmpty()) text(rows, Component.translatable(K + "duels.none"), BookStyle.FADED, pageW(), 0);
        for (Challenge c : open) {
            Component line = c.challenger().equals(me) ? Component.translatable(K + "duels.you_vs", ClientChain.name(c.defender()))
                    : c.defender().equals(me) ? Component.translatable(K + "duels.vs_you", ClientChain.name(c.challenger()))
                    : Component.translatable(K + "duels.vs", ClientChain.name(c.challenger()), ClientChain.name(c.defender()));
            link(rows, ChainAtlas.icon(2), line, BookStyle.INK, pageW(), 0, null, null);
            Component how = Component.translatable(K + "duels.kind." + c.kind().toLowerCase(Locale.ROOT)).append(" · ")
                    .append(Component.translatable(K + (c.lethal() ? "duels.lethal" : "duels.yield"))).append(" · ")
                    .append(Component.translatable(K + "duels.state." + c.state().toLowerCase(Locale.ROOT)));
            text(rows, how, BookStyle.FADED, pageW(), 13);
            if (c.defender().equals(me) && "PENDING".equals(c.state()))
                text(rows, Component.translatable(K + "duels.answer", playTime(c.remaining())), BookStyle.BLOOD, pageW(), 13);
            gap(rows, 3);
        }
        List<History> past = ClientChain.chronicleOf(e).stream().filter(h -> DUEL_KEYS.contains(h.key())).toList();
        gap(rows, 4);
        text(rows, Component.translatable(K + "duels.past"), BookStyle.GOLD, pageW(), 0);
        rule(rows);
        if (past.isEmpty()) text(rows, Component.translatable(K + "duels.past.none"), BookStyle.FADED, pageW(), 0);
        for (int i = past.size() - 1; i >= 0; i--) {
            text(rows, dated(past.get(i)), BookStyle.INK, pageW(), 0);
            gap(rows, 3);
        }
        return rows;
    }

    /** Ticks of play as "about N minutes of play" / "about N hours of play". */
    static Component playTime(int ticks) {
        int minutes = Math.max(1, (int) Math.ceil(ticks / 1200.0));
        if (minutes < 90) return Component.translatable(K + "time.minutes", minutes);
        return Component.translatable(K + "time.hours", Math.round(minutes / 60f));
    }

    // --- actions ---------------------------------------------------------------------------------

    /** "demon" for the Pit's side (demons and the Fallen), "angel" for the Host's, or null for a human. */
    static @Nullable String sideOf(Entry e) {
        if ("leviathan".equals(e.faction())) return "leviathan";
        return "angel".equals(e.faction()) && !"FALLEN".equals(e.branch()) ? "angel" : "demon";
    }

    /** The reader's side: the post they hold, else their allegiance; null for a plain human. */
    static @Nullable String mySide() {
        Entry mine = ClientChain.mine();
        if (mine != null) return sideOf(mine);
        var p = Minecraft.getInstance().player;
        if (p == null) return null;
        Faction f = p.getData(AllAttachments.ALLEGIANCE).faction();
        return f == Faction.DEMON ? "demon" : f == Faction.ANGEL ? "angel" : null;
    }

    private static long today() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : level.getDayTime() / 24000;
    }

    private static String sentKey(Entry e, TalkOption o, int target) {
        return e.faction() + ":" + e.id() + ":" + o.id() + ":" + target;
    }

    private boolean sent(Entry e, TalkOption o, int target) {
        Long day = SENT.get(sentKey(e, o, target));
        return day != null && day == today();
    }

    private void send(Entry e, TalkOption o, int target, Component what) {
        ClientChain.send(e, o, target);
        SENT.put(sentKey(e, o, target), today());
        notice = Component.translatable(K + "actions.sent", what);
        ordering = false;
    }

    private List<Row> actionRows(Entry e, int side) {
        List<Row> rows = new ArrayList<>();
        int w = pageW();
        Entry mine = ClientChain.mine();
        if (mine != null && mine.faction().equals(e.faction()) && mine.id() == e.id()) {
            text(rows, Component.translatable(K + "actions.yours"), BookStyle.FADED, w, 0);
            return rows;
        }
        if (!e.player().isEmpty()) {
            text(rows, Component.translatable(K + "actions.hunter", ClientChain.name(e)), BookStyle.FADED, w, 0);
            return rows;
        }
        if (!ChainSection.alive(e)) {
            text(rows, Component.translatable(K + "actions.unreachable"), BookStyle.FADED, w, 0);
            return rows;
        }
        if (!org.papiricoh.supernaturalcraft.chain.ChainFaction.byId(e.faction()).talks()) {
            // SucroCorp answers no letters: only the rite reaches it.
            text(rows, Component.translatable(K + "actions.board"), BookStyle.FADED, w, 0);
            gap(rows, 5);
            rule(rows);
            link(rows, ChainAtlas.icon(2), Component.translatable(K + "actions.rite.title"), BookStyle.BLOOD, w, 0, null, null);
            text(rows, Component.translatable(K + "actions.rite.board", ClientChain.name(e)), BookStyle.INK, w, 0);
            return rows;
        }
        text(rows, Component.translatable(K + "actions.intro"), BookStyle.INK, w, 0);
        gap(rows, 4);
        String mySide = mySide();
        boolean same = mySide != null && mySide.equals(sideOf(e));
        int myRank = mine == null ? -1 : ChainRank.valueOf(mine.rank()).ordinal();
        int itsRank = ChainRank.valueOf(e.rank()).ordinal();
        boolean lord = mine != null && myRank == ChainRank.LORD.ordinal() && mine.faction().equals(e.faction());
        for (TalkOption o : TalkOption.values()) {
            if (!o.messageable()) continue;
            boolean valid = switch (o) {
                case PACT, AID -> same;
                case SPONSOR -> same && itsRank > myRank;
                case ORDER -> lord;
                default -> true;
            };
            if (!valid) continue;
            Component label = Component.translatable(o.key());
            List<Component> tip = List.of(Component.translatable(K + "actions.tip." + o.id()));
            if (o == TalkOption.ORDER) {
                button(rows, ordering ? label.copy().append(" ▾") : label, 0, () -> ordering = !ordering, tip);
                if (ordering) orderRows(rows, e, w);
            } else if (sent(e, o, -1)) {
                button(rows, label.copy().append(" ✓"), 0, null, List.of(Component.translatable(K + "actions.sent", label)));
            } else {
                button(rows, label, 0, () -> send(e, o, -1, label), tip);
            }
        }
        if (!same) {
            gap(rows, 2);
            text(rows, Component.translatable(K + (mySide == null ? "actions.human" : "actions.foe")), BookStyle.FADED, w, 0);
        }
        if (notice != null) {
            gap(rows, 3);
            text(rows, notice, BookStyle.GOLD, w, 0);
        }
        gap(rows, 5);
        rule(rows);
        link(rows, ChainAtlas.icon(2), Component.translatable(K + "actions.rite.title"), BookStyle.BLOOD, w, 0, null, null);
        text(rows, Component.translatable(K + "actions.rite", ClientChain.name(e), Component.translatable(K + "item.gauntlet"),
                Component.translatable(K + "item.seal")), BookStyle.INK, w, 0);
        return rows;
    }

    /**
     * A Lord's order: set it on one of its rivals (an officer of the Lord's own chain by id, one of the other side as
     * {@code ChainTalk.foreign}), or, in peace, declare war ({@code -2}).
     */
    private void orderRows(List<Row> rows, Entry e, int w) {
        Entry mine = ClientChain.mine();
        text(rows, Component.translatable(K + "actions.order.pick", ClientChain.name(e)), BookStyle.FADED, w, 8);
        int n = 0;
        for (Bond b : ClientChain.bondsOf(ClientChain.actor(e))) {
            Bonds.Kind kind = ClientChain.kind(b);
            if (kind != Bonds.Kind.RIVAL && kind != Bonds.Kind.GRUDGE) continue;
            Entry t = ClientChain.entry(b.to());
            if (t == null || !t.player().isEmpty() || !ChainSection.alive(t)) continue;
            int target = mine != null && t.faction().equals(mine.faction()) ? t.id()
                    : org.papiricoh.supernaturalcraft.chain.ChainTalk.foreign(org.papiricoh.supernaturalcraft.chain.ChainFaction.byId(t.faction()), t.id());
            Component name = ClientChain.name(t);
            Component label = Component.literal("⚔ ").append(name);
            List<Component> tip = List.of(Component.translatable(K + "actions.order.tip", ClientChain.name(e), name), ClientChain.kindName(kind));
            if (sent(e, TalkOption.ORDER, target)) button(rows, label.copy().append(" ✓"), 8, null, tip);
            else button(rows, label, 8, () -> send(e, TalkOption.ORDER, target, Component.translatable(K + "actions.order.sent", name)), tip);
            n++;
        }
        if (n == 0) text(rows, Component.translatable(K + "actions.order.none"), BookStyle.FADED, w, 8);
        if (ClientChain.war.until() < 0) {
            Component label = Component.translatable(K + "actions.order.war");
            List<Component> tip = List.of(Component.translatable(K + "actions.order.war.tip"));
            if (sent(e, TalkOption.ORDER, -2)) button(rows, label.copy().append(" ✓"), 8, null, tip);
            else button(rows, label, 8, () -> send(e, TalkOption.ORDER, -2, label), tip);
        }
    }

    /** For previews: the tab and, for actions, the order picker open. */
    void preview(Tab t, boolean order) {
        tab(t);
        ordering = order;
    }
}
