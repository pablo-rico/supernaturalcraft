package org.papiricoh.supernaturalcraft.weapon.curse;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

/** A hungry weapon's appetite: souls eaten, how fed it is, and (for bound weapons) its master. */
public record CurseState(int souls, int satiation, Optional<UUID> owner, long lastMarkTick) {

    public static final CurseState FRESH = new CurseState(0, CurseLevels.MAX_SATIATION, Optional.empty(), Long.MIN_VALUE / 2);

    public static final Codec<CurseState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("souls", 0).forGetter(CurseState::souls),
            Codec.intRange(0, CurseLevels.MAX_SATIATION).optionalFieldOf("satiation", CurseLevels.MAX_SATIATION).forGetter(CurseState::satiation),
            UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(CurseState::owner),
            Codec.LONG.optionalFieldOf("last_mark", 0L).forGetter(CurseState::lastMarkTick)
    ).apply(i, CurseState::new));

    public static final StreamCodec<ByteBuf, CurseState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CurseState::souls,
            ByteBufCodecs.VAR_INT, CurseState::satiation,
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs::optional), CurseState::owner,
            ByteBufCodecs.VAR_LONG, CurseState::lastMarkTick,
            CurseState::new);

    public int level() {
        return CurseLevels.levelFor(souls);
    }

    public CurseState fed(int soulsGained) {
        return new CurseState(souls + soulsGained, Math.min(CurseLevels.MAX_SATIATION, satiation + CurseLevels.FEED_PER_KILL), owner, lastMarkTick);
    }

    public CurseState hungrier() {
        return new CurseState(souls, Math.max(0, satiation - 1), owner, lastMarkTick);
    }

    public CurseState boundTo(UUID player) {
        return new CurseState(souls, satiation, Optional.of(player), lastMarkTick);
    }

    public CurseState markUsed(long tick) {
        return new CurseState(souls, satiation, owner, tick);
    }

    public boolean ownedBy(UUID player) {
        return owner.isEmpty() || owner.get().equals(player);
    }
}
