package org.papiricoh.supernaturalcraft.chain;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import org.papiricoh.supernaturalcraft.SNConfig;
import java.util.*;

public final class ChainCommands {
    private ChainCommands() {}
    public static LiteralArgumentBuilder<CommandSourceStack> chain() {
        var root = Commands.literal("chain").requires(s -> s.hasPermission(2) && SNConfig.CHAIN_ENABLED.get());
        var list = Commands.literal("list").executes(c -> list(c.getSource(), List.of(ChainFaction.values())));
        var mark = Commands.literal("mark");
        var tree = Commands.literal("tree");
        var vacate = Commands.literal("vacate");
        var interregnum = Commands.literal("interregnum");
        var feud = Commands.literal("feud");
        var seat = Commands.literal("seat");
        for (var f : ChainFaction.values()) {
            tree.then(Commands.literal(f.id())
                    .then(Commands.literal("full").executes(c -> tree(c.getSource(), f, true, c.getSource().getLevel().getSeed()))
                            .then(Commands.argument("seed", com.mojang.brigadier.arguments.LongArgumentType.longArg())
                                    .executes(c -> tree(c.getSource(), f, true, com.mojang.brigadier.arguments.LongArgumentType.getLong(c, "seed")))))
                    .then(Commands.literal("partial").executes(c -> tree(c.getSource(), f, false, c.getSource().getLevel().getGameTime()))
                            .then(Commands.argument("seed", com.mojang.brigadier.arguments.LongArgumentType.longArg())
                                    .executes(c -> tree(c.getSource(), f, false, com.mojang.brigadier.arguments.LongArgumentType.getLong(c, "seed"))))));
            vacate.then(Commands.literal(f.id()).executes(c -> vacate(c.getSource(), f)));
            interregnum.then(Commands.literal(f.id()).then(Commands.literal("end").executes(c -> {
                var server = c.getSource().getServer();
                if (!ChainSavedData.get(server).court(f).vacant()) return fail(c.getSource(), "not_vacant", f.id());
                Officer heir = ChainCourt.crown(server, f, ChainEvents.day(server));
                c.getSource().sendSuccess(() -> Component.translatable("chain.supernaturalcraft.command.crowned", f.id(),
                        heir == null ? "-" : ChainNames.name(heir)), true);
                return 1;
            })));
            feud.then(Commands.literal(f.id()).executes(c -> feud(c.getSource(), f, false))
                    .then(Commands.literal("here").executes(c -> feud(c.getSource(), f, true))));
            seat.then(Commands.literal(f.id())
                    .then(Commands.literal("locate").executes(c -> {
                        var at = org.papiricoh.supernaturalcraft.chain.seat.SeatWorld.locate(c.getSource().getServer(), f);
                        c.getSource().sendSuccess(() -> Component.translatable("chain.supernaturalcraft.command.seat", f.id(),
                                at.getX(), at.getY(), at.getZ()), false);
                        return 1;
                    }))
                    .then(Commands.literal("tp").executes(c -> {
                        var p = c.getSource().getPlayerOrException();
                        var at = org.papiricoh.supernaturalcraft.chain.seat.SeatWorld.arrival(c.getSource().getServer(), f);
                        if (at == null) return fail(c.getSource(), "seat_building", f.id());
                        p.teleportTo(c.getSource().getServer().overworld(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYRot(), p.getXRot());
                        return 1;
                    }))
                    .then(Commands.literal("rebuild").executes(c -> {
                        org.papiricoh.supernaturalcraft.chain.seat.SeatWorld.rebuild(c.getSource().getServer(), f);
                        c.getSource().sendSuccess(() -> Component.translatable("chain.supernaturalcraft.command.seat_rebuild", f.id()), true);
                        return 1;
                    })));
            list.then(Commands.literal(f.id()).executes(c -> list(c.getSource(), List.of(f))));
            mark.then(Commands.literal(f.id()).then(Commands.argument("heat", IntegerArgumentType.integer(0, 1000)).executes(c -> {
                var p = c.getSource().getPlayerOrException(); int heat = IntegerArgumentType.getInteger(c, "heat");
                ChainMark.set(p, f, heat, ChainEvents.day(p.server) + ChainRules.ambushIntervalDays(heat));
                c.getSource().sendSuccess(() -> Component.translatable("chain.supernaturalcraft.command.mark", f.id(), heat), false); return heat;
            })));
        }
        return ChainLifeCommands.attach(root).then(list).then(mark).then(tree).then(vacate).then(interregnum).then(feud).then(seat)
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
    private static int tree(CommandSourceStack source, ChainFaction f, boolean full, long seed) {
        int n = ChainGates.plant(source.getServer(), f, full, seed);
        source.sendSuccess(() -> Component.translatable("chain.supernaturalcraft.command.tree", f.id(), n), true);
        return n;
    }
    private static int vacate(CommandSourceStack source, ChainFaction f) {
        var server = source.getServer(); var data = ChainSavedData.get(server); Officer lord = data.hierarchy(f).lord();
        if (lord == null) return fail(source, "no_lord", f.id());
        Mob mob = OfficerSpawner.mob(lord);
        if (mob != null) mob.discard();
        long day = ChainEvents.day(server);
        data.hierarchy(f).kill(lord, false, day + ChainRules.returnDelayDays(ChainRules.rng(server.overworld().getSeed(), lord.id, day)));
        ChainCourt.vacate(server, f, day);
        long until = data.court(f).vacantUntil;
        source.sendSuccess(() -> Component.translatable("chain.supernaturalcraft.command.vacate", f.id(), until), true);
        return 1;
    }
    private static int feud(CommandSourceStack source, ChainFaction f, boolean here) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var server = source.getServer(); var data = ChainSavedData.get(server); var court = data.court(f);
        if (ChainCourt.pending(server, f) == null) {
            var pair = Feud.pick(data.hierarchy(f), court.vacant(), ChainRules.rng(server.overworld().getSeed(), -60, server.overworld().getGameTime()));
            if (pair == null) return fail(source, "no_feud", f.id());
            court.feudChallenger = pair.challenger().id; court.feudDefender = pair.defender().id; data.changed();
        }
        if (here) {
            if (!FeudStage.here(source.getPlayerOrException(), f)) return fail(source, "no_feud", f.id());
            return 1;
        }
        var out = ChainCourt.settle(server, f, null);
        if (out == null) return fail(source, "no_feud", f.id());
        source.sendSuccess(() -> Component.translatable("chain.supernaturalcraft.command.feud", ChainNames.name(out.winner()),
                ChainNames.name(out.loser())), true);
        return 1;
    }
    private static int fail(CommandSourceStack source, String key, String faction) {
        source.sendFailure(Component.translatable("chain.supernaturalcraft.command." + key, faction));
        return 0;
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
                    OfficerSpawner.name(o), Component.translatable(o.rank.titleKey(o.faction)), o.level, o.status.name()), false);
            count++;
        }
        if (count == 0) source.sendSuccess(() -> Component.translatable("chain.supernaturalcraft.command.empty"), false);
        return count;
    }
}
