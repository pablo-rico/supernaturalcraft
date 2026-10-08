package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.BossTwists;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.balance.Balance;
import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.GarrisonAngelEntity;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.HolyOilSlickBlock;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.OilRings;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelBalance;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelEntity;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelSpoils;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.arena.HouseGround;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.arena.HouseLayout;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllRecipes;
import org.papiricoh.supernaturalcraft.reward.raphael.StormcallerItem;
import org.papiricoh.supernaturalcraft.ritual.RitualConditions;
import org.papiricoh.supernaturalcraft.ritual.RitualRecipe;

import java.util.List;
import java.util.UUID;

/**
 * Raphael, the archangel of the storm (v0.16): his rite wants a thunderstorm, the bolt and the held storm, his health and the
 * hard cap, the house written and given back, the threads of grace (they heal, a hunter in one cuts it, a fallen angel's is
 * gone and laying on hands brings it back), the rings of holy oil (lit, they hold him and he takes more), the roof torn off in
 * the wrath, his fall and his spoils, the factions' greetings and the Stormcaller. Each boss test has its own batch and clears
 * the field first; the weather is global, so every test that touches it has its own batch and clears it.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class RaphaelTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    private static void cleanup(GameTestHelper helper) {
        BossTests.cleanup(helper);
        for (Entity e : helper.getLevel().getAllEntities()) {
            if (e instanceof GarrisonAngelEntity) e.discard();
        }
        clearWeather(helper.getLevel());
    }

    private static void clearWeather(ServerLevel level) {
        level.setWeatherParameters(6000, 0, false, false);
        level.setRainLevel(0);
        level.setThunderLevel(0);
    }

    private static RaphaelEntity spawn(GameTestHelper helper) {
        cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        return helper.spawn(AllEntities.RAPHAEL.get(), MID);
    }

    /** Spawns him, writes his house and pours the rings at once. */
    private static void housed(GameTestHelper helper, RaphaelEntity r) {
        r.buildHouseNow();
        helper.assertTrue(r.ground() != null && !r.writingHouse(), "his house is written round him");
    }

    private static ServerPlayer hunter(GameTestHelper helper, String name, Vec3 at) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.setGameMode(GameType.SURVIVAL);
        p.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN);
        p.moveTo(at.x, at.y, at.z);
        return p;
    }

    private static int count(GameTestHelper helper, Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(MID)).inflate(40),
                e -> e.isAlive() && e.getItem().is(item)).stream().mapToInt(e -> e.getItem().getCount()).sum();
    }

    // --- the rite ----------------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, batch = "raphael_rite", timeoutTicks = 20)
    public static void hisRiteAnswersOnlyInAThunderstorm(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        clearWeather(level);
        RitualRecipe summon = null;
        for (var holder : level.getRecipeManager().getAllRecipesFor(AllRecipes.RITUAL.get())) {
            if (holder.id().getPath().equals("ritual/summon_raphael")) summon = holder.value();
        }
        helper.assertTrue(summon != null, "no summon_raphael");
        RitualRecipe r = summon;
        helper.assertTrue(r.conditions().weather() == RitualConditions.Weather.THUNDER, "his rite wants thunder");
        helper.assertFalse(r.consumeActivator(), "the angel blade is not spent");
        ServerPlayer p = HellTests.ritualist(helper, "sn-test-raphael-rite");
        helper.runAfterDelay(1, () -> {
            HellTests.award(p, "main/war");
            HellTests.award(p, "main/famine");
            HellTests.award(p, "main/pestilence");
            helper.assertTrue("message.supernaturalcraft.ritual.needs_thunder".equals(r.conditions().check(level, p)),
                    "under a clear sky the storm does not answer: " + r.conditions().check(level, p));
            level.setWeatherParameters(0, 6000, true, true);
            level.setRainLevel(1);
            level.setThunderLevel(1);
            helper.assertTrue(r.conditions().check(level, p) == null, "in a thunderstorm it does: " + r.conditions().check(level, p));
            level.setWeatherParameters(0, 6000, true, false);
            level.setThunderLevel(0);
            helper.assertTrue("message.supernaturalcraft.ritual.needs_thunder".equals(r.conditions().check(level, p)), "rain is not enough");
            clearWeather(level);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "raphael_summon", timeoutTicks = 80)
    public static void theRiteCallsHimDownInABoltIntoAHouseInTheStorm(GameTestHelper helper) {
        cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        BlockPos altar = helper.absolutePos(MID);
        helper.assertTrue(new org.papiricoh.supernaturalcraft.ritual.effect.SummonRaphaelEffect().perform(level, altar, null),
                "the rite should bring him");
        List<RaphaelEntity> found = level.getEntitiesOfClass(RaphaelEntity.class, new AABB(altar).inflate(30));
        helper.assertTrue(found.size() == 1, "one Raphael, found " + found.size());
        RaphaelEntity r = found.getFirst();
        helper.assertTrue(r.state() == LuciferEntity.EMERGING && r.wingsShown(), "he kneels in the bolt, his wings out");
        helper.assertTrue(r.arena() != null && r.arena().forcedStorm(), "the storm is held over his arena");
        helper.assertTrue(((ServerLevelData) level.getLevelData()).isThundering(), "a thunderstorm");
        helper.assertFalse(new org.papiricoh.supernaturalcraft.ritual.effect.SummonRaphaelEffect().perform(level, altar.offset(3, 0, 0), null),
                "a second rite finds the world busy");
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(r.ground() != null && !r.writingHouse(), "the house is written while he kneels");
            HouseGround g = r.ground();
            helper.assertTrue(level.getBlockState(g.at(-HouseLayout.HALF_X, 2, 0)).isSolid(), "the west wall stands");
            helper.assertTrue(level.getBlockState(g.at(0, 0, 0)).is(net.minecraft.tags.BlockTags.PLANKS), "a wooden floor under the rite");
            helper.assertTrue(level.getBlockState(g.at(0, HouseLayout.CEILING, 6)).isAir()
                    || level.getBlockState(g.at(0, HouseLayout.CEILING, 6)).is(Blocks.SPRUCE_PLANKS), "a ceiling");
            BossTests.cleanup(helper);
            helper.assertTrue(level.getBlockState(g.at(-HouseLayout.HALF_X, 2, 0)).isAir(), "the arena takes the house back");
            helper.assertTrue(level.getBlockState(g.at(0, 0, 0)).is(Blocks.STONE), "and gives the floor back");
            helper.assertFalse(((ServerLevelData) level.getLevelData()).isThundering(), "and lets the storm go");
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- his health --------------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "raphael_health", timeoutTicks = 60)
    public static void twentySixThousandTrueHealthAndTheHardCap(GameTestHelper helper) {
        RaphaelEntity r = spawn(helper);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(Math.abs(r.trueMaxHealth() - 26_000f) < 1f, "26000 true health alone, has " + r.trueMaxHealth());
            helper.assertTrue(r.getMaxHealth() <= 1024, "vanilla health under the cap");
            helper.assertTrue(r.arena() != null && r.arena().forcedStorm(), "even by egg, the storm comes with him");
            BossHealthGuard.set(r, r.getMaxHealth() * 0.9f);
            r.invulnerableTime = 0;
            float before = r.trueHealth();
            r.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 1e6f);
            float lost = before - r.trueHealth(), hard = Balance.hardCap(r.trueMaxHealth());
            helper.assertTrue(lost <= r.trueMaxHealth() * 0.015f + 0.5f && lost > hard * 0.99f,
                    "a blow of a million takes at most 1.5%: " + lost + " of " + r.trueMaxHealth());
            BossHealthGuard.set(r, r.getMaxHealth() * 0.68f);
            r.invulnerableTime = 0;
            r.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 1e6f);
            helper.assertTrue(Math.abs(r.getHealth() - r.getMaxHealth() * 2f / 3f) < 0.01f, "a blow stops at two thirds");
            helper.assertTrue(r.phase() == 2 && r.state() == LuciferEntity.TRANSITION, "and the healer's phase begins");
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- the healer --------------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "raphael_threads", timeoutTicks = 60)
    public static void aThreadHealsHimUntilAHunterStandsInItOrItsAngelFalls(GameTestHelper helper) {
        RaphaelEntity r = spawn(helper);
        helper.runAfterDelay(3, () -> {
            housed(helper, r);
            ServerLevel level = helper.getLevel();
            r.forceLook(2);
            BossHealthGuard.set(r, r.getMaxHealth() * 0.5f);
            List<GarrisonAngelEntity> angels = r.callGarrison(level);
            helper.assertTrue(angels.size() == RaphaelBalance.GARRISON, "his garrison comes: " + angels.size());
            r.refreshThreads();
            helper.assertTrue(r.activeThreads() == RaphaelBalance.GARRISON, "every angel holds a thread: " + r.activeThreads());
            float healed = r.mendByThreads();
            float expected = RaphaelBalance.tetherHeal(RaphaelBalance.GARRISON, SNConfig.RAPHAEL_TETHER_HEAL.get(), r.trueMaxHealth());
            helper.assertTrue(Math.abs(healed - expected) < 1f, "four threads heal 4 x 0.4% a second: " + healed + " vs " + expected);
            // A hunter steps into the first thread.
            GarrisonAngelEntity first = angels.getFirst();
            Vec3 mid = first.position().add(r.position()).scale(0.5);
            ServerPlayer p = hunter(helper, "sn-raphael-thread", mid);
            r.track(p);
            r.refreshThreads();
            helper.assertTrue(r.activeThreads() == RaphaelBalance.GARRISON - 1, "standing in a thread cuts it: " + r.activeThreads());
            healed = r.mendByThreads();
            expected = RaphaelBalance.tetherHeal(RaphaelBalance.GARRISON - 1, SNConfig.RAPHAEL_TETHER_HEAL.get(), r.trueMaxHealth());
            helper.assertTrue(Math.abs(healed - expected) < 1f, "the cut thread heals nothing: " + healed + " vs " + expected);
            // A second angel falls.
            GarrisonAngelEntity second = angels.get(1);
            second.hurt(helper.getLevel().damageSources().magic(), 1000f);
            r.refreshThreads();
            helper.assertTrue(!second.isAlive() && r.activeThreads() == RaphaelBalance.GARRISON - 2, "its angel falls, its thread goes: " + r.activeThreads());
            helper.assertTrue(r.canRaise(), "laying on hands may raise it");
            helper.assertTrue(r.raiseFallen(level) != null, "it gets up again");
            helper.assertFalse(r.canRaise(), "but only once");
            r.refreshThreads();
            helper.assertTrue(r.activeThreads() == RaphaelBalance.GARRISON - 1, "its thread is back: " + r.activeThreads());
            // Never past the start of the phase.
            BossHealthGuard.set(r, r.getMaxHealth() * 2f / 3f - 0.01f);
            r.mendByThreads();
            helper.assertTrue(r.getHealth() <= r.getMaxHealth() * 2f / 3f + 1e-3f, "the threads never heal him past his phase");
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- the holy oil ------------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "raphael_oil", timeoutTicks = 200)
    public static void aLitRingHoldsHimAndHeTakesMore(GameTestHelper helper) {
        RaphaelEntity r = spawn(helper);
        float[] base = new float[1];
        helper.runAfterDelay(3, () -> {
            housed(helper, r);
            ServerLevel level = helper.getLevel();
            HouseGround g = r.ground();
            for (int i = 0; i < HouseLayout.RINGS.size(); i++) {
                helper.assertTrue(r.rings().state(i) == OilRings.State.LAID, "ring " + i + " is poured");
            }
            helper.assertTrue(level.getBlockState(g.onFloor(HouseLayout.ringCells(0).getFirst())).is(AllBlocks.HOLY_OIL_SLICK.get()), "oil on the floor");
            // What a blow takes from him, free.
            BossHealthGuard.set(r, r.getMaxHealth() * 0.9f);
            r.invulnerableTime = 0;
            float before = r.getHealth();
            r.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, null), 100f);
            base[0] = before - r.getHealth();
            // He stands in ring 0, and it is lit.
            Vec3 c = OilRings.centre(g, 0);
            r.moveTo(c.x, c.y, c.z);
            helper.assertTrue(r.ignite(0), "the ring catches");
            helper.assertTrue(r.rings().state(0) == OilRings.State.BURNING, "it burns");
            // Flint and steel on another ring's oil lights that one.
            helper.assertTrue(HolyOilSlickBlock.light(level, g.onFloor(HouseLayout.ringCells(1).get(3))), "flint and steel on the oil");
            helper.assertTrue(r.rings().state(1) == OilRings.State.BURNING, "lights its whole ring");
            helper.assertTrue(level.getBlockState(g.onFloor(HouseLayout.ringCells(1).get(9))).is(AllBlocks.HOLY_OIL_FIRE.get()), "all round");
            // A burning arrow lands in a third.
            Arrow arrow = new Arrow(EntityType.ARROW, level);
            Vec3 a = OilRings.centre(g, 2).add(0, 0.3, 0);
            arrow.moveTo(a.x, a.y, a.z);
            arrow.igniteForSeconds(5);
            level.addFreshEntity(arrow);
        });
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(r.trapped(), "a lit ring round him holds him");
            helper.assertTrue(r.rings().state(2) == OilRings.State.BURNING, "a burning arrow lights a ring");
            BossHealthGuard.set(r, r.getMaxHealth() * 0.9f);
            r.invulnerableTime = 0;
            float before = r.getHealth();
            r.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 100f);
            float taken = before - r.getHealth();
            helper.assertTrue(Math.abs(taken - base[0] * RaphaelBalance.TRAPPED_VULNERABILITY) < 0.01f,
                    "held, he takes x1.4: " + taken + " vs " + base[0]);
            float h = r.getHealth();
            r.healTrue(1000f);
            helper.assertTrue(r.getHealth() == h, "held, he can't heal");
            helper.assertTrue(r.scheduler().current() == null, "held, he can't strike");
        });
        helper.runAfterDelay(8 + SNConfig.RAPHAEL_TRAP_TICKS.get() + 4, () -> {
            helper.assertFalse(r.trapped(), "he breaks out in time");
            helper.assertTrue(r.rings().state(0) == OilRings.State.SPENT, "and the ring he broke is spent");
            helper.assertFalse(helper.getLevel().getBlockState(r.ground().onFloor(HouseLayout.ringCells(0).getFirst()))
                    .is(AllBlocks.HOLY_OIL_FIRE.get()), "its fire is out");
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- the wrath ---------------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "raphael_roof", timeoutTicks = 240)
    public static void theWrathTearsTheRoofOffAndTheArenaGivesTheHouseBack(GameTestHelper helper) {
        RaphaelEntity r = spawn(helper);
        BlockPos[] slab = new BlockPos[1], floor = new BlockPos[1];
        helper.runAfterDelay(3, () -> {
            housed(helper, r);
            ServerLevel level = helper.getLevel();
            for (BlockPos p : r.ground().roofPositions()) {
                if (level.getBlockState(p).is(Blocks.DARK_OAK_SLAB)) {
                    slab[0] = p;
                    break;
                }
            }
            helper.assertTrue(slab[0] != null, "the house has a roof");
            floor[0] = r.ground().at(2, 0, 2);
            helper.assertTrue(level.getBlockState(floor[0]).getBlock() != Blocks.STONE, "and a floor of its own");
            r.ignite(3);
            r.forceLook(2);
            BossHealthGuard.set(r, r.getMaxHealth() * 0.35f);
            r.beginTransition(3);
            helper.assertTrue(r.wingsShown() && r.veinsLit(), "the wrath: his wings out, his veins alight");
        });
        helper.runAfterDelay(3 + LuciferEntity.FINAL_TRANSITION_TICKS / 2 + 12, () -> {
            ServerLevel level = helper.getLevel();
            helper.assertFalse(r.ground().roofOn(), "the roof is torn off");
            helper.assertTrue(level.getBlockState(slab[0]).isAir(), "open sky over the house");
            helper.assertTrue(level.getBlockState(r.ground().at(-HouseLayout.HALF_X, 2, 0)).isSolid(), "but the walls stand");
            helper.assertTrue(r.rings().state(3) == OilRings.State.LAID, "and the rings are poured again for the new phase");
            cleanup(helper);
            helper.assertTrue(level.getBlockState(floor[0]).is(Blocks.STONE), "the arena gives the floor back");
            helper.assertTrue(level.getBlockState(r.ground().at(-HouseLayout.HALF_X, 2, 0)).isAir(), "and takes the walls away");
            helper.succeed();
        });
    }

    // --- his fall and his spoils -------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, batch = "raphael_spoils", timeoutTicks = 20)
    public static void theFirstVictoryGivesTheStormcallerARematchHalfTheTime(GameTestHelper helper) {
        ServerPlayer p = hunter(helper, "sn-raphael-spoils", helper.absoluteVec(new Vec3(2, 1, 2)));
        RandomSource rng = RandomSource.create(7);
        helper.assertFalse(RaphaelSpoils.beatenBefore(p), "never beaten him");
        List<ItemStack> first = RaphaelSpoils.share(p, rng);
        helper.assertTrue(has(first, AllItems.RAPHAELS_STORMCALLER.get()) && has(first, AllItems.RAPHAEL_TROPHY.get()),
                "the first time: the Stormcaller and the bust");
        HellTests.award(p, RaphaelSpoils.ADVANCEMENT);
        helper.assertTrue(RaphaelSpoils.beatenBefore(p), "his victory is written down");
        int staffs = 0;
        for (int i = 0; i < 40; i++) {
            List<ItemStack> again = RaphaelSpoils.share(p, rng);
            helper.assertTrue(has(again, AllItems.RAPHAEL_TROPHY.get()), "a rematch: the bust");
            if (has(again, AllItems.RAPHAELS_STORMCALLER.get())) staffs++;
        }
        helper.assertTrue(staffs > 5 && staffs < 35, "half the rematches a Stormcaller, " + staffs + "/40");
        helper.succeed();
    }

    private static boolean has(List<ItemStack> stacks, Item item) {
        return stacks.stream().anyMatch(s -> s.is(item));
    }

    @GameTest(template = SNGameTests.ARENA, batch = "raphael_death", timeoutTicks = 280)
    public static void heKneelsBurnsIntoTheFloorAndLeavesHisBust(GameTestHelper helper) {
        RaphaelEntity r = spawn(helper);
        helper.runAfterDelay(3, () -> {
            housed(helper, r);
            r.forceLook(3);
            BossHealthGuard.set(r, 2f);
            r.invulnerableTime = 0;
            r.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 500f);
            helper.assertTrue(r.state() == LuciferEntity.DYING, "at the last he falls");
            helper.assertTrue(r.wingsShown() && r.veinsLit(), "in light, wings and all");
        });
        helper.runAfterDelay(3 + RaphaelBalance.DEATH_BURST + 2, () -> {
            ArenaController arena = r.arena();
            helper.assertTrue(arena != null, "his arena is still open");
            BlockPos under = r.blockPosition().below();
            boolean burnt = false;
            for (BlockPos p : BlockPos.betweenClosed(under.offset(-7, 0, -4), under.offset(7, 0, 6))) {
                if (helper.getLevel().getBlockState(p).is(Blocks.BLACK_CONCRETE) || helper.getLevel().getBlockState(p).is(Blocks.COAL_BLOCK)) {
                    burnt = true;
                    break;
                }
            }
            helper.assertTrue(burnt, "his wings are burnt into the floor");
        });
        helper.runAfterDelay(3 + RaphaelBalance.DEATH_TICKS + 12, () -> {
            helper.assertTrue(r.isRemoved(), "he is gone");
            helper.assertTrue(count(helper, AllItems.RAPHAEL_TROPHY.get()) == 1, "his bust falls");
            helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(MID)).inflate(40)).forEach(Entity::discard);
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- the factions (v0.13) ----------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, batch = "raphael_factions", timeoutTicks = 20)
    public static void heGreetsEveryFaction(GameTestHelper helper) {
        helper.assertTrue(BossTwists.GREETERS.contains("raphael"), "he greets the sworn");
        RaphaelEntity r = AllEntities.RAPHAEL.get().create(helper.getLevel());
        helper.assertTrue(r != null, "a Raphael");
        for (Faction f : Faction.values()) {
            String key = BossTwists.greetingKey(r, f);
            helper.assertTrue(key.equals("message.supernaturalcraft.allegiance.boss.raphael." + f.getSerializedName()), key);
            helper.assertTrue(Language.getInstance().has(key), "a line for every faction: " + key);
        }
        r.discard();
        helper.succeed();
    }

    // --- the Stormcaller ---------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = "raphael_stormcaller", timeoutTicks = 20)
    public static void theStormcallerChainsLightningAndLaysAGrace(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = hunter(helper, "sn-raphael-staff", helper.absoluteVec(new Vec3(1.5, 1, 5.5)));
        p.getAbilities().instabuild = true;
        ItemStack staff = new ItemStack(AllItems.RAPHAELS_STORMCALLER.get());
        net.minecraft.world.entity.monster.Husk[] z = new net.minecraft.world.entity.monster.Husk[4];
        for (int i = 0; i < 4; i++) {
            z[i] = helper.spawn(EntityType.HUSK, new BlockPos(4 + i * 2, 1, 5));
            z[i].setNoAi(true);
        }
        float full = z[0].getHealth();
        List<net.minecraft.world.entity.LivingEntity> struck = StormcallerItem.chain(p, staff, z[0]);
        helper.assertTrue(struck.size() == StormcallerItem.CHAIN_TARGETS, "the lightning leaps to three foes: " + struck.size());
        helper.assertTrue(z[0].getHealth() < full && z[1].getHealth() < full && z[2].getHealth() < full, "each is struck");
        helper.assertTrue(z[3].getHealth() == full, "no further than three");
        helper.assertTrue(struck.get(1) == z[1], "it leaps to the nearest next");
        p.setHealth(6f);
        helper.assertTrue(StormcallerItem.grace(p, staff), "sneaking, a healing grace");
        helper.assertTrue(p.getHealth() > 6f, "it heals its bearer");
        helper.assertFalse(StormcallerItem.grace(p, staff), "and then it must gather again");
        for (var zombie : z) zombie.discard();
        helper.succeed();
    }
}
