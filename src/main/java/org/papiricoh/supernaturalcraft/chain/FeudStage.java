package org.papiricoh.supernaturalcraft.chain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * A feud a hunter comes upon (server, in memory): while one is pending ({@link ChainCourt#pending}) and a hunter walks within
 * {@link Territory#NOTICE} of either officer's territory, both show themselves there with their escorts and fight each other.
 * Whoever kills the other wins it; a hunter who kills one hands the feud to the other; after {@link #TIMEOUT} ticks it is
 * settled by the dice. A restart forgets a staged feud: it is settled in the records the next morning.
 */
public final class FeudStage {

    public static final String TAG = "sn_chain_feud";
    public static final int TIMEOUT = 3 * 60 * 20;

    private record Stage(int challenger, int defender, UUID a, UUID b, long until) {
    }

    private static final Map<ChainFaction, Stage> STAGED = new EnumMap<>(ChainFaction.class);

    private FeudStage() {
    }

    public static boolean staged(ChainFaction f) {
        return STAGED.containsKey(f);
    }

    /** Every second, from {@link ChainEvents}. */
    public static void tick(MinecraftServer server) {
        ServerLevel level = server.overworld();
        long now = level.getGameTime();
        for (ChainFaction f : ChainFaction.values()) {
            Stage stage = STAGED.get(f);
            if (stage != null) {
                Entity a = level.getEntity(stage.a()), b = level.getEntity(stage.b());
                if (now >= stage.until() || a == null || b == null) {
                    STAGED.remove(f);
                    if (ChainCourt.pending(server, f) != null) ChainCourt.settle(server, f, null);
                    continue;
                }
                goAt(a, b);
                goAt(b, a);
                continue;
            }
            Feud.Pair pair = ChainCourt.pending(server, f);
            if (pair == null || !pair.challenger().alive() || !pair.defender().alive()) continue;
            stage(server, f, pair, pair.challenger());
            if (!STAGED.containsKey(f)) stage(server, f, pair, pair.defender());
        }
    }

    /** Forces today's feud of {@code f} to show itself a few blocks in front of {@code viewer} (the {@code feud … here} command). */
    public static boolean here(ServerPlayer viewer, ChainFaction f) {
        Feud.Pair pair = ChainCourt.pending(viewer.server, f);
        if (pair == null || staged(f) || !pair.challenger().alive() || !pair.defender().alive()) return false;
        return show(viewer, f, pair, net.minecraft.core.BlockPos.containing(viewer.position().add(viewer.getLookAngle().multiply(1, 0, 1).normalize().scale(8))));
    }

    private static void stage(MinecraftServer server, ChainFaction f, Feud.Pair pair, Officer where) {
        ServerLevel level = server.overworld();
        var site = Territory.of(level.getSeed(), where, org.papiricoh.supernaturalcraft.chain.seat.SeatWorld.site(server, where.faction));
        ServerPlayer viewer = server.getPlayerList().getPlayers().stream().filter(p -> p.level() == level && p.isAlive() && !p.isSpectator()
                && Math.abs(p.getX() - site.x()) < Territory.NOTICE && Math.abs(p.getZ() - site.z()) < Territory.NOTICE).findFirst().orElse(null);
        if (viewer == null || !level.hasChunkAt(new BlockPos(site.x(), 0, site.z()))) return;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, site.x(), site.z());
        show(viewer, f, pair, new BlockPos(site.x(), y, site.z()));
    }

    private static boolean show(ServerPlayer viewer, ChainFaction f, Feud.Pair pair, BlockPos centre) {
        // Already walking the world: fight where they are.
        Mob a = OfficerSpawner.mob(pair.challenger()), b = OfficerSpawner.mob(pair.defender());
        if (a == null) a = OfficerSpawner.around(pair.challenger(), viewer, centre.offset(-4, 0, 0));
        if (b == null) b = OfficerSpawner.around(pair.defender(), viewer, centre.offset(4, 0, 0));
        if (a == null || b == null) return false;
        a.getPersistentData().putBoolean(TAG, true);
        b.getPersistentData().putBoolean(TAG, true);
        STAGED.put(f, new Stage(pair.challenger().id, pair.defender().id, a.getUUID(), b.getUUID(),
                viewer.serverLevel().getGameTime() + TIMEOUT));
        goAt(a, b);
        goAt(b, a);
        ChainEvents.notice(viewer, "feud_seen", ChainNames.name(pair.challenger()), ChainNames.name(pair.defender()));
        return true;
    }

    /** Keeps a feuder on its rival, unless a hunter has just struck it. */
    private static void goAt(Entity self, Entity rival) {
        if (!(self instanceof Mob mob) || !(rival instanceof LivingEntity foe) || !foe.isAlive()) return;
        LivingEntity attacker = mob.getLastHurtByMob();
        if (attacker instanceof ServerPlayer && mob.tickCount - mob.getLastHurtByMobTimestamp() < 60) return;
        if (mob.getTarget() != foe) mob.setTarget(foe);
    }

    /** From {@link ChainEvents#officerKilled}, once the record has died: settles a staged feud one of whose officers fell. */
    public static void fell(MinecraftServer server, Officer dead, @Nullable Entity killer) {
        Stage stage = STAGED.get(dead.faction);
        if (stage == null || stage.challenger() != dead.id && stage.defender() != dead.id) return;
        STAGED.remove(dead.faction);
        Officer rival = ChainSavedData.get(server).hierarchy(dead.faction).get(stage.challenger() == dead.id ? stage.defender() : stage.challenger());
        // Killed by its rival or by a hunter, the feud goes to the rival either way.
        ChainCourt.settle(server, dead.faction, rival != null && rival.alive() ? rival : null);
    }

    public static void clear() {
        STAGED.clear();
    }
}
