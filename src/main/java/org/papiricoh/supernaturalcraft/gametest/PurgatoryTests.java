package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.eve.MothersHeartwoodItem;
import org.papiricoh.supernaturalcraft.entity.legacy.VampireEntity;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.purgatory.PurgatoryDimension;
import org.papiricoh.supernaturalcraft.purgatory.PurgatoryStanding;
import org.papiricoh.supernaturalcraft.purgatory.PurgatoryWorld;
import org.papiricoh.supernaturalcraft.purgatory.SoulAmberItem;
import org.papiricoh.supernaturalcraft.purgatory.rift.OpenPurgatoryRiftEffect;
import org.papiricoh.supernaturalcraft.purgatory.rift.PurgatoryRift;
import org.papiricoh.supernaturalcraft.purgatory.rift.PurgatoryRiftSavedData;
import org.papiricoh.supernaturalcraft.purgatory.rift.PurgatoryRifts;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.List;

/**
 * Purgatory's ways in and out (v0.39), on the GameTest server's stand-in (no custom dimensions there: Purgatory lives far out in
 * the Overworld, {@link PurgatoryWorld#level}): a hunter's standing kept and read back with defaults; thrown in once, never twice;
 * held until Eve falls, then a rift home leads back where they came from; the rite refused where it does not belong; soul amber's
 * mana; the Heartwood's hush.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class PurgatoryTests {

    private static ServerPlayer hunter(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = CurseTests.mortal(helper, at, ItemStack.EMPTY);
        p.setData(AllAttachments.PURGATORY, PurgatoryStanding.EMPTY);
        return p;
    }

    private static void closeRifts(ServerLevel level) {
        for (PurgatoryRift r : List.copyOf(PurgatoryRiftSavedData.get(level).all())) {
            PurgatoryRifts.collapse(level, r);
            PurgatoryRiftSavedData.get(level).remove(r);
        }
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "purgatory_standing")
    public static void aHuntersStandingIsKeptAndReadWithDefaults(GameTestHelper helper) {
        PurgatoryStanding empty = PurgatoryStanding.CODEC.parse(JsonOps.INSTANCE, new com.google.gson.JsonObject()).getOrThrow();
        helper.assertTrue(empty.equals(PurgatoryStanding.EMPTY), "an old save reads as never been");
        PurgatoryStanding s = PurgatoryStanding.EMPTY.withVisited().withReturn(Level.OVERWORLD, new Vec3(1.5, 64, -3.5)).withEscaped();
        var json = PurgatoryStanding.CODEC.encodeStart(JsonOps.INSTANCE, s).getOrThrow();
        PurgatoryStanding back = PurgatoryStanding.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        helper.assertTrue(back.equals(s), "round trip: " + back);
        helper.assertTrue(back.returnLevel().orElseThrow().equals(Level.OVERWORLD), "the way back");
        helper.assertTrue(PurgatoryWorld.level(helper.getLevel().getServer()) == helper.getLevel().getServer().overworld()
                && helper.getLevel().getServer().getLevel(PurgatoryDimension.LEVEL) == null, "the test server's Purgatory is a place in the Overworld");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "purgatory_thrown", timeoutTicks = 200)
    public static void dickRomansHuntersAreThrownInOnce(GameTestHelper helper) {
        ServerPlayer p = hunter(helper, new BlockPos(3, 1, 3));
        Vec3 before = p.position();
        int n = PurgatoryWorld.throwIn(helper.getLevel().getServer(), List.of(p));
        helper.assertTrue(n == 1 && PurgatoryWorld.pending(p), "a hunter who has never been is torn away");
        PurgatoryWorld.clearPending();
        PurgatoryWorld.throwNow(p);
        PurgatoryStanding s = PurgatoryWorld.standing(p);
        helper.assertTrue(s.visited(), "now they have been");
        helper.assertTrue(s.returnPos().distanceTo(before) < 0.01, "and the way back is where they stood");
        BlockPos o = PurgatoryWorld.origin(helper.getLevel());
        helper.assertTrue(Math.hypot(p.getX() - o.getX(), p.getZ() - o.getZ()) < PurgatoryWorld.LANDING_SEARCH + 8,
                "they land near Purgatory's heart: " + p.position());
        helper.assertTrue(PurgatoryWorld.safe(helper.getLevel(), p.blockPosition()), "on safe ground");
        helper.assertTrue(PurgatoryWorld.throwIn(helper.getLevel().getServer(), List.of(p)) == 0, "never a second time");
        helper.assertFalse(PurgatoryWorld.mayLeave(p), "and Purgatory holds them");
        p.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "purgatory_rifts", timeoutTicks = 60)
    public static void heldUntilEveFallsThenARiftLeadsHome(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        closeRifts(level);
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer p = hunter(helper, new BlockPos(2, 1, 2));
        Vec3 home = helper.absoluteVec(new Vec3(8.5, 1, 8.5));
        p.setData(AllAttachments.PURGATORY, PurgatoryStanding.EMPTY.withVisited().withReturn(Level.OVERWORLD, home));
        PurgatoryRift rift = PurgatoryRifts.open(level, helper.absolutePos(new BlockPos(5, 1, 5)), Direction.Axis.X, PurgatoryRift.Kind.HOME, 200);
        helper.assertBlockPresent(AllBlocks.PURGATORY_RIFT.get(), new BlockPos(5, 2, 5));
        helper.assertTrue(PurgatoryRiftSavedData.get(level).at(rift.anchor().above(2)) == rift, "the rift is known");
        helper.assertTrue(PurgatoryRifts.destination(level, p, rift) == null, "Purgatory will not let them go yet");
        PurgatoryWorld.escaped(p);
        var to = PurgatoryRifts.destination(level, p, rift);
        helper.assertTrue(to != null && to.newLevel() == level.getServer().overworld() && to.pos().distanceTo(home) < 0.01,
                "Eve fallen, it leads where they came from");
        // A rift into Purgatory leads to the landing, and remembers the way back.
        PurgatoryRift into = PurgatoryRifts.open(level, helper.absolutePos(new BlockPos(2, 1, 8)), Direction.Axis.Z, PurgatoryRift.Kind.INTO, 200);
        var in = PurgatoryRifts.destination(level, p, into);
        helper.assertTrue(in != null && in.newLevel() == PurgatoryWorld.level(level.getServer()), "into Purgatory (its stand-in here)");
        helper.assertTrue(PurgatoryWorld.standing(p).returnPos().distanceTo(Vec3.atBottomCenterOf(into.anchor())) < 3, "the way back is by the rift");
        PurgatoryRifts.collapse(level, rift);
        helper.assertBlockNotPresent(AllBlocks.PURGATORY_RIFT.get(), new BlockPos(5, 2, 5));
        closeRifts(level);
        // The rite: refused in the Overworld for nobody (it opens the way in), and refused in Purgatory to a hunter it still holds.
        helper.assertTrue(new OpenPurgatoryRiftEffect().precheck(level, helper.absolutePos(new BlockPos(5, 1, 5)), p) == null, "the way in opens");
        p.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "purgatory_amber", timeoutTicks = 40)
    public static void soulAmberGivesUpItsSoulAndTheHeartwoodHushesHerChildren(GameTestHelper helper) {
        ServerPlayer p = hunter(helper, new BlockPos(2, 1, 2));
        ArcanaData a = ManaManager.get(p);
        a.setMana(0);
        SoulAmberItem.crush(p);
        helper.assertTrue(Math.abs(a.mana() - Math.min(a.maxMana(), SoulAmberItem.MANA)) < 0.01f, "the soul's power: " + a.mana());
        VampireEntity v = helper.spawn(AllEntities.PURGATORY_VAMPIRE.get(), new BlockPos(6, 1, 6));
        v.setTarget(p);
        int n = MothersHeartwoodItem.hush(helper.getLevel(), p);
        helper.assertTrue(n >= 1 && v.getTarget() == null && MothersHeartwoodItem.hushed(v, p), "her child forgets the hunter");
        helper.assertTrue(MothersHeartwoodItem.child(v), "a soul of Purgatory is one of her children");
        helper.assertFalse(MothersHeartwoodItem.child(helper.spawn(AllEntities.ALPHA_VAMPIRE.get(), new BlockPos(8, 1, 8))), "an Alpha never is");
        helper.killAllEntitiesOfClass(net.minecraft.world.entity.Mob.class);
        p.discard();
        helper.succeed();
    }
}
