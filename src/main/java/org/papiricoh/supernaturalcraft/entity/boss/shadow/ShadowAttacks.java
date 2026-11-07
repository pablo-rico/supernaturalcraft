package org.papiricoh.supernaturalcraft.entity.boss.shadow;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.nephilim.NephilimSounds;
import org.papiricoh.supernaturalcraft.nephilim.boss.SeasonAttacks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;

import java.util.List;

import static org.papiricoh.supernaturalcraft.entity.boss.shadow.ShadowBalance.*;
import static org.papiricoh.supernaturalcraft.nephilim.boss.SeasonAttacks.opt;

/** The Shadow's attacks (v0.41), numbers in {@link ShadowBalance}: every one of them violet, the colour of the Empty's dark. */
public final class ShadowAttacks {

    private ShadowAttacks() {
    }

    private static final ParticleOptions TAR = ParticleTypes.SQUID_INK;

    private static SeasonAttacks.Spec spec(String id, String clip, int windup, int recover, ResourceKey<DamageType> type, float damage, String sound) {
        return new SeasonAttacks.Spec(id, clip, ShadowAssets.HIT_TICKS, windup, recover, type, damage, TelegraphMarker.VIOLET, TAR,
                NephilimSounds.get("shadow." + sound));
    }

    static SeasonAttacks.Lanes lash(int tendrils) {
        return new SeasonAttacks.Lanes(spec("lash", "lash", LASH_WINDUP, 14, AllDamageTypes.VOID, LASH_DAMAGE, "lash"), tendrils, LASH_SPREAD,
                LASH_LENGTH, LASH_WIDTH);
    }

    static SeasonAttacks.Pull grab() {
        return new SeasonAttacks.Pull(spec("grab", "grab", GRAB_WINDUP, 14, AllDamageTypes.VOID, GRAB_DAMAGE, "lash"), GRAB_LENGTH, GRAB_WIDTH, GRAB_PULL);
    }

    /** Sleep falls on whoever stands in it. */
    static SeasonAttacks.Zone lull(float radius) {
        return new SeasonAttacks.Zone(spec("lull", "lull", LULL_WINDUP, 18, AllDamageTypes.VOID, LULL_DAMAGE, "lull"), radius, ShadowAttacks::drowse) {
            @Override
            public void onActive(LuciferEntity boss, LivingEntity target) {
                super.onActive(boss, target);
            }
        };
    }

    static MobEffectInstance drowse(LivingEntity e) {
        e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, LULL_TICKS, 0));
        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, LULL_TICKS, 1));
        return new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, LULL_TICKS, 2);
    }

    static SeasonAttacks.Cone surge() {
        return new SeasonAttacks.Cone(spec("surge", "engulf", SURGE_WINDUP, 16, AllDamageTypes.VOID, SURGE_DAMAGE, "engulf"), SURGE_REACH);
    }

    static SeasonAttacks.Ring engulf() {
        return new SeasonAttacks.Ring(spec("engulf", "engulf", ENGULF_WINDUP, 22, AllDamageTypes.VOID, ENGULF_DAMAGE, "engulf"), ENGULF_RADIUS, false);
    }

    static SeasonAttacks.Blink sink() {
        return new SeasonAttacks.Blink(spec("sink", "sink", SINK_WINDUP, 12, AllDamageTypes.VOID, 0, "roar"));
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(opt(() -> lash(1), 3), opt(ShadowAttacks::grab, 2),
            opt(() -> lull(LULL_RADIUS), 1.5f));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(opt(() -> lash(2), 2.5f), opt(ShadowAttacks::grab, 1.5f),
            opt(() -> lull(LULL_RADIUS), 1.5f), opt(ShadowAttacks::surge, 2.5f), opt(ShadowAttacks::sink, 1.2f));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(opt(() -> lash(3), 3), opt(ShadowAttacks::grab, 1.5f),
            opt(() -> lull(LULL_RADIUS_LAST), 2), opt(ShadowAttacks::surge, 2), opt(ShadowAttacks::engulf, 2), opt(ShadowAttacks::sink, 1.2f));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            default -> P3;
        };
    }
}
