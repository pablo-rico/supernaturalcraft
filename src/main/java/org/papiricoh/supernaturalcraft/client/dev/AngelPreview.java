package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.angel.SmiteDeath;
import org.papiricoh.supernaturalcraft.angel.TheFall;
import org.papiricoh.supernaturalcraft.client.angel.ClientAngels;
import org.papiricoh.supernaturalcraft.entity.angel.AngelAssets;
import org.papiricoh.supernaturalcraft.entity.angel.AngelEntity;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.ArrayList;
import java.util.List;

/**
 * The angels (v0.22) in the real renderer, well away from the origin:
 * <ul>
 *   <li>{@code angels}: the four angel vessels and the three fallen in a row (the second angel with its wings shown), from the
 *   front and three-quarters.</li>
 *   <li>{@code angel_smite}: an angel and a Host angel smitten side by side at night (captures 5, 18 and 29 ticks in, then the
 *   flash and the burnt wings from above), then an angel by day (18 ticks in, and its scorch).</li>
 *   <li>{@code the_fall}: the night of the Fall over the Overworld (looking up, then round), and one star landing in view.</li>
 * </ul>
 */
final class AngelPreview {

    static final List<String> NAMES = List.of("angels", "angel_smite", "the_fall");
    private static final BlockPos AT = new BlockPos(520, 0, 540);

    private static int t = -1;
    private static Vec3 base;
    private static final List<Entity> SPAWNED = new ArrayList<>();
    private static AngelEntity winged;

    private AngelPreview() {
    }

    /** The scenes asked for, played one after another (a list in one run: the preview stops once, after the last). */
    private static List<String> queue;

