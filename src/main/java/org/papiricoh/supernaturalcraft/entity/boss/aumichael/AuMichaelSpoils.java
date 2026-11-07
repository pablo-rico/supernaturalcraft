package org.papiricoh.supernaturalcraft.entity.boss.aumichael;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.apocworld.AUStanding;
import org.papiricoh.supernaturalcraft.apocworld.ApocWorldRegistry;
import org.papiricoh.supernaturalcraft.apocworld.ApocWorldSavedData;
import org.papiricoh.supernaturalcraft.apocworld.ApocalypseWorld;
import org.papiricoh.supernaturalcraft.arena.BossShares;
import org.papiricoh.supernaturalcraft.arena.Stakes;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.nephilim.MichaelReturn;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What AU Michael's flight leaves (v0.41): for every hunter who fought him (present, fallen or away, as the boss credit counts
 * them) his advancement, his trophy and his greatcoat, and the road moved on to {@code MICHAEL_FLED}
 * ({@link MichaelReturn#fled}). A hunter away when he fled is settled on their next login ({@link #settle}). The Ascension
 * Shards and the shared boss credit come from his "death", which runs once he is through the rift.
 */
public final class AuMichaelSpoils {

    private AuMichaelSpoils() {
    }

    public static String advancement() {
        return BossProgression.Boss.AU_MICHAEL.advancement;
    }

    /** He is gone through the rift: everyone who fought him gets their share. */
    public static void fled(ServerLevel level, AuMichaelEntity boss) {
        fled(level, boss, List.of());
    }

    /** As above, with {@code also} counted among his fighters whatever the arena says (tests: fake players are no participants). */
    public static void fled(ServerLevel level, AuMichaelEntity boss, Collection<ServerPlayer> also) {
        MinecraftServer server = level.getServer();
        Map<UUID, ServerPlayer> known = new LinkedHashMap<>();
        for (UUID u : BossShares.fighters(level, boss, 64)) known.put(u, null);
        boss.challengers().forEach(p -> known.put(p.getUUID(), p));
        also.forEach(p -> known.put(p.getUUID(), p));
        List<ServerPlayer> here = new ArrayList<>();
        for (Map.Entry<UUID, ServerPlayer> e : known.entrySet()) {
            UUID u = e.getKey();
            ServerPlayer p = e.getValue() != null ? e.getValue() : server.getPlayerList().getPlayer(u);
            List<ItemStack> spoils = List.of(new ItemStack(ApocWorldRegistry.AU_MICHAEL_TROPHY_ITEM.get()),
                    new ItemStack(ApocWorldRegistry.AU_MICHAEL_GREATCOAT.get()));
            if (p == null) {
                Stakes.give(server, u, spoils, false);
                ApocWorldSavedData.get(server).owe(u);
                continue;
            }
            for (ItemStack s : spoils) if (!p.getInventory().add(s)) p.drop(s, false);
            here.add(p);
        }
        for (ServerPlayer p : here) {
            credit(p);
            p.sendSystemMessage(Component.translatable("message.supernaturalcraft.au_michael.fled").withStyle(ChatFormatting.GOLD));
        }
        MichaelReturn.fled(here);
    }

    /** His flight counts for {@code p}: the advancement and their standing. */
    static void credit(ServerPlayer p) {
        ChorusRewards.award(p, advancement());
        ApocalypseWorld.update(p, AUStanding::withMichaelFled);
    }

    /** On login: a hunter who was away when he fled is moved on now. */
    public static void settle(ServerPlayer p) {
        if (!ApocWorldSavedData.get(p.server).settle(p.getUUID())) return;
        credit(p);
        MichaelReturn.fled(List.of(p));
    }

    /** Whether {@code p} has seen him flee (beaten him), by standing or by advancement. */
    public static boolean beaten(ServerPlayer p) {
        return ApocalypseWorld.standing(p).michaelFled() || org.papiricoh.supernaturalcraft.purgatory.PurgatoryWorld.has(p, advancement());
    }
}
