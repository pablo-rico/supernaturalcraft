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
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.client.book.HunterBookScreen;
import org.papiricoh.supernaturalcraft.client.book.BookSection;
import org.papiricoh.supernaturalcraft.client.heaven.ClientHeaven;
import org.papiricoh.supernaturalcraft.client.heaven.MemoryTint;
import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.NaomiSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.ZachariahEntity;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.ZachariahSummoning;
import org.papiricoh.supernaturalcraft.entity.heaven.MemoryFigureEntity;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlot;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlotLayout;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlots;
import org.papiricoh.supernaturalcraft.heaven.roadhouse.Roadhouse;
import org.papiricoh.supernaturalcraft.heaven.roadhouse.RoadhouseLayout;
import org.papiricoh.supernaturalcraft.memory.Memories;
import org.papiricoh.supernaturalcraft.memory.Memory;
import org.papiricoh.supernaturalcraft.memory.MemoryKind;
import org.papiricoh.supernaturalcraft.memory.MemoryLog;
import org.papiricoh.supernaturalcraft.memory.MemoryStage;
import org.papiricoh.supernaturalcraft.network.HeavenFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * v0.18's scenes for the screenshot harness (chained from {@link DevPreview}); pictures land in
 * {@code runs/client/screenshots/sn_<scene>_<beat>.png}. Heaven scenes run in the Heaven dimension (or the Overworld stand-in
 * the world work uses without it); the rest well away from the origin (x = 2000).
 * <ul>
 *   <li>{@code heaven_plot}: the hunter's own plot written at once, its arrival wash and title, the landing, the memory lane, the
 *   wing and home doors, the stage, an overview from high above.</li>
 *   <li>{@code heaven_memories}: a log of made-up memories (a victory, a deal, a pet), one staged on the plot's stage: its tint and
 *   title, its figures and the focus figure's ring, the figure touched (toast), the leave fade.</li>
 *   <li>{@code roadhouse}: the hub written, Ash behind the bar, his menu open.</li>
 *   <li>{@code naomi_model}: Naomi, a guard, a clerk, the chair (empty and with a copy on it), kneeling and hostile training copies,
 *   then the HUD pieces (QTE strap, training test) by payload.</li>
 *   <li>{@code naomi_fight}: a real summoning, the player invulnerable in survival; phase 2 forced at tick 700, death at 1300.</li>
 *   <li>{@code zachariah_model}: Zachariah without and with wings, a clerk, memos, then the HUD pieces (form, approved, docket with
 *   a revision, termination notice) by payload.</li>
 *   <li>{@code zachariah_fight}: a real summoning; phases 2-4 forced at 600/1100/1600, death at 2100.</li>
 *   <li>{@code heaven_sky}: the plot's sky looking up (the noon sun), at the horizon (warm fog), down (the clouds below), then
 *   the same with a memory's tint.</li>
 *   <li>{@code crossroads_natural}: a wild crossroads placed directly in the Overworld, from four sides and above.</li>
 *   <li>{@code heaven_book}: a made-up log; the book's Memories tab (index, a gathered page with its scene, an unseen page) at
 *   GUI scales 2 and 3.</li>
 * </ul>
 */
public final class HeavenPreview {

    private static final List<String> SCENES = List.of("heaven_plot", "heaven_memories", "roadhouse", "naomi_model", "naomi_fight",
            "zachariah_model", "zachariah_fight", "heaven_sky", "crossroads_natural", "heaven_book");
    private static final int STEP = 50;

    private static int t = -1, beatIndex, beatStart;
    private static List<Beat> beats;
    private static final List<Entity> shown = new ArrayList<>();
    private static @Nullable LuciferEntity boss;
    private static Vec3 centre = Vec3.ZERO;

    /** One moment: what the server sets up, what the client does, the picture's name (taken at its end), how long it lasts. */
    private record Beat(String name, @Nullable BiConsumer<ServerPlayer, Vec3> server, @Nullable Runnable client, int length) {
        Beat(String name, @Nullable BiConsumer<ServerPlayer, Vec3> server, @Nullable Runnable client) {
            this(name, server, client, STEP);
        }
    }

    private HeavenPreview() {
    }

