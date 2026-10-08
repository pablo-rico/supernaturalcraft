package org.papiricoh.supernaturalcraft.entity.boss.michael;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.michael.host.HostAngelEntity;
import org.papiricoh.supernaturalcraft.entity.boss.michael.host.HostFormation;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.entity.boss.michael.projectile.MichaelLanceEntity;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.michael.arena.HeavenGround;
import org.papiricoh.supernaturalcraft.network.MichaelFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.GeckoLibServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.function.Supplier;

/**
 * The Archangel Michael, the Sword of Heaven: the end of Heaven's road (Spire → Broken Chorus → Metatron → Michael), as
 * Lucifer Uncaged is the end of Hell's. Built on {@link LuciferEntity}'s fight through its hooks, like Uncaged: six phases,
 * a sixth of his health each, true health above the vanilla cap ({@link #healthScale()}).
 *
 * <pre>EMERGING (he comes down into the Garden) → I The Vessel → II The General (the Host) → III The Lance (shadow wings,
 *   the War in Heaven) → IV The Wings (in the air) → V The Archangel (his true form, the Throne Room) → VI The Sword of
 *   Heaven (the halo broken) → DYING (he kneels; the armour falls)</pre>
 *
 * <p>Two models: the vessel ({@code michael}) in phases I–IV and the true form ({@code michael_archangel}, 4.5 blocks) from
 * V. Their clips live on two controllers, {@code action} and {@code archangel}; {@link #triggerAnim} routes Lucifer's own
 * triggers to the form he wears (as Chuck's does), through {@link MichaelAnimations}' aliases.
 */
public class MichaelEntity extends LuciferEntity {

    public static final byte FORM_VESSEL = 0, FORM_ARCHANGEL = 1;
    public static final byte WINGS_NONE = 0, WINGS_SHADOW = 1, WINGS_STEEL = 2;

