package org.papiricoh.supernaturalcraft.reward.michael;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelBalance;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelEntity;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import net.minecraft.core.component.DataComponents;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * Michael's own lance, pulled out of the ground by a hunter in phase VI: it burns in the hand for
 * {@link MichaelBalance#BORROWED_TICKS} and can be hurled back at him once ({@link ThrownLanceEntity#borrowed}). It never
 * leaves the arena: out of it, dropped, run out, or with him gone, it tears itself free and flies back to him.
 */
public class BorrowedLanceItem extends Item implements GeoItem {

    private static final String MICHAEL = "Michael", EXPIRES = "Expires";
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public BorrowedLanceItem(Properties properties) {
        super(properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    /** The lance as it comes out of the ground: Michael's, until {@code now} + its time. */
    public static ItemStack borrowed(MichaelEntity michael, long now) {
        ItemStack stack = new ItemStack(AllItems.BORROWED_LANCE.get());
        CompoundTag tag = new CompoundTag();
        tag.putInt(MICHAEL, michael.getId());
        tag.putLong(EXPIRES, now + MichaelBalance.BORROWED_TICKS);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static @Nullable MichaelEntity owner(ItemStack stack, Level level) {
        CompoundTag tag = data(stack);
        if (!tag.contains(MICHAEL)) return null;
        return level.getEntity(tag.getInt(MICHAEL)) instanceof MichaelEntity m && m.isAlive() ? m : null;
    }

    public static long expires(ItemStack stack) {
        return data(stack).getLong(EXPIRES);
    }

    /** Whether the borrowed lance must go home now: run out, him gone, or its bearer out of his arena. */
    public static boolean mustReturn(ItemStack stack, Entity holder) {
        MichaelEntity m = owner(stack, holder.level());
        if (m == null || holder.level().getGameTime() >= expires(stack)) return true;
        ArenaController arena = m.arena();
        return arena != null && !arena.contains(holder.position());
    }

    /** It flies back to him out of {@code holder}'s hands. */
    public static void sendHome(ItemStack stack, Entity holder) {
        MichaelEntity m = owner(stack, holder.level());
        if (m != null) m.lanceReturned();
        stack.setCount(0);
        if (holder instanceof ServerPlayer p) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.michael.lance_gone").withStyle(ChatFormatting.GOLD), true);
            p.serverLevel().playSound(null, p.blockPosition(), AllSounds.MICHAEL_LANCE_RECALL.get(), SoundSource.HOSTILE, 1.6f, 1.2f);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (!level.isClientSide && mustReturn(stack, holder)) sendHome(stack, holder);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide) {
            sendHome(stack, entity);
            entity.discard();
        }
        return true;
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
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (getUseDuration(stack, user) - timeLeft < MichaelLanceItem.DRAW_TICKS) return;
        hurlBack(stack, level, user);
    }

    /** Throws it back at him. */
    public static void hurlBack(ItemStack stack, Level level, LivingEntity user) {
        if (level.isClientSide) return;
        MichaelEntity m = owner(stack, level);
        if (m == null) {
            stack.setCount(0);
            return;
        }
        ThrownLanceEntity lance = ThrownLanceEntity.borrowed(level, user, m.getId());
        level.addFreshEntity(lance);
        level.playSound(null, lance, AllSounds.MICHAEL_LANCE_THROW.get(), SoundSource.PLAYERS, 1.4f, 0.9f);
        stack.setCount(0);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.borrowed_lance").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
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
