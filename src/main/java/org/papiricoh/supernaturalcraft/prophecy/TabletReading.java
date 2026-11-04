package org.papiricoh.supernaturalcraft.prophecy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.mastery.Mastery;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;
import org.papiricoh.supernaturalcraft.network.RateLimiter;
import org.papiricoh.supernaturalcraft.network.SNNetworking;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.network.TabletGuessPayload;
import org.papiricoh.supernaturalcraft.network.TabletPayload;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.ArrayList;
import java.util.List;

/**
 * Reading a tablet on the server (v0.36). Opening a chapter shows the reader its glyphs and the letters they know, plus a few
 * free ones ({@link WordCipher#FREE_LETTERS}, more for a prophet; always the same ones for that chapter). A try is judged
 * whole: a wrong one hurts ({@link #WRONG_DAMAGE}, worse and sickening for one who is not a prophet) and says how many glyphs
 * were wrong, and a prophet is shown one more letter; a right one reads the chapter, teaches every letter in it and gives its
 * reward ({@link #reward}). Then the tablet rests ({@link Tablet#restDays}) before its next chapter shows. A tablet read to its
 * end adds {@link #MANA_PER_TABLET} to the mana bar.
 */
public final class TabletReading {

    public static final float WRONG_DAMAGE = 3f, PROPHET_WRONG_DAMAGE = 1f;
    public static final int NAUSEA_TICKS = 120, MANA_PER_TABLET = 25;

    private TabletReading() {
    }

    static int book(Tablet t, int chapter) {
        return t.ordinal() * 16 + chapter;
    }

    static long seed(ServerPlayer sp, Tablet t) {
        return WordCipher.seed(sp.serverLevel().getSeed(), sp.getUUID(), t);
    }

    /** The letters shown free in a chapter (the same every time it is opened). */
    static int free(ServerPlayer sp, Tablet t, int chapter) {
        int count = Prophecies.prophet(sp) ? WordCipher.PROPHET_FREE_LETTERS : WordCipher.FREE_LETTERS;
        return WordCipher.hints(WordCipher.phrase(t, chapter), 0, count, seed(sp, t) + chapter);
    }

    /** Opens {@code t}'s next unread chapter, or says it is all read. */
    public static void open(ServerPlayer sp, Tablet t) {
        int chapter = Prophecies.read(sp, t) + 1;
        if (chapter > Tablet.CHAPTERS) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.tablet.all_read").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        long left = Prophecies.get(sp).ready(t) - Prophecies.today(sp.server);
        if (left > 0) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.tablet.resting", left).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        int free = free(sp, t, chapter);
        Prophecies.update(sp, p -> p.withKnown(t, p.known(t) | free));
        send(sp, t, chapter, -1);
        sp.serverLevel().playSound(null, sp.blockPosition(), AllSounds.TABLET_WHISPER.get(), SoundSource.PLAYERS, 0.6f, 1f);
    }

    private static void send(ServerPlayer sp, Tablet t, int chapter, int status) {
        int[] perm = WordCipher.permutation(seed(sp, t));
        List<Integer> glyphs = new ArrayList<>();
        for (int g : WordCipher.glyphs(WordCipher.phrase(t, chapter), perm)) glyphs.add(g);
        int known = Prophecies.get(sp).known(t);
        List<Integer> key = new ArrayList<>();
        for (int g = 0; g < WordCipher.LETTERS; g++) key.add(-1);
        for (int l = 0; l < WordCipher.LETTERS; l++) if ((known & 1 << l) != 0) key.set(perm[l], l);
        Send.toPlayer(sp, new TabletPayload(book(t, chapter), glyphs, key, Prophecies.prophet(sp), status));
    }

