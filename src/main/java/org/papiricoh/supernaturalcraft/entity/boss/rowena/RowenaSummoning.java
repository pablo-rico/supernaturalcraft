package org.papiricoh.supernaturalcraft.entity.boss.rowena;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.coven.layout.RowenaHouseLayout;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaNpcEntity;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaRelation;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaSavedData;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaServerHandlers;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaSite;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaWorld;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/**
 * Calls Rowena out as a boss (v0.21, P6). A hunter who kept the Book of the Damned challenges her by using the Book at her house
 * at night ({@link #challenge}: inside the house — within {@link #HOUSE_REACH} of its middle — or in its front garden, within
 * {@link RowenaHouseLayout#ARENA_RADIUS} + {@link #SLACK} of the arena's centre); a command calls her anywhere ({@link #summon}).
 * The arena is the front garden ({@link ArenaTheme#ROWENA_HOUSE}, radius {@link RowenaHouseLayout#ARENA_RADIUS}: the layout's
 * contract keeps every block entity outside it, and the arena restores the garden after). Her host self
 * ({@link RowenaNpcEntity}) is not at home during the fight.
 */
public final class RowenaSummoning {

    /** How far beyond the arena's edge a challenge still counts as "in her house". */
    public static final double SLACK = 4;
    /** How far from the house's middle (the hall) still counts as "inside her house". */
    public static final double HOUSE_REACH = 10;

    /** Why a challenge was refused (null: she came). */
    public enum Refusal {NOT_KEPT, NOT_HERE, NOT_NIGHT, BUSY}

    private RowenaSummoning() {
    }

    /** Why {@code player} cannot challenge her where they stand now, or null if they can. */
    public static @Nullable Refusal check(ServerLevel level, ServerPlayer player) {
        if (!org.papiricoh.supernaturalcraft.coven.ending.WitchBossSlot.maySummon(RowenaServerHandlers.relation(player).book(),
                org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss.ROWENA)) return Refusal.NOT_KEPT;
        BlockPos centre = centre(level);
        if (centre == null || level.dimension() != Level.OVERWORLD || !atHer(level, player.getX(), player.getZ())) return Refusal.NOT_HERE;
        if (org.papiricoh.supernaturalcraft.coven.rowena.RowenaWorld.fightAt(level, net.minecraft.world.phys.Vec3.atCenterOf(centre))) return Refusal.BUSY;
        if (!level.isNight()) return Refusal.NOT_NIGHT;
        return null;
    }

    /** Whether ({@code x}, {@code z}) is in her house or its front garden. */
    public static boolean atHer(ServerLevel level, double x, double z) {
        BlockPos centre = centre(level);
        RowenaSavedData data = RowenaSavedData.get(level);
        if (centre == null || data.house() == null) return false;
        BlockPos house = data.house();
        return Math.hypot(x - (centre.getX() + 0.5), z - (centre.getZ() + 0.5)) <= RowenaHouseLayout.ARENA_RADIUS + SLACK
                || Math.hypot(x - (house.getX() + 0.5), z - (house.getZ() + 0.5)) <= HOUSE_REACH;
    }

    /** The centre of a fight in her house, or null if the house is not known to stand. */
    public static @Nullable BlockPos centre(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) return null;
        RowenaSavedData data = RowenaSavedData.get(level);
        if (!data.built() || data.house() == null) return null;
        return RowenaWorld.arenaCentre(new RowenaSite.Site(data.house(), data.rotation()));
    }

    /** {@code player} uses the Book in her house: she comes, or they are told why not. @return her, or null */
    public static @Nullable RowenaEntity challenge(ServerLevel level, ServerPlayer player) {
        Refusal why = check(level, player);
        if (why != null) {
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.rowena.challenge." + why.name().toLowerCase(java.util.Locale.ROOT))
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return null;
        }
        RowenaEntity boss = summon(level, centre(level), player);
        if (boss == null) {
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.rowena.challenge.busy")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
        return boss;
    }

    /** @return Rowena, or null if another fight already holds this world */
    public static @Nullable RowenaEntity summon(ServerLevel level, BlockPos centre, @Nullable ServerPlayer challenger) {
        ArenaController arena = LuciferSummoning.openArena(level, centre, RowenaHouseLayout.ARENA_RADIUS, ArenaTheme.ROWENA_HOUSE);
        if (arena == null) return null;
        RowenaEntity boss = AllEntities.ROWENA.get().create(level);
        if (boss == null) return null;
        for (RowenaNpcEntity npc : level.getEntitiesOfClass(RowenaNpcEntity.class, new net.minecraft.world.phys.AABB(centre).inflate(64))) npc.discard();
        double x = centre.getX() + 0.5, z = centre.getZ() + 0.5;
        Vec3 face = challenger != null ? challenger.position() : Vec3.atCenterOf(centre).add(0, 0, 4);
        float yaw = (float) Math.toDegrees(Math.atan2(-(face.x - x), face.z - z));
        boss.moveTo(x, centre.getY(), z, yaw, 0);
        boss.setYHeadRot(yaw);
        boss.setYBodyRot(yaw);
        boss.bindArena(arena);
        level.addFreshEntity(boss);
        boss.beginEmergence();
        return boss;
    }
}