    static boolean tick(Minecraft mc) {
        String env = DevPreview.env();
        if (env == null) return false;
        if (queue == null) {
            List<String> want = new ArrayList<>();
            for (String s : env.split(",")) if (!s.isBlank()) want.add(s.trim());
            if (want.isEmpty() || !NAMES.containsAll(want)) return false;
            queue = want;
        }
        String scene = queue.getFirst();
        var server = mc.getSingleplayerServer();
        t++;
        int now = t;
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
            switch (scene) {
                case "angels" -> lineUp(mc, p, now);
                case "angel_smite" -> smite(mc, p, now);
                default -> fall(mc, p, now);
            }
        });
        int[] shots = switch (scene) {
            case "angels" -> new int[]{60, 100};
            case "angel_smite" -> new int[]{65, 78, 89, 93, 110, 150, 248, 300};
            default -> new int[]{170, 230, 290, 330, 345, 360, 380};
        };
        for (int i = 0; i < shots.length; i++) if (now == shots[i]) grab(mc, "sn_" + scene + "_" + i + ".png");
        int end = switch (scene) {
            case "angels" -> 110;
            case "angel_smite" -> 310;
            default -> 400;
        };
        if (now >= end) {
            server.execute(AngelPreview::cleanUp);
            ClientAngels.clear();
            t = -1;
            queue.removeFirst();
            if (queue.isEmpty()) {
                queue = null;
                DevPreview.stop(mc);
            }
        }
        return true;
    }

    private static void grab(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), m -> {
        });
    }

    private static void view(ServerPlayer p, Vec3 from, Vec3 at) {
        p.teleportTo(p.serverLevel(), from.x, from.y, from.z, p.getYRot(), p.getXRot());
        p.lookAt(EntityAnchorArgument.Anchor.EYES, at);
    }

    private static void setUp(Minecraft mc, ServerPlayer p, long time) {
        mc.options.hideGui = true;
        mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
        p.setGameMode(GameType.SPECTATOR);
        ServerLevel level = p.serverLevel();
        level.setDayTime(time);
        level.setWeatherParameters(6000, 0, false, false);
        level.getChunk(AT.getX() >> 4, AT.getZ() >> 4);
        base = Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, AT));
    }

    private static <E extends Mob> E spawn(ServerLevel level, net.minecraft.world.entity.EntityType<E> type, Vec3 at, float yaw) {
        E e = type.create(level);
        if (e == null) return null;
        e.moveTo(at.x, at.y, at.z, yaw, 0);
        e.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.COMMAND, null);
        e.setNoAi(true);
        e.setYHeadRot(yaw);
        e.setYBodyRot(yaw);
        e.setPersistenceRequired();
        level.addFreshEntity(e);
        SPAWNED.add(e);
        return e;
    }

    private static void cleanUp() {
        for (Entity e : SPAWNED) if (!e.isRemoved()) e.discard();
        SPAWNED.clear();
        winged = null;
    }

    // --- the line-up --------------------------------------------------------------------------------------------------------

    private static void lineUp(Minecraft mc, ServerPlayer p, int now) {
        ServerLevel level = p.serverLevel();
        if (now == 1) {
            setUp(mc, p, 6000);
            int n = AngelAssets.ANGEL_VESSELS + AngelAssets.FALLEN_VESSELS;
            for (int i = 0; i < n; i++) {
                boolean fallen = i >= AngelAssets.ANGEL_VESSELS;
                Vec3 at = base.add((i - (n - 1) / 2.0) * 1.6, 0, 0);
                AngelEntity a = fallen ? spawn(level, AllEntities.FALLEN_ANGEL.get(), at, 180) : spawn(level, AllEntities.ANGEL.get(), at, 180);
                if (a == null) continue;
                a.setVessel(fallen ? i - AngelAssets.ANGEL_VESSELS : i);
                if (i == 1) winged = a;
            }
            view(p, base.add(0, 2.2, -8.5), base.add(0, 1.1, 0));
        }
        if (winged != null && now % 5 == 0) winged.flashWings();
        if (now == 80) view(p, base.add(-6.5, 2.4, -6), base.add(-0.8, 1.1, 0));
    }

    // --- the smite ----------------------------------------------------------------------------------------------------------

    private static void smite(Minecraft mc, ServerPlayer p, int now) {
        ServerLevel level = p.serverLevel();
        if (now == 1) {
            setUp(mc, p, 18000);
            spawn(level, AllEntities.ANGEL.get(), base.add(-1.6, 0, 0), 180);
            spawn(level, AllEntities.HOST_ANGEL.get(), base.add(1.6, 0, 0), 180);
            view(p, base.add(0.4, 1.9, -6.5), base.add(0, 1.3, 0));
        }
        if (now == 60) {
            for (Entity e : SPAWNED) if (e instanceof LivingEntity l) SmiteDeath.smite(l, p, new ItemStack(AllItems.ANGEL_BLADE.get()));
        }
        // The flash and the wings burnt into the ground, from above and behind.
        if (now == 100) view(p, base.add(0, 6.5, 5.5), base.add(0, 0, 0.6));
        // By day: one more angel.
        if (now == 200) {
            cleanUp();
            level.setDayTime(6000);
            spawn(level, AllEntities.ANGEL.get(), base.add(0, 0, 6), 180);
            view(p, base.add(1.5, 1.9, -0.5), base.add(0, 1.3, 6));
        }
        if (now == 230) {
            for (Entity e : SPAWNED) if (e instanceof LivingEntity l) SmiteDeath.smite(l, p, new ItemStack(AllItems.ANGEL_BLADE.get()));
        }
        if (now == 290) view(p, base.add(0, 6, 11), base.add(0, 0, 6.6));
    }

    // --- the Fall -----------------------------------------------------------------------------------------------------------

    private static void fall(Minecraft mc, ServerPlayer p, int now) {
        ServerLevel level = p.serverLevel();
        if (now == 1) {
            setUp(mc, p, 18000);
            view(p, base.add(0, 2, 0), base.add(0, 30, 40));
        }
        if (now == 20) TheFall.beginNight(level);
        if (now == 200) view(p, base.add(0, 2, 0), base.add(40, 18, 0));
        if (now == 260) view(p, base.add(0, 2, 0), base.add(-30, 12, -40));
        // One star into view, landing about 110 blocks off.
        if (now == 300) {
            view(p, base.add(0, 2, 0), base.add(0, 14, 60));
            TheFall.star(level, base.x + 25, base.z + 110);
        }
        if (now == 390) TheFall.endNight(level);
    }
}
