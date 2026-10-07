package org.papiricoh.supernaturalcraft.bowl;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.List;

/**
 * Carrying a full bowl. In the main hand it takes both hands: the off hand can do nothing (its
 * uses are cancelled, swapping hands is refused, anything put there is moved to the inventory).
 * Jumping, falling and being hit slop some of it out ({@link SpillRules}): liquid is lost with a
 * splash, ingredients drop at the bearer's feet. Tossed, it spills its liquid. Stowed in the
 * inventory, it keeps everything.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class BowlCarry {

    private BowlCarry() {
    }

    /** The bowl in {@code entity}'s main hand, or empty. */
    public static ItemStack carried(LivingEntity entity) {
        ItemStack main = entity.getMainHandItem();
        return main.getItem() instanceof SpellBowlItem ? main : ItemStack.EMPTY;
    }

    public static boolean carrying(LivingEntity entity) {
        return !carried(entity).isEmpty();
    }

    // --- spills ----------------------------------------------------------------------------

    /** A jump with dice roll {@code roll} in [0, 1). @return the spill applied */
    public static SpillRules.Spill jump(ServerPlayer player, double roll) {
        ItemStack bowl = carried(player);
        if (bowl.isEmpty()) return SpillRules.Spill.NONE;
        BowlContents c = SpellBowlItem.contents(bowl);
        return spill(player, bowl, SpillRules.jump(c.liquids().size(), roll));
    }

    public static SpillRules.Spill fall(ServerPlayer player, float distance) {
        ItemStack bowl = carried(player);
        if (bowl.isEmpty()) return SpillRules.Spill.NONE;
        BowlContents c = SpellBowlItem.contents(bowl);
        return spill(player, bowl, SpillRules.fall(distance, c.liquids().size(), c.itemCount()));
    }

    public static SpillRules.Spill hit(ServerPlayer player, float damage, double roll) {
        ItemStack bowl = carried(player);
        if (bowl.isEmpty()) return SpillRules.Spill.NONE;
        BowlContents c = SpellBowlItem.contents(bowl);
        return spill(player, bowl, SpillRules.hit(damage, c.liquids().size(), c.itemCount(), roll));
    }

    /** Takes {@code spill} out of {@code bowl}: the newest doses and ingredients go first. */
    public static SpillRules.Spill spill(ServerPlayer player, ItemStack bowl, SpillRules.Spill spill) {
        if (spill.isNone()) return spill;
        BowlContents c = SpellBowlItem.contents(bowl);
        int doses = Math.min(spill.doses(), c.liquids().size());
        int items = Math.min(spill.items(), c.itemCount());
        if (doses == 0 && items == 0) return SpillRules.Spill.NONE;
        int color = c.mixColor();
        List<ItemStack> dropped = new ArrayList<>();
        for (int i = 0; i < doses; i++) c = c.withoutLastDose();
        for (int i = 0; i < items; i++) {
            dropped.add(c.lastItem());
            c = c.withoutLastItem();
        }
        setContents(bowl, c);
        ServerLevel level = player.serverLevel();
        for (ItemStack stack : dropped) {
            ItemEntity item = new ItemEntity(level, player.getX(), player.getY() + 0.4, player.getZ(), stack,
                    (level.random.nextDouble() - 0.5) * 0.2, 0.2, (level.random.nextDouble() - 0.5) * 0.2);
            item.setDefaultPickUpDelay();
            level.addFreshEntity(item);
        }
        if (doses > 0) splash(level, player.position().add(player.getLookAngle().multiply(0.5, 0, 0.5)).add(0, 0.9, 0), color, doses);
        String key = c.isEmpty() ? "message.supernaturalcraft.bowl.spill_all"
                : doses > 0 && items > 0 ? "message.supernaturalcraft.bowl.spill_both"
                : doses > 0 ? "message.supernaturalcraft.bowl.spill_liquid" : "message.supernaturalcraft.bowl.spill_items";
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GOLD), true);
        return new SpillRules.Spill(doses, items);
    }

    private static void setContents(ItemStack bowl, BowlContents c) {
        if (c.isEmpty()) bowl.remove(AllDataComponents.BOWL_CONTENTS.get());
        else bowl.set(AllDataComponents.BOWL_CONTENTS.get(), c);
    }

    /** Liquid slopping out at {@code at}, in the mix's colour. */
    public static void splash(ServerLevel level, Vec3 at, int color, int doses) {
        ColorParticleOption drop = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | (color & 0xFFFFFF));
        level.sendParticles(drop, at.x, at.y, at.z, 10 + doses * 8, 0.3, 0.2, 0.3, 0.6);
        level.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 8 + doses * 6, 0.3, 0.1, 0.3, 0.2);
        level.playSound(null, at.x, at.y, at.z, AllSounds.BOWL_SPILL.get(), SoundSource.PLAYERS, 0.8f, 0.9f + level.random.nextFloat() * 0.2f);
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) jump(player, player.getRandom().nextDouble());
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) fall(player, event.getDistance());
    }

    @SubscribeEvent
    public static void onHurt(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getSource().is(DamageTypeTags.IS_FALL) || event.getNewDamage() <= 0) return;
        hit(player, event.getNewDamage(), player.getRandom().nextDouble());
    }

    /** Thrown away, the bowl keeps its ingredients but its liquid goes everywhere. */
    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (!(stack.getItem() instanceof SpellBowlItem) || !(event.getEntity().level() instanceof ServerLevel level)) return;
        BowlContents c = SpellBowlItem.contents(stack);
        if (c.liquids().isEmpty()) return;
        int color = c.mixColor();
        int doses = c.liquids().size();
        setContents(stack, c.withoutLiquids());
        event.getEntity().setItem(stack);
        splash(level, event.getEntity().position(), color, doses);
    }

    // --- the busy off hand -------------------------------------------------------------------

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (offHandBusy(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (offHandBusy(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (offHandBusy(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (offHandBusy(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onSwapHands(LivingSwapItemsEvent.Hands event) {
        LivingEntity e = event.getEntity();
        if (e.getMainHandItem().getItem() instanceof SpellBowlItem || e.getOffhandItem().getItem() instanceof SpellBowlItem) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) freeOffHand(player);
    }

    private static boolean offHandBusy(Player player, InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND && carrying(player);
    }

    /**
     * While the bowl is carried, whatever is in the off hand goes to the inventory (or the ground
     * if there is no room). @return whether anything was moved
     */
    public static boolean freeOffHand(ServerPlayer player) {
        if (!carrying(player)) return false;
        ItemStack off = player.getOffhandItem();
        if (off.isEmpty()) return false;
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        if (!player.getInventory().add(off)) player.drop(off, false);
        player.displayClientMessage(Component.translatable("message.supernaturalcraft.bowl.hands_full").withStyle(ChatFormatting.GOLD), true);
        return true;
    }
}
