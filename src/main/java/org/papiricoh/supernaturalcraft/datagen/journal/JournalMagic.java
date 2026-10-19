package org.papiricoh.supernaturalcraft.datagen.journal;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.journal.Unlock;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.adv;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.any;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.bowl;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.craft;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.glyph;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.has;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.ritual;
import static org.papiricoh.supernaturalcraft.datagen.journal.SNJournal.entry;

/**
 * The magic of v0.21: the four schools, mastery (with the one discount rule of {@code magic.ArcanaCost}), the new sigils (by
 * school, with their glyphs), the bowl's target layer and distillates (taglock, bound blood, chrism, hexed ink), and its
 * witchcraft (curses, tracking, control, protection, familiars, the marks they leave, and a Ward over the altar). Every number here is the code's: {@code MasteryRules},
 * {@code ArcanaCost}, {@code SNConfig.HUMAN_RITUAL_DISCOUNT} (15 % by default).
 */
final class JournalMagic {

    private JournalMagic() {
    }

    /** Anyone who has started on any kind of magic. */
    private static Unlock anyMagic() {
        return any(has(AllItems.GRIMOIRE), has(AllItems.SIGIL_PAGE), has(AllItems.SPELL_BOWL), has(AllItems.SPELL_PAGE),
                adv("first_spell"));
    }

    private static String school(String id) {
        return "supernaturalcraft:textures/gui/school/" + id + ".png";
    }

    private static String mastery(String id) {
        return "supernaturalcraft:textures/gui/mastery/" + id + ".png";
    }

    static void add(List<SNJournal.Entry> out) {
        schools(out);
        sigils(out);
        distillates(out);
        witchcraft(out);
    }

    private static void schools(List<SNJournal.Entry> out) {
        out.add(entry("four_schools", JournalChapter.SIGILS).order(6).icon(AllItems.ENOCHIAN_INK)
                .unlock(anyMagic())
                .title("The Four Schools")
                .text("Every effect sigil, every bowl spell and every rite belongs to one of four schools. The school is the colour "
                        + "its light burns in, the mastery it trains and the discount it earns. A spell takes the school of its first "
                        + "effect; forms and modifiers belong to none, and are drawn in plain chalk white.")
                .image(school("enochian"), 32, 32, "Enochian: gold and white")
                .text("Enochian is Heaven's tongue, gold with a white heart: Smite, Mend, Banishing, and the Ward, the one form that "
                        + "is Heaven's own. Holy water and consecrations are Enochian rites.")
                .image(school("infernal"), 32, 32, "Infernal: fire red and ember orange")
                .text("Infernal is Hell's craft, red fire with orange embers: Hellfire, and the summonings and pacts that call on "
                        + "the pit.")
                .image(school("witchcraft"), 32, 32, "Witchcraft: magenta and plum")
                .text("Witchcraft is the old art, magenta smoke on plum: Frost among the sigils, and most of what the spell bowl "
                        + "does, from hex bags to locating spells.")
                .image(school("hunter"), 32, 32, "Hunter: steel and iron blue")
                .text("Hunter is the plain lore of salt, silver and iron, steel grey with a blue edge: Repel, Reveal, Exorcise and "
                        + "the Snare, and the rites that drive things out."));

        out.add(entry("mastery", JournalChapter.SIGILS).order(7).icon(Items.EXPERIENCE_BOTTLE)
                .unlock(anyMagic())
                .title("Mastery")
                .text("Each school trains on its own. A sigil teaches a fifth of the mana it cost; a bowl spell twice that for the "
                        + "same mana; a rite a third of its mana. Practice what you mean to be good at. Mastery stays with you through "
                        + "death.")
                .image(mastery("apprentice"), 32, 32, "Apprentice: 150")
                .text("An Apprentice (150) reads that school's second-tier pages, pays 5% less for its magic and has 5 more mana.")
                .image(mastery("adept"), 32, 32, "Adept: 600")
                .text("An Adept (600) reads third-tier pages, pays 10% less and has 10 more mana.")
                .image(mastery("master"), 32, 32, "Master: 1800")
                .text("A Master (1800) reads a Master's pages, pays 15% less and has 20 more mana. Only your highest rank in any "
                        + "school adds mana: ranks in four schools do not add up to more.")
                .text("Discounts add together and never take off more than 40%: being human saves 15% on all magic, the Hunter's "
                        + "Amulet 10%, and your rank in the spell's school the rest. A scroll still halves what is left on top of that.")
                .text("A page beyond you stays unread, and you keep it. A page of no school, a form or a modifier, wants the rank in "
                        + "any school. What you have learned is never taken away.")
                .text("Morningstar's Spark, what is left in you after you take in Lucifer's grace, reads every page as an Adept "
                        + "would. That is how Echo is read by those who never studied.")
                .items("What steadies the hand", AllItems.HUNTERS_AMULET, AllItems.SPELL_SCROLL, AllItems.LUCIFERS_GRACE));
    }

