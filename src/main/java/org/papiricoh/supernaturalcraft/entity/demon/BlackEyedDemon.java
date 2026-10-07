package org.papiricoh.supernaturalcraft.entity.demon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.EnumSet;

/**
 * The common black-eyed demon riding a stolen human. Brawls up close and, every few seconds,
 * flicks its target away with a gesture of telekinesis.
 */
public class BlackEyedDemon extends DemonEntity {

    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(BlackEyedDemon.class, EntityDataSerializers.INT);
    public static final int VARIANTS = 2;

    public BlackEyedDemon(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected String animPrefix() {
        return "black_eyed_demon";
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
    }

    public int getVariant() {
        return entityData.get(VARIANT);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                  MobSpawnType spawnType, @Nullable SpawnGroupData data) {
        entityData.set(VARIANT, random.nextInt(VARIANTS));
        return super.finalizeSpawn(level, difficulty, spawnType, data);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new TelekinesisGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", getVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(VARIANT, Math.floorMod(tag.getInt("Variant"), VARIANTS));
    }

    /** A flick of the wrist at range: knocks the target back and roughs it up. */
    static class TelekinesisGoal extends Goal {
        private final BlackEyedDemon demon;
        private int cooldown = 60;

        TelekinesisGoal(BlackEyedDemon demon) {
            this.demon = demon;
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (--cooldown > 0 || demon.isSmoking() || demon.isTrapped()) return false;
            LivingEntity target = demon.getTarget();
            if (target == null || !target.isAlive()) return false;
            double d = demon.distanceToSqr(target);
            return d > 9 && d < 100 && demon.getSensing().hasLineOfSight(target);
        }

        @Override
        public void start() {
            LivingEntity target = demon.getTarget();
            if (target == null || !(demon.level() instanceof ServerLevel level)) return;
            demon.getLookControl().setLookAt(target);
            demon.triggerAnim("action", "attack");
            Vec3 push = target.position().subtract(demon.position()).normalize();
            target.hurt(AllDamageTypes.source(level, AllDamageTypes.SPELL, demon), 2.0f);
            target.knockback(1.6, -push.x, -push.z);
            target.push(0, 0.35, 0);
            target.hurtMarked = true;
            demon.playSound(AllSounds.LUCIFER_SNAP.get(), 0.8f, 1.6f);
            cooldown = 100 + demon.random.nextInt(80);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }
}
