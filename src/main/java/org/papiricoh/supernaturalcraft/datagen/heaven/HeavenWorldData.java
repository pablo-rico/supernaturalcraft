package org.papiricoh.supernaturalcraft.datagen.heaven;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

/**
 * v0.18's world data with providers of its own, added from {@code SNDataGenerators}. Heaven's own providers (owned by the world
 * work) go here; the wild crossroads' live in {@link CrossroadsWildData}.
 */
public final class HeavenWorldData {

    private HeavenWorldData() {
    }

    public static void gather(GatherDataEvent event, DataGenerator generator, PackOutput output,
                              CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existing) {
        CrossroadsWildData.gather(event, generator, output, lookup, existing);
    }
}
