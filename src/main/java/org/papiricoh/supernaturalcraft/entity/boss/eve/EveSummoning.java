package org.papiricoh.supernaturalcraft.entity.boss.eve;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.purgatory.PurgatoryWorld;
import org.papiricoh.supernaturalcraft.purgatory.glade.EveGladePlan;
import org.papiricoh.supernaturalcraft.purgatory.glade.EveGladeWorld;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/**
 * How Eve's fight begins (v0.39).
 * <ul>
 *   <li><b>In her glade</b> ({@link #tick}, {@link #begin}): a hunter she has not yet fallen to walks into the clearing; the roots
 *   close round it ({@link ArenaTheme#GLADE}) and she rises before her throne.</li>
 *   <li><b>A rematch</b> ({@link #rite}), by the rite {@code summon_eve} at her throne, once she has fallen to the ritualist.</li>
 *   <li><b>Anywhere</b> ({@link #summon}): commands and tests, a plain arena round the spot.</li>
 * </ul>
 */
public final class EveSummoning {

    /** The arena's radius round a free summoning. */
    public static final int FREE_RADIUS = 22;
    /** How far into the clearing a hunter walks before she wakes. */
    public static final double WAKE = 15;
    /** How near the throne the rematch rite must be held. */
    public static final double RITE_REACH = 12;

    private EveSummoning() {
    }

    /** Whether an Eve fight is on in {@code level}. */
    public static boolean fighting(ServerLevel level) {
        for (ArenaController a : ArenaSavedData.get(level).all()) if (a.isActive() && a.theme() == ArenaTheme.GLADE) return true;
        return false;
    }

    /** Once a second in Purgatory: a hunter she has not fallen to, in her glade, wakes her. */
    public static void tick(ServerLevel level) {
        if (!EveGladeWorld.built(level) || fighting(level)) return;
        Vec3 c = EveGladeWorld.floor(level, EveGladePlan.CENTRE);
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || p.isCreative() || p.position().distanceToSqr(c) > WAKE * WAKE) continue;
            if (PurgatoryWorld.has(p, BossProgression.Boss.EVE.advancement)) continue;
            begin(level, p, false);
            return;
        }
    }

    /** The fight in her glade: {@code hunter} walked in, or called her back ({@code rematch}). @return Eve, or null */
    public static @Nullable EveEntity begin(ServerLevel level, @Nullable ServerPlayer hunter, boolean rematch) {
        Component busy = LuciferSummoning.refusal(level);
        if (busy != null) {
            if (hunter != null) hunter.displayClientMessage(busy, true);
            return null;
        }
        BlockPos centre = EveGladeWorld.point(level, EveGladePlan.CENTRE);
        ArenaController arena = LuciferSummoning.openArena(level, centre, SNConfig.EVE_ARENA_RADIUS.get(), ArenaTheme.GLADE);
        if (arena == null) return null;
        EveEntity boss = AllEntities.EVE.get().create(level);
        if (boss == null) return null;
        Vec3 at = EveGladeWorld.floor(level, EveGladePlan.THRONE);
        Vec3 face = hunter != null ? hunter.position() : Vec3.atBottomCenterOf(centre);
        float yaw = (float) Math.toDegrees(Math.atan2(-(face.x - at.x), face.z - at.z));
        boss.moveTo(at.x, at.y, at.z, yaw, 0);
        boss.setYHeadRot(yaw);
        boss.setYBodyRot(yaw);
        boss.fightInGlade();
        boss.bindArena(arena);
        level.addFreshEntity(boss);
        boss.beginEmergence();
        if (hunter != null && !rematch) {
            hunter.sendSystemMessage(Component.translatable("message.supernaturalcraft.eve.wakes").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
        }
        return boss;
    }

    /** The rematch rite, at her throne. @return whether she answers */
    public static boolean rite(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        Component why = riteRefusal(level, altar);
        if (why != null) {
            if (ritualist != null) ritualist.displayClientMessage(why.copy().withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        return begin(level, ritualist, true) != null;
    }

    /** Why the rite fails here and now, or null: not in Purgatory by her throne, the glade not raised, or a fight already on. */
    public static @Nullable Component riteRefusal(ServerLevel level, BlockPos altar) {
        if (!EveGladeWorld.built(level)) return Component.translatable("message.supernaturalcraft.eve.rite.elsewhere");
        BlockPos throne = EveGladeWorld.point(level, EveGladePlan.THRONE);
        if (throne.distSqr(altar) > RITE_REACH * RITE_REACH) return Component.translatable("message.supernaturalcraft.eve.rite.elsewhere");
        if (fighting(level)) return Component.translatable("message.supernaturalcraft.eve.busy");
        return LuciferSummoning.refusal(level);
    }

    /** Anywhere (commands, tests): a plain arena round {@code at}. */
    public static @Nullable EveEntity summon(ServerLevel level, BlockPos at, @Nullable ServerPlayer facing) {
        ArenaController arena = LuciferSummoning.openArena(level, at, FREE_RADIUS, ArenaTheme.GLADE);
        if (arena == null) {
            if (facing != null) {
                Component why = LuciferSummoning.refusal(level);
                facing.displayClientMessage(why != null ? why : Component.translatable("message.supernaturalcraft.eve.busy")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return null;
        }
        EveEntity boss = AllEntities.EVE.get().create(level);
        if (boss == null) return null;
        boss.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 4.5, 180, 0);
        boss.bindArena(arena);
        level.addFreshEntity(boss);
        boss.beginEmergence();
        return boss;
    }
}
