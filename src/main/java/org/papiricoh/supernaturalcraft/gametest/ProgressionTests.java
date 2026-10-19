package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.arena.PendingReturns;
import org.papiricoh.supernaturalcraft.arena.Stake;
import org.papiricoh.supernaturalcraft.arena.Stakes;
import org.papiricoh.supernaturalcraft.bowl.page.LostPages;
import org.papiricoh.supernaturalcraft.eclipse.EclipseSavedData;
import org.papiricoh.supernaturalcraft.eclipse.Eclipses;
import org.papiricoh.supernaturalcraft.entity.boss.lilith.LilithEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.legacy.Legacies;
import org.papiricoh.supernaturalcraft.legacy.Legacy;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseFile;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseOffice;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseSavedData;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseSites;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.ritual.block.RitualAltarBlockEntity;
import org.papiricoh.supernaturalcraft.ritual.effect.SummonLuciferEffect;
import org.papiricoh.supernaturalcraft.util.WorldClock;

import java.util.List;
import java.util.UUID;

/**
 * Nothing on the main road can be lost for good (v0.19): summonings that do not take spend nothing, fights that are not won
 * give their keys back (also across a restart), every challenger gets their own key, pages and maps come back, and locks a fight
 * holds on the world are let go however it ends.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class ProgressionTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);
    private static final BlockPos ALTAR = new BlockPos(5, 1, 5);

    private static ServerPlayer fake(GameTestHelper helper, String name) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    private static List<ItemStack> owed(GameTestHelper helper, UUID who) {
        return PendingReturns.get(helper.getLevel().getServer()).peek(who);
    }

    private static boolean owes(GameTestHelper helper, UUID who, net.minecraft.world.item.Item item, int n) {
        return owed(helper, who).stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum() == n;
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "progression_failed_ritual", timeoutTicks = 200)
    public static void aFailedRitualGivesItsActivatorBack(GameTestHelper helper) {
        // The Hymnal Map in a flat world: the circle is right and the map is spent, but no spire answers.
        SNGameTests.floor(helper, 11, 11);
        helper.setBlock(ALTAR, AllBlocks.RITUAL_ALTAR.get().defaultBlockState());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) helper.setBlock(ALTAR.offset(dx, 0, dz), AllBlocks.CHALK_LINE.get().defaultBlockState());
            }
        }
        ServerPlayer p = fake(helper, "sn-test-ritualist");
        ManaManager.get(p).setMana(100);
        RitualAltarBlockEntity altar = (RitualAltarBlockEntity) helper.getBlockEntity(ALTAR);
        for (ItemStack s : List.of(new ItemStack(Items.COMPASS), new ItemStack(Items.FEATHER), new ItemStack(Items.GOLD_INGOT),
                new ItemStack(Items.AMETHYST_SHARD), new ItemStack(AllItems.HOLY_WATER.get()))) {
            p.setItemInHand(InteractionHand.MAIN_HAND, s);
            altar.onUse(p, InteractionHand.MAIN_HAND, s);
        }
        ItemStack map = new ItemStack(Items.MAP);
        p.setItemInHand(InteractionHand.MAIN_HAND, map);
        altar.onUse(p, InteractionHand.MAIN_HAND, map);
        helper.assertTrue(altar.isChanneling() && map.isEmpty(), "the ritual started and took the blank map");
        helper.succeedWhen(() -> {
            helper.assertFalse(altar.isChanneling(), "still channelling");
            // A fake player is never on the player list: what comes back is owed to them.
            helper.assertTrue(owes(helper, p.getUUID(), Items.MAP, 1), "the blank map comes back: " + owed(helper, p.getUUID()));
            helper.assertTrue(altar.offerings().stream().filter(s -> !s.isEmpty()).count() == 5, "and the offerings stay");
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "progression_busy", timeoutTicks = 40)
    public static void aSummonIsRefusedBeforeAnythingIsSpentWhileAFightHoldsTheWorld(GameTestHelper helper) {
        BossTests.cleanup(helper);
        ServerLevel level = helper.getLevel();
        SummonLuciferEffect summon = new SummonLuciferEffect();
        helper.assertTrue(summon.precheck(level, helper.absolutePos(MID), null) == null, "a free world takes the summoning");
        ArenaController other = ArenaSavedData.get(level).create(helper.absolutePos(MID.offset(10, 0, 0)), 8);
        var refusal = summon.precheck(level, helper.absolutePos(MID), null);
        helper.assertTrue(refusal != null && refusal.getString().contains(String.valueOf(other.center().getX())),
                "refused, naming where the other fight is: " + refusal);
        BossTests.cleanup(helper);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "progression_stake_lost", timeoutTicks = 60)
    public static void aLostFightGivesItsKeysBack(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        UUID owner = UUID.randomUUID();
        Stakes.offer(Stakes.of(owner, List.of(new ItemStack(AllItems.KEY_TO_THE_CAGE.get()), new ItemStack(AllItems.LAST_SEAL.get()),
                new ItemStack(AllItems.DEMON_BLOOD.get()))));
        boolean summoned = LuciferSummoning.summon(level, helper.absolutePos(MID), null);
        Stakes.offer(null);
        helper.assertTrue(summoned, "Lucifer rose");
        LuciferEntity lucifer = level.getEntitiesOfClass(LuciferEntity.class, new AABB(helper.absolutePos(MID)).inflate(30)).getFirst();
        ArenaController arena = lucifer.arena();
        helper.assertTrue(arena != null && arena.stake() != null && arena.stake().items().size() == 2,
                "his arena holds the keys, not the common offerings");
        helper.runAfterDelay(2, () -> {
            // Everyone fled: he goes back to his Cage.
            arena.beginRestore(false);
            arena.tick(level);
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(owes(helper, owner, AllItems.KEY_TO_THE_CAGE.get(), 1) && owes(helper, owner, AllItems.LAST_SEAL.get(), 1),
                    "the key and the seal come back: " + owed(helper, owner));
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(MID)).inflate(30),
                    e -> e.getItem().is(AllItems.CRACKED_KEY.get())).isEmpty(), "no cracked key on top of the whole one");
            PendingReturns.get(level.getServer()).take(owner);
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "progression_stake_restart", timeoutTicks = 40)
    public static void aRestartMidFightOwesTheKeysAndAVictoryKeepsThem(GameTestHelper helper) {
        BossTests.cleanup(helper);
        ServerLevel level = helper.getLevel();
        UUID owner = UUID.randomUUID();
        Stakes.offer(new Stake(owner, List.of(new ItemStack(AllItems.SHATTERED_HYMN.get())), false));
        ArenaController cut = ArenaSavedData.get(level).create(helper.absolutePos(MID), 8);
        // The stake survives a save and a load.
        var tag = cut.save(level.registryAccess());
        ArenaController loaded = ArenaController.load(tag, ArenaController.blockLookup(level), level.registryAccess());
        helper.assertTrue(loaded.stake() != null && loaded.stake().items().getFirst().is(AllItems.SHATTERED_HYMN.get()), "saved with the arena");
        cut.restoreNow(level, true);
        helper.assertTrue(owes(helper, owner, AllItems.SHATTERED_HYMN.get(), 1), "a fight cut short by a restart owes its hymn");
        PendingReturns.get(level.getServer()).take(owner);

        Stakes.offer(new Stake(owner, List.of(new ItemStack(AllItems.SHATTERED_HYMN.get())), false));
        ArenaController won = ArenaSavedData.get(level).create(helper.absolutePos(MID), 8);
        won.beginRestore(true);
        won.tick(level);
        helper.assertTrue(owed(helper, owner).isEmpty(), "a won fight keeps what it was given");

        Stakes.offer(new Stake(owner, List.of(new ItemStack(AllItems.ANGEL_TABLET.get())), false));
        ArenaController lent = ArenaSavedData.get(level).create(helper.absolutePos(MID), 8);
        lent.lendStake();
        lent.beginRestore(true);
        lent.tick(level);
        helper.assertTrue(owes(helper, owner, AllItems.ANGEL_TABLET.get(), 1), "what was only lent comes back after a victory too");
        PendingReturns.get(level.getServer()).take(owner);
        BossTests.cleanup(helper);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "progression_shares", timeoutTicks = 40)
    public static void everyChallengerGetsTheirOwnSeal(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        LilithEntity lilith = helper.spawn(AllEntities.LILITH.get(), MID);
        ArenaController arena = ArenaSavedData.get(level).create(helper.absolutePos(MID), 16);
        lilith.bindArena(arena);
        ServerPlayer a = fake(helper, "sn-test-a"), b = fake(helper, "sn-test-b");
        arena.join(a);
        arena.join(b);
        List<ItemEntity> drops = new java.util.ArrayList<>(List.of(
                new ItemEntity(level, lilith.getX(), lilith.getY(), lilith.getZ(), new ItemStack(AllItems.LAST_SEAL.get())),
                new ItemEntity(level, lilith.getX(), lilith.getY(), lilith.getZ(), new ItemStack(AllItems.LILITH_TROPHY.get()))));
        NeoForge.EVENT_BUS.post(new LivingDropsEvent(lilith, level.damageSources().playerAttack(a), drops, true));
        // Lilith is a demon: now and then a torn sigil page falls with anything a hunter kills (SigilPageDrops, 8 %). Leave it out.
        drops.removeIf(e -> e.getItem().is(AllItems.SIGIL_PAGE.get()));
        helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().is(AllItems.LILITH_TROPHY.get()), "only the trophy falls: " + drops
                + " arena " + org.papiricoh.supernaturalcraft.arena.BossShares.arenaOf(level, lilith) + " fighters "
                + org.papiricoh.supernaturalcraft.arena.BossShares.fighters(level, lilith, 48) + " bosses tag " + lilith.getType().is(org.papiricoh.supernaturalcraft.registry.AllTags.Entities.BOSSES));
        helper.assertTrue(owes(helper, a.getUUID(), AllItems.LAST_SEAL.get(), 1) && owes(helper, b.getUUID(), AllItems.LAST_SEAL.get(), 1),
                "each challenger gets their own seal");
        PendingReturns.get(level.getServer()).take(a.getUUID());
        PendingReturns.get(level.getServer()).take(b.getUUID());
        BossTests.cleanup(helper);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "progression_pages", timeoutTicks = 40)
    public static void aLostPageComesBack(GameTestHelper helper) {
        ServerPlayer p = fake(helper, "sn-test-pages");
        p.getInventory().clearContent();
        p.getEnderChestInventory().clearContent();
        p.getPersistentData().remove("supernaturalcraft_page_reissued");
        helper.assertTrue(LostPages.check(p) == 0, "nothing earned, nothing given");
        HellTests.award(p, "main/heeded_the_call");
        helper.assertTrue(LostPages.check(p) == 1, "the Messenger's page comes back");
        helper.assertTrue(LostPages.carries(p, LostPages.MESSENGER), "it is in the inventory");
        helper.assertTrue(LostPages.check(p) == 0, "not twice while it is carried");
        p.getInventory().clearContent();
        helper.assertTrue(LostPages.check(p) == 0, "and not again the same day");
        ManaManager.get(p).learnRite(LostPages.MESSENGER);
        p.getPersistentData().remove("supernaturalcraft_page_reissued");
        helper.assertTrue(LostPages.check(p) == 0, "a learnt rite needs no page");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "progression_eclipse", timeoutTicks = 40)
    public static void anEclipseLockWithoutTheDarknessIsLetGo(GameTestHelper helper) {
        BossTests.cleanup(helper);
        ServerLevel level = helper.getLevel();
        try {
            Eclipses.begin(level, helper.absolutePos(MID), 2400);
            Eclipses.lock(level, true);
            ArenaController dark = ArenaSavedData.get(level).create(helper.absolutePos(MID), 8);
            dark.setTheme(ArenaTheme.DARKNESS);
            helper.assertFalse(Eclipses.releaseStaleLock(level), "held while the Darkness is fought");
            dark.restoreNow(level);
            helper.assertFalse(EclipseSavedData.get(level).locked(), "her fight ending, however it ends, lets the sky go");
            Eclipses.lock(level, true);
            ArenaSavedData.get(level).removeClosed();
            helper.assertTrue(Eclipses.releaseStaleLock(level), "a lock from an older world, with no fight, is let go");
        } finally {
            Eclipses.end(level);
            BossTests.cleanup(helper);
        }
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "progression_clock", timeoutTicks = 40)
    public static void daysTurnWithTheSunFrozen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var rule = level.getServer().overworld().getGameRules().getRule(GameRules.RULE_DAYLIGHT);
        boolean was = rule.get();
        try {
            rule.set(true, level.getServer());
            helper.assertTrue(WorldClock.day(level.getServer()) == level.getServer().overworld().getDayTime(), "the sun's day while it moves");
            rule.set(false, level.getServer());
            helper.assertTrue(WorldClock.day(level.getServer()) == level.getServer().overworld().getGameTime(), "game time while it does not");
        } finally {
            rule.set(was, level.getServer());
        }
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "progression_case_vanished", timeoutTicks = 60)
    public static void aCaseWhoseCreaturesVanishedIsClosed(GameTestHelper helper) {
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        JournalTests.Witness w = new JournalTests.Witness(level);
        BlockPos site = helper.absolutePos(MID);
        Vec3 at = Vec3.atBottomCenterOf(site.offset(4, 0, 0));
        w.moveTo(at.x, at.y, at.z, 0, 0);
        level.addNewPlayer(w);
        try {
            CaseFile file = new CaseFile(0, site, "barn", SupernaturalCraft.asResource("vampire"), "", 1, level.getGameTime(), CaseFile.OPEN);
            Legacies.set(w, Legacy.NONE.withRank(1).withCases(l -> {
                l.add(file);
                return l;
            }));
            CaseSites.tick(level, List.of(w));
            CaseSavedData.Site s = CaseSavedData.get(level).get(w.getUUID(), 0);
            helper.assertTrue(s != null && !s.targets.isEmpty(), "the case came alive");
            for (UUID u : List.copyOf(s.targets)) {
                Entity e = level.getEntity(u);
                if (e != null) e.discard();
            }
            for (int i = 0; i < CaseSites.VANISH_CHECKS - 1; i++) CaseSites.checkVanished(level, s, w);
            helper.assertTrue(CaseOffice.open(Legacies.get(w)) != null, "not on the first looks");
            CaseSites.checkVanished(level, s, w);
            CaseFile after = Legacies.get(w).cases().getFirst();
            helper.assertTrue(after.state() == CaseFile.LOST, "closed as lost: " + after);
            helper.assertTrue(Legacies.get(w).caseBlockedUntil() < 0, "with no wait for the next one");
        } finally {
            level.removePlayerImmediately(w, Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }

    /** A Bunker Key lost is a spare from Henry. */
    @GameTest(template = SNGameTests.SMALL, batch = "progression_key", timeoutTicks = 40)
    public static void henryHasASpareKey(GameTestHelper helper) {
        ServerPlayer p = fake(helper, "sn-test-key");
        p.getInventory().clearContent();
        Legacies.set(p, Legacy.NONE);
        helper.assertFalse(org.papiricoh.supernaturalcraft.legacy.LegacyServerHandlers.spareKey(p), "not for an outsider");
        Legacies.set(p, Legacy.NONE.withRank(1));
        helper.assertTrue(org.papiricoh.supernaturalcraft.legacy.LegacyServerHandlers.spareKey(p), "a member without one gets a spare");
        helper.assertFalse(org.papiricoh.supernaturalcraft.legacy.LegacyServerHandlers.spareKey(p), "only one");
        helper.succeed();
    }
}
