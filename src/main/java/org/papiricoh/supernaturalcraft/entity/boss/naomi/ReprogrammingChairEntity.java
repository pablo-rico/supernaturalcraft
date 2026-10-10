package org.papiricoh.supernaturalcraft.entity.boss.naomi;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.entity.boss.BossStrike;
import org.papiricoh.supernaturalcraft.network.HeavenFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * The reprogramming chair a hunter is strapped into (v0.18): Naomi's Strap In seats them ({@link #strap}); they struggle free by
 * mashing jump ({@code ChairStrugglePayload} → {@link #struggle}, at most {@link ChairRules#MAX_PER_SECOND} presses a second), or a
 * friend cuts the straps with {@link ChairRules#ALLY_HITS} blows on the chair. If the time runs out, the drill comes down
 * ({@link ChairRules#drillDamage}, as Divine Wrath), conditions them and heals her. While strapped they cannot dismount
 * ({@code NaomiEvents}, by the synced STRAPPED flag on both sides). Never hurt otherwise; she places one at each of her room's
 * chairs and they go with her.
 */
public class ReprogrammingChairEntity extends Entity implements GeoEntity {

    /** Someone is strapped in: the renderer closes the {@code straps}; dismounting is refused. */
    private static final EntityDataAccessor<Boolean> STRAPPED = SynchedEntityData.defineId(ReprogrammingChairEntity.class, EntityDataSerializers.BOOLEAN);
    /** The seat's height above the chair's feet (where a sitting hunter's hips go). */
    public static final double SEAT = org.papiricoh.supernaturalcraft.heaven.HeavenAssets.CHAIR_SEAT_HEIGHT;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID master, occupant;
    private int needed, presses, allyHits, struggleSound;
    private long deadline;
    private ChairRules.Budget budget = new ChairRules.Budget(0);
    private int orphanTicks;

    public ReprogrammingChairEntity(EntityType<? extends ReprogrammingChairEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STRAPPED, false);
    }

    /** Whether someone is strapped in (synced). */
    public boolean strapped() {
        return entityData.get(STRAPPED);
    }

    /** The Naomi this chair serves (set when she places it). */
    public void serve(UUID naomi) {
        this.master = naomi;
    }

    public @Nullable NaomiEntity master() {
        if (master == null || !(level() instanceof ServerLevel level)) return null;
        return level.getEntity(master) instanceof NaomiEntity n && n.isAlive() ? n : null;
    }

    /** Who is strapped in now, if anyone. */
    public @Nullable Player occupant() {
        if (!strapped() || occupant == null) return null;
        for (Entity e : getPassengers()) if (e instanceof Player p && p.getUUID().equals(occupant)) return p;
        return null;
    }

    public int needed() {
        return needed;
    }

    public int presses() {
        return presses;
    }

    public int allyHits() {
        return allyHits;
    }

    public long deadline() {
        return deadline;
    }

    // --- strapping in ---------------------------------------------------------------------------------------------------

    /** Seats and straps {@code victim}; the drill comes down after {@link ChairRules#ticks}. @return false if taken */
    public boolean strap(Player victim, int phase) {
        if (!(level() instanceof ServerLevel level) || strapped() || !victim.isAlive()) return false;
        victim.stopRiding();
        Vec3 seat = position().add(0, SEAT, 0);
        if (victim instanceof FakePlayer) victim.moveTo(seat.x, getY(), seat.z, getYRot(), 0);
        else victim.teleportTo(seat.x, getY(), seat.z);
        if (!victim.startRiding(this, true)) return false;
        victim.setYRot(getYRot());
        victim.setYHeadRot(getYRot());
        long now = level.getGameTime();
        occupant = victim.getUUID();
        needed = ChairRules.presses(phase, Allegiances.get(victim).faction());
        presses = 0;
        allyHits = 0;
        deadline = now + ChairRules.ticks(phase);
        budget = new ChairRules.Budget(now);
        entityData.set(STRAPPED, true);
        triggerAnim("action", NaomiAnimations.CHAIR_STRAP);
        level.playSound(null, blockPosition(), AllSounds.heaven("naomi.chair_strap"), SoundSource.HOSTILE, 1.5f, 1.0f);
        send(victim, new HeavenFxPayload(getId(), HeavenFxPayload.QTE_START, needed, 0, position(), ChairRules.ticks(phase), ""));
        return true;
    }

    /** The strapped hunter's report: {@code reported} presses since the last. @return presses counted */
    public int struggle(Player from, int reported) {
        if (!(level() instanceof ServerLevel level) || !strapped() || occupant == null || !occupant.equals(from.getUUID())) return 0;
        int ok = budget.accept(Math.max(0, reported), level.getGameTime());
        if (ok <= 0) return 0;
        presses += ok;
        if (++struggleSound % 3 == 0) {
            level.playSound(null, blockPosition(), AllSounds.heaven("naomi.chair_struggle"), SoundSource.PLAYERS, 0.8f,
                    0.9f + random.nextFloat() * 0.2f);
        }
        send(from, new HeavenFxPayload(getId(), HeavenFxPayload.QTE_PROGRESS, presses, needed, position(), 0, ""));
        if (ChairRules.breaksFree(presses, needed)) free(from, false);
        return ok;
    }

    /** Out of the chair: they broke the straps, or a friend did. */
    public void free(@Nullable Player by, boolean ally) {
        if (!(level() instanceof ServerLevel level) || !strapped()) return;
        Player p = occupant();
        triggerAnim("action", NaomiAnimations.CHAIR_RELEASE);
        level.playSound(null, blockPosition(), AllSounds.heaven("naomi.chair_free"), SoundSource.PLAYERS, 1.5f, 1.0f);
        level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 1, getZ(), 20, 0.4, 0.4, 0.4, 0.2);
        if (p != null) {
            send(p, new HeavenFxPayload(getId(), HeavenFxPayload.QTE_END, 1, ally ? 1 : 0, position(), 0, ""));
            p.displayClientMessage(Component.translatable(ally ? "message.supernaturalcraft.naomi.freed" : "message.supernaturalcraft.naomi.broke_free")
                    .withStyle(ChatFormatting.AQUA), true);
        }
        if (ally && by instanceof ServerPlayer helper && helper != p) {
            helper.displayClientMessage(Component.translatable("message.supernaturalcraft.naomi.cut_straps").withStyle(ChatFormatting.AQUA), true);
        }
        release();
    }

    /** Time's up: the drill comes down, they are conditioned, she heals; then the straps open. */
    public void drill() {
        if (!(level() instanceof ServerLevel level) || !strapped()) return;
        Player p = occupant();
        NaomiEntity naomi = master();
        triggerAnim("action", NaomiAnimations.CHAIR_DRILL_DOWN);
        level.playSound(null, blockPosition(), AllSounds.heaven("naomi.drill"), SoundSource.HOSTILE, 2f, 1.0f);
        if (p != null) {
            Entity by = naomi != null ? naomi : this;
            BossStrike.deal(by, p, AllDamageTypes.DIVINE_WRATH, ChairRules.drillDamage(p.getMaxHealth(), Kin.isDemon(p)));
            p.addEffect(new MobEffectInstance(AllMobEffects.CONDITIONED, ChairRules.FAIL_CONDITIONED, 0), naomi);
            level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getEyeY(), p.getZ(), 30, 0.2, 0.2, 0.2, 0.15);
            send(p, new HeavenFxPayload(getId(), HeavenFxPayload.QTE_END, 0, 0, position(), 0, ""));
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.naomi.drilled").withStyle(ChatFormatting.RED), true);
        }
        if (naomi != null) naomi.healTrue(ChairRules.FAIL_HEAL * naomi.trueMaxHealth());
        release();
    }

    /** Opens the straps and lets whoever sits here out (quietly: no payloads). */
    public void release() {
        Player p = occupant();
        entityData.set(STRAPPED, false);
        occupant = null;
        presses = 0;
        allyHits = 0;
        if (p != null && p.getVehicle() == this) {
            p.stopRiding();
            Vec3 out = position().add(Vec3.directionFromRotation(0, getYRot()).scale(1.2));
            if (p instanceof FakePlayer) p.moveTo(out.x, getY(), out.z);
            else p.teleportTo(out.x, getY(), out.z);
        }
    }

    private void send(Player p, HeavenFxPayload payload) {
        if (p instanceof ServerPlayer sp && !(p instanceof FakePlayer) && sp.connection != null) PacketDistributor.sendToPlayer(sp, payload);
    }

    // --- ticking ----------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        if (!isNoGravity()) {
            setDeltaMovement(getDeltaMovement().add(0, -0.04, 0));
            move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().multiply(0, 0.98, 0));
        }
        if (master != null && master() == null) {
            if (++orphanTicks > 40) {
                release();
                discard();
                return;
            }
        } else {
            orphanTicks = 0;
        }
        if (!strapped()) return;
        if (occupant() == null) {
            // They are gone (fallen, carried off, logged out).
            release();
            return;
        }
        if (level.getGameTime() >= deadline) drill();
    }

    /** Test hook: brings the deadline to now (the drill comes down on the next tick). */
    public void hurryDrill() {
        deadline = level().getGameTime();
    }

    // --- a friend's blows -------------------------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isInvulnerableTo(source)) return false;
        if (!strapped() || !(source.getEntity() instanceof Player ally) || ally.getUUID().equals(occupant)) return false;
        allyHits++;
        markHurt();
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 1, getZ(), 8, 0.3, 0.3, 0.3, 0.1);
            level.playSound(null, blockPosition(), AllSounds.heaven("naomi.chair_struggle"), SoundSource.PLAYERS, 1f, 0.7f);
        }
        if (allyHits >= ChairRules.ALLY_HITS) free(ally, true);
        return true;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        return new Vec3(0, SEAT, 0);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && strapped()) release();
        super.remove(reason);
    }

    // --- GeckoLib ---------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = NaomiAnimations.CHAIR_PREFIX;
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + NaomiAnimations.CHAIR_IDLE);
        controllers.add(new AnimationController<>(this, "base", 5, state -> state.setAndContinue(idle)));
        AnimationController<ReprogrammingChairEntity> action = new AnimationController<>(this, "action", 2, state -> PlayState.STOP);
        for (String clip : NaomiAnimations.CHAIR_TRIGGERED) action.triggerableAnim(clip, RawAnimation.begin().thenPlay(pre + clip));
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // --- persistence ------------------------------------------------------------------------------------------------------

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        master = tag.hasUUID("Master") ? tag.getUUID("Master") : null;
        // A reload lets whoever sat here go: the struggle does not survive it.
        entityData.set(STRAPPED, false);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (master != null) tag.putUUID("Master", master);
    }
}
