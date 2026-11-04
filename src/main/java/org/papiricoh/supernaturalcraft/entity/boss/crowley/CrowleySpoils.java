package org.papiricoh.supernaturalcraft.entity.boss.crowley;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceDialogue;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.arena.BossShares;
import org.papiricoh.supernaturalcraft.arena.Stakes;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.util.Players;

import java.util.List;
import java.util.UUID;

/**
 * Crowley's end (v0.35). Beaten, he kneels and bargains: the hunter whose case brought them (else the one who struck last) is
 * asked — same two choices for everyone, in his own words for a demon, an angel or a human: <b>execute</b> him (the long
 * death, his spoils and the Contract Book) or <b>recruit</b> him (he goes in smoke and comes back as their companion). Silence
 * is execution. Everyone else in the fight watches the choice in the chat. One Crowley per world ({@link CrowleySavedData}); a
 * rematch by rite asks nothing (a recruited Crowley's "trial" ends with him back at his hunter's side).
 */
public final class CrowleySpoils {

    public static final String ADVANCEMENT = "main/king_of_the_crossroads";
    public static final String DIALOGUE = "crowley_fate";
    public static final String DEMON = "demon", ANGEL = "angel", HUMAN = "human";
    public static final List<String> NODES = List.of(DEMON, ANGEL, HUMAN);
    /** In this order: silence takes the last (execution). */
    public static final String RECRUIT = "recruit", EXECUTE = "execute";
    public static final List<String> OPTIONS = List.of(RECRUIT, EXECUTE);

    private CrowleySpoils() {
    }

    /** Hooks the talk into the shared dialogue (once, at mod construction). */
    public static void registerDialogue() {
        AllegianceDialogue.register(DIALOGUE, CrowleySpoils::answer);
    }

    /** His words for {@code who}: by what they are. */
    public static String node(LivingEntity who) {
        return Kin.isDemon(who) ? DEMON : Kin.isAngel(who) ? ANGEL : HUMAN;
    }

    /** Who decides: the case's hunter if they are in the fight (or near), else whoever struck last, else the nearest hunter. */
    public static @Nullable ServerPlayer decider(ServerLevel level, CrowleyEntity boss) {
        UUID owner = boss.caseOwner();
        if (owner != null) {
            ServerPlayer p = Players.byUuid(level, owner) instanceof ServerPlayer sp ? sp : null;
            if (p != null && p.isAlive() && p.distanceToSqr(boss) < 64 * 64) return p;
        }
        if (boss.getLastHurtByMob() instanceof ServerPlayer p && p.isAlive()) return p;
        ServerPlayer best = null;
        for (ServerPlayer p : boss.challengers()) if (best == null || p.distanceToSqr(boss) < best.distanceToSqr(boss)) best = p;
        return best;
    }

    /** As he kneels: the talk, or straight to the verdict in a rematch. */
    public static void offer(ServerLevel level, CrowleyEntity boss) {
        CrowleySavedData data = CrowleySavedData.get(level.getServer());
        if (boss.isRematch() || data.decided()) {
            boss.verdict(data.state() == CrowleySavedData.State.RECRUITED, null);
            return;
        }
        if (boss.askedOf() != null) return;
        ServerPlayer who = decider(level, boss);
        if (who == null) {
            boss.verdict(false, null);
            return;
        }
        offerTo(boss, who);
    }

    /** Puts the talk to {@code who} (public for tests: fake players are not in the player index). */
    public static void offerTo(CrowleyEntity boss, ServerPlayer who) {
        boss.askedOf(who.getUUID());
        AllegianceDialogue.ask(who, boss, DIALOGUE, node(who), OPTIONS, CrowleyBalance.TALK_TIMEOUT);
        Component name = who.getDisplayName();
        for (ServerPlayer p : boss.challengers()) {
            if (p == who) continue;
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.crowley.deciding", name)
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
        }
    }

    /** An answer from the shared dialogue. Public for tests (fake players answer by hand). */
    public static void answer(ServerPlayer player, @Nullable Entity speaker, String node, String option) {
        if (!(speaker instanceof CrowleyEntity boss) || !player.getUUID().equals(boss.askedOf())) return;
        decide(player, boss, RECRUIT.equals(option));
    }

    /** The verdict lands: the world remembers it, everyone in the fight hears it, and his death plays out accordingly. */
    public static void decide(@Nullable ServerPlayer by, CrowleyEntity boss, boolean recruit) {
        if (!(boss.level() instanceof ServerLevel level) || boss.verdict() != null) return;
        CrowleySavedData data = CrowleySavedData.get(level.getServer());
        if (!data.decided()) data.decide(recruit ? CrowleySavedData.State.RECRUITED : CrowleySavedData.State.EXECUTED, by != null ? by.getUUID() : null);
        Component name = by != null ? by.getDisplayName() : Component.translatable("message.supernaturalcraft.crowley.nobody");
        String key = "message.supernaturalcraft.crowley." + (recruit ? "recruited" : "executed");
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < 64 * 64) p.displayClientMessage(Component.translatable(key, name).withStyle(ChatFormatting.DARK_RED), false);
        }
        UUID owner = boss.caseOwner();
        if (owner != null && level.getServer().getPlayerList().getPlayer(owner) instanceof ServerPlayer o) {
            org.papiricoh.supernaturalcraft.asylum.AsylumCase.solve(o);
        }
        boss.verdict(recruit, by);
    }

    /** At the very end of his death: the spoils, and his return as a companion if he was recruited. */
    public static void defeated(ServerLevel level, CrowleyEntity boss, List<ServerPlayer> hunters) {
        Vec3 at = boss.position().add(0, 1, 0);
        if (Boolean.TRUE.equals(boss.verdict())) {
            CrowleySavedData data = CrowleySavedData.get(level.getServer());
            CrowleyCompanions.arrive(level, boss.position(), data);
            return;
        }
        ItemStack book = new ItemStack(AllItems.CONTRACT_BOOK.get());
        if (!BossShares.share(level, boss, book)) {
            if (hunters.isEmpty()) spawn(level, at, book.copy());
            for (ServerPlayer p : hunters) Stakes.give(level.getServer(), p.getUUID(), List.of(book.copy()), false);
        }
    }

    private static void spawn(ServerLevel level, Vec3 at, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }
}
