package org.papiricoh.supernaturalcraft.entity.witch;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.bowl.witch.CurseEffect;
import org.papiricoh.supernaturalcraft.bowl.witch.NameMagic;
import org.papiricoh.supernaturalcraft.bowl.spell.Taglock;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * What every witch of the coven shares (v0.21): she keeps her distance and works witchcraft through the magic's own pipeline
 * ({@link WitchMagic}): sigil bolts, curse bags slipped into a pocket, a sip of her brew when hurt, a smoke teleport when
 * cornered, and — if her familiar has stolen someone's taglock — curses laid on its owner from afar. Out of a fight a Hedge
 * Witch hexes villagers into her thralls. Witches, familiars and hexed victims are allies; one hurt alerts her sisters.
 *
 * <p>Actions play a clip on the {@code action} controller ({@link WitchAssets#CLIPS}) and land at the clip's
 * {@link WitchAssets#HIT_TICKS} moment.
 */
public abstract class WitchEntity extends Monster implements GeoEntity {

    /** She keeps between these distances from her target (blocks). */
    public static final double NEAR = 6, FAR = 14;
    /** How far her sisters hear her when she is hurt. */
    public static final double ALERT_RANGE = 16;
    /** How far her familiar may be and still strengthen her; how much stronger she is with it. */
    public static final double FAMILIAR_RANGE = 32, FAMILIAR_POWER = 1.25;
    /** Ticks between her curses on whoever her familiar robbed. */
    public static final int TAGLOCK_CURSE_EVERY = 600;
    public static final int TAGLOCK_CURSE_TICKS = 1200;

    /** The actions a witch can take; each plays its clip. */
    public enum Act {
        BOLT("cast"), BAG("cast"), HEAL("cast"), TELEPORT("teleport"), HEX("hex"), HEX_VILLAGER("hex"), SHIELD("shield");

        public final String clip;

        Act(String clip) {
            this.clip = clip;
        }
    }

    /**
     * A spell is being wound up or cast now (synced, v0.21: the client draws her casting from it): set while an act other
     * than a teleport is pending, and by a subclass's own windups ({@link #setCasting}).
     */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> CASTING = net.minecraft.network.syncher.SynchedEntityData
            .defineId(WitchEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable Act pending;
    private int pendingTicks;
    private @Nullable UUID pendingTarget;
    private Vec3 pendingAt = Vec3.ZERO;
    protected int boltCooldown = 40, bagCooldown = 200, healCooldown = 0, teleportCooldown = 0, villagerCooldown = 600, taglockCooldown = 200;
    private @Nullable UUID familiarId;

    protected WitchEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 12;
    }

    /** The rig and clip prefix ({@link WitchAssets#HEDGE_WITCH}, {@link WitchAssets#COVEN_WITCH}). */
    public abstract String rig();

    /** Her potency before her familiar: the sigils' {@code potency}. */
    protected abstract float basePotency();

    /** The sigil she hurls this time (an effect sigil's path). */
    protected abstract String pickSigil(LivingEntity target);

    /** Ticks between two bolts. */
    protected abstract int boltInterval();

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new CastGoal(this));
        goalSelector.addGoal(5, new HexVillagerGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, WitchEntity.class, FamiliarEntity.class, HexedVillagerEntity.class).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // --- allies --------------------------------------------------------------------------------------------------------

    @Override
    public boolean isAlliedTo(Entity other) {
        return super.isAlliedTo(other) || WitchMagic.kin(other);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !WitchMagic.kin(target) && super.canAttack(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && source.getEntity() instanceof LivingEntity attacker && !WitchMagic.kin(attacker)) {
            alert(attacker);
            Vec3 at = attacker.position();
            if (teleportCooldown <= 0 && pending == null && distanceToSqr(attacker) < 16 && random.nextFloat() < teleportChance()) {
                start(Act.TELEPORT, null, at);
            }
        }
        return hurt;
    }

    /** Chance she blinks away when struck up close. */
    protected float teleportChance() {
        return 0.3f;
    }

    /** Her sisters within {@link #ALERT_RANGE} (and her familiar) turn on {@code attacker}. */
    public void alert(LivingEntity attacker) {
        for (net.minecraft.world.entity.Mob m : level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, getBoundingBox().inflate(ALERT_RANGE),
                m -> m != this && (m instanceof WitchEntity || m instanceof FamiliarEntity) && m.getTarget() == null)) {
            m.setTarget(attacker);
        }
    }

    // --- her familiar ---------------------------------------------------------------------------------------------------

    /** Her bound familiar, if it is alive and near. */
    public @Nullable FamiliarEntity familiar() {
        if (familiarId == null || !(level() instanceof ServerLevel server)) return null;
        Entity e = server.getEntity(familiarId);
        if (e instanceof FamiliarEntity f && f.isAlive() && f.distanceToSqr(this) < FAMILIAR_RANGE * FAMILIAR_RANGE) return f;
        return null;
    }

    /**
     * Her bound familiar wherever it is in her world, if it is loaded and alive (however far it has strayed): she only calls
     * another when this is null.
     */
    public @Nullable FamiliarEntity boundFamiliar() {
        if (familiarId == null || !(level() instanceof ServerLevel server)) return null;
        return server.getEntity(familiarId) instanceof FamiliarEntity f && f.isAlive() ? f : null;
    }

    public void bind(FamiliarEntity familiar) {
        familiarId = familiar.getUUID();
        familiar.bindTo(this);
    }

    /** Bound to the familiar with {@code id} (worldgen binds by id: it may stand in another chunk). */
    public void bindFamiliar(UUID id) {
        familiarId = id;
    }

    public @Nullable UUID familiarId() {
        return familiarId;
    }

    /** How strong her magic is now: stronger while her familiar lives. */
    public float power() {
        return (float) (basePotency() * (familiar() != null ? FAMILIAR_POWER : 1.0));
    }

    // --- acting ----------------------------------------------------------------------------------------------------------

    public boolean busy() {
        return pending != null;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CASTING, false);
    }

    /** Whether she is winding up or casting a spell now (synced). */
    public boolean casting() {
        return entityData.get(CASTING);
    }

    /** A subclass's own windup begins or ends (a Coven Witch's strong curse). */
    protected void setCasting(boolean on) {
        entityData.set(CASTING, on);
    }

    public @Nullable Act pending() {
        return pending;
    }

    /** Begins {@code act}: its clip plays now, it lands at the clip's moment. */
    public void start(Act act, @Nullable LivingEntity target, Vec3 at) {
        pending = act;
        pendingTicks = WitchAssets.HIT_TICKS.getOrDefault(act.clip, 8);
        pendingTarget = target == null ? null : target.getUUID();
        pendingAt = at;
        if (act != Act.TELEPORT) setCasting(true);
        triggerAnim("action", act.clip);
        if (act == Act.HEX || act == Act.HEX_VILLAGER) playSound(AllSounds.coven("witch.hex"), 1f, 1f);
        onStart(act, target);
    }

    /** A subclass's telegraph when an act begins. */
    protected void onStart(Act act, @Nullable LivingEntity target) {
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        if (boltCooldown > 0) boltCooldown--;
        if (bagCooldown > 0) bagCooldown--;
        if (healCooldown > 0) healCooldown--;
        if (teleportCooldown > 0) teleportCooldown--;
        if (villagerCooldown > 0) villagerCooldown--;
        if (taglockCooldown > 0) taglockCooldown--;
        if (pending != null && --pendingTicks <= 0) {
            Act act = pending;
            pending = null;
            setCasting(false);
            LivingEntity target = pendingTarget == null || !(level() instanceof ServerLevel s) ? null
                    : s.getEntity(pendingTarget) instanceof LivingEntity l ? l : null;
            land(act, target, pendingAt);
        }
        if (taglockCooldown <= 0 && pending == null) {
            taglockCooldown = TAGLOCK_CURSE_EVERY;
            curseTheRobbed();
        }
    }

    /** {@code act}'s moment. */
    protected void land(Act act, @Nullable LivingEntity target, Vec3 at) {
        switch (act) {
            case BOLT -> {
                if (target != null && target.isAlive() && hasLineOfSight(target)) WitchMagic.bolt(this, target, pickSigil(target), power());
            }
            case BAG -> {
                if (target instanceof Player p && distanceToSqr(p) < 25) WitchMagic.slipBag(this, p);
            }
            case HEAL -> WitchMagic.heal(this, getMaxHealth() * 0.3f);
            case TELEPORT -> {
                WitchMagic.teleport(this, at, 8, 14, MagicFxPayload.SMOKE_WITCH);
                teleportCooldown = 200;
            }
            case HEX_VILLAGER -> {
                if (target instanceof Villager v && v.isAlive() && distanceToSqr(v) < 16 * 16) HexedVillagerEntity.hex(v, this);
            }
            default -> {
            }
        }
    }

    /** If her familiar holds someone's taglock, she curses its owner from wherever they are (within name-magic reach). */
    protected void curseTheRobbed() {
        FamiliarEntity f = familiar();
        Taglock stolen = f == null ? null : f.stolen();
        if (stolen == null || !(level() instanceof ServerLevel server)) return;
        Entity victim = NameMagic.anywhere(server, stolen.owner());
        if (!(victim instanceof LivingEntity l) || victim.level() != level() || victim.distanceToSqr(this) > NameMagic.REACH * NameMagic.REACH) return;
        triggerAnim("action", "hex");
        WitchMagic.curse(this, l, robbedCurse(), TAGLOCK_CURSE_TICKS);
    }

    /** The curse she lays through a stolen taglock. */
    protected CurseEffect.Curse robbedCurse() {
        return CurseEffect.Curse.MISFORTUNE;
    }

    /** Her choice in a fight, each tick her {@link CastGoal} runs ({@code dist} to the target, squared). */
    protected void decide(LivingEntity target, double dist2, boolean seen) {
        if (busy()) return;
        if (getHealth() < getMaxHealth() * 0.5f && healCooldown <= 0) {
            healCooldown = 400;
            start(Act.HEAL, null, position());
            return;
        }
        if (target instanceof Player && dist2 < 3.5 * 3.5 && bagCooldown <= 0 && seen) {
            bagCooldown = 600;
            start(Act.BAG, target, target.position());
            return;
        }
        if (seen && boltCooldown <= 0 && dist2 < 24 * 24) {
            boltCooldown = boltInterval() + random.nextInt(20);
            start(Act.BOLT, target, target.position());
        }
    }

    // --- sounds ----------------------------------------------------------------------------------------------------------

    protected String soundPrefix() {
        return "hedge";
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AllSounds.coven("witch." + soundPrefix() + "_ambient");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.coven("witch." + soundPrefix() + "_hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.coven("witch." + soundPrefix() + "_death");
    }

    // --- save ------------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (familiarId != null) tag.putUUID("Familiar", familiarId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        familiarId = tag.hasUUID("Familiar") ? tag.getUUID("Familiar") : null;
    }

    // --- GeckoLib --------------------------------------------------------------------------------------------------------

    protected RawAnimation loop(String clip) {
        return RawAnimation.begin().thenLoop("animation." + rig() + "." + clip);
    }

    protected RawAnimation once(String clip) {
        return RawAnimation.begin().thenPlay("animation." + rig() + "." + clip);
    }

    /** The loop the {@code base} controller plays now. */
    protected RawAnimation baseLoop(boolean moving) {
        return loop(moving ? "walk" : "idle");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base", 4, state -> state.setAndContinue(baseLoop(state.isMoving()))));
        AnimationController<WitchEntity> action = new AnimationController<>(this, "action", 2, state -> PlayState.STOP);
        for (String clip : WitchAssets.triggered(rig())) action.triggerableAnim(clip, once(clip));
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    // --- goals -----------------------------------------------------------------------------------------------------------

    /** Keeps between {@link #NEAR} and {@link #FAR} of her target, faces it, and lets her {@link #decide}. */
    static final class CastGoal extends Goal {
        private final WitchEntity witch;
        private int repath;

        CastGoal(WitchEntity witch) {
            this.witch = witch;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = witch.getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            witch.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity t = witch.getTarget();
            if (t == null) return;
            witch.getLookControl().setLookAt(t, 30f, 30f);
            double d2 = witch.distanceToSqr(t);
            boolean seen = witch.getSensing().hasLineOfSight(t);
            if (--repath <= 0) {
                repath = 10;
                if (d2 < NEAR * NEAR) {
                    Vec3 away = DefaultRandomPos.getPosAway(witch, 10, 5, t.position());
                    if (away != null) witch.getNavigation().moveTo(away.x, away.y, away.z, 1.15);
                } else if (d2 > FAR * FAR || !seen) {
                    witch.getNavigation().moveTo(t, 1.0);
                } else {
                    witch.getNavigation().stop();
                }
            }
            witch.decide(t, d2, seen);
        }
    }

    /** Out of a fight, a witch who can hexes a villager nearby into her thrall (a few per place at most). */
    static final class HexVillagerGoal extends Goal {
        private final WitchEntity witch;
        private @Nullable Villager victim;

        HexVillagerGoal(WitchEntity witch) {
            this.witch = witch;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!witch.hexesVillagers() || witch.getTarget() != null || witch.villagerCooldown > 0 || witch.busy()) return false;
            witch.villagerCooldown = 200;
            AABB box = witch.getBoundingBox().inflate(16);
            if (witch.level().getEntitiesOfClass(HexedVillagerEntity.class, witch.getBoundingBox().inflate(32)).size()
                    >= HexedVillagerEntity.MAX_NEARBY) return false;
            List<Villager> villagers = witch.level().getEntitiesOfClass(Villager.class, box, v -> v.isAlive() && !v.isBaby());
            if (villagers.isEmpty()) return false;
            victim = villagers.get(witch.random.nextInt(villagers.size()));
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return victim != null && victim.isAlive() && witch.getTarget() == null && witch.distanceToSqr(victim) < 24 * 24;
        }

        @Override
        public void tick() {
            if (victim == null) return;
            witch.getLookControl().setLookAt(victim);
            if (witch.distanceToSqr(victim) > 6 * 6) {
                witch.getNavigation().moveTo(victim, 0.9);
            } else if (!witch.busy()) {
                witch.getNavigation().stop();
                witch.villagerCooldown = 2400;
                witch.start(Act.HEX_VILLAGER, victim, victim.position());
                victim = null;
            }
        }
    }

    /** Whether she turns villagers into hexed victims when idle. */
    protected boolean hexesVillagers() {
        return false;
    }
}
