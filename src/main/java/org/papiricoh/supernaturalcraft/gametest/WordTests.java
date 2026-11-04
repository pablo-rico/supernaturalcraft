package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.crypt.SigilPlateBlock;
import org.papiricoh.supernaturalcraft.hunter.DevilsTrapBlock;
import org.papiricoh.supernaturalcraft.hunter.wall.AlarmSigilBlock;
import org.papiricoh.supernaturalcraft.hunter.wall.BanishingSigilBlock;
import org.papiricoh.supernaturalcraft.hunter.wall.WallSigilBlock;
import org.papiricoh.supernaturalcraft.hunter.wall.WardIndex;
import org.papiricoh.supernaturalcraft.interrogation.Interrogation;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;
import org.papiricoh.supernaturalcraft.magic.spell.SpellContext;
import org.papiricoh.supernaturalcraft.magic.spell.effect.WordEffects;
import org.papiricoh.supernaturalcraft.network.TabletGuessPayload;
import org.papiricoh.supernaturalcraft.prophecy.Prophecies;
import org.papiricoh.supernaturalcraft.prophecy.Tablet;
import org.papiricoh.supernaturalcraft.prophecy.TabletReading;
import org.papiricoh.supernaturalcraft.prophecy.WordCipher;
import org.papiricoh.supernaturalcraft.prophecy.trials.TrialRules;
import org.papiricoh.supernaturalcraft.prophecy.trials.Trials;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.SNRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** The Word and the Ward (v0.36): traps on ceilings and under rugs, unlit oil, wall sigils, the tablets, interrogation, the Trials. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class WordTests {

    static ServerPlayer player(GameTestHelper helper, ItemStack held) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "word_test"));
        p.setGameMode(GameType.SURVIVAL);
        p.setItemInHand(InteractionHand.MAIN_HAND, held);
        return p;
    }

    private static void trap(GameTestHelper helper, BlockPos center, BlockState base) {
        for (int part = 0; part < 9; part++) {
            helper.setBlock(center.offset(DevilsTrapBlock.dx(part), 0, DevilsTrapBlock.dz(part)), base.setValue(DevilsTrapBlock.PART, part));
        }
    }

    /** A devil's trap on a ceiling holds a demon standing four blocks below it. */
    @GameTest(template = SNGameTests.MEDIUM, timeoutTicks = 60)
    public static void ceilingTrapHoldsTheColumnBelow(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        for (int x = 3; x <= 7; x++) for (int z = 3; z <= 7; z++) helper.setBlock(new BlockPos(x, 5, z), Blocks.STONE.defaultBlockState());
        trap(helper, new BlockPos(5, 4, 5), AllBlocks.DEVILS_TRAP.get().defaultBlockState().setValue(DevilsTrapBlock.FACE, DevilsTrapBlock.Face.CEILING));
        Mob demon = helper.spawn(AllEntities.BLACK_EYED_DEMON.get(), new BlockPos(4, 1, 5));
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(demon.hasEffect(AllMobEffects.TRAPPED), "demon under the ceiling trap is not held");
            helper.succeed();
        });
    }

    /** A trap hidden under a rug still holds. */
    @GameTest(template = SNGameTests.MEDIUM, timeoutTicks = 40)
    public static void coveredTrapStillHolds(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        trap(helper, new BlockPos(5, 1, 5), AllBlocks.DEVILS_TRAP.get().defaultBlockState().setValue(DevilsTrapBlock.COVER, DevilsTrapBlock.Cover.RED));
        Mob demon = helper.spawn(AllEntities.BLACK_EYED_DEMON.get(), new BlockPos(5, 1, 5));
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(demon.hasEffect(AllMobEffects.TRAPPED), "demon on the covered trap is not held");
            helper.succeed();
        });
    }

    /** Unlit holy oil catches all along its line when a redstone signal reaches it. */
    @GameTest(template = SNGameTests.MEDIUM, timeoutTicks = 40)
    public static void unlitOilCatchesOnRedstone(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        for (int x = 2; x <= 8; x++) helper.setBlock(new BlockPos(x, 1, 5), AllBlocks.HOLY_OIL_TRAIL.get().defaultBlockState());
        helper.runAfterDelay(2, () -> helper.setBlock(new BlockPos(1, 1, 5), Blocks.REDSTONE_BLOCK.defaultBlockState()));
        helper.runAfterDelay(10, () -> {
            for (int x = 2; x <= 8; x++) helper.assertBlockPresent(AllBlocks.HOLY_OIL_FIRE.get(), new BlockPos(x, 1, 5));
            helper.succeed();
        });
    }

    /** Slamming a banishing sigil sends every (non-boss) angel round it away, and spends the sigil. */
    @GameTest(template = SNGameTests.MEDIUM, timeoutTicks = 40)
    public static void banishingSigilSendsAngelsAway(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        helper.setBlock(new BlockPos(5, 1, 6), Blocks.STONE.defaultBlockState());
        BlockPos sigil = new BlockPos(5, 1, 5);
        helper.setBlock(sigil, AllBlocks.BANISHING_SIGIL.get().defaultBlockState().setValue(WallSigilBlock.FACING, Direction.NORTH));
        Mob angel = helper.spawn(AllEntities.ANGEL.get(), new BlockPos(2, 1, 2));
        helper.runAfterDelay(5, () -> {
            int sent = BanishingSigilBlock.blast(helper.getLevel(), helper.absolutePos(sigil), player(helper, ItemStack.EMPTY));
            helper.assertTrue(sent == 1 && angel.isRemoved(), "the angel was not banished (" + sent + ")");
            helper.assertBlockNotPresent(AllBlocks.BANISHING_SIGIL.get(), sigil);
            helper.succeed();
        });
    }

    /** A painted ward guards its sphere, and stops guarding when it is scraped off. */
    @GameTest(template = SNGameTests.MEDIUM, timeoutTicks = 40)
    public static void wardGuardsItsSphere(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        BlockPos ward = new BlockPos(5, 1, 5);
        helper.setBlock(ward, AllBlocks.ENOCHIAN_WARD.get().defaultBlockState().setValue(WallSigilBlock.FACING, Direction.UP));
        var level = helper.getLevel();
        var inside = helper.absoluteVec(new net.minecraft.world.phys.Vec3(8.5, 1, 5.5));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(WardIndex.warded(level, inside), "three blocks from the ward is not warded");
            helper.assertFalse(WardIndex.warded(level, inside.add(20, 0, 0)), "twenty blocks away is warded");
            helper.setBlock(ward, Blocks.AIR.defaultBlockState());
        });
        helper.runAfterDelay(4, () -> {
            helper.assertFalse(WardIndex.warded(level, inside), "the ward still guards after it was scraped off");
            helper.succeed();
        });
    }

    /** An alarm sigil lights and gives a redstone signal while a demon is near. */
    @GameTest(template = SNGameTests.MEDIUM, timeoutTicks = 60)
    public static void alarmSigilSignalsADemon(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        BlockPos alarm = new BlockPos(5, 1, 5);
        helper.setBlock(alarm, AllBlocks.ALARM_SIGIL.get().defaultBlockState().setValue(WallSigilBlock.FACING, Direction.UP));
        helper.spawn(AllEntities.BLACK_EYED_DEMON.get(), new BlockPos(2, 1, 2));
        helper.runAfterDelay(30, () -> {
            BlockState s = helper.getBlockState(alarm);
            helper.assertTrue(s.getValue(AlarmSigilBlock.LIT), "the alarm did not light");
            helper.assertTrue(helper.getLevel().getBestNeighborSignal(helper.absolutePos(alarm.east())) == 15, "no redstone signal");
            helper.succeed();
        });
    }

    /** Painting the right sigil on a crypt plate opens the sealed door beside it; the wrong one does nothing. */
    @GameTest(template = SNGameTests.MEDIUM, timeoutTicks = 40)
    public static void sigilPlateOpensItsDoor(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        BlockPos plate = new BlockPos(3, 1, 5);
        helper.setBlock(plate, AllBlocks.SIGIL_PLATE.get().defaultBlockState().setValue(SigilPlateBlock.FACING, Direction.SOUTH)
                .setValue(SigilPlateBlock.SIGIL, SigilPlateBlock.Sigil.WARD));
        for (int y = 1; y <= 3; y++) for (int x = 5; x <= 7; x++) helper.setBlock(new BlockPos(x, y, 5), AllBlocks.SEALED_CRYPT_DOOR.get().defaultBlockState());
        helper.runAfterDelay(2, () -> helper.setBlock(plate.south(), AllBlocks.ALARM_SIGIL.get().defaultBlockState().setValue(WallSigilBlock.FACING, Direction.SOUTH)));
        helper.runAfterDelay(5, () -> {
            helper.assertBlockPresent(AllBlocks.SEALED_CRYPT_DOOR.get(), new BlockPos(6, 2, 5));
            helper.setBlock(plate.south(), AllBlocks.ENOCHIAN_WARD.get().defaultBlockState().setValue(WallSigilBlock.FACING, Direction.SOUTH));
        });
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(helper.getBlockState(plate).getValue(SigilPlateBlock.SOLVED), "the plate was not solved");
            helper.assertBlockNotPresent(AllBlocks.SEALED_CRYPT_DOOR.get(), new BlockPos(6, 2, 5));
            helper.succeed();
        });
    }

    /** The right reading of a chapter of the Demon Tablet reads it, teaches its letters and its sigil. */
    @GameTest(template = SNGameTests.SMALL, timeoutTicks = 20)
    public static void readingAChapterTeachesItsSigil(GameTestHelper helper) {
        // A mortal one: FakePlayers are invulnerable, and the wrong reading must hurt.
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(2, 1, 2), new ItemStack(AllItems.DEMON_TABLET.get()));
        int[] perm = WordCipher.permutation(WordCipher.seed(helper.getLevel().getSeed(), p.getUUID(), Tablet.DEMON));
        int[] inv = WordCipher.inverse(perm);
        List<Integer> wrong = new ArrayList<>(), right = new ArrayList<>();
        for (int g = 0; g < 26; g++) {
            wrong.add(-1);
            right.add(inv[g]);
        }
        float before = p.getHealth();
        TabletReading.guess(p, new TabletGuessPayload(Tablet.DEMON.ordinal() * 16 + 1, wrong));
        helper.assertTrue(Prophecies.read(p, Tablet.DEMON) == 0 && p.getHealth() < before, "a blank reading read the chapter or did not hurt");
        helper.runAfterDelay(5, () -> {
            TabletReading.guess(p, new TabletGuessPayload(Tablet.DEMON.ordinal() * 16 + 1, right));
            helper.assertTrue(Prophecies.read(p, Tablet.DEMON) == 1, "the right reading did not read the chapter");
            helper.assertTrue(ManaManager.get(p).knows(SupernaturalCraft.asResource("mass_exorcism")), "the chapter did not teach Mass Exorcism");
            int letters = WordCipher.lettersOf(WordCipher.phrase(Tablet.DEMON, 1));
            helper.assertTrue((Prophecies.get(p).known(Tablet.DEMON) & letters) == letters, "the chapter's letters were not learnt");
            helper.succeed();
        });
    }

    /** A press is taken only on a demon held by a trap. */
    @GameTest(template = SNGameTests.MEDIUM, timeoutTicks = 20)
    public static void onlyATrappedDemonIsInterrogated(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        Mob free = helper.spawn(AllEntities.BLACK_EYED_DEMON.get(), new BlockPos(2, 1, 2));
        Mob held = helper.spawn(AllEntities.BLACK_EYED_DEMON.get(), new BlockPos(8, 1, 8));
        held.addEffect(new MobEffectInstance(AllMobEffects.TRAPPED, 200));
        ServerPlayer p = player(helper, new ItemStack(AllItems.HOLY_WATER.get(), 4));
        helper.assertFalse(Interrogation.press(p, free, InteractionHand.MAIN_HAND), "a free demon was interrogated");
        float health = held.getHealth();
        helper.assertTrue(Interrogation.press(p, held, InteractionHand.MAIN_HAND), "a held demon was not interrogated");
        helper.assertTrue(held.getHealth() < health && p.getMainHandItem().getCount() == 3, "holy water did not burn it, or was not spent");
        helper.succeed();
    }

    /** The first trial takes two hearts. */
    @GameTest(template = SNGameTests.SMALL, timeoutTicks = 20)
    public static void theTrialsTakeHearts(GameTestHelper helper) {
        ServerPlayer p = player(helper, ItemStack.EMPTY);
        float max = p.getMaxHealth();
        Trials.offer(p);
        helper.assertTrue(Prophecies.get(p).trial() == TrialRules.HOUND, "the first trial did not begin");
        helper.assertTrue(Math.abs(max - p.getMaxHealth() - 2 * TrialRules.HEARTS_PER_TRIAL) < 0.01, "max health " + max + " -> " + p.getMaxHealth());
        helper.succeed();
    }

    /** Seal in a Jar shuts a monster in an empty Purgatory jar the caster carries. */
    @GameTest(template = SNGameTests.MEDIUM, timeoutTicks = 20)
    public static void sealInAJar(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        Mob zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(5, 1, 5));
        ServerPlayer p = player(helper, new ItemStack(AllItems.PURGATORY_JAR.get()));
        SigilComponent sigil = helper.getLevel().registryAccess().registryOrThrow(SNRegistries.SIGIL).get(SupernaturalCraft.asResource("seal_in_jar"));
        helper.assertTrue(sigil != null, "no seal_in_jar sigil");
        boolean done = WordEffects.SEAL_IN_JAR.applyToEntity(new SpellContext(helper.getLevel(), p), sigil, zombie);
        helper.assertTrue(done && zombie.isRemoved(), "the zombie was not sealed");
        helper.assertTrue(p.getInventory().items.stream().anyMatch(s -> s.is(AllItems.PURGATORY_JAR_FULL.get())
                && s.has(org.papiricoh.supernaturalcraft.registry.AllDataComponents.JAR_SOUL.get())), "no full jar with its soul");
        helper.succeed();
    }
}
