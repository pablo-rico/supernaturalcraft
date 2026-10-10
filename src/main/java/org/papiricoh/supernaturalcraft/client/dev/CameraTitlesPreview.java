package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.fx.ClientChuck;
import org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector;
import org.papiricoh.supernaturalcraft.client.gabriel.ClientGabriel;
import org.papiricoh.supernaturalcraft.client.heaven.ClientHeaven;
import org.papiricoh.supernaturalcraft.client.lucifer.ClientLucifer;
import org.papiricoh.supernaturalcraft.client.michael.ClientMichael;
import org.papiricoh.supernaturalcraft.client.raphael.ClientRaphael;
import org.papiricoh.supernaturalcraft.network.AuthorFxPayload;
import org.papiricoh.supernaturalcraft.network.CameraSequencePayload;
import org.papiricoh.supernaturalcraft.network.GabrielFxPayload;
import org.papiricoh.supernaturalcraft.network.HeavenFxPayload;
import org.papiricoh.supernaturalcraft.network.LuciferFxPayload;
import org.papiricoh.supernaturalcraft.network.MichaelFxPayload;
import org.papiricoh.supernaturalcraft.network.RaphaelFxPayload;

/**
 * {@code SN_PREVIEW=camera_titles}: every boss's own title card fired while a camera sequence has the camera (GUI on), one
 * after another, a screenshot of each at its height. A camera shot hides the HUD's layers; a title layer that is not let
 * through ({@link CameraDirector#showDuringSequences}) shows an empty shot here. Screenshots: {@code sn_camera_titles_*}.
 */
final class CameraTitlesPreview {

    private static final BlockPos BASE = new BlockPos(3200, 0, 3200);
    /** Ticks per card; the first card's tick; the shot inside each slot. */
    private static final int SLOT = 110, START = 40, SHOT = 60;
    /** Who, in order. */
    private static final String[] WHO = {"raphael", "gabriel", "michael", "chuck", "naomi", "lucifer"};

    private static int t = -1;

    private CameraTitlesPreview() {
    }

    static boolean tick(Minecraft mc) {
        String env = System.getenv("SN_PREVIEW");
        if (env == null || !java.util.Arrays.asList(env.split(",")).contains("camera_titles")) return false;
        var server = mc.getSingleplayerServer();
        int now = ++t;
        if (now == 1) {
            mc.options.hideGui = false;
            server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = p.serverLevel();
                p.setGameMode(GameType.SURVIVAL);
                p.getAbilities().invulnerable = true;
                p.onUpdateAbilities();
                level.setDayTime(6000);
                level.setWeatherParameters(6000, 0, false, false);
                level.getChunk(BASE.getX() >> 4, BASE.getZ() >> 4);
                BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BASE);
                p.teleportTo(level, ground.getX() + 0.5, ground.getY() + 1, ground.getZ() + 0.5, 0, -5);
            });
        }
        int end = START + WHO.length * SLOT;
        if (now >= START && now < end && mc.player != null) {
            int slot = (now - START) / SLOT, local = (now - START) % SLOT;
            if (local == 0) fire(mc, WHO[slot]);
            if (local == SHOT) {
                String name = String.format("sn_camera_titles_%s_%s.png", WHO[slot], CameraDirector.active() ? "camera" : "nocamera");
                Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), m -> {
                });
            }
        }
        if (now >= end + 10) mc.stop();
        return true;
    }

    /** The camera shot (Lucifer's, about the player), then that boss's title card as the server would send it. */
    private static void fire(Minecraft mc, String who) {
        Vec3 at = mc.player.position().add(mc.player.getLookAngle().multiply(6, 0, 6));
        CameraDirector.play(new CameraSequencePayload(SupernaturalCraft.asResource("lucifer_p2"), -1, at, mc.player.getYRot() + 180, 100));
        switch (who) {
            case "raphael" -> ClientRaphael.handle(new RaphaelFxPayload(-1, RaphaelFxPayload.TITLE, 2, 0, at, 90));
            case "gabriel" -> ClientGabriel.handle(new GabrielFxPayload(-1, GabrielFxPayload.TITLE, 2, 0, at, 100));
            case "michael" -> ClientMichael.handle(new MichaelFxPayload(-1, MichaelFxPayload.TITLE, 3, 0, at, 120));
            case "chuck" -> ClientChuck.handle(new AuthorFxPayload(0, AuthorFxPayload.CHAPTER_TITLE, 2, at, 0, 0, ""));
            case "naomi" -> ClientHeaven.handleFx(new HeavenFxPayload(-1, HeavenFxPayload.NAOMI_TITLE, 2, 0, at, 110, ""));
            default -> ClientLucifer.handle(new LuciferFxPayload(-1, LuciferFxPayload.TITLE, 2, LuciferFxPayload.LUCIFER, 110));
        }
    }
}
