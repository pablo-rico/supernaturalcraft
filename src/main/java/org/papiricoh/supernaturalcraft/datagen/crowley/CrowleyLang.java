package org.papiricoh.supernaturalcraft.datagen.crowley;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Crowley's text (v0.35): his bar and title cards, messages, the talk at his fall, the companion's menu and voice, the Contract
 * Book, the asylum's case, the rite and the advancement. Sound subtitles ({@link #SUBTITLES}) are read by
 * {@code SNLanguageProvider}. Called from {@code SNLang.addAll}. The Spanish lives in {@code es_es.json} under the same keys.
 */
public final class CrowleyLang {

    private CrowleyLang() {
    }

    public static final Map<String, String> SUBTITLES = Map.ofEntries(
            Map.entry("crowley.snap", "Fingers snap"),
            Map.entry("crowley.smoke_out", "Red smoke bursts"),
            Map.entry("crowley.smoke_in", "Red smoke gathers"),
            Map.entry("crowley.paper_storm", "Contracts fly"),
            Map.entry("crowley.hellfire", "Hellfire roars"),
            Map.entry("crowley.seal_break", "A wax seal cracks"),
            Map.entry("crowley.hounds", "Hellhounds snarl"),
            Map.entry("crowley.laugh", "Crowley chuckles"),
            Map.entry("crowley.hurt", "Crowley is struck"),
            Map.entry("crowley.death", "The King of the Crossroads burns"),
            Map.entry("crowley.suit_hurt", "A suit is struck"),
            Map.entry("crowley.suit_death", "A suit goes up in smoke"),
            Map.entry("crowley.patient_ambient", "A patient mutters"),
            Map.entry("crowley.patient_hurt", "A patient shrieks"),
            Map.entry("crowley.patient_death", "A patient falls still"));

