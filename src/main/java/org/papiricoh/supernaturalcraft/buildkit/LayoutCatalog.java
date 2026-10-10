package org.papiricoh.supernaturalcraft.buildkit;

import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.Map;
import java.util.function.Supplier;

/**
 * A set of named layouts (pure) for the dump test ({@code LayoutDumpTest}), which loads every catalog class it knows by name,
 * draws each layout and writes {@code build/layouts/<name>.json} for {@code tools/structview}. An implementation needs a public
 * no-argument constructor. Layout names are file names: lower-case, digits and underscores, unique across catalogs (prefix them
 * by system, e.g. {@code heaven_roadhouse}).
 *
 * <p>Layouts built with the kit return {@code canvas.finish().toPlan()}; layouts that are only a {@link LayoutPlan} work the
 * same (the dump rebuilds a {@link Canvas} from the plan with {@link Canvas#fromPlan}).
 */
public interface LayoutCatalog {

    /** Layout name → a fresh plan (the supplier is called once per dump), in the order they should be listed. */
    Map<String, Supplier<LayoutPlan>> layouts();
}
