package org.papiricoh.supernaturalcraft.datagen.journal;

import net.minecraft.world.item.Items;
import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.adv;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.any;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.has;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.ritual;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.seen;
import static org.papiricoh.supernaturalcraft.datagen.journal.SNJournal.entry;

/**
 * The great ones, in the order a hunter usually meets them. Each boss page opens when the boss is
 * seen, or as soon as calling it up is within reach, so the journal can say how to get there.
 */
final class JournalBosses {

    private JournalBosses() {
    }

    static void add(List<SNJournal.Entry> out) {
        out.add(entry("great_fights", JournalChapter.BOSSES).order(0).icon(AllItems.KEY_TO_THE_CAGE)
                .unlock(any(adv("caught_in_the_trap"), adv("christo"), seen(AllEntities.AZAZEL.get())))
                .title("The Great Fights")
                .text("The great ones do not wander. You call them, by rite or by song, and they come with their own ground: a dome "
                        + "closes over the place and there is no leaving it until one of you is done. Only one such fight can hold a "
                        + "world at a time.")
                .text("Every attack they have is written on the ground before it lands. Learn the colours: red is fire, blue is "
                        + "frost, violet is the mind's reach, gold is judgement, green is the one safe place. Move when you see them, "
                        + "and hit hardest while they recover.")
                .text("Their ground breaks under them, and puts itself back together once it is over. If everyone falls or runs, "
                        + "they simply leave. Everyone who fought shares the credit for a kill."));

        out.add(entry("azazel", JournalChapter.BOSSES).order(1).icon(AllItems.AZAZEL_BLOOD)
                .unlock(any(seen(AllEntities.AZAZEL.get()), adv("caught_in_the_trap"), adv("back_to_hell")))
                .creature(AllEntities.AZAZEL.get())
                .title("Azazel")
                .entity(AllEntities.AZAZEL.get(), "Azazel, the Yellow-Eyed Demon")
                .text("The yellow-eyed one, and the first of the great ones you can reach. Call him up at night in a great circle: "
                        + "three demon blood, three sulfur, two salt, and fire.")
                .recipe(ritual("summon_azazel"), "Summoning Azazel")
                .text("Samuel Colt's iron still holds him. Rails rise in a circle and pentagram around the altar, and he will not "
                        + "step inside while they are charged. Knock him in, shoot him in, or, once his vessel cracks, stand behind the "
                        + "rails when he rushes you as smoke: he goes in a straight line and does not swerve. Held in the rails, he is "
                        + "yours to hit while they go dark and charge again, one by one.")
                .text("Do not meet his eyes when they burn yellow. He throws what he tears out of the ground, drags you to him with a "
                        + "hand, brings the nursery fire down in lines along the floor. His smoke goes into the monsters around him and "
                        + "makes them his; where there are none, he calls demons up. Traps and salt do nothing to him.")
                .items("He leaves two vials of his blood, every time", AllItems.AZAZEL_BLOOD, AllItems.AZAZEL_TROPHY)
                .text("His death starts the Apocalypse: the demons begin breaking the seals on Lucifer's Cage, Ruby comes calling, and "
                        + "the way opens to Alastair and, at the end, to Lilith."));

        out.add(entry("lilith", JournalChapter.BOSSES).order(2).icon(AllItems.LAST_SEAL)
                .unlock(any(seen(AllEntities.LILITH.get()), adv("on_the_head_of_a_pin")))
                .creature(AllEntities.LILITH.get())
                .title("Lilith")
                .entity(AllEntities.LILITH.get(), "Lilith, the First Demon")
                .text("The first demon, and the last of the seals. She only answers once Alastair is dead and eleven seals have "
                        + "broken: a great circle at night, three demon blood, two hellfire embers, two bones and a vial of bound "
                        + "blood to sign her contract.")
                .recipe(ritual("summon_lilith"), "Summoning Lilith")
                .text("Headstones rise in a ring around the circle. When she shines, the white light falls on everyone she can see: "
                        + "put a headstone between you. Each one takes two bursts before it falls, so keep moving. After each burst "
                        + "she is hollow for a moment, and that is when to hit her. In the end her light comes twice.")
                .text("If she writes your name, it is a contract. Everyone who can must wound her before it comes due, and holy "
                        + "wounds pay it down twice as fast. Burn it in time and she staggers. Fail, and hounds you cannot see come for "
                        + "whoever she named.")
                .items("What she leaves", AllItems.LAST_SEAL, AllItems.HOUND_WHISTLE, AllItems.LILITH_TROPHY, AllItems.DAMNED_CONTRACT));

        out.add(entry("last_seal", JournalChapter.BOSSES).order(3).icon(AllItems.LAST_SEAL)
                .unlock(any(has(AllItems.LAST_SEAL), adv("lucifer_rising")))
                .title("The Last Seal")
                .text("Sixty-six seals held the Cage shut, and Lilith was the last. When she dies, it breaks, Lucifer rises, and "
                        + "what is left of the seal falls at your feet.")
                .items("The sixty-sixth seal", AllItems.LAST_SEAL)
                .text("Keep it. Lucifer walks the earth now, but the fight to throw him back is at the end of the Horsemen's road: "
                        + "his rite will not take without the seal, and it burns it. If he wins, the seal is gone with him: you "
                        + "will be calling Lilith up again before you get another try."));

        out.add(entry("lucifer", JournalChapter.BOSSES).order(4).icon(AllItems.MORNINGSTAR_TROPHY)
                .unlock(any(seen(AllEntities.LUCIFER.get()), has(AllItems.KEY_TO_THE_CAGE), adv("pale_rider"), adv("lock_and_key")))
                .creature(AllEntities.LUCIFER.get())
                .title("Lucifer")
                .entity(AllEntities.LUCIFER.get(), "Lucifer, the Morning Star")
                .text("The end of season five. With the four Horsemen beaten and their rings forged into the Key, the Cage can be "
                        + "opened and the Devil thrown back in. The great circle, at night, lit with the Key: three demon blood, two "
                        + "hellfire embers, a holy water and the Last Seal on the altar.")
                .recipe(ritual("summon_lucifer"), "Opening the Cage")
                .text("Only holy things truly hurt him: angel blades, Smite, holy water. The Cage will not let you leave. Every attack "
                        + "is written on the ground first: red is fire, so move; blue is frost; violet is a beam, so break line of "
                        + "sight; gold is judgement. Hit hardest while he recovers after an attack.")
                .text("He has four faces before the end: Lucifer, the Fallen, the Morningstar, and the Archangel Unbound, who takes "
                        + "to the air. If he wins, he walks back into the Cage and leaves a cracked key behind. If he loses, the "
                        + "rings open the Cage under him and he falls (Swan Song): the rings come back to you, and a shackle of the "
                        + "Cage is left on the ground."));

        out.add(entry("lucifers_tricks", JournalChapter.BOSSES).order(5).icon(AllItems.ARCHANGEL_BLADE)
                .unlock(any(seen(AllEntities.LUCIFER.get()), adv("devil_went_down")))
                .title("His Tricks")
                .text("Jump the shockwave when he lands. Break out of an ice cage with any tool. Hide behind ice from the grace beam. "
                        + "Reveal unmasks his illusions; strike one and it bursts.")
                .entity(AllEntities.LUCIFER_ILLUSION.get(), "One of his illusions")
                .text("The Snare only holds him while he recovers. When he gathers his grace, stand in a green sigil or inside a Ward, or "
                        + "die. Break his grace tether by running or by hitting him.")
                .text("Beat him and he leaves everything: the Archangel Blade, his grace, a nether star, the Echo sigil, his "
                        + "likeness in stone, your four rings, and the Cage Shackle you will need one day to open the Cage again. "
                        + "The Colt does nothing to him.")
                .items("The spoils", AllItems.ARCHANGEL_BLADE, AllItems.LUCIFERS_GRACE, AllItems.CAGE_SHACKLE,
                        AllItems.MORNINGSTAR_TROPHY));

        out.add(entry("broken_chorus", JournalChapter.BOSSES).order(6).icon(AllItems.CHOIR_TROPHY)
                .unlock(any(seen(AllEntities.BROKEN_CHORUS.get()), adv("hymnal_spire"), adv("shattered_hymn"), has(AllItems.SHATTERED_HYMN)))
                .creature(AllEntities.BROKEN_CHORUS.get())
                .title("The Broken Chorus")
                .entity(AllEntities.BROKEN_CHORUS.get(), "The Broken Chorus", 0.5f)
                .text("A choir that sang before the Throne until the Throne was empty, and ran together into one broken thing. It is "
                        + "woken on the summit of a Hymnal Spire, in a thunderstorm: lay the Shattered Hymn on the Choir Altar and ring "
                        + "its three notes on the bells. The temple window on the stair shows them, left to right. A wrong note starts "
                        + "the hymn over.")
                .text("Its faces sing, its wings beat, its wheels see. Break each in turn: bring the wings down to the stone, strike "
                        + "the eyes while they are open. Meet no open eye, or hide behind stone, and hide in a pillar's shadow from its "
                        + "light.")
                .text("When it sings its Hymn, one bell lights: ring the bell it sings and it will kneel. Up there, a fall from the "
                        + "summit is forgiven, once.")
                .items("What it leaves", AllItems.SERAPH_WINGS, AllItems.CHOIR_SHARD, AllItems.CHOIR_TROPHY));

        out.add(entry("metatron", JournalChapter.BOSSES).order(7).icon(AllItems.ANGEL_TABLET)
                .unlock(any(seen(AllEntities.METATRON.get()), adv("silence_falls")))
                .creature(AllEntities.METATRON.get())
                .title("Metatron")
                .entity(AllEntities.METATRON.get(), "Metatron, the Scribe of God")
                .text("The Scribe of God. Once the Broken Chorus is silenced, write his name, Metatron, in a book and quill, and offer it in a "
                        + "great circle at night with two choir shards, a chrism, a holy water, two feathers and a glow ink sac.")
                .recipe(ritual("summon_metatron"), "Calling down Metatron")
                .text("He fights with a blade at first. Then his Hand writes burning words across the floor: get out of the strokes "
                        + "before they burn, jump the sweep, be elsewhere when the palm comes down. In the third act he takes to a "
                        + "lectern on a dais with two flights of stairs and will not leave it; climb, or strike from afar. His Book "
                        + "crushes, storms pages, and shuts on one of you until the others wound him enough.")
                .text("At the last he takes up the Tablet and speaks the Word: BE STILL, LOOK AWAY or KNEEL. Obey it, to the letter, "
                        + "for the three seconds it holds. Watch the sky, too: angels fall burning where he points.")
                .items("What he leaves", AllItems.ANGEL_TABLET, AllItems.METATRON_TROPHY, AllItems.CHOIR_SHARD));

        out.add(entry("amara", JournalChapter.BOSSES).order(8).icon(AllItems.ECLIPSE_TROPHY)
                .unlock(any(seen(AllEntities.AMARA.get()), adv("dont_call_me_shurley")))
                .creature(AllEntities.AMARA.get())
                .title("Amara")
                .entity(AllEntities.AMARA.get(), "Amara, the Darkness", 0.5f)
                .text("The Darkness, God's sister, older than the light. Chuck shows you how to call her: three Hands of God (or four, or "
                        + "five: each one beyond three weakens her before she rises) in a void circle in the Overworld, lit with a nether "
                        + "star. Wherever you call her, the town she swallowed rises around you.")
                .recipe(ritual("summon_amara"), "Calling the Darkness")
                .text("She has six bodies. The Mass first: break the four rings that circle her, or three of the cysts at her roots, and "
                        + "she sinks with her core laid open; strike it. Then the Girl, quick and playful: she hides among decoys of herself "
                        + "and the real one, found, is open a while. Then the Woman: close, cruel, and hungry for your soul; light snaps her "
                        + "hold.")
                .text("In her fourth body her brother walks into the town. His light burns her, and she tears at it with chains of smoke: "
                        + "hurt her while she holds them. In the fifth the town comes undone, ring by ring, into islands over the Void, and "
                        + "a black sun with a face fills the sky. In the last the sun dies on a countdown; when it runs out, so does her "
                        + "patience.")
                .text("She does not die. Brought to her knees she lets her brother come to her, and the two of them make peace. The sun "
                        + "comes back for the whole world. Every hunter who fought her has a share of what she leaves.")
                .items("What she leaves", AllItems.PENUMBRA, AllItems.ECLIPSE_SIGHT, AllItems.VOID_ESSENCE, AllItems.ECLIPSE_TROPHY,
                        org.papiricoh.supernaturalcraft.darkness.DarknessRegistry.AMARAS_LIGHT));

        out.add(entry("against_the_dark", JournalChapter.BOSSES).order(9).icon(AllItems.CENSER_OF_GRACE)
                .unlock(any(seen(AllEntities.AMARA.get()), adv("dawn")))
                .title("Against the Dark")
                .text("Carry light, for the dark eats you where you stand. Torches, lanterns, the censer's beam and smiting light all "
                        + "count. She will snuff what you place; light it again.")
                .items("Light", AllItems.CENSER_OF_GRACE, Items.TORCH, Items.LANTERN,
                        Items.FLINT_AND_STEEL)
                .text("When the Totality comes, stand by a burning well. When the ground is unmade, stand in the light."));

        out.add(entry("four_horsemen", JournalChapter.BOSSES).order(10).icon(AllItems.RING_OF_DEATH)
                .unlock(any(adv("lucifer_rising"), adv("four_horsemen"), has(AllItems.RING_OF_WAR), has(AllItems.RING_OF_FAMINE),
                        has(AllItems.RING_OF_PESTILENCE), has(AllItems.RING_OF_DEATH)))
                .title("The Four Horsemen")
                .text("With Lilith dead and Lucifer risen, the Horsemen ride. Their four rings, forged together, are the Key to "
                        + "the Cage, and there is only one way to get a Horseman's ring: win it from him. Once Lilith has fallen "
                        + "they will answer a call.")
                .items("War, Famine, Pestilence, Death", AllItems.RING_OF_WAR, AllItems.RING_OF_FAMINE, AllItems.RING_OF_PESTILENCE,
                        AllItems.RING_OF_DEATH)
                .text("War, Famine and Pestilence come up here in the world, each to his own rite, in any order. Death comes too, "
                        + "but only for a hunter who offers him the other three rings. He gives them back, win or lose.")
                .recipe(ritual("summon_war"), "War")
                .recipe(ritual("summon_famine"), "Famine")
                .recipe(ritual("summon_pestilence"), "Pestilence")
                .recipe(ritual("summon_death"), "Death")
                .text("Each one lays his own ground over the arena and rides his horse for the last part of the fight. Beat him and "
                        + "he leaves his ring, his likeness and his horse, every time you beat him. Then forge the four rings into the "
                        + "Key, and go after the Devil."));

        out.add(entry("lucifer_uncaged", JournalChapter.BOSSES).order(20).icon(AllItems.FALLEN_STAR)
                .unlock(any(seen(AllEntities.LUCIFER_UNCAGED.get()), adv("four_horsemen")))
                .creature(AllEntities.LUCIFER_UNCAGED.get())
                .title("Lucifer Uncaged")
                .entity(AllEntities.LUCIFER_UNCAGED.get(), "Lucifer, uncaged")
                .text("Back in the box, at the end of season 14: once the Shadow sleeps again, he can be put back for good. "
                        + "At the heart of Hell the Pit falls to the lava sea, and over it the Cage hangs on its chains. Under it, on the "
                        + "island, is the dais: draw the circle around its four stones, lay the four rings with two demon blood, a holy "
                        + "water and an ember, and open the Cage with the Key. He comes down in chains, and leaves them behind, one by "
                        + "one.")
                .recipe(ritual("summon_lucifer_uncaged"), "Opening the Cage, for good")
                .text("Six faces. The Prisoner. Hellfire. The cold of the Cage. Legion: kill the hounds, or they will drag you down. "
                        + "The Morning Star: watch for gold on the ground, the stars are falling. Light-Bringer: stand between the wings, "
                        + "and when he burns, find the green. Everything you learned against him still holds, but the floor will not.")
                .text("If you fall, the rings stay on the stone. Beat him and the Cage is shut, and what he leaves behind is a fallen "
                        + "star, a load of rounds, two nether stars and a page of Smite.")
                .items("The Fallen Star", AllItems.FALLEN_STAR));
    }
}
