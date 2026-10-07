package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.journal.HunterLog;
import org.papiricoh.supernaturalcraft.journal.HunterLogEvents;
import org.papiricoh.supernaturalcraft.journal.HunterLogs;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.network.ComposeSpellPayload;
import org.papiricoh.supernaturalcraft.network.JournalActionPayload;
import org.papiricoh.supernaturalcraft.network.LibraryEditPayload;
import org.papiricoh.supernaturalcraft.network.ServerPayloadHandlers;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The hunter's log behind the Hunter's Book: the bestiary, things held, shared boss credit, the library, scrolls. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class JournalTests {

    private static final BlockPos MID = new BlockPos(5, 1, 5);
    private static final String BATCH = "journal";

    private static ServerPlayer hunter(GameTestHelper helper, ItemStack held) {
        SNGameTests.floor(helper, 11, 11);
        return CurseTests.mortal(helper, MID, held);
    }

    /**
     * A real (not fake) player that is never connected: NeoForge refuses advancements to fake players,
     * and the vanilla mock player trips over payloads other mods send on login. Sends go nowhere.
     */
    static final class Witness extends ServerPlayer {
        Witness(ServerLevel level) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), "sn-test-witness"), ClientInformation.createDefault());
            this.connection = new Silent(level.getServer(), this);
            setGameMode(GameType.SURVIVAL);
        }
    }

    private static final class Silent extends ServerGamePacketListenerImpl {
        Silent(MinecraftServer server, ServerPlayer player) {
            super(server, new Connection(PacketFlow.SERVERBOUND) {
                @Override
                public void setListenerForServerboundHandshake(PacketListener listener) {
                }
            }, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
        }

        @Override
        public void send(Packet<?> packet) {
        }

        @Override
        public void send(Packet<?> packet, PacketSendListener listener) {
        }

        @Override
        public void tick() {
        }

        @Override
        public void disconnect(net.minecraft.network.chat.Component reason) {
        }
    }

    /** A witness standing at {@code at}, in the level (so the boss's search finds it). */
    private static Witness witness(GameTestHelper helper, Vec3 at) {
        Witness w = new Witness(helper.getLevel());
        w.moveTo(at.x, at.y, at.z, 0, 0);
        helper.getLevel().addNewPlayer(w);
        return w;
    }

    private static ResourceLocation id(String path) {
        return SupernaturalCraft.asResource(path);
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void slainDemonsFillTheBestiary(GameTestHelper helper) {
        ServerPlayer p = hunter(helper, ItemStack.EMPTY);
        for (int i = 0; i < 2; i++) {
            LivingEntity demon = AllEntities.BLACK_EYED_DEMON.get().create(helper.getLevel());
            demon.moveTo(p.position());
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(demon, p.damageSources().playerAttack(p)));
        }
        HunterLog log = HunterLogs.get(p);
        helper.assertTrue(log.hasSeen(id("black_eyed_demon")), "a slain demon counts as seen");
        helper.assertTrue(log.kills(id("black_eyed_demon")) == 2, "two kills, got " + log.kills(id("black_eyed_demon")));
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void carriedItemsOfTheModAreLogged(GameTestHelper helper) {
        ServerPlayer p = hunter(helper, new ItemStack(AllItems.SALT.get()));
        p.getInventory().add(new ItemStack(Items.DIRT));
        HunterLogEvents.scanInventory(p);
        HunterLog log = HunterLogs.get(p);
        helper.assertTrue(log.has(id("salt")), "salt in hand should be logged");
        helper.assertFalse(log.items().contains(ResourceLocation.withDefaultNamespace("dirt")), "only the mod's items are logged");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void advancementsReachTheSync(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer p = new Witness(helper.getLevel());
        org.papiricoh.supernaturalcraft.reward.ChorusRewards.award(p, "main/first_spell");
        helper.assertTrue(HunterLogs.doneAdvancements(p).contains(id("main/first_spell")), "a done advancement should be synced");
        helper.assertTrue(HunterLogs.get(p).dirty, "earning an advancement should schedule a sync");
        helper.succeed();
    }

    /** Amara falls: both hunters beside her count her as beaten; one far away does not. */
    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void bossCreditIsShared(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        Vec3 at = helper.absoluteVec(MID.getBottomCenter());
        Witness striker = witness(helper, at.add(2, 0, 0));
        Witness friend = witness(helper, at.add(-3, 0, 1));
        Witness faraway = witness(helper, at.add(200, 0, 0));
        try {
            LivingEntity amara = AllEntities.AMARA.get().create(helper.getLevel());
            amara.moveTo(at.x, at.y, at.z);
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(amara, striker.damageSources().playerAttack(striker)));
            var dawn = helper.getLevel().getServer().getAdvancements().get(id("main/dawn"));
            helper.assertTrue(striker.getAdvancements().getOrStartProgress(dawn).isDone(), "the striker beat Amara");
            helper.assertTrue(friend.getAdvancements().getOrStartProgress(dawn).isDone(), "so did the hunter beside them");
            helper.assertFalse(faraway.getAdvancements().getOrStartProgress(dawn).isDone(), "not someone 200 blocks away");
            helper.assertTrue(HunterLogs.get(friend).kills(id("amara")) == 1, "the friend's bestiary counts the kill");
            helper.assertTrue(HunterLogs.get(striker).kills(id("amara")) == 1, "the striker's kill is counted once");
            helper.succeed();
        } finally {
            for (ServerPlayer p : List.of(striker, friend, faraway)) {
                helper.getLevel().removePlayerImmediately(p, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            }
        }
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void entriesAreReadAndMarked(GameTestHelper helper) {
        ServerPlayer p = hunter(helper, ItemStack.EMPTY);
        ResourceLocation entry = id("black_eyes");
        ServerPayloadHandlers.journalAction(p, new JournalActionPayload(JournalActionPayload.MARK_READ, entry));
        ServerPayloadHandlers.journalAction(p, new JournalActionPayload(JournalActionPayload.TOGGLE_BOOKMARK, entry));
        HunterLog log = HunterLogs.get(p);
        helper.assertTrue(log.read().contains(entry), "the entry should be read");
        helper.assertTrue(log.bookmarks().contains(entry), "the entry should be bookmarked");
        ServerPayloadHandlers.journalAction(p, new JournalActionPayload(JournalActionPayload.TOGGLE_BOOKMARK, entry));
        helper.assertFalse(log.bookmarks().contains(entry), "toggling again removes the bookmark");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void libraryKeepsOnlyKnownSigils(GameTestHelper helper) {
        ServerPlayer p = hunter(helper, ItemStack.EMPTY);
        p.setGameMode(GameType.SURVIVAL);
        var arcana = ManaManager.get(p);
        arcana.learn(id("bolt"));
        arcana.learn(id("smite"));
        Spell known = new Spell(Optional.of(id("bolt")), List.of(id("smite")), List.of(), "Holy Bolt");
        Spell unknown = new Spell(Optional.of(id("bolt")), List.of(id("hellfire")), List.of(), "Not Yet");
        ServerPayloadHandlers.libraryEdit(p, new LibraryEditPayload(3, known));
        ServerPayloadHandlers.libraryEdit(p, new LibraryEditPayload(4, unknown));
        ServerPayloadHandlers.libraryEdit(p, new LibraryEditPayload(HunterLog.LIBRARY_SLOTS, known));
        HunterLog log = HunterLogs.get(p);
        helper.assertTrue(log.design(3).equals(known), "a design of known sigils is saved");
        helper.assertTrue(log.design(4).isEmpty(), "a design with an unknown sigil is refused");
        helper.assertTrue(log.designs().size() == HunterLog.LIBRARY_SLOTS, "the library has exactly 24 slots");
        ServerPayloadHandlers.libraryEdit(p, new LibraryEditPayload(3, Spell.EMPTY));
        helper.assertTrue(log.design(3).isEmpty(), "an empty design clears the slot");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void scrollsAreWrittenInBatches(GameTestHelper helper) {
        ServerPlayer p = hunter(helper, new ItemStack(AllItems.GRIMOIRE.get()));
        p.setGameMode(GameType.SURVIVAL);
        ManaManager.get(p).learn(id("bolt"));
        ManaManager.get(p).learn(id("smite"));
        Spell spell = new Spell(Optional.of(id("bolt")), List.of(id("smite")), List.of(), "");
        p.getInventory().add(new ItemStack(Items.PAPER, 3));
        p.getInventory().add(new ItemStack(AllItems.ENOCHIAN_INK.get(), 5));
        ServerPayloadHandlers.compose(p, new ComposeSpellPayload(0, spell, true, 5));
        helper.assertTrue(p.getInventory().countItem(AllItems.SPELL_SCROLL.get()) == 0, "3 sheets cannot make 5 scrolls");
        helper.assertTrue(p.getInventory().countItem(Items.PAPER) == 3, "and nothing is spent trying");
        p.getInventory().add(new ItemStack(Items.PAPER, 2));
        ServerPayloadHandlers.compose(p, new ComposeSpellPayload(0, spell, true, 5));
        helper.assertTrue(p.getInventory().countItem(AllItems.SPELL_SCROLL.get()) == 5, "5 scrolls, got "
                + p.getInventory().countItem(AllItems.SPELL_SCROLL.get()));
        helper.assertTrue(p.getInventory().countItem(Items.PAPER) == 0 && p.getInventory().countItem(AllItems.ENOCHIAN_INK.get()) == 0,
                "5 paper and 5 ink spent");
        ItemStack scroll = ItemStack.EMPTY;
        for (ItemStack s : p.getInventory().items) if (s.is(AllItems.SPELL_SCROLL.get())) scroll = s;
        helper.assertTrue(spell.equals(scroll.get(AllDataComponents.SCROLL_SPELL.get())), "the scrolls carry the spell");
        helper.assertTrue(p.getItemInHand(InteractionHand.MAIN_HAND).is(AllItems.GRIMOIRE.get()), "the grimoire stays in hand");
        helper.succeed();
    }
}
