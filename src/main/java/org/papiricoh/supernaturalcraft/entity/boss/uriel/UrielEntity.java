package org.papiricoh.supernaturalcraft.entity.boss.uriel;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.angel.SmiteDeath;
import org.papiricoh.supernaturalcraft.apocalypse.seal.garrison.GarrisonAnchors;
import org.papiricoh.supernaturalcraft.apocalypse.seal.garrison.GarrisonSite;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.network.UrielFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Uriel, the traitor of the garrison (v0.40, optional; the Seals slice): the angel who has been killing his brothers at the seals
 * for Lucifer. Two phases ({@link UrielBalance}, {@link UrielAttacks}): I, the Soldier, with an escort of rebel angels he calls to
 * his side; II, the Traitor, alone (his rebels flee as his wings open) with flashes of blinding grace.
 *
 * <p>Beaten, no ordinary blow ends him: he stands at the floor of his health, untouchable but to an angel blade
 * ({@code #angelic_blades}, as {@code SmiteDeath} reads a blow). Struck with one, he dies an angel's death; left for
 * {@link UrielBalance#FINISH_TICKS}, his grace pulls him up again. Castiel comes when he is gone ({@code UrielSpoils}).
 */
public class UrielEntity extends LuciferEntity {

    private static final EntityDataAccessor<Boolean> FINISHING = SynchedEntityData.defineId(UrielEntity.class, EntityDataSerializers.BOOLEAN);

    private @Nullable BlockPos garrisonOrigin;
    private int garrisonQ;
    private boolean emerged;
    private int finishTicks;

    public UrielEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 400;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, VANILLA_BASE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FINISHING, false);
    }

    /** Beaten and waiting for the angel blade's blow. */
    public boolean finishing() {
        return entityData.get(FINISHING);
    }

    // --- set-up --------------------------------------------------------------------------------------------------------------

    public void bindGarrison(GarrisonSite site) {
        garrisonOrigin = site.origin().immutable();
        garrisonQ = site.q();
    }

    public @Nullable GarrisonSite garrison() {
        return garrisonOrigin == null ? null : new GarrisonSite(garrisonOrigin, garrisonQ);
    }

    // --- his numbers ---------------------------------------------------------------------------------------------------------

    @Override
    public int maxPhase() {
        return UrielBalance.PHASES;
    }

    @Override
    protected float threshold(int phase) {
        return UrielBalance.threshold(phase);
    }

    @Override
    protected double healthPerExtraPlayer() {
        return UrielBalance.HEALTH_PER_PLAYER;
    }

    @Override
    protected float mundaneMultiplier() {
        return UrielBalance.MUNDANE;
    }

    @Override
    protected float damageFactor() {
        return UrielBalance.DAMAGE_FACTOR;
    }

    @Override
    public boolean isAerialPhase() {
        return false;
    }

    @Override
    public float scale(int phase) {
        return 1f;
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return UrielAttacks.pool(phase);
    }

    @Override
    protected int baseGap(int phase) {
        return UrielBalance.attackGap(phase);
    }

    @Override
    protected String animationPrefix() {
        return "animation." + UrielAssets.RIG + ".";
    }

    @Override
    protected List<String> triggeredAnimations() {
        return UrielAssets.TRIGGERED;
    }

    @Override
    protected String bossBarKey(int phase) {
        return UrielAssets.BAR_PREFIX + ".phase" + phase;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return phase <= 1 ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.WHITE;
    }

    @Override
    protected Component bossBarName(int phase) {
        return Component.translatable(bossBarKey(phase)).withStyle(phase <= 1 ? ChatFormatting.GOLD : ChatFormatting.WHITE);
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return AllParticles.GRACE.get();
    }

    @Override
    protected SoundEvent emergeSound() {
        return AllSounds.apocalypse("uriel.wings");
    }

    @Override
    protected SoundEvent roarSound() {
        return AllSounds.apocalypse("uriel.smite");
    }

    @Override
    protected SoundEvent transformSound() {
        return AllSounds.apocalypse("uriel.wings");
    }

    @Override
    protected SoundEvent ambientBossSound() {
        return AllSounds.apocalypse("uriel.wings");
    }

    @Override
    protected SoundEvent dyingSound() {
        return AllSounds.apocalypse("uriel.death");
    }

    @Override
    protected SoundEvent deflectSound() {
        return AllSounds.apocalypse("uriel.hurt");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.apocalypse("uriel.hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.apocalypse("uriel.death");
    }

    @Override
    protected void clientEmergenceParticles() {
        level().addParticle(AllParticles.GRACE.get(), getRandomX(0.8), getY() + random.nextDouble() * 2, getRandomZ(0.8), 0, 0.05, 0);
    }

    @Override
    protected void clientPhaseParticles(int phase) {
        if (random.nextFloat() < 0.15f) {
            level().addParticle(AllParticles.GRACE.get(), getRandomX(1.0), getY() + 1 + random.nextDouble(), getRandomZ(1.0), 0, 0.02, 0);
        }
    }

    @Override
    protected void dyingParticles(ServerLevel level, boolean last) {
        if (last) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH, getX(), getY() + 1.2, getZ(), 2, 0, 0, 0, 0);
            level.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 1.2, getZ(), 120, 1.2, 1.5, 1.2, 0.25);
            return;
        }
        level.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 1.5, getZ(), 4, 0.3, 0.5, 0.3, 0.05);
    }

    // --- the fight's shape ---------------------------------------------------------------------------------------------------

    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        return LuciferSummoning.openArena(level, blockPosition(), UrielSummoning.FREE_RADIUS, ArenaTheme.GARRISON);
    }

    @Override
    protected void applyTerrain(ServerLevel level, ArenaController arena, int phase) {
    }

    @Override
    protected void playEmergence() {
        fx(UrielFxPayload.TITLE, 1, 0, position(), 80);
    }

    @Override
    protected void playTransition(int to) {
        fx(UrielFxPayload.TITLE, to, 0, position(), 80);
    }

    @Override
    protected void playDeath() {
        fx(UrielFxPayload.DEATH, 0, 0, position(), UrielBalance.DEATH_TICKS);
        fx(UrielFxPayload.TITLE, 3, 0, position(), 80);
    }

    @Override
    protected int transitionTicks(int to) {
        return UrielBalance.TRANSITION;
    }

    @Override
    protected int deathTicks() {
        return UrielBalance.DEATH_TICKS;
    }

    @Override
    protected void onTransitionStart(int to) {
        // Alone for the last: his rebels flee as his wings open.
        triggerAnim("action", "unfurl");
        fx(UrielFxPayload.WINGS, 0, 0, position(), 40);
        if (level() instanceof ServerLevel level) dismissRebels(level);
    }

    @Override
    protected void tickTransitionMotion(int elapsed, boolean last) {
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        getNavigation().stop();
    }

    @Override
    protected void tickEmergence() {
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
    }

    @Override
    protected void tickDyingMotion(int elapsed) {
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
    }

    @Override
    protected void leaveBehind(ServerLevel level, Vec3 at) {
    }

    @Override
    protected void returnToCage(ServerLevel level, String messageKey) {
        super.returnToCage(level, messageKey.replace(".lucifer.", ".uriel."));
    }

    @Override
    protected boolean walks() {
        return !finishing();
    }

    /** Where his rebels come down: the garrison's spawn points, or round him. */
    public List<Vec3> spawnPoints() {
        GarrisonSite site = garrison();
        if (site != null) {
            List<Vec3> s = site.spawns();
            if (!s.isEmpty()) return s;
        }
        List<Vec3> out = new ArrayList<>();
        ArenaController arena = arena();
        Vec3 c = arena != null ? Vec3.atBottomCenterOf(arena.center()) : position();
        for (int i = 0; i < GarrisonAnchors.MIN_SPAWNS; i++) {
            double a = i * Math.PI * 2 / GarrisonAnchors.MIN_SPAWNS;
            out.add(LuciferAttacks.floorAt(this, c.add(Math.cos(a) * 9, 0, Math.sin(a) * 9)));
        }
        return out;
    }

    @Override
    protected Vec3 tetherPoint(ArenaController arena) {
        GarrisonSite site = garrison();
        return site != null ? site.floor(GarrisonAnchors.URIEL) : Vec3.atBottomCenterOf(arena.center());
    }

    // --- his rebels ----------------------------------------------------------------------------------------------------------

    /** His rebels alive now. */
    public int rebels() {
        if (!(level() instanceof ServerLevel level)) return 0;
        int n = 0;
        for (UUID id : minions()) {
            Entity e = level.getEntity(id);
            if (e != null && e.isAlive()) n++;
        }
        return n;
    }

    /** A rebel angel comes down at {@code at} to fight for him. */
    public void raiseRebel(Vec3 at) {
        if (!(level() instanceof ServerLevel level)) return;
        Mob m = AllEntities.REBEL_ANGEL.get().create(level);
        if (m == null) return;
        m.moveTo(at.x, at.y, at.z, random.nextFloat() * 360f, 0);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.MOB_SUMMONED, null);
        m.setPersistenceRequired();
        level.addFreshEntity(m);
        minions().add(m.getUUID());
        level.sendParticles(AllParticles.GRACE.get(), at.x, at.y + 1, at.z, 16, 0.3, 0.8, 0.3, 0.06);
    }

    /** His escort, as the fight begins. */
    public void raiseEscort() {
        List<Vec3> spots = minionSpots(spawnPoints());
        for (int i = 0; i < Math.min(spots.size(), minionCount(UrielBalance.ESCORT)); i++) raiseRebel(spots.get(i));
    }

    private void dismissRebels(ServerLevel level) {
        for (UUID id : new ArrayList<>(minions())) {
            Entity e = level.getEntity(id);
            if (e != null && e.isAlive()) {
                level.sendParticles(AllParticles.GRACE.get(), e.getX(), e.getY() + 1, e.getZ(), 10, 0.3, 0.8, 0.3, 0.05);
                e.discard();
            }
        }
        minions().clear();
    }

    /** The ground cracks under his palm: the top of the floor broken for a while (restored with the arena). */
    public void crack(Vec3 at, float radius) {
        ArenaController arena = arena();
        if (arena == null || !(level() instanceof ServerLevel level)) return;
        int r = (int) Math.ceil(radius);
        BlockPos c = BlockPos.containing(at).below();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > radius * radius || random.nextInt(3) != 0) continue;
                BlockPos p = c.offset(dx, 0, dz);
                BlockState s = level.getBlockState(p);
                if (!s.isSolidRender(level, p) || level.getBlockEntity(p) != null || !level.getBlockState(p.above()).isAir()) continue;
                arena.mutate(level, p, (random.nextBoolean() ? Blocks.CRACKED_STONE_BRICKS : Blocks.COARSE_DIRT).defaultBlockState(), 400);
            }
        }
    }

    /** Bars across the garrison's gate (nobody leaves): called as the arena opens. */
    public void barGate(ServerLevel level, ArenaController arena) {
        GarrisonSite site = garrison();
        if (site == null) return;
        for (BlockPos p : site.gate()) {
            if (level.getBlockState(p).isAir() || level.getBlockState(p).canBeReplaced()) arena.mutate(level, p, Blocks.IRON_BARS.defaultBlockState(), 0);
        }
    }

    // --- his end -------------------------------------------------------------------------------------------------------------

    @Override
    protected boolean interceptDeath() {
        scheduler().cancel();
        entityData.set(FINISHING, true);
        finishTicks = 0;
        triggerAnim("action", "kneel");
        if (level() instanceof ServerLevel level) dismissRebels(level);
        tell("message.supernaturalcraft.uriel.finish", ChatFormatting.GOLD);
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        if (finishing()) {
            if (SmiteDeath.angelicBlow(source) && source.getEntity() instanceof ServerPlayer p) {
                setLastHurtByPlayer(p);
                entityData.set(FINISHING, false);
                beginDying();
                return true;
            }
            if (source.getEntity() instanceof ServerPlayer p && tickCount % 10 == 0) {
                p.displayClientMessage(Component.translatable("message.supernaturalcraft.uriel.blade_only").withStyle(ChatFormatting.GOLD), true);
                playSound(deflectSound(), 1f, 0.8f);
            }
            return false;
        }
        return super.hurt(source, amount);
    }

    private void tickFinishing() {
        getNavigation().stop();
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        scheduler().delay(5);
        if (++finishTicks >= UrielBalance.FINISH_TICKS) {
            // Nobody brought the blade: his grace pulls him up again.
            entityData.set(FINISHING, false);
            BossHealthGuard.set(this, getMaxHealth() * UrielBalance.RALLY);
            triggerAnim("action", "unfurl");
            tell("message.supernaturalcraft.uriel.rallies", ChatFormatting.GOLD, ChatFormatting.ITALIC);
            scheduler().delay(40);
        }
    }

    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        UrielSpoils.defeated(level, this, new ArrayList<>(challengers()));
    }

    // --- per tick ------------------------------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        if (level() instanceof ServerLevel && !isRemoved() && finishing() && state() != DYING) tickFinishing();
        super.customServerAiStep();
        if (isRemoved() || !(level() instanceof ServerLevel)) return;
        if (state() == EMERGING || emerged) return;
        emerged = true;
        if (phase() == 1) raiseEscort();
    }

    public void sound(String id, float volume, float pitch) {
        level().playSound(null, blockPosition(), AllSounds.apocalypse("uriel." + id), SoundSource.HOSTILE, volume, pitch);
    }

    public void fx(byte kind, int arg, int arg2, Vec3 point, int duration) {
        if (!(level() instanceof ServerLevel level)) return;
        UrielFxPayload payload = new UrielFxPayload(getId(), kind, arg, arg2, point, duration);
        for (ServerPlayer p : level.players()) if (p.distanceToSqr(this) < 96 * 96) Send.toPlayer(p, payload);
    }

    public void tell(String key, ChatFormatting... style) {
        if (!(level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 64 * 64) p.displayClientMessage(Component.translatable(key).withStyle(style), false);
        }
    }

    // --- animation -----------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = animationPrefix();
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + "idle"), walk = RawAnimation.begin().thenLoop(pre + "walk"),
                hover = RawAnimation.begin().thenLoop(pre + "hover");
        controllers.add(new AnimationController<>(this, "base", 5, state -> {
            if (state() == DYING || state() == EMERGING || finishing()) return PlayState.STOP;
            if (state.isMoving()) return state.setAndContinue(walk);
            return state.setAndContinue(phase() >= 2 ? hover : idle);
        }));
        AnimationController<LuciferEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String name : triggeredAnimations()) {
            boolean hold = name.equals("death") || name.equals("kneel");
            action.triggerableAnim(name, hold ? RawAnimation.begin().thenPlayAndHold(pre + name) : RawAnimation.begin().thenPlay(pre + name));
        }
        controllers.add(action);
    }

    // --- persistence ---------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Emerged", emerged);
        if (garrisonOrigin != null) {
            tag.put("Garrison", NbtUtils.writeBlockPos(garrisonOrigin));
            tag.putInt("GarrisonQ", garrisonQ);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        emerged = tag.getBoolean("Emerged");
        garrisonOrigin = NbtUtils.readBlockPos(tag, "Garrison").orElse(null);
        garrisonQ = tag.getInt("GarrisonQ");
    }
}
