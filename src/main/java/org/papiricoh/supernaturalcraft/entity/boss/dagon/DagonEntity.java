package org.papiricoh.supernaturalcraft.entity.boss.dagon;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
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
import org.papiricoh.supernaturalcraft.british.birth.BirthScene;
import org.papiricoh.supernaturalcraft.british.site.BritishSites;
import org.papiricoh.supernaturalcraft.british.site.PlacedSite;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;
import java.util.function.Supplier;

/**
 * Dagon, Prince of Hell (v0.41, season 12's finale): she guards Kelly Kline's safe house for Lucifer's child, and a hunter on
 * the nephilim's trail who comes near finds her before the door ({@link DagonSummoning#begin}). Telekinesis and hellfire: see
 * {@link DagonBalance}, {@link DagonAttacks}. When she falls the birth follows ({@link BirthScene}): Kelly dies bringing Jack
 * into the world, Lucifer reaches for him and is thrown back through a rift.
 */
public class DagonEntity extends LuciferEntity {

    private static final double CAMERA_RANGE = 72;

    private @Nullable BlockPos safehouse;
    private int safehouseQ;
    /** Hunters known to be in this fight besides the arena's own (the one who walked up; tests' fake players). Not saved. */
    private final List<ServerPlayer> witnesses = new java.util.ArrayList<>();

    public DagonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 600;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, VANILLA_BASE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    // --- set-up -------------------------------------------------------------------------------------------------------------

    public void bindSafehouse(BlockPos origin, int q) {
        safehouse = origin.immutable();
        safehouseQ = q;
    }

    public @Nullable PlacedSite site() {
        return safehouse == null ? null : new PlacedSite(BritishSites.SAFEHOUSE, safehouse, safehouseQ);
    }

    /** Her power shows (the eyes and the aura's glow): while she works it, from the third phase on, and as she burns. */
    public boolean powerShows() {
        byte s = state();
        return phase() >= DagonBalance.PHASES || s == WINDUP || s == ACTIVE || s == DYING || s == TRANSITION;
    }

    /** {@code p} is in this fight whatever the arena knows (the birth is theirs too). */
    public void witness(ServerPlayer p) {
        if (!witnesses.contains(p)) witnesses.add(p);
    }

    /** Test and command hook: her long death begins now. */
    public void defeatNow() {
        beginDying();
    }

    /** Everyone who fought her: the arena's fighters and her witnesses still about. */
    public List<ServerPlayer> hunters(ServerLevel level) {
        List<ServerPlayer> out = new java.util.ArrayList<>(BritishSpoils.fighters(level, this));
        for (ServerPlayer w : witnesses) {
            if (w.isRemoved() || out.stream().anyMatch(o -> o.getUUID().equals(w.getUUID()))) continue;
            out.add(w);
        }
        return out;
    }

