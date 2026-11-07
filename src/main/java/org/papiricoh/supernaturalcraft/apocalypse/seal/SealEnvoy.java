package org.papiricoh.supernaturalcraft.apocalypse.seal;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceDialogue;
import org.papiricoh.supernaturalcraft.allegiance.MessengerSchedule;
import org.papiricoh.supernaturalcraft.allegiance.power.Crossings;
import org.papiricoh.supernaturalcraft.apocalypse.seal.garrison.GarrisonAnchors;
import org.papiricoh.supernaturalcraft.apocalypse.seal.garrison.GarrisonSite;
import org.papiricoh.supernaturalcraft.apocalypse.seal.garrison.GarrisonWorld;
import org.papiricoh.supernaturalcraft.entity.boss.uriel.UrielSummoning;
import org.papiricoh.supernaturalcraft.util.WorldClock;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Castiel's warning (v0.40): a hunter who has held {@link SealRules#URIEL_AFTER} angel seals is visited by Castiel at dawn (once a
 * day until they hear him out), who tells of the traitor among the Host and where he waits ({@link CastielEnvoyEntity}); heard out,
 * the garrison is raised ({@link GarrisonWorld#ensure}) and a compass points at it. Then, by night, the garrison wakes Uriel
 * ({@link UrielSummoning#tick}).
 */
public final class SealEnvoy {

    /** How often (ticks) the visits are considered. */
    public static final int EVERY = 100;
    /** The day each hunter was last visited (a refusal waits a day). */
    private static final Map<UUID, Long> VISITED = new ConcurrentHashMap<>();
    private static boolean registered;

    private SealEnvoy() {
    }

    /** Hooks Castiel's talk into the shared dialogue (once). */
    public static synchronized void register() {
        if (registered) return;
        registered = true;
        AllegianceDialogue.register(CastielEnvoyEntity.DIALOGUE, SealEnvoy::answer);
    }

    static void forget(UUID id) {
        VISITED.remove(id);
    }

    /** Whether Castiel has come for {@code r}'s hunter's warning: two angel seals held, not yet heard out. */
    public static boolean due(SealRecord r) {
        return r.angelsDefended() >= SealRules.URIEL_AFTER && r.envoy() < 2;
    }

    static void tick(ServerLevel overworld) {
        UrielSummoning.tick(overworld);
        long clock = WorldClock.day(overworld.getServer());
        long day = SealRules.dayOf(clock), time = Math.floorMod(clock, SealRules.DAY);
        if (!MessengerSchedule.isDawn(time)) return;
        for (ServerPlayer p : overworld.players()) {
            if (!p.isAlive() || p.isSpectator() || Crossings.inBattle(p) || UrielSummoning.beaten(p)) continue;
            if (!due(SealWorld.record(p)) || Long.valueOf(day).equals(VISITED.get(p.getUUID())) || CastielEnvoyEntity.visiting(p)) continue;
            VISITED.put(p.getUUID(), day);
            CastielEnvoyEntity.visit(p);
        }
    }

    /** An answer to Castiel (public for tests: fake players answer by hand). */
    public static void answer(ServerPlayer p, @Nullable Entity speaker, String node, String option) {
        if (!(speaker instanceof CastielEnvoyEntity cas) || !p.getUUID().equals(cas.hunter()) || cas.leaving()) return;
        switch (node) {
            case "warn" -> {
                SealWorld.setRecord(p, SealWorld.record(p).withEnvoy(1));
                if ("listen".equals(option)) cas.say(p, CastielEnvoyEntity.TRAITOR);
                else {
                    cas.triggerAnim("action", "turn_away");
                    cas.leave(24);
                }
            }
            case "traitor" -> {
                hearOut(p);
                cas.triggerAnim("action", "nod");
                cas.say(p, CastielEnvoyEntity.GARRISON);
            }
            default -> cas.leave(16);
        }
    }

    /** {@code p} has heard Castiel out: the garrison is raised and a compass points at it. */
    public static void hearOut(ServerPlayer p) {
        SealWorld.setRecord(p, SealWorld.record(p).withEnvoy(2));
        ServerLevel overworld = p.server.overworld();
        GarrisonWorld.ensure(overworld);
        GarrisonSite site = GarrisonWorld.site(p.server);
        if (site == null) return;
        BlockPos at = site.point(GarrisonAnchors.URIEL);
        if (at == null) at = site.origin();
        ItemStack compass = new ItemStack(Items.COMPASS);
        compass.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(Level.OVERWORLD, at)), false));
        compass.set(DataComponents.CUSTOM_NAME, Component.translatable("item.supernaturalcraft.garrison_compass").withStyle(ChatFormatting.GOLD));
        SealSpoils.give(p, compass);
        p.sendSystemMessage(Component.translatable("message.supernaturalcraft.seal.garrison", at.getX(), at.getZ()).withStyle(ChatFormatting.GOLD));
    }
}
