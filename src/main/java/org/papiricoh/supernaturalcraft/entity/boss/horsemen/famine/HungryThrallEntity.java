package org.papiricoh.supernaturalcraft.entity.boss.horsemen.famine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * One of Famine's thralls: a starving husk that pays the hunters no mind and shuffles to its master to be eaten. If it
 * reaches him he drinks its soul and heals; kill it on the way.
 */
public class HungryThrallEntity extends Husk {

    public static final float HEALTH = 14;
    private @Nullable UUID master;

    public HungryThrallEntity(EntityType<? extends Husk> type, Level level) {
        super(type, level);
        xpReward = 1;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
    }

    public void setMaster(UUID master) {
        this.master = master;
    }

    public @Nullable UUID master() {
        return master;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected void addBehaviourGoals() {
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level)) return;
        if (master == null || !(level.getEntity(master) instanceof FamineEntity famine) || !famine.isAlive()) {
            discard();
            return;
        }
        setTarget(null);
        if (tickCount % 10 == 0) getNavigation().moveTo(famine, 1.0);
        if (distanceToSqr(famine) < 3.2 * 3.2) famine.devour(this);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                                  @Nullable SpawnGroupData data) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data);
        setBaby(false);
        setCanPickUpLoot(false);
        return out;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (master != null) tag.putUUID("Master", master);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Master")) master = tag.getUUID("Master");
    }
}
