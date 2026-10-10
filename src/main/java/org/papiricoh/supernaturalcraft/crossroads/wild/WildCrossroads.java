package org.papiricoh.supernaturalcraft.crossroads.wild;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.crossroads.DealTerms;
import org.papiricoh.supernaturalcraft.crossroads.Debts;
import org.papiricoh.supernaturalcraft.entity.demon.CrossroadsDemonEntity;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Burying a crossroads box at a natural crossroads (v0.18): the box goes into the {@code crossroads_soil}, and
 * {@link #BURY_DELAY} ticks later the demon stands on it, with a wild bargain for whoever buried it. Only at night, and once a
 * night per hunter (the night is kept on their deal, {@code CrossroadsDeal.lastWildDay}); a soul already bound gets nothing.
 */
public final class WildCrossroads {

    /** Ticks between the box going in and the demon arriving. */
    public static final int BURY_DELAY = 60;
    /** Who the demon comes for when the hunter who buried the box is gone (a restart): the nearest player this close. */
    private static final double STAND_IN_RANGE = 24;

    /** A box in the ground, waiting for its demon: who buried it (a reference too, for players the level does not list). */
    private record Pending(UUID id, ServerPlayer player) {
    }

    private static final Map<GlobalPos, Pending> PENDING = new ConcurrentHashMap<>();

    private WildCrossroads() {
    }

    /** Why {@code p} cannot bury a box at {@code soil} right now (a translation key), or null if they can. */
    public static @Nullable String cannotBury(ServerPlayer p, ServerLevel level, BlockPos soil) {
        if (!level.getBlockState(soil).is(AllBlocks.CROSSROADS_SOIL.get())) return "message.supernaturalcraft.crossroads.wild.not_here";
        if (Debts.get(p).active()) return "message.supernaturalcraft.crossroads.already_bound";
        if (PENDING.containsKey(GlobalPos.of(level.dimension(), soil))) return "message.supernaturalcraft.crossroads.wild.waiting";
        if (!level.isNight()) return "message.supernaturalcraft.crossroads.wild.day";
        if (!DealTerms.wildAnswers(true, level.getDayTime(), Debts.get(p).lastWildDay())) return "message.supernaturalcraft.crossroads.wild.once";
        return null;
    }

    /**
     * {@code p} buries a box at {@code soil}: the night is spent, the demon is on its way.
     *
     * @return false (with the reason shown to {@code p}) if the crossroads does not answer
     */
    public static boolean bury(ServerPlayer p, ServerLevel level, BlockPos soil) {
        String why = cannotBury(p, level, soil);
        if (why != null) {
            p.displayClientMessage(Component.translatable(why).withStyle(ChatFormatting.DARK_RED), true);
            return false;
        }
        Debts.set(p, Debts.get(p).withLastWildDay(DealTerms.nightIndex(level.getDayTime())));
        PENDING.put(GlobalPos.of(level.dimension(), soil), new Pending(p.getUUID(), p));
        level.scheduleTick(soil, level.getBlockState(soil).getBlock(), BURY_DELAY);
        Vec3 c = Vec3.atCenterOf(soil).add(0, 0.55, 0);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(soil)), c.x, c.y, c.z, 30, 0.3, 0.1, 0.3, 0.1);
        level.sendParticles(ParticleTypes.SMOKE, c.x, c.y, c.z, 12, 0.25, 0.1, 0.25, 0.01);
        level.playSound(null, soil, AllSounds.heaven("crossroads.bury"), SoundSource.PLAYERS, 1f, 1f);
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.crossroads.wild.buried")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        return true;
    }

    /** Whether a box is in the ground at {@code soil}, its demon not yet come. */
    public static boolean pending(ServerLevel level, BlockPos soil) {
        return PENDING.containsKey(GlobalPos.of(level.dimension(), soil));
    }

    /** The soil's scheduled tick: the demon arrives on the crossroads for whoever buried the box. */
    public static @Nullable CrossroadsDemonEntity arrive(ServerLevel level, BlockPos soil) {
        Pending pending = PENDING.remove(GlobalPos.of(level.dimension(), soil));
        Vec3 at = Vec3.atBottomCenterOf(soil.above());
        Player summoner = null;
        if (pending != null) {
            ServerPlayer p = pending.player();
            if (p != null && !p.isRemoved() && p.level() == level) summoner = p;
            else summoner = level.getPlayerByUUID(pending.id());
        }
        if (summoner == null) summoner = level.getNearestPlayer(at.x, at.y, at.z, STAND_IN_RANGE, false);
        // Nobody left to deal with: the crossroads keeps the box.
        if (summoner == null) return null;
        return CrossroadsDemonEntity.summonWild(level, at, summoner);
    }
}
