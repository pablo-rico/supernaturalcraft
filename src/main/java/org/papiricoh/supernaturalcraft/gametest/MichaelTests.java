package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelBalance;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelEntity;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelSpoils;
import org.papiricoh.supernaturalcraft.entity.boss.michael.VesselPossession;
import org.papiricoh.supernaturalcraft.entity.boss.michael.host.HostAngelEntity;
import org.papiricoh.supernaturalcraft.entity.boss.michael.projectile.MichaelLanceEntity;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllRecipes;
import org.papiricoh.supernaturalcraft.reward.michael.BorrowedLanceItem;
import org.papiricoh.supernaturalcraft.reward.michael.HeavenLedger;
import org.papiricoh.supernaturalcraft.reward.michael.ThrownLanceEntity;
import org.papiricoh.supernaturalcraft.reward.michael.WingFlight;
import org.papiricoh.supernaturalcraft.ritual.RitualConditions;
import org.papiricoh.supernaturalcraft.ritual.RitualRecipe;
import org.papiricoh.supernaturalcraft.ritual.effect.SummonMichaelEffect;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Archangel Michael (v0.12): his rite, the relics he gives back, the spoils per hunter and the General's armour ledger,
 * the touch to the forehead, "I need your yes", the Host's captain, the Lance (pinned, recalled, stolen, thrown back), the
 * hunter's own lance, the wings' flight, his Heavens, his transform, and Chuck waiting for him.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class MichaelTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    private static void cleanup(GameTestHelper helper) {
        BossTests.cleanup(helper);
        for (Entity e : helper.getLevel().getAllEntities()) {
            if (e instanceof HostAngelEntity || e instanceof MichaelLanceEntity || e instanceof ThrownLanceEntity) e.discard();
        }
    }

    private static MichaelEntity spawn(GameTestHelper helper) {
        cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        return helper.spawn(AllEntities.MICHAEL.get(), MID);
    }

    private static ServerPlayer hunter(GameTestHelper helper, String name, int dx) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.setGameMode(GameType.SURVIVAL);
        Vec3 at = helper.absoluteVec(MID.offset(dx, 0, 0).getBottomCenter());
        p.moveTo(at.x, at.y, at.z);
        return p;
    }

    private static List<ItemEntity> items(GameTestHelper helper, Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(MID)).inflate(40),
                e -> e.isAlive() && e.getItem().is(item));
    }

    private static int count(GameTestHelper helper, Item item) {
        return items(helper, item).stream().mapToInt(e -> e.getItem().getCount()).sum();
    }

    private static void discardItems(GameTestHelper helper) {
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(MID)).inflate(40)).forEach(Entity::discard);
    }

    /** Puts him into his last phase and lets him die: the spoils fall at the end of his death. */
    private static void defeat(MichaelEntity m, ServerLevel level) {
        m.forceLook(MichaelBalance.PHASES);
        m.setHealth(2f);
        m.invulnerableTime = 0;
        m.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, null), 500f);
    }

    // --- the rite --------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, batch = "michael_rite", timeoutTicks = 20)
    public static void hisRiteAsksForTheScribeByDayInTheOverworld(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.setDayTime(6000);
        RitualRecipe rite = null;
        for (var holder : level.getRecipeManager().getAllRecipesFor(AllRecipes.RITUAL.get())) {
            if (holder.id().getPath().equals("ritual/summon_michael")) rite = holder.value();
        }
        helper.assertTrue(rite != null, "no summon_michael");
        RitualRecipe r = rite;
        helper.assertTrue(r.conditions().dimension().isPresent() && r.conditions().dimension().get().equals(net.minecraft.world.level.Level.OVERWORLD),
                "he is called in the Overworld");
        helper.assertTrue(r.conditions().time() == RitualConditions.Time.DAY, "by day");
        for (Item item : List.of(AllItems.ANGEL_TABLET.get(), AllItems.SERAPH_WINGS.get(), AllItems.CHOIR_SHARD.get(), AllItems.HOLY_WATER.get())) {
            boolean needed = false;
            for (Ingredient i : r.ingredients()) needed |= i.test(new ItemStack(item));
            helper.assertTrue(needed, "his rite wants " + item);
        }
        ServerPlayer p = HellTests.ritualist(helper, "sn-test-michael-rite");
        helper.runAfterDelay(1, () -> {
            helper.assertTrue("message.supernaturalcraft.ritual.not_ready".equals(r.conditions().check(level, p)),
                    "he should wait for Metatron's fall");
            HellTests.award(p, "main/scribe_of_god");
            helper.assertTrue(r.conditions().check(level, p) == null, "the scribe fallen, by day, here: he answers");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "michael_rite_effect", timeoutTicks = 40)
    public static void theRiteCallsHimDownWithTheRelicsToGiveBack(GameTestHelper helper) {
        cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        helper.assertTrue(new SummonMichaelEffect().perform(level, helper.absolutePos(MID), null), "the rite should call him down");
        List<MichaelEntity> found = level.getEntitiesOfClass(MichaelEntity.class, new AABB(helper.absolutePos(MID)).inflate(30));
        helper.assertTrue(found.size() == 1, "one Michael, found " + found.size());
        helper.assertTrue(found.getFirst().relicsOffered(), "he should know the relics were offered");
        helper.assertTrue(found.getFirst().state() == LuciferEntity.EMERGING, "he comes down first");
        cleanup(helper);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "michael_chuck", timeoutTicks = 10)
    public static void chuckWaitsForBothBrothers(GameTestHelper helper) {
        Set<String> beaten = new HashSet<>();
        for (BossProgression.Boss b : BossProgression.Boss.values()) {
            if (b != BossProgression.Boss.CHUCK && b != BossProgression.Boss.MICHAEL) beaten.add(b.advancement);
        }
        helper.assertFalse(BossProgression.allBeforeChuck(beaten::contains), "Chuck should wait for Michael");
        beaten.add(BossProgression.Boss.MICHAEL.advancement);
        helper.assertTrue(BossProgression.allBeforeChuck(beaten::contains), "with Michael fallen too, Chuck can be found");
        helper.succeed();
    }

    // --- relics and spoils -----------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "michael_win", timeoutTicks = 240)
    public static void hisSpoilsAndTheRelicsWhenHeFalls(GameTestHelper helper) {
        MichaelEntity m = spawn(helper);
        m.setRelicsOffered(true);
        helper.runAfterDelay(3, () -> defeat(m, helper.getLevel()));
        helper.runAfterDelay(MichaelBalance.DEATH_TICKS + 15, () -> {
            helper.assertTrue(m.isRemoved(), "he should be gone");
            helper.assertTrue(count(helper, AllItems.ANGEL_TABLET.get()) == 1, "the Angel Tablet comes back");
            helper.assertTrue(count(helper, AllItems.SERAPH_WINGS.get()) == 1, "the Seraph Wings come back");
            helper.assertTrue(count(helper, AllItems.MICHAEL_LANCE.get()) == 1, "his lance falls");
            helper.assertTrue(count(helper, AllItems.MICHAELS_GRACE.get()) == 1, "his grace falls");
            helper.assertTrue(count(helper, AllItems.MICHAEL_TROPHY.get()) == 1, "his likeness falls");
            discardItems(helper);
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "michael_lost", timeoutTicks = 60)
    public static void theRelicsComeBackWhenTheFightIsLost(GameTestHelper helper) {
        MichaelEntity m = spawn(helper);
        m.setRelicsOffered(true);
        helper.runAfterDelay(3, () -> {
            ArenaController arena = m.arena();
            helper.assertTrue(arena != null, "he should open his Heaven");
            arena.beginRestore(false);
        });
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(m.isRemoved(), "he should leave once the fight is lost");
            helper.assertTrue(count(helper, AllItems.ANGEL_TABLET.get()) == 1 && count(helper, AllItems.SERAPH_WINGS.get()) == 1,
                    "the relics come back, lost or won");
            helper.assertTrue(count(helper, AllItems.MICHAEL_LANCE.get()) == 0, "but nothing of his own");
            discardItems(helper);
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "michael_spoils", timeoutTicks = 30)
    public static void eachHunterGetsTheirShareAndANewPieceOfArmour(GameTestHelper helper) {
        cleanup(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer a = hunter(helper, "sn-test-michael-a", 2), b = hunter(helper, "sn-test-michael-b", -2);
        // b already has the helmet from an earlier victory.
        b.setData(AllAttachments.HEAVEN, HeavenLedger.NONE.give(0));
        Vec3 at = helper.absoluteVec(MID.getBottomCenter()).add(0, 1, 0);
        List<Item> pieces = MichaelSpoils.pieces();
        Set<Integer> aGot = new HashSet<>();
        for (int v = 0; v < 4; v++) {
            int before = aGot.size();
            int[] counts = new int[4];
            for (int i = 0; i < 4; i++) counts[i] = count(helper, pieces.get(i));
            MichaelSpoils.drop(level, List.of(a, b), at);
            int newPieces = 0;
            for (int i = 0; i < 4; i++) newPieces += count(helper, pieces.get(i)) - counts[i];
            helper.assertTrue(newPieces == 2, "one piece each victory for each of the two, got " + newPieces);
            for (int i = 0; i < 4; i++) if (a.getData(AllAttachments.HEAVEN).has(i)) aGot.add(i);
            helper.assertTrue(aGot.size() == before + 1, "a new piece every victory, never the same one twice");
            if (v == 2) {
                helper.assertTrue(count(helper, AllItems.GENERAL_HELMET.get()) == 1, "b, who had the helmet, never gets it again before the set is whole");
            }
        }
        helper.assertTrue(a.getData(AllAttachments.HEAVEN).given() == 4, "four victories: the whole set");
        helper.assertTrue(count(helper, AllItems.MICHAEL_LANCE.get()) == 8 && count(helper, AllItems.MICHAELS_GRACE.get()) == 8
                && count(helper, AllItems.MICHAEL_TROPHY.get()) == 8, "a lance, a grace and a trophy for every hunter, every time");
        helper.assertTrue(count(helper, AllItems.GENERAL_HELMET.get()) == 2, "with b's set whole, a fifth victory starts the round again");
        discardItems(helper);
        helper.succeed();
    }

    // --- the touch to the forehead -----------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "michael_touch", timeoutTicks = 60)
    public static void theTouchIsTurnedBrokenOrLands(GameTestHelper helper) {
        MichaelEntity m = spawn(helper);
        helper.runAfterDelay(3, () -> {
            ServerPlayer near = CurseTests.mortal(helper, MID.offset(2, 0, 0), ItemStack.EMPTY);
            // A shield raised in the last half second turns it: he reels.
            helper.assertFalse(MichaelAttacks.contact(m, near, MichaelBalance.TOUCH_PARRY_WINDOW - 2), "a timely shield turns his hand");
            helper.assertTrue(m.isStaggered(), "and he reels");
            // A shield held up all along does not.
            helper.assertFalse(MichaelAttacks.parries(MichaelBalance.TOUCH_PARRY_WINDOW + 20), "a shield held all along is no parry");
            // Thirty true damage while he reaches breaks it.
            MichaelAttacks.ForeheadTouch touch = new MichaelAttacks.ForeheadTouch();
            helper.assertFalse(touch.wound(m, 20, near), "twenty is not enough");
            helper.assertTrue(touch.wound(m, 12, near), "thirty breaks his reach");
            // Out of reach, nothing; in reach without a shield, it smites.
            ServerPlayer far = CurseTests.mortal(helper, MID.offset(9, 0, 0), ItemStack.EMPTY);
            helper.assertFalse(MichaelAttacks.contact(m, far, -1), "out of reach");
            float before = near.getHealth();
            near.invulnerableTime = 0;
            helper.assertTrue(MichaelAttacks.contact(m, near, -1), "it lands");
            helper.assertTrue(near.getHealth() < before, "it burns");
            helper.assertTrue(near.hasEffect(MobEffects.BLINDNESS), "and blinds");
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- "I need your yes" -----------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "michael_yes", timeoutTicks = 260)
    public static void yesHeWearsYouHealsAndLeavesHisFavour(GameTestHelper helper) {
        MichaelEntity m = spawn(helper);
        ServerPlayer p = hunter(helper, "sn-test-michael-yes", 3);
        float[] before = new float[1];
        helper.runAfterDelay(5, () -> {
            m.setHealth(m.getMaxHealth() * 0.9f);
            before[0] = m.getHealth();
            VesselPossession.ask(p, m);
            helper.assertTrue(VesselPossession.pending(p) != null, "the question waits for an answer");
            helper.assertFalse(VesselPossession.answer(p, m.getId() + 1, true), "an answer to someone else's question is ignored");
            helper.assertTrue(VesselPossession.answer(p, m.getId(), true), "yes, in time");
            helper.assertTrue(p.hasEffect(AllMobEffects.VESSEL) && VesselPossession.worn(p), "he wears the hunter");
        });
        helper.runAfterDelay(5 + MichaelBalance.POSSESS_TICKS + 10, () -> {
            helper.assertFalse(VesselPossession.worn(p) || p.hasEffect(AllMobEffects.VESSEL), "he lets go");
            helper.assertTrue(p.hasEffect(AllMobEffects.GRACE_FAVOR), "and leaves his favour");
            helper.assertTrue(m.getHealth() > before[0], "he healed while he wore them");
            // His favour doubles what the hunter does to him.
            ServerPlayer plain = hunter(helper, "sn-test-michael-plain", -3);
            m.setHealth(m.getMaxHealth() * 0.9f);
            m.invulnerableTime = 0;
            float h0 = m.getHealth();
            m.hurt(helper.getLevel().damageSources().playerAttack(plain), 10);
            float plainLoss = h0 - m.getHealth();
            m.invulnerableTime = 0;
            h0 = m.getHealth();
            m.hurt(helper.getLevel().damageSources().playerAttack(p), 10);
            float favourLoss = h0 - m.getHealth();
            helper.assertTrue(Math.abs(favourLoss - plainLoss * MichaelBalance.FAVOR_MULTIPLIER) < 0.01f,
                    "his favour doubles the blow: " + plainLoss + " vs " + favourLoss);
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "michael_no", timeoutTicks = 200)
    public static void silenceIsANoAndTheHostHuntsYou(GameTestHelper helper) {
        MichaelEntity m = spawn(helper);
        ServerPlayer p = hunter(helper, "sn-test-michael-no", 3);
        ServerPlayer q = hunter(helper, "sn-test-michael-refuses", -3);
        helper.runAfterDelay(5, () -> {
            VesselPossession.ask(p, m);
            VesselPossession.ask(q, m);
            helper.assertTrue(VesselPossession.answer(q, m.getId(), false), "no, in time");
            helper.assertTrue(q.hasEffect(AllMobEffects.HEAVENS_MARK), "a no is marked");
        });
        helper.runAfterDelay(5 + MichaelBalance.YES_DECIDE_TICKS + MichaelBalance.YES_GRACE_TICKS + 5, () -> {
            helper.assertTrue(p.hasEffect(AllMobEffects.HEAVENS_MARK), "silence is a no");
            helper.assertFalse(VesselPossession.answer(p, m.getId(), true), "a yes too late counts for nothing");
            helper.assertFalse(p.hasEffect(AllMobEffects.VESSEL), "he does not wear them");
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- the Host --------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "michael_host", timeoutTicks = 80)
    public static void theHostBreaksWhenItsCaptainFalls(GameTestHelper helper) {
        MichaelEntity m = spawn(helper);
        helper.runAfterDelay(3, () -> {
            List<HostAngelEntity> host = m.summonHost(helper.getLevel(), 1);
            helper.assertTrue(host.size() == MichaelBalance.HOST_SOLDIERS + 1, "a company and its captain, got " + host.size());
            helper.assertTrue(host.stream().filter(HostAngelEntity::isCaptain).count() == 1, "one captain");
            helper.assertTrue(host.stream().allMatch(h -> m.minions().contains(h.getUUID())), "all of it his");
            helper.assertFalse(m.formation(0).disordered() || m.hostBroken(), "in formation");
            HostAngelEntity captain = host.stream().filter(HostAngelEntity::isCaptain).findFirst().orElseThrow();
            HostAngelEntity soldier = host.stream().filter(h -> !h.isCaptain()).findFirst().orElseThrow();
            helper.assertFalse(soldier.free(), "a soldier holds its slot");
            captain.hurt(helper.getLevel().damageSources().magic(), 1000);
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(m.formation(0).disordered(), "the captain fallen, the company breaks");
            helper.assertTrue(m.hostBroken(), "and he is exposed");
            helper.assertTrue(m.livingHost() == MichaelBalance.HOST_SOLDIERS, "the soldiers fight on");
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- the Lance ---------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "michael_lance", timeoutTicks = 200)
    public static void theLancePinsStaysAndComesBackOnRecall(GameTestHelper helper) {
        MichaelEntity m = spawn(helper);
        ServerPlayer p = CurseTests.mortal(helper, MID.offset(4, 0, 0), ItemStack.EMPTY);
        MichaelLanceEntity[] lance = new MichaelLanceEntity[1];
        helper.runAfterDelay(3, () -> {
            m.forceLook(MichaelBalance.SHADOW_WINGS_PHASE);
            m.pin(p, MichaelBalance.LANCE_PIN_TICKS);
            helper.assertTrue(m.isPinned(p), "the lance pins");
            lance[0] = m.throwLance(helper.absoluteVec(MID.offset(-8, 0, 0).getBottomCenter()));
            helper.assertFalse(m.lanceHeld(), "out of his hand");
        });
        helper.runAfterDelay(3 + MichaelBalance.LANCE_PIN_TICKS + 3, () -> helper.assertFalse(m.isPinned(p), "the pin lasts two seconds"));
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(lance[0].isAlive() && lance[0].stuck(), "it stays in the ground");
            helper.assertFalse(m.lanceHeld(), "until he calls it");
            m.recallLance();
        });
        helper.runAfterDelay(140, () -> {
            helper.assertTrue(m.lanceHeld(), "it comes back to his hand");
            helper.assertTrue(lance[0].isRemoved(), "and the thrown one is gone");
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "michael_steal", timeoutTicks = 260)
    public static void aStolenLanceRunsOutAndHurtsHimThrownBack(GameTestHelper helper) {
        MichaelEntity m = spawn(helper);
        ServerPlayer p = hunter(helper, "sn-test-michael-thief", 3);
        MichaelLanceEntity[] lance = new MichaelLanceEntity[1];
        ItemStack[] borrowed = new ItemStack[1];
        helper.runAfterDelay(3, () -> {
            m.forceLook(MichaelBalance.ARCHANGEL_PHASE);
            lance[0] = m.throwLance(helper.absoluteVec(MID.offset(-6, 0, 0).getBottomCenter()));
        });
        helper.runAfterDelay(50, () -> {
            helper.assertFalse(lance[0].steal(p), "before phase VI it cannot be pulled out");
            m.forceLook(MichaelBalance.PHASES);
            helper.assertTrue(lance[0].steal(p), "in phase VI it can");
            helper.assertTrue(lance[0].isRemoved() && m.lanceStolen(), "it is the hunter's now");
            for (ItemStack s : p.getInventory().items) if (s.is(AllItems.BORROWED_LANCE.get())) borrowed[0] = s;
            helper.assertTrue(borrowed[0] != null, "the borrowed lance is in hand");
            helper.assertFalse(BorrowedLanceItem.mustReturn(borrowed[0], p), "for now");
        });
        helper.runAfterDelay(52 + MichaelBalance.BORROWED_TICKS, () -> {
            helper.assertTrue(BorrowedLanceItem.mustReturn(borrowed[0], p), "it runs out");
            BorrowedLanceItem.sendHome(borrowed[0], p);
            helper.assertTrue(borrowed[0].isEmpty() && m.lanceHeld(), "and flies back to him");
            // Thrown back at him, it bites past his guard and he reels.
            m.setHealth(m.getMaxHealth() * 0.12f);
            float before = m.getHealth();
            m.struckByOwnLance(p);
            float lost = (before - m.getHealth()) * MichaelBalance.healthScale(2.2, 0.5, 1);
            helper.assertTrue(lost > 50, "a deep wound, true damage " + lost);
            helper.assertTrue(m.isStaggered(), "he reels");
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "michael_thrown", timeoutTicks = 120)
    public static void theHuntersLanceComesBackAndBitesAngels(GameTestHelper helper) {
        cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = hunter(helper, "sn-test-michael-lance", 0);
        p.setYRot(-90);
        p.setXRot(10);
        ThrownLanceEntity lance = ThrownLanceEntity.thrown(level, p, new ItemStack(AllItems.MICHAEL_LANCE.get()));
        level.addFreshEntity(lance);
        double[] far = new double[1];
        helper.runAfterDelay(25, () -> far[0] = lance.distanceTo(p));
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(lance.isRemoved() || lance.isNoPhysics(), "it turns for home");
            helper.assertTrue(lance.isRemoved() || lance.distanceTo(p) < far[0], "and comes back to the hand");
            HostAngelEntity angel = AllEntities.HOST_ANGEL.get().create(level);
            helper.assertTrue(ThrownLanceEntity.multiplierAgainst(angel) == ThrownLanceEntity.BANE_MULTIPLIER, "twice as hard on an angel");
            helper.assertTrue(ThrownLanceEntity.multiplierAgainst(EntityType.ZOMBIE.create(level)) == 1f, "not on a zombie");
            lance.discard();
            helper.succeed();
        });
    }

    // --- the wings ------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = "michael_flight", timeoutTicks = 20)
    public static void theWingsFlyOnHisGraceUntilTheStaminaRunsOut(GameTestHelper helper) {
        ServerPlayer p = hunter(helper, "sn-test-michael-wings", 0);
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(AllItems.SERAPH_WINGS.get()));
        WingFlight.tick(p);
        helper.assertFalse(p.getAbilities().mayfly, "the wings alone do not fly");
        p.setData(AllAttachments.HEAVEN, HeavenLedger.NONE.withGrace(true));
        WingFlight.tick(p);
        helper.assertTrue(p.getAbilities().mayfly, "with his grace they do");
        p.getAbilities().flying = true;
        p.setOnGround(false);
        WingFlight.tick(p);
        helper.assertTrue(p.getData(AllAttachments.HEAVEN).flown(), "the first flight is remembered");
        int max = WingFlight.stamina(p).max();
        for (int i = 0; i < max + 5 && p.getAbilities().flying; i++) WingFlight.tick(p);
        helper.assertFalse(p.getAbilities().flying || p.getAbilities().mayfly, "flight spends the stamina, and then they fold");
        p.setOnGround(true);
        for (int i = 0; i < 40; i++) WingFlight.tick(p);
        helper.assertTrue(p.getAbilities().mayfly, "the ground gives it back");
        p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        WingFlight.tick(p);
        helper.assertFalse(p.getAbilities().mayfly, "without the wings, no flight");
        helper.succeed();
    }

    // --- Heaven and his true form ------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "michael_heaven", timeoutTicks = 60)
    public static void heavenIsWrittenShiftedAndGivenBack(GameTestHelper helper) {
        MichaelEntity m = spawn(helper);
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            m.layHeavenNow(0);
            helper.assertFalse(m.writingHeaven(), "the Garden is down");
            ArenaController arena = m.arena();
            BlockPos grass = null;
            for (var e : arena.placedBlocks().entrySet()) {
                if (e.getValue().is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                    grass = e.getKey();
                    break;
                }
            }
            helper.assertTrue(grass != null, "the Garden's lawn is laid");
            m.layHeavenNow(2);
            helper.assertTrue(m.heavenShown() == 2, "the Throne Room stands");
            helper.assertFalse(level.getBlockState(grass).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK), "over the same ground");
            cleanup(helper);
            helper.assertFalse(level.getBlockState(grass).is(net.minecraft.world.level.block.Blocks.WHITE_CONCRETE)
                    || level.getBlockState(grass).is(net.minecraft.world.level.block.Blocks.SMOOTH_QUARTZ), "and the arena gives it back");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "michael_transform", timeoutTicks = 200)
    public static void theVesselGivesWayToHisTrueForm(GameTestHelper helper) {
        MichaelEntity m = spawn(helper);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(Math.abs(m.trueMaxHealth() - 2200f) < 1f, "about 2200 true health alone, has " + m.trueMaxHealth());
            m.forceLook(MichaelBalance.ARCHANGEL_PHASE - 1);
            m.beginTransition(MichaelBalance.ARCHANGEL_PHASE);
            helper.assertFalse(m.isArchangel(), "the vessel stands until the light peaks");
        });
        helper.runAfterDelay(3 + MichaelBalance.TRANSFORM_SWAP_TICKS + 5, () -> {
            helper.assertTrue(m.isArchangel(), "his true form");
            helper.assertTrue(m.getBbHeight() > 4f, "four and a half blocks tall, is " + m.getBbHeight());
            cleanup(helper);
            helper.succeed();
        });
    }
}
