package org.papiricoh.supernaturalcraft.entity.angel;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * A fallen angel (v0.22), cast down in the Fall when Metatron was beaten ({@code angel.AngelSpawns}): a vessel in worn, torn
 * clothes, dimmed eyes, its wings broken and burnt (always showing). It can't fly any more, so it doesn't blink; it leaps.
 *
 * <p>It hunts humans and Heaven's sworn on sight, and the Host's angels; it leaves Hell's sworn alone unless struck
 * ({@code Kin.sameSide}). Only an angel blade truly kills it, as any angel.
 */
public class FallenAngelEntity extends AngelEntity {

    public static final float HEALTH = 32, DAMAGE = 7;
    /** The leap: from this far, at most once every {@link #LEAP_COOLDOWN} ticks. */
    public static final double LEAP_MIN = 3.5, LEAP_MAX = 8;
    public static final int LEAP_COOLDOWN = 60;

    private int leapCooldown = 20;

    public FallenAngelEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AngelEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH)
                .add(Attributes.ARMOR, 2)
                .add(Attributes.MOVEMENT_SPEED, 0.31)
                .add(Attributes.ATTACK_DAMAGE, DAMAGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    @Override
    public boolean fallen() {
        return true;
    }

    @Override
    protected int vessels() {
        return AngelAssets.FALLEN_VESSELS;
    }

    @Override
    protected boolean canBlink() {
        return false;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                p -> Kin.freeWill(p) || Kin.isAngel(p)));
    }

    @Override
    protected boolean foe(LivingEntity e) {
        return !e.getType().is(AllTags.Entities.BOSSES) && Kin.isAngel(e);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (leapCooldown > 0) leapCooldown--;
        LivingEntity foe = getTarget();
        if (foe == null || !foe.isAlive() || leapCooldown > 0 || !onGround()) return;
        double d = distanceTo(foe);
        if (d < LEAP_MIN || d > LEAP_MAX) return;
        leapCooldown = LEAP_COOLDOWN;
        Vec3 to = foe.position().subtract(position()).multiply(1, 0, 1).normalize();
        setDeltaMovement(to.x * 0.9, 0.42, to.z * 0.9);
        hurtMarked = true;
        triggerAnim("action", "leap");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AllSounds.angel("fallen_ambient");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.angel("fallen_hurt");
    }
}
