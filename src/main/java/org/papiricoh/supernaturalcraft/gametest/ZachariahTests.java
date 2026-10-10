package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.balance.Balance;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.HorsemenGround;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.ApprovalStampItem;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.ClerkAngelEntity;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.Docket;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.FilingCabinetBlock;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.FormRules;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.MemoProjectile;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.OfficeWrap;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.ZachariahAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.ZachariahBalance;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.ZachariahEntity;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.ZachariahSpoils;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.ZachariahSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.arena.ZachariahOfficeLayout;
import org.papiricoh.supernaturalcraft.heaven.passage.HeavenStanding;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.reward.heaven.ZachariahsBladeItem;

import java.util.List;
import java.util.UUID;

/**
 * Zachariah, the angel of Heaven's paperwork (v0.18): his health and the hard cap, the office written and given back, the forms
 * (a quarter while unfiled, half again once filed in the right cabinet, a clerk's stamp files any), the wrap (moved a whole shift
 * with the same speed), Precedent striking where you stood, the docket (honest but for one revision), the Termination Notice
 * (split, or void at a desk), the overdue smite, the office dissolving, his spoils and the home they unlock. Every scene is far
 * from the origin (x ≥ 2000) and every boss test has its own batch and clears the field first.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class ZachariahTests {

    private static final int SITES = 11;

    private static void cleanup(GameTestHelper helper) {
        BossTests.cleanup(helper);
        for (Entity e : helper.getLevel().getAllEntities()) {
            if (e instanceof ClerkAngelEntity || e instanceof MemoProjectile) e.discard();
        }
        for (int k = 0; k < SITES; k++) force(helper.getLevel(), site(k), false);
    }

    /** Keeps a site's chunks loaded and ticking (nobody stands there in a test world). */
    private static void force(ServerLevel level, BlockPos site, boolean on) {
        int r = ZachariahBalance.ARENA_RADIUS + 8;
        for (int cx = (site.getX() - r) >> 4; cx <= (site.getX() + r) >> 4; cx++) {
            for (int cz = (site.getZ() - r) >> 4; cz <= (site.getZ() + r) >> 4; cz++) level.setChunkForced(cx, cz, on);
        }
    }

    /** A scene of its own, far from everything (one per test). */
    private static BlockPos site(int k) {
        return new BlockPos(2000 + 160 * k, 140, 2400);
    }

    /** Zachariah spawned (as by an egg) at {@code site}, on a slab of stone; his office is written after his first tick. */
    private static ZachariahEntity spawn(GameTestHelper helper, BlockPos site) {
        cleanup(helper);
        ServerLevel level = helper.getLevel();
        force(level, site, true);
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) level.setBlockAndUpdate(site.offset(x, -1, z), Blocks.STONE.defaultBlockState());
        ZachariahEntity z = AllEntities.ZACHARIAH.get().create(level);
        z.moveTo(site.getX() + 0.5, site.getY(), site.getZ() + 0.5, 0, 0);
        level.addFreshEntity(z);
        return z;
    }

    private static ServerPlayer hunter(GameTestHelper helper, String name, Vec3 at) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.setGameMode(GameType.SURVIVAL);
        p.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN);
        p.getInventory().clearContent();
        p.moveTo(at.x, at.y, at.z);
        return p;
    }

    private static ServerPlayer mortal(GameTestHelper helper, Vec3 at) {
        ServerPlayer p = CurseTests.mortal(helper, BlockPos.ZERO, ItemStack.EMPTY);
        p.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN);
        // Sturdy enough to take his blows and live: what matters is what they take.
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
        p.moveTo(at.x, at.y, at.z);
        p.setHealth(p.getMaxHealth());
        p.invulnerableTime = 0;
        return p;
    }

    private static int forms(ServerPlayer p) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(AllItems.HEAVENLY_FORM.get())) n++;
        return n;
    }

    private static ItemStack form(ServerPlayer p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(AllItems.HEAVENLY_FORM.get())) return p.getInventory().getItem(i);
        }
        return ItemStack.EMPTY;
    }

    /** True health he loses to {@code amount} from {@code p}'s hand. */
    private static float blow(ZachariahEntity z, ServerPlayer p, float amount) {
        z.invulnerableTime = 0;
        float before = z.trueHealth();
        z.hurt(z.damageSources().playerAttack(p), amount);
        return before - z.trueHealth();
    }

    // --- his health and his office -----------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, batch = "zachariah_health", timeoutTicks = 60)
    public static void fiftyThousandTrueHealthTheHardCapAndQuarters(GameTestHelper helper) {
        ZachariahEntity z = spawn(helper, site(0));
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(Math.abs(z.trueMaxHealth() - 50_000f) < 1f, "50000 true health alone, has " + z.trueMaxHealth());
            helper.assertTrue(z.getMaxHealth() <= 1024, "vanilla health under the cap");
            helper.assertTrue(z.arena() != null && z.arena().theme() == ArenaTheme.OFFICE, "an office arena round him");
            BossHealthGuard.set(z, z.getMaxHealth() * 0.9f);
            z.invulnerableTime = 0;
            float before = z.trueHealth();
            z.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 1e6f);
            float lost = before - z.trueHealth(), hard = Balance.hardCap(z.trueMaxHealth());
            helper.assertTrue(lost <= hard + 0.5f && lost > hard * 0.99f, "a blow of a million takes the hard cap: " + lost + " vs " + hard);
            BossHealthGuard.set(z, z.getMaxHealth() * 0.76f);
            z.invulnerableTime = 0;
            z.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 1e6f);
            helper.assertTrue(Math.abs(z.getHealth() - z.getMaxHealth() * 0.75f) < 0.01f, "a blow stops at three quarters");
            helper.assertTrue(z.phase() == 2 && z.state() == LuciferEntity.TRANSITION, "and Review begins");
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.SMALL, batch = "zachariah_office", timeoutTicks = 60)
    public static void anEggWritesTheOfficeRoundHimWithItsCabinetsAndTheArenaGivesItBack(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos site = site(1);
        ZachariahEntity z = spawn(helper, site);
        helper.runAfterDelay(3, () -> {
            z.buildOfficeNow();
            helper.assertTrue(z.officeReady(), "the office stands");
            BlockPos o = z.officeOrigin();
            for (ArenaCell c : ZachariahOfficeLayout.plan().cells()) {
                BlockPos pos = o.offset(c.dx(), c.dy(), c.dz());
                boolean cabinet = false;
                for (var p : ZachariahOfficeLayout.CABINETS) cabinet |= pos.equals(o.offset(p.x(), p.y(), p.z()));
                if (cabinet) continue;
                helper.assertTrue(level.getBlockState(pos) == HorsemenGround.state(c.block()), "the plan is written at " + c);
            }
            for (int n = 1; n <= 4; n++) {
                BlockState s = level.getBlockState(z.cabinetPos(n));
                helper.assertTrue(s.is(AllBlocks.FILING_CABINET.get()) && s.getValue(FilingCabinetBlock.NUMBER) == n, "cabinet " + n + " stands");
            }
            BlockPos far = o.offset(ZachariahOfficeLayout.RADIUS - 2, -1, 0);
            cleanup(helper);
            helper.assertTrue(level.getBlockState(z.cabinetPos(1)).isAir(), "the arena takes the cabinets back");
            helper.assertTrue(level.getBlockState(far).isAir(), "and the office's floor");
            helper.assertTrue(level.getBlockState(o.below()).is(Blocks.STONE), "and gives the ground back");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.SMALL, batch = "zachariah_in_office", timeoutTicks = 60)
    public static void inAHuntersHeavenTheOfficeIsAlreadyThere(GameTestHelper helper) {
        cleanup(helper);
        ServerLevel level = helper.getLevel();
        BlockPos o = site(2);
        force(level, o, true);
        ZachariahEntity z = ZachariahSummoning.summonInOffice(level, o, null);
        helper.assertTrue(z != null && z.state() == LuciferEntity.EMERGING, "he rises at his desk");
        helper.assertTrue(ZachariahSummoning.summonInOffice(level, o, null) == null, "one Zachariah per office");
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(z.officeReady() && !z.writingOffice(), "nothing to write");
            helper.assertTrue(level.getBlockState(o.offset(ZachariahOfficeLayout.RADIUS - 2, -1, 0)).isAir(), "the office is not written over");
            BlockState s = level.getBlockState(z.cabinetPos(3));
            helper.assertTrue(s.is(AllBlocks.FILING_CABINET.get()) && s.getValue(FilingCabinetBlock.NUMBER) == 3, "the cabinets are put in place");
            cleanup(helper);
            helper.assertTrue(level.getBlockState(z.cabinetPos(3)).isAir(), "and taken back after");
            helper.succeed();
        });
    }

    // --- the paperwork ---------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, batch = "zachariah_forms", timeoutTicks = 60)
    public static void anUnfiledFormIsAQuarterAndAFiledOneHalfAgain(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ZachariahEntity z = spawn(helper, site(3));
        helper.runAfterDelay(3, () -> {
            z.buildOfficeNow();
            ServerPlayer p = hunter(helper, "sn-zach-forms", z.at(ZachariahOfficeLayout.ENTRY));
            z.track(p);
            BossHealthGuard.set(z, z.getMaxHealth() * 0.95f);
            float plain = blow(z, p, 40);
            z.issueForm(p, level.getGameTime());
            helper.assertTrue(forms(p) == 1, "a form in their inventory");
            ItemStack stack = form(p);
            int number = stack.get(AllDataComponents.HEAVENLY_FORM.get()).number();
            helper.assertTrue(z.formOf(p.getUUID()) != null && z.formOf(p.getUUID()).number() == number, "the office knows what it issued");
            float unfiled = blow(z, p, 40);
            helper.assertTrue(Math.abs(unfiled - plain * FormRules.UNFILED) < plain * 0.02f, "a quarter while unfiled: " + unfiled + " vs " + plain);
            int wrong = number % 4 + 1;
            BlockPos wrongPos = z.cabinetPos(wrong);
            helper.assertTrue(FilingCabinetBlock.fileAt(level, wrongPos, level.getBlockState(wrongPos), p, stack) == ZachariahEntity.Filing.WRONG_CABINET,
                    "the wrong cabinet only rattles");
            helper.assertTrue(forms(p) == 1, "and the form is still theirs");
            BlockPos right = z.cabinetPos(number);
            helper.assertTrue(FilingCabinetBlock.fileAt(level, right, level.getBlockState(right), p, form(p)) == ZachariahEntity.Filing.FILED,
                    "the right cabinet files it");
            helper.assertTrue(forms(p) == 0 && z.formOf(p.getUUID()) == null, "the form is gone");
            helper.assertTrue(z.approved(p.getUUID()), "Approved");
            float filed = blow(z, p, 40);
            helper.assertTrue(Math.abs(filed - plain * FormRules.APPROVED) < plain * 0.02f, "half again once Approved: " + filed + " vs " + plain);
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.SMALL, batch = "zachariah_stamp", timeoutTicks = 100)
    public static void aClerksStampFilesAnyForm(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ZachariahEntity z = spawn(helper, site(4));
        helper.runAfterDelay(40, () -> {
            ServerPlayer p = hunter(helper, "sn-zach-stamp", z.officeCentre().add(3, 0, 3));
            z.track(p);
            ItemStack stamp = new ItemStack(AllItems.APPROVAL_STAMP.get(), 2);
            helper.assertTrue(ApprovalStampItem.stamp(level, p, stamp) == ZachariahEntity.Filing.NOTHING_TO_FILE, "nothing to stamp yet");
            helper.assertTrue(stamp.getCount() == 2, "and the stamp is kept");
            z.issueForm(p, level.getGameTime());
            helper.assertTrue(ApprovalStampItem.stamp(level, p, stamp) == ZachariahEntity.Filing.FILED, "stamped and filed");
            helper.assertTrue(stamp.getCount() == 1 && forms(p) == 0 && z.approved(p.getUUID()), "one stamp spent, form gone, Approved");
            List<ClerkAngelEntity> clerks = z.callClerks(level);
            helper.assertTrue(clerks.size() == ZachariahBalance.CLERKS, "three clerks out of the cubicles");
            helper.assertTrue(clerks.stream().allMatch(c -> z.minions().contains(c.getUUID())), "his");
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.SMALL, batch = "zachariah_overdue", timeoutTicks = 80)
    public static void anOverdueFormCallsHeavenToCollect(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ZachariahEntity z = spawn(helper, site(5));
        ServerPlayer[] p = new ServerPlayer[1];
        helper.runAfterDelay(3, () -> {
            z.buildOfficeNow();
            p[0] = hunter(helper, "sn-zach-overdue", z.officeCentre().add(4, 0, 0));
            z.track(p[0]);
            z.issueForm(p[0], level.getGameTime());
            z.backdateForm(p[0].getUUID(), level.getGameTime() - SNConfig.ZACHARIAH_FORM_TICKS.get() - 1);
        });
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(z.formOf(p[0].getUUID()) == null && forms(p[0]) == 0, "the overdue form is taken back");
            helper.assertTrue(z.overdueHunters().contains(p[0].getUUID()), "and Heaven means to collect");
            var forced = z.forcedAttack(p[0]);
            helper.assertTrue(forced != null && forced.get() instanceof ZachariahAttacks.OverdueSmite, "the Overdue Smite cuts in");
            helper.assertTrue(z.overdueHunters().isEmpty(), "once");
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- the endless office ----------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, batch = "zachariah_wrap", timeoutTicks = 100)
    public static void walkingOutOfTheOfficeWalksYouBackIn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ZachariahEntity z = spawn(helper, site(6));
        // Entities in a freshly forced chunk are only visible once its entity section has loaded: give it a moment.
        helper.runAfterDelay(40, () -> {
            Vec3 c = z.officeCentre();
            int w = ZachariahOfficeLayout.WRAP_WINDOW, s = ZachariahOfficeLayout.WRAP_SHIFT;
            // The memo is checked by the rule itself (a fresh far chunk may not list its entities yet in a test world).
            MemoProjectile memo = new MemoProjectile(AllEntities.MEMO_PROJECTILE.get(), level);
            memo.setPos(c.x + w + 0.5, c.y + 1.5, c.z + 2);
            memo.setDeltaMovement(0.6, 0, 0.1);
            helper.assertTrue(OfficeWrap.wraps(z, memo), "something thrown wraps");
            Vec3 off = OfficeWrap.offset(c, memo.position());
            helper.assertTrue(off.distanceTo(new Vec3(-s, 0, 0)) < 1e-6, "a whole shift back: " + off);
            OfficeWrap.move(memo, off, true);
            helper.assertTrue(Math.abs(memo.getX() - (c.x + w + 0.5 - s)) < 1e-6, "the memo comes back in from the west");
            helper.assertTrue(memo.getDeltaMovement().distanceTo(new Vec3(0.6, 0, 0.1)) < 1e-6, "with the same speed");
            ServerPlayer p = hunter(helper, "sn-zach-wrap", c.add(-3, 0, -w - 0.7));
            p.setDeltaMovement(0, 0, -0.3);
            p.setYRot(123f);
            z.track(p);
            z.moveTo(c.x + w + 1, c.y, c.z);
            List<Entity> moved = OfficeWrap.tick(z, level);
            helper.assertTrue(moved.contains(p), "the hunter wraps: " + moved);
            helper.assertTrue(Math.abs(p.getZ() - (c.z - w - 0.7 + s)) < 1e-6 && Math.abs(p.getX() - (c.x - 3)) < 1e-6, "the hunter from the south");
            helper.assertTrue(p.getDeltaMovement().distanceTo(new Vec3(0, 0, -0.3)) < 1e-6 && p.getYRot() == 123f, "same speed, same facing");
            helper.assertFalse(OfficeWrap.wraps(z, z), "he does not wrap");
            helper.assertTrue(Math.abs(z.getX() - (c.x + w + 1)) < 1e-6, "he does not wrap");
            helper.assertTrue(OfficeWrap.tick(z, level).isEmpty(), "inside the window nothing moves");
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.SMALL, batch = "zachariah_precedent", timeoutTicks = 60)
    public static void precedentStrikesWhereYouStood(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ZachariahEntity z = spawn(helper, site(7));
        helper.runAfterDelay(3, () -> {
            z.buildOfficeNow();
            Vec3 c = z.officeCentre();
            Vec3 then = c.add(-6, 0, 5), now = c.add(6, 0, 5);
            ServerPlayer p = mortal(helper, now);
            z.track(p);
            long t = level.getGameTime();
            z.recordTrail(p, t - ZachariahBalance.PRECEDENT_DELAY - 5, then);
            z.recordTrail(p, t - 5, now);
            List<Vec3> spots = ZachariahAttacks.spots(z, List.of(p), t);
            helper.assertTrue(spots.size() == 2, "two spots: then and now, got " + spots);
            helper.assertTrue(spots.stream().anyMatch(v -> v.distanceTo(then) < 0.01), "where they stood");
            helper.assertTrue(spots.stream().anyMatch(v -> v.distanceTo(now) < 0.01), "where they stand");
            p.moveTo(then.x + 0.5, then.y, then.z);
            float before = p.getHealth();
            List<?> struck = ZachariahAttacks.strike(z, spots);
            helper.assertTrue(struck.contains(p) && p.getHealth() < before, "stepping back into the past is struck");
            p.moveTo(c.x, c.y, c.z - 9);
            p.invulnerableTime = 0;
            helper.assertTrue(!ZachariahAttacks.strike(z, spots).contains(p), "away from both, spared");
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.SMALL, batch = "zachariah_docket", timeoutTicks = 60)
    public static void itWasAlreadyWrittenButForOneRevision(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ZachariahEntity z = spawn(helper, site(8));
        helper.runAfterDelay(3, () -> {
            z.buildOfficeNow();
            ServerPlayer p = hunter(helper, "sn-zach-docket", z.officeCentre().add(4, 0, 0));
            z.track(p);
            z.forceLook(2);
            helper.assertFalse(z.docket().active(), "nothing is written in Review");
            z.forceLook(3);
            Docket d = z.docket();
            helper.assertTrue(d.active() && d.size() == 3, "three attacks written: " + d.entries());
            int revisions = 0;
            for (int i = 0; i < 8; i++) {
                String expected = d.pending() != null && d.pending().index() == 0 ? d.pending().replacement() : d.entries().getFirst();
                boolean revisedBefore = d.revised();
                BossAttack<LuciferEntity> a = z.nextForetold();
                helper.assertTrue(a != null && a.id.equals(expected), "attack " + i + " is what was written: " + expected + ", got " + (a == null ? null : a.id));
                helper.assertTrue(a.windup >= ZachariahBalance.FORETOLD_LEAD, "a foretold attack shows early");
                helper.assertTrue(d.size() == 3, "the docket is kept full");
                if (!revisedBefore && d.revised()) revisions++;
                if (d.pending() != null) d.tick(d.pending().due());
            }
            helper.assertTrue(revisions == 1, "exactly one revision in the phase, got " + revisions);
            z.forceLook(4);
            helper.assertTrue(d.size() == 5 && !d.revised(), "five in Final Judgment, and a new revision to come");
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.SMALL, batch = "zachariah_notice", timeoutTicks = 60)
    public static void aTerminationNoticeIsSharedOrVoidedAtADesk(GameTestHelper helper) {
        ZachariahEntity z = spawn(helper, site(9));
        helper.runAfterDelay(3, () -> {
            z.buildOfficeNow();
            Vec3 c = z.officeCentre();
            ServerPlayer a = mortal(helper, c.add(-4, 0, 14));
            ServerPlayer b = mortal(helper, c.add(-2, 0, 14));
            z.track(a);
            z.track(b);
            float max = a.getMaxHealth();
            List<?> paid = ZachariahAttacks.terminate(z, a);
            helper.assertTrue(paid.size() == 2, "the two of them share it: " + paid.size());
            float lostA = max - a.getHealth(), lostB = max - b.getHealth(), each = ZachariahBalance.terminationEach(max, 2);
            helper.assertTrue(lostA > each * 0.8f && lostA < each * 1.05f && lostB > each * 0.8f && lostB < each * 1.05f,
                    "each takes half of two fifths: " + lostA + ", " + lostB + " vs " + each);
            ServerPlayer d = mortal(helper, z.at(ZachariahOfficeLayout.DESK_SAFE.getFirst()));
            z.track(d);
            helper.assertTrue(z.atApprovedDesk(d.position()), "at an Approved desk");
            helper.assertTrue(ZachariahAttacks.terminate(z, d).isEmpty() && d.getHealth() == d.getMaxHealth(), "the notice is void there");
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.SMALL, batch = "zachariah_dissolve", timeoutTicks = 60)
    public static void finalJudgmentDissolvesTheOffice(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ZachariahEntity z = spawn(helper, site(10));
        helper.runAfterDelay(3, () -> {
            z.buildOfficeNow();
            z.forceLook(3);
            helper.assertTrue(z.wingsShown(), "his wings in the third phase");
            helper.assertFalse(z.dissolved(), "the office still stands");
            z.forceLook(4);
            helper.assertTrue(z.dissolved(), "Final Judgment dissolves it");
            BlockPos o = z.officeOrigin();
            for (ArenaCell cell : ZachariahOfficeLayout.dissolve()) {
                BlockPos pos = o.offset(cell.dx(), cell.dy(), cell.dz());
                if (z.arena() != null && !z.arena().contains(pos)) continue;
                helper.assertTrue(level.getBlockState(pos) == HorsemenGround.state(cell.block()), "dissolved at " + cell);
            }
            helper.assertTrue(level.getBlockState(o.below()).isSolid(), "the floor stays");
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- his fall ------------------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, batch = "zachariah_spoils", timeoutTicks = 20)
    public static void hisSpoilsAndTheHomeTheyUnlock(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = hunter(helper, "sn-zach-spoils", helper.absoluteVec(new Vec3(2, 1, 2)));
        RandomSource rng = RandomSource.create(7);
        helper.assertFalse(ZachariahSpoils.beatenBefore(p), "never beaten him");
        List<ItemStack> first = ZachariahSpoils.share(p, rng);
        helper.assertTrue(first.stream().anyMatch(s -> s.is(AllItems.ZACHARIAHS_BLADE.get()))
                && first.stream().anyMatch(s -> s.is(AllItems.HEAVENS_SEAL.get()))
                && first.stream().anyMatch(s -> s.is(AllItems.ZACHARIAH_TROPHY.get())), "the first time: blade, seal and bust");
        HellTests.award(p, ZachariahSpoils.ADVANCEMENT);
        helper.assertTrue(ZachariahSpoils.beatenBefore(p), "his fall is written down");
        boolean relic = false, bare = false;
        for (int i = 0; i < 40; i++) {
            List<ItemStack> again = ZachariahSpoils.share(p, rng);
            helper.assertTrue(again.getFirst().is(AllItems.ZACHARIAH_TROPHY.get()) && again.size() <= 2, "a rematch: the bust, and maybe a relic");
            relic |= again.size() == 2;
            bare |= again.size() == 1;
        }
        helper.assertTrue(relic && bare, "half the time a relic");
        p.setData(AllAttachments.HEAVEN_STANDING, HeavenStanding.NONE);
        ServerPlayer other = hunter(helper, "sn-zach-spoils-2", helper.absoluteVec(new Vec3(2, 1, 2)));
        other.setData(AllAttachments.HEAVEN_STANDING, HeavenStanding.NONE);
        List<ServerPlayer> credited = ZachariahSpoils.unlockHomes(level, p.getUUID(), List.of(p, other));
        helper.assertTrue(credited.size() == 1 && credited.getFirst() == p, "in their own Heaven, the owner's home");
        HeavenStanding s = p.getData(AllAttachments.HEAVEN_STANDING);
        helper.assertTrue(s.homeUnlocked() && s.zachariahWins() == 1, "home unlocked and one victory");
        helper.assertFalse(other.getData(AllAttachments.HEAVEN_STANDING).homeUnlocked(), "a guest's home is not unlocked");
        helper.assertTrue(ZachariahSpoils.unlockHomes(level, null, List.of(other)).size() == 1
                && other.getData(AllAttachments.HEAVEN_STANDING).homeUnlocked(), "an egg's fight credits whoever fought him");
        ItemStack blade = new ItemStack(AllItems.ZACHARIAHS_BLADE.get());
        helper.assertTrue(!ZachariahsBladeItem.countHit(blade) && !ZachariahsBladeItem.countHit(blade) && ZachariahsBladeItem.countHit(blade),
                "his blade files every third blow");
        helper.succeed();
    }
}
