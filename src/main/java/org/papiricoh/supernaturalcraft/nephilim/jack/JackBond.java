package org.papiricoh.supernaturalcraft.nephilim.jack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;

import java.util.Optional;

/**
 * v0.41: a hunter's bond with Jack, an attachment kept through death: where he was told to stay (empty: he follows), until when
 * he is away healing after falling ({@code retreatUntil}, game time), and the last day he gave his blood.
 */
public record JackBond(Optional<GlobalPos> seat, long retreatUntil, long lastBlood) {

    public static final JackBond EMPTY = new JackBond(Optional.empty(), 0L, -100L);

    public static final Codec<JackBond> CODEC = RecordCodecBuilder.create(i -> i.group(
            GlobalPos.CODEC.optionalFieldOf("seat").forGetter(JackBond::seat),
            Codec.LONG.optionalFieldOf("retreat_until", 0L).forGetter(JackBond::retreatUntil),
            Codec.LONG.optionalFieldOf("last_blood", -100L).forGetter(JackBond::lastBlood)
    ).apply(i, JackBond::new));

    public JackBond withSeat(Optional<GlobalPos> s) {
        return new JackBond(s, retreatUntil, lastBlood);
    }

    public JackBond withRetreat(long until) {
        return new JackBond(seat, until, lastBlood);
    }

    public JackBond withBlood(long day) {
        return new JackBond(seat, retreatUntil, day);
    }

    public boolean staying() {
        return seat.isPresent();
    }

    public static GlobalPos at(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim, BlockPos pos) {
        return GlobalPos.of(dim, pos.immutable());
    }
}
