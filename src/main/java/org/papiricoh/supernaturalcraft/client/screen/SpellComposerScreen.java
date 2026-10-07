package org.papiricoh.supernaturalcraft.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.ClientArcana;
import org.papiricoh.supernaturalcraft.magic.item.GrimoireItem;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;
import org.papiricoh.supernaturalcraft.magic.spell.SigilKind;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellBook;
import org.papiricoh.supernaturalcraft.magic.spell.SpellCost;
import org.papiricoh.supernaturalcraft.network.ComposeSpellPayload;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.SNRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Composes a spell onto a grimoire page. Known sigils on the right; click one to place it in the
 * matching slot on the left, click a filled slot to remove it. Inscribing a page costs one
 * Enochian ink; writing a scroll costs paper and ink. The server re-checks everything.
 */
public class SpellComposerScreen extends Screen {

    private static final ResourceLocation BG = SupernaturalCraft.asResource("textures/gui/composer.png");
    private static final int W = 256, H = 196, SLOT = 20, CELL = 21;
    private static final int INK = 0x3B2A1A, FADED = 0x7A6448;

    private int left, top, page;
    private Optional<ResourceLocation> form = Optional.empty();
    private final List<ResourceLocation> effects = new ArrayList<>();
    private final List<ResourceLocation> modifiers = new ArrayList<>();
    private EditBox nameBox;
    private final Map<SigilKind, List<ResourceLocation>> known = new LinkedHashMap<>();
    private final List<Hit> hits = new ArrayList<>();

    private record Hit(int x, int y, ResourceLocation id, boolean slot, SigilKind kind) {
        boolean contains(double mx, double my) {
            return mx >= x && my >= y && mx < x + SLOT && my < y + SLOT;
        }
    }

    public SpellComposerScreen() {
        super(Component.translatable("screen.supernaturalcraft.composer"));
    }

    private @Nullable ItemStack grimoire() {
        var player = Minecraft.getInstance().player;
        if (player == null) return null;
        InteractionHand hand = GrimoireItem.heldHand(player);
        return hand == null ? null : player.getItemInHand(hand);
    }

