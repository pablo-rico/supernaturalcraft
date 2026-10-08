package org.papiricoh.supernaturalcraft.reward;

/**
 * Six wings of the Broken Chorus, worn in the Curios back slot (or in the chest slot, with or without Curios). They rest,
 * fold when you crouch and open wide when you fall or glide; with Michael's Grace taken in they fly
 * ({@code reward/michael/WingFlight}).
 */
public class SeraphWingsItem extends LoreItem implements net.minecraft.world.item.Equipable {

    public SeraphWingsItem(Properties properties) {
        super(properties);
    }

    @Override
    public net.minecraft.world.entity.EquipmentSlot getEquipmentSlot() {
        return net.minecraft.world.entity.EquipmentSlot.CHEST;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(net.minecraft.world.level.Level level,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        return swapWithEquipmentSlot(this, level, player, hand);
    }
}
