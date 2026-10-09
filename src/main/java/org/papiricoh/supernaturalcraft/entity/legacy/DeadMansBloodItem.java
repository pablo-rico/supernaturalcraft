package org.papiricoh.supernaturalcraft.entity.legacy;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.List;

/**
 * A vial of dead man's blood (v0.17): poison to a vampire. Used on one, it stuns it; used with a blade in the other hand, it
 * smears the blade ({@link #CHARGES} blows), and a smeared blade stuns the vampire it strikes ({@link #onHit}).
 */
public class DeadMansBloodItem extends Item {

    /** Blows a smeared blade keeps the blood for. */
    public static final int CHARGES = 3;
    private static final String KEY = "supernaturalcraft_dead_mans_blood";

    public DeadMansBloodItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof VampireEntity v)) return InteractionResult.PASS;
        if (!player.level().isClientSide) {
            v.stun();
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack blood = player.getItemInHand(hand);
        ItemStack blade = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!blade.is(AllTags.Items.BEHEADING)) return InteractionResultHolder.pass(blood);
        if (!level.isClientSide) {
            smear(blade);
            if (!player.getAbilities().instabuild) blood.shrink(1);
            level.playSound(null, player.blockPosition(), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.PLAYERS, 0.8f, 0.7f);
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.legacy.blood_smeared").withStyle(ChatFormatting.DARK_RED), true);
        }
        return InteractionResultHolder.sidedSuccess(blood, level.isClientSide);
    }

    /** Puts {@link #CHARGES} blows of blood on a blade. */
    public static void smear(ItemStack blade) {
        CustomData.update(DataComponents.CUSTOM_DATA, blade, tag -> tag.putInt(KEY, CHARGES));
    }

    public static int charges(ItemStack blade) {
        CompoundTag tag = blade.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getInt(KEY);
    }

    /** A blow landed: a smeared blade stuns a vampire and uses up a charge. */
    public static void onHit(LivingEntity victim, DamageSource source) {
        if (!(victim instanceof VampireEntity v) || victim.level().isClientSide) return;
        ItemStack blade = LegacyWeapons.melee(source);
        int n = charges(blade);
        if (n <= 0) return;
        v.stun();
        CustomData.update(DataComponents.CUSTOM_DATA, blade, tag -> {
            if (n <= 1) tag.remove(KEY);
            else tag.putInt(KEY, n - 1);
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.dead_mans_blood").withStyle(ChatFormatting.GRAY));
    }
}
