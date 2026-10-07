package org.papiricoh.supernaturalcraft.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.concurrent.CompletableFuture;

public class SNTagsProviders {

    public static class Blocks extends BlockTagsProvider {

        public Blocks(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existing) {
            super(output, lookup, SupernaturalCraft.MODID, existing);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(AllBlocks.ROCK_SALT_ORE.get(), AllBlocks.DEEPSLATE_ROCK_SALT_ORE.get(),
                    AllBlocks.NETHER_SULFUR_ORE.get());
            tag(BlockTags.NEEDS_STONE_TOOL).add(AllBlocks.ROCK_SALT_ORE.get(), AllBlocks.DEEPSLATE_ROCK_SALT_ORE.get(),
                    AllBlocks.NETHER_SULFUR_ORE.get());
            tag(Tags.Blocks.ORES).add(AllBlocks.ROCK_SALT_ORE.get(), AllBlocks.DEEPSLATE_ROCK_SALT_ORE.get(),
                    AllBlocks.NETHER_SULFUR_ORE.get());
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(AllBlocks.HELLFORGE.get(), AllBlocks.ECLIPSE_TROPHY.get(), AllBlocks.CHOIR_TROPHY.get());
            tag(BlockTags.NEEDS_IRON_TOOL).add(AllBlocks.HELLFORGE.get());
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(AllBlocks.RITUAL_ALTAR.get(), AllBlocks.HELLFIRE_CRACK.get(),
                    AllBlocks.CAGE_FROST.get(), AllBlocks.CAGE_ICE.get(), AllBlocks.SERAPHIC_PILLAR.get());
            tag(AllTags.Blocks.ARENA_IMMUNE).add(AllBlocks.CHOIR_ALTAR.get())
                    .add(AllBlocks.CHOIR_BELLS.stream().map(net.neoforged.neoforge.registries.DeferredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new));
            tag(AllTags.Blocks.ARENA_IMMUNE).add(AllBlocks.RITUAL_ALTAR.get()).addTag(BlockTags.WITHER_IMMUNE)
                    .addTag(Tags.Blocks.CHESTS).addTag(Tags.Blocks.BARRELS).addTag(BlockTags.SHULKER_BOXES).addTag(BlockTags.BEDS);
            tag(AllTags.Blocks.CHALK_LINES).add(AllBlocks.CHALK_LINE.get(), AllBlocks.BLOOD_CHALK_LINE.get());
            tag(AllTags.Blocks.SNUFFABLE).add(net.minecraft.world.level.block.Blocks.TORCH, net.minecraft.world.level.block.Blocks.WALL_TORCH,
                    net.minecraft.world.level.block.Blocks.SOUL_TORCH, net.minecraft.world.level.block.Blocks.SOUL_WALL_TORCH,
                    net.minecraft.world.level.block.Blocks.REDSTONE_TORCH, net.minecraft.world.level.block.Blocks.REDSTONE_WALL_TORCH,
                    net.minecraft.world.level.block.Blocks.LANTERN, net.minecraft.world.level.block.Blocks.SOUL_LANTERN,
                    net.minecraft.world.level.block.Blocks.JACK_O_LANTERN, net.minecraft.world.level.block.Blocks.GLOWSTONE,
                    net.minecraft.world.level.block.Blocks.SEA_LANTERN, net.minecraft.world.level.block.Blocks.SHROOMLIGHT,
                    net.minecraft.world.level.block.Blocks.END_ROD, net.minecraft.world.level.block.Blocks.LIGHT);
        }
    }

    public static class Items extends ItemTagsProvider {

