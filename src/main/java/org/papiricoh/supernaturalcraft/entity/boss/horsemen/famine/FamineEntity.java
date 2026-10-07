package org.papiricoh.supernaturalcraft.entity.boss.horsemen.famine;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemanEntity;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemanKind;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemanSteedEntity;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.HorsemenLayouts;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.List;
import java.util.UUID;

/**
 * Famine, the Black Rider: an old man in a wheelchair who is always hungry. Three phases on dead farmland:
 *
 * <pre>P1 in his wheelchair (his hunger drains yours; food eaten near him feeds him) → P2 he stands and devours souls
 *   (starving thralls shuffle in to be eaten, and he feeds on whatever lives near him) → P3 on the black horse he
 *   feeds on you: he lifts a hunter and drains them until the others hurt him enough (alone: until you are nearly
 *   empty) → DYING</pre>
 */
public class FamineEntity extends HorsemanEntity {

    /** His hunger reaches this far. */
    public static final double AURA = 10;
    /** Food eaten this close to him feeds him instead. */
    public static final double FEED_RANGE = 12;
    /** True health he gains per point of food eaten near him, and per soul he devours. */
    public static final float HEAL_PER_FOOD = 5f, HEAL_PER_SOUL = 30f;
    /** Damage the others must deal (true health) to break his grip; alone, he lets go at this much health. */
    public static final float GRAB_BREAK = 40f, SOLO_RELEASE_HEALTH = 6f;
    public static final int GRAB_MAX_TICKS = 160;

    private @Nullable UUID grabbed;
    private @Nullable ServerPlayer held;
    private float grabPaid;
    private int grabTicks;

    public FamineEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public HorsemanKind kind() {
        return HorsemanKind.FAMINE;
    }

    @Override
    protected List<ArenaCell> groundPlan(int radius, long seed) {
        return HorsemenLayouts.deadFarm(radius, seed);
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return FamineAttacks.pool(phase);
    }

    /** In his wheelchair he does not walk after you. */
    @Override
    protected boolean walks() {
        return phase() >= 2;
    }

    @Override
    protected String movingClip() {
        return seated() ? "wheel_roll" : "walk";
    }

    /** In his wheelchair: the first phase, and while he stands up out of it. */
    public boolean seated() {
        return !isMounted() && (phase() == 1 || phase() == 2 && state() == TRANSITION);
    }

    @Override
    protected String stillClip() {
        return phase() == 1 && !isMounted() ? "wheel_idle" : "idle";
    }

    // --- feeding ---------------------------------------------------------------------------------------

    private void feed(float trueAmount) {
        if (isInvulnerablePhase() && state() != EMERGING) return;
        // Never back over the floor of the phase before: he can't undo a change of phase.
        float cap = phase() > 1 ? getMaxHealth() * threshold(phase() - 1) : getMaxHealth();
        setHealth(Math.min(cap, getHealth() + trueAmount / healthScale()));
    }

    /** Somebody ate {@code nutrition} worth of food near him: it feeds him instead. */
    public void fedBy(Player eater, int nutrition) {
        feed(nutrition * HEAL_PER_FOOD);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(AllParticles.SOUL_WISP.get(), eater.getX(), eater.getY() + 1.2, eater.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
            level.playSound(null, blockPosition(), AllSounds.FAMINE_DEVOUR.get(), SoundSource.HOSTILE, 1.5f, 1.2f);
        }
        eater.displayClientMessage(Component.translatable("message.supernaturalcraft.famine.fed").withStyle(ChatFormatting.GOLD), true);
    }

