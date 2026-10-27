package org.papiricoh.supernaturalcraft.client.book.chain;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.papiricoh.supernaturalcraft.allegiance.Ranks;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.ChainRank;
import org.papiricoh.supernaturalcraft.chain.Diplomacy;
import org.papiricoh.supernaturalcraft.chain.GuildRules;
import org.papiricoh.supernaturalcraft.client.book.BookStyle;
import org.papiricoh.supernaturalcraft.client.book.HunterBookScreen;
import org.papiricoh.supernaturalcraft.client.chain.ClientChain;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Entry;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Guild;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.History;
import org.papiricoh.supernaturalcraft.network.GuildActionPayload.Action;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * A guild's record (v0.29), opened from its tree or from the reader's own guild plate: on the left its emblem in the guild's
 * arch, its name, founder and founding day, its standard (or how long it has been down), the empty throne and the claim to
 * it, the emblem picker (its Lord) and leaving; on the right a page with three tabs: its members by rank (hunters and hired
 * officers, how many of each rank may be of flesh, the Lord's and Commanders' buttons, and the invitation field), its stance
 * towards every other faction standing (offers both ways, and its Lord's answers), and its chronicle. The book only sends
 * what a member asks ({@link ClientChain#act}); the server judges every right and answers with a fresh sync.
 */
public final class GuildPanel extends ChainPanel {

    public enum Tab { MEMBERS, DIPLOMACY, CHRONICLE }

    private static final String G = K + "guild.";
    /** Left column, the arch and the text beside it; the page to the right. */
    private static final int LX = X0 + 12, LW = 152, ARCH_Y = Y0 + 12, TX = LX + 68, TW = LX + LW - TX;
    private static final int PX0 = LX + LW + 8, PX1 = X1 - 12, TAB_Y = Y0 + 10, PY0 = TAB_Y + 15, PY1 = Y1 - 12;
    /** The emblem picker's cells, six to a row. */
    private static final int CELL = 19, PER_ROW = 6;
    /** A hunter's name, as Mojang allows it. */
    private static final int NAME_MAX = 16;
    private static final int[] TAB_ICON = {1, 4, 0};

    private final String slot;
    private Tab tab = Tab.MEMBERS;
    /** The leave button asks once more; a dismissal too (the officer's id, or -1). */
    private boolean leaving;
    private int dismissing = -1;
    private String invite = "";
    private boolean typing;
    private int fieldX, fieldY, fieldW;
    private @Nullable Component notice;
    private final Column left = new Column(), page = new Column();

    GuildPanel(ChainSection section, HunterBookScreen book, String slot) {
        super(section, book);
        this.slot = slot;
    }

    String slot() {
        return slot;
    }

    @Override
    boolean stale() {
        return ClientChain.guild(slot) == null;
    }

    @Override
    int side() {
        return ChainAtlas.side(ChainFaction.byId(slot));
    }

    /** The reader's post in this guild, or null. */
    private @Nullable Entry me() {
        Entry me = ClientChain.mine();
        return me != null && me.faction().equals(slot) ? me : null;
    }

    private boolean leads() {
        return me() != null && ClientChain.leads();
    }

    // --- drawing ---------------------------------------------------------------------------------

    @Override
    void draw(GuiGraphics g, int mx, int my, float partial) {
        Guild guild = ClientChain.guild(slot);
        if (guild == null) return;
        int side = side();
        drawArch(g, guild, side);
        drawHeader(g, guild, side);
        int bodyY = ARCH_Y + 84;
        if (leads()) bodyY = drawEmblems(g, side, mx, my, bodyY);
        left.place(LX, bodyY, LW, PY1 - bodyY);
        left.draw(g, leftRows(guild, side), mx, my, side, false);
        for (Tab t : Tab.values()) if (t != tab) drawTab(g, side, t, false, mx, my);
        ChainAtlas.page(g, side, PX0, PY0, PX1 - PX0, PY1 - PY0 + 2);
        drawTab(g, side, tab, true, mx, my);
        int top = PY0 + 7;
        if (tab == Tab.MEMBERS && me() != null && ClientChain.mayInvite()) top = drawInvite(g, side, mx, my, top);
        page.place(PX0 + 8, top, PX1 - PX0 - 12, PY1 - top - 3);
        List<Row> rows = switch (tab) {
            case MEMBERS -> memberRows(side);
            case DIPLOMACY -> diplomacyRows();
            case CHRONICLE -> chronicleRows();
        };
        page.draw(g, rows, mx, my, side, true);
    }

    /** The guild's arch with its emblem lit in it. */
    private void drawArch(GuiGraphics g, Guild guild, int side) {
        int ax = LX, ay = ARCH_Y;
        ChainAtlas.draw(g, side, ChainAtlas.portraitBack(side), ax, ay);
        ChainAtlas.emblem(g, guild.emblem(), ax + 16, ay + 31, 32, 1f);
        g.pose().pushPose();
        g.pose().translate(0, 0, 90);
        ChainAtlas.draw(g, side, ChainAtlas.portraitFrame(side), ax, ay);
        g.pose().popPose();
        hit(ax, ay, 64, 80, null, List.of(Component.literal(guild.name()).withStyle(ChatFormatting.BOLD),
                Component.translatable(G + "emblem.tip").withStyle(ChatFormatting.GRAY)));
    }

    private void drawHeader(GuiGraphics g, Guild guild, int side) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 90);
        Component name = Component.literal(guild.name());
        int nw = font.width(name);
        float s = Math.min(1.25f, TW / (float) Math.max(1, nw));
        g.pose().pushPose();
        g.pose().translate(TX, ARCH_Y + 4, 0);
        g.pose().scale(s, s, 1);
        g.drawString(font, name, 0, 0, accent(side), false);
        g.pose().popPose();
        int y = ARCH_Y + 6 + Math.round(9 * s) + 2;
        for (Component c : List.of(Component.translatable(G + "founder", guild.founderName()),
                Component.translatable(G + "founded", guild.founded()))) {
            for (var line : font.split(c, TW)) {
                g.drawString(font, line, TX, y, faded(side), false);
                y += 9;
            }
        }
        y += 4;
        int hunters = 0, hired = 0;
        for (Entry e : ClientChain.members(slot)) {
            if (e.player().isEmpty()) hired++;
            else hunters++;
        }
        for (var line : font.split(Component.translatable(G + "count", hunters, hired), TW)) {
            g.drawString(font, line, TX, y, ink(side), false);
            y += 9;
        }
        Entry me = me();
        if (me != null) {
            y += 2;
            Component post = Component.translatable(G + "your_post",
                    Component.translatable(ChainRank.titleKey(slot, me.rank())));
            for (var line : font.split(post, TW)) {
                g.drawString(font, line, TX, y, accent(side), false);
                y += 9;
            }
        }
        g.pose().popPose();
    }

    /** The Lord's emblem picker: the twelve, the guild's own ringed. @return where the column starts under it */
    private int drawEmblems(GuiGraphics g, int side, int mx, int my, int y) {
        g.drawString(font, Component.translatable(G + "emblem"), LX, y, faded(side), false);
        y += 10;
        int current = ClientChain.emblem(slot);
        for (int i = 0; i < GuildRules.EMBLEMS; i++) {
            int x = LX + (i % PER_ROW) * CELL, cy = y + (i / PER_ROW) * CELL;
            boolean over = inside(mx, my, x, cy, CELL - 1, CELL - 1), on = i == current;
            if (on || over) g.fill(x, cy, x + CELL - 1, cy + CELL - 1, on ? 0x40FFFFFF : 0x22FFFFFF);
            if (on) {
                int a = accent(side);
                g.fill(x, cy, x + CELL - 1, cy + 1, a);
                g.fill(x, cy + CELL - 2, x + CELL - 1, cy + CELL - 1, a);
                g.fill(x, cy, x + 1, cy + CELL - 1, a);
                g.fill(x + CELL - 2, cy, x + CELL - 1, cy + CELL - 1, a);
            }
            ChainAtlas.emblem(g, i, x + 1, cy + 1, 16, on || over ? 1f : 0.55f);
            int pick = i;
            hit(x, cy, CELL - 1, CELL - 1, on ? null : () -> {
                ClientChain.act(Action.EMBLEM, slot, "", pick);
                notice = Component.translatable(G + "emblem.sent");
            }, List.of(Component.translatable(G + (on ? "emblem.current" : "emblem.pick"))));
        }
        return y + 2 * CELL + 4;
    }

    // --- the left column -------------------------------------------------------------------------

    private List<Row> leftRows(Guild guild, int side) {
        List<Row> rows = new ArrayList<>();
        int w = LW - 4;
        long today = today();
        rule(rows);
        if (guild.standard()) {
            text(rows, Component.translatable(G + "standard", guild.x(), guild.y(), guild.z()), ink(side), w, 0);
            text(rows, ChainSection.region(ChainSection.OVERWORLD, guild.x(), guild.z()), faded(side), w, 0);
        } else {
            int warn = ChainAtlas.light(side) ? BookStyle.BLOOD : 0xFFF06A50;
            text(rows, Component.translatable(G + "standard.down", guild.lostDay()), warn, w, 0);
            long shared = guild.lostDay() + GuildRules.HOMELESS_DAYS;
            text(rows, Component.translatable(G + (shared > today ? "homeless" : "homeless.now"), shared), warn, w, 0);
        }
        var court = ClientChain.court(slot);
        if (court != null && court.vacantUntil() >= 0) {
            gap(rows, 3);
            text(rows, Component.translatable(G + "interregnum", court.vacantUntil()), accent(side), w, 0);
            if (me() != null) {
                Component claim = Component.translatable(G + "proclaim");
                button(rows, claim, 0, () -> {
                    ClientChain.act(Action.PROCLAIM, slot, "", 0);
                    notice = Component.translatable(G + "proclaim.sent");
                }, List.of(Component.translatable(G + "proclaim.tip")));
            }
        }
        Entry me = me();
        if (me != null) {
            gap(rows, 4);
            Component label = Component.translatable(G + (leaving ? "leave.confirm" : "leave"));
            List<Component> tip = List.of(Component.translatable(G + ("LORD".equals(me.rank()) ? "leave.tip.lord" : "leave.tip")));
            button(rows, label, 0, () -> {
                if (!leaving) {
                    leaving = true;
                    return;
                }
                leaving = false;
                ClientChain.act(Action.LEAVE, slot, "", 0);
                notice = Component.translatable(G + "leave.sent");
            }, tip);
        }
        if (notice != null) {
            gap(rows, 3);
            text(rows, notice, accent(side), w, 0);
        }
        return rows;
    }

    // --- tabs ------------------------------------------------------------------------------------

    private static Component tabName(Tab t) {
        return Component.translatable(G + "tab." + t.name().toLowerCase(Locale.ROOT));
    }

    private int tabWidth(Tab t, boolean open) {
        return open ? 22 + font.width(tabName(t)) : 22;
    }

    private int tabX(Tab t) {
        int x = PX0 + 4;
        for (Tab o : Tab.values()) {
            if (o == t) return x;
            x += tabWidth(o, o == tab) + 2;
        }
        return x;
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

    void tab(Tab t) {
        tab = t;
        page.scroll = 0;
        dismissing = -1;
    }

    // --- members ---------------------------------------------------------------------------------

    /** The invitation field over the members (a Lord's or a Commander's). @return where the list starts under it */
    private int drawInvite(GuiGraphics g, int side, int mx, int my, int y) {
        int x = PX0 + 8, w = PX1 - PX0 - 16;
        g.drawString(font, Component.translatable(G + "invite"), x, y, BookStyle.FADED, false);
        y += 10;
        Component send = Component.translatable(G + "invite.send");
        int bw = font.width(send) + 10;
        fieldX = x;
        fieldY = y;
        fieldW = w - bw - 3;
        boolean overField = inside(mx, my, fieldX, fieldY, fieldW, 14);
        g.fill(fieldX, fieldY, fieldX + fieldW, fieldY + 14, typing ? 0xFF7A6448 : overField ? 0xFF9A8468 : 0xFFB8A488);
        g.fill(fieldX + 1, fieldY + 1, fieldX + fieldW - 1, fieldY + 13, 0xFFF2E8D2);
        if (invite.isEmpty() && !typing) {
            g.drawString(font, Component.translatable(G + "invite.hint"), fieldX + 4, fieldY + 3, 0xFFA8987E, false);
        } else {
            String shown = font.plainSubstrByWidth(invite, fieldW - 10, true);
            g.drawString(font, shown, fieldX + 4, fieldY + 3, BookStyle.INK, false);
            if (typing && Util.getMillis() / 500 % 2 == 0) {
                int cx = fieldX + 4 + font.width(shown);
                g.fill(cx, fieldY + 2, cx + 1, fieldY + 12, BookStyle.INK);
            }
        }
        hit(fieldX, fieldY, fieldW, 14, null, List.of(Component.translatable(G + "invite.tip")));
        int bx = fieldX + fieldW + 3;
        boolean ready = GuildRules.NAME_MIN <= invite.strip().length();
        boolean hot = ready && inside(mx, my, bx, fieldY, bw, 14);
        ChainAtlas.button(g, side, !ready ? 2 : hot ? 1 : 0, bx, fieldY, bw);
        g.drawString(font, send, bx + 5, fieldY + 3, ready ? label(side) : 0xFF8A7E70, false);
        hit(bx, fieldY, bw, 14, ready ? this::sendInvite : null, List.of(Component.translatable(G + "invite.tip")));
        return fieldY + 20;
    }

    private void sendInvite() {
        String name = invite.strip();
        if (name.length() < 3) return;
        ClientChain.act(Action.INVITE, slot, name, 0);
        notice = Component.translatable(G + "invite.sent", name);
        invite = "";
        stopTyping();
    }

    private void stopTyping() {
        if (typing) section.takeKeyboard(false);
        typing = false;
    }

    private List<Row> memberRows(int side) {
        List<Row> rows = new ArrayList<>();
        int w = page.w - 4;
        List<Entry> members = new ArrayList<>(ClientChain.members(slot));
        members.sort(Comparator.comparing((Entry e) -> e.player().isEmpty()).thenComparingInt(Entry::id));
        int[] count = new int[ChainRank.values().length], hired = new int[ChainRank.values().length];
        for (Entry e : members) {
            int r = ChainRank.valueOf(e.rank()).ordinal();
            count[r]++;
            if (e.player().isEmpty()) hired[r]++;
        }
        ChainRank[] ranks = ChainRank.values();
        for (int r = ranks.length - 1; r >= 0; r--) {
            ChainRank rank = ranks[r];
            Component title = Component.literal(Ranks.roman(r + 1) + " ").append(Component.translatable(ChainRank.titleKey(slot, rank.name())));
            Component head = rank == ChainRank.LORD ? title
                    : Component.translatable(G + "rank", title, count[r], rank.cap(), hired[r], GuildRules.npcCap(rank));
            List<Component> tip = rank == ChainRank.LORD ? List.of(Component.translatable(G + "rank.lord.tip"))
                    : List.of(Component.translatable(G + "rank.tip", GuildRules.npcCap(rank)));
            link(rows, null, head, BookStyle.GOLD, w, 0, null, tip);
            if (count[r] == 0) text(rows, Component.translatable(G + "rank.empty"), BookStyle.FADED, w, 8);
            for (Entry e : members) {
                if (ChainRank.valueOf(e.rank()) != rank) continue;
                memberRow(rows, e, rank, count, hired, w);
            }
            gap(rows, 3);
        }
        return rows;
    }

    /** One member: name (its record on a click), what it is, and the buttons the reader's rank allows. */
    private void memberRow(List<Row> rows, Entry e, ChainRank rank, int[] count, int[] hired, int w) {
        boolean hunter = !e.player().isEmpty(), mine = e.equals(ClientChain.mine());
        Component name = ClientChain.name(e).copy();
        Component shown = mine ? Component.translatable(G + "you", name) : name;
        int colour = hunter ? BookStyle.INK : 0xFF000000 | ClientChain.guildInk(slot);
        List<Component> tip = new ArrayList<>();
        tip.add(name.copy().withStyle(ChatFormatting.BOLD));
        tip.add(ChainSection.title(e).copy().withStyle(ChatFormatting.GRAY));
        tip.add(hunter ? Component.translatable(G + "hunter").withStyle(ChatFormatting.GRAY)
                : Component.translatable(G + "hired", Component.translatable("entity." + e.entityType().replace(':', '.')))
                .withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable(K + "open", name).withStyle(ChatFormatting.DARK_GRAY));
        List<Mini> minis = new ArrayList<>();
        int r = rank.ordinal();
        if (r + 1 < ChainRank.values().length && ClientChain.mayRank(e, ChainRank.values()[r + 1].name())) {
            ChainRank up = ChainRank.values()[r + 1];
            boolean full = count[up.ordinal()] >= up.cap() || !hunter && hired[up.ordinal()] >= GuildRules.npcCap(up);
            minis.add(new Mini(Component.literal("▲"), full ? null : () -> rank(Action.PROMOTE, e, up),
                    List.of(Component.translatable(G + (full ? "promote.full" : "promote"), Component.translatable(ChainRank.titleKey(slot, up.name()))))));
        }
        if (r > 0 && ClientChain.mayRank(e, ChainRank.values()[r - 1].name())) {
            ChainRank down = ChainRank.values()[r - 1];
            boolean full = count[down.ordinal()] >= down.cap() || !hunter && hired[down.ordinal()] >= GuildRules.npcCap(down);
            minis.add(new Mini(Component.literal("▼"), full ? null : () -> rank(Action.DEMOTE, e, down),
                    List.of(Component.translatable(G + (full ? "demote.full" : "demote"), Component.translatable(ChainRank.titleKey(slot, down.name()))))));
        }
        if (ClientChain.mayRank(e, rank.name())) {
            boolean asking = dismissing == e.id();
            minis.add(new Mini(Component.literal(asking ? "✕?" : "✕"), () -> {
                if (dismissing != e.id()) {
                    dismissing = e.id();
                    return;
                }
                dismissing = -1;
                ClientChain.act(Action.DISMISS, slot, "", e.id());
                notice = Component.translatable(G + "dismiss.sent", name);
            }, List.of(Component.translatable(G + (asking ? "dismiss.confirm" : hunter ? "dismiss.hunter" : "dismiss.hired"), name))));
        }
        line(rows, Component.literal("· ").append(shown), colour, w, 8, () -> openRecord(e), tip, minis);
    }

    private void rank(Action action, Entry e, ChainRank to) {
        ClientChain.act(action, slot, "", e.id());
        notice = Component.translatable(G + (action == Action.PROMOTE ? "promote.sent" : "demote.sent"), ClientChain.name(e),
                Component.translatable(ChainRank.titleKey(slot, to.name())));
    }

    private void openRecord(Entry e) {
        click(net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN);
        section.openOfficer(e);
    }

    // --- diplomacy -------------------------------------------------------------------------------

    /** A faction's name as ink on this page: a guild's in its own colour (darkened to read), the world's in theirs. */
    static Component inkName(String faction) {
        if (ClientChain.isGuild(faction)) {
            Guild g = ClientChain.guild(faction);
            return Component.literal(g == null ? "?" : g.name()).withColor(ClientChain.guildInk(faction));
        }
        int col = switch (ChainFaction.byId(faction)) {
            case ANGEL -> BookStyle.MANA;
            case LEVIATHAN -> ChainSection.LEVIATHAN_INK;
            default -> BookStyle.BLOOD;
        };
        return Component.translatable(K + faction).withColor(col);
    }

    private static Component stanceWord(Diplomacy.Stance s) {
        int col = switch (s) {
            case HOSTILE -> BookStyle.BLOOD;
            case NEUTRAL -> BookStyle.FADED;
            case ALLIED -> BookStyle.GOLD;
        };
        return Component.translatable(G + "stance." + s.name().toLowerCase(Locale.ROOT)).withColor(col);
    }

    private List<Row> diplomacyRows() {
        List<Row> rows = new ArrayList<>();
        int w = page.w - 4;
        boolean lead = leads();
        text(rows, Component.translatable(G + (lead ? "diplomacy.lead" : "diplomacy.intro"), inkName(slot)), BookStyle.FADED, w, 0);
        gap(rows, 4);
        ChainFaction self = ChainFaction.byId(slot);
        int shown = 0;
        for (ChainFaction f : ClientChain.standing()) {
            if (f == self) continue;
            shown++;
            String id = f.id();
            Diplomacy.Stance stance = ClientChain.stance(slot, id);
            rule(rows);
            link(rows, null, Component.translatable(G + "stance.line", inkName(id), stanceWord(stance)), BookStyle.INK, w, 0, null, null);
            if (!Diplomacy.open(self, f)) {
                text(rows, Component.translatable(G + "stance.fixed." + (f == ChainFaction.LEVIATHAN ? "board" : "world")), BookStyle.FADED, w, 8);
                continue;
            }
            Diplomacy.Stance theirs = ClientChain.proposal(id, slot), ours = ClientChain.proposal(slot, id);
            if (theirs != null) {
                List<Mini> answer = new ArrayList<>();
                if (lead) {
                    answer.add(new Mini(Component.translatable(G + "accept"), () -> {
                        ClientChain.act(Action.ACCEPT_STANCE, slot, id, 0);
                        notice = Component.translatable(G + "accept.sent", inkName(id));
                    }, List.of(Component.translatable(G + "accept.tip", stanceWord(theirs)))));
                    answer.add(new Mini(Component.translatable(G + "refuse"), () -> {
                        ClientChain.act(Action.REFUSE_STANCE, slot, id, 0);
                        notice = Component.translatable(G + "refuse.sent", inkName(id));
                    }, null));
                }
                text(rows, Component.translatable(G + "offer.in", stanceWord(theirs)), BookStyle.INK, w, 8);
                if (!answer.isEmpty()) line(rows, Component.empty(), BookStyle.INK, w, 8, null, null, answer);
            }
            if (ours != null) text(rows, Component.translatable(G + "offer.out", stanceWord(ours)), BookStyle.FADED, w, 8);
            if (!lead) continue;
            List<Mini> offers = new ArrayList<>();
            for (Diplomacy.Stance s : Diplomacy.Stance.values()) {
                if (s == stance) continue;
                Component word = Component.translatable(G + "stance." + s.name().toLowerCase(Locale.ROOT));
                if (s == Diplomacy.Stance.HOSTILE) {
                    offers.add(new Mini(word, () -> {
                        ClientChain.act(Action.DECLARE, slot, id, 0);
                        notice = Component.translatable(G + "declare.sent", inkName(id));
                    }, List.of(Component.translatable(G + "declare.tip", inkName(id)))));
                } else {
                    boolean offered = s == ours;
                    List<Component> tip = new ArrayList<>();
                    tip.add(Component.translatable(G + (offered ? "offer.already" : "offer.tip"), word, inkName(id)));
                    if (!f.guild()) tip.add(Component.translatable(G + "offer.lord").withStyle(ChatFormatting.GRAY));
                    offers.add(new Mini(word, offered ? null : () -> {
                        ClientChain.act(Action.PROPOSE, slot, id, s.ordinal());
                        notice = Component.translatable(G + "offer.sent", word, inkName(id));
                    }, tip));
                }
            }
            line(rows, Component.translatable(G + "offer"), BookStyle.FADED, w, 8, null, null, offers);
        }
        if (shown == 0) text(rows, Component.translatable(G + "diplomacy.none"), BookStyle.FADED, w, 0);
        if (notice != null) {
            gap(rows, 4);
            text(rows, notice, BookStyle.GOLD, w, 0);
        }
        return rows;
    }

    // --- chronicle -------------------------------------------------------------------------------

    private List<Row> chronicleRows() {
        List<Row> rows = new ArrayList<>();
        int w = page.w - 4, n = 0;
        for (int i = ClientChain.history.size() - 1; i >= 0 && n < 40; i--) {
            History h = ClientChain.history.get(i);
            if (!h.faction().equals(slot) && !h.otherFaction().equals(slot)) continue;
            text(rows, OfficerModal.dated(h), BookStyle.INK, w, 0);
            gap(rows, 4);
            n++;
        }
        if (n == 0) text(rows, Component.translatable(G + "chronicle.empty"), BookStyle.FADED, w, 0);
        return rows;
    }

    // --- input -----------------------------------------------------------------------------------

    @Override
    boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && tab == Tab.MEMBERS && fieldW > 0 && me() != null && ClientChain.mayInvite()
                && inside(mx, my, fieldX, fieldY, fieldW, 14)) {
            typing = true;
            section.takeKeyboard(true);
            return true;
        }
        stopTyping();
        // A second step asked for is let go by any other click.
        boolean wasLeaving = leaving;
        int wasDismissing = dismissing;
        boolean out = super.mouseClicked(mx, my, button);
        if (wasLeaving && leaving) leaving = false;
        if (wasDismissing >= 0 && dismissing == wasDismissing) dismissing = -1;
        return out;
    }

    @Override
    boolean charTyped(char c) {
        if (!typing) return false;
        if ((Character.isLetterOrDigit(c) || c == '_') && c < 128 && invite.length() < NAME_MAX) invite += c;
        return true;
    }

    @Override
    boolean keyPressed(int key) {
        if (typing) {
            switch (key) {
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    if (!invite.isEmpty()) invite = invite.substring(0, invite.length() - 1);
                }
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> sendInvite();
                case GLFW.GLFW_KEY_ESCAPE -> stopTyping();
                default -> {
                }
            }
            return true;
        }
        return super.keyPressed(key);
    }

    @Override
    boolean mouseScrolled(double mx, double my, double sy) {
        if (left.over(mx, my)) left.scroll(sy);
        else if (page.over(mx, my)) page.scroll(sy);
        return true;
    }

    private static long today() {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        return level == null ? 0 : level.getDayTime() / 24000;
    }

    /** For previews: the tab open, and a name half written in the invitation field. */
    public void preview(Tab t, String typed) {
        tab(t);
        invite = typed;
    }
}
