package org.papiricoh.supernaturalcraft.chain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.chain.seat.SeatWorld;
import org.papiricoh.supernaturalcraft.entity.demon.BlackEyedDemon;
import org.papiricoh.supernaturalcraft.entity.angel.AngelEntity;
import org.papiricoh.supernaturalcraft.network.*;
import java.util.*;

@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class OfficerSpawner {
    public static final String OFFICER_ID = "sn_chain_officer_id", FACTION = "sn_chain_faction";
    private static final String ESCORT = "sn_chain_escort";
    /** How near a hunter must come to a stronghold for it to be raised and manned. */
    public static final int SEAT_NOTICE = 160;
    public record Key(ChainFaction faction, int id) {}
    private record Presence(Mob mob, long lastNear) {}
    private static final Map<Key, UUID> MATERIALIZED = new HashMap<>();
    private static final Map<UUID, Presence> PRESENT = new HashMap<>();
    private OfficerSpawner() {}
    public static boolean materialized(Officer o) { return MATERIALIZED.containsKey(new Key(o.faction, o.id)); }
    public static Key key(Entity mob) {
        var t = mob.getPersistentData();
        return t.contains(OFFICER_ID) ? new Key(ChainFaction.byId(t.getString(FACTION)), t.getInt(OFFICER_ID)) : null;
    }
    public static Officer officer(Entity mob) {
        Key k = key(mob);
        return k == null || mob.getServer() == null ? null : ChainSavedData.get(mob.getServer()).hierarchy(k.faction).get(k.id);
    }
    /** The name of the guild a guild's officer serves ("" for the world's chains, whose epithets take no argument). */
    static String guildName(Officer o) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return o.faction.guild() && server != null ? ChainSavedData.get(server).guild(o.faction).name : "";
    }

    public static Component name(Officer o) {
        return Component.translatable(o.rank.titleKey(o.faction))
                .append(" " + ChainNames.name(o) + " ")
                .append(Component.translatable("chain.supernaturalcraft.epithet." + ChainNames.epithet(o.faction, o.branch, o.rank), guildName(o)));
    }
    /** Marks a mob as {@code o} without making it one of the spawner's (a boss keeps its own health and stays put). */
    public static void tag(Mob mob, Officer o) {
        mob.getPersistentData().putInt(OFFICER_ID, o.id);
        mob.getPersistentData().putString(FACTION, o.faction.id());
    }

    public static void bind(Mob mob, Officer o) {
        mob.getPersistentData().putInt(OFFICER_ID, o.id);
        mob.getPersistentData().putString(FACTION, o.faction.id());
        mob.setPersistenceRequired();
        MATERIALIZED.put(new Key(o.faction, o.id), mob.getUUID());
        PRESENT.put(mob.getUUID(), new Presence(mob, mob.level().getGameTime()));
        apply(mob, o);
        sighted(mob, o);
    }
    /**
     * Lets {@code o}'s mob go (v0.29: a guild that lets an officer go, or dissolves): it stays in the world as the wild mob it
     * was, nobody's officer. Call before taking the record out of its chain.
     */
    public static void release(Officer o) {
        Mob mob = mob(o);
        if (mob == null) return;
        untrack(mob);
        mob.getPersistentData().remove(OFFICER_ID);
        mob.getPersistentData().remove(FACTION);
        mob.setCustomName(null);
        mob.setCustomNameVisible(false);
        for (String id : new String[]{"chain_health", "chain_damage"})
            for (var attribute : List.of(Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE)) {
                var a = mob.getAttribute(attribute);
                if (a != null) a.removeModifier(SupernaturalCraft.asResource(id));
            }
        mob.setHealth(Math.min(mob.getHealth(), mob.getMaxHealth()));
        if (mob instanceof BlackEyedDemon d) { d.setOfficerRank(-1); d.setOfficerId(-1); }
        if (mob instanceof AngelEntity a) { a.setOfficerRank(-1); a.setOfficerId(-1); }
        if (mob instanceof org.papiricoh.supernaturalcraft.entity.allegiance.RivalHunterEntity h) h.setOfficerId(-1);
    }

    /** Hands a materialized mob over from one record to another (an officer won over by a guild keeps its body). */
    static void rebind(Officer from, Officer to) {
        Mob mob = mob(from);
        if (mob == null) return;
        untrack(mob);
        bind(mob, to);
    }

    /** Remembers where {@code o} is now, for the book and the war table. */
    static void sighted(Entity mob, Officer o) {
        if (mob.getServer() == null) return;
        o.sighted(mob.level().dimension().location().toString(), mob.getBlockX(), mob.getBlockZ(), ChainEvents.day(mob.getServer()));
        ChainSavedData.get(mob.getServer()).changed();
    }
    /**
     * The officer id a mob syncs to the clients: its own for the world's chains, with a guild's slot (ordinal + 1) above
     * {@link #GUILD_SHIFT} bits (v0.29), so a hired demon is told from one of the Pit's.
     */
    public static final int GUILD_SHIFT = 20;

    public static int syncedId(Officer o) {
        return o.faction.guild() ? (o.faction.ordinal() + 1) << GUILD_SHIFT | o.id : o.id;
    }

    public static void apply(Mob mob, Officer o) {
        float share = mob.getHealth() / mob.getMaxHealth();
        modifier(mob, Attributes.MAX_HEALTH, "chain_health", ChainRules.healthMultiplier(o.rank, o.level));
        modifier(mob, Attributes.ATTACK_DAMAGE, "chain_damage", ChainRules.damageMultiplier(o.rank, o.level));
        mob.setHealth(mob.getMaxHealth() * share);
        mob.setCustomName(name(o)); mob.setCustomNameVisible(true);
        if (mob instanceof BlackEyedDemon d) { d.setVariant(o.variant); d.setOfficerRank(o.rank.ordinal()); d.setOfficerId(syncedId(o)); }
        if (mob instanceof AngelEntity a) { a.setVessel(o.variant); a.setOfficerRank(o.rank.ordinal()); a.setOfficerId(syncedId(o)); }
        if (mob instanceof org.papiricoh.supernaturalcraft.entity.leviathan.BoardLeviathan l) l.setOfficerId(o.id);
        if (mob instanceof org.papiricoh.supernaturalcraft.entity.allegiance.RivalHunterEntity h) { h.setVariant(o.variant); h.setOfficerId(syncedId(o)); }
    }
    private static void modifier(Mob mob, net.minecraft.core.Holder<Attribute> attribute, String id, double multiplier) {
        var a = mob.getAttribute(attribute);
        if (a != null) a.addOrUpdateTransientModifier(new AttributeModifier(SupernaturalCraft.asResource(id), multiplier - 1,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }
    private static String type(Officer o) {
        return ChainRoster.materialize(o.faction, o.branch, o.rank, o.entityType);
    }
    /** The mob {@code o} walks the world in now, or null. */
    public static Mob mob(Officer o) {
        UUID id = MATERIALIZED.get(new Key(o.faction, o.id));
        Presence p = id == null ? null : PRESENT.get(id);
        return p == null || p.mob.isRemoved() ? null : p.mob;
    }
    /** Shows {@code o} (and its escort) on the ground within a few blocks of {@code centre}; null if there is no room. */
    public static Mob around(Officer o, ServerPlayer viewer, BlockPos centre) {
        return around(o, viewer, centre, true, null);
    }
    /** {@link #around}, without its escort (a duel). */
    public static Mob alone(Officer o, ServerPlayer viewer, BlockPos centre) {
        return around(o, viewer, centre, false, null);
    }
    /** {@link #around}; {@code escorts}, if given, receives the escort that came with it. */
    public static Mob around(Officer o, ServerPlayer viewer, BlockPos centre, boolean escorted, List<Mob> escorts) {
        ServerLevel level = viewer.serverLevel();
        for (int attempt = 0; attempt < 12; attempt++) {
            BlockPos column = centre.offset(attempt == 0 ? 0 : level.random.nextInt(9) - 4, 0, attempt == 0 ? 0 : level.random.nextInt(9) - 4);
            for (int dy = 6; dy >= -8; dy--) {
                BlockPos at = column.offset(0, dy, 0);
                if (!ground(level, at)) continue;
                Mob mob = at(o, level, at, viewer);
                if (mob != null && escorted) {
                    var spawned = escort(o, mob, viewer);
                    if (escorts != null) escorts.addAll(spawned);
                }
                return mob;
            }
        }
        return null;
    }
    public static Mob near(Officer o, ServerPlayer target) {
        if (!SNConfig.CHAIN_ENABLED.get() || o == null || !o.alive()) return null;
        ServerLevel level = target.serverLevel();
        for (int attempt = 0; attempt < 32; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2, distance = 16 + level.random.nextDouble() * 16;
            BlockPos column = BlockPos.containing(target.getX() + Math.cos(angle) * distance, target.getY(), target.getZ() + Math.sin(angle) * distance);
            for (int dy = 8; dy >= -12; dy--) {
                BlockPos at = column.offset(0, dy, 0);
                if (!ground(level, at)) continue;
                Mob mob = at(o, level, at, target);
                if (mob != null) escort(o, mob, target);
                return mob;
            }
        }
        return null;
    }
    private static boolean ground(ServerLevel level, BlockPos at) {
        return level.hasChunkAt(at) && level.getWorldBorder().isWithinBounds(at)
                && level.getBlockState(at.below()).isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP)
                && level.getBlockState(at).getCollisionShape(level, at).isEmpty()
                && level.getBlockState(at.above()).getCollisionShape(level, at.above()).isEmpty()
                && level.getFluidState(at).isEmpty();
    }
    public static Mob at(Officer o, ServerLevel level, BlockPos pos, ServerPlayer viewer) {
        // A post a hunter holds is never walked by a mob.
        if (!SNConfig.CHAIN_ENABLED.get() || !o.alive() || o.held() || MATERIALIZED.containsKey(new Key(o.faction, o.id)) || !ground(level, pos)) return null;
        Entity entity = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(type(o))).map(t -> t.create(level)).orElse(null);
        if (!(entity instanceof Mob mob) || !ChainEvents.eligible(mob)) return null;
        mob.moveTo(Vec3.atBottomCenterOf(pos));
        if (!level.noCollision(mob)) return null;
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        bind(mob, o);
        if (!level.addFreshEntity(mob)) { untrack(mob); return null; }
        if (viewer != null) {
            if (!ChainEvents.sameSide(viewer, o) && !ChainTalk.spares(o, viewer)) mob.setTarget(viewer);
            OfficerVoice.greet(mob, o, viewer);
            Send.toPlayer(viewer, new ChainNoticePayload(ChainNoticePayload.TITLE, ChainNames.name(o), o.rank.name(), o.faction.id(),
                    "chain.supernaturalcraft.epithet." + ChainNames.epithet(o.faction, o.branch, o.rank),
                    o.faction.guild() ? List.of(guildName(o)) : List.of()));
        }
        return mob;
    }
    private static List<Mob> escort(Officer leader, Mob mob, ServerPlayer target) {
        List<Mob> out = new ArrayList<>();
        var data = ChainSavedData.get(target.server);
        var children = new ArrayList<>(data.hierarchy(leader.faction).subordinates(leader.id));
        int count = ChainRules.escortSize(leader.rank, ChainRules.rng(target.serverLevel().getSeed(), leader.id, ChainEvents.day(target.server)));
        for (int i = 0; i < count; i++) {
            BlockPos pos = mob.blockPosition().offset((i % 3 - 1) * 2, 0, 2 + i / 3 * 2);
            if (!ground(target.serverLevel(), pos)) continue;
            if (i < children.size()) {
                Mob child = at(children.get(i), target.serverLevel(), pos, target);
                if (child != null) { out.add(child); continue; }
            }
            // A guild has no rank and file of its own: only the officers it hired walk with its own.
            if (leader.faction.guild()) continue;
            var rng = ChainRules.rng(target.serverLevel().getSeed(), leader.id * 31 + i, target.serverLevel().getGameTime());
            String kind = ChainRoster.draw(ChainRoster.pool(leader.faction, leader.branch), rng);
            Entity entity = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(kind)).map(t -> t.create(target.serverLevel())).orElse(null);
            if (!(entity instanceof Mob escort)) continue;
            escort.moveTo(Vec3.atBottomCenterOf(pos));
            if (!target.serverLevel().noCollision(escort)) continue;
            escort.finalizeSpawn(target.serverLevel(), target.serverLevel().getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            escort.getPersistentData().putBoolean(ESCORT, true);
            escort.getPersistentData().putBoolean("sn_chain_excluded", true);
            escort.setPersistenceRequired();
            if (!ChainEvents.sameSide(target, leader) && !ChainTalk.spares(leader, target)) escort.setTarget(target);
            if (target.serverLevel().addFreshEntity(escort)) {
                PRESENT.put(escort.getUUID(), new Presence(escort, escort.level().getGameTime()));
                out.add(escort);
            }
        }
        return out;
    }
    public static void tick(MinecraftServer server) {
        var data = ChainSavedData.get(server);
        for (var entry : List.copyOf(PRESENT.values())) {
            Mob mob = entry.mob;
            Officer o = officer(mob);
            if (!SNConfig.CHAIN_ENABLED.get() || mob.isRemoved() || key(mob) != null && (o == null || !o.alive()
                    || !BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString().equals(type(o)))) {
                if (!mob.isRemoved()) mob.discard(); untrack(mob); continue;
            }
            long now = mob.level().getGameTime();
            boolean nearby = server.getPlayerList().getPlayers().stream().anyMatch(p -> p.level() == mob.level() && p.distanceToSqr(mob) < 64 * 64);
            if (nearby) PRESENT.put(mob.getUUID(), new Presence(mob, now));
            else if (now - entry.lastNear >= 3600) { mob.discard(); continue; }
            if (o != null) {
                apply(mob, o);
                if (mob.getTarget() instanceof ServerPlayer p && sameSideOrInactive(p, o) && !DuelStage.duelling(mob, p)) mob.setTarget(null);
            }
        }
        if (!SNConfig.CHAIN_ENABLED.get() || server.getPlayerList().getPlayers().isEmpty()) return;
        for (var f : ChainFaction.values()) {
            Hierarchy h = data.hierarchy(f);
            if (!data.awake(f)) continue;
            if (f.guild()) {
                standard(server, data, h);
                continue;
            }
            territories(server, h);
            // An awake chain's stronghold is raised at once, out where nobody has been, not when a hunter first comes near.
            SeatWorld.Anchors anchors = seat(server.overworld(), f);
            if (anchors == null) continue;
            BlockPos origin = anchors.origin();
            if (server.getPlayerList().getPlayers().stream().noneMatch(p -> p.level() == server.overworld()
                    && Math.abs(p.getX() - origin.getX()) < SEAT_NOTICE && Math.abs(p.getZ() - origin.getZ()) < SEAT_NOTICE)) continue;
            Garrison.raise(server, f, anchors);
            // Only the Lord and its Commanders hold court; while the throne is vacant the Commanders keep the hall.
            var spots = new ArrayList<BlockPos>(); spots.add(anchors.lord()); spots.addAll(anchors.commanders());
            var occupants = new ArrayList<Officer>(); occupants.add(h.lord());
            occupants.addAll(posts(h.ofRank(ChainRank.COMMANDER), anchors.commanders().size()));
            for (int i = 0; i < occupants.size(); i++) {
                Officer o = occupants.get(i); if (o == null) continue;
                BlockPos pos = spots.get(i);
                ServerPlayer viewer = server.getPlayerList().getPlayers().stream().filter(p -> p.level() == server.overworld()
                        && p.distanceToSqr(Vec3.atCenterOf(pos)) < 48 * 48).findFirst().orElse(null);
                if (viewer != null) { Mob mob = at(o, server.overworld(), pos, viewer); if (mob != null) escort(o, mob, viewer); }
            }
        }
    }
    /**
     * A guild's officers (v0.29) walk only while its standard stands: the lesser ones their territories, its Commanders
     * beside the standard when a hunter comes near it.
     */
    private static void standard(MinecraftServer server, ChainSavedData data, Hierarchy h) {
        Guild g = data.guild(h.faction());
        if (!g.hasStandard) return;
        territories(server, h);
        ServerLevel level = GuildLife.standardLevel(server, g);
        if (level == null) return;
        BlockPos at = new BlockPos(g.standardX, g.standardY, g.standardZ);
        List<Officer> commanders = h.ofRank(ChainRank.COMMANDER).stream().filter(o -> !o.held()).toList();
        for (int i = 0; i < commanders.size(); i++) {
            Officer o = commanders.get(i);
            if (materialized(o)) continue;
            BlockPos pos = at.offset(i % 2 == 0 ? 3 : -3, 0, 2);
            ServerPlayer viewer = level.players().stream().filter(p -> p.distanceToSqr(Vec3.atCenterOf(pos)) < 48 * 48).findFirst().orElse(null);
            if (viewer != null) at(o, level, pos, viewer);
        }
    }

    /** One slot per post, in post order; empty posts are null. */
    private static List<Officer> posts(List<Officer> holders, int count) {
        var out = new ArrayList<Officer>(Collections.nCopies(count, (Officer) null));
        for (int i = 0; i < Math.min(count, holders.size()); i++) out.set(i, holders.get(i));
        return out;
    }
    /** An officer without a seat shows itself (with its escort) when a hunter comes near its territory. */
    private static void territories(MinecraftServer server, Hierarchy h) {
        ServerLevel level = server.overworld();
        for (Officer o : h.alive()) {
            if (o.rank.seated() || o.held() || materialized(o)) continue;
            var site = ChainLife.territory(server, o);
            ServerPlayer viewer = server.getPlayerList().getPlayers().stream().filter(p -> p.level() == level && p.isAlive() && !p.isSpectator()
                    && Math.abs(p.getX() - site.x()) < Territory.NOTICE && Math.abs(p.getZ() - site.z()) < Territory.NOTICE).findFirst().orElse(null);
            if (viewer == null) continue;
            for (int attempt = 0; attempt < 8; attempt++) {
                int x = site.x() + level.random.nextInt(Territory.ROAM) - Territory.ROAM / 2;
                int z = site.z() + level.random.nextInt(Territory.ROAM) - Territory.ROAM / 2;
                if (!level.hasChunkAt(new BlockPos(x, 0, z))) continue;
                BlockPos pos = new BlockPos(x, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                Mob mob = at(o, level, pos, viewer);
                if (mob != null) { escort(o, mob, viewer); break; }
            }
        }
    }
    private static boolean sameSideOrInactive(ServerPlayer p, Officer o) { return !p.isAlive() || ChainEvents.sameSide(p, o); }
    /** Single adapter for the seat subsystem. */
    private static SeatWorld.Anchors seat(ServerLevel level, ChainFaction faction) { return SeatWorld.ensure(level, faction); }
    private static void untrack(Entity mob) {
        Key k = key(mob); if (k != null) MATERIALIZED.remove(k, mob.getUUID()); PRESENT.remove(mob.getUUID());
    }
    @SubscribeEvent public static void join(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel) || !(event.getEntity() instanceof Mob mob)) return;
        if (key(mob) == null && !mob.getPersistentData().getBoolean(ESCORT)) return;
        // Serialized projections are stale. Recreate from the record, even after chunk unload or restart.
        if (event.loadedFromDisk() || !SNConfig.CHAIN_ENABLED.get()) { event.setCanceled(true); return; }
        Key k = key(mob);
        if (k != null && MATERIALIZED.containsKey(k) && !MATERIALIZED.get(k).equals(mob.getUUID())) event.setCanceled(true);
    }
    @SubscribeEvent public static void leave(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        Officer o = officer(event.getEntity());
        if (o != null && PRESENT.containsKey(event.getEntity().getUUID())) {
            if (event.getEntity() instanceof BlackEyedDemon d) o.variant = d.getVariant();
            if (event.getEntity() instanceof AngelEntity a) o.variant = a.vessel();
            sighted(event.getEntity(), o);
        }
        untrack(event.getEntity());
    }
    public static void clear() { for (var p : List.copyOf(PRESENT.values())) p.mob.discard(); MATERIALIZED.clear(); PRESENT.clear(); }
}
