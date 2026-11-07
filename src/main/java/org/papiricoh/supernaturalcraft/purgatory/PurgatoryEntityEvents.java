package org.papiricoh.supernaturalcraft.purgatory;

import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/** The attributes and spawn rules of Purgatory's creatures and the Alphas (v0.39; mod bus). */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class PurgatoryEntityEvents {

    private PurgatoryEntityEvents() {
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(AllEntities.ALPHA_VAMPIRE.get(), org.papiricoh.supernaturalcraft.entity.alpha.AlphaVampireEntity.createAttributes().build());
        event.put(AllEntities.ALPHA_WEREWOLF.get(), org.papiricoh.supernaturalcraft.entity.alpha.AlphaWerewolfEntity.createAttributes().build());
        event.put(AllEntities.ALPHA_SHAPESHIFTER.get(), org.papiricoh.supernaturalcraft.entity.alpha.AlphaShapeshifterEntity.createAttributes().build());
        event.put(AllEntities.PURGATORY_VAMPIRE.get(), org.papiricoh.supernaturalcraft.purgatory.mob.PurgatoryVampireEntity.createAttributes().build());
        event.put(AllEntities.PURGATORY_WEREWOLF.get(), org.papiricoh.supernaturalcraft.purgatory.mob.PurgatoryWerewolfEntity.createAttributes().build());
    }

    /** The souls walk Purgatory's ground like any monster (its biomes are the only ones that spawn them). */
    @SubscribeEvent
    public static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(AllEntities.PURGATORY_VAMPIRE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(AllEntities.PURGATORY_WEREWOLF.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
