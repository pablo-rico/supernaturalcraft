package org.papiricoh.supernaturalcraft.entity.boss.amara;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.papiricoh.supernaturalcraft.entity.hazard.VoidZone;

/** Queries about the pools of Darkness she leaves behind. */
public final class AmaraVoid {

    private AmaraVoid() {
    }

    public static boolean inVoid(ServerLevel level, Entity e) {
        return !level.getEntitiesOfClass(VoidZone.class, e.getBoundingBox().inflate(8, 3, 8), z -> z.contains(e)).isEmpty();
    }
}
