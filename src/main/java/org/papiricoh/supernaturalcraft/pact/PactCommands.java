package org.papiricoh.supernaturalcraft.pact;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.supernaturalcraft.entity.boss.godstiel.DoorScene;
import org.papiricoh.supernaturalcraft.entity.boss.godstiel.GodstielSummoning;
import org.papiricoh.supernaturalcraft.pact.layout.LabAnchors;

/**
 * {@code /supernatural pact …} (v0.39): {@code state} (the caller's step, Alphas and the lab), {@code step <name>} (set the
 * caller's step; earlier advancements are not taken back), {@code alpha <vampire|werewolf|shapeshifter>} (count one down),
 * {@code reset} (the caller's pact and the world's lab forgotten), {@code castiel} and {@code crowley} (call them now),
 * {@code lab} (raise the lab now and go there), {@code door} (start the door's scene at the lab, whatever the step).
 */
public final class PactCommands {

    private PactCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> pact() {
        LiteralArgumentBuilder<CommandSourceStack> step = Commands.literal("step");
        for (PactRules.Step s : PactRules.Step.values()) {
            step.then(Commands.literal(s.name().toLowerCase(java.util.Locale.ROOT)).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                Pacts.update(p, q -> q.withStep(s));
                Pacts.advance(p, s);
                c.getSource().sendSuccess(() -> Component.literal("Pact step: " + s), true);
                return 1;
            }));
        }
        LiteralArgumentBuilder<CommandSourceStack> alpha = Commands.literal("alpha");
        for (PactRules.Alpha a : PactRules.Alpha.values()) {
            alpha.then(Commands.literal(a.name().toLowerCase(java.util.Locale.ROOT)).executes(c -> {
                Pacts.alphaSlain(c.getSource().getPlayerOrException(), a);
                return 1;
            }));
        }
        return Commands.literal("pact")
                .then(Commands.literal("state").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Pact pact = Pacts.get(p);
                    PactSavedData d = PactSavedData.get(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.literal("Pact: " + pact.stage() + ", alphas " + PactRules.count(pact.alphas())
                            + "/3, scenes " + pact.scenes() + ", last visit " + pact.lastVisit() + ", grace " + pact.grace()
                            + "; lab " + d.lab() + (d.labBuilt() ? " (built)" : "") + ", rematches " + d.rematches()), false);
                    return 1;
                }))
                .then(step)
                .then(alpha)
                .then(Commands.literal("reset").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Pacts.set(p, Pact.EMPTY);
                    PactSavedData.get(c.getSource().getServer()).reset();
                    DoorScene.reset();
                    c.getSource().sendSuccess(() -> Component.literal("Pact and lab forgotten."), true);
                    return 1;
                }))
                .then(Commands.literal("castiel").executes(c -> CastielEntity.visit(c.getSource().getPlayerOrException()) != null ? 1 : 0))
                .then(Commands.literal("crowley").executes(c -> CrowleyKingEntity.visit(c.getSource().getPlayerOrException(), null) != null ? 1 : 0))
                .then(Commands.literal("lab").executes(c -> {
                    ServerLevel overworld = c.getSource().getServer().overworld();
                    PactWorld.buildNow(overworld);
                    LabSite site = PactWorld.site(c.getSource().getServer());
                    BlockPos entry = site != null ? site.point(LabAnchors.ENTRY) : null;
                    if (entry != null && c.getSource().getEntity() instanceof ServerPlayer p) {
                        p.teleportTo(overworld, entry.getX() + 0.5, entry.getY(), entry.getZ() + 0.5, p.getYRot(), p.getXRot());
                    }
                    c.getSource().sendSuccess(() -> Component.literal("Lab at " + (entry != null ? entry.toShortString() : "?")), true);
                    return 1;
                }))
                .then(Commands.literal("door").executes(c -> {
                    LabSite site = PactWorld.site(c.getSource().getServer());
                    if (site == null) {
                        c.getSource().sendFailure(Component.literal("No lab yet: /supernatural pact lab"));
                        return 0;
                    }
                    boolean ok = DoorScene.start(c.getSource().getServer().overworld(), site, c.getSource().getPlayer());
                    if (!ok) c.getSource().sendFailure(Component.literal("The door's scene is already playing."));
                    return ok ? 1 : 0;
                }))
                .then(Commands.literal("rematch").executes(c -> {
                    LabSite site = PactWorld.site(c.getSource().getServer());
                    if (site == null) return 0;
                    return GodstielSummoning.begin(c.getSource().getServer().overworld(), c.getSource().getPlayer(), site,
                            site.floor(LabAnchors.HALL), true) != null ? 1 : 0;
                }));
    }
}
