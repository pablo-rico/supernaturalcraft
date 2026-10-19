package org.papiricoh.supernaturalcraft.datagen.abaddon;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Abaddon's text (v0.21, P5): sound subtitles (read by {@code SNLanguageProvider} for {@code AllSounds.ABADDON_SOUNDS}), her bar
 * and title cards, messages, tooltips, the rite's effect, advancements, the crossroads demon's word on her and the greetings
 * of the sworn. Called from {@code SNLang.addAll}.
 */
public final class AbaddonLang {

    private AbaddonLang() {
    }

    public static final Map<String, String> SUBTITLES = Map.ofEntries(
            Map.entry("abaddon.ambient", "Abaddon hums to herself"),
            Map.entry("abaddon.laugh", "Abaddon laughs"),
            Map.entry("abaddon.hurt", "Abaddon is struck"),
            Map.entry("abaddon.death", "The Knight of Hell burns"),
            Map.entry("abaddon.sweep", "Hellfire sweeps"),
            Map.entry("abaddon.smoke", "Black smoke pours out"),
            Map.entry("abaddon.possess", "Smoke forces its way in"),
            Map.entry("abaddon.pin", "An unseen grip closes"),
            Map.entry("abaddon.brand", "The Brand of the Knights burns"),
            Map.entry("abaddon.trapped", "A devil's trap holds"),
            Map.entry("abaddon.knight_ambient", "A knight's plate grinds"),
            Map.entry("abaddon.knight_hurt", "A knight is struck"),
            Map.entry("abaddon.knight_death", "A knight crumbles"),
            Map.entry("abaddon.herald_mark", "A herald's bell tolls"));

    public static void add(BiConsumer<String, String> add) {
        // --- Her bar, title cards, cinematics ------------------------------------------------------------------------------------
        add.accept("entity.supernaturalcraft.abaddon.bar.phase1", "Abaddon, Knight of Hell");
        add.accept("entity.supernaturalcraft.abaddon.bar.phase2", "Abaddon, in Another's Skin");
        add.accept("entity.supernaturalcraft.abaddon.bar.phase3", "Abaddon, the Brand of the Knights");
        add.accept("title.supernaturalcraft.abaddon.phase1", "Knight of Hell");
        add.accept("title.supernaturalcraft.abaddon.phase1.sub", "The last of the Knights. She has waited a very long time.");
        add.accept("title.supernaturalcraft.abaddon.phase2", "Possession");
        add.accept("title.supernaturalcraft.abaddon.phase2.sub", "Her vessel is only a coat. She can wear another.");
        add.accept("title.supernaturalcraft.abaddon.phase3", "The Brand of the Knights");
        add.accept("title.supernaturalcraft.abaddon.phase3.sub", "The seal Cain gave them burns in her palm");
        add.accept("title.supernaturalcraft.abaddon.death", "King of the Damned");
        add.accept("title.supernaturalcraft.abaddon.death.sub", "The last Knight of Hell burns from the inside out");
        add.accept("cinematic.supernaturalcraft.abaddon.victory.title", "King of the Damned");
        add.accept("cinematic.supernaturalcraft.abaddon.victory.subtitle", "Her Book is yours now. So is what it costs.");

        // --- Messages ----------------------------------------------------------------------------------------------------------
        String m = "message.supernaturalcraft.abaddon.";
        add.accept(m + "busy", "Another fight already holds this world: the smoke sinks back into the ground.");
        add.accept(m + "cage_gone", "The fight is over. Abaddon is smoke again, and the smoke is gone.");
        add.accept(m + "victorious", "Abaddon: \"Run along, little hunter. I'll find you when I'm bored.\"");
        add.accept(m + "trapped", "The devil's trap holds her! Every blow lands in full.");
        add.accept(m + "expelled", "You drive Abaddon out of her knight!");
        add.accept(m + "cast_out", "Abaddon is thrown back into her own body, reeling!");
        add.accept("message.supernaturalcraft.book_of_the_damned.read",
                "The pages are skin, the ink is blood. Somewhere in here is the spell that undoes the Mark of Cain — "
                        + "but only a witch could read it.");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.summon_abaddon", "Calls up Abaddon, Knight of Hell");

        // --- Tooltips ----------------------------------------------------------------------------------------------------------
        add.accept("tooltip.supernaturalcraft.book_of_the_damned", "Bound in skin, written in blood. The darkest spells ever set down.");
        add.accept("tooltip.supernaturalcraft.book_of_the_damned.mark", "It holds the cure for the Mark of Cain — for a witch who can read it.");
        add.accept("tooltip.supernaturalcraft.weapon.knights_cleaver",
                "Abaddon's own blade. Use it to send a wave of hellfire down the ground before you.");

        // --- Advancements ------------------------------------------------------------------------------------------------------
        adv(add, "king_of_the_damned", "King of the Damned", "Kill Abaddon, the last Knight of Hell");
        adv(add, "book_of_the_damned", "The Book of the Damned", "Read the Book of the Damned");

        // --- The crossroads demon's Knowledge ------------------------------------------------------------------------------------
        add.accept("book.supernaturalcraft.crossroads.abaddon.name", "Abaddon, Knight of Hell");
        add.accept("book.supernaturalcraft.crossroads.abaddon.weakness",
                "She shrugs off most blades. The First Blade bites her, and so does any hunter wearing the Mark. A devil's trap "
                        + "holds her a while; when she hides in a knight, exorcise her out, or salt the vessel.");

        // --- The sworn are greeted (BossTwists) ----------------------------------------------------------------------------------
        String g = "message.supernaturalcraft.allegiance.boss.abaddon.";
        add.accept(g + "angel", "Abaddon: \"Oh, a feathered one. I used to eat your kind for breakfast.\"");
        add.accept(g + "demon", "Abaddon: \"Kneel, soldier. Hell has a queen again.\"");
        add.accept(g + "human", "Abaddon: \"Another Winchester wannabe. Adorable.\"");
    }

    private static void adv(BiConsumer<String, String> add, String id, String title, String desc) {
        add.accept("advancement.supernaturalcraft." + id, title);
        add.accept("advancement.supernaturalcraft." + id + ".desc", desc);
    }
}
