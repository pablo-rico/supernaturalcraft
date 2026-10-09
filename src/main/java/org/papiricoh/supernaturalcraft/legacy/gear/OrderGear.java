package org.papiricoh.supernaturalcraft.legacy.gear;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import org.papiricoh.supernaturalcraft.legacy.LegacyOrder;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

/** What the order's gear does from tick to tick (v0.17): the spectacles keep Second Sight on their wearer. */
public final class OrderGear {

    /** Second Sight from the spectacles lasts this long and is refreshed every second while they are worn. */
    public static final int SIGHT_TICKS = 60;

    private OrderGear() {
    }

    /** Once a second per player. */
    public static void second(ServerPlayer player) {
        if (LegacyOrder.wears(player, AllItems.SPELLWRIGHTS_SPECTACLES.get())) {
            MobEffectInstance now = player.getEffect(AllMobEffects.SECOND_SIGHT);
            if (now == null || now.getDuration() < SIGHT_TICKS - 20) {
                player.addEffect(new MobEffectInstance(AllMobEffects.SECOND_SIGHT, SIGHT_TICKS, 0, true, false, true));
            }
        }
    }

    /** Whether {@code player} sees through disguises: the spectacles, or Second Sight however it came. */
    public static boolean seesTrue(net.minecraft.world.entity.player.Player player) {
        return player.hasEffect(AllMobEffects.SECOND_SIGHT) || LegacyOrder.wears(player, AllItems.SPELLWRIGHTS_SPECTACLES.get());
    }
}
