package org.papiricoh.supernaturalcraft.legacy.research;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.legacy.Legacies;

/**
 * {@code /supernatural legacy research …} (v0.17), for tests: {@code finish} (every running research, now), {@code grant <topic>}
 * (finish a topic with its reward, offered or not), {@code notes <topic> <n>} (field notes; quote topics with a colon:
 * {@code "creature:minecraft:zombie"}), {@code board} (list what the desk offers).
 */
public final class ResearchCommands {

    private ResearchCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> research() {
        return Commands.literal("research")
                .then(Commands.literal("finish").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    int n = ResearchService.finishAll(p);
                    ctx.getSource().sendSuccess(() -> Component.literal("Finished " + n + " research."), true);
                    return n;
                }))
                .then(Commands.literal("grant").then(Commands.argument("topic", StringArgumentType.greedyString()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    String topic = StringArgumentType.getString(ctx, "topic");
                    if (TopicKind.of(topic) == null) {
                        ctx.getSource().sendFailure(Component.literal("Unknown topic kind: " + topic));
                        return 0;
                    }
                    ResearchService.finish(p, topic);
                    ctx.getSource().sendSuccess(() -> Component.literal("Researched " + topic + "."), true);
                    return 1;
                })))
                .then(Commands.literal("notes").then(Commands.argument("topic", StringArgumentType.string())
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 640)).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            String topic = StringArgumentType.getString(ctx, "topic");
                            int left = IntegerArgumentType.getInteger(ctx, "count");
                            int total = left;
                            while (left > 0) {
                                ItemStack s = FieldNotesItem.stack(topic, Math.min(64, left));
                                left -= s.getCount();
                                if (!p.getInventory().add(s)) p.drop(s, false);
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal(total + " field notes on " + topic + "."), true);
                            return total;
                        }))))
                .then(Commands.literal("board").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    var board = ResearchService.board(p);
                    for (ResearchBoard.Topic t : board) {
                        ctx.getSource().sendSuccess(() -> Component.literal(t.topic() + " (tier " + t.tier() + ", " + t.cost().noteCount()
                                + "× " + t.cost().notes() + ", " + t.ticks() / 20 + " s)"), false);
                    }
                    int slots = ResearchService.maxSlots(p);
                    ctx.getSource().sendSuccess(() -> Component.literal(board.size() + " topics; running "
                            + Legacies.archive(p).slots().size() + "/" + slots + "."), false);
                    return board.size();
                }));
    }
}
