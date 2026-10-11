package org.papiricoh.supernaturalcraft.arena;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * What a summoning put up for a fight (v0.19): the progression keys it spent and whose they were. A fight that ends without a
 * victory (lost, abandoned, cut short by a restart) gives them back; {@code always} gives them back after a victory too (the
 * relics Michael and Death are only shown, never taken).
 *
 * @param owner who summoned
 * @param items the stacks to give back
 * @param always whether a victory gives them back as well
 */
public record Stake(UUID owner, List<ItemStack> items, boolean always) {

    public static final Codec<Stake> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(Stake::owner),
            ItemStack.CODEC.listOf().fieldOf("items").forGetter(Stake::items),
            Codec.BOOL.optionalFieldOf("always", false).forGetter(Stake::always)
    ).apply(i, Stake::new));

    public Stake {
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack s : items) if (!s.isEmpty()) copy.add(s.copy());
        items = List.copyOf(copy);
    }

    public Stake lent() {
        return new Stake(owner, items, true);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
