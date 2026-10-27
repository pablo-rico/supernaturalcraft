package org.papiricoh.supernaturalcraft.chain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.player.Player;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;

import java.util.HashMap;
import java.util.Map;

/**
 * Immutable and copied on death: dying is precisely what earns a mark. One {@link Heat} per faction id; saves from before v0.28
 * kept the demons' and the angels' in fields of their own, still read.
 */
public record ChainMark(Map<String, Heat> marks) {
    public record Heat(int heat, long nextAmbushDay) {
        static final Codec<Heat> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("heat").forGetter(Heat::heat),
                Codec.LONG.fieldOf("day").forGetter(Heat::nextAmbushDay)).apply(i, Heat::new));
    }

    public static final ChainMark EMPTY = new ChainMark(Map.of());
    public static final Codec<ChainMark> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Heat.CODEC).optionalFieldOf("marks", Map.of()).forGetter(ChainMark::marks),
            Codec.INT.optionalFieldOf("demon_heat", 0).forGetter(m -> 0),
            Codec.LONG.optionalFieldOf("demon_day", 0L).forGetter(m -> 0L),
            Codec.INT.optionalFieldOf("angel_heat", 0).forGetter(m -> 0),
            Codec.LONG.optionalFieldOf("angel_day", 0L).forGetter(m -> 0L)).apply(i, ChainMark::read));

    public ChainMark {
        marks = Map.copyOf(marks);
    }

    private static ChainMark read(Map<String, Heat> marks, int demonHeat, long demonDay, int angelHeat, long angelDay) {
        Map<String, Heat> out = new HashMap<>();
        if (demonHeat != 0 || demonDay != 0) out.put(ChainFaction.DEMON.id(), new Heat(demonHeat, demonDay));
        if (angelHeat != 0 || angelDay != 0) out.put(ChainFaction.ANGEL.id(), new Heat(angelHeat, angelDay));
        out.putAll(marks);
        return new ChainMark(out);
    }

    public static ChainMark get(Player p) { return p.getData(AllAttachments.CHAIN_MARK); }

    public int heat(ChainFaction f) { return marks.getOrDefault(f.id(), new Heat(0, 0)).heat(); }

    public long nextAmbushDay(ChainFaction f) { return marks.getOrDefault(f.id(), new Heat(0, 0)).nextAmbushDay(); }

    /** The heat every chain bears this hunter, together. */
    public int total() { return marks.values().stream().mapToInt(Heat::heat).sum(); }

    public ChainMark with(ChainFaction f, int heat, long day) {
        Map<String, Heat> out = new HashMap<>(marks);
        out.put(f.id(), new Heat(Math.max(0, heat), day));
        return new ChainMark(out);
    }

    public static void set(Player p, ChainFaction f, int heat, long day) { p.setData(AllAttachments.CHAIN_MARK, get(p).with(f, heat, day)); }
}
