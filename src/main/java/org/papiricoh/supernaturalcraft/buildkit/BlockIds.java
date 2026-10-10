package org.papiricoh.supernaturalcraft.buildkit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The block ids and state properties a layout may use (pure), read from a list made from the game itself
 * ({@code src/test/resources/vanilla_blocks_1_21_1.txt}, written by {@code tools/structview/structview.py --extract-ids}): one
 * block per line, {@code id prop=v1|v2 prop=…}. {@link #check} catches what the game's parser would refuse — and turn into air
 * in a layout written block by block: an unknown id, an unknown property, a value the property does not take. Mod blocks are
 * allowed by id with {@link #allow} (any properties).
 */
public final class BlockIds {

    /** The resource name of the vanilla list on the test classpath. */
    public static final String VANILLA_RESOURCE = "/vanilla_blocks_1_21_1.txt";

    private final Map<String, Map<String, Set<String>>> blocks;
    private final Set<String> free;

    private BlockIds(Map<String, Map<String, Set<String>>> blocks, Set<String> free) {
        this.blocks = blocks;
        this.free = free;
    }

    /** The vanilla list from the classpath (tests). */
    public static BlockIds vanilla() {
        try (InputStream in = BlockIds.class.getResourceAsStream(VANILLA_RESOURCE)) {
            if (in == null) throw new IllegalStateException(VANILLA_RESOURCE + " is not on the classpath");
            return read(in);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static BlockIds read(InputStream in) throws IOException {
        Map<String, Map<String, Set<String>>> blocks = new HashMap<>();
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        for (String line; (line = r.readLine()) != null; ) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split(" ");
            Map<String, Set<String>> props = new HashMap<>();
            for (int i = 1; i < parts.length; i++) {
                int e = parts[i].indexOf('=');
                props.put(parts[i].substring(0, e), Set.of(parts[i].substring(e + 1).split("\\|")));
            }
            blocks.put(parts[0], props);
        }
        return new BlockIds(blocks, Set.of());
    }

    /** These ids plus {@code ids} (mod blocks), accepted with any properties. */
    public BlockIds allow(String... ids) {
        Set<String> f = new HashSet<>(free);
        for (String id : ids) f.add(St.full(id));
        return new BlockIds(blocks, Collections.unmodifiableSet(f));
    }

    public boolean known(String id) {
        return blocks.containsKey(id) || free.contains(id);
    }

    public int size() {
        return blocks.size();
    }

    /** Null when the state is fine, else why the game would not read it. */
    public String check(String state) {
        if (!St.wellFormed(state)) return "malformed: " + state;
        String id = St.id(state);
        if (free.contains(id)) return null;
        Map<String, Set<String>> props = blocks.get(id);
        if (props == null) return "unknown block " + id;
        for (Map.Entry<String, String> e : St.props(state).entrySet()) {
            Set<String> values = props.get(e.getKey());
            if (values == null) return id + " has no property " + e.getKey() + " (has " + props.keySet() + ")";
            if (!values.contains(e.getValue())) return id + "[" + e.getKey() + "=" + e.getValue() + "] is not one of " + values;
        }
        return null;
    }
}
