package org.papiricoh.supernaturalcraft.hands;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.dyingsun.DyingSun;
import org.papiricoh.supernaturalcraft.hands.mob.BansheeEntity;
import org.papiricoh.supernaturalcraft.hands.mob.ChitterEntity;
import org.papiricoh.supernaturalcraft.hands.mob.ChittersRules;
import org.papiricoh.supernaturalcraft.util.WorldClock;

/**
 * Season 11's wild creatures (v0.42), by the slice's own spawner rather than the biomes' lists: every
 * {@link HandsSpawnRules#PERIOD} ticks it looks round each hunter in the Overworld and, by {@link HandsSpawnRules}, may raise a
 * banshee in the haunted dark or a Chitter pack deep in the caves, a little way off. It also opens season 11 for the world
 * ({@link HandsSavedData}) when the Darkness is first seen loose or a hunter who let her out logs in.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class HandsSpawns {

    /** The core's step "the Darkness is released" (any hunter who has it has season 11 open). */
    public static final ResourceLocation THE_DARKNESS = SupernaturalCraft.asResource("main/the_darkness");

    private HandsSpawns() {
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % HandsSpawnRules.PERIOD != 0) return;
        ServerLevel level = server.overworld();
        boolean loose = DyingSun.released(server);
        HandsSavedData data = HandsSavedData.get(server);
        if (loose) data.setOpened(true);
        if (!level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)) return;
        long day = WorldClock.day(server);
        boolean awake = data.opened() && ChittersRules.awake(day);
        if (!loose && !awake) return;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            RandomSource r = level.random;
            if (loose && r.nextDouble() < HandsSpawnRules.BANSHEE_CHANCE) banshee(level, p, r);
            if (awake && p.getY() < ChittersRules.MAX_Y + 24 && r.nextDouble() < HandsSpawnRules.CHITTER_CHANCE) chitters(level, p, r, day);
        }
    }

    /** Tries to raise a banshee round {@code p}. @return her, or null */
    public static @Nullable BansheeEntity banshee(ServerLevel level, ServerPlayer p, RandomSource r) {
        if (!HandsSpawnRules.banshee(DyingSun.released(level.getServer()), level.isNight(), level.getBiome(p.blockPosition()).is(HandsRegistry.BANSHEE_HAUNTS))) {
            return null;
        }
        if (!level.getEntitiesOfClass(BansheeEntity.class, p.getBoundingBox().inflate(64)).isEmpty()) return null;
        double a = r.nextDouble() * Math.PI * 2, d = HandsSpawnRules.MIN_DISTANCE + r.nextDouble() * (HandsSpawnRules.MAX_DISTANCE - HandsSpawnRules.MIN_DISTANCE);
        int x = (int) (p.getX() + Math.cos(a) * d), z = (int) (p.getZ() + Math.sin(a) * d);
        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        if (!level.isLoaded(ground) || !level.getBiome(ground).is(HandsRegistry.BANSHEE_HAUNTS) || Math.abs(ground.getY() - p.getY()) > 20) return null;
        return HandsRegistry.BANSHEE.get().spawn(level, ground.above(), MobSpawnType.NATURAL);
    }

    /** Tries to raise a Chitter pack in the caves round {@code p}. @return how many rose */
    public static int chitters(ServerLevel level, ServerPlayer p, RandomSource r, long day) {
        if (level.getEntitiesOfClass(ChitterEntity.class, p.getBoundingBox().inflate(48)).size() >= ChittersRules.CAP_NEAR) return 0;
        double a = r.nextDouble() * Math.PI * 2, d = HandsSpawnRules.MIN_DISTANCE + r.nextDouble() * (HandsSpawnRules.MAX_DISTANCE - HandsSpawnRules.MIN_DISTANCE);
        int x = (int) (p.getX() + Math.cos(a) * d), z = (int) (p.getZ() + Math.sin(a) * d);
        int top = Math.min((int) p.getY() + 8, ChittersRules.MAX_Y - 1), bottom = Math.max((int) p.getY() - 16, level.getMinBuildHeight() + 1);
        BlockPos at = null;
        for (int y = top; y >= bottom && at == null; y--) {
            BlockPos q = new BlockPos(x, y, z);
            if (!level.isLoaded(q)) return 0;
            if (level.getBlockState(q).isAir() && level.getBlockState(q.above()).isAir()
                    && level.getBlockState(q.below()).isFaceSturdy(level, q.below(), net.minecraft.core.Direction.UP)) at = q;
        }
        if (at == null || !HandsSpawnRules.chitters(true, day, at.getY(), level.getBrightness(LightLayer.SKY, at), level.getBrightness(LightLayer.BLOCK, at))) {
            return 0;
        }
        int n = ChittersRules.packSize(r.nextDouble()), rose = 0;
        for (int i = 0; i < n; i++) {
            BlockPos spot = i == 0 ? at : at.offset(r.nextInt(3) - 1, 0, r.nextInt(3) - 1);
            if (!level.getBlockState(spot).isAir() || !level.getBlockState(spot.above()).isAir()) spot = at;
            Mob m = HandsRegistry.CHITTER.get().spawn(level, spot, MobSpawnType.EVENT);
            if (m != null) rose++;
        }
        return rose;
    }

    // --- opening season 11 for the world --------------------------------------------------------------------------------------

    private static boolean hasDarkness(ServerPlayer p) {
        AdvancementHolder adv = p.server.getAdvancements().get(THE_DARKNESS);
        return adv != null && p.getAdvancements().getOrStartProgress(adv).isDone();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && hasDarkness(p)) HandsSavedData.get(p.server).setOpened(true);
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getAdvancement().id().equals(THE_DARKNESS)) {
            HandsSavedData.get(p.server).setOpened(true);
        }
    }
}
