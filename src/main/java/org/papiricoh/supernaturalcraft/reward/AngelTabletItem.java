package org.papiricoh.supernaturalcraft.reward;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.List;

/**
 * The Angel Tablet, taken from Metatron still charged: it rewrites its holder's story. Fully healed,
 * every harm undone, the fire put out. Two minutes before it will write again.
 */
public class AngelTabletItem extends LoreItem {

    public static final int COOLDOWN = 20 * 120;

    public AngelTabletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        rewrite(sp);
        server.sendParticles(AllParticles.GRACE.get(), sp.getX(), sp.getY() + 1, sp.getZ(), 50, 0.5, 0.8, 0.5, 0.1);
        server.sendParticles(AllParticles.PAGE.get(), sp.getX(), sp.getY() + 1, sp.getZ(), 20, 0.6, 0.8, 0.6, 0.05);
        server.playSound(null, sp.blockPosition(), AllSounds.ANGEL_TABLET_USE.get(), SoundSource.PLAYERS, 1.5f, 1.2f);
        sp.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResultHolder.consume(stack);
    }

    /** Heals {@code player} whole, lifts every harmful effect and puts out any fire. */
    public static void rewrite(Player player) {
        player.setHealth(player.getMaxHealth());
        player.clearFire();
        List<Holder<MobEffect>> harmful = new ArrayList<>();
        for (MobEffectInstance e : player.getActiveEffects()) {
            if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) harmful.add(e.getEffect());
        }
        harmful.forEach(player::removeEffect);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.angel_tablet.use").withStyle(ChatFormatting.GOLD));
    }
}
