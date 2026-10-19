package org.papiricoh.supernaturalcraft.entity.boss.rowena;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import org.papiricoh.supernaturalcraft.bowl.spell.Taglock;
import org.papiricoh.supernaturalcraft.bowl.witch.CurseEffect;
import org.papiricoh.supernaturalcraft.coven.layout.RowenaHouseLayout;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.entity.witch.FamiliarEntity;
import org.papiricoh.supernaturalcraft.entity.witch.HexedVillagerEntity;
import org.papiricoh.supernaturalcraft.entity.witch.VictimMaster;
import org.papiricoh.supernaturalcraft.entity.witch.WitchMagic;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.network.RowenaFxPayload;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.GeckoLibServices;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Rowena MacLeod as a boss (v0.21, P6): the witch boss for a hunter who kept the Book of the Damned, fought in the front garden of
 * her house at night ({@link ArenaTheme#ROWENA_HOUSE}; {@link RowenaSummoning}). Three phases, a third of her true health each (27 000 alone):
 *
 * <pre>EMERGING (she steps out of yellow smoke: the intro camera) → P1 the Witch at Home (fling, Ignis, Glacies) → P2 the Familiar
 *   and the Hexed (her familiar steals a taglock and she curses down it; hexed victims who heal her if killed) → P3 the
 *   Recitation (she levitates and recites over three bowls; smoke blinks) → DYING (she sinks to her knees)</pre>
 *
 * <p><b>Familiar.</b> {@link RowenaAttacks.CallFamiliar} brings a raven bound to her ({@link FamiliarEntity#bindTo(UUID)}); it
 * steals a taglock as any witch's familiar does, and while it holds one she sends a curse down it every
 * {@link RowenaBalance#HOMING_EVERY} ticks ({@link #tickHoming}). Killing it drops the lock.
 *
 * <p><b>Victims.</b> Her hexed victims ({@link HexedVillagerEntity#serve}) tell her their fate ({@link VictimMaster}): killed, she
 * gains {@link RowenaBalance#VICTIM_HEAL} of her health; freed by Break Hex or salt, she loses {@link RowenaBalance#VICTIM_FREED}.
 *
 * <p><b>Recitation.</b> {@link RowenaAttacks.Recitation}: three {@link RecitationBowlEntity}s (entities: nothing for the arena to
 * restore, no block entity); all broken and she falls, stunned ×{@link RowenaBalance#STUN_VULNERABILITY}.
 *
 * <p>As she dies the spoils go to her hunters ({@link RowenaSpoils}) and the Grand Witch comes for the Book's keeper
 * ({@code coven.ending.GrandWitchNpcEntity}). Optional: she blocks nothing.
 */
public class RowenaEntity extends LuciferEntity implements VictimMaster {

    public static final int MAX_PHASE = RowenaBalance.PHASES;
    public static final int INTRO_TICKS = RowenaCinematics.INTRO_TICKS;

    /** Synced: bit {@link #RECITING} (the {@code recite} loop), {@link #STUNNED} (fallen from the air). */
    private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(RowenaEntity.class, EntityDataSerializers.BYTE);
    public static final int RECITING = 1, STUNNED = 2;

    private boolean emerged;
    private @Nullable UUID familiar;
    private int familiarCooldown, homingTimer, reciteCooldown, stunTicks;
    private final List<UUID> bowls = new ArrayList<>();
    /** Tests: hunters her attacks see beyond the challengers (fake players are not in the level's list). */
    private final List<LivingEntity> tracked = new ArrayList<>();
    private @Nullable Supplier<BossAttack<LuciferEntity>> queued;

    public RowenaEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 800;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, VANILLA_BASE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.FLYING_SPEED, 0.4)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLAGS, (byte) 0);
    }

    private boolean flag(int bit) {
        return (entityData.get(FLAGS) & bit) != 0;
    }

    private void setFlag(int bit, boolean on) {
        byte b = entityData.get(FLAGS);
        entityData.set(FLAGS, (byte) (on ? b | bit : b & ~bit));
    }

    /** She recites over her bowls (synced: the {@code recite} loop). */
    public boolean reciting() {
        return flag(RECITING);
    }

    /** Fallen out of the air, her recitation broken (synced). */
    public boolean stunned() {
        return flag(STUNNED);
    }

    // --- her numbers --------------------------------------------------------------------------------

    @Override
    public int maxPhase() {
        return MAX_PHASE;
    }

    @Override
    protected float threshold(int phase) {
        return RowenaBalance.threshold(phase);
    }

    @Override
    protected double healthPerExtraPlayer() {
        return SNConfig.ROWENA_HEALTH_PER_PLAYER.get();
    }

    @Override
    protected float mundaneMultiplier() {
        return SNConfig.ROWENA_MUNDANE_MULTIPLIER.get().floatValue();
    }

    @Override
    protected float damageFactor() {
        return SNConfig.ROWENA_DAMAGE_FACTOR.get().floatValue();
    }

    /** Phase III she floats over her bowls (unless she has just been knocked out of the air). */
    @Override
    public boolean isAerialPhase() {
        return phase() >= MAX_PHASE && stunTicks <= 0 && state() != DYING && state() != EMERGING;
    }

    /** Over her front garden (open sky): {@link RowenaBalance#HOVER_HEIGHT} up, a {@link RowenaBalance#HOVER_RADIUS} orbit, inside the hedges. */
    @Override
    protected double hoverHeight() {
        return RowenaBalance.HOVER_HEIGHT;
    }

    @Override
    protected double hoverRadius() {
        return RowenaBalance.HOVER_RADIUS;
    }

    @Override
    protected double hoverBob() {
        return 0.15;
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
        return RowenaAttacks.pool(phase);
    }

    @Override
    protected int baseGap(int phase) {
        return RowenaBalance.attackGap(phase);
    }

    @Override
    protected String animationPrefix() {
        return "animation.rowena.";
    }

    @Override
    protected List<String> triggeredAnimations() {
        return RowenaAssets.TRIGGERED;
    }

    /** {@code entity.supernaturalcraft.rowena.bar.phase<n>}: the HUD's key for her bar. */
    @Override
    protected String bossBarKey(int phase) {
        return "entity.supernaturalcraft.rowena.bar.phase" + phase;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return switch (phase) {
            case 1 -> BossEvent.BossBarColor.GREEN;
            case 2 -> BossEvent.BossBarColor.RED;
            default -> BossEvent.BossBarColor.PURPLE;
        };
    }

    @Override
    protected Component bossBarName(int phase) {
        return Component.translatable(bossBarKey(phase)).withStyle(phase == MAX_PHASE ? ChatFormatting.DARK_PURPLE : ChatFormatting.DARK_GREEN);
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return phase == 2 ? ParticleTypes.FLAME : ParticleTypes.WITCH;
    }

    @Override
    protected SoundEvent emergeSound() {
        return AllSounds.coven("rowena.teleport");
    }

    @Override
    protected SoundEvent roarSound() {
        return AllSounds.coven("rowena.laugh");
    }

    @Override
    protected SoundEvent transformSound() {
        return AllSounds.coven("rowena.laugh");
    }

    @Override
    protected SoundEvent ambientBossSound() {
        return AllSounds.coven("rowena.ambient");
    }

    @Override
    protected SoundEvent dyingSound() {
        return AllSounds.coven("rowena.death");
    }

    @Override
    protected SoundEvent deflectSound() {
        return AllSounds.coven("rowena.laugh");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.coven("rowena.hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.coven("rowena.death");
    }

    @Override
    protected void clientEmergenceParticles() {
        level().addParticle(ParticleTypes.WITCH, getRandomX(0.8), getY() + random.nextDouble() * 2, getRandomZ(0.8), 0, 0.05, 0);
    }

    @Override
    protected void dyingParticles(ServerLevel level, boolean last) {
        if (last) {
            level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 1, getZ(), 80, 0.5, 1.0, 0.5, 0.08);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 40, 0.4, 0.8, 0.4, 0.04);
            return;
        }
        level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 1.1, getZ(), 3, 0.25, 0.5, 0.25, 0.02);
    }

    // --- the fight's shape --------------------------------------------------------------------------

    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        return LuciferSummoning.openArena(level, blockPosition(), RowenaHouseLayout.ARENA_RADIUS, ArenaTheme.ROWENA_HOUSE);
    }

    @Override
    protected void playEmergence() {
        triggerAnim("action", "teleport");
        if (level() instanceof ServerLevel level) {
            MagicFx.teleport(level, this, School.WITCHCRAFT, position(), position(), MagicFxPayload.SMOKE_YELLOW, 40);
        }
        RowenaCinematics.intro(this);
    }

    @Override
    protected void playTransition(int to) {
        RowenaCinematics.transition(this, to);
    }

    @Override
    protected void playDeath() {
        RowenaCinematics.death(this);
    }

    @Override
    protected void onTransitionStart(int to) {
        settle();
        if (to == 2) familiarCooldown = 0;
        if (to >= MAX_PHASE) reciteCooldown = 60;
    }

    @Override
    protected void tickTransitionMotion(int elapsed, boolean last) {
        setDeltaMovement(0, last ? 0.03 : Math.min(0, getDeltaMovement().y), 0);
        getNavigation().stop();
    }

    @Override
    protected void tickEmergence() {
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
    }

    @Override
    protected int deathTicks() {
        return RowenaBalance.DEATH_TICKS;
    }

    @Override
    protected void tickDyingMotion(int elapsed) {
        setNoGravity(false);
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
    }

    /** The fight was lost: she laughs and the smoke takes her; nothing is left behind (a stake, if any, comes back by itself). */
    @Override
    protected void leaveBehind(ServerLevel level, Vec3 at) {
    }

    @Override
    protected void returnToCage(ServerLevel level, String messageKey) {
        settle();
        super.returnToCage(level, messageKey.replace(".lucifer.", ".rowena."));
    }

    /** She sinks; her familiar and her victims go with her. */
    @Override
    protected void beginDying() {
        if (level() instanceof ServerLevel level) {
            settle();
            for (UUID id : minions()) {
                Entity e = level.getEntity(id);
                if (e == null) continue;
                level.sendParticles(ParticleTypes.WITCH, e.getX(), e.getY() + 1, e.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
                e.discard();
            }
        }
        super.beginDying();
    }

    /** At the end of her death: the victory, the spoils, and the Grand Witch for the Book's keeper. */
    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        RowenaCinematics.victory(this);
        List<ServerPlayer> hunters = new ArrayList<>(challengers());
        for (LivingEntity e : tracked) if (e instanceof ServerPlayer p && !hunters.contains(p)) hunters.add(p);
        RowenaSpoils.drop(level, this, hunters, position().add(0, 1.2, 0));
        RowenaSpoils.grandWitchComes(level, arena.centerVec(), hunters);
    }

    @Override
    protected boolean walks() {
        return stunTicks <= 0 && !reciting();
    }

    @Override
    protected void customServerAiStep() {
        if (level() instanceof ServerLevel level && !isRemoved()) tickWitch(level);
        super.customServerAiStep();
        if (isRemoved() || !(level() instanceof ServerLevel)) return;
        if (!isAerialPhase()) setNoGravity(false);
        if (state() == EMERGING || emerged) return;
        emerged = true;
        fx(new RowenaFxPayload(getId(), RowenaFxPayload.TITLE, phase(), 0, position(), 70));
    }

    /** Her own states before the shared fight: the stun, her familiar's theft. */
    private void tickWitch(ServerLevel level) {
        byte s = state();
        if (s == EMERGING || s == DYING || s == TRANSITION) return;
        if (familiarCooldown > 0) familiarCooldown--;
        if (reciteCooldown > 0) reciteCooldown--;
        if (stunTicks > 0) {
            getNavigation().stop();
            setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
            scheduler().delay(5);
            if (tickCount % 5 == 0) level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.8, getZ(), 3, 0.3, 0.2, 0.3, 0.01);
            if (--stunTicks <= 0) setFlag(STUNNED, false);
        }
        tickHoming(level);
    }

    // --- phase II: the familiar ---------------------------------------------------------------------

    public @Nullable FamiliarEntity familiar() {
        if (familiar == null || !(level() instanceof ServerLevel level)) return null;
        return level.getEntity(familiar) instanceof FamiliarEntity f && f.isAlive() ? f : null;
    }

    public boolean mayCallFamiliar() {
        return familiar() == null && familiarCooldown <= 0;
    }

    /** A raven answers her, bound to her (it steals for her). */
    public @Nullable FamiliarEntity callFamiliar() {
        if (!(level() instanceof ServerLevel level)) return null;
        FamiliarEntity f = AllEntities.FAMILIAR.get().create(level);
        if (f == null) return null;
        f.setVariant(FamiliarEntity.Variant.RAVEN);
        Vec3 at = position().add(random.nextDouble() * 2 - 1, 1.6, random.nextDouble() * 2 - 1);
        f.moveTo(at.x, at.y, at.z, getYRot(), 0);
        f.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        f.setPersistenceRequired();
        f.bindTo(getUUID());
        level.addFreshEntity(f);
        familiar = f.getUUID();
        minions().add(f.getUUID());
        familiarCooldown = RowenaBalance.FAMILIAR_AGAIN;
        homingTimer = RowenaBalance.HOMING_EVERY;
        MagicFx.teleport(level, f, School.WITCHCRAFT, at, at, MagicFxPayload.SMOKE_WITCH, 30);
        fx(new RowenaFxPayload(getId(), RowenaFxPayload.FAMILIAR, f.getId(), 0, at, 30));
        return f;
    }

    /** The hunter whose taglock her familiar holds, if they are about. */
    public @Nullable LivingEntity robbed() {
        FamiliarEntity f = familiar();
        Taglock lock = f == null ? null : f.stolen();
        if (lock == null) return null;
        for (LivingEntity h : hunters()) if (h.getUUID().equals(lock.owner())) return h;
        var p = org.papiricoh.supernaturalcraft.util.Players.byUuid(level(), lock.owner());
        return p != null && p.isAlive() && p.distanceToSqr(this) < 96 * 96 ? p : null;
    }

    private void tickHoming(ServerLevel level) {
        if (phase() < 2) return;
        LivingEntity victim = robbed();
        if (victim == null) {
            homingTimer = Math.max(homingTimer, RowenaBalance.HOMING_EVERY / 3);
            return;
        }
        if (--homingTimer > 0) return;
        homingTimer = RowenaBalance.HOMING_EVERY;
        sendCurse(level, victim);
    }

    /** One curse down the stolen lock: it finds {@code victim} wherever they hide. */
    public void sendCurse(ServerLevel level, LivingEntity victim) {
        CurseEffect.Curse curse = random.nextBoolean() ? CurseEffect.Curse.PAIN : CurseEffect.Curse.FRAILTY;
        Vec3 hands = position().add(0, 1.3, 0);
        int travel = (int) Math.max(8, Math.min(30, hands.distanceTo(victim.position()) * 1.5));
        MagicFx.bowlSend(level, School.WITCHCRAFT, hands, victim.position().add(0, victim.getBbHeight() / 2, 0), victim, -1, travel);
        fx(new RowenaFxPayload(getId(), RowenaFxPayload.HOMING, victim.getId(), curse.ordinal(), hands, travel));
        WitchMagic.curse(this, victim, curse, RowenaBalance.HOMING_CURSE_TICKS);
        LuciferAttacks.hit(this, victim, AllDamageTypes.SPELL, RowenaBalance.HOMING_DAMAGE);
        playSound(AllSounds.coven("rowena.laugh"), 1.2f, 1.2f);
    }

    // --- phase II: the hexed ------------------------------------------------------------------------

    /** Her living hexed victims. */
    public List<HexedVillagerEntity> victims() {
        List<HexedVillagerEntity> out = new ArrayList<>();
        if (!(level() instanceof ServerLevel level)) return out;
        for (UUID id : minions()) if (level.getEntity(id) instanceof HexedVillagerEntity v && v.isAlive() && !v.freeing()) out.add(v);
        return out;
    }

    /** Raises up to {@code n} hexed victims round her. @return how many rose */
    public int summonVictims(int n) {
        if (!(level() instanceof ServerLevel level)) return 0;
        int room = RowenaBalance.MAX_VICTIMS - victims().size(), made = 0;
        double base = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < Math.min(n, room); i++) {
            HexedVillagerEntity v = AllEntities.HEXED_VILLAGER.get().create(level);
            if (v == null) continue;
            double a = base + i * Math.PI * 2 / Math.max(1, n);
            Vec3 s = LuciferAttacks.floorAt(this, position().add(Math.cos(a) * 4, 0, Math.sin(a) * 4));
            v.moveTo(s.x, s.y, s.z, random.nextFloat() * 360, 0);
            v.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(s)), MobSpawnType.MOB_SUMMONED, null);
            v.serve(getUUID());
            LivingEntity t = attackTarget();
            if (t != null) v.setTarget(t);
            level.addFreshEntity(v);
            minions().add(v.getUUID());
            MagicFx.hex(level, v, s.add(0, 1, 0), MagicFxPayload.HEX_CURSE, 1.4f, 30);
            fx(new RowenaFxPayload(getId(), RowenaFxPayload.VICTIMS, v.getId(), 0, s, 30));
            made++;
        }
        return made;
    }

    @Override
    public void victimKilled(HexedVillagerEntity victim, @Nullable Entity by) {
        if (state() == DYING || isInvulnerablePhase()) return;
        gainShare(RowenaBalance.VICTIM_HEAL);
        fx(new RowenaFxPayload(getId(), RowenaFxPayload.VICTIM_FATE, victim.getId(), 0, victim.position(), 20));
        if (level() instanceof ServerLevel level) {
            MagicFx.hex(level, this, position().add(0, 1, 0), MagicFxPayload.HEX_PROTECT, 1.2f, 20);
            tell(level, "message.supernaturalcraft.rowena.victim_killed");
        }
        playSound(AllSounds.coven("rowena.laugh"), 1.5f, 1f);
    }

    @Override
    public void victimFreed(HexedVillagerEntity victim, @Nullable Entity by) {
        if (state() == DYING || isInvulnerablePhase()) return;
        loseShare(RowenaBalance.VICTIM_FREED);
        fx(new RowenaFxPayload(getId(), RowenaFxPayload.VICTIM_FATE, victim.getId(), 1, victim.position(), 20));
        if (level() instanceof ServerLevel level) {
            MagicFx.hex(level, this, position().add(0, 1, 0), MagicFxPayload.HEX_CURSE, 1.2f, 20);
            tell(level, "message.supernaturalcraft.rowena.victim_freed");
        }
        playSound(AllSounds.coven("rowena.hurt"), 1.5f, 0.9f);
    }

    /** Takes {@code share} of her vanilla health through her own pipeline (never past the phase's floor). */
    public void loseShare(float share) {
        float floor = vanillaFloor();
        setHealth(Math.max(floor, getHealth() - getMaxHealth() * share));
        acceptHealth();
    }

    /** Gives her back {@code share} of her vanilla health (never above where this phase began). */
    public void gainShare(float share) {
        int phase = phase();
        float cap = phase > 1 ? getMaxHealth() * threshold(phase - 1) : getMaxHealth();
        setHealth(Math.min(cap, getHealth() + getMaxHealth() * share));
        acceptHealth();
    }

    // --- phase III: the recitation ------------------------------------------------------------------

    public boolean mayRecite() {
        return reciteCooldown <= 0 && !reciting() && stunTicks <= 0;
    }

    /** Sets her three bowls round the arena and begins to recite. @return the bowls */
    public List<RecitationBowlEntity> beginRecitation() {
        List<RecitationBowlEntity> out = new ArrayList<>();
        if (!(level() instanceof ServerLevel level)) return out;
        clearBowls(level);
        ArenaController arena = arena();
        Vec3 c = arena != null ? arena.centerVec() : position();
        double base = random.nextDouble() * Math.PI * 2;
        double d = arena != null ? Math.min(RowenaBalance.BOWL_DISTANCE, arena.radius() - 2.5) : RowenaBalance.BOWL_DISTANCE;
        for (int i = 0; i < RowenaBalance.BOWLS; i++) {
            double a = base + i * Math.PI * 2 / RowenaBalance.BOWLS;
            Vec3 s = LuciferAttacks.floorAt(this, c.add(Math.cos(a) * d, 0, Math.sin(a) * d));
            RecitationBowlEntity bowl = AllEntities.RECITATION_BOWL.get().create(level);
            if (bowl == null) continue;
            bowl.moveTo(s.x, s.y, s.z, 0, 0);
            bowl.serve(this);
            level.addFreshEntity(bowl);
            bowls.add(bowl.getUUID());
            minions().add(bowl.getUUID());
            MagicFx.bowlSeal(level, School.WITCHCRAFT, s.add(0, 0.6, 0), this, -1, 40);
            out.add(bowl);
        }
        setFlag(RECITING, true);
        return out;
    }

    /** Her bowls still whole. */
    public List<RecitationBowlEntity> bowls() {
        List<RecitationBowlEntity> out = new ArrayList<>();
        if (!(level() instanceof ServerLevel level)) return out;
        for (UUID id : bowls) if (level.getEntity(id) instanceof RecitationBowlEntity b && b.isAlive()) out.add(b);
        return out;
    }

    /** A bowl was smashed. */
    public void bowlBroken(RecitationBowlEntity bowl) {
        int left = (int) bowls().stream().filter(b -> b != bowl).count();
        fx(new RowenaFxPayload(getId(), RowenaFxPayload.BOWL_BROKEN, bowl.getId(), left, bowl.position(), 20));
        playSound(AllSounds.coven("rowena.hurt"), 1.2f, 1.2f);
    }

    /** All three broken: she drops out of the air, stunned. */
    public void recitationBroken() {
        stunTicks = RowenaBalance.STUN_TICKS;
        setFlag(STUNNED, true);
        setFlag(RECITING, false);
        setNoGravity(false);
        if (level() instanceof ServerLevel level) tell(level, "message.supernaturalcraft.rowena.recitation_broken");
        playSound(AllSounds.coven("rowena.hurt"), 2.5f, 0.7f);
    }

    /** The recitation is over (spoken, broken, or cut short): the bowls go, and she will not start another for a while. */
    public void endRecitation() {
        setFlag(RECITING, false);
        reciteCooldown = RowenaBalance.RECITE_AGAIN;
        if (level() instanceof ServerLevel level) clearBowls(level);
    }

    private void clearBowls(ServerLevel level) {
        for (UUID id : bowls) {
            Entity e = level.getEntity(id);
            if (e != null) e.discard();
            minions().remove(id);
        }
        bowls.clear();
    }

    /** Out of every lasting state (a change of phase, her death, the fight lost). */
    private void settle() {
        stunTicks = 0;
        setFlag(STUNNED, false);
        setFlag(RECITING, false);
        if (level() instanceof ServerLevel level) clearBowls(level);
    }

    // --- damage -------------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        Entity by = source.getEntity();
        if (by instanceof HexedVillagerEntity || by instanceof FamiliarEntity) return false;
        return super.hurt(source, amount);
    }

    /** ×{@link RowenaBalance#STUN_VULNERABILITY} while she lies stunned. */
    @Override
    protected float vulnerability(DamageSource source) {
        return super.vulnerability(source) * RowenaBalance.share(stunTicks > 0);
    }

    // --- hunters, effects ---------------------------------------------------------------------------

    /** The hunters her attacks see: the challengers, and anyone tracked (tests). */
    public List<LivingEntity> hunters() {
        List<LivingEntity> out = new ArrayList<>(challengers());
        tracked.removeIf(e -> !e.isAlive() || e.isRemoved());
        for (LivingEntity e : tracked) if (!out.contains(e)) out.add(e);
        return out;
    }

    /** Test hook: {@code e} counts as one of her hunters. */
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

    /** Sends one of the fight's moments to every hunter near. */
    public void fx(RowenaFxPayload payload) {
        if (!(level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 96 * 96) Send.toPlayer(p, payload);
        }
    }

    void tell(ServerLevel level, String key) {
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 64 * 64) p.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_GREEN), true);
        }
    }

    // --- attacks ------------------------------------------------------------------------------------

    public void queue(Supplier<BossAttack<LuciferEntity>> attack) {
        queued = attack;
    }

    /** A queued attack first; the recitation when its time comes; a smoke blink when the hunter runs or hides. */
    @Override
    public @Nullable Supplier<BossAttack<LuciferEntity>> forcedAttack(LivingEntity target) {
        if (queued != null) {
            Supplier<BossAttack<LuciferEntity>> q = queued;
            queued = null;
            return q;
        }
        if (phase() >= MAX_PHASE && mayRecite()) return RowenaAttacks.Recitation::new;
        if (phase() >= 2 && mayCallFamiliar()) return RowenaAttacks.CallFamiliar::new;
        if (distanceToSqr(target) > 18 * 18 || noSightTicks > 60) {
            noSightTicks = 0;
            return RowenaAttacks.SmokeBlink::new;
        }
        return null;
    }

    // --- GeckoLib -----------------------------------------------------------------------------------

    /** Only her own clips: Lucifer's names that she lacks ({@code emerge}, {@code transform_<n>}, …) are dropped. */
    @Override
    public void triggerAnim(@Nullable String controller, String anim) {
        if ("action".equals(controller) && !RowenaAssets.TRIGGERED.contains(anim)) return;
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
                levitate = RawAnimation.begin().thenLoop(pre + "levitate"), recite = RawAnimation.begin().thenLoop(pre + "recite");
        controllers.add(new AnimationController<>(this, "base", 6, state -> {
            if (state() == DYING) return PlayState.STOP;
            if (reciting()) return state.setAndContinue(recite);
            if (phase() >= MAX_PHASE && !stunned() && state() != EMERGING) return state.setAndContinue(levitate);
            return state.setAndContinue(state.isMoving() ? walk : idle);
        }));
        AnimationController<RowenaEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String name : RowenaAssets.TRIGGERED) {
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
        if (familiar != null) tag.putUUID("Familiar", familiar);
        tag.putInt("FamiliarCooldown", familiarCooldown);
        tag.putInt("ReciteCooldown", reciteCooldown);
        tag.putInt("Stun", stunTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        emerged = tag.getBoolean("Emerged");
        familiar = tag.hasUUID("Familiar") ? tag.getUUID("Familiar") : null;
        familiarCooldown = tag.getInt("FamiliarCooldown");
        reciteCooldown = tag.getInt("ReciteCooldown");
        stunTicks = tag.getInt("Stun");
        entityData.set(FLAGS, (byte) (stunTicks > 0 ? STUNNED : 0));
    }
}
