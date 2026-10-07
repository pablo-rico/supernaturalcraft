package org.papiricoh.supernaturalcraft.datagen.lang;

import java.util.function.BiConsumer;

/** v0.8 lang strings owned by the spells agent (locate, concealment, second sight, purification, bind, banish, revive). Item/block/entity/effect names come from their ids (SNLanguageProvider). */
public final class SpellLang {

    private SpellLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        // Blood vials
        add.accept("tooltip.supernaturalcraft.blood_vial.of", "Blood of %s");
        add.accept("tooltip.supernaturalcraft.blood_vial.unknown", "Whose blood, nobody knows");
        add.accept("tooltip.supernaturalcraft.blood_vial.hint", "Poured into a spell bowl, it leads back to its owner");
        add.accept("message.supernaturalcraft.blood_vial.drawn", "You draw %s's blood.");
        add.accept("message.supernaturalcraft.blood_vial.own", "You cut your palm and fill the vial.");

        // Pet collars
        add.accept("tooltip.supernaturalcraft.pet_collar.bound", "Bound to %s");
        add.accept("tooltip.supernaturalcraft.pet_collar.unbound", "Use it on a tamed animal of yours");
        add.accept("tooltip.supernaturalcraft.pet_collar.hint", "The link a locating or reviving spell needs");
        add.accept("message.supernaturalcraft.pet_collar.bound", "%s wears your collar now.");
        add.accept("message.supernaturalcraft.pet_collar.not_tame", "Only a tamed animal will wear a collar.");
        add.accept("message.supernaturalcraft.pet_collar.not_yours", "That animal is not yours to collar.");

        // Locating
        add.accept("message.supernaturalcraft.locate.no_blood", "The spell needs the blood of whoever you seek.");
        add.accept("message.supernaturalcraft.locate.no_collar", "The spell needs your pet's collar.");
        add.accept("message.supernaturalcraft.locate.not_found", "The smoke wanders aimlessly: it finds nothing.");
        add.accept("message.supernaturalcraft.locate.elsewhere", "The smoke sinks back into the bowl: %s is far beyond this world.");
        add.accept("message.supernaturalcraft.locate.dead", "The smoke settles like ash: %s is no more.");
        add.accept("message.supernaturalcraft.locate.drift", "The smoke drifts %s, %s.");
        add.accept("message.supernaturalcraft.locate.here", "The smoke curls about, close by.");
        add.accept("message.supernaturalcraft.locate.band.here", "close by");
        add.accept("message.supernaturalcraft.locate.band.near", "not far");
        add.accept("message.supernaturalcraft.locate.band.far", "far away");
        add.accept("message.supernaturalcraft.locate.band.distant", "very far away");
        add.accept("direction.supernaturalcraft.north", "north");
        add.accept("direction.supernaturalcraft.north_east", "north-east");
        add.accept("direction.supernaturalcraft.east", "east");
        add.accept("direction.supernaturalcraft.south_east", "south-east");
        add.accept("direction.supernaturalcraft.south", "south");
        add.accept("direction.supernaturalcraft.south_west", "south-west");
        add.accept("direction.supernaturalcraft.west", "west");
        add.accept("direction.supernaturalcraft.north_west", "north-west");

        // Concealment
        add.accept("message.supernaturalcraft.concealment.broken", "You struck from hiding: your concealment is broken.");

        // Purification, binding, banishing
        add.accept("message.supernaturalcraft.purify.nothing", "Nothing here needs cleansing.");
        add.accept("message.supernaturalcraft.bind.nothing", "There is nothing here to bind.");
        add.accept("message.supernaturalcraft.bind.bound", "%s is bound to the bowl.");
        add.accept("message.supernaturalcraft.banish.nothing", "Nothing unclean lingers here.");

        // Reviving a pet
        add.accept("message.supernaturalcraft.revive.no_collar", "The spell needs the collar of the pet you lost.");
        add.accept("message.supernaturalcraft.revive.alive", "That pet still lives.");
        add.accept("message.supernaturalcraft.revive.no_trace", "There is nothing left of that pet to call back.");
        add.accept("message.supernaturalcraft.revive.back", "%s comes back to you.");

        // Effect descriptions (shown by JEI and effect-description mods)
        add.accept("effect.supernaturalcraft.concealed.description", "Demons, angels, spirits and hellhounds neither see nor hunt you. Never bosses, and never a hound collecting your debt. Striking one of them breaks it.");
        add.accept("effect.supernaturalcraft.second_sight.description", "You see what hides: ghosts, hellhounds, invisible creatures and hidden curse bags.");
    }
}
