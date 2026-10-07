package org.papiricoh.supernaturalcraft.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One piece of a journal entry, laid out down the pages in order ({@code JournalLayout}). Text
 * flows on to the next page; pictures never split. Text and captions are translation keys.
 */
public sealed interface JournalBlock {

    String type();

    /** A paragraph. */
    record Text(String text) implements JournalBlock {
        public static final MapCodec<Text> CODEC = Codec.STRING.fieldOf("text").xmap(Text::new, Text::text);

        public String type() {
            return "text";
        }
    }

    /** A creature, drawn turning slowly, with an optional caption. */
    record Entity(ResourceLocation entity, Optional<String> caption, float scale) implements JournalBlock {
        public static final MapCodec<Entity> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceLocation.CODEC.fieldOf("entity").forGetter(Entity::entity),
                Codec.STRING.optionalFieldOf("caption").forGetter(Entity::caption),
                Codec.floatRange(0.1f, 4f).optionalFieldOf("scale", 1f).forGetter(Entity::scale)
        ).apply(i, Entity::new));

        public String type() {
            return "entity";
        }
    }

    /** A row of items, each with its tooltip. */
    record Items(List<ResourceLocation> items, Optional<String> caption) implements JournalBlock {
        public static final MapCodec<Items> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceLocation.CODEC.listOf(1, 9).fieldOf("items").forGetter(Items::items),
                Codec.STRING.optionalFieldOf("caption").forGetter(Items::caption)
        ).apply(i, Items::new));

        public String type() {
            return "items";
        }
    }

    /** A recipe from the recipe manager: crafting, ritual or bowl spell. */
    record Recipe(ResourceLocation recipe, Optional<String> caption) implements JournalBlock {
        public static final MapCodec<Recipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceLocation.CODEC.fieldOf("recipe").forGetter(Recipe::recipe),
                Codec.STRING.optionalFieldOf("caption").forGetter(Recipe::caption)
        ).apply(i, Recipe::new));

        public String type() {
            return "recipe";
        }
    }

    /** A picture from a texture (a {@code textures/...png} path), drawn at its size in book pixels. */
    record Image(ResourceLocation texture, int width, int height, Optional<String> caption) implements JournalBlock {
        public static final MapCodec<Image> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceLocation.CODEC.fieldOf("texture").forGetter(Image::texture),
                Codec.intRange(1, 512).fieldOf("width").forGetter(Image::width),
                Codec.intRange(1, 512).fieldOf("height").forGetter(Image::height),
                Codec.STRING.optionalFieldOf("caption").forGetter(Image::caption)
        ).apply(i, Image::new));

        public String type() {
            return "image";
        }
    }

    Map<String, MapCodec<? extends JournalBlock>> TYPES = Map.of(
            "text", Text.CODEC, "entity", Entity.CODEC, "items", Items.CODEC, "recipe", Recipe.CODEC, "image", Image.CODEC);

    Codec<JournalBlock> CODEC = Codec.STRING.dispatch("type", JournalBlock::type, TYPES::get);
}
