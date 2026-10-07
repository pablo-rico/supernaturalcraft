package org.papiricoh.supernaturalcraft.entity.boss.metatron;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * The Hand of God: a marble hand veined with light, holding a quill, reaching down in its robe's sleeve out
 * of a door in Heaven ringed by ofanim. The entity stands at the quill's nib; the rest rises some ten
 * blocks above it (drawn at {@link #SCALE}).
 */
public class ScribeHandEntity extends ScribeConstruct {

    public static final java.util.List<String> TRIGGERED = MetatronAnimations.HAND_TRIGGERED;
    /** Model scale in the renderer. */
    public static final float SCALE = 0.85f;
    /** Blocks from the nib up to the door in Heaven. */
    public static final double PORTAL_HEIGHT = 168 * SCALE / 16;
    /** With the palm turned flat for a slam: how far below the entity its palm lies, and how far ahead its middle reaches. */
    public static final double SLAM_DROP = 101 * SCALE / 16, SLAM_REACH = 1.5;
    /** How far below the ground the nib goes so the fingers brush it in a sweep. */
    public static final double SWEEP_SINK = 1.5;

    public ScribeHandEntity(EntityType<? extends ScribeHandEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount == 3) triggerAnim("action", "appear");
    }

    /** The door in Heaven, its light falling, and ink at the nib. */
    @Override
    protected void clientTick() {
        double py = getY() + PORTAL_HEIGHT;
        for (int i = 0; i < 2; i++) {
            double a = random.nextDouble() * Math.PI * 2, r = 1.2 + random.nextDouble() * 1.8;
            level().addParticle(org.papiricoh.supernaturalcraft.registry.AllParticles.GRACE.get(),
                    getX() + Math.cos(a) * r, py - 0.4 + random.nextDouble() * 0.6, getZ() + Math.sin(a) * r, 0, -0.04, 0);
        }
        if (random.nextFloat() < 0.25f) {
            double a = random.nextDouble() * Math.PI * 2;
            level().addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    getX() + Math.cos(a) * 2.6, py + 0.2, getZ() + Math.sin(a) * 2.6, 0, -0.08, 0);
        }
        if (random.nextFloat() < 0.15f) {
            level().addParticle(org.papiricoh.supernaturalcraft.registry.AllParticles.INK.get(), getX(), getY() + 0.1, getZ(), 0, -0.02, 0);
        }
    }

    /** Ten blocks tall: drawn whenever any of it is in view, not only the nib. */
    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        return new net.minecraft.world.phys.AABB(getX() - 4.5, getY() - 1, getZ() - 4.5, getX() + 4.5, getY() + PORTAL_HEIGHT + 3.5, getZ() + 4.5);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("animation.scribe_hand.idle");
        controllers.add(new AnimationController<>(this, "base", 6, state -> state.setAndContinue(idle)));
        AnimationController<ScribeHandEntity> action = new AnimationController<>(this, "action", 3, state -> software.bernie.geckolib.animation.PlayState.STOP);
        for (String name : TRIGGERED) action.triggerableAnim(name, RawAnimation.begin().thenPlay("animation.scribe_hand." + name));
        controllers.add(action);
    }
}
