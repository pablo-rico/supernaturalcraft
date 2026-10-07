package org.papiricoh.supernaturalcraft.journal;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The generated roads of the roadmap ({@code datagen/journal/RoadmapContent}), read as the client reads them. */
class RoadmapTest {

    private static final Path GENERATED = Path.of("src/generated/resources");
    private static final Path ROADS = GENERATED.resolve("assets/supernaturalcraft/journal/roadmaps");
    private static final Path ADVANCEMENTS = GENERATED.resolve("data/supernaturalcraft/advancement");
    private static final Path LANG = GENERATED.resolve("assets/supernaturalcraft/lang/en_us.json");

    /** Every road, by file name. */
    private static Map<String, Roadmap> roads() throws IOException {
        Map<String, Roadmap> out = new HashMap<>();
        try (var files = Files.list(ROADS)) {
            for (Path f : files.toList()) {
                String name = f.getFileName().toString().replace(".json", "");
                out.put(name, Roadmap.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(Files.readString(f))).getOrThrow());
            }
        }
        return out;
    }

    /** Every node of every road. */
    private static List<RoadmapNode> nodes() throws IOException {
        List<RoadmapNode> all = new ArrayList<>();
        roads().values().forEach(r -> all.addAll(r.nodes()));
        return all;
    }

    private static List<RoadmapNode> cage() throws IOException {
        return roads().get(Roadmap.CAGE.getPath()).nodes();
    }

    private static JsonObject lang() throws IOException {
        return JsonParser.parseString(Files.readString(LANG)).getAsJsonObject();
    }

    private static Map<String, RoadmapNode> byId(List<RoadmapNode> nodes) {
        Map<String, RoadmapNode> out = new HashMap<>();
        nodes.forEach(n -> out.put(n.id(), n));
        return out;
    }

    /** Every advancement, item and entity an unlock names, through {@code any}. */
    private static void collect(Unlock u, List<ResourceLocation> adv, List<ResourceLocation> items, List<ResourceLocation> entities,
                                List<ResourceLocation> rites) {
        u.advancement().ifPresent(adv::add);
        u.item().ifPresent(items::add);
        u.entity().ifPresent(entities::add);
        u.rite().ifPresent(rites::add);
        u.any().forEach(o -> collect(o, adv, items, entities, rites));
    }

    @Test
    void theRoadsAreThere() throws IOException {
        Map<String, Roadmap> roads = roads();
        for (String id : List.of("road_to_the_cage", "the_spell_bowl", "the_crossroads")) assertTrue(roads.containsKey(id), "no road " + id);
        assertTrue(cage().size() >= 20, "the road to the Cage has its steps: " + cage().size());
        Set<Integer> orders = new HashSet<>();
        for (var e : roads.entrySet()) {
            assertTrue(e.getValue().nodes().size() >= 5, e.getKey() + " is too short");
            assertTrue(e.getValue().nodes().getFirst().parents().isEmpty(), e.getKey() + " starts at its root");
            assertTrue(orders.add(e.getValue().order()), e.getKey() + " shares its place in the menu");
        }
        assertEquals(0, roads.get(Roadmap.CAGE.getPath()).order(), "the road to the Cage comes first");
    }

    @Test
    void idsAreUniqueAndParentsExist() throws IOException {
        List<RoadmapNode> all = nodes();
        assertEquals(all.size(), byId(all).size(), "node ids must be unique across every road");
        for (Roadmap road : roads().values()) {
            Map<String, RoadmapNode> byId = byId(road.nodes());
            for (RoadmapNode n : road.nodes()) {
                for (String p : n.parents()) assertTrue(byId.containsKey(p), n.id() + " follows a node not on its road: " + p);
                assertFalse(n.parents().contains(n.id()), n.id() + " follows itself");
            }
        }
    }

    @Test
    void noCycles() throws IOException {
        for (Roadmap road : roads().values()) {
            Map<String, RoadmapNode> byId = byId(road.nodes());
            Set<String> clear = new HashSet<>();
            for (String id : byId.keySet()) visit(id, byId, new HashSet<>(), clear);
        }
    }

    private static void visit(String id, Map<String, RoadmapNode> byId, Set<String> path, Set<String> clear) {
        if (clear.contains(id)) return;
        assertTrue(path.add(id), "cycle through " + path);
        for (String p : byId.get(id).parents()) visit(p, byId, path, clear);
        path.remove(id);
        clear.add(id);
    }

    @Test
    void gridPositionsAreUniqueAndEdgesRunRightwards() throws IOException {
        for (Roadmap road : roads().values()) gridOf(road.nodes());
    }

    private static void gridOf(List<RoadmapNode> nodes) {
        Map<String, RoadmapNode> byId = byId(nodes);
        Set<Long> cells = new HashSet<>();
        for (RoadmapNode n : nodes) {
            assertTrue(n.col() >= 0 && n.row() >= 0, n.id() + " is off the grid");
            assertTrue(cells.add(((long) n.col() << 32) | n.row()), n.id() + " shares its cell with another node");
            for (String p : n.parents()) {
                assertTrue(byId.get(p).col() < n.col(), n.id() + " must sit to the right of its parent " + p);
            }
        }
    }

    @Test
    void doneConditionsExist() throws IOException {
        JsonObject lang = lang();
        for (RoadmapNode n : nodes()) {
            List<ResourceLocation> adv = new ArrayList<>(), items = new ArrayList<>(), entities = new ArrayList<>(), rites = new ArrayList<>();
            collect(n.done(), adv, items, entities, rites);
            assertFalse(adv.isEmpty() && items.isEmpty() && entities.isEmpty() && rites.isEmpty(), n.id() + " can never be done");
            for (ResourceLocation r : rites) {
                assertTrue(lang.has("bowl_spell." + r.getNamespace() + "." + r.getPath()), n.id() + ": no bowl spell " + r);
            }
            for (ResourceLocation a : adv) {
                assertEquals("supernaturalcraft", a.getNamespace());
                assertTrue(Files.isRegularFile(ADVANCEMENTS.resolve(a.getPath() + ".json")), n.id() + ": no advancement " + a);
            }
            // Items and entities of the mod are known by their generated names.
            for (ResourceLocation i : items) {
                String key = i.getNamespace() + "." + i.getPath();
                assertTrue(lang.has("item." + key) || lang.has("block." + key), n.id() + ": no item " + i);
            }
            for (ResourceLocation e : entities) {
                assertTrue(lang.has("entity." + e.getNamespace() + "." + e.getPath()), n.id() + ": no entity " + e);
            }
        }
    }

    @Test
    void everyBossHasItsNode() throws IOException {
        List<RoadmapNode> nodes = cage();
        for (BossProgression.Boss b : BossProgression.Boss.values()) {
            ResourceLocation adv = ResourceLocation.fromNamespaceAndPath("supernaturalcraft", b.advancement);
            assertTrue(nodes.stream().anyMatch(n -> n.boss() && n.done().advancement().filter(adv::equals).isPresent()),
                    "no boss node is done by " + adv + " (" + b + ")");
        }
    }

    @Test
    void namesAndHintsAreTranslated() throws IOException {
        JsonObject lang = lang();
        for (String road : roads().keySet()) {
            String key = Roadmap.titleKey(ResourceLocation.fromNamespaceAndPath("supernaturalcraft", road));
            assertTrue(lang.has(key), "missing " + key);
        }
        for (RoadmapNode n : nodes()) {
            assertTrue(lang.has(n.nameKey()), "missing " + n.nameKey());
            assertTrue(lang.has(n.hintKey()), "missing " + n.hintKey());
        }
    }

    @Test
    void theMainRoadLeadsToTheCage() throws IOException {
        List<RoadmapNode> nodes = cage();
        assertTrue(nodes.getFirst().main() && nodes.getFirst().parents().isEmpty(), "the road starts at its root");
        RoadmapNode last = nodes.stream().filter(RoadmapNode::main).reduce((a, b) -> b).orElseThrow();
        assertTrue(last.boss(), "the main road ends at a boss: " + last.id());
    }
}
