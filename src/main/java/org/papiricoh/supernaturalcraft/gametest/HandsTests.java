package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;
import org.papiricoh.supernaturalcraft.hands.HandClaims;
import org.papiricoh.supernaturalcraft.hands.HandLocator;
import org.papiricoh.supernaturalcraft.hands.Hands;
import org.papiricoh.supernaturalcraft.hands.HandsRegistry;
import org.papiricoh.supernaturalcraft.hands.HandsSpawnRules;
import org.papiricoh.supernaturalcraft.hands.HandsSpawns;
import org.papiricoh.supernaturalcraft.hands.TrackHandsEffect;
import org.papiricoh.supernaturalcraft.hands.altar.HandAltarBlockEntity;
import org.papiricoh.supernaturalcraft.hands.mob.ChittersRules;
import org.papiricoh.supernaturalcraft.hands.mob.ThuleOfficerEntity;
import org.papiricoh.supernaturalcraft.hands.mob.ThuleSoldierEntity;
import org.papiricoh.supernaturalcraft.hands.reliquary.ReliquaryAnchors;
import org.papiricoh.supernaturalcraft.hands.reliquary.ReliquaryBuilder;
import org.papiricoh.supernaturalcraft.util.WorldClock;

/**
 * v0.42, season 11's Hands of God slice: the altar gives each hunter each Hand once (and none while the Darkness is held), each
 * kind of reliquary is built by its command's builder (its altar told its kind, its chests filled), the tracking rite maps an
 * unclaimed reliquary, the officer raises his dead, and the wild spawns keep to their gates.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class HandsTests {

    static ServerPlayer hunter(GameTestHelper helper) {
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(5, 1, 5), ItemStack.EMPTY);
        Hands.set(p, HandClaims.EMPTY);
        return p;
    }

    static HandAltarBlockEntity altar(GameTestHelper helper, S11Assets.Hand kind) {
        BlockPos at = new BlockPos(5, 1, 7);
        helper.setBlock(at, HandsRegistry.HAND_ALTAR.get());
        HandAltarBlockEntity be = (HandAltarBlockEntity) helper.getBlockEntity(at);
        be.setKind(kind);
        return be;
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "hands_altar")
    public static void theAltarGivesEachHunterTheirHandOnce(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        HandAltarBlockEntity altar = altar(helper, S11Assets.Hand.ROD_OF_AARON);
        ServerPlayer a = hunter(helper), b = hunter(helper);
        var rod = HandsRegistry.hand(S11Assets.Hand.ROD_OF_AARON);
        helper.assertTrue(altar.claim(a, true) == HandAltarBlockEntity.Result.GIVEN, "the first hunter takes it");
        helper.assertTrue(a.getInventory().countItem(rod) == 1, "and holds it");
        helper.assertTrue(Hands.claims(a).has(S11Assets.Hand.ROD_OF_AARON), "and it is on their record");
        helper.assertTrue(altar.claim(a, true) == HandAltarBlockEntity.Result.ALREADY, "never twice");
        helper.assertTrue(a.getInventory().countItem(rod) == 1, "still one");
        helper.assertTrue(altar.claim(b, true) == HandAltarBlockEntity.Result.GIVEN, "another hunter takes their own");
        helper.assertTrue(b.getInventory().countItem(rod) == 1, "and holds it");
        helper.assertTrue(altar.claims() == 2, "the altar remembers both");
        // Another altar of the same kind will not give it again; one of another kind will.
        a.getInventory().clearContent();
        HandAltarBlockEntity other = altar(helper, S11Assets.Hand.ROD_OF_AARON);
        other.forget();
        helper.assertTrue(other.claim(a, true) == HandAltarBlockEntity.Result.ALREADY, "each kind once in the whole world");
        other.setKind(S11Assets.Hand.TRUMPET_OF_JERICHO);
        helper.assertTrue(other.claim(a, true) == HandAltarBlockEntity.Result.GIVEN, "but the next kind is theirs");
        helper.assertTrue(Hands.claims(a).count() == 2, "two on the record: " + Hands.claims(a).count());
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "hands_altar")
    public static void theAltarHoldsItsHandWhileTheDarknessIsHeld(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        HandAltarBlockEntity altar = altar(helper, S11Assets.Hand.NAIL_OF_THE_CROSS);
        ServerPlayer p = hunter(helper);
        helper.assertTrue(altar.claim(p, false) == HandAltarBlockEntity.Result.NOT_YET, "not while the Darkness is held");
        helper.assertTrue(p.getInventory().countItem(HandsRegistry.hand(S11Assets.Hand.NAIL_OF_THE_CROSS)) == 0, "nothing given");
        helper.assertTrue(Hands.claims(p).count() == 0 && altar.claims() == 0, "nothing recorded");
        altar.setKind(null);
        helper.assertTrue(altar.claim(p, true) == HandAltarBlockEntity.Result.EMPTY, "an altar of no reliquary gives nothing");
        helper.succeed();
    }

    /** Builds one kind far off the test's own ground and checks its altar and its first chest. */
    static void builds(GameTestHelper helper, S11Assets.Hand kind) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO).offset(1024 + kind.ordinal() * 160, 20, 1024);
        BlockPos at = ReliquaryBuilder.placeAt(level, origin, kind, kind.ordinal() % 4);
        helper.assertTrue(level.getBlockState(at).is(HandsRegistry.HAND_ALTAR.get()), "the altar stands at " + at);
        helper.assertTrue(level.getBlockEntity(at) instanceof HandAltarBlockEntity a && a.kind() == kind, "and keeps its kind");
        BlockPos chest = ReliquaryBuilder.point(origin, kind, kind.ordinal() % 4, ReliquaryAnchors.chest(0));
        helper.assertTrue(chest != null && level.getBlockState(chest).is(Blocks.CHEST), "a chest at " + chest);
        helper.assertTrue(level.getBlockEntity(chest) instanceof ChestBlockEntity c && c.getLootTable() != null, "with its loot to come");
        BlockPos kneel = ReliquaryBuilder.point(origin, kind, kind.ordinal() % 4, ReliquaryAnchors.KNEEL);
        // A carpet (a sixteenth high) is floor enough; the hunter needs the two blocks over it.
        helper.assertTrue(kneel != null && level.getBlockState(kneel).getCollisionShape(level, kneel).max(net.minecraft.core.Direction.Axis.Y) <= 0.2
                && level.getBlockState(kneel.above()).getCollisionShape(level, kneel.above()).isEmpty(), "room to stand before it");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "hands_place_uboat", timeoutTicks = 200)
    public static void theUboatWreckIsBuilt(GameTestHelper helper) {
        builds(helper, S11Assets.Hand.DELPHINES_HAND);
    }

    @GameTest(template = SNGameTests.SMALL, batch = "hands_place_bunker", timeoutTicks = 200)
    public static void theThuleBunkerIsBuilt(GameTestHelper helper) {
        builds(helper, S11Assets.Hand.NAIL_OF_THE_CROSS);
    }

    @GameTest(template = SNGameTests.SMALL, batch = "hands_place_crypt", timeoutTicks = 200)
    public static void theChurchCryptIsBuilt(GameTestHelper helper) {
        builds(helper, S11Assets.Hand.APOSTLES_CHALICE);
    }

    @GameTest(template = SNGameTests.SMALL, batch = "hands_place_temple", timeoutTicks = 200)
    public static void theArkTempleIsBuilt(GameTestHelper helper) {
        builds(helper, S11Assets.Hand.ROD_OF_AARON);
    }

    @GameTest(template = SNGameTests.SMALL, batch = "hands_place_monastery", timeoutTicks = 200)
    public static void theSnowMonasteryIsBuilt(GameTestHelper helper) {
        builds(helper, S11Assets.Hand.TRUMPET_OF_JERICHO);
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "hands_track", timeoutTicks = 200)
    public static void theTrackingRiteMapsAnUnclaimedReliquary(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = hunter(helper);
        BlockPos origin = helper.absolutePos(BlockPos.ZERO).offset(-600, 20, 300);
        BlockPos chalice = ReliquaryBuilder.placeAt(level, origin, S11Assets.Hand.APOSTLES_CHALICE, 0);
        // Every other Hand already taken: the rite can only mean the Chalice.
        HandClaims claims = HandClaims.EMPTY;
        for (S11Assets.Hand h : S11Assets.Hand.values()) if (h != S11Assets.Hand.APOSTLES_CHALICE) claims = claims.with(h);
        Hands.set(p, claims);
        BlockPos rite = helper.absolutePos(new BlockPos(5, 1, 5));
        TrackHandsEffect effect = new TrackHandsEffect();
        helper.assertTrue(effect.precheck(level, rite, p) == null, "the rite is allowed: " + effect.precheck(level, rite, p));
        helper.assertTrue(effect.perform(level, rite, p), "it finds something");
        helper.assertTrue(p.getInventory().countItem(Items.FILLED_MAP) == 1, "a drawn map");
        HandLocator.Located l = HandLocator.nearestUnclaimed(p);
        helper.assertTrue(l != null && l.hand() == S11Assets.Hand.APOSTLES_CHALICE, "pointing at the Chalice: " + l);
        // At its altar, or at a nearer one the world's own crypts gave (the test world may generate them too).
        double placed = Math.hypot(chalice.getX() - p.getX(), chalice.getZ() - p.getZ()), found = Math.hypot(l.pos().getX() - p.getX(), l.pos().getZ() - p.getZ());
        helper.assertTrue(found <= placed + 2, "at the nearest Chalice: " + l.pos() + " (placed at " + chalice + ")");
        // Every Hand taken: the rite is refused.
        Hands.set(p, claims.with(S11Assets.Hand.APOSTLES_CHALICE));
        helper.assertTrue(effect.precheck(level, rite, p) != null, "nothing left to track");
        HandLocator.forget(p);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "hands_thule")
    public static void anOfficerRaisesHisDead(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ThuleOfficerEntity officer = helper.spawn(HandsRegistry.THULE_OFFICER.get(), new BlockPos(5, 1, 5));
        int before = officer.soldiersNear();
        ThuleSoldierEntity s = officer.raiseAt(helper.absolutePos(new BlockPos(3, 1, 3)));
        helper.assertTrue(s != null && s.rising(), "a soldier climbs out of the ground");
        helper.assertTrue(officer.soldiersNear() == before + 1, "and stands with him");
        helper.assertTrue(!officer.getType().is(org.papiricoh.supernaturalcraft.registry.AllTags.Entities.BOSSES), "no boss");
        officer.discard();
        s.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "hands_spawns")
    public static void theWildOnesKeepToTheirGates(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer p = hunter(helper);
        ServerLevel level = helper.getLevel();
        // The sun is whole on the test server: no banshee, whatever the hour.
        helper.assertTrue(HandsSpawns.banshee(level, p, level.random) == null, "no banshee while the Darkness is held");
        long day = WorldClock.day(level.getServer());
        helper.assertTrue(HandsSpawnRules.chitters(true, day, 10, 0, 0) == ChittersRules.awake(day), "the Chitters keep their cycle");
        helper.assertTrue(!HandsSpawnRules.chitters(false, ChittersRules.FIRST, 10, 0, 0), "and wait for season 11");
        helper.succeed();
    }
}