    /** @return true while one of its scenes is running (the harness then does nothing else) */
    public static boolean tick(Minecraft mc) {
        String scene = System.getenv("SN_PREVIEW");
        if (scene == null || !SCENES.contains(scene.trim())) return false;
        scene = scene.trim();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) return true;
        t++;
        if (scene.endsWith("_fight")) return fight(mc, server, scene);
        if (t == 0) {
            beats = switch (scene) {
                case "heaven_plot" -> plot(mc);
                case "heaven_memories" -> memories(mc);
                case "roadhouse" -> roadhouse(mc);
                case "naomi_model" -> naomiModel(mc);
                case "zachariah_model" -> zachariahModel(mc);
                case "heaven_sky" -> sky(mc);
                case "crossroads_natural" -> crossroads(mc);
                default -> book(mc);
            };
            return true;
        }
        if (t < 20) return true;
        if (beatIndex >= beats.size()) {
            if (t - beatStart == 5) {
                mc.setScreen(null);
                server.execute(HeavenPreview::clearShown);
            }
            if (t - beatStart == 15) mc.stop();
            return true;
        }
        Beat b = beats.get(beatIndex);
        if (beatStart == 0) beatStart = t;
        int local = t - beatStart;
        String name = scene;
        if (local == 0 && b.server() != null) server.execute(() -> b.server().accept(server.getPlayerList().getPlayers().getFirst(), centre));
        if (local == 2 && b.client() != null) b.client().run();
        if (local == b.length() - 2) Screenshot.grab(mc.gameDirectory, "sn_" + name + "_" + b.name() + ".png", mc.getMainRenderTarget(), m -> {
        });
        if (local >= b.length()) {
            beatIndex++;
            beatStart = t + 1;
        }
        return true;
    }

    // --- helpers -------------------------------------------------------------------------------------------------------

    private static void clearShown() {
        Minecraft.getInstance().execute(ClientHeaven::clear);
        for (Entity e : shown) e.discard();
        shown.clear();
        boss = null;
    }

    private static void view(ServerPlayer p, ServerLevel level, Vec3 from, Vec3 at) {
        p.teleportTo(level, from.x, from.y, from.z, p.getYRot(), p.getXRot());
        p.lookAt(EntityAnchorArgument.Anchor.EYES, at);
    }

    private static void camera(Minecraft mc, CameraType type, boolean hud) {
        mc.execute(() -> {
            mc.options.setCameraType(type);
            mc.options.hideGui = !hud;
        });
    }

    private static void spectator(ServerPlayer p) {
        p.setGameMode(GameType.SPECTATOR);
    }

    private static void survival(ServerPlayer p) {
        p.setGameMode(GameType.SURVIVAL);
        p.getAbilities().invulnerable = true;
        p.getAbilities().flying = false;
        p.onUpdateAbilities();
    }

    private static void fx(ServerPlayer p, HeavenFxPayload payload) {
        PacketDistributor.sendToPlayer(p, payload);
    }

    private static HeavenFxPayload fx(int entity, byte kind, int arg, int arg2, int duration, String text) {
        return new HeavenFxPayload(entity, kind, arg, arg2, Vec3.ZERO, duration, text);
    }

    private static <T extends Mob> T place(ServerLevel level, EntityType<T> type, Vec3 at, float yaw) {
        T e = type.create(level);
        e.moveTo(at.x, at.y, at.z, yaw, 0);
        e.setYHeadRot(yaw);
        e.setYBodyRot(yaw);
        e.setNoAi(true);
        e.setPersistenceRequired();
        level.addFreshEntity(e);
        shown.add(e);
        return e;
    }

    private static <T extends Entity> T placeEntity(ServerLevel level, EntityType<T> type, Vec3 at, float yaw) {
        T e = type.create(level);
        e.moveTo(at.x, at.y, at.z, yaw, 0);
        level.addFreshEntity(e);
        shown.add(e);
        return e;
    }

    /** A clean stage of quartz well away from the origin in the Overworld. */
    private static Vec3 overworldStage(ServerPlayer p, int x, int z, int radius) {
        ServerLevel level = p.server.overworld();
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        level.getChunk(x >> 4, z >> 4);
        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                level.setBlockAndUpdate(ground.offset(dx, -1, dz), Blocks.SMOOTH_QUARTZ.defaultBlockState());
                for (int dy = 0; dy < 8; dy++) level.setBlockAndUpdate(ground.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }
        }
        Vec3 c = Vec3.atBottomCenterOf(ground);
        p.teleportTo(level, c.x, c.y + 2, c.z + 6, 180, 0);
        return c;
    }

    /** The player's own plot, written at once; {@link #centre} = its origin. */
    private static HeavenPlot ownPlot(ServerPlayer p) {
        ServerLevel level = HeavenPlots.level(p.server);
        HeavenPlot plot = HeavenPlots.ensure(level, p);
        HeavenPlots.writeNow(level, plot);
        centre = Vec3.atBottomCenterOf(HeavenPlots.origin(level, plot));
        return plot;
    }

    private static Vec3 point(ServerLevel level, HeavenPlot plot, org.papiricoh.supernaturalcraft.layout.LayoutPoint at) {
        return Vec3.atBottomCenterOf(HeavenPlots.at(level, plot, at));
    }

    /** A few made-up memories (a victory, a deal, a pet, a sighting, a rank, a case), the first two gathered. */
    static MemoryLog sampleLog() {
        long now = System.currentTimeMillis();
        List<Memory> m = List.of(
                new Memory("boss:azazel", MemoryKind.BOSS_VICTORY, "azazel", "", 48000, now - 86400000L * 6, 0),
                new Memory("deal:1", MemoryKind.CROSSROADS_DEAL, "luck", "bowl", 96000, now - 86400000L * 5, 0),
                new Memory("pet:sample", MemoryKind.PET_LOST, "minecraft:wolf", "Rumsfeld", 120000, now - 86400000L * 4, 0),
                new Memory("seen:supernaturalcraft:hellhound", MemoryKind.FIRST_SIGHTING, "supernaturalcraft:hellhound", "", 130000, 0, 0),
                new Memory("rank:angel_2", MemoryKind.ASCENSION, "angel", "", 200000, now - 86400000L * 2, 2),
                new Memory("boss:lucifer", MemoryKind.BOSS_VICTORY, "lucifer", "", 260000, now - 86400000L, 0),
                new Memory("case:3", MemoryKind.CASE_SOLVED, "supernaturalcraft:vampire", "motel", 300000, now - 3600000L, 0));
        MemoryLog log = MemoryLog.EMPTY;
        for (Memory x : m) log = log.with(x);
        return log.withCollected("boss:azazel").withCollected("deal:1").withCollected("boss:lucifer");
    }

    // --- heaven_plot ---------------------------------------------------------------------------------------------------

    private static List<Beat> plot(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        HeavenPlot[] plot = new HeavenPlot[1];
        out.add(new Beat("arrive", (p, c) -> {
            spectator(p);
            plot[0] = ownPlot(p);
            ServerLevel level = HeavenPlots.level(p.server);
            Vec3 land = point(level, plot[0], HeavenPlotLayout.LANDING);
            view(p, level, land.add(0, 1.6, 4), land.add(0, 1.6, -10));
            fx(p, fx(-1, HeavenFxPayload.ARRIVE, 0, 0, 80, p.getGameProfile().getName()));
        }, () -> camera(mc, CameraType.FIRST_PERSON, true), 30));
        out.add(new Beat("landing", (p, c) -> {
            ServerLevel level = HeavenPlots.level(p.server);
            Vec3 land = point(level, plot[0], HeavenPlotLayout.LANDING);
            view(p, level, land.add(6, 4, 8), land.add(0, 1, -12));
        }, () -> camera(mc, CameraType.FIRST_PERSON, false), 80));
        String[] names = {"lane", "wing_door", "home_door", "stage", "road_door"};
        org.papiricoh.supernaturalcraft.layout.LayoutPoint[] at = {HeavenPlotLayout.SHRINES.getFirst(), HeavenPlotLayout.WING_DOOR,
                HeavenPlotLayout.HOME_DOOR, HeavenPlotLayout.STAGE_CENTER, HeavenPlotLayout.ROAD_DOOR};
        for (int i = 0; i < names.length; i++) {
            org.papiricoh.supernaturalcraft.layout.LayoutPoint pt = at[i];
            out.add(new Beat(names[i], (p, c) -> {
                ServerLevel level = HeavenPlots.level(p.server);
                Vec3 v = point(level, plot[0], pt);
                Vec3 from = v.add(v.subtract(c).normalize().scale(-9)).add(0, 5, 0);
                view(p, level, from, v.add(0, 1.5, 0));
            }, null));
        }
        out.add(new Beat("overview", (p, c) -> view(p, HeavenPlots.level(p.server), c.add(70, 70, 70), c), null, 80));
        out.add(new Beat("overview_far", (p, c) -> view(p, HeavenPlots.level(p.server), c.add(-150, 90, 140), c), null, 80));
        return out;
    }

    // --- heaven_memories -----------------------------------------------------------------------------------------------

    private static List<Beat> memories(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        out.add(new Beat("stage_enter", (p, c) -> {
            spectator(p);
            HeavenPlot plot = ownPlot(p);
            Memories.set(p, sampleLog());
            survival(p);
            ServerLevel level = HeavenPlots.level(p.server);
            Memory m = Memories.get(p).entries().stream().filter(x -> x.id().equals("boss:lucifer")).findFirst().orElseThrow();
            MemoryStage.enter(p, p.getUUID(), MemoryStage.stageCentre(HeavenPlots.origin(level, plot)), m);
        }, () -> camera(mc, CameraType.FIRST_PERSON, true), 40));
        out.add(new Beat("scene", null, () -> camera(mc, CameraType.FIRST_PERSON, false), 120));
        out.add(new Beat("scene_back", (p, c) -> {
            Vec3 pos = p.position();
            p.teleportTo(p.serverLevel(), pos.x, pos.y + 6, pos.z + 10, p.getYRot(), 25);
        }, null, 60));
        out.add(new Beat("focus", (p, c) -> {
            MemoryFigureEntity focus = focus(p);
            if (focus != null) view(p, p.serverLevel(), focus.position().add(0, 1.8, 3.2), focus.position().add(0, 1, 0));
        }, null, 50));
        out.add(new Beat("collected", (p, c) -> {
            MemoryFigureEntity focus = focus(p);
            if (focus != null) MemoryStage.touch(p, focus);
        }, () -> camera(mc, CameraType.FIRST_PERSON, true), 40));
        out.add(new Beat("leave", (p, c) -> MemoryStage.leave(p), null, 40));
        return out;
    }

    private static @Nullable MemoryFigureEntity focus(ServerPlayer p) {
        for (MemoryFigureEntity f : p.serverLevel().getEntitiesOfClass(MemoryFigureEntity.class, p.getBoundingBox().inflate(48))) {
            if (f.isFocus()) return f;
        }
        return null;
    }

    // --- roadhouse -----------------------------------------------------------------------------------------------------

    private static List<Beat> roadhouse(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        Entity[] ash = new Entity[1];
        out.add(new Beat("outside", (p, c) -> {
            spectator(p);
            ServerLevel level = HeavenPlots.level(p.server);
            HeavenPlot hub = HeavenPlots.ensureHub(level);
            HeavenPlots.writeNow(level, hub);
            ash[0] = Roadhouse.ensureAsh(level, hub);
            centre = Vec3.atBottomCenterOf(HeavenPlots.origin(level, hub));
            Vec3 land = point(level, hub, RoadhouseLayout.HUB_LANDING);
            view(p, level, land.add(10, 8, 18), centre.add(0, 3, 0));
        }, () -> camera(mc, CameraType.FIRST_PERSON, false), 80));
        out.add(new Beat("landing", (p, c) -> {
            ServerLevel level = HeavenPlots.level(p.server);
            Vec3 land = Vec3.atBottomCenterOf(HeavenPlots.origin(level, 0).offset(RoadhouseLayout.HUB_LANDING.x(),
                    RoadhouseLayout.HUB_LANDING.y(), RoadhouseLayout.HUB_LANDING.z()));
            view(p, level, land.add(0, 1.6, 0), centre.add(0, 1.6, 0));
        }, null));
        out.add(new Beat("ash", (p, c) -> {
            if (ash[0] != null) view(p, p.serverLevel(), ash[0].position().add(0, 1.6, 3), ash[0].position().add(0, 1.4, 0));
        }, null));
        out.add(new Beat("menu", (p, c) -> {
            survival(p);
            if (ash[0] instanceof org.papiricoh.supernaturalcraft.entity.heaven.AshEntity a) Roadhouse.talk(a, p);
        }, () -> camera(mc, CameraType.FIRST_PERSON, true)));
        return out;
    }

    // --- naomi_model ---------------------------------------------------------------------------------------------------

    private static List<Beat> naomiModel(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        Vec3 eye = new Vec3(0, 1.1, 0);
        out.add(new Beat("front", (p, c) -> {
            spectator(p);
            centre = overworldStage(p, 2000, 1612, 12);
            place(p.serverLevel(), AllEntities.NAOMI.get(), centre, 0);
            view(p, p.serverLevel(), centre.add(0.3, 0.1, 1.9), centre.add(eye));
        }, () -> camera(mc, CameraType.FIRST_PERSON, false)));
        out.add(new Beat("back", (p, c) -> view(p, p.serverLevel(), c.add(-0.3, 0.1, -1.9), c.add(eye)), null));
        out.add(new Beat("side", (p, c) -> view(p, p.serverLevel(), c.add(1.9, 0.1, 0.2), c.add(eye)), null));
        out.add(new Beat("guard_clerk", (p, c) -> {
            place(p.serverLevel(), AllEntities.HEAVEN_GUARD.get(), c.add(-2, 0, 0), 0);
            place(p.serverLevel(), AllEntities.CLERK_ANGEL.get(), c.add(2, 0, 0), 0);
            view(p, p.serverLevel(), c.add(0, 0.6, 5), c.add(0, 1, 0));
        }, null));
        out.add(new Beat("chair", (p, c) -> {
            placeEntity(p.serverLevel(), AllEntities.REPROGRAMMING_CHAIR.get(), c.add(0, 0, -4), 0);
            view(p, p.serverLevel(), c.add(2.5, 1.2, -1), c.add(0, 1, -4));
        }, null));
        out.add(new Beat("copies", (p, c) -> {
            String[] looks = {"@ally:dean", "@ally:sam", "@ally:castiel", "@owner", "@rival:0", "@rival:1", "@rival:2"};
            for (int i = 0; i < looks.length; i++) {
                var copy = place(p.serverLevel(), AllEntities.TRAINING_COPY.get(), c.add(-6 + i * 2, 0, 4), 180);
                copy.raise(java.util.UUID.randomUUID(), looks[i], p.getUUID(), i >= 4);
            }
            view(p, p.serverLevel(), c.add(0, 2, 11), c.add(0, 0.8, 4));
        }, null));
        out.add(new Beat("hud_qte", (p, c) -> {
            survival(p);
            view(p, p.serverLevel(), c.add(0, 0.1, 3), c.add(eye));
            fx(p, fx(-1, HeavenFxPayload.QTE_START, 18, 0, 80, ""));
            fx(p, fx(-1, HeavenFxPayload.QTE_PROGRESS, 9, 18, 0, ""));
        }, () -> camera(mc, CameraType.FIRST_PERSON, true), 40));
        out.add(new Beat("hud_test", (p, c) -> {
            fx(p, fx(-1, HeavenFxPayload.QTE_END, 1, 0, 0, ""));
            fx(p, fx(-1, HeavenFxPayload.TRAINING_TEST, 0, 0, 300, ""));
        }, null, 40));
        out.add(new Beat("hud_title", (p, c) -> {
            fx(p, fx(-1, HeavenFxPayload.TRAINING_TEST, 1, 0, 0, ""));
            fx(p, fx(-1, HeavenFxPayload.NAOMI_TITLE, 2, 0, 100, ""));
        }, null, 40));
        out.add(new Beat("hud_whiteout", (p, c) -> fx(p, fx(-1, HeavenFxPayload.WHITEOUT, 0, 0, 40, "")), null, 12));
        return out;
    }

    // --- zachariah_model -----------------------------------------------------------------------------------------------

    private static List<Beat> zachariahModel(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        Vec3 eye = new Vec3(0, 1.1, 0);
        ZachariahEntity[] z = new ZachariahEntity[1];
        out.add(new Beat("front", (p, c) -> {
            spectator(p);
            centre = overworldStage(p, 2000, 2012, 12);
            z[0] = place(p.serverLevel(), AllEntities.ZACHARIAH.get(), centre, 0);
            z[0].forceLook(1);
            view(p, p.serverLevel(), centre.add(0.3, 0.1, 1.9), centre.add(eye));
        }, () -> camera(mc, CameraType.FIRST_PERSON, false)));
        out.add(new Beat("back", (p, c) -> view(p, p.serverLevel(), c.add(-0.3, 0.1, -1.9), c.add(eye)), null));
        out.add(new Beat("wings_front", (p, c) -> {
            if (z[0] != null) z[0].forceLook(3);
            view(p, p.serverLevel(), c.add(0, 0.6, 5), c.add(0, 1.5, 0));
        }, null));
        out.add(new Beat("wings_back", (p, c) -> view(p, p.serverLevel(), c.add(0.5, 0.8, -5), c.add(0, 1.5, 0)), null));
        out.add(new Beat("clerk_memos", (p, c) -> {
            place(p.serverLevel(), AllEntities.CLERK_ANGEL.get(), c.add(2.5, 0, 1), 0);
            for (int i = 0; i < 4; i++) {
                Entity memo = placeEntity(p.serverLevel(), AllEntities.MEMO_PROJECTILE.get(), c.add(-1.5 + i, 1.4, 2), 0);
                memo.setNoGravity(true);
            }
            view(p, p.serverLevel(), c.add(0, 1.2, 6), c.add(0, 1.2, 1));
        }, null));
        out.add(new Beat("hud_form", (p, c) -> {
            survival(p);
            fx(p, fx(-1, HeavenFxPayload.FORM, 2, 0, 400, ""));
            fx(p, fx(-1, HeavenFxPayload.FORETOLD, 0, 0, 0, "paper_storm,stamp,precedent,termination,wing_buffet"));
            fx(p, fx(-1, HeavenFxPayload.ZACHARIAH_TITLE, 3, 0, 100, ""));
        }, () -> camera(mc, CameraType.FIRST_PERSON, true), 40));
        out.add(new Beat("hud_revision", (p, c) -> fx(p, fx(-1, HeavenFxPayload.REVISION, 2, 0, 30, "shuffle")), null, 18));
        out.add(new Beat("hud_revised", null, null, 30));
        out.add(new Beat("hud_approved", (p, c) -> {
            fx(p, fx(-1, HeavenFxPayload.FORM, 0, 0, 0, ""));
            fx(p, fx(-1, HeavenFxPayload.APPROVED, 0, 0, 200, ""));
        }, null, 20));
        out.add(new Beat("hud_termination", (p, c) -> fx(p, fx(p.getId(), HeavenFxPayload.TERMINATION, 0, 0, 100, "")), null, 30));
        return out;
    }

    // --- heaven_sky ----------------------------------------------------------------------------------------------------

    private static List<Beat> sky(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        out.add(new Beat("up", (p, c) -> {
            spectator(p);
            HeavenPlot plot = ownPlot(p);
            ServerLevel level = HeavenPlots.level(p.server);
            Vec3 land = point(level, plot, HeavenPlotLayout.LANDING);
            p.teleportTo(level, land.x, land.y + 2, land.z, 0, -75);
        }, () -> camera(mc, CameraType.FIRST_PERSON, false), 80));
        out.add(new Beat("horizon", (p, c) -> p.teleportTo(p.serverLevel(), p.getX(), p.getY(), p.getZ(), 30, -2), null));
        out.add(new Beat("down", (p, c) -> p.teleportTo(p.serverLevel(), c.x + 100, c.y + 20, c.z + 100, 225, 35), null));
        out.add(new Beat("edge", (p, c) -> p.teleportTo(p.serverLevel(), c.x + 90, c.y + 4, c.z, 90, 8), null));
        out.add(new Beat("tinted", null, () -> MemoryTint.enter(0xCCB89BFF, 1), 30));
        out.add(new Beat("tinted_up", (p, c) -> p.teleportTo(p.serverLevel(), p.getX(), p.getY(), p.getZ(), 30, -40), null, 30));
        return out;
    }

    // --- crossroads_natural --------------------------------------------------------------------------------------------

    private static List<Beat> crossroads(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        out.add(new Beat("north", (p, c) -> {
            spectator(p);
            ServerLevel level = p.server.overworld();
            level.setDayTime(13000);
            level.setWeatherParameters(6000, 0, false, false);
            level.getChunk(2000 >> 4, 2412 >> 4);
            BlockPos at = org.papiricoh.supernaturalcraft.crossroads.wild.CrossroadsBuilder.placeDirect(level, new BlockPos(2000, 70, 2412), 1818L);
            centre = Vec3.atBottomCenterOf(at);
            view(p, level, centre.add(0, 5, -14), centre);
        }, () -> camera(mc, CameraType.FIRST_PERSON, false), 80));
        out.add(new Beat("east", (p, c) -> view(p, p.serverLevel(), c.add(14, 5, 0), c), null));
        out.add(new Beat("south", (p, c) -> view(p, p.serverLevel(), c.add(0, 5, 14), c), null));
        out.add(new Beat("west", (p, c) -> view(p, p.serverLevel(), c.add(-14, 5, 0), c), null));
        out.add(new Beat("above", (p, c) -> view(p, p.serverLevel(), c.add(0.5, 24, 0.5), c), null));
        out.add(new Beat("ground", (p, c) -> view(p, p.serverLevel(), c.add(3, 1.6, 3), c), null));
        out.add(new Beat("day", (p, c) -> {
            p.serverLevel().setDayTime(6000);
            view(p, p.serverLevel(), c.add(10, 6, -10), c);
        }, null));
        return out;
    }

    // --- heaven_book ---------------------------------------------------------------------------------------------------

    private static List<Beat> book(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        out.add(new Beat("setup", (p, c) -> {
            survival(p);
            Memories.set(p, sampleLog());
        }, () -> ClientHeaven.setLog(sampleLog()), 20));
        HunterBookScreen probe = new HunterBookScreen(HunterBookScreen.Tab.HOME);
        List<BookSection.PreviewShot> shots = probe.memories().previewShots();
        for (int scale : new int[]{2, 3}) {
            for (BookSection.PreviewShot shot : shots) {
                out.add(new Beat(shot.name() + "_s" + scale, null, () -> mc.execute(() -> {
                    mc.options.guiScale().set(scale);
                    mc.resizeDisplay();
                    HunterBookScreen b = new HunterBookScreen(HunterBookScreen.Tab.MEMORIES);
                    mc.setScreen(b);
                    shot.setup().accept(b);
                }), 30));
            }
        }
        return out;
    }

    // --- the fights ----------------------------------------------------------------------------------------------------

    private static boolean fight(Minecraft mc, MinecraftServer server, String scene) {
        boolean naomi = scene.startsWith("naomi");
        int[] phaseAt = naomi ? new int[]{0, 700} : new int[]{0, 600, 1100, 1600};
        int deathAt = naomi ? 1300 : 2100, end = deathAt + 260;
        int now = t;
        if (now == 1) {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
            ServerLevel level = p.serverLevel();
            if (now == 1) {
                centre = overworldStage(p, 2000, naomi ? 2812 : 3212, 4);
                survival(p);
            }
            if (now == 12) {
                BlockPos at = BlockPos.containing(centre);
                boss = naomi ? NaomiSummoning.summon(p.serverLevel(), at, p) : ZachariahSummoning.summon(p.serverLevel(), at, p);
            }
            if (boss == null || boss.isRemoved()) return;
            p.setHealth(p.getMaxHealth());
            p.getFoodData().setFoodLevel(20);
            if (now % 5 == 0) {
                Vec3 to = boss.position().subtract(p.position()).multiply(1, 0, 1);
                if (to.length() > 10 || to.length() < 4) {
                    Vec3 d = to.lengthSqr() < 1e-3 ? new Vec3(0, 0, 1) : to.normalize();
                    p.teleportTo(p.serverLevel(), boss.getX() - d.x * 7, boss.getY(), boss.getZ() - d.z * 7, p.getYRot(), p.getXRot());
                }
                p.lookAt(EntityAnchorArgument.Anchor.EYES, boss.position().add(0, 1.2, 0));
            }
            int phases = phaseAt.length;
            for (int i = 1; i < phases; i++) {
                if (now == phaseAt[i] && boss.phase() < i + 1) {
                    BossHealthGuard.set(boss, boss.getMaxHealth() * (phases - i) / phases + 1);
                    boss.beginTransition(i + 1);
                }
            }
            if (now == deathAt) {
                boss.setAbsorptionAmount(0);
                BossHealthGuard.set(boss, 1f);
                boss.invulnerableTime = 0;
                boss.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, p), 50f);
            }
            if (now == deathAt + 20 && boss.isAlive()) boss.kill();
        });
        if (now >= 100 && now % 40 == 0 && now < end) {
            Screenshot.grab(mc.gameDirectory, String.format("sn_%s_%04d.png", scene, now), mc.getMainRenderTarget(), m -> {
            });
        }
        if (now == end - 5) server.execute(() -> {
            ServerLevel level = server.overworld();
            for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
            ArenaSavedData.get(level).removeClosed();
        });
        if (now >= end) mc.stop();
        return true;
    }
}
