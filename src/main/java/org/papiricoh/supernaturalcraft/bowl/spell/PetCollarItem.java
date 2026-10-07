package org.papiricoh.supernaturalcraft.bowl.spell;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

/**
 * A collar for one of your tamed animals. Used on it, the collar is bound ({@link PetBond}) and the
 * animal wears its twin ({@link AllAttachments#COLLARED}); the item stays with you as the link the
 * Locating and Revive Pet spells need. The {@link PetLedger} follows every collared pet.
 */
public class PetCollarItem extends Item {

    public enum BindResult { BOUND, NOT_TAME, NOT_YOURS }

    public PetCollarItem(Properties props) {
        super(props);
    }

    /** The bond on {@code stack}, if it is a bound collar. */
    @Nullable
    public static PetBond bond(ItemStack stack) {
        return stack.is(AllItems.PET_COLLAR.get()) ? stack.get(AllDataComponents.PET_BOND.get()) : null;
    }

    /** A collar bound to {@code pet} (what a named pet that dies uncollared leaves behind). */
    public static ItemStack boundTo(TamableAnimal pet) {
        ItemStack collar = new ItemStack(AllItems.PET_COLLAR.get());
        collar.set(AllDataComponents.PET_BOND.get(), bondOf(pet));
        return collar;
    }

    static PetBond bondOf(TamableAnimal pet) {
        return new PetBond(pet.getUUID(), PetLedger.nameOf(pet), PetLedger.typeOf(pet), pet.getOwnerUUID());
    }

    /**
     * {@code player} puts {@code collar} on {@code target}. Works only on a tamed animal of theirs; the
     * collar is (re)bound to it and the ledger starts following it. Server side for the effects.
     */
    public static BindResult bind(ItemStack collar, Player player, LivingEntity target) {
        if (!(target instanceof TamableAnimal pet) || !pet.isTame() || pet.getOwnerUUID() == null) return BindResult.NOT_TAME;
        if (!player.getUUID().equals(pet.getOwnerUUID())) return BindResult.NOT_YOURS;
        if (player.level().isClientSide) return BindResult.BOUND;
        collar.set(AllDataComponents.PET_BOND.get(), bondOf(pet));
        pet.setData(AllAttachments.COLLARED, true);
        MinecraftServer server = player.level().getServer();
        if (server != null) PetLedger.get(server).seen(pet);
        player.level().playSound(null, pet.getX(), pet.getY(), pet.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.NEUTRAL, 1f, 1.2f);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HEART, pet.getX(), pet.getY() + pet.getBbHeight() + 0.3, pet.getZ(), 3, 0.3, 0.2, 0.3, 0);
        }
        player.displayClientMessage(Component.translatable("message.supernaturalcraft.pet_collar.bound", pet.getName()).withStyle(ChatFormatting.GOLD), true);
        return BindResult.BOUND;
    }

    /** Fallback for animals that do not claim the click themselves (the event handler usually runs first). */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return interact(stack, player, target);
    }

    static InteractionResult interact(ItemStack stack, Player player, LivingEntity target) {
        if (!(target instanceof TamableAnimal)) return InteractionResult.PASS;
        BindResult r = bind(stack, player, target);
        if (r == BindResult.BOUND) return InteractionResult.sidedSuccess(player.level().isClientSide);
        if (!player.level().isClientSide) {
            player.displayClientMessage(Component.translatable(r == BindResult.NOT_YOURS
                    ? "message.supernaturalcraft.pet_collar.not_yours" : "message.supernaturalcraft.pet_collar.not_tame")
                    .withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResult.FAIL;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(AllDataComponents.PET_BOND.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        PetBond bond = stack.get(AllDataComponents.PET_BOND.get());
        if (bond != null) {
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.pet_collar.bound", bond.name()).withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.pet_collar.unbound").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.pet_collar.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
