package org.papiricoh.supernaturalcraft.allegiance;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.allegiance.power.Passives;
import org.papiricoh.supernaturalcraft.allegiance.power.Power;
import org.papiricoh.supernaturalcraft.allegiance.power.PowerCaster;
import org.papiricoh.supernaturalcraft.allegiance.power.PowerRules;
import org.papiricoh.supernaturalcraft.entity.allegiance.RivalHunterEntity;
import org.papiricoh.supernaturalcraft.hell.HellDimension;
import org.papiricoh.supernaturalcraft.network.AllegianceFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where Grace and Corruption come from (shares of the bar in {@link EssenceRules}): kills of the other side (players only
 * with PvP) and villagers' pacts for demons, scaled by {@code essenceGainMultiplier}; and how a player is resting for the
 * refill ({@link #rest}: prayer and consecrated ground for angels, hellfire and the Nether or Hell for demons). In Amara's
 * darkness a demon gains nothing. Also keeps the time of each player's last kill (bloodlust) and heals a Knight of Hell who
 * kills.
 */
public final class EssenceSources {

    /** Health a Knight of Hell (and up) gets back from each kill. */
    public static final float KILL_REGEN = 4f;
    /** Persistent-data key on a villager: who it made a pact with. */
    public static final String PACT_TAG = "supernaturalcraft_pact";
    /** Each villager's price drop for an angel, as a share of the first cost (at least one). */
    public static final float ANGEL_DISCOUNT = 0.2f;

    private static final Map<UUID, Long> LAST_KILL = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3> LAST_SPOT = new ConcurrentHashMap<>();
    /** Holy ground is looked up at most every few seconds per block a player stands on. */
    private static final Map<UUID, Long> HOLY_CHECKED = new ConcurrentHashMap<>();
    private static final Map<UUID, BlockPos> HOLY_AT = new ConcurrentHashMap<>();
    private static final Set<UUID> HOLY = ConcurrentHashMap.newKeySet();

    /** Bosses who are angels (fallen or not), for the essence of a kill. */
    private static final Set<EntityType<?>> ANGEL_BOSSES = ConcurrentHashMap.newKeySet();

    private EssenceSources() {
    }

    /** Adds {@code base} (scaled) to a sworn player's bar. @return what was added */
    public static float gain(ServerPlayer p, float base) {
        Allegiance a = Allegiances.get(p);
        if (!a.committed() || base <= 0) return 0;
        if (a.isDemon() && BossTwists.inDarkness(p)) return 0;
        float amount = (float) (base * SNConfig.ESSENCE_GAIN_MULTIPLIER.get());
        Allegiances.addEssence(p, amount);
        return amount;
    }

    /** What {@code slain} counts as, for essence. */
    public static EssenceRules.Kind kind(LivingEntity slain) {
        if (slain.getType().is(AllTags.Entities.BOSSES)) {
            if (Kin.isDemon(slain)) return EssenceRules.Kind.DEMON_BOSS;
            if (angelBoss(slain.getType())) return EssenceRules.Kind.ANGEL_BOSS;
            return EssenceRules.Kind.BOSS;
        }
        if (slain instanceof RivalHunterEntity) return EssenceRules.Kind.HUNTER;
        if (Kin.isDemon(slain)) return EssenceRules.Kind.DEMON;
        if (Kin.isAngel(slain)) return EssenceRules.Kind.ANGEL;
        return EssenceRules.Kind.OTHER;
    }

    static boolean angelBoss(EntityType<?> type) {
        if (ANGEL_BOSSES.isEmpty()) {
            ANGEL_BOSSES.add(AllEntities.LUCIFER.get());
            ANGEL_BOSSES.add(AllEntities.LUCIFER_UNCAGED.get());
            ANGEL_BOSSES.add(AllEntities.MICHAEL.get());
            ANGEL_BOSSES.add(AllEntities.METATRON.get());
            ANGEL_BOSSES.add(AllEntities.BROKEN_CHORUS.get());
            ANGEL_BOSSES.add(AllEntities.GABRIEL.get());
            ANGEL_BOSSES.add(AllEntities.RAPHAEL.get());
        }
        return ANGEL_BOSSES.contains(type);
    }

    /** {@code killer} slew {@code slain}: essence, the bloodlust fed, a Knight's wounds closed. */
    public static float onKill(ServerPlayer killer, LivingEntity slain) {
        Allegiance a = Allegiances.get(killer);
        long now = killer.serverLevel().getGameTime();
        Long before = LAST_KILL.put(killer.getUUID(), now);
        if (!a.committed()) return 0;
        boolean suppressed = PowerCaster.suppressed(killer);
        float base = slain instanceof ServerPlayer victim
                ? EssenceRules.forPlayerKill(a.faction(), Allegiances.get(victim).faction(), killer.server.isPvpAllowed())
                : EssenceRules.forKill(a.faction(), kind(slain));
        float got = gain(killer, EssenceRules.share(base, a.maxEssence()));
        if (PowerRules.passiveActive(a, Power.KILL_REGEN, suppressed)) killer.heal(KILL_REGEN);
        if (before != null && EssenceRules.starving(a.faction(), a.rank(), (int) ((now - before) / 20))) {
            AllegianceFx.toSelf(killer, AllegianceFxPayload.WHISPER, Passives.WHISPER_FED, 0, killer.position(), 60);
        }
        return got;
    }

    /** Seconds since {@code p} last killed (counted from login if never). */
    public static int secondsSinceKill(ServerPlayer p) {
        Long t = LAST_KILL.get(p.getUUID());
        long now = p.serverLevel().getGameTime();
        if (t == null || t > now) {
            LAST_KILL.put(p.getUUID(), now);
            return 0;
        }
        return (int) ((now - t) / 20);
    }

    /** Test hook: pretend the last kill was {@code seconds} ago. */
    public static void setLastKill(ServerPlayer p, int secondsAgo) {
        LAST_KILL.put(p.getUUID(), p.serverLevel().getGameTime() - secondsAgo * 20L);
    }

    /**
     * Each second, how a sworn player rests (for the refill): crouched and still is kneeling (in prayer, or in the flames);
     * an angel's holy ground, a demon's fire or lava and the Nether or Hell.
     */
    public static EssenceRules.Rest rest(ServerPlayer p, Allegiance a) {
        Vec3 last = LAST_SPOT.put(p.getUUID(), p.position());
        Vec3 d = last == null ? null : p.position().subtract(last);
        // Lava lets a body sink and bob: only drifting sideways breaks the stillness.
        boolean still = d != null && d.x * d.x + d.z * d.z < 0.01 && Math.abs(d.y) < 0.5;
        boolean kneeling = p.isCrouching() && still;
        if (a.isAngel()) return EssenceRules.angelRest(kneeling, onHolyGround(p));
        if (a.isDemon()) return EssenceRules.demonRest(kneeling, inHellfire(p), infernal(p.serverLevel()));
        return EssenceRules.Rest.NATURAL;
    }

    /** Whether {@code p} stands in fire, soul fire or lava (block states: a test player is never ticked into {@code isInLava}). */
    public static boolean inHellfire(ServerPlayer p) {
        return p.level().getBlockStates(p.getBoundingBox().deflate(0.001))
                .anyMatch(s -> s.is(BlockTags.FIRE) || s.getFluidState().is(FluidTags.LAVA));
    }

    /** The Nether, or Hell. */
    public static boolean infernal(Level level) {
        return level.dimension() == Level.NETHER || HellDimension.isHell(level);
    }

    /** Whether {@code p} stands on holy ground (looked up again when they move to another block, or every 5 s). */
    public static boolean onHolyGround(ServerPlayer p) {
        long now = p.serverLevel().getGameTime();
        BlockPos at = p.blockPosition();
        Long checked = HOLY_CHECKED.get(p.getUUID());
        if (checked == null || now - checked >= 100 || !at.equals(HOLY_AT.get(p.getUUID()))) {
            HOLY_CHECKED.put(p.getUUID(), now);
            HOLY_AT.put(p.getUUID(), at);
            if (ConsecratedGround.holy(p.serverLevel(), at)) HOLY.add(p.getUUID());
            else HOLY.remove(p.getUUID());
        }
        return HOLY.contains(p.getUUID());
    }

    /**
     * A demon crouches and hands a villager an emerald: a pact. One per villager; it gives something from its stall and
     * the demon gains Corruption. @return whether a pact was sealed
     */
    public static boolean pact(ServerPlayer p, Villager villager, ItemStack held) {
        if (!Allegiances.get(p).isDemon() || !p.isCrouching() || !held.is(Items.EMERALD) || villager.isBaby()) return false;
        if (villager.getPersistentData().hasUUID(PACT_TAG)) {
            AllegianceFx.toSelf(p, AllegianceFxPayload.WHISPER, Passives.WHISPER_PACT_REFUSED, 0, villager.position(), 60);
            return true;
        }
        if (!p.getAbilities().instabuild) held.shrink(1);
        villager.getPersistentData().putUUID(PACT_TAG, p.getUUID());
        ItemStack gift = ItemStack.EMPTY;
        var offers = villager.getOffers();
        if (!offers.isEmpty()) {
            MerchantOffer o = offers.get(p.getRandom().nextInt(offers.size()));
            gift = o.getResult().copy();
        }
        if (gift.isEmpty()) gift = new ItemStack(Items.EMERALD, 2);
        if (!p.getInventory().add(gift) && !gift.isEmpty()) p.drop(gift, false);
        gain(p, EssenceRules.share(EssenceRules.VILLAGER_PACT_SHARE, Allegiances.get(p).maxEssence()));
        AllegianceFx.around(villager, AllegianceFxPayload.PACT, 0, 0, villager.getEyePosition(), 40);
        AllegianceFx.toSelf(p, AllegianceFxPayload.PACT, 0, 0, villager.getEyePosition(), 40);
        p.serverLevel().sendParticles(AllParticles.HELLFIRE.get(), villager.getX(), villager.getEyeY(), villager.getZ(), 16, 0.3, 0.3, 0.3, 0.02);
        p.serverLevel().playSound(null, villager.blockPosition(), AllSounds.CONTRACT_BURN.get(), SoundSource.NEUTRAL, 1f, 0.8f);
        AllegianceFx.tell(p, "message.supernaturalcraft.allegiance.pact", true, ChatFormatting.DARK_RED);
        return true;
    }

    /** An angel at a villager's stall: every price a little lower (until the trade window closes). */
    public static void angelPrices(ServerPlayer p, Villager villager) {
        if (!Allegiances.get(p).isAngel()) return;
        for (MerchantOffer o : villager.getOffers()) {
            o.addToSpecialPriceDiff(-Math.max(1, Math.round(o.getBaseCostA().getCount() * ANGEL_DISCOUNT)));
        }
    }

    static void forget(UUID player) {
        LAST_KILL.remove(player);
        LAST_SPOT.remove(player);
        HOLY_CHECKED.remove(player);
        HOLY_AT.remove(player);
        HOLY.remove(player);
    }
}
