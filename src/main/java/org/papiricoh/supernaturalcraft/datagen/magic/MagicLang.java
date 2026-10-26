package org.papiricoh.supernaturalcraft.datagen.magic;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The magic's text (v0.21): sound subtitles (read by {@code SNLanguageProvider} for {@code AllSounds.MAGIC_SOUNDS}) and the
 * schools' names. Called from {@code SNLang.addAll}.
 */
public final class MagicLang {

    private MagicLang() {
    }

    /** Subtitles of the magic's sounds, by event path. */
    public static final Map<String, String> SUBTITLES = Map.ofEntries(
            Map.entry("magic.enochian.cast", "An Enochian sigil rings out"),
            Map.entry("magic.enochian.impact", "Holy light strikes"),
            Map.entry("magic.enochian.charge", "A choir hums"),
            Map.entry("magic.enochian.complete", "Heaven answers the rite"),
            Map.entry("magic.infernal.cast", "An infernal sigil hisses"),
            Map.entry("magic.infernal.impact", "Hellfire bursts"),
            Map.entry("magic.infernal.charge", "Brimstone smoulders"),
            Map.entry("magic.infernal.complete", "Hell answers the rite"),
            Map.entry("magic.witchcraft.cast", "A hex is spoken"),
            Map.entry("magic.witchcraft.impact", "Witchcraft takes hold"),
            Map.entry("magic.witchcraft.charge", "Whispers gather"),
            Map.entry("magic.witchcraft.complete", "The spell is sealed"),
            Map.entry("magic.hunter.cast", "A hunter's sigil cracks"),
            Map.entry("magic.hunter.impact", "Salt and iron strike"),
            Map.entry("magic.hunter.charge", "Chalk lines hum"),
            Map.entry("magic.hunter.complete", "The rite holds"),
            Map.entry("bowl.boil", "The bowl boils"),
            Map.entry("bowl.seal", "Smoke draws a seal"),
            Map.entry("bowl.send", "The spell flies off"),
            Map.entry("ritual.rift", "A rift tears open"),
            Map.entry("ritual.implode", "The circle collapses"));

    public static void add(BiConsumer<String, String> add) {
        add.accept("school.supernaturalcraft.enochian", "Enochian");
        add.accept("school.supernaturalcraft.infernal", "Infernal");
        add.accept("school.supernaturalcraft.witchcraft", "Witchcraft");
        add.accept("school.supernaturalcraft.hunter", "Hunter");
        add.accept("school.supernaturalcraft.none", "No School");
        // Mastery (v0.21)
        add.accept("mastery.supernaturalcraft.none", "Novice");
        add.accept("mastery.supernaturalcraft.apprentice", "Apprentice");
        add.accept("mastery.supernaturalcraft.adept", "Adept");
        add.accept("mastery.supernaturalcraft.master", "Master");
        add.accept("message.supernaturalcraft.mastery.rank", "Your mastery grows: you are now %s in %s.");
        // The rites of the well.
        add.accept("well.supernaturalcraft.deepening", "Rite of Deepening");
        add.accept("well.supernaturalcraft.wellspring", "Rite of the Wellspring");
        add.accept("message.supernaturalcraft.well.no_one", "The well answers only someone standing at it.");
        add.accept("message.supernaturalcraft.well.wrong_tier", "Not this one: after %3$s rite(s), the well asks for %1$s %2$s.");
        add.accept("message.supernaturalcraft.well.resting", "The well is still settling: the next %s in %s day(s).");
        add.accept("message.supernaturalcraft.well.not_full", "The well takes all your mana: come to it full.");
        add.accept("message.supernaturalcraft.well.deepening", "Rite of Deepening %s: your mana runs to %s.");
        add.accept("message.supernaturalcraft.well.wellspring", "Rite of the Wellspring %s: your mana returns at %3$s a second.");
        add.accept("message.supernaturalcraft.well.milestone", "%s rites of the well: you are %s.");
        add.accept("well.supernaturalcraft.title.1", "Deepened");
        add.accept("well.supernaturalcraft.title.2", "Wellspring-born");
        add.accept("well.supernaturalcraft.title.3", "Fathomless");
        add.accept("well.supernaturalcraft.title.4", "Abyss-deep");
        add.accept("well.supernaturalcraft.title.5", "Boundless");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.deepen_well", "Takes your whole bar: +50 mana, or quicker mana, for good");
        add.accept("message.supernaturalcraft.page.needs_rank", "This page is beyond you. It needs %s rank in %s.");
        add.accept("message.supernaturalcraft.page.needs_rank_any", "This page is beyond you. It needs %s rank in any school.");
        // The bowl's target layer and its distillates (v0.21)
        add.accept("message.supernaturalcraft.taglock.snipped", "You snip a lock of %s's hair.");
        add.accept("message.supernaturalcraft.taglock.caught", "%s noticed!");
        add.accept("message.supernaturalcraft.taglock.noticed", "Something tugs at your hair. It was %s.");
        add.accept("tooltip.supernaturalcraft.taglock.blank", "Use on a creature to snip a lock of its hair (sneak-use: your own).");
        add.accept("tooltip.supernaturalcraft.taglock.of", "Hair of %s");
        add.accept("tooltip.supernaturalcraft.taglock.bowl", "In a spell bowl it names the spell's target.");
        add.accept("tooltip.supernaturalcraft.bound_blood.of", "Bound to %s");
        add.accept("tooltip.supernaturalcraft.bound_blood.unbound", "Bound to no one");
        add.accept("tooltip.supernaturalcraft.hexed_ink.school", "Steeped in %s");
        add.accept("tooltip.supernaturalcraft.hexed_ink.inert", "Inert: steeped in no school");
        add.accept("bowl_spell.supernaturalcraft.break_deal", "Break the Deal");
        add.accept("bowl_spell.supernaturalcraft.break_deal.desc", "Burns a crossroads contract and takes back what was sold.");
        witchcraft(add);
    }

