package org.papiricoh.supernaturalcraft.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * One road of the roadmap ({@code assets/<ns>/journal/roadmaps/<id>.json}, written by
 * {@code datagen/journal/SNRoadmap}): its nodes, its place in the roadmap's menu ({@code order})
 * and its icon. Its title is {@code roadmap.<ns>.<id>.title}. Node ids are unique across every road. A road may be cut into
 * {@code chapters} (v0.38: bands of columns, left to right; the road to the Cage has them).
 */
public record Roadmap(int order, ResourceLocation icon, List<RoadmapNode> nodes, List<RoadmapChapter> chapters) {

    /** The road to the Cage: the main road, the one the dashboard follows. */
    public static final ResourceLocation CAGE = ResourceLocation.fromNamespaceAndPath("supernaturalcraft", "road_to_the_cage");

    public static final Codec<Roadmap> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("order", 0).forGetter(Roadmap::order),
            ResourceLocation.CODEC.fieldOf("icon").forGetter(Roadmap::icon),
            RoadmapNode.CODEC.listOf().fieldOf("nodes").forGetter(Roadmap::nodes),
            RoadmapChapter.CODEC.listOf().optionalFieldOf("chapters", List.of()).forGetter(Roadmap::chapters)
    ).apply(i, Roadmap::new));

    public Roadmap {
        nodes = List.copyOf(nodes);
        chapters = List.copyOf(chapters);
    }

    public Roadmap(int order, ResourceLocation icon, List<RoadmapNode> nodes) {
        this(order, icon, nodes, List.of());
    }

    /** The chapter that holds column {@code col}, or null. */
    public @org.jetbrains.annotations.Nullable RoadmapChapter chapterAt(int col) {
        for (RoadmapChapter c : chapters) if (c.holds(col)) return c;
        return null;
    }

    public static String titleKey(ResourceLocation id) {
        return "roadmap." + id.getNamespace() + "." + id.getPath() + ".title";
    }
}
