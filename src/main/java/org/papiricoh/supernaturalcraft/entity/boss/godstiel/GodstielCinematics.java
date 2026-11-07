package org.papiricoh.supernaturalcraft.entity.boss.godstiel;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;
import org.papiricoh.supernaturalcraft.network.Send;

/**
 * The beats of Godstiel's fight (v0.39): the door to Purgatory opening (Castiel takes the souls and turns on Crowley), "I'm your
 * new God", each phase's turn, the move to the reservoir, his fall and his fate. The cameras are
 * {@code cinematics/godstiel_*.json} (anchored on him, or on the door); the title cards are the client's
 * ({@code GodstielFxPayload.TITLE}); letterbox and flash plain {@link CinematicPayload}s without a title.
 */
public final class GodstielCinematics {

    private static final double RANGE = 72;
    public static final int DOOR_TICKS = 160, INTRO_TICKS = 110, RESERVOIR_TICKS = 80, DEATH_TICKS = 140, FATE_TICKS = 90;
    /** His colours: soul light, then the Leviathans' black. */
    public static final int LIGHT = 0xBFE9FF, GOO = 0x150A1E;

    private GodstielCinematics() {
    }

    private static void send(Entity anchor, CinematicPayload payload) {
        if (!(anchor.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) if (p.distanceToSqr(anchor) < RANGE * RANGE) Send.toPlayer(p, payload);
    }

    private static void camera(Entity anchor, String name, int ticks) {
        CinematicLocks.play(anchor, SupernaturalCraft.asResource("godstiel_" + name), ticks, RANGE);
    }

    /** The door opens and Castiel walks into the light (anchored on Castiel as he stands before it). */
    public static void door(Entity castiel) {
        camera(castiel, "door", DOOR_TICKS);
        send(castiel, new CinematicPayload(DOOR_TICKS, 0.15f, LIGHT, 0.5f, true, "", ""));
    }

    /** "I'm your new God." */
    public static void intro(GodstielEntity boss) {
        camera(boss, "intro", INTRO_TICKS);
        send(boss, new CinematicPayload(INTRO_TICKS, 0.1f, LIGHT, 0.35f, true, "", ""));
        boss.tell("message.supernaturalcraft.godstiel.new_god", ChatFormatting.AQUA, ChatFormatting.ITALIC);
    }

    /** Into phase {@code to}: a flash of light (I-II) or of black (from III); the reservoir gets its own camera. */
    public static void transition(GodstielEntity boss, int to) {
        boolean dark = to >= GodstielBalance.LEVIATHAN_PHASE;
        send(boss, new CinematicPayload(GodstielBalance.TRANSITION, 0.2f, dark ? GOO : LIGHT, 0.45f, true, "", ""));
        if (to == GodstielBalance.RESERVOIR_PHASE && boss.lab() != null) camera(boss, "reservoir", RESERVOIR_TICKS);
        boss.tell("message.supernaturalcraft.godstiel.phase" + to, dark ? ChatFormatting.DARK_PURPLE : ChatFormatting.AQUA, ChatFormatting.ITALIC);
    }

    /** The rematch's end: the Leviathans tear out and he falls. */
    public static void death(GodstielEntity boss) {
        camera(boss, "death", DEATH_TICKS);
        send(boss, new CinematicPayload(DEATH_TICKS, 0.25f, GOO, 0.5f, true, "", ""));
    }

    /** Forgiven (he rises as the goo pours off him) or condemned (the water takes him). */
    public static void fate(GodstielEntity boss, boolean forgiven) {
        camera(boss, "fate", FATE_TICKS);
        String k = forgiven ? "forgiven" : "condemned";
        send(boss, new CinematicPayload(FATE_TICKS, 0.2f, forgiven ? LIGHT : GOO, 0.5f, true, "cinematic.supernaturalcraft.godstiel." + k + ".title",
                "cinematic.supernaturalcraft.godstiel." + k + ".subtitle"));
    }
}
