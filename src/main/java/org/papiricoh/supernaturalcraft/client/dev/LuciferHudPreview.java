package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector;
import org.papiricoh.supernaturalcraft.client.lucifer.ClientLucifer;
import org.papiricoh.supernaturalcraft.client.lucifer.LuciferGui;
import org.papiricoh.supernaturalcraft.network.CameraSequencePayload;
import org.papiricoh.supernaturalcraft.network.LuciferFxPayload;

import java.util.Arrays;
import java.util.List;

/**
 * The Cage's HUD in the real renderer (GUI on), far from the origin (3000, 3000): {@code SN_PREVIEW=lucifer_hud},
 * {@code uncaged_hud} or both, one after the other. A boss bar under that Lucifer's keys goes through every phase (the bars coming down or torn off),
 * is struck a few times in each, and every title card plays in turn (the opening, each phase, the victory), as the server
 * would send them, each with its camera sequence (the titles must be seen through the camera shots). Screenshots: {@code sn_lucifer_hud_*} and {@code sn_uncaged_hud_*}.
 */
final class LuciferHudPreview {

    private static final BlockPos BASE = new BlockPos(3000, 0, 3000);
    /** Ticks per phase, and when the first begins. */
    private static final int BLOCK = 120, START = 40;
    /** Within a phase: the shots (the chain taut, bursting, the title burned in, the bar struck). */
    private static final int[] SHOTS = {12, 22, 62, 100};

    private static int t = -1, run;
    private static ServerBossEvent bar;

    private LuciferHudPreview() {
    }

    static boolean tick(Minecraft mc) {
        String env = System.getenv("SN_PREVIEW");
        List<String> scenes = env == null ? List.of()
                : Arrays.stream(env.split(",")).filter(s -> s.equals("lucifer_hud") || s.equals("uncaged_hud")).toList();
        if (scenes.isEmpty()) return false;
        String scene = scenes.get(Math.min(run, scenes.size() - 1));
        boolean uncaged = "uncaged_hud".equals(scene);
        var server = mc.getSingleplayerServer();
        int now = ++t;
        int phases = LuciferGui.maxPhase(uncaged), victory = START + phases * BLOCK, end = victory + 150;
        server.execute(() -> serverSide(mc, server.getPlayerList().getPlayers().getFirst(), uncaged, now, phases, victory, end));
        // The title cards, played on the client as the server would send them.
        if (now >= START && now < victory && (now - START) % BLOCK == 0) {
            int phase = (now - START) / BLOCK + 1;
            int which = phase == 1 ? LuciferFxPayload.TITLE_INTRO : phase;
            ClientLucifer.handle(new LuciferFxPayload(-1, LuciferFxPayload.TITLE, which,
                    uncaged ? LuciferFxPayload.UNCAGED : LuciferFxPayload.LUCIFER, phase == phases ? 160 : 110));
            // The camera shot the title comes with, about the player (there is no Lucifer here to frame).
            if (mc.player != null) {
                String shot = (uncaged ? "uncaged_" : "lucifer_") + (phase == 1 ? "intro" : "p" + phase);
                CameraDirector.play(new CameraSequencePayload(SupernaturalCraft.asResource(shot), -1,
                        mc.player.position().add(mc.player.getLookAngle().multiply(6, 0, 6)), mc.player.getYRot() + 180, 100));
            }
        }
        if (now == victory) {
            ClientLucifer.handle(new LuciferFxPayload(-1, LuciferFxPayload.TITLE, LuciferFxPayload.TITLE_VICTORY,
                    uncaged ? LuciferFxPayload.UNCAGED : LuciferFxPayload.LUCIFER, 150));
        }
        if (now >= START && now < victory) {
            int local = (now - START) % BLOCK;
            for (int s : SHOTS) if (local == s) grab(mc, scene, now);
        }
        if (now == victory + 20 || now == victory + 48 || now == victory + 80) grab(mc, scene, now);
        if (now >= end) {
            if (run + 1 < scenes.size()) {
                run++;
                t = -1;
            } else {
                mc.stop();
            }
        }
        return true;
    }

    private static void serverSide(Minecraft mc, ServerPlayer p, boolean uncaged, int now, int phases, int victory, int end) {
        ServerLevel level = p.serverLevel();
        String prefix = uncaged ? "entity.supernaturalcraft.lucifer_uncaged.bar.phase" : "entity.supernaturalcraft.lucifer.bar.phase";
        if (now == 1) {
            mc.options.hideGui = false;
            p.setGameMode(GameType.SURVIVAL);
            p.getAbilities().invulnerable = true;
            p.onUpdateAbilities();
            level.setDayTime(6000);
            level.setWeatherParameters(6000, 0, false, false);
            level.getChunk(BASE.getX() >> 4, BASE.getZ() >> 4);
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BASE);
            p.teleportTo(level, ground.getX() + 0.5, ground.getY() + 1, ground.getZ() + 0.5, 0, -5);
        }
        if (now == START - 10) {
            bar = new ServerBossEvent(Component.translatable(prefix + 1), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
            bar.setDarkenScreen(true);
            bar.addPlayer(p);
        }
        if (bar == null) return;
        if (now >= START && now < victory) {
            int phase = (now - START) / BLOCK + 1, local = (now - START) % BLOCK;
            if (local == 0) {
                bar.setName(Component.translatable(prefix + phase));
                bar.setProgress(1 - (phase - 1) / (float) phases);
            }
            // Struck three times in each phase.
            if (local == 70 || local == 78 || local == 86) bar.setProgress(Math.max(0, bar.getProgress() - 0.035f));
        }
        if (now == victory) bar.setProgress(0);
        if (now == end - 2) {
            bar.removeAllPlayers();
            bar = null;
        }
    }

    private static void grab(Minecraft mc, String scene, int now) {
        Screenshot.grab(mc.gameDirectory, String.format("sn_%s_%04d.png", scene, now), mc.getMainRenderTarget(), m -> {
        });
    }
}
