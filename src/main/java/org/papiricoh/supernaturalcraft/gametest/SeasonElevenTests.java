package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.darkness.Darkness;
import org.papiricoh.supernaturalcraft.darkness.DarknessRelease;
import org.papiricoh.supernaturalcraft.darkness.DarknessRules;
import org.papiricoh.supernaturalcraft.darkness.Darknesses;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;
import org.papiricoh.supernaturalcraft.darkness.fog.FoggedVillagerEntity;
import org.papiricoh.supernaturalcraft.dyingsun.DyingSun;
import org.papiricoh.supernaturalcraft.dyingsun.DyingSunRules;
import org.papiricoh.supernaturalcraft.dyingsun.DyingSunSavedData;
import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity;

import java.util.UUID;

/**
 * Season 11 (v0.42): the dying sun (global: each test in its own batch, the world's sun put back as it was), the Darkness's road
 * per hunter, the fog, and Amara through her six bodies to peace.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class SeasonElevenTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    private static ServerPlayer hunter(GameTestHelper helper, String name) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.setGameMode(GameType.SURVIVAL);
        Vec3 v = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(2, 1, 2)));
        p.moveTo(v.x, v.y, v.z);
        Darknesses.set(p, Darkness.EMPTY);
        return p;
    }

    @GameTest(template = SNGameTests.SMALL, batch = "s11_sun_first", timeoutTicks = 40)
    public static void onlyTheFirstReleaseStartsTheSunDying(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        CompoundTag saved = DyingSunSavedData.get(server).snapshot();
        try {
            DyingSun.clear(server);
            ServerPlayer a = hunter(helper, "sn-s11-first"), b = hunter(helper, "sn-s11-second");
            helper.assertTrue(DarknessRelease.release(a), "the first hunter's road did not begin");
            helper.assertTrue(Darknesses.step(a) == DarknessRules.Step.RELEASED, "not RELEASED");
            helper.assertTrue(DyingSunSavedData.get(server).state() == DyingSunRules.DYING, "the sun is not dying");
            long start = DyingSunSavedData.get(server).startTick();
            helper.assertTrue(DarknessRelease.release(b), "the second hunter's road did not begin");
            helper.assertTrue(DyingSunSavedData.get(server).startTick() == start, "a second release restarted the sun");
            helper.assertFalse(DarknessRelease.release(a), "released twice");
            helper.assertTrue(DyingSun.restore(server), "restore said it was not dying");
            helper.assertTrue(DyingSunSavedData.get(server).state() == DyingSunRules.RESTORED, "not restored");
            ServerPlayer c = hunter(helper, "sn-s11-late");
            DarknessRelease.release(c);
            helper.assertTrue(DyingSunSavedData.get(server).state() == DyingSunRules.RESTORED, "the sun died again after peace");
            helper.assertTrue(DyingSun.compute(server) == 0f, "a restored sun is dark");
        } finally {
            DyingSunSavedData.get(server).restoreSnapshot(saved);
            DyingSun.tick(server, true);
        }
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "s11_sun_dark", timeoutTicks = 40)
    public static void theSunGrowsDarkerAndAmaraTakesItToBlack(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        CompoundTag saved = DyingSunSavedData.get(server).snapshot();
        try {
            DyingSun.preview(server, 0.5f);
            float d = DyingSun.compute(server);
            helper.assertTrue(Math.abs(d - 0.5f) < 0.02f, "darkness " + d);
            DyingSun.fightOverride(server, 0f);
            helper.assertTrue(Math.abs(DyingSun.compute(server) - DyingSunRules.FIGHT_CEILING) < 0.01f, "her countdown at zero is not the fight's depth: " + DyingSun.compute(server));
            DyingSun.fightOverride(server, -1f);
            helper.assertTrue(Math.abs(DyingSun.compute(server) - d) < 1e-4, "letting go did not give the sun back its own darkness");
        } finally {
            DyingSunSavedData.get(server).restoreSnapshot(saved);
            DyingSun.tick(server, true);
        }
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "s11_road", timeoutTicks = 40)
    public static void theRoadMovesOnlyForward(GameTestHelper helper) {
        ServerPlayer p = hunter(helper, "sn-s11-road");
        helper.assertTrue(Darknesses.advance(p, DarknessRules.Step.CASIFER), "did not advance");
        helper.assertFalse(Darknesses.advance(p, DarknessRules.Step.SUPERIOR), "went backwards");
        helper.assertTrue(Darknesses.huntsHands(p), "a hunter with Casifer cannot claim Hands");
        for (S11Assets.Hand h : new S11Assets.Hand[]{S11Assets.Hand.DELPHINES_HAND, S11Assets.Hand.ROD_OF_AARON, S11Assets.Hand.DELPHINES_HAND}) {
            Darknesses.handClaimed(p, h);
        }
        helper.assertTrue(Darknesses.get(p).handCount() == 2, "a Hand counted twice: " + Darknesses.get(p).handCount());
        Darknesses.handClaimed(p, S11Assets.Hand.TRUMPET_OF_JERICHO);
        helper.assertTrue(DarknessRules.revealDue(Darknesses.step(p), Darknesses.get(p).handCount()), "three Hands, and Chuck is not due");
        Darknesses.advance(p, DarknessRules.Step.DONE);
        helper.assertFalse(Darknesses.huntsHands(p), "Hands still claimable after peace");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "s11_fog", timeoutTicks = 60)
    public static void theFogTakesAVillagerAndLightGivesItBack(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Villager v = EntityType.VILLAGER.create(level);
        helper.assertTrue(v != null, "no villager");
        Vec3 at = helper.absoluteVec(new Vec3(5.5, 1, 5.5));
        v.moveTo(at.x, at.y, at.z);
        v.setCustomName(net.minecraft.network.chat.Component.literal("Tom"));
        level.addFreshEntity(v);
        FoggedVillagerEntity f = FoggedVillagerEntity.take(level, v);
        helper.assertTrue(f != null && v.isRemoved(), "the fog did not take him");
        helper.assertTrue(f.villager() != null, "the fogged villager forgot who he was");
        helper.assertTrue(f.cure(), "no cure");
        var back = level.getEntitiesOfClass(Villager.class, new net.minecraft.world.phys.AABB(BlockPos.containing(at)).inflate(3));
        helper.assertTrue(back.size() == 1 && back.getFirst().hasCustomName()
                && "Tom".equals(back.getFirst().getCustomName().getString()), "the villager that came back is not him");
        back.forEach(net.minecraft.world.entity.Entity::discard);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "s11_amara_six", timeoutTicks = 400)
    public static void sheWalksThroughHerSixBodiesAndMakesPeace(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        CompoundTag saved = DyingSunSavedData.get(server).snapshot();
        DyingSun.preview(server, 0.4f);
        AmaraEntity a = AmaraTests.summon(helper);
        helper.assertTrue(a.form() == AmaraEntity.MASS && a.phase() == 1, "she did not start as the Mass");
        int[] forms = {0, AmaraEntity.MASS, AmaraEntity.GIRL, AmaraEntity.WOMAN, AmaraEntity.WOMAN, AmaraEntity.VOID, AmaraEntity.WOMAN};
        float[] floors = {0, 0.84f, 0.68f, 0.52f, 0.36f, 0.18f};
        for (int phase = 1; phase < AmaraEntity.MAX_PHASE; phase++) {
            BossHealthGuard.set(a, a.getMaxHealth() * (floors[phase] + 0.005f));
            a.invulnerableTime = 0;
            if (phase == 1) {
                AmaraTests.breakAnchors(helper, a);
                a.hurtPart(a.part(AmaraEntity.CORE), AmaraTests.blow(helper), 1e6f);
            } else {
                a.setHidden(false);
                a.hurt(AmaraTests.blow(helper), 1e6f);
            }
            helper.assertTrue(a.phase() == phase + 1, "phase " + phase + " did not end: " + a.phase());
            helper.assertTrue(a.form() == forms[phase + 1], "phase " + (phase + 1) + " wears form " + a.form());
            a.skipToFight();
            if (phase + 1 == 4) helper.assertTrue(a.chuck() != null && a.chuckLight() == 1f, "her brother did not come in phase IV");
        }
        helper.assertTrue(a.isPickable(), "her body cannot be struck in the last phase");
        BossHealthGuard.set(a, 2f);
        a.invulnerableTime = 0;
        a.setHidden(false);
        a.hurt(AmaraTests.blow(helper), 1e6f);
        helper.assertTrue(a.state() == AmaraEntity.DYING, "she did not kneel");
        helper.assertTrue(a.skyStage() == 5, "no dawn in her sky");
        helper.runAfterDelay(AmaraEntity.DEATH_TICKS + 10, () -> {
            try {
                helper.assertTrue(a.isRemoved(), "she did not leave with her brother");
                helper.assertTrue(DyingSunSavedData.get(server).state() == DyingSunRules.RESTORED, "the sun did not come back");
                AmaraTests.finish(helper);
            } finally {
                DyingSunSavedData.get(server).restoreSnapshot(saved);
                DyingSun.tick(server, true);
            }
        });
    }
}
