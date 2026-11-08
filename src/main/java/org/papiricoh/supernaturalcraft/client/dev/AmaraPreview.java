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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.client.amara.AmaraFxRenderer;
import org.papiricoh.supernaturalcraft.client.amara.AmaraHud;
import org.papiricoh.supernaturalcraft.client.amara.AmaraOverlay;
import org.papiricoh.supernaturalcraft.client.amara.ClientAmara;
import org.papiricoh.supernaturalcraft.client.dyingsun.ClientDyingSun;
import org.papiricoh.supernaturalcraft.dyingsun.DyingSunPayload;
import org.papiricoh.supernaturalcraft.dyingsun.DyingSunRules;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAssets;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraLook;
import org.papiricoh.supernaturalcraft.network.AmaraFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * v0.42 preview scenes for the Darkness's look (the art agent's): {@code amara_model} (both rigs, every clip), {@code amara_form}
 * (the five ages side by side), {@code amara_hud} (the eclipse HUD through the six phases, the enrage and the dawn, with fake
 * values), {@code amara_fx} (every {@code AmaraFxPayload} kind), {@code amara_sky} (the sky stages), {@code dying_sun} (the world's
 * sun at several darkness values). Same shape as {@code NephilimPreview}: it runs only when every name in {@code SN_PREVIEW} is
 * one of its {@link #NAMES}; captures in {@code runs/client/screenshots/sn_<scene>_<beat>.png}.
 *
 * <p>The scenes never depend on the fight's own state machine: the look is staged on the client ({@link ClientAmara#fake}) and
 * each clip is played straight on the staged entity's {@code action} controller (a triggerable added for the preview).
 */
final class AmaraPreview {

    static final List<String> NAMES = List.of("amara_model", "amara_form", "amara_hud", "amara_fx", "amara_sky", "dying_sun");
    private static final int FOV = 70;
    private static final int X = 18800, Z = -12600;

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
    private static volatile int amaraId = -1;
    private static final List<Integer> lineup = new java.util.concurrent.CopyOnWriteArrayList<>();

    private AmaraPreview() {
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
        }
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) return true;
        if (scene == null) {
            if (queue.isEmpty()) {
                if (savedFov > 0) mc.options.fov().set(savedFov);
                reset();
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
            reset();
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
                case "amara_model" -> model();
                case "amara_form" -> ages();
                case "amara_hud" -> hud(mc);
                case "amara_fx" -> fx();
                case "amara_sky" -> sky();
                default -> dyingSun();
            };
        }
        if (beatIndex >= beats.size()) {
            server.execute(AmaraPreview::clearStaged);
            mc.setScreen(null);
            mc.options.hideGui = true;
            reset();
            scene = null;
            return true;
        }
        Beat b = beats.get(beatIndex);
        if (beatStart == 0) beatStart = t;
        int local = t - beatStart;
        Vec3 c = base;
        if (local == 0 && b.server != null) server.execute(() -> b.server.accept(player(server), c));
        if (local == 3 && b.client != null) {
            try {
                b.client.run();
            } catch (RuntimeException e) {
                org.papiricoh.supernaturalcraft.SupernaturalCraft.LOGGER.warn("[preview] {} {} failed", scene, b.name, e);
            }
        }
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

    private static void reset() {
        ClientAmara.clearFakes();
        AmaraFxRenderer.clear();
        AmaraOverlay.clear();
        lineup.clear();
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
        level.setDayTime(scene.equals("amara_sky") ? 6000 : 13500);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.setWeatherParameters(12000, 0, false, false);
        int x = X + NAMES.indexOf(scene) * 160;
        level.getChunk(x >> 4, Z >> 4);
        BlockPos g = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, Z));
        for (Entity old : level.getEntitiesOfClass(Entity.class, new AABB(g).inflate(48), e -> e instanceof Mob)) old.discard();
        for (int dx = -18; dx <= 18; dx++) {
            for (int dz = -18; dz <= 18; dz++) {
                int h = Math.floorMod(dx * 7 + dz * 13, 9);
                level.setBlockAndUpdate(g.offset(dx, -1, dz), h == 0 ? Blocks.COBBLESTONE.defaultBlockState()
                        : h < 3 ? Blocks.COARSE_DIRT.defaultBlockState() : (dx + dz) % 2 == 0 ? Blocks.STONE_BRICKS.defaultBlockState()
                        : Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
                for (int dy = 0; dy < 14; dy++) level.setBlockAndUpdate(g.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }
        }
        Vec3 b = Vec3.atBottomCenterOf(g);
        p.teleportTo(level, b.x, b.y + 3, b.z + 8, 180, 15);
        base = b;
    }

    private static Mob amara(ServerLevel level, Vec3 at, float yaw) {
        Mob e = AllEntities.AMARA.get().create(level);
        e.moveTo(at.x, at.y, at.z, yaw, 0);
        e.setYHeadRot(yaw);
        e.setYBodyRot(yaw);
        e.setNoAi(true);
        e.setPersistenceRequired();
        e.setInvulnerable(true);
        level.addFreshEntity(e);
        staged.add(e);
        amaraId = e.getId();
        return e;
    }

    private static void clearStaged() {
        staged.forEach(Entity::discard);
        staged.clear();
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

    /** Plays {@code animation.<rig>.<clip>} on a staged entity's {@code action} controller, whatever the fight has registered. */
    private static void play(int id, String rig, String clip, boolean loop) {
        Minecraft mc = Minecraft.getInstance();
        Entity e = mc.level == null ? null : mc.level.getEntity(id);
        if (!(e instanceof GeoEntity geo)) return;
        var controllers = geo.getAnimatableInstanceCache().getManagerForId(e.getId()).getAnimationControllers();
        AnimationController<?> c = controllers.get("action");
        if (c == null && !controllers.isEmpty()) c = controllers.values().iterator().next();
        if (c == null) return;
        String key = "sn_preview_" + rig + "_" + clip;
        String name = "animation." + rig + "." + clip;
        c.triggerableAnim(key, loop ? RawAnimation.begin().thenLoop(name) : RawAnimation.begin().thenPlayAndHold(name));
        c.forceAnimationReset();
        c.tryTriggerAnimation(key);
    }

    private static ClientAmara.Fake look(int form, int phase) {
        return new ClientAmara.Fake(form, phase, phase >= 5 ? 2 : phase >= 3 ? 1 : 0, phase == 4 ? 0.7f : 0f, phase == 6 ? 0.6f : 1f, false);
    }

    private static int hitOf(String clip) {
        return AmaraAssets.HIT_TICKS.getOrDefault(clip, AmaraAssets.CLIP_TICKS.getOrDefault(clip, 30) / 2);
    }

    // --- amara_model -----------------------------------------------------------------------------------------------------

    private static List<Beat> model() {
        List<Beat> out = new ArrayList<>();
        out.add(new Beat("mass", (p, c) -> {
            clearStaged();
            amara(p.serverLevel(), c, 0);
            view(p, c.add(0, 7, 24), c.add(0, 5, 0));
        }, () -> ClientAmara.fake(look(AmaraLook.MASS, 1)), 60));
        for (String clip : List.of("slam", "sweep", "grasp", "spikes", "bloom", "flare", "totality", "collapse", "expose", "shatter")) {
            int at = clip.equals("shatter") ? hitOf(clip) : 20;
            out.add(new Beat("mass_" + clip, null, () -> play(amaraId, AmaraAssets.RIG_MASS, clip, false), at + 4));
        }
        out.add(new Beat("form", (p, c) -> view(p, c.add(0, 1.6, 4.4), c.add(0, 1.1, 0)), () -> {
            ClientAmara.fake(look(AmaraLook.WOMAN, 3));
            play(amaraId, AmaraAssets.RIG_FORM, "idle", true);
        }, 40));
        for (String clip : AmaraAssets.FORM_TRIGGERED) {
            int at = Math.min(hitOf(clip), 50);
            int phase = clip.matches("giggle_step|hide|found|toss|tantrum|laugh|become_girl") ? 2
                    : clip.matches("wound|wall|scream") ? 4 : clip.matches("crumble|pull|starfall|ascend|become_void") ? 5
                    : clip.matches("countdown|enrage|become_sun") ? 6 : 3;
            int form = phase == 2 ? AmaraLook.GIRL : phase == 5 ? AmaraLook.VOID : AmaraLook.WOMAN;
            out.add(new Beat("form_" + clip, (p, c) -> view(p, c.add(2.6, 1.7, 3.2), c.add(0, 1.0, 0)), () -> {
                ClientAmara.fake(look(form, phase));
                if (clip.equals("wound")) ClientAmara.chains(amaraId, 60);
                play(amaraId, AmaraAssets.RIG_FORM, clip, false);
            }, at + 5));
        }
        return out;
    }

    // --- amara_form ------------------------------------------------------------------------------------------------------

    private static List<Beat> ages() {
        List<Beat> out = new ArrayList<>();
        out.add(new Beat("lineup", (p, c) -> {
            clearStaged();
            lineup.clear();
            for (int i = 0; i < 5; i++) {
                Mob e = amara(p.serverLevel(), c.add((i - 2) * 1.9, 0, 0), 0);
                lineup.add(e.getId());
            }
            view(p, c.add(0, 1.6, 7.5), c.add(0, 1.0, 0));
        }, () -> {
            AmaraAssets.Age[] ages = AmaraAssets.Age.values();
            for (int i = 0; i < lineup.size() && i < ages.length; i++) {
                int form = switch (ages[i]) {
                    case GIRL, TEEN -> AmaraLook.GIRL;
                    case VOID -> AmaraLook.VOID;
                    default -> AmaraLook.WOMAN;
                };
                ClientAmara.fake(lineup.get(i), new ClientAmara.Fake(form, form == AmaraLook.VOID ? 5 : 3, 0, 0, 1, false));
                play(lineup.get(i), AmaraAssets.RIG_FORM, ages[i] == AmaraAssets.Age.BABY ? "bundle_idle"
                        : ages[i] == AmaraAssets.Age.GIRL ? "child_idle" : "idle", true);
            }
        }, 60));
        out.add(new Beat("lineup_close", (p, c) -> view(p, c.add(0, 1.4, 4.2), c.add(0, 1.0, 0)), null, 30));
        out.add(new Beat("woman_face", (p, c) -> view(p, c.add(1.9, 1.75, 1.3), c.add(1.9, 1.62, 0)), null, 30));
        out.add(new Beat("void_face", (p, c) -> view(p, c.add(3.8, 2.15, 1.6), c.add(3.8, 2.0, 0)), null, 30));
        out.add(new Beat("lineup_back", (p, c) -> view(p, c.add(0, 1.8, -6.5), c.add(0, 1.0, 0)), null, 30));
        return out;
    }

    // --- amara_hud -------------------------------------------------------------------------------------------------------

    /** Draws the bar at a fixed state over the world. */
    private static final class Bars extends Screen {
        int phase = 1;
        float health = 1f;
        final UUID id = UUID.randomUUID();

        Bars() {
            super(Component.empty());
        }

        @Override
        public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
            AmaraHud.draw(g, id, Component.translatable("entity.supernaturalcraft.amara"), phase, health, 4, partial);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    private static List<Beat> hud(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        Bars bars = new Bars();
        float[] health = {0.92f, 0.78f, 0.6f, 0.45f, 0.3f, 0.14f};
        out.add(new Beat("open", (p, c) -> view(p, c.add(0, 1.6, 6), c.add(0, 1.2, 0)), () -> {
            ClientDyingSun.apply(new DyingSunPayload(DyingSunRules.DYING, 0.4f));
            ClientAmara.fake(look(AmaraLook.MASS, 1));
            mc.setScreen(bars);
        }, 30));
        for (int ph = 1; ph <= 6; ph++) {
            int phase = ph;
            float h = health[ph - 1];
            out.add(new Beat("phase" + ph, null, () -> {
                ClientAmara.fake(look(phase == 1 ? AmaraLook.MASS : phase == 2 ? AmaraLook.GIRL : phase == 5 ? AmaraLook.VOID : AmaraLook.WOMAN, phase));
                bars.phase = phase;
                bars.health = h;
            }, 50));
            out.add(new Beat("phase" + ph + "_hit", null, () -> bars.health = h - 0.06f, 8));
        }
        out.add(new Beat("countdown", null, () -> ClientAmara.fake(new ClientAmara.Fake(AmaraLook.WOMAN, 6, 4, 0, 0.2f, false)), 30));
        out.add(new Beat("enrage", null, () -> ClientAmara.fake(new ClientAmara.Fake(AmaraLook.WOMAN, 6, 4, 0, 0f, true)), 40));
        out.add(new Beat("dawn", null, () -> {
            ClientAmara.fake(new ClientAmara.Fake(AmaraLook.SPENT, 6, 5, 0, 0f, false));
            bars.health = 0.02f;
        }, 90));
        out.add(new Beat("cards", null, () -> {
            mc.setScreen(null);
            mc.options.hideGui = false;
            ClientAmara.fake(look(AmaraLook.WOMAN, 3));
        }, 6));
        for (int i = 0; i < AmaraOverlay.TITLES; i++) {
            int which = i;
            out.add(new Beat("title" + i, null, () -> AmaraFxRenderer.add(new AmaraFxPayload(-1, AmaraFxPayload.TITLE, Vec3.ZERO, which, 0, 120)), 56));
        }
        for (int line : new int[]{2, 7, 11, 17}) {
            out.add(new Beat("voice" + line, null, () -> AmaraFxRenderer.add(new AmaraFxPayload(-1, AmaraFxPayload.VOICE, Vec3.ZERO, line, 0, 80)), 44));
        }
        out.add(new Beat("cracks", null, () -> AmaraOverlay.cracks(60), 20));
        out.add(new Beat("end", null, () -> {
            mc.options.hideGui = true;
            ClientDyingSun.apply(new DyingSunPayload(DyingSunRules.NONE, 0f));
        }, 4));
        return out;
    }

    // --- amara_fx --------------------------------------------------------------------------------------------------------

    private static Beat fxBeat(String name, int length, int form, int phase, java.util.function.Function<Vec3, AmaraFxPayload> payload) {
        return new Beat(name, null, () -> {
            ClientAmara.fake(look(form, phase));
            AmaraFxRenderer.add(payload.apply(base));
        }, length);
    }

    private static List<Beat> fx() {
        List<Beat> out = new ArrayList<>();
        int w = AmaraLook.WOMAN;
        out.add(new Beat("stage", (p, c) -> {
            clearStaged();
            amara(p.serverLevel(), c, 0);
            view(p, c.add(7, 4.5, 9), c.add(0, 1.0, 2));
        }, () -> ClientAmara.fake(look(AmaraLook.MASS, 1)), 30));
        out.add(fxBeat("slam", 18, AmaraLook.MASS, 1, c -> new AmaraFxPayload(amaraId, AmaraFxPayload.SLAM, c.add(4, 0, 5), 0, 0, 30)));
        out.add(fxBeat("sweep", 14, AmaraLook.MASS, 1, c -> new AmaraFxPayload(amaraId, AmaraFxPayload.SWEEP, c, 30, 8, 30)));
        out.add(fxBeat("spike", 10, AmaraLook.MASS, 1, c -> new AmaraFxPayload(amaraId, AmaraFxPayload.SPIKE, c.add(3, 0, 6), 0, 0, 30)));
        out.add(fxBeat("ring_out", 12, AmaraLook.MASS, 1, c -> new AmaraFxPayload(amaraId, AmaraFxPayload.RING_OUT, c, 0, 14, 30)));
        out.add(fxBeat("shatter", 40, AmaraLook.MASS, 1, c -> new AmaraFxPayload(amaraId, AmaraFxPayload.SHATTER, c, 0, 8, 60)));
        out.add(new Beat("girl", (p, c) -> view(p, c.add(4, 2.5, 7), c.add(0, 0.8, 2)), null, 4));
        out.add(fxBeat("shadow_step", 20, AmaraLook.GIRL, 2, c -> new AmaraFxPayload(amaraId, AmaraFxPayload.SHADOW_STEP, c.add(0, 0, 2), 90, 5, 30)));
        out.add(fxBeat("hide", 30, AmaraLook.GIRL, 2, c -> new AmaraFxPayload(amaraId, AmaraFxPayload.HIDE, c.add(-2, 0, 3), 0, 2, 80)));
        out.add(fxBeat("found", 10, AmaraLook.GIRL, 2, c -> new AmaraFxPayload(amaraId, AmaraFxPayload.FOUND, c.add(1, 0, 4), 0, 6, 20)));
        out.add(fxBeat("telekinesis", 30, w, 3, c -> new AmaraFxPayload(amaraId, AmaraFxPayload.TELEKINESIS, c.add(0, 0, 4), 180, 4, 60)));
        out.add(fxBeat("soul_drain", 30, w, 3, c -> new AmaraFxPayload(amaraId, AmaraFxPayload.SOUL_DRAIN, c.add(2, 0, 5), 0, 0, 60)));
        out.add(fxBeat("chuck_light", 24, w, 4, c -> new AmaraFxPayload(amaraId, AmaraFxPayload.CHUCK_LIGHT, c.add(-3, 0, 5), 0, 0.8f, 200)));
        out.add(fxBeat("chuck_wound", 24, w, 4, c -> new AmaraFxPayload(amaraId, AmaraFxPayload.CHUCK_WOUND, c.add(-3, 0, 5), 0, 0, 50)));
        out.add(fxBeat("smoke_wall", 24, w, 4, c -> new AmaraFxPayload(amaraId, AmaraFxPayload.SMOKE_WALL, c.add(3, 0, 3), 60, 8, 60)));
        out.add(new Beat("void", (p, c) -> view(p, c.add(10, 6, 12), c.add(0, 0, 0)), null, 4));
        out.add(fxBeat("crumble", 26, AmaraLook.VOID, 5, c -> new AmaraFxPayload(amaraId, AmaraFxPayload.CRUMBLE, c, 0, 9, 50)));
        out.add(fxBeat("island_sink", 20, AmaraLook.VOID, 5, c -> new AmaraFxPayload(amaraId, AmaraFxPayload.ISLAND_SINK, c.add(-6, 0, 4), 0, 4, 30)));
        out.add(fxBeat("gravity", 40, AmaraLook.VOID, 5, c -> new AmaraFxPayload(amaraId, AmaraFxPayload.GRAVITY, c, 0, 0, 100)));
        out.add(fxBeat("starfall", 34, AmaraLook.VOID, 5, c -> new AmaraFxPayload(amaraId, AmaraFxPayload.STARFALL, c.add(4, 0, 4), 0, 2.5f, 30)));
        out.add(fxBeat("sun_die", 12, w, 6, c -> new AmaraFxPayload(amaraId, AmaraFxPayload.SUN_DIE, c, 0, 0.4f, 20)));
        out.add(fxBeat("chroma_hit", 6, w, 6, c -> new AmaraFxPayload(amaraId, AmaraFxPayload.CHROMA_HIT, c, 0, 1f, 10)));
        out.add(fxBeat("flash", 4, w, 6, c -> new AmaraFxPayload(amaraId, AmaraFxPayload.FLASH, c, 2, 0.7f, 10)));
        out.add(fxBeat("reconcile", 70, AmaraLook.SPENT, 6, c -> new AmaraFxPayload(amaraId, AmaraFxPayload.RECONCILE, c.add(0, 0, 2), 0, 0, 120)));
        return out;
    }

    // --- amara_sky -------------------------------------------------------------------------------------------------------

    private static Vec3 up(Vec3 c, float x, float y, float z) {
        return c.add(x * 60, 1.6 + y * 60, z * 60);
    }

    private static List<Beat> sky() {
        List<Beat> out = new ArrayList<>();
        Runnable none = () -> ClientAmara.fake(new ClientAmara.Fake(AmaraLook.WOMAN, 3, 0, 0, 1, false));
        out.add(new Beat("plain", (p, c) -> view(p, c.add(0, 1.6, 0), up(c, 0.32f, 0.5f, -0.72f)), none, 30));
        out.add(new Beat("black_sun", null, () -> ClientAmara.fake(new ClientAmara.Fake(AmaraLook.WOMAN, 3, 1, 0, 1, false)), 40));
        out.add(new Beat("face_shut", (p, c) -> view(p, c.add(0, 1.6, 6), up(c, 0.12f, 0.86f, -0.5f)),
                () -> ClientAmara.fake(new ClientAmara.Fake(AmaraLook.VOID, 5, 2, 0, 1, false)), 40));
        out.add(new Beat("face_open", null, () -> ClientAmara.fake(new ClientAmara.Fake(AmaraLook.VOID, 5, 3, 0, 1, false)), 40));
        out.add(new Beat("gravity", null, () -> {
            ClientAmara.gravity(base, 100);
            AmaraFxRenderer.add(new AmaraFxPayload(-1, AmaraFxPayload.GRAVITY, base, 0, 0, 100));
        }, 40));
        out.add(new Beat("dying", (p, c) -> view(p, c.add(0, 1.6, 0), up(c, 0.32f, 0.5f, -0.72f)),
                () -> ClientAmara.fake(new ClientAmara.Fake(AmaraLook.WOMAN, 6, 4, 0, 0.5f, false)), 40));
        out.add(new Beat("dying_late", null, () -> ClientAmara.fake(new ClientAmara.Fake(AmaraLook.WOMAN, 6, 4, 0, 0.1f, false)), 30));
        out.add(new Beat("rage", null, () -> ClientAmara.fake(new ClientAmara.Fake(AmaraLook.WOMAN, 6, 4, 0, 0f, true)), 30));
        out.add(new Beat("dawn", null, () -> ClientAmara.fake(new ClientAmara.Fake(AmaraLook.SPENT, 6, 5, 0, 0f, false)), 40));
        out.add(new Beat("dawn_wide", (p, c) -> view(p, c.add(0, 4, 14), c.add(0, 30, -40)), null, 30));
        return out;
    }

    // --- dying_sun -------------------------------------------------------------------------------------------------------

    private static Beat darkness(String name, float d, int time, BiConsumer<ServerPlayer, Vec3> camera) {
        return new Beat(name, (p, c) -> {
            p.serverLevel().setDayTime(time);
            camera.accept(p, c);
        }, () -> ClientDyingSun.apply(new DyingSunPayload(d > 0 ? DyingSunRules.DYING : DyingSunRules.NONE, d)), 130);
    }

    private static List<Beat> dyingSun() {
        List<Beat> out = new ArrayList<>();
        BiConsumer<ServerPlayer, Vec3> sun = (p, c) -> view(p, c.add(0, 1.6, 0), c.add(50, 40, 0));
        BiConsumer<ServerPlayer, Vec3> land = (p, c) -> view(p, c.add(-14, 6, 14), c.add(0, 1, 0));
        out.add(darkness("sun_000", 0f, 3000, sun));
        out.add(darkness("sun_025", 0.25f, 3000, sun));
        out.add(darkness("sun_050", 0.5f, 3000, sun));
        out.add(darkness("sun_085", 0.85f, 3000, sun));
        out.add(darkness("land_085", 0.85f, 6000, land));
        out.add(darkness("land_000", 0f, 6000, land));
        out.add(darkness("night_000", 0f, 18000, (p, c) -> view(p, c.add(0, 1.6, 0), c.add(10, 60, 20))));
        out.add(darkness("night_060", 0.6f, 18000, (p, c) -> view(p, c.add(0, 1.6, 0), c.add(10, 60, 20))));
        out.add(darkness("night_085", 0.85f, 18000, (p, c) -> view(p, c.add(0, 1.6, 0), c.add(10, 60, 20))));
        out.add(new Beat("end", (p, c) -> p.serverLevel().setDayTime(6000), () -> ClientDyingSun.apply(new DyingSunPayload(DyingSunRules.NONE, 0f)), 4));
        return out;
    }
}
