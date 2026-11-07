package org.papiricoh.supernaturalcraft.apocalypse;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/** The attributes of the core's new creatures (v0.40; mod bus): Alastair, Ruby and Ruby the story NPC. */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class ApocalypseEntityEvents {

    private ApocalypseEntityEvents() {
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(AllEntities.ALASTAIR.get(), org.papiricoh.supernaturalcraft.entity.boss.alastair.AlastairEntity.createAttributes().build());
        event.put(AllEntities.RUBY.get(), Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 200).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 8).add(Attributes.FOLLOW_RANGE, 40).add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.ARMOR, 6).build());
        event.put(AllEntities.RUBY_NPC.get(), org.papiricoh.supernaturalcraft.apocalypse.ruby.RubyNpcEntity.createAttributes().build());
    }
}
