package org.papiricoh.supernaturalcraft.legacy.artifact;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.legacy.gen.ArtifactGenerator;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/** Making and finding cursed artifacts (v0.17). Case rewards and chest loot call {@link #roll}. */
public final class Artifacts {

    private Artifacts() {
    }

    /** An unidentified cursed artifact rolled from {@code seed}, rarity at most {@code maxRarity} (0 common … 3 legendary). */
    public static ItemStack roll(long seed, int maxRarity) {
        return of(ArtifactGenerator.roll(seed, maxRarity));
    }

    public static ItemStack of(ArtifactData data) {
        ItemStack stack = new ItemStack(AllItems.CURSED_ARTIFACT.get());
        stack.set(AllDataComponents.ARTIFACT.get(), data);
        return stack;
    }

    public static @Nullable ArtifactData data(ItemStack stack) {
        return stack.isEmpty() ? null : stack.get(AllDataComponents.ARTIFACT.get());
    }

    /** The first artifact in the inventory rolled from {@code seed}, or EMPTY. */
    public static ItemStack find(Player player, long seed) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            ArtifactData d = data(s);
            if (d != null && d.seed() == seed) return s;
        }
        return ItemStack.EMPTY;
    }
}
