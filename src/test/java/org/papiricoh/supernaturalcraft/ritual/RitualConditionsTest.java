package org.papiricoh.supernaturalcraft.ritual;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@code requires_advancement}: one advancement as before, or a list of them (Death's rite wants all three Horsemen). */
class RitualConditionsTest {

    private static RitualConditions parse(String json) {
        return RitualConditions.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    @Test
    void aSingleAdvancementStillReads() {
        RitualConditions c = parse("{\"time\": \"night\", \"requires_advancement\": \"supernaturalcraft:main/yellow_eyed\"}");
        assertEquals(List.of(ResourceLocation.parse("supernaturalcraft:main/yellow_eyed")), c.requiresAdvancement());
        assertEquals(RitualConditions.Time.NIGHT, c.time());
    }

    @Test
    void aListReadsInOrder() {
        RitualConditions c = parse("{\"requires_advancement\": [\"supernaturalcraft:main/war\", \"supernaturalcraft:main/famine\", "
                + "\"supernaturalcraft:main/pestilence\"]}");
        assertEquals(3, c.requiresAdvancement().size());
        assertEquals("main/famine", c.requiresAdvancement().get(1).getPath());
    }

    @Test
    void noneMeansNone() {
        assertTrue(parse("{}").requiresAdvancement().isEmpty());
    }

    @Test
    void oneIsWrittenBackAsAPlainStringAndManyAsAList() {
        RitualConditions one = parse("{\"requires_advancement\": \"supernaturalcraft:main/devil_went_down\"}");
        JsonElement out = RitualConditions.CODEC.encodeStart(JsonOps.INSTANCE, one).getOrThrow();
        assertTrue(out.getAsJsonObject().get("requires_advancement").isJsonPrimitive());
        RitualConditions many = parse("{\"requires_advancement\": [\"a:b\", \"c:d\"]}");
        out = RitualConditions.CODEC.encodeStart(JsonOps.INSTANCE, many).getOrThrow();
        assertTrue(out.getAsJsonObject().get("requires_advancement").isJsonArray());
        assertEquals(many, parse(out.toString()));
    }

    @Test
    void theSkyARiteNeeds() {
        assertEquals(RitualConditions.Weather.ANY, parse("{}").weather());
        RitualConditions storm = parse("{\"weather\": \"thunder\"}");
        assertEquals(RitualConditions.Weather.THUNDER, storm.weather());
        assertTrue(!storm.weather().fits(true, false), "rain alone is not a thunderstorm");
        assertTrue(storm.weather().fits(true, true));
        assertTrue(RitualConditions.Weather.RAIN.fits(false, true), "a thunderstorm is rain too");
        assertTrue(!RitualConditions.Weather.RAIN.fits(false, false));
        JsonElement out = RitualConditions.CODEC.encodeStart(JsonOps.INSTANCE, storm).getOrThrow();
        assertEquals(storm, parse(out.toString()));
    }
}
