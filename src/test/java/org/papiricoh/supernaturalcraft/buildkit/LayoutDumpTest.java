package org.papiricoh.supernaturalcraft.buildkit;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dumps every known layout to {@code build/layouts/<name>.json} for {@code tools/structview} (run it alone with
 * {@code ./gradlew test --tests '*LayoutDumpTest'}, then {@code python tools/structview/structview.py build/layouts/<name>.json}).
 *
 * <p>Catalogs are found by class name ({@link #CATALOGS}, plus any listed comma-separated in the {@code SN_LAYOUT_CATALOGS}
 * environment variable) and skipped when absent, so this test never depends on who has written what yet. A catalog is any class
 * with a public no-argument constructor and a {@code layouts()} method returning {@code Map<String, Supplier<?>>} whose
 * suppliers give a {@link LayoutPlan} or a {@link Canvas} ({@link LayoutCatalog} is the typed form). {@code SN_LAYOUTS}
 * (comma-separated names) limits the dump to those layouts.
 *
 * <p>JSON: {@code {"name", "bounds": {"min", "max"}, "cells": [[x, y, z, "state"]…], "decor": [{kind, x, y, z, facing, data,
 * extra}…], "anchors": {name: [x, y, z]}, "zones": [{name, min, max, anchors}]}}.
 */
class LayoutDumpTest {

    /** Catalog classes probed in order (missing ones are skipped). Add new catalogs here. */
    static final List<String> CATALOGS = List.of(
            "org.papiricoh.supernaturalcraft.buildkit.demo.KitDemoCatalog",
            "org.papiricoh.supernaturalcraft.heaven.plot.HeavenLayoutCatalog",
            "org.papiricoh.supernaturalcraft.heaven.HeavenLayoutCatalog",
            "org.papiricoh.supernaturalcraft.layout.LayoutCatalogs",
            "org.papiricoh.supernaturalcraft.crossroads.CrossroadsLayoutCatalog",
            "org.papiricoh.supernaturalcraft.buildkit.BossRoomCatalog",
            "org.papiricoh.supernaturalcraft.memory.scenes.MemorySceneCatalog");

    static final Path OUT = Path.of("build", "layouts");

    @Test
    void dumpEveryLayout() throws Exception {
        Map<String, Canvas> all = load();
        assertFalse(all.isEmpty(), "no layouts found (the kit's demo catalog at least should be)");
        Files.createDirectories(OUT);
        for (Map.Entry<String, Canvas> e : all.entrySet()) {
            Path file = OUT.resolve(e.getKey() + ".json");
            Files.writeString(file, json(e.getKey(), e.getValue()), StandardCharsets.UTF_8);
            assertTrue(Files.size(file) > 0);
        }
    }

    /** Every layout of every catalog found, as a canvas, in catalog order. */
    static Map<String, Canvas> load() throws Exception {
        List<String> names = new ArrayList<>(CATALOGS);
        String extra = System.getenv("SN_LAYOUT_CATALOGS");
        if (extra != null) for (String s : extra.split(",")) if (!s.isBlank()) names.add(s.strip());
        String only = System.getenv("SN_LAYOUTS");
        List<String> filter = only == null ? null : List.of(only.split(","));
        Map<String, Canvas> out = new LinkedHashMap<>();
        for (String cls : names) {
            Class<?> k;
            try {
                k = Class.forName(cls);
            } catch (ClassNotFoundException e) {
                continue;
            }
            Object catalog = k.getDeclaredConstructor().newInstance();
            Method m = k.getMethod("layouts");
            Map<?, ?> layouts = (Map<?, ?>) m.invoke(catalog);
            for (Map.Entry<?, ?> e : layouts.entrySet()) {
                String name = (String) e.getKey();
                if (filter != null && !filter.contains(name)) continue;
                if (!name.matches("[a-z0-9_]+")) throw new IllegalStateException("layout name must be [a-z0-9_]+: " + name);
                if (out.containsKey(name)) throw new IllegalStateException("two layouts named " + name);
                Object v = ((Supplier<?>) e.getValue()).get();
                Canvas canvas = v instanceof Canvas cv ? cv : Canvas.fromPlan(name, (LayoutPlan) v);
                out.put(name, canvas);
            }
        }
        return out;
    }

    static String json(String name, Canvas c) {
        StringBuilder sb = new StringBuilder(c.size() * 48);
        Box b = c.bounds();
        sb.append("{\"name\":").append(q(name)).append(",\n\"bounds\":{\"min\":[").append(b.x0()).append(',').append(b.y0()).append(',')
                .append(b.z0()).append("],\"max\":[").append(b.x1()).append(',').append(b.y1()).append(',').append(b.z1()).append("]},\n\"cells\":[");
        boolean first = true;
        for (Cell cell : c.cells()) {
            if (!first) sb.append(",\n");
            first = false;
            sb.append('[').append(cell.x()).append(',').append(cell.y()).append(',').append(cell.z()).append(',').append(q(cell.state())).append(']');
        }
        sb.append("],\n\"decor\":[");
        first = true;
        for (Decor d : c.decor()) {
            if (!first) sb.append(",\n");
            first = false;
            sb.append("{\"kind\":").append(q(d.kind().name())).append(",\"x\":").append(d.x()).append(",\"y\":").append(d.y()).append(",\"z\":")
                    .append(d.z()).append(",\"facing\":").append(q(d.facing())).append(",\"data\":").append(q(d.data())).append(",\"extra\":")
                    .append(q(d.extra())).append('}');
        }
        sb.append("],\n\"anchors\":").append(anchors(c.anchors())).append(",\n\"zones\":[");
        first = true;
        for (LayoutZone z : c.zones()) {
            if (!first) sb.append(",\n");
            first = false;
            Box zb = z.bounds();
            sb.append("{\"name\":").append(q(z.name())).append(",\"min\":[").append(zb.x0()).append(',').append(zb.y0()).append(',').append(zb.z0())
                    .append("],\"max\":[").append(zb.x1()).append(',').append(zb.y1()).append(',').append(zb.z1()).append("],\"anchors\":")
                    .append(anchors(z.anchors())).append('}');
        }
        return sb.append("]}\n").toString();
    }

    private static String anchors(Map<String, int[]> a) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, int[]> e : a.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            int[] p = e.getValue();
            sb.append(q(e.getKey())).append(":[").append(p[0]).append(',').append(p[1]).append(',').append(p[2]).append(']');
        }
        return sb.append('}').toString();
    }

    static String q(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                default -> {
                    if (ch < 0x20) sb.append(String.format("\\u%04x", (int) ch));
                    else sb.append(ch);
                }
            }
        }
        return sb.append('"').toString();
    }
}