    private static void distillates(List<SNJournal.Entry> out) {
        out.add(entry("taglocks", JournalChapter.BOWL).order(10).icon(AllItems.TAGLOCK)
                .unlock(any(has(AllItems.TAGLOCK), has(AllItems.SPELL_BOWL), has(AllItems.SPELL_PAGE)))
                .title("Taglocks")
                .text("A twist of paper and string. Use a blank taglock on a creature or a player to snip a lock of their hair; sneak "
                        + "and use it in the air to take your own.")
                .recipe(craft("taglock"), "Blank taglocks")
                .text("Snipping is quiet but not invisible: one time in four they notice. A beast turns on you; a person is told "
                        + "someone tugged at their hair, and by whom.")
                .text("In the bowl a filled taglock names whom the spell is for, as a dose of someone's blood does, and no recipe "
                        + "counts it as an ingredient: drop it in beside the rest. If someone's blood is poured in as well, the blood "
                        + "speaks first.")
                .recipe(bowl("locate_player"), "Locating a person: water, and their blood poured in or their taglock"));

        out.add(entry("bound_blood", JournalChapter.BOWL).order(11).icon(AllItems.BOUND_BLOOD)
                .unlock(any(has(AllItems.BOUND_BLOOD), has(AllItems.CHRISM), has(AllItems.HEXED_INK), Unlock.rite(rl("brew_bound_blood"))))
                .title("Bound Blood")
                .text("A vial of someone's blood, bound to them in the spell bowl: pour in their blood, add redstone and a string, "
                        + "and recite. An Infernal working. The vial remembers whose it is.")
                .recipe(bowl("brew_bound_blood"), "Binding blood")
                .items("Bound blood, and the vial it starts as", AllItems.BOUND_BLOOD, AllItems.BLOOD_VIAL)
                .text("Lilith will not come for a sheet of paper any more: her contract is signed in bound blood. Keep the vials "
                        + "apart; the label is the only thing that tells two of them apart to anyone but the bowl.")
                .recipe(ritual("summon_lilith"), "Summoning Lilith"));

        out.add(entry("chrism", JournalChapter.BOWL).order(12).icon(AllItems.CHRISM)
                .unlock(any(has(AllItems.CHRISM), has(AllItems.BOUND_BLOOD), has(AllItems.HEXED_INK), Unlock.rite(rl("brew_chrism"))))
                .title("Chrism")
                .text("Holy oil, blessed in the spell bowl: two doses of holy water, a holy oil and a glistering melon slice. An "
                        + "Enochian working, and an easy one.")
                .recipe(bowl("brew_chrism"), "Blessing chrism")
                .text("It is not thrown and not burned: rites ask for it as a reagent. The angel blade takes one chrism and one holy "
                        + "water where it used to take two holy water, and Metatron will not come down without it.")
                .recipe(ritual("forge_angel_blade"), "Forging an angel blade")
                .recipe(ritual("summon_metatron"), "Calling down Metatron"));

        out.add(entry("hexed_ink", JournalChapter.BOWL).order(13).icon(AllItems.HEXED_INK)
                .unlock(any(has(AllItems.HEXED_INK), has(AllItems.CHRISM), has(AllItems.BOUND_BLOOD), Unlock.rite(rl("brew_hexed_ink"))))
                .title("Hexed Ink")
                .text("Enochian ink steeped in one school's magic: pour in a potion, add the ink and something to steep it in. A gold "
                        + "nugget steeps it Enochian, blaze powder Infernal, an amethyst shard Witchcraft, salt Hunter. Nothing to "
                        + "steep in, nothing to brew.")
                .recipe(bowl("brew_hexed_ink"), "Steeping hexed ink")
                .items("What the ink steeps in", Items.GOLD_NUGGET, Items.BLAZE_POWDER, Items.AMETHYST_SHARD, AllItems.SALT)
                .text("Write a page of your grimoire while you carry hexed ink of that spell's school and the Scriptorium uses it "
                        + "instead of plain ink. The page remembers: a spell of that school cast from it is 15% stronger. Rewrite "
                        + "the page and the ink is spent again.")
                .text("The Enochian orb is forged with one vial of hexed ink, of any school, in place of a second plain one.")
                .recipe(ritual("forge_enochian_orb"), "The Enochian orb"));
    }

