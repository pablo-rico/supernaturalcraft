package org.papiricoh.supernaturalcraft.entity.boss.abaddon;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.BossDamage;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.HorsemenGround;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.hunter.DevilsTrapBlock;
import org.papiricoh.supernaturalcraft.magic.spell.SpellHooks;
import org.papiricoh.supernaturalcraft.mark.Marks;
import org.papiricoh.supernaturalcraft.network.AbaddonFxPayload;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
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
 * Abaddon, Knight of Hell (v0.21): an optional superboss after Cain and the three Horsemen, called up by her rite
 * ({@code summon_abaddon}) into a hall of blackstone, chains and braziers ({@link ArenaTheme#KNIGHTS}, {@link AbaddonGround}).
 * Three phases, a third of her true health each (24 000 alone):
 *
 * <pre>EMERGING (black smoke, then her vessel: the intro camera) → P1 the Knight of Hell (her herald marks, hellfire, her
 *   knights) → P2 Possession (she leaves her vessel as smoke and rides a knight) → P3 the Brand of the Knights (telekinesis and
 *   the Brand) → DYING (she burns from inside)</pre>
 *
 * <p><b>Damage.</b> {@link AbaddonBalance#share}: 35 % of every blow but the First Blade's or a Marked hunter's; a painted devil's
 * trap under her holds her {@link AbaddonBalance#TRAPPED_TICKS} at full damage (then she breaks it, {@link #breakFree}).
 *
 * <p><b>Possession.</b> {@link AbaddonAttacks.SmokeOut} sends her out of her vessel along a yellow lane into one of her knights
 * ({@link #beginPossession}): her body is hidden ({@link #smoke()}, untouchable, not pickable) and every blow on the knight
 * lands on her instead ({@link #hurtThroughVessel}). Exorcise, Banish by Name (her taglock or the knight's), a Salt Shot, the
 * knight standing on a devil's trap or its death force her out ({@link #expel}): she is back in her body, stunned
 * {@link AbaddonBalance#STUN_TICKS} at ×2 damage. Otherwise she leaves of her own will after
 * {@link AbaddonBalance#POSSESS_TICKS}.
 *
 * <p>As she dies the Book of the Damned goes to every hunter who fought her ({@link AbaddonSpoils}). Optional: she blocks nothing.
 */
public class AbaddonEntity extends LuciferEntity implements SpellHooks.Expellable {

    public static final int MAX_PHASE = AbaddonBalance.PHASES;
    public static final int INTRO_TICKS = AbaddonCinematics.INTRO_TICKS;

    /** Synced: bit {@link #SMOKE} (she is smoke in a knight: drawn not at all), {@link #TRAPPED}, {@link #STUNNED}. */
    private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(AbaddonEntity.class, EntityDataSerializers.BYTE);
    public static final int SMOKE = 1, TRAPPED = 2, STUNNED = 4;

    private boolean emerged;
    private @Nullable UUID possessing;
    private int possessTicks, possessCooldown, stunTicks, trappedTicks, trapCooldown, sinceSummon;
    private @Nullable BlockPos trapAt;
    /** A blow passed on from her possessed knight (the only kind that reaches her while she is smoke). */
    private boolean forwarding;
    private @Nullable HorsemenGround ground;
    private boolean groundLaid, groundBranded;
    /** Tests: hunters her attacks see beyond the challengers (fake players are not in the level's list). */
    private final List<LivingEntity> tracked = new ArrayList<>();
    /** Tests and previews: an attack to use next, whatever the pool says. */
    private @Nullable Supplier<BossAttack<LuciferEntity>> queued;

    public AbaddonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 800;
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
        builder.define(FLAGS, (byte) 0);
    }

    private boolean flag(int bit) {
        return (entityData.get(FLAGS) & bit) != 0;
    }

    private void setFlag(int bit, boolean on) {
        byte b = entityData.get(FLAGS);
        entityData.set(FLAGS, (byte) (on ? b | bit : b & ~bit));
    }

    /** She is smoke riding a knight (synced): her body is not drawn, cannot be struck or picked. */
    public boolean smoke() {
        return flag(SMOKE);
    }

    /** A devil's trap holds her (synced: the {@code trapped} loop). */
    public boolean trapped() {
        return flag(TRAPPED);
    }

    /** Just forced out of a knight (synced): stunned, ×2 damage. */
    public boolean stunned() {
        return flag(STUNNED);
    }

    // --- her numbers --------------------------------------------------------------------------------

    @Override
    public int maxPhase() {
        return MAX_PHASE;
    }

    @Override
    protected float threshold(int phase) {
        return AbaddonBalance.threshold(phase);
    }

    @Override
    protected double healthPerExtraPlayer() {
        return SNConfig.ABADDON_HEALTH_PER_PLAYER.get();
    }

    @Override
    protected float mundaneMultiplier() {
        return SNConfig.ABADDON_MUNDANE_MULTIPLIER.get().floatValue();
    }

    @Override
    protected float damageFactor() {
        return SNConfig.ABADDON_DAMAGE_FACTOR.get().floatValue();
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
        return INTRO_TICKS;
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return AbaddonAttacks.pool(phase);
    }

    @Override
    protected int baseGap(int phase) {
        return AbaddonBalance.attackGap(phase);
    }

    @Override
    protected String animationPrefix() {
        return "animation.abaddon.";
    }

    @Override
    protected List<String> triggeredAnimations() {
        return AbaddonAssets.TRIGGERED;
    }

    /** {@code entity.supernaturalcraft.abaddon.bar.phase<n>}: the HUD's key for her bar. */
    @Override
    protected String bossBarKey(int phase) {
        return "entity.supernaturalcraft.abaddon.bar.phase" + phase;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return phase == 2 ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.RED;
    }

    @Override
    protected Component bossBarName(int phase) {
        return Component.translatable(bossBarKey(phase)).withStyle(phase == MAX_PHASE ? ChatFormatting.DARK_RED : ChatFormatting.RED);
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return phase == 2 ? AllParticles.DEMON_SMOKE.get() : AllParticles.HELLFIRE.get();
    }

    @Override
    protected SoundEvent emergeSound() {
        return AllSounds.abaddon("smoke");
    }

    @Override
    protected SoundEvent roarSound() {
        return AllSounds.abaddon("laugh");
    }

    @Override
    protected SoundEvent transformSound() {
        return AllSounds.abaddon("laugh");
    }

    @Override
    protected SoundEvent ambientBossSound() {
        return AllSounds.abaddon("ambient");
    }

    @Override
    protected SoundEvent dyingSound() {
        return AllSounds.abaddon("death");
    }

    @Override
    protected SoundEvent deflectSound() {
        return AllSounds.abaddon("hurt");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.abaddon("hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.abaddon("death");
    }

    @Override
    protected void clientEmergenceParticles() {
        level().addParticle(ParticleTypes.LARGE_SMOKE, getRandomX(0.8), getY() + random.nextDouble() * 2, getRandomZ(0.8), 0, 0.05, 0);
    }

    @Override
    protected void dyingParticles(ServerLevel level, boolean last) {
        if (last) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 80, 0.5, 1.0, 0.5, 0.08);
            level.sendParticles(AllParticles.HELLFIRE.get(), getX(), getY() + 1, getZ(), 60, 0.4, 0.8, 0.4, 0.06);
            return;
        }
        level.sendParticles(AllParticles.HELLFIRE.get(), getX(), getY() + 1.1, getZ(), 3, 0.25, 0.5, 0.25, 0.02);
    }

    // --- the fight's shape --------------------------------------------------------------------------

    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        return LuciferSummoning.openArena(level, blockPosition(), SNConfig.ABADDON_ARENA_RADIUS.get(), ArenaTheme.KNIGHTS);
    }

    @Override
    protected void playEmergence() {
        AbaddonCinematics.intro(this);
    }

    @Override
    protected void playTransition(int to) {
        AbaddonCinematics.transition(this, to);
    }

    @Override
    protected void playDeath() {
        AbaddonCinematics.death(this);
    }

    @Override
    protected void onTransitionStart(int to) {
        if (level() instanceof ServerLevel level) {
            settle(level);
            level.sendParticles(AllParticles.DEMON_SMOKE.get(), getX(), getY() + 1.2, getZ(), 15, 0.5, 0.8, 0.5, 0.05);
        }
        // Possession needs knights: she calls them as it begins; the Brand brings one more.
        AbaddonAttacks.summonRetinue(this, to == 2 ? AbaddonBalance.KNIGHTS_PER_CALL : 1, to >= 3);
        sinceSummon = 0;
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
        return AbaddonBalance.DEATH_TICKS;
    }

    @Override
    protected void tickDyingMotion(int elapsed) {
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
    }

    /** The Brand's phase: her hall's braziers burn soul-blue and the floor cracks ({@link AbaddonGround#branded}). */
    @Override
    protected void applyTerrain(ServerLevel level, ArenaController arena, int phase) {
        if (phase >= MAX_PHASE && ground != null && !groundBranded) {
            groundBranded = true;
            ground = ground.mapped(AbaddonGround::branded);
            groundLaid = false;
        }
    }

    /** The fight was lost: she goes back into the smoke; nothing is left behind (the stake, if any, comes back by itself). */
    @Override
    protected void leaveBehind(ServerLevel level, Vec3 at) {
    }

    @Override
    protected void returnToCage(ServerLevel level, String messageKey) {
        if (possessing != null) settle(level);
        super.returnToCage(level, messageKey.replace(".lucifer.", ".abaddon."));
    }

    /** She burns from inside; her knights crumble with her. */
    @Override
    protected void beginDying() {
        if (level() instanceof ServerLevel level) {
            settle(level);
            for (UUID id : minions()) {
                Entity e = level.getEntity(id);
                if (e == null) continue;
                level.sendParticles(AllParticles.DEMON_SMOKE.get(), e.getX(), e.getY() + 1, e.getZ(), 6, 0.3, 0.8, 0.3, 0.05);
                e.discard();
            }
        }
        super.beginDying();
    }

    /** At the end of her death: the victory, and the spoils (the Book for each hunter, the cleaver, the bust). */
    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        AbaddonCinematics.victory(this);
        List<ServerPlayer> hunters = new ArrayList<>(challengers());
        for (LivingEntity e : tracked) if (e instanceof ServerPlayer p && !hunters.contains(p)) hunters.add(p);
        AbaddonSpoils.drop(level, this, hunters, position().add(0, 1.2, 0));
    }

    @Override
    protected boolean walks() {
        return possessing == null && stunTicks <= 0 && trappedTicks <= 0;
    }

    @Override
    protected void customServerAiStep() {
        if (level() instanceof ServerLevel level && !isRemoved()) tickKnight(level);
        super.customServerAiStep();
        if (isRemoved() || !(level() instanceof ServerLevel level)) return;
        ArenaController arena = arena();
        if (arena != null && arena.isActive()) tickGround(level, arena);
        if (state() == EMERGING || emerged) return;
        emerged = true;
        fx(new AbaddonFxPayload(getId(), AbaddonFxPayload.TITLE, phase(), 0, position(), 70));
    }

    /** Her own states before the shared fight: possession, the stun, the trap. */
    private void tickKnight(ServerLevel level) {
        byte s = state();
        if (s == EMERGING || s == DYING || s == TRANSITION) return;
        if (possessCooldown > 0) possessCooldown--;
        if (trapCooldown > 0) trapCooldown--;
        sinceSummon++;
        if (possessing != null) {
            hold();
            scheduler().delay(5);
            tickPossession(level);
            return;
        }
        if (stunTicks > 0) {
            hold();
            scheduler().delay(5);
            if (tickCount % 5 == 0) level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.8, getZ(), 3, 0.3, 0.2, 0.3, 0.01);
            if (--stunTicks <= 0) setFlag(STUNNED, false);
            return;
        }
        if (trappedTicks > 0) {
            hold();
            scheduler().delay(5);
            if (--trappedTicks <= 0) breakFree(level);
            return;
        }
        if (trapCooldown <= 0) {
            BlockPos feet = blockPosition();
            BlockState st = level.getBlockState(feet);
            if (st.getBlock() instanceof DevilsTrapBlock) beginTrapped(level, DevilsTrapBlock.center(feet, st));
        }
    }

    private void hold() {
        getNavigation().stop();
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
    }

    private void tickGround(ServerLevel level, ArenaController arena) {
        if (ground == null) {
            if (groundLaid) return; // laid before a reload: not pinned again over its own blocks
            ground = HorsemenGround.pin(level, arena, AbaddonGround.plan(arena.radius(), arena.center().asLong()));
        }
        if (!groundLaid && ground.tick(level, arena)) groundLaid = true;
    }

    /** Test and preview hook: lays her hall now. */
    public void layGroundNow() {
        if (!(level() instanceof ServerLevel level) || arena() == null) return;
        ArenaController arena = arena();
        if (ground == null) ground = HorsemenGround.pin(level, arena, AbaddonGround.plan(arena.radius(), arena.center().asLong()));
        ground.finish(level, arena);
        groundLaid = true;
    }

    // --- the trap -----------------------------------------------------------------------------------

    /** A devil's trap holds her: no attacks, no steps, full damage. */
    public void beginTrapped(ServerLevel level, BlockPos centre) {
        trappedTicks = AbaddonBalance.TRAPPED_TICKS;
        trapAt = centre.immutable();
        scheduler().cancel();
        hold();
        teleportTo(centre.getX() + 0.5, getY(), centre.getZ() + 0.5);
        setFlag(TRAPPED, true);
        fx(new AbaddonFxPayload(getId(), AbaddonFxPayload.TRAPPED, 0, 0, Vec3.atBottomCenterOf(centre), trappedTicks));
        playSound(AllSounds.abaddon("trapped"), 2f, 1f);
        tell(level, "message.supernaturalcraft.abaddon.trapped");
    }

    /** She tears the trap's seal apart: it is gone, the hunters near are thrown back, and traps fail on her for a while. */
    public void breakFree(ServerLevel level) {
        trappedTicks = 0;
        trapCooldown = AbaddonBalance.TRAP_COOLDOWN;
        setFlag(TRAPPED, false);
        BlockPos at = trapAt != null ? trapAt : blockPosition();
        // The trap was painted by a hunter, not laid by the fight: it is broken for good (not through the arena's snapshot).
        if (level.getBlockState(at).getBlock() instanceof DevilsTrapBlock) level.destroyBlock(at, false);
        trapAt = null;
        fx(new AbaddonFxPayload(getId(), AbaddonFxPayload.TRAP_FLARE, 0, 0, Vec3.atBottomCenterOf(at), 20));
        level.sendParticles(AllParticles.HELLFIRE.get(), getX(), getY() + 0.3, getZ(), 60, 1.5, 0.2, 1.5, 0.05);
        for (LivingEntity e : hunters()) {
            Vec3 away = e.position().subtract(position()).multiply(1, 0, 1);
            if (away.length() > 5) continue;
            away = away.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : away.normalize();
            e.push(away.x * 1.2, 0.45, away.z * 1.2);
            e.hurtMarked = true;
        }
        playSound(AllSounds.abaddon("laugh"), 2f, 0.9f);
        scheduler().delay(20);
    }

    // --- possession ---------------------------------------------------------------------------------

    public @Nullable UUID possessing() {
        return possessing;
    }

    public boolean mayPossess() {
        return possessing == null && possessCooldown <= 0 && trappedTicks <= 0 && stunTicks <= 0;
    }

    /** She rides {@code knight}: her body hidden and untouchable, the knight stronger, its wounds hers. */
    public void beginPossession(AbaddonKnightEntity knight) {
        if (!(level() instanceof ServerLevel)) return;
        possessing = knight.getUUID();
        possessTicks = AbaddonBalance.POSSESS_TICKS;
        knight.setPossessor(getUUID());
        setFlag(SMOKE, true);
        hold();
        fx(new AbaddonFxPayload(getId(), AbaddonFxPayload.POSSESS, knight.getId(), 1, knight.position(), possessTicks));
        knight.playSound(AllSounds.abaddon("possess"), 2f, 1f);
    }

    private void tickPossession(ServerLevel level) {
        Entity e = level.getEntity(possessing);
        if (!(e instanceof AbaddonKnightEntity knight) || knight.isRemoved()) {
            leave(level, null, false, AbaddonFxPayload.BY_VESSEL);
            return;
        }
        if (!knight.isAlive()) {
            leave(level, knight, true, AbaddonFxPayload.BY_VESSEL);
            return;
        }
        if (level.getBlockState(knight.blockPosition()).getBlock() instanceof DevilsTrapBlock) {
            leave(level, knight, true, AbaddonFxPayload.BY_TRAP);
            return;
        }
        if (--possessTicks <= 0) leave(level, knight, false, 0);
    }

    /** She is forced out of her knight. @return false if she rides none */
    public boolean expel(@Nullable Entity by, int how) {
        if (possessing == null || !(level() instanceof ServerLevel level)) return false;
        Entity e = level.getEntity(possessing);
        leave(level, e instanceof AbaddonKnightEntity k ? k : null, true, how);
        if (by != null) fx(new AbaddonFxPayload(getId(), AbaddonFxPayload.EXPELLED_BY, by.getId(), how, by.position(), AbaddonBalance.STUN_TICKS));
        if (by instanceof ServerPlayer p) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.abaddon.expelled").withStyle(ChatFormatting.GOLD), true);
        }
        return true;
    }

    @Override
    public boolean onExpelled(@Nullable Entity by, int how) {
        return expel(by, switch (how) {
            case SpellHooks.Expellable.NAME -> AbaddonFxPayload.BY_NAME;
            case SpellHooks.Expellable.SALT -> AbaddonFxPayload.BY_SALT;
            default -> AbaddonFxPayload.BY_EXORCISE;
        });
    }

    /** She leaves the knight (forced: {@code stun}), back into her body beside it. */
    private void leave(ServerLevel level, @Nullable AbaddonKnightEntity knight, boolean stun, int how) {
        possessing = null;
        possessCooldown = AbaddonBalance.POSSESS_COOLDOWN;
        setFlag(SMOKE, false);
        if (knight != null) {
            knight.setPossessor(null);
            ArenaController arena = arena();
            Vec3 at = knight.position();
            if (arena == null || arena.horizontalDistance(at) < arena.radius() - 2) teleportTo(at.x, at.y, at.z);
        }
        int id = knight != null ? knight.getId() : -1;
        level.sendParticles(AllParticles.DEMON_SMOKE.get(), getX(), getY() + 1, getZ(), 12, 0.4, 0.9, 0.4, 0.05);
        if (stun) {
            stunTicks = AbaddonBalance.STUN_TICKS;
            setFlag(STUNNED, true);
            scheduler().cancel();
            fx(new AbaddonFxPayload(getId(), AbaddonFxPayload.EXPEL, id, how, position(), stunTicks));
            playSound(AllSounds.abaddon("hurt"), 2.5f, 0.7f);
            tell(level, "message.supernaturalcraft.abaddon.cast_out");
        } else {
            fx(new AbaddonFxPayload(getId(), AbaddonFxPayload.POSSESS, id, 0, position(), 0));
            playSound(AllSounds.abaddon("smoke"), 2f, 1.1f);
        }
        scheduler().delay(20);
    }

    /** Out of any knight, untrapped, unstunned (a change of phase, her death, the fight lost). */
    private void settle(ServerLevel level) {
        if (possessing != null) {
            Entity e = level.getEntity(possessing);
            if (e instanceof AbaddonKnightEntity k) k.setPossessor(null);
            possessing = null;
        }
        stunTicks = 0;
        trappedTicks = 0;
        trapAt = null;
        entityData.set(FLAGS, (byte) 0);
    }

    /** A blow on her possessed knight, landing on her. */
    public boolean hurtThroughVessel(DamageSource source, float amount) {
        forwarding = true;
        try {
            return hurt(source, amount);
        } finally {
            forwarding = false;
        }
    }

    // --- damage -------------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        // While she is smoke her body cannot be touched: every blow must come through the knight she rides.
        if (possessing != null && !forwarding && !BossDamage.passesThrough(source)) return false;
        if (source.getEntity() instanceof AbaddonKnightEntity || source.getEntity() instanceof AbaddonHeraldEntity) return false;
        return super.hurt(source, amount);
    }

    /** Beyond Lucifer's own: her resistance, but for the First Blade, a Marked hunter or a trap; ×2 while stunned. */
    @Override
    protected float vulnerability(DamageSource source) {
        return super.vulnerability(source) * AbaddonBalance.share(fullBlow(source), stunTicks > 0);
    }

    /** Whether {@code source} lands in full on her: the First Blade, a Marked attacker, or while a devil's trap holds her. */
    public boolean fullBlow(DamageSource source) {
        if (trappedTicks > 0) return true;
        if (source.getDirectEntity() instanceof LivingEntity by && by.getMainHandItem().is(AllItems.FIRST_BLADE.get())) return true;
        return source.getEntity() instanceof Player p && Marks.marked(p);
    }

    @Override
    public boolean isPickable() {
        return !smoke() && super.isPickable();
    }

    @Override
    public boolean isInvisible() {
        return smoke() || super.isInvisible();
    }

    // --- hunters, retinue, effects ------------------------------------------------------------------

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

    /** Her living knights (and heralds, with {@code heralds}). */
    public List<Entity> retinue(boolean knights, boolean heralds) {
        List<Entity> out = new ArrayList<>();
        if (!(level() instanceof ServerLevel level)) return out;
        minions().removeIf(id -> {
            Entity e = level.getEntity(id);
            return e == null || !e.isAlive();
        });
        for (UUID id : minions()) {
            Entity e = level.getEntity(id);
            if (knights && e instanceof AbaddonKnightEntity || heralds && e instanceof AbaddonHeraldEntity) out.add(e);
        }
        return out;
    }

    /** Ticks since she last called her retinue. */
    public int sinceSummon() {
        return sinceSummon;
    }

    public void summoned() {
        sinceSummon = 0;
    }

    /** Sends one of the fight's moments to every hunter near. */
    public void fx(AbaddonFxPayload payload) {
        if (!(level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 96 * 96) Send.toPlayer(p, payload);
        }
    }

    private void tell(ServerLevel level, String key) {
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 64 * 64) p.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_RED), true);
        }
    }

    // --- attacks ------------------------------------------------------------------------------------

    public void queue(Supplier<BossAttack<LuciferEntity>> attack) {
        queued = attack;
    }

    /** A queued attack first; her retinue when none is left; the herald's mark when the hunter runs or hides. */
    @Override
    public @Nullable Supplier<BossAttack<LuciferEntity>> forcedAttack(LivingEntity target) {
        if (queued != null) {
            Supplier<BossAttack<LuciferEntity>> q = queued;
            queued = null;
            return q;
        }
        if (sinceSummon >= AbaddonBalance.SUMMON_EVERY && retinue(true, true).isEmpty()) return AbaddonAttacks.SummonRetinue::new;
        if (distanceToSqr(target) > 16 * 16 || noSightTicks > 60) {
            noSightTicks = 0;
            return AbaddonAttacks.HeraldMark::new;
        }
        return null;
    }

    // --- GeckoLib -----------------------------------------------------------------------------------

    /** Only her own clips: Lucifer's names that she lacks ({@code emerge}, {@code transform_<n>}, …) are dropped. */
    @Override
    public void triggerAnim(@Nullable String controller, String anim) {
        if ("action".equals(controller) && !AbaddonAssets.TRIGGERED.contains(anim)) return;
        if (level().isClientSide) {
            var manager = getAnimatableInstanceCache().getManagerForId(getId());
            if (controller != null) manager.tryTriggerAnimation(controller, anim);
            else manager.tryTriggerAnimation(anim);
        } else {
            GeckoLibServices.NETWORK.triggerEntityAnim(this, false, controller, anim);
        }
    }

    static final List<String> HOLDS = List.of("death");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = animationPrefix();
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + "idle"), walk = RawAnimation.begin().thenLoop(pre + "walk"),
                caught = RawAnimation.begin().thenLoop(pre + "trapped");
        controllers.add(new AnimationController<>(this, "base", 6, state -> {
            if (state() == DYING) return PlayState.STOP;
            if (trapped()) return state.setAndContinue(caught);
            return state.setAndContinue(state.isMoving() ? walk : idle);
        }));
        AnimationController<AbaddonEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String name : AbaddonAssets.TRIGGERED) {
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
        if (possessing != null) tag.putUUID("Possessing", possessing);
        tag.putInt("PossessTicks", possessTicks);
        tag.putInt("PossessCooldown", possessCooldown);
        tag.putInt("Stun", stunTicks);
        tag.putInt("Trapped", trappedTicks);
        tag.putInt("TrapCooldown", trapCooldown);
        if (trapAt != null) tag.putLong("TrapAt", trapAt.asLong());
        tag.putBoolean("GroundLaid", groundLaid);
        tag.putBoolean("GroundBranded", groundBranded);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        emerged = tag.getBoolean("Emerged");
        possessing = tag.hasUUID("Possessing") ? tag.getUUID("Possessing") : null;
        possessTicks = tag.getInt("PossessTicks");
        possessCooldown = tag.getInt("PossessCooldown");
        stunTicks = tag.getInt("Stun");
        trappedTicks = tag.getInt("Trapped");
        trapCooldown = tag.getInt("TrapCooldown");
        trapAt = tag.contains("TrapAt") ? BlockPos.of(tag.getLong("TrapAt")) : null;
        groundLaid = tag.getBoolean("GroundLaid");
        groundBranded = tag.getBoolean("GroundBranded");
        byte f = 0;
        if (possessing != null) f |= SMOKE;
        if (trappedTicks > 0) f |= TRAPPED;
        if (stunTicks > 0) f |= STUNNED;
        entityData.set(FLAGS, f);
    }
}
