package org.papiricoh.supernaturalcraft.hands;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Whether season 11 has begun in this world (v0.42), kept by the Overworld: set the first time the Darkness is seen loose (the
 * sun dying) or a hunter who has let her out logs in. Once set it stays: the Chitters keep their cycle ever after, while the
 * banshee asks the dying sun itself.
 */
public class HandsSavedData extends SavedData {

    private static final String NAME = "supernaturalcraft_hands";

    private boolean opened;

    public static HandsSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(HandsSavedData::new, HandsSavedData::load, null), NAME);
    }

    private static HandsSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        HandsSavedData d = new HandsSavedData();
        d.opened = tag.getBoolean("Opened");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("Opened", opened);
        return tag;
    }

    public boolean opened() {
        return opened;
    }

    public void setOpened(boolean open) {
        if (opened != open) {
            opened = open;
            setDirty();
        }
    }
}
