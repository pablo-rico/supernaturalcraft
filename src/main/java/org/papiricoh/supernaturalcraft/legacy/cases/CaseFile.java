package org.papiricoh.supernaturalcraft.legacy.cases;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * A case Henry has handed a hunter (v0.17), rolled by {@code CaseGenerator}: where, what, and the twist.
 *
 * @param index its place in the hunter's sequence (the case file item carries it)
 * @param site where it happens (the case file's map points there)
 * @param scenario village, barn, haunted_house, graveyard, night_woods …
 * @param monster the entity type behind it
 * @param twist named_leader, weakness, hostage … ("" for none)
 * @param tier how hard (1–5, from the hunter's rank)
 * @param issued game time it was handed out
 * @param state {@link #OPEN}, {@link #ACTIVE} (the site is written and its creatures out), {@link #SOLVED} or {@link #LOST}
 */
public record CaseFile(int index, BlockPos site, String scenario, ResourceLocation monster, String twist, int tier, long issued,
                       int state) {

    public static final int OPEN = 0;
    public static final int ACTIVE = 1;
    public static final int SOLVED = 2;
    public static final int LOST = 3;

    public static final Codec<CaseFile> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("index").forGetter(CaseFile::index),
            BlockPos.CODEC.fieldOf("site").forGetter(CaseFile::site),
            Codec.STRING.fieldOf("scenario").forGetter(CaseFile::scenario),
            ResourceLocation.CODEC.fieldOf("monster").forGetter(CaseFile::monster),
            Codec.STRING.optionalFieldOf("twist", "").forGetter(CaseFile::twist),
            Codec.INT.optionalFieldOf("tier", 1).forGetter(CaseFile::tier),
            Codec.LONG.optionalFieldOf("issued", 0L).forGetter(CaseFile::issued),
            Codec.INT.optionalFieldOf("state", OPEN).forGetter(CaseFile::state)
    ).apply(i, CaseFile::new));

    public CaseFile withState(int state) {
        return new CaseFile(index, site, scenario, monster, twist, tier, issued, state);
    }

    public boolean closed() {
        return state == SOLVED || state == LOST;
    }
}
