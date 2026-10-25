package org.papiricoh.supernaturalcraft.weapon.melee;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import org.papiricoh.supernaturalcraft.hunter.HunterBladeItem;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * An angelic blade drawn in 3D ({@link AngelicBladeAssets}): a {@link HunterBladeItem} (demon bane) whose GeckoLib model
 * idles dark, glows while {@link AngelicBladeAssets#CHARGE charging} and blinks on a {@link AngelicBladeAssets#FLASH flash},
 * both triggered from the server on the exact stack.
 */
public abstract class AngelicBladeItem extends HunterBladeItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String id;

    protected AngelicBladeItem(String id, Tier tier, float demonMultiplier, boolean harvestsBlood, Properties properties) {
        super(tier, demonMultiplier, harvestsBlood, properties);
        this.id = id;
        GeoItem.registerSyncedAnimatable(this);
    }

    /** Plays {@code clip} on this exact stack for everyone watching. */
    public void play(LivingEntity holder, ItemStack stack, String clip) {
        if (holder.level() instanceof ServerLevel level) {
            triggerAnim(holder, GeoItem.getOrAssignId(stack, level), "main", clip);
        }
    }

    public void stop(LivingEntity holder, ItemStack stack, String clip) {
        if (holder.level() instanceof ServerLevel level) {
            stopTriggeredAnim(holder, GeoItem.getOrAssignId(stack, level), "main", clip);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop(AngelicBladeAssets.clip(id, AngelicBladeAssets.IDLE));
        AnimationController<AngelicBladeItem> main = new AnimationController<>(this, "main", 2, s -> s.setAndContinue(idle));
        main.triggerableAnim(AngelicBladeAssets.CHARGE, RawAnimation.begin().thenLoop(AngelicBladeAssets.clip(id, AngelicBladeAssets.CHARGE)));
        main.triggerableAnim(AngelicBladeAssets.FLASH, RawAnimation.begin().thenPlay(AngelicBladeAssets.clip(id, AngelicBladeAssets.FLASH)));
        controllers.add(main);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
