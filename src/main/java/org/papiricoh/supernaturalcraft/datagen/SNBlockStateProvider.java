package org.papiricoh.supernaturalcraft.datagen;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.hunter.DevilsTrapBlock;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.ritual.block.FlatLineBlock;

public class SNBlockStateProvider extends BlockStateProvider {

    public SNBlockStateProvider(PackOutput output, ExistingFileHelper existing) {
        super(output, SupernaturalCraft.MODID, existing);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlockWithItem(AllBlocks.ROCK_SALT_ORE.get(), cubeAll(AllBlocks.ROCK_SALT_ORE.get()));
        simpleBlockWithItem(AllBlocks.DEEPSLATE_ROCK_SALT_ORE.get(), cubeAll(AllBlocks.DEEPSLATE_ROCK_SALT_ORE.get()));
        simpleBlockWithItem(AllBlocks.NETHER_SULFUR_ORE.get(), cubeAll(AllBlocks.NETHER_SULFUR_ORE.get()));

        simpleBlockWithItem(AllBlocks.RITUAL_ALTAR.get(), models().getExistingFile(modLoc("block/ritual_altar")));
        simpleBlockWithItem(AllBlocks.CHOIR_ALTAR.get(), models().getExistingFile(modLoc("block/choir_altar")));
        for (var bell : AllBlocks.CHOIR_BELLS) {
            var model = models().getExistingFile(modLoc("block/" + bell.getId().getPath()));
            getVariantBuilder(bell.get()).forAllStates(st -> net.neoforged.neoforge.client.model.generators.ConfiguredModel.builder()
                    .modelFile(model).build());
            simpleBlockItem(bell.get(), model);
        }

        horizontalBlock(AllBlocks.MORNINGSTAR_TROPHY.get(), models().getExistingFile(modLoc("block/morningstar_trophy")));
        horizontalBlock(AllBlocks.ECLIPSE_TROPHY.get(), models().getExistingFile(modLoc("block/eclipse_trophy")));
        horizontalBlock(AllBlocks.CHOIR_TROPHY.get(), models().getExistingFile(modLoc("block/choir_trophy")));
        var wellLit = models().cubeBottomTop("light_well", modLoc("block/light_well_side"), modLoc("block/light_well_bottom"),
                modLoc("block/light_well_top_lit"));
        var wellOut = models().cubeBottomTop("light_well_out", modLoc("block/light_well_side_out"), modLoc("block/light_well_bottom"),
                modLoc("block/light_well_top"));
        getVariantBuilder(AllBlocks.LIGHT_WELL.get()).forAllStates(st -> net.neoforged.neoforge.client.model.generators.ConfiguredModel.builder()
                .modelFile(st.getValue(org.papiricoh.supernaturalcraft.light.LightWellBlock.LIT) ? wellLit : wellOut).build());
        horizontalBlock(AllBlocks.HELLFORGE.get(), models().orientable("hellforge", modLoc("block/hellforge_side"),
                modLoc("block/hellforge_front"), modLoc("block/hellforge_top")));
        simpleBlockItem(AllBlocks.HELLFORGE.get(), models().getExistingFile(modLoc("block/hellforge")));
        simpleBlock(AllBlocks.HELLFIRE_CRACK.get());
        simpleBlock(AllBlocks.CAGE_FROST.get());
        simpleBlock(AllBlocks.SERAPHIC_PILLAR.get());
        simpleBlock(AllBlocks.CAGE_ICE.get(), models().cubeAll("cage_ice", modLoc("block/cage_ice")).renderType("translucent"));

        floorLine(AllBlocks.SALT_LINE.get(), "salt_line");
        floorLine(AllBlocks.CHALK_LINE.get(), "chalk_line");
        floorLine(AllBlocks.BLOOD_CHALK_LINE.get(), "blood_chalk_line");

        getVariantBuilder(AllBlocks.DEVILS_TRAP.get()).forAllStates(state -> {
            int part = state.getValue(DevilsTrapBlock.PART);
            return ConfiguredModel.builder().modelFile(decal("devils_trap_" + part, "devils_trap_" + part, false)).build();
        });
    }

    ModelFile decal(String name, String texture, boolean top) {
        return models().withExistingParent(name, modLoc(top ? "block/template_floor_decal_top" : "block/template_floor_decal"))
                .texture("decal", modLoc("block/" + texture));
    }

    /** A dot in the middle plus one stroke per connected side, like redstone dust. */
    private void floorLine(Block block, String name) {
        ModelFile dot = decal(name + "_dot", name + "_dot", true);
        ModelFile side = decal(name + "_side", name + "_side", false);
        MultiPartBlockStateBuilder builder = getMultipartBuilder(block);
        builder.part().modelFile(dot).addModel().end();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            builder.part().modelFile(side).rotationY(((int) dir.toYRot() + 180) % 360).addModel()
                    .condition(FlatLineBlock.BY_DIRECTION.get(dir), true).end();
        }
    }
}
