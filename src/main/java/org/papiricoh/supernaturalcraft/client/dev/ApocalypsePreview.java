package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.apocalypse.SwanSong;
import org.papiricoh.supernaturalcraft.apocalypse.blood.BloodData;
import org.papiricoh.supernaturalcraft.apocalypse.blood.Bloods;
import org.papiricoh.supernaturalcraft.apocalypse.ruby.RubyAssets;
import org.papiricoh.supernaturalcraft.apocalypse.ruby.RubyFight;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealSavedData;
import org.papiricoh.supernaturalcraft.apocalypse.warehouse.WarehouseAnchors;
import org.papiricoh.supernaturalcraft.apocalypse.warehouse.WarehouseSite;
import org.papiricoh.supernaturalcraft.apocalypse.warehouse.WarehouseWorld;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.entity.boss.alastair.AlastairAssets;
import org.papiricoh.supernaturalcraft.entity.boss.alastair.AlastairEntity;
import org.papiricoh.supernaturalcraft.entity.boss.alastair.AlastairSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.hunter.DevilsTrapBlock;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Preview scenes for the Apocalypse's core (v0.40): {@code alastair_model} (his clips, bound and free), {@code alastair_fight}
 * (a real fight with his HUD), {@code warehouse} (the warehouse from its yard, from above and at the trap), {@code ruby_model}
 * (the NPC and the fight's clips), {@code ruby_fight} (her fight with its bar), {@code blood_hud} (the vial at each tier and the
 * withdrawal vignette), {@code blood_wheel} (the power wheel of a hunter with the blood in them), {@code panic_room} (the room
 * and its door) and {@code swan_song} (Lucifer thrown back into the Cage). Captures in
 * {@code runs/client/screenshots/sn_<scene>_<beat>.png}. Same shape as {@code SealPreview}: it runs only when every name in
 * {@code SN_PREVIEW} is one of its {@link #NAMES}; the world's seals are saved before each scene and put back after it.
 */
final class ApocalypsePreview {

    static final List<String> NAMES = List.of("alastair_model", "alastair_fight", "warehouse", "ruby_model", "ruby_fight", "blood_hud",
            "blood_wheel", "panic_room", "swan_song");
    private static final int FOV = 70;
    private static final int X = 12400, Z = 9600;

    private record Beat(String name, BiConsumer<ServerPlayer, Vec3> server, boolean hud, int length, Runnable client) {
    }

    private static List<String> queue;
    private static String scene;
    private static int t, beatIndex, beatStart;
    private static volatile Vec3 base;
    private static List<Beat> beats;
    private static final List<Entity> staged = new ArrayList<>();
    private static volatile Vec3 camFrom, camAt;
    private static CompoundTag seals;
    private static int savedFov = -1;
    private static double savedFovEffect;

    private ApocalypsePreview() {
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
                case "alastair_model" -> alastairModel();
                case "alastair_fight" -> alastairFight();
                case "warehouse" -> warehouse();
                case "ruby_model" -> rubyModel();
                case "ruby_fight" -> rubyFight();
                case "blood_hud" -> bloodHud();
                case "blood_wheel" -> bloodWheel();
                case "panic_room" -> panicRoom();
                default -> swanSong();
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
        if (local == 20 && b.client != null) b.client.run();
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

    /** The scenes a hunter walks into themselves (their HUD, their wheel): survival, not a spectator. */
    private static boolean firstPerson(String which) {
        return which.startsWith("blood") || which.equals("panic_room");
    }

    private static void stage(MinecraftServer server, String which) {
        ServerPlayer p = player(server);
        p.setGameMode(firstPerson(which) ? GameType.CREATIVE : GameType.SPECTATOR);
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.removeAllEffects();
        ServerLevel level = server.overworld();
        seals = SealSavedData.get(server).snapshot();
        org.papiricoh.supernaturalcraft.eclipse.Eclipses.end(level);
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        level.setDayTime(which.equals("alastair_fight") || which.equals("swan_song") || which.equals("ruby_fight") ? 18000 : 6000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.setWeatherParameters(12000, 0, false, false);
        int x = X + NAMES.indexOf(which) * 200;
        level.getChunk(x >> 4, Z >> 4);
        BlockPos g = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, Z));
        for (Entity old : level.getEntitiesOfClass(Entity.class, new AABB(g).inflate(60), e -> !(e instanceof Player))) old.discard();
        if (!which.equals("warehouse")) floor(level, g, 28, 28);
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
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        if (seals != null) SealSavedData.get(server).restore(seals);
        ServerPlayer p = player(server);
        Bloods.update(p, d -> BloodData.EMPTY);
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
        return new Beat(name, server, false, length, null);
    }

    private static Beat hud(String name, int length, BiConsumer<ServerPlayer, Vec3> server) {
        return new Beat(name, server, true, length, null);
    }

    private static void play(Mob m, String clip) {
        if (m instanceof software.bernie.geckolib.animatable.GeoEntity g) g.triggerAnim("action", clip);
    }

    // --- SN_PREVIEW=alastair_model -----------------------------------------------------------------------------------------------

    private static List<Beat> alastairModel() {
        List<Beat> out = new ArrayList<>();
        List<Mob> holder = new ArrayList<>();
        out.add(beat("bound", 50, (p, c) -> {
            AlastairEntity a = mob(p.serverLevel(), AllEntities.ALASTAIR.get(), c, 0);
            a.chain(true);
            holder.add(a);
            view(p, c.add(0, 2.2, 4.5), c.add(0, 1.0, 0));
        }));
        out.add(beat("free", 40, (p, c) -> {
            ((AlastairEntity) holder.getFirst()).chain(false);
            view(p, c.add(0, 2.2, 4.5), c.add(0, 1.2, 0));
        }));
        for (String clip : AlastairAssets.TRIGGERED) {
            out.add(beat(clip, 40, (p, c) -> {
                play(holder.getFirst(), clip);
                view(p, c.add(2.2, 2.0, 4.0), c.add(0, 1.2, 0));
            }));
        }
        out.add(beat("back", 30, (p, c) -> view(p, c.add(-1.5, 2.2, -4.2), c.add(0, 1.2, 0))));
        return out;
    }

    // --- SN_PREVIEW=alastair_fight -----------------------------------------------------------------------------------------------

    private static List<Beat> alastairFight() {
        List<Beat> out = new ArrayList<>();
        AlastairEntity[] boss = new AlastairEntity[1];
        out.add(hud("bound", 120, (p, c) -> {
            boss[0] = AlastairSummoning.summon(p.serverLevel(), BlockPos.containing(c), null);
            if (boss[0] != null) boss[0].chain(true);
            view(p, c.add(0, 3, 12), c.add(0, 1.4, 4.5));
        }));
        out.add(hud("free", 120, (p, c) -> {
            if (boss[0] != null) boss[0].beginTransition(2);
            view(p, c.add(6, 3.5, 12), c.add(0, 1.2, 4.5));
        }));
        out.add(hud("tormentor", 80, (p, c) -> view(p, c.add(-7, 3.5, 10), c.add(0, 1.2, 4.5))));
        out.add(hud("rack", 160, (p, c) -> {
            if (boss[0] != null) boss[0].beginTransition(3);
            view(p, c.add(0, 5, 14), c.add(0, 1.2, 4.5));
        }));
        return out;
    }

    // --- SN_PREVIEW=warehouse ----------------------------------------------------------------------------------------------------

    private static List<Beat> warehouse() {
        List<Beat> out = new ArrayList<>();
        WarehouseSite[] site = new WarehouseSite[1];
        out.add(beat("yard", 80, (p, c) -> {
            site[0] = WarehouseWorld.placeAt(p.serverLevel(), BlockPos.containing(c), 0);
            Vec3 entry = site[0].floor(WarehouseAnchors.ENTRY);
            view(p, entry.add(0, 1.7, -6), site[0].floor(WarehouseAnchors.TRAP).add(0, 3, 0));
        }));
        out.add(beat("above", 50, (p, c) -> view(p, site[0].floor(WarehouseAnchors.TRAP).add(0, 40, -30), site[0].floor(WarehouseAnchors.TRAP))));
        out.add(beat("doors", 50, (p, c) -> view(p, site[0].floor(WarehouseAnchors.ENTRY).add(0, 1.7, 0), site[0].floor(WarehouseAnchors.TRAP).add(0, 1, 0))));
        out.add(beat("trap", 60, (p, c) -> {
            Vec3 tr = site[0].floor(WarehouseAnchors.TRAP);
            AlastairEntity a = mob(p.serverLevel(), AllEntities.ALASTAIR.get(), tr, 180);
            a.chain(true);
            for (Vec3 s : site[0].angels()) mob(p.serverLevel(), AllEntities.ANGEL.get(), s, 0);
            view(p, tr.add(5, 3.5, 8), tr.add(0, 1, 0));
        }));
        out.add(beat("hall", 50, (p, c) -> {
            Vec3 tr = site[0].floor(WarehouseAnchors.TRAP);
            view(p, tr.add(-12, 4, 9), tr.add(8, 2, -6));
        }));
        return out;
    }

    // --- SN_PREVIEW=ruby_model ---------------------------------------------------------------------------------------------------

    private static List<Beat> rubyModel() {
        List<Beat> out = new ArrayList<>();
        List<Mob> holder = new ArrayList<>();
        out.add(beat("npc", 40, (p, c) -> {
            holder.add(mob(p.serverLevel(), AllEntities.RUBY_NPC.get(), c.add(-1.2, 0, 0), 0));
            holder.add(mob(p.serverLevel(), AllEntities.RUBY.get(), c.add(1.2, 0, 0), 0));
            view(p, c.add(0, 2.0, 4.5), c.add(0, 1.1, 0));
        }));
        for (String clip : RubyAssets.TRIGGERED) {
            out.add(beat(clip, 34, (p, c) -> {
                play(holder.get(1), clip);
                play(holder.getFirst(), clip);
                view(p, c.add(1.6, 1.9, 4.0), c.add(0, 1.1, 0));
            }));
        }
        return out;
    }

    // --- SN_PREVIEW=ruby_fight ---------------------------------------------------------------------------------------------------

    private static List<Beat> rubyFight() {
        List<Beat> out = new ArrayList<>();
        out.add(hud("reveal", 80, (p, c) -> {
            var r = RubyFight.spawn(p.serverLevel(), c, 1);
            if (r != null) staged.add(r);
            view(p, c.add(0, 2.4, 6), c.add(0, 1.2, 0));
        }));
        out.add(hud("fight", 80, (p, c) -> view(p, c.add(4, 2.4, 5), c.add(0, 1.2, 0))));
        return out;
    }

    // --- SN_PREVIEW=blood_hud ----------------------------------------------------------------------------------------------------

    private static List<Beat> bloodHud() {
        List<Beat> out = new ArrayList<>();
        out.add(hud("one_dose", 40, (p, c) -> {
            Bloods.update(p, d -> BloodData.EMPTY);
            Bloods.drink(p);
            view(p, c.add(0, 1.7, 0), c.add(0, 1.5, -10));
        }));
        out.add(hud("tier_3", 40, (p, c) -> {
            for (int i = 0; i < 15; i++) Bloods.drink(p);
        }));
        out.add(hud("withdrawal", 60, (p, c) -> Bloods.update(p, d -> d.withBlood(0).withDependency(0.9f))));
        return out;
    }

    // --- SN_PREVIEW=blood_wheel --------------------------------------------------------------------------------------------------

    private static List<Beat> bloodWheel() {
        List<Beat> out = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        out.add(new Beat("hunter", (p, c) -> {
            Bloods.update(p, d -> BloodData.EMPTY);
            for (int i = 0; i < 16; i++) Bloods.drink(p);
            view(p, c.add(0, 1.7, 0), c.add(0, 1.5, -10));
        }, true, 60, () -> mc.setScreen(new org.papiricoh.supernaturalcraft.client.allegiance.PowerWheelScreen(
                org.papiricoh.supernaturalcraft.client.allegiance.AllegianceClientEvents.WHEEL))));
        return out;
    }

    // --- SN_PREVIEW=panic_room ---------------------------------------------------------------------------------------------------

    private static List<Beat> panicRoom() {
        List<Beat> out = new ArrayList<>();
        out.add(beat("outside", 60, (p, c) -> {
            ServerLevel level = p.serverLevel();
            BlockPos o = BlockPos.containing(c);
            // An iron room, inside 5×3×5, a devil's trap on the ceiling, the door in its south wall, a window of bars.
            for (int x = -3; x <= 3; x++)
                for (int y = -1; y <= 3; y++)
                    for (int z = -3; z <= 3; z++) {
                        boolean inside = Math.abs(x) <= 2 && y >= 0 && y <= 2 && Math.abs(z) <= 2;
                        level.setBlockAndUpdate(o.offset(x, y, z), inside ? Blocks.AIR.defaultBlockState() : Blocks.IRON_BLOCK.defaultBlockState());
                    }
            level.setBlockAndUpdate(o.offset(0, 2, 0), AllBlocks.DEVILS_TRAP.get().defaultBlockState().setValue(DevilsTrapBlock.FACE, DevilsTrapBlock.Face.CEILING));
            level.setBlockAndUpdate(o.offset(-1, 0, -2), Blocks.RED_BED.defaultBlockState());
            level.setBlockAndUpdate(o.offset(2, 1, 0).east(), Blocks.IRON_BARS.defaultBlockState());
            var door = AllBlocks.PANIC_ROOM_DOOR.get().defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
            level.setBlockAndUpdate(o.offset(0, 0, 3), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
            level.setBlockAndUpdate(o.offset(0, 1, 3), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
            view(p, c.add(2, 1.7, 7), c.add(0, 1, 3));
        }));
        out.add(hud("inside", 80, (p, c) -> {
            Bloods.update(p, d -> new BloodData(0f, 0.8f, 3, 0));
            view(p, c.add(0, 1.62, 1.5), c.add(0, 2.6, -1));
        }));
        return out;
    }

    // --- SN_PREVIEW=swan_song ----------------------------------------------------------------------------------------------------

    private static List<Beat> swanSong() {
        List<Beat> out = new ArrayList<>();
        LuciferEntity[] l = new LuciferEntity[1];
        out.add(beat("lucifer", 80, (p, c) -> {
            ServerLevel level = p.serverLevel();
            ArenaController arena = org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning.openArena(level, BlockPos.containing(c), 18,
                    org.papiricoh.supernaturalcraft.arena.ArenaTheme.CAGE);
            l[0] = mob(level, AllEntities.LUCIFER.get(), c, 0);
            if (arena != null) l[0].bindArena(arena);
            view(p, c.add(0, 3, 9), c.add(0, 1.4, 0));
        }));
        out.add(beat("the_rings", 30, (p, c) -> {
            ArenaController arena = l[0].arena();
            if (arena != null) SwanSong.play(l[0], p.serverLevel(), arena);
        }));
        out.add(beat("the_fall", 60, (p, c) -> view(p, c.add(4, 6, 6), c)));
        out.add(beat("the_cage", 60, (p, c) -> view(p, c.add(0.5, 10, 0.5), c.add(0, -4, 0))));
        return out;
    }
}
