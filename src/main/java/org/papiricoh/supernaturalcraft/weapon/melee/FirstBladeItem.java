package org.papiricoh.supernaturalcraft.weapon.melee;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.weapon.curse.CurseLevels;
import org.papiricoh.supernaturalcraft.weapon.curse.CurseState;
import org.papiricoh.supernaturalcraft.weapon.curse.Curses;

import java.util.List;

/**
 * Tier IV, cursed. A blade of old bone that answers only to the one who forged it. It grows hungry,
 * feeds on kills and grows stronger: L2 kills heal, L3 a lunge, L4 bleeding wounds, L5 the Mark —
 * well fed, it will not let its bearer die (once every five minutes).
 */
public class FirstBladeItem extends GeoSwordItem {

    public static final int LUNGE_COOLDOWN = 60, BLEED_TICKS = 60;

    public FirstBladeItem(Tier tier, Properties properties) {
        super("first_blade", tier, properties.component(AllDataComponents.CURSE, CurseState.FRESH), List.of(), List.of("hunger_pulse", "lunge"));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (entity instanceof ServerPlayer player) {
            boolean wasStarving = CurseLevels.starving(Curses.state(stack).satiation());
            Curses.tickCarried(player, stack);
            if (wasStarving && level.getGameTime() % Curses.STARVE_INTERVAL == 0) play(player, stack, "hunger_pulse");
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        CurseState s = Curses.state(stack);
        if (s.level() >= 4 && s.ownedBy(attacker.getUUID())) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, BLEED_TICKS, 0), attacker);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CurseState s = Curses.state(stack);
        if (s.level() < 3 || !s.ownedBy(player.getUUID())) return InteractionResultHolder.pass(stack);
        if (player instanceof ServerPlayer sp) {
            AngelBladeItem.dash(sp, stack, 0.8f);
            play(sp, stack, "lunge");
            sp.getCooldowns().addCooldown(this, LUNGE_COOLDOWN);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        CurseState s = Curses.state(stack);
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.curse.state", s.level(), s.souls(), s.satiation())
                .withStyle(CurseLevels.starving(s.satiation()) ? ChatFormatting.DARK_RED : ChatFormatting.RED));
        if (s.owner().isPresent()) tooltip.add(Component.translatable("tooltip.supernaturalcraft.curse.bound").withStyle(ChatFormatting.DARK_GRAY));
    }
}
