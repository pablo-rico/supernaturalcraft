package org.papiricoh.supernaturalcraft.entity.boss;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.balance.BossDifficulty;

/**
 * The difficulty a boss fight keeps (v0.24): the world's ({@link #current}) until the fight starts, then fixed by
 * {@link #lock} and saved with the boss, so changing the world's difficulty halfway through changes nothing. A boss
 * from an older save, already fighting, counts as Normal.
 */
public final class DifficultyLock {

    private static final String KEY = "Difficulty";
    private @Nullable BossDifficulty locked;

    /** The settings for {@code level} as it is now (Normal if the config turns difficulty scaling off). */
    public static BossDifficulty current(Level level) {
        if (SNConfig.SPEC.isLoaded() && !SNConfig.DIFFICULTY_SCALING.get()) return BossDifficulty.NORMAL;
        return BossDifficulty.of(level.getDifficulty().getId(), level.getLevelData().isHardcore());
    }

    /** The fight's settings once locked, the world's before. */
    public BossDifficulty get(Level level) {
        return locked != null ? locked : current(level);
    }

    /** Fixes the world's current settings for the rest of the fight; in Peaceful, tells {@code challengers} it is story mode. */
    public BossDifficulty lock(Level level, Iterable<? extends Player> challengers) {
        locked = current(level);
        if (locked.peaceful() && !level.isClientSide) {
            for (Player p : challengers) p.displayClientMessage(Component.translatable("message.supernaturalcraft.boss_story_mode"), true);
        }
        return locked;
    }

    /** Test and command hook. */
    public void set(BossDifficulty difficulty) {
        locked = difficulty;
    }

    public void save(CompoundTag tag) {
        if (locked != null) tag.putByte(KEY, (byte) locked.id());
    }

    /** {@code fighting}: whether the saved boss had already started (then a missing key means Normal). */
    public void load(CompoundTag tag, boolean fighting) {
        if (tag.contains(KEY)) locked = BossDifficulty.byId(tag.getByte(KEY));
        else locked = fighting ? BossDifficulty.NORMAL : null;
    }
}
