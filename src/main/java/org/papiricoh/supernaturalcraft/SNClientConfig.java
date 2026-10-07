package org.papiricoh.supernaturalcraft;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Per-player presentation options. Nothing here changes gameplay. */
public class SNClientConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue CINEMATICS = BUILDER
            .comment("Play full camera cinematics for boss intros, phase changes and deaths.",
                    "If false, a short letterbox and title are shown instead.")
            .define("cinematics", true);
    public static final ModConfigSpec.IntValue SKIP_HOLD_TICKS = BUILDER
            .comment("How long (ticks) to hold the skip key to skip a cinematic.")
            .defineInRange("skipHoldTicks", 20, 1, 200);
    public static final ModConfigSpec.BooleanValue DISTORTION = BUILDER
            .comment("Screen distortion shader near the Darkness. A cheaper vignette is always used.")
            .define("distortion", true);
    public static final ModConfigSpec.BooleanValue ECLIPSE_SKY = BUILDER
            .comment("Draw the custom eclipse sky. Turn off if another mod or shader pack replaces the sky.")
            .define("eclipseSky", true);
    public static final ModConfigSpec.BooleanValue HALLUCINATIONS = BUILDER
            .comment("Whispers and fleeting shades when sanity runs low.")
            .define("hallucinations", true);
    public static final ModConfigSpec.DoubleValue COLT_CAMERA_KICK = BUILDER
            .comment("How hard the Colt kicks the camera when it fires (0 = off). Also scaled by Screen Effects.")
            .defineInRange("coltCameraKick", 1.0, 0.0, 2.0);
    public static final ModConfigSpec.BooleanValue COLT_SCREEN_FLASH = BUILDER
            .comment("A brief warm flash at the screen's edge when the Colt fires.")
            .define("coltScreenFlash", true);
    public static final ModConfigSpec.BooleanValue COLT_FIRST_PERSON_ARMS = BUILDER
            .comment("Draw your arms holding the Colt in first person. Turn off if another mod already draws them.")
            .define("coltFirstPersonArms", true);
    public static final ModConfigSpec.BooleanValue COLT_CHAMBER_HUD = BUILDER
            .comment("Show the Colt's cylinder on screen while it is in hand.")
            .define("coltChamberHud", true);
    public static final ModConfigSpec.BooleanValue HELL_TORMENT = BUILDER
            .comment("Hell's Torment: a red pulse at the screen's edge, whispers and shapes in the fog the longer you stay. Never harmful.")
            .define("hellTorment", true);
    public static final ModConfigSpec.BooleanValue HELL_FOG = BUILDER
            .comment("Hell's thick fog. Turn off if a shader pack draws its own.")
            .define("hellFog", true);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
