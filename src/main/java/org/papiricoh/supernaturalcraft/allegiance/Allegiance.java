package org.papiricoh.supernaturalcraft.allegiance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A player's allegiance (attachment {@code ALLEGIANCE}, kept through death). Immutable: every change makes a new one.
 *
 * @param faction          human, angel or demon
 * @param rank             0–4 (for a human, the hunter's rank 0–3)
 * @param essence          Grace (angel) or Corruption (demon), 0..{@link Ranks#maxEssence}
 * @param cooldownUntil    game time before which a cured player may not choose a side again
 * @param tollHearts       max-health hearts the crossroads took for "Make me one of you" (given back by the cure)
 * @param cureStage        nights of the demon cure already done (0 = none)
 * @param lastCureNight    the night index ({@code dayTime / 24000}) of the last cure rite
 * @param messenger        Heaven's messenger: {@link #MESSENGER_NONE}, {@link #MESSENGER_DUE}, {@link #MESSENGER_DECLINED}
 *                         or {@link #MESSENGER_HEEDED}
 * @param messengerDay     the day index on/after which he comes (due or declined)
 * @param pendingDemon     the hounds took a soul bound to the crossroads: the player rises a demon on respawn
 */
public record Allegiance(Faction faction, int rank, float essence, long cooldownUntil, int tollHearts, int cureStage,
                         long lastCureNight, int messenger, long messengerDay, boolean pendingDemon) {

    public static final int MESSENGER_NONE = 0, MESSENGER_DUE = 1, MESSENGER_DECLINED = 2, MESSENGER_HEEDED = 3;

    public static final Allegiance HUMAN = new Allegiance(Faction.HUMAN, 0, 0, 0, 0, 0, -1, MESSENGER_NONE, 0, false);

    public static final Codec<Allegiance> CODEC = RecordCodecBuilder.create(i -> i.group(
            Faction.CODEC.optionalFieldOf("faction", Faction.HUMAN).forGetter(Allegiance::faction),
            Codec.INT.optionalFieldOf("rank", 0).forGetter(Allegiance::rank),
            Codec.FLOAT.optionalFieldOf("essence", 0f).forGetter(Allegiance::essence),
            Codec.LONG.optionalFieldOf("cooldown_until", 0L).forGetter(Allegiance::cooldownUntil),
            Codec.INT.optionalFieldOf("toll_hearts", 0).forGetter(Allegiance::tollHearts),
            Codec.INT.optionalFieldOf("cure_stage", 0).forGetter(Allegiance::cureStage),
            Codec.LONG.optionalFieldOf("last_cure_night", -1L).forGetter(Allegiance::lastCureNight),
            Codec.INT.optionalFieldOf("messenger", MESSENGER_NONE).forGetter(Allegiance::messenger),
            Codec.LONG.optionalFieldOf("messenger_day", 0L).forGetter(Allegiance::messengerDay),
            Codec.BOOL.optionalFieldOf("pending_demon", false).forGetter(Allegiance::pendingDemon)
    ).apply(i, Allegiance::new));

    public Allegiance {
        rank = Ranks.clamp(faction, rank);
        essence = Math.max(0, Math.min(Ranks.maxEssence(faction, rank), essence));
    }

    public boolean isAngel() {
        return faction == Faction.ANGEL;
    }

    public boolean isDemon() {
        return faction == Faction.DEMON;
    }

    public boolean isHuman() {
        return faction == Faction.HUMAN;
    }

    /** Sworn to Heaven or Hell (a hunter, whatever their rank, is still free to choose). */
    public boolean committed() {
        return faction.supernatural();
    }

    /** The hunter's rank (0 unless human). */
    public int hunterRank() {
        return isHuman() ? rank : 0;
    }

    public int maxEssence() {
        return Ranks.maxEssence(faction, rank);
    }

    /** May choose a side now: human, and past any cure's cooldown. */
    public boolean mayChoose(long gameTime) {
        return isHuman() && gameTime >= cooldownUntil;
    }

    /** Turned (rank 1, a fresh bar half full); the hunter's ranks are given up. */
    public Allegiance convert(Faction to) {
        return new Allegiance(to, to.supernatural() ? 1 : 0, Ranks.maxEssence(to, 1) / 2f, cooldownUntil, tollHearts, 0, -1,
                messenger, messengerDay, false);
    }

    public Allegiance withRank(int r) {
        return new Allegiance(faction, r, essence, cooldownUntil, tollHearts, cureStage, lastCureNight, messenger, messengerDay, pendingDemon);
    }

    public Allegiance withEssence(float e) {
        return new Allegiance(faction, rank, e, cooldownUntil, tollHearts, cureStage, lastCureNight, messenger, messengerDay, pendingDemon);
    }

    public Allegiance addEssence(float delta) {
        return withEssence(essence + delta);
    }

    public Allegiance withToll(int hearts) {
        return new Allegiance(faction, rank, essence, cooldownUntil, hearts, cureStage, lastCureNight, messenger, messengerDay, pendingDemon);
    }

    public Allegiance withCure(int stage, long night) {
        return new Allegiance(faction, rank, essence, cooldownUntil, tollHearts, stage, night, messenger, messengerDay, pendingDemon);
    }

    public Allegiance withMessenger(int state, long day) {
        return new Allegiance(faction, rank, essence, cooldownUntil, tollHearts, cureStage, lastCureNight, state, day, pendingDemon);
    }

    public Allegiance withPendingDemon(boolean pending) {
        return new Allegiance(faction, rank, essence, cooldownUntil, tollHearts, cureStage, lastCureNight, messenger, messengerDay, pending);
    }

    /** Cured: human again with no ranks, the crossroads' toll repaid, and no new side until {@code cooldownUntil}. */
    public Allegiance cured(long cooldownUntil) {
        return new Allegiance(Faction.HUMAN, 0, 0, cooldownUntil, 0, 0, -1, messenger, messengerDay, false);
    }
}
