package org.papiricoh.supernaturalcraft.datagen.heaven;

import org.papiricoh.supernaturalcraft.entity.boss.zachariah.ZachariahAttacks;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Zachariah's text (owned by the Zachariah work): his lines, memos, form and docket messages, title cards, spoils.
 * <p>v0.18: created by the foundations, called from {@code SNLang.addAll}. Every English string of its owner goes here (a key may
 * only be added once across all lang classes). Keys the client reads: {@code title.supernaturalcraft.zachariah.phase<1-4>} (+
 * {@code .sub}), {@code .death}, {@code .dissolve} (each + {@code .sub}) for {@code ZACHARIAH_TITLE}; {@code
 * docket.supernaturalcraft.zachariah.<attack id>} for each id a {@code FORETOLD}/{@code REVISION} payload names.
 */
public final class ZachariahLang {

    private ZachariahLang() {
    }

    /** What the docket calls each of his attacks. */
    public static final Map<String, String> DOCKET = new LinkedHashMap<>();

    static {
        DOCKET.put(ZachariahAttacks.PAPER_STORM, "Paper Storm");
        DOCKET.put(ZachariahAttacks.RUBBER_STAMP, "Rubber Stamp");
        DOCKET.put(ZachariahAttacks.CLERKS, "Clerical Support");
        DOCKET.put(ZachariahAttacks.PRECEDENT, "Precedent");
        DOCKET.put(ZachariahAttacks.CUBICLE_SHUFFLE, "Cubicle Shuffle");
        DOCKET.put(ZachariahAttacks.TERMINATION, "Termination Notice");
        DOCKET.put(ZachariahAttacks.REASSIGNMENT, "Reassignment");
        DOCKET.put(ZachariahAttacks.WING_BUFFET, "Wing Buffet");
        DOCKET.put(ZachariahAttacks.SMITE_OF_HEAVEN, "Smite of Heaven");
        DOCKET.put(ZachariahAttacks.OVERDUE_SMITE, "Overdue Smite");
        DOCKET.put(ZachariahAttacks.BLINK, "Site Visit");
    }

    public static void add(BiConsumer<String, String> add) {
        String m = "message.supernaturalcraft.zachariah.";
        add.accept(m + "busy", "Another fight already holds this office. Take a number.");
        add.accept(m + "cage_gone", "The office empties. Somewhere, a file is closed without you.");
        add.accept(m + "victorious", "Zachariah: \"Your request has been denied. Have a blessed day.\"");
        add.accept(m + "form_issued", "Zachariah hands you a Heavenly Form: file it in cabinet %s.");
        add.accept(m + "overdue", "Your form is overdue. Heaven is coming to collect.");
        add.accept(m + "approved", "Filed. APPROVED: your blows land harder for a while.");
        add.accept(m + "stamped", "Stamped and filed. APPROVED.");
        add.accept(m + "denied", "DENIED. Your wounds will not close until the paperwork clears.");
        add.accept(m + "reassigned", "You have been reassigned.");
        add.accept(m + "notice", "A Termination Notice, with your name on it. Find an Approved desk, or someone to share it with.");
        add.accept(m + "notice_void", "You stand at an Approved desk: the notice is void.");
        add.accept(m + "notice_shared", "The notice comes due, and you take your share of it.");
        add.accept(m + "notice_served", "The notice comes due.");
        add.accept(m + "wrong_cabinet", "This is cabinet %s. Your form goes in cabinet %s.");
        add.accept(m + "office_closed", "The drawer is locked: nobody is processing forms right now.");
        add.accept(m + "filed_for_another", "Filed on a colleague's behalf. They are Approved.");
        add.accept(m + "nothing_to_stamp", "You have no form waiting to be stamped.");

        String t = "title.supernaturalcraft.zachariah.";
        add.accept(t + "phase1", "Intake");
        add.accept(t + "phase1.sub", "Zachariah will see you now. Please have your forms ready");
        add.accept(t + "phase2", "Review");
        add.accept(t + "phase2.sub", "Your file is under review. The office rearranges itself");
        add.accept(t + "phase3", "It Was Already Written");
        add.accept(t + "phase3.sub", "Read the docket: he does exactly what it says. Almost");
        add.accept(t + "phase4", "Final Judgment");
        add.accept(t + "phase4.sub", "The office falls away: there is only Heaven, and his verdict");
        add.accept(t + "dissolve", "The Office Dissolves");
        add.accept(t + "dissolve.sub", "Walls, desks and ceiling go up into golden light");
        add.accept(t + "death", "Out of Office");
        add.accept(t + "death.sub", "Zachariah's file is closed");

        String c = "cinematic.supernaturalcraft.zachariah.";
        add.accept(c + "victory.title", "Out of Office");
        add.accept(c + "victory.subtitle", "Your home in Heaven is yours");

        for (Map.Entry<String, String> e : DOCKET.entrySet()) add.accept("docket.supernaturalcraft.zachariah." + e.getKey(), e.getValue());

        String tip = "tooltip.supernaturalcraft.";
        add.accept(tip + "heavenly_form.blank", "A blank Heavenly Form. Nobody issued it, so nobody wants it.");
        add.accept(tip + "heavenly_form.cabinet", "File in cabinet %s");
        add.accept(tip + "heavenly_form.rule", "Until it is filed your blows against Zachariah are worth a quarter.");
        add.accept(tip + "approval_stamp", "Use in Zachariah's office: files your Heavenly Form at once, whatever its cabinet.");
        add.accept(tip + "zachariahs_blade", "Every third blow files the target: it takes a quarter more from everything for 3 seconds.");
        add.accept(tip + "weapon.zachariahs_blade", "Every third blow files the target: +25% damage taken from all sources for 3 s.");
        add.accept(tip + "heavens_seal", "The first blow a great enemy lands on you each minute is halved.");
        add.accept(tip + "heavens_seal.wear", "Carry it in your off hand, or wear it as a charm.");

        String d = "decor.supernaturalcraft.zachariah.";
        add.accept(d + "memory", "FILED: %s (%s)");
        add.accept(d + "kind.boss_victory", "A victory");
        add.accept(d + "kind.crossroads_deal", "A deal at the crossroads");
        add.accept(d + "kind.case_solved", "A case closed");
        add.accept(d + "kind.case_lost", "A case lost");
        add.accept(d + "kind.ascension", "An ascension");
        add.accept(d + "kind.heeded_call", "A call heeded");
        add.accept(d + "kind.legacy_rank", "A promotion");
        add.accept(d + "kind.pet_lost", "A friend lost");
        add.accept(d + "kind.first_sighting", "A first sighting");
        add.accept(d + "kind.favourite_prey", "A favourite prey");
        add.accept(d + "motto.0", "PATIENCE IS A VIRTUE. SO IS COMPLIANCE.");
        add.accept(d + "motto.1", "EVERYTHING HAPPENS FOR A REASON. THE REASON IS ON FILE.");
        add.accept(d + "motto.2", "THERE IS NO \"I\" IN HOST.");
        add.accept(d + "motto.3", "IT WAS ALREADY WRITTEN.");
    }
}