    /** A thrall reached him: he drinks its soul. */
    public void devour(HungryThrallEntity thrall) {
        if (!(level() instanceof ServerLevel level) || !thrall.isAlive()) return;
        feed(HEAL_PER_SOUL);
        triggerAnim("action", "devour");
        soulBeam(level, thrall.position().add(0, 1, 0));
        level.playSound(null, blockPosition(), AllSounds.FAMINE_DEVOUR.get(), SoundSource.HOSTILE, 2f, 0.9f);
        for (ServerPlayer p : challengers()) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.famine.devoured").withStyle(ChatFormatting.DARK_RED), true);
        }
        thrall.discard();
    }

    void soulBeam(ServerLevel level, Vec3 from) {
        Vec3 to = position().add(0, 1.4, 0);
        Vec3 d = to.subtract(from);
        int n = (int) Math.max(4, d.length() * 3);
        for (int i = 0; i <= n; i++) {
            Vec3 p = from.add(d.scale(i / (double) n));
            level.sendParticles(AllParticles.SOUL_WISP.get(), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
        }
    }

    // --- the grab --------------------------------------------------------------------------------------

    public @Nullable UUID grabbed() {
        return grabbed;
    }

    public boolean grab(ServerPlayer p) {
        if (grabbed != null) return false;
        grabbed = p.getUUID();
        held = p;
        grabPaid = 0;
        grabTicks = 0;
        triggerAnim("action", isMounted() ? "mounted_grab" : "grab");
        boolean alone = challengers().size() <= 1;
        p.displayClientMessage(Component.translatable(alone ? "message.supernaturalcraft.famine.grabbed_solo"
                : "message.supernaturalcraft.famine.grabbed").withStyle(ChatFormatting.DARK_RED), true);
        return true;
    }

    public void release() {
        if (grabbed == null) return;
        if (held != null) {
            ServerPlayer p = held;
            p.setNoGravity(false);
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.famine.released").withStyle(ChatFormatting.GOLD), true);
            Vec3 away = p.position().subtract(position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 0.01) away = new Vec3(1, 0, 0);
            away = away.normalize();
            p.setDeltaMovement(away.x * 0.8, 0.3, away.z * 0.8);
            p.hurtMarked = true;
        }
        grabbed = null;
        held = null;
        grabPaid = 0;
    }

    private void tickGrab(ServerLevel level) {
        if (grabbed == null) return;
        ServerPlayer p = held;
        if (p == null || !p.isAlive() || p.isRemoved() || state() == TRANSITION || ++grabTicks > GRAB_MAX_TICKS) {
            release();
            return;
        }
        // Held up in front of him, a little off the ground.
        Vec3 front = position().add(net.minecraft.world.phys.Vec3.directionFromRotation(0, getYRot()).scale(1.3)).add(0, isMounted() ? 2.4 : 1.4, 0);
        p.teleportTo(front.x, front.y, front.z);
        p.setDeltaMovement(Vec3.ZERO);
        p.hurtMarked = true;
        if (grabTicks % 10 == 0) {
            p.hurt(AllDamageTypes.source(level, AllDamageTypes.STARVED, this), 1.5f * attackDamageMultiplier());
            p.getFoodData().setFoodLevel(Math.max(0, p.getFoodData().getFoodLevel() - 1));
            p.getFoodData().setSaturation(0);
            feed(3);
            soulBeam(level, p.position().add(0, 1, 0));
        }
        boolean alone = challengers().size() <= 1;
        if (alone && p.getHealth() <= SOLO_RELEASE_HEALTH) release();
    }

    /** Damage from anyone but the one he holds pays down his grip. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        float before = trueHealth();
        boolean hurt = super.hurt(source, amount);
        float dealt = before - trueHealth();
        if (hurt && grabbed != null && source.getEntity() instanceof Player p && !p.getUUID().equals(grabbed)) {
            grabPaid += dealt;
            if (grabPaid >= GRAB_BREAK) release();
        }
        return hurt;
    }

    // --- ticking ---------------------------------------------------------------------------------------

    @Override
    protected void tickFight(ServerLevel level, ArenaController arena) {
        tickGrab(level);
        if (state() == EMERGING) return;
        // His hunger drains yours.
        if (tickCount % 20 == 0) {
            for (ServerPlayer p : challengers()) {
                if (p.distanceToSqr(this) > AURA * AURA) continue;
                p.getFoodData().addExhaustion(phase() == 1 ? 5f : 3f);
                if (tickCount % 60 == 0) level.sendParticles(AllParticles.SOUL_WISP.get(), p.getX(), p.getY() + 1, p.getZ(), 3, 0.2, 0.4, 0.2, 0.02);
            }
            if (tickCount % 80 == 0) level.playSound(null, blockPosition(), AllSounds.FAMINE_HUNGER.get(), SoundSource.HOSTILE, 1.5f, 1f);
        }
        // From his second phase he feeds on whatever lives near him.
        if (phase() >= 2 && tickCount % 40 == 0) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(6),
                    e -> e != this && e.isAlive() && !(e instanceof Player) && !(e instanceof HorsemanSteedEntity)
                            && !e.getType().is(AllTags.Entities.BOSSES) && !(e instanceof net.minecraft.world.entity.decoration.ArmorStand))) {
                if (e instanceof HungryThrallEntity thrall) {
                    devour(thrall);
                    continue;
                }
                if (e.hurt(AllDamageTypes.source(level, AllDamageTypes.STARVED, this), 4)) {
                    feed(4);
                    soulBeam(level, e.position().add(0, e.getBbHeight() * 0.6, 0));
                }
            }
        }
    }

    @Override
    protected void onTransitionStart(int to) {
        release();
        super.onTransitionStart(to);
        if (to == 2) triggerAnim("action", "stand_up");
    }

    @Override
    public void remove(RemovalReason reason) {
        release();
        super.remove(reason);
    }

    @Override
    protected SoundEvent ambientSound() {
        return AllSounds.FAMINE_AMBIENT.get();
    }

    @Override
    protected SoundEvent hurtSound() {
        return AllSounds.FAMINE_HURT.get();
    }

    @Override
    protected SoundEvent deathSound() {
        return AllSounds.FAMINE_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        grabbed = null;
    }
}
