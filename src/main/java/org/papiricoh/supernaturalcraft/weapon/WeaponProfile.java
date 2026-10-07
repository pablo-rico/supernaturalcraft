package org.papiricoh.supernaturalcraft.weapon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

/**
 * Balance data for one weapon, from the {@code supernaturalcraft:weapon_profile} item data map
 * ({@code data/<ns>/data_maps/item/weapon_profile.json}). Packs rebalance tiers and rune slots
 * without code.
 */
public record WeaponProfile(int tier, int runeSlots, Kind kind, boolean holy, boolean cursed) {

    public enum Kind implements StringRepresentable {
        MELEE, CATALYST;

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final Codec<WeaponProfile> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 4).fieldOf("tier").forGetter(WeaponProfile::tier),
            Codec.intRange(0, 4).optionalFieldOf("rune_slots", 0).forGetter(WeaponProfile::runeSlots),
            Kind.CODEC.optionalFieldOf("kind", Kind.MELEE).forGetter(WeaponProfile::kind),
            Codec.BOOL.optionalFieldOf("holy", false).forGetter(WeaponProfile::holy),
            Codec.BOOL.optionalFieldOf("cursed", false).forGetter(WeaponProfile::cursed)
    ).apply(i, WeaponProfile::new));
}
