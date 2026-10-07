package org.papiricoh.supernaturalcraft.weapon;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import org.papiricoh.supernaturalcraft.weapon.melee.SilverMacheteItem;

/** Melee weapon behaviour that lives outside the item classes: backstabs and beheadings. */
public final class WeaponEvents {

    private WeaponEvents() {
    }

    public static void onCriticalHit(CriticalHitEvent event) {
        Player player = event.getEntity();
        if (player.getMainHandItem().getItem() instanceof SilverMacheteItem && event.getTarget() instanceof LivingEntity target
                && player.getAttackStrengthScale(0.5f) > 0.9f && SilverMacheteItem.isBehind(player, target)) {
            event.setCriticalHit(true);
            event.setDamageMultiplier(Math.max(event.getDamageMultiplier(), SilverMacheteItem.BACKSTAB_MULTIPLIER));
        }
    }

    /** The Soul Scythe drinks a share of the life it takes. */
    public static void onDamage(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        if (event.getSource().getDirectEntity() instanceof LivingEntity attacker && event.getSource().getEntity() == attacker
                && attacker.getMainHandItem().getItem() instanceof org.papiricoh.supernaturalcraft.weapon.melee.SoulScytheItem) {
            attacker.heal(event.getNewDamage() * org.papiricoh.supernaturalcraft.weapon.melee.SoulScytheItem.LIFESTEAL);
        }
    }

    /** A machete kill from behind takes the head; demons have none worth keeping, so they bleed instead. */
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(event.getSource().getDirectEntity() instanceof Player killer)
                || !(killer.getMainHandItem().getItem() instanceof SilverMacheteItem)
                || !SilverMacheteItem.isBehind(killer, dead)) {
            return;
        }
        ItemStack head = headOf(dead);
        if (head.isEmpty()) return;
        event.getDrops().add(new ItemEntity(dead.level(), dead.getX(), dead.getY() + 0.5, dead.getZ(), head));
    }

    static ItemStack headOf(LivingEntity e) {
        EntityType<?> t = e.getType();
        if (t.is(AllTags.Entities.DEMONS)) return new ItemStack(AllItems.DEMON_BLOOD.get());
        if (t == EntityType.ZOMBIE) return new ItemStack(Items.ZOMBIE_HEAD);
        if (t == EntityType.SKELETON) return new ItemStack(Items.SKELETON_SKULL);
        if (t == EntityType.WITHER_SKELETON) return new ItemStack(Items.WITHER_SKELETON_SKULL);
        if (t == EntityType.CREEPER) return new ItemStack(Items.CREEPER_HEAD);
        if (t == EntityType.PIGLIN) return new ItemStack(Items.PIGLIN_HEAD);
        if (e instanceof Player p) {
            ItemStack s = new ItemStack(Items.PLAYER_HEAD);
            s.set(DataComponents.PROFILE, new ResolvableProfile(p.getGameProfile()));
            return s;
        }
        return ItemStack.EMPTY;
    }
}
