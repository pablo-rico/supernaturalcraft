package org.papiricoh.supernaturalcraft.magic.spell;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Renamed sigils (v0.21): old ids resolve to new ones, never to nothing, and never in a chain. */
class SigilAliasTest {

    private static final String NS = "supernaturalcraft";
    private static final Path SIGILS = Path.of("src/main/resources/data/" + NS + "/" + NS + "/sigil");

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(NS, path);
    }

    @Test
    void bindBecameSnare() {
        assertTrue(SigilAliases.isAlias(id("bind")));
        assertEquals(id("snare"), SigilAliases.resolve(id("bind")));
    }

    @Test
    void currentIdsResolveToThemselves() {
        for (String s : new String[]{"snare", "smite", "bolt", "echo"}) {
            assertFalse(SigilAliases.isAlias(id(s)), s);
            assertEquals(id(s), SigilAliases.resolve(id(s)), s);
        }
        ResourceLocation foreign = ResourceLocation.fromNamespaceAndPath("othermod", "bind");
        assertEquals(foreign, SigilAliases.resolve(foreign), "only this mod's old ids are renamed");
    }

    @Test
    void everyAliasLeadsToAShippedSigilInOneStep() {
        for (Map.Entry<ResourceLocation, ResourceLocation> e : SigilAliases.ALIASES.entrySet()) {
            ResourceLocation to = e.getValue();
            assertFalse(SigilAliases.isAlias(to), e.getKey() + " → " + to + " is itself renamed: aliases must not chain");
            assertTrue(Files.isRegularFile(SIGILS.resolve(to.getPath() + ".json")), e.getKey() + " → " + to + ": no such sigil file");
            assertFalse(Files.exists(SIGILS.resolve(e.getKey().getPath() + ".json")),
                    "the old sigil " + e.getKey() + " still ships its own file: the alias would hide it");
        }
    }
}
