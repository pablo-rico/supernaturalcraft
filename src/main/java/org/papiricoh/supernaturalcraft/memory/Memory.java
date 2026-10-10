package org.papiricoh.supernaturalcraft.memory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Locale;

/**
 * One moment of a hunter's story (v0.18, pure): what their Heaven's memory lane shows and stages. Contract from the
 * foundations, shared by the memory work (log, hooks, backfill), the scenes and the client (book pages).
 *
 * @param id       dedupe key, unique in a log ({@code boss:michael}, {@code deal:3}, {@code case:5}, {@code rank:angel_2},
 *                 {@code pet:<uuid>}, {@code seen:<entity id>})
 * @param kind     what it is of
 * @param subject  the who or what ({@link MemoryKind} says which, e.g. a boss id or an entity type id)
 * @param detail   the how (an outcome, a wish, a scenario, a name); may be empty
 * @param gameTime the Overworld game time it happened at (0 if only known from a backfill)
 * @param realTime epoch millis it happened at (advancements remember it); 0 if unknown
 * @param variant  a small number the kind gives meaning to (a rank, a count); 0 if none
 */
public record Memory(String id, MemoryKind kind, String subject, String detail, long gameTime, long realTime, int variant) {

    public static final Codec<MemoryKind> KIND_CODEC = Codec.STRING.xmap(s -> MemoryKind.valueOf(s.toUpperCase(Locale.ROOT)),
            k -> k.name().toLowerCase(Locale.ROOT));

    public static final Codec<Memory> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(Memory::id),
            KIND_CODEC.fieldOf("kind").forGetter(Memory::kind),
            Codec.STRING.optionalFieldOf("subject", "").forGetter(Memory::subject),
            Codec.STRING.optionalFieldOf("detail", "").forGetter(Memory::detail),
            Codec.LONG.optionalFieldOf("game_time", 0L).forGetter(Memory::gameTime),
            Codec.LONG.optionalFieldOf("real_time", 0L).forGetter(Memory::realTime),
            Codec.INT.optionalFieldOf("variant", 0).forGetter(Memory::variant)
    ).apply(i, Memory::new));
}
