package org.papiricoh.supernaturalcraft.british;

import org.papiricoh.supernaturalcraft.network.CountingRegistrar;

/** v0.41: this slice's payloads (called from {@code SNNetworking.register}; client handlers only from lambdas). */
public final class BritishNet {

    private BritishNet() {
    }

    public static void register(CountingRegistrar registrar) {
        registrar.playToClient(BritishFxPayload.TYPE, BritishFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.british.ClientBritishFx.handle(payload)));
    }
}
