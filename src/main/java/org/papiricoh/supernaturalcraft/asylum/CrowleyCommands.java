package org.papiricoh.supernaturalcraft.asylum;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.supernaturalcraft.asylum.layout.AsylumAnchors;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleyCompanions;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleySavedData;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleySummoning;

/**
 * {@code /supernatural crowley …} (v0.35): {@code place} (raise the asylum now and go there), {@code fight} (start his fight in
 * the asylum as if the caller walked in with the case), {@code state}, {@code reset} (his fate undecided, no companion),
 * {@code companion summon|dismiss} (recruit him for the caller / send him off).
 */
public final class CrowleyCommands {

    private CrowleyCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> crowley() {
        return Commands.literal("crowley")
                .then(Commands.literal("place").executes(c -> {
                    ServerLevel overworld = c.getSource().getServer().overworld();
                    AsylumWorld.buildNow(overworld);
                    AsylumFight f = AsylumWorld.fight(c.getSource().getServer());
                    BlockPos entry = f != null ? f.point(AsylumAnchors.ENTRY) : null;
                    if (entry != null && c.getSource().getEntity() instanceof ServerPlayer p) {
                        p.teleportTo(overworld, entry.getX() + 0.5, entry.getY(), entry.getZ() + 0.5, p.getYRot(), p.getXRot());
                    }
                    c.getSource().sendSuccess(() -> Component.literal("Asylum at " + (entry != null ? entry.toShortString() : "?")), true);
                    return 1;
                }))
                .then(Commands.literal("fight").executes(c -> {
                    AsylumFight f = AsylumWorld.fight(c.getSource().getServer());
                    if (f == null) {
                        c.getSource().sendFailure(Component.literal("No asylum yet: /supernatural crowley place"));
                        return 0;
                    }
                    ServerPlayer p = c.getSource().getPlayer();
                    boolean ok = CrowleySummoning.begin(c.getSource().getServer().overworld(), p, f, false) != null;
                    if (!ok) c.getSource().sendFailure(Component.literal("Another fight already holds this world."));
                    return ok ? 1 : 0;
                }))
                .then(Commands.literal("state").executes(c -> {
                    CrowleySavedData d = CrowleySavedData.get(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.literal("Crowley: " + d.state() + ", owner " + d.owner() + ", seat " + d.seat()
                            + ", asylum " + d.asylum() + (d.asylumBuilt() ? " (built)" : "") + ", rematches " + d.rematches()), false);
                    return 1;
                }))
                .then(Commands.literal("reset").executes(c -> {
                    CrowleyCompanions.dismiss(c.getSource().getServer());
                    CrowleySavedData.get(c.getSource().getServer()).reset();
                    c.getSource().sendSuccess(() -> Component.literal("Crowley's fate is undecided again."), true);
                    return 1;
                }))
                .then(Commands.literal("companion")
                        .then(Commands.literal("summon").executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            CrowleySavedData d = CrowleySavedData.get(c.getSource().getServer());
                            CrowleyCompanions.dismiss(c.getSource().getServer());
                            d.decide(CrowleySavedData.State.RECRUITED, p.getUUID());
                            CrowleyCompanions.arrive(p.serverLevel(), CrowleyCompanions.besideOf(p), d);
                            return 1;
                        }))
                        .then(Commands.literal("dismiss").executes(c -> {
                            CrowleyCompanions.dismiss(c.getSource().getServer());
                            return 1;
                        })));
    }
}
