package org.papiricoh.supernaturalcraft.author;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.Chapter;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckAnimations;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckLook;
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

/**
 * The Author at home, in his bathrobe: he types at his desk once a hunter has cast "Find the Author"
 * ({@link AuthorWorld#tickNpc} puts him there), cannot be hurt, never wanders and never despawns. Used, he gets up and
 * talks ({@link AuthorServerHandlers}); choosing "I'm ready" begins the fight. Drawn by the same renderer as the boss
 * ({@link ChuckLook}), with the man's clips: {@code sit_type} at the desk, {@code rise}, {@code talk}, {@code idle}.
 */
public class AuthorNpcEntity extends PathfinderMob implements GeoEntity, ChuckLook {

    private static final EntityDataAccessor<Boolean> SEATED = SynchedEntityData.defineId(AuthorNpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> TALKING = SynchedEntityData.defineId(AuthorNpcEntity.class, EntityDataSerializers.BOOLEAN);
    /** He sits back down after this long without a word. */
    private static final int PATIENCE = 400;
    /** How close a hunter must stand to talk with him. */
    public static final double REACH = 8;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable BlockPos chair, stand;
    private float deskYaw;
    private int quiet;
    private @Nullable UUID listener;
    /** Where each hunter is in the conversation (not saved: a reload starts it over). */
    private final Map<UUID, String> nodes = new HashMap<>();
    /** Client: letters typed since the last carriage return. */
    private int typed;

    public AuthorNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setInvulnerable(true);
        setNoAi(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SEATED, true);
        builder.define(TALKING, false);
    }

    /** Where he lives: his chair, the spot beside it where he stands to talk, the way his desk faces. */
    public void home(BlockPos chair, BlockPos stand, float deskYaw) {
        this.chair = chair.immutable();
        this.stand = stand.immutable();
        this.deskYaw = deskYaw;
        sitDown();
    }

    public boolean seated() {
        return entityData.get(SEATED);
    }

    public boolean talking() {
        return entityData.get(TALKING);
    }

    /** Where {@code hunter} is in the conversation, or null if they are not talking to him. */
    public @Nullable String node(UUID hunter) {
        return nodes.get(hunter);
    }

    /** Gets up (if seated) and turns to {@code hunter}, who is now at {@code node}. */
    public void talkTo(ServerPlayer hunter, String node) {
        nodes.put(hunter.getUUID(), node);
        listener = hunter.getUUID();
        quiet = 0;
        if (seated()) {
            entityData.set(SEATED, false);
            if (stand != null) moveTo(Vec3.atBottomCenterOf(stand));
            triggerAnim("action", "rise");
        }
        entityData.set(TALKING, true);
        face(hunter.position());
    }

    /** The conversation with {@code hunter} is over. */
    public void forget(UUID hunter) {
        nodes.remove(hunter);
    }

    private void sitDown() {
        nodes.clear();
        listener = null;
        entityData.set(TALKING, false);
        entityData.set(SEATED, true);
        if (chair != null) {
            Vec3 at = Vec3.atBottomCenterOf(chair);
            moveTo(at.x, at.y, at.z, deskYaw, 0);
        }
        setRot(deskYaw);
    }

    private void face(Vec3 target) {
        float yaw = (float) Math.toDegrees(Math.atan2(-(target.x - getX()), target.z - getZ()));
        setRot(yaw);
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
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientTyping();
            return;
        }
        if (!talking()) {
            if (seated()) setRot(deskYaw);
            return;
        }
        Player who = listener == null ? null : level().getPlayerByUUID(listener);
        if (who != null && who.distanceTo(this) < REACH * 2) face(who.position());
        if (++quiet > PATIENCE || who == null || who.distanceTo(this) > REACH * 3) sitDown();
    }

    /** The typewriter's clatter while he works (client side: no packets for every key). */
    private void clientTyping() {
        if (!seated() || talking() || random.nextInt(3) != 0) return;
        Level level = level();
        double x = getX(), y = getY() + 1, z = getZ();
        if (++typed > 40 + random.nextInt(25)) {
            typed = 0;
            level.playLocalSound(x, y, z, AllSounds.CHUCK_BELL.get(), SoundSource.NEUTRAL, 0.35f, 1.0f, false);
            level.playLocalSound(x, y, z, AllSounds.CHUCK_CARRIAGE.get(), SoundSource.NEUTRAL, 0.4f, 1.0f, false);
            return;
        }
        level.playLocalSound(x, y, z, AllSounds.CHUCK_TYPE.get(), SoundSource.NEUTRAL, 0.25f, 0.85f + random.nextFloat() * 0.3f, false);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) AuthorServerHandlers.open(this, sp);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return talking() ? null : AllSounds.AUTHOR_NPC_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 600;
    }

    // --- an untouchable man ----------------------------------------------------------------------------

    @Override
    public Chapter.Outfit outfit() {
        return Chapter.Outfit.ROBE;
    }

    @Override
    public boolean divine() {
        return false;
    }

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
    protected void doPush(net.minecraft.world.entity.Entity entity) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    // --- save -----------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (chair != null) tag.put("Chair", NbtUtils.writeBlockPos(chair));
        if (stand != null) tag.put("Stand", NbtUtils.writeBlockPos(stand));
        tag.putFloat("DeskYaw", deskYaw);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        chair = NbtUtils.readBlockPos(tag, "Chair").orElse(null);
        stand = NbtUtils.readBlockPos(tag, "Stand").orElse(null);
        deskYaw = tag.getFloat("DeskYaw");
        if (chair != null) sitDown();
    }

    // --- animation --------------------------------------------------------------------------------------

    private static RawAnimation loop(String name) {
        return RawAnimation.begin().thenLoop(ChuckAnimations.HUMAN + name);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation sitType = loop("sit_type"), talk = loop("talk"), idle = loop("idle");
        controllers.add(new AnimationController<>(this, "base", 6, state -> {
            if (talking()) return state.setAndContinue(talk);
            return state.setAndContinue(seated() ? sitType : idle);
        }));
        AnimationController<AuthorNpcEntity> action = new AnimationController<>(this, "action", 2, state -> PlayState.STOP);
        action.triggerableAnim("rise", RawAnimation.begin().thenPlay(ChuckAnimations.HUMAN + "rise"));
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
