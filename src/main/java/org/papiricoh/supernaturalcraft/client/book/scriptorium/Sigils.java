package org.papiricoh.supernaturalcraft.client.book.scriptorium;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.client.ClientArcana;
import org.papiricoh.supernaturalcraft.client.legacy.ClientLegacy;
import org.papiricoh.supernaturalcraft.magic.spell.SigilLookup;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;
import org.papiricoh.supernaturalcraft.magic.spell.SigilKind;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellCost;
import org.papiricoh.supernaturalcraft.registry.SNRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The client's view of the sigil registry: what exists, what the reader knows, what a spell costs. */
final class Sigils {

    private static final String[] ROMAN = {"I", "II", "III"};

    private Sigils() {
    }

    static @Nullable Registry<SigilComponent> registry() {
        var level = Minecraft.getInstance().level;
        return level == null ? null : level.registryAccess().registry(SNRegistries.SIGIL).orElse(null);
    }

    static @Nullable SigilComponent get(@Nullable ResourceLocation id) {
        var level = Minecraft.getInstance().level;
        return level == null || id == null ? null : SigilLookup.get(level.registryAccess(), ClientLegacy.archive(), id);
    }

    /** Whether the reader may draw this sigil (in creative, every one). */
    static boolean known(ResourceLocation id) {
        var player = Minecraft.getInstance().player;
        return (player != null && player.getAbilities().instabuild) || ClientArcana.known().contains(id);
    }

    /** Every sigil of a kind, lowest tier first, then by id. */
    static List<ResourceLocation> byKind(SigilKind kind) {
        var level = Minecraft.getInstance().level;
        if (level == null) return List.of();
        // The registry's sigils and the reader's generated formulas (v0.17).
        Map<ResourceLocation, SigilComponent> all = SigilLookup.all(level.registryAccess(), ClientLegacy.archive());
        List<ResourceLocation> out = new ArrayList<>();
        for (var e : all.entrySet()) if (e.getValue().kind() == kind) out.add(e.getKey());
        out.sort(Comparator.<ResourceLocation>comparingInt(id -> all.get(id).tier()).thenComparing(ResourceLocation::toString));
        return out;
    }

    static MutableComponent name(ResourceLocation id) {
        return SigilLookup.name(ClientLegacy.archive(), id);
    }

    static MutableComponent description(ResourceLocation id) {
        return SigilLookup.description(ClientLegacy.archive(), id);
    }

    static MutableComponent source(ResourceLocation id) {
        return SigilLookup.source(ClientLegacy.archive(), id);
    }

    static Component kindName(SigilKind kind) {
        return Component.translatable("screen.supernaturalcraft.composer.kind." + kind.getSerializedName());
    }

    static String tier(int tier) {
        return ROMAN[Math.max(0, Math.min(ROMAN.length - 1, tier - 1))];
    }

    /** 12 → "12", 1.5 → "1.5". */
    static String num(float f) {
        return f == Math.round(f) ? String.valueOf(Math.round(f)) : String.format(Locale.ROOT, "%.1f", f);
    }

    static float manaMultiplier(SigilComponent s) {
        return s.param("mana_multiplier", 1f);
    }

    /** Draws a 16×16 glyph at {@code size}; an unknown sigil is a faint black silhouette. */
    static void glyph(GuiGraphics g, ResourceLocation id, int x, int y, int size, boolean known) {
        if (!known) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(0, 0, 0, 0.35f);
        }
        g.blit(SigilComponent.glyphTexture(id), x, y, size, size, 0, 0, 16, 16, 16, 16);
        if (!known) {
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.disableBlend();
        }
    }

    /** The mana a spell costs to cast from the grimoire. */
    static float mana(Spell spell) {
        float form = spell.form().map(Sigils::get).map(SigilComponent::manaCost).orElse(0f);
        List<Float> effects = new ArrayList<>(), mults = new ArrayList<>();
        for (ResourceLocation id : spell.effects()) {
            SigilComponent s = get(id);
            if (s != null) effects.add(s.manaCost());
        }
        for (ResourceLocation id : spell.modifiers()) {
            SigilComponent s = get(id);
            if (s != null) mults.add(manaMultiplier(s));
        }
        return SpellCost.mana(form, effects, mults, 1f);
    }

    /** The reagents every cast burns, merged by item. */
    static Map<Item, Integer> reagents(Spell spell) {
        Map<Item, Integer> out = new LinkedHashMap<>();
        List<ResourceLocation> all = new ArrayList<>();
        spell.form().ifPresent(all::add);
        all.addAll(spell.effects());
        all.addAll(spell.modifiers());
        for (ResourceLocation id : all) {
            SigilComponent s = get(id);
            if (s == null) continue;
            for (SigilComponent.Reagent r : s.reagents()) out.merge(r.item().value(), r.count(), Integer::sum);
        }
        return out;
    }

    /** Name, kind, description, cost and reagents, as a tooltip. */
    static List<Component> tooltip(ResourceLocation id, @Nullable Component action) {
        SigilComponent s = get(id);
        List<Component> lines = new ArrayList<>();
        if (s == null) return lines;
        if (!known(id)) {
            lines.add(Component.translatable("screen.supernaturalcraft.book.scriptorium.unknown").withStyle(ChatFormatting.GRAY));
            lines.add(kindName(s.kind()).copy().append(" · ").append(Component.translatable(
                    "screen.supernaturalcraft.book.scriptorium.tier", tier(s.tier()))).withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.translatable("screen.supernaturalcraft.book.scriptorium.found",
                    source(id)).withStyle(ChatFormatting.DARK_AQUA));
        } else {
            lines.add(name(id).withStyle(ChatFormatting.GOLD));
            lines.add(kindName(s.kind()).copy().append(" · ").append(Component.translatable(
                    "screen.supernaturalcraft.book.scriptorium.tier", tier(s.tier()))).withStyle(ChatFormatting.DARK_GRAY));
            lines.add(description(id).withStyle(ChatFormatting.GRAY));
            lines.add(s.kind() == SigilKind.MODIFIER
                    ? Component.translatable("screen.supernaturalcraft.composer.multiplier", num(manaMultiplier(s))).withStyle(ChatFormatting.AQUA)
                    : Component.translatable("screen.supernaturalcraft.composer.mana", num(s.manaCost())).withStyle(ChatFormatting.AQUA));
            if (!s.reagents().isEmpty()) {
                MutableComponent r = Component.translatable("screen.supernaturalcraft.book.scriptorium.burns").append(" ");
                boolean first = true;
                for (SigilComponent.Reagent re : s.reagents()) {
                    if (!first) r.append(", ");
                    r.append(re.count() + "× ").append(re.item().value().getDescription());
                    first = false;
                }
                lines.add(r.withStyle(ChatFormatting.RED));
            }
        }
        if (action != null) lines.add(action.copy().withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        return lines;
    }
}
