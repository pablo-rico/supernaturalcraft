package org.papiricoh.supernaturalcraft.entity.boss.dick;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.HorsemenGround;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.entity.leviathan.LeviathanEntity;
import org.papiricoh.supernaturalcraft.leviathan.BoraxSensitive;
import org.papiricoh.supernaturalcraft.leviathan.LeviathanGooBlock;
import org.papiricoh.supernaturalcraft.network.DickFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.GeckoLibServices;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Dick Roman (v0.25), head of Roman Enterprises and of the Leviathans: an optional boss after Raphael, called by a rite with
 * the bone of a righteous mortal washed in the blood of the three ({@code summon_dick_roman}) into his penthouse boardroom
 * ({@link ArenaTheme#BOARDROOM}, {@link BoardroomGround}). Four phases, a quarter of his true health each:
 *
 * <pre>EMERGING (he steps out, buttoning his jacket) → P1 the Executive → P2 the Maw (his head splits) → P3 the Copy (he wears
 *   a hunter's face among doubles) → P4 the Flood (the boardroom drowns in goo) → DYING (he bursts)</pre>
 *
 * <p>Borax is the hunter's friend: it shows him for what he is when he hides among his doubles ({@link #onBorax}: he staggers,
 * {@link DickBalance#EXPOSED_HARM} more harm a while), and dries the Flood's goo into islands ({@link #dryGoo}) until he
 * surges it back. Optional: he blocks nothing ({@code BossProgression.Boss.optional}).
 */
public class DickRomanEntity extends LuciferEntity implements BoraxSensitive {

    private static final EntityDataAccessor<String> DISGUISE = SynchedEntityData.defineId(DickRomanEntity.class, EntityDataSerializers.STRING);
    public static final int MAX_PHASE = DickBalance.PHASES;

    private @Nullable HorsemenGround ground, flood;
    private boolean groundLaid, flooding, floodLaid, emerged;
    /**
     * v0.28: fought in his own penthouse, the SucroCorp tower's boardroom, as the Leviathans' Lord: the room is real, so
     * nothing is laid round him, and the Flood fills its open floor ({@link #openFloor}).
     */
    private boolean inPlace;
    private int hiddenTicks, exposedTicks;
    private final List<UUID> doubles = new ArrayList<>();
    /** Tests: hunters his attacks see beyond the challengers (fake players are not in the level's list). */
    private final List<LivingEntity> tracked = new ArrayList<>();
    /** Tests and previews: an attack to use next, whatever the pool says. */
    private @Nullable Supplier<BossAttack<LuciferEntity>> queued;

    public DickRomanEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 700;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, VANILLA_BASE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DISGUISE, "");
    }

    // --- his numbers --------------------------------------------------------------------------------

    @Override
    public int maxPhase() {
        return MAX_PHASE;
    }

    @Override
    protected float threshold(int phase) {
        return DickBalance.threshold(phase);
    }

    @Override
    protected double healthPerExtraPlayer() {
        return SNConfig.DICK_HEALTH_PER_PLAYER.get();
    }

    @Override
    protected float mundaneMultiplier() {
        return SNConfig.DICK_MUNDANE_MULTIPLIER.get().floatValue();
    }

    @Override
    protected float damageFactor() {
        return SNConfig.DICK_DAMAGE_FACTOR.get().floatValue();
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
    protected int emergeTicks() {
        return DickCinematics.INTRO_TICKS;
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return DickAttacks.pool(phase);
    }

    @Override
    protected int baseGap(int phase) {
        return DickBalance.attackGap(phase);
    }

    @Override
    protected String animationPrefix() {
        return "animation.dick_roman.";
    }

    @Override
    protected List<String> triggeredAnimations() {
        return DickAssets.TRIGGERED;
    }

    /** {@code entity.supernaturalcraft.dick_roman.bar.phase<n>}: the HUD's key for his bar. */
    @Override
    protected String bossBarKey(int phase) {
        return "entity.supernaturalcraft.dick_roman.bar.phase" + phase;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return phase == 1 ? BossEvent.BossBarColor.WHITE : phase == 3 ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.RED;
    }

    @Override
    protected Component bossBarName(int phase) {
        return Component.translatable(bossBarKey(phase)).withStyle(phase == 1 ? ChatFormatting.WHITE : ChatFormatting.DARK_RED);
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return ParticleTypes.SQUID_INK;
    }

    @Override
    protected SoundEvent emergeSound() {
        return AllSounds.dick("arrive");
    }

    @Override
    protected SoundEvent roarSound() {
        return AllSounds.dick("maw");
    }

    @Override
    protected SoundEvent transformSound() {
        return AllSounds.dick("maw");
    }

    @Override
    protected SoundEvent ambientBossSound() {
        return AllSounds.dick("laugh");
    }

    @Override
    protected SoundEvent dyingSound() {
        return AllSounds.dick("death");
    }

    @Override
    protected SoundEvent deflectSound() {
        return AllSounds.dick("hurt");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.dick("hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.dick("death");
    }

    @Override
    protected void clientEmergenceParticles() {
    }

    /** From the Maw on, black goo drips from his jaws. */
    @Override
    protected void clientPhaseParticles(int phase) {
        if (phase < 2 || random.nextFloat() >= 0.15f) return;
        level().addParticle(ParticleTypes.SQUID_INK, getRandomX(0.4), getY() + 1.6, getRandomZ(0.4), 0, -0.02, 0);
    }

    @Override
    protected void dyingParticles(ServerLevel level, boolean last) {
        if (last) {
            level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1, getZ(), 120, 0.8, 1.0, 0.8, 0.2);
            return;
        }
        level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1.1, getZ(), 3, 0.3, 0.5, 0.3, 0.02);
    }

    // --- the fight's shape --------------------------------------------------------------------------

    /** Spawned by egg or command: his boardroom round wherever he stands. */
    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        return LuciferSummoning.openArena(level, blockPosition(), SNConfig.DICK_ARENA_RADIUS.get(), ArenaTheme.BOARDROOM);
    }

    @Override
    protected void playEmergence() {
        DickCinematics.intro(this);
    }

    @Override
    protected void playTransition(int to) {
        DickCinematics.transition(this, to);
    }

    @Override
    protected void playDeath() {
        DickCinematics.death(this);
    }

    @Override
    protected void onTransitionStart(int to) {
        endCopy();
        if (to == 2) triggerAnim("action", "maw_open");
        else if (to == 3) queue(DickAttacks.Copy::new);
        else if (to >= MAX_PHASE) triggerAnim("action", "melt");
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
        return DickBalance.DEATH_TICKS;
    }

    @Override
    protected void tickDyingMotion(int elapsed) {
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
    }

    /** The Flood: from the last phase the open floor drowns in goo (laid a few hundred blocks a tick). */
    @Override
    protected void applyTerrain(ServerLevel level, ArenaController arena, int phase) {
        if (phase < MAX_PHASE || flooding) return;
        flooding = true;
        layGroundNow();
        flood = inPlace ? goo(openFloor(level, arena)) : floodOf(ground);
        floodLaid = false;
    }

    /** He fights where he stands, in a room already built: nothing is laid, and the Flood finds the floor itself. */
    public void fightInPlace() {
        inPlace = true;
        groundLaid = true;
    }

    public boolean inPlace() {
        return inPlace;
    }

    /** The air just above the floor in the arena's circle: where the goo goes in his own penthouse. */
    private static List<BlockPos> openFloor(ServerLevel level, ArenaController arena) {
        List<BlockPos> out = new ArrayList<>();
        BlockPos c = arena.center();
        int in = arena.radius() - BoardroomGround.MARGIN;
        for (int dx = -in; dx <= in; dx++)
            for (int dz = -in; dz <= in; dz++) {
                if (dx * dx + dz * dz > in * in) continue;
                for (int dy = -1; dy <= 2; dy++) {
                    BlockPos p = c.offset(dx, dy, dz);
                    if (level.getBlockState(p).isAir() && !level.getBlockState(p.below()).isAir()) {
                        out.add(p);
                        break;
                    }
                }
            }
        return out;
    }

    private static HorsemenGround goo(List<BlockPos> at) {
        return HorsemenGround.fixed(at, Collections.nCopies(at.size(), BoardroomGround.FLOOD));
    }

    private static HorsemenGround floodOf(HorsemenGround ground) {
        List<BlockPos> at = ground.positionsOf(BoardroomGround.FLOOR_AIR);
        return HorsemenGround.fixed(at, Collections.nCopies(at.size(), BoardroomGround.FLOOD));
    }

    /** The fight was lost: nothing is left behind (the bone comes back through the arena's stake). */
    @Override
    protected void leaveBehind(ServerLevel level, Vec3 at) {
    }

    @Override
    protected void returnToCage(ServerLevel level, String messageKey) {
        super.returnToCage(level, messageKey.replace(".lucifer.", ".dick_roman."));
    }

    /** At the end of his death: the victory and his spoils per hunter. */
    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        endCopy();
        DickCinematics.victory(this);
        List<ServerPlayer> hunters = new ArrayList<>(challengers());
        for (LivingEntity e : tracked) if (e instanceof ServerPlayer p && !hunters.contains(p)) hunters.add(p);
        DickSpoils.give(level, hunters, position().add(0, 1.2, 0), random);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (isRemoved() || !(level() instanceof ServerLevel level)) return;
        ArenaController arena = arena();
        if (arena != null && arena.isActive()) tickGround(level, arena);
        if (exposedTicks > 0) exposedTicks--;
        if (hiddenTicks > 0 && --hiddenTicks == 0) endCopy();
        if (state() == EMERGING || emerged) return;
        emerged = true;
        fx(new DickFxPayload(getId(), DickFxPayload.TITLE, phase(), 0, position(), 70));
    }

    private void tickGround(ServerLevel level, ArenaController arena) {
        if (inPlace) {
            if (flood != null && !floodLaid && flood.tick(level, arena)) floodLaid = true;
            return;
        }
        if (ground == null) {
            if (groundLaid) return; // laid before a reload: not pinned again over its own blocks
            ground = HorsemenGround.pin(level, arena, BoardroomGround.plan(arena.radius()));
        }
        if (!groundLaid && ground.tick(level, arena)) groundLaid = true;
        if (flood != null && !floodLaid && groundLaid && flood.tick(level, arena)) floodLaid = true;
    }

    /** Test and preview hook: lays his boardroom now. */
    public void layGroundNow() {
        if (!(level() instanceof ServerLevel level) || arena() == null) return;
        ArenaController arena = arena();
        groundLaid = true;
        if (inPlace) return;
        if (ground == null) ground = HorsemenGround.pin(level, arena, BoardroomGround.plan(arena.radius()));
        ground.finish(level, arena);
    }

    /** Test and preview hook: floods the boardroom now. */
    public void floodNow() {
        if (!(level() instanceof ServerLevel level) || arena() == null) return;
        layGroundNow();
        if (flood == null) flood = inPlace ? goo(openFloor(level, arena())) : floodOf(ground);
        flood.finish(level, arena());
        flooding = true;
        floodLaid = true;
    }

    /** The cells the Flood covers (empty before the last phase). */
    public List<BlockPos> floodCells() {
        return flood == null ? List.of() : flood.positions();
    }

    // --- borax and the goo --------------------------------------------------------------------------

    /**
     * Borax on him: hidden among his doubles, he is shown (they burst) and staggers; seen, it only stings (a short
     * stagger).
     */
    @Override
    public void onBorax(ServerLevel level, @Nullable Entity owner) {
        if (state() == DYING || state() == EMERGING) return;
        if (hidden()) {
            int burst = endCopy();
            exposedTicks = DickBalance.EXPOSED_TICKS;
            triggerAnim("action", "stagger");
            fx(new DickFxPayload(getId(), DickFxPayload.EXPOSED, burst, 0, position(), DickBalance.EXPOSED_TICKS));
        } else {
            exposedTicks = Math.max(exposedTicks, DickBalance.EXPOSED_TICKS / 2);
        }
        playSound(AllSounds.leviathan("borax_fizz"), 1.5f, 0.8f);
    }

    /** Whether borax has him staggering (blows land harder). */
    public boolean exposed() {
        return exposedTicks > 0;
    }

    /**
     * Borax landing at {@code at} dries the goo within {@code radius} (v0.25, {@code leviathan.Borax#dry}): through the arena of
     * the Dick Roman whose floor it is, so it all comes back when the fight ends.
     */
    public static void dryGoo(ServerLevel level, Vec3 at, double radius) {
        DickRomanEntity dick = holding(level, at);
        if (dick == null || dick.arena() == null) return;
        ArenaController arena = dick.arena();
        BlockState dried = AllBlocks.LEVIATHAN_GOO.get().defaultBlockState().setValue(LeviathanGooBlock.DRIED, true);
        int r = (int) Math.ceil(radius);
        BlockPos c = BlockPos.containing(at);
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -2, -r), c.offset(r, 2, r))) {
            if (p.distToCenterSqr(at.x, p.getY() + 0.5, at.z) > radius * radius) continue;
            BlockState s = level.getBlockState(p);
            if (s.is(AllBlocks.LEVIATHAN_GOO.get()) && !s.getValue(LeviathanGooBlock.DRIED)) arena.mutate(level, p.immutable(), dried, 0);
        }
    }

    /** The goo surges back over every dried patch. @return how many patches */
    public int surge(ServerLevel level) {
        ArenaController arena = arena();
        if (arena == null || flood == null) return 0;
        BlockState wet = AllBlocks.LEVIATHAN_GOO.get().defaultBlockState();
        int n = 0;
        for (BlockPos p : flood.positions()) {
            BlockState s = level.getBlockState(p);
            if (s.is(AllBlocks.LEVIATHAN_GOO.get()) && s.getValue(LeviathanGooBlock.DRIED) && arena.mutate(level, p, wet, 0)) n++;
        }
        return n;
    }

    /** The Dick Roman whose arena holds {@code at}, or null. */
    public static @Nullable DickRomanEntity holding(ServerLevel level, Vec3 at) {
        for (DickRomanEntity d : level.getEntitiesOfClass(DickRomanEntity.class, new AABB(at, at).inflate(48))) {
            if (d.arena() != null && d.arena().contains(BlockPos.containing(at))) return d;
        }
        return null;
    }

    // --- the copy -----------------------------------------------------------------------------------

    /** {@code ""}, or {@code player:<uuid>}: whose face he wears (drawn by the client as that hunter). */
    public String disguise() {
        return entityData.get(DISGUISE);
    }

    public boolean hidden() {
        return !disguise().isEmpty();
    }

    /**
     * He takes {@code hunter}'s face and so many doubles in the same skin come out of the goo round {@code centre}; he stands
     * in one of their places. @return the doubles
     */
    public List<LeviathanEntity> beginCopy(ServerLevel level, LivingEntity hunter, Vec3 centre) {
        endCopy();
        String face = "player:" + hunter.getUUID();
        entityData.set(DISGUISE, face);
        hiddenTicks = DickBalance.COPY_TICKS;
        int n = minionCount(DickBalance.COPY_DOUBLES);
        List<Vec3> spots = new ArrayList<>();
        double base = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i <= n; i++) {
            double a = base + i * Math.PI * 2 / (n + 1);
            spots.add(DickAttacks.floor(this, centre.add(Math.cos(a) * DickBalance.COPY_RING, 0, Math.sin(a) * DickBalance.COPY_RING)));
        }
        Collections.shuffle(spots, new java.util.Random(random.nextLong()));
        Vec3 mine = spots.removeFirst();
        teleportTo(mine.x, mine.y, mine.z);
        getNavigation().stop();
        fx(new DickFxPayload(getId(), DickFxPayload.COPY, hunter.getId(), 1, mine, 30));
        List<LeviathanEntity> out = new ArrayList<>();
        for (Vec3 s : spots) {
            LeviathanEntity d = AllEntities.LEVIATHAN.get().create(level);
            if (d == null) continue;
            d.moveTo(s.x, s.y, s.z, random.nextFloat() * 360, 0);
            d.serve(getUUID());
            d.setDisguise(face);
            var hp = d.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) hp.setBaseValue(DickBalance.DOUBLE_HEALTH);
            d.setHealth(DickBalance.DOUBLE_HEALTH);
            var dmg = d.getAttribute(Attributes.ATTACK_DAMAGE);
            if (dmg != null) dmg.setBaseValue(DickBalance.DOUBLE_DAMAGE);
            d.setTarget(hunter);
            d.setPersistenceRequired();
            level.addFreshEntity(d);
            doubles.add(d.getUUID());
            minions().add(d.getUUID());
            fx(new DickFxPayload(d.getId(), DickFxPayload.COPY, hunter.getId(), 0, s, 30));
            out.add(d);
        }
        playSound(AllSounds.dick("copy"), 2.0f, 1.0f);
        return out;
    }

    /** He drops the borrowed face; his doubles burst into goo. @return how many burst */
    public int endCopy() {
        entityData.set(DISGUISE, "");
        hiddenTicks = 0;
        int n = 0;
        if (level() instanceof ServerLevel level) {
            for (UUID id : doubles) {
                if (level.getEntity(id) instanceof LeviathanEntity d && d.isAlive()) {
                    level.sendParticles(ParticleTypes.SQUID_INK, d.getX(), d.getY() + 1, d.getZ(), 30, 0.3, 0.6, 0.3, 0.05);
                    d.discard();
                    n++;
                }
            }
        }
        doubles.clear();
        return n;
    }

    /** His staff (the Leviathans he called) still standing. */
    public int staff() {
        int n = 0;
        if (level() instanceof ServerLevel level) {
            for (UUID id : minions()) if (!doubles.contains(id) && level.getEntity(id) instanceof LeviathanEntity l && l.isAlive()) n++;
        }
        return n;
    }

    /** His doubles still standing. */
    public List<LeviathanEntity> doubles() {
        List<LeviathanEntity> out = new ArrayList<>();
        if (level() instanceof ServerLevel level) {
            for (UUID id : doubles) if (level.getEntity(id) instanceof LeviathanEntity d && d.isAlive()) out.add(d);
        }
        return out;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && reason.shouldDestroy()) endCopy();
        super.remove(reason);
    }

    // --- damage -------------------------------------------------------------------------------------

    /** Beyond Lucifer's own: hidden among his doubles he takes little; staggering from borax, more. */
    @Override
    protected float vulnerability(DamageSource source) {
        float v = super.vulnerability(source);
        if (hidden()) v *= DickBalance.HIDDEN_HARM;
        if (exposed()) v *= DickBalance.EXPOSED_HARM;
        return v;
    }

    // --- hunters and effects ------------------------------------------------------------------------

    /** The hunters his attacks see: the challengers, and anyone tracked (tests). */
    public List<LivingEntity> hunters() {
        List<LivingEntity> out = new ArrayList<>(challengers());
        tracked.removeIf(e -> !e.isAlive() || e.isRemoved());
        for (LivingEntity e : tracked) if (!out.contains(e)) out.add(e);
        return out;
    }

    /** Test hook: {@code e} counts as one of his hunters. */
    public void track(LivingEntity e) {
        if (!tracked.contains(e)) tracked.add(e);
    }

    /** Sends one of the fight's moments to every hunter near. */
    public void fx(DickFxPayload payload) {
        if (level() instanceof ServerLevel level) DickFxPayload.send(level, position(), payload);
    }

    // --- attacks ------------------------------------------------------------------------------------

    /** Tests and previews: the next attack he uses, whatever the pool says. */
    public void queue(Supplier<BossAttack<LuciferEntity>> attack) {
        queued = attack;
    }

    /** A queued attack first; the contract when the hunter keeps away or hides. */
    @Override
    public @Nullable Supplier<BossAttack<LuciferEntity>> forcedAttack(LivingEntity target) {
        if (queued != null) {
            Supplier<BossAttack<LuciferEntity>> q = queued;
            queued = null;
            return q;
        }
        if (distanceToSqr(target) > 16 * 16 || noSightTicks > 60) {
            noSightTicks = 0;
            return phase() >= MAX_PHASE ? DickAttacks.Tentacles::new : DickAttacks.SignHere::new;
        }
        return null;
    }

    // --- GeckoLib -----------------------------------------------------------------------------------

    /** Only his own clips: Lucifer's names that he lacks are dropped. */
    @Override
    public void triggerAnim(@Nullable String controller, String anim) {
        if ("action".equals(controller) && !DickAssets.TRIGGERED.contains(anim)) return;
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
    static final List<String> HOLDS = List.of("death");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = animationPrefix();
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + "idle"), walk = RawAnimation.begin().thenLoop(pre + "walk"),
                mawIdle = RawAnimation.begin().thenLoop(pre + "maw_idle");
        controllers.add(new AnimationController<>(this, "base", 6, state -> {
            if (state() == DYING) return PlayState.STOP;
            return state.setAndContinue(state.isMoving() ? walk : lookPhase() >= 2 ? mawIdle : idle);
        }));
        AnimationController<DickRomanEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String name : DickAssets.TRIGGERED) {
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
        tag.putBoolean("GroundLaid", groundLaid);
        tag.putBoolean("Flooding", flooding);
        tag.putBoolean("FloodLaid", floodLaid);
        tag.putBoolean("InPlace", inPlace);
        tag.putInt("HiddenTicks", hiddenTicks);
        tag.putString("Disguise", disguise());
        ListTag list = new ListTag();
        for (UUID id : doubles) list.add(NbtUtils.createUUID(id));
        tag.put("Doubles", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        emerged = tag.getBoolean("Emerged");
        groundLaid = tag.getBoolean("GroundLaid");
        flooding = tag.getBoolean("Flooding");
        floodLaid = tag.getBoolean("FloodLaid");
        inPlace = tag.getBoolean("InPlace");
        hiddenTicks = tag.getInt("HiddenTicks");
        entityData.set(DISGUISE, tag.getString("Disguise"));
        doubles.clear();
        for (Tag t : tag.getList("Doubles", Tag.TAG_INT_ARRAY)) doubles.add(NbtUtils.loadUUID(t));
    }
}
