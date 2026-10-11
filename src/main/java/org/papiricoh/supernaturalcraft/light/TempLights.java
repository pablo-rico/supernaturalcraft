package org.papiricoh.supernaturalcraft.light;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;

/**
 * Light that lingers a moment: an invisible vanilla light block in an empty space, removed again after a while. Inside a boss
 * arena it goes through the arena so it is always restored; outside, each one's end is saved with the level (v0.19), so a
 * restart or a crash never leaves light blocks behind for good.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class TempLights extends SavedData {

    private static final String NAME = "supernaturalcraft_temp_lights";
    /** Light block position → the game tick it goes out. */
    private final Long2LongOpenHashMap until = new Long2LongOpenHashMap();

    public static TempLights get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(TempLights::new, TempLights::load), NAME);
    }

    private static TempLights load(CompoundTag tag, HolderLookup.Provider registries) {
        TempLights t = new TempLights();
        long[] pos = tag.getLongArray("Pos"), end = tag.getLongArray("Until");
        for (int i = 0; i < Math.min(pos.length, end.length); i++) t.until.put(pos[i], end[i]);
        return t;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLongArray("Pos", until.keySet().toLongArray());
        tag.putLongArray("Until", until.values().toLongArray());
        return tag;
    }

    public static boolean place(ServerLevel level, BlockPos pos, int lightLevel, int ticks) {
        BlockState now = level.getBlockState(pos);
        if (!now.isAir() && !now.is(Blocks.LIGHT)) return false;
        if (now.is(Blocks.LIGHT) && now.getValue(LightBlock.LEVEL) >= lightLevel) return false;
        BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, Math.min(15, lightLevel));
        ArenaController arena = ArenaSavedData.get(level).at(pos.getCenter());
        if (arena != null) return arena.mutate(level, pos, light, ticks);
        level.setBlock(pos, light, 3);
        TempLights data = get(level);
        data.until.merge(pos.asLong(), level.getGameTime() + ticks, Math::max);
        data.setDirty();
        return true;
    }

    /** How many lights are waiting to go out (tests). */
    public int pending() {
        return until.size();
    }

    /** Puts out every light that is due and loaded (one left in an unloaded chunk waits for it). */
    public void tick(ServerLevel level) {
        if (until.isEmpty()) return;
        long now = level.getGameTime();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        var it = until.long2LongEntrySet().fastIterator();
        boolean changed = false;
        while (it.hasNext()) {
            Long2LongMap.Entry e = it.next();
            if (e.getLongValue() > now) continue;
            m.set(e.getLongKey());
            if (!level.isLoaded(m)) continue;
            if (level.getBlockState(m).is(Blocks.LIGHT)) level.setBlock(m, Blocks.AIR.defaultBlockState(), 3);
            it.remove();
            changed = true;
        }
        if (changed) setDirty();
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % 5 == 0) {
            get(level).tick(level);
        }
    }
}
