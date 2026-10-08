package org.papiricoh.supernaturalcraft.client.book.home;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.BowlSpells;
import org.papiricoh.supernaturalcraft.client.ClientArcana;
import org.papiricoh.supernaturalcraft.client.ClientHunterLog;
import org.papiricoh.supernaturalcraft.client.allegiance.AllegianceGui;
import org.papiricoh.supernaturalcraft.client.book.BookAtlas;
import org.papiricoh.supernaturalcraft.client.book.BookData;
import org.papiricoh.supernaturalcraft.client.book.BookSection;
import org.papiricoh.supernaturalcraft.client.book.BookStyle;
import org.papiricoh.supernaturalcraft.client.book.HunterBookScreen.Tab;
import org.papiricoh.supernaturalcraft.client.book.journal.Ink;
import org.papiricoh.supernaturalcraft.client.book.journal.JournalPages;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.crossroads.DealTerms;
import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.journal.JournalEntry;
import org.papiricoh.supernaturalcraft.journal.RoadmapNode;
import org.papiricoh.supernaturalcraft.journal.RoadmapState;
import org.papiricoh.supernaturalcraft.network.HunterLogSyncPayload;
import org.papiricoh.supernaturalcraft.registry.SNRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The dashboard. Left page: the hunter (model, mana, sanity, marks, boons, the mod's effects on
 * them) and the deal they owe. Right page: the next step on the road, what they have collected,
 * shortcuts to the other sections and their bookmarks. Everything is drawn by hand; clicks are
 * hit-tested against the same rectangles.
 */
public class HomeSection extends BookSection {

    private static final int L = BookStyle.LEFT_X, R = BookStyle.RIGHT_X, PW = BookStyle.PAGE_W, TOP = BookStyle.PAGE_Y;
    private static final String KEY = "screen.supernaturalcraft.book.home.";
    private static final int DAY = 24000;

    // Left page.
    private static final int MODEL_X = L, MODEL_Y = 38, MODEL_W = 58, MODEL_H = 80;
    private static final int STAT_X = L + 64, STAT_W = PW - 64;
    private static final int EFFECTS_Y = 124, EFFECT_ROW = 18, MAX_EFFECTS = 3, DEAL_Y = 166, FACTION_H = 22;
    // Right page.
    private static final int NEXT_Y = 38, NEXT_H = 40;
    private static final int COLLECTION_Y = 104, COLLECTION_ROW = 11;
    private static final int CARDS_Y = 163, CARD_W = 58, CARD_H = 32, CARD_GAP = 3;
    private static final int MARKS_Y = 202, MARKS_LIST_Y = 214, MARKS_ROW = 11, MARKS_BOTTOM = 248;

    /** One line of the collection: a count and where clicking it leads. */
    private record Tally(Component label, int have, int of, Runnable go) {
    }

    private double marksScroll;
    /** Where the faction row was drawn this frame (clicks are tested against it). */
    private int factionY = -1;
    /** {@code SN_PREVIEW=book}: the faction row's tooltip as if hovered. */
    private static boolean forcedFactionHover;
    private static final ResourceLocation ALLEGIANCE_ROAD = SupernaturalCraft.asResource("heaven_hell_free_will");

