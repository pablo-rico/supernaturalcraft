package org.papiricoh.supernaturalcraft.structure.enochian;

import org.papiricoh.supernaturalcraft.buildkit.LayoutCatalog;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** The buried Enochian crypt (v0.36) for the layout dump ({@code LayoutDumpTest}) and {@code tools/structview} (pure). */
public final class EnochianCryptCatalog implements LayoutCatalog {

    @Override
    public Map<String, Supplier<LayoutPlan>> layouts() {
        Map<String, Supplier<LayoutPlan>> out = new LinkedHashMap<>();
        out.put("enochian_crypt", () -> EnochianCryptPlan.of(0).plan());
        out.put("enochian_crypt_seed7", () -> EnochianCryptPlan.of(7).plan());
        return out;
    }
}
