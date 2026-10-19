package org.papiricoh.supernaturalcraft.entity.boss.cain;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.network.CainFxPayload;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.GeckoLibServices;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Cain, the Father of Murder (v0.21): an optional superboss after Lucifer, met as a beekeeper at his hidden farm
 * ({@code cain.CainWorld}) who takes up a hunter's challenge at night ({@code cain.CainServerHandlers}). Three phases, a third of
 * his true health each (19 000 alone):
 *
 * <pre>EMERGING (he sets down his smoker: the intro camera) → P1 the Beekeeper → P2 the Mark awakens (the Mark burns on his
 *   forearm) → P3 the Father of Murder (he takes up the First Blade, his eyes go red) → DYING (he kneels and offers the Mark)</pre>
 *
 * <p>The First Blade, his own, bites him ×{@link CainBalance#FIRST_BLADE_VULNERABILITY}. As he dies he offers each hunter who
 * fought him the Mark ({@link CainSpoils#offerAll}); whoever refuses may take it later at his grave. Optional: he blocks nothing
 * ({@code BossProgression.Boss.optional}).
 */
public class CainEntity extends LuciferEntity {

    public static final int MAX_PHASE = CainBalance.PHASES;
    /** His intro: the camera's length ({@code cinematics/cain_intro.json}). */
    public static final int INTRO_TICKS = CainCinematics.INTRO_TICKS;

    /** His farm's hives (where the Mark's flare leaves shelter), empty when spawned away from it. */
    private final List<BlockPos> hives = new ArrayList<>();
    private boolean emerged, offered;
    /** Tests: hunters his attacks see beyond the challengers (fake players are not in the level's list). */
    private final List<LivingEntity> tracked = new ArrayList<>();
    /** Tests and previews: an attack to use next, whatever the pool says. */
    private @Nullable Supplier<BossAttack<LuciferEntity>> queued;

    public CainEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 700;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, VANILLA_BASE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    // --- his numbers --------------------------------------------------------------------------------

    @Override
    public int maxPhase() {
        return MAX_PHASE;
    }

    @Override
    protected float threshold(int phase) {
        return CainBalance.threshold(phase);
    }

    @Override
    protected double healthPerExtraPlayer() {
        return SNConfig.CAIN_HEALTH_PER_PLAYER.get();
    }

    @Override
    protected float mundaneMultiplier() {
        return SNConfig.CAIN_MUNDANE_MULTIPLIER.get().floatValue();
    }

    @Override
    protected float damageFactor() {
        return SNConfig.CAIN_DAMAGE_FACTOR.get().floatValue();
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
        return INTRO_TICKS;
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return CainAttacks.pool(phase);
    }

    @Override
    protected int baseGap(int phase) {
        return CainBalance.attackGap(phase);
    }

    @Override
    protected String animationPrefix() {
        return "animation.cain.";
    }

    @Override
    protected List<String> triggeredAnimations() {
        return CainAssets.TRIGGERED;
    }

    /** {@code entity.supernaturalcraft.cain.bar.phase<n>}: the HUD's key for his bar. */
    @Override
    protected String bossBarKey(int phase) {
        return "entity.supernaturalcraft.cain.bar.phase" + phase;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return phase == 1 ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.RED;
    }

    @Override
    protected Component bossBarName(int phase) {
        return Component.translatable(bossBarKey(phase)).withStyle(phase == 1 ? ChatFormatting.GOLD : ChatFormatting.DARK_RED);
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return phase >= 2 ? AllParticles.HELLFIRE.get() : ParticleTypes.FALLING_HONEY;
    }

    @Override
    protected SoundEvent emergeSound() {
        return AllSounds.cain("ambient");
    }

    @Override
    protected SoundEvent roarSound() {
        return AllSounds.cain("mark_flare");
    }

    @Override
    protected SoundEvent transformSound() {
        return AllSounds.cain("mark_flare");
    }

    @Override
    protected SoundEvent ambientBossSound() {
        return AllSounds.cain("ambient");
    }

    @Override
    protected SoundEvent dyingSound() {
        return AllSounds.cain("death");
    }

    @Override
    protected SoundEvent deflectSound() {
        return AllSounds.cain("hurt");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.cain("hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.cain("death");
    }

    @Override
    protected void clientEmergenceParticles() {
        if (random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.FALLING_HONEY, getRandomX(1.0), getY() + 1 + random.nextDouble(), getRandomZ(1.0), 0, 0, 0);
        }
    }

    /** From phase II the Mark smoulders on his right forearm: a few embers there, not about his head. */
    @Override
    protected void clientPhaseParticles(int phase) {
        if (random.nextFloat() >= 0.12f) return;
        double yaw = Math.toRadians(yBodyRot);
        // His right side: facing (-sin, cos), so right is (-cos, -sin); the forearm hangs from the elbow to the wrist.
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw), side = 0.36, along = 0.75 + random.nextDouble() * 0.4;
        double fx = -Math.sin(yaw) * 0.05, fz = Math.cos(yaw) * 0.05;
        level().addParticle(phaseParticle(phase), getX() + rx * side + fx, getY() + along, getZ() + rz * side + fz, 0, 0.01, 0);
    }

    @Override
    protected void dyingParticles(ServerLevel level, boolean last) {
        if (last) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 60, 0.5, 0.8, 0.5, 0.05);
            level.sendParticles(AllParticles.HELLFIRE.get(), getX(), getY() + 1, getZ(), 40, 0.4, 0.6, 0.4, 0.05);
            return;
        }
        level.sendParticles(AllParticles.HELLFIRE.get(), getX(), getY() + 1.1, getZ(), 2, 0.3, 0.4, 0.3, 0.01);
    }

    // --- the fight's shape --------------------------------------------------------------------------

    /** Spawned by egg or command: his yard round wherever he stands. */
    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        return LuciferSummoning.openArena(level, blockPosition(), SNConfig.CAIN_ARENA_RADIUS.get(), ArenaTheme.FARM);
    }

    @Override
    protected void playEmergence() {
        CainCinematics.intro(this);
    }

    @Override
    protected void playTransition(int to) {
        CainCinematics.transition(this, to);
    }

    @Override
    protected void playDeath() {
        CainCinematics.death(this);
    }

    @Override
    protected void onTransitionStart(int to) {
        // Not Lucifer's (no shield, no flight): the Mark flares as it wakes (the renderer shows it from phase II, the blade
        // and his eyes from phase III, from the synced phase).
        if (level() instanceof ServerLevel level) {
            level.sendParticles(AllParticles.HELLFIRE.get(), getX(), getY() + 1.2, getZ(), 40, 0.4, 0.7, 0.4, 0.05);
        }
        triggerAnim("action", "mark_flare");
    }

    @Override
    protected void tickTransitionMotion(int elapsed, boolean last) {
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        getNavigation().stop();
    }

    @Override
    protected void tickEmergence() {
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
    }

    @Override
    protected int deathTicks() {
        return CainBalance.DEATH_TICKS;
    }

    @Override
    protected void tickDyingMotion(int elapsed) {
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
    }

    /** His yard needs nothing changed between phases (his attacks change it themselves, for a while). */
    @Override
    protected void applyTerrain(ServerLevel level, ArenaController arena, int phase) {
    }

    /** The fight was lost: he goes back to his bees; nothing is left behind. */
    @Override
    protected void leaveBehind(ServerLevel level, Vec3 at) {
    }

    @Override
    protected void returnToCage(ServerLevel level, String messageKey) {
        super.returnToCage(level, messageKey.replace(".lucifer.", ".cain."));
    }

    /** He kneels; the Mark is offered to every hunter who fought him. */
    @Override
    protected void beginDying() {
        super.beginDying();
        if (!offered && level() instanceof ServerLevel) {
            offered = true;
            CainSpoils.offerAll(this, hunters());
        }
    }

    /** At the end of his death: the victory, his spoils per hunter, and his grave at the farm. */
    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        CainCinematics.victory(this);
        List<ServerPlayer> hunters = new ArrayList<>(challengers());
        for (LivingEntity e : tracked) if (e instanceof ServerPlayer p && !hunters.contains(p)) hunters.add(p);
        CainSpoils.drop(level, hunters, position().add(0, 1.2, 0), random);
        org.papiricoh.supernaturalcraft.cain.CainWorld.fallen(level, this);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (isRemoved() || !(level() instanceof ServerLevel)) return;
        byte s = state();
        if (s == EMERGING || emerged) return;
        emerged = true;
        fx(new CainFxPayload(getId(), CainFxPayload.TITLE, phase(), 0, position(), 70));
    }

    // --- his farm -----------------------------------------------------------------------------------

    /** Where his farm's hives stand (the flare's shelters lie toward them). */
    public void setHives(List<BlockPos> at) {
        hives.clear();
        for (BlockPos p : at) hives.add(p.immutable());
    }

    public List<BlockPos> hives() {
        return hives;
    }

    // --- damage -------------------------------------------------------------------------------------

    /** Beyond Lucifer's own: his own First Blade bites him harder. */
    @Override
    protected float vulnerability(DamageSource source) {
        float v = super.vulnerability(source);
        if (source.getDirectEntity() instanceof LivingEntity by && by.getMainHandItem().is(AllItems.FIRST_BLADE.get())) {
            v *= CainBalance.FIRST_BLADE_VULNERABILITY;
        }
        return v;
    }

    // --- hunters and effects ------------------------------------------------------------------------

    /** The hunters his attacks see: the challengers, and anyone tracked (tests). */
    public List<LivingEntity> hunters() {
        List<LivingEntity> out = new ArrayList<>(challengers());
        tracked.removeIf(e -> !e.isAlive() || e.isRemoved());
        for (LivingEntity e : tracked) if (!out.contains(e)) out.add(e);
        return out;
    }

    /** Test hook: {@code e} counts as one of his hunters. */
    public void track(LivingEntity e) {
        if (!tracked.contains(e)) tracked.add(e);
    }

    /** Sends one of the fight's moments to every hunter near. */
    public void fx(CainFxPayload payload) {
        if (!(level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 96 * 96) Send.toPlayer(p, payload);
        }
    }

    // --- attacks ------------------------------------------------------------------------------------

    /** Tests and previews: the next attack he uses, whatever the pool says. */
    public void queue(Supplier<BossAttack<LuciferEntity>> attack) {
        queued = attack;
    }

    /** A queued attack first; a lunge when the hunter runs or hides. */
    @Override
    public @Nullable Supplier<BossAttack<LuciferEntity>> forcedAttack(LivingEntity target) {
        if (queued != null) {
            Supplier<BossAttack<LuciferEntity>> q = queued;
            queued = null;
            return q;
        }
        if (distanceToSqr(target) > 15 * 15 || noSightTicks > 60) {
            noSightTicks = 0;
            return phase() >= MAX_PHASE ? CainAttacks.Execution::new : CainAttacks.Lunge::new;
        }
        return null;
    }

    // --- GeckoLib -----------------------------------------------------------------------------------

    /** Only his own clips: Lucifer's names that he lacks ({@code emerge}, {@code transform_<n>}, …) are dropped. */
    @Override
    public void triggerAnim(@Nullable String controller, String anim) {
        if ("action".equals(controller) && !CainAssets.TRIGGERED.contains(anim)) return;
        if (level().isClientSide) {
            var manager = getAnimatableInstanceCache().getManagerForId(getId());
            if (controller != null) {
                // Restart a clip already playing (the 2nd and 3rd lunge come every 16 ticks during the 24-tick clip).
                var c = manager.getAnimationControllers().get(controller);
                if (c != null) c.forceAnimationReset();
                manager.tryTriggerAnimation(controller, anim);
            } else {
                manager.tryTriggerAnimation(anim);
            }
        } else {
            GeckoLibServices.NETWORK.triggerEntityAnim(this, false, controller, anim);
        }
    }

    /** Clips that hold their last frame. */
    static final List<String> HOLDS = List.of("death");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = animationPrefix();
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + "idle"), walk = RawAnimation.begin().thenLoop(pre + "walk");
        controllers.add(new AnimationController<>(this, "base", 6, state -> {
            if (state() == DYING) return PlayState.STOP;
            return state.setAndContinue(state.isMoving() ? walk : idle);
        }));
        AnimationController<CainEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String name : CainAssets.TRIGGERED) {
            action.triggerableAnim(name, HOLDS.contains(name)
                    ? RawAnimation.begin().thenPlayAndHold(pre + name)
                    : RawAnimation.begin().thenPlay(pre + name));
        }
        controllers.add(action);
    }

    // --- persistence --------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Emerged", emerged);
        tag.putBoolean("Offered", offered);
        tag.put("Hives", new LongArrayTag(hives.stream().mapToLong(BlockPos::asLong).toArray()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        emerged = tag.getBoolean("Emerged");
        offered = tag.getBoolean("Offered");
        hives.clear();
        for (long l : tag.getLongArray("Hives")) hives.add(BlockPos.of(l));
    }
}