    private static ResourceLocation rl(String path) {
        return SupernaturalCraft.asResource(path);
    }

    /** Opens once any of these bowl spells is known (by their recipe file names). */
    private static Unlock rites(String... spells) {
        Unlock[] options = new Unlock[spells.length];
        for (int i = 0; i < spells.length; i++) options[i] = Unlock.rite(rl(spells[i]));
        return any(options);
    }

    private static Unlock sigilBook() {
        return any(has(AllItems.GRIMOIRE), has(AllItems.SIGIL_PAGE));
    }

    // --- the new sigils (v0.21) ------------------------------------------------------------------------------------------

    private static void sigils(List<SNJournal.Entry> out) {
        out.add(entry("sigils_of_heaven", JournalChapter.SIGILS).order(8).icon(AllItems.HOLY_WATER)
                .unlock(sigilBook())
                .title("Sigils of Heaven")
                .text("Three Enochian effects are found where the choir once sang: Radiance and Halo in the temples of the Hymnal "
                        + "Spires, and in their vaults, one time in three, Judgement.")
                .image(glyph("radiance"), 32, 32, "Radiance")
                .text("Radiance (second tier) is a blinding flash: a little harm, two and a half times that on the unholy, three "
                        + "seconds of blindness and five of a glowing outline. A boss only takes the burn.")
                .image(glyph("halo"), 32, 32, "Halo")
                .text("Halo (second tier) is helpful: a ring of golden hearts that soaks up harm for fifteen seconds. It never "
                        + "touches a foe.")
                .image(glyph("judgement"), 32, 32, "Judgement")
                .text("Judgement is a Master's sigil and costs a holy water. It marks the ground under its target; a moment and a "
                        + "half later a column of light falls there and every foe within two and a half blocks takes 16 harm, half "
                        + "as much again if unholy. Whatever moved away in time is spared."));

        out.add(entry("sigils_of_hell", JournalChapter.SIGILS).order(9).icon(AllItems.SULFUR)
                .unlock(sigilBook())
                .title("Sigils of Hell")
                .text("Crowley's Corridors keep the Infernal pages: their chests hold Brimstone, Hellchain and, more rarely, Soul "
                        + "Rend.")
                .image(glyph("brimstone"), 32, 32, "Brimstone")
                .text("Brimstone (second tier) burns a pinch of sulfur and leaves a cloud of burning sulphur where it lands: five "
                        + "pulses of hellfire a second apart, on every foe within two blocks of that spot.")
                .image(glyph("hellchain"), 32, 32, "Hellchain")
                .text("Hellchain (second tier) is a chain of hellfire that burns the target and drags it to you. A boss is too "
                        + "heavy to drag: it only burns.")
                .image(glyph("soul_rend"), 32, 32, "Soul Rend")
                .text("Soul Rend (third tier) tears at the soul for 5 harm, and you drink half of what it actually does."));

        out.add(entry("sigils_of_the_craft", JournalChapter.SIGILS).order(10).icon(Items.AMETHYST_SHARD)
                .unlock(sigilBook())
                .title("Sigils of the Craft")
                .text("Witchcraft written as sigils. Old graves give up Hex and Pain; Glamour is shelved in the bunker library.")
                .image(glyph("hex"), 32, 32, "Hex")
                .text("Hex (first tier) lays a jinx for fifteen seconds. Bosses shrug it off, and a protection bag or a Ward of the "
                        + "Name turns it away.")
                .image(glyph("pain"), 32, 32, "Pain")
                .text("Pain (second tier) is harm that goes straight through armour, and a limp: Slowness for three seconds. A boss "
                        + "takes the harm and no more.")
                .image(glyph("glamour"), 32, 32, "Glamour")
                .text("Glamour (third tier) is helpful: you, or a friend, turn unseen for five seconds, and every creature hunting "
                        + "you within sixteen blocks sees an enemy in the nearest other creature instead, or loses the scent. Bosses "
                        + "are never fooled."));

        out.add(entry("sigils_of_the_hunt", JournalChapter.SIGILS).order(11).icon(AllItems.SALT)
                .unlock(sigilBook())
                .title("Sigils of the Hunt")
                .text("Plain hunter's work. Salt Shot turns up in graves; Silver Edge in the bunker library.")
                .image(glyph("salt_shot"), 32, 32, "Salt Shot")
                .text("Salt Shot (first tier) burns a salt: a blast of rock salt that scatters a ghost, breaks a possession and hits "
                        + "a demon two and a half times as hard, knocking it back. Anything else is only stung.")
                .image(glyph("silver_edge"), 32, 32, "Silver Edge")
                .text("Silver Edge (second tier) lays three charges on your hands for a minute: the next three blows of whatever "
                        + "you hold, even a bare fist, count as silver against werewolves and shapeshifters. Each blow that lands "
                        + "spends one."));

        out.add(entry("chain_and_glyph", JournalChapter.SIGILS).order(12).icon(AllItems.SIGIL_PAGE)
                .unlock(sigilBook())
                .title("Chain and Glyph")
                .text("Two forms of the second tier, both shelved in the bunker library.")
                .image(glyph("chain"), 32, 32, "Chain")
                .text("Chain throws a bolt of force at the first thing in your line, up to ten blocks, then leaps on to up to three "
                        + "more, each the nearest not yet struck within five blocks of the last and each a fifth weaker than the one "
                        + "before. A harmful spell leaps between foes, a helpful one between friends.")
                .image(glyph("glyph"), 32, 32, "Glyph")
                .text("Glyph plants a small sigil on the floor where you look (within six blocks; at your feet if you look at "
                        + "nothing). It lies quiet for thirty seconds until something hostile to you steps on it; then the spell goes "
                        + "off on everything standing on it, and the glyph is spent. You can keep three down at once: a fourth wipes "
                        + "out your oldest."));

        out.add(entry("linger_and_quicken", JournalChapter.SIGILS).order(13).icon(AllItems.SIGIL_PAGE)
                .unlock(sigilBook())
                .title("Linger and Quicken")
                .text("Two modifiers of the second tier from the bunker library. Each makes the spell cost half as much again.")
                .image(glyph("linger"), 32, 32, "Linger")
                .text("Linger leaves a zone where the spell first takes hold: for four seconds it repeats the spell every second at "
                        + "half strength on whatever stands in it. One zone per cast.")
                .image(glyph("quicken"), 32, 32, "Quicken")
                .text("Quicken lets your hand recover faster: the spell's wait, whichever of its sigils sets it, is cut by two "
                        + "fifths."));
    }

