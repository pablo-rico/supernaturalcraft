package org.papiricoh.supernaturalcraft.entity.boss.zachariah;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.PositionTrail;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.HorsemenGround;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.arena.ZachariahOfficeLayout;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;
import org.papiricoh.supernaturalcraft.network.HeavenFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.GeckoLibServices;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Zachariah, the angel of Heaven's paperwork (v0.18): an optional boss in his endless office above a hunter's own Heaven. Four
 * phases, a quarter of his true health each (50 000 alone on the power curve):
 *
 * <pre>EMERGING → I Intake → II Review (the cubicles shuffle) → III It Was Already Written (his six wings shown; the docket)
 *   → IV Final Judgment (the office dissolves into golden sky) → DYING</pre>
 *
 * <p><b>Paperwork</b> ({@link FormRules}): at each phase's start and every {@code zachariah.formTicks} every hunter is issued a
 * Heavenly Form for one of the four filing cabinets. While it is unfiled their blows land at a quarter; filed in its cabinet
 * (or with a clerk's approval stamp) they are Approved for a while and hit harder; overdue, Heaven collects ({@link
 * ZachariahAttacks.OverdueSmite}). <b>The docket</b> ({@link Docket}): from phase III he announces his next attacks and keeps to
 * them, but for one revision a phase. <b>The wrap</b> ({@link OfficeWrap}): the office repeats, and walking out of it walks you
 * back in.
 *
 * <p><b>The office.</b> In a hunter's Heaven it is already written (permanent, {@link ZachariahSummoning#summonInOffice}); spawned
 * by egg or command, {@link ZachariahOfficeLayout#plan()} is written round him through the arena (and given back). Either way the
 * four filing cabinets stand at {@link ZachariahOfficeLayout#CABINETS}: if the plan does not hold a numbered
 * {@code supernaturalcraft:filing_cabinet} there, the fight puts one there through the arena as it starts. Memories of the owner
 * hang as text at {@link ZachariahOfficeLayout#FRAMES} (display entities he spawns and takes with him; never block entities).
 */
public class ZachariahEntity extends LuciferEntity {

    public static final int MAX_PHASE = ZachariahBalance.PHASES;

    /** His six wings are shown (phase III on, his death): the renderer shows the {@code wings} bone. */
    private static final EntityDataAccessor<Boolean> WINGS = SynchedEntityData.defineId(ZachariahEntity.class, EntityDataSerializers.BOOLEAN);

    /** What a hunter was issued: the cabinet and when. */
    public record Issued(int number, long issued) {
    }

    /** What filing a form came to. */
    public enum Filing { FILED, WRONG_CABINET, NOTHING_TO_FILE }

    // the office
    private @Nullable UUID owner;
    private boolean permanentOffice, emerged, cabinetsPlaced, decorPlaced;
    private @Nullable HorsemenGround writing;
    private boolean officeBegun, dissolved, docketPending;
    private int shuffleStep;
    private final List<UUID> decor = new ArrayList<>();
    // the paperwork
    private final Map<UUID, Issued> forms = new HashMap<>();
    private final Map<UUID, Integer> lastNumber = new HashMap<>();
    private final Map<UUID, Long> approvedUntil = new HashMap<>();
    private final Deque<UUID> overdue = new ArrayDeque<>();
    private long nextIssue = -1;
    // the docket
    private final Docket docket = new Docket();
    private String lastForetold = "";
    private @Nullable LivingEntity noticeOn;
    // the precedent
    private final Map<UUID, PositionTrail> trails = new HashMap<>();
    private long staggeredUntil;
    /** Tests: hunters the office sees beyond the challengers (fake players are not in the level's list). */
    private final List<LivingEntity> tracked = new ArrayList<>();
    /** Tests: an attack to use next, whatever the pool says. */
    private @Nullable Supplier<BossAttack<LuciferEntity>> queued;

    public ZachariahEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 900;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, VANILLA_BASE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WINGS, false);
    }

    /** Whether his six wings are shown (synced). */
    public boolean wingsShown() {
        return entityData.get(WINGS);
    }

    // --- his numbers --------------------------------------------------------------------------------

    @Override
    public int maxPhase() {
        return MAX_PHASE;
    }

    @Override
    protected float threshold(int phase) {
        return ZachariahBalance.threshold(phase);
    }

    @Override
    protected double healthPerExtraPlayer() {
        return SNConfig.ZACHARIAH_HEALTH_PER_PLAYER.get();
    }

    @Override
    protected float mundaneMultiplier() {
        return SNConfig.ZACHARIAH_MUNDANE_MULTIPLIER.get().floatValue();
    }

    @Override
    protected float damageFactor() {
        return SNConfig.ZACHARIAH_DAMAGE_FACTOR.get().floatValue();
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
        return ZachariahAttacks.pool(phase);
    }

    @Override
    protected int baseGap(int phase) {
        return ZachariahBalance.attackGap(phase);
    }

    @Override
    protected String animationPrefix() {
        return ZachariahAnimations.PREFIX;
    }

    @Override
    protected List<String> triggeredAnimations() {
        return ZachariahAnimations.TRIGGERED;
    }

    @Override
    protected String bossBarKey(int phase) {
        return "entity.supernaturalcraft.zachariah";
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return phase >= MAX_PHASE ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.WHITE;
    }

    @Override
    protected Component bossBarName(int phase) {
        return Component.translatable(bossBarKey(phase)).withStyle(phase == maxPhase() ? ChatFormatting.GOLD : ChatFormatting.WHITE);
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return ParticleTypes.END_ROD;
    }

    private static SoundEvent sound(String id) {
        return AllSounds.heaven("zachariah." + id);
    }

    @Override
    protected SoundEvent emergeSound() {
        return sound("wings");
    }

    @Override
    protected SoundEvent roarSound() {
        return sound("approved");
    }

    @Override
    protected SoundEvent transformSound() {
        return sound("wings");
    }

    @Override
    protected SoundEvent ambientBossSound() {
        return sound("ambient");
    }

    @Override
    protected SoundEvent dyingSound() {
        return sound("death");
    }

    @Override
    protected SoundEvent deflectSound() {
        return sound("denied");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return sound("hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return sound("death");
    }

    @Override
    protected void clientEmergenceParticles() {
        level().addParticle(ParticleTypes.END_ROD, getRandomX(1.0), getY() + random.nextDouble() * 2.0, getRandomZ(1.0), 0, 0.04, 0);
        level().addParticle(ParticleTypes.WHITE_ASH, getRandomX(1.5), getY() + 1 + random.nextDouble(), getRandomZ(1.5), 0, -0.02, 0);
    }

    @Override
    protected void dyingParticles(ServerLevel level, boolean last) {
        if (last) {
            level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1.2, getZ(), 3, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.2, getZ(), 160, 1.5, 1.8, 1.5, 0.3);
            return;
        }
        level.sendParticles(ParticleTypes.WHITE_ASH, getX(), getY() + 1.4, getZ(), 6, 0.6, 0.8, 0.6, 0.02);
        level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.6, getZ(), 2, 0.2, 0.3, 0.2, 0.05);
    }

    /** He doesn't walk while he reels or while his office is still being written. */
    @Override
    protected boolean walks() {
        return !isStaggered() && !writingOffice();
    }

    // --- the fight's shape --------------------------------------------------------------------------

    /** Spawned by egg or command: an office round wherever he stands (written through the arena, given back after). */
    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        return LuciferSummoning.openArena(level, blockPosition(), ZachariahBalance.ARENA_RADIUS, ArenaTheme.OFFICE, false);
    }

    /** Called by {@link ZachariahSummoning#summonInOffice}: the office is already there (a hunter's Heaven), and whose it is. */
    public void inOffice(@Nullable UUID owner) {
        this.owner = owner;
        this.permanentOffice = true;
    }

    /** The hunter whose Heaven this office is above (null for an egg or a command). */
    public @Nullable UUID owner() {
        return owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
    }

    @Override
    protected Vec3 tetherPoint(ArenaController arena) {
        return at(ZachariahOfficeLayout.DAIS);
    }

    @Override
    protected void playEmergence() {
        ZachariahCinematics.intro(this);
    }

    @Override
    protected void playTransition(int to) {
        ZachariahCinematics.transition(this, to);
    }

    @Override
    protected void playDeath() {
        ZachariahCinematics.death(this);
    }

    /** A phase changes: a fresh form for everyone; his wings for the third; the docket from the third. */
    @Override
    protected void onTransitionStart(int to) {
        // Not Lucifer's (no shield, no flight).
        if (!(level() instanceof ServerLevel)) return;
        staggeredUntil = 0;
        noticeOn = null;
        if (to >= 3 && !wingsShown()) {
            entityData.set(WINGS, true);
            triggerAnim("action", ZachariahAnimations.WINGS_REVEAL);
        }
        openDocket(to);
        nextIssue = level().getGameTime() + transitionTicks(to);
    }

    @Override
    public void forceLook(int phase) {
        super.forceLook(phase);
        entityData.set(WINGS, phase >= 3);
        openDocket(phase);
        if (phase >= MAX_PHASE && level() instanceof ServerLevel level && arena() != null) dissolveOffice(level, arena(), true);
    }

    @Override
    protected void tickTransitionMotion(int elapsed, boolean last) {
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        getNavigation().stop();
    }

    @Override
    protected int deathTicks() {
        return ZachariahBalance.DEATH_TICKS;
    }

    @Override
    protected void tickDyingMotion(int elapsed) {
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        if (elapsed == ZachariahBalance.DEATH_BURST && level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1.2, getZ(), 4, 0.5, 0.5, 0.5, 0);
            playSound(sound("wings"), 4f, 0.6f);
        }
    }

    /** Halfway through a change of phase: the last dissolves the office into sky. */
    @Override
    protected void applyTerrain(ServerLevel level, ArenaController arena, int phase) {
        if (phase >= MAX_PHASE) dissolveOffice(level, arena, false);
    }

    /** The fight was lost: nothing is left but a form nobody will file. */
    @Override
    protected void leaveBehind(ServerLevel level, Vec3 at) {
    }

    @Override
    protected void returnToCage(ServerLevel level, String messageKey) {
        closeTheBooks(level);
        super.returnToCage(level, messageKey.replace(".lucifer.", ".zachariah."));
    }

    @Override
    protected void beginDying() {
        entityData.set(WINGS, true);
        noticeOn = null;
        if (level() instanceof ServerLevel level) {
            dismissClerks(level);
            fx(HeavenFxPayload.of(getId(), HeavenFxPayload.FORETOLD, 0, 0, 0));
        }
        docket.open(0);
        super.beginDying();
    }

    /** At the end of his death: the victory, his spoils per hunter, and the owner's home is theirs. */
    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        ZachariahCinematics.victory(this);
        List<ServerPlayer> hunters = challengers();
        ZachariahSpoils.drop(level, hunters, position().add(0, 1.2, 0), random);
        ZachariahSpoils.unlockHomes(level, owner, hunters);
        closeTheBooks(level);
    }

    /** The fight is over: every form crumbles, the décor goes, the HUDs clear. */
    private void closeTheBooks(ServerLevel level) {
        for (LivingEntity h : hunters()) {
            if (h instanceof Player p) removeForms(p);
            if (h instanceof ServerPlayer p) fxTo(p, HeavenFxPayload.of(getId(), HeavenFxPayload.FORM, 0, 0, 0));
        }
        forms.clear();
        overdue.clear();
        approvedUntil.clear();
        discardDecor(level);
    }

    // --- ticking ------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (isRemoved() || !(level() instanceof ServerLevel level)) return;
        ArenaController arena = arena();
        if (arena == null || !arena.isActive()) return;
        long now = level.getGameTime();
        boolean ready = tickOffice(level, arena);
        byte s = state();
        if (s == DYING) return;
        if (ready && !cabinetsPlaced) placeCabinets(level, arena);
        if (ready && !decorPlaced) placeDecor(level);
        OfficeWrap.tick(this, level);
        if (now % ZachariahBalance.TRAIL_EVERY == 0) recordTrails(now);
        if (s == EMERGING) return;
        if (!emerged) {
            emerged = true;
            if (phase() < 3) entityData.set(WINGS, false);
            fx(HeavenFxPayload.of(getId(), HeavenFxPayload.ZACHARIAH_TITLE, phase(), 0, 70));
            openDocket(phase());
            if (nextIssue < 0) nextIssue = now + 20;
        }
        if (docketPending) {
            docketPending = false;
            openDocket(phase());
        }
        if (s == TRANSITION) return;
        if (!ready) return;
        tickPaperwork(level, now);
        if (docket.tick(now)) announce();
        if (isStaggered()) {
            getNavigation().stop();
            if (scheduler().current() == null) scheduler().delay(5);
        }
    }

    // --- the office ---------------------------------------------------------------------------------

    /** The office's origin: the arena's centre (the first air above its floor). */
    public BlockPos officeOrigin() {
        ArenaController arena = arena();
        return arena != null ? arena.center() : blockPosition();
    }

    public Vec3 officeCentre() {
        return Vec3.atBottomCenterOf(officeOrigin());
    }

    /** Where a fixed point of the office is (feet). */
    public Vec3 at(LayoutPoint p) {
        return ZachariahAttacks.at(officeOrigin(), p);
    }

    /** {@code p} on the office's floor (its main level: the office is flat). */
    public Vec3 onFloor(Vec3 p) {
        return new Vec3(p.x, arena() != null ? officeOrigin().getY() : p.y, p.z);
    }

    /** Whether the office stands (written, or always there): the fight may go on. */
    public boolean officeReady() {
        return permanentOffice || (officeBegun && !writingOffice());
    }

    /** Whether the office (or its dissolving) is still being written: the fight waits for it. */
    public boolean writingOffice() {
        return writing != null && !writing.done();
    }

    private boolean tickOffice(ServerLevel level, ArenaController arena) {
        if (!permanentOffice && !officeBegun) {
            officeBegun = true;
            writing = fixed(officeOrigin(), ZachariahOfficeLayout.plan().cells());
        }
        if (!writingOffice()) return officeReady();
        boolean done = writing.tick(level, arena);
        if (done) {
            for (LivingEntity h : hunters()) unstick(level, h);
            unstick(level, this);
            return officeReady();
        }
        byte s = state();
        if (s != EMERGING && s != TRANSITION && s != DYING) {
            if (scheduler().current() == null) scheduler().delay(10);
            getNavigation().stop();
        }
        return officeReady() && !writingOffice();
    }

    /** Test and preview hook: the office written now (and its cabinets placed). */
    public void buildOfficeNow() {
        if (!(level() instanceof ServerLevel level) || arena() == null) return;
        ArenaController arena = arena();
        if (!permanentOffice && !officeBegun) {
            officeBegun = true;
            writing = fixed(officeOrigin(), ZachariahOfficeLayout.plan().cells());
        }
        if (writing != null) writing.finish(level, arena);
        if (!cabinetsPlaced) placeCabinets(level, arena);
    }

    private static HorsemenGround fixed(BlockPos origin, List<ArenaCell> cells) {
        List<BlockPos> positions = new ArrayList<>(cells.size());
        List<String> blocks = new ArrayList<>(cells.size());
        for (ArenaCell c : cells) {
            positions.add(origin.offset(c.dx(), c.dy(), c.dz()));
            blocks.add(c.block());
        }
        return HorsemenGround.fixed(positions, blocks);
    }

    /** Lifts {@code e} out of any block the office was written into. */
    private static void unstick(ServerLevel level, Entity e) {
        if (level.noCollision(e, e.getBoundingBox())) return;
        for (int up = 1; up <= ZachariahOfficeLayout.CEILING + 2; up++) {
            if (level.noCollision(e, e.getBoundingBox().move(0, up, 0))) {
                if (e instanceof FakePlayer) e.moveTo(e.getX(), e.getY() + up, e.getZ());
                else e.teleportTo(e.getX(), e.getY() + up, e.getZ());
                return;
            }
        }
    }

    /** The filing cabinet {@code number} (1-4) as the office expects it, facing its centre. */
    public static BlockState cabinet(int number) {
        LayoutPoint p = ZachariahOfficeLayout.CABINETS.get(number - 1);
        Direction facing = Math.abs(p.x()) >= Math.abs(p.z())
                ? (p.x() > 0 ? Direction.WEST : Direction.EAST) : (p.z() > 0 ? Direction.NORTH : Direction.SOUTH);
        return AllBlocks.FILING_CABINET.get().defaultBlockState().setValue(FilingCabinetBlock.NUMBER, number).setValue(FilingCabinetBlock.FACING, facing);
    }

    /** Where cabinet {@code number} (1-4) stands. */
    public BlockPos cabinetPos(int number) {
        LayoutPoint p = ZachariahOfficeLayout.CABINETS.get(number - 1);
        return officeOrigin().offset(p.x(), p.y(), p.z());
    }

    /** The rule: a numbered cabinet at each of the four points, the plan's own if it has one, else one through the arena. */
    private void placeCabinets(ServerLevel level, ArenaController arena) {
        cabinetsPlaced = true;
        for (int n = 1; n <= FormRules.CABINETS; n++) {
            BlockPos pos = cabinetPos(n);
            BlockState there = level.getBlockState(pos);
            if (there.is(AllBlocks.FILING_CABINET.get()) && there.getValue(FilingCabinetBlock.NUMBER) == n) continue;
            arena.mutate(level, pos, cabinet(n), 0);
        }
    }

    /** The owner's memories, filed as text on the office's walls (display entities, taken away with him). */
    private void placeDecor(ServerLevel level) {
        decorPlaced = true;
        List<Component> lines = memoryLines(owner != null ? level.getServer().getPlayerList().getPlayer(owner) : null,
                ZachariahOfficeLayout.FRAMES.size());
        for (int i = 0; i < lines.size() && i < ZachariahOfficeLayout.FRAMES.size(); i++) {
            Vec3 at = at(ZachariahOfficeLayout.FRAMES.get(i)).add(0, 0.6, 0);
            CompoundTag tag = new CompoundTag();
            tag.putString("id", "minecraft:text_display");
            ListTag pos = new ListTag();
            pos.add(net.minecraft.nbt.DoubleTag.valueOf(at.x));
            pos.add(net.minecraft.nbt.DoubleTag.valueOf(at.y));
            pos.add(net.minecraft.nbt.DoubleTag.valueOf(at.z));
            tag.put("Pos", pos);
            tag.putString("text", Component.Serializer.toJson(lines.get(i), level.registryAccess()));
            tag.putString("billboard", "vertical");
            tag.putInt("background", 0xC0F2EAD3);
            tag.putInt("line_width", 120);
            Entity e = EntityType.loadEntityRecursive(tag, level, x -> x);
            if (e != null && level.addFreshEntity(e)) decor.add(e.getUUID());
        }
    }

    /**
     * What hangs on the office's walls: the owner's latest memories, filed ({@code decor.supernaturalcraft.zachariah.memory}: kind and
     * subject); with no owner, Heaven's own stock of framed platitudes.
     */
    static List<Component> memoryLines(@Nullable ServerPlayer owner, int count) {
        List<Component> out = new ArrayList<>();
        if (owner != null) {
            var log = owner.getData(org.papiricoh.supernaturalcraft.registry.AllAttachments.MEMORY_LOG).entries();
            for (int i = log.size() - 1; i >= 0 && out.size() < count; i--) {
                var m = log.get(i);
                out.add(Component.translatable("decor.supernaturalcraft.zachariah.memory",
                        Component.translatable("decor.supernaturalcraft.zachariah.kind." + m.kind().name().toLowerCase(java.util.Locale.ROOT)),
                        m.subject()));
            }
        }
        for (int i = out.size(); i < count; i++) out.add(Component.translatable("decor.supernaturalcraft.zachariah.motto." + (i % 4)));
        return out;
    }

    private void discardDecor(ServerLevel level) {
        for (UUID id : decor) {
            Entity e = level.getEntity(id);
            if (e != null) e.discard();
        }
        decor.clear();
    }

    /** The décor he hung (tests). */
    public List<UUID> decor() {
        return List.copyOf(decor);
    }

    /** One step of the cubicle shuffle: partitions stand for a while (through the arena). @return blocks changed */
    public int shuffleCubicles() {
        ArenaController arena = arena();
        if (!(level() instanceof ServerLevel level) || arena == null) return 0;
        int n = 0;
        BlockPos o = officeOrigin();
        for (ArenaCell c : ZachariahOfficeLayout.shuffle(shuffleStep++)) {
            if (arena.mutate(level, o.offset(c.dx(), c.dy(), c.dz()), HorsemenGround.state(c.block()), ZachariahBalance.SHUFFLE_TICKS)) n++;
        }
        return n;
    }

    /** Final Judgment: the walls and ceiling dissolve into sky ({@code now}: all at once, for tests and previews). */
    public void dissolveOffice(ServerLevel level, ArenaController arena, boolean now) {
        if (dissolved) return;
        dissolved = true;
        HorsemenGround g = fixed(officeOrigin(), ZachariahOfficeLayout.dissolve());
        if (now) g.finish(level, arena);
        else writing = g;
        Vec3 c = officeCentre();
        level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + ZachariahOfficeLayout.CEILING, c.z, 160, 12, 1.5, 12, 0.05);
        fx(HeavenFxPayload.of(getId(), HeavenFxPayload.ZACHARIAH_TITLE, MAX_PHASE, 2, 0));
    }

    public boolean dissolved() {
        return dissolved;
    }

    // --- the paperwork ------------------------------------------------------------------------------

    private void tickPaperwork(ServerLevel level, long now) {
        int formTicks = SNConfig.ZACHARIAH_FORM_TICKS.get();
        for (Map.Entry<UUID, Issued> e : List.copyOf(forms.entrySet())) {
            if (!FormRules.overdue(e.getValue().issued(), now, formTicks)) continue;
            forms.remove(e.getKey());
            if (!overdue.contains(e.getKey())) overdue.add(e.getKey());
            if (hunter(e.getKey()) instanceof Player p) {
                removeForms(p);
                if (p instanceof ServerPlayer sp) {
                    say(sp, "overdue");
                    fxTo(sp, HeavenFxPayload.of(getId(), HeavenFxPayload.FORM, 0, 0, 0));
                }
            }
        }
        if (nextIssue >= 0 && now >= nextIssue && scheduler().current() == null) {
            issueForms(now);
            nextIssue = now + formTicks;
        }
        approvedUntil.values().removeIf(until -> !FormRules.approved(until, now));
        if (now % 100 == 0) {
            for (Map.Entry<UUID, Issued> e : forms.entrySet()) {
                if (hunter(e.getKey()) instanceof ServerPlayer sp) {
                    fxTo(sp, HeavenFxPayload.of(getId(), HeavenFxPayload.FORM, e.getValue().number(), 0,
                            FormRules.ticksLeft(e.getValue().issued(), now, formTicks)));
                }
            }
        }
    }

    /** A new Heavenly Form for every hunter (any old one is taken back). */
    public void issueForms(long now) {
        for (LivingEntity h : hunters()) if (h instanceof Player p && h.isAlive()) issueForm(p, now);
        playSound(sound("paper_storm"), 1.5f, 1.4f);
    }

    /** A new form for {@code p}: a different cabinet from their last. @return the form */
    public HeavenlyForm issueForm(Player p, long now) {
        int number = FormRules.numberFor(random.nextInt(1 << 16), lastNumber.getOrDefault(p.getUUID(), 0));
        lastNumber.put(p.getUUID(), number);
        forms.put(p.getUUID(), new Issued(number, now));
        removeForms(p);
        HeavenlyForm form = new HeavenlyForm(number, now, p.getUUID());
        ItemStack stack = new ItemStack(AllItems.HEAVENLY_FORM.get());
        stack.set(AllDataComponents.HEAVENLY_FORM.get(), form);
        if (!p.getInventory().add(stack)) {
            ItemEntity item = new ItemEntity(level(), p.getX(), p.getY() + 0.5, p.getZ(), stack);
            item.setNoPickUpDelay();
            level().addFreshEntity(item);
        }
        if (p instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.zachariah.form_issued", FormRules.roman(number))
                    .withStyle(ChatFormatting.GOLD), true);
            fxTo(sp, HeavenFxPayload.of(getId(), HeavenFxPayload.FORM, number, 0, SNConfig.ZACHARIAH_FORM_TICKS.get()));
        }
        return form;
    }

    /** Takes every Heavenly Form out of {@code p}'s inventory. */
    public static void removeForms(Player p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(AllItems.HEAVENLY_FORM.get())) inv.setItem(i, ItemStack.EMPTY);
        }
    }

    /** What {@code hunter} holds unfiled, if anything. */
    public @Nullable Issued formOf(UUID hunter) {
        return forms.get(hunter);
    }

    public boolean approved(UUID hunter) {
        Long until = approvedUntil.get(hunter);
        return until != null && FormRules.approved(until, level().getGameTime());
    }

    /**
     * Files {@code hunter}'s form in cabinet {@code cabinet} ({@code stamped}: an approval stamp, any cabinet). Filed, they are
     * Approved. @return what came of it
     */
    public Filing file(UUID hunter, int cabinet, boolean stamped) {
        Issued f = forms.get(hunter);
        if (f == null) return Filing.NOTHING_TO_FILE;
        if (!stamped && !FormRules.matches(f.number(), cabinet)) return Filing.WRONG_CABINET;
        forms.remove(hunter);
        long now = level().getGameTime();
        approvedUntil.put(hunter, now + FormRules.APPROVED_TICKS);
        LivingEntity h = hunter(hunter);
        if (h instanceof Player p) removeForms(p);
        if (h instanceof ServerPlayer sp) {
            say(sp, stamped ? "stamped" : "approved");
            fxTo(sp, HeavenFxPayload.of(getId(), HeavenFxPayload.FORM, 0, 0, 0));
            fxTo(sp, HeavenFxPayload.of(getId(), HeavenFxPayload.APPROVED, 0, 0, FormRules.APPROVED_TICKS));
        }
        if (h != null) {
            level().playSound(null, h.blockPosition(), sound(stamped ? "stamp" : "file"), SoundSource.PLAYERS, 1.0f, 1.0f);
            level().playSound(null, h.blockPosition(), sound("approved"), SoundSource.PLAYERS, 1.0f, 1.2f);
        }
        return Filing.FILED;
    }

    /** The next hunter whose form went overdue (and is still here), or null. */
    private @Nullable LivingEntity takeOverdue() {
        while (!overdue.isEmpty()) {
            LivingEntity h = hunter(overdue.poll());
            if (h != null && h.isAlive()) return h;
        }
        return null;
    }

    /** Hunters whose forms went overdue, waiting for their smite. */
    public List<UUID> overdueHunters() {
        return List.copyOf(overdue);
    }

    /** Test hook: the form {@code hunter} holds was issued at {@code issued}. */
    public void backdateForm(UUID hunter, long issued) {
        Issued f = forms.get(hunter);
        if (f != null) forms.put(hunter, new Issued(f.number(), issued));
    }

    /** Beyond Lucifer's own: an unfiled form weighs a hunter's blow down, an approval lifts it; reeling, ×1.3. */
    @Override
    protected float vulnerability(DamageSource source) {
        float v = super.vulnerability(source);
        if (source.getEntity() instanceof Player p) v *= paperworkFactor(p.getUUID());
        if (isStaggered()) v *= ZachariahBalance.STAGGER_VULNERABILITY;
        return v;
    }

    /** What {@code hunter}'s paperwork does to their blows now. */
    public float paperworkFactor(UUID hunter) {
        return FormRules.damageFactor(forms.containsKey(hunter), approved(hunter));
    }

    // --- the docket (phases III and IV) -------------------------------------------------------------

    private void openDocket(int phase) {
        docket.open(ZachariahBalance.docketSize(phase));
        if (docket.active()) {
            fillDocket();
            announce();
        } else {
            fx(HeavenFxPayload.of(getId(), HeavenFxPayload.FORETOLD, 0, 0, 0));
        }
    }

    private void fillDocket() {
        LivingEntity target = attackTarget();
        docket.fill(lastForetold, prev -> ZachariahAttacks.pick(this, target, prev));
    }

    /** What is written now, to everyone near. */
    private void announce() {
        fx(new HeavenFxPayload(getId(), HeavenFxPayload.FORETOLD, docket.size(), phase(), Vec3.ZERO, 0, docket.text()));
        playSound(sound("docket"), 1.2f, 1.0f);
    }

    public Docket docket() {
        return docket;
    }

    /** Takes the docket's head and writes the next; once a phase, after a few, an entry is revised. @return the attack, or null */
    public @Nullable BossAttack<LuciferEntity> nextForetold() {
        if (!docket.active()) return null;
        if (docket.size() == 0) fillDocket();
        String id = docket.next();
        if (id == null) return null;
        lastForetold = id;
        fillDocket();
        if (!docket.revised() && docket.taken() >= ZachariahBalance.REVISION_AFTER && docket.size() >= 2) revise();
        announce();
        BossAttack<LuciferEntity> attack = ZachariahAttacks.create(id, ZachariahBalance.FORETOLD_LEAD);
        return attack != null ? attack : ZachariahAttacks.create(ZachariahAttacks.PAPER_STORM, ZachariahBalance.FORETOLD_LEAD);
    }

    /** This phase's revision: an entry (never the next) struck through, another attack in its place soon. */
    private void revise() {
        int index = 1 + random.nextInt(docket.size() - 1);
        String old = docket.entries().get(index);
        String replacement = old;
        for (int i = 0; i < 8 && replacement.equals(old); i++) replacement = ZachariahAttacks.pick(this, attackTarget(), old);
        if (docket.revise(index, replacement, level().getGameTime() + ZachariahBalance.REVISION_WARNING)) {
            fx(new HeavenFxPayload(getId(), HeavenFxPayload.REVISION, index, 0, Vec3.ZERO, ZachariahBalance.REVISION_WARNING, replacement));
            playSound(sound("docket"), 1.5f, 0.7f);
        }
    }

    // --- the Termination Notice and the desks -------------------------------------------------------

    public boolean noticeServed() {
        return noticeOn != null && noticeOn.isAlive();
    }

    /** A notice served on {@code on} (null: withdrawn), due in {@code ticks}. */
    public void serveNotice(@Nullable LivingEntity on, int ticks) {
        noticeOn = on;
        if (on == null) return;
        fx(HeavenFxPayload.of(on.getId(), HeavenFxPayload.TERMINATION, 0, 0, ticks));
        if (on instanceof ServerPlayer p) say(p, "notice");
    }

    /** Whether {@code pos} stands at one of the Approved desks. */
    public boolean atApprovedDesk(Vec3 pos) {
        for (LayoutPoint p : ZachariahOfficeLayout.DESK_SAFE) {
            Vec3 d = at(p);
            double dx = pos.x - d.x, dz = pos.z - d.z;
            if (dx * dx + dz * dz <= ZachariahBalance.DESK_RADIUS * ZachariahBalance.DESK_RADIUS && Math.abs(pos.y - d.y) < 2.5) return true;
        }
        return false;
    }

    // --- the precedent ------------------------------------------------------------------------------

    private void recordTrails(long now) {
        for (LivingEntity h : hunters()) {
            trails.computeIfAbsent(h.getUUID(), k -> PositionTrail.covering(ZachariahBalance.PRECEDENT_DELAY + 40, ZachariahBalance.TRAIL_EVERY))
                    .record(now, h.getX(), h.getY(), h.getZ());
        }
    }

    /** Test hook: {@code e} was at {@code pos} at {@code tick}. */
    public void recordTrail(LivingEntity e, long tick, Vec3 pos) {
        trails.computeIfAbsent(e.getUUID(), k -> PositionTrail.covering(ZachariahBalance.PRECEDENT_DELAY + 40, ZachariahBalance.TRAIL_EVERY))
                .record(tick, pos.x, pos.y, pos.z);
    }

    /** Where {@code e} stood {@code ticks} ago, folded into the office's window (null if unknown). */
    public @Nullable Vec3 whereWas(LivingEntity e, long now, int ticks) {
        PositionTrail trail = trails.get(e.getUUID());
        PositionTrail.Sample s = trail == null ? null : trail.back(now, ticks);
        if (s == null) return null;
        Vec3 c = officeCentre();
        return new Vec3(c.x + ZachariahBalance.fold(s.x() - c.x), s.y(), c.z + ZachariahBalance.fold(s.z() - c.z));
    }

    // --- his clerks ---------------------------------------------------------------------------------

    /** His clerks out of the cubicles (up to {@link ZachariahBalance#CLERKS} standing). */
    public List<ClerkAngelEntity> callClerks(ServerLevel level) {
        List<ClerkAngelEntity> out = new ArrayList<>();
        int room = ZachariahBalance.CLERKS - clerks().size();
        List<LayoutPoint> spawns = new ArrayList<>(ZachariahOfficeLayout.CLERK_SPAWNS);
        java.util.Collections.shuffle(spawns, new java.util.Random(random.nextLong()));
        for (int i = 0; i < room && i < spawns.size(); i++) {
            ClerkAngelEntity c = AllEntities.CLERK_ANGEL.get().create(level);
            if (c == null) continue;
            Vec3 at = at(spawns.get(i));
            c.moveTo(at.x, at.y, at.z, random.nextFloat() * 360, 0);
            c.finalizeSpawn(level, level.getCurrentDifficultyAt(c.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            c.serve(getUUID());
            level.addFreshEntity(c);
            minions().add(c.getUUID());
            out.add(c);
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1, at.z, 16, 0.3, 0.8, 0.3, 0.02);
        }
        return out;
    }

    /** His clerks standing now. */
    public List<ClerkAngelEntity> clerks() {
        List<ClerkAngelEntity> out = new ArrayList<>();
        if (!(level() instanceof ServerLevel level)) return out;
        for (UUID id : minions()) if (level.getEntity(id) instanceof ClerkAngelEntity c && c.isAlive()) out.add(c);
        return out;
    }

    private void dismissClerks(ServerLevel level) {
        for (ClerkAngelEntity c : clerks()) {
            level.sendParticles(ParticleTypes.END_ROD, c.getX(), c.getY() + 1, c.getZ(), 12, 0.3, 0.8, 0.3, 0.03);
            c.discard();
        }
    }

    // --- reeling ------------------------------------------------------------------------------------

    public boolean isStaggered() {
        return level().getGameTime() < staggeredUntil;
    }

    public void stagger() {
        staggeredUntil = level().getGameTime() + ZachariahBalance.STAGGER_TICKS;
        scheduler().cancel();
        scheduler().delay(ZachariahBalance.STAGGER_TICKS);
        getNavigation().stop();
        triggerAnim("action", ZachariahAnimations.STAGGER);
    }

    // --- hunters and messages -----------------------------------------------------------------------

    /** The hunters his office sees: the challengers, and anyone tracked (tests). */
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

    private @Nullable LivingEntity hunter(UUID id) {
        for (LivingEntity h : hunters()) if (h.getUUID().equals(id)) return h;
        return level() instanceof ServerLevel level && level.getPlayerByUUID(id) instanceof Player p ? p : null;
    }

    /** One of his lines to {@code p} ({@code message.supernaturalcraft.zachariah.<key>}). */
    public void say(ServerPlayer p, String key) {
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.zachariah." + key).withStyle(ChatFormatting.GOLD), true);
    }

    /** Sends one of the office's moments to every hunter near. */
    public void fx(HeavenFxPayload payload) {
        if (!(level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 96 * 96) PacketDistributor.sendToPlayer(p, payload);
        }
    }

    /** Sends a moment to one hunter (never to a fake one). */
    public static void fxTo(ServerPlayer p, HeavenFxPayload payload) {
        if (!(p instanceof FakePlayer)) PacketDistributor.sendToPlayer(p, payload);
    }

    /** The Zachariah whose office holds {@code pos}, if any. */
    public static @Nullable ZachariahEntity holding(ServerLevel level, Vec3 pos) {
        ArenaController arena = ArenaSavedData.get(level).at(pos);
        if (arena == null || arena.bossId() == null) return null;
        return level.getEntity(arena.bossId()) instanceof ZachariahEntity z && z.isAlive() ? z : null;
    }

    // --- attacks ------------------------------------------------------------------------------------

    /** Tests and previews: the next attack he uses, whatever the pool or the docket says. */
    public void queue(Supplier<BossAttack<LuciferEntity>> attack) {
        queued = attack;
    }

    /** A queued attack first; an overdue form's smite; a blink after a runner; then the docket, from phase III. */
    @Override
    public @Nullable Supplier<BossAttack<LuciferEntity>> forcedAttack(LivingEntity target) {
        if (queued != null) {
            Supplier<BossAttack<LuciferEntity>> q = queued;
            queued = null;
            return q;
        }
        LivingEntity debtor = takeOverdue();
        if (debtor != null) return () -> new ZachariahAttacks.OverdueSmite(debtor);
        if (distanceToSqr(target) > ZachariahBalance.BLINK_DISTANCE * ZachariahBalance.BLINK_DISTANCE
                || noSightTicks > ZachariahBalance.BLINK_SIGHT_TICKS) {
            noSightTicks = 0;
            return ZachariahAttacks.Blink::new;
        }
        if (docket.active()) {
            BossAttack<LuciferEntity> foretold = nextForetold();
            if (foretold != null) return () -> foretold;
        }
        return null;
    }

    // --- GeckoLib -----------------------------------------------------------------------------------

    /** Only his own clips: Lucifer's names that he lacks ({@code transform_<n>}, …) are dropped. */
    @Override
    public void triggerAnim(@Nullable String controller, String anim) {
        if ("action".equals(controller) && !ZachariahAnimations.TRIGGERED.contains(anim)) return;
        if (level().isClientSide) {
            var manager = getAnimatableInstanceCache().getManagerForId(getId());
            if (controller != null) manager.tryTriggerAnimation(controller, anim);
            else manager.tryTriggerAnimation(anim);
        } else {
            GeckoLibServices.NETWORK.triggerEntityAnim(this, false, controller, anim);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = animationPrefix();
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + ZachariahAnimations.IDLE);
        RawAnimation walk = RawAnimation.begin().thenLoop(pre + ZachariahAnimations.WALK);
        controllers.add(new AnimationController<>(this, "base", 6, state -> {
            byte s = state();
            if (s == EMERGING || s == DYING) return PlayState.STOP;
            return state.setAndContinue(state.isMoving() ? walk : idle);
        }));
        AnimationController<ZachariahEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String name : ZachariahAnimations.TRIGGERED) {
            action.triggerableAnim(name, ZachariahAnimations.HOLDS.contains(name)
                    ? RawAnimation.begin().thenPlayAndHold(pre + name)
                    : RawAnimation.begin().thenPlay(pre + name));
        }
        controllers.add(action);
    }

    // --- persistence --------------------------------------------------------------------------------

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel level && reason.shouldDestroy()) discardDecor(level);
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Wings", wingsShown());
        tag.putBoolean("Emerged", emerged);
        tag.putBoolean("PermanentOffice", permanentOffice);
        tag.putBoolean("Dissolved", dissolved);
        tag.putInt("ShuffleStep", shuffleStep);
        if (owner != null) tag.putUUID("Owner", owner);
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Issued> e : forms.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Hunter", e.getKey());
            t.putInt("Number", e.getValue().number());
            t.putLong("Issued", e.getValue().issued());
            list.add(t);
        }
        tag.put("Forms", list);
        tag.putLong("NextIssue", nextIssue);
        ListTag decorTag = new ListTag();
        for (UUID id : decor) decorTag.add(StringTag.valueOf(id.toString()));
        tag.put("Decor", decorTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(WINGS, tag.getBoolean("Wings"));
        emerged = tag.getBoolean("Emerged");
        permanentOffice = tag.getBoolean("PermanentOffice");
        dissolved = tag.getBoolean("Dissolved");
        shuffleStep = tag.getInt("ShuffleStep");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        forms.clear();
        for (Tag t : tag.getList("Forms", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            if (c.hasUUID("Hunter")) forms.put(c.getUUID("Hunter"), new Issued(c.getInt("Number"), c.getLong("Issued")));
        }
        nextIssue = tag.contains("NextIssue") ? tag.getLong("NextIssue") : -1;
        decor.clear();
        for (Tag t : tag.getList("Decor", Tag.TAG_STRING)) {
            try {
                decor.add(UUID.fromString(t.getAsString()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        // An egg's office was written already (the arena still holds it); the docket is written afresh.
        officeBegun = true;
        cabinetsPlaced = true;
        decorPlaced = true;
        docketPending = emerged;
    }
}
