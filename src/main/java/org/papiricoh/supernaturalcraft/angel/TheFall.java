package org.papiricoh.supernaturalcraft.angel;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.network.SmiteFxPayload;

import java.util.List;

/**
 * The Fall (v0.22): Metatron beaten, Heaven's angels are cast down. On the next night (that very night, if it is night) the
 * whole Overworld sky rains falling angels ({@link SmiteFxPayload#FALL_NIGHT}; the client draws it) until dawn; on every night
 * after, now and then a single one streaks down somewhere near a hunter ({@link SmiteFxPayload#FALL_STAR}). Only a sight: the
 * fallen themselves walk in as any monster ({@link AngelSpawns}).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class TheFall {

    /** On later nights, the chance each second that one more falls near some hunter. */
    public static final float STAR_CHANCE = 1f / 150;
    /** How long a single star takes to fall, in ticks; how far from the hunter it lands. */
    public static final int STAR_TICKS = 60;
    public static final double STAR_MIN = 60, STAR_MAX = 160, STAR_SEEN = 320;

    private TheFall() {
    }

    /** Metatron is beaten: the fallen are loosed, and the Fall's night is coming. */
    public static void open(ServerLevel overworld) {
        HostsSavedData data = HostsSavedData.get(overworld.getServer());
        if (data.fallen()) return;
        data.setAngels(true);
        data.setFallen(true, true);
        if (overworld.isNight()) beginNight(overworld);
    }

    /** The Fall's night begins now (also the debug command's): every hunter in the Overworld sees it. */
    public static void beginNight(ServerLevel overworld) {
        HostsSavedData data = HostsSavedData.get(overworld.getServer());
        if (!data.fallen()) data.setFallen(true, false);
        data.setFallNight(true);
        Send.inDimension(overworld, nightPayload(true));
    }

    /** The Fall's night ends (dawn, or the debug command). */
    public static void endNight(ServerLevel overworld) {
        HostsSavedData data = HostsSavedData.get(overworld.getServer());
        data.setFallNight(false);
        Send.inDimension(overworld, nightPayload(false));
    }

    /** One angel streaks down toward ({@code x}, {@code z}), seen from far around. */
    public static void star(ServerLevel level, double x, double z) {
        Send.near(level, null, x, level.getSeaLevel(), z, STAR_SEEN,
                new SmiteFxPayload(SmiteFxPayload.FALL_STAR, 0, 0, new Vec3(x, 0, z), 0, STAR_TICKS));
    }

    private static SmiteFxPayload nightPayload(boolean on) {
        return new SmiteFxPayload(SmiteFxPayload.FALL_NIGHT, 0, on ? 1 : 0, Vec3.ZERO, 0, 0);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || level.getGameTime() % 20 != 0) return;
        HostsSavedData data = HostsSavedData.get(level.getServer());
        if (!data.fallen()) return;
        boolean night = level.isNight();
        if (data.fallNight() && !night) endNight(level);
        else if (data.fallPending() && night) beginNight(level);
        else if (night && !data.fallNight() && level.random.nextFloat() < STAR_CHANCE) {
            List<ServerPlayer> players = level.players();
            if (players.isEmpty()) return;
            ServerPlayer p = players.get(level.random.nextInt(players.size()));
            double angle = level.random.nextDouble() * Mth.TWO_PI, r = Mth.lerp(level.random.nextDouble(), STAR_MIN, STAR_MAX);
            star(level, p.getX() + Math.cos(angle) * r, p.getZ() + Math.sin(angle) * r);
        }
    }

    /** Whoever comes into the Overworld during the Fall's night sees it too. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        resend(event.getEntity());
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        resend(event.getEntity());
    }

    private static void resend(net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer sp) || sp.level().dimension() != Level.OVERWORLD) return;
        if (HostsSavedData.get(sp.server).fallNight()) Send.toPlayer(sp, nightPayload(true));
    }
}
