package org.papiricoh.supernaturalcraft.cain;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceDialogue;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.entity.boss.cain.CainEntity;
import org.papiricoh.supernaturalcraft.mark.Marks;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;
import org.papiricoh.supernaturalcraft.util.WorldClock;

/**
 * Server side of the talk with Cain (v0.21), over the shared dialogue ({@link AllegianceDialogue}, id {@link CainDialogue#ID}):
 * opening it when a hunter uses him ({@code main/met_cain} the first time), every answer after that, the jar of honey (one a
 * day) and the challenge, which starts his fight in his yard ({@link CainWorld#challenge}).
 */
public final class CainServerHandlers {

    public static final String MET = "main/met_cain";

    private CainServerHandlers() {
    }

    /** Hooks him into the shared dialogue (once, at mod construction). */
    public static void register() {
        AllegianceDialogue.register(CainDialogue.ID, CainServerHandlers::answer);
    }

    /** What he knows of {@code player}. */
    public static CainDialogue.Context context(ServerPlayer player) {
        boolean night = !SNConfig.CAIN_NIGHT_ONLY.get() || player.level().isNight();
        long today = WorldClock.day(player.server) / 24000L;
        return new CainDialogue.Context(AuthorWorld.done(player, MET), AuthorWorld.done(player, CainWorld.DEVIL_WENT_DOWN), night,
                player.getData(AllAttachments.CAIN_HONEY_DAY) == today, Marks.marked(player));
    }

    /** {@code player} used him: he turns and greets them. */
    public static void open(CainNpcEntity npc, ServerPlayer player) {
        if (npc.distanceTo(player) > CainNpcEntity.REACH) return;
        CainDialogue.Context ctx = context(player);
        String node = CainDialogue.start(ctx);
        if (!ctx.met()) ChorusRewards.award(player, MET);
        npc.talkTo(player);
        say(player, npc, node, ctx);
    }

    private static void say(ServerPlayer player, Entity npc, String node, CainDialogue.Context ctx) {
        AllegianceDialogue.ask(player, npc, CainDialogue.ID, node, CainDialogue.options(node, ctx), 0);
    }

    /** An answer from the shared dialogue (already checked against the question asked). */
    public static void answer(ServerPlayer player, @Nullable Entity speaker, String node, String option) {
        if (!(speaker instanceof CainNpcEntity npc) || !npc.isAlive() || npc.distanceTo(player) > CainNpcEntity.REACH * 2) return;
        choose(player, npc, node, option);
    }

    /**
     * Answers {@code option} to {@code node}. @return the node he answers with ({@code ""} if the talk ended,
     * {@link CainDialogue#FIGHT} if the fight started), or null if refused. Public for tests (fake players answer by hand).
     */
    public static @Nullable String choose(ServerPlayer player, CainNpcEntity npc, String node, String option) {
        CainDialogue.Context ctx = context(player);
        String next = CainDialogue.next(node, option, ctx);
        if (next == null) return null;
        if (next.isEmpty()) {
            npc.stopTalking();
            return next;
        }
        if (next.equals(CainDialogue.FIGHT)) {
            npc.stopTalking();
            AllegianceDialogue.close(player, npc);
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.cain.challenge").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), false);
            CainEntity boss = CainWorld.challenge(player.serverLevel(), npc, player);
            return boss != null ? next : null;
        }
        if (next.equals(CainDialogue.HONEY)) {
            player.setData(AllAttachments.CAIN_HONEY_DAY, WorldClock.day(player.server) / 24000L);
            ItemStack jar = new ItemStack(AllItems.CAINS_HONEY.get());
            if (!player.getInventory().add(jar)) player.drop(jar, false);
        }
        npc.talkTo(player);
        say(player, npc, next, context(player));
        return next;
    }
}
