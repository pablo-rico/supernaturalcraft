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
    public static Component name(Officer o) {
        return Component.translatable("chain.supernaturalcraft.rank." + o.rank.name().toLowerCase(java.util.Locale.ROOT))
                .append(" " + ChainNames.name(o) + " ")
                .append(Component.translatable("chain.supernaturalcraft.epithet." + ChainNames.epithet(o.faction, o.branch, o.rank)));
    }
    public static void bind(Mob mob, Officer o) {
        mob.getPersistentData().putInt(OFFICER_ID, o.id);
        mob.getPersistentData().putString(FACTION, o.faction.id());
        mob.setPersistenceRequired();
        MATERIALIZED.put(new Key(o.faction, o.id), mob.getUUID());
        PRESENT.put(mob.getUUID(), new Presence(mob, mob.level().getGameTime()));
        apply(mob, o);
    }
    public static void apply(Mob mob, Officer o) {
        float share = mob.getHealth() / mob.getMaxHealth();
        modifier(mob, Attributes.MAX_HEALTH, "chain_health", ChainRules.healthMultiplier(o.rank, o.level));
        modifier(mob, Attributes.ATTACK_DAMAGE, "chain_damage", ChainRules.damageMultiplier(o.rank, o.level));
        mob.setHealth(mob.getMaxHealth() * share);
        mob.setCustomName(name(o)); mob.setCustomNameVisible(true);
        if (mob instanceof BlackEyedDemon d) { d.setVariant(o.variant); d.setOfficerRank(o.rank.ordinal()); }
        if (mob instanceof AngelEntity a) { a.setVessel(o.variant); a.setOfficerRank(o.rank.ordinal()); }
    }
    private static void modifier(Mob mob, net.minecraft.core.Holder<Attribute> attribute, String id, double multiplier) {
        var a = mob.getAttribute(attribute);
        if (a != null) a.addOrUpdateTransientModifier(new AttributeModifier(SupernaturalCraft.asResource(id), multiplier - 1,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }
    private static String type(Officer o) {
        return o.faction == ChainFaction.ANGEL ? "supernaturalcraft:" + (o.branch == ChainFaction.Branch.FALLEN ? "fallen_angel" : "angel") : o.entityType;
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
        if (!SNConfig.CHAIN_ENABLED.get() || !o.alive() || MATERIALIZED.containsKey(new Key(o.faction, o.id)) || !ground(level, pos)) return null;
        Entity entity = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(type(o))).create(level);
        if (!(entity instanceof Mob mob) || !ChainEvents.eligible(mob)) return null;
        mob.moveTo(Vec3.atBottomCenterOf(pos));
        if (!level.noCollision(mob)) return null;
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        bind(mob, o);
        if (!level.addFreshEntity(mob)) { untrack(mob); return null; }
        if (viewer != null) {
            if (!ChainEvents.sameSide(viewer, o)) mob.setTarget(viewer);
            Send.toPlayer(viewer, new ChainNoticePayload(ChainNoticePayload.TITLE, ChainNames.name(o), o.rank.name(), o.faction.id(),
                    "chain.supernaturalcraft.epithet." + ChainNames.epithet(o.faction, o.branch, o.rank), List.of()));
        }
        return mob;
    }
    private static void escort(Officer leader, Mob mob, ServerPlayer target) {
        var data = ChainSavedData.get(target.server);
        var children = new ArrayList<>(data.hierarchy(leader.faction).subordinates(leader.id));
        int count = ChainRules.escortSize(leader.rank, ChainRules.rng(target.serverLevel().getSeed(), leader.id, ChainEvents.day(target.server)));
        for (int i = 0; i < count; i++) {
            BlockPos pos = mob.blockPosition().offset((i % 3 - 1) * 2, 0, 2 + i / 3 * 2);
            if (!ground(target.serverLevel(), pos)) continue;
            if (i < children.size() && at(children.get(i), target.serverLevel(), pos, target) != null) continue;
            Entity entity = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(type(leader))).create(target.serverLevel());
            if (!(entity instanceof Mob escort)) continue;
            escort.moveTo(Vec3.atBottomCenterOf(pos));
            if (!target.serverLevel().noCollision(escort)) continue;
            escort.finalizeSpawn(target.serverLevel(), target.serverLevel().getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            escort.getPersistentData().putBoolean(ESCORT, true);
            escort.getPersistentData().putBoolean("sn_chain_excluded", true);
            escort.setPersistenceRequired();
            if (!ChainEvents.sameSide(target, leader)) escort.setTarget(target);
            if (target.serverLevel().addFreshEntity(escort)) PRESENT.put(escort.getUUID(), new Presence(escort, escort.level().getGameTime()));
        }
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
                if (mob.getTarget() instanceof ServerPlayer p && sameSideOrInactive(p, o)) mob.setTarget(null);
            }
        }
        if (!SNConfig.CHAIN_ENABLED.get() || server.getPlayerList().getPlayers().isEmpty()) return;
        for (var f : ChainFaction.values()) {
            Hierarchy h = data.hierarchy(f);
            if (h.lord() == null && h.ofRank(ChainRank.LIEUTENANT).isEmpty()) continue;
            var site = org.papiricoh.supernaturalcraft.chain.seat.SeatSite.of(server.overworld().getSeed(), f);
            if (server.getPlayerList().getPlayers().stream().noneMatch(p -> p.level() == server.overworld()
                    && Math.abs(p.getX() - site.x()) < 128 && Math.abs(p.getZ() - site.z()) < 128)) continue;
            SeatWorld.Anchors anchors = seat(server.overworld(), f);
            var spots = new ArrayList<BlockPos>(); spots.add(anchors.lord()); spots.addAll(anchors.lieutenants());
            var occupants = new ArrayList<Officer>(); occupants.add(h.lord()); occupants.addAll(h.ofRank(ChainRank.LIEUTENANT));
            for (int i = 0; i < occupants.size(); i++) {
                Officer o = occupants.get(i); if (o == null) continue;
                BlockPos pos = spots.get(i);
                ServerPlayer viewer = server.getPlayerList().getPlayers().stream().filter(p -> p.level() == server.overworld()
                        && p.distanceToSqr(Vec3.atCenterOf(pos)) < 48 * 48).findFirst().orElse(null);
                if (viewer != null) { Mob mob = at(o, server.overworld(), pos, viewer); if (mob != null) escort(o, mob, viewer); }
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
            ChainSavedData.get(event.getLevel().getServer()).changed();
        }
        untrack(event.getEntity());
    }
    public static void clear() { for (var p : List.copyOf(PRESENT.values())) p.mob.discard(); MATERIALIZED.clear(); PRESENT.clear(); }
}
