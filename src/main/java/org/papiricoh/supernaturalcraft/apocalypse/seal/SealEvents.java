package org.papiricoh.supernaturalcraft.apocalypse.seal;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.network.SealFxPayload;
import org.papiricoh.supernaturalcraft.network.SealSitePayload;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.util.WorldClock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The seals' server side (v0.40, the Seals slice), each tick of the server: the count ({@link SealRules#breaksNow}), the events
 * (called by {@link #tickSpawner}, run each second by {@link #run}), the plagues turning back ({@link SealPlagues#revert}), the
 * omens of the stage ({@link SealOmens}) and Castiel's warning ({@link SealEnvoy}). The count starts as the first hunter beats
 * Azazel (or, for worlds that had, as they log in). Everything is synced on change, login, respawn and a change of dimension.
 *
 * <p>Events never come on the GameTest server unless a test turns them on ({@link SealSavedData#setTestEvents}).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class SealEvents {

    /** How often (ticks) the count is checked. */
    public static final int COUNT_EVERY = 100;

    private SealEvents() {
    }

    /** Forgets what is kept in memory for a player who left ({@code PlayerCleanup}). */
    public static void forget(UUID id) {
        SealOmens.forget(id);
        SealEnvoy.forget(id);
    }

    /** Whether events may come now (never on the GameTest server unless a test asked). */
    public static boolean eventsOn(MinecraftServer server) {
        return !GameTestHooks.isGametestServer() || SealSavedData.get(server).testEvents();
    }

    // --- the server's tick -----------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        long t = overworld.getGameTime();
        SealSavedData data = SealSavedData.get(server);
        if (t % 5 == 0) SealPlagues.revert(server);
        org.papiricoh.supernaturalcraft.apocalypse.seal.garrison.GarrisonWorld.tick(overworld);
        if (!data.started()) return;
        if (t % 20 == 0 && !data.events().isEmpty()) run(overworld, data, t);
        if (t % COUNT_EVERY == 17) tickCount(server, data);
        if (t % SealRules.CHECK_EVERY == 41 && eventsOn(server)) tickSpawner(overworld, data, t);
        if (t % SealOmens.EVERY == 63) SealOmens.tick(overworld, data);
        if (t % SealEnvoy.EVERY == 89) SealEnvoy.tick(overworld);
    }

    /** The passive count: a seal breaks once its day comes. */
    public static void tickCount(MinecraftServer server, SealSavedData data) {
        long now = WorldClock.day(server);
        if (SealRules.breaksNow(data.started(), data.broken(), now, data.lastBreakDay(), data.heldSince()) && !data.risen()) {
            SealWorld.breakSeal(server, null);
        }
    }

    // --- calling hunters -------------------------------------------------------------------------------------------------------

    static void tickSpawner(ServerLevel level, SealSavedData data, long now) {
        long day = SealRules.dayOf(WorldClock.day(level.getServer()));
        for (ServerPlayer p : level.players()) {
            if (!p.isAlive() || p.isSpectator() || p.isCreative()) continue;
            SealRecord r = SealWorld.record(p);
            if (!SealRules.mayCall(SealWorld.chapterTwo(p), data.eventsFor(p.getUUID()), data.events().size(), r.calledOn(day), now,
                    r.lastEvent(), data.broken(), data.risen())) continue;
            if (level.random.nextFloat() >= SealRules.CHANCE) continue;
            start(level, p, SealKind.pick(level.random.nextDouble()));
        }
    }

    /**
     * Calls {@code hunter} to a seal event of {@code kind} (the spawner, commands, tests): a site near them, the warning, and the
     * Seal Compass the first time. @return the event, or null if no site would do
     */
    public static @Nullable SealEvent start(ServerLevel level, ServerPlayer hunter, SealKind kind) {
        BlockPos site = SealStaging.site(level, hunter, kind, level.random);
        return site == null ? null : startAt(level, hunter, kind, site);
    }

    /** Calls {@code hunter} to a seal event of {@code kind} at {@code site} (commands, tests). */
    public static SealEvent startAt(ServerLevel level, @Nullable ServerPlayer hunter, SealKind kind, BlockPos site) {
        SealSavedData data = SealSavedData.get(level.getServer());
        if (!data.started()) data.start(WorldClock.day(level.getServer()));
        long now = level.getGameTime();
        SealEvent e = new SealEvent(data.nextEventId(), kind, site, now, now + SealRules.EVENT_TICKS, hunter != null ? hunter.getUUID() : null);
        data.add(e);
        if (hunter != null) {
            SealRecord r = SealWorld.record(hunter).called(now, SealRules.dayOf(WorldClock.day(level.getServer())));
            if (!r.compass()) {
                SealSpoils.give(hunter, new ItemStack(AllItems.SEAL_COMPASS.get()));
                r = r.withCompass();
            }
            SealWorld.setRecord(hunter, r);
        }
        if (SealStaging.fits(level, site) || level.isLoaded(site)) SealStaging.stage(level, e, SealWorld.effectiveStage(level.getServer()));
        warn(level, e, hunter);
        return e;
    }

    /** The warning: a card, a sound and a line in the chat with the way and the distance, to the hunter and chapter II nearby. */
    static void warn(ServerLevel level, SealEvent e, @Nullable ServerPlayer hunter) {
        List<ServerPlayer> told = new ArrayList<>();
        if (hunter != null) told.add(hunter);
        for (ServerPlayer p : level.players()) {
            if (p != hunter && p.distanceToSqr(Vec3.atCenterOf(e.pos)) < 160 * 160 && SealWorld.chapterTwo(p)) told.add(p);
        }
        Component kind = Component.translatable(e.kind.nameKey()).withStyle(ChatFormatting.DARK_RED);
        for (ServerPlayer p : told) {
            int dist = (int) Math.round(Math.sqrt(p.distanceToSqr(Vec3.atCenterOf(e.pos))));
            p.sendSystemMessage(Component.translatable("message.supernaturalcraft.seal.warning", kind, dist,
                    Component.translatable(direction(p.position(), Vec3.atCenterOf(e.pos)))).withStyle(ChatFormatting.RED));
            p.sendSystemMessage(Component.translatable(e.kind.hintKey()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            Send.toPlayer(p, new SealFxPayload(-1, SealFxPayload.WARNING, e.kind.ordinal(), 0, Vec3.atCenterOf(e.pos), SealRules.CARD_TICKS));
            Send.toPlayer(p, payload(e, level.getGameTime()));
        }
    }

    /** The key of the way from {@code from} to {@code to}, one of eight ({@code seal.supernaturalcraft.dir.<n>}, 0 = north). */
    public static String direction(Vec3 from, Vec3 to) {
        double dx = to.x - from.x, dz = to.z - from.z;
        // North is -z; clockwise.
        double deg = Math.toDegrees(Math.atan2(dx, -dz));
        int n = Math.floorMod((int) Math.round(deg / 45.0), 8);
        return "seal.supernaturalcraft.dir." + n;
    }

    // --- running them ----------------------------------------------------------------------------------------------------------

    /** Each second: every event's rite, its creatures, its outcome. */
    static void run(ServerLevel level, SealSavedData data, long now) {
        for (SealEvent e : new ArrayList<>(data.events())) {
            tickEvent(level, data, e, now);
        }
    }

    /** One event's second (public for tests, which drive it by hand). @return its outcome now */
    public static SealRules.Outcome tickEvent(ServerLevel level, SealSavedData data, SealEvent e, long now) {
        boolean loaded = level.isLoaded(e.pos) && level.isPositionEntityTicking(e.pos);
        if (!e.staged && loaded) SealStaging.stage(level, e, SealWorld.effectiveStage(level.getServer()));
        List<ServerPlayer> hunters = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (p.isAlive() && !p.isSpectator() && p.distanceToSqr(Vec3.atCenterOf(e.pos)) <= SealRules.PRESENCE * SealRules.PRESENCE) {
                hunters.add(p);
                e.present.add(p.getUUID());
            }
        }
        e.setProgress(e.progress + SealRules.riteStep(20, !hunters.isEmpty()));
        SealRules.Outcome outcome;
        int drivers = 0, wards = 0;
        if (loaded && e.staged) {
            SealStaging.tick(level, e, hunters, now);
            wards = SealStaging.count(level, e.wards);
            if (e.wardsAlive >= 0 && wards < e.wardsAlive) {
                for (int i = wards; i < e.wardsAlive; i++) e.setProgress(SealRules.wardSlain(e.progress));
            }
            e.wardsAlive = wards;
            drivers = SealStaging.count(level, e.drivers) + (focusStands(level, e) ? 1 : 0);
            outcome = SealRules.outcome(e.progress, drivers, wards, e.wardsAtStart);
            if (now % 100 == 0 && outcome == SealRules.Outcome.RUNNING) {
                Send.near(level, null, e.pos.getX() + 0.5, e.pos.getY() + 1, e.pos.getZ() + 0.5, 48,
                        new SealFxPayload(-1, SealFxPayload.PULSE, (int) e.progress, 0, Vec3.atCenterOf(e.pos), 40));
            }
        } else {
            drivers = e.drivers.size() + (e.focus != null ? 1 : 0);
            wards = Math.max(0, e.wardsAlive);
            outcome = e.progress >= SealRules.RITE_MAX ? SealRules.Outcome.BROKEN : SealRules.Outcome.RUNNING;
        }
        if (outcome == SealRules.Outcome.RUNNING && now >= e.ends) outcome = SealRules.Outcome.BROKEN;
        if (outcome == SealRules.Outcome.RUNNING) {
            SealSitePayload site = new SealSitePayload(e.kind.ordinal(), e.pos, Mth.floor(e.progress), e.ticksLeft(now), drivers, wards, e.wardsAtStart);
            for (ServerPlayer p : concerned(level, e)) Send.toPlayer(p, site);
            data.setDirty();
            return outcome;
        }
        finish(level, data, e, outcome == SealRules.Outcome.HELD);
        return outcome;
    }

    static boolean focusStands(ServerLevel level, SealEvent e) {
        return e.focus != null && level.getBlockState(e.focus).is(AllBlocks.SEAL_HEX_FOCUS.get());
    }

    /** The hunters an event concerns who are online: the one it called and everyone who has been there. */
    static List<ServerPlayer> concerned(ServerLevel level, SealEvent e) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) if (e.concerns(p.getUUID())) out.add(p);
        // Fake players (tests) are not in the list: those standing there count too.
        for (ServerPlayer p : level.players()) if (!out.contains(p) && e.concerns(p.getUUID())) out.add(p);
        return out;
    }

    /** The event is over: held (every share, the day won) or broken (a seal goes, at once). */
    public static void finish(ServerLevel level, SealSavedData data, SealEvent e, boolean held) {
        data.remove(e);
        List<ServerPlayer> hunters = concerned(level, e);
        if (level.isLoaded(e.pos)) SealStaging.clear(level, e, held);
        Vec3 at = Vec3.atCenterOf(e.pos);
        if (held) {
            SealSpoils.held(level, e, hunters);
            for (ServerPlayer p : hunters) Send.toPlayer(p, new SealFxPayload(-1, SealFxPayload.HELD, e.kind.ordinal(), 0, at, SealRules.CARD_TICKS));
        } else {
            SealSpoils.broken(level, e, hunters);
            SealWorld.breakSeal(level.getServer(), e);
        }
        for (ServerPlayer p : hunters) Send.toPlayer(p, SealSitePayload.NONE);
    }

    /** Ends every event without a verdict (Lilith fell; commands, tests): their creatures go, no seal breaks. */
    public static void endAll(MinecraftServer server) {
        ServerLevel level = server.overworld();
        SealSavedData data = SealSavedData.get(server);
        for (SealEvent e : new ArrayList<>(data.events())) {
            data.remove(e);
            if (level.isLoaded(e.pos)) SealStaging.clear(level, e, true);
            for (ServerPlayer p : concerned(level, e)) Send.toPlayer(p, SealSitePayload.NONE);
        }
    }

    /** A hunter broke a hex focus ({@link SealBlocks.HexFocus}): a word to them (the next second decides the event). */
    static void focusBroken(ServerLevel level, BlockPos pos, @Nullable net.minecraft.world.entity.player.Player by) {
        SealSavedData data = SealSavedData.get(level.getServer());
        for (SealEvent e : data.events()) {
            if (pos.equals(e.focus) && by instanceof ServerPlayer p) {
                e.present.add(p.getUUID());
                p.displayClientMessage(Component.translatable("message.supernaturalcraft.seal.hex_broken").withStyle(ChatFormatting.GOLD), true);
            }
        }
    }

    // --- players ---------------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        // Worlds where someone had already beaten Azazel: the count starts now.
        if (AuthorWorld.done(p, SealWorld.AZAZEL)) {
            SealWorld.begin(p.server);
            if (SealWorld.broken(p.server) >= SealRules.PASSIVE_MAX) org.papiricoh.supernaturalcraft.reward.ChorusRewards.award(p, SealWorld.ELEVENTH);
        }
        syncFull(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) syncFull(p);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) syncFull(p);
    }

    /** What a hunter's client is told of {@code e} now. */
    static SealSitePayload payload(SealEvent e, long now) {
        return new SealSitePayload(e.kind.ordinal(), e.pos, Mth.floor(e.progress), e.ticksLeft(now), e.drivers.size() + (e.focus != null ? 1 : 0),
                Math.max(0, e.wardsAlive), e.wardsAtStart);
    }

    static void syncFull(ServerPlayer p) {
        SealWorld.sync(p);
        SealSavedData data = SealSavedData.get(p.server);
        SealEvent e = data.eventOf(p.getUUID());
        if (e == null) {
            Send.toPlayer(p, SealSitePayload.NONE);
        } else {
            Send.toPlayer(p, payload(e, p.server.overworld().getGameTime()));
        }
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        ResourceLocation id = event.getAdvancement().id();
        if (id.equals(SupernaturalCraft.asResource(SealWorld.AZAZEL))) SealWorld.begin(p.server);
        if (id.equals(SupernaturalCraft.asResource(SealWorld.LILITH))) {
            // Past chapter II: their calls end (the event's creatures stay for the others it concerns).
            Send.toPlayer(p, SealSitePayload.NONE);
        }
    }

    // --- the creatures ---------------------------------------------------------------------------------------------------------

    /** An angel slain at a seal leaves the burnt shape of its wings on the ground (in the ledger: it fades with the plagues). */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level) || !dead.getPersistentData().contains(SealEvent.TAG)) return;
        if (!Kin.isAngel(dead)) return;
        BlockPos at = dead.blockPosition();
        if (level.getBlockState(at).canBeReplaced() && level.getFluidState(at).isEmpty()
                && level.getBlockState(at.below()).isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP)) {
            SealPlagues.turn(level, at, AllBlocks.WING_SCORCH.get().defaultBlockState(), WorldClock.day(level.getServer()) + SealPlagues.EVENT_LASTS);
        }
    }

    /** A creature of an event that is over (unloaded when it ended) goes as soon as it is back. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk() == false) return;
        Entity e = event.getEntity();
        int id = e.getPersistentData().getInt(SealEvent.TAG);
        if (id == 0 || e.level().dimension() != Level.OVERWORLD || !(e.level() instanceof ServerLevel level)) return;
        if (SealSavedData.get(level.getServer()).event(id) == null) {
            if (e instanceof net.minecraft.world.entity.npc.Villager v) {
                v.setNoAi(false);
                v.getPersistentData().remove(SealEvent.TAG);
            } else {
                event.setCanceled(true);
            }
        }
    }
}
