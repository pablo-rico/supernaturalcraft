package org.papiricoh.supernaturalcraft.apocalypse.seal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * What the seals' creatures share (v0.40): a GeckoLib rig of {@link SealAssets} with its loops ({@code idle}, {@code walk}) on a
 * {@code base} controller and every other clip triggerable on {@code action}, a texture variant, and the seal event that raised
 * them ({@link SealEvent#TAG}; one raised by an egg or a command belongs to none). Missing art only shows as a missing model.
 */
public abstract class SealCreature extends Monster implements GeoEntity {

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int variant = -1;

    protected SealCreature(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    /** The rig's name in {@link SealAssets}. */
    public abstract String rig();

    /** How many texture variants the rig has. */
    protected int variants() {
        return 1;
    }

    /** This one's texture variant (fixed by its id, so every client agrees). */
    public int variant() {
        if (variant < 0) variant = Math.floorMod(getUUID().hashCode(), Math.max(1, variants()));
        return variant;
    }

    /** The seal event that raised it, or 0. */
    public int sealEvent() {
        return getPersistentData().getInt(SealEvent.TAG);
    }

    /** Plays a clip of {@link SealAssets#CLIPS} once (nothing if the rig has no such clip). */
    public void play(String clip) {
        if (!level().isClientSide && SealAssets.CLIPS.getOrDefault(rig(), List.of()).contains(clip)) triggerAnim("action", clip);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = "animation." + rig() + ".";
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + "idle"), walk = RawAnimation.begin().thenLoop(pre + "walk");
        controllers.add(new AnimationController<>(this, "base", 5, state -> state.setAndContinue(state.isMoving() ? walk : idle)));
        AnimationController<SealCreature> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        List<String> loops = SealAssets.LOOPS.getOrDefault(rig(), List.of());
        for (String clip : SealAssets.CLIPS.getOrDefault(rig(), List.of())) {
            if (loops.contains(clip)) continue;
            boolean hold = clip.equals("death") || clip.equals("fade");
            action.triggerableAnim(clip, hold ? RawAnimation.begin().thenPlayAndHold(pre + clip) : RawAnimation.begin().thenPlay(pre + clip));
        }
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }
}