    private static final EntityDataAccessor<Byte> FORM = SynchedEntityData.defineId(MichaelEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> WINGS = SynchedEntityData.defineId(MichaelEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> HALO_BROKEN = SynchedEntityData.defineId(MichaelEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LANCE_HELD = SynchedEntityData.defineId(MichaelEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDimensions ARCHANGEL_SIZE = EntityDimensions.scalable(1.6f, 4.6f).withEyeHeight(4.1f);

    private float healthScale = 1f;
    private boolean scaled;
    /** His three Heavens, pinned to the arena as he arrives; null until then (and again after a reload). */
    private @Nullable HeavenGround heaven;
    /** Whether the Angel Tablet and the Seraph Wings were offered for him (they go back, win or lose). */
    private boolean relicsOffered;
    /** Game time until which he reels (a parried or broken touch). */
    private long staggeredUntil;
    /** Game time of his next question ("I need your yes"). */
    private long nextAsk = -1;
    /** One formation per company of the Host (two in phase V). */
    private final HostFormation[] companies = {new HostFormation(), new HostFormation()};
    /** A captain has fallen since the Host last came down: he is exposed. */
    private boolean hostBroken;
    private int reinforcements, livingHostCache = -1;
    /** The lance he threw (stuck in the ground or flying), while it is not in his hand. */
    private @Nullable UUID lanceId;
    private long lanceAwaySince;
    /** A hunter pulled his lance out of the ground (phase VI): it is theirs for a few seconds. */
    private boolean lanceStolen;
    /** Hunters pinned by his lance: where, and until when. */
    private final Map<UUID, Pin> pinned = new HashMap<>();
    /** Phase IV: he has come down to gather himself. */
    private boolean landed;
    private int aerialAttacks;
    /** While true, no hit cap (his own lance thrown back at him). */
    private boolean uncapped;
    private @Nullable Vec3 emergeTo;
    /** Tests: an attack to use next, whatever the pool says. */
    private @Nullable Supplier<BossAttack<LuciferEntity>> queued;

    /** A hunter held where the lance struck. */
    private record Pin(LivingEntity who, Vec3 at, long until) {
    }

    private static final ResourceLocation UNARMED_SPEED = SupernaturalCraft.asResource("michael_unarmed");

    public MichaelEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 2000;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MichaelBalance.BASE_HEALTH)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FORM, FORM_VESSEL);
        builder.define(WINGS, WINGS_NONE);
        builder.define(HALO_BROKEN, false);
        builder.define(LANCE_HELD, true);
    }

    // --- his looks ----------------------------------------------------------------------------------

    public boolean isArchangel() {
        return entityData.get(FORM) == FORM_ARCHANGEL;
    }

    public void setArchangel(boolean archangel) {
        entityData.set(FORM, archangel ? FORM_ARCHANGEL : FORM_VESSEL);
        if (archangel) setWings(WINGS_STEEL);
        refreshDimensions();
    }

    public byte wings() {
        return entityData.get(WINGS);
    }

    public void setWings(byte wings) {
        entityData.set(WINGS, wings);
    }

    public boolean haloBroken() {
        return entityData.get(HALO_BROKEN);
    }

    public void setHaloBroken(boolean broken) {
        entityData.set(HALO_BROKEN, broken);
    }

    /** Whether the Lance is in his hand (not thrown, not stolen). */
    public boolean lanceHeld() {
        return entityData.get(LANCE_HELD);
    }

    /** Without his lance he is quicker on his feet (and has only the blade's reach). */
    public void setLanceHeld(boolean held) {
        entityData.set(LANCE_HELD, held);
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(UNARMED_SPEED);
            if (!held) speed.addTransientModifier(new AttributeModifier(UNARMED_SPEED, MichaelBalance.UNARMED_SPEED - 1,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    public boolean relicsOffered() {
        return relicsOffered;
    }

    public void setRelicsOffered(boolean offered) {
        relicsOffered = offered;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (FORM.equals(key)) refreshDimensions();
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return isArchangel() ? ARCHANGEL_SIZE : getType().getDimensions();
    }

    // --- his numbers --------------------------------------------------------------------------------

    @Override
    public int maxPhase() {
        return MichaelBalance.PHASES;
    }

    @Override
    protected float threshold(int phase) {
        return MichaelBalance.threshold(phase);
    }

    @Override
    protected float healthScale() {
        return healthScale;
    }

    /** Test and command hook. */
    public void setHealthScale(float scale) {
        healthScale = scale;
        scaled = true;
    }

    @Override
    protected void scaleHealthToChallengers() {
        healthScale = MichaelBalance.healthScale(SNConfig.MICHAEL_HEALTH_MULTIPLIER.get(), SNConfig.MICHAEL_HEALTH_PER_PLAYER.get(),
                challengers().size());
        scaled = true;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(MichaelBalance.BASE_HEALTH);
        setHealth((float) MichaelBalance.BASE_HEALTH);
    }

    @Override
    protected float mundaneMultiplier() {
        return SNConfig.MICHAEL_MUNDANE_MULTIPLIER.get().floatValue();
    }

    @Override
    protected float hitCap() {
        return uncapped ? 100_000f : SNConfig.MICHAEL_HIT_CAP.get().floatValue();
    }

    @Override
    public float attackDamageMultiplier() {
        return SNConfig.MICHAEL_DAMAGE_MULTIPLIER.get().floatValue();
    }

    /** Phase IV, in the air, except while he has come down to gather himself. */
    @Override
    public boolean isAerialPhase() {
        return phase() == MichaelBalance.AERIAL_PHASE && !landed;
    }

    public boolean landed() {
        return landed;
    }

    public void setLanded(boolean landed) {
        this.landed = landed;
        if (landed) setNoGravity(false);
    }

    @Override
    public float scale(int phase) {
        return 1.0f;
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return MichaelAttacks.pool(phase);
    }

    @Override
    protected int baseGap(int phase) {
        return MichaelBalance.attackGap(phase);
    }

    @Override
    protected int emergeTicks() {
        return MichaelBalance.EMERGE_TICKS;
    }

    @Override
    protected int deathTicks() {
        return MichaelBalance.DEATH_TICKS;
    }

    @Override
    protected int transitionTicks(int to) {
        return to == MichaelBalance.ARCHANGEL_PHASE ? MichaelBalance.TRANSFORM_TICKS : MichaelBalance.TRANSITION_TICKS;
    }

    @Override
    protected String animationPrefix() {
        return isArchangel() ? MichaelAnimations.ARCHANGEL : MichaelAnimations.VESSEL;
    }

    @Override
    protected List<String> triggeredAnimations() {
        return MichaelAnimations.triggered(MichaelAnimations.VESSEL_CLIPS);
    }

    /** The boss bar's name key: the celestial HUD ({@code MichaelHud}) recognises its prefix and draws its own bar. */
    @Override
    protected String bossBarKey(int phase) {
        return "entity.supernaturalcraft.michael.bar.phase" + phase;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return phase >= MichaelBalance.ARCHANGEL_PHASE ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.BLUE;
    }

    @Override
    protected Component bossBarName(int phase) {
        return Component.translatable(bossBarKey(phase)).withStyle(ChatFormatting.AQUA);
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return AllParticles.GRACE.get();
    }

    @Override
    protected SoundEvent ambientBossSound() {
        return AllSounds.MICHAEL_AMBIENT.get();
    }

    @Override
    protected SoundEvent emergeSound() {
        return AllSounds.MICHAEL_TRUMPET.get();
    }

    @Override
    protected SoundEvent roarSound() {
        return AllSounds.MICHAEL_CHOIR.get();
    }

    @Override
    protected SoundEvent transformSound() {
        return AllSounds.MICHAEL_TRANSFORM.get();
    }

    @Override
    protected SoundEvent dyingSound() {
        return AllSounds.MICHAEL_DEATH.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.MICHAEL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.MICHAEL_DEATH.get();
    }

    // --- the fight's shape --------------------------------------------------------------------------

    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        return LuciferSummoning.openArena(level, blockPosition(), SNConfig.MICHAEL_ARENA_RADIUS.get(), ArenaTheme.HEAVEN);
    }

    /** Halfway through a change of phase: the next Heaven, if this phase is fought in another (III and V). */
    @Override
    protected void applyTerrain(ServerLevel level, ArenaController arena, int phase) {
        shiftHeaven(level, arena, MichaelBalance.arenaOf(phase));
        if (phase == MichaelBalance.HOST_PHASE || phase == MichaelBalance.ARCHANGEL_PHASE) {
            reinforcements = 1;
            summonHost(level, phase == MichaelBalance.ARCHANGEL_PHASE ? 2 : 1);
        }
    }

    // --- Heaven -------------------------------------------------------------------------------------

    /** The seed of his Heavens: the same arena is laid the same way again. */
    private long heavenSeed(ArenaController arena) {
        return arena.center().asLong() * 31 + ArenaTheme.HEAVEN;
    }

    private HeavenGround heaven(ServerLevel level, ArenaController arena) {
        if (heaven == null) heaven = HeavenGround.pin(level, arena, heavenSeed(arena));
        return heaven;
    }

    /** Starts writing Heaven {@code which} (0 the Garden, 1 the War in Heaven, 2 the Throne Room) and tells the hunters. */
    public void shiftHeaven(ServerLevel level, ArenaController arena, int which) {
        HeavenGround h = heaven(level, arena);
        if (h.current() == which) return;
        h.begin(which);
        fx(level, new MichaelFxPayload(getId(), MichaelFxPayload.ARENA_SHIFT, which, 0, arena.centerVec(), 60));
        playSound(AllSounds.MICHAEL_CHOIR.get(), 3.0f, which == 1 ? 0.8f : 1.0f);
    }

    /** Whether a Heaven is still being written: the fight waits for it. */
    public boolean writingHeaven() {
        return heaven != null && heaven.writing();
    }

    /** The Heaven standing (or being written), -1 before the first. */
    public int heavenShown() {
        return heaven != null ? heaven.current() : -1;
    }

    /** Test and preview hook: writes the current Heaven (or {@code which}, if it is another) all at once. */
    public void layHeavenNow(int which) {
        if (!(level() instanceof ServerLevel level) || arena() == null) return;
        ArenaController arena = arena();
        shiftHeaven(level, arena, which);
        heaven(level, arena).finish(level, arena);
    }

    private void tickHeaven(ServerLevel level, ArenaController arena) {
        HeavenGround h = heaven(level, arena);
        if (h.current() < 0) shiftHeaven(level, arena, MichaelBalance.arenaOf(phase()));
        if (!h.writing()) return;
        h.tick(level, arena);
        byte s = state();
        if (s != EMERGING && s != TRANSITION && s != DYING) {
            // The fight waits while Heaven changes round it.
            if (scheduler().current() == null) scheduler().delay(10);
            getNavigation().stop();
        }
    }

    @Override
    protected void playEmergence() {
        MichaelCinematics.intro(this);
        emergeTo = position();
        setNoGravity(true);
        setPos(emergeTo.x, emergeTo.y + MichaelBalance.DESCENT, emergeTo.z);
        if (level() instanceof ServerLevel level) say(level, MichaelQuotes.onPhase(1, uncagedBeaten()));
    }

    @Override
    protected void playTransition(int to) {
        MichaelCinematics.transition(this, to);
    }

    @Override
    protected void playDeath() {
        MichaelCinematics.death(this);
    }

    @Override
    protected void beginDying() {
        super.beginDying();
        if (level() instanceof ServerLevel level) VesselPossession.releaseAll(this, level);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel level) VesselPossession.releaseAll(this, level);
        super.remove(reason);
    }

    @Override
    protected void onTransitionStart(int to) {
        // A change of phase brings the lance home, and him off the ground.
        if (!lanceHeld() && !lanceStolen) {
            if (lance() != null) lance().discard();
            lanceReturned();
        }
        landed = false;
        aerialAttacks = 0;
        pinned.clear();
        if (to == MichaelBalance.SHADOW_WINGS_PHASE) setWings(WINGS_SHADOW);
        if (to == MichaelBalance.ARCHANGEL_PHASE && level() instanceof ServerLevel level) {
            // The vessel bursts into light; the true form stands up at the light's peak (tickTransitionMotion).
            fx(level, new MichaelFxPayload(getId(), MichaelFxPayload.TRANSFORM, 0, 0, position(), MichaelBalance.TRANSFORM_TICKS));
        }
        if (to == MichaelBalance.PHASES) {
            setHaloBroken(true);
            playSound(AllSounds.MICHAEL_HALO_BREAK.get(), 4.0f, 1.0f);
        }
        if (level() instanceof ServerLevel level) say(level, MichaelQuotes.onPhase(to, uncagedBeaten()));
    }

    /** He stands still through a change of phase; into his true form, the vessel gives way at the light's peak. */
    @Override
    protected void tickTransitionMotion(int elapsed, boolean last) {
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        if (phase() == MichaelBalance.ARCHANGEL_PHASE && !isArchangel() && elapsed >= MichaelBalance.TRANSFORM_SWAP_TICKS) {
            setArchangel(true);
            triggerAnim("archangel", "emerge");
            playSound(AllSounds.MICHAEL_TRANSFORM.get(), 4.0f, 1.2f);
            if (level() instanceof ServerLevel level) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, getX(), getY() + 2, getZ(), 200, 1.5, 2.5, 1.5, 0.3);
            }
        }
    }

