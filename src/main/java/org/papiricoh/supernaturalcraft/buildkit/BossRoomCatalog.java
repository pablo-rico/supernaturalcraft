package org.papiricoh.supernaturalcraft.buildkit;

import org.papiricoh.supernaturalcraft.crossroads.wild.CrossroadsLayout;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.arena.ReprogrammingRoomLayout;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.arena.ZachariahOfficeLayout;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The v0.18 boss rooms and the wild crossroads, for the layout dump ({@code LayoutDumpTest}) and {@code tools/structview} (pure).
 */
public final class BossRoomCatalog implements LayoutCatalog {

    @Override
    public Map<String, Supplier<LayoutPlan>> layouts() {
        Map<String, Supplier<LayoutPlan>> out = new LinkedHashMap<>();
        out.put("naomi_room", ReprogrammingRoomLayout::plan);
        out.put("zachariah_office", ZachariahOfficeLayout::plan);
        out.put("crossroads_wild", () -> CrossroadsLayout.plan(7L, 0));
        out.put("crossroads_wild_east", () -> CrossroadsLayout.plan(1234567L, 1));
        return out;
    }
}
