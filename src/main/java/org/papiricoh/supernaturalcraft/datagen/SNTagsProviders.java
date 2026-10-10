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
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(AllBlocks.HELLFORGE.get(), AllBlocks.ECLIPSE_TROPHY.get(), AllBlocks.CHOIR_TROPHY.get(),
                    AllBlocks.AZAZEL_TROPHY.get(), AllBlocks.LILITH_TROPHY.get(), AllBlocks.METATRON_TROPHY.get(),
                    AllBlocks.WAR_TROPHY.get(), AllBlocks.FAMINE_TROPHY.get(), AllBlocks.PESTILENCE_TROPHY.get(), AllBlocks.DEATH_TROPHY.get(),
                    AllBlocks.MICHAEL_TROPHY.get(), AllBlocks.GABRIEL_TROPHY.get(),
                    AllBlocks.RAPHAEL_TROPHY.get(), AllBlocks.BUNKER_DOOR.get(), AllBlocks.MEN_OF_LETTERS_EMBLEM.get(),
                    AllBlocks.NAOMI_TROPHY.get(), AllBlocks.ZACHARIAH_TROPHY.get(), AllBlocks.CLOUD_STONE.get(), AllBlocks.CLOUD_BRICKS.get(),
                    AllBlocks.FILING_CABINET.get());
            tag(BlockTags.MINEABLE_WITH_SHOVEL).add(AllBlocks.CROSSROADS_SOIL.get());
            tag(BlockTags.MINEABLE_WITH_AXE).add(AllBlocks.RESEARCH_DESK.get(), AllBlocks.MAP_TABLE.get(), AllBlocks.ARCHIVE_SHELF.get());
            tag(BlockTags.NEEDS_IRON_TOOL).add(AllBlocks.HELLFORGE.get());
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(AllBlocks.RITUAL_ALTAR.get(), AllBlocks.HELLFIRE_CRACK.get(),
                    AllBlocks.CAGE_FROST.get(), AllBlocks.CAGE_ICE.get(), AllBlocks.SERAPHIC_PILLAR.get());
            tag(AllTags.Blocks.ARENA_IMMUNE).add(AllBlocks.CHOIR_ALTAR.get())
                    .add(AllBlocks.CHOIR_BELLS.stream().map(net.neoforged.neoforge.registries.DeferredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new));
            tag(AllTags.Blocks.ARENA_IMMUNE).add(AllBlocks.RITUAL_ALTAR.get()).addTag(BlockTags.WITHER_IMMUNE)
                    .addTag(Tags.Blocks.CHESTS).addTag(Tags.Blocks.BARRELS).addTag(BlockTags.SHULKER_BOXES).addTag(BlockTags.BEDS);
            tag(AllTags.Blocks.CHALK_LINES).add(AllBlocks.CHALK_LINE.get(), AllBlocks.BLOOD_CHALK_LINE.get());
            // v0.13: holy ground reaches a few blocks round these (SNConfig consecratedRadius).
            tag(AllTags.Blocks.CONSECRATED).add(AllBlocks.RITUAL_ALTAR.get(), AllBlocks.CHOIR_ALTAR.get(), AllBlocks.ENOCHIAN_PILLAR.get());
            // v0.8
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(AllBlocks.SPELL_BOWL.get(), AllBlocks.GRAVE_HEADSTONE.get());
            tag(BlockTags.MINEABLE_WITH_SHOVEL).add(AllBlocks.GRAVE_SOIL.get());
            // Hell
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(AllBlocks.HELLSTONE.get(), AllBlocks.HELLSTONE_BRICKS.get(), AllBlocks.RACK_STONE.get(),
                    AllBlocks.MEAT_HOOK.get(), AllBlocks.BRIMSTONE_ORE.get(), AllBlocks.HELLFIRE_VENT.get(), AllBlocks.CORRIDOR_STONE.get(),
                    AllBlocks.CORRIDOR_BRICKS.get(), AllBlocks.ABYSSAL_STONE.get(), AllBlocks.ABYSSAL_SHARD_ORE.get());
            tag(BlockTags.MINEABLE_WITH_SHOVEL).add(AllBlocks.ASH_BLOCK.get(), AllBlocks.CONGEALED_BLOOD.get());
            tag(BlockTags.NEEDS_IRON_TOOL).add(AllBlocks.BRIMSTONE_ORE.get());
            tag(BlockTags.NEEDS_DIAMOND_TOOL).add(AllBlocks.ABYSSAL_SHARD_ORE.get());
            tag(Tags.Blocks.ORES).add(AllBlocks.BRIMSTONE_ORE.get(), AllBlocks.ABYSSAL_SHARD_ORE.get());
            tag(BlockTags.INFINIBURN_NETHER).add(AllBlocks.HELLSTONE.get(), AllBlocks.RACK_STONE.get());
            // The Cage: nothing breaks it, nothing moves it. The island's floor is left to the fight (and restored after).
            net.minecraft.world.level.block.Block[] cage = {AllBlocks.CAGE_BARS.get(), AllBlocks.CAGE_FRAME.get(), AllBlocks.CAGE_SEAL.get(),
                    AllBlocks.CAGE_CHAIN.get(), AllBlocks.ABYSSAL_BEDROCK.get(), AllBlocks.ENOCHIAN_PILLAR.get(), AllBlocks.HELLFIRE_BRAZIER.get(),
                    AllBlocks.CAGE_RITUAL_STONE.get(), AllBlocks.HELL_RIFT.get()};
            tag(BlockTags.WITHER_IMMUNE).add(cage).add(AllBlocks.ABYSSAL_FLAGSTONE.get(), AllBlocks.GRAVE_BONES.get());
            tag(BlockTags.DRAGON_IMMUNE).add(cage).add(AllBlocks.ABYSSAL_FLAGSTONE.get(), AllBlocks.GRAVE_BONES.get());
            tag(AllTags.Blocks.ARENA_IMMUNE).add(cage);
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
            tag(AllTags.Items.HOLY_WEAPONS).add(AllItems.ANGEL_BLADE.get(), AllItems.ARCHANGEL_BLADE.get(), AllItems.EXORCISTS_MACE.get(),
                    AllItems.MICHAEL_LANCE.get(), AllItems.BORROWED_LANCE.get(), AllItems.GABRIEL_BLADE.get());
            tag(AllTags.Items.DEMON_BANE).add(AllItems.MICHAEL_LANCE.get(), AllItems.GABRIEL_BLADE.get());
            // v0.18: the spoils of Heaven's offices.
            tag(AllTags.Items.HOLY_WEAPONS).add(AllItems.NAOMIS_DRILL.get(), AllItems.ZACHARIAHS_BLADE.get());
            tag(AllTags.Items.DEMON_BANE).add(AllItems.ZACHARIAHS_BLADE.get());
            // The General's armour (v0.12): enchantable and trimmable like any other.
            tag(ItemTags.HEAD_ARMOR_ENCHANTABLE).add(AllItems.GENERAL_HELMET.get());
            tag(ItemTags.CHEST_ARMOR_ENCHANTABLE).add(AllItems.GENERAL_CHESTPLATE.get());
            tag(ItemTags.LEG_ARMOR_ENCHANTABLE).add(AllItems.GENERAL_LEGGINGS.get());
            tag(ItemTags.FOOT_ARMOR_ENCHANTABLE).add(AllItems.GENERAL_BOOTS.get());
            tag(ItemTags.TRIDENT_ENCHANTABLE).add(AllItems.MICHAEL_LANCE.get());
            tag(ItemTags.DURABILITY_ENCHANTABLE).add(AllItems.MICHAEL_LANCE.get());
            tag(ItemTags.SWORDS).add(AllItems.RUBYS_KNIFE.get(), AllItems.ANGEL_BLADE.get(), AllItems.ARCHANGEL_BLADE.get(),
                    AllItems.SILVER_MACHETE.get(), AllItems.SOUL_SCYTHE.get(), AllItems.HELLFIRE_GREATSWORD.get(), AllItems.FIRST_BLADE.get(),
                    AllItems.PENUMBRA.get(), AllItems.GABRIEL_BLADE.get(), AllItems.NAOMIS_DRILL.get(), AllItems.ZACHARIAHS_BLADE.get());
            tag(ItemTags.MACE_ENCHANTABLE).add(AllItems.EXORCISTS_MACE.get());
            tag(ItemTags.DURABILITY_ENCHANTABLE).add(AllItems.EXORCISTS_MACE.get());
            tag(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", "back"))).add(AllItems.SERAPH_WINGS.get());
            tag(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", "necklace")))
                    .add(AllItems.HUNTERS_AMULET.get(), AllItems.SAMS_AMULET.get());
            // v0.17: a cursed artifact works worn as a charm too.
            tag(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", "charm"))).add(AllItems.CURSED_ARTIFACT.get(),
                    AllItems.AQUARIAN_STAR.get(), AllItems.NAOMIS_DIADEM.get(), AllItems.HEAVENS_SEAL.get());
            tag(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", "ring"))).add(AllItems.MEN_OF_LETTERS_RING.get());
            tag(ItemTags.HEAD_ARMOR_ENCHANTABLE).add(AllItems.SPELLWRIGHTS_SPECTACLES.get());
            tag(AllTags.Items.GHOST_BANE).add(net.minecraft.world.item.Items.IRON_SWORD, net.minecraft.world.item.Items.IRON_AXE,
                    net.minecraft.world.item.Items.IRON_SHOVEL, net.minecraft.world.item.Items.IRON_PICKAXE, net.minecraft.world.item.Items.IRON_HOE,
                    AllItems.SILVER_MACHETE.get(), AllItems.RUBYS_KNIFE.get());
            tag(AllTags.Items.BOWL_REJECTS).add(AllItems.SPELL_BOWL.get());
            // v0.17: what takes a vampire's head, and what silver there is.
            tag(AllTags.Items.BEHEADING).addTag(ItemTags.SWORDS).addTag(ItemTags.AXES).add(AllItems.SILVER_MACHETE.get());
            tag(AllTags.Items.SILVER).add(AllItems.SILVER_MACHETE.get());
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
            tag(AllTags.Entities.DEMONS).add(AllEntities.BLACK_EYED_DEMON.get(), AllEntities.DEMON_OCCULTIST.get(), AllEntities.HELLHOUND.get(),
                    AllEntities.AZAZEL.get(), AllEntities.LILITH.get(), AllEntities.CROSSROADS_DEMON.get());
            tag(AllTags.Entities.SPIRITS).add(AllEntities.GHOST.get());
            tag(AllTags.Entities.ANGELS).add(AllEntities.CHOIR_ECHO.get(), AllEntities.HOST_ANGEL.get(), AllEntities.HOST_ALLY.get(),
                    AllEntities.MESSENGER.get(), AllEntities.GARRISON_ANGEL.get(), AllEntities.HEAVEN_GUARD.get(), AllEntities.CLERK_ANGEL.get());
            tag(AllTags.Entities.SUPERNATURAL).addTag(AllTags.Entities.DEMONS).addTag(AllTags.Entities.SPIRITS)
                    .add(AllEntities.LUCIFER.get(), AllEntities.LUCIFER_ILLUSION.get(), AllEntities.AMARA.get(), AllEntities.LUCIFER_UNCAGED.get(),
                            AllEntities.MICHAEL.get(), AllEntities.GABRIEL.get(), AllEntities.GABRIEL_DOUBLE.get(), AllEntities.RAPHAEL.get(),
                            AllEntities.VAMPIRE.get(), AllEntities.WEREWOLF.get(), AllEntities.SHAPESHIFTER.get(),
                            AllEntities.NAOMI.get(), AllEntities.ZACHARIAH.get());
            tag(AllTags.Entities.CAGE_DWELLERS).addTag(AllTags.Entities.DEMONS)
                    .add(AllEntities.LUCIFER.get(), AllEntities.LUCIFER_ILLUSION.get(), AllEntities.LUCIFER_UNCAGED.get(), AllEntities.CAGED_LUCIFER.get());
            tag(AllTags.Entities.DARKNESS).add(AllEntities.AMARA.get(), AllEntities.AMARA_SHADE.get());
            tag(AllTags.Entities.BOSSES).add(AllEntities.LUCIFER.get(), AllEntities.AMARA.get(), AllEntities.BROKEN_CHORUS.get(),
                            AllEntities.LUCIFER_UNCAGED.get(), AllEntities.AZAZEL.get(), AllEntities.LILITH.get(),
                            AllEntities.METATRON.get(), AllEntities.CHUCK.get(), AllEntities.WAR.get(), AllEntities.FAMINE.get(),
                            AllEntities.PESTILENCE.get(), AllEntities.DEATH.get(), AllEntities.MICHAEL.get(), AllEntities.GABRIEL.get(),
                            AllEntities.RAPHAEL.get(), AllEntities.NAOMI.get(), AllEntities.ZACHARIAH.get())
                    .addOptionalTag(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES);
            tag(AllTags.Entities.COLT_EXECUTES).addTag(AllTags.Entities.DEMONS)
                    .add(AllEntities.AMARA_SHADE.get(), AllEntities.CHOIR_ECHO.get(), AllEntities.LUCIFER_ILLUSION.get(),
                            AllEntities.GABRIEL_DOUBLE.get(), AllEntities.VAMPIRE.get(), AllEntities.WEREWOLF.get(),
                            AllEntities.SHAPESHIFTER.get());
            // v0.15: what one round of the Colt never executes (archangels and above, other mods' bosses); mod bosses are #bosses.
            tag(AllTags.Entities.COLT_IMMUNE).add(AllEntities.CAGED_LUCIFER.get(), AllEntities.MESSENGER.get())
                    .addOptionalTag(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES);
        }
    }

    public static class Structures extends TagsProvider<net.minecraft.world.level.levelgen.structure.Structure> {

        public Structures(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existing) {
            super(output, Registries.STRUCTURE, lookup, SupernaturalCraft.MODID, existing);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(AllTags.Structures.HYMNAL_SPIRES).add(SNStructures.HYMNAL_SPIRE);
            tag(AllTags.Structures.LUCIFERS_CAGE).add(SNStructures.LUCIFERS_CAGE);
            tag(AllTags.Structures.GRAVES).add(SNStructures.GRAVE);
        }
    }

    public static class DamageTypes extends TagsProvider<DamageType> {

        public DamageTypes(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existing) {
            super(output, Registries.DAMAGE_TYPE, lookup, SupernaturalCraft.MODID, existing);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(AllTags.DamageTypes.HOLY).add(AllDamageTypes.SMITE, AllDamageTypes.HOLY_WATER, AllDamageTypes.GRACE, AllDamageTypes.COLT,
                    AllDamageTypes.LANCE);
            tag(AllTags.DamageTypes.EXACT_BOSS_DAMAGE).add(AllDamageTypes.COLT);
            tag(DamageTypeTags.BYPASSES_SHIELD).add(AllDamageTypes.COLT);
            tag(AllTags.DamageTypes.LUCIFER_IMMUNE).add(net.minecraft.world.damagesource.DamageTypes.IN_FIRE,
                    net.minecraft.world.damagesource.DamageTypes.ON_FIRE, net.minecraft.world.damagesource.DamageTypes.LAVA,
                    net.minecraft.world.damagesource.DamageTypes.HOT_FLOOR, net.minecraft.world.damagesource.DamageTypes.FALL,
                    net.minecraft.world.damagesource.DamageTypes.DROWN, net.minecraft.world.damagesource.DamageTypes.WITHER,
                    net.minecraft.world.damagesource.DamageTypes.IN_WALL, net.minecraft.world.damagesource.DamageTypes.CRAMMING,
                    net.minecraft.world.damagesource.DamageTypes.FREEZE, AllDamageTypes.HELLFIRE, AllDamageTypes.ARENA_BARRIER);
            tag(DamageTypeTags.BYPASSES_ARMOR).add(AllDamageTypes.SMITE, AllDamageTypes.HOLY_WATER, AllDamageTypes.GRACE,
                    AllDamageTypes.ARENA_BARRIER, AllDamageTypes.HYMN, AllDamageTypes.COLT, AllDamageTypes.WHITE_LIGHT,
                    AllDamageTypes.PLAGUE, AllDamageTypes.STARVED);
            tag(DamageTypeTags.NO_KNOCKBACK).add(AllDamageTypes.PLAGUE, AllDamageTypes.STARVED);
            tag(DamageTypeTags.IS_FIRE).add(AllDamageTypes.HELLFIRE);
            tag(DamageTypeTags.NO_KNOCKBACK).add(AllDamageTypes.ARENA_BARRIER, AllDamageTypes.HOLY_WATER);
            tag(DamageTypeTags.WITCH_RESISTANT_TO).add(AllDamageTypes.SPELL);
            // v0.15: Divine Wrath lands on top of the blow it rides with (no i-frames) and nothing turns it aside.
            tag(DamageTypeTags.BYPASSES_ARMOR).add(AllDamageTypes.DIVINE_WRATH);
            tag(DamageTypeTags.BYPASSES_ENCHANTMENTS).add(AllDamageTypes.DIVINE_WRATH);
            tag(DamageTypeTags.BYPASSES_EFFECTS).add(AllDamageTypes.DIVINE_WRATH);
            tag(DamageTypeTags.BYPASSES_RESISTANCE).add(AllDamageTypes.DIVINE_WRATH);
            tag(DamageTypeTags.BYPASSES_SHIELD).add(AllDamageTypes.DIVINE_WRATH);
            tag(DamageTypeTags.BYPASSES_COOLDOWN).add(AllDamageTypes.DIVINE_WRATH);
            tag(DamageTypeTags.NO_KNOCKBACK).add(AllDamageTypes.DIVINE_WRATH);
        }
    }
}
