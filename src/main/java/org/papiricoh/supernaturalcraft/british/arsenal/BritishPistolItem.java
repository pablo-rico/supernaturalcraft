package org.papiricoh.supernaturalcraft.british.arsenal;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.balance.WeaponDamage;
import org.papiricoh.supernaturalcraft.british.BritishRegistry;
import org.papiricoh.supernaturalcraft.weapon.ascension.Ascension;

import java.util.List;

/**
 * The British Men of Letters' suppressed pistol (v0.41): a shot along the hunter's line of sight that spends one
 * {@code enchanted_round} (creative spends none). What it hits takes {@link ArsenalRules#PISTOL_DAMAGE} at the arsenal's scale,
 * raised by the pistol's ascension, and the round's hex ({@link Hex#mark}; a boss takes only the damage, through its own
 * pipeline).
 */
public class BritishPistolItem extends Item {

    public BritishPistolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ItemStack ammo = findAmmo(player);
        if (ammo.isEmpty() && !player.getAbilities().instabuild) {
            level.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 1.6f);
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.supernaturalcraft.british_pistol.empty").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (level instanceof ServerLevel server) {
            fire(server, player, stack);
            if (!player.getAbilities().instabuild) ammo.shrink(1);
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
        player.getCooldowns().addCooldown(this, ArsenalRules.PISTOL_COOLDOWN);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** The shot: whatever living thing is first along the line of sight, before a wall. @return what it hit, or null */
    public static @Nullable LivingEntity fire(ServerLevel level, Player player, ItemStack pistol) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1f);
        Vec3 end = eye.add(look.scale(ArsenalRules.PISTOL_RANGE));
        BlockHitResult wall = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (wall.getType() != HitResult.Type.MISS) end = wall.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, end, new AABB(eye, end).inflate(1.0),
                e -> e instanceof LivingEntity l && l.isAlive() && !e.isSpectator() && e != player, 0.3f);
        Vec3 stop = hit != null ? hit.getLocation() : end;
        double length = stop.distanceTo(eye);
        for (double d = 1.0; d < length; d += 0.8) {
            Vec3 q = eye.add(look.scale(d));
            level.sendParticles(ParticleTypes.CRIT, q.x, q.y - 0.1, q.z, 1, 0, 0, 0, 0);
        }
        level.playSound(null, player.blockPosition(), BritishRegistry.sound("ketch.shot"), SoundSource.PLAYERS, 0.8f, 1.2f);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) return null;
        float damage = WeaponDamage.ability(Ascension.scale(pistol, ArsenalRules.PISTOL_DAMAGE));
        Entity e = target;
        if (e.hurt(player.damageSources().playerAttack(player), damage)) Hex.mark(target);
        level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 8, 0.2, 0.3, 0.2, 0.02);
        return target;
    }

    static ItemStack findAmmo(Player player) {
        ItemStack off = player.getOffhandItem();
        if (off.is(BritishRegistry.ENCHANTED_ROUND.get())) return off;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(BritishRegistry.ENCHANTED_ROUND.get())) return s;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.british_pistol").withStyle(ChatFormatting.GRAY));
    }
}
