package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.allegiance.MobReactions;
import org.papiricoh.supernaturalcraft.angel.AngelSpawns;
import org.papiricoh.supernaturalcraft.angel.HostsSavedData;
import org.papiricoh.supernaturalcraft.angel.SmiteDeath;
import org.papiricoh.supernaturalcraft.entity.angel.AngelAssets;
import org.papiricoh.supernaturalcraft.entity.angel.AngelEntity;
import org.papiricoh.supernaturalcraft.entity.angel.FallenAngelEntity;
import org.papiricoh.supernaturalcraft.entity.boss.michael.host.HostAngelEntity;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/**
 * Angels on earth (v0.22): an angel blade smites (the angel stands burning, untouchable, then dies of the blow), any other
 * weapon kills plainly and barely bites; the bosses' angels are smitten too; the fallen side with Hell's sworn and hunt
 * hunters; the world's flags gate their natural spawns.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class AngelTests {

    private static <T extends net.minecraft.world.entity.Mob> T spawn(GameTestHelper helper, net.minecraft.world.entity.EntityType<T> type, BlockPos at) {
        T e = helper.spawnWithNoFreeWill(type, at);
        if (e instanceof AngelEntity a) {
            a.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        return e;
    }

    private static ServerPlayer striker(GameTestHelper helper, ItemStack weapon) {
        ServerPlayer p = ArsenalTests.fighter(helper, new BlockPos(5, 1, 4), weapon);
        CurseTests.armed(p);
        return p;
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "angels_smite")
    public static void anAngelBladeSmitesAnAngel(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        AngelEntity angel = spawn(helper, AllEntities.ANGEL.get(), new BlockPos(5, 1, 5));
        angel.setHealth(1);
        ServerPlayer p = striker(helper, new ItemStack(AllItems.ANGEL_BLADE.get()));
        p.attack(angel);
        helper.assertTrue(angel.isAlive() && SmiteDeath.smiting(angel), "struck dead by an angel blade, it should stand burning");
        helper.runAfterDelay(10, () -> {
            angel.hurt(helper.getLevel().damageSources().generic(), 100);
            helper.assertTrue(angel.isAlive(), "it can't be hurt while it burns");
        });
        helper.runAfterDelay(AngelAssets.SMITTEN_TICKS + 3, () -> {
            helper.assertTrue(angel.isDeadOrDying() || angel.isRemoved(), "it should fall once the light is out");
            helper.assertFalse(SmiteDeath.smiting(angel), "nothing left smiting");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "angels_smite")
    public static void theBossesAngelsAreSmittenToo(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        HostAngelEntity host = spawn(helper, AllEntities.HOST_ANGEL.get(), new BlockPos(5, 1, 5));
        host.setHealth(1);
        ServerPlayer p = striker(helper, new ItemStack(AllItems.ZACHARIAHS_BLADE.get()));
        p.attack(host);
        helper.assertTrue(host.isAlive() && SmiteDeath.smiting(host), "a soldier of the Host struck by Zachariah's blade should burn");
        helper.runAfterDelay(AngelAssets.SMITTEN_TICKS + 3, () -> {
            helper.assertTrue(host.isDeadOrDying() || host.isRemoved(), "and then fall");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "angels_smite")
    public static void anyOtherWeaponKillsPlainlyAndBarelyBites(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        AngelEntity blunt = spawn(helper, AllEntities.ANGEL.get(), new BlockPos(3, 1, 5));
        AngelEntity bladed = spawn(helper, AllEntities.ANGEL.get(), new BlockPos(7, 1, 5));
        AngelEntity weak = spawn(helper, AllEntities.FALLEN_ANGEL.get(), new BlockPos(5, 1, 8));
        ServerPlayer sword = striker(helper, new ItemStack(Items.DIAMOND_SWORD));
        sword.attack(blunt);
        ServerPlayer blade = striker(helper, new ItemStack(AllItems.ANGEL_BLADE.get()));
        blade.attack(bladed);
        float swordDealt = blunt.getMaxHealth() - blunt.getHealth(), bladeDealt = bladed.getMaxHealth() - bladed.getHealth();
        helper.assertTrue(swordDealt > 0 && swordDealt < bladeDealt * 0.4f,
                "a diamond sword should barely bite (" + swordDealt + ") next to an angel blade (" + bladeDealt + ")");
        weak.setHealth(0.5f);
        ArsenalTests.fullyCharged(sword);
        sword.attack(weak);
        helper.assertTrue(weak.isDeadOrDying() && !SmiteDeath.smiting(weak), "killed by a plain sword it just dies");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "angels_sides")
    public static void eachSideKnowsItsOwn(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        AngelEntity angel = spawn(helper, AllEntities.ANGEL.get(), new BlockPos(3, 1, 5));
        FallenAngelEntity fallen = spawn(helper, AllEntities.FALLEN_ANGEL.get(), new BlockPos(7, 1, 5));
        ServerPlayer human = ArsenalTests.fighter(helper, new BlockPos(5, 1, 2), ItemStack.EMPTY);
        human.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN);
        ServerPlayer demon = ArsenalTests.fighter(helper, new BlockPos(5, 1, 8), ItemStack.EMPTY);
        demon.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN.convert(Faction.DEMON).withRank(1));
        ServerPlayer seraph = ArsenalTests.fighter(helper, new BlockPos(5, 1, 9), ItemStack.EMPTY);
        seraph.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN.convert(Faction.ANGEL).withRank(1));
        helper.assertTrue(MobReactions.ignores(fallen, demon), "a fallen angel leaves Hell's sworn alone");
        helper.assertFalse(MobReactions.ignores(fallen, human), "a fallen angel goes for a hunter");
        helper.assertFalse(MobReactions.ignores(fallen, seraph), "a fallen angel goes for Heaven's sworn");
        helper.assertTrue(MobReactions.ignores(angel, seraph), "an angel leaves Heaven's sworn alone");
        helper.assertFalse(MobReactions.ignores(angel, demon), "an angel goes for Hell's sworn");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "angels_world")
    public static void lilithAndMetatronOpenTheWay(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        HostsSavedData data = HostsSavedData.get(server);
        boolean angels = data.angels(), fallen = data.fallen();
        try {
            data.setAngels(false);
            data.setFallen(false, false);
            helper.assertFalse(AngelSpawns.allowed(helper.getLevel(), false, MobSpawnType.NATURAL), "no angels before Lilith");
            helper.assertTrue(AngelSpawns.allowed(helper.getLevel(), false, MobSpawnType.SPAWN_EGG), "an egg always works");
            data.setAngels(true);
            helper.assertTrue(AngelSpawns.allowed(helper.getLevel(), false, MobSpawnType.NATURAL), "angels once Lilith is beaten");
            helper.assertFalse(AngelSpawns.allowed(helper.getLevel(), true, MobSpawnType.NATURAL), "no fallen before the Fall");
            org.papiricoh.supernaturalcraft.angel.TheFall.open(server.overworld());
            helper.assertTrue(data.fallen() && (data.fallPending() || data.fallNight()), "Metatron beaten: the Fall is coming");
            helper.assertTrue(AngelSpawns.allowed(helper.getLevel(), true, MobSpawnType.NATURAL), "the fallen walk after it");
        } finally {
            data.setFallen(fallen, false);
            data.setAngels(angels);
            if (data.fallNight()) org.papiricoh.supernaturalcraft.angel.TheFall.endNight(server.overworld());
        }
        helper.succeed();
    }
}
