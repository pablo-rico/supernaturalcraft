package org.papiricoh.supernaturalcraft.crossroads;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A player's standing with the crossroads (a data attachment that survives death). {@code NONE}
 * until a deal is sealed. Owned by the crossroads package: see {@link DealTerms} for the rules
 * and {@link Debts} for how they are enforced.
 *
 * @param wish          the wish's id ({@link DealTerms.Wish#id()})
 * @param arg           which variant of the wish (hearts or mana, items or pet)
 * @param hounds        the pack still hunting (COLLECTING)
 * @param demon         the hostile demon walking (HUNTED), if it is in its vessel
 * @param penaltyPending a collected soul still owes its hollowness: applied on respawn
 * @param demonGoneAt   day time at which the hunted demon last fled its vessel
 * @param soulBound     "Bind my soul" (v0.13): if the hounds collect, the soul comes back a demon instead of hollow
 * @param wild          struck at a natural crossroads (v0.18): a shorter term, a bigger pack, a longer hunt
 * @param lastWildDay   {@link DealTerms#nightIndex} of the night this hunter last buried a crossroads box (kept from deal to deal)
 */
public record CrossroadsDeal(State state, String wish, long sealedAt, long dueAt, List<UUID> hounds,
                             Optional<UUID> demon, long huntStartedAt, int arg, boolean penaltyPending,
                             long demonGoneAt, boolean soulBound, boolean wild, long lastWildDay) {

    public enum State {
        /** No deal, or none since the last was settled. */
        NONE,
        /** Sealed; the debt is not yet due. */
        OPEN,
        /** Due: the hounds are out. */
        COLLECTING,
        /** The deal is being broken: the demon walks again, and must die. */
        HUNTED,
        /** Settled by surviving the hunt or killing the demon. */
        FREE,
        /** Settled by dying: the soul was taken. */
        COLLECTED;

        public static final Codec<State> CODEC = Codec.STRING.xmap(s -> {
            try {
                return State.valueOf(s.toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return NONE;
            }
        }, s -> s.name().toLowerCase(java.util.Locale.ROOT));
    }

    /** No crossroads box buried yet. */
    public static final long NEVER = Long.MIN_VALUE;

    public static final CrossroadsDeal NONE = new CrossroadsDeal(State.NONE, "", 0, 0, List.of(), Optional.empty(), 0, 0, false, 0, false, false, NEVER);

    public static final Codec<CrossroadsDeal> CODEC = RecordCodecBuilder.create(i -> i.group(
            State.CODEC.optionalFieldOf("state", State.NONE).forGetter(CrossroadsDeal::state),
            Codec.STRING.optionalFieldOf("wish", "").forGetter(CrossroadsDeal::wish),
            Codec.LONG.optionalFieldOf("sealed_at", 0L).forGetter(CrossroadsDeal::sealedAt),
            Codec.LONG.optionalFieldOf("due_at", 0L).forGetter(CrossroadsDeal::dueAt),
            UUIDUtil.CODEC.listOf().optionalFieldOf("hounds", List.of()).forGetter(CrossroadsDeal::hounds),
            UUIDUtil.CODEC.optionalFieldOf("demon").forGetter(CrossroadsDeal::demon),
            Codec.LONG.optionalFieldOf("hunt_started_at", 0L).forGetter(CrossroadsDeal::huntStartedAt),
            Codec.INT.optionalFieldOf("arg", 0).forGetter(CrossroadsDeal::arg),
            Codec.BOOL.optionalFieldOf("penalty_pending", false).forGetter(CrossroadsDeal::penaltyPending),
            Codec.LONG.optionalFieldOf("demon_gone_at", 0L).forGetter(CrossroadsDeal::demonGoneAt),
            Codec.BOOL.optionalFieldOf("soul_bound", false).forGetter(CrossroadsDeal::soulBound),
            Codec.BOOL.optionalFieldOf("wild", false).forGetter(CrossroadsDeal::wild),
            Codec.LONG.optionalFieldOf("last_wild_day", NEVER).forGetter(CrossroadsDeal::lastWildDay)
    ).apply(i, CrossroadsDeal::new));

    public CrossroadsDeal {
        hounds = List.copyOf(hounds);
    }

    /** A freshly sealed deal. */
    public static CrossroadsDeal sealed(DealTerms.Wish wish, int arg, long now) {
        return sealed(wish, arg, now, wish.days);
    }

    /** A freshly sealed deal with a term of {@code days} (a wild bargain's is shorter). */
    public static CrossroadsDeal sealed(DealTerms.Wish wish, int arg, long now, int days) {
        return new CrossroadsDeal(State.OPEN, wish.id(), now, DealTerms.dueAt(now, days), List.of(), Optional.empty(), 0, arg, false, 0, false,
                false, NEVER);
    }

    /** Whether a debt is outstanding (a new deal cannot be made meanwhile). */
    public boolean active() {
        return DealTerms.active(state);
    }

    /** @return the wish, or null for no (or an unknown) deal */
    public DealTerms.Wish wishKind() {
        return DealTerms.Wish.byId(wish);
    }

    public CrossroadsDeal withState(State s) {
        return new CrossroadsDeal(s, wish, sealedAt, dueAt, hounds, demon, huntStartedAt, arg, penaltyPending, demonGoneAt, soulBound, wild, lastWildDay);
    }

    public CrossroadsDeal withHounds(List<UUID> h) {
        return new CrossroadsDeal(state, wish, sealedAt, dueAt, h, demon, huntStartedAt, arg, penaltyPending, demonGoneAt, soulBound, wild, lastWildDay);
    }

    public CrossroadsDeal withDemon(Optional<UUID> d) {
        return new CrossroadsDeal(state, wish, sealedAt, dueAt, hounds, d, huntStartedAt, arg, penaltyPending, demonGoneAt, soulBound, wild, lastWildDay);
    }

    public CrossroadsDeal withHuntStartedAt(long t) {
        return new CrossroadsDeal(state, wish, sealedAt, dueAt, hounds, demon, t, arg, penaltyPending, demonGoneAt, soulBound, wild, lastWildDay);
    }

    public CrossroadsDeal withDueAt(long t) {
        return new CrossroadsDeal(state, wish, sealedAt, t, hounds, demon, huntStartedAt, arg, penaltyPending, demonGoneAt, soulBound, wild, lastWildDay);
    }

    public CrossroadsDeal withPenaltyPending(boolean p) {
        return new CrossroadsDeal(state, wish, sealedAt, dueAt, hounds, demon, huntStartedAt, arg, p, demonGoneAt, soulBound, wild, lastWildDay);
    }

    public CrossroadsDeal withSoulBound(boolean b) {
        return new CrossroadsDeal(state, wish, sealedAt, dueAt, hounds, demon, huntStartedAt, arg, penaltyPending, demonGoneAt, b, wild, lastWildDay);
    }

    public CrossroadsDeal withDemonGoneAt(long t) {
        return new CrossroadsDeal(state, wish, sealedAt, dueAt, hounds, demon, huntStartedAt, arg, penaltyPending, t, soulBound, wild, lastWildDay);
    }

    public CrossroadsDeal withWild(boolean w) {
        return new CrossroadsDeal(state, wish, sealedAt, dueAt, hounds, demon, huntStartedAt, arg, penaltyPending, demonGoneAt, soulBound, w, lastWildDay);
    }

    public CrossroadsDeal withLastWildDay(long night) {
        return new CrossroadsDeal(state, wish, sealedAt, dueAt, hounds, demon, huntStartedAt, arg, penaltyPending, demonGoneAt, soulBound, wild, night);
    }
}
