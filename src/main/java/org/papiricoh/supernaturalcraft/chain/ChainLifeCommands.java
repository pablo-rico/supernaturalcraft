package org.papiricoh.supernaturalcraft.chain;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import org.papiricoh.supernaturalcraft.chain.narrative.Situation;

import java.util.Locale;
import java.util.function.BiFunction;

/**
 * Test commands for the chains' social life (v0.27), under {@code /supernatural chain}: bonds, duels and challenges, fights
 * between the sides, the front and the war, a post of one's own, and making an officer talk. Officers are named
 * {@code <demon|angel> <id>}.
 */
public final class ChainLifeCommands {
    private ChainLifeCommands() {}

    private static final String OK = "chain.supernaturalcraft.command.life";

    static LiteralArgumentBuilder<CommandSourceStack> attach(LiteralArgumentBuilder<CommandSourceStack> root) {
        var bonds = Commands.literal("bonds");
        var bond = Commands.literal("bond");
        var challenge = Commands.literal("challenge");
        var talk = Commands.literal("talk");
        var say = Commands.literal("say");
        var clash = Commands.literal("clash");
        var join = Commands.literal("join");
        for (ChainFaction f : ChainFaction.values()) {
            bonds.then(officer(f, (c, o) -> bondsOf(c.getSource(), o)));
            // bond <f> <id> me <value> | bond <f> <id> <g> <id2> <value>
            var from = Commands.literal(f.id()).then(Commands.argument("id", IntegerArgumentType.integer(0))
                    .then(Commands.literal("me").then(Commands.argument("value", IntegerArgumentType.integer(Bonds.MIN, Bonds.MAX)).executes(c -> {
                        Officer o = get(c, f, "id");
                        ServerPlayer p = c.getSource().getPlayerOrException();
                        ChainSavedData.get(p.server).bonds().set(Bonds.Actor.of(o), Bonds.Actor.of(p.getUUID()), IntegerArgumentType.getInteger(c, "value"));
                        return done(c, o, "regard for you set");
                    }))));
            bond.then(from);
            for (ChainFaction g : ChainFaction.values())
                bond.then(Commands.literal(f.id() + "_" + g.id()).then(Commands.argument("a", IntegerArgumentType.integer(0))
                        .then(Commands.argument("b", IntegerArgumentType.integer(0)).then(Commands.argument("value", IntegerArgumentType.integer(Bonds.MIN, Bonds.MAX))
                                .executes(c -> {
                                    Officer a = get(c, f, "a"), b = get(c, g, "b");
                                    ChainSavedData.get(c.getSource().getServer()).bonds().set(Bonds.Actor.of(a), Bonds.Actor.of(b), IntegerArgumentType.getInteger(c, "value"));
                                    ChainSavedData.get(c.getSource().getServer()).changed();
                                    return done(c, a, "regard for " + ChainNames.name(b) + " set");
                                })))));
            challenge.then(Commands.literal(f.id()).then(Commands.argument("id", IntegerArgumentType.integer(0))
                    .executes(c -> challengeMe(c, f, null))
                    .then(Commands.argument("lethal", BoolArgumentType.bool()).executes(c -> challengeMe(c, f, BoolArgumentType.getBool(c, "lethal"))))));
            talk.then(officer(f, (c, o) -> talk(c, o)));
            var idArg = Commands.argument("id", IntegerArgumentType.integer(0));
            for (Situation s : Situation.values())
                idArg.then(Commands.literal(s.id()).executes(c -> {
                    Officer o = get(c, f, "id");
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    OfficerVoice.tell(p, o, s, p.getGameProfile().getName(), "Vesh", "near 1200, -300");
                    return 1;
                }));
            say.then(Commands.literal(f.id()).then(idArg));
        }
        for (ChainRank r : ChainRank.values())
            if (r != ChainRank.LORD) join.then(Commands.literal(r.name().toLowerCase(Locale.ROOT)).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                Officer post = ChainLife.join(p.server, p, r);
                if (post == null) { c.getSource().sendFailure(Component.translatable(OK, "no post (sworn to a side, rank I+, chain awake, a free rank?)")); return 0; }
                return done(c, post, "sworn in as " + post.rank.name());
            }));
        clash.then(Commands.argument("demon", IntegerArgumentType.integer(0)).then(Commands.argument("angel", IntegerArgumentType.integer(0))
                .then(Commands.literal("skirmish").executes(c -> clash(c, "skirmish", false))
                        .then(Commands.literal("here").executes(c -> clash(c, "skirmish", true))))
                .then(Commands.literal("champion").executes(c -> clash(c, "vow", false))
                        .then(Commands.literal("here").executes(c -> clash(c, "vow", true))))));
        return root.then(bonds).then(bond).then(challenge).then(talk).then(say).then(clash).then(join)
                .then(Commands.literal("leave").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ChainLife.leave(p.server, p.getUUID());
                    c.getSource().sendSuccess(() -> Component.translatable(OK, "left the chain"), true);
                    return 1;
                }))
                .then(Commands.literal("war").then(Commands.literal("start").executes(c -> {
                            ChainLife.declareWar(c.getSource().getServer(), null, ChainEvents.day(c.getSource().getServer()));
                            c.getSource().sendSuccess(() -> Component.translatable(OK, "war declared"), true);
                            return 1;
                        }))
                        .then(Commands.literal("end").executes(c -> {
                            var server = c.getSource().getServer();
                            ChainLife.endWar(server, ChainLife.front(server), ChainEvents.day(server));
                            c.getSource().sendSuccess(() -> Component.translatable(OK, "war ended"), true);
                            return 1;
                        })))
                .then(Commands.literal("front").then(Commands.literal("show").executes(c -> {
                            Front front = ChainLife.front(c.getSource().getServer());
                            StringBuilder b = new StringBuilder();
                            for (int i = 0; i < Front.SECTORS; i++)
                                b.append(i + 1).append(':').append(front.owner(i).id().charAt(0)).append('(').append(front.score(i)).append(") ");
                            c.getSource().sendSuccess(() -> Component.translatable(OK, b.toString().trim()), false);
                            return 1;
                        }))
                        .then(Commands.literal("set").then(Commands.argument("sector", IntegerArgumentType.integer(1, Front.SECTORS))
                                .then(Commands.literal("demon").executes(c -> setSector(c, ChainFaction.DEMON)))
                                .then(Commands.literal("angel").executes(c -> setSector(c, ChainFaction.ANGEL))))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> officer(ChainFaction f, BiFunction<CommandContext<CommandSourceStack>, Officer, Integer> run) {
        return Commands.literal(f.id()).then(Commands.argument("id", IntegerArgumentType.integer(0)).executes(c -> run.apply(c, get(c, f, "id"))));
    }

    private static Officer get(CommandContext<CommandSourceStack> c, ChainFaction f, String arg) {
        Officer o = ChainSavedData.get(c.getSource().getServer()).hierarchy(f).get(IntegerArgumentType.getInteger(c, arg));
        if (o == null || !o.alive()) throw new IllegalArgumentException("No such officer: " + f.id() + " " + IntegerArgumentType.getInteger(c, arg));
        return o;
    }

    private static int done(CommandContext<CommandSourceStack> c, Officer o, String what) {
        ChainSavedData.get(c.getSource().getServer()).changed();
        c.getSource().sendSuccess(() -> Component.translatable(OK, (o.held() ? o.playerName : ChainNames.name(o)) + ": " + what), true);
        return 1;
    }

    private static int bondsOf(CommandSourceStack source, Officer o) {
        var data = ChainSavedData.get(source.getServer());
        var list = data.bonds().of(Bonds.Actor.of(o));
        source.sendSuccess(() -> Component.translatable(OK, ChainNames.name(o) + " (" + Temperament.of(o).id() + "): " + list.size() + " bonds"), false);
        for (var b : list) source.sendSuccess(() -> Component.translatable(OK, "  -> " + b.to.id() + " " + b.regard + (b.pactUntil >= 0 ? " pact " + b.pactUntil : "")), false);
        return list.size();
    }

    private static int challengeMe(CommandContext<CommandSourceStack> c, ChainFaction f, Boolean lethal) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Officer o = get(c, f, "id");
        var data = ChainSavedData.get(p.server);
        Challenge old = ChainLife.pendingFor(data, p.getUUID());
        if (old != null) old.state = Challenge.State.DONE;
        boolean same = ChainEvents.sameSide(p, o);
        Officer post = ChainLife.held(data, p.getUUID());
        ChainLife.issue(p.server, o, p.getUUID(), lethal == null ? !same : lethal,
                same && post != null && o.rank.ordinal() < post.rank.ordinal() ? "post" : "grudge", ChainEvents.day(p.server));
        return done(c, o, "challenge thrown");
    }

    /** Brings the officer before you (alone) and opens the conversation you would have with it. */
    private static int talk(CommandContext<CommandSourceStack> c, Officer o) {
        ServerPlayer p;
        try {
            p = c.getSource().getPlayerOrException();
        } catch (CommandSyntaxException e) {
            return 0;
        }
        Mob mob = OfficerSpawner.mob(o);
        if (mob == null) mob = OfficerSpawner.alone(o, p, p.blockPosition().relative(p.getDirection(), 3));
        if (mob == null) { c.getSource().sendFailure(Component.translatable(OK, "no room for it here")); return 0; }
        if (ChainEvents.sameSide(p, o) && o.rank == ChainRank.LORD) ChainTalk.audience(p, mob, o);
        else if (ChainEvents.sameSide(p, o)) ChainTalk.ally(p, mob, o);
        else ChainTalk.parley(p, mob, o);
        return 1;
    }

    private static int clash(CommandContext<CommandSourceStack> c, String reason, boolean here) throws CommandSyntaxException {
        Officer a = get(c, ChainFaction.DEMON, "demon"), b = get(c, ChainFaction.ANGEL, "angel");
        var server = c.getSource().getServer();
        Challenge ch = ChainLife.clash(server, a, b, reason, ChainEvents.day(server));
        if (here && !ClashStage.here(c.getSource().getPlayerOrException(), ch)) {
            c.getSource().sendFailure(Component.translatable(OK, "could not stage it here; it stays pending"));
            return 0;
        }
        return done(c, a, reason + " against " + ChainNames.name(b) + (here ? " (here)" : " (pending)"));
    }

    private static int setSector(CommandContext<CommandSourceStack> c, ChainFaction f) {
        var server = c.getSource().getServer();
        int sector = IntegerArgumentType.getInteger(c, "sector");
        ChainLife.front(server).set(sector - 1, f);
        ChainSavedData.get(server).changed();
        c.getSource().sendSuccess(() -> Component.translatable(OK, "sector " + sector + " -> " + f.id()), true);
        return 1;
    }
}
