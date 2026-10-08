package org.papiricoh.supernaturalcraft.entity.boss.michael.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelBalance;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelEntity;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * The Lance of Michael, thrown by him: it pins whoever it strikes ({@link MichaelBalance#LANCE_PIN_TICKS}, a column of
 * light where it lands), stays stuck where it falls until he calls it back ({@link #recall}), and in phase VI a hunter can
 * pull it out ({@link #steal}: a {@code borrowed_lance}) and hurl it back at him.
 */
public class MichaelLanceEntity extends AbstractArrow {

    private static final EntityDataAccessor<Boolean> RECALLED = SynchedEntityData.defineId(MichaelLanceEntity.class, EntityDataSerializers.BOOLEAN);
    public static final float DAMAGE = 16f;
    public static final double SPEED = 1.9;

    private boolean dealtDamage;

    public MichaelLanceEntity(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
        pickup = Pickup.DISALLOWED;
    }

    /** Hurled by {@code michael} from his hand toward {@code at}. */
    public static MichaelLanceEntity hurl(MichaelEntity michael, Vec3 at) {
        MichaelLanceEntity lance = new MichaelLanceEntity(AllEntities.MICHAEL_LANCE.get(), michael.level());
        lance.setOwner(michael);
        Vec3 from = michael.position().add(0, michael.getBbHeight() * 0.8, 0);
        lance.setPos(from.x, from.y, from.z);
        Vec3 dir = at.subtract(from);
        // A slight arc: aim a little above, gravity does the rest.
        double flat = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        lance.shoot(dir.x, dir.y + flat * 0.06, dir.z, (float) SPEED, 0.5f);
        return lance;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RECALLED, false);
    }

    public boolean recalled() {
        return entityData.get(RECALLED);
    }

    public boolean stuck() {
        return inGround && !recalled();
    }

    private @Nullable MichaelEntity michael() {
        return getOwner() instanceof MichaelEntity m && m.isAlive() ? m : null;
    }

    /** He calls it back: it tears out of the ground and flies to his hand. */
    public void recall() {
        if (recalled()) return;
        entityData.set(RECALLED, true);
        setNoPhysics(true);
        inGround = false;
        playSound(AllSounds.MICHAEL_LANCE_RECALL.get(), 2.0f, 1.0f);
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            MichaelEntity m = michael();
            if (m == null) {
                // He is gone: so is his lance.
                discard();
                return;
            }
            if (recalled()) {
                Vec3 hand = m.position().add(0, m.getBbHeight() * 0.7, 0);
                Vec3 to = hand.subtract(position());
                if (to.length() < 1.6) {
                    m.lanceReturned();
                    discard();
                    return;
                }
                setDeltaMovement(to.normalize().scale(Math.min(1.6, 0.4 + to.length() * 0.15)));
            }
            if (stuck() && tickCount % 6 == 0 && level() instanceof ServerLevel level) {
                level.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 0.5, getZ(), 2, 0.1, 0.6, 0.1, 0.01);
            }
        }
        super.tick();
    }

    @Override
    protected void tickDespawn() {
        // It stays where it fell until he calls it back.
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        return super.canHitEntity(e) && !dealtDamage && !recalled() && e instanceof LivingEntity living
                && !(getOwner() instanceof MichaelEntity m && !MichaelAttacks.isFoe(m, living));
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        Entity e = hit.getEntity();
        dealtDamage = true;
        MichaelEntity m = michael();
        float damage = DAMAGE * (m != null ? m.attackDamageMultiplier() : 1f);
        if (e.hurt(AllDamageTypes.source(level(), AllDamageTypes.LANCE, this, getOwner()), damage) && e instanceof LivingEntity living && m != null) {
            m.pin(living, MichaelBalance.LANCE_PIN_TICKS);
        }
        lightColumn();
        setDeltaMovement(getDeltaMovement().multiply(-0.01, -0.1, -0.01));
        playSound(AllSounds.MICHAEL_LANCE_IMPACT.get(), 2.0f, 1.0f);
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        boolean first = !inGround;
        super.onHitBlock(hit);
        if (first && !recalled()) {
            lightColumn();
            dealtDamage = true;
            if (michael() != null && michael().phase() >= MichaelBalance.PHASES && level() instanceof ServerLevel level) {
                MichaelAttacks.say(michael(), level, "message.supernaturalcraft.michael.lance_free");
            }
        }
    }

    private void lightColumn() {
        if (level() instanceof ServerLevel level) {
            for (int i = 0; i < 24; i++) {
                level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + i * 0.5, getZ(), 2, 0.15, 0.1, 0.15, 0.01);
            }
            level.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 0.3, getZ(), 30, 1.2, 0.2, 1.2, 0.05);
            level.playSound(null, blockPosition(), AllSounds.MICHAEL_LANCE_IMPACT.get(), SoundSource.HOSTILE, 2.5f, 0.8f);
        }
    }

    /** In phase VI, a hunter can pull it out of the ground. */
    @Override
    public boolean isPickable() {
        return inGround && !recalled();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) return stuck() ? InteractionResult.SUCCESS : InteractionResult.PASS;
        return player instanceof ServerPlayer p && steal(p) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    /** A hunter pulls it out (phase VI only): it is theirs for a few seconds. */
    public boolean steal(ServerPlayer hunter) {
        MichaelEntity m = michael();
        if (m == null || !stuck() || m.phase() < MichaelBalance.PHASES) return false;
        ItemStack borrowed = org.papiricoh.supernaturalcraft.reward.michael.BorrowedLanceItem.borrowed(m, hunter.serverLevel().getGameTime());
        if (!hunter.getInventory().add(borrowed)) return false;
        m.lanceStolen(hunter);
        playSound(AllSounds.MICHAEL_LANCE_RECALL.get(), 2.0f, 1.4f);
        discard();
        return true;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return AllSounds.MICHAEL_LANCE_IMPACT.get();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("DealtDamage", dealtDamage);
        tag.putBoolean("Recalled", recalled());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        dealtDamage = tag.getBoolean("DealtDamage");
        entityData.set(RECALLED, tag.getBoolean("Recalled"));
    }
}
