package org.papiricoh.supernaturalcraft.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/**
 * A chapter of a road (v0.38): the band of the roadmap's grid from column {@code from} to {@code to}, both included. The
 * road to the Cage reads as the series does, chapter by chapter. Title and seasons:
 * {@code roadmap.<ns>.<road>.chapter.<id>.title} / {@code .seasons}.
 */
public record RoadmapChapter(String id, int from, int to) {

    public static final Codec<RoadmapChapter> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(RoadmapChapter::id),
            Codec.INT.fieldOf("from").forGetter(RoadmapChapter::from),
            Codec.INT.fieldOf("to").forGetter(RoadmapChapter::to)
    ).apply(i, RoadmapChapter::new));

    public boolean holds(int col) {
        return col >= from && col <= to;
    }

    /** I, II, III... for the {@code n}th chapter (1-based). */
    public static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"};
        return n >= 0 && n < r.length ? r[n] : Integer.toString(n);
    }

    public static String titleKey(ResourceLocation road, String id) {
        return "roadmap." + road.getNamespace() + "." + road.getPath() + ".chapter." + id + ".title";
    }

    public static String seasonsKey(ResourceLocation road, String id) {
        return "roadmap." + road.getNamespace() + "." + road.getPath() + ".chapter." + id + ".seasons";
    }
}
