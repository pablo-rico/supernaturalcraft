package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.BowlContents;
import org.papiricoh.supernaturalcraft.bowl.BowlInput;
import org.papiricoh.supernaturalcraft.bowl.BowlLiquid;
import org.papiricoh.supernaturalcraft.bowl.BowlSpellRecipe;
import org.papiricoh.supernaturalcraft.bowl.Dose;
import org.papiricoh.supernaturalcraft.bowl.SpellBowlBlockEntity;
import org.papiricoh.supernaturalcraft.entity.demon.BlackEyedDemon;
import org.papiricoh.supernaturalcraft.hex.BleedingEffect;
import org.papiricoh.supernaturalcraft.hex.CurseBagBlockEntity;
import org.papiricoh.supernaturalcraft.hex.HexBags;
import org.papiricoh.supernaturalcraft.hex.ProtectionBagItem;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllRecipes;

import java.util.List;
import java.util.UUID;

/** Hex bags: whom a curse bag touches, how its bleeding grows, burning bags, slipping them, and the protection bag. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class HexTests {

    private static final BlockPos BAG = new BlockPos(2, 1, 2);

    private static ServerPlayer player(GameTestHelper helper, String name, BlockPos at) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.setGameMode(GameType.SURVIVAL);
        Vec3 v = helper.absoluteVec(at.getBottomCenter());
        p.moveTo(v.x, v.y, v.z, 0, 0);
        return p;
    }

    private static ServerPlayer warded(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = player(helper, "sn-test-warded", at);
        p.getInventory().add(HexBags.protectionBag(p.getUUID()));
        return p;
    }

    private static CurseBagBlockEntity bag(GameTestHelper helper, BlockPos at, UUID maker) {
        helper.setBlock(at, AllBlocks.CURSE_BAG.get().defaultBlockState());
        CurseBagBlockEntity be = (CurseBagBlockEntity) helper.getBlockEntity(at);
        be.setMaker(maker);
        return be;
    }

    private static boolean cursed(Player p) {
        return p.hasEffect(AllMobEffects.JINXED) || p.hasEffect(AllMobEffects.BLEEDING);
    }

    private static int curseBags(Player p) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(AllItems.CURSE_BAG.get())) n += p.getInventory().getItem(i).getCount();
        }
        return n;
    }

    private static int bleeding(Player p) {
        MobEffectInstance e = p.getEffect(AllMobEffects.BLEEDING);
        return e == null ? -1 : e.getAmplifier();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void aCurseBagJinxesStrangersButNotItsMakerOrTheWarded(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ServerPlayer maker = player(helper, "sn-test-maker", new BlockPos(1, 1, 1));
        ServerPlayer stranger = player(helper, "sn-test-stranger", new BlockPos(3, 1, 3));
        ServerPlayer holder = warded(helper, new BlockPos(1, 1, 3));
        CurseBagBlockEntity be = bag(helper, BAG, maker.getUUID());
        double luck = stranger.getAttributeValue(Attributes.LUCK);
        int hit = be.pulse(List.of(maker, stranger, holder), false);
        helper.assertTrue(hit == 1, "only the stranger is afflicted, hit " + hit);
        helper.assertTrue(stranger.hasEffect(AllMobEffects.JINXED) && stranger.hasEffect(AllMobEffects.BLEEDING), "the stranger is jinxed and bleeds");
        helper.assertTrue(stranger.getAttributeValue(Attributes.LUCK) < luck, "the jinx sours luck: " + stranger.getAttributeValue(Attributes.LUCK));
        helper.assertTrue(!cursed(maker), "the maker is spared");
        helper.assertTrue(!cursed(holder), "the protection bag wards its holder");
        // The bag's effects cannot be laid on a holder any other way either.
        holder.addEffect(new MobEffectInstance(AllMobEffects.JINXED, 100));
        helper.assertTrue(!holder.hasEffect(AllMobEffects.JINXED), "a holder is immune to the jinx");
        // Out of reach, nothing happens.
        ServerPlayer far = player(helper, "sn-test-far", new BlockPos(3, 1, 3));
        far.moveTo(far.getX() + 20, far.getY(), far.getZ());
        helper.assertTrue(be.pulse(List.of(far), false) == 0 && !cursed(far), "a curse bag reaches only " + HexBags.RADIUS + " blocks");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, timeoutTicks = 400)
    public static void bleedingGrowsWhileExposedAndEbbsAway(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ServerPlayer victim = CurseTests.mortal(helper, new BlockPos(3, 1, 3), ItemStack.EMPTY);
        CurseBagBlockEntity be = bag(helper, BAG, UUID.randomUUID());
        be.pulse(List.of(victim), false);
        helper.assertTrue(bleeding(victim) == 0, "first pulse: bleeding I, have " + bleeding(victim));
        for (int i = 0; i < 8; i++) be.pulse(List.of(victim), false);
        helper.assertTrue(bleeding(victim) == 2, "nine pulses: bleeding III, have " + bleeding(victim));
        float health = victim.getHealth();
        BleedingEffect.wound(helper.getLevel(), victim);
        helper.assertTrue(victim.getHealth() < health, "bleeding wounds: " + health + " -> " + victim.getHealth());
        int exposure = HexBags.exposure(victim);
        helper.runAfterDelay(260, () -> {
            victim.removeEffect(AllMobEffects.BLEEDING);
            be.pulse(List.of(victim), false);
            helper.assertTrue(HexBags.exposure(victim) < exposure, "exposure ebbs away: " + exposure + " -> " + HexBags.exposure(victim));
            helper.assertTrue(bleeding(victim) == 1, "back by the bag after a while: bleeding II, have " + bleeding(victim));
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.MEDIUM)
    public static void burningFindsBagsHiddenOrCarried(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        UUID maker = UUID.randomUUID();
        bag(helper, new BlockPos(3, 1, 3), maker);
        bag(helper, new BlockPos(6, 1, 5), maker);
        BlockPos chest = new BlockPos(7, 1, 7);
        helper.setBlock(chest, Blocks.CHEST.defaultBlockState());
        ((ChestBlockEntity) helper.getBlockEntity(chest)).setItem(4, HexBags.curseBag(maker));
        bag(helper, new BlockPos(10, 1, 10), maker);
        int burned = HexBags.burnNear(helper.getLevel(), helper.absolutePos(new BlockPos(5, 1, 5)), 4);
        helper.assertTrue(burned == 3, "two hidden bags and one in a chest burn, burned " + burned);
        helper.assertBlockNotPresent(AllBlocks.CURSE_BAG.get(), new BlockPos(3, 1, 3));
        helper.assertBlockNotPresent(AllBlocks.CURSE_BAG.get(), new BlockPos(6, 1, 5));
        helper.assertTrue(((ChestBlockEntity) helper.getBlockEntity(chest)).getItem(4).isEmpty(), "the chest's bag burned");
        helper.assertBlockPresent(AllBlocks.CURSE_BAG.get(), new BlockPos(10, 1, 10));
        // Flint and steel on a hidden bag burns it too.
        ServerPlayer p = player(helper, "sn-test-burner", new BlockPos(9, 1, 9));
        ItemStack flint = new ItemStack(Items.FLINT_AND_STEEL);
        p.setItemInHand(InteractionHand.MAIN_HAND, flint);
        BlockPos last = helper.absolutePos(new BlockPos(10, 1, 10));
        helper.getLevel().getBlockState(last).useItemOn(flint, helper.getLevel(), p, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(last), Direction.UP, last, false));
        helper.assertBlockNotPresent(AllBlocks.CURSE_BAG.get(), new BlockPos(10, 1, 10));
        // And one in a pocket.
        p.getInventory().add(HexBags.curseBag(maker));
        helper.assertTrue(HexBags.burnCarried(p) == 1, "the carried bag burns");
        helper.assertTrue(curseBags(p) == 0, "no bag left on the player");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void aSlippedBagCursesItsNewOwnerUnlessWarded(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ServerPlayer witch = player(helper, "sn-test-witch", new BlockPos(1, 1, 1));
        ServerPlayer mark = player(helper, "sn-test-mark", new BlockPos(3, 1, 3));
        ServerPlayer holder = warded(helper, new BlockPos(1, 1, 3));
        ItemStack bag = HexBags.curseBag(witch.getUUID());
        witch.setItemInHand(InteractionHand.MAIN_HAND, bag);
        helper.assertTrue(!HexBags.slip(witch, holder, bag) && bag.getCount() == 1, "a protection bag turns it away");
        helper.assertTrue(HexBags.slip(witch, mark, bag), "slipped");
        helper.assertTrue(bag.isEmpty(), "the witch's bag is gone");
        helper.assertTrue(curseBags(mark) == 1, "the mark carries it now");
        helper.assertTrue(HexBags.afflictCarrier(mark, false) && mark.hasEffect(AllMobEffects.JINXED), "and it curses them");
        // Its maker can carry it unharmed.
        witch.getInventory().add(HexBags.curseBag(witch.getUUID()));
        helper.assertTrue(!HexBags.afflictCarrier(witch, false) && !cursed(witch), "the maker carries their own bag safely");
        // Hidden in a chest, it gets whoever opens it.
        ServerPlayer opener = player(helper, "sn-test-opener", new BlockPos(3, 1, 1));
        SimpleContainer chest = new SimpleContainer(27);
        chest.setItem(13, HexBags.curseBag(witch.getUUID()));
        ChestMenu menu = ChestMenu.threeRows(1, opener.getInventory(), chest);
        helper.assertTrue(HexBags.afflictOpener(opener, menu), "opening the chest afflicts");
        MobEffectInstance jinx = opener.getEffect(AllMobEffects.JINXED);
        helper.assertTrue(jinx != null && jinx.getDuration() > HexBags.JINX_TICKS, "for longer than a pulse");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void aProtectionBagKeepsDemonsOffUntilProvoked(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        BlackEyedDemon demon = helper.spawn(AllEntities.BLACK_EYED_DEMON.get(), new BlockPos(2, 1, 2));
        ServerPlayer holder = warded(helper, new BlockPos(1, 1, 1));
        ServerPlayer stranger = player(helper, "sn-test-bare", new BlockPos(3, 1, 3));
        demon.setTarget(holder);
        helper.assertTrue(demon.getTarget() == null, "the demon cannot take the holder as a target");
        demon.setTarget(stranger);
        helper.assertTrue(demon.getTarget() == stranger, "but anyone else, yes");
        demon.setTarget(null);
        demon.hurt(holder.damageSources().playerAttack(holder), 1.0f);
        demon.setTarget(holder);
        helper.assertTrue(demon.getTarget() == holder, "struck, it hunts the holder");
        // A spent bag crumbles, and protects no more.
        ItemStack bag = HexBags.protectionBag(holder);
        helper.assertTrue(ProtectionBagItem.drain(bag, holder, 100) && ProtectionBagItem.charge(bag) == HexBags.PROTECTION_CHARGE - 100, "drains");
        helper.assertTrue(!ProtectionBagItem.drain(bag, holder, HexBags.PROTECTION_CHARGE) && bag.isEmpty(), "and crumbles at zero");
        helper.assertTrue(!HexBags.isProtected(holder), "no more protection");
        demon.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void theHexBagSpellSewsABagForItsCaster(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        helper.setBlock(BAG, AllBlocks.SPELL_BOWL.get().defaultBlockState());
        SpellBowlBlockEntity bowl = (SpellBowlBlockEntity) helper.getBlockEntity(BAG);
        ServerPlayer p = player(helper, "sn-test-hexer", new BlockPos(3, 1, 2));
        bowl.setContents(BowlContents.of(List.of(new ItemStack(Items.LEATHER), new ItemStack(Items.BONE), new ItemStack(Items.SPIDER_EYE),
                new ItemStack(AllItems.GRAVE_DIRT.get()), new ItemStack(Items.STRING)), List.of(Dose.of(BowlLiquid.DEMON_BLOOD))));
        var holder = helper.getLevel().getRecipeManager().getRecipeFor(AllRecipes.BOWL_SPELL.get(), BowlInput.of(bowl.contents()), helper.getLevel())
                .orElseThrow(() -> new IllegalStateException("no recipe for the curse bag mix"));
        BowlSpellRecipe recipe = holder.value();
        helper.assertTrue(recipe.spell(holder.id()).equals(SupernaturalCraft.asResource("hex_bags")), "it is the Hex Bags spell");
        ManaManager.get(p).learnRite(recipe.spell(holder.id()));
        ManaManager.get(p).setMana(100);
        ItemStack flint = new ItemStack(Items.FLINT_AND_STEEL);
        p.setItemInHand(InteractionHand.MAIN_HAND, flint);
        helper.assertTrue(bowl.tryLight(p, flint, InteractionHand.MAIN_HAND) == SpellBowlBlockEntity.LightResult.LIT, "lit");
        bowl.ageSession(20);
        helper.assertTrue(bowl.resolveRecitation(p, true, 0) == SpellBowlBlockEntity.Outcome.CAST, "cast");
        ItemStack made = ItemStack.EMPTY;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(AllItems.CURSE_BAG.get())) made = p.getInventory().getItem(i);
        }
        helper.assertTrue(!made.isEmpty(), "the caster has a curse bag");
        var hex = made.get(AllDataComponents.HEX_BAG.get());
        helper.assertTrue(hex != null && hex.maker().equals(p.getUUID()), "made by the caster: " + hex);
        // Set down, the bag keeps its maker.
        BlockPos spot = new BlockPos(1, 1, 1);
        helper.setBlock(spot, AllBlocks.CURSE_BAG.get().defaultBlockState());
        helper.getBlockEntity(spot).applyComponentsFromItemStack(made);
        helper.assertTrue(((CurseBagBlockEntity) helper.getBlockEntity(spot)).maker().orElse(null) instanceof UUID u && u.equals(p.getUUID()),
                "the hidden bag remembers its maker");
        helper.succeed();
    }
}