    // --- the bowl's witchcraft (v0.21) -------------------------------------------------------------------------------------

    private static void witchcraft(List<SNJournal.Entry> out) {
        out.add(entry("witch_curses", JournalChapter.BOWL).order(14).icon(Items.FERMENTED_SPIDER_EYE)
                .unlock(any(rites("curse_of_misfortune", "curse_of_pain", "curse_of_frailty", "hex_of_rot", "curse_of_silence"),
                        adv("first_targeted_spell")))
                .title("Curses")
                .text("A curse is aimed. Put something of your victim's in the bowl with the rest: a dose of their blood poured in "
                        + "beyond what the spell asks for, or a taglock of their hair. Without it the bowl will not catch. With it, "
                        + "the curse finds them anywhere in the world, so long as they are somewhere awake.")
                .text("It is refused by the great ones, every boss, and by other hunters unless the world allows them to fight each "
                        + "other. A Ward of the Name turns it away: the curse fails and you take a heart and a half for trying. A "
                        + "Mirror Hex sends it back on you, once.")
                .recipe(bowl("curse_of_misfortune"), "Misfortune: bad luck and a jinx, five minutes")
                .recipe(bowl("curse_of_pain"), "Pain: a minute of Agony")
                .recipe(bowl("curse_of_frailty"), "Frailty: weakness, slow hands, and a fifth more harm, two minutes")
                .recipe(bowl("hex_of_rot"), "Rot: hunger, a little poison, and the food in their pack rots, two minutes")
                .recipe(bowl("curse_of_silence"), "Silence: no sigils and no powers, a minute")
                .text("Agony hurts every two seconds but, like poison, never kills. Break Hex lifts any of them."));

        out.add(entry("witch_tracking", JournalChapter.BOWL).order(15).icon(Items.ENDER_EYE)
                .unlock(rites("locate_being", "scrying", "trace_the_familiar"))
                .title("Tracking")
                .text("Locate Being is the Locating spell for anyone at all, player or creature: water, a compass, string and salt, "
                        + "and something of theirs.")
                .recipe(bowl("locate_being"), "Locate Being")
                .text("Scrying shows you in the smoke where they are in this world: the land they stand in and roughly where, and "
                        + "it outlines them for thirty seconds for everyone to see. A Ward of the Name veils its bearer.")
                .recipe(bowl("scrying"), "Scrying")
                .text("Trace the Familiar needs nothing of anyone's: the smoke leads to the nearest witch, or a witch's bound "
                        + "familiar, within a hundred and sixty blocks.")
                .recipe(bowl("trace_the_familiar"), "Trace the Familiar"));

        out.add(entry("witch_control", JournalChapter.BOWL).order(16).icon(Items.CHAIN)
                .unlock(rites("binding_of_the_name", "banish_by_name", "compel", "break_hex"))
                .title("Binding by Name")
                .text("Binding of the Name roots whoever the bowl names for twenty seconds, if they are within two hundred and "
                        + "fifty-six blocks in this world. A boss is never held, though one or two can be checked for a breath.")
                .recipe(bowl("binding_of_the_name"), "Binding of the Name")
                .text("Banish by Name sends the named away as the banishing rites would: a ghost laid to rest, a demon or a hound "
                        + "cast back into the dark, a lesser angel sent home, a demon player expelled (it hurts, and burns their "
                        + "Corruption away), an angel player flung far. Never a boss, never a crossroads demon (only its death breaks "
                        + "a deal), never anything of this world.")
                .recipe(bowl("banish_by_name"), "Banish by Name")
                .text("Compel makes a creature, the named one or else the nearest within eight blocks of the bowl, follow you and "
                        + "spare you for a minute. A named player is drawn slowly toward the bowl for five seconds.")
                .recipe(bowl("compel"), "Compel")
                .text("Break Hex frees the named, or you, of every curse of the craft (jinx, bleeding, agony, rot, silence, marks, "
                        + "compulsion, bad luck) and burns the curse bags within eight blocks of the bowl and of them, and any they "
                        + "carry.")
                .recipe(bowl("break_hex"), "Break Hex"));

        out.add(entry("name_wards", JournalChapter.BOWL).order(17).icon(Items.GLOW_INK_SAC)
                .unlock(rites("ward_of_the_name", "mirror_hex"))
                .title("Wards of the Name")
                .text("Protection is laid on you, or on whoever a taglock in the bowl names. It needs no blood.")
                .recipe(bowl("ward_of_the_name"), "Ward of the Name: ten minutes")
                .text("Under a Ward of the Name every curse and binding aimed at you fails, and stings whoever spoke it; scrying "
                        + "shows them nothing; Hex and the curse bags slide off you.")
                .recipe(bowl("mirror_hex"), "Mirror Hex: twenty minutes, or until it breaks")
                .text("A Mirror Hex sends the next curse back to its caster and breaks. A caster under their own Ward shrugs off "
                        + "what comes back."));

        out.add(entry("familiar_bond", JournalChapter.BOWL).order(18).icon(Items.FEATHER)
                .unlock(any(rites("familiar_bond"), rites("trace_the_familiar")))
                .title("Familiar Bond")
                .text("Rowena's working. Name a cat or a parrot you have tamed, with a taglock of its fur or feathers, within "
                        + "sixty-four blocks of the bowl: it becomes your familiar, healed and steadied a while (Regeneration and "
                        + "Resistance for a minute).")
                .recipe(bowl("familiar_bond"), "Familiar Bond")
                .text("A familiar answers to Trace the Familiar. What else it can do for a witch is still being written."));

        out.add(entry("marks_of_the_craft", JournalChapter.BOWL).order(19).icon(Items.SPIDER_EYE)
                .unlock(any(rites("curse_of_misfortune", "curse_of_pain", "curse_of_frailty", "hex_of_rot", "curse_of_silence",
                        "ward_of_the_name", "mirror_hex", "compel"), adv("first_targeted_spell")))
                .title("Marks of the Craft")
                .text("What the witchcraft leaves on a body, as your own eyes will show it.")
                .text("Agony: a pulse of harm every two seconds that stops at half a heart. Rot: now and then a bite of food in "
                        + "your pack turns to rotten flesh. Silenced: no sigil will come and no power answers. Marked: everything "
                        + "hurts a fifth more. Compelled: you follow, or you are drawn.")
                .text("Name-Warded and Mirrored are the protections. Silver Edge counts down the silver blows you have left.")
                .items("What lifts a curse", AllItems.HOLY_WATER, AllItems.SALT, Items.CANDLE, Items.FLINT));

        out.add(entry("warded_rites", JournalChapter.RITUALS).order(5).icon(AllItems.RITUAL_ALTAR)
                .unlock(any(has(AllItems.RITUAL_ALTAR), adv("christo")))
                .title("A Ward over the Altar")
                .text("A circle broken mid-rite lashes out: thunder, a burn, and now and then something comes through. Cast a Ward "
                        + "of your own over the altar before you light it and the Ward takes the backlash: no burn, and nothing "
                        + "answers. The rite is still lost, and so are its offerings.")
                .image(glyph("ward"), 32, 32, "The Ward"));
    }
}
