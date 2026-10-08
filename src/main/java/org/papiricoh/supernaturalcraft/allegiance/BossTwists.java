package org.papiricoh.supernaturalcraft.allegiance;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.BossDamage;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What the bosses make of a hunter's side (v0.13), by events and minimal hooks; their fights do not change otherwise.
 * <ul>
 *   <li>Every boss greets an angel, a demon or a ranked hunter once per arena:
 *   {@code message.supernaturalcraft.allegiance.boss.<boss>.<angel|demon|human>} (boss = its entity id path).</li>
 *   <li>Lucifer (exactly {@code lucifer}, in his second phase) offers each demon to serve him, once per arena
 *   ({@link LuciferBargain}).</li>
 *   <li>Amara: Consumption rises at half the rate for demons; angels lose Grace in her arena and strike her for 20% less;
 *   half her attacks pass over a demon; no Corruption is gained in her darkness.</li>
 *   <li>Chuck: his arena ({@link ArenaTheme#AUTHOR}) suppresses every power ({@link org.papiricoh.supernaturalcraft.allegiance.power.PowerCaster}).</li>
 * </ul>
 */
public final class BossTwists {

    /** Amara ignores a demon in alternate windows of this many ticks. */
    public static final int AMARA_WINDOW = 100;
    public static final float AMARA_DEMON_CONSUMPTION = 0.5f, AMARA_ANGEL_DAMAGE = 0.8f;

    private static final Map<UUID, Set<UUID>> GREETED = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<UUID>> OFFERED = new ConcurrentHashMap<>();

    private BossTwists() {
    }

    /** Every second, per level: greetings and Lucifer's offer. */
    public static void tick(ServerLevel level) {
        ArenaSavedData data = ArenaSavedData.get(level);
        Set<UUID> live = new HashSet<>();
        for (ArenaController arena : data.all()) {
            if (!arena.isActive() || arena.bossId() == null) continue;
            live.add(arena.id());
            if (!(level.getEntity(arena.bossId()) instanceof LivingEntity boss) || !boss.isAlive()) continue;
            List<ServerPlayer> inside = new ArrayList<>();
            for (ServerPlayer p : level.players()) {
                if (p.isAlive() && !p.isSpectator() && arena.contains(p.position())) inside.add(p);
            }
            visit(arena.id(), boss, inside);
        }
        GREETED.keySet().retainAll(live);
        OFFERED.keySet().retainAll(live);
    }

    /** One arena's boss and the hunters inside it (public for tests, whose players are not in the level's list). */
    public static void visit(UUID arena, LivingEntity boss, List<ServerPlayer> inside) {
        for (ServerPlayer p : inside) greet(arena, boss, p);
        if (boss.getType() == AllEntities.LUCIFER.get() && boss instanceof LuciferEntity lucifer && lucifer.phase() == 2
                && lucifer.state() != LuciferEntity.DYING) {
            for (ServerPlayer p : inside) {
                if (!Allegiances.get(p).isDemon() || LuciferBargain.worn(p)) continue;
                if (OFFERED.computeIfAbsent(arena, k -> ConcurrentHashMap.newKeySet()).add(p.getUUID())) LuciferBargain.offer(p, lucifer);
            }
        }
    }

    /** Whether Lucifer already made his offer to {@code player} in {@code arena}. */
    public static boolean offered(UUID arena, Player player) {
        Set<UUID> s = OFFERED.get(arena);
        return s != null && s.contains(player.getUUID());
    }

    static void greet(UUID arena, LivingEntity boss, ServerPlayer p) {
        Allegiance a = Allegiances.get(p);
        if (a.isHuman() && a.rank() <= 0) return;
        if (!GREETED.computeIfAbsent(arena, k -> ConcurrentHashMap.newKeySet()).add(p.getUUID())) return;
        p.displayClientMessage(Component.translatable(greetingKey(boss, a.faction()))
                .withStyle(a.isAngel() ? ChatFormatting.GOLD : a.isDemon() ? ChatFormatting.RED : ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
    }

    public static String greetingKey(Entity boss, Faction faction) {
        return "message.supernaturalcraft.allegiance.boss." + BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()).getPath()
                + "." + faction.getSerializedName();
    }

    /** The bosses who greet (entity id paths): every key {@link #greetingKey} can produce has a line. */
    public static final List<String> GREETERS = List.of("azazel", "lilith", "lucifer", "metatron", "amara", "broken_chorus",
            "war", "famine", "pestilence", "death", "lucifer_uncaged", "michael", "chuck", "gabriel", "raphael");

    // --- Amara ---------------------------------------------------------------------------------------------------------

    private static @Nullable ArenaController arenaAt(ServerPlayer p) {
        return ArenaSavedData.get(p.serverLevel()).at(p.position());
    }

    /** In Amara's darkness (her arena). */
    public static boolean inDarkness(ServerPlayer p) {
        ArenaController a = arenaAt(p);
        return a != null && a.theme() == ArenaTheme.DARKNESS;
    }

    /** Consumption's step for {@code p}: a demon is eaten at half the rate. */
    public static float consumption(ServerPlayer p, float delta) {
        return delta > 0 && Allegiances.get(p).isDemon() ? delta * AMARA_DEMON_CONSUMPTION : delta;
    }

    /** Each Consumption step ({@code ticks} long): an angel's Grace drains in her darkness. */
    public static void amaraDrain(ServerPlayer p, int ticks) {
        if (Allegiances.get(p).isAngel()) Allegiances.addEssence(p, -EssenceRules.AMARA_DRAIN_PER_SECOND * ticks / 20f);
    }

    /** Whether her attack passes over {@code e} now: a demon, in every other window. */
    public static boolean amaraIgnores(Entity e, long gameTime) {
        return Kin.isDemon(e) && e instanceof Player && ((gameTime / AMARA_WINDOW + e.getId()) & 1) == 0;
    }

    /** A blow on Amara: an angel's are 20% weaker (exact damage, the Colt's, is left alone). */
    public static float amaraDamage(DamageSource source, float amount) {
        return source.getEntity() instanceof Player p && Allegiances.get(p).isAngel() && !BossDamage.isExact(source)
                ? amount * AMARA_ANGEL_DAMAGE : amount;
    }

    // --- Chuck ---------------------------------------------------------------------------------------------------------

    /** Powers are nullified in the Author's arena ("I gave you that"). */
    public static boolean suppressed(ServerPlayer p) {
        ArenaController a = arenaAt(p);
        return a != null && a.theme() == ArenaTheme.AUTHOR;
    }
}