    public void tell(String key) {
        if (!(level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 48 * 48) p.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), false);
        }
    }

    void fx(byte kind, int arg, int arg2, Vec3 at, int duration) {
        if (level() instanceof ServerLevel level) BritishFxPayload.send(level, getId(), kind, arg, arg2, at, duration);
    }

    private void camera(String name, int ticks) {
        CinematicLocks.play(this, SupernaturalCraft.asResource("dagon_" + name), ticks, CAMERA_RANGE);
    }

    // --- her numbers --------------------------------------------------------------------------------------------------------

    @Override
    public int maxPhase() {
        return DagonBalance.PHASES;
    }

    @Override
    protected float threshold(int phase) {
        return DagonBalance.threshold(phase);
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
        return DagonBalance.EMERGE;
    }

    @Override
    protected int deathTicks() {
        return DagonBalance.deathTicks();
    }

    @Override
    protected int transitionTicks(int to) {
        return DagonBalance.TRANSITION;
    }

    @Override
    protected boolean walks() {
        return phase() < DagonBalance.PHASES;
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return DagonAttacks.pool(phase);
    }

    @Override
    protected int baseGap(int phase) {
        return DagonBalance.attackGap(phase);
    }

    @Override
    public @Nullable Supplier<BossAttack<LuciferEntity>> forcedAttack(LivingEntity target) {
        if (distanceToSqr(target) > DagonBalance.PULL_DISTANCE * DagonBalance.PULL_DISTANCE || noSightTicks > DagonBalance.PULL_NO_SIGHT) {
            noSightTicks = 0;
            return DagonAttacks.Drag::new;
        }
        return null;
    }

    @Override
    protected String animationPrefix() {
        return "animation." + DagonAssets.RIG + ".";
    }

    @Override
    protected List<String> triggeredAnimations() {
        return DagonAssets.TRIGGERED;
    }

    @Override
    protected String bossBarKey(int phase) {
        return "entity.supernaturalcraft.dagon.bar.phase" + phase;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return BossEvent.BossBarColor.RED;
    }

    @Override
    protected Component bossBarName(int phase) {
        return Component.translatable(bossBarKey(phase)).withStyle(phase >= DagonBalance.PHASES ? ChatFormatting.GOLD : ChatFormatting.DARK_RED);
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return AllParticles.HELLFIRE.get();
    }

    @Override
    protected void clientPhaseParticles(int phase) {
        if (random.nextFloat() < 0.25f) {
            level().addParticle(AllParticles.HELLFIRE.get(), getRandomX(0.6), getY() + random.nextDouble() * 0.4, getRandomZ(0.6), 0, 0.03, 0);
        }
    }

    @Override
    protected SoundEvent emergeSound() {
        return BritishRegistry.sound("dagon.fire");
    }

    @Override
    protected SoundEvent roarSound() {
        return BritishRegistry.sound("dagon.scream");
    }

    @Override
    protected SoundEvent transformSound() {
        return BritishRegistry.sound("dagon.fire");
    }

    @Override
    protected SoundEvent ambientBossSound() {
        return BritishRegistry.sound("dagon.crush");
    }

    @Override
    protected SoundEvent dyingSound() {
        return BritishRegistry.sound("dagon.death");
    }

    @Override
    protected SoundEvent deflectSound() {
        return BritishRegistry.sound("dagon.hurt");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return BritishRegistry.sound("dagon.hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return BritishRegistry.sound("dagon.death");
    }

    @Override
    protected void clientEmergenceParticles() {
        level().addParticle(AllParticles.HELLFIRE.get(), getRandomX(1.0), getY() + 0.1, getRandomZ(1.0), 0, 0.1, 0);
    }

    @Override
    protected void dyingParticles(ServerLevel level, boolean last) {
        if (last) {
            level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(AllParticles.HELLFIRE.get(), getX(), getY() + 1, getZ(), 120, 0.5, 1.0, 0.5, 0.12);
            return;
        }
        level.sendParticles(AllParticles.HELLFIRE.get(), getX(), getY() + 1.2, getZ(), 4, 0.25, 0.6, 0.25, 0.03);
    }

    // --- the fight's shape --------------------------------------------------------------------------------------------------

    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        return LuciferSummoning.openArena(level, blockPosition(), DagonSummoning.FREE_RADIUS, ArenaTheme.SAFEHOUSE);
    }

    @Override
    protected void applyTerrain(ServerLevel level, ArenaController arena, int phase) {
    }

    @Override
    protected void playEmergence() {
        fx(BritishFxPayload.TITLE, BritishFxPayload.WHO_DAGON, 1, position(), 70);
        camera("intro", 60);
        tell("message.supernaturalcraft.dagon.intro");
    }

    @Override
    protected void playTransition(int to) {
        fx(BritishFxPayload.TITLE, BritishFxPayload.WHO_DAGON, to, position(), 60);
        fx(BritishFxPayload.RING, 60, BritishFxPayload.FIRE, position(), 20);
        triggerAnim("action", "scream");
        tell("message.supernaturalcraft.dagon.phase" + to);
    }

    @Override
    protected void playDeath() {
        camera("death", deathTicks());
        tell("message.supernaturalcraft.dagon.dying");
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
        super.returnToCage(level, "message.supernaturalcraft.dagon.left");
    }

    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        Vec3 at = position();
        BritishSpoils.share(level, this, at, new ItemStack(BritishRegistry.DAGON_TROPHY_ITEM.get()), new ItemStack(BritishRegistry.DAGONS_BRAND.get()));
        List<ServerPlayer> hunters = hunters(level);
        for (ServerPlayer p : hunters) {
            BritishOrder.update(p, b -> b.with(British.DAGON_BEATEN));
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.dagon.defeated").withStyle(ChatFormatting.GOLD), false);
        }
        fx(BritishFxPayload.TITLE, BritishFxPayload.WHO_DAGON, 0, at, 90);
        BirthScene.after(level, site(), at, hunters);
    }

    // --- animation ----------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = animationPrefix();
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + "idle"), walk = RawAnimation.begin().thenLoop(pre + "walk"),
                hover = RawAnimation.begin().thenLoop(pre + "hover_idle"), guard = RawAnimation.begin().thenLoop(pre + "guard");
        controllers.add(new AnimationController<>(this, "base", 6, state -> {
            if (state() == DYING) return PlayState.STOP;
            if (state() == EMERGING) return state.setAndContinue(guard);
            if (phase() >= DagonBalance.PHASES) return state.setAndContinue(hover);
            return state.setAndContinue(state.isMoving() ? walk : idle);
        }));
        AnimationController<LuciferEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String name : triggeredAnimations()) {
            action.triggerableAnim(name, name.equals("death")
                    ? RawAnimation.begin().thenPlayAndHold(pre + name)
                    : RawAnimation.begin().thenPlay(pre + name));
        }
        controllers.add(action);
    }

    // --- persistence --------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (safehouse != null) {
            tag.put("Safehouse", NbtUtils.writeBlockPos(safehouse));
            tag.putInt("SafehouseQ", safehouseQ);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        safehouse = NbtUtils.readBlockPos(tag, "Safehouse").orElse(null);
        safehouseQ = tag.getInt("SafehouseQ");
    }
}
