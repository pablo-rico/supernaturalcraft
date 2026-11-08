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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;
import org.papiricoh.supernaturalcraft.hands.HandsRegistry;
import org.papiricoh.supernaturalcraft.hands.mob.BansheeEntity;
import org.papiricoh.supernaturalcraft.hands.mob.ChitterNests;
import org.papiricoh.supernaturalcraft.hands.mob.HandsMob;
import org.papiricoh.supernaturalcraft.hands.reliquary.ReliquaryAnchors;
import org.papiricoh.supernaturalcraft.hands.reliquary.ReliquaryBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * v0.42 preview scenes for the Hands of God slice: {@code reliquary_uboat}, {@code reliquary_bunker}, {@code reliquary_crypt},
 * {@code reliquary_temple}, {@code reliquary_monastery}, {@code thule}, {@code banshee}, {@code chitters}. Same shape as
 * {@code NephilimPreview}: it runs only when every name in {@code SN_PREVIEW} is one of its {@link #NAMES}; captures in
 * {@code runs/client/screenshots/sn_<scene>_<beat>.png}.
 * <ul>
 *   <li>{@code reliquary_*}: the reliquary built far out in the Overworld (the wreck in a box of sea): from afar, from its way in,
 *   from above, and at its altar (the Hand's place) from where a hunter kneels.</li>
 *   <li>{@code thule}: three soldiers and an officer in a line, then each of their clips.</li>
 *   <li>{@code banshee}: two banshees shown, then their clips (the scream, the claw, the fade and the return).</li>
 *   <li>{@code chitters}: a pack on a cave floor with its nest woven round it, then their clips.</li>
 * </ul>
 */
final class HandsPreview {

    static final List<String> NAMES = List.of("reliquary_uboat", "reliquary_bunker", "reliquary_crypt", "reliquary_temple",
            "reliquary_monastery", "thule", "banshee", "chitters");
    private static final int FOV = 70;
    /** Where the scenes are staged: far east of the world's spawn, one after another. */
    private static final int OFFSET = 24_000, STEP = 300;

    private record Beat(String name, BiConsumer<ServerPlayer, Vec3> server, int length) {
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

    private HandsPreview() {
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
                case "reliquary_uboat" -> reliquary(S11Assets.Hand.DELPHINES_HAND);
                case "reliquary_bunker" -> reliquary(S11Assets.Hand.NAIL_OF_THE_CROSS);
                case "reliquary_crypt" -> reliquary(S11Assets.Hand.APOSTLES_CHALICE);
                case "reliquary_temple" -> reliquary(S11Assets.Hand.ROD_OF_AARON);
                case "reliquary_monastery" -> reliquary(S11Assets.Hand.TRUMPET_OF_JERICHO);
                case "thule" -> thule();
                case "banshee" -> banshee();
                default -> chitters();
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
        if (local == 0 && b.server != null) server.execute(() -> b.server.accept(player(server), c));
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
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.setDayTime(which.equals("banshee") ? 18000 : 6000);
        level.setWeatherParameters(12000, 0, false, false);
        int x = OFFSET + NAMES.indexOf(which) * STEP, z = 0;
        level.getChunk(x >> 4, z >> 4);
        BlockPos g = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        for (Entity old : level.getEntitiesOfClass(Entity.class, new AABB(g).inflate(90), e -> !(e instanceof Player))) old.discard();
        Vec3 b = Vec3.atBottomCenterOf(g);
        p.teleportTo(level, b.x, b.y + 3, b.z + 8, 180, 15);
        base = b;
    }

    private static void unstage(MinecraftServer server) {
        staged.forEach(Entity::discard);
        staged.clear();
        ServerLevel level = server.overworld();
        for (Entity e : level.getEntitiesOfClass(Entity.class, new AABB(BlockPos.containing(base)).inflate(100), e -> e instanceof Mob)) e.discard();
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
        return new Beat(name, server, length);
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

    /** A box of sea round the wreck's site (the preview world has none): sand under, water over. */
    private static void sea(ServerLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = -30; x <= 30; x++) {
            for (int z = -30; z <= 30; z++) {
                for (int y = -4; y <= 18; y++) {
                    m.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    level.setBlock(m, y <= -3 ? Blocks.SAND.defaultBlockState() : Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    // --- SN_PREVIEW=reliquary_* ------------------------------------------------------------------------------------------------

    private static List<Beat> reliquary(S11Assets.Hand kind) {
        List<Beat> out = new ArrayList<>();
        BlockPos[] origin = new BlockPos[1];
        BlockPos[] altar = new BlockPos[1];
        ReliquaryAnchors.Bounds bounds = ReliquaryAnchors.bounds(kind);
        int reach = bounds.reach();
        out.add(beat("afar", 100, (p, c) -> {
            ServerLevel level = p.serverLevel();
            BlockPos o = BlockPos.containing(c);
            if (kind == S11Assets.Hand.DELPHINES_HAND) {
                o = o.below(12);
                sea(level, o);
            }
            origin[0] = o;
            altar[0] = ReliquaryBuilder.placeAt(level, o, kind, 0);
            for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(o).inflate(reach + 10))) {
                m.setNoAi(true);
                staged.add(m);
            }
            Vec3 at = Vec3.atBottomCenterOf(o);
            view(p, at.add(reach * 0.9, reach * 0.55 + 4, -reach * 1.2), at.add(0, 2, 0));
        }));
        out.add(beat("entry", 60, (p, c) -> {
            BlockPos e = ReliquaryBuilder.point(origin[0], kind, 0, ReliquaryAnchors.ENTRY);
            Vec3 from = Vec3.atBottomCenterOf(e).add(0, 2.2, -2);
            view(p, from, Vec3.atBottomCenterOf(origin[0]).add(0, 1.5, 4));
        }));
        out.add(beat("above", 60, (p, c) -> {
            Vec3 at = Vec3.atBottomCenterOf(origin[0]);
            view(p, at.add(reach * 0.6, reach * 1.4 + 10, -reach * 0.6), at.add(0, bounds.bottomY() / 2.0, bounds.maxZ() / 3.0));
        }));
        out.add(beat("altar", 60, (p, c) -> {
            BlockPos k = ReliquaryBuilder.point(origin[0], kind, 0, ReliquaryAnchors.KNEEL);
            Vec3 a = Vec3.atBottomCenterOf(altar[0]);
            Vec3 kv = Vec3.atBottomCenterOf(k);
            Vec3 back = kv.subtract(a).normalize().scale(1.5);
            view(p, kv.add(back).add(0, 1.8, 0), a.add(0, 0.8, 0));
        }));
        return out;
    }

    // --- SN_PREVIEW=thule / banshee / chitters ---------------------------------------------------------------------------------

    /** A line of creatures on a stone pad, viewed from the front, then each one-shot clip of theirs. */
    private static List<Beat> lineUp(String what, List<EntityType<? extends HandsMob>> types, S11Assets.Rig[] rigs, boolean dark) {
        List<Beat> out = new ArrayList<>();
        List<HandsMob> line = new ArrayList<>();
        out.add(beat("line_up", 60, (p, c) -> {
            ServerLevel level = p.serverLevel();
            BlockPos o = BlockPos.containing(c);
            for (int x = -6; x <= 6; x++) for (int z = -4; z <= 6; z++) {
                level.setBlock(o.offset(x, -1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                if (dark) level.setBlock(o.offset(x, 5, z), Blocks.DEEPSLATE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            if (dark) {
                level.setBlock(o.offset(0, 4, 3), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true),
                        Block.UPDATE_CLIENTS);
                ChitterNests.weave(level, o.offset(0, 0, 2), level.random);
            }
            for (int i = 0; i < types.size(); i++) {
                HandsMob m = mob(level, types.get(i), c.add(-3 + i * 2, 0, 0), 0);
                if (m instanceof BansheeEntity b) {
                    b.reveal();
                    m.setPos(m.getX(), m.getY() + 0.6, m.getZ());
                }
                line.add(m);
            }
            view(p, c.add(0, 1.9, 7), c.add(0, 1, 0));
        }));
        for (S11Assets.Rig rig : rigs) {
            for (String clip : rig.triggered()) {
                if (clip.equals("death")) continue;
                out.add(beat(rig.rig() + "_" + clip, 40, (p, c) -> line.forEach(m -> {
                    if (m instanceof BansheeEntity b) b.reveal();
                    if (m.getType().toShortString().equals(rig.rig())) m.triggerAnim("action", clip);
                })));
            }
        }
        out.add(beat("death", 50, (p, c) -> line.forEach(m -> m.triggerAnim("action", "death"))));
        return out;
    }

    private static List<Beat> thule() {
        return lineUp("thule", List.of(HandsRegistry.THULE_SOLDIER.get(), HandsRegistry.THULE_OFFICER.get(), HandsRegistry.THULE_SOLDIER.get(),
                HandsRegistry.THULE_SOLDIER.get()), new S11Assets.Rig[]{S11Assets.THULE_SOLDIER, S11Assets.THULE_OFFICER}, false);
    }

    private static List<Beat> banshee() {
        return lineUp("banshee", List.of(HandsRegistry.BANSHEE.get(), HandsRegistry.BANSHEE.get()), new S11Assets.Rig[]{S11Assets.BANSHEE}, false);
    }

    private static List<Beat> chitters() {
        return lineUp("chitters", List.of(HandsRegistry.CHITTER.get(), HandsRegistry.CHITTER.get(), HandsRegistry.CHITTER.get()),
                new S11Assets.Rig[]{S11Assets.CHITTER}, true);
    }
}
