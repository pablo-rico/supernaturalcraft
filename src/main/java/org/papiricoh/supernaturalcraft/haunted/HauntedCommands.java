package org.papiricoh.supernaturalcraft.haunted;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.coven.CovenCommands;
import org.papiricoh.supernaturalcraft.coven.PlanWriter;

/** {@code /supernatural haunted place}: builds a haunted house where the caller stands, as worldgen would (v0.31). */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class HauntedCommands {

    private HauntedCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("supernatural").requires(s -> s.hasPermission(2))
                .then(Commands.literal("haunted").then(Commands.literal("place").executes(HauntedCommands::place))));
    }

    private static int place(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos at = BlockPos.containing(ctx.getSource().getPosition());
        level.getChunk(at);
        BlockPos origin = new BlockPos(at.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ()), at.getZ());
        BlockPos placed = place(level, origin, level.getSeed() ^ origin.asLong() ^ HauntedHouseStructure.SALT);
        ctx.getSource().sendSuccess(() -> Component.literal("Haunted house placed at " + placed.toShortString()), true);
        return 1;
    }

    /** Builds a house round {@code origin} through its structure piece and records the start, so {@code /locate} and the journal see it. */
    public static BlockPos place(ServerLevel level, BlockPos origin, long seed) {
        int dir = (int) Math.floorMod(seed ^ (seed >>> 29), 4L);
        HauntedHousePiece piece = new HauntedHousePiece(origin, dir, seed);
        int r = HauntedHouseLayout.RADIUS + 2;
        BoundingBox box = new BoundingBox(origin.getX() - r, origin.getY() - HauntedHouseLayout.DEPTH - 2, origin.getZ() - r,
                origin.getX() + r, origin.getY() + HauntedHouseLayout.HEIGHT + 2, origin.getZ() + r);
        PlanWriter.load(level, box);
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random, box, new ChunkPos(origin), origin);
        CovenCommands.register(level, HauntedHouseStructure.KEY, piece, new ChunkPos(origin));
        return origin;
    }
}
