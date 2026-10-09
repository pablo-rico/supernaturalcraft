package org.papiricoh.supernaturalcraft.legacy;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.entity.legacy.HenryEntity;
import org.papiricoh.supernaturalcraft.legacy.bunker.BunkerBuilder;
import org.papiricoh.supernaturalcraft.legacy.bunker.BunkerLocator;
import org.papiricoh.supernaturalcraft.legacy.bunker.BunkerSavedData;
import org.papiricoh.supernaturalcraft.legacy.bunker.BunkerWorld;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseFile;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseOffice;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseSavedData;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseSites;
import org.papiricoh.supernaturalcraft.legacy.research.ResearchCommands;

/**
 * {@code /supernatural legacy …} (v0.17), for tests: {@code rank <0-5>} (with the rank's advancements and gear), {@code reset},
 * {@code join} (as if Henry's offer were accepted), {@code henry} (he calls now), {@code case new|go|solve|lose},
 * {@code bunker locate|tp|place}, and {@code research …} ({@link ResearchCommands}).
 */
public final class LegacyCommands {

    private LegacyCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> legacy() {
        return Commands.literal("legacy")
                .then(Commands.literal("rank").then(Commands.argument("rank", IntegerArgumentType.integer(0, LegacyRules.MAX_RANK))
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            int rank = IntegerArgumentType.getInteger(ctx, "rank");
                            int before = Legacies.rank(p);
                            Legacies.update(p, l -> l.withRank(rank).withHenry(rank > 0 ? Legacy.HENRY_JOINED : l.henry(), rank > 0 ? -1 : l.henryDay()));
                            for (int r = Math.max(1, before + 1); r <= rank; r++) LegacyOrder.promoted(p, r);
                            ctx.getSource().sendSuccess(() -> Component.literal("Legacy rank " + rank + " (" + LegacyRules.title(rank) + ")."), true);
                            return rank;
                        })))
                .then(Commands.literal("reset").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Legacies.set(p, Legacy.NONE);
                    Legacies.setArchive(p, Archive.EMPTY);
                    ctx.getSource().sendSuccess(() -> Component.literal("Legacy and archive cleared."), true);
                    return 1;
                }))
                .then(Commands.literal("join").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    boolean joined = LegacyOrder.join(p);
                    ctx.getSource().sendSuccess(() -> Component.literal(joined ? "Joined the Men of Letters." : "Already a member."), true);
                    return joined ? 1 : 0;
                }))
                .then(Commands.literal("henry").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    HenryEntity h = HenryEntity.visit(p);
                    return h == null ? 0 : 1;
                }))
                .then(Commands.literal("case")
                        .then(Commands.literal("new").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            CaseFile c = CaseOffice.issue(p, p.blockPosition());
                            if (c == null) {
                                ctx.getSource().sendFailure(Component.literal("Not a member, or a case is already open."));
                                return 0;
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal("Case " + c.index() + ": " + c.monster() + " at " + c.scenario()
                                    + " (" + c.site().getX() + ", " + c.site().getZ() + "), twist '" + c.twist() + "', tier " + c.tier()), true);
                            return 1;
                        }))
                        .then(Commands.literal("go").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            CaseFile c = CaseOffice.open(Legacies.get(p));
                            if (c == null) return 0;
                            ServerLevel level = p.serverLevel();
                            level.getChunk(c.site().getX() >> 4, c.site().getZ() >> 4);
                            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.site().getX() + 16, c.site().getZ());
                            p.teleportTo(level, c.site().getX() + 16.5, y, c.site().getZ() + 0.5, p.getYRot(), p.getXRot());
                            return 1;
                        }))
                        .then(Commands.literal("solve").executes(ctx -> end(ctx, CaseFile.SOLVED)))
                        .then(Commands.literal("lose").executes(ctx -> end(ctx, CaseFile.LOST))))
                .then(Commands.literal("bunker")
                        .then(Commands.literal("locate").executes(LegacyCommands::locate))
                        .then(Commands.literal("tp").executes(LegacyCommands::tp))
                        .then(Commands.literal("place").executes(LegacyCommands::place)))
                .then(ResearchCommands.research());
    }

    private static int end(CommandContext<CommandSourceStack> ctx, int outcome) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        CaseFile c = CaseOffice.open(Legacies.get(p));
        if (c == null) return 0;
        ServerLevel level = p.server.overworld();
        CaseSavedData.Site site = CaseSavedData.get(level).get(p.getUUID(), c.index());
        if (site == null) site = CaseSites.materialize(level, p, c);
        if (site != null) CaseSites.end(level, site, outcome);
        return 1;
    }

    private static int locate(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getServer().overworld();
        BunkerLocator.Site site = BunkerLocator.of(level);
        BlockPos d = BunkerWorld.door(site);
        boolean built = BunkerSavedData.get(level).built();
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.supernaturalcraft.legacy.bunker", d.getX(), d.getY(), d.getZ(),
                (int) Math.round(Math.hypot(d.getX(), d.getZ())), Component.translatable(built ? "commands.supernaturalcraft.legacy.built"
                        : "commands.supernaturalcraft.legacy.unbuilt")), false);
        return 1;
    }

    private static int tp(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p == null) return 0;
        ServerLevel level = ctx.getSource().getServer().overworld();
        BlockPos out = BunkerWorld.outside(BunkerLocator.of(level));
        level.getChunk(out);
        int y = Math.max(out.getY(), level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, out.getX(), out.getZ()));
        p.teleportTo(level, out.getX() + 0.5, y, out.getZ() + 0.5, p.getYRot(), p.getXRot());
        return 1;
    }

    private static int place(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos at = BlockPos.containing(ctx.getSource().getPosition());
        int turn = ctx.getSource().getPlayer() == null ? 0 : turnFacing(ctx.getSource().getPlayer().getYRot());
        BlockPos origin = BunkerBuilder.originOn(level, at.getX(), at.getZ());
        BunkerBuilder.placeAt(level, origin, turn);
        if (level.dimension() == Level.OVERWORLD) BunkerSavedData.get(level).setBunker(origin, turn, true);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.supernaturalcraft.legacy.placed", origin.toShortString()), true);
        return 1;
    }

    /** The turn whose hut door looks back at someone facing {@code yaw}. */
    static int turnFacing(float yaw) {
        int dir = Math.floorMod(Math.round(yaw / 90f), 4);
        return (dir + 2) % 4;
    }
}
