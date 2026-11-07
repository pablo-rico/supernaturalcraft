package org.papiricoh.supernaturalcraft.datagen.british;

import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.british.BritishRegistry;
import org.papiricoh.supernaturalcraft.datagen.SNBlockStateProvider;
import org.papiricoh.supernaturalcraft.datagen.SNItemModelProvider;
import org.papiricoh.supernaturalcraft.datagen.slice.SliceData;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * v0.41 datagen hooks: season 12, the British Men of Letters, Ketch, Hess, Dagon and Jack's birth. Textures not yet drawn by
 * {@code tools/artgen} are tracked as generated so {@code runData} runs before the art lands; the busts use the art's own block
 * models ({@code block/<id>}). Sounds point at their own files once they exist, until then at re-pitched vanilla events.
 */
public final class BritishData implements SliceData {

    private static final ExistingFileHelper.ResourceType TEXTURE = new ExistingFileHelper.ResourceType(PackType.CLIENT_RESOURCES, ".png", "textures");

    /** Vanilla stand-ins (event, pitch) until the sounds are encoded. */
    static final Map<String, Object[]> FALLBACK = Map.ofEntries(
            Map.entry("ketch.shot", new Object[]{"entity.arrow.shoot", 1.8f}),
            Map.entry("ketch.knife", new Object[]{"entity.player.attack.sweep", 1.4f}),
            Map.entry("ketch.grenade", new Object[]{"entity.generic.explode", 1.6f}),
            Map.entry("ketch.smoke", new Object[]{"block.fire.extinguish", 0.7f}),
            Map.entry("ketch.hurt", new Object[]{"entity.pillager.hurt", 0.9f}),
            Map.entry("ketch.death", new Object[]{"entity.pillager.death", 0.9f}),
            Map.entry("hess.cast", new Object[]{"entity.evoker.cast_spell", 1.2f}),
            Map.entry("hess.ward", new Object[]{"block.beacon.activate", 1.4f}),
            Map.entry("hess.command", new Object[]{"entity.evoker.prepare_summon", 1.3f}),
            Map.entry("hess.hex", new Object[]{"entity.witch.throw", 1.0f}),
            Map.entry("hess.hurt", new Object[]{"entity.witch.hurt", 1.1f}),
            Map.entry("hess.death", new Object[]{"entity.witch.death", 1.0f}),
            Map.entry("dagon.fling", new Object[]{"entity.breeze.wind_burst", 0.7f}),
            Map.entry("dagon.crush", new Object[]{"block.anvil.land", 0.5f}),
            Map.entry("dagon.fire", new Object[]{"entity.blaze.shoot", 0.6f}),
            Map.entry("dagon.scream", new Object[]{"entity.ghast.scream", 0.8f}),
            Map.entry("dagon.hurt", new Object[]{"entity.vex.hurt", 0.6f}),
            Map.entry("dagon.death", new Object[]{"entity.blaze.death", 0.6f}),
            Map.entry("british_agent.shot", new Object[]{"entity.arrow.shoot", 1.5f}),
            Map.entry("british_agent.hurt", new Object[]{"entity.vindicator.hurt", 1.0f}),
            Map.entry("british_agent.death", new Object[]{"entity.vindicator.death", 1.0f}),
            Map.entry("kelly_kline.labor", new Object[]{"entity.villager.hurt", 1.4f}),
            Map.entry("kelly_kline.glow", new Object[]{"block.beacon.power_select", 1.2f}),
            Map.entry("kelly_kline.cry", new Object[]{"entity.allay.ambient_without_item", 0.7f}));
    static final Set<String> FAR = Set.of("dagon.scream", "dagon.fire", "kelly_kline.glow");

    @Override
    public void lang(BiConsumer<String, String> add) {
        BritishLang.add(add);
    }

    @Override
    public Map<String, String> names() {
        return BritishLang.NAMES;
    }

    @Override
    public Map<String, String> subtitles() {
        return BritishLang.SUBTITLES;
    }

