package org.papiricoh.supernaturalcraft.datagen.journal;

import net.minecraft.world.item.Items;
import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.adv;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.any;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.craft;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.glyph;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.has;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.ritual;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.seen;
import static org.papiricoh.supernaturalcraft.datagen.journal.SNJournal.entry;

/** Basics, sigils and rituals: what every hunter learns before the first real hunt. */
final class JournalBasics {

    private JournalBasics() {
    }

    static void add(List<SNJournal.Entry> out) {
        basics(out);
        sigils(out);
        rituals(out);
    }

    private static void basics(List<SNJournal.Entry> out) {
        out.add(entry("family_business", JournalChapter.BASICS).order(0).icon(AllItems.SALT)
                .title("The Family Business")
                .text("Saving people, hunting things. If you are reading this, you already know the world is not what they told you. "
                        + "Demons walk at night wearing people. The dead do not always stay down. And somewhere under it all, something "
                        + "is waiting in a Cage.")
                .text("This journal is everything I know. Pages fill in as you see things, kill things and get your hands on things: "
                        + "a page you have not earned stays blank. Read in order if you are new. Start with salt.")
                .items("The first tools of the trade", AllItems.SALT, AllItems.CHALK, AllItems.HOLY_WATER, AllItems.GRIMOIRE)
                .text("Chalk comes from calcite and bone meal, and you will draw every circle in this book with it. Mix four chalk "
                        + "with a demon's blood and you have blood chalk, for the circles that matter.")
                .recipe(craft("chalk"), "Chalk: calcite and bone meal"));

        out.add(entry("salt", JournalChapter.BASICS).order(1).icon(AllItems.SALT)
                .title("Salt")
                .text("Salt is your first friend. Rock salt shows up in the stone like any ore, deeper as deepslate. No ore nearby? "
                        + "Boil a bucket of water dry in a furnace.")
                .items("Rock salt, in stone and deepslate", AllItems.ROCK_SALT_ORE, AllItems.DEEPSLATE_ROCK_SALT_ORE, AllItems.SALT)
                .recipe(craft("salt_from_evaporation"), "Salt from a water bucket")
                .text("Pour it on the ground as a line. You can walk over it; a demon cannot. It is a wall to them, solid as stone, "
                        + "and they will walk the long way round rather than test it. A ghost cannot cross it either.")
                .text("Salt will not stop the great ones. Nothing you pour on the floor holds a boss. Keep a stack on you anyway: "
                        + "the exorcism, the binding, the bones of a restless grave and half the bowl spells want it."));

        out.add(entry("holy_water", JournalChapter.BASICS).order(2).icon(AllItems.HOLY_WATER)
                .title("Holy Water")
                .text("Three bottles of water and a pinch of salt on a ritual altar, inside a small chalk ring, lit with flint and "
                        + "steel: three flasks of holy water. That is the first rite any hunter learns.")
                .recipe(ritual("consecration"), "Consecration")
                .text("Throw it like a splash potion. It scalds demons. It scatters a ghost for a while. It shows a hellhound for "
                        + "what it is. In Hell, a dash of it eases the Torment.")
                .text("You will pour more of it into rites and bowls than you ever throw. Make it by the dozen."));

        out.add(entry("devils_trap", JournalChapter.BASICS).order(3).icon(AllItems.DEVILS_TRAP)
                .title("Devil's Trap")
                .text("A devil's trap is painted, not built. A square of chalk with a lit candle at each corner, demon blood, salt, "
                        + "paper and red dye on the altar: two traps. Use one on the floor and it paints the whole 3x3 seal around "
                        + "where you clicked, or nothing at all if it does not fit.")
                .recipe(ritual("binding"), "Binding: two devil's traps")
                .text("Any demon standing on any part of it is held fast. It can still turn and swing at you, but it cannot walk, "
                        + "jump or smoke out of its vessel. That last part is the point: a demon you trap is a demon you get to finish, "
                        + "and its blood with it.")
                .text("The big ones laugh at paint. Azazel walks right over a trap and burns it on the way."));

        out.add(entry("hunters_amulet", JournalChapter.BASICS).order(4).icon(AllItems.HUNTERS_AMULET)
                .title("The Amulet")
                .text("Two strings, a gold ingot, and a drop of demon blood in the middle. It grows warm when something supernatural "
                        + "comes within sixteen blocks of you, and it steadies your hand: sigils cost a tenth less mana while you wear it.")
                .recipe(craft("hunters_amulet"), "The Hunter's Amulet")
                .text("Wear it around your neck if you have the slot for it (Curios), or keep it in your hotbar or off hand."));

        out.add(entry("mana_and_sanity", JournalChapter.BASICS).order(5).icon(AllItems.ENOCHIAN_INK)
                .title("Mana and Sanity")
                .text("Every sigil and every rite draws on your mana. You start with a hundred and it seeps back on its own, a couple "
                        + "of points a second. The blue bar shows where you stand.")
                .text("It can run deeper. An archangel's grace adds fifty for good; the Darkness's mark adds twenty-five; a crossroads "
                        + "deal will sell you twenty-five more, at the usual price.")
                .text("Sanity is the other bar, and most hunters never see it move. Some cursed things are paid for in your mind. "
                        + "Let it run low and you will hear whispers, and the words start to twist in your mouth. It comes back slowly on its own; "
                        + "a Purification steadies it at once."));

        out.add(entry("hunters_book", JournalChapter.BASICS).order(6).icon(AllItems.GRIMOIRE)
                .title("The Hunter's Book")
                .text("Sneak and use your grimoire and the whole book opens. Four ribbons down the edge: Home, Journal, Scriptorium "
                        + "and Roadmap.")
                .text("Home is the state of you: mana, sanity, what has changed you, any deal you owe on, and the next thing worth "
                        + "hunting. The Journal is these pages; mark the ones you need with a ribbon. The Scriptorium is where spells "
                        + "are composed, written into the grimoire or onto scrolls, and kept in a library for later.")
                .text("The Roadmap is the long road from salt to the Cage, every hunt that matters laid out in order. What you have not "
                        + "found yet is a shadow on it."));
    }

