package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.entity.boss.uncaged.LuciferUncagedEntity;
import org.papiricoh.supernaturalcraft.entity.boss.uncaged.UncagedSummoning;
import org.papiricoh.supernaturalcraft.entity.hellhound.HellhoundEntity;
import org.papiricoh.supernaturalcraft.hell.HellDimension;
import org.papiricoh.supernaturalcraft.hell.cage.CageController;
import org.papiricoh.supernaturalcraft.hell.cage.CageLayout;
import org.papiricoh.supernaturalcraft.hell.rift.HellRift;
import org.papiricoh.supernaturalcraft.hell.rift.HellRifts;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hell in the real renderer:
 * <ul>
 *   <li>{@code hell}: the Pit and the Cage from a gate, the island, inside the Cage, beneath the island;
 *   then the Rack, the Ash Wastes and Crowley's Corridors, and two hellhounds (one revealed).</li>
 *   <li>{@code uncaged}: the Cage opens and Lucifer Uncaged is let down; then each of his six looks.</li>
 *   <li>{@code rift}: a rift opened in the Overworld.</li>
 * </ul>
 */
final class HellPreview {

    private static int t = -1;
    private static final Map<ResourceKey<Biome>, BlockPos> REGIONS = new HashMap<>();
    private static LuciferUncagedEntity boss;

    private HellPreview() {
    }

