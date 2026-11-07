package org.papiricoh.supernaturalcraft.apocalypse.seal;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.supernaturalcraft.apocalypse.seal.garrison.GarrisonAnchors;
import org.papiricoh.supernaturalcraft.apocalypse.seal.garrison.GarrisonSite;
import org.papiricoh.supernaturalcraft.apocalypse.seal.garrison.GarrisonWorld;
import org.papiricoh.supernaturalcraft.entity.boss.uriel.UrielSummoning;
import org.papiricoh.supernaturalcraft.util.WorldClock;


/**
 * {@code /supernatural seals …} (v0.40, the Seals slice; debug text, not translated):
 * <ul>
 *   <li>{@code status}: the count, the stage, the next break, the events, the ledger, the caller's share;</li>
 *   <li>{@code start} (the count now), {@code break} (the next seal now), {@code mend} (a day more), {@code set <n>} (n broken),
 *   {@code stage <s>} (as many broken as the stage needs), {@code risen}, {@code echo}, {@code reset} (the count, the events and
 *   the ledger forgotten; the ledger turned back first);</li>
 *   <li>{@code event <kind>} (calls the caller to one here, now), {@code end held|broken} (the caller's event decided now),
 *   {@code revert} (every plague turned back now);</li>
 *   <li>{@code angels <n>} (the caller's angel seals held), {@code envoy} (Castiel comes now), {@code garrison} (raise the garrison
 *   now and go there), {@code garrison here} (raise it at the caller's feet), {@code uriel} (his fight at the garrison now).</li>
 * </ul>
 */
public final class SealCommands {

