package org.papiricoh.supernaturalcraft.cain;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.cain.layout.CainFarmLayout;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaWorld;
import org.papiricoh.supernaturalcraft.datagen.SNStructures;
import org.papiricoh.supernaturalcraft.entity.boss.cain.CainEntity;
import org.papiricoh.supernaturalcraft.entity.boss.cain.CainSpoils;
import org.papiricoh.supernaturalcraft.entity.boss.cain.CainSummoning;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.List;

/**
 * Cain's world on the server (v0.21): making sure his farm stands where the world says it does (found in the world's own
 * structure start, or built in an older world when a hunter first comes near — {@link #ensureFarm}, idempotent), keeping him at
 * home ({@link #tickNpc}: one of him, only while a hunter near has put Lucifer back in his box and has not beaten him, never
 * during a fight there), starting his fight in his yard ({@link #challenge}), and his grave once he has fallen
 * ({@link #fallen}, {@link #useGrave}: the Mark offered again).
 */
public final class CainWorld {

    /** He is at home while an eligible hunter is this close. */
    public static final double NPC_RANGE = 48;
    /** The farm is checked (and built if missing) when a hunter comes this close. */
    public static final double FARM_RANGE = 112;
    /** Who may meet him: a hunter who has beaten Lucifer. */
    public static final String DEVIL_WENT_DOWN = "main/devil_went_down";

    private CainWorld() {
    }

    public static BlockPos home(CainSite.Site site) {
        return CainFarm.at(site, CainFarmLayout.NPC_HOME);
    }

    public static BlockPos arenaCentre(CainSite.Site site) {
        return CainFarm.at(site, CainFarmLayout.ARENA_CENTRE);
    }

    public static BlockPos graveSpot(CainSite.Site site) {
        return CainFarm.at(site, CainFarmLayout.GRAVE);
    }

    /** Where his address points: the lane outside his gate. */
    public static BlockPos address(ServerLevel level) {
        return CainFarm.at(CainSite.of(level), CainFarmLayout.ENTRY);
    }

    /** Whether {@code player} may find him at home: Lucifer beaten, Cain not (yet). */
    public static boolean expects(Player player) {
        return player instanceof ServerPlayer sp && AuthorWorld.done(sp, DEVIL_WENT_DOWN) && !AuthorWorld.done(sp, CainSpoils.ADVANCEMENT);
    }

    /** Once a second in the overworld. {@code players} are explicit so tests can pass their own. */
    public static void tick(ServerLevel level, List<? extends Player> players) {
        if (players.isEmpty()) return;
        CainSavedData data = CainSavedData.get(level);
        if (data.farm() == null) {
            boolean onRing = players.stream().anyMatch(p -> {
                double d = Math.hypot(p.getX(), p.getZ());
                return d > CainRing.MIN - FARM_RANGE * 2 && d < CainRing.MAX + FARM_RANGE * 2;
            });
            if (!onRing) return;
        }
        CainSite.Site site = CainSite.of(level);
        ensureFarm(level, data, site, players);
        ensureGrave(level, data);
        tickNpc(level, data, site, players);
    }

