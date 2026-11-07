package org.papiricoh.supernaturalcraft.purgatory;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.alpha.AlphaSpawns;
import org.papiricoh.supernaturalcraft.pact.PactRules;
import org.papiricoh.supernaturalcraft.purgatory.glade.EveGladePlan;
import org.papiricoh.supernaturalcraft.purgatory.glade.EveGladeWorld;

import java.util.Locale;

/**
 * {@code /supernatural purgatory …} and {@code /supernatural alpha …} (v0.39):
 * <ul>
 *   <li>{@code purgatory go}: into Purgatory now (the landing), the way back remembered;</li>
 *   <li>{@code purgatory back}: home now, wherever one came from;</li>
 *   <li>{@code purgatory glade}: raise Eve's glade now and go there;</li>
 *   <li>{@code purgatory reset}: the caller's standing forgotten (never been, never got out), and the glade's site with it;</li>
 *   <li>{@code alpha spawn <vampire|werewolf|shapeshifter>}: that Alpha comes for the caller, a few blocks off.</li>
 * </ul>
 */
public final class PurgatoryCommands {

    private PurgatoryCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> purgatory() {
        return Commands.literal("purgatory")
                .then(Commands.literal("go").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ServerLevel to = PurgatoryWorld.level(c.getSource().getServer());
                    PurgatoryWorld.send(p, to, PurgatoryWorld.landing(to));
                    return 1;
                }))
                .then(Commands.literal("back").executes(c -> {
                    PurgatoryWorld.goHome(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("glade").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ServerLevel level = PurgatoryWorld.level(c.getSource().getServer());
                    EveGladeWorld.buildNow(level);
                    BlockPos entry = EveGladeWorld.point(level, EveGladePlan.ENTRY);
                    if (!PurgatoryWorld.inPurgatory(p.level())) PurgatoryWorld.rememberWayBack(p, p.serverLevel(), p.position());
                    p.teleportTo(level, entry.getX() + 0.5, entry.getY(), entry.getZ() + 0.5, 180, 0);
                    c.getSource().sendSuccess(() -> Component.literal("Eve's glade at " + EveGladeWorld.site(level).toShortString()), true);
                    return 1;
                }))
                .then(Commands.literal("reset").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    p.setData(org.papiricoh.supernaturalcraft.registry.AllAttachments.PURGATORY, PurgatoryStanding.EMPTY);
                    EveGladeWorld.cancel();
                    PurgatoryWorld.clearPending();
                    PurgatorySavedData.get(c.getSource().getServer()).reset();
                    c.getSource().sendSuccess(() -> Component.literal("Purgatory forgets you (and where the glade is)."), true);
                    return 1;
                }));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> alpha() {
        return Commands.literal("alpha").then(Commands.literal("spawn")
                .then(Commands.argument("kind", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"vampire", "werewolf", "shapeshifter"}, b))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            PactRules.Alpha a;
                            try {
                                a = PactRules.Alpha.valueOf(StringArgumentType.getString(c, "kind").toUpperCase(Locale.ROOT));
                            } catch (IllegalArgumentException e) {
                                c.getSource().sendFailure(Component.literal("vampire, werewolf or shapeshifter"));
                                return 0;
                            }
                            Vec3 at = AlphaSpawns.ground(p.serverLevel(), p.position().add(p.getLookAngle().multiply(8, 0, 8)));
                            AlphaSpawns.come(p.serverLevel(), a, at != null ? at : p.position(), p);
                            return 1;
                        })));
    }
}
