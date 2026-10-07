package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.metatron.MetatronAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.metatron.MetatronEntity;
import org.papiricoh.supernaturalcraft.entity.boss.metatron.MetatronSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.metatron.MetatronTerrain;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;

import java.util.List;

/**
 * Metatron in the real renderer. {@code SN_PREVIEW=metatron}: his descent and library, the Hand writing,
 * the dais and lectern, the Book crushing, the Tablet, the Fall, a rewrite, his death; each construct
 * attack driven by hand against a dummy. {@code SN_PREVIEW=metatron_fight}: a real fight over the shoulder
 * (the player cannot be hurt), phases 2-4 forced along the way. Both well away from the origin.
 */
final class MetatronPreview {

    private static int t = -1;
    private static BlockPos altar;
    private static MetatronEntity boss;
    private static ArmorStand dummy;
    private static BossAttack<LuciferEntity> driven;
    private static int drivenFrom;

    private MetatronPreview() {
    }

    static boolean tick(Minecraft mc) {
        String scene = System.getenv("SN_PREVIEW");
        if ("metatron_hand".equals(scene)) return handScene(mc);
        if (!"metatron".equals(scene) && !"metatron_fight".equals(scene)) return false;
        boolean fight = "metatron_fight".equals(scene);
        var server = mc.getSingleplayerServer();
        t++;
        int now = t;
        if (fight && now == 1) mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
            if (fight) fight(mc, p, now);
            else scene(mc, p, now);
        });
        if (!fight) {
            for (int s : new int[]{60, 100, 135, 190, 268, 304, 360, 420, 455, 470, 520, 625, 672, 760, 800}) {
                if (now == s) grab(mc, "sn_metatron_%04d.png", now);
            }
        } else if (now >= 100 && now % 40 == 0 && now < 1390) {
            grab(mc, "sn_metatron_fight_%04d.png", now);
        }
        if (now >= (fight ? 1400 : 840)) mc.stop();
        return true;
    }

    /**
     * {@code SN_PREVIEW=metatron_hand}: only the Hand of God. It comes down out of its door, hangs at rest
     * (from below, beside, close), then writes, slams and sweeps against a dummy.
     */
    private static Vec3 camFrom, camAt;

    /** Remembers a view and holds it: a teleport right after a change of dimension can be lost. */
    private static void hold(ServerPlayer p, Vec3 from, Vec3 at) {
        camFrom = from;
        camAt = at;
        view(p, from, at);
    }

    private static boolean handScene(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        t++;
        int now = t + 30;
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
            ServerLevel level = server.overworld();
            if (now == 31) {
                p.setGameMode(GameType.SPECTATOR);
                // The preview world may have left the player in Hell.
                p.teleportTo(level, 400.5, 120, 400.5, 0, 0);
                setUp(mc, level);
            }
            if (now < 50) return;
            if (camFrom != null && now % 5 == 0 && p.position().distanceToSqr(camFrom) > 0.25) view(p, camFrom, camAt);
            if (now == 50) {
                summon(level, p);
                if (boss != null) boss.forceLook(2);
            }
            if (boss == null) return;
            boss.scheduler().delay(40);
            if (now == 52) {
                // Stand him off to one side so the Hand hangs over open ground.
                Vec3 c = Vec3.atBottomCenterOf(altar);
                boss.teleportTo(c.x - 6, c.y, c.z);
            }
            tickDriven(now);
            var hand = boss.hand();
            if (hand == null) return;
            Vec3 h = hand.position();
            if (now == 62) hold(p, h.add(10, 3, -14), h.add(0, 5, 0));
            if (now == 100) hold(p, h.add(3, -0.5, -4), h.add(0, 9, 0));
            if (now == 140) hold(p, h.add(-14, 6, -8), h.add(0, 5, 0));
            if (now == 180) hold(p, h.add(3, 3.5, -6), h.add(0, 3.2, 0));
            if (now == 215) {
                dummy = new ArmorStand(EntityType.ARMOR_STAND, level);
                Vec3 d = Vec3.atBottomCenterOf(altar).add(4, 0, 4);
                dummy.moveTo(d.x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(d.x), (int) Math.floor(d.z)), d.z);
                level.addFreshEntity(dummy);
                hold(p, d.add(-8, 6, -11), d.add(0, 2, 0));
                drive(new MetatronAttacks.QuillScript(), now);
            }
            if (now == 320) {
                hold(p, dummy.position().add(-11, 5, -6), dummy.position().add(0, 2, 0));
                drive(new MetatronAttacks.PalmSlam(), now);
            }
            if (now == 390) {
                hold(p, dummy.position().add(-10, 7, -12), dummy.position());
                drive(new MetatronAttacks.Sweep(), now);
            }
            if (now == 465) finish(level);
        });
        for (int s : new int[]{58, 85, 120, 160, 200, 250, 272, 310, 344, 352, 420, 428}) {
            if (now == s) grab(mc, "sn_metatron_hand_%04d.png", now);
        }
        if (now >= 470) mc.stop();
        return true;
    }

    private static void grab(Minecraft mc, String pattern, int now) {
        Screenshot.grab(mc.gameDirectory, String.format(pattern, now), mc.getMainRenderTarget(), m -> {
        });
    }

    private static void view(ServerPlayer p, Vec3 from, Vec3 at) {
        p.teleportTo(p.serverLevel(), from.x, from.y, from.z, p.getYRot(), p.getXRot());
        p.lookAt(EntityAnchorArgument.Anchor.EYES, at);
    }

    private static void setUp(Minecraft mc, ServerLevel level) {
        mc.options.hideGui = true;
        level.setDayTime(18000);
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        level.getChunk(400 >> 4, 412 >> 4);
        altar = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(400, 0, 412));
    }

    private static void summon(ServerLevel level, ServerPlayer p) {
        MetatronSummoning.summon(level, altar, p);
        var found = level.getEntitiesOfClass(MetatronEntity.class, new AABB(altar).inflate(16));
        boss = found.isEmpty() ? null : found.getFirst();
    }

    private static void finish(ServerLevel level) {
        if (boss != null && !boss.isRemoved()) boss.discard();
        if (dummy != null) dummy.discard();
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
    }

    /** Runs one construct attack against the dummy: windup at {@code at}, then its active ticks. */
    private static void drive(BossAttack<LuciferEntity> attack, int now) {
        driven = attack;
        drivenFrom = now;
        attack.onWindup(boss, dummy);
    }

    private static void tickDriven(int now) {
        if (driven == null) return;
        int t0 = drivenFrom + driven.windup;
        if (now == t0) driven.onActive(boss, dummy);
        if (now >= t0 && now < t0 + driven.active) driven.tickActive(boss, dummy, now - t0);
        if (now == t0 + driven.active + driven.recover) {
            driven.onEnd(boss);
            driven = null;
        }
    }

    private static void scene(Minecraft mc, ServerPlayer p, int now) {
        ServerLevel level = p.serverLevel();
        if (now == 1) {
            p.setGameMode(GameType.SPECTATOR);
            setUp(mc, level);
            view(p, Vec3.atBottomCenterOf(altar).add(0, 3, -10), Vec3.atBottomCenterOf(altar).add(0, 1, 6));
        }
        if (now == 10) summon(level, p);
        if (boss == null) return;
        // Keep him from fighting on his own: the scene drives every attack.
        boss.scheduler().delay(40);
        tickDriven(now);
        Vec3 c = Vec3.atBottomCenterOf(altar);
        if (now == 125) view(p, c.add(-16, 16, -16), c);
        if (now == 140) boss.beginTransition(2);
        if (now == 228) {
            dummy = new ArmorStand(EntityType.ARMOR_STAND, level);
            Vec3 d = c.add(4, 0, -4);
            dummy.moveTo(d.x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(d.x), (int) Math.floor(d.z)), d.z);
            level.addFreshEntity(dummy);
            view(p, c.add(-8, 9, -12), c.add(2, 0, -2));
        }
        if (now == 230) drive(new MetatronAttacks.QuillScript(), now);
        if (now == 320) boss.beginTransition(3);
        if (now == 410) view(p, c.add(9, 6, -11), c.add(0, 3, 0));
        if (now == 425) {
            view(p, dummy.position().add(-9, 7, -9), dummy.position());
            drive(new MetatronAttacks.TomeCrush(), now);
        }
        if (now == 480) boss.beginTransition(4);
        if (now == 580) {
            view(p, c.add(-12, 10, -12), c);
            drive(new MetatronAttacks.TheFall(), now);
        }
        if (now == 660) {
            MetatronTerrain.rewrite(level, boss.arena(), level.getRandom());
            view(p, c.add(0, 18, -14), c);
        }
        if (now == 700) {
            view(p, boss.position().add(-5, 1, -6), boss.position().add(0, 1.4, 0));
            boss.setHealth(1f);
            boss.invulnerableTime = 0;
            boss.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, null), 10f);
        }
        if (now == 835) finish(level);
    }

    private static void fight(Minecraft mc, ServerPlayer p, int now) {
        ServerLevel level = p.serverLevel();
        if (now == 1) {
            p.setGameMode(GameType.SURVIVAL);
            p.getAbilities().invulnerable = true;
            p.onUpdateAbilities();
            setUp(mc, level);
            Vec3 stand = Vec3.atBottomCenterOf(altar).add(0, 0, -12);
            p.teleportTo(level, stand.x, stand.y, stand.z, 0, 10);
        }
        if (now == 10) summon(level, p);
        if (boss == null || boss.isRemoved()) return;
        if (now % 5 == 0) {
            Vec3 to = boss.position().subtract(p.position()).multiply(1, 0, 1);
            Vec3 side = new Vec3(-to.z, 0, to.x).normalize().scale(Math.max(2.5, to.length() * 0.45));
            p.lookAt(EntityAnchorArgument.Anchor.EYES, boss.position().add(side).add(0, 1.2, 0));
        }
        p.setHealth(p.getMaxHealth());
        p.getFoodData().setFoodLevel(20);
        if (now == 400 || now == 700 || now == 1000) {
            float at = now == 400 ? 0.76f : now == 700 ? 0.51f : 0.26f;
            boss.setHealth(boss.getMaxHealth() * at);
            boss.invulnerableTime = 0;
            boss.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, p), 40f);
        }
        if (now == 1390) finish(level);
    }
}
