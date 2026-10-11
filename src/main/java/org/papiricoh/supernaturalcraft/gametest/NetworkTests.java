package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.debug.AuditStats;
import org.papiricoh.supernaturalcraft.entity.boss.lilith.LilithEntity;
import org.papiricoh.supernaturalcraft.journal.HunterLogs;
import org.papiricoh.supernaturalcraft.legacy.Archive;
import org.papiricoh.supernaturalcraft.legacy.Legacies;
import org.papiricoh.supernaturalcraft.legacy.Legacy;
import org.papiricoh.supernaturalcraft.legacy.cases.table.WarTableServerHandlers;
import org.papiricoh.supernaturalcraft.legacy.research.ResearchService;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.network.JournalActionPayload;
import org.papiricoh.supernaturalcraft.network.SNNetworking;
import org.papiricoh.supernaturalcraft.network.ServerPayloadHandlers;
import org.papiricoh.supernaturalcraft.network.WarTableTerrainRequestPayload;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

/** What goes over the wire on a busy server (v0.19): requests checked and paced, syncs only when and to whom they matter. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class NetworkTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    private static JournalTests.Witness witness(GameTestHelper helper, BlockPos rel) {
        JournalTests.Witness w = new JournalTests.Witness(helper.getLevel());
        Vec3 at = helper.absoluteVec(rel.getBottomCenter());
        w.moveTo(at.x, at.y, at.z, 0, 0);
        helper.getLevel().addNewPlayer(w);
        return w;
    }

    private static void gone(GameTestHelper helper, Entity... players) {
        for (Entity e : players) helper.getLevel().removePlayerImmediately((net.minecraft.server.level.ServerPlayer) e, Entity.RemovalReason.DISCARDED);
    }

    /** Packets of one kind sent while {@code work} runs, with the audit counters on. */
    private static long sends(String type, Runnable work) {
        boolean was = AuditStats.enabled();
        AuditStats.enable(true);
        try {
            work.run();
            return AuditStats.sent().stream().filter(l -> l.key().equals(type)).mapToLong(AuditStats.Line::count).sum();
        } finally {
            AuditStats.enable(was);
        }
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "net_terrain", timeoutTicks = 40)
    public static void terrainIsForMembersAndPaced(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        for (int x = 4; x <= 6; x++) for (int z = 4; z <= 6; z++) helper.setBlock(new BlockPos(x, 1, z), AllBlocks.MAP_TABLE.get().defaultBlockState());
        BlockPos last = helper.absolutePos(new BlockPos(6, 1, 6));
        BlockState s = helper.getLevel().getBlockState(last);
        s.getBlock().setPlacedBy(helper.getLevel(), last, s, null, new ItemStack(AllItems.MAP_TABLE.get()));
        BlockPos centre = helper.absolutePos(new BlockPos(5, 1, 5));
        JournalTests.Witness outsider = witness(helper, new BlockPos(2, 1, 2)), member = witness(helper, new BlockPos(8, 1, 8));
        try {
            Legacies.set(member, Legacy.NONE.withRank(1));
            var ask = new WarTableTerrainRequestPayload(centre, List.of());
            helper.assertFalse(WarTableServerHandlers.terrain(outsider, ask), "an outsider's table shows them nothing");
            int taken = 0;
            for (int i = 0; i < 10; i++) if (WarTableServerHandlers.terrain(member, ask)) taken++;
            helper.assertTrue(taken >= 1 && taken <= 5, "a member is answered, at a pace: " + taken + " of 10 at once");
        } finally {
            gone(helper, outsider, member);
        }
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "net_journal", timeoutTicks = 40)
    public static void theJournalOnlyKeepsTheModsEntries(GameTestHelper helper) {
        JournalTests.Witness p = witness(helper, new BlockPos(2, 1, 2));
        try {
            ServerPayloadHandlers.journalAction(p, new JournalActionPayload(JournalActionPayload.MARK_READ, ResourceLocation.parse("minecraft:anything")));
            helper.assertTrue(HunterLogs.get(p).read().isEmpty(), "a foreign id is not stored");
            ServerPayloadHandlers.journalAction(p, new JournalActionPayload(JournalActionPayload.MARK_READ, SupernaturalCraft.asResource("salt")));
            helper.assertTrue(HunterLogs.get(p).read().size() == 1, "the mod's own entry is");
            HunterLogs.get(p).kill(SupernaturalCraft.asResource("vampire"));
            helper.assertTrue(HunterLogs.get(p).killsDirty, "a kill waits for the next sync");
        } finally {
            gone(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "net_cinematic", timeoutTicks = 40)
    public static void aCinematicSparesBystanders(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        LilithEntity lilith = helper.spawn(AllEntities.LILITH.get(), MID);
        ArenaController arena = ArenaSavedData.get(level).create(helper.absolutePos(MID), 12);
        lilith.bindArena(arena);
        JournalTests.Witness fighter = witness(helper, MID.offset(3, 0, 0)), bystander = witness(helper, MID.offset(20, 0, 0));
        try {
            arena.join(fighter);
            CinematicLocks.play(lilith, SupernaturalCraft.asResource("lilith_death"), 100, 64);
            helper.assertTrue(CinematicLocks.locked(fighter), "the challenger watches, untouchable");
            helper.assertFalse(CinematicLocks.locked(bystander), "the passer-by 20 blocks off keeps their camera and can be hurt");
            CinematicLocks.forgetPlayer(fighter.getUUID());
            helper.assertFalse(CinematicLocks.locked(fighter), "a skipped scene lets go of the lock");
        } finally {
            gone(helper, fighter, bystander);
            BossTests.cleanup(helper);
        }
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "net_syncs", timeoutTicks = 40)
    public static void syncsGoOutOnceAndOnlyWhatChanged(GameTestHelper helper) {
        JournalTests.Witness p = witness(helper, new BlockPos(2, 1, 2));
        try {
            // A research started: notes paid, a slot taken, one sync.
            Legacies.set(p, Legacy.NONE.withRank(1));
            Legacies.setArchive(p, Archive.EMPTY.withNotes("place", 20));
            p.getInventory().add(new ItemStack(net.minecraft.world.item.Items.PAPER, 9));
            p.getInventory().add(new ItemStack(net.minecraft.world.item.Items.INK_SAC, 9));
            long legacy = sends("legacy_sync", () -> helper.assertTrue(ResearchService.start(p, "lore:bunker") == null, "research started"));
            helper.assertTrue(legacy == 1, "one sync for the whole start, sent " + legacy);
            // Mana alone moves: the known sigils stay home.
            ArcanaData arcana = ManaManager.get(p);
            SNNetworking.syncArcana(p, true);
            helper.assertFalse(arcana.listsDirty, "sent");
            arcana.setMana(arcana.mana() - 5);
            helper.assertFalse(arcana.listsDirty, "mana alone does not send the lists");
            arcana.learn(SupernaturalCraft.asResource("smite"));
            helper.assertTrue(arcana.listsDirty, "a sigil learnt does");
        } finally {
            gone(helper, p);
        }
        helper.succeed();
    }
}
