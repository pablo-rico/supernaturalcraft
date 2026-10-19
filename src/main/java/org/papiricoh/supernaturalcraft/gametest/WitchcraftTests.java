package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.allegiance.power.PowerCaster;
import org.papiricoh.supernaturalcraft.bowl.BowlContents;
import org.papiricoh.supernaturalcraft.bowl.BowlInput;
import org.papiricoh.supernaturalcraft.bowl.BowlLiquid;
import org.papiricoh.supernaturalcraft.bowl.BowlSpellRecipe;
import org.papiricoh.supernaturalcraft.bowl.Dose;
import org.papiricoh.supernaturalcraft.bowl.SpellBowlBlockEntity;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlCast;
import org.papiricoh.supernaturalcraft.bowl.spell.BoundBloodItem;
import org.papiricoh.supernaturalcraft.bowl.spell.Taglock;
import org.papiricoh.supernaturalcraft.bowl.witch.ControlEffects;
import org.papiricoh.supernaturalcraft.bowl.witch.CurseEffect;
import org.papiricoh.supernaturalcraft.bowl.witch.Maledictions;
import org.papiricoh.supernaturalcraft.bowl.witch.NameMagic;
import org.papiricoh.supernaturalcraft.entity.legacy.LegacyWeapons;
import org.papiricoh.supernaturalcraft.entity.magic.SpellCarrier;
import org.papiricoh.supernaturalcraft.entity.magic.WardEntity;
import org.papiricoh.supernaturalcraft.hex.CurseEffects;
import org.papiricoh.supernaturalcraft.hex.CurseEvents;
import org.papiricoh.supernaturalcraft.hex.HexHooks;
import org.papiricoh.supernaturalcraft.magic.item.GrimoireItem;
import org.papiricoh.supernaturalcraft.magic.item.HexedInkItem;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.mastery.Mastery;
import org.papiricoh.supernaturalcraft.magic.mastery.MasteryRules;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellCaster;
import org.papiricoh.supernaturalcraft.magic.spell.SpellContext;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllRecipes;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import org.papiricoh.supernaturalcraft.ritual.PatternGeometry;
import org.papiricoh.supernaturalcraft.ritual.block.RitualAltarBlockEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The witchcraft of v0.21 (P1b): the bowl's curses and how they land (Ward of the Name, Mirror Hex, bosses, PvP), whom a bowl
 * names, Break Hex, Banish by Name, the distillates (bound blood, hexed ink, chrism), familiars, the curse effects (Agony, Rot,
 * Silence, Silver Edge), glyphs, Linger and Quicken, hexed ink on a grimoire page, a Ward that takes a rite's backlash, and the
 * roadmap milestones (first targeted spell, mastery ranks).
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class WitchcraftTests {

    private static final BlockPos BOWL = new BlockPos(2, 1, 2);
    private static final BlockPos STAND = new BlockPos(3, 1, 2);

    private static ResourceLocation id(String path) {
        return SupernaturalCraft.asResource(path);
    }

    private static Spell spell(String form, List<String> effects, List<String> modifiers) {
        return new Spell(Optional.of(id(form)), effects.stream().map(WitchcraftTests::id).toList(),
                modifiers.stream().map(WitchcraftTests::id).toList(), "");
    }

    private static Spell spell(String form, String... effects) {
        return spell(form, Arrays.asList(effects), List.of());
    }

    /** A FakePlayer at {@code at} (survival, human, 100 mana), looking at {@code facing} if given. */
    private static ServerPlayer witch(GameTestHelper helper, String name, BlockPos at, LivingEntity facing) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.setGameMode(GameType.SURVIVAL);
        p.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN);
        Vec3 v = helper.absoluteVec(at.getBottomCenter());
        p.moveTo(v.x, v.y, v.z, 0, 0);
        if (facing != null) p.lookAt(EntityAnchorArgument.Anchor.EYES, facing.getEyePosition());
        ManaManager.get(p).setMana(100);
        return p;
    }

    /** A real (non-fake) player standing in the level, who can earn advancements and be found by UUID; remove it when done. */
    private static JournalTests.Witness witness(GameTestHelper helper, BlockPos at) {
        JournalTests.Witness w = new JournalTests.Witness(helper.getLevel());
        Vec3 v = helper.absoluteVec(at.getBottomCenter());
        w.moveTo(v.x, v.y, v.z, 0, 0);
        helper.getLevel().addNewPlayer(w);
        try {
            var f = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            f.setAccessible(true);
            f.setInt(w, 0);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        ManaManager.get(w).setMana(100);
        return w;
    }

    private static void dismiss(GameTestHelper helper, ServerPlayer w) {
        helper.getLevel().removePlayerImmediately(w, Entity.RemovalReason.DISCARDED);
    }

    private static boolean done(ServerPlayer p, String advancement) {
        var adv = p.server.getAdvancements().get(id(advancement));
        if (adv == null) throw new IllegalStateException("no advancement " + advancement);
        return p.getAdvancements().getOrStartProgress(adv).isDone();
    }

    private static Mob zombie(GameTestHelper helper, BlockPos at) {
        Mob z = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, at);
        z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
        return z;
    }

    private static BowlSpellRecipe recipe(GameTestHelper helper, String path) {
        return helper.getLevel().getRecipeManager().byKey(id("bowl_spell/" + path))
                .map(h -> (BowlSpellRecipe) h.value())
                .orElseThrow(() -> new IllegalStateException("no bowl spell " + path));
    }

    private static ItemStack taglock(UUID owner, String name) {
        ItemStack t = new ItemStack(AllItems.TAGLOCK.get());
        t.set(org.papiricoh.supernaturalcraft.registry.AllDataComponents.TAGLOCK.get(), new Taglock(owner, name, Taglock.HAIR));
        return t;
    }

    private static BowlCast cast(GameTestHelper helper, ServerPlayer caster, BowlContents contents, BowlSpellRecipe recipe) {
        return new BowlCast(helper.getLevel(), helper.absolutePos(BOWL), caster, contents, recipe);
    }

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

    private static Optional<ResourceLocation> match(GameTestHelper helper, BowlContents contents) {
        return helper.getLevel().getRecipeManager().getRecipeFor(AllRecipes.BOWL_SPELL.get(), BowlInput.of(contents), helper.getLevel())
                .map(h -> h.id());
    }

    private static List<ItemStack> painMix() {
        return List.of(new ItemStack(Items.CACTUS), new ItemStack(Items.IRON_NUGGET), new ItemStack(Items.SPIDER_EYE));
    }

    // --- how a curse lands ----------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, batch = "witch_curses")
    public static void aCurseTakesHoldUnlessWardedOrMirrored(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ServerPlayer caster = CurseTests.mortal(helper, STAND, ItemStack.EMPTY);
        BowlSpellRecipe pain = recipe(helper, "curse_of_pain");
        helper.assertTrue(pain.requiresTarget(), "the Curse of Pain is aimed at someone");
        CurseEffect curse = (CurseEffect) pain.effect();
        helper.assertTrue(curse.curse() == CurseEffect.Curse.PAIN, "curse_of_pain lays " + curse.curse());
        BowlCast cast = cast(helper, caster, BowlContents.EMPTY, pain);

        Mob plain = zombie(helper, new BlockPos(0, 1, 0));
        helper.assertTrue(Maledictions.strike(cast, plain, curse::lay) == Maledictions.Outcome.CURSED, "an unprotected target is cursed");
        helper.assertTrue(plain.hasEffect(AllMobEffects.AGONY), "and suffers Agony");

        // A Ward of the Name turns it, and the caster pays.
        Mob warded = zombie(helper, new BlockPos(1, 1, 0));
        warded.addEffect(new MobEffectInstance(AllMobEffects.NAME_WARDED, 600));
        float before = caster.getHealth();
        Maledictions.Outcome o = Maledictions.strike(cast, warded, curse::lay);
        helper.assertTrue(o == Maledictions.Outcome.WARDED && o.spent(), "a warded target turns the curse, got " + o);
        helper.assertFalse(warded.hasEffect(AllMobEffects.AGONY), "the warded target was cursed anyway");
        helper.assertTrue(Math.abs(before - caster.getHealth() - Maledictions.BACKLASH) < 0.01f,
                "the ward should cost the caster " + Maledictions.BACKLASH + ": " + before + " → " + caster.getHealth());
        caster.invulnerableTime = 0;
        caster.setHealth(caster.getMaxHealth());

        // A Mirror Hex sends it back once, and breaks.
        Mob mirrored = zombie(helper, new BlockPos(2, 1, 0));
        mirrored.addEffect(new MobEffectInstance(AllMobEffects.MIRRORED, 600));
        o = Maledictions.strike(cast, mirrored, curse::lay);
        helper.assertTrue(o == Maledictions.Outcome.MIRRORED, "a mirrored target sends the curse back, got " + o);
        helper.assertFalse(mirrored.hasEffect(AllMobEffects.AGONY), "the mirrored target was cursed anyway");
        helper.assertFalse(mirrored.hasEffect(AllMobEffects.MIRRORED), "the mirror should break");
        helper.assertTrue(caster.hasEffect(AllMobEffects.AGONY), "the caster should get their own curse back");
        helper.assertTrue(Maledictions.strike(cast, mirrored, curse::lay) == Maledictions.Outcome.CURSED, "a broken mirror stops nothing");
        helper.assertTrue(mirrored.hasEffect(AllMobEffects.AGONY), "the second curse lands");

        // A mirror against a warded caster: nobody suffers.
        caster.removeEffect(AllMobEffects.AGONY);
        caster.addEffect(new MobEffectInstance(AllMobEffects.NAME_WARDED, 600));
        Mob mirror2 = zombie(helper, new BlockPos(3, 1, 0));
        mirror2.addEffect(new MobEffectInstance(AllMobEffects.MIRRORED, 600));
        helper.assertTrue(Maledictions.strike(cast, mirror2, curse::lay) == Maledictions.Outcome.MIRRORED, "the mirror still turns it");
        helper.assertFalse(caster.hasEffect(AllMobEffects.AGONY) || mirror2.hasEffect(AllMobEffects.AGONY), "a warded caster is spared their own curse");

        // Bosses are never cursed (and a refusal does not spend the bowl).
        LivingEntity boss = AllEntities.AZAZEL.get().create(helper.getLevel());
        helper.assertTrue(boss != null && NameMagic.boss(boss), "Azazel counts as a boss");
        helper.assertFalse(NameMagic.mayAfflict(caster, boss), "a boss may be afflicted");
        o = Maledictions.strike(cast, boss, curse::lay);
        helper.assertTrue(o == Maledictions.Outcome.REFUSED && !o.spent(), "a boss should refuse the curse, got " + o);
        helper.assertFalse(boss.hasEffect(AllMobEffects.AGONY), "the boss was cursed");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "witch_pvp")
    public static void withoutPvpOtherPlayersCannotBeCursed(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        MinecraftServer server = helper.getLevel().getServer();
        boolean was = server.isPvpAllowed();
        try {
            ServerPlayer caster = witch(helper, "sn-test-witch", STAND, null);
            ServerPlayer other = witch(helper, "sn-test-neighbour", new BlockPos(1, 1, 1), null);
            Mob zombie = zombie(helper, new BlockPos(0, 1, 3));
            CurseEffect silence = new CurseEffect(CurseEffect.Curse.SILENCE, 200);
            BowlCast cast = cast(helper, caster, BowlContents.EMPTY, recipe(helper, "curse_of_silence"));
            server.setPvpAllowed(false);
            helper.assertFalse(NameMagic.mayAfflict(caster, other), "PvP is off: another player is off limits");
            helper.assertTrue(NameMagic.mayAfflict(caster, caster) && NameMagic.mayAfflict(caster, zombie), "yourself and monsters are always fair game");
            helper.assertTrue(Maledictions.strike(cast, other, silence::lay) == Maledictions.Outcome.REFUSED, "the curse should be refused");
            helper.assertFalse(other.hasEffect(AllMobEffects.SILENCED), "the neighbour was silenced without PvP");
            server.setPvpAllowed(true);
            helper.assertTrue(Maledictions.strike(cast, other, silence::lay) == Maledictions.Outcome.CURSED, "with PvP the curse lands");
            helper.assertTrue(other.hasEffect(AllMobEffects.SILENCED), "the neighbour is silenced");
        } finally {
            server.setPvpAllowed(was);
        }
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "witch_curses")
    public static void eachCurseLaysItsMarksAndBreakHexLiftsThem(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ServerPlayer victim = witch(helper, "sn-test-victim", STAND, null);
        for (CurseEffect.Curse c : CurseEffect.Curse.values()) new CurseEffect(c, 400).lay(victim);
        for (var e : List.of(MobEffects.UNLUCK, AllMobEffects.JINXED, AllMobEffects.AGONY, MobEffects.WEAKNESS, MobEffects.DIG_SLOWDOWN,
                AllMobEffects.MARKED, AllMobEffects.SILENCED, MobEffects.HUNGER, MobEffects.POISON, AllMobEffects.ROT)) {
            helper.assertTrue(victim.hasEffect(e), "the curses should lay " + e.getRegisteredName());
        }
        // A later system's hex, freed through the hook.
        hookOnce();
        victim.getPersistentData().putBoolean(TEST_HEX, true);
        helper.assertTrue(ControlEffects.BreakHex.free(victim), "Break Hex found nothing to lift");
        for (var e : List.of(AllMobEffects.JINXED, AllMobEffects.AGONY, AllMobEffects.ROT, AllMobEffects.SILENCED, AllMobEffects.MARKED,
                MobEffects.UNLUCK)) {
            helper.assertFalse(victim.hasEffect(e), "Break Hex left " + e.getRegisteredName());
        }
        helper.assertFalse(victim.getPersistentData().getBoolean(TEST_HEX), "Break Hex should call the HexHooks");
        helper.assertFalse(ControlEffects.BreakHex.free(victim), "nothing is left to lift the second time");
        helper.succeed();
    }

    private static final String TEST_HEX = "sn_test_hexed";
    private static boolean hooked;

    private static synchronized void hookOnce() {
        if (hooked) return;
        hooked = true;
        HexHooks.onBreakHex(e -> {
            if (!e.getPersistentData().getBoolean(TEST_HEX)) return false;
            e.getPersistentData().remove(TEST_HEX);
            return true;
        });
    }

    // --- whom the bowl names --------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL)
    public static void theBowlFindsWhomItsComponentNames(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ServerPlayer caster = witch(helper, "sn-test-seeker", STAND, null);
        Mob zombie = zombie(helper, new BlockPos(2, 1, 4));
        BowlSpellRecipe bind = recipe(helper, "binding_of_the_name");
        BowlContents named = BowlContents.of(List.of(taglock(zombie.getUUID(), "Zed")), List.of());
        BowlCast c = cast(helper, caster, named, bind);
        NameMagic.Lookup anywhere = NameMagic.find(c, -1);
        helper.assertTrue(anywhere.ok() && anywhere.entity() == zombie, "the taglock should find the zombie: " + anywhere);
        helper.assertTrue(NameMagic.find(c, 0).ok(), "same world, any distance");
        helper.assertTrue(NameMagic.find(c, NameMagic.REACH).ok(), "within reach");
        helper.assertTrue(NameMagic.find(c, 1).found() == NameMagic.Found.TOO_FAR, "two blocks away is too far for a reach of one");
        helper.assertTrue(NameMagic.find(cast(helper, caster, BowlContents.EMPTY, bind), -1).found() == NameMagic.Found.NO_TARGET, "nothing names nobody");
        BowlCast stranger = cast(helper, caster, BowlContents.of(List.of(taglock(UUID.randomUUID(), "Nobody")), List.of()), bind);
        helper.assertTrue(NameMagic.find(stranger, -1).found() == NameMagic.Found.LOST, "a stranger's hair finds no one");
        // Poured blood names first; a vial of bound blood names its owner.
        BowlCast blood = cast(helper, caster, BowlContents.of(List.of(taglock(UUID.randomUUID(), "Nobody")),
                List.of(Dose.blood(zombie.getUUID(), "Zed"))), bind);
        helper.assertTrue(blood.target().map(t -> t.owner().equals(zombie.getUUID())).orElse(false), "poured blood is the target first");
        BowlCast vial = cast(helper, caster, BowlContents.of(List.of(BoundBloodItem.of(caster.getUUID(), "me")), List.of()), bind);
        helper.assertTrue(vial.target().map(t -> t.owner().equals(caster.getUUID())).orElse(false), "bound blood names its owner");
        zombie.discard();
        helper.assertTrue(NameMagic.find(c, -1).found() == NameMagic.Found.LOST, "the dead are lost to the bowl");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void banishingHoldsOnlyWhatIsNotOfThisWorld(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(ControlEffects.BanishByName.banishable(AllEntities.GHOST.get().create(level)), "a ghost can be laid to rest");
        helper.assertTrue(ControlEffects.BanishByName.banishable(AllEntities.BLACK_EYED_DEMON.get().create(level)), "a demon can be cast out");
        helper.assertFalse(ControlEffects.BanishByName.banishable(EntityType.ZOMBIE.create(level)), "a zombie is of this world");
        helper.assertFalse(ControlEffects.BanishByName.banishable(AllEntities.CROSSROADS_DEMON.get().create(level)), "a deal is broken only by death");
        helper.assertFalse(ControlEffects.BanishByName.banishable(AllEntities.AZAZEL.get().create(level)), "bosses are never banished");
        ServerPlayer human = witch(helper, "sn-test-human", STAND, null);
        ServerPlayer demon = witch(helper, "sn-test-demon", STAND, null);
        demon.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN.convert(Faction.DEMON));
        ServerPlayer angel = witch(helper, "sn-test-angel", STAND, null);
        angel.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN.convert(Faction.ANGEL));
        helper.assertFalse(ControlEffects.BanishByName.banishable(human), "a human is of this world");
        helper.assertTrue(ControlEffects.BanishByName.banishable(demon), "a demon player can be expelled");
        helper.assertTrue(ControlEffects.BanishByName.banishable(angel), "an angel player can be flung away");
        helper.succeed();
    }

    // --- distillates and familiars --------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL)
    public static void inkSteepsInTheSchoolOfItsSteeping(GameTestHelper helper) {
        helper.assertTrue(ControlEffects.Brew.steepSchool(new ItemStack(Items.GOLD_NUGGET)) == School.ENOCHIAN, "gold steeps Enochian");
        helper.assertTrue(ControlEffects.Brew.steepSchool(new ItemStack(Items.BLAZE_POWDER)) == School.INFERNAL, "blaze powder steeps Infernal");
        helper.assertTrue(ControlEffects.Brew.steepSchool(new ItemStack(Items.AMETHYST_SHARD)) == School.WITCHCRAFT, "amethyst steeps Witchcraft");
        helper.assertTrue(ControlEffects.Brew.steepSchool(new ItemStack(AllItems.SALT.get())) == School.HUNTER, "salt steeps Hunter");
        helper.assertTrue(ControlEffects.Brew.steepSchool(new ItemStack(Items.DIRT)).isNone(), "dirt steeps nothing");
        // The tag the recipe asks for and the code that reads it must agree.
        int n = 0;
        for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(AllTags.Items.INK_STEEPS)) {
            n++;
            helper.assertFalse(ControlEffects.Brew.steepSchool(new ItemStack(holder.value())).isNone(),
                    holder.getRegisteredName() + " is in #ink_steeps but steeps no school");
        }
        helper.assertTrue(n == 4, "one steep per school, found " + n);
        for (School s : List.of(School.ENOCHIAN, School.INFERNAL, School.WITCHCRAFT, School.HUNTER)) {
            helper.assertTrue(HexedInkItem.school(HexedInkItem.of(s)) == s, "hexed ink keeps its school " + s);
        }
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void onlyYourTamedCatOrParrotBecomesAFamiliar(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ServerPlayer witch = witch(helper, "sn-test-rowena", STAND, null);
        ServerPlayer other = witch(helper, "sn-test-other", STAND, null);
        Cat cat = helper.spawnWithNoFreeWill(EntityType.CAT, new BlockPos(0, 1, 0));
        Parrot parrot = helper.spawnWithNoFreeWill(EntityType.PARROT, new BlockPos(1, 1, 0));
        Wolf wolf = helper.spawnWithNoFreeWill(EntityType.WOLF, new BlockPos(2, 1, 0));
        Cat stray = helper.spawnWithNoFreeWill(EntityType.CAT, new BlockPos(3, 1, 0));
        Cat theirs = helper.spawnWithNoFreeWill(EntityType.CAT, new BlockPos(4, 1, 0));
        for (var pet : List.of(cat, parrot, wolf)) {
            pet.setTame(true, false);
            pet.setOwnerUUID(witch.getUUID());
        }
        theirs.setTame(true, false);
        theirs.setOwnerUUID(other.getUUID());
        helper.assertTrue(ControlEffects.FamiliarBond.eligible(cat, witch), "your cat");
        helper.assertTrue(ControlEffects.FamiliarBond.eligible(parrot, witch), "your parrot");
        helper.assertFalse(ControlEffects.FamiliarBond.eligible(wolf, witch), "a wolf is no familiar");
        helper.assertFalse(ControlEffects.FamiliarBond.eligible(stray, witch), "a stray is nobody's");
        helper.assertFalse(ControlEffects.FamiliarBond.eligible(theirs, witch), "someone else's cat");
        helper.assertFalse(ControlEffects.FamiliarBond.eligible(witch, witch), "a witch is not her own familiar");
        helper.succeed();
    }

    // --- the curses' effects ---------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL)
    public static void rotTurnsOneBiteOfFood(GameTestHelper helper) {
        ServerPlayer p = witch(helper, "sn-test-pantry", STAND, null);
        p.getInventory().clearContent();
        p.getInventory().setItem(0, new ItemStack(Items.ROTTEN_FLESH, 3));
        helper.assertFalse(CurseEffects.Rot.rotOne(p), "rotten flesh cannot rot further");
        p.getInventory().setItem(1, new ItemStack(Items.BREAD, 2));
        p.getInventory().setItem(2, new ItemStack(Items.DIAMOND));
        helper.assertTrue(CurseEffects.Rot.rotOne(p), "the bread should rot");
        helper.assertTrue(p.getInventory().countItem(Items.BREAD) == 1, "one bite rotted, not " + (2 - p.getInventory().countItem(Items.BREAD)));
        helper.assertTrue(p.getInventory().countItem(Items.ROTTEN_FLESH) == 4, "and became rotten flesh");
        helper.assertTrue(p.getInventory().countItem(Items.DIAMOND) == 1, "diamonds do not rot");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "witch_agony")
    public static void agonyHurtsButNeverKills(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ServerPlayer p = CurseTests.mortal(helper, STAND, ItemStack.EMPTY);
        CurseEffects.Agony agony = AllMobEffects.AGONY.get();
        helper.assertTrue(agony.shouldApplyEffectTickThisTick(CurseEffects.Agony.PULSE * 3, 0)
                && !agony.shouldApplyEffectTickThisTick(CurseEffects.Agony.PULSE * 3 + 1, 0), "Agony pulses every " + CurseEffects.Agony.PULSE);
        p.setHealth(20);
        agony.applyEffectTick(p, 0);
        helper.assertTrue(Math.abs(p.getHealth() - (20 - CurseEffects.Agony.DAMAGE)) < 0.01f, "a pulse hurts " + CurseEffects.Agony.DAMAGE + ": " + p.getHealth());
        for (float start : new float[]{2f, 1.2f, 1f}) {
            p.invulnerableTime = 0;
            p.setHealth(start);
            agony.applyEffectTick(p, 0);
            helper.assertTrue(p.isAlive() && p.getHealth() >= 1f - 0.01f, "Agony took " + start + " down to " + p.getHealth());
        }
        // Not even a Curse of Frailty (MARKED, +20 % harm) on top lets it kill.
        p.addEffect(new MobEffectInstance(AllMobEffects.MARKED, 200));
        for (float start : new float[]{2.5f, 1.5f, 1.05f}) {
            p.invulnerableTime = 0;
            p.setHealth(start);
            agony.applyEffectTick(p, 2);
            helper.assertTrue(p.isAlive() && p.getHealth() > 0, "a marked bearer died of Agony from " + start);
        }
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "witch_agony")
    public static void silverEdgeSpendsAChargePerBlow(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ServerPlayer p = witch(helper, "sn-test-hunter", STAND, null);
        Mob zombie = zombie(helper, new BlockPos(2, 1, 2));
        // The sigil gives three charges (amplifier 2).
        ResolvedSpell edge = ResolvedSpell.resolve(helper.getLevel().registryAccess(), null, spell("touch", "silver_edge"));
        helper.assertTrue(edge != null, "Silver Edge did not resolve");
        edge.applyTo(new SpellContext(helper.getLevel(), p), p);
        MobEffectInstance e = p.getEffect(AllMobEffects.SILVER_EDGE);
        helper.assertTrue(e != null && e.getAmplifier() == 2, "three charges expected: " + e);
        var blow = p.damageSources().playerAttack(p);
        helper.assertTrue(LegacyWeapons.silverEdged(blow) && LegacyWeapons.silver(blow), "a blow under Silver Edge counts as silver");
        Snowball thrown = new Snowball(helper.getLevel(), p);
        helper.assertFalse(LegacyWeapons.silverEdged(p.damageSources().thrown(thrown, p)), "only melee blows are edged");
        // Each landed blow spends one (through the damage event).
        for (int left = 1; left >= 0; left--) {
            zombie.invulnerableTime = 0;
            zombie.setHealth(zombie.getMaxHealth());
            helper.assertTrue(zombie.hurt(blow, 1f), "the blow did not land");
            MobEffectInstance now = p.getEffect(AllMobEffects.SILVER_EDGE);
            helper.assertTrue(now != null && now.getAmplifier() == left, "expected " + (left + 1) + " charges, have " + now);
        }
        CurseEvents.spendSilverEdge(p);
        helper.assertFalse(p.hasEffect(AllMobEffects.SILVER_EDGE), "the last charge should end it");
        helper.assertFalse(LegacyWeapons.silver(blow), "a bare fist with no charges is not silver");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void theCurseOfSilenceStopsSigilsAndPowers(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        Mob zombie = zombie(helper, new BlockPos(2, 1, 3));
        ServerPlayer p = witch(helper, "sn-test-silenced", new BlockPos(2, 1, 1), zombie);
        p.addEffect(new MobEffectInstance(AllMobEffects.SILENCED, 200));
        long cooldown = ManaManager.get(p).cooldownUntil();
        helper.assertTrue(SpellCaster.cast(p, spell("touch", "smite"), 1f) == SpellCaster.Result.SILENCED, "a silenced witch cast a sigil");
        helper.assertTrue(ManaManager.get(p).mana() == 100 && ManaManager.get(p).cooldownUntil() == cooldown, "a refused cast costs nothing");
        helper.assertTrue(PowerCaster.suppressed(p), "and her powers are suppressed");
        helper.assertTrue(ControlEffects.BreakHex.free(p), "Break Hex lifts the silence");
        helper.assertFalse(PowerCaster.suppressed(p), "the powers come back");
        helper.assertTrue(SpellCaster.cast(p, spell("touch", "smite"), 1f).success(), "and so do the sigils");
        helper.succeed();
    }

    // --- sigils: glyph, linger, quicken, hexed ink ----------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL)
    public static void aCasterKeepsAtMostThreeGlyphs(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = witch(helper, "sn-test-glyphs", STAND, null);
        ServerPlayer rival = witch(helper, "sn-test-rival", STAND, null);
        SpellCarrier carrier = SpellCarrier.of(spell("glyph", "hex"), new SpellContext(level, p));
        WardEntity[] mine = new WardEntity[3];
        for (int i = 0; i < 3; i++) {
            mine[i] = WardEntity.glyph(level, p, carrier, 600);
            Vec3 at = helper.absoluteVec(new BlockPos(i, 1, 0).getBottomCenter());
            mine[i].setPos(at.x, at.y, at.z);
            mine[i].tickCount = 300 - i * 100;
            level.addFreshEntity(mine[i]);
        }
        WardEntity theirs = WardEntity.glyph(level, rival, carrier, 600);
        theirs.setPos(helper.absoluteVec(new BlockPos(4, 1, 4).getBottomCenter()));
        theirs.tickCount = 999;
        level.addFreshEntity(theirs);
        helper.assertTrue(mine[0].isTrap() && mine[0].getRadius() >= WardEntity.GLYPH_RADIUS, "a glyph is a small trap");
        WardEntity.makeRoomForGlyph(level, p);
        helper.assertTrue(mine[0].isRemoved(), "the oldest glyph should give way");
        helper.assertTrue(!mine[1].isRemoved() && !mine[2].isRemoved(), "the newer glyphs stay");
        helper.assertFalse(theirs.isRemoved(), "another caster's glyph is not ours to remove");
        WardEntity.makeRoomForGlyph(level, p);
        helper.assertTrue(!mine[1].isRemoved() && !mine[2].isRemoved(), "with room for one more, nothing else goes");
        for (WardEntity w : mine) w.discard();
        theirs.discard();
        // Casting the form: four glyphs in a row leave three.
        ServerPlayer caster = witch(helper, "sn-test-glyph-caster", STAND, null);
        caster.setGameMode(GameType.CREATIVE);
        for (int i = 0; i < 4; i++) {
            helper.assertTrue(SpellCaster.cast(caster, spell("glyph", "hex"), 1f).success(), "glyph cast " + i + " failed");
        }
        long glyphs = level.getEntities(AllEntities.SIGIL_GLYPH.get(), w -> caster.getUUID().equals(w.ownerId()) && w.isAlive()).size();
        helper.assertTrue(glyphs == WardEntity.MAX_GLYPHS, "expected " + WardEntity.MAX_GLYPHS + " glyphs, found " + glyphs);
        level.getEntities(AllEntities.SIGIL_GLYPH.get(), w -> caster.getUUID().equals(w.ownerId())).forEach(Entity::discard);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "witch_glyph", timeoutTicks = 60)
    public static void aGlyphSpringsOnAFoeAndSparesAFriend(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ServerLevel level = helper.getLevel();
        JournalTests.Witness owner = witness(helper, new BlockPos(0, 1, 4));
        SpellCarrier carrier = SpellCarrier.of(spell("glyph", "hex"), new SpellContext(level, owner));
        WardEntity trap = WardEntity.glyph(level, owner, carrier, 600);
        trap.setPos(helper.absoluteVec(new BlockPos(3, 1, 1).getBottomCenter()));
        level.addFreshEntity(trap);
        WardEntity underFoot = WardEntity.glyph(level, owner, carrier, 600);
        underFoot.setPos(helper.absoluteVec(new BlockPos(0, 1, 4).getBottomCenter()));
        level.addFreshEntity(underFoot);
        Mob zombie = zombie(helper, new BlockPos(3, 1, 1));
        helper.runAfterDelay(20, () -> {
            try {
                helper.assertTrue(zombie.hasEffect(AllMobEffects.JINXED), "the glyph should have jinxed the zombie on it");
                helper.assertTrue(trap.isRemoved(), "a sprung glyph is spent");
                helper.assertFalse(underFoot.isRemoved(), "a glyph does not spring under its own caster");
                helper.assertFalse(owner.hasEffect(AllMobEffects.JINXED), "nor curse them");
            } finally {
                underFoot.discard();
                dismiss(helper, owner);
            }
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void quickenShortensTheWaitAndLingerLeavesAZone(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        var access = helper.getLevel().registryAccess();
        ResolvedSpell plain = ResolvedSpell.resolve(access, null, spell("touch", "smite"));
        ResolvedSpell quick = ResolvedSpell.resolve(access, null, spell("touch", List.of("smite"), List.of("quicken")));
        ResolvedSpell lingering = ResolvedSpell.resolve(access, null, spell("touch", List.of("smite"), List.of("linger")));
        ResolvedSpell both = ResolvedSpell.resolve(access, null, spell("touch", List.of("smite"), List.of("linger", "quicken")));
        helper.assertTrue(plain != null && quick != null && lingering != null && both != null, "a spell did not resolve");
        helper.assertTrue(plain.cooldown() > 0, "smite has a cooldown");
        helper.assertTrue(quick.cooldown() == Math.round(plain.cooldown() * 0.6f), "Quicken: " + plain.cooldown() + " → " + quick.cooldown());
        helper.assertTrue(both.cooldown() == Math.round(Math.max(plain.cooldown(), lingering.cooldown()) * 0.6f), "Quicken shortens Linger's wait too");
        helper.assertTrue(plain.lingerTicks() == 0 && quick.lingerTicks() == 0, "no zone without Linger");
        helper.assertTrue(lingering.lingerTicks() == 80, "Linger lasts 80 ticks, not " + lingering.lingerTicks());
        // The real cast: the wait it sets, and the zone it leaves.
        Mob a = zombie(helper, new BlockPos(1, 1, 3));
        Mob b = zombie(helper, new BlockPos(3, 1, 3));
        ServerPlayer quickCaster = witch(helper, "sn-test-quick", new BlockPos(1, 1, 1), a);
        ServerPlayer lingerCaster = witch(helper, "sn-test-linger", new BlockPos(3, 1, 1), b);
        long now = helper.getLevel().getGameTime();
        helper.assertTrue(SpellCaster.cast(quickCaster, spell("touch", List.of("smite"), List.of("quicken")), 1f).success(), "the quickened smite failed");
        long wait = ManaManager.get(quickCaster).cooldownUntil() - now;
        helper.assertTrue(wait == quick.cooldown(), "the cast should wait " + quick.cooldown() + " ticks, waits " + wait);
        helper.assertTrue(SpellCaster.cast(lingerCaster, spell("touch", List.of("smite"), List.of("linger")), 1f).success(), "the lingering smite failed");
        List<? extends WardEntity> zones = helper.getLevel().getEntities(AllEntities.LINGERING_ZONE.get(), w -> lingerCaster.getUUID().equals(w.ownerId()));
        helper.assertTrue(zones.size() == 1, "one lingering zone per cast, found " + zones.size());
        helper.assertTrue(zones.getFirst().getLifetime() == 80 && zones.getFirst().distanceToSqr(b) < 4, "the zone lasts 80 ticks where the smite landed");
        helper.assertTrue(helper.getLevel().getEntities(AllEntities.LINGERING_ZONE.get(), w -> quickCaster.getUUID().equals(w.ownerId())).isEmpty(),
                "a spell without Linger leaves no zone");
        zones.forEach(Entity::discard);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "witch_ink")
    public static void hexedInkStrengthensSpellsOfItsSchool(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ItemStack grimoire = new ItemStack(AllItems.GRIMOIRE.get());
        helper.assertTrue(GrimoireItem.inkOf(grimoire, 2).isNone(), "a fresh grimoire has no ink");
        GrimoireItem.setInk(grimoire, 2, School.ENOCHIAN);
        helper.assertTrue(GrimoireItem.inkOf(grimoire, 2) == School.ENOCHIAN && GrimoireItem.inkOf(grimoire, 0).isNone(), "page 3 is in Enochian ink");
        helper.assertTrue(GrimoireItem.inkOf(grimoire, 99).isNone() && GrimoireItem.inkOf(grimoire, -1).isNone(), "no page, no ink");
        GrimoireItem.setInk(grimoire, 2, School.NONE);
        helper.assertFalse(grimoire.has(org.papiricoh.supernaturalcraft.registry.AllDataComponents.PAGE_INKS.get()), "plain ink everywhere clears the component");
        // A smite (Enochian) from an Enochian page hits 15 % harder; from an Infernal page, no harder.
        School[] inks = {School.NONE, School.ENOCHIAN, School.INFERNAL};
        float[] dealt = new float[3];
        for (int i = 0; i < 3; i++) {
            Mob z = zombie(helper, new BlockPos(i * 2, 1, 3));
            ServerPlayer p = witch(helper, "sn-test-ink-" + i, new BlockPos(i * 2, 1, 1), z);
            float before = z.getHealth();
            helper.assertTrue(SpellCaster.cast(p, spell("touch", "smite"), 1f, inks[i]).success(), "the smite failed with " + inks[i]);
            dealt[i] = before - z.getHealth();
        }
        helper.assertTrue(dealt[0] > 0, "the smite did no harm");
        helper.assertTrue(Math.abs(dealt[1] - dealt[0] * SpellCaster.INK_POTENCY) < 0.05f, "Enochian ink: " + dealt[0] + " → " + dealt[1]);
        helper.assertTrue(Math.abs(dealt[2] - dealt[0]) < 0.01f, "another school's ink changes nothing: " + dealt[0] + " vs " + dealt[2]);
        helper.succeed();
    }

    // --- the bowl: targets, a curse cast, a brew into a rite --------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL)
    public static void aTargetedSpellWillNotCatchWithoutSomethingOfTheTarget(GameTestHelper helper) {
        SpellBowlBlockEntity bowl = bowl(helper);
        ServerPlayer p = witch(helper, "sn-test-hexer", STAND, null);
        Mob zombie = zombie(helper, new BlockPos(0, 1, 4));
        BowlContents bare = BowlContents.of(painMix(), List.of(Dose.of(BowlLiquid.DEMON_BLOOD)));
        helper.assertTrue(match(helper, bare).equals(Optional.of(id("bowl_spell/curse_of_pain"))), "the bare mix is still the Curse of Pain");
        bowl.setContents(bare);
        ManaManager.get(p).learnRite(id("curse_of_pain"));
        helper.assertTrue(light(bowl, p) == SpellBowlBlockEntity.LightResult.BLOCKED, "a curse with no one to curse should not catch");
        helper.assertTrue(ManaManager.get(p).mana() == 100 && !bowl.casting(), "and costs nothing");
        // Named blood stands in for the target; nameless blood spoils the mix; other spells take no extra blood.
        BowlContents named = BowlContents.of(painMix(), List.of(Dose.of(BowlLiquid.DEMON_BLOOD), Dose.blood(zombie.getUUID(), "Zed")));
        helper.assertTrue(BowlInput.of(named).namedBlood() == 1, "one dose names someone");
        helper.assertTrue(match(helper, named).equals(Optional.of(id("bowl_spell/curse_of_pain"))), "named blood should stand in: " + match(helper, named));
        BowlContents nameless = BowlContents.of(painMix(), List.of(Dose.of(BowlLiquid.DEMON_BLOOD), Dose.of(BowlLiquid.BLOOD)));
        helper.assertTrue(match(helper, nameless).isEmpty(), "nameless blood names no one: " + match(helper, nameless));
        BowlContents sight = BowlContents.of(List.of(new ItemStack(Items.SPIDER_EYE), new ItemStack(AllItems.ECTOPLASM.get()),
                new ItemStack(Items.AMETHYST_SHARD)), List.of(Dose.of(BowlLiquid.HOLY_WATER), Dose.blood(zombie.getUUID(), "Zed")));
        helper.assertTrue(match(helper, sight).isEmpty(), "an untargeted spell takes no stand-in blood: " + match(helper, sight));
        // A taglock is left out of the match and names the target.
        BowlContents tagged = BowlContents.of(List.of(new ItemStack(Items.CACTUS), taglock(zombie.getUUID(), "Zed"), new ItemStack(Items.IRON_NUGGET),
                new ItemStack(Items.SPIDER_EYE)), List.of(Dose.of(BowlLiquid.DEMON_BLOOD)));
        helper.assertTrue(match(helper, tagged).equals(Optional.of(id("bowl_spell/curse_of_pain"))), "the taglock spoiled the mix");
        bowl.setContents(tagged);
        helper.assertTrue(light(bowl, p) == SpellBowlBlockEntity.LightResult.LIT, "with the taglock the bowl catches");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "witch_cast", timeoutTicks = 60)
    public static void aCurseSpokenOverATaglockReachesItsTarget(GameTestHelper helper) {
        SpellBowlBlockEntity bowl = bowl(helper);
        JournalTests.Witness w = witness(helper, STAND);
        Mob zombie = zombie(helper, new BlockPos(0, 1, 4));
        try {
            bowl.setContents(BowlContents.of(List.of(new ItemStack(Items.CACTUS), new ItemStack(Items.IRON_NUGGET), new ItemStack(Items.SPIDER_EYE),
                    taglock(zombie.getUUID(), "Zed")), List.of(Dose.of(BowlLiquid.DEMON_BLOOD))));
            ManaManager.get(w).learnRite(id("curse_of_pain"));
            ManaManager.get(w).setMana(100);
            helper.assertFalse(done(w, "main/first_targeted_spell"), "the witness starts fresh");
            helper.assertTrue(light(bowl, w) == SpellBowlBlockEntity.LightResult.LIT, "the bowl did not catch");
        } catch (RuntimeException e) {
            dismiss(helper, w);
            throw e;
        }
        helper.runAfterDelay(15, () -> {
            try {
                SpellBowlBlockEntity.Outcome o = bowl.resolveRecitation(w, true, 0);
                helper.assertTrue(o == SpellBowlBlockEntity.Outcome.CAST, "expected the curse to work, got " + o);
                helper.assertTrue(zombie.hasEffect(AllMobEffects.AGONY), "the named zombie should suffer Agony");
                helper.assertFalse(w.hasEffect(AllMobEffects.AGONY), "the caster was cursed");
                helper.assertTrue(bowl.contents().isEmpty(), "the bowl was consumed");
                helper.assertTrue(done(w, "main/first_targeted_spell"), "the first targeted spell is a milestone");
                helper.assertTrue(done(w, "main/first_spell"), "and still the first spell");
            } finally {
                dismiss(helper, w);
            }
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.SMALL, batch = "witch_cast")
    public static void masteryRanksAreRoadmapMilestones(GameTestHelper helper) {
        JournalTests.Witness w = witness(helper, STAND);
        try {
            Mastery.set(w, School.INFERNAL, MasteryRules.Rank.ADEPT.xp);
            helper.assertTrue(done(w, "main/mastery_apprentice") && done(w, "main/mastery_adept"), "an Adept is past Apprentice and Adept");
            helper.assertFalse(done(w, "main/mastery_master"), "but not a Master");
            Mastery.set(w, School.HUNTER, MasteryRules.Rank.MASTER.xp);
            helper.assertTrue(done(w, "main/mastery_master"), "a Master of any school");
        } finally {
            dismiss(helper, w);
        }
        helper.succeed();
    }

    private static final BlockPos ALTAR = new BlockPos(5, 1, 5);
    private static final BlockPos FAR_BOWL = new BlockPos(1, 1, 5);

    @GameTest(template = SNGameTests.MEDIUM, batch = "witch_chrism", timeoutTicks = 320)
    public static void chrismFromTheBowlFeedsTheAngelBladeRite(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        helper.setBlock(ALTAR, AllBlocks.RITUAL_ALTAR.get().defaultBlockState());
        for (PatternGeometry.Cell c : PatternGeometry.cells(List.of("c###c", "#   #", "# A #", "#   #", "c###c"))) {
            helper.setBlock(ALTAR.offset(c.dx(), 0, c.dz()), c.symbol() == 'c'
                    ? Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true) : AllBlocks.CHALK_LINE.get().defaultBlockState());
        }
        helper.setBlock(FAR_BOWL, AllBlocks.SPELL_BOWL.get().defaultBlockState());
        SpellBowlBlockEntity bowl = (SpellBowlBlockEntity) helper.getBlockEntity(FAR_BOWL);
        RitualAltarBlockEntity altar = (RitualAltarBlockEntity) helper.getBlockEntity(ALTAR);
        ServerPlayer p = witch(helper, "sn-test-chrism", new BlockPos(1, 1, 4), null);
        BowlContents mix = BowlContents.of(List.of(new ItemStack(AllItems.HOLY_OIL.get()), new ItemStack(Items.GLISTERING_MELON_SLICE)),
                List.of(Dose.of(BowlLiquid.HOLY_WATER), Dose.of(BowlLiquid.HOLY_WATER)));
        helper.assertTrue(match(helper, mix).equals(Optional.of(id("bowl_spell/brew_chrism"))), "the mix should brew chrism: " + match(helper, mix));
        bowl.setContents(mix);
        ManaManager.get(p).learnRite(id("brew_chrism"));
        helper.assertTrue(light(bowl, p) == SpellBowlBlockEntity.LightResult.LIT, "the bowl did not catch");
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(12);
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(bowl.resolveRecitation(p, true, 0) == SpellBowlBlockEntity.Outcome.CAST, "the brew failed");
            List<ItemEntity> made = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, e -> e.getItem().is(AllItems.CHRISM.get()));
            helper.assertTrue(made.size() == 1 && made.getFirst().getItem().getCount() == 1, "one chrism should come out of the bowl, found " + made.size());
            ItemStack chrism = made.getFirst().getItem().copy();
            made.getFirst().discard();
            ManaManager.get(p).setMana(100);
            for (ItemStack s : List.of(new ItemStack(Items.DIAMOND_SWORD), chrism, new ItemStack(AllItems.HOLY_WATER.get()),
                    new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.GOLD_INGOT), new ItemStack(AllItems.HELLFIRE_EMBER.get()))) {
                p.setItemInHand(InteractionHand.MAIN_HAND, s);
                altar.onUse(p, InteractionHand.MAIN_HAND, s);
            }
            ItemStack flint = new ItemStack(Items.FLINT_AND_STEEL);
            p.setItemInHand(InteractionHand.MAIN_HAND, flint);
            altar.onUse(p, InteractionHand.MAIN_HAND, flint);
            helper.assertTrue(altar.isChanneling(), "the Angel Blade rite should take the bowl's chrism");
        });
        helper.succeedWhen(() -> helper.assertItemEntityPresent(AllItems.ANGEL_BLADE.get(), ALTAR.above(), 3.0));
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "witch_backlash", timeoutTicks = 100)
    public static void aWardInTheCircleTakesTheBacklash(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        helper.setBlock(ALTAR, AllBlocks.RITUAL_ALTAR.get().defaultBlockState());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) helper.setBlock(ALTAR.offset(dx, 0, dz), AllBlocks.CHALK_LINE.get().defaultBlockState());
            }
        }
        RitualAltarBlockEntity altar = (RitualAltarBlockEntity) helper.getBlockEntity(ALTAR);
        ServerLevel level = helper.getLevel();
        JournalTests.Witness w = witness(helper, new BlockPos(5, 1, 3));
        Vec3 top = Vec3.atBottomCenterOf(helper.absolutePos(ALTAR).above());
        WardEntity ward = new WardEntity(level, w, Spell.EMPTY, new SpellContext(level, w), 400);
        try {
            ward.setPos(top);
            level.addFreshEntity(ward);
            helper.assertTrue(WardEntity.shelteringWard(level, w.getUUID(), top) == ward, "the ward should cover the altar");
            helper.assertTrue(WardEntity.shelteringWard(level, UUID.randomUUID(), top) == null, "a stranger's rite is not sheltered");
            ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
            for (ItemStack s : List.of(water.copy(), water.copy(), water.copy(), new ItemStack(AllItems.SALT.get()), new ItemStack(Items.FLINT_AND_STEEL))) {
                w.setItemInHand(InteractionHand.MAIN_HAND, s);
                altar.onUse(w, InteractionHand.MAIN_HAND, s);
            }
            helper.assertTrue(altar.isChanneling(), "consecration did not start");
        } catch (RuntimeException e) {
            dismiss(helper, w);
            throw e;
        }
        float health = w.getHealth();
        helper.runAfterDelay(5, () -> helper.setBlock(ALTAR.east(), Blocks.AIR));
        helper.runAfterDelay(30, () -> {
            try {
                helper.assertFalse(altar.isChanneling(), "the rite kept going with a broken circle");
                helper.assertEntityNotPresent(AllEntities.BLACK_EYED_DEMON.get());
                helper.assertTrue(w.getHealth() == health, "the warded ritualist was burned: " + health + " → " + w.getHealth());
                helper.assertItemEntityNotPresent(AllItems.HOLY_WATER.get());
            } finally {
                ward.discard();
                dismiss(helper, w);
            }
            helper.succeed();
        });
    }
}
