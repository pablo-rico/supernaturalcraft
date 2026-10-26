package org.papiricoh.supernaturalcraft.chain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.chain.narrative.Situation;
import org.papiricoh.supernaturalcraft.chain.seat.SeatWorld;
import org.papiricoh.supernaturalcraft.network.ChainDuelPayload;
import org.papiricoh.supernaturalcraft.network.Send;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Fights between the two sides that a hunter comes upon (server, in memory; v0.27): a skirmish (two officers and their
 * escorts) or a champion's duel (two officers alone, in a ring). While such a {@link Challenge} is pending and a hunter walks
 * within {@link Territory#NOTICE} of either officer's territory, both show themselves there and go at each other. Whoever
 * kills the other wins; a hunter who kills one hands it to the other; after {@link #TIMEOUT} ticks the dice settle it. Hunters
 * who strike one side are remembered as helping the other ({@link ChainLife#clashSettled}). A restart forgets a staged fight:
 * it is settled in the records on its day.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class ClashStage {
    public static final int TIMEOUT = 3 * 60 * 20, RING = 8;
    public static final String TAG = "sn_chain_clash";

    private static final class Stage {
        final int challenge;
        final Officer a, b;
        final Set<UUID> sideA = new HashSet<>(), sideB = new HashSet<>();
        /** Hunters who struck side B (helping A) and side A (helping B). */
        final Set<UUID> helpedA = new HashSet<>(), helpedB = new HashSet<>();
        final long until;
        final BlockPos centre;
        UUID mobA, mobB;

        Stage(int challenge, Officer a, Officer b, long until, BlockPos centre) {
            this.challenge = challenge;
            this.a = a;
            this.b = b;
            this.until = until;
            this.centre = centre;
        }
    }

    private static final Map<Integer, Stage> STAGED = new HashMap<>();

    private ClashStage() {
    }

    public static boolean staged(int challenge) {
        return STAGED.containsKey(challenge);
    }

    /** Every second, from {@link ChainEvents}. */
    public static void tick(MinecraftServer server) {
        var data = ChainSavedData.get(server);
        ServerLevel level = server.overworld();
        long now = level.getGameTime();
        for (Stage stage : List.copyOf(STAGED.values())) {
            Entity a = level.getEntity(stage.mobA), b = level.getEntity(stage.mobB);
            if (now >= stage.until || a == null || b == null) {
                finish(server, stage, null);
                continue;
            }
            goAt(stage, level, stage.sideA, b, stage.sideB);
            goAt(stage, level, stage.sideB, a, stage.sideA);
        }
        if (server.getPlayerList().getPlayers().isEmpty()) return;
        for (Challenge c : data.challenges()) {
            if (c.kind != Challenge.Kind.OFFICER_OFFICER || c.state != Challenge.State.PENDING || STAGED.containsKey(c.id)) continue;
            Officer x = officer(data, c.challenger), y = officer(data, c.defender);
            if (x == null || y == null || !x.alive() || !y.alive() || DuelStage.busy(x) || DuelStage.busy(y)) continue;
            if (!stage(server, c, x, y, x)) stage(server, c, x, y, y);
        }
    }

    static @Nullable Officer officer(ChainSavedData data, Bonds.Actor actor) {
        return actor.player() ? null : data.hierarchy(actor.faction()).get(actor.officerId());
    }

    private static boolean stage(MinecraftServer server, Challenge c, Officer x, Officer y, Officer where) {
        ServerLevel level = server.overworld();
        var site = ChainLife.territory(server, where);
        ServerPlayer viewer = server.getPlayerList().getPlayers().stream().filter(p -> p.level() == level && p.isAlive() && !p.isSpectator()
                && Math.abs(p.getX() - site.x()) < Territory.NOTICE && Math.abs(p.getZ() - site.z()) < Territory.NOTICE).findFirst().orElse(null);
        if (viewer == null || !level.hasChunkAt(new BlockPos(site.x(), 0, site.z()))) return false;
        int y0 = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, site.x(), site.z());
        return show(viewer, c, x, y, new BlockPos(site.x(), y0, site.z()));
    }

    /** Forces a pending fight to show itself a few blocks in front of {@code viewer} (the {@code clash … here} command). */
    public static boolean here(ServerPlayer viewer, Challenge c) {
        var data = ChainSavedData.get(viewer.server);
        Officer x = officer(data, c.challenger), y = officer(data, c.defender);
        if (x == null || y == null || STAGED.containsKey(c.id)) return false;
        return show(viewer, c, x, y, BlockPos.containing(viewer.position().add(viewer.getLookAngle().multiply(1, 0, 1).normalize().scale(10))));
    }

    private static boolean show(ServerPlayer viewer, Challenge c, Officer x, Officer y, BlockPos centre) {
        boolean skirmish = "skirmish".equals(c.reason);
        List<Mob> escortsA = new ArrayList<>(), escortsB = new ArrayList<>();
        Mob a = OfficerSpawner.mob(x), b = OfficerSpawner.mob(y);
        if (a == null) a = OfficerSpawner.around(x, viewer, centre.offset(-5, 0, 0), skirmish, escortsA);
        if (b == null) b = OfficerSpawner.around(y, viewer, centre.offset(5, 0, 0), skirmish, escortsB);
        if (a == null || b == null) return false;
        c.state = Challenge.State.STAGED;
        Stage stage = new Stage(c.id, x, y, viewer.serverLevel().getGameTime() + TIMEOUT, centre);
        stage.mobA = a.getUUID();
        stage.mobB = b.getUUID();
        stage.sideA.add(a.getUUID());
        stage.sideB.add(b.getUUID());
        for (Mob m : escortsA) stage.sideA.add(m.getUUID());
        for (Mob m : escortsB) stage.sideB.add(m.getUUID());
        for (Mob m : List.of(a, b)) m.getPersistentData().putBoolean(TAG, true);
        for (Mob m : escortsA) m.getPersistentData().putBoolean(TAG, true);
        for (Mob m : escortsB) m.getPersistentData().putBoolean(TAG, true);
        STAGED.put(c.id, stage);
        ChainSavedData.get(viewer.server).changed();
        a.setTarget(b);
        b.setTarget(a);
        OfficerVoice.say(a, x, skirmish ? Situation.SKIRMISH_CRY : Situation.DUEL_START, viewer.getGameProfile().getName(), ChainNames.name(y), null, true);
        if (!skirmish) Send.near(viewer.serverLevel(), null, centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, DuelStage.WATCH,
                new ChainDuelPayload(-1 - c.id, false, centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, RING, a.getId(), b.getId(),
                        ChainNames.name(x), ChainNames.name(y), true, -1));
        ChainEvents.notice(viewer, "skirmish_near");
        return true;
    }

    /** Keeps every fighter of one side on the other side, the leader first, unless a hunter has just struck it. */
    private static void goAt(Stage stage, ServerLevel level, Set<UUID> side, Entity leader, Set<UUID> foes) {
        for (UUID id : side) {
            if (!(level.getEntity(id) instanceof Mob mob) || !mob.isAlive()) continue;
            LivingEntity attacker = mob.getLastHurtByMob();
            if (attacker instanceof ServerPlayer && mob.tickCount - mob.getLastHurtByMobTimestamp() < 60) continue;
            if (mob.getTarget() != null && mob.getTarget().isAlive() && foes.contains(mob.getTarget().getUUID())) continue;
            LivingEntity foe = leader instanceof LivingEntity l && l.isAlive() ? l : null;
            if (foe == null) for (UUID f : foes) if (level.getEntity(f) instanceof LivingEntity l && l.isAlive()) { foe = l; break; }
            if (foe != null) mob.setTarget(foe);
        }
    }

    /** Remembers hunters who strike a side, as helping the other. */
    @SubscribeEvent
    public static void struck(LivingDamageEvent.Post event) {
        if (STAGED.isEmpty() || !(event.getSource().getEntity() instanceof ServerPlayer p)) return;
        UUID victim = event.getEntity().getUUID();
        for (Stage s : STAGED.values()) {
            if (s.sideB.contains(victim)) s.helpedA.add(p.getUUID());
            else if (s.sideA.contains(victim)) s.helpedB.add(p.getUUID());
        }
    }

    /** From {@link ChainEvents#officerKilled}: one of a staged fight's officers fell, so the other wins it. */
    public static void fell(MinecraftServer server, Officer dead, @Nullable Entity killer) {
        for (Stage s : List.copyOf(STAGED.values())) {
            if (s.a == dead) finish(server, s, s.b);
            else if (s.b == dead) finish(server, s, s.a);
        }
    }

    private static void finish(MinecraftServer server, Stage stage, @Nullable Officer winner) {
        STAGED.remove(stage.challenge);
        Send.near(server.overworld(), null, stage.centre.getX() + 0.5, stage.centre.getY(), stage.centre.getZ() + 0.5, DuelStage.WATCH,
                ChainDuelPayload.end(-1 - stage.challenge, -1));
        var data = ChainSavedData.get(server);
        Challenge c = null;
        for (Challenge x : data.challenges()) if (x.id == stage.challenge) c = x;
        if (c == null) return;
        Set<UUID> helpers = winner == stage.a ? stage.helpedA : winner == stage.b ? stage.helpedB : Set.of();
        ChainLife.clashSettled(server, c, winner, true, helpers);
        for (Set<UUID> side : List.of(stage.sideA, stage.sideB))
            for (UUID id : side)
                if (server.overworld().getEntity(id) instanceof Mob m) m.getPersistentData().remove(TAG);
    }

    public static void clear() {
        STAGED.clear();
    }
}
