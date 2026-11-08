package org.papiricoh.supernaturalcraft.darkness;

import org.papiricoh.supernaturalcraft.dyingsun.DyingSunPayload;
import org.papiricoh.supernaturalcraft.network.CountingRegistrar;

/** v0.42: season 11's core payloads (called from {@code SNNetworking.register}; client handlers only from lambdas). */
public final class DarknessNet {

    private DarknessNet() {
    }

    public static void register(CountingRegistrar registrar) {
        registrar.playToClient(DyingSunPayload.TYPE, DyingSunPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.dyingsun.ClientDyingSun.apply(payload)));
    }
}
