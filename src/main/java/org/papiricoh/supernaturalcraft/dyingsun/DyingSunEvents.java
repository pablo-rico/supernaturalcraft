package org.papiricoh.supernaturalcraft.dyingsun;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.debug.AuditStats;

import java.util.List;

/**
 * What the dying sun does to the world (v0.42), all of it only in the Overworld and only as dark as the sun is: monsters weigh
 * more in the spawn lists and walk by day once it is half gone, crops grow slower, and past {@link DyingSunRules#UNDEAD_SPARED}
 * the sun no longer burns the dead.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class DyingSunEvents {

    /** Ticks between two daylight walkers near a player, and how many monsters may already be near. */
    static final int DAY_SPAWN_INTERVAL = 200, DAY_SPAWN_CAP = 6;
    private static final List<EntityType<? extends Mob>> DAY_WALKERS = List.of(EntityType.ZOMBIE, EntityType.SKELETON,
            EntityType.SPIDER, EntityType.ZOMBIE, EntityType.HUSK);
    /** Ids of undead that were not burning when their tick began. */
    private static final IntSet UNLIT = new IntOpenHashSet();

    private DyingSunEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        long t = server.overworld().getGameTime();
        if (t % 20 == 0) DyingSun.tick(server, false);
        if (t % DAY_SPAWN_INTERVAL == 0 && DyingSunRules.daySpawns(DyingSun.darkness(server))) {
            long sn$t = AuditStats.start();
            try {
                daySpawns(server.overworld(), DyingSun.darkness(server));
            } finally {
                AuditStats.stop("dyingSun.daySpawns", sn$t);
            }
        }
    }

    /** By day, with the sun more than half gone, the dead walk: now and then one near each player out under the sky. */
    static void daySpawns(ServerLevel level, float d) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING) || !level.isDay()) return;
        RandomSource r = level.random;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || r.nextFloat() > d || !level.canSeeSky(p.blockPosition())) continue;
            int near = level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32), m -> m instanceof Enemy).size();
            if (near >= DAY_SPAWN_CAP) continue;
            double angle = r.nextDouble() * Math.PI * 2, dist = 20 + r.nextDouble() * 12;
            int x = (int) (p.getX() + Math.cos(angle) * dist), z = (int) (p.getZ() + Math.sin(angle) * dist);
            BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (!level.isLoaded(at) || Math.abs(at.getY() - p.getY()) > 16 || !level.getFluidState(at).isEmpty()) continue;
            DAY_WALKERS.get(r.nextInt(DAY_WALKERS.size())).spawn(level, at, MobSpawnType.EVENT);
        }
    }

    /** Natural monster spawns lean on the monsters as the sun fails (each one listed again, once or twice). */
    @SubscribeEvent
    public static void onPotentialSpawns(LevelEvent.PotentialSpawns event) {
        if (event.getMobCategory() != MobCategory.MONSTER || !(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD) return;
        float d = DyingSun.darkness(level.getServer());
        int extra = (int) Math.floor(DyingSunRules.spawnBonus(d)) - 1;
        if (extra <= 0) return;
        List<MobSpawnSettings.SpawnerData> base = List.copyOf(event.getSpawnerDataList());
        for (int i = 0; i < extra; i++) for (MobSpawnSettings.SpawnerData s : base) event.addSpawnerData(s);
    }

    /** Crops grow slower under a dying sun (refused growth ticks, by chance). */
    @SubscribeEvent
    public static void onCropGrow(CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        float chance = DyingSunRules.cropDenyChance(DyingSun.darkness(level.getServer()));
        if (chance > 0 && level.random.nextFloat() < chance) event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
    }

    // Sunlight ignites the undead inside their own tick; whatever caught fire this tick out of a clear sky, with no fire or lava
    // about, was the sun, and the sun is too weak to burn them now.
    @SubscribeEvent
    public static void onEntityTickPre(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide && mob.level().dimension() == Level.OVERWORLD
                && mob.getType().is(EntityTypeTags.UNDEAD) && !mob.isOnFire() && mob.level().getServer() != null
                && DyingSunRules.undeadSpared(DyingSun.darkness(mob.level().getServer()))) {
            UNLIT.add(mob.getId());
        }
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide || !UNLIT.remove(mob.getId())) return;
        if (mob.isOnFire() && !mob.isInLava() && !mob.isInWall() && mob.level().getBlockStates(mob.getBoundingBox())
                .noneMatch(s -> s.is(net.minecraft.tags.BlockTags.FIRE) || s.is(net.minecraft.world.level.block.Blocks.LAVA))) {
            mob.clearFire();
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) DyingSun.sync(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) DyingSun.sync(p);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) DyingSun.sync(p);
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        DyingSun.reset();
        UNLIT.clear();
    }
}
