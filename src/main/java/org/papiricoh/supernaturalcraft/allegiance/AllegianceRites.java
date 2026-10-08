package org.papiricoh.supernaturalcraft.allegiance;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.network.AllegianceFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

/**
 * What each allegiance rite does (convert, rank up, cure), with its advancement, cinematic and title.
 * <ul>
 *   <li>{@code convert}: a human free to choose becomes rank I of the side (a hunter's ranks are given up).</li>
 *   <li>{@code rank_up}: exactly one rank up ({@code rank} is the rank reached; a human's are the hunter's).</li>
 *   <li>{@code cure} of a demon: one night of three, the demon standing in a devil's trap; the last night makes them human
 *   and gives back the crossroads' hearts. {@code cure} of an angel (ripping out Grace): human at once, and the Grace is
 *   left in a vial ({@code result}).</li>
 * </ul>
 * Every change of side or rank sends {@link AllegianceFxPayload#ASCENSION} (arg = faction ordinal, arg2 = rank; on a cure
 * arg = the side left behind and arg2 = 0) to the player and everyone watching.
 */
public final class AllegianceRites {

    /** Ticks the ascension cinematic and title card last (the client plays them on its own). */
    public static final int ASCENSION_TICKS = 100;

    private AllegianceRites() {
    }

    public static boolean perform(AllegianceEffect effect, ServerLevel level, BlockPos altar, ServerPlayer ritualist) {
        Allegiance a = Allegiances.get(ritualist);
        long now = level.getGameTime();
        return switch (effect.op()) {
            case CONVERT -> {
                if (!effect.faction().supernatural() || !a.mayChoose(now)) yield false;
                convert(ritualist, effect.faction());
                yield true;
            }
            case RANK_UP -> {
                int to = effect.rank().orElse(a.rank() + 1);
                if (a.faction() != effect.faction() || a.rank() != to - 1 || to > effect.faction().maxRank()) yield false;
                rankUp(ritualist, to);
                yield true;
            }
            case CURE -> effect.faction() == Faction.DEMON ? cureNight(ritualist, level)
                    : effect.faction() == Faction.ANGEL && ripOutGrace(ritualist, altar, effect.result().orElse(null));
        };
    }

    /** {@code player} becomes rank I of {@code faction} (the rites, the crossroads' wish, a soul the hounds took). */
    public static void convert(ServerPlayer player, Faction faction) {
        Allegiance next = Allegiances.get(player).convert(faction);
        ascend(player, next);
    }

    /** One rank up on the player's own road. */
    public static void rankUp(ServerPlayer player, int rank) {
        Allegiance a = Allegiances.get(player);
        Allegiance next = a.withRank(rank);
        // A new rank comes with its bar at least half full.
        next = next.withEssence(Math.max(next.essence(), next.maxEssence() / 2f));
        ascend(player, next);
    }

    /** Sets {@code next} and marks the moment: advancement, cinematic + title card, sound, a line in chat. */
    public static void ascend(ServerPlayer player, Allegiance next) {
        Allegiances.set(player, next);
        Toll.apply(player);
        String adv = Ranks.advancement(next.faction(), next.rank());
        if (adv != null) ChorusRewards.award(player, adv);
        AllegianceFx.around(player, AllegianceFxPayload.ASCENSION, next.faction().ordinal(), next.rank(), player.position(), ASCENSION_TICKS);
        player.serverLevel().playSound(null, player.blockPosition(), ascensionSound(next.faction()), SoundSource.PLAYERS, 1.5f, 1f);
        player.displayClientMessage(Component.translatable("message.supernaturalcraft.allegiance.ascended",
                Component.translatable(Ranks.titleKey(next.faction(), next.rank()))).withStyle(color(next.faction())), false);
    }

