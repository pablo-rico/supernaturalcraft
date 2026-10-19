package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceCombat;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.allegiance.power.Passives;
import org.papiricoh.supernaturalcraft.mark.MarkData;
import org.papiricoh.supernaturalcraft.mark.MarkEvents;
import org.papiricoh.supernaturalcraft.mark.MarkRules;
import org.papiricoh.supernaturalcraft.mark.Marks;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.ritual.RitualRecipe;

/**
 * The Mark of Cain on the server (v0.21 P4): given once and taken off without taking a Knight's rank; its Thirst resting five
 * minutes after a kill and then climbing ({@code Marks.second}); a kill slaking it; healing cut by it; Cain's honey; the
 * bloodlust's x1.2 and the First Blade's x3 in a bearer's hand; feeding on its bearer at full; the fall of a human or an angel
 * who dies at full Thirst (and a demon's not); a Mark taken again after it was taken off coming back at half; the Knight's rite
 * needing the Mark; the Knights of old marked at login; and the one meter, a demon's Corruption draining only in the Mark's
 * bloodlust. Fake players are not ticked: {@code Marks.second} is called by hand.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class MarkTests {

    private static final float EPS = 1e-4f;

    private static ServerPlayer bearer(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = CurseTests.mortal(helper, at, ItemStack.EMPTY);
        p.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN);
        p.setData(AllAttachments.MARK, MarkData.NONE);
        return p;
    }

    private static Zombie bare(GameTestHelper helper, BlockPos at) {
        Zombie z = ArsenalTests.dummy(helper, at, 0);
        z.getAttribute(Attributes.ARMOR).setBaseValue(0);
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        z.setHealth(1000);
        return z;
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "mark_grant")
    public static void theMarkIsGivenOnceAndTakenOffWithoutTheRank(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer human = bearer(helper, new BlockPos(3, 1, 3));
        helper.assertTrue(Marks.grant(human, null, true), "granted");
        helper.assertFalse(Marks.grant(human, null, true), "only once");
        helper.assertTrue(Marks.marked(human) && Marks.thirst(human) == 0f, "quiet at first");
        helper.assertTrue(Marks.remove(human) && !Marks.marked(human) && Marks.get(human).everRemoved(), "taken off, and remembered");
        helper.assertFalse(Marks.remove(human), "nothing left to take off");
        helper.assertTrue(Marks.thirst(human) == 0f, "no Mark, no Thirst");
        // Taken again, it remembers the way.
        Marks.grant(human, null, false);
        helper.assertTrue(Math.abs(Marks.thirst(human) - MarkRules.RETAKEN_THIRST) < EPS, "taken again it comes back at half: " + Marks.thirst(human));
        helper.assertTrue(Marks.inBloodlust(human), "straight into bloodlust");
        // A Knight of Hell keeps the rank when the Mark is taken off.
        ServerPlayer knight = bearer(helper, new BlockPos(7, 1, 7));
        AllegianceTests.swear(knight, Faction.DEMON, 3);
        helper.assertTrue(Marks.migrate(knight) && Marks.marked(knight), "a Knight bears it");
        helper.assertTrue(Marks.remove(knight), "taken off");
        helper.assertTrue(Allegiances.get(knight).isDemon() && Allegiances.get(knight).rank() == 3, "still a Knight of Hell");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "mark_thirst")
    public static void theThirstRestsAfterAKillThenClimbsAndAKillSlakesIt(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer p = bearer(helper, new BlockPos(5, 1, 5));
        Marks.grant(p, null, false);
        Marks.setThirst(p, 0.3f);
        Marks.setLastKill(p, 299);
        Marks.second(p);
        helper.assertTrue(Math.abs(Marks.thirst(p) - 0.3f) < EPS, "rests within five minutes of a kill: " + Marks.thirst(p));
        Marks.setLastKill(p, 301);
        Marks.second(p);
        float risen = Marks.thirst(p);
        helper.assertTrue(risen > 0.3f && Math.abs(risen - (0.3f + MarkRules.RISE_PER_MINUTE / 60f)) < EPS,
                "past the grace it climbs a sixtieth of a hundredth a second: " + risen);
        // A real kill: the zombie's five souls slake 0.05, and the grace starts again.
        Marks.setThirst(p, 0.6f);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.NETHERITE_SWORD));
        CurseTests.armed(p);
        Zombie z = ArsenalTests.dummy(helper, new BlockPos(5, 1, 6), 180);
        z.setHealth(1f);
        p.attack(z);
        helper.assertTrue(z.isDeadOrDying(), "the zombie should die");
        helper.assertTrue(Math.abs(Marks.thirst(p) - (0.6f - MarkRules.relief(z.getMaxHealth()))) < EPS, "a kill slakes it: " + Marks.thirst(p));
        helper.assertTrue(Marks.get(p).lastKill() == helper.getLevel().getGameTime(), "and the rest starts again");
        Marks.second(p);
        helper.assertTrue(Math.abs(Marks.thirst(p) - 0.55f) < EPS, "resting after the kill");
        // An unmarked killer's kill slakes nothing.
        ServerPlayer plain = bearer(helper, new BlockPos(3, 1, 3));
        helper.assertTrue(Marks.onKill(plain, z) == 0f && !Marks.marked(plain), "no Mark, nothing to slake");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "mark_heal")
    public static void healingIsCutByTheThirstAndTheHoneyQuietsIt(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer p = bearer(helper, new BlockPos(5, 1, 5));
        Marks.grant(p, null, false);
        Marks.setThirst(p, 0.6f);
        p.setHealth(5f);
        p.heal(10f);
        helper.assertTrue(Math.abs(p.getHealth() - 12f) < 0.01f, "at 0.6 a heal of 10 keeps 7: " + p.getHealth());
        helper.assertTrue(Math.abs(Marks.heal(p, 10) - 7f) < 0.01f, "the factor");
        ServerPlayer plain = bearer(helper, new BlockPos(3, 1, 3));
        plain.setHealth(5f);
        plain.heal(10f);
        helper.assertTrue(Math.abs(plain.getHealth() - 15f) < 0.01f, "unmarked, a heal is whole: " + plain.getHealth());
        // Cain's honey: a quarter off, never below nothing; nothing to an unmarked hunter.
        Marks.setThirst(p, 0.6f);
        new ItemStack(AllItems.CAINS_HONEY.get()).finishUsingItem(helper.getLevel(), p);
        helper.assertTrue(Math.abs(Marks.thirst(p) - 0.35f) < EPS, "the honey slakes a quarter: " + Marks.thirst(p));
        Marks.setThirst(p, 0.1f);
        new ItemStack(AllItems.CAINS_HONEY.get()).finishUsingItem(helper.getLevel(), p);
        helper.assertTrue(Marks.thirst(p) == 0f, "never below nothing");
        new ItemStack(AllItems.CAINS_HONEY.get()).finishUsingItem(helper.getLevel(), plain);
        helper.assertTrue(!Marks.marked(plain) && Marks.thirst(plain) == 0f, "honey is only honey to the unmarked");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "mark_blows")
    public static void bloodlustAndTheFirstBladeHitHarderInABearersHand(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer marked = bearer(helper, new BlockPos(2, 1, 2));
        ServerPlayer plain = bearer(helper, new BlockPos(8, 1, 2));
        Marks.grant(marked, null, false);
        float[] loss = new float[6];
        // Bare hands: x1.2 in bloodlust, nothing before it.
        loss[0] = blow(helper, plain, new BlockPos(2, 1, 5));
        Marks.setThirst(marked, 0.4f);
        loss[1] = blow(helper, marked, new BlockPos(4, 1, 5));
        Marks.setThirst(marked, 0.6f);
        loss[2] = blow(helper, marked, new BlockPos(6, 1, 5));
        // The First Blade: x3 in a bearer's hand (and the bloodlust on top), nothing more in an unmarked human's.
        ItemStack blade = CurseTests.cursed(AllItems.FIRST_BLADE.get(), 0, 90, marked.getUUID());
        marked.setItemInHand(InteractionHand.MAIN_HAND, blade);
        plain.setItemInHand(InteractionHand.MAIN_HAND, CurseTests.cursed(AllItems.FIRST_BLADE.get(), 0, 90, plain.getUUID()));
        loss[3] = blow(helper, plain, new BlockPos(8, 1, 5));
        Marks.setThirst(marked, 0f);
        loss[4] = blow(helper, marked, new BlockPos(2, 1, 8));
        Marks.setThirst(marked, 0.6f);
        loss[5] = blow(helper, marked, new BlockPos(5, 1, 8));
        helper.assertTrue(loss[0] > 0 && Math.abs(loss[1] / loss[0] - 1f) < 0.01f, "no bloodlust below half: " + loss[1] + " vs " + loss[0]);
        helper.assertTrue(Math.abs(loss[2] / loss[0] - MarkRules.BLOODLUST_MELEE) < 0.02f, "bloodlust x1.2: " + loss[2] + " vs " + loss[0]);
        helper.assertTrue(Math.abs(loss[4] / loss[3] - MarkRules.FIRST_BLADE) < 0.05f, "the First Blade x3 for its bearer: " + loss[4] + " vs " + loss[3]);
        helper.assertTrue(Math.abs(loss[5] / loss[3] - MarkRules.FIRST_BLADE * MarkRules.BLOODLUST_MELEE) < 0.06f,
                "x3 and x1.2 together: " + loss[5] + " vs " + loss[3]);
        helper.assertTrue(AllegianceCombat.knightsBlade(marked, blade) && !AllegianceCombat.knightsBlade(plain, plain.getMainHandItem()),
                "the blade answers the Mark, not an unmarked human");
        helper.succeed();
    }

    /** One blow of 10 from {@code p} on a fresh unarmoured zombie at {@code at}: what it lost. */
    private static float blow(GameTestHelper helper, ServerPlayer p, BlockPos at) {
        Zombie z = bare(helper, at);
        z.hurt(p.damageSources().playerAttack(p), 10f);
        float lost = 1000f - z.getHealth();
        z.discard();
        return lost;
    }

    /** At full Thirst the Mark feeds on its bearer every ten seconds; the Thirst holds. */
    @GameTest(template = SNGameTests.MEDIUM, batch = "mark_slow", timeoutTicks = 260)
    public static void atFullThirstTheMarkFeedsOnItsBearer(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer p = bearer(helper, new BlockPos(5, 1, 5));
        Marks.grant(p, null, false);
        Marks.setThirst(p, 1f);
        p.setHealth(20f);
        ServerLevel level = helper.getLevel();
        helper.onEachTick(() -> {
            if (level.getGameTime() % 20 == 0 && p.isAlive()) Marks.second(p);
        });
        helper.runAfterDelay(225, () -> {
            helper.assertTrue(p.getHealth() <= 20f - MarkRules.FEED_HURT * 0.99f && p.getHealth() >= 20f - MarkRules.FEED_HURT * 2.01f,
                    "in eleven seconds it fed once or twice: " + p.getHealth());
            helper.assertTrue(MarkRules.full(Marks.thirst(p)), "and the Thirst holds at full");
            helper.succeed();
        });
    }

    /** A human who dies at full Thirst rises a demon (the Mark's fall), with the Thirst at half. */
    @GameTest(template = SNGameTests.MEDIUM, batch = "mark_fall", timeoutTicks = 40)
    public static void aHumanWhoDiesAtFullThirstRisesADemon(GameTestHelper helper) {
        fall(helper, Faction.HUMAN);
    }

    /** An angel's grace burns out under the Mark: they fall too. */
    @GameTest(template = SNGameTests.MEDIUM, batch = "mark_fall_angel", timeoutTicks = 40)
    public static void anAngelWhoDiesAtFullThirstFalls(GameTestHelper helper) {
        fall(helper, Faction.ANGEL);
    }

    private static void fall(GameTestHelper helper, Faction faction) {
        SNGameTests.floor(helper, 11, 11);
        JournalTests.Witness w = CovenTests.witness(helper, new BlockPos(5, 1, 5));
        if (faction != Faction.HUMAN) AllegianceTests.swear(w, faction, 1);
        else w.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN);
        w.setData(AllAttachments.MARK, MarkData.NONE);
        Marks.grant(w, null, false);
        Marks.setThirst(w, 1f);
        // A demon at full Thirst has nothing left to lose.
        ServerPlayer demon = bearer(helper, new BlockPos(2, 1, 2));
        AllegianceTests.swear(demon, Faction.DEMON, 1);
        Marks.grant(demon, null, false);
        Marks.setThirst(demon, 1f);
        boolean demonFalls = Marks.onDeath(demon);
        // Below full, nobody falls.
        ServerPlayer thirsty = bearer(helper, new BlockPos(8, 1, 8));
        Marks.grant(thirsty, null, false);
        Marks.setThirst(thirsty, 0.99f);
        boolean early = Marks.onDeath(thirsty);
        w.hurt(w.damageSources().genericKill(), Float.MAX_VALUE);
        boolean dead = w.isDeadOrDying();
        boolean pending = Allegiances.get(w).pendingDemon();
        float after = Marks.thirst(w);
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(w, false));
        Allegiance risen = Allegiances.get(w);
        boolean stillMarked = Marks.marked(w);
        CovenTests.leave(helper, w);
        helper.assertTrue(dead, "the bearer dies");
        helper.assertTrue(pending, faction + " at full Thirst: the Mark will not let them stay dead");
        helper.assertTrue(Math.abs(after - MarkRules.AFTER_FALL) < EPS, "the Thirst left at half: " + after);
        helper.assertTrue(risen.isDemon() && !risen.pendingDemon(), "they rise a demon: " + risen.faction());
        helper.assertTrue(stillMarked, "still bearing the Mark");
        helper.assertFalse(demonFalls, "a demon does not fall further");
        helper.assertFalse(early, "below full Thirst nobody falls");
        helper.succeed();
    }

    /** The Knight's rite needs the Mark already borne (taken from Cain): a Prince without it is refused. */
    @GameTest(template = SNGameTests.SMALL, batch = "mark_rite")
    public static void theKnightsRiteNeedsTheMark(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RitualRecipe rite = (RitualRecipe) level.getRecipeManager().byKey(SupernaturalCraft.asResource("ritual/mark_of_cain")).orElseThrow().value();
        ServerPlayer prince = CovenTests.fake(helper, "sn-test-mark-prince", new BlockPos(1, 1, 1));
        AllegianceTests.swear(prince, Faction.DEMON, 2);
        prince.setData(AllAttachments.MARK, MarkData.NONE);
        String unmarked = rite.conditions().check(level, prince);
        // The advancement alone is not the Mark (P6: it stays earned after an unmark).
        HellTests.award(prince, Marks.ADVANCEMENT);
        String takenOff = rite.conditions().check(level, prince);
        Marks.grant(prince, null, false);
        String marked = rite.conditions().check(level, prince);
        ServerPlayer young = CovenTests.fake(helper, "sn-test-mark-young", new BlockPos(2, 1, 2));
        AllegianceTests.swear(young, Faction.DEMON, 1);
        young.setData(AllAttachments.MARK, MarkData.NONE);
        Marks.grant(young, null, false);
        String tooYoung = rite.conditions().check(level, young);
        helper.assertTrue("message.supernaturalcraft.ritual.not_ready".equals(unmarked), "a Prince without the Mark is refused: " + unmarked);
        helper.assertTrue("message.supernaturalcraft.ritual.needs_mark".equals(takenOff), "the advancement without the Mark is refused: " + takenOff);
        helper.assertTrue(marked == null, "a Prince who bears it may rise: " + marked);
        helper.assertTrue(tooYoung != null, "a Crossroads Demon with the Mark is still too young");
        helper.succeed();
    }

    /** The Knights of Hell of older worlds bear the Mark from their next login, and it is their only bloodlust. */
    @GameTest(template = SNGameTests.SMALL, batch = "mark_knights")
    public static void theKnightsOfOldBearTheMarkAndItIsTheirOneMeter(GameTestHelper helper) {
        ServerPlayer knight = CovenTests.fake(helper, "sn-test-mark-knight", new BlockPos(1, 1, 1));
        AllegianceTests.swear(knight, Faction.DEMON, 3);
        knight.setData(AllAttachments.MARK, MarkData.NONE);
        ServerPlayer prince = CovenTests.fake(helper, "sn-test-mark-prince2", new BlockPos(2, 1, 2));
        AllegianceTests.swear(prince, Faction.DEMON, 2);
        prince.setData(AllAttachments.MARK, MarkData.NONE);
        MarkEvents.onLogin(new PlayerEvent.PlayerLoggedInEvent(knight));
        MarkEvents.onLogin(new PlayerEvent.PlayerLoggedInEvent(prince));
        helper.assertTrue(Marks.marked(knight) && Marks.thirst(knight) == 0f, "a Knight of old wakes up Marked, quiet");
        helper.assertFalse(Marks.marked(prince), "a Prince does not");
        // One made a Knight some other way is marked on the next second.
        ServerPlayer made = CovenTests.fake(helper, "sn-test-mark-made", new BlockPos(3, 1, 3));
        AllegianceTests.swear(made, Faction.DEMON, 3);
        made.setData(AllAttachments.MARK, MarkData.NONE);
        Marks.second(made);
        helper.assertTrue(Marks.marked(made), "marked on the next second");
        // The one meter: Corruption drains only in the Mark's bloodlust.
        float full = Allegiances.get(knight).essence();
        Marks.setThirst(knight, 0.4f);
        helper.assertFalse(Passives.bloodlust(knight, Allegiances.get(knight)), "no bloodlust below half");
        helper.assertTrue(Allegiances.get(knight).essence() == full, "nothing drained");
        Marks.setThirst(knight, 0.7f);
        helper.assertTrue(Passives.bloodlust(knight, Allegiances.get(knight)), "bloodlust from the Thirst");
        helper.assertTrue(Allegiances.get(knight).essence() < full, "Corruption drains");
        helper.succeed();
    }

    /** A Knight whose Mark was taken off (P6, {@code unmark}) is not marked again behind their back. */
    @GameTest(template = SNGameTests.SMALL, batch = "mark_knights")
    public static void aKnightWhoseMarkWasTakenOffStaysUnmarked(GameTestHelper helper) {
        ServerPlayer knight = CovenTests.fake(helper, "sn-test-mark-unmarked", new BlockPos(1, 1, 1));
        AllegianceTests.swear(knight, Faction.DEMON, 3);
        knight.setData(AllAttachments.MARK, MarkData.NONE);
        Marks.migrate(knight);
        Marks.remove(knight);
        Marks.second(knight);
        MarkEvents.onLogin(new PlayerEvent.PlayerLoggedInEvent(knight));
        helper.assertFalse(Marks.marked(knight), "the Mark taken off stays off (the migration is for Knights who never bore it)");
        helper.succeed();
    }
}
