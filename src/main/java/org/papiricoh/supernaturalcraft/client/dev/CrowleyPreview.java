package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.client.crowley.ClientCrowleyFx;
import org.papiricoh.supernaturalcraft.client.crowley.CrowleyHud;
import org.papiricoh.supernaturalcraft.client.crowley.CrowleyRenderers;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicFxManager;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleyAssets;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleyCompanionEntity;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleyEntity;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleySuitEntity;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.PossessedPatientEntity;
import org.papiricoh.supernaturalcraft.network.CrowleyFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Crowley (v0.35) in the real renderer, well away from the origin, at a plain 70°. Screenshots {@code sn_crowley_*.png}.
 * <ul>
 *   <li>{@code crowley_model}: him and his retinue side by side, his front, side, back and face (eyes dark, then lit), every clip of
 *   his at its moment, seated on (the end of) {@code throne_sit} and on one knee (the end of {@code kneel}).</li>
 *   <li>{@code crowley_retinue}: the suited demon and the patient close, each clip at its moment.</li>
 *   <li>{@code crowley_hud}: the contract bar at three healths in its three phases, a seal cracking; the title cards.</li>
 *   <li>{@code crowley_fx}: each {@link CrowleyFxPayload} kind staged close on a plain floor.</li>
 *   <li>{@code asylum}: {@code /supernatural crowley place}, then the façade from the drive, the hall, the cell wing and the
 *   throne room, by day and by night.</li>
 * </ul>
 */
final class CrowleyPreview {

    static final List<String> NAMES = List.of("crowley_model", "crowley_retinue", "crowley_hud", "crowley_fx", "asylum");
    private static final int FOV = 70;
    private static final int X = 7800, Z = 6800;

    private record Beat(String name, BiConsumer<ServerPlayer, Vec3> server, Runnable client, int length) {
    }

    private static List<String> queue;
    private static String scene;
    private static int t, beatIndex, beatStart;
    private static volatile Vec3 base;
    private static List<Beat> beats;
    private static final List<Entity> staged = new ArrayList<>();
    private static volatile Vec3 camFrom, camAt;
    private static int savedFov = -1;

