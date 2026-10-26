package org.papiricoh.supernaturalcraft.chain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.chain.narrative.Situation;
import org.papiricoh.supernaturalcraft.network.ChainDuelPayload;
import org.papiricoh.supernaturalcraft.network.Send;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Duels between a hunter and an officer (server, in memory; v0.27): the challenge rite, a challenge an officer threw and the
 * hunter took, a duel for the throne. The officer comes alone ({@link #ARRIVAL} ticks after it is called) to a ring of
 * {@link #RADIUS} blocks; a hunter who stays out of the ring for {@link #OUTSIDE_GRACE} ticks forfeits; after {@link #TIMEOUT}
 * it is a draw. Across the sides the duel is to the death (a kill in it is a proper kill and pays half again); between two of
 * one side the loser yields below {@link Challenge#YIELD_FRACTION} of its health and nobody dies. The ring and the two
 * names reach every player near by {@link ChainDuelPayload}. A restart drops a staged duel: its challenge waits again.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class DuelStage {
    public static final int RADIUS = 8, ARRIVAL = 200, OUTSIDE_GRACE = 60, TIMEOUT = 5 * 60 * 20, WATCH = 64;
    public static final String TAG = "sn_chain_duel";

    public enum End { HUNTER_WON, OFFICER_WON, DRAW }

    private static final class Duel {
        final int challenge;
        final UUID player;
        final ChainFaction faction;
        final int officer;
        final BlockPos centre;
        final long startAt, until;
        final boolean lethal;
        UUID mob;
        int outside;

        Duel(int challenge, UUID player, ChainFaction faction, int officer, BlockPos centre, long startAt, boolean lethal) {
            this.challenge = challenge;
            this.player = player;
            this.faction = faction;
            this.officer = officer;
            this.centre = centre;
            this.startAt = startAt;
            this.until = startAt + TIMEOUT;
            this.lethal = lethal;
        }
    }

    private static final Map<UUID, Duel> DUELS = new HashMap<>();

    private DuelStage() {
    }

    public static boolean busy(UUID player) {
        return DUELS.containsKey(player);
    }

    /** Whether {@code o} is in a duel now (or on its way to one). */
    public static boolean busy(Officer o) {
        return DUELS.values().stream().anyMatch(d -> d.faction == o.faction && d.officer == o.id);
    }

    /** Whether {@code mob} and {@code p} are fighting a duel with each other (their sides do not keep them apart then). */
    public static boolean duelling(Entity mob, Player p) {
        Duel d = DUELS.get(p.getUUID());
        return d != null && mob.getUUID().equals(d.mob);
    }

    /**
     * Calls {@code o} to a duel with {@code player} at {@code centre}, after {@code delay} ticks. @return false if either is
     * already in one or the officer is not alive.
     */
    public static boolean begin(ServerPlayer player, Challenge c, Officer o, BlockPos centre, int delay) {
        if (busy(player.getUUID()) || busy(o) || !o.alive() || o.held()) return false;
        c.state = Challenge.State.STAGED;
        DUELS.put(player.getUUID(), new Duel(c.id, player.getUUID(), o.faction, o.id, centre.immutable(),
                player.serverLevel().getGameTime() + delay, c.lethal));
        ChainSavedData.get(player.server).changed();
        return true;
    }

    /**
     * Starts a duel at once with an officer already standing there (a test, or a Lord on its throne). @return false if either
     * is already in a duel.
     */
    public static boolean startNow(ServerPlayer player, Challenge c, Officer o, Mob mob, BlockPos centre) {
        if (!begin(player, c, o, centre, 0)) return false;
        Duel d = DUELS.get(player.getUUID());
        d.mob = mob.getUUID();
        mob.getPersistentData().putBoolean(TAG, true);
        mob.setTarget(player);
        return true;
    }

    /** Every second, from {@link ChainEvents}. */
    public static void tick(MinecraftServer server) {
        if (DUELS.isEmpty()) return;
        var data = ChainSavedData.get(server);
        for (Duel d : List.copyOf(DUELS.values())) {
            ServerPlayer p = server.getPlayerList().getPlayer(d.player);
            Officer o = data.hierarchy(d.faction).get(d.officer);
            Challenge c = challenge(data, d.challenge);
            if (p == null || o == null || !o.alive() || c == null) { drop(server, d, c); continue; }
            ServerLevel level = p.serverLevel();
            long now = level.getGameTime();
            if (d.mob == null) {
                if (now < d.startAt) continue;
                Mob mob = arrive(p, o, d);
                if (mob == null) { drop(server, d, c); continue; }
                d.mob = mob.getUUID();
                Send.near(level, null, d.centre.getX() + 0.5, d.centre.getY(), d.centre.getZ() + 0.5, WATCH,
                        new ChainDuelPayload(d.challenge, false, d.centre.getX() + 0.5, d.centre.getY(), d.centre.getZ() + 0.5, RADIUS,
                                p.getId(), mob.getId(), p.getGameProfile().getName(), ChainNames.name(o), d.lethal, -1));
                OfficerVoice.say(mob, o, Situation.DUEL_START, p.getGameProfile().getName(), null, null, true);
                ChainEvents.notice(p, "duel_begins", ChainNames.name(o));
                continue;
            }
            Entity mob = level.getEntity(d.mob);
            if (!(mob instanceof Mob m) || !m.isAlive()) { end(server, d, End.DRAW, false); continue; }
            if (now >= d.until) { end(server, d, End.DRAW, false); continue; }
            Vec3 centre = Vec3.atBottomCenterOf(d.centre);
            if (m.position().distanceToSqr(centre) > (RADIUS + 2) * (RADIUS + 2)) m.teleportTo(centre.x, centre.y, centre.z);
            if (m.getTarget() != p) m.setTarget(p);
            if (p.position().distanceToSqr(centre) > (RADIUS + 1) * (RADIUS + 1) || p.level() != level) d.outside += 20;
            else d.outside = 0;
            if (d.outside >= OUTSIDE_GRACE) {
                ChainEvents.notice(p, "duel_forfeit", ChainNames.name(o));
                end(server, d, End.OFFICER_WON, false);
            }
        }
    }

    /** The officer comes alone: the mob already walking the world is pulled in, or a new one appears at the ring's edge. */
    private static @Nullable Mob arrive(ServerPlayer p, Officer o, Duel d) {
        Mob mob = OfficerSpawner.mob(o);
        BlockPos edge = d.centre.offset(RADIUS / 2, 0, 0);
        if (mob == null) mob = OfficerSpawner.alone(o, p, edge);
        else mob.teleportTo(edge.getX() + 0.5, edge.getY(), edge.getZ() + 0.5);
        if (mob == null) return null;
        mob.getPersistentData().putBoolean(TAG, true);
        mob.setTarget(p);
        return mob;
    }

    private static @Nullable Challenge challenge(ChainSavedData data, int id) {
        for (Challenge c : data.challenges()) if (c.id == id) return c;
        return null;
    }

    /** A duel that cannot go on (the hunter left, the officer is gone): its challenge waits again. */
    private static void drop(MinecraftServer server, Duel d, @Nullable Challenge c) {
        DUELS.remove(d.player);
        if (c != null && c.state == Challenge.State.STAGED) c.state = Challenge.State.PENDING;
        broadcastEnd(server, d, -1);
        ChainSavedData.get(server).changed();
    }

    private static void broadcastEnd(MinecraftServer server, Duel d, int winner) {
        ServerPlayer p = server.getPlayerList().getPlayer(d.player);
        ServerLevel level = p != null ? p.serverLevel() : server.overworld();
        Send.near(level, null, d.centre.getX() + 0.5, d.centre.getY(), d.centre.getZ() + 0.5, WATCH, ChainDuelPayload.end(d.challenge, winner));
    }

    /** Settles a duel: {@code died} when the loser fell (only in a duel to the death). */
    private static void end(MinecraftServer server, Duel d, End how, boolean died) {
        DUELS.remove(d.player);
        var data = ChainSavedData.get(server);
        Challenge c = challenge(data, d.challenge);
        Officer o = data.hierarchy(d.faction).get(d.officer);
        ServerPlayer p = server.getPlayerList().getPlayer(d.player);
        Entity mob = p == null || d.mob == null ? null : p.serverLevel().getEntity(d.mob);
        broadcastEnd(server, d, how == End.HUNTER_WON ? p == null ? -1 : p.getId() : how == End.OFFICER_WON && mob != null ? mob.getId() : -1);
        if (c != null) c.state = Challenge.State.DONE;
        if (o != null && c != null) ChainLife.duelSettled(server, c, o, d.player, p, mob, how, died);
        if (mob instanceof Mob m && m.isAlive()) {
            m.getPersistentData().remove(TAG);
            m.setTarget(null);
            // A beaten officer of the hunter's own side walks away rather than die.
            if (how == End.HUNTER_WON) m.discard();
        }
        data.changed();
    }

    /** From {@link ChainEvents#officerKilled}: the officer of a duel fell. */
    public static void fell(MinecraftServer server, Officer dead, @Nullable Entity killer) {
        for (Duel d : List.copyOf(DUELS.values()))
            if (d.faction == dead.faction && d.officer == dead.id) end(server, d, killer instanceof Player ? End.HUNTER_WON : End.DRAW, true);
    }

    /** A kill in a duel to the death counts as a proper kill (whatever the weapon). */
    public static boolean properKill(Entity mob) {
        return mob.getPersistentData().getBoolean(TAG) && DUELS.values().stream().anyMatch(d -> d.lethal && mob.getUUID().equals(d.mob));
    }

    /** A hunter dying in a duel to the death loses it. */
    @SubscribeEvent
    public static void died(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.isCanceled()) return;
        Duel d = DUELS.get(p.getUUID());
        if (d != null && d.mob != null) end(p.server, d, End.OFFICER_WON, true);
    }

    /** Between two of one side nobody dies: the blow that would bring one below the yield line ends the duel instead. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void hurt(LivingIncomingDamageEvent event) {
        if (DUELS.isEmpty() || event.getEntity().level().isClientSide) return;
        LivingEntity victim = event.getEntity();
        for (Duel d : List.copyOf(DUELS.values())) {
            if (d.lethal || d.mob == null) continue;
            boolean hunter = victim.getUUID().equals(d.player), officer = victim.getUUID().equals(d.mob);
            if (!hunter && !officer) continue;
            if (victim.getHealth() - event.getAmount() > victim.getMaxHealth() * Challenge.YIELD_FRACTION) return;
            event.setCanceled(true);
            MinecraftServer server = victim.getServer();
            if (server == null) return;
            Officer o = ChainSavedData.get(server).hierarchy(d.faction).get(d.officer);
            Entity mob = victim.level() instanceof ServerLevel l ? l.getEntity(d.mob) : null;
            if (o != null && mob != null)
                OfficerVoice.say(mob, o, officer ? Situation.YIELD : Situation.DUEL_WON, victim.getName().getString(), null, null, true);
            end(server, d, officer ? End.HUNTER_WON : End.OFFICER_WON, false);
            return;
        }
    }

    public static void clear() {
        DUELS.clear();
    }
}
