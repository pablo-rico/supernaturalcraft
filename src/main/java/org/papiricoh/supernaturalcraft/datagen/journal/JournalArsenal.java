package org.papiricoh.supernaturalcraft.datagen.journal;

import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.weapon.Rune;

import java.util.List;

import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.adv;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.any;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.craft;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.has;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.ritual;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.seen;
import static org.papiricoh.supernaturalcraft.datagen.journal.SNJournal.entry;

/** Weapons, catalysts, the Colt, the Hellforge and its runes, and the spoils of the great fights. */
final class JournalArsenal {

    private JournalArsenal() {
    }

    static void add(List<SNJournal.Entry> out) {
        out.add(entry("the_arsenal", JournalChapter.ARSENAL).order(0).icon(AllItems.ANGEL_BLADE)
                .title("The Arsenal")
                .text("Every blade and catalyst worth carrying is forged by ritual, not at a crafting table, and each has a tier, from "
                        + "I to IV, and room for one to three runes. The tier sets what graving them costs.")
                .text("Catalysts are held opposite a grimoire, in the other hand, and change your spells; alone, they have a spell "
                        + "of their own. The heavier weapons are swung by charging: hold use, then let go.")
                .text("Holy weapons count as holy wherever they land. That matters more than their edge against the things at the "
                        + "end of this book."));

        out.add(entry("rubys_knife", JournalChapter.ARSENAL).order(1).icon(AllItems.RUBYS_KNIFE)
                .unlock(any(has(AllItems.RUBYS_KNIFE), has(AllItems.BLOOD_CHALK), adv("the_knife"), adv("caught_in_the_trap")))
                .title("Ruby's Knife")
                .text("An iron sword, two demon blood and a hellfire ember on the altar of a blood circle. It cuts demons deeper than "
                        + "steel should.")
                .recipe(ritual("forge_rubys_knife"), "Forging Ruby's knife")
                .text("A demon slain with it always bleeds: the blood is yours every time, smoke or no smoke. Cold iron, too, so it "
                        + "scatters a ghost. Later it is the key to something worse."));

        out.add(entry("silver_machete", JournalChapter.ARSENAL).order(2).icon(AllItems.SILVER_MACHETE)
                .unlock(any(has(AllItems.SILVER_MACHETE), adv("christo")))
                .title("The Silver Machete")
                .text("Three iron ingots, a salt and a holy water in a small circle. Cheap, and made for one thing.")
                .recipe(ritual("forge_silver_machete"), "Forging the machete")
                .text("Strikes from behind are double criticals, and a kill from behind takes the head. Get behind them."));

        out.add(entry("angel_blade", JournalChapter.ARSENAL).order(3).icon(AllItems.ANGEL_BLADE)
                .unlock(any(has(AllItems.ANGEL_BLADE), has(AllItems.EXORCISTS_MACE), has(AllItems.HELLFIRE_EMBER), adv("angel_blade")))
                .title("Holy Steel")
                .text("The angel blade: a diamond sword, two holy water, two gold ingots and a hellfire ember, in a binding circle. "
                        + "Hold use, then let go, and you dash through your enemies in a line of light.")
                .recipe(ritual("forge_angel_blade"), "Forging an angel blade")
                .text("The exorcist's mace: an iron block in place of the sword. It may stun what it hits, and brought down from a "
                        + "height it sends out a ring of holy force.")
                .recipe(ritual("forge_exorcists_mace"), "Forging the exorcist's mace")
                .text("Both are holy. Both are tier II. Both are what you want in your hand the first time you open the Cage."));

        out.add(entry("catalysts", JournalChapter.ARSENAL).order(4).icon(AllItems.ENOCHIAN_ORB)
                .unlock(any(has(AllItems.GRIMOIRE), has(AllItems.EMBER_STAFF), has(AllItems.ENOCHIAN_ORB), has(AllItems.CENSER_OF_GRACE)))
                .title("Catalysts")
                .text("The ember staff, from sticks, sulfur and flint in a small circle. Opposite a grimoire, your hellfire burns "
                        + "hotter and bolts burst where they land. Alone, it hurls fireballs.")
                .recipe(ritual("forge_ember_staff"), "The ember staff")
                .text("The Enochian orb: spells cost less and come back faster, and bolts split in three. Alone, it throws a seeking "
                        + "glyph.")
                .recipe(ritual("forge_enochian_orb"), "The Enochian orb")
                .text("The censer of grace, holy: holy spells strike harder, Burst lingers and Ward grows. Alone, hold use for a beam "
                        + "of light that lights the ground where it falls. Against the Darkness, that beam is worth more than any blade.")
                .recipe(ritual("forge_censer_of_grace"), "The censer of grace"));

        out.add(entry("heavy_weapons", JournalChapter.ARSENAL).order(5).icon(AllItems.HELLFIRE_GREATSWORD)
                .unlock(any(has(AllItems.SOUL_SCYTHE), has(AllItems.HELLFIRE_GREATSWORD), has(AllItems.BLOOD_CHALK)))
                .title("Scythe and Greatsword")
                .text("The soul scythe is forged at night in a blood circle. Its swings reap a wide arc and drink some of the life they "
                        + "take. Hold use and let go for a crescent of souls.")
                .recipe(ritual("forge_soul_scythe"), "The soul scythe")
                .text("The hellfire greatsword is slow and heavy. Hold use and let go to cleave a cone in front of you and leave a "
                        + "trail of hellfire behind it.")
                .recipe(ritual("forge_hellfire_greatsword"), "The hellfire greatsword")
                .text("Both are tier III. The scythe also lights the rite that forges the Ring of Death."));

        out.add(entry("the_colt", JournalChapter.ARSENAL).order(6).icon(AllItems.THE_COLT)
                .unlock(any(has(AllItems.THE_COLT), has(AllItems.ENDLESS_COLT), adv("nothing_it_cant_kill"), adv("devil_went_down")))
                .title("The Colt")
                .text("A Paterson revolver, 1836, five chambers. Samuel Colt made it for one job. There's nothing this gun can't kill. "
                        + "Lucifer carries it, loaded, and leaves it when he falls.")
                .items("The Colt and its rounds", AllItems.THE_COLT, AllItems.COLT_BULLET)
                .text("One round kills any lesser demon outright, any shade, echo or illusion. Against a great one it strikes a fixed, "
                        + "heavy blow that no armour or limit blunts, though it will not carry them past one of their turning points. "
                        + "Use to fire; sneak-use or the reload key seats one round at a time.")
                .text("The rounds cannot be crafted. Once you have held the Colt, forge them by night in a blood circle, eight at a "
                        + "time. A few lie in the Hymnal Spire's temple, and with the great ones when they fall.")
                .recipe(ritual("forge_colt_bullets"), "Forging consecrated rounds")
                .items("The Endless Colt (creative only)", AllItems.ENDLESS_COLT)
                .text("Some hunters write their own rules. The Endless Colt is the same gun, always loaded: it never spends a round "
                        + "and never needs reloading. No rite or grave gives it; it is only found in creative mode."));

        out.add(entry("archangel_blade", JournalChapter.ARSENAL).order(7).icon(AllItems.ARCHANGEL_BLADE)
                .unlock(any(has(AllItems.ARCHANGEL_BLADE), adv("devil_went_down")))
                .title("The Archangel Blade")
                .text("Lucifer's own blade, left when he falls. Holy, deadly to demons, and tier IV.")
                .items("The Archangel Blade", AllItems.ARCHANGEL_BLADE)
                .text("Use it and it smites everything in a cone before you, for a price in mana."));

        out.add(entry("hellforge", JournalChapter.ARSENAL).order(8).icon(AllItems.HELLFORGE)
                .unlock(any(has(AllItems.HELLFORGE), has(AllItems.HELLFIRE_EMBER), adv("hellforge")))
                .title("The Hellforge")
                .text("An anvil, a blast furnace, two hellfire embers and two obsidian in a binding circle raise a Hellforge. It "
                        + "graves runes into weapons, and holds nothing itself.")
                .recipe(ritual("forge_hellforge"), "Raising the Hellforge")
                .text("Put the weapon in, and the runes beside it, as many as it has free slots. Grave costs the weapon's tier times "
                        + "two levels per rune. No more than two alike on one weapon.")
                .text("Purge strips a weapon for one level and gives the runes back, all but one, lost at random. Think before you "
                        + "grave."));

        out.add(entry("runes", JournalChapter.ARSENAL).order(9).icon(AllItems.RUNE_BLANK)
                .unlock(any(has(AllItems.HELLFORGE), has(AllItems.RUNE_BLANK), adv("hellforge"), adv("graven")))
                .title("Runes")
                .text("A blank rune is smooth stone, chalk and Enochian ink. Carve it at a small circle: the offerings on the altar, "
                        + "the blank in your hand to light it.")
                .recipe(craft("rune_blank"), "A blank rune")
                .text("For blades: Edge cuts deeper, Ember sets alight, Frost slows, Leech heals you from what you deal, Swiftness "
                        + "quickens your swing. For catalysts: Resonance makes spells cheaper and harder, Focus brings them back faster, "
                        + "Echo makes them repeat now and then. Sanctity fits anything and makes every blow holy.")
                .items("Blade runes", AllItems.RUNES.get(Rune.EDGE), AllItems.RUNES.get(Rune.EMBER), AllItems.RUNES.get(Rune.FROST),
                        AllItems.RUNES.get(Rune.LEECH), AllItems.RUNES.get(Rune.SWIFTNESS))
                .items("Catalyst runes, and Sanctity", AllItems.RUNES.get(Rune.RESONANCE), AllItems.RUNES.get(Rune.FOCUS),
                        AllItems.RUNES.get(Rune.ECHO), AllItems.RUNES.get(Rune.SANCTITY))
                .recipe(ritual("carve_rune_sanctity"), "Carving Sanctity")
                .text("Two runes come only from the great ones. Void, from what the Darkness leaves, carved at night: it drinks the "
                        + "light, and only a tier IV weapon will take it. Hymn, from the Chorus's shards: every fourth blow or spell "
                        + "rings out again as holy light.")
                .items("The rare runes", AllItems.RUNES.get(Rune.VOID), AllItems.RUNES.get(Rune.HYMN)));

        out.add(entry("hungry_things", JournalChapter.ARSENAL).order(10).icon(AllItems.FIRST_BLADE)
                .unlock(any(has(AllItems.FIRST_BLADE), has(AllItems.WHISPERING_CODEX), adv("mark_of_cain"), adv("whispers"),
                        adv("the_knife")))
                .title("Hungry Things")
                .text("The First Blade. A great circle at night, lit with Ruby's knife: two bone blocks, three demon blood, a netherite "
                        + "ingot, a wither skeleton skull and a hellfire ember. It is bound to whoever forged it, and burns anyone else "
                        + "who holds it.")
                .recipe(ritual("forge_first_blade"), "The First Blade")
                .text("Feed it kills and it grows: at the third level it lets you lunge, and at the fifth, well fed, it will not let "
                        + "you die. Starve it and it feeds on you. It whispers. Do not listen.")
                .text("The Whispering Codex is the same hunger in a book: a cursed catalyst, bound to its maker. Spells cast through it "
                        + "cost blood and sanity instead of mana, and they chain and pierce. Alone, it speaks the Forbidden Word, and "
                        + "whatever it marks takes more harm.")
                .recipe(ritual("forge_whispering_codex"), "The Whispering Codex")
                .text("Watch your sanity with the Codex in hand. When it runs low, the words twist."));

        out.add(entry("penumbra", JournalChapter.ARSENAL).order(11).icon(AllItems.PENUMBRA)
                .unlock(any(has(AllItems.PENUMBRA), has(AllItems.ECLIPSE_SIGHT), adv("penumbra"), adv("dawn")))
                .title("Spoils of the Dark")
                .text("Penumbra, the blade that drinks the light, left by the Darkness. It drinks light from bright places and from lit "
                        + "foes. Hold use and let go for an Umbra Wave that blinds and wounds, stronger the more light it holds. "
                        + "It cuts deeper in the dark.")
                .items("Penumbra and the Eclipse Sight", AllItems.PENUMBRA, AllItems.ECLIPSE_SIGHT, AllItems.VOID_ESSENCE)
                .text("The Eclipse Sight is a vial of her own sight. Drink it and you are marked for good: darkness and blindness "
                        + "cannot take you, the eclipse cannot hide the world, hellhounds are plain to see, and your mana runs "
                        + "twenty-five deeper."));

        out.add(entry("hound_whistle", JournalChapter.ARSENAL).order(12).icon(AllItems.HOUND_WHISTLE)
                .unlock(any(has(AllItems.HOUND_WHISTLE), seen(AllEntities.BOUND_HELLHOUND.get())))
                .creature(AllEntities.BOUND_HELLHOUND.get())
                .title("Lilith's Whistle")
                .entity(AllEntities.BOUND_HELLHOUND.get(), "A hellhound at heel", 0.55f)
                .text("Lilith keeps her dogs on a whistle, and she drops it when she dies. Blow it and a hellhound runs at your side "
                        + "for a minute, for a little mana.")
                .text("Yours you can see. It goes for whatever hurts you and whatever you strike, and it never turns on a player. "
                        + "One at a time."));

        out.add(entry("trophies", JournalChapter.ARSENAL).order(13).icon(AllItems.MORNINGSTAR_TROPHY)
                .unlock(any(has(AllItems.AZAZEL_TROPHY), has(AllItems.LILITH_TROPHY), has(AllItems.MORNINGSTAR_TROPHY),
                        has(AllItems.CHOIR_TROPHY), has(AllItems.METATRON_TROPHY), has(AllItems.ECLIPSE_TROPHY)))
                .title("Trophies")
                .text("Every great one leaves its likeness behind, a little statue to set on a shelf and remember the night by.")
                .items("The trophies", AllItems.AZAZEL_TROPHY, AllItems.LILITH_TROPHY, AllItems.MORNINGSTAR_TROPHY, AllItems.CHOIR_TROPHY,
                        AllItems.METATRON_TROPHY, AllItems.ECLIPSE_TROPHY)
                .text("Do not get too attached to three of them. War wants Lucifer's, Famine the Darkness's and Pestilence the "
                        + "Chorus's."));
    }
}
