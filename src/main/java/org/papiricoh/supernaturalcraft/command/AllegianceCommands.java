package org.papiricoh.supernaturalcraft.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceRites;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.allegiance.Ranks;
import org.papiricoh.supernaturalcraft.allegiance.Toll;
import org.papiricoh.supernaturalcraft.allegiance.power.PowerCaster;
import org.papiricoh.supernaturalcraft.entity.allegiance.MessengerEntity;

import java.util.Arrays;
import java.util.Locale;

/**
 * {@code /supernatural allegiance …} (v0.13), for testing the sides without their rites:
 * <ul>
 *   <li>{@code set <human|angel|demon> [rank]}: that side and rank (default 1; 0 for a plain human), with the ascension.</li>
 *   <li>{@code rank <n>}: another rank on the same side.</li>
 *   <li>{@code essence <n>}: Grace or Corruption (clamped to the rank's bar).</li>
 *   <li>{@code messenger}: Heaven's messenger comes now.</li>
 *   <li>{@code reset}: a plain human again, no cooldowns, no toll.</li>
 * </ul>
 */
final class AllegianceCommands {

    private AllegianceCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> allegiance() {
        return Commands.literal("allegiance")
                .then(Commands.literal("set").then(Commands.argument("faction", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Faction.values()).map(Faction::getSerializedName), b))
                        .executes(ctx -> set(ctx.getSource(), StringArgumentType.getString(ctx, "faction"), -1))
                        .then(Commands.argument("rank", IntegerArgumentType.integer(0, 4))
                                .executes(ctx -> set(ctx.getSource(), StringArgumentType.getString(ctx, "faction"), IntegerArgumentType.getInteger(ctx, "rank"))))))
                .then(Commands.literal("rank").then(Commands.argument("rank", IntegerArgumentType.integer(0, 4)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Allegiance a = Allegiances.get(p);
                    AllegianceRites.ascend(p, a.withRank(IntegerArgumentType.getInteger(ctx, "rank")));
                    return 1;
                })))
                .then(Commands.literal("essence").then(Commands.argument("value", FloatArgumentType.floatArg(0)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Allegiances.set(p, Allegiances.get(p).withEssence(FloatArgumentType.getFloat(ctx, "value")));
                    float now = Allegiances.get(p).essence();
                    ctx.getSource().sendSuccess(() -> Component.literal("Essence " + now + " / " + Allegiances.get(p).maxEssence()), false);
                    return 1;
                })))
                .then(Commands.literal("messenger").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    return MessengerEntity.visit(p) != null ? 1 : 0;
                }))
                .then(Commands.literal("reset").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Allegiances.set(p, Allegiance.HUMAN);
                    Toll.apply(p);
                    PowerCaster.forget(p);
                    ctx.getSource().sendSuccess(() -> Component.literal("Human again."), false);
                    return 1;
                }));
    }

    private static int set(CommandSourceStack source, String name, int rank) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Faction faction;
        try {
            faction = Faction.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Unknown side: " + name));
            return 0;
        }
        int r = rank < 0 ? (faction.supernatural() ? 1 : 0) : Ranks.clamp(faction, rank);
        Allegiance a = Allegiances.get(p);
        Allegiance next = new Allegiance(faction, r, Ranks.maxEssence(faction, r) / 2f, 0, a.tollHearts(), 0, -1, a.messenger(), a.messengerDay(), false);
        AllegianceRites.ascend(p, next);
        return 1;
    }
}
