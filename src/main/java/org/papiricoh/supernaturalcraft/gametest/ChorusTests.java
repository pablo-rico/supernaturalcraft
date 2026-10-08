package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChoirEchoEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusPart;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusSummoning;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.List;

/** The Broken Chorus's parts and phases. Each test runs in its own batch (one boss per world). */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class ChorusTests {

    static final BlockPos MID = new BlockPos(24, 1, 24);

    /** A Chorus over an open arena at the middle of the template, past its descent and ready to fight. */
    static ChorusEntity summon(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(MID);
        ArenaController arena = ChorusSummoning.openArena(level, at);
        ChorusEntity c = AllEntities.BROKEN_CHORUS.get().create(level);
        c.moveTo(at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 0, 0);
        c.bindArena(arena, at, List.of());
        level.addFreshEntity(c);
        c.skipToFight();
        return c;
    }

    static void finish(GameTestHelper helper) {
        BossTests.cleanup(helper);
        helper.succeed();
    }

    static DamageSource holy(GameTestHelper helper) {
        return AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null);
    }

    /** Strikes part {@code i} until it breaks (or 60 blows, each up to its hard cap). */
    static void breakPart(GameTestHelper helper, ChorusEntity c, int i) {
        for (int n = 0; n < 60 && c.partAlive(i); n++) c.part(i).hurt(holy(helper), 1e6f);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "chorus_ids", timeoutTicks = 40)
    public static void partIdsFollowTheBoss(GameTestHelper helper) {
        ChorusEntity c = summon(helper);
        for (int i = 0; i < ChorusEntity.PART_COUNT; i++) {
            helper.assertTrue(c.part(i).getId() == c.getId() + i + 1, "part " + i + " has id " + c.part(i).getId());
        }
        finish(helper);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "chorus_pick", timeoutTicks = 40)
    public static void onlyThePhasesPartsCanBeStruck(GameTestHelper helper) {
        ChorusEntity c = summon(helper);
        helper.runAfterDelay(2, () -> {
            for (int i = 0; i < ChorusEntity.PART_COUNT; i++) {
                boolean want = ChorusEntity.kindOf(i) == ChorusPart.Kind.FACE || ChorusEntity.kindOf(i) == ChorusPart.Kind.SHELL;
                helper.assertTrue(c.partPickable(i) == want, "phase 1: part " + i + " pickable=" + c.partPickable(i));
            }
            float before = c.poolSum();
            helper.assertFalse(c.part(ChorusEntity.FIRST_WING).hurt(holy(helper), 30), "a wing should shrug off blows in phase 1");
            helper.assertFalse(c.part(ChorusEntity.SHELL).hurt(holy(helper), 30), "the shell turns blows away");
            helper.assertTrue(c.poolSum() == before, "health moved without a face being struck");
            finish(helper);
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "chorus_pools", timeoutTicks = 60)
    public static void healthIsWhatThePartsHaveLeft(GameTestHelper helper) {
        ChorusEntity c = summon(helper);
        helper.runAfterDelay(2, () -> {
            // v0.15: the curve's 28 000, shared out in the shape of the old 1600.
            helper.assertTrue(Math.abs(c.poolSum() - 28_000) < 0.5f && Math.abs(c.totalPool() - 28_000) < 0.5f,
                    "its pools should hold 28000, hold " + c.poolSum());
            helper.assertTrue(Math.abs(c.getHealth() - c.getMaxHealth()) < 0.01f, "it should start at full health");
            float before = c.poolSum();
            c.part(ChorusEntity.FIRST_FACE).hurt(helper.getLevel().damageSources().magic(), 1e6f);
            float dealt = before - c.poolSum();
            helper.assertTrue(Math.abs(dealt - 420) < 0.05f, "a huge blow should be held to the hard cap 420, dealt " + dealt);
            for (int f = 0; f < 4; f++) breakPart(helper, c, ChorusEntity.FIRST_FACE + f);
            helper.assertTrue(c.phase() == 2 && c.state() == ChorusEntity.TRANSITION, "breaking every face should open phase 2");
            helper.assertTrue(Math.abs(c.poolSum() - 21_000) < 0.5f, "the faces held 7000, so 21000 should be left: " + c.poolSum());
            helper.assertTrue(Math.abs(c.getHealth() / c.getMaxHealth() - 0.75f) < 0.001f, "health should show three quarters");
            helper.assertTrue(ChorusSummoning.ARENA_RADIUS > 0 && c.aliveOf(ChorusPart.Kind.FACE) == 0, "faces gone");
            int echoes = helper.getLevel().getEntitiesOfClass(ChoirEchoEntity.class, c.getBoundingBox().inflate(40)).size();
            helper.assertTrue(echoes == 6, "four broken faces should have freed six echoes (at most), found " + echoes);
            finish(helper);
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "chorus_wheels", timeoutTicks = 40)
    public static void wheelsChangeSpeedWithoutJumping(GameTestHelper helper) {
        ChorusEntity c = summon(helper);
        helper.runAfterDelay(30, () -> {
            double now = c.time(0);
            double[] before = new double[3];
            for (int w = 0; w < 3; w++) before[w] = c.wheelSpin(w, now);
            c.setWheelSpeed(1);
            for (int w = 0; w < 3; w++) {
                double after = c.wheelSpin(w, now);
                double diff = Math.abs(Math.IEEEremainder(after - before[w], Math.PI * 2));
                helper.assertTrue(diff < 1e-3, "wheel " + w + " jumped by " + diff);
            }
            helper.assertTrue(c.wheelSpin(0, now + 10) - c.wheelSpin(0, now) > 0.3, "the outer wheel should speed up");
            finish(helper);
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "chorus_eyes", timeoutTicks = 60)
    public static void eyesAreStruckOnlyWhileOpen(GameTestHelper helper) {
        ChorusEntity c = summon(helper);
        helper.runAfterDelay(2, () -> {
            for (int f = 0; f < 4; f++) breakPart(helper, c, ChorusEntity.FIRST_FACE + f);
            c.skipToFight();
            for (int w = 0; w < 6; w++) breakPart(helper, c, ChorusEntity.FIRST_WING + w);
            c.skipToFight();
            helper.assertTrue(c.phase() == 3, "phase 3 after the wings, is " + c.phase());
            double t = c.time(0);
            int open = -1, shut = -1;
            for (int e = 0; e < 12; e++) {
                if (c.eyeOpen(e, t)) open = e;
                else shut = e;
            }
            helper.assertTrue(open >= 0 && shut >= 0, "some eyes should be open and some shut");
            helper.assertTrue(c.partPickable(ChorusEntity.FIRST_EYE + open), "an open eye can be struck");
            helper.assertFalse(c.partPickable(ChorusEntity.FIRST_EYE + shut), "a shut eye cannot");
            helper.assertFalse(c.part(ChorusEntity.FIRST_EYE + shut).hurt(holy(helper), 20), "a shut eye took damage");
            finish(helper);
        });
    }

    // --- the first phase's rules ----------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "chorus_gaze", timeoutTicks = 40)
    public static void theGazeJudgesSightAndAttention(GameTestHelper helper) {
        ChorusEntity c = summon(helper);
        helper.runAfterDelay(2, () -> {
            net.minecraft.server.level.ServerPlayer p = CurseTests.mortal(helper, new BlockPos(24, 1, 34), net.minecraft.world.item.ItemStack.EMPTY);
            net.minecraft.world.phys.Vec3 eye = helper.absoluteVec(new net.minecraft.world.phys.Vec3(24.5, 6, 24.5));
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, eye);
            helper.assertTrue(org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusGaze.verdict(c, eye, p)
                    == org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusBalance.Gaze.FULL, "looking into an eye in plain sight: full judgment");
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, p.getEyePosition().add(0, 0, 10));
            helper.assertTrue(org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusGaze.verdict(c, eye, p)
                    == org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusBalance.Gaze.BURN, "seen but looking away: a burn");
            for (int y = 1; y <= 6; y++) {
                for (int x = 22; x <= 26; x++) helper.setBlock(new BlockPos(x, y, 31), net.minecraft.world.level.block.Blocks.STONE);
            }
            helper.assertTrue(org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusGaze.verdict(c, eye, p)
                    == org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusBalance.Gaze.NONE, "behind stone: unseen");
            finish(helper);
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "chorus_voices", timeoutTicks = 40)
    public static void aBrokenFaceFallsSilent(GameTestHelper helper) {
        ChorusEntity c = summon(helper);
        helper.runAfterDelay(2, () -> {
            var target = helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(24, 1, 34));
            helper.assertTrue(new org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusAttacks.LionRoar().weight(c, target) > 0,
                    "the lion roars while it has its face");
            breakPart(helper, c, ChorusEntity.FIRST_FACE + org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusAttacks.LION);
            helper.assertTrue(new org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusAttacks.LionRoar().weight(c, target) == 0,
                    "a broken lion face must not roar");
            helper.assertTrue(new org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusAttacks.Litany().weight(c, target) > 0,
                    "the man still sings");
            int echoes = helper.getLevel().getEntitiesOfClass(ChoirEchoEntity.class, c.getBoundingBox().inflate(40)).size();
            helper.assertTrue(echoes == 2, "a broken face frees two echoes, found " + echoes);
            finish(helper);
        });
    }

    /** A Chorus bound to the seven bells of a ChoirAltarTests ring around the middle. */
    static ChorusEntity summonWithBells(GameTestHelper helper) {
        ChorusEntity c = summon(helper);
        java.util.List<BlockPos> bells = new java.util.ArrayList<>();
        for (int i = 0; i < 7; i++) {
            helper.setBlock(ChoirAltarTests.bell(i), org.papiricoh.supernaturalcraft.registry.AllBlocks.CHOIR_BELLS.get(i).get());
            bells.add(helper.absolutePos(ChoirAltarTests.bell(i)));
        }
        c.bindArena(c.arena(), helper.absolutePos(MID), bells);
        return c;
    }

    @GameTest(template = SNGameTests.ARENA, batch = "chorus_hymn_bell", timeoutTicks = 60)
    public static void theRightBellBreaksTheHymn(GameTestHelper helper) {
        ChorusEntity c = summonWithBells(helper);
        var hymn = new org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusAttacks.TheHymn();
        net.minecraft.server.level.ServerPlayer p = CurseTests.mortal(helper, new BlockPos(24, 1, 34), net.minecraft.world.item.ItemStack.EMPTY);
        helper.runAfterDelay(2, () -> {
            hymn.onWindup(c, p);
            hymn.tickActive(c, p, 1);
            int note = hymn.note();
            helper.assertTrue(note >= 0 && c.hymnNote() == note, "it should be singing a note");
            BlockPos lit = helper.absolutePos(ChoirAltarTests.bell(note));
            helper.assertTrue(helper.getLevel().getBlockState(lit).getValue(org.papiricoh.supernaturalcraft.chorus.ChoirBellBlock.LIT),
                    "the sung bell should glow");
            float before = p.getHealth();
            hymn.bell(c, (note + 1) % 7, p);
            helper.assertTrue(p.getHealth() < before, "a wrong bell should jar its ringer");
            helper.assertFalse(hymn.broken(), "a wrong bell does not break the Hymn");
            hymn.bell(c, note, p);
            helper.assertTrue(hymn.broken(), "the right bell breaks it");
        });
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(c.kneeling(), "a broken Hymn brings it to its knees");
            hymn.onEnd(c);
            helper.assertFalse(helper.getLevel().getBlockState(helper.absolutePos(ChoirAltarTests.bell(0)))
                    .getValue(org.papiricoh.supernaturalcraft.chorus.ChoirBellBlock.LIT), "no bell should glow after");
            float before = c.poolSum();
            c.part(ChorusEntity.FIRST_FACE).hurt(helper.getLevel().damageSources().magic(), 10);
            helper.assertTrue(Math.abs(before - c.poolSum() - 15) < 0.01f, "kneeling it takes half again, took " + (before - c.poolSum()));
            finish(helper);
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "chorus_hymn_shadow", timeoutTicks = 40)
    public static void theHymnSparesThoseInShadow(GameTestHelper helper) {
        ChorusEntity c = summon(helper);
        helper.runAfterDelay(2, () -> {
            net.minecraft.server.level.ServerPlayer open = CurseTests.mortal(helper, new BlockPos(14, 1, 24), net.minecraft.world.item.ItemStack.EMPTY);
            net.minecraft.server.level.ServerPlayer hidden = CurseTests.mortal(helper, new BlockPos(38, 1, 24), net.minecraft.world.item.ItemStack.EMPTY);
            // A roofed alcove: no line from the halo, far overhead, reaches him.
            for (int y = 1; y <= 3; y++) {
                for (int z = 22; z <= 26; z++) helper.setBlock(new BlockPos(36, y, z), net.minecraft.world.level.block.Blocks.STONE);
            }
            for (int x = 36; x <= 40; x++) {
                for (int z = 22; z <= 26; z++) helper.setBlock(new BlockPos(x, 3, z), net.minecraft.world.level.block.Blocks.STONE);
            }
            float o = open.getHealth(), h = hidden.getHealth();
            org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusAttacks.TheHymn.unmaking(c, java.util.List.of(open, hidden));
            helper.assertTrue(open.getHealth() < o, "the Hymn should strike the one in the open");
            helper.assertTrue(hidden.getHealth() == h, "the one in shadow should be spared");
            finish(helper);
        });
    }

    // --- transitions and the end -------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "chorus_collapse", timeoutTicks = 200)
    public static void eachPhaseDropsARingOfThePlatform(GameTestHelper helper) {
        ChorusEntity c = summon(helper);
        helper.runAfterDelay(2, () -> {
            for (int f = 0; f < 4; f++) breakPart(helper, c, ChorusEntity.FIRST_FACE + f);
            helper.assertTrue(c.arena().floorRadius() == ChorusEntity.FLOOR_RADIUS[1], "the platform is whole before the change");
        });
        helper.runAfterDelay(2 + ChorusEntity.TRANSITION_TICKS[2] / 2 + 4, () -> {
            helper.assertTrue(c.arena().floorRadius() == ChorusEntity.FLOOR_RADIUS[2], "phase 2 should leave a floor of "
                    + ChorusEntity.FLOOR_RADIUS[2] + ", left " + c.arena().floorRadius());
            helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.AIR, new BlockPos(24 + 20, 0, 24));
            helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.STONE, new BlockPos(24 + 10, 0, 24));
            finish(helper);
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "chorus_death", timeoutTicks = 80)
    public static void itsDeathMendsTheSummitAndTheSky(GameTestHelper helper) {
        ChorusEntity c = summon(helper);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(helper.getLevel().getLevelData().isThundering(), "the fight holds a storm");
            for (int f = 0; f < 4; f++) breakPart(helper, c, ChorusEntity.FIRST_FACE + f);
            c.skipToFight();
            for (int w = 0; w < 6; w++) breakPart(helper, c, ChorusEntity.FIRST_WING + w);
            c.skipToFight();
            c.forceEyes(0xFFF);
            for (int e = 0; e < 12; e++) breakPart(helper, c, ChorusEntity.FIRST_EYE + e);
            c.skipToFight();
            helper.assertTrue(c.phase() == 4, "the last voice remains, phase " + c.phase());
            breakPart(helper, c, ChorusEntity.CORE);
            helper.assertTrue(c.state() == ChorusEntity.DYING, "breaking the core should begin its death");
            c.skipDying();
        });
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(c.isRemoved(), "it should be gone");
            var arenas = org.papiricoh.supernaturalcraft.arena.ArenaSavedData.get(helper.getLevel()).all();
            helper.assertTrue(arenas.stream().noneMatch(org.papiricoh.supernaturalcraft.arena.ArenaController::isActive), "its arena should close");
            helper.assertFalse(helper.getLevel().getLevelData().isThundering(), "the storm should be let go");
            finish(helper);
        });
    }
}
