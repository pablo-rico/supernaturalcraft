package org.papiricoh.supernaturalcraft.apocalypse.blood;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Demon blood withdrawal (v0.40): the marker {@code BloodEvents} keeps on a drinker whose body has run dry while the
 * dependency holds. What it does (weakness, slowness, the visions) is applied by {@code BloodEvents} from the faction's
 * curve in {@link BloodRules}; the effect itself is the icon and the "why".
 */
public class WithdrawalEffect extends MobEffect {

    public WithdrawalEffect() {
        super(MobEffectCategory.HARMFUL, 0x5A1010);
    }
}
