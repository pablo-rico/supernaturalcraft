package org.papiricoh.supernaturalcraft.entity.boss.uncaged;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
import org.papiricoh.supernaturalcraft.hell.cage.CageController;
import org.papiricoh.supernaturalcraft.hell.cage.CageLayout;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;
import java.util.function.Supplier;

/**
 * Lucifer Uncaged: the archangel himself, out of the Cage, the hardest fight there is. Six phases, a
 * sixth of his health each; ten times Lucifer's health (kept above the vanilla cap by
 * {@link #healthScale()}), twice his strength, and attacks of his own on top of Lucifer's.
 *
 * <pre>EMERGING (down from the Cage) → P1 Prisoner → P2 Hellfire → P3 Cold of the Cage → P4 Legion
 *   → P5 Morning Star → P6 Light-Bringer (in the air) → DYING (the chains drag him back)</pre>
 */
public class LuciferUncagedEntity extends LuciferEntity {

    public static final int MAX_PHASE = 6, UNCAGED_EMERGE_TICKS = 160;
    /** He is let down from the Cage over these ticks of his emergence (after the iris has opened). */
    private static final int DESCENT_START = 45, DESCENT_END = 150;

    private float healthScale = 1f;
    /** Whether he came down out of the Cage (and so goes back up into it when he falls). */
    private boolean fromCage;
    private @Nullable Vec3 deathStart;
    private int attacksSinceNova;

    public LuciferUncagedEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 2000;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.ATTACK_DAMAGE, 16.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    public void setFromCage(boolean fromCage) {
        this.fromCage = fromCage;
    }

    public boolean fromCage() {
        return fromCage;
    }

    // --- the variant's numbers --------------------------------------------------------------------

    @Override
    public int maxPhase() {
        return MAX_PHASE;
    }

    @Override
    protected float threshold(int phase) {
        return UncagedBalance.threshold(phase);
    }

    @Override
    protected float healthScale() {
        return healthScale;
    }

    @Override
    protected float mundaneMultiplier() {
        return SNConfig.UNCAGED_MUNDANE_MULTIPLIER.get().floatValue();
    }

    @Override
    protected float hitCap() {
        return SNConfig.UNCAGED_HIT_CAP.get().floatValue();
    }

    @Override
    public float attackDamageMultiplier() {
        return SNConfig.UNCAGED_DAMAGE_MULTIPLIER.get().floatValue();
    }

    @Override
    protected void scaleHealthToChallengers() {
        int n = Math.max(1, challengers().size());
        double base = Math.min(1024.0, SNConfig.LUCIFER_HEALTH.get());
        healthScale = UncagedBalance.healthScale(SNConfig.UNCAGED_HEALTH_MULTIPLIER.get(), SNConfig.UNCAGED_HEALTH_PER_PLAYER.get(), n);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(base);
        setHealth((float) base);
    }

    /** Spawned by egg or command (no emergence): scale his health on his first tick in the fight. */
    @Override
    protected void customServerAiStep() {
        if (healthScale <= 1f && state() != EMERGING && SNConfig.UNCAGED_HEALTH_MULTIPLIER.get() > 1) scaleHealthToChallengers();
        super.customServerAiStep();
    }

    /** The fight is given up (everyone fled or fell): he goes back into the Cage and leaves the rings. */
    public void abandon() {
        if (level() instanceof ServerLevel server) returnToCage(server, "message.supernaturalcraft.uncaged.victorious");
    }

    /** Test and command hook: sets the true-health multiplier directly (normally fixed when he emerges). */
    public void setHealthScale(float scale) {
        this.healthScale = scale;
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return UncagedAttacks.pool(phase);
    }

    @Override
    protected int baseGap(int phase) {
        return UncagedBalance.attackGap(phase);
    }

    @Override
    public float scale(int phase) {
        return UncagedBalance.scale(phase);
    }

    @Override
    protected int emergeTicks() {
        return UNCAGED_EMERGE_TICKS;
    }

    @Override
    protected int deathTicks() {
        return DEATH_TICKS;
    }

    @Override
    protected String animationPrefix() {
        return "animation.lucifer_uncaged.";
    }

    @Override
    protected List<String> triggeredAnimations() {
        return UncagedAnimations.TRIGGERED;
    }

    @Override
    protected String bossBarKey(int phase) {
        return "entity.supernaturalcraft.lucifer_uncaged.phase" + phase;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return switch (phase) {
            case 1, 2 -> BossEvent.BossBarColor.RED;
            case 3 -> BossEvent.BossBarColor.BLUE;
            case 4 -> BossEvent.BossBarColor.PURPLE;
            case 5 -> BossEvent.BossBarColor.YELLOW;
            default -> BossEvent.BossBarColor.WHITE;
        };
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return switch (phase) {
            case 1 -> AllParticles.DEMON_SMOKE.get();
            case 2 -> AllParticles.HELLFIRE.get();
            case 3 -> AllParticles.FROST.get();
            case 4 -> AllParticles.ASH.get();
            default -> AllParticles.GRACE.get();
        };
    }

