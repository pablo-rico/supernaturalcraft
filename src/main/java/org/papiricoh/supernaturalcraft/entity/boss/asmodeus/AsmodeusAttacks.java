package org.papiricoh.supernaturalcraft.entity.boss.asmodeus;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.nephilim.NephilimSounds;
import org.papiricoh.supernaturalcraft.nephilim.boss.SeasonAttacks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

import java.util.List;

import static org.papiricoh.supernaturalcraft.entity.boss.asmodeus.AsmodeusBalance.*;
import static org.papiricoh.supernaturalcraft.nephilim.boss.SeasonAttacks.opt;

/** Asmodeus's attacks (v0.41), numbers in {@link AsmodeusBalance}: red his cane and hounds, violet his borrowed face, gold the grace. */
public final class AsmodeusAttacks {

    private AsmodeusAttacks() {
    }

    private static SeasonAttacks.Spec spec(String id, String clip, int windup, int recover, ResourceKey<DamageType> type,
                                           float damage, int colour, ParticleOptions p, String sound) {
        return new SeasonAttacks.Spec(id, clip, AsmodeusAssets.HIT_TICKS, windup, recover, type, damage, colour, p,
                NephilimSounds.get("asmodeus." + sound));
    }

    static SeasonAttacks.Cone cane() {
        return new SeasonAttacks.Cone(spec("cane", "cane_strike", CANE_WINDUP, 14, AllDamageTypes.HELLFIRE, CANE_DAMAGE, TelegraphMarker.RED,
                ParticleTypes.CRIT, "cane"), CANE_REACH);
    }

    static SeasonAttacks.Ring slam(float radius, float damage, int colour) {
        return new SeasonAttacks.Ring(spec("slam", "cane_slam", colour == TelegraphMarker.GOLD ? NOVA_WINDUP : SLAM_WINDUP, 18,
                colour == TelegraphMarker.GOLD ? AllDamageTypes.GRACE : AllDamageTypes.HELLFIRE, damage, colour,
                colour == TelegraphMarker.GOLD ? AllParticles.GRACE.get() : ParticleTypes.FLAME, "slam"), radius, false);
    }

    static SeasonAttacks.Summon hounds(int cap) {
        return new SeasonAttacks.Summon(spec("hounds", "summon", SUMMON_WINDUP, 16, AllDamageTypes.HELLFIRE, 0, TelegraphMarker.RED,
                ParticleTypes.LARGE_SMOKE, "summon"), () -> AllEntities.HELLHOUND.get(), HOUNDS_PER_CALL, cap);
    }

    static SeasonAttacks.Blink face() {
        return new SeasonAttacks.Blink(spec("face", "shapeshift", BLINK_WINDUP, 10, AllDamageTypes.HELLFIRE, 0, TelegraphMarker.VIOLET,
                ParticleTypes.WITCH, "summon"));
    }

    static SeasonAttacks.Lanes grace(int count) {
        return new SeasonAttacks.Lanes(spec("grace", "grace_blast", GRACE_WINDUP, 16, AllDamageTypes.GRACE, GRACE_DAMAGE, TelegraphMarker.GOLD,
                AllParticles.GRACE.get(), "grace"), count, GRACE_SPREAD, GRACE_LENGTH, GRACE_WIDTH);
    }

    static SeasonAttacks.Ring smite() {
        return new SeasonAttacks.Ring(spec("smite", "grace_blast", SMITE_WINDUP, 14, AllDamageTypes.GRACE, SMITE_DAMAGE, TelegraphMarker.GOLD,
                AllParticles.GRACE.get(), "grace"), SMITE_RADIUS, true);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(opt(AsmodeusAttacks::cane, 3),
            opt(() -> slam(SLAM_RADIUS, SLAM_DAMAGE, TelegraphMarker.RED), 2), opt(() -> hounds(MAX_HOUNDS), 1.2f), opt(AsmodeusAttacks::face, 1));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(opt(AsmodeusAttacks::cane, 2),
            opt(() -> grace(1), 3), opt(AsmodeusAttacks::smite, 2.5f), opt(() -> hounds(MAX_HOUNDS), 1), opt(AsmodeusAttacks::face, 1));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(opt(AsmodeusAttacks::cane, 1.5f),
            opt(() -> grace(3), 3), opt(AsmodeusAttacks::smite, 2), opt(() -> slam(NOVA_RADIUS, NOVA_DAMAGE, TelegraphMarker.GOLD), 2),
            opt(() -> hounds(MAX_HOUNDS_LAST), 1.2f), opt(AsmodeusAttacks::face, 1));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            default -> P3;
        };
    }
}
