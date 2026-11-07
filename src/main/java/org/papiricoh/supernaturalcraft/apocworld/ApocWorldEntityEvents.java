package org.papiricoh.supernaturalcraft.apocworld;

import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.apocworld.army.AuPrisonerEntity;
import org.papiricoh.supernaturalcraft.apocworld.army.AuSoldierEntity;
import org.papiricoh.supernaturalcraft.entity.boss.aumichael.AuMichaelEntity;

/** The attributes and spawn rules of the Apocalypse World's creatures and AU Michael (v0.41; mod bus). */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class ApocWorldEntityEvents {

    private ApocWorldEntityEvents() {
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(ApocWorldRegistry.AU_ANGEL_SOLDIER.get(), AuSoldierEntity.createAttributes().build());
        event.put(ApocWorldRegistry.AU_PRISONER.get(), AuPrisonerEntity.createAttributes().build());
        event.put(ApocWorldRegistry.AU_MICHAEL.get(), AuMichaelEntity.createAttributes().build());
    }

    /** Michael's soldiers walk the burned world's ground in any light (its biomes are the only ones that spawn them). */
    @SubscribeEvent
    public static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ApocWorldRegistry.AU_ANGEL_SOLDIER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                AuSoldierEntity::canSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
