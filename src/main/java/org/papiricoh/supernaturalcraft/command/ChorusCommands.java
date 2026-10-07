package org.papiricoh.supernaturalcraft.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.papiricoh.supernaturalcraft.chorus.ChoirAltarBlockEntity;
import org.papiricoh.supernaturalcraft.chorus.Melody;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusEntity;

import java.util.List;

/**
 * {@code /supernatural chorus …}: melody get|set &lt;pos&gt; [a b c] (notes 0-6, red to purple) |
 * kneel (summoning and phases: {@code /supernatural boss}).
 */
final class ChorusCommands {

    private ChorusCommands() {
    }

    /** {@code /supernatural spire place [seed]}: builds a Hymnal Spire on the highest point near you. */
    static LiteralArgumentBuilder<CommandSourceStack> spire() {
        return Commands.literal("spire").then(Commands.literal("place")
                .executes(ctx -> placeSpire(ctx, ctx.getSource().getLevel().getRandom().nextLong()))
                .then(Commands.argument("seed", com.mojang.brigadier.arguments.LongArgumentType.longArg())
                        .executes(ctx -> placeSpire(ctx, com.mojang.brigadier.arguments.LongArgumentType.getLong(ctx, "seed")))));
    }

    private static int placeSpire(CommandContext<CommandSourceStack> ctx, long seed) {
        var plan = org.papiricoh.supernaturalcraft.structure.HymnalSpire.placeDirect(ctx.getSource().getLevel(),
                BlockPos.containing(ctx.getSource().getPosition()), seed);
        ctx.getSource().sendSuccess(() -> Component.literal("A Hymnal Spire rises; its altar is at " + plan.altar().toShortString() + ", its hymn ")
                .append(Melody.describe(plan.melody())), true);
        return 1;
    }

    static LiteralArgumentBuilder<CommandSourceStack> chorus() {
        return Commands.literal("chorus")
                .then(Commands.literal("melody").then(Commands.argument("altar", BlockPosArgument.blockPos())
                        .then(Commands.literal("get").executes(ctx -> {
                            ChoirAltarBlockEntity altar = altar(ctx);
                            if (altar == null) return 0;
                            altar.ensureMelody();
                            ctx.getSource().sendSuccess(() -> Component.translatable("message.supernaturalcraft.choir_altar.hymn",
                                    Melody.describe(altar.melody())), false);
                            return 1;
                        }))
                        .then(Commands.literal("set")
                                .then(Commands.argument("a", IntegerArgumentType.integer(0, 6))
                                        .then(Commands.argument("b", IntegerArgumentType.integer(0, 6))
                                                .then(Commands.argument("c", IntegerArgumentType.integer(0, 6)).executes(ctx -> {
                                                    ChoirAltarBlockEntity altar = altar(ctx);
                                                    if (altar == null) return 0;
                                                    altar.setMelody(new byte[]{(byte) IntegerArgumentType.getInteger(ctx, "a"),
                                                            (byte) IntegerArgumentType.getInteger(ctx, "b"), (byte) IntegerArgumentType.getInteger(ctx, "c")});
                                                    ctx.getSource().sendSuccess(() -> Component.translatable("message.supernaturalcraft.choir_altar.tuned",
                                                            Melody.describe(altar.melody())), true);
                                                    return 1;
                                                })))))))
                .then(Commands.literal("kneel").executes(ctx -> {
                    List<ChorusEntity> all = choruses(ctx.getSource());
                    all.forEach(c -> c.kneel(ChorusEntity.KNEEL_TICKS));
                    return all.size();
                }));
    }

    private static ChoirAltarBlockEntity altar(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "altar");
        if (ctx.getSource().getLevel().getBlockEntity(pos) instanceof ChoirAltarBlockEntity a) return a;
        ctx.getSource().sendFailure(Component.literal("No Choir Altar at " + pos.toShortString()));
        return null;
    }

    private static List<ChorusEntity> choruses(CommandSourceStack source) {
        return source.getLevel().getEntitiesOfClass(ChorusEntity.class,
                new net.minecraft.world.phys.AABB(source.getPosition(), source.getPosition()).inflate(96));
    }
}
