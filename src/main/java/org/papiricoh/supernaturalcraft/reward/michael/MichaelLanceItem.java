package org.papiricoh.supernaturalcraft.reward.michael;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * The Lance of Michael: a spear in the hand (holy, demon-bane), and thrown like a trident it comes back, strikes twice as
 * hard at angels and demons and pins what it strikes for a moment ({@link ThrownLanceEntity}). Drawn with the same
 * GeckoLib model as his own ({@code geo/item/michael_lance.geo.json}).
 */
public class MichaelLanceItem extends Item implements GeoItem {

    /** Ticks it must be drawn back before it flies. */
    public static final int DRAW_TICKS = 10;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MichaelLanceItem(Properties properties) {
        super(properties.attributes(attributes()));
        GeoItem.registerSyncedAnimatable(this);
    }

    public static ItemAttributeModifiers attributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 9.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.8, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(
                        org.papiricoh.supernaturalcraft.SupernaturalCraft.asResource("lance_reach"), 1.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getDamageValue() >= stack.getMaxDamage() - 1) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (!(user instanceof Player player) || getUseDuration(stack, user) - timeLeft < DRAW_TICKS) return;
        if (!level.isClientSide) {
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(user.getUsedItemHand()));
            ThrownLanceEntity lance = ThrownLanceEntity.thrown(level, player, stack);
            level.addFreshEntity(lance);
            level.playSound(null, lance, AllSounds.MICHAEL_LANCE_THROW.get(), SoundSource.PLAYERS, 1.0f, 1.1f);
            if (!player.hasInfiniteMaterials()) player.getInventory().removeItem(stack);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.michael_lance").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("animation.michael_lance.idle");
        controllers.add(new AnimationController<>(this, "main", 3, s -> s.setAndContinue(idle)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
