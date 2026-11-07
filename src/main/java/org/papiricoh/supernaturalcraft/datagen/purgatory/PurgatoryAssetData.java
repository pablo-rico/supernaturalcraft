package org.papiricoh.supernaturalcraft.datagen.purgatory;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import org.papiricoh.supernaturalcraft.datagen.SNBlockStateProvider;
import org.papiricoh.supernaturalcraft.datagen.SNItemModelProvider;
import org.papiricoh.supernaturalcraft.purgatory.rift.PurgatoryRiftBlock;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/**
 * Purgatory's block states and models (v0.39), called from {@code SNBlockStateProvider} and {@code SNItemModelProvider}: the log
 * (bark and rings), leaves (cutout, untinted), the stones, soul amber ore, the rift (Hell's portal shapes, translucent); the soul
 * amber and the Heartwood's sprites, the eggs.
 */
public final class PurgatoryAssetData {

    private PurgatoryAssetData() {
    }

    public static void blockStates(SNBlockStateProvider p) {
        p.logBlock(AllBlocks.PURGATORY_LOG.get());
        p.simpleBlockItem(AllBlocks.PURGATORY_LOG.get(), new ModelFile.UncheckedModelFile(p.modLoc("block/purgatory_log")));
        p.axisBlock(AllBlocks.PURGATORY_WOOD.get(), p.modLoc("block/purgatory_log"), p.modLoc("block/purgatory_log"));
        p.simpleBlockItem(AllBlocks.PURGATORY_WOOD.get(), new ModelFile.UncheckedModelFile(p.modLoc("block/purgatory_wood")));
        p.simpleBlockWithItem(AllBlocks.PURGATORY_LEAVES.get(), p.models().cubeAll("purgatory_leaves", p.modLoc("block/purgatory_leaves"))
                .renderType("cutout_mipped"));
        for (var b : java.util.List.of(AllBlocks.PURGATORY_PLANKS, AllBlocks.PURGATORY_STONE, AllBlocks.PURGATORY_COBBLESTONE,
                AllBlocks.PURGATORY_STONE_BRICKS, AllBlocks.SOUL_AMBER_ORE)) {
            p.simpleBlockWithItem(b.get(), p.cubeAll(b.get()));
        }
        ModelFile ns = p.models().withExistingParent("purgatory_rift_ns", p.mcLoc("block/nether_portal_ns"))
                .texture("portal", p.modLoc("block/purgatory_rift")).texture("particle", p.modLoc("block/purgatory_rift")).renderType("translucent");
        ModelFile ew = p.models().withExistingParent("purgatory_rift_ew", p.mcLoc("block/nether_portal_ew"))
                .texture("portal", p.modLoc("block/purgatory_rift")).texture("particle", p.modLoc("block/purgatory_rift")).renderType("translucent");
        p.getVariantBuilder(AllBlocks.PURGATORY_RIFT.get()).forAllStates(st -> ConfiguredModel.builder()
                .modelFile(st.getValue(PurgatoryRiftBlock.AXIS) == Direction.Axis.X ? ns : ew).build());
    }

    public static void itemModels(SNItemModelProvider p) {
        p.basicItem(AllItems.SOUL_AMBER.get());
        p.basicItem(AllItems.MOTHERS_HEARTWOOD.get());
        for (var egg : java.util.List.of(AllItems.ALPHA_VAMPIRE_SPAWN_EGG, AllItems.ALPHA_WEREWOLF_SPAWN_EGG, AllItems.ALPHA_SHAPESHIFTER_SPAWN_EGG,
                AllItems.PURGATORY_VAMPIRE_SPAWN_EGG, AllItems.PURGATORY_WEREWOLF_SPAWN_EGG)) {
            p.withExistingParent(egg.getId().getPath(), p.mcLoc("item/template_spawn_egg"));
        }
    }
}
