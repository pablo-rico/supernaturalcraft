package org.papiricoh.supernaturalcraft.apocworld;

import org.papiricoh.supernaturalcraft.entity.boss.aumichael.AuMichaelFxPayload;
import org.papiricoh.supernaturalcraft.network.CountingRegistrar;

/** v0.41: this slice's payloads (called from {@code SNNetworking.register}; client handlers only from lambdas). */
public final class ApocWorldNet {

    private ApocWorldNet() {
    }

    public static void register(CountingRegistrar registrar) {
        registrar.playToClient(AuMichaelFxPayload.TYPE, AuMichaelFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.apocworld.ClientAuMichaelFx.handle(payload)));
    }
}
