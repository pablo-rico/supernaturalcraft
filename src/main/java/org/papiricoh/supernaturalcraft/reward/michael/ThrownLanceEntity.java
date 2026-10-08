package org.papiricoh.supernaturalcraft.reward.michael;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelEntity;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * The Lance of Michael thrown by a hunter: it strikes twice as hard at angels and demons, pins what it strikes for a
 * second and comes back to the hand that threw it, like a trident with loyalty.
 *
 * <p>The {@link #borrowed} one is Michael's own, pulled out of the ground in phase VI: thrown back at him it lands
 * hard ({@link MichaelEntity#struckByOwnLance}) and he reels; either way it flies back to him, never to the hunter.
 */
public class ThrownLanceEntity extends AbstractArrow {

    private static final EntityDataAccessor<Boolean> BORROWED = SynchedEntityData.defineId(ThrownLanceEntity.class, EntityDataSerializers.BOOLEAN);
    public static final float DAMAGE = 12f;
    /** Against angels and demons. */
    public static final float BANE_MULTIPLIER = 2f;
    public static final int PIN_TICKS = 20;
    /** How fast it flies home. */
    private static final double RETURN = 0.15;

    private boolean dealtDamage;
    private int michaelId = -1;

    public ThrownLanceEntity(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
    }

    /** Thrown by {@code thrower}, carrying {@code stack} back with it. */
    public static ThrownLanceEntity thrown(Level level, LivingEntity thrower, ItemStack stack) {
        ThrownLanceEntity lance = new ThrownLanceEntity(AllEntities.THROWN_LANCE.get(), level);
        lance.setOwner(thrower);
        lance.setPos(thrower.getX(), thrower.getEyeY() - 0.1, thrower.getZ());
        lance.setPickupItemStack(stack.copy());
        lance.pickup = thrower instanceof Player p && p.hasInfiniteMaterials() ? Pickup.CREATIVE_ONLY : Pickup.ALLOWED;
        lance.shootFromRotation(thrower, thrower.getXRot(), thrower.getYRot(), 0, 2.6f, 0.5f);
        return lance;
    }

    /** Michael's own lance, hurled back at him by {@code thrower}. */
    public static ThrownLanceEntity borrowed(Level level, LivingEntity thrower, int michaelId) {
        ThrownLanceEntity lance = thrown(level, thrower, ItemStack.EMPTY);
        lance.entityData.set(BORROWED, true);
        lance.michaelId = michaelId;
        lance.pickup = Pickup.DISALLOWED;
        return lance;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BORROWED, false);
    }

    public boolean borrowed() {
        return entityData.get(BORROWED);
    }

    private @Nullable MichaelEntity michael() {
        return michaelId >= 0 && level().getEntity(michaelId) instanceof MichaelEntity m && m.isAlive() ? m : null;
    }

    @Override
    public void tick() {
        if (inGroundTime > 4) dealtDamage = true;
        if (!level().isClientSide && borrowed() && (dealtDamage || tickCount > 100)) {
            // Michael's lance goes home to him, hit or miss.
            MichaelEntity m = michael();
            if (m != null) m.lanceReturned();
            discard();
            return;
        }
        Entity owner = getOwner();
        if (!borrowed() && (dealtDamage || isNoPhysics()) && owner != null) {
            if (!owner.isAlive() || owner instanceof ServerPlayer p && p.isSpectator()) {
                if (!level().isClientSide && pickup == Pickup.ALLOWED) spawnAtLocation(getPickupItem(), 0.1f);
                discard();
            } else {
                setNoPhysics(true);
                Vec3 to = owner.getEyePosition().subtract(position());
                setPosRaw(getX(), getY() + to.y * 0.015 * 3, getZ());
                if (level().isClientSide) yOld = getY();
                setDeltaMovement(getDeltaMovement().scale(0.95).add(to.normalize().scale(RETURN)));
            }
        }
        super.tick();
    }

    @Override
    protected @Nullable EntityHitResult findHitEntity(Vec3 from, Vec3 to) {
        return dealtDamage ? null : super.findHitEntity(from, to);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        Entity e = hit.getEntity();
        dealtDamage = true;
        Entity owner = getOwner();
        if (borrowed() && e instanceof MichaelEntity m && owner instanceof ServerPlayer p) {
            m.struckByOwnLance(p);
        } else {
            float damage = DAMAGE * multiplierAgainst(e);
            if (e.hurt(AllDamageTypes.source(level(), AllDamageTypes.LANCE, this, owner != null ? owner : this), damage)
                    && e instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, PIN_TICKS, 6, false, false, true));
                if (level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.END_ROD, living.getX(), living.getY() + 1, living.getZ(), 16, 0.2, 0.8, 0.2, 0.02);
                }
            }
        }
        setDeltaMovement(getDeltaMovement().multiply(-0.01, -0.1, -0.01));
        playSound(AllSounds.MICHAEL_LANCE_IMPACT.get(), 1.2f, 1.1f);
    }

    /** Twice as hard on angels and demons. */
    public static float multiplierAgainst(Entity e) {
        return e.getType().is(AllTags.Entities.ANGELS) || e.getType().is(AllTags.Entities.DEMONS) ? BANE_MULTIPLIER : 1f;
    }

    @Override
    protected boolean tryPickup(Player player) {
        if (borrowed()) return false;
        return super.tryPickup(player) || isNoPhysics() && ownedBy(player) && player.getInventory().add(getPickupItem());
    }

    @Override
    public void playerTouch(Player player) {
        if (!borrowed() && (ownedBy(player) || getOwner() == null)) super.playerTouch(player);
    }

    @Override
    protected void tickDespawn() {
        if (pickup != Pickup.ALLOWED) super.tickDespawn();
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(AllItems.MICHAEL_LANCE.get());
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return AllSounds.MICHAEL_LANCE_IMPACT.get();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("DealtDamage", dealtDamage);
        tag.putBoolean("Borrowed", borrowed());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        dealtDamage = tag.getBoolean("DealtDamage");
        entityData.set(BORROWED, tag.getBoolean("Borrowed"));
    }
}
