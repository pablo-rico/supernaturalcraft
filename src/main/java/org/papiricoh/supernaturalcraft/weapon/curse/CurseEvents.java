package org.papiricoh.supernaturalcraft.weapon.curse;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.weapon.catalyst.WhisperingCodexItem;
import org.papiricoh.supernaturalcraft.weapon.melee.FirstBladeItem;

/** Hungry weapons in a fight: who they hurt, what they eat, and the Mark that won't let you die. */
public final class CurseEvents {

    public static final float SATED_BONUS = 1.15f, MARKED_BONUS = 1.2f;
    public static final int MARK_COOLDOWN = 6000;

    private CurseEvents() {
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.hasEffect(AllMobEffects.MARKED)) event.setAmount(event.getAmount() * MARKED_BONUS);
        if (!(event.getSource().getDirectEntity() instanceof ServerPlayer attacker) || event.getSource().getEntity() != attacker) return;
        ItemStack held = attacker.getMainHandItem();
        if (!(held.getItem() instanceof FirstBladeItem)) return;
        CurseState s = Curses.state(held);
        if (!s.ownedBy(attacker.getUUID())) {
            event.setAmount(Math.min(event.getAmount(), 1f));
            return;
        }
        if (CurseLevels.sated(s.satiation())) event.setAmount(event.getAmount() * SATED_BONUS);
    }

    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer dying && tryMark(dying)) {
            event.setCanceled(true);
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        LivingEntity dead = event.getEntity();
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = killer.getItemInHand(hand);
            boolean melee = hand == InteractionHand.MAIN_HAND && stack.getItem() instanceof FirstBladeItem
                    && event.getSource().getDirectEntity() == killer;
            boolean magic = stack.getItem() instanceof WhisperingCodexItem && event.getSource().getDirectEntity() != killer;
            if ((melee || magic) && Curses.state(stack).ownedBy(killer.getUUID())) {
                Curses.feed(killer, stack, dead.getMaxHealth());
                if (melee && Curses.state(stack).level() >= 2) killer.heal(2f);
            }
        }
    }

    /** Level-five First Blade, well fed: a killing blow leaves you standing at one heart, once in a while. */
    static boolean tryMark(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof FirstBladeItem)) continue;
            CurseState s = Curses.state(stack);
            long now = player.level().getGameTime();
            if (s.level() >= 5 && CurseLevels.sated(s.satiation()) && s.ownedBy(player.getUUID()) && now - s.lastMarkTick() >= MARK_COOLDOWN) {
                stack.set(org.papiricoh.supernaturalcraft.registry.AllDataComponents.CURSE, s.markUsed(now));
                player.setHealth(1f);
                player.serverLevel().sendParticles(AllParticles.DEMON_SMOKE.get(), player.getX(), player.getY() + 1, player.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
                player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1f, 0.5f);
                player.displayClientMessage(Component.translatable("message.supernaturalcraft.curse.mark").withStyle(ChatFormatting.DARK_RED), true);
                return true;
            }
        }
        return false;
    }
}
