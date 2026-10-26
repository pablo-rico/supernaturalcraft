package org.papiricoh.supernaturalcraft.chain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.chain.narrative.Situation;
import org.papiricoh.supernaturalcraft.chain.narrative.Voice;
import org.papiricoh.supernaturalcraft.network.ChainNoticePayload;
import org.papiricoh.supernaturalcraft.network.Send;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Officers speaking aloud (v0.27): a {@link Voice} line for a {@link Situation}, heard in the chat by every player within
 * {@link #RANGE} blocks of the speaker ({@link ChainNoticePayload#SAY}). Each officer waits {@link #COOLDOWN} ticks between
 * lines, unless the moment matters ({@code force}: a duel, a death).
 */
@net.neoforged.fml.common.EventBusSubscriber(modid = org.papiricoh.supernaturalcraft.SupernaturalCraft.MODID)
public final class OfficerVoice {
    public static final int RANGE = 32, COOLDOWN = 400;
    private static final Map<OfficerSpawner.Key, Long> LAST = new HashMap<>();

    private OfficerVoice() {
    }

    /** {@code target}: %1$s (a hunter), {@code other}: %2$s (another officer), {@code detail}: %3$s; null passes empty. */
    public static boolean say(Entity speaker, Officer o, Situation s, @Nullable String target, @Nullable String other,
                              @Nullable String detail, boolean force) {
        if (!(speaker.level() instanceof ServerLevel level) || o.held()) return false;
        long now = level.getGameTime();
        var key = new OfficerSpawner.Key(o.faction, o.id);
        if (!force && now - LAST.getOrDefault(key, Long.MIN_VALUE / 2) < COOLDOWN) return false;
        String line = Voice.line(o, s, now / 20);
        if (line == null) return false;
        LAST.put(key, now);
        var payload = payload(o, line, target, other, detail);
        for (ServerPlayer p : level.players())
            if (p.distanceToSqr(speaker) < RANGE * RANGE) Send.toPlayer(p, payload);
        return true;
    }

    /** A line sent to one hunter wherever they are (a Lord's vow, a challenge thrown from afar). */
    public static void tell(ServerPlayer to, Officer o, Situation s, @Nullable String target, @Nullable String other, @Nullable String detail) {
        String line = Voice.line(o, s, to.serverLevel().getGameTime() / 20);
        if (line != null) Send.toPlayer(to, payload(o, line, target, other, detail));
    }

    private static ChainNoticePayload payload(Officer o, String line, String target, String other, String detail) {
        return new ChainNoticePayload(ChainNoticePayload.SAY, ChainNames.name(o), o.rank.name(), Voice.of(o).id(), line,
                List.of(target == null ? "" : target, other == null ? "" : other, detail == null ? "" : detail));
    }

    /** What an officer says on first showing itself to {@code viewer}: back with a scar, an ally, an old foe, or a new one. */
    static void greet(Entity mob, Officer o, ServerPlayer viewer) {
        boolean returned = "returned".equals(o.history.peekLast());
        int met = o.metWith(viewer.getUUID());
        o.meet(viewer.getUUID());
        String name = viewer.getGameProfile().getName();
        if (returned) {
            say(mob, o, Situation.RETURN_SCARRED, name, null, null, true);
            o.log("shown");
        }
        else if (ChainEvents.sameSide(viewer, o)) say(mob, o, Situation.SIGHT_ALLY, name, null, null, false);
        else say(mob, o, met > 0 || o.killsOf(viewer.getUUID()) > 0 ? Situation.SIGHT_AGAIN : Situation.SIGHT_FOE, name, null, null, true);
    }

    /** Below a quarter of its health an officer says so, once per showing ({@link Situation#FLEE}). */
    @net.neoforged.bus.api.SubscribeEvent
    public static void hurt(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        var mob = event.getEntity();
        if (mob.level().isClientSide || mob.getHealth() <= 0 || mob.getHealth() > mob.getMaxHealth() / 4
                || mob.getPersistentData().getBoolean("sn_chain_fled")) return;
        Officer o = OfficerSpawner.officer(mob);
        if (o == null || !o.alive()) return;
        mob.getPersistentData().putBoolean("sn_chain_fled", true);
        String who = event.getSource().getEntity() instanceof ServerPlayer p ? p.getGameProfile().getName() : null;
        say(mob, o, Situation.FLEE, who, null, null, true);
    }

    public static void clear() {
        LAST.clear();
    }
}
