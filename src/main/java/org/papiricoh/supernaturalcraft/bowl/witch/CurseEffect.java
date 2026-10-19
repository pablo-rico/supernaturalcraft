package org.papiricoh.supernaturalcraft.bowl.witch;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlCast;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlSpellEffect;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

import java.util.Locale;

/**
 * The bowl's curses (v0.21), each aimed by a personal component at anyone loaded anywhere on the server ({@link NameMagic}),
 * landing by {@link Maledictions} (refused on bosses and, without PvP, on other players; turned by a Ward of the Name, sent back
 * by a Mirror Hex). {@code duration} is in ticks.
 * <ul>
 *   <li>{@code misfortune}: Bad Luck and a long jinx (JINXED).</li>
 *   <li>{@code pain}: AGONY, magic harm every two seconds that, like poison, never kills.</li>
 *   <li>{@code frailty}: Weakness, Mining Fatigue, and MARKED (a fifth more harm from everything).</li>
 *   <li>{@code silence}: SILENCED (no sigils, no allegiance powers).</li>
 *   <li>{@code rot}: Hunger and a while of Poison, and ROT (food in the pack turns to rotten flesh).</li>
 * </ul>
 */
public record CurseEffect(Curse curse, int duration) implements BowlSpellEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("curse");

    public enum Curse implements StringRepresentable {
        MISFORTUNE, PAIN, FRAILTY, SILENCE, ROT;

        public static final Codec<Curse> CODEC = StringRepresentable.fromEnum(Curse::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final MapCodec<CurseEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Curse.CODEC.fieldOf("curse").forGetter(CurseEffect::curse),
            Codec.intRange(20, 72000).optionalFieldOf("duration", 1200).forGetter(CurseEffect::duration)
    ).apply(i, CurseEffect::new));

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Nullable
    @Override
    public String precheck(BowlCast cast) {
        return cast.target().isEmpty() ? "message.supernaturalcraft.bowl.needs_target" : null;
    }

    @Override
    public boolean perform(BowlCast cast) {
        NameMagic.Lookup l = NameMagic.find(cast, -1);
        if (!l.ok()) {
            NameMagic.explain(cast, l);
            return false;
        }
        return Maledictions.strike(cast, l.entity(), this::lay).spent();
    }

    /** The curse itself, on {@code victim}. Public for tests. */
    public void lay(LivingEntity victim) {
        int d = duration;
        switch (curse) {
            case MISFORTUNE -> {
                victim.addEffect(new MobEffectInstance(MobEffects.UNLUCK, d, 1));
                victim.addEffect(new MobEffectInstance(AllMobEffects.JINXED, d, 0));
            }
            case PAIN -> victim.addEffect(new MobEffectInstance(AllMobEffects.AGONY, d, 0));
            case FRAILTY -> {
                victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, d, 0));
                victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, d, 1));
                victim.addEffect(new MobEffectInstance(AllMobEffects.MARKED, d, 0));
            }
            case SILENCE -> victim.addEffect(new MobEffectInstance(AllMobEffects.SILENCED, d, 0));
            case ROT -> {
                victim.addEffect(new MobEffectInstance(MobEffects.HUNGER, d, 1));
                victim.addEffect(new MobEffectInstance(MobEffects.POISON, Math.min(d, 400), 0));
                victim.addEffect(new MobEffectInstance(AllMobEffects.ROT, d, 0));
            }
        }
    }
}
