package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.Channel;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielBalance;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielDoubleEntity;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielEntity;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielSpoils;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.PieProjectile;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.arena.ChannelLayouts;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllRecipes;
import org.papiricoh.supernaturalcraft.reward.gabriel.GabrielBladeItem;
import org.papiricoh.supernaturalcraft.reward.gabriel.TricksterCandyItem;
import org.papiricoh.supernaturalcraft.reward.gabriel.TricksterRemoteItem;
import org.papiricoh.supernaturalcraft.ritual.RitualConditions;
import org.papiricoh.supernaturalcraft.ritual.RitualRecipe;
import org.papiricoh.supernaturalcraft.trickster.PrankRules;
import org.papiricoh.supernaturalcraft.trickster.TricksterLedger;
import org.papiricoh.supernaturalcraft.trickster.TricksterMarks;
import org.papiricoh.supernaturalcraft.trickster.TricksterPranks;
import org.papiricoh.supernaturalcraft.weapon.Holy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gabriel, the Trickster (v0.14): his rite, his health, the four sets written and given back, the laugh track and the
 * applause, the quiz, the heart monitor, the nurses, the spokesmen, the factions' twists, his spoils, the remote, the
 * blade and the candy, and the pranks (the third puts you on his trail; none of them touches a block or an inventory).
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class GabrielTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);
    /** Ticks for him to open TV Land and write the sitcom round him. */
    private static final int SETTLE = 12;

    private static void cleanup(GameTestHelper helper) {
        BossTests.cleanup(helper);
        for (Entity e : helper.getLevel().getAllEntities()) {
            if (e instanceof GabrielDoubleEntity || e instanceof PieProjectile) e.discard();
        }
    }

    private static GabrielEntity spawn(GameTestHelper helper) {
        cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        return helper.spawn(AllEntities.GABRIEL.get(), MID);
    }

    private static ServerPlayer hunter(GameTestHelper helper, String name) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.setGameMode(GameType.SURVIVAL);
        p.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN);
        Vec3 at = helper.absoluteVec(MID.offset(6, 0, 6).getBottomCenter());
        p.moveTo(at.x, at.y, at.z);
        return p;
    }

    /** A hunter who can be hurt, standing at {@code at} (absolute). */
    private static ServerPlayer mortalAt(GameTestHelper helper, Vec3 at) {
        ServerPlayer p = CurseTests.mortal(helper, MID, ItemStack.EMPTY);
        p.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN);
        p.moveTo(at.x, at.y, at.z);
        return p;
    }

    private static Vec3 onTop(BlockPos block) {
        return Vec3.atBottomCenterOf(block.above());
    }

    private static int count(GameTestHelper helper, Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(MID)).inflate(40),
                e -> e.isAlive() && e.getItem().is(item)).stream().mapToInt(e -> e.getItem().getCount()).sum();
    }

    private static void discardItems(GameTestHelper helper) {
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(MID)).inflate(40)).forEach(Entity::discard);
    }

    /** What a hit of {@code amount} from {@code source} takes from his vanilla health before his own vulnerabilities. */
    private static float base(GabrielEntity g, net.minecraft.world.damagesource.DamageSource source, float amount) {
        float mult = Holy.isHoly(source) ? 1f : SNConfig.GABRIEL_MUNDANE_MULTIPLIER.get().floatValue();
        return amount * mult / (g.trueMaxHealth() / g.getMaxHealth());
    }

    // --- the rite --------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, batch = "gabriel_rite", timeoutTicks = 20)
    public static void hisRiteWantsTheTricksterSightedAndEatsTheBait(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // The batches share the world's clock: put it back as it was when done.
        long clock = level.getDayTime();
        level.setDayTime(18000);
        RitualRecipe summon = null, bait = null;
        for (var holder : level.getRecipeManager().getAllRecipesFor(AllRecipes.RITUAL.get())) {
            if (holder.id().getPath().equals("ritual/summon_gabriel")) summon = holder.value();
            if (holder.id().getPath().equals("ritual/sweeten_the_pot")) bait = holder.value();
        }
        helper.assertTrue(summon != null && bait != null, "no summon_gabriel or sweeten_the_pot");
        RitualRecipe r = summon, b = bait;
        helper.assertTrue(r.activator().test(new ItemStack(AllItems.TRICKSTER_BAIT.get())), "the bait lights his rite");
        helper.assertTrue(r.consumeActivator(), "and the rite eats it (one bait, one try)");
        helper.assertTrue(r.conditions().time() == RitualConditions.Time.NIGHT, "by night");
        helper.assertTrue(r.conditions().dimension().isPresent() && r.conditions().dimension().get().equals(net.minecraft.world.level.Level.OVERWORLD),
                "in the Overworld");
        boolean oil = false;
        for (Ingredient i : r.ingredients()) oil |= i.test(new ItemStack(AllItems.HOLY_OIL.get()));
        helper.assertTrue(oil, "a ring of holy oil round the sweets");
        ServerPlayer p = HellTests.ritualist(helper, "sn-test-gabriel-rite");
        helper.runAfterDelay(1, () -> {
            HellTests.award(p, "main/devil_went_down");
            helper.assertTrue("message.supernaturalcraft.ritual.not_ready".equals(r.conditions().check(level, p)),
                    "Lucifer beaten is not enough: he must have been sighted");
            helper.assertTrue("message.supernaturalcraft.ritual.not_ready".equals(b.conditions().check(level, p)),
                    "nor can the bait be made before");
            HellTests.award(p, "main/trickster_sighted");
            helper.assertTrue(r.conditions().check(level, p) == null, "sighted, by night, here: the bait will do");
            helper.assertTrue(b.conditions().check(level, p) == null, "and the bait can be made");
            level.setDayTime(clock + 1);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "gabriel_summon", timeoutTicks = 40)
    public static void theBaitOpensTvLandInARingOfHolyOil(GameTestHelper helper) {
        cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        helper.assertTrue(new org.papiricoh.supernaturalcraft.ritual.effect.SummonGabrielEffect().perform(level, helper.absolutePos(MID), null),
                "the bait should bring him");
        List<GabrielEntity> found = level.getEntitiesOfClass(GabrielEntity.class, new AABB(helper.absolutePos(MID)).inflate(30));
        helper.assertTrue(found.size() == 1, "one Gabriel, found " + found.size());
        helper.assertTrue(found.getFirst().state() == LuciferEntity.EMERGING, "he makes his entrance first");
        helper.assertTrue(found.getFirst().costume() == Channel.Costume.JACKET, "in his own jacket");
        int fire = 0;
        BlockPos c = helper.absolutePos(MID);
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-4, 0, -4), c.offset(4, 0, 4))) {
            if (level.getBlockState(pos).is(org.papiricoh.supernaturalcraft.registry.AllBlocks.HOLY_OIL_FIRE.get())) fire++;
        }
        helper.assertTrue(fire >= 12, "a ring of holy oil burns round the altar, " + fire + " fires");
        cleanup(helper);
        helper.assertFalse(level.getBlockState(c.offset(3, 0, 0)).is(org.papiricoh.supernaturalcraft.registry.AllBlocks.HOLY_OIL_FIRE.get()),
                "and the arena puts it out when it closes");
        helper.succeed();
    }

    // --- his health ------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "gabriel_health", timeoutTicks = 60)
    public static void fifteenHundredTrueHealthInFourQuarters(GameTestHelper helper) {
        GabrielEntity g = spawn(helper);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(Math.abs(g.trueMaxHealth() - 1500f) < 1f, "about 1500 true health alone, has " + g.trueMaxHealth());
            helper.assertTrue(g.arena() != null, "TV Land is open");
            g.setHealth(g.getMaxHealth() * 0.76f);
            g.setChannelClock(GabrielBalance.LAUGH_TICKS);
            g.invulnerableTime = 0;
            g.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 35f);
            helper.assertTrue(Math.abs(g.getHealth() - g.getMaxHealth() * 0.75f) < 0.01f, "a blow stops at 75%, is " + g.getHealth() / g.getMaxHealth());
            helper.assertTrue(g.phase() == 2 && g.state() == LuciferEntity.TRANSITION, "and the commercial break into CH 5 begins");
            helper.assertTrue(g.costume() == Channel.Costume.SWEATER, "the costume changes halfway through the break, not before");
        });
        helper.runAfterDelay(3 + LuciferEntity.TRANSITION_TICKS / 2 + 2, () -> {
            helper.assertTrue(g.costume() == Channel.Costume.TUXEDO, "halfway through, the host's tuxedo");
            helper.assertTrue(g.shownChannel() == Channel.GAME_SHOW, "CH 5 on the air");
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- the sets --------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "gabriel_sets", timeoutTicks = 60)
    public static void everyChannelWritesItsSetAndTheArenaGivesItAllBack(GameTestHelper helper) {
        GabrielEntity g = spawn(helper);
        helper.runAfterDelay(SETTLE, () -> {
            ServerLevel level = helper.getLevel();
            helper.assertTrue(g.ground() != null && !g.writingSet(), "the sitcom is written round him");
            BlockPos door = g.ground().at(ChannelLayouts.DOORS.get(1));
            helper.assertTrue(door != null && level.getBlockState(door).is(Blocks.OAK_DOOR), "a door in the sitcom's back flat");
            g.layChannelNow(Channel.GAME_SHOW);
            BlockPos red = g.platformCentre(0), blue = g.platformCentre(1), yellow = g.platformCentre(2);
            helper.assertTrue(level.getBlockState(red).is(Blocks.RED_CONCRETE) && level.getBlockState(blue).is(Blocks.BLUE_CONCRETE)
                    && level.getBlockState(yellow).is(Blocks.YELLOW_CONCRETE), "the game show's three platforms");
            helper.assertTrue(level.getBlockState(red.below(ChannelLayouts.PIT_DEPTH)).is(Blocks.SLIME_BLOCK), "over a foam pit");
            helper.assertFalse(level.getBlockState(door).is(Blocks.OAK_DOOR), "on the same ground the sitcom stood on");
            helper.assertTrue(g.costume() == Channel.Costume.TUXEDO, "the host's tuxedo");
            g.layChannelNow(Channel.HOSPITAL);
            ChannelLayouts.Spot bed = ChannelLayouts.BEDS.getFirst();
            BlockPos blanket = g.ground().at(bed.dx() + Integer.signum(bed.dx()), 1, bed.dz());
            helper.assertTrue(level.getBlockState(blanket).is(Blocks.LIGHT_BLUE_WOOL), "the ward's beds");
            helper.assertFalse(level.getBlockState(red).is(Blocks.RED_CONCRETE), "the platforms are gone");
            helper.assertTrue(level.getBlockState(red.below()).isSolid(), "and their pits filled in");
            g.layChannelNow(Channel.COMMERCIAL);
            ChannelLayouts.Spot podium = ChannelLayouts.PODIUMS.get(2);
            BlockPos top = g.ground().at(podium.dx(), 1, podium.dz());
            helper.assertTrue(level.getBlockState(top).is(Blocks.SMOOTH_QUARTZ), "the commercial's podiums");
            helper.assertTrue(g.costume() == Channel.Costume.SUIT, "the spokesman's suit");
            cleanup(helper);
            helper.assertTrue(level.getBlockState(top).isAir(), "the arena takes the podium back");
            helper.assertTrue(level.getBlockState(red).is(Blocks.STONE), "and gives the floor back");
            helper.succeed();
        });
    }

    // --- CH 2: the sitcom ------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "gabriel_laugh", timeoutTicks = 60)
    public static void theLaughTrackMakesHimUntouchableAndTheQuietOpensHimUp(GameTestHelper helper) {
        GabrielEntity g = spawn(helper);
        helper.runAfterDelay(SETTLE, () -> {
            var smite = AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null);
            g.setChannelClock(5);
            helper.assertTrue(g.laughingNow(), "the LAUGH sign starts lit");
            float before = g.getHealth();
            g.invulnerableTime = 0;
            helper.assertFalse(g.hurt(smite, 10f), "the audience is laughing: nothing touches him");
            helper.assertTrue(g.getHealth() == before, "his health didn't move");
            g.setChannelClock(GabrielBalance.LAUGH_TICKS + 5);
            helper.assertFalse(g.laughingNow(), "the sign goes dark");
            g.setHealth(g.getMaxHealth() * 0.9f);
            before = g.getHealth();
            g.invulnerableTime = 0;
            g.hurt(smite, 10f);
            float taken = before - g.getHealth(), expected = base(g, smite, 10f) * GabrielBalance.QUIET_VULNERABILITY;
            helper.assertTrue(Math.abs(taken - expected) < 0.01f, "in the quiet he takes x1.3: " + taken + " vs " + expected);
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "gabriel_applause", timeoutTicks = 80)
    public static void theApplauseHealsHimIfNobodyHitsHimInTheQuiet(GameTestHelper helper) {
        GabrielEntity g = spawn(helper);
        float[] at = new float[1];
        helper.runAfterDelay(SETTLE, () -> {
            g.setHealth(g.getMaxHealth() * 0.9f);
            at[0] = g.getHealth();
            g.setChannelClock(GabrielBalance.LAUGH_TICKS + GabrielBalance.APPLAUSE_AFTER - 3);
        });
        helper.runAfterDelay(SETTLE + 1, () -> helper.assertTrue(g.getHealth() == at[0], "not before the applause"));
        helper.runAfterDelay(SETTLE + 6, () -> {
            float healed = g.getHealth() - at[0];
            helper.assertTrue(Math.abs(healed - GabrielBalance.APPLAUSE_HEAL * g.getMaxHealth()) < 0.01f,
                    "nobody hit him: a bow and 2% back, healed " + healed / g.getMaxHealth());
            // A hunter's blow in the next quiet spell keeps the audience quiet.
            ServerPlayer p = mortalAt(helper, g.position().add(2, 0, 0));
            int cycle = GabrielBalance.LAUGH_TICKS + GabrielBalance.QUIET_TICKS;
            g.setChannelClock(cycle + GabrielBalance.LAUGH_TICKS + GabrielBalance.APPLAUSE_AFTER - 3);
            g.invulnerableTime = 0;
            g.hurt(p.damageSources().playerAttack(p), 2f);
            at[0] = g.getHealth();
        });
        helper.runAfterDelay(SETTLE + 6 + 10, () -> {
            helper.assertTrue(g.getHealth() == at[0], "a hit in the quiet: no applause yet");
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "gabriel_peel", timeoutTicks = 60)
    public static void aBananaPeelTripsAHunter(GameTestHelper helper) {
        GabrielEntity g = spawn(helper);
        helper.runAfterDelay(SETTLE, () -> {
            ServerLevel level = helper.getLevel();
            BlockPos spot = BlockPos.containing(g.position().add(7, 0, 7));
            helper.assertTrue(g.dropPeel(level, spot), "a peel lies on the floor");
            helper.assertTrue(level.getBlockState(spot).is(Blocks.YELLOW_CARPET), "a yellow peel");
            ServerPlayer p = mortalAt(helper, Vec3.atBottomCenterOf(spot));
            helper.assertTrue(g.slipCheck(p), "stepping on it is a slip");
            helper.assertTrue(p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "it slows you");
            helper.assertFalse(level.getBlockState(spot).is(Blocks.YELLOW_CARPET), "and the peel is gone");
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- CH 5: the game show ---------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "gabriel_quiz", timeoutTicks = 120)
    public static void theQuizPunishesTheWrongAndStunsHimForTheRight(GameTestHelper helper) {
        GabrielEntity g = spawn(helper);
        ServerPlayer[] who = new ServerPlayer[3];
        int[] wrongPlatform = new int[1];
        helper.runAfterDelay(SETTLE, () -> {
            g.forceLook(2);
            g.layChannelNow(Channel.GAME_SHOW);
        });
        helper.runAfterDelay(SETTLE + 2, () -> {
            for (int i = 0; i < 3; i++) who[i] = mortalAt(helper, g.position().add(10, 0, 10));
            g.startQuiz(List.of(who));
            helper.assertTrue(g.quizActive() && g.quizQuestion() >= 0, "a question is up");
            int right = g.quizRightPlatform();
            wrongPlatform[0] = (right + 1) % 3;
            Vec3 ok = onTop(g.platformCentre(right)), wrong = onTop(g.platformCentre(wrongPlatform[0]));
            who[0].moveTo(ok.x, ok.y, ok.z);
            who[1].moveTo(wrong.x, wrong.y, wrong.z);
            helper.assertTrue(g.platformUnder(who[0]) == right && g.platformUnder(who[1]) == wrongPlatform[0], "on their platforms");
            helper.assertTrue(g.platformUnder(who[2]) == -1, "and one on none");
            g.endQuiz();
            helper.assertFalse(g.quizActive(), "the buzzer");
            helper.assertTrue(who[0].hasEffect(MobEffects.DAMAGE_BOOST), "right: strength");
            helper.assertTrue(g.isStunned(), "right: he reels");
            helper.assertTrue(who[1].getHealth() < who[1].getMaxHealth(), "wrong: punished");
            helper.assertTrue(helper.getLevel().getBlockState(g.platformCentre(wrongPlatform[0])).isAir(), "wrong: the trapdoor opens");
            helper.assertTrue(who[2].getHealth() < who[2].getMaxHealth(), "no answer: the mallet");
            helper.assertFalse(who[0].getHealth() < who[0].getMaxHealth(), "the right one is spared");
            // Stunned, he takes more.
            var smite = AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null);
            g.setHealth(g.getMaxHealth() * 0.7f);
            float before = g.getHealth();
            g.invulnerableTime = 0;
            g.hurt(smite, 10f);
            float taken = before - g.getHealth(), expected = base(g, smite, 10f) * GabrielBalance.STUN_VULNERABILITY;
            helper.assertTrue(Math.abs(taken - expected) < 0.01f, "stunned: x1.25, " + taken + " vs " + expected);
        });
        helper.runAfterDelay(SETTLE + 2 + GabrielBalance.TRAPDOOR_TICKS + 3, () -> {
            helper.assertFalse(helper.getLevel().getBlockState(g.platformCentre(wrongPlatform[0])).isAir(), "the trapdoor shuts again");
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- CH 7: the hospital ----------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "gabriel_beat", timeoutTicks = 80)
    public static void aBlowOnTheBeepIsCritical(GameTestHelper helper) {
        GabrielEntity g = spawn(helper);
        ServerPlayer[] p = new ServerPlayer[1];
        float[] onBeat = new float[1];
        helper.runAfterDelay(SETTLE, () -> {
            g.forceLook(3);
            g.layChannelNow(Channel.HOSPITAL);
            g.setHealth(g.getMaxHealth() * 0.45f);
            p[0] = mortalAt(helper, g.position().add(2, 0, 0));
        });
        helper.runAfterDelay(SETTLE + 3, () -> {
            g.beat(helper.getLevel());
            helper.assertTrue(g.onBeat(), "the beep");
            float before = g.getHealth();
            g.invulnerableTime = 0;
            g.hurt(p[0].damageSources().playerAttack(p[0]), 10f);
            onBeat[0] = before - g.getHealth();
        });
        helper.runAfterDelay(SETTLE + 3 + 11, () -> {
            helper.assertFalse(g.onBeat(), "between beeps");
            float before = g.getHealth();
            g.invulnerableTime = 0;
            g.hurt(p[0].damageSources().playerAttack(p[0]), 10f);
            float off = before - g.getHealth();
            helper.assertTrue(off > 0 && Math.abs(onBeat[0] / off - GabrielBalance.BEAT_CRIT) < 0.01f,
                    "on the beep x1.5: " + onBeat[0] + " vs " + off);
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "gabriel_nurse", timeoutTicks = 80)
    public static void aNurseWhoReachesHimHealsHim(GameTestHelper helper) {
        GabrielEntity g = spawn(helper);
        float[] at = new float[1];
        GabrielDoubleEntity[] nurse = new GabrielDoubleEntity[2];
        helper.runAfterDelay(SETTLE, () -> {
            g.forceLook(3);
            g.layChannelNow(Channel.HOSPITAL);
            g.setHealth(g.getMaxHealth() * 0.4f);
            at[0] = g.getHealth();
            nurse[0] = g.spawnDouble(helper.getLevel(), GabrielDoubleEntity.Role.NURSE, g.position().add(1, 0, 0));
            nurse[1] = g.spawnDouble(helper.getLevel(), GabrielDoubleEntity.Role.NURSE, g.position().add(9, 0, 0));
            helper.assertTrue(nurse[0] != null && nurse[0].costume() == Channel.Costume.LAB_COAT, "a nurse in a lab coat");
            // The second is stopped on her way: one blow and she's gone.
            nurse[1].hurt(helper.getLevel().damageSources().generic(), 1f);
            helper.assertTrue(nurse[1].isRemoved(), "a struck nurse vanishes");
        });
        helper.runAfterDelay(SETTLE + 4, () -> {
            helper.assertTrue(nurse[0].isRemoved(), "the nurse who reached him is gone");
            float healed = g.getHealth() - at[0];
            helper.assertTrue(Math.abs(healed - GabrielBalance.NURSE_HEAL * g.getMaxHealth()) < 0.01f,
                    "and he is healed 3% (once): " + healed / g.getMaxHealth());
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- CH 9: the commercial --------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "gabriel_spokesmen", timeoutTicks = 100)
    public static void aDoublePunishesAndOnlyTheRealOneTakesTheBlow(GameTestHelper helper) {
        GabrielEntity g = spawn(helper);
        ServerPlayer[] p = new ServerPlayer[1];
        float[] health = new float[1];
        helper.runAfterDelay(SETTLE, () -> {
            g.forceLook(4);
            g.layChannelNow(Channel.COMMERCIAL);
        });
        helper.runAfterDelay(SETTLE + 3, () -> {
            List<GabrielDoubleEntity> men = g.spokesmen();
            helper.assertTrue(men.size() == GabrielBalance.SPOKESMEN - 1, "four spokesmen beside him, " + men.size());
            for (GabrielDoubleEntity d : men) helper.assertTrue(d.costume() == Channel.Costume.SUIT, "all in the suit");
            Vec3 real = g.podiumSpot(g.realPodium());
            helper.assertTrue(real != null && g.position().distanceTo(real) < 0.6, "he stands at his podium");
            p[0] = mortalAt(helper, men.getFirst().position().add(1, 0, 0));
            g.setHealth(g.getMaxHealth() * 0.2f);
            health[0] = g.getHealth();
            Vec3 was = p[0].position();
            GabrielDoubleEntity fake = men.getFirst();
            fake.hurt(p[0].damageSources().playerAttack(p[0]), 5f);
            helper.assertTrue(fake.isRemoved(), "a double goes at one blow");
            helper.assertTrue(g.getHealth() == health[0], "and he takes nothing from it");
            boolean punished = p[0].position().distanceTo(was) > 2 || p[0].hasEffect(MobEffects.BLINDNESS) || p[0].getHealth() < p[0].getMaxHealth();
            helper.assertTrue(punished, "the hunter is punished (moved, pied or hurt)");
            helper.assertTrue(g.shufflePending(), "the podiums are about to shuffle");
            // Each punishment, one by one.
            ServerPlayer q = mortalAt(helper, g.position().add(-3, 0, 3));
            g.punish(helper.getLevel(), q, GabrielEntity.PUNISH_PIE);
            helper.assertTrue(q.hasEffect(MobEffects.BLINDNESS), "a pie in the face");
            Vec3 from = q.position();
            g.punish(helper.getLevel(), q, GabrielEntity.PUNISH_TELEPORT);
            helper.assertTrue(q.position().distanceTo(from) > 2, "a short teleport");
            float hp = q.getHealth();
            g.punish(helper.getLevel(), q, GabrielEntity.PUNISH_DAMAGE);
            helper.assertTrue(q.getHealth() < hp, "a blow");
        });
        helper.runAfterDelay(SETTLE + 3 + GabrielBalance.STRUCK_SHUFFLE_DELAY + 3, () -> {
            helper.assertFalse(g.shufflePending(), "shuffled");
            helper.assertTrue(g.spokesmen().size() == GabrielBalance.SPOKESMEN - 1, "the doubles made up to four again");
            g.invulnerableTime = 0;
            g.hurt(p[0].damageSources().playerAttack(p[0]), 10f);
            helper.assertTrue(g.getHealth() < health[0], "the real one takes the blow");
            helper.assertTrue(g.shufflePending(), "and shuffles");
            cleanup(helper);
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(GabrielDoubleEntity.class, new AABB(helper.absolutePos(MID)).inflate(30)).isEmpty(),
                    "his doubles go with him");
            helper.succeed();
        });
    }

    // --- the factions (v0.13) --------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "gabriel_factions", timeoutTicks = 60)
    public static void theFactionsGetTheirTwists(GameTestHelper helper) {
        GabrielEntity g = spawn(helper);
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(BossTwists.GREETERS.contains("gabriel"), "he greets the sworn");
            ServerPlayer human = hunter(helper, "sn-gabriel-human"), demon = hunter(helper, "sn-gabriel-demon"), angel = hunter(helper, "sn-gabriel-angel");
            AllegianceTests.swear(demon, Faction.DEMON, 1);
            AllegianceTests.swear(angel, Faction.ANGEL, 1);
            float ratio = g.blowTo(demon, 10f) / g.blowTo(human, 10f);
            helper.assertTrue(Math.abs(ratio - GabrielBalance.DEMON_DAMAGE_TAKEN) < 1e-4, "a demon is the episode's villain: x1.15, " + ratio);
            float grace = angel.getData(AllAttachments.ALLEGIANCE).essence();
            g.commercialBreakSecond(List.of(angel, human));
            helper.assertTrue(angel.getData(AllAttachments.ALLEGIANCE).essence() == grace, "no drain outside a commercial break");
            g.setHealth(g.getMaxHealth() * 0.74f);
            g.beginTransition(2);
            g.commercialBreakSecond(List.of(angel, human, demon));
            float drained = grace - angel.getData(AllAttachments.ALLEGIANCE).essence();
            helper.assertTrue(Math.abs(drained - GabrielBalance.ANGEL_GRACE_DRAIN) < 1e-3, "an angel loses Grace in the break: " + drained);
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "gabriel_free_will", timeoutTicks = 60)
    public static void freeWillSeesThroughOneMoreDouble(GameTestHelper helper) {
        GabrielEntity g = spawn(helper);
        helper.runAfterDelay(SETTLE, () -> {
            g.forceLook(4);
            g.layChannelNow(Channel.COMMERCIAL);
        });
        helper.runAfterDelay(SETTLE + 3, () -> {
            ServerPlayer human = hunter(helper, "sn-gabriel-free"), demon = hunter(helper, "sn-gabriel-sworn");
            AllegianceTests.swear(demon, Faction.DEMON, 1);
            List<GabrielDoubleEntity> men = g.spokesmen();
            GabrielDoubleEntity seen = g.tellFor(human, men);
            helper.assertTrue(seen != null && men.contains(seen), "a human sees through one of the doubles");
            helper.assertTrue(g.tellFor(demon, men) == null, "the sworn do not");
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- his fall and his spoils -----------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, batch = "gabriel_spoils", timeoutTicks = 20)
    public static void theFirstVictoryGivesEverythingARematchAShare(GameTestHelper helper) {
        ServerPlayer p = hunter(helper, "sn-gabriel-spoils");
        RandomSource r = RandomSource.create(5);
        List<ItemStack> first = GabrielSpoils.share(p, r);
        helper.assertTrue(p.getData(AllAttachments.TRICKSTER).victories() == 1, "the victory is written down");
        helper.assertTrue(has(first, AllItems.TRICKSTER_REMOTE.get()) && has(first, AllItems.GABRIEL_BLADE.get())
                && has(first, AllItems.GABRIEL_TROPHY.get()), "the first time: the remote, the blade, the trophy");
        int candy = first.stream().filter(s -> s.is(AllItems.TRICKSTER_CANDY.get())).mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(candy >= GabrielSpoils.CANDY_MIN && candy <= GabrielSpoils.CANDY_MAX, "and 4-6 candies, " + candy);
        int prizes = 0;
        for (int i = 0; i < 40; i++) {
            List<ItemStack> again = GabrielSpoils.share(p, r);
            helper.assertTrue(has(again, AllItems.GABRIEL_TROPHY.get()) && has(again, AllItems.TRICKSTER_CANDY.get()), "a rematch: trophy and candy");
            helper.assertFalse(has(again, AllItems.TRICKSTER_REMOTE.get()) && has(again, AllItems.GABRIEL_BLADE.get()), "never both prizes again");
            if (has(again, AllItems.TRICKSTER_REMOTE.get()) || has(again, AllItems.GABRIEL_BLADE.get())) prizes++;
        }
        helper.assertTrue(prizes > 5 && prizes < 35, "half the rematches a prize, " + prizes + "/40");
        helper.assertTrue(p.getData(AllAttachments.TRICKSTER).victories() == 41, "every victory counted");
        helper.succeed();
    }

    private static boolean has(List<ItemStack> stacks, Item item) {
        return stacks.stream().anyMatch(s -> s.is(item));
    }

    @GameTest(template = SNGameTests.ARENA, batch = "gabriel_death", timeoutTicks = 280)
    public static void heFallsAndLeavesHisSpoils(GameTestHelper helper) {
        GabrielEntity g = spawn(helper);
        helper.runAfterDelay(SETTLE, () -> {
            g.forceLook(4);
            g.layChannelNow(Channel.COMMERCIAL);
        });
        helper.runAfterDelay(SETTLE + 3, () -> {
            helper.assertTrue(!g.spokesmen().isEmpty(), "the spokesmen are up");
            g.setHealth(2f);
            g.invulnerableTime = 0;
            g.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 500f);
            helper.assertTrue(g.state() == LuciferEntity.DYING, "at 0 he falls");
            helper.assertTrue(g.costume() == Channel.Costume.JACKET, "back in his own jacket");
            helper.assertTrue(g.spokesmen().isEmpty(), "the doubles go");
        });
        helper.runAfterDelay(SETTLE + 3 + LuciferEntity.DEATH_TICKS + 12, () -> {
            helper.assertTrue(g.isRemoved(), "he is gone... or is he?");
            helper.assertTrue(count(helper, AllItems.GABRIEL_TROPHY.get()) == 1, "his trophy falls");
            helper.assertTrue(count(helper, AllItems.TRICKSTER_CANDY.get()) >= GabrielSpoils.CANDY_MIN, "and his candy");
            discardItems(helper);
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- the remote, the blade, the candy --------------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = "gabriel_remote", timeoutTicks = 20)
    public static void theRemoteChangesChannelsButNeverOnTheWrongOnes(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerLevel level = helper.getLevel();
        RandomSource r = RandomSource.create(9);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 3));
        Pig named = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(7, 1, 7));
        named.setCustomName(net.minecraft.network.chat.Component.literal("Bacon"));
        Villager farmer = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(5, 1, 8));
        farmer.setVillagerData(farmer.getVillagerData().setProfession(VillagerProfession.FARMER));
        helper.assertTrue(TricksterRemoteItem.changeable(pig), "a pig can change channels");
        helper.assertFalse(TricksterRemoteItem.changeable(named), "never a named creature");
        helper.assertFalse(TricksterRemoteItem.changeable(farmer), "never a working villager");
        helper.assertTrue(TricksterRemoteItem.change(level, pig, TricksterRemoteItem.Change.SHRINK, r) == pig && TricksterMarks.shrunk(pig), "shrunk");
        TricksterRemoteItem.change(level, pig, TricksterRemoteItem.Change.DISGUISE, r);
        helper.assertTrue(TricksterMarks.hatted(pig) && pig.hasCustomName(), "dressed up, with a TV name");
        pig.setHealth(pig.getMaxHealth() / 2);
        LivingEntity next = TricksterRemoteItem.change(level, pig, TricksterRemoteItem.Change.SWAP, r);
        helper.assertTrue(next != null && next.getType() != EntityType.PIG && pig.isRemoved(), "swapped for something else its size");
        helper.assertTrue(Math.abs(next.getHealth() / next.getMaxHealth() - 0.5f) < 0.1f, "as hurt as it was");
        for (Entity e : List.of(next, named, farmer)) e.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "gabriel_blade", timeoutTicks = 20)
    public static void theBladeCallsTwoAfterimagesAndTheCandyDoesSomething(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        Zombie z = ArsenalTests.dummy(helper, new BlockPos(5, 1, 6), 180);
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(5, 1, 5), new ItemStack(AllItems.GABRIEL_BLADE.get()));
        CurseTests.armed(p);
        float before = z.getHealth();
        float each = GabrielBladeItem.afterimages(p, z);
        helper.assertTrue(each > 1f, "each afterimage hits for a share of the blow, " + each);
        helper.assertTrue(before - z.getHealth() > each, "both afterimages land: " + (before - z.getHealth()));
        ItemStack candy = new ItemStack(AllItems.TRICKSTER_CANDY.get());
        p.removeAllEffects();
        candy.finishUsingItem(helper.getLevel(), p);
        boolean any = false;
        for (var e : TricksterCandyItem.POOL) any |= p.hasEffect(e);
        helper.assertTrue(any, "the candy does something");
        z.discard();
        helper.succeed();
    }

    // --- the pranks ------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, batch = "gabriel_sighted", timeoutTicks = 20)
    public static void theThirdPrankPutsTheHunterOnHisTrail(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        JournalTests.Witness w = new JournalTests.Witness(helper.getLevel());
        Vec3 at = helper.absoluteVec(new BlockPos(2, 1, 2).getBottomCenter());
        w.moveTo(at.x, at.y, at.z, 0, 0);
        helper.getLevel().addNewPlayer(w);
        try {
            long day = TricksterPranks.day(helper.getLevel().getServer());
            helper.assertTrue(PrankRules.due(true, w.getData(AllAttachments.TRICKSTER), day), "a fresh hunter is due a prank");
            TricksterPranks.play(w, PrankRules.Prank.LAUGH_TRACK);
            helper.assertFalse(PrankRules.due(true, w.getData(AllAttachments.TRICKSTER), day), "one a day");
            TricksterPranks.play(w, PrankRules.Prank.CANDY_WRAPPER);
            helper.assertFalse(AuthorWorld.done(w, "main/trickster_sighted"), "two are not enough");
            TricksterPranks.play(w, PrankRules.Prank.LAUGH_TRACK);
            helper.assertTrue(w.getData(AllAttachments.TRICKSTER).sightings() == PrankRules.SIGHTINGS_NEEDED, "three noticed");
            helper.assertTrue(AuthorWorld.done(w, "main/trickster_sighted"), "the third puts them on his trail");
            helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(BlockPos.containing(at)).inflate(4)).forEach(Entity::discard);
            helper.succeed();
        } finally {
            w.setData(AllAttachments.TRICKSTER, TricksterLedger.NONE);
            helper.getLevel().removePlayerImmediately(w, Entity.RemovalReason.DISCARDED);
        }
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "gabriel_pranks", timeoutTicks = 80)
    public static void thePranksNeverTouchABlockOrAnInventory(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerLevel level = helper.getLevel();
        BlockPos chestAt = new BlockPos(8, 1, 8);
        helper.setBlock(chestAt, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(helper.absolutePos(chestAt));
        chest.setItem(4, new ItemStack(Items.DIAMOND, 3));
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 7));
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(7, 1, 3));
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(5, 1, 5), new ItemStack(Items.IRON_SWORD));
        p.getInventory().setItem(8, new ItemStack(Items.BREAD, 5));
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        BlockPos o = helper.absolutePos(BlockPos.ZERO);
        for (BlockPos pos : BlockPos.betweenClosed(o, o.offset(10, 5, 10))) blocks.put(pos.immutable(), level.getBlockState(pos));
        List<ItemStack> inventory = new ArrayList<>();
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) inventory.add(p.getInventory().getItem(i).copy());
        for (PrankRules.Prank prank : PrankRules.Prank.values()) {
            helper.assertTrue(TricksterPranks.play(p, prank), "the prank " + prank + " finds what it needs");
        }
        helper.assertTrue(TricksterMarks.hatted(pig) || TricksterMarks.hatted(villager), "somebody wears a party hat");
        helper.assertFalse(level.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(3),
                e -> e.getItem().is(AllItems.CANDY_WRAPPER.get())).isEmpty(), "a candy wrapper at their feet");
        helper.runAfterDelay(TricksterPranks.CHEST_OPEN_TICKS + 5, () -> {
            for (var e : blocks.entrySet()) {
                helper.assertTrue(level.getBlockState(e.getKey()) == e.getValue(), "a prank changed the block at " + e.getKey());
            }
            ItemStack kept = chest.getItem(4);
            helper.assertTrue(kept.is(Items.DIAMOND) && kept.getCount() == 3, "nothing moves in the chest");
            for (int i = 0; i < inventory.size(); i++) {
                helper.assertTrue(ItemStack.matches(inventory.get(i), p.getInventory().getItem(i)), "nothing moves in the hunter's pack, slot " + i);
            }
            level.getEntitiesOfClass(ItemEntity.class, new AABB(o).inflate(16)).forEach(Entity::discard);
            pig.discard();
            villager.discard();
            helper.succeed();
        });
    }
}
