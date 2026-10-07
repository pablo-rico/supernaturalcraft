package org.papiricoh.supernaturalcraft.datagen.horsemen;

import java.util.function.BiConsumer;

/**
 * The Four Horsemen in English: boss bars, cinematics, what they say and do to you, death messages, rituals,
 * advancements, the antidote, the death clock. Names of entities and items come from their ids (SNLanguageProvider).
 */
public final class HorsemenLang {

    private HorsemenLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        war(add);
        famine(add);
        pestilence(add);
        death(add);
        shared(add);
    }

    private static void bars(BiConsumer<String, String> add, String id, String... names) {
        for (int i = 0; i < names.length; i++) add.accept("entity.supernaturalcraft." + id + ".phase" + (i + 1), names[i]);
    }

    private static void cinematic(BiConsumer<String, String> add, String id, String what, String title, String subtitle) {
        String k = "cinematic.supernaturalcraft." + id + (what.isEmpty() ? "" : "." + what);
        if (title != null) add.accept(k + ".title", title);
        add.accept(k + ".subtitle", subtitle);
    }

    private static void messages(BiConsumer<String, String> add, String id, String victorious, String gone) {
        add.accept("message.supernaturalcraft." + id + ".busy", "Another fight already holds this world.");
        add.accept("message.supernaturalcraft." + id + ".victorious", victorious);
        add.accept("message.supernaturalcraft." + id + ".cage_gone", gone);
    }

    private static void adv(BiConsumer<String, String> add, String id, String title, String desc) {
        add.accept("advancement.supernaturalcraft." + id, title);
        add.accept("advancement.supernaturalcraft." + id + ".desc", desc);
    }

    private static void death(BiConsumer<String, String> add, String type, String plain, String byPlayer) {
        add.accept("death.attack.supernaturalcraft." + type, plain);
        add.accept("death.attack.supernaturalcraft." + type + ".player", byPlayer);
    }

    static void war(BiConsumer<String, String> add) {
        bars(add, "war", "War, the Red Rider", "War, Fury of Nations", "War, on the Red Horse");
        cinematic(add, "war", "", "WAR", "\"I'm War. And this town's got a little problem with its neighbours.\"");
        cinematic(add, "war", "phase2", "FURY", "Break his standards. Under his spell, look for the ones who kneel.");
        cinematic(add, "war", "mount", "THE RED HORSE", "\"Look at you. You can't even get your own people to stop fighting.\"");
        cinematic(add, "war", "death", null, "He looks at his bare hand, where the ring was.");
        cinematic(add, "war", "victory", "THE RING OF WAR", "Dean's way: take the ring, and the war goes with it.");
        messages(add, "war", "\"Give it time. You'll do my work for me.\" He rides off the field.", "The field falls quiet. He is gone.");
        add.accept("message.supernaturalcraft.war.parry", "You turned his blade aside: he's open!");
        add.accept("message.supernaturalcraft.war.marked", "War whispers in your ear. Everyone around you is a demon now...");
        add.accept("message.supernaturalcraft.war.innocent", "That was no demon. The blow comes back on you.");
        add.accept("message.supernaturalcraft.war.betrayal", "You struck a friend. War laughs.");
        add.accept("message.supernaturalcraft.war.standard", "His standard falls; his fury cools.");
        add.accept("hud.supernaturalcraft.war.fury", "Fury");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.summon_war", "Calls up War, the Red Rider");
        adv(add, "war", "Red Rider", "Defeat War and take his ring");
    }

    static void famine(BiConsumer<String, String> add) {
        bars(add, "famine", "Famine, the Black Rider", "Famine, Devourer of Souls", "Famine, on the Black Horse");
        cinematic(add, "famine", "", "FAMINE", "\"I'm Famine. I'm always hungry.\" Don't eat near him.");
        cinematic(add, "famine", "phase2", "THE DEVOURER", "His thralls come to be eaten. Kill them on the way.");
        cinematic(add, "famine", "mount", "THE BLACK HORSE", "\"You're empty inside, hunter. Let me fill up on you.\"");
        cinematic(add, "famine", "death", null, "\"So... hungry...\"");
        cinematic(add, "famine", "victory", "THE RING OF FAMINE", "His hunger finally eats him.");
        messages(add, "famine", "\"Still hungry.\" He wheels himself away.", "The dead field is quiet. He is gone.");
        add.accept("message.supernaturalcraft.famine.fed", "You ate near Famine, and he grew stronger.");
        add.accept("message.supernaturalcraft.famine.devoured", "Famine drinks a soul and heals.");
        add.accept("message.supernaturalcraft.famine.grabbed", "Famine has you! Hit him, all of you, to break his grip.");
        add.accept("message.supernaturalcraft.famine.grabbed_solo", "Famine has you! Hold on: he lets go when you're nearly empty.");
        add.accept("message.supernaturalcraft.famine.released", "His grip breaks.");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.summon_famine", "Calls up Famine, the Black Rider");
        adv(add, "famine", "Always Hungry", "Defeat Famine and take his ring");
        death(add, "starved", "%1$s was eaten away", "%1$s was fed on by %2$s");
    }

    static void pestilence(BiConsumer<String, String> add) {
        bars(add, "pestilence", "Pestilence, Patient Zero", "Pestilence, Lord of Flies", "Pestilence, on the Pale-Green Horse");
        cinematic(add, "pestilence", "", "PESTILENCE", "*cough* \"Sorry. I'm a little under the weather. You'll be too.\"");
        cinematic(add, "pestilence", "phase2", "THE FLIES", "Burn the swarms: fire aspect, a lighter, a burning block.");
        cinematic(add, "pestilence", "mount", "THE SICKLY HORSE", "*cough* *cough* \"Excuse me.\"");
        cinematic(add, "pestilence", "death", null, "One last, rattling cough.");
        cinematic(add, "pestilence", "victory", "THE RING OF PESTILENCE", "The swamp's fever breaks.");
        messages(add, "pestilence", "*cough* \"Get well soon.\" He limps away into the fog.", "The swamp clears. He is gone.");
        add.accept("message.supernaturalcraft.pestilence.vial", "An antidote vial has turned up in the swamp.");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.summon_pestilence", "Calls up Pestilence, the Pale-Green Rider");
        add.accept("jei.supernaturalcraft.info.antidote_vial", "Turns up in the swamp while Pestilence fights. Drink it to cure the plague "
                + "and keep it off for fifteen seconds.");
        add.accept("jei.supernaturalcraft.info.plague", "Pestilence's plague stacks up to five times: each stack eats at you, takes a heart "
                + "of max health and stops natural healing. Only his antidote cures it.");
        add.accept("tooltip.supernaturalcraft.antidote_vial", "Cures the plague; none for 15 s");
        adv(add, "pestilence", "Patient Zero", "Defeat Pestilence and take his ring");
        death(add, "plague", "%1$s died of the plague", "%1$s caught %2$s's plague");
    }

    static void death(BiConsumer<String, String> add) {
        bars(add, "death", "Death", "Death, Reaper of Reapers", "Death, the World of the Dead", "Death, on the Pale Horse");
        cinematic(add, "death", "", "DEATH", "\"Sit down. Have some pizza. Your clock is already running.\"");
        cinematic(add, "death", "phase2", "THE REAPERS", "You'll only see them when your time is nearly up. Kill one to wind your clock back.");
        cinematic(add, "death", "phase3", "THE WORLD OF THE DEAD", "\"Life, death... it's all the same to me.\" Clocks run twice as fast.");
        cinematic(add, "death", "mount", "THE PALE HORSE", "\"The end is the end. Even for you.\"");
        cinematic(add, "death", "death", null, "He sets down his cane and sighs. \"Fine.\"");
        cinematic(add, "death", "victory", "THE RING OF DEATH",
                "\"Take my ring. You'll need all four. And bring it back when you're done.\" He gives back the three you offered.");
        messages(add, "death", "\"Your time isn't up. Not yet.\" He hands back the rings and goes back to his junk food.",
                "Death has somewhere to be. He leaves the rings behind.");
        add.accept("message.supernaturalcraft.death.clock_reset", "Your clock winds back.");
        add.accept("message.supernaturalcraft.death.limbo", "Your time is up. Find the light, quickly.");
        add.accept("message.supernaturalcraft.death.escaped", "You walk back out of the light.");
        add.accept("message.supernaturalcraft.death.reaped", "Death reaps what is his.");
        add.accept("message.supernaturalcraft.death.rings_returned", "Death gives back the rings you offered him.");
        add.accept("hud.supernaturalcraft.limbo", "LIMBO");
        add.accept("hud.supernaturalcraft.limbo.hint", "Reach the light");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.summon_death", "Calls up Death, the Pale Rider (gives back the rings either way)");
        add.accept("jei.supernaturalcraft.info.death_clock", "In Death's fight every hunter carries a clock of about a minute. Hit him, or "
                + "kill a reaper, to wind it back. When it runs out you fall into limbo: reach the light in fifteen seconds or die.");
        adv(add, "pale_rider", "The Pale Rider", "Defeat Death and take his ring");
        death(add, "reaped", "%1$s was reaped", "%1$s was reaped by %2$s");
    }

    static void shared(BiConsumer<String, String> add) {
        add.accept("jei.supernaturalcraft.info.horseman_steed", "Left behind by a Horseman every time he falls: a better horse "
                + "than any you could breed. Tame it, saddle it and ride it like any other.");
        add.accept("jei.supernaturalcraft.info.horsemen_trophies", "Left behind by a Horseman every time he falls.");
    }
}
