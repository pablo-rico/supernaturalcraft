package org.papiricoh.supernaturalcraft.bowl.witch;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlCast;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

import java.util.function.Consumer;

/**
 * How a curse from the bowl lands (v0.21), the same for every curse and control spell: a boss or a player the PvP rules protect
 * is refused; a {@code NAME_WARDED} target turns it away (the curse fails, its protection flares and the caster takes
 * {@link #BACKLASH}); a {@code MIRRORED} target sends it back (the mirror breaks and the caster gets the curse instead, unless
 * they are warded themselves); otherwise it takes hold. Each outcome shows a HEX_PULSE.
 */
public final class Maledictions {

    /** What a warded target's protection does to the one who cursed them. */
    public static final float BACKLASH = 3f;
    public static final int FX_TICKS = 30;

    private Maledictions() {
    }

    public enum Outcome {
        CURSED, WARDED, MIRRORED, REFUSED;

        /** Whether the spell went off (the bowl is spent): all but a refusal. */
        public boolean spent() {
            return this != REFUSED;
        }
    }

    /** Lays {@code curse} on {@code victim} from {@code cast}, keeping the rules above. */
    public static Outcome strike(BowlCast cast, LivingEntity victim, Consumer<LivingEntity> curse) {
        ServerPlayer caster = cast.caster();
        if (!NameMagic.mayAfflict(caster, victim)) {
            NameMagic.refuse(cast, victim);
            return Outcome.REFUSED;
        }
        cast.sendTo(NameMagic.chest(victim), victim);
        if (victim != caster && victim.hasEffect(AllMobEffects.NAME_WARDED)) {
            protect(victim);
            caster.hurt(AllDamageTypes.source(cast.level(), AllDamageTypes.SPELL, null), BACKLASH);
            tell(caster, "message.supernaturalcraft.witchcraft.warded", victim);
            return Outcome.WARDED;
        }
        if (victim != caster && victim.hasEffect(AllMobEffects.MIRRORED)) {
            victim.removeEffect(AllMobEffects.MIRRORED);
            protect(victim);
            tell(caster, "message.supernaturalcraft.witchcraft.mirrored", victim);
            if (caster.hasEffect(AllMobEffects.NAME_WARDED)) {
                protect(caster);
            } else {
                curse.accept(caster);
                hexed(caster);
            }
            return Outcome.MIRRORED;
        }
        curse.accept(victim);
        hexed(victim);
        return Outcome.CURSED;
    }

    private static void protect(LivingEntity e) {
        if (e.level() instanceof net.minecraft.server.level.ServerLevel l) {
            MagicFx.hex(l, e, e.position().add(0, e.getBbHeight() / 2, 0), MagicFxPayload.HEX_PROTECT, 1.2f, FX_TICKS);
        }
    }

    private static void hexed(LivingEntity e) {
        if (e.level() instanceof net.minecraft.server.level.ServerLevel l) {
            MagicFx.hex(l, e, e.position().add(0, e.getBbHeight() / 2, 0), MagicFxPayload.HEX_CURSE, 1.2f, FX_TICKS);
        }
    }

    private static void tell(ServerPlayer caster, String key, LivingEntity victim) {
        caster.displayClientMessage(Component.translatable(key, victim.getName()).withStyle(ChatFormatting.DARK_PURPLE), false);
    }
}
