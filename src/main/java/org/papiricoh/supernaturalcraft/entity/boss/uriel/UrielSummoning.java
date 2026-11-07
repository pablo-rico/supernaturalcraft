package org.papiricoh.supernaturalcraft.entity.boss.uriel;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealWorld;
import org.papiricoh.supernaturalcraft.apocalypse.seal.garrison.GarrisonAnchors;
import org.papiricoh.supernaturalcraft.apocalypse.seal.garrison.GarrisonSite;
import org.papiricoh.supernaturalcraft.apocalypse.seal.garrison.GarrisonWorld;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/**
 * Calling Uriel (v0.40, the Seals slice).
 * <ul>
 *   <li><b>At his garrison</b> ({@link #tick}): by night, a hunter Castiel has warned ({@code SealRecord.envoy} 2) who walks up to
 *   where he waits ({@link GarrisonAnchors#WAKE_REACH}) finds him there, and the fight begins ({@link #begin}): the arena over the
 *   whole yard, the gate barred, his escort of rebel angels.</li>
 *   <li><b>Anywhere</b> ({@link #summon}): commands and tests, a plain arena round the spot.</li>
 * </ul>
 */
public final class UrielSummoning {

    /** The arena's radius round a free summoning. */
    public static final int FREE_RADIUS = 22;

    private UrielSummoning() {
    }

    /** Whether an Uriel fight is on anywhere. */
    public static boolean fighting(MinecraftServer server) {
        for (ArenaController a : ArenaSavedData.get(server.overworld()).all()) if (a.isActive() && a.theme() == ArenaTheme.GARRISON) return true;
        return false;
    }

    /** Whether {@code p} is the kind of hunter he waits for: warned by Castiel. */
    public static boolean awaited(ServerPlayer p) {
        return SealWorld.record(p).envoy() >= 2;
    }

    /** Each few seconds: a warned hunter at the garrison by night wakes the fight. */
    public static void tick(ServerLevel overworld) {
        GarrisonSite site = GarrisonWorld.site(overworld.getServer());
        if (site == null || GarrisonWorld.building() || overworld.isDay() || fighting(overworld.getServer())) return;
        for (ServerPlayer p : overworld.players()) {
            if (!p.isAlive() || p.isSpectator() || !awaited(p) || !site.near(p.position(), GarrisonAnchors.WAKE_REACH)) continue;
            if (LuciferSummoning.refusal(overworld) != null) return;
            begin(overworld, site, p);
            return;
        }
    }

    /** The fight at the garrison. */
    public static @Nullable UrielEntity begin(ServerLevel level, GarrisonSite site, @Nullable ServerPlayer hunter) {
        Component busy = LuciferSummoning.refusal(level);
        if (busy != null) {
            if (hunter != null) hunter.displayClientMessage(busy, true);
            return null;
        }
        BlockPos centre = site.point(GarrisonAnchors.ARENA_CENTRE);
        ArenaController arena = LuciferSummoning.openArena(level, centre != null ? centre : site.origin(), GarrisonAnchors.ARENA_RADIUS, ArenaTheme.GARRISON);
        if (arena == null) return null;
        UrielEntity boss = AllEntities.URIEL.get().create(level);
        if (boss == null) return null;
        Vec3 at = site.floor(GarrisonAnchors.URIEL);
        Vec3 face = hunter != null ? hunter.position() : site.floor(GarrisonAnchors.ENTRY);
        float yaw = (float) Math.toDegrees(Math.atan2(-(face.x - at.x), face.z - at.z));
        boss.moveTo(at.x, at.y, at.z, yaw, 0);
        boss.setYHeadRot(yaw);
        boss.setYBodyRot(yaw);
        boss.bindGarrison(site);
        boss.bindArena(arena);
        level.addFreshEntity(boss);
        boss.barGate(level, arena);
        boss.beginEmergence();
        return boss;
    }

    /** @return the boss, or null (and a message to the caller) if it cannot come here now */
    public static @Nullable UrielEntity summon(ServerLevel level, BlockPos at, @Nullable ServerPlayer caller) {
        ArenaController arena = LuciferSummoning.openArena(level, at, FREE_RADIUS, ArenaTheme.GARRISON);
        if (arena == null) {
            if (caller != null) {
                Component why = LuciferSummoning.refusal(level);
                if (why != null) caller.displayClientMessage(why, true);
            }
            return null;
        }
        UrielEntity boss = AllEntities.URIEL.get().create(level);
        if (boss == null) return null;
        boss.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        boss.bindArena(arena);
        level.addFreshEntity(boss);
        boss.beginEmergence();
        return boss;
    }

    /** Whether {@code p} has brought him down. */
    public static boolean beaten(ServerPlayer p) {
        return AuthorWorld.done(p, BossProgression.Boss.URIEL.advancement);
    }
}
