package org.papiricoh.supernaturalcraft.datagen.apocalypse;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.papiricoh.supernaturalcraft.datagen.SNBlockStateProvider;
import org.papiricoh.supernaturalcraft.datagen.SNItemModelProvider;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

/**
 * The Apocalypse's models and block states (v0.40, both slices), called from {@code SNBlockStateProvider} and
 * {@code SNItemModelProvider}. Textures not yet drawn by {@code tools/artgen} are tracked as generated, so {@code runData}
 * runs before the art lands; the busts and the hex focus use ART's own block models ({@code block/<id>}).
 */
public final class ApocalypseAssetData {

    private static final ExistingFileHelper.ResourceType TEXTURE = new ExistingFileHelper.ResourceType(PackType.CLIENT_RESOURCES, ".png", "textures");

    private ApocalypseAssetData() {
    }

    private static ResourceLocation pending(ExistingFileHelper existing, ResourceLocation tex) {
        if (!existing.exists(tex, TEXTURE)) existing.trackGenerated(tex, TEXTURE);
        return tex;
    }

    public static void blockStates(SNBlockStateProvider p, ExistingFileHelper existing) {
        for (DeferredBlock<?> trophy : List.of(AllBlocks.ALASTAIR_TROPHY, AllBlocks.URIEL_TROPHY)) {
            p.horizontalBlock(trophy.get(), new ModelFile.UncheckedModelFile(p.modLoc("block/" + trophy.getId().getPath())));
        }
        p.simpleBlock(AllBlocks.SEAL_HEX_FOCUS.get(), new ModelFile.UncheckedModelFile(p.modLoc("block/seal_hex_focus")));
        p.doorBlockWithRenderType(AllBlocks.PANIC_ROOM_DOOR.get(), pending(existing, p.modLoc("block/panic_room_door_bottom")),
                pending(existing, p.modLoc("block/panic_room_door_top")), "cutout");
        for (DeferredBlock<?> decal : List.of(AllBlocks.BLOOD_POOL, AllBlocks.WING_SCORCH)) {
            String id = decal.getId().getPath();
            p.simpleBlock(decal.get(), p.models().withExistingParent(id, p.mcLoc("block/carpet"))
                    .texture("wool", pending(existing, p.modLoc("block/" + id))).renderType("cutout"));
        }
    }

    public static void itemModels(SNItemModelProvider p, ExistingFileHelper existing) {
        for (DeferredItem<? extends Item> flat : List.of(AllItems.CAGE_SHACKLE, AllItems.RUBYS_FLASK, AllItems.PANIC_ROOM_DOOR,
                AllItems.WITNESS_SHROUD, AllItems.SCORCHED_FEATHER, AllItems.PLAGUE_ASH, AllItems.SACRIFICIAL_ASH, AllItems.SEAL_COMPASS)) {
            pending(existing, p.modLoc("item/" + flat.getId().getPath()));
            p.basicItem(flat.get());
        }
        p.withExistingParent(AllItems.ALASTAIR_SCALPEL.getId().getPath(), p.mcLoc("item/handheld"))
                .texture("layer0", pending(existing, p.modLoc("item/alastair_scalpel")));
        for (DeferredItem<? extends Item> block : List.of(AllItems.ALASTAIR_TROPHY, AllItems.URIEL_TROPHY, AllItems.SEAL_HEX_FOCUS)) {
            p.getBuilder(block.getId().getPath()).parent(new ModelFile.UncheckedModelFile(p.modLoc("block/" + block.getId().getPath())));
        }
        for (DeferredItem<? extends Item> egg : List.of(AllItems.ALASTAIR_SPAWN_EGG, AllItems.RUBY_SPAWN_EGG, AllItems.URIEL_SPAWN_EGG,
                AllItems.REBEL_ANGEL_SPAWN_EGG, AllItems.WITNESS_SPAWN_EGG, AllItems.SEAL_REAPER_SPAWN_EGG)) {
            p.withExistingParent(egg.getId().getPath(), p.mcLoc("item/template_spawn_egg"));
        }
    }
}
