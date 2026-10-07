package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.ghost.GhostEntity;
import org.papiricoh.supernaturalcraft.grave.GraveBonesBlock;
import org.papiricoh.supernaturalcraft.grave.GraveBonesBlockEntity;
import org.papiricoh.supernaturalcraft.grave.GraveBuilder;
import org.papiricoh.supernaturalcraft.grave.GraveHeadstoneBlock;
import org.papiricoh.supernaturalcraft.grave.GraveLayout;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/**
 * Ghosts and their graves. All in one batch that sets the clock to midnight first: a ghost with a
 * grave sinks back into it by day.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class GhostTests {

    private static final String BATCH = "ghosts";
    private static final BlockPos MID = new BlockPos(5, 1, 5);

    private static void night(GameTestHelper helper) {
        helper.getLevel().setDayTime(18000);
    }

    private static GhostEntity ghost(GameTestHelper helper, BlockPos at) {
        GhostEntity g = helper.spawn(AllEntities.GHOST.get(), at);
        g.setNoAi(true);
        return g;
    }

    /** Bones on the floor at {@code rel} with air above, and their block entity. */
    private static GraveBonesBlockEntity bones(GameTestHelper helper, BlockPos rel) {
        helper.setBlock(rel, AllBlocks.GRAVE_BONES.get().defaultBlockState());
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(rel)) instanceof GraveBonesBlockEntity be)) {
            throw new IllegalStateException("grave bones have no block entity");
        }
        return be;
    }

    private static void use(GameTestHelper helper, ServerPlayer p, ItemStack stack, BlockPos rel) {
        BlockPos abs = helper.absolutePos(rel);
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.4, 0), Direction.UP, abs, false);
        helper.getLevel().getBlockState(abs).useItemOn(stack, helper.getLevel(), p, InteractionHand.MAIN_HAND, hit);
    }

    // --- the graveyard -------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void placeDirectDigsAGraveyard(GameTestHelper helper) {
        night(helper);
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                for (int y = 0; y <= 3; y++) helper.setBlock(new BlockPos(x, y, z), y == 3 ? Blocks.GRASS_BLOCK : Blocks.STONE);
            }
        }
        ServerLevel level = helper.getLevel();
        BlockPos ground = helper.absolutePos(new BlockPos(5, 3, 5));
        long seed = 0;
        while (GraveLayout.plan(ground, seed).count() != 3) seed++;
        GraveLayout.Plan plan = GraveBuilder.placeAt(level, ground, seed);
        for (int i = 0; i < plan.count(); i++) {
            BlockState head = level.getBlockState(plan.headstone(i));
            helper.assertTrue(head.is(AllBlocks.GRAVE_HEADSTONE.get()), "no headstone for grave " + i + " at " + plan.headstone(i));
            helper.assertTrue(head.getValue(GraveHeadstoneBlock.FACING) == plan.headstoneFacing(), "headstone " + i + " faces away from its grave");
            for (BlockPos m : plan.mound(i)) helper.assertTrue(level.getBlockState(m).is(AllBlocks.GRAVE_SOIL.get()), "no grave soil at " + m);
        }
        BlockState bones = level.getBlockState(plan.bones());
        helper.assertTrue(bones.is(AllBlocks.GRAVE_BONES.get()), "no bones at " + plan.bones());
        helper.assertFalse(bones.getValue(GraveBonesBlock.RESTED), "fresh bones should be restless");
        helper.assertTrue(level.getBlockEntity(plan.bones()) instanceof GraveBonesBlockEntity, "the bones lost their block entity");
        helper.assertTrue(level.getBlockEntity(plan.chest()) instanceof RandomizableContainerBlockEntity c && GraveBuilder.GRAVE_LOOT.equals(c.getLootTable()),
                "no buried chest with grave loot at " + plan.chest());
        int found = 0;
        for (BlockPos p : BlockPos.betweenClosed(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(10, 5, 10)))) {
            if (level.getBlockState(p).is(AllBlocks.GRAVE_BONES.get())) found++;
        }
        helper.assertTrue(found == 1, "exactly one restless grave, found " + found + " bones");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void bonesHoldUntilRested(GameTestHelper helper) {
        night(helper);
        SNGameTests.floor(helper, 11, 11);
        GraveBonesBlockEntity be = bones(helper, MID);
        BlockPos abs = helper.absolutePos(MID);
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(5, 1, 3), new ItemStack(Items.DIAMOND_PICKAXE));
        ServerLevel level = helper.getLevel();
        helper.assertTrue(level.getBlockState(abs).getDestroyProgress(p, level, abs) == 0f, "restless bones can be dug up");
        helper.assertTrue(level.getBlockState(abs).getExplosionResistance(level, abs, null) > 1000f, "restless bones can be blown up");
        be.markRested();
        helper.assertTrue(level.getBlockState(abs).getValue(GraveBonesBlock.RESTED), "markRested did not mark them");
        helper.assertTrue(level.getBlockState(abs).getDestroyProgress(p, level, abs) > 0f, "bones at rest should break like any block");
        helper.assertTrue(be.raiseGhost(level) == null, "bones at rest raised a ghost");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void onlyOneGhostPerGrave(GameTestHelper helper) {
        night(helper);
        SNGameTests.floor(helper, 11, 11);
        GraveBonesBlockEntity be = bones(helper, MID);
        ServerLevel level = helper.getLevel();
        GhostEntity first = be.raiseGhost(level);
        GhostEntity second = be.raiseGhost(level);
        helper.assertTrue(first != null && second != null, "the bones raised nothing");
        helper.assertTrue(first.isRemoved(), "raising again should replace the old ghost");
        helper.assertTrue(helper.absolutePos(MID).equals(second.bones()), "the ghost does not know its bones");
        second.setNoAi(true);
        // A stray copy that also claims these bones gives up on its own.
        GhostEntity stray = ghost(helper, new BlockPos(3, 2, 3));
        stray.setBones(helper.absolutePos(MID));
        helper.succeedWhen(() -> {
            helper.assertTrue(stray.isRemoved(), "the stray copy lingers");
            helper.assertFalse(second.isRemoved(), "the real ghost vanished");
        });
    }

    // --- damage ---------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void mundaneHarmPassesThrough(GameTestHelper helper) {
        night(helper);
        SNGameTests.floor(helper, 11, 11);
        GhostEntity g = ghost(helper, new BlockPos(5, 1, 6));
        float hp = g.getHealth();
        helper.assertFalse(g.hurt(helper.getLevel().damageSources().generic(), 6f), "generic damage landed");
        helper.assertFalse(g.hurt(helper.getLevel().damageSources().inFire(), 6f), "fire landed");
        ServerPlayer p = CurseTests.mortal(helper, MID, new ItemStack(Items.STONE_SWORD));
        CurseTests.armed(p);
        p.attack(g);
        helper.assertTrue(g.getHealth() == hp, "a stone sword hurt it: " + g.getHealth());
        helper.assertFalse(g.isDispersed(), "a stone sword scattered it");
        helper.assertTrue(g.isAlive(), "it died");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void coldIronScattersIt(GameTestHelper helper) {
        night(helper);
        SNGameTests.floor(helper, 11, 11);
        GhostEntity g = ghost(helper, new BlockPos(5, 1, 6));
        ServerPlayer p = CurseTests.mortal(helper, MID, new ItemStack(Items.IRON_SWORD));
        CurseTests.armed(p);
        p.attack(g);
        helper.assertTrue(g.isDispersed(), "an iron sword should scatter it");
        helper.assertTrue(g.isAlive() && !g.isRemoved(), "scattered is not dead");
        helper.assertFalse(g.isPickable(), "a scattered ghost can still be hit");
        GhostEntity h = ghost(helper, new BlockPos(7, 1, 7));
        h.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.HOLY_WATER, null), 1f);
        helper.assertTrue(h.isDispersed(), "holy water should scatter it");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH, timeoutTicks = 300)
    public static void itGathersAgainAtItsBones(GameTestHelper helper) {
        night(helper);
        SNGameTests.floor(helper, 11, 11);
        GraveBonesBlockEntity be = bones(helper, MID);
        GhostEntity g = be.raiseGhost(helper.getLevel());
        helper.assertTrue(g != null, "no ghost");
        g.setNoAi(true);
        g.moveTo(g.getX() + 3, g.getY(), g.getZ());
        g.disperse();
        helper.assertTrue(g.isDispersed(), "not dispersed");
        helper.succeedWhen(() -> {
            helper.assertFalse(g.isDispersed(), "still scattered");
            helper.assertTrue(g.isAlive() && !g.isRemoved(), "it never came back");
            helper.assertTrue(g.position().distanceTo(Vec3.atCenterOf(helper.absolutePos(MID))) < 4, "it gathered far from its bones");
        });
    }

    // --- laying it to rest ------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void saltAndBurnLaysItToRest(GameTestHelper helper) {
        night(helper);
        SNGameTests.floor(helper, 11, 11);
        ServerLevel level = helper.getLevel();
        GraveBonesBlockEntity be = bones(helper, MID);
        BlockPos abs = helper.absolutePos(MID);
        GhostEntity g = be.raiseGhost(level);
        helper.assertTrue(g != null, "no ghost");
        g.setNoAi(true);
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(5, 1, 3), ItemStack.EMPTY);
        // Still buried: the salt can't reach.
        helper.setBlock(MID.above(), Blocks.DIRT);
        ItemStack salt = new ItemStack(AllItems.SALT.get(), 4);
        use(helper, p, salt, MID);
        helper.assertFalse(level.getBlockState(abs).getValue(GraveBonesBlock.SALTED), "buried bones were salted");
        helper.setBlock(MID.above(), Blocks.AIR);
        // Fire alone does nothing.
        use(helper, p, new ItemStack(Items.FLINT_AND_STEEL), MID);
        helper.assertFalse(level.getBlockState(abs).getValue(GraveBonesBlock.RESTED), "unsalted bones were burned");
        helper.assertFalse(g.isFading(), "fire alone freed the ghost");
        use(helper, p, salt, MID);
        helper.assertTrue(level.getBlockState(abs).getValue(GraveBonesBlock.SALTED), "salt did not take");
        helper.assertTrue(salt.getCount() == 3, "salting should cost one salt, left " + salt.getCount());
        use(helper, p, new ItemStack(Items.FLINT_AND_STEEL), MID);
        helper.assertTrue(level.getBlockState(abs).getValue(GraveBonesBlock.RESTED), "burned bones are not at rest");
        helper.assertTrue(g.isFading(), "the ghost did not start to fade");
        helper.succeedWhen(() -> {
            helper.assertTrue(g.isRemoved(), "the ghost is still here");
            helper.assertItemEntityPresent(AllItems.ECTOPLASM.get(), MID.above(), 2);
        });
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void layToRestFreesItsBones(GameTestHelper helper) {
        night(helper);
        SNGameTests.floor(helper, 11, 11);
        GraveBonesBlockEntity be = bones(helper, MID);
        GhostEntity g = be.raiseGhost(helper.getLevel());
        helper.assertTrue(g != null, "no ghost");
        g.setNoAi(true);
        g.moveTo(g.getX() + 2, g.getY(), g.getZ() - 1);
        g.layToRest();
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(MID)).getValue(GraveBonesBlock.RESTED), "its bones are not at rest");
        helper.succeedWhen(() -> helper.assertTrue(g.isRemoved(), "the ghost lingers"));
    }

    // --- salt and hauntings -----------------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void saltBarsItsWay(GameTestHelper helper) {
        night(helper);
        SNGameTests.floor(helper, 11, 11);
        for (int z = 0; z <= 4; z++) helper.setBlock(new BlockPos(5, 1, z), AllBlocks.SALT_LINE.get());
        GhostEntity barred = ghost(helper, new BlockPos(2, 1, 2));
        GhostEntity free = ghost(helper, new BlockPos(2, 1, 8));
        // Through a wall is fine.
        helper.setBlock(new BlockPos(4, 1, 8), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 2, 8), Blocks.STONE);
        for (int i = 0; i < 20; i++) {
            barred.move(MoverType.SELF, new Vec3(0.4, 0, 0));
            free.move(MoverType.SELF, new Vec3(0.4, 0, 0));
        }
        int line = helper.absolutePos(new BlockPos(5, 1, 0)).getX();
        helper.assertTrue(barred.getBoundingBox().maxX <= line + 1.0e-6, "it crossed the salt: x " + barred.getX() + ", line at " + line);
        helper.assertTrue(free.getX() > line + 2, "without salt it should pass (through stone too): x " + free.getX());
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void itSnuffsTheLights(GameTestHelper helper) {
        night(helper);
        SNGameTests.floor(helper, 11, 11);
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true));
        helper.setBlock(new BlockPos(7, 1, 7), Blocks.TORCH);
        GhostEntity g = ghost(helper, MID);
        int n = g.snuffLights(helper.absolutePos(MID));
        helper.assertTrue(n == 2, "expected 2 lights out, got " + n);
        helper.assertFalse(helper.getBlockState(new BlockPos(3, 1, 3)).getValue(CandleBlock.LIT), "the candle still burns");
        helper.assertBlockNotPresent(Blocks.TORCH, new BlockPos(7, 1, 7));
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void telekinesisThrowsAndHurts(GameTestHelper helper) {
        night(helper);
        SNGameTests.floor(helper, 11, 11);
        GhostEntity g = ghost(helper, new BlockPos(5, 1, 3));
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(5, 1, 7), ItemStack.EMPTY);
        float hp = p.getHealth();
        g.lashOut(p);
        helper.assertTrue(p.getHealth() < hp, "telekinesis did no harm");
        helper.assertTrue(p.getDeltaMovement().y > 0 && p.getDeltaMovement().z > 0, "it should throw you up and away: " + p.getDeltaMovement());
        helper.assertTrue(g.isManifest(), "it should show itself as it lashes out");
        helper.succeed();
    }
}
