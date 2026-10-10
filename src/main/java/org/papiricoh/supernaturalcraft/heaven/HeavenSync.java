package org.papiricoh.supernaturalcraft.heaven;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.heaven.passage.HeavenPassage;
import org.papiricoh.supernaturalcraft.heaven.passage.HeavenStanding;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlot;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlots;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlotsSavedData;
import org.papiricoh.supernaturalcraft.network.HeavenSyncPayload;

/**
 * Sends a hunter their {@link HeavenStanding} and what they need to know of the plot they stand in (v0.18, {@link HeavenSyncPayload}).
 * <p>{@code standing}: the standing encoded with {@link HeavenStanding#CODEC} (NBT ops). {@code plot} (empty if they stand in
 * no plot and have none): the keys below; the plot they stand in while in Heaven, else their own.
 */
public final class HeavenSync {

    /** String: the plot owner's UUID. */
    public static final String OWNER = "owner";
    /** String: the plot owner's name ("Roadhouse" for plot 0). */
    public static final String OWNER_NAME = "owner_name";
    /** Int: the plot's grid index (0 = the Roadhouse). */
    public static final String INDEX = "index";
    /** Boolean: the plot is the Roadhouse. */
    public static final String HUB = "hub";
    /** Boolean: the receiving player owns this plot. */
    public static final String MINE = "mine";
    /** Boolean: fully written. Int {@link #PROGRESS}: 0-100 while it is being written. */
    public static final String BUILT = "built", PROGRESS = "progress";
    /** Booleans: the clinical wing's seal is open; Naomi's lift to the office is open; the home is unlocked. */
    public static final String WING_OPEN = "wing_open", OFFICE_OPEN = "office_open", HOME_UNLOCKED = "home_unlocked";
    /** Boolean: the owner welcomes visitors. */
    public static final String WELCOME = "welcome";

    private HeavenSync() {
    }

    public static void send(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, payload(player));
    }

    public static HeavenSyncPayload payload(ServerPlayer player) {
        HeavenStanding standing = HeavenPassage.get(player);
        CompoundTag s = new CompoundTag();
        Tag encoded = HeavenStanding.CODEC.encodeStart(NbtOps.INSTANCE, standing)
                .resultOrPartial(e -> LogUtils.getLogger().warn("Heaven standing: {}", e)).orElse(null);
        if (encoded instanceof CompoundTag c) s = c;
        return new HeavenSyncPayload(s, plotTag(player));
    }

    private static CompoundTag plotTag(ServerPlayer player) {
        ServerLevel level = HeavenPlots.level(player.server);
        HeavenPlot plot = null;
        if (player.serverLevel() == level) plot = HeavenPlots.plotAt(level, player.position());
        if (plot == null) {
            HeavenPlotsSavedData data = HeavenPlotsSavedData.peek(level);
            plot = data == null ? null : data.of(player.getUUID());
        }
        return plot == null ? new CompoundTag() : tag(plot, player);
    }

    public static CompoundTag tag(HeavenPlot plot, @Nullable ServerPlayer viewer) {
        CompoundTag t = new CompoundTag();
        t.putString(OWNER, plot.owner.toString());
        t.putString(OWNER_NAME, plot.ownerName);
        t.putInt(INDEX, plot.index);
        t.putBoolean(HUB, plot.hub());
        t.putBoolean(MINE, viewer != null && viewer.getUUID().equals(plot.owner));
        t.putBoolean(BUILT, plot.built());
        t.putInt(PROGRESS, HeavenPlots.progress(plot));
        t.putBoolean(WING_OPEN, plot.wingOpen);
        t.putBoolean(OFFICE_OPEN, plot.liftOpen);
        t.putBoolean(HOME_UNLOCKED, plot.homeOpen);
        t.putBoolean(WELCOME, plot.welcome);
        return t;
    }
}