    private SealCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> seals() {
        LiteralArgumentBuilder<CommandSourceStack> event = Commands.literal("event");
        for (SealKind k : SealKind.values()) {
            event.then(Commands.literal(k.id()).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                SealEvent e = SealEvents.start(p.serverLevel(), p, k);
                if (e == null) e = SealEvents.startAt(p.serverLevel(), p, k, p.blockPosition().offset(8, 0, 0));
                SealEvent fe = e;
                c.getSource().sendSuccess(() -> Component.literal("Seal event " + fe.id + " (" + k.id() + ") at " + fe.pos.toShortString()), true);
                return 1;
            }));
        }
        return Commands.literal("seals")
                .then(Commands.literal("status").executes(c -> {
                    MinecraftServer server = c.getSource().getServer();
                    SealSavedData d = SealSavedData.get(server);
                    long now = WorldClock.day(server);
                    StringBuilder sb = new StringBuilder("Seals: ").append(d.started() ? "started" : "not started").append(", broken ").append(d.broken())
                            .append("/").append(SealRules.TOTAL).append(", stage ").append(SealWorld.stage(server)).append(", next in ")
                            .append(SealRules.daysLeft(now, d.nextBreak())).append(" day(s), delayed ").append(d.delayDays()).append("/")
                            .append(SealRules.DELAY_CAP).append(" (").append(d.heldSince()).append(" since the last)")
                            .append(d.risen() ? ", RISEN" : "").append(d.echo() ? ", ECHO" : "").append("; events ").append(d.events().size())
                            .append(", ledger ").append(d.ledger().size()).append("; garrison ").append(d.garrison()).append(d.garrisonBuilt() ? " (built)" : "");
                    for (SealEvent e : d.events()) {
                        sb.append("\n  #").append(e.id).append(' ').append(e.kind.id()).append(" at ").append(e.pos.toShortString()).append(", rite ")
                                .append((int) e.progress()).append(", drivers ").append(e.drivers().size()).append(", wards ").append(e.wards().size());
                    }
                    if (c.getSource().getEntity() instanceof ServerPlayer p) {
                        SealRecord r = SealWorld.record(p);
                        sb.append("\nYou: held ").append(r.defended()).append(", let break ").append(r.letBreak()).append(", angel seals ")
                                .append(r.angelsDefended()).append(", envoy ").append(r.envoy()).append(", chapter II ").append(SealWorld.chapterTwo(p));
                    }
                    c.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                    return 1;
                }))
                .then(Commands.literal("start").executes(c -> {
                    SealWorld.begin(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.literal("The count has begun."), true);
                    return 1;
                }))
                .then(Commands.literal("break").executes(c -> {
                    MinecraftServer server = c.getSource().getServer();
                    SealWorld.begin(server);
                    boolean ok = SealWorld.breakSeal(server, null);
                    c.getSource().sendSuccess(() -> Component.literal(ok ? "A seal broke." : "No seal can break now."), true);
                    return ok ? 1 : 0;
                }))
                .then(Commands.literal("mend").executes(c -> {
                    boolean ok = SealSavedData.get(c.getSource().getServer()).delay();
                    SealWorld.syncAll(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.literal(ok ? "The next seal waits a day more." : "The count cannot be held back further."), true);
                    return ok ? 1 : 0;
                }))
                .then(Commands.literal("set").then(Commands.argument("n", IntegerArgumentType.integer(0, SealRules.TOTAL)).executes(c -> {
                    setBroken(c.getSource().getServer(), IntegerArgumentType.getInteger(c, "n"));
                    c.getSource().sendSuccess(() -> Component.literal("Seals broken: " + SealWorld.broken(c.getSource().getServer())), true);
                    return 1;
                })))
                .then(Commands.literal("stage").then(Commands.argument("s", IntegerArgumentType.integer(0, SealRules.MAX_STAGE)).executes(c -> {
                    setBroken(c.getSource().getServer(), Math.min(SealRules.PASSIVE_MAX, IntegerArgumentType.getInteger(c, "s") * SealRules.PER_STAGE));
                    c.getSource().sendSuccess(() -> Component.literal("Stage " + SealWorld.stage(c.getSource().getServer())), true);
                    return 1;
                })))
                .then(Commands.literal("risen").executes(c -> {
                    SealWorld.lilithFell(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.literal("Lucifer walks."), true);
                    return 1;
                }))
                .then(Commands.literal("echo").executes(c -> {
                    SealWorld.cageClosed(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.literal("The Cage is shut: an echo."), true);
                    return 1;
                }))
                .then(Commands.literal("reset").executes(c -> {
                    MinecraftServer server = c.getSource().getServer();
                    SealEvents.endAll(server);
                    SealSavedData d = SealSavedData.get(server);
                    for (int i = 0; i < 64 && !d.ledger().isEmpty(); i++) SealPlagues.revertAll(server);
                    d.reset();
                    SealWorld.syncAll(server);
                    c.getSource().sendSuccess(() -> Component.literal("The seals are forgotten."), true);
                    return 1;
                }))
                .then(event)
                .then(Commands.literal("end")
                        .then(Commands.literal("held").executes(c -> end(c.getSource(), true)))
                        .then(Commands.literal("broken").executes(c -> end(c.getSource(), false))))
                .then(Commands.literal("revert").executes(c -> {
                    MinecraftServer server = c.getSource().getServer();
                    int n = SealSavedData.get(server).ledger().size();
                    for (int i = 0; i < 64 && !SealSavedData.get(server).ledger().isEmpty(); i++) SealPlagues.revertAll(server);
                    c.getSource().sendSuccess(() -> Component.literal("Plagues turned back: " + n), true);
                    return 1;
                }))
                .then(Commands.literal("angels").then(Commands.argument("n", IntegerArgumentType.integer(0, 99)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    SealWorld.setRecord(p, SealWorld.record(p).withAngels(IntegerArgumentType.getInteger(c, "n")));
                    c.getSource().sendSuccess(() -> Component.literal("Angel seals held: " + SealWorld.defendedAngelSeals(p)), true);
                    return 1;
                })))
                .then(Commands.literal("envoy").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    boolean ok = CastielEnvoyEntity.visit(p) != null;
                    c.getSource().sendSuccess(() -> Component.literal(ok ? "Castiel comes." : "Castiel could not come."), true);
                    return ok ? 1 : 0;
                }))
                .then(Commands.literal("garrison")
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            ServerLevel overworld = c.getSource().getServer().overworld();
                            GarrisonWorld.buildNow(overworld);
                            GarrisonSite site = GarrisonWorld.site(c.getSource().getServer());
                            if (site == null) return 0;
                            var at = site.floor(GarrisonAnchors.ENTRY);
                            p.teleportTo(overworld, at.x, at.y, at.z, p.getYRot(), p.getXRot());
                            c.getSource().sendSuccess(() -> Component.literal("The garrison at " + site.origin().toShortString()), true);
                            return 1;
                        })
                        .then(Commands.literal("here").executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            BlockPos at = p.blockPosition();
                            GarrisonSite site = GarrisonWorld.placeAt(p.serverLevel(), at, 0);
                            c.getSource().sendSuccess(() -> Component.literal("The garrison raised at " + site.origin().toShortString()), true);
                            return 1;
                        })))
                .then(Commands.literal("uriel").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    GarrisonSite site = GarrisonWorld.site(c.getSource().getServer());
                    if (site == null) {
                        c.getSource().sendFailure(Component.literal("No garrison yet: /supernatural seals garrison"));
                        return 0;
                    }
                    boolean ok = UrielSummoning.begin(c.getSource().getServer().overworld(), site, p) != null;
                    c.getSource().sendSuccess(() -> Component.literal(ok ? "Uriel waits no longer." : "Uriel cannot come now."), true);
                    return ok ? 1 : 0;
                }));
    }

    static void setBroken(MinecraftServer server, int n) {
        SealSavedData d = SealSavedData.get(server);
        d.start(WorldClock.day(server));
        d.setRisen(false);
        d.setEcho(false);
        d.setBroken(n, WorldClock.day(server));
        if (SealRules.stage(n, false) >= SealRules.ANGEL_STAGE) SealWorld.openAngels(server, d);
        SealWorld.syncAll(server);
    }

    static int end(CommandSourceStack src, boolean held) {
        ServerPlayer p;
        try {
            p = src.getPlayerOrException();
        } catch (Exception ex) {
            return 0;
        }
        SealSavedData d = SealSavedData.get(src.getServer());
        SealEvent e = d.eventOf(p.getUUID());
        if (e == null) {
            src.sendFailure(Component.literal("You are called to no seal."));
            return 0;
        }
        e.present().add(p.getUUID());
        SealEvents.finish(src.getServer().overworld(), d, e, held);
        src.sendSuccess(() -> Component.literal("Seal event " + e.id + " " + (held ? "held" : "broken") + "."), true);
        return 1;
    }
}
