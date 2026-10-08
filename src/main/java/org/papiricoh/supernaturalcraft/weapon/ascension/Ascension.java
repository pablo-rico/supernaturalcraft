package org.papiricoh.supernaturalcraft.weapon.ascension;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.balance.Balance;
import org.papiricoh.supernaturalcraft.balance.ProgressionScale;
import org.papiricoh.supernaturalcraft.balance.Vitality;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.entity.boss.BossDamage;
import org.papiricoh.supernaturalcraft.hunter.gear.HunterGearItem;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.reward.michael.GeneralArmorItem;
import org.papiricoh.supernaturalcraft.weapon.WeaponProfiles;
import org.papiricoh.supernaturalcraft.weapon.catalyst.Catalysts;

/**
 * Ascension (v0.15): a weapon or a piece of armour raised at the Hellforge, one tier at a time, with the Ascension Shard of
 * the next tier. A weapon's own damage, and every ability it has, is multiplied by {@link Balance#ascension}; armour hardens
 * and turns aside part of the great enemies' blows (Aegis, in {@code balance.DefenceEvents}).
 * <ul>
 *   <li>any weapon with a {@code weapon_profile} ascends from 0 to V;</li>
 *   <li>Hunter's Gear from 0 to IV;</li>
 *   <li>the General's armour is born at IV (Michael's own) and only a fifth-tier shard raises it to V.</li>
 * </ul>
 * Spells and faction powers have no item to ascend: against great enemies they grow with the hunter's tier instead
 * ({@link #vsBoss}).
 */
public final class Ascension {

    /** Where the General's armour starts. */
    public static final int GENERAL_BASE = 4;
    /** Hunter's Gear ascends no higher than this. */
    public static final int HUNTER_GEAR_MAX = 4;
    /** Experience levels an ascension costs, per tier reached. */
    public static final int LEVELS_PER_TIER = 5;

    private Ascension() {
    }

    // --- What can ascend, and how far ----------------------------------------------------------------------------------

    /** The stack's Ascension, 0-5 (a piece of the General's armour is at least IV). */
    public static int level(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        int raw = stack.getOrDefault(AllDataComponents.ASCENSION, 0);
        return stack.getItem() instanceof GeneralArmorItem ? Math.max(GENERAL_BASE, raw) : raw;
    }

    /** The highest Ascension this stack can reach (0 = it never ascends). */
    public static int maxLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.getItem() instanceof HunterGearItem) return HUNTER_GEAR_MAX;
        if (stack.getItem() instanceof GeneralArmorItem) return ProgressionScale.MAX_TIER;
        return WeaponProfiles.of(stack) != null ? ProgressionScale.MAX_TIER : 0;
    }

    public static boolean ascendable(ItemStack stack) {
        return maxLevel(stack) > 0;
    }

    public static boolean isArmor(ItemStack stack) {
        return stack.getItem() instanceof HunterGearItem || stack.getItem() instanceof GeneralArmorItem;
    }

    /** Experience levels it costs to reach {@code tier}. */
    public static int cost(int tier) {
        return LEVELS_PER_TIER * tier;
    }

    /** The shard tier of {@code shard}, or 0 if it is no Ascension Shard. */
    public static int shardTier(ItemStack shard) {
        return shard.getItem() instanceof AscensionShardItem s ? s.tier() : 0;
    }

    /**
     * Why {@code shard} can't ascend {@code stack} for {@code player}, or null if it can: {@code no_item}, {@code not_ascendable},
     * {@code max}, {@code no_shard}, {@code wrong_shard} or {@code no_xp}.
     */
    public static @Nullable String problem(ItemStack stack, ItemStack shard, @Nullable Player player) {
        if (stack.isEmpty()) return "no_item";
        if (!ascendable(stack)) return "not_ascendable";
        int level = level(stack);
        if (level >= maxLevel(stack)) return "max";
        int tier = shardTier(shard);
        if (tier == 0) return "no_shard";
        if (!ProgressionScale.shardFits(level, tier)) return "wrong_shard";
        if (player != null && !player.getAbilities().instabuild && player.experienceLevel < cost(level + 1)) return "no_xp";
        return null;
    }

    /** Raises {@code stack} one tier (no checks, no cost). */
    public static void raise(ItemStack stack) {
        stack.set(AllDataComponents.ASCENSION, Math.min(maxLevel(stack), level(stack) + 1));
    }

    // --- Damage ----------------------------------------------------------------------------------------------------------

    /** The multiplier of an Ascension level, config included (pure numbers if the server config is not loaded yet). */
    public static float multiplier(int level) {
        if (level <= 0) return 1f;
        try {
            return Balance.ascension(level);
        } catch (IllegalStateException notLoaded) {
            return ProgressionScale.ascensionMultiplier(level);
        }
    }

    public static float multiplier(ItemStack stack) {
        return multiplier(level(stack));
    }

    /** What an ability of {@code stack} worth {@code base} deals once its weapon's Ascension is counted. */
    public static float scale(ItemStack stack, float base) {
        return base * multiplier(stack);
    }

    /** {@link #scale} with whatever {@code user} holds in the main hand. */
    public static float held(@Nullable LivingEntity user, float base) {
        return user == null ? base : scale(user.getMainHandItem(), base);
    }

    // --- Spells and powers against the great enemies --------------------------------------------------------------------

    /**
     * A hunter's tier (1-5) from the great enemies they have beaten ({@link ProgressionScale#playerTier}): their advancements,
     * or the Vitality those victories left (which tests can grant to a fake player).
     */
    public static int playerTier(ServerPlayer player) {
        Vitality v = player.getData(AllAttachments.VITALITY);
        return ProgressionScale.playerTier(path -> AuthorWorld.done(player, path) || v.has(bossOf(path)));
    }

    private static String bossOf(String advancement) {
        for (BossProgression.Boss b : BossProgression.Boss.values()) if (b.advancement.equals(advancement)) return b.id();
        return "";
    }

    /** The tier a hunter's spells strike great enemies at: their own, or their catalyst's Ascension if higher. */
    public static int spellTier(ServerPlayer player) {
        return Math.max(playerTier(player), level(Catalysts.held(player).stack()));
    }

    /**
     * A spell or power worth {@code damage}, cast by {@code caster} on {@code target}: against a great enemy (or one of its parts)
     * it is multiplied by the caster's {@link #spellTier}; against anything else it is unchanged.
     */
    public static float vsBoss(@Nullable Entity caster, Entity target, float damage) {
        if (!(caster instanceof ServerPlayer player) || !BossDamage.isBoss(target)) return damage;
        return damage * multiplier(spellTier(player));
    }
}
