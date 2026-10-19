package org.papiricoh.supernaturalcraft.entity.witch;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Whoever a hexed victim serves (v0.21, P6: Rowena in her fight), told of its fate: killed (its life goes to her) or freed by
 * Break Hex or salt (the broken hex rebounds on her).
 */
public interface VictimMaster {

    void victimKilled(HexedVillagerEntity victim, @Nullable Entity by);

    void victimFreed(HexedVillagerEntity victim, @Nullable Entity by);
}
