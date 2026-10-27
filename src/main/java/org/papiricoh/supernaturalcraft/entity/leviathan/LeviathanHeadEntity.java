package org.papiricoh.supernaturalcraft.entity.leviathan;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.leviathan.BoraxSensitive;
import org.papiricoh.supernaturalcraft.leviathan.LeviathanRules;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * A Leviathan's severed head (v0.25), crawling back to its body on its tongue. Blows only knock it away (it cannot be hurt);
 * borax, fire or the void end it, and so does a hunter bagging it (using it with an empty hand). Kept away from its body
 * ({@link LeviathanRules#headLost}) it is lost; within {@link LeviathanRules#REJOIN_RANGE} it rejoins. Either way the
 * Leviathan's fate is decided on the body ({@link LeviathanEntity#finish}, {@link LeviathanEntity#rejoin}).
 */
public class LeviathanHeadEntity extends PathfinderMob implements GeoEntity, BoraxSensitive {

    private static final String PREFIX = "animation.leviathan_head.";
    /** The face it wears: its body's skin ({@link LeviathanEntity#skin()}), "" for a plain Leviathan. */
    private static final net.minecraft.network.syncher.EntityDataAccessor<String> SKIN =
            net.minecraft.network.syncher.SynchedEntityData.defineId(LeviathanHeadEntity.class, net.minecraft.network.syncher.EntityDataSerializers.STRING);
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID body;
    private int ticksAway;

    public LeviathanHeadEntity(EntityType<? extends LeviathanHeadEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    public void setBody(UUID body) {
        this.body = body;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN, "");
    }

    public String skin() {
        return entityData.get(SKIN);
    }

    public void setSkin(String skin) {
        entityData.set(SKIN, skin);
    }

    private @Nullable LeviathanEntity body() {
        return body != null && level() instanceof ServerLevel level && level.getEntity(body) instanceof LeviathanEntity l && l.isAlive() ? l : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        LeviathanEntity b = body();
        if (b == null || !b.headless()) {
            if (tickCount > 5) discard();
            return;
        }
        double d = distanceToSqr(b);
        if (LeviathanRules.rejoins(d)) {
            b.rejoin();
            discard();
            return;
        }
        ticksAway = d > LeviathanRules.HEAD_LOST_RANGE * LeviathanRules.HEAD_LOST_RANGE ? ticksAway + 1 : 0;
        if (LeviathanRules.headLost(d, ticksAway)) {
            end(b.getLastHurtByMob(), false);
            return;
        }
        if (tickCount % 10 == 0 && onGround()) getNavigation().moveTo(b, 1.0);
        if (tickCount % 30 == 0 && walkAnimation.isMoving()) {
            level.playSound(null, blockPosition(), AllSounds.leviathan("head_crawl"), SoundSource.HOSTILE, 0.7f, 1.0f);
        }
    }

    /** The head is done for, and its Leviathan with it ({@code borax}: borax did it, the clean end a chain respects). */
    private void end(@Nullable Entity killer, boolean borax) {
        LeviathanEntity b = body();
        if (b != null) b.finish(killer, borax);
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        if (source.is(AllDamageTypes.BORAX) || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            end(source.getEntity(), source.is(AllDamageTypes.BORAX));
            return true;
        }
        Entity by = source.getEntity();
        if (by != null) {
            Vec3 away = position().subtract(by.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1.0e-4) setDeltaMovement(away.normalize().scale(0.9).add(0, 0.4, 0));
            hurtMarked = true;
            triggerAnim("action", "snap");
        }
        return false;
    }

    @Override
    public void onBorax(ServerLevel level, @Nullable Entity owner) {
        end(owner, true);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!player.getItemInHand(hand).isEmpty()) return super.mobInteract(player, hand);
        if (!level().isClientSide) {
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.leviathan.bagged").withStyle(ChatFormatting.GRAY), true);
            end(player, false);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (body != null) tag.putUUID("Body", body);
        tag.putInt("TicksAway", ticksAway);
        tag.putString("Skin", skin());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        body = tag.hasUUID("Body") ? tag.getUUID("Body") : null;
        ticksAway = tag.getInt("TicksAway");
        setSkin(tag.getString("Skin"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop(PREFIX + "idle"), crawl = RawAnimation.begin().thenLoop(PREFIX + "crawl");
        controllers.add(new AnimationController<>(this, "base", 3, state -> state.setAndContinue(state.isMoving() ? crawl : idle)));
        controllers.add(new AnimationController<>(this, "action", 1, state -> PlayState.STOP)
                .triggerableAnim("snap", RawAnimation.begin().thenPlay(PREFIX + "snap")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
