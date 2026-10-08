package org.papiricoh.supernaturalcraft.gametest;

import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.lilith.HeadstoneBlock;
import org.papiricoh.supernaturalcraft.entity.boss.lilith.LilithAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.lilith.LilithEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.hellhound.BoundHellhoundEntity;
import org.papiricoh.supernaturalcraft.entity.hellhound.HellhoundEntity;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllRecipes;
import org.papiricoh.supernaturalcraft.ritual.RitualRecipe;
import org.papiricoh.supernaturalcraft.ritual.block.RitualAltarBlockEntity;

import java.util.List;
import java.util.UUID;

/** Lilith: three phases, her headstones and white light, her contracts and hounds, her ritual and her spoils. Each test in its own batch. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class LilithTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    private static LilithEntity spawn(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        return helper.spawn(AllEntities.LILITH.get(), MID);
    }

    private static float smite(LilithEntity l, ServerLevel level, float amount) {
        float before = l.trueHealth();
        l.invulnerableTime = 0;
        l.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, null), amount);
        return before - l.trueHealth();
    }

    private static ServerPlayer hunter(GameTestHelper helper, String name) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
    }

    @GameTest(template = SNGameTests.ARENA, batch = "lilith_health", timeoutTicks = 60)
    public static void fiveHundredHealthThreePhasesAndHeadstones(GameTestHelper helper) {
        LilithEntity l = spawn(helper);
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            helper.assertTrue(Math.abs(l.trueMaxHealth() - 8000f) < 0.5f, "true health should be 8000, is " + l.trueMaxHealth());
            ArenaController arena = l.arena();
            helper.assertTrue(arena != null && arena.theme() == ArenaTheme.SEAL, "she should open a seal arena");
            helper.assertTrue(l.maxPhase() == 3, "three phases");
            helper.assertTrue(l.headstones().size() >= 5, "too few headstones: " + l.headstones().size());
            for (BlockPos p : l.headstones()) {
                BlockState lower = level.getBlockState(p), upper = level.getBlockState(p.above());
                helper.assertTrue(lower.is(AllBlocks.CRACKED_HEADSTONE.get()) && lower.getValue(HeadstoneBlock.HALF) == DoubleBlockHalf.LOWER,
                        "no headstone at " + p.toShortString());
                helper.assertTrue(upper.is(AllBlocks.CRACKED_HEADSTONE.get()) && upper.getValue(HeadstoneBlock.HALF) == DoubleBlockHalf.UPPER,
                        "no headstone top at " + p.toShortString());
            }
            BlockPos first = l.headstones().getFirst();
            BossTests.cleanup(helper);
            helper.assertFalse(level.getBlockState(first).is(AllBlocks.CRACKED_HEADSTONE.get()), "the headstones should go with the arena");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "lilith_threshold", timeoutTicks = 60)
    public static void thresholdsStopAtThirds(GameTestHelper helper) {
        LilithEntity l = spawn(helper);
        helper.runAfterDelay(3, () -> {
            BossHealthGuard.set(l, l.getMaxHealth() * 0.67f);
            smite(l, helper.getLevel(), 1e6f);
            helper.assertTrue(Math.abs(l.getHealth() / l.getMaxHealth() - 2f / 3f) < 0.002f, "health should stop at two thirds");
            helper.assertTrue(l.phase() == 2 && l.state() == LuciferEntity.TRANSITION, "crossing it should begin phase 2");
            l.forceLook(2);
            BossHealthGuard.set(l, l.getMaxHealth() * 0.34f);
            smite(l, helper.getLevel(), 1e6f);
            helper.assertTrue(Math.abs(l.getHealth() / l.getMaxHealth() - 1f / 3f) < 0.002f, "health should stop at one third");
            helper.assertTrue(l.phase() == 3, "crossing it should begin phase 3");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "lilith_light", timeoutTicks = 60)
    public static void whiteLightIsStoppedByAHeadstoneThatCracksAndFalls(GameTestHelper helper) {
        LilithEntity l = spawn(helper);
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            ArenaController arena = l.arena();
            Vec3 at = l.position();
            ArmorStand open = new ArmorStand(EntityType.ARMOR_STAND, level), hidden = new ArmorStand(EntityType.ARMOR_STAND, level);
            open.moveTo(at.x + 5, at.y, at.z);
            hidden.moveTo(at.x - 5, at.y, at.z);
            level.addFreshEntity(open);
            level.addFreshEntity(hidden);
            BlockPos stone = BlockPos.containing(at.x - 3, at.y, at.z);
            BlockState base = AllBlocks.CRACKED_HEADSTONE.get().defaultBlockState().setValue(HeadstoneBlock.FACING, Direction.EAST);
            arena.mutate(level, stone, base.setValue(HeadstoneBlock.HALF, DoubleBlockHalf.LOWER), 0);
            arena.mutate(level, stone.above(), base.setValue(HeadstoneBlock.HALF, DoubleBlockHalf.UPPER), 0);
            helper.assertTrue(LilithAttacks.WhiteLight.blocked(l, open) == null, "the light should reach someone in the open");
            var cover = LilithAttacks.WhiteLight.blocked(l, hidden);
            helper.assertTrue(cover != null && level.getBlockState(cover.getBlockPos()).is(AllBlocks.CRACKED_HEADSTONE.get()),
                    "the headstone should stand between her and the hidden one");
            l.crackHeadstone(level, cover.getBlockPos());
            helper.assertTrue(level.getBlockState(stone).getValue(HeadstoneBlock.CRACKS) == 1, "one burst, one crack");
            l.crackHeadstone(level, stone.above());
            helper.assertFalse(level.getBlockState(stone).is(AllBlocks.CRACKED_HEADSTONE.get())
                    || level.getBlockState(stone.above()).is(AllBlocks.CRACKED_HEADSTONE.get()), "a twice-cracked headstone should fall");
            open.discard();
            hidden.discard();
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "lilith_spent", timeoutTicks = 60)
    public static void sheIsOpenAfterHerLight(GameTestHelper helper) {
        LilithEntity l = spawn(helper);
        helper.runAfterDelay(3, () -> {
            LilithAttacks.WhiteLight.burst(l);
            helper.assertTrue(l.isSpent(), "she should be spent after her light");
            float dealt = smite(l, helper.getLevel(), 10f);
            helper.assertTrue(Math.abs(dealt - 14f) < 0.1f, "spent, a holy blow of 10 should take 14, took " + dealt);
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "lilith_contract_burn", timeoutTicks = 60)
    public static void woundsBurnAContract(GameTestHelper helper) {
        LilithEntity l = spawn(helper);
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            ServerPlayer p = hunter(helper, "sn-test-signed");
            l.contracts().sign(p.getUUID(), level.getGameTime(), 200);
            l.invulnerableTime = 0;
            // A holy wound of 70 is worth 140 against a contract: past its 1.5% of her (120).
            l.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, p), 70f);
            helper.assertFalse(l.contracts().has(p.getUUID()), "holy wounds worth 120 should burn the contract");
            helper.assertTrue(l.hasEffect(AllMobEffects.STUNNED), "a burned contract should stagger her");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "lilith_contract_due", timeoutTicks = 60)
    public static void aContractDueBringsUnseenHounds(GameTestHelper helper) {
        LilithEntity l = spawn(helper);
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            ServerPlayer p = hunter(helper, "sn-test-due");
            Vec3 at = helper.absoluteVec(MID.offset(6, 0, 6).getBottomCenter());
            p.moveTo(at.x, at.y, at.z);
            l.houndsComeFor(level, p);
            List<HellhoundEntity> hounds = level.getEntitiesOfClass(HellhoundEntity.class, new AABB(BlockPos.containing(at)).inflate(10));
            helper.assertTrue(hounds.size() == 2, "two hounds should come in the first phase, found " + hounds.size());
            for (HellhoundEntity h : hounds) {
                helper.assertFalse(h.isRevealed(), "the contract's hounds come unseen");
                helper.assertTrue(h.getTarget() == p, "the hounds should hunt the one who signed");
                helper.assertTrue(l.minions().contains(h.getUUID()), "the hounds should go when the fight ends");
            }
            BossTests.cleanup(helper);
            helper.assertTrue(hounds.stream().allMatch(HellhoundEntity::isRemoved), "the hounds should go with her");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "lilith_colt", timeoutTicks = 60)
    public static void theColtStunsHer(GameTestHelper helper) {
        LilithEntity l = spawn(helper);
        helper.runAfterDelay(3, () -> {
            float before = l.trueHealth();
            l.invulnerableTime = 0;
            l.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.COLT, null), 60f);
            helper.assertTrue(Math.abs(before - l.trueHealth() - 60f) < 0.5f, "an exact blow of 60 should take exactly 60");
            helper.assertTrue(l.hasEffect(AllMobEffects.STUNNED), "a Colt round should stagger her");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "lilith_death", timeoutTicks = 400)
    public static void deathLeavesTheLastSeal(GameTestHelper helper) {
        LilithEntity l = spawn(helper);
        helper.runAfterDelay(3, () -> l.beginTransition(3));
        helper.runAfterDelay(5, () -> BossHealthGuard.set(l, 5f));
        helper.runAfterDelay(LuciferEntity.TRANSITION_TICKS + 8, () -> {
            smite(l, helper.getLevel(), 50f);
            helper.assertTrue(l.isAlive() && l.state() == LuciferEntity.DYING, "the killing blow should start her death");
        });
        helper.runAfterDelay(LuciferEntity.TRANSITION_TICKS + 8 + LilithEntity.LILITH_DEATH_TICKS + 10, () -> {
            helper.assertTrue(l.isRemoved(), "she is still here after her death");
            int seals = 0, contracts = 0;
            boolean trophy = false, whistle = false;
            for (ItemEntity e : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(MID)).inflate(40))) {
                ItemStack s = e.getItem();
                if (s.is(AllItems.LAST_SEAL.get())) seals += s.getCount();
                if (s.is(AllItems.DAMNED_CONTRACT.get())) contracts += s.getCount();
                trophy |= s.is(AllItems.LILITH_TROPHY.get());
                whistle |= s.is(AllItems.HOUND_WHISTLE.get());
                e.discard();
            }
            helper.assertTrue(seals == 1, "one last seal, found " + seals);
            helper.assertTrue(trophy && whistle, "her trophy and whistle should fall");
            helper.assertTrue(contracts >= 2, "two to four contracts, found " + contracts);
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "lilith_rite", timeoutTicks = 60)
    public static void herRiteNeedsAzazelBeaten(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.setDayTime(18000);
        HellTests.draw(helper, HellTests.GREAT_CIRCLE);
        ServerPlayer p = HellTests.ritualist(helper, "sn-test-lilith");
        RitualAltarBlockEntity altar = (RitualAltarBlockEntity) helper.getBlockEntity(new BlockPos(5, 1, 5));
        helper.runAfterDelay(1, () -> {
            HellTests.offer(altar, p, new ItemStack(AllItems.DEMON_BLOOD.get()), new ItemStack(AllItems.DEMON_BLOOD.get()),
                    new ItemStack(AllItems.DEMON_BLOOD.get()), new ItemStack(AllItems.HELLFIRE_EMBER.get()), new ItemStack(AllItems.HELLFIRE_EMBER.get()),
                    new ItemStack(Items.BONE), new ItemStack(Items.BONE), new ItemStack(Items.PAPER));
            HellTests.offer(altar, p, new ItemStack(Items.FLINT_AND_STEEL));
            helper.assertFalse(altar.isChanneling(), "her rite started for someone who never beat Azazel");
            HellTests.award(p, "main/yellow_eyed");
            HellTests.offer(altar, p, new ItemStack(Items.FLINT_AND_STEEL));
            helper.assertTrue(altar.isChanneling(), "her rite should start once Azazel is beaten");
            // Break the circle before she comes: no fight should outlive the test.
            helper.setBlock(new BlockPos(5, 1, 5), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            level.setDayTime(6000);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.SMALL, batch = "lilith_recipes", timeoutTicks = 20)
    public static void luciferNeedsTheLastSeal(GameTestHelper helper) {
        RitualRecipe lucifer = null;
        for (var holder : helper.getLevel().getRecipeManager().getAllRecipesFor(AllRecipes.RITUAL.get())) {
            if (holder.id().getPath().equals("ritual/summon_lucifer")) lucifer = holder.value();
        }
        helper.assertTrue(lucifer != null, "no summon_lucifer");
        boolean seal = false;
        for (Ingredient i : lucifer.ingredients()) seal |= i.test(new ItemStack(AllItems.LAST_SEAL.get()));
        helper.assertTrue(seal, "Lucifer's summoning should need the last seal");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "lilith_whistle", timeoutTicks = 40)
    public static void theWhistleCallsABoundHound(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = hunter(helper, "sn-test-whistle");
        Vec3 at = helper.absoluteVec(MID.getBottomCenter());
        p.moveTo(at.x, at.y, at.z);
        BoundHellhoundEntity hound = BoundHellhoundEntity.call(level, p, at.x + 2, at.y, at.z);
        helper.assertTrue(hound != null && hound.isAlive(), "no hound answered");
        helper.assertTrue(hound.isRevealed(), "a bound hound is always seen");
        helper.assertFalse(hound.canAttack(p), "a bound hound never turns on a player");
        helper.assertTrue(p.getUUID().equals(hound.ownerId()), "the hound should know its holder");
        hound.setLife(1);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(hound.isRemoved(), "the hound should go when its time is up");
            helper.succeed();
        });
    }
}