    /** Whether one of his challengers has put Lucifer back in the box: his lines change. */
    public boolean uncagedBeaten() {
        if (!(level() instanceof ServerLevel level)) return false;
        var adv = level.getServer().getAdvancements().get(SupernaturalCraft.asResource("main/back_in_the_box"));
        if (adv == null) return false;
        for (ServerPlayer p : challengers()) if (p.getAdvancements().getOrStartProgress(adv).isDone()) return true;
        return false;
    }

    /** Michael says {@code key} to everyone near. */
    public void say(ServerLevel level, String key) {
        MichaelAttacks.say(this, level, key);
    }

    /** He comes down out of Heaven's light onto the spot he was called to. */
    @Override
    protected void tickEmergence() {
        setDeltaMovement(Vec3.ZERO);
        if (emergeTo == null) return;
        int elapsed = MichaelBalance.EMERGE_TICKS - stateTimer;
        float k = Math.min(1f, elapsed / (MichaelBalance.EMERGE_TICKS * 0.7f));
        double ease = 1 - (1 - k) * (1 - k);
        setPos(emergeTo.x, emergeTo.y + MichaelBalance.DESCENT * (1 - ease), emergeTo.z);
        if (k >= 1f) {
            setNoGravity(false);
            emergeTo = null;
        }
    }

