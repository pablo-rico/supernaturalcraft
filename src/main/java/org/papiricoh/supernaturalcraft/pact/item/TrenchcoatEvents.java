package org.papiricoh.supernaturalcraft.pact.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.leviathan.BoraxSensitive;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

/**
 * Castiel's trench coat (v0.39): left behind when the Leviathans dragged him under, it remembers them. Its wearer takes
 * {@link #LEVIATHAN_SHARE} of what a Leviathan (anything borax burns) deals, and cannot be sedated by their food.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class TrenchcoatEvents {

    public static final float LEVIATHAN_SHARE = 0.6f;

    private TrenchcoatEvents() {
    }

    public static boolean wearing(LivingEntity e) {
        return e.getItemBySlot(EquipmentSlot.CHEST).is(AllItems.CASTIELS_TRENCHCOAT.get());
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide || !wearing(e)) return;
        if (event.getSource().getEntity() instanceof BoraxSensitive) event.setAmount(event.getAmount() * LEVIATHAN_SHARE);
    }

    @SubscribeEvent
    public static void onTooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        if (event.getItemStack().is(AllItems.CASTIELS_TRENCHCOAT.get())) {
            event.getToolTip().add(net.minecraft.network.chat.Component.translatable("tooltip.supernaturalcraft.castiels_trenchcoat")
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
        }
    }

    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().is(AllMobEffects.SEDATED) && wearing(event.getEntity())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
}
