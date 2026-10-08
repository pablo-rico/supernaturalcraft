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
import org.papiricoh.supernaturalcraft.entity.boss.azazel.AzazelEntity;
import org.papiricoh.supernaturalcraft.entity.boss.azazel.AzazelSummoning;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;

import java.util.List;

/**
 * Azazel in the real renderer ({@code SN_PREVIEW=azazel}): the smoke taking shape, the rails charged
 * round the circle, Azazel held in them, the cracked vessel, the smoke rush and his death.
 */
final class AzazelPreview {

    private static int t = -1;
    private static BlockPos altar;
    private static AzazelEntity boss;

    private AzazelPreview() {
    }

    static boolean tick(Minecraft mc) {
        if ("azazel_fight".equals(System.getenv("SN_PREVIEW"))) return fight(mc);
        if (!"azazel".equals(System.getenv("SN_PREVIEW"))) return false;
        var server = mc.getSingleplayerServer();
        t++;
        int now = t;
        server.execute(() -> scene(mc, server.getPlayerList().getPlayers().getFirst(), now));
        for (int s : new int[]{50, 75, 120, 150, 215, 260, 300, 345}) {
            if (now == s) Screenshot.grab(mc.gameDirectory, String.format("sn_azazel_%04d.png", now), mc.getMainRenderTarget(), m -> {
            });
        }
        if (now >= 380) mc.stop();
        return true;
    }

    /**
     * {@code SN_PREVIEW=azazel_fight}: a real fight seen over the shoulder. The player is a challenger
     * (survival) who cannot be hurt; phase 2 is forced halfway. A shot every two seconds.
     */
    private static boolean fight(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        t++;
        int now = t;
        if (now == 1) mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
            ServerLevel level = p.serverLevel();
            if (now == 1) {
                mc.options.hideGui = true;
                p.setGameMode(GameType.SURVIVAL);
                p.getAbilities().invulnerable = true;
                p.onUpdateAbilities();
                level.setDayTime(18000);
                for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
                ArenaSavedData.get(level).removeClosed();
                level.getChunk(400 >> 4, 412 >> 4);
                altar = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(400, 0, 412));
                Vec3 stand = Vec3.atBottomCenterOf(altar).add(0, 0, -11);
                p.teleportTo(level, stand.x, stand.y, stand.z, 0, 10);
                // A couple of zombies for his smoke to ride.
                for (int i = 0; i < 2; i++) {
                    var z = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
                    if (z == null) continue;
                    z.moveTo(altar.getX() + 8 + i * 2, altar.getY(), altar.getZ() + 4, 0, 0);
                    z.setNoAi(false);
                    z.setPersistenceRequired();
                    level.addFreshEntity(z);
                }
            }
            if (now == 10) {
                AzazelSummoning.summon(level, altar, p);
                var found = level.getEntitiesOfClass(AzazelEntity.class, new AABB(altar).inflate(16));
                boss = found.isEmpty() ? null : found.getFirst();
            }
            if (boss == null || boss.isRemoved()) return;
            if (now % 5 == 0) {
                // Look past him, to one side, so the over-the-shoulder camera does not hide him behind the player.
                Vec3 to = boss.position().subtract(p.position()).multiply(1, 0, 1);
                Vec3 side = new Vec3(-to.z, 0, to.x).normalize().scale(Math.max(2.5, to.length() * 0.45));
                p.lookAt(EntityAnchorArgument.Anchor.EYES, boss.position().add(side).add(0, 1.2, 0));
            }
            p.setHealth(p.getMaxHealth());
            p.getFoodData().setFoodLevel(20);
            if (now == 600) {
                BossHealthGuard.set(boss, boss.getMaxHealth() * 0.51f);
                boss.invulnerableTime = 0;
                boss.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, p), 20f);
            }
            if (now == 1290) {
                boss.discard();
                for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
                ArenaSavedData.get(level).removeClosed();
            }
        });
        if (now >= 100 && now % 40 == 0 && now < 1290) {
            Screenshot.grab(mc.gameDirectory, String.format("sn_azazel_fight_%04d.png", now), mc.getMainRenderTarget(), m -> {
            });
        }
        if (now >= 1300) mc.stop();
        return true;
    }

    private static void view(ServerPlayer p, Vec3 from, Vec3 at) {
        p.teleportTo(p.serverLevel(), from.x, from.y, from.z, p.getYRot(), p.getXRot());
        p.lookAt(EntityAnchorArgument.Anchor.EYES, at);
    }

    private static void scene(Minecraft mc, ServerPlayer p, int now) {
        ServerLevel level = p.serverLevel();
        if (now == 1) {
            mc.options.hideGui = true;
            p.setGameMode(GameType.SPECTATOR);
            level.setDayTime(18000);
            for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
            ArenaSavedData.get(level).removeClosed();
            // Well away from the origin, where the GameTests build Lucifer's Cage.
            level.getChunk(400 >> 4, 412 >> 4);
            altar = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(400, 0, 412));
            view(p, Vec3.atBottomCenterOf(altar).add(0, 3, -9), Vec3.atBottomCenterOf(altar).add(0, 1, 6));
        }
        if (now == 10) {
            AzazelSummoning.summon(level, altar, p);
            var found = level.getEntitiesOfClass(AzazelEntity.class, new AABB(altar).inflate(16));
            boss = found.isEmpty() ? null : found.getFirst();
        }
        if (boss == null) return;
        Vec3 c = Vec3.atBottomCenterOf(altar);
        if (now == 105) view(p, c.add(-9, 8, -9), c.add(0, 0, 2));
        if (now == 128) {
            boss.teleportTo(c.x + 1.5, c.y, c.z + 1.5);
        }
        if (now == 140) view(p, c.add(-4, 3, -4), boss.position().add(0, 1, 0));
        if (now == 170) {
            boss.trap().forceRecharge(level, boss.arena());
            boss.teleportTo(c.x, c.y, c.z + 9);
            boss.beginTransition(2);
        }
        if (now == 252) {
            boss.setSmoke(true);
            view(p, boss.position().add(-4, 2, -5), boss.position().add(0, 1, 0));
        }
        if (now == 270) {
            boss.setSmoke(false);
            BossHealthGuard.set(boss, 1f);
            boss.invulnerableTime = 0;
            boss.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, null), 10f);
        }
        if (now == 375) {
            if (!boss.isRemoved()) boss.discard();
            for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
            ArenaSavedData.get(level).removeClosed();
        }
    }
}
