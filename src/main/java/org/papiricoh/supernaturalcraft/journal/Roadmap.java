package org.papiricoh.supernaturalcraft.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * One road of the roadmap ({@code assets/<ns>/journal/roadmaps/<id>.json}, written by
 * {@code datagen/journal/SNRoadmap}): its nodes, its place in the roadmap's menu ({@code order})
 * and its icon. Its title is {@code roadmap.<ns>.<id>.title}. Node ids are unique across every road.
 */
public record Roadmap(int order, ResourceLocation icon, List<RoadmapNode> nodes) {

    /** The road to the Cage: the main road, the one the dashboard follows. */
    public static final ResourceLocation CAGE = ResourceLocation.fromNamespaceAndPath("supernaturalcraft", "road_to_the_cage");

    public static final Codec<Roadmap> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("order", 0).forGetter(Roadmap::order),
            ResourceLocation.CODEC.fieldOf("icon").forGetter(Roadmap::icon),
            RoadmapNode.CODEC.listOf().fieldOf("nodes").forGetter(Roadmap::nodes)
    ).apply(i, Roadmap::new));

    public Roadmap {
        nodes = List.copyOf(nodes);
    }

    public static String titleKey(ResourceLocation id) {
        return "roadmap." + id.getNamespace() + "." + id.getPath() + ".title";
    }
}