    private static void sigils(List<SNJournal.Entry> out) {
        out.add(entry("grimoire", JournalChapter.SIGILS).order(0).icon(AllItems.GRIMOIRE)
                .title("The Grimoire")
                .text("A book, a vial of Enochian ink, chalk and leather. Ink is an ink sac, sulfur and salt in a glass bottle; "
                        + "you will want plenty of it.")
                .recipe(craft("grimoire"), "The grimoire")
                .recipe(craft("enochian_ink"), "Enochian ink")
                .text("The first time you open it its first pages make sense: Touch, Bolt, Smite and Mend. Everything else you learn "
                        + "from torn sigil pages.")
                .text("It holds six pages, each a spell. Use it to cast the open page; sneak and turn the scroll wheel (or press the "
                        + "page key) to turn pages. Sneak-use opens this book, and the Scriptorium inside it is where spells are written: "
                        + "each page you inscribe costs a vial of ink."));

        out.add(entry("sigil_forms", JournalChapter.SIGILS).order(1).icon(AllItems.SIGIL_PAGE)
                .unlock(any(has(AllItems.GRIMOIRE), has(AllItems.SIGIL_PAGE)))
                .title("Forms")
                .text("A spell is a form, up to three effects and up to three modifiers. The form is how it reaches the world.")
                .image(glyph("bolt"), 32, 32, "The glyph of Bolt")
                .text("Touch takes whatever is within arm's reach; a helpful spell with nothing there falls back on you. Bolt is "
                        + "thrown and flies straight until it hits something. Burst is a pulse outward from you. Ward is a circle at "
                        + "your feet that keeps working and stops hostile projectiles.")
                .text("Touch and Bolt are cheap and quick. Burst and Ward cost more and your hand needs longer to recover. Ward is "
                        + "the harder sigil to come by, and the one you will want when something big starts to glow."));

        out.add(entry("sigil_effects", JournalChapter.SIGILS).order(2).icon(AllItems.SIGIL_PAGE)
                .unlock(any(has(AllItems.GRIMOIRE), has(AllItems.SIGIL_PAGE)))
                .title("Effects")
                .text("The effects are what the spell does. You cannot draw the same one twice in a spell.")
                .image(glyph("smite"), 32, 32, "The glyph of Smite")
                .text("Smite is holy force, twice as hard on demons and the dead. Hellfire burns flesh and a pinch of sulfur. Frost "
                        + "slows, freezes, and turns still water to ice. Mend closes wounds and smothers flames. Repel throws the target "
                        + "away from you. Reveal strips invisibility, outlines what it touches and unmasks illusions.")
                .text("Two are a hunter's sigils. Exorcise is agony to a demon, and casts a trapped one out entirely; it burns salt. "
                        + "Bind holds a demon as a devil's trap would and hobbles anything else; it burns demon blood.")
                .items("What the sigils burn", AllItems.SULFUR, AllItems.SALT, AllItems.DEMON_BLOOD));

        out.add(entry("sigil_modifiers", JournalChapter.SIGILS).order(3).icon(AllItems.SIGIL_PAGE)
                .unlock(any(has(AllItems.GRIMOIRE), has(AllItems.SIGIL_PAGE)))
                .title("Modifiers")
                .text("Modifiers bend a spell and multiply its cost. Empower makes the effects stronger. Extend makes them last "
                        + "longer and reach further. Widen spreads them over an area.")
                .image(glyph("echo"), 32, 32, "The glyph of Echo")
                .text("Echo makes the spell repeat itself a moment later. It is written in a hand no one can follow unless they have "
                        + "been touched by grace. Lucifer carries a page of it.")
                .text("The Scriptorium shows the cost of a spell as you build it. Watch it: three modifiers on a Burst will empty "
                        + "you in one cast."));

        out.add(entry("sigil_pages", JournalChapter.SIGILS).order(4).icon(AllItems.SIGIL_PAGE)
                .unlock(any(has(AllItems.GRIMOIRE), has(AllItems.SIGIL_PAGE)))
                .title("Torn Pages")
                .text("Demons carry scraps of their masters' grimoires. Kill one and now and then a torn sigil page falls out of it; "
                        + "occultists carry them far more often than the black-eyed brawlers. Read a page to learn its sigil for good.")
                .items("A torn sigil page", AllItems.SIGIL_PAGE)
                .text("Some pages are written in the third tier, in a hand that burns your eyes. Keep them. Once you have taken in "
                        + "an archangel's grace, they will read like anything else.")
                .text("The Scriptorium keeps an encyclopedia of the sixteen sigils: the ones you know, and the shapes of the ones "
                        + "you do not."));

        out.add(entry("spell_scrolls", JournalChapter.SIGILS).order(5).icon(AllItems.SPELL_SCROLL)
                .unlock(any(has(AllItems.GRIMOIRE), has(AllItems.SPELL_SCROLL)))
                .title("Scrolls")
                .text("Any spell you can compose you can write out in full on a scroll: a sheet of paper and a vial of Enochian ink "
                        + "each. The Scriptorium writes them in batches if you have the paper and ink for it.")
                .items("Paper, ink, scroll", Items.PAPER, AllItems.ENOCHIAN_INK, AllItems.SPELL_SCROLL)
                .text("A scroll is read aloud once and burns up. Anyone can read one, sigils known or not, and it costs half the mana "
                        + "the spell would. Hand them to whoever hunts with you."));
    }

