package org.papiricoh.supernaturalcraft.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The great enemies the client has seen ticking (v0.19): effects that only matter while one is about (the Chorus's rays, the
 * Author's pages) look here first instead of searching the world's entities every frame. Client thread only; the set
 * forgets entities on its own.
 */
public final class ClientPresence {

    private static final Set<Entity> SEEN = Collections.newSetFromMap(new WeakHashMap<>());

    private ClientPresence() {
    }

    /** Called from a boss's own tick, on the client. */
    public static void mark(Entity e) {
        SEEN.add(e);
    }

    /** The marked entities of {@code type} still alive in {@code level}. */
    public static <T extends Entity> List<T> live(Class<T> type, Level level) {
        if (SEEN.isEmpty()) return List.of();
        List<T> out = new ArrayList<>();
        SEEN.removeIf(Entity::isRemoved);
        for (Entity e : SEEN) if (type.isInstance(e) && e.level() == level) out.add(type.cast(e));
        return out;
    }

    public static boolean any(Class<? extends Entity> type, Level level) {
        return !live(type, level).isEmpty();
    }
}
