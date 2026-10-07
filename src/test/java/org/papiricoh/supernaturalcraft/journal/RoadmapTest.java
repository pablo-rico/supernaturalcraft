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

/** The generated roadmap ({@code datagen/journal/RoadmapContent}), read as the client reads it. */
class RoadmapTest {

    private static final Path GENERATED = Path.of("src/generated/resources");
    private static final Path ROADMAP = GENERATED.resolve("assets/supernaturalcraft/journal/roadmap.json");
    private static final Path ADVANCEMENTS = GENERATED.resolve("data/supernaturalcraft/advancement");
    private static final Path LANG = GENERATED.resolve("assets/supernaturalcraft/lang/en_us.json");

    private static List<RoadmapNode> nodes() throws IOException {
        return RoadmapNode.LIST_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(Files.readString(ROADMAP))).getOrThrow();
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
    private static void collect(Unlock u, List<ResourceLocation> adv, List<ResourceLocation> items, List<ResourceLocation> entities) {
        u.advancement().ifPresent(adv::add);
        u.item().ifPresent(items::add);
        u.entity().ifPresent(entities::add);
        u.any().forEach(o -> collect(o, adv, items, entities));
    }

    @Test
    void idsAreUniqueAndParentsExist() throws IOException {
        List<RoadmapNode> nodes = nodes();
        assertTrue(nodes.size() >= 20, "the road has its steps: " + nodes.size());
        Map<String, RoadmapNode> byId = byId(nodes);
        assertEquals(nodes.size(), byId.size(), "duplicate node ids");
        for (RoadmapNode n : nodes) {
            for (String p : n.parents()) assertTrue(byId.containsKey(p), n.id() + " follows a missing node " + p);
            assertFalse(n.parents().contains(n.id()), n.id() + " follows itself");
        }
    }

    @Test
    void noCycles() throws IOException {
        Map<String, RoadmapNode> byId = byId(nodes());
        Set<String> clear = new HashSet<>();
        for (String id : byId.keySet()) visit(id, byId, new HashSet<>(), clear);
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
        List<RoadmapNode> nodes = nodes();
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
            List<ResourceLocation> adv = new ArrayList<>(), items = new ArrayList<>(), entities = new ArrayList<>();
            collect(n.done(), adv, items, entities);
            assertFalse(adv.isEmpty() && items.isEmpty() && entities.isEmpty(), n.id() + " can never be done");
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
        List<RoadmapNode> nodes = nodes();
        for (BossProgression.Boss b : BossProgression.Boss.values()) {
            ResourceLocation adv = ResourceLocation.fromNamespaceAndPath("supernaturalcraft", b.advancement);
            assertTrue(nodes.stream().anyMatch(n -> n.boss() && n.done().advancement().filter(adv::equals).isPresent()),
                    "no boss node is done by " + adv + " (" + b + ")");
        }
    }

    @Test
    void namesAndHintsAreTranslated() throws IOException {
        JsonObject lang = lang();
        for (RoadmapNode n : nodes()) {
            assertTrue(lang.has(n.nameKey()), "missing " + n.nameKey());
            assertTrue(lang.has(n.hintKey()), "missing " + n.hintKey());
        }
    }

    @Test
    void theMainRoadLeadsToTheCage() throws IOException {
        List<RoadmapNode> nodes = nodes();
        assertTrue(nodes.getFirst().main() && nodes.getFirst().parents().isEmpty(), "the road starts at its root");
        RoadmapNode last = nodes.stream().filter(RoadmapNode::main).reduce((a, b) -> b).orElseThrow();
        assertTrue(last.boss(), "the main road ends at a boss: " + last.id());
    }
}
