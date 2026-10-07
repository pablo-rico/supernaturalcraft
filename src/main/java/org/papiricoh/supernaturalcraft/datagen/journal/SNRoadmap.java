package org.papiricoh.supernaturalcraft.datagen.journal;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.journal.RoadmapNode;
import org.papiricoh.supernaturalcraft.journal.Unlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * The road to the Cage: every step of the boss roadmap with its English name and hint. Writes
 * {@code assets/supernaturalcraft/journal/roadmap.json} and feeds the lang file ({@link #addLang}).
 * <b>New progression gets its node here</b> (see CLAUDE.md).
 */
public final class SNRoadmap implements DataProvider {

    private final PackOutput output;

    public SNRoadmap(PackOutput output) {
        this.output = output;
    }

    /** Every node, in the order the dashboard looks for the next objective. */
    public static List<Node> nodes() {
        List<Node> all = new ArrayList<>();
        RoadmapContent.addAll(all);
        return all;
    }

    public static Node node(String id, int col, int row) {
        return new Node(id, col, row);
    }

    public static final class Node {
        final String id;
        final int col, row;
        ResourceLocation icon = ResourceLocation.withDefaultNamespace("book");
        final List<String> parents = new ArrayList<>();
        boolean boss, main;
        Unlock done = Unlock.ALWAYS;
        Optional<ResourceLocation> entry = Optional.empty();
        String name, hint;

        private Node(String id, int col, int row) {
            this.id = id;
            this.col = col;
            this.row = row;
        }

        public Node icon(ItemLike item) {
            this.icon = BuiltInRegistries.ITEM.getKey(item.asItem());
            return this;
        }

        public Node icon(String itemId) {
            this.icon = ResourceLocation.parse(itemId);
            return this;
        }

        public Node after(String... parentIds) {
            parents.addAll(List.of(parentIds));
            return this;
        }

        public Node boss() {
            this.boss = true;
            return this;
        }

        public Node main() {
            this.main = true;
            return this;
        }

        /** Done with an advancement of the mod, e.g. {@code "main/yellow_eyed"}. */
        public Node advancement(String path) {
            this.done = Unlock.advancement(SupernaturalCraft.asResource(path));
            return this;
        }

        public Node done(Unlock done) {
            this.done = done;
            return this;
        }

        /** The journal entry the node opens (an id in the mod's namespace). */
        public Node entry(String entryId) {
            this.entry = Optional.of(SupernaturalCraft.asResource(entryId));
            return this;
        }

        public Node name(String english) {
            this.name = english;
            return this;
        }

        public Node hint(String english) {
            this.hint = english;
            return this;
        }

        RoadmapNode build() {
            if (name == null || hint == null) throw new IllegalStateException("Roadmap node " + id + " needs a name and a hint");
            return new RoadmapNode(id, col, row, icon, parents, boss, main, done, entry);
        }
    }

    public static void addLang(BiConsumer<String, String> add) {
        for (Node n : nodes()) {
            RoadmapNode built = n.build();
            add.accept(built.nameKey(), n.name);
            add.accept(built.hintKey(), n.hint);
        }
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<RoadmapNode> built = nodes().stream().map(Node::build).toList();
        JsonElement json = RoadmapNode.LIST_CODEC.encodeStart(JsonOps.INSTANCE, built).getOrThrow();
        return DataProvider.saveStable(cache, json, output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                .resolve(SupernaturalCraft.MODID).resolve("journal/roadmap.json"));
    }

    @Override
    public String getName() {
        return "SupernaturalCraft roadmap";
    }
}