    private static void spell(BiConsumer<String, String> add, String id, String name, String desc) {
        add.accept("bowl_spell.supernaturalcraft." + id, name);
        add.accept("bowl_spell.supernaturalcraft." + id + ".desc", desc);
    }

    /** The v0.21 bowl witchcraft: spells, their JEI lines, their messages; and the sigils' and rites' new messages. */
    private static void witchcraft(BiConsumer<String, String> add) {
        spell(add, "curse_of_misfortune", "Curse of Misfortune", "Bad luck dogs whoever the bowl names: worse loot, weaker blows, a long jinx.");
        spell(add, "curse_of_pain", "Curse of Pain", "Pain that comes and goes for a minute. Like poison, it never quite kills.");
        spell(add, "curse_of_frailty", "Curse of Frailty", "Strength drains away: weaker, slower to work, and every blow lands harder.");
        spell(add, "curse_of_silence", "Curse of Silence", "For a minute the named can neither cast a sigil nor call on their powers.");
        spell(add, "hex_of_rot", "Hex of Rot", "Hunger and sickness, and the food in their pack rots.");
        spell(add, "locate_being", "Locate Being", "The smoke drifts toward whoever the bowl names, person or creature.");
        spell(add, "scrying", "Scrying", "The smoke shows you where the named stands, and outlines them for a while.");
        spell(add, "trace_the_familiar", "Trace the Familiar", "The smoke leads to the nearest witch, or a witch's familiar.");
        spell(add, "binding_of_the_name", "Binding of the Name", "Roots the named to the spot, if they are within a few hundred blocks.");
        spell(add, "banish_by_name", "Banish by Name", "Sends the named away from afar: a ghost to rest, a demon to the dark, an angel home.");
        spell(add, "compel", "Compel", "A creature follows you and will not raise a hand to you. A person named is drawn to the bowl.");
        spell(add, "break_hex", "Break Hex", "Lifts every curse from the named (or from you) and burns the curse bags near both.");
        spell(add, "ward_of_the_name", "Ward of the Name", "For ten minutes, curses aimed at the named fail and turn on whoever cast them.");
        spell(add, "mirror_hex", "Mirror Hex", "The next curse aimed at the named goes back to its caster.");
        spell(add, "wellspring", "Rite of the Wellspring", "Takes all your mana and gives it back quicker for good: each rite adds 1% of your bar a second.");
        spell(add, "brew_chrism", "Brew Chrism", "Holy oil consecrated into chrism, which the great rites ask for.");
        spell(add, "brew_bound_blood", "Brew Bound Blood", "Binds the poured blood to its owner, in a vial the strongest rites ask for.");
        spell(add, "anoint_the_bone", "Anoint the Bone", "Washes the bone of a righteous mortal in the blood of the three: a fallen angel, a King of Hell and an Alpha. It is the only key to Dick Roman's door.");
        spell(add, "brew_hexed_ink", "Brew Hexed Ink", "Steeps Enochian ink in one school: gold for Enochian, blaze powder for Infernal, amethyst for Witchcraft, salt for the Hunters.");
        spell(add, "familiar_bond", "Familiar Bond", "Binds a cat or parrot you tamed to you as a witch's familiar.");
        // v0.21 (P6): the Mark's cure.
        spell(add, "unmark", "Unmark", "Takes the Mark of Cain off whoever the bound blood names. Read from the Book of the Damned, "
                + "by the witch who survived its keeper's choice, standing at the bowl.");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.curse", "Curses whoever the bowl names");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.bind_by_name", "Roots whoever the bowl names");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.banish_by_name", "Banishes whoever the bowl names");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.compel", "Bends a creature to your will");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.break_hex", "Lifts curses and burns curse bags");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.name_ward", "Guards a name against curses");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.scry", "Shows where the named stands");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.familiar_bond", "Makes a tamed pet your familiar");
        add.accept("jei.supernaturalcraft.bowl_spell.needs_target", "Needs something of the target's: blood, bound blood or a taglock");
        // Messages
        add.accept("message.supernaturalcraft.bowl.needs_target", "This spell needs something of its target in the bowl: blood, bound blood or a taglock.");
        add.accept("message.supernaturalcraft.witchcraft.lost", "The smoke cannot find %s.");
        add.accept("message.supernaturalcraft.witchcraft.elsewhere", "%s is in another world, beyond the smoke's reach.");
        add.accept("message.supernaturalcraft.witchcraft.too_far", "%s is too far away for the spell to take hold.");
        add.accept("message.supernaturalcraft.witchcraft.boss", "%s is far too strong for a bowl's curse.");
        add.accept("message.supernaturalcraft.witchcraft.no_pvp", "Your magic will not touch %s: this world forbids harming other hunters.");
        add.accept("message.supernaturalcraft.witchcraft.warded", "%s is warded. The curse turns back on you!");
        add.accept("message.supernaturalcraft.witchcraft.mirrored", "A mirror hex on %s throws the curse back at you!");
        add.accept("message.supernaturalcraft.witchcraft.of_this_world", "%s is of this world: there is nowhere to banish it to.");
        add.accept("message.supernaturalcraft.witchcraft.nothing_near", "There is nothing near the bowl to compel.");
        add.accept("message.supernaturalcraft.witchcraft.no_hex", "There is no curse on %s to break.");
        add.accept("message.supernaturalcraft.scrying.vision", "The smoke shows you %s, in the %s, near %s, %s, %s.");
        add.accept("message.supernaturalcraft.scrying.veiled", "The smoke parts around %s: something veils them.");
        add.accept("message.supernaturalcraft.brew.no_blood", "There is no one's blood in the bowl to bind.");
        add.accept("message.supernaturalcraft.brew.no_school", "Nothing in the bowl steeps the ink in a school.");
        add.accept("message.supernaturalcraft.familiar.not_yours", "%s is not a cat or parrot you tamed.");
        add.accept("message.supernaturalcraft.familiar.bound", "%s is bound to you now, your familiar.");
        add.accept("message.supernaturalcraft.cast.silenced", "A curse stills your tongue: no sigil will come.");
        add.accept("message.supernaturalcraft.ritual.backlash_warded", "The circle breaks, but your ward takes the backlash.");
    }
}
