package org.papiricoh.supernaturalcraft.darkness.casifer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.darkness.DarknessRegistry;
import org.papiricoh.supernaturalcraft.darkness.DarknessRules;
import org.papiricoh.supernaturalcraft.darkness.Darknesses;
import org.papiricoh.supernaturalcraft.entity.boss.godstiel.GodstielAssets;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.util.Players;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Casifer (v0.42): Lucifer wearing Castiel, out of the Cage to "help" against his aunt. He walks with one hunter from the
 * Cage visit until Amara takes him ({@link DarknessRules.Step#CASIFER} until {@link DarknessRules.Step#TAKEN}), snaps his
 * fingers at whatever attacks them (an archangel holding back), says what he thinks of everything ({@link #VOICE_LINES}), and
 * points the way to the nearest Hand of God he can feel ({@link CasiferHints}). Untouchable; one per hunter; drawn with
 * Castiel's rig and {@code S11Assets.CASIFER_TEXTURE}. In Amara's phase IV a freed one fights beside Chuck ({@link #FIGHT}).
 */
public class CasiferEntity extends PathfinderMob implements GeoEntity {

    public static final double FOLLOW = 4, BLINK = 24, REACH = 12;
    public static final int SNAP_COOLDOWN = 60, TALK_EVERY = 1200;
    public static final float SNAP_DAMAGE = 16f;
    /** How many of his lines there are ({@code message.supernaturalcraft.casifer.line.<n>}). */
    public static final int VOICE_LINES = 12;
    /** His two jobs: walking with a hunter, or fighting beside Chuck in Amara's fight (no hunter, no leash). */
    public static final byte COMPANION = 0, FIGHT = 1;

    private static final Map<UUID, UUID> WITH = new ConcurrentHashMap<>();
    private static final String PREFIX = "animation." + GodstielAssets.RIG + ".";

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID hunter;
    private byte role = COMPANION;
    private int cooldown = SNAP_COOLDOWN, talk = 400;

    public CasiferEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    /** The Casifer walking with {@code hunter} now, if any. */
    public static @Nullable CasiferEntity with(ServerPlayer hunter) {
        UUID id = WITH.get(hunter.getUUID());
        if (id == null) return null;
        if (hunter.serverLevel().getEntity(id) instanceof CasiferEntity c && c.isAlive()) return c;
        WITH.remove(hunter.getUUID());
        return null;
    }

    /** Whether {@code hunter} should have him at their side (their road is between the Cage and Amara taking him). */
    public static boolean due(ServerPlayer hunter) {
        DarknessRules.Step s = Darknesses.step(hunter);
        return s.atLeast(DarknessRules.Step.CASIFER) && !s.atLeast(DarknessRules.Step.TAKEN);
    }

    /** He finds {@code hunter} (behind them, with a snap). */
    public static @Nullable CasiferEntity arrive(ServerPlayer hunter) {
        if (with(hunter) != null) return null;
        ServerLevel level = hunter.serverLevel();
        CasiferEntity c = DarknessRegistry.CASIFER.get().create(level);
        if (c == null) return null;
        Vec3 at = hunter.position().subtract(Vec3.directionFromRotation(0, hunter.getYRot()).scale(2));
        c.moveTo(at.x, hunter.getY(), at.z, hunter.getYRot(), 0);
        c.hunter = hunter.getUUID();
        level.addFreshEntity(c);
        WITH.put(hunter.getUUID(), c.getUUID());
        c.snapFx(level);
        return c;
    }

    /** Amara's phase IV: a freed Casifer at {@code at}, fighting beside Chuck. */
    public static @Nullable CasiferEntity freed(ServerLevel level, Vec3 at) {
        CasiferEntity c = DarknessRegistry.CASIFER.get().create(level);
        if (c == null) return null;
        c.role = FIGHT;
        c.moveTo(at.x, at.y, at.z, 0, 0);
        level.addFreshEntity(c);
        c.snapFx(level);
        return c;
    }

    public byte role() {
        return role;
    }

    private void snapFx(ServerLevel level) {
        level.playSound(null, blockPosition(), DarknessRegistry.CASIFER_SOUNDS.get("casifer.snap").get(), SoundSource.NEUTRAL, 1f, 1f);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, getX(), getY() + 1.2, getZ(), 16, 0.3, 0.6, 0.3, 0.02);
        level.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 1.2, getZ(), 10, 0.3, 0.6, 0.3, 0.05);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 12f, 1f));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        if (role == COMPANION) {
            ServerPlayer p = hunter == null ? null : Players.byUuid(level, hunter) instanceof ServerPlayer sp ? sp : null;
            if (p == null || !p.isAlive() || p.level() != level || !due(p)) {
                snapFx(level);
                if (hunter != null) WITH.remove(hunter);
                discard();
                return;
            }
            double d = distanceToSqr(p);
            if (d > BLINK * BLINK) {
                Vec3 at = p.position().subtract(Vec3.directionFromRotation(0, p.getYRot()).scale(2));
                teleportTo(at.x, p.getY(), at.z);
                snapFx(level);
            } else if (d > FOLLOW * FOLLOW) {
                getNavigation().moveTo(p, 1.15);
            } else {
                getNavigation().stop();
            }
            if (--talk <= 0) {
                talk = TALK_EVERY + random.nextInt(600);
                speak(p);
            }
        }
        if (--cooldown <= 0) snap(level);
    }

    /** A line of his to his hunter, and now and then the way to a Hand. */
    private void speak(ServerPlayer p) {
        triggerAnim("action", "talk");
        if (random.nextFloat() < 0.5f && CasiferHints.hint(p, this)) return;
        int n = random.nextInt(VOICE_LINES);
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.casifer.line." + n).withStyle(ChatFormatting.RED, ChatFormatting.ITALIC), false);
        if (random.nextFloat() < 0.3f) level().playSound(null, blockPosition(), DarknessRegistry.CASIFER_SOUNDS.get("casifer.laugh").get(), SoundSource.NEUTRAL, 0.8f, 1f);
    }

    /** He smites the nearest enemy in reach (never the hunters, never her: he will not be the one to face her). */
    private void snap(ServerLevel level) {
        LivingEntity target = null;
        double best = REACH * REACH;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(REACH),
                e -> e instanceof Enemy && e.isAlive() && !(e instanceof org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity))) {
            double ed = e.distanceToSqr(this);
            if (ed < best) {
                best = ed;
                target = e;
            }
        }
        if (target == null) {
            cooldown = 20;
            return;
        }
        cooldown = SNAP_COOLDOWN;
        getLookControl().setLookAt(target);
        triggerAnim("action", "snap");
        float dmg = target.getType().is(org.papiricoh.supernaturalcraft.registry.AllTags.Entities.BOSSES) ? SNAP_DAMAGE * 0.25f : SNAP_DAMAGE;
        target.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, this), dmg);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, target.getX(), target.getY() + 1, target.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
        level.playSound(null, target.blockPosition(), DarknessRegistry.CASIFER_SOUNDS.get("casifer.snap").get(), SoundSource.NEUTRAL, 0.9f, 1.1f);
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
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (hunter != null) tag.putUUID("Hunter", hunter);
        tag.putByte("Role", role);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Hunter")) {
            hunter = tag.getUUID("Hunter");
            WITH.put(hunter, getUUID());
        }
        role = tag.getByte("Role");
    }

    // --- GeckoLib (Castiel's rig and clips) -----------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop(PREFIX + "idle"), walk = RawAnimation.begin().thenLoop(PREFIX + "walk");
        controllers.add(new AnimationController<>(this, "base", 6, state -> state.setAndContinue(state.isMoving() ? walk : idle)));
        AnimationController<CasiferEntity> action = new AnimationController<>(this, "action", 2, state -> PlayState.STOP);
        for (String clip : new String[]{"snap", "talk", "smite", "appear", "vanish"}) {
            action.triggerableAnim(clip, RawAnimation.begin().thenPlay(PREFIX + clip));
        }
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /** The block he stands on, for tests. */
    public BlockPos feet() {
        return blockPosition();
    }
}
