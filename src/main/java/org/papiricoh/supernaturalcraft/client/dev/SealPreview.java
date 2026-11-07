package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealEvents;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealKind;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealRules;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealSavedData;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealWorld;
import org.papiricoh.supernaturalcraft.apocalypse.seal.garrison.GarrisonAnchors;
import org.papiricoh.supernaturalcraft.apocalypse.seal.garrison.GarrisonSite;
import org.papiricoh.supernaturalcraft.apocalypse.seal.garrison.GarrisonWorld;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.uriel.UrielAssets;
import org.papiricoh.supernaturalcraft.entity.boss.uriel.UrielSummoning;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.util.WorldClock;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Preview scenes for the seals (v0.40, the Seals slice): {@code seals_sky} (the night sky at stages 2, 3 and 4: the red moon,
 * the blood rain, the falling stars), {@code seal_events} (five kinds of event staged round the camera, with the mini HUD),
 * {@code uriel_model} (Uriel's clips), {@code uriel_fight} (a real fight, with its HUD) and {@code garrison} (the garrison from
 * its gate, from above and the chapel). Captures in {@code runs/client/screenshots/sn_<scene>_<beat>.png}. Same shape as
 * {@code PurgatoryPreview}: it runs only when every name in {@code SN_PREVIEW} is one of its {@link #NAMES}. The world's seals
 * are saved before each scene and put back after it.
 */
final class SealPreview {

    static final List<String> NAMES = List.of("seals_sky", "seal_events", "uriel_model", "uriel_fight", "garrison");
    private static final int FOV = 70;
    private static final int X = 10400, Z = 8800;

    private record Beat(String name, BiConsumer<ServerPlayer, Vec3> server, boolean hud, int length) {
    }

    private static List<String> queue;
    private static String scene;
    private static int t, beatIndex, beatStart;
    private static volatile Vec3 base;
    private static List<Beat> beats;
    private static final List<Entity> staged = new ArrayList<>();
    private static volatile Vec3 camFrom, camAt;
    private static CompoundTag seals;
    private static int savedFov = -1;
    private static double savedFovEffect;

    private SealPreview() {
    }

    static boolean tick(Minecraft mc) {
        String env = DevPreview.env();
        if (env == null) return false;
        if (queue == null) {
            List<String> want = new ArrayList<>();
            for (String s : env.split(",")) want.add(s.trim());
            if (want.isEmpty() || !NAMES.containsAll(want)) return false;
            queue = want;
            savedFov = mc.options.fov().get();
            savedFovEffect = mc.options.fovEffectScale().get();
        }
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) return true;
        if (scene == null) {
            if (queue.isEmpty()) {
                if (savedFov > 0) {
                    mc.options.fov().set(savedFov);
                    mc.options.fovEffectScale().set(savedFovEffect);
                }
                DevPreview.stop(mc);
                return true;
            }
            scene = queue.removeFirst();
            t = -1;
            beats = null;
            beatIndex = 0;
            beatStart = 0;
            base = null;
            camFrom = null;
            mc.options.fov().set(FOV);
            mc.options.fovEffectScale().set(0.0);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.hideGui = true;
        }
        t++;
        if (t == 0) {
            String which = scene;
            server.execute(() -> stage(server, which));
            return true;
        }
        if (base == null || t < 30) return true;
        Vec3 f = camFrom, a = camAt;
        if (f != null && t % 5 == 0) {
            server.execute(() -> {
                ServerPlayer p = player(server);
                if (p.position().distanceToSqr(f) > 0.09) look(p, f, a);
                p.setDeltaMovement(Vec3.ZERO);
            });
        }
        if (beats == null) {
            beats = switch (scene) {
                case "seals_sky" -> sky();
                case "seal_events" -> events();
                case "uriel_model" -> urielModel();
                case "uriel_fight" -> urielFight();
                default -> garrison();
            };
        }
        if (beatIndex >= beats.size()) {
            server.execute(() -> unstage(server));
            scene = null;
            return true;
        }
        Beat b = beats.get(beatIndex);
        if (beatStart == 0) beatStart = t;
        int local = t - beatStart;
        Vec3 c = base;
        if (local == 0) {
            mc.options.hideGui = !b.hud;
            if (b.server != null) server.execute(() -> b.server.accept(player(server), c));
        }
        if (local == b.length - 2) Screenshot.grab(mc.gameDirectory, "sn_" + scene + "_" + b.name + ".png", mc.getMainRenderTarget(), m -> {
        });
        if (local >= b.length) {
            beatIndex++;
            beatStart = t + 1;
        }
        return true;
    }

    // --- staging ---------------------------------------------------------------------------------------------------------------

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static void stage(MinecraftServer server, String which) {
        ServerPlayer p = player(server);
        p.setGameMode(GameType.SPECTATOR);
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.removeAllEffects();
        ServerLevel level = server.overworld();
        seals = SealSavedData.get(server).snapshot();
        org.papiricoh.supernaturalcraft.eclipse.Eclipses.end(level);
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        level.setDayTime(which.equals("garrison") || which.equals("uriel_model") ? 6000 : 18000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.setWeatherParameters(12000, 0, false, false);
        int x = X + NAMES.indexOf(which) * 200;
        level.getChunk(x >> 4, Z >> 4);
        BlockPos g = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, Z));
        for (Entity old : level.getEntitiesOfClass(Entity.class, new AABB(g).inflate(60), e -> !(e instanceof Player))) old.discard();
        if (!which.equals("garrison")) floor(level, g, 28, 28);
        Vec3 b = Vec3.atBottomCenterOf(g);
        p.teleportTo(level, b.x, b.y + 3, b.z + 8, 180, 15);
        base = b;
    }

    private static void unstage(MinecraftServer server) {
        staged.forEach(Entity::discard);
        staged.clear();
        SealEvents.endAll(server);
        ServerLevel level = server.overworld();
        for (Entity e : level.getEntitiesOfClass(Entity.class, new AABB(BlockPos.containing(base)).inflate(90),
                e -> e instanceof LuciferEntity || e instanceof TelegraphMarker || e instanceof Mob)) e.discard();
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        level.setWeatherParameters(12000, 0, false, false);
        if (seals != null) SealSavedData.get(server).restore(seals);
        SealWorld.syncAll(server);
        player(server).setGameMode(GameType.SPECTATOR);
    }

    private static void floor(ServerLevel level, BlockPos g, int rx, int rz) {
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dz = -rz; dz <= rz; dz++) {
                int h = Math.floorMod(dx * 7 + dz * 13, 11);
                level.setBlockAndUpdate(g.offset(dx, -1, dz), h == 0 ? Blocks.COARSE_DIRT.defaultBlockState() : h < 3 ? Blocks.PODZOL.defaultBlockState()
                        : Blocks.GRASS_BLOCK.defaultBlockState());
                for (int dy = 0; dy < 12; dy++) level.setBlockAndUpdate(g.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static <T extends Mob> T mob(ServerLevel level, EntityType<T> type, Vec3 at, float yaw) {
        T e = type.create(level);
        e.moveTo(at.x, at.y, at.z, yaw, 0);
        e.setYHeadRot(yaw);
        e.setYBodyRot(yaw);
        e.setNoAi(true);
        e.setPersistenceRequired();
        level.addFreshEntity(e);
        staged.add(e);
        return e;
    }

    private static void view(ServerPlayer p, Vec3 eye, Vec3 at) {
        Vec3 from = eye.subtract(0, 1.62, 0);
        camFrom = from;
        camAt = at;
        look(p, from, at);
    }

    private static void look(ServerPlayer p, Vec3 from, Vec3 at) {
        p.teleportTo(p.serverLevel(), from.x, from.y, from.z, p.getYRot(), p.getXRot());
        p.lookAt(EntityAnchorArgument.Anchor.EYES, at);
    }

    private static Beat beat(String name, int length, BiConsumer<ServerPlayer, Vec3> server) {
        return new Beat(name, server, false, length);
    }

    private static Beat hud(String name, int length, BiConsumer<ServerPlayer, Vec3> server) {
        return new Beat(name, server, true, length);
    }

    private static void broken(MinecraftServer server, int n, boolean risen) {
        SealSavedData d = SealSavedData.get(server);
        d.start(WorldClock.day(server));
        d.setBroken(n, WorldClock.day(server));
        d.setRisen(risen);
        d.setEcho(false);
        SealWorld.syncAll(server);
    }

    // --- SN_PREVIEW=seals_sky ----------------------------------------------------------------------------------------------------

    private static List<Beat> sky() {
        List<Beat> out = new ArrayList<>();
        // The moon at 18000 stands overhead; look up and a little east.
        out.add(beat("stage2_moon", 120, (p, c) -> {
            broken(p.server, 6, false);
            view(p, c.add(0, 1.7, 0), c.add(8, 40, 0));
        }));
        out.add(beat("stage2_horizon", 60, (p, c) -> view(p, c.add(0, 1.7, 0), c.add(0, 4, -40))));
        out.add(beat("stage3_blood_rain", 140, (p, c) -> {
            broken(p.server, 9, false);
            p.serverLevel().setWeatherParameters(0, 6000, true, false);
            view(p, c.add(0, 1.7, 0), c.add(0, 1, -12));
        }));
        out.add(beat("stage4_stars", 200, (p, c) -> {
            broken(p.server, SealRules.TOTAL, true);
            p.serverLevel().setWeatherParameters(12000, 0, false, false);
            view(p, c.add(0, 1.7, 0), c.add(20, 30, -20));
        }));
        return out;
    }

    // --- SN_PREVIEW=seal_events ---------------------------------------------------------------------------------------------------

    private static List<Beat> events() {
        List<Beat> out = new ArrayList<>();
        SealKind[] kinds = {SealKind.WITNESSES, SealKind.REAPERS, SealKind.SLAIN_ANGELS, SealKind.SACRIFICE, SealKind.BLOOD_WATER};
        for (SealKind k : kinds) {
            out.add(hud(k.id(), 80, (p, c) -> {
                SealEvents.endAll(p.server);
                broken(p.server, 3, false);
                BlockPos site = BlockPos.containing(c.add(0, 0, -6));
                SealEvents.startAt(p.serverLevel(), p, k, site);
                view(p, c.add(0, 4, 6), Vec3.atBottomCenterOf(site));
            }));
        }
        return out;
    }

    // --- SN_PREVIEW=uriel_model ---------------------------------------------------------------------------------------------------

    private static List<Beat> urielModel() {
        List<Beat> out = new ArrayList<>();
        List<Mob> holder = new ArrayList<>();
        out.add(beat("idle", 40, (p, c) -> {
            holder.add(mob(p.serverLevel(), AllEntities.URIEL.get(), c, 0));
            view(p, c.add(0, 2.2, 4.5), c.add(0, 1.2, 0));
        }));
        for (String clip : UrielAssets.TRIGGERED) {
            out.add(beat(clip, 40, (p, c) -> {
                if (!holder.isEmpty() && holder.getFirst() instanceof software.bernie.geckolib.animatable.GeoEntity g) g.triggerAnim("action", clip);
                view(p, c.add(2.2, 2.0, 4.0), c.add(0, 1.2, 0));
            }));
        }
        out.add(beat("rebels", 40, (p, c) -> {
            for (int i = 0; i < 3; i++) mob(p.serverLevel(), AllEntities.REBEL_ANGEL.get(), c.add(-3 + i * 3, 0, -3), 0);
            mob(p.serverLevel(), AllEntities.WITNESS.get(), c.add(-3, 0, 2), 0);
            mob(p.serverLevel(), AllEntities.SEAL_REAPER.get(), c.add(3, 0, 2), 0);
            view(p, c.add(0, 3, 8), c.add(0, 1, -1));
        }));
        return out;
    }

    // --- SN_PREVIEW=uriel_fight ---------------------------------------------------------------------------------------------------

    private static List<Beat> urielFight() {
        List<Beat> out = new ArrayList<>();
        out.add(hud("emerge", 140, (p, c) -> {
            UrielSummoning.summon(p.serverLevel(), BlockPos.containing(c), null);
            view(p, c.add(0, 3, 10), c.add(0, 1.4, 0));
        }));
        for (int i = 0; i < 4; i++) {
            int k = i;
            out.add(hud("fight_" + k, 60, (p, c) -> view(p, c.add(Math.cos(k) * 9, 3.5, Math.sin(k) * 9), c.add(0, 1.2, 0))));
        }
        return out;
    }

    // --- SN_PREVIEW=garrison ------------------------------------------------------------------------------------------------------

    private static List<Beat> garrison() {
        List<Beat> out = new ArrayList<>();
        GarrisonSite[] site = new GarrisonSite[1];
        out.add(beat("gate", 80, (p, c) -> {
            site[0] = GarrisonWorld.placeAt(p.serverLevel(), BlockPos.containing(c), 0);
            Vec3 entry = site[0].floor(GarrisonAnchors.ENTRY);
            view(p, entry.add(0, 1.7, -3), site[0].floor(GarrisonAnchors.YARD).add(0, 2, 0));
        }));
        out.add(beat("above", 50, (p, c) -> view(p, site[0].floor(GarrisonAnchors.YARD).add(0, 40, -26), site[0].floor(GarrisonAnchors.YARD))));
        out.add(beat("yard", 50, (p, c) -> view(p, site[0].floor(GarrisonAnchors.CASTIEL).add(0, 1.7, 0), site[0].floor(GarrisonAnchors.URIEL).add(0, 2, 6))));
        out.add(beat("chapel", 50, (p, c) -> {
            Vec3 u = site[0].floor(GarrisonAnchors.URIEL);
            view(p, u.add(0, 1.7, 9), u.add(0, 1.5, 13));
        }));
        return out;
    }
}
