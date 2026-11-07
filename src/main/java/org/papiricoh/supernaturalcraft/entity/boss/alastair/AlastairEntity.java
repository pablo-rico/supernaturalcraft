package org.papiricoh.supernaturalcraft.entity.boss.alastair;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
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
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.apocalypse.warehouse.WarehouseSite;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.network.AlastairFxPayload;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.weapon.Holy;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Alastair, Hell's torturer (v0.40, season 4): the boss of the warehouse. He starts {@link #bound()} in the angels' devil's
 * trap, kneeling in chains with his guards round him; only holy things hurt him much then ({@link AlastairBalance#BOUND_MUNDANE}).
 * At two thirds he breaks free (the trap is scuffed out, the angels die) and fights with his instruments; at a third the white
 * smoke comes out of him, the rack rises and his demons climb out of the floor. See {@link AlastairBalance}, {@link AlastairAttacks}.
 */
public class AlastairEntity extends LuciferEntity {

    private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(AlastairEntity.class, EntityDataSerializers.BYTE);
    public static final int BOUND = 1, EYES = 2;
    /** How long breaking free (and the move into the last phase) lasts. */
    public static final int TRANSITION = 50;
    private static final int TAUNTS = 6;

    private @Nullable BlockPos warehouse;
    private int warehouseQ;
    private boolean emerged;
    private final List<UUID> guards = new ArrayList<>();
    private int taunt;

    public AlastairEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 400;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, VANILLA_BASE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
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

    /** Chained in the trap (phase I): kneels, does not move. */
    public boolean bound() {
        return flag(BOUND);
    }

    /** His white eyes show (the renderer's glow). */
    public boolean eyesLit() {
        return flag(EYES);
    }

    // --- set-up -------------------------------------------------------------------------------------------------------------

    /** In his warehouse: the trap he is chained in and his demons' doors are its. */
    public void bindWarehouse(BlockPos origin, int q) {
        warehouse = origin.immutable();
        warehouseQ = q;
    }

    public @Nullable WarehouseSite site() {
        return warehouse == null ? null : new WarehouseSite(warehouse, warehouseQ);
    }

    /** Chained (the first fight at the warehouse) or not (a rematch, a command). */
    public void chain(boolean on) {
        setFlag(BOUND, on);
    }

    /** The angels holding him: they die when he breaks free. */
    public void guardedBy(List<? extends Entity> angels) {
        for (Entity e : angels) guards.add(e.getUUID());
    }

    public List<Vec3> spawnPoints() {
        WarehouseSite s = site();
        if (s != null) return s.spawns();
        List<Vec3> out = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 2 * i;
            out.add(position().add(Math.cos(a) * 8, 0, Math.sin(a) * 8));
        }
        return out;
    }

    /** His demons standing. */
    public int demons() {
        if (!(level() instanceof ServerLevel level)) return 0;
        int n = 0;
        for (UUID id : minions()) if (level.getEntity(id) instanceof Monster m && m.isAlive()) n++;
        return n;
    }

    public void fx(byte kind, int arg, int arg2, Vec3 at, int duration) {
        if (!(level() instanceof ServerLevel level)) return;
        AlastairFxPayload p = new AlastairFxPayload(getId(), kind, arg, arg2, at, duration);
        for (ServerPlayer pl : level.players()) if (pl.distanceToSqr(this) < 96 * 96) Send.toPlayer(pl, p);
    }

    /** One of his lines to the hunters near (they come round in order). */
    public void taunt() {
        if (!(level() instanceof ServerLevel level)) return;
        String key = "message.supernaturalcraft.alastair.taunt." + (taunt++ % TAUNTS);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 48 * 48) p.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
        }
        playSound(AllSounds.apocalypse("alastair.laugh"), 1.5f, 1f);
    }

    // --- his numbers --------------------------------------------------------------------------------------------------------

    @Override
    public int maxPhase() {
        return AlastairBalance.PHASES;
    }

    @Override
    protected float threshold(int phase) {
        return AlastairBalance.threshold(phase);
    }

    @Override
    protected double healthPerExtraPlayer() {
        return SNConfig.CROWLEY_HEALTH_PER_PLAYER.get();
    }

    @Override
    protected float mundaneMultiplier() {
        return SNConfig.CROWLEY_MUNDANE_MULTIPLIER.get().floatValue();
    }

    @Override
    protected float damageFactor() {
        return SNConfig.CROWLEY_DAMAGE_FACTOR.get().floatValue();
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
        return 40;
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return AlastairAttacks.pool(phase);
    }

    @Override
    protected int baseGap(int phase) {
        return AlastairBalance.attackGap(phase);
    }

    @Override
    protected String animationPrefix() {
        return "animation." + AlastairAssets.RIG + ".";
    }

    @Override
    protected List<String> triggeredAnimations() {
        return AlastairAssets.TRIGGERED;
    }

    @Override
    protected String bossBarKey(int phase) {
        return AlastairAssets.BAR_PREFIX + ".phase" + phase;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return phase >= AlastairBalance.PHASES ? BossEvent.BossBarColor.WHITE : BossEvent.BossBarColor.RED;
    }

    @Override
    protected Component bossBarName(int phase) {
        return Component.translatable(bossBarKey(phase)).withStyle(phase >= AlastairBalance.PHASES ? ChatFormatting.WHITE : ChatFormatting.DARK_RED);
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return phase >= AlastairBalance.PHASES ? ParticleTypes.CLOUD : AllParticles.DEMON_SMOKE.get();
    }

    @Override
    protected SoundEvent emergeSound() {
        return AllSounds.apocalypse("alastair.chains");
    }

    @Override
    protected SoundEvent roarSound() {
        return AllSounds.apocalypse("alastair.laugh");
    }

    @Override
    protected SoundEvent transformSound() {
        return AllSounds.apocalypse("alastair.chain_break");
    }

    @Override
    protected SoundEvent ambientBossSound() {
        return AllSounds.apocalypse("alastair.hum");
    }

    @Override
    protected SoundEvent dyingSound() {
        return AllSounds.apocalypse("alastair.death");
    }

    @Override
    protected SoundEvent deflectSound() {
        return AllSounds.apocalypse("alastair.hurt");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.apocalypse("alastair.hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.apocalypse("alastair.death");
    }

    @Override
    protected void clientEmergenceParticles() {
        level().addParticle(ParticleTypes.SMOKE, getRandomX(0.6), getY() + random.nextDouble() * 2, getRandomZ(0.6), 0, 0.02, 0);
    }

    @Override
    protected void dyingParticles(ServerLevel level, boolean last) {
        if (last) {
            level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 80, 0.5, 1.0, 0.5, 0.08);
            return;
        }
        level.sendParticles(ParticleTypes.CLOUD, getX(), getEyeY(), getZ(), 3, 0.15, 0.1, 0.15, 0.02);
    }

    // --- the fight's shape --------------------------------------------------------------------------------------------------

    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        return LuciferSummoning.openArena(level, blockPosition(), AlastairSummoning.FREE_RADIUS, ArenaTheme.WAREHOUSE);
    }

    @Override
    protected void applyTerrain(ServerLevel level, ArenaController arena, int phase) {
        if (phase != 2 || !bound()) return;
        breakFree(level, arena);
    }

    /** The trap is scuffed out, the chains snap, the angels holding him die. */
    private void breakFree(ServerLevel level, ArenaController arena) {
        setFlag(BOUND, false);
        BlockPos c = blockPosition();
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 0; dy++) {
                    BlockPos p = c.offset(dx, dy, dz);
                    if (level.getBlockState(p).is(AllBlocks.DEVILS_TRAP.get())) arena.mutate(level, p, Blocks.AIR.defaultBlockState(), 0);
                }
            }
        for (UUID id : guards) {
            Entity e = level.getEntity(id);
            if (e == null) continue;
            level.sendParticles(ParticleTypes.FLASH, e.getX(), e.getEyeY(), e.getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(AllParticles.GRACE.get(), e.getX(), e.getEyeY(), e.getZ(), 30, 0.3, 0.6, 0.3, 0.1);
            if (level.getBlockState(e.blockPosition()).canBeReplaced()) arena.mutate(level, e.blockPosition(), AllBlocks.WING_SCORCH.get().defaultBlockState(), 0);
            e.discard();
        }
        guards.clear();
        fx(AlastairFxPayload.FREE, 0, 0, position(), 30);
        playSound(AllSounds.apocalypse("alastair.chain_break"), 3f, 0.8f);
    }

    @Override
    protected void playEmergence() {
        fx(AlastairFxPayload.TITLE, 1, 0, position(), 70);
    }

    @Override
    protected void playTransition(int to) {
        fx(AlastairFxPayload.TITLE, to, 0, position(), 70);
        triggerAnim("action", to == 2 ? "break_free" : "smoke");
    }

    @Override
    protected void playDeath() {
    }

    @Override
    protected int transitionTicks(int to) {
        return TRANSITION;
    }

    @Override
    protected void onTransitionStart(int to) {
        setFlag(EYES, to >= AlastairBalance.PHASES);
    }

    @Override
    protected void tickTransitionMotion(int elapsed, boolean last) {
        setDeltaMovement(0, getDeltaMovement().y, 0);
    }

    @Override
    protected boolean walks() {
        return !bound();
    }

    @Override
    protected void leaveBehind(ServerLevel level, Vec3 at) {
    }

    @Override
    protected void returnToCage(ServerLevel level, String messageKey) {
        super.returnToCage(level, "message.supernaturalcraft.alastair.left");
    }

    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        Vec3 at = position();
        level.addFreshEntity(new ItemEntity(level, at.x, at.y + 0.5, at.z, new ItemStack(AllItems.ALASTAIR_SCALPEL.get())));
        level.addFreshEntity(new ItemEntity(level, at.x, at.y + 0.5, at.z, new ItemStack(AllItems.ALASTAIR_TROPHY.get())));
        for (ServerPlayer p : challengers()) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.alastair.defeated").withStyle(ChatFormatting.GOLD), false);
        }
        fx(AlastairFxPayload.TITLE, 0, 1, at, 90);
    }

    // --- damage -------------------------------------------------------------------------------------------------------------

    @Override
    protected float vulnerability(DamageSource source) {
        float v = super.vulnerability(source);
        if (bound() && !Holy.isHoly(source)) v *= AlastairBalance.BOUND_MUNDANE;
        return v;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && bound() && Holy.isHoly(source)) triggerAnim("action", "hurt_holy");
        return hurt;
    }

    // --- per tick -----------------------------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        if (bound()) {
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
        }
        super.customServerAiStep();
        if (isRemoved() || !(level() instanceof ServerLevel)) return;
        setFlag(EYES, phase() >= AlastairBalance.PHASES || scheduler().current() != null && !bound());
        if (state() == EMERGING || emerged) return;
        emerged = true;
        if (bound()) taunt();
    }

    // --- animation ----------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = animationPrefix();
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + "idle"), walk = RawAnimation.begin().thenLoop(pre + "walk"),
                bound = RawAnimation.begin().thenLoop(pre + "bound_idle"), hum = RawAnimation.begin().thenLoop(pre + "hum_idle");
        controllers.add(new AnimationController<>(this, "base", 5, state -> {
            if (state() == DYING) return PlayState.STOP;
            if (bound()) return state.setAndContinue(bound);
            if (state.isMoving()) return state.setAndContinue(walk);
            return state.setAndContinue(scheduler().current() == null ? hum : idle);
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
        tag.putBoolean("Emerged", emerged);
        tag.putByte("AlastairFlags", entityData.get(FLAGS));
        if (warehouse != null) {
            tag.put("Warehouse", NbtUtils.writeBlockPos(warehouse));
            tag.putInt("WarehouseQ", warehouseQ);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        emerged = tag.getBoolean("Emerged");
        if (tag.contains("AlastairFlags")) entityData.set(FLAGS, tag.getByte("AlastairFlags"));
        warehouse = NbtUtils.readBlockPos(tag, "Warehouse").orElse(null);
        warehouseQ = tag.getInt("WarehouseQ");
    }
}
