package org.papiricoh.supernaturalcraft.client.dev;

import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemanEntity;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemanKind;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemanSteedEntity;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemenSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.death.DeathEntity;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.pestilence.Plague;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.war.WarEntity;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.war.WarIllusions;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.ArrayList;
import java.util.List;

/**
 * The Four Horsemen in the real renderer, well away from the origin.
 * <ul>
 *   <li>{@code SN_PREVIEW=horsemen}: the four on foot, then mounted, and their four horses loose (one saddled).</li>
 *   <li>{@code SN_PREVIEW=war_fight|famine_fight|pestilence_fight|death_fight}: a real fight over the shoulder (the
 *   player cannot be hurt and flies, but is no spectator, or the arena would call the fight abandoned), every phase
 *   forced along the way. Death's shows the clock on the HUD, the reapers, limbo's grey and the world of the dead.</li>
 * </ul>
 */
final class HorsemenPreview {

    private static int t = -1;
    private static BlockPos altar;
    private static HorsemanEntity boss;
    private static final List<Entity> shown = new ArrayList<>();

    private HorsemenPreview() {
    }

    private static HorsemanKind fightKind(String scene) {
        return switch (scene) {
            case "war_fight" -> HorsemanKind.WAR;
            case "famine_fight" -> HorsemanKind.FAMINE;
            case "pestilence_fight" -> HorsemanKind.PESTILENCE;
            case "death_fight" -> HorsemanKind.DEATH;
            default -> null;
        };
    }

