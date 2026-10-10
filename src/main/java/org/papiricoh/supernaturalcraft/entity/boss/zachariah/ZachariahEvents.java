package org.papiricoh.supernaturalcraft.entity.boss.zachariah;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.BossDamage;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.reward.heaven.HeavensSealItem;
import org.papiricoh.supernaturalcraft.reward.heaven.ZachariahsBladeItem;

/**
 * The rules of Zachariah's paperwork outside his own code (v0.18): DENIED ({@code PAPERWORK}) stops all healing; a target filed by
 * his Blade takes {@link ZachariahsBladeItem#FILED_BONUS} more from every blow; Heaven's Seal halves a great enemy's first blow in
 * a while.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class ZachariahEvents {

    private ZachariahEvents() {
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (event.getEntity().hasEffect(AllMobEffects.PAPERWORK)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        if (ZachariahsBladeItem.filed(victim) && event.getSource().getEntity() instanceof LivingEntity) {
            event.setAmount(event.getAmount() * (1f + ZachariahsBladeItem.FILED_BONUS));
        }
        if (victim instanceof Player p && event.getSource().getEntity() != null && BossDamage.isBoss(event.getSource().getEntity())
                && HeavensSealItem.blunts(p, p.level().getGameTime())) {
            event.setAmount(event.getAmount() * HeavensSealItem.KEEP);
        }
    }
}
