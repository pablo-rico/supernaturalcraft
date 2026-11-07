package org.papiricoh.supernaturalcraft.entity.boss.eve;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.entity.legacy.ShapeshifterEntity;
import org.papiricoh.supernaturalcraft.entity.legacy.VampireEntity;
import org.papiricoh.supernaturalcraft.entity.legacy.WerewolfEntity;
import org.papiricoh.supernaturalcraft.purgatory.mob.PurgatorySoul;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.List;
import java.util.UUID;

/**
 * The Mother's Heartwood (v0.39): Eve's heart, gone to wood when she fell, still warm. Held up, it speaks with her voice: every one of
 * her children near (vampires, werewolves, shapeshifters and Purgatory's souls; never an Alpha or a great enemy) forgets the one
 * who holds it for {@link #HUSH_TICKS}, and will not take them for prey again until then ({@code PurgatoryEvents} keeps it).
 */
public class MothersHeartwoodItem extends Item {

    public static final double REACH = 20;
    public static final int HUSH_TICKS = 30 * 20, COOLDOWN = 120 * 20;
    static final String UNTIL = "sn_hushed_until", BY = "sn_hushed_by";

    public MothersHeartwoodItem(Properties properties) {
        super(properties);
    }

    /** Whether {@code e} is one of Eve's children the Heartwood hushes. */
    public static boolean child(LivingEntity e) {
        if (e.getType().is(AllTags.Entities.BOSSES) || e instanceof org.papiricoh.supernaturalcraft.entity.alpha.Alpha) return false;
        return e instanceof PurgatorySoul || e instanceof VampireEntity || e instanceof WerewolfEntity || e instanceof ShapeshifterEntity;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        int n = hush((ServerLevel) level, player);
        player.getCooldowns().addCooldown(this, COOLDOWN);
        level.playSound(null, player.blockPosition(), AllSounds.purgatory("eve_whisper"), SoundSource.PLAYERS, 1.2f, 1.1f);
        player.displayClientMessage(Component.translatable("message.supernaturalcraft.mothers_heartwood.hush", n)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
        return InteractionResultHolder.consume(stack);
    }

    /** Hushes every child near {@code holder}. @return how many */
    public static int hush(ServerLevel level, Player holder) {
        long until = level.getGameTime() + HUSH_TICKS;
        int n = 0;
        for (Mob m : level.getEntitiesOfClass(Mob.class, holder.getBoundingBox().inflate(REACH), m -> m.isAlive() && child(m))) {
            CompoundTag d = m.getPersistentData();
            d.putLong(UNTIL, until);
            d.putUUID(BY, holder.getUUID());
            if (m.getTarget() == holder) m.setTarget(null);
            level.sendParticles(ParticleTypes.WAX_ON, m.getX(), m.getEyeY(), m.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
            n++;
        }
        return n;
    }

    /** Whether {@code mob} is hushed toward {@code target} right now. */
    public static boolean hushed(Mob mob, LivingEntity target) {
        CompoundTag d = mob.getPersistentData();
        if (!d.contains(UNTIL) || !d.hasUUID(BY)) return false;
        UUID by = d.getUUID(BY);
        return by.equals(target.getUUID()) && mob.level().getGameTime() < d.getLong(UNTIL);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.supernaturalcraft.mothers_heartwood").withStyle(ChatFormatting.GRAY));
    }
}