    // --- drawing -------------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        if (mc.player == null) return;
        renderHunter(g, mx, my);
        renderNext(g, mx, my);
        renderCollection(g, mx, my);
        renderCards(g, mx, my);
        renderBookmarks(g, mx, my);
    }

    private void renderHunter(GuiGraphics g, int mx, int my) {
        Player player = mc.player;
        Ink.heading(g, font, Component.translatable(KEY + "hunter"), L, TOP + 2, PW, BookStyle.INK);
        drawPlayer(g, player, mx, my);

        int y = MODEL_Y + 2;
        Ink.left(g, font, player.getName(), STAT_X, y, STAT_W, BookStyle.INK);
        y += 13;
        y = stat(g, Component.translatable(KEY + "mana"), Math.round(ClientArcana.mana()), Math.round(ClientArcana.maxMana()),
                ClientArcana.mana() / Math.max(1, ClientArcana.maxMana()), BookAtlas.BAR_MANA, STAT_X, y);
        float sanity = ClientArcana.sanity();
        y = stat(g, Component.translatable(KEY + "sanity"), Math.round(sanity), 100, sanity / 100f,
                sanity < 30 ? BookAtlas.BAR_DANGER : BookAtlas.BAR_SANITY, STAT_X, y);

        // Marks of grace and of the void, as little inked labels.
        int bx = STAT_X;
        if (ClientArcana.hasGrace()) bx = badge(g, Component.translatable(KEY + "grace"), bx, y, BookStyle.GOLD, mx, my, KEY + "grace.desc");
        if (ClientArcana.voidMark()) bx = badge(g, Component.translatable(KEY + "void_mark"), bx, y, 0xFF4B2A6A, mx, my, KEY + "void_mark.desc");
        if (bx != STAT_X) y += 15;
        List<Component> boons = new ArrayList<>();
        if (ClientHunterLog.bonusHearts() > 0) boons.add(Component.translatable(KEY + "boon.hearts", ClientHunterLog.bonusHearts()));
        if (ClientHunterLog.bonusMana() > 0) boons.add(Component.translatable(KEY + "boon.mana", ClientHunterLog.bonusMana()));
        if (!boons.isEmpty()) {
            Component line = boons.size() == 1 ? boons.getFirst() : Component.translatable(KEY + "boon.both", boons.get(0), boons.get(1));
            Ink.left(g, font, line, STAT_X, y, STAT_W, BookStyle.FADED);
            if (Ink.over(mx, my, STAT_X, y - 1, STAT_W, 10)) book.tooltip(List.of(Component.translatable(KEY + "boon.desc")));
            y += 10;
        }

        // The side they are on (v0.13), across the page under the portrait; the effects make room for it.
        factionY = Math.max(MODEL_Y + MODEL_H + 3, y + 2);
        renderFaction(g, factionY, mx, my);
        int effectsY = Math.max(EFFECTS_Y, factionY + FACTION_H + 2);
        int maxEffects = effectsY > EFFECTS_Y ? MAX_EFFECTS - 1 : MAX_EFFECTS;

        // The mod's effects on the hunter: icon, name, time left.
        List<MobEffectInstance> effects = player.getActiveEffects().stream()
                .filter(e -> e.getEffect().unwrapKey().map(k -> k.location().getNamespace().equals(SupernaturalCraft.MODID)).orElse(false))
                .sorted((a, b) -> Integer.compare(b.getDuration(), a.getDuration())).toList();
        int ey = effectsY;
        float tickRate = mc.level != null ? mc.level.tickRateManager().tickrate() : 20;
        // Three rows (two under a faction row), or one fewer and "...and N more".
        int rows = effects.size() > maxEffects ? maxEffects - 1 : effects.size();
        for (int i = 0; i < rows; i++) {
            MobEffectInstance e = effects.get(i);
            g.blit(L + 1, ey, 0, 18, 18, mc.getMobEffectTextures().get(e.getEffect()));
            Component name = e.getEffect().value().getDisplayName().copy();
            if (e.getAmplifier() > 0) name = Component.empty().append(name).append(" ").append(Component.translatable("enchantment.level." + (e.getAmplifier() + 1)));
            Component time = MobEffectUtil.formatDuration(e, 1f, tickRate);
            int tw = font.width(time);
            Ink.left(g, font, name, L + 24, ey + 6, PW - 24 - tw - 8, BookStyle.INK);
            Ink.right(g, font, time, L + PW - 2, ey + 6, BookStyle.FADED);
            ey += EFFECT_ROW;
        }
        if (rows < effects.size()) {
            Ink.left(g, font, Component.translatable(KEY + "more_effects", effects.size() - rows), L + 24, ey + 4, PW - 24, BookStyle.FADED);
            ey += EFFECT_ROW;
        }
        if (effects.isEmpty()) {
            Ink.centred(g, font, Component.translatable(KEY + "no_effects").withStyle(ChatFormatting.ITALIC), L + PW / 2, ey + 4, PW, BookStyle.FADED);
            ey += 14;
        }

        // The deal sits low on the page, but never over the effects (its block is at most 68 high).
        renderDeal(g, Mth.clamp(ey + 6, DEAL_Y, TOP + BookStyle.PAGE_H - 68), mx, my);
    }

    private void drawPlayer(GuiGraphics g, Player player, int mx, int my) {
        float cx = MODEL_X + MODEL_W / 2f, cy = MODEL_Y + MODEL_H / 2f;
        // Turns a little towards the mouse, as in the inventory (but drawn in book space, unclipped).
        float ax = (float) Math.atan((cx - mx) / 40f), ay = (float) Math.atan((cy - 16 - my) / 40f);
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf camera = new Quaternionf().rotateX(ay * 20 * Mth.DEG_TO_RAD);
        pose.mul(camera);
        float bodyRot = player.yBodyRot, yRot = player.getYRot(), xRot = player.getXRot(), headO = player.yHeadRotO, head = player.yHeadRot;
        // A portrait, not a snapshot: never mid-flinch or falling dead.
        int hurt = player.hurtTime, death = player.deathTime;
        player.hurtTime = 0;
        player.deathTime = 0;
        player.yBodyRot = 180 + ax * 20;
        player.setYRot(180 + ax * 40);
        player.setXRot(-ay * 20);
        player.yHeadRot = player.getYRot();
        player.yHeadRotO = player.getYRot();
        float size = 36 / player.getScale();
        // A shadow on the page to stand on.
        int feet = Math.round(cy + player.getBbHeight() / 2 * 36);
        g.fill((int) cx - 18, feet, (int) cx + 18, feet + 1, 0x1E2A1A0A);
        g.fill((int) cx - 12, feet + 1, (int) cx + 12, feet + 2, 0x162A1A0A);
        InventoryScreen.renderEntityInInventory(g, cx, cy, size, new Vector3f(0, player.getBbHeight() / 2, 0), pose, camera, player);
        player.yBodyRot = bodyRot;
        player.setYRot(yRot);
        player.setXRot(xRot);
        player.yHeadRotO = headO;
        player.yHeadRot = head;
        player.hurtTime = hurt;
        player.deathTime = death;
    }

    /** A labelled bar: "Mana        72 / 100" over the bar. Returns the y under it. */
    private int stat(GuiGraphics g, Component label, int value, int max, float frac, BookAtlas.Sprite fill, int x, int y) {
        return stat(g, label, BookStyle.FADED, value, max, frac, fill, 0xFFFFFF, x, y, STAT_W);
    }

    /** As above, {@code w} wide, the fill tinted {@code tint}. */
    private int stat(GuiGraphics g, Component label, int labelColour, int value, int max, float frac, BookAtlas.Sprite fill, int tint,
                     int x, int y, int w) {
        Ink.left(g, font, label, x, y, w - 4 - font.width(value + " / " + max), labelColour);
        Ink.right(g, font, Component.literal(value + " / " + max), x + w - 2, y, BookStyle.INK);
        g.setColor(((tint >> 16) & 255) / 255f, ((tint >> 8) & 255) / 255f, (tint & 255) / 255f, 1);
        bar(g, x, y + 10, w - 2, Mth.clamp(frac, 0, 1), fill);
        g.setColor(1, 1, 1, 1);
        return y + 22;
    }

    // --- the faction row (v0.13) ---------------------------------------------------------------------

    /** The next step on the allegiance road (the side's next rite), or null at its end. */
    private static RoadmapNode nextRite() {
        var nodes = BookData.roadmap(ALLEGIANCE_ROAD);
        if (nodes.isEmpty()) return null;
        return RoadmapState.next(nodes, RoadmapState.of(nodes, ClientHunterLog.PROGRESS));
    }

    /**
     * Emblem, rank and the Grace or Corruption bar (a hunter's ranks have none: "free will" instead). Hover: the next rite;
     * click: the roadmap, on it.
     */
    private void renderFaction(GuiGraphics g, int y, int mx, int my) {
        var a = org.papiricoh.supernaturalcraft.allegiance.Allegiances.get(mc.player);
        var f = a.faction();
        boolean over = forcedFactionHover || Ink.over(mx, my, L, y - 1, PW, FACTION_H);
        if (over) Ink.hover(g, L, y - 1, PW, FACTION_H);
        AllegianceGui.drawEmblem(g, f, L + 10, y + 10, 18, a.committed() || a.rank() > 0 ? 1 : 0.55f);
        int x = L + 23, w = PW - 23;
        Component rank = Component.translatable(org.papiricoh.supernaturalcraft.allegiance.Ranks.titleKey(f, a.rank()));
        String roman = org.papiricoh.supernaturalcraft.allegiance.Ranks.roman(a.rank());
        Component title = roman.isEmpty() ? rank : Component.empty().append(rank).append(" " + roman);
        int titleColour = over ? BookStyle.GOLD : a.isDemon() ? BookStyle.BLOOD : a.isAngel() ? BookStyle.GOLD : BookStyle.INK;
        if (a.committed()) {
            stat(g, title, titleColour, Math.round(a.essence()), a.maxEssence(), a.essence() / Math.max(1f, a.maxEssence()),
                    a.isAngel() ? BookAtlas.BAR_SANITY : BookAtlas.BAR_DANGER, a.isAngel() ? 0xF0C860 : 0xFFFFFF, x, y, w);
        } else {
            Ink.left(g, font, title, x, y, w, titleColour);
            long wait = mc.level == null ? 0 : a.cooldownUntil() - mc.level.getGameTime();
            Component sub = wait > 0 ? Component.translatable(KEY + "allegiance.cured", Math.max(1, (wait + DAY - 1) / DAY))
                    : Component.translatable(KEY + "allegiance.free_will");
            Ink.left(g, font, sub.copy().withStyle(ChatFormatting.ITALIC), x, y + 11, w, BookStyle.FADED);
        }
        if (over) {
            List<Component> lines = new ArrayList<>();
            lines.add(title);
            if (a.committed()) {
                lines.add(Component.translatable(KEY + "allegiance." + f.getSerializedName(), Math.round(a.essence()), a.maxEssence())
                        .withStyle(ChatFormatting.GRAY));
            }
            RoadmapNode next = nextRite();
            if (next != null) {
                lines.add(Component.translatable(KEY + "allegiance.next", Component.translatable(next.nameKey())).withStyle(ChatFormatting.GOLD));
                lines.add(Component.translatable(next.hintKey()).withStyle(ChatFormatting.GRAY));
            } else {
                lines.add(Component.translatable(KEY + "allegiance.top").withStyle(ChatFormatting.GRAY));
            }
            lines.add(Component.translatable(KEY + "allegiance.open").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
            book.tooltip(lines);
        }
    }

    /** A bar sprite stretched to w by its middle (its 3 px ends kept), filled to {@code frac}. */
    private void bar(GuiGraphics g, int x, int y, int w, float frac, BookAtlas.Sprite fill) {
        stretch(g, BookAtlas.BAR_FRAME, x, y, w);
        int fw = Math.round(w * frac);
        if (fw > 0) {
            book.scissor(g, x, y, x + fw, y + 8);
            stretch(g, fill, x, y, w);
            g.disableScissor();
        }
    }

    private static void stretch(GuiGraphics g, BookAtlas.Sprite s, int x, int y, int w) {
        int cap = 3, sh = s.sheet();
        g.blit(s.texture(), x, y, s.u(), s.v(), cap, s.h(), sh, sh);
        g.blit(s.texture(), x + cap, y, w - 2 * cap, s.h(), s.u() + cap, s.v(), s.w() - 2 * cap, s.h(), sh, sh);
        g.blit(s.texture(), x + w - cap, y, s.u() + s.w() - cap, s.v(), cap, s.h(), sh, sh);
    }

    private int badge(GuiGraphics g, Component text, int x, int y, int color, int mx, int my, String descKey) {
        int w = font.width(text) + 8;
        BookAtlas.panel(g, BookAtlas.CARD, x, y - 2, w, 13);
        g.drawString(font, text, x + 4, y + 1, color, false);
        if (Ink.over(mx, my, x, y - 2, w, 13)) book.tooltip(List.of(text, Component.translatable(descKey).withStyle(ChatFormatting.GRAY)));
        return x + w + 3;
    }

    private void renderDeal(GuiGraphics g, int y, int mx, int my) {
        Ink.heading(g, font, Component.translatable(KEY + "deal"), L, y, PW, BookStyle.INK);
        y += 24;
        var deal = ClientHunterLog.deal();
        if (deal.isEmpty()) {
            Ink.centred(g, font, Component.translatable(KEY + "deal.none").withStyle(ChatFormatting.ITALIC), L + PW / 2, y + 4, PW, BookStyle.FADED);
            return;
        }
        HunterLogSyncPayload.DealSummary d = deal.get();
        int top = y;
        g.renderFakeItem(Ink.item(SupernaturalCraft.asResource("crossroads_contract")), L + 2, y + 2);
        Component wish = Component.translatable(DealTerms.keyOf(d.wish() + "." + d.arg()));
        Ink.left(g, font, wish, L + 24, y, PW - 24, BookStyle.INK);
        Ink.left(g, font, Component.translatable(KEY + "deal.state." + d.state()), L + 24, y + 11, PW - 24, BookStyle.FADED);
        y += 24;
        Component when;
        int color = BookStyle.INK;
        switch (d.state()) {
            case "open" -> {
                long left = ClientHunterLog.dealTicksLeft();
                if (left <= 0) {
                    when = Component.translatable(KEY + "deal.due");
                    color = BookStyle.BLOOD;
                } else {
                    long days = left / DAY, hours = left % DAY / 1000;
                    when = days > 0 ? Component.translatable(KEY + "deal.left.days", days, hours) : Component.translatable(KEY + "deal.left.hours", Math.max(1, hours));
                    if (left < DAY) color = BookStyle.BLOOD;
                }
            }
            case "collecting" -> {
                when = Component.translatable(KEY + "deal.hounds");
                color = BookStyle.BLOOD;
            }
            case "hunted" -> {
                when = Component.translatable(KEY + "deal.hunted");
                color = BookStyle.BLOOD;
            }
            default -> when = null;
        }
        if (when != null) {
            for (FormattedCharSequence line : font.split(when, PW - 4)) {
                g.drawString(font, line, L + 2, y, color, false);
                y += 10;
            }
        }
        if (Ink.over(mx, my, L, top, PW, 22)) {
            book.tooltip(List.of(wish, Component.translatable(DealTerms.keyOf(d.wish() + "." + d.arg()) + ".desc").withStyle(ChatFormatting.GRAY)));
        }
    }

    // --- right page --------------------------------------------------------------------------------

    private RoadmapNode next() {
        var nodes = BookData.roadmap();
        return RoadmapState.next(nodes, RoadmapState.of(nodes, ClientHunterLog.PROGRESS));
    }

    private void renderNext(GuiGraphics g, int mx, int my) {
        Ink.heading(g, font, Component.translatable(KEY + "next"), R, TOP + 2, PW, BookStyle.INK);
        RoadmapNode node = next();
        BookAtlas.panel(g, BookAtlas.CARD, R, NEXT_Y, PW, NEXT_H);
        if (node == null) {
            Ink.centred(g, font, Component.translatable(KEY + "next.none").withStyle(ChatFormatting.ITALIC), R + PW / 2, NEXT_Y + 16, PW - 12, BookStyle.FADED);
            return;
        }
        boolean over = Ink.over(mx, my, R, NEXT_Y, PW, NEXT_H);
        if (over) {
            Ink.hover(g, R + 2, NEXT_Y + 2, PW - 4, NEXT_H - 4);
            book.tooltip(List.of(Component.translatable(KEY + "next.open")));
        }
        int fx = R + 4, fy = NEXT_Y + (NEXT_H - 32) / 2;
        if (node.boss()) {
            BookAtlas.bossFrame(1).draw(g, fx, fy);
            Ink.silhouette(g, Ink.item(node.icon()), fx + 8, fy + 8);
        } else {
            BookAtlas.stepFrame(1).draw(g, fx + 4, fy + 4);
            Ink.silhouette(g, Ink.item(node.icon()), fx + 8, fy + 8);
        }
        int tx = R + 42, tw = PW - 46;
        Ink.left(g, font, Component.translatable(node.nameKey()), tx, NEXT_Y + 6, tw, over ? BookStyle.GOLD : BookStyle.INK);
        List<FormattedCharSequence> hint = font.split(Component.translatable(node.hintKey()), tw);
        for (int i = 0; i < Math.min(2, hint.size()); i++) {
            FormattedCharSequence line = hint.get(i);
            if (i == 1 && hint.size() > 2) {
                // Cut the second line short when the hint runs on.
                StringBuilder sb = new StringBuilder();
                line.accept((idx, style, cp) -> {
                    sb.appendCodePoint(cp);
                    return true;
                });
                line = Ink.fit(font, Component.literal(sb + " " + Ink.ELLIPSIS), tw);
            }
            g.drawString(font, line, tx, NEXT_Y + 17 + i * 9, BookStyle.FADED, false);
        }
    }

    private List<Tally> collection() {
        List<Tally> out = new ArrayList<>();
        var level = mc.level;
        int sigils = 0, known = 0;
        if (level != null) {
            var registry = level.registryAccess().registryOrThrow(SNRegistries.SIGIL);
            sigils = registry.size();
            for (ResourceLocation id : ClientArcana.known()) if (registry.containsKey(id)) known++;
        }
        out.add(new Tally(Component.translatable(KEY + "sigils"), known, sigils, () -> book.show(Tab.SCRIPTORIUM)));
        List<ResourceLocation> spells = level != null ? BowlSpells.allSpells(level.getRecipeManager()) : List.of();
        int learned = (int) spells.stream().filter(ClientArcana.rites()::contains).count();
        out.add(new Tally(Component.translatable(KEY + "bowl_spells"), learned, spells.size(), () -> {
            book.show(Tab.JOURNAL);
            book.journal().openChapter(JournalChapter.BOWL);
        }));
        int beaten = 0;
        for (BossProgression.Boss b : BossProgression.Boss.values()) {
            if (ClientHunterLog.advancements().contains(SupernaturalCraft.asResource(b.advancement))) beaten++;
        }
        out.add(new Tally(Component.translatable(KEY + "bosses"), beaten, BossProgression.Boss.values().length, () -> book.show(Tab.ROADMAP)));
        Set<ResourceLocation> creatures = new HashSet<>();
        JournalChapter bestiary = null;
        for (JournalEntry e : BookData.entries().values()) {
            if (e.creature().isEmpty()) continue;
            creatures.add(e.creature().get());
            if (bestiary == null) bestiary = e.chapter();
        }
        int seen = (int) creatures.stream().filter(c -> ClientHunterLog.seen().contains(c) || ClientHunterLog.kills(c) > 0).count();
        JournalChapter bestiaryChapter = bestiary != null ? bestiary : JournalChapter.DEMONS;
        out.add(new Tally(Component.translatable(KEY + "bestiary"), seen, creatures.size(), () -> {
            book.show(Tab.JOURNAL);
            book.journal().openChapter(bestiaryChapter);
        }));
        int unread = JournalPages.unreadCount();
        out.add(new Tally(Component.translatable(KEY + "unread"), unread, -1, () -> {
            book.show(Tab.JOURNAL);
            for (JournalChapter c : JournalChapter.values()) {
                if (JournalPages.chapter(c).stream().anyMatch(JournalPages.Page::unread)) {
                    book.journal().openChapter(c);
                    return;
                }
            }
            book.journal().openIndex();
        }));
        return out;
    }

    private void renderCollection(GuiGraphics g, int mx, int my) {
        Ink.heading(g, font, Component.translatable(KEY + "collection"), R, COLLECTION_Y - 20, PW, BookStyle.INK);
        List<Tally> tallies = collection();
        for (int i = 0; i < tallies.size(); i++) {
            Tally t = tallies.get(i);
            int y = COLLECTION_Y + i * COLLECTION_ROW;
            boolean over = Ink.over(mx, my, R, y - 1, PW, COLLECTION_ROW);
            if (over) Ink.hover(g, R, y - 1, PW, COLLECTION_ROW);
            Component count = t.of() < 0 ? Component.literal(String.valueOf(t.have())) : Component.literal(t.have() + " / " + t.of());
            boolean complete = t.of() > 0 && t.have() >= t.of();
            int color = t.of() < 0 ? (t.have() > 0 ? BookStyle.GOLD : BookStyle.FADED) : complete ? BookStyle.GOLD : BookStyle.INK;
            Ink.left(g, font, t.label(), R + 4, y + 1, PW - 60, over ? BookStyle.INK : BookStyle.FADED);
            // Dotted leader from the label to the count, as in a ledger.
            int lx = R + 6 + font.width(t.label()), rx = R + PW - 6 - font.width(count);
            for (int x = lx + 2; x < rx - 2; x += 3) g.fill(x, y + 7, x + 1, y + 8, 0x553B2A1A);
            Ink.right(g, font, count, R + PW - 4, y + 1, color);
            if (t.of() < 0 && t.have() > 0) BookAtlas.DOT_GOLD.draw(g, R + PW - 12 - font.width(count), y + 2);
        }
    }

    private record Card(Component label, ResourceLocation icon, Runnable go) {
    }

    private List<Card> cards() {
        return List.of(
                new Card(Component.translatable(KEY + "card.journal"), ResourceLocation.withDefaultNamespace("writable_book"), () -> book.show(Tab.JOURNAL)),
                new Card(Component.translatable(KEY + "card.scriptorium"), SupernaturalCraft.asResource("spell_scroll"), () -> book.show(Tab.SCRIPTORIUM)),
                new Card(Component.translatable(KEY + "card.roadmap"), ResourceLocation.withDefaultNamespace("filled_map"), () -> book.show(Tab.ROADMAP)));
    }

    private static int cardX(int i) {
        return R + i * (CARD_W + CARD_GAP);
    }

    private void renderCards(GuiGraphics g, int mx, int my) {
        List<Card> cards = cards();
        for (int i = 0; i < cards.size(); i++) {
            Card c = cards.get(i);
            int x = cardX(i);
            boolean over = Ink.over(mx, my, x, CARDS_Y, CARD_W, CARD_H);
            BookAtlas.panel(g, BookAtlas.CARD, x, CARDS_Y, CARD_W, CARD_H);
            if (over) Ink.hover(g, x + 2, CARDS_Y + 2, CARD_W - 4, CARD_H - 4);
            g.renderFakeItem(Ink.item(c.icon()), x + (CARD_W - 16) / 2, CARDS_Y + 4);
            Ink.centredFitted(g, font, c.label(), x + CARD_W / 2, CARDS_Y + 21, CARD_W - 6, over ? BookStyle.GOLD : BookStyle.INK);
        }
    }

    private void renderBookmarks(GuiGraphics g, int mx, int my) {
        Component label = Component.translatable(KEY + "bookmarks");
        BookAtlas.RIBBON_ON.draw(g, R + 2, MARKS_Y - 3, 5, 12);
        g.drawString(font, label, R + 10, MARKS_Y, BookStyle.INK, false);
        Ink.rule(g, R + 14 + font.width(label), MARKS_Y + 4, PW - 18 - font.width(label), 0x553B2A1A);
        List<ResourceLocation> marks = ClientHunterLog.bookmarks();
        if (marks.isEmpty()) {
            Ink.left(g, font, Component.translatable(KEY + "bookmarks.none").withStyle(ChatFormatting.ITALIC), R + 4, MARKS_LIST_Y + 1, PW - 8, BookStyle.FADED);
            return;
        }
        int max = Math.max(0, marks.size() * MARKS_ROW - (MARKS_BOTTOM - MARKS_LIST_Y));
        marksScroll = Mth.clamp(marksScroll, 0, max);
        book.scissor(g, R, MARKS_LIST_Y, R + PW, MARKS_BOTTOM);
        boolean inList = my >= MARKS_LIST_Y && my < MARKS_BOTTOM;
        for (int i = 0; i < marks.size(); i++) {
            int y = MARKS_LIST_Y + i * MARKS_ROW - (int) marksScroll;
            if (y + MARKS_ROW < MARKS_LIST_Y || y > MARKS_BOTTOM) continue;
            boolean over = inList && Ink.over(mx, my, R, y, PW - 4, MARKS_ROW);
            if (over) Ink.hover(g, R, y, PW - 4, MARKS_ROW);
            g.drawString(font, "›", R + 4, y + 2, BookStyle.BLOOD, false);
            Ink.left(g, font, JournalPages.titleOf(marks.get(i)), R + 12, y + 2, PW - 18, over ? BookStyle.GOLD : BookStyle.INK);
        }
        g.disableScissor();
        if (max > 0) {
            int track = MARKS_BOTTOM - MARKS_LIST_Y, thumb = Math.max(6, track * track / (marks.size() * MARKS_ROW));
            int ty = MARKS_LIST_Y + (int) ((track - thumb) * (marksScroll / max));
            g.fill(R + PW - 3, ty, R + PW, ty + thumb, BookStyle.FADED);
        }
    }

    @Override
    public List<PreviewShot> previewShots() {
        return List.of(new PreviewShot("open", b -> forcedFactionHover = false),
                new PreviewShot("faction", b -> forcedFactionHover = true));
    }

    // --- input ---------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || mc.player == null) return false;
        if (factionY >= 0 && Ink.over(mx, my, L, factionY - 1, PW, FACTION_H)) {
            click();
            book.show(Tab.ROADMAP);
            RoadmapNode node = nextRite();
            if (node != null) book.roadmap().focus(node.id());
            else book.roadmap().select(ALLEGIANCE_ROAD);
            return true;
        }
        if (Ink.over(mx, my, R, NEXT_Y, PW, NEXT_H)) {
            RoadmapNode node = next();
            if (node != null) {
                click();
                book.show(Tab.ROADMAP);
                book.roadmap().focus(node.id());
            }
            return true;
        }
        List<Tally> tallies = collection();
        for (int i = 0; i < tallies.size(); i++) {
            if (Ink.over(mx, my, R, COLLECTION_Y + i * COLLECTION_ROW - 1, PW, COLLECTION_ROW)) {
                click();
                tallies.get(i).go().run();
                return true;
            }
        }
        List<Card> cards = cards();
        for (int i = 0; i < cards.size(); i++) {
            if (Ink.over(mx, my, cardX(i), CARDS_Y, CARD_W, CARD_H)) {
                click();
                cards.get(i).go().run();
                return true;
            }
        }
        if (my >= MARKS_LIST_Y && my < MARKS_BOTTOM && Ink.over(mx, my, R, MARKS_LIST_Y, PW - 4, MARKS_BOTTOM - MARKS_LIST_Y)) {
            int i = (int) Math.floor((my - MARKS_LIST_Y + marksScroll) / MARKS_ROW);
            List<ResourceLocation> marks = ClientHunterLog.bookmarks();
            if (i >= 0 && i < marks.size()) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
                book.show(Tab.JOURNAL);
                book.journal().openEntry(marks.get(i));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (Ink.over(mx, my, R, MARKS_LIST_Y, PW, MARKS_BOTTOM - MARKS_LIST_Y)) {
            marksScroll = Math.max(0, marksScroll - sy * MARKS_ROW);
            return true;
        }
        return false;
    }

    private void click() {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }
}
