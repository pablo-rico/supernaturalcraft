package org.papiricoh.supernaturalcraft.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.metatron.MetatronAnimations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Metatron's Java-side names against his generated GeckoLib files, and the art his blocks and items need. */
class MetatronAssetsTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/supernaturalcraft");

    private static JsonObject read(String path) throws IOException {
        return JsonParser.parseString(Files.readString(ASSETS.resolve(path))).getAsJsonObject();
    }

    @Test
    void everyAnimationJavaUsesExists() throws IOException {
        JsonObject anims = read("animations/entity/metatron.animation.json").getAsJsonObject("animations");
        for (String name : MetatronAnimations.TRIGGERED) assertTrue(anims.has("animation.metatron." + name), "missing " + name);
        for (String name : MetatronAnimations.LOOPS) assertTrue(anims.has("animation.metatron." + name), "missing loop " + name);
    }

    @Test
    void animationsOnlyTouchBonesThatExist() throws IOException {
        for (String model : new String[]{"metatron", "scribe_hand", "scribe_book"}) {
            Set<String> bones = new HashSet<>();
            read("geo/entity/" + model + ".geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                    .getAsJsonArray("bones").forEach(b -> bones.add(b.getAsJsonObject().get("name").getAsString()));
            JsonObject anims = read("animations/entity/" + model + ".animation.json").getAsJsonObject("animations");
            for (String anim : anims.keySet()) {
                for (String bone : anims.getAsJsonObject(anim).getAsJsonObject("bones").keySet()) {
                    assertTrue(bones.contains(bone), model + ": " + anim + " animates unknown bone " + bone);
                }
            }
            if (model.equals("metatron")) {
                for (String b : new String[]{org.papiricoh.supernaturalcraft.client.render.MetatronRenderer.WINGS,
                        org.papiricoh.supernaturalcraft.client.render.MetatronRenderer.ROBE,
                        org.papiricoh.supernaturalcraft.client.render.MetatronRenderer.TABLET}) assertTrue(bones.contains(b), b);
            }
        }
    }

    @Test
    void theConstructsHaveTheirClips() throws IOException {
        JsonObject hand = read("animations/entity/scribe_hand.animation.json").getAsJsonObject("animations");
        for (String n : MetatronAnimations.HAND_TRIGGERED) assertTrue(hand.has("animation.scribe_hand." + n), n);
        assertTrue(hand.has("animation.scribe_hand.idle"));
        JsonObject book = read("animations/entity/scribe_book.animation.json").getAsJsonObject("animations");
        for (String n : MetatronAnimations.BOOK_TRIGGERED) assertTrue(book.has("animation.scribe_book." + n), n);
        assertTrue(book.has("animation.scribe_book.idle"));
    }

    @Test
    void texturesExist() {
        for (int p = 1; p <= 4; p++) {
            assertTrue(Files.exists(ASSETS.resolve("textures/entity/metatron_p" + p + ".png")));
            assertTrue(Files.exists(ASSETS.resolve("textures/entity/metatron_p" + p + "_glowmask.png")));
        }
        for (String t : new String[]{"scribe_hand", "scribe_hand_glowmask", "scribe_book", "scribe_book_glowmask"}) {
            assertTrue(Files.exists(ASSETS.resolve("textures/entity/" + t + ".png")), t);
        }
        assertTrue(Files.exists(ASSETS.resolve("models/block/metatron_trophy.json")));
        assertTrue(Files.exists(ASSETS.resolve("textures/block/scripture_stone.png")));
        assertTrue(Files.exists(ASSETS.resolve("textures/item/angel_tablet.png")));
        for (int i = 0; i < 4; i++) {
            assertTrue(Files.exists(ASSETS.resolve("textures/particle/ink_" + i + ".png")));
            assertTrue(Files.exists(ASSETS.resolve("textures/particle/page_" + i + ".png")));
        }
        for (String c : new String[]{"intro", "p2", "p3", "p4", "death"}) assertTrue(Files.exists(ASSETS.resolve("cinematics/metatron_" + c + ".json")), c);
    }
}
