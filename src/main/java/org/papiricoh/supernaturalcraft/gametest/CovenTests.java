package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.bowl.spell.TaglockItem;
import org.papiricoh.supernaturalcraft.coven.PlanWriter;
import org.papiricoh.supernaturalcraft.coven.covenstead.CovenPiece;
import org.papiricoh.supernaturalcraft.coven.layout.CovenLayout;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.entity.witch.CovenWitchEntity;
import org.papiricoh.supernaturalcraft.entity.witch.FamiliarEntity;
import org.papiricoh.supernaturalcraft.entity.witch.HedgeWitchEntity;
import org.papiricoh.supernaturalcraft.entity.witch.HexedVillagerEntity;
import org.papiricoh.supernaturalcraft.entity.witch.WitchAssets;
import org.papiricoh.supernaturalcraft.entity.witch.WitchEntity;
import org.papiricoh.supernaturalcraft.entity.witch.WitchMagic;
import org.papiricoh.supernaturalcraft.hex.HexHooks;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The coven's creatures (v0.21 P3): witches casting through the magic's own pipeline, the Coven Witch's shield and her marked
 * curse (stepping out of the violet circle dodges it), familiars that spy and steal a lock of hair for their witch to curse its
 * owner with (unless warded or mirrored), hexed villagers (killed: the hex passes on; freed: a villager again, with thanks) and
 * a covenstead that builds with its bowl and its keepers. Tests whose creatures think for themselves each have their own batch,
 * and the players they hunt are real ({@link JournalTests.Witness}: in {@code level.players()}, never hurt, as they never tick).
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class CovenTests {

    static JournalTests.Witness witness(GameTestHelper helper, BlockPos rel) {
        JournalTests.Witness w = new JournalTests.Witness(helper.getLevel());
        Vec3 v = helper.absoluteVec(Vec3.atBottomCenterOf(rel));
        w.moveTo(v.x, v.y, v.z, 0, 0);
        helper.getLevel().addNewPlayer(w);
        return w;
    }

    static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    static ServerPlayer fake(GameTestHelper helper, String name, BlockPos rel) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.setGameMode(GameType.SURVIVAL);
        Vec3 v = helper.absoluteVec(Vec3.atBottomCenterOf(rel));
        p.moveTo(v.x, v.y, v.z, 0, 0);
        return p;
    }

    /** A stone cell round {@code rel} (interior one block wide, two high): nothing outside sees in. */
    static void cell(GameTestHelper helper, BlockPos rel) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    boolean inside = dx == 0 && dz == 0 && dy < 2;
                    if (!inside) helper.setBlock(rel.offset(dx, dy, dz), Blocks.STONE.defaultBlockState());
                }
            }
        }
    }

    private static <T extends WitchEntity> T witch(GameTestHelper helper, EntityType<T> type, BlockPos rel) {
        T w = helper.spawn(type, rel);
        w.setPersistenceRequired();
        return w;
    }

    private static FamiliarEntity familiar(GameTestHelper helper, WitchEntity witch, FamiliarEntity.Variant variant, BlockPos rel) {
        FamiliarEntity f = helper.spawn(AllEntities.FAMILIAR.get(), rel);
        f.setVariant(variant);
        witch.bind(f);
        return f;
    }

    // --- witches -----------------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = "coven_bolts", timeoutTicks = 120)
    public static void aHedgeWitchCastsSigilBoltsAndHerKinAreHerAllies(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        HedgeWitchEntity w = witch(helper, AllEntities.HEDGE_WITCH.get(), new BlockPos(2, 1, 5));
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(8, 1, 5));
        pig.setNoAi(true);
        helper.assertTrue(WitchMagic.bolt(w, pig, "hex", 1f), "a hex bolt flies");
        helper.assertTrue(WitchMagic.bolt(w, pig, "frost", 1f), "a frost bolt flies");
        helper.assertTrue(WitchMagic.bolt(w, pig, "pain", 1f), "a pain bolt flies");
        helper.assertFalse(WitchMagic.bolt(w, pig, "no_such_sigil", 1f), "an unknown sigil is no bolt");
        // Witches, familiars and the hexed are one household; a pig is not.
        HexedVillagerEntity thrall = helper.spawn(AllEntities.HEXED_VILLAGER.get(), new BlockPos(5, 1, 9));
        CovenWitchEntity sister = witch(helper, AllEntities.COVEN_WITCH.get(), new BlockPos(5, 1, 1));
        helper.assertTrue(w.isAlliedTo(sister) && w.isAlliedTo(thrall) && !w.isAlliedTo(pig), "the coven's allies");
        helper.assertFalse(w.canAttack(sister) || w.canAttack(thrall), "a witch never turns on her own");
        helper.assertTrue(WitchMagic.kin(helper.spawn(EntityType.WITCH, new BlockPos(9, 1, 9))), "a vanilla witch is kin too");
        helper.succeedWhen(() -> {
            helper.assertTrue(pig.hasEffect(AllMobEffects.JINXED), "the hex reaches the pig");
            helper.assertTrue(pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the frost or pain slows it");
            for (Mob m : helper.getLevel().getEntitiesOfClass(Mob.class, new AABB(helper.absolutePos(new BlockPos(5, 1, 5))).inflate(12))) m.discard();
        });
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "coven_shield", timeoutTicks = CovenWitchEntity.SHIELD_REGEN + 120)
    public static void theCovenWitchsShieldHoldsUntilBrokenAndRisesAgain(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerLevel level = helper.getLevel();
        CovenWitchEntity battered = witch(helper, AllEntities.COVEN_WITCH.get(), new BlockPos(2, 1, 2));
        CovenWitchEntity salted = witch(helper, AllEntities.COVEN_WITCH.get(), new BlockPos(8, 1, 2));
        CovenWitchEntity unhexed = witch(helper, AllEntities.COVEN_WITCH.get(), new BlockPos(2, 1, 8));
        CovenWitchEntity shot = witch(helper, AllEntities.COVEN_WITCH.get(), new BlockPos(8, 1, 8));
        float full = battered.getMaxHealth();
        helper.assertTrue(full == 48f && battered.shielded(), "48 health behind a shield");
        // The bubble swallows blows until it has taken SHIELD_HP of them, then breaks.
        battered.hurt(level.damageSources().generic(), 5);
        battered.hurt(level.damageSources().generic(), 5);
        helper.assertTrue(battered.getHealth() == full && battered.shielded(), "two blows swallowed");
        battered.hurt(level.damageSources().generic(), 5);
        helper.assertFalse(battered.shielded(), "the third breaks it (" + CovenWitchEntity.SHIELD_HP + " in all)");
        helper.assertTrue(battered.getHealth() == full, "the blow that breaks it is spent on it");
        battered.invulnerableTime = 0;
        battered.hurt(level.damageSources().generic(), 5);
        helper.assertTrue(battered.getHealth() < full, "unshielded, she bleeds");
        // Salt and Break Hex shatter it at once; the Colt goes straight through it.
        helper.assertTrue(HexHooks.saltStruck(salted, null) && !salted.shielded(), "salt breaks it");
        helper.assertFalse(HexHooks.saltStruck(salted, null), "nothing left to break");
        helper.assertTrue(HexHooks.breakHex(unhexed, null) && !unhexed.shielded(), "Break Hex breaks it");
        shot.hurt(AllDamageTypes.source(level, AllDamageTypes.COLT, null), 10);
        helper.assertTrue(shot.getHealth() < full, "the Colt is not stopped by a bubble");
        // A familiar answers her call, bound to her.
        FamiliarEntity f = salted.callFamiliar();
        helper.assertTrue(f != null && salted.familiar() == f && salted.getUUID().equals(f.witchId()), "a familiar answers, bound to her");
        helper.assertTrue(Math.abs(salted.power() - 1.4f * WitchEntity.FAMILIAR_POWER) < 1e-4, "her magic grows while it lives: " + salted.power());
        helper.runAfterDelay(CovenWitchEntity.SHIELD_REGEN + 60, () -> {
            helper.assertTrue(battered.shielded(), "the shield rises again after " + CovenWitchEntity.SHIELD_REGEN + " ticks");
            for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(helper.absolutePos(new BlockPos(5, 1, 5))).inflate(40))) m.discard();
            helper.succeed();
        });
    }

    /** A Coven Witch hunting {@code w}: the first violet circle under them, and whether the curse took (its marks). */
    private static void covenCurse(GameTestHelper helper, boolean stepOut) {
        SNGameTests.floor(helper, 11, 11);
        ServerLevel level = helper.getLevel();
        CovenWitchEntity witch = witch(helper, AllEntities.COVEN_WITCH.get(), new BlockPos(1, 1, 1));
        JournalTests.Witness w = witness(helper, new BlockPos(8, 1, 8));
        witch.setTarget(w);
        long[] marked = {-1};
        int land = CovenWitchEntity.CURSE_WINDUP + WitchAssets.HIT_TICKS.get("hex");
        helper.onEachTick(() -> {
            long now = level.getGameTime();
            if (marked[0] < 0) {
                List<TelegraphMarker> circles = level.getEntitiesOfClass(TelegraphMarker.class, w.getBoundingBox().inflate(2),
                        m -> m.color() == TelegraphMarker.VIOLET);
                if (circles.isEmpty()) return;
                marked[0] = now;
                if (stepOut) w.moveTo(w.getX(), w.getY(), w.getZ() - 4);
                return;
            }
            if (now < marked[0] + land + 8) return;
            boolean cursed = w.hasEffect(AllMobEffects.MARKED) || w.hasEffect(AllMobEffects.SILENCED);
            for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(helper.absolutePos(new BlockPos(5, 1, 5))).inflate(40))) m.discard();
            leave(helper, w);
            if (stepOut) helper.assertFalse(cursed, "out of the violet circle, her curse misses");
            else helper.assertTrue(cursed, "standing in the violet circle, her curse (frailty or silence) lands");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "coven_curse_stay", timeoutTicks = 400)
    public static void aCovenWitchsCurseLandsOnWhoeverStaysInHerCircle(GameTestHelper helper) {
        covenCurse(helper, false);
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "coven_curse_dodge", timeoutTicks = 400)
    public static void steppingOutOfTheVioletCircleDodgesHerCurse(GameTestHelper helper) {
        covenCurse(helper, true);
    }

    // --- familiars ---------------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = "coven_spy", timeoutTicks = 100)
    public static void aFamiliarSpiesForItsWitch(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        // A wall between the witch and the hunter: only her familiar can see them.
        for (int z = 0; z < 11; z++) for (int y = 1; y <= 4; y++) helper.setBlock(new BlockPos(3, y, z), Blocks.STONE.defaultBlockState());
        HedgeWitchEntity witch = witch(helper, AllEntities.HEDGE_WITCH.get(), new BlockPos(1, 1, 5));
        FamiliarEntity cat = familiar(helper, witch, FamiliarEntity.Variant.CAT, new BlockPos(6, 1, 5));
        JournalTests.Witness w = witness(helper, new BlockPos(9, 1, 5));
        helper.succeedWhen(() -> {
            helper.assertTrue(w.hasEffect(MobEffects.GLOWING), "the familiar marks whom it sees");
            helper.assertTrue(witch.getTarget() == w, "and tells its witch, who cannot see them herself");
            cat.discard();
            witch.discard();
            leave(helper, w);
        });
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "coven_steal", timeoutTicks = 400)
    public static void aFamiliarStealsALockOfHairAndDropsItWhenKilled(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        // A familiar steals only for a witch (P3 fix): she stands by, still, while her cat works.
        HedgeWitchEntity mistress = witch(helper, AllEntities.HEDGE_WITCH.get(), new BlockPos(1, 1, 1));
        mistress.setNoAi(true);
        FamiliarEntity cat = familiar(helper, mistress, FamiliarEntity.Variant.CAT, new BlockPos(4, 1, 5));
        cat.setPersistenceRequired();
        JournalTests.Witness w = witness(helper, new BlockPos(5, 1, 5));
        helper.succeedWhen(() -> {
            var lock = cat.stolen();
            helper.assertTrue(lock != null, "the cat has not robbed anyone yet (alive " + cat.isAlive() + ", " + cat.distanceTo(w)
                    + " blocks off, may steal " + cat.canSteal() + ", sees " + cat.hasLineOfSight(w) + ", hunter listed "
                    + helper.getLevel().players().contains(w) + ", bound " + (cat.witchId() != null) + ", her mistress there " + (cat.master() != null)
                    + ", holds " + cat.held() + ")");
            helper.assertTrue(lock.owner().equals(w.getUUID()), "it holds a lock of the hunter's hair");
            leave(helper, w);
            cat.hurt(helper.getLevel().damageSources().generic(), 1000);
            List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, cat.getBoundingBox().inflate(3),
                    i -> i.getItem().is(AllItems.TAGLOCK.get()) && TaglockItem.filled(i.getItem()));
            dropped.forEach(Entity::discard);
            mistress.discard();
            helper.assertTrue(dropped.size() == 1, "killed, it drops what it stole");
        });
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "coven_robbed", timeoutTicks = 360)
    public static void aStolenLockCursesItsOwnerUnlessWardedOrMirrored(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        // Three hunters walled in, out of every familiar's sight; each witch's familiar already holds one's hair.
        BlockPos[] cells = {new BlockPos(1, 1, 9), new BlockPos(5, 1, 9), new BlockPos(9, 1, 9)};
        JournalTests.Witness[] hunters = new JournalTests.Witness[3];
        HedgeWitchEntity[] witches = new HedgeWitchEntity[3];
        for (int i = 0; i < 3; i++) {
            cell(helper, cells[i]);
            hunters[i] = witness(helper, cells[i]);
            witches[i] = witch(helper, AllEntities.HEDGE_WITCH.get(), new BlockPos(1 + i * 4, 1, 2));
            FamiliarEntity f = familiar(helper, witches[i], FamiliarEntity.Variant.CAT, new BlockPos(1 + i * 4, 1, 4));
            f.setHeld(TaglockItem.of(hunters[i]));
        }
        hunters[1].addEffect(new MobEffectInstance(AllMobEffects.NAME_WARDED, 2400, 0));
        hunters[2].addEffect(new MobEffectInstance(AllMobEffects.MIRRORED, 2400, 0));
        helper.succeedWhen(() -> {
            helper.assertTrue(hunters[0].hasEffect(MobEffects.UNLUCK) && hunters[0].hasEffect(AllMobEffects.JINXED),
                    "the robbed hunter is cursed with misfortune from afar");
            helper.assertTrue(witches[1].getHealth() < witches[1].getMaxHealth(), "the ward turns the curse back on the witch's own hide");
            helper.assertFalse(hunters[1].hasEffect(MobEffects.UNLUCK), "the warded hunter is spared");
            helper.assertTrue(witches[2].hasEffect(MobEffects.UNLUCK), "the mirror sends the curse home");
            helper.assertFalse(hunters[2].hasEffect(MobEffects.UNLUCK) || hunters[2].hasEffect(AllMobEffects.MIRRORED),
                    "the mirrored hunter is spared and the mirror spent");
            for (Mob m : helper.getLevel().getEntitiesOfClass(Mob.class, new AABB(helper.absolutePos(new BlockPos(5, 1, 5))).inflate(40))) m.discard();
            leave(helper, hunters);
        });
    }

    // --- hexed villagers ---------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = "coven_hexed")
    public static void killingAHexedVillagerPassesItsHexToTheKiller(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        Villager v = helper.spawn(EntityType.VILLAGER, new BlockPos(5, 1, 5));
        HexedVillagerEntity hexed = HexedVillagerEntity.hex(v, null);
        helper.assertTrue(hexed != null && !v.isAlive() && hexed.isAlive(), "the villager became one of hers");
        ServerPlayer killer = fake(helper, "sn-test-hexkiller", new BlockPos(5, 1, 7));
        hexed.hurt(helper.getLevel().damageSources().playerAttack(killer), 1000);
        helper.assertTrue(hexed.isDeadOrDying(), "it dies");
        helper.assertTrue(killer.hasEffect(AllMobEffects.JINXED) && killer.hasEffect(MobEffects.UNLUCK), "its hex clings to its killer");
        helper.assertTrue(killer.getEffect(AllMobEffects.JINXED).getDuration() > HexedVillagerEntity.JINX_TICKS - 20, "for two minutes");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(Villager.class, new AABB(helper.absolutePos(new BlockPos(5, 1, 5))).inflate(6)).isEmpty(),
                "no villager comes back from that");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "coven_freed", timeoutTicks = 120)
    public static void breakHexFreesAHexedVillagerWhoThanksItsRescuer(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerLevel level = helper.getLevel();
        Villager v = helper.spawn(EntityType.VILLAGER, new BlockPos(5, 1, 5));
        v.setVillagerData(v.getVillagerData().setProfession(VillagerProfession.LIBRARIAN));
        v.setCustomName(net.minecraft.network.chat.Component.literal("Agnes"));
        HexedVillagerEntity hexed = HexedVillagerEntity.hex(v, null);
        JournalTests.Witness w = witness(helper, new BlockPos(5, 1, 8));
        helper.assertTrue(HexHooks.breakHex(hexed, w), "Break Hex frees it");
        helper.assertTrue(hexed.freeing() && hexed.isAlive(), "it slumps first");
        helper.assertFalse(HexHooks.breakHex(hexed, w), "a second Break Hex finds nothing to break");
        AABB around = new AABB(helper.absolutePos(new BlockPos(5, 1, 5))).inflate(4);
        helper.runAfterDelay(HexedVillagerEntity.FREE_TICKS + 5, () -> {
            List<Villager> back = level.getEntitiesOfClass(Villager.class, around);
            List<ItemEntity> emeralds = level.getEntitiesOfClass(ItemEntity.class, around, i -> i.getItem().is(Items.EMERALD));
            boolean saved = AuthorWorld.done(w, HexedVillagerEntity.ADVANCEMENT);
            int rep = back.isEmpty() ? 0 : back.getFirst().getPlayerReputation(w);
            back.forEach(Entity::discard);
            emeralds.forEach(Entity::discard);
            leave(helper, w);
            helper.assertTrue(!hexed.isAlive(), "the hexed one is gone");
            helper.assertTrue(back.size() == 1, "a villager stands again, found " + back.size());
            helper.assertTrue(back.getFirst().getVillagerData().getProfession() == VillagerProfession.LIBRARIAN
                    && "Agnes".equals(back.getFirst().getCustomName().getString()), "the very villager it was");
            int n = emeralds.stream().mapToInt(i -> i.getItem().getCount()).sum();
            helper.assertTrue(n >= 1 && n <= 3, "one to three emeralds, got " + n);
            helper.assertTrue(rep > 0, "it speaks well of its rescuer: " + rep);
            helper.assertTrue(saved, "main/hexed_villager_saved");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "coven_salted", timeoutTicks = 100)
    public static void aSaltRoundFreesOneFoundAlreadyHexed(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerLevel level = helper.getLevel();
        HexedVillagerEntity hexed = helper.spawn(AllEntities.HEXED_VILLAGER.get(), new BlockPos(5, 1, 5));
        ServerPlayer hunter = fake(helper, "sn-test-salter", new BlockPos(5, 1, 8));
        helper.assertTrue(HexHooks.saltStruck(hexed, hunter) && hexed.freeing(), "salt frees it");
        AABB around = new AABB(helper.absolutePos(new BlockPos(5, 1, 5))).inflate(4);
        helper.runAfterDelay(HexedVillagerEntity.FREE_TICKS + 5, () -> {
            List<Villager> back = level.getEntitiesOfClass(Villager.class, around);
            back.forEach(Entity::discard);
            level.getEntitiesOfClass(ItemEntity.class, around).forEach(Entity::discard);
            helper.assertTrue(!hexed.isAlive() && back.size() == 1, "a (new) villager stands where it was");
            helper.succeed();
        });
    }

    // --- the covenstead ----------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "coven_build", timeoutTicks = 200)
    public static void aCovensteadBuildsWithItsBowlChestsAndKeepers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SNGameTests.floor(helper, 48, 48);
        BlockPos origin = helper.absolutePos(new BlockPos(24, 7, 24));
        int dir = 1;
        long seed = 12345L;
        CovenPiece piece = new CovenPiece(origin, dir, seed);
        BoundingBox box = PlanWriter.fullBox(origin, CovenLayout.plan(seed, dir), CovenLayout.HEIGHT);
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random, box,
                new net.minecraft.world.level.ChunkPos(origin), origin);
        var b = CovenLayout.at(CovenLayout.BOWL, dir);
        helper.assertTrue(level.getBlockState(origin.offset(b.x(), b.y(), b.z())).is(AllBlocks.SPELL_BOWL.get()), "the coven's own bowl stands");
        for (var c : CovenLayout.CHESTS) {
            var at = CovenLayout.at(c, dir);
            var be = level.getBlockEntity(origin.offset(at.x(), at.y(), at.z()));
            helper.assertTrue(be instanceof RandomizableContainerBlockEntity r && r.getLootTable() != null
                    && r.getLootTable().location().toString().equals(CovenLayout.LOOT), "a chest with the coven's loot at " + at);
        }
        AABB all = new AABB(origin).inflate(CovenLayout.RADIUS + 4);
        List<HedgeWitchEntity> witches = level.getEntitiesOfClass(HedgeWitchEntity.class, all);
        List<FamiliarEntity> familiars = level.getEntitiesOfClass(FamiliarEntity.class, all);
        Set<UUID> ids = new HashSet<>();
        witches.forEach(w -> ids.add(w.getUUID()));
        level.getEntitiesOfClass(CovenWitchEntity.class, all).forEach(w -> ids.add(w.getUUID()));
        boolean bound = familiars.stream().allMatch(f -> f.witchId() != null && ids.contains(f.witchId()));
        boolean persistent = witches.stream().allMatch(Mob::isPersistenceRequired);
        for (Mob m : level.getEntitiesOfClass(Mob.class, all.inflate(8))) m.discard();
        helper.assertTrue(witches.size() >= 2 && witches.size() <= 4, "two to four hedge witches keep it, found " + witches.size());
        helper.assertTrue(familiars.size() == 2, "two familiars, found " + familiars.size());
        helper.assertTrue(bound, "each familiar is bound to a witch of this coven");
        helper.assertTrue(persistent, "the keepers never despawn");
        helper.succeed();
    }
}