    private Registry<SigilComponent> sigils() {
        return Minecraft.getInstance().level.registryAccess().registryOrThrow(SNRegistries.SIGIL);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        ItemStack stack = grimoire();
        if (stack == null) {
            onClose();
            return;
        }
        SpellBook book = stack.getOrDefault(AllDataComponents.SPELL_BOOK, SpellBook.EMPTY);
        nameBox = new EditBox(font, left + 12, top + 104, 112, 14, Component.translatable("screen.supernaturalcraft.composer.name"));
        nameBox.setMaxLength(Spell.MAX_NAME);
        addRenderableWidget(nameBox);
        loadPage(book.selected());

        addRenderableWidget(Button.builder(Component.translatable("screen.supernaturalcraft.journal.open"),
                b -> Minecraft.getInstance().setScreen(new JournalScreen(this))).bounds(left + 62, top + 5, 44, 14).build());
        for (int i = 0; i < SpellBook.PAGES; i++) {
            int p = i;
            addRenderableWidget(Button.builder(Component.literal(String.valueOf(i + 1)), b -> loadPage(p))
                    .bounds(left + 110 + i * 22, top + 5, 20, 14).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.supernaturalcraft.composer.inscribe"), b -> send(false))
                .bounds(left + 10, top + 170, 74, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.supernaturalcraft.composer.scroll"), b -> send(true))
                .bounds(left + 88, top + 170, 74, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.supernaturalcraft.composer.clear"), b -> {
            form = Optional.empty();
            effects.clear();
            modifiers.clear();
            nameBox.setValue("");
        }).bounds(left + 166, top + 170, 80, 18).build());

        known.clear();
        boolean all = Minecraft.getInstance().player.getAbilities().instabuild;
        for (SigilKind kind : SigilKind.values()) known.put(kind, new ArrayList<>());
        for (Map.Entry<net.minecraft.resources.ResourceKey<SigilComponent>, SigilComponent> e : sigils().entrySet()) {
            ResourceLocation id = e.getKey().location();
            if (all || ClientArcana.known().contains(id)) known.get(e.getValue().kind()).add(id);
        }
        known.values().forEach(l -> l.sort(Comparator.comparing(ResourceLocation::toString)));
    }

    private void loadPage(int index) {
        ItemStack stack = grimoire();
        if (stack == null) return;
        page = index;
        Spell spell = stack.getOrDefault(AllDataComponents.SPELL_BOOK, SpellBook.EMPTY).page(index);
        form = spell.form();
        effects.clear();
        effects.addAll(spell.effects());
        modifiers.clear();
        modifiers.addAll(spell.modifiers());
        nameBox.setValue(spell.name());
    }

    private Spell draft() {
        return new Spell(form, List.copyOf(effects), List.copyOf(modifiers), nameBox.getValue().trim());
    }

    private void send(boolean scroll) {
        PacketDistributor.sendToServer(new ComposeSpellPayload(page, draft(), scroll));
    }

    // --- rendering ------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        hits.clear();
        g.drawString(font, title, left + 12, top + 8, INK, false);

        label(g, "form", 12, 26);
        slot(g, left + 12, top + 36, form.orElse(null), SigilKind.FORM);
        label(g, "effects", 40, 26);
        for (int i = 0; i < Spell.MAX_EFFECTS; i++) {
            slot(g, left + 40 + i * 22, top + 36, i < effects.size() ? effects.get(i) : null, SigilKind.EFFECT);
        }
        label(g, "modifiers", 12, 62);
        for (int i = 0; i < Spell.MAX_MODIFIERS; i++) {
            slot(g, left + 12 + i * 22, top + 72, i < modifiers.size() ? modifiers.get(i) : null, SigilKind.MODIFIER);
        }
        label(g, "name", 12, 94);
        drawCost(g);

        int y = top + 26;
        for (Map.Entry<SigilKind, List<ResourceLocation>> e : known.entrySet()) {
            g.drawString(font, Component.translatable("screen.supernaturalcraft.composer.known." + e.getKey().getSerializedName()),
                    left + 136, y, FADED, false);
            y += 10;
            int col = 0;
            for (ResourceLocation id : e.getValue()) {
                int x = left + 136 + col * CELL;
                glyph(g, x, y, id, inDraft(id));
                hits.add(new Hit(x, y, id, false, e.getKey()));
                if (++col == 5) {
                    col = 0;
                    y += CELL;
                }
            }
            if (col != 0 || e.getValue().isEmpty()) y += CELL;
            y += 2;
        }

        for (Hit h : hits) {
            if (h.contains(mx, my) && h.id() != null) {
                g.renderComponentTooltip(font, tooltip(h.id()), mx, my);
                break;
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        super.renderBackground(g, mx, my, partial);
        g.blit(BG, left, top, 0, 0, W, H, 256, 256);
    }

    private void label(GuiGraphics g, String key, int x, int y) {
        g.drawString(font, Component.translatable("screen.supernaturalcraft.composer." + key), left + x, top + y, FADED, false);
    }

    private boolean inDraft(ResourceLocation id) {
        return form.map(id::equals).orElse(false) || effects.contains(id) || modifiers.contains(id);
    }

    private void slot(GuiGraphics g, int x, int y, @Nullable ResourceLocation id, SigilKind kind) {
        g.fill(x, y, x + SLOT, y + SLOT, 0x553B2A1A);
        g.renderOutline(x, y, SLOT, SLOT, 0xFF5C4630);
        if (id != null) {
            g.blit(SigilComponent.glyphTexture(id), x + 2, y + 2, 0, 0, 16, 16, 16, 16);
        }
        hits.add(new Hit(x, y, id, true, kind));
    }

    private void glyph(GuiGraphics g, int x, int y, ResourceLocation id, boolean used) {
        g.fill(x, y, x + SLOT, y + SLOT, used ? 0x66B07D18 : 0x333B2A1A);
        g.blit(SigilComponent.glyphTexture(id), x + 2, y + 2, 0, 0, 16, 16, 16, 16);
    }

    private void drawCost(GuiGraphics g) {
        Registry<SigilComponent> reg = sigils();
        SigilComponent f = form.map(reg::get).orElse(null);
        if (f == null || effects.isEmpty()) {
            g.drawString(font, Component.translatable("screen.supernaturalcraft.composer.incomplete"), left + 12, top + 124, FADED, false);
            return;
        }
        List<Float> effectCosts = new ArrayList<>();
        Map<Item, Integer> reagents = new LinkedHashMap<>();
        f.reagents().forEach(r -> reagents.merge(r.item().value(), r.count(), Integer::sum));
        for (ResourceLocation id : effects) {
            SigilComponent s = reg.get(id);
            if (s == null) continue;
            effectCosts.add(s.manaCost());
            s.reagents().forEach(r -> reagents.merge(r.item().value(), r.count(), Integer::sum));
        }
        List<Float> mults = new ArrayList<>();
        for (ResourceLocation id : modifiers) {
            SigilComponent s = reg.get(id);
            if (s == null) continue;
            mults.add(s.param("mana_multiplier", 1f));
            s.reagents().forEach(r -> reagents.merge(r.item().value(), r.count(), Integer::sum));
        }
        float mana = SpellCost.mana(f.manaCost(), effectCosts, mults, 1f);
        g.drawString(font, Component.translatable("screen.supernaturalcraft.composer.mana", Math.round(mana)), left + 12, top + 124, INK, false);
        int x = left + 12;
        for (Map.Entry<Item, Integer> e : reagents.entrySet()) {
            g.renderItem(new ItemStack(e.getKey()), x, top + 136);
            g.drawString(font, "×" + e.getValue(), x + 16, top + 141, INK, false);
            x += 34;
        }
    }

    private List<Component> tooltip(ResourceLocation id) {
        SigilComponent s = sigils().get(id);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(SigilComponent.translationKey(id)).withStyle(ChatFormatting.GOLD));
        if (s == null) return lines;
        lines.add(Component.translatable("screen.supernaturalcraft.composer.kind." + s.kind().getSerializedName())
                .withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable(SigilComponent.translationKey(id) + ".desc").withStyle(ChatFormatting.GRAY));
        if (s.kind() == SigilKind.MODIFIER) {
            lines.add(Component.translatable("screen.supernaturalcraft.composer.multiplier",
                    String.format("%.1f", s.param("mana_multiplier", 1f))).withStyle(ChatFormatting.BLUE));
        } else {
            lines.add(Component.translatable("screen.supernaturalcraft.composer.mana", Math.round(s.manaCost())).withStyle(ChatFormatting.BLUE));
        }
        for (SigilComponent.Reagent r : s.reagents()) {
            lines.add(Component.literal(r.count() + "× ").append(r.stack().getHoverName()).withStyle(ChatFormatting.DARK_AQUA));
        }
        return lines;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (Hit h : hits) {
            if (!h.contains(mx, my)) continue;
            if (h.slot()) {
                if (h.id() != null) remove(h.id(), h.kind());
            } else {
                add(h.id(), h.kind());
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private void add(ResourceLocation id, SigilKind kind) {
        switch (kind) {
            case FORM -> form = Optional.of(id);
            case EFFECT -> {
                if (!effects.contains(id) && effects.size() < Spell.MAX_EFFECTS) effects.add(id);
            }
            case MODIFIER -> {
                if (modifiers.size() < Spell.MAX_MODIFIERS) modifiers.add(id);
            }
        }
    }

    private void remove(ResourceLocation id, SigilKind kind) {
        switch (kind) {
            case FORM -> form = Optional.empty();
            case EFFECT -> effects.remove(id);
            case MODIFIER -> modifiers.remove(id);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
