package org.papiricoh.supernaturalcraft.coven.ending;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.bowl.spell.BloodSample;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaNpcEntity;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaRelation;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaServerHandlers;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.RowenaAllyEntity;
import org.papiricoh.supernaturalcraft.magic.MagicAssets;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.mark.Marks;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

/**
 * Taking the Mark of Cain off (v0.21, P6): {@code unmark}, the Book of the Damned's cure, read by a witch who survived the
 * witch boss fight — Rowena for a hunter who gave her the Book and saw the Grand Witch fall, the Grand Witch for one who kept the
 * Book and killed Rowena (and must show her the Book: she reads the cure from it). The price is a vial of the Marked hunter's
 * own bound blood. Two ways to it, one rule ({@link #why}):
 * <ul>
 *   <li>the survivor's dialogue ({@code RowenaDialogue.OPT_UNMARK}, {@code GrandWitchDialogue.OPT_UNMARK}): she casts it, taking the
 *   vial from the hunter's pack;</li>
 *   <li>the bowl spell {@code unmark} ({@link UnmarkEffect}), cast by anyone who knows it with the vial in the bowl and the
 *   survivor within {@link #SURVIVOR_REACH} blocks.</li>
 * </ul>
 * {@link Marks#remove} keeps a Knight's rank and remembers the Mark was once borne (taken again at Cain's grave, it comes back
 * thirsty). Grants {@link WitchBossSlot#UNMARKED}.
 */
public final class Unmarking {

    /** How near the survivor must stand to the bowl. */
    public static final double SURVIVOR_REACH = 8;

    private Unmarking() {
    }

    /** A vial of {@code player}'s own bound blood in their pack, or empty. */
    public static ItemStack bloodOf(Player player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            BloodSample b = s.is(AllItems.BOUND_BLOOD.get()) ? s.get(AllDataComponents.BLOOD_SAMPLE.get()) : null;
            if (b != null && b.owner().equals(player.getUUID())) return s;
        }
        return ItemStack.EMPTY;
    }

    public static boolean holdsBook(@Nullable Player player) {
        return player != null && player.getInventory().countItem(AllItems.BOOK_OF_THE_DAMNED.get()) > 0;
    }

    /**
     * Why the Mark cannot come off {@code target} now (a translation key), or null. {@code book}: whether the Book is at hand (the
     * Grand Witch reads from it); {@code needBlood}: whether the vial must be in their pack (the dialogue; in the bowl it is
     * an ingredient).
     */
    public static @Nullable String why(ServerPlayer target, boolean book, boolean needBlood) {
        if (!Marks.marked(target)) return "message.supernaturalcraft.unmark.not_marked";
        RowenaRelation.Book choice = RowenaServerHandlers.relation(target).book();
        if (!WitchBossSlot.beaten(choice, p -> AuthorWorld.done(target, p))) return "message.supernaturalcraft.unmark.no_survivor";
        if (choice == RowenaRelation.Book.KEPT && !book) return "message.supernaturalcraft.unmark.needs_book";
        if (needBlood && bloodOf(target).isEmpty()) return "message.supernaturalcraft.unmark.needs_blood";
        return null;
    }

    /** Whether {@code witch} is the survivor who may take the Mark off {@code target} (Rowena for the Book's giver, the Grand Witch for its keeper). */
    public static boolean survivorFor(LivingEntity witch, ServerPlayer target) {
        RowenaRelation.Book choice = RowenaServerHandlers.relation(target).book();
        return switch (choice) {
            case GIVEN -> witch instanceof RowenaNpcEntity || witch instanceof RowenaAllyEntity;
            case KEPT -> witch instanceof GrandWitchNpcEntity;
            case NONE -> false;
        };
    }

    /** The survivor for {@code target} within {@link #SURVIVOR_REACH} of {@code at}, or null. */
    public static @Nullable LivingEntity survivorNear(ServerLevel level, Vec3 at, ServerPlayer target) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(SURVIVOR_REACH),
                e -> e.isAlive() && e.position().distanceTo(at) <= SURVIVOR_REACH)) {
            if (survivorFor(e, target)) return e;
        }
        return null;
    }

    /**
     * {@code survivor} takes the Mark off {@code target}, taking a vial of their bound blood from their pack if {@code takeBlood}.
     * {@code sealFrom}: where the spell's seal rises and flies to them from (her bowl, her hands), or null when a spell bowl
     * draws its own. Does not check {@link #why}. @return false if they bore no Mark
     */
    public static boolean perform(ServerPlayer target, @Nullable LivingEntity survivor, @Nullable Vec3 sealFrom, boolean takeBlood) {
        if (!Marks.marked(target)) return false;
        if (takeBlood && !target.getAbilities().instabuild) {
            ItemStack blood = bloodOf(target);
            if (!blood.isEmpty()) blood.shrink(1);
        }
        ServerLevel level = target.serverLevel();
        if (sealFrom != null) {
            int seal = MagicAssets.sealIndex("unmark");
            MagicFx.bowlSeal(level, School.WITCHCRAFT, sealFrom, survivor, seal, 40);
            MagicFx.bowlSend(level, School.WITCHCRAFT, sealFrom, target.position().add(0, 1, 0), target, seal,
                    (int) Math.max(16, Math.min(60, sealFrom.distanceTo(target.position()) * 2)));
        }
        MagicFx.markFlare(level, target, Marks.thirst(target), MagicFxPayload.MARK_PULSE, 60);
        MagicFx.hex(level, target, target.position().add(0, 1, 0), MagicFxPayload.HEX_PROTECT, 1.6f, 40);
        Marks.remove(target);
        level.playSound(null, target.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.5f, 0.6f);
        level.playSound(null, target.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.5f, 0.5f);
        target.displayClientMessage(Component.translatable("message.supernaturalcraft.unmark.done").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
        ChorusRewards.award(target, WitchBossSlot.UNMARKED);
        return true;
    }
}
