package org.papiricoh.supernaturalcraft.datagen.gabriel;

import java.util.function.BiConsumer;

/** Gabriel's client-side text (v0.14): the HUD (signs, channel cards, quiz panel, heart monitor, banner) and the prank hints. */
public final class GabrielUiLang {

    private static final String HUD = "hud.supernaturalcraft.gabriel.";

    private GabrielUiLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        // The banner bar's lamp and the studio signs (stand-ins while their textures are missing).
        add.accept(HUD + "on_air", "ON AIR");
        add.accept(HUD + "sign.laugh", "LAUGH");
        add.accept(HUD + "sign.applause", "APPLAUSE");
        add.accept(HUD + "sign.on_air", "ON AIR");

        // The game show's panel.
        add.accept(HUD + "quiz.platform.0", "RED");
        add.accept(HUD + "quiz.platform.1", "BLUE");
        add.accept(HUD + "quiz.platform.2", "YELLOW");
        add.accept(HUD + "quiz.right", "RIGHT!");
        add.accept(HUD + "quiz.wrong", "WRONG!");

        // The heart monitor.
        add.accept(HUD + "monitor.bpm", "%s BPM");
        add.accept(HUD + "monitor.hit", "NOW!");

        // The channels' title cards, and the episode's end.
        add.accept(HUD + "title.sitcom", "THE SITCOM");
        add.accept(HUD + "title.sitcom.sub", "Filmed before a live studio audience");
        add.accept(HUD + "title.game_show", "NUTCRACKER!");
        add.accept(HUD + "title.game_show.sub", "Answer fast, or get whacked");
        add.accept(HUD + "title.hospital", "DR. SEXY, M.D.");
        add.accept(HUD + "title.hospital.sub", "The doctor will see you now");
        add.accept(HUD + "title.commercial", "A WORD FROM OUR SPONSOR");
        add.accept(HUD + "title.commercial.sub", "Trickster Treats: they're to die for!");
        add.accept(HUD + "title.end", "EPISODE OVER");
        add.accept(HUD + "title.end.sitcom", "Stay tuned.");
        add.accept(HUD + "title.end.game_show", "Thanks for playing!");
        add.accept(HUD + "title.end.hospital", "Time of death: now.");
        add.accept(HUD + "title.end.commercial", "...or was it?");

        // The pranks' quiet hints.
        add.accept(HUD + "prank.candy_wrapper", "A candy wrapper. You haven't eaten any candy.");
        add.accept(HUD + "prank.party_hat", "Is that... a party hat?");
        add.accept(HUD + "prank.laugh_track", "Somewhere out of sight, an audience laughs.");
        add.accept(HUD + "prank.tv_line", "That villager has been watching too much TV.");
        add.accept(HUD + "prank.chest", "Did that chest just open by itself?");
    }
}
