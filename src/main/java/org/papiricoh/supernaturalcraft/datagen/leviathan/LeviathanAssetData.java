package org.papiricoh.supernaturalcraft.datagen.leviathan;

import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import org.papiricoh.supernaturalcraft.datagen.SNBlockStateProvider;
import org.papiricoh.supernaturalcraft.leviathan.LeviathanGooBlock;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;

/** The Leviathans' block models (v0.25): the goo is a floor decal, wet or dried white. */
public final class LeviathanAssetData {

    private LeviathanAssetData() {
    }

    public static void blockStates(SNBlockStateProvider p) {
        String goo = AllBlocks.LEVIATHAN_GOO.getId().getPath();
        ModelFile wet = p.models().withExistingParent(goo, p.modLoc("block/template_floor_decal")).texture("decal", p.modLoc("block/" + goo));
        ModelFile dried = p.models().withExistingParent(goo + "_dried", p.modLoc("block/template_floor_decal"))
                .texture("decal", p.modLoc("block/" + goo + "_dried"));
        p.getVariantBuilder(AllBlocks.LEVIATHAN_GOO.get()).forAllStates(state ->
                ConfiguredModel.builder().modelFile(state.getValue(LeviathanGooBlock.DRIED) ? dried : wet).build());
    }
}
