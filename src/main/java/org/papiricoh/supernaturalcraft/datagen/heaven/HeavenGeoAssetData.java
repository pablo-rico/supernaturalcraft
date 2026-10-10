package org.papiricoh.supernaturalcraft.datagen.heaven;

import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.SeparateTransformsModelBuilder;
import org.papiricoh.supernaturalcraft.datagen.SNItemModelProvider;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

/**
 * v0.18's 3D assets as datagen sees them (owned by the 3D art work): the GeckoLib weapons' item models (Naomi's Drill,
 * Zachariah's Blade), the busts' item models and the spawn eggs. Called from {@code SNItemModelProvider}; the busts' block states
 * are {@link HeavenAssetData}'s (they point at the block models heaven_items_art.py writes).
 */
public final class HeavenGeoAssetData {

    /** The weapons' heights in model pixels (heaven_items_art.py prints them): the in-hand transforms shrink them to a sword's size. */
    static final float DRILL_HEIGHT = 28.6f;
    static final float BLADE_HEIGHT = 27.1f;

    private HeavenGeoAssetData() {
    }

    public static void itemModels(SNItemModelProvider p) {
        geoWeapon(p, AllItems.NAOMIS_DRILL.getId().getPath(), DRILL_HEIGHT);
        geoWeapon(p, AllItems.ZACHARIAHS_BLADE.getId().getPath(), BLADE_HEIGHT);
        for (var trophy : List.of(AllItems.NAOMI_TROPHY, AllItems.ZACHARIAH_TROPHY)) {
            String id = trophy.getId().getPath();
            p.withExistingParent(id, p.modLoc("block/" + id));
        }
        for (var egg : List.of(AllItems.NAOMI_SPAWN_EGG, AllItems.ZACHARIAH_SPAWN_EGG, AllItems.HEAVEN_GUARD_SPAWN_EGG,
                AllItems.CLERK_ANGEL_SPAWN_EGG, AllItems.ASH_SPAWN_EGG)) {
            p.withExistingParent(egg.getId().getPath(), p.mcLoc("item/template_spawn_egg"));
        }
    }

    /**
     * A GeckoLib weapon (upright and centred, like {@code SNItemModelProvider.geoWeapon}): the 3D model everywhere but the
     * inventory, where its hand-drawn icon ({@code item/<id>_icon}) is used. Vanilla's handheld transforms turned 45° and
     * scaled so a model {@code height} pixels tall reads like a sword.
     */
    private static void geoWeapon(SNItemModelProvider p, String name, float height) {
        float k = Math.min(1f, 20f / height);
        ModelFile entity = new ModelFile.UncheckedModelFile("builtin/entity");
        ItemModelBuilder base = p.nested().parent(entity).transforms()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(0, -90, 10).translation(0, 4, 0.5f).scale(0.85f * k).end()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(0, 90, -100).translation(0, 4, 0.5f).scale(0.85f * k).end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, -90, -20).translation(1.13f, 3.2f, 1.13f).scale(0.68f * k).end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 90, -70).translation(1.13f, 3.2f, 1.13f).scale(0.68f * k).end()
                .transform(ItemDisplayContext.GROUND).rotation(0, 0, -45).translation(0, 2, 0).scale(0.5f * k).end()
                .transform(ItemDisplayContext.FIXED).rotation(0, 180, -45).scale(k).end()
                .transform(ItemDisplayContext.HEAD).rotation(0, 180, 0).translation(0, 13, 7).scale(k).end()
                .end();
        ItemModelBuilder gui = p.nested().parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", p.modLoc("item/" + name + "_icon"));
        p.getBuilder(name).customLoader(SeparateTransformsModelBuilder::begin).base(base)
                .perspective(ItemDisplayContext.GUI, gui).end();
    }
}
