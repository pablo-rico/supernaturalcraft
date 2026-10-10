package org.papiricoh.supernaturalcraft.datagen.heaven;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootTable;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.function.BiConsumer;

/**
 * v0.18's entity loot tables, called from {@code SNEntityLoot} (owned by the journal and data work). The bosses' spoils are dealt
 * by code, per hunter ({@code NaomiSpoils}, {@code ZachariahSpoils}); their tables stay empty.
 */
public final class HeavenLoot {

    private HeavenLoot() {
    }

    public static void entities(BiConsumer<EntityType<?>, LootTable.Builder> add) {
        add.accept(AllEntities.NAOMI.get(), LootTable.lootTable());
        add.accept(AllEntities.ZACHARIAH.get(), LootTable.lootTable());
        add.accept(AllEntities.HEAVEN_GUARD.get(), LootTable.lootTable());
        add.accept(AllEntities.CLERK_ANGEL.get(), LootTable.lootTable());
        add.accept(AllEntities.TRAINING_COPY.get(), LootTable.lootTable());
    }
}
