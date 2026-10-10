package org.papiricoh.supernaturalcraft.entity.heaven;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.memory.MemoryStage;

import java.util.Optional;
import java.util.UUID;

/**
 * A figure in a staged memory: a creature, an ally or the hunter themselves, frozen in a pose; never hurt, never pushed. Only
 * the focus figure can be aimed at: touching it gathers the memory.
 * <p>Synced for the renderer: {@link #figure()} (an entity type id, {@code @owner} or {@code @ally:dean|sam|castiel|bobby}),
 * {@link #pose()} (a pose name; unknown ones stand), {@link #scale()}, {@link #isFocus()}, {@link #tint()} (ARGB, the scene's)
 * and {@link #ownerId()} (whose memory it is: whose skin {@code @owner} wears). Server only: the stage it belongs to.
 */
public class MemoryFigureEntity extends Entity {

    private static final EntityDataAccessor<String> FIGURE = SynchedEntityData.defineId(MemoryFigureEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> POSE_NAME = SynchedEntityData.defineId(MemoryFigureEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(MemoryFigureEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> FOCUS = SynchedEntityData.defineId(MemoryFigureEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TINT = SynchedEntityData.defineId(MemoryFigureEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(MemoryFigureEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    /** The centre of the stage it stands on (server: it goes when that stage does). */
    private @Nullable BlockPos stage;

    public MemoryFigureEntity(EntityType<? extends MemoryFigureEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /** Sets what it shows; call before adding it to the level. */
    public MemoryFigureEntity setup(String figure, String pose, float scale, boolean focus, int tint, @Nullable UUID owner, BlockPos stage) {
        entityData.set(FIGURE, figure);
        entityData.set(POSE_NAME, pose == null || pose.isEmpty() ? "stand" : pose);
        entityData.set(SCALE, Math.max(0.05f, scale));
        entityData.set(FOCUS, focus);
        entityData.set(TINT, tint);
        entityData.set(OWNER, Optional.ofNullable(owner));
        this.stage = stage.immutable();
        refreshDimensions();
        return this;
    }

    public String figure() {
        return entityData.get(FIGURE);
    }

    /** The pose's name ({@code stand}, {@code kneel}, {@code strike}, {@code fallen}, {@code sit}, {@code offer}...). */
    public String pose() {
        return entityData.get(POSE_NAME);
    }

    public float scale() {
        return entityData.get(SCALE);
    }

    public boolean isFocus() {
        return entityData.get(FOCUS);
    }

    public void setFocus(boolean focus) {
        entityData.set(FOCUS, focus);
    }

    public int tint() {
        return entityData.get(TINT);
    }

    public @Nullable UUID ownerId() {
        return entityData.get(OWNER).orElse(null);
    }

    public @Nullable BlockPos stage() {
        return stage;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FIGURE, "");
        builder.define(POSE_NAME, "stand");
        builder.define(SCALE, 1f);
        builder.define(FOCUS, false);
        builder.define(TINT, 0xFFFFFFFF);
        builder.define(OWNER, Optional.empty());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SCALE.equals(key)) refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(scale());
    }

    @Override
    public void tick() {
        super.tick();
        // A figure whose memory is no longer staged goes (a stage put back while its chunk was unloaded).
        if (level() instanceof ServerLevel server && tickCount % 40 == 0 && !MemoryStage.holds(server, stage, getUUID())) discard();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!isFocus()) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp && hand == InteractionHand.MAIN_HAND) MemoryStage.touch(sp, this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean isPickable() {
        return isFocus();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return true;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(FIGURE, tag.getString("Figure"));
        entityData.set(POSE_NAME, tag.contains("Pose") ? tag.getString("Pose") : "stand");
        entityData.set(SCALE, tag.contains("Scale") ? tag.getFloat("Scale") : 1f);
        entityData.set(FOCUS, tag.getBoolean("Focus"));
        entityData.set(TINT, tag.contains("Tint") ? tag.getInt("Tint") : 0xFFFFFFFF);
        entityData.set(OWNER, tag.hasUUID("MemoryOwner") ? Optional.of(tag.getUUID("MemoryOwner")) : Optional.empty());
        stage = tag.contains("Stage") ? BlockPos.of(tag.getLong("Stage")) : null;
        refreshDimensions();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("Figure", figure());
        tag.putString("Pose", pose());
        tag.putFloat("Scale", scale());
        tag.putBoolean("Focus", isFocus());
        tag.putInt("Tint", tint());
        UUID owner = ownerId();
        if (owner != null) tag.putUUID("MemoryOwner", owner);
        if (stage != null) tag.putLong("Stage", stage.asLong());
    }
}
