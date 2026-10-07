package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.author.AuthorDialogue;
import org.papiricoh.supernaturalcraft.author.AuthorEvents;
import org.papiricoh.supernaturalcraft.author.AuthorNpcEntity;
import org.papiricoh.supernaturalcraft.author.AuthorRewards;
import org.papiricoh.supernaturalcraft.author.AuthorSavedData;
import org.papiricoh.supernaturalcraft.author.AuthorServerHandlers;
import org.papiricoh.supernaturalcraft.author.AuthorSite;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.author.AuthorsPenItem;
import org.papiricoh.supernaturalcraft.author.CabinBuilder;
import org.papiricoh.supernaturalcraft.author.Chronicle;
import org.papiricoh.supernaturalcraft.author.FindTheAuthorEffect;
import org.papiricoh.supernaturalcraft.author.Manuscript;
import org.papiricoh.supernaturalcraft.author.SamsAmulet;
import org.papiricoh.supernaturalcraft.author.TypewriterBlock;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckEntity;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMapDecorations;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.List;

/**
 * The Author's world: his cabin (no block entities, so his arena can erase and restore it), when he is at home, the
 * spell that finds him and the page that teaches it, the dialogue that begins his test, and what passing it gives.
 * Every test that touches the world's {@link AuthorSavedData} has its own batch and puts the record back.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class AuthorWorldTests {

    /** The cabin's floor centre in the ARENA template, its door looking south. */
    private static final BlockPos CABIN = new BlockPos(24, 1, 18);

    private static AuthorSite.Site cabin(GameTestHelper helper, int turn) {
        SNGameTests.floor(helper, 48, 48);
        BlockPos origin = helper.absolutePos(CABIN);
        CabinBuilder.placeAt(helper.getLevel(), origin, turn);
        return new AuthorSite.Site(origin, turn);
    }

    /** Points the world's record at the test's cabin; returns the record as it was. */
    private static CompoundTag adopt(GameTestHelper helper, AuthorSite.Site site, boolean cast) {
        AuthorSavedData data = AuthorSavedData.get(helper.getLevel());
        CompoundTag before = data.snapshot();
        data.restore(new CompoundTag());
        data.setCabin(site.origin(), site.rotation(), true);
        data.setSpellCast(cast);
        return before;
    }

    private static void putBack(GameTestHelper helper, CompoundTag before, Entity... gone) {
        ServerLevel level = helper.getLevel();
        AuthorSavedData.get(level).restore(before);
        for (AuthorNpcEntity n : level.getEntitiesOfClass(AuthorNpcEntity.class, new AABB(helper.absolutePos(CABIN)).inflate(80))) n.discard();
        for (Entity e : gone) {
            if (e instanceof ServerPlayer p) level.removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
            else if (e != null) e.discard();
        }
    }

    private static JournalTests.Witness witness(GameTestHelper helper, Vec3 at) {
        JournalTests.Witness w = new JournalTests.Witness(helper.getLevel());
        w.moveTo(at.x, at.y, at.z, 0, 0);
        helper.getLevel().addNewPlayer(w);
        return w;
    }

    private static List<AuthorNpcEntity> npcs(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(AuthorNpcEntity.class, new AABB(helper.absolutePos(CABIN)).inflate(40));
    }

    private static void beatEverythingBeforeHim(ServerPlayer p) {
        for (BossProgression.Boss b : BossProgression.Boss.values()) if (b != BossProgression.Boss.CHUCK) ChorusRewards.award(p, b.advancement);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "author_cabin")
    public static void theCabinHasNoBlockEntitiesAndAllItsFurniture(GameTestHelper helper) {
        AuthorSite.Site site = cabin(helper, 1);
        ServerLevel level = helper.getLevel();
        BoundingBox box = CabinBuilder.box(site.origin(), site.rotation());
        int blocks = 0;
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            helper.assertTrue(level.getBlockEntity(p) == null, "a block entity at " + p.toShortString() + ": " + level.getBlockState(p));
            if (!level.getBlockState(p).isAir()) blocks++;
        }
        helper.assertTrue(blocks > 400, "a whole cabin, got " + blocks + " blocks");
        BlockState tw = level.getBlockState(AuthorWorld.typewriter(site));
        helper.assertTrue(tw.is(AllBlocks.TYPEWRITER.get()), "the typewriter on the desk, found " + tw);
        // Turned once clockwise, the keys that faced south (the chair) face west.
        helper.assertTrue(tw.getValue(TypewriterBlock.FACING) == net.minecraft.core.Direction.WEST, "the typewriter turned with the cabin: " + tw);
        helper.assertTrue(level.getBlockState(AuthorWorld.chair(site)).is(net.minecraft.world.level.block.Blocks.SPRUCE_STAIRS), "his chair");
        helper.assertTrue(level.getBlockState(AuthorWorld.chair(site).above()).isAir(), "room to sit");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "author_npc_absent")
    public static void heIsNotHomeBeforeTheSpell(GameTestHelper helper) {
        AuthorSite.Site site = cabin(helper, 0);
        CompoundTag before = adopt(helper, site, false);
        JournalTests.Witness w = witness(helper, Vec3.atBottomCenterOf(AuthorWorld.centre(site)));
        AuthorWorld.tick(helper.getLevel(), List.of(w));
        AuthorWorld.tick(helper.getLevel(), List.of(w));
        boolean none = npcs(helper).isEmpty();
        putBack(helper, before, w);
        helper.assertTrue(none, "nobody at the desk before \"Find the Author\"");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "author_npc_home")
    public static void afterTheSpellHeIsAtHisDesk(GameTestHelper helper) {
        AuthorSite.Site site = cabin(helper, 2);
        CompoundTag before = adopt(helper, site, true);
        JournalTests.Witness w = witness(helper, Vec3.atBottomCenterOf(AuthorWorld.centre(site)).add(0, 0, 30));
        AuthorWorld.tick(helper.getLevel(), List.of(w));
        AuthorWorld.tick(helper.getLevel(), List.of(w));
        List<AuthorNpcEntity> found = npcs(helper);
        boolean one = found.size() == 1;
        boolean seated = one && found.getFirst().seated() && found.getFirst().blockPosition().equals(AuthorWorld.chair(site));
        boolean safe = one && !found.getFirst().hurt(helper.getLevel().damageSources().generic(), 1000) && found.getFirst().isAlive();
        putBack(helper, before, w);
        helper.assertTrue(one, "exactly one Author once the spell is cast and a hunter is near, found " + found.size());
        helper.assertTrue(seated, "he sits at his desk");
        helper.assertTrue(safe, "he cannot be hurt");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "author_spell")
    public static void theSpellWaitsForEveryEnemyThenDrawsTheMap(GameTestHelper helper) {
        AuthorSite.Site site = cabin(helper, 0);
        CompoundTag before = adopt(helper, site, false);
        JournalTests.Witness w = witness(helper, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(5, 1, 40))));
        try {
            helper.assertFalse(AuthorWorld.readyForHim(w), "a fresh hunter is not ready");
            helper.assertFalse(AuthorEvents.offerPage(w), "no page before the last enemy falls");
            for (BossProgression.Boss b : BossProgression.Boss.values()) {
                if (b == BossProgression.Boss.CHUCK || b == BossProgression.Boss.LUCIFER_UNCAGED) continue;
                ChorusRewards.award(w, b.advancement);
            }
            helper.assertFalse(AuthorWorld.readyForHim(w), "one enemy short");
            helper.assertFalse(hasPage(w), "still no page");
            ChorusRewards.award(w, BossProgression.Boss.LUCIFER_UNCAGED.advancement);
            helper.assertTrue(AuthorWorld.readyForHim(w), "every enemy before him beaten");
            helper.assertTrue(hasPage(w), "the last enemy's fall hands over the page of Find the Author");
            helper.assertFalse(AuthorEvents.offerPage(w), "only one page");
            helper.assertTrue(FindTheAuthorEffect.cast(helper.getLevel(), w, w.position()), "the spell works");
            helper.assertTrue(AuthorSavedData.get(helper.getLevel()).spellCast(), "the world expects him now");
            ItemStack map = ItemStack.EMPTY;
            for (ItemStack s : w.getInventory().items) if (s.is(net.minecraft.world.item.Items.FILLED_MAP)) map = s;
            helper.assertFalse(map.isEmpty(), "a map in the caster's hands");
            MapDecorations decos = map.get(DataComponents.MAP_DECORATIONS);
            MapDecorations.Entry mark = decos == null ? null : decos.decorations().get("author");
            BlockPos centre = AuthorWorld.centre(site);
            helper.assertTrue(mark != null && mark.type().is(AllMapDecorations.AUTHOR_CABIN.getKey()), "the map carries his mark");
            helper.assertTrue(mark.x() == centre.getX() && mark.z() == centre.getZ(), "the mark is on the cabin: " + mark.x() + "," + mark.z());
        } finally {
            putBack(helper, before, w);
        }
        helper.succeed();
    }

    private static boolean hasPage(ServerPlayer p) {
        for (ItemStack s : p.getInventory().items) {
            if (s.is(AllItems.SPELL_PAGE.get()) && AuthorEvents.SPELL.equals(s.get(AllDataComponents.BOWL_SPELL.get()))) return true;
        }
        return false;
    }

    @GameTest(template = SNGameTests.ARENA, batch = "author_rewards")
    public static void eachHunterIsRewardedOnlyForTheirFirstVictory(GameTestHelper helper) {
        AuthorSite.Site site = cabin(helper, 0);
        CompoundTag before = adopt(helper, site, true);
        Vec3 at = Vec3.atBottomCenterOf(AuthorWorld.centre(site));
        JournalTests.Witness w = witness(helper, at.add(2, 0, 0));
        JournalTests.Witness late = witness(helper, at.add(-2, 0, 0));
        try {
            beatEverythingBeforeHim(w);
            AuthorRewards.victory(helper.getLevel(), List.of(w), at, false);
            helper.assertTrue(count(w, AllItems.THE_END_MANUSCRIPT.get()) == 1, "The End");
            helper.assertTrue(count(w, AllItems.AUTHORS_PEN.get()) == 1, "the Pen");
            helper.assertTrue(count(w, AllItems.SAMS_AMULET.get()) == 1, "Sam's amulet");
            helper.assertTrue(AuthorWorld.done(w, "main/the_end"), "the last advancement");
            Manuscript text = null;
            for (ItemStack s : w.getInventory().items) if (s.is(AllItems.THE_END_MANUSCRIPT.get())) text = s.get(AllDataComponents.MANUSCRIPT.get());
            helper.assertTrue(text != null && text.hunter().equals(w.getGameProfile().getName()), "it is the hunter's story");
            helper.assertTrue(text.lines().getLast().equals(Chronicle.THE_END) && String.join(" ", text.lines()).toLowerCase().contains("lucifer"),
                    "it tells what they beat, and ends");
            AuthorRewards.victory(helper.getLevel(), List.of(w), at, false);
            helper.assertTrue(count(w, AllItems.AUTHORS_PEN.get()) == 1, "nothing twice");
            AuthorRewards.victory(helper.getLevel(), List.of(w, late), at, true);
            helper.assertTrue(count(w, AllItems.AUTHORS_PEN.get()) == 1, "another draft gives its asker nothing more");
            helper.assertTrue(count(late, AllItems.AUTHORS_PEN.get()) == 1 && count(late, AllItems.THE_END_MANUSCRIPT.get()) == 1,
                    "a hunter's first victory rewards them, even in someone else's draft");
        } finally {
            putBack(helper, before, w, late);
        }
        helper.succeed();
    }

    private static int count(ServerPlayer p, net.minecraft.world.item.Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(item)) n += s.getCount();
        return n;
    }

    @GameTest(template = SNGameTests.ARENA, batch = "author_ready", timeoutTicks = 200)
    public static void sayingReadyBeginsTheTestInTheCabin(GameTestHelper helper) {
        BossTests.cleanup(helper);
        AuthorSite.Site site = cabin(helper, 0);
        CompoundTag before = adopt(helper, site, true);
        AuthorNpcEntity npc = AuthorWorld.spawnNpc(helper.getLevel(), site);
        JournalTests.Witness w = witness(helper, Vec3.atBottomCenterOf(AuthorWorld.centre(site)));
        try {
            helper.assertTrue(npc != null, "he is home");
            AuthorServerHandlers.open(npc, w);
            helper.assertTrue("hello".equals(npc.node(w.getUUID())), "he greets a stranger");
            helper.assertTrue(AuthorWorld.done(w, "main/the_author"), "meeting him is an advancement");
            helper.assertTrue(!npc.seated() && npc.talking(), "he gets up to talk");
            helper.assertTrue(AuthorServerHandlers.choose(w, npc, AuthorDialogue.DRAFT) == null, "no drafts for those who never finished");
            helper.assertTrue("who".equals(AuthorServerHandlers.choose(w, npc, "who")), "a topic");
            helper.assertTrue(AuthorServerHandlers.choose(w, npc, AuthorDialogue.BEGIN) == null, "begin only after ready");
            helper.assertTrue(AuthorDialogue.READY.equals(AuthorServerHandlers.choose(w, npc, AuthorDialogue.READY)), "ready");
            helper.assertTrue(AuthorDialogue.BEGIN.equals(AuthorServerHandlers.choose(w, npc, AuthorDialogue.BEGIN)), "begin");
            helper.assertTrue(npc.isRemoved(), "the man at home leaves the scene");
            List<ChuckEntity> chuck = helper.getLevel().getEntitiesOfClass(ChuckEntity.class, new AABB(AuthorWorld.centre(site)).inflate(6));
            helper.assertTrue(chuck.size() == 1, "the Author stands in his cabin, found " + chuck.size());
            helper.assertTrue(AuthorWorld.fightAt(helper.getLevel(), Vec3.atCenterOf(AuthorWorld.centre(site))), "a fight holds the cabin");
            AuthorWorld.tick(helper.getLevel(), List.of(w));
            helper.assertTrue(npcs(helper).isEmpty(), "no man at the desk while he is being fought");
        } finally {
            putBack(helper, before, w);
            BossTests.cleanup(helper);
        }
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM)
    public static void thePenRewritesACowButNeverAHunter(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        Mob cow = helper.spawn(EntityType.COW, new BlockPos(5, 1, 5));
        Mob pig = AuthorsPenItem.rewriteCreature(cow);
        helper.assertTrue(pig != null && pig.getType() == EntityType.PIG && cow.isRemoved(), "a cow becomes the next of its tier");
        JournalTests.Witness w = new JournalTests.Witness(helper.getLevel());
        helper.assertTrue(AuthorsPenItem.rewriteCreature(w) == null, "never a person");
        pig.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM)
    public static void samsAmuletGivesFourHeartsWhileWorn(GameTestHelper helper) {
        JournalTests.Witness w = new JournalTests.Witness(helper.getLevel());
        double base = w.getAttributeValue(Attributes.MAX_HEALTH);
        w.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(AllItems.SAMS_AMULET.get()));
        SamsAmulet.hearts(w, !SamsAmulet.worn(w).isEmpty());
        helper.assertTrue(w.getAttributeValue(Attributes.MAX_HEALTH) == base + SamsAmulet.BONUS, "four more hearts");
        w.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
        SamsAmulet.hearts(w, !SamsAmulet.worn(w).isEmpty());
        helper.assertTrue(w.getAttributeValue(Attributes.MAX_HEALTH) == base, "gone when it comes off");
        helper.assertTrue(SamsAmulet.isPowerful(org.papiricoh.supernaturalcraft.registry.AllEntities.AZAZEL.get())
                && !SamsAmulet.isPowerful(EntityType.COW), "it sees the powerful");
        helper.succeed();
    }
}
