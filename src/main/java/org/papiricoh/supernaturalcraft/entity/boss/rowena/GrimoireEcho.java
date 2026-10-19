package org.papiricoh.supernaturalcraft.entity.boss.rowena;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.bowl.BowlContents;

/**
 * What Rowena's Grimoire remembers (v0.21, P6; the {@code grimoire_echo} component): the last aimed bowl spell its bearer spoke
 * at a bowl, as the recipe and what was in the bowl (whose taglock or blood named the target).
 */
public record GrimoireEcho(ResourceLocation recipe, BowlContents contents) {

    public static final Codec<GrimoireEcho> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("recipe").forGetter(GrimoireEcho::recipe),
            BowlContents.CODEC.fieldOf("contents").forGetter(GrimoireEcho::contents)
    ).apply(i, GrimoireEcho::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GrimoireEcho> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, GrimoireEcho::recipe,
            BowlContents.STREAM_CODEC, GrimoireEcho::contents,
            GrimoireEcho::new);
}
