package org.papiricoh.supernaturalcraft.entity.boss.crowley;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
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
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.asylum.AsylumFight;
import org.papiricoh.supernaturalcraft.asylum.layout.AsylumAnchors;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.network.CrowleyFxPayload;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Crowley, King of the Crossroads (v0.35): the boss of the asylum. Three phases, one room each (hall, cell wing, throne room);
 * between them he vanishes in red smoke, the bars of the next doorway fall, and he waits in the next room (the throne room he
 * takes seated, and rises once half that phase is gone). See {@link CrowleyBalance} and {@link CrowleyAttacks}.
 *
 * <p>Beaten, he does not die at once: he kneels ({@link #kneeling()}) and the case's hunter decides (silence: execution) —
 * {@link CrowleySpoils}. Executed, his long death plays; recruited, he goes in smoke and comes back as
 * {@link CrowleyCompanionEntity}. Either way his death is a boss death (credit, shares, spoils).
 *
 * <p>Synced state the renderer reads: {@link #eyesLit()}, {@link #seated()}, {@link #kneeling()}, {@link #vanished()}.
 */
public class CrowleyEntity extends LuciferEntity {

    /** Persistent-data flag on hounds and crossroads demons he called (they are his: {@link CrowleyRetinue#ours}). */
    public static final String CALLED_TAG = "sn_crowley_called";

    private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(CrowleyEntity.class, EntityDataSerializers.BYTE);
    public static final int EYES = 1, SEATED = 2, KNEELING = 4, VANISHED = 8;
    /** How long a transition (vanish, the doors, appear) lasts, and when within it he leaves and arrives. */
    public static final int TRANSITION = 80, LEAVE_AT = 14, ARRIVE_AT = 60;

    private @Nullable BlockPos asylumOrigin;
    private int asylumQ;
    private boolean emerged, risen, rematch;
    private int kneelTicks = -1, sincePatients;
    private @Nullable UUID caseOwner, decider, askedOf;
    private @Nullable Boolean verdictRecruit;
    private final List<Integer> collected = new ArrayList<>();
    private @Nullable UUID collectedFrom;
    private int collectedTicks;

    public CrowleyEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 900;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, VANILLA_BASE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLAGS, (byte) 0);
    }

    protected boolean flag(int bit) {
        return (entityData.get(FLAGS) & bit) != 0;
    }

    protected void setFlag(int bit, boolean on) {
        byte b = entityData.get(FLAGS);
        entityData.set(FLAGS, (byte) (on ? b | bit : b & ~bit));
    }

    public boolean eyesLit() {
        return flag(EYES);
    }

    public boolean seated() {
        return flag(SEATED);
    }

    public boolean kneeling() {
        return flag(KNEELING);
    }

    public boolean vanished() {
        return flag(VANISHED);
    }

    // --- set-up ---------------------------------------------------------------------------------------------------------

    /** In his asylum: the fight walks its rooms (else, spawned by egg or command, every room is where he stands). */
    public void bindAsylum(BlockPos origin, int q) {
        asylumOrigin = origin.immutable();
        asylumQ = q;
    }

    public @Nullable AsylumFight asylum() {
        return asylumOrigin == null ? null : new AsylumFight(asylumOrigin, asylumQ);
    }

    /** The hunter whose case brought them here: they decide his fate (else the one who struck last). */
    public void caseOwner(@Nullable UUID owner) {
        caseOwner = owner;
    }

    public @Nullable UUID caseOwner() {
        return caseOwner;
    }

    /** A rematch (by rite): no talk at the end, only the fight. */
    public void rematch(boolean on) {
        rematch = on;
    }

    public boolean isRematch() {
        return rematch;
    }

    // --- his numbers ----------------------------------------------------------------------------------------------------

    @Override
    public int maxPhase() {
        return CrowleyBalance.PHASES;
    }

    @Override
    protected float threshold(int phase) {
        return CrowleyBalance.threshold(phase);
    }

    @Override
    protected double healthPerExtraPlayer() {
        return SNConfig.CROWLEY_HEALTH_PER_PLAYER.get();
    }

    @Override
    protected float mundaneMultiplier() {
        return SNConfig.CROWLEY_MUNDANE_MULTIPLIER.get().floatValue();
    }

    @Override
    protected float damageFactor() {
        return SNConfig.CROWLEY_DAMAGE_FACTOR.get().floatValue();
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
        return CrowleyCinematics.INTRO_TICKS;
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return CrowleyAttacks.pool(phase);
    }

    @Override
    protected int baseGap(int phase) {
        return CrowleyBalance.attackGap(phase);
    }

    @Override
    protected String animationPrefix() {
        return "animation.crowley.";
    }

    @Override
    protected List<String> triggeredAnimations() {
        return CrowleyAssets.TRIGGERED;
    }

    @Override
    protected String bossBarKey(int phase) {
        return CrowleyAssets.BAR_PREFIX + ".phase" + phase;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return BossEvent.BossBarColor.RED;
    }

    @Override
    protected Component bossBarName(int phase) {
        return Component.translatable(bossBarKey(phase)).withStyle(phase == CrowleyBalance.PHASES ? ChatFormatting.DARK_RED : ChatFormatting.RED);
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return AllParticles.DEMON_SMOKE.get();
    }

    @Override
    protected SoundEvent emergeSound() {
        return AllSounds.crowley("smoke_in");
    }

    @Override
    protected SoundEvent roarSound() {
        return AllSounds.crowley("laugh");
    }

    @Override
    protected SoundEvent transformSound() {
        return AllSounds.crowley("smoke_out");
    }

    @Override
    protected SoundEvent ambientBossSound() {
        return AllSounds.crowley("laugh");
    }

    @Override
    protected SoundEvent dyingSound() {
        return AllSounds.crowley("death");
    }

    @Override
    protected SoundEvent deflectSound() {
        return AllSounds.crowley("hurt");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.crowley("hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.crowley("death");
    }

    @Override
    protected void clientEmergenceParticles() {
        level().addParticle(ParticleTypes.LARGE_SMOKE, getRandomX(0.8), getY() + random.nextDouble() * 2, getRandomZ(0.8), 0, 0.05, 0);
    }

    @Override
    protected void dyingParticles(ServerLevel level, boolean last) {
        if (last) {
            level.sendParticles(AllParticles.DEMON_SMOKE.get(), getX(), getY() + 1, getZ(), 80, 0.5, 1.0, 0.5, 0.08);
            return;
        }
        if (Boolean.TRUE.equals(verdictRecruit)) return;
        level.sendParticles(AllParticles.HELLFIRE.get(), getX(), getY() + 1.1, getZ(), 2, 0.2, 0.5, 0.2, 0.02);
    }

    // --- the fight's shape ----------------------------------------------------------------------------------------------

    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        return LuciferSummoning.openArena(level, blockPosition(), CrowleySummoning.FREE_RADIUS, ArenaTheme.ASYLUM);
    }

    @Override
    protected void applyTerrain(ServerLevel level, ArenaController arena, int phase) {
        AsylumFight a = asylum();
        if (a == null) return;
        String gate = AsylumFight.opensAt(phase);
        if (gate != null) for (BlockPos p : a.gate(gate)) arena.mutate(level, p, Blocks.AIR.defaultBlockState(), 0);
    }

    /** Bars across every gate the fight has not reached yet (and the front door): called as the arena opens. */
    public void shutGates(ServerLevel level, ArenaController arena) {
        AsylumFight a = asylum();
        if (a == null) return;
        for (String g : AsylumAnchors.GATES) {
            for (BlockPos p : a.gate(g)) {
                if (level.getBlockState(p).isAir() || level.getBlockState(p).canBeReplaced()) {
                    arena.mutate(level, p, Blocks.IRON_BARS.defaultBlockState(), 0);
                }
            }
        }
    }

    @Override
    protected void playEmergence() {
        CrowleyCinematics.intro(this);
    }

    @Override
    protected void playTransition(int to) {
        CrowleyCinematics.transition(this, to);
    }

    @Override
    protected void playDeath() {
        if (!Boolean.TRUE.equals(verdictRecruit)) CrowleyCinematics.execution(this);
    }

    @Override
    protected int transitionTicks(int to) {
        return TRANSITION;
    }

    @Override
    protected void onTransitionStart(int to) {
        fx(new CrowleyFxPayload(getId(), CrowleyFxPayload.SEAL_BREAK, to - 1, 0, position(), 30));
        triggerAnim("action", "vanish");
    }

    @Override
    protected void tickTransitionMotion(int elapsed, boolean last) {
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        getNavigation().stop();
        if (elapsed == LEAVE_AT) {
            fx(new CrowleyFxPayload(getId(), CrowleyFxPayload.VANISH, 0, 0, position(), 30));
            playSound(AllSounds.crowley("smoke_out"), 2f, 1f);
            setFlag(VANISHED, true);
            Vec3 to = roomCentre(phase());
            teleportTo(to.x, to.y, to.z);
            if (phase() == 2) {
                CrowleyAttacks.callPeople(this, false, CrowleyBalance.PATIENTS_PER_WAVE);
                sincePatients = 0;
            }
        }
        if (elapsed == ARRIVE_AT) arrive();
    }

    private void arrive() {
        setFlag(VANISHED, false);
        if (phase() >= CrowleyBalance.PHASES) {
            AsylumFight a = asylum();
            Vec3 seat = seat();
            teleportTo(seat.x, seat.y, seat.z);
            faceRoom();
            setFlag(SEATED, true);
            CrowleyAttacks.callCrossroads(this, CrowleyBalance.CROSSROADS_DEMONS);
            CrowleyCinematics.throne(this);
            CrowleyAttacks.callPeople(this, true, CrowleyBalance.SUITS_PER_CALL);
        } else {
            triggerAnim("action", "appear");
        }
        fx(new CrowleyFxPayload(getId(), CrowleyFxPayload.APPEAR, 0, 0, position(), 30));
        fx(new CrowleyFxPayload(getId(), CrowleyFxPayload.TITLE, phase(), 0, position(), 70));
        playSound(AllSounds.crowley("smoke_in"), 2f, 1f);
    }

    /** On his throne: the seat is a bottom slab, he sits on its top (else, with no asylum, where he stands). */
    private Vec3 seat() {
        AsylumFight a = asylum();
        return a != null ? a.floor(AsylumAnchors.THRONE_SEAT).add(0, 0.5, 0) : position();
    }

    /** Seated, he looks down the throne room. */
    private void faceRoom() {
        AsylumFight a = asylum();
        if (a == null) return;
        Vec3 to = a.floor(AsylumAnchors.THRONE);
        float yaw = (float) Math.toDegrees(Math.atan2(-(to.x - getX()), to.z - getZ()));
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
    }

    /** Where the fight stands in {@code phase}: its room in the asylum, else the arena's centre. */
    public Vec3 roomCentre(int phase) {
        AsylumFight a = asylum();
        if (a != null) return a.roomCentre(phase);
        ArenaController arena = arena();
        return arena != null ? Vec3.atBottomCenterOf(arena.center()) : position();
    }

    /** Where his people come through in the current room (round him when he has no asylum). */
    public List<Vec3> spawnPoints() {
        AsylumFight a = asylum();
        if (a != null) {
            List<Vec3> s = a.spawns(phase());
            if (!s.isEmpty()) return s;
        }
        List<Vec3> out = new ArrayList<>();
        Vec3 c = roomCentre(phase());
        for (int i = 0; i < AsylumAnchors.MIN_SPAWNS; i++) {
            double ang = i * Math.PI / 2 + Math.PI / 4;
            out.add(org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.floorAt(this, c.add(Math.cos(ang) * 6, 0, Math.sin(ang) * 6)));
        }
        return out;
    }

    @Override
    protected Vec3 tetherPoint(ArenaController arena) {
        return roomCentre(phase());
    }

    @Override
    protected void tickEmergence() {
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
    }

    @Override
    protected int deathTicks() {
        return Boolean.TRUE.equals(verdictRecruit) ? 30 : CrowleyBalance.DEATH_TICKS;
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
        super.returnToCage(level, messageKey.replace(".lucifer.", ".crowley."));
    }

    @Override
    protected boolean walks() {
        return !seated() && !kneeling() && !vanished();
    }

    // --- damage ---------------------------------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        if (kneeling() || vanished()) return false;
        return super.hurt(source, amount);
    }

    @Override
    protected float vulnerability(DamageSource source) {
        return super.vulnerability(source) * CrowleyBalance.share(guards());
    }

    /** His people standing near him (they cover him: {@link CrowleyBalance#share}). */
    public int guards() {
        int n = 0;
        for (Entity e : people()) if (e.distanceToSqr(this) < CrowleyBalance.GUARD_RANGE * CrowleyBalance.GUARD_RANGE) n++;
        return n;
    }

    @Override
    public boolean isPickable() {
        return !vanished() && super.isPickable();
    }

    @Override
    public boolean isInvisible() {
        return vanished() || super.isInvisible();
    }

    // --- his end --------------------------------------------------------------------------------------------------------

    @Override
    protected boolean interceptDeath() {
        scheduler().cancel();
        setFlag(SEATED, false);
        setFlag(KNEELING, true);
        triggerAnim("action", "kneel");
        kneelTicks = 0;
        for (Entity e : people()) {
            if (level() instanceof ServerLevel level) level.sendParticles(AllParticles.DEMON_SMOKE.get(), e.getX(), e.getY() + 1, e.getZ(), 8, 0.3, 0.8, 0.3, 0.05);
            e.discard();
        }
        fx(new CrowleyFxPayload(getId(), CrowleyFxPayload.TITLE, CrowleyBalance.PHASES, 1, position(), 70));
        return true;
    }

    /** The verdict (from {@link CrowleySpoils}): his death plays out as an execution or a recruitment. */
    public void verdict(boolean recruit, @Nullable ServerPlayer by) {
        if (verdictRecruit != null || !kneeling()) return;
        verdictRecruit = recruit;
        decider = by != null ? by.getUUID() : null;
        setFlag(KNEELING, false);
        if (recruit) {
            triggerAnim("action", "vanish");
            fx(new CrowleyFxPayload(getId(), CrowleyFxPayload.VANISH, 0, 0, position(), 30));
        }
        beginDying();
    }

    /** Who the talk was put to (only their answer counts). */
    public void askedOf(@Nullable UUID who) {
        askedOf = who;
    }

    public @Nullable UUID askedOf() {
        return askedOf;
    }

    public @Nullable Boolean verdict() {
        return verdictRecruit;
    }

    public @Nullable UUID decider() {
        return decider;
    }

    @Override
    protected void beginDying() {
        if (Boolean.TRUE.equals(verdictRecruit)) {
            super.beginDying();
            setFlag(VANISHED, true);
            return;
        }
        super.beginDying();
    }

    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        CrowleyCinematics.victory(this);
        CrowleySpoils.defeated(level, this, new ArrayList<>(challengers()));
    }

    private void tickKneeling(ServerLevel level) {
        getNavigation().stop();
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        scheduler().delay(5);
        kneelTicks++;
        if (kneelTicks == CrowleyBalance.KNEEL_TICKS) CrowleySpoils.offer(level, this);
        // Nobody answered (everyone gone, or a fake player in a test that never does): the default.
        if (kneelTicks > CrowleyBalance.KNEEL_TICKS + CrowleyBalance.TALK_TIMEOUT + 200) verdict(false, null);
    }

    // --- per tick -------------------------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        if (level() instanceof ServerLevel level && !isRemoved()) {
            if (kneeling()) tickKneeling(level);
            else if (seated() && state() != TRANSITION) tickSeated();
            if (phase() == 2 && state() != TRANSITION && !kneeling() && ++sincePatients >= CrowleyBalance.PATIENT_WAVE_EVERY) {
                sincePatients = 0;
                CrowleyAttacks.callPeople(this, false, CrowleyBalance.PATIENTS_PER_WAVE);
            }
            tickCollected(level);
            setFlag(EYES, state() == TRANSITION || scheduler().current() != null || phase() >= CrowleyBalance.PHASES);
        }
        super.customServerAiStep();
        if (isRemoved() || !(level() instanceof ServerLevel)) return;
        if (state() == EMERGING || emerged) return;
        emerged = true;
        fx(new CrowleyFxPayload(getId(), CrowleyFxPayload.TITLE, phase(), 0, position(), 70));
    }

    private void tickSeated() {
        AsylumFight a = asylum();
        if (a != null) {
            Vec3 seat = seat();
            if (position().distanceToSqr(seat) > 0.25) teleportTo(seat.x, seat.y, seat.z);
            faceRoom();
        }
        getNavigation().stop();
        float floor = getMaxHealth() * CrowleyBalance.threshold(CrowleyBalance.PHASES - 1) * CrowleyBalance.RISE_AT;
        if (!risen && getHealth() <= floor) {
            risen = true;
            setFlag(SEATED, false);
            triggerAnim("action", "throne_rise");
            playSound(AllSounds.crowley("laugh"), 2f, 0.9f);
            CrowleyAttacks.callPeople(this, true, CrowleyBalance.SUITS_PER_CALL);
        }
    }

    /** Collection: {@code debtor}'s hounds are on them for {@code ticks}. */
    public void markCollected(ServerPlayer debtor, int ticks) {
        collectedFrom = debtor.getUUID();
        collectedTicks = ticks;
    }

    private void tickCollected(ServerLevel level) {
        if (collectedFrom == null) return;
        if (--collectedTicks <= 0) {
            collectedFrom = null;
            return;
        }
        if (tickCount % 10 != 0) return;
        Entity d = level.getEntity(collectedFrom);
        if (!(d instanceof LivingEntity debtor) || !debtor.isAlive()) return;
        for (Entity e : people()) {
            if (e instanceof org.papiricoh.supernaturalcraft.entity.hellhound.HellhoundEntity h) h.setTarget(debtor);
        }
    }

    // --- his people -----------------------------------------------------------------------------------------------------

    /** Every one of his people alive in the fight. */
    public List<Entity> people() {
        List<Entity> out = new ArrayList<>();
        if (!(level() instanceof ServerLevel level)) return out;
        for (UUID id : minions()) {
            Entity e = level.getEntity(id);
            if (e != null && e.isAlive()) out.add(e);
        }
        return out;
    }

    public int people(Class<? extends Entity> kind) {
        int n = 0;
        for (Entity e : people()) if (kind.isInstance(e)) n++;
        return n;
    }

    /** He is behind someone in a puff of red smoke (the snap). */
    public void blink(Vec3 to) {
        fx(new CrowleyFxPayload(getId(), CrowleyFxPayload.VANISH, 0, 0, position(), 16));
        teleportTo(to.x, to.y, to.z);
        fx(new CrowleyFxPayload(getId(), CrowleyFxPayload.APPEAR, 0, 0, to, 16));
        playSound(AllSounds.crowley("smoke_in"), 1.5f, 1.2f);
    }

    public void fx(CrowleyFxPayload payload) {
        if (!(level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) if (p.distanceToSqr(this) < 96 * 96) Send.toPlayer(p, payload);
    }

    public void tell(String key, ChatFormatting... style) {
        if (!(level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 64 * 64) p.displayClientMessage(Component.translatable(key).withStyle(style), false);
        }
    }

    // --- animation ------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = animationPrefix();
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + "idle"), walk = RawAnimation.begin().thenLoop(pre + "walk"),
                sit = RawAnimation.begin().thenLoop(pre + "throne_idle"), kneel = RawAnimation.begin().thenLoop(pre + "kneel_idle");
        controllers.add(new AnimationController<>(this, "base", 5, state -> {
            if (state() == DYING || state() == EMERGING) return PlayState.STOP;
            if (kneeling()) return state.setAndContinue(kneel);
            if (seated()) return state.setAndContinue(sit);
            return state.setAndContinue(state.isMoving() ? walk : idle);
        }));
        AnimationController<LuciferEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String name : triggeredAnimations()) {
            boolean hold = name.equals("death") || name.equals("vanish");
            action.triggerableAnim(name, hold
                    ? RawAnimation.begin().thenPlayAndHold(pre + name)
                    : RawAnimation.begin().thenPlay(pre + name));
        }
        controllers.add(action);
    }

    // --- persistence ----------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Emerged", emerged);
        tag.putBoolean("Risen", risen);
        tag.putBoolean("Rematch", rematch);
        tag.putByte("CrowleyFlags", entityData.get(FLAGS));
        tag.putInt("KneelTicks", kneelTicks);
        if (asylumOrigin != null) {
            tag.put("AsylumOrigin", NbtUtils.writeBlockPos(asylumOrigin));
            tag.putInt("AsylumQ", asylumQ);
        }
        if (caseOwner != null) tag.putUUID("CaseOwner", caseOwner);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        emerged = tag.getBoolean("Emerged");
        risen = tag.getBoolean("Risen");
        rematch = tag.getBoolean("Rematch");
        if (tag.contains("CrowleyFlags")) entityData.set(FLAGS, (byte) (tag.getByte("CrowleyFlags") & ~VANISHED));
        kneelTicks = tag.contains("KneelTicks") ? tag.getInt("KneelTicks") : -1;
        if (kneeling()) kneelTicks = Math.min(kneelTicks, CrowleyBalance.KNEEL_TICKS - 1);
        asylumOrigin = NbtUtils.readBlockPos(tag, "AsylumOrigin").orElse(null);
        asylumQ = tag.getInt("AsylumQ");
        caseOwner = tag.hasUUID("CaseOwner") ? tag.getUUID("CaseOwner") : null;
    }
}