    /** The demon cure, one night of it. False (nothing spent) outside a devil's trap or on a night already used. */
    static boolean cureNight(ServerPlayer player, ServerLevel level) {
        Allegiance a = Allegiances.get(player);
        if (!a.isDemon()) return false;
        if (!inDevilsTrap(player)) {
            AllegianceFx.tell(player, "message.supernaturalcraft.allegiance.cure_needs_trap", false, ChatFormatting.RED);
            return false;
        }
        long night = CureProgress.nightIndex(level.getDayTime());
        if (!CureProgress.canAdvance(a.cureStage(), a.lastCureNight(), night)) {
            AllegianceFx.tell(player, "message.supernaturalcraft.allegiance.cure_same_night", false, ChatFormatting.GRAY);
            return false;
        }
        int stage = a.cureStage() + 1;
        level.sendParticles(AllParticles.DEMON_SMOKE.get(), player.getX(), player.getEyeY(), player.getZ(), 40, 0.3, 0.6, 0.3, 0.05);
        if (!CureProgress.complete(stage)) {
            Allegiances.set(player, a.withCure(stage, night));
            AllegianceFx.tell(player, "message.supernaturalcraft.allegiance.cure_night", false, ChatFormatting.GOLD, stage, CureProgress.NIGHTS);
            player.hurt(player.damageSources().magic(), 4f);
            return true;
        }
        cure(player, Faction.DEMON);
        return true;
    }

    /** An angel tears out their Grace: human again, the Grace left in a vial on the altar. */
    static boolean ripOutGrace(ServerPlayer player, BlockPos altar, Item result) {
        if (!Allegiances.get(player).isAngel()) return false;
        cure(player, Faction.ANGEL);
        if (result != null) {
            ItemEntity vial = new ItemEntity(player.serverLevel(), altar.getX() + 0.5, altar.getY() + 1.2, altar.getZ() + 0.5, new ItemStack(result));
            vial.setDefaultPickUpDelay();
            player.serverLevel().addFreshEntity(vial);
        }
        player.hurt(player.damageSources().magic(), 6f);
        return true;
    }

    /** Human again: ranks gone, the toll given back, no new side for {@link CureProgress#CHOOSE_AGAIN_TICKS}. */
    public static void cure(ServerPlayer player, Faction from) {
        Allegiance a = Allegiances.get(player);
        Allegiances.set(player, a.cured(player.serverLevel().getGameTime() + CureProgress.CHOOSE_AGAIN_TICKS));
        Toll.apply(player);
        org.papiricoh.supernaturalcraft.allegiance.power.PowerCaster.forget(player);
        Allegiances.setFlag(player, Allegiances.EYES | Allegiances.TRUE_FORM | Allegiances.SMOKE | Allegiances.POSSESSING, false);
        AllegianceFx.around(player, AllegianceFxPayload.ASCENSION, from.ordinal(), 0, player.position(), ASCENSION_TICKS);
        player.serverLevel().playSound(null, player.blockPosition(), AllSounds.ALLEGIANCE_ASCEND_HUNTER.get(), SoundSource.PLAYERS, 1.5f, 0.8f);
        AllegianceFx.tell(player, "message.supernaturalcraft.allegiance.cured." + from.getSerializedName(), false, ChatFormatting.GOLD);
    }

    /** Standing in (any part of) a devil's trap. */
    public static boolean inDevilsTrap(ServerPlayer player) {
        BlockPos at = player.blockPosition();
        return player.level().getBlockState(at).is(AllBlocks.DEVILS_TRAP.get()) || player.level().getBlockState(at.below()).is(AllBlocks.DEVILS_TRAP.get());
    }

    static SoundEvent ascensionSound(Faction faction) {
        return switch (faction) {
            case ANGEL -> AllSounds.ALLEGIANCE_ASCEND_ANGEL.get();
            case DEMON -> AllSounds.ALLEGIANCE_ASCEND_DEMON.get();
            case HUMAN -> AllSounds.ALLEGIANCE_ASCEND_HUNTER.get();
        };
    }

    static ChatFormatting color(Faction faction) {
        return switch (faction) {
            case ANGEL -> ChatFormatting.GOLD;
            case DEMON -> ChatFormatting.DARK_RED;
            case HUMAN -> ChatFormatting.GRAY;
        };
    }
}
