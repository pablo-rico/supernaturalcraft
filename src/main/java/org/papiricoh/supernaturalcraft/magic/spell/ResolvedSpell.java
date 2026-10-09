package org.papiricoh.supernaturalcraft.magic.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** A {@link Spell} with its sigils looked up and their behaviours attached. */
public record ResolvedSpell(Spell spell, SigilComponent form, SpellBehavior.Form formBehavior,
                            List<Part<SpellBehavior.Effect>> effects, List<Part<SpellBehavior.Modifier>> modifiers) {

    public record Part<B extends SpellBehavior>(ResourceLocation id, SigilComponent sigil, B behavior) {
    }

    /** Null if the spell is incomplete or names a sigil that no longer exists or is the wrong kind. */
    public static @Nullable ResolvedSpell resolve(RegistryAccess access, Spell spell) {
        return resolve(access, null, spell);
    }

    /** As {@link #resolve(RegistryAccess, Spell)}, also finding the caster's generated formulas ({@link SigilLookup}). */
    public static @Nullable ResolvedSpell resolve(RegistryAccess access, @Nullable org.papiricoh.supernaturalcraft.legacy.Archive archive, Spell spell) {
        if (!spell.isComplete()) return null;
        java.util.function.Function<ResourceLocation, SigilComponent> sigils = id -> SigilLookup.get(access, archive, id);
        SigilComponent form = sigils.apply(spell.form().get());
        if (form == null || form.kind() != SigilKind.FORM
                || !(SpellBehaviors.get(form.behavior()) instanceof SpellBehavior.Form formBehavior)) {
            return null;
        }
        List<Part<SpellBehavior.Effect>> effects = new ArrayList<>();
        for (ResourceLocation id : spell.effects()) {
            SigilComponent s = sigils.apply(id);
            if (s == null || s.kind() != SigilKind.EFFECT || !(SpellBehaviors.get(s.behavior()) instanceof SpellBehavior.Effect b)) {
                return null;
            }
            effects.add(new Part<>(id, s, b));
        }
        List<Part<SpellBehavior.Modifier>> modifiers = new ArrayList<>();
        for (ResourceLocation id : spell.modifiers()) {
            SigilComponent s = sigils.apply(id);
            if (s == null || s.kind() != SigilKind.MODIFIER || !(SpellBehaviors.get(s.behavior()) instanceof SpellBehavior.Modifier b)) {
                return null;
            }
            modifiers.add(new Part<>(id, s, b));
        }
        return new ResolvedSpell(spell, form, formBehavior, List.copyOf(effects), List.copyOf(modifiers));
    }

    public float manaCost(float discount) {
        return SpellCost.mana(form.manaCost(),
                effects.stream().map(p -> p.sigil().manaCost()).toList(),
                modifiers.stream().map(p -> p.sigil().param("mana_multiplier", 1.0f)).toList(),
                discount);
    }

    public int cooldown() {
        int cd = form.cooldown();
        for (Part<?> p : effects) cd = Math.max(cd, p.sigil().cooldown());
        for (Part<?> p : modifiers) cd = Math.max(cd, p.sigil().cooldown());
        return cd;
    }

    public List<SigilComponent.Reagent> reagents() {
        List<SigilComponent.Reagent> all = new ArrayList<>(form.reagents());
        effects.forEach(p -> all.addAll(p.sigil().reagents()));
        modifiers.forEach(p -> all.addAll(p.sigil().reagents()));
        return all;
    }

    public boolean allBeneficial() {
        return effects.stream().allMatch(p -> p.behavior().beneficial());
    }

    /** Applies every effect that is meant for {@code target}: helpful ones to friends, harmful ones to the rest. */
    public boolean applyTo(SpellContext ctx, Entity target) {
        boolean friend = isFriend(ctx.caster, target);
        boolean any = false;
        for (Part<SpellBehavior.Effect> p : effects) {
            if (p.behavior().beneficial() == friend) {
                any |= p.behavior().applyToEntity(ctx, p.sigil(), target);
            }
        }
        return any;
    }

    public boolean applyToBlock(SpellContext ctx, BlockPos pos, Direction face) {
        boolean any = false;
        for (Part<SpellBehavior.Effect> p : effects) {
            any |= p.behavior().applyToBlock(ctx, p.sigil(), pos, face);
        }
        return any;
    }

    public static boolean isFriend(LivingEntity caster, Entity target) {
        if (target == caster) return true;
        if (target instanceof Player && caster instanceof Player) return true;
        if (target instanceof OwnableEntity owned && owned.getOwner() == caster) return true;
        return caster.isAlliedTo(target);
    }
}
