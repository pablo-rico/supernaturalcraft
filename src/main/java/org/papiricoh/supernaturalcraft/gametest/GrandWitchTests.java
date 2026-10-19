package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.GrandWitchAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.GrandWitchBalance;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.GrandWitchEntity;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.GrandWitchSpoils;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.GrandWitchSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.RowenaAllyEntity;
import org.papiricoh.supernaturalcraft.entity.witch.CovenWitchEntity;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.ritual.effect.SummonGrandWitchEffect;

import java.util.List;

/**
 * The Grand Witch (v0.21 P6), each test in its own batch with the arena cleaned first: the Coven's rite and arena; her three
 * channelers' shield (every blow swallowed while one still channels; a broken channeler stops for 240 ticks, a dead one is gone
 * for good); past phase I her coven fights and Rowena arrives for the last (untouchable, at the hunters' side, going home when
 * it ends); frost under every hunter, the inferno round her, the crush that pins; the mass taglock and Rowena's green circles;
 * the smoke veil; her spoils.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class GrandWitchTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    private static GrandWitchEntity spawn(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        return helper.spawn(AllEntities.GRAND_WITCH.get(), MID);
    }

    /** Her arena open, her coven sent away, her AI off, in phase {@code phase}, at the middle. */
    private static void still(GameTestHelper helper, GrandWitchEntity g, int phase) {
        helper.assertTrue(g.arena() != null && g.arena().theme() == ArenaTheme.COVEN, "the Coven's arena opens round her");
        g.setNoAi(true);
        for (CovenWitchEntity w : g.channelers()) w.discard();
        g.forceLook(phase);
        Vec3 v = helper.absoluteVec(Vec3.atBottomCenterOf(MID));
        g.moveTo(v.x, v.y, v.z, 0, 0);
    }

    private static Vec3 c() {
        return Vec3.atBottomCenterOf(MID);
    }

    private static boolean landed(LivingEntity m, GrandWitchEntity g, float base) {
        float want = base * g.attackDamageMultiplier();
        return Math.abs(RowenaTests.loss(m) - want) <= want * 0.1f + 0.05f;
    }

    private static void sweep(GameTestHelper helper) {
        for (Entity e : helper.getLevel().getEntitiesOfClass(Entity.class, new AABB(helper.absolutePos(MID)).inflate(40),
                e -> e instanceof CovenWitchEntity || e instanceof RowenaAllyEntity)) {
            e.discard();
        }
    }

    // --- her rite --------------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "grand_witch_rite", timeoutTicks = 60)
    public static void theCovensRiteCallsHerAndHerSistersIntoTheirArena(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        BlockPos altar = helper.absolutePos(MID);
        helper.assertTrue(new SummonGrandWitchEffect().perform(level, altar, null), "the Grand Coven answers");
        helper.runAfterDelay(5, () -> {
            List<GrandWitchEntity> found = level.getEntitiesOfClass(GrandWitchEntity.class, new AABB(altar).inflate(24));
            helper.assertTrue(found.size() == 1, "one Grand Witch: " + found.size());
            GrandWitchEntity g = found.getFirst();
            helper.assertTrue(g.arena() != null && g.arena().theme() == ArenaTheme.COVEN && g.arena().radius() == SNConfig.GRAND_WITCH_ARENA_RADIUS.get(),
                    "the Coven's arena, round the altar");
            helper.assertTrue(Math.abs(Math.hypot(g.getX() - (altar.getX() + 0.5), g.getZ() - (altar.getZ() + 0.5)) - GrandWitchSummoning.DISTANCE) < 0.6,
                    "she steps out a few blocks from the rite");
            helper.assertTrue(g.channelers().size() == GrandWitchBalance.CHANNELERS && g.shielded(), "three sisters round her, channelling");
            helper.assertFalse(new SummonGrandWitchEffect().perform(level, altar.offset(3, 0, 0), null), "a second rite finds the world busy");
            BossTests.cleanup(helper);
            sweep(helper);
            helper.succeed();
        });
    }

    // --- P1, the Coven's Shield ---------------------------------------------------------------------------------------------

    /**
     * Every blow swallowed while one sister still channels; a sister whose bubble breaks stops for 240 ticks; a dead one is gone;
     * the last channel broken, the shield falls; the interrupted come back to it.
     */
    @GameTest(template = SNGameTests.ARENA, batch = "grand_witch_shield", timeoutTicks = 420)
    public static void herCovensChannelSwallowsEveryBlowUntilItIsBroken(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        GrandWitchEntity[] g = new GrandWitchEntity[1];
        CovenWitchEntity[] w = new CovenWitchEntity[3];
        ServerPlayer[] p = new ServerPlayer[1];
        int ready = 2 + GrandWitchEntity.INTRO_TICKS + 10, broke = ready + 6;
        helper.runAfterDelay(2, () -> g[0] = GrandWitchSummoning.summon(helper.getLevel(), helper.absolutePos(MID), null));
        helper.runAfterDelay(ready, () -> {
            helper.assertTrue(g[0].state() != GrandWitchEntity.EMERGING, "out of the smoke");
            List<CovenWitchEntity> coven = g[0].channelers();
            helper.assertTrue(coven.size() == 3 && g[0].channeling() == 3 && g[0].shielded(), "three channel, she is shielded");
            for (int i = 0; i < 3; i++) w[i] = coven.get(i);
            p[0] = CurseTests.mortal(helper, MID.offset(-4, 0, 0), ItemStack.EMPTY);
            BossHealthGuard.set(g[0], 900);
            g[0].invulnerableTime = 0;
            helper.assertFalse(g[0].hurt(g[0].damageSources().playerAttack(p[0]), 50) || g[0].getHealth() < 900, "a blow is swallowed");
            w[0].breakShield();
            w[1].kill();
        });
        helper.runAfterDelay(ready + 3, () -> {
            helper.assertFalse(w[0].channeling(), "a sister whose bubble broke stops channelling");
            helper.assertTrue(g[0].channelers().size() == 2 && g[0].channeling() == 1 && g[0].shielded(), "one still channels: still shielded");
            g[0].invulnerableTime = 0;
            helper.assertFalse(g[0].hurt(g[0].damageSources().playerAttack(p[0]), 50) || g[0].getHealth() < 900, "still swallowed");
            w[2].breakShield();
        });
        helper.runAfterDelay(broke, () -> {
            helper.assertTrue(g[0].channeling() == 0 && !g[0].shielded(), "the last channel broken: the shield falls");
            g[0].invulnerableTime = 0;
            g[0].hurt(g[0].damageSources().playerAttack(p[0]), 50);
            helper.assertTrue(g[0].getHealth() < 900, "and a blow lands");
        });
        helper.runAfterDelay(ready + GrandWitchBalance.INTERRUPT_TICKS - 20, () ->
                helper.assertFalse(w[0].channeling() || g[0].shielded(), "interrupted for " + GrandWitchBalance.INTERRUPT_TICKS + " ticks"));
        helper.runAfterDelay(broke + GrandWitchBalance.INTERRUPT_TICKS + 10, () -> {
            helper.assertTrue(w[0].channeling() && w[2].channeling(), "the interrupted channel again");
            helper.assertTrue(g[0].channelers().size() == 2 && g[0].shielded(), "the dead one is gone for good; the shield is back");
            BossTests.cleanup(helper);
            sweep(helper);
            helper.succeed();
        });
    }

    /** Past phase I her sisters stop channelling and the shield is gone; at the last phase Rowena arrives, and nothing hurts her. */
    @GameTest(template = SNGameTests.ARENA, batch = "grand_witch_phases", timeoutTicks = 80)
    public static void pastTheShieldHerCovenFightsAndRowenaArrivesForTheLast(GameTestHelper helper) {
        GrandWitchEntity g = spawn(helper);
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(g.channelers().size() == 3 && g.shielded(), "her coven channels");
            g.setNoAi(true);
            ServerPlayer p = CurseTests.mortal(helper, MID.offset(-4, 0, 0), ItemStack.EMPTY);
            g.beginTransition(2);
            helper.assertTrue(g.channeling() == 0 && !g.shielded(), "phase II: they fight instead");
            helper.assertTrue(g.ally() == null, "no Rowena yet");
            g.beginTransition(3);
            RowenaAllyEntity r = g.ally();
            helper.assertTrue(r != null && r.witch() == g, "phase III: Rowena steps out of her smoke");
            helper.assertTrue(RowenaAllyEntity.arrive(g) == r, "only one of her");
            helper.assertFalse(r.hurt(r.damageSources().playerAttack(p), 20) || r.hurt(g.damageSources().mobAttack(g), 20), "nothing hurts her");
            helper.assertTrue(r.isAlliedTo(p), "she is on the hunters' side");
            helper.assertFalse(GrandWitchAttacks.isFoe(g, r), "the Grand Witch's hexes never land on her");
            r.farewell(5);
        });
        helper.runAfterDelay(14, () -> {
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(RowenaAllyEntity.class, new AABB(helper.absolutePos(MID)).inflate(30)).isEmpty(),
                    "when it is over she goes home");
            BossTests.cleanup(helper);
            sweep(helper);
            helper.succeed();
        });
    }

    // --- P2, the Matriarch's Hexes -------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "grand_witch_frost", timeoutTicks = 60)
    public static void frostUnderEveryHunterUpToFour(GameTestHelper helper) {
        GrandWitchEntity g = spawn(helper);
        helper.runAfterDelay(4, () -> {
            still(helper, g, 2);
            Mob[] h = new Mob[5];
            for (int i = 0; i < 5; i++) {
                double a = i * Math.PI * 2 / 5;
                h[i] = RowenaTests.dummy(helper, c().add(Math.cos(a) * (5 + i), 0, Math.sin(a) * (5 + i)));
                g.track(h[i]);
            }
            Mob clear = RowenaTests.dummy(helper, c().add(-14, 0, -14));
            GrandWitchAttacks.FrostHex f = new GrandWitchAttacks.FrostHex();
            f.onWindup(g, h[0]);
            f.onActive(g, h[0]);
            for (int i = 0; i < 4; i++) {
                var slow = h[i].getEffect(MobEffects.MOVEMENT_SLOWDOWN);
                helper.assertTrue(landed(h[i], g, GrandWitchBalance.FROST_DAMAGE) && slow != null && slow.getAmplifier() == 4
                        && slow.getDuration() == GrandWitchBalance.FROST_SLOW_TICKS, "hunter " + i + ": frost and a heavy slow");
            }
            helper.assertTrue(RowenaTests.loss(h[4]) == 0, "four circles at most: the farthest hunter is spared");
            helper.assertTrue(RowenaTests.loss(clear) == 0, "nobody without a circle");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "grand_witch_inferno", timeoutTicks = 60)
    public static void theInfernoBurnsHerRedCircle(GameTestHelper helper) {
        GrandWitchEntity g = spawn(helper);
        helper.runAfterDelay(4, () -> {
            still(helper, g, 2);
            Mob near = RowenaTests.dummy(helper, c().add(4, 0, 0)), far = RowenaTests.dummy(helper, c().add(9, 0, 0));
            GrandWitchAttacks.Inferno i = new GrandWitchAttacks.Inferno();
            helper.assertTrue(i.weight(g, near) > i.weight(g, far), "she favours it when the hunter is close");
            i.onWindup(g, near);
            i.onActive(g, near);
            helper.assertTrue(landed(near, g, GrandWitchBalance.INFERNO_DAMAGE) && near.isOnFire(), "inside six blocks: fire");
            helper.assertTrue(RowenaTests.loss(far) == 0 && !far.isOnFire(), "outside it: nothing");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "grand_witch_crush", timeoutTicks = 60)
    public static void theCrushPinsWhoeverStaysInItsCircle(GameTestHelper helper) {
        GrandWitchEntity g = spawn(helper);
        helper.runAfterDelay(4, () -> {
            still(helper, g, 2);
            Mob target = RowenaTests.dummy(helper, c().add(6, 0, 0)), clear = RowenaTests.dummy(helper, c().add(-6, 0, 0));
            GrandWitchAttacks.Crush cr = new GrandWitchAttacks.Crush();
            cr.onWindup(g, target);
            cr.onActive(g, target);
            helper.assertTrue(cr.held().contains(target) && !cr.held().contains(clear), "the one in the circle is held, no one else");
            var slow = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            helper.assertTrue(landed(target, g, GrandWitchBalance.CRUSH_DAMAGE) && slow != null && slow.getDuration() == GrandWitchBalance.CRUSH_PIN_TICKS,
                    "crushed and pinned " + GrandWitchBalance.CRUSH_PIN_TICKS + " ticks");
            Vec3 held = target.position();
            target.teleportTo(held.x + 4, held.y, held.z);
            cr.tickActive(g, target, 5);
            helper.assertTrue(target.position().distanceTo(held) < 0.3, "and cannot step out");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    // --- P3, the Mass Taglock -------------------------------------------------------------------------------------------------

    /** A red circle follows every hunter; the curse lands on each one not standing in one of Rowena's two green circles. */
    @GameTest(template = SNGameTests.ARENA, batch = "grand_witch_mass", timeoutTicks = 60)
    public static void theMassTaglockSparesOnlyThoseInRowenasCircles(GameTestHelper helper) {
        GrandWitchEntity g = spawn(helper);
        helper.runAfterDelay(4, () -> {
            still(helper, g, 3);
            RowenaAllyEntity r = RowenaAllyEntity.arrive(g);
            helper.assertTrue(r != null, "Rowena at the hunters' side");
            Mob a = RowenaTests.dummy(helper, c().add(-7, 0, 0)), b = RowenaTests.dummy(helper, c().add(8, 0, 0));
            g.track(a);
            g.track(b);
            GrandWitchAttacks.MassTaglock m = new GrandWitchAttacks.MassTaglock();
            m.onWindup(g, a);
            helper.assertTrue(m.safe().size() == GrandWitchBalance.SAFE_CIRCLES, "two green circles");
            for (Vec3 s : m.safe()) {
                double toA = Math.hypot(s.x - a.getX(), s.z - a.getZ()), toB = Math.hypot(s.x - b.getX(), s.z - b.getZ());
                helper.assertTrue(Math.min(toA, toB) <= GrandWitchBalance.SAFE_REACH + 0.5 && Math.min(toA, toB) >= 2.5,
                        "each drawn near a hunter, not under them: " + toA + ", " + toB);
            }
            helper.assertTrue(RowenaTests.loss(a) == 0 && RowenaTests.loss(b) == 0, "nothing lands while the circles follow");
            Vec3 safe = m.safe().getFirst();
            a.teleportTo(safe.x, a.getY(), safe.z);
            m.onActive(g, a);
            helper.assertTrue(RowenaTests.loss(a) == 0 && !a.hasEffect(MobEffects.WEAKNESS), "the one in a green circle is spared");
            helper.assertTrue(landed(b, g, GrandWitchBalance.MASS_DAMAGE) && b.hasEffect(MobEffects.WEAKNESS), "the other is cursed and struck");
            helper.assertTrue(r.getHealth() == r.getMaxHealth(), "Rowena is never touched");
            BossTests.cleanup(helper);
            sweep(helper);
            helper.succeed();
        });
    }

    /** Her smoke veil is never used while her coven shields her; otherwise she lands behind the hunter in a yellow circle. */
    @GameTest(template = SNGameTests.ARENA, batch = "grand_witch_veil", timeoutTicks = 60)
    public static void herSmokeVeilLandsBehindTheHunter(GameTestHelper helper) {
        GrandWitchEntity g = spawn(helper);
        Mob[] t = new Mob[1];
        GrandWitchAttacks.SmokeVeil v = new GrandWitchAttacks.SmokeVeil();
        helper.runAfterDelay(4, () -> {
            t[0] = RowenaTests.dummy(helper, c().add(6, 0, 0));
            helper.assertTrue(g.shielded() && v.weight(g, t[0]) == 0, "not while shielded");
            g.forceLook(2);
        });
        helper.runAfterDelay(7, () -> {
            Mob target = t[0];
            still(helper, g, 2);
            helper.assertTrue(!g.shielded() && v.weight(g, target) > 0, "past the shield, yes");
            Vec3 to = target.position().add(2.5, 0, 0);
            Mob there = RowenaTests.dummy(helper, helper.relativeVec(to));
            v.onWindup(g, target);
            v.onActive(g, target);
            helper.assertTrue(g.position().distanceTo(to) < 0.6, "she lands behind the hunter");
            helper.assertTrue(landed(there, g, GrandWitchBalance.VEIL_DAMAGE) && there.hasEffect(MobEffects.BLINDNESS), "whoever stood there is struck and blinded");
            helper.assertTrue(RowenaTests.loss(target) == 0, "the hunter is not");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    // --- her spoils -------------------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "grand_witch_spoils", timeoutTicks = 60)
    public static void theStaffTheFirstTimeABustAlways(GameTestHelper helper) {
        GrandWitchEntity g = spawn(helper);
        helper.runAfterDelay(4, () -> {
            still(helper, g, 1);
            ServerLevel level = helper.getLevel();
            ServerPlayer first = CovenTests.fake(helper, "sn-test-gw-first", MID.offset(3, 0, 0));
            ServerPlayer veteran = CovenTests.fake(helper, "sn-test-gw-veteran", MID.offset(-3, 0, 0));
            HellTests.award(veteran, GrandWitchSpoils.ADVANCEMENT);
            Vec3 at = g.position().add(0, 1.2, 0);
            GrandWitchSpoils.drop(level, g, List.of(first, veteran), at);
            List<ItemEntity> ground = level.getEntitiesOfClass(ItemEntity.class, new AABB(BlockPos.containing(at)).inflate(3));
            int staves = count(ground, AllItems.COVEN_STAFF.get()), busts = count(ground, AllItems.GRAND_WITCH_TROPHY.get());
            ground.forEach(Entity::discard);
            helper.assertTrue(staves == 1 && busts == 2, "the Coven Staff the first time, a bust each: " + staves + ", " + busts);
            BossTests.cleanup(helper);
            sweep(helper);
            helper.succeed();
        });
    }

    private static int count(List<ItemEntity> items, net.minecraft.world.item.Item item) {
        return items.stream().filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
    }
}
