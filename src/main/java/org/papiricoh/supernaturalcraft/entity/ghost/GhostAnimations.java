package org.papiricoh.supernaturalcraft.entity.ghost;

import java.util.List;

/** The ghost's clip and bone names, kept apart from the entity so JUnit can check them against the art. */
public final class GhostAnimations {

    public static final String PREFIX = "animation.ghost.";
    public static final String IDLE = "idle", FLOAT = "float", SCREAM = "scream", FADE = "fade", FLICKER = "flicker";
    public static final List<String> LOOPS = List.of(IDLE, FLOAT);
    public static final List<String> TRIGGERED = List.of(SCREAM, FADE, FLICKER);
    /** Bones the Java side relies on (the head turns; the arms reach out in the scream). */
    public static final List<String> BONES = List.of("head", "right_arm", "left_arm");

    private GhostAnimations() {
    }

    public static String clip(String name) {
        return PREFIX + name;
    }
}
