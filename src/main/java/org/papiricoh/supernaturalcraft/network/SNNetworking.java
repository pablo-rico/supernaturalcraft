package org.papiricoh.supernaturalcraft.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.network.client.ClientPayloadHandlers;

import java.util.List;

/**
 * Payload registration and server-side send helpers. Client handlers are only referenced from
 * lambdas, which keeps the client-only classes off a dedicated server's classloader.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public class SNNetworking {

    private static final String VERSION = "10";

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);

        registrar.playToClient(ArcanaSyncPayload.TYPE, ArcanaSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloadHandlers.handleArcanaSync(payload)));

        registrar.playToClient(CinematicPayload.TYPE, CinematicPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloadHandlers.handleCinematic(payload)));
        registrar.playToClient(ArenaStatePayload.TYPE, ArenaStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloadHandlers.handleArenaState(payload)));

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

        // Gabriel, the Trickster (v0.14).
        registrar.playToClient(GabrielFxPayload.TYPE, GabrielFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.gabriel.ClientGabriel.handle(payload)));

        // Raphael, the archangel of the storm (v0.16).
        registrar.playToClient(RaphaelFxPayload.TYPE, RaphaelFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> org.papiricoh.supernaturalcraft.client.raphael.ClientRaphael.handle(payload)));

        registrar.playToServer(SelectSpellPayload.TYPE, SelectSpellPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ServerPayloadHandlers.handleSelectSpell(payload, context)));
        registrar.playToServer(ComposeSpellPayload.TYPE, ComposeSpellPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ServerPayloadHandlers.handleCompose(payload, context)));
    }

    public static void syncArcana(ServerPlayer player) {
        ArcanaData data = ManaManager.get(player);
        data.dirty = false;
        PacketDistributor.sendToPlayer(player, new ArcanaSyncPayload(data.mana(), data.maxMana(), data.cooldownUntil(),
                List.copyOf(data.known()), (data.hasGrace() ? ArcanaSyncPayload.GRACE : 0) | (data.hasVoidMark() ? ArcanaSyncPayload.VOID_MARK : 0),
                data.sanity(), List.copyOf(data.rites())));
    }

    // Login, respawn and dimension change all hand the client a fresh player, so each resyncs.

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncArcana(player);
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncArcana(player);
    }

    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncArcana(player);
    }
}
