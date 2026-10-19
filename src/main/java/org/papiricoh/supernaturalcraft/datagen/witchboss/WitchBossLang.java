package org.papiricoh.supernaturalcraft.datagen.witchboss;

import org.papiricoh.supernaturalcraft.coven.ending.GrandWitchDialogue;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The witch bosses' and the ending's text (v0.21, P6): Rowena's and the Grand Witch's bars and title cards, messages, the Grand
 * Witch's words at peace ({@link GrandWitchDialogue}), tooltips, the rite's and the bowl spell's lines, advancements, the
 * crossroads demon's word on them and the greetings of the sworn. Their sound subtitles are {@code CovenLang.SUBTITLES}; Rowena's
 * own new lines are in {@code CovenLang}. Called from {@code SNLang.addAll}.
 */
public final class WitchBossLang {

    private WitchBossLang() {
    }

    /** The Grand Witch's lines at peace, by node. */
    static final Map<String, String> GRAND_WITCH_LINES = Map.of(
            GrandWitchDialogue.HELLO, "So. The redhead is finally dead, and by a hunter's hand. The Grand Coven pays its debts: show me "
                    + "that Book, give me your blood, and I'll burn that ugly Mark off your arm.",
            GrandWitchDialogue.STRANGER, "I have no business with you, hunter. Walk away while I still find you dull.",
            GrandWitchDialogue.UNMARKED, "It is done. The Book is still yours — keep it close, and keep it away from anyone like her.",
            GrandWitchDialogue.NEEDS_BLOOD, "The cure must know whose Mark it unmakes. A vial of your own bound blood — the bowl can make "
                    + "it, if you know how.",
            GrandWitchDialogue.NEEDS_BOOK, "And read it from what, my memory? Bring the Book of the Damned. You kept it; carry it.",
            GrandWitchDialogue.NOT_MARKED, "You bear no Mark. The Coven's debt is paid — for now.");

