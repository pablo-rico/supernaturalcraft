package org.papiricoh.supernaturalcraft.entity.boss.soulless;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.nephilim.NephilimSounds;
import org.papiricoh.supernaturalcraft.nephilim.boss.SeasonAttacks;
import org.papiricoh.supernaturalcraft.nephilim.jack.JackAssets;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

import java.util.List;

import static org.papiricoh.supernaturalcraft.entity.boss.soulless.SoullessJackBalance.*;
import static org.papiricoh.supernaturalcraft.nephilim.boss.SeasonAttacks.opt;

/** Soulless Jack's attacks (v0.41), numbers in {@link SoullessJackBalance}: gold his grace, violet what he unmakes and throws. */
public final class SoullessJackAttacks {

    private SoullessJackAttacks() {
    }

    private static SeasonAttacks.Spec spec(String id, String clip, int windup, int recover, ResourceKey<DamageType> type, float damage,
                                           int colour, ParticleOptions p, String sound) {
        return new SeasonAttacks.Spec(id, clip, JackAssets.HIT_TICKS, windup, recover, type, damage, colour, p, NephilimSounds.get("jack." + sound));
    }

    static SeasonAttacks.Lanes blast(int lanes, int windup) {
        return new SeasonAttacks.Lanes(spec("blast", "smite", windup, 14, AllDamageTypes.GRACE, BLAST_DAMAGE, TelegraphMarker.GOLD,
                AllParticles.GRACE.get(), "smite"), lanes, BLAST_SPREAD, BLAST_LENGTH, BLAST_WIDTH);
    }

    /** The wave of his hand: a violet cone, and whoever is in it is thrown back. */
    static SeasonAttacks.Cone wave() {
        return new SeasonAttacks.Cone(spec("wave", "wave_aside", WAVE_WINDUP, 12, AllDamageTypes.SMITE, WAVE_DAMAGE, TelegraphMarker.VIOLET,
                ParticleTypes.REVERSE_PORTAL, "shield"), WAVE_REACH) {
            @Override
            public void onActive(LuciferEntity boss, LivingEntity target) {
                super.onActive(boss, target);
                for (LivingEntity e : SeasonAttacks.inRing(boss, boss.position(), WAVE_REACH)) {
                    Vec3 away = e.position().subtract(boss.position()).multiply(1, 0, 1).normalize().scale(WAVE_PUSH);
                    e.setDeltaMovement(away.x, 0.45, away.z);
                    e.hurtMarked = true;
                }
            }
        };
    }

    static SeasonAttacks.Zone snap(float radius) {
        return new SeasonAttacks.Zone(spec("snap", "snap", SNAP_WINDUP, 14, AllDamageTypes.ERASED, SNAP_DAMAGE, TelegraphMarker.VIOLET,
                ParticleTypes.SQUID_INK, "snap"), radius, e -> new MobEffectInstance(MobEffects.WITHER, SNAP_WITHER, 1));
    }

    static SeasonAttacks.Ring nova(float radius) {
        return new SeasonAttacks.Ring(spec("nova", "nova", NOVA_WINDUP, 22, AllDamageTypes.GRACE, NOVA_DAMAGE, TelegraphMarker.GOLD,
                AllParticles.GRACE.get(), "nova"), radius, false);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(opt(() -> blast(1, BLAST_WINDUP), 3),
            opt(SoullessJackAttacks::wave, 2), opt(() -> snap(SNAP_RADIUS), 2));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(opt(() -> blast(3, BLAST_WINDUP), 3),
            opt(SoullessJackAttacks::wave, 1.5f), opt(() -> snap(SNAP_RADIUS_WIDE), 2), opt(() -> nova(NOVA_RADIUS), 1.5f));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(opt(() -> blast(3, BLAST_WINDUP_LAST), 3),
            opt(() -> snap(SNAP_RADIUS_WIDE), 2), opt(() -> nova(NOVA_RADIUS_LAST), 2));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            default -> P3;
        };
    }
}
