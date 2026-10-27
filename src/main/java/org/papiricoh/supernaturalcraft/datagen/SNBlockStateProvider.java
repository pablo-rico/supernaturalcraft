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
        guildStandard();
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
        org.papiricoh.supernaturalcraft.datagen.raphael.RaphaelAssetData.blockStates(this);
        org.papiricoh.supernaturalcraft.datagen.leviathan.LeviathanAssetData.blockStates(this);
        // v0.21 (P4): Cain's bust, ART's own block model (block/cain_trophy, on trophy_base), turned by FACING.
        horizontalBlock(AllBlocks.CAIN_TROPHY.get(), new net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile(
                modLoc("block/" + AllBlocks.CAIN_TROPHY.getId().getPath())));
        // v0.21 (P5): Abaddon's bust, ART's own block model (block/abaddon_trophy), turned by FACING.
        horizontalBlock(AllBlocks.ABADDON_TROPHY.get(), new net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile(
                modLoc("block/" + AllBlocks.ABADDON_TROPHY.getId().getPath())));
        // v0.21 (P6): the witch bosses' busts, ART's own block models, turned by FACING.
        for (var trophy : java.util.List.of(AllBlocks.ROWENA_TROPHY, AllBlocks.GRAND_WITCH_TROPHY)) {
            horizontalBlock(trophy.get(), models().getExistingFile(modLoc("block/" + trophy.getId().getPath())));
        }
        org.papiricoh.supernaturalcraft.datagen.legacy.LegacyAssetData.blockStates(this);
        org.papiricoh.supernaturalcraft.datagen.heaven.HeavenAssetData.blockStates(this);
    }

    /**
     * The Cage's fittings with real shapes: their block models (elements) come from tools/artgen/hell_art.py, only the
     * blockstates are written here. Bars and posts are multipart like vanilla iron bars.
     */
    private void cageFittings() {
        ModelFile pillar = models().getExistingFile(modLoc("block/enochian_pillar"));
        axisRotated(AllBlocks.ENOCHIAN_PILLAR.get(), pillar, net.minecraft.world.level.block.RotatedPillarBlock.AXIS);
        simpleBlockItem(AllBlocks.ENOCHIAN_PILLAR.get(), pillar);
        ModelFile chain = models().getExistingFile(modLoc("block/cage_chain"));
        axisRotated(AllBlocks.CAGE_CHAIN.get(), chain, net.minecraft.world.level.block.ChainBlock.AXIS);
        ModelFile side = models().getExistingFile(modLoc("block/cage_bars_side"));
        bars(AllBlocks.CAGE_BARS.get(), models().getExistingFile(modLoc("block/cage_bars_post")), side);
        ModelFile post = models().getExistingFile(modLoc("block/cage_post"));
        bars(AllBlocks.CAGE_POST.get(), post, side);
        simpleBlockItem(AllBlocks.CAGE_POST.get(), models().getExistingFile(modLoc("block/cage_post_inventory")));
        simpleBlockWithItem(AllBlocks.HELLFIRE_BRAZIER.get(), models().getExistingFile(modLoc("block/hellfire_brazier")));
        ModelFile capital = models().getExistingFile(modLoc("block/enochian_capital"));
        getVariantBuilder(AllBlocks.ENOCHIAN_CAPITAL.get()).forAllStates(st -> ConfiguredModel.builder().modelFile(capital)
                .rotationX(st.getValue(org.papiricoh.supernaturalcraft.hell.block.EnochianCapitalBlock.HALF)
                        == net.minecraft.world.level.block.state.properties.Half.TOP ? 0 : 180).build());
        simpleBlockItem(AllBlocks.ENOCHIAN_CAPITAL.get(), capital);
        var flag = modLoc("block/abyssal_flagstone");
        ModelFile stairs = models().stairs("abyssal_flagstone_stairs", flag, flag, flag);
        stairsBlock(AllBlocks.ABYSSAL_FLAGSTONE_STAIRS.get(), stairs,
                models().stairsInner("abyssal_flagstone_stairs_inner", flag, flag, flag),
                models().stairsOuter("abyssal_flagstone_stairs_outer", flag, flag, flag));
        simpleBlockItem(AllBlocks.ABYSSAL_FLAGSTONE_STAIRS.get(), stairs);
        ModelFile slab = models().slab("abyssal_flagstone_slab", flag, flag, flag);
        slabBlock(AllBlocks.ABYSSAL_FLAGSTONE_SLAB.get(), slab, models().slabTop("abyssal_flagstone_slab_top", flag, flag, flag),
                cubeAll(AllBlocks.ABYSSAL_FLAGSTONE.get()));
        simpleBlockItem(AllBlocks.ABYSSAL_FLAGSTONE_SLAB.get(), slab);
        ModelFile lantern = models().getExistingFile(modLoc("block/seal_lantern"));
        ModelFile hanging = models().getExistingFile(modLoc("block/seal_lantern_hanging"));
        getVariantBuilder(AllBlocks.SEAL_LANTERN.get()).forAllStates(st -> ConfiguredModel.builder()
                .modelFile(st.getValue(net.minecraft.world.level.block.LanternBlock.HANGING) ? hanging : lantern).build());
        for (var b : java.util.List.of(AllBlocks.CHAIN_ANCHOR, AllBlocks.FALLEN_WATCHER)) {
            ModelFile model = models().getExistingFile(modLoc("block/" + b.getId().getPath()));
            horizontalBlock(b.get(), model);
            simpleBlockItem(b.get(), model);
        }
    }

    /** Y up as drawn; Z laid down along north-south, X turned on from there (as the vanilla chain and logs). */
    private void axisRotated(Block block, ModelFile model, net.minecraft.world.level.block.state.properties.EnumProperty<Direction.Axis> prop) {
        getVariantBuilder(block).forAllStates(st -> {
            var axis = st.getValue(prop);
            return ConfiguredModel.builder().modelFile(model)
                    .rotationX(axis == Direction.Axis.Y ? 0 : 90)
                    .rotationY(axis == Direction.Axis.X ? 90 : 0).build();
        });
    }

    /** Vanilla bars' multipart: the post always, one side model per joined side (drawn reaching north). */
    private void bars(Block block, ModelFile post, ModelFile side) {
        MultiPartBlockStateBuilder mp = getMultipartBuilder(block);
        mp.part().modelFile(post).addModel().end();
        int rot = 0;
        for (var prop : java.util.List.of(net.minecraft.world.level.block.CrossCollisionBlock.NORTH, net.minecraft.world.level.block.CrossCollisionBlock.EAST,
                net.minecraft.world.level.block.CrossCollisionBlock.SOUTH, net.minecraft.world.level.block.CrossCollisionBlock.WEST)) {
            mp.part().modelFile(side).rotationY(rot).addModel().condition(prop, true).end();
            rot += 90;
        }
    }

    /** Hell's stone, its ores, and the Cage's unbreakable fittings. */
    private void hell() {
        for (var b : java.util.List.of(AllBlocks.HELLSTONE, AllBlocks.HELLSTONE_BRICKS, AllBlocks.RACK_STONE, AllBlocks.CONGEALED_BLOOD,
                AllBlocks.ASH_BLOCK, AllBlocks.BRIMSTONE_ORE, AllBlocks.CORRIDOR_STONE, AllBlocks.CORRIDOR_BRICKS, AllBlocks.ABYSSAL_STONE,
                AllBlocks.ABYSSAL_SHARD_ORE, AllBlocks.CAGE_FRAME, AllBlocks.CAGE_SEAL, AllBlocks.ABYSSAL_BEDROCK, AllBlocks.ABYSSAL_FLAGSTONE,
                AllBlocks.ABYSSAL_BRICKS)) {
            simpleBlockWithItem(b.get(), cubeAll(b.get()));
        }
        simpleBlockWithItem(AllBlocks.HELLFIRE_VENT.get(), models().cubeBottomTop("hellfire_vent", modLoc("block/hellfire_vent_side"),
                modLoc("block/hellstone"), modLoc("block/hellfire_vent_top")));
        simpleBlockWithItem(AllBlocks.CAGE_RITUAL_STONE.get(), models().cubeBottomTop("cage_ritual_stone", modLoc("block/cage_ritual_stone_side"),
                modLoc("block/abyssal_bedrock"), modLoc("block/cage_ritual_stone")));
        simpleBlock(AllBlocks.MEAT_HOOK.get(), models().cross("meat_hook", modLoc("block/meat_hook")).renderType("cutout"));
        cageFittings();
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

    /**
     * The guild's standard (v0.29): a two-block pole with a crossbar and a cloth, one model per emblem. The cloth is tint 0 (the
     * guild's dye, {@code GuildStandardColors}); the emblem is drawn over it untinted. Turned with its facing.
     */
    private void guildStandard() {
        var models = new java.util.ArrayList<net.neoforged.neoforge.client.model.generators.BlockModelBuilder>();
        for (int e = 0; e < org.papiricoh.supernaturalcraft.chain.GuildRules.EMBLEMS; e++) {
            var m = models().withExistingParent("guild_standard_" + e, mcLoc("block/block"))
                    .texture("pole", modLoc("block/guild_standard_pole")).texture("cloth", modLoc("block/guild_standard_cloth"))
                    .texture("emblem", modLoc("block/guild_emblem_" + e)).texture("particle", modLoc("block/guild_standard_pole"))
                    .renderType("cutout");
            m.element().from(7, 0, 7).to(9, 30, 9).allFaces((dir, face) -> face.texture("#pole").uvs(7, 0, 9, 16)).end();
            m.element().from(2, 27, 7.5f).to(14, 28.5f, 8.5f).allFaces((dir, face) -> face.texture("#pole").uvs(0, 0, 16, 2)).end();
            m.element().from(3, 12, 7.75f).to(13, 27, 8.25f).allFaces((dir, face) -> face.texture("#cloth").uvs(0, 0, 16, 16).tintindex(0)).end();
            m.element().from(3, 12, 7.7f).to(13, 27, 8.3f)
                    .face(net.minecraft.core.Direction.NORTH).texture("#emblem").uvs(0, 0, 16, 16).end()
                    .face(net.minecraft.core.Direction.SOUTH).texture("#emblem").uvs(0, 0, 16, 16).end().end();
            models.add(m);
        }
        getVariantBuilder(AllBlocks.GUILD_STANDARD.get()).forAllStates(st -> ConfiguredModel.builder()
                .modelFile(models.get(st.getValue(org.papiricoh.supernaturalcraft.chain.GuildStandardBlock.EMBLEM)))
                .rotationY(((int) st.getValue(org.papiricoh.supernaturalcraft.chain.GuildStandardBlock.FACING).toYRot() + 180) % 360).build());
    }
}
