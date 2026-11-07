package org.papiricoh.supernaturalcraft.datagen.slice;

import java.util.List;

/** v0.41: every content slice's datagen hooks, in a fixed order (the shared providers walk this list). */
public final class Slices {

    private Slices() {
    }

    public static final List<SliceData> ALL = List.of(
            new org.papiricoh.supernaturalcraft.datagen.british.BritishData(),     // season 12: the British, Ketch, Hess, Dagon
            new org.papiricoh.supernaturalcraft.datagen.apocworld.ApocWorldData(), // season 13: the Apocalypse World, AU Michael
            new org.papiricoh.supernaturalcraft.datagen.nephilim.NephilimData());  // Jack, Asmodeus, season 14, the Empty
}
