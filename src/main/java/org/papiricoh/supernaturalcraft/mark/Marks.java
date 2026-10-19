package org.papiricoh.supernaturalcraft.mark;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceFx;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.power.Passives;
import org.papiricoh.supernaturalcraft.allegiance.power.PowerCaster;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.network.AllegianceFxPayload;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Mark of Cain on the server (v0.21): who bears it ({@link MarkData}, the {@code MARK} attachment), giving and taking it,
 * the Thirst's second-by-second climb and what it does ({@link MarkRules}), and the owner-only sync
 * ({@code MagicFx.syncMark}: on change of the Mark, each {@link #SYNC_STEP} of Thirst, on login, respawn and change of
 * dimension). The Mark's flares ({@code MARK_FLARE}) are seen by everyone near.
 */
public final class Marks {

    /** The advancement for bearing the Mark (impossible: granted here). */
    public static final String ADVANCEMENT = "main/marked";
    /** The owner's HUD is told when the Thirst has moved this much since the last sync. */
    public static final float SYNC_STEP = 0.01f;
    /** How long a flare plays. */
    public static final int FLARE_TICKS = 40;

    private static final Map<UUID, Float> SYNCED = new ConcurrentHashMap<>();
    private static final Set<UUID> FEEDING = ConcurrentHashMap.newKeySet();

    private Marks() {
    }

    public static MarkData get(Player player) {
        return player.getData(AllAttachments.MARK);
    }

    public static boolean marked(Player player) {
        return get(player).marked();
    }

    /** The Thirst (0 if unmarked). */
    public static float thirst(Player player) {
        MarkData m = get(player);
        return m.marked() ? m.thirst() : 0f;
    }

    /** Whether {@code player} bears the Mark and is in bloodlust now. */
    public static boolean inBloodlust(Player player) {
        MarkData m = get(player);
        return m.marked() && MarkRules.bloodlust(m.thirst());
    }

    /** Sets the data, and tells the owner when it changed enough to show. */
    public static void set(ServerPlayer player, MarkData data) {
        MarkData before = get(player);
        player.setData(AllAttachments.MARK, data);
        Float last = SYNCED.get(player.getUUID());
        if (before.marked() != data.marked() || last == null || Math.abs(last - data.thirst()) >= SYNC_STEP - 1e-6f
                || (MarkRules.full(data.thirst()) != MarkRules.full(last))) {
            sync(player);
        }
    }

    /** The owner's HUD, now (and what others see: the Mark on the arm, its bloodlust, as allegiance flags). */
    public static void sync(ServerPlayer player) {
        MarkData m = get(player);
        SYNCED.put(player.getUUID(), m.thirst());
        MagicFx.syncMark(player, m.marked(), m.thirst());
        showOthers(player, m);
    }

    /** The Mark as others see it ({@code Allegiances.MARKED}, {@code MARK_BLOODLUST}; a resync only when it changes). */
    static void showOthers(ServerPlayer player, MarkData m) {
        Allegiances.setFlag(player, Allegiances.MARKED, m.marked());
        Allegiances.setFlag(player, Allegiances.MARK_BLOODLUST, m.marked() && MarkRules.bloodlust(m.thirst()));
    }

    /**
     * Gives {@code player} the Mark ({@code main/marked}), with no Thirst — or, if it was once taken off (P6 {@code unmark}),
     * with {@link MarkRules#RETAKEN_THIRST} already ({@link MarkData#regranted}); {@code by} is who gave it (Cain),
     * {@code flare} plays its burning in. @return false if they already bore it
     */
    public static boolean grant(ServerPlayer player, @Nullable UUID by, boolean flare) {
        MarkData before = get(player);
        if (before.marked()) return false;
        set(player, MarkData.regranted(before, player.serverLevel().getGameTime(), Optional.ofNullable(by)));
        ChorusRewards.award(player, ADVANCEMENT);
        if (before.everRemoved()) {
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.mark.remembers")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), false);
            announce(player, 0f, get(player).thirst());
        }
        if (flare) {
            MagicFx.markFlare(player.serverLevel(), player, 0f, MagicFxPayload.MARK_BLOODLUST, FLARE_TICKS * 2);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2f, 0.6f);
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.mark.granted")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
        }
        return true;
    }

    /** Takes the Mark off (P6, Rowena's {@code unmark}); a Knight keeps their rank. @return false if there was none */
    public static boolean remove(ServerPlayer player) {
        if (!marked(player)) return false;
        set(player, get(player).removed());
        FEEDING.remove(player.getUUID());
        return true;
    }

    /** Sets the Thirst (commands, tests, the honey). */
    public static void setThirst(ServerPlayer player, float thirst) {
        MarkData m = get(player);
        if (!m.marked()) return;
        float before = m.thirst();
        set(player, m.withThirst(thirst));
        announce(player, before, get(player).thirst());
    }

    /** Test hook: the last kill was {@code secondsAgo} seconds ago. */
    public static void setLastKill(ServerPlayer player, int secondsAgo) {
        MarkData m = get(player);
        set(player, m.killedAt(player.serverLevel().getGameTime() - secondsAgo * 20L, m.thirst()));
    }

    // --- each second -------------------------------------------------------------------------------------------------------

    /** Once a second per living player: the Thirst climbs past the grace; at full the Mark feeds on its bearer. */
    public static void second(ServerPlayer player) {
        MarkData m = get(player);
        if (!m.marked()) {
            // A Knight of Hell made some other way (a command, an older world mid-session) bears the Mark too: one meter.
            migrate(player);
            return;
        }
        long now = player.serverLevel().getGameTime();
        // Chuck's arena takes the powers; the Mark sleeps there too.
        if (PowerCaster.suppressed(player)) return;
        float before = m.thirst();
        float after = MarkRules.rise(before, now - m.lastKill(), 1);
        if (after != before) {
            set(player, m.withThirst(after));
            announce(player, before, after);
        }
        showOthers(player, get(player));
        if (MarkRules.full(after)) {
            int s = (int) ((now / 20) % MarkRules.FEED_INTERVAL);
            if (s == 0) {
                player.hurt(player.damageSources().starve(), MarkRules.FEED_HURT);
                AllegianceFx.toSelf(player, AllegianceFxPayload.WHISPER,
                        FEEDING.add(player.getUUID()) ? Passives.WHISPER_BLOODLUST_FED : Passives.WHISPER_STARVING, 0, player.position(), 60);
            }
        } else {
            FEEDING.remove(player.getUUID());
        }
    }

    /** The Thirst moved from {@code before} to {@code after}: flares and whispers. */
    static void announce(ServerPlayer player, float before, float after) {
        if (MarkRules.bloodlustStarts(before, after)) {
            MagicFx.markFlare(player.serverLevel(), player, after, MagicFxPayload.MARK_BLOODLUST, FLARE_TICKS);
            AllegianceFx.toSelf(player, AllegianceFxPayload.WHISPER, Passives.WHISPER_BLOODLUST, 0, player.position(), 80);
        } else if (MarkRules.pulses(before, after)) {
            MagicFx.markFlare(player.serverLevel(), player, after, MagicFxPayload.MARK_PULSE, FLARE_TICKS);
        }
    }

    // --- kills, healing, blows, death --------------------------------------------------------------------------------------

    /** {@code killer} slew {@code slain}: the Mark drinks. @return the Thirst slaked */
    public static float onKill(ServerPlayer killer, LivingEntity slain) {
        MarkData m = get(killer);
        if (!m.marked()) return 0;
        boolean wasLusting = MarkRules.bloodlust(m.thirst());
        float after = MarkRules.afterKill(m.thirst(), slain.getMaxHealth());
        set(killer, m.killedAt(killer.serverLevel().getGameTime(), after));
        FEEDING.remove(killer.getUUID());
        MagicFx.markFlare(killer.serverLevel(), killer, after, MagicFxPayload.MARK_KILL, FLARE_TICKS);
        if (wasLusting) AllegianceFx.toSelf(killer, AllegianceFxPayload.WHISPER, Passives.WHISPER_FED, 0, killer.position(), 60);
        return m.thirst() - after;
    }

    /** What a heal of {@code amount} comes to for {@code player}. */
    public static float heal(Player player, float amount) {
        MarkData m = get(player);
        return m.marked() ? amount * MarkRules.healFactor(m.thirst()) : amount;
    }

    /**
     * {@code player} died bearing the Mark at full Thirst and not a demon: they will rise one ({@link Allegiance#pendingDemon},
     * the crossroads' own way back). @return whether they fall
     */
    public static boolean onDeath(ServerPlayer player) {
        MarkData m = get(player);
        Allegiance a = Allegiances.get(player);
        if (!m.marked() || !MarkRules.fallsOnDeath(a.faction(), m.thirst())) return false;
        Allegiances.set(player, a.withPendingDemon(true));
        set(player, m.withThirst(MarkRules.AFTER_FALL));
        player.displayClientMessage(Component.translatable(a.isAngel() ? "message.supernaturalcraft.mark.fall_angel"
                : "message.supernaturalcraft.mark.fall").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), false);
        return true;
    }

    /** The Knights of Hell of older worlds bear the Mark (once, silently). @return whether it was given */
    public static boolean migrate(ServerPlayer player) {
        // Only for Knights who never bore it: a Mark taken off (P6 unmark) stays off; it can be taken again at Cain's grave.
        if (get(player).everRemoved()) return false;
        Allegiance a = Allegiances.get(player);
        if (!MarkRules.owedTheMark(a.faction(), a.rank(), marked(player))) return false;
        return grant(player, null, false);
    }

    public static void forget(UUID player) {
        SYNCED.remove(player);
        FEEDING.remove(player);
    }
}
