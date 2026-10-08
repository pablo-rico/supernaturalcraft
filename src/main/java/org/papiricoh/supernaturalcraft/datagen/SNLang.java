package org.papiricoh.supernaturalcraft.datagen;

import java.util.function.BiConsumer;

/** Free-form strings: GUI text, chat messages, sigil names. Grouped by system. */
public class SNLang {

    static void addAll(BiConsumer<String, String> add) {
        org.papiricoh.supernaturalcraft.datagen.horsemen.HorsemenLang.add(add);
        org.papiricoh.supernaturalcraft.datagen.michael.MichaelLang.add(add);
        org.papiricoh.supernaturalcraft.datagen.allegiance.AllegianceLang.add(add);
        org.papiricoh.supernaturalcraft.datagen.allegiance.AllegianceUiLang.add(add);
        org.papiricoh.supernaturalcraft.datagen.gabriel.GabrielLang.add(add);
        org.papiricoh.supernaturalcraft.datagen.gabriel.GabrielUiLang.add(add);
        org.papiricoh.supernaturalcraft.datagen.raphael.RaphaelLang.add(add);
        org.papiricoh.supernaturalcraft.datagen.raphael.RaphaelServerLang.add(add);
        org.papiricoh.supernaturalcraft.datagen.balance.BalanceLang.add(add);
        author(add);
        hunter(add);
        magic(add);
        ritual(add);
        lucifer(add);
        rewards(add);
        advancements(add);
        journal(add);
        arsenal(add);
        chorus(add);
        hell(add);
        azazel(add);
        lilith(add);
        metatron(add);
        spellBowl(add);
        book(add);
        sigils(add);
    }

    /** v0.10: the Author. Each part's strings live in its own class (combat, arena, client effects, the world). */
    static void author(BiConsumer<String, String> add) {
        org.papiricoh.supernaturalcraft.datagen.chuck.ChuckLang.add(add);
        org.papiricoh.supernaturalcraft.datagen.chuck.ChuckArenaLang.add(add);
        org.papiricoh.supernaturalcraft.datagen.chuck.FourthWallLang.add(add);
        org.papiricoh.supernaturalcraft.datagen.chuck.AuthorWorldLang.add(add);
        adv(add, "the_author", "The Author", "Find the cabin at the end of the map, and the man waiting in it");
        adv(add, "the_end", "The End", "Pass the Author's test");
    }

    /** v0.9: the Hunter's Book, its journal entries and roadmap. */
    static void book(BiConsumer<String, String> add) {
        org.papiricoh.supernaturalcraft.datagen.lang.BookLang.add(add);
        org.papiricoh.supernaturalcraft.datagen.lang.HomeLang.add(add);
        org.papiricoh.supernaturalcraft.datagen.lang.JournalUiLang.add(add);
        org.papiricoh.supernaturalcraft.datagen.lang.ScriptoriumLang.add(add);
        org.papiricoh.supernaturalcraft.datagen.lang.RoadmapLang.add(add);
        org.papiricoh.supernaturalcraft.datagen.journal.SNJournal.addLang(add);
        org.papiricoh.supernaturalcraft.datagen.journal.SNRoadmap.addLang(add);
    }

    /** v0.8: the spell bowl, ghosts and the crossroads. Each part's strings live in datagen/lang. */
    static void spellBowl(BiConsumer<String, String> add) {
        org.papiricoh.supernaturalcraft.datagen.lang.BowlLang.add(add);
        org.papiricoh.supernaturalcraft.datagen.lang.GhostLang.add(add);
        org.papiricoh.supernaturalcraft.datagen.lang.CrossroadsLang.add(add);
        org.papiricoh.supernaturalcraft.datagen.lang.SpellLang.add(add);
        org.papiricoh.supernaturalcraft.datagen.lang.ContentLang.add(add);
        adv(add, "first_spell", "Words of Power", "Cast a spell from a spell bowl");
        adv(add, "salt_and_burn", "Salt and Burn", "Lay a ghost to rest by salting and burning its bones");
        adv(add, "deal_with_the_devil", "Sealed with a Kiss", "Make a deal with a crossroads demon");
        adv(add, "debt_paid", "Off the Hook", "Get out of a crossroads deal alive");
    }

    /** Metatron, the Scribe of God: his Hand and Book, his lectern, the Angel Tablet and the Word. */
    static void metatron(BiConsumer<String, String> add) {
        add.accept("entity.supernaturalcraft.metatron.phase1", "Metatron, the Scribe");
        add.accept("entity.supernaturalcraft.metatron.phase2", "Metatron, the Hand of God");
        add.accept("entity.supernaturalcraft.metatron.phase3", "Metatron, the Library of Heaven");
        add.accept("entity.supernaturalcraft.metatron.phase4", "Metatron, the Angel Tablet");
        add.accept("cinematic.supernaturalcraft.metatron.title", "METATRON");
        add.accept("cinematic.supernaturalcraft.metatron.subtitle", "The Scribe of God. You wrote his name; he has written yours.");
        add.accept("cinematic.supernaturalcraft.metatron.phase2.title", "THE HAND OF GOD");
        add.accept("cinematic.supernaturalcraft.metatron.phase2.subtitle", "Get out of the strokes before the words burn.");
        add.accept("cinematic.supernaturalcraft.metatron.phase3.title", "THE LIBRARY OF HEAVEN");
        add.accept("cinematic.supernaturalcraft.metatron.phase3.subtitle", "He will not leave his lectern: climb the stairs, or strike from afar.");
        add.accept("cinematic.supernaturalcraft.metatron.phase4.title", "THE ANGEL TABLET");
        add.accept("cinematic.supernaturalcraft.metatron.phase4.subtitle", "Heed the Word. Watch the sky.");
        add.accept("cinematic.supernaturalcraft.metatron.death.subtitle", "The pages fall still.");
        add.accept("cinematic.supernaturalcraft.metatron.victory.title", "THE END OF THE STORY");
        add.accept("cinematic.supernaturalcraft.metatron.victory.subtitle", "The Tablet is yours. Write carefully.");
        add.accept("message.supernaturalcraft.metatron.busy", "Another fight already holds this world.");
        add.accept("message.supernaturalcraft.metatron.victorious", "\"Every story needs an ending.\" He closes his book and is gone.");
        add.accept("message.supernaturalcraft.metatron.cage_gone", "The library folds away. He is gone.");
        add.accept("word.supernaturalcraft.subtitle", "The Word of God");
        add.accept("word.supernaturalcraft.be_still", "BE STILL");
        add.accept("word.supernaturalcraft.look_away", "LOOK AWAY");
        add.accept("word.supernaturalcraft.kneel", "KNEEL");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.summon_metatron", "Calls down Metatron, the Scribe of God");
        add.accept("jei.supernaturalcraft.info.angel_tablet", "Dropped by Metatron. Once Lucifer has fallen, write \"Metatron\" in a book and quill and "
                + "call him down at night in a great circle with it, two choir shards, two holy water, two feathers and a glow ink sac.");
        add.accept("tooltip.supernaturalcraft.angel_tablet", "The word of God, cut in stone. It still burns.");
        add.accept("tooltip.supernaturalcraft.angel_tablet.use", "Use: rewrites your story; healed whole, every harm undone (2 min)");
        adv(add, "scribe_of_god", "The Scribe of God", "Defeat Metatron");
        adv(add, "obeyed", "Thy Will Be Done", "Keep all three of Metatron's Words in one fight");
    }

