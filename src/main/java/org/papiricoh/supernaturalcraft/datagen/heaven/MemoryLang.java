package org.papiricoh.supernaturalcraft.datagen.heaven;

import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.memory.MemoryKind;
import org.papiricoh.supernaturalcraft.memory.MemorySets;
import org.papiricoh.supernaturalcraft.memory.MemoryText;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The memories' text (owned by the memory work): memory titles and lines, set names and bonuses, the stage's messages.
 * <p>v0.18: called from {@code SNLang.addAll}. Keys come from {@link MemoryText}: {@code memory.supernaturalcraft.kind.<kind>},
 * {@code .title.<kind>} (with per-enemy {@code .title.boss_victory.<boss>}, per-side {@code .title.ascension.<side>} and
 * {@code .title.crossroads_deal.bowl|wild}), {@code .line.<kind>} ({@code %1$s} the subject's name, {@code %2$s} the variant),
 * {@code .set.<set>} (+ {@code .bonus}).
 */
public final class MemoryLang {

    private MemoryLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        kinds(add);
        bosses(add);
        sets(add);
        messages(add);
    }

    private static void kinds(BiConsumer<String, String> add) {
        Map<MemoryKind, String[]> text = Map.of(
                MemoryKind.BOSS_VICTORY, new String[]{"A Victory", "A Great Enemy Falls", "You stood over %1$s when it was done."},
                MemoryKind.CROSSROADS_DEAL, new String[]{"A Deal", "A Kiss at the Crossroads", "You made a deal: %1$s. You knew the price."},
                MemoryKind.CASE_SOLVED, new String[]{"A Case Closed", "Case Closed", "Saving people, hunting things: %1$s, put down for good."},
                MemoryKind.CASE_LOST, new String[]{"A Case Lost", "The One That Got Away", "Not every hunt ends well. %1$s walked away."},
                MemoryKind.ASCENSION, new String[]{"A Rank", "What You Became", "You rose to rank %2$s."},
                MemoryKind.HEEDED_CALL, new String[]{"The Call", "The Messenger at Dawn", "Heaven sent for you, and you said yes."},
                MemoryKind.LEGACY_RANK, new String[]{"A Legacy", "The Men of Letters", "The Men of Letters made you rank %2$s of their order."},
                MemoryKind.PET_LOST, new String[]{"A Companion", "A Good Dog", "%1$s did not come home. Some of them wait for you up here."},
                MemoryKind.FIRST_SIGHTING, new String[]{"A First Sight", "The First Time", "The first time you saw %1$s, and lived to write it down."},
                MemoryKind.FAVOURITE_PREY, new String[]{"A Favourite", "Old Habits", "You have put down %2$s of %1$s. Practice makes perfect."});
        for (MemoryKind k : MemoryKind.values()) {
            String[] t = text.get(k);
            add.accept(MemoryText.kindKey(k), t[0]);
            add.accept(MemoryText.titleKey(k), t[1]);
            add.accept(MemoryText.PREFIX + "line." + MemoryText.kind(k), t[2]);
        }
        add.accept(MemoryText.PREFIX + "title.ascension.angel", "Wings");
        add.accept(MemoryText.PREFIX + "title.ascension.demon", "Black Eyes");
        add.accept(MemoryText.PREFIX + "title.ascension.hunter", "The Family Business");
        add.accept(MemoryText.PREFIX + "title.crossroads_deal.bowl", "A Kiss at the Crossroads");
        add.accept(MemoryText.PREFIX + "title.crossroads_deal.wild", "Where Two Roads Meet");
    }

    private static void bosses(BiConsumer<String, String> add) {
        Map<BossProgression.Boss, String> titles = new java.util.EnumMap<>(BossProgression.Boss.class);
        titles.put(BossProgression.Boss.AZAZEL, "Yellow Eyes");
        titles.put(BossProgression.Boss.LILITH, "The Last Seal");
        titles.put(BossProgression.Boss.LUCIFER, "The Devil Went Down");
        titles.put(BossProgression.Boss.GABRIEL, "Changing Channels");
        titles.put(BossProgression.Boss.WAR, "The Red Rider");
        titles.put(BossProgression.Boss.FAMINE, "The Black Rider");
        titles.put(BossProgression.Boss.PESTILENCE, "The Green Rider");
        titles.put(BossProgression.Boss.RAPHAEL, "The Storm in the Old House");
        titles.put(BossProgression.Boss.BROKEN_CHORUS, "When the Singing Stopped");
        titles.put(BossProgression.Boss.METATRON, "The Scribe's Last Word");
        titles.put(BossProgression.Boss.NAOMI, "Out of the Chair");
        titles.put(BossProgression.Boss.ZACHARIAH, "Out of Office");
        titles.put(BossProgression.Boss.AMARA, "The Darkness Before Dawn");
        titles.put(BossProgression.Boss.DEATH, "The Pale Rider");
        titles.put(BossProgression.Boss.LUCIFER_UNCAGED, "Back in the Box");
        titles.put(BossProgression.Boss.MICHAEL, "The Sword of Heaven");
        titles.put(BossProgression.Boss.CHUCK, "The End");
        for (BossProgression.Boss b : BossProgression.Boss.values()) {
            add.accept(MemoryText.PREFIX + "title.boss_victory." + b.id(), titles.getOrDefault(b, "A Great Enemy Falls"));
        }
    }

    private static void sets(BiConsumer<String, String> add) {
        set(add, MemorySets.Set.VICTORIES, "Victories", "+3% Aegis against great enemies");
        set(add, MemorySets.Set.ALL_VICTORIES, "All Victories", "+1 heart");
        set(add, MemorySets.Set.CROSSROADS, "Crossroads", "+1 day on every deal you strike");
        set(add, MemorySets.Set.CASES, "Cases", "Research takes 5% less time");
        set(add, MemorySets.Set.KIN, "Kin", "+10 max mana");
        set(add, MemorySets.Set.COMPANIONS, "Companions", "Your tamed pets mend while they are near you");
        set(add, MemorySets.Set.SIGHTINGS, "Sightings", "+5% damage to creatures you have seen (never great enemies)");
    }

    private static void set(BiConsumer<String, String> add, MemorySets.Set s, String name, String bonus) {
        add.accept(s.key(), name);
        add.accept(s.key() + ".bonus", bonus);
    }

    private static void messages(BiConsumer<String, String> add) {
        add.accept(MemoryText.message("new"), "A new memory waits for you in Heaven.");
        add.accept(MemoryText.message("collected"), "Memory gathered: %s");
        add.accept(MemoryText.message("already"), "You have already gathered this memory.");
        add.accept(MemoryText.message("not_yours"), "This memory is not yours to gather.");
        add.accept(MemoryText.message("busy"), "Someone is reliving another memory on the stage.");
        add.accept(MemoryText.message("empty_shrine"), "This shrine is empty. It is waiting for a memory.");
        add.accept(MemoryText.message("set_complete"), "Memories gathered: %s. %s.");

        String c = "commands.supernaturalcraft.memory.";
        add.accept(c + "list", "%s memories, %s gathered:");
        add.accept(c + "no_kind", "No such kind of memory.");
        add.accept(c + "added", "Remembered %s.");
        add.accept(c + "exists", "Already remembered: %s.");
        add.accept(c + "collected", "Gathered %s memories.");
        add.accept(c + "backfilled", "Backfilled: the log now holds %s memories.");
        add.accept(c + "reset", "Memories forgotten.");
        add.accept(c + "no_memory", "There is no memory number %s.");
        add.accept(c + "staged", "Staging %s: %s.");
    }
}
