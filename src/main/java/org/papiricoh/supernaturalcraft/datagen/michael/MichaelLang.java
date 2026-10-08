package org.papiricoh.supernaturalcraft.datagen.michael;

import java.util.function.BiConsumer;

/**
 * The Archangel Michael in English: his celestial boss bar and title cards, what he says and does to you ("I need your
 * yes"), death messages, the ritual, advancements, the crossroads demon's page on him, his rewards. Names of entities and
 * items come from their ids (SNLanguageProvider, NAMES).
 */
public final class MichaelLang {

    /** How many of his lines there are ({@code message.supernaturalcraft.michael.quote.<n>}); the later ones after Uncaged. */
    public static final int QUOTES = 8, QUOTES_AFTER_UNCAGED = 4;

    private MichaelLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        String[] bars = {"Michael, in his Vessel", "Michael, General of Heaven", "Michael, the Lance", "Michael, on the Wings of Heaven",
                "Michael the Archangel", "Michael, the Sword of Heaven"};
        for (int i = 0; i < bars.length; i++) add.accept("entity.supernaturalcraft.michael.bar.phase" + (i + 1), bars[i]);
        String[][] titles = {
                {"The Vessel", "\"I need your yes.\""},
                {"The General", "The Host of Heaven takes the field."},
                {"The Lance", "His shadow spreads its wings."},
                {"The Wings", "He takes to Heaven's sky."},
                {"The Archangel", "The vessel burns away. The Sword of Heaven stands."},
                {"The Sword of Heaven", "His halo breaks. He will not yield."}};
        for (int i = 0; i < titles.length; i++) {
            add.accept("title.supernaturalcraft.michael.phase" + (i + 1), titles[i][0]);
            add.accept("title.supernaturalcraft.michael.phase" + (i + 1) + ".sub", titles[i][1]);
        }
        add.accept("title.supernaturalcraft.michael.death", "The Sword Falls");
        add.accept("title.supernaturalcraft.michael.death.sub", "\"I was a good son. I did everything He asked.\"");
        add.accept("title.supernaturalcraft.michael.victory", "Heaven Is Silent");
        add.accept("title.supernaturalcraft.michael.victory.sub", "His lance, his grace, and a piece of his armour are yours.");
        add.accept("title.supernaturalcraft.michael.enochian", "MICHAEL");

        add.accept("message.supernaturalcraft.michael.busy", "Another fight already holds this world.");
        add.accept("message.supernaturalcraft.michael.victorious", "\"This was always going to end one way.\" Heaven closes over him.");
        add.accept("message.supernaturalcraft.michael.cage_gone", "The light fades. He has gone back to Heaven.");
        add.accept("message.supernaturalcraft.michael.relics", "The Angel Tablet and the Seraph Wings fall back to earth.");

        add.accept("screen.supernaturalcraft.michael.ask", "Michael needs your yes");
        add.accept("screen.supernaturalcraft.michael.ask.body", "\"Let me in. You'll feel what I feel: the whole of Heaven, behind one sword.\"");
        add.accept("screen.supernaturalcraft.michael.yes", "Yes");
        add.accept("screen.supernaturalcraft.michael.no", "No");
        add.accept("message.supernaturalcraft.michael.asked", "Michael's voice fills your head: \"I need your yes.\"");
        add.accept("message.supernaturalcraft.michael.possessed", "You said yes. He is wearing you.");
        add.accept("message.supernaturalcraft.michael.released", "He lets you go, and leaves a little of his grace behind.");
        add.accept("message.supernaturalcraft.michael.refused", "You said no. The Host turns its blades on you.");
        add.accept("message.supernaturalcraft.michael.parry", "You turned his hand aside: he's open!");
        add.accept("message.supernaturalcraft.michael.touch_broken", "You broke his reach.");
        add.accept("message.supernaturalcraft.michael.captain_falls", "The captain falls. The Host breaks ranks!");
        add.accept("message.supernaturalcraft.michael.lance_free", "His lance stands in the ground. Pull it out!");
        add.accept("message.supernaturalcraft.michael.lance_gone", "The lance tears itself from your hands and flies back to him.");

        String[] quotes = {
                "\"I need your yes.\"",
                "\"I'm a good son. I follow orders.\"",
                "\"You think you're the first to stand against Heaven?\"",
                "\"I don't hate you. I don't feel anything about you at all.\"",
                "\"Lucifer is my brother. And I love him.\"",
                "\"Then I'll do it without your yes.\"",
                "\"Our Father gave the order. I am the order.\"",
                "\"Stop. It's over. It was always over.\""};
        for (int i = 0; i < quotes.length; i++) add.accept("message.supernaturalcraft.michael.quote." + i, quotes[i]);
        String[] afterUncaged = {
                "\"You put my brother back in the box. Do you know what that cost me?\"",
                "\"I was meant to be the one to end him. You took that from me.\"",
                "\"Lucifer always found someone else to do his fighting.\"",
                "\"He's back in the Cage. Now it's just you and me.\""};
        for (int i = 0; i < afterUncaged.length; i++) add.accept("message.supernaturalcraft.michael.quote.uncaged." + i, afterUncaged[i]);

        add.accept("death.attack.supernaturalcraft.lance", "%1$s was run through by the Lance of Michael");
        add.accept("death.attack.supernaturalcraft.lance.player", "%1$s was run through by %2$s's lance");
        add.accept("death.attack.supernaturalcraft.steel_feather", "%1$s was cut down by steel feathers");
        add.accept("death.attack.supernaturalcraft.steel_feather.player", "%1$s was cut down by %2$s's steel feathers");

        add.accept("tooltip.supernaturalcraft.michael_lance", "Throw it: it comes back. Twice as hard on angels and demons.");
        add.accept("tooltip.supernaturalcraft.borrowed_lance", "It burns in your hand. Throw it back at him!");
        add.accept("tooltip.supernaturalcraft.michaels_grace", "Wear the Seraph Wings and take it in: they will carry you.");
        add.accept("tooltip.supernaturalcraft.general_armor", "Michael's own. The whole set wards off holy harm and raises a wing of light.");
        add.accept("hud.supernaturalcraft.flight", "Wings");
        add.accept("message.supernaturalcraft.michael.grace.absorbed", "Michael's grace settles in you. Wear the Seraph Wings, and they will carry you.");
        add.accept("message.supernaturalcraft.michael.grace.already", "His grace is already in you.");
        add.accept("message.supernaturalcraft.michael.ward", "A wing of light takes the blow.");

        add.accept("advancement.supernaturalcraft.sword_of_heaven", "The Sword of Heaven");
        add.accept("advancement.supernaturalcraft.sword_of_heaven.desc", "Defeat the Archangel Michael");
        add.accept("advancement.supernaturalcraft.wings_of_heaven", "Wings of Heaven");
        add.accept("advancement.supernaturalcraft.wings_of_heaven.desc", "Fly on the Seraph Wings with Michael's Grace");
        add.accept("advancement.supernaturalcraft.general", "General of Heaven");
        add.accept("advancement.supernaturalcraft.general.desc", "Wear all four pieces of the General's armour");

        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.summon_michael", "Calls down Michael, the Sword of Heaven (gives back the relics either way)");
        add.accept("jei.supernaturalcraft.info.michael", "Michael, the Sword of Heaven, is called down by day in a great circle in the "
                + "Overworld, once Metatron has fallen: offer the Angel Tablet, the Seraph Wings, three choir shards and two holy waters, "
                + "and wake it with an angel blade. He gives the relics back, win or lose.");
    }
}
