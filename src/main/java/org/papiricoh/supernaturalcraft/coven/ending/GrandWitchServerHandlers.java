package org.papiricoh.supernaturalcraft.coven.ending;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceDialogue;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaRelation;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaServerHandlers;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.mark.Marks;

/**
 * Server side of the talk with the Grand Witch at peace (v0.21, P6), over the shared dialogue ({@link GrandWitchDialogue#ID}).
 * Her one service, the Mark's removal, is {@link Unmarking}'s rule: she reads the cure from the Book the hunter kept (they must
 * carry it) and takes a vial of their bound blood.
 */
public final class GrandWitchServerHandlers {

    private GrandWitchServerHandlers() {
    }

    public static void register() {
        AllegianceDialogue.register(GrandWitchDialogue.ID, GrandWitchServerHandlers::answer);
    }

    public static GrandWitchDialogue.Context context(ServerPlayer player) {
        boolean victor = RowenaServerHandlers.relation(player).book() == RowenaRelation.Book.KEPT
                && AuthorWorld.done(player, BossProgression.Boss.ROWENA.advancement);
        return new GrandWitchDialogue.Context(victor, Marks.marked(player));
    }

    public static void open(GrandWitchNpcEntity npc, ServerPlayer player) {
        if (npc.distanceTo(player) > GrandWitchNpcEntity.REACH) return;
        GrandWitchDialogue.Context ctx = context(player);
        npc.getLookControl().setLookAt(player);
        say(player, npc, GrandWitchDialogue.start(ctx), ctx);
    }

    private static void say(ServerPlayer player, Entity npc, String node, GrandWitchDialogue.Context ctx) {
        AllegianceDialogue.ask(player, npc, GrandWitchDialogue.ID, node, GrandWitchDialogue.options(node, ctx), 0);
    }

    public static void answer(ServerPlayer player, @Nullable Entity speaker, String node, String option) {
        if (!(speaker instanceof GrandWitchNpcEntity npc) || !npc.isAlive() || npc.distanceTo(player) > GrandWitchNpcEntity.REACH * 2) return;
        choose(player, npc, node, option);
    }

    /** Answers {@code option} to {@code node}. @return the node she answers with ({@code ""} if the talk ended), or null. Public for tests. */
    public static @Nullable String choose(ServerPlayer player, GrandWitchNpcEntity npc, String node, String option) {
        GrandWitchDialogue.Context ctx = context(player);
        String next = GrandWitchDialogue.next(node, option, ctx);
        if (next == null || next.isEmpty()) return next;
        if (next.equals(GrandWitchDialogue.UNMARK)) {
            String why = Unmarking.why(player, Unmarking.holdsBook(player), true);
            if (why == null) {
                npc.cast();
                Unmarking.perform(player, npc, npc.position().add(0, 1.6, 0), true);
                next = GrandWitchDialogue.UNMARKED;
            } else if (why.endsWith("needs_book")) {
                next = GrandWitchDialogue.NEEDS_BOOK;
            } else if (why.endsWith("needs_blood")) {
                next = GrandWitchDialogue.NEEDS_BLOOD;
            } else if (why.endsWith("not_marked")) {
                next = GrandWitchDialogue.NOT_MARKED;
            } else {
                next = GrandWitchDialogue.STRANGER;
            }
        }
        say(player, npc, next, context(player));
        return next;
    }
}
