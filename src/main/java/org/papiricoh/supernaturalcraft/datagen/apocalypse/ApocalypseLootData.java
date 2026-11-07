package org.papiricoh.supernaturalcraft.datagen.apocalypse;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootTable;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.function.BiConsumer;

/** The core's entity loot (v0.40): Alastair's and Ruby's spoils are given by code, per hunter; their tables stay empty. */
public final class ApocalypseLootData {

    private ApocalypseLootData() {
    }

    public static void entities(BiConsumer<EntityType<?>, LootTable.Builder> add) {
        add.accept(AllEntities.ALASTAIR.get(), LootTable.lootTable());
        add.accept(AllEntities.RUBY.get(), LootTable.lootTable());
    }
}
