package org.papiricoh.supernaturalcraft.client.cinematic;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cinematic.CameraSequence;

import java.util.HashMap;
import java.util.Map;

/** Loads {@code assets/<ns>/cinematics/*.json}; resource packs may restage any shot. */
public class CinematicSequences extends SimpleJsonResourceReloadListener {

    private static final Map<ResourceLocation, CameraSequence> LOADED = new HashMap<>();

    public CinematicSequences() {
        super(new com.google.gson.Gson(), "cinematics");
    }

    public static CameraSequence get(ResourceLocation id) {
        return LOADED.get(id);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        LOADED.clear();
        files.forEach((id, json) -> CameraSequence.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(err -> SupernaturalCraft.LOGGER.error("Bad cinematic {}: {}", id, err))
                .ifPresent(seq -> LOADED.put(id, seq)));
        SupernaturalCraft.LOGGER.info("Loaded {} cinematics", LOADED.size());
    }
}