    static boolean tick(Minecraft mc) {
        String scene = System.getenv("SN_PREVIEW");
        if (scene == null) return false;
        HorsemanKind kind = fightKind(scene);
        if (!"horsemen".equals(scene) && kind == null) return false;
        var server = mc.getSingleplayerServer();
        t++;
        int now = t;
        int end = kind == null ? 330 : kind == HorsemanKind.DEATH ? 1500 : 1150;
        if (kind != null && now == 1) mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
            if (kind == null) showcase(mc, p, now);
            else fight(mc, p, kind, now);
        });
        if (kind == null) {
            for (int s : new int[]{60, 100, 160, 200, 260, 300}) if (now == s) grab(mc, "sn_horsemen_%04d.png", now);
        } else if (now >= 60 && now % 40 == 0 && now < end - 10) {
            grab(mc, "sn_" + kind.id() + "_fight_%04d.png", now);
        }
        if (now >= end) mc.stop();
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

    private static void setUp(ServerLevel level, int x, int z) {
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        level.getChunk(x >> 4, z >> 4);
        altar = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
    }

    // --- the four, on foot and mounted, and their horses ----------------------------------------------

    private static void showcase(Minecraft mc, ServerPlayer p, int now) {
        ServerLevel level = p.serverLevel();
        Vec3 c;
        if (now == 1) {
            mc.options.hideGui = true;
            p.setGameMode(GameType.SPECTATOR);
            setUp(level, 460, 412);
            c = Vec3.atBottomCenterOf(altar);
            HorsemanKind[] kinds = HorsemanKind.values();
            for (int i = 0; i < kinds.length; i++) {
                HorsemanEntity h = kinds[i].type().create(level);
                h.setNoAi(true);
                h.moveTo(c.x - 6 + i * 4, c.y, c.z, 180, 0);
                h.setYHeadRot(180);
                h.setYBodyRot(180);
                level.addFreshEntity(h);
                shown.add(h);
            }
            view(p, c.add(0, 2.2, -9), c.add(0, 1.3, 0));
        }
        if (altar == null) return;
        c = Vec3.atBottomCenterOf(altar);
        if (now == 40) view(p, c.add(0, 2.0, -7), c.add(0, 1.2, 0));
        if (now == 120) {
            for (Entity e : shown) if (e instanceof HorsemanEntity h) h.setMounted(true);
            view(p, c.add(0, 3.2, -12), c.add(0, 1.8, 0));
        }
        if (now == 180) view(p, c.add(9, 3.2, -8), c.add(0, 1.8, 0));
        if (now == 230) {
            for (Entity e : shown) e.discard();
            shown.clear();
            HorsemanKind[] kinds = HorsemanKind.values();
            for (int i = 0; i < kinds.length; i++) {
                HorsemanSteedEntity s = AllEntities.HORSEMAN_STEED.get().create(level);
                s.finalizeSpawn(level, level.getCurrentDifficultyAt(altar), MobSpawnType.MOB_SUMMONED, null);
                s.setKind(kinds[i]);
                s.setNoAi(true);
                if (i == 0) s.equipSaddle(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SADDLE), null);
                s.moveTo(c.x - 7.5 + i * 5, c.y, c.z, 90, 0);
                s.setYBodyRot(90);
                s.setYHeadRot(90);
                level.addFreshEntity(s);
                shown.add(s);
            }
            view(p, c.add(0, 3, -11), c.add(0, 1.0, 0));
        }
        if (now == 280) view(p, c.add(-9, 2.5, -6), c.add(-2, 1.0, 0));
        if (now == 325) {
            for (Entity e : shown) e.discard();
            shown.clear();
        }
    }

    // --- a real fight ----------------------------------------------------------------------------------

    private static void fight(Minecraft mc, ServerPlayer p, HorsemanKind kind, int now) {
        ServerLevel level = p.serverLevel();
        if (now == 1) {
            mc.options.hideGui = false;
            p.setGameMode(GameType.SURVIVAL);
            p.getAbilities().invulnerable = true;
            p.getAbilities().mayfly = true;
            p.onUpdateAbilities();
            setUp(level, 400 + kind.ordinal() * 70, 520);
            Vec3 stand = Vec3.atBottomCenterOf(altar).add(0, 0, -11);
            p.teleportTo(level, stand.x, stand.y, stand.z, 0, 10);
        }
        if (now == 10) boss = HorsemenSummoning.summon(kind, level, altar, p);
        if (boss == null || boss.isRemoved()) return;
        if (now % 10 == 0 && boss.arena() != null && (p.distanceTo(boss) > 13 || boss.arena().horizontalDistance(p.position()) > boss.arena().radius() - 3)) {
            // Thrown out of the fight (his war cry, a charge): back in, a few steps from him toward the centre.
            Vec3 c = boss.arena().centerVec();
            Vec3 toward = c.subtract(boss.position()).multiply(1, 0, 1);
            Vec3 at = boss.position().add(toward.lengthSqr() < 1 ? new Vec3(0, 0, -8) : toward.normalize().scale(8)).add(0, 1.5, 0);
            p.teleportTo(level, at.x, at.y, at.z, p.getYRot(), p.getXRot());
        }
        if (now % 5 == 0) {
            Vec3 to = boss.position().subtract(p.position()).multiply(1, 0, 1);
            Vec3 side = new Vec3(-to.z, 0, to.x).normalize().scale(Math.max(2.5, to.length() * 0.45));
            p.lookAt(EntityAnchorArgument.Anchor.EYES, boss.position().add(side).add(0, 1.2, 0));
        }
        p.setHealth(p.getMaxHealth());
        p.getFoodData().setFoodLevel(20);
        if (Plague.stacks(p) > 3) Plague.cure(p, 0);
        int[] at = kind == HorsemanKind.DEATH ? new int[]{400, 750, 1100} : new int[]{400, 780};
        for (int i = 0; i < at.length; i++) {
            if (now == at[i]) {
                float share = 1f - (i + 1f) / boss.maxPhase() + 0.01f;
                BossHealthGuard.set(boss, boss.getMaxHealth() * share);
                boss.invulnerableTime = 0;
                boss.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, p), 30f);
            }
        }
        if (kind == HorsemanKind.WAR && now == 560 && boss instanceof WarEntity war) WarIllusions.cast(level, war, List.of(p));
        if (boss instanceof DeathEntity death) {
            // Time running short: the reapers show; then out of time: limbo; then the light out.
            if (now == 520) death.track(p).restore(300, -1);
            if (now == 620) death.track(p).restore(1, -1);
            if (now == 700 && death.exitOf(level, p) != null) {
                var exit = death.exitOf(level, p);
                p.teleportTo(level, exit.getX(), exit.getY(), exit.getZ(), p.getYRot(), p.getXRot());
            }
            if (now == 1380) death.track(p).restore(250, -1);
        }
        if (now == (kind == HorsemanKind.DEATH ? 1490 : 1140)) {
            boss.discard();
            for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
            ArenaSavedData.get(level).removeClosed();
            level.getEntitiesOfClass(Entity.class, new AABB(altar).inflate(40), e -> e instanceof HorsemanSteedEntity).forEach(Entity::discard);
        }
    }
}
