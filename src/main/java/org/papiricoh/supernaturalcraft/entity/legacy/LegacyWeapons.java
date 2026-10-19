package org.papiricoh.supernaturalcraft.entity.legacy;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/** The old rules for the old monsters (v0.17): what takes a head, what is silver, and what nothing can refuse. */
public final class LegacyWeapons {

    private LegacyWeapons() {
    }

    /** The weapon behind a melee blow (the attacker's main hand), or empty for anything else (arrows, spells, falls…). */
    public static ItemStack melee(DamageSource source) {
        if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker) return attacker.getMainHandItem();
        return ItemStack.EMPTY;
    }

    /** A blow that takes a head: melee with a {@code #beheading} item. */
    public static boolean beheads(DamageSource source) {
        return melee(source).is(AllTags.Items.BEHEADING);
    }

    /** A blow of silver: melee with a {@code #silver} item, or any melee blow under the Silver Edge sigil (v0.21). */
    public static boolean silver(DamageSource source) {
        return melee(source).is(AllTags.Items.SILVER) || silverEdged(source);
    }

    /** A melee blow from someone with Silver Edge charges left (v0.21; a charge is spent once it lands: {@code hex.CurseEvents}). */
    public static boolean silverEdged(DamageSource source) {
        return source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
                && attacker.hasEffect(org.papiricoh.supernaturalcraft.registry.AllMobEffects.SILVER_EDGE);
    }

    /** /kill, the void and the Colt: nothing escapes these (no rising again, no fleeing, no borrowed skin). */
    public static boolean absolute(DamageSource source) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || source.is(org.papiricoh.supernaturalcraft.registry.AllDamageTypes.COLT);
    }
}
