package org.papiricoh.supernaturalcraft.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.BossDamage;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.reward.ColtItem;
import org.papiricoh.supernaturalcraft.reward.colt.ColtReload;
import org.papiricoh.supernaturalcraft.reward.colt.ColtShot;
import org.papiricoh.supernaturalcraft.ritual.RitualConditions;
import org.papiricoh.supernaturalcraft.ritual.RitualRecipe;
import org.papiricoh.supernaturalcraft.ritual.effect.CraftItemEffect;

import java.util.List;

/** The Colt executes the lesser, hits bosses for exactly 60 past their caps, and is fed eight rounds at a time. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class ColtTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);
    private static final float SHOT = 60f;

    private static ItemStack colt(int rounds) {
        ItemStack s = new ItemStack(AllItems.THE_COLT.get());
        s.set(AllDataComponents.COLT_AMMO, rounds);
        return s;
    }

    private static LuciferEntity lucifer(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        return helper.spawn(AllEntities.LUCIFER.get(), MID);
    }

    @GameTest(template = SNGameTests.MEDIUM)
    public static void executesEveryTaggedKind(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<EntityType<?>> kinds = List.of(AllEntities.BLACK_EYED_DEMON.get(), AllEntities.DEMON_OCCULTIST.get(),
                AllEntities.AMARA_SHADE.get(), AllEntities.CHOIR_ECHO.get(), AllEntities.LUCIFER_ILLUSION.get());
        int x = 2;
        for (EntityType<?> kind : kinds) {
            var e = helper.spawn(kind, new BlockPos(x, 2, 4));
            x += 2;
            ColtShot.Outcome out = ColtShot.strike(level, null, e);
            helper.assertTrue(out == ColtShot.Outcome.EXECUTED, kind.getDescriptionId() + " was not executed: " + out);
            helper.assertTrue(!e.isAlive() || e.isRemoved(), kind.getDescriptionId() + " survived a consecrated round");
        }
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM)
    public static void ordinaryMobTakesTwenty(GameTestHelper helper) {
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(4, 2, 4));
        float before = golem.getHealth();
        ColtShot.Outcome out = ColtShot.strike(helper.getLevel(), null, golem);
        helper.assertTrue(out == ColtShot.Outcome.OTHER, "a golem is neither boss nor demon: " + out);
        helper.assertTrue(Math.abs(before - golem.getHealth() - 20) < 0.01f, "expected 20, dealt " + (before - golem.getHealth()));
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "colt_lucifer", timeoutTicks = 60)
    public static void luciferTakesSixtyPastTheCap(GameTestHelper helper) {
        LuciferEntity l = lucifer(helper);
        helper.runAfterDelay(2, () -> {
            // A mundane blow first: its i-frames must not eat into the round.
            l.hurt(helper.getLevel().damageSources().magic(), 10f);
            float before = l.getHealth();
            ColtShot.Outcome out = ColtShot.strike(helper.getLevel(), null, l);
            helper.assertTrue(out == ColtShot.Outcome.BOSS, "Lucifer is a boss: " + out);
            helper.assertTrue(Math.abs(before - l.getHealth() - SHOT) < 0.01f, "expected 60 past the cap of 40, dealt " + (before - l.getHealth()));
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "colt_lucifer_floor", timeoutTicks = 60)
    public static void luciferStopsAtThreshold(GameTestHelper helper) {
        LuciferEntity l = lucifer(helper);
        helper.runAfterDelay(2, () -> {
            l.setHealth(l.getMaxHealth() * 0.75f + 30);
            ColtShot.strike(helper.getLevel(), null, l);
            helper.assertTrue(Math.abs(l.getHealth() - l.getMaxHealth() * 0.75f) < 0.01f,
                    "a round must stop at the threshold, health at " + l.getHealth() / l.getMaxHealth());
            helper.assertTrue(l.phase() == 2 && l.state() == LuciferEntity.TRANSITION, "crossing 75% should begin phase two");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "colt_ray", timeoutTicks = 60)
    public static void realShotHitsLuciferAndSpendsARound(GameTestHelper helper) {
        LuciferEntity l = lucifer(helper);
        helper.runAfterDelay(2, () -> {
            ItemStack gun = colt(ColtItem.CAPACITY);
            // Eight blocks north of him, aiming at his chest.
            ServerPlayer p = ArsenalTests.fighter(helper, MID.north(8), gun);
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, l.getBoundingBox().getCenter());
            float before = l.getHealth();
            ColtShot.Outcome out = ColtItem.fire(p, gun);
            helper.assertTrue(out == ColtShot.Outcome.BOSS, "the round should have struck Lucifer: " + out);
            helper.assertTrue(Math.abs(before - l.getHealth() - SHOT) < 0.01f, "expected 60, dealt " + (before - l.getHealth()));
            helper.assertTrue(ColtItem.rounds(gun) == ColtItem.CAPACITY - 1, "a round should be spent, " + ColtItem.rounds(gun) + " left");
            helper.assertTrue(ColtItem.chamber(gun) == 1, "the cylinder should have turned to chamber 1");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "colt_amara", timeoutTicks = 60)
    public static void amarasPartsAndCoreTakeSixty(GameTestHelper helper) {
        AmaraEntity a = AmaraTests.summon(helper);
        ServerLevel level = helper.getLevel();
        var anchor = a.part(AmaraEntity.FIRST_ANCHOR);
        helper.assertTrue(BossDamage.isBoss(anchor), "her parts should count as the boss");
        float partBefore = a.partHealth(AmaraEntity.FIRST_ANCHOR);
        ColtShot.Outcome out = ColtShot.strike(level, null, anchor);
        helper.assertTrue(out == ColtShot.Outcome.BOSS, "an anchor is part of a boss: " + out);
        float dealt = partBefore - a.partHealth(AmaraEntity.FIRST_ANCHOR);
        helper.assertTrue(Math.abs(dealt - Math.min(SHOT, partBefore)) < 0.01f || !a.partAlive(AmaraEntity.FIRST_ANCHOR),
                "the anchor should lose exactly 60 (no holy bonus), lost " + dealt);
        AmaraTests.breakAnchors(helper, a);
        float before = a.getHealth();
        ColtShot.strike(level, null, a.part(AmaraEntity.CORE));
        // Under her own eclipse, still exactly 60: exact damage skips the eclipse's holy bonus too.
        helper.assertTrue(Math.abs(before - a.getHealth() - SHOT) < 0.01f, "the core should take exactly 60, took " + (before - a.getHealth()));
        a.setHealth(a.getMaxHealth() * 0.7f + 20);
        ColtShot.strike(level, null, a.part(AmaraEntity.CORE));
        helper.assertTrue(Math.abs(a.getHealth() - a.getMaxHealth() * 0.7f) < 0.5f, "the round must stop at her threshold, health " + a.getHealth());
        AmaraTests.finish(helper);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "colt_chorus", timeoutTicks = 60)
    public static void chorusPartTakesSixty(GameTestHelper helper) {
        ChorusEntity c = ChorusTests.summon(helper);
        helper.runAfterDelay(2, () -> {
            ServerLevel level = helper.getLevel();
            var face = c.part(ChorusEntity.FIRST_FACE);
            helper.assertTrue(BossDamage.isBoss(face), "its parts should count as the boss");
            float pool = c.poolSum(), part = c.partHealth(ChorusEntity.FIRST_FACE);
            ColtShot.Outcome out = ColtShot.strike(level, null, face);
            helper.assertTrue(out == ColtShot.Outcome.BOSS, "a face is part of a boss: " + out);
            helper.assertTrue(Math.abs(pool - c.poolSum() - SHOT) < 0.01f, "expected 60 past the cap of 40, dealt " + (pool - c.poolSum()));
            helper.assertTrue(Math.abs(part - c.partHealth(ChorusEntity.FIRST_FACE) - SHOT) < 0.01f, "the face should lose 60");
            float left = c.partHealth(ChorusEntity.FIRST_FACE);
            pool = c.poolSum();
            ColtShot.strike(level, null, face);
            helper.assertTrue(Math.abs(pool - c.poolSum() - left) < 0.01f, "a round takes no more than the part has: " + (pool - c.poolSum()));
            helper.assertFalse(c.partAlive(ChorusEntity.FIRST_FACE), "the face should have broken");
            ChorusTests.finish(helper);
        });
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void legacySixRoundStackClampsToFive(GameTestHelper helper) {
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var json = JsonParser.parseString("{\"id\":\"supernaturalcraft:the_colt\",\"count\":1,"
                + "\"components\":{\"supernaturalcraft:colt_ammo\":6}}");
        ItemStack s = ItemStack.CODEC.parse(ops, json).getOrThrow();
        helper.assertTrue(ColtItem.rounds(s) == ColtItem.CAPACITY, "an old six-round Colt should load full (5), has " + ColtItem.rounds(s));
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void reloadSeatsOneRoundAtATime(GameTestHelper helper) {
        ItemStack gun = colt(0);
        ServerPlayer p = ArsenalTests.fighter(helper, new BlockPos(1, 1, 1), gun);
        p.getInventory().add(new ItemStack(AllItems.COLT_BULLET.get(), 3));
        helper.assertTrue(ColtItem.startReload(p, gun), "the reload did not start");
        ColtReload.State state = gun.get(AllDataComponents.COLT_RELOAD);
        helper.assertTrue(state != null && state.count() == 3, "should plan three rounds, plans " + state);
        long t0 = state.start();
        ColtItem.tickReload(p, gun, true, t0 + ColtReload.insertTick(0) - 1);
        helper.assertTrue(ColtItem.rounds(gun) == 0, "seated a round too early");
        ColtItem.tickReload(p, gun, true, t0 + ColtReload.insertTick(0));
        helper.assertTrue(ColtItem.rounds(gun) == 1 && bullets(p) == 2, "first round: " + ColtItem.rounds(gun) + " loaded, " + bullets(p) + " carried");
        ColtItem.tickReload(p, gun, true, t0 + ColtReload.insertTick(2));
        helper.assertTrue(ColtItem.rounds(gun) == 3 && bullets(p) == 0, "all three: " + ColtItem.rounds(gun) + " loaded, " + bullets(p) + " carried");
        helper.assertTrue(ColtItem.reloading(gun), "the gun closes only after the outro");
        ColtItem.tickReload(p, gun, true, t0 + ColtReload.total(3));
        helper.assertFalse(ColtItem.reloading(gun), "the reload should be over");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void puttingTheGunAwayStopsTheReload(GameTestHelper helper) {
        ItemStack gun = colt(2);
        ServerPlayer p = ArsenalTests.fighter(helper, new BlockPos(1, 1, 1), gun);
        p.getInventory().add(new ItemStack(AllItems.COLT_BULLET.get(), 5));
        helper.assertTrue(ColtItem.startReload(p, gun), "the reload did not start");
        long t0 = gun.get(AllDataComponents.COLT_RELOAD).start();
        ColtItem.tickReload(p, gun, false, t0 + ColtReload.insertTick(1));
        helper.assertFalse(ColtItem.reloading(gun), "switching away should end the reload");
        helper.assertTrue(ColtItem.rounds(gun) == 2 && bullets(p) == 5, "nothing should have been seated or spent");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void anEmptyColtOnlyClicks(GameTestHelper helper) {
        ItemStack gun = colt(0);
        ServerPlayer p = ArsenalTests.fighter(helper, new BlockPos(1, 1, 1), gun);
        gun.getItem().use(helper.getLevel(), p, InteractionHand.MAIN_HAND);
        helper.assertTrue(ColtItem.rounds(gun) == 0 && ColtItem.chamber(gun) == 0, "an empty gun should neither fire nor turn");
        helper.assertTrue(p.getCooldowns().isOnCooldown(gun.getItem()), "the click should still cost a moment");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void roundsAreForgedByNightEightAtATime(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        var holder = manager.byKey(SupernaturalCraft.asResource("ritual/forge_colt_bullets"));
        helper.assertTrue(holder.isPresent() && holder.get().value() instanceof RitualRecipe, "the bullet ritual is missing");
        RitualRecipe r = (RitualRecipe) holder.get().value();
        helper.assertTrue(r.pattern().location().equals(SupernaturalCraft.asResource("blood_circle")), "it should be a blood circle: " + r.pattern());
        helper.assertTrue(r.conditions().time() == RitualConditions.Time.NIGHT, "it should need the night");
        helper.assertTrue(r.effect() instanceof CraftItemEffect c && c.result().is(AllItems.COLT_BULLET.get()) && c.result().getCount() == 8,
                "it should make eight rounds");
        for (var recipe : manager.getAllRecipesFor(RecipeType.CRAFTING)) {
            ItemStack out = recipe.value().getResultItem(helper.getLevel().registryAccess());
            helper.assertFalse(out.is(AllItems.COLT_BULLET.get()), "rounds can still be crafted: " + recipe.id());
        }
        helper.assertTrue(new ItemStack(AllItems.COLT_BULLET.get()).getMaxStackSize() == 16, "rounds should stack to sixteen");
        helper.succeed();
    }

    private static int bullets(ServerPlayer p) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(AllItems.COLT_BULLET.get())) n += s.getCount();
        }
        return n;
    }
}
