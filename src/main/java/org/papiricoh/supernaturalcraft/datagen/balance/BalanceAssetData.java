package org.papiricoh.supernaturalcraft.datagen.balance;

import org.papiricoh.supernaturalcraft.balance.ProgressionScale;
import org.papiricoh.supernaturalcraft.datagen.SNItemModelProvider;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.ArrayList;
import java.util.List;

/**
 * Item models of the power curve (v0.15): the five Ascension Shards and the four pieces of Hunter's Gear, flat icons drawn by
 * tools/artgen/balance_art.py (which also draws the gear's armour layers, {@code textures/models/armor/hunter_layer_*}).
 */
public final class BalanceAssetData {

    private BalanceAssetData() {
    }

    public static void itemModels(SNItemModelProvider p) {
        List<String> ids = new ArrayList<>();
        for (int tier = 1; tier <= ProgressionScale.MAX_TIER; tier++) ids.add(AllItems.shardOf(tier).getId().getPath());
        for (var gear : List.of(AllItems.HUNTERS_CAP, AllItems.HUNTERS_JACKET, AllItems.HUNTERS_JEANS, AllItems.HUNTERS_BOOTS)) {
            ids.add(gear.getId().getPath());
        }
        for (String id : ids) p.withExistingParent(id, p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/" + id));
    }
}
