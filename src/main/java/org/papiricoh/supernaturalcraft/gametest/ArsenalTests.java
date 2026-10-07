package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.weapon.melee.AngelBladeItem;
import org.papiricoh.supernaturalcraft.weapon.melee.ExorcistMaceItem;

import java.lang.reflect.Field;
import java.util.UUID;

/** The v0.2 weapons do what their tooltips promise. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class ArsenalTests {

    static ServerPlayer fighter(GameTestHelper helper, BlockPos at, ItemStack weapon) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "sn-test-fighter"));
        p.setGameMode(GameType.SURVIVAL);
        Vec3 v = helper.absoluteVec(at.getBottomCenter());
        p.moveTo(v.x, v.y, v.z, 0, 0);
        p.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        return p;
    }

    /** FakePlayers are never ticked, so their swing never recharges on its own. */
    static void fullyCharged(ServerPlayer p) {
        try {
            Field f = net.minecraft.world.entity.LivingEntity.class.getDeclaredField("attackStrengthTicker");
            f.setAccessible(true);
            f.setInt(p, 1000);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static Zombie dummy(GameTestHelper helper, BlockPos at, float yaw) {
        Zombie z = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, at);
        z.setYRot(yaw);
        z.setYHeadRot(yaw);
        z.setYBodyRot(yaw);
        return z;
    }

    @GameTest(template = SNGameTests.MEDIUM)
    public static void macheteBackstabHitsHarderThanAFrontalBlow(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        // Both zombies face south (+z); the fighter stands north of one (behind it) and south of the other (in front).
        Zombie back = dummy(helper, new BlockPos(3, 1, 4), 0);
        Zombie front = dummy(helper, new BlockPos(7, 1, 6), 0);
        ServerPlayer p = fighter(helper, new BlockPos(3, 1, 3), new ItemStack(AllItems.SILVER_MACHETE.get()));
        fullyCharged(p);
        p.attack(back);
        float backDealt = back.getMaxHealth() - back.getHealth();
        Vec3 v = helper.absoluteVec(new BlockPos(7, 1, 7).getBottomCenter());
        p.moveTo(v.x, v.y, v.z, 180, 0);
        fullyCharged(p);
        p.attack(front);
        float frontDealt = front.getMaxHealth() - front.getHealth();
        helper.assertTrue(frontDealt > 0, "the frontal blow did no damage");
        helper.assertTrue(backDealt >= frontDealt * 1.8f, "backstab dealt " + backDealt + " vs frontal " + frontDealt);
        p.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM)
    public static void maceSmashSendsAHolyShockwave(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        Zombie struck = dummy(helper, new BlockPos(5, 1, 5), 0);
        Zombie near = dummy(helper, new BlockPos(7, 1, 5), 0);
        Zombie far = dummy(helper, new BlockPos(5, 1, 10), 0);
        ItemStack mace = new ItemStack(AllItems.EXORCISTS_MACE.get());
        ServerPlayer p = fighter(helper, new BlockPos(5, 1, 4), mace);
        p.fallDistance = 2f;  // ring radius 3 + 2/2 = 4: "near" is 2 blocks away, "far" is 5
        helper.assertTrue(ExorcistMaceItem.shockwaveRadius(2f) == 4f, "test assumes a radius of 4");
        ((ExorcistMaceItem) mace.getItem()).hurtEnemy(mace, struck, p);
        helper.assertTrue(near.getHealth() < near.getMaxHealth(), "a zombie inside the ring was not hit");
        helper.assertTrue(far.getHealth() == far.getMaxHealth(), "a zombie outside the ring was hit");
        p.discard();
        helper.succeed();
    }

    // Its own batch: a flame trail from a neighbouring test can drift onto the target and add a point of fire.
    @GameTest(template = SNGameTests.MEDIUM, batch = "arsenal_dash", timeoutTicks = 40)
    public static void angelBladeDashStrikesEachEnemyOnce(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        LivingEntity z = dummy(helper, new BlockPos(5, 1, 6), 0);
        ServerPlayer p = fighter(helper, new BlockPos(5, 1, 5), new ItemStack(AllItems.ANGEL_BLADE.get()));
        AngelBladeItem.dash(p, 1f);
        helper.runAfterDelay(20, () -> {
            float dealt = z.getMaxHealth() - z.getHealth();
            helper.assertTrue(Math.abs(dealt - AngelBladeItem.dashDamage(1f)) < 0.01f,
                    "dash should strike once for " + AngelBladeItem.dashDamage(1f) + ", dealt " + dealt);
            p.discard();
            helper.succeed();
        });
    }

    // --- catalysts ---------------------------------------------------------------------

    private static org.papiricoh.supernaturalcraft.magic.spell.Spell boltSmite() {
        return new org.papiricoh.supernaturalcraft.magic.spell.Spell(java.util.Optional.of(SupernaturalCraft.asResource("bolt")),
                java.util.List.of(SupernaturalCraft.asResource("smite")), java.util.List.of(), "");
    }

    @GameTest(template = SNGameTests.MEDIUM)
    public static void enochianOrbCheapensAndSplitsBolts(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer p = fighter(helper, new BlockPos(5, 1, 5), new ItemStack(AllItems.GRIMOIRE.get()));
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(AllItems.ENOCHIAN_ORB.get()));
        var arcana = org.papiricoh.supernaturalcraft.magic.mana.ManaManager.get(p);
        arcana.setMana(100);
        var result = org.papiricoh.supernaturalcraft.magic.spell.SpellCaster.cast(p, boltSmite(), 1f);
        helper.assertTrue(result.success(), "cast failed: " + result);
        float expected = 100 - (10 + 12) * org.papiricoh.supernaturalcraft.weapon.catalyst.EnochianOrbItem.MANA;
        helper.assertTrue(Math.abs(arcana.mana() - expected) < 0.01f, "expected " + expected + " mana left, have " + arcana.mana());
        int bolts = helper.getLevel().getEntitiesOfClass(org.papiricoh.supernaturalcraft.entity.magic.SigilBolt.class,
                p.getBoundingBox().inflate(4)).size();
        helper.assertTrue(bolts == org.papiricoh.supernaturalcraft.weapon.catalyst.EnochianOrbItem.SPLIT, "expected 3 bolts, saw " + bolts);
        p.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM)
    public static void withoutACatalystABoltStaysSingle(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer p = fighter(helper, new BlockPos(5, 1, 5), new ItemStack(AllItems.GRIMOIRE.get()));
        org.papiricoh.supernaturalcraft.magic.mana.ManaManager.get(p).setMana(100);
        org.papiricoh.supernaturalcraft.magic.spell.SpellCaster.cast(p, boltSmite(), 1f);
        int bolts = helper.getLevel().getEntitiesOfClass(org.papiricoh.supernaturalcraft.entity.magic.SigilBolt.class,
                p.getBoundingBox().inflate(4)).size();
        helper.assertTrue(bolts == 1, "expected 1 bolt, saw " + bolts);
        p.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM)
    public static void emberStaffStepsAsideForAGrimoire(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ItemStack staff = new ItemStack(AllItems.EMBER_STAFF.get());
        ServerPlayer p = fighter(helper, new BlockPos(5, 1, 5), staff);
        org.papiricoh.supernaturalcraft.magic.mana.ManaManager.get(p).setMana(100);
        var alone = staff.getItem().use(helper.getLevel(), p, InteractionHand.MAIN_HAND);
        helper.assertTrue(alone.getResult().consumesAction(), "the staff alone should cast its fireball");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(org.papiricoh.supernaturalcraft.entity.projectile.HellfireBolt.class,
                p.getBoundingBox().inflate(4)).isEmpty(), "no fireball");
        p.getCooldowns().removeCooldown(staff.getItem());
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(AllItems.GRIMOIRE.get()));
        var withBook = staff.getItem().use(helper.getLevel(), p, InteractionHand.MAIN_HAND);
        helper.assertTrue(withBook.getResult() == net.minecraft.world.InteractionResult.PASS, "with a grimoire the staff must step aside");
        p.discard();
        helper.succeed();
    }

    // --- tier III ----------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, timeoutTicks = 60)
    public static void soulCrescentCutsThroughALine(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        Zombie a = dummy(helper, new BlockPos(5, 1, 4), 0), b = dummy(helper, new BlockPos(5, 1, 6), 0), c = dummy(helper, new BlockPos(5, 1, 8), 0);
        ServerPlayer p = fighter(helper, new BlockPos(5, 1, 1), new ItemStack(AllItems.SOUL_SCYTHE.get()));
        p.setXRot(10);
        org.papiricoh.supernaturalcraft.weapon.melee.SoulScytheItem.throwCrescent(p);
        helper.runAfterDelay(30, () -> {
            for (Zombie z : java.util.List.of(a, b, c)) {
                helper.assertTrue(z.getHealth() < z.getMaxHealth(), "the crescent missed a zombie at " + z.blockPosition());
            }
            p.discard();
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.MEDIUM, timeoutTicks = 80)
    public static void greatswordLeavesBurningGround(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        Zombie near = dummy(helper, new BlockPos(5, 1, 4), 0);
        Zombie onTrail = dummy(helper, new BlockPos(5, 1, 8), 0);  // 7 blocks out: past the cleave, on the trail
        ServerPlayer p = fighter(helper, new BlockPos(5, 1, 1), new ItemStack(AllItems.HELLFIRE_GREATSWORD.get()));
        onTrail.setNoAi(true);
        org.papiricoh.supernaturalcraft.weapon.melee.HellfireGreatswordItem.cleave(p);
        helper.assertTrue(near.getHealth() < near.getMaxHealth(), "the cleave missed a zombie in the cone");
        helper.assertTrue(onTrail.getHealth() == onTrail.getMaxHealth(), "the cleave reached too far");
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(onTrail.getHealth() < onTrail.getMaxHealth(), "the flame trail never burned the zombie standing in it");
            p.discard();
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.MEDIUM, timeoutTicks = 60)
    public static void censerBeamBurnsAndLights(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        Zombie z = dummy(helper, new BlockPos(5, 1, 6), 0);
        ItemStack censer = new ItemStack(AllItems.CENSER_OF_GRACE.get());
        ServerPlayer p = fighter(helper, new BlockPos(5, 1, 1), censer);
        p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, z.getEyePosition());
        org.papiricoh.supernaturalcraft.magic.mana.ManaManager.get(p).setMana(100);
        var item = (org.papiricoh.supernaturalcraft.weapon.catalyst.CenserOfGraceItem) censer.getItem();
        int duration = item.getUseDuration(censer, p);
        for (int t = 1; t <= 20; t++) item.onUseTick(helper.getLevel(), p, censer, duration - t);
        helper.assertTrue(z.getHealth() < z.getMaxHealth(), "the beam did not burn its target");
        helper.assertTrue(org.papiricoh.supernaturalcraft.magic.mana.ManaManager.get(p).mana() <= 80.01f, "the beam should cost a mana per tick");
        boolean lit = false;
        for (BlockPos pos : BlockPos.betweenClosed(z.blockPosition().offset(-1, -1, -2), z.blockPosition().offset(1, 2, 1))) {
            lit |= helper.getLevel().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.LIGHT);
        }
        helper.assertTrue(lit, "the beam did not light the ground it struck");
        p.discard();
        helper.succeed();
    }
}
