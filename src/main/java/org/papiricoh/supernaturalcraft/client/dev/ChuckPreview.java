package org.papiricoh.supernaturalcraft.client.dev;

import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import net.minecraft.client.CameraType;
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
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckBalance;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;

import java.util.List;

/**
 * The Author's fight in the real renderer. {@code SN_PREVIEW=chuck}: he stands up in his arena (the intro), then each
 * chapter in turn (real transitions), the key attacks driven by hand against a dummy (the Snap, the glass, the keys, a
 * sweeping line, the hands, an ink echo, a narration), the fake credits, the finale with Dean, Sam and Castiel, his
 * death. {@code SN_PREVIEW=chuck_fight}: a real fight over the shoulder (the player cannot be hurt), each chapter
 * forced along the way. Both staged far from the origin, at 2000, 2000.
 */
final class ChuckPreview {

    private static final int STAGE = 2000;
    private static int t = -1;
    private static BlockPos spot;
    private static ChuckEntity boss;
    private static ArmorStand dummy;
    private static BossAttack<LuciferEntity> driven;
    private static int drivenFrom;

    private ChuckPreview() {
    }

    /** @return true while this preview is running (it owns the tick) */
    static boolean tick(Minecraft mc) {
        String scene = System.getenv("SN_PREVIEW");
        if (!"chuck".equals(scene) && !"chuck_fight".equals(scene)) return false;
        boolean fight = "chuck_fight".equals(scene);
        var server = mc.getSingleplayerServer();
        if (server == null) return true;
        t++;
        int now = t;
        if (fight && now == 1) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
            if (fight) fight(mc, p, now);
            else scene(mc, p, now);
        });
        if (!fight) {
            for (int s : SHOTS) {
                if (now == s) grab(mc, "sn_chuck_%04d.png", now);
            }
        } else if (now >= 120 && now % 40 == 0 && now < 1590) {
            grab(mc, "sn_chuck_fight_%04d.png", now);
        }
        if (now >= (fight ? 1600 : 1820)) mc.stop();
        return true;
    }

    private static final int[] SHOTS = {40, 80, 120, 150, 185, 205, 245, 300, 340, 380, 420, 445, 500, 540, 640, 690, 740,
            790, 830, 885, 905, 960, 1010, 1060, 1085, 1150, 1230, 1275, 1300, 1350, 1420, 1500, 1570, 1600, 1680, 1760};

    private static void grab(Minecraft mc, String pattern, int now) {
        Screenshot.grab(mc.gameDirectory, String.format(pattern, now), mc.getMainRenderTarget(), m -> {
        });
    }

    private static void view(ServerPlayer p, Vec3 from, Vec3 at) {
        if (p.isCreative() && !p.getAbilities().flying) {
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
        }
        p.teleportTo(p.serverLevel(), from.x, from.y, from.z, p.getYRot(), p.getXRot());
        p.lookAt(EntityAnchorArgument.Anchor.EYES, at);
    }

    private static void setUp(ServerLevel level) {
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        level.getChunk(STAGE >> 4, STAGE >> 4);
        spot = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(STAGE, 0, STAGE));
    }

    private static void summon(ServerLevel level, ServerPlayer p) {
        ChuckSummoning.summon(level, spot, p, false);
        var found = level.getEntitiesOfClass(ChuckEntity.class, new AABB(spot).inflate(16));
        boss = found.isEmpty() ? null : found.getFirst();
    }

    private static void finish(ServerLevel level) {
        if (boss != null && !boss.isRemoved()) boss.discard();
        if (dummy != null) dummy.discard();
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
    }

    private static void placeDummy(ServerLevel level, Vec3 c) {
        if (dummy != null) dummy.discard();
        dummy = new ArmorStand(EntityType.ARMOR_STAND, level);
        Vec3 d = c.add(7, 0, 5);
        dummy.moveTo(d.x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(d.x), (int) Math.floor(d.z)), d.z);
        level.addFreshEntity(dummy);
    }

    /** Runs one attack against the dummy: its wind-up now, then its active ticks, then its end. */
    private static void drive(BossAttack<LuciferEntity> attack, int now) {
        if (dummy == null || boss == null) return;
        if (driven != null) driven.onEnd(boss);
        driven = attack;
        drivenFrom = now;
        boss.triggerAnim("action", attack.animation);
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
            // Creative and flying, not spectator: a spectator isn't a living participant, and the arena would give the
            // fight up for abandoned after failureSeconds. He never targets a creative player.
            p.setGameMode(GameType.CREATIVE);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            setUp(level);
            mc.options.hideGui = false;
            Vec3 c = Vec3.atBottomCenterOf(spot);
            view(p, c.add(0, 3, -12), c.add(0, 1.5, 0));
        }
        if (now == 10) summon(level, p);
        if (boss == null || boss.isRemoved()) return;
        Vec3 c = Vec3.atBottomCenterOf(spot);
        // The scene drives every attack; between them he waits.
        boss.scheduler().delay(40);
        tickDriven(now);
        if (now == 160) {
            boss.setWritingIgnored(true);
            placeDummy(level, c);
            view(p, c.add(-10, 7, -10), c.add(4, 0, 3));
            drive(new ChuckAttacks.Snap(ChuckBalance.snapCountdown(1)), now);
        }
        if (now == 230) drive(new ChuckAttacks.ThrowGlass(), now);
        if (now == 265) {
            view(p, c.add(0, 4, -14), c.add(0, 1.5, 0));
            boss.beginTransition(2);
        }
        if (now == 400) {
            view(p, c.add(-14, 12, -14), c);
            drive(new ChuckAttacks.TypewriterRain(), now);
        }
        if (now == 470) {
            view(p, c.add(0, 9, -20), c);
            drive(new ChuckAttacks.LineSweep(), now);
        }
        if (now == 620) {
            view(p, c.add(0, 5, -18), c.add(0, 4, 0));
            boss.beginTransition(3);
        }
        if (now == 760) {
            view(p, c.add(-20, 12, -20), c.add(0, 5, 0));
            boss.spawnPages(level);
        }
        if (now == 800) {
            view(p, dummy.position().add(-12, 8, -10), dummy.position());
            drive(new ChuckAttacks.HandSlam(), now);
        }
        if (now == 860) {
            view(p, dummy.position().add(-10, 6, -12), dummy.position());
            drive(new ChuckAttacks.InkEcho(), now);
        }
        if (now == 930) {
            view(p, c.add(0, 9, -24), c.add(0, 8, 0));
            boss.beginTransition(4);
        }
        if (now == 1040) {
            view(p, boss.position().add(-14, 9, -14), boss.position().add(0, 8, 0));
            boss.rollFakeCredits(level.getGameTime());
        }
        if (now == 1120) {
            view(p, c.add(0, 8, -24), c.add(0, 8, 0));
            boss.beginTransition(5);
        }
        if (now == 1255) {
            view(p, c.add(-6, 4, -16), c.add(0, 6, 0));
            drive(new ChuckAttacks.Narration(), now);
        }
        if (now == 1330) {
            if (driven != null) {
                driven.onEnd(boss);
                driven = null;
            }
            view(p, boss.position().add(-8, 3, -12), boss.position().add(0, 3, 0));
            BossHealthGuard.set(boss, 1.5f);
            boss.openWindow(100);
            boss.invulnerableTime = 0;
            boss.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, null), 10f);
        }
        if (now == 1560) {
            boss.invulnerableTime = 0;
            boss.hurt(level.damageSources().playerAttack(p), 5f);
        }
        if (now == 1810) finish(level);
    }

    private static void fight(Minecraft mc, ServerPlayer p, int now) {
        ServerLevel level = p.serverLevel();
        if (now == 1) {
            p.setGameMode(GameType.SURVIVAL);
            p.getAbilities().invulnerable = true;
            p.onUpdateAbilities();
            setUp(level);
            mc.options.hideGui = false;
            Vec3 stand = Vec3.atBottomCenterOf(spot).add(0, 0, -12);
            p.teleportTo(level, stand.x, stand.y, stand.z, 0, 10);
        }
        if (now == 10) summon(level, p);
        if (boss == null || boss.isRemoved()) return;
        if (now == 11) boss.setWritingIgnored(true);
        if (now % 5 == 0) {
            Vec3 to = boss.position().subtract(p.position()).multiply(1, 0, 1);
            Vec3 side = new Vec3(-to.z, 0, to.x).normalize().scale(Math.max(2.5, to.length() * 0.45));
            p.lookAt(EntityAnchorArgument.Anchor.EYES, boss.position().add(side).add(0, Math.min(6, boss.getBbHeight() * 0.5), 0));
        }
        p.setHealth(p.getMaxHealth());
        p.getFoodData().setFoodLevel(20);
        int[] at = {400, 650, 950, 1250};
        for (int i = 0; i < at.length; i++) {
            if (now != at[i]) continue;
            BossHealthGuard.set(boss, boss.getMaxHealth() * (ChuckBalance.threshold(i + 1) + 0.01f));
            boss.openWindow(5);
            boss.invulnerableTime = 0;
            boss.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, p), 40f);
        }
        if (now == 1590) finish(level);
    }
}
