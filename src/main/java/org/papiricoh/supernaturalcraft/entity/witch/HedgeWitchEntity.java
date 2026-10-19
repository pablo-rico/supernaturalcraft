package org.papiricoh.supernaturalcraft.entity.witch;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * The Hedge Witch (v0.21): a lone witch in patched layers. She keeps her distance and hurls hexes (mostly the {@code hex}
 * sigil, now and then {@code frost}, rarely {@code pain}), slips a curse bag into the pocket of whoever comes close, drinks her
 * brew when hurt, blinks away in smoke when cornered, and out of a fight hexes the villagers she finds into her thralls.
 * Night spawns in forests and swamps, rarer in plains; two to four at a coven.
 */
public class HedgeWitchEntity extends WitchEntity {

    public HedgeWitchEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 26.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    public String rig() {
        return WitchAssets.HEDGE_WITCH;
    }

    @Override
    protected float basePotency() {
        return 1.0f;
    }

    @Override
    protected int boltInterval() {
        return 60;
    }

    @Override
    protected String pickSigil(LivingEntity target) {
        int roll = random.nextInt(10);
        return roll < 6 ? "hex" : roll < 9 ? "frost" : "pain";
    }

    @Override
    protected boolean hexesVillagers() {
        return true;
    }
}
