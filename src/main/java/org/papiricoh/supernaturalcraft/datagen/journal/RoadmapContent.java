package org.papiricoh.supernaturalcraft.datagen.journal;

import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.journal.Unlock;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

import static org.papiricoh.supernaturalcraft.datagen.journal.SNRoadmap.node;

/**
 * The roadmap's nodes (owned by the roadmap work package). Columns are how deep a step sits on the
 * road (left to right), rows are branches: the hymn and the holy water above, the main road through
 * row 2, Lilith, the side jobs and Lucifer's spoils below. The list order is the order the dashboard
 * walks the main road to find the next objective, so main nodes follow the bosses as
 * {@code BossProgression} meets them.
 * <p>
 * A step done by holding an item also counts as done once a later step that needs it is done (the
 * item was spent), so the road never shows a gap behind the hunter.
 */
final class RoadmapContent {

    private RoadmapContent() {
    }

    static void addAll(List<SNRoadmap.Node> out) {
        // --- the first hunt -----------------------------------------------------------------------
        out.add(node("salt", 0, 2).icon(AllItems.SALT.get()).main().advancement("main/root").entry("salt")
                .name("Salt")
                .hint("Every hunt starts with salt. Dig rock salt out of the stone and keep some on you."));
        out.add(node("black_eyes", 1, 2).after("salt").icon(AllItems.DEMON_BLOOD.get()).main().advancement("main/black_eyes")
                .entry("black_eyes")
                .name("Black Eyes")
                .hint("Demons walk the night with black eyes. Kill one and take its blood."));
        out.add(node("devils_trap", 2, 2).after("black_eyes").icon(AllItems.DEVILS_TRAP.get()).main()
                .advancement("main/caught_in_the_trap").entry("devils_trap")
                .name("It's a Trap")
                .hint("Bind demon blood, salt, paper and red dye in a binding circle to paint a devil's trap."));
        out.add(node("rituals", 1, 0).after("salt").icon(AllItems.RITUAL_ALTAR.get()).main()
                .done(Unlock.any(item("ritual_altar"), adv("main/christo"))).entry("rituals")
                .name("The Ritual Altar")
                .hint("Raise a ritual altar and chalk a circle around it. Every rite starts there."));
        out.add(node("holy_water", 2, 0).after("rituals").icon(AllItems.HOLY_WATER.get()).main().advancement("main/christo")
                .entry("holy_water")
                .name("Christo")
                .hint("Lay water bottles and salt in a small circle around the altar and light it to consecrate holy water."));

        // --- the Yellow-Eyed Demon and the Key ------------------------------------------------------
        out.add(node("azazel", 3, 2).after("devils_trap").icon(AllItems.AZAZEL_TROPHY.get()).boss().main()
                .advancement("main/yellow_eyed").entry("azazel")
                .name("Azazel")
                .hint("Draw a great circle in blood chalk with lit black candles, offer demon blood, sulfur and salt by night, and call the Yellow-Eyed Demon."));
        out.add(node("hellfire_ember", 3, 1).after("devils_trap", "holy_water").icon(AllItems.HELLFIRE_EMBER.get()).main()
                .done(Unlock.any(adv("main/back_to_hell"), adv("main/lock_and_key"))).entry("devils_trap")
                .name("Hellfire Ember")
                .hint("Hold a demon in a devil's trap and exorcise it with salt and holy water in a blood circle. Keep the ember it leaves."));
        out.add(node("azazel_blood", 4, 2).after("azazel").icon(AllItems.AZAZEL_BLOOD.get()).main()
                .done(Unlock.any(item("azazel_blood"), adv("main/lock_and_key"))).entry("azazel")
                .name("Azazel's Blood")
                .hint("The Yellow-Eyed Demon bleeds when he falls. Gather what he leaves behind."));
        out.add(node("key_to_the_cage", 5, 2).after("azazel_blood", "hellfire_ember").icon(AllItems.KEY_TO_THE_CAGE.get()).main()
                .advancement("main/lock_and_key").entry("key_to_the_cage")
                .name("The Key to the Cage")
                .hint("By night, forge Azazel's blood, hellfire embers, gold and a nether star in a great circle."));
        out.add(node("lilith", 4, 3).after("azazel").icon(AllItems.LILITH_TROPHY.get()).boss().main()
                .advancement("main/lucifer_rising").entry("lilith")
                .name("Lilith")
                .hint("With Azazel dead, call the first demon by night: demon blood, hellfire embers, bones and paper in a great circle."));
        out.add(node("last_seal", 5, 3).after("lilith").icon(AllItems.LAST_SEAL.get()).main()
                .done(Unlock.any(item("last_seal"), adv("main/devil_went_down"))).entry("last_seal")
                .name("The Last Seal")
                .hint("Lilith is the last seal. Take it from her when she breaks."));

        // --- the Devil ------------------------------------------------------------------------------
        out.add(node("lucifer", 6, 2).after("key_to_the_cage", "last_seal").icon(AllItems.MORNINGSTAR_TROPHY.get()).boss().main()
                .advancement("main/devil_went_down").entry("lucifer")
                .name("Lucifer")
                .hint("Lay the Last Seal, demon blood, embers and holy water in a great circle by night and turn the Key to the Cage."));
        out.add(node("the_colt", 7, 4).after("lucifer").icon(AllItems.THE_COLT.get())
                .advancement("main/nothing_it_cant_kill").entry("the_colt")
                .name("The Colt")
                .hint("Lucifer falls with the gun that can kill anything. Pick it up; its rounds are forged by night."));
        out.add(node("grace", 7, 5).after("lucifer").icon(AllItems.LUCIFERS_GRACE.get())
                .advancement("main/grace").entry("lucifers_grace")
                .name("Lucifer's Grace")
                .hint("An archangel's grace spills when Lucifer dies. Claim it."));

        // --- the Hymnal Spire and the Broken Chorus --------------------------------------------------
        out.add(node("hymnal_spire", 3, 0).after("holy_water").icon(AllItems.CHOIR_ALTAR.get()).main()
                .advancement("main/hymnal_spire").entry("hymnal_spire")
                .name("The Hymnal Spire")
                .hint("Turn a map into a hymnal map in a small circle and follow it to a spire high in the mountains."));
        out.add(node("shattered_hymn", 4, 0).after("hymnal_spire").icon(AllItems.SHATTERED_HYMN.get()).main()
                .advancement("main/shattered_hymn").entry("broken_chorus")
                .name("The Shattered Hymn")
                .hint("Search the spire for a Shattered Hymn, or ink one in a great circle with a feather."));
        out.add(node("broken_chorus", 5, 0).after("shattered_hymn").icon(AllItems.CHOIR_TROPHY.get()).boss().main()
                .advancement("main/silence_falls").entry("broken_chorus")
                .name("The Broken Chorus")
                .hint("Arm the spire's choir altar with the Shattered Hymn and ring its bells. Listen for the wrong note."));

        // --- the Scribe of God ------------------------------------------------------------------------
        out.add(node("metatron", 7, 3).after("lucifer").icon(AllItems.METATRON_TROPHY.get()).boss().main()
                .advancement("main/scribe_of_god").entry("metatron")
                .name("Metatron")
                .hint("Write his name in a book, lay it in a great circle with choir shards, holy water and feathers by night, and he will answer."));

        // --- the Darkness ---------------------------------------------------------------------------
        out.add(node("eclipse", 7, 1).after("lucifer").icon("minecraft:black_candle").main()
                .done(Unlock.any(Unlock.entity(SupernaturalCraft.asResource("amara")), adv("main/dawn"))).entry("eclipse")
                .name("The Eclipse")
                .hint("Darken the sun: crying obsidian, black dye, an ender eye and demon blood in a great circle, by day."));
        out.add(node("amara", 8, 1).after("eclipse").icon(AllItems.ECLIPSE_TROPHY.get()).boss().main()
                .advancement("main/dawn").entry("amara")
                .name("Amara")
                .hint("Under the eclipse, draw a void circle and wake the Darkness with a nether star."));

        // --- the Cage ---------------------------------------------------------------------------------
        out.add(node("hell", 7, 2).after("lucifer").icon(AllItems.HELLSTONE.get()).main()
                .advancement("main/highway_to_hell").entry("hell")
                .name("Highway to Hell")
                .hint("Reforge the cracked Key, then open a rift into Hell: a void circle by night, or a great circle in the Nether."));
        out.add(node("four_horsemen", 9, 2).after("hell", "amara", "broken_chorus").icon(AllItems.RING_OF_DEATH.get()).main()
                .advancement("main/four_horsemen").entry("four_horsemen")
                .name("The Four Horsemen")
                .hint("In Hell, forge the rings of War, Famine, Pestilence and Death from what the Devil, the Darkness and the Chorus left behind."));
        out.add(node("lucifer_uncaged", 10, 2).after("four_horsemen").icon(AllItems.FALLEN_STAR.get()).boss().main()
                .advancement("main/back_in_the_box").entry("lucifer_uncaged")
                .name("Lucifer Uncaged")
                .hint("Bring the four rings and the Key to the dais of the Cage in Hell. Put him back in the box."));

        // --- side jobs --------------------------------------------------------------------------------
        out.add(node("grimoire", 1, 4).after("salt").icon(AllItems.GRIMOIRE.get())
                .advancement("main/fine_print").entry("grimoire")
                .name("The Grimoire")
                .hint("Bind a grimoire and fill its pages with the sigils you find."));
        out.add(node("spell_bowl", 2, 4).after("grimoire").icon(AllItems.SPELL_BOWL.get())
                .advancement("main/first_spell").entry("spell_bowl")
                .name("The Spell Bowl")
                .hint("Mix a spell in a spell bowl and recite the words over it."));
        out.add(node("salt_and_burn", 3, 4).after("spell_bowl").icon(AllItems.ECTOPLASM.get())
                .advancement("main/salt_and_burn").entry("ghosts")
                .name("Salt and Burn")
                .hint("Find where a ghost's bones lie, salt them and burn them."));
        out.add(node("crossroads", 3, 5).after("spell_bowl").icon(AllItems.CROSSROADS_CONTRACT.get())
                .advancement("main/deal_with_the_devil").entry("crossroads")
                .name("Crossroads Deal")
                .hint("Learn the crossroads summoning from a spell page, mix it in your bowl by night and hear what the demon offers. Mind the fine print."));
    }

    private static Unlock adv(String path) {
        return Unlock.advancement(SupernaturalCraft.asResource(path));
    }

    private static Unlock item(String path) {
        return Unlock.item(SupernaturalCraft.asResource(path));
    }
}
