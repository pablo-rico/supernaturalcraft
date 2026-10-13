package org.papiricoh.supernaturalcraft.allegiance.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.heaven.HeavenDimension;
import org.papiricoh.supernaturalcraft.heaven.passage.HeavenPassage;
import org.papiricoh.supernaturalcraft.heaven.passage.HeavenStanding;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlot;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlots;
import org.papiricoh.supernaturalcraft.hell.HellDimension;
import org.papiricoh.supernaturalcraft.hell.rift.HellRift;
import org.papiricoh.supernaturalcraft.hell.rift.HellRifts;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.List;

/**
 * The General's and the King's crossings (v0.20). Ascend takes an angel and the friends who kneel beside them up to the
 * angel's plot of Heaven (each keeps their own way back, as through a gate); in Heaven it takes them back down, each along
 * their way back. Descend takes a demon and their kneeling friends down to Hell, where a rift would have led; in Hell it
 * takes them back up, each to where they sleep (a King's way out, besides a rift or the rite of escape). Neither works in
 * a boss's arena, and none of it moves anyone who is fighting there.
 */
public final class Crossings {

    private Crossings() {
    }

    /** Whether {@code p} stands in a fight's arena (a boss, a memory, the Cage). */
    public static boolean inBattle(ServerPlayer p) {
        return ArenaSavedData.get(p.serverLevel()).at(p.position()) != null;
    }

    /** Who goes along: the caster and the friends who kneel near, none of them inside an arena. */
    static List<ServerPlayer> party(ServerPlayer p, int radius) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer q : PowerTargets.party(p, p.serverLevel().players(), radius)) if (q == p || !inBattle(q)) out.add(q);
        return out;
    }

    // --- Ascend ----------------------------------------------------------------------------------------------------------

    static @Nullable ActivePowers.Result ascend(ServerPlayer p, int radius) {
        List<ServerPlayer> party = party(p, radius);
        if (HeavenDimension.isHeaven(p.serverLevel())) return leaveHeaven(party) ? new ActivePowers.Result(null, p.position(), 40) : null;
        return ascend(p, HeavenPlots.level(p.server), party);
    }

    /** {@code party} (the caster first) goes up to the caster's plot in {@code heaven}; guests only if visits are allowed. */
    public static @Nullable ActivePowers.Result ascend(ServerPlayer p, ServerLevel heaven, List<ServerPlayer> party) {
        HeavenPlot plot = HeavenPlots.ensure(heaven, p);
        Vec3 landing = HeavenPlots.landing(heaven, plot);
        int i = 0;
        for (ServerPlayer q : party) {
            if (q != p && !SNConfig.HEAVEN_VISITS.get()) continue;
            ActivePowers.release(q);
            ServerLevel from = q.serverLevel();
            from.sendParticles(AllParticles.GRACE.get(), q.getX(), q.getY() + 1, q.getZ(), 30, 0.3, 0.8, 0.3, 0.08);
            HeavenPassage.enter(from, q, q.position());
            go(q, heaven, landing.add(offset(i++)), HeavenPlots.LANDING_YAW);
            HeavenPassage.arrived(q, plot);
        }
        heaven.playSound(null, BlockPos.containing(landing), AllSounds.ALLEGIANCE_ASCEND_ANGEL.get(), SoundSource.PLAYERS, 1f, 1.2f);
        return new ActivePowers.Result(null, landing, 40);
    }

    /** Back down out of Heaven, each along their own way back. @return whether anyone went */
    public static boolean leaveHeaven(List<ServerPlayer> party) {
        boolean any = false;
        for (ServerPlayer q : party) {
            HeavenStanding.Link link = HeavenPassage.exitFor(q.serverLevel(), q);
            ServerLevel to = q.server.getLevel(link.dimension());
            if (to == null) continue;
            ActivePowers.release(q);
            go(q, to, link.pos(), q.getYRot());
            HeavenPassage.clearReturn(q);
            any = true;
        }
        return any;
    }

    // --- Descend ---------------------------------------------------------------------------------------------------------

    static @Nullable ActivePowers.Result descend(ServerPlayer p, int radius) {
        List<ServerPlayer> party = party(p, radius);
        if (HellDimension.isHell(p.serverLevel())) return leaveHell(p.serverLevel(), party) ? new ActivePowers.Result(null, p.position(), 40) : null;
        ServerLevel hell = p.server.getLevel(HellDimension.LEVEL);
        if (hell == null) return null;
        double scale = DimensionType.getTeleportationScale(p.level().dimensionType(), hell.dimensionType());
        int[] xz = HellRifts.scaledTarget(p.getX(), p.getZ(), scale, hell.getWorldBorder().getSize() / 2);
        BlockPos at = HellRifts.findLanding(hell, xz[0], xz[1], Direction.Axis.X);
        return descend(p, hell, Vec3.atBottomCenterOf(at), party);
    }

    /** {@code party} goes down into {@code hell} at {@code landing}. */
    public static ActivePowers.Result descend(ServerPlayer p, ServerLevel hell, Vec3 landing, List<ServerPlayer> party) {
        int i = 0;
        for (ServerPlayer q : party) {
            ActivePowers.release(q);
            q.serverLevel().sendParticles(AllParticles.HELLFIRE.get(), q.getX(), q.getY() + 1, q.getZ(), 30, 0.3, 0.8, 0.3, 0.05);
            go(q, hell, landing.add(offset(i++)), q.getYRot());
        }
        hell.playSound(null, BlockPos.containing(landing), AllSounds.ALLEGIANCE_ASCEND_DEMON.get(), SoundSource.PLAYERS, 1f, 0.8f);
        return new ActivePowers.Result(null, landing, 40);
    }

    /** Back up out of Hell, each to where they sleep (or the world's spawn). @return whether anyone went */
    public static boolean leaveHell(ServerLevel hell, List<ServerPlayer> party) {
        boolean any = false;
        for (ServerPlayer q : party) {
            HellRift.Link home = HellRifts.home(hell, q);
            ServerLevel to = q.server.getLevel(home.dimension());
            if (to == null) continue;
            ActivePowers.release(q);
            go(q, to, home.pos(), q.getYRot());
            any = true;
        }
        return any;
    }

    // --- moving ----------------------------------------------------------------------------------------------------------

    /** The friends stand round the caster, not inside them. */
    static Vec3 offset(int i) {
        if (i == 0) return Vec3.ZERO;
        double a = i * Math.PI * 2 / 6;
        return new Vec3(Math.cos(a) * 1.5, 0, Math.sin(a) * 1.5);
    }

    /** Moves {@code q} to {@code pos} in {@code to} (another level, or theirs: test players have no connection). */
    static void go(ServerPlayer q, ServerLevel to, Vec3 pos, float yaw) {
        if (to == q.serverLevel()) {
            HeavenPassage.move(q, pos, yaw);
            return;
        }
        q.fallDistance = 0;
        q.setDeltaMovement(Vec3.ZERO);
        q.teleportTo(to, pos.x, pos.y, pos.z, yaw, q.getXRot());
    }
}
