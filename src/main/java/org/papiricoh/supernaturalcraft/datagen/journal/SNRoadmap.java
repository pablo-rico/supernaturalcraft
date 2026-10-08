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
import org.papiricoh.supernaturalcraft.journal.Roadmap;
import org.papiricoh.supernaturalcraft.journal.RoadmapNode;
import org.papiricoh.supernaturalcraft.journal.Unlock;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * The roadmap's roads (the road to the Cage, the spell bowl, the crossroads...): every step with its
 * English name and hint. Writes {@code assets/supernaturalcraft/journal/roadmaps/<road>.json} and
 * feeds the lang file ({@link #addLang}). <b>New progression gets its node here</b> (see CLAUDE.md).
 * Node ids are unique across all roads (their lang keys are {@code roadmap.supernaturalcraft.<node>.*}).
 */
public final class SNRoadmap implements DataProvider {

    private final PackOutput output;

    public SNRoadmap(PackOutput output) {
        this.output = output;
    }

    /** Every road, in the roadmap menu's order. */
    public static List<Road> roads() {
        List<Road> all = new ArrayList<>();
        RoadmapContent.addAll(all);
        return all;
    }

    /** A road: {@code id} is its file name, {@code title} its English name in the menu. */
    public static Road road(String id, ItemLike icon, String title) {
        return new Road(id, BuiltInRegistries.ITEM.getKey(icon.asItem()), title);
    }

    public static final class Road {
        final String id;
        final ResourceLocation icon;
        final String title;
        /** Its nodes, in the order "Next" looks for the next objective. */
        public final List<Node> nodes = new ArrayList<>();

        private Road(String id, ResourceLocation icon, String title) {
            this.id = id;
            this.icon = icon;
            this.title = title;
        }

        public Road add(Node node) {
            nodes.add(node);
            return this;
        }
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
        Optional<String> branch = Optional.empty();
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

        /** Done once the bowl spell with this id (in the mod's namespace) is learned. */
        public Node rite(String spell) {
            this.done = Unlock.rite(SupernaturalCraft.asResource(spell));
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

        /** On one side's branch ({@code "angel"}, {@code "demon"}, {@code "hunter"}): forsaken once sworn to another. */
        public Node branch(String side) {
            this.branch = Optional.of(side);
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
            return new RoadmapNode(id, col, row, icon, parents, boss, main, done, entry, branch);
        }
    }

    public static void addLang(BiConsumer<String, String> add) {
        for (Road r : roads()) {
            add.accept(Roadmap.titleKey(SupernaturalCraft.asResource(r.id)), r.title);
            for (Node n : r.nodes) {
                RoadmapNode built = n.build();
                add.accept(built.nameKey(), n.name);
                add.accept(built.hintKey(), n.hint);
            }
        }
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path root = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(SupernaturalCraft.MODID).resolve("journal/roadmaps");
        List<CompletableFuture<?>> writes = new ArrayList<>();
        List<Road> roads = roads();
        for (int i = 0; i < roads.size(); i++) {
            Road r = roads.get(i);
            Roadmap built = new Roadmap(i, r.icon, r.nodes.stream().map(Node::build).toList());
            JsonElement json = Roadmap.CODEC.encodeStart(JsonOps.INSTANCE, built).getOrThrow();
            writes.add(DataProvider.saveStable(cache, json, root.resolve(r.id + ".json")));
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "SupernaturalCraft roadmap";
    }
}