        public Items(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                     CompletableFuture<TagsProvider.TagLookup<Block>> blockTags, ExistingFileHelper existing) {
            super(output, lookup, blockTags, SupernaturalCraft.MODID, existing);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(AllTags.Items.SALT).add(AllItems.SALT.get());
            tag(AllTags.Items.HELD_LIGHT_SOURCES).add(net.minecraft.world.item.Items.TORCH, net.minecraft.world.item.Items.SOUL_TORCH,
                    net.minecraft.world.item.Items.LANTERN, net.minecraft.world.item.Items.SOUL_LANTERN, net.minecraft.world.item.Items.GLOWSTONE,
                    net.minecraft.world.item.Items.SHROOMLIGHT, net.minecraft.world.item.Items.SEA_LANTERN, net.minecraft.world.item.Items.JACK_O_LANTERN,
                    net.minecraft.world.item.Items.END_ROD, net.minecraft.world.item.Items.GLOW_BERRIES, AllItems.CENSER_OF_GRACE.get());
            tag(AllTags.Items.DEMON_BANE).add(AllItems.RUBYS_KNIFE.get(), AllItems.ANGEL_BLADE.get(), AllItems.ARCHANGEL_BLADE.get(),
                    AllItems.EXORCISTS_MACE.get());
            tag(AllTags.Items.HOLY_WEAPONS).add(AllItems.ANGEL_BLADE.get(), AllItems.ARCHANGEL_BLADE.get(), AllItems.EXORCISTS_MACE.get());
            tag(ItemTags.SWORDS).add(AllItems.RUBYS_KNIFE.get(), AllItems.ANGEL_BLADE.get(), AllItems.ARCHANGEL_BLADE.get(),
                    AllItems.SILVER_MACHETE.get(), AllItems.SOUL_SCYTHE.get(), AllItems.HELLFIRE_GREATSWORD.get(), AllItems.FIRST_BLADE.get(),
                    AllItems.PENUMBRA.get());
            tag(ItemTags.MACE_ENCHANTABLE).add(AllItems.EXORCISTS_MACE.get());
            tag(ItemTags.DURABILITY_ENCHANTABLE).add(AllItems.EXORCISTS_MACE.get());
            tag(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", "back"))).add(AllItems.SERAPH_WINGS.get());
            tag(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", "necklace")))
                    .add(AllItems.HUNTERS_AMULET.get());
            tag(Tags.Items.ORES).add(AllItems.ROCK_SALT_ORE.get(), AllItems.DEEPSLATE_ROCK_SALT_ORE.get(),
                    AllItems.NETHER_SULFUR_ORE.get());
        }
    }

    public static class Entities extends EntityTypeTagsProvider {

        public Entities(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existing) {
            super(output, lookup, SupernaturalCraft.MODID, existing);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(AllTags.Entities.DEMONS).add(AllEntities.BLACK_EYED_DEMON.get(), AllEntities.DEMON_OCCULTIST.get());
            tag(AllTags.Entities.SUPERNATURAL).addTag(AllTags.Entities.DEMONS)
                    .add(AllEntities.LUCIFER.get(), AllEntities.LUCIFER_ILLUSION.get(), AllEntities.AMARA.get());
            tag(AllTags.Entities.CAGE_DWELLERS).addTag(AllTags.Entities.DEMONS)
                    .add(AllEntities.LUCIFER.get(), AllEntities.LUCIFER_ILLUSION.get());
            tag(AllTags.Entities.DARKNESS).add(AllEntities.AMARA.get(), AllEntities.AMARA_SHADE.get());
            tag(AllTags.Entities.BOSSES).add(AllEntities.LUCIFER.get(), AllEntities.AMARA.get(), AllEntities.BROKEN_CHORUS.get())
                    .addOptionalTag(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES);
            tag(AllTags.Entities.COLT_EXECUTES).addTag(AllTags.Entities.DEMONS)
                    .add(AllEntities.AMARA_SHADE.get(), AllEntities.CHOIR_ECHO.get(), AllEntities.LUCIFER_ILLUSION.get());
        }
    }

    public static class Structures extends TagsProvider<net.minecraft.world.level.levelgen.structure.Structure> {

        public Structures(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existing) {
            super(output, Registries.STRUCTURE, lookup, SupernaturalCraft.MODID, existing);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(AllTags.Structures.HYMNAL_SPIRES).add(SNStructures.HYMNAL_SPIRE);
        }
    }

    public static class DamageTypes extends TagsProvider<DamageType> {

        public DamageTypes(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existing) {
            super(output, Registries.DAMAGE_TYPE, lookup, SupernaturalCraft.MODID, existing);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(AllTags.DamageTypes.HOLY).add(AllDamageTypes.SMITE, AllDamageTypes.HOLY_WATER, AllDamageTypes.GRACE, AllDamageTypes.COLT);
            tag(AllTags.DamageTypes.EXACT_BOSS_DAMAGE).add(AllDamageTypes.COLT);
            tag(DamageTypeTags.BYPASSES_SHIELD).add(AllDamageTypes.COLT);
            tag(AllTags.DamageTypes.LUCIFER_IMMUNE).add(net.minecraft.world.damagesource.DamageTypes.IN_FIRE,
                    net.minecraft.world.damagesource.DamageTypes.ON_FIRE, net.minecraft.world.damagesource.DamageTypes.LAVA,
                    net.minecraft.world.damagesource.DamageTypes.HOT_FLOOR, net.minecraft.world.damagesource.DamageTypes.FALL,
                    net.minecraft.world.damagesource.DamageTypes.DROWN, net.minecraft.world.damagesource.DamageTypes.WITHER,
                    net.minecraft.world.damagesource.DamageTypes.IN_WALL, net.minecraft.world.damagesource.DamageTypes.CRAMMING,
                    net.minecraft.world.damagesource.DamageTypes.FREEZE, AllDamageTypes.HELLFIRE, AllDamageTypes.ARENA_BARRIER);
            tag(DamageTypeTags.BYPASSES_ARMOR).add(AllDamageTypes.SMITE, AllDamageTypes.HOLY_WATER, AllDamageTypes.GRACE,
                    AllDamageTypes.ARENA_BARRIER, AllDamageTypes.HYMN, AllDamageTypes.COLT);
            tag(DamageTypeTags.IS_FIRE).add(AllDamageTypes.HELLFIRE);
            tag(DamageTypeTags.NO_KNOCKBACK).add(AllDamageTypes.ARENA_BARRIER, AllDamageTypes.HOLY_WATER);
            tag(DamageTypeTags.WITCH_RESISTANT_TO).add(AllDamageTypes.SPELL);
        }
    }
}
