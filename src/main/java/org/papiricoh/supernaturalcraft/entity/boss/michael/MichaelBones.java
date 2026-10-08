package org.papiricoh.supernaturalcraft.entity.boss.michael;

import java.util.List;

/**
 * The bones of Michael's models that the code drives (michael contract). Procedural bones are turned by the renderer every
 * frame, so no clip may key them; toggled bones are only shown or hidden. {@code MichaelAssetsTest} holds the art to it.
 */
public final class MichaelBones {

    /** Turned by the archangel's renderer: the halo spins, the visor breathes, the cape sways. */
    public static final List<String> PROCEDURAL = List.of("halo_spin", "visor_light", "cape_0", "cape_1", "cape_2", "cape_3", "cape_4");
    /** The vessel's: shown or hidden by the renderer (no clip keys their visibility). */
    public static final String LANCE = "lance", BLADE = "blade", SHADOW_WINGS = "shadow_wings", PALM_LIGHT = "palm_light";
    /** Bones every model must have. */
    public static final List<String> VESSEL_REQUIRED = List.of("root", "body", "head", "right_arm", "left_arm", "right_leg",
            "left_leg", "right_hand", "left_hand", PALM_LIGHT, BLADE, LANCE, SHADOW_WINGS, "wing_l1", "wing_r1", "wing_l2", "wing_r2");
    public static final List<String> ARCHANGEL_REQUIRED = List.of("root", "body", "head", "visor_light", "right_arm", "left_arm",
            "right_hand", "left_hand", "right_leg", "left_leg", LANCE, "halo", "halo_spin", "cape_0", "cape_1", "cape_2", "cape_3",
            "cape_4", "wing_l1", "wing_l2", "wing_l3", "wing_r1", "wing_r2", "wing_r3");
    public static final List<String> HOST_REQUIRED = List.of("root", "body", "head", "right_arm", "left_arm", "right_leg",
            "left_leg", "right_hand", "left_hand", "blade", "shield", "breastplate", "helmet", "plume", "wings_folded", "wings_open");
    /** The halo's spears hidden once it breaks (phase VI). */
    public static final List<String> BROKEN_SPEARS = List.of("halo_spear_01", "halo_spear_04", "halo_spear_07", "halo_spear_10");
    public static final int HALO_SPEARS = 12;
    /** The least the true form must be built of: it is the most worked model of the mod. */
    public static final int ARCHANGEL_MIN_BONES = 200;

    private MichaelBones() {
    }

    public static String haloSpear(int i) {
        return String.format("halo_spear_%02d", i);
    }
}
