package org.papiricoh.supernaturalcraft.weapon.melee;

import java.util.List;
import java.util.Set;

/**
 * The angelic blades' art (tools/artgen weapon_models): each one a GeckoLib item, {@code geo/item/<id>.geo.json} drawn upright
 * with its point up (the reverse grip is the item model's transforms), its own glowmask and inventory icon. The {@link #LIGHT}
 * bone is a glowing sheath kept at scale 0 by {@code idle}; {@code charge} lights it up and holds, {@code flash} is a blink.
 */
public final class AngelicBladeAssets {

    public static final List<String> IDS = List.of("angel_blade", "archangel_blade", "gabriel_blade");
    public static final String BLADE = "blade", GRIP = "grip", LIGHT = "light";
    public static final List<String> BONES = List.of(BLADE, GRIP, LIGHT);
    public static final String IDLE = "idle", CHARGE = "charge", FLASH = "flash";
    public static final List<String> CLIPS = List.of(IDLE, CHARGE, FLASH);
    public static final Set<String> LOOPS = Set.of(IDLE, CHARGE);

    /** Heights in model pixels (tools/artgen prints them), for the item model's scale. */
    public static final float ANGEL_HEIGHT = 16.5f, ARCHANGEL_HEIGHT = 21.1f, GABRIEL_HEIGHT = 22.1f;
    /** The middle of each grip along the blade's axis, in model pixels from the origin: where the fist closes. */
    public static final float ANGEL_GRIP = -5.25f, ARCHANGEL_GRIP = -6.45f, GABRIEL_GRIP = -4.45f;

    private AngelicBladeAssets() {
    }

    /** {@code id}'s height in model pixels (16 if not a blade of this set). */
    public static float height(String id) {
        return switch (id) {
            case "archangel_blade" -> ARCHANGEL_HEIGHT;
            case "gabriel_blade" -> GABRIEL_HEIGHT;
            default -> ANGEL_HEIGHT;
        };
    }

    /** Where {@code id}'s grip is, in model pixels along the blade. */
    public static float grip(String id) {
        return switch (id) {
            case "archangel_blade" -> ARCHANGEL_GRIP;
            case "gabriel_blade" -> GABRIEL_GRIP;
            default -> ANGEL_GRIP;
        };
    }

    public static String geo(String id) {
        return "geo/item/" + id + ".geo.json";
    }

    public static String anim(String id) {
        return "animations/item/" + id + ".animation.json";
    }

    public static String clip(String id, String clip) {
        return "animation." + id + "." + clip;
    }
}
