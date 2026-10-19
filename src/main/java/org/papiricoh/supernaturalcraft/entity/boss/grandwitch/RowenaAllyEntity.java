package org.papiricoh.supernaturalcraft.entity.boss.grandwitch;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaAssets;
import org.papiricoh.supernaturalcraft.entity.witch.CovenWitchEntity;
import org.papiricoh.supernaturalcraft.entity.witch.WitchMagic;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.network.GrandWitchFxPayload;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.GeckoLibServices;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;

/**
 * Rowena at the hunters' side against the Grand Coven (v0.21, P6; precedent: the Host's soldiers, {@code HostAllyEntity}). She
 * arrives in her smoke when the Grand Witch's last phase begins ({@link #arrive}), keeps near the hunters, throws a bolt at the
 * coven every {@link GrandWitchBalance#ALLY_BOLT_EVERY} ticks and, when the mass taglock comes, draws the green circles to stand in
 * ({@link #drawCircles}: the boss chooses where). She cannot be hurt, never despawns on her own, and goes home in her smoke when the
 * fight is over ({@link #farewell}, or with no Grand Witch about). Drawn with her own rig ({@link RowenaAssets}), her boss clips.
 */
public class RowenaAllyEntity extends PathfinderMob implements GeoEntity {

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID boss;
    private int boltCooldown = GrandWitchBalance.ALLY_BOLT_EVERY, lonely, leaving = -1;

    public RowenaAllyEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setInvulnerable(true);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    /** Rowena steps out of her smoke beside a hunter of {@code witch}'s fight. @return her, or null */
    public static @Nullable RowenaAllyEntity arrive(GrandWitchEntity witch) {
        if (!(witch.level() instanceof ServerLevel level) || witch.ally() != null) return witch.ally();
        RowenaAllyEntity r = AllEntities.ROWENA_ALLY.get().create(level);
        if (r == null) return null;
        List<LivingEntity> hunters = witch.hunters();
        Vec3 near = hunters.isEmpty() ? witch.position().add(4, 0, 0) : hunters.getFirst().position();
        Vec3 at = LuciferAttacks.floorAt(witch, near.add(witch.getRandom().nextDouble() * 3 - 1.5, 0, witch.getRandom().nextDouble() * 3 - 1.5));
        r.moveTo(at.x, at.y, at.z, LuciferAttacks.yawTo(at, witch.position()), 0);
        r.boss = witch.getUUID();
        level.addFreshEntity(r);
        witch.setAlly(r);
        MagicFx.teleport(level, r, School.WITCHCRAFT, at, at, MagicFxPayload.SMOKE_YELLOW, 40);
        witch.fx(new GrandWitchFxPayload(r.getId(), GrandWitchFxPayload.ALLY, 1, 0, at, 40));
        r.triggerAnim("action", "teleport");
        r.playSound(AllSounds.coven("rowena.laugh"), 1.5f, 1f);
        witch.tell(level, "message.supernaturalcraft.grand_witch.rowena_arrives");
        return r;
    }

    public @Nullable GrandWitchEntity witch() {
        return boss != null && level() instanceof ServerLevel s && s.getEntity(boss) instanceof GrandWitchEntity w && w.isAlive() ? w : null;
    }

    /** She draws the green circles at {@code spots} (the boss shows them): a sweep of her hands toward the floor. */
    public void drawCircles(List<Vec3> spots) {
        if (spots.isEmpty()) return;
        getNavigation().stop();
        getLookControl().setLookAt(spots.getFirst().x, spots.getFirst().y, spots.getFirst().z);
        triggerAnim("action", "glacies");
        playSound(AllSounds.coven("rowena.recite"), 1.2f, 1.2f);
    }

    /** The fight is over: she stays to see it, then goes home in her smoke after {@code ticks}. */
    public void farewell(int ticks) {
        leaving = Math.max(1, ticks);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (leaving >= 0) {
            getNavigation().stop();
            if (--leaving <= 0) vanish();
            return;
        }
        GrandWitchEntity w = witch();
        if (w == null) {
            if (++lonely > 100) vanish();
            return;
        }
        lonely = 0;
        follow(w);
        if (--boltCooldown <= 0) {
            boltCooldown = GrandWitchBalance.ALLY_BOLT_EVERY;
            LivingEntity target = boltTarget(w);
            if (target != null && hasLineOfSight(target)) {
                getLookControl().setLookAt(target);
                triggerAnim("action", "fling");
                WitchMagic.bolt(this, target, "pain", GrandWitchBalance.ALLY_BOLT_POTENCY);
            }
        }
    }

    private void follow(GrandWitchEntity w) {
        if (tickCount % 10 != 0) return;
        List<LivingEntity> hunters = w.hunters();
        if (hunters.isEmpty()) return;
        LivingEntity h = hunters.getFirst();
        double d = distanceToSqr(h);
        if (d > 7 * 7) getNavigation().moveTo(h, 1.1);
        else if (d < 3 * 3) getNavigation().stop();
        getLookControl().setLookAt(w);
    }

    /** One of the coven still standing, else the Grand Witch herself. */
    private @Nullable LivingEntity boltTarget(GrandWitchEntity w) {
        CovenWitchEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (CovenWitchEntity c : level().getEntitiesOfClass(CovenWitchEntity.class, getBoundingBox().inflate(24))) {
            double d = c.distanceToSqr(this);
            if (c.isAlive() && d < best) {
                best = d;
                nearest = c;
            }
        }
        return nearest != null ? nearest : w;
    }

    private void vanish() {
        if (level() instanceof ServerLevel level) {
            MagicFx.teleport(level, this, School.WITCHCRAFT, position(), position(), MagicFxPayload.SMOKE_YELLOW, 30);
            for (ServerPlayer p : level.players()) {
                if (p.distanceToSqr(this) < 96 * 96) {
                    org.papiricoh.supernaturalcraft.network.Send.toPlayer(p, new GrandWitchFxPayload(getId(), GrandWitchFxPayload.ALLY, 0, 0, position(), 30));
                }
            }
        }
        discard();
    }

    // --- an untouchable ally -----------------------------------------------------------------------------------------------

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isInvulnerableTo(source) && super.hurt(source, amount);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof net.minecraft.world.entity.player.Player || super.isAlliedTo(other);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (boss != null) tag.putUUID("Boss", boss);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        boss = tag.hasUUID("Boss") ? tag.getUUID("Boss") : null;
    }

    // --- GeckoLib --------------------------------------------------------------------------------------------------------

    @Override
    public void triggerAnim(@Nullable String controller, String anim) {
        if (!RowenaAssets.TRIGGERED.contains(anim)) return;
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
        RawAnimation idle = RawAnimation.begin().thenLoop("animation.rowena.idle"), walk = RawAnimation.begin().thenLoop("animation.rowena.walk");
        controllers.add(new AnimationController<>(this, "base", 6, state -> state.setAndContinue(state.isMoving() ? walk : idle)));
        AnimationController<RowenaAllyEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String name : RowenaAssets.TRIGGERED) {
            if (!name.equals("death")) action.triggerableAnim(name, RawAnimation.begin().thenPlay("animation.rowena." + name));
        }
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
