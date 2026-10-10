package org.papiricoh.supernaturalcraft.crossroads.wild;

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

/** {@code /supernatural crossroads place [seed]}: lays a natural crossroads on the ground where you stand (v0.18). */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class CrossroadsWildCommands {

    private CrossroadsWildCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("supernatural").requires(s -> s.hasPermission(2))
                .then(Commands.literal("crossroads")
                        .then(Commands.literal("place")
                                .executes(ctx -> place(ctx, ctx.getSource().getLevel().getRandom().nextLong()))
                                .then(Commands.argument("seed", LongArgumentType.longArg())
                                        .executes(ctx -> place(ctx, LongArgumentType.getLong(ctx, "seed")))))));
    }

    private static int place(CommandContext<CommandSourceStack> ctx, long seed) {
        BlockPos origin = CrossroadsBuilder.placeDirect(ctx.getSource().getLevel(), BlockPos.containing(ctx.getSource().getPosition()), seed);
        BlockPos soil = CrossroadsBuilder.centre(origin);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.supernaturalcraft.crossroads.placed", soil.toShortString(),
                String.valueOf(seed)), true);
        return 1;
    }
}
