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
import org.papiricoh.supernaturalcraft.client.godstiel.ClientGodstielFx;
import org.papiricoh.supernaturalcraft.client.godstiel.GodstielHud;
import org.papiricoh.supernaturalcraft.client.godstiel.GodstielRenderers;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicFxManager;
import org.papiricoh.supernaturalcraft.entity.boss.godstiel.GodstielAssets;
import org.papiricoh.supernaturalcraft.entity.boss.godstiel.GodstielBalance;
import org.papiricoh.supernaturalcraft.entity.boss.godstiel.GodstielEntity;
import org.papiricoh.supernaturalcraft.network.GodstielFxPayload;
import org.papiricoh.supernaturalcraft.pact.LabSite;
import org.papiricoh.supernaturalcraft.pact.PactWorld;
import org.papiricoh.supernaturalcraft.pact.layout.LabAnchors;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Season 6 (v0.39) in the real renderer, well away from the origin, at a plain 70°. Screenshots {@code sn_<scene>_*.png}.
 * <ul>
 *   <li>{@code godstiel_model}: Castiel, Godstiel and Crowley as King side by side; Godstiel in each phase's look (front and
 *   face); every clip of his at its moment.</li>
 *   <li>{@code godstiel_hud}: the "light into goo" bar in its five phases at falling health, the struggle; the title cards and
 *   the voices.</li>
 *   <li>{@code godstiel_fx}: each {@link GodstielFxPayload} kind staged close on a plain floor.</li>
 *   <li>{@code crowleys_lab}: {@code /supernatural pact lab}, then the pump house from the drive, the hall, the deck, by day and
 *   by night.</li>
 * </ul>
 */
final class GodstielPreview {

    static final List<String> NAMES = List.of("godstiel_model", "godstiel_hud", "godstiel_fx", "crowleys_lab");
    private static final int FOV = 70;
    private static final int X = 8400, Z = 7400;

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

