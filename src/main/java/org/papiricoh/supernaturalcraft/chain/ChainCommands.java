package org.papiricoh.supernaturalcraft.chain;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import org.papiricoh.supernaturalcraft.SNConfig;
import java.util.*;

public final class ChainCommands {
    private ChainCommands() {}
    public static LiteralArgumentBuilder<CommandSourceStack> chain() {
        var root = Commands.literal("chain").requires(s -> s.hasPermission(2) && SNConfig.CHAIN_ENABLED.get());
        var list = Commands.literal("list").executes(c -> list(c.getSource(), List.of(ChainFaction.values())));
        var mark = Commands.literal("mark");
        for (var f : ChainFaction.values()) {
            list.then(Commands.literal(f.id()).executes(c -> list(c.getSource(), List.of(f))));
            mark.then(Commands.literal(f.id()).then(Commands.argument("heat", IntegerArgumentType.integer(0, 1000)).executes(c -> {
                var p = c.getSource().getPlayerOrException(); int heat = IntegerArgumentType.getInteger(c, "heat");
                ChainMark.set(p, f, heat, ChainEvents.day(p.server) + ChainRules.ambushIntervalDays(heat));
                c.getSource().sendSuccess(() -> Component.translatable("chain.supernaturalcraft.command.mark", f.id(), heat), false); return heat;
            })));
        }
        return root.then(list).then(mark)
                .then(Commands.literal("spawn").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(ids(c.getSource()), b))
                        .executes(c -> {
                            String id = StringArgumentType.getString(c, "id");
                            Officer officer = resolve(c.getSource(), id);
                            boolean spawned = officer != null && OfficerSpawner.near(officer, c.getSource().getPlayerOrException()) != null;
                            c.getSource().sendSuccess(() -> Component.translatable("chain.supernaturalcraft.command." + (spawned ? "spawn" : "unavailable"), id), false);
                            return spawned ? 1 : 0;
                        })))
                .then(Commands.literal("conclave").executes(c -> {
                    ChainEvents.conclave(c.getSource().getServer());
                    ChainEvents.dispatch(c.getSource().getServer(), c.getSource().getServer().getPlayerList().getPlayers());
                    c.getSource().sendSuccess(() -> Component.translatable("chain.supernaturalcraft.command.conclave"), true); return 1;
                }))
                .then(Commands.literal("reset").executes(c -> {
                    ChainEvents.reset(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.translatable("chain.supernaturalcraft.command.reset"), true); return 1;
                }));
    }
    private static List<String> ids(CommandSourceStack source) {
        List<String> ids = new ArrayList<>(); var d = ChainSavedData.get(source.getServer());
        for (var f : ChainFaction.values()) for (var o : d.hierarchy(f).alive()) ids.add(f.id() + ":" + o.id);
        return ids;
    }
    /** Qualified ids avoid ambiguity; bare ids work only when unique across both chains. */
    private static Officer resolve(CommandSourceStack source, String id) {
        var d = ChainSavedData.get(source.getServer()); List<Officer> matches = new ArrayList<>();
        for (var f : ChainFaction.values()) for (var o : d.hierarchy(f).alive())
            if (id.equals(f.id() + ":" + o.id) || id.equals(Integer.toString(o.id))) matches.add(o);
        return matches.size() == 1 ? matches.getFirst() : null;
    }
    private static int list(CommandSourceStack source, List<ChainFaction> factions) {
        var d = ChainSavedData.get(source.getServer()); int count = 0;
        for (var f : factions) for (var o : d.hierarchy(f).all()) {
            source.sendSuccess(() -> Component.translatable("chain.supernaturalcraft.command.entry", f.id() + ":" + o.id,
                    OfficerSpawner.name(o), Component.translatable("chain.supernaturalcraft.rank." + o.rank.name().toLowerCase(Locale.ROOT)), o.level, o.status.name()), false);
            count++;
        }
        if (count == 0) source.sendSuccess(() -> Component.translatable("chain.supernaturalcraft.command.empty"), false);
        return count;
    }
}
