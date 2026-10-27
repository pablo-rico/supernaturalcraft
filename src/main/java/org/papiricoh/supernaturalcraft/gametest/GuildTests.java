package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.allegiance.MobReactions;
import org.papiricoh.supernaturalcraft.chain.ChainEvents;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.ChainGates;
import org.papiricoh.supernaturalcraft.chain.ChainLife;
import org.papiricoh.supernaturalcraft.chain.ChainRank;
import org.papiricoh.supernaturalcraft.chain.ChainSavedData;
import org.papiricoh.supernaturalcraft.chain.Diplomacy;
import org.papiricoh.supernaturalcraft.chain.GuildLife;
import org.papiricoh.supernaturalcraft.chain.GuildMembers;
import org.papiricoh.supernaturalcraft.chain.GuildRules;
import org.papiricoh.supernaturalcraft.chain.GuildStandardBlock;
import org.papiricoh.supernaturalcraft.chain.GuildStandardItem;
import org.papiricoh.supernaturalcraft.chain.Officer;
import org.papiricoh.supernaturalcraft.chain.OfficerSpawner;
import org.papiricoh.supernaturalcraft.entity.demon.BlackEyedDemon;
import org.papiricoh.supernaturalcraft.network.GuildActionPayload;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/** The guilds hunters found (v0.29): founding, members, hiring, stances, the standard and the interregnum. One batch each. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public final class GuildTests {

    private static ChainSavedData reset(GameTestHelper h) {
        ChainEvents.reset(h.getLevel().getServer());
        SNGameTests.floor(h, 11, 11);
        return ChainSavedData.get(h.getLevel().getServer());
    }

    /** A hunter of no side ({@code name} only reads the test). */
    private static ServerPlayer hunter(GameTestHelper h, String name) {
        return CurseTests.mortal(h, new BlockPos(5, 1, 5), ItemStack.EMPTY);
    }

    /** A guild founded by {@code p} where the test stands (the place rules are {@code GuildsTest}'s). */
    private static ChainFaction found(GameTestHelper h, ServerPlayer p, String name) {
        ChainFaction slot = GuildLife.establish(p, name, 11, h.absolutePos(new BlockPos(5, 1, 5)));
        h.assertTrue(slot != null, "the guild is founded");
        return slot;
    }

    private static BlackEyedDemon wildDemon(GameTestHelper h) {
        BlackEyedDemon d = AllEntities.BLACK_EYED_DEMON.get().create(h.getLevel());
        d.moveTo(h.absoluteVec(new BlockPos(3, 1, 3).getBottomCenter()));
        d.setNoAi(true);
        h.getLevel().addFreshEntity(d);
        return d;
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "guild_found")
    public static void aStandardFoundsAGuildAndAFounderFoundsOnlyOne(GameTestHelper h) {
        var data = reset(h);
        ServerPlayer p = hunter(h, "Dean");
        h.assertTrue("guild_place_central".equals(GuildLife.foundRefusal(p, "Team Free Will", new BlockPos(10, 64, 10))),
                "a standard does not stand at the heart of the world");
        ChainFaction slot = found(h, p, "Team Free Will");
        h.assertTrue(slot == ChainFaction.GUILD_1 && data.awake(slot), "the first free slot wakes");
        Officer lord = data.hierarchy(slot).lord();
        h.assertTrue(lord != null && p.getUUID().equals(lord.player), "its founder sits on its throne");
        h.assertTrue(data.guild(slot).generation == 1 && data.guild(slot).hasStandard, "with its standard planted");
        h.assertTrue("guild_already_member".equals(GuildLife.foundRefusal(p, "Another One", h.absolutePos(new BlockPos(5, 1, 5)))),
                "a founder founds one guild");
        h.assertTrue(GuildMembers.of(p.server, p.getUUID()) == slot, "the index knows them");
        h.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "guild_join")
    public static void acceptingAnInvitationLeavesThePit(GameTestHelper h) {
        var data = reset(h);
        ChainGates.plant(h.getLevel().getServer(), ChainFaction.DEMON, false, 3);
        ServerPlayer founder = hunter(h, "Dean"), demon = hunter(h, "Crowley");
        ChainFaction slot = found(h, founder, "Team Free Will");
        Allegiances.set(demon, new Allegiance(Faction.DEMON, 2, 0, 0, 0, 0, -1, 0, 0, false));
        h.assertTrue(ChainLife.join(demon.server, demon, ChainRank.SOLDIER) != null, "sworn into the Pit first");
        h.assertFalse(GuildLife.act(demon, GuildActionPayload.Action.ACCEPT_INVITE, slot.id(), "", 0), "nobody joins uninvited");
        data.guild(slot).invites.put(demon.getUUID(), ChainEvents.day(demon.server) + GuildRules.INVITE_DAYS);
        h.assertTrue(GuildLife.act(demon, GuildActionPayload.Action.ACCEPT_INVITE, slot.id(), "", 0), "invited, they join");
        h.assertTrue(data.hierarchy(ChainFaction.DEMON).heldBy(demon.getUUID()) == null, "their post in the Pit is given up");
        Officer post = data.hierarchy(slot).heldBy(demon.getUUID());
        h.assertTrue(post != null && post.rank == ChainRank.SOLDIER, "they come in at the first rank");
        h.assertTrue(ChainLife.join(demon.server, demon, ChainRank.SOLDIER) == null, "and cannot swear into the Pit again");
        h.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "guild_hire")
    public static void aWildDemonHiredForItsPriceServesUpToTheCap(GameTestHelper h) {
        var data = reset(h);
        ServerPlayer p = hunter(h, "Bobby");
        ChainFaction slot = found(h, p, "Singer Salvage");
        BlackEyedDemon d = wildDemon(h);
        h.assertTrue(GuildLife.hire(p, d) == null, "no coin, no service");
        p.getInventory().add(new ItemStack(AllItems.SULFUR.get(), 16));
        p.getInventory().add(new ItemStack(Items.GOLD_INGOT, 8));
        Officer o = GuildLife.hire(p, d);
        h.assertTrue(o != null && o.faction == slot && o.rank == ChainRank.SOLDIER, "paid, it signs on at the first rank");
        h.assertTrue(OfficerSpawner.officer(d) == o, "the mob is the officer");
        h.assertTrue(p.getInventory().countItem(AllItems.SULFUR.get()) == 0 && p.getInventory().countItem(Items.GOLD_INGOT) == 0, "the price is paid");
        var hierarchy = data.hierarchy(slot);
        while (hierarchy.npcFree(ChainRank.SOLDIER)) hierarchy.recruit(ChainRank.SOLDIER, Officer.NONE, hierarchy.nextId(), "supernaturalcraft:black_eyed_demon", 0);
        p.getInventory().add(new ItemStack(AllItems.SULFUR.get(), 16));
        p.getInventory().add(new ItemStack(Items.GOLD_INGOT, 8));
        h.assertTrue(GuildLife.hire(p, wildDemon(h)) == null, "half of each rank is the hunters'");
        h.assertTrue(hierarchy.invariantsHold(), "the guild's tree holds");
        h.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "guild_desert")
    public static void aBeatenOfficerDesertsARankLowerAndABoundOneKeepsItsRank(GameTestHelper h) {
        var data = reset(h);
        var server = h.getLevel().getServer();
        ChainGates.plant(server, ChainFaction.DEMON, true, 5);
        ServerPlayer p = hunter(h, "Dean");
        ChainFaction slot = found(h, p, "Team Free Will");
        var pit = data.hierarchy(ChainFaction.DEMON);
        Officer lieutenant = pit.npcs().stream().filter(o -> o.rank == ChainRank.LIEUTENANT).findFirst().orElseThrow();
        long seed = lieutenant.nameSeed;
        Officer deserter = GuildLife.desert(server, lieutenant, p.getUUID());
        h.assertTrue(deserter != null && deserter.faction == slot && deserter.rank == ChainRank.CAPTAIN, "it deserts a rank lower");
        h.assertTrue(deserter.nameSeed == seed, "under its own name");
        h.assertTrue(pit.get(lieutenant.id) == null && pit.invariantsHold(), "the Pit mends its tree");
        Officer commander = pit.npcs().stream().filter(o -> o.rank == ChainRank.COMMANDER).findFirst().orElseThrow();
        h.assertTrue(GuildLife.desert(server, commander, p.getUUID()) == null, "a Commander is not won in a duel");
        Officer bound = GuildLife.bind(server, commander, p.getUUID());
        h.assertTrue(bound != null && bound.rank == ChainRank.COMMANDER, "a rite binds it at its rank");
        Officer lord = pit.lord();
        h.assertTrue(GuildLife.bind(server, lord, p.getUUID()) == null, "never a Lord");
        h.assertTrue(data.hierarchy(slot).invariantsHold(), "the guild's tree holds");
        h.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "guild_stance")
    public static void wildDemonsMeetAMemberAsTheirGuildStands(GameTestHelper h) {
        var data = reset(h);
        ServerPlayer p = hunter(h, "Sam"), q = hunter(h, "Eileen");
        ChainFaction slot = found(h, p, "Men of Letters");
        BlackEyedDemon d = wildDemon(h);
        h.assertFalse(MobReactions.ignores(d, p), "a new guild is the Pit's foe");
        data.diplomacy().propose(slot, ChainFaction.DEMON, Diplomacy.Stance.NEUTRAL);
        data.diplomacy().accept(ChainFaction.DEMON, slot);
        data.changed();
        h.assertTrue(MobReactions.ignores(d, p), "at peace with the Pit, its demons leave them be");
        ChainFaction other = found(h, q, "British Men of Letters");
        h.assertFalse(GuildLife.spares(p, q), "two guilds at peace still may fight");
        data.diplomacy().propose(slot, other, Diplomacy.Stance.ALLIED);
        data.diplomacy().accept(other, slot);
        data.changed();
        h.assertTrue(GuildLife.spares(p, q) && GuildLife.spares(q, p), "allies do each other no harm");
        h.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "guild_standard")
    public static void aTakenDownStandardLeavesTheGuildHomeless(GameTestHelper h) {
        var data = reset(h);
        ServerPlayer p = hunter(h, "Jody");
        ChainFaction slot = found(h, p, "Sioux Falls");
        BlockPos at = h.absolutePos(new BlockPos(5, 1, 5));
        h.getLevel().setBlock(at, AllBlocks.GUILD_STANDARD.get().defaultBlockState()
                .setValue(GuildStandardBlock.SLOT, ChainFaction.GUILDS.indexOf(slot)), 3);
        h.getLevel().removeBlock(at, false);
        h.assertFalse(data.guild(slot).hasStandard, "the guild is homeless");
        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(at).inflate(2));
        h.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(AllItems.GUILD_STANDARD.get())
                && e.getItem().get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag().getString(GuildStandardItem.SLOT).equals(slot.id())),
                "its standard drops, its guild's own");
        h.assertTrue("guild_not_here".equals(GuildLife.plantRefusal(p, slot, data.guild(slot).generation + 1, at)), "an older guild's standard is inert");
        GuildLife.plant(p, slot, at);
        h.assertTrue(data.guild(slot).hasStandard, "raised again, it has its seat back");
        drops.forEach(e -> e.discard());
        h.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "guild_interregnum")
    public static void anEmptyThroneGoesToAClaimantOrTheGuildIsNoMore(GameTestHelper h) {
        var data = reset(h);
        var server = h.getLevel().getServer();
        ServerPlayer dean = hunter(h, "Dean"), sam = hunter(h, "Sam");
        ChainFaction slot = found(h, dean, "Team Free Will");
        data.guild(slot).invites.put(sam.getUUID(), ChainEvents.day(server) + 1);
        GuildLife.act(sam, GuildActionPayload.Action.ACCEPT_INVITE, slot.id(), "", 0);
        GuildLife.leave(server, dean.getUUID());
        h.assertTrue(data.court(slot).vacant() && data.hierarchy(slot).lord() == null, "the founder gone, the throne stands empty");
        h.assertTrue(GuildLife.act(sam, GuildActionPayload.Action.PROCLAIM, "", "", 0), "a member claims it");
        GuildLife.crown(server, slot, data.court(slot).vacantUntil);
        Officer lord = data.hierarchy(slot).lord();
        h.assertTrue(lord != null && sam.getUUID().equals(lord.player) && sam.getUUID().equals(data.guild(slot).founder), "the claimant takes it");
        BlackEyedDemon d = wildDemon(h);
        sam.getInventory().add(new ItemStack(AllItems.SULFUR.get(), 16));
        sam.getInventory().add(new ItemStack(Items.GOLD_INGOT, 8));
        Officer hired = GuildLife.hire(sam, d);
        h.assertTrue(hired != null, "it hires");
        int generation = data.guild(slot).generation;
        GuildLife.leave(server, sam.getUUID());
        h.assertFalse(data.guild(slot).founded() || data.awake(slot), "its last hunter gone, the guild is no more");
        h.assertTrue(data.guild(slot).generation == generation, "the slot remembers its generation");
        h.assertTrue(OfficerSpawner.key(d) == null && d.isAlive() && d.getCustomName() == null, "its hired demon goes back to the wild, untagged");
        h.assertTrue(ChainLife.front(server).held(slot) == 0, "and its ground to its neighbours");
        ServerPlayer cas = hunter(h, "Castiel");
        h.assertTrue(found(h, cas, "Heaven's Rejects") == slot, "the slot is free for the next founder");
        h.assertTrue(data.guild(slot).generation == generation + 1, "as a new generation");
        d.discard();
        h.succeed();
    }
}
