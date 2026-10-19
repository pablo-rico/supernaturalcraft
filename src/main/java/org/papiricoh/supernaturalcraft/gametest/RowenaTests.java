package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.bowl.BowlContents;
import org.papiricoh.supernaturalcraft.bowl.BowlLiquid;
import org.papiricoh.supernaturalcraft.bowl.BowlSpellRecipe;
import org.papiricoh.supernaturalcraft.bowl.Dose;
import org.papiricoh.supernaturalcraft.bowl.SpellBowlBlockEntity;
import org.papiricoh.supernaturalcraft.bowl.spell.Taglock;
import org.papiricoh.supernaturalcraft.bowl.spell.TaglockItem;
import org.papiricoh.supernaturalcraft.coven.ending.GrandWitchNpcEntity;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaNpcEntity;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaRelation;
import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.GrimoireEcho;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RecitationBowlEntity;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaBalance;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaEntity;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaSpoils;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenasGrimoireItem;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.entity.witch.FamiliarEntity;
import org.papiricoh.supernaturalcraft.entity.witch.HexedVillagerEntity;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

import java.util.List;

/**
 * Rowena as a boss (v0.21 P6), each test in its own batch with the arena cleaned first: her house's arena; every P1 spell shown
 * on the ground and landing only where it was shown (fling, Ignis, Glacies) and her yellow smoke blink; her raven's stolen lock
 * and the curse it carries every 120 ticks until the raven dies; her hexed victims (killed, they heal her 4 %; freed, they cost
 * her 3 %; never past where the phase began); the recitation (bowls only a hunter can break, three blows each; all broken and
 * she falls stunned for 100 ticks at ×1.5; otherwise twelve per bowl still whole to everyone in the arena); her spoils and the
 * Grand Witch for the Book's keeper; her Grimoire's echo.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class RowenaTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    private static RowenaEntity spawn(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        return helper.spawn(AllEntities.ROWENA.get(), MID);
    }

    /** She stands still in her house's arena (AI off once it is open). */
    private static void still(GameTestHelper helper, RowenaEntity r) {
        helper.assertTrue(r.arena() != null && r.arena().theme() == ArenaTheme.ROWENA_HOUSE, "her house's arena opens round her");
        r.setNoAi(true);
        Vec3 v = helper.absoluteVec(Vec3.atBottomCenterOf(MID));
        r.moveTo(v.x, v.y, v.z, 0, 0);
    }

    static Mob dummy(GameTestHelper helper, Vec3 rel) {
        Mob m = helper.spawnWithNoFreeWill(EntityType.PIG, BlockPos.containing(rel));
        m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        m.setHealth(1000);
        Vec3 v = helper.absoluteVec(rel);
        m.moveTo(v.x, v.y, v.z, 0, 0);
        return m;
    }

    static float loss(LivingEntity m) {
        return 1000f - m.getHealth();
    }

    /** One of her blows of {@code base} as it lands on a dummy (her multiplier; the divine share is part of it). */
    private static boolean landed(LivingEntity m, RowenaEntity r, float base) {
        float want = base * r.attackDamageMultiplier();
        return Math.abs(loss(m) - want) <= want * 0.1f + 0.05f;
    }

    private static Vec3 c() {
        return Vec3.atBottomCenterOf(MID);
    }

    // --- her house -----------------------------------------------------------------------------------------------------

    /** Called, she opens her house as the arena (radius 12) and her host self leaves; a second call finds the world busy. */
    @GameTest(template = SNGameTests.ARENA, batch = "rowena_summon", timeoutTicks = 60)
    public static void sheIsCalledIntoHerHouseAndHerHostSelfLeaves(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        RowenaNpcEntity host = helper.spawn(AllEntities.ROWENA_NPC.get(), MID.offset(3, 0, 3));
        BlockPos centre = helper.absolutePos(MID);
        RowenaEntity r = RowenaSummoning.summon(level, centre, null);
        helper.assertTrue(r != null, "she comes");
        ArenaController arena = r.arena();
        helper.assertTrue(arena != null && arena.theme() == ArenaTheme.ROWENA_HOUSE && arena.radius() == 12, "her house, a ring of twelve");
        helper.assertTrue(host.isRemoved(), "her host self is not at home during the fight");
        helper.assertTrue(RowenaSummoning.summon(level, centre.offset(4, 0, 0), null) == null, "a second call finds the world busy");
        ServerPlayer giver = CovenTests.fake(helper, "sn-test-rowena-giver", MID.offset(2, 0, 0));
        giver.setData(org.papiricoh.supernaturalcraft.registry.AllAttachments.ROWENA, RowenaRelation.NONE.withBook(RowenaRelation.Book.GIVEN));
        helper.assertTrue(RowenaSummoning.check(level, giver) == RowenaSummoning.Refusal.NOT_KEPT, "only a keeper challenges her");
        helper.assertTrue(RowenaSummoning.challenge(level, giver) == null, "a giver's Book calls nobody");
        BossTests.cleanup(helper);
        helper.succeed();
    }

    /** A Book read by its keeper in Rowena's house (a stand-in house round this test): she comes by night, not by day. */
    private static RowenaEntity rowenaNear(ServerLevel level, BlockPos centre) {
        List<RowenaEntity> found = level.getEntitiesOfClass(RowenaEntity.class, new AABB(centre).inflate(16));
        return found.isEmpty() ? null : found.getFirst();
    }

    /**
     * The Book, read by its keeper in her house: by night she comes (not by day; not elsewhere); once she has fallen to them it
     * calls the Grand Witch there instead, and read while sneaking it calls Rowena out again.
     */
    @GameTest(template = SNGameTests.ARENA, batch = "rowena_challenge", timeoutTicks = 60)
    public static void theKeepersBookCallsHerOutInHerHouseByNight(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        org.papiricoh.supernaturalcraft.coven.rowena.RowenaSavedData data = org.papiricoh.supernaturalcraft.coven.rowena.RowenaSavedData.get(level);
        net.minecraft.nbt.CompoundTag before = data.snapshot();
        long time = level.getDayTime();
        BlockPos centre = helper.absolutePos(MID);
        BlockPos offset = org.papiricoh.supernaturalcraft.coven.rowena.RowenaWorld.arenaCentre(
                new org.papiricoh.supernaturalcraft.coven.rowena.RowenaSite.Site(BlockPos.ZERO, 0));
        data.setHouse(centre.subtract(offset), 0, true);
        ServerPlayer keeper = CovenTests.fake(helper, "sn-test-rowena-keeper", MID.offset(3, 0, 2));
        keeper.setData(org.papiricoh.supernaturalcraft.registry.AllAttachments.ROWENA, RowenaRelation.NONE.withBook(RowenaRelation.Book.KEPT));
        ItemStack book = new ItemStack(AllItems.BOOK_OF_THE_DAMNED.get());
        keeper.setItemInHand(InteractionHand.MAIN_HAND, book);
        level.setDayTime(6000);
        String[] fail = {null};
        helper.runAfterDelay(2, () -> {
            try {
                helper.assertTrue(RowenaSummoning.centre(level) != null && RowenaSummoning.centre(level).equals(centre), "her house's middle is here");
                helper.assertTrue(RowenaSummoning.check(level, keeper) == RowenaSummoning.Refusal.NOT_NIGHT, "by day: " + RowenaSummoning.check(level, keeper));
                book.use(level, keeper, InteractionHand.MAIN_HAND);
                helper.assertTrue(rowenaNear(level, centre) == null, "not by day");
            } catch (RuntimeException e) {
                fail[0] = e.getMessage();
            }
            level.setDayTime(18000);
        });
        helper.runAfterDelay(4, () -> {
            try {
                if (fail[0] != null) throw new net.minecraft.gametest.framework.GameTestAssertException(fail[0]);
                helper.assertTrue(RowenaSummoning.check(level, keeper) == null, "by night, in her house: " + RowenaSummoning.check(level, keeper));
                book.use(level, keeper, InteractionHand.MAIN_HAND);
                RowenaEntity r = rowenaNear(level, centre);
                helper.assertTrue(r != null && r.arena() != null && r.arena().theme() == ArenaTheme.ROWENA_HOUSE, "she comes, in her house");
                BossTests.cleanup(helper);
                // Beaten: the Book calls the Grand Witch instead; read while sneaking, Rowena again.
                HellTests.award(keeper, RowenaSpoils.ADVANCEMENT);
                book.use(level, keeper, InteractionHand.MAIN_HAND);
                List<GrandWitchNpcEntity> gw = level.getEntitiesOfClass(GrandWitchNpcEntity.class, new AABB(centre).inflate(16));
                helper.assertTrue(gw.size() == 1 && keeper.getUUID().equals(gw.getFirst().keeper()), "the Grand Witch comes to her champion");
                helper.assertTrue(rowenaNear(level, centre) == null, "not Rowena");
                gw.forEach(Entity::discard);
                keeper.setShiftKeyDown(true);
                book.use(level, keeper, InteractionHand.MAIN_HAND);
                helper.assertTrue(rowenaNear(level, centre) != null, "read while sneaking: a rematch");
                BossTests.cleanup(helper);
                keeper.setShiftKeyDown(false);
                Vec3 far = helper.absoluteVec(new Vec3(2, 1, 2));
                keeper.moveTo(far.x, far.y + 40, far.z + 40);
                helper.assertTrue(RowenaSummoning.check(level, keeper) == RowenaSummoning.Refusal.NOT_HERE, "away from her house");
                book.use(level, keeper, InteractionHand.MAIN_HAND);
                helper.assertTrue(level.getEntitiesOfClass(GrandWitchNpcEntity.class, keeper.getBoundingBox().inflate(16)).isEmpty()
                        && rowenaNear(level, centre) == null, "elsewhere it calls nobody");
            } finally {
                BossTests.cleanup(helper);
                data.restore(before);
                level.setDayTime(time);
            }
            helper.succeed();
        });
    }

    // --- P1, the Witch at Home ------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "rowena_fling", timeoutTicks = 60)
    public static void theFlingThrowsWhoeverIsInItsLaneAndNoOneElse(GameTestHelper helper) {
        RowenaEntity r = spawn(helper);
        helper.runAfterDelay(3, () -> {
            still(helper, r);
            Mob target = dummy(helper, c().add(0, 0, 6)), lane = dummy(helper, c().add(0.3, 0, 3)),
                    beside = dummy(helper, c().add(3, 0, 4)), behind = dummy(helper, c().add(0, 0, -3));
            RowenaAttacks.Fling f = new RowenaAttacks.Fling();
            f.onWindup(r, target);
            helper.assertTrue(loss(target) == 0 && loss(lane) == 0, "nothing lands in the windup");
            f.onActive(r, target);
            helper.assertTrue(landed(target, r, RowenaBalance.FLING_DAMAGE) && landed(lane, r, RowenaBalance.FLING_DAMAGE),
                    "the lane is struck: " + loss(target) + ", " + loss(lane));
            helper.assertTrue(lane.getDeltaMovement().z > 0.5, "and thrown down it: " + lane.getDeltaMovement());
            helper.assertTrue(loss(beside) == 0 && loss(behind) == 0, "beside it and behind her nobody is touched");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "rowena_ignis", timeoutTicks = 60)
    public static void ignisBurnsItsRedCircleAndNoFurther(GameTestHelper helper) {
        RowenaEntity r = spawn(helper);
        helper.runAfterDelay(3, () -> {
            still(helper, r);
            Mob target = dummy(helper, c().add(6, 0, 0)), near = dummy(helper, c().add(8, 0, 0)), clear = dummy(helper, c().add(10, 0, 0));
            RowenaAttacks.Ignis a = new RowenaAttacks.Ignis();
            a.onWindup(r, target);
            target.teleportTo(target.getX(), target.getY(), target.getZ() + 6);
            a.onActive(r, target);
            helper.assertTrue(landed(near, r, RowenaBalance.IGNIS_DAMAGE) && near.isOnFire(), "the circle where the hunter stood burns: " + loss(near));
            helper.assertTrue(loss(target) == 0, "the hunter who left it is spared");
            helper.assertTrue(loss(clear) == 0 && !clear.isOnFire(), "outside the circle nothing burns");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "rowena_glacies", timeoutTicks = 60)
    public static void glaciesFreezesItsBlueCircleAndNoFurther(GameTestHelper helper) {
        RowenaEntity r = spawn(helper);
        helper.runAfterDelay(3, () -> {
            still(helper, r);
            Mob target = dummy(helper, c().add(-6, 0, 0)), clear = dummy(helper, c().add(-10.5, 0, 0));
            RowenaAttacks.Glacies a = new RowenaAttacks.Glacies();
            a.onWindup(r, target);
            a.onActive(r, target);
            var slow = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            helper.assertTrue(landed(target, r, RowenaBalance.GLACIES_DAMAGE), "struck: " + loss(target));
            helper.assertTrue(slow != null && slow.getDuration() == RowenaBalance.GLACIES_SLOW_TICKS && target.getTicksFrozen() > 0,
                    "slowed " + RowenaBalance.GLACIES_SLOW_TICKS + " ticks and frozen");
            helper.assertTrue(loss(clear) == 0 && clear.getEffect(MobEffects.MOVEMENT_SLOWDOWN) == null, "outside the circle, nothing");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "rowena_blink", timeoutTicks = 60)
    public static void herSmokeBlinkLandsBehindTheHunterInItsYellowCircle(GameTestHelper helper) {
        RowenaEntity r = spawn(helper);
        helper.runAfterDelay(3, () -> {
            still(helper, r);
            Mob target = dummy(helper, c().add(6, 0, 0));
            Vec3 to = target.position().add(new Vec3(1, 0, 0).scale(2.5));
            Mob there = dummy(helper, helper.relativeVec(to));
            RowenaAttacks.SmokeBlink b = new RowenaAttacks.SmokeBlink();
            b.onWindup(r, target);
            helper.assertTrue(r.position().distanceTo(to) > 5, "nothing moves in the windup");
            b.onActive(r, target);
            helper.assertTrue(r.position().distanceTo(to) < 0.6, "she lands behind the hunter: " + r.position() + " vs " + to);
            helper.assertTrue(landed(there, r, RowenaBalance.SMOKE_DAMAGE) && there.hasEffect(MobEffects.BLINDNESS), "whoever stood in the circle is struck and blinded");
            helper.assertTrue(loss(target) == 0, "the hunter, two and a half blocks off, is not");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    // --- P2, the Familiar and the Hexed -------------------------------------------------------------------------------

    private static int cursed(LivingEntity e) {
        int d = 0;
        var agony = e.getEffect(AllMobEffects.AGONY);
        var weak = e.getEffect(MobEffects.WEAKNESS);
        if (agony != null) d = Math.max(d, agony.getDuration());
        if (weak != null) d = Math.max(d, weak.getDuration());
        return d;
    }

    /**
     * Her raven holds a hunter's lock: a curse down it every {@link RowenaBalance#HOMING_EVERY} ticks, wherever they stand; the
     * raven killed drops the lock and the curses stop.
     */
    @GameTest(template = SNGameTests.ARENA, batch = "rowena_familiar", timeoutTicks = 460)
    public static void herRavenStealsALockAndSheCursesDownItUntilTheRavenDies(GameTestHelper helper) {
        RowenaEntity r = spawn(helper);
        Mob[] hunter = new Mob[1];
        FamiliarEntity[] raven = new FamiliarEntity[1];
        int t0 = 5, every = RowenaBalance.HOMING_EVERY;
        helper.runAfterDelay(t0, () -> {
            helper.assertTrue(r.arena() != null, "her arena");
            r.forceLook(2);
            r.scheduler().delay(100_000);
            hunter[0] = dummy(helper, c().add(9, 0, 9));
            r.track(hunter[0]);
            helper.assertTrue(r.mayCallFamiliar(), "phase II: she may call her familiar");
            raven[0] = r.callFamiliar();
            helper.assertTrue(raven[0] != null && raven[0] == r.familiar(), "a raven answers her");
            helper.assertFalse(r.mayCallFamiliar(), "one at a time");
            raven[0].setHeld(TaglockItem.of(hunter[0]));
            helper.assertTrue(r.robbed() == hunter[0], "it holds the hunter's lock");
        });
        helper.runAfterDelay(t0 + every - 15, () -> helper.assertTrue(cursed(hunter[0]) == 0, "no curse before " + every + " ticks"));
        helper.runAfterDelay(t0 + every + 12, () -> {
            int d = cursed(hunter[0]);
            helper.assertTrue(d > RowenaBalance.HOMING_CURSE_TICKS - 30, "the first curse down the lock: " + d);
            helper.assertTrue(loss(hunter[0]) > 0, "and it hurts");
        });
        helper.runAfterDelay(t0 + 2 * every - 15, () -> {
            int d = cursed(hunter[0]);
            helper.assertTrue(d < RowenaBalance.HOMING_CURSE_TICKS - 90, "no second curse yet: " + d);
        });
        helper.runAfterDelay(t0 + 2 * every + 12, () -> {
            int d = cursed(hunter[0]);
            helper.assertTrue(d > RowenaBalance.HOMING_CURSE_TICKS - 30, "the second, " + every + " ticks on: " + d);
            raven[0].kill();
        });
        helper.runAfterDelay(t0 + 2 * every + 16, () -> {
            helper.assertTrue(r.familiar() == null && r.robbed() == null, "the raven is dead and holds nothing");
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, raven[0].getBoundingBox().inflate(6),
                    e -> e.getItem().is(AllItems.TAGLOCK.get()));
            helper.assertTrue(drops.stream().anyMatch(e -> {
                Taglock t = e.getItem().get(AllDataComponents.TAGLOCK.get());
                return t != null && t.owner().equals(hunter[0].getUUID());
            }), "the lock drops where it dies");
            drops.forEach(Entity::discard);
        });
        helper.runAfterDelay(t0 + 3 * every + 30, () -> {
            int d = cursed(hunter[0]);
            helper.assertTrue(d < RowenaBalance.HOMING_CURSE_TICKS - 120, "no curse after the raven died: " + d);
            BossTests.cleanup(helper);
            hunter[0].discard();
            helper.succeed();
        });
    }

    /** Killed, a victim heals her 4 % of her health (never past where the phase began); freed, it costs her 3 %. */
    @GameTest(template = SNGameTests.ARENA, batch = "rowena_victims", timeoutTicks = 60)
    public static void herVictimsHealHerIfKilledAndCostHerIfFreed(GameTestHelper helper) {
        RowenaEntity r = spawn(helper);
        helper.runAfterDelay(3, () -> {
            still(helper, r);
            r.forceLook(2);
            float max = r.getMaxHealth();
            BossHealthGuard.set(r, max * 0.5f);
            int first = r.summonVictims(RowenaBalance.VICTIMS_PER_CALL), more = r.summonVictims(5);
            helper.assertTrue(first == 2 && more == 1 && r.victims().size() == RowenaBalance.MAX_VICTIMS,
                    "two at a call, three at most: " + first + " + " + more);
            List<HexedVillagerEntity> v = r.victims();
            helper.assertTrue(v.stream().allMatch(h -> r.getUUID().equals(h.master())), "they serve her");
            float before = r.getHealth();
            v.get(0).free(null);
            float freed = before - r.getHealth();
            helper.assertTrue(Math.abs(freed - max * RowenaBalance.VICTIM_FREED) < 0.05f, "freed costs her 3 %: " + freed);
            float mid = r.getHealth();
            v.get(1).kill();
            float healed = r.getHealth() - mid;
            helper.assertTrue(Math.abs(healed - max * RowenaBalance.VICTIM_HEAL) < 0.05f, "killed heals her 4 %: " + healed);
            BossHealthGuard.set(r, max * 0.65f);
            v.get(2).kill();
            helper.assertTrue(Math.abs(r.getHealth() - max * RowenaBalance.threshold(1)) < 0.05f, "never past where the phase began: " + r.getHealth() / max);
            helper.assertFalse(r.hurt(r.damageSources().mobAttack(v.get(0)), 20), "her victims never wound her");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    // --- P3, the Recitation -----------------------------------------------------------------------------------------------

    /** Only a hunter's blow cracks a bowl (one per eight ticks); three blows break it; all three broken, she falls stunned. */
    @GameTest(template = SNGameTests.ARENA, batch = "rowena_recite_broken", timeoutTicks = 60)
    public static void allThreeBowlsBrokenAndSheFallsStunned(GameTestHelper helper) {
        RowenaEntity r = spawn(helper);
        helper.runAfterDelay(3, () -> {
            still(helper, r);
            r.forceLook(3);
            Mob target = dummy(helper, c().add(4, 0, 0));
            ServerPlayer p = CurseTests.mortal(helper, MID.offset(-3, 0, 0), ItemStack.EMPTY);
            RowenaAttacks.Recitation rec = new RowenaAttacks.Recitation();
            rec.onWindup(r, target);
            List<RecitationBowlEntity> bowls = r.bowls();
            helper.assertTrue(r.reciting() && bowls.size() == RowenaBalance.BOWLS, "three bowls, and she recites: " + bowls.size());
            Vec3 centre = r.arena().centerVec();
            helper.assertTrue(bowls.stream().allMatch(b -> Math.abs(Math.hypot(b.getX() - centre.x, b.getZ() - centre.z) - RowenaBalance.BOWL_DISTANCE) < 1),
                    "round the middle of her house, seven blocks out");
            RecitationBowlEntity b0 = bowls.getFirst();
            helper.assertFalse(b0.hurt(r.damageSources().mobAttack(target), 5), "a beast's blow does nothing");
            helper.assertFalse(b0.hurt(r.damageSources().mobAttack(r), 5), "nor her own");
            helper.assertTrue(b0.hurt(r.damageSources().playerAttack(p), 1) && b0.hits() == 1, "a hunter's blow cracks it");
            helper.assertFalse(b0.hurt(r.damageSources().playerAttack(p), 1), "a sweep is one blow");
            helper.assertTrue(b0.hits() == 1, "still one crack");
            b0.strike();
            helper.assertTrue(b0.isAlive() && b0.hits() == 2, "two");
            b0.strike();
            helper.assertTrue(b0.isRemoved() && r.bowls().size() == 2, "three blows break it");
            rec.tickActive(r, target, 10);
            helper.assertFalse(rec.broken() || r.stunned(), "two left: she recites on");
            for (RecitationBowlEntity b : r.bowls()) for (int i = 0; i < RowenaBalance.BOWL_HITS; i++) b.strike();
            rec.tickActive(r, target, 11);
            helper.assertTrue(rec.broken() && rec.endEarly(r, target), "all broken: the spell fails");
            helper.assertTrue(r.stunned() && !r.reciting(), "she falls out of the air, stunned");
            helper.assertFalse(r.isAerialPhase(), "and stays down");
            rec.onEnd(r);
            helper.assertFalse(r.mayRecite(), "no new recitation at once");
            helper.assertFalse(rec.spoken(), "the hex was never spoken");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    /** Stunned she takes half as much again from a blow, for 100 ticks. */
    @GameTest(template = SNGameTests.ARENA, batch = "rowena_stun", timeoutTicks = 160)
    public static void stunnedSheTakesHalfAsMuchAgainForAHundredTicks(GameTestHelper helper) {
        RowenaEntity r = spawn(helper);
        float[] lossOf = new float[2];
        ServerPlayer[] p = new ServerPlayer[1];
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(r.arena() != null, "her arena");
            p[0] = CurseTests.mortal(helper, MID.offset(-3, 0, 0), ItemStack.EMPTY);
            r.recitationBroken();
            helper.assertTrue(r.stunned(), "stunned");
            lossOf[0] = blow(r, p[0]);
        });
        helper.runAfterDelay(3 + RowenaBalance.STUN_TICKS - 12, () -> helper.assertTrue(r.stunned(), "still stunned near the end"));
        helper.runAfterDelay(3 + RowenaBalance.STUN_TICKS + 8, () -> {
            helper.assertFalse(r.stunned(), "the stun wears off after " + RowenaBalance.STUN_TICKS + " ticks");
            lossOf[1] = blow(r, p[0]);
            helper.assertTrue(Math.abs(lossOf[0] / lossOf[1] - RowenaBalance.STUN_VULNERABILITY) < 0.1f,
                    "stunned, ×1.5: " + lossOf[0] + " vs " + lossOf[1]);
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    private static float blow(RowenaEntity r, ServerPlayer p) {
        r.invulnerableTime = 0;
        BossHealthGuard.set(r, 900);
        r.hurt(r.damageSources().playerAttack(p), 20);
        return 900 - r.getHealth();
    }

    /** Bowls left whole: a red warning over the arena, then twelve for each to everyone in it, and nobody outside. */
    @GameTest(template = SNGameTests.ARENA, batch = "rowena_recite_spoken", timeoutTicks = 60)
    public static void bowlsLeftWholeAndTheHexSweepsTheArena(GameTestHelper helper) {
        RowenaEntity r = spawn(helper);
        helper.runAfterDelay(3, () -> {
            still(helper, r);
            r.forceLook(3);
            Mob inside = dummy(helper, c().add(4, 0, 0)), outside = dummy(helper, c().add(16, 0, 0));
            RowenaAttacks.Recitation rec = new RowenaAttacks.Recitation();
            rec.onWindup(r, inside);
            for (int i = 0; i < RowenaBalance.BOWL_HITS; i++) r.bowls().getFirst().strike();
            helper.assertTrue(r.bowls().size() == 2, "one broken, two whole");
            rec.tickActive(r, inside, RowenaBalance.RECITE_TICKS - RowenaBalance.WIPE_WARNING - 2);
            ServerLevel level = helper.getLevel();
            AABB arena = new AABB(r.arena().centerVec(), r.arena().centerVec()).inflate(3);
            int before = level.getEntitiesOfClass(TelegraphMarker.class, arena).size();
            rec.tickActive(r, inside, RowenaBalance.RECITE_TICKS - RowenaBalance.WIPE_WARNING);
            int warned = level.getEntitiesOfClass(TelegraphMarker.class, arena).size();
            helper.assertTrue(warned > before, "the red warning over the whole house");
            helper.assertTrue(loss(inside) == 0, "nothing lands before the end");
            rec.tickActive(r, inside, RowenaBalance.RECITE_TICKS - 1);
            helper.assertTrue(rec.spoken() && !rec.broken(), "the hex is spoken");
            helper.assertTrue(landed(inside, r, RowenaBalance.wipeDamage(2)), "twelve for each bowl still whole: " + loss(inside)
                    + " (want " + RowenaBalance.wipeDamage(2) * r.attackDamageMultiplier() + ")");
            helper.assertTrue(loss(outside) == 0, "outside her house nobody is touched");
            rec.onEnd(r);
            helper.assertTrue(r.bowls().isEmpty() && !r.reciting(), "the bowls go when it ends");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    // --- her spoils ---------------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "rowena_spoils", timeoutTicks = 60)
    public static void herGrimoireTheFirstTimeABustAlwaysAndTheGrandWitchForTheKeeper(GameTestHelper helper) {
        RowenaEntity r = spawn(helper);
        helper.runAfterDelay(3, () -> {
            still(helper, r);
            ServerLevel level = helper.getLevel();
            ServerPlayer first = CovenTests.fake(helper, "sn-test-rowena-first", MID.offset(3, 0, 0));
            ServerPlayer veteran = CovenTests.fake(helper, "sn-test-rowena-veteran", MID.offset(-3, 0, 0));
            HellTests.award(veteran, RowenaSpoils.ADVANCEMENT);
            first.setData(org.papiricoh.supernaturalcraft.registry.AllAttachments.ROWENA, RowenaRelation.NONE.withBook(RowenaRelation.Book.GIVEN));
            veteran.setData(org.papiricoh.supernaturalcraft.registry.AllAttachments.ROWENA, RowenaRelation.NONE.withBook(RowenaRelation.Book.KEPT));
            Vec3 at = r.position().add(0, 1.2, 0);
            RowenaSpoils.drop(level, r, List.of(first, veteran), at);
            List<ItemEntity> ground = level.getEntitiesOfClass(ItemEntity.class, new AABB(BlockPos.containing(at)).inflate(3));
            int grimoires = count(ground, AllItems.ROWENAS_GRIMOIRE.get()), busts = count(ground, AllItems.ROWENA_TROPHY.get());
            ground.forEach(Entity::discard);
            helper.assertTrue(grimoires == 1 && busts == 2, "her Grimoire the first time, a bust each: " + grimoires + ", " + busts);
            helper.assertTrue(RowenaSpoils.grandWitchComes(level, at, List.of(first)) == null, "no Grand Witch for a giver");
            GrandWitchNpcEntity gw = RowenaSpoils.grandWitchComes(level, at, List.of(first, veteran));
            helper.assertTrue(gw != null && veteran.getUUID().equals(gw.keeper()), "she walks out of the smoke for the keeper");
            gw.discard();
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    private static int count(List<ItemEntity> items, net.minecraft.world.item.Item item) {
        return items.stream().filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
    }

    // --- Rowena's Grimoire --------------------------------------------------------------------------------------------------

    private static final BlockPos BOWL = new BlockPos(5, 1, 5), STAND = new BlockPos(5, 1, 7);

    private static ItemStack taglock(LivingEntity e) {
        return TaglockItem.of(e);
    }

    private static int slotOf(ServerPlayer p, net.minecraft.world.item.Item item) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(item)) return i;
        return -1;
    }

    /**
     * Her Grimoire remembers the aimed spell its bearer spoke at a bowl (and whose lock named the target), speaks it again with no
     * bowl for the spell's mana, then needs a minute; a lock in the other hand aims it elsewhere; a brew is never remembered; with
     * nothing remembered it does nothing and costs no time.
     */
    @GameTest(template = SNGameTests.MEDIUM, batch = "rowena_grimoire", timeoutTicks = 60)
    public static void herGrimoireEchoesTheLastAimedSpellOnceAMinute(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        helper.setBlock(BOWL, AllBlocks.SPELL_BOWL.get().defaultBlockState());
        SpellBowlBlockEntity bowl = (SpellBowlBlockEntity) helper.getBlockEntity(BOWL);
        JournalTests.Witness w = CovenTests.witness(helper, STAND);
        Mob a = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1)), b = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(9, 1, 9));
        a.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
        b.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
        ItemStack spare = new ItemStack(AllItems.ROWENAS_GRIMOIRE.get());
        try {
            bowl.setContents(BowlContents.of(List.of(new ItemStack(Items.CACTUS), new ItemStack(Items.IRON_NUGGET), new ItemStack(Items.SPIDER_EYE),
                    taglock(a)), List.of(Dose.of(BowlLiquid.DEMON_BLOOD))));
            ManaManager.get(w).learnRite(SupernaturalCraft.asResource("curse_of_pain"));
            ManaManager.get(w).setMana(100);
            ItemStack flint = new ItemStack(Items.FLINT_AND_STEEL);
            w.setItemInHand(InteractionHand.MAIN_HAND, flint);
            w.getInventory().add(new ItemStack(AllItems.ROWENAS_GRIMOIRE.get()));
            helper.assertTrue(slotOf(w, AllItems.ROWENAS_GRIMOIRE.get()) != w.getInventory().selected, "the Grimoire rides in the pack");
            helper.assertTrue(bowl.tryLight(w, flint, InteractionHand.MAIN_HAND) == SpellBowlBlockEntity.LightResult.LIT, "the bowl catches");
        } catch (RuntimeException e) {
            CovenTests.leave(helper, w);
            throw e;
        }
        helper.runAfterDelay(15, () -> {
            try {
                helper.assertTrue(bowl.resolveRecitation(w, true, 0) == SpellBowlBlockEntity.Outcome.CAST, "the curse is spoken at the bowl");
                helper.assertTrue(a.hasEffect(AllMobEffects.AGONY), "and lands");
                int slot = slotOf(w, AllItems.ROWENAS_GRIMOIRE.get());
                ItemStack g = w.getInventory().removeItemNoUpdate(slot);
                GrimoireEcho echo = RowenasGrimoireItem.echo(g);
                helper.assertTrue(echo != null && echo.recipe().equals(SupernaturalCraft.asResource("bowl_spell/curse_of_pain")),
                        "the Grimoire in the pack remembers it: " + echo);
                // A brew is never remembered.
                BowlSpellRecipe sight = helper.getLevel().getRecipeManager().byKey(SupernaturalCraft.asResource("bowl_spell/second_sight"))
                        .map(h -> (BowlSpellRecipe) h.value()).orElseThrow();
                w.getInventory().add(g);
                RowenasGrimoireItem.remember(w, SupernaturalCraft.asResource("bowl_spell/second_sight"), sight, BowlContents.EMPTY);
                g = w.getInventory().removeItemNoUpdate(slotOf(w, AllItems.ROWENAS_GRIMOIRE.get()));
                helper.assertTrue(echo.equals(RowenasGrimoireItem.echo(g)), "an unaimed spell does not overwrite the echo");
                // Spoken again, with no bowl.
                a.removeAllEffects();
                w.setItemInHand(InteractionHand.MAIN_HAND, g);
                w.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                ManaManager.get(w).setMana(100);
                var used = g.use(helper.getLevel(), w, InteractionHand.MAIN_HAND);
                helper.assertTrue(used.getResult().consumesAction(), "the echo is spoken: " + used.getResult());
                helper.assertTrue(a.hasEffect(AllMobEffects.AGONY), "at the remembered target");
                helper.assertTrue(ManaManager.get(w).mana() < 100, "for the spell's mana");
                helper.assertTrue(w.getCooldowns().isOnCooldown(g.getItem()) && w.getCooldowns().getCooldownPercent(g.getItem(), 0) > 0.99f,
                        "then a minute's rest");
                helper.assertTrue(echo.equals(RowenasGrimoireItem.echo(g)), "and nothing of the remembered bowl is spent");
                // A lock in the other hand aims it elsewhere.
                a.removeAllEffects();
                w.setItemInHand(InteractionHand.OFF_HAND, taglock(b));
                ManaManager.get(w).setMana(100);
                helper.assertTrue(RowenasGrimoireItem.recast(w, g) == null, "spoken again at another");
                helper.assertTrue(b.hasEffect(AllMobEffects.AGONY) && !a.hasEffect(AllMobEffects.AGONY), "the lock in the other hand names the target");
                // Nothing remembered: nothing happens, and no time is lost.
                w.getCooldowns().removeCooldown(spare.getItem());
                w.setItemInHand(InteractionHand.MAIN_HAND, spare);
                w.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                var empty = spare.use(helper.getLevel(), w, InteractionHand.MAIN_HAND);
                helper.assertFalse(empty.getResult().consumesAction(), "an empty Grimoire does nothing");
                helper.assertFalse(w.getCooldowns().isOnCooldown(spare.getItem()), "and costs no time");
                helper.assertTrue("message.supernaturalcraft.rowenas_grimoire.empty".equals(RowenasGrimoireItem.recast(w, spare)), "it says so");
            } finally {
                CovenTests.leave(helper, w);
                a.discard();
                b.discard();
            }
            helper.succeed();
        });
    }
}
