package org.papiricoh.supernaturalcraft.entity.legacy;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.legacy.HenryDialogue;
import org.papiricoh.supernaturalcraft.legacy.Legacies;
import org.papiricoh.supernaturalcraft.legacy.LegacyAssets;
import org.papiricoh.supernaturalcraft.legacy.LegacySchedule;
import org.papiricoh.supernaturalcraft.legacy.LegacyServerHandlers;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Henry Winchester, Men of Letters, out of 1958 (v0.17). Two lives:
 *
 * <ul>
 *   <li><b>calling</b> ({@link #visit}): at dawn after a hunter first beats Lucifer he appears a little way off, walks up, tips
 *       his hat and makes his offer ({@link HenryDialogue}). Answered or not, he then walks off and is gone; turned away or left
 *       standing, he calls again some dawns later ({@link LegacySchedule});</li>
 *   <li><b>at home</b> ({@link #home}): in the bunker's war room by the map table, where members talk to him for cases.</li>
 * </ul>
 *
 * He can't be hurt, never despawns and is never pushed about. The talk itself is server-driven ({@link LegacyServerHandlers}).
 */
public class HenryEntity extends PathfinderMob implements GeoEntity {

    private static final EntityDataAccessor<Boolean> TALKING = SynchedEntityData.defineId(HenryEntity.class, EntityDataSerializers.BOOLEAN);
    private static final String PREFIX = "animation.henry_winchester.";
    /** How close a hunter must be to talk with him. */
    public static final double REACH = 8;
    /** He stops this close to the hunter he came for, and gives up on them past {@link #LEASH}. */
    private static final double STOP = 3, LEASH = 48;
    /** Ticks he waits for an answer; ticks he walks off before vanishing; the longest a call can last. */
    private static final int PATIENCE = 1200, EXIT_TICKS = 80, MAX_CALL = 20 * 240;
    /** Who each calling Henry came for (one at a time per hunter). */
    private static final Map<UUID, UUID> VISITING = new ConcurrentHashMap<>();

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID hunter;
    private @Nullable BlockPos home;
    private float homeYaw;
    private int life, quiet, leavingAt = -1;
    private boolean spoke;
    /** Where each hunter is in the talk (not saved: a reload starts it over). */
    private final Map<UUID, HenryDialogue.Stage> stages = new HashMap<>();

    public HenryEntity(EntityType<? extends HenryEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setInvulnerable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TALKING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 10f, 1f));
    }

    // --- his two lives -------------------------------------------------------------------------------------------------

    /** He comes calling on {@code hunter}: some steps away, walking up. Null if he could not. */
    public static @Nullable HenryEntity visit(ServerPlayer hunter) {
        ServerLevel level = hunter.serverLevel();
        HenryEntity h = AllEntities.HENRY.get().create(level);
        if (h == null) return null;
        Vec3 dir = Vec3.directionFromRotation(0, hunter.getYRot() + 30);
        Vec3 at = hunter.position().add(dir.scale(10));
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(at));
        double y = Math.abs(top.getY() - hunter.getY()) <= 4 ? top.getY() : hunter.getY();
        h.moveTo(at.x, y, at.z, hunter.getYRot() + 180, 0);
        h.hunter = hunter.getUUID();
        level.addFreshEntity(h);
        VISITING.put(hunter.getUUID(), h.getUUID());
        level.playSound(null, h.blockPosition(), AllSounds.LEGACY_HENRY_GREET.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
        return h;
    }

    /** Whether a calling Henry is with {@code hunter} now. */
    public static boolean visiting(ServerPlayer hunter) {
        UUID id = VISITING.get(hunter.getUUID());
        if (id == null) return false;
        if (hunter.serverLevel().getEntity(id) instanceof HenryEntity h && h.isAlive()) return true;
        VISITING.remove(hunter.getUUID());
        return false;
    }

    /** He lives here now (the bunker's war room), facing {@code yaw}. */
    public void home(BlockPos spot, float yaw) {
        home = spot.immutable();
        homeYaw = yaw;
        hunter = null;
        moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, yaw, 0);
        setRot(yaw);
    }

    public @Nullable BlockPos homeSpot() {
        return home;
    }

    public @Nullable UUID hunter() {
        return hunter;
    }

    public boolean calling() {
        return home == null;
    }

    public boolean talking() {
        return entityData.get(TALKING);
    }

    public boolean leaving() {
        return leavingAt >= 0;
    }

    // --- the talk --------------------------------------------------------------------------------------------------------

    /** Where {@code player} is in the talk, or null. */
    public @Nullable HenryDialogue.Stage stage(UUID player) {
        return stages.get(player);
    }

    /** {@code player} is now at {@code stage}: he turns to them and talks. */
    public void talkTo(ServerPlayer player, HenryDialogue.Stage stage) {
        stages.put(player.getUUID(), stage);
        entityData.set(TALKING, true);
        quiet = 0;
        getNavigation().stop();
        getLookControl().setLookAt(player, 30, 30);
        switch (stage) {
            case OFFER, OFFER_AGAIN, FAREWELL -> triggerAnim("action", "tip_hat");
            case WAR_ROOM, CASE_GIVEN, CASE_SOLVED -> triggerAnim("action", "point_map");
            default -> {
            }
        }
    }

    /** The talk with {@code player} is over. */
    public void forget(UUID player) {
        stages.remove(player);
        if (stages.isEmpty()) entityData.set(TALKING, false);
    }

    /** He walks off and is gone (only when calling; at home he stays). */
    public void leave() {
        if (home != null || leavingAt >= 0) return;
        leavingAt = life;
        entityData.set(TALKING, false);
        if (hunter != null && level() instanceof ServerLevel level && level.getPlayerByUUID(hunter) instanceof ServerPlayer p) {
            if (stages.containsKey(p.getUUID())) LegacyServerHandlers.close(p, this);
            Vec3 away = position().subtract(p.position()).multiply(1, 0, 1).normalize().scale(12).add(position());
            getNavigation().moveTo(away.x, away.y, away.z, 1.0);
        }
        stages.clear();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        life++;
        if (home != null) {
            tickHome();
            return;
        }
        ServerPlayer p = hunter == null ? null : (ServerPlayer) level().getPlayerByUUID(hunter);
        if (leavingAt >= 0) {
            if (life - leavingAt >= EXIT_TICKS) vanish();
            return;
        }
        if (p == null || !p.isAlive() || p.level() != level() || p.distanceToSqr(this) > LEASH * LEASH || life > MAX_CALL
                || Legacies.member(p) && stages.isEmpty()) {
            if (p != null && !Legacies.member(p)) Legacies.update(p, l -> LegacySchedule.declined(l, p.serverLevel().getDayTime()));
            leave();
            return;
        }
        getLookControl().setLookAt(p, 30, 30);
        if (!spoke) {
            if (distanceToSqr(p) > STOP * STOP) {
                if (life % 10 == 0) getNavigation().moveTo(p, 0.9);
                if (life > 400) {
                    // He couldn't find a way: he walks the rest in a blink, as a gentleman would.
                    Vec3 near = p.position().add(position().subtract(p.position()).normalize().scale(STOP));
                    moveTo(near.x, p.getY(), near.z);
                }
                return;
            }
            spoke = true;
            getNavigation().stop();
            LegacyServerHandlers.open(this, p);
            return;
        }
        if (talking() && ++quiet > PATIENCE) {
            // Silence is an answer too.
            if (!Legacies.member(p)) Legacies.update(p, l -> LegacySchedule.declined(l, p.serverLevel().getDayTime()));
            leave();
        }
    }

    private void tickHome() {
        if (home != null && distanceToSqr(Vec3.atBottomCenterOf(home)) > 4) moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, homeYaw, 0);
        if (stages.isEmpty()) {
            if (life % 40 == 0 && level().getNearestPlayer(this, 6) == null) setRot(homeYaw);
            return;
        }
        if (++quiet > PATIENCE) {
            for (UUID u : Map.copyOf(stages).keySet()) {
                if (level().getPlayerByUUID(u) instanceof ServerPlayer sp) LegacyServerHandlers.close(sp, this);
            }
            stages.clear();
            entityData.set(TALKING, false);
        }
        for (UUID u : Map.copyOf(stages).keySet()) {
            Player who = level().getPlayerByUUID(u);
            if (who == null || who.distanceTo(this) > REACH * 2) forget(u);
            else getLookControl().setLookAt(who, 30, 30);
        }
    }

    private void vanish() {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, getX(), getY() + 1, getZ(), 12, 0.3, 0.6, 0.3, 0.02);
        }
        if (hunter != null) VISITING.remove(hunter, getUUID());
        discard();
    }

    private void setRot(float yaw) {
        setYRot(yaw);
        setYHeadRot(yaw);
        setYBodyRot(yaw);
        yRotO = yaw;
        yBodyRotO = yaw;
        yHeadRotO = yaw;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp && !leaving()) {
            if (calling() && (hunter == null || !hunter.equals(sp.getUUID()))) return InteractionResult.PASS;
            spoke = true;
            LegacyServerHandlers.open(this, sp);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    // --- an untouchable man ----------------------------------------------------------------------------------------------

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isInvulnerableTo(source) && super.hurt(source, amount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    // --- save ------------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (hunter != null) tag.putUUID("Hunter", hunter);
        if (home != null) tag.put("Home", NbtUtils.writeBlockPos(home));
        tag.putFloat("HomeYaw", homeYaw);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        hunter = tag.hasUUID("Hunter") ? tag.getUUID("Hunter") : null;
        home = NbtUtils.readBlockPos(tag, "Home").orElse(null);
        homeYaw = tag.getFloat("HomeYaw");
        // Reloaded mid-call: the talk is lost, so he goes.
        if (home == null) leavingAt = 0;
    }

    // --- animation -------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop(PREFIX + "idle"), walk = RawAnimation.begin().thenLoop(PREFIX + "walk"),
                talk = RawAnimation.begin().thenLoop(PREFIX + "talk");
        controllers.add(new AnimationController<>(this, "base", 5, state -> {
            if (state.isMoving()) return state.setAndContinue(walk);
            return state.setAndContinue(talking() ? talk : idle);
        }));
        AnimationController<HenryEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String clip : LegacyAssets.triggered("henry_winchester")) action.triggerableAnim(clip, RawAnimation.begin().thenPlay(PREFIX + clip));
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
