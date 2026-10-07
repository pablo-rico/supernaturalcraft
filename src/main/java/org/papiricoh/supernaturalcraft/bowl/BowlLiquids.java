package org.papiricoh.supernaturalcraft.bowl;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.bowl.spell.BloodSample;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.Optional;

/** Which held items pour into a bowl as which liquid, and what a glass bottle scoops back out. */
public final class BowlLiquids {

    private BowlLiquids() {
    }

    /** The dose {@code stack} pours into a bowl, or null if it is not a liquid the bowl takes. */
    @Nullable
    public static Dose fromStack(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.is(Items.POTION)) {
            PotionContents potion = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            if (potion.is(Potions.WATER)) return Dose.of(BowlLiquid.WATER);
            return Dose.tinted(BowlLiquid.POTION, potion.getColor() & 0xFFFFFF);
        }
        if (stack.is(AllItems.HOLY_WATER.get())) return Dose.of(BowlLiquid.HOLY_WATER);
        if (stack.is(AllItems.DEMON_BLOOD.get())) return Dose.of(BowlLiquid.DEMON_BLOOD);
        if (stack.is(AllItems.BLOOD_VIAL.get())) {
            BloodSample sample = stack.get(AllDataComponents.BLOOD_SAMPLE.get());
            return sample == null ? Dose.of(BowlLiquid.BLOOD) : Dose.blood(sample.owner(), sample.name());
        }
        if (stack.is(Items.HONEY_BOTTLE)) return Dose.of(BowlLiquid.HONEY);
        if (stack.is(Items.DRAGON_BREATH)) return Dose.of(BowlLiquid.DRAGON_BREATH);
        return null;
    }

    /** What is left in the hand after pouring {@code dose}: the empty bottle, or nothing. */
    public static ItemStack remainder(Dose dose) {
        return dose.kind().bottled ? new ItemStack(Items.GLASS_BOTTLE) : ItemStack.EMPTY;
    }

    /**
     * {@code dose} scooped back up with a glass bottle. A blood dose keeps whose blood it is. A
     * potion dose comes back as a Mundane Potion in the same colour: its effects are lost in the
     * bowl (a dose only remembers its colour).
     */
    public static ItemStack bottle(Dose dose) {
        return switch (dose.kind()) {
            case WATER -> PotionContents.createItemStack(Items.POTION, Potions.WATER);
            case HOLY_WATER -> new ItemStack(AllItems.HOLY_WATER.get());
            case DEMON_BLOOD -> new ItemStack(AllItems.DEMON_BLOOD.get());
            case HONEY -> new ItemStack(Items.HONEY_BOTTLE);
            case DRAGON_BREATH -> new ItemStack(Items.DRAGON_BREATH);
            case BLOOD -> {
                ItemStack vial = new ItemStack(AllItems.BLOOD_VIAL.get());
                if (dose.owner().isPresent()) {
                    vial.set(AllDataComponents.BLOOD_SAMPLE.get(), new BloodSample(dose.owner().get(), dose.ownerName().orElse("?")));
                }
                yield vial;
            }
            case POTION -> {
                ItemStack potion = new ItemStack(Items.POTION);
                potion.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.of(Potions.MUNDANE),
                        Optional.of(0xFF000000 | dose.color()), java.util.List.of()));
                yield potion;
            }
        };
    }
}
