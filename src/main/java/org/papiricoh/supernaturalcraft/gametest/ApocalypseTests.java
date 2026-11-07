package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.power.Power;
import org.papiricoh.supernaturalcraft.allegiance.power.PowerCaster;
import org.papiricoh.supernaturalcraft.allegiance.power.PowerRules;
import org.papiricoh.supernaturalcraft.apocalypse.ApocalypseMigration;
import org.papiricoh.supernaturalcraft.apocalypse.blood.BloodData;
import org.papiricoh.supernaturalcraft.apocalypse.blood.BloodRules;
import org.papiricoh.supernaturalcraft.apocalypse.blood.Bloods;
import org.papiricoh.supernaturalcraft.apocalypse.blood.PanicRoom;
import org.papiricoh.supernaturalcraft.apocalypse.blood.PanicRoomDoorBlockEntity;
import org.papiricoh.supernaturalcraft.apocalypse.ruby.RubyData;
import org.papiricoh.supernaturalcraft.apocalypse.ruby.RubyEntity;
import org.papiricoh.supernaturalcraft.apocalypse.ruby.RubyFight;
import org.papiricoh.supernaturalcraft.apocalypse.ruby.RubyRules;
import org.papiricoh.supernaturalcraft.apocalypse.ruby.Rubys;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealSavedData;
import org.papiricoh.supernaturalcraft.entity.boss.alastair.AlastairEntity;
import org.papiricoh.supernaturalcraft.entity.boss.alastair.AlastairSummoning;
import org.papiricoh.supernaturalcraft.hunter.DevilsTrapBlock;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllRecipes;
import org.papiricoh.supernaturalcraft.ritual.RitualRecipe;

import java.util.List;

