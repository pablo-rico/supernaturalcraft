package org.papiricoh.supernaturalcraft.weapon;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.papiricoh.supernaturalcraft.balance.WeaponDamage;

/** Scales the weapon's own damage while preserving speed, reach and equipment slots. */
public final class WeaponAttributes {
    private WeaponAttributes() {
    }

    public static ItemAttributeModifiers scaled(ItemAttributeModifiers original) {
        var builder = ItemAttributeModifiers.builder();
        for (var entry : original.modifiers()) {
            var modifier = entry.modifier();
            if (entry.attribute().equals(Attributes.ATTACK_DAMAGE)) {
                modifier = new AttributeModifier(modifier.id(), modifier.amount() * WeaponDamage.WEAPON_DAMAGE_SCALE,
                        modifier.operation());
            }
            builder.add(entry.attribute(), modifier, entry.slot());
        }
        return builder.build();
    }
}
