package org.papiricoh.supernaturalcraft.cain;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cain.layout.CainFarmLayout;
import org.papiricoh.supernaturalcraft.mark.MarkData;
import org.papiricoh.supernaturalcraft.mark.Marks;

/**
 * {@code /supernatural cain …} and {@code /supernatural mark …} (v0.21): {@code cain locate|tp|place|ensure|address|npc}: where his
 * farm is (hidden from {@code /locate}), going there, building it where you stand (and making it his), making sure it stands,
 * his address, and calling him home now; {@code mark grant|remove|thirst <0-1>|info}: the Mark of Cain on yourself.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class CainCommands {

    private CainCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("supernatural").requires(s -> s.hasPermission(2)).then(cain()).then(mark()));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> cain() {
        return Commands.literal("cain")
                .then(Commands.literal("locate").executes(CainCommands::locate))
                .then(Commands.literal("tp").executes(CainCommands::tp))
                .then(Commands.literal("place").executes(CainCommands::place))
                .then(Commands.literal("ensure").executes(CainCommands::ensure))
                .then(Commands.literal("address").executes(CainCommands::address))
                .then(Commands.literal("npc").executes(CainCommands::npc));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> mark() {
        return Commands.literal("mark")
                .then(Commands.literal("grant").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    boolean ok = Marks.grant(p, null, true);
                    ctx.getSource().sendSuccess(() -> Component.literal(ok ? "The Mark burns into your arm." : "You already bear it."), false);
                    return ok ? 1 : 0;
                }))
                .then(Commands.literal("remove").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    boolean ok = Marks.remove(p);
                    ctx.getSource().sendSuccess(() -> Component.literal(ok ? "The Mark is gone." : "You bear no Mark."), false);
                    return ok ? 1 : 0;
                }))
                .then(Commands.literal("thirst").then(Commands.argument("value", FloatArgumentType.floatArg(0, 1)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Marks.setThirst(p, FloatArgumentType.getFloat(ctx, "value"));
                    ctx.getSource().sendSuccess(() -> Component.literal("Thirst: " + Marks.thirst(p)), false);
                    return 1;
                })))
                .then(Commands.literal("info").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    MarkData m = Marks.get(p);
                    long since = (p.serverLevel().getGameTime() - m.lastKill()) / 20;
                    ctx.getSource().sendSuccess(() -> Component.literal("Marked: " + m.marked() + ", thirst " + m.thirst()
                            + ", last kill " + since + " s ago"), false);
                    return 1;
                }));
    }

    private static ServerLevel overworld(CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getServer().overworld();
    }

    private static int locate(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = overworld(ctx);
        CainSite.Site site = CainSite.of(level);
        BlockPos h = CainWorld.arenaCentre(site);
        CainSavedData data = CainSavedData.get(level);
        ctx.getSource().sendSuccess(() -> Component.literal("Cain's farm: " + h.getX() + " " + h.getY() + " " + h.getZ() + " ("
                + (int) Math.round(Math.hypot(h.getX(), h.getZ())) + " blocks from the origin, " + (data.built() ? "built" : "not yet built")
                + (data.grave() != null ? ", his grave dug" : "") + ")"), false);
        return 1;
    }

    private static int tp(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p == null) return 0;
        ServerLevel level = overworld(ctx);
        BlockPos entry = CainFarm.at(CainSite.of(level), CainFarmLayout.ENTRY);
        level.getChunk(entry);
        p.teleportTo(level, entry.getX() + 0.5, Math.max(entry.getY(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                entry.getX(), entry.getZ())), entry.getZ() + 0.5, p.getYRot(), p.getXRot());
        return 1;
    }

    private static int place(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos at = BlockPos.containing(ctx.getSource().getPosition());
        int turn = ctx.getSource().getPlayer() == null ? 0 : Math.floorMod(Math.round(ctx.getSource().getPlayer().getYRot() / 90f) + 2, 4);
        BlockPos origin = CainFarm.originOn(level, at.getX(), at.getZ());
        CainFarm.placeAt(level, origin, turn);
        if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) CainSavedData.get(level).setFarm(origin, turn, true);
        ctx.getSource().sendSuccess(() -> Component.literal("Cain's farm placed at " + origin.toShortString()), true);
        return 1;
    }

    private static int ensure(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = overworld(ctx);
        BlockPos origin = CainWorld.ensureNow(level, CainSavedData.get(level), CainSite.of(level));
        ctx.getSource().sendSuccess(() -> Component.literal("Cain's farm stands at " + origin.toShortString()), true);
        return 1;
    }

    private static int address(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p == null) return 0;
        p.getInventory().add(CainsAddressItem.pointed(overworld(ctx)));
        return 1;
    }

    private static int npc(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = overworld(ctx);
        CainSavedData data = CainSavedData.get(level);
        if (!data.built()) {
            ctx.getSource().sendFailure(Component.literal("His farm is not built yet (/supernatural cain ensure)."));
            return 0;
        }
        CainNpcEntity npc = CainWorld.spawnNpc(level, CainSite.of(level));
        return npc != null ? 1 : 0;
    }
}
