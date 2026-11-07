package org.papiricoh.supernaturalcraft.entity.boss.eve;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.entity.legacy.VampireEntity;
import org.papiricoh.supernaturalcraft.entity.legacy.WerewolfEntity;
import org.papiricoh.supernaturalcraft.network.EveFxPayload;
import org.papiricoh.supernaturalcraft.purgatory.mob.PurgatorySoul;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.GeckoLibServices;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Eve, the Mother of All (v0.39): the first of the monsters, the boss of Purgatory, in her glade (Dick Roman's death throws the
 * hunters there; {@code EveSummoning} starts the fight when one walks in). Four phases, a quarter of her true health each, and no
 * special weakness:
 *
 * <pre>EMERGING (she rises out of the roots) → P1 the Mother (her lash, the roots, her children) → P2 the Swarm (roaches pour out of
 *   her) → P3 Mother's Call (Purgatory's souls turn on the hunters; the ring of thorns) → P4 the Mother of All (the whole glade
 *   erupts) → DYING (she withers)</pre>
 *
 * <p>Her children (Purgatory's vampires and werewolves, born frail) and the souls who answer her call are her minions: a child or
 * soul put down is taken back into the earth at once, whatever put it down. Beaten, she opens the way home ({@code EveSpoils}).
 */
public class EveEntity extends LuciferEntity {

    public static final int MAX_PHASE = EveBalance.PHASES;

    private boolean emerged, inGlade, overgrown;
    /** Tests: hunters her attacks see beyond the challengers (fake players are not in the level's list). */
    private final List<LivingEntity> tracked = new ArrayList<>();
    private @Nullable Supplier<BossAttack<LuciferEntity>> queued;
    private int births;
    /** The children she bore this fight (they wither with her; the souls who answered her call go back to the woods). */
    private final List<UUID> born = new ArrayList<>();

    public EveEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 1000;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, VANILLA_BASE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    // --- her numbers --------------------------------------------------------------------------------

    @Override
    public int maxPhase() {
        return MAX_PHASE;
    }

    @Override
    protected float threshold(int phase) {
        return EveBalance.threshold(phase);
    }

    @Override
    protected double healthPerExtraPlayer() {
        return SNConfig.EVE_HEALTH_PER_PLAYER.get();
    }

    @Override
    protected float mundaneMultiplier() {
        return SNConfig.EVE_MUNDANE_MULTIPLIER.get().floatValue();
    }

    @Override
    protected float damageFactor() {
        return SNConfig.EVE_DAMAGE_FACTOR.get().floatValue();
    }

    @Override
    public boolean isAerialPhase() {
        return false;
    }

    @Override
    public float scale(int phase) {
        return phase >= MAX_PHASE ? 1.1f : 1f;
    }

    @Override
    protected int emergeTicks() {
        return EveCinematics.INTRO_TICKS;
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return EveAttacks.pool(phase);
    }

    @Override
    protected int baseGap(int phase) {
        return EveBalance.attackGap(phase);
    }

    @Override
    protected String animationPrefix() {
        return "animation.eve.";
    }

    @Override
    protected List<String> triggeredAnimations() {
        return EveAssets.TRIGGERED;
    }

    /** {@code entity.supernaturalcraft.eve.bar.phase<n>}: the HUD's key for her bar. */
    @Override
    protected String bossBarKey(int phase) {
        return "entity.supernaturalcraft.eve.bar.phase" + phase;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return phase == 1 ? BossEvent.BossBarColor.GREEN : phase == 3 ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.YELLOW;
    }

    @Override
    protected Component bossBarName(int phase) {
        return Component.translatable(bossBarKey(phase)).withStyle(phase >= MAX_PHASE ? ChatFormatting.GOLD : ChatFormatting.DARK_GREEN);
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return ParticleTypes.SPORE_BLOSSOM_AIR;
    }

    @Override
    protected SoundEvent emergeSound() {
        return AllSounds.purgatory("eve_laugh");
    }

    @Override
    protected SoundEvent roarSound() {
        return AllSounds.purgatory("eve_call");
    }

    @Override
    protected SoundEvent transformSound() {
        return AllSounds.purgatory("eve_roots");
    }

    @Override
    protected SoundEvent ambientBossSound() {
        return AllSounds.purgatory("eve_whisper");
    }

    @Override
    protected SoundEvent dyingSound() {
        return AllSounds.purgatory("eve_death");
    }

    @Override
    protected SoundEvent deflectSound() {
        return AllSounds.purgatory("eve_laugh");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.purgatory("eve_hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.purgatory("eve_death");
    }

    /** Client: roots and mould about her feet as she rises. */
    @Override
    protected void clientEmergenceParticles() {
        level().addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, getRandomX(1.5), getY() + 0.1, getRandomZ(1.5), 0, 0.05, 0);
        level().addParticle(ParticleTypes.MYCELIUM, getRandomX(1.2), getY() + 0.2, getRandomZ(1.2), 0, 0.02, 0);
    }

    /** From the Swarm on, roaches drop from her skirts; in the last phase, amber motes rise off the crown. */
    @Override
    protected void clientPhaseParticles(int phase) {
        if (random.nextFloat() < 0.12f) level().addParticle(ParticleTypes.MYCELIUM, getRandomX(0.5), getY() + 0.3, getRandomZ(0.5), 0, -0.02, 0);
        if (phase >= MAX_PHASE && random.nextFloat() < 0.15f) {
            level().addParticle(ParticleTypes.WAX_ON, getRandomX(0.4), getY() + getBbHeight() + 0.1, getRandomZ(0.4), 0, 0.03, 0);
        }
    }

    @Override
    protected void dyingParticles(ServerLevel level, boolean last) {
        if (last) {
            level.sendParticles(ParticleTypes.ASH, getX(), getY() + 1, getZ(), 160, 0.7, 1.0, 0.7, 0.1);
            level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1, getZ(), 40, 0.5, 0.8, 0.5, 0.05);
            return;
        }
        level.sendParticles(ParticleTypes.ASH, getX(), getY() + 1.1, getZ(), 4, 0.3, 0.6, 0.3, 0.02);
    }

    // --- the fight's shape --------------------------------------------------------------------------

    /** Spawned by egg or command: a glade-themed arena round wherever she stands. */
    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        return LuciferSummoning.openArena(level, blockPosition(), SNConfig.EVE_ARENA_RADIUS.get(), ArenaTheme.GLADE);
    }

    @Override
    protected void playEmergence() {
        EveCinematics.intro(this);
    }

    @Override
    protected void playTransition(int to) {
        EveCinematics.transition(this, to);
    }

    @Override
    protected void playDeath() {
        EveCinematics.death(this);
    }

    @Override
    protected void onTransitionStart(int to) {
        if (to == 3) queue(EveAttacks.Call::new);
        else if (to >= MAX_PHASE) queue(EveAttacks.RootField::new);
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
    protected int deathTicks() {
        return EveBalance.DEATH_TICKS;
    }

    /** As she starts to wither, her children wither with her; the souls she called forget her. */
    @Override
    protected void beginDying() {
        if (level() instanceof ServerLevel level) {
            for (UUID id : minions()) {
                Entity e = level.getEntity(id);
                if (e == null) continue;
                if (born.contains(id)) {
                    level.sendParticles(ParticleTypes.ASH, e.getX(), e.getY() + 1, e.getZ(), 20, 0.3, 0.6, 0.3, 0.03);
                    e.discard();
                } else if (e instanceof Mob m) {
                    m.setTarget(null);
                }
            }
            born.clear();
        }
        super.beginDying();
    }

    @Override
    protected void tickDyingMotion(int elapsed) {
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
    }

    /** The Mother of All: her glade overgrows, roots and moss breaking up the open ground (all of it given back). */
    @Override
    protected void applyTerrain(ServerLevel level, ArenaController arena, int phase) {
        if (phase < MAX_PHASE || overgrown) return;
        overgrown = true;
        BlockPos c = arena.center();
        int r = arena.radius() - 2;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r || random.nextFloat() > 0.18f) continue;
                for (int dy = 2; dy >= -3; dy--) {
                    BlockPos p = c.offset(dx, dy, dz);
                    if (!level.getBlockState(p.above()).isAir()) continue;
                    var s = level.getBlockState(p);
                    if (s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.PODZOL) || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.DIRT)) {
                        arena.mutate(level, p, random.nextBoolean() ? Blocks.ROOTED_DIRT.defaultBlockState() : Blocks.MOSS_BLOCK.defaultBlockState(), 0);
                        break;
                    }
                }
            }
        }
    }

    /** Fought in her glade: the clearing is real, nothing is laid round her. */
    public void fightInGlade() {
        inGlade = true;
    }

    public boolean inGlade() {
        return inGlade;
    }

    /** The fight was lost: she sinks back into the roots, nothing is left behind. */
    @Override
    protected void leaveBehind(ServerLevel level, Vec3 at) {
    }

    @Override
    protected void returnToCage(ServerLevel level, String messageKey) {
        super.returnToCage(level, messageKey.replace(".lucifer.", ".eve."));
    }

    /** At the end of her death: the victory, her spoils per hunter, and the way home. */
    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        EveCinematics.victory(this);
        EveSpoils.give(level, this, hunters(), arena);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (isRemoved() || !(level() instanceof ServerLevel level)) return;
        if (tickCount % 10 == 0) reclaim(level);
        if (state() == EMERGING || emerged) return;
        emerged = true;
        fx(new EveFxPayload(getId(), EveFxPayload.TITLE, phase(), 0, position(), 70));
    }

    // --- her children ------------------------------------------------------------------------------------------------------

    /** Her children and the souls who answered her, still standing. */
    public int children() {
        int n = 0;
        if (level() instanceof ServerLevel level) {
            for (UUID id : minions()) if (level.getEntity(id) instanceof LivingEntity e && e.isAlive()) n++;
        }
        return n;
    }

    /** A child or soul put down (a vampire downed, a werewolf broken off to mend) is taken back into the earth at once. */
    private void reclaim(ServerLevel level) {
        for (UUID id : List.copyOf(minions())) {
            Entity e = level.getEntity(id);
            if (e == null || !e.isAlive()) {
                minions().remove(id);
                continue;
            }
            boolean down = e instanceof VampireEntity v && v.downed() || e instanceof WerewolfEntity w && w.fleeing();
            if (!down) continue;
            level.sendParticles(ParticleTypes.SOUL, e.getX(), e.getY() + 1, e.getZ(), 16, 0.3, 0.5, 0.3, 0.03);
            e.discard();
            minions().remove(id);
        }
    }

    /** Where her {@code i}th child climbs out: round the arena's centre. */
    public Vec3 birthSpot(int i) {
        ArenaController arena = arena();
        Vec3 c = arena != null ? arena.centerVec() : position();
        double ring = arena != null ? Math.min(11, arena.radius() * 0.5) : 6;
        double a = Math.PI * 2 * (i + births * 3) / 8.0 + Math.PI / 8;
        return LuciferAttacks.floorAt(this, c.add(Math.cos(a) * ring, 0, Math.sin(a) * ring));
    }

    /** A child climbs out at {@code at} and goes for {@code target}: a frail soul of a vampire or a werewolf. */
    public @Nullable Mob birth(Vec3 at, @Nullable LivingEntity target) {
        if (!(level() instanceof ServerLevel level) || children() >= EveBalance.CHILD_CAP) return null;
        Mob child = (births++ % 2 == 0 ? AllEntities.PURGATORY_VAMPIRE.get() : AllEntities.PURGATORY_WEREWOLF.get()).create(level);
        if (child == null) return null;
        child.moveTo(at.x, at.y, at.z, random.nextFloat() * 360, 0);
        var hp = child.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(EveBalance.CHILD_HEALTH);
        child.setHealth(EveBalance.CHILD_HEALTH);
        var dmg = child.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.setBaseValue(EveBalance.CHILD_DAMAGE);
        child.setPersistenceRequired();
        level.addFreshEntity(child);
        if (target != null) child.setTarget(target);
        minions().add(child.getUUID());
        born.add(child.getUUID());
        level.sendParticles(ParticleTypes.MYCELIUM, at.x, at.y + 0.2, at.z, 30, 0.6, 0.2, 0.6, 0.05);
        return child;
    }

    /** Mother's call: every soul of Purgatory within reach turns on the hunters (up to so many). @return how many answered */
    public int callSouls(@Nullable LivingEntity target) {
        if (!(level() instanceof ServerLevel level)) return 0;
        int n = 0;
        List<LivingEntity> hunters = hunters();
        for (Mob m : level.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(EveBalance.CALL_RANGE),
                m -> m instanceof PurgatorySoul && m.isAlive() && !minions().contains(m.getUUID()))) {
            if (n >= EveBalance.CALL_MAX) break;
            LivingEntity prey = target;
            double best = Double.MAX_VALUE;
            for (LivingEntity h : hunters) {
                double d = h.distanceToSqr(m);
                if (d < best) {
                    best = d;
                    prey = h;
                }
            }
            if (prey != null) m.setTarget(prey);
            m.setPersistenceRequired();
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
            minions().add(m.getUUID());
            fx(new EveFxPayload(m.getId(), EveFxPayload.ANSWERED, 0, 0, m.getEyePosition(), 60));
            n++;
        }
        return n;
    }

    // --- hunters and effects ------------------------------------------------------------------------

    /** The hunters her attacks see: the challengers, and anyone tracked (tests). */
    public List<LivingEntity> hunters() {
        List<LivingEntity> out = new ArrayList<>(challengers());
        tracked.removeIf(e -> !e.isAlive() || e.isRemoved());
        for (LivingEntity e : tracked) if (!out.contains(e)) out.add(e);
        return out;
    }

    /** Test hook: {@code e} counts as one of her hunters. */
    public void track(LivingEntity e) {
        if (!tracked.contains(e)) tracked.add(e);
    }

    /** Sends one of the fight's moments to every hunter near. */
    public void fx(EveFxPayload payload) {
        if (level() instanceof ServerLevel level) EveFxPayload.send(level, position(), payload);
    }

    // --- attacks ------------------------------------------------------------------------------------

    /** Tests and previews: the next attack she uses, whatever the pool says. */
    public void queue(Supplier<BossAttack<LuciferEntity>> attack) {
        queued = attack;
    }

    /** A queued attack first; the roots when the hunter keeps away or hides (they find you anywhere). */
    @Override
    public @Nullable Supplier<BossAttack<LuciferEntity>> forcedAttack(LivingEntity target) {
        if (queued != null) {
            Supplier<BossAttack<LuciferEntity>> q = queued;
            queued = null;
            return q;
        }
        if (distanceToSqr(target) > 16 * 16 || noSightTicks > 60) {
            noSightTicks = 0;
            return EveAttacks.RootGrasp::new;
        }
        return null;
    }

    /** Whether {@code e} is one of the boss's own (her blows pass it by). */
    public boolean isOwn(Entity e) {
        return e instanceof PurgatorySoul || minions().contains(e.getUUID());
    }

    /** Tests: anything in {@code box} around her she would hit. */
    public List<LivingEntity> foesNear(double r) {
        return EveAttacks.foes(this, new AABB(position(), position()).inflate(r));
    }

    // --- GeckoLib -----------------------------------------------------------------------------------

    /** Only her own clips: Lucifer's names that she lacks are dropped. */
    @Override
    public void triggerAnim(@Nullable String controller, String anim) {
        if ("action".equals(controller) && !EveAssets.TRIGGERED.contains(anim)) return;
        if (level().isClientSide) {
            var manager = getAnimatableInstanceCache().getManagerForId(getId());
            if (controller != null) {
                var c = manager.getAnimationControllers().get(controller);
                if (c != null) c.forceAnimationReset();
                manager.tryTriggerAnimation(controller, anim);
            } else {
                manager.tryTriggerAnimation(anim);
            }
        } else {
            GeckoLibServices.NETWORK.triggerEntityAnim(this, false, controller, anim);
        }
    }

    /** Clips that hold their last frame. */
    static final List<String> HOLDS = List.of("death", "emerge");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = animationPrefix();
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + "idle"), walk = RawAnimation.begin().thenLoop(pre + "walk");
        controllers.add(new AnimationController<>(this, "base", 6, state -> {
            if (state() == DYING || state() == EMERGING) return PlayState.STOP;
            return state.setAndContinue(state.isMoving() ? walk : idle);
        }));
        AnimationController<EveEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String name : EveAssets.TRIGGERED) {
            action.triggerableAnim(name, HOLDS.contains(name)
                    ? RawAnimation.begin().thenPlayAndHold(pre + name)
                    : RawAnimation.begin().thenPlay(pre + name));
        }
        controllers.add(action);
    }

    // --- persistence --------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Emerged", emerged);
        tag.putBoolean("InGlade", inGlade);
        tag.putBoolean("Overgrown", overgrown);
        tag.putInt("Births", births);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        emerged = tag.getBoolean("Emerged");
        inGlade = tag.getBoolean("InGlade");
        overgrown = tag.getBoolean("Overgrown");
        births = tag.getInt("Births");
    }
}
