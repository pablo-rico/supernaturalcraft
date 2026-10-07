package org.papiricoh.supernaturalcraft.magic.spell;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.spell.effect.SpellEffects;
import org.papiricoh.supernaturalcraft.magic.spell.form.SpellForms;

import java.util.HashMap;
import java.util.Map;

/**
 * The Java behaviours sigil JSON can point at. Other mods add their own with {@link #register}
 * during mod construction; a sigil naming an unknown behaviour simply never resolves.
 */
public final class SpellBehaviors {

    private static final Map<ResourceLocation, SpellBehavior> BEHAVIORS = new HashMap<>();

    /** Modifiers are fully data-driven: this one behaviour reads every number from the sigil. */
    public static final SpellBehavior.Modifier MODIFIER = (ctx, sigil) -> {
        ctx.potency *= sigil.param("potency_multiplier", 1.0f);
        ctx.durationScale *= sigil.param("duration_multiplier", 1.0f);
        ctx.range *= sigil.param("range_multiplier", 1.0f);
        ctx.area += sigil.param("area_bonus", 0.0f);
        ctx.echoes += (int) sigil.param("echo", 0.0f);
    };

    private SpellBehaviors() {
    }

    public static void register(ResourceLocation id, SpellBehavior behavior) {
        if (BEHAVIORS.putIfAbsent(id, behavior) != null) {
            throw new IllegalStateException("Duplicate spell behaviour " + id);
        }
    }

    public static @Nullable SpellBehavior get(ResourceLocation id) {
        return BEHAVIORS.get(id);
    }

    static {
        register(SupernaturalCraft.asResource("modifier"), MODIFIER);
        SpellForms.registerAll();
        SpellEffects.registerAll();
    }

    public static void init() {
    }
}
