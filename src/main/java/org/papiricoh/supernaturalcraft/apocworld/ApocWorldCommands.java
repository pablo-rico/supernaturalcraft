package org.papiricoh.supernaturalcraft.apocworld;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
import org.papiricoh.supernaturalcraft.apocworld.army.AuArmy;
import org.papiricoh.supernaturalcraft.apocworld.camp.CampBuilder;
import org.papiricoh.supernaturalcraft.apocworld.fortress.FortressAnchors;
import org.papiricoh.supernaturalcraft.apocworld.fortress.FortressSite;
import org.papiricoh.supernaturalcraft.apocworld.fortress.FortressWorld;
import org.papiricoh.supernaturalcraft.nephilim.Nephilims;

/**
 * {@code /supernatural apocworld …} (v0.41, level 2; debug):
 * <ul>
 *   <li>{@code go}: into the burned world now (the landing), as by a rift; {@code back}: home now;</li>
 *   <li>{@code rift [ticks]}: a rift here (in from our side, home from the burned one);</li>
 *   <li>{@code fortress}: raise Michael's fortress now and stand on its road;</li>
 *   <li>{@code camp}: a prisoner camp or checkpoint (by the position's hash) here; {@code patrol}: a squad sent at the caller;</li>
 *   <li>{@code state}: the caller's standing; {@code reset}: forgotten (never been, no camps, Michael not fled).</li>
 * </ul>
 * Self-registered under the mod's {@code /supernatural} root (Brigadier merges literals of one name).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class ApocWorldCommands {

    private ApocWorldCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("supernatural").requires(s -> s.hasPermission(2)).then(apocworld()));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> apocworld() {
        return Commands.literal("apocworld").requires(s -> s.hasPermission(2))
                .then(Commands.literal("go").executes(c -> {
                    ApocalypseWorld.send(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("back").executes(c -> {
                    ApocalypseWorld.goHome(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("rift").executes(c -> rift(c.getSource(), ApocalypseWorld.RITE_RIFT_TICKS))
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(20)).executes(c ->
                                rift(c.getSource(), IntegerArgumentType.getInteger(c, "ticks")))))
                .then(Commands.literal("fortress").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ServerLevel level = ApocalypseWorld.level(p.server);
                    FortressSite site = FortressWorld.site(p.server);
                    if (site == null || !FortressWorld.built(p.server)) {
                        FortressWorld.ensure(level);
                        site = FortressWorld.site(p.server);
                        if (site != null && !FortressWorld.built(p.server)) site = FortressWorld.placeAt(level, site.origin(), site.q());
                    }
                    if (site == null) return 0;
                    BlockPos entry = site.point(FortressAnchors.ENTRY);
                    if (entry == null) entry = site.origin();
                    ApocalypseWorld.rememberWayBack(p, p.serverLevel(), p.position());
                    p.teleportTo(level, entry.getX() + 0.5, entry.getY(), entry.getZ() + 0.5, p.getYRot(), 0);
                    BlockPos at = site.throne();
                    c.getSource().sendSuccess(() -> Component.literal("Michael's fortress, throne at " + at.toShortString()), true);
                    return 1;
                }))
                .then(Commands.literal("camp").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    BlockPos o = CampBuilder.placeDirect(p.serverLevel(), p.blockPosition().offset(0, 0, 18), p.blockPosition().asLong() * 31 + p.level().getGameTime());
                    c.getSource().sendSuccess(() -> Component.literal("An outpost at " + o.toShortString()), true);
                    return 1;
                }))
                .then(Commands.literal("patrol").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    int n = AuArmy.patrol(p.serverLevel(), p, 3);
                    c.getSource().sendSuccess(() -> Component.literal(n + " soldiers sent"), true);
                    return n;
                }))
                .then(Commands.literal("state").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    AUStanding s = ApocalypseWorld.standing(p);
                    c.getSource().sendSuccess(() -> Component.literal("road " + Nephilims.step(p) + ", visited " + s.visited() + ", camps freed "
                            + s.campsFreed() + ", Michael fled " + s.michaelFled() + ", in the burned world " + ApocalypseWorld.in(p)), false);
                    return 1;
                }))
                .then(Commands.literal("reset").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    p.setData(ApocWorldRegistry.AU_STANDING, AUStanding.EMPTY);
                    ApocWorldSavedData.get(p.server).settle(p.getUUID());
                    c.getSource().sendSuccess(() -> Component.literal("Apocalypse World standing forgotten"), true);
                    return 1;
                }));
    }

    private static int rift(CommandSourceStack source, int ticks) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        BlockPos at = p.blockPosition().relative(p.getDirection(), 4);
        boolean ok = ApocalypseWorld.openRift(p.serverLevel(), at, ticks);
        source.sendSuccess(() -> Component.literal(ok ? "A rift for " + ticks + " ticks" : "No rift"), true);
        return ok ? 1 : 0;
    }
}
