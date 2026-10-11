package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.papiricoh.supernaturalcraft.legacy.research.NoteLedger;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.BowlContents;
import org.papiricoh.supernaturalcraft.bowl.BowlLiquid;
import org.papiricoh.supernaturalcraft.bowl.Dose;
import org.papiricoh.supernaturalcraft.bowl.SpellBowlBlockEntity;
import org.papiricoh.supernaturalcraft.bowl.effect.ApplyEffectEffect;
import org.papiricoh.supernaturalcraft.journal.HunterLogs;
import org.papiricoh.supernaturalcraft.legacy.Archive;
import org.papiricoh.supernaturalcraft.legacy.Legacies;
import org.papiricoh.supernaturalcraft.legacy.Legacy;
import org.papiricoh.supernaturalcraft.legacy.artifact.ArtifactData;
import org.papiricoh.supernaturalcraft.legacy.artifact.ArtifactTraits;
import org.papiricoh.supernaturalcraft.legacy.artifact.Artifacts;
import org.papiricoh.supernaturalcraft.legacy.gen.GeneratedFormula;
import org.papiricoh.supernaturalcraft.legacy.gen.GeneratedRite;
import org.papiricoh.supernaturalcraft.legacy.research.CreatureFiles;
import org.papiricoh.supernaturalcraft.legacy.research.FieldNotesItem;
import org.papiricoh.supernaturalcraft.legacy.research.ResearchBoard;
import org.papiricoh.supernaturalcraft.legacy.research.ResearchMenu;
import org.papiricoh.supernaturalcraft.legacy.research.ResearchServerHandlers;
import org.papiricoh.supernaturalcraft.legacy.research.ResearchService;
import org.papiricoh.supernaturalcraft.legacy.research.ResearchSlot;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;
import org.papiricoh.supernaturalcraft.magic.spell.SigilKind;
import org.papiricoh.supernaturalcraft.magic.spell.SigilLookup;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellCaster;
import org.papiricoh.supernaturalcraft.network.ResearchActionPayload;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** The Men of Letters' research (v0.17): the desk, paying and waiting, formulas, rites, artifacts and creature files. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class ResearchTests {

    private static final BlockPos DESK = new BlockPos(2, 1, 2);
    private static final BlockPos STAND = new BlockPos(3, 1, 2);
    private static final String ZOMBIE = "creature:minecraft:zombie";

    private static ServerPlayer member(GameTestHelper helper, ServerPlayer p, int rank) {
        p.setGameMode(GameType.SURVIVAL);
        Vec3 v = helper.absoluteVec(STAND.getBottomCenter());
        p.moveTo(v.x, v.y, v.z, 0, 0);
        Legacies.set(p, Legacy.NONE.withRank(rank));
        Legacies.setArchive(p, Archive.EMPTY);
        ManaManager.get(p).setMana(100);
        return p;
    }

    private static ServerPlayer fake(GameTestHelper helper, int rank) {
        return member(helper, FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "sn-test-scholar")), rank);
    }

    private static void give(ServerPlayer p, ItemStack... stacks) {
        for (ItemStack s : stacks) p.getInventory().add(s);
    }

    private static int count(ServerPlayer p, String noteTopic) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.getItem() instanceof FieldNotesItem && FieldNotesItem.topic(s).equals(noteTopic)) n += s.getCount();
        }
        return n;
    }

    /** Pulls every running research's end to now. */
    private static void hurry(ServerPlayer p) {
        long now = p.level().getGameTime();
        Archive a = Legacies.archive(p);
        List<ResearchSlot> slots = new ArrayList<>();
        for (ResearchSlot s : a.slots()) slots.add(new ResearchSlot(s.topic(), s.start(), now));
        Legacies.setArchive(p, a.withSlots(slots));
    }

    @GameTest(template = SNGameTests.SMALL, batch = "research")
    public static void paidResearchFinishesByGameTime(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        helper.setBlock(DESK, AllBlocks.RESEARCH_DESK.get().defaultBlockState());
        JournalTests.Witness p = new JournalTests.Witness(helper.getLevel());
        member(helper, p, 0);
        HunterLogs.get(p).see(ResourceLocation.withDefaultNamespace("zombie"));

        // A stranger cannot open the desk.
        BlockHitResult hit = new BlockHitResult(helper.absoluteVec(DESK.getCenter()), Direction.UP, helper.absolutePos(DESK), false);
        helper.getBlockState(DESK).useWithoutItem(helper.getLevel(), p, hit);
        helper.assertFalse(p.containerMenu instanceof ResearchMenu, "a non-member opened the desk");

        Legacies.update(p, l -> l.withRank(1));
        helper.getBlockState(DESK).useWithoutItem(helper.getLevel(), p, hit);
        helper.assertTrue(p.containerMenu instanceof ResearchMenu, "a member opens the desk");
        int menu = p.containerMenu.containerId;

        ResearchBoard.Topic t = ResearchService.find(p, ZOMBIE);
        helper.assertTrue(t != null, "the zombie's file is on the board");
        ResearchServerHandlers.action(p, new ResearchActionPayload(menu, ResearchActionPayload.START, ZOMBIE));
        helper.assertTrue(Legacies.archive(p).slots().isEmpty(), "nothing starts unpaid");

        // Loose notes carried to the desk are filed in the Archive when it opens (v0.19), and paid from there.
        give(p, FieldNotesItem.stack(ZOMBIE, t.cost().noteCount() + 1), new ItemStack(Items.PAPER, t.cost().paper()),
                new ItemStack(Items.INK_SAC, t.cost().ink()), FieldNotesItem.stack(FieldNotesItem.ARCANE, 5));
        helper.getBlockState(DESK).useWithoutItem(helper.getLevel(), p, hit);
        menu = p.containerMenu.containerId;
        helper.assertTrue(count(p, ZOMBIE) == 0 && count(p, FieldNotesItem.ARCANE) == 0, "the desk files the loose notes");
        helper.assertTrue(Legacies.archive(p).notes(ZOMBIE) == t.cost().noteCount() + 1, "filed: " + Legacies.archive(p).notes());
        ResearchServerHandlers.action(p, new ResearchActionPayload(menu, ResearchActionPayload.START, ZOMBIE));
        Archive a = Legacies.archive(p);
        helper.assertTrue(a.slots().size() == 1 && a.slots().getFirst().topic().equals(ZOMBIE), "research started: " + a.slots());
        helper.assertTrue(a.notes(ZOMBIE) == 1, "paid in zombie notes only, have " + a.notes(ZOMBIE));
        helper.assertTrue(a.notes(FieldNotesItem.ARCANE) == 5, "other notes untouched");
        helper.assertTrue(p.getInventory().countItem(Items.PAPER) == 0 && p.getInventory().countItem(Items.INK_SAC) == 0, "paper and ink spent");
        helper.assertTrue(ResearchService.find(p, ZOMBIE) == null, "a running topic leaves the board");
        helper.assertTrue(ResearchService.start(p, "lore:bunker") != null, "rank 1 has a single desk");

        helper.assertTrue(ResearchService.tick(p) == 0, "not due yet");
        hurry(p);
        helper.assertTrue(ResearchService.tick(p) == 1, "due research finishes");
        a = Legacies.archive(p);
        helper.assertTrue(a.slots().isEmpty(), "the slot is free");
        helper.assertTrue(a.fileLevel(ResourceLocation.withDefaultNamespace("zombie")) == 1, "the file is at level 1");
        helper.assertTrue(a.knows(ZOMBIE), "and in the archive");
        helper.assertTrue(ResearchService.find(p, ZOMBIE) != null, "a file always has a next level");

        // Cancelling gives nothing back.
        Legacies.updateArchive(p, x -> x.withNotes(FieldNotesItem.PLACE, 10));
        give(p, new ItemStack(Items.PAPER, 5), new ItemStack(Items.INK_SAC, 5));
        helper.assertTrue(ResearchService.start(p, "lore:bunker") == null, "lore started");
        helper.assertTrue(ResearchService.cancel(p, "lore:bunker"), "cancelled");
        helper.assertTrue(Legacies.archive(p).slots().isEmpty() && !Legacies.archive(p).knows("lore:bunker"), "cancel drops it");

        // Five finished research make an Initiate.
        for (String topic : List.of("lore:order_history", "lore:bunker", "lore:henry", "creature:minecraft:zombie")) {
            ResearchService.finish(p, topic);
        }
        helper.assertTrue(Legacies.rank(p) == 2, "five research and one kind make rank 2, rank is " + Legacies.rank(p));
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "research")
    public static void aLearnedFormulaComposesAndCasts(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 3));
        ServerPlayer p = fake(helper, 1);
        p.lookAt(EntityAnchorArgument.Anchor.EYES, zombie.getEyePosition());
        ResearchService.finish(p, "formula:0");
        Archive a = Legacies.archive(p);
        GeneratedFormula f = a.formula(0);
        helper.assertTrue(f != null, "formula 0 was worked out");
        helper.assertTrue(ManaManager.get(p).knows(f.id()), "and learnt");
        helper.assertTrue(a.counter("formula") == 1 && ResearchService.find(p, "formula:1") != null, "the next formula is on offer");
        var access = helper.getLevel().registryAccess();
        helper.assertTrue(SigilLookup.get(access, a, f.id()) != null, "SigilLookup finds it");
        helper.assertTrue(SigilLookup.get(access, null, f.id()) == null, "but only in its hunter's archive");
        SigilComponent s = f.sigil();
        SigilComponent base = SigilLookup.get(access, null, f.base());
        helper.assertTrue(base != null && base.kind() == s.kind() && base.behavior().equals(s.behavior()), "same kind and behaviour as its base");

        ResourceLocation form = s.kind() == SigilKind.FORM ? f.id() : SupernaturalCraft.asResource("touch");
        ResourceLocation effect = s.kind() == SigilKind.EFFECT ? f.id() : SupernaturalCraft.asResource("smite");
        List<ResourceLocation> mods = s.kind() == SigilKind.MODIFIER ? List.of(f.id()) : List.of();
        Spell spell = new Spell(Optional.of(form), List.of(effect), mods, "");
        helper.assertTrue(ResolvedSpell.resolve(access, a, spell) != null, "a spell with the formula resolves");
        helper.assertTrue(ResolvedSpell.resolve(access, spell) == null, "not without the archive");
        for (SigilComponent.Reagent r : s.reagents()) give(p, r.stack());
        ManaManager.get(p).setMana(150);
        SpellCaster.Result result = SpellCaster.cast(p, spell, 1f);
        helper.assertTrue(result.success(), "casting with " + f.name() + " (" + f.base() + "): " + result);

        // Someone else cannot cast it.
        ServerPlayer other = fake(helper, 1);
        helper.assertTrue(SpellCaster.cast(other, spell, 1f) == SpellCaster.Result.INCOMPLETE, "a stranger's cast fails");
        p.discard();
        other.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "research", timeoutTicks = 60)
    public static void theBowlAcceptsAGeneratedRite(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        helper.setBlock(DESK, AllBlocks.SPELL_BOWL.get().defaultBlockState());
        SpellBowlBlockEntity bowl = (SpellBowlBlockEntity) helper.getBlockEntity(DESK);
        ServerPlayer p = fake(helper, 1);
        ResearchService.finish(p, "rite:0");
        GeneratedRite rite = Legacies.archive(p).rite(GeneratedRite.id(0));
        helper.assertTrue(rite != null, "rite 0 was worked out");
        helper.assertTrue(ManaManager.get(p).knowsRite(rite.id()), "and learnt");
        helper.assertTrue(rite.effect() instanceof ApplyEffectEffect, "a tier I rite lays an effect: " + rite.effect());
        List<ItemStack> items = rite.ingredients().stream().map(id -> new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id))).toList();
        List<Dose> liquids = rite.liquids().stream().map(l -> Dose.of(BowlLiquid.fromId(l))).toList();
        bowl.setContents(BowlContents.of(items, liquids));

        ServerPlayer stranger = fake(helper, 1);
        ItemStack flint = new ItemStack(Items.FLINT_AND_STEEL);
        stranger.setItemInHand(InteractionHand.MAIN_HAND, flint);
        BowlContents before = bowl.contents();
        // The stranger has no such rite: the mix answers to nothing and blows up in their face.
        helper.assertTrue(bowl.tryLight(stranger, flint, InteractionHand.MAIN_HAND) == SpellBowlBlockEntity.LightResult.BACKLASH, "a stranger's light backlashes");
        bowl.setContents(before);

        ManaManager.get(p).setMana(150);
        ItemStack flint2 = new ItemStack(Items.FLINT_AND_STEEL);
        p.setItemInHand(InteractionHand.MAIN_HAND, flint2);
        helper.assertTrue(bowl.tryLight(p, flint2, InteractionHand.MAIN_HAND) == SpellBowlBlockEntity.LightResult.LIT, "the hunter lights their rite");
        var effect = ((ApplyEffectEffect) rite.effect()).effect();
        helper.runAfterDelay(15, () -> {
            SpellBowlBlockEntity.Outcome o = bowl.resolveRecitation(p, true, 0);
            helper.assertTrue(o == SpellBowlBlockEntity.Outcome.CAST, "the rite works, got " + o);
            helper.assertTrue(p.hasEffect(effect), "its effect was laid");
            helper.assertTrue(bowl.contents().isEmpty(), "the bowl was consumed");
            p.discard();
            stranger.discard();
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.SMALL, batch = "research")
    public static void researchIdentifiesAnArtifact(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ServerPlayer p = fake(helper, 1);
        ItemStack artifact = Artifacts.roll(424242L, 0);
        ArtifactData d = Artifacts.data(artifact);
        helper.assertTrue(d != null && !d.identified(), "rolled unidentified");
        p.setItemInHand(InteractionHand.OFF_HAND, artifact);
        String topic = "artifact:424242";
        ResearchBoard.Topic t = ResearchService.find(p, topic);
        helper.assertTrue(t != null && t.tier() == 1, "a common artifact is a tier I topic");
        Legacies.updateArchive(p, x -> x.withNotes(FieldNotesItem.RELIC, t.cost().noteCount()));
        give(p, new ItemStack(Items.PAPER, 9), new ItemStack(Items.INK_SAC, 9));
        helper.assertTrue(ResearchService.start(p, topic) == null, "started");
        hurry(p);
        ResearchService.tick(p);
        ArtifactData after = p.getOffhandItem().get(AllDataComponents.ARTIFACT.get());
        helper.assertTrue(after != null && after.identified(), "the artifact is identified");
        helper.assertTrue(Legacies.archive(p).artifacts().stream().anyMatch(x -> x.seed() == 424242L), "and filed");
        helper.assertTrue(ResearchService.find(p, topic) == null, "and off the board");

        // Its boons work from the off hand.
        for (String boon : d.boons()) helper.assertTrue(ArtifactTraits.has(p, boon), "boon " + boon + " is active");
        if (d.boons().contains("night_eyes")) {
            ArtifactTraits.apply(p, "night_eyes");
            helper.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION), "night eyes");
        }
        ArtifactTraits.apply(p, "sixth_sense");
        helper.assertTrue(p.hasEffect(AllMobEffects.SECOND_SIGHT), "sixth sense is Second Sight");
        p.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "research_damage")
    public static void aCreatureFileBitesButNeverOnABoss(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        LivingEntity filed = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 3));
        LivingEntity plain = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 3));
        ServerPlayer scholar = fake(helper, 3);
        Legacies.setArchive(scholar, new Archive(java.util.Set.of(ZOMBIE), Map.of("minecraft:zombie", 5,
                "supernaturalcraft:azazel", 9), Map.of(), List.of(), List.of(), List.of(), Map.of()));
        ServerPlayer novice = fake(helper, 1);
        float a0 = filed.getHealth(), b0 = plain.getHealth();
        filed.hurt(helper.getLevel().damageSources().playerAttack(scholar), 4f);
        plain.hurt(helper.getLevel().damageSources().playerAttack(novice), 4f);
        float withFile = a0 - filed.getHealth(), without = b0 - plain.getHealth();
        helper.assertTrue(without > 0, "the plain hit landed");
        helper.assertTrue(withFile > without * 1.1f, "the file adds damage: " + withFile + " vs " + without);
        helper.assertTrue(withFile < without * 1.35f, "within the cap: " + withFile + " vs " + without);
        LivingEntity boss = AllEntities.AZAZEL.get().create(helper.getLevel());
        helper.assertTrue(boss != null && CreatureFiles.multiplier(scholar, boss) == 1f, "a file never helps against a boss");
        helper.assertTrue(CreatureFiles.multiplier(scholar, filed) > 1f, "but does against its creature");
        if (boss != null) boss.discard();
        scholar.discard();
        novice.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "research")
    public static void aMemberFilesLooseNotes(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ServerPlayer p = fake(helper, 1);
        ServerPlayer outsider = fake(helper, 0);
        Legacies.set(outsider, Legacy.NONE);

        // Used in the hand, a stack is filed whole; an outsider keeps theirs.
        p.setItemInHand(InteractionHand.MAIN_HAND, FieldNotesItem.stack(ZOMBIE, 3));
        p.getMainHandItem().use(helper.getLevel(), p, InteractionHand.MAIN_HAND);
        helper.assertTrue(p.getMainHandItem().isEmpty(), "the used notes leave the hand");
        helper.assertTrue(Legacies.archive(p).notes(ZOMBIE) == 3, "and are filed: " + Legacies.archive(p).notes());
        outsider.setItemInHand(InteractionHand.MAIN_HAND, FieldNotesItem.stack(ZOMBIE, 3));
        outsider.getMainHandItem().use(helper.getLevel(), outsider, InteractionHand.MAIN_HAND);
        helper.assertTrue(outsider.getMainHandItem().getCount() == 3 && Legacies.archive(outsider).notes().isEmpty(), "an outsider keeps them");

        // Walked over, loose notes are filed instead of pocketed.
        Vec3 at = helper.absoluteVec(STAND.getBottomCenter());
        ItemEntity loose = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, FieldNotesItem.stack(FieldNotesItem.RELIC, 2));
        loose.setDefaultPickUpDelay();
        helper.getLevel().addFreshEntity(loose);
        helper.assertFalse(NoteLedger.pickUp(outsider, loose), "an outsider does not file them");
        helper.assertFalse(NoteLedger.pickUp(p, loose), "not before the pick-up delay is over");
        loose.setNoPickUpDelay();
        helper.assertTrue(NoteLedger.pickUp(p, loose), "a member files them");
        helper.assertTrue(loose.isRemoved() && Legacies.archive(p).notes(FieldNotesItem.RELIC) == 2, "the item is gone and the notes filed");
        helper.assertTrue(!p.getInventory().contains(st -> st.is(AllItems.FIELD_NOTES.get())), "nothing carried");

        // What is filed can be paid out, never below none.
        Legacies.updateArchive(p, a -> a.withNotes(ZOMBIE, -5));
        helper.assertTrue(Legacies.archive(p).notes(ZOMBIE) == 0 && !Legacies.archive(p).notes().containsKey(ZOMBIE), "spent to none");
        helper.assertFalse(NoteLedger.file(outsider, ZOMBIE, 4), "an outsider files nothing");

        // The notes are saved with the Archive, and an Archive saved before them still loads.
        Archive saved = Legacies.archive(p);
        var tag = Archive.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, saved).getOrThrow();
        helper.assertTrue(Archive.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag).getOrThrow().notes().equals(saved.notes()), "notes saved");
        ((net.minecraft.nbt.CompoundTag) tag).remove("notes");
        helper.assertTrue(Archive.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag).getOrThrow().notes().isEmpty(), "an old archive loads");
        p.discard();
        outsider.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "research")
    public static void chestNotesGoStraightToTheArchive(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ServerPlayer p = fake(helper, 1);
        ServerPlayer outsider = fake(helper, 0);
        Legacies.set(outsider, Legacy.NONE);
        // The bunker's vault always holds place and arcane notes.
        LootTable vault = helper.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                SupernaturalCraft.asResource("chests/bunker_vault")));
        List<ItemStack> theirs = vault.getRandomItems(chest(helper, outsider));
        helper.assertTrue(theirs.stream().anyMatch(st -> st.is(AllItems.FIELD_NOTES.get())), "an outsider finds the notes as items");
        List<ItemStack> mine = vault.getRandomItems(chest(helper, p));
        helper.assertTrue(mine.stream().noneMatch(st -> st.is(AllItems.FIELD_NOTES.get())), "a member finds none loose: " + mine);
        Archive a = Legacies.archive(p);
        helper.assertTrue(a.notes(FieldNotesItem.PLACE) >= 4 && a.notes(FieldNotesItem.ARCANE) >= 2, "they are filed: " + a.notes());
        helper.assertTrue(!mine.isEmpty(), "the rest of the loot stays");
        p.discard();
        outsider.discard();
        helper.succeed();
    }

    private static LootParams chest(GameTestHelper helper, ServerPlayer opener) {
        return new LootParams.Builder(helper.getLevel()).withParameter(LootContextParams.ORIGIN, helper.absoluteVec(DESK.getCenter()))
                .withParameter(LootContextParams.THIS_ENTITY, opener).create(LootContextParamSets.CHEST);
    }
}
