package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.legacy.HenryEntity;
import org.papiricoh.supernaturalcraft.entity.legacy.ShapeshifterEntity;
import org.papiricoh.supernaturalcraft.entity.legacy.VampireEntity;
import org.papiricoh.supernaturalcraft.entity.legacy.WerewolfEntity;
import org.papiricoh.supernaturalcraft.legacy.Archive;
import org.papiricoh.supernaturalcraft.legacy.HenryDialogue;
import org.papiricoh.supernaturalcraft.legacy.Legacies;
import org.papiricoh.supernaturalcraft.legacy.Legacy;
import org.papiricoh.supernaturalcraft.legacy.LegacyOrder;
import org.papiricoh.supernaturalcraft.legacy.LegacyRules;
import org.papiricoh.supernaturalcraft.legacy.LegacySchedule;
import org.papiricoh.supernaturalcraft.legacy.LegacyServerHandlers;
import org.papiricoh.supernaturalcraft.legacy.bunker.BunkerBuilder;
import org.papiricoh.supernaturalcraft.legacy.bunker.BunkerDoorBlock;
import org.papiricoh.supernaturalcraft.legacy.bunker.BunkerLayout;
import org.papiricoh.supernaturalcraft.legacy.bunker.BunkerLocator;
import org.papiricoh.supernaturalcraft.legacy.bunker.BunkerProtection;
import org.papiricoh.supernaturalcraft.legacy.bunker.BunkerSavedData;
import org.papiricoh.supernaturalcraft.legacy.bunker.BunkerWorld;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseFile;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseOffice;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseSavedData;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseSites;
import org.papiricoh.supernaturalcraft.legacy.gear.OrderGear;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMapDecorations;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

import java.util.List;
import java.util.UUID;

