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
import org.papiricoh.supernaturalcraft.british.BritishRegistry;
import org.papiricoh.supernaturalcraft.british.agent.BritishAgentAssets;
import org.papiricoh.supernaturalcraft.british.agent.BritishAgentEntity;
import org.papiricoh.supernaturalcraft.british.birth.BirthAssets;
import org.papiricoh.supernaturalcraft.british.birth.BirthScene;
import org.papiricoh.supernaturalcraft.british.compound.CompoundAnchors;
import org.papiricoh.supernaturalcraft.british.safehouse.SafehouseAnchors;
import org.papiricoh.supernaturalcraft.british.site.BritishSites;
import org.papiricoh.supernaturalcraft.british.site.PlacedSite;
import org.papiricoh.supernaturalcraft.british.site.SiteBuilder;
import org.papiricoh.supernaturalcraft.client.british.BritishRenderers;
import org.papiricoh.supernaturalcraft.entity.boss.dagon.DagonAssets;
import org.papiricoh.supernaturalcraft.entity.boss.dagon.DagonEntity;
import org.papiricoh.supernaturalcraft.entity.boss.dagon.DagonSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.hess.HessAssets;
import org.papiricoh.supernaturalcraft.entity.boss.hess.HessEntity;
import org.papiricoh.supernaturalcraft.entity.boss.hess.HessSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.ketch.KetchAssets;
import org.papiricoh.supernaturalcraft.entity.boss.ketch.KetchEntity;
import org.papiricoh.supernaturalcraft.entity.boss.ketch.KetchSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * v0.41 preview scenes for season 12: {@code ketch_model}, {@code hess_model}, {@code dagon_model}, {@code british_agents},
 * {@code birth_model} (every rig's clips), {@code ketch_fight}, {@code hess_fight}, {@code dagon_fight} (real fights with their
 * HUDs), {@code british_hq} and {@code kelly_safehouse} (the two places, from outside, above and within) and {@code jack_birth}
 * (the birth scene with its camera). Same shape as {@code ApocalypsePreview}: it runs only when every name in
 * {@code SN_PREVIEW} is one of its {@link #NAMES}; captures in {@code runs/client/screenshots/sn_<scene>_<beat>.png}.
 */
final class BritishPreview {

    static final List<String> NAMES = List.of("ketch_model", "hess_model", "dagon_model", "british_agents", "birth_model", "ketch_fight",
            "hess_fight", "dagon_fight", "british_hq", "kelly_safehouse", "jack_birth");
    private static final int FOV = 70;
    private static final int X = 15200, Z = 11200;

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

    private BritishPreview() {
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
        if (f != null && t % 5 == 0 && !scene.equals("jack_birth")) {
            server.execute(() -> {
                ServerPlayer p = player(server);
                if (p.position().distanceToSqr(f) > 0.09) look(p, f, a);
                p.setDeltaMovement(Vec3.ZERO);
            });
        }
        if (beats == null) {
            beats = switch (scene) {
                case "ketch_model" -> model(BritishRegistry.KETCH.get(), KetchAssets.TRIGGERED, null);
                case "hess_model" -> model(BritishRegistry.HESS.get(), HessAssets.TRIGGERED, BritishRegistry.HESS_ENVOY.get());
                case "dagon_model" -> dagonModel();
                case "british_agents" -> agents();
                case "birth_model" -> birthModel();
                case "ketch_fight" -> ketchFight();
                case "hess_fight" -> hessFight();
                case "dagon_fight" -> dagonFight();
                case "british_hq" -> place(true);
                case "kelly_safehouse" -> place(false);
                default -> birth();
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

    private static void stage(MinecraftServer server, String which) {
        ServerPlayer p = player(server);
        p.setGameMode(GameType.SPECTATOR);
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.removeAllEffects();
        ServerLevel level = server.overworld();
        org.papiricoh.supernaturalcraft.eclipse.Eclipses.end(level);
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        BirthScene.reset();
        BritishRenderers.forceDagonPower(null);
        level.setDayTime(which.equals("ketch_fight") || which.equals("dagon_fight") || which.equals("jack_birth") ? 18000 : 6000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.setWeatherParameters(12000, 0, false, false);
        int x = X + NAMES.indexOf(which) * 220;
        level.getChunk(x >> 4, Z >> 4);
        BlockPos g = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, Z));
        for (Entity old : level.getEntitiesOfClass(Entity.class, new AABB(g).inflate(60), e -> !(e instanceof Player))) old.discard();
        if (!which.equals("british_hq") && !which.equals("kelly_safehouse") && !which.equals("jack_birth")) floor(level, g, 28, 28);
        Vec3 b = Vec3.atBottomCenterOf(g);
        p.teleportTo(level, b.x, b.y + 3, b.z + 8, 180, 15);
        base = b;
    }

    private static void unstage(MinecraftServer server) {
        staged.forEach(Entity::discard);
        staged.clear();
        ServerLevel level = server.overworld();
        BirthScene.reset();
        BritishRenderers.forceDagonPower(null);
        for (Entity e : level.getEntitiesOfClass(Entity.class, new AABB(BlockPos.containing(base)).inflate(90),
                e -> e instanceof LuciferEntity || e instanceof TelegraphMarker || e instanceof Mob)) e.discard();
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        player(server).setGameMode(GameType.SPECTATOR);
    }

    private static void floor(ServerLevel level, BlockPos g, int rx, int rz) {
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dz = -rz; dz <= rz; dz++) {
                int h = Math.floorMod(dx * 7 + dz * 13, 11);
                level.setBlockAndUpdate(g.offset(dx, -1, dz), h == 0 ? Blocks.GRAVEL.defaultBlockState() : h < 3 ? Blocks.COARSE_DIRT.defaultBlockState()
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

    private static void play(Mob m, String clip) {
        if (m instanceof software.bernie.geckolib.animatable.GeoEntity g) g.triggerAnim("action", clip);
    }

    // --- the rigs ----------------------------------------------------------------------------------------------------------------

    /** Every triggered clip of {@code type}'s rig, and {@code also} (a second user of the rig) beside it. */
    private static List<Beat> model(EntityType<? extends Mob> type, List<String> clips, EntityType<? extends Mob> also) {
        List<Beat> out = new ArrayList<>();
        List<Mob> holder = new ArrayList<>();
        out.add(beat("front", 40, (p, c) -> {
            holder.add(mob(p.serverLevel(), type, c, 0));
            if (also != null) holder.add(mob(p.serverLevel(), also, c.add(1.6, 0, 0), 0));
            view(p, c.add(0.5, 2.0, 4.5), c.add(0.5, 1.1, 0));
        }));
        out.add(beat("back", 30, (p, c) -> view(p, c.add(-1.5, 2.2, -4.2), c.add(0, 1.1, 0))));
        for (String clip : clips) {
            out.add(beat(clip, 36, (p, c) -> {
                play(holder.getFirst(), clip);
                view(p, c.add(2.2, 2.0, 4.0), c.add(0, 1.1, 0));
            }));
        }
        return out;
    }

    private static List<Beat> dagonModel() {
        List<Beat> out = new ArrayList<>(model(BritishRegistry.DAGON.get(), DagonAssets.TRIGGERED, null));
        out.add(1, beat("power", 30, (p, c) -> {
            BritishRenderers.forceDagonPower(true);
            view(p, c.add(0, 2.0, 3.2), c.add(0, 1.6, 0));
        }));
        return out;
    }

    private static List<Beat> agents() {
        List<Beat> out = new ArrayList<>();
        List<Mob> holder = new ArrayList<>();
        out.add(beat("variants", 40, (p, c) -> {
            for (int v = 0; v < BritishAgentEntity.VARIANTS; v++) {
                BritishAgentEntity a = mob(p.serverLevel(), BritishRegistry.BRITISH_AGENT.get(), c.add((v - 1) * 1.4, 0, 0), 0);
                a.setVariant(v);
                holder.add(a);
            }
            view(p, c.add(0, 2.0, 5.0), c.add(0, 1.1, 0));
        }));
        for (String clip : BritishAgentAssets.TRIGGERED) {
            out.add(beat(clip, 34, (p, c) -> {
                for (Mob m : holder) play(m, clip);
                view(p, c.add(2.0, 2.0, 4.5), c.add(0, 1.1, 0));
            }));
        }
        return out;
    }

    private static List<Beat> birthModel() {
        List<Beat> out = new ArrayList<>();
        List<Mob> holder = new ArrayList<>();
        out.add(beat("pair", 40, (p, c) -> {
            holder.add(mob(p.serverLevel(), BritishRegistry.KELLY_KLINE.get(), c.add(-0.9, 0, 0), 0));
            holder.add(mob(p.serverLevel(), BritishRegistry.ROURKE_LUCIFER.get(), c.add(0.9, 0, 0), 0));
            view(p, c.add(0, 2.0, 4.5), c.add(0, 1.1, 0));
        }));
        for (String clip : BirthAssets.TRIGGERED) {
            out.add(beat("kelly_" + clip, 50, (p, c) -> {
                play(holder.getFirst(), clip);
                view(p, c.add(-0.9, 1.8, 3.2), c.add(-0.9, 0.9, 0));
            }));
        }
        for (String clip : List.of("reach", "thrown")) {
            out.add(beat("rourke_" + clip, 40, (p, c) -> {
                play(holder.get(1), clip);
                view(p, c.add(0.9, 2.0, 3.6), c.add(0.9, 1.2, 0));
            }));
        }
        return out;
    }

    // --- the fights --------------------------------------------------------------------------------------------------------------

    private static List<Beat> ketchFight() {
        List<Beat> out = new ArrayList<>();
        KetchEntity[] boss = new KetchEntity[1];
        out.add(hud("operative", 120, (p, c) -> {
            boss[0] = (KetchEntity) KetchSummoning.summon(p.serverLevel(), BlockPos.containing(c), null);
            view(p, c.add(0, 2.6, 12), c.add(0, 1.4, 5));
        }));
        out.add(hud("hound", 100, (p, c) -> {
            if (boss[0] != null) boss[0].beginTransition(2);
            view(p, c.add(6, 3, 12), c.add(0, 1.2, 5));
        }));
        out.add(hud("nothing_personal", 120, (p, c) -> {
            if (boss[0] != null) boss[0].beginTransition(3);
            view(p, c.add(-6, 3, 11), c.add(0, 1.2, 5));
        }));
        return out;
    }

    private static List<Beat> hessFight() {
        List<Beat> out = new ArrayList<>();
        HessEntity[] boss = new HessEntity[1];
        out.add(hud("director", 120, (p, c) -> {
            boss[0] = (HessEntity) HessSummoning.summon(p.serverLevel(), BlockPos.containing(c), null);
            view(p, c.add(0, 2.6, 12), c.add(0, 1.4, 5));
        }));
        out.add(hud("agents", 80, (p, c) -> {
            if (boss[0] != null) for (Vec3 s : boss[0].agentSpots()) boss[0].callAgent(p.serverLevel(), s);
            view(p, c.add(0, 6, 16), c.add(0, 1.2, 5));
        }));
        out.add(hud("ward", 100, (p, c) -> {
            if (boss[0] != null) {
                boss[0].beginTransition(2);
                boss[0].raiseWard(100);
            }
            view(p, c.add(6, 3, 12), c.add(0, 1.2, 5));
        }));
        out.add(hud("folio", 120, (p, c) -> {
            if (boss[0] != null) boss[0].beginTransition(3);
            view(p, c.add(-6, 4, 12), c.add(0, 1.2, 5));
        }));
        return out;
    }

    private static List<Beat> dagonFight() {
        List<Beat> out = new ArrayList<>();
        DagonEntity[] boss = new DagonEntity[1];
        out.add(hud("prince", 120, (p, c) -> {
            boss[0] = (DagonEntity) DagonSummoning.summon(p.serverLevel(), BlockPos.containing(c), null);
            view(p, c.add(0, 2.6, 12), c.add(0, 1.6, 5));
        }));
        out.add(hud("hellfire", 100, (p, c) -> {
            if (boss[0] != null) boss[0].beginTransition(2);
            view(p, c.add(6, 3, 12), c.add(0, 1.4, 5));
        }));
        out.add(hud("unbroken", 120, (p, c) -> {
            if (boss[0] != null) boss[0].beginTransition(3);
            view(p, c.add(-6, 3, 11), c.add(0, 1.8, 5));
        }));
        out.add(hud("death", 100, (p, c) -> {
            if (boss[0] != null) boss[0].defeatNow();
            view(p, c.add(0, 2.4, 10), c.add(0, 1.4, 5));
        }));
        return out;
    }

    // --- the places --------------------------------------------------------------------------------------------------------------

    private static List<Beat> place(boolean compound) {
        List<Beat> out = new ArrayList<>();
        PlacedSite[] site = new PlacedSite[1];
        String entry = compound ? CompoundAnchors.ENTRY : SafehouseAnchors.ENTRY;
        String heart = compound ? CompoundAnchors.HESS : SafehouseAnchors.GUARD;
        out.add(beat("approach", 80, (p, c) -> {
            site[0] = SiteBuilder.placeAt(p.serverLevel(), compound ? BritishSites.COMPOUND : BritishSites.SAFEHOUSE, BlockPos.containing(c), 0);
            view(p, site[0].floor(entry).add(0, 1.8, -6), site[0].floor(heart).add(0, 3, 0));
        }));
        out.add(beat("above", 50, (p, c) -> view(p, site[0].floor(heart).add(0, 42, -32), site[0].floor(heart))));
        out.add(beat("entry", 50, (p, c) -> view(p, site[0].floor(entry).add(0, 1.7, 0), site[0].floor(heart).add(0, 1, 0))));
        if (compound) {
            out.add(beat("war_room", 60, (p, c) -> {
                Vec3 h = site[0].floor(CompoundAnchors.HESS);
                mob(p.serverLevel(), BritishRegistry.HESS.get(), h, 180);
                view(p, h.add(4, 2.6, -6), h.add(0, 1.2, 0));
            }));
        } else {
            out.add(beat("porch", 60, (p, c) -> {
                Vec3 g = site[0].floor(SafehouseAnchors.GUARD);
                mob(p.serverLevel(), BritishRegistry.DAGON.get(), g, 180);
                view(p, g.add(3, 2.4, -7), g.add(0, 1.4, 0));
            }));
            out.add(beat("nursery", 60, (p, c) -> {
                Vec3 cot = site[0].floor(SafehouseAnchors.COT);
                mob(p.serverLevel(), BritishRegistry.KELLY_KLINE.get(), cot, 0);
                view(p, site[0].floor(SafehouseAnchors.ROURKE).add(0, 1.7, 0), cot.add(0, 0.8, 0));
            }));
        }
        return out;
    }

    private static List<Beat> birth() {
        List<Beat> out = new ArrayList<>();
        PlacedSite[] site = new PlacedSite[1];
        out.add(beat("cabin", 40, (p, c) -> {
            site[0] = SiteBuilder.placeAt(p.serverLevel(), BritishSites.SAFEHOUSE, BlockPos.containing(c), 0);
            view(p, site[0].floor(SafehouseAnchors.ROURKE).add(0, 1.7, 0), site[0].floor(SafehouseAnchors.COT).add(0, 0.8, 0));
        }));
        out.add(beat("labor", 60, (p, c) -> BirthScene.start(p.serverLevel(), List.of(), site[0].floor(SafehouseAnchors.COT),
                site[0].floor(SafehouseAnchors.ROURKE), site[0].floor(SafehouseAnchors.RIFT))));
        out.add(beat("lucifer", 40, (p, c) -> {
        }));
        out.add(beat("light", 45, (p, c) -> {
        }));
        out.add(beat("thrown", 40, (p, c) -> {
        }));
        out.add(beat("fade", 70, (p, c) -> {
        }));
        return out;
    }
}
