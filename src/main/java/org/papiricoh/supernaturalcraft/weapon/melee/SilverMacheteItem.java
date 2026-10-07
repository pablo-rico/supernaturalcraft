package org.papiricoh.supernaturalcraft.weapon.melee;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.phys.Vec3;

/**
 * A heavy silver machete. Struck from behind, a blow is always a double critical; a kill from
 * behind takes the head (see WeaponEvents).
 */
public class SilverMacheteItem extends SwordItem {

    public static final float BACKSTAB_MULTIPLIER = 2.0f;

    public SilverMacheteItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    /** True when {@code attacker} stands behind {@code target} (outside its forward half-cone). */
    public static boolean isBehind(Entity attacker, LivingEntity target) {
        Vec3 facing = target.getViewVector(1.0f).multiply(1, 0, 1);
        Vec3 toTarget = target.position().subtract(attacker.position()).multiply(1, 0, 1);
        if (facing.lengthSqr() < 1.0E-4 || toTarget.lengthSqr() < 1.0E-4) return false;
        return facing.normalize().dot(toTarget.normalize()) > 0.5;
    }
}
