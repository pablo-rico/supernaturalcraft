package org.papiricoh.supernaturalcraft.entity.witch;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.bowl.witch.CurseEffect;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.UUID;

/**
 * The Coven Witch (v0.21), the coven's elite. Behind a magic shield that swallows harm until {@link #SHIELD_HP} of it breaks
 * the bubble (or a salt round, or Break Hex, shatters it at once); it rises again after {@link #SHIELD_REGEN} ticks. She calls a
 * familiar when a fight begins, blinks away in smoke more readily, and lays stronger curses — Frailty or Silence — on a player
 * within {@link #CURSE_RANGE}: a violet circle marks the ground under them first, and stepping out of it before the hex lands
 * spares them. She can also {@link #setChanneling channel} (the {@code channel} loop) for whoever leads her coven (P6).
 * Found only at covens (and at the Grand Witch's side).
 */
public class CovenWitchEntity extends WitchEntity {

    private static final EntityDataAccessor<Boolean> SHIELDED = SynchedEntityData.defineId(CovenWitchEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CHANNELING = SynchedEntityData.defineId(CovenWitchEntity.class, EntityDataSerializers.BOOLEAN);

    /** Harm the shield swallows before it breaks. */
    public static final float SHIELD_HP = 14;
    /** Ticks until a broken shield rises again. */
    public static final int SHIELD_REGEN = 400;
    /** The shield's radius, for its WARD_PULSE effects. */
    public static final float SHIELD_RADIUS = 1.1f;
    /** How far her strong curses reach; ticks between them; the warning before one lands; how long they last. */
    public static final double CURSE_RANGE = 16;
    public static final int CURSE_EVERY = 240, CURSE_WINDUP = 12, CURSE_TICKS = 600;
    /** The violet circle's radius: out of it, the curse misses. */
    public static final float CURSE_RADIUS = 1.5f;
    /** Ticks before she calls another familiar. */
    public static final int FAMILIAR_EVERY = 1200;

    private float shieldDamage;
    private int shieldRegen;
    private int curseCooldown = 100, familiarCooldown;
    private int windup;
    private @Nullable UUID windupTarget;
    private Vec3 windupAt = Vec3.ZERO;
    private long lastShieldHitFx;

    public CovenWitchEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 25;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 48.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHIELDED, true);
        builder.define(CHANNELING, false);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                                  @Nullable SpawnGroupData data) {
        setPersistenceRequired();
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    public String rig() {
        return WitchAssets.COVEN_WITCH;
    }

    @Override
    protected String soundPrefix() {
        return "coven";
    }

    @Override
    protected float basePotency() {
        return 1.4f;
    }

    @Override
    protected int boltInterval() {
        return 45;
    }

    @Override
    protected float teleportChance() {
        return 0.5f;
    }

    @Override
    protected String pickSigil(LivingEntity target) {
        int roll = random.nextInt(10);
        return roll < 4 ? "pain" : roll < 7 ? "hex" : "frost";
    }

    @Override
    protected CurseEffect.Curse robbedCurse() {
        return random.nextBoolean() ? CurseEffect.Curse.PAIN : CurseEffect.Curse.MISFORTUNE;
    }

    // --- the shield ------------------------------------------------------------------------------------------------------

    public boolean shielded() {
        return entityData.get(SHIELDED);
    }

    /** Shatters her shield now (salt, Break Hex, the shield's own limit). @return whether there was one */
    public boolean breakShield() {
        if (!shielded()) return false;
        entityData.set(SHIELDED, false);
        shieldDamage = 0;
        shieldRegen = SHIELD_REGEN;
        if (level() instanceof ServerLevel server) {
            MagicFx.ward(server, School.WITCHCRAFT, this, position().add(0, getBbHeight() / 2, 0), SHIELD_RADIUS, MagicFxPayload.WARD_BROKEN, 20);
            server.playSound(null, getX(), getY(), getZ(), AllSounds.coven("witch.shield_break"), SoundSource.HOSTILE, 1f, 1f);
        }
        return true;
    }

    /** Her bubble rises again at once (v0.21 P6: she resumes channelling for the Grand Witch whole, to be broken again). */
    public void restoreShield() {
        if (!shielded()) raiseShield();
        shieldRegen = 0;
    }

    private void raiseShield() {
        entityData.set(SHIELDED, true);
        shieldDamage = 0;
        if (level() instanceof ServerLevel server) {
            MagicFx.ward(server, School.WITCHCRAFT, this, position().add(0, getBbHeight() / 2, 0), SHIELD_RADIUS, MagicFxPayload.WARD_RAISED, 20);
            server.playSound(null, getX(), getY(), getZ(), AllSounds.coven("witch.shield_up"), SoundSource.HOSTILE, 1f, 1f);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || !shielded() || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || isInvulnerableTo(source)
                || source.is(org.papiricoh.supernaturalcraft.registry.AllDamageTypes.COLT)) {
            return super.hurt(source, amount);
        }
        // The bubble swallows the blow; enough of them break it.
        shieldDamage += amount;
        if (source.getEntity() instanceof LivingEntity attacker && !WitchMagic.kin(attacker)) {
            if (getTarget() == null) setTarget(attacker);
            alert(attacker);
        }
        if (shieldDamage >= SHIELD_HP) {
            breakShield();
            if (source.getEntity() != null && random.nextFloat() < 0.5f && !busy()) start(Act.TELEPORT, null, source.getEntity().position());
        } else if (level() instanceof ServerLevel server && server.getGameTime() - lastShieldHitFx > 6) {
            lastShieldHitFx = server.getGameTime();
            MagicFx.ward(server, School.WITCHCRAFT, this, position().add(0, getBbHeight() / 2, 0), SHIELD_RADIUS, MagicFxPayload.WARD_HIT, 10);
        }
        return false;
    }

    // --- channelling (P6) ------------------------------------------------------------------------------------------------

    public boolean channeling() {
        return entityData.get(CHANNELING);
    }

    /** While she channels she stands still, plays her {@code channel} loop and casts nothing. */
    public void setChanneling(boolean on) {
        entityData.set(CHANNELING, on);
        if (on) getNavigation().stop();
    }

    @Override
    public boolean isImmobile() {
        return super.isImmobile() || channeling();
    }

    @Override
    protected RawAnimation baseLoop(boolean moving) {
        return channeling() ? loop("channel") : super.baseLoop(moving);
    }

    // --- acting ----------------------------------------------------------------------------------------------------------

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        if (curseCooldown > 0) curseCooldown--;
        if (familiarCooldown > 0) familiarCooldown--;
        if (!shielded() && --shieldRegen <= 0 && !busy()) start(Act.SHIELD, null, position());
        if (windup > 0 && --windup == 0) {
            LivingEntity t = windupTarget != null && level() instanceof ServerLevel s && s.getEntity(windupTarget) instanceof LivingEntity l ? l : null;
            if (t != null) start(Act.HEX, t, windupAt);
            else setCasting(false);
        }
    }

    @Override
    protected void decide(LivingEntity target, double dist2, boolean seen) {
        if (channeling() || busy() || windup > 0) return;
        // Only a dead or unloaded familiar is replaced: one that has strayed (beyond FAMILIAR_RANGE) is still hers.
        if (boundFamiliar() == null && familiarCooldown <= 0) {
            familiarCooldown = FAMILIAR_EVERY;
            callFamiliar();
        }
        if (target instanceof Player && seen && curseCooldown <= 0 && dist2 < CURSE_RANGE * CURSE_RANGE && level() instanceof ServerLevel server) {
            curseCooldown = CURSE_EVERY + random.nextInt(60);
            windup = CURSE_WINDUP;
            windupTarget = target.getUUID();
            windupAt = target.position();
            TelegraphMarker.circle(server, windupAt, CURSE_RADIUS, TelegraphMarker.VIOLET,
                    CURSE_WINDUP + WitchAssets.HIT_TICKS.get("hex"));
            // v0.21: the cast circle at her hands for the whole windup, and the synced casting flag.
            setCasting(true);
            Vec3 hands = position().add(getLookAngle().multiply(1, 0, 1).normalize().scale(0.4)).add(0, getBbHeight() * 0.6, 0);
            org.papiricoh.supernaturalcraft.magic.fx.MagicFx.cast(server, this, org.papiricoh.supernaturalcraft.magic.school.School.WITCHCRAFT,
                    hands, target.getEyePosition(), 2, false, CURSE_WINDUP + WitchAssets.HIT_TICKS.get("hex"));
            return;
        }
        super.decide(target, dist2, seen);
    }

    @Override
    protected void land(Act act, @Nullable LivingEntity target, Vec3 at) {
        switch (act) {
            case HEX -> {
                if (target == null || !target.isAlive()) return;
                double dx = target.getX() - at.x, dz = target.getZ() - at.z;
                if (dx * dx + dz * dz > CURSE_RADIUS * CURSE_RADIUS) return;
                WitchMagic.curse(this, target, random.nextBoolean() ? CurseEffect.Curse.FRAILTY : CurseEffect.Curse.SILENCE, CURSE_TICKS);
            }
            case SHIELD -> raiseShield();
            default -> super.land(act, target, at);
        }
    }

    /** A cat or a raven answers her call, bound to her. */
    public @Nullable FamiliarEntity callFamiliar() {
        if (!(level() instanceof ServerLevel server)) return null;
        FamiliarEntity f = AllEntities.FAMILIAR.get().create(server);
        if (f == null) return null;
        f.setVariant(random.nextBoolean() ? FamiliarEntity.Variant.CAT : FamiliarEntity.Variant.RAVEN);
        Vec3 at = position().add(random.nextDouble() * 2 - 1, f.variant() == FamiliarEntity.Variant.RAVEN ? 1.5 : 0, random.nextDouble() * 2 - 1);
        f.moveTo(at.x, at.y, at.z, getYRot(), 0);
        f.setPersistenceRequired();
        server.addFreshEntity(f);
        bind(f);
        MagicFx.teleport(server, f, School.WITCHCRAFT, at, at, MagicFxPayload.SMOKE_WITCH, 30);
        return f;
    }

    // --- save ------------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Shielded", shielded());
        tag.putFloat("ShieldDamage", shieldDamage);
        tag.putInt("ShieldRegen", shieldRegen);
        tag.putBoolean("Channeling", channeling());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Shielded")) entityData.set(SHIELDED, tag.getBoolean("Shielded"));
        shieldDamage = tag.getFloat("ShieldDamage");
        shieldRegen = tag.getInt("ShieldRegen");
        entityData.set(CHANNELING, tag.getBoolean("Channeling"));
    }
}
