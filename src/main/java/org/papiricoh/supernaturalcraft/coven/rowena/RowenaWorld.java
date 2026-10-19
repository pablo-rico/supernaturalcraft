package org.papiricoh.supernaturalcraft.coven.rowena;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.coven.layout.RowenaHouseLayout;
import org.papiricoh.supernaturalcraft.datagen.SNStructures;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.List;

/**
 * Rowena's world on the server (v0.21): making sure her house stands where the world says it does (found in the world's own
 * structure start, or built in an older world when a hunter first comes near — {@link #ensureHouse}, idempotent), and keeping
 * her at home while hunters are about ({@link #tickNpc}: one of her, never during a fight at the house).
 */
public final class RowenaWorld {

    /** She is at home while a hunter is this close. */
    public static final double NPC_RANGE = 48;
    /** The house is checked (and built if missing) when a hunter comes this close. */
    public static final double HOUSE_RANGE = 96;

    private RowenaWorld() {
    }

    public static BlockPos home(RowenaSite.Site site) {
        return RowenaHouse.at(site, RowenaHouseLayout.NPC_HOME);
    }

    public static BlockPos bowl(RowenaSite.Site site) {
        return RowenaHouse.at(site, RowenaHouseLayout.BOWL);
    }

    /** Where a fight in her house (P6) is centred, and how far its arena reaches. */
    public static BlockPos arenaCentre(RowenaSite.Site site) {
        return RowenaHouse.at(site, RowenaHouseLayout.ARENA_CENTRE);
    }

    /** Once a second in the overworld. {@code players} are explicit so tests can pass their own. */
    public static void tick(ServerLevel level, List<? extends Player> players) {
        if (players.isEmpty()) return;
        RowenaSavedData data = RowenaSavedData.get(level);
        if (data.house() == null) {
            // Nobody needs the site yet unless someone wanders out to the ring.
            boolean onRing = players.stream().anyMatch(p -> {
                double d = Math.hypot(p.getX(), p.getZ());
                return d > RowenaRing.MIN - HOUSE_RANGE * 2 && d < RowenaRing.MAX + HOUSE_RANGE * 2;
            });
            if (!onRing) return;
        }
        RowenaSite.Site site = RowenaSite.of(level);
        ensureHouse(level, data, site, players);
        tickNpc(level, data, RowenaSite.of(level), players);
    }

    /** If a hunter is near and the house is not known to stand, find it (worldgen) or build it (older worlds). */
    public static void ensureHouse(ServerLevel level, RowenaSavedData data, RowenaSite.Site site, List<? extends Player> players) {
        if (data.built()) return;
        Vec3 c = Vec3.atCenterOf(site.origin());
        if (players.stream().noneMatch(p -> Math.hypot(p.getX() - c.x, p.getZ() - c.z) < HOUSE_RANGE)) return;
        BoundingBox box = RowenaHouse.fullBox(site.origin(), site.rotation());
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) if (!level.hasChunk(cx, cz)) return;
        }
        ensureNow(level, data, site);
    }

    /** As {@link #ensureHouse}, at once and with nobody near (commands, tests). Idempotent. @return the house's origin */
    public static BlockPos ensureNow(ServerLevel level, RowenaSavedData data, RowenaSite.Site site) {
        if (data.built() && data.house() != null) return data.house();
        if (level.getBlockState(bowl(site)).is(AllBlocks.SPELL_BOWL.get())) {
            data.setHouse(site.origin(), site.rotation(), true);
            return site.origin();
        }
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(SNStructures.ROWENA_HOUSE);
        if (structure != null) {
            StructureStart start = level.getChunk(site.origin()).getStartForStructure(structure);
            if (start != null && start.isValid()) {
                for (StructurePiece piece : start.getPieces()) {
                    if (piece instanceof RowenaHousePiece hp) {
                        data.setHouse(hp.origin(), hp.rotation(), true);
                        return hp.origin();
                    }
                }
            }
        }
        // An older world (or one without structures): she moves in now.
        BlockPos origin = RowenaHouse.originOn(level, site.origin().getX(), site.origin().getZ());
        RowenaHouse.placeAt(level, origin, site.rotation());
        data.setHouse(origin, site.rotation(), true);
        SupernaturalCraft.LOGGER.info("Rowena's house was built at {}", origin.toShortString());
        return origin;
    }

    /** She is at home while hunters are near, outside any fight; never two of her. */
    public static void tickNpc(ServerLevel level, RowenaSavedData data, RowenaSite.Site site, List<? extends Player> players) {
        if (!data.built()) return;
        BlockPos home = home(site);
        Vec3 c = Vec3.atBottomCenterOf(home);
        if (players.stream().noneMatch(p -> p.position().distanceToSqr(c) < 160 * 160)) return;
        List<RowenaNpcEntity> npcs = level.getEntitiesOfClass(RowenaNpcEntity.class, new AABB(home).inflate(64));
        if (fightAt(level, c)) {
            for (RowenaNpcEntity n : npcs) n.discard();
            return;
        }
        for (int i = 1; i < npcs.size(); i++) npcs.get(i).discard();
        if (!npcs.isEmpty()) return;
        boolean near = players.stream().anyMatch(p -> p.isAlive() && !p.isSpectator() && p.position().distanceTo(c) < NPC_RANGE);
        if (!near || !level.isPositionEntityTicking(home)) return;
        spawnNpc(level, site);
    }

    public static @Nullable RowenaNpcEntity spawnNpc(ServerLevel level, RowenaSite.Site site) {
        RowenaNpcEntity npc = AllEntities.ROWENA_NPC.get().create(level);
        if (npc == null) return null;
        BlockPos home = home(site);
        npc.home(home);
        npc.moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0, 0);
        level.addFreshEntity(npc);
        return npc;
    }

    /** Whether a fight (an arena) holds the ground round {@code at}. */
    public static boolean fightAt(ServerLevel level, Vec3 at) {
        for (ArenaController a : ArenaSavedData.get(level).all()) {
            if (a.status() != ArenaController.Status.CLOSED && a.horizontalDistance(at) <= a.radius() + 16) return true;
        }
        return false;
    }
}
