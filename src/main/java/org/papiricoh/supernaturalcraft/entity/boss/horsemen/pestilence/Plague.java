package org.papiricoh.supernaturalcraft.entity.boss.horsemen.pestilence;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

/** Giving, curing and warding off the plague. The antidote's ward lives in the victim's persistent data. */
public final class Plague {

    private static final String IMMUNE_UNTIL = "supernaturalcraft:plague_immune_until";

    private Plague() {
    }

    public static int stacks(LivingEntity e) {
        MobEffectInstance i = e.getEffect(AllMobEffects.PLAGUE);
        return i == null ? 0 : PlagueStacks.stacks(i.getAmplifier());
    }

    public static boolean immune(LivingEntity e) {
        return e.getPersistentData().getLong(IMMUNE_UNTIL) > e.level().getGameTime();
    }

    /** {@code doses} more stacks (up to the cap), refreshing how long it lasts. No effect while the antidote wards. */
    public static boolean infect(LivingEntity e, int doses) {
        if (immune(e) || doses <= 0) return false;
        MobEffectInstance i = e.getEffect(AllMobEffects.PLAGUE);
        int amplifier = PlagueStacks.add(i == null ? -1 : i.getAmplifier(), doses);
        if (i != null) e.removeEffect(AllMobEffects.PLAGUE);
        e.addEffect(new MobEffectInstance(AllMobEffects.PLAGUE, PlagueStacks.DURATION_TICKS, amplifier, false, true, true));
        if (e.getHealth() > e.getMaxHealth()) e.setHealth(e.getMaxHealth());
        return true;
    }

    /** The antidote: no more plague, and none for {@code immunityTicks}. */
    public static void cure(LivingEntity e, int immunityTicks) {
        e.removeEffect(AllMobEffects.PLAGUE);
        e.getPersistentData().putLong(IMMUNE_UNTIL, e.level().getGameTime() + immunityTicks);
    }
}
