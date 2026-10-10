package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceCrossroads;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceDialogue;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceEffect;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceRequirement;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceRites;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.BossTwists;
import org.papiricoh.supernaturalcraft.allegiance.ConsecratedGround;
import org.papiricoh.supernaturalcraft.allegiance.CureProgress;
import org.papiricoh.supernaturalcraft.allegiance.EssenceRules;
import org.papiricoh.supernaturalcraft.allegiance.EssenceSources;
import org.papiricoh.supernaturalcraft.allegiance.Expulsion;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.allegiance.HolyOilFireBlock;
import org.papiricoh.supernaturalcraft.allegiance.LuciferBargain;
import org.papiricoh.supernaturalcraft.allegiance.MobReactions;
import org.papiricoh.supernaturalcraft.allegiance.Ranks;
import org.papiricoh.supernaturalcraft.allegiance.SummonMessengerEffect;
import org.papiricoh.supernaturalcraft.allegiance.Toll;
import org.papiricoh.supernaturalcraft.allegiance.item.HolyOilItem;
import org.papiricoh.supernaturalcraft.allegiance.power.Passives;
import org.papiricoh.supernaturalcraft.allegiance.power.Power;
import org.papiricoh.supernaturalcraft.allegiance.power.PowerCaster;
import org.papiricoh.supernaturalcraft.allegiance.power.PowerRules;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.crossroads.CrossroadsDeal;
import org.papiricoh.supernaturalcraft.crossroads.DealTerms;
import org.papiricoh.supernaturalcraft.crossroads.Deals;
import org.papiricoh.supernaturalcraft.crossroads.Debts;
import org.papiricoh.supernaturalcraft.entity.allegiance.HostAllyEntity;
import org.papiricoh.supernaturalcraft.entity.allegiance.MessengerEntity;
import org.papiricoh.supernaturalcraft.entity.allegiance.RivalHunterEntity;
import org.papiricoh.supernaturalcraft.entity.boss.amara.Consumption;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.michael.host.HostAngelEntity;
import org.papiricoh.supernaturalcraft.entity.demon.BlackEyedDemon;
import org.papiricoh.supernaturalcraft.entity.projectile.HolyWaterProjectile;
import org.papiricoh.supernaturalcraft.hunter.SaltLineBlock;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.ritual.RitualConditions;
import org.papiricoh.supernaturalcraft.ritual.RitualRecipe;
import org.papiricoh.supernaturalcraft.ritual.block.RitualAltarBlockEntity;
import org.papiricoh.supernaturalcraft.ritual.effect.ExorciseEffect;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** v0.13 Allegiance: the rites, the weaknesses, the crossroads' soul, free will, mobs, powers and the bosses' twists. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class AllegianceTests {

    /** Swears {@code p} to {@code faction} at {@code rank} with a full bar (no rite, no fx). */
    public static void swear(ServerPlayer p, Faction faction, int rank) {
        Allegiance a = Allegiance.HUMAN.convert(faction).withRank(rank);
        p.setData(AllAttachments.ALLEGIANCE, a.withEssence(a.maxEssence()));
    }

    private static ServerPlayer fake(GameTestHelper helper, String name, BlockPos at) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.setGameMode(GameType.SURVIVAL);
        p.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN);
        Vec3 v = helper.absoluteVec(at.getBottomCenter());
        p.moveTo(v.x, v.y, v.z, 0, 0);
        return p;
    }

    /** A real (unconnected) player, in the level's player list and mortal at once. */
    private static JournalTests.Witness real(GameTestHelper helper, BlockPos at) {
        JournalTests.Witness w = new JournalTests.Witness(helper.getLevel());
        Vec3 v = helper.absoluteVec(at.getBottomCenter());
        w.moveTo(v.x, v.y, v.z, 0, 0);
        try {
            var f = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            f.setAccessible(true);
            f.setInt(w, 0);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        helper.getLevel().addNewPlayer(w);
        return w;
    }

    private static void gone(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    private static AllegianceEffect effect(AllegianceEffect.Op op, Faction faction, int rank) {
        return new AllegianceEffect(op, faction, rank < 0 ? Optional.empty() : Optional.of(rank), Optional.empty());
    }

    // --- the rites -----------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, batch = "allegiance_rites")
    public static void everyRiteClimbsOneRankAndNoMore(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos altar = helper.absolutePos(new BlockPos(2, 1, 2));
        long now = level.getGameTime();
        ServerPlayer angel = fake(helper, "sn-test-angel", new BlockPos(1, 1, 1));
        helper.assertTrue(effect(AllegianceEffect.Op.CONVERT, Faction.ANGEL, -1).perform(level, altar, angel), "Receive Grace");
        helper.assertTrue(Allegiances.get(angel).isAngel() && Allegiances.get(angel).rank() == 1, "a Lesser Angel");
        helper.assertFalse(effect(AllegianceEffect.Op.RANK_UP, Faction.ANGEL, 3).perform(level, altar, angel), "no skipping a rank");
        for (int r = 2; r <= 4; r++) {
            helper.assertTrue(effect(AllegianceEffect.Op.RANK_UP, Faction.ANGEL, r).perform(level, altar, angel), "angel rank " + r);
            helper.assertTrue(Allegiances.get(angel).rank() == r, "now rank " + r);
        }
        helper.assertFalse(effect(AllegianceEffect.Op.RANK_UP, Faction.DEMON, 2).perform(level, altar, angel), "not a demon's rite");
        helper.assertFalse(effect(AllegianceEffect.Op.CONVERT, Faction.DEMON, -1).perform(level, altar, angel), "an angel can't turn demon");
        // Ripping out Grace: human again, and no new side for three days.
        helper.assertTrue(new AllegianceEffect(AllegianceEffect.Op.CURE, Faction.ANGEL, Optional.empty(), Optional.of(AllItems.VIAL_OF_GRACE.get()))
                .perform(level, altar, angel), "Grace torn out");
        Allegiance cured = Allegiances.get(angel);
        helper.assertTrue(cured.isHuman() && cured.rank() == 0 && cured.cooldownUntil() >= now + CureProgress.CHOOSE_AGAIN_TICKS, "human, waiting: " + cured);
        helper.assertFalse(effect(AllegianceEffect.Op.CONVERT, Faction.DEMON, -1).perform(level, altar, angel), "too soon to choose again");
        helper.assertItemEntityPresent(AllItems.VIAL_OF_GRACE.get(), new BlockPos(2, 2, 2), 2);

        ServerPlayer demon = fake(helper, "sn-test-demon", new BlockPos(1, 1, 3));
        helper.assertTrue(effect(AllegianceEffect.Op.CONVERT, Faction.DEMON, -1).perform(level, altar, demon), "made a demon");
        for (int r = 2; r <= 4; r++) {
            helper.assertTrue(effect(AllegianceEffect.Op.RANK_UP, Faction.DEMON, r).perform(level, altar, demon), "demon rank " + r);
        }
        helper.assertTrue(Allegiances.get(demon).rank() == 4 && Allegiances.get(demon).essence() >= Ranks.maxEssence(Faction.DEMON, 4) / 2f,
                "a King with half a bar at least");

        ServerPlayer hunter = fake(helper, "sn-test-hunter", new BlockPos(3, 1, 3));
        for (int r = 1; r <= 3; r++) {
            helper.assertTrue(effect(AllegianceEffect.Op.RANK_UP, Faction.HUMAN, r).perform(level, altar, hunter), "hunter rank " + r);
        }
        helper.assertFalse(effect(AllegianceEffect.Op.RANK_UP, Faction.HUMAN, 4).perform(level, altar, hunter), "a Legend is the top");
        helper.assertTrue(Allegiances.get(hunter).isHuman() && Allegiances.get(hunter).hunterRank() == 3, "a Legend, still human");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "allegiance_rites")
    public static void theRitesAreLoadedAndAskTheRightSide(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (String id : List.of("receive_grace", "seraph_ascension", "archangel_ascension", "usurp_the_host", "prince_of_hell",
                "mark_of_cain", "usurp_the_throne", "hunters_oath", "veterans_vigil", "legend_of_the_road", "demon_cure", "rip_out_grace",
                "consecrate_ground")) {
            var holder = level.getRecipeManager().byKey(SupernaturalCraft.asResource("ritual/" + id));
            helper.assertTrue(holder.isPresent() && holder.get().value() instanceof RitualRecipe r && r.manaCost() <= 175, "ritual " + id + " loaded");
        }
        for (String id : List.of("purified_blood", "summon_messenger")) {
            helper.assertTrue(level.getRecipeManager().byKey(SupernaturalCraft.asResource("bowl_spell/" + id)).isPresent(), "bowl " + id + " loaded");
        }
        RitualRecipe seraph = (RitualRecipe) level.getRecipeManager().byKey(SupernaturalCraft.asResource("ritual/seraph_ascension")).orElseThrow().value();
        ServerPlayer human = fake(helper, "sn-test-human", new BlockPos(1, 1, 1));
        ServerPlayer angel1 = fake(helper, "sn-test-angel1", new BlockPos(2, 1, 1));
        ServerPlayer angel2 = fake(helper, "sn-test-angel2", new BlockPos(3, 1, 1));
        swear(angel1, Faction.ANGEL, 1);
        swear(angel2, Faction.ANGEL, 2);
        helper.assertTrue("message.supernaturalcraft.allegiance.wrong_side.angel".equals(seraph.conditions().check(level, human)), "a human is refused");
        helper.assertTrue("message.supernaturalcraft.allegiance.wrong_rank".equals(seraph.conditions().check(level, angel2)), "a Seraph is refused");
        helper.assertTrue(seraph.conditions().check(level, angel1) == null, "a Lesser Angel may rise");
        // Choosing a side waits out a cure's cooldown.
        RitualConditions choose = new RitualConditions(RitualConditions.Time.ANY, Optional.empty(), false, List.of(),
                Optional.of(new AllegianceRequirement(Faction.HUMAN, Optional.empty(), Optional.empty(), Optional.of(false))));
        helper.assertTrue(choose.check(level, human) == null, "free to choose");
        human.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN.cured(level.getGameTime() + 100));
        helper.assertTrue("message.supernaturalcraft.allegiance.cooldown".equals(choose.check(level, human)), "not on the cooldown");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "allegiance_altar", timeoutTicks = 240)
    public static void aHumanPaysAQuarterLessAndHolyGroundIsMade(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        BlockPos altarAt = new BlockPos(5, 1, 5);
        helper.setBlock(altarAt, AllBlocks.RITUAL_ALTAR.get().defaultBlockState());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) helper.setBlock(altarAt.offset(dx, 0, dz), AllBlocks.CHALK_LINE.get().defaultBlockState());
            }
        }
        RitualAltarBlockEntity altar = (RitualAltarBlockEntity) helper.getBlockEntity(altarAt);
        ServerPlayer human = fake(helper, "sn-test-ritualist", new BlockPos(3, 1, 5));
        ManaManager.get(human).setMana(100);
        for (ItemStack s : List.of(new ItemStack(AllItems.HOLY_WATER.get()), new ItemStack(AllItems.HOLY_WATER.get()), new ItemStack(AllItems.HOLY_WATER.get()),
                new ItemStack(AllItems.SALT.get()), new ItemStack(AllItems.SALT.get()), new ItemStack(Items.WHITE_CANDLE))) {
            human.setItemInHand(InteractionHand.MAIN_HAND, s);
            altar.onUse(human, InteractionHand.MAIN_HAND, s);
        }
        ItemStack flint = new ItemStack(Items.FLINT_AND_STEEL);
        human.setItemInHand(InteractionHand.MAIN_HAND, flint);
        altar.onUse(human, InteractionHand.MAIN_HAND, flint);
        helper.assertTrue(altar.isChanneling(), "Consecrate Ground started");
        float paid = 100 - ManaManager.get(human).mana();
        helper.assertTrue(Math.abs(paid - 30) < 0.01f, "a human pays 30 of 40, paid " + paid);
        ServerPlayer angel = fake(helper, "sn-test-angel", new BlockPos(7, 1, 5));
        swear(angel, Faction.ANGEL, 1);
        helper.assertTrue(Allegiances.ritualCost(angel, 40) == 40, "an angel pays it all");
        BlockPos holy = helper.absolutePos(altarAt.offset(4, 0, 0));
        helper.succeedWhen(() -> {
            helper.assertFalse(altar.isChanneling(), "still channelling");
            helper.assertTrue(ConsecratedGround.get(helper.getLevel()).inZone(holy), "four blocks off the altar is holy now");
        });
    }

    // --- weaknesses ----------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = "allegiance_weak", timeoutTicks = 100)
    public static void aDemonPlayerIsHeldBurntAndExpelledButNotKilled(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerLevel level = helper.getLevel();
        JournalTests.Witness demon = real(helper, new BlockPos(5, 1, 5));
        JournalTests.Witness human = real(helper, new BlockPos(8, 1, 8));
        swear(demon, Faction.DEMON, 2);
        helper.assertTrue(SaltLineBlock.isWarded(demon), "salt stops a demon player");
        helper.assertFalse(SaltLineBlock.isWarded(human), "but not a human");
        AllBlocks.DEVILS_TRAP.get().defaultBlockState().entityInside(level, demon.blockPosition(), demon);
        AllBlocks.DEVILS_TRAP.get().defaultBlockState().entityInside(level, human.blockPosition(), human);
        helper.assertTrue(demon.hasEffect(AllMobEffects.TRAPPED), "the trap holds a demon player");
        helper.assertFalse(human.hasEffect(AllMobEffects.TRAPPED), "a human walks out");
        // Holy water from above.
        HolyWaterProjectile flask = new HolyWaterProjectile(AllEntities.HOLY_WATER.get(), level);
        flask.setPos(demon.getX(), demon.getY() + 2.6, demon.getZ());
        flask.setDeltaMovement(0, -0.6, 0);
        level.addFreshEntity(flask);
        float before = demon.getHealth();
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(demon.getHealth() < before, "holy water burns a demon player: " + demon.getHealth());
            demon.setHealth(demon.getMaxHealth());
            demon.invulnerableTime = 0;
            // The rite of exorcism on a trapped demon player: expelled, not cast out.
            new ExorciseEffect(6).perform(level, demon.blockPosition(), null);
            helper.assertTrue(demon.isAlive() && demon.getHealth() >= 1, "expelled, alive: " + demon.getHealth());
            helper.assertTrue(demon.getHealth() < demon.getMaxHealth(), "and it hurt");
            helper.assertTrue(Allegiances.get(demon).essence() == EssenceRules.EXPELLED_KEEPS, "the Corruption is torn out");
            helper.assertTrue(human.getHealth() == human.getMaxHealth(), "the human is untouched");
            gone(helper, demon, human);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "allegiance_weak")
    public static void holyOilHoldsAnAngelAndTheSigilBanishes(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(5, 1, 5));
        int lit = HolyOilItem.pour(level, center);
        helper.assertTrue(lit == 16, "a ring of 16 fires, lit " + lit);
        helper.assertTrue(HolyOilFireBlock.enclosed(level, Vec3.atBottomCenterOf(center)), "the centre is inside");
        helper.assertTrue(HolyOilFireBlock.enclosed(level, Vec3.atBottomCenterOf(center.offset(1, 0, 1))), "off-centre is inside too");
        helper.assertFalse(HolyOilFireBlock.enclosed(level, Vec3.atBottomCenterOf(center.offset(4, 0, 0))), "outside the ring is not");
        ServerPlayer angel = CurseTests.mortal(helper, new BlockPos(5, 1, 5), ItemStack.EMPTY);
        ServerPlayer human = CurseTests.mortal(helper, new BlockPos(5, 1, 4), ItemStack.EMPTY);
        swear(angel, Faction.ANGEL, 2);
        helper.assertTrue(HolyOilFireBlock.hold(level, angel), "an angel in the ring is held");
        helper.assertTrue(angel.hasEffect(AllMobEffects.TRAPPED), "held");
        BlockPos fire = center.offset(2, 0, 0);
        helper.assertTrue(level.getBlockState(fire).is(AllBlocks.HOLY_OIL_FIRE.get()), "fire on the ring");
        level.getBlockState(fire).entityInside(level, fire, angel);
        level.getBlockState(fire).entityInside(level, fire, human);
        helper.assertTrue(angel.getHealth() < angel.getMaxHealth(), "it burns an angel who crosses");
        helper.assertTrue(human.getHealth() == human.getMaxHealth() && !human.isOnFire(), "and no one else");
        // The banishing sigil: the angel's Grace is gone, a lesser angel is sent back.
        Expulsion.banish(angel, null);
        helper.assertTrue(Allegiances.get(angel).essence() == 0, "banished: no Grace left");
        HostAngelEntity soldier = helper.spawn(AllEntities.HOST_ANGEL.get(), new BlockPos(8, 1, 8));
        helper.assertTrue(Expulsion.banish(soldier, null) && soldier.isRemoved(), "a soldier of the Host is sent back");
        helper.assertFalse(Expulsion.banish(human, null), "a human is not an angel");
        helper.succeed();
    }

    // --- the crossroads' soul -----------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, batch = "allegiance_soul")
    public static void aBoundSoulRisesADemon(GameTestHelper helper) {
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(2, 1, 2), ItemStack.EMPTY);
        p.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN);
        helper.assertTrue(Deals.offer(p).contains(new Deals.Option(DealTerms.Wish.CONVERT, 0)), "a human is offered \"Make me one of you\"");
        helper.assertTrue(Deals.seal(p, null, DealTerms.Wish.RARE_ITEM, 0, true), "sealed with the soul bound");
        helper.assertTrue(Debts.get(p).soulBound(), "bound");
        // The hounds come and collect.
        Debts.set(p, Debts.get(p).withState(CrossroadsDeal.State.COLLECTING));
        Debts.collect(p);
        helper.assertTrue(Allegiances.get(p).pendingDemon(), "the soul will rise a demon");
        helper.assertFalse(Debts.get(p).penaltyPending(), "and does not come back hollow");
        helper.assertTrue(AllegianceCrossroads.rise(p), "risen");
        helper.assertTrue(Allegiances.get(p).isDemon() && Allegiances.get(p).rank() == 1 && !Allegiances.get(p).pendingDemon(), "a Crossroads Demon");
        helper.assertFalse(Deals.offer(p).contains(new Deals.Option(DealTerms.Wish.CONVERT, 0)), "a demon is not offered it again");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "allegiance_cure", timeoutTicks = 60)
    public static void makeMeOneOfYouCostsTwoHeartsAndTheCureGivesThemBack(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long day = level.getDayTime();
        level.setDayTime((day / 24000) * 24000 + 18000);
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(2, 1, 2), ItemStack.EMPTY);
        p.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN);
        Toll.apply(p);
        double base = p.getAttributeValue(Attributes.MAX_HEALTH);
        helper.assertTrue(Deals.seal(p, null, DealTerms.Wish.CONVERT, 0), "the wish is granted");
        helper.assertTrue(Allegiances.get(p).isDemon() && Allegiances.get(p).tollHearts() == Toll.CONVERT_HEARTS, "a demon, two hearts owed");
        helper.assertTrue(p.getAttributeValue(Attributes.MAX_HEALTH) == base - 4, "two hearts gone: " + p.getAttributeValue(Attributes.MAX_HEALTH));
        helper.assertFalse(Debts.get(p).active(), "nothing falls due: no hounds");
        BlockPos at = new BlockPos(2, 1, 2);
        AllegianceEffect cure = effect(AllegianceEffect.Op.CURE, Faction.DEMON, -1);
        BlockPos altar = helper.absolutePos(new BlockPos(0, 1, 0));
        helper.assertFalse(cure.perform(level, altar, p), "no cure outside a devil's trap");
        helper.setBlock(at, AllBlocks.DEVILS_TRAP.get().defaultBlockState()
                .setValue(org.papiricoh.supernaturalcraft.hunter.DevilsTrapBlock.PART, org.papiricoh.supernaturalcraft.hunter.DevilsTrapBlock.CENTER));
        helper.assertTrue(cure.perform(level, altar, p), "first night");
        helper.assertFalse(cure.perform(level, altar, p), "not twice in a night");
        for (int night = 2; night <= CureProgress.NIGHTS; night++) {
            level.setDayTime(level.getDayTime() + 24000);
            helper.assertTrue(cure.perform(level, altar, p), "night " + night);
        }
        helper.assertTrue(Allegiances.get(p).isHuman() && Allegiances.get(p).tollHearts() == 0, "human again: " + Allegiances.get(p));
        helper.assertTrue(p.getAttributeValue(Attributes.MAX_HEALTH) == base, "the hearts are back");
        level.setDayTime(day);
        helper.succeed();
    }

    // --- free will, mobs -----------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, batch = "allegiance_mobs")
    public static void freeWillRefusesHeavensMarkAndPossession(GameTestHelper helper) {
        ServerPlayer human = fake(helper, "sn-test-free", new BlockPos(1, 1, 1));
        ServerPlayer angel = fake(helper, "sn-test-sworn", new BlockPos(3, 1, 3));
        swear(angel, Faction.ANGEL, 1);
        human.addEffect(new MobEffectInstance(AllMobEffects.HEAVENS_MARK, 200));
        human.addEffect(new MobEffectInstance(AllMobEffects.POSSESSED, 200));
        angel.addEffect(new MobEffectInstance(AllMobEffects.HEAVENS_MARK, 200));
        helper.assertFalse(human.hasEffect(AllMobEffects.HEAVENS_MARK) || human.hasEffect(AllMobEffects.POSSESSED), "free will refuses both");
        helper.assertTrue(angel.hasEffect(AllMobEffects.HEAVENS_MARK), "the sworn have no such shield");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "allegiance_mobs")
    public static void theSameSideLeavesItsOwnAlone(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer angel = fake(helper, "sn-test-angel", new BlockPos(2, 1, 2));
        ServerPlayer demon = fake(helper, "sn-test-demon", new BlockPos(8, 1, 8));
        ServerPlayer human = fake(helper, "sn-test-human", new BlockPos(5, 1, 2));
        swear(angel, Faction.ANGEL, 1);
        swear(demon, Faction.DEMON, 1);
        HostAngelEntity host = helper.spawn(AllEntities.HOST_ANGEL.get(), new BlockPos(3, 1, 5));
        host.setTarget(angel);
        helper.assertTrue(host.getTarget() == null, "the Host ignores an angel");
        host.setTarget(human);
        helper.assertTrue(host.getTarget() == human, "but not a human");
        BlackEyedDemon d = helper.spawn(AllEntities.BLACK_EYED_DEMON.get(), new BlockPos(7, 1, 5));
        d.setTarget(demon);
        helper.assertTrue(d.getTarget() == null, "a demon does not hunt a demon");
        d.setTarget(angel);
        helper.assertTrue(d.getTarget() == angel, "it hunts an angel");
        d.setTarget(null);
        d.setLastHurtByMob(demon);
        helper.assertFalse(MobReactions.ignores(d, demon), "struck first, it strikes back");
        swear(demon, Faction.DEMON, 4);
        helper.assertTrue(MobReactions.ignores(d, demon), "unless it is the King");
        helper.assertTrue(RivalHunterEntity.quarry(angel) && RivalHunterEntity.quarry(d), "a rival hunter hunts the sworn and demons");
        helper.assertFalse(RivalHunterEntity.quarry(human), "never a human");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "allegiance_ally", timeoutTicks = 200)
    public static void aSoldierOfTheHostFollowsItsGeneral(GameTestHelper helper) {
        SNGameTests.floor(helper, 48, 48);
        JournalTests.Witness general = real(helper, new BlockPos(6, 1, 24));
        swear(general, Faction.ANGEL, 4);
        HostAllyEntity ally = HostAllyEntity.summon(helper.getLevel(), general, helper.absoluteVec(new BlockPos(20, 1, 24).getBottomCenter()), true);
        helper.assertTrue(ally != null && ally.isCaptain(), "a captain answers");
        helper.assertFalse(ally.canAttack(general), "never its general");
        double start = ally.distanceTo(general);
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(ally.isAlive() && ally.distanceTo(general) < start - 4, "it came to its general: " + ally.distanceTo(general));
            ally.setLife(1);
            gone(helper, general);
            helper.succeed();
        });
    }

    // --- powers --------------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = "allegiance_powers")
    public static void castingIsRefusedWithoutEssenceOnCooldownOrInTheAuthorsArena(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerLevel level = helper.getLevel();
        ServerPlayer angel = fake(helper, "sn-test-caster", new BlockPos(5, 1, 5));
        swear(angel, Faction.ANGEL, 1);
        helper.assertTrue(PowerCaster.cast(angel, Power.SMITE, -1) == PowerRules.Verdict.RANK_TOO_LOW, "smite is a Seraph's");
        helper.assertTrue(PowerCaster.cast(angel, Power.SMOKE, -1) == PowerRules.Verdict.NOT_YOURS, "smoke is a demon's");
        helper.assertTrue(PowerCaster.cast(angel, Power.WINGS, -1) == PowerRules.Verdict.RANK_TOO_LOW, "wings come at II");
        float full = Allegiances.get(angel).essence();
        angel.setHealth(10);
        helper.assertTrue(PowerCaster.cast(angel, Power.HEALING_TOUCH, -1) == PowerRules.Verdict.OK, "a touch to the brow");
        helper.assertTrue(angel.getHealth() > 10, "healed");
        helper.assertTrue(Allegiances.get(angel).essence() == full - PowerCaster.cost(Power.HEALING_TOUCH), "paid in Grace");
        helper.assertTrue(PowerCaster.cast(angel, Power.HEALING_TOUCH, -1) == PowerRules.Verdict.COOLING_DOWN, "not again so soon");
        angel.setData(AllAttachments.ALLEGIANCE, Allegiances.get(angel).withEssence(0));
        helper.assertTrue(PowerCaster.cast(angel, Power.ANGEL_RADIO, -1) == PowerRules.Verdict.NO_ESSENCE, "no Grace, no radio");
        swear(angel, Faction.ANGEL, 1);
        ArenaController arena = ArenaSavedData.get(level).create(angel.blockPosition(), 12);
        arena.setTheme(ArenaTheme.AUTHOR);
        helper.assertTrue(PowerCaster.cast(angel, Power.ANGEL_RADIO, -1) == PowerRules.Verdict.SUPPRESSED, "\"I gave you that\"");
        Passives.second(angel);
        helper.assertTrue(Allegiances.flag(angel, org.papiricoh.supernaturalcraft.allegiance.Allegiances.SUPPRESSED), "flagged suppressed");
        arena.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        Passives.second(angel);
        helper.assertTrue(PowerCaster.cast(angel, Power.ANGEL_RADIO, -1) == PowerRules.Verdict.OK, "out of his arena, it works");
        helper.assertFalse(Allegiances.flag(angel, org.papiricoh.supernaturalcraft.allegiance.Allegiances.SUPPRESSED), "the flag is lifted");
        PowerCaster.forget(angel);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "allegiance_powers")
    public static void aSeraphSmitesALesserDemon(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer angel = fake(helper, "sn-test-seraph", new BlockPos(5, 1, 2));
        swear(angel, Faction.ANGEL, 2);
        BlackEyedDemon d = helper.spawn(AllEntities.BLACK_EYED_DEMON.get(), new BlockPos(5, 1, 5));
        d.setNoAi(true);
        angel.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, d.getEyePosition());
        helper.assertTrue(PowerCaster.cast(angel, Power.SMITE, d.getId()) == PowerRules.Verdict.OK, "smite");
        helper.assertTrue(d.isDeadOrDying(), "the demon burns out from the eyes");
        float before = Allegiances.get(angel).essence();
        BlackEyedDemon d2 = helper.spawn(AllEntities.BLACK_EYED_DEMON.get(), new BlockPos(7, 1, 7));
        EssenceSources.onKill(angel, d2);
        helper.assertTrue(Allegiances.get(angel).essence() >= Math.min(Allegiances.get(angel).maxEssence(), before + EssenceRules.KILL_OPPOSITE) - 0.01f,
                "a kill of the other side feeds Grace");
        PowerCaster.forget(angel);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "allegiance_powers")
    public static void bloodlustPactsAndAHuntersGifts(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer knight = fake(helper, "sn-test-knight", new BlockPos(2, 1, 2));
        swear(knight, Faction.DEMON, 3);
        knight.setData(AllAttachments.ALLEGIANCE, Allegiances.get(knight).withEssence(10));
        EssenceSources.setLastKill(knight, EssenceRules.BLOODLUST_GRACE_SECONDS + 10);
        helper.assertTrue(Passives.bloodlust(knight, Allegiances.get(knight)), "starved of a kill");
        helper.assertTrue(Allegiances.get(knight).essence() < 10, "the Corruption drains");
        EssenceSources.setLastKill(knight, 0);
        helper.assertFalse(Passives.bloodlust(knight, Allegiances.get(knight)), "fed");
        // A pact with a villager: crouched, an emerald.
        Villager v = helper.spawn(EntityType.VILLAGER, new BlockPos(4, 1, 2));
        v.setNoAi(true);
        knight.setPose(Pose.CROUCHING);
        float before = Allegiances.get(knight).essence();
        helper.assertTrue(EssenceSources.pact(knight, v, new ItemStack(Items.EMERALD)), "a pact");
        helper.assertTrue(Allegiances.get(knight).essence() > before, "Corruption for it");
        float after = Allegiances.get(knight).essence();
        EssenceSources.pact(knight, v, new ItemStack(Items.EMERALD));
        helper.assertTrue(Allegiances.get(knight).essence() == after, "one pact per villager");
        ServerPlayer human = fake(helper, "sn-test-human", new BlockPos(6, 1, 2));
        human.setPose(Pose.CROUCHING);
        helper.assertFalse(EssenceSources.pact(human, v, new ItemStack(Items.EMERALD)), "a human makes no pacts");
        // Every rank, on any road: 25 more mana.
        swear(human, Faction.HUMAN, 1);
        Passives.second(human);
        helper.assertTrue(ManaManager.get(human).maxMana() == 125, "a Hunter's discipline");
        swear(human, Faction.ANGEL, 3);
        Passives.second(human);
        helper.assertTrue(ManaManager.get(human).maxMana() == 175, "an Archangel can afford the Host's rite");
        human.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN);
        Passives.second(human);
        helper.assertTrue(ManaManager.get(human).maxMana() == 100, "gone with the rank");
        helper.succeed();
    }

    // --- the messenger -------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = "allegiance_messenger", timeoutTicks = 60)
    public static void theMessengerOffersGraceAndNeverComesToADemon(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        JournalTests.Witness w = real(helper, new BlockPos(5, 1, 5));
        w.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN);
        MessengerEntity m = MessengerEntity.visit(w);
        helper.assertTrue(m != null && MessengerEntity.visiting(w), "he comes");
        m.answer(w, "greeting", "listen");
        var q = AllegianceDialogue.pending(w);
        helper.assertTrue(q != null && q.node().equals("offer") && q.dialogue().equals(MessengerEntity.DIALOGUE), "and makes his offer");
        helper.assertTrue(AllegianceDialogue.answer(w, m.getId(), MessengerEntity.DIALOGUE, "offer", "accept"), "accepted");
        helper.assertTrue(w.getInventory().countItem(AllItems.VIAL_OF_GRACE.get()) == 1, "a vial of Grace");
        helper.assertTrue(Allegiances.get(w).messenger() == Allegiance.MESSENGER_HEEDED, "heeded");
        helper.assertTrue(m.leaving(), "he goes");
        ServerPlayer demon = fake(helper, "sn-test-demon", new BlockPos(2, 1, 2));
        swear(demon, Faction.DEMON, 1);
        helper.assertFalse(new SummonMessengerEffect().perform(helper.getLevel(), demon.blockPosition(), demon), "never to a demon");
        m.discard();
        gone(helper, w);
        helper.succeed();
    }

    // --- the bosses ----------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, batch = "allegiance_amara")
    public static void amaraEatsADemonAtHalfTheRate(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ServerPlayer demon = CurseTests.mortal(helper, new BlockPos(1, 1, 1), ItemStack.EMPTY);
        ServerPlayer human = CurseTests.mortal(helper, new BlockPos(3, 1, 3), ItemStack.EMPTY);
        ServerPlayer angel = CurseTests.mortal(helper, new BlockPos(1, 1, 3), ItemStack.EMPTY);
        swear(demon, Faction.DEMON, 1);
        human.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN);
        swear(angel, Faction.ANGEL, 1);
        Consumption.set(demon, 0);
        Consumption.set(human, 0);
        Consumption.tick(null, demon, true);
        Consumption.tick(null, human, true);
        float d = Consumption.get(demon), h = Consumption.get(human);
        // Each against the plain rate where they stand (the light may differ a little across the room).
        float plainD = org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraBalance.consumptionPerSecond(Consumption.lightAt(demon), true) * Consumption.INTERVAL / 20f;
        float plainH = org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraBalance.consumptionPerSecond(Consumption.lightAt(human), true) * Consumption.INTERVAL / 20f;
        helper.assertTrue(plainD > 0 && Math.abs(d - plainD * BossTwists.AMARA_DEMON_CONSUMPTION) < 0.001f, "a demon at half the rate: " + d + " of " + plainD);
        helper.assertTrue(Math.abs(h - plainH) < 0.001f, "a human at the full rate: " + h + " of " + plainH);
        float grace = Allegiances.get(angel).essence();
        Consumption.tick(null, angel, true);
        helper.assertTrue(Allegiances.get(angel).essence() < grace, "her darkness drinks an angel's Grace");
        helper.assertTrue(BossTwists.amaraDamage(helper.getLevel().damageSources().playerAttack(angel), 10) == 10 * BossTwists.AMARA_ANGEL_DAMAGE,
                "an angel strikes her for less");
        long t = helper.getLevel().getGameTime();
        helper.assertTrue(BossTwists.amaraIgnores(demon, t) != BossTwists.amaraIgnores(demon, t + BossTwists.AMARA_WINDOW), "half her attacks pass over a demon");
        helper.assertFalse(BossTwists.amaraIgnores(human, t) || BossTwists.amaraIgnores(human, t + BossTwists.AMARA_WINDOW), "never a human");
        Consumption.release(demon);
        Consumption.release(human);
        Consumption.release(angel);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "allegiance_lucifer", timeoutTicks = 60)
    public static void luciferOffersADemonOnceAndItIsATrap(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        LuciferEntity lucifer = helper.spawn(AllEntities.LUCIFER.get(), new BlockPos(24, 1, 24));
        ServerPlayer demon = fake(helper, "sn-test-prince", new BlockPos(20, 1, 24));
        ServerPlayer human = fake(helper, "sn-test-hunter", new BlockPos(28, 1, 24));
        swear(demon, Faction.DEMON, 2);
        UUID arena = UUID.randomUUID();
        BossTwists.visit(arena, lucifer, List.of(demon, human));
        helper.assertTrue(AllegianceDialogue.pending(demon) == null, "not in his first phase");
        lucifer.forceLook(2);
        BossTwists.visit(arena, lucifer, List.of(demon, human));
        var q = AllegianceDialogue.pending(demon);
        helper.assertTrue(q != null && q.dialogue().equals(LuciferBargain.DIALOGUE), "he makes his offer");
        helper.assertTrue(AllegianceDialogue.pending(human) == null, "only to a demon");
        AllegianceDialogue.forget(demon.getUUID());
        BossTwists.visit(arena, lucifer, List.of(demon, human));
        helper.assertTrue(AllegianceDialogue.pending(demon) == null && BossTwists.offered(arena, demon), "once");
        // The other levels' ticks (no fight in them) must not make him forget and offer again.
        for (ServerLevel other : helper.getLevel().getServer().getAllLevels()) {
            if (other != helper.getLevel()) BossTwists.tick(other);
        }
        helper.assertTrue(BossTwists.offered(arena, demon), "another level's tick does not forget the offer");
        LuciferBargain.offer(demon, lucifer);
        helper.assertTrue(AllegianceDialogue.answer(demon, lucifer.getId(), LuciferBargain.DIALOGUE, "offer", "serve"), "\"I serve\"");
        helper.assertTrue(LuciferBargain.worn(demon) && demon.hasEffect(AllMobEffects.VESSEL), "and he wears the demon");
        LuciferBargain.release(demon);
        helper.assertFalse(LuciferBargain.worn(demon), "then lets go");
        BossTests.cleanup(helper);
        helper.succeed();
    }
}
