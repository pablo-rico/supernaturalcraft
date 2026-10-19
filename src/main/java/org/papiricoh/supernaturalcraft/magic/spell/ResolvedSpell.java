package org.papiricoh.supernaturalcraft.magic.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.magic.MagicAssets;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.magic.school.School;

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

    /** The longest cooldown among its sigils, times every modifier's {@code cooldown_multiplier} (v0.21: Quicken). */
    public int cooldown() {
        int cd = form.cooldown();
        for (Part<?> p : effects) cd = Math.max(cd, p.sigil().cooldown());
        for (Part<?> p : modifiers) cd = Math.max(cd, p.sigil().cooldown());
        float scale = 1f;
        for (Part<?> p : modifiers) scale *= p.sigil().param("cooldown_multiplier", 1.0f);
        return Math.max(0, Math.round(cd * scale));
    }

    /** How long a Linger modifier's zone lasts (v0.21; 0 without one): the longest {@code linger_ticks} among the modifiers. */
    public int lingerTicks() {
        int t = 0;
        for (Part<?> p : modifiers) t = Math.max(t, Math.round(p.sigil().param("linger_ticks", 0f)));
        return t;
    }

    public List<SigilComponent.Reagent> reagents() {
        List<SigilComponent.Reagent> all = new ArrayList<>(form.reagents());
        effects.forEach(p -> all.addAll(p.sigil().reagents()));
        modifiers.forEach(p -> all.addAll(p.sigil().reagents()));
        return all;
    }

    /** The spell's school (v0.21): its first effect's ({@link School#NONE} if that has none). */
    public School school() {
        return effects.isEmpty() ? School.NONE : effects.getFirst().sigil().school();
    }

    /** The highest tier among its sigils (1-3, 4 for a Master's sigil). */
    public int tier() {
        int t = form.tier();
        for (Part<?> p : effects) t = Math.max(t, p.sigil().tier());
        for (Part<?> p : modifiers) t = Math.max(t, p.sigil().tier());
        return t;
    }

    /** The {@code MagicAssets.IMPACTS} index of an effect's impact, by its behaviour (generated formulas share their base's). */
    public static int impactIndex(SigilComponent effect) {
        return MagicAssets.impactIndex(effect.behavior().getPath());
    }

    /** The impact index of the spell's first effect, or -1. */
    public int impactIndex() {
        return effects.isEmpty() ? -1 : impactIndex(effects.getFirst().sigil());
    }

    public boolean allBeneficial() {
        return effects.stream().allMatch(p -> p.behavior().beneficial());
    }

    /** Applies every effect that is meant for {@code target}: helpful ones to friends, harmful ones to the rest. */
    public boolean applyTo(SpellContext ctx, Entity target) {
        boolean friend = isFriend(ctx.caster, target);
        boolean any = false;
        SigilComponent first = null;
        for (Part<SpellBehavior.Effect> p : effects) {
            if (p.behavior().beneficial() == friend) {
                boolean hit = p.behavior().applyToEntity(ctx, p.sigil(), target);
                if (hit && first == null) first = p.sigil();
                any |= hit;
            }
        }
        if (first != null) {
            impact(ctx, target.position().add(0, target.getBbHeight() / 2, 0), target, first);
            linger(ctx, target.position());
        }
        return any;
    }

    public boolean applyToBlock(SpellContext ctx, BlockPos pos, Direction face) {
        boolean any = false;
        SigilComponent first = null;
        for (Part<SpellBehavior.Effect> p : effects) {
            boolean hit = p.behavior().applyToBlock(ctx, p.sigil(), pos, face);
            if (hit && first == null) first = p.sigil();
            any |= hit;
        }
        if (first != null) {
            impact(ctx, Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5), null, first);
            linger(ctx, Vec3.atBottomCenterOf(pos.relative(face)));
        }
        return any;
    }

    /** A lingering zone's potency, as a share of the cast's. */
    public static final float LINGER_POTENCY = 0.5f;

    /**
     * The Linger modifier (v0.21): the first place this cast takes hold gets a zone that re-applies the spell every second for
     * {@link SpellContext#lingerTicks} at {@link #LINGER_POTENCY} (once per cast; the zone's own pulses never linger again).
     */
    private void linger(SpellContext ctx, Vec3 at) {
        if (ctx.lingerTicks <= 0 || ctx.lingered) return;
        ctx.lingered = true;
        org.papiricoh.supernaturalcraft.entity.magic.WardEntity zone = org.papiricoh.supernaturalcraft.entity.magic.WardEntity.lingering(
                ctx.level, ctx.caster, org.papiricoh.supernaturalcraft.entity.magic.SpellCarrier.of(spell, ctx)
                        .withPotency(ctx.potency * LINGER_POTENCY).withArea(Math.max(1.5f, ctx.area)), ctx.lingerTicks);
        zone.setPos(at.x, at.y, at.z);
        zone.setSchool(school());
        ctx.level.addFreshEntity(zone);
        MagicFx.ward(ctx.level, school(), zone, at, zone.getRadius(), org.papiricoh.supernaturalcraft.network.MagicFxPayload.WARD_RAISED,
                org.papiricoh.supernaturalcraft.magic.spell.form.SpellForms.WARD_PULSE_TICKS);
    }

    /** One IMPACT effect (v0.21) where {@code effect} took hold, within the cast's {@link SpellContext#impactBudget}. */
    private void impact(SpellContext ctx, Vec3 at, @Nullable Entity hit, SigilComponent effect) {
        if (ctx.impactBudget <= 0) return;
        ctx.impactBudget--;
        School school = effect.school().isNone() ? school() : effect.school();
        // An effect may ask for a longer or wider impact of its own (v0.21: Judgement's column, {@code impact_ticks}/{@code impact_radius}).
        float radius = Math.max(ctx.area, effect.param("impact_radius", 0f));
        int ticks = Math.round(effect.param("impact_ticks", IMPACT_TICKS));
        MagicFx.impact(ctx.level, school, at, ctx.caster.getEyePosition(), impactIndex(effect), radius, hit, ticks);
    }

    /** How long an impact effect plays. */
    public static final int IMPACT_TICKS = 16;

    public static boolean isFriend(LivingEntity caster, Entity target) {
        if (target == caster) return true;
        if (target instanceof Player && caster instanceof Player) return true;
        if (target instanceof OwnableEntity owned && owned.getOwner() == caster) return true;
        return caster.isAlliedTo(target);
    }
}
