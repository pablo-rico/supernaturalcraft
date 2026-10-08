package org.papiricoh.supernaturalcraft.entity.boss.lilith;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
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
import org.papiricoh.supernaturalcraft.entity.hellhound.HellhoundEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.magic.spell.SpellHooks;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;
import org.papiricoh.supernaturalcraft.weapon.Holy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Lilith, the first demon. Called up by a ritual (once Azazel has fallen) wherever it is drawn; three
 * phases, a third of her health each. Her fall breaks the last seal that Lucifer's summoning needs.
 *
 * <pre>EMERGING (a white flash; headstones rise) → P1 The First Demon → T → P2 The Contract Comes Due
 *   → T → P3 The Last Seal → DYING</pre>
 *
 * <p>Two things of her own: <b>contracts</b> ({@link ContractLedger}) that bring invisible hellhounds
 * down on a hunter unless the others wound her enough in time, and her <b>white light</b>, which burns
 * anyone she can see: hide behind the headstones the arena raises, which crack and fall as they take
 * it. After each burst she is spent for a moment. Built on {@link LuciferEntity}'s fight through its hooks.
 */
public class LilithEntity extends LuciferEntity implements SpellHooks.Exorcisable {

    public static final int MAX_PHASE = LilithBalance.PHASES, LILITH_EMERGE_TICKS = 80, LILITH_DEATH_TICKS = 120;
    private static final int HEADSTONES_AT = 30;
    public static final int COLT_STUN_TICKS = 40, CONTRACT_STUN_TICKS = 20;

    private final ContractLedger contracts = new ContractLedger();
    /** Lower halves of the standing headstones. */
    private final List<BlockPos> headstones = new ArrayList<>();
    private boolean headstonesRaised;
    private long spentUntil;
    private int attacksSinceLight;
    private boolean allowHold;

