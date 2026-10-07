package org.papiricoh.supernaturalcraft.entity.boss.chorus;

import java.util.List;

/** Animations the server triggers by name; must match {@code broken_chorus.names.txt}. */
public final class ChorusAnimations {

    public static final List<String> TRIGGERED = List.of("sing_man", "sing_lion", "sing_ox", "sing_eagle", "hymn", "kneel",
            "dive", "transform_2", "transform_3", "transform_4", "emerge", "death");

    private ChorusAnimations() {
    }
}
