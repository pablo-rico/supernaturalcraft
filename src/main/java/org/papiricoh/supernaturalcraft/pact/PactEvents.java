package org.papiricoh.supernaturalcraft.pact;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.MessengerSchedule;
import org.papiricoh.supernaturalcraft.allegiance.power.Crossings;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.entity.allegiance.MessengerEntity;
import org.papiricoh.supernaturalcraft.util.WorldClock;

/**
 * Season 6's world (v0.39): Castiel's visits at dawn, Crowley's at midnight when he tires of waiting, the lab's building.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class PactEvents {

    /** How often (ticks) the visits are considered. */
    public static final int PERIOD = 100;
    /** Midnight's window, in day ticks. */
    public static final long MIDNIGHT_START = 17_500, MIDNIGHT_END = 19_000;

    private PactEvents() {
    }

    /** At mod construction. */
    public static void bootstrap() {
        PactServer.registerDialogues();
        org.papiricoh.supernaturalcraft.entity.boss.godstiel.GodstielSpoils.registerDialogue();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel overworld = event.getServer().overworld();
        PactWorld.tick(overworld);
        org.papiricoh.supernaturalcraft.entity.boss.godstiel.DoorScene.tick();
        if (overworld.getGameTime() % PERIOD != 37) return;
        long clock = WorldClock.day(event.getServer());
        long day = clock / 24000L, time = Math.floorMod(clock, 24000L);
        for (ServerPlayer p : overworld.players()) {
            if (!p.isAlive() || p.isSpectator() || Crossings.inBattle(p)) continue;
            visits(p, day, time);
        }
        // Castiel, forgiven, keeps his penance in Purgatory: he finds the hunter who forgave him there.
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
            if (p.isAlive() && !p.isSpectator() && inPurgatory(p) && Pacts.step(p) == PactRules.Step.FORGIVEN
                    && CastielCompanionEntity.with(p) == null) {
                CastielCompanionEntity.arrive(p);
            }
        }
    }

    public static boolean inPurgatory(ServerPlayer p) {
        return p.level().dimension() == org.papiricoh.supernaturalcraft.purgatory.PurgatoryDimension.LEVEL;
    }

    /** Whether {@code p} may be called on now (not mid-talk with another visitor). */
    static boolean free(ServerPlayer p) {
        return !CastielEntity.visiting(p) && !CrowleyKingEntity.visiting(p) && !MessengerEntity.visiting(p);
    }

    static void visits(ServerPlayer p, long day, long time) {
        if (!free(p)) return;
        Pact pact = Pacts.get(p);
        PactRules.Step step = pact.stage();
        // Castiel: at dawn, once Raphael has fallen, when he has something to say and his rest is over.
        if (MessengerSchedule.isDawn(time) && AuthorWorld.done(p, BossProgression.Boss.RAPHAEL.advancement)
                && PactRules.visitDue(day, pact.lastVisit()) && PactDialogue.castielOpens(step, pact.alphas(), pact.scenes()) != null) {
            Pacts.update(p, q -> q.withVisit(day));
            CastielEntity.visit(p);
            return;
        }
        // Crowley: at midnight, tired of waiting for a hunter Castiel sent him.
        if (time >= MIDNIGHT_START && time < MIDNIGHT_END && PactRules.crowleyImpatient(step, day, pact.lastVisit())) {
            Pacts.update(p, q -> q.withVisit(day));
            CrowleyKingEntity.visit(p, null);
            return;
        }
        // Crowley again, with the lab, if he could not hand it over when the last Alpha fell.
        if (step == PactRules.Step.RITE_READY && !pact.seen(PactDialogue.SCENE_LAB) && pact.lastVisit() != day) {
            Pacts.update(p, q -> q.withVisit(day));
            CrowleyKingEntity.visit(p, null);
        }
    }
}
