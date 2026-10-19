package org.papiricoh.supernaturalcraft.entity.boss.cain;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceDialogue;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.mark.Marks;
import org.papiricoh.supernaturalcraft.network.CainFxPayload;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.weapon.curse.Curses;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * What Cain leaves (v0.21), per hunter who fought him: his bust and three jars of his honey the first time; a rematch the bust
 * and one jar. A hunter's first victory is the one before they hold {@link #ADVANCEMENT} (the kill grants it just after).
 *
 * <p><b>The Mark.</b> As he kneels he offers it to every hunter who fought him (the shared dialogue, id {@link #DIALOGUE},
 * node {@link #OFFER}: {@link #TAKE} or {@link #REFUSE}; silence refuses). Whoever refuses (or was not asked) may take it at his
 * grave later, if they beat him ({@link #GRAVE}, asked by the grave itself: {@code cain.CainWorld#useGrave}). Taking it gives
 * the Mark ({@code mark.Marks#grant}, {@code main/marked}) and — the first time a hunter takes it, if they carry no First Blade
 * — his First Blade, bound to them (canon: the Mark and the Blade go together; without the Mark the Blade only hungers).
 *
 * <p><b>Taken again.</b> A hunter whose Mark was taken off (Rowena's or the Grand Witch's {@code unmark}, P6) may take it again at
 * his grave if they beat him ({@link #GRAVE_AGAIN}), but it comes back with {@code MarkRules.RETAKEN_THIRST} (0.5) already:
 * "it remembers the way" ({@code MarkData.everRemoved}, {@code Marks.grant}).
 */
public final class CainSpoils {

    public static final String ADVANCEMENT = "main/first_born";
    public static final String DIALOGUE = "cain_mark";
    public static final String OFFER = "offer", GRAVE = "grave", TAKE = "take", REFUSE = "refuse";
    /** The grave's offer to a hunter whose Mark was taken off (P6 {@code unmark}): it comes back thirsty. */
    public static final String GRAVE_AGAIN = "grave_again";
    /** How close to his grave a hunter must stand to take the Mark there. */
    public static final double GRAVE_REACH = 6;

    private CainSpoils() {
    }

    // --- the spoils ----------------------------------------------------------------------------------------------------

    /** Everyone's share at {@code at}; with no hunter to credit, one rematch's share. */
    public static void drop(ServerLevel level, List<ServerPlayer> hunters, Vec3 at, RandomSource random) {
        if (hunters.isEmpty()) {
            for (ItemStack s : rematch()) spawn(level, at, s);
            return;
        }
        for (ServerPlayer hunter : hunters) for (ItemStack s : share(hunter)) spawn(level, at, s);
    }

    public static List<ItemStack> share(ServerPlayer hunter) {
        return AuthorWorld.done(hunter, ADVANCEMENT) ? rematch() : first();
    }

    /** The first victory: the bust and three jars of honey. */
    public static List<ItemStack> first() {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(AllItems.CAIN_TROPHY.get()));
        out.add(new ItemStack(AllItems.CAINS_HONEY.get(), 3));
        return out;
    }

    /** A rematch: the bust and a jar. */
    public static List<ItemStack> rematch() {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(AllItems.CAIN_TROPHY.get()));
        out.add(new ItemStack(AllItems.CAINS_HONEY.get()));
        return out;
    }

    private static void spawn(ServerLevel level, Vec3 at, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }

    // --- the Mark ------------------------------------------------------------------------------------------------------

    /** Hooks the offer into the shared dialogue (once, at mod construction). */
    public static void registerDialogue() {
        AllegianceDialogue.register(DIALOGUE, CainSpoils::answer);
    }

    /** As he kneels: the Mark offered to each of {@code hunters} who does not bear it. @return how many were asked */
    public static int offerAll(CainEntity cain, List<? extends LivingEntity> hunters) {
        int n = 0;
        for (LivingEntity h : hunters) {
            if (!(h instanceof ServerPlayer p)) continue;
            if (Marks.marked(p)) {
                p.displayClientMessage(Component.translatable("message.supernaturalcraft.cain.already_marked").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), false);
                continue;
            }
            AllegianceDialogue.ask(p, cain, DIALOGUE, OFFER, List.of(TAKE, REFUSE), CainBalance.OFFER_TICKS);
            cain.fx(new CainFxPayload(cain.getId(), CainFxPayload.OFFER, p.getId(), 0, cain.position(), CainBalance.OFFER_TICKS));
            n++;
        }
        if (n > 0) cain.playSound(AllSounds.cain("offer_mark"), 2f, 1f);
        return n;
    }

    /** Whether {@code player} may take the Mark at his grave: they beat him and do not bear it. */
    public static boolean mayTakeAtGrave(ServerPlayer player) {
        return !Marks.marked(player) && AuthorWorld.done(player, ADVANCEMENT);
    }

    /** At his grave: the Mark offered again (the grave speaks through the hunter's own panel). */
    public static void offerAtGrave(ServerPlayer player) {
        String node = Marks.get(player).everRemoved() ? GRAVE_AGAIN : GRAVE;
        AllegianceDialogue.ask(player, player, DIALOGUE, node, List.of(TAKE, REFUSE), 0);
    }

    /** An answer from the shared dialogue. */
    public static void answer(ServerPlayer player, @Nullable Entity speaker, String node, String option) {
        if (!TAKE.equals(option)) {
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.cain.refused").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
            return;
        }
        if (GRAVE.equals(node) || GRAVE_AGAIN.equals(node)) {
            if (!org.papiricoh.supernaturalcraft.cain.CainWorld.nearGrave(player, GRAVE_REACH) || !mayTakeAtGrave(player)) return;
            take(player, null);
        } else {
            take(player, speaker instanceof CainEntity c ? c.getUUID() : null);
        }
    }

    /**
     * {@code player} takes the Mark: it burns into their arm; the first time, his First Blade too if they carry none.
     * Public for tests (fake players answer by hand). @return whether they took it now
     */
    public static boolean take(ServerPlayer player, @Nullable UUID cain) {
        boolean firstTime = !AuthorWorld.done(player, Marks.ADVANCEMENT);
        if (!Marks.grant(player, cain, true)) return false;
        if (firstTime && player.getInventory().countItem(AllItems.FIRST_BLADE.get()) == 0) {
            ItemStack blade = new ItemStack(AllItems.FIRST_BLADE.get());
            blade.set(AllDataComponents.CURSE, Curses.state(blade).boundTo(player.getUUID()));
            if (!player.getInventory().add(blade)) player.drop(blade, false);
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.cain.blade").withStyle(ChatFormatting.DARK_RED), false);
        }
        Send.toPlayer(player, new CinematicPayload(40, 0.3f, CainCinematics.FLASH, 0.8f, false, "", ""));
        if (player.serverLevel() instanceof ServerLevel level) {
            for (ServerPlayer p : level.players()) {
                if (p.distanceToSqr(player) < 64 * 64) {
                    Send.toPlayer(p, new CainFxPayload(-1, CainFxPayload.OFFER, player.getId(), 1, player.position(), 60));
                }
            }
        }
        return true;
    }
}
