package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.balance.Balance;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss;
import org.papiricoh.supernaturalcraft.entity.boss.BossDamage;
import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import org.papiricoh.supernaturalcraft.entity.boss.BossStrike;
import org.papiricoh.supernaturalcraft.entity.boss.CappedBoss;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusEntity;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielBalance;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/**
 * The power curve's promises (v0.15): every great enemy has the curve's true health, no blow (however large, from
 * whatever source, after whatever bonus) takes more than its hard cap, health written from outside becomes one capped blow
 * that still respects the phase floor, {@code /kill} still kills, and a boss's Divine Wrath goes through armour. Each test
 * runs in its own batch (one boss per world) and clears the field first.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class BalanceTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);
    /** While set, every blow to a great enemy is multiplied a thousandfold as it comes in (another mod's bonus). */
    private static volatile boolean bonus;
    private static boolean listening;

    private static DamageSource holy(GameTestHelper helper) {
        return AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null);
    }

    private static LuciferEntity lucifer(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        return helper.spawn(AllEntities.LUCIFER.get(), MID);
    }

    /** True health {@code boss} lost to {@code blow} (its i-frames cleared first). */
    private static float lost(Entity boss, CappedBoss capped, Runnable blow) {
        net.minecraft.world.entity.LivingEntity l = (net.minecraft.world.entity.LivingEntity) boss;
        l.invulnerableTime = 0;
        float before = l.getHealth();
        blow.run();
        return (before - l.getHealth()) * capped.healthScale();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "balance_curve", timeoutTicks = 40)
    public static void everyGreatEnemyHasTheCurvesHealthAndItsHardCap(GameTestHelper helper) {
        BossTests.cleanup(helper);
        ServerLevel level = helper.getLevel();
        Vec3 at = helper.absoluteVec(MID.above().getCenter());
        for (Boss b : Boss.values()) {
            if (b == Boss.AMARA || b == Boss.BROKEN_CHORUS) continue;
            Entity e = BuiltInRegistries.ENTITY_TYPE.get(SupernaturalCraft.asResource(b.entity)).create(level);
            helper.assertTrue(e instanceof LuciferEntity, b + " should be a great enemy on Lucifer's base");
            LuciferEntity boss = (LuciferEntity) e;
            boss.moveTo(at.x, at.y, at.z, 0, 0);
            if (boss instanceof GabrielEntity g) g.setChannelClock(GabrielBalance.LAUGH_TICKS);
            float curve = Balance.bossHealth(b);
            helper.assertTrue(Math.abs(boss.trueMaxHealth() - curve) < 1f, b + ": true health " + boss.trueMaxHealth() + ", the curve says " + curve);
            helper.assertTrue(boss.getMaxHealth() <= 1024, b + ": vanilla health over the cap");
            float hard = Balance.hardCap(boss.trueMaxHealth());
            float dealt = lost(boss, boss, () -> boss.hurt(holy(helper), 1e6f));
            helper.assertTrue(dealt > hard * 0.99f && dealt <= hard * 1.001f, b + ": a 1e6 blow took " + dealt + ", hard cap " + hard);
            dealt = lost(boss, boss, () -> boss.hurt(level.damageSources().magic(), 1e9f));
            helper.assertTrue(dealt <= hard * 1.001f, b + ": a huge mundane blow took " + dealt);
            boss.discard();
        }
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "balance_amara", timeoutTicks = 60)
    public static void amaraHasTheCurvesHealthAndItsHardCap(GameTestHelper helper) {
        AmaraEntity a = AmaraTests.summon(helper);
        helper.assertTrue(Math.abs(a.trueMaxHealth() - Balance.bossHealth(Boss.AMARA)) < 1f, "true health " + a.trueMaxHealth());
        float hard = Balance.hardCap(a.trueMaxHealth());
        float anchor = a.partHealth(AmaraEntity.FIRST_ANCHOR);
        a.hurtPart(a.part(AmaraEntity.FIRST_ANCHOR), holy(helper), 1e6f);
        float part = anchor - a.partHealth(AmaraEntity.FIRST_ANCHOR);
        helper.assertTrue(part <= hard * 1.001f && part > 0, "an anchor lost " + part + ", hard cap " + hard);
        a.beginTransition(4);
        a.skipToFight();
        float dealt = lost(a, a, () -> a.hurt(holy(helper), 1e6f));
        helper.assertTrue(dealt > hard * 0.99f && dealt <= hard * 1.001f, "her final form took " + dealt + ", hard cap " + hard);
        AmaraTests.finish(helper);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "balance_chorus", timeoutTicks = 60)
    public static void theChorusHasTheCurvesHealthAndItsHardCap(GameTestHelper helper) {
        ChorusEntity c = ChorusTests.summon(helper);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(Math.abs(c.trueMaxHealth() - Balance.bossHealth(Boss.BROKEN_CHORUS)) < 1f, "true health " + c.trueMaxHealth());
            float hard = Balance.hardCap(c.trueMaxHealth());
            float pool = c.poolSum();
            c.part(ChorusEntity.FIRST_FACE).hurt(holy(helper), 1e6f);
            float dealt = pool - c.poolSum();
            helper.assertTrue(dealt > hard * 0.99f && dealt <= hard * 1.001f, "a face took " + dealt + ", hard cap " + hard);
            ChorusTests.finish(helper);
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "balance_bypass", timeoutTicks = 60)
    public static void onlyKillAndTheVoidSkipThePipeline(GameTestHelper helper) {
        LuciferEntity l = lucifer(helper);
        helper.runAfterDelay(2, () -> {
            ServerLevel level = helper.getLevel();
            float hard = Balance.hardCap(l.trueMaxHealth());
            // Any damage type flagged to bypass invulnerability (other mods add their own) is capped like any blow.
            var types = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
            for (var holder : types.getTagOrEmpty(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                DamageSource s = new DamageSource(holder);
                if (BossDamage.passesThrough(s)) continue;
                float dealt = lost(l, l, () -> l.hurt(s, 1e9f));
                helper.assertTrue(dealt <= hard * 1.001f, holder.getRegisteredName() + " took " + dealt);
            }
            helper.assertTrue(BossDamage.passesThrough(level.damageSources().genericKill()), "/kill passes through");
            helper.assertTrue(BossDamage.passesThrough(level.damageSources().fellOutOfWorld()), "the void passes through");
            helper.assertFalse(BossDamage.passesThrough(level.damageSources().magic()), "magic does not");
            l.kill();
        });
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(l.isRemoved() || !l.isAlive(), "/kill still kills a great enemy");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "balance_guard", timeoutTicks = 60)
    public static void healthWrittenFromOutsideIsOneCappedBlow(GameTestHelper helper) {
        LuciferEntity l = lucifer(helper);
        float[] expected = new float[1];
        helper.runAfterDelay(3, () -> {
            expected[0] = l.getHealth() - Balance.hardCap(l.trueMaxHealth()) / l.healthScale();
            // Another mod writes him down to 1.
            l.setHealth(1f);
        });
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(Math.abs(l.getHealth() - expected[0]) < 0.01f, "it should have cost one hard cap, health " + l.getHealth()
                    + ", expected " + expected[0]);
            helper.assertTrue(l.phase() == 1 && l.isAlive(), "and nothing more");
            BossHealthGuard.set(l, l.getMaxHealth() * 0.76f);
            l.setHealth(l.getMaxHealth() * 0.01f);
        });
        helper.runAfterDelay(7, () -> {
            helper.assertTrue(Math.abs(l.getHealth() - l.getMaxHealth() * 0.75f) < 0.01f,
                    "it should stop at the phase floor, health at " + l.getHealth() / l.getMaxHealth());
            helper.assertTrue(l.phase() == 2 && l.state() == LuciferEntity.TRANSITION, "and start the change of phase like a blow");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "balance_bonus", timeoutTicks = 60)
    public static void anIncomingBonusCannotPassTheHardCap(GameTestHelper helper) {
        if (!listening) {
            listening = true;
            NeoForge.EVENT_BUS.addListener((LivingIncomingDamageEvent e) -> {
                if (bonus && e.getEntity() instanceof CappedBoss) e.setAmount(e.getAmount() * 1000f);
            });
        }
        LuciferEntity l = lucifer(helper);
        helper.runAfterDelay(2, () -> {
            float hard = Balance.hardCap(l.trueMaxHealth());
            bonus = true;
            float dealt;
            try {
                dealt = lost(l, l, () -> l.hurt(holy(helper), 10f));
            } finally {
                bonus = false;
            }
            helper.assertTrue(dealt > 10f && dealt <= hard * 1.001f, "the bonus should be held to the hard cap " + hard + ", dealt " + dealt);
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "balance_wrath", timeoutTicks = 20)
    public static void divineWrathIgnoresArmour(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(4, 2, 4), ItemStack.EMPTY);
        // Full diamond, as attributes (test players do not tick their equipment).
        p.getAttribute(Attributes.ARMOR).setBaseValue(20);
        p.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(8);
        LuciferEntity boss = AllEntities.LUCIFER.get().create(level);
        Vec3 at = helper.absoluteVec(new BlockPos(8, 2, 4).getCenter());
        boss.moveTo(at.x, at.y, at.z, 0, 0);

        DamageSource wrath = AllDamageTypes.source(level, AllDamageTypes.DIVINE_WRATH, boss);
        float before = p.getHealth();
        p.hurt(wrath, 5f);
        helper.assertTrue(before - p.getHealth() >= 4.5f, "Divine Wrath should go through diamond, took " + (before - p.getHealth()));

        p.setHealth(p.getMaxHealth());
        p.invulnerableTime = 0;
        float total = 16f, divine = Balance.split(total).divine();
        before = p.getHealth();
        BossStrike.deal(boss, p, DamageTypes.MOB_ATTACK, total);
        float first = before - p.getHealth();
        helper.assertTrue(first >= divine * 0.9f, "at least its Divine Wrath lands, took " + first + " of " + divine);
        helper.assertTrue(first < total * 0.75f, "the rest is cut by the armour, took " + first);
        before = p.getHealth();
        BossStrike.deal(boss, p, DamageTypes.MOB_ATTACK, total);
        helper.assertTrue(before - p.getHealth() < 0.01f, "the same blow again inside the i-frames is nothing, took " + (before - p.getHealth()));
        boss.discard();
        helper.succeed();
    }
}
