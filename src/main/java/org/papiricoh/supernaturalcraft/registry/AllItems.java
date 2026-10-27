package org.papiricoh.supernaturalcraft.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.hunter.DevilsTrapItem;
import org.papiricoh.supernaturalcraft.hunter.HuntersAmuletItem;
import org.papiricoh.supernaturalcraft.magic.item.CageKeyItem;
import org.papiricoh.supernaturalcraft.magic.item.GrimoireItem;
import org.papiricoh.supernaturalcraft.magic.item.SigilPageItem;
import org.papiricoh.supernaturalcraft.magic.item.SpellScrollItem;
import org.papiricoh.supernaturalcraft.reward.ArchangelBladeItem;
import org.papiricoh.supernaturalcraft.weapon.Rune;
import org.papiricoh.supernaturalcraft.weapon.RuneItem;
import org.papiricoh.supernaturalcraft.weapon.catalyst.EmberStaffItem;
import org.papiricoh.supernaturalcraft.weapon.catalyst.EnochianOrbItem;
import org.papiricoh.supernaturalcraft.weapon.melee.AngelBladeItem;
import org.papiricoh.supernaturalcraft.weapon.melee.ExorcistMaceItem;
import org.papiricoh.supernaturalcraft.weapon.melee.SilverMacheteItem;
import org.papiricoh.supernaturalcraft.reward.ColtItem;
import org.papiricoh.supernaturalcraft.reward.LucifersGraceItem;
import org.papiricoh.supernaturalcraft.hunter.HolyWaterItem;
import org.papiricoh.supernaturalcraft.hunter.HunterBladeItem;

