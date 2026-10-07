package org.papiricoh.supernaturalcraft.entity.ghost;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.projectile.HolyWaterProjectile;

/** Holy water scatters any ghost caught in its splash (the flask itself only burns demons). */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class GhostEvents {

    /** How far a holy water splash reaches a ghost. */
    public static final double SPLASH = 3.0;

    private GhostEvents() {
    }

    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof HolyWaterProjectile flask) || !(flask.level() instanceof ServerLevel level)) return;
        splash(level, event.getRayTraceResult().getLocation());
    }

    /** Disperses every ghost within {@link #SPLASH} of {@code at}; returns how many. */
    public static int splash(ServerLevel level, Vec3 at) {
        int n = 0;
        for (GhostEntity g : level.getEntitiesOfClass(GhostEntity.class, new AABB(at, at).inflate(SPLASH + 1))) {
            if (!g.isInert() && g.position().add(0, g.getBbHeight() / 2, 0).distanceToSqr(at) <= (SPLASH + 0.5) * (SPLASH + 0.5)) {
                g.disperse();
                n++;
            }
        }
        return n;
    }
}
