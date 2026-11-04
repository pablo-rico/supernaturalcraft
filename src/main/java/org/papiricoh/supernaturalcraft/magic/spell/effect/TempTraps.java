package org.papiricoh.supernaturalcraft.magic.spell.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.hunter.DevilsTrapBlock;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Devil's traps thrown by Cast Trap (v0.36): painted in whatever ninths of the 3x3 are free and stand on solid ground, wiped
 * when their time is up (only the ninths still there and still theirs). Not saved: a trap thrown before a restart stays.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class TempTraps {

    private record Thrown(ResourceKey<Level> dimension, List<BlockPos> cells, long until) {
    }

    private static final List<Thrown> THROWN = new ArrayList<>();

    private TempTraps() {
    }

    /** Paints a trap centred on {@code center} for {@code ticks}. @return whether any of it was painted */
    public static boolean place(ServerLevel level, BlockPos center, int ticks) {
        BlockState base = AllBlocks.DEVILS_TRAP.get().defaultBlockState();
        List<BlockPos> cells = new ArrayList<>();
        for (int part = 0; part < 9; part++) {
            BlockPos p = center.offset(DevilsTrapBlock.dx(part), 0, DevilsTrapBlock.dz(part));
            BlockState here = level.getBlockState(p);
            if ((here.isAir() || here.canBeReplaced()) && here.getFluidState().isEmpty()
                    && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) {
                cells.add(p.immutable());
            }
        }
        if (cells.size() < 9) return false;
        // Set the ninths without updates first: a seal missing a ninth breaks itself.
        for (BlockPos p : cells) {
            int part = (p.getZ() - center.getZ() + 1) * 3 + (p.getX() - center.getX() + 1);
            level.setBlock(p, base.setValue(DevilsTrapBlock.PART, part), Block.UPDATE_CLIENTS);
        }
        synchronized (THROWN) {
            THROWN.add(new Thrown(level.dimension(), cells, level.getGameTime() + ticks));
        }
        return true;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        synchronized (THROWN) {
            if (THROWN.isEmpty()) return;
            for (Iterator<Thrown> it = THROWN.iterator(); it.hasNext(); ) {
                Thrown t = it.next();
                ServerLevel level = server.getLevel(t.dimension());
                if (level == null) {
                    it.remove();
                    continue;
                }
                if (level.getGameTime() < t.until()) continue;
                for (BlockPos p : t.cells()) {
                    if (level.isLoaded(p) && level.getBlockState(p).is(AllBlocks.DEVILS_TRAP.get())) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
                it.remove();
            }
        }
    }
}
