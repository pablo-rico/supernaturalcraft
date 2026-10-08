package org.papiricoh.supernaturalcraft.allegiance;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.supernaturalcraft.crossroads.CrossroadsHooks;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

/**
 * The crossroads' side of the allegiance ({@link CrossroadsHooks.Soul}): "Make me one of you" turns a human into a
 * Crossroads Demon at once, for {@link Toll#CONVERT_HEARTS} hearts; "Bind my soul" makes a collected soul rise a demon on
 * respawn ({@link Allegiance#pendingDemon}) instead of coming back hollow.
 */
public final class AllegianceCrossroads implements CrossroadsHooks.Soul {

    private AllegianceCrossroads() {
    }

    static void install() {
        CrossroadsHooks.soul = new AllegianceCrossroads();
    }

    @Override
    public boolean mayConvert(ServerPlayer player) {
        return Allegiances.get(player).mayChoose(player.serverLevel().getGameTime());
    }

    @Override
    public boolean convert(ServerPlayer player) {
        if (!mayConvert(player)) return false;
        Allegiance next = Allegiances.get(player).convert(Faction.DEMON).withToll(Toll.CONVERT_HEARTS);
        AllegianceRites.ascend(player, next);
        AllegianceFx.tell(player, "message.supernaturalcraft.allegiance.convert_sealed", false, ChatFormatting.DARK_RED, Toll.CONVERT_HEARTS);
        return true;
    }

    @Override
    public void bound(ServerPlayer player) {
        ChorusRewards.award(player, "main/soul_bound");
        AllegianceFx.tell(player, "message.supernaturalcraft.allegiance.soul_bound", false, ChatFormatting.DARK_RED);
    }

    @Override
    public boolean collected(ServerPlayer player) {
        Allegiance a = Allegiances.get(player);
        if (!a.isHuman()) return false;
        Allegiances.set(player, a.withPendingDemon(true));
        AllegianceFx.tell(player, "message.supernaturalcraft.allegiance.soul_taken", false, ChatFormatting.DARK_RED);
        return true;
    }

    /** On respawn: a soul the hounds took comes back a Crossroads Demon. @return whether it did */
    public static boolean rise(ServerPlayer player) {
        Allegiance a = Allegiances.get(player);
        if (!a.pendingDemon()) return false;
        AllegianceRites.ascend(player, a.withPendingDemon(false).convert(Faction.DEMON));
        return true;
    }
}
