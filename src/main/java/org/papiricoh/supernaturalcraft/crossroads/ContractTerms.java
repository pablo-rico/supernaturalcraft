package org.papiricoh.supernaturalcraft.crossroads;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/**
 * What is written on a crossroads contract: whose soul, for what wish ({@code "upgrade.0"}), due
 * when (game time), and how it ended ({@link #OPEN}, {@link #PAID}, {@link #VOID} or
 * {@link #COLLECTED}).
 */
public record ContractTerms(UUID owner, String ownerName, String wish, long dueAt, String status) {

    public static final String OPEN = "open", PAID = "paid", VOID = "void", COLLECTED = "collected";

    public static final Codec<ContractTerms> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(ContractTerms::owner),
            Codec.STRING.fieldOf("owner_name").forGetter(ContractTerms::ownerName),
            Codec.STRING.fieldOf("wish").forGetter(ContractTerms::wish),
            Codec.LONG.fieldOf("due_at").forGetter(ContractTerms::dueAt),
            Codec.STRING.optionalFieldOf("status", OPEN).forGetter(ContractTerms::status)
    ).apply(i, ContractTerms::new));

    public static final StreamCodec<ByteBuf, ContractTerms> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ContractTerms::owner,
            ByteBufCodecs.STRING_UTF8, ContractTerms::ownerName,
            ByteBufCodecs.STRING_UTF8, ContractTerms::wish,
            ByteBufCodecs.VAR_LONG, ContractTerms::dueAt,
            ByteBufCodecs.STRING_UTF8, ContractTerms::status,
            ContractTerms::new);

    public ContractTerms(UUID owner, String ownerName, String wish, long dueAt) {
        this(owner, ownerName, wish, dueAt, OPEN);
    }

    public ContractTerms withStatus(String s) {
        return new ContractTerms(owner, ownerName, wish, dueAt, s);
    }

    public boolean open() {
        return OPEN.equals(status);
    }
}
