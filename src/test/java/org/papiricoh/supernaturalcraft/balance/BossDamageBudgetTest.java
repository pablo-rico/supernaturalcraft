package org.papiricoh.supernaturalcraft.balance;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v0.24: with no per-hit cap on bosses, no damaging sigil may outclass the real (doubled) Angel Blade. At every tier a cast,
 * at its best potency and against an unholy boss, deals at most {@link #BURST} reference blows, and a sigil with a
 * cooldown keeps within {@link #SUSTAINED} of the blade's damage per second.
 */
class BossDamageBudgetTest {

    private static final Path SIGILS = Path.of("src/main/resources/data/supernaturalcraft/supernaturalcraft/sigil");
    /** Best potency a hunter stacks: Empower x1.5 and matching ink x1.15. */
    private static final float MAX_POTENCY = 1.5f * 1.15f;
    // Magic deals double (WeaponDamage.MAGIC_DAMAGE_SCALE), so the old budgets of 4 and 1.5 blows double too.
    private static final float BURST = 8f, SUSTAINED = 3f;
    /** The Angel Blade's attacks per second. */
    private static final float BLADE_RATE = 4f + WeaponDamage.ANGEL_BLADE_SPEED;

    @Test
    void noDamagingSigilOutclassesTheReferenceBlade() throws IOException {
        List<JsonObject> damaging = sigils().stream().filter(s -> params(s).has("damage")).toList();
        assertFalse(damaging.isEmpty(), "no damaging sigils found under " + SIGILS);
        for (JsonObject s : damaging) {
            String behavior = s.get("behavior").getAsString();
            String path = behavior.substring(behavior.indexOf(':') + 1);
            JsonObject p = params(s);
            float perCast = p.get("damage").getAsFloat() * (p.has("vs_unholy") ? p.get("vs_unholy").getAsFloat() : 1f)
                    * MAX_POTENCY * SpellBossDamage.effectScale(path) * WeaponDamage.MAGIC_DAMAGE_SCALE;
            for (int t = 1; t <= ProgressionScale.MAX_TIER; t++) {
                float cast = perCast * ProgressionScale.ascensionMultiplier(t), ref = ProgressionScale.bladeHit(t);
                assertTrue(cast <= BURST * ref, path + " at tier " + t + ": " + cast + " per cast vs reference " + ref);
                if (s.has("cooldown")) {
                    float dps = cast * 20f / Math.max(1, s.get("cooldown").getAsInt());
                    assertTrue(dps <= SUSTAINED * ref * BLADE_RATE, path + " at tier " + t + ": " + dps + " per second");
                }
            }
        }
    }

    private static List<JsonObject> sigils() throws IOException {
        try (Stream<Path> files = Files.list(SIGILS)) {
            return files.filter(f -> f.toString().endsWith(".json")).map(BossDamageBudgetTest::read).toList();
        }
    }

    private static JsonObject read(Path f) {
        try {
            return JsonParser.parseString(Files.readString(f)).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException(f.toString(), e);
        }
    }

    private static JsonObject params(JsonObject sigil) {
        return sigil.has("params") ? sigil.getAsJsonObject("params") : new JsonObject();
    }
}
