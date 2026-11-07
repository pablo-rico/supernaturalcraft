package org.papiricoh.supernaturalcraft.datagen.apocalypse;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.function.BiConsumer;

/**
 * The seals' entity loot (v0.40, the Seals slice): Uriel's spoils are given by code, per hunter ({@code UrielSpoils}), and a held
 * seal pays its family's spoil ({@code SealSpoils}); here only the odd one off a creature slain by a hunter: a Scorched Feather off a
 * rebel angel, a Witness Shroud off a Witness. A reaper leaves nothing.
 */
public final class SealLootData {

    /** The chance a hunter's kill drops the family's spoil. */
    public static final float REBEL_FEATHER = 0.35f, WITNESS_SHROUD = 0.25f;

    private SealLootData() {
    }

    public static void entities(BiConsumer<EntityType<?>, LootTable.Builder> add) {
        add.accept(AllEntities.URIEL.get(), LootTable.lootTable());
        add.accept(AllEntities.REBEL_ANGEL.get(), chance(AllItems.SCORCHED_FEATHER.get(), REBEL_FEATHER));
        add.accept(AllEntities.WITNESS.get(), chance(AllItems.WITNESS_SHROUD.get(), WITNESS_SHROUD));
        add.accept(AllEntities.SEAL_REAPER.get(), LootTable.lootTable());
    }

    private static LootTable.Builder chance(ItemLike item, float chance) {
        return LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                .when(LootItemKilledByPlayerCondition.killedByPlayer())
                .when(LootItemRandomChanceCondition.randomChance(chance))
                .add(LootItem.lootTableItem(item)));
    }
}
