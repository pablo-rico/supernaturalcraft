package org.papiricoh.supernaturalcraft.client.dev;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.cain.CainFarm;
import org.papiricoh.supernaturalcraft.cain.CainNpcEntity;
import org.papiricoh.supernaturalcraft.cain.CainSite;
import org.papiricoh.supernaturalcraft.cain.CainWorld;
import org.papiricoh.supernaturalcraft.cain.layout.CainFarmLayout;
import org.papiricoh.supernaturalcraft.client.allegiance.ClientAllegiance;
import org.papiricoh.supernaturalcraft.client.cain.ClientCainFx;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicFxManager;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import org.papiricoh.supernaturalcraft.entity.boss.cain.CainAssets;
import org.papiricoh.supernaturalcraft.entity.boss.cain.CainAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.cain.CainBalance;
import org.papiricoh.supernaturalcraft.entity.boss.cain.CainEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.network.CainFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Cain, the Father of Murder, and the Mark of Cain (v0.21) in the real renderer, well away from the origin, at a plain 70° with
 * no FOV effects (the dev client's own wider view stretched figures at the edges). Screenshots: {@code sn_cain_*.png} and so on.
 * <ul>
 *   <li>{@code SN_PREVIEW=cain}: the three phases side by side by day and by night, close-ups (the Mark from phase II, the eyes
 *   and the First Blade in phase III), the peaceful Cain, every clip at its blow, then each attack's telegraph and effect staged
 *   on a plain yard (the swarm, a lunge, the furrow, Abel's blood, the thrown tools, the execution, the Mark's flare), the offer of
 *   the Mark (to another hunter, to the camera, taken), and the four title cards.</li>
 *   <li>{@code SN_PREVIEW=cain_farm}: his farm placed by {@code /supernatural cain place}: from above, the gate, the yard, the
 *   house, the hives, the fields, the peaceful Cain tending, at dusk and at night.</li>
 *   <li>{@code SN_PREVIEW=cain_fight}: the farm, then a real fight called by {@code /supernatural boss summon cain}: the intro, then each
 *   phase's attacks forced one by one (a shot of the telegraph, two of the effect, at close range), the phase changes (camera and
 *   title card), and his death with the offer of the Mark.</li>
 *   <li>{@code SN_PREVIEW=mark_hud}: the bearer's Thirst by the hotbar at 0.2, 0.6 and 1.0 (the Mark on the bare arm in first
 *   person), the Mark seen in third person, another bearer's forearm (calm and in bloodlust, by day and night), and the offer and
 *   taking of the Mark in first person.</li>
 * </ul>
 */
final class CainPreview {

    static final List<String> NAMES = List.of("cain", "cain_farm", "cain_fight", "mark_hud");
    private static final int FOV = 70;
    private static final int X = 6400, Z = 6400;
    private static final int DUMMY_ID = 0x5CA1_0001;

    /** One moment: what the server sets up, what the client does, the picture's name (taken at its end), how long it lasts. */
    private record Beat(String name, BiConsumer<ServerPlayer, Vec3> server, Runnable client, int length, boolean shot) {
        Beat(String name, BiConsumer<ServerPlayer, Vec3> server, Runnable client, int length) {
            this(name, server, client, length, true);
        }
    }

    private static List<String> queue;
    private static String scene;
    private static int t;
    private static volatile Vec3 base;
    private static List<Beat> beats;
    private static int beatIndex, beatStart;
    private static final List<Entity> staged = new ArrayList<>();
    private static volatile Vec3 camFrom, camAt;
    private static int savedFov = -1;
    private static double savedFovEffect;
    private static RemotePlayer dummy;

    private CainPreview() {
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
            Fight.reset();
            MagicFxManager.clear();
            ClientCainFx.clear();
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
        if (base == null || t < 20) return true;
        Vec3 f = camFrom, a = camAt;
        if (f != null && t % 5 == 0) {
            server.execute(() -> {
                ServerPlayer p = player(server);
                if (p.position().distanceToSqr(f) > 0.09) look(p, f, a);
                p.setDeltaMovement(Vec3.ZERO);
            });
        }
        boolean done = scene.equals("cain_fight") ? Fight.tick(mc, server) : beats(mc, server);
        if (done) {
            server.execute(() -> unstage(server));
            if (dummy != null && mc.level != null) mc.level.removeEntity(DUMMY_ID, Entity.RemovalReason.DISCARDED);
            dummy = null;
            ClientAllegiance.forceFlags(mc.player, 0);
            mc.setScreen(null);
            scene = null;
        }
        return true;
    }

    // --- beats -----------------------------------------------------------------------------------------------------------------

    private static boolean beats(Minecraft mc, MinecraftServer server) {
        if (beats == null) {
            beats = switch (scene) {
                case "cain" -> model(mc);
                case "cain_farm" -> farm(mc);
                default -> markHud(mc);
            };
        }
        if (beatIndex >= beats.size()) return true;
        Beat b = beats.get(beatIndex);
        if (beatStart == 0) beatStart = t;
        int local = t - beatStart;
        Vec3 c = base;
        if (local == 0 && b.server != null) server.execute(() -> b.server.accept(player(server), c));
        if (local == 2 && b.client != null) b.client.run();
        if (b.shot && local == b.length - 2) shot(mc, b.name);
        if (local >= b.length) {
            beatIndex++;
            beatStart = t + 1;
        }
        return false;
    }

