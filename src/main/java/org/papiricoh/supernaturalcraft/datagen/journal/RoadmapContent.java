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

    static void addAll(List<SNRoadmap.Road> roads) {
        roads.add(cage(SNRoadmap.road("road_to_the_cage", AllItems.KEY_TO_THE_CAGE.get(), "The Road to the Cage")));
        roads.add(bowl(SNRoadmap.road("the_spell_bowl", AllItems.SPELL_BOWL.get(), "The Spell Bowl")));
        roads.add(crossroads(SNRoadmap.road("the_crossroads", AllItems.CROSSROADS_CONTRACT.get(), "The Crossroads")));
        roads.add(allegiance(SNRoadmap.road("heaven_hell_free_will", AllItems.ANGEL_BLADE.get(), "Heaven, Hell and Free Will")));
    }

    private static SNRoadmap.Road cage(SNRoadmap.Road road) {
        List<SNRoadmap.Node> out = road.nodes;
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
        out.add(node("the_colt", 8, 4).after("lucifer").icon(AllItems.THE_COLT.get())
                .advancement("main/nothing_it_cant_kill").entry("the_colt")
                .name("The Colt")
                .hint("Lucifer falls with the gun that can kill anything. Pick it up; its rounds are forged by night."));
        out.add(node("grace", 8, 5).after("lucifer").icon(AllItems.LUCIFERS_GRACE.get())
                .advancement("main/grace").entry("lucifers_grace")
                .name("Lucifer's Grace")
                .hint("An archangel's grace spills when Lucifer dies. Claim it."));
        // --- the Trickster (v0.14): a side road, asked for by nothing ------------------------------------------------
        out.add(node("gabriel", 7, 3).after("lucifer").icon(AllItems.TRICKSTER_REMOTE.get()).boss()
                .advancement("main/changing_channels").entry("gabriel")
                .name("The Trickster")
                .hint("After Lucifer, somebody starts playing harmless pranks. Notice three, cook his bait in a small circle, then lay holy oil, "
                        + "cake and sugar by night and wake it with the bait. Optional: nothing else needs him."));

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
        out.add(node("metatron", 6, 0).after("broken_chorus").icon(AllItems.METATRON_TROPHY.get()).boss().main()
                .advancement("main/scribe_of_god").entry("metatron")
                .name("Metatron")
                .hint("With the Chorus silenced, write his name in a book, lay it in a great circle with choir shards, holy water and feathers by night, and he will answer."));
        // --- the Sword of Heaven (v0.12): the end of Heaven's road, beside Lucifer Uncaged -----------------
        out.add(node("michael", 11, 0).after("metatron").icon(AllItems.MICHAEL_LANCE.get()).boss().main()
                .advancement("main/sword_of_heaven").entry("michael")
                .name("Michael")
                .hint("By day, offer the Angel Tablet and the Seraph Wings in a great circle and wake it with an angel blade. Never give him your yes lightly."));
        out.add(node("wings_of_heaven", 12, 0).after("michael").icon(AllItems.MICHAELS_GRACE.get())
                .advancement("main/wings_of_heaven").entry("michaels_grace")
                .name("Wings of Heaven")
                .hint("Wear the Seraph Wings and take in Michael's Grace: they will carry you."));
        out.add(node("general_armor", 13, 0).after("michael").icon(AllItems.GENERAL_HELMET.get())
                .advancement("main/general").entry("general_armor")
                .name("General of Heaven")
                .hint("He leaves one piece of his armour each time he falls. Beat him four times to wear it whole."));

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
        // --- the Four Horsemen (v0.11): War, Famine and Pestilence after Lucifer, then Death -------
        out.add(node("war", 7, 4).after("lucifer").icon(AllItems.WAR_TROPHY.get()).boss().main()
                .advancement("main/war").entry("war")
                .name("War")
                .hint("Swords, gunpowder and demon blood in a great circle, lit with flint and steel. Parry, break his standards, trust no demon."));
        out.add(node("famine", 7, 5).after("lucifer").icon(AllItems.FAMINE_TROPHY.get()).boss().main()
                .advancement("main/famine").entry("famine")
                .name("Famine")
                .hint("Rotten flesh, bread, wheat and salt in a great circle. Don't eat near him; kill his thralls before he does."));
        out.add(node("pestilence", 7, 6).after("lucifer").icon(AllItems.PESTILENCE_TROPHY.get()).boss().main()
                .advancement("main/pestilence").entry("pestilence")
                .name("Pestilence")
                .hint("A fermented spider eye, mushrooms and clotted blood in a great circle. Drink his antidote, burn his flies."));
        out.add(node("death", 9, 3).after("war", "famine", "pestilence").icon(AllItems.DEATH_TROPHY.get()).boss().main()
                .advancement("main/pale_rider").entry("death")
                .name("Death")
                .hint("Offer him the three rings in a great circle and wake it with the Soul Scythe. Keep hitting him: your clock is running."));
        out.add(node("four_horsemen", 10, 2).after("death", "amara", "broken_chorus").icon(AllItems.RING_OF_DEATH.get()).main()
                .advancement("main/four_horsemen").entry("four_horsemen")
                .name("The Four Horsemen")
                .hint("Hold all four rings, each won from its Horseman."));
        out.add(node("lucifer_uncaged", 11, 2).after("four_horsemen").icon(AllItems.FALLEN_STAR.get()).boss().main()
                .advancement("main/back_in_the_box").entry("lucifer_uncaged")
                .name("Lucifer Uncaged")
                .hint("Bring the four rings and the Key to the dais of the Cage in Hell. Put him back in the box."));
        out.add(node("find_the_author", 12, 2).after("lucifer_uncaged", "michael").icon(AllItems.FALLEN_STAR.get()).main()
                .rite("find_the_author").entry("find_the_author")
                .name("Find the Author")
                .hint("With every great enemy beaten, a page falls at your feet. Cast its spell in the bowl: it draws a map to a cabin very far away."));
        out.add(node("the_author", 13, 2).after("find_the_author").icon(AllItems.TYPEWRITER.get()).main()
                .advancement("main/the_author").entry("author_cabin")
                .name("The Man in the Cabin")
                .hint("Follow the map to the cabin at the end of the world and talk to the man at the typewriter."));
        out.add(node("chuck", 14, 2).after("the_author").icon(AllItems.THE_END_MANUSCRIPT.get()).boss().main()
                .advancement("main/the_end").entry("chuck")
                .name("The Author")
                .hint("When you are ready, tell him. He will test you, chapter by chapter, and write your ending."));

        // --- side jobs --------------------------------------------------------------------------------
        out.add(node("grimoire", 1, 4).after("salt").icon(AllItems.GRIMOIRE.get())
                .advancement("main/fine_print").entry("grimoire")
                .name("The Grimoire")
                .hint("Bind a grimoire and fill its pages with the sigils you find."));
        out.add(node("salt_and_burn", 2, 5).after("grimoire").icon(AllItems.ECTOPLASM.get())
                .advancement("main/salt_and_burn").entry("ghosts")
                .name("Salt and Burn")
                .hint("Find where a ghost's bones lie, salt them and burn them."));
        return road;
    }

    /** The spell bowl: the bowl, the words, and every spell it can hold. */
    private static SNRoadmap.Road bowl(SNRoadmap.Road road) {
        List<SNRoadmap.Node> out = road.nodes;
        out.add(node("bowl_bowl", 0, 3).icon(AllItems.SPELL_BOWL.get()).main()
                .done(Unlock.any(item("spell_bowl"), adv("main/first_spell"))).entry("spell_bowl")
                .name("A Bronze Bowl")
                .hint("Hammer copper around a little gold into a spell bowl, and set it down where you mean to work."));
        out.add(node("bowl_page", 1, 2).after("bowl_bowl").icon(AllItems.SPELL_PAGE.get()).main()
                .done(Unlock.any(item("spell_page"), adv("main/first_spell"))).entry("spell_pages")
                .name("Spell Pages")
                .hint("The words are written on spell pages: demons carry them, old chests hide them, librarians sell them and graves give them up."));
        out.add(node("bowl_liquids", 1, 4).after("bowl_bowl").icon(AllItems.HOLY_WATER.get()).main()
                .done(Unlock.any(item("holy_water"), item("demon_blood"), item("blood_vial"), adv("main/first_spell"))).entry("bowl_liquids")
                .name("Something to Pour")
                .hint("Every spell wants its liquids: water, holy water, demon blood, a vial of someone's blood, honey."));
        out.add(node("bowl_first_spell", 2, 3).after("bowl_page", "bowl_liquids").icon(AllItems.SPELL_BOWL.get()).boss().main()
                .advancement("main/first_spell").entry("spell_bowl")
                .name("Words of Power")
                .hint("Fill the bowl with what a page asks for, light it, and recite the words without stumbling."));

        out.add(node("bowl_second_sight", 3, 0).after("bowl_first_spell").icon("minecraft:spider_eye").main()
                .rite("second_sight").entry("concealment")
                .name("Second Sight")
                .hint("Learn to see what hides: ghosts, hellhounds, hex bags tucked away."));
        out.add(node("bowl_concealment", 3, 1).after("bowl_first_spell").icon("minecraft:phantom_membrane").main()
                .rite("concealment").entry("concealment")
                .name("Concealment")
                .hint("Learn to hide from demons, angels, hounds and spirits. Never from the great ones."));
        out.add(node("bowl_locate", 3, 2).after("bowl_first_spell").icon("minecraft:compass").main()
                .rite("locate").entry("locating")
                .name("The Locating Spell")
                .hint("Learn to send smoke after what you seek: a person, a pet, a grave, a spire."));
        out.add(node("bowl_hex_bags", 3, 3).after("bowl_first_spell").icon(AllItems.CURSE_BAG.get()).main()
                .rite("hex_bags").entry("hex_bags")
                .name("Hex Bags")
                .hint("Learn to bind a curse into a pouch, or a ward against demons."));
        out.add(node("bowl_purification", 3, 4).after("bowl_first_spell").icon("minecraft:lily_of_the_valley").main()
                .rite("purification").entry("cleansing")
                .name("Purification")
                .hint("Learn to cleanse curses, possession and restless spirits, and to break a crossroads deal."));
        out.add(node("bowl_bind_banish", 3, 5).after("bowl_first_spell").icon("minecraft:chain").main()
                .rite("bind_banish").entry("cleansing")
                .name("Binding and Banishing")
                .hint("Learn to hold a creature to the bowl, or to send demons, hounds and ghosts back where they came from."));
        out.add(node("bowl_summon_crossroads", 3, 6).after("bowl_first_spell").icon(AllItems.CROSSROADS_CONTRACT.get()).main()
                .rite("summon_crossroads").entry("crossroads")
                .name("The Crossroads Summoning")
                .hint("Learn the words that bring a crossroads demon to you. Think twice before you say them."));

        out.add(node("bowl_salt_and_burn", 4, 0).after("bowl_second_sight").icon(AllItems.ECTOPLASM.get())
                .advancement("main/salt_and_burn").entry("salt_and_burn")
                .name("Salt and Burn")
                .hint("Now you can see the dead: find a restless one's bones, salt them and burn them."));
        out.add(node("bowl_pet_collar", 4, 1).after("bowl_locate").icon(AllItems.PET_COLLAR.get())
                .done(item("pet_collar")).entry("pet_collars")
                .name("A Pet's Collar")
                .hint("Stitch a collar from leather, string and an iron nugget and put it on a pet you have tamed."));
        out.add(node("bowl_blood_vial", 4, 2).after("bowl_locate").icon(AllItems.BLOOD_VIAL.get())
                .done(item("blood_vial")).entry("blood_vials")
                .name("A Vial of Blood")
                .hint("Use an empty bottle on someone to draw their blood, or on the air to draw your own. The smoke follows blood."));
        out.add(node("bowl_bags", 4, 3).after("bowl_hex_bags").icon(AllItems.PROTECTION_BAG.get())
                .done(Unlock.any(item("curse_bag"), item("protection_bag"))).entry("hex_bags")
                .name("A Bag in Hand")
                .hint("Mix a hex bag: hide a curse near your enemy, or carry a ward to keep demons off you."));
        out.add(node("bowl_revive_pet", 5, 1).after("bowl_pet_collar").icon("minecraft:glistering_melon_slice")
                .rite("revive_pet").entry("pet_collars")
                .name("Revive a Pet")
                .hint("Learn to call a fallen pet back by its collar."));
        return road;
    }

    /** The crossroads: the summoning, the deal, the debt and every way out of it. */
    private static SNRoadmap.Road crossroads(SNRoadmap.Road road) {
        List<SNRoadmap.Node> out = road.nodes;
        out.add(node("xr_bowl", 0, 2).icon(AllItems.SPELL_BOWL.get()).main()
                .done(Unlock.any(adv("main/first_spell"), adv("main/deal_with_the_devil"))).entry("spell_bowl")
                .name("Words of Power")
                .hint("Every deal starts at a spell bowl. Cast your first spell."));
        out.add(node("xr_summoning", 1, 1).after("xr_bowl").icon(AllItems.SPELL_PAGE.get()).main()
                .done(Unlock.any(Unlock.rite(SupernaturalCraft.asResource("summon_crossroads")), adv("main/deal_with_the_devil")))
                .entry("crossroads")
                .name("The Summoning")
                .hint("Find the spell page that teaches the crossroads summoning."));
        out.add(node("xr_damned_contract", 1, 3).after("xr_bowl").icon(AllItems.DAMNED_CONTRACT.get()).main()
                .done(Unlock.any(item("damned_contract"), adv("main/deal_with_the_devil"))).entry("crossroads")
                .name("A Damned Contract")
                .hint("The demon wants paper it can sign: occultists carry damned contracts, Lilith keeps them, Crowley's cells hide them."));
        out.add(node("xr_deal", 2, 2).after("xr_summoning", "xr_damned_contract").icon(AllItems.CROSSROADS_CONTRACT.get()).boss().main()
                .advancement("main/deal_with_the_devil").entry("the_deal")
                .name("Sealed with a Kiss")
                .hint("By night, fill the bowl with demon blood, a bone, grave dirt, a daisy and the contract, say the words, and choose a wish."));
        out.add(node("xr_hounds", 3, 1).after("xr_deal").icon(AllItems.HOUND_WHISTLE.get())
                .done(Unlock.any(Unlock.entity(SupernaturalCraft.asResource("hellhound")), adv("main/debt_paid"))).entry("hellhounds")
                .name("When the Hounds Come")
                .hint("When the debt falls due, the hounds come for you. You will not see them coming."));
        out.add(node("xr_break", 3, 3).after("xr_deal").icon(AllItems.HELLFIRE_EMBER.get())
                .done(Unlock.any(Unlock.rite(SupernaturalCraft.asResource("purification")), adv("main/debt_paid"))).entry("the_debt")
                .name("Breaking the Deal")
                .hint("Purification can break a deal: holy water, the contract, salt and a hellfire ember. The demon will come back angry."));
        out.add(node("xr_debt_paid", 4, 2).after("xr_deal").icon(AllItems.CROSSROADS_CONTRACT.get()).boss().main()
                .advancement("main/debt_paid").entry("the_debt")
                .name("Off the Hook")
                .hint("Survive the hunt, or kill the demon before the debt falls due. Die, and they take what they came for."));
        return road;
    }

    /**
     * Heaven, Hell and free will (v0.13): from the crossroads of the soul, three branches. Angel above (Heaven's messenger,
     * then four rites), the hunter's ranks through the middle, Hell below (a soul bound to the crossroads, then the rites).
     * Every rank is an advancement awarded by code; the branches a hunter swore against go grey. Human first, so a hunter
     * free to choose is pointed at the hunter's oath.
     */
    private static SNRoadmap.Road allegiance(SNRoadmap.Road road) {
        List<SNRoadmap.Node> out = road.nodes;
        out.add(node("crossroads_of_the_soul", 0, 1).icon(AllItems.HUNTERS_AMULET.get())
                .done(Unlock.any(adv("main/root"), adv("main/fine_print"))).entry("allegiance")
                .name("The Crossroads of the Soul")
                .hint("Every hunter is human, and free. Heaven may call, Hell may bargain, or you may stay what you are and climb the hunter's ranks."));
        // --- the hunter's ranks: free will ----------------------------------------------------------
        out.add(node("hunter_1", 1, 1).after("crossroads_of_the_soul").icon(AllItems.COLT_BULLET.get()).branch("hunter")
                .advancement("main/hunter_1").entry("hunter_ranks")
                .name("Hunter")
                .hint("Swear the hunter's oath at the altar: a Colt bullet, four salt, two holy water and a silver machete. Only a human sworn to no side may take it."));
        out.add(node("hunter_2", 2, 1).after("hunter_1").icon(AllItems.SILVER_MACHETE.get()).branch("hunter")
                .advancement("main/hunter_2").entry("hunter_ranks")
                .name("Veteran")
                .hint("By night, keep the veteran's vigil with what is left of Lilith and of Metatron: their trophies."));
        out.add(node("hunter_3", 3, 1).after("hunter_2").icon(AllItems.THE_COLT.get()).branch("hunter").boss()
                .advancement("main/hunter_3").entry("hunter_ranks")
                .name("Legend")
                .hint("Become a legend of the road: the four Horsemen's rings and a Fallen Star on the altar."));
        // --- Heaven ---------------------------------------------------------------------------------
        out.add(node("heeded_the_call", 1, 0).after("crossroads_of_the_soul").icon(AllItems.VIAL_OF_GRACE.get()).branch("angel")
                .advancement("main/heeded_the_call").entry("the_messenger")
                .name("Heeding the Call")
                .hint("After your first victory over Azazel, Heaven sends a messenger at dawn. Hear him out and take the Vial of Grace."));
        out.add(node("angel_1", 2, 0).after("heeded_the_call").icon(AllItems.ANGEL_BLADE.get()).branch("angel")
                .advancement("main/angel_1").entry("angel_path")
                .name("Lesser Angel")
                .hint("Receive Grace by day in the Overworld: the Vial of Grace, two holy water and four gold, with an Angel Blade to raise it."));
        out.add(node("angel_2", 3, 0).after("angel_1").icon(AllItems.SERAPH_WINGS.get()).branch("angel")
                .advancement("main/angel_2").entry("angel_path")
                .name("Seraph")
                .hint("Ascend with a Damned Contract, three choir shards and the Seraph Wings: your own wings will carry you."));
        out.add(node("angel_3", 4, 0).after("angel_2").icon(AllItems.ARCHANGEL_BLADE.get()).branch("angel")
                .advancement("main/angel_3").entry("angel_path")
                .name("Archangel")
                .hint("By day, give up Lucifer's Grace and an Archangel Blade to become an archangel."));
        out.add(node("angel_4", 5, 0).after("angel_3").icon(AllItems.MICHAEL_LANCE.get()).branch("angel").boss()
                .advancement("main/angel_4").entry("angel_path")
                .name("General of the Host")
                .hint("Usurp the Host: Michael's Grace and a Fallen Star, with Michael's Lance to command them."));
        // --- Hell -----------------------------------------------------------------------------------
        out.add(node("soul_bound", 1, 2).after("crossroads_of_the_soul").icon(AllItems.CROSSROADS_CONTRACT.get()).branch("demon")
                .advancement("main/soul_bound").entry("demon_path")
                .name("Soul Bound")
                .hint("At the crossroads, tick \"Bind my soul\" before you seal a deal, or wish to be made one of them."));
        out.add(node("demon_1", 2, 2).after("soul_bound").icon(AllItems.DEMON_BLOOD.get()).branch("demon")
                .advancement("main/demon_1").entry("demon_path")
                .name("Crossroads Demon")
                .hint("Let the hounds take a soul bound to the crossroads and rise from it a demon, or ask the demon to make you one."));
        out.add(node("demon_2", 3, 2).after("demon_1").icon(AllItems.AZAZEL_BLOOD.get()).branch("demon")
                .advancement("main/demon_2").entry("demon_path")
                .name("Prince of Hell")
                .hint("By night in the Overworld: Azazel's blood, a choir shard and two demon blood make a Prince of Hell."));
        out.add(node("demon_3", 4, 2).after("demon_2").icon(AllItems.FIRST_BLADE.get()).branch("demon")
                .advancement("main/demon_3").entry("demon_path")
                .name("Knight of Hell")
                .hint("Take the Mark of Cain: the Angel Tablet and two demon blood, with the First Blade to raise it."));
        out.add(node("demon_4", 5, 2).after("demon_3").icon(AllItems.FALLEN_STAR.get()).branch("demon").boss()
                .advancement("main/demon_4").entry("demon_path")
                .name("King of Hell")
                .hint("Usurp the throne on the dais of Lucifer's Cage: a Fallen Star and Michael's Grace."));
        return road;
    }

    private static Unlock adv(String path) {
        return Unlock.advancement(SupernaturalCraft.asResource(path));
    }

    private static Unlock item(String path) {
        return Unlock.item(SupernaturalCraft.asResource(path));
    }
}
