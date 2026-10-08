package org.papiricoh.supernaturalcraft.reward;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.hunter.HunterBladeItem;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;

/** Lucifer's own blade. A holy weapon, deadly to demons, and it smites in a cone on use. */
public class ArchangelBladeItem extends HunterBladeItem {

    public static final float SMITE_MANA = 30f, SMITE_DAMAGE = 10f, SMITE_RANGE = 6f;
    public static final int COOLDOWN = 40;

    public ArchangelBladeItem(Tier tier, Properties properties) {
        super(tier, 4.0f, true, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.consume(stack);
        if (!ManaManager.tryConsume(sp, SMITE_MANA)) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.cast.no_mana").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        ServerLevel server = sp.serverLevel();
        Vec3 look = sp.getLookAngle().multiply(1, 0, 1).normalize();
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().inflate(SMITE_RANGE))) {
            Vec3 to = e.position().subtract(sp.position()).multiply(1, 0, 1);
            if (e == sp || ResolvedSpell.isFriend(sp, e) || to.length() > SMITE_RANGE || to.normalize().dot(look) < 0.5) continue;
            e.hurt(AllDamageTypes.source(server, AllDamageTypes.SMITE, sp),
                    org.papiricoh.supernaturalcraft.weapon.ascension.Ascension.scale(stack, SMITE_DAMAGE));
        }
        for (int i = 1; i <= 6; i++) {
            Vec3 p = sp.position().add(0, 1.2, 0).add(look.scale(i));
            server.sendParticles(AllParticles.GRACE.get(), p.x, p.y, p.z, 6, 0.3 * i / 2, 0.3, 0.3 * i / 2, 0.02);
        }
        server.playSound(null, sp.blockPosition(), AllSounds.LUCIFER_SMITE.get(), SoundSource.PLAYERS, 0.8f, 1.6f);
        sp.getCooldowns().addCooldown(this, COOLDOWN);
        sp.swing(hand, true);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.archangel_blade", Math.round(SMITE_MANA)).withStyle(ChatFormatting.GOLD));
    }
}