    /** A try at a chapter. */
    public static void guess(ServerPlayer sp, TabletGuessPayload payload) {
        if (!RateLimiter.allow(sp, "tablet_guess", 0.5, 2)) return;
        Tablet t = Tablet.byOrdinal(payload.book() / 16);
        int chapter = payload.book() % 16;
        if (chapter != Prophecies.read(sp, t) + 1 || chapter > Tablet.CHAPTERS || !holds(sp, t)) return;
        long today = Prophecies.today(sp.server);
        if (Prophecies.get(sp).ready(t) > today) return;
        String phrase = WordCipher.phrase(t, chapter);
        int[] perm = WordCipher.permutation(seed(sp, t));
        int[] guess = new int[WordCipher.LETTERS];
        for (int g = 0; g < WordCipher.LETTERS; g++) guess[g] = g < payload.guess().size() ? payload.guess().get(g) : -1;
        int wrong = WordCipher.wrong(phrase, perm, guess);
        boolean prophet = Prophecies.prophet(sp);
        if (wrong > 0) {
            float blow = Math.min(prophet ? PROPHET_WRONG_DAMAGE : WRONG_DAMAGE, Math.max(0, sp.getHealth() - 1));
            if (blow > 0) sp.hurt(sp.damageSources().magic(), blow);
            if (!prophet) sp.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_TICKS, 0));
            if (prophet) {
                int known = Prophecies.get(sp).known(t);
                int more = WordCipher.hints(phrase, known, WordCipher.PROPHET_REVEAL, seed(sp, t) ^ sp.level().getGameTime());
                Prophecies.update(sp, p -> p.withKnown(t, p.known(t) | more));
            }
            sp.serverLevel().playSound(null, sp.blockPosition(), AllSounds.GLYPH_WRONG.get(), SoundSource.PLAYERS, 1f, 1f);
            send(sp, t, chapter, wrong);
            return;
        }
        int rest = Tablet.restDays(chapter);
        Prophecies.update(sp, p -> p.withRead(t, chapter).withKnown(t, p.known(t) | WordCipher.lettersOf(phrase)).withReady(t, today + rest));
        send(sp, t, chapter, 0);
        sp.displayClientMessage(Component.translatable("message.supernaturalcraft.tablet.read",
                Component.translatable("item.supernaturalcraft." + t.id() + "_tablet"), chapter,
                Component.translatable("tablet.supernaturalcraft." + t.id() + "." + chapter)).withStyle(ChatFormatting.GOLD), false);
        sp.serverLevel().playSound(null, sp.blockPosition(), AllSounds.TABLET_WHISPER.get(), SoundSource.PLAYERS, 1.2f, 1.2f);
        reward(sp, t, chapter);
        if (rest > 0) sp.displayClientMessage(Component.translatable("message.supernaturalcraft.tablet.rests", rest).withStyle(ChatFormatting.GRAY), false);
    }

    private static boolean holds(ServerPlayer sp, Tablet t) {
        for (ItemStack s : List.of(sp.getMainHandItem(), sp.getOffhandItem())) {
            if (s.getItem() instanceof TabletItem ti && ti.tablet() == t) return true;
            if (t == Tablet.ANGEL && s.is(AllItems.ANGEL_TABLET.get())) return true;
        }
        return false;
    }

    /** What reading {@code chapter} of {@code t} gives. */
    static void reward(ServerPlayer sp, Tablet t, int chapter) {
        switch (t) {
            case ANGEL -> {
                switch (chapter) {
                    case 1 -> teach(sp, "the_fall");
                    case 2 -> teach(sp, "angel_radio");
                    case Tablet.SUPREME_WARD_CHAPTER -> sp.displayClientMessage(Component.translatable("message.supernaturalcraft.tablet.supreme_ward")
                            .withStyle(ChatFormatting.AQUA), false);
                    case 4 -> teach(sp, "stolen_grace");
                    case 5 -> teach(sp, "enochs_chain");
                    case 6 -> teach(sp, "mass_expulsion");
                    default -> {
                    }
                }
            }
            case DEMON -> {
                switch (chapter) {
                    case 1 -> teach(sp, "mass_exorcism");
                    case Tablet.ENGRAVING_CHAPTER -> {
                        sp.displayClientMessage(Component.translatable("message.supernaturalcraft.tablet.engraving").withStyle(ChatFormatting.RED), false);
                        ItemStack chisel = new ItemStack(AllItems.ENGRAVED_DEVILS_TRAP.get(), 2);
                        if (!sp.getInventory().add(chisel)) sp.drop(chisel, false);
                    }
                    case 3 -> teach(sp, "cast_trap");
                    case 4 -> teach(sp, "broken_contract");
                    case 5 -> teach(sp, "bound_smoke");
                    case Tablet.TRIALS_CHAPTER -> org.papiricoh.supernaturalcraft.prophecy.trials.Trials.offer(sp);
                    default -> {
                    }
                }
            }
            case LEVIATHAN -> {
                switch (chapter) {
                    case 1 -> teach(sp, "purgatory_bind");
                    case 2 -> teach(sp, "seal_in_jar");
                    case 3 -> teach(sp, "hunger");
                    case Tablet.ALLY_CHAPTER -> sp.displayClientMessage(Component.translatable("message.supernaturalcraft.tablet.ally")
                            .withStyle(ChatFormatting.DARK_AQUA), false);
                    case 5 -> teach(sp, "mimicry");
                    case 6 -> teach(sp, "black_tide");
                    default -> {
                    }
                }
            }
        }
        if (chapter == Tablet.CHAPTERS) {
            Mastery.refreshMana(sp);
            SNNetworking.syncArcana(sp);
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.tablet.complete", MANA_PER_TABLET).withStyle(ChatFormatting.LIGHT_PURPLE), false);
            ChorusRewards.award(sp, "main/word_of_god");
        }
    }

    private static void teach(ServerPlayer sp, String sigil) {
        ResourceLocation id = SupernaturalCraft.asResource(sigil);
        if (!ManaManager.get(sp).learn(id)) return;
        SNNetworking.syncArcana(sp);
        sp.displayClientMessage(Component.translatable("message.supernaturalcraft.page.learned",
                Component.translatable(SigilComponent.translationKey(id)).withStyle(ChatFormatting.GOLD)), false);
        sp.serverLevel().playSound(null, sp.blockPosition(), AllSounds.SIGIL_LEARN.get(), SoundSource.PLAYERS, 1f, 1f);
    }

    /** The mana the tablets read to their end give {@code player}. */
    public static float wordMana(Player player) {
        Prophecy p = Prophecies.get(player);
        int whole = 0;
        for (Tablet t : Tablet.values()) if (p.read(t) >= Tablet.CHAPTERS) whole++;
        return whole * MANA_PER_TABLET;
    }
}
