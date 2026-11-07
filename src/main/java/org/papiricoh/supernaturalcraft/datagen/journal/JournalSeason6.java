package org.papiricoh.supernaturalcraft.datagen.journal;

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
 * Season 6 (v0.39): Castiel's call, the King of Hell, the lab and its door, Godstiel and his fate, and what he leaves. Every
 * number here is the code's ({@code PactRules}, {@code GodstielBalance}, the items).
 */
final class JournalSeason6 {

    private JournalSeason6() {
    }

    static void add(List<SNJournal.Entry> out) {
        out.add(entry("castiel", JournalChapter.ANGELS).order(50).icon(net.minecraft.world.item.Items.FEATHER)
                .unlock(any(seen(AllEntities.CASTIEL.get()), adv("castiels_call")))
                .creature(AllEntities.CASTIEL.get())
                .title("Castiel")
                .entity(AllEntities.CASTIEL.get(), "Castiel, angel of the Lord")
                .text("An angel in a trench coat, losing a civil war in Heaven. With Raphael gone his legions are still marching, and "
                        + "Castiel means to stop them whatever it costs. He comes at dawn, says what he needs and is gone before you can "
                        + "ask what it costs.")
                .text("He sends you to a partner at a crossroads, asks after you while you hunt, and on the last night tells you to bring "
                        + "your blade. Listen to what he does not say."));

        out.add(entry("castiel_forgiven", JournalChapter.ANGELS).order(51).icon(AllItems.GRACE_SHARD)
                .unlock(any(seen(AllEntities.CASTIEL_COMPANION.get()), adv("castiel_forgiven")))
                .creature(AllEntities.CASTIEL_COMPANION.get())
                .title("Castiel, Forgiven")
                .entity(AllEntities.CASTIEL_COMPANION.get(), "Castiel, keeping his penance")
                .text("Forgiven, Castiel goes where the Leviathans came from to keep his penance. A hunter who forgave him finds him in "
                        + "Purgatory: he walks with them, smites whatever monster comes close, and stays behind when they leave."));
        out.add(entry("crowley_king", JournalChapter.DEMONS).order(70).icon(AllItems.CROSSROADS_CONTRACT)
                .unlock(any(seen(AllEntities.CROWLEY_KING.get()), adv("king_of_hell")))
                .creature(AllEntities.CROWLEY_KING.get())
                .title("The King of Hell")
                .entity(AllEntities.CROWLEY_KING.get(), "Crowley, King of Hell")
                .text("With Lucifer back in his Cage, somebody had to run Hell. Crowley stepped up from the crossroads, and Castiel is "
                        + "his partner now: they mean to open Purgatory and split what is inside.")
                .text("Sent to him, call on a crossroads (bury a crossroads box at a wild crossroads, or call its demon in the spell "
                        + "bowl) and the King comes instead of his demon. Leave him waiting three days and he comes to you at midnight. "
                        + "He wants the three Alphas brought down; when they are, he hands you his lab and a compass to it."));

        out.add(entry("crowleys_lab", JournalChapter.PLACES).order(42).icon(AllItems.GODSTIEL_SPAWN_EGG)
                .unlock(any(adv("the_alphas"), adv("the_man_who_knew_too_much")))
                .title("Crowley's Lab")
                .text("An old brick pump house at the edge of a reservoir, far from anyone, where nobody has run the waterworks for "
                        + "decades. Crowley made it his workshop: cages along one wall (he kept the Alphas there), benches along the "
                        + "other, a clear floor in the middle for a great circle.")
                .text("By night, with the three Alphas down, a great circle round an altar in the middle of the hall opens the door: "
                        + "Alpha blood, demon blood, holy water, an eye of ender, soul sand, two bones and gold, woken with an angel "
                        + "blade (kept). Castiel will be there. So will Crowley.")
                .recipe(ritual("open_purgatory_door"), "Opening the door"));

        out.add(entry("godstiel", JournalChapter.BOSSES).order(60).icon(AllItems.NEW_GOD_BLADE)
                .unlock(any(seen(AllEntities.GODSTIEL.get()), adv("the_man_who_knew_too_much")))
                .creature(AllEntities.GODSTIEL.get())
                .title("Godstiel")
                .entity(AllEntities.GODSTIEL.get(), "Castiel, full of Purgatory")
                .text("Castiel opened Purgatory's door and took every soul inside it into himself. He calls himself the new God now, "
                        + "and for a while he is right. Something came in with the souls. The longer the fight lasts, the less of him "
                        + "is Castiel. Five phases, four in the hall and the last out on the reservoir.")
                .text("The New God: gold circles under every hunter (light falls), a gold lane ahead (a palm of grace that throws you), "
                        + "a gold ring round him (light that burns and blinds). Judgment: angels and demons dragged in to kneel in gold "
                        + "rings and burst when he judges them; kill them first. Bolts fall down a fan of gold lanes.")
                .text("The Cracks and the Voices: the Leviathans show. A violet cone close in front is the mouth in his chest; a wider "
                        + "one is goo, which pools and burns. A violet ring is the tentacles sweeping round him, a violet lane their slam. "
                        + "From now on borax burns him.")
                .text("The inner fight: as each of the last three phases begins, and halfway through it, Castiel doubles over fighting "
                        + "what is in him. He takes far more from every blow then. Speak to him (use on him): each of you keeps him in it "
                        + "a little longer.")
                .text("The Overflow: the big doors open, he goes out over the reservoir and the water turns black. Violet geysers on the "
                        + "deck; Leviathans climb out of the water (borax bursts them).")
                .text("Beaten, he kneels and the one who opened the door decides: forgive him, or leave him to the Leviathans. Say "
                        + "nothing and they take him. Either way, the Leviathans are out.")
                .recipe(ritual("summon_godstiel"), "Calling him back (a rematch, at the lab's altar)")
                .items("What everyone who fought him gets", AllItems.NEW_GOD_BLADE, AllItems.SIGIL_PAGE));

        out.add(entry("godstiel_spoils", JournalChapter.ARSENAL).order(50).icon(AllItems.NEW_GOD_BLADE)
                .unlock(any(has(AllItems.NEW_GOD_BLADE.get()), has(AllItems.GRACE_SHARD.get()), has(AllItems.CASTIELS_TRENCHCOAT.get()),
                        adv("the_man_who_knew_too_much")))
                .title("The New God's Leavings")
                .text("The Blade of the New God: an angelic blade, deadly to demons, still full of what Castiel swallowed. Used, it lets "
                        + "a wave of Purgatory's light go ahead of you (35 mana), twice as hard on anything borax burns.")
                .text("A page of Wrath of the New God: a Master's sigil, Castiel's own judgement: a wide column of soul light where it "
                        + "marked, a moment later, cruel to the unholy. Burns holy oil.")
                .text("Forgiven, he leaves a Shard of his Grace: taken in, it widens your well by 25 mana, once. Condemned, all that "
                        + "comes back up is his trench coat: Leviathans deal less to whoever wears it, and their food cannot sedate them.")
                .items("Blade, shard and coat", AllItems.NEW_GOD_BLADE, AllItems.GRACE_SHARD, AllItems.CASTIELS_TRENCHCOAT));
    }
}
