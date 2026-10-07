package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.hellhound.HellhoundEntity;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import org.papiricoh.supernaturalcraft.reward.colt.ColtShot;

/** The hellhound: unseen until something holy touches it, and a demon in every rule that counts. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class HellhoundTests {

    private static final BlockPos MID = new BlockPos(5, 1, 5);

    @GameTest(template = SNGameTests.MEDIUM)
    public static void holyWaterRevealsIt(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        HellhoundEntity hound = helper.spawn(AllEntities.HELLHOUND.get(), MID);
        hound.setNoAi(true);
        helper.assertFalse(hound.isRevealed(), "a hellhound should start unseen");
        hound.hurt(helper.getLevel().damageSources().magic(), 1f);
        helper.assertFalse(hound.isRevealed(), "an unholy wound revealed it");
        hound.invulnerableTime = 0;
        hound.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.HOLY_WATER, null), 2f);
        helper.assertTrue(hound.isRevealed(), "holy water should reveal it");
        hound.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM)
    public static void itIsADemon(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        helper.assertTrue(AllEntities.HELLHOUND.get().is(AllTags.Entities.DEMONS), "hellhounds must count as demons");
        HellhoundEntity hound = helper.spawn(AllEntities.HELLHOUND.get(), MID);
        hound.setNoAi(true);
        helper.assertTrue(ColtShot.strike(helper.getLevel(), null, hound) == ColtShot.Outcome.EXECUTED, "the Colt should execute a hellhound");
        helper.assertFalse(hound.isAlive(), "the hellhound survived the Colt");
        helper.succeed();
    }
}
