package org.papiricoh.supernaturalcraft.bowl.spell;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;
import java.util.UUID;

/**
 * A vial of someone's blood ({@link BloodSample}): a glass bottle used on another player (1 damage
 * to them), or on yourself by sneaking and using it in the air (2 damage). Poured into a spell bowl
 * it is a dose of {@code BLOOD} that a locating spell follows back to its owner.
 */
public class BloodVialItem extends Item {

    /** What drawing blood costs: from someone else, and from yourself. */
    public static final float OTHER_DAMAGE = 1f;
    public static final float SELF_DAMAGE = 2f;

    public BloodVialItem(Properties props) {
        super(props);
    }

    public static ItemStack of(UUID owner, String name) {
        ItemStack vial = new ItemStack(AllItems.BLOOD_VIAL.get());
        vial.set(AllDataComponents.BLOOD_SAMPLE.get(), new BloodSample(owner, name));
        return vial;
    }

    /**
     * {@code drawer} fills the glass bottle in {@code hand} with {@code from}'s blood (themself or
     * someone else), hurting them for it. Server side.
     */
    public static void draw(Player drawer, InteractionHand hand, LivingEntity from) {
        boolean self = drawer == from;
        ItemStack bottle = drawer.getItemInHand(hand);
        ItemStack vial = of(from.getUUID(), from.getName().getString());
        from.hurt(from.damageSources().generic(), self ? SELF_DAMAGE : OTHER_DAMAGE);
        drawer.setItemInHand(hand, ItemUtils.createFilledResult(bottle, drawer, vial));
        drawer.level().playSound(null, from.getX(), from.getY(), from.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.8f, 0.7f);
        if (drawer.level() instanceof ServerLevel level) {
            level.sendParticles(new DustParticleOptions(new Vector3f(0.55f, 0.02f, 0.04f), 1.0f),
                    from.getX(), from.getY() + from.getBbHeight() * 0.6, from.getZ(), 8, 0.2, 0.2, 0.2, 0.0);
        }
        drawer.displayClientMessage(self
                ? Component.translatable("message.supernaturalcraft.blood_vial.own").withStyle(ChatFormatting.DARK_RED)
                : Component.translatable("message.supernaturalcraft.blood_vial.drawn", from.getName()).withStyle(ChatFormatting.DARK_RED), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BloodSample sample = stack.get(AllDataComponents.BLOOD_SAMPLE.get());
        if (sample != null) {
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.blood_vial.of", sample.name()).withStyle(ChatFormatting.DARK_RED));
        } else {
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.blood_vial.unknown").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.blood_vial.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
