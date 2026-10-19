package org.papiricoh.supernaturalcraft.mark;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.Optional;
import java.util.UUID;

/**
 * Whether a player bears the Mark of Cain and how thirsty it is (v0.21; the {@code MARK} attachment, kept through death). Pure.
 *
 * @param marked    bears the Mark
 * @param thirst    the Mark's Thirst, 0-1 ({@link MarkRules})
 * @param lastKill  game time of the bearer's last kill (the grace counts from it)
 * @param grantedBy who gave it (Cain's entity UUID, or empty: a Knight of Hell's own, or a command)
 * @param everRemoved the Mark was once taken off (P6, {@code unmark}): taken again, it comes back already thirsty
 *                    ({@link MarkRules#RETAKEN_THIRST}, "it remembers the way")
 */
public record MarkData(boolean marked, float thirst, long lastKill, Optional<UUID> grantedBy, boolean everRemoved) {

    public static final MarkData NONE = new MarkData(false, 0f, 0L, Optional.empty(), false);

    public static final Codec<MarkData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("marked", false).forGetter(MarkData::marked),
            Codec.FLOAT.optionalFieldOf("thirst", 0f).forGetter(MarkData::thirst),
            Codec.LONG.optionalFieldOf("last_kill", 0L).forGetter(MarkData::lastKill),
            UUIDUtil.CODEC.optionalFieldOf("granted_by").forGetter(MarkData::grantedBy),
            Codec.BOOL.optionalFieldOf("ever_removed", false).forGetter(MarkData::everRemoved)
    ).apply(i, MarkData::new));

    public MarkData {
        thirst = MarkRules.clamp(thirst);
        if (grantedBy == null) grantedBy = Optional.empty();
    }

    /** One that was never taken off. */
    public MarkData(boolean marked, float thirst, long lastKill, Optional<UUID> grantedBy) {
        this(marked, thirst, lastKill, grantedBy, false);
    }

    /** Freshly marked at game time {@code now}: no thirst, the grace starting. */
    public static MarkData granted(long now, Optional<UUID> by) {
        return new MarkData(true, 0f, now, by, false);
    }

    /**
     * Marked again at game time {@code now} after {@code before}: the first time with no thirst; if the Mark was once taken
     * off ({@link #everRemoved}), with {@link MarkRules#RETAKEN_THIRST} already ("it remembers the way").
     */
    public static MarkData regranted(MarkData before, long now, Optional<UUID> by) {
        return new MarkData(true, MarkRules.retakenThirst(before.everRemoved()), now, by, before.everRemoved());
    }

    public MarkData withThirst(float t) {
        return new MarkData(marked, t, lastKill, grantedBy, everRemoved);
    }

    public MarkData killedAt(long now, float t) {
        return new MarkData(marked, t, now, grantedBy, everRemoved);
    }

    /**
     * The Mark taken off (P6: Rowena's {@code unmark}): no thirst; what the bearer became (a Knight's rank) stays, and so does
     * the memory of it ({@link #everRemoved}).
     */
    public MarkData removed() {
        return new MarkData(false, 0f, lastKill, Optional.empty(), true);
    }
}
