package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.azazel.AzazelEntity;
import org.papiricoh.supernaturalcraft.entity.boss.azazel.AzazelSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.azazel.ColtRailBlock;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.hunter.DevilsTrapBlock;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllRecipes;
import org.papiricoh.supernaturalcraft.ritual.RitualRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Azazel: his health and two phases, Samuel Colt's rails, the Colt, his ritual and his blood. Each test in its own batch. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class AzazelTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    private static AzazelEntity spawn(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        return helper.spawn(AllEntities.AZAZEL.get(), MID);
    }

    private static float smite(AzazelEntity a, ServerLevel level, float amount) {
        float before = a.getHealth();
        a.invulnerableTime = 0;
        a.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, null), amount);
        return before - a.getHealth();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "azazel_health", timeoutTicks = 60)
    public static void fourHundredHealthInASulfurArena(GameTestHelper helper) {
        AzazelEntity a = spawn(helper);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(Math.abs(a.getMaxHealth() - 400f) < 0.5f, "health should be 400, is " + a.getMaxHealth());
            ArenaController arena = a.arena();
            helper.assertTrue(arena != null && arena.theme() == ArenaTheme.SULFUR, "he should open a sulphur arena");
            helper.assertTrue(a.maxPhase() == 2, "two phases");
            helper.assertTrue(a.trap().built() && a.trap().rails().size() > 30, "Colt's rails were not laid");
            for (BlockPos p : a.trap().rails()) {
                helper.assertTrue(helper.getLevel().getBlockState(p).is(AllBlocks.COLT_RAIL.get()), "no rail at " + p.toShortString());
            }
            helper.assertFalse(a.isHeld(), "he should have stepped out of the rails he was spawned in");
            BossTests.cleanup(helper);
            helper.assertFalse(helper.getLevel().getBlockState(a.trap().rails().getFirst()).is(AllBlocks.COLT_RAIL.get()),
                    "the rails should go when the arena is put back");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "azazel_threshold", timeoutTicks = 60)
    public static void heavyHitStopsAtHalf(GameTestHelper helper) {
        AzazelEntity a = spawn(helper);
        helper.runAfterDelay(3, () -> {
            a.setHealth(a.getMaxHealth() * 0.52f);
            smite(a, helper.getLevel(), 500f);
            helper.assertTrue(Math.abs(a.getHealth() / a.getMaxHealth() - 0.5f) < 0.001f,
                    "health should stop at half, is " + a.getHealth() / a.getMaxHealth());
            helper.assertTrue(a.phase() == 2 && a.state() == LuciferEntity.TRANSITION, "crossing it should begin phase 2");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "azazel_rails", timeoutTicks = 80)
    public static void railTrapHoldsHimAndRecharges(GameTestHelper helper) {
        AzazelEntity a = spawn(helper);
        helper.runAfterDelay(3, () -> {
            Vec3 c = a.arena().centerVec();
            a.teleportTo(c.x + 1, c.y, c.z + 1);
        });
        helper.runAfterDelay(6, () -> {
            ServerLevel level = helper.getLevel();
            helper.assertTrue(a.hasEffect(AllMobEffects.TRAPPED) && a.isHeld(), "the rails should hold him");
            helper.assertFalse(a.trap().charged(), "the rails should be spent");
            BlockState rail = level.getBlockState(a.trap().rails().getFirst());
            helper.assertFalse(rail.getValue(ColtRailBlock.CHARGED), "a spent rail should be cold");
            float dealt = smite(a, level, 10f);
            helper.assertTrue(Math.abs(dealt - 15f) < 0.1f, "held, a holy blow of 10 should take 15, took " + dealt);
            a.trap().forceRecharge(level, a.arena());
            helper.assertTrue(a.trap().charged(), "the rails should charge again");
            helper.assertTrue(level.getBlockState(a.trap().rails().getFirst()).getValue(ColtRailBlock.CHARGED), "a charged rail should glow");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "azazel_path", timeoutTicks = 40)
    public static void chargedRailsBlockOnlyHim(GameTestHelper helper) {
        AzazelEntity a = spawn(helper);
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            BlockPos p = a.trap().rails().getFirst();
            BlockState charged = level.getBlockState(p);
            helper.assertTrue(charged.getBlockPathType(level, p, a) == PathType.BLOCKED, "he must not path across charged iron");
            helper.assertTrue(charged.getBlockPathType(level, p, null) == null, "anyone else walks over it");
            BlockState cold = charged.setValue(ColtRailBlock.CHARGED, false);
            helper.assertTrue(cold.getBlockPathType(level, p, a) == null, "spent iron does not stop him");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "azazel_painted", timeoutTicks = 80)
    public static void aPaintedTrapDoesNotHoldHim(GameTestHelper helper) {
        AzazelEntity a = spawn(helper);
        BlockPos[] at = new BlockPos[1];
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            at[0] = a.blockPosition();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int part = (dz + 1) * 3 + (dx + 1);
                    level.setBlock(at[0].offset(dx, 0, dz), AllBlocks.DEVILS_TRAP.get().defaultBlockState().setValue(DevilsTrapBlock.PART, part), 3);
                }
            }
        });
        helper.runAfterDelay(30, () -> {
            helper.assertFalse(a.hasEffect(AllMobEffects.TRAPPED), "a painted trap should not hold him");
            helper.assertFalse(helper.getLevel().getBlockState(at[0]).is(AllBlocks.DEVILS_TRAP.get()), "he should burn the trap off the floor");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "azazel_colt", timeoutTicks = 60)
    public static void theColtStunsHimAndDealsSixty(GameTestHelper helper) {
        AzazelEntity a = spawn(helper);
        helper.runAfterDelay(3, () -> {
            float before = a.getHealth();
            a.invulnerableTime = 0;
            a.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.COLT, null), 60f);
            float dealt = before - a.getHealth();
            helper.assertTrue(Math.abs(dealt - 60f) < 0.5f, "a Colt round should take exactly 60, took " + dealt);
            helper.assertTrue(a.hasEffect(AllMobEffects.STUNNED), "a Colt round should stagger him");
            a.setSmoke(true);
            a.invulnerableTime = 0;
            float smokeBefore = a.getHealth();
            a.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 10f);
            helper.assertTrue(a.getHealth() == smokeBefore, "as smoke he should be untouchable");
            a.invulnerableTime = 0;
            a.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.COLT, null), 60f);
            helper.assertFalse(a.isSmoke(), "a Colt round should knock him out of the smoke");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "azazel_death", timeoutTicks = 400)
    public static void deathLeavesHisBlood(GameTestHelper helper) {
        AzazelEntity a = spawn(helper);
        helper.runAfterDelay(3, () -> a.beginTransition(2));
        helper.runAfterDelay(5, () -> a.setHealth(5f));
        helper.runAfterDelay(LuciferEntity.TRANSITION_TICKS + 8, () -> {
            smite(a, helper.getLevel(), 50f);
            helper.assertTrue(a.isAlive() && a.state() == LuciferEntity.DYING, "the killing blow should start his death");
        });
        helper.runAfterDelay(LuciferEntity.TRANSITION_TICKS + 8 + AzazelEntity.AZAZEL_DEATH_TICKS + 10, () -> {
            helper.assertTrue(a.isRemoved(), "he is still here after his death");
            AABB box = new AABB(helper.absolutePos(MID)).inflate(40);
            int blood = 0;
            boolean trophy = false;
            for (ItemEntity e : helper.getLevel().getEntitiesOfClass(ItemEntity.class, box)) {
                if (e.getItem().is(AllItems.AZAZEL_BLOOD.get())) blood += e.getItem().getCount();
                if (e.getItem().is(AllItems.AZAZEL_TROPHY.get())) trophy = true;
                e.discard();
            }
            helper.assertTrue(blood == 2, "two vials of his blood, found " + blood);
            helper.assertTrue(trophy, "no trophy among his spoils");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "azazel_ritual", timeoutTicks = 120)
    public static void theRitualCallsHimUpOutsideTheRails(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        BlockPos altar = helper.absolutePos(MID);
        helper.assertTrue(AzazelSummoning.summon(level, altar, null), "the summoning failed");
        List<AzazelEntity> found = level.getEntitiesOfClass(AzazelEntity.class, new AABB(altar).inflate(16));
        helper.assertTrue(found.size() == 1 && found.getFirst().state() == LuciferEntity.EMERGING, "he should be taking shape");
        AzazelEntity a = found.getFirst();
        helper.assertFalse(AzazelSummoning.summon(level, altar, null), "a second summoning should fail while he is here");
        helper.runAfterDelay(AzazelEntity.AZAZEL_EMERGE_TICKS + 5, () -> {
            helper.assertTrue(a.trap().built(), "the rails should rise while he takes shape");
            helper.assertTrue(a.state() != LuciferEntity.EMERGING && !a.isHeld(), "he should stand outside the rails");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.SMALL, batch = "azazel_recipes", timeoutTicks = 20)
    public static void hisRitualAndTheKey(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager().getAllRecipesFor(AllRecipes.RITUAL.get());
        Map<String, List<String>> bySignature = new HashMap<>();
        RitualRecipe summon = null, key = null;
        for (var holder : recipes) {
            RitualRecipe r = holder.value();
            bySignature.computeIfAbsent(signature(r), k -> new ArrayList<>()).add(holder.id().toString());
            if (holder.id().getPath().equals("ritual/summon_azazel")) summon = r;
            if (holder.id().getPath().equals("ritual/forge_key_to_the_cage")) key = r;
        }
        helper.assertTrue(summon != null && key != null, "missing summon_azazel or forge_key_to_the_cage");
        bySignature.forEach((sig, ids) -> helper.assertTrue(ids.size() == 1, "rituals with the same offerings and fire: " + ids));
        helper.assertTrue(summon.manaCost() <= 100, "the first boss must be callable with a beginner's mana");
        int blood = 0, demon = 0;
        for (Ingredient i : key.ingredients()) {
            if (i.test(new ItemStack(AllItems.AZAZEL_BLOOD.get()))) blood++;
            if (i.test(new ItemStack(AllItems.DEMON_BLOOD.get()))) demon++;
        }
        helper.assertTrue(blood == 2 && demon == 0, "the Key should take two of Azazel's blood and no demon blood");
        helper.succeed();
    }

    private static String signature(RitualRecipe r) {
        List<String> items = new ArrayList<>();
        for (Ingredient i : r.ingredients()) items.add(firstItem(i));
        items.sort(String::compareTo);
        return r.pattern().location() + "|" + firstItem(r.activator()) + "|" + items;
    }

    private static String firstItem(Ingredient i) {
        ItemStack[] stacks = i.getItems();
        return stacks.length == 0 ? "?" : net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stacks[0].getItem()).toString();
    }
}
