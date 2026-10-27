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
import org.papiricoh.supernaturalcraft.weapon.melee.AngelicBladeAssets;

public class SNItemModelProvider extends ItemModelProvider {

    public SNItemModelProvider(PackOutput output, ExistingFileHelper existing) {
        super(output, SupernaturalCraft.MODID, existing);
    }

    @Override
    protected void registerModels() {
        // The guild's standard (v0.29): its cloth in the banner's dye (tint 0), its pole over it.
        withExistingParent("guild_standard", mcLoc("item/generated")).texture("layer0", modLoc("item/guild_standard_cloth"))
                .texture("layer1", modLoc("item/guild_standard_pole"));
        flat(AllItems.SALT, AllItems.SULFUR, AllItems.DEMON_BLOOD, AllItems.HELLFIRE_EMBER, AllItems.CHALK,
                AllItems.BLOOD_CHALK, AllItems.HOLY_WATER, AllItems.DEVILS_TRAP,
                AllItems.SPELL_SCROLL, AllItems.SIGIL_PAGE, AllItems.ENOCHIAN_INK, AllItems.HUNTERS_AMULET,
                AllItems.KEY_TO_THE_CAGE, AllItems.CRACKED_KEY);
        handheld(AllItems.RUBYS_KNIFE, AllItems.SILVER_MACHETE, AllItems.EXORCISTS_MACE, AllItems.EMBER_STAFF);
        flat(AllItems.ENOCHIAN_ORB, AllItems.RUNE_BLANK);
        // v0.25: the Leviathans' things.
        flat(AllItems.BORAX_BLEND, AllItems.BORAX, AllItems.BORAX_SPRAY, AllItems.BORAX_SPRAYER, AllItems.TURDUCKEN_SLAMMER,
                AllItems.BIGGERSONS_BURGER, AllItems.RIGHTEOUS_BONE, AllItems.ANOINTED_RIGHTEOUS_BONE, AllItems.FALLEN_BLOOD,
                AllItems.KINGS_BLOOD, AllItems.ALPHA_BLOOD, AllItems.LEVIATHAN_TABLET);
        withExistingParent(AllItems.LEVIATHAN_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        for (var egg : java.util.List.of(AllItems.DEMON_BRUTE_SPAWN_EGG, AllItems.SULFUR_PRIEST_SPAWN_EGG, AllItems.DEMON_FLAYER_SPAWN_EGG, AllItems.DEMON_TORMENTOR_SPAWN_EGG, AllItems.ANGEL_TEMPLAR_SPAWN_EGG, AllItems.ANGEL_HERALD_SPAWN_EGG, AllItems.ANGEL_CHOIRMASTER_SPAWN_EGG, AllItems.ANGEL_DOMINION_SPAWN_EGG, AllItems.GRIGORI_SPAWN_EGG))
            withExistingParent(egg.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(AllItems.DICK_ROMAN_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        AllItems.RUNES.values().forEach(r -> basicItem(r.get()));
        colt(AllItems.THE_COLT, "the_colt");
        // The same gun (its glint tells it apart): it shares the Colt's icon.
        colt(AllItems.ENDLESS_COLT, "the_colt");
        geoWeapon(AllItems.SOUL_SCYTHE, 30);
        geoWeapon(AllItems.HELLFIRE_GREATSWORD, 32);
        geoWeapon(AllItems.CENSER_OF_GRACE, 24);
        geoWeapon(AllItems.FIRST_BLADE, 25);
        geoTome(AllItems.WHISPERING_CODEX);
        grimoire();
        flat(AllItems.COLT_BULLET, AllItems.LUCIFERS_GRACE);
        withExistingParent(AllItems.MORNINGSTAR_TROPHY.getId().getPath(), modLoc("block/morningstar_trophy"));
        withExistingParent(AllItems.ECLIPSE_TROPHY.getId().getPath(), modLoc("block/eclipse_trophy"));
        withExistingParent(AllItems.CHOIR_TROPHY.getId().getPath(), modLoc("block/choir_trophy"));
        geoWeapon(AllItems.PENUMBRA, 30);
        geoDagger(AllItems.ANGEL_BLADE, AngelicBladeAssets.ANGEL_HEIGHT, AngelicBladeAssets.ANGEL_GRIP);
        geoDagger(AllItems.ARCHANGEL_BLADE, AngelicBladeAssets.ARCHANGEL_HEIGHT, AngelicBladeAssets.ARCHANGEL_GRIP);
        geoDagger(AllItems.GABRIEL_BLADE, AngelicBladeAssets.GABRIEL_HEIGHT, AngelicBladeAssets.GABRIEL_GRIP);
        flat(AllItems.VOID_ESSENCE, AllItems.ECLIPSE_SIGHT);
        flat(AllItems.SHATTERED_HYMN, AllItems.CHOIR_SHARD, AllItems.SERAPH_WINGS);
        withExistingParent(AllItems.BLACK_EYED_DEMON_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(AllItems.DEMON_OCCULTIST_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(AllItems.LUCIFER_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        flat(AllItems.BRIMSTONE, AllItems.RACK_HOOK, AllItems.DAMNED_CONTRACT, AllItems.ABYSSAL_SHARD, AllItems.HELLHOUND_FANG,
                AllItems.FALLEN_STAR, AllItems.RING_OF_WAR, AllItems.RING_OF_FAMINE, AllItems.RING_OF_PESTILENCE, AllItems.RING_OF_DEATH);
        withExistingParent(AllItems.HELLHOUND_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(AllItems.ANGEL_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(AllItems.FALLEN_ANGEL_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(AllItems.LUCIFER_UNCAGED_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(AllItems.AZAZEL_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(AllItems.AZAZEL_TROPHY.getId().getPath(), modLoc("block/azazel_trophy"));
        flat(AllItems.AZAZEL_BLOOD, AllItems.LAST_SEAL, AllItems.HOUND_WHISTLE, AllItems.ANGEL_TABLET);
        flat(AllItems.GAUNTLET_OF_ASH, AllItems.SEAL_OF_SUMMONS);
        withExistingParent(AllItems.METATRON_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(AllItems.METATRON_TROPHY.getId().getPath(), modLoc("block/metatron_trophy"));
        withExistingParent(AllItems.LILITH_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(AllItems.LILITH_TROPHY.getId().getPath(), modLoc("block/lilith_trophy"));
        withExistingParent("meat_hook", mcLoc("item/generated")).texture("layer0", modLoc("block/meat_hook"));
        // The Cage's chain and lantern are flat sprites (like vanilla's); its bars show a short run of the 3D model.
        for (String id : new String[]{"cage_chain", "seal_lantern"}) {
            withExistingParent(id, mcLoc("item/generated")).texture("layer0", modLoc("item/" + id));
        }
        withExistingParent("cage_bars", modLoc("block/cage_bars_inventory"));
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
        org.papiricoh.supernaturalcraft.datagen.michael.MichaelAssetData.itemModels(this);
        org.papiricoh.supernaturalcraft.datagen.allegiance.AllegianceAssetData.itemModels(this);
        org.papiricoh.supernaturalcraft.datagen.gabriel.GabrielAssetData.itemModels(this);
        org.papiricoh.supernaturalcraft.datagen.raphael.RaphaelAssetData.itemModels(this);
        org.papiricoh.supernaturalcraft.datagen.legacy.LegacyAssetData.itemModels(this);
        org.papiricoh.supernaturalcraft.datagen.heaven.HeavenAssetData.itemModels(this);
        org.papiricoh.supernaturalcraft.datagen.heaven.HeavenGeoAssetData.itemModels(this);
        org.papiricoh.supernaturalcraft.datagen.balance.BalanceAssetData.itemModels(this);
        // v0.21: the bowl's target layer and its distillates (sprites drawn by tools/artgen; tolerated if not drawn yet).
        pendingFlat(AllItems.TAGLOCK, AllItems.CHRISM, AllItems.HEXED_INK, AllItems.BOUND_BLOOD);
        // v0.21 (P3): the witches' spawn eggs and Rowena's calling card.
        for (var egg : java.util.List.of(AllItems.HEDGE_WITCH_SPAWN_EGG, AllItems.COVEN_WITCH_SPAWN_EGG, AllItems.FAMILIAR_SPAWN_EGG,
                AllItems.HEXED_VILLAGER_SPAWN_EGG)) {
            withExistingParent(egg.getId().getPath(), mcLoc("item/template_spawn_egg"));
        }
        needles(AllItems.ROWENAS_CALLING_CARD);
        // v0.21 (P4): Cain's honey and address, his bust (its block model) and his egg.
        pendingFlat(AllItems.CAINS_HONEY);
        // His address points at his gate like Rowena's card: 32 needle frames on the "angle" property (CainClientEvents).
        needles(AllItems.CAINS_ADDRESS);
        withExistingParent(AllItems.CAIN_TROPHY.getId().getPath(), modLoc("block/" + AllItems.CAIN_TROPHY.getId().getPath()));
        withExistingParent(AllItems.CAIN_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        // v0.21 (P5): Abaddon's Book, her cleaver (GeckoLib, its icon in the inventory), her bust and the eggs.
        pendingFlat(AllItems.BOOK_OF_THE_DAMNED);
        geoWeapon(AllItems.KNIGHTS_CLEAVER, org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonAssets.CLEAVER_HEIGHT);
        withExistingParent(AllItems.ABADDON_TROPHY.getId().getPath(), modLoc("block/" + AllItems.ABADDON_TROPHY.getId().getPath()));
        for (var egg : java.util.List.of(AllItems.ABADDON_SPAWN_EGG, AllItems.ABADDON_KNIGHT_SPAWN_EGG, AllItems.ABADDON_HERALD_SPAWN_EGG)) {
            withExistingParent(egg.getId().getPath(), mcLoc("item/template_spawn_egg"));
        }
        // v0.21 (P6): Rowena's Grimoire (flat), the Coven Staff (GeckoLib, held as a staff, its icon in the inventory), the busts
        // (ART's block models) and the eggs.
        pendingFlat(AllItems.ROWENAS_GRIMOIRE);
        geoStaff(AllItems.COVEN_STAFF, COVEN_STAFF_HEIGHT);
        for (var trophy : java.util.List.of(AllItems.ROWENA_TROPHY, AllItems.GRAND_WITCH_TROPHY)) {
            withExistingParent(trophy.getId().getPath(), modLoc("block/" + trophy.getId().getPath()));
        }
        for (var egg : java.util.List.of(AllItems.ROWENA_SPAWN_EGG, AllItems.GRAND_WITCH_SPAWN_EGG)) {
            withExistingParent(egg.getId().getPath(), mcLoc("item/template_spawn_egg"));
        }
        // Ore block items come from simpleBlockWithItem in the block state provider.
    }

    /** The Coven Staff's height in model pixels (tools/artgen prints it). */
    static final float COVEN_STAFF_HEIGHT = 35.6f;

    /** A GeckoLib staff (v0.21: the Coven Staff; the Stormcaller's transforms): planted upright in the hand, its icon in the GUI. */
    private void geoStaff(DeferredItem<? extends Item> item, float height) {
        float k = Math.min(1f, 26f / height);
        ModelFile entity = new ModelFile.UncheckedModelFile("builtin/entity");
        ItemModelBuilder base = nested().parent(entity).transforms()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(80, -90, 0).translation(0, 2.5f, 2.5f).scale(0.85f * k).end()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(80, 90, 0).translation(0, 2.5f, 2.5f).scale(0.85f * k).end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, -90, -20).translation(0.2f, 2.6f, 1.13f).scale(0.58f * k).end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 90, 20).translation(0.2f, 2.6f, 1.13f).scale(0.58f * k).end()
                .transform(ItemDisplayContext.GROUND).rotation(0, 0, -45).translation(0, 2, 0).scale(0.5f * k).end()
                .transform(ItemDisplayContext.FIXED).rotation(0, 180, -45).scale(k).end()
                .transform(ItemDisplayContext.HEAD).rotation(0, 180, 0).translation(0, 13, 7).scale(k).end()
                .end();
        withIcon(item.getId().getPath(), base);
    }

    /**
     * A compass-like item (v0.21: Rowena's calling card): needle frames {@code item/<id>_00..NN} drawn by tools/artgen, picked by
     * vanilla's {@code angle} as the Letters compass picks its own (frame N/2 points ahead). With no frames yet, a flat item.
     */
    private void needles(DeferredItem<? extends Item> item) {
        String id = item.getId().getPath();
        int n = 0;
        while (existingFileHelper.exists(modLoc(String.format("item/%s_%02d", id, n)), TEXTURE)) n++;
        if (n < 2) {
            pendingFlat(item);
            return;
        }
        for (int i = 0; i < n; i++) {
            getBuilder(String.format("item/%s_%02d", id, i)).parent(new net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile("item/generated"))
                    .texture("layer0", modLoc(String.format("item/%s_%02d", id, i)));
        }
        int ahead = n / 2;
        var model = getBuilder(id).parent(new net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", modLoc(String.format("item/%s_%02d", id, ahead)));
        net.minecraft.resources.ResourceLocation angle = net.minecraft.resources.ResourceLocation.withDefaultNamespace("angle");
        for (int j = 1; j < n; j++) {
            model.override().predicate(angle, (2 * j - 1) / (2f * n))
                    .model(new net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile(modLoc(String.format("item/%s_%02d", id, (ahead + j) % n)))).end();
        }
        model.override().predicate(angle, (2 * n - 1) / (2f * n))
                .model(new net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile(modLoc(String.format("item/%s_%02d", id, ahead)))).end();
    }

    /**
     * Flat items whose sprite tools/artgen may not have written yet: a missing {@code textures/item/<id>.png} is declared as
     * generated so the model is still written (the game shows the missing texture until the art lands).
     */
    @SafeVarargs
    private void pendingFlat(DeferredItem<? extends Item>... items) {
        for (DeferredItem<? extends Item> item : items) {
            net.minecraft.resources.ResourceLocation tex = modLoc("item/" + item.getId().getPath());
            if (!existingFileHelper.exists(tex, TEXTURE)) existingFileHelper.trackGenerated(tex, TEXTURE);
            basicItem(item.get());
        }
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
     * An angelic blade (weapon_models, drawn point up about its middle): held in a reverse grip in the hands, the point out of
     * the bottom of the fist as the show's angels hold it; upright like any 3D weapon elsewhere, its icon in the GUI. In the hand
     * the blade turns by {@code roll} about the item's Z (after the Y quarter turn, model +Y goes to (0, cos, -sin) of the hand's
     * frame), and the translation brings the grip ({@code grip} pixels along the blade) to where vanilla's handheld sword has its
     * handle, so the fist closes on it whatever the blade's length.
     */
    private void geoDagger(DeferredItem<? extends Item> item, float height, float grip) {
        String name = item.getId().getPath();
        float k = Math.min(1f, 20f / height);
        float tpScale = 0.85f * k, fpScale = 0.6f * k;
        float[] tp = daggerHold(130, grip * tpScale, -3.1f, 1.76f);
        float[] fp = daggerHold(140, grip * fpScale, 2.0f, 0f);
        ModelFile entity = new ModelFile.UncheckedModelFile("builtin/entity");
        ItemModelBuilder base = nested().parent(entity).transforms()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(0, -90, 130).translation(0, tp[0], tp[1]).scale(tpScale).end()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(0, 90, -130).translation(0, tp[0], tp[1]).scale(tpScale).end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, -90, 140).translation(1.13f, fp[0], fp[1]).scale(fpScale).end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 90, -140).translation(1.13f, fp[0], fp[1]).scale(fpScale).end()
                .transform(ItemDisplayContext.GROUND).rotation(0, 0, -45).translation(0, 2, 0).scale(0.5f * k).end()
                .transform(ItemDisplayContext.FIXED).rotation(0, 180, -45).scale(k).end()
                .transform(ItemDisplayContext.HEAD).rotation(0, 180, 0).translation(0, 13, 7).scale(k).end()
                .end();
        withIcon(name, base);
    }

    /** The (y, z) translation that puts a grip {@code along} pixels up the blade, rolled by {@code roll}, at (handleY, handleZ). */
    private static float[] daggerHold(float roll, float along, float handleY, float handleZ) {
        double r = Math.toRadians(roll);
        return new float[]{(float) (handleY - along * Math.cos(r)), (float) (handleZ + along * Math.sin(r))};
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
     * The grimoire: a GeckoLib journal built lying flat, pages up, hinge on the model origin (so the open spread is centred
     * on it; the closed book lies on its back half, 4.5 px off). First person: tilted 50 degrees toward the eye and turned
     * so the strap hangs outside, low in the corner clear of the crosshair (vanilla mirrors it for the left hand). Third
     * person: lifted onto the fist, pages up toward the holder. Ground and frame: closed, re-centred.
     */
    private void grimoire() {
        ModelFile entity = new ModelFile.UncheckedModelFile("builtin/entity");
        ItemModelBuilder base = nested().parent(entity).transforms()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(50, 165, 0).translation(-2, 5, -1.5f).scale(0.45f).end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(50, 165, 0).translation(-2, 5, -1.5f).scale(0.45f).end()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(105, 0, 0).translation(0, 1, 2.5f).scale(0.45f).end()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(105, 0, 0).translation(0, 1, 2.5f).scale(0.45f).end()
                .transform(ItemDisplayContext.GROUND).translation(2.25f, 2.5f, 0).scale(0.5f).end()
                .transform(ItemDisplayContext.FIXED).rotation(-90, 0, 0).translation(3.4f, 0, 0).scale(0.75f).end()
                .end();
        withIcon(AllItems.GRIMOIRE.getId().getPath(), base);
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