    public static void add(BiConsumer<String, String> add) {
        // --- Rowena ------------------------------------------------------------------------------------------------------------
        add.accept("entity.supernaturalcraft.rowena.bar.phase1", "Rowena, at Home");
        add.accept("entity.supernaturalcraft.rowena.bar.phase2", "Rowena, the Familiar and the Hexed");
        add.accept("entity.supernaturalcraft.rowena.bar.phase3", "Rowena, the Recitation");
        add.accept("title.supernaturalcraft.rowena.phase1", "Rowena MacLeod");
        add.accept("title.supernaturalcraft.rowena.phase1.sub", "Three hundred years of spite, in her own house");
        add.accept("title.supernaturalcraft.rowena.phase2", "The Familiar and the Hexed");
        add.accept("title.supernaturalcraft.rowena.phase2.sub", "Kill the bird. Free the poor souls — don't kill them.");
        add.accept("title.supernaturalcraft.rowena.phase3", "The Recitation");
        add.accept("title.supernaturalcraft.rowena.phase3.sub", "Break the bowls before she finishes the spell");
        add.accept("title.supernaturalcraft.rowena.death", "The Witch Is Dead");
        add.accept("title.supernaturalcraft.rowena.death.sub", "For now. She always did have a way of coming back.");
        add.accept("cinematic.supernaturalcraft.rowena.victory.title", "The Witch Is Dead");
        add.accept("cinematic.supernaturalcraft.rowena.victory.subtitle", "The Book is yours. The Grand Coven will want a word.");
        String r = "message.supernaturalcraft.rowena.";
        add.accept(r + "cage_gone", "The fight is over. Rowena laughs from somewhere, and the smoke is gone.");
        add.accept(r + "victorious", "Rowena: \"Off you pop, darling. Do come back when you've learnt some manners.\"");
        add.accept(r + "victim_killed", "The hexed soul dies — and its life flows into Rowena!");
        add.accept(r + "victim_freed", "The hex breaks and rebounds on Rowena!");
        add.accept(r + "recitation", "Rowena begins to recite over her bowls. Break them!");
        add.accept(r + "recitation_broken", "The last bowl shatters — Rowena falls out of the air!");
        add.accept(r + "challenge.not_kept", "The pages are silent. This Book answers only the one who kept it from her.");
        add.accept(r + "challenge.not_here", "Read it in her house or her garden, where she can hear it.");
        add.accept(r + "challenge.not_night", "The house is quiet. She only answers after dark.");
        add.accept(r + "challenge.busy", "Another fight already holds this world: the house stays quiet.");

        // --- The Grand Witch ---------------------------------------------------------------------------------------------------
        add.accept("entity.supernaturalcraft.grand_witch.bar.phase1", "The Grand Witch, Shielded by her Coven");
        add.accept("entity.supernaturalcraft.grand_witch.bar.phase2", "The Grand Witch, Matriarch of the Coven");
        add.accept("entity.supernaturalcraft.grand_witch.bar.phase3", "The Grand Witch, Every Taglock at Once");
        add.accept("title.supernaturalcraft.grand_witch.phase1", "The Grand Witch");
        add.accept("title.supernaturalcraft.grand_witch.phase1.sub", "Three sisters channel into her. Break the channel.");
        add.accept("title.supernaturalcraft.grand_witch.phase2", "The Matriarch's Hexes");
        add.accept("title.supernaturalcraft.grand_witch.phase2.sub", "Frost, fire and an unseen fist");
        add.accept("title.supernaturalcraft.grand_witch.phase3", "The Mass Taglock");
        add.accept("title.supernaturalcraft.grand_witch.phase3.sub", "She holds a lock of everyone's hair. Stand in Rowena's circles.");
        add.accept("title.supernaturalcraft.grand_witch.death", "The Grand Coven Falls");
        add.accept("title.supernaturalcraft.grand_witch.death.sub", "Rowena watches, and for once she doesn't gloat");
        add.accept("cinematic.supernaturalcraft.grand_witch.victory.title", "The Grand Coven Falls");
        add.accept("cinematic.supernaturalcraft.grand_witch.victory.subtitle", "Rowena keeps her Book. Now she keeps her word.");
        String g = "message.supernaturalcraft.grand_witch.";
        add.accept(g + "busy", "Another fight already holds this world: the Coven will not come.");
        add.accept(g + "not_yours", "The Grand Coven does not answer you: only one who gave Rowena the Book can call them.");
        add.accept(g + "cage_gone", "The fight is over. The Grand Coven melts back into the smoke.");
        add.accept(g + "victorious", "The Grand Witch: \"Tell Rowena we'll be seeing her.\"");
        add.accept(g + "shield_down", "Her coven's channel breaks: the Grand Witch's shield is down!");
        add.accept(g + "rowena_arrives", "Rowena steps out of the smoke: \"Did you think I'd miss this?\"");
        add.accept(g + "rowena_thanks", "Rowena: \"Come to my house, darling. I'll take that Mark off you — a deal's a deal.\"");
        add.accept(g + "arrives", "Yellow smoke gathers, and the Grand Witch steps out of it. She has come for you.");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.call_the_grand_coven", "Calls the Grand Coven: the Grand Witch and her sisters");
        // Her words at peace.
        for (String node : GrandWitchDialogue.NODES) {
            String key = "dialogue.supernaturalcraft." + GrandWitchDialogue.ID + "." + node;
            add.accept(key, GRAND_WITCH_LINES.get(node));
            for (String option : GrandWitchDialogue.allOptions(node)) {
                add.accept(key + "." + option, option.equals(GrandWitchDialogue.OPT_UNMARK) ? "Take the Mark off me." : "Goodbye.");
            }
        }

        // --- The Book, the choice and the cure ----------------------------------------------------------------------------------
        add.accept("message.supernaturalcraft.book_choice.given", "You give Rowena the Book of the Damned. The Grand Coven will come for it.");
        add.accept("message.supernaturalcraft.book_choice.kept", "You keep the Book of the Damned. Rowena will not forgive it.");
        add.accept("message.supernaturalcraft.ritual.needs_mark", "This rite asks for the Mark of Cain on the one who performs it.");
        add.accept("message.supernaturalcraft.book_of_the_damned.coven_waits", "The Grand Witch will meet you in Rowena's house. Read the Book there.");
        String u = "message.supernaturalcraft.unmark.";
        add.accept(u + "not_marked", "There is no Mark to take off.");
        add.accept(u + "no_survivor", "Only the witch who survived your choice can read this cure.");
        add.accept(u + "no_survivor_near", "The witch who must read the cure is not here.");
        add.accept(u + "needs_book", "The cure is read from the Book of the Damned: it must be at hand.");
        add.accept(u + "needs_blood", "The cure needs a vial of the Marked's own bound blood.");
        add.accept(u + "done", "The Mark of Cain burns one last time — and is gone.");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.unmark", "Takes the Mark of Cain off the named");

        // --- Spoils --------------------------------------------------------------------------------------------------------------
        add.accept("tooltip.supernaturalcraft.rowenas_grimoire", "Rowena's own. It remembers the last curse you spoke at a bowl.");
        add.accept("tooltip.supernaturalcraft.rowenas_grimoire.empty", "Speak an aimed bowl spell while you carry it");
        add.accept("tooltip.supernaturalcraft.rowenas_grimoire.echo", "Remembers: %s");
        add.accept("tooltip.supernaturalcraft.weapon.rowenas_grimoire",
                "Use it to speak the remembered spell again without a bowl (once a minute); a taglock in the other hand names a new target.");
        add.accept("tooltip.supernaturalcraft.weapon.coven_staff",
                "Witchcraft sigils cost less and strike harder. Use it to lay the Coven's hex on whatever it points at.");
        add.accept("message.supernaturalcraft.rowenas_grimoire.empty", "The pages are blank: speak an aimed spell at a bowl first.");
        add.accept("message.supernaturalcraft.rowenas_grimoire.forgotten", "The words have faded from its pages.");

        // --- Advancements --------------------------------------------------------------------------------------------------------
        adv(add, "gave_the_book", "A Witch's Bargain", "Give Rowena the Book of the Damned");
        adv(add, "kept_the_book", "Finders Keepers", "Keep the Book of the Damned from Rowena");
        adv(add, "rowena_defeated", "Ding Dong", "Kill Rowena MacLeod in her own house");
        adv(add, "grand_witch_defeated", "The Grand Coven", "Defeat the Grand Witch at Rowena's side");
        adv(add, "unmarked", "Clean Slate", "Have the Mark of Cain taken off you");

        // --- The crossroads demon's Knowledge -------------------------------------------------------------------------------------
        add.accept("book.supernaturalcraft.crossroads.rowena.name", "Rowena MacLeod");
        add.accept("book.supernaturalcraft.crossroads.rowena.weakness",
                "Kill her raven and her curses lose your scent. Free her hexed puppets with Break Hex or salt — killing them feeds "
                        + "her. When she floats and recites, smash her three bowls and she drops like a stone.");
        add.accept("book.supernaturalcraft.crossroads.grand_witch.name", "The Grand Witch");
        add.accept("book.supernaturalcraft.crossroads.grand_witch.weakness",
                "Her shield is her sisters: crack their own shields and the channel breaks. When she takes everyone's taglock at "
                        + "once, stand where Rowena tells you.");

        // --- The sworn are greeted (BossTwists) ----------------------------------------------------------------------------------
        add.accept("message.supernaturalcraft.allegiance.boss.rowena.angel", "Rowena: \"A halo in my house? Wipe your wings.\"");
        add.accept("message.supernaturalcraft.allegiance.boss.rowena.demon", "Rowena: \"One of Hell's errand boys. I've hexed better.\"");
        add.accept("message.supernaturalcraft.allegiance.boss.rowena.human", "Rowena: \"You should have taken the deal, darling.\"");
        add.accept("message.supernaturalcraft.allegiance.boss.grand_witch.angel", "The Grand Witch: \"Heaven sends its pets now? How quaint.\"");
        add.accept("message.supernaturalcraft.allegiance.boss.grand_witch.demon", "The Grand Witch: \"The Coven does not bargain with demons.\"");
        add.accept("message.supernaturalcraft.allegiance.boss.grand_witch.human", "The Grand Witch: \"Rowena's little hunter. How touching.\"");
    }

    private static void adv(BiConsumer<String, String> add, String id, String title, String desc) {
        add.accept("advancement.supernaturalcraft." + id, title);
        add.accept("advancement.supernaturalcraft." + id + ".desc", desc);
    }
}
