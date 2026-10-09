package org.papiricoh.supernaturalcraft.gametest;

import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.BossDamage;
import org.papiricoh.supernaturalcraft.entity.boss.CappedBoss;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import org.papiricoh.supernaturalcraft.reward.ColtItem;
import org.papiricoh.supernaturalcraft.reward.colt.ColtReload;
import org.papiricoh.supernaturalcraft.reward.colt.ColtShot;
import org.papiricoh.supernaturalcraft.ritual.RitualConditions;
import org.papiricoh.supernaturalcraft.ritual.RitualRecipe;
import org.papiricoh.supernaturalcraft.ritual.effect.CraftItemEffect;

import java.util.List;

/**
 * The Colt (v0.15) kills any living thing in one round but players, great enemies and archangels-and-above; a great enemy
 * takes exactly its hard cap past every multiplier, never past a phase; it is fed eight rounds at a time.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class ColtTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    /** What a round takes from {@code boss}, in vanilla health: 5% of its true health, exactly. */
    private static float shot(CappedBoss boss) {
        return ColtShot.bossDamage(boss) / boss.healthScale();
    }

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
    public static void anyLivingThingDiesToOneRound(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<EntityType<? extends net.minecraft.world.entity.Mob>> kinds = List.of(EntityType.IRON_GOLEM, EntityType.WARDEN,
                AllEntities.HOST_ANGEL.get(), AllEntities.HELLHOUND.get(), AllEntities.REAPER.get());
        int x = 2;
        for (EntityType<? extends net.minecraft.world.entity.Mob> kind : kinds) {
            LivingEntity e = helper.spawnWithNoFreeWill(kind, new BlockPos(x, 2, 6));
            x += 3;
            ColtShot.Outcome out = ColtShot.strike(level, null, e);
            helper.assertTrue(out == ColtShot.Outcome.EXECUTED, kind.getDescriptionId() + " was not executed: " + out);
            helper.assertTrue(!e.isAlive() || e.isRemoved(), kind.getDescriptionId() + " survived a round");
        }
        helper.succeed();
    }

    /** v0.17: the Colt is the one thing the old monsters' rules can't refuse: no beheading, no silver, no borrowed skin needed. */
    @GameTest(template = SNGameTests.MEDIUM)
    public static void theMenOfLettersMonstersDieToOneRound(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var vampire = helper.spawnWithNoFreeWill(AllEntities.VAMPIRE.get(), new BlockPos(2, 2, 4));
        var werewolf = helper.spawnWithNoFreeWill(AllEntities.WEREWOLF.get(), new BlockPos(5, 2, 4));
        var shifter = helper.spawnWithNoFreeWill(AllEntities.SHAPESHIFTER.get(), new BlockPos(8, 2, 4));
        shifter.disguiseAsVillager(level.getRandom());
        helper.assertTrue(shifter.disguised(), "the shapeshifter should start in a borrowed skin");
        for (LivingEntity e : List.<LivingEntity>of(vampire, werewolf, shifter)) {
            // A blow a moment ago (its i-frames) must not save it either.
            e.hurt(level.damageSources().generic(), 2f);
            ColtShot.Outcome out = ColtShot.strike(level, null, e);
            helper.assertTrue(out == ColtShot.Outcome.EXECUTED, e.getType().getDescriptionId() + " was not executed: " + out);
            helper.assertTrue(!e.isAlive() || e.isRemoved(), e.getType().getDescriptionId() + " survived the Colt");
        }
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "colt_immune")
    public static void archangelsAndOtherBossesAreHurtNotExecuted(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // The Wither is in #c:bosses (and so #bosses) but no great enemy of ours: its hard cap, at least colt.otherDamage.
        LivingEntity wither = helper.spawnWithNoFreeWill(EntityType.WITHER, new BlockPos(4, 2, 4));
        float before = wither.getHealth();
        ColtShot.Outcome out = ColtShot.strike(level, null, wither);
        helper.assertTrue(out != ColtShot.Outcome.EXECUTED && wither.isAlive(), "the Wither must survive a round: " + out);
        helper.assertTrue(before - wither.getHealth() > 0 && before - wither.getHealth() <= ColtShot.immuneDamage(wither.getMaxHealth()) + 0.01f,
                "dealt " + (before - wither.getHealth()));
        // The caged Lucifer is #colt_immune.
        LivingEntity caged = helper.spawn(AllEntities.CAGED_LUCIFER.get(), new BlockPos(9, 2, 4));
        helper.assertTrue(caged.getType().is(AllTags.Entities.COLT_IMMUNE), "the caged Lucifer should be #colt_immune");
        ColtShot.strike(level, null, caged);
        helper.assertTrue(caged.isAlive() && !caged.isRemoved(), "the caged Lucifer is never executed");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "colt_player")
    public static void aPlayerIsNotExecutedByDefault(GameTestHelper helper) {
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(4, 2, 4), ItemStack.EMPTY);
        // More than one round's worth of hearts, so surviving it proves it was not an execution.
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(60);
        p.setHealth(60);
        float before = p.getHealth();
        ColtShot.Outcome out = ColtShot.strike(helper.getLevel(), null, p);
        helper.assertTrue(out != ColtShot.Outcome.EXECUTED, "a player is no lesser thing: " + out);
        helper.assertTrue(p.isAlive(), "a player survives a round");
        helper.assertTrue(before - p.getHealth() > 0, "but takes colt.otherDamage (after armour), took " + (before - p.getHealth()));
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "colt_lucifer", timeoutTicks = 60)
    public static void luciferTakesHisHardCap(GameTestHelper helper) {
        LuciferEntity l = lucifer(helper);
        helper.runAfterDelay(2, () -> {
            // A mundane blow first: its i-frames must not eat into the round.
            l.hurt(helper.getLevel().damageSources().magic(), 10f);
            float before = l.getHealth();
            ColtShot.Outcome out = ColtShot.strike(helper.getLevel(), null, l);
            helper.assertTrue(out == ColtShot.Outcome.BOSS, "Lucifer is a boss: " + out);
            helper.assertTrue(Math.abs(before - l.getHealth() - shot(l)) < 0.01f, "expected 5% of him, " + shot(l) + ", dealt " + (before - l.getHealth()));
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "colt_share", timeoutTicks = 20)
    public static void aRoundTakesFivePercentButOnlyTheHardCapFromTheAuthor(GameTestHelper helper) {
        BossTests.cleanup(helper);
        ServerLevel level = helper.getLevel();
        var colt = org.papiricoh.supernaturalcraft.registry.AllDamageTypes.source(level,
                org.papiricoh.supernaturalcraft.registry.AllDamageTypes.COLT, null);
        Vec3 at = helper.absoluteVec(MID.above().getCenter());
        LuciferEntity azazel = AllEntities.AZAZEL.get().create(level);
        azazel.moveTo(at.x, at.y, at.z, 0, 0);
        float before = azazel.trueHealth();
        azazel.hurt(colt, 1e6f);
        float took = before - azazel.trueHealth();
        helper.assertTrue(Math.abs(took - 250f) < 0.5f, "a round should take 5% of Azazel's 5000, took " + took);
        LuciferEntity chuck = AllEntities.CHUCK.get().create(level);
        chuck.moveTo(at.x, at.y, at.z, 0, 0);
        before = chuck.trueHealth();
        chuck.hurt(colt, 1e6f);
        took = before - chuck.trueHealth();
        helper.assertTrue(Math.abs(took - 1500f) < 1f, "the Author takes only his hard cap, 1500 of 100000, took " + took);
        // Never past a phase floor, whatever the share.
        BossHealthGuard.set(azazel, azazel.getMaxHealth() * 0.5f + 1);
        azazel.invulnerableTime = 0;
        azazel.hurt(colt, 1e6f);
        helper.assertTrue(Math.abs(azazel.getHealth() - azazel.getMaxHealth() * 0.5f) < 0.01f, "a round stops at his threshold");
        azazel.discard();
        chuck.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "colt_lucifer_floor", timeoutTicks = 60)
    public static void luciferStopsAtThreshold(GameTestHelper helper) {
        LuciferEntity l = lucifer(helper);
        helper.runAfterDelay(2, () -> {
            BossHealthGuard.set(l, l.getMaxHealth() * 0.75f + shot(l) * 0.5f);
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
            helper.assertTrue(Math.abs(before - l.getHealth() - shot(l)) < 0.01f, "expected " + shot(l) + ", dealt " + (before - l.getHealth()));
            helper.assertTrue(ColtItem.rounds(gun) == ColtItem.CAPACITY - 1, "a round should be spent, " + ColtItem.rounds(gun) + " left");
            helper.assertTrue(ColtItem.chamber(gun) == 1, "the cylinder should have turned to chamber 1");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "colt_amara", timeoutTicks = 60)
    public static void amarasPartsAndCoreTakeHerHardCap(GameTestHelper helper) {
        AmaraEntity a = AmaraTests.summon(helper);
        ServerLevel level = helper.getLevel();
        var anchor = a.part(AmaraEntity.FIRST_ANCHOR);
        helper.assertTrue(BossDamage.isBoss(anchor), "her parts should count as the boss");
        float partBefore = a.partHealth(AmaraEntity.FIRST_ANCHOR);
        ColtShot.Outcome out = ColtShot.strike(level, null, anchor);
        helper.assertTrue(out == ColtShot.Outcome.BOSS, "an anchor is part of a boss: " + out);
        float dealt = partBefore - a.partHealth(AmaraEntity.FIRST_ANCHOR);
        float cap = ColtShot.bossDamage(a);
        helper.assertTrue(Math.abs(dealt - Math.min(cap, partBefore)) < 0.01f || !a.partAlive(AmaraEntity.FIRST_ANCHOR),
                "the anchor should lose exactly 5% of her, " + cap + " (no holy bonus), lost " + dealt);
        AmaraTests.breakAnchors(helper, a);
        float before = a.getHealth();
        ColtShot.strike(level, null, a.part(AmaraEntity.CORE));
        // Under her own eclipse, still exactly the hard cap: exact damage skips the eclipse's holy bonus too.
        helper.assertTrue(Math.abs(before - a.getHealth() - shot(a)) < 0.01f, "the core should take exactly " + shot(a) + ", took " + (before - a.getHealth()));
        BossHealthGuard.set(a, a.getMaxHealth() * 0.7f + shot(a) * 0.5f);
        ColtShot.strike(level, null, a.part(AmaraEntity.CORE));
        helper.assertTrue(Math.abs(a.getHealth() - a.getMaxHealth() * 0.7f) < 0.5f, "the round must stop at her threshold, health " + a.getHealth());
        AmaraTests.finish(helper);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "colt_chorus", timeoutTicks = 60)
    public static void chorusPartTakesItsHardCap(GameTestHelper helper) {
        ChorusEntity c = ChorusTests.summon(helper);
        helper.runAfterDelay(2, () -> {
            ServerLevel level = helper.getLevel();
            var face = c.part(ChorusEntity.FIRST_FACE);
            helper.assertTrue(BossDamage.isBoss(face), "its parts should count as the boss");
            float pool = c.poolSum(), part = c.partHealth(ChorusEntity.FIRST_FACE);
            ColtShot.Outcome out = ColtShot.strike(level, null, face);
            helper.assertTrue(out == ColtShot.Outcome.BOSS, "a face is part of a boss: " + out);
            float cap = ColtShot.bossDamage(c);
            helper.assertTrue(Math.abs(pool - c.poolSum() - cap) < 0.05f, "expected 5% of it, " + cap + ", dealt " + (pool - c.poolSum()));
            helper.assertTrue(Math.abs(part - c.partHealth(ChorusEntity.FIRST_FACE) - cap) < 0.05f, "the face should lose the hard cap");
            while (c.partHealth(ChorusEntity.FIRST_FACE) > cap) ColtShot.strike(level, null, face);
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
    public static void theEndlessColtNeverRunsDry(GameTestHelper helper) {
        ItemStack gun = new ItemStack(AllItems.ENDLESS_COLT.get());
        ServerPlayer p = ArsenalTests.fighter(helper, new BlockPos(1, 1, 1), gun);
        helper.assertTrue(ColtItem.endless(gun) && ColtItem.rounds(gun) == ColtItem.CAPACITY, "it should come loaded");
        for (int i = 0; i < ColtItem.CAPACITY + 2; i++) {
            helper.assertTrue(ColtItem.canFire(p, gun), "it should always fire, shot " + i);
            ColtItem.fire(p, gun);
        }
        helper.assertTrue(ColtItem.rounds(gun) == ColtItem.CAPACITY, "no round should be spent, " + ColtItem.rounds(gun) + " left");
        helper.assertFalse(ColtItem.startReload(p, gun), "a full gun has nothing to reload");
        helper.assertFalse(ColtItem.endless(new ItemStack(AllItems.THE_COLT.get())), "the ordinary Colt is not endless");
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
        helper.assertTrue(new ItemStack(AllItems.COLT_BULLET.get()).getMaxStackSize() == 64, "rounds should stack to sixty-four");
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
