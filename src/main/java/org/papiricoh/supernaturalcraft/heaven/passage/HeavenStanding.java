package org.papiricoh.supernaturalcraft.heaven.passage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A hunter's standing in Heaven (v0.18, attachment {@code HEAVEN_STANDING}, survives death): where to send them back to, their
 * plot, whether their home is theirs yet, who may visit, their last rest and their victories there. Immutable.
 * <p>Created by the foundations; owned by the world work (it may add fields: keep the codec backward compatible with
 * {@code optionalFieldOf}).
 *
 * @param returnLink      where the gate they came in by stood (empty once used, or if they never left the world)
 * @param plotIndex       their plot's index in the grid, -1 before their first visit
 * @param homeUnlocked    Zachariah has fallen: the house, the hearth and the homecoming rite are theirs
 * @param visitorsWelcome whether Ash may send other hunters to their Heaven
 * @param trusted         hunters who may always visit and open their chests
 * @param lastRest        game time of their last rest at the hearth
 * @param naomiWins       victories over Naomi
 * @param zachariahWins   victories over Zachariah
 */
public record HeavenStanding(Optional<Link> returnLink, int plotIndex, boolean homeUnlocked, boolean visitorsWelcome,
                             List<UUID> trusted, long lastRest, int naomiWins, int zachariahWins) {

    /** A place in a dimension. */
    public record Link(ResourceKey<Level> dimension, Vec3 pos) {
        public static final Codec<Link> CODEC = RecordCodecBuilder.create(i -> i.group(
                Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Link::dimension),
                Vec3.CODEC.fieldOf("pos").forGetter(Link::pos)
        ).apply(i, Link::new));
    }

    public static final HeavenStanding NONE = new HeavenStanding(Optional.empty(), -1, false, false, List.of(), 0L, 0, 0);

    public static final Codec<HeavenStanding> CODEC = RecordCodecBuilder.create(i -> i.group(
            Link.CODEC.optionalFieldOf("return").forGetter(HeavenStanding::returnLink),
            Codec.INT.optionalFieldOf("plot", -1).forGetter(HeavenStanding::plotIndex),
            Codec.BOOL.optionalFieldOf("home", false).forGetter(HeavenStanding::homeUnlocked),
            Codec.BOOL.optionalFieldOf("visitors", false).forGetter(HeavenStanding::visitorsWelcome),
            UUIDUtil.CODEC.listOf().optionalFieldOf("trusted", List.of()).forGetter(HeavenStanding::trusted),
            Codec.LONG.optionalFieldOf("last_rest", 0L).forGetter(HeavenStanding::lastRest),
            Codec.INT.optionalFieldOf("naomi_wins", 0).forGetter(HeavenStanding::naomiWins),
            Codec.INT.optionalFieldOf("zachariah_wins", 0).forGetter(HeavenStanding::zachariahWins)
    ).apply(i, HeavenStanding::new));

    public HeavenStanding withReturn(Optional<Link> link) {
        return new HeavenStanding(link, plotIndex, homeUnlocked, visitorsWelcome, trusted, lastRest, naomiWins, zachariahWins);
    }

    public HeavenStanding withPlot(int index) {
        return new HeavenStanding(returnLink, index, homeUnlocked, visitorsWelcome, trusted, lastRest, naomiWins, zachariahWins);
    }

    public HeavenStanding withHome(boolean unlocked) {
        return new HeavenStanding(returnLink, plotIndex, unlocked, visitorsWelcome, trusted, lastRest, naomiWins, zachariahWins);
    }

    public HeavenStanding withVisitors(boolean welcome) {
        return new HeavenStanding(returnLink, plotIndex, homeUnlocked, welcome, trusted, lastRest, naomiWins, zachariahWins);
    }

    public HeavenStanding withTrusted(List<UUID> list) {
        return new HeavenStanding(returnLink, plotIndex, homeUnlocked, visitorsWelcome, List.copyOf(list), lastRest, naomiWins, zachariahWins);
    }

    public HeavenStanding withLastRest(long time) {
        return new HeavenStanding(returnLink, plotIndex, homeUnlocked, visitorsWelcome, trusted, time, naomiWins, zachariahWins);
    }

    public HeavenStanding withNaomiWin() {
        return new HeavenStanding(returnLink, plotIndex, homeUnlocked, visitorsWelcome, trusted, lastRest, naomiWins + 1, zachariahWins);
    }

    public HeavenStanding withZachariahWin() {
        return new HeavenStanding(returnLink, plotIndex, homeUnlocked, visitorsWelcome, trusted, lastRest, naomiWins, zachariahWins + 1);
    }
}
