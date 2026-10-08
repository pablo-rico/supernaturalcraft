package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.BowlContents;
import org.papiricoh.supernaturalcraft.bowl.BowlLiquid;
import org.papiricoh.supernaturalcraft.bowl.BowlSpellRecipe;
import org.papiricoh.supernaturalcraft.bowl.Dose;
import org.papiricoh.supernaturalcraft.bowl.SpellBowlBlockEntity;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlCast;
import org.papiricoh.supernaturalcraft.bowl.spell.BanishEffect;
import org.papiricoh.supernaturalcraft.bowl.spell.BindEffect;
import org.papiricoh.supernaturalcraft.bowl.spell.Binding;
import org.papiricoh.supernaturalcraft.bowl.spell.Bindings;
import org.papiricoh.supernaturalcraft.bowl.spell.BloodSample;
import org.papiricoh.supernaturalcraft.bowl.spell.Concealment;
import org.papiricoh.supernaturalcraft.bowl.spell.LocateEffect;
import org.papiricoh.supernaturalcraft.bowl.spell.PetBond;
import org.papiricoh.supernaturalcraft.bowl.spell.PetCollarItem;
import org.papiricoh.supernaturalcraft.bowl.spell.PetLedger;
import org.papiricoh.supernaturalcraft.bowl.spell.PurifyEffect;
import org.papiricoh.supernaturalcraft.bowl.spell.RevivePetEffect;
import org.papiricoh.supernaturalcraft.crossroads.CrossroadsHooks;
import org.papiricoh.supernaturalcraft.entity.demon.BlackEyedDemon;
import org.papiricoh.supernaturalcraft.entity.ghost.GhostEntity;
import org.papiricoh.supernaturalcraft.entity.hellhound.HellhoundEntity;
import org.papiricoh.supernaturalcraft.grave.GraveBonesBlock;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The bowl spells: blood vials, collars and the pet ledger, Locating, Concealment, Purification,
 * Binding, Banishing and Revive Pet. Effects are driven through a {@link BowlCast} built from the
 * real recipe; Purification also goes through the bowl itself (light, recite). Spells that reach out
 * over an area each run in their own batch, so they cannot touch a neighbouring test's creatures.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class SpellTests {

    private static final String BATCH = "spells";
    private static final BlockPos BOWL = new BlockPos(5, 1, 5);
    private static final BlockPos STAND = new BlockPos(6, 1, 5);

    // --- helpers ------------------------------------------------------------------------------

    private static ServerPlayer player(GameTestHelper helper, String name) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.setGameMode(GameType.SURVIVAL);
        Vec3 v = helper.absoluteVec(STAND.getBottomCenter());
        p.moveTo(v.x, v.y, v.z, 0, 0);
        return p;
    }

    private static BowlSpellRecipe recipe(GameTestHelper helper, String name) {
        ResourceLocation id = SupernaturalCraft.asResource("bowl_spell/" + name);
        return helper.getLevel().getRecipeManager().byKey(id)
                .map(h -> h.value() instanceof BowlSpellRecipe r ? r : null)
                .orElseThrow(() -> new IllegalStateException("no bowl spell recipe " + id));
    }

    private static BowlCast cast(GameTestHelper helper, ServerPlayer caster, BowlSpellRecipe recipe, List<ItemStack> items, List<Dose> liquids) {
        return new BowlCast(helper.getLevel(), helper.absolutePos(BOWL), caster, BowlContents.of(items, liquids), recipe);
    }

    private static Wolf tamedWolf(GameTestHelper helper, ServerPlayer owner, BlockPos at, String name) {
        Wolf wolf = helper.spawn(EntityType.WOLF, at);
        wolf.setNoAi(true);
        wolf.setTame(true, true);
        wolf.setOwnerUUID(owner.getUUID());
        if (name != null) wolf.setCustomName(Component.literal(name));
        return wolf;
    }

    private static ItemStack collar(Wolf wolf, ServerPlayer owner) {
        ItemStack collar = new ItemStack(AllItems.PET_COLLAR.get());
        PetCollarItem.BindResult r = PetCollarItem.bind(collar, owner, wolf);
        if (r != PetCollarItem.BindResult.BOUND) throw new IllegalStateException("could not collar the wolf: " + r);
        return collar;
    }

    private static PetLedger ledger(GameTestHelper helper) {
        return PetLedger.get(helper.getLevel().getServer());
    }

    // --- recipes ------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, batch = BATCH)
    public static void theSpellRecipesLoadWithTheirEffects(GameTestHelper helper) {
        Map<String, Class<?>> expected = Map.of(
                "locate_player", LocateEffect.class, "locate_pet", LocateEffect.class, "locate_spire", LocateEffect.class,
                "locate_grave", LocateEffect.class, "purification", PurifyEffect.class, "bind", BindEffect.class,
                "banish", BanishEffect.class, "revive_pet", RevivePetEffect.class);
        expected.forEach((name, type) -> {
            BowlSpellRecipe r = recipe(helper, name);
            helper.assertTrue(type.isInstance(r.effect()), name + " has a " + r.effect().getClass().getSimpleName());
        });
        helper.assertTrue(((LocateEffect) recipe(helper, "locate_pet").effect()).target() == LocateEffect.Target.PET, "locate_pet seeks a pet");
        helper.assertTrue(((LocateEffect) recipe(helper, "locate_spire").effect()).structure().isPresent(), "locate_spire names its structures");
        helper.assertTrue(((BindEffect) recipe(helper, "bind").effect()).holdRadius() == 4, "bind holds within 4");
        helper.succeed();
    }

    // --- blood vials --------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, batch = BATCH)
    public static void aBottleDrawsAnotherPlayersBlood(GameTestHelper helper) {
        ServerPlayer drawer = player(helper, "sn-test-drawer");
        ServerPlayer victim = CurseTests.mortal(helper, new BlockPos(2, 1, 2), ItemStack.EMPTY);
        drawer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
        var use = new PlayerInteractEvent.EntityInteract(drawer, InteractionHand.MAIN_HAND, victim);
        NeoForge.EVENT_BUS.post(use);
        helper.assertTrue(use.isCanceled(), "the bottle's use on a player was taken over");
        ItemStack vial = drawer.getMainHandItem();
        BloodSample sample = vial.get(AllDataComponents.BLOOD_SAMPLE.get());
        helper.assertTrue(vial.is(AllItems.BLOOD_VIAL.get()) && sample != null && victim.getUUID().equals(sample.owner()),
                "the drawer holds a vial of the victim's blood, has " + vial + " " + sample);
        helper.assertTrue(victim.getHealth() <= 19.01f && victim.getHealth() >= 18.99f, "it cost the victim 1 health, has " + victim.getHealth());

        // Sneaking with a bottle in the air, a player bleeds into it themself.
        ServerPlayer self = CurseTests.mortal(helper, new BlockPos(1, 1, 1), new ItemStack(Items.GLASS_BOTTLE));
        self.setShiftKeyDown(true);
        var own = new PlayerInteractEvent.RightClickItem(self, InteractionHand.MAIN_HAND);
        NeoForge.EVENT_BUS.post(own);
        BloodSample mine = self.getMainHandItem().get(AllDataComponents.BLOOD_SAMPLE.get());
        helper.assertTrue(own.isCanceled() && mine != null && self.getUUID().equals(mine.owner()), "their own blood: " + self.getMainHandItem());
        helper.assertTrue(self.getHealth() <= 18.01f, "at 2 health, has " + self.getHealth());
        // Standing, the bottle is just a bottle.
        ServerPlayer plain = player(helper, "sn-test-plain");
        plain.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
        var notSneaking = new PlayerInteractEvent.RightClickItem(plain, InteractionHand.MAIN_HAND);
        NeoForge.EVENT_BUS.post(notSneaking);
        helper.assertFalse(notSneaking.isCanceled(), "a bottle used normally is left alone");
        helper.succeed();
    }

    // --- collars and the ledger ---------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void aCollarBindsOnlyYourOwnTamedPet(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer owner = player(helper, "sn-test-owner");
        ServerPlayer stranger = player(helper, "sn-test-stranger");
        Wolf wolf = tamedWolf(helper, owner, new BlockPos(3, 1, 3), "Rex");
        Wolf wild = helper.spawn(EntityType.WOLF, new BlockPos(7, 1, 7));
        wild.setNoAi(true);
        ItemStack c = new ItemStack(AllItems.PET_COLLAR.get());
        helper.assertTrue(PetCollarItem.bind(c, owner, wild) == PetCollarItem.BindResult.NOT_TAME, "a wild wolf will not wear it");
        helper.assertTrue(PetCollarItem.bind(c, stranger, wolf) == PetCollarItem.BindResult.NOT_YOURS, "nor someone else's");
        helper.assertTrue(!c.has(AllDataComponents.PET_BOND.get()), "the collar is still unbound");
        // Through the interaction event (before the wolf's own, which would sit it down).
        owner.setItemInHand(InteractionHand.MAIN_HAND, c);
        var use = new PlayerInteractEvent.EntityInteract(owner, InteractionHand.MAIN_HAND, wolf);
        NeoForge.EVENT_BUS.post(use);
        helper.assertTrue(use.isCanceled() && !wolf.isOrderedToSit(), "the collar went on instead of a sit order");
        PetBond bond = c.get(AllDataComponents.PET_BOND.get());
        helper.assertTrue(bond != null && bond.pet().equals(wolf.getUUID()) && bond.name().equals("Rex") && bond.owner().equals(owner.getUUID()),
                "bound to Rex: " + bond);
        helper.assertTrue(wolf.getData(AllAttachments.COLLARED), "Rex wears it");
        Optional<PetLedger.Entry> entry = ledger(helper).entry(wolf.getUUID());
        helper.assertTrue(entry.isPresent() && entry.get().alive() && entry.get().lastPos().equals(wolf.blockPosition()), "the ledger follows Rex: " + entry);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "spells_named_pet")
    public static void aNamedPetLeavesItsCollarAndTheDemonCanBringItBack(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer owner = player(helper, "sn-test-named");
        Wolf rex = tamedWolf(helper, owner, new BlockPos(3, 1, 3), "Rex");
        Wolf nameless = tamedWolf(helper, owner, new BlockPos(7, 1, 7), null);
        UUID rexId = rex.getUUID();
        rex.kill();
        nameless.kill();
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(12),
                i -> i.getItem().is(AllItems.PET_COLLAR.get()));
        helper.assertTrue(drops.size() == 1, "one collar on the ground, found " + drops.size());
        PetBond bond = drops.getFirst().getItem().get(AllDataComponents.PET_BOND.get());
        helper.assertTrue(bond != null && bond.pet().equals(rexId), "and it is Rex's: " + bond);
        Optional<PetLedger.Entry> entry = ledger(helper).entry(rexId);
        helper.assertTrue(entry.isPresent() && entry.get().revivable() && entry.get().snapshot().contains("CustomName"),
                "the ledger keeps Rex as he was: " + entry);
        helper.assertTrue(ledger(helper).entry(nameless.getUUID()).isEmpty(), "an unnamed, uncollared pet is not recorded");
        // The crossroads demon's "recover what was lost" uses the same ledger.
        helper.assertTrue(CrossroadsHooks.petRevival.available(owner), "the demon can offer Rex back");
        Vec3 at = helper.absoluteVec(new Vec3(5.5, 1, 5.5));
        helper.assertTrue(CrossroadsHooks.petRevival.revive(owner, at), "and does");
        Entity back = helper.getLevel().getEntity(rexId);
        helper.assertTrue(back instanceof Wolf w && w.isAlive() && owner.getUUID().equals(w.getOwnerUUID()), "Rex is back: " + back);
        helper.assertFalse(CrossroadsHooks.petRevival.available(owner), "and there is no one else to bring back");
        helper.succeed();
    }

    // --- locating -----------------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void locatingFindsACollaredWolf(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer owner = player(helper, "sn-test-seeker");
        Wolf wolf = tamedWolf(helper, owner, new BlockPos(1, 1, 9), "Rex");
        ItemStack collar = collar(wolf, owner);
        BowlCast cast = cast(helper, owner, recipe(helper, "locate_pet"),
                List.of(collar, new ItemStack(Items.BONE), new ItemStack(Items.COMPASS)), List.of(Dose.of(BowlLiquid.WATER)));
        LocateEffect effect = (LocateEffect) cast.recipe().effect();
        helper.assertTrue(effect.precheck(cast) == null, "the collar is in the bowl");
        LocateEffect.Located where = effect.resolve(cast);
        helper.assertTrue(where.kind() == LocateEffect.Located.Kind.FOUND && where.pos().distanceTo(wolf.position()) < 1.5,
                "found Rex where he stands: " + where + " vs " + wolf.position());
        helper.assertTrue(effect.perform(cast), "the spell worked");
        helper.assertTrue(cast.returned().size() == 1 && cast.returned().getFirst().is(AllItems.PET_COLLAR.get()), "the collar comes back: " + cast.returned());
        // Without its collar, the spell cannot even be lit.
        BowlCast bare = cast(helper, owner, cast.recipe(), List.of(new ItemStack(AllItems.PET_COLLAR.get()), new ItemStack(Items.BONE),
                new ItemStack(Items.COMPASS)), List.of(Dose.of(BowlLiquid.WATER)));
        helper.assertTrue(effect.precheck(bare) != null, "an unbound collar leads nowhere");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = BATCH)
    public static void locatingFollowsBlood(GameTestHelper helper) {
        ServerPlayer caster = player(helper, "sn-test-bleeder");
        BowlSpellRecipe recipe = recipe(helper, "locate_player");
        List<ItemStack> items = List.of(new ItemStack(Items.BONE), new ItemStack(Items.COMPASS), new ItemStack(AllItems.SALT.get()));
        BowlCast own = cast(helper, caster, recipe, items, List.of(Dose.of(BowlLiquid.WATER), Dose.blood(caster.getUUID(), "Bleeder")));
        LocateEffect effect = (LocateEffect) recipe.effect();
        LocateEffect.Located where = effect.resolve(own);
        helper.assertTrue(where.kind() == LocateEffect.Located.Kind.FOUND && where.pos().distanceTo(caster.position()) < 2, "the smoke finds the caster: " + where);
        helper.assertTrue(effect.perform(own), "and the spell works");
        BowlCast stranger = cast(helper, caster, recipe, items, List.of(Dose.of(BowlLiquid.WATER), Dose.blood(UUID.randomUUID(), "Nobody")));
        helper.assertTrue(effect.resolve(stranger).kind() == LocateEffect.Located.Kind.LOST, "the blood of no one here leads nowhere");
        helper.assertFalse(effect.perform(stranger), "and the bowl keeps its contents");
        BowlCast dry = cast(helper, caster, recipe, items, List.of(Dose.of(BowlLiquid.WATER), Dose.of(BowlLiquid.WATER)));
        helper.assertTrue(effect.precheck(dry) != null, "no blood, no spell");
        helper.succeed();
    }

    // --- concealment --------------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void concealmentHidesFromDemonsButNotFromBossesOrDebtCollectors(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer p = CurseTests.mortal(helper, STAND, ItemStack.EMPTY);
        BlackEyedDemon demon = helper.spawn(AllEntities.BLACK_EYED_DEMON.get(), new BlockPos(2, 1, 2));
        demon.setNoAi(true);
        demon.setTarget(p);
        helper.assertTrue(demon.getTarget() == p, "before the spell the demon hunts the player");
        p.addEffect(new MobEffectInstance(AllMobEffects.CONCEALED, 3600));
        helper.assertTrue(demon.getTarget() == null, "the spell taking hold makes it lose the player");
        demon.setTarget(p);
        helper.assertTrue(demon.getTarget() == null, "a concealed player cannot be targeted by a demon");
        helper.assertTrue(p.getVisibilityPercent(demon) == 0, "nor seen");
        // Bosses are never fooled (made, not spawned: no arena).
        var lilith = AllEntities.LILITH.get().create(helper.getLevel());
        helper.assertTrue(lilith != null && !Concealment.affects(lilith), "Lilith is a boss");
        lilith.setTarget(p);
        helper.assertTrue(lilith.getTarget() == p, "Lilith sees through it");
        lilith.discard();
        // A hound collecting a debt sees its quarry; any other hound does not.
        HellhoundEntity collector = helper.spawn(AllEntities.HELLHOUND.get(), new BlockPos(8, 1, 8));
        collector.setNoAi(true);
        collector.setQuarry(p);
        collector.setTarget(p);
        helper.assertTrue(collector.getTarget() == p, "the debt collector finds its quarry");
        HellhoundEntity stray = helper.spawn(AllEntities.HELLHOUND.get(), new BlockPos(8, 1, 2));
        stray.setNoAi(true);
        stray.setTarget(p);
        helper.assertTrue(stray.getTarget() == null, "a stray hound does not");
        // Striking a demon breaks the spell.
        demon.hurt(helper.getLevel().damageSources().playerAttack(p), 1);
        helper.assertFalse(p.hasEffect(AllMobEffects.CONCEALED), "attacking from hiding breaks the concealment");
        demon.setTarget(p);
        helper.assertTrue(demon.getTarget() == p, "and the demon can hunt again");
        helper.succeed();
    }

    // --- purification -------------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = "spells_purify")
    public static void purificationLiftsAfflictionsButNotASoullessDoom(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer p = CurseTests.mortal(helper, STAND, ItemStack.EMPTY);
        // A human can't be possessed at all (free will, v0.13): the possessed one here is sworn.
        AllegianceTests.swear(p, org.papiricoh.supernaturalcraft.allegiance.Faction.ANGEL, 1);
        p.addEffect(new MobEffectInstance(MobEffects.POISON, 600));
        p.addEffect(new MobEffectInstance(AllMobEffects.POSSESSED, 600));
        p.addEffect(new MobEffectInstance(AllMobEffects.MARKED, 600));
        p.addEffect(new MobEffectInstance(AllMobEffects.SOULLESS, 6000));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600));
        ManaManager.get(p).setSanity(20);
        p.getInventory().add(new ItemStack(AllItems.CURSE_BAG.get()));
        Husk husk = helper.spawn(EntityType.HUSK, new BlockPos(2, 1, 2));
        husk.setNoAi(true);
        husk.addEffect(new MobEffectInstance(AllMobEffects.POSSESSED, 600));
        GhostEntity ghost = helper.spawn(AllEntities.GHOST.get(), new BlockPos(8, 2, 8));
        ghost.setNoAi(true);
        helper.setBlock(new BlockPos(3, 1, 7), AllBlocks.CURSE_BAG.get().defaultBlockState());

        BowlSpellRecipe recipe = recipe(helper, "purification");
        BowlCast cast = cast(helper, p, recipe, List.of(), List.of());
        helper.assertTrue(recipe.effect().perform(cast), "the purification worked");
        helper.assertFalse(p.hasEffect(MobEffects.POISON) || p.hasEffect(AllMobEffects.POSSESSED) || p.hasEffect(AllMobEffects.MARKED),
                "poison, possession and the mark are lifted: " + p.getActiveEffects());
        helper.assertTrue(p.hasEffect(AllMobEffects.SOULLESS), "a collected soul is not the bowl's to cure");
        helper.assertTrue(p.hasEffect(MobEffects.MOVEMENT_SPEED), "blessings stay");
        helper.assertTrue(ManaManager.get(p).sanity() == 20 + PurifyEffect.SANITY_RESTORED, "sanity +40, has " + ManaManager.get(p).sanity());
        helper.assertTrue(p.getInventory().countItem(AllItems.CURSE_BAG.get()) == 0, "the bag in their pocket burned");
        helper.assertFalse(husk.hasEffect(AllMobEffects.POSSESSED), "the husk is itself again");
        helper.assertTrue(ghost.isDispersed(), "the ghost scattered");
        helper.assertBlockNotPresent(AllBlocks.CURSE_BAG.get(), new BlockPos(3, 1, 7));
        // Cast again over clean ground with a steady mind: nothing to do.
        ManaManager.get(p).setSanity(ArcanaData.MAX_SANITY);
        helper.assertFalse(recipe.effect().perform(cast(helper, p, recipe, List.of(), List.of())), "nothing here needs cleansing");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "spells_bowl", timeoutTicks = 80)
    public static void purificationThroughTheBowl(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, AllBlocks.SPELL_BOWL.get().defaultBlockState());
        SpellBowlBlockEntity bowl = (SpellBowlBlockEntity) helper.getBlockEntity(at);
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(3, 1, 2), ItemStack.EMPTY);
        AllegianceTests.swear(p, org.papiricoh.supernaturalcraft.allegiance.Faction.ANGEL, 1);
        p.addEffect(new MobEffectInstance(AllMobEffects.POSSESSED, 600));
        ManaManager.get(p).setMana(100);
        ManaManager.get(p).learnRite(SupernaturalCraft.asResource("purification"));
        bowl.setContents(BowlContents.of(List.of(new ItemStack(AllItems.SALT.get()), new ItemStack(AllItems.SALT.get()),
                new ItemStack(Items.LILY_OF_THE_VALLEY)), List.of(Dose.of(BowlLiquid.HOLY_WATER), Dose.of(BowlLiquid.HOLY_WATER))));
        ItemStack flint = new ItemStack(Items.FLINT_AND_STEEL);
        p.setItemInHand(InteractionHand.MAIN_HAND, flint);
        var lit = bowl.tryLight(p, flint, InteractionHand.MAIN_HAND);
        helper.assertTrue(lit == SpellBowlBlockEntity.LightResult.LIT, "the bowl caught: " + lit);
        helper.runAfterDelay(20, () -> {
            var outcome = bowl.resolveRecitation(p, true, 0);
            helper.assertTrue(outcome == SpellBowlBlockEntity.Outcome.CAST, "the words took hold: " + outcome);
            helper.assertFalse(p.hasEffect(AllMobEffects.POSSESSED), "the possession is gone");
            helper.assertTrue(bowl.contents().isEmpty(), "the bowl was spent");
            helper.succeed();
        });
    }

    // --- binding and banishing ----------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = "spells_bind", timeoutTicks = 100)
    public static void aBoundDemonIsHeldNearTheBowl(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        helper.setBlock(BOWL, AllBlocks.SPELL_BOWL.get().defaultBlockState());
        ServerPlayer p = player(helper, "sn-test-binder");
        BlackEyedDemon demon = helper.spawn(AllEntities.BLACK_EYED_DEMON.get(), new BlockPos(7, 1, 5));
        demon.setNoAi(true);
        BowlSpellRecipe recipe = recipe(helper, "bind");
        BowlCast cast = cast(helper, p, recipe, List.of(), List.of());
        helper.assertTrue(((BindEffect) recipe.effect()).choose(cast) == demon, "the demon is the one to bind");
        helper.assertTrue(recipe.effect().perform(cast), "the binding took");
        Optional<Binding> b = Bindings.of(demon);
        helper.assertTrue(b.isPresent() && b.get().anchor().equals(helper.absolutePos(BOWL)) && b.get().radius() == 4, "bound to the bowl: " + b);
        helper.assertTrue(demon.hasEffect(AllMobEffects.TRAPPED), "a bound demon is trapped");
        helper.assertFalse(((BindEffect) recipe.effect()).perform(cast(helper, p, recipe, List.of(), List.of())), "nothing else here to bind");
        // Carried off, it is pulled back to the edge.
        Vec3 away = helper.absoluteVec(new Vec3(10.5, 1, 10.5));
        demon.teleportTo(away.x, away.y, away.z);
        Vec3 anchor = Vec3.atBottomCenterOf(helper.absolutePos(BOWL));
        helper.runAfterDelay(25, () -> {
            double d = Math.hypot(demon.getX() - anchor.x, demon.getZ() - anchor.z);
            helper.assertTrue(d <= 4.01, "the demon was pulled back within 4 blocks, is " + d);
            helper.assertTrue(Bindings.of(demon).isPresent(), "still bound");
            helper.setBlock(BOWL, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        });
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(Bindings.of(demon).isEmpty(), "with the bowl gone the binding dropped");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "spells_banish")
    public static void banishingLaysGhostsToRestAndCastsOutDemons(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer p = player(helper, "sn-test-banisher");
        BlockPos bonesAt = new BlockPos(1, 1, 9);
        helper.setBlock(bonesAt, AllBlocks.GRAVE_BONES.get().defaultBlockState());
        GhostEntity ghost = helper.spawn(AllEntities.GHOST.get(), new BlockPos(2, 2, 8));
        ghost.setNoAi(true);
        ghost.setBones(helper.absolutePos(bonesAt));
        BlackEyedDemon demon = helper.spawn(AllEntities.BLACK_EYED_DEMON.get(), new BlockPos(8, 1, 3));
        demon.setNoAi(true);
        Husk husk = helper.spawn(EntityType.HUSK, new BlockPos(8, 1, 8));
        husk.setNoAi(true);
        BowlSpellRecipe recipe = recipe(helper, "banish");
        helper.assertTrue(recipe.effect().perform(cast(helper, p, recipe, List.of(), List.of())), "the banishing worked");
        helper.assertTrue(demon.isRemoved(), "the demon was cast out");
        helper.assertTrue(ghost.isFading(), "the ghost fades");
        helper.assertTrue(helper.getBlockState(bonesAt).getValue(GraveBonesBlock.RESTED), "its bones are at rest");
        helper.assertFalse(husk.isRemoved(), "a husk is nothing unclean");
        int drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(12)).size();
        helper.assertTrue(drops == 0, "nothing is left behind, found " + drops);
        helper.assertFalse(recipe.effect().perform(cast(helper, p, recipe, List.of(), List.of())), "nothing left to banish");
        helper.succeed();
    }

    // --- reviving a pet -----------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = "spells_revive")
    public static void reviveBringsBackACollaredWolf(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer owner = player(helper, "sn-test-mourner");
        Wolf wolf = tamedWolf(helper, owner, new BlockPos(2, 1, 2), "Rex");
        ItemStack collar = collar(wolf, owner);
        UUID id = wolf.getUUID();
        BowlSpellRecipe recipe = recipe(helper, "revive_pet");
        List<ItemStack> items = List.of(collar, new ItemStack(Items.BONE), new ItemStack(Items.GLISTERING_MELON_SLICE));
        List<Dose> liquids = List.of(Dose.of(BowlLiquid.WATER), Dose.of(BowlLiquid.HONEY));
        helper.assertTrue("message.supernaturalcraft.revive.alive".equals(recipe.effect().precheck(cast(helper, owner, recipe, items, liquids))),
                "a living pet cannot be revived");
        wolf.kill();
        Optional<PetLedger.Entry> dead = ledger(helper).entry(id);
        helper.assertTrue(dead.isPresent() && dead.get().revivable(), "the ledger keeps a snapshot of Rex: " + dead);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(12),
                i -> i.getItem().is(AllItems.PET_COLLAR.get())).isEmpty(), "a collared pet drops no second collar");
        BowlCast cast = cast(helper, owner, recipe, items, liquids);
        helper.assertTrue(recipe.effect().precheck(cast) == null, "Rex can be called back");
        helper.assertTrue(recipe.effect().perform(cast), "the spell worked");
        Entity back = helper.getLevel().getEntity(id);
        helper.assertTrue(back instanceof Wolf, "Rex is back: " + back);
        Wolf rex = (Wolf) back;
        helper.assertTrue(rex.isAlive() && rex.getHealth() == rex.getMaxHealth(), "whole: " + rex.getHealth());
        helper.assertTrue(owner.getUUID().equals(rex.getOwnerUUID()) && rex.isTame(), "and still the owner's");
        helper.assertTrue(rex.hasCustomName() && "Rex".equals(rex.getCustomName().getString()), "and still Rex");
        helper.assertTrue(rex.getData(AllAttachments.COLLARED), "wearing the collar");
        helper.assertTrue(rex.position().distanceTo(Vec3.atBottomCenterOf(helper.absolutePos(BOWL).above())) < 1, "beside the bowl");
        helper.assertTrue(cast.returned().size() == 1 && cast.returned().getFirst().is(AllItems.PET_COLLAR.get()), "the collar comes back");
        helper.assertTrue(ledger(helper).entry(id).map(PetLedger.Entry::alive).orElse(false), "the ledger knows Rex lives");
        helper.assertTrue(recipe.effect().precheck(cast(helper, owner, recipe, items, liquids)) != null, "and cannot bring him back twice");
        helper.succeed();
    }
}
