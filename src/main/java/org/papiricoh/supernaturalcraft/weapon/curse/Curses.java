package org.papiricoh.supernaturalcraft.weapon.curse;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;

/**
 * The appetite shared by every hungry weapon: it grows hungry while carried, feeds on kills, and
 * punishes a starving bearer.
 */
public final class Curses {

    public static final int STARVE_INTERVAL = 100, WHISPERS = 8;

    private Curses() {
    }

    public static CurseState state(ItemStack stack) {
        return stack.getOrDefault(AllDataComponents.CURSE, CurseState.FRESH);
    }

    /** Called every tick the weapon sits in a player's inventory. */
    public static void tickCarried(ServerPlayer player, ItemStack stack) {
        CurseState s = state(stack);
        if (s.owner().isPresent() && !s.ownedBy(player.getUUID())) {
            if (player.tickCount % 20 == 0) rejects(player);
            return;
        }
        if (s.owner().isEmpty()) stack.set(AllDataComponents.CURSE, s = s.boundTo(player.getUUID()));
        long time = player.level().getGameTime();
        // A Knight of Hell's own blade (v0.13) hungers no more: the Knight's bloodlust takes its place.
        if (time % CurseLevels.HUNGER_INTERVAL == 0 && !org.papiricoh.supernaturalcraft.allegiance.AllegianceCombat.knightsBlade(player, stack)) {
            stack.set(AllDataComponents.CURSE, s = s.hungrier());
        }
        if (CurseLevels.starving(s.satiation()) && time % STARVE_INTERVAL == 0) starve(player, s);
    }

    /** One pulse of a starving weapon's hunger. Public for tests. */
    public static void starve(ServerPlayer player, CurseState s) {
        player.hurt(player.damageSources().magic(), 1f);
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
        if (s.satiation() == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 1));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
        }
        whisper(player);
    }

    public static void whisper(ServerPlayer player) {
        int line = player.getRandom().nextInt(WHISPERS);
        player.displayClientMessage(Component.translatable("message.supernaturalcraft.whisper." + line)
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.15f, 0.5f);
    }

    /** A bound weapon in the wrong hands burns them. */
    public static void rejects(ServerPlayer player) {
        player.hurt(player.damageSources().inFire(), 1f);
        player.displayClientMessage(Component.translatable("message.supernaturalcraft.curse.rejects")
                .withStyle(ChatFormatting.DARK_RED), true);
    }

    /** Feeds the weapon; tells the bearer when it grows stronger. */
    public static void feed(ServerPlayer player, ItemStack stack, float victimMaxHealth) {
        CurseState before = state(stack);
        CurseState after = before.fed(CurseLevels.soulsFor(victimMaxHealth));
        stack.set(AllDataComponents.CURSE, after);
        if (after.level() > before.level()) {
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.curse.level", stack.getHoverName(), after.level())
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.5f, 0.7f);
        }
    }
}
