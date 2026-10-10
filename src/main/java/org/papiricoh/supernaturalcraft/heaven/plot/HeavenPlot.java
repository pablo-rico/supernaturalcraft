package org.papiricoh.supernaturalcraft.heaven.plot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * One plot of Heaven as the world remembers it (v0.18): whose it is, where (its grid index), how it is built (style and seed,
 * so the plan can be rebuilt the same), how far its writer has got, which of its seals are open, and a mirror of its owner's
 * welcome so Ash can send visitors while the owner is away. Mutable; owned by {@link HeavenPlotsSavedData}, which must be marked
 * dirty after a change.
 */
public final class HeavenPlot {

    public final UUID owner;
    public final int index;
    public String ownerName;
    public HeavenPlotLayout.Style style;
    public long seed;
    /** The writer's position ({@link PlotWriter.Cursor}); null before the first write. */
    public PlotWriter.Cursor cursor;
    /** Whether its gate plaza has been written (someone can land). */
    public boolean plaza;
    /** Chunks forced while it is written (freed when done, also after a restart). */
    public final Set<Long> forced = new HashSet<>();
    /** The home yard's box relative to the origin (min x, y, z, max x, y, z), from the plan's {@code home} zone. */
    public int[] homeBox;
    /** Seals opened: the clinical wing's door, Naomi's lift, the home's door. */
    public boolean wingOpen, liftOpen, homeOpen;
    /** Mirror of the owner's {@code HeavenStanding}: visitors welcome, and who they trust. */
    public boolean welcome;
    public final List<UUID> trusted = new ArrayList<>();
    /** Victories the owner has not been told of yet (they were away when the boss fell). */
    public int pendingNaomi, pendingZachariah;
    /** The Roadhouse's Ash (plot 0 only). */
    public @Nullable UUID ash;

    public HeavenPlot(UUID owner, int index, String ownerName, HeavenPlotLayout.Style style, long seed) {
        this.owner = owner;
        this.index = index;
        this.ownerName = ownerName;
        this.style = style;
        this.seed = seed;
    }

    public boolean hub() {
        return index == PlotGrid.HUB;
    }

    public boolean built() {
        return cursor != null && cursor.stage() == PlotWriter.Stage.DONE;
    }

    public boolean writing() {
        return cursor != null && cursor.stage() != PlotWriter.Stage.DONE;
    }

    CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putUUID("Owner", owner);
        t.putInt("Index", index);
        t.putString("Name", ownerName);
        t.putString("Style", style.name().toLowerCase(Locale.ROOT));
        t.putLong("Seed", seed);
        if (cursor != null) t.put("Cursor", cursor.save());
        t.putBoolean("Plaza", plaza);
        ListTag f = new ListTag();
        for (long k : forced) f.add(LongTag.valueOf(k));
        t.put("Forced", f);
        if (homeBox != null) t.putIntArray("HomeBox", homeBox);
        t.putBoolean("WingOpen", wingOpen);
        t.putBoolean("LiftOpen", liftOpen);
        t.putBoolean("HomeOpen", homeOpen);
        t.putBoolean("Welcome", welcome);
        ListTag tr = new ListTag();
        for (UUID u : trusted) tr.add(NbtUtils.createUUID(u));
        t.put("Trusted", tr);
        t.putInt("PendingNaomi", pendingNaomi);
        t.putInt("PendingZachariah", pendingZachariah);
        if (ash != null) t.putUUID("Ash", ash);
        return t;
    }

    static HeavenPlot load(CompoundTag t) {
        HeavenPlotLayout.Style style;
        try {
            style = HeavenPlotLayout.Style.valueOf(t.getString("Style").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            style = HeavenPlotLayout.Style.HUNTER;
        }
        HeavenPlot p = new HeavenPlot(t.getUUID("Owner"), t.getInt("Index"), t.getString("Name"), style, t.getLong("Seed"));
        if (t.contains("Cursor")) p.cursor = PlotWriter.Cursor.load(t.getCompound("Cursor"));
        p.plaza = t.getBoolean("Plaza");
        for (Tag k : t.getList("Forced", Tag.TAG_LONG)) p.forced.add(((LongTag) k).getAsLong());
        if (t.contains("HomeBox")) {
            int[] b = t.getIntArray("HomeBox");
            p.homeBox = b.length == 6 ? b : null;
        }
        p.wingOpen = t.getBoolean("WingOpen");
        p.liftOpen = t.getBoolean("LiftOpen");
        p.homeOpen = t.getBoolean("HomeOpen");
        p.welcome = t.getBoolean("Welcome");
        for (Tag u : t.getList("Trusted", Tag.TAG_INT_ARRAY)) p.trusted.add(NbtUtils.loadUUID(u));
        p.pendingNaomi = t.getInt("PendingNaomi");
        p.pendingZachariah = t.getInt("PendingZachariah");
        if (t.hasUUID("Ash")) p.ash = t.getUUID("Ash");
        return p;
    }
}
