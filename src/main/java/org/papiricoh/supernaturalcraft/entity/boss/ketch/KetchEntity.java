package org.papiricoh.supernaturalcraft.entity.boss.ketch;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.british.British;
import org.papiricoh.supernaturalcraft.british.BritishFxPayload;
import org.papiricoh.supernaturalcraft.british.BritishOrder;
import org.papiricoh.supernaturalcraft.british.BritishRegistry;
import org.papiricoh.supernaturalcraft.british.BritishSpoils;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;
import java.util.function.Supplier;

/**
 * Arthur Ketch (v0.41, season 12): the British Men of Letters' operative, sent after a hunter who turned Dr. Hess down or was
 * written off. He catches them at night wherever they are ({@link KetchSummoning#ambush}), in a small arena of his own that
 * leaves other fights alone. Pistol, knife, hex grenades and smoke: see {@link KetchBalance}, {@link KetchAttacks}. Beaten, he
 * leaves every hunter the British arsenal, his bust and his dossier on the compound.
 */
public class KetchEntity extends LuciferEntity {

    private static final int TAUNTS = 5;
    private int taunt;

    public KetchEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 400;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, VANILLA_BASE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    /** Test and command hook: the long death begins now. */
    public void defeatNow() {
        beginDying();
    }

    /** One of his polite lines to the hunters near (they come round in order). */
    public void taunt() {
        if (!(level() instanceof ServerLevel level)) return;
        String key = "message.supernaturalcraft.ketch.taunt." + (taunt++ % TAUNTS);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 48 * 48) p.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
        }
    }

    void fx(byte kind, int arg, int arg2, Vec3 at, int duration) {
        if (level() instanceof ServerLevel level) BritishFxPayload.send(level, getId(), kind, arg, arg2, at, duration);
    }

    // --- his numbers --------------------------------------------------------------------------------------------------------

    @Override
    public int maxPhase() {
        return KetchBalance.PHASES;
    }

    @Override
    protected float threshold(int phase) {
        return KetchBalance.threshold(phase);
    }

    @Override
    public boolean isAerialPhase() {
        return false;
    }

    @Override
    public float scale(int phase) {
        return 1f;
    }

    @Override
    protected int emergeTicks() {
        return KetchBalance.EMERGE;
    }

    @Override
    protected int deathTicks() {
        return KetchBalance.DEATH;
    }

    @Override
    protected int transitionTicks(int to) {
        return KetchBalance.TRANSITION;
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return KetchAttacks.pool(phase);
    }

    @Override
    protected int baseGap(int phase) {
        return KetchBalance.attackGap(phase);
    }

    @Override
    public @Nullable Supplier<BossAttack<LuciferEntity>> forcedAttack(LivingEntity target) {
        if (distanceToSqr(target) > KetchBalance.CHASE_DISTANCE * KetchBalance.CHASE_DISTANCE || noSightTicks > KetchBalance.CHASE_NO_SIGHT) {
            noSightTicks = 0;
            return KetchAttacks.Vanish::new;
        }
        return null;
    }

    @Override
    protected String animationPrefix() {
        return "animation." + KetchAssets.RIG + ".";
    }

    @Override
    protected List<String> triggeredAnimations() {
        return KetchAssets.TRIGGERED;
    }

    @Override
    protected String bossBarKey(int phase) {
        return "entity.supernaturalcraft.ketch.bar.phase" + phase;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return phase >= KetchBalance.PHASES ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.WHITE;
    }

    @Override
    protected Component bossBarName(int phase) {
        return Component.translatable(bossBarKey(phase)).withStyle(phase >= KetchBalance.PHASES ? ChatFormatting.RED : ChatFormatting.GRAY);
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return ParticleTypes.SMOKE;
    }

    @Override
    protected void clientPhaseParticles(int phase) {
    }

    @Override
    protected SoundEvent emergeSound() {
        return BritishRegistry.sound("ketch.smoke");
    }

    @Override
    protected SoundEvent roarSound() {
        return BritishRegistry.sound("ketch.shot");
    }

    @Override
    protected SoundEvent transformSound() {
        return BritishRegistry.sound("ketch.smoke");
    }

    @Override
    protected SoundEvent ambientBossSound() {
        return BritishRegistry.sound("ketch.knife");
    }

    @Override
    protected SoundEvent dyingSound() {
        return BritishRegistry.sound("ketch.death");
    }

    @Override
    protected SoundEvent deflectSound() {
        return BritishRegistry.sound("ketch.hurt");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return BritishRegistry.sound("ketch.hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return BritishRegistry.sound("ketch.death");
    }

    @Override
    protected void clientEmergenceParticles() {
        level().addParticle(ParticleTypes.LARGE_SMOKE, getRandomX(0.8), getY() + random.nextDouble() * 1.6, getRandomZ(0.8), 0, 0.02, 0);
    }

    @Override
    protected void dyingParticles(ServerLevel level, boolean last) {
        if (last) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 40, 0.4, 0.8, 0.4, 0.04);
            return;
        }
        if (random.nextInt(3) == 0) level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1, getZ(), 2, 0.2, 0.4, 0.2, 0.01);
    }

    // --- the fight's shape --------------------------------------------------------------------------------------------------

    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        return LuciferSummoning.openArena(level, blockPosition(), KetchSummoning.AMBUSH_RADIUS, ArenaTheme.AMBUSH, false);
    }

    @Override
    protected void applyTerrain(ServerLevel level, ArenaController arena, int phase) {
    }

    @Override
    protected void playEmergence() {
        fx(BritishFxPayload.TITLE, BritishFxPayload.WHO_KETCH, 1, position(), 70);
        taunt();
    }

    @Override
    protected void playTransition(int to) {
        fx(BritishFxPayload.TITLE, BritishFxPayload.WHO_KETCH, to, position(), 60);
        fx(BritishFxPayload.CLOUD, 30, BritishFxPayload.SMOKE, position(), 30);
        triggerAnim("action", "taunt");
    }

    @Override
    protected void playDeath() {
    }

    @Override
    protected void onTransitionStart(int to) {
    }

    @Override
    protected void tickTransitionMotion(int elapsed, boolean last) {
        setDeltaMovement(0, getDeltaMovement().y, 0);
    }

    @Override
    protected void tickDyingMotion(int elapsed) {
        setDeltaMovement(0, getDeltaMovement().y, 0);
    }

    @Override
    protected void leaveBehind(ServerLevel level, Vec3 at) {
    }

    @Override
    protected void returnToCage(ServerLevel level, String messageKey) {
        super.returnToCage(level, "message.supernaturalcraft.ketch.left");
    }

    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        Vec3 at = position();
        BritishSpoils.share(level, this, at, new ItemStack(BritishRegistry.BRITISH_PISTOL.get()),
                new ItemStack(BritishRegistry.ENCHANTED_ROUND.get(), 16), new ItemStack(BritishRegistry.HEX_GRENADE.get(), 4),
                new ItemStack(BritishRegistry.KETCH_TROPHY_ITEM.get()), new ItemStack(BritishRegistry.BRITISH_DOSSIER.get()));
        for (ServerPlayer p : BritishSpoils.fighters(level, this)) {
            BritishOrder.update(p, b -> b.with(British.KETCH_BEATEN));
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.ketch.defeated").withStyle(ChatFormatting.GOLD), false);
        }
        fx(BritishFxPayload.TITLE, BritishFxPayload.WHO_KETCH, 0, at, 90);
    }

    // --- animation ----------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = animationPrefix();
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + "idle"), walk = RawAnimation.begin().thenLoop(pre + "walk"),
                run = RawAnimation.begin().thenLoop(pre + "run"), aim = RawAnimation.begin().thenLoop(pre + "aim");
        controllers.add(new AnimationController<>(this, "base", 5, state -> {
            if (state() == DYING) return PlayState.STOP;
            if (state.isMoving()) return state.setAndContinue(phase() >= 2 ? run : walk);
            return state.setAndContinue(state() == WINDUP || state() == ACTIVE ? aim : idle);
        }));
        AnimationController<LuciferEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String name : triggeredAnimations()) {
            action.triggerableAnim(name, name.equals("death")
                    ? RawAnimation.begin().thenPlayAndHold(pre + name)
                    : RawAnimation.begin().thenPlay(pre + name));
        }
        controllers.add(action);
    }
}
