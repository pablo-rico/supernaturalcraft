package org.papiricoh.supernaturalcraft.leviathan;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/** Something borax hurts (v0.25): the Leviathans, their heads and Dick Roman. {@link Borax} calls it. */
public interface BoraxSensitive {

    /** Borax touched it ({@code owner} threw or sprayed it, if anyone did). */
    void onBorax(ServerLevel level, @Nullable Entity owner);
}
