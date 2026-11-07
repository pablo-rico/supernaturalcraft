package org.papiricoh.supernaturalcraft.nephilim;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.supernaturalcraft.empty.EmptyWorld;
import org.papiricoh.supernaturalcraft.entity.boss.soulless.SoullessJackSummoning;
import org.papiricoh.supernaturalcraft.nephilim.jack.JackCompanions;
import org.papiricoh.supernaturalcraft.nephilim.michael.MichaelEnvoyEntity;
import org.papiricoh.supernaturalcraft.nephilim.michael.MichaelPossession;
import org.papiricoh.supernaturalcraft.nephilim.michael.Mindscape;

import java.util.Locale;

/**
 * {@code /supernatural nephilim …} (v0.41): {@code state}, {@code step <step>}, {@code reset}, {@code jack} (he comes now),
 * {@code offer} (Michael comes now), {@code yes} (worn, then the mindscape), {@code mindscape}, {@code leaves} (Jack trades his
 * soul now), {@code soulless} (he comes now), {@code empty go|home}, {@code migrate}.
 */
public final class NephilimCommands {

    private NephilimCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> nephilim() {
        LiteralArgumentBuilder<CommandSourceStack> step = Commands.literal("step");
        for (NephilimRules.Step s : NephilimRules.Step.values()) {
            step.then(Commands.literal(s.name().toLowerCase(Locale.ROOT)).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                Nephilims.update(p, n -> n.withStep(NephilimRules.Step.NONE).withVisit(-100));
                Nephilims.advance(p, s);
                c.getSource().sendSuccess(() -> Component.literal("Nephilim step: " + s), true);
                return 1;
            }));
        }
        return Commands.literal("nephilim")
                .then(Commands.literal("state").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Nephilim n = Nephilims.get(p);
                    c.getSource().sendSuccess(() -> Component.literal("Nephilim: " + n.stage() + ", last visit " + n.lastVisit() + ", scenes "
                            + Integer.toHexString(n.scenes()) + ", answer " + n.answer() + ", grace " + n.grace() + "; Jack "
                            + (JackCompanions.with(p) != null ? "here" : "away") + ", worn " + MichaelPossession.busy(p)
                            + ", in the Empty " + EmptyWorld.in(p)), false);
                    return 1;
                }))
                .then(step)
                .then(Commands.literal("reset").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    JackCompanions.dismiss(p);
                    MichaelPossession.release(p.getUUID());
                    Nephilims.set(p, Nephilim.EMPTY.withSeen(NephilimMigration.MIGRATED));
                    c.getSource().sendSuccess(() -> Component.literal("Nephilim reset."), true);
                    return 1;
                }))
                .then(Commands.literal("jack").executes(c -> {
                    JackCompanions.arrive(c.getSource().getPlayerOrException(), null);
                    return 1;
                }))
                .then(Commands.literal("offer").executes(c -> MichaelEnvoyEntity.visit(c.getSource().getPlayerOrException()) != null ? 1 : 0))
                .then(Commands.literal("yes").executes(c -> {
                    MichaelPossession.begin(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("mindscape").executes(c -> Mindscape.enter(c.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("leaves").executes(c -> {
                    NephilimEvents.jackLeaves(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("soulless").executes(c -> SoullessJackSummoning.ambush(c.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("empty")
                        .then(Commands.literal("go").executes(c -> {
                            EmptyWorld.send(c.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("home").executes(c -> {
                            EmptyWorld.home(c.getSource().getPlayerOrException());
                            return 1;
                        })))
                .then(Commands.literal("migrate").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Nephilims.update(p, n -> new Nephilim(n.step(), n.lastVisit(), n.scenes() & ~NephilimMigration.MIGRATED, n.answer(), n.grace()));
                    boolean done = NephilimMigration.migrate(p);
                    c.getSource().sendSuccess(() -> Component.literal(done ? "Migrated: the new road is done." : "Nothing to migrate."), true);
                    return 1;
                }));
    }
}
