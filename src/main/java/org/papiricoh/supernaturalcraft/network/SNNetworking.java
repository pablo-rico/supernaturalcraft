package org.papiricoh.supernaturalcraft.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.DepthRules;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.network.client.ClientPayloadHandlers;

import java.util.List;

/**
 * Payload registration and server-side send helpers. Client handlers are only referenced from
 * lambdas, which keeps the client-only classes off a dedicated server's classloader.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public class SNNetworking {

    private static final String VERSION = "14";

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        CountingRegistrar registrar = new CountingRegistrar(event.registrar(VERSION));

        registrar.playToClient(ArcanaSyncPayload.TYPE, ArcanaSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloadHandlers.handleArcanaSync(payload)));

        registrar.playToClient(CinematicPayload.TYPE, CinematicPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloadHandlers.handleCinematic(payload)));
        registrar.playToClient(ArenaStatePayload.TYPE, ArenaStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloadHandlers.handleArenaState(payload)));

        registrar.playToServer(CinematicSkipPayload.TYPE, CinematicSkipPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer p) org.papiricoh.supernaturalcraft.cinematic.CinematicLocks.forgetPlayer(p.getUUID());
                }));
        registrar.playToClient(CameraSequencePayload.TYPE, CameraSequencePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloadHandlers.handleCameraSequence(payload)));
        registrar.playToClient(ConsumptionPayload.TYPE, ConsumptionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloadHandlers.handleConsumption(payload)));
        registrar.playToClient(AmaraFxPayload.TYPE, AmaraFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloadHandlers.handleAmaraFx(payload)));
        registrar.playToClient(ChorusFxPayload.TYPE, ChorusFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloadHandlers.handleChorusFx(payload)));
        registrar.playToClient(DebrisPayload.TYPE, DebrisPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloadHandlers.handleDebris(payload)));
        registrar.playToClient(EclipseStatePayload.TYPE, EclipseStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloadHandlers.handleEclipse(payload)));

        registrar.playToClient(TormentPayload.TYPE, TormentPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloadHandlers.handleTorment(payload)));

        registrar.playToClient(ColtShotPayload.TYPE, ColtShotPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloadHandlers.handleColtShot(payload)));
        registrar.playToClient(ColtActionPayload.TYPE, ColtActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloadHandlers.handleColtAction(payload)));
        registrar.playToServer(ColtInputPayload.TYPE, ColtInputPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ServerPayloadHandlers.handleColtInput(payload, context)));

        // The spell bowl, its locating smoke and the crossroads (v0.8).
        registrar.playToClient(OpenRecitationPayload.TYPE, OpenRecitationPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.bowl.client.BowlClientHandlers.openRecitation(payload)));
        registrar.playToServer(RecitationResultPayload.TYPE, RecitationResultPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.bowl.BowlServerHandlers.recitationResult(payload, context)));
        registrar.playToClient(SmokeTrailPayload.TYPE, SmokeTrailPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.bowl.spell.client.SmokeTrails.add(payload)));
        registrar.playToClient(DealOfferPayload.TYPE, DealOfferPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.crossroads.client.DealClientHandlers.openDeal(payload)));
        registrar.playToServer(DealChoicePayload.TYPE, DealChoicePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.crossroads.DealServerHandlers.choice(payload, context)));

        // The Hunter's Book (v0.9).
        registrar.playToClient(HunterLogSyncPayload.TYPE, HunterLogSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.ClientHunterLog.update(payload)));
        registrar.playToClient(LibrarySyncPayload.TYPE, LibrarySyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.ClientHunterLog.updateLibrary(payload)));
        registrar.playToServer(JournalActionPayload.TYPE, JournalActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ServerPayloadHandlers.handleJournalAction(payload, context)));
        registrar.playToServer(LibraryEditPayload.TYPE, LibraryEditPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ServerPayloadHandlers.handleLibraryEdit(payload, context)));

        // The Author (v0.10).
        registrar.playToClient(AuthorFxPayload.TYPE, AuthorFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.chuck.fx.ClientChuck.handle(payload)));
        registrar.playToClient(AuthorDialoguePayload.TYPE, AuthorDialoguePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.chuck.screen.AuthorScreens.open(payload)));
        registrar.playToServer(AuthorChoicePayload.TYPE, AuthorChoicePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.author.AuthorServerHandlers.choice(payload, context)));

        // The Four Horsemen (v0.11).
        registrar.playToClient(HorsemenFxPayload.TYPE, HorsemenFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.horsemen.fx.ClientHorsemen.handle(payload)));
        // The Archangel Michael (v0.12).
        registrar.playToClient(MichaelFxPayload.TYPE, MichaelFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.michael.ClientMichael.handle(payload)));
        registrar.playToServer(VesselAnswerPayload.TYPE, VesselAnswerPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.entity.boss.michael.VesselPossession.answer(payload, context)));

        // Allegiance: angels, demons and hunters (v0.13).
        registrar.playToClient(AllegianceSyncPayload.TYPE, AllegianceSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.allegiance.ClientAllegiance.handleSync(payload)));
        registrar.playToClient(AllegianceDialoguePayload.TYPE, AllegianceDialoguePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.allegiance.ClientAllegiance.handleDialogue(payload)));
        registrar.playToClient(AllegianceFxPayload.TYPE, AllegianceFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.allegiance.ClientAllegiance.handleFx(payload)));
        registrar.playToServer(CastPowerPayload.TYPE, CastPowerPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.allegiance.AllegianceServerHandlers.cast(payload, context)));
        registrar.playToServer(AllegianceChoicePayload.TYPE, AllegianceChoicePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.allegiance.AllegianceServerHandlers.choice(payload, context)));

        // Angels and the Fall (v0.22).
        registrar.playToClient(SmiteFxPayload.TYPE, SmiteFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.angel.ClientAngels.handleFx(payload)));

        // Gabriel, the Trickster (v0.14).
        registrar.playToClient(GabrielFxPayload.TYPE, GabrielFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.gabriel.ClientGabriel.handle(payload)));

        // Both Lucifers' title cards (the Cage's HUD).
        registrar.playToClient(LuciferFxPayload.TYPE, LuciferFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.lucifer.ClientLucifer.handle(payload)));

        // Raphael, the archangel of the storm (v0.16).
        registrar.playToClient(RaphaelFxPayload.TYPE, RaphaelFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.raphael.ClientRaphael.handle(payload)));

        // The Men of Letters (v0.17).
        registrar.playToClient(LegacySyncPayload.TYPE, LegacySyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.legacy.ClientLegacy.handle(payload)));
        registrar.playToClient(LegacyFxPayload.TYPE, LegacyFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.legacy.ClientLegacy.handleFx(payload)));
        registrar.playToServer(LegacyChoicePayload.TYPE, LegacyChoicePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.legacy.LegacyServerHandlers.choice(payload, context)));
        registrar.playToServer(ResearchActionPayload.TYPE, ResearchActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.legacy.research.ResearchServerHandlers.action(payload, context)));
        registrar.playToClient(ResearchBoardPayload.TYPE, ResearchBoardPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.legacy.ClientLegacy.handleBoard(payload)));
        registrar.playToClient(OpenWarTablePayload.TYPE, OpenWarTablePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.legacy.table.ClientWarTable.open(payload)));
        registrar.playToClient(WarTableTerrainPayload.TYPE, WarTableTerrainPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.legacy.table.ClientWarTable.handleTerrain(payload)));
        registrar.playToServer(WarTableActionPayload.TYPE, WarTableActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.legacy.cases.table.WarTableServerHandlers.action(payload, context)));
        registrar.playToServer(WarTableTerrainRequestPayload.TYPE, WarTableTerrainRequestPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.legacy.cases.table.WarTableServerHandlers.terrain(payload, context)));

        // Heaven: a hunter's own Heaven, its memories, Naomi and Zachariah (v0.18).
        registrar.playToClient(HeavenFxPayload.TYPE, HeavenFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.heaven.ClientHeaven.handleFx(payload)));
        registrar.playToClient(MemorySyncPayload.TYPE, MemorySyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.heaven.ClientHeaven.handleMemorySync(payload)));
        registrar.playToClient(HeavenSyncPayload.TYPE, HeavenSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.heaven.ClientHeaven.handleHeavenSync(payload)));
        registrar.playToClient(AshMenuPayload.TYPE, AshMenuPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.heaven.ClientHeaven.handleAshMenu(payload)));
        registrar.playToServer(AshChoicePayload.TYPE, AshChoicePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.heaven.HeavenServerHandlers.ashChoice(payload, context)));
        registrar.playToServer(ChairStrugglePayload.TYPE, ChairStrugglePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.entity.boss.naomi.NaomiServerHandlers.struggle(payload, context)));

        // Cain, the Father of Murder (v0.21).
        registrar.playToClient(CainFxPayload.TYPE, CainFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.cain.ClientCainFx.handle(payload)));
        // Abaddon, Knight of Hell (v0.21).
        registrar.playToClient(AbaddonFxPayload.TYPE, AbaddonFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.abaddon.ClientAbaddonFx.handle(payload)));
        // Rowena and the Grand Witch (v0.21, P6).
        registrar.playToClient(RowenaFxPayload.TYPE, RowenaFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.witchboss.ClientRowenaFx.handle(payload)));
        registrar.playToClient(GrandWitchFxPayload.TYPE, GrandWitchFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.witchboss.ClientGrandWitchFx.handle(payload)));
        // The magic: its effects, ritual charges, the Mark and mastery (v0.21).
        registrar.playToClient(MagicFxPayload.TYPE, MagicFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.magic.fx.ClientMagicFx.handle(payload)));
        registrar.playToClient(RitualChargePayload.TYPE, RitualChargePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.magic.fx.ClientMagicFx.handleCharge(payload)));
        registrar.playToClient(MarkSyncPayload.TYPE, MarkSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.mark.ClientMark.handle(payload)));
        registrar.playToClient(MasterySyncPayload.TYPE, MasterySyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.magic.ClientMastery.handle(payload)));

        registrar.playToServer(SelectSpellPayload.TYPE, SelectSpellPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ServerPayloadHandlers.handleSelectSpell(payload, context)));
        registrar.playToServer(ComposeSpellPayload.TYPE, ComposeSpellPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ServerPayloadHandlers.handleCompose(payload, context)));
    }

    public static void syncArcana(ServerPlayer player) {
        syncArcana(player, false);
    }

    /** Sends the owner their arcana; the known sigils and rites only when they changed, or {@code full} (a fresh client player). */
    public static void syncArcana(ServerPlayer player, boolean full) {
        ArcanaData data = ManaManager.get(player);
        boolean lists = full || data.listsDirty;
        data.dirty = false;
        data.listsDirty = false;
        int flags = (data.hasGrace() ? ArcanaSyncPayload.GRACE : 0) | (data.hasVoidMark() ? ArcanaSyncPayload.VOID_MARK : 0)
                | (lists ? ArcanaSyncPayload.LISTS : 0);
        Send.toPlayer(player, new ArcanaSyncPayload(data.mana(), data.maxMana(), data.cooldownUntil(),
                lists ? List.copyOf(data.known()) : List.of(), flags, data.sanity(), lists ? List.copyOf(data.rites()) : List.of(),
                data.rites(DepthRules.Kind.DEEPENING), data.rites(DepthRules.Kind.WELLSPRING)));
    }

    // Login, respawn and dimension change all hand the client a fresh player, so each resyncs.

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncArcana(player, true);
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncArcana(player, true);
    }

    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncArcana(player, true);
    }
}
