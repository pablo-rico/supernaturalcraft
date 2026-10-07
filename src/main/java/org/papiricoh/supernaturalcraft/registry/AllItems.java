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
                    .attributes(SwordItem.createAttributes(Tiers.IRON, 2, -2.0f))));
    public static final DeferredItem<AngelBladeItem> ANGEL_BLADE = ITEMS.register("angel_blade",
            () -> new AngelBladeItem(Tiers.DIAMOND, new Item.Properties().rarity(Rarity.RARE)
                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 3, -2.2f))));

    // --- Arsenal -----------------------------------------------------------------------------
    public static final DeferredItem<SilverMacheteItem> SILVER_MACHETE = ITEMS.register("silver_machete",
            () -> new SilverMacheteItem(Tiers.IRON, new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.3f))));
    public static final DeferredItem<ExorcistMaceItem> EXORCISTS_MACE = ITEMS.register("exorcists_mace",
            () -> new ExorcistMaceItem(new Item.Properties().rarity(Rarity.RARE).durability(600)
                    .component(net.minecraft.core.component.DataComponents.TOOL, net.minecraft.world.item.MaceItem.createToolProperties())
                    .attributes(net.minecraft.world.item.MaceItem.createAttributes())));

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
                    .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 5, -2.2f))));
    public static final DeferredItem<ColtItem> THE_COLT = ITEMS.register("the_colt",
            () -> new ColtItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    /** Made only by ritual, eight at a time (or found): a sixteen-stack is a fortune. */
    public static final DeferredItem<Item> COLT_BULLET = ITEMS.registerSimpleItem("colt_bullet",
            new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
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
                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 4, -2.8f))));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.weapon.melee.HellfireGreatswordItem> HELLFIRE_GREATSWORD = ITEMS.register("hellfire_greatsword",
            () -> new org.papiricoh.supernaturalcraft.weapon.melee.HellfireGreatswordItem(Tiers.DIAMOND, new Item.Properties().rarity(Rarity.RARE)
                    .fireResistant().attributes(SwordItem.createAttributes(Tiers.DIAMOND, 6, -3.0f))));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.weapon.catalyst.CenserOfGraceItem> CENSER_OF_GRACE = ITEMS.register("censer_of_grace",
            () -> new org.papiricoh.supernaturalcraft.weapon.catalyst.CenserOfGraceItem(new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredItem<org.papiricoh.supernaturalcraft.weapon.melee.FirstBladeItem> FIRST_BLADE = ITEMS.register("first_blade",
            () -> new org.papiricoh.supernaturalcraft.weapon.melee.FirstBladeItem(Tiers.NETHERITE, new Item.Properties().rarity(Rarity.EPIC)
                    .fireResistant().attributes(SwordItem.createAttributes(Tiers.NETHERITE, 5, -2.2f))));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.weapon.catalyst.WhisperingCodexItem> WHISPERING_CODEX = ITEMS.register("whispering_codex",
            () -> new org.papiricoh.supernaturalcraft.weapon.catalyst.WhisperingCodexItem(new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredItem<org.papiricoh.supernaturalcraft.weapon.melee.PenumbraItem> PENUMBRA = ITEMS.register("penumbra",
            () -> new org.papiricoh.supernaturalcraft.weapon.melee.PenumbraItem(Tiers.NETHERITE, new Item.Properties().rarity(Rarity.EPIC)
                    .fireResistant().attributes(SwordItem.createAttributes(Tiers.NETHERITE, 6, -2.3f))));
    public static final DeferredItem<Item> VOID_ESSENCE = ITEMS.register("void_essence",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
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
    public static final DeferredItem<Item> AZAZEL_BLOOD = lore("azazel_blood", new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> AZAZEL_TROPHY = ITEMS.register("azazel_trophy",
            () -> new BlockItem(AllBlocks.AZAZEL_TROPHY.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<DeferredSpawnEggItem> AZAZEL_SPAWN_EGG = ITEMS.register("azazel_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.AZAZEL, 0x1a1a1f, 0xf2d22e, new Item.Properties().rarity(Rarity.RARE)));

    // --- Lilith ---------------------------------------------------------------------------------
    /** The sixty-sixth seal, broken when Lilith falls: Lucifer's summoning needs it. */
    public static final DeferredItem<Item> LAST_SEAL = lore("last_seal", new Item.Properties().rarity(Rarity.RARE).stacksTo(16));
    public static final DeferredItem<org.papiricoh.supernaturalcraft.reward.HoundWhistleItem> HOUND_WHISTLE = ITEMS.register("hound_whistle",
            () -> new org.papiricoh.supernaturalcraft.reward.HoundWhistleItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(1)));
    public static final DeferredItem<BlockItem> LILITH_TROPHY = ITEMS.register("lilith_trophy",
            () -> new BlockItem(AllBlocks.LILITH_TROPHY.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<DeferredSpawnEggItem> LILITH_SPAWN_EGG = ITEMS.register("lilith_spawn_egg",
            () -> new DeferredSpawnEggItem(AllEntities.LILITH, 0xe8dcc0, 0xffffff, new Item.Properties().rarity(Rarity.RARE)));

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

    private static DeferredItem<Item> lore(String id, Item.Properties props) {
        return ITEMS.register(id, () -> new org.papiricoh.supernaturalcraft.reward.LoreItem(props));
    }

    private static DeferredItem<Item> ring(String id) {
        return lore(id, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                .component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    }

    private static DeferredItem<BlockItem> blockItem(DeferredBlock<?> block) {
        return ITEMS.registerSimpleBlockItem(block);
    }

    public static void init() {
    }
}
