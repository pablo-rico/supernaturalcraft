package org.papiricoh.supernaturalcraft.memory;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.Arrays;
import java.util.Locale;

/**
 * {@code /supernatural memory ...} (v0.18, level 2), for testing: {@code list}, {@code add <kind> <subject>},
 * {@code collect <id>|all}, {@code backfill}, {@code reset}, {@code stage <n>} (stages the n-th memory of the log on a stage
 * centred where you stand) and {@code leave}.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class MemoryCommands {

    private static final String KEY = "commands.supernaturalcraft.memory.";

    private MemoryCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("supernatural").requires(s -> s.hasPermission(2)).then(memory()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> memory() {
        return Commands.literal("memory")
                .then(Commands.literal("list").executes(MemoryCommands::list))
                .then(Commands.literal("add")
                        .then(Commands.argument("kind", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(MemoryKind.values())
                                        .map(MemoryText::kind), b))
                                .then(Commands.argument("subject", StringArgumentType.greedyString()).executes(MemoryCommands::add))))
                .then(Commands.literal("collect")
                        .then(Commands.literal("all").executes(ctx -> collect(ctx, null)))
                        .then(Commands.argument("id", StringArgumentType.greedyString())
                                .executes(ctx -> collect(ctx, StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("backfill").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    MemoryBackfill.run(p);
                    ctx.getSource().sendSuccess(() -> Component.translatable(KEY + "backfilled", Memories.get(p).entries().size()), true);
                    return 1;
                }))
                .then(Commands.literal("reset").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Memories.set(p, MemoryLog.EMPTY.withBackfilled());
                    ctx.getSource().sendSuccess(() -> Component.translatable(KEY + "reset"), true);
                    return 1;
                }))
                .then(Commands.literal("stage")
                        .then(Commands.argument("n", IntegerArgumentType.integer(0)).executes(MemoryCommands::stage)))
                .then(Commands.literal("leave").executes(ctx -> MemoryStage.leave(ctx.getSource().getPlayerOrException()) ? 1 : 0));
    }

    private static int list(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        MemoryLog log = Memories.get(p);
        ctx.getSource().sendSuccess(() -> Component.translatable(KEY + "list", log.entries().size(), log.collected().size()), false);
        int i = 0;
        for (Memory m : log.entries()) {
            int n = i++;
            ctx.getSource().sendSuccess(() -> Component.literal(n + ". " + (log.isCollected(m.id()) ? "[x] " : "[ ] ") + m.id() + "  ")
                    .append(Component.translatable(MemoryText.titleKey(m))), false);
        }
        for (MemorySets.Set s : MemorySets.Set.values()) {
            int have = MemorySets.progress(s, log.entries(), log.collected());
            ctx.getSource().sendSuccess(() -> Component.translatable(s.key()).append(": " + have + "/" + s.goal()), false);
        }
        return log.entries().size();
    }

    private static int add(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        MemoryKind kind;
        try {
            kind = MemoryKind.valueOf(StringArgumentType.getString(ctx, "kind").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.translatable(KEY + "no_kind"));
            return 0;
        }
        String subject = StringArgumentType.getString(ctx, "subject");
        Memory m = new Memory(MemoryText.kind(kind) + ":" + subject, kind, subject, "", Memories.gameTime(p), System.currentTimeMillis(), 1);
        boolean added = Memories.append(p, m);
        ctx.getSource().sendSuccess(() -> Component.translatable(KEY + (added ? "added" : "exists"), m.id()), true);
        return added ? 1 : 0;
    }

    private static int collect(CommandContext<CommandSourceStack> ctx, String id) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        int n = 0;
        if (id == null) {
            for (Memory m : Memories.get(p).entries()) if (Memories.collect(p, m.id())) n++;
        } else if (Memories.collect(p, id)) {
            n = 1;
        }
        int done = n;
        ctx.getSource().sendSuccess(() -> Component.translatable(KEY + "collected", done), true);
        return n;
    }

    private static int stage(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        int n = IntegerArgumentType.getInteger(ctx, "n");
        MemoryLog log = Memories.get(p);
        if (n >= log.entries().size()) {
            ctx.getSource().sendFailure(Component.translatable(KEY + "no_memory", n));
            return 0;
        }
        Memory m = log.entries().get(n);
        MemoryStage.Result r = MemoryStage.enter(p, p.getUUID(), BlockPos.containing(p.position()), m);
        ctx.getSource().sendSuccess(() -> Component.translatable(KEY + "staged", m.id(), r.name().toLowerCase(Locale.ROOT)), true);
        return 1;
    }
}
