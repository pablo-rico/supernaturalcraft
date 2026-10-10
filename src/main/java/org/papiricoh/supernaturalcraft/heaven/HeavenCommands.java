package org.papiricoh.supernaturalcraft.heaven;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.papiricoh.supernaturalcraft.heaven.gate.HeavenGate;
import org.papiricoh.supernaturalcraft.heaven.gate.HeavenGates;
import org.papiricoh.supernaturalcraft.heaven.passage.HeavenPassage;
import org.papiricoh.supernaturalcraft.heaven.passage.HeavenStanding;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlot;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlots;
import org.papiricoh.supernaturalcraft.heaven.roadhouse.Roadhouse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@code /supernatural heaven …} (permission level 2), v0.18:
 * <ul>
 *   <li>{@code tp [player]}: to your own Heaven's landing (or that player's), given and begun if need be.</li>
 *   <li>{@code roadhouse}: to Ash's Roadhouse (plot 0), written if need be.</li>
 *   <li>{@code plot}: what the plot you stand in (else your own) is: owner, index, origin, progress, seals.</li>
 *   <li>{@code build}: writes that plot again from the start; {@code finish}: writes the rest of it now, at once.</li>
 *   <li>{@code gate}: opens a rite's gate into your Heaven in front of you.</li>
 *   <li>{@code visitors}: toggles whether your Heaven welcomes visitors; {@code trust <player>}: toggles trusting them.</li>
 *   <li>{@code home}: makes your home yours (as if Zachariah had fallen); {@code win naomi|zachariah}: a victory as the bosses
 *   report it.</li>
 *   <li>{@code exit}: out of Heaven, as its exit gate would take you; {@code reset}: forgets your standing (not your plot).</li>
 * </ul>
 */
public final class HeavenCommands {