    public static void add(BiConsumer<String, String> add) {
        add.accept("entity.supernaturalcraft.crowley.bar.phase1",
                "Crowley, King of the Crossroads");
        add.accept("entity.supernaturalcraft.crowley.bar.phase2",
                "Crowley, Among the Inmates");
        add.accept("entity.supernaturalcraft.crowley.bar.phase3",
                "Crowley, on His Throne");
        add.accept("title.supernaturalcraft.crowley.phase1",
                "The King of the Crossroads");
        add.accept("title.supernaturalcraft.crowley.phase1.sub",
                "Hello, boys. Do come in.");
        add.accept("title.supernaturalcraft.crowley.phase2",
                "The Cell Wing");
        add.accept("title.supernaturalcraft.crowley.phase2.sub",
                "Mind the patients. They bite.");
        add.accept("title.supernaturalcraft.crowley.phase3",
                "The Throne Room");
        add.accept("title.supernaturalcraft.crowley.phase3.sub",
                "Every king needs a chair");
        add.accept("title.supernaturalcraft.crowley.fall",
                "The King Kneels");
        add.accept("title.supernaturalcraft.crowley.fall.sub",
                "And, naturally, he has an offer");
        add.accept("cinematic.supernaturalcraft.crowley.victory.title",
                "King of the Crossroads");
        add.accept("cinematic.supernaturalcraft.crowley.victory.subtitle",
                "His contracts are yours now. Read the small print.");
        add.accept("cinematic.supernaturalcraft.crowley.recruited.title",
                "A Deal Between Gentlemen");
        add.accept("cinematic.supernaturalcraft.crowley.recruited.subtitle",
                "The King of the Crossroads works for you now. Allegedly.");
        add.accept("message.supernaturalcraft.crowley.hello",
                "Crowley: \"Hello, boys. I was beginning to think you'd never find the place.\"");
        add.accept("message.supernaturalcraft.crowley.phase2",
                "Crowley: \"Let's take this somewhere with more character.\"");
        add.accept("message.supernaturalcraft.crowley.phase3",
                "Crowley: \"Fine. My office. Wipe your feet.\"");
        add.accept("message.supernaturalcraft.crowley.collection",
                "You owe a crossroads debt. Crowley has come to collect.");
        add.accept("message.supernaturalcraft.crowley.deciding",
                "%s decides what becomes of Crowley.");
        add.accept("message.supernaturalcraft.crowley.nobody",
                "Silence");
        add.accept("message.supernaturalcraft.crowley.executed",
                "%s ends the King of the Crossroads.");
        add.accept("message.supernaturalcraft.crowley.recruited",
                "%s strikes a deal with Crowley. He works for them now.");
        add.accept("message.supernaturalcraft.crowley.busy",
                "Another fight already holds this world: Crowley sends his regrets.");
        add.accept("message.supernaturalcraft.crowley.cage_gone",
                "The fight is over. Crowley is gone in a puff of red smoke.");
        add.accept("message.supernaturalcraft.crowley.victorious",
                "Crowley: \"Do come again. Bring a better plan.\"");
        add.accept("message.supernaturalcraft.crowley.rite.undecided",
                "Crowley has never been beaten. Find him first: the Men of Letters have a file on an asylum.");
        add.accept("message.supernaturalcraft.crowley.rite.elsewhere",
                "He only answers in his own throne room, in the asylum.");
        add.accept("message.supernaturalcraft.crowley.follow",
                "Crowley: \"Lead on. I'll try to keep up.\"");
        add.accept("message.supernaturalcraft.crowley.info.boss",
                "Crowley: \"If I were you — and thank God I'm not — I'd worry about %s next.\"");
        add.accept("message.supernaturalcraft.crowley.info.no_boss",
                "Crowley: \"There's nobody big left on your road, darling. Enjoy the quiet.\"");
        add.accept("message.supernaturalcraft.crowley.info.officer",
                "Crowley: \"One of the Pit's: %s, last seen around X %s, Z %s. You didn't hear it from me.\"");
        add.accept("message.supernaturalcraft.crowley.info.no_officer",
                "Crowley: \"The Pit's lot are keeping their heads down. Smart of them.\"");
        add.accept("message.supernaturalcraft.crowley.intrigue.wait",
                "Crowley: \"Patience. A good rumour needs a few days to ripen.\"");
        add.accept("message.supernaturalcraft.crowley.intrigue.none",
                "Crowley: \"Nobody down there worth turning on anybody else. Yet.\"");
        add.accept("message.supernaturalcraft.crowley.intrigue.done",
                "Crowley: \"A word in the right ear. %s will be at %s's throat by morning.\"");
        add.accept("message.supernaturalcraft.crowley.deal.wait",
                "Crowley: \"One deal a day. I have a reputation.\"");
        add.accept("message.supernaturalcraft.crowley.deal.trade.short",
                "Crowley: \"%s emeralds. I don't do credit. Not any more.\"");
        add.accept("message.supernaturalcraft.crowley.deal.favour.none",
                "Crowley: \"You don't owe anyone anything. How dull.\"");
        add.accept("message.supernaturalcraft.crowley.deal.favour.wait",
                "Crowley: \"Favours are once every %s days. Even mine.\"");
        add.accept("message.supernaturalcraft.crowley.deal.luck",
                "Crowley snaps his fingers. The crossroads smile on you for a while.");
        add.accept("message.supernaturalcraft.crowley.deal.reveal",
                "Crowley snaps his fingers. Every hellhound near you shows itself.");
        add.accept("message.supernaturalcraft.crowley.deal.trade",
                "Crowley snaps his fingers, and your pockets are heavier.");
        add.accept("message.supernaturalcraft.crowley.deal.favour",
                "Crowley snaps his fingers. Your crossroads contract is void.");
        add.accept("message.supernaturalcraft.arena.barrier.asylum",
                "The asylum's bars won't give. The King hasn't dismissed you.");
        add.accept("message.supernaturalcraft.arena.no_escape.asylum",
                "Red smoke swallows the jump.");
        add.accept("message.supernaturalcraft.contract_book.not_a_mob",
                "That can't sign anything.");
        add.accept("message.supernaturalcraft.contract_book.too_great",
                "Too great a thing to bind with ink.");
        add.accept("message.supernaturalcraft.contract_book.bound",
                "Already bound by a contract.");
        add.accept("message.supernaturalcraft.contract_book.resting",
                "The book needs time before the next contract.");
        add.accept("message.supernaturalcraft.contract_book.cost",
                "This contract costs %s levels.");
        add.accept("message.supernaturalcraft.contract_book.lapsed",
                "The contract with %s has lapsed.");
        add.accept("message.supernaturalcraft.contract_book.signed",
                "%s signs. It fights for you, for now.");
        add.accept("tooltip.supernaturalcraft.contract_book",
                "Use on a creature to bind it by contract for a while. Costs experience by its size.");
        add.accept("dialogue.supernaturalcraft.crowley_fate.demon",
                "\"Well. One of ours, standing over the King of the Crossroads. Kill me and the Pit gets another sheep with a knife. Keep me, and I'll put you where you belong — on top. I know every name in that chain, darling. Every debt.\"");
        add.accept("dialogue.supernaturalcraft.crowley_fate.angel",
                "\"An angel. Of course. Heaven's finest, here to tidy up. Go on, then, smite. Or — hear me out — keep me. Heaven has its secrets and Hell has its ledgers, and I've read both. You'll find me very useful, feathers.\"");
        add.accept("dialogue.supernaturalcraft.crowley_fate.human",
                "\"Right. Hunter. You've won, I'm kneeling, it's all very dramatic. Here's a deal between gentlemen: you don't put that blade in me, and I'll be the best friend you ever regretted having. No soul required. Just this once.\"");
        add.accept("dialogue.supernaturalcraft.crowley_fate.demon.recruit",
                "Keep him. A demon who knows the Pit is worth more alive.");
        add.accept("dialogue.supernaturalcraft.crowley_fate.demon.execute",
                "End him. The Pit needs no more kings.");
        add.accept("dialogue.supernaturalcraft.crowley_fate.angel.recruit",
                "Keep him. Let Hell's ledgers work for Heaven.");
        add.accept("dialogue.supernaturalcraft.crowley_fate.angel.execute",
                "Smite him. Heaven does not bargain.");
        add.accept("dialogue.supernaturalcraft.crowley_fate.human.recruit",
                "Deal. But one wrong move and you're done.");
        add.accept("dialogue.supernaturalcraft.crowley_fate.human.execute",
                "No deals. Not with you.");
        add.accept("dialogue.supernaturalcraft.crowley_companion.menu",
                "\"What is it now? I'm a busy man.\"");
        add.accept("dialogue.supernaturalcraft.crowley_companion.menu.info",
                "What do you know?");
        add.accept("dialogue.supernaturalcraft.crowley_companion.menu.deal",
                "Let's make a deal.");
        add.accept("dialogue.supernaturalcraft.crowley_companion.menu.intrigue",
                "Stir things up in the Pit.");
        add.accept("dialogue.supernaturalcraft.crowley_companion.menu.stay",
                "Stay here.");
        add.accept("dialogue.supernaturalcraft.crowley_companion.menu.follow",
                "Come with me.");
        add.accept("dialogue.supernaturalcraft.crowley_companion.menu.bye",
                "Nothing. Carry on.");
        add.accept("dialogue.supernaturalcraft.crowley_companion.deals",
                "\"Deals, then. One a day, no souls, no small print. Well — less small print.\"");
        add.accept("dialogue.supernaturalcraft.crowley_companion.deals.luck",
                "Crossroads luck (Luck, 20 minutes)");
        add.accept("dialogue.supernaturalcraft.crowley_companion.deals.reveal",
                "Show me the hounds (reveals hellhounds nearby)");
        add.accept("dialogue.supernaturalcraft.crowley_companion.deals.trade",
                "Payment in kind (8 emeralds for supplies)");
        add.accept("dialogue.supernaturalcraft.crowley_companion.deals.favour",
                "A favour (voids a crossroads debt, once a week)");
        add.accept("dialogue.supernaturalcraft.crowley_companion.deals.back",
                "Something else.");
        add.accept("voice.supernaturalcraft.crowley.boss.0",
                "Another one down. I'd clap, but I'm wearing gloves.");
        add.accept("voice.supernaturalcraft.crowley.boss.1",
                "Impressive. Messy, but impressive.");
        add.accept("voice.supernaturalcraft.crowley.boss.2",
                "You know, that one owed me money.");
        add.accept("voice.supernaturalcraft.crowley.boss.3",
                "Bravo. Now let's never do that again.");
        add.accept("voice.supernaturalcraft.crowley.boss.4",
                "I'll put it in the ledger: one less problem.");
        add.accept("voice.supernaturalcraft.crowley.hell.0",
                "Home sweet home. I'd show you around, but they'd recognise me.");
        add.accept("voice.supernaturalcraft.crowley.hell.1",
                "Don't touch anything. Especially the screaming.");
        add.accept("voice.supernaturalcraft.crowley.hell.2",
                "I used to run this place, you know. Better lighting.");
        add.accept("voice.supernaturalcraft.crowley.hell.3",
                "Smell that? Sulphur and bad decisions.");
        add.accept("voice.supernaturalcraft.crowley.heaven.0",
                "Heaven? Not in this suit. I'll wait for you downstairs.");
        add.accept("voice.supernaturalcraft.crowley.heaven.1",
                "Give the feathers my regards. Or don't.");
        add.accept("voice.supernaturalcraft.crowley.heaven.2",
                "Some doors are closed to me, darling. That one's bolted.");
        add.accept("voice.supernaturalcraft.crowley.night.0",
                "The witching hour. My favourite.");
        add.accept("voice.supernaturalcraft.crowley.night.1",
                "Lovely night for a deal at a crossroads.");
        add.accept("voice.supernaturalcraft.crowley.night.2",
                "Out at this hour? People will talk.");
        add.accept("voice.supernaturalcraft.crowley.hurt.0",
                "You're bleeding on my coat.");
        add.accept("voice.supernaturalcraft.crowley.hurt.1",
                "Do try not to die. I've only just got used to you.");
        add.accept("voice.supernaturalcraft.crowley.hurt.2",
                "If you die, who'll I complain to?");
        add.accept("voice.supernaturalcraft.crowley.hurt.3",
                "Drink something. Holy, preferably not.");
        add.accept("voice.supernaturalcraft.crowley.idle.0",
                "Are we going anywhere, or is this a walking tour of nothing?");
        add.accept("voice.supernaturalcraft.crowley.idle.1",
                "I had an empire once. Now I carry your luggage.");
        add.accept("voice.supernaturalcraft.crowley.idle.2",
                "Don't look at me like that. I'm reformed. Mostly.");
        add.accept("voice.supernaturalcraft.crowley.idle.3",
                "A soul is a terrible thing to waste. Just saying.");
        add.accept("voice.supernaturalcraft.crowley.idle.4",
                "Moose, squirrel — I miss the old nicknames.");
        add.accept("voice.supernaturalcraft.crowley.idle.5",
                "Tell me again why I agreed to this.");
        add.accept("voice.supernaturalcraft.crowley.pit.0",
                "The Pit's lot. Amateurs with a dress code.");
        add.accept("voice.supernaturalcraft.crowley.pit.1",
                "Those clowns think they run Hell. Adorable.");
        add.accept("voice.supernaturalcraft.crowley.pit.2",
                "Careful — the Pit's watching. Let it.");
        add.accept("voice.supernaturalcraft.crowley.pit.3",
                "I've had better demons fetch my coffee.");
        add.accept("voice.supernaturalcraft.crowley.fight.0",
                "Allow me.");
        add.accept("voice.supernaturalcraft.crowley.fight.1",
                "Off you go, then.");
        add.accept("voice.supernaturalcraft.crowley.fight.2",
                "I do hate violence. I'm just very good at it.");
        add.accept("voice.supernaturalcraft.crowley.fight.3",
                "Boys — sic 'em.");
        add.accept("voice.supernaturalcraft.crowley.rain.0",
                "Rain. On cashmere. Marvellous.");
        add.accept("voice.supernaturalcraft.crowley.rain.1",
                "Must you walk in the rain? Some of us dry-clean.");
        add.accept("voice.supernaturalcraft.crowley.recruited.0",
                "Right. Partners, then. Don't make it weird.");
        add.accept("voice.supernaturalcraft.crowley.recruited.1",
                "A deal is a deal. I'm a man of my word. Ask anyone. Anyone alive.");
        add.accept("voice.supernaturalcraft.crowley.recruited.2",
                "I'll be your King of Hell in waiting. Lead on.");
        add.accept("voice.supernaturalcraft.crowley.retreat.0",
                "I'll be back, darling. Don't wait up.");
        add.accept("voice.supernaturalcraft.crowley.retreat.1",
                "That's quite enough of that. Back in ten.");
        add.accept("voice.supernaturalcraft.crowley.retreat.2",
                "A tactical withdrawal. Very kingly.");
        add.accept("voice.supernaturalcraft.crowley.back.0",
                "Miss me?");
        add.accept("voice.supernaturalcraft.crowley.back.1",
                "Back. Freshly pressed.");
        add.accept("voice.supernaturalcraft.crowley.back.2",
                "Did anything explode while I was gone? Good.");
        add.accept("voice.supernaturalcraft.crowley.seated.0",
                "Fine. I'll wait. I'm excellent at waiting. Centuries of practice.");
        add.accept("voice.supernaturalcraft.crowley.seated.1",
                "Nice place. Needs a throne.");
        add.accept("voice.supernaturalcraft.crowley.seated.2",
                "I'll be here. Judging your décor.");
        add.accept("voice.supernaturalcraft.crowley.stranger.0",
                "Do I know you? No. Keep it that way.");
        add.accept("voice.supernaturalcraft.crowley.stranger.1",
                "Talk to my associate. I'm on retainer.");
        add.accept("voice.supernaturalcraft.crowley.stranger.2",
                "No deals with strangers. New policy.");
        add.accept("legacy.supernaturalcraft.case.scenario.asylum",
                "an abandoned asylum");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.summon_crowley",
                "Drags Crowley back to his throne (rematch)");
        add.accept("advancement.supernaturalcraft.king_of_the_crossroads",
                "King of the Crossroads");
        add.accept("advancement.supernaturalcraft.king_of_the_crossroads.desc",
                "Beat Crowley in his asylum, and decide his fate");
    }
}
