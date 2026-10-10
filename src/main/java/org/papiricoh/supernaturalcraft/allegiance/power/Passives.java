package org.papiricoh.supernaturalcraft.allegiance.power;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceFx;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.EssenceRules;
import org.papiricoh.supernaturalcraft.allegiance.EssenceSources;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.network.AllegianceFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The passives, once a second per player (v0.13); the combat ones (fire immunity's damage, the First Blade, the hunter's
 * edge, dominion) are in {@code AllegianceCombat}, kill regeneration in {@link EssenceSources#onKill}. All of them are off
 * while suppressed (Chuck's arena), which is also when {@code SUPPRESSED} is flagged and
 * {@link AllegianceFxPayload#SUPPRESSED} sent (arg 1 on, 0 off).
 */
public final class Passives {

    /** {@link AllegianceFxPayload#WHISPER} lines (indices agreed with the client's {@code MESSAGES}; never reorder). */
    public static final int WHISPER_BLOODLUST = 0, WHISPER_BLOODLUST_FED = 1, WHISPER_STARVING = 2, WHISPER_PRAYER = 3,
            WHISPER_CONSECRATED = 4, WHISPER_SENSE = 5, WHISPER_MESSENGER_GONE = 6, WHISPER_PACT_REFUSED = 7,
            WHISPER_SWORN_BLADE = 8, WHISPER_TRAPPED_OIL = 9, WHISPER_FED = 10;
    /** Night vision refreshed below this many ticks left. */
    private static final int NIGHT_VISION_TICKS = 300, NIGHT_VISION_REFRESH = 220;
    /** Sense: how many things it marks at most, and how long each mark lasts. */
    private static final int SENSE_LIMIT = 16, SENSE_TICKS = 30;

    private static final Set<UUID> STARVING = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> FEEDING = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> PRAYING = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> HOLY = ConcurrentHashMap.newKeySet();
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
        if (a.isAngel()) angel(p);
        if (PowerRules.passiveActive(a, Power.BLOODLUST, suppressed)) bloodlust(p, a);
    }

    /** Rank II and up of either side need neither food nor sleep. */
    public static boolean beyondNeed(Allegiance a, boolean suppressed) {
        return a.committed() && a.rank() >= 2 && !suppressed;
    }

    private static void angel(ServerPlayer p) {
        float got = EssenceSources.pray(p);
        boolean holy = EssenceSources.onHolyGround(p);
        if (holy && HOLY.add(p.getUUID())) AllegianceFx.toSelf(p, AllegianceFxPayload.WHISPER, WHISPER_CONSECRATED, 0, p.position(), 60);
        if (!holy) HOLY.remove(p.getUUID());
        boolean praying = got >= EssenceRules.PRAY_PER_SECOND * 0.99f;
        if (praying && PRAYING.add(p.getUUID())) AllegianceFx.toSelf(p, AllegianceFxPayload.WHISPER, WHISPER_PRAYER, 0, p.position(), 60);
        if (!praying) PRAYING.remove(p.getUUID());
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
            Allegiances.addEssence(p, -EssenceRules.BLOODLUST_DRAIN_PER_SECOND);
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
        PRAYING.remove(player);
        HOLY.remove(player);
        SENSING.remove(player);
    }
}