    /** Lilith, the first demon: her contracts, her white light, and the last seal. */
    static void lilith(BiConsumer<String, String> add) {
        add.accept("entity.supernaturalcraft.lilith.phase1", "Lilith, the First Demon");
        add.accept("entity.supernaturalcraft.lilith.phase2", "Lilith, Holder of Contracts");
        add.accept("entity.supernaturalcraft.lilith.phase3", "Lilith, the Last Seal");
        add.accept("cinematic.supernaturalcraft.lilith.title", "LILITH");
        add.accept("cinematic.supernaturalcraft.lilith.subtitle", "The first demon. When she shines, get behind a headstone.");
        add.accept("cinematic.supernaturalcraft.lilith.phase2.title", "THE CONTRACT COMES DUE");
        add.accept("cinematic.supernaturalcraft.lilith.phase2.subtitle", "Wound her to burn a contract before the hounds come.");
        add.accept("cinematic.supernaturalcraft.lilith.phase3.title", "THE LAST SEAL");
        add.accept("cinematic.supernaturalcraft.lilith.phase3.subtitle", "Her light comes twice now.");
        add.accept("cinematic.supernaturalcraft.lilith.death.subtitle", "Her light goes out. Somewhere, a seal breaks.");
        add.accept("cinematic.supernaturalcraft.lilith.victory.title", "LUCIFER RISING");
        add.accept("cinematic.supernaturalcraft.lilith.victory.subtitle", "The last seal is broken. The Cage can be opened.");
        add.accept("message.supernaturalcraft.lilith.busy", "Another fight already holds this world.");
        add.accept("message.supernaturalcraft.lilith.victorious", "She smiles, and the white light takes her somewhere else.");
        add.accept("message.supernaturalcraft.lilith.cage_gone", "The circle is broken. She is gone.");
        add.accept("message.supernaturalcraft.lilith.contract_signed", "Your name is written in her contract. Wound her, all of you, before it comes due.");
        add.accept("message.supernaturalcraft.lilith.contract_due", "The contract comes due in %s...");
        add.accept("message.supernaturalcraft.lilith.contract_called", "The contract is due. Can you hear the dogs?");
        add.accept("message.supernaturalcraft.lilith.contract_burned", "The contract burns.");
        add.accept("message.supernaturalcraft.lilith.light_warning", "Her light is rising: get behind a headstone!");
        add.accept("message.supernaturalcraft.whistle.already", "A hound already runs at your side.");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.summon_lilith", "Calls up Lilith, the First Demon");
        add.accept("jei.supernaturalcraft.info.last_seal", "Dropped by Lilith. The last of the sixty-six seals: Lucifer's summoning needs it. "
                + "Once Azazel has fallen, call her up at night in a great circle with three demon blood, two hellfire embers, two bones "
                + "and a sheet of paper, lit with flint and steel.");
        add.accept("jei.supernaturalcraft.info.hound_whistle", "Dropped by Lilith. Blow it to call a hellhound to your side for a minute.");
        add.accept("tooltip.supernaturalcraft.last_seal", "The sixty-sixth seal. Break it, and the Cage opens.");
        add.accept("tooltip.supernaturalcraft.hound_whistle", "\"You're going to feel this.\"");
        add.accept("tooltip.supernaturalcraft.hound_whistle.use", "Use: a hellhound runs at your side for a minute (%s mana)");
        adv(add, "lucifer_rising", "Lucifer Rising", "Defeat Lilith and break the last seal");
        adv(add, "no_deal", "No Deal", "Burn one of Lilith's contracts before it comes due");
    }

    /** Azazel, the Yellow-Eyed Demon: the first boss, and Samuel Colt's rails that hold him. */
    static void azazel(BiConsumer<String, String> add) {
        add.accept("entity.supernaturalcraft.azazel.phase1", "Azazel, the Yellow-Eyed Demon");
        add.accept("entity.supernaturalcraft.azazel.phase2", "Azazel, Smoke and Fire");
        add.accept("cinematic.supernaturalcraft.azazel.title", "AZAZEL");
        add.accept("cinematic.supernaturalcraft.azazel.subtitle", "The Yellow-Eyed Demon. Get him into the rails.");
        add.accept("cinematic.supernaturalcraft.azazel.phase2.title", "SMOKE AND FIRE");
        add.accept("cinematic.supernaturalcraft.azazel.phase2.subtitle", "When he rushes as smoke, stand behind the rails.");
        add.accept("cinematic.supernaturalcraft.azazel.death.subtitle", "The smoke is torn out of him and drawn down into the ground.");
        add.accept("cinematic.supernaturalcraft.azazel.victory.title", "SAVING PEOPLE, HUNTING THINGS");
        add.accept("cinematic.supernaturalcraft.azazel.victory.subtitle", "His blood is on the ground. The Key to the Cage needs it.");
        add.accept("message.supernaturalcraft.azazel.busy", "Another fight already holds this world.");
        add.accept("message.supernaturalcraft.azazel.victorious", "He slips out of the circle as yellow smoke, laughing.");
        add.accept("message.supernaturalcraft.azazel.cage_gone", "The circle is broken. He is gone.");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.summon_azazel", "Calls up Azazel, the Yellow-Eyed Demon");
        add.accept("jei.supernaturalcraft.info.azazel_blood", "Dropped by Azazel, two vials every time he falls. The Key to the Cage is forged "
                + "with it. Call him up at night with a great circle, three demon blood, three sulfur and two salt, lit with flint and steel.");
        add.accept("tooltip.supernaturalcraft.azazel_blood", "He fed it to the special children in their cribs.");
        adv(add, "yellow_eyed", "Saving People, Hunting Things", "Defeat Azazel, the Yellow-Eyed Demon");
        adv(add, "railroaded", "Railroaded", "Hold Azazel in Samuel Colt's rails");
    }

