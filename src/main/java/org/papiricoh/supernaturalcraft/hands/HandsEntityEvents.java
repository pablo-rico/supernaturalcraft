package org.papiricoh.supernaturalcraft.hands;

import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.hands.mob.BansheeEntity;
import org.papiricoh.supernaturalcraft.hands.mob.ChitterEntity;
import org.papiricoh.supernaturalcraft.hands.mob.ThuleOfficerEntity;
import org.papiricoh.supernaturalcraft.hands.mob.ThuleSoldierEntity;

/** The attributes and spawn placements of season 11's creatures (v0.42; mod bus). Their wild spawning is {@link HandsSpawns}'. */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class HandsEntityEvents {

    private HandsEntityEvents() {
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(HandsRegistry.THULE_SOLDIER.get(), ThuleSoldierEntity.createAttributes().build());
        event.put(HandsRegistry.THULE_OFFICER.get(), ThuleOfficerEntity.createAttributes().build());
        event.put(HandsRegistry.BANSHEE.get(), BansheeEntity.createAttributes().build());
        event.put(HandsRegistry.CHITTER.get(), ChitterEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(HandsRegistry.THULE_SOLDIER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(HandsRegistry.THULE_OFFICER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(HandsRegistry.BANSHEE.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BansheeEntity::canSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(HandsRegistry.CHITTER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ChitterEntity::canSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
