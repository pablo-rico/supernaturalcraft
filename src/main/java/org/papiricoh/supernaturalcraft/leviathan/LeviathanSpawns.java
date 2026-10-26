package org.papiricoh.supernaturalcraft.leviathan;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.angel.HostsSavedData;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.entity.leviathan.LeviathanEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * When the Leviathans come (v0.25): once anyone in the world has beaten Raphael (Castiel swallowed Purgatory, and they rode
 * out with him). Kept in {@link HostsSavedData#leviathans()}; opened by Raphael's advancement (a share of the credit or a
 * command counts too), and on the first login of a hunter who beat him before v0.25. They never spawn naturally: they take
 * villagers' places at night ({@link #infiltrate}), and come with the Men of Letters' village cases. Eggs and commands always work.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class LeviathanSpawns {

    private LeviathanSpawns() {
    }

    public static boolean open(ServerLevel level) {
        return HostsSavedData.get(level.getServer()).leviathans();
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (event.getAdvancement().id().equals(SupernaturalCraft.asResource(BossProgression.Boss.RAPHAEL.advancement))) {
            HostsSavedData.get(p.server).setLeviathans(true);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        HostsSavedData data = HostsSavedData.get(p.server);
        if (data.leviathans()) return;
        AdvancementHolder adv = p.server.getAdvancements().get(SupernaturalCraft.asResource(BossProgression.Boss.RAPHAEL.advancement));
        if (adv != null && p.getAdvancements().getOrStartProgress(adv).isDone()) data.setLeviathans(true);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        if (level.getGameTime() % LeviathanRules.INFILTRATION_PERIOD != 0 || !level.isNight() || !open(level)) return;
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator()) infiltrate(level, p, level.random.nextFloat(), level.random.nextFloat());
        }
    }

    /** {@link #infiltrate(ServerLevel, Player, float, float)}, never in plain sight. */
    public static LeviathanEntity infiltrate(ServerLevel level, Player hunter, float roll) {
        return infiltrate(level, hunter, roll, 1f);
    }

    /**
     * One night's check in the village around {@code hunter}: with {@code roll} under the chance, a villager is taken. Most
     * times one nobody is looking at, and a Leviathan quietly wears its face; with {@code witnessRoll} under
     * {@link LeviathanRules#WITNESS_CHANCE}, one in front of {@code hunter} ({@link #takenInSight}). @return the Leviathan, or null
     */
    public static LeviathanEntity infiltrate(ServerLevel level, Player hunter, float roll, float witnessRoll) {
        double r = LeviathanRules.VILLAGE_RADIUS;
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, hunter.getBoundingBox().inflate(r, r / 2, r), v -> v.isAlive() && !v.isBaby());
        int leviathans = level.getEntitiesOfClass(LeviathanEntity.class, hunter.getBoundingBox().inflate(r, r / 2, r), l -> l.master() == null).size();
        if (!LeviathanRules.infiltrates(leviathans, villagers.size(), roll)) return null;
        if (LeviathanRules.witnessed(witnessRoll)) {
            for (Villager v : villagers) {
                if (v.distanceToSqr(hunter) < LeviathanRules.WITNESS_RANGE * LeviathanRules.WITNESS_RANGE && hunter.hasLineOfSight(v)) {
                    return takenInSight(level, v);
                }
            }
        }
        List<Villager> unseen = new ArrayList<>();
        for (Villager v : villagers) {
            boolean seen = false;
            for (Player p : level.players()) {
                if (p.distanceToSqr(v) < 32 * 32 && p.hasLineOfSight(v)) {
                    seen = true;
                    break;
                }
            }
            if (!seen) unseen.add(v);
        }
        if (unseen.isEmpty()) return null;
        return LeviathanEntity.replace(unseen.get(level.random.nextInt(unseen.size())));
    }

    /**
     * A villager taken in plain sight: it screams, and the Leviathan that ate it stands there with its maw open. Every hunter
     * who saw it caught one red-handed ({@link LeviathanRules#RED_HANDED}).
     */
    public static LeviathanEntity takenInSight(ServerLevel level, Villager villager) {
        List<ServerPlayer> saw = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && p.distanceToSqr(villager) < LeviathanRules.WITNESS_RANGE * LeviathanRules.WITNESS_RANGE
                    && p.hasLineOfSight(villager)) {
                saw.add(p);
            }
        }
        level.playSound(null, villager.blockPosition(), net.minecraft.sounds.SoundEvents.VILLAGER_DEATH, net.minecraft.sounds.SoundSource.NEUTRAL, 1.5f, 1.3f);
        LeviathanEntity l = LeviathanEntity.replace(villager);
        if (l == null) return null;
        l.reveal();
        level.playSound(null, l.blockPosition(), org.papiricoh.supernaturalcraft.registry.AllSounds.leviathan("bite"), net.minecraft.sounds.SoundSource.HOSTILE, 1.5f, 0.8f);
        for (ServerPlayer p : saw) {
            org.papiricoh.supernaturalcraft.reward.ChorusRewards.award(p, LeviathanRules.RED_HANDED);
            p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.supernaturalcraft.leviathan.taken")
                    .withStyle(net.minecraft.ChatFormatting.DARK_RED), true);
        }
        return l;
    }
}
