package org.papiricoh.supernaturalcraft.entity.boss.metatron;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/** The Book: a great tome held open in the air. It falls on you, shuts on you, and storms you with its pages. */
public class ScribeBookEntity extends ScribeConstruct {

    public static final java.util.List<String> TRIGGERED = MetatronAnimations.BOOK_TRIGGERED;

    public ScribeBookEntity(EntityType<? extends ScribeBookEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("animation.scribe_book.idle");
        controllers.add(new AnimationController<>(this, "base", 6, state -> state.setAndContinue(idle)));
        AnimationController<ScribeBookEntity> action = new AnimationController<>(this, "action", 3, state -> software.bernie.geckolib.animation.PlayState.STOP);
        for (String name : TRIGGERED) {
            boolean hold = name.equals("close") || name.equals("slam");
            action.triggerableAnim(name, hold ? RawAnimation.begin().thenPlayAndHold("animation.scribe_book." + name)
                    : RawAnimation.begin().thenPlay("animation.scribe_book." + name));
        }
        controllers.add(action);
    }
}
