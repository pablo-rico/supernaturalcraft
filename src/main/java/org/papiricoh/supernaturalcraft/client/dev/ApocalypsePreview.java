package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Preview scenes for the Apocalypse's core (v0.40): Alastair, the warehouse, Ruby, the demon blood, the Panic Room and Swan Song. Captures in {@code runs/client/screenshots/sn_<scene>_<beat>.png}. Same shape as {@code PurgatoryPreview}: it runs
 * only when every name in {@code SN_PREVIEW} is one of its {@link #NAMES}.
 */
final class ApocalypsePreview {

    static final List<String> NAMES = List.of("alastair_model", "alastair_fight", "warehouse", "ruby_model", "ruby_fight", "blood_hud", "blood_wheel", "panic_room", "swan_song");

    private ApocalypsePreview() {
    }

    static boolean tick(Minecraft mc) {
        return false;
    }
}
