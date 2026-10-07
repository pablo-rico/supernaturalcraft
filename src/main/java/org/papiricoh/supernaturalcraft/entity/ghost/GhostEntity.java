package org.papiricoh.supernaturalcraft.entity.ghost;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.RedstoneWallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.projectile.HolyWaterProjectile;
import org.papiricoh.supernaturalcraft.grave.GraveBonesBlock;
import org.papiricoh.supernaturalcraft.grave.GraveBonesBlockEntity;
import org.papiricoh.supernaturalcraft.grave.GraveLayout;
import org.papiricoh.supernaturalcraft.magic.spell.SpellHooks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import org.papiricoh.supernaturalcraft.weapon.Holy;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * A vengeful spirit bound to the bones in its grave. It drifts through walls (never across a line
 * of salt), stays within {@link GhostBalance#LEASH} of its bones, and haunts whoever comes near at
 * night: a cold that slows, lights snuffed out, and a telekinetic shove. It is unseen but for
 * flickers and the moments it lashes out (Second Sight and the Reveal sigil show it).
 *
 * <p>Ordinary harm passes through it. Cold iron, holy water and anything holy <b>disperse</b> it
 * for a while ({@link #disperse()}); only salting and burning its bones, or a banishing, lay it to
 * rest for good ({@link #layToRest()}). At dawn it sinks back into its grave.
 */
public class GhostEntity extends Monster implements GeoEntity, SpellHooks.Revealable {

    private static final EntityDataAccessor<Long> MANIFEST_UNTIL = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> REVEALED_UNTIL = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DISPERSED_AT = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> FADE_AT = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.LONG);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(GhostAnimations.clip(GhostAnimations.IDLE));
    private static final RawAnimation FLOAT = RawAnimation.begin().thenLoop(GhostAnimations.clip(GhostAnimations.FLOAT));
    private static final RawAnimation FADE = RawAnimation.begin().thenPlayAndHold(GhostAnimations.clip(GhostAnimations.FADE));

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private BlockPos bones;
    /** Whether the fade under way is the last one (laid to rest) rather than going home at dawn. */
    private boolean fadeIsRest;
    private int dayTicks;
    private int hauntCooldown = GhostBalance.HAUNT_INTERVAL / 2;

    /** Client only: how much of it shows (0–1), smoothed by {@code GhostClientHooks}. */
    public float clientAlpha, clientAlphaO;

    public GhostEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        moveControl = new GhostGoals.GhostMoveControl(this);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FLYING_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 3).add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MANIFEST_UNTIL, -1L);
        builder.define(REVEALED_UNTIL, -1L);
        builder.define(DISPERSED_AT, -1L);
        builder.define(FADE_AT, -1L);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new GhostGoals.Telekinesis(this));
        goalSelector.addGoal(3, new GhostGoals.Haunt(this));
        goalSelector.addGoal(6, new GhostGoals.Drift(this));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false, p -> canHaunt()));
    }

    // --- state --------------------------------------------------------------------------------------

    /** The bones it is bound to; null for a ghost with no grave (a spawn egg). */
    @Nullable
    public BlockPos bones() {
        return bones;
    }

    public void setBones(@Nullable BlockPos bones) {
        this.bones = bones == null ? null : bones.immutable();
    }

    public boolean isDispersed() {
        return GhostBalance.stillDispersed(entityData.get(DISPERSED_AT), level().getGameTime());
    }

    /** Game time its last dispersal began (-1 if never). */
    public long dispersedAt() {
        return entityData.get(DISPERSED_AT);
    }

    /** Fading away for good or back into its grave. */
    public boolean isFading() {
        return entityData.get(FADE_AT) >= 0;
    }

    public long fadeStart() {
        return entityData.get(FADE_AT);
    }

    /** Dispersed or fading: it does nothing and nothing touches it. */
    public boolean isInert() {
        return isFading() || isDispersed();
    }

    public boolean isManifest() {
        return entityData.get(MANIFEST_UNTIL) > level().getGameTime();
    }

    public boolean isRevealed() {
        return entityData.get(REVEALED_UNTIL) > level().getGameTime();
    }

    /** Shows itself to everyone for {@code ticks}. */
    public void manifest(int ticks) {
        long until = level().getGameTime() + ticks;
        if (until > entityData.get(MANIFEST_UNTIL)) entityData.set(MANIFEST_UNTIL, until);
    }

    /** Night here (never in a dimension with fixed time). */
    public boolean nightHere() {
        return !level().dimensionType().hasFixedTime() && GhostBalance.isNight(level().getDayTime());
    }

    /** Whether it may haunt and hunt right now. */
    public boolean canHaunt() {
        return !isInert() && nightHere();
    }

    // --- the public API: scattered, or set free ---------------------------------------------------

    /** Scatters it for {@link GhostBalance#DISPERSE_TICKS}; it gathers again at its bones. Leaves ectoplasm now and then. */
    public void disperse() {
        if (!(level() instanceof ServerLevel server) || isInert()) return;
        entityData.set(DISPERSED_AT, server.getGameTime());
        setTarget(null);
        setDeltaMovement(Vec3.ZERO);
        playSound(AllSounds.GHOST_DISPERSE.get(), 1.2f, 0.9f + random.nextFloat() * 0.2f);
        server.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.0, getZ(), 18, 0.3, 0.6, 0.3, 0.03);
        server.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0, getZ(), 10, 0.3, 0.5, 0.3, 0.02);
        server.sendParticles(AllParticles.FROST.get(), getX(), getY() + 1.0, getZ(), 12, 0.4, 0.6, 0.4, 0.02);
        if (random.nextFloat() < GhostBalance.ECTOPLASM_CHANCE) spawnAtLocation(new ItemStack(AllItems.ECTOPLASM.get()));
    }

    /**
     * Lays it to rest for good: its bones (if loaded) are marked rested and never raise it again;
     * it wails, fades, and is gone.
     */
    public void layToRest() {
        if (!(level() instanceof ServerLevel server)) return;
        if (bones != null && server.isLoaded(bones) && server.getBlockEntity(bones) instanceof GraveBonesBlockEntity be) be.markRested();
        if (isFading() && fadeIsRest) return;
        fadeIsRest = true;
        entityData.set(FADE_AT, server.getGameTime());
        setTarget(null);
        playSound(AllSounds.GHOST_WAIL.get(), 1.6f, 1.0f);
        server.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.0, getZ(), 30, 0.35, 0.8, 0.35, 0.05);
        server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.0, getZ(), 12, 0.3, 0.8, 0.3, 0.04);
    }

    /** Dawn: it sinks back into its grave (the bones will raise it again tomorrow night). */
    private void goHome(ServerLevel server) {
        if (isFading()) return;
        fadeIsRest = false;
        entityData.set(FADE_AT, server.getGameTime());
        setTarget(null);
        playSound(AllSounds.GHOST_WHISPER.get(), 0.8f, 0.7f);
    }

    /** Gathers again over its bones after a dispersal. */
    private void reform(ServerLevel server) {
        entityData.set(DISPERSED_AT, -1L);
        if (bones != null) {
            BlockPos at = bones.above(GraveLayout.DEPTH + 1);
            moveTo(at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5, getYRot(), 0);
        }
        playSound(AllSounds.GHOST_WHISPER.get(), 1.0f, 0.8f);
        server.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.0, getZ(), 10, 0.3, 0.6, 0.3, 0.02);
    }

    @Override
    public void onRevealed() {
        if (!(level() instanceof ServerLevel server)) return;
        entityData.set(REVEALED_UNTIL, server.getGameTime() + GhostBalance.REVEAL_TICKS);
        server.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 1.0, getZ(), 25, 0.4, 0.6, 0.4, 0.05);
    }

    // --- damage -------------------------------------------------------------------------------------

    /** What scatters a ghost: cold iron in hand, holy water, any holy harm. */
    public static boolean isBane(DamageSource source) {
        if (Holy.isHoly(source)) return true;
        if (source.getDirectEntity() instanceof HolyWaterProjectile) return true;
        return source.getDirectEntity() instanceof LivingEntity attacker && source.getDirectEntity() == source.getEntity()
                && isColdIron(attacker.getMainHandItem());
    }

    /** {@code #supernaturalcraft:ghost_bane}, or any iron-tier tool or weapon (other mods' iron counts too). */
    public static boolean isColdIron(ItemStack stack) {
        if (stack.is(AllTags.Items.GHOST_BANE)) return true;
        return stack.getItem() instanceof net.minecraft.world.item.TieredItem tiered && tiered.getTier() == net.minecraft.world.item.Tiers.IRON;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        // /kill and the void still work.
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        if (isInert()) return false;
        if (isBane(source)) {
            disperse();
            return true;
        }
        // Ordinary harm passes through: it only flickers.
        if (source.getEntity() instanceof Player) {
            manifest(10);
            triggerAnim("action", GhostAnimations.FLICKER);
            playSound(AllSounds.GHOST_HURT.get(), 0.8f, 1.0f);
        }
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isInert() && super.isPickable();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) {
        return false;
    }

    @Override
    public boolean isAffectedByFluids() {
        return false;
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return bones == null && super.removeWhenFarAway(distance);
    }

    // --- movement: through walls, never over salt ---------------------------------------------------

    @Override
    public void move(MoverType type, Vec3 delta) {
        super.move(type, GhostWards.clip(this, delta));
    }

    // --- ticking ------------------------------------------------------------------------------------

    @Override
    public void tick() {
        noPhysics = true;
        setNoGravity(true);
        super.tick();
        noPhysics = true;
        if (level().isClientSide) {
            if (FMLEnvironment.dist == Dist.CLIENT) org.papiricoh.supernaturalcraft.client.ghost.GhostClientHooks.tick(this);
            return;
        }
        upkeep((ServerLevel) level());
    }

    private void upkeep(ServerLevel server) {
        long now = server.getGameTime();
        long fadeAt = entityData.get(FADE_AT);
        if (fadeAt >= 0) {
            setDeltaMovement(getDeltaMovement().scale(0.5).add(0, 0.01, 0));
            if (now - fadeAt >= GhostBalance.FADE_TICKS) discard();
            return;
        }
        if (bones != null) {
            // Dawn sends it home; boneless ghosts only go quiet by day.
            if (nightHere()) dayTicks = 0;
            else if (++dayTicks > GhostBalance.DAWN_GRACE) {
                goHome(server);
                return;
            }
            if (tickCount % 40 == 0 && server.isLoaded(bones) && !stillBound(server)) return;
        }
        long dispersedAt = entityData.get(DISPERSED_AT);
        if (dispersedAt >= 0) {
            setDeltaMovement(Vec3.ZERO);
            if (!GhostBalance.stillDispersed(dispersedAt, now)) reform(server);
            return;
        }
        if (!canHaunt()) {
            if (getTarget() != null) setTarget(null);
            return;
        }
        if (bones != null && distanceToSqr(Vec3.atCenterOf(bones)) > GhostBalance.LEASH * GhostBalance.LEASH) {
            setTarget(null);
            BlockPos home = bones.above(GraveLayout.DEPTH + 1);
            getMoveControl().setWantedPosition(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, GhostBalance.CHASE_SPEED);
        }
        if (--hauntCooldown <= 0) {
            hauntCooldown = GhostBalance.HAUNT_INTERVAL + random.nextInt(30);
            haunt(nearbyPlayers());
        }
    }

    /** Checks its bones still hold it; if not, it fades (bones gone or at rest) or vanishes (a stale copy). */
    private boolean stillBound(ServerLevel server) {
        BlockState s = server.getBlockState(bones);
        if (!(s.getBlock() instanceof GraveBonesBlock) || s.getValue(GraveBonesBlock.RESTED)) {
            layToRest();
            return false;
        }
        if (server.getBlockEntity(bones) instanceof GraveBonesBlockEntity be && be.ghostId() != null && !be.owns(this)) {
            discard();
            return false;
        }
        return true;
    }

    // --- hauntings ----------------------------------------------------------------------------------

    /** Living, non-creative players within {@link GhostBalance#HAUNT_RANGE}. */
    public List<Player> nearbyPlayers() {
        double r = GhostBalance.HAUNT_RANGE;
        return level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(r),
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative() && p.distanceToSqr(this) <= r * r
                        && !org.papiricoh.supernaturalcraft.bowl.spell.Concealment.hides(this, p));
    }

    /** One haunting of these players: the cold, or the lights going out around the nearest. */
    public void haunt(List<? extends Player> players) {
        if (players.isEmpty() || !(level() instanceof ServerLevel)) return;
        if (random.nextBoolean()) {
            chill(players);
        } else {
            Player nearest = players.stream().min((a, b) -> Double.compare(a.distanceToSqr(this), b.distanceToSqr(this))).orElseThrow();
            if (snuffLights(nearest.blockPosition()) == 0) chill(players);
        }
        if (random.nextInt(3) == 0) playSound(AllSounds.GHOST_WHISPER.get(), 1.0f, 0.8f + random.nextFloat() * 0.3f);
    }

    /** The cold: Slowness I, frost in the air and on the edges of sight. */
    public void chill(List<? extends Player> players) {
        if (!(level() instanceof ServerLevel server)) return;
        for (Player p : players) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, GhostBalance.COLD_TICKS, 0), this);
            p.setTicksFrozen(Math.max(p.getTicksFrozen(), 70));
            server.sendParticles(AllParticles.FROST.get(), p.getX(), p.getY() + 1.0, p.getZ(), 14, 0.5, 0.7, 0.5, 0.01);
        }
    }

    /**
     * Snuffs out lights around {@code around}: candles and campfires go dark, torches fall off the
     * wall. At most {@link GhostBalance#SNUFF_MAX} at a time. Returns how many.
     */
    public int snuffLights(BlockPos around) {
        if (!(level() instanceof ServerLevel server)) return 0;
        int r = GhostBalance.SNUFF_RADIUS;
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(around.offset(-r, -3, -r), around.offset(r, 3, r))) {
            if (snuffable(server.getBlockState(p))) found.add(p.immutable());
        }
        net.minecraft.Util.shuffle(found, random);
        int n = 0;
        for (BlockPos p : found) {
            if (n >= GhostBalance.SNUFF_MAX) break;
            BlockState s = server.getBlockState(p);
            if (s.hasProperty(BlockStateProperties.LIT)) {
                server.setBlock(p, s.setValue(BlockStateProperties.LIT, false), Block.UPDATE_ALL);
                server.playSound(null, p, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.8f);
            } else {
                server.destroyBlock(p, true, this);
                server.playSound(null, p, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 1.4f);
            }
            server.sendParticles(ParticleTypes.SMOKE, p.getX() + 0.5, p.getY() + 0.6, p.getZ() + 0.5, 6, 0.1, 0.1, 0.1, 0.01);
            n++;
        }
        return n;
    }

    static boolean snuffable(BlockState s) {
        if (s.getBlock() instanceof AbstractCandleBlock || s.getBlock() instanceof CampfireBlock) {
            return s.hasProperty(BlockStateProperties.LIT) && s.getValue(BlockStateProperties.LIT);
        }
        return s.getBlock() instanceof BaseTorchBlock && s.is(AllTags.Blocks.SNUFFABLE)
                && !(s.getBlock() instanceof RedstoneTorchBlock) && !(s.getBlock() instanceof RedstoneWallTorchBlock);
    }

    /** Telekinesis: hurls {@code target} away from it and hurts it. It shows itself as it screams. */
    public void lashOut(LivingEntity target) {
        if (!(level() instanceof ServerLevel server)) return;
        manifest(GhostBalance.MANIFEST_TICKS);
        triggerAnim("action", GhostAnimations.SCREAM);
        getLookControl().setLookAt(target);
        Vec3 push = target.position().subtract(position());
        push = push.horizontalDistanceSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : push.normalize();
        target.hurt(AllDamageTypes.source(server, AllDamageTypes.SPELL, this), GhostBalance.TELEKINESIS_DAMAGE);
        target.knockback(GhostBalance.TELEKINESIS_PUSH, -push.x, -push.z);
        target.push(0, GhostBalance.TELEKINESIS_LIFT, 0);
        target.hurtMarked = true;
        playSound(AllSounds.GHOST_WAIL.get(), 1.0f, 1.3f);
        server.sendParticles(AllParticles.FROST.get(), target.getX(), target.getY() + 1.0, target.getZ(), 16, 0.4, 0.5, 0.4, 0.05);
    }

    // --- sound --------------------------------------------------------------------------------------

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        if (isInert()) return null;
        return random.nextInt(3) == 0 ? AllSounds.GHOST_WHISPER.get() : AllSounds.GHOST_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.GHOST_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.GHOST_DISPERSE.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    // --- glow (client) ------------------------------------------------------------------------------

    @Override
    public boolean isCurrentlyGlowing() {
        // Only someone with Second Sight sees its outline: a client-side answer for the local player.
        if (level().isClientSide && FMLEnvironment.dist == Dist.CLIENT
                && org.papiricoh.supernaturalcraft.client.ghost.GhostClientHooks.glows(this)) return true;
        return super.isCurrentlyGlowing();
    }

    // --- save ---------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (bones != null) tag.putLong("Bones", bones.asLong());
        tag.putLong("DispersedAt", entityData.get(DISPERSED_AT));
        tag.putLong("FadeAt", entityData.get(FADE_AT));
        tag.putBoolean("FadeIsRest", fadeIsRest);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        bones = tag.contains("Bones") ? BlockPos.of(tag.getLong("Bones")) : null;
        if (tag.contains("DispersedAt")) entityData.set(DISPERSED_AT, tag.getLong("DispersedAt"));
        if (tag.contains("FadeAt")) entityData.set(FADE_AT, tag.getLong("FadeAt"));
        fadeIsRest = tag.getBoolean("FadeIsRest");
    }

    // --- GeckoLib -----------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base", 5, state -> {
            if (isInert()) return state.setAndContinue(FADE);
            double dx = getX() - xo, dz = getZ() - zo;
            return state.setAndContinue(dx * dx + dz * dz > 4.0e-4 ? FLOAT : IDLE);
        }));
        controllers.add(new AnimationController<>(this, "action", 2, state -> PlayState.STOP)
                .triggerableAnim(GhostAnimations.SCREAM, RawAnimation.begin().thenPlay(GhostAnimations.clip(GhostAnimations.SCREAM)))
                .triggerableAnim(GhostAnimations.FLICKER, RawAnimation.begin().thenPlay(GhostAnimations.clip(GhostAnimations.FLICKER))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /** Unused: the ghost never fights hand to hand. */
    @Override
    public boolean doHurtTarget(Entity target) {
        return false;
    }
}
