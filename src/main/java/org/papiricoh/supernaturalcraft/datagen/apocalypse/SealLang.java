package org.papiricoh.supernaturalcraft.datagen.apocalypse;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The seals' English text (v0.40, the Seals slice): the seals and their events, the omens, Uriel and his garrison, the rites of
 * the seals' spoils, Castiel's warning. Called from {@code SNLang.addAll}; the subtitles ({@link #SUBTITLES}) are read by
 * {@code SNLanguageProvider}. The Spanish goes to {@code build/agent_lang/es_es.seals.json} (merged into {@code es_es.json} by the
 * main session); both come from one table, so they always carry the same keys and placeholders.
 */
public final class SealLang {

    private SealLang() {
    }

    /** Subtitles by sound event path ({@code seal.<id>}, {@code uriel.<id>}, {@code music.uriel}). */
    public static final Map<String, String> SUBTITLES = Map.ofEntries(
            Map.entry("seal.warning", "A seal trembles"),
            Map.entry("seal.breaking", "A seal cracks"),
            Map.entry("seal.broken", "A seal breaks"),
            Map.entry("seal.held", "A seal holds"),
            Map.entry("seal.witness_wail", "A Witness wails"),
            Map.entry("seal.rite_chant", "Demons chant"),
            Map.entry("seal.thunder_far", "Distant thunder"),
            Map.entry("uriel.slash", "Uriel strikes"),
            Map.entry("uriel.palm", "The ground cracks"),
            Map.entry("uriel.smite", "Uriel smites"),
            Map.entry("uriel.flash", "Grace flares"),
            Map.entry("uriel.wings", "Wings beat"),
            Map.entry("uriel.hurt", "Uriel hurts"),
            Map.entry("uriel.death", "Uriel's light goes out"),
            Map.entry("music.uriel", "Music plays"));

    public static void add(BiConsumer<String, String> add) {
        add.accept("message.supernaturalcraft.arena.barrier.garrison", "The garrison's light holds you in.");
        add.accept("message.supernaturalcraft.arena.no_escape.garrison", "\"Nobody leaves the garrison, hunter.\"");
        add.accept("advancement.supernaturalcraft.heavens_traitor", "Heaven's Traitor");
        add.accept("advancement.supernaturalcraft.heavens_traitor.desc", "Bring down Uriel, the angel who kills angels for Lucifer");
        add.accept("advancement.supernaturalcraft.the_rising_of_the_witnesses", "The Rising of the Witnesses");
        add.accept("advancement.supernaturalcraft.the_rising_of_the_witnesses.desc", "Hold a seal against the ghosts of the ones you could not save");
        add.accept("advancement.supernaturalcraft.seal_defended", "Not Today");
        add.accept("advancement.supernaturalcraft.seal_defended.desc", "Stop the demons breaking a seal");
        add.accept("advancement.supernaturalcraft.eleventh_seal", "The End Is Nigh");
        add.accept("advancement.supernaturalcraft.eleventh_seal.desc", "See the eleventh seal break: only Lilith is left");
        add.accept("message.supernaturalcraft.ritual.needs_seals", "The seals still hold. Lilith will not come until only she is left.");
        add.accept("seal.supernaturalcraft.kind.witnesses", "The Rising of the Witnesses");
        add.accept("seal.supernaturalcraft.kind.witnesses.hint", "Kill the demon who keeps the rite and break its hex; the Witnesses fade when it ends.");
        add.accept("seal.supernaturalcraft.kind.reapers", "The Murder of the Reapers");
        add.accept("seal.supernaturalcraft.kind.reapers.hint", "Keep the bound reapers alive: kill the demons before they kill them all.");
        add.accept("seal.supernaturalcraft.kind.cemetery", "The Opened Graves");
        add.accept("seal.supernaturalcraft.kind.cemetery.hint", "The dead walk round a hex in the ground: kill its keeper and break the hex.");
        add.accept("seal.supernaturalcraft.kind.slain_angels", "The Slaughter of the Host");
        add.accept("seal.supernaturalcraft.kind.slain_angels.hint", "Rebel angels are killing the Host's own: kill the rebels before every angel falls.");
        add.accept("seal.supernaturalcraft.kind.fleeing_angel", "The Hunted Angel");
        add.accept("seal.supernaturalcraft.kind.fleeing_angel.hint", "An angel runs from the rebels sent to kill it: reach it and cut them down.");
        add.accept("seal.supernaturalcraft.kind.garrison", "The Last Garrison");
        add.accept("seal.supernaturalcraft.kind.garrison.hint", "A garrison of angels holds the seal against the rebels: fight beside them.");
        add.accept("seal.supernaturalcraft.kind.blood_water", "The Water Turned to Blood");
        add.accept("seal.supernaturalcraft.kind.blood_water.hint", "The water round a hex is turning to blood: break the hex before the rite ends.");
        add.accept("seal.supernaturalcraft.kind.withered_crops", "The Withered Fields");
        add.accept("seal.supernaturalcraft.kind.withered_crops.hint", "The land round a hex is dying: break the hex before the rite ends.");
        add.accept("seal.supernaturalcraft.kind.dead_livestock", "The Death of the Herds");
        add.accept("seal.supernaturalcraft.kind.dead_livestock.hint", "The beasts round a hex are dying one by one: break the hex.");
        add.accept("seal.supernaturalcraft.kind.swarms", "The Swarms");
        add.accept("seal.supernaturalcraft.kind.swarms.hint", "Flies pour out of a hex: break it.");
        add.accept("seal.supernaturalcraft.kind.sacrifice", "The Sacrifice");
        add.accept("seal.supernaturalcraft.kind.sacrifice.hint", "A demon is sacrificing bound villagers on its altar: save them, kill it, break its hex.");
        add.accept("seal.supernaturalcraft.kind.priest", "The Murdered Priest");
        add.accept("seal.supernaturalcraft.kind.priest.hint", "Demons have come for a priest: keep him alive and kill them.");
        add.accept("seal.supernaturalcraft.kind.witch", "The Witch's Summoning");
        add.accept("seal.supernaturalcraft.kind.witch.hint", "A witch is calling something up: kill her and break her hex before it answers.");
        add.accept("seal.supernaturalcraft.family.dead", "The Dead and the Spirits");
        add.accept("seal.supernaturalcraft.family.angels", "The Angels");
        add.accept("seal.supernaturalcraft.family.plagues", "The Plagues");
        add.accept("seal.supernaturalcraft.family.rites", "The Demons' Rites");
        add.accept("seal.supernaturalcraft.seal.1", "The Righteous Man's Blood Spilled in Hell");
        add.accept("seal.supernaturalcraft.seal.2", "The Rising of the Witnesses");
        add.accept("seal.supernaturalcraft.seal.3", "The Murder of the Reapers");
        add.accept("seal.supernaturalcraft.seal.4", "The Water Turned to Blood");
        add.accept("seal.supernaturalcraft.seal.5", "The Slaughter of the Angels");
        add.accept("seal.supernaturalcraft.seal.6", "The Plague of Flies");
        add.accept("seal.supernaturalcraft.seal.7", "The Murder of the Priest");
        add.accept("seal.supernaturalcraft.seal.8", "The Withering of the Fields");
        add.accept("seal.supernaturalcraft.seal.9", "The Death of the Herds");
        add.accept("seal.supernaturalcraft.seal.10", "The Witch's Summoning");
        add.accept("seal.supernaturalcraft.seal.11", "The Breaking of the Vessel");
        add.accept("seal.supernaturalcraft.seal.12", "Lilith's Death: the Last Seal");
        add.accept("seal.supernaturalcraft.dir.0", "the north");
        add.accept("seal.supernaturalcraft.dir.1", "the north-east");
        add.accept("seal.supernaturalcraft.dir.2", "the east");
        add.accept("seal.supernaturalcraft.dir.3", "the south-east");
        add.accept("seal.supernaturalcraft.dir.4", "the south");
        add.accept("seal.supernaturalcraft.dir.5", "the south-west");
        add.accept("seal.supernaturalcraft.dir.6", "the west");
        add.accept("seal.supernaturalcraft.dir.7", "the north-west");
        add.accept("message.supernaturalcraft.seal.warning", "A seal trembles %2$s blocks to %3$s: %1$s.");
        add.accept("message.supernaturalcraft.seal.broken", "The seal %1$s has broken: %2$s.");
        add.accept("message.supernaturalcraft.seal.broken_by", "The seal %1$s has broken (%3$s): %2$s.");
        add.accept("message.supernaturalcraft.seal.stage.1", "Something has changed in the world. The nights feel longer.");
        add.accept("message.supernaturalcraft.seal.stage.2", "The moon rises red. The angels are coming down.");
        add.accept("message.supernaturalcraft.seal.stage.3", "It rains blood, and the land sickens.");
        add.accept("message.supernaturalcraft.seal.stage.4", "The stars are falling. Demons walk by day.");
        add.accept("message.supernaturalcraft.seal.held", "The seal holds: %s. The next one will wait a day longer.");
        add.accept("message.supernaturalcraft.seal.held_late", "The seal holds: %s. The count cannot be held back any longer, but the demons lost this one.");
        add.accept("message.supernaturalcraft.seal.uriel_hint", "You have saved the Host twice. Someone up there will want a word.");
        add.accept("message.supernaturalcraft.seal.hex_broken", "The hex breaks.");
        add.accept("message.supernaturalcraft.seal.compass", "%1$s: %2$s blocks to %3$s (rite %4$s%%)");
        add.accept("message.supernaturalcraft.seal.compass.none", "No seal is calling you.");
        add.accept("message.supernaturalcraft.seal.garrison", "Castiel: \"Uriel waits at an old garrison of ours, at %1$s, %2$s. Go at night.\"");
        add.accept("message.supernaturalcraft.seal.rite.ward", "The Witnesses will not see you for a day.");
        add.accept("message.supernaturalcraft.seal.rite.feather", "The feather burns: %s angels show themselves.");
        add.accept("message.supernaturalcraft.seal.rite.feather_garrison", "The ash drifts toward the garrison: %1$s blocks to %2$s.");
        add.accept("message.supernaturalcraft.seal.rite.cleanse", "The land is cleansed: %s plagued blocks turn back.");
        add.accept("message.supernaturalcraft.seal.rite.mend", "Someone has mended a seal: the next one breaks in %s day(s).");
        add.accept("message.supernaturalcraft.seal.rite.mend_none", "There is no count of seals to hold back.");
        add.accept("message.supernaturalcraft.seal.rite.mend_cap", "The seals will not bend any further.");
        add.accept("message.supernaturalcraft.uriel.finish", "Uriel falls to his knees. Only an angel blade can end him.");
        add.accept("message.supernaturalcraft.uriel.blade_only", "Only an angel blade can end him.");
        add.accept("message.supernaturalcraft.uriel.rallies", "Nobody brought the blade. Uriel's grace pulls him up again.");
        add.accept("message.supernaturalcraft.uriel.victorious", "Uriel sheathes his blade. \"Tell Castiel he's next.\"");
        add.accept("message.supernaturalcraft.uriel.cage_gone", "Uriel's light goes out of the garrison, and he with it.");
        add.accept("title.supernaturalcraft.seal.warning", "A SEAL TREMBLES");
        add.accept("title.supernaturalcraft.seal.broken", "THE SEAL %s IS BROKEN");
        add.accept("title.supernaturalcraft.seal.held", "THE SEAL HOLDS");
        add.accept("title.supernaturalcraft.uriel.phase1", "URIEL");
        add.accept("title.supernaturalcraft.uriel.phase1.sub", "The Soldier");
        add.accept("title.supernaturalcraft.uriel.phase2", "THE TRAITOR");
        add.accept("title.supernaturalcraft.uriel.phase2.sub", "\"Lucifer is my brother too.\"");
        add.accept("title.supernaturalcraft.uriel.fall", "URIEL FALLS");
        add.accept("title.supernaturalcraft.uriel.fall.sub", "Heaven's traitor, burnt out");
        add.accept("title.supernaturalcraft.uriel.castiel", "CASTIEL");
        add.accept("title.supernaturalcraft.uriel.castiel.sub", "\"He was my friend.\"");
        add.accept("entity.supernaturalcraft.uriel.bar.phase1", "Uriel, the Soldier");
        add.accept("entity.supernaturalcraft.uriel.bar.phase2", "Uriel, the Traitor");
        add.accept("hud.supernaturalcraft.seal.rite", "Rite %1$s%% · %2$s:%3$s left");
        add.accept("hud.supernaturalcraft.seal.wards", "Demons %1$s · saved %2$s of %3$s");
        add.accept("hud.supernaturalcraft.seal.drivers", "Still standing: %s");
        add.accept("screen.supernaturalcraft.book.tab.seals", "The Seals");
        add.accept("screen.supernaturalcraft.book.seals.title", "The Sixty-Six Seals");
        add.accept("screen.supernaturalcraft.book.seals.unstarted", "The seals still hold. They will start to break when the first hunter kills Azazel.");
        add.accept("screen.supernaturalcraft.book.seals.standing", "Seal %s: still standing");
        add.accept("screen.supernaturalcraft.book.seals.broken", "Broken: %1$s of %2$s");
        add.accept("screen.supernaturalcraft.book.seals.stage.0", "The world does not know yet");
        add.accept("screen.supernaturalcraft.book.seals.stage.1", "The first omens");
        add.accept("screen.supernaturalcraft.book.seals.stage.2", "The red moon");
        add.accept("screen.supernaturalcraft.book.seals.stage.3", "Blood rain and plague");
        add.accept("screen.supernaturalcraft.book.seals.stage.4", "The stars fall");
        add.accept("screen.supernaturalcraft.book.seals.risen", "Lucifer walks the Earth.");
        add.accept("screen.supernaturalcraft.book.seals.echo", "The Cage is shut again. The world remembers.");
        add.accept("screen.supernaturalcraft.book.seals.lilith", "Only Lilith is left: her death is the last seal.");
        add.accept("screen.supernaturalcraft.book.seals.next", "The next seal breaks in %s day(s).");
        add.accept("screen.supernaturalcraft.book.seals.delayed", "Days won: %1$s of %2$s");
        add.accept("screen.supernaturalcraft.book.seals.share", "Your Share");
        add.accept("screen.supernaturalcraft.book.seals.held", "Seals held: %s");
        add.accept("screen.supernaturalcraft.book.seals.let_break", "Seals let break: %s");
        add.accept("screen.supernaturalcraft.book.seals.angels", "Angel seals held: %1$s (%2$s call Castiel)");
        add.accept("screen.supernaturalcraft.book.seals.call", "The Call");
        add.accept("screen.supernaturalcraft.book.seals.no_call", "No seal is calling you now.");
        add.accept("screen.supernaturalcraft.book.seals.where", "%1$s blocks to %2$s");
        add.accept("item.supernaturalcraft.garrison_compass", "Castiel's Compass");
        add.accept("tooltip.supernaturalcraft.seal_compass", "Hold it: it points to the seal that calls you.");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.witness_ward", "Wards the hunters in the circle against the Witnesses for a day");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.feather_sight", "Shows every angel nearby (and, to the warned, the way to the garrison)");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.cleanse_plague", "Turns back every plague round the altar");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.mend_seal", "The next seal breaks a day later");
        add.accept("dialogue.supernaturalcraft.castiel_envoy.warn", "You've saved my brothers twice at the seals. I've watched. Somebody tells the rebels where we'll be, every time. There's a traitor in the Host.");
        add.accept("dialogue.supernaturalcraft.castiel_envoy.warn.listen", "Who?");
        add.accept("dialogue.supernaturalcraft.castiel_envoy.warn.later", "Not now, Cas.");
        add.accept("dialogue.supernaturalcraft.castiel_envoy.traitor", "Uriel. My friend. He kills angels who won't join Lucifer. He waits at an old garrison of ours, by night. I can't face him alone.");
        add.accept("dialogue.supernaturalcraft.castiel_envoy.traitor.where", "Tell me where.");
        add.accept("dialogue.supernaturalcraft.castiel_envoy.traitor.doubt", "Are you sure about this?");
        add.accept("dialogue.supernaturalcraft.castiel_envoy.garrison", "This will show you the way. Bring an angel blade: nothing else will end him.");
        add.accept("dialogue.supernaturalcraft.castiel_envoy.garrison.go", "Go.");
        add.accept("dialogue.supernaturalcraft.castiel_envoy.after", "He was a soldier, and he was my friend. He believed Lucifer would free us. Thank you. I'm sorry it had to be you.");
        add.accept("dialogue.supernaturalcraft.castiel_envoy.after.go", "Go.");
    }
}
