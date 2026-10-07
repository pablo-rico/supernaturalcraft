package org.papiricoh.supernaturalcraft.bowl.spell;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlCast;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlSpellEffect;

import java.util.Optional;

/**
 * The Revive Pet spell: the bound collar among the ingredients names a pet the {@link PetLedger}
 * remembers dead; it comes back whole beside the bowl, and the collar comes back to the caster.
 */
public record RevivePetEffect() implements BowlSpellEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("revive_pet");
    public static final RevivePetEffect INSTANCE = new RevivePetEffect();
    public static final MapCodec<RevivePetEffect> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Nullable
    @Override
    public String precheck(BowlCast cast) {
        Optional<ItemStack> collar = LocateEffect.collar(cast);
        if (collar.isEmpty()) return "message.supernaturalcraft.revive.no_collar";
        return PetRevivals.whyNot(cast.level(), PetCollarItem.bond(collar.get()).pet());
    }

    @Override
    public boolean perform(BowlCast cast) {
        Optional<ItemStack> collar = LocateEffect.collar(cast);
        if (collar.isEmpty()) {
            cast.caster().displayClientMessage(Component.translatable("message.supernaturalcraft.revive.no_collar").withStyle(ChatFormatting.GRAY), false);
            return false;
        }
        PetBond bond = PetCollarItem.bond(collar.get());
        String why = PetRevivals.whyNot(cast.level(), bond.pet());
        if (why != null) {
            cast.caster().displayClientMessage(Component.translatable(why).withStyle(ChatFormatting.GRAY), false);
            return false;
        }
        Vec3 at = Vec3.atBottomCenterOf(cast.bowl().above());
        Entity pet = PetRevivals.revive(cast.level(), bond.pet(), at);
        if (pet == null) return false;
        cast.giveBack(collar.get());
        cast.caster().displayClientMessage(Component.translatable("message.supernaturalcraft.revive.back", pet.getName()).withStyle(ChatFormatting.GOLD), true);
        return true;
    }
}
