package org.papiricoh.supernaturalcraft.hell.cage;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferLook;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Lucifer as he waits in the closed Cage: in chains, head bowed, looking up now and then at whoever
 * has come so far. Nothing can touch him; he is scenery, swapped for the real fight when the Cage opens.
 */
public class CagedLuciferEntity extends Mob implements GeoEntity, LuciferLook {

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public CagedLuciferEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        setNoAi(true);
        setNoGravity(true);
        setInvulnerable(true);
        setPersistenceRequired();
        setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 100).add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    /** Puts him on his spot over the iris, facing the throne's way out. */
    public static void place(ServerLevelAccessor level, EntityType<CagedLuciferEntity> type) {
        CagedLuciferEntity e = type.create(level.getLevel());
        if (e == null) return;
        BlockPos at = CageLayout.THRONE;
        e.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        e.setYHeadRot(0);
        e.setYBodyRot(0);
        e.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        level.addFreshEntityWithPassengers(e);
    }

    @Override
    public int lookPhase() {
        return 1;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
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
    public boolean isPickable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0, 0, 0);
        if (!level().isClientSide && tickCount % 40 == 0) {
            // Only one may stand in the Cage, and none while it is open.
            ServerLevel server = (ServerLevel) level();
            if (!CageController.get(server).isClosed()) discard();
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation chained = RawAnimation.begin().thenLoop("animation.lucifer_uncaged.chained");
        controllers.add(new AnimationController<>(this, "base", 10, state -> state.setAndContinue(chained)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
