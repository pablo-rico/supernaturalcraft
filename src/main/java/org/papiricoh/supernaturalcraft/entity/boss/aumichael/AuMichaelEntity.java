package org.papiricoh.supernaturalcraft.entity.boss.aumichael;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.apocworld.ApocWorldRegistry;
import org.papiricoh.supernaturalcraft.apocworld.fortress.FortressSite;
import org.papiricoh.supernaturalcraft.apocworld.rift.AuRift;
import org.papiricoh.supernaturalcraft.apocworld.rift.AuRifts;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The Michael of the Apocalypse World (v0.41, season 13): the general who won his war, at the throne of his fortress. Four phases
 * ({@link AuMichaelBalance}, {@link AuMichaelAttacks}): his blade and the smite; his wings, the gale and his soldiers; the burned
 * sky's lightning; and the air over his court.
 *
 * <p><b>He never dies here.</b> At the last floor he breaks off ({@link #interceptDeath}): he goes down on one knee, a rift tears
 * open behind his throne, Jack's grace flares gold at the hunters' sides and drives him back, and he steps into the rift and is
 * gone ({@link #fleeing()}). Only then does his "death" run (short, unseen), so his fall still counts as a boss's for everyone who
 * fought him (credit, shards, the advancement), and {@link AuMichaelSpoils} hands out the rest and moves the road on.
 */
public class AuMichaelEntity extends LuciferEntity {

    private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(AuMichaelEntity.class, EntityDataSerializers.BYTE);
    public static final int WINGS = 1, EYES = 2, FLEEING = 4, GONE = 8;
    public static final ResourceLocation ENTRANCE = SupernaturalCraft.asResource("au_michael_entrance"),
            FLIGHT = SupernaturalCraft.asResource("au_michael_flight");
    /** How long his entrance and his transitions last (ticks). */
    public static final int EMERGE = 90, TRANSITION = 60, LAST_TRANSITION = 100;
    private static final int TAUNTS = 5;

    private @Nullable BlockPos fortress;
    private int fortressQ;
    private int fleeTicks = -1, taunt;
    private @Nullable Vec3 fleeFrom;
    private @Nullable BlockPos riftAt;

    public AuMichaelEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 600;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, VANILLA_BASE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.29)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.FOLLOW_RANGE, 56.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
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

    /** His wings are out (phase II on). */
    public boolean wingsOut() {
        return flag(WINGS);
    }

    /** His blue-white eyes show. */
    public boolean eyesLit() {
        return flag(EYES);
    }

    /** He has broken off and is fleeing through the rift. */
    public boolean fleeing() {
        return flag(FLEEING);
    }

    /** He has stepped into the rift: nothing of him is left to see. */
    public boolean gone() {
        return flag(GONE);
    }

    // --- set-up -------------------------------------------------------------------------------------------------------------

    /** At his fortress: his soldiers' doors and the rift's alcove are its. */
    public void bindFortress(BlockPos origin, int q) {
        fortress = origin.immutable();
        fortressQ = q;
    }

    public @Nullable FortressSite site() {
        return fortress == null ? null : new FortressSite(fortress, fortressQ);
    }

    public List<Vec3> spawnPoints() {
        FortressSite s = site();
        if (s != null) return s.spawns();
        List<Vec3> out = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 2 * i + Math.PI / 4;
            out.add(position().add(Math.cos(a) * 9, 0, Math.sin(a) * 9));
        }
        return out;
    }

    /** His soldiers standing. */
    public int soldiers() {
        if (!(level() instanceof ServerLevel level)) return 0;
        int n = 0;
        for (UUID id : minions()) if (level.getEntity(id) instanceof LivingEntity m && m.isAlive()) n++;
        return n;
    }

    public void fx(byte kind, int arg, int arg2, Vec3 at, int duration) {
        if (!(level() instanceof ServerLevel level)) return;
        AuMichaelFxPayload p = new AuMichaelFxPayload(getId(), kind, arg, arg2, at, duration);
        for (ServerPlayer pl : level.players()) if (pl.distanceToSqr(this) < 128 * 128) Send.toPlayer(pl, p);
    }

    /** A bolt of the burned sky at {@code at} (visual: the blow is the attack's). */
    public void bolt(Vec3 at) {
        if (!(level() instanceof ServerLevel level)) return;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(at.x, at.y, at.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        fx(AuMichaelFxPayload.BOLT, 0, 0, at, 6);
    }

    /** One of his lines to the hunters near (they come round in order). */
    public void taunt() {
        if (!(level() instanceof ServerLevel level)) return;
        String key = "message.supernaturalcraft.au_michael.taunt." + (taunt++ % TAUNTS);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 64 * 64) p.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
        }
    }

    // --- his numbers --------------------------------------------------------------------------------------------------------

    @Override
    public int maxPhase() {
        return AuMichaelBalance.PHASES;
    }

    @Override
    protected float threshold(int phase) {
        return AuMichaelBalance.threshold(phase);
    }

    @Override
    protected double healthPerExtraPlayer() {
        return SNConfig.MICHAEL_HEALTH_PER_PLAYER.get();
    }

    @Override
    protected float mundaneMultiplier() {
        return SNConfig.MICHAEL_MUNDANE_MULTIPLIER.get().floatValue();
    }

    @Override
    protected float damageFactor() {
        return SNConfig.MICHAEL_DAMAGE_FACTOR.get().floatValue();
    }

    @Override
    public boolean isAerialPhase() {
        return AuMichaelBalance.aerial(phase()) && !fleeing();
    }

    @Override
    protected double hoverHeight() {
        return AuMichaelBalance.HOVER_HEIGHT;
    }

    @Override
    protected double hoverRadius() {
        return AuMichaelBalance.HOVER_RADIUS;
    }

    @Override
    public float scale(int phase) {
        return 1f;
    }

    @Override
    protected int emergeTicks() {
        return EMERGE;
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return fleeing() ? List.of() : AuMichaelAttacks.pool(phase);
    }

    @Override
    protected int baseGap(int phase) {
        return AuMichaelBalance.attackGap(phase);
    }

    @Override
    public @Nullable Supplier<BossAttack<LuciferEntity>> forcedAttack(LivingEntity target) {
        if (fleeing()) return null;
        if (!isAerialPhase() && (distanceToSqr(target) > 18 * 18 || noSightTicks > 60)) {
            noSightTicks = 0;
            return AuMichaelAttacks.WingStep::new;
        }
        return null;
    }

    @Override
    protected String animationPrefix() {
        return "animation." + AuMichaelAssets.RIG + ".";
    }

    @Override
    protected List<String> triggeredAnimations() {
        return AuMichaelAssets.TRIGGERED;
    }

    @Override
    protected String bossBarKey(int phase) {
        return "entity.supernaturalcraft." + AuMichaelAssets.RIG + ".bar.phase" + phase;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return switch (phase) {
            case 1 -> BossEvent.BossBarColor.WHITE;
            case 2 -> BossEvent.BossBarColor.YELLOW;
            case 3 -> BossEvent.BossBarColor.PURPLE;
            default -> BossEvent.BossBarColor.BLUE;
        };
    }

    @Override
    protected Component bossBarName(int phase) {
        return Component.translatable(bossBarKey(phase)).withStyle(phase >= 3 ? ChatFormatting.AQUA : ChatFormatting.GOLD);
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return phase >= 3 ? ParticleTypes.ELECTRIC_SPARK : AllParticles.GRACE.get();
    }

    @Override
    protected SoundEvent emergeSound() {
        return ApocWorldRegistry.michael("gale");
    }

    @Override
    protected SoundEvent roarSound() {
        return ApocWorldRegistry.michael("smite");
    }

    @Override
    protected SoundEvent transformSound() {
        return ApocWorldRegistry.michael("thunder");
    }

    @Override
    protected SoundEvent ambientBossSound() {
        return ApocWorldRegistry.michael("gale");
    }

    @Override
    protected SoundEvent dyingSound() {
        return ApocWorldRegistry.michael("flee");
    }

    @Override
    protected SoundEvent deflectSound() {
        return ApocWorldRegistry.michael("hurt");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ApocWorldRegistry.michael("hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ApocWorldRegistry.michael("flee");
    }

    @Override
    protected void clientEmergenceParticles() {
        level().addParticle(AllParticles.GRACE.get(), getRandomX(1.2), getY() + random.nextDouble() * 2, getRandomZ(1.2), 0, 0.04, 0);
    }

    @Override
    protected void dyingParticles(ServerLevel level, boolean last) {
        // He is already gone through the rift: nothing of him to light.
    }

    @Override
    protected int deathTicks() {
        return 4;
    }

    // --- the fight's shape --------------------------------------------------------------------------------------------------

    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        return LuciferSummoning.openArena(level, blockPosition(), AuMichaelSummoning.FREE_RADIUS, ArenaTheme.FORTRESS);
    }

    @Override
    protected void applyTerrain(ServerLevel level, ArenaController arena, int phase) {
    }

    @Override
    protected void playEmergence() {
        fx(AuMichaelFxPayload.TITLE, 1, 0, position(), 80);
        if (site() != null) CinematicLocks.play(this, ENTRANCE, EMERGE, 64);
    }

    @Override
    protected void playTransition(int to) {
        fx(AuMichaelFxPayload.TITLE, to, 0, position(), 70);
        triggerAnim("action", to == 3 ? "lightning" : to == 2 ? "wing_beat" : "command");
        if (to == 2 || to == 4) taunt();
    }

    @Override
    protected void playDeath() {
    }

    @Override
    protected int transitionTicks(int to) {
        return to == maxPhase() ? LAST_TRANSITION : TRANSITION;
    }

    @Override
    protected void onTransitionStart(int to) {
        super.onTransitionStart(to);
        setFlag(WINGS, AuMichaelBalance.wings(to));
        setFlag(EYES, to >= 3);
    }

    @Override
    protected void tickTransitionMotion(int elapsed, boolean last) {
        if (last) setDeltaMovement(0, elapsed < 60 ? 0.07 : 0, 0);
        else setDeltaMovement(0, getDeltaMovement().y, 0);
    }

    @Override
    protected boolean walks() {
        return !fleeing();
    }

    @Override
    protected void leaveBehind(ServerLevel level, Vec3 at) {
    }

    @Override
    protected void returnToCage(ServerLevel level, String messageKey) {
        super.returnToCage(level, "message.supernaturalcraft.au_michael.left");
    }

    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        AuMichaelSpoils.fled(level, this);
    }

    // --- his flight ---------------------------------------------------------------------------------------------------------

    @Override
    protected boolean interceptDeath() {
        scheduler().cancel();
        getNavigation().stop();
        setFlag(FLEEING, true);
        setFlag(EYES, true);
        setNoGravity(true);
        fleeTicks = 0;
        fleeFrom = position();
        triggerAnim("action", "death");
        fx(AuMichaelFxPayload.TITLE, 0, 1, position(), 90);
        if (level() instanceof ServerLevel level) openFleeRift(level);
        return true;
    }

    /** The rift he leaves by: in the alcove behind his throne at his fortress, else a little behind him. */
    private void openFleeRift(ServerLevel level) {
        FortressSite s = site();
        BlockPos at;
        int ticks;
        if (s != null) {
            at = s.rift();
            ticks = AuMichaelBalance.FLEE_RIFT_TICKS;
        } else {
            Vec3 back = position().add(Vec3.directionFromRotation(0, getYRot()).scale(-4));
            at = AuRifts.groundNear(level, BlockPos.containing(back), getBlockY());
            ticks = AuMichaelBalance.GONE_AT + 200;
        }
        AuRift near = AuRifts.near(level, at, 4, AuRift.Kind.HOME);
        if (near == null) AuRifts.open(level, at, s != null ? axisFor(s) : Direction.Axis.X, AuRift.Kind.HOME, ticks);
        riftAt = at.immutable();
    }

    private static Direction.Axis axisFor(FortressSite s) {
        BlockPos t = s.throne(), r = s.rift();
        return Math.abs(r.getX() - t.getX()) > Math.abs(r.getZ() - t.getZ()) ? Direction.Axis.Z : Direction.Axis.X;
    }

    private void tickFlight(ServerLevel level) {
        if (gone()) return;
        int t = fleeTicks++;
        setDeltaMovement(Vec3.ZERO);
        getNavigation().stop();
        if (t == AuMichaelBalance.JACK_AT) {
            CinematicLocks.play(this, FLIGHT, AuMichaelBalance.GONE_AT - AuMichaelBalance.JACK_AT + 20, 64);
            for (ServerPlayer p : challengers()) {
                fx(AuMichaelFxPayload.JACK, p.getId(), 0, p.position(), 60);
                p.displayClientMessage(Component.translatable("message.supernaturalcraft.au_michael.jack").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
            }
        }
        if (t >= AuMichaelBalance.JACK_AT && t < AuMichaelBalance.TURN_AT && t % 2 == 0) {
            // Jack's grace, gold, from each hunter's side into him.
            for (ServerPlayer p : challengers()) {
                Vec3 from = p.position().add(0, 1.2, 0), to = position().add(0, 1.2, 0);
                for (int i = 0; i <= 8; i++) {
                    Vec3 q = from.lerp(to, i / 8.0);
                    level.sendParticles(AllParticles.GRACE.get(), q.x, q.y, q.z, 1, 0.05, 0.05, 0.05, 0);
                }
            }
        }
        if (t == AuMichaelBalance.TURN_AT) {
            triggerAnim("action", "rift_flee");
            if (riftAt != null) {
                Vec3 r = Vec3.atBottomCenterOf(riftAt);
                float yaw = (float) Math.toDegrees(Math.atan2(-(r.x - getX()), r.z - getZ()));
                setYRot(yaw);
                setYBodyRot(yaw);
                setYHeadRot(yaw);
            }
        }
        if (t > AuMichaelBalance.TURN_AT && riftAt != null && fleeFrom != null) {
            double k = Math.min(1, (t - AuMichaelBalance.TURN_AT) / (double) (AuMichaelBalance.GONE_AT - AuMichaelBalance.TURN_AT));
            Vec3 to = Vec3.atBottomCenterOf(riftAt).add(0, 0.2, 0);
            Vec3 at = fleeFrom.lerp(to, k * k);
            setPos(at.x, at.y, at.z);
        }
        if (t >= AuMichaelBalance.GONE_AT) {
            Vec3 r = riftAt != null ? Vec3.atBottomCenterOf(riftAt) : position();
            fx(AuMichaelFxPayload.FLEE, 0, 0, r, 30);
            level.sendParticles(ParticleTypes.FLASH, r.x, r.y + 2, r.z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, r.x, r.y + 2, r.z, 40, 0.6, 1.5, 0.6, 0.04);
            setFlag(GONE, true);
            setInvisible(true);
            beginDying();
        }
    }

    // --- damage -------------------------------------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        if (fleeing() && !org.papiricoh.supernaturalcraft.entity.boss.BossDamage.passesThrough(source)) return false;
        return super.hurt(source, amount);
    }

    // --- per tick -----------------------------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        if (level() instanceof ServerLevel level && fleeing() && state() != DYING && !isRemoved()) {
            tickFlight(level);
            // Keep his arena and his bar alive (his pool is empty while he flees: no attack starts).
            super.customServerAiStep();
            if (!gone()) setDeltaMovement(Vec3.ZERO);
            return;
        }
        super.customServerAiStep();
        if (isRemoved() || !(level() instanceof ServerLevel)) return;
        setFlag(WINGS, AuMichaelBalance.wings(phase()));
        if (phase() >= 3 || scheduler().current() != null && scheduler().current().id.equals("smite")) setFlag(EYES, true);
        else if (phase() < 3) setFlag(EYES, false);
    }

    // --- animation ----------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = animationPrefix();
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + "idle"), walk = RawAnimation.begin().thenLoop(pre + "walk"),
                hover = RawAnimation.begin().thenLoop(pre + "hover_idle");
        controllers.add(new AnimationController<>(this, "base", 5, state -> {
            if (state() == DYING || fleeing()) return PlayState.STOP;
            if (isAerialPhase()) return state.setAndContinue(hover);
            return state.setAndContinue(state.isMoving() ? walk : idle);
        }));
        AnimationController<LuciferEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String name : triggeredAnimations()) {
            boolean hold = name.equals("death") || name.equals("rift_flee");
            action.triggerableAnim(name, hold ? RawAnimation.begin().thenPlayAndHold(pre + name) : RawAnimation.begin().thenPlay(pre + name));
        }
        controllers.add(action);
    }

    // --- persistence --------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("AuMichaelFlags", entityData.get(FLAGS));
        tag.putInt("FleeTicks", fleeTicks);
        if (riftAt != null) tag.put("RiftAt", NbtUtils.writeBlockPos(riftAt));
        if (fortress != null) {
            tag.put("Fortress", NbtUtils.writeBlockPos(fortress));
            tag.putInt("FortressQ", fortressQ);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AuMichaelFlags")) entityData.set(FLAGS, tag.getByte("AuMichaelFlags"));
        fleeTicks = tag.contains("FleeTicks") ? tag.getInt("FleeTicks") : -1;
        riftAt = NbtUtils.readBlockPos(tag, "RiftAt").orElse(null);
        fortress = NbtUtils.readBlockPos(tag, "Fortress").orElse(null);
        fortressQ = tag.getInt("FortressQ");
        if (fleeing()) {
            fleeFrom = position();
            setNoGravity(true);
        }
    }
}
