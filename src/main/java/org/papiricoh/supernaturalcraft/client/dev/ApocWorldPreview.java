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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.apocworld.ApocWorldRegistry;
import org.papiricoh.supernaturalcraft.apocworld.ApocalypseWorld;
import org.papiricoh.supernaturalcraft.apocworld.army.AuSoldierAssets;
import org.papiricoh.supernaturalcraft.apocworld.camp.CampBuilder;
import org.papiricoh.supernaturalcraft.apocworld.camp.CampPlan;
import org.papiricoh.supernaturalcraft.apocworld.fortress.FortressAnchors;
import org.papiricoh.supernaturalcraft.apocworld.fortress.FortressSite;
import org.papiricoh.supernaturalcraft.apocworld.fortress.FortressWorld;
import org.papiricoh.supernaturalcraft.apocworld.rift.AuRifts;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import org.papiricoh.supernaturalcraft.entity.boss.aumichael.AuMichaelBalance;
import org.papiricoh.supernaturalcraft.entity.boss.aumichael.AuMichaelEntity;
import org.papiricoh.supernaturalcraft.entity.boss.aumichael.AuMichaelSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * v0.41 preview scenes for the Apocalypse World and AU Michael: {@code apoc_world} (the burned land and its sky from the landing),
 * {@code prisoner_camp} (a camp from its gate, from above, at the cages), {@code au_fortress} (the citadel from its road, its court,
 * the throne), {@code au_army} (the soldiers' and prisoners' clips) and {@code au_michael_fight} (his four phases and his flight).
 * Same shape as {@code ApocalypsePreview}: it runs only when every name in {@code SN_PREVIEW} is one of its {@link #NAMES};
 * captures in {@code runs/client/screenshots/sn_<scene>_<beat>.png}. Everything is staged in the burned world (or its stand-in, in
 * a world without it), far out from its landing.
 */
final class ApocWorldPreview {

    static final List<String> NAMES = List.of("apoc_world", "prisoner_camp", "au_fortress", "au_army", "au_michael_fight");
    private static final int FOV = 70;
    /** Where each scene is staged, from the burned world's heart (east, one scene after another). */
    private static final int OFFSET = 900, STEP = 220;

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

    private ApocWorldPreview() {
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
        if (base == null || t < 40) return true;
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
                case "apoc_world" -> land();
                case "prisoner_camp" -> camp();
                case "au_fortress" -> fortress();
                case "au_army" -> army();
                default -> fight();
            };
        }
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

    // --- staging ---------------------------------------------------------------------------------------------------------------

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static ServerLevel au(MinecraftServer server) {
        return ApocalypseWorld.level(server);
    }

    private static void stage(MinecraftServer server, String which) {
        ServerPlayer p = player(server);
        p.setGameMode(which.equals("au_michael_fight") ? GameType.CREATIVE : GameType.SPECTATOR);
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.removeAllEffects();
        ServerLevel level = au(server);
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        if (!ApocalypseWorld.real(level)) {
            level.setDayTime(10400);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            level.setWeatherParameters(12000, 0, false, false);
        }
        BlockPos o = ApocalypseWorld.origin(level);
        int x = o.getX() + (which.equals("apoc_world") ? 0 : OFFSET + NAMES.indexOf(which) * STEP), z = o.getZ();
        level.getChunk(x >> 4, z >> 4);
        BlockPos g = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        for (Entity old : level.getEntitiesOfClass(Entity.class, new AABB(g).inflate(80), e -> !(e instanceof Player))) old.discard();
        Vec3 b = Vec3.atBottomCenterOf(g);
        p.teleportTo(level, b.x, b.y + 3, b.z + 8, 180, 15);
        base = b;
    }

    private static void unstage(MinecraftServer server) {
        staged.forEach(Entity::discard);
        staged.clear();
        ServerLevel level = au(server);
        for (Entity e : level.getEntitiesOfClass(Entity.class, new AABB(BlockPos.containing(base)).inflate(100),
                e -> e instanceof LuciferEntity || e instanceof TelegraphMarker || e instanceof Mob)) e.discard();
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        AuRifts.collapseAll(level);
        player(server).setGameMode(GameType.SPECTATOR);
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

    private static <T extends Mob> T mob(ServerLevel level, net.minecraft.world.entity.EntityType<T> type, Vec3 at, float yaw) {
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

    // --- SN_PREVIEW=apoc_world -------------------------------------------------------------------------------------------------

    private static List<Beat> land() {
        List<Beat> out = new ArrayList<>();
        out.add(beat("west_sun", 60, (p, c) -> view(p, c.add(0, 4, 0), c.add(-60, 10, 0))));
        out.add(beat("east", 60, (p, c) -> view(p, c.add(0, 4, 0), c.add(60, 0, 10))));
        out.add(beat("high", 60, (p, c) -> view(p, c.add(0, 40, 0), c.add(80, 0, 80))));
        out.add(beat("sky", 60, (p, c) -> view(p, c.add(0, 3, 0), c.add(10, 60, 0))));
        out.add(beat("rift", 60, (p, c) -> {
            ApocalypseWorld.openRift(p.serverLevel(), BlockPos.containing(c.add(0, 0, -6)), 400);
            view(p, c.add(3, 2, 2), c.add(0, 2, -6));
        }));
        return out;
    }

    // --- SN_PREVIEW=prisoner_camp ----------------------------------------------------------------------------------------------

    private static List<Beat> camp() {
        List<Beat> out = new ArrayList<>();
        Vec3[] origin = new Vec3[1];
        out.add(beat("gate", 80, (p, c) -> {
            ServerLevel level = p.serverLevel();
            BlockPos o = BlockPos.containing(c);
            CampBuilder.placeCamp(level, o, 0);
            for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(o).inflate(20))) {
                m.setNoAi(true);
                staged.add(m);
            }
            origin[0] = Vec3.atBottomCenterOf(o);
            view(p, origin[0].add(0, 3, -22), origin[0].add(0, 1, 0));
        }));
        out.add(beat("above", 60, (p, c) -> view(p, origin[0].add(14, 22, -14), origin[0])));
        out.add(beat("cages", 60, (p, c) -> view(p, origin[0].add(0, 2.2, -2), origin[0].add(0, 1, 3))));
        out.add(beat("checkpoint", 80, (p, c) -> {
            ServerLevel level = p.serverLevel();
            BlockPos o = BlockPos.containing(c.add(60, 0, 0));
            o = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, o);
            CampBuilder.build(level, CampBuilder.fullBox(o, 0), o, 1, CampPlan.plan(CampPlan.Kind.CHECKPOINT, 0));
            for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(o).inflate(16))) {
                m.setNoAi(true);
                staged.add(m);
            }
            view(p, Vec3.atBottomCenterOf(o).add(-6, 4, -16), Vec3.atBottomCenterOf(o));
        }));
        return out;
    }

    // --- SN_PREVIEW=au_fortress ------------------------------------------------------------------------------------------------

    private static List<Beat> fortress() {
        List<Beat> out = new ArrayList<>();
        FortressSite[] site = new FortressSite[1];
        out.add(beat("afar", 100, (p, c) -> {
            ServerLevel level = p.serverLevel();
            BlockPos o = BlockPos.containing(c);
            site[0] = FortressWorld.placeAt(level, o.offset(0, 1, 0), 0);
            Vec3 entry = site[0].floor(FortressAnchors.ENTRY);
            view(p, entry.add(10, 18, -50), Vec3.atCenterOf(site[0].throne()).add(0, 12, 0));
        }));
        out.add(beat("road", 60, (p, c) -> view(p, site[0].floor(FortressAnchors.ENTRY).add(0, 2, 0), Vec3.atCenterOf(site[0].centre()).add(0, 4, 0))));
        out.add(beat("court", 60, (p, c) -> view(p, Vec3.atCenterOf(site[0].centre()).add(0, 3, -18), Vec3.atCenterOf(site[0].throne()))));
        out.add(beat("throne", 60, (p, c) -> view(p, Vec3.atCenterOf(site[0].throne()).add(-3, 2, -6), Vec3.atCenterOf(site[0].throne()).add(0, 1, 0))));
        return out;
    }

    // --- SN_PREVIEW=au_army ----------------------------------------------------------------------------------------------------

    private static List<Beat> army() {
        List<Beat> out = new ArrayList<>();
        List<Mob> line = new ArrayList<>();
        out.add(beat("line_up", 60, (p, c) -> {
            ServerLevel level = p.serverLevel();
            for (int i = 0; i < 4; i++) {
                var s = mob(level, ApocWorldRegistry.AU_ANGEL_SOLDIER.get(), c.add(-3 + i * 1.6, 0, 0), 0);
                s.setVessel(i);
                line.add(s);
            }
            for (int i = 0; i < 4; i++) {
                var pr = mob(level, ApocWorldRegistry.AU_PRISONER.get(), c.add(-3 + i * 1.6, 0, 3), 0);
                pr.setLook(i);
            }
            view(p, c.add(0, 1.9, 8), c.add(0, 1, 1));
        }));
        for (String clip : AuSoldierAssets.TRIGGERED) {
            if (clip.equals("death")) continue;
            out.add(beat(clip, 40, (p, c) -> line.forEach(m -> ((software.bernie.geckolib.animatable.GeoEntity) m).triggerAnim("action", clip))));
        }
        return out;
    }

    // --- SN_PREVIEW=au_michael_fight -------------------------------------------------------------------------------------------

    private static List<Beat> fight() {
        List<Beat> out = new ArrayList<>();
        AuMichaelEntity[] boss = new AuMichaelEntity[1];
        out.add(hud("entrance", 80, (p, c) -> {
            if (AuMichaelSummoning.summon(p.serverLevel(), BlockPos.containing(c), p) instanceof AuMichaelEntity m) boss[0] = m;
            view(p, c.add(0, 2, -6), c.add(0, 1.6, 4.5));
        }));
        out.add(hud("phase1", 120, (p, c) -> view(p, c.add(4, 2, -4), c.add(0, 1.6, 4.5))));
        for (int ph = 2; ph <= AuMichaelBalance.PHASES; ph++) {
            final int phase = ph;
            out.add(hud("phase" + phase, 150, (p, c) -> {
                if (boss[0] != null && boss[0].isAlive()) boss[0].beginTransition(phase);
                view(p, c.add(-5, 2.5, -5), c.add(0, phase == AuMichaelBalance.PHASES ? 6 : 1.6, 4.5));
            }));
        }
        out.add(hud("flight", 60, (p, c) -> {
            if (boss[0] != null && boss[0].isAlive()) BossHealthGuard.set(boss[0], 1f);
            view(p, c.add(5, 2, -3), c.add(0, 2, 4.5));
        }));
        out.add(hud("jack", AuMichaelBalance.TURN_AT, (p, c) -> {
        }));
        out.add(hud("gone", AuMichaelBalance.GONE_AT - AuMichaelBalance.TURN_AT + 10, (p, c) -> {
        }));
        return out;
    }
}