    /** Hell: its rifts and regions, the Horsemen's rings, the Cage and Lucifer Uncaged. */
    static void hell(BiConsumer<String, String> add) {
        String[] phases = {"Lucifer, the Prisoner", "Lucifer, Hellfire", "Lucifer, Cold of the Cage", "Lucifer, Lord of the Legion",
                "Lucifer, the Morning Star", "Lucifer, Light-Bringer"};
        for (int i = 0; i < phases.length; i++) add.accept("entity.supernaturalcraft.lucifer_uncaged.phase" + (i + 1), phases[i]);
        add.accept("cinematic.supernaturalcraft.uncaged.title", "LUCIFER UNCAGED");
        add.accept("cinematic.supernaturalcraft.uncaged.subtitle", "You opened the door. Now I walk through it.");
        String[][] titles = {
                {"HELLFIRE", "Every soul down here burned once. Now it is your turn."},
                {"THE COLD OF THE CAGE", "Do you know how cold it gets in there?"},
                {"LEGION", "Kill the hounds, or they will drag you down."},
                {"THE MORNING STAR", "Watch for gold on the ground. The stars are falling."},
                {"LIGHT-BRINGER", "Stand between the wings. When he burns, find the green."}};
        for (int i = 0; i < titles.length; i++) {
            add.accept("cinematic.supernaturalcraft.uncaged.phase" + (i + 2) + ".title", titles[i][0]);
            add.accept("cinematic.supernaturalcraft.uncaged.phase" + (i + 2) + ".subtitle", titles[i][1]);
        }
        add.accept("cinematic.supernaturalcraft.uncaged.death.subtitle", "The chains remember him.");
        add.accept("cinematic.supernaturalcraft.uncaged.victory.title", "BACK IN THE BOX");
        add.accept("cinematic.supernaturalcraft.uncaged.victory.subtitle", "The Cage is shut. Hell is quiet. For now.");
        add.accept("message.supernaturalcraft.uncaged.free", "The Cage is open. He is coming down.");
        add.accept("message.supernaturalcraft.uncaged.already_free", "The Cage is already open.");
        add.accept("message.supernaturalcraft.uncaged.victorious", "He goes back into the Cage on his own. The Rings are left on the stone.");
        add.accept("message.supernaturalcraft.rift.closing", "The rift is closing...");

        add.accept("biome.supernaturalcraft.the_rack", "The Rack");
        add.accept("biome.supernaturalcraft.ash_wastes", "Ash Wastes");
        add.accept("biome.supernaturalcraft.crowleys_corridors", "Crowley's Corridors");
        add.accept("biome.supernaturalcraft.the_pit", "The Pit");

        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.open_hell_rift", "Tears a rift into Hell behind the altar");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.escape_hell", "Tears a rift home, to your bed");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.summon_lucifer_uncaged", "Opens the Cage: Lucifer Uncaged");
        add.accept("jei.supernaturalcraft.ritual.eclipse", "Under an eclipse");
        add.accept("jei.supernaturalcraft.ritual.dimension", "Only in %s");
        add.accept("jei.supernaturalcraft.ritual.requires", "Needs: %s");
        add.accept("jei.supernaturalcraft.dimension.minecraft.overworld", "the Overworld");
        add.accept("jei.supernaturalcraft.dimension.minecraft.the_nether", "the Nether");
        add.accept("jei.supernaturalcraft.dimension.minecraft.the_end", "the End");
        add.accept("jei.supernaturalcraft.dimension.supernaturalcraft.hell", "Hell");
        add.accept("jei.supernaturalcraft.info.rings", "The Rings of the Four Horsemen. Each is won from its Horseman: call up War, Famine "
                + "and Pestilence by their rites once Lucifer has fallen, then offer their three rings in a great circle to call up Death. Laid "
                + "together on the dais under Lucifer's Cage, at the heart of the Pit, the four open it.");
        add.accept("jei.supernaturalcraft.info.fallen_star", "Dropped by Lucifer Uncaged. The rarest thing there is; its use is still to come.");
        add.accept("jei.supernaturalcraft.info.hell_materials", "Found in Hell: brimstone in the Ash Wastes, hooks on the Rack, contracts "
                + "in Crowley's Corridors, shards in the walls of the Pit, fangs from hellhounds.");

        add.accept("tooltip.supernaturalcraft.brimstone", "Sulfur from Hell's own fires.");
        add.accept("tooltip.supernaturalcraft.rack_hook", "From the Rack, where souls are taken apart.");
        add.accept("tooltip.supernaturalcraft.damned_contract", "Signed in blood. Ten years, then the hounds come.");
        add.accept("tooltip.supernaturalcraft.abyssal_shard", "Rock from the walls of the Pit, cold as the Cage.");
        add.accept("tooltip.supernaturalcraft.hellhound_fang", "You never saw what it came out of.");
        add.accept("tooltip.supernaturalcraft.fallen_star", "What is left when the Morning Star falls.");
        add.accept("tooltip.supernaturalcraft.ring_of_war", "\"I'm War.\"");
        add.accept("tooltip.supernaturalcraft.ring_of_famine", "\"I'm Famine. I'm always hungry.\"");
        add.accept("tooltip.supernaturalcraft.ring_of_pestilence", "\"Pestilence. I make things sick.\"");
        add.accept("tooltip.supernaturalcraft.ring_of_death", "\"I'm older than God.\"");

