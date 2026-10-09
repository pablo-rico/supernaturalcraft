package org.papiricoh.supernaturalcraft.legacy.gen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormulaGeneratorTest {

    private static final Path SIGILS = Path.of("src/main/resources/data/supernaturalcraft/supernaturalcraft/sigil");

    /** The real sigils, read from their JSON. */
    static List<FormulaGenerator.Base> bases() throws IOException {
        List<FormulaGenerator.Base> out = new ArrayList<>();
        try (Stream<Path> files = Files.list(SIGILS)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                JsonObject o = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
                Map<String, Float> params = new LinkedHashMap<>();
                if (o.has("params")) for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("params").entrySet()) {
                    params.put(e.getKey(), e.getValue().getAsFloat());
                }
                String id = "supernaturalcraft:" + f.getFileName().toString().replace(".json", "");
                String color = o.has("color") ? o.get("color").getAsString().replace("#", "") : "F2E6B0";
                out.add(new FormulaGenerator.Base(id, o.get("kind").getAsString(), o.get("behavior").getAsString(),
                        o.has("tier") ? o.get("tier").getAsInt() : 1, o.has("mana_cost") ? o.get("mana_cost").getAsFloat() : 0,
                        o.has("cooldown") ? o.get("cooldown").getAsInt() : 0, params, Integer.parseInt(color, 16)));
            }
        }
        assertFalse(out.isEmpty());
        return out;
    }

    @Test
    void deterministicSafeAndUniquelyNamed() throws IOException {
        List<FormulaGenerator.Base> bases = bases();
        Map<String, FormulaGenerator.Base> byId = new LinkedHashMap<>();
        bases.forEach(b -> byId.put(b.id(), b));
        for (int hunter = 0; hunter < 6; hunter++) {
            long salt = GenSeed.of(1234L + hunter, new UUID(hunter, 99), "formula", 0);
            Set<String> names = new HashSet<>();
            for (int i = 0; i < 120; i++) {
                int tier = RiteGenerator.tierOf(i);
                FormulaGenerator.Spec s = FormulaGenerator.generate(bases, salt, i, tier);
                assertNotNull(s);
                assertEquals(s, FormulaGenerator.generate(bases, salt, i, tier), "deterministic");
                assertTrue(names.add(s.name()), "unique name " + s.name());
                FormulaGenerator.Base base = byId.get(s.baseId());
                assertNotNull(base);
                assertFalse(FormulaGenerator.EXCLUDED.contains(base.id()));
                assertTrue(base.tier() <= Math.min(3, tier), "base tier within the research tier");
                assertTrue(s.sigilTier() >= base.tier() && s.sigilTier() <= Math.min(3, base.tier() + 1));
                assertEquals(base.params().keySet(), s.params().keySet());
                for (Map.Entry<String, Float> e : s.params().entrySet()) {
                    float v = base.params().get(e.getKey());
                    float out = e.getValue();
                    if (e.getKey().equals("echo")) {
                        assertEquals(v, out);
                        continue;
                    }
                    if (e.getKey().equals("mana_multiplier")) {
                        assertTrue(out >= 1, "a modifier never makes spells cheaper");
                        continue;
                    }
                    boolean mult = "modifier".equals(base.kind()) && e.getKey().endsWith("_multiplier");
                    double limit = mult ? 1 + (v - 1) * FormulaGenerator.MAX_FACTOR : v * FormulaGenerator.MAX_FACTOR;
                    assertTrue(out <= limit + 1e-3, "never stronger than one tier up: " + e + " of " + base.id());
                    Float cap = FormulaGenerator.CAPS.get(e.getKey());
                    if (cap != null) assertTrue(out <= Math.max(cap, v) + 1e-3, "absolute cap " + e);
                    double low = mult ? 1 + (v - 1) * FormulaGenerator.MIN_FACTOR : v * FormulaGenerator.MIN_FACTOR;
                    assertTrue(out >= low - 0.02, "not uselessly weak " + e);
                }
                assertTrue(s.manaCost() >= base.manaCost() * FormulaGenerator.MIN_MANA_SHARE - 0.1, "mana scales with potency");
                assertTrue(s.manaCost() <= base.manaCost() * 1.5 + 0.1);
                assertTrue(s.cooldown() >= base.cooldown() && s.cooldown() <= 1200);
                assertTrue(s.color() >= 0 && s.color() <= 0xFFFFFF);
            }
        }
    }

    @Test
    void earlyTiersOnlyUseEarlyBases() throws IOException {
        List<FormulaGenerator.Base> usable = FormulaGenerator.usable(bases(), 1);
        assertFalse(usable.isEmpty());
        usable.forEach(b -> assertEquals(1, b.tier()));
        assertTrue(FormulaGenerator.usable(bases(), 5).stream().noneMatch(b -> b.id().endsWith(":echo") || b.id().endsWith(":banishing")));
        assertTrue(FormulaGenerator.maxFactor(1) < FormulaGenerator.maxFactor(5));
        assertTrue(FormulaGenerator.maxFactor(5) <= FormulaGenerator.MAX_FACTOR);
    }
}
