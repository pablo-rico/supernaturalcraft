package org.papiricoh.supernaturalcraft.ritual;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import org.papiricoh.supernaturalcraft.registry.AllRecipes;

import java.util.List;
import java.util.stream.Stream;

/**
 * A book and quill, or a written book, with a name written in it ({@code "type": "supernaturalcraft:written_name",
 * "name": "Metatron"}). Shown in JEI as a book and quill with the name on its first page.
 */
public record WrittenNameIngredient(String name) implements ICustomIngredient {

    public static final MapCodec<WrittenNameIngredient> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(WrittenNameIngredient::name)).apply(i, WrittenNameIngredient::new));

    @Override
    public boolean test(ItemStack stack) {
        WritableBookContent writable = stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
        if (stack.is(Items.WRITABLE_BOOK) && writable != null) {
            return WrittenName.holds(writable.pages().stream().map(Filterable::raw).toList(), null, name);
        }
        WrittenBookContent written = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (stack.is(Items.WRITTEN_BOOK) && written != null) {
            List<String> pages = written.pages().stream().map(p -> p.raw().getString()).toList();
            return WrittenName.holds(pages, written.title().raw(), name);
        }
        return false;
    }

    @Override
    public Stream<ItemStack> getItems() {
        ItemStack book = new ItemStack(Items.WRITABLE_BOOK);
        book.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(List.of(Filterable.passThrough(name))));
        book.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return Stream.of(book);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return AllRecipes.WRITTEN_NAME.get();
    }
}