        adv(add, "highway_to_hell", "Highway to Hell", "Cross a rift into Hell");
        adv(add, "hellhound_heel", "Hellhound on My Trail", "Take a fang from a hellhound");
        adv(add, "four_horsemen", "The Four Horsemen", "Hold the rings of War, Famine, Pestilence and Death");
        adv(add, "back_in_the_box", "Back in the Box", "Defeat Lucifer Uncaged and shut the Cage");
    }

    /** The Broken Chorus, the Hymnal Spire and their pieces. */
    static void chorus(BiConsumer<String, String> add) {
        add.accept("entity.supernaturalcraft.broken_chorus.phase1", "The Broken Chorus");
        add.accept("entity.supernaturalcraft.broken_chorus.phase2", "The Broken Chorus, Six-Winged");
        add.accept("entity.supernaturalcraft.broken_chorus.phase3", "The Broken Chorus, Wheel Within Wheel");
        add.accept("entity.supernaturalcraft.broken_chorus.phase4", "The Last Voice");
        add.accept("cinematic.supernaturalcraft.chorus.title", "THE BROKEN CHORUS");
        add.accept("cinematic.supernaturalcraft.chorus.subtitle", "We sang for Him. He did not come back.");
        add.accept("cinematic.supernaturalcraft.chorus.phase2.title", "SIX-WINGED");
        add.accept("cinematic.supernaturalcraft.chorus.phase2.subtitle", "Bring its wings down to the stone.");
        add.accept("cinematic.supernaturalcraft.chorus.phase3.title", "WHEEL WITHIN WHEEL");
        add.accept("cinematic.supernaturalcraft.chorus.phase3.subtitle", "Strike the eyes while they are open. Do not meet their gaze.");
        add.accept("cinematic.supernaturalcraft.chorus.phase4.title", "THE LAST VOICE");
        add.accept("cinematic.supernaturalcraft.chorus.phase4.subtitle", "Only the shade of the pillars will hide you.");
        add.accept("cinematic.supernaturalcraft.chorus.victory.title", "SILENCE");
        add.accept("cinematic.supernaturalcraft.chorus.victory.subtitle", "The choir has finished its song.");
        add.accept("message.supernaturalcraft.chorus.kneels", "The Hymn breaks! The Chorus kneels.");
        add.accept("message.supernaturalcraft.chorus.hymn", "The Chorus begins its Hymn! Ring the bell it sings.");
        add.accept("message.supernaturalcraft.chorus.gaze", "An eye opens upon you. Look away, or hide behind stone!");
        add.accept("message.supernaturalcraft.chorus.shade", "Its light swells. Only a pillar's shadow will hide you!");
        add.accept("message.supernaturalcraft.chorus.arena_gone", "The Chorus has nothing left to hold it here.");
        add.accept("message.supernaturalcraft.chorus.victorious", "The Chorus rises back into the storm, unanswered.");
        add.accept("message.supernaturalcraft.chorus.arena_taken", "Another fight already holds this world.");
        add.accept("message.supernaturalcraft.choir_altar.tuning", "Ring three bells: they become this altar's hymn.");
        add.accept("message.supernaturalcraft.choir_altar.tuned", "The altar's hymn is now %s");
        add.accept("message.supernaturalcraft.choir_altar.hymn", "This altar's hymn: %s");
        add.accept("message.supernaturalcraft.choir_altar.already", "A hymn already lies on the altar.");
        add.accept("message.supernaturalcraft.choir_altar.no_storm", "The bells are dull. Only a storm carries the hymn to heaven.");
        add.accept("message.supernaturalcraft.choir_altar.armed", "The Shattered Hymn waits. Ring its notes upon the bells.");
        add.accept("message.supernaturalcraft.choir_altar.discord", "Discord! The hymn falls apart.");
        add.accept("tooltip.supernaturalcraft.shattered_hymn", "Lay it on a Choir Altar in a storm, then ring its notes.");
        add.accept("item.supernaturalcraft.hymnal_map", "Hymnal Map");
        add.accept("message.supernaturalcraft.locate.nothing", "The map stays blank: nothing of the kind lies near enough.");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.locate_structure", "Draws a map to the nearest Hymnal Spire, high in the mountains.");
        add.accept("book.supernaturalcraft.hymn.page1", "We were a choir of many, and we sang before the Throne. Then the Throne was empty, and our voices ran together, and we became one thing, and that thing is broken.");
        add.accept("book.supernaturalcraft.hymn.page2", "Above, on the summit, the seven bells wait. Bring the Shattered Hymn to the altar while the storm is overhead, and ring three notes.");
        add.accept("book.supernaturalcraft.hymn.page3", "Which three? The glass behind this book remembers. Read it as you read these pages: from left to right.");
    }

    private static void hunter(BiConsumer<String, String> add) {
        add.accept("tooltip.supernaturalcraft.hunters_amulet", "Warms near the supernatural. Sigils cost 10% less while worn.");
        add.accept("message.supernaturalcraft.amulet.warm", "Your amulet grows warm…");
    }

    private static void magic(BiConsumer<String, String> add) {
        add.accept("key.categories.supernaturalcraft", "SupernaturalCraft");
        add.accept("key.supernaturalcraft.cycle_spell", "Turn grimoire page");

        add.accept("item.supernaturalcraft.spell_scroll.named", "Scroll of %s");
        add.accept("item.supernaturalcraft.sigil_page.named", "Sigil Page: %s");
        add.accept("spell.supernaturalcraft.unnamed", "Unfinished spell");
        add.accept("spell.supernaturalcraft.auto_name", "%1$s of %2$s");
        add.accept("tooltip.supernaturalcraft.grimoire.hint", "Use to cast · Sneak-use to compose · Sneak+scroll to turn pages");

        add.accept("message.supernaturalcraft.grimoire.page", "Page %s: ");
        add.accept("message.supernaturalcraft.grimoire.blank", "blank");
        add.accept("message.supernaturalcraft.grimoire.empty_page", "This page is blank. Sneak-use the grimoire to compose a spell.");
        add.accept("message.supernaturalcraft.grimoire.first_open",
                "The grimoire's first pages make sense to you now: Touch, Bolt, Smite and Mend. Sneak-use it to compose a spell.");
        add.accept("message.supernaturalcraft.cast.incomplete", "The spell is unfinished, or names a sigil that no longer exists.");
        add.accept("message.supernaturalcraft.cast.cooldown", "Your hand is still shaking from the last sigil.");
        add.accept("message.supernaturalcraft.cast.no_mana", "Not enough mana.");
        add.accept("message.supernaturalcraft.cast.no_reagents", "You are missing the reagents this spell burns.");
        add.accept("message.supernaturalcraft.page.illegible", "The ink has run; this page is illegible.");
        add.accept("message.supernaturalcraft.page.needs_grace", "This sigil burns your eyes. Only someone touched by grace could learn it.");
        add.accept("message.supernaturalcraft.page.known", "You already know this sigil.");
        add.accept("message.supernaturalcraft.page.learned", "You learned the sigil %s.");
        add.accept("message.supernaturalcraft.compose.incomplete", "A spell needs a form and at least one effect.");
        add.accept("message.supernaturalcraft.compose.unknown", "You don't know every sigil in that spell.");
        add.accept("message.supernaturalcraft.compose.duplicate", "The same effect can't be drawn twice.");
        add.accept("message.supernaturalcraft.compose.need_ink", "Inscribing a page takes a vial of Enochian ink.");
        add.accept("message.supernaturalcraft.compose.need_paper_ink", "A scroll takes a sheet of paper and a vial of Enochian ink.");

        add.accept("screen.supernaturalcraft.composer", "Grimoire");
        add.accept("screen.supernaturalcraft.composer.name", "Spell name");
        add.accept("screen.supernaturalcraft.composer.form", "Form");
        add.accept("screen.supernaturalcraft.composer.effects", "Effects");
        add.accept("screen.supernaturalcraft.composer.modifiers", "Modifiers");
        add.accept("screen.supernaturalcraft.composer.inscribe", "Inscribe");
        add.accept("screen.supernaturalcraft.composer.scroll", "Write Scroll");
        add.accept("screen.supernaturalcraft.composer.clear", "Clear");
        add.accept("screen.supernaturalcraft.composer.incomplete", "Needs a form and an effect");
        add.accept("screen.supernaturalcraft.composer.mana", "Mana: %s");
        add.accept("screen.supernaturalcraft.composer.multiplier", "Mana ×%s");
        add.accept("screen.supernaturalcraft.composer.known.form", "Forms");
        add.accept("screen.supernaturalcraft.composer.known.effect", "Effects");
        add.accept("screen.supernaturalcraft.composer.known.modifier", "Modifiers");
        add.accept("screen.supernaturalcraft.composer.kind.form", "Form");
        add.accept("screen.supernaturalcraft.composer.kind.effect", "Effect");
        add.accept("screen.supernaturalcraft.composer.kind.modifier", "Modifier");
    }

    private static void ritual(BiConsumer<String, String> add) {
        add.accept("message.supernaturalcraft.ritual.busy", "A ritual is already under way.");
        add.accept("message.supernaturalcraft.ritual.altar_full", "There is no room left on the altar.");
        add.accept("message.supernaturalcraft.ritual.unknown_pattern", "This ritual names a circle no one has drawn before.");
        add.accept("message.supernaturalcraft.ritual.broken_circle", "The circle is incomplete. Look for the red sparks.");
        add.accept("message.supernaturalcraft.ritual.needs_night", "This rite can only be performed at night.");
        add.accept("message.supernaturalcraft.ritual.needs_day", "This rite can only be performed by day.");
        add.accept("message.supernaturalcraft.ritual.needs_rain", "This rite can only be performed in the rain.");
        add.accept("message.supernaturalcraft.ritual.needs_thunder", "This rite can only be performed in a thunderstorm.");
        add.accept("message.supernaturalcraft.ritual.wrong_dimension", "This rite cannot be performed here.");
        add.accept("message.supernaturalcraft.ritual.no_mana", "You lack the mana to begin this rite.");
        add.accept("message.supernaturalcraft.ritual.backlash", "The circle broke. Something came through.");
        add.accept("message.supernaturalcraft.ritual.unanswered", "The rite ends, and nothing answers.");
        add.accept("tooltip.supernaturalcraft.key_to_the_cage", "Opens what should stay closed.");
        add.accept("jei.supernaturalcraft.ritual", "Ritual");
        add.accept("jei.supernaturalcraft.ritual.night", "Only at night");
        add.accept("jei.supernaturalcraft.ritual.day", "Only by day");
        add.accept("jei.supernaturalcraft.ritual.weather.rain", "Only in the rain");
        add.accept("jei.supernaturalcraft.ritual.weather.thunder", "Only in a thunderstorm");
        add.accept("jei.supernaturalcraft.ritual.mana", "Mana: %s");
        add.accept("jei.supernaturalcraft.ritual.duration", "%ss");
        add.accept("jei.supernaturalcraft.ritual.activator", "Light with");
        add.accept("jei.supernaturalcraft.ritual.consumed", "(consumed)");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.exorcise", "Casts out every trapped demon nearby, leaving hellfire embers.");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.summon_lucifer", "Opens the Cage.");
        add.accept("jei.supernaturalcraft.sigils", "Sigils");
    }

    private static void lucifer(BiConsumer<String, String> add) {
        add.accept("entity.supernaturalcraft.lucifer.phase1", "Lucifer");
        add.accept("entity.supernaturalcraft.lucifer.phase2", "Lucifer, the Fallen");
        add.accept("entity.supernaturalcraft.lucifer.phase3", "Lucifer, Morningstar");
        add.accept("entity.supernaturalcraft.lucifer.phase4", "Lucifer, Archangel Unbound");
        add.accept("cinematic.supernaturalcraft.emerge.title", "LUCIFER");
        add.accept("cinematic.supernaturalcraft.emerge.subtitle", "Hello, Sam. Hello, Dean. Hello, whoever you are.");
        add.accept("cinematic.supernaturalcraft.phase2.title", "THE FALLEN");
        add.accept("cinematic.supernaturalcraft.phase3.title", "THE CAGE REMEMBERS");
        add.accept("cinematic.supernaturalcraft.phase4.title", "ARCHANGEL UNBOUND");
        add.accept("cinematic.supernaturalcraft.phase4.subtitle", "Find shelter from the light.");
        add.accept("cinematic.supernaturalcraft.smite.warning", "He gathers his grace — find a safe sigil or a Ward!");
        add.accept("cinematic.supernaturalcraft.victory.title", "CAST DOWN");
        add.accept("cinematic.supernaturalcraft.victory.subtitle", "The Cage is closed. For now.");
        add.accept("message.supernaturalcraft.lucifer.already_free", "The Cage is already open somewhere in this world.");
        add.accept("message.supernaturalcraft.lucifer.victorious", "\"That's the thing about cages. They're only as strong as the ones who keep them.\" Lucifer returns below, and the key cracks.");
        add.accept("message.supernaturalcraft.lucifer.cage_gone", "The Cage folds shut around Lucifer and drags him under.");
        add.accept("message.supernaturalcraft.arena.barrier", "The Cage will not let you leave.");
        add.accept("message.supernaturalcraft.arena.no_escape", "Something in the Cage swallows the jump.");
    }

    static void rewards(BiConsumer<String, String> add) {
        add.accept("tooltip.supernaturalcraft.archangel_blade", "Use: smite everything before you (%s mana)");
        add.accept("tooltip.supernaturalcraft.the_colt", "\"There's nothing this gun can't kill.\" Except archangels, and the great enemies.");
        add.accept("tooltip.supernaturalcraft.the_colt.controls", "Use to fire. Sneak-use or [R] to reload, [I] to inspect.");
        add.accept("tooltip.supernaturalcraft.colt_bullet", "Consecrated. Forged by night in a blood circle, eight at a time.");
        add.accept("key.supernaturalcraft.reload_weapon", "Reload Weapon");
        add.accept("key.supernaturalcraft.inspect_weapon", "Inspect Weapon");
        add.accept("tooltip.supernaturalcraft.the_colt.rounds", "Rounds: %s / %s");
        add.accept("tooltip.supernaturalcraft.endless_colt", "Never runs dry. Creative only.");
        add.accept("tooltip.supernaturalcraft.lucifers_grace", "Use to take it in: +50 maximum mana, and tier-three sigils become legible.");
        add.accept("message.supernaturalcraft.colt.empty", "*click*");
        add.accept("message.supernaturalcraft.colt.no_bullets", "You have no consecrated rounds.");
        add.accept("message.supernaturalcraft.grace.already", "You already carry a shard of grace.");
        add.accept("message.supernaturalcraft.grace.absorbed", "Light pours into you. The air hums. Your mana deepens, and the hardest sigils come clear.");
    }

    private static void adv(BiConsumer<String, String> add, String id, String title, String desc) {
        add.accept("advancement.supernaturalcraft." + id, title);
        add.accept("advancement.supernaturalcraft." + id + ".desc", desc);
    }

    static void advancements(BiConsumer<String, String> add) {
        adv(add, "root", "Saving People, Hunting Things", "Find rock salt. The family business starts here.");
        adv(add, "black_eyes", "Black Eyes", "Kill a black-eyed demon");
        adv(add, "fine_print", "Read the Fine Print", "Bind a grimoire");
        adv(add, "christo", "Christo", "Consecrate holy water at a ritual altar");
        adv(add, "caught_in_the_trap", "It's a Trap", "Paint a devil's trap");
        adv(add, "back_to_hell", "Back to Hell", "Exorcise a trapped demon and keep the ember it leaves");
        adv(add, "the_knife", "The Knife", "Forge Ruby's knife");
        adv(add, "angel_blade", "Heaven's Steel", "Forge an angel blade");
        adv(add, "lock_and_key", "Lock and Key", "Forge the Key to the Cage");
        adv(add, "devil_went_down", "The Devil Went Down", "Defeat Lucifer");
        adv(add, "nothing_it_cant_kill", "Nothing It Can't Kill", "Hold the Colt. Its rounds are forged by night, eight at a time");
        adv(add, "grace", "Touched by Grace", "Claim Lucifer's grace");
        adv(add, "hellforge", "Hellforge", "Raise a Hellforge");
        adv(add, "graven", "Graven", "Carve your first rune");
        adv(add, "mark_of_cain", "The Mark of Cain", "Forge the First Blade. It is yours now, and you are its.");
        adv(add, "whispers", "Whispers", "Bind the Whispering Codex");
        adv(add, "dawn", "Dawn", "Defeat Amara, the Darkness, and bring back the sun");
        adv(add, "penumbra", "Penumbra", "Take up the blade that drinks the light");
        adv(add, "void_rune", "Graven in Nothing", "Carve a rune of the Void");
        adv(add, "hymnal_spire", "Stairway to Heaven", "Climb to a Hymnal Spire, high in the mountains");
        adv(add, "shattered_hymn", "Sheet Music", "Find a Shattered Hymn");
        adv(add, "silence_falls", "Silence Falls", "Defeat the Broken Chorus");
        adv(add, "silence", "Off Key", "Break the Chorus's Hymn by ringing the right bell");
        adv(add, "seraph_wings", "Six Wings", "Wear what is left of a seraph");
        adv(add, "hymn_rune", "Graven in Song", "Carve a Hymn rune");
    }

    static void journal(BiConsumer<String, String> add) {
        add.accept("screen.supernaturalcraft.journal", "Hunter's Journal");
        add.accept("screen.supernaturalcraft.journal.open", "Journal");
    }

    static void arsenal(BiConsumer<String, String> add) {
        add.accept("tooltip.supernaturalcraft.weapon.tier", "Tier %s %s");
        add.accept("tooltip.supernaturalcraft.weapon.kind.melee", "weapon");
        add.accept("tooltip.supernaturalcraft.weapon.kind.catalyst", "catalyst");
        add.accept("tooltip.supernaturalcraft.weapon.runes", "Runes %s/%s:");
        add.accept("tooltip.supernaturalcraft.weapon.silver_machete", "Strikes from behind are double criticals. Kills from behind take the head.");
        add.accept("tooltip.supernaturalcraft.weapon.exorcists_mace", "May stun. Brought down from a height, it sends out a ring of holy force.");
        add.accept("tooltip.supernaturalcraft.weapon.angel_blade", "Hold use, then release: dash through your enemies in a line of light.");
        add.accept("tooltip.supernaturalcraft.weapon.rubys_knife", "Demons slain with it always bleed.");
        add.accept("tooltip.supernaturalcraft.weapon.archangel_blade", "Use: smite everything before you.");
        add.accept("tooltip.supernaturalcraft.weapon.ember_staff",
                "Opposite a grimoire: hellfire +20%, bolts burst where they land. Alone: hurls fireballs (6 mana).");
        add.accept("tooltip.supernaturalcraft.weapon.enochian_orb",
                "Opposite a grimoire: spells cost 15% less, recover 20% faster, bolts split in three. Alone: a seeking glyph (10 mana).");
        add.accept("tooltip.supernaturalcraft.weapon.soul_scythe",
                "Swings reap a 140° arc and drink 15% of the life they take. Hold use, release: a crescent of souls (15 mana).");
        add.accept("tooltip.supernaturalcraft.weapon.hellfire_greatsword",
                "Slow and heavy. Hold use, release: cleave a 120° cone and leave a trail of hellfire.");
        add.accept("tooltip.supernaturalcraft.weapon.censer_of_grace",
                "Opposite a grimoire: holy spells +25%, Burst lingers, Ward 1.5x. Alone: hold use for a beam of light (1 mana/tick) that lights the ground.");
        add.accept("tooltip.supernaturalcraft.weapon.first_blade",
                "Cursed. Bound to its forger. Kills feed it and make it stronger; left hungry, it feeds on you. L3: use to lunge. L5: well fed, it will not let you die.");
        add.accept("tooltip.supernaturalcraft.weapon.whispering_codex",
                "Cursed catalyst. Spells cost blood and sanity instead of mana, chain and pierce. Alone: speak the Forbidden Word to mark a foe (+20% harm taken).");
        add.accept("tooltip.supernaturalcraft.curse.state", "Level %s · %s souls · satiation %s");
        add.accept("tooltip.supernaturalcraft.curse.bound", "Bound to its forger");
        add.accept("message.supernaturalcraft.curse.rejects", "It does not know you. It burns.");
        add.accept("message.supernaturalcraft.curse.level", "%s grows stronger: level %s.");
        add.accept("message.supernaturalcraft.curse.mark", "The Mark will not let you die.");
        add.accept("message.supernaturalcraft.codex.misfire", "The words twist in your mouth.");
        add.accept("message.supernaturalcraft.whisper.0", "It is not enough.");
        add.accept("message.supernaturalcraft.whisper.1", "Feed me.");
        add.accept("message.supernaturalcraft.whisper.2", "They are all so soft.");
        add.accept("message.supernaturalcraft.whisper.3", "You were made for this.");
        add.accept("message.supernaturalcraft.whisper.4", "Cain did not stop either.");
        add.accept("message.supernaturalcraft.whisper.5", "Just one more.");
        add.accept("message.supernaturalcraft.whisper.6", "Can you hear them? They hear you.");
        add.accept("message.supernaturalcraft.whisper.7", "The mark itches. Scratch it.");
        add.accept("container.supernaturalcraft.hellforge", "Hellforge");
        add.accept("screen.supernaturalcraft.hellforge.inscribe", "Grave");
        add.accept("screen.supernaturalcraft.hellforge.purge", "Purge");
        add.accept("screen.supernaturalcraft.hellforge.cost", "Cost: %s levels");
        add.accept("screen.supernaturalcraft.hellforge.no_weapon", "Place a weapon");
        add.accept("screen.supernaturalcraft.hellforge.no_runes", "Add runes to grave");
        add.accept("screen.supernaturalcraft.hellforge.full", "No rune slots left");
        add.accept("screen.supernaturalcraft.hellforge.wrong_kind", "That rune won't take");
        add.accept("screen.supernaturalcraft.hellforge.too_many", "At most two alike");
        add.accept("screen.supernaturalcraft.hellforge.tier", "Needs a tier IV weapon");
        add.accept("screen.supernaturalcraft.hellforge.no_xp", "Not enough experience");
        add.accept("tooltip.supernaturalcraft.rune.blank", "Carve it into a rune at a ritual altar.");
        add.accept("tooltip.supernaturalcraft.rune.fit.melee", "Graves into weapons");
        add.accept("tooltip.supernaturalcraft.rune.fit.catalyst", "Graves into catalysts");
        add.accept("tooltip.supernaturalcraft.rune.fit.any", "Graves into anything");
        add.accept("tooltip.supernaturalcraft.rune.edge", "+1.5 damage");
        add.accept("tooltip.supernaturalcraft.rune.ember", "Sets struck foes alight");
        add.accept("tooltip.supernaturalcraft.rune.frost", "Slows struck foes");
        add.accept("tooltip.supernaturalcraft.rune.leech", "Heals you for 8% of the damage dealt");
        add.accept("tooltip.supernaturalcraft.rune.sanctity", "Your blows count as holy");
        add.accept("tooltip.supernaturalcraft.rune.swiftness", "+8% attack speed");
        add.accept("tooltip.supernaturalcraft.rune.resonance", "Spells cost 10% less and hit 10% harder");
        add.accept("tooltip.supernaturalcraft.rune.focus", "Spells recover 15% faster");
        add.accept("tooltip.supernaturalcraft.rune.echo", "15% chance a spell echoes");
        add.accept("tooltip.supernaturalcraft.rune.void", "Drinks the light: see the Darkness");
        add.accept("tooltip.supernaturalcraft.rune.hymn", "Every fourth blow or spell rings out again as holy light");
        add.accept("tooltip.supernaturalcraft.choir_shard", "A piece of a wheel that once turned before the Throne. An eye still watches from it.");
        add.accept("tooltip.supernaturalcraft.seraph_wings", "Worn on the back (Curios). Only for show — and what a show.");
        add.accept("jei.supernaturalcraft.info.colt", "One round of the Colt kills any living thing outright, except players, the great enemies and archangels and those above them. A great enemy takes "
                + "a twentieth of its full strength that nothing blunts (the Author far less; it still stops at each phase); an archangel, a heavy blow. Five chambers, loaded one round at a time: sneak-use or [R]. Its rounds cannot be crafted: forge them by night in a blood circle, eight at a time, once you have held the Colt; a few lie in the Hymnal Spire's temple and with the fallen bosses.");
        add.accept("jei.supernaturalcraft.info.chorus", "Found on the summit of a Hymnal Spire. Lay a Shattered Hymn on the Choir Altar in a thunderstorm, then ring the hymn's three notes on the bells: the temple window on the stair shows them, left to right. Creative players can read an altar's hymn with an empty hand, or sneak and ring three bells to set it.");
        add.accept("message.supernaturalcraft.ritual.not_ready", "You aren't ready for this rite yet.");
        add.accept("message.supernaturalcraft.ritual.needs_eclipse", "This rite needs the sun eclipsed.");
        add.accept("key.supernaturalcraft.skip_cinematic", "Skip cinematic (hold)");
        add.accept("cinematic.supernaturalcraft.skip", "Hold %s to skip");
        add.accept("entity.supernaturalcraft.amara.phase1", "Amara, the Darkness");
        add.accept("entity.supernaturalcraft.amara.phase2", "Amara, the Hunger Below");
        add.accept("entity.supernaturalcraft.amara.phase3", "Amara, the Black Sun");
        add.accept("entity.supernaturalcraft.amara.phase4", "Amara, Unmade");
        add.accept("message.supernaturalcraft.amara.exposed", "Her core lies open. Strike it!");
        add.accept("tooltip.supernaturalcraft.penumbra.light", "Light drunk: %s/%s");
        add.accept("tooltip.supernaturalcraft.weapon.penumbra",
                "Drinks light from bright places and lit foes. Hold use, release: an Umbra Wave that blinds and wounds (light / 5). +30% damage in the dark.");
        add.accept("tooltip.supernaturalcraft.eclipse_sight", "Drink to be marked: darkness and blindness cannot take you, the eclipse cannot hide the world, +25 mana.");
        add.accept("message.supernaturalcraft.eclipse_sight.marked", "The dark looks back at you, and you see through it. You are marked.");
        add.accept("message.supernaturalcraft.eclipse_sight.already", "You already carry her mark.");
        add.accept("jei.supernaturalcraft.info.hellforge", "Place a weapon and runes, then Grave (tier x 2 levels per rune). Purge returns the runes but one, lost at random (1 level). "
                + "With an Ascension Shard of the next tier in the leftmost slot, Ascend raises the weapon (or Hunter's Gear, or the General's armour) one tier.");
        add.accept("jei.supernaturalcraft.info.runes", "Carved from a blank rune at a small circle. Each fits weapons, catalysts or both; at most two alike on one weapon. The Void rune takes a tier IV weapon.");
        add.accept("jei.supernaturalcraft.info.void_essence", "Left behind by the Darkness. Carve it into a Void rune.");
        add.accept("message.supernaturalcraft.amara.totality", "Totality comes. Stand by a burning well!");
        add.accept("message.supernaturalcraft.amara.unmaking", "The ground is unmade. Stand in the light!");
        add.accept("message.supernaturalcraft.amara.arena_taken", "Another fight already holds this world.");
        add.accept("message.supernaturalcraft.amara.arena_gone", "The Darkness has nothing left to hold her here.");
        add.accept("message.supernaturalcraft.amara.victorious", "The Darkness withdraws, sated. For now.");
        add.accept("cinematic.supernaturalcraft.amara.title", "AMARA");
        add.accept("cinematic.supernaturalcraft.amara.subtitle", "Before the light, there was only me.");
        add.accept("cinematic.supernaturalcraft.amara.phase2.title", "THE HUNGER BELOW");
        add.accept("cinematic.supernaturalcraft.amara.phase2.subtitle", "Break the cysts at her roots.");
        add.accept("cinematic.supernaturalcraft.amara.phase3.title", "THE BLACK SUN");
        add.accept("cinematic.supernaturalcraft.amara.phase3.subtitle", "Keep the wells burning.");
        add.accept("cinematic.supernaturalcraft.amara.phase4.title", "UNMADE");
        add.accept("cinematic.supernaturalcraft.amara.phase4.subtitle", "Stand in the light.");
        add.accept("cinematic.supernaturalcraft.amara.victory.title", "DAWN");
        add.accept("cinematic.supernaturalcraft.amara.victory.subtitle", "The sun returns. The Darkness sleeps.");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.summon_amara", "Calls the Darkness down. Only under an eclipse.");
        add.accept("message.supernaturalcraft.eclipse.begins", "Something passes before the sun. The Darkness is watching.");
        add.accept("message.supernaturalcraft.eclipse.ends", "The sun returns.");
        add.accept("message.supernaturalcraft.eclipse.already", "The sun is already eclipsed.");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.begin_eclipse", "Eclipses the sun over the whole dimension for ten minutes. Demons walk by day, the dead do not burn, holy strikes bite deeper.");
        for (org.papiricoh.supernaturalcraft.weapon.Rune r : org.papiricoh.supernaturalcraft.weapon.Rune.values()) {
            String n = r.getSerializedName();
            add.accept("rune.supernaturalcraft." + n, n.substring(0, 1).toUpperCase() + n.substring(1));
        }
    }

    private static void sigil(BiConsumer<String, String> add, String id, String name, String desc) {
        add.accept("sigil.supernaturalcraft." + id, name);
        add.accept("sigil.supernaturalcraft." + id + ".desc", desc);
    }

    private static void sigils(BiConsumer<String, String> add) {
        sigil(add, "touch", "Touch", "Whatever is within arm's reach. Helpful spells fall back on yourself.");
        sigil(add, "bolt", "Bolt", "A thrown sigil that flies straight and breaks on the first thing it meets.");
        sigil(add, "burst", "Burst", "A pulse outward from you.");
        sigil(add, "ward", "Ward", "A circle at your feet that keeps working and stops hostile projectiles.");
        sigil(add, "smite", "Smite", "Holy force. Twice as hard against demons and the dead.");
        sigil(add, "hellfire", "Hellfire", "Burns flesh. Burns a pinch of sulfur.");
        sigil(add, "frost", "Frost", "Slows and freezes. Turns still water to ice.");
        sigil(add, "exorcise", "Exorcise", "Agony to demons. A trapped demon is cast out entirely. Burns salt.");
        sigil(add, "bind", "Bind", "Holds a demon as a devil's trap would; hobbles anything else. Burns demon blood.");
        sigil(add, "mend", "Mend", "Closes wounds and smothers flames.");
        sigil(add, "repel", "Repel", "Hurls the target away from you.");
        sigil(add, "reveal", "Reveal", "Strips invisibility, outlines the target and unmasks illusions.");
        sigil(add, "empower", "Empower", "Stronger effects.");
        sigil(add, "extend", "Extend", "Longer effects and greater reach.");
        sigil(add, "widen", "Widen", "Spreads the effect over an area.");
        sigil(add, "echo", "Echo", "The spell repeats itself a moment later.");
    }
}
