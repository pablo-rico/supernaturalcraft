package org.papiricoh.supernaturalcraft.magic.mastery;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.magic.spell.SpellCastEvent;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * Server side of magical mastery (v0.21, rules in {@link MasteryRules}): XP from sigils ({@link SpellCastEvent}), the bowl and
 * rites, the mana bonus folded into {@link ArcanaData#maxMana()}, and the owner-only sync ({@link MagicFx#syncMastery}), sent
 * when the whole-number XP changes and on login, respawn and dimension change.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class Mastery {

    private Mastery() {
    }

    public static MasteryData get(Player player) {
        return player.getData(AllAttachments.MASTERY);
    }

    /** The rank in {@code school} that counts for its pages ({@link MasteryData#rankFor}). */
    public static MasteryRules.Rank rankFor(Player player, School school) {
        return get(player).rankFor(school);
    }

    /**
     * Whether {@code player} may learn a page of {@code tier} in {@code school}: the matching rank, or Morningstar's Spark (as
     * Adept). Creative players may learn anything.
     */
    public static boolean mayLearn(Player player, School school, int tier) {
        if (player.getAbilities().instabuild) return true;
        return MasteryRules.mayLearn(tier, rankFor(player, school), ManaManager.get(player).hasGrace());
    }

    /** Why a page of {@code tier} in {@code school} cannot be learnt yet: the rank it needs, in that school or in any. */
    public static Component needsRank(School school, int tier) {
        Component rank = Component.translatable("mastery.supernaturalcraft." + MasteryRules.requiredRank(tier).id());
        return (school.isNone()
                ? Component.translatable("message.supernaturalcraft.page.needs_rank_any", rank)
                : Component.translatable("message.supernaturalcraft.page.needs_rank", rank, Component.translatable("school.supernaturalcraft." + school.id())))
                .withStyle(ChatFormatting.RED);
    }

    /** Adds XP for a sigil that cost {@code mana}. */
    public static void sigilCast(ServerPlayer player, School school, float mana) {
        add(player, school, MasteryRules.sigilXp(mana));
    }

    /** Adds XP for a bowl spell that cost {@code mana}. */
    public static void bowlCast(ServerPlayer player, School school, float mana) {
        add(player, school, MasteryRules.bowlXp(mana));
    }

    /** Adds XP for a rite that cost {@code mana}. */
    public static void ritualDone(ServerPlayer player, School school, float mana) {
        add(player, school, MasteryRules.ritualXp(mana));
    }

    /** Adds {@code gain} XP in {@code school} (nothing for {@link School#NONE}); tells the player when they reach a new rank. */
    public static void add(ServerPlayer player, School school, float gain) {
        if (school.isNone() || gain <= 0) return;
        set(player, school, get(player).xp(school) + gain, true);
    }

    /** Sets the XP in {@code school} (commands, tests), keeping the client and max mana in step. */
    public static void set(ServerPlayer player, School school, float xp) {
        set(player, school, xp, false);
    }

    private static void set(ServerPlayer player, School school, float xp, boolean announce) {
        if (school.isNone()) return;
        MasteryData data = get(player);
        float before = data.xp(school);
        MasteryRules.Rank was = data.rank(school);
        data.setXp(school, xp);
        player.setData(AllAttachments.MASTERY, data);
        MasteryRules.Rank now = data.rank(school);
        if (now != was) {
            refreshMana(player);
            if (announce && now.atLeast(was)) announce(player, school, now);
            // v0.21: the roadmap's mastery milestones (any school), granted by code.
            if (now.atLeast(was)) {
                for (MasteryRules.Rank r : MasteryRules.Rank.values()) {
                    if (r != MasteryRules.Rank.NONE && now.atLeast(r)) {
                        org.papiricoh.supernaturalcraft.reward.ChorusRewards.award(player, "main/mastery_" + r.id());
                    }
                }
            }
        }
        if ((int) before != (int) data.xp(school)) sync(player);
    }

    private static void announce(ServerPlayer player, School school, MasteryRules.Rank rank) {
        player.displayClientMessage(Component.translatable("message.supernaturalcraft.mastery.rank",
                Component.translatable("mastery.supernaturalcraft." + rank.id()), Component.translatable("school.supernaturalcraft." + school.id()))
                .withStyle(ChatFormatting.GOLD), false);
        player.serverLevel().playSound(null, player.blockPosition(), AllSounds.SIGIL_LEARN.get(), SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    /** Puts the mastery's mana bonus (highest rank's) into the arcana. */
    public static void refreshMana(ServerPlayer player) {
        ManaManager.get(player).setMasteryMana(MasteryRules.manaBonus(get(player).ranks()));
    }

    /** Sends the owner their XP. */
    public static void sync(ServerPlayer player) {
        MasteryData d = get(player);
        MagicFx.syncMastery(player, (int) d.xp(School.ENOCHIAN), (int) d.xp(School.INFERNAL), (int) d.xp(School.WITCHCRAFT),
                (int) d.xp(School.HUNTER));
    }

    // --- events ----------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onSpellCast(SpellCastEvent event) {
        sigilCast(event.getCaster(), event.getSpell().school(), event.getManaSpent());
    }

    // The bonus is transient in the arcana: put it back before the arcana is first synced (high priority).

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            refreshMana(p);
            sync(p);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            refreshMana(p);
            sync(p);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            refreshMana(p);
            sync(p);
        }
    }
}
