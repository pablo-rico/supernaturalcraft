package org.papiricoh.supernaturalcraft.client.book.scriptorium;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.ClientArcana;
import org.papiricoh.supernaturalcraft.client.book.BookAtlas;
import org.papiricoh.supernaturalcraft.client.book.BookButton;
import org.papiricoh.supernaturalcraft.client.book.BookSection;
import org.papiricoh.supernaturalcraft.client.book.BookStyle;
import org.papiricoh.supernaturalcraft.client.book.HunterBookScreen;
import org.papiricoh.supernaturalcraft.magic.item.GrimoireItem;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellBook;
import org.papiricoh.supernaturalcraft.network.ComposeSpellPayload;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Scriptorium: the left page is the composing table (form, effects and modifiers, an animated
 * preview, the cost, the name, and where to write it: a page of the grimoire in hand or a batch of
 * scrolls); the right page holds two leaves, the sigil encyclopedia ({@link SigilIndex}) and the
 * design library ({@link Library}). The draft survives tab changes and, through {@link #lastDraft},
 * closing the book.
 */
public class ScriptoriumSection extends BookSection {

    private static final int L = BookStyle.LEFT_X, W = BookStyle.PAGE_W, TOP = BookStyle.PAGE_Y;
    /** Where the right page's leaves start, under their headings. */
    static final int RIGHT_TOP = TOP + 18;

    private static final int SLOT_Y = TOP + 11, PREVIEW_Y = TOP + 35, PREVIEW_H = 70;
    private static final int COST_Y = PREVIEW_Y + PREVIEW_H + 4, NAME_Y = COST_Y + 21, PAGE_Y = NAME_Y + 17;
    private static final int INSCRIBE_Y = PAGE_Y + 19, SCROLL_Y = INSCRIBE_Y + 20, CLEAR_Y = SCROLL_Y + 21;
    private static final int FORM_X = L + 8, EFFECT_X = FORM_X + 28, MODIFIER_X = EFFECT_X + 72, SLOT_STEP = 22;
    private static final int PAGE_BUTTONS_X = L + 30, PAGE_STEP = 17;

    // What the table held when the book was last closed.
    private static Spell lastDraft = Spell.EMPTY;
    private static int lastPage = -1, lastCount = 1;
    private static boolean lastLibrary;

    final Draft draft = Draft.of(lastDraft);
    int page = lastPage;
    int count = lastCount;
    boolean library = lastLibrary;
    final SigilIndex index = new SigilIndex(this);
    final Library designs = new Library(this);

    private int ticks;
    /** For screenshots: holds the preview at this point of its loop (0..1). */
    private @Nullable Float freeze;
    private @Nullable Component status;
    private int statusColor, statusTicks;

    private InkField name;
    private final BookButton[] pages = new BookButton[SpellBook.PAGES];
    private BookButton inscribe, scrolls, fewer, more, clear;

    // --- state ----------------------------------------------------------------------------------

    HunterBookScreen book() {
        return book;
    }

    Font font() {
        return font;
    }

    private static @Nullable ItemStack grimoire() {
        var player = Minecraft.getInstance().player;
        InteractionHand hand = player == null ? null : GrimoireItem.heldHand(player);
        return hand == null ? null : player.getItemInHand(hand);
    }

    private static SpellBook spellBook(@Nullable ItemStack grimoire) {
        return grimoire == null ? SpellBook.EMPTY : grimoire.getOrDefault(AllDataComponents.SPELL_BOOK, SpellBook.EMPTY);
    }

    private static boolean creative() {
        var player = Minecraft.getInstance().player;
        return player != null && player.getAbilities().instabuild;
    }

    private static int owned(Item item) {
        var player = Minecraft.getInstance().player;
        return player == null ? 0 : player.getInventory().countItem(item);
    }

    void loadDraft(Spell spell) {
        draft.load(spell);
        if (name != null) name.setValue(spell.name());
    }

    /** Puts a known sigil on the table, in the slot of its kind. */
    void addSigil(ResourceLocation id) {
        SigilComponent s = Sigils.get(id);
        if (s != null && Sigils.known(id) && draft.add(id, s)) {
            click();
        } else {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 0.6f, 0.4f));
        }
    }

    void click() {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.2f, 0.5f));
    }

    void status(Component text, int color) {
        status = text;
        statusColor = color;
        statusTicks = 80;
    }

    private void selectPage(int i) {
        page = i;
        Spell there = spellBook(grimoire()).page(i);
        if (!there.isEmpty()) loadDraft(there);
    }

    private void setCount(int n) {
        count = Math.max(1, Math.min(ComposeSpellPayload.MAX_SCROLLS, n));
    }

    private void inscribe() {
        Spell spell = draft.toSpell();
        PacketDistributor.sendToServer(new ComposeSpellPayload(page, spell, false, 1));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.0f));
        status(Component.translatable(spell.isEmpty() ? "screen.supernaturalcraft.book.scriptorium.erased"
                : "screen.supernaturalcraft.book.scriptorium.inscribed", page + 1), BookStyle.INK);
    }

    private void writeScrolls() {
        PacketDistributor.sendToServer(new ComposeSpellPayload(page, draft.toSpell(), true, count));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.0f));
        status(Component.translatable("screen.supernaturalcraft.book.scriptorium.written", count), BookStyle.INK);
    }

    private void showLeaf(boolean lib) {
        if (lib == library) return;
        library = lib;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
        book.refresh();
    }

    // --- widgets --------------------------------------------------------------------------------

    @Override
    public void init() {
        ItemStack grimoire = grimoire();
        if (page < 0) {
            SpellBook sb = spellBook(grimoire);
            page = sb.selected();
            if (draft.toSpell().isEmpty() && grimoire != null) loadDraft(sb.current());
        }
        name = book.add(new InkField(font, L + 30, NAME_Y + 3, W - 32, Spell.MAX_NAME,
                Component.translatable("screen.supernaturalcraft.composer.name"),
                Component.translatable("screen.supernaturalcraft.book.scriptorium.name_hint"), v -> draft.name = v));
        name.setValue(draft.name);
        for (int i = 0; i < SpellBook.PAGES; i++) {
            int p = i;
            pages[i] = book.add(new BookButton(PAGE_BUTTONS_X + i * PAGE_STEP, PAGE_Y, 14, 14, Component.literal(String.valueOf(i + 1)),
                    () -> selectPage(p)));
        }
        inscribe = book.add(new BookButton(L, INSCRIBE_Y, 84, 16, Component.translatable("screen.supernaturalcraft.composer.inscribe"), this::inscribe));
        scrolls = book.add(new BookButton(L, SCROLL_Y, 64, 16, Component.empty(), this::writeScrolls));
        fewer = book.add(new BookButton(L + 66, SCROLL_Y + 1, 14, 14, Component.literal("-"), () -> setCount(count - 1)));
        more = book.add(new BookButton(L + 82, SCROLL_Y + 1, 14, 14, Component.literal("+"), () -> setCount(count + 1)));
        clear = book.add(new BookButton(L, CLEAR_Y, 50, 14, Component.translatable("screen.supernaturalcraft.composer.clear"), () -> {
            loadDraft(Spell.EMPTY);
        }));
        if (library) designs.init();
    }

    @Override
    public void tick() {
        ticks++;
        if (statusTicks > 0 && --statusTicks == 0) status = null;
    }

    @Override
    public void hidden() {
        lastDraft = draft.toSpell();
        lastPage = page;
        lastCount = count;
        lastLibrary = library;
    }

    // --- drawing --------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        Spell spell = draft.toSpell();
        ItemStack grimoire = grimoire();
        SpellBook sb = spellBook(grimoire);
        updateWidgets(spell, grimoire, sb);

        drawSlots(g, mx, my);
        float time = freeze != null ? freeze * SpellPreview.PERIOD : ticks + partial;
        BookAtlas.panel(g, BookAtlas.CARD, L, PREVIEW_Y, W, PREVIEW_H);
        SpellPreview.render(g, book, font, L + 3, PREVIEW_Y + 3, W - 6, PREVIEW_H - 6, SpellPreview.Look.of(draft), time,
                draft.form != null ? GrimoireItem.spellName(spell) : null,
                Component.translatable("screen.supernaturalcraft.book.scriptorium.idle"));
        drawCost(g, mx, my, spell);

        g.drawString(font, Component.translatable("screen.supernaturalcraft.book.scriptorium.name"), L, NAME_Y + 3, BookStyle.FADED, false);
        g.fill(L + 28, NAME_Y + 13, L + W, NAME_Y + 14, 0x667A6448);

        g.drawString(font, Component.translatable("screen.supernaturalcraft.book.scriptorium.page"), L, PAGE_Y + 3, BookStyle.FADED, false);
        Spell onPage = sb.page(page);
        if (grimoire != null) {
            String there = onPage.isEmpty() ? Component.translatable("screen.supernaturalcraft.book.scriptorium.blank").getString()
                    : GrimoireItem.spellName(onPage).getString();
            int room = L + W - (PAGE_BUTTONS_X + SpellBook.PAGES * PAGE_STEP);
            if (font.width(there) > room) there = font.plainSubstrByWidth(there, room - font.width("…")) + "…";
            g.drawString(font, there, L + W - font.width(there), PAGE_Y + 3, BookStyle.FADED, false);
        }
        for (int i = 0; i < SpellBook.PAGES; i++) {
            int bx = PAGE_BUTTONS_X + i * PAGE_STEP;
            if (grimoire != null && mx >= bx && mx < bx + 14 && my >= PAGE_Y && my < PAGE_Y + 14) {
                book.tooltip(List.of(GrimoireItem.describe(sb.page(i), i)));
            }
        }

        drawDestination(g, spell, grimoire, onPage);
        drawHeadings(g, mx, my);
        if (library) designs.render(g, mx, my);
        else index.render(g, mx, my);
    }

    @Override
    public void renderOverlay(GuiGraphics g, int mx, int my, float partial) {
        SpellBook sb = spellBook(grimoire());
        if (grimoire() == null) return;
        for (int i = 0; i < SpellBook.PAGES; i++) {
            int bx = PAGE_BUTTONS_X + i * PAGE_STEP;
            if (i == page) g.renderOutline(bx - 1, PAGE_Y - 1, 16, 16, BookStyle.GOLD);
            if (!sb.page(i).isEmpty()) g.fill(bx + 10, PAGE_Y + 2, bx + 12, PAGE_Y + 4, BookStyle.GOLD);
        }
    }

    private void updateWidgets(Spell spell, @Nullable ItemStack grimoire, SpellBook sb) {
        boolean held = grimoire != null, creative = creative();
        Spell onPage = sb.page(page);
        boolean erase = spell.isEmpty() && !onPage.isEmpty();
        for (BookButton b : pages) b.active = held;
        inscribe.active = held && !onPage.equals(spell) && (spell.isComplete() || erase)
                && (creative || erase || owned(AllItems.ENOCHIAN_INK.get()) >= 1);
        inscribe.setMessage(Component.translatable(erase ? "screen.supernaturalcraft.book.scriptorium.erase" : "screen.supernaturalcraft.composer.inscribe"));
        scrolls.active = held && spell.isComplete()
                && (creative || owned(Items.PAPER) >= count && owned(AllItems.ENOCHIAN_INK.get()) >= count);
        scrolls.setMessage(Component.translatable("screen.supernaturalcraft.book.scriptorium.scrolls", count));
        fewer.active = count > 1;
        more.active = count < ComposeSpellPayload.MAX_SCROLLS;
        clear.active = !draft.all().isEmpty() || !draft.name.isEmpty();
    }

    /** The seven slots of the table, under their labels; a click on a filled one lifts the sigil. */
    private void drawSlots(GuiGraphics g, int mx, int my) {
        label(g, "form", FORM_X, 20);
        label(g, "effects", EFFECT_X, Spell.MAX_EFFECTS * SLOT_STEP - 2);
        label(g, "modifiers", MODIFIER_X, Spell.MAX_MODIFIERS * SLOT_STEP - 2);
        slot(g, mx, my, FORM_X, draft.form);
        for (int i = 0; i < Spell.MAX_EFFECTS; i++) {
            slot(g, mx, my, EFFECT_X + i * SLOT_STEP, i < draft.effects.size() ? draft.effects.get(i) : null);
        }
        for (int i = 0; i < Spell.MAX_MODIFIERS; i++) {
            slot(g, mx, my, MODIFIER_X + i * SLOT_STEP, i < draft.modifiers.size() ? draft.modifiers.get(i) : null);
        }
        g.drawString(font, "+", EFFECT_X - 8, SLOT_Y + 6, BookStyle.FADED, false);
        g.drawString(font, "×", MODIFIER_X - 8, SLOT_Y + 6, BookStyle.FADED, false);
    }

    private void label(GuiGraphics g, String key, int x, int w) {
        Component text = Component.translatable("screen.supernaturalcraft.composer." + key);
        g.drawString(font, text, x + (w - font.width(text)) / 2, TOP + 1, BookStyle.FADED, false);
    }

    private void slot(GuiGraphics g, int mx, int my, int x, @Nullable ResourceLocation id) {
        boolean over = mx >= x && mx < x + 20 && my >= SLOT_Y && my < SLOT_Y + 20;
        (over && id != null ? BookAtlas.SIGIL_SLOT_HOVER : BookAtlas.SIGIL_SLOT).draw(g, x, SLOT_Y);
        if (id == null) return;
        Sigils.glyph(g, id, x + 2, SLOT_Y + 2, 16, true);
        if (over) book.tooltip(Sigils.tooltip(id, Component.translatable("screen.supernaturalcraft.book.scriptorium.click_remove")));
    }

    /** "Mana: 42" and the reagents a cast burns, red where the satchel falls short. */
    private void drawCost(GuiGraphics g, int mx, int my, Spell spell) {
        float mana = Sigils.mana(spell);
        boolean tooMuch = mana > ClientArcana.maxMana();
        Component text = Component.translatable("screen.supernaturalcraft.composer.mana", Sigils.num(mana));
        g.drawString(font, text, L, COST_Y + 4, spell.isEmpty() ? BookStyle.FADED : tooMuch ? BookStyle.BLOOD : BookStyle.MANA, false);
        if (tooMuch && mx >= L && mx < L + font.width(text) && my >= COST_Y && my < COST_Y + 16) {
            book.tooltip(List.of(Component.translatable("screen.supernaturalcraft.book.scriptorium.too_much", Sigils.num(ClientArcana.maxMana()))
                    .withStyle(ChatFormatting.RED)));
        }
        int x = L + font.width(text) + 10;
        Map<Item, Integer> reagents = Sigils.reagents(spell);
        if (reagents.isEmpty()) {
            if (spell.isComplete()) {
                g.drawString(font, Component.translatable("screen.supernaturalcraft.book.scriptorium.no_reagents"), x, COST_Y + 4, BookStyle.FADED, false);
            }
            return;
        }
        boolean creative = creative();
        for (var e : reagents.entrySet()) {
            int have = owned(e.getKey());
            ItemStack stack = new ItemStack(e.getKey());
            g.renderItem(stack, x, COST_Y);
            String n = "×" + e.getValue();
            g.drawString(font, n, x + 17, COST_Y + 4, !creative && have < e.getValue() ? BookStyle.BLOOD : BookStyle.INK, false);
            if (mx >= x && mx < x + 17 + font.width(n) && my >= COST_Y && my < COST_Y + 16) {
                List<Component> lines = new ArrayList<>();
                lines.add(stack.getHoverName());
                lines.add(Component.translatable("screen.supernaturalcraft.book.scriptorium.per_cast", e.getValue(), have)
                        .withStyle(have < e.getValue() ? ChatFormatting.RED : ChatFormatting.GRAY));
                book.tooltip(lines);
            }
            x += 17 + font.width(n) + 6;
        }
    }

    /** What the Inscribe and Scrolls buttons will spend, and anything that stops them. */
    private void drawDestination(GuiGraphics g, Spell spell, @Nullable ItemStack grimoire, Spell onPage) {
        boolean creative = creative();
        int ink = owned(AllItems.ENOCHIAN_INK.get()), paper = owned(Items.PAPER);
        int tx = L + 90;
        if (grimoire != null) {
            Component where = Component.translatable("screen.supernaturalcraft.book.scriptorium.on_page", page + 1);
            g.drawString(font, where, tx, INSCRIBE_Y, BookStyle.INK, false);
            if (onPage.equals(spell) && !spell.isEmpty()) {
                g.drawString(font, Component.translatable("screen.supernaturalcraft.book.scriptorium.same"), tx, INSCRIBE_Y + 9, BookStyle.FADED, false);
            } else if (!spell.isEmpty()) {
                need(g, "ink", 1, ink, creative, tx, INSCRIBE_Y + 9);
            }
        }
        int sx = L + 100;
        need(g, "paper", count, paper, creative, sx, SCROLL_Y);
        need(g, "ink", count, ink, creative, sx, SCROLL_Y + 9);

        Component hint = null;
        int color = BookStyle.FADED;
        if (status != null) {
            hint = status;
            color = statusColor;
        } else if (grimoire == null) {
            hint = Component.translatable("screen.supernaturalcraft.book.scriptorium.no_grimoire");
            color = BookStyle.BLOOD;
        } else if (!spell.isComplete() && !draft.all().isEmpty()) {
            hint = Component.translatable("screen.supernaturalcraft.composer.incomplete");
        }
        if (hint != null) {
            int y = CLEAR_Y + (font.split(hint, W - 56).size() > 1 ? -1 : 3);
            for (FormattedCharSequence line : font.split(hint, W - 56)) {
                g.drawString(font, line, L + 56, y, color, false);
                y += 9;
            }
        }
    }

    /** "Ink 8/5": what a batch needs over what the satchel holds, red when short. */
    private void need(GuiGraphics g, String what, int need, int have, boolean creative, int x, int y) {
        Component text = Component.translatable("screen.supernaturalcraft.book.scriptorium." + what, need, have);
        g.drawString(font, text, x, y, !creative && have < need ? BookStyle.BLOOD : BookStyle.FADED, false);
    }

    /** The two leaves of the right page, as inked headings; the open one is underlined. */
    private void drawHeadings(GuiGraphics g, int mx, int my) {
        heading(g, mx, my, false, BookStyle.RIGHT_X, "screen.supernaturalcraft.book.scriptorium.tab.sigils");
        heading(g, mx, my, true, BookStyle.RIGHT_X + W / 2, "screen.supernaturalcraft.book.scriptorium.tab.library");
        g.fill(BookStyle.RIGHT_X + W / 2, TOP + 1, BookStyle.RIGHT_X + W / 2 + 1, TOP + 9, 0x557A6448);
    }

    private void heading(GuiGraphics g, int mx, int my, boolean lib, int x, String key) {
        Component text = Component.translatable(key);
        int half = W / 2, tx = x + (half - font.width(text)) / 2;
        boolean open = lib == library;
        boolean over = !open && mx >= x && mx < x + half && my >= TOP && my < TOP + 14;
        g.drawString(font, text, tx, TOP + 1, open ? BookStyle.INK : over ? BookStyle.GOLD : BookStyle.FADED, false);
        if (open) BookAtlas.FLOURISH.draw(g, x + 10, TOP + 10, half - 20, 5);
    }

    // --- input ----------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // A click anywhere else on the page lets go of the name field.
        if (name != null && name.isFocused()) book.setFocused(null);
        if (my >= TOP && my < TOP + 14 && mx >= BookStyle.RIGHT_X && mx < BookStyle.RIGHT_X + W) {
            showLeaf(mx >= BookStyle.RIGHT_X + W / 2);
            return true;
        }
        if (my >= SLOT_Y && my < SLOT_Y + 20) {
            if (hit(mx, FORM_X) && draft.form != null) {
                draft.form = null;
                click();
                return true;
            }
            for (int i = 0; i < draft.effects.size(); i++) {
                if (hit(mx, EFFECT_X + i * SLOT_STEP)) {
                    draft.effects.remove(i);
                    click();
                    return true;
                }
            }
            for (int i = 0; i < draft.modifiers.size(); i++) {
                if (hit(mx, MODIFIER_X + i * SLOT_STEP)) {
                    draft.modifiers.remove(i);
                    click();
                    return true;
                }
            }
        }
        return library ? designs.mouseClicked(mx, my, button) : index.mouseClicked(mx, my, button);
    }

    private static boolean hit(double mx, int x) {
        return mx >= x && mx < x + 20;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (my >= SCROLL_Y && my < SCROLL_Y + 16 && mx >= L && mx < L + W && sy != 0) {
            setCount(count + (sy > 0 ? 1 : -1));
            return true;
        }
        return false;
    }

    // --- screenshots ----------------------------------------------------------------------------

    private static Spell spell(String form, List<String> effects, List<String> modifiers, String name) {
        return new Spell(Optional.of(SupernaturalCraft.asResource(form)), effects.stream().map(SupernaturalCraft::asResource).toList(),
                modifiers.stream().map(SupernaturalCraft::asResource).toList(), name);
    }

    @Override
    public List<PreviewShot> previewShots() {
        return List.of(
                new PreviewShot("compose", b -> {
                    ScriptoriumSection s = b.scriptorium();
                    s.loadDraft(spell("bolt", List.of("smite", "hellfire"), List.of("empower"), ""));
                    s.count = 8;
                    s.freeze = 0.24f;
                    s.library = false;
                    s.index.select(SupernaturalCraft.asResource("hellfire"));
                    b.refresh();
                }),
                new PreviewShot("ward", b -> {
                    ScriptoriumSection s = b.scriptorium();
                    s.loadDraft(spell("ward", List.of("mend", "frost"), List.of("widen", "echo"), "Cold Comfort"));
                    s.freeze = 0.45f;
                    s.library = false;
                    s.index.select(SupernaturalCraft.asResource("echo"));
                    b.refresh();
                }),
                new PreviewShot("library", b -> {
                    ScriptoriumSection s = b.scriptorium();
                    s.loadDraft(spell("burst", List.of("frost"), List.of("extend"), "Cold Snap"));
                    s.freeze = 0.5f;
                    s.library = true;
                    s.designs.select(1);
                    b.refresh();
                }),
                new PreviewShot("sigils", b -> {
                    ScriptoriumSection s = b.scriptorium();
                    s.loadDraft(spell("touch", List.of("mend"), List.of(), ""));
                    s.freeze = 0.4f;
                    s.library = false;
                    s.index.select(SupernaturalCraft.asResource("frost"));
                    b.refresh();
                }),
                new PreviewShot("sigils_unknown", b -> {
                    ScriptoriumSection s = b.scriptorium();
                    s.library = false;
                    s.freeze = 0.4f;
                    s.index.select(SupernaturalCraft.asResource("bind"));
                    b.refresh();
                }));
    }
}