    private static void rituals(List<SNJournal.Entry> out) {
        out.add(entry("rituals", JournalChapter.RITUALS).order(0).icon(AllItems.RITUAL_ALTAR)
                .title("Rituals")
                .text("Every rite starts with an altar: polished blackstone bricks, a gold ingot and a candle. Set it down, and draw "
                        + "the circle the rite asks for around it, on the same level, with chalk lines and candles lit where it wants them.")
                .recipe(craft("ritual_altar"), "The ritual altar")
                .text("Lay the offerings on the altar one at a time, then light the rite with what it asks for, flint and steel more "
                        + "often than not. Some rites want the night, some the day, some a certain place. The rite takes its mana up "
                        + "front.")
                .text("If the circle is wrong, red sparks mark the first bad block. Once it is burning, never smudge a line or snuff "
                        + "a candle: the circle breaks, lightning falls, it burns you, and something comes through that should not."));

        out.add(entry("ritual_circles", JournalChapter.RITUALS).order(1).icon(AllItems.BLOOD_CHALK)
                .title("The Circles")
                .text("The small circle: eight chalk lines around the altar, nothing else. The binding circle: a 5x5 chalk square "
                        + "with a lit candle on each corner. The blood circle is the same square drawn in blood chalk.")
                .recipe(craft("blood_chalk"), "Blood chalk: four chalk and a demon's blood")
                .text("The great circle is the one for summonings: a ring of blood chalk nine wide, with six lit black candles inside "
                        + "it. The void circle is a ring of the same size broken by crying obsidian at its four points, with four black "
                        + "candles close around the altar.")
                .text("There is one more circle, drawn only around four old stones under the Cage in Hell. If you ever need it, you "
                        + "will know.")
                .items("Chalk, blood chalk, black candle, crying obsidian", AllItems.CHALK, AllItems.BLOOD_CHALK, Items.BLACK_CANDLE,
                        Items.CRYING_OBSIDIAN));

        out.add(entry("exorcism", JournalChapter.RITUALS).order(2).icon(AllItems.HELLFIRE_EMBER)
                .unlock(any(has(AllItems.DEVILS_TRAP), adv("caught_in_the_trap"), seen(AllEntities.BLACK_EYED_DEMON.get())))
                .title("Exorcism")
                .text("Trap them first. Then a blood circle, two salt and a holy water on the altar, and fire. Every trapped demon "
                        + "within six blocks is cast out of its vessel and sent back down, and each one leaves a hellfire ember behind.")
                .recipe(ritual("exorcism"), "Exorcism")
                .items("Hellfire ember", AllItems.HELLFIRE_EMBER)
                .text("Embers are what the serious rites are paid in: the angel blade, the Hellforge, the Key itself. Every demon you "
                        + "kill in a trap instead of casting out is one ember you will not have. The Exorcise sigil does the same "
                        + "work, one trapped demon at a time."));

        out.add(entry("key_to_the_cage", JournalChapter.RITUALS).order(3).icon(AllItems.KEY_TO_THE_CAGE)
                .unlock(any(has(AllItems.AZAZEL_BLOOD), has(AllItems.KEY_TO_THE_CAGE), adv("yellow_eyed")))
                .title("The Key to the Cage")
                .text("The Key is forged, not found. A great circle, at night: a nether star, four hellfire embers, a gold block and "
                        + "two vials of Azazel's own blood. Nothing else will do for the blood. He has to fall first.")
                .recipe(ritual("forge_key_to_the_cage"), "Forging the Key")
                .text("The Key lights the rite that opens the Cage, and later, the rifts that lead down into Hell. Lose to what is in "
                        + "the Cage and he leaves a cracked key behind him; a great circle at night, two embers and a demon blood make "
                        + "it whole again.")
                .recipe(ritual("reforge_key"), "Reforging a cracked key"));

        out.add(entry("eclipse", JournalChapter.RITUALS).order(4).icon(Items.SCULK_CATALYST)
                .unlock(any(adv("devil_went_down"), seen(AllEntities.AMARA.get()), has(AllItems.ECLIPSE_TROPHY)))
                .title("The Eclipse")
                .text("A great rite can put out the sun. A great circle in the Overworld, by day: two crying obsidian, two black dye, "
                        + "an eye of ender, two demon blood and a sculk catalyst. It costs more mana than a hunter is born with.")
                .recipe(ritual("begin_eclipse"), "Eclipsing the sun")
                .text("For ten minutes the whole world goes dark. Demons walk by day, and more of them. The dead do not burn in the "
                        + "light that is left. Holy strikes bite a little deeper.")
                .text("Only under an eclipse can the Darkness be called. That is the real reason you would ever do this."));
    }
}
