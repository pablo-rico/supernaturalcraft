package org.papiricoh.supernaturalcraft.client.heaven.render;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads a synced look off an entity by the name of its getter (v0.18): for state the entity classes of other parts of the work
 * expose under names this side can only expect (a figure's pose, Zachariah's wings). The first public no-argument method found
 * among the names is remembered per class; none found reads as the fallback, quietly.
 */
public final class Accessors {

    private static final Method NONE;
    private static final Map<Class<?>, Map<String, Method>> CACHE = new ConcurrentHashMap<>();

    static {
        try {
            NONE = Object.class.getMethod("hashCode");
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private Accessors() {
    }

    private static @Nullable Object call(Object target, String... names) {
        Map<String, Method> byName = CACHE.computeIfAbsent(target.getClass(), c -> new ConcurrentHashMap<>());
        String key = String.join("|", names);
        Method m = byName.computeIfAbsent(key, k -> find(target.getClass(), names));
        if (m == NONE) return null;
        try {
            return m.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException e) {
            byName.put(key, NONE);
            return null;
        }
    }

    private static Method find(Class<?> c, String... names) {
        for (String n : names) {
            try {
                Method m = c.getMethod(n);
                if (m.getParameterCount() == 0 && m.getReturnType() != void.class) return m;
            } catch (NoSuchMethodException ignored) {
                // try the next name
            }
        }
        return NONE;
    }

    public static boolean bool(Object target, boolean fallback, String... names) {
        return call(target, names) instanceof Boolean b ? b : fallback;
    }

    public static String string(Object target, String fallback, String... names) {
        Object o = call(target, names);
        return o == null ? fallback : o.toString();
    }

    public static float number(Object target, float fallback, String... names) {
        return call(target, names) instanceof Number n ? n.floatValue() : fallback;
    }

    public static int integer(Object target, int fallback, String... names) {
        return call(target, names) instanceof Number n ? n.intValue() : fallback;
    }
}