    private GodstielPreview() {
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
                GodstielRenderers.Godstiel.forceLook(0);
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
            ClientGodstielFx.clear();
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
                case "godstiel_model" -> model();
                case "godstiel_hud" -> hud(mc);
                case "crowleys_lab" -> lab();
                default -> fx();
            };
        }
        if (beatIndex >= beats.size()) {
            server.execute(() -> {
                staged.forEach(Entity::discard);
                staged.clear();
            });
            mc.setScreen(null);
            mc.options.hideGui = true;
            GodstielRenderers.Godstiel.forceLook(0);
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
                        : (dx + dz) % 2 == 0 ? Blocks.POLISHED_ANDESITE.defaultBlockState() : Blocks.SMOOTH_STONE.defaultBlockState());
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

    // --- godstiel_model ----------------------------------------------------------------------------------------------------

    private static List<Beat> model() {
        List<Beat> out = new ArrayList<>();
        Vec3 eye = new Vec3(0, 1.2, 0);
        out.add(new Beat("lineup", (p, c) -> {
            clearStaged();
            ServerLevel level = p.serverLevel();
            mob(level, AllEntities.CASTIEL.get(), c.add(-2.4, 0, 0.6), 0);
            mob(level, AllEntities.GODSTIEL.get(), c, 0);
            mob(level, AllEntities.CROWLEY_KING.get(), c.add(2.4, 0, 0.6), 0);
            view(p, c.add(0, 1.7, 7.0), c.add(0, 1.0, 0));
        }, null, 50));
        for (int phase = 1; phase <= GodstielBalance.PHASES; phase++) {
            int look = phase;
            out.add(new Beat("phase" + phase, (p, c) -> {
                clearStaged();
                mob(p.serverLevel(), AllEntities.GODSTIEL.get(), c, 0);
                view(p, c.add(0.4, 1.6, 3.0), c.add(eye));
            }, () -> GodstielRenderers.Godstiel.forceLook(look), 30));
            out.add(still("phase" + phase + "_face", (p, c) -> view(p, c.add(0.15, 1.7, 1.1), c.add(0, 1.65, 0))));
        }
        out.add(new Beat("reset_look", null, () -> GodstielRenderers.Godstiel.forceLook(0), 2));
        for (String clip : GodstielAssets.TRIGGERED) {
            int at = GodstielAssets.HIT_TICKS.getOrDefault(clip, GodstielAssets.CLIP_TICKS.getOrDefault(clip, 20) / 2);
            int look = clip.matches("bite|sweep|slam|retch|surge|struggle|recover|crack") ? 4 : 1;
            out.add(new Beat("clip_" + clip, (p, c) -> {
                clearStaged();
                GodstielEntity g = mob(p.serverLevel(), AllEntities.GODSTIEL.get(), c, 0);
                g.triggerAnim("action", clip);
                view(p, c.add(2.4, 1.7, 2.8), c.add(0, 1.0, 0));
            }, () -> GodstielRenderers.Godstiel.forceLook(look), at + 6));
        }
        return out;
    }

    // --- godstiel_hud ------------------------------------------------------------------------------------------------------

    /** Draws the bar at fixed states, over the world. */
    private static final class Bars extends Screen {
        private final int[] phases;
        private final float[] health;
        private final UUID[] ids = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};

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
                GodstielHud.draw(g, ids[i], Component.translatable(GodstielAssets.BAR_PREFIX + ".phase" + phases[i]), phases[i], health[i],
                        14 + i * 58, partial);
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
                () -> mc.setScreen(new Bars(new int[]{1, 2, 3, 4, 5}, new float[]{0.95f, 0.72f, 0.55f, 0.33f, 0.1f})), 40));
        out.add(new Beat("bars_struggle", null, () -> ClientGodstielFx.handle(new GodstielFxPayload(-1, GodstielFxPayload.STRUGGLE, 1, 1, Vec3.ZERO, 80)), 16));
        for (int ph = 1; ph <= GodstielBalance.PHASES; ph++) {
            int phase = ph;
            out.add(new Beat("title_phase" + ph, null, () -> {
                mc.setScreen(null);
                mc.options.hideGui = false;
                ClientGodstielFx.handle(new GodstielFxPayload(-1, GodstielFxPayload.TITLE, phase, 0, Vec3.ZERO, 100));
            }, 44));
        }
        for (int w = 1; w <= 3; w++) {
            int which = w;
            out.add(new Beat("title_end" + w, null, () -> ClientGodstielFx.handle(new GodstielFxPayload(-1, GodstielFxPayload.TITLE, 5, which, Vec3.ZERO, 100)), 44));
        }
        out.add(new Beat("voices", null, () -> ClientGodstielFx.handle(new GodstielFxPayload(-1, GodstielFxPayload.VOICES, 3, 0, Vec3.ZERO, 70)), 40));
        out.add(new Beat("end", null, () -> mc.options.hideGui = true, 4));
        return out;
    }

    // --- godstiel_fx -------------------------------------------------------------------------------------------------------

    private static int godId = -1;

    private static Beat fxBeat(String name, int length, java.util.function.Function<Vec3, GodstielFxPayload> payload) {
        return new Beat(name, null, () -> {
            Vec3 c = base;
            ClientGodstielFx.handle(payload.apply(c));
        }, length);
    }

    private static List<Beat> fx() {
        List<Beat> out = new ArrayList<>();
        out.add(new Beat("stage", (p, c) -> {
            clearStaged();
            GodstielEntity g = mob(p.serverLevel(), AllEntities.GODSTIEL.get(), c, 0);
            godId = g.getId();
            view(p, c.add(6, 3.4, 7), c.add(0, 0.8, 2.5));
        }, null, 30));
        out.add(fxBeat("smite", 10, c -> new GodstielFxPayload(godId, GodstielFxPayload.SMITE, 18, 0, c.add(0, 0, 4), 20)));
        out.add(fxBeat("palm", 10, c -> new GodstielFxPayload(godId, GodstielFxPayload.PALM, 0, 12, c, 16)));
        out.add(fxBeat("radiance", 12, c -> new GodstielFxPayload(godId, GodstielFxPayload.RADIANCE, 70, 0, c, 30)));
        out.add(fxBeat("judged", 10, c -> new GodstielFxPayload(godId, GodstielFxPayload.JUDGED, -1, 0, c.add(3, 0, 3), 24)));
        out.add(fxBeat("bolt", 10, c -> new GodstielFxPayload(godId, GodstielFxPayload.BOLT, 40, 120, c, 14)));
        out.add(fxBeat("crack", 12, c -> new GodstielFxPayload(godId, GodstielFxPayload.CRACK, 2, 0, c, 40)));
        out.add(fxBeat("goo", 10, c -> new GodstielFxPayload(godId, GodstielFxPayload.GOO, 0, 9, c, 16)));
        out.add(fxBeat("sweep", 10, c -> new GodstielFxPayload(godId, GodstielFxPayload.SWEEP, 55, 0, c, 16)));
        out.add(fxBeat("surge", 12, c -> new GodstielFxPayload(godId, GodstielFxPayload.SURGE, 22, 0, c.add(-3, 0, 3), 24)));
        out.add(fxBeat("struggle", 30, c -> new GodstielFxPayload(godId, GodstielFxPayload.STRUGGLE, 1, 0, c, 100)));
        out.add(fxBeat("door", 50, c -> new GodstielFxPayload(-1, GodstielFxPayload.DOOR, 1, 0, c.add(0, 4, 3), 120)));
        out.add(fxBeat("souls", 40, c -> new GodstielFxPayload(godId, GodstielFxPayload.SOULS, 0, 0, c.add(0, 4, 3), 60)));
        return out;
    }

    // --- crowleys_lab ------------------------------------------------------------------------------------------------------

    private static Vec3 pt(MinecraftServer server, String name) {
        LabSite site = PactWorld.site(server);
        BlockPos b = site == null ? null : site.point(name);
        return b == null ? base : Vec3.atBottomCenterOf(b);
    }

    /** From {@code from}, {@code back} blocks further from {@code to} and {@code up} higher, looking at {@code to} raised by {@code at}. */
    private static void frame(ServerPlayer p, String from, String to, double back, double up, double at) {
        MinecraftServer server = p.server;
        Vec3 a = pt(server, from), b = pt(server, to);
        Vec3 dir = a.subtract(b).multiply(1, 0, 1).normalize();
        view(p, a.add(dir.scale(back)).add(0, up, 0), b.add(0, at, 0));
    }

    private static List<Beat> lab() {
        List<Beat> out = new ArrayList<>();
        out.add(new Beat("built", (p, c) -> {
            p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(), "supernatural pact lab");
            frame(p, LabAnchors.ENTRY, LabAnchors.HALL, 18, 12, 4);
        }, null, 160));
        out.add(still("front", (p, c) -> frame(p, LabAnchors.ENTRY, LabAnchors.HALL, 3, 1.8, 4)));
        out.add(still("hall", (p, c) -> frame(p, LabAnchors.RITE, LabAnchors.HALL, 4, 2.5, 1.5)));
        out.add(still("cages", (p, c) -> frame(p, LabAnchors.HALL, LabAnchors.CROWLEY, 2, 2.0, 1.0)));
        out.add(still("deck", (p, c) -> frame(p, LabAnchors.HALL, LabAnchors.DECK, -3, 4, 0.5)));
        out.add(new Beat("night", (p, c) -> {
            p.serverLevel().setDayTime(18000);
            frame(p, LabAnchors.DECK, LabAnchors.HALL, 14, 8, 4);
        }, null, 40));
        out.add(still("hall_night", (p, c) -> frame(p, LabAnchors.RITE, LabAnchors.HALL, 4, 2.5, 1.5)));
        out.add(new Beat("day", (p, c) -> p.serverLevel().setDayTime(6000), null, 4));
        return out;
    }
}
