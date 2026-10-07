package org.papiricoh.supernaturalcraft.datagen.journal;

import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.adv;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.any;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.has;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.ritual;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.seen;
import static org.papiricoh.supernaturalcraft.datagen.journal.SNJournal.entry;

/** Where the hunts take you: the Hymnal Spire, Hell and its regions, the Cage. */
final class JournalPlaces {

    private JournalPlaces() {
    }

    static void add(List<SNJournal.Entry> out) {
        out.add(entry("hymnal_spire", JournalChapter.PLACES).order(0).icon(AllItems.SHATTERED_HYMN)
                .unlock(any(adv("christo"), adv("hymnal_spire"), has(AllItems.SHATTERED_HYMN), has(AllItems.CHOIR_SHARD)))
                .title("The Hymnal Spire")
                .text("High on the mountains stand ruins of white stone, a stair winding up to a broken platform on the peak. To find "
                        + "one, draw a Hymnal Map: a small circle, a compass, a feather, a gold ingot, an amethyst shard and a holy water "
                        + "on the altar, and a blank map to light it.")
                .recipe(ritual("hymnal_map"), "Drawing a Hymnal Map")
                .text("Halfway up the stair stands a temple. Its window keeps the hymn, three panes read left to right, and its vault "
                        + "always keeps a Shattered Hymn. The rest of its chests hold holy water, salt, ink and, if you are lucky, a few "
                        + "of the Colt's rounds.")
                .items("The Shattered Hymn", AllItems.SHATTERED_HYMN)
                .text("On the summit wait the Choir Altar and its seven bells. In a storm, the altar's light reaches the clouds. Read "
                        + "the Broken Chorus's page before you ring anything."));

        out.add(entry("hell", JournalChapter.PLACES).order(1).icon(AllItems.BRIMSTONE)
                .unlock(any(adv("devil_went_down"), adv("highway_to_hell")))
                .title("The Road to Hell")
                .text("Only those who have beaten Lucifer can open the way. In the Overworld it takes the void circle and the Key, "
                        + "at night, with demon blood, embers, sulfur, a nether star and holy water.")
                .recipe(ritual("open_hell_rift"), "A rift from the Overworld")
                .text("In the Nether it is cheaper: a great circle, two demon blood, two embers, sulfur and a blaze rod, lit with "
                        + "flint and steel at any hour.")
                .recipe(ritual("open_hell_rift_nether"), "A rift from the Nether")
                .text("A rift stays open twenty minutes. Like the Nether, one step in Hell is eight up here, and the way back opens "
                        + "where you land. Lost down there? A binding circle in Hell with two salt, a holy water and brimstone tears a "
                        + "rift home, to your bed.")
                .recipe(ritual("escape_hell"), "The way home"));

        out.add(entry("hell_regions", JournalChapter.PLACES).order(2).icon(AllItems.DAMNED_CONTRACT)
                .unlock(adv("highway_to_hell"))
                .title("The Regions of Hell")
                .text("Hell is a roofed cavern world, like the Nether but taller. The Pit is at its heart; three regions lie around it.")
                .text("The Ash Wastes burn hottest: brimstone in the rock. The Rack is wet with blood, and chains hang from its roof "
                        + "with meat hooks on the end. Crowley's Corridors are Hell as he ran it, an endless queue: a maze of stone "
                        + "corridors lined with barred cells, some keeping a chest, and in them, contracts.")
                .items("What Hell gives", AllItems.BRIMSTONE, AllItems.RACK_HOOK, AllItems.CONGEALED_BLOOD, AllItems.DAMNED_CONTRACT,
                        AllItems.ABYSSAL_SHARD, AllItems.HELLHOUND_FANG)
                .text("Shards come from the walls of the Pit, fangs from hellhounds. Demons and occultists roam all of it, and the "
                        + "hounds hunt there in packs.")
                .text("Hell gets into you. Torment grows while you stay, faster on the Rack, and fades once you leave: red at the edge "
                        + "of sight, whispers, shapes in the fog. It never hurts you. Salt in your off hand or a dash of holy water "
                        + "eases it."));

        out.add(entry("the_cage", JournalChapter.PLACES).order(3).icon(AllItems.ABYSSAL_SHARD)
                .unlock(any(seen(AllEntities.CAGED_LUCIFER.get()), adv("highway_to_hell")))
                .creature(AllEntities.CAGED_LUCIFER.get())
                .title("The Cage")
                .entity(AllEntities.CAGED_LUCIFER.get(), "Lucifer, waiting in chains")
                .text("At the heart of Hell, at its very centre, the Pit falls sheer to the lava sea. Over it, on its chains, hangs the "
                        + "Cage, and under the Cage an island with a dais and four old stones.")
                .text("Lucifer waits inside, in chains, head bowed. Now and then he looks up at whoever has come so far. Nothing can "
                        + "touch him while the Cage is shut.")
                .items("The four rings open it", AllItems.RING_OF_WAR, AllItems.RING_OF_FAMINE, AllItems.RING_OF_PESTILENCE,
                        AllItems.RING_OF_DEATH, AllItems.KEY_TO_THE_CAGE)
                .text("The iris in its floor opens only for the four rings and the Key. When it does, he comes down."));
    }
}
