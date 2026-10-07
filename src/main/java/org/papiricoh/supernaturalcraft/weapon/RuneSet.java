package org.papiricoh.supernaturalcraft.weapon;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/** The runes graved into one weapon. Immutable, as a data component must be. */
public record RuneSet(List<Rune> runes) {

    public static final RuneSet EMPTY = new RuneSet(List.of());
    public static final Codec<RuneSet> CODEC = Rune.CODEC.listOf(0, 4).xmap(RuneSet::new, RuneSet::runes);
    public static final StreamCodec<ByteBuf, RuneSet> STREAM_CODEC = ByteBufCodecs.idMapper(i -> Rune.values()[i], Rune::ordinal)
            .apply(ByteBufCodecs.list(4)).map(RuneSet::new, RuneSet::runes);

    public RuneSet {
        runes = List.copyOf(runes);
    }

    public int count(Rune rune) {
        int n = 0;
        for (Rune r : runes) if (r == rune) n++;
        return n;
    }

    public boolean has(Rune rune) {
        return runes.contains(rune);
    }

    /** Why {@code rune} can't be added to a weapon with this profile, or null if it can. */
    public String rejection(Rune rune, WeaponProfile profile) {
        if (runes.size() >= profile.runeSlots()) return "full";
        if (!rune.fits(profile.kind())) return "wrong_kind";
        if (count(rune) >= Rune.MAX_SAME) return "too_many";
        if (rune == Rune.VOID && profile.tier() < 4) return "tier";
        return null;
    }

    public RuneSet with(Rune rune) {
        List<Rune> copy = new ArrayList<>(runes);
        copy.add(rune);
        return new RuneSet(copy);
    }

    /** Purging returns every rune but one, chosen at random, which is lost. */
    public List<Rune> survivorsOfPurge(RandomSource random) {
        if (runes.isEmpty()) return List.of();
        List<Rune> copy = new ArrayList<>(runes);
        copy.remove(random.nextInt(copy.size()));
        return copy;
    }
}
