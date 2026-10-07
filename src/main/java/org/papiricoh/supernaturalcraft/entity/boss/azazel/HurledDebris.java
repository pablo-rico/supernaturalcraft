package org.papiricoh.supernaturalcraft.entity.boss.azazel;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/**
 * A block Azazel tore out of the ground with his mind and threw. It is drawn as the block it was and
 * shatters on whatever it meets; it never becomes a block again (the arena puts the hole back).
 */
public class HurledDebris extends Projectile {

    private static final EntityDataAccessor<BlockState> STATE = SynchedEntityData.defineId(HurledDebris.class, EntityDataSerializers.BLOCK_STATE);
    private static final int LIFETIME = 120;
    private float damage = 6f;

    public HurledDebris(EntityType<? extends HurledDebris> type, Level level) {
        super(type, level);
    }

    public static HurledDebris of(Level level, LivingEntity owner, BlockState state, Vec3 at, float damage) {
        HurledDebris d = new HurledDebris(AllEntities.HURLED_DEBRIS.get(), level);
        d.setOwner(owner);
        d.setPos(at.x, at.y, at.z);
        d.entityData.set(STATE, state);
        d.damage = damage;
        return d;
    }

    public BlockState blockState() {
        return entityData.get(STATE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STATE, Blocks.DIRT.defaultBlockState());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > LIFETIME) {
            shatter();
            return;
        }
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !level().isClientSide) {
            onHit(hit);
            return;
        }
        Vec3 v = getDeltaMovement();
        setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        setDeltaMovement(v.scale(0.99).add(0, isNoGravity() ? 0 : -0.03, 0));
        if (level().isClientSide && tickCount % 2 == 0) {
            level().addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, blockState()), getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !(target instanceof AzazelEntity) && !(target instanceof HurledDebris);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity victim = result.getEntity();
        if (getOwner() instanceof LuciferEntity boss) {
            LuciferAttacks.hit(boss, victim, AllDamageTypes.SPELL, damage);
        } else {
            victim.hurt(damageSources().thrown(this, getOwner()), damage);
        }
        if (victim instanceof LivingEntity living) {
            Vec3 push = getDeltaMovement().multiply(1, 0, 1).normalize();
            living.knockback(0.6, -push.x, -push.z);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        shatter();
    }

    private void shatter() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, blockState()), getX(), getY(), getZ(), 30, 0.4, 0.4, 0.4, 0.1);
            server.playSound(null, blockPosition(), blockState().getSoundType().getBreakSound(), SoundSource.HOSTILE, 1.2f, 0.8f);
        }
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Block", NbtUtils.writeBlockState(blockState()));
        tag.putFloat("Damage", damage);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(STATE, NbtUtils.readBlockState(level().holderLookup(Registries.BLOCK), tag.getCompound("Block")));
        damage = tag.getFloat("Damage");
    }
}