    // --- the fight's shape ------------------------------------------------------------------------

    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        return LuciferSummoning.openArena(level, blockPosition(), SNConfig.UNCAGED_ARENA_RADIUS.get(), ArenaTheme.ABYSS);
    }

    @Override
    protected void applyTerrain(ServerLevel level, ArenaController arena, int phase) {
        UncagedTerrain.apply(level, arena, phase);
    }

    @Override
    protected void playEmergence() {
        UncagedCinematics.emergence(this);
    }

    @Override
    protected void playTransition(int to) {
        UncagedCinematics.transition(this, to);
    }

    @Override
    protected void playDeath() {
        UncagedCinematics.death(this);
    }

    @Override
    protected void onTransitionStart(int to) {
        playSound(AllSounds.UNCAGED_CHAINS.get(), 4.0f, 0.6f + to * 0.08f);
        if (to == MAX_PHASE) setNoGravity(true);
    }

    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        UncagedCinematics.victory(this);
        if (fromCage) CageController.get(level).close(level);
        // He is up in the Cage now; the spoils fall on the dais.
        Vec3 c = arena.centerVec();
        moveTo(c.x, c.y + 1, c.z);
    }

    @Override
    protected void leaveBehind(ServerLevel level, Vec3 at) {
        for (var ring : List.of(AllItems.RING_OF_WAR, AllItems.RING_OF_FAMINE, AllItems.RING_OF_PESTILENCE, AllItems.RING_OF_DEATH)) {
            level.addFreshEntity(new ItemEntity(level, at.x, at.y, at.z, new ItemStack(ring.get())));
        }
        if (fromCage) CageController.get(level).close(level);
    }

    /** Let down from the open Cage on his chains, onto the island. */
    @Override
    protected void tickEmergence() {
        if (!fromCage) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        setNoGravity(true);
        int elapsed = UNCAGED_EMERGE_TICKS - stateTimer;
        double top = CageLayout.THRONE.getY(), bottom = CageLayout.ISLAND_Y + 1;
        double t = Math.max(0, Math.min(1, (elapsed - DESCENT_START) / (double) (DESCENT_END - DESCENT_START)));
        double eased = t * t * (3 - 2 * t);
        setPos(getX(), top + (bottom - top) * eased, getZ());
        setDeltaMovement(Vec3.ZERO);
        if (elapsed >= DESCENT_END) setNoGravity(isAerialPhase());
        if (level() instanceof ServerLevel server && elapsed % 3 == 0 && t > 0 && t < 1) {
            server.sendParticles(AllParticles.DEMON_SMOKE.get(), getX(), getY() + 1, getZ(), 6, 0.4, 1.0, 0.4, 0.02);
            server.sendParticles(ParticleTypes.ASH, getX(), getY() + 3, getZ(), 8, 1.5, 2, 1.5, 0.01);
        }
    }

    /** The chains take him back: up through the iris into the Cage, while the light pours out of him. */
    @Override
    protected void tickDyingMotion(int elapsed) {
        if (!fromCage) {
            super.tickDyingMotion(elapsed);
            return;
        }
        setNoGravity(true);
        if (deathStart == null) deathStart = position();
        Vec3 to = Vec3.atBottomCenterOf(CageLayout.THRONE);
        double t = Math.max(0, Math.min(1, (elapsed - 40) / 140.0));
        double eased = t * t * (3 - 2 * t);
        Vec3 at = deathStart.lerp(to, eased);
        setPos(at.x, at.y, at.z);
        setDeltaMovement(Vec3.ZERO);
        if (level() instanceof ServerLevel server && elapsed % 2 == 0) {
            server.sendParticles(AllParticles.HELLFIRE.get(), getX(), getY() + 1.5, getZ(), 4, 0.5, 1, 0.5, 0.05);
            if (elapsed % 20 == 0) playSound(AllSounds.UNCAGED_CHAINS.get(), 3.0f, 0.5f);
        }
    }

    @Override
    public @Nullable Supplier<BossAttack<LuciferEntity>> forcedAttack(LivingEntity target) {
        if (isEnraged() && attacksSinceNova >= 5) {
            attacksSinceNova = 0;
            return UncagedAttacks.Supernova::new;
        }
        if (phase() >= 5 && attacksSinceSmite >= 4) {
            attacksSinceSmite = 0;
            return LuciferAttacks.Smite::new;
        }
        if (!isAerialPhase() && (distanceToSqr(target) > 18 * 18 || noSightTicks > 60)) {
            noSightTicks = 0;
            return LuciferAttacks.Teleport::new;
        }
        return null;
    }

    @Override
    public void onStage(AttackScheduler.Stage stage, @Nullable BossAttack<LuciferEntity> attack) {
        super.onStage(stage, attack);
        if (stage == AttackScheduler.Stage.IDLE && attack == null) attacksSinceNova++;
    }

    @Override
    public void startSeenByPlayer(net.minecraft.server.level.ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (state() == EMERGING && fromCage) {
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.uncaged.free")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
        }
    }

    // --- persistence --------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("HealthScale", healthScale);
        tag.putBoolean("FromCage", fromCage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        healthScale = tag.contains("HealthScale") ? tag.getFloat("HealthScale") : 1f;
        fromCage = tag.getBoolean("FromCage");
        super.readAdditionalSaveData(tag);
    }

    public static BlockPos cageSpot() {
        return CageLayout.THRONE;
    }
}
