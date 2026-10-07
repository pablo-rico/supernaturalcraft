package org.papiricoh.supernaturalcraft.weapon;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/** What counts as holy harm, for every boss that shrugs off the mundane. */
public final class Holy {

    private Holy() {
    }

    /** A holy damage type, or a melee blow with a holy weapon or a SANCTITY-graved one. */
    public static boolean isHoly(DamageSource source) {
        if (source.is(AllTags.DamageTypes.HOLY)) return true;
        return source.getDirectEntity() instanceof LivingEntity attacker && source.getDirectEntity() == source.getEntity()
                && isHolyWeapon(attacker.getMainHandItem());
    }

    public static boolean isHolyWeapon(ItemStack stack) {
        if (stack.is(AllTags.Items.HOLY_WEAPONS)) return true;
        return stack.getOrDefault(AllDataComponents.RUNES, RuneSet.EMPTY).has(Rune.SANCTITY);
    }
}
