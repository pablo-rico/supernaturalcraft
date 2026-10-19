package org.papiricoh.supernaturalcraft.entity.boss.grandwitch;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
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
import org.papiricoh.supernaturalcraft.entity.boss.BossDamage;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.entity.witch.CovenWitchEntity;
import org.papiricoh.supernaturalcraft.entity.witch.WitchMagic;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.network.GrandWitchFxPayload;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.GeckoLibServices;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The Grand Witch of the Grand Coven (v0.21, P6): the witch boss for a hunter who gave Rowena the Book of the Damned, called by
 * the Coven's rite ({@code call_the_grand_coven}; {@link GrandWitchSummoning}) wherever it is done ({@link ArenaTheme#COVEN}).
 * Three phases, a third of her true health each (27 000 alone):
 *
 * <pre>EMERGING (the coven gathers: the intro camera) → P1 the Coven's Shield (three Coven Witches channel into her; she cannot be
 *   hurt while one still does) → P2 the Matriarch's Hexes (frost, inferno, crush) → P3 the Mass Taglock (every hunter's lock at
 *   once; Rowena at their side draws green circles to stand in; smoke veil) → DYING</pre>
 *
 * <p><b>The shield.</b> {@link #channelers()} are {@link CovenWitchEntity}s set to channel ({@link CovenWitchEntity#setChanneling});
 * while any still channels she swallows every blow ({@link GrandWitchBalance#shielded}). A channeler whose own shield breaks is
 * interrupted for {@link GrandWitchBalance#INTERRUPT_TICKS}; killed, she is gone. Past phase I the survivors fight as witches.
 *
 * <p><b>Rowena.</b> At phase III Rowena arrives in her smoke ({@link RowenaAllyEntity}), untouchable, and draws the green circles
 * of the mass taglock. As the Grand Witch dies the spoils go to her hunters ({@link GrandWitchSpoils}); Rowena goes home, where
 * she takes the Mark off her allies. Optional: she blocks nothing.
 */
public class GrandWitchEntity extends LuciferEntity {

    public static final int MAX_PHASE = GrandWitchBalance.PHASES;
    public static final int INTRO_TICKS = GrandWitchCinematics.INTRO_TICKS;

    /** Synced: bit {@link #SHIELDED} (her coven's shield: the {@code shield} loop). */
    private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(GrandWitchEntity.class, EntityDataSerializers.BYTE);
    public static final int SHIELDED = 1;

    private boolean emerged, covenCalled;
    private final List<UUID> channelers = new ArrayList<>();
    private final Map<UUID, Integer> interrupted = new HashMap<>();
    private final Map<UUID, Boolean> wasShielded = new HashMap<>();
    private @Nullable UUID ally;
    private final List<LivingEntity> tracked = new ArrayList<>();
    private @Nullable Supplier<BossAttack<LuciferEntity>> queued;

    public GrandWitchEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 800;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, VANILLA_BASE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLAGS, (byte) 0);
    }

    /** Her coven's shield is up (synced). */
    public boolean shielded() {
        return (entityData.get(FLAGS) & SHIELDED) != 0;
    }

    // --- her numbers --------------------------------------------------------------------------------

    @Override
    public int maxPhase() {
        return MAX_PHASE;
    }

    @Override
    protected float threshold(int phase) {
        return GrandWitchBalance.threshold(phase);
    }

    @Override
    protected double healthPerExtraPlayer() {
        return SNConfig.GRAND_WITCH_HEALTH_PER_PLAYER.get();
    }

    @Override
    protected float mundaneMultiplier() {
        return SNConfig.GRAND_WITCH_MUNDANE_MULTIPLIER.get().floatValue();
    }

    @Override
    protected float damageFactor() {
        return SNConfig.GRAND_WITCH_DAMAGE_FACTOR.get().floatValue();
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
        return GrandWitchAttacks.pool(phase);
    }

    @Override
    protected int baseGap(int phase) {
        return GrandWitchBalance.attackGap(phase);
    }

    @Override
    protected String animationPrefix() {
        return "animation.grand_witch.";
    }

    @Override
    protected List<String> triggeredAnimations() {
        return GrandWitchAssets.TRIGGERED;
    }

    /** {@code entity.supernaturalcraft.grand_witch.bar.phase<n>}: the HUD's key for her bar. */
    @Override
    protected String bossBarKey(int phase) {
        return "entity.supernaturalcraft.grand_witch.bar.phase" + phase;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return switch (phase) {
            case 1 -> BossEvent.BossBarColor.PURPLE;
            case 2 -> BossEvent.BossBarColor.BLUE;
            default -> BossEvent.BossBarColor.RED;
        };
    }

    @Override
    protected Component bossBarName(int phase) {
        return Component.translatable(bossBarKey(phase)).withStyle(phase == MAX_PHASE ? ChatFormatting.DARK_RED : ChatFormatting.DARK_PURPLE);
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return phase == 2 ? ParticleTypes.SNOWFLAKE : ParticleTypes.WITCH;
    }

    @Override
    protected SoundEvent emergeSound() {
        return AllSounds.coven("grand_witch.smoke_veil");
    }

    @Override
    protected SoundEvent roarSound() {
        return AllSounds.coven("grand_witch.laugh");
    }

    @Override
    protected SoundEvent transformSound() {
        return AllSounds.coven("grand_witch.laugh");
    }

    @Override
    protected SoundEvent ambientBossSound() {
        return AllSounds.coven("grand_witch.ambient");
    }

    @Override
    protected SoundEvent dyingSound() {
        return AllSounds.coven("grand_witch.death");
    }

    @Override
    protected SoundEvent deflectSound() {
        return AllSounds.coven("grand_witch.shield");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.coven("grand_witch.hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.coven("grand_witch.death");
    }

    @Override
    protected void clientEmergenceParticles() {
        level().addParticle(ParticleTypes.WITCH, getRandomX(0.8), getY() + random.nextDouble() * 2.2, getRandomZ(0.8), 0, 0.05, 0);
    }

    @Override
    protected void dyingParticles(ServerLevel level, boolean last) {
        if (last) {
            level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 1, getZ(), 80, 0.5, 1.1, 0.5, 0.08);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 40, 0.4, 0.8, 0.4, 0.04);
            return;
        }
        level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 1.2, getZ(), 3, 0.25, 0.5, 0.25, 0.02);
    }

    // --- the fight's shape --------------------------------------------------------------------------

    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        return LuciferSummoning.openArena(level, blockPosition(), SNConfig.GRAND_WITCH_ARENA_RADIUS.get(), ArenaTheme.COVEN);
    }

    @Override
    protected void playEmergence() {
        callCoven();
        if (level() instanceof ServerLevel level) {
            MagicFx.teleport(level, this, School.WITCHCRAFT, position(), position(), MagicFxPayload.SMOKE_WITCH, 40);
        }
        GrandWitchCinematics.intro(this);
    }

    @Override
    protected void playTransition(int to) {
        GrandWitchCinematics.transition(this, to);
    }

    @Override
    protected void playDeath() {
        GrandWitchCinematics.death(this);
    }

    @Override
    protected void onTransitionStart(int to) {
        if (to >= 2) releaseCoven();
        if (to >= MAX_PHASE) RowenaAllyEntity.arrive(this);
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
        return GrandWitchBalance.DEATH_TICKS;
    }

    @Override
    protected void tickDyingMotion(int elapsed) {
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
    }

    @Override
    protected void leaveBehind(ServerLevel level, Vec3 at) {
    }

    @Override
    protected void returnToCage(ServerLevel level, String messageKey) {
        super.returnToCage(level, messageKey.replace(".lucifer.", ".grand_witch."));
    }

    /** She sinks; her coven scatters in smoke; Rowena stays to see it and goes home. */
    @Override
    protected void beginDying() {
        if (level() instanceof ServerLevel level) {
            setShielded(false);
            for (UUID id : minions()) {
                Entity e = level.getEntity(id);
                if (e == null || e instanceof RowenaAllyEntity) continue;
                MagicFx.teleport(level, e, School.WITCHCRAFT, e.position(), e.position(), MagicFxPayload.SMOKE_WITCH, 30);
                e.discard();
            }
            RowenaAllyEntity r = ally();
            if (r != null) r.farewell(deathTicks());
        }
        super.beginDying();
    }

    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        GrandWitchCinematics.victory(this);
        List<ServerPlayer> hunters = new ArrayList<>(challengers());
        for (LivingEntity e : tracked) if (e instanceof ServerPlayer p && !hunters.contains(p)) hunters.add(p);
        GrandWitchSpoils.drop(level, this, hunters, position().add(0, 1.2, 0));
        GrandWitchSpoils.rowenaThanks(hunters);
    }

    @Override
    protected boolean walks() {
        return !shielded();
    }

    @Override
    protected void customServerAiStep() {
        if (level() instanceof ServerLevel level && !isRemoved()) tickCoven(level);
        super.customServerAiStep();
        if (isRemoved() || !(level() instanceof ServerLevel)) return;
        if (state() == EMERGING || emerged) return;
        emerged = true;
        if (!covenCalled && phase() == 1) callCoven();
        fx(new GrandWitchFxPayload(getId(), GrandWitchFxPayload.TITLE, phase(), 0, position(), 70));
    }

    // --- the coven ----------------------------------------------------------------------------------

    /** Her three channelers rise round her and begin to channel. */
    public void callCoven() {
        if (!(level() instanceof ServerLevel level) || covenCalled) return;
        covenCalled = true;
        double base = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < GrandWitchBalance.CHANNELERS; i++) {
            CovenWitchEntity w = AllEntities.COVEN_WITCH.get().create(level);
            if (w == null) continue;
            double a = base + i * Math.PI * 2 / GrandWitchBalance.CHANNELERS;
            Vec3 s = LuciferAttacks.floorAt(this, position().add(Math.cos(a) * GrandWitchBalance.CHANNEL_DISTANCE, 0,
                    Math.sin(a) * GrandWitchBalance.CHANNEL_DISTANCE));
            float yaw = LuciferAttacks.yawTo(s, position());
            w.moveTo(s.x, s.y, s.z, yaw, 0);
            w.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(s)), MobSpawnType.MOB_SUMMONED, null);
            w.setPersistenceRequired();
            w.setYHeadRot(yaw);
            w.setYBodyRot(yaw);
            level.addFreshEntity(w);
            w.setChanneling(true);
            channelers.add(w.getUUID());
            wasShielded.put(w.getUUID(), true);
            minions().add(w.getUUID());
            MagicFx.teleport(level, w, School.WITCHCRAFT, s, s, MagicFxPayload.SMOKE_WITCH, 30);
            fx(new GrandWitchFxPayload(w.getId(), GrandWitchFxPayload.CHANNEL, getId(), 1, w.position(), GrandWitchBalance.BEAM_REFRESH + 10));
        }
        setShielded(true);
    }

    /** Her living channelers (channelling or not). */
    public List<CovenWitchEntity> channelers() {
        List<CovenWitchEntity> out = new ArrayList<>();
        if (!(level() instanceof ServerLevel level)) return out;
        for (UUID id : channelers) if (level.getEntity(id) instanceof CovenWitchEntity w && w.isAlive()) out.add(w);
        return out;
    }

    /** How many still channel into her. */
    public int channeling() {
        int n = 0;
        for (CovenWitchEntity w : channelers()) if (w.channeling()) n++;
        return n;
    }

    private void tickCoven(ServerLevel level) {
        if (phase() != 1 || state() == DYING) {
            if (shielded()) setShielded(false);
            return;
        }
        for (CovenWitchEntity w : channelers()) {
            UUID id = w.getUUID();
            boolean was = wasShielded.getOrDefault(id, true), now = w.shielded();
            wasShielded.put(id, now);
            if (w.channeling() && was && !now) interrupt(w);
            Integer left = interrupted.get(id);
            if (left != null && !w.channeling()) {
                if (left <= 1) {
                    interrupted.remove(id);
                    // She resumes whole: her bubble rises with the channel, so it can be broken (and the channel with it) again.
                    w.restoreShield();
                    wasShielded.put(id, true);
                    w.setChanneling(true);
                    w.setTarget(null);
                    fx(new GrandWitchFxPayload(w.getId(), GrandWitchFxPayload.CHANNEL, getId(), 1, w.position(), GrandWitchBalance.BEAM_REFRESH + 10));
                } else {
                    interrupted.put(id, left - 1);
                }
            }
            if (w.channeling()) {
                w.getLookControl().setLookAt(this);
                if (tickCount % GrandWitchBalance.BEAM_REFRESH == 0) {
                    fx(new GrandWitchFxPayload(w.getId(), GrandWitchFxPayload.CHANNEL, getId(), 1, w.position(), GrandWitchBalance.BEAM_REFRESH + 10));
                }
                if (tickCount % 80 == 0) w.playSound(AllSounds.coven("witch.channel"), 0.8f, 1f);
            }
        }
        boolean up = GrandWitchBalance.shielded(phase(), channeling());
        if (up != shielded()) setShielded(up);
    }

    /** {@code w}'s channel is broken (her shield gave): she stops for a while and fights. */
    public void interrupt(CovenWitchEntity w) {
        w.setChanneling(false);
        interrupted.put(w.getUUID(), GrandWitchBalance.INTERRUPT_TICKS);
        LivingEntity t = attackTarget();
        if (t != null) w.setTarget(t);
        fx(new GrandWitchFxPayload(w.getId(), GrandWitchFxPayload.CHANNEL, getId(), 0, w.position(), 0));
    }

    /** Past phase I her coven stops channelling and fights. */
    private void releaseCoven() {
        LivingEntity t = attackTarget();
        for (CovenWitchEntity w : channelers()) {
            if (w.channeling()) fx(new GrandWitchFxPayload(w.getId(), GrandWitchFxPayload.CHANNEL, getId(), 0, w.position(), 0));
            w.setChanneling(false);
            if (t != null) w.setTarget(t);
        }
        interrupted.clear();
        setShielded(false);
    }

    private void setShielded(boolean on) {
        if (on == shielded()) return;
        entityData.set(FLAGS, (byte) (on ? SHIELDED : 0));
        fx(new GrandWitchFxPayload(getId(), GrandWitchFxPayload.SHIELD, on ? 1 : 0, 0, position(), 20));
        if (level() instanceof ServerLevel level) {
            MagicFx.ward(level, School.WITCHCRAFT, this, position().add(0, getBbHeight() / 2, 0), 1.6f,
                    on ? MagicFxPayload.WARD_RAISED : MagicFxPayload.WARD_BROKEN, 20);
            if (!on && phase() == 1 && state() != DYING) tell(level, "message.supernaturalcraft.grand_witch.shield_down");
        }
        playSound(AllSounds.coven(on ? "grand_witch.shield" : "grand_witch.shield_break"), 2f, 1f);
    }

    public @Nullable RowenaAllyEntity ally() {
        if (ally == null || !(level() instanceof ServerLevel level)) return null;
        return level.getEntity(ally) instanceof RowenaAllyEntity r && r.isAlive() ? r : null;
    }

    void setAlly(RowenaAllyEntity r) {
        ally = r.getUUID();
        minions().add(r.getUUID());
    }

    // --- damage -------------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        Entity by = source.getEntity();
        // Her own coven never wounds her (a stray bolt, a familiar).
        if (by != null && by != this && WitchMagic.kin(by)) return false;
        if (shielded() && !BossDamage.passesThrough(source)) {
            if (by instanceof net.minecraft.world.entity.player.Player && tickCount % 10 == 0) playSound(deflectSound(), 1.2f, 1.2f);
            if (level() instanceof ServerLevel level && tickCount % 6 == 0) {
                MagicFx.ward(level, School.WITCHCRAFT, this, position().add(0, getBbHeight() / 2, 0), 1.6f, MagicFxPayload.WARD_HIT, 10);
            }
            return false;
        }
        return super.hurt(source, amount);
    }

    // --- hunters, effects ---------------------------------------------------------------------------

    public List<LivingEntity> hunters() {
        List<LivingEntity> out = new ArrayList<>(challengers());
        tracked.removeIf(e -> !e.isAlive() || e.isRemoved());
        for (LivingEntity e : tracked) if (!out.contains(e)) out.add(e);
        return out;
    }

    public void track(LivingEntity e) {
        if (!tracked.contains(e)) tracked.add(e);
    }

    /** The nearest challenger; with none, the nearest tracked hunter (tests: fake players are never challengers). */
    @Override
    public @Nullable LivingEntity attackTarget() {
        LivingEntity t = super.attackTarget();
        if (t != null) return t;
        tracked.removeIf(e -> !e.isAlive() || e.isRemoved());
        LivingEntity best = null;
        for (LivingEntity e : tracked) if (best == null || e.distanceToSqr(this) < best.distanceToSqr(this)) best = e;
        return best;
    }

    public void fx(GrandWitchFxPayload payload) {
        if (!(level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 96 * 96) Send.toPlayer(p, payload);
        }
    }

    void tell(ServerLevel level, String key) {
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 64 * 64) p.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE), true);
        }
    }

    // --- attacks ------------------------------------------------------------------------------------

    public void queue(Supplier<BossAttack<LuciferEntity>> attack) {
        queued = attack;
    }

    @Override
    public @Nullable Supplier<BossAttack<LuciferEntity>> forcedAttack(LivingEntity target) {
        if (queued != null) {
            Supplier<BossAttack<LuciferEntity>> q = queued;
            queued = null;
            return q;
        }
        if (phase() >= 2 && (distanceToSqr(target) > 18 * 18 || noSightTicks > 60)) {
            noSightTicks = 0;
            return GrandWitchAttacks.SmokeVeil::new;
        }
        return null;
    }

    // --- GeckoLib -----------------------------------------------------------------------------------

    @Override
    public void triggerAnim(@Nullable String controller, String anim) {
        if ("action".equals(controller) && !GrandWitchAssets.TRIGGERED.contains(anim)) return;
        if (level().isClientSide) {
            var manager = getAnimatableInstanceCache().getManagerForId(getId());
            if (controller != null) manager.tryTriggerAnimation(controller, anim);
            else manager.tryTriggerAnimation(anim);
        } else {
            GeckoLibServices.NETWORK.triggerEntityAnim(this, false, controller, anim);
        }
    }

    static final List<String> HOLDS = List.of("death");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = animationPrefix();
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + "idle"), walk = RawAnimation.begin().thenLoop(pre + "walk"),
                shield = RawAnimation.begin().thenLoop(pre + "shield");
        controllers.add(new AnimationController<>(this, "base", 6, state -> {
            if (state() == DYING) return PlayState.STOP;
            if (shielded()) return state.setAndContinue(shield);
            return state.setAndContinue(state.isMoving() ? walk : idle);
        }));
        AnimationController<GrandWitchEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String name : GrandWitchAssets.TRIGGERED) {
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
        tag.putBoolean("CovenCalled", covenCalled);
        ListTag list = new ListTag();
        for (UUID id : channelers) list.add(NbtUtils.createUUID(id));
        tag.put("Channelers", list);
        if (ally != null) tag.putUUID("Ally", ally);
        CompoundTag paused = new CompoundTag();
        interrupted.forEach((id, ticks) -> paused.putInt(id.toString(), ticks));
        tag.put("Interrupted", paused);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        emerged = tag.getBoolean("Emerged");
        covenCalled = tag.getBoolean("CovenCalled");
        channelers.clear();
        for (Tag t : tag.getList("Channelers", Tag.TAG_INT_ARRAY)) channelers.add(NbtUtils.loadUUID(t));
        ally = tag.hasUUID("Ally") ? tag.getUUID("Ally") : null;
        interrupted.clear();
        CompoundTag paused = tag.getCompound("Interrupted");
        for (String k : paused.getAllKeys()) {
            try {
                interrupted.put(UUID.fromString(k), paused.getInt(k));
            } catch (IllegalArgumentException ignored) {
                // a damaged key: that witch just stays as she is
            }
        }
    }
}