    @Override
    public void sounds(ExistingFileHelper existing, BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add) {
        BritishRegistry.SOUNDS.forEach((event, holder) -> {
            ResourceLocation file = ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, event.replace('.', '/'));
            SoundDefinition def = SoundDefinition.definition().subtitle("subtitles.supernaturalcraft." + event);
            if (existing.exists(file, PackType.CLIENT_RESOURCES, ".ogg", "sounds")) {
                def.with(SoundDefinition.Sound.sound(file, SoundDefinition.SoundType.SOUND).attenuationDistance(FAR.contains(event) ? 64 : 20));
            } else {
                Object[] f = FALLBACK.getOrDefault(event, new Object[]{"entity.vex.ambient", 1.0f});
                def.with(SoundDefinition.Sound.sound(ResourceLocation.withDefaultNamespace((String) f[0]), SoundDefinition.SoundType.EVENT)
                        .pitch((Float) f[1]));
            }
            add.accept(holder, def);
        });
    }

    private static ResourceLocation pending(ExistingFileHelper existing, ResourceLocation tex) {
        if (!existing.exists(tex, TEXTURE)) existing.trackGenerated(tex, TEXTURE);
        return tex;
    }

    private static List<DeferredBlock<?>> trophies() {
        return List.of(BritishRegistry.KETCH_TROPHY, BritishRegistry.HESS_TROPHY, BritishRegistry.DAGON_TROPHY);
    }

    @Override
    public void blockStates(SNBlockStateProvider p, ExistingFileHelper existing) {
        for (DeferredBlock<?> trophy : trophies()) {
            p.horizontalBlock(trophy.get(), new ModelFile.UncheckedModelFile(p.modLoc("block/" + trophy.getId().getPath())));
        }
    }

    @Override
    public void itemModels(SNItemModelProvider p, ExistingFileHelper existing) {
        for (DeferredItem<? extends Item> flat : List.of(BritishRegistry.ENCHANTED_ROUND, BritishRegistry.HEX_GRENADE, BritishRegistry.BRITISH_DOSSIER,
                BritishRegistry.HESS_FOLIO, BritishRegistry.DAGONS_BRAND)) {
            pending(existing, p.modLoc("item/" + flat.getId().getPath()));
            p.basicItem(flat.get());
        }
        p.withExistingParent(BritishRegistry.BRITISH_PISTOL.getId().getPath(), p.mcLoc("item/handheld"))
                .texture("layer0", pending(existing, p.modLoc("item/british_pistol")));
        for (DeferredItem<? extends Item> block : List.of(BritishRegistry.KETCH_TROPHY_ITEM, BritishRegistry.HESS_TROPHY_ITEM, BritishRegistry.DAGON_TROPHY_ITEM)) {
            p.getBuilder(block.getId().getPath()).parent(new ModelFile.UncheckedModelFile(p.modLoc("block/" + block.getId().getPath())));
        }
        for (DeferredItem<? extends Item> egg : List.of(BritishRegistry.KETCH_SPAWN_EGG, BritishRegistry.HESS_SPAWN_EGG, BritishRegistry.DAGON_SPAWN_EGG,
                BritishRegistry.BRITISH_AGENT_SPAWN_EGG)) {
            p.withExistingParent(egg.getId().getPath(), p.mcLoc("item/template_spawn_egg"));
        }
    }

    @Override
    public void entityLoot(BiConsumer<EntityType<?>, LootTable.Builder> add) {
        // The bosses' spoils are given by code, one of each per hunter.
        add.accept(BritishRegistry.KETCH.get(), LootTable.lootTable());
        add.accept(BritishRegistry.HESS.get(), LootTable.lootTable());
        add.accept(BritishRegistry.DAGON.get(), LootTable.lootTable());
        add.accept(BritishRegistry.BRITISH_AGENT.get(), LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(BritishRegistry.ENCHANTED_ROUND.get()).apply(SetItemCountFunction.setCount(UniformGenerator.between(0, 3))))));
    }

    @Override
    public void blockLoot(Consumer<Block> dropSelf, BiConsumer<Block, LootTable.Builder> add) {
        for (DeferredBlock<?> trophy : trophies()) dropSelf.accept(trophy.get());
    }

    @Override
    public void blockTags(BiConsumer<TagKey<Block>, Block> add) {
        for (DeferredBlock<?> trophy : trophies()) add.accept(BlockTags.MINEABLE_WITH_PICKAXE, trophy.get());
    }

    @Override
    public void entityTags(BiConsumer<TagKey<EntityType<?>>, EntityType<?>> add) {
        for (EntityType<?> boss : List.of(BritishRegistry.KETCH.get(), BritishRegistry.HESS.get(), BritishRegistry.DAGON.get())) {
            add.accept(AllTags.Entities.BOSSES, boss);
        }
        // Dagon is a Prince of Hell; the story figures take no Colt round (Lucifer least of all).
        add.accept(AllTags.Entities.DEMONS, BritishRegistry.DAGON.get());
        add.accept(AllTags.Entities.SUPERNATURAL, BritishRegistry.DAGON.get());
        for (EntityType<?> npc : List.of(BritishRegistry.HESS_ENVOY.get(), BritishRegistry.KELLY_KLINE.get(), BritishRegistry.ROURKE_LUCIFER.get())) {
            add.accept(AllTags.Entities.COLT_IMMUNE, npc);
        }
    }

    @Override
    public void recipes(RecipeOutput out) {
        // Enchanted rounds, the British way: a brass case (copper), a lead core, salt, powder.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, BritishRegistry.ENCHANTED_ROUND.get(), 4)
                .requires(Items.COPPER_INGOT).requires(Items.IRON_NUGGET).requires(AllItems.SALT.get()).requires(Items.GUNPOWDER)
                .unlockedBy("has_british_pistol", InventoryChangeTrigger.TriggerInstance.hasItems(BritishRegistry.BRITISH_PISTOL.get()))
                .save(out);
        // A hex grenade: an iron canister, powder, and a hex sealed in ink and salt.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, BritishRegistry.HEX_GRENADE.get(), 2)
                .requires(Items.IRON_INGOT).requires(Items.GUNPOWDER).requires(Items.INK_SAC).requires(AllItems.SALT.get())
                .unlockedBy("has_hex_grenade", InventoryChangeTrigger.TriggerInstance.hasItems(BritishRegistry.HEX_GRENADE.get()))
                .save(out);
    }
}
