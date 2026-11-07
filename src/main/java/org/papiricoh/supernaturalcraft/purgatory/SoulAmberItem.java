package org.papiricoh.supernaturalcraft.purgatory;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.network.SNNetworking;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;

/**
 * Soul amber (v0.39): a lump of Purgatory's amber with a soul caught in it, mined from its stone (and, now and then, carried out of
 * it by a Leviathan or an Alpha). Two uses: the rite {@code open_purgatory_rift} burns it to tear the way in or home, and crushed
 * in the hand it gives up the soul's power, the way Castiel drank Purgatory's: {@link #MANA} mana back at once (a short cooldown;
 * not while the bar is full).
 */
public class SoulAmberItem extends Item {

    public static final float MANA = 25f;
    public static final int COOLDOWN = 10 * 20;

    public SoulAmberItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        ServerPlayer p = (ServerPlayer) player;
        ArcanaData a = ManaManager.get(p);
        if (a.mana() >= a.maxMana() - 0.5f) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.soul_amber.full").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        crush(p);
        if (!p.getAbilities().instabuild) stack.shrink(1);
        p.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResultHolder.consume(stack);
    }

    /** The soul's power, into {@code p}'s mana. */
    public static void crush(ServerPlayer p) {
        ArcanaData a = ManaManager.get(p);
        a.setMana(a.mana() + MANA);
        SNNetworking.syncArcana(p);
        ServerLevel level = p.serverLevel();
        level.sendParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 1.2, p.getZ(), 14, 0.3, 0.4, 0.3, 0.04);
        level.playSound(null, p.blockPosition(), AllSounds.purgatory("soul_death"), SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.supernaturalcraft.soul_amber", (int) MANA).withStyle(ChatFormatting.GRAY));
    }
}
