package org.papiricoh.supernaturalcraft.entity;

import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferIllusion;
import org.papiricoh.supernaturalcraft.entity.demon.BlackEyedDemon;
import org.papiricoh.supernaturalcraft.entity.demon.DemonOccultist;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public class SNEntityEvents {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(AllEntities.BLACK_EYED_DEMON.get(), BlackEyedDemon.createAttributes().build());
        event.put(AllEntities.DEMON_OCCULTIST.get(), DemonOccultist.createAttributes().build());
        event.put(AllEntities.LUCIFER.get(), LuciferEntity.createAttributes().build());
        event.put(AllEntities.AMARA.get(), org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity.createAttributes().build());
        event.put(AllEntities.AMARA_SHADE.get(), org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraShade.createAttributes().build());
        event.put(AllEntities.BROKEN_CHORUS.get(), org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusEntity.createAttributes().build());
        event.put(AllEntities.CHOIR_ECHO.get(), org.papiricoh.supernaturalcraft.entity.boss.chorus.ChoirEchoEntity.createAttributes().build());
        event.put(AllEntities.LUCIFER_ILLUSION.get(), LuciferIllusion.createAttributes().build());
        event.put(AllEntities.HELLHOUND.get(), org.papiricoh.supernaturalcraft.entity.hellhound.HellhoundEntity.createAttributes().build());
        event.put(AllEntities.LUCIFER_UNCAGED.get(), org.papiricoh.supernaturalcraft.entity.boss.uncaged.LuciferUncagedEntity.createAttributes().build());
        event.put(AllEntities.CAGED_LUCIFER.get(), org.papiricoh.supernaturalcraft.hell.cage.CagedLuciferEntity.createAttributes().build());
        event.put(AllEntities.AZAZEL.get(), org.papiricoh.supernaturalcraft.entity.boss.azazel.AzazelEntity.createAttributes().build());
        event.put(AllEntities.LILITH.get(), org.papiricoh.supernaturalcraft.entity.boss.lilith.LilithEntity.createAttributes().build());
        event.put(AllEntities.BOUND_HELLHOUND.get(), org.papiricoh.supernaturalcraft.entity.hellhound.HellhoundEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(AllEntities.BLACK_EYED_DEMON.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(AllEntities.DEMON_OCCULTIST.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(AllEntities.HELLHOUND.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
