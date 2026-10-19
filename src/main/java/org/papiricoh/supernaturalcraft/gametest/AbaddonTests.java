package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.arena.PendingReturns;
import org.papiricoh.supernaturalcraft.bowl.BowlContents;
import org.papiricoh.supernaturalcraft.bowl.BowlSpellRecipe;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlCast;
import org.papiricoh.supernaturalcraft.bowl.spell.Taglock;
import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonBalance;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonEntity;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonHeraldEntity;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonKnightEntity;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonSpoils;
import org.papiricoh.supernaturalcraft.hunter.DevilsTrapBlock;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellCaster;
import org.papiricoh.supernaturalcraft.mark.MarkData;
import org.papiricoh.supernaturalcraft.mark.Marks;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.ritual.RitualConditions;
import org.papiricoh.supernaturalcraft.ritual.RitualRecipe;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Abaddon (v0.21 P5), each test in its own batch with the arena cleaned first. Her damage policy (a 35 % share of ordinary blows;
 * the First Blade, a Marked hunter or a devil's trap land in full); possession (her body untouchable, the knight's wounds hers)
 * and every way she is forced out (the Exorcise sigil, Banish by Name with her taglock or the knight's, Salt Shot, the knight on
 * a devil's trap, the knight's death) into a 100-tick stun at double harm; her own exit after 320 ticks; the trap that holds her
 * 120 ticks before she breaks it and shrugs traps off for 600; the Brand's gaps; the telekinetic pin; the herald's mark that
 * follows, holds and strikes; the knight's guard; the Book for every participant and the cleaver and bust only the first time;
 * her rite.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class AbaddonTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    private static AbaddonEntity spawn(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        return helper.spawn(AllEntities.ABADDON.get(), MID);
    }

    private static void inHerHall(GameTestHelper helper, AbaddonEntity a) {
        helper.assertTrue(a.arena() != null && a.arena().theme() == ArenaTheme.KNIGHTS, "her hall opens round her (KNIGHTS)");
    }

    private static Mob dummy(GameTestHelper helper, Vec3 rel) {
        Mob m = helper.spawnWithNoFreeWill(EntityType.PIG, BlockPos.containing(rel));
        m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        m.setHealth(1000);
        Vec3 v = helper.absoluteVec(rel);
        m.moveTo(v.x, v.y, v.z, 0, 0);
        return m;
    }

    private static boolean struck(Mob m) {
        return m.getHealth() < 1000f;
    }

    private static ServerPlayer hunter(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = CurseTests.mortal(helper, at, ItemStack.EMPTY);
        p.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN);
        p.setData(AllAttachments.MARK, MarkData.NONE);
        return p;
    }

    /** What one blow of 20 from {@code p} takes off her (vanilla health), from 900. */
    private static float blow(AbaddonEntity a, ServerPlayer p) {
        a.invulnerableTime = 0;
        BossHealthGuard.set(a, 900);
        a.hurt(a.damageSources().playerAttack(p), 20);
        return 900 - a.getHealth();
    }

    /** She rides a knight of her own: her retinue called, the first knight (held still) possessed. */
    private static AbaddonKnightEntity ridden(GameTestHelper helper, AbaddonEntity a) {
        int n = AbaddonAttacks.summonRetinue(a, 2, true);
        helper.assertTrue(n == 3 && a.retinue(true, false).size() == 2 && a.retinue(false, true).size() == 1, "two knights and a herald: " + n);
        AbaddonKnightEntity k = (AbaddonKnightEntity) a.retinue(true, false).getFirst();
        for (Entity e : a.retinue(true, true)) ((Mob) e).setNoAi(true);
        Vec3 v = helper.absoluteVec(new Vec3(MID.getX() + 4.5, 1, MID.getZ() + 0.5));
        k.moveTo(v.x, v.y, v.z, 90, 0);
        // The rest of her retinue rose at random round her: stand them off to the west, out of the line a caster aims down
        // at the ridden knight (a touch sigil that met another knight first made this test flaky).
        int i = 0;
        for (Entity e : a.retinue(true, true)) {
            if (e == k) continue;
            Vec3 off = helper.absoluteVec(new Vec3(MID.getX() - 6.5, 1, MID.getZ() - 3.5 + 7 * i++));
            e.moveTo(off.x, off.y, off.z, 0, 0);
        }
        a.beginPossession(k);
        helper.assertTrue(a.smoke() && k.possessed() && k.getUUID().equals(a.possessing()), "she rides the knight");
        return k;
    }

    private static void castOut(GameTestHelper helper, AbaddonEntity a, AbaddonKnightEntity k, String how) {
        helper.assertTrue(a.possessing() == null && !a.smoke() && !k.possessed(), how + ": she is forced out of the knight");
        helper.assertTrue(a.stunned(), how + ": and stunned");
    }

    // --- her damage policy -------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "abaddon_share", timeoutTicks = 60)
    public static void sheTakesAThirdOfABlowButAllOfTheBladesTheMarkedsAndTheTrappeds(GameTestHelper helper) {
        AbaddonEntity a = spawn(helper);
        helper.runAfterDelay(3, () -> {
            inHerHall(helper, a);
            a.setNoAi(true);
            ServerPlayer p = hunter(helper, new BlockPos(20, 1, 20));
            float plain = blow(a, p);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AllItems.FIRST_BLADE.get()));
            float blade = blow(a, p);
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            Marks.grant(p, null, false);
            float marked = blow(a, p);
            Marks.remove(p);
            float again = blow(a, p);
            helper.assertTrue(plain > 0 && Math.abs(again - plain) < 0.01f, "the same blow twice: " + plain + " / " + again);
            helper.assertTrue(blade / plain >= 1 / AbaddonBalance.RESIST - 0.05f, "the First Blade lands in full: " + blade + " vs " + plain);
            helper.assertTrue(Math.abs(marked / plain - 1 / AbaddonBalance.RESIST) < 0.1f, "a Marked hunter's blow lands in full: " + marked + " vs " + plain);
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    /** A devil's trap holds her 120 ticks (every blow in full), then she breaks it, and for 600 ticks no trap holds her. */
    @GameTest(template = SNGameTests.ARENA, batch = "abaddon_trap", timeoutTicks = 200)
    public static void aDevilsTrapHoldsHerThenSheBreaksItAndShrugsTrapsOff(GameTestHelper helper) {
        AbaddonEntity a = spawn(helper);
        BlockPos[] trap = new BlockPos[2];
        float[] loss = new float[2];
        ServerPlayer[] p = new ServerPlayer[1];
        helper.runAfterDelay(3, () -> {
            inHerHall(helper, a);
            p[0] = hunter(helper, new BlockPos(20, 1, 20));
            loss[0] = blow(a, p[0]);
            trap[0] = a.blockPosition();
            helper.getLevel().setBlock(trap[0], trapState(), 2);
        });
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(a.trapped(), "the trap holds her");
            loss[1] = blow(a, p[0]);
            helper.assertTrue(Math.abs(loss[1] / loss[0] - 1 / AbaddonBalance.RESIST) < 0.1f, "trapped, every blow lands in full: " + loss[1] + " vs " + loss[0]);
        });
        helper.runAfterDelay(4 + AbaddonBalance.TRAPPED_TICKS - 10, () -> helper.assertTrue(a.trapped(), "still held near the end"));
        helper.runAfterDelay(4 + AbaddonBalance.TRAPPED_TICKS + 8, () -> {
            helper.assertFalse(a.trapped(), "she breaks free after " + AbaddonBalance.TRAPPED_TICKS + " ticks");
            helper.assertFalse(helper.getLevel().getBlockState(trap[0]).getBlock() instanceof DevilsTrapBlock, "and the trap with it");
            trap[1] = a.blockPosition();
            helper.getLevel().setBlock(trap[1], trapState(), 2);
        });
        helper.runAfterDelay(4 + AbaddonBalance.TRAPPED_TICKS + 20, () -> {
            helper.assertFalse(a.trapped(), "a fresh trap does not hold her for a while");
            helper.getLevel().removeBlock(trap[1], false);
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    private static net.minecraft.world.level.block.state.BlockState trapState() {
        return AllBlocks.DEVILS_TRAP.get().defaultBlockState().setValue(DevilsTrapBlock.PART, DevilsTrapBlock.CENTER);
    }

    // --- possession --------------------------------------------------------------------------------------------------------

    /** Ridden, her body cannot be struck and her knight's wounds are hers; salt drives her out into a 100-tick stun at x2. */
    @GameTest(template = SNGameTests.ARENA, batch = "abaddon_possess", timeoutTicks = 200)
    public static void herKnightsWoundsAreHersAndSaltDrivesHerOutIntoAStun(GameTestHelper helper) {
        AbaddonEntity a = spawn(helper);
        AbaddonKnightEntity[] k = new AbaddonKnightEntity[1];
        ServerPlayer[] p = new ServerPlayer[1];
        float[] loss = new float[2];
        helper.runAfterDelay(3, () -> {
            inHerHall(helper, a);
            k[0] = ridden(helper, a);
            p[0] = hunter(helper, new BlockPos(20, 1, 20));
            BossHealthGuard.set(a, 900);
            helper.assertFalse(a.hurt(a.damageSources().playerAttack(p[0]), 20), "her smoke body cannot be struck");
            float kh = k[0].getHealth();
            k[0].hurt(a.damageSources().playerAttack(p[0]), 20);
            helper.assertTrue(a.getHealth() < 900 && k[0].getHealth() == kh, "the knight's wounds are hers: " + a.getHealth() + ", knight " + k[0].getHealth());
            // Salt Shot at the knight she rides.
            ServerPlayer caster = caster(helper, MID.offset(2, 0, 0), k[0]);
            caster.getInventory().add(new ItemStack(AllItems.SALT.get(), 4));
            SpellCaster.Result r = SpellCaster.cast(caster, spell("touch", "salt_shot"), 1f);
            helper.assertTrue(r.success(), "salt shot: " + r);
            castOut(helper, a, k[0], "salt");
            loss[0] = blow(a, p[0]);
        });
        helper.runAfterDelay(3 + AbaddonBalance.STUN_TICKS - 15, () -> helper.assertTrue(a.stunned(), "stunned for " + AbaddonBalance.STUN_TICKS + " ticks"));
        helper.runAfterDelay(3 + AbaddonBalance.STUN_TICKS + 8, () -> {
            helper.assertFalse(a.stunned(), "the stun wears off");
            loss[1] = blow(a, p[0]);
            helper.assertTrue(Math.abs(loss[0] / loss[1] - AbaddonBalance.STUN_VULNERABILITY) < 0.1f, "stunned she takes double: " + loss[0] + " vs " + loss[1]);
            BossTests.cleanup(helper);
            helper.assertTrue(k[0].isRemoved(), "her knights go with her");
            helper.succeed();
        });
    }

    /** Left alone she leaves the knight herself after 320 ticks, unstunned. */
    @GameTest(template = SNGameTests.ARENA, batch = "abaddon_possess_end", timeoutTicks = 400)
    public static void sheLeavesHerKnightByHerselfAfterAWhile(GameTestHelper helper) {
        AbaddonEntity a = spawn(helper);
        AbaddonKnightEntity[] k = new AbaddonKnightEntity[1];
        helper.runAfterDelay(3, () -> {
            inHerHall(helper, a);
            k[0] = ridden(helper, a);
        });
        helper.runAfterDelay(3 + AbaddonBalance.POSSESS_TICKS - 20, () -> helper.assertTrue(a.smoke() && k[0].possessed(), "still riding"));
        helper.runAfterDelay(3 + AbaddonBalance.POSSESS_TICKS + 10, () -> {
            helper.assertTrue(a.possessing() == null && !k[0].possessed() && !a.stunned(), "she leaves by herself, unstunned");
            helper.assertFalse(a.mayPossess(), "and does not ride again at once");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "abaddon_expel_exorcise", timeoutTicks = 60)
    public static void theExorciseSigilForcesHerOut(GameTestHelper helper) {
        expel(helper, "exorcise", (a, k) -> {
            ServerPlayer caster = caster(helper, MID.offset(2, 0, 0), k);
            caster.getInventory().add(new ItemStack(AllItems.SALT.get(), 4));
            SpellCaster.Result r = SpellCaster.cast(caster, spell("touch", "exorcise"), 1f);
            helper.assertTrue(r.success(), "exorcise: " + r);
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "abaddon_expel_name", timeoutTicks = 60)
    public static void banishByNameWithHerTaglockForcesHerOut(GameTestHelper helper) {
        expel(helper, "banish (hers)", (a, k) -> banish(helper, a));
    }

    @GameTest(template = SNGameTests.ARENA, batch = "abaddon_expel_name_knight", timeoutTicks = 60)
    public static void banishByNameWithTheKnightsTaglockForcesHerOut(GameTestHelper helper) {
        expel(helper, "banish (the knight's)", (a, k) -> banish(helper, k));
    }

    @GameTest(template = SNGameTests.ARENA, batch = "abaddon_expel_trap", timeoutTicks = 60)
    public static void herKnightOnADevilsTrapForcesHerOut(GameTestHelper helper) {
        expel(helper, "the trap", (a, k) -> helper.getLevel().setBlock(k.blockPosition(), trapState(), 2));
    }

    @GameTest(template = SNGameTests.ARENA, batch = "abaddon_expel_death", timeoutTicks = 60)
    public static void herKnightsDeathForcesHerOut(GameTestHelper helper) {
        expel(helper, "the knight's death", (a, k) -> k.kill());
    }

    /** {@code how} is done to her ridden knight at tick 5; by tick 8 she is out of it and stunned. */
    private static void expel(GameTestHelper helper, String what, java.util.function.BiConsumer<AbaddonEntity, AbaddonKnightEntity> how) {
        AbaddonEntity a = spawn(helper);
        AbaddonKnightEntity[] k = new AbaddonKnightEntity[1];
        helper.runAfterDelay(3, () -> {
            inHerHall(helper, a);
            k[0] = ridden(helper, a);
        });
        helper.runAfterDelay(5, () -> how.accept(a, k[0]));
        helper.runAfterDelay(8, () -> {
            castOut(helper, a, k[0], what);
            BlockPos at = k[0].blockPosition();
            if (helper.getLevel().getBlockState(at).getBlock() instanceof DevilsTrapBlock) helper.getLevel().removeBlock(at, false);
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    private static void banish(GameTestHelper helper, Entity named) {
        ServerLevel level = helper.getLevel();
        ServerPlayer caster = caster(helper, MID.offset(-3, 0, 0), named);
        BowlSpellRecipe recipe = level.getRecipeManager().byKey(SupernaturalCraft.asResource("bowl_spell/banish_by_name"))
                .map(h -> (BowlSpellRecipe) h.value()).orElseThrow();
        ItemStack lock = new ItemStack(AllItems.TAGLOCK.get());
        lock.set(AllDataComponents.TAGLOCK.get(), new Taglock(named.getUUID(), "Abaddon", Taglock.HAIR));
        BowlCast cast = new BowlCast(level, helper.absolutePos(MID.offset(-3, 0, 2)), caster, BowlContents.of(List.of(lock), List.of()), recipe);
        helper.assertTrue(recipe.effect().perform(cast), "the banishing takes");
    }

    private static Spell spell(String form, String... effects) {
        return new Spell(Optional.of(SupernaturalCraft.asResource(form)),
                java.util.Arrays.stream(effects).map(SupernaturalCraft::asResource).toList(), List.of(), "");
    }

    private static ServerPlayer caster(GameTestHelper helper, BlockPos at, Entity facing) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "sn-test-exorcist"));
        p.setGameMode(GameType.SURVIVAL);
        p.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN);
        Vec3 v = helper.absoluteVec(Vec3.atBottomCenterOf(at));
        p.moveTo(v.x, v.y, v.z);
        p.lookAt(EntityAnchorArgument.Anchor.EYES, facing.getEyePosition());
        ManaManager.get(p).setMana(100);
        return p;
    }

    // --- her retinue -------------------------------------------------------------------------------------------------------

    /** A knight's raised shield turns blows from the front, not from behind. */
    @GameTest(template = SNGameTests.ARENA, batch = "abaddon_guard", timeoutTicks = 60)
    public static void aKnightsShieldTurnsBlowsFromTheFront(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        AbaddonKnightEntity k = helper.spawn(AllEntities.ABADDON_KNIGHT.get(), MID);
        k.setNoAi(true);
        Vec3 v = helper.absoluteVec(Vec3.atBottomCenterOf(MID));
        k.moveTo(v.x, v.y, v.z, 0, 0);
        Zombie front = ArsenalTests.dummy(helper, MID.offset(0, 0, 2), 180), back = ArsenalTests.dummy(helper, MID.offset(0, 0, -2), 0);
        helper.runAfterDelay(2, k::startGuard);
        helper.runAfterDelay(9, () -> {
            helper.assertTrue(k.guarding(), "his shield is up");
            float h = k.getHealth();
            helper.assertFalse(k.hurt(k.damageSources().mobAttack(front), 6), "a blow from the front glances off");
            helper.assertTrue(k.getHealth() == h, "untouched");
            helper.assertTrue(k.hurt(k.damageSources().mobAttack(back), 6) && k.getHealth() < h, "a blow from behind lands");
            k.discard();
            front.discard();
            back.discard();
            helper.succeed();
        });
    }

    /** Her herald's mark follows its target, holds still, then strikes where it held. */
    @GameTest(template = SNGameTests.ARENA, batch = "abaddon_herald", timeoutTicks = 80)
    public static void theHeraldsMarkFollowsHoldsAndStrikes(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        AbaddonHeraldEntity h = helper.spawn(AllEntities.ABADDON_HERALD.get(), MID);
        h.setNoAi(true);
        Mob target = dummy(helper, new Vec3(30.5, 1, 24.5));
        Mob[] stand = new Mob[1];
        helper.runAfterDelay(2, () -> h.startMark(target));
        helper.runAfterDelay(12, () -> target.teleportTo(target.getX(), target.getY(), target.getZ() + 6));
        helper.runAfterDelay(2 + AbaddonBalance.MARK_FOLLOW + 4, () -> {
            // Past the follow: the mark holds where the target stood; the target runs and someone else steps in.
            Vec3 held = target.position();
            target.teleportTo(held.x - 8, held.y, held.z);
            stand[0] = dummy(helper, helper.relativeVec(held));
        });
        helper.runAfterDelay(2 + AbaddonBalance.MARK_FOLLOW + AbaddonBalance.MARK_HOLD + 6, () -> {
            helper.assertTrue(struck(stand[0]), "the strike lands where the mark held (it followed the target)");
            helper.assertFalse(struck(target), "the target got out in time");
            h.discard();
            helper.succeed();
        });
    }

    // --- her attacks -------------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "abaddon_brand", timeoutTicks = 60)
    public static void theBrandBurnsItsRingsButSparesTheGaps(GameTestHelper helper) {
        AbaddonEntity a = spawn(helper);
        helper.runAfterDelay(3, () -> {
            inHerHall(helper, a);
            a.setNoAi(true);
            Vec3 c = helper.relativeVec(a.position());
            Mob target = dummy(helper, c.add(0, 0, 2));
            AbaddonAttacks.Brand b = new AbaddonAttacks.Brand();
            b.onWindup(a, target);
            helper.assertTrue(b.gaps().size() == 3 && b.gaps().stream().allMatch(g -> g.size() == AbaddonBalance.BRAND_GAPS), "three rings with their gaps");
            float r = AbaddonBalance.BRAND_RINGS[1];
            Vec3 gap = helper.relativeVec(b.gaps().get(1).getFirst());
            Vec3 dir = gap.subtract(c).multiply(1, 0, 1).normalize();
            Mob inGap = dummy(helper, new Vec3(gap.x, c.y, gap.z));
            Mob onRing = dummy(helper, c.add(new Vec3(dir.z, 0, -dir.x).scale(r)));
            Mob between = dummy(helper, c.add(dir.scale((AbaddonBalance.BRAND_RINGS[0] + AbaddonBalance.BRAND_RINGS[1]) / 2)));
            b.onActive(a, target);
            helper.assertFalse(struck(inGap), "the green gap is safe");
            helper.assertFalse(struck(between), "between the rings is safe");
            helper.assertTrue(struck(onRing), "the ring burns away from its gaps");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "abaddon_pin", timeoutTicks = 60)
    public static void theTelekineticPinHoldsWhoeverStaysInItsCircle(GameTestHelper helper) {
        AbaddonEntity a = spawn(helper);
        helper.runAfterDelay(3, () -> {
            inHerHall(helper, a);
            a.setNoAi(true);
            Mob target = dummy(helper, new Vec3(30.5, 1, 24.5)), clear = dummy(helper, new Vec3(18.5, 1, 30.5));
            AbaddonAttacks.TelekineticPin pin = new AbaddonAttacks.TelekineticPin();
            pin.onWindup(a, target);
            pin.onActive(a, target);
            helper.assertTrue(pin.pinned().contains(target) && !pin.pinned().contains(clear), "the one in the circle is held, no one else");
            helper.assertTrue(struck(target) && target.getEffect(MobEffects.MOVEMENT_SLOWDOWN) != null
                    && target.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration() == AbaddonBalance.PIN_TICKS, "held " + AbaddonBalance.PIN_TICKS + " ticks");
            Vec3 held = target.position();
            target.teleportTo(held.x + 4, held.y, held.z);
            pin.tickActive(a, target, 5);
            helper.assertTrue(target.position().distanceTo(held) < 0.3, "and cannot step out");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    // --- her spoils and her rite -------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "abaddon_spoils", timeoutTicks = 60)
    public static void theBookGoesToEveryoneTheCleaverOnlyTheFirstTime(GameTestHelper helper) {
        AbaddonEntity a = spawn(helper);
        helper.runAfterDelay(3, () -> {
            inHerHall(helper, a);
            a.setNoAi(true);
            ServerLevel level = helper.getLevel();
            PendingReturns owed = PendingReturns.get(level.getServer());
            ServerPlayer first = CovenTests.fake(helper, "sn-test-abaddon-first", MID.offset(3, 0, 0));
            ServerPlayer veteran = CovenTests.fake(helper, "sn-test-abaddon-veteran", MID.offset(-3, 0, 0));
            HellTests.award(veteran, AbaddonSpoils.ADVANCEMENT);
            a.arena().join(first);
            a.arena().join(veteran);
            Vec3 at = a.position().add(0, 1.2, 0);
            AbaddonSpoils.drop(level, a, List.of(first, veteran), at);
            List<ItemStack> f = owed.peek(first.getUUID()), v = owed.peek(veteran.getUUID());
            boolean books = f.stream().anyMatch(s -> s.is(AllItems.BOOK_OF_THE_DAMNED.get())) && v.stream().anyMatch(s -> s.is(AllItems.BOOK_OF_THE_DAMNED.get()));
            List<ItemEntity> ground = level.getEntitiesOfClass(ItemEntity.class, new AABB(BlockPos.containing(at)).inflate(3));
            int cleavers = count(ground, AllItems.KNIGHTS_CLEAVER.get()), busts = count(ground, AllItems.ABADDON_TROPHY.get()),
                    loose = count(ground, AllItems.BOOK_OF_THE_DAMNED.get());
            ground.forEach(Entity::discard);
            owed.take(first.getUUID());
            owed.take(veteran.getUUID());
            helper.assertTrue(books, "a Book for every participant (owed while they are away)");
            helper.assertTrue(loose == 0, "no Book left on the floor: " + loose);
            helper.assertTrue(cleavers == 1 && busts == 2, "the cleaver for the first win only, a bust each: " + cleavers + " cleavers, " + busts + " busts");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    private static int count(List<ItemEntity> items, net.minecraft.world.item.Item item) {
        return items.stream().filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "abaddon_rite", timeoutTicks = 60)
    public static void herRiteCallsHerIntoHerHall(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        RitualRecipe r = (RitualRecipe) level.getRecipeManager().byKey(SupernaturalCraft.asResource("ritual/summon_abaddon")).orElseThrow().value();
        helper.assertTrue(r.conditions().time() == RitualConditions.Time.NIGHT, "by night");
        helper.assertTrue(r.conditions().dimension().filter(net.minecraft.world.level.Level.OVERWORLD::equals).isPresent(), "in the Overworld");
        helper.assertTrue(r.conditions().requiresAdvancement().stream().map(net.minecraft.resources.ResourceLocation::getPath).toList()
                .containsAll(List.of("main/first_born", "main/war", "main/famine", "main/pestilence")), "after Cain and three Horsemen");
        helper.assertTrue(r.activator().test(new ItemStack(AllItems.FIRST_BLADE.get())) && !r.consumeActivator(), "the First Blade wakes it, and is kept");
        helper.assertTrue(r.ingredients().size() == 8 && r.manaCost() == 120f, "eight offerings, 120 mana");
        ServerPlayer p = CovenTests.fake(helper, "sn-test-abaddon-rite", new BlockPos(2, 1, 2));
        helper.assertTrue("message.supernaturalcraft.ritual.not_ready".equals(r.conditions().check(level, p)), "not before Cain and the Horsemen");
        BlockPos altar = helper.absolutePos(MID);
        helper.assertTrue(new org.papiricoh.supernaturalcraft.ritual.effect.SummonAbaddonEffect().perform(level, altar, null), "she rises");
        helper.runAfterDelay(5, () -> {
            List<AbaddonEntity> found = level.getEntitiesOfClass(AbaddonEntity.class, new AABB(altar).inflate(24));
            helper.assertTrue(found.size() == 1, "one Abaddon: " + found.size());
            helper.assertTrue(found.getFirst().arena() != null && found.getFirst().arena().theme() == ArenaTheme.KNIGHTS, "in her hall");
            helper.assertFalse(new org.papiricoh.supernaturalcraft.ritual.effect.SummonAbaddonEffect().perform(level, altar.offset(3, 0, 0), null),
                    "a second rite finds the world busy");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }
}
