package org.papiricoh.supernaturalcraft.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.SeparateTransformsModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredItem;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllItems;

public class SNItemModelProvider extends ItemModelProvider {

    public SNItemModelProvider(PackOutput output, ExistingFileHelper existing) {
        super(output, SupernaturalCraft.MODID, existing);
    }

    @Override
    protected void registerModels() {
        flat(AllItems.SALT, AllItems.SULFUR, AllItems.DEMON_BLOOD, AllItems.HELLFIRE_EMBER, AllItems.CHALK,
                AllItems.BLOOD_CHALK, AllItems.HOLY_WATER, AllItems.DEVILS_TRAP,
                AllItems.GRIMOIRE, AllItems.SPELL_SCROLL, AllItems.SIGIL_PAGE, AllItems.ENOCHIAN_INK, AllItems.HUNTERS_AMULET,
                AllItems.KEY_TO_THE_CAGE, AllItems.CRACKED_KEY);
        handheld(AllItems.RUBYS_KNIFE, AllItems.ANGEL_BLADE, AllItems.ARCHANGEL_BLADE,
                AllItems.SILVER_MACHETE, AllItems.EXORCISTS_MACE, AllItems.EMBER_STAFF);
        flat(AllItems.ENOCHIAN_ORB, AllItems.RUNE_BLANK);
        AllItems.RUNES.values().forEach(r -> basicItem(r.get()));
        colt(AllItems.THE_COLT, "the_colt");
        // The same gun (its glint tells it apart): it shares the Colt's icon.
        colt(AllItems.ENDLESS_COLT, "the_colt");
        geoWeapon(AllItems.SOUL_SCYTHE, 30);
        geoWeapon(AllItems.HELLFIRE_GREATSWORD, 32);
        geoWeapon(AllItems.CENSER_OF_GRACE, 24);
        geoWeapon(AllItems.FIRST_BLADE, 25);
        geoTome(AllItems.WHISPERING_CODEX);
        flat(AllItems.COLT_BULLET, AllItems.LUCIFERS_GRACE);
        withExistingParent(AllItems.MORNINGSTAR_TROPHY.getId().getPath(), modLoc("block/morningstar_trophy"));
        withExistingParent(AllItems.ECLIPSE_TROPHY.getId().getPath(), modLoc("block/eclipse_trophy"));
        withExistingParent(AllItems.CHOIR_TROPHY.getId().getPath(), modLoc("block/choir_trophy"));
        geoWeapon(AllItems.PENUMBRA, 30);
        flat(AllItems.VOID_ESSENCE, AllItems.ECLIPSE_SIGHT);
        flat(AllItems.SHATTERED_HYMN, AllItems.CHOIR_SHARD, AllItems.SERAPH_WINGS);
        withExistingParent(AllItems.BLACK_EYED_DEMON_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(AllItems.DEMON_OCCULTIST_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(AllItems.LUCIFER_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        flat(AllItems.BRIMSTONE, AllItems.RACK_HOOK, AllItems.DAMNED_CONTRACT, AllItems.ABYSSAL_SHARD, AllItems.HELLHOUND_FANG,
                AllItems.FALLEN_STAR, AllItems.RING_OF_WAR, AllItems.RING_OF_FAMINE, AllItems.RING_OF_PESTILENCE, AllItems.RING_OF_DEATH);
        withExistingParent(AllItems.HELLHOUND_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(AllItems.LUCIFER_UNCAGED_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(AllItems.AZAZEL_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(AllItems.AZAZEL_TROPHY.getId().getPath(), modLoc("block/azazel_trophy"));
        flat(AllItems.AZAZEL_BLOOD, AllItems.LAST_SEAL, AllItems.HOUND_WHISTLE, AllItems.ANGEL_TABLET);
        withExistingParent(AllItems.METATRON_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(AllItems.METATRON_TROPHY.getId().getPath(), modLoc("block/metatron_trophy"));
        withExistingParent(AllItems.LILITH_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(AllItems.LILITH_TROPHY.getId().getPath(), modLoc("block/lilith_trophy"));
        for (String id : new String[]{"meat_hook", "cage_bars", "cage_chain"}) {
            withExistingParent(id, mcLoc("item/generated")).texture("layer0", modLoc("block/" + id));
        }
        // v0.8
        spellBowl();
        flat(AllItems.BLOOD_VIAL, AllItems.PET_COLLAR, AllItems.SPELL_PAGE, AllItems.CROSSROADS_CONTRACT, AllItems.ECTOPLASM,
                AllItems.GRAVE_DIRT, AllItems.PROTECTION_BAG);
        withExistingParent(AllItems.CURSE_BAG.getId().getPath(), modLoc("block/curse_bag"));
        withExistingParent(AllItems.GRAVE_HEADSTONE.getId().getPath(), modLoc("block/grave_headstone"));
        withExistingParent(AllItems.GRAVE_BONES.getId().getPath(), modLoc("block/grave_bones"));
        withExistingParent(AllItems.GHOST_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(AllItems.CROSSROADS_DEMON_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        org.papiricoh.supernaturalcraft.datagen.chuck.ChuckAssetData.itemModels(this);
        org.papiricoh.supernaturalcraft.datagen.horsemen.HorsemenAssetData.itemModels(this);
        // Ore block items come from simpleBlockWithItem in the block state provider.
    }

    /**
     * A GeckoLib weapon: the 3D model everywhere but the inventory, where its rendered icon is used.
     * Transforms are vanilla's handheld ones turned 45° (the model's blade points up; a sprite's runs
     * corner to corner) and scaled so a model {@code height} pixels tall reads like a sword.
     */
    private void geoWeapon(DeferredItem<? extends Item> item, float height) {
        String name = item.getId().getPath();
        float k = Math.min(1f, 20f / height);
        ModelFile entity = new ModelFile.UncheckedModelFile("builtin/entity");
        ItemModelBuilder base = nested().parent(entity).transforms()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(0, -90, 10).translation(0, 4, 0.5f).scale(0.85f * k).end()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(0, 90, -100).translation(0, 4, 0.5f).scale(0.85f * k).end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, -90, -20).translation(1.13f, 3.2f, 1.13f).scale(0.68f * k).end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 90, -70).translation(1.13f, 3.2f, 1.13f).scale(0.68f * k).end()
                .transform(ItemDisplayContext.GROUND).rotation(0, 0, -45).translation(0, 2, 0).scale(0.5f * k).end()
                .transform(ItemDisplayContext.FIXED).rotation(0, 180, -45).scale(k).end()
                .transform(ItemDisplayContext.HEAD).rotation(0, 180, 0).translation(0, 13, 7).scale(k).end()
                .end();
        withIcon(name, base);
    }

    /**
     * The Colt: built at four units to the pixel with its origin in the gun hand's grip, barrel
     * along -Z, so the transforms here are a quarter scale about the hand. In third person a
     * half-turn about the barrel puts it right way up in a raised hand (and muzzle-down in a
     * lowered one); in first person ColtItemExtensions has already put the hand in place.
     */
    private void colt(DeferredItem<? extends Item> item, String icon) {
        ModelFile entity = new ModelFile.UncheckedModelFile("builtin/entity");
        ItemModelBuilder base = nested().parent(entity).transforms()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(0, 180, 180).translation(0, 0.5f, 0).scale(0.26f).end()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(0, 180, 180).translation(0, 0.5f, 0).scale(0.26f).end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, 0, 0).translation(0, 0, 0).scale(0.16f).end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 0, 0).translation(0, 0, 0).scale(0.16f).end()
                .transform(ItemDisplayContext.GROUND).rotation(0, 0, 90).translation(0, 2, 0).scale(0.15f).end()
                .transform(ItemDisplayContext.FIXED).rotation(0, 90, 0).scale(0.3f).end()
                .transform(ItemDisplayContext.HEAD).rotation(0, 90, 0).translation(0, 10, 0).scale(0.3f).end()
                .end();
        withIcon(item.getId().getPath(), icon, base);
    }

    /**
     * A GeckoLib book: held upright and smaller than a weapon, its eyed cover (the model's north face)
     * turned to the viewer. GeckoLib mirrors X, so a first-person turn of 200° faces the camera.
     */
    private void geoTome(DeferredItem<? extends Item> item) {
        ModelFile entity = new ModelFile.UncheckedModelFile("builtin/entity");
        ItemModelBuilder base = nested().parent(entity).transforms()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).translation(0, 3, 1).scale(0.55f).end()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND).translation(0, 3, 1).scale(0.55f).end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, 200, 0).translation(-1.5f, 5.5f, 0).scale(0.5f).end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 160, 0).translation(-1.5f, 5.5f, 0).scale(0.5f).end()
                .transform(ItemDisplayContext.GROUND).translation(0, 2, 0).scale(0.5f).end()
                .transform(ItemDisplayContext.FIXED).scale(0.9f).end()
                .end();
        withIcon(item.getId().getPath(), base);
    }

    /**
     * The spell bowl: drawn by its BEWLR (the bowl model plus what is in it) in every context, the
     * inventory included. Transforms owned by the bowl code (tuned in SN_PREVIEW=bowl).
     */
    private void spellBowl() {
        ModelFile entity = new ModelFile.UncheckedModelFile("builtin/entity");
        // Third person: the item hangs from the main hand (ItemInHandLayer frame: +x outward, +y ahead, +z up).
        // Tipped 47 degrees to stay level on arms lifted 43 degrees (BowlArmPoses.LIFT), shifted in toward the
        // other hand so the bowl sits between both; mirrored for the left hand by the transform itself.
        getBuilder(AllItems.SPELL_BOWL.getId().getPath()).parent(entity).guiLight(net.minecraft.client.renderer.block.model.BlockModel.GuiLight.SIDE).transforms()
                .transform(ItemDisplayContext.GUI).rotation(35, 225, 0).translation(0, 3.5f, 0).scale(0.85f).end()
                .transform(ItemDisplayContext.GROUND).translation(0, 3, 0).scale(0.5f).end()
                .transform(ItemDisplayContext.FIXED).translation(0, 3, 0).scale(0.75f).end()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(47, 0, 0).translation(-5.5f, 0, 2).scale(0.45f).end()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(47, 0, 0).translation(-5.5f, 0, 2).scale(0.45f).end()
                // First person: placed by SpellBowlItemExtensions.applyForgeHandTransform.
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).scale(0.6f).end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND).scale(0.6f).end()
                .end();
    }

    private void withIcon(String name, ItemModelBuilder base) {
        withIcon(name, name, base);
    }

    private void withIcon(String name, String iconName, ItemModelBuilder base) {
        ItemModelBuilder icon = nested().parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", modLoc("item/" + iconName + "_icon"));
        getBuilder(name).customLoader(SeparateTransformsModelBuilder::begin).base(base)
                .perspective(ItemDisplayContext.GUI, icon).end();
    }

    @SafeVarargs
    private void flat(DeferredItem<? extends Item>... items) {
        for (DeferredItem<? extends Item> item : items) {
            basicItem(item.get());
        }
    }

    @SafeVarargs
    private void handheld(DeferredItem<? extends Item>... items) {
        for (DeferredItem<? extends Item> item : items) {
            withExistingParent(item.getId().getPath(), mcLoc("item/handheld"))
                    .texture("layer0", modLoc("item/" + item.getId().getPath()));
        }
    }
}
