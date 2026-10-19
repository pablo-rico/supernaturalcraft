package org.papiricoh.supernaturalcraft.bowl;

import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.bowl.spell.BloodSample;
import org.papiricoh.supernaturalcraft.bowl.spell.Taglock;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.Optional;

/**
 * The bowl's target layer (v0.21): what in a bowl says <i>whom</i> a spell is for. A blood vial ({@link BloodSample}), a vial of
 * bound blood (the same component) and a taglock ({@link Taglock}: hair, or later a possession or a familiar's theft) all
 * resolve to their owner. Possessions (a named thing someone dropped) are not read yet: only blood and taglocks.
 *
 * <p>A dose of blood poured in is the target first ({@code BowlCast#target}); otherwise the first personal item among the
 * ingredients. Taglocks and vials of bound blood are {@link #isTargetOnly target-only}: a recipe ignores them when matching
 * unless it asks for one.
 */
public final class PersonalComponent {

    private PersonalComponent() {
    }

    /** Whose it is, and how it came to be theirs ({@link Taglock#kind()}). */
    public static Optional<Taglock> resolve(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        Taglock tag = stack.get(AllDataComponents.TAGLOCK.get());
        if (tag != null) return Optional.of(tag);
        BloodSample blood = stack.get(AllDataComponents.BLOOD_SAMPLE.get());
        if (blood != null && (stack.is(AllItems.BLOOD_VIAL.get()) || stack.is(AllItems.BOUND_BLOOD.get()))) return Optional.of(Taglock.of(blood));
        return Optional.empty();
    }

    /** The target a dose of blood names, if it has an owner. */
    public static Optional<Taglock> resolve(Dose dose) {
        if (dose.kind() != BowlLiquid.BLOOD || dose.owner().isEmpty()) return Optional.empty();
        return Optional.of(new Taglock(dose.owner().get(), dose.ownerName().orElse("?"), Taglock.BLOOD));
    }

    /** Whether {@code stack} only names a target (a taglock, a vial of bound blood): recipes leave it out of their match. */
    public static boolean isTargetOnly(ItemStack stack) {
        return stack.is(AllItems.TAGLOCK.get()) || stack.is(AllItems.BOUND_BLOOD.get());
    }
}
