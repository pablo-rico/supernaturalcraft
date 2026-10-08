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
        horizontalBlock(AllBlocks.AZAZEL_TROPHY.get(), models().getExistingFile(modLoc("block/azazel_trophy")));
        horizontalBlock(AllBlocks.LILITH_TROPHY.get(), models().getExistingFile(modLoc("block/lilith_trophy")));
        horizontalBlock(AllBlocks.METATRON_TROPHY.get(), models().getExistingFile(modLoc("block/metatron_trophy")));
        simpleBlock(AllBlocks.SCRIPTURE_STONE.get());
        getVariantBuilder(AllBlocks.CRACKED_HEADSTONE.get()).forAllStates(st -> {
            String half = st.getValue(org.papiricoh.supernaturalcraft.entity.boss.lilith.HeadstoneBlock.HALF)
                    == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER ? "lower" : "upper";
            int cracks = st.getValue(org.papiricoh.supernaturalcraft.entity.boss.lilith.HeadstoneBlock.CRACKS);
            var model = models().getExistingFile(modLoc("block/cracked_headstone_" + half + "_" + cracks));
            int y = ((int) st.getValue(org.papiricoh.supernaturalcraft.entity.boss.lilith.HeadstoneBlock.FACING).toYRot() + 180) % 360;
            return ConfiguredModel.builder().modelFile(model).rotationY(y).build();
        });
        coltRail();
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

        // v0.8: models written by tools/artgen (bowl_art.py, ghost_art.py).
        var bowl = models().getExistingFile(modLoc("block/spell_bowl"));
        getVariantBuilder(AllBlocks.SPELL_BOWL.get()).forAllStates(st -> ConfiguredModel.builder().modelFile(bowl).build());
        var curseBag = models().getExistingFile(modLoc("block/curse_bag"));
        getVariantBuilder(AllBlocks.CURSE_BAG.get()).forAllStates(st -> ConfiguredModel.builder().modelFile(curseBag).build());
        var bones = models().getExistingFile(modLoc("block/grave_bones"));
        var bonesSalted = models().getExistingFile(modLoc("block/grave_bones_salted"));
        var bonesRested = models().getExistingFile(modLoc("block/grave_bones_rested"));
        getVariantBuilder(AllBlocks.GRAVE_BONES.get()).forAllStates(st -> ConfiguredModel.builder().modelFile(
                st.getValue(org.papiricoh.supernaturalcraft.grave.GraveBonesBlock.RESTED) ? bonesRested
                        : st.getValue(org.papiricoh.supernaturalcraft.grave.GraveBonesBlock.SALTED) ? bonesSalted : bones).build());
        horizontalBlock(AllBlocks.GRAVE_HEADSTONE.get(), models().getExistingFile(modLoc("block/grave_headstone")));
        simpleBlockWithItem(AllBlocks.GRAVE_SOIL.get(), models().cubeBottomTop("grave_soil", modLoc("block/grave_soil_side"),
                mcLoc("block/dirt"), modLoc("block/grave_soil")));

        hell();

        floorLine(AllBlocks.SALT_LINE.get(), "salt_line");
        floorLine(AllBlocks.CHALK_LINE.get(), "chalk_line");
        floorLine(AllBlocks.BLOOD_CHALK_LINE.get(), "blood_chalk_line");

        getVariantBuilder(AllBlocks.DEVILS_TRAP.get()).forAllStates(state -> {
            int part = state.getValue(DevilsTrapBlock.PART);
            return ConfiguredModel.builder().modelFile(decal("devils_trap_" + part, "devils_trap_" + part, false)).build();
        });
        org.papiricoh.supernaturalcraft.datagen.chuck.ChuckAssetData.blockStates(this);
        org.papiricoh.supernaturalcraft.datagen.horsemen.HorsemenAssetData.blockStates(this);
        org.papiricoh.supernaturalcraft.datagen.michael.MichaelAssetData.blockStates(this);
        org.papiricoh.supernaturalcraft.datagen.allegiance.AllegianceAssetData.blockStates(this);
        org.papiricoh.supernaturalcraft.datagen.gabriel.GabrielAssetData.blockStates(this);
    }

    /** Hell's stone, its ores, and the Cage's unbreakable fittings. */
    private void hell() {
        for (var b : java.util.List.of(AllBlocks.HELLSTONE, AllBlocks.HELLSTONE_BRICKS, AllBlocks.RACK_STONE, AllBlocks.CONGEALED_BLOOD,
                AllBlocks.ASH_BLOCK, AllBlocks.BRIMSTONE_ORE, AllBlocks.CORRIDOR_STONE, AllBlocks.CORRIDOR_BRICKS, AllBlocks.ABYSSAL_STONE,
                AllBlocks.ABYSSAL_SHARD_ORE, AllBlocks.CAGE_FRAME, AllBlocks.CAGE_SEAL, AllBlocks.ABYSSAL_BEDROCK, AllBlocks.ABYSSAL_FLAGSTONE)) {
            simpleBlockWithItem(b.get(), cubeAll(b.get()));
        }
        simpleBlockWithItem(AllBlocks.HELLFIRE_VENT.get(), models().cubeBottomTop("hellfire_vent", modLoc("block/hellfire_vent_side"),
                modLoc("block/hellstone"), modLoc("block/hellfire_vent_top")));
        simpleBlockWithItem(AllBlocks.CAGE_RITUAL_STONE.get(), models().cubeBottomTop("cage_ritual_stone", modLoc("block/cage_ritual_stone_side"),
                modLoc("block/abyssal_bedrock"), modLoc("block/cage_ritual_stone")));
        axisBlock(AllBlocks.ENOCHIAN_PILLAR.get(), modLoc("block/enochian_pillar"), modLoc("block/enochian_pillar_top"));
        simpleBlockItem(AllBlocks.ENOCHIAN_PILLAR.get(), models().getExistingFile(modLoc("block/enochian_pillar")));
        paneBlockWithRenderType(AllBlocks.CAGE_BARS.get(), modLoc("block/cage_bars"), modLoc("block/cage_bars"), "cutout");
        ModelFile chain = models().withExistingParent("cage_chain", mcLoc("block/chain"))
                .texture("all", modLoc("block/cage_chain")).texture("particle", modLoc("block/cage_chain")).renderType("cutout");
        getVariantBuilder(AllBlocks.CAGE_CHAIN.get()).forAllStates(st -> {
            var axis = st.getValue(net.minecraft.world.level.block.ChainBlock.AXIS);
            return ConfiguredModel.builder().modelFile(chain)
                    .rotationX(axis == net.minecraft.core.Direction.Axis.Y ? 0 : 90)
                    .rotationY(axis == net.minecraft.core.Direction.Axis.X ? 90 : 0).build();
        });
        simpleBlock(AllBlocks.MEAT_HOOK.get(), models().cross("meat_hook", modLoc("block/meat_hook")).renderType("cutout"));
        var brazier = models().getBuilder("hellfire_brazier").parent(new ModelFile.UncheckedModelFile("block/block"))
                .texture("particle", modLoc("block/hellfire_brazier_side"))
                .texture("side", modLoc("block/hellfire_brazier_side")).texture("top", modLoc("block/hellfire_brazier_top"))
                .texture("stem", modLoc("block/cage_frame"));
        brazier.element().from(5, 0, 5).to(11, 10, 11).allFaces((d, f) -> f.texture("#stem")).end();
        brazier.element().from(1, 10, 1).to(15, 14, 15).allFaces((d, f) -> f.texture(d == Direction.UP ? "#top" : "#side")).end();
        brazier.renderType("cutout");
        simpleBlockWithItem(AllBlocks.HELLFIRE_BRAZIER.get(), brazier);
        ModelFile ns = models().withExistingParent("hell_rift_ns", mcLoc("block/nether_portal_ns"))
                .texture("portal", modLoc("block/hell_rift")).texture("particle", modLoc("block/hell_rift")).renderType("translucent");
        ModelFile ew = models().withExistingParent("hell_rift_ew", mcLoc("block/nether_portal_ew"))
                .texture("portal", modLoc("block/hell_rift")).texture("particle", modLoc("block/hell_rift")).renderType("translucent");
        getVariantBuilder(AllBlocks.HELL_RIFT.get()).forAllStates(st -> ConfiguredModel.builder()
                .modelFile(st.getValue(org.papiricoh.supernaturalcraft.hell.rift.HellRiftBlock.AXIS) == net.minecraft.core.Direction.Axis.X ? ns : ew)
                .build());
    }

    /** Samuel Colt's rails: one straight and one diagonal drawing (turned for the other two ways) and a crossing, charged or cold. */
    private void coltRail() {
        java.util.Map<String, ModelFile> models = new java.util.HashMap<>();
        for (String kind : new String[]{"straight", "diagonal", "cross"}) {
            for (String heat : new String[]{"", "_charged"}) {
                models.put(kind + heat, decal("colt_rail_" + kind + heat, "colt_rail_" + kind + heat, false));
            }
        }
        getVariantBuilder(AllBlocks.COLT_RAIL.get()).forAllStates(st -> {
            var shape = st.getValue(org.papiricoh.supernaturalcraft.entity.boss.azazel.ColtRailBlock.SHAPE);
            String heat = st.getValue(org.papiricoh.supernaturalcraft.entity.boss.azazel.ColtRailBlock.CHARGED) ? "_charged" : "";
            String kind = switch (shape) {
                case NS, EW -> "straight";
                case DIAG_A, DIAG_B -> "diagonal";
                case CROSS -> "cross";
            };
            int turn = shape == org.papiricoh.supernaturalcraft.entity.boss.azazel.RailTrapLayout.Shape.EW
                    || shape == org.papiricoh.supernaturalcraft.entity.boss.azazel.RailTrapLayout.Shape.DIAG_B ? 90 : 0;
            return ConfiguredModel.builder().modelFile(models.get(kind + heat)).rotationY(turn).build();
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
