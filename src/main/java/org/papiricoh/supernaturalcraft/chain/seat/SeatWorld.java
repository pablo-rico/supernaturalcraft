package org.papiricoh.supernaturalcraft.chain.seat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Command-free seat lifecycle. Call on the server thread; both factions always live in the Overworld. */
public final class SeatWorld {
    private SeatWorld() {}

    /** World-space feet positions, in stable lieutenant slot order (0..2). */
    public record Anchors(BlockPos origin, BlockPos lord, List<BlockPos> lieutenants, BlockPos entrance) {
        public Anchors {
            origin = origin.immutable();
            lord = lord.immutable();
            entrance = entrance.immutable();
            lieutenants = lieutenants.stream().map(BlockPos::immutable).toList();
            if (lieutenants.size() != 3) throw new IllegalArgumentException("Three lieutenant posts required");
        }
    }

    /** Builds once, including in old worlds; subsequent calls preserve damage and player edits, even after restart. */
    public static Anchors ensure(ServerLevel level, ChainFaction faction) {
        Objects.requireNonNull(faction);
        ServerLevel overworld = level.getServer().overworld();
        if (!overworld.getServer().isSameThread()) throw new IllegalStateException("Seats require the server thread");
        SeatData data = overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SeatData::new, SeatData::load, null), "supernaturalcraft_chain_seats");
        LayoutPlan plan = PLANS.get(faction);
        BlockPos origin = data.built.get(faction);
        if (origin == null) {
            origin = SeatPlacement.origin(overworld, faction);
            SeatPlacement.place(overworld, origin, faction, plan);
            data.built.put(faction, origin.immutable());
            data.setDirty();
        }
        return anchors(origin, plan);
    }

    private static final Map<ChainFaction, LayoutPlan> PLANS = Map.of(
            ChainFaction.DEMON, SeatLayoutCatalog.plan(ChainFaction.DEMON),
            ChainFaction.ANGEL, SeatLayoutCatalog.plan(ChainFaction.ANGEL));

    private static Anchors anchors(BlockPos origin, LayoutPlan plan) {
        return new Anchors(origin, at(origin, plan.points().get(SeatLayoutCatalog.LORD)),
                SeatLayoutCatalog.LIEUTENANTS.stream().map(n -> at(origin, plan.points().get(n))).toList(),
                at(origin, plan.points().get(SeatLayoutCatalog.ENTRANCE)));
    }

    private static BlockPos at(BlockPos origin, LayoutPoint point) {
        return origin.offset(point.x(), point.y(), point.z());
    }

    private static final class SeatData extends SavedData {
        private final Map<ChainFaction, BlockPos> built = new EnumMap<>(ChainFaction.class);

        static SeatData load(CompoundTag tag, HolderLookup.Provider registries) {
            SeatData data = new SeatData();
            for (ChainFaction faction : ChainFaction.values())
                NbtUtils.readBlockPos(tag, faction.id()).ifPresent(p -> data.built.put(faction, p));
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            built.forEach((faction, pos) -> tag.put(faction.id(), NbtUtils.writeBlockPos(pos)));
            return tag;
        }
    }
}
