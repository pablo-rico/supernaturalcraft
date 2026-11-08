package org.papiricoh.supernaturalcraft.hands.mob;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * What the slice's creatures share (v0.42): a GeckoLib rig of {@code S11Assets} with its loops on the {@code base} controller
 * (chosen by {@link #loop}) and its one-shot clips on {@code action} ({@link #play}; {@code death} holds its last frame).
 */
public abstract class HandsMob extends Monster implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected HandsMob(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    /** The rig (files and clip names). */
    protected abstract S11Assets.Rig rig();

    /** The loop to play now, one of the rig's {@code loops}. */
    protected abstract String loop(AnimationState<HandsMob> state);

    /** Plays a one-shot clip of the rig (server side: it reaches the trackers). */
    public void play(String clip) {
        if (!level().isClientSide) triggerAnim("action", clip);
    }

    @Override
    public void die(DamageSource source) {
        play("death");
        super.die(source);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = "animation." + rig().rig() + ".";
        java.util.Map<String, RawAnimation> loops = new java.util.HashMap<>();
        for (String l : rig().loops()) loops.put(l, RawAnimation.begin().thenLoop(pre + l));
        controllers.add(new AnimationController<HandsMob>(this, "base", 5, state -> {
            if (isDeadOrDying()) return PlayState.STOP;
            RawAnimation a = loops.get(loop(state));
            return a == null ? PlayState.STOP : state.setAndContinue(a);
        }));
        AnimationController<HandsMob> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String clip : rig().triggered()) {
            action.triggerableAnim(clip, clip.equals("death") ? RawAnimation.begin().thenPlayAndHold(pre + clip) : RawAnimation.begin().thenPlay(pre + clip));
        }
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
