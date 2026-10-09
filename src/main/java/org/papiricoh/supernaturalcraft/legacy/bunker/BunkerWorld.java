package org.papiricoh.supernaturalcraft.legacy.bunker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.legacy.HenryEntity;
import org.papiricoh.supernaturalcraft.legacy.Legacies;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllMapDecorations;

import java.util.List;

/**
 * The bunker on the server (v0.17): making sure it stands where the world says (built now in worlds that never generated it),
 * Henry at home in its war room while a member is near, and the map that leads there.
 */
public final class BunkerWorld {

    /** The bunker is checked (and built if missing) when a hunter comes this close; Henry is home while a member is this close. */
    public static final double CHECK_RANGE = 96, HOME_RANGE = 48;
    public static final int MAP_COLOR = 0x3E5F3A;
    public static final String MAP_NAME = "item.supernaturalcraft.bunker_map";

    private BunkerWorld() {
    }

    // --- places ---------------------------------------------------------------------------------------------------------

    public static BunkerLocator.Site site(ServerLevel level) {
        return BunkerLocator.of(level);
    }

    public static BlockPos door(BunkerLocator.Site site) {
        return BunkerBuilder.at(site.origin(), site.rotation(), BunkerLayout.DOOR);
    }

    public static BlockPos outside(BunkerLocator.Site site) {
        return BunkerBuilder.at(site.origin(), site.rotation(), BunkerLayout.OUTSIDE);
    }

    public static BlockPos mapTable(BunkerLocator.Site site) {
        return BunkerBuilder.at(site.origin(), site.rotation(), BunkerLayout.MAP_TABLE);
    }

    public static BlockPos henrySpot(BunkerLocator.Site site) {
        return BunkerBuilder.at(site.origin(), site.rotation(), BunkerLayout.HENRY);
    }

    public static BlockPos warRoom(BunkerLocator.Site site) {
        return BunkerBuilder.at(site.origin(), site.rotation(), BunkerLayout.WAR_ROOM);
    }

    /** The yaw of Henry at his spot looking at the map table (local +X). */
    public static float henryYaw(int rotation) {
        int[] d = BunkerLayout.rotate(1, 0, rotation);
        return (float) Math.toDegrees(Math.atan2(-d[0], d[1]));
    }

    // --- every second -----------------------------------------------------------------------------------------------------

    /** Once a second in the overworld; {@code players} explicit so tests can pass their own. */
    public static void tick(ServerLevel level, List<? extends Player> players) {
        if (players.isEmpty()) return;
        BunkerSavedData data = BunkerSavedData.get(level);
        if (data.origin() == null) {
            // Nobody needs the site yet unless someone wanders out to the ring.
            int[] ring = BunkerPlacement.ring();
            boolean onRing = players.stream().anyMatch(p -> {
                double d = Math.hypot(p.getX(), p.getZ());
                return d > ring[0] - CHECK_RANGE * 2 && d < ring[1] + CHECK_RANGE * 2;
            });
            if (!onRing) return;
        }
        BunkerLocator.Site site = BunkerLocator.of(level);
        ensureBunker(level, data, site, players);
        tickHenry(level, data, BunkerLocator.of(level), players);
    }

    /** If a hunter is near and the bunker is not known to stand, find it (worldgen) or build it (older worlds). */
    public static void ensureBunker(ServerLevel level, BunkerSavedData data, BunkerLocator.Site site, List<? extends Player> players) {
        if (data.built()) return;
        Vec3 c = Vec3.atCenterOf(site.origin());
        if (players.stream().noneMatch(p -> Math.hypot(p.getX() - c.x, p.getZ() - c.z) < CHECK_RANGE)) return;
        BoundingBox box = BunkerBuilder.box(site.origin(), site.rotation());
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) if (!level.hasChunk(cx, cz)) return;
        }
        if (level.getBlockState(mapTable(site)).is(AllBlocks.MAP_TABLE.get())) {
            data.setBunker(site.origin(), site.rotation(), true);
            return;
        }
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(BunkerStructure.KEY);
        if (structure != null) {
            StructureStart start = level.getChunk(site.origin()).getStartForStructure(structure);
            if (start != null && start.isValid()) {
                for (StructurePiece piece : start.getPieces()) {
                    if (piece instanceof BunkerPiece bp) {
                        data.setBunker(bp.origin(), bp.rotation(), true);
                        return;
                    }
                }
            }
        }
        BlockPos origin = BunkerBuilder.originOn(level, site.origin().getX(), site.origin().getZ());
        BunkerBuilder.placeAt(level, origin, site.rotation());
        data.setBunker(origin, site.rotation(), true);
        SupernaturalCraft.LOGGER.info("The Men of Letters' bunker was built at {}", origin.toShortString());
    }

    /** Henry is home by the map table while a member is near; never two of him. */
    public static void tickHenry(ServerLevel level, BunkerSavedData data, BunkerLocator.Site site, List<? extends Player> players) {
        if (!data.built()) return;
        BlockPos spot = henrySpot(site);
        List<HenryEntity> home = homeHenries(level, site);
        for (int i = 1; i < home.size(); i++) home.get(i).discard();
        if (!home.isEmpty()) return;
        Vec3 at = Vec3.atBottomCenterOf(spot);
        boolean near = players.stream().anyMatch(p -> p.isAlive() && !p.isSpectator() && Legacies.member(p) && p.position().distanceTo(at) < HOME_RANGE);
        if (!near || !level.isPositionEntityTicking(spot)) return;
        spawnHenry(level, site);
    }

    public static List<HenryEntity> homeHenries(ServerLevel level, BunkerLocator.Site site) {
        return level.getEntitiesOfClass(HenryEntity.class, new AABB(henrySpot(site)).inflate(64), h -> !h.calling());
    }

    public static @Nullable HenryEntity spawnHenry(ServerLevel level, BunkerLocator.Site site) {
        HenryEntity h = AllEntities.HENRY.get().create(level);
        if (h == null) return null;
        h.home(henrySpot(site), henryYaw(site.rotation()));
        level.addFreshEntity(h);
        return h;
    }

    // --- the map ----------------------------------------------------------------------------------------------------------

    /** A map of the bunker's hut, marked with the order's sign (Henry hands it over with the key). */
    public static ItemStack map(ServerLevel level) {
        BunkerLocator.Site site = BunkerLocator.of(level);
        BlockPos target = door(site);
        ServerLevel overworld = level.getServer().overworld();
        ItemStack map = MapItem.create(overworld, target.getX(), target.getZ(), (byte) 2, true, true);
        MapItem.renderBiomePreviewMap(overworld, map);
        MapItemSavedData.addTargetDecoration(map, target, "bunker", AllMapDecorations.BUNKER);
        map.set(DataComponents.ITEM_NAME, Component.translatable(MAP_NAME));
        map.set(DataComponents.MAP_COLOR, new MapItemColor(MAP_COLOR));
        return map;
    }
}
