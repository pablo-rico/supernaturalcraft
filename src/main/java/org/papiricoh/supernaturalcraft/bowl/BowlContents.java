package org.papiricoh.supernaturalcraft.bowl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

/**
 * What a spell bowl holds: up to {@link #MAX_ITEMS} ingredients (one of each stack, in the order
 * they went in) and up to {@link #MAX_DOSES} doses of liquid. Immutable with value equality (the
 * items live in an {@link ItemContainerContents}), as a data component must be: it travels on the
 * bowl item when the bowl is picked up and back into the block entity when it is set down.
 */
public record BowlContents(ItemContainerContents items, List<Dose> liquids) {

    public static final int MAX_ITEMS = 8;
    public static final int MAX_DOSES = 4;
    public static final BowlContents EMPTY = new BowlContents(ItemContainerContents.EMPTY, List.of());

    public static final Codec<BowlContents> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemContainerContents.CODEC.optionalFieldOf("items", ItemContainerContents.EMPTY).forGetter(BowlContents::items),
            Dose.CODEC.listOf().optionalFieldOf("liquids", List.of()).forGetter(BowlContents::liquids)
    ).apply(i, BowlContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BowlContents> STREAM_CODEC = StreamCodec.composite(
            ItemContainerContents.STREAM_CODEC, BowlContents::items,
            Dose.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_DOSES)), BowlContents::liquids,
            BowlContents::new);

    public BowlContents {
        liquids = List.copyOf(liquids);
    }

    public static BowlContents of(List<ItemStack> items, List<Dose> liquids) {
        return new BowlContents(ItemContainerContents.fromItems(items.stream().filter(s -> !s.isEmpty()).toList()), liquids);
    }

    /** Copies of the ingredients, in the order they went in. */
    public List<ItemStack> stacks() {
        List<ItemStack> out = new ArrayList<>();
        items.nonEmptyItems().forEach(s -> out.add(s.copy()));
        return out;
    }

    public int itemCount() {
        return (int) items.nonEmptyStream().count();
    }

    public List<BowlLiquid> liquidKinds() {
        return liquids.stream().map(Dose::kind).toList();
    }

    public boolean isEmpty() {
        return itemCount() == 0 && liquids.isEmpty();
    }

    public boolean canAddItem() {
        return itemCount() < MAX_ITEMS;
    }

    public boolean canAddDose() {
        return liquids.size() < MAX_DOSES;
    }

    /** One of {@code stack} added (count forced to 1). Caller checks {@link #canAddItem()}. */
    public BowlContents withItem(ItemStack stack) {
        List<ItemStack> list = stacks();
        list.add(stack.copyWithCount(1));
        return of(list, liquids);
    }

    public BowlContents withoutLastItem() {
        List<ItemStack> list = stacks();
        if (list.isEmpty()) return this;
        list.removeLast();
        return of(list, liquids);
    }

    public ItemStack lastItem() {
        List<ItemStack> list = stacks();
        return list.isEmpty() ? ItemStack.EMPTY : list.getLast();
    }

    public BowlContents withDose(Dose dose) {
        List<Dose> list = new ArrayList<>(liquids);
        list.add(dose);
        return new BowlContents(items, list);
    }

    public BowlContents withoutLastDose() {
        if (liquids.isEmpty()) return this;
        return new BowlContents(items, liquids.subList(0, liquids.size() - 1));
    }

    public Dose lastDose() {
        return liquids.isEmpty() ? null : liquids.getLast();
    }

    public BowlContents withoutLiquids() {
        return new BowlContents(items, List.of());
    }

    /** The colour the liquids make together (0xRRGGBB). */
    public int mixColor() {
        return BowlMix.mixColor(liquids.stream().map(Dose::color).toList(), liquids.stream().map(d -> d.kind().tint).toList());
    }
}
