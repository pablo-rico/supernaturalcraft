package org.papiricoh.supernaturalcraft.magic.spell;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** The three slots of a composed spell. */
public enum SigilKind implements StringRepresentable {
    /** How the spell travels: touch, bolt, burst, ward. Exactly one per spell. */
    FORM,
    /** What it does to whatever it reaches. Up to three. */
    EFFECT,
    /** How it is bent: stronger, longer, wider, repeated. Up to three. */
    MODIFIER;

    public static final Codec<SigilKind> CODEC = StringRepresentable.fromEnum(SigilKind::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