    /** If a hunter is near and the farm is not known to stand, find it (worldgen) or build it (older worlds). */
    public static void ensureFarm(ServerLevel level, CainSavedData data, CainSite.Site site, List<? extends Player> players) {
        if (data.built()) return;
        Vec3 c = Vec3.atCenterOf(site.origin());
        if (players.stream().noneMatch(p -> Math.hypot(p.getX() - c.x, p.getZ() - c.z) < FARM_RANGE)) return;
        BoundingBox box = CainFarm.fullBox(site.origin(), site.rotation());
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) if (!level.hasChunk(cx, cz)) return;
        }
        ensureNow(level, data, site);
    }

    /** As {@link #ensureFarm}, at once and with nobody near (commands, tests). Idempotent. @return the farm's origin */
    public static BlockPos ensureNow(ServerLevel level, CainSavedData data, CainSite.Site site) {
        if (data.built() && data.farm() != null) return data.farm();
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(SNStructures.CAIN_FARM);
        if (structure != null) {
            StructureStart start = level.getChunk(site.origin()).getStartForStructure(structure);
            if (start != null && start.isValid()) {
                for (StructurePiece piece : start.getPieces()) {
                    if (piece instanceof CainFarmPiece fp) {
                        data.setFarm(fp.origin(), fp.rotation(), true);
                        return fp.origin();
                    }
                }
            }
        }
        // An older world (or one without structures): the farm is raised now.
        BlockPos origin = CainFarm.originOn(level, site.origin().getX(), site.origin().getZ());
        CainFarm.placeAt(level, origin, site.rotation());
        data.setFarm(origin, site.rotation(), true);
        SupernaturalCraft.LOGGER.info("Cain's farm was built at {}", origin.toShortString());
        return origin;
    }

    /** His headstone stands where he was laid, once its chunk is loaded. */
    static void ensureGrave(ServerLevel level, CainSavedData data) {
        BlockPos g = data.grave();
        if (g == null || !level.isLoaded(g)) return;
        BlockState s = level.getBlockState(g);
        if (s.is(AllBlocks.GRAVE_HEADSTONE.get()) || !s.canBeReplaced()) return;
        level.setBlockAndUpdate(g, AllBlocks.GRAVE_HEADSTONE.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                Direction.from2DDataValue(data.rotation() + 2)));
    }

    /** He is home while an eligible hunter is near, outside any fight; never two of him. */
    public static void tickNpc(ServerLevel level, CainSavedData data, CainSite.Site site, List<? extends Player> players) {
        if (!data.built()) return;
        BlockPos home = home(site);
        Vec3 c = Vec3.atBottomCenterOf(home);
        if (players.stream().noneMatch(p -> p.position().distanceToSqr(c) < 160 * 160)) return;
        List<CainNpcEntity> npcs = level.getEntitiesOfClass(CainNpcEntity.class, new AABB(home).inflate(64));
        boolean wanted = players.stream().anyMatch(p -> p.isAlive() && !p.isSpectator() && p.position().distanceTo(c) < NPC_RANGE * 2 && expects(p));
        if (RowenaWorld.fightAt(level, c) || !wanted) {
            for (CainNpcEntity n : npcs) n.discard();
            return;
        }
        for (int i = 1; i < npcs.size(); i++) npcs.get(i).discard();
        if (!npcs.isEmpty()) return;
        boolean near = players.stream().anyMatch(p -> p.isAlive() && !p.isSpectator() && p.position().distanceTo(c) < NPC_RANGE && expects(p));
        if (!near || !level.isPositionEntityTicking(home)) return;
        spawnNpc(level, site);
    }

    public static @Nullable CainNpcEntity spawnNpc(ServerLevel level, CainSite.Site site) {
        CainNpcEntity npc = AllEntities.CAIN_NPC.get().create(level);
        if (npc == null) return null;
        BlockPos home = home(site);
        npc.home(home);
        npc.moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0, 0);
        level.addFreshEntity(npc);
        return npc;
    }

    /**
     * {@code challenger} has called him out: in his yard if he is at his farm (its arena centre, his hives), else where he
     * stands. The peaceful Cain goes; the boss stands in his place. @return the boss, or null if another fight holds the world
     */
    public static @Nullable CainEntity challenge(ServerLevel level, CainNpcEntity npc, ServerPlayer challenger) {
        CainSavedData data = CainSavedData.get(level);
        BlockPos centre = npc.blockPosition(), spot = npc.blockPosition();
        List<BlockPos> hives = List.of();
        if (data.built() && data.farm() != null) {
            CainSite.Site site = new CainSite.Site(data.farm(), data.rotation());
            BlockPos ac = arenaCentre(site);
            if (ac.distSqr(npc.blockPosition()) < 64 * 64) {
                centre = ac;
                spot = CainFarm.at(site, CainFarmLayout.BOSS_SPAWN);
                hives = CainFarm.hives(site);
            }
        }
        CainEntity boss = CainSummoning.summon(level, centre, spot, hives, challenger);
        if (boss != null) npc.discard();
        return boss;
    }

    /** By command: in his yard if {@code at} is near his built farm, else at {@code at}. */
    public static @Nullable CainEntity summonNear(ServerLevel level, BlockPos at, @Nullable ServerPlayer by) {
        CainSavedData data = CainSavedData.get(level);
        if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD && data.built() && data.farm() != null) {
            CainSite.Site site = new CainSite.Site(data.farm(), data.rotation());
            BlockPos ac = arenaCentre(site);
            if (ac.distSqr(at) < 64 * 64) {
                for (CainNpcEntity n : level.getEntitiesOfClass(CainNpcEntity.class, new AABB(ac).inflate(64))) n.discard();
                return CainSummoning.summon(level, ac, CainFarm.at(site, CainFarmLayout.BOSS_SPAWN), CainFarm.hives(site), by);
            }
        }
        BlockPos spot = at.offset(0, 0, 5);
        return CainSummoning.summon(level, at, spot, List.of(), by);
    }

    /** He has fallen: his grave is dug at his farm (the first time), and stands from now on. */
    public static void fallen(ServerLevel level, CainEntity cain) {
        if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
        CainSavedData data = CainSavedData.get(level);
        if (data.grave() != null || data.farm() == null) return;
        data.setGrave(graveSpot(new CainSite.Site(data.farm(), data.rotation())));
        ensureGrave(level, data);
    }

    /** Whether {@code player} stands within {@code reach} of his grave. */
    public static boolean nearGrave(ServerPlayer player, double reach) {
        BlockPos g = CainSavedData.get(player.serverLevel()).grave();
        return g != null && player.level().dimension() == net.minecraft.world.level.Level.OVERWORLD
                && player.position().distanceToSqr(Vec3.atBottomCenterOf(g)) <= reach * reach;
    }

    /** {@code player} used the block at {@code pos}: if it is his grave, the Mark is offered again. @return whether it was */
    public static boolean useGrave(ServerPlayer player, BlockPos pos) {
        if (player.level().dimension() != net.minecraft.world.level.Level.OVERWORLD) return false;
        BlockPos g = CainSavedData.get(player.serverLevel()).grave();
        if (g == null || !g.equals(pos)) return false;
        if (CainSpoils.mayTakeAtGrave(player)) {
            CainSpoils.offerAtGrave(player);
        } else {
            String key = org.papiricoh.supernaturalcraft.mark.Marks.marked(player) ? "message.supernaturalcraft.cain.grave_marked"
                    : "message.supernaturalcraft.cain.grave_quiet";
            player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
        return true;
    }
}
