package org.papiricoh.supernaturalcraft.client.s1214;

import java.util.List;

/**
 * Which boss bar belongs to which v0.41 boss (pure: no Minecraft types, tested by {@code S1214BarsTest}).
 *
 * <p>The convention every v0.41 boss follows: its {@code ServerBossEvent} is named with
 * {@code Component.translatable("entity.supernaturalcraft.<rig>.bar.phase<n>")} (n = 1, 2, 3...; a key of just
 * {@code entity.supernaturalcraft.<rig>.bar} reads as phase 1), where {@code <rig>} is one of {@link #RIGS}. The key's text is
 * the boss's name (it may differ per phase). Sheets: {@code textures/gui/<rig>_bar.png} ({@code tools/artgen/s1214_gui_art.py}).
 */
public final class S1214Bars {

    private S1214Bars() {
    }

    public static final String PREFIX = "entity.supernaturalcraft.";
    /** The eight v0.41 bosses with a bar, by rig (= entity id). */
    public static final List<String> RIGS = List.of("ketch", "hess", "dagon", "asmodeus", "au_michael", "michael_vessel", "soulless_jack", "shadow");
    /** Emblems hung under each bar (one per phase): three, but AU Michael's four. */
    public static final int PHASES = 3;

    /** How many phases (emblems) {@code rig}'s bar shows. */
    public static int phases(String rig) {
        return "au_michael".equals(rig) ? 4 : PHASES;
    }

    /** The rig whose bar this key is, or null. */
    public static String rigOf(String key) {
        if (key == null || !key.startsWith(PREFIX)) return null;
        String rest = key.substring(PREFIX.length());
        for (String rig : RIGS) {
            String bar = rig + ".bar";
            if (rest.equals(bar) || rest.startsWith(bar + ".")) return rig;
        }
        return null;
    }

    /** The phase in a bar key ({@code ....bar.phase<n>}, any number of digits; 1 if none), at least 1. */
    public static int phase(String key) {
        if (key == null) return 1;
        int i = key.lastIndexOf(".phase");
        if (i < 0) return 1;
        String digits = key.substring(i + ".phase".length());
        if (digits.isEmpty()) return 1;
        for (int k = 0; k < digits.length(); k++) if (!Character.isDigit(digits.charAt(k))) return 1;
        try {
            return Math.max(1, Integer.parseInt(digits));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** The sheet path of a rig's bar (relative to the mod's assets). */
    public static String sheet(String rig) {
        return "textures/gui/" + rig + "_bar.png";
    }
}
