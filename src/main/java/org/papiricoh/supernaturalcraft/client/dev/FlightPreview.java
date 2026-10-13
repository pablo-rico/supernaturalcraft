package org.papiricoh.supernaturalcraft.client.dev;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;

import java.util.UUID;

/**
 * DevPreview scene {@code flight} (v0.20): the flight pose seen from the side. A stand-in angel (a client-side player, as
 * another player on a server is) hovers before the camera, cruises past, dives, climbs, banks round in a circle and lands;
 * then the local player flies off and is shot from behind. Screenshots {@code sn_flight_*.png}.
 */
final class FlightPreview {

    private static final String SCENES = System.getenv("SN_PREVIEW");
    private static final int DUMMY_ID = 987_654;
    private static int tick = -1;
    private static volatile Vec3 origin;
    private static RemotePlayer dummy;

    private FlightPreview() {
    }

    static boolean tick(Minecraft mc) {
        if (!"flight".equals(SCENES)) return false;
        var server = mc.getSingleplayerServer();
        if (server == null || mc.level == null || mc.player == null) return true;
        if (tick < 0) {
            tick = 0;
            mc.options.hideGui = true;
            server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                var level = p.serverLevel();
                level.setDayTime(6000);
                level.setWeatherParameters(12000, 0, false, false);
                p.setGameMode(GameType.SURVIVAL);
                Allegiance a = Allegiance.HUMAN.convert(Faction.ANGEL).withRank(2);
                Allegiances.set(p, a.withEssence(a.maxEssence()));
                int x = p.getBlockX() + 600, z = p.getBlockZ() + 600;
                level.getChunk(x >> 4, z >> 4);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 6;
                p.teleportTo(level, x + 0.5, y, z - 4.5, 0, 10);
                origin = new Vec3(x + 0.5, y, z + 0.5);
                // The camera stands on a pane of glass (flying, the local player would shed feathers into the shot).
                level.setBlockAndUpdate(net.minecraft.core.BlockPos.containing(x + 0.5, y - 1, z - 4.5),
                        net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState());
                p.getAbilities().invulnerable = true;
                p.getAbilities().mayfly = true;
                p.onUpdateAbilities();
            });
            return true;
        }
        int t = ++tick;
        if (t == 30 && origin != null) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            dummy = new RemotePlayer(mc.level, new GameProfile(UUID.nameUUIDFromBytes("sn-flight".getBytes()), "Castiel"));
            dummy.setId(DUMMY_ID);
            // Rank II: wings, but not yet the shining ones (their glow would hide the body).
            Allegiance a = Allegiance.HUMAN.convert(Faction.ANGEL).withRank(2);
            dummy.setData(AllAttachments.ALLEGIANCE, a);
            place(origin, 90);
            mc.level.addEntity(dummy);
        }
        if (dummy != null && t > 30) {
            fly(mc, t);
            if (t < 340) {
                // The camera holds still five blocks south of the path, looking along +z at it.
                mc.player.setPos(origin.x, origin.y, origin.z - 5);
                mc.player.setDeltaMovement(Vec3.ZERO);
                mc.player.setYRot(0);
                mc.player.setXRot(8);
            }
        }
        for (int shot : new int[]{70, 78, 86, 140, 196, 226, 262, 296, 306, 316, 370, 390, 450}) {
            if (t == shot) Screenshot.grab(mc.gameDirectory, String.format("sn_flight_%04d.png", t), mc.getMainRenderTarget(), msg -> {
            });
        }
        if (t >= 470) {
            if (dummy != null) mc.level.removeEntity(DUMMY_ID, Entity.RemovalReason.DISCARDED);
            mc.stop();
        }
        return true;
    }

    /** The stand-in's path: hover, a pass from left to right, a dive, a climb, a banked circle, a landing. */
    private static void fly(Minecraft mc, int t) {
        if (t < 100) {
            place(origin, 90);                                                    // hovering, side on
        } else if (t < 180) {
            place(origin.add(-22 + (t - 100) * 0.55, 0, 0), -90);                  // cruising past (+x), in front at t 140
        } else if (t < 240) {
            place(origin.add(-14 + (t - 180) * 0.5, 9 - (t - 180) * 0.3, 0), -90); // diving
        } else if (t < 285) {
            place(origin.add(-12 + (t - 240) * 0.5, -6 + (t - 240) * 0.3, 0), -90); // climbing
        } else if (t < 330) {
            double a = (t - 285) * 0.12;                                           // a tight banked circle before the camera
            Vec3 at = origin.add(Math.sin(a) * 3, 0, 1.5 - Math.cos(a) * 3);
            place(at, (float) (-a * 180 / Math.PI) - 90);
        } else if (t < 340) {
            dummy.setOnGround(true);
            place(new Vec3(origin.x, mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(origin.x), (int) Math.floor(origin.z)),
                    origin.z), 90);
        } else {
            // The dummy has landed; now the local player flies off ahead, seen from behind.
            if (t == 340) {
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                var server = mc.getSingleplayerServer();
                server.execute(() -> {
                    ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                    p.teleportTo(p.serverLevel(), origin.x, origin.y + 3, origin.z - 5, -60, 10);
                    p.getAbilities().flying = true;
                    p.onUpdateAbilities();
                });
            }
            // Looking along +z but flying along +x: the camera behind sees the body side on (to t 400), then straight on.
            boolean side = t < 400;
            mc.player.setYRot(side ? 0 : -90);
            mc.player.setXRot(10);
            mc.player.setDeltaMovement(new Vec3(0.6, 0.02, 0));
        }
    }

    private static void place(Vec3 at, float yaw) {
        if (!dummy.onGround() || at.y > origin.y - 3) dummy.setOnGround(false);
        dummy.setPos(at.x, at.y, at.z);
        dummy.setYRot(yaw);
        dummy.setYBodyRot(yaw);
        dummy.setYHeadRot(yaw);
        dummy.yBodyRotO = yaw;
        dummy.yHeadRotO = yaw;
        dummy.setXRot(0);
    }
}