public class AllItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SupernaturalCraft.MODID);

    // --- Reagents ----------------------------------------------------------------------------
    /** Salt pours as a salt line, like redstone dust places wire. */
    public static final DeferredItem<ItemNameBlockItem> SALT = ITEMS.register("salt",
            () -> new ItemNameBlockItem(AllBlocks.SALT_LINE.get(), new Item.Properties()));
    public static final DeferredItem<Item> SULFUR = ITEMS.registerSimpleItem("sulfur");
    public static final DeferredItem<Item> DEMON_BLOOD = ITEMS.registerSimpleItem("demon_blood");
    public static final DeferredItem<Item> HELLFIRE_EMBER = ITEMS.registerSimpleItem("hellfire_ember",
            new Item.Properties().rarity(Rarity.UNCOMMON).fireResistant());
    public static final DeferredItem<ItemNameBlockItem> CHALK = ITEMS.register("chalk",
            () -> new ItemNameBlockItem(AllBlocks.CHALK_LINE.get(), new Item.Properties()));
    public static final DeferredItem<ItemNameBlockItem> BLOOD_CHALK = ITEMS.register("blood_chalk",
            () -> new ItemNameBlockItem(AllBlocks.BLOOD_CHALK_LINE.get(), new Item.Properties()));

    // --- Hunter tools ------------------------------------------------------------------------
    public static final DeferredItem<HolyWaterItem> HOLY_WATER = ITEMS.register("holy_water",
            () -> new HolyWaterItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<DevilsTrapItem> DEVILS_TRAP = ITEMS.register("devils_trap",
            () -> new DevilsTrapItem(AllBlocks.DEVILS_TRAP.get(), new Item.Properties().stacksTo(16)));
    public static final DeferredItem<HunterBladeItem> RUBYS_KNIFE = ITEMS.register("rubys_knife",
            () -> new HunterBladeItem(Tiers.IRON, 2.5f, true, new Item.Properties()
                    .attributes(org.papiricoh.supernaturalcraft.weapon.WeaponAttributes.scaled(SwordItem.createAttributes(Tiers.IRON, 2, -2.0f)))));
    public static final DeferredItem<AngelBladeItem> ANGEL_BLADE = ITEMS.register("angel_blade",
            () -> new AngelBladeItem(Tiers.DIAMOND, new Item.Properties().rarity(Rarity.RARE)
                    .attributes(org.papiricoh.supernaturalcraft.weapon.WeaponAttributes.scaled(SwordItem.createAttributes(Tiers.DIAMOND, org.papiricoh.supernaturalcraft.balance.WeaponDamage.ANGEL_BLADE_EXTRA,
                            org.papiricoh.supernaturalcraft.balance.WeaponDamage.ANGEL_BLADE_SPEED)))));

    // --- Arsenal -----------------------------------------------------------------------------
    public static final DeferredItem<SilverMacheteItem> SILVER_MACHETE = ITEMS.register("silver_machete",
            () -> new SilverMacheteItem(Tiers.IRON, new Item.Properties()
                    .attributes(org.papiricoh.supernaturalcraft.weapon.WeaponAttributes.scaled(SwordItem.createAttributes(Tiers.IRON, 3, -2.3f)))));
    public static final DeferredItem<ExorcistMaceItem> EXORCISTS_MACE = ITEMS.register("exorcists_mace",
            () -> new ExorcistMaceItem(new Item.Properties().rarity(Rarity.RARE).durability(600)
                    .component(net.minecraft.core.component.DataComponents.TOOL, net.minecraft.world.item.MaceItem.createToolProperties())
                    .attributes(org.papiricoh.supernaturalcraft.weapon.WeaponAttributes.scaled(net.minecraft.world.item.MaceItem.createAttributes()))));

    // --- Magic -------------------------------------------------------------------------------
    public static final DeferredItem<Item> ENOCHIAN_INK = ITEMS.registerSimpleItem("enochian_ink");
    public static final DeferredItem<GrimoireItem> GRIMOIRE = ITEMS.register("grimoire",
            () -> new GrimoireItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<SpellScrollItem> SPELL_SCROLL = ITEMS.register("spell_scroll",
            () -> new SpellScrollItem(new Item.Properties()));
    public static final DeferredItem<SigilPageItem> SIGIL_PAGE = ITEMS.register("sigil_page",
            () -> new SigilPageItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<HuntersAmuletItem> HUNTERS_AMULET = ITEMS.register("hunters_amulet",
            () -> new HuntersAmuletItem(new Item.Properties().rarity(Rarity.RARE)));

    // --- The Cage ----------------------------------------------------------------------------
    public static final DeferredItem<CageKeyItem> KEY_TO_THE_CAGE = ITEMS.register("key_to_the_cage",
            () -> new CageKeyItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<Item> CRACKED_KEY = ITEMS.registerSimpleItem("cracked_key",
            new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());

    // --- Spoils of the Cage ------------------------------------------------------------------
    public static final DeferredItem<ArchangelBladeItem> ARCHANGEL_BLADE = ITEMS.register("archangel_blade",
            () -> new ArchangelBladeItem(Tiers.NETHERITE, new Item.Properties().rarity(Rarity.EPIC).fireResistant()
                    .attributes(org.papiricoh.supernaturalcraft.weapon.WeaponAttributes.scaled(SwordItem.createAttributes(Tiers.NETHERITE, 5, -2.2f)))));
    public static final DeferredItem<ColtItem> THE_COLT = ITEMS.register("the_colt",
            () -> new ColtItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    /** Creative only (no recipe, no loot): the Colt that never runs dry. */
    public static final DeferredItem<ColtItem> ENDLESS_COLT = ITEMS.register("endless_colt",
            () -> new ColtItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant(), true));
    /** Made only by ritual, eight at a time (or found). */
    public static final DeferredItem<Item> COLT_BULLET = ITEMS.registerSimpleItem("colt_bullet",
            new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<LucifersGraceItem> LUCIFERS_GRACE = ITEMS.register("lucifers_grace",
            () -> new LucifersGraceItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> MORNINGSTAR_TROPHY = ITEMS.register("morningstar_trophy",
            () -> new BlockItem(AllBlocks.MORNINGSTAR_TROPHY.get(), new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<EmberStaffItem> EMBER_STAFF = ITEMS.register("ember_staff",
            () -> new EmberStaffItem(new Item.Properties()));
    public static final DeferredItem<EnochianOrbItem> ENOCHIAN_ORB = ITEMS.register("enochian_orb",
            () -> new EnochianOrbItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<org.papiricoh.supernaturalcraft.weapon.melee.SoulScytheItem> SOUL_SCYTHE = ITEMS.register("soul_scythe",
            () -> new org.papiricoh.supernaturalcraft.weapon.melee.SoulScytheItem(Tiers.DIAMOND, new Item.Properties().rarity(Rarity.RARE)
                    .attributes(org.papiricoh.supernaturalcraft.weapon.WeaponAttributes.scaled(SwordItem.createAttributes(Tiers.DIAMOND, 4, -2.8f)))));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.weapon.melee.HellfireGreatswordItem> HELLFIRE_GREATSWORD = ITEMS.register("hellfire_greatsword",
            () -> new org.papiricoh.supernaturalcraft.weapon.melee.HellfireGreatswordItem(Tiers.DIAMOND, new Item.Properties().rarity(Rarity.RARE)
                    .fireResistant().attributes(org.papiricoh.supernaturalcraft.weapon.WeaponAttributes.scaled(SwordItem.createAttributes(Tiers.DIAMOND, 6, -3.0f)))));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.weapon.catalyst.CenserOfGraceItem> CENSER_OF_GRACE = ITEMS.register("censer_of_grace",
            () -> new org.papiricoh.supernaturalcraft.weapon.catalyst.CenserOfGraceItem(new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredItem<org.papiricoh.supernaturalcraft.weapon.melee.FirstBladeItem> FIRST_BLADE = ITEMS.register("first_blade",
            () -> new org.papiricoh.supernaturalcraft.weapon.melee.FirstBladeItem(Tiers.NETHERITE, new Item.Properties().rarity(Rarity.EPIC)
                    .fireResistant().attributes(org.papiricoh.supernaturalcraft.weapon.WeaponAttributes.scaled(SwordItem.createAttributes(Tiers.NETHERITE, 5, -2.2f)))));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.weapon.catalyst.WhisperingCodexItem> WHISPERING_CODEX = ITEMS.register("whispering_codex",
            () -> new org.papiricoh.supernaturalcraft.weapon.catalyst.WhisperingCodexItem(new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredItem<org.papiricoh.supernaturalcraft.weapon.melee.PenumbraItem> PENUMBRA = ITEMS.register("penumbra",
            () -> new org.papiricoh.supernaturalcraft.weapon.melee.PenumbraItem(Tiers.NETHERITE, new Item.Properties().rarity(Rarity.EPIC)
                    .fireResistant().attributes(org.papiricoh.supernaturalcraft.weapon.WeaponAttributes.scaled(SwordItem.createAttributes(Tiers.NETHERITE, 6, -2.3f)))));
    public static final DeferredItem<Item> VOID_ESSENCE = ITEMS.register("void_essence",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE).fireResistant()));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.reward.EclipseSightItem> ECLIPSE_SIGHT = ITEMS.register("eclipse_sight",
            () -> new org.papiricoh.supernaturalcraft.reward.EclipseSightItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)));
    public static final DeferredItem<BlockItem> ECLIPSE_TROPHY = blockItem(AllBlocks.ECLIPSE_TROPHY);

    public static final DeferredItem<BlockItem> HELLFORGE = blockItem(AllBlocks.HELLFORGE);
    public static final DeferredItem<RuneItem> RUNE_BLANK = ITEMS.register("rune_blank", () -> new RuneItem(null, new Item.Properties()));
    /** One carved rune item per {@link Rune}, registered as rune_&lt;name&gt;. */
    public static final java.util.Map<Rune, DeferredItem<RuneItem>> RUNES = registerRunes();

    private static java.util.Map<Rune, DeferredItem<RuneItem>> registerRunes() {
        java.util.Map<Rune, DeferredItem<RuneItem>> map = new java.util.EnumMap<>(Rune.class);
        for (Rune r : Rune.values()) {
            map.put(r, ITEMS.register("rune_" + r.getSerializedName(),
                    () -> new RuneItem(r, new Item.Properties().rarity(r == Rune.VOID ? Rarity.EPIC : Rarity.UNCOMMON))));
        }
        return java.util.Collections.unmodifiableMap(map);
    }

    // --- Spawn eggs --------------------------------------------------------------------------
    public static final DeferredItem<DeferredSpawnEggItem> BLACK_EYED_DEMON_SPAWN_EGG = ITEMS.register("black_eyed_demon_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.BLACK_EYED_DEMON, 0x16161b, 0x050505, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> DEMON_OCCULTIST_SPAWN_EGG = ITEMS.register("demon_occultist_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.DEMON_OCCULTIST, 0x521826, 0xd9a92b, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> LUCIFER_SPAWN_EGG = ITEMS.register("lucifer_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.LUCIFER, 0x1d1d22, 0xff2a12, new Item.Properties().rarity(Rarity.EPIC)));

    // --- Blocks ------------------------------------------------------------------------------
    public static final DeferredItem<BlockItem> RITUAL_ALTAR = blockItem(AllBlocks.RITUAL_ALTAR);

    // --- The Hymnal Spire ---------------------------------------------------------------------
    public static final DeferredItem<Item> SHATTERED_HYMN = ITEMS.register("shattered_hymn",
            () -> new org.papiricoh.supernaturalcraft.reward.LoreItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(16).fireResistant()));
    public static final DeferredItem<Item> CHOIR_SHARD = ITEMS.register("choir_shard",
            () -> new org.papiricoh.supernaturalcraft.reward.LoreItem(new Item.Properties().rarity(Rarity.RARE).fireResistant()));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.reward.SeraphWingsItem> SERAPH_WINGS = ITEMS.register("seraph_wings",
            () -> new org.papiricoh.supernaturalcraft.reward.SeraphWingsItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant()));
    public static final DeferredItem<BlockItem> CHOIR_TROPHY = ITEMS.register("choir_trophy",
            () -> new BlockItem(AllBlocks.CHOIR_TROPHY.get(), new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<BlockItem> CHOIR_ALTAR = ITEMS.register("choir_altar",
            () -> new BlockItem(AllBlocks.CHOIR_ALTAR.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final java.util.List<DeferredItem<BlockItem>> CHOIR_BELLS = AllBlocks.CHOIR_BELLS.stream()
            .map(b -> ITEMS.register(b.getId().getPath(), () -> new BlockItem(b.get(), new Item.Properties().rarity(Rarity.EPIC))))
            .toList();
    public static final DeferredItem<BlockItem> ROCK_SALT_ORE = blockItem(AllBlocks.ROCK_SALT_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_ROCK_SALT_ORE = blockItem(AllBlocks.DEEPSLATE_ROCK_SALT_ORE);
    public static final DeferredItem<BlockItem> NETHER_SULFUR_ORE = blockItem(AllBlocks.NETHER_SULFUR_ORE);

    // --- Hell ----------------------------------------------------------------------------------
    public static final DeferredItem<Item> BRIMSTONE = lore("brimstone", new Item.Properties().fireResistant());
    public static final DeferredItem<Item> RACK_HOOK = lore("rack_hook", new Item.Properties());
    public static final DeferredItem<Item> DAMNED_CONTRACT = lore("damned_contract", new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> ABYSSAL_SHARD = lore("abyssal_shard", new Item.Properties().rarity(Rarity.UNCOMMON).fireResistant());
    public static final DeferredItem<Item> HELLHOUND_FANG = lore("hellhound_fang", new Item.Properties().rarity(Rarity.UNCOMMON));
    /** What is left when the Morning Star is dragged back into his Cage. */
    public static final DeferredItem<Item> FALLEN_STAR = lore("fallen_star", new Item.Properties().rarity(Rarity.EPIC).fireResistant()
            .component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    /** The four Rings of the Horsemen: together they open the Cage. */
    public static final DeferredItem<Item> RING_OF_WAR = ring("ring_of_war");
    public static final DeferredItem<Item> RING_OF_FAMINE = ring("ring_of_famine");
    public static final DeferredItem<Item> RING_OF_PESTILENCE = ring("ring_of_pestilence");
    public static final DeferredItem<Item> RING_OF_DEATH = ring("ring_of_death");

    // --- Azazel ---------------------------------------------------------------------------------
    /** The blood Azazel fed the special children: the Key to the Cage is forged with it. */
    public static final DeferredItem<Item> AZAZEL_BLOOD = lore("azazel_blood", new Item.Properties().rarity(Rarity.RARE).fireResistant());
    public static final DeferredItem<BlockItem> AZAZEL_TROPHY = ITEMS.register("azazel_trophy",
            () -> new BlockItem(AllBlocks.AZAZEL_TROPHY.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<DeferredSpawnEggItem> AZAZEL_SPAWN_EGG = ITEMS.register("azazel_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.AZAZEL, 0x1a1a1f, 0xf2d22e, new Item.Properties().rarity(Rarity.RARE)));

    // --- Lilith ---------------------------------------------------------------------------------
    /** The sixty-sixth seal, broken when Lilith falls: Lucifer's summoning needs it. */
    public static final DeferredItem<Item> LAST_SEAL = lore("last_seal", new Item.Properties().rarity(Rarity.RARE).stacksTo(16).fireResistant());
    public static final DeferredItem<org.papiricoh.supernaturalcraft.reward.HoundWhistleItem> HOUND_WHISTLE = ITEMS.register("hound_whistle",
            () -> new org.papiricoh.supernaturalcraft.reward.HoundWhistleItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(1)));
    public static final DeferredItem<BlockItem> LILITH_TROPHY = ITEMS.register("lilith_trophy",
            () -> new BlockItem(AllBlocks.LILITH_TROPHY.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<DeferredSpawnEggItem> LILITH_SPAWN_EGG = ITEMS.register("lilith_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.LILITH, 0xe8dcc0, 0xffffff, new Item.Properties().rarity(Rarity.RARE)));

    // --- Metatron -------------------------------------------------------------------------------
    /** The Angel Tablet, still charged from the fight: it rewrites its holder's story whole. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.reward.AngelTabletItem> ANGEL_TABLET = ITEMS.register("angel_tablet",
            () -> new org.papiricoh.supernaturalcraft.reward.AngelTabletItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant()));
    public static final DeferredItem<BlockItem> METATRON_TROPHY = ITEMS.register("metatron_trophy",
            () -> new BlockItem(AllBlocks.METATRON_TROPHY.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> METATRON_SPAWN_EGG = ITEMS.register("metatron_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.METATRON, 0x6b5a48, 0xffd978, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredItem<DeferredSpawnEggItem> HELLHOUND_SPAWN_EGG = ITEMS.register("hellhound_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.HELLHOUND, 0x0b0909, 0xff4a12, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> LUCIFER_UNCAGED_SPAWN_EGG = ITEMS.register("lucifer_uncaged_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.LUCIFER_UNCAGED, 0x050506, 0xb3121a, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredItem<BlockItem> HELLSTONE = blockItem(AllBlocks.HELLSTONE);
    public static final DeferredItem<BlockItem> HELLSTONE_BRICKS = blockItem(AllBlocks.HELLSTONE_BRICKS);
    public static final DeferredItem<BlockItem> RACK_STONE = blockItem(AllBlocks.RACK_STONE);
    public static final DeferredItem<BlockItem> CONGEALED_BLOOD = blockItem(AllBlocks.CONGEALED_BLOOD);
    public static final DeferredItem<BlockItem> MEAT_HOOK = blockItem(AllBlocks.MEAT_HOOK);
    public static final DeferredItem<BlockItem> ASH_BLOCK = blockItem(AllBlocks.ASH_BLOCK);
    public static final DeferredItem<BlockItem> BRIMSTONE_ORE = blockItem(AllBlocks.BRIMSTONE_ORE);
    public static final DeferredItem<BlockItem> HELLFIRE_VENT = blockItem(AllBlocks.HELLFIRE_VENT);
    public static final DeferredItem<BlockItem> CORRIDOR_STONE = blockItem(AllBlocks.CORRIDOR_STONE);
    public static final DeferredItem<BlockItem> CORRIDOR_BRICKS = blockItem(AllBlocks.CORRIDOR_BRICKS);
    public static final DeferredItem<BlockItem> ABYSSAL_STONE = blockItem(AllBlocks.ABYSSAL_STONE);
    public static final DeferredItem<BlockItem> ABYSSAL_SHARD_ORE = blockItem(AllBlocks.ABYSSAL_SHARD_ORE);
    /** The Cage's own blocks: creative only, nothing in survival can break them. */
    public static final java.util.List<DeferredItem<BlockItem>> CAGE_BLOCKS = java.util.stream.Stream.of(AllBlocks.CAGE_BARS,
                    AllBlocks.CAGE_FRAME, AllBlocks.CAGE_SEAL, AllBlocks.CAGE_CHAIN, AllBlocks.ABYSSAL_BEDROCK, AllBlocks.ABYSSAL_FLAGSTONE,
                    AllBlocks.ENOCHIAN_PILLAR, AllBlocks.HELLFIRE_BRAZIER, AllBlocks.CAGE_RITUAL_STONE)
            .map(b -> ITEMS.register(b.getId().getPath(), () -> new BlockItem(b.get(), new Item.Properties().rarity(Rarity.EPIC))))
            .toList();

    // --- The spell bowl and its spells (v0.8) --------------------------------------------------
    public static final DeferredItem<org.papiricoh.supernaturalcraft.bowl.SpellBowlItem> SPELL_BOWL = ITEMS.register("spell_bowl",
            () -> new org.papiricoh.supernaturalcraft.bowl.SpellBowlItem(AllBlocks.SPELL_BOWL.get(), new Item.Properties().stacksTo(1)));
    /** One bowl spell, written out with its Latin: reading it teaches the spell. Which one is a component. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.bowl.page.SpellPageItem> SPELL_PAGE = ITEMS.register("spell_page",
            () -> new org.papiricoh.supernaturalcraft.bowl.page.SpellPageItem(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(16)));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.bowl.spell.BloodVialItem> BLOOD_VIAL = ITEMS.register("blood_vial",
            () -> new org.papiricoh.supernaturalcraft.bowl.spell.BloodVialItem(new Item.Properties().stacksTo(16)));
    // --- v0.21: the bowl's target layer and its distillates -----------------------------------
    /** A snipped lock of someone's hair (blank until used on a creature): names a bowl spell's target. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.bowl.spell.TaglockItem> TAGLOCK = ITEMS.register("taglock",
            () -> new org.papiricoh.supernaturalcraft.bowl.spell.TaglockItem(new Item.Properties().stacksTo(16)));
    /** Holy oil, blessed in the bowl: a reagent rites ask for. */
    public static final DeferredItem<Item> CHRISM = ITEMS.register("chrism",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    /** Ink steeped in one school's magic ({@code school} component): lends a sigil that school. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.magic.item.HexedInkItem> HEXED_INK = ITEMS.register("hexed_ink",
            () -> new org.papiricoh.supernaturalcraft.magic.item.HexedInkItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    /** Blood bound to its owner in the bowl ({@code blood_sample} component): a reagent rites and curses ask for. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.bowl.spell.BoundBloodItem> BOUND_BLOOD = ITEMS.register("bound_blood",
            () -> new org.papiricoh.supernaturalcraft.bowl.spell.BoundBloodItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.bowl.spell.PetCollarItem> PET_COLLAR = ITEMS.register("pet_collar",
            () -> new org.papiricoh.supernaturalcraft.bowl.spell.PetCollarItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.hex.CurseBagItem> CURSE_BAG = ITEMS.register("curse_bag",
            () -> new org.papiricoh.supernaturalcraft.hex.CurseBagItem(AllBlocks.CURSE_BAG.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.hex.ProtectionBagItem> PROTECTION_BAG = ITEMS.register("protection_bag",
            () -> new org.papiricoh.supernaturalcraft.hex.ProtectionBagItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.crossroads.CrossroadsContractItem> CROSSROADS_CONTRACT = ITEMS.register("crossroads_contract",
            () -> new org.papiricoh.supernaturalcraft.crossroads.CrossroadsContractItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    /** What a dispersed ghost leaves behind. */
    public static final DeferredItem<Item> ECTOPLASM = lore("ectoplasm", new Item.Properties());
    public static final DeferredItem<Item> GRAVE_DIRT = lore("grave_dirt", new Item.Properties());
    public static final DeferredItem<BlockItem> GRAVE_HEADSTONE = blockItem(AllBlocks.GRAVE_HEADSTONE);
    public static final DeferredItem<BlockItem> GRAVE_SOIL = blockItem(AllBlocks.GRAVE_SOIL);
    public static final DeferredItem<BlockItem> GRAVE_BONES = ITEMS.register("grave_bones",
            () -> new BlockItem(AllBlocks.GRAVE_BONES.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<DeferredSpawnEggItem> GHOST_SPAWN_EGG = ITEMS.register("ghost_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.GHOST, 0xcfd8dc, 0x6f8fa8, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> CROSSROADS_DEMON_SPAWN_EGG = ITEMS.register("crossroads_demon_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.CROSSROADS_DEMON, 0x101014, 0xc0121c, new Item.Properties()));
    // --- The witches (v0.21) ----------------------------------------------------------------------
    public static final DeferredItem<DeferredSpawnEggItem> HEDGE_WITCH_SPAWN_EGG = ITEMS.register("hedge_witch_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.HEDGE_WITCH, 0x4a5233, 0x9c6b3e, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> COVEN_WITCH_SPAWN_EGG = ITEMS.register("coven_witch_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.COVEN_WITCH, 0x1c1420, 0xb23a8e, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> FAMILIAR_SPAWN_EGG = ITEMS.register("familiar_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.FAMILIAR, 0x151517, 0xd8c23a, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> HEXED_VILLAGER_SPAWN_EGG = ITEMS.register("hexed_villager_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.HEXED_VILLAGER, 0x6b5a4a, 0x5a2752, new Item.Properties()));
    /** Rowena's calling card (dropped by coven witches): its ink runs toward her house ({@code compass_target}). */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.coven.rowena.CallingCardItem> ROWENAS_CALLING_CARD = ITEMS.register("rowenas_calling_card",
            () -> new org.papiricoh.supernaturalcraft.coven.rowena.CallingCardItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    // --- The Author (v0.10) ------------------------------------------------------------------------
    /** "The End": the chronicle of your hunt, in the Author's hand. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.author.ManuscriptItem> THE_END_MANUSCRIPT = ITEMS.register("the_end_manuscript",
            () -> new org.papiricoh.supernaturalcraft.author.ManuscriptItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    /** The Author's Pen: rewrites a small patch of the world. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.author.AuthorsPenItem> AUTHORS_PEN = ITEMS.register("authors_pen",
            () -> new org.papiricoh.supernaturalcraft.author.AuthorsPenItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    /** Sam's amulet: it glows near the powerful. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.author.SamsAmuletItem> SAMS_AMULET = ITEMS.register("sams_amulet",
            () -> new org.papiricoh.supernaturalcraft.author.SamsAmuletItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<BlockItem> TYPEWRITER = ITEMS.register("typewriter",
            () -> new BlockItem(AllBlocks.TYPEWRITER.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> CHUCK_SPAWN_EGG = ITEMS.register("chuck_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.CHUCK, 0xf2ece0, 0x6b4e33, new Item.Properties().rarity(Rarity.EPIC)));

    // --- The Chain of Command (v0.27): the challenge rite's tokens ---------------------------------------------------------
    public static final DeferredItem<Item> GAUNTLET_OF_ASH = lore("gauntlet_of_ash", new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> SEAL_OF_SUMMONS = lore("seal_of_summons", new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));

    private static DeferredItem<Item> lore(String id, Item.Properties props) {
        return ITEMS.register(id, () -> new org.papiricoh.supernaturalcraft.reward.LoreItem(props));
    }

    private static DeferredItem<Item> ring(String id) {
        return lore(id, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                .component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    }

    // --- The Men of Letters (v0.17) --------------------------------------------------------------------------------------
    public static final DeferredItem<BlockItem> BUNKER_DOOR = blockItem(AllBlocks.BUNKER_DOOR);
    public static final DeferredItem<BlockItem> RESEARCH_DESK = blockItem(AllBlocks.RESEARCH_DESK);
    public static final DeferredItem<BlockItem> MAP_TABLE = blockItem(AllBlocks.MAP_TABLE);
    public static final DeferredItem<BlockItem> ARCHIVE_SHELF = blockItem(AllBlocks.ARCHIVE_SHELF);
    public static final DeferredItem<BlockItem> MEN_OF_LETTERS_EMBLEM = blockItem(AllBlocks.MEN_OF_LETTERS_EMBLEM);
    public static final DeferredItem<org.papiricoh.supernaturalcraft.legacy.bunker.BunkerKeyItem> BUNKER_KEY = ITEMS.register("bunker_key",
            () -> new org.papiricoh.supernaturalcraft.legacy.bunker.BunkerKeyItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.legacy.research.FieldNotesItem> FIELD_NOTES = ITEMS.register("field_notes",
            () -> new org.papiricoh.supernaturalcraft.legacy.research.FieldNotesItem(new Item.Properties()));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.legacy.cases.CaseFileItem> CASE_FILE = ITEMS.register("case_file",
            () -> new org.papiricoh.supernaturalcraft.legacy.cases.CaseFileItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.entity.legacy.DeadMansBloodItem> DEAD_MANS_BLOOD = ITEMS.register("dead_mans_blood",
            () -> new org.papiricoh.supernaturalcraft.entity.legacy.DeadMansBloodItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.legacy.artifact.CursedArtifactItem> CURSED_ARTIFACT = ITEMS.register("cursed_artifact",
            () -> new org.papiricoh.supernaturalcraft.legacy.artifact.CursedArtifactItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.legacy.gear.OrderGearItem> MEN_OF_LETTERS_RING = ITEMS.register("men_of_letters_ring",
            () -> new org.papiricoh.supernaturalcraft.legacy.gear.OrderGearItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.legacy.gear.SpectaclesItem> SPELLWRIGHTS_SPECTACLES = ITEMS.register("spellwrights_spectacles",
            () -> new org.papiricoh.supernaturalcraft.legacy.gear.SpectaclesItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
                    .durability(net.minecraft.world.item.ArmorItem.Type.HELMET.getDurability(25))));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.legacy.gear.HenrysCaseItem> HENRYS_CASE = ITEMS.register("henrys_case",
            () -> new org.papiricoh.supernaturalcraft.legacy.gear.HenrysCaseItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
                    .component(net.minecraft.core.component.DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.EMPTY)));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.legacy.gear.OrderGearItem> AQUARIAN_STAR = ITEMS.register("aquarian_star",
            () -> new org.papiricoh.supernaturalcraft.legacy.gear.OrderGearItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    /** The war table's blueprint (v0.19): researched, then crafted (and returned) into map table pieces. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.legacy.cases.table.WarTableBlueprintItem> WAR_TABLE_BLUEPRINT = ITEMS.register("war_table_blueprint",
            () -> new org.papiricoh.supernaturalcraft.legacy.cases.table.WarTableBlueprintItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    /** The order's compass (v0.19): points at its holder's open case, or at a pin set at a war table. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.legacy.cases.table.LettersCompassItem> LETTERS_COMPASS = ITEMS.register("letters_compass",
            () -> new org.papiricoh.supernaturalcraft.legacy.cases.table.LettersCompassItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<DeferredSpawnEggItem> HENRY_WINCHESTER_SPAWN_EGG = ITEMS.register("henry_winchester_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.HENRY, 0x3b3328, 0xc9b48a, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> VAMPIRE_SPAWN_EGG = ITEMS.register("vampire_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.VAMPIRE, 0x1b1a1f, 0x8a1020, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> WEREWOLF_SPAWN_EGG = ITEMS.register("werewolf_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.WEREWOLF, 0x4a3a2c, 0xd8c23a, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> SHAPESHIFTER_SPAWN_EGG = ITEMS.register("shapeshifter_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.SHAPESHIFTER, 0xb08f74, 0x6b4a3a, new Item.Properties()));

    private static DeferredItem<BlockItem> blockItem(DeferredBlock<?> block) {
        return ITEMS.registerSimpleBlockItem(block);
    }

    // --- The Four Horsemen (v0.11) ---------------------------------------------------------------
    /** Found in the swamp while Pestilence fights: cures the plague and keeps it off for a while. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.entity.boss.horsemen.pestilence.AntidoteVialItem> ANTIDOTE_VIAL =
            ITEMS.register("antidote_vial", () -> new org.papiricoh.supernaturalcraft.entity.boss.horsemen.pestilence.AntidoteVialItem(
                    new Item.Properties().stacksTo(4).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<BlockItem> WAR_TROPHY = ITEMS.register("war_trophy",
            () -> new BlockItem(AllBlocks.WAR_TROPHY.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> FAMINE_TROPHY = ITEMS.register("famine_trophy",
            () -> new BlockItem(AllBlocks.FAMINE_TROPHY.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> PESTILENCE_TROPHY = ITEMS.register("pestilence_trophy",
            () -> new BlockItem(AllBlocks.PESTILENCE_TROPHY.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> DEATH_TROPHY = ITEMS.register("death_trophy",
            () -> new BlockItem(AllBlocks.DEATH_TROPHY.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> WAR_SPAWN_EGG = ITEMS.register("war_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.WAR, 0x5a0f12, 0xc8202a, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> FAMINE_SPAWN_EGG = ITEMS.register("famine_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.FAMINE, 0x2a2622, 0x9a8a62, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> PESTILENCE_SPAWN_EGG = ITEMS.register("pestilence_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.PESTILENCE, 0x8fa36a, 0x3d4a22, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> DEATH_SPAWN_EGG = ITEMS.register("death_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.DEATH, 0x0c0c0e, 0xd8d8d0, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> REAPER_SPAWN_EGG = ITEMS.register("reaper_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.REAPER, 0x111114, 0xbcb8ae, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> HORSEMAN_STEED_SPAWN_EGG = ITEMS.register("horseman_steed_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.HORSEMAN_STEED, 0x7a1414, 0xe2ddd2, new Item.Properties().rarity(Rarity.RARE)));

    // --- The Archangel Michael (v0.12) -----------------------------------------------------------
    /** The Lance of Michael: left by him every victory. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.reward.michael.MichaelLanceItem> MICHAEL_LANCE = ITEMS.register("michael_lance",
            () -> new org.papiricoh.supernaturalcraft.reward.michael.MichaelLanceItem(new Item.Properties().stacksTo(1).durability(750).rarity(Rarity.EPIC).fireResistant()));
    /** His own lance, pulled out of the ground in his last phase: it burns in the hand and goes back to him. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.reward.michael.BorrowedLanceItem> BORROWED_LANCE = ITEMS.register("borrowed_lance",
            () -> new org.papiricoh.supernaturalcraft.reward.michael.BorrowedLanceItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    /** Michael's Grace: with the Seraph Wings, flight. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.reward.michael.MichaelsGraceItem> MICHAELS_GRACE = ITEMS.register("michaels_grace",
            () -> new org.papiricoh.supernaturalcraft.reward.michael.MichaelsGraceItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<BlockItem> MICHAEL_TROPHY = ITEMS.register("michael_trophy",
            () -> new BlockItem(AllBlocks.MICHAEL_TROPHY.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.reward.michael.GeneralArmorItem> GENERAL_HELMET = general("general_helmet", net.minecraft.world.item.ArmorItem.Type.HELMET);
    public static final DeferredItem<org.papiricoh.supernaturalcraft.reward.michael.GeneralArmorItem> GENERAL_CHESTPLATE = general("general_chestplate", net.minecraft.world.item.ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<org.papiricoh.supernaturalcraft.reward.michael.GeneralArmorItem> GENERAL_LEGGINGS = general("general_leggings", net.minecraft.world.item.ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<org.papiricoh.supernaturalcraft.reward.michael.GeneralArmorItem> GENERAL_BOOTS = general("general_boots", net.minecraft.world.item.ArmorItem.Type.BOOTS);
    public static final DeferredItem<DeferredSpawnEggItem> MICHAEL_SPAWN_EGG = ITEMS.register("michael_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.MICHAEL, 0xd8dde4, 0x6fb4ff, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> HOST_ANGEL_SPAWN_EGG = ITEMS.register("host_angel_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.HOST_ANGEL, 0x2b2f38, 0xd9c27a, new Item.Properties()));
    // --- Angels and the fallen (v0.22) ---
    public static final DeferredItem<DeferredSpawnEggItem> ANGEL_SPAWN_EGG = ITEMS.register("angel_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.ANGEL, 0x3a3f4a, 0x8fc4ff, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> FALLEN_ANGEL_SPAWN_EGG = ITEMS.register("fallen_angel_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.FALLEN_ANGEL, 0x4a3b30, 0x2a2a2a, new Item.Properties()));

    // --- Allegiance (v0.13) ---------------------------------------------------------------------------------------------
    /** Grace in a vial: the messenger's gift, the key to the rite Receive Grace; what ripping out Grace leaves. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.allegiance.item.VialOfGraceItem> VIAL_OF_GRACE = ITEMS.register("vial_of_grace",
            () -> new org.papiricoh.supernaturalcraft.allegiance.item.VialOfGraceItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    /** Holy oil: a ring of its fire holds an angel. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.allegiance.item.HolyOilItem> HOLY_OIL = ITEMS.register("holy_oil",
            () -> new org.papiricoh.supernaturalcraft.allegiance.item.HolyOilItem(new Item.Properties().stacksTo(16)));
    /** Human blood made holy: the demon cure, a night at a time. */
    public static final DeferredItem<Item> PURIFIED_BLOOD = ITEMS.register("purified_blood",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<DeferredSpawnEggItem> RIVAL_HUNTER_SPAWN_EGG = ITEMS.register("rival_hunter_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.RIVAL_HUNTER, 0x4a3b2c, 0xb8b2a6, new Item.Properties()));

    // --- Gabriel, the Trickster (v0.14) ---------------------------------------------------------------------------------
    /** The bait for the Trickster: the activator of his rite (consumed). Made by the rite Sweeten the Pot. */
    public static final DeferredItem<Item> TRICKSTER_BAIT = ITEMS.register("trickster_bait",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    /** A candy wrapper: what one of his pranks leaves behind. Nothing more. */
    public static final DeferredItem<Item> CANDY_WRAPPER = ITEMS.register("candy_wrapper",
            () -> new Item(new Item.Properties()));
    /** Trickster Candy: one good (or silly) effect at random. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.reward.gabriel.TricksterCandyItem> TRICKSTER_CANDY = ITEMS.register("trickster_candy",
            () -> new org.papiricoh.supernaturalcraft.reward.gabriel.TricksterCandyItem(new Item.Properties().rarity(Rarity.UNCOMMON)
                    .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).alwaysEdible().fast().build())));
    /** The Trickster's Remote: changes a creature's channel. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.reward.gabriel.TricksterRemoteItem> TRICKSTER_REMOTE = ITEMS.register("trickster_remote",
            () -> new org.papiricoh.supernaturalcraft.reward.gabriel.TricksterRemoteItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    /** Gabriel's archangel blade. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.reward.gabriel.GabrielBladeItem> GABRIEL_BLADE = ITEMS.register("gabriel_blade",
            () -> new org.papiricoh.supernaturalcraft.reward.gabriel.GabrielBladeItem(Tiers.NETHERITE, new Item.Properties().rarity(Rarity.EPIC).fireResistant()
                    .attributes(org.papiricoh.supernaturalcraft.weapon.WeaponAttributes.scaled(SwordItem.createAttributes(Tiers.NETHERITE, 4, -2.0f)))));
    public static final DeferredItem<BlockItem> GABRIEL_TROPHY = ITEMS.register("gabriel_trophy",
            () -> new BlockItem(AllBlocks.GABRIEL_TROPHY.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> GABRIEL_SPAWN_EGG = ITEMS.register("gabriel_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.GABRIEL, 0x5b5a3a, 0xe6c04a, new Item.Properties().rarity(Rarity.EPIC)));

    // --- Raphael, the archangel of the storm (v0.16) ---------------------------------------------------------------------
    public static final DeferredItem<org.papiricoh.supernaturalcraft.reward.raphael.StormcallerItem> RAPHAELS_STORMCALLER = ITEMS.register("raphaels_stormcaller",
            () -> new org.papiricoh.supernaturalcraft.reward.raphael.StormcallerItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<BlockItem> RAPHAEL_TROPHY = ITEMS.register("raphael_trophy",
            () -> new BlockItem(AllBlocks.RAPHAEL_TROPHY.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> RAPHAEL_SPAWN_EGG = ITEMS.register("raphael_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.RAPHAEL, 0x23252b, 0x9fc4ff, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> GARRISON_ANGEL_SPAWN_EGG = ITEMS.register("garrison_angel_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.GARRISON_ANGEL, 0x4a4f5a, 0xb8c7dd, new Item.Properties()));

    // --- Heaven (v0.18): a hunter's own Heaven, Naomi, Zachariah and the wild crossroads --------------------------------
    public static final DeferredItem<BlockItem> HEARTH = ITEMS.register("hearth",
            () -> new BlockItem(AllBlocks.HEARTH.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> CLOUD_STONE = ITEMS.register("cloud_stone",
            () -> new BlockItem(AllBlocks.CLOUD_STONE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CLOUD_BRICKS = ITEMS.register("cloud_bricks",
            () -> new BlockItem(AllBlocks.CLOUD_BRICKS.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> FILING_CABINET = ITEMS.register("filing_cabinet",
            () -> new BlockItem(AllBlocks.FILING_CABINET.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CROSSROADS_SOIL = ITEMS.register("crossroads_soil",
            () -> new BlockItem(AllBlocks.CROSSROADS_SOIL.get(), new Item.Properties()));
    /** A crossroads box: bury it at a natural crossroads. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.crossroads.wild.CrossroadsBoxItem> CROSSROADS_BOX = ITEMS.register("crossroads_box",
            () -> new org.papiricoh.supernaturalcraft.crossroads.wild.CrossroadsBoxItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    /** Naomi's drill (holy). */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.reward.heaven.NaomisDrillItem> NAOMIS_DRILL = ITEMS.register("naomis_drill",
            () -> new org.papiricoh.supernaturalcraft.reward.heaven.NaomisDrillItem(Tiers.NETHERITE, new Item.Properties().rarity(Rarity.EPIC).fireResistant()
                    .attributes(org.papiricoh.supernaturalcraft.weapon.WeaponAttributes.scaled(SwordItem.createAttributes(Tiers.NETHERITE, 3, -2.2f)))));
    /** Naomi's diadem (a charm). */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.reward.heaven.NaomisDiademItem> NAOMIS_DIADEM = ITEMS.register("naomis_diadem",
            () -> new org.papiricoh.supernaturalcraft.reward.heaven.NaomisDiademItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    /** Zachariah's angel blade (holy). */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.reward.heaven.ZachariahsBladeItem> ZACHARIAHS_BLADE = ITEMS.register("zachariahs_blade",
            () -> new org.papiricoh.supernaturalcraft.reward.heaven.ZachariahsBladeItem(Tiers.NETHERITE, new Item.Properties().rarity(Rarity.EPIC).fireResistant()
                    .attributes(org.papiricoh.supernaturalcraft.weapon.WeaponAttributes.scaled(SwordItem.createAttributes(Tiers.NETHERITE, 4, -2.1f)))));
    /** Heaven's Seal (a charm). */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.reward.heaven.HeavensSealItem> HEAVENS_SEAL = ITEMS.register("heavens_seal",
            () -> new org.papiricoh.supernaturalcraft.reward.heaven.HeavensSealItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    /** A Heavenly Form (only in Zachariah's office). */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.entity.boss.zachariah.HeavenlyFormItem> HEAVENLY_FORM = ITEMS.register("heavenly_form",
            () -> new org.papiricoh.supernaturalcraft.entity.boss.zachariah.HeavenlyFormItem(new Item.Properties().stacksTo(1)));
    /** A clerk's approval stamp. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.entity.boss.zachariah.ApprovalStampItem> APPROVAL_STAMP = ITEMS.register("approval_stamp",
            () -> new org.papiricoh.supernaturalcraft.entity.boss.zachariah.ApprovalStampItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<BlockItem> NAOMI_TROPHY = ITEMS.register("naomi_trophy",
            () -> new BlockItem(AllBlocks.NAOMI_TROPHY.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> ZACHARIAH_TROPHY = ITEMS.register("zachariah_trophy",
            () -> new BlockItem(AllBlocks.ZACHARIAH_TROPHY.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> NAOMI_SPAWN_EGG = ITEMS.register("naomi_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.NAOMI, 0xe9ecef, 0x5fb7c9, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> ZACHARIAH_SPAWN_EGG = ITEMS.register("zachariah_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.ZACHARIAH, 0x3b3f4a, 0xe8d49a, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> HEAVEN_GUARD_SPAWN_EGG = ITEMS.register("heaven_guard_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.HEAVEN_GUARD, 0x2c2f36, 0xd8f4ff, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> CLERK_ANGEL_SPAWN_EGG = ITEMS.register("clerk_angel_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.CLERK_ANGEL, 0x6b6150, 0xf2e6c4, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> ASH_SPAWN_EGG = ITEMS.register("ash_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.ASH, 0x4a3420, 0xb08a4a, new Item.Properties()));

    // --- The power curve (v0.15) ---------------------------------------------------------------------------------------
    /** Ascension Shards I-V: each raises a weapon or armour piece one tier at the Hellforge. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.weapon.ascension.AscensionShardItem> ASCENSION_SHARD_1 = shard(1, Rarity.UNCOMMON);
    public static final DeferredItem<org.papiricoh.supernaturalcraft.weapon.ascension.AscensionShardItem> ASCENSION_SHARD_2 = shard(2, Rarity.UNCOMMON);
    public static final DeferredItem<org.papiricoh.supernaturalcraft.weapon.ascension.AscensionShardItem> ASCENSION_SHARD_3 = shard(3, Rarity.RARE);
    public static final DeferredItem<org.papiricoh.supernaturalcraft.weapon.ascension.AscensionShardItem> ASCENSION_SHARD_4 = shard(4, Rarity.RARE);
    public static final DeferredItem<org.papiricoh.supernaturalcraft.weapon.ascension.AscensionShardItem> ASCENSION_SHARD_5 = shard(5, Rarity.EPIC);
    public static final DeferredItem<org.papiricoh.supernaturalcraft.hunter.gear.HunterGearItem> HUNTERS_CAP = hunterGear("hunters_cap", net.minecraft.world.item.ArmorItem.Type.HELMET);
    public static final DeferredItem<org.papiricoh.supernaturalcraft.hunter.gear.HunterGearItem> HUNTERS_JACKET = hunterGear("hunters_jacket", net.minecraft.world.item.ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<org.papiricoh.supernaturalcraft.hunter.gear.HunterGearItem> HUNTERS_JEANS = hunterGear("hunters_jeans", net.minecraft.world.item.ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<org.papiricoh.supernaturalcraft.hunter.gear.HunterGearItem> HUNTERS_BOOTS = hunterGear("hunters_boots", net.minecraft.world.item.ArmorItem.Type.BOOTS);

    /** Shard of {@code tier}, 1-5. */
    public static DeferredItem<org.papiricoh.supernaturalcraft.weapon.ascension.AscensionShardItem> shardOf(int tier) {
        return switch (tier) {
            case 1 -> ASCENSION_SHARD_1;
            case 2 -> ASCENSION_SHARD_2;
            case 3 -> ASCENSION_SHARD_3;
            case 4 -> ASCENSION_SHARD_4;
            case 5 -> ASCENSION_SHARD_5;
            default -> throw new IllegalArgumentException("no shard of tier " + tier);
        };
    }

    private static DeferredItem<org.papiricoh.supernaturalcraft.weapon.ascension.AscensionShardItem> shard(int tier, Rarity rarity) {
        return ITEMS.register("ascension_shard_" + tier, () -> new org.papiricoh.supernaturalcraft.weapon.ascension.AscensionShardItem(tier,
                new Item.Properties().stacksTo(16).rarity(rarity).fireResistant()));
    }

    private static DeferredItem<org.papiricoh.supernaturalcraft.hunter.gear.HunterGearItem> hunterGear(String id, net.minecraft.world.item.ArmorItem.Type type) {
        return ITEMS.register(id, () -> new org.papiricoh.supernaturalcraft.hunter.gear.HunterGearItem(AllArmorMaterials.HUNTER, type,
                new Item.Properties().durability(type.getDurability(AllArmorMaterials.HUNTER_DURABILITY))));
    }

    private static DeferredItem<org.papiricoh.supernaturalcraft.reward.michael.GeneralArmorItem> general(String id, net.minecraft.world.item.ArmorItem.Type type) {
        return ITEMS.register(id, () -> new org.papiricoh.supernaturalcraft.reward.michael.GeneralArmorItem(AllArmorMaterials.GENERAL, type,
                new Item.Properties().durability(type.getDurability(AllArmorMaterials.GENERAL_DURABILITY)).rarity(Rarity.EPIC).fireResistant()));
    }

    // --- Cain, the Father of Murder, and the Mark (v0.21) ---------------------------------------------------------------------
    /** A jar of Cain's honey: food, and to a bearer of the Mark a quarter of its Thirst slaked. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.mark.CainsHoneyItem> CAINS_HONEY = ITEMS.register("cains_honey",
            () -> new org.papiricoh.supernaturalcraft.mark.CainsHoneyItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)
                    .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(6).saturationModifier(0.4f).alwaysEdible().build())));
    /** Rowena's note of where Cain keeps his bees: its ink leads to his gate ({@code compass_target}). */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.cain.CainsAddressItem> CAINS_ADDRESS = ITEMS.register("cains_address",
            () -> new org.papiricoh.supernaturalcraft.cain.CainsAddressItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<BlockItem> CAIN_TROPHY = ITEMS.register("cain_trophy",
            () -> new BlockItem(AllBlocks.CAIN_TROPHY.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> CAIN_SPAWN_EGG = ITEMS.register("cain_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.CAIN, 0x5a4632, 0xb0201a, new Item.Properties().rarity(Rarity.EPIC)));

    // --- Abaddon, Knight of Hell (v0.21) --------------------------------------------------------------------------------------
    /** The Book of the Damned: Abaddon's spoil, one per hunter who fought her; Rowena's price for the Mark's removal (P6). */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.entity.boss.abaddon.BookOfTheDamnedItem> BOOK_OF_THE_DAMNED = ITEMS.register("book_of_the_damned",
            () -> new org.papiricoh.supernaturalcraft.entity.boss.abaddon.BookOfTheDamnedItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    /** The Knights' Cleaver: Abaddon's own blade (Tier IV, cursed); use it to send a wave of hellfire down the ground. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.entity.boss.abaddon.KnightsCleaverItem> KNIGHTS_CLEAVER = ITEMS.register("knights_cleaver",
            () -> new org.papiricoh.supernaturalcraft.entity.boss.abaddon.KnightsCleaverItem(Tiers.NETHERITE, new Item.Properties().rarity(Rarity.EPIC)
                    .fireResistant().attributes(org.papiricoh.supernaturalcraft.weapon.WeaponAttributes.scaled(SwordItem.createAttributes(Tiers.NETHERITE, 7, -2.8f)))));
    public static final DeferredItem<BlockItem> ABADDON_TROPHY = ITEMS.register("abaddon_trophy",
            () -> new BlockItem(AllBlocks.ABADDON_TROPHY.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> ABADDON_SPAWN_EGG = ITEMS.register("abaddon_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.ABADDON, 0x1a1414, 0xb3261e, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> ABADDON_KNIGHT_SPAWN_EGG = ITEMS.register("abaddon_knight_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.ABADDON_KNIGHT, 0x2b2626, 0x8a1c14, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> ABADDON_HERALD_SPAWN_EGG = ITEMS.register("abaddon_herald_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.ABADDON_HERALD, 0x3a1e22, 0xd8452a, new Item.Properties()));

    // --- Rowena and the Grand Witch (v0.21, P6) -------------------------------------------------------------------------------
    /** Rowena's Grimoire: a catalyst that speaks again the last aimed bowl spell its bearer cast, without a bowl, once a minute. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenasGrimoireItem> ROWENAS_GRIMOIRE = ITEMS.register("rowenas_grimoire",
            () -> new org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenasGrimoireItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<BlockItem> ROWENA_TROPHY = ITEMS.register("rowena_trophy",
            () -> new BlockItem(AllBlocks.ROWENA_TROPHY.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> ROWENA_SPAWN_EGG = ITEMS.register("rowena_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.ROWENA, 0x1f3b26, 0xc0392b, new Item.Properties().rarity(Rarity.EPIC)));
    /** The Coven Staff: the Grand Witch's own, a Witchcraft catalyst (GeckoLib). */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.entity.boss.grandwitch.CovenStaffItem> COVEN_STAFF = ITEMS.register("coven_staff",
            () -> new org.papiricoh.supernaturalcraft.entity.boss.grandwitch.CovenStaffItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<BlockItem> GRAND_WITCH_TROPHY = ITEMS.register("grand_witch_trophy",
            () -> new BlockItem(AllBlocks.GRAND_WITCH_TROPHY.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> GRAND_WITCH_SPAWN_EGG = ITEMS.register("grand_witch_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.GRAND_WITCH, 0x1a1024, 0x9b59d0, new Item.Properties().rarity(Rarity.EPIC)));

    // --- The Leviathans (v0.25) -----------------------------------------------------------------------------------------
    /** Salt, sand and bone meal, ground together: smelted, borax. */
    public static final DeferredItem<Item> BORAX_BLEND = ITEMS.register("borax_blend", () -> new Item(new Item.Properties()));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.leviathan.BoraxItems.Powder> BORAX = ITEMS.register("borax",
            () -> new org.papiricoh.supernaturalcraft.leviathan.BoraxItems.Powder(new Item.Properties()));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.leviathan.BoraxItems.Spray> BORAX_SPRAY = ITEMS.register("borax_spray",
            () -> new org.papiricoh.supernaturalcraft.leviathan.BoraxItems.Spray(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.leviathan.BoraxItems.Sprayer> BORAX_SPRAYER = ITEMS.register("borax_sprayer",
            () -> new org.papiricoh.supernaturalcraft.leviathan.BoraxItems.Sprayer(new Item.Properties().rarity(Rarity.RARE)));
    /** SucroCorp's food: filling, and laced (Sedated). */
    public static final DeferredItem<Item> TURDUCKEN_SLAMMER = ITEMS.register("turducken_slammer", () -> new Item(new Item.Properties()
            .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(10).saturationModifier(1.0f)
                    .effect(() -> new net.minecraft.world.effect.MobEffectInstance(AllMobEffects.SEDATED,
                            org.papiricoh.supernaturalcraft.leviathan.LeviathanRules.SLAMMER_SEDATION), 1.0f).build())));
    public static final DeferredItem<Item> BIGGERSONS_BURGER = ITEMS.register("biggersons_burger", () -> new Item(new Item.Properties()
            .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(8).saturationModifier(0.9f)
                    .effect(() -> new net.minecraft.world.effect.MobEffectInstance(AllMobEffects.SEDATED,
                            org.papiricoh.supernaturalcraft.leviathan.LeviathanRules.BURGER_SEDATION), 1.0f).build())));
    /** The bone of a righteous mortal (a nun's, from a church crypt), and the same washed in the blood of the three. */
    public static final DeferredItem<Item> RIGHTEOUS_BONE = ITEMS.register("righteous_bone", () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> ANOINTED_RIGHTEOUS_BONE = ITEMS.register("anointed_righteous_bone",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    /** The three bloods: of a fallen angel (Raphael), of a King of Hell (a crossroads demon), of an Alpha (a vampire). */
    public static final DeferredItem<Item> FALLEN_BLOOD = ITEMS.register("fallen_blood", () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> KINGS_BLOOD = ITEMS.register("kings_blood", () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> ALPHA_BLOOD = ITEMS.register("alpha_blood", () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    /** A shard of the Leviathan Tablet, the Word of God: it teaches Leviathan's Bane. */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.magic.item.SigilPageItem> LEVIATHAN_TABLET = ITEMS.register("leviathan_tablet",
            () -> new org.papiricoh.supernaturalcraft.magic.item.SigilPageItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                    .component(AllDataComponents.SIGIL_PAGE, org.papiricoh.supernaturalcraft.SupernaturalCraft.asResource("leviathans_bane"))));
    public static final DeferredItem<DeferredSpawnEggItem> DICK_ROMAN_SPAWN_EGG = ITEMS.register("dick_roman_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.DICK_ROMAN, 0x2f3136, 0xb3202a, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> LEVIATHAN_SPAWN_EGG = ITEMS.register("leviathan_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.LEVIATHAN, 0x3b3f45, 0x0b0b0d, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> LEVIATHAN_EXECUTIVE_SPAWN_EGG = ITEMS.register("leviathan_executive_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.LEVIATHAN_EXECUTIVE, 0x22252b, 0xc9a227, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> LEVIATHAN_GUARD_SPAWN_EGG = ITEMS.register("leviathan_guard_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.LEVIATHAN_GUARD, 0x1d2a33, 0x0b0b0d, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> LEVIATHAN_CHEMIST_SPAWN_EGG = ITEMS.register("leviathan_chemist_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.LEVIATHAN_CHEMIST, 0xe8e8e2, 0x0b0b0d, new Item.Properties()));
    // The Chain of Command's rank and file (v0.26).
    public static final DeferredItem<DeferredSpawnEggItem> DEMON_BRUTE_SPAWN_EGG = ITEMS.register("demon_brute_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.DEMON_BRUTE, 0x3a3632, 0x0b0b0b, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> SULFUR_PRIEST_SPAWN_EGG = ITEMS.register("sulfur_priest_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.SULFUR_PRIEST, 0x1a1612, 0xc9a43a, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> DEMON_FLAYER_SPAWN_EGG = ITEMS.register("demon_flayer_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.DEMON_FLAYER, 0x6b5a4a, 0x2a0a0a, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> DEMON_TORMENTOR_SPAWN_EGG = ITEMS.register("demon_tormentor_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.DEMON_TORMENTOR, 0x3b2a20, 0x5a5a5a, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> ANGEL_TEMPLAR_SPAWN_EGG = ITEMS.register("angel_templar_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.ANGEL_TEMPLAR, 0x4a4d52, 0xd8dde3, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> ANGEL_HERALD_SPAWN_EGG = ITEMS.register("angel_herald_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.ANGEL_HERALD, 0xe8e0cc, 0xf2c94c, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> ANGEL_CHOIRMASTER_SPAWN_EGG = ITEMS.register("angel_choirmaster_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.ANGEL_CHOIRMASTER, 0xf2f2f2, 0x22346b, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> ANGEL_DOMINION_SPAWN_EGG = ITEMS.register("angel_dominion_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.ANGEL_DOMINION, 0xbfae7a, 0x2b2b30, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> GRIGORI_SPAWN_EGG = ITEMS.register("grigori_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.GRIGORI, 0x141414, 0x6a6a6a, new Item.Properties()));

    public static void init() {
    }
}
