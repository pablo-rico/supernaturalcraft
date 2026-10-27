package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.chain.ChainEvents;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.ChainGates;
import org.papiricoh.supernaturalcraft.chain.ChainSavedData;
import org.papiricoh.supernaturalcraft.chain.Officer;
import org.papiricoh.supernaturalcraft.chain.OfficerSpawner;
import org.papiricoh.supernaturalcraft.chain.SucroPenthouse;
import org.papiricoh.supernaturalcraft.chain.seat.SeatWorld;
import org.papiricoh.supernaturalcraft.entity.boss.dick.DickRomanEntity;
import org.papiricoh.supernaturalcraft.entity.leviathan.BoardLeviathan;
import org.papiricoh.supernaturalcraft.entity.leviathan.LeviathanEntity;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;
import java.util.Map;

/** The Leviathans' chain (v0.28): hostile to every hunter, ended properly by borax, and Dick Roman on its throne. One batch each. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public final class LeviathanChainTests {

    private static ChainSavedData board(GameTestHelper h) {
        ChainEvents.reset(h.getLevel().getServer());
        var data = ChainSavedData.get(h.getLevel().getServer());
        ChainGates.plant(h.getLevel().getServer(), ChainFaction.LEVIATHAN, false, 11);
        return data;
    }

    private static ServerPlayer player(GameTestHelper h, Faction f) {
        ServerPlayer p = CurseTests.mortal(h, new BlockPos(5, 1, 5), ItemStack.EMPTY);
        Allegiances.set(p, new Allegiance(f, 2, 0, 0, 0, 0, -1, 0, 0, false));
        return p;
    }

    private static BoardLeviathan guard(GameTestHelper h, Officer o) {
        BoardLeviathan g = AllEntities.LEVIATHAN_GUARD.get().create(h.getLevel());
        g.moveTo(h.absoluteVec(new BlockPos(3, 1, 3).getBottomCenter()));
        g.setNoAi(true);
        h.getLevel().addFreshEntity(g);
        OfficerSpawner.bind(g, o);
        return g;
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "board_hostile")
    public static void theBoardServesNoHunterAndHearsNone(GameTestHelper h) {
        SNGameTests.floor(h, 11, 11);
        var data = board(h);
        Officer o = data.hierarchy(ChainFaction.LEVIATHAN).npcs().getFirst();
        for (Faction f : new Faction[]{Faction.DEMON, Faction.ANGEL, Faction.HUMAN})
            h.assertFalse(ChainEvents.sameSide(player(h, f), o), "A Leviathan is no " + f + "'s ally");
        h.assertFalse(o.faction.talks() || o.faction.joinable(), "SucroCorp neither talks nor takes a hunter in");
        h.assertTrue(data.hierarchy(ChainFaction.LEVIATHAN).invariantsHold(), "The Board's tree holds");
        h.assertTrue(org.papiricoh.supernaturalcraft.chain.ChainLife.front(h.getLevel().getServer()).held(ChainFaction.LEVIATHAN) > 0,
                "The Board takes its share of the front as it is founded");
        h.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "board_borax")
    public static void onlyBoraxEndsAnOfficerForGood(GameTestHelper h) {
        SNGameTests.floor(h, 11, 11);
        var data = board(h);
        var p = player(h, Faction.HUMAN);
        var officers = data.hierarchy(ChainFaction.LEVIATHAN).npcs().stream().filter(x -> !x.rank.seated()).toList();
        Officer boraxed = officers.get(0), bagged = officers.get(1);
        var a = guard(h, boraxed);
        h.assertFalse(ChainEvents.properKill(a, boraxed, h.getLevel().damageSources().playerAttack(p)), "A blade alone is no proper end");
        a.finish(p, true);
        h.assertTrue(boraxed.status == Officer.Status.DEAD, "Borax ends it for good");
        var b = guard(h, bagged);
        b.finish(p, false);
        h.assertFalse(b.getPersistentData().getBoolean(LeviathanEntity.BORAX_FINISHED), "A bag is not borax");
        h.assertFalse(bagged.alive(), "Bagged, it is gone for now");
        h.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "board_penthouse", timeoutTicks = 80)
    public static void theFounderFallsInHisOwnBoardroom(GameTestHelper h) {
        BossTests.cleanup(h);
        SNGameTests.floor(h, 48, 48);
        var data = board(h);
        Officer lord = data.hierarchy(ChainFaction.LEVIATHAN).lord();
        h.assertTrue(lord != null && lord.boss(), "Dick Roman founds the Board while he has never fallen");
        h.assertTrue(SucroPenthouse.riteRefusal(h.getLevel().getServer()) != null,
                "His rite waits while he holds the throne");
        ServerPlayer p = player(h, Faction.HUMAN);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(AllItems.ANOINTED_RIGHTEOUS_BONE.get()));
        BlockPos centre = h.absolutePos(new BlockPos(24, 1, 24)), seat = h.absolutePos(new BlockPos(24, 1, 20));
        var anchors = new SeatWorld.Anchors(centre, seat, List.of(centre, centre), centre, centre, Map.of(), List.of(),
                Map.of(SucroPenthouse.BOARDROOM, centre, SucroPenthouse.SEAT, seat));
        BlockPos floor = centre.below();
        var floorBefore = h.getLevel().getBlockState(floor);
        DickRomanEntity dick = SucroPenthouse.open(p, anchors);
        h.assertTrue(dick != null && dick.inPlace() && dick.arena() != null, "The bone opens the fight where he sits");
        h.assertTrue(OfficerSpawner.officer(dick) == lord, "He is the Lord's record");
        h.runAfterDelay(20, () -> {
            h.assertTrue(h.getLevel().getBlockState(floor) == floorBefore, "His own boardroom is not laid over");
            h.assertFalse(h.getLevel().getBlockState(centre).is(Blocks.CAVE_AIR), "Nothing of the summoned boardroom is written");
            ChainEvents.officerKilled(dick, h.getLevel().damageSources().playerAttack(p));
            h.assertTrue(lord.status == Officer.Status.DEAD, "He falls for good");
            h.assertTrue(data.founderFallen(ChainFaction.LEVIATHAN), "and never sits on the throne again");
            h.assertTrue(data.headless(ChainFaction.LEVIATHAN) && !data.court(ChainFaction.LEVIATHAN).vacant(),
                    "the Board is beheaded, with no interregnum");
            h.assertTrue(data.history().stream().anyMatch(x -> x.key().equals("dick_fell")), "The chronicle tells it");
            h.assertTrue(SucroPenthouse.riteRefusal(h.getLevel().getServer()) == null, "His rite is a rematch now");
            BossTests.cleanup(h);
            h.succeed();
        });
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "board_beheaded")
    public static void theBoardDiesWithItsFounder(GameTestHelper h) {
        SNGameTests.floor(h, 11, 11);
        var data = board(h);
        var server = h.getLevel().getServer();
        var hierarchy = data.hierarchy(ChainFaction.LEVIATHAN);
        Officer lord = hierarchy.lord();
        int officers = hierarchy.alive().size();
        h.assertTrue(org.papiricoh.supernaturalcraft.chain.ChainLife.front(server).held(ChainFaction.LEVIATHAN) > 0, "It held ground");
        // His death, as the penthouse fight ends it.
        DickRomanEntity dick = AllEntities.DICK_ROMAN.get().create(h.getLevel());
        dick.moveTo(h.absoluteVec(new BlockPos(5, 1, 5).getBottomCenter()));
        OfficerSpawner.tag(dick, lord);
        ChainEvents.officerKilled(dick, h.getLevel().damageSources().generic());
        dick.discard();
        h.assertTrue(data.headless(ChainFaction.LEVIATHAN), "The Board is beheaded");
        h.assertFalse(data.court(ChainFaction.LEVIATHAN).vacant(), "No interregnum");
        h.assertTrue(org.papiricoh.supernaturalcraft.chain.ChainLife.front(server).held(ChainFaction.LEVIATHAN) == 0,
                "Its ground is shared out");
        h.assertFalse(data.standing().contains(ChainFaction.LEVIATHAN), "It goes to war no more");
        for (int i = 0; i < 6; i++) {
            ChainEvents.conclave(server);
        }
        h.assertTrue(hierarchy.lord() == null, "Nobody takes the chair");
        h.assertTrue(hierarchy.alive().size() <= officers - 1, "Nobody new joins");
        h.assertTrue(hierarchy.invariantsHold(), "What is left still holds together");
        h.succeed();
    }
}