    // --- staging ---------------------------------------------------------------------------------------------------------------

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static void stage(MinecraftServer server, String which) {
        ServerPlayer p = player(server);
        ServerLevel level = server.overworld();
        p.setGameMode(GameType.SPECTATOR);
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.removeAllEffects();
        org.papiricoh.supernaturalcraft.eclipse.Eclipses.end(level);
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        level.setDayTime(6000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.setWeatherParameters(12000, 0, false, false);
        int x = X + NAMES.indexOf(which) * 160;
        level.getChunk(x >> 4, Z >> 4);
        BlockPos g = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, Z));
        for (Entity old : level.getEntitiesOfClass(Entity.class, new AABB(g).inflate(60),
                e -> e instanceof LuciferEntity || e instanceof CainNpcEntity || e instanceof ArmorStand || e instanceof TelegraphMarker)) {
            old.discard();
        }
        Vec3 b = Vec3.atBottomCenterOf(g);
        if (which.equals("cain") || which.equals("mark_hud")) floor(level, g, 20, 20);
        p.teleportTo(level, b.x, b.y + 3, b.z + 8, 180, 15);
        base = b;
    }

    private static void unstage(MinecraftServer server) {
        ServerPlayer p = player(server);
        ServerLevel level = server.overworld();
        staged.forEach(Entity::discard);
        staged.clear();
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(),
                "supernatural mark remove");
        p.getInventory().clearContent();
        p.setGameMode(GameType.SPECTATOR);
    }

    private static void floor(ServerLevel level, BlockPos g, int rx, int rz) {
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dz = -rz; dz <= rz; dz++) {
                int h = Math.floorMod(dx * 7 + dz * 13, 11);
                level.setBlockAndUpdate(g.offset(dx, -1, dz), h == 0 ? Blocks.COARSE_DIRT.defaultBlockState()
                        : h == 1 ? Blocks.PODZOL.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState());
                for (int dy = 0; dy < 10; dy++) level.setBlockAndUpdate(g.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
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

    private static CainEntity cain(ServerLevel level, Vec3 at, float yaw, int phase) {
        CainEntity c = mob(level, AllEntities.CAIN.get(), at, yaw);
        c.forceLook(phase);
        return c;
    }

    private static ArmorStand stand(ServerLevel level, Vec3 at, float yaw) {
        ArmorStand s = EntityType.ARMOR_STAND.create(level);
        s.moveTo(at.x, at.y, at.z, yaw, 0);
        level.addFreshEntity(s);
        staged.add(s);
        return s;
    }

    private static void clearStaged() {
        staged.forEach(Entity::discard);
        staged.clear();
        Minecraft.getInstance().execute(() -> {
            MagicFxManager.clear();
            ClientCainFx.clear();
        });
    }

    /** Puts the camera's eye at {@code eye} (the player's feet 1.62 below), looking at {@code at}. */
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

    private static void camera(Minecraft mc, CameraType type, boolean hud) {
        mc.execute(() -> {
            mc.options.setCameraType(type);
            mc.options.hideGui = !hud;
        });
    }

    private static void fx(ServerPlayer p, CainFxPayload payload) {
        PacketDistributor.sendToPlayer(p, payload);
    }

    private static void command(ServerPlayer p, String cmd) {
        p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(), cmd);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "sn_" + scene + "_" + name + ".png", mc.getMainRenderTarget(), m -> {
        });
    }

    private static Beat still(String name, BiConsumer<ServerPlayer, Vec3> server) {
        return new Beat(name, server, null, 30);
    }

    // --- SN_PREVIEW=cain -----------------------------------------------------------------------------------------------------

    private static List<Beat> model(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        Vec3 eye = new Vec3(0, 1.1, 0);
        // The three phases side by side, facing us (south).
        out.add(new Beat("lineup_day", (p, c) -> {
            clearStaged();
            for (int i = 1; i <= 3; i++) cain(p.serverLevel(), c.add((i - 2) * 2.6, 0, 0), 0, i);
            view(p, c.add(0, 1.7, 7.5), c.add(0, 1.0, 0));
        }, () -> camera(mc, CameraType.FIRST_PERSON, false), 40));
        out.add(still("lineup_night", (p, c) -> p.serverLevel().setDayTime(18000)));
        out.add(new Beat("p1_front", (p, c) -> {
            clearStaged();
            p.serverLevel().setDayTime(6000);
            cain(p.serverLevel(), c, 0, 1);
            view(p, c.add(0.3, 1.6, 2.6), c.add(eye));
        }, null, 30));
        out.add(still("p1_back", (p, c) -> view(p, c.add(-0.3, 1.6, -2.6), c.add(eye))));
        out.add(still("p1_side", (p, c) -> view(p, c.add(2.6, 1.6, 0.2), c.add(eye))));
        out.add(still("p1_face", (p, c) -> view(p, c.add(0.1, 1.75, 1.1), c.add(0, 1.7, 0))));
        out.add(new Beat("p2_arm", (p, c) -> {
            clearStaged();
            cain(p.serverLevel(), c, 0, 2);
            // His right forearm is on his west side (he faces south).
            view(p, c.add(-1.3, 1.35, 1.3), c.add(-0.35, 0.95, 0));
        }, null, 40));
        out.add(still("p2_arm_night", (p, c) -> p.serverLevel().setDayTime(18000)));
        out.add(new Beat("p3_front_night", (p, c) -> {
            clearStaged();
            cain(p.serverLevel(), c, 0, 3);
            view(p, c.add(0.3, 1.6, 2.6), c.add(eye));
        }, null, 70));
        out.add(still("p3_eyes_night", (p, c) -> view(p, c.add(0.05, 1.72, 0.95), c.add(0, 1.68, 0))));
        out.add(still("p3_blade", (p, c) -> view(p, c.add(-1.6, 1.4, 1.7), c.add(-0.45, 0.9, 0))));
        out.add(still("p3_eyes_day", (p, c) -> {
            p.serverLevel().setDayTime(6000);
            view(p, c.add(0.05, 1.72, 0.95), c.add(0, 1.68, 0));
        }));
        out.add(new Beat("npc_front", (p, c) -> {
            clearStaged();
            mob(p.serverLevel(), AllEntities.CAIN_NPC.get(), c, 0);
            view(p, c.add(0.3, 1.6, 2.6), c.add(eye));
        }, null, 30));
        // Every clip he plays once, caught at its blow (phase III: the blade in hand).
        for (String clip : CainAssets.TRIGGERED) {
            int at = CainAssets.HIT_TICKS.getOrDefault(clip, 20);
            out.add(new Beat("clip_" + clip, (p, c) -> {
                clearStaged();
                CainEntity e = cain(p.serverLevel(), c, 0, clip.equals("swarm") || clip.equals("lunge") ? 1 : 3);
                e.triggerAnim("action", clip);
                view(p, c.add(2.0, 1.6, 2.4), c.add(eye));
            }, null, at + 6));
        }
        // The attacks, staged on the plain yard: the telegraph alone, then the effect over it.
        out.add(new Beat("swarm_telegraph", (p, c) -> {
            clearStaged();
            ServerLevel level = p.serverLevel();
            cain(level, c, 0, 1).triggerAnim("action", "swarm");
            TelegraphMarker.circle(level, c.add(0, 0, 5), CainBalance.SWARM_RADIUS, TelegraphMarker.YELLOW, 100);
            TelegraphMarker.circle(level, c.add(5, 0, 2), CainBalance.SWARM_RADIUS, TelegraphMarker.YELLOW, 100);
            view(p, c.add(-3, 4.5, 11), c.add(1.5, 0.6, 3.5));
        }, null, 14));
        out.add(new Beat("swarm", (p, c) -> {
            for (Vec3 s : List.of(c.add(0, 0, 5), c.add(5, 0, 2))) {
                fx(p, new CainFxPayload(-1, CainFxPayload.SWARM, Math.round(CainBalance.SWARM_RADIUS), 0, s, CainBalance.SWARM_TICKS));
            }
        }, null, 26));
        out.add(still("swarm_close", (p, c) -> view(p, c.add(1.5, 1.6, 9.5), c.add(0, 1.2, 5))));
        out.add(new Beat("lunge_telegraph", (p, c) -> {
            clearStaged();
            cain(p.serverLevel(), c, 0, 1).triggerAnim("action", "lunge");
            TelegraphMarker.line(p.serverLevel(), c, 0, CainBalance.LUNGE_WIDTH, CainBalance.LUNGE_REACH, TelegraphMarker.RED, 60);
            view(p, c.add(6, 3.2, 6), c.add(0, 0.5, 5));
        }, null, 14));
        out.add(new Beat("lunge", (p, c) -> fx(p, new CainFxPayload(staged.getFirst().getId(), CainFxPayload.LUNGE, 1, 0,
                c.add(0, 0, CainBalance.LUNGE_REACH - 1), 10)), null, 5));
        out.add(new Beat("lunge_late", (p, c) -> {
        }, null, 7));
        out.add(new Beat("furrow_telegraph", (p, c) -> {
            clearStaged();
            cain(p.serverLevel(), c, 0, 2).triggerAnim("action", "furrow");
            TelegraphMarker.cone(p.serverLevel(), c, 0, CainBalance.FURROW_REACH, TelegraphMarker.RED, 40);
            view(p, c.add(7, 4, 9), c.add(0, 0.5, 4));
        }, null, 18));
        out.add(new Beat("furrow", (p, c) -> fx(p, new CainFxPayload(staged.getFirst().getId(), CainFxPayload.FURROW, 0,
                Math.round(CainBalance.FURROW_REACH), c, 20)), null, 6));
        out.add(new Beat("furrow_wave", (p, c) -> {
        }, null, 8));
        out.add(new Beat("furrow_scar", (p, c) -> {
        }, null, 26));
        out.add(new Beat("abels_blood_telegraph", (p, c) -> {
            clearStaged();
            ServerLevel level = p.serverLevel();
            cain(level, c, 0, 2).triggerAnim("action", "abels_blood");
            TelegraphMarker.ring(level, c, CainBalance.ABEL_OUTER, TelegraphMarker.RED, 60);
            TelegraphMarker.circle(level, c, CainBalance.ABEL_INNER, TelegraphMarker.SAFE, 60);
            view(p, c.add(4, 6.5, 11), c.add(0, 0, 1));
        }, null, 22));
        out.add(new Beat("abels_blood", (p, c) -> {
            spikes(p.serverLevel(), c);
            fx(p, new CainFxPayload(staged.getFirst().getId(), CainFxPayload.SPIKES, Math.round(CainBalance.ABEL_OUTER * 10),
                    Math.round(CainBalance.ABEL_INNER * 10), c, CainBalance.ABEL_SPIKE_TICKS));
        }, null, 5));
        out.add(new Beat("abels_blood_mist", (p, c) -> {
        }, null, 20));
        out.add(still("abels_blood_close", (p, c) -> view(p, c.add(2.5, 1.4, 7.5), c.add(0, 0.5, 5))));
        out.add(new Beat("tools_telegraph", (p, c) -> {
            clearSpikes(p.serverLevel(), c);
            clearStaged();
            CainEntity e = cain(p.serverLevel(), c, 0, 2);
            e.triggerAnim("action", "throw_tools");
            for (int i = 0; i < 3; i++) {
                TelegraphMarker.line(p.serverLevel(), c, -25 + i * 25, CainBalance.TOOLS_WIDTH, 11, TelegraphMarker.VIOLET, 60);
            }
            view(p, c.add(7, 3.5, 10), c.add(0, 0.8, 4));
        }, null, 16));
        out.add(new Beat("tools", (p, c) -> {
            for (int i = 0; i < 3; i++) {
                float yaw = -25 + i * 25;
                Vec3 land = c.add(Vec3.directionFromRotation(0, yaw).scale(11));
                fx(p, new CainFxPayload(staged.getFirst().getId(), CainFxPayload.TOOLS, i, 0, land, CainBalance.TOOLS_FLIGHT));
            }
        }, null, 5));
        out.add(new Beat("tools_stuck", (p, c) -> {
        }, null, 12));
        out.add(still("tools_close", (p, c) -> view(p, c.add(1.5, 1.2, 13.5), c.add(0, 0.4, 10.5))));
        out.add(new Beat("execution_telegraph", (p, c) -> {
            clearStaged();
            ServerLevel level = p.serverLevel();
            cain(level, c, 0, 3);
            stand(level, c.add(0, 0, 5), 0);
            TelegraphMarker.circle(level, c.add(0, 0, 5), CainBalance.EXECUTION_RADIUS, TelegraphMarker.RED, 60);
            view(p, c.add(5, 3, 9), c.add(0, 0.8, 4));
        }, null, 16));
        out.add(new Beat("execution", (p, c) -> {
            CainEntity e = (CainEntity) staged.getFirst();
            Vec3 behind = c.add(0, 0, 5 + CainBalance.EXECUTION_BEHIND);
            fx(p, new CainFxPayload(e.getId(), CainFxPayload.EXECUTION, staged.get(1).getId(), 0, behind, 16));
            e.teleportTo(behind.x, behind.y, behind.z);
            e.setYRot(180);
            e.setYBodyRot(180);
            e.setYHeadRot(180);
            e.triggerAnim("action", "execution");
        }, null, 4));
        out.add(new Beat("execution_blade", (p, c) -> {
        }, null, 5));
        out.add(new Beat("flare_rune", (p, c) -> {
            clearStaged();
            ServerLevel level = p.serverLevel();
            level.setDayTime(18000);
            CainEntity e = cain(level, c, 0, 3);
            float r = 13;
            TelegraphMarker.ring(level, c, r, TelegraphMarker.RED, 80);
            fx(p, new CainFxPayload(e.getId(), CainFxPayload.MARK_FLARE, Math.round(r * 10), 3, c, CainBalance.FLARE_WINDUP + 4));
            for (int i = 0; i < 3; i++) {
                double a = Math.PI * 2 * i / 3 + 0.4;
                Vec3 s = c.add(Math.cos(a) * 9.5, 0, Math.sin(a) * 9.5);
                TelegraphMarker.circle(level, s, CainBalance.FLARE_SAFE_RADIUS, TelegraphMarker.SAFE, 80);
                fx(p, new CainFxPayload(e.getId(), CainFxPayload.FLARE_SHELTER, i, Math.round(CainBalance.FLARE_SAFE_RADIUS * 10), s,
                        CainBalance.FLARE_WINDUP + 4));
            }
            view(p, c.add(0, 9, 16), c.add(0, 0, 1));
        }, null, 16));
        out.add(new Beat("flare_charged", (p, c) -> ((CainEntity) staged.getFirst()).triggerAnim("action", "mark_flare"), null, 26));
        out.add(new Beat("flare_burst", (p, c) -> {
        }, null, 11));
        out.add(new Beat("flare_fire", (p, c) -> {
        }, null, 8));
        out.add(new Beat("flare_rune_close", (p, c) -> {
            clearStaged();
            ServerLevel level = p.serverLevel();
            CainEntity e = cain(level, c, 0, 3);
            TelegraphMarker.ring(level, c, 13, TelegraphMarker.RED, 80);
            fx(p, new CainFxPayload(e.getId(), CainFxPayload.MARK_FLARE, 130, 0, c, 60));
            view(p, c.add(0.5, 4.5, 4.5), c.add(0, 0, 0.3));
        }, null, 40));
        // The offer of the Mark: to another hunter (an armour stand stands in), to the camera, then taken.
        out.add(new Beat("offer_other", (p, c) -> {
            clearStaged();
            ServerLevel level = p.serverLevel();
            CainEntity e = cain(level, c, 0, 3);
            e.triggerAnim("action", "death");
            ArmorStand s = stand(level, c.add(0, 0, 3.2), 180);
            fx(p, new CainFxPayload(e.getId(), CainFxPayload.OFFER, s.getId(), 0, e.position(), CainBalance.OFFER_TICKS));
            view(p, c.add(3.4, 1.8, 2.2), c.add(0, 1.3, 1.6));
        }, null, 40));
        out.add(new Beat("offer_taken", (p, c) -> fx(p, new CainFxPayload(-1, CainFxPayload.OFFER, staged.get(1).getId(), 1,
                staged.get(1).position(), 60)), null, 8));
        out.add(new Beat("offer_taken_flash", (p, c) -> {
        }, null, 10));
        out.add(new Beat("offer_camera", (p, c) -> {
            clearStaged();
            CainEntity e = cain(p.serverLevel(), c, 0, 3);
            e.triggerAnim("action", "death");
            view(p, c.add(0, 1.62, 3.0), c.add(0, 0.9, 0));
            fx(p, new CainFxPayload(e.getId(), CainFxPayload.OFFER, p.getId(), 0, e.position(), CainBalance.OFFER_TICKS));
        }, null, 40));
        // The title cards (GUI on).
        for (int i = 1; i <= 4; i++) {
            int phase = Math.min(i, 3);
            boolean fall = i == 4;
            out.add(new Beat(fall ? "title_death" : "title_phase" + phase, (p, c) -> {
                clearStaged();
                p.serverLevel().setDayTime(fall ? 18000 : 12500);
                cain(p.serverLevel(), c, 0, phase);
                view(p, c.add(0.3, 1.6, 3.2), c.add(eye));
                fx(p, new CainFxPayload(-1, CainFxPayload.TITLE, phase, fall ? 1 : 0, c, fall ? 100 : 90));
            }, () -> camera(mc, CameraType.FIRST_PERSON, true), 36));
        }
        return out;
    }

    /** Abel's blood's spikes, as the attack raises them (every other block of the ring). */
    private static void spikes(ServerLevel level, Vec3 centre) {
        var spike = Blocks.POINTED_DRIPSTONE.defaultBlockState().setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.UP)
                .setValue(PointedDripstoneBlock.THICKNESS, DripstoneThickness.TIP);
        BlockPos o = BlockPos.containing(centre);
        int r = (int) Math.ceil(CainBalance.ABEL_OUTER);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (Math.floorMod(dx + dz, 2) != 0 || !CainBalance.inAbelsRing(Math.sqrt(dx * dx + dz * dz))) continue;
                level.setBlockAndUpdate(o.offset(dx, -1, dz), Blocks.ROOTED_DIRT.defaultBlockState());
                level.setBlockAndUpdate(o.offset(dx, 0, dz), spike);
            }
        }
    }

    private static void clearSpikes(ServerLevel level, Vec3 centre) {
        BlockPos o = BlockPos.containing(centre);
        int r = (int) Math.ceil(CainBalance.ABEL_OUTER);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (level.getBlockState(o.offset(dx, 0, dz)).is(Blocks.POINTED_DRIPSTONE)) {
                    level.setBlockAndUpdate(o.offset(dx, 0, dz), Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(o.offset(dx, -1, dz), Blocks.GRASS_BLOCK.defaultBlockState());
                }
            }
        }
    }

    // --- SN_PREVIEW=cain_farm ------------------------------------------------------------------------------------------------

    private static List<Beat> farm(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        BlockPos[] origin = new BlockPos[1];
        out.add(new Beat("placed", (p, c) -> {
            p.teleportTo(p.serverLevel(), c.x, c.y + 1, c.z, 180, 0);
            command(p, "supernatural cain place");
            origin[0] = CainFarm.originOn(p.serverLevel(), (int) Math.floor(c.x), (int) Math.floor(c.z));
            CainNpcEntity npc = CainWorld.spawnNpc(p.serverLevel(), new CainSite.Site(origin[0], 0));
            if (npc != null) staged.add(npc);
            Vec3 o = Vec3.atBottomCenterOf(origin[0]);
            view(p, o.add(34, 26, 34), o.add(0, 0, -4));
        }, () -> camera(mc, CameraType.FIRST_PERSON, false), 80));
        out.add(still("above", (p, c) -> {
            Vec3 o = Vec3.atBottomCenterOf(origin[0]);
            view(p, o.add(0.01, 52, 6), o);
        }));
        out.add(still("gate", (p, c) -> view(p, at(origin[0], CainFarmLayout.ENTRY).add(0, 1.62, 4), at(origin[0], CainFarmLayout.NPC_HOME)
                .add(0, 2, 0))));
        out.add(still("yard_from_porch", (p, c) -> view(p, at(origin[0], CainFarmLayout.NPC_HOME).add(0, 1.62, 4),
                at(origin[0], CainFarmLayout.ARENA_CENTRE).add(0, 0.5, 6))));
        out.add(still("house", (p, c) -> view(p, at(origin[0], CainFarmLayout.ARENA_CENTRE).add(4, 2.4, -6),
                at(origin[0], CainFarmLayout.NPC_HOME).add(0, 2.5, -2))));
        out.add(still("hives", (p, c) -> {
            Vec3 h = at(origin[0], CainFarmLayout.HIVES.get(1));
            view(p, h.add(-5, 2.2, 3), h.add(0, 0.6, 0));
        }));
        out.add(still("fields", (p, c) -> {
            Vec3 f = at(origin[0], CainFarmLayout.FIELDS.getFirst());
            view(p, f.add(-4, 3.2, 6), f.add(2, 0, 0));
        }));
        out.add(still("grave_spot", (p, c) -> {
            Vec3 g = at(origin[0], CainFarmLayout.GRAVE);
            view(p, g.add(-3, 2, 4), g);
        }));
        out.add(new Beat("npc", (p, c) -> {
            Entity npc = staged.isEmpty() ? null : staged.getFirst();
            if (npc == null) return;
            Vec3 n = npc.position();
            float yaw = npc.getYRot() * (float) Math.PI / 180;
            Vec3 front = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
            view(p, n.add(front.scale(2.8)).add(0.4, 1.6, 0), n.add(0, 1.2, 0));
        }, null, 60));
        out.add(still("npc_later", (p, c) -> {
            Entity npc = staged.isEmpty() ? null : staged.getFirst();
            if (npc == null) return;
            view(p, npc.position().add(3, 2.2, 3), npc.position().add(0, 1.0, 0));
        }));
        out.add(new Beat("dusk", (p, c) -> {
            p.serverLevel().setDayTime(12600);
            Vec3 o = Vec3.atBottomCenterOf(origin[0]);
            view(p, o.add(-26, 14, 28), o.add(0, 2, -6));
        }, null, 40));
        out.add(still("night", (p, c) -> p.serverLevel().setDayTime(18000)));
        out.add(still("night_yard", (p, c) -> view(p, at(origin[0], CainFarmLayout.ENTRY).add(0, 1.62, 0), at(origin[0], CainFarmLayout.NPC_HOME)
                .add(0, 2, 0))));
        return out;
    }

    private static Vec3 at(BlockPos origin, org.papiricoh.supernaturalcraft.layout.LayoutPoint p) {
        return Vec3.atBottomCenterOf(CainFarm.at(origin, 0, p));
    }

    // --- SN_PREVIEW=mark_hud -------------------------------------------------------------------------------------------------

    private static List<Beat> markHud(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        out.add(new Beat("thirst_02", (p, c) -> {
            p.setGameMode(GameType.SURVIVAL);
            p.getAbilities().invulnerable = true;
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            p.getInventory().clearContent();
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            command(p, "supernatural mark grant");
            command(p, "supernatural mark thirst 0.2");
            camFrom = null;
            p.teleportTo(p.serverLevel(), c.x, c.y, c.z + 3, 180, 20);
        }, () -> camera(mc, CameraType.FIRST_PERSON, true), 60));
        out.add(still("thirst_02_b", null));
        out.add(new Beat("thirst_06", (p, c) -> command(p, "supernatural mark thirst 0.6"), null, 40));
        out.add(new Beat("thirst_06_beat", (p, c) -> {
        }, null, 11));
        out.add(new Beat("thirst_06_rest", (p, c) -> {
        }, null, 9));
        out.add(new Beat("thirst_10", (p, c) -> command(p, "supernatural mark thirst 1.0"), null, 40));
        out.add(new Beat("thirst_10_b", (p, c) -> {
        }, null, 7));
        out.add(new Beat("thirst_10_night", (p, c) -> p.serverLevel().setDayTime(18000), null, 30));
        out.add(new Beat("third_person_front", (p, c) -> p.serverLevel().setDayTime(6000),
                () -> camera(mc, CameraType.THIRD_PERSON_FRONT, false), 30));
        out.add(new Beat("third_person_night", (p, c) -> p.serverLevel().setDayTime(18000),
                () -> camera(mc, CameraType.THIRD_PERSON_FRONT, false), 30));
        // Another bearer: a stand-in player, his right forearm toward the camera.
        out.add(new Beat("other_calm", (p, c) -> {
            p.serverLevel().setDayTime(6000);
            p.setGameMode(GameType.SPECTATOR);
            view(p, c.add(-0.75, 1.25, 1.45), c.add(-0.32, 0.95, 0));
        }, () -> {
            camera(mc, CameraType.FIRST_PERSON, false);
            spawnDummy(mc, base, Allegiances.MARKED);
        }, 40));
        out.add(new Beat("other_bloodlust", (p, c) -> {
        }, () -> ClientAllegiance.forceFlags(dummy, Allegiances.MARKED | Allegiances.MARK_BLOODLUST), 20));
        out.add(new Beat("other_bloodlust_beat", (p, c) -> {
        }, null, 7));
        out.add(new Beat("other_night", (p, c) -> p.serverLevel().setDayTime(18000), null, 30));
        out.add(still("other_full", (p, c) -> view(p, c.add(0.6, 1.5, 3.2), c.add(0, 1.0, 0))));
        // The offer of the Mark before the camera, then taken.
        out.add(new Beat("offer_fp", (p, c) -> {
            p.serverLevel().setDayTime(18000);
            command(p, "supernatural mark remove");
            p.setGameMode(GameType.SURVIVAL);
            p.getAbilities().invulnerable = true;
            p.onUpdateAbilities();
            camFrom = null;
            p.teleportTo(p.serverLevel(), c.x, c.y, c.z + 3, 180, 5);
            fx(p, new CainFxPayload(-1, CainFxPayload.OFFER, p.getId(), 0, p.position(), CainBalance.OFFER_TICKS));
        }, () -> {
            camera(mc, CameraType.FIRST_PERSON, true);
            if (dummy != null && mc.level != null) mc.level.removeEntity(DUMMY_ID, Entity.RemovalReason.DISCARDED);
            dummy = null;
        }, 40));
        out.add(new Beat("taken_fp", (p, c) -> {
            command(p, "supernatural mark grant");
            fx(p, new CainFxPayload(-1, CainFxPayload.OFFER, p.getId(), 1, p.position(), 60));
        }, null, 7));
        out.add(new Beat("taken_fp_late", (p, c) -> {
        }, null, 14));
        out.add(new Beat("taken_fp_marked", (p, c) -> {
        }, null, 30));
        return out;
    }

    /** A stand-in for another player, facing the camera's south, with these allegiance flags. */
    private static void spawnDummy(Minecraft mc, Vec3 at, int flags) {
        if (mc.level == null) return;
        if (dummy != null) mc.level.removeEntity(DUMMY_ID, Entity.RemovalReason.DISCARDED);
        dummy = new RemotePlayer(mc.level, new GameProfile(UUID.nameUUIDFromBytes("sn-cain-mark".getBytes()), "Dean"));
        dummy.setId(DUMMY_ID);
        dummy.moveTo(at.x, at.y, at.z, 0, 0);
        dummy.setYHeadRot(0);
        dummy.setYBodyRot(0);
        dummy.yBodyRotO = 0;
        dummy.yHeadRotO = 0;
        mc.level.addEntity(dummy);
        ClientAllegiance.forceFlags(dummy, flags);
    }

    // --- SN_PREVIEW=cain_fight -----------------------------------------------------------------------------------------------

    /** The real fight: the farm, his summoning, each attack forced in turn and photographed, the phase changes, his death. */
    static final class Fight {

        /** An attack to force: its phase, name, the attack, its windup (ticks), and the camera (distance, height, look bias). */
        record Step(int phase, String name, Supplier<BossAttack<LuciferEntity>> attack, int windup, double dist, double height,
                    double lookBias, int[] after) {
        }

        private static final List<Step> STEPS = List.of(
                new Step(1, "lunge", CainAttacks.Lunge::new, CainBalance.LUNGE_WINDUP, 9, 4, 0.45, new int[]{2, 18, 34}),
                new Step(1, "swarm", CainAttacks.Swarm::new, CainBalance.SWARM_WINDUP, 7, 6.5, 0.15, new int[]{4, 24}),
                new Step(2, "furrow", CainAttacks.Furrow::new, CainBalance.FURROW_WINDUP, 6.5, 5, 0.5, new int[]{3, 10, 30}),
                new Step(2, "abels_blood", CainAttacks.AbelsBlood::new, CainBalance.ABEL_WINDUP, 11, 8, 1.0, new int[]{3, 20}),
                new Step(2, "throw_tools", CainAttacks.ThrowTools::new, CainBalance.TOOLS_WINDUP, 10, 4, 0.45, new int[]{2, 5, 14}),
                new Step(3, "execution", CainAttacks.Execution::new, CainBalance.EXECUTION_WINDUP, 6, 6, 0.0, new int[]{2, 6}),
                new Step(3, "mark_flare", CainAttacks.MarkFlare::new, CainBalance.FLARE_WINDUP, 12, 15, 1.0, new int[]{3, 12}));

        private static volatile int bossId = -1;
        private static int step, queuedAt = -1, windupAt = -1, activeAt = -1, transitionAt = -1, deathAt = -1, summonedAt = -1;
        private static int idleSince = -1;

        static void reset() {
            bossId = -1;
            step = 0;
            queuedAt = windupAt = activeAt = transitionAt = deathAt = summonedAt = idleSince = -1;
        }

        static boolean tick(Minecraft mc, MinecraftServer server) {
            Vec3 c = base;
            if (t == 20) {
                mc.options.hideGui = false;
                server.execute(() -> {
                    ServerPlayer p = player(server);
                    ServerLevel level = p.serverLevel();
                    level.setDayTime(12300);
                    p.teleportTo(level, c.x, c.y + 1, c.z, 180, 0);
                    command(p, "supernatural cain place");
                    BlockPos origin = CainFarm.originOn(level, (int) Math.floor(c.x), (int) Math.floor(c.z));
                    Vec3 centre = Vec3.atBottomCenterOf(CainFarm.at(origin, 0, CainFarmLayout.ARENA_CENTRE));
                    p.setGameMode(GameType.SURVIVAL);
                    p.getAbilities().invulnerable = true;
                    p.getAbilities().mayfly = true;
                    p.getAbilities().flying = false;
                    p.onUpdateAbilities();
                    camFrom = null;
                    p.teleportTo(level, centre.x, centre.y, centre.z + 3, 180, 0);
                    base = centre;
                });
            }
            if (t == 70) {
                server.execute(() -> {
                    ServerPlayer p = player(server);
                    command(p, "supernatural boss summon cain");
                    CainEntity boss = p.serverLevel().getEntitiesOfClass(CainEntity.class, p.getBoundingBox().inflate(64)).stream()
                            .findFirst().orElse(null);
                    bossId = boss == null ? -1 : boss.getId();
                });
                summonedAt = t;
            }
            if (summonedAt < 0) return false;
            int since = t - summonedAt;
            if (since == 20 || since == 50 || since == 85) shot(mc, "intro_" + since);
            if (since > 400 && bossId < 0) return true;
            CainEntity boss = mc.level == null || bossId < 0 ? null : mc.level.getEntity(bossId) instanceof CainEntity e ? e : null;
            // Keep the hunter whole and still.
            if (t % 4 == 0) server.execute(() -> {
                ServerPlayer p = player(server);
                p.setHealth(p.getMaxHealth());
                p.getFoodData().setFoodLevel(20);
                p.removeAllEffects();
            });
            if (deathAt >= 0) return death(mc, server, boss);
            if (boss == null || since < 110) return false;
            byte state = boss.state();
            if (transitionAt >= 0) {
                int k = t - transitionAt;
                if (k == 14 || k == 42 || k == 70) shot(mc, "phase" + boss.lookPhase() + "_transition_" + k);
                if (state != LuciferEntity.IDLE && state != LuciferEntity.TRANSITION || k < 30) return false;
                if (state == LuciferEntity.TRANSITION) return false;
                transitionAt = -1;
                return false;
            }
            if (step >= STEPS.size()) {
                if (state == LuciferEntity.IDLE) {
                    deathAt = t;
                    int id = bossId;
                    server.execute(() -> {
                        ServerPlayer p = player(server);
                        if (p.serverLevel().getEntity(id) instanceof CainEntity b) {
                            b.setAbsorptionAmount(0);
                            BossHealthGuard.set(b, 1f);
                            b.invulnerableTime = 0;
                            b.hurt(AllDamageTypes.source(p.serverLevel(), AllDamageTypes.SMITE, p), 50f);
                        }
                        placeCamera(p, null, 9, 3.5, 0.6);
                    });
                }
                return false;
            }
            Step s = STEPS.get(step);
            if (queuedAt < 0) {
                if (state != LuciferEntity.IDLE) {
                    idleSince = -1;
                    return false;
                }
                if (idleSince < 0) idleSince = t;
                if (t - idleSince < 6) return false;
                if (boss.lookPhase() < s.phase) {
                    transitionAt = t;
                    int to = s.phase, id = bossId;
                    server.execute(() -> {
                        ServerPlayer p = player(server);
                        if (p.serverLevel().getEntity(id) instanceof CainEntity b) {
                            BossHealthGuard.set(b, b.getMaxHealth() * (CainBalance.PHASES - to + 1) / CainBalance.PHASES + 1);
                            b.beginTransition(to);
                            placeCamera(p, b, 8, 2.6, 0.9);
                        }
                    });
                    return false;
                }
                queuedAt = t;
                windupAt = activeAt = -1;
                int id = bossId;
                server.execute(() -> {
                    ServerPlayer p = player(server);
                    if (p.serverLevel().getEntity(id) instanceof CainEntity b) {
                        placeCamera(p, b, s.dist, s.height, s.lookBias);
                        b.queue(s.attack);
                    }
                });
                return false;
            }
            String name = "phase" + s.phase + "_" + s.name;
            if (windupAt < 0 && state == LuciferEntity.WINDUP) windupAt = t;
            if (windupAt >= 0 && activeAt < 0 && t == windupAt + Math.max(4, (int) (s.windup * 0.7))) shot(mc, name + "_telegraph");
            if (activeAt < 0 && state == LuciferEntity.ACTIVE) activeAt = t;
            if (activeAt >= 0) {
                for (int k : s.after) if (t == activeAt + k) shot(mc, name + "_fx_" + k);
            }
            int last = activeAt < 0 ? 0 : activeAt + s.after[s.after.length - 1];
            boolean finished = activeAt >= 0 && t > last && (state == LuciferEntity.IDLE || state == LuciferEntity.RECOVER && t > last + 30);
            if (finished || t - queuedAt > 220) {
                if (activeAt < 0) org.papiricoh.supernaturalcraft.SupernaturalCraft.LOGGER.warn("[preview] Cain never used {}", s.name);
                step++;
                queuedAt = -1;
                idleSince = -1;
            }
            return false;
        }

        /** His death, the offer of the Mark (its panel and the Mark before the camera), the victory. */
        private static boolean death(Minecraft mc, MinecraftServer server, CainEntity boss) {
            int k = t - deathAt;
            if (k == 16 || k == 40 || k == 70 || k == 110 || k == 150 || k == 190) shot(mc, "death_" + k);
            if (k == 205) mc.setScreen(null);
            if (k == 240) shot(mc, "victory");
            return k >= 260;
        }

        /**
         * The camera: the hunter hovering {@code dist} out from Cain (on the side they stand now) and {@code height} up, looking at
         * the point {@code lookBias} of the way from the hunter's feet to him.
         */
        private static void placeCamera(ServerPlayer p, CainEntity b, double dist, double height, double lookBias) {
            ServerLevel level = p.serverLevel();
            if (b == null) {
                b = level.getEntitiesOfClass(CainEntity.class, p.getBoundingBox().inflate(64)).stream().findFirst().orElse(null);
                if (b == null) return;
            }
            Vec3 bp = b.position();
            Vec3 dir = p.position().subtract(bp).multiply(1, 0, 1);
            dir = dir.lengthSqr() < 0.5 ? new Vec3(0, 0, 1) : dir.normalize();
            Vec3 feet = bp.add(dir.scale(dist));
            Vec3 floor = new Vec3(feet.x, bp.y, feet.z);
            Vec3 at = floor.lerp(bp, lookBias).add(0, 0.6, 0);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            view(p, floor.add(0, height, 0), at);
        }
    }
}
