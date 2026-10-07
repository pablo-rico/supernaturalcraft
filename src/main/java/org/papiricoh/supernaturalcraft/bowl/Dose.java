package org.papiricoh.supernaturalcraft.bowl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

/**
 * One bottle's worth of liquid in a bowl. {@code owner}/{@code ownerName} are set only for
 * {@link BowlLiquid#BLOOD}: whose blood it is (what a locating spell follows).
 */
public record Dose(BowlLiquid kind, int color, Optional<UUID> owner, Optional<String> ownerName) {

    public static final Codec<BowlLiquid> LIQUID_CODEC = Codec.STRING.comapFlatMap(s -> {
        BowlLiquid l = BowlLiquid.fromId(s);
        return l == null ? com.mojang.serialization.DataResult.error(() -> "Unknown bowl liquid " + s)
                : com.mojang.serialization.DataResult.success(l);
    }, BowlLiquid::id);

    public static final StreamCodec<ByteBuf, BowlLiquid> LIQUID_STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(i -> BowlLiquid.values()[Math.floorMod(i, BowlLiquid.values().length)], BowlLiquid::ordinal);

    public static final Codec<Dose> CODEC = RecordCodecBuilder.create(i -> i.group(
            LIQUID_CODEC.fieldOf("kind").forGetter(Dose::kind),
            Codec.INT.optionalFieldOf("color", -1).forGetter(Dose::color),
            UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(Dose::owner),
            Codec.STRING.optionalFieldOf("owner_name").forGetter(Dose::ownerName)
    ).apply(i, (k, c, o, n) -> new Dose(k, c == -1 ? k.color : c, o, n)));

    public static final StreamCodec<ByteBuf, Dose> STREAM_CODEC = StreamCodec.composite(
            LIQUID_STREAM_CODEC, Dose::kind,
            ByteBufCodecs.INT, Dose::color,
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs::optional), Dose::owner,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs::optional), Dose::ownerName,
            Dose::new);

    public static Dose of(BowlLiquid kind) {
        return new Dose(kind, kind.color, Optional.empty(), Optional.empty());
    }

    public static Dose tinted(BowlLiquid kind, int color) {
        return new Dose(kind, color, Optional.empty(), Optional.empty());
    }

    public static Dose blood(UUID owner, String name) {
        return new Dose(BowlLiquid.BLOOD, BowlLiquid.BLOOD.color, Optional.of(owner), Optional.of(name));
    }
}
