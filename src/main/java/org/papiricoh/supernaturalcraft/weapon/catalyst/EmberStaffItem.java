package org.papiricoh.supernaturalcraft.weapon.catalyst;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.projectile.HellfireBolt;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellContext;

/**
 * Tier I catalyst: a staff crowned with a live ember. Feeds hellfire spells, makes bolts burst
 * where they land, and throws fireballs of its own (useless against demons, who are made of the stuff).
 */
public class EmberStaffItem extends CatalystItem {

    public static final float HELLFIRE_POTENCY = 1.2f, BOLT_BURST = 1.5f, BOLT_DAMAGE = 6f;

    public EmberStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public void shape(SpellContext ctx, ItemStack stack, ResolvedSpell spell) {
        if (Catalyst.hasEffect(spell, "hellfire")) ctx.potency *= HELLFIRE_POTENCY;
        ctx.traits = ctx.traits.withBoltBurstArea(ctx.traits.boltBurstArea() + BOLT_BURST);
    }

    @Override
    protected float ownMana() {
        return 6f;
    }

    @Override
    protected int ownCooldown() {
        return 20;
    }

    @Override
    protected boolean ownSpell(ServerPlayer player, ItemStack stack) {
        Vec3 look = player.getLookAngle();
        HellfireBolt bolt = new HellfireBolt(player.level(), player, look,
                org.papiricoh.supernaturalcraft.weapon.ascension.Ascension.scale(stack, BOLT_DAMAGE), 1.2f);
        bolt.setPos(player.getX() + look.x, player.getEyeY() - 0.1, player.getZ() + look.z);
        player.level().addFreshEntity(bolt);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.7f, 1.2f);
        return true;
    }
}
