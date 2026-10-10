package org.papiricoh.supernaturalcraft.entity.boss.zachariah;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.boss.BossStrike;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/**
 * A memo of Zachariah's Paper Storm (v0.18): a sheet of Heaven's stationery flung by his telekinesis. It flies straight with no
 * drop, cuts the first hunter it meets ({@link ZachariahBalance#MEMO_DAMAGE}, his blow) and is gone; a wall or the floor stops it.
 * It wraps round the endless office like everything else thrown there. The client draws it as a paper sprite.
 */
public class MemoProjectile extends Projectile {

    private int life = ZachariahBalance.MEMO_LIFE;

    public MemoProjectile(EntityType<? extends MemoProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    /** One memo from {@code boss} at {@code from}, along {@code dir}. */
    public static MemoProjectile throwFrom(LuciferEntity boss, Vec3 from, Vec3 dir) {
        MemoProjectile m = new MemoProjectile(AllEntities.MEMO_PROJECTILE.get(), boss.level());
        m.setOwner(boss);
        m.setPos(from.x, from.y, from.z);
        m.shoot(dir.x, dir.y, dir.z, ZachariahBalance.MEMO_SPEED, 2.0f);
        boss.level().addFreshEntity(m);
        return m;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && --life <= 0) {
            discard();
            return;
        }
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            onHit(hit);
            if (isRemoved()) return;
        }
        Vec3 v = getDeltaMovement();
        setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        ProjectileUtil.rotateTowardsMovement(this, 0.5f);
        if (level().isClientSide && random.nextFloat() < 0.3f) {
            level().addParticle(ParticleTypes.WHITE_ASH, getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        if (!super.canHitEntity(e) || !(e instanceof LivingEntity living)) return false;
        return !(getOwner() instanceof LuciferEntity boss) || ZachariahAttacks.isFoe(boss, living);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (level().isClientSide) return;
        float mult = getOwner() instanceof LuciferEntity boss ? boss.attackDamageMultiplier() : 1f;
        BossStrike.land(getOwner(), hit.getEntity(), AllDamageTypes.source(level(), AllDamageTypes.SPELL, this, getOwner()),
                ZachariahBalance.MEMO_DAMAGE * mult);
        if (level() instanceof ServerLevel level) level.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 6, 0.1, 0.1, 0.1, 0.2);
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!level().isClientSide) {
            if (level() instanceof ServerLevel level) level.sendParticles(ParticleTypes.WHITE_ASH, getX(), getY(), getZ(), 6, 0.2, 0.2, 0.2, 0.02);
            discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Life", life);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Life")) life = tag.getInt("Life");
    }
}