    @Override
    protected void clientEmergenceParticles() {
        level().addParticle(AllParticles.GRACE.get(), getRandomX(1.2), getY() + random.nextDouble() * 2, getRandomZ(1.2), 0, 0.03, 0);
    }

    /** At the end of his death: his spoils for every hunter who fought him, and the relics back. */
    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        MichaelCinematics.victory(this);
        Vec3 at = position().add(0, 1.5, 0);
        MichaelSpoils.drop(level, this, at);
        returnRelics(level, at);
    }

    /** The fight was lost: he leaves nothing, but gives back what was offered for him. */
    @Override
    protected void leaveBehind(ServerLevel level, Vec3 at) {
        returnRelics(level, at);
    }

    private void returnRelics(ServerLevel level, Vec3 at) {
        if (!relicsOffered) return;
        relicsOffered = false;
        for (ItemStack stack : List.of(new ItemStack(AllItems.ANGEL_TABLET.get()), new ItemStack(AllItems.SERAPH_WINGS.get()))) {
            ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack);
            item.setDefaultPickUpDelay();
            level.addFreshEntity(item);
        }
    }

    @Override
    protected void returnToCage(ServerLevel level, String messageKey) {
        super.returnToCage(level, messageKey.replace(".lucifer.", ".michael."));
    }

    // --- ticking ------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        if (!scaled && state() != EMERGING) scaleHealthToChallengers();
        super.customServerAiStep();
        if (isRemoved() || !(level() instanceof ServerLevel level)) return;
        ArenaController arena = arena();
        if (arena == null || !arena.isActive()) return;
        tickHeaven(level, arena);
        if (tickCount % 10 == 0) livingHostCache = -1;
        byte s = state();
        if (!isAerialPhase() && isNoGravity() && s != EMERGING) setNoGravity(false);
        tickPins(level);
        if (s == DYING) return;
        VesselPossession.tick(this, level);
        if (s != EMERGING && s != TRANSITION) {
            tickQuestion(level);
            tickGeneral();
            if (tickCount % MichaelQuotes.GAP == MichaelQuotes.GAP / 2) say(level, MichaelQuotes.any(random.nextInt(1000), uncagedBeaten()));
        }
    }

    // --- "I need your yes" --------------------------------------------------------------------------

    private void tickQuestion(ServerLevel level) {
        long now = level.getGameTime();
        if (nextAsk < 0) nextAsk = now + MichaelBalance.askGap(phase()) / 2;
        if (now < nextAsk || VesselPossession.busy(this) || writingHeaven()) return;
        nextAsk = now + MichaelBalance.askGap(phase());
        List<ServerPlayer> hunters = challengers();
        if (hunters.isEmpty()) return;
        ServerPlayer asked = hunters.get(random.nextInt(hunters.size()));
        triggerAnim("action", "ask_yes");
        VesselPossession.ask(asked, this);
    }

    /** Heals {@code amount} true health, never past the start of the phase he is in. */
    public void healTrue(float amount) {
        float ceiling = phase() <= 1 ? getMaxHealth() : getMaxHealth() * threshold(phase() - 1);
        setHealth(Math.min(ceiling, getHealth() + amount / healthScale()));
    }

    // --- the touch, staggers, his damage policy -----------------------------------------------------

    public boolean isStaggered() {
        return level().getGameTime() < staggeredUntil;
    }

    /** He reels for {@code ticks}: whatever he was doing stops, and he is open. */
    public void stagger(int ticks, @Nullable ServerPlayer by, String messageKey) {
        staggeredUntil = level().getGameTime() + ticks;
        scheduler().cancel();
        scheduler().delay(ticks);
        getNavigation().stop();
        triggerAnim("action", isArchangel() ? "stagger" : "parried");
        playSound(AllSounds.MICHAEL_HURT.get(), 2.0f, 0.7f);
        if (by != null) by.displayClientMessage(Component.translatable(messageKey).withStyle(ChatFormatting.GOLD), true);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, getX(), getY() + getBbHeight() * 0.7, getZ(), 30, 0.4, 0.5, 0.4, 0.3);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        float before = getHealth();
        boolean hurt = super.hurt(source, amount);
        float taken = (before - getHealth()) * healthScale();
        if (taken > 0 && scheduler().current() instanceof MichaelAttacks.ForeheadTouch touch
                && scheduler().stage() == AttackScheduler.Stage.WINDUP && source.getEntity() instanceof LivingEntity by) {
            touch.wound(this, taken, by);
        }
        return hurt;
    }

    /**
     * Beyond Lucifer's own: a hunter he let go of strikes twice as hard (his grace's favour); with a captain fallen he is
     * exposed; reeling, he is open.
     */
    @Override
    protected float vulnerability(DamageSource source) {
        float v = super.vulnerability(source);
        if (source.getEntity() instanceof LivingEntity by && by.hasEffect(AllMobEffects.GRACE_FAVOR)) v *= MichaelBalance.FAVOR_MULTIPLIER;
        if (hostBroken) v *= MichaelBalance.HOST_BROKEN_MULTIPLIER;
        if (isStaggered()) v *= MichaelBalance.STAGGER_VULNERABILITY;
        return v;
    }

    // --- the Host ------------------------------------------------------------------------------------

    public HostFormation formation(int company) {
        return companies[Math.floorMod(company, companies.length)];
    }

    public boolean hostBroken() {
        return hostBroken;
    }

    /** Calls the Host down round him: {@code captains} companies, each a captain and its soldiers, in a shield wall. */
    public List<HostAngelEntity> summonHost(ServerLevel level, int captains) {
        List<HostAngelEntity> out = new ArrayList<>();
        hostBroken = false;
        int soldiers = captains > 1 ? MichaelBalance.HOST_SOLDIERS_EACH_OF_TWO : MichaelBalance.HOST_SOLDIERS;
        for (int c = 0; c < captains; c++) {
            companies[c] = new HostFormation();
            int size = soldiers + 1;
            for (int i = 0; i < size; i++) {
                HostAngelEntity soldier = AllEntities.HOST_ANGEL.get().create(level);
                if (soldier == null) continue;
                double a = (c * size + i) * Math.PI * 2 / (captains * size) + random.nextDouble() * 0.2;
                Vec3 at = LuciferAttacks.floorAt(this, position().add(Math.cos(a) * 4.5, 0, Math.sin(a) * 4.5));
                soldier.moveTo(at.x, at.y, at.z, random.nextFloat() * 360, 0);
                soldier.finalizeSpawn(level, level.getCurrentDifficultyAt(soldier.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                soldier.setVessel(random.nextInt(HostAngelEntity.VESSELS));
                soldier.setCaptain(i == 0);
                soldier.setMaster(getUUID());
                soldier.enlist(c, i, size);
                level.addFreshEntity(soldier);
                minions().add(soldier.getUUID());
                out.add(soldier);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, at.x, at.y + 2, at.z, 20, 0.2, 2, 0.2, 0.02);
            }
        }
        livingHostCache = -1;
        playSound(AllSounds.HOST_MARCH.get(), 3.0f, 1.0f);
        triggerAnim("action", "summon_host");
        return out;
    }

    /** Soldiers of the Host still standing. */
    public int livingHost() {
        if (livingHostCache >= 0) return livingHostCache;
        int n = 0;
        if (level() instanceof ServerLevel level) {
            for (var id : minions()) if (level.getEntity(id) instanceof HostAngelEntity h && h.isAlive()) n++;
        }
        livingHostCache = n;
        return n;
    }

    public int hostReinforcements() {
        return reinforcements;
    }

    public void useReinforcement() {
        reinforcements = Math.max(0, reinforcements - 1);
    }

    /** A captain is dead: its company breaks, and he is exposed. */
    public void captainFell(int company) {
        formation(company).captainFalls();
        hostBroken = true;
        livingHostCache = -1;
        playSound(AllSounds.MICHAEL_HURT.get(), 2.5f, 0.6f);
        if (level() instanceof ServerLevel level) MichaelAttacks.say(this, level, "message.supernaturalcraft.michael.captain_falls");
    }

    /** His next order to every company still formed, against {@code foe}. */
    public void orderHost(LivingEntity foe) {
        int foes = Math.max(1, challengers().size());
        for (HostFormation f : companies) f.give(HostFormation.next(f.order(), distanceTo(foe), foes));
    }

    /** Who a soldier of the Host goes for: a marked hunter first (the nearest), else his own target. */
    public @Nullable LivingEntity hostTarget(HostAngelEntity soldier) {
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (ServerPlayer p : challengers()) {
            if (!p.hasEffect(AllMobEffects.HEAVENS_MARK)) continue;
            double d = p.distanceToSqr(soldier);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        if (best != null) return best;
        LivingEntity mine = getTarget();
        return mine != null && mine.isAlive() ? mine : attackTarget();
    }

    /** Whether he hangs back behind the Host, commanding (phases II and V, while it stands and no one is close). */
    public boolean directing() {
        int phase = phase();
        if ((phase != MichaelBalance.HOST_PHASE && phase != MichaelBalance.ARCHANGEL_PHASE) || livingHost() == 0) return false;
        LivingEntity t = getTarget();
        return t == null || distanceToSqr(t) > MichaelBalance.ENGAGE_DISTANCE * MichaelBalance.ENGAGE_DISTANCE;
    }

    @Override
    protected boolean walks() {
        return !directing() && !isStaggered();
    }

    /** Behind the Host: keeps about ten blocks from his target. */
    private void tickGeneral() {
        if (isStaggered()) {
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            return;
        }
        if (!directing() || scheduler().current() != null || isAerialPhase()) return;
        LivingEntity t = getTarget();
        if (t == null) return;
        double d = distanceTo(t);
        if (d > 7 && d < 13) {
            getNavigation().stop();
            return;
        }
        Vec3 back = position().subtract(t.position()).multiply(1, 0, 1);
        if (back.lengthSqr() < 0.01) back = new Vec3(1, 0, 0);
        Vec3 want = t.position().add(back.normalize().scale(10));
        getNavigation().moveTo(want.x, want.y, want.z, 1.0);
    }

    // --- the Lance -----------------------------------------------------------------------------------

    public @Nullable MichaelLanceEntity lance() {
        if (lanceId == null || !(level() instanceof ServerLevel level)) return null;
        return level.getEntity(lanceId) instanceof MichaelLanceEntity l && l.isAlive() ? l : null;
    }

    /** He hurls his lance at {@code at}: it leaves his hand until he calls it back. */
    public MichaelLanceEntity throwLance(Vec3 at) {
        MichaelLanceEntity lance = MichaelLanceEntity.hurl(this, at);
        level().addFreshEntity(lance);
        lanceId = lance.getUUID();
        lanceAwaySince = level().getGameTime();
        setLanceHeld(false);
        playSound(AllSounds.MICHAEL_LANCE_THROW.get(), 2.5f, 1.0f);
        return lance;
    }

    /** Calls the lance back; if it is nowhere (and not stolen), it is simply in his hand again. */
    public void recallLance() {
        if (lanceHeld() || lanceStolen) return;
        MichaelLanceEntity lance = lance();
        if (lance != null) lance.recall();
        else lanceReturned();
    }

    /** The lance is back in his hand. */
    public void lanceReturned() {
        lanceId = null;
        lanceStolen = false;
        setLanceHeld(true);
        playSound(AllSounds.MICHAEL_LANCE_RECALL.get(), 1.6f, 1.0f);
    }

    /** {@code hunter} pulled it out of the ground: it is theirs, for now. */
    public void lanceStolen(ServerPlayer hunter) {
        lanceId = null;
        lanceStolen = true;
        playSound(AllSounds.MICHAEL_HURT.get(), 2f, 1.3f);
    }

    public boolean lanceStolen() {
        return lanceStolen;
    }

    /** Ticks since the lance left his hand (0 while he holds it). */
    public long lanceAwayTicks() {
        return lanceHeld() ? 0 : level().getGameTime() - lanceAwaySince;
    }

    /** His own lance, hurled back at him by {@code by}: it bites deep, past his guard, and he reels. */
    public void struckByOwnLance(ServerPlayer by) {
        invulnerableTime = 0;
        uncapped = true;
        try {
            hurt(AllDamageTypes.source(level(), AllDamageTypes.LANCE, by), MichaelBalance.BORROWED_LANCE_DAMAGE);
        } finally {
            uncapped = false;
        }
        if (isAlive() && state() != DYING && state() != TRANSITION) stagger(MichaelBalance.BORROWED_STUN_TICKS, by, "message.supernaturalcraft.michael.parry");
        if (level() instanceof ServerLevel level) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, getX(), getY() + getBbHeight() * 0.6, getZ(), 60, 0.6, 1, 0.6, 0.3);
        }
        playSound(AllSounds.MICHAEL_HALO_BREAK.get(), 3f, 1.2f);
    }

    /** The lance pins {@code who} where they stand for {@code ticks}. */
    public void pin(LivingEntity who, int ticks) {
        pinned.put(who.getUUID(), new Pin(who, who.position(), level().getGameTime() + ticks));
        who.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6, false, false, true));
        if (level() instanceof ServerLevel level) {
            fx(level, new MichaelFxPayload(getId(), MichaelFxPayload.LANCE_PIN, who.getId(), 0, who.position(), ticks));
        }
    }

    /** Whether {@code who} is pinned by the lance now. */
    public boolean isPinned(LivingEntity who) {
        Pin p = pinned.get(who.getUUID());
        return p != null && level().getGameTime() < p.until();
    }

    private void tickPins(ServerLevel level) {
        long now = level.getGameTime();
        pinned.values().removeIf(p -> now >= p.until() || !p.who().isAlive() || p.who().isRemoved());
        for (Pin p : pinned.values()) {
            LivingEntity e = p.who();
            if (e.position().distanceToSqr(p.at()) > 0.3 * 0.3) e.teleportTo(p.at().x, p.at().y, p.at().z);
            e.setDeltaMovement(0, Math.min(0, e.getDeltaMovement().y), 0);
            e.hurtMarked = true;
            if ((now - p.until()) % 5 == 0) {
                for (int i = 0; i < 8; i++) level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, p.at().x, p.at().y + i * 0.6,
                        p.at().z, 1, 0.1, 0.1, 0.1, 0.0);
            }
        }
    }

    // --- attacks -------------------------------------------------------------------------------------

    @Override
    public void onStage(AttackScheduler.Stage stage, @Nullable BossAttack<LuciferEntity> attack) {
        super.onStage(stage, attack);
        if (stage == AttackScheduler.Stage.WINDUP && phase() == MichaelBalance.AERIAL_PHASE && !(attack instanceof MichaelAttacks.Land)) aerialAttacks++;
    }

    /** Quicker between blows without the lance. */
    @Override
    public int attackGap() {
        int gap = super.attackGap();
        return lanceHeld() ? gap : Math.round(gap * MichaelBalance.UNARMED_GAP);
    }

    /** Tests and previews: the next attack he uses, whatever the pool says. */
    public void queue(Supplier<BossAttack<LuciferEntity>> attack) {
        queued = attack;
    }

    /** A hunter he wears is no target; anyone else is, the nearest first. */
    @Override
    public @Nullable LivingEntity attackTarget() {
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (ServerPlayer p : challengers()) {
            if (VesselPossession.worn(p)) continue;
            double d = p.distanceToSqr(this);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best != null ? best : super.attackTarget();
    }

    /** Never Lucifer's own forced moves: a queued attack, or the gap closer when the target runs or hides. */
    @Override
    public @Nullable Supplier<BossAttack<LuciferEntity>> forcedAttack(LivingEntity target) {
        if (queued != null) {
            Supplier<BossAttack<LuciferEntity>> q = queued;
            queued = null;
            return q;
        }
        if (phase() == MichaelBalance.AERIAL_PHASE) {
            if (!landed && aerialAttacks >= MichaelBalance.AERIAL_ATTACKS_BEFORE_LANDING) {
                aerialAttacks = 0;
                return MichaelAttacks.Land::new;
            }
            return null;
        }
        if (!lanceHeld() && !lanceStolen && lanceAwayTicks() > MichaelBalance.LANCE_AWAY_TICKS) return MichaelAttacks.LanceRecall::new;
        if (directing()) return null;
        if (distanceToSqr(target) > 18 * 18 || noSightTicks > 60) {
            noSightTicks = 0;
            return LuciferAttacks.Teleport::new;
        }
        return null;
    }

    /** Sends one of Michael's moments to every hunter near. */
    public void fx(ServerLevel level, MichaelFxPayload payload) {
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 96 * 96) PacketDistributor.sendToPlayer(p, payload);
        }
    }

    // --- GeckoLib -----------------------------------------------------------------------------------

    /**
     * Lucifer's fight triggers clips on {@code action} by his names ({@code emerge}, {@code transform_<n>}, {@code death},
     * each attack's): they are routed to the form he wears, through the aliases; a clip that form lacks is dropped.
     */
    @Override
    public void triggerAnim(@Nullable String controller, String anim) {
        if ("action".equals(controller)) {
            if (isArchangel()) {
                String a = MichaelAnimations.ARCHANGEL_ALIAS.getOrDefault(anim, anim);
                if (!MichaelAnimations.ARCHANGEL_CLIPS.contains(a) || MichaelAnimations.LOOPS.contains(a)) return;
                controller = "archangel";
                anim = a;
            } else {
                String v = MichaelAnimations.VESSEL_ALIAS.getOrDefault(anim, anim);
                if (!MichaelAnimations.VESSEL_CLIPS.contains(v) || MichaelAnimations.LOOPS.contains(v)) return;
                anim = v;
            }
        }
        if (level().isClientSide) {
            var manager = getAnimatableInstanceCache().getManagerForId(getId());
            if (controller != null) manager.tryTriggerAnimation(controller, anim);
            else manager.tryTriggerAnimation(anim);
        } else {
            GeckoLibServices.NETWORK.triggerEntityAnim(this, false, controller, anim);
        }
    }

    private static RawAnimation once(String prefix, String name) {
        return MichaelAnimations.HOLDS.contains(name)
                ? RawAnimation.begin().thenPlayAndHold(prefix + name)
                : RawAnimation.begin().thenPlay(prefix + name);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String v = MichaelAnimations.VESSEL, a = MichaelAnimations.ARCHANGEL;
        RawAnimation idle = RawAnimation.begin().thenLoop(v + "idle"), walk = RawAnimation.begin().thenLoop(v + "walk");
        RawAnimation hover = RawAnimation.begin().thenLoop(v + "hover");
        RawAnimation aIdle = RawAnimation.begin().thenLoop(a + "idle"), aWalk = RawAnimation.begin().thenLoop(a + "walk");
        RawAnimation aHover = RawAnimation.begin().thenLoop(a + "hover");
        controllers.add(new AnimationController<>(this, "base", 6, state -> {
            byte s = state();
            if (s == EMERGING || s == TRANSITION || s == DYING) return PlayState.STOP;
            if (isArchangel()) return state.setAndContinue(isAerialPhase() ? aHover : state.isMoving() ? aWalk : aIdle);
            return state.setAndContinue(isAerialPhase() ? hover : state.isMoving() ? walk : idle);
        }));
        AnimationController<MichaelEntity> vessel = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String name : MichaelAnimations.triggered(MichaelAnimations.VESSEL_CLIPS)) vessel.triggerableAnim(name, once(v, name));
        controllers.add(vessel);
        AnimationController<MichaelEntity> archangel = new AnimationController<>(this, "archangel", 3, state -> PlayState.STOP);
        for (String name : MichaelAnimations.triggered(MichaelAnimations.ARCHANGEL_CLIPS)) archangel.triggerableAnim(name, once(a, name));
        controllers.add(archangel);
    }

    // --- persistence --------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Archangel", isArchangel());
        tag.putByte("Wings", wings());
        tag.putBoolean("HaloBroken", haloBroken());
        tag.putBoolean("LanceHeld", lanceHeld());
        tag.putFloat("HealthScale", healthScale);
        tag.putBoolean("Scaled", scaled);
        tag.putBoolean("RelicsOffered", relicsOffered);
        tag.putBoolean("HostBroken", hostBroken);
        tag.putInt("Reinforcements", reinforcements);
        if (lanceId != null) tag.putUUID("Lance", lanceId);
        tag.putBoolean("LanceStolen", lanceStolen);
        tag.putBoolean("Landed", landed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setArchangel(tag.getBoolean("Archangel"));
        setWings(tag.getByte("Wings"));
        setHaloBroken(tag.getBoolean("HaloBroken"));
        setLanceHeld(!tag.contains("LanceHeld") || tag.getBoolean("LanceHeld"));
        healthScale = tag.contains("HealthScale") ? tag.getFloat("HealthScale") : 1f;
        scaled = tag.getBoolean("Scaled");
        relicsOffered = tag.getBoolean("RelicsOffered");
        hostBroken = tag.getBoolean("HostBroken");
        reinforcements = tag.getInt("Reinforcements");
        lanceId = tag.hasUUID("Lance") ? tag.getUUID("Lance") : null;
        lanceStolen = tag.getBoolean("LanceStolen");
        landed = tag.getBoolean("Landed");
    }
}
