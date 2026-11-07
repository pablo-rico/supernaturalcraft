package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
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
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.empty.EmptyWorld;
import org.papiricoh.supernaturalcraft.entity.boss.asmodeus.AsmodeusSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.michaelvessel.MichaelVesselSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.shadow.ShadowSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.soulless.SoullessJackSummoning;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRegistry;
import org.papiricoh.supernaturalcraft.nephilim.michael.Mindscape;
import software.bernie.geckolib.animatable.GeoEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * v0.41 preview scenes for Jack, Asmodeus, season 14, the Empty and the Shadow: {@code jack_model} (the companion and soulless
 * Jack side by side, through their clips), {@code asmodeus_model}, {@code michael_vessel_model}, {@code shadow_model} (each rig
 * through its clips), {@code asmodeus_fight}, {@code michael_vessel_fight}, {@code soulless_jack_fight}, {@code shadow_fight}
 * (a real fight with the HUD), {@code mindscape} (inside a hunter's head) and {@code the_empty} (its floor and sleepers). Same
 * shape as {@code ApocalypsePreview}: it runs only when every name in {@code SN_PREVIEW} is one of its {@link #NAMES};
 * captures in {@code runs/client/screenshots/sn_<scene>_<beat>.png}.
 */
final class NephilimPreview {

    static final List<String> NAMES = List.of("jack_model", "asmodeus_model", "michael_vessel_model", "shadow_model", "asmodeus_fight",
            "michael_vessel_fight", "soulless_jack_fight", "shadow_fight", "mindscape", "the_empty");
    private static final int FOV = 70;
    private static final int X = 15600, Z = -9600;

    private record Beat(String name, BiConsumer<ServerPlayer, Vec3> server, boolean hud, int length) {
    }

    private static List<String> queue;
    private static String scene;
    private static int t, beatIndex, beatStart;
    private static volatile Vec3 base;
    private static List<Beat> beats;
    private static final List<Entity> staged = new ArrayList<>();
    private static volatile Vec3 camFrom, camAt;
    private static int savedFov = -1;
    private static double savedFovEffect;

    private NephilimPreview() {
    }

    /** @return whether this class ran the tick (it owns the current scene list) */
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
        if (beats == null) beats = beatsOf(scene);
        if (beatIndex >= beats.size()) {
            mc.setScreen(null);
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

    private static List<Beat> beatsOf(String which) {
        return switch (which) {
            case "jack_model" -> model(List.of("idle", "smite", "shield", "burn_grace", "nova"), NephilimRegistry.JACK.get(), NephilimRegistry.SOULLESS_JACK.get());
            case "asmodeus_model" -> model(List.of("idle", "cane_strike", "grace_drink", "grace_blast", "shapeshift"), NephilimRegistry.ASMODEUS.get(), null);
            case "michael_vessel_model" -> model(List.of("idle", "snap", "choke", "wing_flare", "smite"), NephilimRegistry.MICHAEL_VESSEL.get(), null);
            case "shadow_model" -> model(List.of("idle", "lash", "engulf", "lull", "roar"), NephilimRegistry.SHADOW.get(), null);
            case "asmodeus_fight" -> fight((p, c) -> AsmodeusSummoning.summon(p.serverLevel(), BlockPos.containing(c), p));
            case "michael_vessel_fight" -> fight((p, c) -> MichaelVesselSummoning.summon(p.serverLevel(), BlockPos.containing(c), p));
            case "soulless_jack_fight" -> fight((p, c) -> SoullessJackSummoning.summon(p.serverLevel(), BlockPos.containing(c), p));
            case "shadow_fight" -> fight((p, c) -> ShadowSummoning.summon(p.serverLevel(), BlockPos.containing(c), p));
            case "mindscape" -> List.of(
                    new Beat("enter", (p, c) -> {
                        p.setGameMode(GameType.CREATIVE);
                        Mindscape.enter(p);
                    }, true, 80),
                    new Beat("room", (p, c) -> view(p, Vec3.atCenterOf(Mindscape.ORIGIN.offset(Mindscape.ROOM - 2, 5, Mindscape.ROOM - 2)),
                            Vec3.atCenterOf(Mindscape.ORIGIN.offset(0, 1, -Mindscape.ROOM + 3))), true, 60));
            default -> List.of(
                    new Beat("arrive", (p, c) -> EmptyWorld.send(p), false, 60),
                    new Beat("floor", (p, c) -> {
                        Vec3 at = Vec3.atCenterOf(EmptyWorld.centre(p.serverLevel()));
                        view(p, at.add(18, 10, 18), at);
                    }, false, 60),
                    new Beat("sleepers", (p, c) -> {
                        Vec3 at = Vec3.atCenterOf(EmptyWorld.centre(p.serverLevel()));
                        view(p, at.add(4, 3, 14), at.add(0, 0, 9));
                    }, false, 60),
                    new Beat("home", (p, c) -> EmptyWorld.home(p), false, 20));
        };
    }

    /** A rig through its clips: front, then three-quarters; a second rig beside it if given. */
    private static List<Beat> model(List<String> clips, EntityType<? extends Mob> type, EntityType<? extends Mob> twin) {
        List<Beat> out = new ArrayList<>();
        out.add(new Beat("stage", (p, c) -> {
            mob(p.serverLevel(), type, c, 0);
            if (twin != null) mob(p.serverLevel(), twin, c.add(-2.2, 0, 0), 0);
            view(p, c.add(twin == null ? 0 : -1.1, 1.6, 4.6), c.add(twin == null ? 0 : -1.1, 1.0, 0));
        }, false, 40));
        for (String clip : clips) {
            out.add(new Beat(clip, (p, c) -> {
                for (Entity e : staged) if (e instanceof GeoEntity g) g.triggerAnim("action", clip);
            }, false, 26));
        }
        out.add(new Beat("three_quarters", (p, c) -> view(p, c.add(3.4, 1.8, 3.4), c.add(twin == null ? 0 : -1.1, 1.0, 0)), false, 40));
        return out;
    }

    private static List<Beat> fight(BiConsumer<ServerPlayer, Vec3> start) {
        return List.of(
                new Beat("rise", (p, c) -> {
                    p.setGameMode(GameType.CREATIVE);
                    start.accept(p, c);
                    view(p, c.add(0, 2.2, 9), c.add(0, 1.2, 0));
                }, true, 120),
                new Beat("fight", (p, c) -> view(p, c.add(6, 3.5, 7), c.add(0, 1.2, 0)), true, 160));
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
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        level.setDayTime(which.contains("fight") ? 18000 : 6000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.setWeatherParameters(12000, 0, false, false);
        int x = X + NAMES.indexOf(which) * 200;
        level.getChunk(x >> 4, Z >> 4);
        BlockPos g = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, Z));
        for (Entity old : level.getEntitiesOfClass(Entity.class, new AABB(g).inflate(60), e -> !(e instanceof Player))) old.discard();
        floor(level, g, 28, 28);
        Vec3 b = Vec3.atBottomCenterOf(g);
        p.teleportTo(level, b.x, b.y + 3, b.z + 8, 180, 15);
        base = b;
    }

    private static void unstage(MinecraftServer server) {
        staged.forEach(Entity::discard);
        staged.clear();
        ServerLevel level = server.overworld();
        for (Entity e : level.getEntitiesOfClass(Entity.class, new AABB(BlockPos.containing(base)).inflate(90),
                e -> e instanceof LuciferEntity || e instanceof TelegraphMarker || e instanceof Mob)) e.discard();
        for (Entity e : level.getEntitiesOfClass(LuciferEntity.class, new AABB(Mindscape.ORIGIN).inflate(30))) e.discard();
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        ServerPlayer p = player(server);
        p.setGameMode(GameType.SPECTATOR);
    }

    private static void floor(ServerLevel level, BlockPos g, int rx, int rz) {
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dz = -rz; dz <= rz; dz++) {
                int h = Math.floorMod(dx * 7 + dz * 13, 11);
                level.setBlockAndUpdate(g.offset(dx, -1, dz), h == 0 ? Blocks.GRAVEL.defaultBlockState() : h < 3 ? Blocks.COARSE_DIRT.defaultBlockState()
                        : Blocks.STONE_BRICKS.defaultBlockState());
                for (int dy = 0; dy < 12; dy++) level.setBlockAndUpdate(g.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static void mob(ServerLevel level, EntityType<? extends Mob> type, Vec3 at, float yaw) {
        Mob e = type.create(level);
        if (e == null) return;
        e.moveTo(at.x, at.y, at.z, yaw, 0);
        e.setYHeadRot(yaw);
        e.setYBodyRot(yaw);
        e.setNoAi(true);
        e.setPersistenceRequired();
        level.addFreshEntity(e);
        staged.add(e);
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
}
