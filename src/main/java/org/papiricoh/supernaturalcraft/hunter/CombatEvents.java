package org.papiricoh.supernaturalcraft.hunter;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/** Weapon bonuses against demons, and the guaranteed blood harvest of Ruby's knife. */
public class CombatEvents {

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!org.papiricoh.supernaturalcraft.allegiance.Kin.isDemon(target)) return;
        if (event.getSource().getDirectEntity() instanceof LivingEntity attacker
                && attacker.getMainHandItem().getItem() instanceof DemonBane bane) {
            event.setAmount(event.getAmount() * bane.demonDamageMultiplier());
        }
    }

    public static void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (!dead.getType().is(AllTags.Entities.DEMONS)) return;
        Entity killer = event.getSource().getDirectEntity();
        if (killer instanceof LivingEntity attacker
                && attacker.getMainHandItem().getItem() instanceof DemonBane bane && bane.harvestsBlood()) {
            boolean hasBlood = event.getDrops().stream().anyMatch(e -> e.getItem().is(AllItems.DEMON_BLOOD.get()));
            if (!hasBlood) {
                event.getDrops().add(new ItemEntity(dead.level(), dead.getX(), dead.getY(), dead.getZ(),
                        new ItemStack(AllItems.DEMON_BLOOD.get())));
            }
        }
    }
}