    private HeavenCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("supernatural").requires(s -> s.hasPermission(2)).then(heaven()));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> heaven() {
        return Commands.literal("heaven")
                .then(Commands.literal("tp").executes(ctx -> tp(ctx, null))
                        .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> tp(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("roadhouse").executes(HeavenCommands::roadhouse))
                .then(Commands.literal("plot").executes(HeavenCommands::plot))
                .then(Commands.literal("build").executes(ctx -> build(ctx, false)))
                .then(Commands.literal("finish").executes(ctx -> build(ctx, true)))
                .then(Commands.literal("gate").executes(HeavenCommands::gate))
                .then(Commands.literal("visitors").executes(HeavenCommands::visitors))
                .then(Commands.literal("trust").then(Commands.argument("player", EntityArgument.player()).executes(HeavenCommands::trust)))
                .then(Commands.literal("home").executes(HeavenCommands::home))
                .then(Commands.literal("win")
                        .then(Commands.literal("naomi").executes(ctx -> {
                            HeavenPlots.onNaomiDefeated(ctx.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("zachariah").executes(ctx -> {
                            HeavenPlots.onZachariahDefeated(ctx.getSource().getPlayerOrException());
                            return 1;
                        })))
                .then(Commands.literal("exit").executes(HeavenCommands::exit))
                .then(Commands.literal("reset").executes(HeavenCommands::reset));
    }

    private static int tp(CommandContext<CommandSourceStack> ctx, ServerPlayer whose) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        ServerLevel heaven = HeavenPlots.level(p.server);
        ServerPlayer owner = whose == null ? p : whose;
        HeavenPlot plot = HeavenPlots.ensure(heaven, owner);
        if (p.serverLevel() != heaven) HeavenPassage.enter(p.serverLevel(), p, p.position());
        Roadhouse.send(p, plot);
        return 1;
    }

    private static int roadhouse(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        ServerLevel heaven = HeavenPlots.level(p.server);
        HeavenPlot hub = HeavenPlots.ensureHub(heaven);
        if (p.serverLevel() != heaven) HeavenPassage.enter(p.serverLevel(), p, p.position());
        Roadhouse.send(p, hub);
        if (hub.built()) Roadhouse.ensureAsh(heaven, hub);
        return 1;
    }

    private static HeavenPlot here(ServerPlayer p) {
        ServerLevel heaven = HeavenPlots.level(p.server);
        HeavenPlot plot = p.serverLevel() == heaven ? HeavenPlots.plotAt(heaven, p.position()) : null;
        return plot != null ? plot : HeavenPlots.of(p.server, p.getUUID());
    }

    private static int plot(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        HeavenPlot plot = here(p);
        if (plot == null) {
            ctx.getSource().sendFailure(Component.translatable("commands.supernaturalcraft.heaven.no_plot"));
            return 0;
        }
        BlockPos o = HeavenPlots.origin(HeavenPlots.level(p.server), plot);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.supernaturalcraft.heaven.plot", plot.ownerName, plot.index,
                o.getX(), o.getY(), o.getZ(), HeavenPlots.progress(plot), flag(plot.wingOpen), flag(plot.liftOpen), flag(plot.homeOpen),
                flag(plot.welcome)), false);
        return 1;
    }

    private static String flag(boolean b) {
        return b ? "open" : "sealed";
    }

    private static int build(CommandContext<CommandSourceStack> ctx, boolean now) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        HeavenPlot plot = here(p);
        if (plot == null) {
            ctx.getSource().sendFailure(Component.translatable("commands.supernaturalcraft.heaven.no_plot"));
            return 0;
        }
        ServerLevel heaven = HeavenPlots.level(p.server);
        if (now) HeavenPlots.writeNow(heaven, plot);
        else HeavenPlots.rebuild(heaven, plot);
        ctx.getSource().sendSuccess(() -> Component.translatable(now ? "commands.supernaturalcraft.heaven.finished"
                : "commands.supernaturalcraft.heaven.rebuilding", plot.ownerName), true);
        return 1;
    }

    private static int gate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        if (!HeavenGates.canOpenIn(p.serverLevel())) {
            ctx.getSource().sendFailure(Component.translatable("commands.supernaturalcraft.heaven.not_here"));
            return 0;
        }
        HeavenPlots.ensure(HeavenPlots.level(p.server), p);
        // As if the altar stood where the player stands: the gate opens six blocks ahead.
        Vec3 look = Vec3.directionFromRotation(0, p.getYRot());
        BlockPos altar = BlockPos.containing(p.position().add(look.scale(1.5)));
        HeavenGates.openAtAltar(p.serverLevel(), altar, p, HeavenGate.Kind.GATE, p.getUUID());
        return 1;
    }

    private static int visitors(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        HeavenStanding s = HeavenPassage.get(p);
        HeavenPassage.set(p, s.withVisitors(!s.visitorsWelcome()));
        boolean now = !s.visitorsWelcome();
        ctx.getSource().sendSuccess(() -> Component.translatable(now ? "commands.supernaturalcraft.heaven.visitors_on"
                : "commands.supernaturalcraft.heaven.visitors_off"), false);
        return 1;
    }

    private static int trust(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        ServerPlayer other = EntityArgument.getPlayer(ctx, "player");
        HeavenStanding s = HeavenPassage.get(p);
        List<java.util.UUID> list = new ArrayList<>(s.trusted());
        boolean add = !list.remove(other.getUUID());
        if (add) list.add(other.getUUID());
        HeavenPassage.set(p, s.withTrusted(list));
        ctx.getSource().sendSuccess(() -> Component.translatable(add ? "commands.supernaturalcraft.heaven.trusted"
                : "commands.supernaturalcraft.heaven.untrusted", other.getGameProfile().getName()), false);
        return 1;
    }

    private static int home(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        HeavenPassage.set(p, HeavenPassage.get(p).withHome(true));
        HeavenPlot plot = HeavenPlots.of(p.server, p.getUUID());
        if (plot != null) HeavenPlots.openHome(HeavenPlots.level(p.server), plot);
        return 1;
    }

    private static int exit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        HeavenStanding.Link link = HeavenPassage.exitFor(p.serverLevel(), p);
        ServerLevel to = p.server.getLevel(link.dimension());
        if (to == null) return 0;
        p.teleportTo(to, link.pos().x, link.pos().y, link.pos().z, p.getYRot(), p.getXRot());
        HeavenPassage.clearReturn(p);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        HeavenPassage.set(p, HeavenStanding.NONE.withPlot(HeavenPassage.get(p).plotIndex()).withReturn(Optional.empty()));
        return 1;
    }
}
