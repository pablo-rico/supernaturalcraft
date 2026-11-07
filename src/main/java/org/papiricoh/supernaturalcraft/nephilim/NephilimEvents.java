package org.papiricoh.supernaturalcraft.nephilim;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.MessengerSchedule;
import org.papiricoh.supernaturalcraft.allegiance.power.Crossings;
import org.papiricoh.supernaturalcraft.entity.boss.soulless.SoullessJackSummoning;
import org.papiricoh.supernaturalcraft.nephilim.jack.JackCompanions;
import org.papiricoh.supernaturalcraft.nephilim.michael.MichaelEnvoyEntity;
import org.papiricoh.supernaturalcraft.nephilim.michael.MichaelPossession;
import org.papiricoh.supernaturalcraft.util.WorldClock;

/**
 * v0.41: season 14's clock, every five seconds, for each hunter in the Overworld whose rest is over
 * ({@link NephilimRules#visitDue}):
 * <ul>
 *   <li>{@code MICHAEL_FLED}/{@code OFFERED}: Michael comes at dawn with his offer (again, until his vessel falls).</li>
 *   <li>{@code VESSEL_DOWN}: at night Jack says goodbye; he has traded his soul for his power ({@code SOULLESS}).</li>
 *   <li>{@code SOULLESS}: the next night he comes back without it ({@link SoullessJackSummoning#ambush}).</li>
 * </ul>
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class NephilimEvents {

    private static final int PERIOD = 100;
    public static final long NIGHT_START = 13_500, NIGHT_END = 22_500;
    /** How many lines Jack says leaving ({@code message.supernaturalcraft.jack.leaving.<n>}). */
    public static final int LEAVING_LINES = 3;

    private NephilimEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        if (overworld.getGameTime() % PERIOD != 71) return;
        long clock = WorldClock.day(server);
        long day = clock / 24000L, time = Math.floorMod(clock, 24000L);
        for (ServerPlayer p : overworld.players()) {
            if (!p.isAlive() || p.isSpectator() || Crossings.inBattle(p) || MichaelPossession.busy(p)) continue;
            Nephilim n = Nephilims.get(p);
            if (!NephilimRules.visitDue(n.lastVisit(), day)) continue;
            switch (n.stage()) {
                case MICHAEL_FLED, OFFERED -> {
                    if (MessengerSchedule.isDawn(time) && !MichaelEnvoyEntity.visiting(p)) {
                        Nephilims.update(p, q -> q.withVisit(day));
                        MichaelEnvoyEntity.visit(p);
                    }
                }
                case VESSEL_DOWN -> {
                    if (night(time)) {
                        jackLeaves(p);
                        Nephilims.update(p, q -> q.withVisit(day));
                    }
                }
                case SOULLESS -> {
                    if (night(time) && SoullessJackSummoning.ambush(p)) Nephilims.update(p, q -> q.withVisit(day));
                }
                default -> {
                }
            }
        }
    }

    public static boolean night(long time) {
        return time >= NIGHT_START && time < NIGHT_END;
    }

    /** Jack has traded his soul away for his power: he says goodbye and goes. */
    public static void jackLeaves(ServerPlayer p) {
        for (int i = 0; i < LEAVING_LINES; i++) {
            p.sendSystemMessage(Component.translatable("message.supernaturalcraft.jack.leaving." + i).withStyle(ChatFormatting.YELLOW));
        }
        JackCompanions.dismiss(p);
        Nephilims.advance(p, NephilimRules.Step.SOULLESS);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        MichaelPossession.release(event.getEntity().getUUID());
    }
}
