package org.papiricoh.supernaturalcraft.author;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * {@code /supernatural author cabin locate|tp|place}: where the Author's cabin is ({@code /locate} cannot find a
 * structure with its own placement), going there, or building one where you stand (and making it his).
 * {@code /supernatural author spell} casts "Find the Author" without a bowl; {@code author reset} forgets the spell,
 * who met him and who was rewarded.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class AuthorCommands {

    private AuthorCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("supernatural").requires(s -> s.hasPermission(2)).then(author()));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> author() {
        return Commands.literal("author")
                .then(Commands.literal("cabin")
                        .then(Commands.literal("locate").executes(AuthorCommands::locate))
                        .then(Commands.literal("tp").executes(AuthorCommands::tp))
                        .then(Commands.literal("place").executes(AuthorCommands::place)))
                .then(Commands.literal("spell").executes(AuthorCommands::spell))
                .then(Commands.literal("reset").executes(AuthorCommands::reset));
    }

    private static ServerLevel overworld(CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getServer().overworld();
    }

    private static int locate(CommandContext<CommandSourceStack> ctx) {
        AuthorSite.Site site = AuthorSite.of(overworld(ctx));
        BlockPos c = AuthorWorld.centre(site);
        boolean built = AuthorSavedData.get(overworld(ctx)).built();
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.supernaturalcraft.author.cabin", c.getX(), c.getY(), c.getZ(),
                (int) Math.round(Math.hypot(c.getX(), c.getZ())), Component.translatable(built ? "commands.supernaturalcraft.author.built"
                        : "commands.supernaturalcraft.author.unbuilt")), false);
        return 1;
    }

    private static int tp(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p == null) return 0;
        ServerLevel level = overworld(ctx);
        AuthorSite.Site site = AuthorSite.of(level);
        BlockPos door = CabinBuilder.at(site.origin(), site.rotation(), new int[]{0, 1, 10});
        level.getChunk(door);
        p.teleportTo(level, door.getX() + 0.5, Math.max(door.getY(), level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                door.getX(), door.getZ())), door.getZ() + 0.5, p.getYRot(), p.getXRot());
        return 1;
    }

    private static int place(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos at = BlockPos.containing(ctx.getSource().getPosition());
        int turn = ctx.getSource().getPlayer() == null ? 0 : turnFacing(ctx.getSource().getPlayer().getYRot());
        BlockPos origin = CabinBuilder.originOn(level, at.getX(), at.getZ());
        CabinBuilder.placeAt(level, origin, turn);
        if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) AuthorSavedData.get(level).setCabin(origin, turn, true);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.supernaturalcraft.author.placed", origin.toShortString()), true);
        return 1;
    }

    /** The turn whose door looks back at someone facing {@code yaw}. */
    static int turnFacing(float yaw) {
        // Door looks south at turn 0, west at 1, north at 2, east at 3; the player looks the other way.
        int dir = Math.floorMod(Math.round(yaw / 90f), 4); // 0 south, 1 west, 2 north, 3 east
        return (dir + 2) % 4;
    }

    private static int spell(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p == null) return 0;
        return FindTheAuthorEffect.cast(overworld(ctx), p, p.position()) ? 1 : 0;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) {
        AuthorSavedData data = AuthorSavedData.get(overworld(ctx));
        BlockPos cabin = data.cabin();
        int turn = data.rotation();
        boolean built = data.built();
        data.restore(new net.minecraft.nbt.CompoundTag());
        if (cabin != null) data.setCabin(cabin, turn, built);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.supernaturalcraft.author.reset"), true);
        return 1;
    }
}