    static boolean tick(Minecraft mc) {
        String scene = System.getenv("SN_PREVIEW");
        if (!"hell".equals(scene) && !"uncaged".equals(scene) && !"rift".equals(scene)) return false;
        var server = mc.getSingleplayerServer();
        t++;
        int now = t;
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
            ServerLevel hell = server.getLevel(HellDimension.LEVEL);
            switch (scene) {
                case "hell" -> hell(mc, p, hell, now);
                case "uncaged" -> uncaged(p, hell, now);
                default -> rift(p, now);
            }
        });
        switch (scene) {
            case "hell" -> shots(mc, "hell", now, 150, 260, 370, 470, 600, 730, 860, 960);
            case "uncaged" -> shots(mc, "uncaged", now, 70, 130, 175, 235, 275, 315, 355, 395, 435);
            default -> shots(mc, "rift", now, 120, 200);
        }
        int end = switch (scene) {
            case "hell" -> 980;
            case "uncaged" -> 460;
            default -> 220;
        };
        if (now >= end) mc.stop();
        return true;
    }

    private static void shots(Minecraft mc, String name, int now, int... at) {
        for (int s : at) {
            if (now == s) Screenshot.grab(mc.gameDirectory, String.format("sn_%s_%04d.png", name, now), mc.getMainRenderTarget(), m -> {
            });
        }
    }

    private static void view(ServerPlayer p, ServerLevel level, Vec3 from, Vec3 at) {
        p.teleportTo(level, from.x, from.y, from.z, p.getYRot(), p.getXRot());
        p.lookAt(EntityAnchorArgument.Anchor.EYES, at);
    }

    /** A standing spot in the nearest column of each region (searched outward from the Pit). */
    private static void findRegions(ServerLevel hell) {
        var source = hell.getChunkSource().getGenerator().getBiomeSource();
        var sampler = hell.getChunkSource().randomState().sampler();
        for (int r = 300; r < 4000 && REGIONS.size() < 3; r += 96) {
            for (int k = 0; k < 16; k++) {
                double a = Math.PI * 2 * k / 16;
                int x = (int) (Math.cos(a) * r), z = (int) (Math.sin(a) * r);
                var biome = source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(96), QuartPos.fromBlock(z), sampler);
                for (var key : List.of(HellDimension.THE_RACK, HellDimension.ASH_WASTES, HellDimension.CROWLEYS_CORRIDORS)) {
                    if (biome.is(key) && !REGIONS.containsKey(key)) REGIONS.put(key, new BlockPos(x, 96, z));
                }
            }
        }
    }

    private static void region(ServerPlayer p, ServerLevel hell, ResourceKey<Biome> key) {
        BlockPos at = REGIONS.get(key);
        if (at == null) return;
        BlockPos stand = HellRifts.findLanding(hell, at.getX(), at.getZ(), Direction.Axis.X);
        Vec3 eye = Vec3.atBottomCenterOf(stand).add(0, 1.6, 0);
        view(p, hell, eye, eye.add(12, -1, 4));
    }

    private static void hell(Minecraft mc, ServerPlayer p, ServerLevel hell, int now) {
        if (now == 1) {
            mc.options.hideGui = true;
            p.setGameMode(GameType.SPECTATOR);
            // The preview world comes from the GameTest world, which never generates structures: build the Cage by hand.
            org.papiricoh.supernaturalcraft.hell.cage.CageBuilder.build(hell, org.papiricoh.supernaturalcraft.hell.cage.CageBuilder.extent(), true);
            findRegions(hell);
        }
        if (now == 10) view(p, hell, new Vec3(86, 101, 0.5), new Vec3(0, 118, 0));
        if (now == 160) view(p, hell, new Vec3(16, 99, 16), new Vec3(0, 132, 0));
        if (now == 270) view(p, hell, new Vec3(0.5, 141, 7.5), new Vec3(0.5, 126.5, 0.5));
        if (now == 380) view(p, hell, new Vec3(34, 74, 34), new Vec3(0, 92, 0));
        if (now == 480) region(p, hell, HellDimension.THE_RACK);
        if (now == 610) region(p, hell, HellDimension.ASH_WASTES);
        if (now == 740) region(p, hell, HellDimension.CROWLEYS_CORRIDORS);
        if (now == 870) {
            p.setGameMode(GameType.CREATIVE);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            region(p, hell, HellDimension.THE_RACK);
            Vec3 look = p.getEyePosition().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(5));
            for (int i = 0; i < 2; i++) {
                HellhoundEntity h = AllEntities.HELLHOUND.get().create(hell);
                if (h == null) continue;
                BlockPos ground = p.blockPosition().offset(5, 0, i == 0 ? 0 : 3);
                while (hell.getBlockState(ground.below()).isAir() && ground.getY() > 40) ground = ground.below();
                while (!hell.getBlockState(ground).isAir() && ground.getY() < 200) ground = ground.above();
                h.moveTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, p.getYRot() + 180, 0);
                h.setNoAi(true);
                if (i == 1) h.reveal(2000);
                hell.addFreshEntity(h);
            }
        }
        if (now == 975) {
            for (Entity e : hell.getEntitiesOfClass(HellhoundEntity.class, new AABB(p.blockPosition()).inflate(16))) e.discard();
        }
    }

    private static void uncaged(ServerPlayer p, ServerLevel hell, int now) {
        if (now == 1) {
            Minecraft.getInstance().options.hideGui = true;
            p.setGameMode(GameType.SPECTATOR);
            org.papiricoh.supernaturalcraft.hell.cage.CageBuilder.build(hell, org.papiricoh.supernaturalcraft.hell.cage.CageBuilder.extent(), false);
            view(p, hell, new Vec3(12, 101, 12), new Vec3(0, 112, 0));
        }
        if (now == 40) {
            CageController.get(hell).set(hell, false);
            UncagedSummoning.summon(hell, CageLayout.ALTAR, null);
            var found = hell.getEntitiesOfClass(LuciferUncagedEntity.class, new AABB(CageLayout.THRONE).inflate(8));
            boss = found.isEmpty() ? null : found.getFirst();
        }
        if (now == 200) view(p, hell, new Vec3(5, 99.5, 6), new Vec3(0, 99.5, 0));
        if (boss != null) {
            for (int phase = 2; phase <= 6; phase++) {
                if (now == 200 + phase * 40 - 60 + 20) boss.forceLook(phase);
            }
        }
        if (now == 450) {
            if (boss != null) boss.discard();
            for (ArenaController a : List.copyOf(ArenaSavedData.get(hell).all())) a.restoreNow(hell);
            ArenaSavedData.get(hell).removeClosed();
            CageController.get(hell).set(hell, false);
        }
    }

    private static void rift(ServerPlayer p, int now) {
        if (now == 1) {
            Minecraft.getInstance().options.hideGui = true;
            p.setGameMode(GameType.SPECTATOR);
            ServerLevel level = p.server.overworld();
            level.setDayTime(18000);
            BlockPos anchor = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    p.blockPosition().offset(0, 0, 6));
            HellRifts.open(level, anchor, Direction.Axis.X, HellRift.Kind.OUTBOUND, null);
            view(p, level, Vec3.atBottomCenterOf(anchor).add(2.5, 2.2, -5), Vec3.atBottomCenterOf(anchor).add(0, 2.5, 0));
        }
        if (now == 140) {
            Vec3 at = p.position();
            view(p, p.serverLevel(), at.add(-3, 0, 3), at.add(1.5, 0, 6));
        }
    }
}
