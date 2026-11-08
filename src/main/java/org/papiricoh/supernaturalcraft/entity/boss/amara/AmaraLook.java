package org.papiricoh.supernaturalcraft.entity.boss.amara;

/**
 * What the client may ask Amara (v0.42) to draw her, her sky and her HUD: synced entity data on the server side, read by
 * {@code client.amara.*}. The renderer picks the rig from {@link #form()}, the sky its stage from {@link #skyStage()}.
 */
public interface AmaraLook {

    /** Which body she wears: {@link #MASS} (phase I, rig {@code amara}) or one of the form rig's ages. */
    int form();

    int MASS = 0, GIRL = 1, WOMAN = 2, VOID = 3, SPENT = 4;

    /** @return the age the form rig wears for {@link #form()} (null for the mass) */
    default AmaraAssets.Age age() {
        return switch (form()) {
            case GIRL -> AmaraAssets.Age.GIRL;
            case WOMAN, SPENT -> AmaraAssets.Age.WOMAN;
            case VOID -> AmaraAssets.Age.VOID;
            default -> null;
        };
    }

    /**
     * Her sky: 0 none (the plain dying sun), 1 the black sun with its corona (III-IV), 2 the eclipse with a face, huge, eyes
     * shut (V), 3 the face's eyes open (V, below half), 4 the sun dying (VI: the corona gutters), 5 dawn (the reconciliation:
     * Chuck's light breaks the disc, rays).
     */
    int skyStage();

    /** Chuck's light in phase IV, 0..1 (1 whole; she wounds it, the hunters protect it). 0 outside phase IV. */
    float chuckLight();

    /** Phase VI's countdown, 1 at its start down to 0 (then the enrage). 1 outside phase VI. */
    float countdown();

    /** Whether her rage is up (phase VI's countdown ran out): the HUD and the sky flare. */
    boolean enraged();

    /** The fight's phase, 1..6. */
    int phase();
}
