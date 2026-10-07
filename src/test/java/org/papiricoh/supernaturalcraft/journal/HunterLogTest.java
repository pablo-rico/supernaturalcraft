package org.papiricoh.supernaturalcraft.journal;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HunterLogTest {

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("supernaturalcraft", path);
    }

    @Test
    void bookmarksToggleAndStopAtTwelve() {
        HunterLog log = new HunterLog();
        for (int i = 0; i < HunterLog.MAX_BOOKMARKS; i++) assertTrue(log.toggleBookmark(id("e" + i)));
        assertFalse(log.toggleBookmark(id("one_too_many")), "a full set refuses another");
        assertEquals(HunterLog.MAX_BOOKMARKS, log.bookmarks().size());
        assertFalse(log.toggleBookmark(id("e0")), "toggling a marked entry unmarks it");
        assertFalse(log.bookmarks().contains(id("e0")));
    }

    @Test
    void killsCountAndSeeingIsOnce() {
        HunterLog log = new HunterLog();
        assertTrue(log.see(id("ghost")));
        assertFalse(log.see(id("ghost")), "seen twice is still one sighting");
        log.kill(id("ghost"));
        log.kill(id("ghost"));
        assertEquals(2, log.kills(id("ghost")));
        assertTrue(log.obtain(id("salt")));
        assertFalse(log.obtain(id("salt")));
    }

    @Test
    void libraryHasFixedSlots() {
        HunterLog log = new HunterLog();
        assertEquals(HunterLog.LIBRARY_SLOTS, log.designs().size());
        Spell s = new Spell(Optional.of(id("bolt")), List.of(id("smite")), List.of(), "Holy Bolt");
        assertTrue(log.setDesign(5, s));
        assertFalse(log.setDesign(-1, s));
        assertFalse(log.setDesign(HunterLog.LIBRARY_SLOTS, s));
        assertEquals(s, log.design(5));
        assertTrue(log.design(6).isEmpty());
    }

    @Test
    void survivesTheCodec() {
        HunterLog log = new HunterLog();
        log.kill(id("azazel"));
        log.obtain(id("salt"));
        log.markRead(id("salt"));
        log.toggleBookmark(id("azazel"));
        log.setDesign(2, new Spell(Optional.of(id("touch")), List.of(id("mend")), List.of(), "Mend"));
        var json = HunterLog.CODEC.encodeStart(JsonOps.INSTANCE, log).getOrThrow();
        HunterLog back = HunterLog.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json.toString())).getOrThrow();
        assertEquals(1, back.kills(id("azazel")));
        assertTrue(back.hasSeen(id("azazel")));
        assertTrue(back.has(id("salt")));
        assertTrue(back.read().contains(id("salt")));
        assertEquals(List.of(id("azazel")), back.bookmarks());
        assertEquals("Mend", back.design(2).name());
        assertEquals(HunterLog.LIBRARY_SLOTS, back.designs().size());
    }

    @Test
    void unlocksTestProgressAndRefuseTwoConditions() {
        Progress p = new Progress() {
            public boolean done(ResourceLocation a) {
                return a.getPath().equals("main/root");
            }

            public boolean seen(ResourceLocation e) {
                return false;
            }

            public boolean has(ResourceLocation i) {
                return i.getPath().equals("salt");
            }
        };
        assertTrue(Unlock.ALWAYS.test(p));
        assertTrue(Unlock.advancement(id("main/root")).test(p));
        assertFalse(Unlock.entity(id("ghost")).test(p));
        assertTrue(Unlock.any(Unlock.entity(id("ghost")), Unlock.item(id("salt"))).test(p));
        var bad = JsonParser.parseString("{\"entity\":\"supernaturalcraft:ghost\",\"item\":\"supernaturalcraft:salt\"}");
        assertTrue(Unlock.CODEC.parse(JsonOps.INSTANCE, bad).isError(), "one condition per unlock");
        var nested = JsonParser.parseString("{\"any\":[{\"entity\":\"supernaturalcraft:ghost\"},{\"advancement\":\"supernaturalcraft:main/root\"}]}");
        assertTrue(Unlock.CODEC.parse(JsonOps.INSTANCE, nested).getOrThrow().test(p));
    }
}