    public LilithEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 250;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, VANILLA_BASE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.31)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    public ContractLedger contracts() {
        return contracts;
    }

    public List<BlockPos> headstones() {
        return headstones;
    }

    public boolean isSpent() {
        return level() instanceof ServerLevel level && level.getGameTime() < spentUntil;
    }

    /** Called by the white light: she has poured herself out and is open for a moment. */
    public void spend() {
        spentUntil = level().getGameTime() + LilithBalance.EMPTY_TICKS;
    }

    // --- her numbers --------------------------------------------------------------------------------

    @Override
    public int maxPhase() {
        return MAX_PHASE;
    }

    @Override
    protected float threshold(int phase) {
        return LilithBalance.threshold(phase);
    }

    @Override
    protected float mundaneMultiplier() {
        return SNConfig.LILITH_MUNDANE_MULTIPLIER.get().floatValue();
    }

    @Override
    protected double healthPerExtraPlayer() {
        return SNConfig.LILITH_HEALTH_PER_PLAYER.get();
    }

    @Override
    protected float damageFactor() {
        return SNConfig.LILITH_DAMAGE_FACTOR.get().floatValue();
    }

    @Override
    public boolean isAerialPhase() {
        return false;
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return LilithAttacks.pool(phase);
    }

    @Override
    protected int baseGap(int phase) {
        return LilithBalance.attackGap(phase);
    }

    @Override
    public float scale(int phase) {
        return 1.0f;
    }

    @Override
    protected int emergeTicks() {
        return LILITH_EMERGE_TICKS;
    }

    @Override
    protected int deathTicks() {
        return LILITH_DEATH_TICKS;
    }

    @Override
    protected int transitionTicks(int to) {
        return TRANSITION_TICKS;
    }

    @Override
    protected String animationPrefix() {
        return "animation.lilith.";
    }

    @Override
    protected List<String> triggeredAnimations() {
        return LilithAnimations.TRIGGERED;
    }

    @Override
    protected String bossBarKey(int phase) {
        return "entity.supernaturalcraft.lilith.phase" + phase;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return phase == 2 ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.WHITE;
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return AllParticles.WHITE_LIGHT.get();
    }

    @Override
    protected SoundEvent emergeSound() {
        return AllSounds.LILITH_LIGHT_BURST.get();
    }

    @Override
    protected SoundEvent roarSound() {
        return AllSounds.LILITH_LAUGH.get();
    }

    @Override
    protected SoundEvent transformSound() {
        return AllSounds.LILITH_LIGHT_CHARGE.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AllSounds.LILITH_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.LILITH_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.LILITH_DEATH.get();
    }

    // --- the fight's shape --------------------------------------------------------------------------

    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        return LuciferSummoning.openArena(level, blockPosition(), SNConfig.LILITH_ARENA_RADIUS.get(), ArenaTheme.SEAL);
    }

    @Override
    protected void applyTerrain(ServerLevel level, ArenaController arena, int phase) {
        if (phase >= 3) LilithTerrain.bleach(level, arena, headstones);
    }

    @Override
    protected void playEmergence() {
        LilithCinematics.emergence(this);
    }

    @Override
    protected void playTransition(int to) {
        LilithCinematics.transition(this, to);
    }

    @Override
    protected void playDeath() {
        LilithCinematics.death(this);
    }

    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        LilithCinematics.victory(this);
        contracts.clear();
    }

    @Override
    protected void onTransitionStart(int to) {
        contracts.clear();
        if (level() instanceof ServerLevel level) {
            level.sendParticles(AllParticles.WHITE_LIGHT.get(), getX(), getY() + 1.4, getZ(), 40, 0.4, 0.8, 0.4, 0.1);
            ArenaController arena = arena();
            // Never leave the last phase without cover.
            if (arena != null && headstonesRaised && standing(level) < 2) raiseHeadstones(level, arena, 3, to * 977L);
        }
    }

    @Override
    protected void tickTransitionMotion(int elapsed, boolean last) {
        setDeltaMovement(0, getDeltaMovement().y, 0);
        if (level() instanceof ServerLevel level && elapsed % 4 == 0) {
            level.sendParticles(AllParticles.WHITE_LIGHT.get(), getX(), getY() + 1.5, getZ(), 4, 0.25, 0.4, 0.25, 0.06);
        }
    }

    /** A flash, and she is standing there; halfway through, the headstones rise. */
    @Override
    protected void tickEmergence() {
        setDeltaMovement(Vec3.ZERO);
        int elapsed = LILITH_EMERGE_TICKS - stateTimer;
        if (!(level() instanceof ServerLevel level)) return;
        if (elapsed < 30 && elapsed % 3 == 0) {
            level.sendParticles(AllParticles.WHITE_LIGHT.get(), getX(), getY() + 1, getZ(), 6, 0.4, 0.9, 0.4, 0.03);
        }
        if (elapsed == 2) level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
        ArenaController arena = arena();
        if (elapsed == HEADSTONES_AT && arena != null) raiseAll(level, arena);
    }

    @Override
    protected void clientEmergenceParticles() {
        level().addParticle(AllParticles.WHITE_LIGHT.get(), getRandomX(1.2), getY() + random.nextDouble() * 2, getRandomZ(1.2), 0, 0.03, 0);
    }

    @Override
    protected void tickDyingMotion(int elapsed) {
        setNoGravity(false);
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
    }

    /** Her light gutters and goes out. */
    @Override
    protected void dyingParticles(ServerLevel level, boolean last) {
        if (last) {
            level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1.2, getZ(), 2, 0, 0, 0, 0);
            level.sendParticles(AllParticles.WHITE_LIGHT.get(), getX(), getY() + 1, getZ(), 150, 1.2, 1.2, 1.2, 0.25);
            return;
        }
        level.sendParticles(AllParticles.WHITE_LIGHT.get(), getX(), getY() + 1.4, getZ(), 5, 0.3, 0.5, 0.3, 0.05);
    }

    @Override
    protected Vec3 tetherPoint(ArenaController arena) {
        Vec3 c = arena.centerVec();
        Vec3 out = position().subtract(c).multiply(1, 0, 1);
        if (out.lengthSqr() < 0.01) out = new Vec3(1, 0, 0);
        Vec3 at = c.add(out.normalize().scale(6));
        BlockPos floor = ArenaTerrain.surface((ServerLevel) level(), arena, (int) Math.floor(at.x), (int) Math.floor(at.z));
        return new Vec3(at.x, floor != null ? floor.getY() + 1 : c.y + 1, at.z);
    }

    @Override
    protected void leaveBehind(ServerLevel level, Vec3 at) {
        level.sendParticles(AllParticles.WHITE_LIGHT.get(), getX(), getY() + 1, getZ(), 60, 0.5, 1.0, 0.5, 0.1);
        contracts.clear();
    }

    @Override
    protected void returnToCage(ServerLevel level, String messageKey) {
        super.returnToCage(level, messageKey.replace(".lucifer.", ".lilith."));
    }

    // --- headstones ----------------------------------------------------------------------------------

    private void raiseAll(ServerLevel level, ArenaController arena) {
        headstonesRaised = true;
        raiseHeadstones(level, arena, SNConfig.LILITH_HEADSTONES.get(), getUUID().getLeastSignificantBits());
        level.playSound(null, arena.center(), net.minecraft.sounds.SoundEvents.GRAVEL_BREAK, SoundSource.HOSTILE, 2.5f, 0.6f);
    }

    /** Raises up to {@code count} headstones round the arena's centre, facing it. */
    public void raiseHeadstones(ServerLevel level, ArenaController arena, int count, long seed) {
        BlockPos c = arena.center();
        double turn = (seed & 0xFFFF) / 65536.0 * Math.PI * 2;
        for (LilithHeadstones.Spot s : LilithHeadstones.ring(count, seed, turn)) {
            BlockPos floor = ArenaTerrain.surface(level, arena, c.getX() + s.dx(), c.getZ() + s.dz());
            if (floor == null) continue;
            BlockPos lower = floor.above();
            if (headstones.contains(lower) || !level.getBlockState(lower.above()).getCollisionShape(level, lower.above()).isEmpty()) continue;
            Direction facing = Direction.getNearest(-s.dx(), 0, -s.dz());
            BlockState base = AllBlocks.CRACKED_HEADSTONE.get().defaultBlockState().setValue(HeadstoneBlock.FACING, facing);
            if (arena.mutate(level, lower, base.setValue(HeadstoneBlock.HALF, DoubleBlockHalf.LOWER), 0)
                    && arena.mutate(level, lower.above(), base.setValue(HeadstoneBlock.HALF, DoubleBlockHalf.UPPER), 0)) {
                headstones.add(lower);
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, lower.getX() + 0.5, lower.getY(), lower.getZ() + 0.5, 6, 0.4, 0.1, 0.4, 0.01);
            }
        }
    }

    public int standing(ServerLevel level) {
        headstones.removeIf(p -> !level.getBlockState(p).is(AllBlocks.CRACKED_HEADSTONE.get()));
        return headstones.size();
    }

    /** The white light struck the headstone at {@code pos} (either half): one more crack, or it falls. */
    public void crackHeadstone(ServerLevel level, BlockPos pos) {
        ArenaController arena = arena();
        BlockState hit = level.getBlockState(pos);
        if (arena == null || !hit.is(AllBlocks.CRACKED_HEADSTONE.get())) return;
        BlockPos lower = hit.getValue(HeadstoneBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        BlockState state = level.getBlockState(lower);
        if (!state.is(AllBlocks.CRACKED_HEADSTONE.get())) return;
        int cracks = state.getValue(HeadstoneBlock.CRACKS) + 1;
        if (cracks >= LilithBalance.HEADSTONE_CRACKS) {
            level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, state),
                    lower.getX() + 0.5, lower.getY() + 1, lower.getZ() + 0.5, 40, 0.4, 0.8, 0.4, 0.1);
            level.playSound(null, lower, net.minecraft.sounds.SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 2f, 0.6f);
            arena.revert(level, lower.above());
            arena.revert(level, lower);
            // Reverting puts back what was there; if that was nothing it is air already.
            if (level.getBlockState(lower).is(AllBlocks.CRACKED_HEADSTONE.get())) level.setBlock(lower, Blocks.AIR.defaultBlockState(), 3);
            headstones.remove(lower);
            return;
        }
        arena.mutate(level, lower, state.setValue(HeadstoneBlock.CRACKS, cracks), 0);
        BlockState upper = level.getBlockState(lower.above());
        if (upper.is(AllBlocks.CRACKED_HEADSTONE.get())) arena.mutate(level, lower.above(), upper.setValue(HeadstoneBlock.CRACKS, cracks), 0);
        level.sendParticles(ParticleTypes.CRIT, lower.getX() + 0.5, lower.getY() + 1.2, lower.getZ() + 0.5, 12, 0.3, 0.5, 0.3, 0.1);
    }

    // --- contracts -----------------------------------------------------------------------------------

    /** Marks {@code hunter} with a contract that comes due in this phase's time. */
    public boolean signContract(ServerPlayer hunter) {
        if (!(level() instanceof ServerLevel level)) return false;
        int ticks = LilithBalance.contractSeconds(phase()) * 20;
        if (!contracts.sign(hunter.getUUID(), level.getGameTime(), ticks)) return false;
        hunter.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false, true));
        hunter.displayClientMessage(Component.translatable("message.supernaturalcraft.lilith.contract_signed").withStyle(ChatFormatting.RED), false);
        level.playSound(null, hunter.blockPosition(), AllSounds.CONTRACT_SIGN.get(), SoundSource.HOSTILE, 1.5f, 0.8f);
        return true;
    }

    private void tickContracts(ServerLevel level, ArenaController arena) {
        long now = level.getGameTime();
        for (ContractLedger.Contract c : contracts.all()) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(c.holder);
            if (p == null || !p.isAlive() || p.level() != level || !arena.contains(p.position())) {
                contracts.cancel(c.holder);
                continue;
            }
            if ((c.dueAt - now) % 20 == 0) {
                long s = Math.max(1, (c.dueAt - now) / 20);
                p.displayClientMessage(Component.translatable("message.supernaturalcraft.lilith.contract_due", s).withStyle(ChatFormatting.RED), true);
                TelegraphMarker.ring(level, p.position(), 1.2f, TelegraphMarker.WHITE, 21);
            }
        }
        for (UUID id : contracts.due(now)) {
            if (level.getServer().getPlayerList().getPlayer(id) instanceof ServerPlayer p) houndsComeFor(level, p);
        }
    }

    /** The contract is due: unseen hounds, hunting the one who signed. */
    public void houndsComeFor(ServerLevel level, ServerPlayer p) {
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.lilith.contract_called").withStyle(ChatFormatting.DARK_RED), true);
        level.playSound(null, p.blockPosition(), AllSounds.HELLHOUND_BARK.get(), SoundSource.HOSTILE, 3f, 0.7f);
        int n = LilithBalance.houndsOnDue(phase());
        for (int i = 0; i < n; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            Vec3 at = LuciferAttacks.floorAt(this, p.position().add(Math.cos(a) * 5, 0, Math.sin(a) * 5));
            HellhoundEntity hound = AllEntities.HELLHOUND.get().create(level);
            if (hound == null) continue;
            hound.moveTo(at.x, at.y, at.z, random.nextFloat() * 360, 0);
            hound.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.MOB_SUMMONED, null);
            hound.setTarget(p);
            level.addFreshEntity(hound);
            minions().add(hound.getUUID());
        }
    }

    private void burnContracts(float credit) {
        if (!(level() instanceof ServerLevel level) || credit <= 0) return;
        for (UUID id : contracts.pay(credit, LilithBalance.contractBreak(SNConfig.LILITH_CONTRACT_BREAK_SHARE.get().floatValue(), trueMaxHealth()))) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
            if (p != null) {
                p.removeEffect(MobEffects.GLOWING);
                p.displayClientMessage(Component.translatable("message.supernaturalcraft.lilith.contract_burned").withStyle(ChatFormatting.GOLD), true);
                level.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1.2, p.getZ(), 30, 0.3, 0.5, 0.3, 0.03);
                level.playSound(null, p.blockPosition(), AllSounds.CONTRACT_BURN.get(), SoundSource.HOSTILE, 1.5f, 1.0f);
                ChorusRewards.award(p, "main/no_deal");
            }
            stun(CONTRACT_STUN_TICKS);
        }
    }

    // --- ticking -------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (isRemoved() || !(level() instanceof ServerLevel level)) return;
        ArenaController arena = arena();
        if (arena == null || !arena.isActive()) return;
        // Spawned by egg or command, she never emerged: raise the headstones now.
        if (!headstonesRaised && state() != EMERGING) raiseAll(level, arena);
        if (state() != DYING) tickContracts(level, arena);
        if (isSpent() && tickCount % 3 == 0) {
            level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.6, getZ(), 2, 0.2, 0.2, 0.2, 0.01);
        }
    }

    @Override
    protected float vulnerability(DamageSource source) {
        return LilithBalance.vulnerability(state() == RECOVER, isSpent());
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        Holder<MobEffect> e = effect.getEffect();
        if (e.is(AllMobEffects.TRAPPED.getKey()) || e.is(AllMobEffects.STUNNED.getKey())) return allowHold;
        return super.canBeAffected(effect);
    }

    private void stun(int ticks) {
        scheduler().cancel();
        scheduler().delay(ticks);
        allowHold = true;
        addEffect(new MobEffectInstance(AllMobEffects.STUNNED, ticks, 0, false, true, true));
        allowHold = false;
    }

    // --- damage --------------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        float before = trueHealth();
        boolean hurt = super.hurt(source, amount);
        float dealt = before - trueHealth();
        if (hurt && !isInvulnerablePhase()) {
            if (source.getEntity() instanceof Player) burnContracts(LilithBalance.contractCredit(dealt, Holy.isHoly(source)));
            if (source.is(AllDamageTypes.COLT)) {
                stun(COLT_STUN_TICKS);
                if (level() instanceof ServerLevel level) {
                    level.sendParticles(AllParticles.WHITE_LIGHT.get(), getX(), getY() + 1.5, getZ(), 30, 0.3, 0.5, 0.3, 0.1);
                }
            }
        }
        return hurt;
    }

    @Override
    public boolean onExorcised(float potency) {
        if (isInvulnerablePhase()) return false;
        invulnerableTime = 0;
        return hurt(AllDamageTypes.source(level(), AllDamageTypes.SMITE, null), 10f * potency);
    }

    // --- attacks -------------------------------------------------------------------------------------

    @Override
    public @Nullable Supplier<BossAttack<LuciferEntity>> forcedAttack(LivingEntity target) {
        if (attacksSinceLight >= LilithBalance.whiteLightEvery(phase())) {
            attacksSinceLight = 0;
            return LilithAttacks.WhiteLight::new;
        }
        if (distanceToSqr(target) > 18 * 18 || noSightTicks > 100) {
            noSightTicks = 0;
            return LuciferAttacks.Teleport::new;
        }
        return null;
    }

    @Override
    public void onStage(AttackScheduler.Stage stage, @Nullable BossAttack<LuciferEntity> attack) {
        super.onStage(stage, attack);
        if (stage == AttackScheduler.Stage.IDLE && attack == null) attacksSinceLight++;
    }

    /** Test hook: the next attack will be her white light. */
    public void forceWhiteLightNext() {
        attacksSinceLight = LilithBalance.whiteLightEvery(phase());
    }

    // --- persistence ---------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("HeadstonesRaised", headstonesRaised);
        tag.putLongArray("Headstones", headstones.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putLong("SpentUntil", spentUntil);
        tag.putInt("SinceLight", attacksSinceLight);
        ListTag list = new ListTag();
        for (ContractLedger.Contract c : contracts.all()) {
            CompoundTag ct = new CompoundTag();
            ct.put("Holder", NbtUtils.createUUID(c.holder));
            ct.putLong("Due", c.dueAt);
            ct.putFloat("Paid", c.paid());
            list.add(ct);
        }
        tag.put("Contracts", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        headstonesRaised = tag.getBoolean("HeadstonesRaised");
        headstones.clear();
        for (long l : tag.getLongArray("Headstones")) headstones.add(BlockPos.of(l));
        spentUntil = tag.getLong("SpentUntil");
        attacksSinceLight = tag.getInt("SinceLight");
        contracts.clear();
        for (Tag t : tag.getList("Contracts", Tag.TAG_COMPOUND)) {
            CompoundTag ct = (CompoundTag) t;
            contracts.restore(NbtUtils.loadUUID(ct.get("Holder")), ct.getLong("Due"), ct.getFloat("Paid"));
        }
    }
}
