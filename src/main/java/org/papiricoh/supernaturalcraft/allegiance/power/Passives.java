package org.papiricoh.supernaturalcraft.allegiance.power;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceFx;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.BossTwists;
import org.papiricoh.supernaturalcraft.allegiance.EssenceRules;
import org.papiricoh.supernaturalcraft.allegiance.EssenceSources;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.network.AllegianceFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The passives, once a second per player (v0.13), and the refill of Grace and Corruption ({@link #regen}); the combat ones (fire immunity's damage, the First Blade, the hunter's
 * edge, dominion) are in {@code AllegianceCombat}, kill regeneration in {@link EssenceSources#onKill}. All of them are off
 * while suppressed (Chuck's arena), which is also when {@code SUPPRESSED} is flagged and
 * {@link AllegianceFxPayload#SUPPRESSED} sent (arg 1 on, 0 off).
 */
public final class Passives {

    /** {@link AllegianceFxPayload#WHISPER} lines (indices agreed with the client's {@code MESSAGES}; never reorder). */
    public static final int WHISPER_BLOODLUST = 0, WHISPER_BLOODLUST_FED = 1, WHISPER_STARVING = 2, WHISPER_PRAYER = 3,
            WHISPER_CONSECRATED = 4, WHISPER_SENSE = 5, WHISPER_MESSENGER_GONE = 6, WHISPER_PACT_REFUSED = 7,
            WHISPER_SWORN_BLADE = 8, WHISPER_TRAPPED_OIL = 9, WHISPER_FED = 10, WHISPER_HELLFIRE_BATH = 11,
            WHISPER_BRIMSTONE = 12, WHISPER_PRAYER_FAR = 13, WHISPER_SANCTUARY = 14, WHISPER_FLAMES = 15;
    /** Night vision refreshed below this many ticks left. */
    private static final int NIGHT_VISION_TICKS = 300, NIGHT_VISION_REFRESH = 220;
    /** Sense: how many things it marks at most, and how long each mark lasts. */
    private static final int SENSE_LIMIT = 16, SENSE_TICKS = 30;

    private static final Set<UUID> STARVING = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> FEEDING = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, EssenceRules.Rest> RESTING = new ConcurrentHashMap<>();
    private static final Set<UUID> SENSING = ConcurrentHashMap.newKeySet();

    private Passives() {
    }

    public static void second(ServerPlayer p) {
        Allegiance a = Allegiances.get(p);
        boolean suppressed = PowerCaster.suppressed(p);
        if (Allegiances.flag(p, Allegiances.SUPPRESSED) != suppressed) {
            Allegiances.setFlag(p, Allegiances.SUPPRESSED, suppressed);
            AllegianceFx.toSelf(p, AllegianceFxPayload.SUPPRESSED, suppressed ? 1 : 0, 0, p.position(), 0);
        }
        // Every rank deepens the mana (not suppressed: Chuck takes powers, not what a hunter has become).
        ManaManager.get(p).setAllegianceMana(org.papiricoh.supernaturalcraft.allegiance.Ranks.manaBonus(a.faction(), a.rank())
                + org.papiricoh.supernaturalcraft.memory.MemoryBonuses.extraMana(p));
        if (!a.committed()) {
            if (PowerRules.passiveActive(a, Power.HUNTER_SENSE, suppressed)) sense(p);
            return;
        }
        if (PowerRules.passiveActive(a, Power.BLACK_EYES, suppressed)) {
            MobEffectInstance nv = p.getEffect(MobEffects.NIGHT_VISION);
            if (nv == null || nv.getDuration() < NIGHT_VISION_REFRESH) {
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_TICKS, 0, true, false, false));
            }
        }
        if (beyondNeed(a, suppressed)) {
            // From rank II neither hunger nor sleep.
            p.getFoodData().setFoodLevel(20);
            p.getFoodData().setSaturation(Math.max(p.getFoodData().getSaturationLevel(), 5f));
            p.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        }
        if (PowerRules.passiveActive(a, Power.FIRE_IMMUNITY, suppressed) && p.isOnFire()) p.clearFire();
        boolean starving = PowerRules.passiveActive(a, Power.BLOODLUST, suppressed) && bloodlust(p, a);
        regen(p, Allegiances.get(p), EssenceRules.regenBlocked(suppressed, starving, BossTwists.inDarkness(p)));
    }

    /** Rank II and up of either side need neither food nor sleep. */
    public static boolean beyondNeed(Allegiance a, boolean suppressed) {
        return a.committed() && a.rank() >= 2 && !suppressed;
    }

    /**
     * The refill, each second: a share of the bar by how the player rests ({@link EssenceSources#rest}), scaled by
     * {@code essenceRegenMultiplier}. A whisper when the rest deepens; grace above a praying angel, hellfire round a demon.
     * @return what was added
     */
    public static float regen(ServerPlayer p, Allegiance a, boolean blocked) {
        EssenceRules.Rest rest = EssenceSources.rest(p, a);
        EssenceRules.Rest before = RESTING.put(p.getUUID(), rest);
        if (rest != EssenceRules.Rest.NATURAL && rest != before) {
            AllegianceFx.toSelf(p, AllegianceFxPayload.WHISPER, whisper(a.faction(), rest), 0, p.position(), 60);
        }
        float amount = (float) (EssenceRules.regenPerSecond(a.faction(), a.maxEssence(), rest, blocked) * SNConfig.ESSENCE_REGEN_MULTIPLIER.get());
        if (amount <= 0 || a.essence() >= a.maxEssence()) return 0;
        Allegiances.addEssence(p, amount);
        if (rest.factor > EssenceRules.Rest.SANCTUARY.factor && p.serverLevel().getGameTime() % 100 < 20) {
            if (a.isAngel()) {
                p.serverLevel().sendParticles(AllParticles.GRACE.get(), p.getX(), p.getY() + 1.8, p.getZ(), 4, 0.2, 0.1, 0.2, 0.01);
            } else {
                p.serverLevel().sendParticles(AllParticles.HELLFIRE.get(), p.getX(), p.getY() + 0.6, p.getZ(), 6, 0.3, 0.4, 0.3, 0.01);
                p.serverLevel().sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1.4, p.getZ(), 2, 0.2, 0.2, 0.2, 0.01);
            }
        }
        return amount;
    }

    private static int whisper(Faction side, EssenceRules.Rest rest) {
        boolean angel = side == Faction.ANGEL;
        return switch (rest) {
            case SANCTUM -> angel ? WHISPER_PRAYER : WHISPER_HELLFIRE_BATH;
            case DEVOTION -> angel ? WHISPER_PRAYER_FAR : WHISPER_BRIMSTONE;
            default -> angel ? WHISPER_SANCTUARY : WHISPER_FLAMES;
        };
    }

    /** A Knight of Hell starved of a kill: Corruption drains, then health. @return whether it bit */
    public static boolean bloodlust(ServerPlayer p, Allegiance a) {
        int s = EssenceSources.secondsSinceKill(p);
        if (!EssenceRules.starving(Faction.DEMON, a.rank(), s)) {
            STARVING.remove(p.getUUID());
            FEEDING.remove(p.getUUID());
            return false;
        }
        if (STARVING.add(p.getUUID())) AllegianceFx.toSelf(p, AllegianceFxPayload.WHISPER, WHISPER_BLOODLUST, 0, p.position(), 80);
        if (a.essence() > 0) {
            Allegiances.addEssence(p, -EssenceRules.share(EssenceRules.BLOODLUST_DRAIN_SHARE, a.maxEssence()));
        } else if (s % EssenceRules.BLOODLUST_HURT_INTERVAL == 0) {
            p.hurt(p.damageSources().starve(), EssenceRules.BLOODLUST_HURT);
            // The first bite on the body: "the Mark is feeding on you"; after that, "starving".
            AllegianceFx.toSelf(p, AllegianceFxPayload.WHISPER, FEEDING.add(p.getUUID()) ? WHISPER_BLOODLUST_FED : WHISPER_STARVING, 0, p.position(), 60);
        }
        return true;
    }

    /** A Veteran's sense: the supernatural within range are marked ({@link AllegianceFxPayload#RADIO_PING}, arg 2). */
    public static int sense(ServerPlayer p) {
        int range = Power.HUNTER_SENSE.range;
        List<LivingEntity> near = p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(range),
                e -> e != p && e.isAlive() && e.distanceTo(p) <= range
                        && (e.getType().is(AllTags.Entities.SUPERNATURAL) || Kin.isDemon(e) || Kin.isAngel(e)));
        near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        int n = Math.min(SENSE_LIMIT, near.size());
        for (int i = 0; i < n; i++) {
            LivingEntity e = near.get(i);
            AllegianceFx.toSelf(p, AllegianceFxPayload.RADIO_PING, 2, e.getId(), e.position(), SENSE_TICKS);
        }
        if (n > 0 && SENSING.add(p.getUUID())) AllegianceFx.toSelf(p, AllegianceFxPayload.WHISPER, WHISPER_SENSE, 0, p.position(), 60);
        if (n == 0) SENSING.remove(p.getUUID());
        return n;
    }

    public static void forget(UUID player) {
        STARVING.remove(player);
        FEEDING.remove(player);
        RESTING.remove(player);
        SENSING.remove(player);
    }
}
