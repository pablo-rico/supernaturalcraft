package org.papiricoh.supernaturalcraft.chain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.player.Player;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;

/** Immutable and copied on death: dying is precisely what earns a mark. */
public record ChainMark(int demonHeat, long demonNextAmbushDay, int angelHeat, long angelNextAmbushDay) {
    public static final ChainMark EMPTY = new ChainMark(0, 0, 0, 0);
    public static final Codec<ChainMark> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("demon_heat").forGetter(ChainMark::demonHeat),
            Codec.LONG.fieldOf("demon_day").forGetter(ChainMark::demonNextAmbushDay),
            Codec.INT.fieldOf("angel_heat").forGetter(ChainMark::angelHeat),
            Codec.LONG.fieldOf("angel_day").forGetter(ChainMark::angelNextAmbushDay)).apply(i, ChainMark::new));
    public static ChainMark get(Player p) { return p.getData(AllAttachments.CHAIN_MARK); }
    public int heat(ChainFaction f) { return f == ChainFaction.DEMON ? demonHeat : angelHeat; }
    public long nextAmbushDay(ChainFaction f) { return f == ChainFaction.DEMON ? demonNextAmbushDay : angelNextAmbushDay; }
    public ChainMark with(ChainFaction f, int heat, long day) {
        return f == ChainFaction.DEMON ? new ChainMark(Math.max(0, heat), day, angelHeat, angelNextAmbushDay)
                : new ChainMark(demonHeat, demonNextAmbushDay, Math.max(0, heat), day);
    }
    public static void set(Player p, ChainFaction f, int heat, long day) { p.setData(AllAttachments.CHAIN_MARK, get(p).with(f, heat, day)); }
}
