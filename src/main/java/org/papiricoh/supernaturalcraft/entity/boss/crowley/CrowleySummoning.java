package org.papiricoh.supernaturalcraft.entity.boss.crowley;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.asylum.AsylumCase;
import org.papiricoh.supernaturalcraft.asylum.AsylumFight;
import org.papiricoh.supernaturalcraft.asylum.AsylumWorld;
import org.papiricoh.supernaturalcraft.asylum.layout.AsylumAnchors;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/**
 * How Crowley's fight begins (v0.35).
 * <ul>
 *   <li><b>In his asylum</b> ({@link #begin}): a hunter holding the asylum's case steps through the front door; the arena opens
 *   over the whole building ({@link ArenaTheme#ASYLUM}), the bars fall across every doorway, and he is waiting in the hall.</li>
 *   <li><b>A rematch</b> ({@link #rite}), by the rite {@code summon_crowley} in his throne room, once his fate is decided: the
 *   same fight with no talk at the end. A recruited Crowley steps away from his hunter for it and comes back after.</li>
 *   <li><b>Anywhere</b> ({@link #summon}): commands and tests, a plain arena round the spot.</li>
 * </ul>
 */
public final class CrowleySummoning {

    /** The arena's radius round the asylum's origin (it covers the building), and round a free summoning. */
    public static final int ASYLUM_RADIUS = 34, FREE_RADIUS = 20;
    /** How close to the threshold point starts the fight. */
    public static final double THRESHOLD = 2.5;

    private CrowleySummoning() {
    }

    /** Whether a Crowley fight is on anywhere (the companion keeps away from it). */
    public static boolean fighting(MinecraftServer server) {
        for (ArenaController a : ArenaSavedData.get(server.overworld()).all()) if (a.isActive() && a.theme() == ArenaTheme.ASYLUM) return true;
        return false;
    }

    /** Once a second: a hunter with the case at the asylum's door starts the fight. */
    public static void tick(ServerLevel overworld) {
        CrowleySavedData data = CrowleySavedData.get(overworld.getServer());
        if (data.decided() || !data.asylumBuilt() || fighting(overworld.getServer())) return;
        AsylumFight f = new AsylumFight(data.asylum(), data.asylumQ());
        BlockPos door = f.point(AsylumAnchors.HALL_THRESHOLD);
        if (door == null) return;
        Vec3 at = Vec3.atBottomCenterOf(door);
        for (ServerPlayer p : overworld.players()) {
            if (p.isSpectator() || p.position().distanceToSqr(at) > THRESHOLD * THRESHOLD || AsylumCase.open(p) == null) continue;
            begin(overworld, p, f, false);
            return;
        }
    }

    /** The fight in the asylum: {@code hunter} brought it (their case), {@code rematch} by rite. @return Crowley, or null */
    public static @Nullable CrowleyEntity begin(ServerLevel level, @Nullable ServerPlayer hunter, AsylumFight f, boolean rematch) {
        Component busy = LuciferSummoning.refusal(level);
        if (busy != null) {
            if (hunter != null) hunter.displayClientMessage(busy, true);
            return null;
        }
        ArenaController arena = LuciferSummoning.openArena(level, f.origin(), ASYLUM_RADIUS, ArenaTheme.ASYLUM);
        if (arena == null) return null;
        CrowleyEntity boss = AllEntities.CROWLEY.get().create(level);
        if (boss == null) return null;
        Vec3 hall = rematch ? f.floor(AsylumAnchors.THRONE) : f.roomCentre(1);
        Vec3 face = hunter != null ? hunter.position() : f.floor(AsylumAnchors.HALL_THRESHOLD);
        float yaw = (float) Math.toDegrees(Math.atan2(-(face.x - hall.x), face.z - hall.z));
        boss.moveTo(hall.x, hall.y, hall.z, yaw, 0);
        boss.setYHeadRot(yaw);
        boss.setYBodyRot(yaw);
        boss.bindAsylum(f.origin(), f.q());
        boss.caseOwner(hunter != null ? hunter.getUUID() : null);
        boss.rematch(rematch);
        boss.bindArena(arena);
        if (rematch) {
            CrowleyCompanions.dismiss(level.getServer());
            CrowleySavedData.get(level.getServer()).rematched();
        }
        level.addFreshEntity(boss);
        boss.shutGates(level, arena);
        if (rematch) {
            // The rite is held in the throne room: every door is open behind it, the hall and the cells are not his this time.
            for (String g : AsylumAnchors.GATES) for (BlockPos p : f.gate(g)) arena.mutate(level, p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 0);
        }
        boss.beginEmergence();
        return boss;
    }

    /** The rematch rite, in the throne room. @return whether he answers */
    public static boolean rite(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        Component why = riteRefusal(level, altar);
        if (why != null) {
            if (ritualist != null) ritualist.displayClientMessage(why.copy().withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        AsylumFight f = AsylumWorld.fight(level.getServer());
        return f != null && begin(level, ritualist, f, true) != null;
    }

    /** Why the rite fails here and now, or null: his fate undecided (find him first), or not in his throne room. */
    public static @Nullable Component riteRefusal(ServerLevel level, BlockPos altar) {
        CrowleySavedData data = CrowleySavedData.get(level.getServer());
        if (!data.decided()) return Component.translatable("message.supernaturalcraft.crowley.rite.undecided");
        AsylumFight f = AsylumWorld.fight(level.getServer());
        BlockPos rite = f != null ? f.point(AsylumAnchors.RITE) : null;
        if (rite == null || level.dimension() != net.minecraft.world.level.Level.OVERWORLD || rite.distSqr(altar) > 6 * 6) {
            return Component.translatable("message.supernaturalcraft.crowley.rite.elsewhere");
        }
        return LuciferSummoning.refusal(level);
    }

    /** Anywhere (commands, tests): a plain arena round {@code at}. */
    public static @Nullable CrowleyEntity summon(ServerLevel level, BlockPos at, @Nullable ServerPlayer facing) {
        ArenaController arena = LuciferSummoning.openArena(level, at, FREE_RADIUS, ArenaTheme.ASYLUM);
        if (arena == null) {
            if (facing != null) {
                Component why = LuciferSummoning.refusal(level);
                facing.displayClientMessage(why != null ? why : Component.translatable("message.supernaturalcraft.crowley.busy")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return null;
        }
        CrowleyEntity boss = AllEntities.CROWLEY.get().create(level);
        if (boss == null) return null;
        double x = at.getX() + 0.5, z = at.getZ() + 4.5;
        boss.moveTo(x, at.getY(), z, 180, 0);
        boss.caseOwner(facing != null ? facing.getUUID() : null);
        boss.bindArena(arena);
        level.addFreshEntity(boss);
        boss.beginEmergence();
        return boss;
    }
}
