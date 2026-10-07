package org.papiricoh.supernaturalcraft.compat.curios.client;

import org.papiricoh.supernaturalcraft.registry.AllItems;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

/** Client-side Curios hooks; only ever called when Curios is loaded. */
public final class CuriosClient {

    private CuriosClient() {
    }

    public static void register() {
        CuriosRendererRegistry.register(AllItems.SERAPH_WINGS.get(), SeraphWingsRenderer::new);
    }
}
