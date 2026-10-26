package org.papiricoh.supernaturalcraft.leviathan;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.leviathan.ThrownBoraxEntity;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

/** The borax items that do something in the hand (v0.25). */
public final class BoraxItems {

    private BoraxItems() {
    }

    /** A box of borax: rubbed straight into a Leviathan (or its head) within reach. */
    public static class Powder extends Item {
        public Powder(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
            if (!(target instanceof BoraxSensitive s)) return InteractionResult.PASS;
            if (player.level() instanceof ServerLevel level) {
                s.onBorax(level, player);
                stack.consume(1, player);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
    }

    /** A bottle of borax spray, thrown like a splash potion ({@link ThrownBoraxEntity}). */
    public static class Spray extends Item {
        public Spray(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.5f, 0.6f);
            if (!level.isClientSide) {
                ThrownBoraxEntity thrown = new ThrownBoraxEntity(level, player);
                thrown.setItem(stack);
                thrown.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0f, 0.7f, 1.0f);
                level.addFreshEntity(thrown);
            }
            stack.consume(1, player);
            player.getCooldowns().addCooldown(this, 10);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    /**
     * Roman Enterprises' own pump sprayer, taken from Dick Roman: a cone of borax {@link #RANGE} blocks long, one charge a
     * puff (its durability). Used while sneaking with borax in the inventory, it refills {@link #CHARGES_PER_BOX} a box.
     */
    public static class Sprayer extends Item {
        public static final double RANGE = 5, CONE_COS = Math.cos(Math.toRadians(30));
        public static final int CHARGES = 48, CHARGES_PER_BOX = 16, COOLDOWN = 12;

        public Sprayer(Properties properties) {
            super(properties.durability(CHARGES));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (player.isShiftKeyDown()) return refill(level, player, stack);
            if (stack.getDamageValue() >= stack.getMaxDamage() - 1) return InteractionResultHolder.fail(stack);
            if (level instanceof ServerLevel server) {
                Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
                Borax.touch(server, new AABB(eye, eye).inflate(RANGE), e -> {
                    Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
                    double d = to.length();
                    return d <= RANGE && (d < 1 || to.normalize().dot(look) >= CONE_COS);
                }, player);
                for (int i = 1; i <= 4; i++) Borax.dry(server, eye.add(look.scale(i * RANGE / 4)).subtract(0, 1.2, 0), 1.2);
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.WHITE_ASH, eye.x + look.x * 2, eye.y + look.y * 2, eye.z + look.z * 2,
                        60, look.x * 1.5 + 0.4, 0.4, look.z * 1.5 + 0.4, 0.05);
                stack.setDamageValue(stack.getDamageValue() + 1);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6f, 1.6f);
            player.getCooldowns().addCooldown(this, COOLDOWN);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        private InteractionResultHolder<ItemStack> refill(Level level, Player player, ItemStack stack) {
            if (stack.getDamageValue() == 0) return InteractionResultHolder.pass(stack);
            int slot = player.getInventory().findSlotMatchingItem(new ItemStack(AllItems.BORAX.get()));
            if (slot < 0 && !player.getAbilities().instabuild) return InteractionResultHolder.fail(stack);
            if (!level.isClientSide) {
                if (slot >= 0 && !player.getAbilities().instabuild) player.getInventory().removeItem(slot, 1);
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - CHARGES_PER_BOX));
                level.playSound(null, player.blockPosition(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 0.6f, 1.4f);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.borax_sprayer").withStyle(ChatFormatting.GRAY));
        }
    }
}
