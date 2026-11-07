package org.papiricoh.supernaturalcraft.pact.item;

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
import org.papiricoh.supernaturalcraft.leviathan.BoraxSensitive;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.weapon.melee.AngelicBladeAssets;
import org.papiricoh.supernaturalcraft.weapon.melee.AngelicBladeItem;

import java.util.List;

/**
 * The Blade of the New God (v0.39): the blade Castiel carried when he called himself God, still full of what he swallowed.
 * An angelic blade (demon bane, holy). Used, it lets a little of Purgatory's light go in a wave ahead: souls that sear
 * whatever they pass through, and twice as hard anything the Leviathans' borax would burn.
 */
public class NewGodBladeItem extends AngelicBladeItem {

    public static final String ID = "new_god_blade";
    public static final float WAVE_MANA = 35f, WAVE_DAMAGE = 12f, WAVE_RANGE = 8f, LEVIATHAN_FACTOR = 2f;
    public static final int COOLDOWN = 50;

    public NewGodBladeItem(Tier tier, Properties properties) {
        super(ID, tier, 3.5f, false, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.consume(stack);
        if (!ManaManager.tryConsume(sp, WAVE_MANA)) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.cast.no_mana").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        ServerLevel server = sp.serverLevel();
        Vec3 look = sp.getLookAngle().multiply(1, 0, 1).normalize();
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().inflate(WAVE_RANGE))) {
            Vec3 to = e.position().subtract(sp.position()).multiply(1, 0, 1);
            if (e == sp || ResolvedSpell.isFriend(sp, e) || to.length() > WAVE_RANGE || to.normalize().dot(look) < 0.6) continue;
            float base = WAVE_DAMAGE * (e instanceof BoraxSensitive ? LEVIATHAN_FACTOR : 1f);
            e.hurt(AllDamageTypes.source(server, AllDamageTypes.GRACE, sp),
                    org.papiricoh.supernaturalcraft.weapon.ascension.Ascension.weaponAbility(stack, base));
        }
        for (int i = 1; i <= 8; i++) {
            Vec3 p = sp.position().add(0, 1.1, 0).add(look.scale(i));
            server.sendParticles(AllParticles.GRACE.get(), p.x, p.y, p.z, 8, 0.25 * i / 2, 0.35, 0.25 * i / 2, 0.02);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, p.x, p.y, p.z, 2, 0.2 * i / 2, 0.3, 0.2 * i / 2, 0.02);
        }
        server.playSound(null, sp.blockPosition(), AllSounds.godstiel("smite"), SoundSource.PLAYERS, 0.8f, 1.3f);
        sp.getCooldowns().addCooldown(this, COOLDOWN);
        sp.swing(hand, true);
        play(sp, stack, AngelicBladeAssets.FLASH);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.new_god_blade", Math.round(WAVE_MANA)).withStyle(ChatFormatting.AQUA));
    }
}
