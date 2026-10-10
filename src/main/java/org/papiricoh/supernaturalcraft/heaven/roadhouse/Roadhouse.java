package org.papiricoh.supernaturalcraft.heaven.roadhouse;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.entity.heaven.AshEntity;
import org.papiricoh.supernaturalcraft.heaven.passage.HeavenPassage;
import org.papiricoh.supernaturalcraft.heaven.passage.HeavenStanding;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlot;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlots;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlotsSavedData;
import org.papiricoh.supernaturalcraft.network.AshChoicePayload;
import org.papiricoh.supernaturalcraft.network.AshMenuPayload;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Harvelle's Roadhouse in Heaven (v0.18): Ash behind its bar, his menu (hints, the Heavens a hunter may visit, their own
 * welcome) and what each choice does. Menu format: {@link AshMenu}.
 */
public final class Roadhouse {

    /** Talks so far and the hints' rotation, per hunter (only for variety: not saved). */
    private static final Map<UUID, Integer> TALKS = new ConcurrentHashMap<>();

    private Roadhouse() {
    }

    /** Ash at his spot behind the bar of {@code hub}, spawned if he is not there. */
    public static @Nullable AshEntity ensureAsh(ServerLevel level, HeavenPlot hub) {
        BlockPos spot = HeavenPlots.at(level, hub, RoadhouseLayout.ASH_SPOT);
        // His chunk's entities are still loading: he may well be there, so never make a second one yet.
        if (!level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(spot))) {
            return hub.ash != null && level.getEntity(hub.ash) instanceof AshEntity known && known.isAlive() ? known : null;
        }
        List<AshEntity> there = level.getEntitiesOfClass(AshEntity.class, new AABB(spot).inflate(24), AshEntity::isAlive);
        if (!there.isEmpty()) {
            // There is only one Ash: keep the one the hub knows (else the first) and send any others away.
            AshEntity keep = there.stream().filter(a -> a.getUUID().equals(hub.ash)).findFirst().orElse(there.getFirst());
            for (AshEntity a : there) if (a != keep) a.discard();
            if (!keep.getUUID().equals(hub.ash)) {
                hub.ash = keep.getUUID();
                HeavenPlotsSavedData.get(level).setDirty();
            }
            return keep;
        }
        AshEntity ash = AllEntities.ASH.get().create(level);
        if (ash == null) return null;
        ash.post(spot, 0f);
        ash.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.STRUCTURE, null);
        level.addFreshEntity(ash);
        hub.ash = ash.getUUID();
        HeavenPlotsSavedData.get(level).setDirty();
        return ash;
    }

    /** {@code player} talks to {@code ash}: he greets them (the first time earns {@code met_ash}) and his menu opens. */
    public static void talk(AshEntity ash, ServerPlayer player) {
        boolean first = !done(player, "main/met_ash");
        if (first) ChorusRewards.award(player, "main/met_ash");
        int visit = TALKS.merge(player.getUUID(), 1, Integer::sum) - 1;
        ash.say(first ? "laugh" : "talk");
        if (first) ash.level().playSound(null, ash.blockPosition(), AllSounds.heaven("heaven.ash_greet"), SoundSource.NEUTRAL, 1.0f, 1.0f);
        PacketDistributor.sendToPlayer(player, new AshMenuPayload(ash.getId(), menu(player, first, visit)));
    }

    /** What Ash's menu shows {@code player} (see {@link AshMenu}). */
    public static CompoundTag menu(ServerPlayer player, boolean first, int visit) {
        AshDialogue.Progress progress = progress(player, first);
        CompoundTag t = new CompoundTag();
        t.putString(AshMenu.GREETING, AshDialogue.greeting(progress, visit));
        ListTag hints = new ListTag();
        for (AshDialogue.Line line : AshDialogue.hints(progress, visit)) {
            CompoundTag h = new CompoundTag();
            h.putString(AshMenu.HINT_KEY, line.key());
            ListTag args = new ListTag();
            line.args().forEach(a -> args.add(StringTag.valueOf(a)));
            h.put(AshMenu.HINT_ARGS, args);
            hints.add(h);
        }
        t.put(AshMenu.HINTS, hints);
        ListTag visits = new ListTag();
        for (HeavenPlot plot : visitable(player)) {
            CompoundTag v = new CompoundTag();
            v.putString(AshMenu.VISIT_UUID, plot.owner.toString());
            v.putString(AshMenu.VISIT_NAME, plot.ownerName);
            v.putBoolean(AshMenu.VISIT_TRUSTED, plot.trusted.contains(player.getUUID()));
            v.putBoolean(AshMenu.VISIT_ONLINE, player.server.getPlayerList().getPlayer(plot.owner) != null);
            visits.add(v);
        }
        t.put(AshMenu.VISITS, visits);
        t.putBoolean(AshMenu.WELCOME, HeavenPassage.get(player).visitorsWelcome());
        t.putBoolean(AshMenu.VISITS_ENABLED, SNConfig.HEAVEN_VISITS.get());
        t.putBoolean(AshMenu.HAS_PLOT, HeavenPlots.of(player.server, player.getUUID()) != null);
        return t;
    }

    /** Where {@code player} stands, as Ash sees it. */
    public static AshDialogue.Progress progress(ServerPlayer player, boolean first) {
        HeavenStanding s = HeavenPassage.get(player);
        var log = player.getData(AllAttachments.MEMORY_LOG);
        BossProgression.Boss next = BossProgression.next(adv -> done(player, adv));
        return new AshDialogue.Progress(first, HeavenPlots.of(player.server, player.getUUID()) != null, log.entries().size(),
                log.collected().size(), SNConfig.NAOMI_MEMORIES_TO_OPEN.get(), s.naomiWins(), s.zachariahWins(), s.homeUnlocked(),
                s.lastRest() > 0, next == null ? null : next.key() + ".name", s.visitorsWelcome(), visitable(player).size());
    }

    /** The Heavens {@code player} may visit: those that welcome visitors (if the server allows visits) or trust them. */
    public static List<HeavenPlot> visitable(ServerPlayer player) {
        HeavenPlotsSavedData data = HeavenPlotsSavedData.peek(HeavenPlots.level(player.server));
        if (data == null) return List.of();
        List<HeavenPlot> out = new ArrayList<>();
        for (HeavenPlot p : data.all()) {
            if (p.hub() || p.owner.equals(player.getUUID()) || !p.plaza) continue;
            if (mayVisit(player, p)) out.add(p);
        }
        out.sort(Comparator.comparing(p -> p.ownerName.toLowerCase(java.util.Locale.ROOT)));
        return out.size() > AshMenu.MAX_VISITS ? out.subList(0, AshMenu.MAX_VISITS) : out;
    }

    public static boolean mayVisit(ServerPlayer player, HeavenPlot plot) {
        return plot.trusted.contains(player.getUUID()) || plot.welcome && SNConfig.HEAVEN_VISITS.get();
    }

    /** A choice in Ash's menu, already checked to come from someone at his bar. */
    public static void choose(AshEntity ash, ServerPlayer player, byte action, String arg) {
        ServerLevel level = player.serverLevel();
        switch (action) {
            case AshChoicePayload.VISIT -> {
                UUID target;
                try {
                    target = UUID.fromString(arg);
                } catch (IllegalArgumentException e) {
                    return;
                }
                HeavenPlot plot = HeavenPlotsSavedData.get(level).of(target);
                if (plot == null || plot.hub() || !mayVisit(player, plot)) {
                    player.displayClientMessage(Component.translatable("message.supernaturalcraft.heaven.not_welcome")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
                    return;
                }
                ash.say("point");
                send(player, plot);
            }
            case AshChoicePayload.GO_HOME -> {
                HeavenPlot own = HeavenPlotsSavedData.get(level).of(player.getUUID());
                if (own == null) {
                    player.displayClientMessage(Component.translatable("message.supernaturalcraft.heaven.no_plot")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
                    return;
                }
                ash.say("nod");
                HeavenPlots.ensure(level, player);
                send(player, own);
            }
            case AshChoicePayload.TOGGLE_VISITORS -> {
                HeavenStanding s = HeavenPassage.get(player);
                HeavenPassage.set(player, s.withVisitors(!s.visitorsWelcome()));
                ash.say("nod");
                PacketDistributor.sendToPlayer(player, new AshMenuPayload(ash.getId(), menu(player, false, TALKS.getOrDefault(player.getUUID(), 0))));
            }
            case AshChoicePayload.HINT -> {
                int visit = TALKS.merge(player.getUUID(), 1, Integer::sum);
                ash.say("talk");
                PacketDistributor.sendToPlayer(player, new AshMenuPayload(ash.getId(), menu(player, false, visit)));
            }
            default -> ash.say("nod");
        }
    }

    /** Sends {@code player} to {@code plot}'s landing (both in the level plots live in). */
    public static void send(ServerPlayer player, HeavenPlot plot) {
        ServerLevel level = HeavenPlots.level(player.server);
        var to = HeavenPlots.landing(level, plot);
        if (player.serverLevel() == level) {
            HeavenPassage.move(player, to, HeavenPlots.LANDING_YAW);
        } else {
            player.teleportTo(level, to.x, to.y, to.z, HeavenPlots.LANDING_YAW, 0);
        }
        HeavenPassage.arrived(player, plot);
    }

    private static boolean done(ServerPlayer player, String id) {
        AdvancementHolder adv = player.server.getAdvancements().get(SupernaturalCraft.asResource(id));
        return adv != null && player.getAdvancements().getOrStartProgress(adv).isDone();
    }
}