    private CrowleyPreview() {
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
        }
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) return true;
        if (scene == null) {
            if (queue.isEmpty()) {
                if (savedFov > 0) mc.options.fov().set(savedFov);
                CrowleyRenderers.forceEyes(null);
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
            ClientCrowleyFx.clear();
            mc.options.fov().set(FOV);
            mc.options.fovEffectScale().set(0.0);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.hideGui = true;
        }
        t++;
        if (t == 0) {
            server.execute(() -> stage(server));
            return true;
        }
        if (base == null || t < 20) return true;
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
                case "crowley_model" -> model(mc);
                case "crowley_retinue" -> retinue(mc);
                case "crowley_hud" -> hud(mc);
                case "asylum" -> asylum(mc);
                default -> fx(mc);
            };
        }
        if (beatIndex >= beats.size()) {
            server.execute(() -> {
                staged.forEach(Entity::discard);
                staged.clear();
            });
            mc.setScreen(null);
            mc.options.hideGui = true;
            CrowleyRenderers.forceEyes(null);
            scene = null;
            return true;
        }
        Beat b = beats.get(beatIndex);
        if (beatStart == 0) beatStart = t;
        int local = t - beatStart;
        Vec3 c = base;
        if (local == 0 && b.server != null) server.execute(() -> b.server.accept(player(server), c));
        if (local == 2 && b.client != null) b.client.run();
        if (local == b.length - 2) {
            Screenshot.grab(mc.gameDirectory, "sn_" + scene + "_" + b.name + ".png", mc.getMainRenderTarget(), m -> {
            });
        }
        if (local >= b.length) {
            beatIndex++;
            beatStart = t + 1;
        }
        return true;
    }

    // --- staging ---------------------------------------------------------------------------------------------------------

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static void stage(MinecraftServer server) {
        ServerPlayer p = player(server);
        ServerLevel level = server.overworld();
        p.setGameMode(GameType.SPECTATOR);
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        level.setDayTime(6000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.setWeatherParameters(12000, 0, false, false);
        level.getChunk(X >> 4, Z >> 4);
        BlockPos g = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(X, 0, Z));
        for (Entity old : level.getEntitiesOfClass(Entity.class, new AABB(g).inflate(40), e -> e instanceof Mob)) old.discard();
        for (int dx = -14; dx <= 14; dx++) {
            for (int dz = -14; dz <= 14; dz++) {
                int h = Math.floorMod(dx * 7 + dz * 13, 9);
                level.setBlockAndUpdate(g.offset(dx, -1, dz), h == 0 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                        : (dx + dz) % 2 == 0 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState());
                for (int dy = 0; dy < 8; dy++) level.setBlockAndUpdate(g.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }
        }
        Vec3 b = Vec3.atBottomCenterOf(g);
        p.teleportTo(level, b.x, b.y + 3, b.z + 8, 180, 15);
        base = b;
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

    private static void clearStaged() {
        staged.forEach(Entity::discard);
        staged.clear();
        Minecraft.getInstance().execute(MagicFxManager::clear);
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

    private static Beat still(String name, BiConsumer<ServerPlayer, Vec3> server) {
        return new Beat(name, server, null, 30);
    }

    private static <T extends Mob> Beat clip(String name, EntityType<T> type, String clip, int at, Mob[] holder) {
        return new Beat(name, (p, c) -> {
            clearStaged();
            T e = mob(p.serverLevel(), type, c, 0);
            holder[0] = e;
            if (e instanceof software.bernie.geckolib.animatable.GeoEntity g) g.triggerAnim("action", clip);
            view(p, c.add(2.2, 1.6, 2.6), c.add(0, 1.0, 0));
        }, null, at + 6);
    }

    // --- crowley_model -----------------------------------------------------------------------------------------------------

    private static List<Beat> model(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        Vec3 eye = new Vec3(0, 1.1, 0);
        out.add(new Beat("lineup", (p, c) -> {
            clearStaged();
            ServerLevel level = p.serverLevel();
            mob(level, AllEntities.CROWLEY_SUIT.get(), c.add(-2.4, 0, 0.6), 0);
            mob(level, AllEntities.CROWLEY.get(), c, 0);
            mob(level, AllEntities.POSSESSED_PATIENT.get(), c.add(2.4, 0, 0.6), 0);
            view(p, c.add(0, 1.7, 7.0), c.add(0, 1.0, 0));
        }, null, 50));
        out.add(new Beat("front", (p, c) -> {
            clearStaged();
            mob(p.serverLevel(), AllEntities.CROWLEY.get(), c, 0);
            view(p, c.add(0.3, 1.5, 2.6), c.add(eye));
        }, null, 30));
        out.add(still("side", (p, c) -> view(p, c.add(2.6, 1.5, 0.2), c.add(eye))));
        out.add(still("back", (p, c) -> view(p, c.add(-0.4, 1.5, -2.6), c.add(eye))));
        out.add(still("face", (p, c) -> view(p, c.add(0.15, 1.6, 1.0), c.add(0, 1.55, 0))));
        out.add(new Beat("face_eyes", null, () -> CrowleyRenderers.forceEyes(true), 20));
        out.add(new Beat("face_eyes_night", (p, c) -> p.serverLevel().setDayTime(18000), null, 20));
        out.add(new Beat("companion", (p, c) -> {
            clearStaged();
            p.serverLevel().setDayTime(6000);
            mob(p.serverLevel(), AllEntities.CROWLEY_COMPANION.get(), c, 0);
            view(p, c.add(1.2, 1.5, 2.6), c.add(eye));
        }, () -> CrowleyRenderers.forceEyes(null), 30));
        Mob[] holder = new Mob[1];
        for (String clip : CrowleyAssets.TRIGGERED) {
            int at = CrowleyAssets.HIT_TICKS.getOrDefault(clip, 10);
            out.add(clip("clip_" + clip, AllEntities.CROWLEY.get(), clip, at, holder));
        }
        // Seated (the end of sitting down) and on one knee (the end of kneeling), from the front and the side.
        int sit = CrowleyAssets.CLIP_TICKS.get("throne_sit"), kneel = CrowleyAssets.CLIP_TICKS.get("kneel");
        out.add(clip("seated", AllEntities.CROWLEY.get(), "throne_sit", sit - 4, holder));
        out.add(clip("kneeling", AllEntities.CROWLEY.get(), "kneel", kneel - 4, holder));
        return out;
    }

    // --- crowley_retinue ---------------------------------------------------------------------------------------------------

    private static List<Beat> retinue(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        Vec3 eye = new Vec3(0, 1.1, 0);
        out.add(new Beat("suit", (p, c) -> {
            clearStaged();
            mob(p.serverLevel(), AllEntities.CROWLEY_SUIT.get(), c, 0);
            view(p, c.add(0.8, 1.5, 2.6), c.add(eye));
        }, null, 40));
        out.add(still("suit_side", (p, c) -> view(p, c.add(2.6, 1.5, 0.4), c.add(eye))));
        out.add(new Beat("patient", (p, c) -> {
            clearStaged();
            mob(p.serverLevel(), AllEntities.POSSESSED_PATIENT.get(), c, 0);
            view(p, c.add(0.8, 1.5, 2.6), c.add(eye));
        }, null, 40));
        out.add(still("patient_back", (p, c) -> view(p, c.add(-0.8, 1.5, -2.6), c.add(eye))));
        Mob[] holder = new Mob[1];
        for (String clip : CrowleyAssets.RETINUE_CLIPS.get(CrowleyAssets.SUIT)) {
            if (CrowleyAssets.RETINUE_LOOPS.get(CrowleyAssets.SUIT).contains(clip)) continue;
            out.add(clip("suit_" + clip, AllEntities.CROWLEY_SUIT.get(), clip, CrowleyAssets.RETINUE_HIT_TICKS.getOrDefault(clip, 8) + 2, holder));
        }
        for (String clip : CrowleyAssets.RETINUE_CLIPS.get(CrowleyAssets.PATIENT)) {
            if (CrowleyAssets.RETINUE_LOOPS.get(CrowleyAssets.PATIENT).contains(clip)) continue;
            out.add(clip("patient_" + clip, AllEntities.POSSESSED_PATIENT.get(), clip, CrowleyAssets.RETINUE_HIT_TICKS.getOrDefault(clip, 8) + 6, holder));
        }
        return out;
    }

    // --- crowley_hud -------------------------------------------------------------------------------------------------------

    /** Draws the contract bar at fixed states, over the world. */
    private static final class Bars extends Screen {
        private final int[] phases;
        private final float[] health;
        private final UUID[] ids = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};

        Bars(int[] phases, float[] health) {
            super(Component.empty());
            this.phases = phases;
            this.health = health;
        }

        @Override
        public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
            for (int i = 0; i < phases.length; i++) {
                CrowleyHud.draw(g, ids[i], Component.translatable(CrowleyAssets.BAR_PREFIX + ".phase" + phases[i]), phases[i], health[i],
                        12 + i * 70, partial);
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    private static List<Beat> hud(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        out.add(new Beat("bars", (p, c) -> view(p, c.add(0, 1.6, 6), c.add(0, 1.2, 0)),
                () -> mc.setScreen(new Bars(new int[]{1, 2, 3}, new float[]{0.92f, 0.55f, 0.14f})), 40));
        out.add(new Beat("bars_hit", null, () -> mc.setScreen(new Bars(new int[]{1, 2, 3}, new float[]{0.85f, 0.4f, 0.06f})), 6));
        out.add(new Beat("seal_cracking", null, () -> CrowleyHud.sealBroken(2), 8));
        out.add(new Beat("seal_broken", null, null, 20));
        for (int ph = 1; ph <= 3; ph++) {
            int phase = ph;
            out.add(new Beat("title_phase" + ph, null, () -> {
                mc.setScreen(null);
                mc.options.hideGui = false;
                ClientCrowleyFx.handle(new CrowleyFxPayload(-1, CrowleyFxPayload.TITLE, phase, 0, Vec3.ZERO, 100));
            }, 44));
        }
        out.add(new Beat("title_fall", null, () -> ClientCrowleyFx.handle(new CrowleyFxPayload(-1, CrowleyFxPayload.TITLE, 3, 1, Vec3.ZERO, 120)), 70));
        out.add(new Beat("end", null, () -> mc.options.hideGui = true, 4));
        return out;
    }

    // --- crowley_fx --------------------------------------------------------------------------------------------------------

    private static int crowleyId = -1, targetId = -1;

    private static Beat fxBeat(String name, int length, java.util.function.Function<Vec3, CrowleyFxPayload> payload) {
        return new Beat(name, null, () -> {
            Vec3 c = base;
            ClientCrowleyFx.handle(payload.apply(c));
        }, length);
    }

    private static List<Beat> fx(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        out.add(new Beat("stage", (p, c) -> {
            clearStaged();
            CrowleyEntity cr = mob(p.serverLevel(), AllEntities.CROWLEY.get(), c, 0);
            CrowleySuitEntity s = mob(p.serverLevel(), AllEntities.CROWLEY_SUIT.get(), c.add(0, 0, 5), 180);
            crowleyId = cr.getId();
            targetId = s.getId();
            view(p, c.add(6, 3.4, 7), c.add(0, 0.8, 2.5));
        }, null, 30));
        out.add(fxBeat("snap", 5, c -> new CrowleyFxPayload(crowleyId, CrowleyFxPayload.SNAP, targetId, 0, c.add(-0.4, 1.5, 0.3), 10)));
        out.add(fxBeat("red_smoke", 40, c -> new CrowleyFxPayload(crowleyId, CrowleyFxPayload.RED_SMOKE, 30, 0, c.add(0, 0, 3), 80)));
        out.add(fxBeat("vanish", 8, c -> new CrowleyFxPayload(crowleyId, CrowleyFxPayload.VANISH, 0, 0, c.add(3, 0, 0), 14)));
        out.add(fxBeat("appear", 10, c -> new CrowleyFxPayload(crowleyId, CrowleyFxPayload.APPEAR, 0, 0, c.add(-3, 0, 1), 14)));
        out.add(fxBeat("paper_storm", 22, c -> new CrowleyFxPayload(crowleyId, CrowleyFxPayload.PAPER_STORM, 0, 9, c, 40)));
        out.add(fxBeatNull("paper_storm_late", 18));
        out.add(fxBeat("hellfire_line", 16, c -> new CrowleyFxPayload(crowleyId, CrowleyFxPayload.HELLFIRE_LINE, -60, 80, c.add(3, 0, -2), 60)));
        out.add(fxBeat("seal_break_before", 10, c -> new CrowleyFxPayload(crowleyId, CrowleyFxPayload.SEAL_BREAK, 1, 0, c, 30)));
        out.add(fxBeatNull("seal_break_burst", 10));
        out.add(fxBeat("hounds", 34, c -> new CrowleyFxPayload(crowleyId, CrowleyFxPayload.HOUNDS, 3, 0, c.add(-2, 0, 4), 120)));
        out.add(fxBeat("collect", 30, c -> new CrowleyFxPayload(crowleyId, CrowleyFxPayload.COLLECT, targetId, 0, c.add(0, 0, 5), 100)));
        out.add(fxBeat("companion_poof", 8, c -> new CrowleyFxPayload(-1, CrowleyFxPayload.COMPANION_POOF, 1, 0, c.add(4, 0, 4), 12)));
        return out;
    }

    // --- asylum ------------------------------------------------------------------------------------------------------------

    private static Vec3 pt(MinecraftServer server, String name) {
        org.papiricoh.supernaturalcraft.asylum.AsylumFight f = org.papiricoh.supernaturalcraft.asylum.AsylumWorld.fight(server);
        BlockPos b = f == null ? null : f.point(name);
        return b == null ? base : Vec3.atBottomCenterOf(b);
    }

    /** From {@code from}, {@code back} blocks further from {@code to} and {@code up} higher, looking at {@code to} raised by {@code at}. */
    private static void frame(ServerPlayer p, String from, String to, double back, double up, double at) {
        MinecraftServer server = p.server;
        Vec3 a = pt(server, from), b = pt(server, to);
        Vec3 dir = a.subtract(b).multiply(1, 0, 1).normalize();
        view(p, a.add(dir.scale(back)).add(0, up, 0), b.add(0, at, 0));
    }

    private static List<Beat> asylum(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        String entry = org.papiricoh.supernaturalcraft.asylum.layout.AsylumAnchors.ENTRY, hall = org.papiricoh.supernaturalcraft.asylum.layout.AsylumAnchors.HALL,
                threshold = org.papiricoh.supernaturalcraft.asylum.layout.AsylumAnchors.HALL_THRESHOLD,
                cells = org.papiricoh.supernaturalcraft.asylum.layout.AsylumAnchors.CELLS, throne = org.papiricoh.supernaturalcraft.asylum.layout.AsylumAnchors.THRONE,
                seat = org.papiricoh.supernaturalcraft.asylum.layout.AsylumAnchors.THRONE_SEAT;
        out.add(new Beat("built", (p, c) -> {
            p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(), "supernatural crowley place");
            frame(p, entry, hall, 14, 9, 8);
        }, null, 160));
        out.add(still("facade_close", (p, c) -> frame(p, entry, hall, 2, 1.8, 6)));
        out.add(still("hall", (p, c) -> frame(p, threshold, hall, 0, 1.7, 1.5)));
        out.add(still("cells", (p, c) -> frame(p, hall, cells, -6, 1.7, 1.5)));
        out.add(still("throne", (p, c) -> frame(p, throne, seat, 7, 1.7, 1.2)));
        out.add(new Beat("facade_night", (p, c) -> {
            p.serverLevel().setDayTime(18000);
            frame(p, entry, hall, 14, 9, 8);
        }, null, 40));
        out.add(still("throne_night", (p, c) -> frame(p, throne, seat, 7, 1.7, 1.2)));
        out.add(new Beat("day", (p, c) -> p.serverLevel().setDayTime(6000), null, 4));
        return out;
    }

    private static Beat fxBeatNull(String name, int length) {
        return new Beat(name, null, null, length);
    }
}
