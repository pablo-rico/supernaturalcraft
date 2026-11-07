package org.papiricoh.supernaturalcraft.apocalypse;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.supernaturalcraft.apocalypse.blood.BloodData;
import org.papiricoh.supernaturalcraft.apocalypse.blood.BloodRules;
import org.papiricoh.supernaturalcraft.apocalypse.blood.Bloods;
import org.papiricoh.supernaturalcraft.apocalypse.ruby.RubyFight;
import org.papiricoh.supernaturalcraft.apocalypse.ruby.RubyRules;
import org.papiricoh.supernaturalcraft.apocalypse.ruby.Rubys;
import org.papiricoh.supernaturalcraft.apocalypse.warehouse.WarehouseSite;
import org.papiricoh.supernaturalcraft.apocalypse.warehouse.WarehouseWorld;

import java.util.Locale;

/**
 * {@code /supernatural apocalypse …} (v0.40, level 2):
 * <ul>
 *   <li>{@code warehouse build|here|where}: raise Alastair's warehouse where the world wants it, here, or say where it is;</li>
 *   <li>{@code ruby step <step>|visit|ambush|state}: move Ruby's road, call her now, set her on you;</li>
 *   <li>{@code blood drink [n]|clear|dependency <0-1>|state}: doses of demon blood, the cure, the need;</li>
 *   <li>{@code migrate}: run the v0.40 migration on yourself again.</li>
 * </ul>
 */
public final class ApocalypseCommands {

    private ApocalypseCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> apocalypse() {
        LiteralArgumentBuilder<CommandSourceStack> step = Commands.literal("step");
        for (RubyRules.Step s : RubyRules.Step.values()) {
            step.then(Commands.literal(s.name().toLowerCase(Locale.ROOT)).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                Rubys.update(p, d -> d.withStep(s.ordinal()));
                c.getSource().sendSuccess(() -> Component.literal("Ruby step: " + s), true);
                return 1;
            }));
        }
        return Commands.literal("apocalypse")
                .then(Commands.literal("warehouse")
                        .then(Commands.literal("build").executes(c -> {
                            WarehouseWorld.buildNow(c.getSource().getServer().overworld());
                            return where(c.getSource());
                        }))
                        .then(Commands.literal("here").executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            WarehouseWorld.placeAt(p.serverLevel(), p.blockPosition().above(), 0);
                            return where(c.getSource());
                        }))
                        .then(Commands.literal("where").executes(c -> where(c.getSource()))))
                .then(Commands.literal("ruby")
                        .then(step)
                        .then(Commands.literal("visit").executes(c -> RubyFight.visitNow(c.getSource().getPlayerOrException()) != null ? 1 : 0))
                        .then(Commands.literal("ambush").executes(c -> RubyFight.ambush(c.getSource().getPlayerOrException()) != null ? 1 : 0))
                        .then(Commands.literal("state").executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            c.getSource().sendSuccess(() -> Component.literal("Ruby: " + Rubys.step(p) + " " + Rubys.get(p)), false);
                            return 1;
                        })))
                .then(Commands.literal("blood")
                        .then(Commands.literal("drink").executes(c -> drink(c.getSource(), 1))
                                .then(Commands.argument("doses", IntegerArgumentType.integer(1, 30))
                                        .executes(c -> drink(c.getSource(), IntegerArgumentType.getInteger(c, "doses")))))
                        .then(Commands.literal("clear").executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            Bloods.update(p, d -> BloodData.EMPTY);
                            return 1;
                        }))
                        .then(Commands.literal("dependency").then(Commands.argument("value", FloatArgumentType.floatArg(0, 1)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            float v = FloatArgumentType.getFloat(c, "value");
                            Bloods.update(p, d -> d.withDependency(v));
                            return 1;
                        })))
                        .then(Commands.literal("state").executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            BloodData d = Bloods.get(p);
                            c.getSource().sendSuccess(() -> Component.literal("Blood " + d + ", tier " + BloodRules.tier(d.doses())
                                    + (BloodRules.withdrawal(d) ? ", in withdrawal" : "")), false);
                            return 1;
                        })))
                .then(Commands.literal("migrate").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Rubys.update(p, d -> new org.papiricoh.supernaturalcraft.apocalypse.ruby.RubyData(d.step(), d.lastVisit(), d.scenes(),
                            d.flags() & ~org.papiricoh.supernaturalcraft.apocalypse.ruby.RubyData.MIGRATED));
                    ApocalypseMigration.migrate(p);
                    return 1;
                }));
    }

    private static int drink(CommandSourceStack source, int n) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        for (int i = 0; i < n; i++) Bloods.drink(p);
        return n;
    }

    private static int where(CommandSourceStack source) {
        WarehouseSite s = WarehouseWorld.site(source.getServer());
        if (s == null) {
            source.sendFailure(Component.literal("No warehouse yet."));
            return 0;
        }
        var t = s.trap();
        source.sendSuccess(() -> Component.literal("Alastair's warehouse: trap at " + t.getX() + " " + t.getY() + " " + t.getZ()
                + (WarehouseWorld.building() ? " (building)" : "")), false);
        return 1;
    }
}
