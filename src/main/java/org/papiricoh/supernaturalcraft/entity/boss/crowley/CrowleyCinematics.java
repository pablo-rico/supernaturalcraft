package org.papiricoh.supernaturalcraft.entity.boss.crowley;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;
import org.papiricoh.supernaturalcraft.network.Send;

/**
 * The beats of Crowley's fight (v0.35): his intro in the hall ("Hello, boys"), the throne room's reveal, his execution and the
 * victory. The cameras are {@code cinematics/crowley_intro|throne|execution.json} (anchored on him); the title cards are the
 * client's ({@code CrowleyFxPayload.TITLE}); letterbox and flash plain {@link CinematicPayload}s without a title.
 */
public final class CrowleyCinematics {

    private static final double RANGE = 64;
    public static final int INTRO_TICKS = 100, THRONE_TICKS = 80, EXECUTION_TICKS = 140;
    /** His colour: a crossroads red. */
    public static final int FLASH = 0x7A0E0E;

    private CrowleyCinematics() {
    }

    private static void send(LuciferEntity boss, CinematicPayload payload) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) if (p.distanceToSqr(boss) < RANGE * RANGE) Send.toPlayer(p, payload);
    }

    private static void camera(LuciferEntity boss, String name, int ticks) {
        CinematicLocks.play(boss, SupernaturalCraft.asResource("crowley_" + name), ticks, RANGE);
    }

    public static void intro(CrowleyEntity boss) {
        camera(boss, "intro", INTRO_TICKS);
        send(boss, new CinematicPayload(INTRO_TICKS, 0.1f, FLASH, 0.3f, true, "", ""));
        boss.tell("message.supernaturalcraft.crowley.hello", net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.ITALIC);
    }

    /** Into phase {@code to}: a red flash as he goes; the throne room gets its own camera once he sits ({@link CrowleyEntity}). */
    public static void transition(CrowleyEntity boss, int to) {
        send(boss, new CinematicPayload(CrowleyEntity.TRANSITION, 0.2f, FLASH, 0.4f, true, "", ""));
        boss.tell("message.supernaturalcraft.crowley.phase" + to, net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.ITALIC);
    }

    public static void throne(CrowleyEntity boss) {
        camera(boss, "throne", THRONE_TICKS);
    }

    public static void execution(CrowleyEntity boss) {
        camera(boss, "execution", EXECUTION_TICKS);
        send(boss, new CinematicPayload(EXECUTION_TICKS, 0.25f, FLASH, 0.5f, true, "", ""));
    }

    public static void victory(CrowleyEntity boss) {
        boolean recruited = Boolean.TRUE.equals(boss.verdict());
        String k = recruited ? "recruited" : "victory";
        send(boss, new CinematicPayload(80, 0f, 0xFFFFFF, 0.5f, true, "cinematic.supernaturalcraft.crowley." + k + ".title",
                "cinematic.supernaturalcraft.crowley." + k + ".subtitle"));
    }
}
