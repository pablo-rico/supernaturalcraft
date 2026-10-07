package org.papiricoh.supernaturalcraft.entity.boss.chuck;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.network.AuthorFxPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * The moments of the Author's fight the client shows ({@link AuthorFxPayload}), sent to everyone watching: the
 * players inside (or near) his arena, spectators included.
 */
public final class ChuckFx {

    /** Beyond the arena's edge, still in the audience. */
    private static final double MARGIN = 24, NO_ARENA_RANGE = 64;

    private ChuckFx() {
    }

    /** Everyone watching {@code boss}'s fight. */
    public static List<ServerPlayer> audience(ChuckEntity boss) {
        List<ServerPlayer> out = new ArrayList<>();
        if (!(boss.level() instanceof ServerLevel level)) return out;
        ArenaController arena = boss.arena();
        for (ServerPlayer p : level.players()) {
            boolean near = arena != null ? arena.horizontalDistance(p.position()) <= arena.radius() + MARGIN
                    : p.distanceToSqr(boss) < NO_ARENA_RANGE * NO_ARENA_RANGE;
            if (near) out.add(p);
        }
        return out;
    }

    public static void send(ChuckEntity boss, byte kind, int arg, Vec3 point, float radius, int duration, String text) {
        AuthorFxPayload payload = new AuthorFxPayload(boss.getId(), kind, arg, point, radius, duration, text);
        for (ServerPlayer p : audience(boss)) PacketDistributor.sendToPlayer(p, payload);
    }

    private static void sendTo(LivingEntity who, AuthorFxPayload payload) {
        if (who instanceof ServerPlayer p && p.connection != null) PacketDistributor.sendToPlayer(p, payload);
    }

    /** A chapter opens: its number and title. */
    public static void chapterTitle(ChuckEntity boss, Chapter chapter) {
        send(boss, AuthorFxPayload.CHAPTER_TITLE, chapter.ordinal(), boss.position(), 0, 100, chapter.titleKey());
    }

    /** He narrates {@code key} (typed out over {@code duration}). */
    public static void narrate(ChuckEntity boss, String key, int duration) {
        send(boss, AuthorFxPayload.NARRATE, 0, boss.position(), 0, duration, key);
    }

    public static void snapCount(ChuckEntity boss, Vec3 center, double radius, int duration) {
        send(boss, AuthorFxPayload.SNAP_COUNT, 0, center, (float) radius, duration, "");
    }

    /** {@code who} was pulled back from {@code from}: sent about them, to everyone watching. */
    public static void backspace(ChuckEntity boss, LivingEntity who, Vec3 from) {
        AuthorFxPayload payload = new AuthorFxPayload(who.getId(), AuthorFxPayload.BACKSPACE, 0, from, 0, 20, "");
        for (ServerPlayer p : audience(boss)) PacketDistributor.sendToPlayer(p, payload);
    }

    public static void fakeCredits(ChuckEntity boss, int duration) {
        send(boss, AuthorFxPayload.FAKE_CREDITS, 0, boss.position(), 0, duration, "");
    }

    public static void credits(ChuckEntity boss, int duration) {
        send(boss, AuthorFxPayload.CREDITS, 0, boss.position(), 0, duration, "");
    }

    public static void hudRewrite(ChuckEntity boss, int duration) {
        send(boss, AuthorFxPayload.HUD_REWRITE, 0, boss.position(), 0, duration, "");
    }

    public static void whiteOut(ChuckEntity boss, float whiteness, int duration) {
        send(boss, AuthorFxPayload.WHITE_OUT, Math.round(whiteness * 100), boss.position(), 0, duration, "");
    }

    public static void crack(ChuckEntity boss, Vec3 at, int duration) {
        send(boss, AuthorFxPayload.CRACK, 0, at, 0, duration, "");
    }

    /** The rules changed to {@code mask} for {@code duration} (0 mask: back to normal). */
    public static void rule(ChuckEntity boss, int mask, int duration) {
        String key = mask == 0 ? "" : AuthorRules.key(Integer.lowestOneBit(mask));
        send(boss, AuthorFxPayload.RULE, mask, boss.position(), 0, duration, key);
    }

    /** {@code who}'s gravity changed to {@code mode} for {@code duration}: sent only to them. */
    public static void gravity(ChuckEntity boss, LivingEntity who, byte mode, int duration) {
        sendTo(who, new AuthorFxPayload(boss.getId(), AuthorFxPayload.GRAVITY, mode, who.position(), 0, duration, ""));
    }
}
