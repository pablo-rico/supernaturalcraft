package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemanEntity;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemanKind;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemanSteedEntity;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemenEvents;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.death.DeathClock;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.death.DeathEntity;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.death.LimboExitEntity;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.famine.FamineEntity;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.pestilence.FlySwarmEntity;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.pestilence.Plague;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.war.WarEntity;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.war.WarMirageEntity;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.war.WarStandardEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllRecipes;
import org.papiricoh.supernaturalcraft.ritual.RitualRecipe;
import org.papiricoh.supernaturalcraft.ritual.effect.SummonDeathEffect;

import java.util.List;
import java.util.UUID;

/**
 * The Four Horsemen: their rites, their spoils (every time), War's illusion and standards, Famine's hunger and grip,
 * Pestilence's plague, antidote and flies, Death's clock, limbo and the rings he gives back. Each test in its own batch.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class HorsemenTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    private static void cleanup(GameTestHelper helper) {
        BossTests.cleanup(helper);
        for (Entity e : helper.getLevel().getAllEntities()) {
            if (e instanceof HorsemanSteedEntity || e instanceof WarMirageEntity || e instanceof WarStandardEntity
                    || e instanceof FlySwarmEntity || e instanceof LimboExitEntity) {
                e.discard();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends HorsemanEntity> T spawn(GameTestHelper helper, HorsemanKind kind) {
        cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        return (T) helper.spawn(kind.type(), MID);
    }

    private static ServerPlayer hunter(GameTestHelper helper, String name) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.setGameMode(GameType.SURVIVAL);
        Vec3 at = helper.absoluteVec(MID.offset(3, 0, 0).getBottomCenter());
        p.moveTo(at.x, at.y, at.z);
        return p;
    }

    private static RitualRecipe rite(GameTestHelper helper, String id) {
        for (var holder : helper.getLevel().getRecipeManager().getAllRecipesFor(AllRecipes.RITUAL.get())) {
            if (holder.id().getPath().equals("ritual/" + id)) return holder.value();
        }
        return null;
    }

    private static boolean needs(RitualRecipe r, Item item) {
        for (Ingredient i : r.ingredients()) if (i.test(new ItemStack(item))) return true;
        return false;
    }

    private static int count(GameTestHelper helper, Item item) {
        return items(helper, item).stream().mapToInt(e -> e.getItem().getCount()).sum();
    }

    private static List<ItemEntity> items(GameTestHelper helper, Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(MID)).inflate(40),
                e -> e.isAlive() && e.getItem().is(item));
    }

    /** Puts a Horseman into his last phase and lets him die: the spoils fall at the end of his death. */
    private static void defeat(HorsemanEntity h, ServerLevel level) {
        h.forceLook(h.maxPhase());
        h.setHealth(2f);
        h.invulnerableTime = 0;
        h.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, null), 500f);
    }

    // --- rites ----------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, batch = "horsemen_rites", timeoutTicks = 20)
    public static void theRitesAskForLuciferAndTheOverworldAndDeathForTheRings(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = HellTests.ritualist(helper, "sn-test-horsemen-rites");
        for (String id : List.of("summon_war", "summon_famine", "summon_pestilence", "summon_death")) {
            RitualRecipe r = rite(helper, id);
            helper.assertTrue(r != null, "no " + id);
            helper.assertTrue(r.conditions().dimension().isPresent()
                    && r.conditions().dimension().get().equals(net.minecraft.world.level.Level.OVERWORLD), id + " should be in the Overworld");
            helper.assertTrue("message.supernaturalcraft.ritual.not_ready".equals(r.conditions().check(level, p)),
                    id + " should wait for Lucifer's defeat");
        }
        RitualRecipe death = rite(helper, "summon_death");
        helper.assertTrue(death != null, "no summon_death");
        helper.assertTrue(needs(death, AllItems.RING_OF_WAR.get()) && needs(death, AllItems.RING_OF_FAMINE.get())
                && needs(death, AllItems.RING_OF_PESTILENCE.get()), "Death wants the three rings offered");
        helper.assertTrue(death.conditions().requiresAdvancement().size() == 3, "Death wants all three Horsemen beaten");
        HellTests.award(p, "main/devil_went_down");
        for (String id : List.of("summon_war", "summon_famine", "summon_pestilence")) {
            helper.assertTrue(rite(helper, id).conditions().check(level, p) == null, id + " should be ready after Lucifer, here");
        }
        helper.assertTrue("message.supernaturalcraft.ritual.not_ready".equals(death.conditions().check(level, p)),
                "Death should wait for all three");
        HellTests.award(p, "main/war");
        HellTests.award(p, "main/famine");
        helper.assertTrue("message.supernaturalcraft.ritual.not_ready".equals(death.conditions().check(level, p)),
                "two of three is not enough");
        HellTests.award(p, "main/pestilence");
        helper.assertTrue(death.conditions().check(level, p) == null, "Death should answer once all three have fallen, here");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "horsemen_forges", timeoutTicks = 20)
    public static void theForgesAreGoneAndTheCageStillOpensWithTheFourRings(GameTestHelper helper) {
        for (String ring : List.of("war", "famine", "pestilence", "death")) {
            helper.assertTrue(rite(helper, "forge_ring_of_" + ring) == null, "the forge of the ring of " + ring + " should be gone");
        }
        RitualRecipe uncaged = rite(helper, "summon_lucifer_uncaged");
        helper.assertTrue(uncaged != null, "no summon_lucifer_uncaged");
        for (HorsemanKind k : HorsemanKind.values()) helper.assertTrue(needs(uncaged, k.ring()), "the Cage needs the ring of " + k.id());
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "death_rite", timeoutTicks = 40)
    public static void deathsRiteCallsHimWithTheRingsToGiveBack(GameTestHelper helper) {
        cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
                boolean ok = new SummonDeathEffect().perform(level, helper.absolutePos(MID), null);
        helper.assertTrue(ok, "the rite's effect should call Death up");
        List<DeathEntity> deaths = level.getEntitiesOfClass(DeathEntity.class, new AABB(helper.absolutePos(MID)).inflate(30));
        helper.assertTrue(deaths.size() == 1, "one Death, found " + deaths.size());
        helper.assertTrue(deaths.getFirst().ringsOffered(), "he should know the rings were offered");
        cleanup(helper);
        helper.succeed();
    }

    // --- spoils ---------------------------------------------------------------------------------------

    private static void spoils(GameTestHelper helper, HorsemanKind kind) {
        HorsemanEntity h = spawn(helper, kind);
        helper.runAfterDelay(3, () -> defeat(h, helper.getLevel()));
        helper.runAfterDelay(170, () -> {
            helper.assertTrue(h.isRemoved(), kind.id() + " should be gone");
            helper.assertTrue(items(helper, kind.ring()).size() == 1, "his ring should fall");
            helper.assertTrue(items(helper, kind.trophy()).size() == 1, "his trophy should fall");
            List<HorsemanSteedEntity> steeds = helper.getLevel().getEntitiesOfClass(HorsemanSteedEntity.class,
                    new AABB(helper.absolutePos(MID)).inflate(40));
            helper.assertTrue(steeds.size() == 1 && steeds.getFirst().kind() == kind && !steeds.getFirst().isTamed(),
                    "his horse should stay behind, untamed");
            items(helper, kind.ring()).forEach(Entity::discard);
            items(helper, kind.trophy()).forEach(Entity::discard);
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "famine_spoils", timeoutTicks = 220)
    public static void famineLeavesHisRingTrophyAndHorse(GameTestHelper helper) {
        spoils(helper, HorsemanKind.FAMINE);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "pestilence_spoils", timeoutTicks = 220)
    public static void pestilenceLeavesHisRingTrophyAndHorse(GameTestHelper helper) {
        spoils(helper, HorsemanKind.PESTILENCE);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "war_spoils", timeoutTicks = 420)
    public static void warLeavesItAllAgainInARematch(GameTestHelper helper) {
        HorsemanEntity first = spawn(helper, HorsemanKind.WAR);
        helper.runAfterDelay(3, () -> defeat(first, helper.getLevel()));
        helper.runAfterDelay(170, () -> {
            helper.assertTrue(items(helper, AllItems.RING_OF_WAR.get()).size() == 1, "the first fight should leave his ring");
            WarEntity second = (WarEntity) helper.spawn(AllEntities.WAR.get(), MID);
            helper.runAfterDelay(3, () -> defeat(second, helper.getLevel()));
        });
        helper.runAfterDelay(350, () -> {
            helper.assertTrue(count(helper, AllItems.RING_OF_WAR.get()) == 2, "the rematch should leave another ring");
            helper.assertTrue(count(helper, AllItems.WAR_TROPHY.get()) == 2, "and another trophy");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(HorsemanSteedEntity.class, new AABB(helper.absolutePos(MID)).inflate(40)).size() == 2,
                    "and another horse");
            items(helper, AllItems.RING_OF_WAR.get()).forEach(Entity::discard);
            items(helper, AllItems.WAR_TROPHY.get()).forEach(Entity::discard);
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "death_spoils", timeoutTicks = 220)
    public static void deathLeavesHisOwnAndGivesBackTheThree(GameTestHelper helper) {
        DeathEntity d = spawn(helper, HorsemanKind.DEATH);
        d.setRingsOffered(true);
        helper.runAfterDelay(3, () -> defeat(d, helper.getLevel()));
        helper.runAfterDelay(170, () -> {
            helper.assertTrue(items(helper, AllItems.RING_OF_DEATH.get()).size() == 1, "his ring should fall");
            for (Item ring : List.of(AllItems.RING_OF_WAR.get(), AllItems.RING_OF_FAMINE.get(), AllItems.RING_OF_PESTILENCE.get())) {
                helper.assertTrue(items(helper, ring).size() == 1, "he should give back " + ring);
                items(helper, ring).forEach(Entity::discard);
            }
            items(helper, AllItems.RING_OF_DEATH.get()).forEach(Entity::discard);
            items(helper, AllItems.DEATH_TROPHY.get()).forEach(Entity::discard);
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "death_lost", timeoutTicks = 60)
    public static void deathGivesTheRingsBackWhenTheFightIsLost(GameTestHelper helper) {
        DeathEntity d = spawn(helper, HorsemanKind.DEATH);
        d.setRingsOffered(true);
        helper.runAfterDelay(3, () -> {
            ArenaController arena = d.arena();
            helper.assertTrue(arena != null, "he should open an arena");
            arena.beginRestore(false);
        });
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(d.isRemoved(), "he should leave once the fight is lost");
            for (Item ring : List.of(AllItems.RING_OF_WAR.get(), AllItems.RING_OF_FAMINE.get(), AllItems.RING_OF_PESTILENCE.get())) {
                helper.assertTrue(items(helper, ring).size() == 1, "he should give back " + ring);
                items(helper, ring).forEach(Entity::discard);
            }
            helper.assertTrue(items(helper, AllItems.RING_OF_DEATH.get()).isEmpty(), "but not his own");
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- War --------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "war_innocent", timeoutTicks = 60)
    public static void strikingAnInnocentUnderTheIllusionHurtsYou(GameTestHelper helper) {
        WarEntity war = spawn(helper, HorsemanKind.WAR);
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            ServerPlayer p = CurseTests.mortal(helper, MID.offset(3, 0, 0), ItemStack.EMPTY);
            war.mark(p, 200);
            WarMirageEntity innocent = AllEntities.WAR_MIRAGE.get().create(level);
            innocent.moveTo(helper.absoluteVec(MID.offset(-3, 0, 0).getBottomCenter()));
            innocent.setOwner(war.getUUID());
            innocent.setInnocent(true);
            level.addFreshEntity(innocent);
            float before = p.getHealth(), mirageBefore = innocent.getHealth(), fury = war.fury().value();
            innocent.hurt(level.damageSources().playerAttack(p), 6f);
            helper.assertTrue(p.getHealth() < before, "the blow should come back on the striker");
            helper.assertTrue(innocent.getHealth() == mirageBefore, "the innocent should take nothing");
            helper.assertTrue(war.fury().value() > fury, "War should grow angrier");
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "war_friend", timeoutTicks = 60)
    public static void strikingAFriendUnderTheIllusionHurtsYou(GameTestHelper helper) {
        WarEntity war = spawn(helper, HorsemanKind.WAR);
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            ServerPlayer striker = CurseTests.mortal(helper, MID.offset(3, 0, 0), ItemStack.EMPTY);
            ServerPlayer friend = CurseTests.mortal(helper, MID.offset(4, 0, 0), ItemStack.EMPTY);
            war.mark(striker, 200);
            float s = striker.getHealth();
            // (The test server forbids PvP, so the damage event's own check is called directly.)
            helper.assertTrue(org.papiricoh.supernaturalcraft.entity.boss.horsemen.war.WarIllusions.strikeFriend(level, striker, 5f),
                    "a marked hunter's blow on a friend should be turned");
            helper.assertTrue(striker.getHealth() < s, "the blow should come back on the striker");
            helper.assertFalse(org.papiricoh.supernaturalcraft.entity.boss.horsemen.war.WarIllusions.strikeFriend(level, friend, 5f),
                    "an unmarked hunter's blow should land as usual");
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "war_standards", timeoutTicks = 80)
    public static void breakingAStandardCoolsHisFury(GameTestHelper helper) {
        WarEntity war = spawn(helper, HorsemanKind.WAR);
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            war.raiseStandards(level, war.arena());
            helper.assertTrue(war.standing(level) == WarEntity.STANDARDS, "four standards, found " + war.standing(level));
            war.fury().set(60);
            WarStandardEntity standard = (WarStandardEntity) level.getEntity(war.standards().getFirst());
            helper.assertFalse(standard.hurt(level.damageSources().generic(), 100), "only a hunter can break one");
            ServerPlayer p = hunter(helper, "sn-test-standard");
            standard.hurt(level.damageSources().playerAttack(p), 100);
            helper.assertTrue(war.standing(level) == WarEntity.STANDARDS - 1, "one standard should be down");
            helper.assertTrue(war.fury().value() < 60, "his fury should drop: " + war.fury().value());
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- Famine ----------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "famine_eating", timeoutTicks = 60)
    public static void eatingNearFamineFeedsHim(GameTestHelper helper) {
        FamineEntity f = spawn(helper, HorsemanKind.FAMINE);
        helper.runAfterDelay(3, () -> {
            f.setHealth(f.getMaxHealth() * 0.8f);
            float before = f.getHealth();
            ServerPlayer p = hunter(helper, "sn-test-famine-eater");
            helper.assertTrue(HorsemenEvents.ateNear(p, 6), "he should notice someone eating");
            helper.assertTrue(f.getHealth() > before, "food eaten near him should heal him");
            ServerPlayer far = hunter(helper, "sn-test-famine-far");
            far.moveTo(f.getX() + 30, f.getY(), f.getZ());
            helper.assertFalse(HorsemenEvents.ateNear(far, 6), "far away, eating is safe");
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "famine_grab", timeoutTicks = 60)
    public static void hisGripBreaksWhenTheOthersHurtHim(GameTestHelper helper) {
        FamineEntity f = spawn(helper, HorsemanKind.FAMINE);
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            ServerPlayer held = hunter(helper, "sn-test-famine-held");
            ServerPlayer friend = hunter(helper, "sn-test-famine-friend");
            helper.assertTrue(f.grab(held), "he should seize a hunter");
            helper.assertTrue(held.getUUID().equals(f.grabbed()), "he should be holding them");
            f.invulnerableTime = 0;
            f.hurt(level.damageSources().playerAttack(held), 100);
            helper.assertTrue(f.grabbed() != null, "the one held can't free themself");
            for (int i = 0; i < 3 && f.grabbed() != null; i++) {
                f.invulnerableTime = 0;
                f.hurt(level.damageSources().playerAttack(friend), 100);
            }
            helper.assertTrue(f.grabbed() == null, "the others' blows should break his grip");
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- Pestilence ------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = "pestilence_antidote", timeoutTicks = 40)
    public static void theAntidoteCuresThePlagueAndWardsIt(GameTestHelper helper) {
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(5, 1, 5), new ItemStack(AllItems.ANTIDOTE_VIAL.get()));
        Plague.infect(p, 2);
        Plague.infect(p, 1);
        helper.assertTrue(Plague.stacks(p) == 3, "doses should stack: " + Plague.stacks(p));
        helper.assertTrue(p.getMaxHealth() < 20, "the plague should take hearts away");
        p.getMainHandItem().finishUsingItem(helper.getLevel(), p);
        helper.assertTrue(Plague.stacks(p) == 0, "the antidote should cure it");
        helper.assertFalse(Plague.infect(p, 1), "and keep it off for a while");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "pestilence_flies", timeoutTicks = 60)
    public static void fireDispersesASwarmAndSteelDoesNot(GameTestHelper helper) {
        var pestilence = spawn(helper, HorsemanKind.PESTILENCE);
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            FlySwarmEntity swarm = AllEntities.FLY_SWARM.get().create(level);
            swarm.moveTo(helper.absoluteVec(MID.offset(-4, 2, 0).getBottomCenter()));
            swarm.setOwner(pestilence.getUUID());
            level.addFreshEntity(swarm);
            ServerPlayer p = hunter(helper, "sn-test-flies");
            swarm.hurt(level.damageSources().playerAttack(p), 20);
            helper.assertTrue(swarm.isAlive() && !swarm.isRemoved(), "steel should pass through the flies");
            swarm.hurt(level.damageSources().inFire(), 1);
            helper.assertTrue(swarm.isRemoved(), "fire should disperse them");
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- Death ------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "death_clock", timeoutTicks = 60)
    public static void hittingDeathWindsYourClockBack(GameTestHelper helper) {
        DeathEntity d = spawn(helper, HorsemanKind.DEATH);
        helper.runAfterDelay(3, () -> {
            ServerPlayer p = hunter(helper, "sn-test-clock");
            DeathClock c = d.track(p);
            c.restore(100, -1);
            d.invulnerableTime = 0;
            d.hurt(helper.getLevel().damageSources().playerAttack(p), 5);
            helper.assertTrue(c.remaining() == c.fullTicks(), "a blow on him should wind the clock back, it shows " + c.remaining());
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "death_limbo_out", timeoutTicks = 60)
    public static void atZeroLimboAndTheLightLeadsOut(GameTestHelper helper) {
        DeathEntity d = spawn(helper, HorsemanKind.DEATH);
        ServerPlayer[] p = new ServerPlayer[1];
        helper.runAfterDelay(3, () -> {
            p[0] = hunter(helper, "sn-test-limbo-out");
            d.track(p[0]).restore(1, -1);
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(d.inLimbo(p[0]), "at zero the hunter should fall into limbo");
            LimboExitEntity exit = d.exitOf(helper.getLevel(), p[0]);
            helper.assertTrue(exit != null, "a light out should appear");
            helper.assertTrue(exit.position().distanceTo(p[0].position()) > 5, "and not at their feet");
            p[0].moveTo(exit.getX(), exit.getY(), exit.getZ());
        });
        helper.runAfterDelay(9, () -> {
            helper.assertFalse(d.inLimbo(p[0]), "reaching the light should lead out of limbo");
            helper.assertTrue(d.clock(p[0]).remaining() > d.clock(p[0]).fullTicks() - 10, "with the clock full again");
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "death_limbo_dies", timeoutTicks = 60)
    public static void limboRunsOutAndDeathReaps(GameTestHelper helper) {
        DeathEntity d = spawn(helper, HorsemanKind.DEATH);
        ServerPlayer[] p = new ServerPlayer[1];
        helper.runAfterDelay(3, () -> {
            p[0] = CurseTests.mortal(helper, MID.offset(6, 0, 0), ItemStack.EMPTY);
            d.track(p[0]).restore(0, 3);
        });
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(p[0].isDeadOrDying(), "a hunter whose limbo runs out should die");
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "death_world", timeoutTicks = 60)
    public static void theWorldOfTheDeadTurnsGreyAndBack(GameTestHelper helper) {
        DeathEntity d = spawn(helper, HorsemanKind.DEATH);
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            d.layGroundNow();
            ArenaController arena = d.arena();
            BlockPos grass = null;
            for (var e : arena.placedBlocks().entrySet()) {
                if (e.getValue().is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                    grass = e.getKey();
                    break;
                }
            }
            helper.assertTrue(grass != null, "his living world should be laid");
            d.flip(level, true);
            d.finishFlip();
            helper.assertTrue(level.getBlockState(grass).is(net.minecraft.world.level.block.Blocks.LIGHT_GRAY_CONCRETE), "the grass should go grey");
            helper.assertTrue(d.deadWorld(), "the world of the dead holds the arena");
            d.flip(level, false);
            d.finishFlip();
            helper.assertTrue(level.getBlockState(grass).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK), "and come back to life");
            cleanup(helper);
            helper.assertFalse(level.getBlockState(grass).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK), "the arena gives the ground back");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "horsemen_health", timeoutTicks = 60)
    public static void trueHealthAndPhases(GameTestHelper helper) {
        HorsemanEntity war = spawn(helper, HorsemanKind.WAR);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(Math.abs(war.trueMaxHealth() - 800f) < 1f, "War should have 800 true health, has " + war.trueMaxHealth());
            helper.assertTrue(war.maxPhase() == 3, "three phases");
            war.setHealth(war.getMaxHealth() * 0.7f);
            war.invulnerableTime = 0;
            war.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 500f);
            helper.assertTrue(war.phase() == 2 && war.state() == LuciferEntity.TRANSITION, "crossing two thirds begins phase 2");
            war.forceLook(3);
            war.beginTransition(3);
            helper.assertTrue(war.isMounted() && war.getBbHeight() > 2.5f, "he mounts his horse for the last phase");
            cleanup(helper);
            helper.succeed();
        });
    }
}
