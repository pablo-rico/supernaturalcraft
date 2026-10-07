package org.papiricoh.supernaturalcraft.entity.magic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellContext;
import org.papiricoh.supernaturalcraft.weapon.catalyst.CatalystTraits;

/**
 * The part of a spell that has to survive inside an entity until it lands: which sigils, and the
 * numbers the modifiers produced at cast time.
 */
public record SpellCarrier(Spell spell, float potency, float durationScale, float area, int color, CatalystTraits traits) {

    public SpellCarrier(Spell spell, float potency, float durationScale, float area, int color) {
        this(spell, potency, durationScale, area, color, CatalystTraits.NONE);
    }

    public static SpellCarrier of(Spell spell, SpellContext ctx) {
        return new SpellCarrier(spell, ctx.potency, ctx.durationScale, ctx.area, ctx.color, ctx.traits);
    }

    public SpellCarrier withPotency(float p) {
        return new SpellCarrier(spell, p, durationScale, area, color, traits);
    }

    public void applyTo(SpellContext ctx) {
        ctx.potency = potency;
        ctx.durationScale = durationScale;
        ctx.area = area;
        ctx.color = color;
        ctx.traits = traits;
    }

    public void save(CompoundTag tag) {
        Spell.CODEC.encodeStart(NbtOps.INSTANCE, spell).result().ifPresent(t -> tag.put("Spell", t));
        tag.putFloat("Potency", potency);
        tag.putFloat("DurationScale", durationScale);
        tag.putFloat("Area", area);
        tag.putInt("Color", color);
        tag.putIntArray("Traits", new int[]{traits.boltSplit(), traits.boltPierce(), Float.floatToIntBits(traits.boltBurstArea()),
                traits.burstLingerTicks(), Float.floatToIntBits(traits.wardScale()), traits.touchChain()});
    }

    public static SpellCarrier load(CompoundTag tag) {
        Spell spell = tag.contains("Spell", Tag.TAG_COMPOUND)
                ? Spell.CODEC.parse(NbtOps.INSTANCE, tag.get("Spell")).result().orElse(Spell.EMPTY)
                : Spell.EMPTY;
        int[] t = tag.getIntArray("Traits");
        CatalystTraits traits = t.length == 6 ? new CatalystTraits(t[0], t[1], Float.intBitsToFloat(t[2]), t[3],
                Float.intBitsToFloat(t[4]), t[5]) : CatalystTraits.NONE;
        return new SpellCarrier(spell, tag.getFloat("Potency"), tag.getFloat("DurationScale"), tag.getFloat("Area"),
                tag.getInt("Color"), traits);
    }
}