/**
 * The Men of Letters' world (v0.17, agent A): Henry's offer, the bunker's door and building, cases coming alive and closing,
 * and the old rules of the three monsters. Tests that touch the world's bunker record have their own batch and put it back.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class LegacyTests {

    private static JournalTests.Witness witness(GameTestHelper helper, BlockPos rel) {
        JournalTests.Witness w = new JournalTests.Witness(helper.getLevel());
        Vec3 at = helper.absoluteVec(rel.getBottomCenter());
        w.moveTo(at.x, at.y, at.z, 0, 0);
        helper.getLevel().addNewPlayer(w);
        return w;
    }

    private static void gone(GameTestHelper helper, Entity... entities) {
        for (Entity e : entities) {
            if (e instanceof ServerPlayer p) helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
            else if (e != null) e.discard();
        }
    }

    private static ServerPlayer fake(GameTestHelper helper, ItemStack held) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "sn-test-legacy"));
        p.setGameMode(GameType.SURVIVAL);
        p.setItemInHand(InteractionHand.MAIN_HAND, held);
        return p;
    }

    /** Points the world's bunker record at {@code origin}; returns the record as it was. */
    private static CompoundTag adoptBunker(GameTestHelper helper, BlockPos origin, boolean built) {
        BunkerSavedData data = BunkerSavedData.get(helper.getLevel());
        CompoundTag before = data.snapshot();
        data.setBunker(origin, 0, built);
        return before;
    }

    // --- Henry ---------------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = "legacy_henry_join")
    public static void henryOffersAndTheHunterJoins(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        CompoundTag before = adoptBunker(helper, helper.absolutePos(new BlockPos(5, 1, 5)), true);
        JournalTests.Witness w = witness(helper, new BlockPos(5, 1, 3));
        Legacies.set(w, LegacySchedule.luciferBeaten(Legacy.NONE, 0));
        HenryEntity h = HenryEntity.visit(w);
        helper.assertTrue(h != null, "Henry came");
        h.moveTo(w.getX(), w.getY(), w.getZ() + 2);
        LegacyServerHandlers.open(h, w);
        helper.assertTrue(h.stage(w.getUUID()) == HenryDialogue.Stage.OFFER, "the offer first, got " + h.stage(w.getUUID()));
        helper.assertTrue(LegacyServerHandlers.answer(w, h, HenryDialogue.NEW_CASE) == HenryDialogue.Stage.OFFER, "no case before joining");
        helper.assertTrue(!Legacies.member(w), "not a member yet");
        HenryDialogue.Stage next = LegacyServerHandlers.answer(w, h, HenryDialogue.ACCEPT);
        helper.assertTrue(next == HenryDialogue.Stage.WELCOME, "welcome, got " + next);
        Legacy l = Legacies.get(w);
        boolean key = w.getInventory().contains(s -> s.is(AllItems.BUNKER_KEY.get()));
        ItemStack map = ItemStack.EMPTY;
        for (ItemStack s : w.getInventory().items) if (s.is(Items.FILLED_MAP)) map = s;
        MapDecorations decos = map.getOrDefault(DataComponents.MAP_DECORATIONS, MapDecorations.EMPTY);
        boolean marked = decos.decorations().values().stream().anyMatch(d -> d.type().is(AllMapDecorations.BUNKER));
        boolean adv = org.papiricoh.supernaturalcraft.author.AuthorWorld.done(w, LegacyRules.advancement(1));
        helper.assertTrue(LegacyServerHandlers.answer(w, h, HenryDialogue.CLOSE) == null, "the talk ends");
        boolean leaving = h.leaving();
        BunkerSavedData.get(helper.getLevel()).restore(before);
        gone(helper, w, h);
        helper.assertTrue(l.rank() == 1 && l.henry() == Legacy.HENRY_JOINED, "an Aspirant: " + l);
        helper.assertTrue(key, "the Bunker Key");
        helper.assertTrue(marked, "a map marked with the bunker");
        helper.assertTrue(adv, "advancement legacy_1");
        helper.assertTrue(leaving, "he goes once the talk is over");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "legacy_henry_decline")
    public static void turnedAwayHenryWillComeBack(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        JournalTests.Witness w = witness(helper, new BlockPos(5, 1, 3));
        Legacies.set(w, LegacySchedule.luciferBeaten(Legacy.NONE, 0));
        HenryEntity h = HenryEntity.visit(w);
        h.moveTo(w.getX(), w.getY(), w.getZ() + 2);
        LegacyServerHandlers.open(h, w);
        HenryDialogue.Stage next = LegacyServerHandlers.answer(w, h, HenryDialogue.DECLINE);
        Legacy l = Legacies.get(w);
        long today = LegacySchedule.dayIndex(helper.getLevel().getDayTime());
        gone(helper, w, h);
        helper.assertTrue(next == HenryDialogue.Stage.FAREWELL, "farewell, got " + next);
        helper.assertTrue(!l.member() && l.henry() == Legacy.HENRY_DECLINED, "declined: " + l);
        helper.assertTrue(l.henryDay() == today + LegacyRules.HENRY_RETURN_DAYS, "back in three days: " + l.henryDay() + " vs " + today);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "legacy_rank")
    public static void researchRaisesTheRankWithItsGear(GameTestHelper helper) {
        JournalTests.Witness w = witness(helper, new BlockPos(2, 1, 2));
        Legacies.set(w, Legacy.NONE.withRank(1));
        Archive a = Archive.EMPTY;
        for (int i = 0; i < 5; i++) a = a.finish("lore:test_" + i);
        Legacies.setArchive(w, a);
        int rank = LegacyOrder.checkRank(w);
        boolean ring = w.getInventory().contains(s -> s.is(AllItems.MEN_OF_LETTERS_RING.get()));
        boolean wears = LegacyOrder.wears(w, AllItems.MEN_OF_LETTERS_RING.get());
        boolean adv = org.papiricoh.supernaturalcraft.author.AuthorWorld.done(w, LegacyRules.advancement(2));
        w.setItemSlot(EquipmentSlot.HEAD, new ItemStack(AllItems.SPELLWRIGHTS_SPECTACLES.get()));
        OrderGear.second(w);
        boolean sight = w.hasEffect(AllMobEffects.SECOND_SIGHT);
        gone(helper, w);
        helper.assertTrue(rank == 2, "Initiate after five, got " + rank);
        helper.assertTrue(ring && wears, "the ring, carried counts as worn");
        helper.assertTrue(adv, "advancement legacy_2");
        helper.assertTrue(sight, "the spectacles give Second Sight");
        helper.succeed();
    }

    // --- the bunker ----------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = "legacy_door")
    public static void onlyTheKeyOpensTheDoor(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        BlockPos lower = new BlockPos(5, 1, 5);
        BlockState closed = AllBlocks.BUNKER_DOOR.get().defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        helper.setBlock(lower, closed.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(lower.above(), closed.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(lower);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.SOUTH, abs, false);
        ServerPlayer hand = fake(helper, ItemStack.EMPTY);
        level.getBlockState(abs).useWithoutItem(level, hand, hit);
        boolean byHand = level.getBlockState(abs).getValue(DoorBlock.OPEN);
        helper.setBlock(lower.east(), Blocks.REDSTONE_BLOCK);
        boolean byRedstone = level.getBlockState(abs).getValue(DoorBlock.OPEN);
        ServerPlayer keyed = fake(helper, new ItemStack(AllItems.BUNKER_KEY.get()));
        level.getBlockState(abs).useItemOn(keyed.getMainHandItem(), level, keyed, InteractionHand.MAIN_HAND, hit);
        boolean byKey = level.getBlockState(abs).getValue(DoorBlock.OPEN) && level.getBlockState(abs.above()).getValue(DoorBlock.OPEN);
        level.getBlockState(abs).useWithoutItem(level, hand, hit);
        boolean shut = !level.getBlockState(abs).getValue(DoorBlock.OPEN);
        float outsider = level.getBlockState(abs).getDestroyProgress(hand, level, abs);
        Legacies.set(keyed, Legacy.NONE.withRank(1));
        boolean member = BunkerProtection.mayBreak(keyed);
        helper.assertTrue(!byHand, "a bare hand doesn't open it");
        helper.assertTrue(!byRedstone, "nor redstone");
        helper.assertTrue(byKey, "the key opens both halves");
        helper.assertTrue(shut, "anyone can pull it shut");
        helper.assertTrue(outsider == 0f && !BunkerProtection.mayBreak(hand), "an outsider can't break it");
        helper.assertTrue(member, "a member can");
        helper.assertTrue(level.getBlockState(abs).getBlock() instanceof BunkerDoorBlock, "still the door");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SPIRE, batch = "legacy_bunker", timeoutTicks = 200)
    public static void theBunkerBuildsWithHenryAtHome(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // High enough that the plan's lowest level (41 down) stays above the world's floor.
        BlockPos origin = helper.absolutePos(new BlockPos(32, 50, 44));
        BunkerBuilder.placeAt(level, origin, 0);
        BunkerLocator.Site site = new BunkerLocator.Site(origin, 0);
        CompoundTag before = adoptBunker(helper, origin, true);
        int desks = 0;
        for (int[] d : BunkerLayout.DESKS) if (level.getBlockState(BunkerBuilder.at(origin, 0, d)).is(AllBlocks.RESEARCH_DESK.get())) desks++;
        boolean table = level.getBlockState(BunkerWorld.mapTable(site)).is(AllBlocks.MAP_TABLE.get());
        boolean door = level.getBlockState(BunkerWorld.door(site)).is(AllBlocks.BUNKER_DOOR.get())
                && level.getBlockState(BunkerWorld.door(site).above()).is(AllBlocks.BUNKER_DOOR.get());
        boolean trap = level.getBlockState(BunkerBuilder.at(origin, 0, BunkerLayout.TRAP)).is(AllBlocks.DEVILS_TRAP.get());
        boolean emblem = level.getBlockState(BunkerBuilder.at(origin, 0, BunkerLayout.EMBLEM_AT)).is(AllBlocks.MEN_OF_LETTERS_EMBLEM.get());
        JournalTests.Witness w = witness(helper, new BlockPos(32, 50 + BunkerLayout.HENRY[1], 44 + BunkerLayout.HENRY[2] + 3));
        Legacies.set(w, Legacy.NONE.withRank(1));
        BunkerWorld.tickHenry(level, BunkerSavedData.get(level), site, List.of(w));
        BunkerWorld.tickHenry(level, BunkerSavedData.get(level), site, List.of(w));
        List<HenryEntity> home = BunkerWorld.homeHenries(level, site);
        boolean henry = home.size() == 1 && home.get(0).blockPosition().equals(BunkerWorld.henrySpot(site));
        BunkerSavedData.get(level).restore(before);
        gone(helper, w);
        home.forEach(Entity::discard);
        String asPlanned = builtAsPlanned(level, origin);
        String decorated = decorated(level, origin);
        for (Entity e : level.getEntitiesOfClass(Entity.class, net.minecraft.world.phys.AABB.of(BunkerBuilder.box(origin, 0)),
                e -> e instanceof net.minecraft.world.entity.decoration.HangingEntity || e instanceof net.minecraft.world.entity.decoration.ArmorStand)) {
            e.discard();
        }
        helper.assertTrue(asPlanned == null, "built as planned: " + asPlanned);
        helper.assertTrue(decorated == null, "decorated: " + decorated);
        helper.assertTrue(desks == 4, "four research desks, found " + desks);
        helper.assertTrue(table, "the map table");
        helper.assertTrue(door, "the door, both halves");
        helper.assertTrue(trap, "the dungeon's devil's trap");
        helper.assertTrue(emblem, "the emblem in the war room floor");
        helper.assertTrue(henry, "one Henry at home by the table, found " + home.size());
        helper.succeed();
    }

    /** Null if every block of the plan stands in the world as planned (falling blocks over air excepted), else what differs. */
    private static String builtAsPlanned(ServerLevel level, BlockPos origin) {
        java.util.Set<BlockPos> trap = new java.util.HashSet<>();
        BlockPos t = BunkerBuilder.at(origin, 0, BunkerLayout.TRAP);
        for (int part = 0; part < 9; part++) trap.add(t.offset(org.papiricoh.supernaturalcraft.hunter.DevilsTrapBlock.dx(part), 0,
                org.papiricoh.supernaturalcraft.hunter.DevilsTrapBlock.dz(part)));
        int wrong = 0;
        String first = null;
        for (BunkerLayout.Cell c : BunkerLayout.cells()) {
            BlockPos at = BunkerBuilder.at(origin, 0, new int[]{c.x(), c.y(), c.z()});
            if (trap.contains(at)) continue;
            BlockState want = org.papiricoh.supernaturalcraft.author.CabinBuilder.state(c.state()), got = level.getBlockState(at);
            if (got.equals(want) || want.getBlock() instanceof net.minecraft.world.level.block.FallingBlock) continue;
            wrong++;
            if (first == null) first = c + " is " + got;
        }
        return wrong == 0 ? null : wrong + " blocks differ, first " + first;
    }

    /** Null if the decoration is in: frames, paintings and stands hung, banners patterned, loot tables set, signs written. */
    private static String decorated(ServerLevel level, BlockPos origin) {
        net.minecraft.world.phys.AABB area = net.minecraft.world.phys.AABB.of(BunkerBuilder.box(origin, 0));
        long frames = 0, paintings = 0, stands = 0;
        for (var d : BunkerLayout.decor()) {
            switch (d.kind()) {
                case ITEM_FRAME -> frames++;
                case PAINTING -> paintings++;
                case ARMOR_STAND -> stands++;
                default -> {
                    BlockPos at = BunkerBuilder.at(origin, 0, new int[]{d.x(), d.y(), d.z()});
                    var be = level.getBlockEntity(at);
                    String fail = switch (d.kind()) {
                        case BANNER -> be instanceof net.minecraft.world.level.block.entity.BannerBlockEntity b && b.getPatterns().layers().stream()
                                .anyMatch(l -> l.pattern().unwrapKey().map(k -> k.location().getPath().equals("men_of_letters")).orElse(false))
                                ? null : "banner at " + at.toShortString() + " without the order's pattern";
                        case LOOT -> be instanceof net.minecraft.world.RandomizableContainer r && r.getLootTable() != null ? null : "no loot at " + at.toShortString();
                        case SIGN -> be instanceof net.minecraft.world.level.block.entity.SignBlockEntity sg
                                && !sg.getFrontText().getMessage(1, false).getString().isEmpty() ? null : "blank sign at " + at.toShortString();
                        case VAULT -> be instanceof net.minecraft.world.level.block.entity.vault.VaultBlockEntity v
                                && v.getConfig().keyItem().is(AllItems.BUNKER_KEY.get()) ? null : "vault at " + at.toShortString() + " without the key";
                        default -> null;
                    };
                    if (fail != null) return fail;
                }
            }
        }
        long hungFrames = level.getEntitiesOfClass(net.minecraft.world.entity.decoration.ItemFrame.class, area).size();
        long hungPaintings = level.getEntitiesOfClass(net.minecraft.world.entity.decoration.Painting.class, area).size();
        long standing = level.getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class, area).size();
        if (hungFrames < frames || hungPaintings < paintings || standing < stands) {
            return "frames " + hungFrames + "/" + frames + ", paintings " + hungPaintings + "/" + paintings + ", stands " + standing + "/" + stands;
        }
        return null;
    }

    // --- cases ---------------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "legacy_case_solved", timeoutTicks = 200)
    public static void aCaseComesAliveAndIsSolved(GameTestHelper helper) {
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        JournalTests.Witness w = witness(helper, new BlockPos(24, 1, 6));
        BlockPos site = helper.absolutePos(new BlockPos(24, 1, 24));
        CaseFile file = new CaseFile(0, site, "barn", SupernaturalCraft.asResource("vampire"), "", 1, level.getGameTime(), CaseFile.OPEN);
        Legacies.set(w, Legacy.NONE.withRank(1).withCases(l -> {
            l.add(file);
            return l;
        }));
        CaseSites.tick(level, List.of(w));
        CaseSavedData.Site s = CaseSavedData.get(level).get(w.getUUID(), 0);
        helper.assertTrue(s != null, "the site came alive with its hunter near");
        BlockPos centre = s.centre;
        boolean hay = level.getBlockState(centre.offset(-2, 0, -3)).is(Blocks.HAY_BLOCK);
        int targets = s.targets.size();
        boolean active = CaseOffice.open(Legacies.get(w)).state() == CaseFile.ACTIVE;
        for (UUID u : List.copyOf(s.targets)) {
            if (level.getEntity(u) instanceof VampireEntity v) v.kill();
        }
        Legacy after = Legacies.get(w);
        boolean restored = !level.getBlockState(centre.offset(-2, 0, -3)).is(Blocks.HAY_BLOCK);
        boolean notes = w.getInventory().contains(st -> st.is(AllItems.FIELD_NOTES.get()));
        boolean cleared = CaseSavedData.get(level).get(w.getUUID(), 0) == null;
        gone(helper, w);
        helper.assertTrue(hay, "the barn stands");
        helper.assertTrue(targets >= 2, "a nest of vampires, got " + targets);
        helper.assertTrue(active, "the case is active");
        helper.assertTrue(after.cases().get(0).state() == CaseFile.SOLVED && after.casesSolved() == 1, "solved: " + after);
        helper.assertTrue(restored, "the barn is gone again");
        helper.assertTrue(notes, "field notes for the archive");
        helper.assertTrue(cleared, "the site is filed away");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "legacy_case_lost", timeoutTicks = 200)
    public static void aCaseLeftAloneGoesCold(GameTestHelper helper) {
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        JournalTests.Witness w = witness(helper, new BlockPos(24, 1, 6));
        BlockPos site = helper.absolutePos(new BlockPos(24, 1, 24));
        CaseFile file = new CaseFile(3, site, "graveyard", SupernaturalCraft.asResource("shapeshifter"), "hostage", 2, 0, CaseFile.OPEN);
        Legacies.set(w, Legacy.NONE.withRank(2).withCases(l -> {
            l.add(file);
            return l;
        }));
        CaseSavedData.Site s = CaseSites.materialize(level, w, file);
        boolean hostage = s.hostage != null && level.getEntity(s.hostage) != null;
        List<UUID> creatures = List.copyOf(s.targets);
        w.moveTo(w.getX() + 500, w.getY(), w.getZ());
        s.lastNear = level.getGameTime() - CaseSites.LOST_AFTER - 1;
        CaseSites.tick(level, List.of(w));
        Legacy after = Legacies.get(w);
        boolean vanished = creatures.stream().allMatch(u -> level.getEntity(u) == null);
        gone(helper, w);
        helper.assertTrue(hostage, "a hostage at the site");
        helper.assertTrue(after.cases().get(0).state() == CaseFile.LOST && after.casesSolved() == 0, "lost: " + after);
        helper.assertTrue(vanished, "its creatures are gone");
        helper.succeed();
    }

    // --- the monsters --------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = "legacy_vampire")
    public static void onlyBeheadingKeepsAVampireDown(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        VampireEntity v = helper.spawn(AllEntities.VAMPIRE.get(), new BlockPos(5, 1, 5));
        ServerPlayer club = fake(helper, new ItemStack(Items.STICK));
        v.hurt(helper.getLevel().damageSources().playerAttack(club), 1000f);
        boolean survived = v.isAlive() && v.downed() && v.getHealth() >= 1f;
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(3, 1, 5));
        ServerPlayer blooded = fake(helper, new ItemStack(AllItems.DEAD_MANS_BLOOD.get()));
        VampireEntity w = helper.spawn(AllEntities.VAMPIRE.get(), new BlockPos(7, 1, 5));
        boolean bit = w.doHurtTarget(pig) && pig.hasEffect(AllMobEffects.BLEEDING);
        AllItems.DEAD_MANS_BLOOD.get().interactLivingEntity(blooded.getMainHandItem(), blooded, w, InteractionHand.MAIN_HAND);
        boolean stunned = w.stunned();
        ServerPlayer sword = fake(helper, new ItemStack(Items.IRON_SWORD));
        v.invulnerableTime = 0;
        v.hurt(helper.getLevel().damageSources().playerAttack(sword), 1000f);
        boolean dead = !v.isAlive();
        w.discard();
        pig.discard();
        helper.assertTrue(survived, "a club only drops it");
        helper.assertTrue(bit, "its bite bleeds");
        helper.assertTrue(stunned, "dead man's blood stuns it");
        helper.assertTrue(dead, "a blade takes its head");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "legacy_werewolf")
    public static void onlySilverKillsAWerewolf(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        WerewolfEntity wolf = helper.spawn(AllEntities.WEREWOLF.get(), new BlockPos(5, 1, 5));
        wolf.forceForm(true);
        boolean wolfForm = wolf.wolfForm();
        ServerPlayer iron = fake(helper, new ItemStack(Items.IRON_SWORD));
        wolf.hurt(helper.getLevel().damageSources().playerAttack(iron), 1000f);
        boolean fled = wolf.isAlive() && wolf.fleeing() && wolf.getHealth() <= 1.01f;
        ServerPlayer silver = fake(helper, new ItemStack(AllItems.SILVER_MACHETE.get()));
        wolf.invulnerableTime = 0;
        wolf.hurt(helper.getLevel().damageSources().playerAttack(silver), 1000f);
        boolean dead = !wolf.isAlive();
        helper.assertTrue(wolfForm, "the wolf by night");
        helper.assertTrue(fled, "iron leaves it at 1 health, fleeing");
        helper.assertTrue(dead, "silver kills it");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "legacy_shapeshifter", timeoutTicks = 100)
    public static void aShapeshifterIsShownForWhatItIs(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ShapeshifterEntity a = helper.spawn(AllEntities.SHAPESHIFTER.get(), new BlockPos(3, 1, 5));
        a.setNoAi(true);
        a.setDisguise("villager:minecraft:farmer");
        float max = a.getHealth();
        ServerPlayer stick = fake(helper, new ItemStack(Items.STICK));
        a.hurt(helper.getLevel().damageSources().playerAttack(stick), 10f);
        float lost = max - a.getHealth();
        ServerPlayer silver = fake(helper, new ItemStack(AllItems.SILVER_MACHETE.get()));
        a.invulnerableTime = 0;
        a.hurt(helper.getLevel().damageSources().playerAttack(silver), 1f);
        boolean shed = !a.disguised() && a.revealed();
        helper.assertTrue(lost > 2f && lost < 5f, "disguised, it takes a fraction: " + lost);
        helper.assertTrue(shed, "silver shows it");
        ShapeshifterEntity b = helper.spawn(AllEntities.SHAPESHIFTER.get(), new BlockPos(8, 1, 5));
        b.setNoAi(true);
        b.setDisguise("villager:minecraft:librarian");
        JournalTests.Witness seer = witness(helper, new BlockPos(5, 1, 5));
        seer.addEffect(new MobEffectInstance(AllMobEffects.SECOND_SIGHT, 200));
        helper.runAfterDelay(25, () -> {
            boolean seen = !b.disguised();
            gone(helper, seer);
            helper.assertTrue(seen, "Second Sight sees through it");
            helper.succeed();
        });
    }
}
