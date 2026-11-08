package org.papiricoh.supernaturalcraft.datagen.journal;

import net.minecraft.world.item.Items;
import org.papiricoh.supernaturalcraft.darkness.DarknessRegistry;
import org.papiricoh.supernaturalcraft.darkness.DarknessRules;
import org.papiricoh.supernaturalcraft.dyingsun.DyingSunRules;
import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.adv;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.any;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.has;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.seen;
import static org.papiricoh.supernaturalcraft.datagen.journal.SNJournal.entry;

/**
 * v0.42: season 11, the core. Chapter {@code DARKNESS}, orders 0-49 (the Hands slice's entries take 50-99). Every number here is
 * the code's ({@code DarknessRules}, {@code DyingSunRules}, {@code AmaraEntity}).
 */
final class JournalDarkness {

    private JournalDarkness() {
    }

    static void add(List<SNJournal.Entry> out) {
        out.add(entry("the_darkness", JournalChapter.DARKNESS).order(0).icon(AllItems.VOID_ESSENCE)
                .unlock(any(adv("the_darkness"), adv("unmarked")))
                .title("The Darkness")
                .text("The Mark of Cain was never only a curse. It was the lock God put on his sister, the Darkness, before there was "
                        + "light, and Cain wore it so that no one else would. Take it off and the lock comes off with it.")
                .text("When the cure is read and the Mark burns away, something old and patient pours out of you like black smoke. "
                        + "That is Amara. She is a newborn for now. She will not stay one.")
                .text("From that day she visits you at dawn, a little older each time: after the release, after Superior, after the "
                        + "Cage, after each Hand of God, and when her brother shows his face. Talk to her. She remembers how you treat her."));

        out.add(entry("dying_sun", JournalChapter.DARKNESS).order(1).icon(Items.SCULK_CATALYST)
                .unlock(adv("the_darkness"))
                .title("The Dying Sun")
                .text("The first hunter in a world to let the Darkness out starts the sun dying, for everyone. Every day it gives a "
                        + "little less light, and it never stops by itself: it nears " + Math.round(DyingSunRules.CEILING * 100)
                        + "% darkness and stays there until someone brings Amara to peace.")
                .text("Under it monsters weigh more in every spawn, crops grow slower, and once it is " + Math.round(DyingSunRules.UNDEAD_SPARED * 100)
                        + "% gone the dead stop burning. Past " + Math.round(DyingSunRules.DAY_SPAWNS * 100) + "% they walk by day.")
                .text("When she makes peace the sun comes back whole, for every hunter in the world, and it never dies again."));

        out.add(entry("superior", JournalChapter.DARKNESS).order(2).icon(Items.COMPASS)
                .unlock(any(adv("form_and_void"), seen(DarknessRegistry.FOGGED_VILLAGER.get())))
                .creature(DarknessRegistry.FOGGED_VILLAGER.get())
                .title("Superior")
                .entity(DarknessRegistry.FOGGED_VILLAGER.get(), "A villager the fog took")
                .text("The first town the black fog swallowed. Castiel gives you a compass to it after the release: two roads crossing, "
                        + "four houses with their windows out, and the people still walking the streets. They are not people any more.")
                .text("The fog takes villagers: black veins, black eyes, mindless rage. At night, while the sun is dying, it breaks out in "
                        + "villages near the hunters on this road. Light cures them (a strong one, held to their face) and so does holy "
                        + "water: what comes back is the villager it was, profession and all. When Amara makes peace, the fog lets go of everyone."));

        out.add(entry("casifer", JournalChapter.DARKNESS).order(3).icon(AllItems.CAGE_SHACKLE)
                .unlock(any(adv("o_brother_where_art_thou"), seen(DarknessRegistry.CASIFER.get())))
                .creature(DarknessRegistry.CASIFER.get())
                .title("Casifer")
                .entity(DarknessRegistry.CASIFER.get(), "Lucifer, wearing Castiel")
                .text("Castiel comes the day after Superior with bad news: nothing in Heaven can stand against her. One being has, once. "
                        + "He is in the Cage. Go down to Hell and stand before it.")
                .text("Lucifer offers to help, if he can get out. Castiel says yes before you can stop him, and Lucifer walks out of the "
                        + "Cage wearing him. He follows you, snaps his fingers at whatever attacks you, has an opinion about everything, and "
                        + "now and then points the way to a Hand of God he can feel.")
                .text("He does not last. Once the Hands are found and their owner has shown himself, Amara comes for him at dawn. She "
                        + "has a score to settle with her brother's favourite."));

        out.add(entry("chuck_revealed", JournalChapter.DARKNESS).order(4).icon(AllItems.TYPEWRITER)
                .unlock(any(adv("dont_call_me_shurley"), seen(DarknessRegistry.CHUCK_ALLY.get())))
                .creature(DarknessRegistry.CHUCK_ALLY.get())
                .title("Don't Call Me Shurley")
                .entity(DarknessRegistry.CHUCK_ALLY.get(), "Chuck")
                .text("Hold " + DarknessRules.HANDS_FOR_CHUCK + " Hands of God and, one dawn, a man in a flannel shirt is waiting for "
                        + "you with a coffee. You may have read his books. He is God, and Amara is his sister.")
                .text("He tells you how to call her: the Hands in a void circle, under an open sky, a nether star to light it. He will be "
                        + "there. He owes her that.")
                .text("In her fight he stands in the town from her fourth body on. His light burns her: while it shines, every blow "
                        + "bites deeper. She will try to put it out with chains of smoke; hurt her while she holds them and her grip breaks."));

        out.add(entry("amara_visits", JournalChapter.DARKNESS).order(5).icon(Items.BLACK_CANDLE)
                .unlock(seen(DarknessRegistry.AMARA_VISITOR.get()))
                .creature(DarknessRegistry.AMARA_VISITOR.get())
                .title("Amara at Dawn")
                .entity(DarknessRegistry.AMARA_VISITOR.get(), "Amara, growing up")
                .text("A baby in a curl of smoke, then a girl in a black dress, then a girl of fifteen, then a woman. She is hungry: if a "
                        + "villager is near, she drinks its soul.")
                .text("You may let her (she will like you better for it) or stop her: strike at her, or hold a light up to her before she "
                        + "feeds. The villager lives, and she remembers being refused."));

        out.add(entry("amaras_light", JournalChapter.DARKNESS).order(6).icon(DarknessRegistry.AMARAS_LIGHT)
                .unlock(has(DarknessRegistry.AMARAS_LIGHT))
                .title("Amara's Light")
                .text("Her gift to each hunter who brought her to peace: a mote of the dawn she gave back, in black glass. Held in either "
                        + "hand it lights the way like a lantern no darkness can snuff."));
    }
}
