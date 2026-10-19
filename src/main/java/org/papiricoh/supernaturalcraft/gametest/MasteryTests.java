package org.papiricoh.supernaturalcraft.gametest;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.bowl.BowlContents;
import org.papiricoh.supernaturalcraft.bowl.BowlInput;
import org.papiricoh.supernaturalcraft.bowl.BowlLiquid;
import org.papiricoh.supernaturalcraft.bowl.BowlSpellRecipe;
import org.papiricoh.supernaturalcraft.bowl.Dose;
import org.papiricoh.supernaturalcraft.bowl.SpellBowlBlockEntity;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlCast;
import org.papiricoh.supernaturalcraft.bowl.page.SpellPageItem;
import org.papiricoh.supernaturalcraft.bowl.spell.LocateEffect;
import org.papiricoh.supernaturalcraft.bowl.spell.Taglock;
import org.papiricoh.supernaturalcraft.bowl.spell.TaglockItem;
import org.papiricoh.supernaturalcraft.magic.ArcanaCost;
import org.papiricoh.supernaturalcraft.magic.item.SigilPageItem;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.mastery.Mastery;
import org.papiricoh.supernaturalcraft.magic.mastery.MasteryRules;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
import org.papiricoh.supernaturalcraft.magic.spell.SigilLookup;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellCaster;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllRecipes;
import org.papiricoh.supernaturalcraft.ritual.block.RitualAltarBlockEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The magic of v0.21 end to end: the Bind sigil renamed to Snare (old knowledge and old spells still work), the one discount
 * rule ({@link ArcanaCost}) on a ritual and a bowl cast, mastery XP and its rank-up mana, the learning gate on sigil and spell
 * pages, a taglock naming the bowl's target, and the Snare's short hold.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class MasteryTests {

    private static ResourceLocation id(String path) {
        return SupernaturalCraft.asResource(path);
    }

    private static Spell spell(String form, String... effects) {
        return new Spell(Optional.of(id(form)), Arrays.stream(effects).map(MasteryTests::id).toList(), List.of(), "");
    }

    /** A human FakePlayer at {@code at}, looking at {@code facing} if given, with 100 mana. */
    private static ServerPlayer hunter(GameTestHelper helper, String name, BlockPos at, LivingEntity facing) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.setGameMode(GameType.SURVIVAL);
        p.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN);
        Vec3 v = helper.absoluteVec(at.getBottomCenter());
        p.moveTo(v.x, v.y, v.z, 0, 0);
        if (facing != null) p.lookAt(EntityAnchorArgument.Anchor.EYES, facing.getEyePosition());
        ManaManager.get(p).setMana(100);
        return p;
    }

    private static float humanDiscount() {
        return (float) SNConfig.HUMAN_RITUAL_DISCOUNT.getAsDouble();
    }

    // --- Bind → Snare -------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL)
    public static void oldBindKnowledgeLoadsAsSnare(GameTestHelper helper) {
        // A player saved before v0.21 knew "bind".
        ArcanaData old = ArcanaData.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"known\": [\"supernaturalcraft:bind\", \"supernaturalcraft:smite\"]}")).getOrThrow();
        helper.assertTrue(old.known().contains(id("snare")), "the old Bind is known as the Snare: " + old.known());
        helper.assertFalse(old.known().contains(id("bind")), "the old id is gone from the list: " + old.known());
        helper.assertTrue(old.knows(id("bind")) && old.knows(id("snare")) && old.knows(id("smite")), "asked by either name, it is known");
        // Learning by the old name learns the new one, once.
        ArcanaData fresh = new ArcanaData();
        helper.assertTrue(fresh.learn(id("bind")), "learning Bind teaches something");
        helper.assertTrue(fresh.known().contains(id("snare")) && !fresh.known().contains(id("bind")), "it taught the Snare: " + fresh.known());
        helper.assertFalse(fresh.learn(id("snare")), "and the Snare is then already known");
        helper.assertTrue(SigilLookup.get(helper.getLevel().registryAccess(), null, id("bind")) != null, "the lookup answers to the old id");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void aSpellWrittenWithBindStillCastsAsTheSnare(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        LivingEntity demon = helper.spawnWithNoFreeWill(AllEntities.BLACK_EYED_DEMON.get(), new BlockPos(2, 1, 3));
        ServerPlayer p = hunter(helper, "sn-test-old-grimoire", new BlockPos(2, 1, 1), demon);
        // A grimoire page or formula written before v0.21 still says "bind".
        Spell old = spell("touch", "bind");
        ResolvedSpell resolved = ResolvedSpell.resolve(helper.getLevel().registryAccess(), old);
        helper.assertTrue(resolved != null, "touch+bind no longer resolves");
        helper.assertTrue(resolved.school() == School.HUNTER, "it is the hunters' Snare now, school " + resolved.school());
        helper.assertTrue(resolved.effects().getFirst().sigil().equals(SigilLookup.get(helper.getLevel().registryAccess(), null, id("snare"))),
                "the effect is the Snare's sigil");
        SpellCaster.Result r = SpellCaster.cast(p, old, 1f);
        helper.assertTrue(r.success(), "the old spell failed to cast: " + r);
        helper.assertTrue(demon.hasEffect(AllMobEffects.TRAPPED), "it held the demon as the Snare does");
        p.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, timeoutTicks = 80)
    public static void theSnareHoldsADemonForTwoSecondsAndRootsAnythingElse(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        LivingEntity demon = helper.spawnWithNoFreeWill(AllEntities.BLACK_EYED_DEMON.get(), new BlockPos(1, 1, 3));
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 3));
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
        ServerPlayer p = hunter(helper, "sn-test-snarer", new BlockPos(1, 1, 1), demon);
        helper.assertTrue(SpellCaster.cast(p, spell("touch", "snare"), 1f).success(), "snare on the demon failed");
        MobEffectInstance held = demon.getEffect(AllMobEffects.TRAPPED);
        helper.assertTrue(held != null && held.getDuration() > 0 && held.getDuration() <= 40, "the demon is trapped for 2 s: " + held);
        // Anything that is not a demon is rooted instead.
        ManaManager.get(p).setMana(100);
        ManaManager.get(p).setCooldownUntil(0);
        Vec3 v = helper.absoluteVec(new BlockPos(3, 1, 1).getBottomCenter());
        p.moveTo(v.x, v.y, v.z);
        p.lookAt(EntityAnchorArgument.Anchor.EYES, zombie.getEyePosition());
        helper.assertTrue(SpellCaster.cast(p, spell("touch", "snare"), 1f).success(), "snare on the zombie failed");
        MobEffectInstance root = zombie.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        helper.assertTrue(root != null && root.getAmplifier() == 7 && root.getDuration() <= 40, "the zombie is rooted (Slowness VIII, 2 s): " + root);
        helper.assertFalse(zombie.hasEffect(AllMobEffects.TRAPPED), "only demons are trapped");
        helper.runAfterDelay(45, () -> {
            helper.assertFalse(demon.hasEffect(AllMobEffects.TRAPPED), "the Snare is a breath, not a devil's trap: still held");
            helper.assertFalse(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the root outlasted 2 s");
            p.discard();
            helper.succeed();
        });
    }

    // --- mastery --------------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL)
    public static void castingTrainsTheSchoolAndARankAddsMana(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 3));
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
        ServerPlayer p = hunter(helper, "sn-test-student", new BlockPos(2, 1, 1), zombie);
        ArcanaData arcana = ManaManager.get(p);
        Mastery.set(p, School.ENOCHIAN, 149);
        float maxBefore = arcana.maxMana();
        helper.assertTrue(Mastery.get(p).rank(School.ENOCHIAN) == MasteryRules.Rank.NONE, "149 XP is no rank yet");
        helper.assertTrue(SpellCaster.cast(p, spell("touch", "smite"), 1f).success(), "the smite failed");
        float paid = 100 - arcana.mana();
        float xp = Mastery.get(p).xp(School.ENOCHIAN);
        helper.assertTrue(Math.abs(xp - (149 + MasteryRules.sigilXp(paid))) < 0.01f,
                "a sigil gives a fifth of its mana: expected " + (149 + paid / 5) + ", have " + xp);
        helper.assertTrue(Mastery.get(p).xp(School.HUNTER) == 0 && Mastery.get(p).xp(School.INFERNAL) == 0, "only Enochian trained");
        helper.assertTrue(Mastery.get(p).rank(School.ENOCHIAN) == MasteryRules.Rank.APPRENTICE, "the cast made an Apprentice");
        helper.assertTrue(Math.abs(arcana.maxMana() - (maxBefore + MasteryRules.Rank.APPRENTICE.manaBonus)) < 0.01f,
                "an Apprentice has 5 more max mana: " + maxBefore + " → " + arcana.maxMana());
        // Only the highest rank counts: an Adept in another school lifts it to +10, not +15.
        Mastery.set(p, School.INFERNAL, MasteryRules.Rank.ADEPT.xp);
        helper.assertTrue(Math.abs(arcana.maxMana() - (maxBefore + MasteryRules.Rank.ADEPT.manaBonus)) < 0.01f,
                "Apprentice and Adept give the Adept's +10: " + maxBefore + " → " + arcana.maxMana());
        // The rank discounts the school's own sigils (5 % for the Apprentice on top of the human's).
        helper.assertTrue(Math.abs(ArcanaCost.discount(p, ArcanaCost.Tool.SIGIL, School.ENOCHIAN)
                - ArcanaCost.multiplier(true, humanDiscount(), false, MasteryRules.Rank.APPRENTICE)) < 1e-4, "the Apprentice's discount");
        p.discard();
        helper.succeed();
    }

    // --- discounts ------------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = "mastery_altar", timeoutTicks = 240)
    public static void humanAmuletAndMasteryStackOnARite(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        BlockPos altarAt = new BlockPos(5, 1, 5);
        helper.setBlock(altarAt, AllBlocks.RITUAL_ALTAR.get().defaultBlockState());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) helper.setBlock(altarAt.offset(dx, 0, dz), AllBlocks.CHALK_LINE.get().defaultBlockState());
            }
        }
        RitualAltarBlockEntity altar = (RitualAltarBlockEntity) helper.getBlockEntity(altarAt);
        ServerPlayer p = hunter(helper, "sn-test-adept-ritualist", new BlockPos(3, 1, 5), null);
        p.getInventory().setItem(8, new ItemStack(AllItems.HUNTERS_AMULET.get()));
        Mastery.set(p, School.ENOCHIAN, MasteryRules.Rank.ADEPT.xp);
        ManaManager.get(p).setMana(100);
        for (ItemStack s : List.of(new ItemStack(AllItems.HOLY_WATER.get()), new ItemStack(AllItems.HOLY_WATER.get()), new ItemStack(AllItems.HOLY_WATER.get()),
                new ItemStack(AllItems.SALT.get()), new ItemStack(AllItems.SALT.get()), new ItemStack(Items.WHITE_CANDLE))) {
            p.setItemInHand(InteractionHand.MAIN_HAND, s);
            altar.onUse(p, InteractionHand.MAIN_HAND, s);
        }
        ItemStack flint = new ItemStack(Items.FLINT_AND_STEEL);
        p.setItemInHand(InteractionHand.MAIN_HAND, flint);
        altar.onUse(p, InteractionHand.MAIN_HAND, flint);
        helper.assertTrue(altar.isChanneling(), "Consecrate Ground (Enochian, 40 mana) started");
        float paid = 100 - ManaManager.get(p).mana();
        float multiplier = ArcanaCost.multiplier(true, humanDiscount(), true, MasteryRules.Rank.ADEPT);
        helper.assertTrue(Math.abs(multiplier - Math.max(1 - ArcanaCost.MAX_DISCOUNT, 1 - humanDiscount() - 0.10f - 0.10f)) < 1e-4,
                "human, amulet and Adept add up: " + multiplier);
        helper.assertTrue(Math.abs(paid - 40 * multiplier) < 0.01f, "expected to pay " + 40 * multiplier + " of 40, paid " + paid);
        // Mastery of another school does nothing for an Enochian rite.
        helper.assertTrue(ArcanaCost.discount(p, ArcanaCost.Tool.RITUAL, School.INFERNAL)
                > ArcanaCost.discount(p, ArcanaCost.Tool.RITUAL, School.ENOCHIAN) + 0.05f, "Adept in Enochian only");
        // Let the rite finish: a circle left burning when the test area is cleared would break.
        helper.succeedWhen(() -> helper.assertFalse(altar.isChanneling(), "still channelling"));
    }

    private static final BlockPos BOWL = new BlockPos(2, 1, 2);
    private static final BlockPos STAND = new BlockPos(3, 1, 2);

    private static SpellBowlBlockEntity bowl(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        helper.setBlock(BOWL, AllBlocks.SPELL_BOWL.get().defaultBlockState());
        return (SpellBowlBlockEntity) helper.getBlockEntity(BOWL);
    }

    private static SpellBowlBlockEntity.LightResult light(SpellBowlBlockEntity bowl, ServerPlayer p) {
        ItemStack flint = new ItemStack(Items.FLINT_AND_STEEL);
        p.setItemInHand(InteractionHand.MAIN_HAND, flint);
        return bowl.tryLight(p, flint, InteractionHand.MAIN_HAND);
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void humanAmuletAndMasteryStackOnABowl(GameTestHelper helper) {
        SpellBowlBlockEntity bowl = bowl(helper);
        ServerPlayer p = hunter(helper, "sn-test-adept-witch", STAND, null);
        p.getInventory().setItem(8, new ItemStack(AllItems.HUNTERS_AMULET.get()));
        Mastery.set(p, School.WITCHCRAFT, MasteryRules.Rank.ADEPT.xp);
        // Second Sight: Witchcraft, 30 mana.
        bowl.setContents(BowlContents.of(List.of(new ItemStack(Items.SPIDER_EYE), new ItemStack(AllItems.ECTOPLASM.get()),
                new ItemStack(Items.AMETHYST_SHARD)), List.of(Dose.of(BowlLiquid.HOLY_WATER))));
        var holder = helper.getLevel().getRecipeManager().getRecipeFor(AllRecipes.BOWL_SPELL.get(), BowlInput.of(bowl.contents()), helper.getLevel())
                .orElseThrow(() -> new IllegalStateException("no recipe for the Second Sight mix"));
        BowlSpellRecipe recipe = holder.value();
        helper.assertTrue(recipe.school() == School.WITCHCRAFT, "Second Sight is Witchcraft, not " + recipe.school());
        ManaManager.get(p).learnRite(recipe.spell(holder.id()));
        ManaManager.get(p).setMana(100);
        helper.assertTrue(light(bowl, p) == SpellBowlBlockEntity.LightResult.LIT, "the bowl did not catch");
        float paid = 100 - ManaManager.get(p).mana();
        float multiplier = ArcanaCost.multiplier(true, humanDiscount(), true, MasteryRules.Rank.ADEPT);
        helper.assertTrue(Math.abs(multiplier - Math.max(1 - ArcanaCost.MAX_DISCOUNT, 1 - humanDiscount() - 0.10f - 0.10f)) < 1e-4,
                "human, amulet and Adept add up: " + multiplier);
        float expected = recipe.manaCost() * multiplier;
        helper.assertTrue(Math.abs(paid - expected) < 0.01f, "expected to pay " + expected + " of " + recipe.manaCost() + ", paid " + paid);
        helper.succeed();
    }

    // --- the learning gate ----------------------------------------------------------------------------------------------

    private static boolean read(ServerPlayer p, ItemStack page) {
        p.setItemInHand(InteractionHand.MAIN_HAND, page);
        return page.getItem().use(p.level(), p, InteractionHand.MAIN_HAND).getResult().consumesAction();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void hardPagesWaitForMastery(GameTestHelper helper) {
        ServerPlayer p = hunter(helper, "sn-test-novice", STAND, null);
        ArcanaData arcana = ManaManager.get(p);
        // The Snare (Hunter, tier 2), from a page that still names the old Bind.
        helper.assertFalse(read(p, SigilPageItem.of(AllItems.SIGIL_PAGE.get(), id("bind"))), "a novice read a tier-2 page");
        helper.assertFalse(arcana.knows(id("snare")), "and learned it");
        Mastery.set(p, School.ENOCHIAN, MasteryRules.Rank.MASTER.xp);
        helper.assertFalse(read(p, SigilPageItem.of(AllItems.SIGIL_PAGE.get(), id("snare"))), "mastery of another school opened a Hunter page");
        Mastery.set(p, School.HUNTER, MasteryRules.Rank.APPRENTICE.xp);
        helper.assertTrue(read(p, SigilPageItem.of(AllItems.SIGIL_PAGE.get(), id("bind"))), "an Apprentice Hunter could not read the page");
        helper.assertTrue(arcana.knows(id("snare")) && arcana.known().contains(id("snare")), "the page taught the Snare");
        // A bowl spell page (Binding and Banishing: Witchcraft, tier 2).
        ItemStack bowlPage = SpellPageItem.of(id("bind_banish"));
        helper.assertFalse(read(p, bowlPage), "a novice witch read a tier-2 spell page");
        helper.assertFalse(arcana.knowsRite(id("bind_banish")), "and learned it");
        Mastery.set(p, School.WITCHCRAFT, MasteryRules.Rank.APPRENTICE.xp);
        helper.assertTrue(read(p, SpellPageItem.of(id("bind_banish"))), "an Apprentice witch could not read it");
        helper.assertTrue(arcana.knowsRite(id("bind_banish")), "the spell page taught Binding and Banishing");
        // Morningstar's Spark counts as Adept: Echo (tier 3, no school) for a player with no mastery at all.
        ServerPlayer sparked = hunter(helper, "sn-test-sparked", STAND, null);
        helper.assertFalse(read(sparked, SigilPageItem.of(AllItems.SIGIL_PAGE.get(), id("echo"))), "Echo read with no rank and no Spark");
        ManaManager.get(sparked).setGrace(true);
        helper.assertTrue(read(sparked, SigilPageItem.of(AllItems.SIGIL_PAGE.get(), id("echo"))), "Morningstar's Spark did not open Echo");
        // Creative players read anything.
        ServerPlayer creative = hunter(helper, "sn-test-creative", STAND, null);
        creative.setGameMode(GameType.CREATIVE);
        helper.assertTrue(read(creative, SigilPageItem.of(AllItems.SIGIL_PAGE.get(), id("ward"))), "creative mode is gated");
        helper.assertTrue(ManaManager.get(creative).knows(id("ward")), "the creative player learned Ward");
        helper.succeed();
    }

    // --- the taglock ----------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, timeoutTicks = 60)
    public static void aTaglockNamesWhomTheLocatingSpellSeeks(GameTestHelper helper) {
        SpellBowlBlockEntity bowl = bowl(helper);
        ServerPlayer p = hunter(helper, "sn-test-hair", STAND, null);
        // Sneak-use: a lock of your own hair.
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AllItems.TAGLOCK.get(), 2));
        TaglockItem.snip(p, InteractionHand.MAIN_HAND, p, false);
        ItemStack lock = p.getInventory().items.stream().filter(TaglockItem::filled).findFirst().orElse(ItemStack.EMPTY);
        helper.assertTrue(!lock.isEmpty(), "no filled taglock after the snip");
        helper.assertTrue(p.getMainHandItem().getCount() == 1, "one blank taglock was used");
        Taglock tag = lock.get(org.papiricoh.supernaturalcraft.registry.AllDataComponents.TAGLOCK.get());
        helper.assertTrue(tag != null && tag.owner().equals(p.getUUID()) && Taglock.HAIR.equals(tag.kind()), "the hair is the snipper's: " + tag);
        // Locate a person with only water poured in: the taglock names the target, and does not spoil the recipe.
        List<ItemStack> items = List.of(new ItemStack(Items.BONE), new ItemStack(Items.COMPASS), new ItemStack(AllItems.SALT.get()), lock.copy());
        List<Dose> liquids = List.of(Dose.of(BowlLiquid.WATER));
        bowl.setContents(BowlContents.of(items, liquids));
        var holder = helper.getLevel().getRecipeManager().getRecipeFor(AllRecipes.BOWL_SPELL.get(), BowlInput.of(bowl.contents()), helper.getLevel());
        helper.assertTrue(holder.isPresent() && holder.get().id().equals(id("bowl_spell/locate_player")),
                "the taglock kept the mix from matching Locate: " + holder.map(h -> h.id().toString()).orElse("nothing"));
        BowlSpellRecipe recipe = holder.get().value();
        BowlCast probe = new BowlCast(helper.getLevel(), helper.absolutePos(BOWL), p, bowl.contents(), recipe);
        helper.assertTrue(probe.target().map(t -> t.owner().equals(p.getUUID())).orElse(false), "the bowl's target is the taglock's owner");
        LocateEffect effect = (LocateEffect) recipe.effect();
        helper.assertTrue(effect.precheck(probe) == null, "with a taglock the spell needs no named blood");
        // A stranger's hair leads nowhere.
        ItemStack stranger = lock.copy();
        stranger.set(org.papiricoh.supernaturalcraft.registry.AllDataComponents.TAGLOCK.get(), new Taglock(UUID.randomUUID(), "Nobody", Taglock.HAIR));
        BowlCast lost = new BowlCast(helper.getLevel(), helper.absolutePos(BOWL), p,
                BowlContents.of(List.of(new ItemStack(Items.BONE), new ItemStack(Items.COMPASS), new ItemStack(AllItems.SALT.get()), stranger), liquids), recipe);
        helper.assertTrue(effect.resolve(lost).kind() == LocateEffect.Located.Kind.LOST, "a stranger's hair found someone");
        // The real casting: light, recite, and the smoke goes to the caster; the bowl trains Witchcraft at twice a sigil's rate.
        ManaManager.get(p).learnRite(id("locate"));
        ManaManager.get(p).setMana(100);
        helper.assertTrue(light(bowl, p) == SpellBowlBlockEntity.LightResult.LIT, "the bowl did not catch");
        float paid = 100 - ManaManager.get(p).mana();
        helper.runAfterDelay(15, () -> {
            SpellBowlBlockEntity.Outcome o = bowl.resolveRecitation(p, true, 0);
            helper.assertTrue(o == SpellBowlBlockEntity.Outcome.CAST, "expected the spell to work, got " + o);
            float xp = Mastery.get(p).xp(School.WITCHCRAFT);
            helper.assertTrue(Math.abs(xp - MasteryRules.bowlXp(paid)) < 0.01f, "the bowl gave " + xp + " Witchcraft XP, expected " + MasteryRules.bowlXp(paid));
            helper.assertTrue(bowl.contents().isEmpty(), "the bowl was consumed");
            helper.succeed();
        });
    }
}
