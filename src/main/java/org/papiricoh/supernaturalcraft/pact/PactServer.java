package org.papiricoh.supernaturalcraft.pact;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceDialogue;
import org.papiricoh.supernaturalcraft.pact.layout.LabAnchors;
import org.papiricoh.supernaturalcraft.prophecy.Prophecies;

import java.util.Optional;

/**
 * Season 6 on the server (v0.39): what Castiel's and Crowley's answers do ({@link PactDialogue.Act}), and how Crowley takes a
 * crossroads call meant for a crossroads demon when the hunter is the one Castiel sent him.
 */
public final class PactServer {

    private PactServer() {
    }

    /** Called once at start-up: the two story talks come back through the shared dialogue. */
    public static void registerDialogues() {
        AllegianceDialogue.register(PactDialogue.CASTIEL, (player, speaker, node, option) -> {
            if (speaker instanceof CastielEntity c && player.getUUID().equals(c.hunter()) && !c.leaving()) castiel(player, c, node, option);
        });
        AllegianceDialogue.register(PactDialogue.CROWLEY, (player, speaker, node, option) -> {
            if (speaker instanceof CrowleyKingEntity c && player.getUUID().equals(c.hunter()) && !c.leaving()) crowley(player, c, node, option);
        });
    }

    static long today(ServerPlayer p) {
        return Prophecies.today(p.server);
    }

    /** Castiel's answer handler (public for tests: fake players answer by hand). */
    public static void castiel(ServerPlayer p, CastielEntity cas, String node, String option) {
        PactDialogue.Turn t = PactDialogue.castiel(node, option);
        switch (t.act()) {
            case CASTIEL_HELP -> {
                Pacts.advance(p, PactRules.Step.CALLED);
                Pacts.update(p, q -> q.withVisit(today(p)));
                tell(p, "message.supernaturalcraft.pact.sent_to_crowley", ChatFormatting.GOLD);
                cas.triggerAnim("action", "nod");
                cas.leave(20);
                return;
            }
            case CASTIEL_REFUSE -> {
                Pacts.update(p, q -> q.withVisit(today(p)));
                cas.triggerAnim("action", "turn_away");
                cas.leave(30);
                return;
            }
            case SCENE_DONE -> Pacts.update(p, q -> q.withSeen("doubt".equals(node) ? PactDialogue.SCENE_DOUBT : PactDialogue.SCENE_READY));
            default -> {
            }
        }
        if (t.next() != null) cas.say(p, t.next());
        else cas.leave(16);
    }

    /** Crowley's answer handler (public for tests). */
    public static void crowley(ServerPlayer p, CrowleyKingEntity crowley, String node, String option) {
        PactDialogue.Turn t = PactDialogue.crowley(node, option);
        switch (t.act()) {
            case CROWLEY_DEAL -> {
                Pacts.advance(p, PactRules.Step.MET_CROWLEY);
                Pacts.advance(p, PactRules.Step.HUNTING);
                tell(p, "message.supernaturalcraft.pact.hunting", ChatFormatting.DARK_RED);
                crowley.triggerAnim("action", "snap");
                crowley.leave(14);
                return;
            }
            case CROWLEY_LEAVE -> {
                Pacts.update(p, q -> q.withVisit(today(p)));
                crowley.triggerAnim("action", "dismiss");
                crowley.leave(20);
                return;
            }
            case CROWLEY_LAB -> {
                handOverLab(p);
                crowley.triggerAnim("action", "point");
                crowley.leave(24);
                return;
            }
            default -> {
            }
        }
        if (t.next() != null) crowley.say(p, t.next());
        else {
            crowley.triggerAnim("action", "dismiss");
            crowley.leave(20);
        }
    }

    /** Crowley gives {@code p} his lab: it is raised (if it is not yet), and a lodestone compass points at it. */
    public static void handOverLab(ServerPlayer p) {
        Pacts.update(p, q -> q.withSeen(PactDialogue.SCENE_LAB).withVisit(today(p)));
        ServerLevel overworld = p.server.overworld();
        PactWorld.ensure(overworld);
        LabSite site = PactWorld.site(p.server);
        if (site == null) return;
        BlockPos rite = site.point(LabAnchors.RITE);
        if (rite == null) rite = site.origin();
        ItemStack compass = new ItemStack(Items.COMPASS);
        compass.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(Level.OVERWORLD, rite)), false));
        compass.set(DataComponents.CUSTOM_NAME, Component.translatable("item.supernaturalcraft.crowleys_compass").withStyle(ChatFormatting.DARK_RED));
        if (!p.getInventory().add(compass)) p.drop(compass, false);
        p.sendSystemMessage(Component.translatable("message.supernaturalcraft.pact.lab", rite.getX(), rite.getZ()).withStyle(ChatFormatting.GOLD));
    }

    /**
     * A crossroads call ({@code at}) from {@code summoner}: Crowley takes it if Castiel sent them to him and he has not struck
     * a deal yet. @return whether he came (the crossroads demon does not)
     */
    public static boolean crowleyAnswers(@Nullable Player summoner, ServerLevel level, Vec3 at) {
        if (!(summoner instanceof ServerPlayer p) || level.dimension() != Level.OVERWORLD) return false;
        PactRules.Step step = Pacts.step(p);
        if (step != PactRules.Step.CALLED && step != PactRules.Step.MET_CROWLEY) return false;
        return CrowleyKingEntity.visit(p, at) != null;
    }

    /** All three Alphas are down: Crowley comes at once to hand over the lab. */
    static void riteReady(ServerPlayer p) {
        if (p.level().dimension() == Level.OVERWORLD && !CrowleyKingEntity.visiting(p)) CrowleyKingEntity.visit(p, null);
    }

    static void tell(ServerPlayer p, String key, ChatFormatting style) {
        p.sendSystemMessage(Component.translatable(key).withStyle(style));
    }

    /** Whether {@code e} is one of season 6's story visitors. */
    public static boolean storyNpc(Entity e) {
        return e instanceof StoryNpcEntity;
    }
}
