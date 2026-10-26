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
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.client.leviathan.ClientDickFx;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicFxManager;
import org.papiricoh.supernaturalcraft.entity.boss.dick.DickAssets;
import org.papiricoh.supernaturalcraft.entity.boss.dick.DickBalance;
import org.papiricoh.supernaturalcraft.entity.boss.dick.DickRomanEntity;
import org.papiricoh.supernaturalcraft.entity.boss.dick.DickSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.leviathan.LeviathanAssets;
import org.papiricoh.supernaturalcraft.entity.leviathan.LeviathanEntity;
import org.papiricoh.supernaturalcraft.entity.leviathan.LeviathanHeadEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.leviathan.Borax;
import org.papiricoh.supernaturalcraft.network.DickFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The Leviathans and Dick Roman (v0.25) in the real renderer, well away from the origin, at a plain 70°. Screenshots:
 * {@code sn_leviathan_*.png}, {@code sn_dick_*.png}.
 * <ul>
 *   <li>{@code SN_PREVIEW=leviathan}: a disguised Leviathan beside a revealed one (day and night), close-ups of the maw, every
 *   clip at its moment, the headless body with its head, the reveal, borax and decapitation effects, the items laid out.</li>
 *   <li>{@code SN_PREVIEW=dick_model}: his four looks side by side (the face, the maw, the copy's skin, the goo mantle), close-ups,
 *   and every clip at its blow.</li>
 *   <li>{@code SN_PREVIEW=dick_fight}: his boardroom raised by a real summoning, the intro with its title card and bar, each
 *   attack's telegraph and effect staged, the copy among doubles, the Flood and borax drying it, the death card.</li>
 * </ul>
 */
final class LeviathanPreview {

    static final List<String> NAMES = List.of("leviathan", "dick_model", "dick_fight");
    private static final int FOV = 70;
    private static final int X = 7600, Z = 7600;

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
    private static DickRomanEntity boss;

    private LeviathanPreview() {
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
            MagicFxManager.clear();
            ClientDickFx.clear();
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
        if (beats == null) {
            beats = switch (scene) {
                case "leviathan" -> leviathan();
                case "dick_model" -> dickModel();
                default -> dickFight();
            };
        }
        if (beatIndex >= beats.size()) {
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
                e -> e instanceof LuciferEntity || e instanceof LeviathanEntity || e instanceof LeviathanHeadEntity || e instanceof ArmorStand
                        || e instanceof TelegraphMarker || e instanceof ItemEntity)) {
            old.discard();
        }
        floor(level, g, 22, 22);
        Vec3 b = Vec3.atBottomCenterOf(g);
        p.teleportTo(level, b.x, b.y + 3, b.z + 8, 180, 15);
        base = b;
    }

    private static void unstage(MinecraftServer server) {
        ServerLevel level = server.overworld();
        clearStaged();
        if (boss != null) boss.discard();
        boss = null;
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        player(server).setGameMode(GameType.SPECTATOR);
    }

    private static void floor(ServerLevel level, BlockPos g, int rx, int rz) {
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dz = -rz; dz <= rz; dz++) {
                int h = Math.floorMod(dx * 7 + dz * 13, 11);
                level.setBlockAndUpdate(g.offset(dx, -1, dz), h == 0 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState());
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

    private static LeviathanEntity leviathan(ServerLevel level, Vec3 at, float yaw, String disguise) {
        LeviathanEntity l = mob(level, AllEntities.LEVIATHAN.get(), at, yaw);
        l.setDisguise(disguise);
        return l;
    }

    private static DickRomanEntity dick(ServerLevel level, Vec3 at, float yaw, int look) {
        DickRomanEntity d = mob(level, AllEntities.DICK_ROMAN.get(), at, yaw);
        d.forceLook(look);
        return d;
    }

    private static void clearStaged() {
        staged.forEach(Entity::discard);
        staged.clear();
        Minecraft.getInstance().execute(() -> {
            MagicFxManager.clear();
            ClientDickFx.clear();
        });
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

    private static void fx(ServerPlayer p, DickFxPayload payload) {
        PacketDistributor.sendToPlayer(p, payload);
    }

    private static Beat beat(String name, int length, BiConsumer<ServerPlayer, Vec3> server) {
        return new Beat(name, server, false, length);
    }

    // --- SN_PREVIEW=leviathan --------------------------------------------------------------------------------------------------

    private static List<Beat> leviathan() {
        List<Beat> out = new ArrayList<>();
        Vec3 eye = new Vec3(0, 1.3, 0);
        out.add(beat("lineup_day", 40, (p, c) -> {
            clearStaged();
            ServerLevel level = p.serverLevel();
            leviathan(level, c.add(-1.4, 0, 0), 0, "villager:minecraft:farmer");
            leviathan(level, c.add(1.4, 0, 0), 0, "").triggerAnim("action", "reveal");
            view(p, c.add(0, 1.7, 6), c.add(0, 1.0, 0));
        }));
        out.add(beat("lineup_night", 30, (p, c) -> p.serverLevel().setDayTime(18000)));
        out.add(beat("front", 30, (p, c) -> {
            clearStaged();
            p.serverLevel().setDayTime(6000);
            leviathan(p.serverLevel(), c, 0, "");
            view(p, c.add(0.3, 1.6, 2.6), c.add(eye));
        }));
        out.add(beat("back", 30, (p, c) -> view(p, c.add(-0.3, 1.6, -2.6), c.add(eye))));
        out.add(beat("side", 30, (p, c) -> view(p, c.add(2.6, 1.6, 0.2), c.add(eye))));
        for (String clip : LeviathanAssets.TRIGGERED) {
            int at = LeviathanAssets.HIT_TICKS.getOrDefault(clip, LeviathanAssets.CLIP_TICKS.getOrDefault(clip, 20) - 2);
            out.add(beat("clip_" + clip, at + 6, (p, c) -> {
                clearStaged();
                leviathan(p.serverLevel(), c, 0, "").triggerAnim("action", clip);
                view(p, c.add(1.8, 1.7, 2.2), c.add(eye));
            }));
        }
        out.add(beat("maw_close", 30, (p, c) -> {
            clearStaged();
            leviathan(p.serverLevel(), c, 0, "").triggerAnim("action", "reveal");
            view(p, c.add(0.1, 1.75, 1.2), c.add(0, 1.65, 0));
        }));
        out.add(beat("headless", 40, (p, c) -> {
            clearStaged();
            ServerLevel level = p.serverLevel();
            LeviathanEntity l = leviathan(level, c, 0, "");
            l.setNoAi(false);
            l.decapitate(level.damageSources().generic());
            view(p, c.add(2.2, 1.8, 3.2), c.add(0, 0.5, 0));
        }));
        out.add(beat("head_close", 30, (p, c) -> {
            for (LeviathanHeadEntity h : p.serverLevel().getEntitiesOfClass(LeviathanHeadEntity.class, new AABB(BlockPos.containing(c)).inflate(8))) {
                staged.add(h);
                view(p, h.position().add(0.9, 1.0, 0.9), h.position().add(0, 0.2, 0));
            }
        }));
        out.add(beat("borax", 14, (p, c) -> {
            clearStaged();
            ServerLevel level = p.serverLevel();
            leviathan(level, c, 0, "");
            Borax.splash(level, c, 2.5, p);
            view(p, c.add(0, 2.2, 4.5), c.add(0, 0.8, 0));
        }));
        out.add(beat("items", 30, (p, c) -> {
            clearStaged();
            ServerLevel level = p.serverLevel();
            List<ItemStack> items = List.of(new ItemStack(AllItems.BORAX_BLEND.get()), new ItemStack(AllItems.BORAX.get()),
                    new ItemStack(AllItems.BORAX_SPRAY.get()), new ItemStack(AllItems.BORAX_SPRAYER.get()), new ItemStack(AllItems.TURDUCKEN_SLAMMER.get()),
                    new ItemStack(AllItems.BIGGERSONS_BURGER.get()), new ItemStack(AllItems.RIGHTEOUS_BONE.get()),
                    new ItemStack(AllItems.ANOINTED_RIGHTEOUS_BONE.get()), new ItemStack(AllItems.FALLEN_BLOOD.get()),
                    new ItemStack(AllItems.KINGS_BLOOD.get()), new ItemStack(AllItems.ALPHA_BLOOD.get()), new ItemStack(AllItems.LEVIATHAN_TABLET.get()));
            for (int i = 0; i < items.size(); i++) {
                ItemEntity e = new ItemEntity(level, c.x - 2.75 + (i % 6) * 1.1, c.y + 0.1, c.z + (i / 6) * 1.1, items.get(i), 0, 0, 0);
                e.setNoGravity(true);
                e.setNeverPickUp();
                e.setUnlimitedLifetime();
                level.addFreshEntity(e);
                staged.add(e);
            }
            view(p, c.add(0, 3.2, 3.6), c.add(0, 0.2, 0.6));
        }));
        return out;
    }

    // --- SN_PREVIEW=dick_model -------------------------------------------------------------------------------------------------

    private static List<Beat> dickModel() {
        List<Beat> out = new ArrayList<>();
        Vec3 eye = new Vec3(0, 1.3, 0);
        out.add(beat("lineup", 40, (p, c) -> {
            clearStaged();
            ServerLevel level = p.serverLevel();
            for (int i = 1; i <= 4; i++) dick(level, c.add((i - 2.5) * 2.2, 0, 0), 0, i);
            view(p, c.add(0, 1.8, 8), c.add(0, 1.0, 0));
        }));
        for (int look = 1; look <= 4; look += 3) {
            int l = look;
            out.add(beat("p" + l + "_front", 30, (p, c) -> {
                clearStaged();
                dick(p.serverLevel(), c, 0, l);
                view(p, c.add(0.3, 1.6, 2.6), c.add(eye));
            }));
            out.add(beat("p" + l + "_back", 30, (p, c) -> view(p, c.add(-0.3, 1.6, -2.6), c.add(eye))));
            out.add(beat("p" + l + "_face", 30, (p, c) -> view(p, c.add(0.1, 1.8, 1.1), c.add(0, 1.72, 0))));
        }
        out.add(beat("p2_maw", 30, (p, c) -> {
            clearStaged();
            dick(p.serverLevel(), c, 0, 2).triggerAnim("action", "maw_open");
            view(p, c.add(0.6, 1.85, 1.4), c.add(0, 1.7, 0));
        }));
        for (String clip : DickAssets.TRIGGERED) {
            int at = DickAssets.HIT_TICKS.getOrDefault(clip, DickAssets.CLIP_TICKS.getOrDefault(clip, 20) - 2);
            out.add(beat("clip_" + clip, at + 6, (p, c) -> {
                clearStaged();
                int look = clip.equals("downsizing") || clip.equals("sign_here") || clip.equals("staff_meeting") || clip.equals("emerge") ? 1
                        : clip.equals("tentacle_slam") || clip.equals("melt") ? 4 : 2;
                dick(p.serverLevel(), c, 0, look).triggerAnim("action", clip);
                view(p, c.add(2.2, 1.7, 2.6), c.add(eye));
            }));
        }
        return out;
    }

    // --- SN_PREVIEW=dick_fight -------------------------------------------------------------------------------------------------

    private static List<Beat> dickFight() {
        List<Beat> out = new ArrayList<>();
        out.add(new Beat("intro", (p, c) -> {
            clearStaged();
            boss = DickSummoning.summon(p.serverLevel(), BlockPos.containing(c), p);
            if (boss != null) boss.layGroundNow();
            p.setGameMode(GameType.CREATIVE);
            view(p, c.add(0, 2.2, 7), c.add(0, 1.2, -4));
        }, true, 60));
        out.add(beat("boardroom_above", 30, (p, c) -> view(p, c.add(0, 22, 10), c.add(0, 0, -1))));
        out.add(beat("boardroom_table", 30, (p, c) -> view(p, c.add(6, 3, -2), c.add(0, 1, -7))));
        out.add(new Beat("hud", (p, c) -> {
            if (boss != null) boss.setNoAi(true);
            view(p, c.add(0, 2.0, 6), c.add(0, 1.4, -4));
        }, true, 120));
        out.add(beat("downsizing", 20, (p, c) -> {
            Vec3 at = bossAt(c);
            TelegraphMarker.circle(p.serverLevel(), at, DickBalance.DOWNSIZE_RADIUS, TelegraphMarker.RED, 40);
            fx(p, new DickFxPayload(boss == null ? -1 : boss.getId(), DickFxPayload.DOWNSIZE, Math.round(DickBalance.DOWNSIZE_RADIUS * 10), 0, at, 12));
            view(p, at.add(0, 6, 9), at);
        }));
        out.add(beat("sign_here", 24, (p, c) -> {
            Vec3 at = bossAt(c);
            TelegraphMarker.line(p.serverLevel(), at, 0, DickBalance.SIGN_WIDTH, DickBalance.SIGN_REACH, TelegraphMarker.VIOLET, 40);
            fx(p, new DickFxPayload(p.getId(), DickFxPayload.CONTRACT, p.getId(), 0, at.add(0, 0, 6), 40));
            view(p, at.add(4, 5, 10), at.add(0, 0, 6));
        }));
        out.add(beat("bite", 12, (p, c) -> {
            Vec3 at = bossAt(c);
            if (boss != null) boss.forceLook(2);
            TelegraphMarker.cone(p.serverLevel(), at, 0, DickBalance.BITE_REACH, TelegraphMarker.RED, 30);
            fx(p, new DickFxPayload(boss == null ? -1 : boss.getId(), DickFxPayload.BITE, 0, Math.round(DickBalance.BITE_REACH * 10), at, 8));
            view(p, at.add(3, 3, 6), at.add(0, 1, 2));
        }));
        out.add(beat("goo_spew", 20, (p, c) -> {
            Vec3 at = bossAt(c).add(3, 0, 3);
            TelegraphMarker.circle(p.serverLevel(), at, DickBalance.GOO_RADIUS, TelegraphMarker.RED, 30);
            fx(p, new DickFxPayload(boss == null ? -1 : boss.getId(), DickFxPayload.GOO_SPLASH, Math.round(DickBalance.GOO_RADIUS * 10), 0, at, 14));
            view(p, at.add(0, 5, 7), at);
        }));
        out.add(beat("copy", 30, (p, c) -> {
            if (boss == null) return;
            boss.forceLook(3);
            boss.beginCopy(p.serverLevel(), p, c);
            for (LeviathanEntity d : boss.doubles()) d.setNoAi(true);
            view(p, c.add(0, 4, 11), c);
        }));
        out.add(beat("exposed", 16, (p, c) -> {
            if (boss == null) return;
            Borax.splash(p.serverLevel(), boss.position(), 2.5, p);
            view(p, boss.position().add(0, 3, 6), boss.position().add(0, 1, 0));
        }));
        out.add(beat("flood", 40, (p, c) -> {
            if (boss == null) return;
            boss.forceLook(4);
            boss.floodNow();
            view(p, c.add(0, 9, 13), c);
        }));
        out.add(beat("flood_dried", 20, (p, c) -> {
            Borax.splash(p.serverLevel(), c.add(4, 0, 4), 2.5, p);
            Borax.splash(p.serverLevel(), c.add(-4, 0, 2), 2.5, p);
            view(p, c.add(0, 8, 11), c);
        }));
        out.add(beat("tentacle", 14, (p, c) -> {
            Vec3 at = c.add(-6, 0, 6);
            TelegraphMarker.line(p.serverLevel(), at, -135, DickBalance.TENTACLE_WIDTH, DickBalance.TENTACLE_REACH, TelegraphMarker.RED, 30);
            fx(p, new DickFxPayload(boss == null ? -1 : boss.getId(), DickFxPayload.TENTACLE, -135, Math.round(DickBalance.TENTACLE_REACH * 10), at, 14));
            view(p, c.add(4, 6, 12), c.add(-2, 0, 4));
        }));
        out.add(beat("surge", 40, (p, c) -> {
            fx(p, new DickFxPayload(boss == null ? -1 : boss.getId(), DickFxPayload.SURGE, 16, 0, c, 40));
            view(p, c.add(0, 10, 14), c);
        }));
        for (int phase = 1; phase <= 4; phase++) {
            int ph = phase;
            out.add(new Beat("title_p" + ph, (p, c) -> fx(p, new DickFxPayload(boss == null ? -1 : boss.getId(), DickFxPayload.TITLE, ph, 0, c, 70)),
                    true, 40));
        }
        out.add(new Beat("title_death", (p, c) -> fx(p, new DickFxPayload(boss == null ? -1 : boss.getId(), DickFxPayload.TITLE, 4, 1, c, 90)),
                true, 50));
        return out;
    }

    private static Vec3 bossAt(Vec3 c) {
        return boss != null ? boss.position() : c;
    }

}
