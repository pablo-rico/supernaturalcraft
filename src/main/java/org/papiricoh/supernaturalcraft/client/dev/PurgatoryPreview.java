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
import org.papiricoh.supernaturalcraft.client.eve.ClientEveFx;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicFxManager;
import org.papiricoh.supernaturalcraft.entity.alpha.AlphaShapeshifterEntity;
import org.papiricoh.supernaturalcraft.entity.boss.eve.EveAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.eve.EveEntity;
import org.papiricoh.supernaturalcraft.entity.boss.eve.EveSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.purgatory.PurgatoryWorld;
import org.papiricoh.supernaturalcraft.purgatory.glade.EveGladePlan;
import org.papiricoh.supernaturalcraft.purgatory.glade.EveGladeWorld;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Preview scenes for Purgatory (v0.39): {@code SN_PREVIEW=eve_model} (Eve in each phase's look and her clips),
 * {@code eve_fight} (a real fight with her HUD), {@code alphas} (the three side by side and their moves) and {@code purgatory}
 * (the landing and the woods round it, then her glade from its way in and from above). Captures in
 * {@code runs/client/screenshots/sn_<scene>_<beat>.png}.
 */
final class PurgatoryPreview {

    static final List<String> NAMES = List.of("eve_model", "eve_fight", "alphas", "purgatory");
    private static final int FOV = 70;
    private static final int X = 9200, Z = 7600;

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

    private PurgatoryPreview() {
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
            MagicFxManager.clear();
            ClientEveFx.clear();
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
                case "eve_model" -> eveModel();
                case "eve_fight" -> eveFight();
                case "alphas" -> alphas();
                default -> purgatory();
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
        if (which.equals("purgatory")) {
            ServerLevel level = PurgatoryWorld.level(server);
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            Vec3 land = PurgatoryWorld.landing(level);
            p.teleportTo(level, land.x, land.y + 1.6, land.z, 0, 5);
            base = land;
            return;
        }
        ServerLevel level = server.overworld();
        org.papiricoh.supernaturalcraft.eclipse.Eclipses.end(level);
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        level.setDayTime(which.equals("alphas") ? 18000 : 13000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.setWeatherParameters(12000, 0, false, false);
        int x = X + NAMES.indexOf(which) * 160;
        level.getChunk(x >> 4, Z >> 4);
        BlockPos g = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, Z));
        for (Entity old : level.getEntitiesOfClass(Entity.class, new AABB(g).inflate(60), e -> !(e instanceof Player))) old.discard();
        floor(level, g, 26, 26);
        Vec3 b = Vec3.atBottomCenterOf(g);
        p.teleportTo(level, b.x, b.y + 3, b.z + 8, 180, 15);
        base = b;
    }

    private static void unstage(MinecraftServer server) {
        staged.forEach(Entity::discard);
        staged.clear();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity e : level.getEntitiesOfClass(Entity.class, new AABB(BlockPos.containing(base)).inflate(80),
                    e -> e instanceof LuciferEntity || e instanceof TelegraphMarker)) e.discard();
            for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
            ArenaSavedData.get(level).removeClosed();
        }
        Minecraft.getInstance().execute(() -> {
            MagicFxManager.clear();
            ClientEveFx.clear();
        });
        player(server).setGameMode(GameType.SPECTATOR);
    }

    private static void floor(ServerLevel level, BlockPos g, int rx, int rz) {
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dz = -rz; dz <= rz; dz++) {
                int h = Math.floorMod(dx * 7 + dz * 13, 11);
                level.setBlockAndUpdate(g.offset(dx, -1, dz), h == 0 ? Blocks.PODZOL.defaultBlockState() : h < 3 ? Blocks.MOSS_BLOCK.defaultBlockState()
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

    // --- SN_PREVIEW=eve_model --------------------------------------------------------------------------------------------------

    private static EveEntity eve;

    private static List<Beat> eveModel() {
        List<Beat> out = new ArrayList<>();
        for (int look = 1; look <= 4; look++) {
            int l = look;
            out.add(beat("look" + l, 50, (p, c) -> {
                if (eve == null || eve.isRemoved()) eve = mob(p.serverLevel(), AllEntities.EVE.get(), c, 0);
                eve.forceLook(l);
                view(p, c.add(0, 1.6, 3.4), c.add(0, 1.1, 0));
            }));
        }
        out.add(beat("side", 40, (p, c) -> view(p, c.add(3.2, 1.6, 0.5), c.add(0, 1.1, 0))));
        for (String clip : List.of("lash", "root_grasp", "birth", "roach_swarm", "call", "root_field", "thorn_ring")) {
            out.add(beat(clip, 32, (p, c) -> {
                view(p, c.add(1.5, 1.7, 3.6), c.add(0, 1.0, 0));
                eve.triggerAnim("action", clip);
            }));
        }
        out.add(beat("end", 4, (p, c) -> {
            eve.discard();
            eve = null;
        }));
        return out;
    }

    // --- SN_PREVIEW=eve_fight --------------------------------------------------------------------------------------------------

    private static List<Beat> eveFight() {
        List<Beat> out = new ArrayList<>();
        out.add(hud("emerge", 90, (p, c) -> {
            p.setGameMode(GameType.SURVIVAL);
            p.getAbilities().invulnerable = true;
            p.onUpdateAbilities();
            eve = EveSummoning.summon(p.serverLevel(), BlockPos.containing(c), p);
            view(p, c.add(0, 1.7, 10), c.add(0, 1.2, 4.5));
        }));
        out.add(hud("lash", 30, (p, c) -> eve.queue(EveAttacks.Lash::new)));
        out.add(hud("roots", 40, (p, c) -> eve.queue(EveAttacks.RootGrasp::new)));
        out.add(hud("birth", 45, (p, c) -> eve.queue(EveAttacks.Birth::new)));
        out.add(hud("swarm", 50, (p, c) -> {
            eve.forceLook(2);
            eve.queue(EveAttacks.Swarm::new);
        }));
        out.add(hud("thorns", 45, (p, c) -> {
            eve.forceLook(3);
            eve.queue(EveAttacks.ThornRing::new);
        }));
        out.add(hud("field", 60, (p, c) -> {
            eve.forceLook(4);
            eve.queue(EveAttacks.RootField::new);
        }));
        out.add(hud("hud", 20, (p, c) -> view(p, c.add(0, 4, 14), c.add(0, 1, 2))));
        out.add(beat("end", 4, (p, c) -> {
            p.getAbilities().invulnerable = false;
            p.onUpdateAbilities();
            if (eve != null) eve.discard();
            eve = null;
        }));
        return out;
    }

    // --- SN_PREVIEW=alphas -----------------------------------------------------------------------------------------------------

    private static List<Beat> alphas() {
        List<Beat> out = new ArrayList<>();
        List<Mob> three = new ArrayList<>();
        out.add(beat("lineup", 50, (p, c) -> {
            three.add(mob(p.serverLevel(), AllEntities.ALPHA_VAMPIRE.get(), c.add(-2.5, 0, 0), 0));
            three.add(mob(p.serverLevel(), AllEntities.ALPHA_WEREWOLF.get(), c, 0));
            three.add(mob(p.serverLevel(), AllEntities.ALPHA_SHAPESHIFTER.get(), c.add(2.5, 0, 0), 0));
            view(p, c.add(0, 1.9, 6.5), c.add(0, 1.3, 0));
        }));
        String[][] moves = {{"0", "lunge"}, {"0", "drain"}, {"0", "command"}, {"1", "leap"}, {"1", "howl"}, {"1", "slam"}, {"2", "steal"},
                {"2", "slough"}};
        for (String[] m : moves) {
            out.add(beat(m[1], 30, (p, c) -> {
                Mob a = three.get(Integer.parseInt(m[0]));
                view(p, a.position().add(1.2, 1.8, 3.6), a.position().add(0, 1.2, 0));
                ((software.bernie.geckolib.animatable.GeoEntity) a).triggerAnim("action", m[1]);
            }));
        }
        out.add(beat("stolen", 30, (p, c) -> {
            if (three.get(2) instanceof AlphaShapeshifterEntity s) s.setDisguise("player:" + p.getUUID());
            view(p, three.get(2).position().add(0, 1.8, 3.5), three.get(2).position().add(0, 1.2, 0));
        }));
        out.add(beat("end", 4, (p, c) -> three.clear()));
        return out;
    }

    // --- SN_PREVIEW=purgatory --------------------------------------------------------------------------------------------------

    private static List<Beat> purgatory() {
        List<Beat> out = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int k = i;
            out.add(beat("landing_" + k, 40, (p, c) -> {
                double a = Math.PI / 2 * k;
                view(p, c.add(0, 1.7, 0), c.add(Math.cos(a) * 10, 1.4, Math.sin(a) * 10));
            }));
        }
        out.add(beat("above", 50, (p, c) -> view(p, c.add(0, 40, 0), c.add(30, 0, 30))));
        out.add(beat("glade_entry", 80, (p, c) -> {
            ServerLevel level = PurgatoryWorld.level(p.server);
            EveGladeWorld.buildNow(level);
            Vec3 entry = EveGladeWorld.floor(level, EveGladePlan.ENTRY);
            Vec3 throne = EveGladeWorld.floor(level, EveGladePlan.THRONE);
            p.teleportTo(level, entry.x, entry.y, entry.z, 180, 0);
            view(p, entry.add(0, 1.7, 0), throne.add(0, 2, 0));
        }));
        out.add(beat("glade_above", 50, (p, c) -> {
            ServerLevel level = PurgatoryWorld.level(p.server);
            Vec3 centre = EveGladeWorld.floor(level, EveGladePlan.CENTRE);
            view(p, centre.add(0, 34, 26), centre);
        }));
        out.add(beat("glade_throne", 50, (p, c) -> {
            ServerLevel level = PurgatoryWorld.level(p.server);
            Vec3 throne = EveGladeWorld.floor(level, EveGladePlan.THRONE);
            view(p, throne.add(0, 2.2, 7), throne.add(0, 2.5, -2));
        }));
        return out;
    }
}
