package org.papiricoh.supernaturalcraft.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Finding a player by id (v0.19): on the server through the level's entity index, not a walk over every player. */
public final class Players {

    private Players() {
    }

    /** The player with this id in {@code level}, or null (the same answer as {@link Level#getPlayerByUUID}, in constant time). */
    public static @Nullable Player byUuid(Level level, @Nullable UUID id) {
        if (id == null) return null;
        if (level instanceof ServerLevel server) return server.getEntity(id) instanceof Player p ? p : null;
        return level.getPlayerByUUID(id);
    }
}
