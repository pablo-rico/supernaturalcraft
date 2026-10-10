package org.papiricoh.supernaturalcraft.datagen.heaven;

import org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss;
import org.papiricoh.supernaturalcraft.crossroads.DealTerms;
import org.papiricoh.supernaturalcraft.crossroads.DealTerms.Affliction;
import org.papiricoh.supernaturalcraft.crossroads.DealTerms.Wish;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The wild crossroads' text (owned by the crossroads work): wild wishes, the box, the bargain's screen.
 * <p>v0.18: created by the foundations, called from {@code SNLang.addAll}. Every English string of its owner goes here (a key may
 * only be added once across all lang classes).
 */
public final class CrossroadsWildLang {

    /** How each trophy reads on the offer and the contract. */
    private static final Map<Boss, String[]> TROPHIES = new EnumMap<>(Boss.class);

    static {
        trophy(Boss.AZAZEL, "Azazel's bust", "The Yellow-Eyed Demon, in stone, eyes and all");
        trophy(Boss.LILITH, "Lilith's bust", "The first demon, carved white and smiling");
        trophy(Boss.LUCIFER, "The Morningstar trophy", "The Devil's own likeness. He'd be flattered");
        trophy(Boss.GABRIEL, "Gabriel's bust", "The Trickster, mid-grin");
        trophy(Boss.WAR, "War's trophy", "The red rider, mounted on your wall");
        trophy(Boss.FAMINE, "Famine's trophy", "The hungry one, at last with nothing to eat");
        trophy(Boss.PESTILENCE, "Pestilence's trophy", "The sick one, wiped down. Mostly");
        trophy(Boss.RAPHAEL, "Raphael's bust", "The archangel of the storm, still crackling");
        trophy(Boss.BROKEN_CHORUS, "The Choir trophy", "What is left of the Broken Chorus, humming");
        trophy(Boss.METATRON, "Metatron's bust", "The Scribe of God, quill behind his ear");
        trophy(Boss.NAOMI, "Naomi's bust", "Heaven's reprogrammer, drill at rest");
        trophy(Boss.ZACHARIAH, "Zachariah's bust", "Heaven's middle manager, stamped and filed");
        trophy(Boss.AMARA, "The Eclipse trophy", "The Darkness, caught in a sliver of light");
        trophy(Boss.DEATH, "Death's trophy", "The pale rider. Don't look it in the eye");
        trophy(Boss.MICHAEL, "Michael's bust", "The Sword of Heaven, finally holding still");
    }

    private CrossroadsWildLang() {
    }

    private static void trophy(Boss boss, String name, String desc) {
        TROPHIES.put(boss, new String[]{name, desc});
    }

    public static void add(BiConsumer<String, String> add) {
        // --- The box and the soil ---------------------------------------------------------------------------------------------
        add.accept("tooltip.supernaturalcraft.crossroads_box",
                "Bury it at night in the trodden earth where two old roads cross. Someone will come.");
        add.accept("commands.supernaturalcraft.crossroads.placed", "A natural crossroads, its soil at %s (seed %s)");
        add.accept("message.supernaturalcraft.crossroads.wild.not_here", "This is no crossroads.");
        add.accept("message.supernaturalcraft.crossroads.wild.waiting", "Something is already on its way.");
        add.accept("message.supernaturalcraft.crossroads.wild.day", "The road is empty by day. Come back after dark.");
        add.accept("message.supernaturalcraft.crossroads.wild.once", "The crossroads answered you once tonight. Once is plenty.");
        add.accept("message.supernaturalcraft.crossroads.wild.buried", "You bury the box. The wind drops. Somewhere, a dog barks.");

        // --- The screen -------------------------------------------------------------------------------------------------------
        add.accept("screen.supernaturalcraft.deal.wild", "A Wild Bargain");
        add.accept("screen.supernaturalcraft.deal.wild.line.0", "\"A box in the dirt, the old way. I do love a traditionalist.\"");
        add.accept("screen.supernaturalcraft.deal.wild.line.1", "\"Out here the menu's better. The prices, well... they're out here too.\"");
        add.accept("screen.supernaturalcraft.deal.wild.line.2", "\"No bowl, no candles, no middleman. Just you, me and the dogs.\"");
        add.accept("screen.supernaturalcraft.deal.wild.line.3", "\"I've already ticked the little box for you. Saves time.\"");

        // --- Wild wishes ------------------------------------------------------------------------------------------------------
        add.accept(Wish.ASCEND.key(0), "A sharper edge");
        add.accept(Wish.ASCEND.key(0) + ".desc", "The weapon in your hand, one Ascension higher. No shard, no forge, no sweat.");
        for (int i = 0; i < DealTerms.TROPHY_BOSSES.size(); i++) {
            String[] t = TROPHIES.get(DealTerms.TROPHY_BOSSES.get(i));
            if (t == null) throw new IllegalStateException("No trophy text for " + DealTerms.TROPHY_BOSSES.get(i));
            add.accept(Wish.TROPHY.key(i), t[0]);
            add.accept(Wish.TROPHY.key(i) + ".desc", t[1] + ", and one of the shards it left. I keep a collection.");
        }
        add.accept(Wish.REVIVE.key(0), "Your best friend, back");
        add.accept(Wish.REVIVE.key(0) + ".desc", "Your last dead pet, on its feet. Even if there was nothing left of it. Especially then.");
        String[][] curses = {
                {"A sated blade", "Every hungry weapon you carry, fed to bursting. For now."},
                {"Heaven's Mark, wiped", "Heaven loses your number. Not my problem if they find it again."},
                {"A soul, refilled", "That hollow feeling? Gone. Temporarily filled with something of mine."},
                {"Luck, unjinxed", "Whoever hexed you, their bag is now a bag of nothing."}};
        for (Affliction a : Affliction.values()) {
            add.accept(Wish.UNCURSE.key(a.ordinal()), curses[a.ordinal()][0]);
            add.accept(Wish.UNCURSE.key(a.ordinal()) + ".desc", curses[a.ordinal()][1]);
        }
    }
}
