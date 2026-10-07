package org.papiricoh.supernaturalcraft.network.client;

import org.papiricoh.supernaturalcraft.client.ClientArcana;
import org.papiricoh.supernaturalcraft.client.arena.ClientArenas;
import org.papiricoh.supernaturalcraft.client.cinematic.ClientCinematics;
import org.papiricoh.supernaturalcraft.network.ArcanaSyncPayload;
import org.papiricoh.supernaturalcraft.network.ArenaStatePayload;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;

/** Client-side payload handling. Only ever referenced from lambdas in SNNetworking. */
public final class ClientPayloadHandlers {

    private ClientPayloadHandlers() {
    }

    public static void handleColtShot(org.papiricoh.supernaturalcraft.network.ColtShotPayload payload) {
        org.papiricoh.supernaturalcraft.client.colt.ColtClient.onShot(payload);
    }

    public static void handleColtAction(org.papiricoh.supernaturalcraft.network.ColtActionPayload payload) {
        org.papiricoh.supernaturalcraft.client.colt.ColtClient.onAction(payload);
    }

    public static void handleArcanaSync(ArcanaSyncPayload payload) {
        ClientArcana.update(payload);
    }

    public static void handleCinematic(CinematicPayload payload) {
        ClientCinematics.play(payload);
    }

    public static void handleArenaState(ArenaStatePayload payload) {
        ClientArenas.update(payload);
    }

    public static void handleEclipse(org.papiricoh.supernaturalcraft.network.EclipseStatePayload payload) {
        org.papiricoh.supernaturalcraft.client.eclipse.ClientEclipse.apply(payload);
    }

    public static void handleCameraSequence(org.papiricoh.supernaturalcraft.network.CameraSequencePayload payload) {
        org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector.play(payload);
    }

    public static void handleConsumption(org.papiricoh.supernaturalcraft.network.ConsumptionPayload payload) {
        org.papiricoh.supernaturalcraft.client.amara.ConsumptionOverlay.set(payload.value());
    }

    public static void handleAmaraFx(org.papiricoh.supernaturalcraft.network.AmaraFxPayload payload) {
        org.papiricoh.supernaturalcraft.client.amara.AmaraFxRenderer.add(payload);
    }

    public static void handleDebris(org.papiricoh.supernaturalcraft.network.DebrisPayload payload) {
        org.papiricoh.supernaturalcraft.client.fx.DebrisFx.add(payload);
    }

    public static void handleChorusFx(org.papiricoh.supernaturalcraft.network.ChorusFxPayload payload) {
        org.papiricoh.supernaturalcraft.client.chorus.ChorusFxRenderer.add(payload);
    }
}
