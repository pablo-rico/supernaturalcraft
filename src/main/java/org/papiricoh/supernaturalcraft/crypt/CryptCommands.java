package org.papiricoh.supernaturalcraft.crypt;

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
import org.papiricoh.supernaturalcraft.structure.enochian.EnochianCryptAnchors;

/**
 * {@code /supernatural crypt place}: builds an Enochian crypt with its shrine where the caller stands, as worldgen would, and
 * records it so a confession's map and {@code /locate} find it (v0.36).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class CryptCommands {

    private CryptCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("supernatural").requires(s -> s.hasPermission(2))
                .then(Commands.literal("crypt").then(Commands.literal("place").executes(CryptCommands::place))));
    }

    private static int place(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos at = BlockPos.containing(ctx.getSource().getPosition());
        level.getChunk(at);
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
        BlockPos origin = new BlockPos(at.getX(), ground - EnochianCryptAnchors.GROUND_Y, at.getZ());
        CryptPiece piece = new CryptPiece(origin, level.getSeed() ^ origin.asLong() ^ CryptStructure.SALT);
        BoundingBox box = piece.getBoundingBox();
        PlanWriter.load(level, box);
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random, box, new ChunkPos(origin), origin);
        CovenCommands.register(level, CryptStructure.KEY, piece, new ChunkPos(origin));
        BlockPos entrance = origin.offset(6, EnochianCryptAnchors.GROUND_Y, 5);
        ctx.getSource().sendSuccess(() -> Component.literal("Enochian crypt placed; entrance at " + entrance.toShortString()), true);
        return 1;
    }
}
