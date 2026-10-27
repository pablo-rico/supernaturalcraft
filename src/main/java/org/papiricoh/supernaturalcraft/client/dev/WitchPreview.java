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
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.bowl.spell.TaglockItem;
import org.papiricoh.supernaturalcraft.client.magic.fx.ClientMagicFx;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicFxManager;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicOverlay;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaNpcEntity;
import org.papiricoh.supernaturalcraft.entity.witch.CovenWitchEntity;
import org.papiricoh.supernaturalcraft.entity.witch.FamiliarEntity;
import org.papiricoh.supernaturalcraft.entity.witch.HedgeWitchEntity;
import org.papiricoh.supernaturalcraft.entity.witch.HexedVillagerEntity;
import org.papiricoh.supernaturalcraft.entity.witch.WitchEntity;
import org.papiricoh.supernaturalcraft.entity.witch.WitchMagic;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code SN_PREVIEW=witches,coven,church,rowena_house,rowena_npc} (any of them, in any order): the coven (v0.21), staged far from
 * the origin. Screenshots {@code sn_<scene>_<moment>.png}, wide and close, each scene ending by day ({@code _day}). Shot at a
 * plain 70° with no flying zoom (restored at the end): the dev client's wider view stretched figures at the edges of the frame
 * into what looked like bodies lying down. Camera positions are the eye's.
 * <ul>
 *   <li>{@code witches}: the lineup (a Hedge Witch, a shielded Coven Witch, a cat and a perched raven each holding a taglock, a
 *   hexed villager) wide and close; casting glow off/on/faded; the shield struck (cracks gathering), broken and raised again; a
 *   smoke teleport through the real TELEPORT path to a known spot (origin close, arrival close, then back with the camera
 *   waiting at the arrival); a yellow (boss) blink's gathering close; a villager freed close (sigils, ring, golden burst); the
 *   familiar's eyes at the edges of the view (raven, then cat; the GUI shown for those two shots).</li>
 *   <li>{@code coven}, {@code church}, {@code rowena_house}: built by their {@code /supernatural coven …} commands where the camera
 *   stands, then shot from two high corners, close, and by day (Rowena's house: from its door and inside by her desk too).</li>
 *   <li>{@code rowena_npc}: Rowena at home close up — idle, reading (the camera a spectator, since she stops reading when
 *   someone is near), talking (her face at 3 blocks), handing a page over (her casting glow, at 3 blocks), a yellow smoke
 *   blink.</li>
 * </ul>
 */
final class WitchPreview {

    static final List<String> NAMES = List.of("witches", "coven", "church", "rowena_house", "rowena_npc", "haunted_house");
    private static final int X = 1400, Z = 1400;
    private static final long HAUNTED_SEED = 23L;

    private static List<String> queue;
    private static String scene;
    private static int t;
    private static volatile Vec3 base;
    private static final List<Entity> staged = new ArrayList<>();
    private static HedgeWitchEntity hedge;
    private static CovenWitchEntity coven;
    private static HexedVillagerEntity hexed;
    private static RowenaNpcEntity rowena;
    private static Vec3 camFrom, camAt;
    private static Vec3 houseOrigin;
    private static int savedFov = -1;
    private static double savedFovEffect;
    /** The preview's eye: a plain 70 degrees (the dev client's own 100 plus the flying zoom stretched figures at the edges). */
    private static final int FOV = 70;

    private WitchPreview() {
    }

    static boolean tick(Minecraft mc) {
        String env = DevPreview.env();
        if (env == null) return false;
        if (queue == null) {
            List<String> want = new ArrayList<>();
            for (String s : env.split(",")) want.add(s.trim());
            if (want.isEmpty() || !NAMES.containsAll(want)) return false;
            queue = want;
            mc.options.hideGui = true;
            savedFov = mc.options.fov().get();
            savedFovEffect = mc.options.fovEffectScale().get();
            mc.options.fov().set(FOV);
            mc.options.fovEffectScale().set(0.0);
        }
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
            MagicFxManager.clear();
        }
        t++;
        MinecraftServer server = mc.getSingleplayerServer();
        if (t == 0) {
            String which = scene;
            server.execute(() -> stage(server, which));
            return true;
        }
        if (base == null) return true;
        if (camFrom != null && t % 5 == 0) {
            Vec3 f = camFrom, a = camAt;
            server.execute(() -> {
                ServerPlayer p = player(server);
                if (p.position().distanceToSqr(f) > 0.25) look(p, f, a);
            });
        }
        boolean done = switch (scene) {
            case "witches" -> witches(mc, server);
            case "haunted_house" -> haunted(mc, server);
            case "rowena_npc" -> rowenaNpc(mc, server);
            default -> structure(mc, server);
        };
        if (done) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.hideGui = true;
            mc.options.fov().set(FOV);
            scene = null;
            base = null;
            camFrom = null;
        }
        return true;
    }

    // --- staging -----------------------------------------------------------------------------------------------------------

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static void stage(MinecraftServer server, String which) {
        ServerPlayer p = player(server);
        ServerLevel level = server.overworld();
        // Spectator: the camera passes through walls and is never pushed out of a block (Rowena's scene switches to creative
        // while she should notice someone near).
        p.setGameMode(which.equals("rowena_npc") ? GameType.CREATIVE : GameType.SPECTATOR);
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.removeAllEffects();
        org.papiricoh.supernaturalcraft.eclipse.Eclipses.end(level);
        level.setDayTime(18000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.setWeatherParameters(12000, 0, false, false);
        staged.forEach(Entity::discard);
        staged.clear();
        int x = X + NAMES.indexOf(which) * 120;
        level.getChunk(x >> 4, Z >> 4);
        BlockPos g = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, Z));
        AABB around = new AABB(g).inflate(40);
        for (Entity old : level.getEntitiesOfClass(Entity.class, around, e -> e instanceof WitchEntity || e instanceof FamiliarEntity
                || e instanceof HexedVillagerEntity || e instanceof RowenaNpcEntity || e.getType() == EntityType.VILLAGER)) old.discard();
        Vec3 b = Vec3.atBottomCenterOf(g);
        switch (which) {
            case "witches", "rowena_npc" -> {
                floor(level, g, 16, 10);
                p.teleportTo(level, b.x, b.y + 4, b.z + 12, 180, 15);
                if (which.equals("witches")) {
                    hedge = mob(level, AllEntities.HEDGE_WITCH.get(), b.add(-6, 0, 0));
                    coven = mob(level, AllEntities.COVEN_WITCH.get(), b.add(-2.5, 0, 0));
                    FamiliarEntity cat = mob(level, AllEntities.FAMILIAR.get(), b.add(1, 0, 0.5));
                    cat.setVariant(FamiliarEntity.Variant.CAT);
                    cat.setHeld(TaglockItem.of(p));
                    FamiliarEntity raven = mob(level, AllEntities.FAMILIAR.get(), b.add(3.5, 0, 0.5));
                    raven.setVariant(FamiliarEntity.Variant.RAVEN);
                    raven.setHeld(TaglockItem.of(p));
                    hexed = mob(level, AllEntities.HEXED_VILLAGER.get(), b.add(7, 0, 0));
                } else {
                    rowena = mob(level, AllEntities.ROWENA_NPC.get(), b);
                }
            }
            case "haunted_house" -> {
                p.teleportTo(level, b.x, b.y + 1, b.z, 180, 0);
                // The preview world is superflat (bedrock a few blocks down): raise a pad so the cellar and the well fit.
                BlockPos pad = g.above(12);
                for (int dx = -34; dx <= 34; dx++) {
                    for (int dz = -34; dz <= 34; dz++) {
                        for (int y = g.getY(); y < pad.getY(); y++) {
                            level.setBlock(new BlockPos(g.getX() + dx, y, g.getZ() + dz), y == pad.getY() - 1
                                    ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState(), 2);
                        }
                    }
                }
                houseOrigin = Vec3.atBottomCenterOf(pad);
                org.papiricoh.supernaturalcraft.haunted.HauntedCommands.place(level, pad, HAUNTED_SEED);
            }
            default -> {
                // The structures build where the camera stands, facing north (Rowena's door then looks south, at us).
                p.teleportTo(level, b.x, b.y + 1, b.z, 180, 0);
                houseOrigin = Vec3.atBottomCenterOf(org.papiricoh.supernaturalcraft.coven.rowena.RowenaHouse.originOn(level, g.getX(), g.getZ()));
                String cmd = switch (which) {
                    case "coven" -> "supernatural coven covenstead place";
                    case "church" -> "supernatural coven church place";
                    case "haunted_house" -> "supernatural haunted place";
                    default -> "supernatural coven rowena place";
                };
                server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(), cmd);
                int back = which.equals("haunted_house") ? 44 : 30;
                p.teleportTo(level, b.x + back, b.y + back * 22 / 30, b.z + back, 135, 30);
            }
        }
        base = b;
    }

    private static void floor(ServerLevel level, BlockPos g, int rx, int rz) {
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dz = -rz; dz <= rz; dz++) {
                level.setBlockAndUpdate(g.offset(dx, -1, dz), (dx * 3 + dz) % 7 == 0 ? Blocks.PODZOL.defaultBlockState()
                        : Blocks.GRASS_BLOCK.defaultBlockState());
                for (int dy = 0; dy < 8; dy++) level.setBlockAndUpdate(g.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static <T extends Mob> T mob(ServerLevel level, EntityType<T> type, Vec3 at) {
        T e = type.create(level);
        e.moveTo(at.x, at.y, at.z, 0, 0);
        e.setYHeadRot(0);
        e.setYBodyRot(0);
        e.setNoAi(true);
        e.setSilent(false);
        e.setPersistenceRequired();
        level.addFreshEntity(e);
        staged.add(e);
        return e;
    }

    /** Puts the camera's eye at {@code eye} (the player's feet 1.62 below), looking at {@code at}. */
    private static void view(MinecraftServer server, Vec3 eye, Vec3 at) {
        Vec3 from = eye.subtract(0, 1.62, 0);
        camFrom = from;
        camAt = at;
        server.execute(() -> look(player(server), from, at));
    }

    private static void look(ServerPlayer p, Vec3 from, Vec3 at) {
        p.teleportTo(p.serverLevel(), from.x, from.y, from.z, p.getYRot(), p.getXRot());
        p.lookAt(EntityAnchorArgument.Anchor.EYES, at);
    }

    private static void daylight(MinecraftServer server, boolean day) {
        long time = day ? 6000 : 18000;
        server.execute(() -> server.overworld().setDayTime(time));
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) mc.level.setDayTime(time);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "sn_" + scene + "_" + name + ".png", mc.getMainRenderTarget(), m -> {
        });
    }

    private static void teleport(int entity, Vec3 from, Vec3 to, int smoke, float height, int duration) {
        ClientMagicFx.handle(new MagicFxPayload(MagicFxPayload.Kind.TELEPORT, School.WITCHCRAFT, entity, from, to, smoke, height, duration));
    }

    // --- witches ---------------------------------------------------------------------------------------------------------

    /** Where the Hedge Witch blinks to (west and back a little: both ends fit a 6-block shot). */
    private static final Vec3 BLINK_TO = new Vec3(-10, 0, -2);

    /** Ticks the timeline has held for the freed villager. */
    private static int hold;

    /** Whether the hexed villager (client side) has become a plain villager. */
    private static boolean freedVillager(Minecraft mc) {
        if (mc.level == null || hexed == null) return true;
        AABB box = new AABB(hexed.position(), hexed.position()).inflate(3);
        return mc.level.getEntitiesOfClass(HexedVillagerEntity.class, box).isEmpty()
                && !mc.level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class, box).isEmpty();
    }

    private static boolean witches(Minecraft mc, MinecraftServer server) {
        Vec3 b = base;
        if (t == 20) view(server, b.add(0.5, 3.6, 12), b.add(0.5, 0.9, 0));
        if (t == 36) shot(mc, "lineup");
        if (t == 40) view(server, b.add(-2.5, 1.9, 3.6), b.add(-2.5, 1.2, 0));
        if (t == 48) shot(mc, "coven_close");
        if (t == 52) view(server, b.add(2.3, 1.3, 3.2), b.add(2.3, 0.35, 0.5));
        if (t == 60) shot(mc, "familiars_close");
        if (t == 64) view(server, b.add(7, 1.8, 3.4), b.add(7, 1.2, 0));
        if (t == 72) shot(mc, "hexed_close");
        // Casting glow: off, on (a cast and a hex), fading.
        if (t == 80) view(server, b.add(-4.25, 1.9, 5.2), b.add(-4.25, 1.2, 0));
        if (t == 84) shot(mc, "glow_off");
        if (t == 86) server.execute(() -> {
            hedge.triggerAnim("action", "cast");
            coven.triggerAnim("action", "hex");
        });
        if (t == 92) shot(mc, "glow_on");
        if (t == 100) shot(mc, "glow_on_b");
        if (t == 130) shot(mc, "glow_faded");
        // The shield: struck four times (cracks gather), broken, raised again.
        if (t == 140) view(server, b.add(-2.5, 1.9, 4), b.add(-2.5, 1.1, 0));
        for (int i = 0; i < 4; i++) {
            if (t == 150 + i * 12) server.execute(() -> coven.hurt(coven.level().damageSources().playerAttack(player(server)), 3f));
        }
        if (t == 153) shot(mc, "shield_hit");
        if (t == 190) shot(mc, "shield_cracked");
        if (t == 200) server.execute(() -> coven.breakShield());
        if (t == 202) shot(mc, "shield_break_a");
        if (t == 207) shot(mc, "shield_break_b");
        if (t == 214) shot(mc, "shield_break_c");
        if (t == 230) server.execute(() -> coven.start(WitchEntity.Act.SHIELD, null, coven.position()));
        if (t == 241) shot(mc, "shield_raise");
        if (t == 246) shot(mc, "shield_raise_b");
        if (t == 262) shot(mc, "shield_up");
        // A smoke teleport through the real path (MagicFx.TELEPORT), to a known spot: the origin close, both ends, the arrival close.
        if (t == 280) view(server, b.add(-6, 1.9, 4.5), b.add(-6, 1.1, 0));
        if (t == 290) server.execute(() -> blink(hedge, b.add(BLINK_TO)));
        if (t == 291) shot(mc, "teleport_burst");
        if (t == 293) shot(mc, "teleport_figure");
        if (t == 296) shot(mc, "teleport_dissolve");
        if (t == 298) view(server, b.add(-10, 1.9, 2.6), b.add(-10, 1.0, -2));
        if (t == 304) shot(mc, "teleport_arrive");
        if (t == 308) shot(mc, "teleport_arrived");
        // And back, with the camera waiting where she will appear: the smoke gathering, the puff.
        if (t == 310) view(server, b.add(-6, 1.9, 4.5), b.add(-6, 1.1, 0));
        if (t == 316) server.execute(() -> blink(hedge, b.add(-6, 0, 0)));
        if (t == 318) shot(mc, "arrive_gather");
        if (t == 322) shot(mc, "arrive_gather_b");
        if (t == 325) shot(mc, "arrive_puff");
        if (t == 330) shot(mc, "arrive_after");
        // A yellow (boss) blink: its long gathering at the destination, close.
        if (t == 334) view(server, b.add(-3, 2.0, -0.5), b.add(-3, 1.0, -5));
        if (t == 340) teleport(-1, b.add(3, 0, -3), b.add(-3, 0, -5), MagicFxPayload.SMOKE_YELLOW, 2.2f, 60);
        if (t == 350) shot(mc, "yellow_gathering");
        if (t == 364) shot(mc, "yellow_gather_peak");
        if (t == 369) shot(mc, "yellow_puff");
        if (t == 376) shot(mc, "yellow_after");
        // The hexed villager freed, close: sigils lifting, the ring climbing, the golden burst.
        if (t == 386) view(server, b.add(7, 1.8, 3.6), b.add(7, 1.4, 0));
        if (t == 396) server.execute(() -> {
            boolean ok = hexed.free(player(server));
            if (!ok) org.papiricoh.supernaturalcraft.SupernaturalCraft.LOGGER.warn("[preview] the hexed villager was not freed: alive={} "
                    + "health={} removed={} freeing={}", hexed.isAlive(), hexed.getHealth(), hexed.isRemoved(), hexed.freeing());
        });
        if (t == 403) shot(mc, "freed_sigils");
        if (t == 410) shot(mc, "freed_sigils_b");
        if (t == 416) shot(mc, "freed_ring");
        if (t == 425) shot(mc, "freed_burst");
        if (t == 429) shot(mc, "freed_burst_b");
        // The villager it becomes: the server's 30-tick freeing can run late against the client's clock, so the timeline
        // holds here until the hexed one is gone and a villager stands in its place (or two seconds pass).
        if (t == 433 && !freedVillager(mc) && hold++ < 40) t--;
        if (t == 436) shot(mc, "freed_villager");
        // A familiar's eyes at the edges of the view (the overlay is GUI: shown for these two shots).
        if (t == 450) {
            mc.options.hideGui = false;
            MagicOverlay.watched(true);
        }
        if (t == 462) shot(mc, "watched_raven");
        if (t == 472) MagicOverlay.watched(false);
        if (t == 484) shot(mc, "watched_cat");
        if (t == 486) mc.options.hideGui = true;
        // By day (the eyes long gone).
        if (t == 540) {
            daylight(server, true);
            view(server, b.add(0.5, 3.6, 12), b.add(0.5, 0.9, 0));
        }
        if (t == 560) shot(mc, "day");
        if (t == 564) view(server, b.add(-2.5, 1.9, 3.6), b.add(-2.5, 1.2, 0));
        if (t == 574) shot(mc, "day_coven");
        if (t == 580) daylight(server, false);
        return t >= 585;
    }

    /** A smoke blink through the real path: she moves, the TELEPORT effect and its sounds go out as in a fight. */
    private static void blink(WitchEntity w, Vec3 to) {
        if (!(w.level() instanceof ServerLevel level)) return;
        Vec3 from = w.position();
        w.teleportTo(to.x, to.y, to.z);
        org.papiricoh.supernaturalcraft.magic.fx.MagicFx.teleport(level, w, School.WITCHCRAFT, from, to, MagicFxPayload.SMOKE_WITCH,
                WitchMagic.FX_TICKS * 2);
        level.playSound(null, from.x, from.y, from.z, org.papiricoh.supernaturalcraft.registry.AllSounds.coven("witch.teleport"),
                net.minecraft.sounds.SoundSource.HOSTILE, 1f, 1f);
    }

    // --- coven, church, Rowena's house -------------------------------------------------------------------------------------

    private static boolean structure(Minecraft mc, MinecraftServer server) {
        Vec3 b = scene.equals("rowena_house") && houseOrigin != null ? houseOrigin : base;
        boolean house = scene.equals("rowena_house");
        Vec3 closeFrom = house ? b.add(0, 4.5, 27) : b.add(4, 6, 17), closeAt = house ? b.add(0, 3, 15) : b.add(0, 1, 0);
        if (t == 40) view(server, b.add(30, 24, 30), b.add(0, 2, 0));
        if (t == 70) shot(mc, "wide_se");
        if (t == 74) view(server, b.add(-28, 20, -26), b.add(0, 2, 0));
        if (t == 94) shot(mc, "wide_nw");
        if (t == 98) view(server, closeFrom, closeAt);
        if (t == 118) shot(mc, "close");
        // Inside: from the hall, through the library door to her desk (cells per RowenaHouseLayout, door to the south).
        if (house && t == 122) view(server, b.add(1, 1.6, -3), b.add(-5, 0.9, -3));
        if (house && t == 140) shot(mc, "inside");
        if (house && t == 142) view(server, b.add(-3.5, 1.6, -2), b.add(-5, 0.8, -4));
        if (house && t == 148) shot(mc, "inside_desk");
        if (t == 150) {
            daylight(server, true);
            view(server, b.add(30, 24, 30), b.add(0, 2, 0));
        }
        if (t == 172) shot(mc, "day");
        if (t == 176) view(server, closeFrom, closeAt);
        if (t == 192) shot(mc, "day_close");
        if (t == 196) daylight(server, false);
        return t >= 200;
    }

    // --- the haunted house: outside, inside, every hiding spot, the family up, then laid to rest -------------------------------

    private static List<org.papiricoh.supernaturalcraft.haunted.HauntedHouseLayout.Spot> spots;

    private static Vec3 hp(org.papiricoh.supernaturalcraft.layout.LayoutPoint p) {
        BlockPos o = BlockPos.containing(houseOrigin);
        long seed = HAUNTED_SEED;
        return Vec3.atBottomCenterOf(org.papiricoh.supernaturalcraft.haunted.HauntedHouse.at(o, p, seed, hauntedDir()));
    }

    private static int hauntedDir() {
        return (int) Math.floorMod(HAUNTED_SEED ^ (HAUNTED_SEED >>> 29), 4L);
    }

    /** A point already placed by the layout (the hiding spots): only offset from the origin. */
    private static Vec3 placed(org.papiricoh.supernaturalcraft.layout.LayoutPoint p) {
        return Vec3.atBottomCenterOf(BlockPos.containing(houseOrigin).offset(p.x(), p.y(), p.z()));
    }

    /** Whether a standing camera fits at {@code p} (two cells of air). */
    private static boolean open(ServerLevel level, BlockPos p) {
        return level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir();
    }

    /** A view of {@code target} from the open side of it: the first direction with {@code dist} clear cells in front. */
    private static void inside(MinecraftServer server, Vec3 target, int dist) {
        ServerLevel level = server.overworld();
        BlockPos t0 = BlockPos.containing(target);
        for (int d = dist; d >= 1; d--) {
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                boolean clear = true;
                for (int k = 1; k <= d && clear; k++) clear = open(level, t0.relative(dir, k));
                if (!clear) continue;
                BlockPos eye = t0.relative(dir, d);
                view(server, Vec3.atBottomCenterOf(eye).add(0, 1.62, 0), target.add(0, 0.5, 0));
                return;
            }
        }
        view(server, target.add(0, 2.5, 0), target);
    }

    /** The first open cell under a roof walking from the gate towards the house's middle: just inside the front door. */
    private static Vec3 hallFrom(MinecraftServer server, Vec3 entry) {
        ServerLevel level = server.overworld();
        Vec3 step = houseOrigin.subtract(entry).multiply(1, 0, 1).normalize();
        for (int k = 1; k < 30; k++) {
            BlockPos p = BlockPos.containing(entry.add(step.scale(k)));
            if (!open(level, p)) continue;
            for (int up = 2; up <= 6; up++) if (!level.getBlockState(p.above(up)).isAir()) return Vec3.atBottomCenterOf(p);
        }
        return entry;
    }

    private static boolean haunted(Minecraft mc, MinecraftServer server) {
        Vec3 b = houseOrigin;
        if (spots == null) spots = org.papiricoh.supernaturalcraft.haunted.HauntedHouseLayout.spots(HAUNTED_SEED, hauntedDir());
        if (t == 40) view(server, b.add(44, 32, 44), b.add(0, 4, 0));
        if (t == 70) shot(mc, "wide_se");
        if (t == 74) view(server, b.add(-40, 28, -38), b.add(0, 4, 0));
        if (t == 94) shot(mc, "wide_nw");
        Vec3 entry = hp(org.papiricoh.supernaturalcraft.haunted.HauntedHouseLayout.ENTRY);
        if (t == 98) view(server, entry.add(entry.subtract(b).normalize().scale(9)).add(0, 3.5, 0), entry.add(0, 2, 0));
        if (t == 118) shot(mc, "front");
        if (t == 122) {
            Vec3 hall = hallFrom(server, entry);
            view(server, hall.add(0, 1.62, 0), b.add(0, 1.6, 0));
        }
        if (t == 140) shot(mc, "hall");
        Vec3 study = hp(org.papiricoh.supernaturalcraft.haunted.HauntedHouseLayout.STUDY_CHEST);
        if (t == 144) inside(server, study, 4);
        if (t == 174) shot(mc, "study");
        // Each hiding spot, seen from where a hunter would dig.
        int base = 180;
        for (int i = 0; i < spots.size(); i++) {
            var sp = spots.get(i);
            int at = base + i * 30;
            if (t == at) inside(server, placed(sp.access()), 3);
            if (t == at + 26) shot(mc, "bones_" + sp.where().name().toLowerCase(java.util.Locale.ROOT));
        }
        int up = base + spots.size() * 30;
        // The family raised, seen from the hall.
        if (t == up) server.execute(() -> {
            ServerLevel level = server.overworld();
            for (BlockPos pos : org.papiricoh.supernaturalcraft.haunted.HauntedHouse.bones(BlockPos.containing(b), HAUNTED_SEED, hauntedDir())) {
                if (level.getBlockEntity(pos) instanceof org.papiricoh.supernaturalcraft.grave.GraveBonesBlockEntity be) be.raiseGhost(level);
            }
        });
        if (t == up + 6) {
            ServerLevel level = server.overworld();
            var ghosts = level.getEntitiesOfClass(org.papiricoh.supernaturalcraft.entity.ghost.GhostEntity.class, new AABB(BlockPos.containing(b)).inflate(40));
            if (!ghosts.isEmpty()) inside(server, ghosts.getFirst().position(), 4);
        }
        if (t == up + 30) shot(mc, "ghosts");
        // Laid to rest: the study's chest appears.
        if (t == up + 34) server.execute(() -> {
            ServerLevel level = server.overworld();
            for (BlockPos pos : org.papiricoh.supernaturalcraft.haunted.HauntedHouse.bones(BlockPos.containing(b), HAUNTED_SEED, hauntedDir())) {
                org.papiricoh.supernaturalcraft.grave.GraveRites.burn(level, pos, null);
            }
        });
        if (t == up + 40) inside(server, study, 4);
        if (t == up + 62) shot(mc, "study_open");
        if (t == up + 64) {
            daylight(server, true);
            view(server, b.add(44, 32, 44), b.add(0, 4, 0));
        }
        if (t == up + 90) shot(mc, "day");
        if (t == up + 94) view(server, entry.add(entry.subtract(b).normalize().scale(9)).add(0, 3.5, 0), entry.add(0, 2, 0));
        if (t == up + 112) shot(mc, "day_front");
        if (t == up + 116) daylight(server, false);
        if (t >= up + 120) {
            spots = null;
            return true;
        }
        return false;
    }

    // --- Rowena at home --------------------------------------------------------------------------------------------------

    private static boolean rowenaNpc(Minecraft mc, MinecraftServer server) {
        Vec3 b = base;
        // Idle: someone near (creative), so she does not read.
        if (t == 20) view(server, b.add(0, 2.2, 6), b.add(0, 1.1, 0));
        if (t == 34) shot(mc, "idle_wide");
        if (t == 38) view(server, b.add(0, 1.7, 3), b.add(0, 1.5, 0));
        if (t == 50) shot(mc, "idle_close");
        // Reading: nobody near (a spectator does not count), close.
        if (t == 54) server.execute(() -> player(server).setGameMode(GameType.SPECTATOR));
        if (t == 56) view(server, b.add(0.6, 1.8, 3), b.add(0, 1.2, 0));
        if (t == 96) shot(mc, "read_close");
        if (t == 108) shot(mc, "read_close_b");
        // Talking (her face at 3 blocks), then handing a page over (her rings glow).
        if (t == 110) server.execute(() -> player(server).setGameMode(GameType.CREATIVE));
        if (t == 112) view(server, b.add(0.3, 1.65, 3), b.add(0, 1.55, 0));
        if (t == 118) server.execute(() -> rowena.talkTo(player(server)));
        if (t == 134) shot(mc, "talk_close");
        if (t == 140) shot(mc, "talk_face");
        if (t == 144) view(server, b.add(0.8, 1.65, 3), b.add(0, 1.15, 0));
        if (t == 150) server.execute(() -> rowena.give());
        if (t == 157) shot(mc, "give_glow");
        if (t == 163) shot(mc, "give_glow_close");
        if (t == 196) shot(mc, "glow_off");
        // A yellow blink: she steps a little away in smoke, shot at 5 blocks.
        if (t == 200) view(server, b.add(1.5, 2.0, 5), b.add(1.5, 1, -0.8));
        if (t == 206) {
            Vec3 from = rowena.position(), to = b.add(3, 0, -1.5);
            server.execute(() -> rowena.teleportTo(to.x, to.y, to.z));
            teleport(rowena.getId(), from, to, MagicFxPayload.SMOKE_YELLOW, rowena.getBbHeight(), 40);
        }
        if (t == 208) shot(mc, "teleport_a");
        if (t == 211) shot(mc, "teleport_b");
        if (t == 216) shot(mc, "teleport_c");
        if (t == 230) shot(mc, "teleport_d");
        if (t == 240) {
            daylight(server, true);
            view(server, b.add(1.5, 2.2, 7), b.add(2, 1.2, -1));
        }
        if (t == 258) shot(mc, "day");
        if (t == 262) view(server, b.add(3, 1.7, 1.5), b.add(3, 1.5, -1.5));
        if (t == 272) shot(mc, "day_close");
        if (t == 276) daylight(server, false);
        return t >= 280;
    }
}
