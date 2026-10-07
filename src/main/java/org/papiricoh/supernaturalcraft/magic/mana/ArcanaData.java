package org.papiricoh.supernaturalcraft.magic.mana;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A player's magic: current mana, the sigils they have learned, and what has permanently changed
 * them. Mutable and stored as a data attachment that survives death.
 */
public class ArcanaData {

    public static final float BASE_MAX_MANA = 100f;
    public static final float GRACE_BONUS = 50f;
    public static final float VOID_MARK_BONUS = 25f;
    public static final float MAX_SANITY = 100f;

    public static final Codec<ArcanaData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("mana", BASE_MAX_MANA).forGetter(d -> d.mana),
            ResourceLocation.CODEC.listOf().optionalFieldOf("known", List.of()).forGetter(d -> List.copyOf(d.known)),
            Codec.BOOL.optionalFieldOf("grace", false).forGetter(d -> d.grace),
            Codec.LONG.optionalFieldOf("cooldown_until", 0L).forGetter(d -> d.cooldownUntil),
            Codec.FLOAT.optionalFieldOf("sanity", MAX_SANITY).forGetter(d -> d.sanity),
            Codec.BOOL.optionalFieldOf("void_mark", false).forGetter(d -> d.voidMark)
    ).apply(i, ArcanaData::new));

    private float mana;
    private final Set<ResourceLocation> known;
    private boolean grace;
    private long cooldownUntil;
    private float sanity;
    private boolean voidMark;

    /** Transient: set whenever something the client shows has changed. */
    public boolean dirty = true;

    public ArcanaData() {
        this(BASE_MAX_MANA, List.of(), false, 0L, MAX_SANITY, false);
    }

    private ArcanaData(float mana, List<ResourceLocation> known, boolean grace, long cooldownUntil, float sanity, boolean voidMark) {
        this.mana = mana;
        this.known = new HashSet<>(known);
        this.grace = grace;
        this.cooldownUntil = cooldownUntil;
        this.sanity = sanity;
        this.voidMark = voidMark;
    }

    public float mana() {
        return mana;
    }

    public float maxMana() {
        return BASE_MAX_MANA + (grace ? GRACE_BONUS : 0) + (voidMark ? VOID_MARK_BONUS : 0);
    }

    public void setMana(float value) {
        float clamped = Math.max(0, Math.min(maxMana(), value));
        if ((int) clamped != (int) mana) dirty = true;
        mana = clamped;
    }

    public boolean knows(ResourceLocation sigil) {
        return known.contains(sigil);
    }

    public Set<ResourceLocation> known() {
        return Set.copyOf(known);
    }

    /** @return false if it was already known */
    public boolean learn(ResourceLocation sigil) {
        boolean added = known.add(sigil);
        dirty |= added;
        return added;
    }

    public void forgetAll() {
        known.clear();
        dirty = true;
    }

    public boolean hasGrace() {
        return grace;
    }

    public void setGrace(boolean value) {
        grace = value;
        dirty = true;
    }

    /** Sanity, spent by the Whispering Codex; low sanity brings whispers and misfires. */
    public float sanity() {
        return sanity;
    }

    public void setSanity(float value) {
        float clamped = Math.max(0, Math.min(MAX_SANITY, value));
        if ((int) clamped != (int) sanity) dirty = true;
        sanity = clamped;
    }

    /** Amara's permanent mark: sight in the eclipse, resistance to darkness, deeper mana. */
    public boolean hasVoidMark() {
        return voidMark;
    }

    public void setVoidMark(boolean value) {
        voidMark = value;
        dirty = true;
    }

    public long cooldownUntil() {
        return cooldownUntil;
    }

    public void setCooldownUntil(long gameTime) {
        cooldownUntil = gameTime;
        dirty = true;
    }
}
