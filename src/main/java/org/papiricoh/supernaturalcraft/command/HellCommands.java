package org.papiricoh.supernaturalcraft.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.supernaturalcraft.hell.HellDimension;
import org.papiricoh.supernaturalcraft.hell.cage.CageBuilder;
import org.papiricoh.supernaturalcraft.hell.cage.CageController;
import org.papiricoh.supernaturalcraft.hell.cage.CageLayout;
import org.papiricoh.supernaturalcraft.hell.rift.HellRift;
import org.papiricoh.supernaturalcraft.hell.rift.HellRifts;

/**
 * {@code /supernatural hell …} and {@code /supernatural cage …}, for testing Hell without its rituals:
 * <ul>
 *   <li>{@code hell tp [x z]}: to Hell, landing near (x, z) (default: your position scaled as a rift would).</li>
 *   <li>{@code hell return}: back to your bed or the world spawn.</li>
 *   <li>{@code hell rift}: opens an outbound rift in front of you.</li>
 *   <li>{@code cage open|close}: the Cage's iris; {@code cage place} rebuilds the whole structure at 0, 0.</li>
 * </ul>
 */
final class HellCommands {

    private HellCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> hell() {
        return Commands.literal("hell")
                .then(Commands.literal("tp")
                        .executes(ctx -> tp(ctx, null, null))
                        .then(Commands.argument("x", IntegerArgumentType.integer()).then(Commands.argument("z", IntegerArgumentType.integer())
                                .executes(ctx -> tp(ctx, IntegerArgumentType.getInteger(ctx, "x"), IntegerArgumentType.getInteger(ctx, "z"))))))
                .then(Commands.literal("return").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    ServerLevel hell = p.server.getLevel(HellDimension.LEVEL);
                    if (hell == null) return 0;
                    HellRift.Link home = HellRifts.home(hell, p);
                    ServerLevel to = p.server.getLevel(home.dimension());
                    if (to == null) return 0;
                    p.teleportTo(to, home.pos().x, home.pos().y, home.pos().z, p.getYRot(), p.getXRot());
                    return 1;
                }))
                .then(Commands.literal("rift").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    ServerLevel level = p.serverLevel();
                    Direction facing = p.getDirection();
                    BlockPos anchor = p.blockPosition().relative(facing, 4);
                    if (HellDimension.isHell(level)) {
                        HellRifts.open(level, anchor, org.papiricoh.supernaturalcraft.hell.rift.RiftShape.axisFacing(facing),
                                HellRift.Kind.ESCAPE, HellRifts.home(level, p));
                    } else {
                        HellRifts.open(level, anchor, org.papiricoh.supernaturalcraft.hell.rift.RiftShape.axisFacing(facing),
                                HellRift.Kind.OUTBOUND, null);
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal("A rift tears open."), true);
                    return 1;
                }));
    }

    private static int tp(CommandContext<CommandSourceStack> ctx, Integer x, Integer z) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        ServerLevel hell = p.server.getLevel(HellDimension.LEVEL);
        if (hell == null) {
            ctx.getSource().sendFailure(Component.literal("Hell is not loaded on this server."));
            return 0;
        }
        int tx, tz;
        if (x != null) {
            tx = x;
            tz = z;
        } else {
            double scale = net.minecraft.world.level.dimension.DimensionType.getTeleportationScale(p.level().dimensionType(), hell.dimensionType());
            int[] xz = HellRifts.scaledTarget(p.getX(), p.getZ(), scale, hell.getWorldBorder().getSize() / 2);
            tx = xz[0];
            tz = xz[1];
        }
        BlockPos at = HellRifts.findLanding(hell, tx, tz, Direction.Axis.X);
        p.teleportTo(hell, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYRot(), p.getXRot());
        ctx.getSource().sendSuccess(() -> Component.literal("Welcome to Hell (" + at.toShortString() + ")."), true);
        return 1;
    }

    static LiteralArgumentBuilder<CommandSourceStack> cage() {
        return Commands.literal("cage")
                .then(Commands.literal("open").executes(ctx -> cageIris(ctx, true)))
                .then(Commands.literal("close").executes(ctx -> cageIris(ctx, false)))
                .then(Commands.literal("place").executes(ctx -> {
                    ServerLevel level = ctx.getSource().getLevel();
                    CageBuilder.build(level, CageBuilder.extent(), true);
                    CageController.get(level).set(level, false);
                    ctx.getSource().sendSuccess(() -> Component.literal("Lucifer's Cage hangs over " + CageLayout.ALTAR.toShortString() + "."), true);
                    return 1;
                }));
    }

    private static int cageIris(CommandContext<CommandSourceStack> ctx, boolean open) {
        ServerLevel level = ctx.getSource().getLevel();
        CageController cage = CageController.get(level);
        if (open) cage.open(level);
        else cage.close(level);
        ctx.getSource().sendSuccess(() -> Component.literal(open ? "The Cage opens." : "The Cage closes."), true);
        return 1;
    }
}
