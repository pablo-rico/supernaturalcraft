package org.papiricoh.supernaturalcraft.allegiance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * A ritual's {@code "allegiance"} condition: the ritualist's side and current rank. {@code {"faction": "angel", "rank": 1}}
 * (a Lesser Angel, about to rise); {@code {"faction": "human", "chosen": false}} (a human free to choose a side: not on a
 * cure's cooldown). Rank may be omitted (any); {@code "min_rank"} accepts that rank or higher.
 */
public record AllegianceRequirement(Faction faction, Optional<Integer> rank, Optional<Integer> minRank, Optional<Boolean> chosen) {

    public static final Codec<AllegianceRequirement> CODEC = RecordCodecBuilder.create(i -> i.group(
            Faction.CODEC.fieldOf("faction").forGetter(AllegianceRequirement::faction),
            Codec.INT.optionalFieldOf("rank").forGetter(AllegianceRequirement::rank),
            Codec.INT.optionalFieldOf("min_rank").forGetter(AllegianceRequirement::minRank),
            Codec.BOOL.optionalFieldOf("chosen").forGetter(AllegianceRequirement::chosen)
    ).apply(i, AllegianceRequirement::new));

    public static AllegianceRequirement of(Faction faction, int rank) {
        return new AllegianceRequirement(faction, Optional.of(rank), Optional.empty(), Optional.empty());
    }

    /** @return null if met, else the translation key saying why not */
    public String check(Allegiance a, long gameTime) {
        if (a.faction() != faction) return "message.supernaturalcraft.allegiance.wrong_side." + faction.getSerializedName();
        if (rank.isPresent() && a.rank() != rank.get()) return "message.supernaturalcraft.allegiance.wrong_rank";
        if (minRank.isPresent() && a.rank() < minRank.get()) return "message.supernaturalcraft.allegiance.wrong_rank";
        if (chosen.isPresent() && !chosen.get() && !a.mayChoose(gameTime)) return "message.supernaturalcraft.allegiance.cooldown";
        return null;
    }
}
