package org.papiricoh.supernaturalcraft.british.birth;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.pact.PactDialogue;
import org.papiricoh.supernaturalcraft.pact.StoryNpcEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

/**
 * Kelly Kline (v0.41): the mother of Lucifer's child, in labour on a cot in the safe house. Only ever staged by the birth
 * ({@link BirthScene}): she says nothing to anyone, her light pours out ({@code glow}) and fades ({@code fade}, held).
 */
public class KellyKlineEntity extends StoryNpcEntity {

    public KellyKlineEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected String dialogue() {
        return "kelly_kline";
    }

    @Override
    protected String prefix() {
        return "animation." + BirthAssets.RIG + ".";
    }

    @Override
    protected List<String> actions() {
        return BirthAssets.TRIGGERED;
    }

    @Override
    protected String appearClip() {
        return "none";
    }

    @Override
    protected String leaveClip() {
        return "fade";
    }

    @Override
    protected @Nullable PactDialogue.Line opening(ServerPlayer hunter) {
        return null;
    }

    @Override
    protected void flourish(ServerLevel level, boolean arriving) {
        if (!arriving) level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.8, getZ(), 30, 0.4, 0.4, 0.4, 0.02);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = prefix();
        RawAnimation labor = RawAnimation.begin().thenLoop(pre + "labor");
        controllers.add(new AnimationController<>(this, "base", 5, state -> state.setAndContinue(labor)));
        AnimationController<StoryNpcEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String clip : actions()) {
            action.triggerableAnim(clip, clip.equals("fade") ? RawAnimation.begin().thenPlayAndHold(pre + clip) : RawAnimation.begin().thenPlay(pre + clip));
        }
        controllers.add(action);
    }
}
