package org.papiricoh.supernaturalcraft.entity.boss.michaelvessel;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.nephilim.NephilimSounds;
import org.papiricoh.supernaturalcraft.nephilim.boss.SeasonAttacks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

import java.util.List;

import static org.papiricoh.supernaturalcraft.entity.boss.michaelvessel.MichaelVesselBalance.*;
import static org.papiricoh.supernaturalcraft.nephilim.boss.SeasonAttacks.opt;

/**
 * Michael's attacks in this world (v0.41), numbers in {@link MichaelVesselBalance}: blue his light and wings, violet his
 * choking hand, red where his improved monsters come.
 */
public final class MichaelVesselAttacks {

    private MichaelVesselAttacks() {
    }

    private static SeasonAttacks.Spec spec(String id, String clip, int windup, int recover, ResourceKey<DamageType> type, float damage,
                                           int colour, ParticleOptions p, String sound) {
        return new SeasonAttacks.Spec(id, clip, MichaelVesselAssets.HIT_TICKS, windup, recover, type, damage, colour, p,
                NephilimSounds.get("michael_vessel." + sound));
    }

    static SeasonAttacks.Cone slash() {
        return new SeasonAttacks.Cone(spec("slash", "slash", SLASH_WINDUP, 12, AllDamageTypes.SMITE, SLASH_DAMAGE, TelegraphMarker.BLUE,
                ParticleTypes.SWEEP_ATTACK, "smite"), SLASH_REACH);
    }

    static SeasonAttacks.Lanes smite(int lanes) {
        return new SeasonAttacks.Lanes(spec("smite", "smite", SMITE_WINDUP, 16, AllDamageTypes.GRACE, SMITE_DAMAGE, TelegraphMarker.BLUE,
                ParticleTypes.END_ROD, "smite"), lanes, SMITE_SPREAD, SMITE_LENGTH, SMITE_WIDTH);
    }

    static SeasonAttacks.Ring flare(float radius) {
        return new SeasonAttacks.Ring(spec("flare", "wing_flare", FLARE_WINDUP, 18, AllDamageTypes.GRACE, FLARE_DAMAGE, TelegraphMarker.BLUE,
                AllParticles.GRACE.get(), "flare"), radius, false);
    }

    static SeasonAttacks.Zone choke() {
        return new SeasonAttacks.Zone(spec("choke", "choke", CHOKE_WINDUP, 16, AllDamageTypes.SMITE, CHOKE_DAMAGE, TelegraphMarker.VIOLET,
                ParticleTypes.REVERSE_PORTAL, "choke"), CHOKE_RADIUS, e -> new MobEffectInstance(MobEffects.LEVITATION, CHOKE_LIFT, 0));
    }

    static SeasonAttacks.Blink blink() {
        return new SeasonAttacks.Blink(spec("blink", "teleport", BLINK_WINDUP, 8, AllDamageTypes.SMITE, 0, TelegraphMarker.BLUE,
                ParticleTypes.END_ROD, "flare"));
    }

    /** The snap: a vampire or a werewolf, made "better" (stronger, faster, harder to kill) for a while. */
    static SeasonAttacks.Summon snap(int perSnap) {
        return new SeasonAttacks.Summon(spec("snap", "snap", SNAP_WINDUP, 14, AllDamageTypes.SMITE, 0, TelegraphMarker.RED,
                ParticleTypes.END_ROD, "snap"), () -> Math.random() < 0.5 ? AllEntities.VAMPIRE.get() : AllEntities.WEREWOLF.get(), perSnap, MAX_MONSTERS) {
            @Override
            public void onActive(LuciferEntity boss, LivingEntity target) {
                int before = boss.minions().size();
                super.onActive(boss, target);
                List<java.util.UUID> ids = boss.minions();
                for (int i = before; i < ids.size(); i++) {
                    if (((net.minecraft.server.level.ServerLevel) boss.level()).getEntity(ids.get(i)) instanceof Mob m) improve(m);
                }
            }
        };
    }

    /** A monster Michael has made better. */
    public static void improve(Mob m) {
        int t = IMPROVED_SECONDS * 20;
        m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, t, IMPROVED_AMPLIFIER));
        m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, t, IMPROVED_AMPLIFIER));
        m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, t, IMPROVED_AMPLIFIER));
        m.addEffect(new MobEffectInstance(MobEffects.GLOWING, t, 0));
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(opt(MichaelVesselAttacks::slash, 3),
            opt(() -> smite(1), 2.5f), opt(() -> snap(MONSTERS_PER_SNAP), 1.2f), opt(MichaelVesselAttacks::blink, 1));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(opt(MichaelVesselAttacks::slash, 2),
            opt(() -> smite(1), 2), opt(() -> flare(FLARE_RADIUS), 2.5f), opt(MichaelVesselAttacks::choke, 2),
            opt(() -> snap(MONSTERS_PER_SNAP), 1), opt(MichaelVesselAttacks::blink, 1.2f));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(opt(MichaelVesselAttacks::slash, 1.5f),
            opt(() -> smite(3), 3), opt(() -> flare(FLARE_RADIUS_LAST), 2.5f), opt(MichaelVesselAttacks::choke, 2),
            opt(() -> snap(MONSTERS_PER_SNAP_LAST), 1.2f), opt(MichaelVesselAttacks::blink, 1.2f));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            default -> P3;
        };
    }
}
