package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.eclipse.EclipseSavedData;
import org.papiricoh.supernaturalcraft.eclipse.EclipseEvents;
import org.papiricoh.supernaturalcraft.eclipse.Eclipses;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.ritual.RitualConditions;
import org.papiricoh.supernaturalcraft.ritual.effect.BeginEclipseEffect;

import java.util.Optional;

/**
 * The ritual eclipse. It is one state for the whole dimension, so every test gets a batch of its
 * own (batches run one after another) and leaves the sun as it found it.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class EclipseTests {

    static final RitualConditions NEEDS_ECLIPSE = new RitualConditions(RitualConditions.Time.ANY, Optional.empty(), true, Optional.empty());

    private static void begin(GameTestHelper helper) {
        Eclipses.end(helper.getLevel());
        helper.assertTrue(Eclipses.begin(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), 2400), "the eclipse would not begin");
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "eclipse_begin")
    public static void theRiteEclipsesTheSunOnceAndOpensTheDarkRites(GameTestHelper helper) {
        Eclipses.end(helper.getLevel());
        helper.assertTrue(NEEDS_ECLIPSE.check(helper.getLevel(), null) != null, "an eclipse rite passed under a clear sky");
        var effect = new BeginEclipseEffect(Optional.empty());
        helper.assertTrue(effect.perform(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), null), "the rite did not begin an eclipse");
        helper.assertTrue(Eclipses.active(helper.getLevel()), "no eclipse after the rite");
        helper.assertFalse(effect.perform(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), null), "a second eclipse began over the first");
        helper.assertTrue(NEEDS_ECLIPSE.check(helper.getLevel(), null) == null, "an eclipse rite failed under the eclipse");
        Eclipses.end(helper.getLevel());
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "eclipse_lapse")
    public static void theEclipseLapsesUnlessHeld(GameTestHelper helper) {
        begin(helper);
        var data = EclipseSavedData.get(helper.getLevel());
        Eclipses.lock(helper.getLevel(), true);
        data.setEndTick(helper.getLevel().getGameTime());
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(Eclipses.active(helper.getLevel()), "a held eclipse lapsed");
            data.setLocked(false, helper.getLevel().getGameTime(), 0);
        });
        helper.runAfterDelay(6, () -> {
            helper.assertFalse(Eclipses.active(helper.getLevel()), "the eclipse outlived its time");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "eclipse_holy")
    public static void holyStrikesBiteDeeperUnderTheEclipse(GameTestHelper helper) {
        begin(helper);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(5, 1, 5));
        pig.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 5f);
        float dealt = pig.getMaxHealth() - pig.getHealth();
        Eclipses.end(helper.getLevel());
        helper.assertTrue(Math.abs(dealt - 5f * EclipseEvents.HOLY_BONUS) < 0.01f, "expected " + 5f * EclipseEvents.HOLY_BONUS + ", dealt " + dealt);
        helper.succeed();
    }

    private static Zombie bareZombie(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        helper.getLevel().setDayTime(6000);
        Zombie z = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(5, 1, 5));
        z.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        z.setBaby(false);
        return z;
    }

    /** The control: in this test world, a zombie under the noon sun does catch fire. */
    @GameTest(template = SNGameTests.MEDIUM, batch = "eclipse_sun_control", timeoutTicks = 300)
    public static void aZombieBurnsUnderAClearNoon(GameTestHelper helper) {
        Eclipses.end(helper.getLevel());
        Zombie z = bareZombie(helper);
        helper.succeedWhen(() -> helper.assertTrue(z.isOnFire(), "not burning yet"));
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "eclipse_undead", timeoutTicks = 300)
    public static void theDeadDoNotBurnUnderTheEclipse(GameTestHelper helper) {
        begin(helper);
        Zombie z = bareZombie(helper);
        helper.onEachTick(() -> {
            if (z.isOnFire()) {
                Eclipses.end(helper.getLevel());
                helper.fail("the zombie caught fire under the eclipse");
            }
        });
        helper.runAfterDelay(200, () -> {
            Eclipses.end(helper.getLevel());
            helper.succeed();
        });
    }
}
