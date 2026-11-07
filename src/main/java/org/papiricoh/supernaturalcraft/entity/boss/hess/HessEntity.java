package org.papiricoh.supernaturalcraft.entity.boss.hess;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.british.British;
import org.papiricoh.supernaturalcraft.british.BritishFxPayload;
import org.papiricoh.supernaturalcraft.british.BritishOrder;
import org.papiricoh.supernaturalcraft.british.BritishRegistry;
import org.papiricoh.supernaturalcraft.british.BritishSpoils;
import org.papiricoh.supernaturalcraft.british.agent.BritishAgentEntity;
import org.papiricoh.supernaturalcraft.british.compound.CompoundAnchors;
import org.papiricoh.supernaturalcraft.british.site.BritishSites;
import org.papiricoh.supernaturalcraft.british.site.PlacedSite;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.legacy.Legacies;
import org.papiricoh.supernaturalcraft.legacy.research.FieldNotesItem;
import org.papiricoh.supernaturalcraft.legacy.research.NoteLedger;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dr. Mildred Hess (v0.41, season 12): the head of the British Men of Letters, fought in the war room of their compound
 * ({@link HessSummoning#begin}). Wand bolts, hex bags, her agents, a ward that turns blows aside and the folio's curse: see
 * {@link HessBalance}, {@link HessAttacks}. Beaten, she leaves every hunter her bust, her folio and the Order's arsenal; one of
 * the American Men of Letters also carries off a few of London's files.
 */
public class HessEntity extends LuciferEntity {

    /** Archive notes a member of the Men of Letters files from London's war room (once). */
    public static final int LETTERS_NOTES = 3;

    private @Nullable BlockPos compound;
    private int compoundQ;
    private int wardTicks;

    public HessEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 400;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, VANILLA_BASE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    // --- set-up -------------------------------------------------------------------------------------------------------------

    /** At the compound: her agents come in at its doors. */
    public void bindCompound(BlockPos origin, int q) {
        compound = origin.immutable();
        compoundQ = q;
    }

    public @Nullable PlacedSite site() {
        return compound == null ? null : new PlacedSite(BritishSites.COMPOUND, compound, compoundQ);
    }

    /** Where her agents come in. */
    public List<Vec3> agentSpots() {
        PlacedSite s = site();
        if (s != null) {
            List<Vec3> out = s.series(CompoundAnchors.AGENT_PREFIX);
            if (!out.isEmpty()) return out;
        }
        List<Vec3> out = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 2 * i + Math.PI / 4;
            out.add(position().add(Math.cos(a) * 8, 0, Math.sin(a) * 8));
        }
        return out;
    }

    /** Her agents standing. */
    public int agents() {
        if (!(level() instanceof ServerLevel level)) return 0;
        int n = 0;
        for (UUID id : minions()) if (level.getEntity(id) instanceof BritishAgentEntity a && a.isAlive()) n++;
        return n;
    }

    /** One of her agents comes in at {@code at}. */
    public @Nullable BritishAgentEntity callAgent(ServerLevel level, Vec3 at) {
        BritishAgentEntity a = BritishRegistry.BRITISH_AGENT.get().create(level);
        if (a == null) return null;
        a.moveTo(at.x, at.y, at.z, getRandom().nextFloat() * 360, 0);
        a.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.MOB_SUMMONED, null);
        a.setPersistenceRequired();
        level.addFreshEntity(a);
        minions().add(a.getUUID());
        level.sendParticles(ParticleTypes.POOF, at.x, at.y + 1, at.z, 12, 0.3, 0.6, 0.3, 0.02);
        return a;
    }

    /** Test and command hook: the long death begins now. */
    public void defeatNow() {
        beginDying();
    }

    /** Whether her ward holds now. */
    public boolean warded() {
        return wardTicks > 0;
    }

    public void raiseWard(int ticks) {
        wardTicks = ticks;
        if (level() instanceof ServerLevel level) {
            BritishFxPayload.send(level, getId(), BritishFxPayload.WARD, Math.round(HessBalance.WARD_RADIUS * 10), 0, position(), ticks);
        }
    }

    /** A line of hers to the hunters near. */
    public void tell(String key) {
        if (!(level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 48 * 48) p.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC), false);
        }
    }

    void fx(byte kind, int arg, int arg2, Vec3 at, int duration) {
        if (level() instanceof ServerLevel level) BritishFxPayload.send(level, getId(), kind, arg, arg2, at, duration);
    }

    // --- her numbers --------------------------------------------------------------------------------------------------------

    @Override
    public int maxPhase() {
        return HessBalance.PHASES;
    }

    @Override
    protected float threshold(int phase) {
        return HessBalance.threshold(phase);
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
        return HessBalance.EMERGE;
    }

    @Override
    protected int deathTicks() {
        return HessBalance.DEATH;
    }

    @Override
    protected int transitionTicks(int to) {
        return HessBalance.TRANSITION;
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return HessAttacks.pool(phase);
    }

    @Override
    protected int baseGap(int phase) {
        return HessBalance.attackGap(phase);
    }

    @Override
    public @Nullable java.util.function.Supplier<org.papiricoh.supernaturalcraft.entity.boss.BossAttack<LuciferEntity>> forcedAttack(
            net.minecraft.world.entity.LivingEntity target) {
        return null;
    }

    @Override
    protected float vulnerability(DamageSource source) {
        return super.vulnerability(source) * HessBalance.taken(warded());
    }

    @Override
    protected String animationPrefix() {
        return "animation." + HessAssets.RIG + ".";
    }

    @Override
    protected List<String> triggeredAnimations() {
        return HessAssets.TRIGGERED;
    }

    @Override
    protected String bossBarKey(int phase) {
        return "entity.supernaturalcraft.hess.bar.phase" + phase;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return phase >= HessBalance.PHASES ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.BLUE;
    }

    @Override
    protected Component bossBarName(int phase) {
        return Component.translatable(bossBarKey(phase)).withStyle(phase >= HessBalance.PHASES ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_AQUA);
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return ParticleTypes.ENCHANT;
    }

    @Override
    protected void clientPhaseParticles(int phase) {
        if (random.nextFloat() < 0.2f) level().addParticle(ParticleTypes.ENCHANT, getRandomX(0.8), getY() + 1.4, getRandomZ(0.8), 0, 0.4, 0);
    }

    @Override
    protected SoundEvent emergeSound() {
        return BritishRegistry.sound("hess.command");
    }

    @Override
    protected SoundEvent roarSound() {
        return BritishRegistry.sound("hess.cast");
    }

    @Override
    protected SoundEvent transformSound() {
        return BritishRegistry.sound("hess.ward");
    }

    @Override
    protected SoundEvent ambientBossSound() {
        return BritishRegistry.sound("hess.hex");
    }

    @Override
    protected SoundEvent dyingSound() {
        return BritishRegistry.sound("hess.death");
    }

    @Override
    protected SoundEvent deflectSound() {
        return BritishRegistry.sound("hess.ward");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return BritishRegistry.sound("hess.hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return BritishRegistry.sound("hess.death");
    }

    @Override
    protected void clientEmergenceParticles() {
        level().addParticle(ParticleTypes.ENCHANT, getRandomX(0.8), getY() + random.nextDouble() * 1.8, getRandomZ(0.8), 0, 0.3, 0);
    }

    @Override
    protected void dyingParticles(ServerLevel level, boolean last) {
        if (last) {
            level.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 1, getZ(), 80, 0.6, 1.0, 0.6, 1.0);
            return;
        }
        level.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 1.2, getZ(), 3, 0.3, 0.4, 0.3, 0.4);
    }

    // --- the fight's shape --------------------------------------------------------------------------------------------------

    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        return LuciferSummoning.openArena(level, blockPosition(), HessSummoning.FREE_RADIUS, ArenaTheme.BRITISH_HQ);
    }

    @Override
    protected void applyTerrain(ServerLevel level, ArenaController arena, int phase) {
    }

    @Override
    protected void playEmergence() {
        fx(BritishFxPayload.TITLE, BritishFxPayload.WHO_HESS, 1, position(), 70);
        tell("message.supernaturalcraft.hess.intro");
    }

    @Override
    protected void playTransition(int to) {
        fx(BritishFxPayload.TITLE, BritishFxPayload.WHO_HESS, to, position(), 60);
        triggerAnim("action", to >= HessBalance.PHASES ? "folio_read" : "ward_raise");
        tell("message.supernaturalcraft.hess.phase" + to);
    }

    @Override
    protected void playDeath() {
        tell("message.supernaturalcraft.hess.dying");
    }

    @Override
    protected void onTransitionStart(int to) {
    }

    @Override
    protected void tickTransitionMotion(int elapsed, boolean last) {
        setDeltaMovement(0, getDeltaMovement().y, 0);
    }

    @Override
    protected void tickDyingMotion(int elapsed) {
        setDeltaMovement(0, getDeltaMovement().y, 0);
    }

    @Override
    protected void leaveBehind(ServerLevel level, Vec3 at) {
    }

    @Override
    protected void returnToCage(ServerLevel level, String messageKey) {
        super.returnToCage(level, "message.supernaturalcraft.hess.left");
    }

    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        Vec3 at = position();
        BritishSpoils.share(level, this, at, new ItemStack(BritishRegistry.HESS_TROPHY_ITEM.get()), new ItemStack(BritishRegistry.HESS_FOLIO.get()),
                new ItemStack(BritishRegistry.ENCHANTED_ROUND.get(), 24), new ItemStack(BritishRegistry.HEX_GRENADE.get(), 4));
        for (ServerPlayer p : BritishSpoils.fighters(level, this)) {
            BritishOrder.update(p, b -> b.with(British.HESS_BEATEN));
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.hess.defeated").withStyle(ChatFormatting.GOLD), false);
            lettersBonus(p);
        }
        fx(BritishFxPayload.TITLE, BritishFxPayload.WHO_HESS, 0, at, 90);
    }

    /** One of the American Men of Letters carries off a few of London's files (once). */
    public static boolean lettersBonus(ServerPlayer p) {
        if (!Legacies.member(p) || BritishOrder.get(p).has(British.LETTERS_BONUS)) return false;
        BritishOrder.update(p, b -> b.with(British.LETTERS_BONUS));
        NoteLedger.file(p, FieldNotesItem.ARCANE, LETTERS_NOTES);
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.hess.files").withStyle(ChatFormatting.DARK_AQUA), false);
        return true;
    }

    // --- per tick -----------------------------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (wardTicks > 0) wardTicks--;
    }

    // --- animation ----------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = animationPrefix();
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + "idle"), walk = RawAnimation.begin().thenLoop(pre + "walk");
        controllers.add(new AnimationController<>(this, "base", 5, state -> {
            if (state() == DYING) return PlayState.STOP;
            return state.setAndContinue(state.isMoving() ? walk : idle);
        }));
        AnimationController<LuciferEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String name : triggeredAnimations()) {
            action.triggerableAnim(name, name.equals("death")
                    ? RawAnimation.begin().thenPlayAndHold(pre + name)
                    : RawAnimation.begin().thenPlay(pre + name));
        }
        controllers.add(action);
    }

    // --- persistence --------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (compound != null) {
            tag.put("Compound", NbtUtils.writeBlockPos(compound));
            tag.putInt("CompoundQ", compoundQ);
        }
        tag.putInt("Ward", wardTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        compound = NbtUtils.readBlockPos(tag, "Compound").orElse(null);
        compoundQ = tag.getInt("CompoundQ");
        wardTicks = tag.getInt("Ward");
    }
}
