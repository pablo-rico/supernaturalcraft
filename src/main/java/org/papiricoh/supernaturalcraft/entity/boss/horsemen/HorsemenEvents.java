package org.papiricoh.supernaturalcraft.entity.boss.horsemen;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.famine.FamineEntity;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.pestilence.Plague;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.pestilence.PlagueStacks;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

/** The Horsemen's rules that live outside their fights: food eaten near Famine feeds him; the plague stops natural healing. */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class HorsemenEvents {

    private HorsemenEvents() {
    }

    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player eater) || eater.level().isClientSide) return;
        FoodProperties food = event.getItem().getFoodProperties(eater);
        if (food != null) ateNear(eater, food.nutrition());
    }

    /** {@code eater} just ate {@code nutrition} worth: any Famine close by takes it for himself. */
    public static boolean ateNear(Player eater, int nutrition) {
        boolean fed = false;
        for (FamineEntity famine : eater.level().getEntitiesOfClass(FamineEntity.class, new AABB(eater.blockPosition())
                .inflate(FamineEntity.FEED_RANGE))) {
            if (!famine.isAlive() || famine.distanceTo(eater) > FamineEntity.FEED_RANGE) continue;
            famine.fedBy(eater, nutrition);
            fed = true;
        }
        return fed;
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        LivingEntity e = event.getEntity();
        if (e.hasEffect(AllMobEffects.PLAGUE) && !PlagueStacks.healAllowed(event.getAmount())) event.setCanceled(true);
    }

    static boolean plagued(LivingEntity e) {
        return Plague.stacks(e) > 0;
    }
}
