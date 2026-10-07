package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.metatron.MetatronAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.metatron.MetatronEntity;
import org.papiricoh.supernaturalcraft.entity.boss.metatron.MetatronTerrain;
import org.papiricoh.supernaturalcraft.entity.boss.metatron.ScribeBookEntity;
import org.papiricoh.supernaturalcraft.entity.boss.metatron.ScribeHandEntity;
import org.papiricoh.supernaturalcraft.entity.boss.metatron.ScriptoriumLayout;
import org.papiricoh.supernaturalcraft.entity.boss.metatron.WordJudge;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.reward.AngelTabletItem;
import org.papiricoh.supernaturalcraft.ritual.WrittenNameIngredient;
import org.papiricoh.supernaturalcraft.ritual.block.RitualAltarBlockEntity;

import java.util.List;
import java.util.UUID;

/** Metatron: true health and quarters, the library and the lectern, his Hand and Book, the Tablet, his rite and his spoils. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class MetatronTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    private static MetatronEntity spawn(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        return helper.spawn(AllEntities.METATRON.get(), MID);
    }

    private static void smite(MetatronEntity m, ServerLevel level, float amount) {
        m.invulnerableTime = 0;
        m.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, null), amount);
    }

    private static ItemStack book(String text) {
        ItemStack book = new ItemStack(Items.WRITABLE_BOOK);
        book.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(List.of(Filterable.passThrough(text))));
        return book;
    }

    @GameTest(template = SNGameTests.ARENA, batch = "metatron_health", timeoutTicks = 60)
    public static void eighteenHundredTrueHealthAndALibrary(GameTestHelper helper) {
        MetatronEntity m = spawn(helper);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(Math.abs(m.trueMaxHealth() - 1800f) < 1f, "true health should be 1800, is " + m.trueMaxHealth());
            ArenaController arena = m.arena();
            helper.assertTrue(arena != null && arena.theme() == ArenaTheme.SCRIPTORIUM, "he should open a library arena");
            helper.assertTrue(m.maxPhase() == 4, "four phases");
            helper.assertTrue(m.libraryRaised(), "the library did not rise");
            long shelves = arena.placedBlocks().values().stream().filter(s -> s.is(Blocks.BOOKSHELF)).count();
            helper.assertTrue(shelves > 60, "too few shelves: " + shelves);
            BossTests.cleanup(helper);
            helper.assertTrue(arena.placedBlocks().isEmpty() || !arena.isActive(), "the library should go with the arena");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "metatron_threshold", timeoutTicks = 60)
    public static void thresholdsInQuarters(GameTestHelper helper) {
        MetatronEntity m = spawn(helper);
        helper.runAfterDelay(3, () -> {
            m.setHealth(m.getMaxHealth() * 0.77f);
            smite(m, helper.getLevel(), 500f);
            helper.assertTrue(Math.abs(m.getHealth() / m.getMaxHealth() - 0.75f) < 0.002f, "health should stop at three quarters");
            helper.assertTrue(m.phase() == 2 && m.state() == LuciferEntity.TRANSITION, "crossing it should begin phase 2");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "metatron_lectern", timeoutTicks = 200)
    public static void heTakesToHisLecternAndStays(GameTestHelper helper) {
        MetatronEntity m = spawn(helper);
        Vec3[] held = new Vec3[1];
        helper.runAfterDelay(3, () -> m.beginTransition(3));
        helper.runAfterDelay(LuciferEntity.TRANSITION_TICKS + 8, () -> {
            ServerLevel level = helper.getLevel();
            ArenaController arena = m.arena();
            helper.assertTrue(m.daisRaised(), "the dais did not rise");
            BlockPos c = arena.center();
            helper.assertTrue(level.getBlockState(c.offset(2, ScriptoriumLayout.DAIS_HEIGHT - 1, 2)).is(AllBlocks.SCRIPTURE_STONE.get()), "no dais top");
            helper.assertTrue(level.getBlockState(c.offset(0, ScriptoriumLayout.DAIS_HEIGHT - 1, -(ScriptoriumLayout.DAIS_HALF + 1)))
                    .is(Blocks.QUARTZ_STAIRS), "no stairs up the north side");
            helper.assertTrue(m.position().distanceTo(m.lecternSpot(arena)) < 0.5, "he should stand at his lectern, is at " + m.position());
            held[0] = m.position();
        });
        helper.runAfterDelay(LuciferEntity.TRANSITION_TICKS + 48, () -> {
            helper.assertTrue(m.position().distanceTo(held[0]) < 0.3, "he should not leave his lectern");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "metatron_constructs", timeoutTicks = 100)
    public static void hisHandAndBookComeWithTheirPhasesAndCannotBeHurt(GameTestHelper helper) {
        MetatronEntity m = spawn(helper);
        Object[] seen = new Object[2];
        helper.runAfterDelay(3, () -> m.forceLook(2));
        helper.runAfterDelay(20, () -> {
            ScribeHandEntity hand = m.hand();
            helper.assertTrue(hand != null, "no Hand in the second phase");
            helper.assertTrue(m.book() == null, "the Book came too early");
            helper.assertFalse(hand.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 100f), "the Hand can be hurt");
            seen[0] = hand;
            m.forceLook(3);
        });
        helper.runAfterDelay(40, () -> {
            ScribeBookEntity book = m.book();
            helper.assertTrue(book != null, "no Book in the third phase");
            helper.assertFalse(book.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 100f), "the Book can be hurt");
            seen[1] = book;
            m.discard();
        });
        helper.runAfterDelay(44, () -> {
            helper.assertTrue(((ScribeHandEntity) seen[0]).isRemoved() && ((ScribeBookEntity) seen[1]).isRemoved(), "the constructs should go with him");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "metatron_crush", timeoutTicks = 100)
    public static void theBookCrushesShelves(GameTestHelper helper) {
        MetatronEntity m = spawn(helper);
        helper.runAfterDelay(3, () -> m.forceLook(3));
        helper.runAfterDelay(20, () -> {
            ServerLevel level = helper.getLevel();
            ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, level);
            Vec3 at = helper.absoluteVec(MID.offset(6, 0, 6).getBottomCenter());
            stand.moveTo(at.x, at.y, at.z);
            level.addFreshEntity(stand);
            // Shelves standing round the target: whichever way the Book falls, they are under it.
            BlockPos feet = BlockPos.containing(at);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dz != 0) m.arena().mutate(level, feet.offset(dx, 0, dz), Blocks.BOOKSHELF.defaultBlockState(), 0);
                }
            }
            MetatronAttacks.TomeCrush crush = new MetatronAttacks.TomeCrush();
            crush.onWindup(m, stand);
            crush.onActive(m, stand);
            crush.tickActive(m, stand, 4);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    helper.assertFalse(level.getBlockState(feet.offset(dx, 0, dz)).is(Blocks.BOOKSHELF),
                            "the Book should splinter the shelves it falls on (" + dx + "," + dz + ")");
                }
            }
            crush.onEnd(m);
            stand.discard();
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "metatron_bound", timeoutTicks = 60)
    public static void theBookHoldsAHunterUntilHeIsWounded(GameTestHelper helper) {
        MetatronEntity m = spawn(helper);
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            ServerPlayer p = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "sn-test-bound"));
            m.trapInBook(p);
            helper.assertTrue(p.getUUID().equals(m.trapped()) && p.hasEffect(AllMobEffects.STUNNED), "the Book should hold the hunter");
            m.invulnerableTime = 0;
            m.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, p), 30f);
            helper.assertTrue(m.trapped() == null, "thirty in wounds should open the Book");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "metatron_word", timeoutTicks = 60)
    public static void theWordPunishesTheDisobedient(GameTestHelper helper) {
        MetatronEntity m = spawn(helper);
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            IronGolem golem = EntityType.IRON_GOLEM.create(level);
            Vec3 at = helper.absoluteVec(MID.offset(5, 0, 0).getBottomCenter());
            golem.moveTo(at.x, at.y, at.z);
            golem.setNoAi(true);
            level.addFreshEntity(golem);
            float before = golem.getHealth();
            golem.invulnerableTime = 0;
            MetatronAttacks.TheWord.punish(m, golem, WordJudge.Order.BE_STILL);
            helper.assertTrue(golem.getHealth() < before - 5, "breaking the Word should hurt");
            helper.assertTrue(golem.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "breaking BE STILL should slow");
            golem.discard();
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "metatron_rewrite", timeoutTicks = 60)
    public static void theTabletRewritesTheGroundAwayFromTheDais(GameTestHelper helper) {
        MetatronEntity m = spawn(helper);
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            ArenaController arena = m.arena();
            int before = arena.placedBlocks().size();
            int changed = MetatronTerrain.rewrite(level, arena, level.getRandom());
            helper.assertTrue(changed > 4 && arena.placedBlocks().size() > before, "the ground was not rewritten");
            BlockPos c = arena.center();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    if (!ScriptoriumLayout.daisFootprint(dx, dz)) continue;
                    for (int y = 0; y <= 3; y++) {
                        helper.assertFalse(level.getBlockState(c.offset(dx, y, dz)).is(AllBlocks.SCRIPTURE_STONE.get()),
                                "a rewrite touched the dais's ground at " + dx + "," + dz);
                    }
                }
            }
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "metatron_death", timeoutTicks = 400)
    public static void deathLeavesTheTablet(GameTestHelper helper) {
        MetatronEntity m = spawn(helper);
        helper.runAfterDelay(3, () -> m.beginTransition(4));
        helper.runAfterDelay(5, () -> m.setHealth(3f));
        helper.runAfterDelay(LuciferEntity.TRANSITION_TICKS + 8, () -> {
            smite(m, helper.getLevel(), 50f);
            helper.assertTrue(m.isAlive() && m.state() == LuciferEntity.DYING, "the killing blow should start his death");
        });
        helper.runAfterDelay(LuciferEntity.TRANSITION_TICKS + 8 + MetatronEntity.METATRON_DEATH_TICKS + 10, () -> {
            helper.assertTrue(m.isRemoved(), "he is still here after his death");
            boolean tablet = false, trophy = false;
            for (ItemEntity e : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(MID)).inflate(40))) {
                tablet |= e.getItem().is(AllItems.ANGEL_TABLET.get());
                trophy |= e.getItem().is(AllItems.METATRON_TROPHY.get());
                e.discard();
            }
            helper.assertTrue(tablet && trophy, "the Tablet and his bust should fall");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.SMALL, batch = "metatron_tablet", timeoutTicks = 20)
    public static void theTabletRewritesItsHolder(GameTestHelper helper) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "sn-test-tablet"));
        p.setHealth(4f);
        p.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
        p.setRemainingFireTicks(100);
        AngelTabletItem.rewrite(p);
        helper.assertTrue(p.getHealth() == p.getMaxHealth(), "the Tablet should heal whole");
        helper.assertFalse(p.hasEffect(MobEffects.POISON), "harm should be undone");
        helper.assertTrue(p.hasEffect(MobEffects.MOVEMENT_SPEED), "blessings should stay");
        helper.assertFalse(p.isOnFire(), "the fire should be put out");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "metatron_name", timeoutTicks = 20)
    public static void onlyHisWrittenNameWillDo(GameTestHelper helper) {
        WrittenNameIngredient name = new WrittenNameIngredient("Metatron");
        helper.assertTrue(name.test(book("I call upon Metatron, scribe of God")), "his name should do");
        helper.assertFalse(name.test(book("I call upon somebody")), "another name should not");
        helper.assertFalse(name.test(new ItemStack(Items.WRITABLE_BOOK)), "an empty book should not");
        helper.assertFalse(name.test(new ItemStack(Items.PAPER)), "paper is not a book");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "metatron_rite", timeoutTicks = 60)
    public static void hisRiteNeedsLuciferBeaten(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.setDayTime(18000);
        HellTests.draw(helper, HellTests.GREAT_CIRCLE);
        ServerPlayer p = HellTests.ritualist(helper, "sn-test-metatron");
        RitualAltarBlockEntity altar = (RitualAltarBlockEntity) helper.getBlockEntity(new BlockPos(5, 1, 5));
        helper.runAfterDelay(1, () -> {
            HellTests.offer(altar, p, book("Metatron"), new ItemStack(AllItems.CHOIR_SHARD.get()), new ItemStack(AllItems.CHOIR_SHARD.get()),
                    new ItemStack(AllItems.HOLY_WATER.get()), new ItemStack(AllItems.HOLY_WATER.get()), new ItemStack(Items.FEATHER),
                    new ItemStack(Items.FEATHER), new ItemStack(Items.GLOW_INK_SAC));
            HellTests.offer(altar, p, new ItemStack(Items.FLINT_AND_STEEL));
            helper.assertFalse(altar.isChanneling(), "his rite started for someone who never beat Lucifer");
            HellTests.award(p, "main/devil_went_down");
            HellTests.offer(altar, p, new ItemStack(Items.FLINT_AND_STEEL));
            helper.assertTrue(altar.isChanneling(), "his rite should start once Lucifer is beaten");
            helper.setBlock(new BlockPos(5, 1, 5), Blocks.AIR.defaultBlockState());
            level.setDayTime(6000);
            helper.succeed();
        });
    }
}
