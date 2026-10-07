package org.papiricoh.supernaturalcraft.entity.boss.horsemen.pestilence;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.UUID;

/**
 * A cloud of Pestilence's flies (particles only, no model): it chases the nearest hunter, blinds and sickens whoever it
 * engulfs, and nothing but fire touches it. A blade with Fire Aspect, a burning arrow, flint and steel, any fire damage,
 * a burning hunter walking into it or the swarm drifting through a burning block: gone in a puff of smoke.
 */
public class FlySwarmEntity extends Monster {

    public static final int LIFETIME = 600;
    private @Nullable UUID owner;
    private int touchCooldown;

    public FlySwarmEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FLYING_SPEED, 0.2)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public @Nullable UUID owner() {
        return owner;
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        if (level().isClientSide) {
            for (int i = 0; i < 3; i++) {
                level().addParticle(AllParticles.FLY.get(), getRandomX(0.9), getY() + random.nextDouble() * getBbHeight(), getRandomZ(0.9),
                        (random.nextDouble() - 0.5) * 0.1, (random.nextDouble() - 0.5) * 0.05, (random.nextDouble() - 0.5) * 0.1);
            }
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (tickCount > LIFETIME || owner != null && !(level.getEntity(owner) instanceof LivingEntity o && o.isAlive())) {
            discard();
            return;
        }
        if (isOnFire() || level.getBlockState(blockPosition()).getBlock() instanceof BaseFireBlock) {
            disperse(level);
            return;
        }
        Player target = level.getNearestPlayer(this, 40);
        if (target != null && (target.isCreative() || target.isSpectator())) target = null;
        if (target != null) {
            Vec3 to = target.getEyePosition().add(0, -0.4, 0).subtract(position());
            double speed = getAttributeValue(Attributes.FLYING_SPEED);
            Vec3 v = getDeltaMovement().scale(0.8).add(to.normalize().scale(speed * 0.35));
            if (v.length() > speed) v = v.normalize().scale(speed);
            setDeltaMovement(v);
            if (touchCooldown > 0) touchCooldown--;
            if (getBoundingBox().inflate(0.3).intersects(target.getBoundingBox())) engulf(level, target);
        } else {
            setDeltaMovement(getDeltaMovement().scale(0.8));
        }
        if (tickCount % 30 == 0) level.playSound(null, blockPosition(), AllSounds.FLY_SWARM_BUZZ.get(), SoundSource.HOSTILE, 0.8f, 0.9f + random.nextFloat() * 0.2f);
    }

    private void engulf(ServerLevel level, Player target) {
        if (target.isOnFire()) {
            disperse(level);
            return;
        }
        if (touchCooldown > 0) return;
        touchCooldown = 20;
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, true, true));
        target.hurt(AllDamageTypes.source(level, AllDamageTypes.PLAGUE, this), 2f);
        Plague.infect(target, 1);
    }

    /** Fire takes it: a puff of smoke and the buzzing stops. */
    public void disperse(ServerLevel level) {
        level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.5, getZ(), 20, 0.4, 0.4, 0.4, 0.03);
        level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.5, getZ(), 8, 0.3, 0.3, 0.3, 0.02);
        level.playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.8f, 1.4f);
        discard();
    }

    /** Whether this blow carries fire: fire damage, a burning arrow, a weapon with Fire Aspect, a burning attacker. */
    public static boolean fiery(DamageSource source) {
        if (source.is(DamageTypeTags.IS_FIRE)) return true;
        Entity direct = source.getDirectEntity();
        if (direct != null && direct.isOnFire()) return true;
        if (source.getEntity() instanceof LivingEntity attacker && direct == attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            var fireAspect = attacker.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .getHolder(Enchantments.FIRE_ASPECT);
            if (fireAspect.isPresent() && EnchantmentHelper.getItemEnchantmentLevel(fireAspect.get(), weapon) > 0) return true;
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        if (fiery(source)) {
            disperse((ServerLevel) level());
            return true;
        }
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.FLINT_AND_STEEL) || held.is(Items.FIRE_CHARGE)) {
            if (level() instanceof ServerLevel level) {
                disperse(level);
                held.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
    }
}
