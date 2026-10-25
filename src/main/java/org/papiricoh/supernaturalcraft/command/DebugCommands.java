package org.papiricoh.supernaturalcraft.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.debug.AuditStats;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@code /supernatural debug …}: the audit counters ({@link AuditStats}). {@code stats on|off|reset} switches and clears them;
 * {@code net} lists packets and bytes per second by payload each way; {@code perf} the probed server code; {@code dump} writes
 * all of it to {@code sn_audit.md} in the server folder, as a server with the counters on does when it stops.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class DebugCommands {

    private DebugCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> debug() {
        return Commands.literal("debug")
                .then(Commands.literal("stats")
                        .then(Commands.literal("on").executes(ctx -> toggle(ctx.getSource(), true)))
                        .then(Commands.literal("off").executes(ctx -> toggle(ctx.getSource(), false)))
                        .then(Commands.literal("reset").executes(ctx -> {
                            AuditStats.reset();
                            ctx.getSource().sendSuccess(() -> Component.literal("Audit counters cleared."), false);
                            return 1;
                        })))
                .then(Commands.literal("crowd")
                        .then(Commands.literal("spawn").then(Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 100))
                                .executes(ctx -> {
                                    int n = org.papiricoh.supernaturalcraft.debug.AuditCrowd.spawn(ctx.getSource().getLevel(), ctx.getSource().getPosition(),
                                            com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "count"), 24);
                                    ctx.getSource().sendSuccess(() -> Component.literal(n + " stand-ins walking about."), true);
                                    return n;
                                })))
                        .then(Commands.literal("gather").executes(ctx -> {
                            org.papiricoh.supernaturalcraft.debug.AuditCrowd.gather(ctx.getSource().getPosition(), 10);
                            return org.papiricoh.supernaturalcraft.debug.AuditCrowd.size();
                        }))
                        .then(Commands.literal("clear").executes(ctx -> {
                            int n = org.papiricoh.supernaturalcraft.debug.AuditCrowd.clear();
                            ctx.getSource().sendSuccess(() -> Component.literal(n + " stand-ins gone."), true);
                            return n;
                        })))
                .then(Commands.literal("net").executes(ctx -> print(ctx.getSource(), net(12))))
                .then(Commands.literal("perf").executes(ctx -> print(ctx.getSource(), perf(12))))
                .then(Commands.literal("dump").then(Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(ctx -> {
                    Path file = ctx.getSource().getServer().getServerDirectory().resolve("sn_audit_"
                            + com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "name") + ".md");
                    try {
                        dump(file, ctx.getSource().getServer());
                    } catch (IOException e) {
                        ctx.getSource().sendFailure(Component.literal("Could not write " + file + ": " + e.getMessage()));
                        return 0;
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal("Wrote " + file.toAbsolutePath()), false);
                    return 1;
                })).executes(ctx -> {
                    Path file = file(ctx.getSource().getServer().getServerDirectory());
                    try {
                        dump(file, ctx.getSource().getServer());
                    } catch (IOException e) {
                        ctx.getSource().sendFailure(Component.literal("Could not write " + file + ": " + e.getMessage()));
                        return 0;
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal("Wrote " + file.toAbsolutePath()), false);
                    return 1;
                }))
                // v0.22: angels and the Fall.
                .then(Commands.literal("fall")
                        .then(Commands.literal("start").executes(ctx -> {
                            org.papiricoh.supernaturalcraft.angel.TheFall.beginNight(ctx.getSource().getServer().overworld());
                            return say(ctx.getSource(), "The Fall's night begins (seen at night, in the Overworld)");
                        }))
                        .then(Commands.literal("stop").executes(ctx -> {
                            org.papiricoh.supernaturalcraft.angel.TheFall.endNight(ctx.getSource().getServer().overworld());
                            return say(ctx.getSource(), "The Fall's night ends");
                        }))
                        .then(Commands.literal("star").executes(ctx -> {
                            var at = ctx.getSource().getPosition();
                            var look = ctx.getSource().getRotation();
                            double yaw = Math.toRadians(look.y);
                            org.papiricoh.supernaturalcraft.angel.TheFall.star(ctx.getSource().getLevel(),
                                    at.x - Math.sin(yaw) * 100, at.z + Math.cos(yaw) * 100);
                            return say(ctx.getSource(), "One falls, 100 blocks ahead");
                        })))
                .then(Commands.literal("angels")
                        .then(Commands.literal("open").executes(ctx -> {
                            org.papiricoh.supernaturalcraft.angel.HostsSavedData.get(ctx.getSource().getServer()).setAngels(true);
                            org.papiricoh.supernaturalcraft.angel.HostsSavedData.get(ctx.getSource().getServer()).setFallen(true, false);
                            return say(ctx.getSource(), "Angels and the fallen now spawn");
                        }))
                        .then(Commands.literal("close").executes(ctx -> {
                            org.papiricoh.supernaturalcraft.angel.HostsSavedData.get(ctx.getSource().getServer()).setAngels(false);
                            org.papiricoh.supernaturalcraft.angel.HostsSavedData.get(ctx.getSource().getServer()).setFallen(false, false);
                            return say(ctx.getSource(), "Angels and the fallen no longer spawn");
                        }))
                        .then(Commands.literal("status").executes(ctx -> {
                            var d = org.papiricoh.supernaturalcraft.angel.HostsSavedData.get(ctx.getSource().getServer());
                            return say(ctx.getSource(), "Angels: " + d.angels() + ", fallen: " + d.fallen() + ", Fall pending: "
                                    + d.fallPending() + ", Fall night: " + d.fallNight());
                        })))
                .then(Commands.literal("smite").executes(ctx -> {
                    var src = ctx.getSource();
                    var near = src.getLevel().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                            net.minecraft.world.phys.AABB.ofSize(src.getPosition(), 32, 16, 32),
                            e -> org.papiricoh.supernaturalcraft.angel.SmiteDeath.angelKind(e) && e.isAlive());
                    near.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(src.getPosition())));
                    for (var e : near) {
                        if (org.papiricoh.supernaturalcraft.angel.SmiteDeath.smite(e, src.getPlayer(),
                                new net.minecraft.world.item.ItemStack(org.papiricoh.supernaturalcraft.registry.AllItems.ANGEL_BLADE.get()))) {
                            return say(src, "Smote " + e.getName().getString());
                        }
                    }
                    src.sendFailure(Component.literal("No angel (not a boss) within 16 blocks"));
                    return 0;
                }));
    }

    private static int say(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), true);
        return 1;
    }

    private static Path file(Path serverDir) {
        return serverDir.resolve("sn_audit.md");
    }

    /** Writes every counter to {@code file}. */
    public static void dump(Path file, net.minecraft.server.MinecraftServer server) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# SupernaturalCraft audit (" + fmt(AuditStats.seconds()) + " s)");
        lines.add("");
        lines.add("Players online: " + server.getPlayerList().getPlayerCount() + " (stand-ins: "
                + org.papiricoh.supernaturalcraft.debug.AuditCrowd.size() + "); average tick: "
                + fmt(server.getAverageTickTimeNanos() / 1e6) + " ms");
        lines.add("");
        lines.add("## Network");
        lines.addAll(net(Integer.MAX_VALUE));
        lines.add("");
        lines.add("## Server code");
        lines.addAll(perf(Integer.MAX_VALUE));
        Files.write(file, lines);
    }

    /** With the counters on, the server leaves its numbers behind when it stops. */
    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        if (!AuditStats.enabled()) return;
        Path file = file(event.getServer().getServerDirectory());
        try {
            dump(file, event.getServer());
            SupernaturalCraft.LOGGER.info("Audit counters written to {}", file.toAbsolutePath());
        } catch (IOException e) {
            SupernaturalCraft.LOGGER.warn("Could not write the audit counters", e);
        }
    }

    private static int toggle(CommandSourceStack source, boolean on) {
        AuditStats.enable(on);
        source.sendSuccess(() -> Component.literal("Audit counters " + (on ? "on (and cleared)." : "off.")), true);
        return 1;
    }

    private static int print(CommandSourceStack source, List<String> lines) {
        if (!AuditStats.enabled()) source.sendFailure(Component.literal("Audit counters are off: /supernatural debug stats on"));
        for (String l : lines) source.sendSuccess(() -> Component.literal(l), false);
        SupernaturalCraft.LOGGER.info("Audit:\n{}", String.join("\n", lines));
        return lines.size();
    }

    /** Payloads by bytes per second, out then in. */
    static List<String> net(int top) {
        double s = AuditStats.seconds();
        List<String> out = new ArrayList<>();
        out.add("Out (server to clients), " + fmt(s) + " s: payload | packets/s | bytes/s | largest");
        AuditStats.sent().stream().limit(top).forEach(l -> out.add("  " + l.key() + " | " + fmt(l.count() / s) + " | "
                + fmt(l.total() / s) + " | " + l.max()));
        out.add("In (clients to server): payload | packets/s | bytes/s | largest");
        AuditStats.received().stream().limit(top).forEach(l -> out.add("  " + l.key() + " | " + fmt(l.count() / s) + " | "
                + fmt(l.total() / s) + " | " + l.max()));
        return out;
    }

    /** Probed code by time per second. */
    static List<String> perf(int top) {
        double s = AuditStats.seconds();
        List<String> out = new ArrayList<>();
        out.add("Probe | calls/s | ms/s | avg us | max us (" + fmt(s) + " s)");
        AuditStats.probes().stream().limit(top).forEach(l -> out.add("  " + l.key() + " | " + fmt(l.count() / s) + " | "
                + fmt(l.total() / 1e6 / s) + " | " + fmt(l.total() / 1e3 / Math.max(1, l.count())) + " | " + fmt(l.max() / 1e3)));
        return out;
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, v >= 100 ? "%.0f" : "%.2f", v);
    }
}
