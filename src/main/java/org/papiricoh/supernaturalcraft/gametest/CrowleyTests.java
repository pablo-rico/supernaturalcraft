package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import org.papiricoh.supernaturalcraft.chain.ChainEvents;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.ContractBookItem;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleyBalance;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleyCompanionEntity;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleyCompanions;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleyEntity;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleySavedData;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleySpoils;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleySuitEntity;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleySummoning;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/**
 * Crowley (v0.35), each test in its own batch with the arena cleaned first: his phases (no skipping, the vanish between rooms),
 * his people's cover, the talk at his fall (only the asked hunter's answer counts; execution and recruitment set the world's
 * state, and recruitment brings the companion), the companion's retreat, the Contract Book's refusals, his suits outside the
 * chain, and the rematch rite refused while his fate is undecided.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class CrowleyTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    private static CrowleyEntity spawn(GameTestHelper helper) {
        BossTests.cleanup(helper);
        CrowleySavedData.get(helper.getLevel().getServer()).reset();
        SNGameTests.floor(helper, 48, 48);
        return helper.spawn(AllEntities.CROWLEY.get(), MID);
    }

    private static ServerPlayer hunter(GameTestHelper helper, BlockPos at) {
        return CurseTests.mortal(helper, at, ItemStack.EMPTY);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "crowley_phases", timeoutTicks = 260)
    public static void heWalksFromRoomToRoomWithoutSkippingAPhase(GameTestHelper helper) {
        CrowleyEntity c = spawn(helper);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(c.arena() != null && c.arena().theme() == ArenaTheme.ASYLUM, "his arena is the asylum's theme");
            c.setNoAi(false);
            // A blow lands him just under the first threshold: he goes, and only to the next room.
            BossHealthGuard.set(c, c.getMaxHealth() * CrowleyBalance.threshold(1) - 1);
        });
        helper.runAfterDelay(3 + CrowleyEntity.LEAVE_AT + 4, () -> helper.assertTrue(c.vanished(), "he vanishes between rooms"));
        helper.runAfterDelay(3 + CrowleyEntity.TRANSITION + 10, () -> {
            helper.assertTrue(c.phase() == 2, "one phase at a time: " + c.phase());
            helper.assertTrue(!c.vanished(), "he is back in the next room");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "crowley_cover", timeoutTicks = 60)
    public static void hisPeopleCoverHimUntilTheyAreDown(GameTestHelper helper) {
        CrowleyEntity c = spawn(helper);
        helper.runAfterDelay(3, () -> {
            c.setNoAi(true);
            ServerPlayer p = hunter(helper, new BlockPos(20, 1, 20));
            c.invulnerableTime = 0;
            BossHealthGuard.set(c, 900);
            c.hurt(c.damageSources().playerAttack(p), 20);
            float alone = 900 - c.getHealth();
            for (int i = 0; i < CrowleyBalance.GUARDED_BY; i++) {
                Mob s = helper.spawnWithNoFreeWill(AllEntities.CROWLEY_SUIT.get(), MID.offset(2 + i, 0, 0));
                c.minions().add(s.getUUID());
            }
            c.invulnerableTime = 0;
            BossHealthGuard.set(c, 900);
            c.hurt(c.damageSources().playerAttack(p), 20);
            float guarded = 900 - c.getHealth();
            helper.assertTrue(alone > 0 && Math.abs(guarded - alone * CrowleyBalance.RESIST) < alone * 0.05f,
                    "guarded he takes " + CrowleyBalance.RESIST + " of a blow: " + guarded + " vs " + alone);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "crowley_recruit", timeoutTicks = 400)
    public static void recruitedHeComesBackAsTheDecidersCompanion(GameTestHelper helper) {
        CrowleyEntity c = spawn(helper);
        ServerPlayer[] p = new ServerPlayer[2];
        helper.runAfterDelay(3, () -> {
            p[0] = hunter(helper, new BlockPos(20, 1, 20));
            p[1] = hunter(helper, new BlockPos(28, 1, 20));
            c.caseOwner(p[0].getUUID());
            c.setNoAi(false);
            BossHealthGuard.set(c, c.getMaxHealth() * CrowleyBalance.threshold(2) - 1);
        });
        helper.runAfterDelay(3 + 2 * CrowleyEntity.TRANSITION + 10, () -> BossHealthGuard.set(c, 1f));
        helper.runAfterDelay(3 + 2 * CrowleyEntity.TRANSITION + 15, () -> {
            helper.assertTrue(c.kneeling(), "beaten, he kneels");
            helper.assertTrue(c.isAlive(), "and does not die yet");
            // Fake players are not in the player index: the case's hunter is asked by hand, as the server would.
            CrowleySpoils.offerTo(c, p[0]);
        });
        helper.runAfterDelay(3 + 2 * CrowleyEntity.TRANSITION + 15 + CrowleyBalance.KNEEL_TICKS + 5, () -> {
            helper.assertTrue(p[0].getUUID().equals(c.askedOf()), "the case's hunter is asked");
            CrowleySpoils.answer(p[1], c, CrowleySpoils.HUMAN, CrowleySpoils.EXECUTE);
            helper.assertTrue(c.verdict() == null, "another hunter's answer does not count");
            CrowleySpoils.answer(p[0], c, CrowleySpoils.HUMAN, CrowleySpoils.RECRUIT);
            helper.assertTrue(Boolean.TRUE.equals(c.verdict()), "the asked hunter recruits him");
        });
        helper.runAfterDelay(3 + 2 * CrowleyEntity.TRANSITION + 15 + CrowleyBalance.KNEEL_TICKS + 50, () -> {
            CrowleySavedData d = CrowleySavedData.get(helper.getLevel().getServer());
            helper.assertTrue(d.state() == CrowleySavedData.State.RECRUITED && p[0].getUUID().equals(d.owner()), "the world remembers: " + d.state());
            helper.assertTrue(!c.isAlive(), "the boss is gone");
            helper.assertTrue(d.companion() != null, "his companion form arrived");
            // A fake owner is never online, so that one has already gone home: call him again and beat him down.
            CrowleyCompanions.arrive(helper.getLevel(), helper.absoluteVec(new net.minecraft.world.phys.Vec3(24, 1, 24)), d);
            CrowleyCompanionEntity comp = CrowleyCompanions.present(helper.getLevel().getServer(), d);
            helper.assertTrue(comp != null, "he comes when called");
            comp.retreat();
            helper.assertTrue(d.absentUntil() > helper.getLevel().getGameTime() && CrowleyCompanions.present(helper.getLevel().getServer(), d) == null,
                    "beaten down, the companion retreats for a while");
            d.reset();
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "crowley_execute", timeoutTicks = 600)
    public static void executedHeDiesAndTheWorldRemembers(GameTestHelper helper) {
        CrowleyEntity c = spawn(helper);
        ServerPlayer[] p = new ServerPlayer[1];
        helper.runAfterDelay(3, () -> {
            p[0] = hunter(helper, new BlockPos(20, 1, 20));
            c.setNoAi(false);
            BossHealthGuard.set(c, c.getMaxHealth() * CrowleyBalance.threshold(2) - 1);
        });
        helper.runAfterDelay(3 + 2 * CrowleyEntity.TRANSITION + 10, () -> BossHealthGuard.set(c, 1f));
        helper.runAfterDelay(3 + 2 * CrowleyEntity.TRANSITION + 15, () -> CrowleySpoils.decide(p[0], c, false));
        helper.runAfterDelay(3 + 2 * CrowleyEntity.TRANSITION + 15 + CrowleyBalance.DEATH_TICKS + 20, () -> {
            CrowleySavedData d = CrowleySavedData.get(helper.getLevel().getServer());
            helper.assertTrue(d.state() == CrowleySavedData.State.EXECUTED, "executed: " + d.state());
            helper.assertTrue(!c.isAlive(), "his long death has played out");
            helper.assertTrue(CrowleySummoning.riteRefusal(helper.getLevel(), helper.absolutePos(MID)) != null,
                    "the rematch is only held in his own throne room");
            d.reset();
            helper.assertTrue(CrowleySummoning.riteRefusal(helper.getLevel(), helper.absolutePos(MID)) != null, "undecided: no rematch");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "crowley_misc")
    public static void theBookRefusesTheGreatAndHisSuitsServeNoChain(GameTestHelper helper) {
        Mob zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        CrowleySuitEntity suit = (CrowleySuitEntity) helper.spawnWithNoFreeWill(AllEntities.CROWLEY_SUIT.get(), new BlockPos(5, 1, 5));
        helper.assertTrue(ContractBookItem.refusal(zombie) == null, "a zombie can sign");
        helper.assertTrue(ContractBookItem.refusal(suit) == null, "so can a suit");
        Mob boss = helper.spawnWithNoFreeWill(AllEntities.ABADDON.get(), new BlockPos(8, 1, 8));
        helper.assertTrue("too_great".equals(ContractBookItem.refusal(boss)), "a boss cannot");
        boss.discard();
        helper.assertTrue(ChainEvents.factionOf(suit) == null && !ChainEvents.eligible(suit), "his suits are no officers of the Pit");
        helper.succeed();
    }
}
