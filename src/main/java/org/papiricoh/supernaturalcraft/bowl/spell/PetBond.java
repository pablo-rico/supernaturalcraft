package org.papiricoh.supernaturalcraft.bowl.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** Which tamed animal a collar belongs to: its UUID, its name and its kind, and its owner. */
public record PetBond(UUID pet, String name, ResourceLocation type, UUID owner) {

    public static final Codec<PetBond> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("pet").forGetter(PetBond::pet),
            Codec.STRING.fieldOf("name").forGetter(PetBond::name),
            ResourceLocation.CODEC.fieldOf("type").forGetter(PetBond::type),
            UUIDUtil.CODEC.fieldOf("owner").forGetter(PetBond::owner)
    ).apply(i, PetBond::new));

    public static final StreamCodec<ByteBuf, PetBond> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, PetBond::pet,
            ByteBufCodecs.STRING_UTF8, PetBond::name,
            ResourceLocation.STREAM_CODEC, PetBond::type,
            UUIDUtil.STREAM_CODEC, PetBond::owner,
            PetBond::new);
}
