package org.papiricoh.supernaturalcraft.datagen.chuck;

import java.util.function.BiConsumer;

/**
 * The fourth wall in English: the fake credits he rolls in chapter 4, the real ones at the end, the names he gives the
 * hunter's things when he rewrites the HUD, the lines typed on his manuscript pages. Owned by the client-effects work.
 */
public final class FourthWallLang {

    private static final String K = "fourth_wall.supernaturalcraft.";

    /** {@code hud.item.<n>}: what he calls the item in your hand (as many as {@code ChuckHud.ITEM_NAMES}). */
    private static final String[] ITEM_NAMES = {
            "Plot Device", "Chekhov's Sword", "Something Shiny", "A Prop", "Red Herring", "Unnecessary Exposition",
            "Deus Ex Machina (Broken)", "Your Last Hope", "Filler", "Comic Relief", "Placeholder", "It Won't Help",
            "Cut Content", "Continuity Error", "Foreshadowing", "Fan Service"};

    /** {@code page.<n>}: typed on the floating manuscript pages (as many as {@code AuthorTargetRenderer.PAGE_LINES}). */
    private static final String[] PAGE_LINES = {
            "and the hunter came to the cabin", "and the hunter would not stop", "INT. THE IMPALA - NIGHT",
            "the hunter falls. (rewrite?)", "no. too easy. again.", "and then, against all odds,",
            "TO BE CONTINUED", "the end. the end. the end."};

    private FourthWallLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        add.accept(K + "the_end", "THE END");
        add.accept(K + "not_like_this", "...no. Not like this.");
        add.accept(K + "backspace", "the hunter was here five seconds ago");

        // The fake credits (chapter 4).
        add.accept(K + "fake.title", "SupernaturalCraft");
        add.accept(K + "fake.production", "A Chuck Shurley Production");
        fake(add, "directed_by", "Directed by", "god", "God");
        fake(add, "written_by", "Written by", "carver", "Carver Edlund");
        fake(add, "starring", "Starring", "chuck_as_himself", "Chuck Shurley as Himself");
        fake(add, "and", "and", "a_hunter", "A Hunter (uncredited)");
        fake(add, "lead_developer", "Lead Developer", "kevin", "Kevin Tran");
        fake(add, "texture_artist", "Texture Artist", "becky", "Becky Rosen");
        fake(add, "sound_design", "Sound Design", "gabriel", "Gabriel (\"the Trickster\")");
        fake(add, "quality_assurance", "Quality Assurance", "crowley", "Crowley (every bug marked \"won't fix\")");
        fake(add, "lore_consultant", "Lore Consultant", "metatron", "Metatron (uncredited, furious)");
        fake(add, "vehicle", "Vehicle", "impala", "1967 Chevrolet Impala");
        fake(add, "catering", "Catering", "biggersons", "Biggerson's");
        fake(add, "music", "Music", "wayward", "Something about a wayward son (licence pending)");
        add.accept(K + "fake.disclaimer", "No hunters were harmed in the making of this ending.");

        // The real credits (after the last page).
        add.accept(K + "credits.title", "SupernaturalCraft");
        add.accept(K + "credits.story", "The story of a hunter");
        add.accept(K + "credits.a_hunter", "A Hunter");
        add.accept(K + "credits.defeated", "Who they stopped");
        add.accept(K + "credits.and_the_author", "...and the Author");
        add.accept(K + "credits.allies", "Who stood with them at the end");
        add.accept(K + "credits.dean", "Dean Winchester");
        add.accept(K + "credits.sam", "Sam Winchester");
        add.accept(K + "credits.castiel", "Castiel");
        add.accept(K + "credits.written_by", "Written by");
        add.accept(K + "credits.chuck", "Chuck Shurley");
        add.accept(K + "credits.chuck_note", "(he insists)");
        add.accept(K + "credits.rewritten_by", "Rewritten by");
        add.accept(K + "credits.thanks", "For every hunter who never made it to the last page");
        add.accept(K + "credits.mod", "Thank you for playing.");
        add.accept(K + "credits.carry_on", "Carry on.");

        // The rewritten HUD.
        for (int i = 0; i < ITEM_NAMES.length; i++) add.accept(K + "hud.item." + i, ITEM_NAMES[i]);
        add.accept(K + "hud.empty", "Nothing (fitting)");

        // The manuscript pages.
        for (int i = 0; i < PAGE_LINES.length; i++) add.accept(K + "page." + i, PAGE_LINES[i]);
    }

    private static void fake(BiConsumer<String, String> add, String role, String roleText, String who, String whoText) {
        add.accept(K + "fake." + role, roleText);
        add.accept(K + "fake." + who, whoText);
    }
}