/**
 * v0.40, the core: the road's new gates (season 4 ends with Lilith, season 5 with Lucifer), the demon blood and its powers,
 * the Panic Room's cure, Ruby (only a demon-killing blade ends her; she leaves the Colt), Alastair breaking out of the trap and
 * the migration of a hunter from before v0.40.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class ApocalypseTests {

    static RitualRecipe rite(GameTestHelper helper, String id) {
        for (var h : helper.getLevel().getRecipeManager().getAllRecipesFor(AllRecipes.RITUAL.get())) {
            if (h.id().getPath().equals("ritual/" + id)) return h.value();
        }
        return null;
    }

    static boolean requires(RitualRecipe r, String adv) {
        return r.conditions().requiresAdvancement().contains(ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, adv));
    }

    @GameTest(template = SNGameTests.SMALL, batch = "apocalypse_road", timeoutTicks = 20)
    public static void theRoadRunsAsTheSeasonsDo(GameTestHelper helper) {
        for (String h : List.of("summon_war", "summon_famine", "summon_pestilence", "open_hell_rift", "open_hell_rift_nether")) {
            helper.assertTrue(requires(rite(helper, h), "main/lucifer_rising"), h + " opens after Lilith");
        }
        helper.assertTrue(requires(rite(helper, "summon_lucifer"), "main/pale_rider"), "Lucifer waits for Death");
        helper.assertTrue(requires(rite(helper, "forge_key_to_the_cage"), "main/pale_rider"), "the Key needs Death's ring");
        helper.assertTrue(requires(rite(helper, "summon_raphael"), "main/devil_went_down"), "Raphael waits for Lucifer");
        RitualRecipe lilith = rite(helper, "summon_lilith");
        helper.assertTrue(requires(lilith, "main/on_the_head_of_a_pin"), "Lilith waits for Alastair");
        helper.assertTrue(lilith.conditions().sealsBroken() == 11, "and for eleven seals");
        helper.assertTrue(rite(helper, "open_hell_rift").activator().test(new ItemStack(AllItems.LAST_SEAL.get())), "the Last Seal opens Hell");
        helper.assertTrue(rite(helper, "open_hell_rift").activator().test(new ItemStack(AllItems.KEY_TO_THE_CAGE.get())), "as the Key still does");
        helper.assertFalse(rite(helper, "open_hell_rift").consumeActivator(), "and is not spent");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "apocalypse_blood", timeoutTicks = 40)
    public static void theBloodGivesItsPowersToAnyoneWhileItLasts(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(5, 1, 5), ItemStack.EMPTY);
        Bloods.update(p, d -> BloodData.EMPTY);
        helper.assertTrue(PowerCaster.cast(p, Power.BLOOD_PUSH, -1) == PowerRules.Verdict.NO_BLOOD, "no blood, no shove");
        Bloods.drink(p);
        BloodData d = Bloods.get(p);
        helper.assertTrue(d.doses() == 1 && d.blood() > 0, "a dose is in the body");
        helper.assertTrue(PowerCaster.cast(p, Power.BLOOD_PUSH, -1) == PowerRules.Verdict.OK, "a hunter shoves with one dose");
        helper.assertTrue(Bloods.get(p).blood() < d.blood(), "the shove is paid in blood");
        helper.assertTrue(PowerCaster.cast(p, Power.BLOOD_SMITE, -1) == PowerRules.Verdict.NO_BLOOD, "the raised hand needs fifteen doses");
        Bloods.update(p, x -> BloodData.EMPTY);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "apocalypse_panic", timeoutTicks = 60)
    public static void thePanicRoomCuresWhoIsShutInIt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long clock = level.getDayTime();
        // An iron box, inside 3×3×3 (x 4-6, y 1-3, z 4-6), a devil's trap on its ceiling, the door in its north wall.
        for (int x = 3; x <= 7; x++)
            for (int y = 0; y <= 4; y++)
                for (int z = 3; z <= 7; z++) {
                    boolean inside = x >= 4 && x <= 6 && y >= 1 && y <= 3 && z >= 4 && z <= 6;
                    helper.setBlock(new BlockPos(x, y, z), inside ? Blocks.AIR.defaultBlockState() : Blocks.IRON_BLOCK.defaultBlockState());
                }
        helper.setBlock(new BlockPos(5, 3, 5), AllBlocks.DEVILS_TRAP.get().defaultBlockState().setValue(DevilsTrapBlock.FACE, DevilsTrapBlock.Face.CEILING));
        var door = AllBlocks.PANIC_ROOM_DOOR.get().defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH);
        helper.setBlock(new BlockPos(5, 1, 3), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(new BlockPos(5, 2, 3), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(5, 1, 5), ItemStack.EMPTY);
        Bloods.update(p, d -> new BloodData(0f, 0.9f, 3, 0));
        BlockPos lower = helper.absolutePos(new BlockPos(5, 1, 3));
        helper.assertTrue(PanicRoom.use(level, lower, level.getBlockState(lower), p), "the door locks the hunter in");
        helper.assertTrue(level.getBlockEntity(lower) instanceof PanicRoomDoorBlockEntity be && be.locked(), "locked");
        helper.assertTrue(Bloods.get(p).inPanicRoom(), "they are in the room");
        PanicRoom.tick(p);
        helper.assertTrue(Bloods.get(p).dependency() > 0, "not cured at once");
        level.setDayTime(clock + BloodRules.DAY_TICKS + 10);
        PanicRoom.tick(p);
        BloodData after = Bloods.get(p);
        level.setDayTime(clock);
        helper.assertTrue(after.dependency() == 0 && !after.inPanicRoom(), "a day inside cures them: " + after);
        helper.assertTrue(after.doses() == 3, "what they learnt stays");
        helper.assertFalse(level.getBlockEntity(lower) instanceof PanicRoomDoorBlockEntity be2 && be2.locked(), "and the door lets them out");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "apocalypse_ruby", timeoutTicks = 100)
    public static void onlyADemonKillingBladeEndsRubyAndSheLeavesTheColt(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        Vec3 at = helper.absoluteVec(new Vec3(24, 1, 24));
        RubyEntity ruby = RubyFight.spawn(level, at, 1);
        helper.assertTrue(ruby != null, "Ruby comes");
        ServerPlayer plain = CurseTests.mortal(helper, new BlockPos(22, 1, 24), new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
        ServerPlayer bane = CurseTests.mortal(helper, new BlockPos(26, 1, 24), new ItemStack(AllItems.RUBYS_KNIFE.get()));
        helper.runAfterDelay(RubyEntity.INTRO_TICKS + 5, () -> {
            ruby.hurt(level.damageSources().playerAttack(plain), 100_000f);
            helper.assertTrue(ruby.isAlive() && ruby.getHealth() >= 1, "a sword leaves her standing");
            ruby.hurt(level.damageSources().playerAttack(bane), 100_000f);
            helper.assertTrue(!ruby.isAlive(), "her own knife ends her");
        });
        helper.runAfterDelay(RubyEntity.INTRO_TICKS + 15, () -> {
            List<ItemEntity> colts = level.getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(6), e -> e.getItem().is(AllItems.THE_COLT.get()));
            helper.assertTrue(!colts.isEmpty(), "she leaves the Colt");
            colts.forEach(ItemEntity::discard);
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "apocalypse_alastair", timeoutTicks = 200)
    public static void alastairBreaksOutOfTheTrapAtTheSecondPhase(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        AlastairEntity boss = AlastairSummoning.summon(level, helper.absolutePos(new BlockPos(24, 1, 20)), null);
        helper.assertTrue(boss != null, "Alastair comes");
        boss.chain(true);
        helper.assertTrue(boss.bound(), "chained in the trap");
        helper.runAfterDelay(60, () -> boss.beginTransition(2));
        helper.runAfterDelay(60 + AlastairEntity.TRANSITION + 10, () -> {
            helper.assertFalse(boss.bound(), "the second phase breaks the chains");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.SMALL, batch = "apocalypse_migration", timeoutTicks = 20)
    public static void aHunterPastTheOldLuciferIsGivenTheShackleOnce(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var seals = SealSavedData.get(level.getServer());
        var before = seals.snapshot();
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(2, 1, 2), ItemStack.EMPTY);
        Rubys.update(p, d -> RubyData.EMPTY);
        for (String a : List.of("main/yellow_eyed", "main/lucifer_rising", "main/devil_went_down")) HellTests.award(p, a);
        ApocalypseMigration.migrate(p);
        RubyData d = Rubys.get(p);
        helper.assertTrue(d.has(RubyData.MIGRATED), "migrated");
        helper.assertTrue(Rubys.step(p) == RubyRules.Step.DONE, "Ruby is behind them");
        helper.assertTrue(d.has(RubyData.SHACKLE_GIVEN), "the Shackle the new rite wants was given");
        helper.assertTrue(seals.echo(), "the world past the old Lucifer has its Cage shut");
        Rubys.update(p, x -> x.withStep(0));
        ApocalypseMigration.migrate(p);
        helper.assertTrue(Rubys.step(p) == RubyRules.Step.NONE, "only once");
        seals.restore(before);
        helper.succeed();
    }
}
