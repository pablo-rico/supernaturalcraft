package org.papiricoh.supernaturalcraft.entity.boss.crowley;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.hellhound.BoundHellhoundEntity;
import org.papiricoh.supernaturalcraft.heaven.HeavenDimension;
import org.papiricoh.supernaturalcraft.hell.HellDimension;
import org.papiricoh.supernaturalcraft.network.CrowleyFxPayload;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.util.Players;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * Crowley recruited (v0.35): one per world ({@link CrowleySavedData}), the boss's rig. He follows his hunter (blinking to them
 * in red smoke when they are far or change worlds; never into Heaven), fights at their side (a telekinetic snap, bound hounds),
 * talks (his remarks: {@link CrowleyVoice}) and answers a right-click with his menu ({@link CrowleyCompanions}). Told to stay,
 * he sits (stairs, a slab) near the spot, anywhere — a hunter's house — until told to follow. At 0 health he does not die: he
 * retreats for {@link CrowleyBalance#RETREAT_TICKS} and comes back. Only the copy the world names
 * ({@link CrowleySavedData#companion}) lives; any other discards itself.
 *
 * <p>Synced state the renderer reads: {@link #eyesLit()} and {@link #seated()} (loop {@code throne_idle}).
 */
public class CrowleyCompanionEntity extends PathfinderMob implements GeoEntity {

    private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(CrowleyCompanionEntity.class, EntityDataSerializers.BYTE);
    public static final int EYES = 1, SEATED = 2;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int snapCooldown, houndCooldown, remarkCooldown = CrowleyBalance.REMARK_COOLDOWN / 3, eyes;
    private @Nullable LivingEntity foe;

    public CrowleyCompanionEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, CrowleyBalance.COMPANION_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, CrowleyBalance.COMPANION_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLAGS, (byte) 0);
    }

    protected boolean flag(int bit) {
        return (entityData.get(FLAGS) & bit) != 0;
    }

    protected void setFlag(int bit, boolean on) {
        byte b = entityData.get(FLAGS);
        entityData.set(FLAGS, (byte) (on ? b | bit : b & ~bit));
    }

    public boolean eyesLit() {
        return flag(EYES);
    }

    public boolean seated() {
        return flag(SEATED);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        UUID owner = owner();
        return super.isAlliedTo(other) || other.getUUID().equals(owner)
                || other instanceof BoundHellhoundEntity h && owner != null && owner.equals(h.ownerId());
    }

    public @Nullable UUID owner() {
        return level() instanceof ServerLevel s ? CrowleySavedData.get(s.getServer()).owner() : null;
    }

    public @Nullable ServerPlayer ownerPlayer() {
        UUID id = owner();
        if (id == null || !(level() instanceof ServerLevel s)) return null;
        return s.getServer().getPlayerList().getPlayer(id);
    }

    // --- per tick -------------------------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) return;
        CrowleySavedData data = CrowleySavedData.get(level.getServer());
        if (data.state() != CrowleySavedData.State.RECRUITED || !getUUID().equals(data.companion())) {
            discard();
            return;
        }
        if (snapCooldown > 0) snapCooldown--;
        if (houndCooldown > 0) houndCooldown--;
        if (remarkCooldown > 0) remarkCooldown--;
        if (eyes > 0 && --eyes == 0) setFlag(EYES, false);
        ServerPlayer owner = ownerPlayer();
        if (data.seat() != null) {
            tickSeated(level, data);
        } else {
            setFlag(SEATED, false);
            if (owner == null || owner.level() != level || !CrowleyCompanions.welcome(owner)) {
                // Off with his hunter (logged out, or through a portal): he comes back to them by the manager.
                poof(level, false);
                discard();
                return;
            }
            tickFollow(level, owner);
        }
        tickFight(level, owner);
        if (owner != null && tickCount % 20 == 0 && remarkCooldown <= 0 && owner.distanceToSqr(this) < 16 * 16) {
            String s = CrowleyCompanions.situation(level, this, owner, foe);
            if (s != null) say(owner, s);
        }
    }

    private void tickFollow(ServerLevel level, ServerPlayer owner) {
        double d2 = distanceToSqr(owner);
        if (d2 > CrowleyBalance.FOLLOW_BLINK * CrowleyBalance.FOLLOW_BLINK) {
            Vec3 to = CrowleyCompanions.besideOf(owner);
            poof(level, false);
            teleportTo(to.x, to.y, to.z);
            poof(level, true);
        } else if (foe == null && d2 > 4 * 4) {
            getNavigation().moveTo(owner, 1.1);
        } else if (foe == null) {
            getNavigation().stop();
        }
    }

    private void tickSeated(ServerLevel level, CrowleySavedData data) {
        BlockPos seat = data.seat();
        if (seat == null) return;
        if (data.seatLevel() != null && !level.dimension().equals(data.seatLevel())) {
            discard();
            return;
        }
        Vec3 at = sittingPoint(level, seat);
        if (foe == null && position().distanceToSqr(at) > CrowleyBalance.SEAT_RADIUS * CrowleyBalance.SEAT_RADIUS) teleportTo(at.x, at.y, at.z);
        if (foe == null && position().distanceToSqr(at) > 0.04) getNavigation().moveTo(at.x, at.y, at.z, 1.0);
        boolean sitting = foe == null && position().distanceToSqr(at) < 0.5;
        if (sitting) {
            getNavigation().stop();
            setDeltaMovement(Vec3.ZERO);
        }
        setFlag(SEATED, sitting && isSeat(level.getBlockState(BlockPos.containing(at))));
    }

    /** Where he sits near {@code seat}: on stairs or a bottom slab within 3 blocks with room above, else the spot itself. */
    public static Vec3 sittingPoint(ServerLevel level, BlockPos seat) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(seat.offset(-3, -1, -3), seat.offset(3, 1, 3))) {
            if (!isSeat(level.getBlockState(p)) || !level.getBlockState(p.above()).isAir() || !level.getBlockState(p.above(2)).isAir()) continue;
            double d = p.distSqr(seat);
            if (d < bestD) {
                bestD = d;
                best = p.immutable();
            }
        }
        return best != null ? new Vec3(best.getX() + 0.5, best.getY() + 0.5, best.getZ() + 0.5) : Vec3.atBottomCenterOf(seat);
    }

    static boolean isSeat(BlockState s) {
        return s.getBlock() instanceof StairBlock && s.getValue(StairBlock.HALF) == Half.BOTTOM
                || s.getBlock() instanceof SlabBlock && s.getValue(SlabBlock.TYPE) == SlabType.BOTTOM;
    }

    private void tickFight(ServerLevel level, @Nullable ServerPlayer owner) {
        if (foe != null && (!foe.isAlive() || foe.distanceToSqr(this) > 24 * 24)) foe = null;
        if (foe == null && owner != null && owner.distanceToSqr(this) < 20 * 20) {
            LivingEntity a = owner.getLastHurtByMob(), b = owner.getLastHurtMob();
            if (a != null && a.isAlive() && a != this && !isAlliedTo(a) && owner.tickCount - owner.getLastHurtByMobTimestamp() < 100) foe = a;
            else if (b != null && b.isAlive() && b != this && !isAlliedTo(b) && owner.tickCount - owner.getLastHurtMobTimestamp() < 100) foe = b;
        }
        if (foe == null) return;
        getLookControl().setLookAt(foe, 30, 30);
        double d2 = distanceToSqr(foe);
        if (d2 > 5 * 5) getNavigation().moveTo(foe, 1.15);
        else getNavigation().stop();
        if (snapCooldown <= 0 && d2 < 6 * 6 && hasLineOfSight(foe)) snap(level, foe);
        if (houndCooldown <= 0 && owner != null) {
            houndCooldown = CrowleyBalance.COMPANION_HOUNDS_COOLDOWN;
            for (int i = 0; i < CrowleyBalance.COMPANION_HOUNDS; i++) {
                Vec3 s = foe.position().add(i == 0 ? 2.5 : -2.5, 0, 1);
                BoundHellhoundEntity h = BoundHellhoundEntity.call(level, owner, s.x, s.y, s.z);
                if (h != null) h.setTarget(foe);
            }
            fx(level, new CrowleyFxPayload(getId(), CrowleyFxPayload.HOUNDS, CrowleyBalance.COMPANION_HOUNDS, 0, foe.position(), 30));
            playSound(AllSounds.crowley("hounds"), 1.4f, 1.1f);
        }
    }

    private void snap(ServerLevel level, LivingEntity target) {
        snapCooldown = CrowleyBalance.COMPANION_SNAP_COOLDOWN;
        triggerAnim("action", "snap");
        setFlag(EYES, true);
        eyes = 30;
        Vec3 away = target.position().subtract(position()).multiply(1, 0, 1);
        away = away.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : away.normalize();
        target.hurt(AllDamageTypes.source(level, AllDamageTypes.SPELL, this), CrowleyBalance.COMPANION_DAMAGE);
        target.setDeltaMovement(away.x * CrowleyBalance.SNAP_PUSH, 0.4, away.z * CrowleyBalance.SNAP_PUSH);
        target.hurtMarked = true;
        fx(level, new CrowleyFxPayload(getId(), CrowleyFxPayload.SNAP, target.getId(), 0, getEyePosition(), 12));
        playSound(AllSounds.crowley("snap"), 1.5f, 1f);
    }

    // --- harm ---------------------------------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        Entity by = source.getEntity();
        if (by != null && (by.getUUID().equals(owner()) || isAlliedTo(by))) return false;
        if (by instanceof LivingEntity l && l != this) foe = l;
        if (getHealth() - amount <= 1f) {
            retreat();
            return false;
        }
        return super.hurt(source, amount);
    }

    /** Beaten down: "I'll be back, darling." Gone for a while; then he comes back (to his hunter, or to his seat). */
    public void retreat() {
        if (!(level() instanceof ServerLevel level)) return;
        CrowleySavedData data = CrowleySavedData.get(level.getServer());
        data.setAbsentUntil(level.getServer().overworld().getGameTime() + CrowleyBalance.RETREAT_TICKS);
        ServerPlayer owner = ownerPlayer();
        if (owner != null) say(owner, CrowleyVoice.RETREAT);
        poof(level, false);
        discard();
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    // --- talk ---------------------------------------------------------------------------------------------------------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) {
            if (sp.getUUID().equals(owner())) {
                triggerAnim("action", "talk");
                CrowleyCompanions.openMenu(sp, this);
            } else {
                say(sp, CrowleyVoice.STRANGER);
            }
        }
        return InteractionResult.CONSUME;
    }

    /** A line of {@code situation} to {@code to} (in the chat, his name first), and the cooldown. */
    public void say(ServerPlayer to, String situation) {
        remarkCooldown = CrowleyBalance.REMARK_COOLDOWN;
        Component line = Component.translatable("entity.supernaturalcraft.crowley").withStyle(ChatFormatting.DARK_RED)
                .append(Component.literal(": ").withStyle(ChatFormatting.DARK_RED))
                .append(Component.translatable(CrowleyVoice.pick(situation, random.nextInt(1000))).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        to.sendSystemMessage(line);
    }

    public void poof(ServerLevel level, boolean arrives) {
        fx(level, new CrowleyFxPayload(getId(), CrowleyFxPayload.COMPANION_POOF, arrives ? 1 : 0, 0, position(), 24));
        level.playSound(null, getX(), getY(), getZ(), AllSounds.crowley(arrives ? "smoke_in" : "smoke_out"), getSoundSource(), 1f, 1.1f);
    }

    private void fx(ServerLevel level, CrowleyFxPayload payload) {
        for (ServerPlayer p : level.players()) if (p.distanceToSqr(this) < 64 * 64) Send.toPlayer(p, payload);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.crowley("hurt");
    }

    // --- animation ----------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = "animation.crowley.";
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + "idle"), walk = RawAnimation.begin().thenLoop(pre + "walk"),
                sit = RawAnimation.begin().thenLoop(pre + "throne_idle");
        controllers.add(new AnimationController<>(this, "base", 5,
                s -> s.setAndContinue(seated() ? sit : s.isMoving() ? walk : idle)));
        AnimationController<CrowleyCompanionEntity> action = new AnimationController<>(this, "action", 2, s -> PlayState.STOP);
        for (String clip : CrowleyAssets.TRIGGERED) action.triggerableAnim(clip, RawAnimation.begin().thenPlay(pre + clip));
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Hounds", houndCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        houndCooldown = tag.getInt("Hounds");
    }

    /** Whether {@code level} is one he follows his hunter into. */
    public static boolean follows(Level level) {
        if (HeavenDimension.isHeaven(level)) return false;
        return !HellDimension.isHell(level) || org.papiricoh.supernaturalcraft.SNConfig.CROWLEY_COMPANION_FOLLOWS_TO_HELL.get();
    }

    /** Hunters about him (for {@link AABB} checks in tests). */
    AABB around(double r) {
        return getBoundingBox().inflate(r);
    }
}
