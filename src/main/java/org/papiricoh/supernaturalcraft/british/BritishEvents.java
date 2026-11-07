package org.papiricoh.supernaturalcraft.british;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.MessengerSchedule;
import org.papiricoh.supernaturalcraft.allegiance.power.Crossings;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.british.birth.BirthScene;
import org.papiricoh.supernaturalcraft.british.site.SiteBuilder;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.entity.allegiance.MessengerEntity;
import org.papiricoh.supernaturalcraft.entity.boss.dagon.DagonSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.hess.HessSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.ketch.KetchSummoning;
import org.papiricoh.supernaturalcraft.nephilim.Nephilim;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRules;
import org.papiricoh.supernaturalcraft.nephilim.Nephilims;
import org.papiricoh.supernaturalcraft.util.WorldClock;

/**
 * Season 12's world (v0.41): Hess's visit at dawn once Amara is beaten, the betrayal of a hunter who joined the British, Ketch's
 * night hunts, the two unique places being raised, and the in-place fights at the compound and the safe house.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class BritishEvents {

    /** How often (ticks) the visits and hunts are considered. */
    public static final int PERIOD = 100;

    private BritishEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel overworld = event.getServer().overworld();
        SiteBuilder.tick(overworld);
        BirthScene.tick();
        long gt = overworld.getGameTime();
        if (gt % 20 == 11) {
            HessSummoning.tick(overworld);
            DagonSummoning.tick(overworld);
        }
        if (gt % PERIOD != 71) return;
        long clock = WorldClock.day(event.getServer());
        long day = clock / 24000L, time = Math.floorMod(clock, 24000L);
        for (ServerPlayer p : overworld.players()) {
            if (!p.isAlive() || p.isSpectator() || Crossings.inBattle(p)) continue;
            visits(p, day, time);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        BirthScene.reset();
    }

    /** Whether {@code p} may be called on now (not mid-talk with another visitor). */
    static boolean free(ServerPlayer p) {
        return !HessEnvoyEntity.visiting(p) && !MessengerEntity.visiting(p)
                && !org.papiricoh.supernaturalcraft.pact.CastielEntity.visiting(p)
                && !org.papiricoh.supernaturalcraft.pact.CrowleyKingEntity.visiting(p)
                && !org.papiricoh.supernaturalcraft.apocalypse.ruby.RubyNpcEntity.visiting(p);
    }

    /** One hunter's turn: Hess at dawn, London's verdict on a hunter who joined, Ketch by night. */
    public static void visits(ServerPlayer p, long day, long time) {
        if (p.level().dimension() != Level.OVERWORLD) return;
        BirthScene.catchUp(p);
        British b = BritishOrder.get(p);
        BritishRules.Choice choice = b.stance();
        if (BritishRules.betrayalDue(choice, b.day(), day)) {
            BritishServer.betray(p);
            choice = BritishRules.Choice.BETRAYED;
        }
        if (!free(p)) return;
        Nephilim n = Nephilims.get(p);
        if (MessengerSchedule.isDawn(time) && BritishRules.ultimatumDue(AuthorWorld.done(p, BossProgression.Boss.AMARA.advancement), choice)
                && NephilimRules.visitDue(n.lastVisit(), day)) {
            Nephilims.update(p, q -> q.withVisit(day));
            HessEnvoyEntity.visit(p);
            return;
        }
        if (BritishRules.night(time) && BritishOrder.hunted(p) && BritishRules.huntDue(b.lastHunt(), day) && !KetchSummoning.near(p)) {
            BritishOrder.update(p, x -> x.withHunt(day));
            KetchSummoning.ambush(p);
        }
    }
}
