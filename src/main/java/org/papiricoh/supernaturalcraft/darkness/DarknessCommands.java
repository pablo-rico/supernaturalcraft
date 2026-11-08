package org.papiricoh.supernaturalcraft.darkness;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.darkness.casifer.CasiferEntity;
import org.papiricoh.supernaturalcraft.darkness.fog.FogOutbreaks;
import org.papiricoh.supernaturalcraft.darkness.superior.Superior;
import org.papiricoh.supernaturalcraft.dyingsun.DyingSun;
import org.papiricoh.supernaturalcraft.dyingsun.DyingSunSavedData;

import java.util.Locale;

/**
 * Season 11's debug commands (v0.42), under the mod's {@code /supernatural} root (level 2; the output is for developers):
 * <ul>
 *   <li>{@code darkness state|reset|step <step>|release|superior|cage|reveal|taken|visit|hand <kind>|fog|casifer};</li>
 *   <li>{@code sun state|begin|restore|clear|set <darkness>}.</li>
 * </ul>
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class DarknessCommands {

    private DarknessCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("supernatural").requires(s -> s.hasPermission(2)).then(darkness()).then(sun()));
    }

    private static int say(CommandSourceStack s, String text) {
        s.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> darkness() {
        return Commands.literal("darkness")
                .then(Commands.literal("state").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Darkness d = Darknesses.get(p);
                    return say(c.getSource(), "step=" + d.stage() + " since=" + d.since() + " hands=" + Integer.toBinaryString(d.hands())
                            + " visited=" + d.visited() + " bond=" + d.bond() + " souls=" + d.souls() + " sun=" + DyingSunSavedData.get(p.server).state());
                }))
                .then(Commands.literal("reset").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Darknesses.set(p, Darkness.EMPTY.withSeen(DarknessMigration.MIGRATED));
                    return say(c.getSource(), "season 11 reset");
                }))
                .then(Commands.literal("step").then(Commands.argument("step", StringArgumentType.word()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    DarknessRules.Step s = DarknessRules.Step.valueOf(StringArgumentType.getString(c, "step").toUpperCase(Locale.ROOT));
                    Darknesses.update(p, d -> d.withStep(s, Darknesses.today(p.server)));
                    return say(c.getSource(), "step " + s);
                })))
                .then(Commands.literal("release").executes(c -> {
                    DarknessRelease.release(c.getSource().getPlayerOrException());
                    return say(c.getSource(), "released");
                }))
                .then(Commands.literal("superior").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    var at = Superior.ensure(p.server.overworld());
                    p.teleportTo(p.server.overworld(), at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 0, 0);
                    return say(c.getSource(), "Superior at " + at.toShortString());
                }))
                .then(Commands.literal("cage").executes(c -> say(c.getSource(), "cage " + DarknessStory.visitCage(c.getSource().getPlayerOrException()))))
                .then(Commands.literal("reveal").executes(c -> say(c.getSource(), "reveal " + DarknessStory.reveal(c.getSource().getPlayerOrException()))))
                .then(Commands.literal("taken").executes(c -> say(c.getSource(), "taken " + DarknessStory.take(c.getSource().getPlayerOrException()))))
                .then(Commands.literal("visit").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Darkness d = Darknesses.get(p);
                    org.papiricoh.supernaturalcraft.darkness.visit.AmaraVisitorEntity.visit(p, Math.max(1, DarknessRules.milestone(d.stage(), d.handCount())),
                            DarknessRules.ageFor(d.stage(), d.handCount()));
                    return say(c.getSource(), "visit");
                }))
                .then(Commands.literal("hand").then(Commands.argument("kind", StringArgumentType.word()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    S11Assets.Hand h = S11Assets.Hand.valueOf(StringArgumentType.getString(c, "kind").toUpperCase(Locale.ROOT));
                    Darknesses.handClaimed(p, h);
                    return say(c.getSource(), "hand " + h + " claimed");
                })))
                .then(Commands.literal("fog").executes(c -> say(c.getSource(), "took "
                        + FogOutbreaks.outbreak(c.getSource().getLevel(), c.getSource().getPlayerOrException()))))
                .then(Commands.literal("casifer").executes(c -> say(c.getSource(), "casifer " + (CasiferEntity.arrive(c.getSource().getPlayerOrException()) != null))));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> sun() {
        return Commands.literal("sun")
                .then(Commands.literal("state").executes(c -> {
                    var d = DyingSunSavedData.get(c.getSource().getServer());
                    return say(c.getSource(), "state=" + d.state() + " start=" + d.startTick() + " darkness=" + DyingSun.compute(c.getSource().getServer()));
                }))
                .then(Commands.literal("begin").executes(c -> say(c.getSource(), "began " + DyingSun.begin(c.getSource().getServer(), c.getSource().getPlayerOrException()))))
                .then(Commands.literal("restore").executes(c -> say(c.getSource(), "restored " + DyingSun.restore(c.getSource().getServer()))))
                .then(Commands.literal("clear").executes(c -> {
                    DyingSun.clear(c.getSource().getServer());
                    return say(c.getSource(), "cleared");
                }))
                .then(Commands.literal("set").then(Commands.argument("darkness", FloatArgumentType.floatArg(0, 0.84f)).executes(c -> {
                    DyingSun.preview(c.getSource().getServer(), FloatArgumentType.getFloat(c, "darkness"));
                    return say(c.getSource(), "set");
                })));
    }
}
