package org.papiricoh.supernaturalcraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.ritual.block.RitualAltarBlockEntity;

public class AllBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SupernaturalCraft.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RitualAltarBlockEntity>> RITUAL_ALTAR =
            BLOCK_ENTITIES.register("ritual_altar", () -> BlockEntityType.Builder
                    .of(RitualAltarBlockEntity::new, AllBlocks.RITUAL_ALTAR.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<org.papiricoh.supernaturalcraft.chorus.ChoirAltarBlockEntity>> CHOIR_ALTAR =
            BLOCK_ENTITIES.register("choir_altar", () -> BlockEntityType.Builder
                    .of(org.papiricoh.supernaturalcraft.chorus.ChoirAltarBlockEntity::new, AllBlocks.CHOIR_ALTAR.get()).build(null));

    public static void init() {
    }
}
