package org.papiricoh.supernaturalcraft.entity.boss.metatron;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTerrain;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Metatron, the Scribe of God. Four phases, a quarter of his true health each (above the vanilla cap,
 * like Lucifer Uncaged's):
 *
 * <pre>EMERGING (the library rises) → P1 The Scribe (blade in hand) → P2 The Hand of God (the quill-hand joins him)
 *   → P3 The Library of Heaven (he takes to his lectern on its dais, and calls the Book) → P4 The Angel Tablet → DYING</pre>
 *
 * <p>His Hand and Book are {@link ScribeConstruct}s: untouchable, they drift at his side and his attacks
 * order them about. From the third phase he never leaves the lectern: climb the stairs or strike from
 * afar. With the Tablet he brings down the Fall, speaks the Word, and rewrites the ground.
 */
public class MetatronEntity extends LuciferEntity {

    public static final int MAX_PHASE = MetatronBalance.PHASES, METATRON_EMERGE_TICKS = 100, METATRON_DEATH_TICKS = 140;
    private static final int LIBRARY_AT = 40;
    public static final int BOOK_TRAP_TICKS = 60;
    public static final float BOOK_TRAP_RELEASE = 25f;

    private float healthScale = 1f;
    private boolean libraryRaised, daisRaised;
    private @Nullable UUID handId, bookId;
    private @Nullable Vec3 riseFrom;
    // The Book shut on a hunter.
    private @Nullable UUID trapped;
    private long trapUntil;
    private float trapPaid;
    // The Tablet.
    private int attacksSinceWord;
    private long nextRewrite;
    private @Nullable WordJudge.Order lastWord;
    private final Map<UUID, EnumSet<WordJudge.Order>> obeyed = new HashMap<>();

    public MetatronEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 1000;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MetatronBalance.BASE_HEALTH)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    // --- his numbers --------------------------------------------------------------------------------

    @Override
    public int maxPhase() {
        return MAX_PHASE;
    }

    @Override
    protected float threshold(int phase) {
        return MetatronBalance.threshold(phase);
    }

    @Override
    protected float healthScale() {
        return healthScale;
    }

    /** Test hook. */
    public void setHealthScale(float scale) {
        healthScale = scale;
    }

    @Override
    protected float mundaneMultiplier() {
        return SNConfig.METATRON_MUNDANE_MULTIPLIER.get().floatValue();
    }

    @Override
    protected float hitCap() {
        return SNConfig.METATRON_HIT_CAP.get().floatValue();
    }

    @Override
    public float attackDamageMultiplier() {
        return SNConfig.METATRON_DAMAGE_MULTIPLIER.get().floatValue();
    }

    @Override
    public boolean isAerialPhase() {
        return false;
    }

    @Override
    protected void scaleHealthToChallengers() {
        healthScale = MetatronBalance.healthScale(SNConfig.METATRON_HEALTH_MULTIPLIER.get(), SNConfig.METATRON_HEALTH_PER_PLAYER.get(),
                challengers().size());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(MetatronBalance.BASE_HEALTH);
        setHealth((float) MetatronBalance.BASE_HEALTH);
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return MetatronAttacks.pool(phase);
    }

    @Override
    protected int baseGap(int phase) {
        return MetatronBalance.attackGap(phase);
    }

    @Override
    public float scale(int phase) {
        return MetatronBalance.scale(phase);
    }

    @Override
    protected int emergeTicks() {
        return METATRON_EMERGE_TICKS;
    }

    @Override
    protected int deathTicks() {
        return METATRON_DEATH_TICKS;
    }

    @Override
    protected int transitionTicks(int to) {
        return TRANSITION_TICKS;
    }

    @Override
    protected String animationPrefix() {
        return "animation.metatron.";
    }

    @Override
    protected List<String> triggeredAnimations() {
        return MetatronAnimations.TRIGGERED;
    }

    @Override
    protected String bossBarKey(int phase) {
        return "entity.supernaturalcraft.metatron.phase" + phase;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return phase >= 4 ? BossEvent.BossBarColor.WHITE : BossEvent.BossBarColor.YELLOW;
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return phase >= 3 ? AllParticles.PAGE.get() : AllParticles.GRACE.get();
    }

    @Override
    protected SoundEvent emergeSound() {
        return AllSounds.METATRON_RISE.get();
    }

    @Override
    protected SoundEvent roarSound() {
        return AllSounds.METATRON_WORD.get();
    }

    @Override
    protected SoundEvent transformSound() {
        return AllSounds.METATRON_RISE.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AllSounds.METATRON_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.METATRON_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.METATRON_DEATH.get();
    }

    @Override
    protected boolean walks() {
        return !MetatronBalance.onLectern(phase());
    }

    // --- the fight's shape --------------------------------------------------------------------------

    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        return LuciferSummoning.openArena(level, blockPosition(), SNConfig.METATRON_ARENA_RADIUS.get(), ArenaTheme.SCRIPTORIUM);
    }

    @Override
    protected void applyTerrain(ServerLevel level, ArenaController arena, int phase) {
        if (phase >= 4) nextRewrite = level.getGameTime() + 200;
    }

    @Override
    protected void playEmergence() {
        MetatronCinematics.emergence(this);
    }

    @Override
    protected void playTransition(int to) {
        MetatronCinematics.transition(this, to);
    }

    @Override
    protected void playDeath() {
        MetatronCinematics.death(this);
        releaseTrap();
        if (hand() != null) hand().vanish();
        if (book() != null) book().vanish();
    }

    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        MetatronCinematics.victory(this);
    }

    @Override
    protected void onTransitionStart(int to) {
        releaseTrap();
        if (!(level() instanceof ServerLevel level)) return;
        level.sendParticles(AllParticles.PAGE.get(), getX(), getY() + 1.4, getZ(), 40, 0.6, 0.8, 0.6, 0.1);
        ArenaController arena = arena();
        if (to >= MetatronBalance.LECTERN_PHASE && arena != null && !daisRaised) {
            raiseDais(level, arena);
            riseFrom = position();
        }
    }

    /** Into the third phase he floats up onto his dais; otherwise he stands where he is. */
    @Override
    protected void tickTransitionMotion(int elapsed, boolean last) {
        ArenaController arena = arena();
        if (phase() == MetatronBalance.LECTERN_PHASE && riseFrom != null && arena != null) {
            setNoGravity(true);
            double t = Math.min(1, elapsed / (double) (TRANSITION_TICKS - 10));
            double eased = t * t * (3 - 2 * t);
            Vec3 at = riseFrom.lerp(lecternSpot(arena), eased).add(0, Math.sin(t * Math.PI) * 2.5, 0);
            setPos(at.x, at.y, at.z);
            setDeltaMovement(Vec3.ZERO);
            if (level() instanceof ServerLevel level && elapsed % 3 == 0) {
                level.sendParticles(AllParticles.PAGE.get(), getX(), getY() + 0.3, getZ(), 4, 0.4, 0.2, 0.4, 0.02);
            }
            return;
        }
        setDeltaMovement(0, getDeltaMovement().y, 0);
    }

    /** Pages gathering into a man; halfway through, the library rises around the circle. */
    @Override
    protected void tickEmergence() {
        setDeltaMovement(Vec3.ZERO);
        int elapsed = METATRON_EMERGE_TICKS - stateTimer;
        if (!(level() instanceof ServerLevel level)) return;
        if (elapsed < 40 && elapsed % 3 == 0) {
            level.sendParticles(AllParticles.PAGE.get(), getX(), getY() + 1, getZ(), 6, 1.2, 1.0, 1.2, 0.04);
        }
        ArenaController arena = arena();
        if (elapsed == LIBRARY_AT && arena != null) raiseLibrary(level, arena);
    }

    @Override
    protected void clientEmergenceParticles() {
        level().addParticle(AllParticles.PAGE.get(), getRandomX(1.6), getY() + random.nextDouble() * 2.2, getRandomZ(1.6), 0, 0.02, 0);
    }

    @Override
    protected void tickDyingMotion(int elapsed) {
        setNoGravity(false);
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
    }

    @Override
    protected void dyingParticles(ServerLevel level, boolean last) {
        if (last) {
            level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1.2, getZ(), 2, 0, 0, 0, 0);
            level.sendParticles(AllParticles.PAGE.get(), getX(), getY() + 1, getZ(), 160, 1.5, 1.5, 1.5, 0.3);
            level.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 1, getZ(), 80, 1.0, 1.2, 1.0, 0.2);
            return;
        }
        level.sendParticles(AllParticles.PAGE.get(), getX(), getY() + 1.4, getZ(), 4, 0.4, 0.6, 0.4, 0.05);
    }

    @Override
    protected Vec3 tetherPoint(ArenaController arena) {
        if (MetatronBalance.onLectern(phase()) && daisRaised) return lecternSpot(arena);
        return super.tetherPoint(arena);
    }

    @Override
    protected void leaveBehind(ServerLevel level, Vec3 at) {
        level.sendParticles(AllParticles.PAGE.get(), getX(), getY() + 1, getZ(), 80, 0.8, 1.0, 0.8, 0.1);
        releaseTrap();
    }

    @Override
    protected void returnToCage(ServerLevel level, String messageKey) {
        super.returnToCage(level, messageKey.replace(".lucifer.", ".metatron."));
    }

    // --- the library ---------------------------------------------------------------------------------

    private void raiseLibrary(ServerLevel level, ArenaController arena) {
        libraryRaised = true;
        BlockPos c = arena.center();
        for (ScriptoriumLayout.Place p : ScriptoriumLayout.library()) {
            BlockPos floor = ArenaTerrain.surface(level, arena, c.getX() + p.dx(), c.getZ() + p.dz());
            if (floor == null) continue;
            BlockPos at = floor.above(1 + p.dy());
            BlockState state = switch (p.kind()) {
                case CHISELED -> Blocks.CHISELED_BOOKSHELF.defaultBlockState();
                case LECTERN -> Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.getNearest(-p.dx(), 0, -p.dz()));
                default -> Blocks.BOOKSHELF.defaultBlockState();
            };
            arena.mutate(level, at, state, 0);
        }
        level.playSound(null, c, AllSounds.SCRIBE_BOOK_PAGES.get(), SoundSource.HOSTILE, 3f, 0.6f);
    }

    /** The dais and its two flights of stairs, at the arena's centre. */
    public void raiseDais(ServerLevel level, ArenaController arena) {
        daisRaised = true;
        BlockPos c = arena.center();
        for (ScriptoriumLayout.Place p : ScriptoriumLayout.dais()) {
            BlockState state = switch (p.kind()) {
                case STAIR -> Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, p.north() ? Direction.SOUTH : Direction.NORTH);
                default -> AllBlocks.SCRIPTURE_STONE.get().defaultBlockState();
            };
            arena.mutate(level, c.offset(p.dx(), p.dy(), p.dz()), state, 0);
        }
        arena.mutate(level, c.offset(0, ScriptoriumLayout.lecternDy(), 1),
                Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.NORTH), 0);
        level.sendParticles(AllParticles.GRACE.get(), c.getX() + 0.5, c.getY() + 2, c.getZ() + 0.5, 80, 2.5, 1.2, 2.5, 0.05);
    }

    /** Where he stands once he has taken to the lectern. */
    public Vec3 lecternSpot(ArenaController arena) {
        return Vec3.atBottomCenterOf(arena.center()).add(0, ScriptoriumLayout.lecternDy(), 0);
    }

    public boolean libraryRaised() {
        return libraryRaised;
    }

    public boolean daisRaised() {
        return daisRaised;
    }

    // --- the constructs ------------------------------------------------------------------------------

    public @Nullable ScribeHandEntity hand() {
        return level() instanceof ServerLevel s && handId != null && s.getEntity(handId) instanceof ScribeHandEntity h && h.isAlive() ? h : null;
    }

    public @Nullable ScribeBookEntity book() {
        return level() instanceof ServerLevel s && bookId != null && s.getEntity(bookId) instanceof ScribeBookEntity b && b.isAlive() ? b : null;
    }

    /** Calls up whichever constructs this phase should have and doesn't. */
    public void ensureConstructs(ServerLevel level) {
        int phase = phase();
        if (MetatronBalance.hasHand(phase) && hand() == null) {
            ScribeHandEntity h = AllEntities.SCRIBE_HAND.get().create(level);
            if (h != null) {
                h.bind(this);
                Vec3 at = position().add(0, 6, 0);
                h.moveTo(at.x, at.y, at.z, getYRot(), 0);
                level.addFreshEntity(h);
                handId = h.getUUID();
                level.sendParticles(AllParticles.GRACE.get(), at.x, at.y, at.z, 60, 1.2, 1.2, 1.2, 0.1);
            }
        }
        if (MetatronBalance.hasBook(phase) && book() == null) {
            ScribeBookEntity b = AllEntities.SCRIBE_BOOK.get().create(level);
            if (b != null) {
                b.bind(this);
                Vec3 at = position().add(0, 7, 0);
                b.moveTo(at.x, at.y, at.z, getYRot(), 0);
                level.addFreshEntity(b);
                bookId = b.getUUID();
                level.sendParticles(AllParticles.PAGE.get(), at.x, at.y, at.z, 60, 1.2, 1.2, 1.2, 0.1);
            }
        }
    }

    /** Where a free construct drifts: the hand at his right shoulder, the book at his left, both above him. */
    public Vec3 constructRest(ScribeConstruct c) {
        float yaw = yBodyRot * ((float) Math.PI / 180f);
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        double side = c instanceof ScribeHandEntity ? (MetatronBalance.onLectern(phase()) ? 6.0 : 5.0) : (MetatronBalance.onLectern(phase()) ? 4.5 : 3.5);
        double s = c instanceof ScribeHandEntity ? 1 : -1;
        return position().add(right.scale(side * s)).add(0, c instanceof ScribeHandEntity ? 3.5 : 3.5, 0);
    }

    // --- the book's trap -----------------------------------------------------------------------------

    /** The Book shuts with {@code hunter} inside. Wounding Metatron enough opens it again. */
    public void trapInBook(ServerPlayer hunter) {
        if (!(level() instanceof ServerLevel level)) return;
        trapped = hunter.getUUID();
        trapUntil = level.getGameTime() + BOOK_TRAP_TICKS;
        trapPaid = 0;
        hunter.addEffect(new MobEffectInstance(AllMobEffects.STUNNED, BOOK_TRAP_TICKS, 0, false, true, true), this);
        hunter.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, BOOK_TRAP_TICKS, 0, false, false, false), this);
    }

    public @Nullable UUID trapped() {
        return trapped;
    }

    public void releaseTrap() {
        if (trapped != null && level() instanceof ServerLevel level && level.getPlayerByUUID(trapped) instanceof Player p) {
            p.removeEffect(AllMobEffects.STUNNED);
            p.removeEffect(net.minecraft.world.effect.MobEffects.DARKNESS);
        }
        trapped = null;
        ScribeBookEntity b = book();
        if (b != null) {
            b.triggerAnim("action", "open");
            b.release();
        }
    }

    private void tickTrap(ServerLevel level) {
        if (trapped == null) return;
        Player p = level.getPlayerByUUID(trapped);
        if (p == null || !p.isAlive() || level.getGameTime() >= trapUntil) {
            releaseTrap();
            return;
        }
        ScribeBookEntity b = book();
        if (b != null) b.order(p.position().add(0, 0.2, 0), 1);
        if (level.getGameTime() % 20 == 0) LuciferAttacks.hit(this, p, AllDamageTypes.SPELL, 3);
    }

    // --- ticking -------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        // Spawned by egg or command: his health scales on his first tick in the fight.
        if (healthScale <= 1f && state() != EMERGING) scaleHealthToChallengers();
        super.customServerAiStep();
        if (isRemoved() || !(level() instanceof ServerLevel level)) return;
        ArenaController arena = arena();
        if (arena == null || !arena.isActive()) return;
        if (!libraryRaised && state() != EMERGING) raiseLibrary(level, arena);
        byte s = state();
        if (s == DYING) return;
        if (MetatronBalance.onLectern(phase()) && s != TRANSITION) {
            if (!daisRaised) raiseDais(level, arena);
            Vec3 spot = lecternSpot(arena);
            setNoGravity(true);
            getNavigation().stop();
            setDeltaMovement(Vec3.ZERO);
            if (position().distanceToSqr(spot) > 0.01) setPos(spot.x, spot.y, spot.z);
        }
        if (s != EMERGING && s != TRANSITION && tickCount % 10 == 0) ensureConstructs(level);
        tickTrap(level);
        if (phase() >= 4 && s != TRANSITION && level.getGameTime() >= nextRewrite) {
            MetatronTerrain.rewrite(level, arena, random);
            nextRewrite = level.getGameTime() + MetatronBalance.REWRITE_EVERY;
        }
    }

    // --- damage --------------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        float before = trueHealth();
        boolean hurt = super.hurt(source, amount);
        if (hurt && trapped != null && source.getEntity() instanceof Player) {
            trapPaid += before - trueHealth();
            if (trapPaid >= BOOK_TRAP_RELEASE) releaseTrap();
        }
        return hurt;
    }

    // --- attacks -------------------------------------------------------------------------------------

    @Override
    public @Nullable Supplier<BossAttack<LuciferEntity>> forcedAttack(LivingEntity target) {
        if (phase() >= 4 && attacksSinceWord >= MetatronBalance.WORD_EVERY) {
            attacksSinceWord = 0;
            return MetatronAttacks.TheWord::new;
        }
        if (walks() && (distanceToSqr(target) > 18 * 18 || noSightTicks > 100)) {
            noSightTicks = 0;
            return LuciferAttacks.Teleport::new;
        }
        return null;
    }

    @Override
    public void onStage(AttackScheduler.Stage stage, @Nullable BossAttack<LuciferEntity> attack) {
        super.onStage(stage, attack);
        if (stage == AttackScheduler.Stage.IDLE && attack == null) attacksSinceWord++;
    }

    /** Test hook: the next attack will be the Word. */
    public void forceWordNext() {
        attacksSinceWord = MetatronBalance.WORD_EVERY;
    }

    public @Nullable WordJudge.Order lastWord() {
        return lastWord;
    }

    public void setLastWord(WordJudge.Order order) {
        lastWord = order;
    }

    /** {@code hunter} kept the Word {@code order}: once they have kept all three, they earn "Obeyed". */
    public void kept(ServerPlayer hunter, WordJudge.Order order) {
        EnumSet<WordJudge.Order> set = obeyed.computeIfAbsent(hunter.getUUID(), k -> EnumSet.noneOf(WordJudge.Order.class));
        if (set.add(order) && set.size() == WordJudge.Order.values().length) {
            org.papiricoh.supernaturalcraft.reward.ChorusRewards.award(hunter, "main/obeyed");
        }
    }

    // --- persistence ---------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("HealthScale", healthScale);
        tag.putBoolean("Library", libraryRaised);
        tag.putBoolean("Dais", daisRaised);
        if (handId != null) tag.putUUID("Hand", handId);
        if (bookId != null) tag.putUUID("Book", bookId);
        tag.putInt("SinceWord", attacksSinceWord);
        tag.putLong("NextRewrite", nextRewrite);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        healthScale = tag.contains("HealthScale") ? tag.getFloat("HealthScale") : 1f;
        super.readAdditionalSaveData(tag);
        libraryRaised = tag.getBoolean("Library");
        daisRaised = tag.getBoolean("Dais");
        handId = tag.hasUUID("Hand") ? tag.getUUID("Hand") : null;
        bookId = tag.hasUUID("Book") ? tag.getUUID("Book") : null;
        attacksSinceWord = tag.getInt("SinceWord");
        nextRewrite = tag.getLong("NextRewrite");
    }

    /** Everything within {@code r} of {@code at} that could be hurt, for tests and attacks. */
    public List<LivingEntity> foesNear(Vec3 at, double r) {
        return level().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r), e -> e != this && e.isAlive());
    }
}
