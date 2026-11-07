package org.papiricoh.supernaturalcraft.apocalypse.seal;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/** The attributes (and, as the slice needs, spawn rules) of the seals' creatures and Uriel (v0.40, the Seals slice; mod bus). */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class SealEntityEvents {

    static {
        // At mod construction (this subscriber is loaded then), before any recipe is read: the seals' rite effects and
        // Castiel's talk. Idempotent.
        SealRites.bootstrap();
        SealEnvoy.register();
    }

    private SealEntityEvents() {
    }

    /** A second chance, should the class have been loaded late. */
    @SubscribeEvent
    public static void setup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            SealRites.bootstrap();
            SealEnvoy.register();
        });
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(AllEntities.URIEL.get(), org.papiricoh.supernaturalcraft.entity.boss.uriel.UrielEntity.createAttributes().build());
        event.put(AllEntities.REBEL_ANGEL.get(), Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 60).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 7).add(Attributes.FOLLOW_RANGE, 32).add(Attributes.ARMOR, 6).build());
        event.put(AllEntities.WITNESS.get(), Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 24).add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 4).add(Attributes.FOLLOW_RANGE, 32).build());
        event.put(AllEntities.SEAL_REAPER.get(), Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE, 0).build());
        event.put(AllEntities.CASTIEL_ENVOY.get(), CastielEnvoyEntity.createAttributes().build());
    }
}
