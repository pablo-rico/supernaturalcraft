package org.papiricoh.supernaturalcraft.darkness.fog;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.darkness.DarknessRegistry;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraBalance;

/**
 * A villager the black fog took (v0.42, "Out of the Darkness, Into the Fire"): black veins, black eyes, mindless and violent.
 * It keeps the villager it was ({@link #villager}, its whole save) and becomes it again when cured: strong light on it
 * ({@link AmaraBalance#LANCE_PROOF} or more) or holy water ({@link #cure}). It burns in no sun. When the Darkness is at peace every
 * fogged villager is cured where it stands ({@code FogOutbreaks}).
 */
public class FoggedVillagerEntity extends Monster {

    private @Nullable CompoundTag villager;

    public FoggedVillagerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 22.0).add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 28.0);
    }

    /** The fog takes {@code v}: it is replaced by one of these, which keeps it. */
    public static @Nullable FoggedVillagerEntity take(ServerLevel level, Villager v) {
        FoggedVillagerEntity f = DarknessRegistry.FOGGED_VILLAGER.get().create(level);
        if (f == null) return null;
        CompoundTag saved = new CompoundTag();
        if (!v.save(saved)) return null;
        f.villager = saved;
        f.moveTo(v.getX(), v.getY(), v.getZ(), v.getYRot(), v.getXRot());
        if (v.hasCustomName()) f.setCustomName(v.getCustomName());
        f.setPersistenceRequired();
        v.discard();
        level.addFreshEntity(f);
        level.sendParticles(ParticleTypes.SQUID_INK, f.getX(), f.getY() + 1, f.getZ(), 30, 0.3, 0.6, 0.3, 0.05);
        level.playSound(null, f.blockPosition(), DarknessRegistry.DARKNESS_SOUNDS.get("darkness.fog_infect").get(), SoundSource.HOSTILE, 1f, 1f);
        return f;
    }

    /** The fog lets go: the villager it was comes back where it stands. @return whether it was cured */
    public boolean cure() {
        if (!(level() instanceof ServerLevel level) || isRemoved()) return false;
        Villager v = net.minecraft.world.entity.EntityType.VILLAGER.create(level);
        if (v == null) return false;
        if (villager != null) {
            CompoundTag t = villager.copy();
            t.remove("UUID");
            t.remove("Pos");
            v.load(t);
        }
        v.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        v.setHealth(v.getMaxHealth());
        level.addFreshEntity(v);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1, getZ(), 16, 0.4, 0.6, 0.4, 0.05);
        level.playSound(null, blockPosition(), DarknessRegistry.DARKNESS_SOUNDS.get("darkness.cure").get(), SoundSource.NEUTRAL, 1f, 1f);
        discard();
        return true;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (random.nextFloat() < 0.3f) level().addParticle(ParticleTypes.SMOKE, getRandomX(0.4), getY() + random.nextDouble() * getBbHeight(), getRandomZ(0.4), 0, 0.01, 0);
            return;
        }
        if (tickCount % 20 == 0 && level().getBrightness(LightLayer.BLOCK, BlockPos.containing(getEyePosition())) >= AmaraBalance.LANCE_PROOF) cure();
        // She is at peace: the fog lets go of whoever it still holds, wherever they load.
        else if (tickCount % 40 == 0 && level().getServer() != null && org.papiricoh.supernaturalcraft.dyingsun.DyingSunSavedData.get(level().getServer()).state()
                == org.papiricoh.supernaturalcraft.dyingsun.DyingSunRules.RESTORED) cure();
    }

    @Override
    protected boolean isSunBurnTick() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    public @Nullable CompoundTag villager() {
        return villager;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (villager != null) tag.put("Villager", villager);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Villager")) villager = tag.getCompound("Villager");
    }
}
