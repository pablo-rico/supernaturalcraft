package org.papiricoh.supernaturalcraft.datagen.journal;

import net.minecraft.world.item.Items;
import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.journal.Unlock;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.adv;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.any;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.bowl;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.craft;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.has;
import static org.papiricoh.supernaturalcraft.datagen.journal.SNJournal.entry;

/** The spell bowl: the old witchcraft, liquids, Latin and smoke. */
final class JournalBowl {

    private JournalBowl() {
    }

    /** Spell entries open once the hunter has a bowl or a page to read. */
    private static Unlock bowlOrPage() {
        return any(has(AllItems.SPELL_BOWL), has(AllItems.SPELL_PAGE), adv("first_spell"));
    }

    static void add(List<SNJournal.Entry> out) {
        out.add(entry("spell_bowl", JournalChapter.BOWL).order(0).icon(AllItems.SPELL_BOWL)
                .title("The Spell Bowl")
                .text("Older than sigils and slower: witchcraft, the way Bobby did it. Five copper ingots and a gold one make the bowl. "
                        + "Set it on solid ground.")
                .recipe(craft("spell_bowl"), "The spell bowl")
                .text("Pour in up to four bottles and drop in up to eight ingredients, nothing spare: the mix has to be exactly what "
                        + "the spell asks for. Light it with flint and steel or a fire charge, then speak the Latin before the fire "
                        + "dies. Typing errors cost time; accents and spaces do not matter.")
                .text("A mix no spell answers to blows up in your face: black smoke, a burn, something nasty for a while, and "
                        + "everything in the bowl is lost. Words you never learned do nothing at all.")
                .text("Sneak with empty hands to lift the bowl, contents and all. It takes both hands, and it spills if you jump, "
                        + "fall or get hit. Put in your pack, it keeps everything."));

        out.add(entry("bowl_liquids", JournalChapter.BOWL).order(1).icon(Items.HONEY_BOTTLE)
                .unlock(bowlOrPage())
                .title("What Goes in the Bowl")
                .text("Use a bottle on the bowl to pour it in: water, holy water, demon blood, honey, dragon's breath, a potion, or a "
                        + "vial of someone's blood. An empty bottle scoops a dose back out.")
                .items("Some of the liquids", AllItems.HOLY_WATER, AllItems.DEMON_BLOOD, AllItems.BLOOD_VIAL, Items.HONEY_BOTTLE,
                        Items.DRAGON_BREATH)
                .text("Use anything else on it to drop it in. Each spell's page lists exactly what it takes. Grave dirt, ectoplasm, "
                        + "bones and salt turn up in more than one of them, so keep some by."));

        out.add(entry("spell_pages", JournalChapter.BOWL).order(2).icon(AllItems.SPELL_PAGE)
                .unlock(bowlOrPage())
                .title("Spell Pages")
                .text("You cannot cast what you do not know the words to. Loose pages with a spell written out, Latin and all, turn up "
                        + "in old graves and forgotten chests, and demons carry them: occultists more often than the black-eyed kind.")
                .items("A spell page", AllItems.SPELL_PAGE)
                .text("A master librarian will sell you one for emeralds and a book. And a crossroads demon who comes back angry "
                        + "always has one on it.")
                .text("Read a page to learn its spell, with every recipe for it; the page is used up. Once you know a spell, this "
                        + "journal keeps its recipes for you."));

        out.add(entry("locating", JournalChapter.BOWL).order(3).icon(Items.COMPASS)
                .unlock(bowlOrPage())
                .title("Locating Spells")
                .text("The smoke drifts toward what you seek and tells you which way, and roughly how far. What you put in the bowl "
                        + "decides what it looks for.")
                .recipe(bowl("locate_player"), "A person, by their blood")
                .recipe(bowl("locate_pet"), "A pet, by its collar")
                .recipe(bowl("locate_grave"), "A restless grave")
                .recipe(bowl("locate_spire"), "A Hymnal Spire")
                .text("If what you seek is in another world, or nowhere at all, the smoke sinks back and the bowl keeps its "
                        + "contents."));

        out.add(entry("blood_vials", JournalChapter.BOWL).order(4).icon(AllItems.BLOOD_VIAL)
                .unlock(any(has(AllItems.BLOOD_VIAL), has(AllItems.SPELL_BOWL), has(AllItems.SPELL_PAGE)))
                .title("Blood Vials")
                .text("A glass bottle used on another player draws a vial of their blood; it stings them a little. Sneak and use it "
                        + "in the air to cut your own palm instead, which stings more.")
                .items("A vial of blood", AllItems.BLOOD_VIAL)
                .text("Poured into a bowl, the blood leads the Locating spell straight back to whoever it came from. Keep a vial of "
                        + "each person you hunt with. You will be glad of it the night one of them goes missing."));

        out.add(entry("pet_collars", JournalChapter.BOWL).order(5).icon(AllItems.PET_COLLAR)
                .unlock(any(has(AllItems.PET_COLLAR), has(AllItems.SPELL_BOWL), has(AllItems.SPELL_PAGE)))
                .title("Collars")
                .text("Leather, string and an iron nugget. Use the collar on a tamed animal of yours and it wears the twin of it; the "
                        + "one in your hand is the link.")
                .recipe(craft("pet_collar"), "A pet collar")
                .text("With the collar in the bowl you can find a lost pet, and the collar comes back to you. If the worst has "
                        + "happened, there is a spell for that too.")
                .recipe(bowl("revive_pet"), "Revive Pet"));

        out.add(entry("hex_bags", JournalChapter.BOWL).order(6).icon(AllItems.CURSE_BAG)
                .unlock(any(has(AllItems.CURSE_BAG), has(AllItems.PROTECTION_BAG), has(AllItems.SPELL_PAGE), has(AllItems.SPELL_BOWL)))
                .title("Hex Bags")
                .text("Witches' work. A curse bag tucked out of sight curses whoever lingers near it, all but the one who made it: bad "
                        + "luck, softer blows, a hunger that will not quit, and bleeding that gets worse the longer they stay. Now and "
                        + "then it draws worse: lightning, vermin, or after dark, a demon.")
                .recipe(bowl("hex_bag_curse"), "A curse bag")
                .text("Set it down somewhere hidden, tuck it in a chest, or sneak up and slip it into someone's pocket. Found one? "
                        + "Only fire breaks it: flint and steel, a fire charge, any flame that reaches it, or a Purification.")
                .text("A protection bag in your pack keeps the lesser demons from seeking you out unless you strike first, and no "
                        + "curse bag can touch you. It wears thin while demons are near, and crumbles to dust in the end.")
                .recipe(bowl("hex_bag_protection"), "A protection bag"));

        out.add(entry("cleansing", JournalChapter.BOWL).order(7).icon(Items.LILY_OF_THE_VALLEY)
                .unlock(bowlOrPage())
                .title("Purification and Banishing")
                .text("Purification cleanses everyone near the bowl, and always the one who cast it: possession, jinxes, bleeding and "
                        + "marks lift, minds steady, the possessed are themselves again, curse bags burn and ghosts scatter. A variant "
                        + "of it breaks a crossroads deal.")
                .recipe(bowl("purification"), "Purification")
                .text("Binding holds the nearest creature to the bowl, a ghost before anything else, never a player or one of the "
                        + "great ones. It cannot stray more than a few steps for as long as the bowl stands.")
                .recipe(bowl("bind"), "Binding")
                .text("Banishing is the big hammer. Every ghost near the bowl is laid to rest for good, and demons and hellhounds "
                        + "are cast back into the dark, leaving nothing behind.")
                .recipe(bowl("banish"), "Banishing"));

        out.add(entry("concealment", JournalChapter.BOWL).order(8).icon(Items.PHANTOM_MEMBRANE)
                .unlock(bowlOrPage())
                .title("Concealment and Second Sight")
                .text("Concealment hides you for three minutes from demons, angels, spirits and hellhounds: they neither see nor "
                        + "hunt you. Never from the great ones, and never from a hound that has come to collect on your debt. Strike "
                        + "one of them and it is broken.")
                .recipe(bowl("concealment"), "Concealment")
                .text("Second Sight is the other way round. For three minutes you see what hides: ghosts, hellhounds, invisible "
                        + "things and hidden curse bags, all outlined.")
                .recipe(bowl("second_sight"), "Second Sight"));
    }
}
