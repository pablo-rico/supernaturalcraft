package org.papiricoh.supernaturalcraft.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.eclipse.Eclipses;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;

import java.util.List;
import java.util.Locale;

/**
 * {@code /supernatural boss …} for every boss of the mod:
 * <ul>
 *   <li>{@code summon [lucifer|amara|chorus|uncaged|azazel|lilith|metatron|chuck|war|famine|pestilence|death] [<pos>]}: calls one down at your feet (or at {@code pos})
 *   without its ritual; alone, {@code summon} still calls Lucifer. Amara brings her eclipse if none hangs
 *   in the sky; the Chorus its storm.</li>
 *   <li>{@code phase <2-6>}: every boss within 96 blocks begins that phase (capped at its last).</li>
 *   <li>{@code health <fraction>}: sets their health to that share of the maximum.</li>
 * </ul>
 */
final class BossCommands {

    enum Boss {
        LUCIFER("Lucifer rises from the Cage."),
        AMARA("Amara rises out of the eclipse."),
        CHORUS("The Broken Chorus descends."),
        UNCAGED("Lucifer walks free of the Cage."),
        AZAZEL("Yellow smoke gathers into a man."),
        LILITH("A white flash, and she is there."),
        METATRON("A shaft of light, and the Scribe stands in it."),
        CHUCK("Somewhere, a typewriter bell rings."),
        WAR("Hoofbeats, and a man in a red suit."),
        FAMINE("A creak of wheels, and someone hungry."),
        PESTILENCE("A cough, somewhere close."),
        DEATH("A cane taps on stone.");

        final String risen;

        Boss(String risen) {
            this.risen = risen;
        }

        String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private static final double RANGE = 96;

    private BossCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> boss() {
        LiteralArgumentBuilder<CommandSourceStack> summon = Commands.literal("summon")
                .executes(ctx -> summon(ctx, Boss.LUCIFER, null));
        for (Boss boss : Boss.values()) {
            summon.then(Commands.literal(boss.id())
                    .executes(ctx -> summon(ctx, boss, null))
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                            .executes(ctx -> summon(ctx, boss, BlockPosArgument.getLoadedBlockPos(ctx, "pos")))));
        }
        return Commands.literal("boss")
                .then(summon)
                .then(Commands.literal("phase").then(Commands.argument("phase", IntegerArgumentType.integer(2, 6)).executes(ctx -> {
                    int n = IntegerArgumentType.getInteger(ctx, "phase");
                    int count = 0;
                    for (Entity e : bosses(ctx.getSource())) {
                        if (e instanceof LuciferEntity l) l.beginTransition(Math.min(n, l.maxPhase()));
                        else if (e instanceof AmaraEntity a) a.beginTransition(Math.min(n, 4));
                        else if (e instanceof ChorusEntity c) c.beginTransition(Math.min(n, 4));
                        count++;
                    }
                    return report(ctx, count);
                })))
                .then(Commands.literal("health").then(Commands.argument("fraction", FloatArgumentType.floatArg(0.01f, 1f)).executes(ctx -> {
                    float f = FloatArgumentType.getFloat(ctx, "fraction");
                    int count = 0;
                    for (Entity e : bosses(ctx.getSource())) {
                        if (e instanceof ChorusEntity c) c.scaleHealth(f);
                        else if (e instanceof net.minecraft.world.entity.LivingEntity l) l.setHealth(l.getMaxHealth() * f);
                        count++;
                    }
                    return report(ctx, count);
                })));
    }

    private static int summon(CommandContext<CommandSourceStack> ctx, Boss boss, @Nullable BlockPos at) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        @Nullable ServerPlayer player = source.getPlayer();
        BlockPos here = at != null ? at : BlockPos.containing(source.getPosition());
        boolean ok = switch (boss) {
            // Lucifer's altar sits on the floor; the others are centred where you stand.
            case LUCIFER -> LuciferSummoning.summon(level, at != null ? at : here.below(), player);
            case AMARA -> {
                if (!Eclipses.active(level)) Eclipses.begin(level, here, Eclipses.defaultTicks());
                yield AmaraSummoning.summon(level, here, player);
            }
            case CHORUS -> ChorusSummoning.summon(level, here, player);
            case UNCAGED -> org.papiricoh.supernaturalcraft.entity.boss.uncaged.UncagedSummoning.summon(level,
                    at != null ? at : here, player);
            case AZAZEL -> org.papiricoh.supernaturalcraft.entity.boss.azazel.AzazelSummoning.summon(level, at != null ? at : here, player);
            case LILITH -> org.papiricoh.supernaturalcraft.entity.boss.lilith.LilithSummoning.summon(level, at != null ? at : here, player);
            case METATRON -> org.papiricoh.supernaturalcraft.entity.boss.metatron.MetatronSummoning.summon(level, at != null ? at : here, player);
            case CHUCK -> org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckSummoning.summon(level, at != null ? at : here, player, false);
            case WAR, FAMINE, PESTILENCE, DEATH -> org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemenSummoning.summon(
                    org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemanKind.valueOf(boss.name()), level, at != null ? at : here, player) != null;
        };
        if (ok) source.sendSuccess(() -> Component.literal(boss.risen), true);
        else source.sendFailure(Component.literal("Another fight already holds this world (try /supernatural arena restore)."));
        return ok ? 1 : 0;
    }

    static List<Entity> bosses(CommandSourceStack source) {
        return source.getLevel().getEntities((Entity) null, new AABB(source.getPosition(), source.getPosition()).inflate(RANGE),
                e -> e instanceof LuciferEntity || e instanceof AmaraEntity || e instanceof ChorusEntity);
    }

    private static int report(CommandContext<CommandSourceStack> ctx, int count) {
        if (count == 0) ctx.getSource().sendFailure(Component.literal("No boss within " + (int) RANGE + " blocks."));
        return count;
    }
}
