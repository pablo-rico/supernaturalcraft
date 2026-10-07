package org.papiricoh.supernaturalcraft.weapon.melee;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * A sword drawn with a GeckoLib model ("geo/item/&lt;id&gt;.geo.json"). It loops
 * "animation.&lt;id&gt;.idle" and can trigger the named one-shots from the server.
 */
public abstract class GeoSwordItem extends SwordItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String id;
    private final List<String> loops, triggers;

    protected GeoSwordItem(String id, Tier tier, Properties properties, List<String> loops, List<String> triggers) {
        super(tier, properties);
        this.id = id;
        this.loops = loops;
        this.triggers = triggers;
        GeoItem.registerSyncedAnimatable(this);
    }

    /** Plays a triggered animation on this exact stack for everyone watching. */
    public void play(LivingEntity holder, ItemStack stack, String name) {
        if (holder.level() instanceof ServerLevel level) {
            triggerAnim(holder, GeoItem.getOrAssignId(stack, level), "main", name);
        }
    }

    public void stopPlaying(LivingEntity holder, ItemStack stack, String name) {
        if (holder.level() instanceof ServerLevel level) {
            stopTriggeredAnim(holder, GeoItem.getOrAssignId(stack, level), "main", name);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("animation." + id + ".idle");
        AnimationController<GeoSwordItem> main = new AnimationController<>(this, "main", 3, s -> s.setAndContinue(idle));
        for (String l : loops) main.triggerableAnim(l, RawAnimation.begin().thenLoop("animation." + id + "." + l));
        for (String t : triggers) main.triggerableAnim(t, RawAnimation.begin().thenPlay("animation." + id + "." + t));
        controllers.add(main);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
