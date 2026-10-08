package org.papiricoh.supernaturalcraft.client.dev;

import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.entity.boss.lilith.LilithAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.lilith.LilithEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lilith.LilithSummoning;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;

import java.util.List;

/**
 * Lilith in the real renderer. {@code SN_PREVIEW=lilith}: her flash, the headstones, a burst of white
 * light, her three looks and her death. {@code SN_PREVIEW=lilith_fight}: a real fight over the shoulder
 * (the player cannot be hurt), phases 2 and 3 forced along the way. Both well away from the origin.
 */
final class LilithPreview {

    private static int t = -1;
    private static BlockPos altar;
    private static LilithEntity boss;

    private LilithPreview() {
    }

    static boolean tick(Minecraft mc) {
        String scene = System.getenv("SN_PREVIEW");
        if (!"lilith".equals(scene) && !"lilith_fight".equals(scene)) return false;
        boolean fight = "lilith_fight".equals(scene);
        var server = mc.getSingleplayerServer();
        t++;
        int now = t;
        if (fight && now == 1) mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
            if (fight) fight(mc, p, now);
            else scene(mc, p, now);
        });
        int[] shots = fight ? null : new int[]{50, 75, 120, 152, 215, 300, 395, 430};
        if (shots != null) {
            for (int s : shots) if (now == s) grab(mc, "sn_lilith_%04d.png", now);
        } else if (now >= 100 && now % 40 == 0 && now < 1290) {
            grab(mc, "sn_lilith_fight_%04d.png", now);
        }
        if (now >= (fight ? 1300 : 470)) mc.stop();
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
        // Well away from the origin, where the GameTests build Lucifer's Cage.
        level.getChunk(400 >> 4, 412 >> 4);
        altar = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(400, 0, 412));
    }

    private static void summon(ServerLevel level, ServerPlayer p) {
        LilithSummoning.summon(level, altar, p);
        var found = level.getEntitiesOfClass(LilithEntity.class, new AABB(altar).inflate(16));
        boss = found.isEmpty() ? null : found.getFirst();
    }

    private static void finish(ServerLevel level) {
        if (boss != null && !boss.isRemoved()) boss.discard();
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
    }

    private static void scene(Minecraft mc, ServerPlayer p, int now) {
        ServerLevel level = p.serverLevel();
        if (now == 1) {
            p.setGameMode(GameType.SPECTATOR);
            setUp(mc, level);
            view(p, Vec3.atBottomCenterOf(altar).add(0, 3, -9), Vec3.atBottomCenterOf(altar).add(0, 1, 6));
        }
        if (now == 10) summon(level, p);
        if (boss == null) return;
        Vec3 c = Vec3.atBottomCenterOf(altar);
        if (now == 105) view(p, c.add(-12, 12, -12), c);
        if (now == 140) view(p, boss.position().add(-6, 2.5, -6), boss.position().add(0, 1.4, 0));
        if (now == 150) LilithAttacks.WhiteLight.burst(boss);
        if (now == 170) boss.beginTransition(2);
        if (now == 260) boss.beginTransition(3);
        if (now == 350) {
            BossHealthGuard.set(boss, 1f);
            boss.invulnerableTime = 0;
            boss.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, null), 10f);
        }
        if (now == 465) finish(level);
    }

    private static void fight(Minecraft mc, ServerPlayer p, int now) {
        ServerLevel level = p.serverLevel();
        if (now == 1) {
            p.setGameMode(GameType.SURVIVAL);
            p.getAbilities().invulnerable = true;
            p.onUpdateAbilities();
            setUp(mc, level);
            Vec3 stand = Vec3.atBottomCenterOf(altar).add(0, 0, -11);
            p.teleportTo(level, stand.x, stand.y, stand.z, 0, 10);
        }
        if (now == 10) summon(level, p);
        if (boss == null || boss.isRemoved()) return;
        if (now % 5 == 0) {
            // Look past her, to one side, so the over-the-shoulder camera does not hide her behind the player.
            Vec3 to = boss.position().subtract(p.position()).multiply(1, 0, 1);
            Vec3 side = new Vec3(-to.z, 0, to.x).normalize().scale(Math.max(2.5, to.length() * 0.45));
            p.lookAt(EntityAnchorArgument.Anchor.EYES, boss.position().add(side).add(0, 1.2, 0));
        }
        p.setHealth(p.getMaxHealth());
        p.getFoodData().setFoodLevel(20);
        if (now == 500 || now == 900) {
            BossHealthGuard.set(boss, boss.getMaxHealth() * (now == 500 ? 0.68f : 0.34f));
            boss.invulnerableTime = 0;
            boss.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, p), 20f);
        }
        if (now == 1290) finish(level);
    }
}
