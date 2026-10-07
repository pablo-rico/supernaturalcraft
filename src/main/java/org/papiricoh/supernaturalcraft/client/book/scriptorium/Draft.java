package org.papiricoh.supernaturalcraft.client.book.scriptorium;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The spell on the composing table: a mutable {@link Spell} the reader builds sigil by sigil. */
final class Draft {

    @Nullable ResourceLocation form;
    final List<ResourceLocation> effects = new ArrayList<>();
    final List<ResourceLocation> modifiers = new ArrayList<>();
    String name = "";

    static Draft of(Spell spell) {
        Draft d = new Draft();
        d.load(spell);
        return d;
    }

    void load(Spell spell) {
        form = spell.form().orElse(null);
        effects.clear();
        effects.addAll(spell.effects());
        modifiers.clear();
        modifiers.addAll(spell.modifiers());
        name = spell.name();
    }

    void clear() {
        load(Spell.EMPTY);
    }

    /**
     * Puts a sigil in the slot its kind belongs to: the form replaces the form, effects (no repeats)
     * and modifiers fill up to their limits.
     *
     * @return whether the draft changed
     */
    boolean add(ResourceLocation id, SigilComponent sigil) {
        return switch (sigil.kind()) {
            case FORM -> {
                if (id.equals(form)) yield false;
                form = id;
                yield true;
            }
            case EFFECT -> {
                if (effects.contains(id) || effects.size() >= Spell.MAX_EFFECTS) yield false;
                effects.add(id);
                yield true;
            }
            case MODIFIER -> {
                if (modifiers.size() >= Spell.MAX_MODIFIERS) yield false;
                modifiers.add(id);
                yield true;
            }
        };
    }

    boolean contains(ResourceLocation id) {
        return id.equals(form) || effects.contains(id) || modifiers.contains(id);
    }

    Spell toSpell() {
        return new Spell(Optional.ofNullable(form), List.copyOf(effects), List.copyOf(modifiers), name.trim());
    }

    /** Every sigil in drawing order: form, effects, modifiers. */
    List<ResourceLocation> all() {
        List<ResourceLocation> all = new ArrayList<>();
        if (form != null) all.add(form);
        all.addAll(effects);
        all.addAll(modifiers);
        return all;
    }
}
