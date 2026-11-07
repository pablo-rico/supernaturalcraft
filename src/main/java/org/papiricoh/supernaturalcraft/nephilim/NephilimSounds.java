package org.papiricoh.supernaturalcraft.nephilim;

import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;
import java.util.Map;

/**
 * v0.41 core's sounds, {@code <rig>.<id>} (each rig's {@code *Assets.SOUNDS} plus the boss plumbing's {@code emerge},
 * {@code roar}, {@code transform}, {@code ambient}); definitions in {@code NephilimData} (vanilla re-pitched until encoded).
 */
public final class NephilimSounds {

    private static final List<String> BOSS = List.of("emerge", "roar", "transform", "ambient");

    private NephilimSounds() {
    }

    private static List<String> with(List<String> own) {
        List<String> out = new java.util.ArrayList<>(own);
        for (String s : BOSS) if (!out.contains(s)) out.add(s);
        return out;
    }

    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> ASMODEUS =
            AllSounds.registerGroup("asmodeus", with(org.papiricoh.supernaturalcraft.entity.boss.asmodeus.AsmodeusAssets.SOUNDS));
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> MICHAEL_VESSEL =
            AllSounds.registerGroup("michael_vessel", with(org.papiricoh.supernaturalcraft.entity.boss.michaelvessel.MichaelVesselAssets.SOUNDS));
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> JACK =
            AllSounds.registerGroup("jack", with(org.papiricoh.supernaturalcraft.nephilim.jack.JackAssets.SOUNDS));
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> SHADOW =
            AllSounds.registerGroup("shadow", with(org.papiricoh.supernaturalcraft.entity.boss.shadow.ShadowAssets.SOUNDS));

    /** Every group's sounds, for the definitions. */
    public static Map<String, DeferredHolder<SoundEvent, SoundEvent>> all() {
        Map<String, DeferredHolder<SoundEvent, SoundEvent>> out = new java.util.LinkedHashMap<>();
        out.putAll(ASMODEUS);
        out.putAll(MICHAEL_VESSEL);
        out.putAll(JACK);
        out.putAll(SHADOW);
        return out;
    }

    /** One of them by its full id ({@code asmodeus.cane}), or null. */
    public static SoundEvent get(String id) {
        DeferredHolder<SoundEvent, SoundEvent> h = all().get(id);
        return h == null ? null : h.get();
    }

    static void init() {
    }
}
