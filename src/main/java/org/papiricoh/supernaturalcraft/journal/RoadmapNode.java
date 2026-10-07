package org.papiricoh.supernaturalcraft.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * A step on one of the roadmap's roads ({@link Roadmap}, written by {@code datagen/journal/SNRoadmap}). It sits at {@code [col, row]} on the roadmap's grid, follows
 * its {@code parents}, and is done when {@code done} holds. {@code main} marks the main road: the
 * dashboard's next objective is the first available step on it. Name and hint are
 * {@code roadmap.<ns>.<id>.name} / {@code .hint}.
 */
public record RoadmapNode(String id, int col, int row, ResourceLocation icon, List<String> parents, boolean boss,
                          boolean main, Unlock done, Optional<ResourceLocation> entry) {

    public static final Codec<RoadmapNode> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(RoadmapNode::id),
            Codec.INT.fieldOf("col").forGetter(RoadmapNode::col),
            Codec.INT.fieldOf("row").forGetter(RoadmapNode::row),
            ResourceLocation.CODEC.fieldOf("icon").forGetter(RoadmapNode::icon),
            Codec.STRING.listOf().optionalFieldOf("parents", List.of()).forGetter(RoadmapNode::parents),
            Codec.BOOL.optionalFieldOf("boss", false).forGetter(RoadmapNode::boss),
            Codec.BOOL.optionalFieldOf("main", false).forGetter(RoadmapNode::main),
            Unlock.CODEC.fieldOf("done").forGetter(RoadmapNode::done),
            ResourceLocation.CODEC.optionalFieldOf("entry").forGetter(RoadmapNode::entry)
    ).apply(i, RoadmapNode::new));

    public RoadmapNode {
        parents = List.copyOf(parents);
    }

    public String nameKey() {
        return "roadmap.supernaturalcraft." + id + ".name";
    }

    public String hintKey() {
        return "roadmap.supernaturalcraft." + id + ".hint";
    }
}
