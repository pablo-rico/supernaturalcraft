package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.BowlContents;
import org.papiricoh.supernaturalcraft.bowl.BowlLiquid;
import org.papiricoh.supernaturalcraft.bowl.Dose;
import org.papiricoh.supernaturalcraft.bowl.SpellBowlBlockEntity;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.DepthRules;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.ritual.block.RitualAltarBlockEntity;
import org.papiricoh.supernaturalcraft.ritual.effect.DeepenWellEffect;
import org.papiricoh.supernaturalcraft.util.WorldClock;

import java.util.List;
import java.util.UUID;

/** The rites of the well: the Deepening at the altar, the Wellspring in the bowl, their tiers, rests and whole-bar cost. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class WellTests {

    private static final BlockPos ALTAR = new BlockPos(5, 1, 5);
    private static final BlockPos BOWL = new BlockPos(2, 1, 2);
    private static final BlockPos STAND = new BlockPos(3, 1, 2);

    private static ServerPlayer hunter(GameTestHelper helper, String name) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.setGameMode(GameType.SURVIVAL);
        Vec3 v = helper.absoluteVec(STAND.getBottomCenter());
        p.moveTo(v.x, v.y, v.z, 0, 0);
        fill(p);
        return p;
    }

    private static void fill(ServerPlayer p) {
        ArcanaData data = ManaManager.get(p);
        data.setMana(data.maxMana());
    }

    /** A plain chalk square round the altar: the Deepening's first circle. */
    private static RitualAltarBlockEntity smallCircle(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        helper.setBlock(ALTAR, AllBlocks.RITUAL_ALTAR.get().defaultBlockState());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) helper.setBlock(ALTAR.offset(dx, 0, dz), AllBlocks.CHALK_LINE.get().defaultBlockState());
            }
        }
        return (RitualAltarBlockEntity) helper.getBlockEntity(ALTAR);
    }

    private static void offerAndLight(RitualAltarBlockEntity altar, ServerPlayer p, ItemStack... stacks) {
        for (ItemStack s : stacks) {
            p.setItemInHand(InteractionHand.MAIN_HAND, s);
            altar.onUse(p, InteractionHand.MAIN_HAND, s);
        }
        ItemStack flint = new ItemStack(Items.FLINT_AND_STEEL);
        p.setItemInHand(InteractionHand.MAIN_HAND, flint);
        altar.onUse(p, InteractionHand.MAIN_HAND, flint);
    }

    private static ItemStack[] firstDeepening() {
        return new ItemStack[]{new ItemStack(Items.AMETHYST_SHARD), new ItemStack(AllItems.ENOCHIAN_INK.get())};
    }

    @GameTest(template = SNGameTests.MEDIUM)
    public static void deepeningTakesTheWholeBarAndAFullOne(GameTestHelper helper) {
        RitualAltarBlockEntity altar = smallCircle(helper);
        ServerPlayer p = hunter(helper, "sn-test-deepen");
        ManaManager.get(p).setMana(60);
        offerAndLight(altar, p, firstDeepening());
        helper.assertFalse(altar.isChanneling(), "a half-full bar started the rite");
        helper.assertTrue(ManaManager.get(p).mana() == 60, "a refused rite took mana");
        fill(p);
        ItemStack flint = new ItemStack(Items.FLINT_AND_STEEL);
        p.setItemInHand(InteractionHand.MAIN_HAND, flint);
        altar.onUse(p, InteractionHand.MAIN_HAND, flint);
        helper.assertTrue(altar.isChanneling(), "the first Deepening did not start on a full bar");
        helper.assertTrue(ManaManager.get(p).mana() == 0, "the rite should take the whole bar, left " + ManaManager.get(p).mana());
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM)
    public static void deepeningWidensTheBarThenRests(GameTestHelper helper) {
        ServerPlayer p = hunter(helper, "sn-test-deepen-rest");
        BlockPos at = helper.absolutePos(ALTAR);
        DeepenWellEffect first = new DeepenWellEffect(DeepenWellEffect.Kind.DEEPENING, 1);
        helper.assertTrue(first.precheck(helper.getLevel(), at, p) == null, "a full, fresh hunter was refused");
        helper.assertTrue(first.perform(helper.getLevel(), at, p), "the rite did not take");
        ArcanaData data = ManaManager.get(p);
        helper.assertTrue(data.rites(DepthRules.Kind.DEEPENING) == 1, "one Deepening counted");
        helper.assertTrue(data.maxMana() == ArcanaData.BASE_MAX_MANA + DepthRules.MANA_PER_DEEPENING, "max mana " + data.maxMana());
        long now = WorldClock.day(helper.getLevel().getServer());
        helper.assertTrue(data.nextRite(DepthRules.Kind.DEEPENING) == now + DepthRules.TICKS_PER_DAY, "the well rests a day after the first");
        fill(p);
        helper.assertTrue(first.precheck(helper.getLevel(), at, p) != null, "a resting well took a second rite");
        // The Wellspring rests on its own.
        DeepenWellEffect spring = new DeepenWellEffect(DeepenWellEffect.Kind.WELLSPRING, 1);
        helper.assertTrue(spring.precheck(helper.getLevel(), at, p) == null, "the Deepening's rest held the Wellspring");
        data.setNextRite(DepthRules.Kind.DEEPENING, now);
        helper.assertTrue(first.precheck(helper.getLevel(), at, p) == null, "the rest is over");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM)
    public static void eachTierWantsItsOwnRites(GameTestHelper helper) {
        ServerPlayer p = hunter(helper, "sn-test-deepen-tier");
        BlockPos at = helper.absolutePos(ALTAR);
        ArcanaData data = ManaManager.get(p);
        DeepenWellEffect second = new DeepenWellEffect(DeepenWellEffect.Kind.DEEPENING, 2);
        helper.assertTrue(second.precheck(helper.getLevel(), at, p) != null, "a fresh hunter did the doubled rite");
        data.setRites(DepthRules.Kind.DEEPENING, 5);
        fill(p);
        helper.assertTrue(second.precheck(helper.getLevel(), at, p) == null, "the sixth rite is the doubled one");
        helper.assertTrue(new DeepenWellEffect(DeepenWellEffect.Kind.DEEPENING, 1).precheck(helper.getLevel(), at, p) != null,
                "the plain rite still took after the fifth");
        data.setRites(DepthRules.Kind.DEEPENING, 40);
        fill(p);
        helper.assertTrue(new DeepenWellEffect(DeepenWellEffect.Kind.DEEPENING, 3).precheck(helper.getLevel(), at, p) == null,
                "the third tier repeats for ever");
        helper.assertTrue(data.maxMana() == ArcanaData.BASE_MAX_MANA + 40 * DepthRules.MANA_PER_DEEPENING, "forty Deepenings: " + data.maxMana());
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, timeoutTicks = 60)
    public static void wellspringIsSpokenOverTheBowl(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        helper.setBlock(BOWL, AllBlocks.SPELL_BOWL.get().defaultBlockState());
        SpellBowlBlockEntity bowl = (SpellBowlBlockEntity) helper.getBlockEntity(BOWL);
        ServerPlayer p = hunter(helper, "sn-test-wellspring");
        ArcanaData data = ManaManager.get(p);
        bowl.setContents(BowlContents.of(List.of(new ItemStack(AllItems.CHOIR_SHARD.get())), List.of(Dose.of(BowlLiquid.HOLY_WATER))));
        ItemStack flint = new ItemStack(Items.FLINT_AND_STEEL);
        p.setItemInHand(InteractionHand.MAIN_HAND, flint);
        helper.assertTrue(bowl.tryLight(p, flint, InteractionHand.MAIN_HAND) == SpellBowlBlockEntity.LightResult.UNKNOWN,
                "the Wellspring was spoken without its page");
        data.learnRite(SupernaturalCraft.asResource("wellspring"));
        data.setMana(40);
        helper.assertTrue(bowl.tryLight(p, flint, InteractionHand.MAIN_HAND) == SpellBowlBlockEntity.LightResult.BLOCKED,
                "the bowl caught on a bar that was not full");
        fill(p);
        helper.assertTrue(bowl.tryLight(p, flint, InteractionHand.MAIN_HAND) == SpellBowlBlockEntity.LightResult.LIT, "the bowl did not catch");
        helper.assertTrue(data.mana() == 0, "the Wellspring should take the whole bar");
        float before = data.regenPerTick();
        helper.runAfterDelay(20, () -> {
            SpellBowlBlockEntity.Outcome o = bowl.resolveRecitation(p, true, 0);
            helper.assertTrue(o == SpellBowlBlockEntity.Outcome.CAST, "expected the Wellspring to take, got " + o);
            helper.assertTrue(data.rites(DepthRules.Kind.WELLSPRING) == 1, "one Wellspring counted");
            float expected = DepthRules.regenPerSecond(data.maxMana(), 1) / 20f;
            helper.assertTrue(Math.abs(data.regenPerTick() - expected) < 1e-5 && data.regenPerTick() > before,
                    "regen " + data.regenPerTick() + " per tick, expected " + expected);
            helper.succeed();
        });
    }
}
