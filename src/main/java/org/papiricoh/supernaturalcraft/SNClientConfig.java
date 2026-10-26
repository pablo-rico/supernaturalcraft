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

    public static final ModConfigSpec.IntValue FACTION_DETAIL_DISTANCE = BUILDER
            .comment("Beyond this many blocks other players' wings, regalia and true forms are not drawn (their eyes still are).",
                    "Lower it for crowded servers.")
            .defineInRange("factionDetailDistance", 48, 8, 256);
    public static final ModConfigSpec.IntValue DEBRIS_LIMIT = BUILDER
            .comment("Most falling-debris pieces drawn at once in boss arenas.")
            .defineInRange("debrisLimit", 1500, 0, 10000);

    public static final ModConfigSpec.IntValue MAGIC_FX_INTENSITY = BUILDER
            .comment("How much of the magic's effects is drawn: 0 = minimal (no particles beyond the essentials),",
                    "1 = reduced, 2 = full (cast circles, trails, pillars, lights).")
            .defineInRange("magicFxIntensity", 2, 0, 2);
    public static final ModConfigSpec.BooleanValue MAGIC_FX_SHAKE = BUILDER
            .comment("Let great rites and spells shake the camera.")
            .define("magicFxShake", true);

    public static final ModConfigSpec.IntValue TOOLTIP_MAX_WIDTH = BUILDER
            .comment("Widest a SupernaturalCraft item tooltip line may be, in GUI pixels, before it wraps (0 = never wrap).")
            .defineInRange("tooltipMaxWidth", 200, 0, 1000);

    /** How many falling angels streak across the sky on the night of the Fall (v0.22). */
    public enum FallDensity { LOW, NORMAL }

    public static final ModConfigSpec.EnumValue<FallDensity> FALL_DENSITY = BUILDER
            .comment("How many falling angels cross the sky on the night of the Fall: LOW (a third as many) or NORMAL.")
            .defineEnum("fallDensity", FallDensity.NORMAL);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
