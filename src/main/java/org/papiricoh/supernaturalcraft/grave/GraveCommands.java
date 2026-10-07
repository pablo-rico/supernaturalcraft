package org.papiricoh.supernaturalcraft.grave;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * {@code /supernatural grave place [seed]}: digs a graveyard on the ground where you stand.
 * {@code /supernatural grave raise}: raises the ghost of the nearest restless bones now, whatever the hour.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class GraveCommands {

    private GraveCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("supernatural").requires(s -> s.hasPermission(2))
                .then(Commands.literal("grave")
                        .then(Commands.literal("place")
                                .executes(ctx -> place(ctx, ctx.getSource().getLevel().getRandom().nextLong()))
                                .then(Commands.argument("seed", LongArgumentType.longArg())
                                        .executes(ctx -> place(ctx, LongArgumentType.getLong(ctx, "seed")))))
                        .then(Commands.literal("raise").executes(GraveCommands::raise))));
    }

    private static int place(CommandContext<CommandSourceStack> ctx, long seed) {
        GraveLayout.Plan plan = GraveBuilder.placeDirect(ctx.getSource().getLevel(), BlockPos.containing(ctx.getSource().getPosition()), seed);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.supernaturalcraft.grave.placed", plan.count(),
                plan.bones().toShortString(), String.valueOf(seed)), true);
        return plan.count();
    }

    private static int raise(CommandContext<CommandSourceStack> ctx) {
        var level = ctx.getSource().getLevel();
        BlockPos at = BlockPos.containing(ctx.getSource().getPosition());
        GraveBonesBlockEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-16, -8, -16), at.offset(16, 8, 16))) {
            if (level.getBlockEntity(p) instanceof GraveBonesBlockEntity be && !be.getBlockState().getValue(GraveBonesBlock.RESTED)) {
                double d = p.distSqr(at);
                if (d < bestD) {
                    bestD = d;
                    best = be;
                }
            }
        }
        if (best == null || best.raiseGhost(level) == null) {
            ctx.getSource().sendFailure(Component.translatable("commands.supernaturalcraft.grave.no_bones"));
            return 0;
        }
        BlockPos pos = best.getBlockPos();
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.supernaturalcraft.grave.raised", pos.toShortString()), true);
        return 1;
    }
}
