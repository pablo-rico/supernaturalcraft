package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceDialogue;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.cain.CainDialogue;
import org.papiricoh.supernaturalcraft.cain.CainFarm;
import org.papiricoh.supernaturalcraft.cain.CainNpcEntity;
import org.papiricoh.supernaturalcraft.cain.CainSavedData;
import org.papiricoh.supernaturalcraft.cain.CainServerHandlers;
import org.papiricoh.supernaturalcraft.cain.CainSite;
import org.papiricoh.supernaturalcraft.cain.CainWorld;
import org.papiricoh.supernaturalcraft.cain.layout.CainFarmLayout;
import org.papiricoh.supernaturalcraft.entity.boss.cain.CainAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.cain.CainBalance;
import org.papiricoh.supernaturalcraft.entity.boss.cain.CainEntity;
import org.papiricoh.supernaturalcraft.entity.boss.cain.CainSpoils;
import org.papiricoh.supernaturalcraft.mark.Marks;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.weapon.curse.Curses;

import java.util.List;

/**
 * Cain (v0.21 P4). His attacks, each in its own batch: every one lands on whoever stands in what it showed and spares whoever
 * does not (the lunge's lane, the swarm's circles with their poison and blindness, the furrow's cone and the earth it tears up,
 * Abel's blood ring with its safe middle and its spikes that sink again, the tools' lanes, the execution's circle and his blink
 * behind it, the flare that spares only the shelters toward his hives). His offer of the Mark as he kneels (taken through the
 * shared dialogue, with his First Blade the first time), his spoils, his grave (which offers it again, and thirsty to one whose
 * Mark was taken off), his farm (raised once), the fight only at night, and the beekeeper at home only for hunters who have put
 * Lucifer back in his box and not yet beaten him. Tests that touch the world's {@link CainSavedData} restore it.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class CainTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    /** A still pig with health to spare: his blows are found on it (fake players are not found by a search). */
    private static Mob dummy(GameTestHelper helper, Vec3 rel) {
        Mob m = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos((int) Math.floor(rel.x), (int) Math.floor(rel.y), (int) Math.floor(rel.z)));
        m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        m.setHealth(1000);
        Vec3 v = helper.absoluteVec(rel);
        m.moveTo(v.x, v.y, v.z, 0, 0);
        return m;
    }

    private static boolean struck(Mob m) {
        return m.getHealth() < 1000f;
    }

    /** Cain at the middle of a clean ARENA floor, his yard opened round him, standing still for the test. */
    private static CainEntity cain(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        return helper.spawn(AllEntities.CAIN.get(), MID);
    }

    private static void still(GameTestHelper helper, CainEntity c) {
        helper.assertTrue(c.arena() != null && c.arena().theme() == ArenaTheme.FARM, "his yard opens round him (FARM)");
        c.setNoAi(true);
        Vec3 v = helper.absoluteVec(Vec3.atBottomCenterOf(MID));
        c.teleportTo(v.x, v.y, v.z);
    }

    private static Vec3 rel(double dx, double dz) {
        return new Vec3(MID.getX() + 0.5 + dx, 1, MID.getZ() + 0.5 + dz);
    }

    // --- P1 · the Beekeeper ------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "cain_lunge", timeoutTicks = 60)
    public static void theLungeRunsDownItsLaneOnly(GameTestHelper helper) {
        CainEntity c = cain(helper);
        helper.runAfterDelay(3, () -> {
            still(helper, c);
            Mob target = dummy(helper, rel(6, 0)), beside = dummy(helper, rel(0, 5)), behind = dummy(helper, rel(-4, 0));
            Vec3 start = c.position();
            CainAttacks.Lunge lunge = new CainAttacks.Lunge();
            lunge.onWindup(c, target);
            lunge.onActive(c, target);
            helper.assertTrue(struck(target), "the hunter in the lane is run down");
            helper.assertFalse(struck(beside) || struck(behind), "nobody beside or behind the lane");
            helper.assertTrue(c.position().distanceTo(start) > 5, "he ends his dash down the lane, from " + start + " to " + c.position());
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "cain_swarm", timeoutTicks = 60)
    public static void theSwarmStingsPoisonsAndBlindsInItsCircles(GameTestHelper helper) {
        CainEntity c = cain(helper);
        helper.runAfterDelay(3, () -> {
            still(helper, c);
            Mob target = dummy(helper, rel(5, 3));
            // Beyond his yard, where not even the stray circle can fall.
            Mob far = dummy(helper, new Vec3(1.5, 1, 1.5));
            CainAttacks.Swarm swarm = new CainAttacks.Swarm();
            swarm.onWindup(c, target);
            swarm.onActive(c, target);
            helper.assertTrue(struck(target), "stung in its circle");
            helper.assertTrue(target.hasEffect(MobEffects.POISON) && target.hasEffect(MobEffects.BLINDNESS), "poisoned and blinded");
            helper.assertFalse(struck(far) || far.hasEffect(MobEffects.POISON), "nothing outside the circles");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    // --- P2 · the Mark awakens ---------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "cain_furrow", timeoutTicks = 60)
    public static void theFurrowTearsItsConeAndTheYardIsGivenBack(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        // His fields: grass in front of him (+x) before the yard is opened, so the arena remembers it.
        for (int dx = 1; dx <= 8; dx++) for (int dz = -2; dz <= 2; dz++) helper.setBlock(MID.offset(dx, -1, dz), Blocks.GRASS_BLOCK);
        CainEntity c = helper.spawn(AllEntities.CAIN.get(), MID);
        helper.runAfterDelay(3, () -> {
            still(helper, c);
            Mob target = dummy(helper, rel(4, 0)), behind = dummy(helper, rel(-4, 0));
            CainAttacks.Furrow furrow = new CainAttacks.Furrow();
            furrow.onWindup(c, target);
            furrow.onActive(c, target);
            helper.assertTrue(struck(target), "the hunter in the cone is thrown");
            helper.assertFalse(struck(behind), "nobody behind him");
            int torn = 0;
            for (int dx = 1; dx <= 8; dx++) for (int dz = -2; dz <= 2; dz++) if (helper.getBlockState(MID.offset(dx, -1, dz)).is(Blocks.ROOTED_DIRT)) torn++;
            helper.assertTrue(torn > 0, "the earth in the cone is torn up");
            BossTests.cleanup(helper);
            for (int dx = 1; dx <= 8; dx++) {
                for (int dz = -2; dz <= 2; dz++) helper.assertBlockPresent(Blocks.GRASS_BLOCK, MID.offset(dx, -1, dz));
            }
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "cain_abel", timeoutTicks = 120)
    public static void abelsBloodRunsThroughTheRingAndSparesTheGroundByHim(GameTestHelper helper) {
        CainEntity c = cain(helper);
        int[] spikes = new int[1];
        helper.runAfterDelay(3, () -> {
            still(helper, c);
            Mob ring = dummy(helper, rel(5, 0)), near = dummy(helper, rel(0, 1.5)), out = dummy(helper, rel(-9.5, 0));
            CainAttacks.AbelsBlood abel = new CainAttacks.AbelsBlood();
            abel.onWindup(c, ring);
            abel.onActive(c, ring);
            helper.assertTrue(struck(ring), "run through in the ring");
            helper.assertFalse(struck(near), "the green by him is safe");
            helper.assertFalse(struck(out), "beyond the ring is safe");
            for (BlockPos p : BlockPos.betweenClosed(MID.offset(-8, 0, -8), MID.offset(8, 0, 8))) {
                if (helper.getBlockState(p).is(Blocks.POINTED_DRIPSTONE)) spikes[0]++;
            }
            helper.assertTrue(spikes[0] > 10, "spikes burst up round him: " + spikes[0]);
        });
        helper.runAfterDelay(3 + CainBalance.ABEL_SPIKE_TICKS + 10, () -> {
            int left = 0;
            for (BlockPos p : BlockPos.betweenClosed(MID.offset(-8, 0, -8), MID.offset(8, 0, 8))) {
                if (helper.getBlockState(p).is(Blocks.POINTED_DRIPSTONE)) left++;
            }
            helper.assertTrue(left == 0, "the spikes sink again (" + left + " of " + spikes[0] + " left)");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "cain_tools", timeoutTicks = 60)
    public static void theToolsFlyDownTheirLanes(GameTestHelper helper) {
        CainEntity c = cain(helper);
        helper.runAfterDelay(3, () -> {
            still(helper, c);
            Mob target = dummy(helper, rel(8, 0)), beside = dummy(helper, rel(0, 6));
            CainAttacks.ThrowTools tools = new CainAttacks.ThrowTools();
            tools.onWindup(c, target);
            tools.onActive(c, target);
            helper.assertFalse(struck(target), "nothing lands before the tools do");
            tools.tickActive(c, target, CainBalance.TOOLS_FLIGHT);
            helper.assertTrue(struck(target), "the tools strike down the lane");
            helper.assertFalse(struck(beside), "nobody off the lanes");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    // --- P3 · the Father of Murder -----------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "cain_execution", timeoutTicks = 60)
    public static void theExecutionFallsOnItsCircleFromBehind(GameTestHelper helper) {
        CainEntity c = cain(helper);
        helper.runAfterDelay(3, () -> {
            still(helper, c);
            Mob target = dummy(helper, rel(6, 0)), aside = dummy(helper, rel(6, 4.5));
            CainAttacks.Execution ex = new CainAttacks.Execution();
            ex.onWindup(c, target);
            ex.onActive(c, target);
            helper.assertTrue(struck(target), "the blade comes down on the circle");
            helper.assertFalse(struck(aside), "a step outside it is safe");
            double d = c.position().subtract(target.position()).horizontalDistance();
            helper.assertTrue(d < CainBalance.EXECUTION_RADIUS && d > 0.5, "he blinks in behind his mark: " + d);
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "cain_flare", timeoutTicks = 60)
    public static void theFlareBurnsTheYardButTheSheltersByHisHives(GameTestHelper helper) {
        CainEntity c = cain(helper);
        helper.runAfterDelay(3, () -> {
            still(helper, c);
            BlockPos centre = c.arena().center();
            // One hive to the east: its shelter lies at the yard's edge toward it.
            c.setHives(List.of(centre.offset(20, 0, 0)));
            double reach = c.arena().radius() + 1 - 3.5;
            Mob sheltered = dummy(helper, rel(reach, 0)), exposed = dummy(helper, rel(-3, 0));
            CainAttacks.MarkFlare flare = new CainAttacks.MarkFlare();
            flare.onWindup(c, exposed);
            flare.onActive(c, exposed);
            helper.assertTrue(struck(exposed), "the yard burns");
            helper.assertFalse(struck(sheltered), "the shelter toward his hive is safe");
            helper.assertTrue(CainAttacks.shelters(c, Vec3.atBottomCenterOf(centre), c.arena().radius() + 1).size() == CainBalance.FLARE_SHELTERS,
                    "three shelters");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    /** His own First Blade bites him half as hard again as any other blade. */
    @GameTest(template = SNGameTests.ARENA, batch = "cain_blade", timeoutTicks = 60)
    public static void hisOwnBladeBitesHimHarder(GameTestHelper helper) {
        CainEntity c = cain(helper);
        helper.runAfterDelay(3, () -> {
            still(helper, c);
            // Held by a mob, so no hunter's own bonuses (ascension, the Mark, runes) muddy the measure.
            net.minecraft.world.entity.monster.Zombie withBlade = ArsenalTests.dummy(helper, MID.offset(2, 0, 0), 90);
            net.minecraft.world.entity.monster.Zombie withSword = ArsenalTests.dummy(helper, MID.offset(-2, 0, 0), 270);
            withBlade.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(AllItems.FIRST_BLADE.get()));
            withSword.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
            org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard.set(c, c.getMaxHealth() * 0.9f);
            float[] lost = new float[2];
            int i = 0;
            for (net.minecraft.world.entity.monster.Zombie z : List.of(withBlade, withSword)) {
                c.invulnerableTime = 0;
                float before = c.trueHealth();
                c.hurt(c.damageSources().mobAttack(z), 4f);
                lost[i++] = before - c.trueHealth();
            }
            helper.assertTrue(lost[1] > 0 && Math.abs(lost[0] / lost[1] - CainBalance.FIRST_BLADE_VULNERABILITY) < 0.1f,
                    "the First Blade x1.5 on him: " + lost[0] + " vs " + lost[1]);
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    // --- the Mark offered, his spoils, his grave ---------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "cain_offer", timeoutTicks = 60)
    public static void asHeKneelsHeOffersTheMarkAndHisBlade(GameTestHelper helper) {
        CainEntity c = cain(helper);
        helper.runAfterDelay(3, () -> {
            still(helper, c);
            ServerPlayer taker = CovenTests.fake(helper, "sn-test-cain-taker", MID.offset(3, 0, 0));
            ServerPlayer refuser = CovenTests.fake(helper, "sn-test-cain-refuser", MID.offset(-3, 0, 0));
            ServerPlayer bearer = CovenTests.fake(helper, "sn-test-cain-bearer", MID.offset(0, 0, 3));
            Marks.grant(bearer, null, false);
            helper.assertTrue(CainSpoils.offerAll(c, List.of(taker, refuser, bearer)) == 2, "offered to the two who do not bear it");
            AllegianceDialogue.Pending q = AllegianceDialogue.pending(taker);
            helper.assertTrue(q != null && CainSpoils.DIALOGUE.equals(q.dialogue()) && CainSpoils.OFFER.equals(q.node())
                    && q.options().equals(List.of(CainSpoils.TAKE, CainSpoils.REFUSE)), "the offer is asked: " + q);
            helper.assertTrue(AllegianceDialogue.pending(bearer) == null, "a bearer is not asked");
            helper.assertTrue(AllegianceDialogue.answer(taker, c.getId(), CainSpoils.DIALOGUE, CainSpoils.OFFER, CainSpoils.TAKE), "the answer counts");
            helper.assertTrue(Marks.marked(taker) && Marks.thirst(taker) == 0f, "the Mark burns into the taker's arm, quiet");
            helper.assertTrue(Marks.get(taker).grantedBy().filter(c.getUUID()::equals).isPresent(), "given by Cain");
            ItemStack blade = taker.getInventory().items.stream().filter(s -> s.is(AllItems.FIRST_BLADE.get())).findFirst().orElse(ItemStack.EMPTY);
            helper.assertTrue(!blade.isEmpty() && Curses.state(blade).ownedBy(taker.getUUID()), "and his First Blade, bound to them");
            AllegianceDialogue.answer(refuser, c.getId(), CainSpoils.DIALOGUE, CainSpoils.OFFER, CainSpoils.REFUSE);
            helper.assertFalse(Marks.marked(refuser) || refuser.getInventory().countItem(AllItems.FIRST_BLADE.get()) > 0, "refused: nothing");
            // A hunter who already carries a First Blade is not given a second.
            ServerPlayer armed = CovenTests.fake(helper, "sn-test-cain-armed", MID.offset(0, 0, -3));
            armed.getInventory().add(new ItemStack(AllItems.FIRST_BLADE.get()));
            helper.assertTrue(CainSpoils.take(armed, c.getUUID()) && armed.getInventory().countItem(AllItems.FIRST_BLADE.get()) == 1, "one blade");
            helper.assertFalse(CainSpoils.take(armed, c.getUUID()), "the Mark is taken once");
            // His spoils: the bust and three jars the first time, the bust and one after.
            ServerPlayer winner = CovenTests.fake(helper, "sn-test-cain-winner", MID.offset(2, 0, 2));
            List<ItemStack> first = CainSpoils.share(winner);
            helper.assertTrue(count(first, AllItems.CAIN_TROPHY.get()) == 1 && count(first, AllItems.CAINS_HONEY.get()) == 3, "first: bust and three jars");
            HellTests.award(winner, CainSpoils.ADVANCEMENT);
            List<ItemStack> again = CainSpoils.share(winner);
            helper.assertTrue(count(again, AllItems.CAIN_TROPHY.get()) == 1 && count(again, AllItems.CAINS_HONEY.get()) == 1, "rematch: bust and a jar");
            for (ServerPlayer p : List.of(taker, refuser, bearer, armed, winner)) AllegianceDialogue.forget(p.getUUID());
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    private static int count(List<ItemStack> stacks, net.minecraft.world.item.Item item) {
        return stacks.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "cain_grave", timeoutTicks = 40)
    public static void hisGraveOffersTheMarkAgainToThoseWhoBeatHim(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerLevel level = helper.getLevel();
        CainSavedData data = CainSavedData.get(level);
        CompoundTag before = data.snapshot();
        BlockPos grave = helper.absolutePos(new BlockPos(5, 1, 5));
        try {
            data.restore(new CompoundTag());
            data.setGrave(grave);
            level.setBlockAndUpdate(grave, AllBlocks.GRAVE_HEADSTONE.get().defaultBlockState());
            ServerPlayer stranger = CovenTests.fake(helper, "sn-test-grave-stranger", new BlockPos(5, 1, 7));
            ServerPlayer victor = CovenTests.fake(helper, "sn-test-grave-victor", new BlockPos(5, 1, 3));
            HellTests.award(victor, CainSpoils.ADVANCEMENT);
            helper.assertFalse(CainWorld.useGrave(victor, grave.east()), "only his grave speaks");
            helper.assertTrue(CainWorld.useGrave(stranger, grave) && AllegianceDialogue.pending(stranger) == null,
                    "to one who never beat him the grave is quiet");
            helper.assertTrue(CainWorld.useGrave(victor, grave), "his grave");
            AllegianceDialogue.Pending q = AllegianceDialogue.pending(victor);
            helper.assertTrue(q != null && CainSpoils.GRAVE.equals(q.node()), "the Mark is offered again: " + q);
            helper.assertTrue(AllegianceDialogue.answer(victor, q.entity(), CainSpoils.DIALOGUE, CainSpoils.GRAVE, CainSpoils.TAKE)
                    && Marks.marked(victor) && Marks.thirst(victor) == 0f, "taken at the grave, quiet the first time");
            // A Mark taken off comes back thirsty, and the grave says so.
            Marks.remove(victor);
            helper.assertTrue(CainWorld.useGrave(victor, grave), "his grave again");
            q = AllegianceDialogue.pending(victor);
            helper.assertTrue(q != null && CainSpoils.GRAVE_AGAIN.equals(q.node()), "it knows the Mark was taken off: " + q);
            AllegianceDialogue.answer(victor, q.entity(), CainSpoils.DIALOGUE, CainSpoils.GRAVE_AGAIN, CainSpoils.TAKE);
            helper.assertTrue(Marks.marked(victor) && Math.abs(Marks.thirst(victor) - 0.5f) < 1e-4, "it remembers the way: " + Marks.thirst(victor));
            helper.assertTrue(victor.getInventory().countItem(AllItems.FIRST_BLADE.get()) <= 1, "never a second blade");
            // Taking it from afar does not count.
            Marks.remove(victor);
            CainWorld.useGrave(victor, grave);
            q = AllegianceDialogue.pending(victor);
            victor.moveTo(victor.getX() + 40, victor.getY(), victor.getZ());
            if (q != null) AllegianceDialogue.answer(victor, q.entity(), CainSpoils.DIALOGUE, q.node(), CainSpoils.TAKE);
            helper.assertFalse(Marks.marked(victor), "the Mark is taken at the grave, not from forty blocks off");
            AllegianceDialogue.forget(victor.getUUID());
            AllegianceDialogue.forget(stranger.getUUID());
        } finally {
            data.restore(before);
        }
        helper.succeed();
    }

    // --- the farm, the beekeeper, the challenge ----------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, batch = "cain_farm", timeoutTicks = 100)
    public static void hisFarmIsRaisedOnceWithHivesBeyondHisYardAndHisGraveDug(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CainSavedData data = CainSavedData.get(level);
        CompoundTag before = data.snapshot();
        // Far from every other test: an older world raises it when first needed.
        BlockPos planned = new BlockPos(-24016, 70, 24016);
        try {
            data.restore(new CompoundTag());
            CainSite.Site site = new CainSite.Site(planned, 1);
            BlockPos origin = CainWorld.ensureNow(level, data, site);
            helper.assertTrue(data.built() && origin.equals(data.farm()) && data.rotation() == 1, "raised and remembered");
            CainSite.Site at = new CainSite.Site(origin, 1);
            for (BlockPos h : CainFarm.hives(at)) helper.assertTrue(level.getBlockState(h).is(Blocks.BEEHIVE), "a hive at " + h.toShortString());
            // A mark a second raising would wipe out.
            BlockPos porch = CainWorld.home(at).below();
            level.setBlock(porch, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
            helper.assertTrue(CainWorld.ensureNow(level, data, site).equals(origin), "asked again, the same farm");
            helper.assertTrue(level.getBlockState(porch).is(Blocks.GOLD_BLOCK), "never raised twice");
            // His yard holds no block entity (the fight's arena erases and restores it).
            BlockPos centre = CainWorld.arenaCentre(at);
            int r = CainFarmLayout.ARENA_RADIUS + 1;
            BoundingBox box = CainFarm.fullBox(origin, 1);
            for (BlockPos p : BlockPos.betweenClosed(centre.getX() - r, box.minY(), centre.getZ() - r, centre.getX() + r, box.maxY(), centre.getZ() + r)) {
                if (Math.hypot(p.getX() - centre.getX(), p.getZ() - centre.getZ()) > r) continue;
                helper.assertTrue(level.getBlockEntity(p) == null, "a block entity in his yard at " + p.toShortString() + ": " + level.getBlockState(p));
            }
            // His grave is dug once he has fallen, beyond the yard.
            CainEntity fake = AllEntities.CAIN.get().create(level);
            CainWorld.fallen(level, fake);
            BlockPos g = CainWorld.graveSpot(at);
            helper.assertTrue(g.equals(data.grave()) && level.getBlockState(g).is(AllBlocks.GRAVE_HEADSTONE.get()), "his grave at " + g.toShortString());
            helper.assertTrue(Math.hypot(g.getX() - centre.getX(), g.getZ() - centre.getZ()) > CainFarmLayout.ARENA_RADIUS, "outside his yard");
            fake.discard();
            // His address leads to his gate.
            ItemStack note = org.papiricoh.supernaturalcraft.cain.CainsAddressItem.pointed(level);
            var pin = note.get(AllDataComponents.COMPASS_TARGET.get());
            BlockPos gate = CainWorld.address(level);
            helper.assertTrue(pin != null && pin.x() == gate.getX() && pin.z() == gate.getZ(), "the address's ink runs to his gate: " + pin);
        } finally {
            data.restore(before);
        }
        helper.succeed();
    }

    /** The beekeeper at home: one of him, only for a hunter near who has beaten Lucifer and not him. */
    @GameTest(template = SNGameTests.ARENA, batch = "cain_npc", timeoutTicks = 60)
    public static void theBeekeeperIsHomeOnlyForThoseWhoPutLuciferBack(GameTestHelper helper) {
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        CainSavedData data = new CainSavedData();
        // The farm's origin so that his porch (1, 0, -19 from it) lies inside the template.
        BlockPos origin = helper.absolutePos(new BlockPos(23, 1, 42));
        data.setFarm(origin, 0, true);
        CainSite.Site site = new CainSite.Site(origin, 0);
        BlockPos home = CainWorld.home(site);
        List<CainNpcEntity> before = level.getEntitiesOfClass(CainNpcEntity.class, new AABB(home).inflate(70));
        before.forEach(CainNpcEntity::discard);
        ServerPlayer p = CovenTests.fake(helper, "sn-test-cain-guest", new BlockPos(26, 1, 26));
        CainWorld.tickNpc(level, data, site, List.of(p));
        int early = npcs(level, home).size();
        HellTests.award(p, CainWorld.DEVIL_WENT_DOWN);
        CainWorld.tickNpc(level, data, site, List.of(p));
        CainWorld.tickNpc(level, data, site, List.of(p));
        List<CainNpcEntity> one = npcs(level, home);
        int twice = 0;
        if (!one.isEmpty()) {
            CainWorld.spawnNpc(level, site);
            CainWorld.tickNpc(level, data, site, List.of(p));
            twice = npcs(level, home).size();
        }
        boolean untouchable = one.isEmpty() || (!one.getFirst().hurt(level.damageSources().playerAttack(p), 1000) && one.getFirst().isAlive());
        HellTests.award(p, CainSpoils.ADVANCEMENT);
        CainWorld.tickNpc(level, data, site, List.of(p));
        int after = npcs(level, home).size();
        npcs(level, home).forEach(CainNpcEntity::discard);
        helper.assertTrue(early == 0, "not before Lucifer is back in his box: " + early);
        helper.assertTrue(one.size() == 1 && one.getFirst().blockPosition().equals(home), "exactly one of him, on his porch: " + one.size());
        helper.assertTrue(twice == 1, "never two of him: " + twice);
        helper.assertTrue(untouchable, "the beekeeper cannot be hurt");
        helper.assertTrue(after == 0, "once beaten, he is gone from his porch: " + after);
        helper.succeed();
    }

    private static List<CainNpcEntity> npcs(ServerLevel level, BlockPos home) {
        return level.getEntitiesOfClass(CainNpcEntity.class, new AABB(home).inflate(70), e -> !e.isRemoved());
    }

    /** He takes up the challenge only at night: by day he sends the hunter off; by night the fight starts where he stands. */
    @GameTest(template = SNGameTests.ARENA, batch = "cain_challenge", timeoutTicks = 60)
    public static void heTakesUpTheChallengeOnlyAtNight(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        CainSavedData data = CainSavedData.get(level);
        CompoundTag before = data.snapshot();
        long time = level.getDayTime();
        data.restore(new CompoundTag());
        CainNpcEntity npc = AllEntities.CAIN_NPC.get().create(level);
        BlockPos at = helper.absolutePos(MID);
        npc.home(at);
        npc.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        level.addFreshEntity(npc);
        ServerPlayer p = CovenTests.fake(helper, "sn-test-cain-challenger", MID.offset(3, 0, 0));
        String[] results = new String[3];
        level.setDayTime(6000);
        helper.runAfterDelay(2, () -> {
            results[0] = CainServerHandlers.choose(p, npc, CainDialogue.AGAIN, CainDialogue.OPT_FIGHT);
            HellTests.award(p, CainWorld.DEVIL_WENT_DOWN);
            results[1] = CainServerHandlers.choose(p, npc, CainDialogue.AGAIN, CainDialogue.OPT_FIGHT);
            level.setDayTime(18000);
        });
        helper.runAfterDelay(5, () -> {
            results[2] = CainServerHandlers.choose(p, npc, CainDialogue.AGAIN, CainDialogue.OPT_FIGHT);
            List<CainEntity> bosses = level.getEntitiesOfClass(CainEntity.class, new AABB(at).inflate(24));
            boolean npcGone = npc.isRemoved();
            boolean yard = bosses.size() == 1 && bosses.getFirst().arena() != null && bosses.getFirst().arena().theme() == ArenaTheme.FARM;
            BossTests.cleanup(helper);
            if (!npc.isRemoved()) npc.discard();
            AllegianceDialogue.forget(p.getUUID());
            level.setDayTime(time);
            data.restore(before);
            helper.assertTrue(results[0] == null, "a hunter who has not beaten Lucifer cannot even ask: " + results[0]);
            helper.assertTrue(CainDialogue.NOT_TONIGHT.equals(results[1]), "by day he sends them off: " + results[1]);
            helper.assertTrue(CainDialogue.FIGHT.equals(results[2]), "by night the fight starts: " + results[2]);
            helper.assertTrue(bosses.size() == 1 && npcGone, "the beekeeper gives way to the Father of Murder: " + bosses.size());
            helper.assertTrue(yard, "in his yard");
            helper.succeed();
        });
    }

    /** The honey he hands over: one jar a day. */
    @GameTest(template = SNGameTests.MEDIUM, batch = "cain_honey", timeoutTicks = 20)
    public static void heGivesAJarOfHoneyADay(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerLevel level = helper.getLevel();
        CainNpcEntity npc = AllEntities.CAIN_NPC.get().create(level);
        BlockPos at = helper.absolutePos(new BlockPos(5, 1, 5));
        npc.home(at);
        npc.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        level.addFreshEntity(npc);
        JournalTests.Witness w = CovenTests.witness(helper, new BlockPos(5, 1, 7));
        CainServerHandlers.open(npc, w);
        boolean met = AuthorWorld.done(w, CainServerHandlers.MET);
        String first = CainServerHandlers.choose(w, npc, CainDialogue.AGAIN, CainDialogue.OPT_HONEY);
        int jars = w.getInventory().countItem(AllItems.CAINS_HONEY.get());
        String second = CainServerHandlers.choose(w, npc, CainDialogue.AGAIN, CainDialogue.OPT_HONEY);
        int still = w.getInventory().countItem(AllItems.CAINS_HONEY.get());
        String leave = CainServerHandlers.choose(w, npc, CainDialogue.AGAIN, CainDialogue.LEAVE);
        AllegianceDialogue.forget(w.getUUID());
        CovenTests.leave(helper, w);
        npc.discard();
        helper.assertTrue(met, "main/met_cain the first time");
        helper.assertTrue(CainDialogue.HONEY.equals(first) && jars == 1, "a jar: " + first + ", " + jars);
        helper.assertTrue(CainDialogue.NO_HONEY.equals(second) && still == 1, "one a day: " + second + ", " + still);
        helper.assertTrue("".equals(leave), "goodbye ends the talk");
        helper.succeed();
    }
}
