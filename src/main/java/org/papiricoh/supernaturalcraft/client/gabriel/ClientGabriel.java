package org.papiricoh.supernaturalcraft.client.gabriel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.Channel;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.QuizBank;
import org.papiricoh.supernaturalcraft.network.GabrielFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.trickster.PrankRules;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Plays out each {@link GabrielFxPayload} (v0.14) and keeps what lasts a while: the channel flip (static, rolling bars, the
 * "CH n" display), the studio signs, the quiz round, the heart monitor's beeps, the prank's hint, who wears a party hat and
 * which double free will sees through. {@link GabrielOverlay} draws the screen side, {@link GabrielWorldFx} the world side.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ClientGabriel {

    /** How long the "CH n" display stays up after a flip; how long a sign stays on the HUD after its last word. */
    static final int OSD_TICKS = 90, SIGN_DARK_HOLD = 40, QUIZ_RESULT_TICKS = 40, PRANK_TICKS = 110, BEAT_HOLD = 80;

    static long ticks;

    // The channel flip.
    static int flipChannel = -1, flipLength;
    static long flipAt = Long.MIN_VALUE / 2;

    // The studio signs: lit or dark, and when the server last spoke of each.
    static final boolean[] SIGN_LIT = new boolean[3];
    /** Client tick each sign leaves the HUD (Long.MAX_VALUE: until told otherwise). */
    static final long[] SIGN_UNTIL = {Long.MIN_VALUE / 2, Long.MIN_VALUE / 2, Long.MIN_VALUE / 2};
    static final long[] SIGN_LIT_AT = {Long.MIN_VALUE / 2, Long.MIN_VALUE / 2, Long.MIN_VALUE / 2};

    // The quiz round.
    static int quizQuestion = -1, quizLength, quizResult = -1;
    static int[] quizDeal = {0, 1, 2};
    static long quizAt, quizResultAt = Long.MIN_VALUE / 2;

    // The heart monitor.
    static final Deque<Long> BEEPS = new ArrayDeque<>();
    static int beatPeriod = 20;

    // The prank's hint.
    static int prank = -1;
    static long prankAt = Long.MIN_VALUE / 2;

    /** Mob id → game time its party hat comes off. */
    static final Map<Integer, Long> HATS = new HashMap<>();
    /** Double id → client tick free will stops seeing through it. */
    static final Map<Integer, Long> SEEN_THROUGH = new HashMap<>();

    private ClientGabriel() {
    }

    public static void handle(GabrielFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        switch (p.kind()) {
            case GabrielFxPayload.CHANNEL -> flip(p.arg(), p.duration());
            case GabrielFxPayload.SIGN -> sign(p.arg(), p.arg2() == 1, p.duration());
            case GabrielFxPayload.QUIZ -> {
                if (p.arg() >= 0) quiz(p.arg(), QuizBank.unpack(p.arg2()), p.duration());
                else answer(p.arg2() == 1);
            }
            case GabrielFxPayload.BEAT -> beat(p.duration());
            case GabrielFxPayload.REVEAL -> GabrielWorldFx.reveal(p.entity(), p.point(), p.duration());
            case GabrielFxPayload.PRANK -> prank(p.arg(), p.point());
            case GabrielFxPayload.HAT -> hat(p.entity(), p.duration());
            case GabrielFxPayload.FREE_WILL_TELL -> SEEN_THROUGH.put(p.entity(), ticks + Math.max(1, p.duration()));
            case GabrielFxPayload.TITLE -> title(p.arg(), p.arg2() == 1, p.duration());
            default -> {
            }
        }
    }

    // --- the moments (also the preview's hooks) --------------------------------------------------------------------------

    /** Zap: static and rolling bars for {@code duration} ticks, then "CH n" in the corner. */
    public static void flip(int channel, int duration) {
        flipChannel = channel;
        flipLength = Math.max(6, duration);
        flipAt = ticks;
        // A new channel has its own signs and rules: what the last one left on screen goes.
        java.util.Arrays.fill(SIGN_UNTIL, Long.MIN_VALUE / 2);
        quizQuestion = -1;
        BEEPS.clear();
        ui(AllSounds.GABRIEL_STATIC.get(), 1f, 0.8f);
    }

    /**
     * A sign lit or dark for {@code duration} ticks (it stays on the HUD that long, and a little more); lit with no duration
     * stays until told otherwise, dark with none goes after a moment.
     */
    public static void sign(int which, boolean lit, int duration) {
        if (which < 0 || which >= SIGN_LIT.length) return;
        if (lit && (!SIGN_LIT[which] || SIGN_UNTIL[which] < ticks)) SIGN_LIT_AT[which] = ticks;
        SIGN_LIT[which] = lit;
        SIGN_UNTIL[which] = duration > 0 ? ticks + duration + 20 : lit ? Long.MAX_VALUE : ticks + SIGN_DARK_HOLD;
    }

    public static void quiz(int question, int[] deal, int duration) {
        if (question < 0 || question >= QuizBank.QUESTIONS.size()) return;
        quizQuestion = question;
        quizDeal = deal;
        quizLength = Math.max(1, duration);
        quizAt = ticks;
        quizResult = -1;
    }

    public static void answer(boolean right) {
        if (quizQuestion < 0) return;
        quizResult = right ? 1 : 0;
        quizResultAt = ticks;
        ui(right ? AllSounds.GABRIEL_DING.get() : AllSounds.GABRIEL_BUZZER.get(), 1f, 0.9f);
    }

    public static void beat(int toNext) {
        BEEPS.addLast(ticks);
        while (BEEPS.size() > 12) BEEPS.removeFirst();
        if (toNext > 0) beatPeriod = toNext;
    }

    public static void prank(int which, Vec3 point) {
        PrankRules.Prank[] all = PrankRules.Prank.values();
        if (which < 0 || which >= all.length) return;
        prank = which;
        prankAt = ticks;
        Minecraft mc = Minecraft.getInstance();
        SoundEvent sound = switch (all[which]) {
            case CANDY_WRAPPER, PARTY_HAT -> AllSounds.GABRIEL_SNAP.get();
            case LAUGH_TRACK -> AllSounds.GABRIEL_LAUGH_TRACK.get();
            case TV_LINE -> AllSounds.GABRIEL_APPLAUSE.get();
            case CHEST -> null;
        };
        if (sound == null || mc.level == null || mc.player == null) return;
        Vec3 at = point == null || point.lengthSqr() < 1e-6 ? mc.player.position() : point;
        float volume = all[which] == PrankRules.Prank.LAUGH_TRACK || all[which] == PrankRules.Prank.TV_LINE ? 0.45f : 0.7f;
        mc.getSoundManager().play(new SimpleSoundInstance(sound, SoundSource.AMBIENT, volume, 1f, RandomSource.create(), at.x, at.y, at.z));
    }

    public static void hat(int entity, int duration) {
        Minecraft mc = Minecraft.getInstance();
        if (duration <= 0 || mc.level == null) HATS.remove(entity);
        else HATS.put(entity, mc.level.getGameTime() + duration);
    }

    public static void title(int channel, boolean end, int duration) {
        Channel[] all = Channel.values();
        if (channel < 0 || channel >= all.length) return;
        GabrielOverlay.add(new GabrielOverlay.TitleCard(all[channel], end, duration));
    }

    // --- what the renderers ask --------------------------------------------------------------------------------------------

    /** Whether free will sees through this double (for this hunter). */
    public static boolean seenThrough(int entity) {
        Long until = SEEN_THROUGH.get(entity);
        return until != null && until > ticks;
    }

    /** Whether this mob wears a party hat now. */
    public static boolean hatted(int entity) {
        Long until = HATS.get(entity);
        Minecraft mc = Minecraft.getInstance();
        return until != null && mc.level != null && until > mc.level.getGameTime();
    }

    static boolean anyHats() {
        return !HATS.isEmpty();
    }

    private static void ui(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    /** Forget everything (left the world, or a preview between scenes). */
    public static void clear() {
        flipChannel = -1;
        flipAt = Long.MIN_VALUE / 2;
        java.util.Arrays.fill(SIGN_UNTIL, Long.MIN_VALUE / 2);
        quizQuestion = -1;
        BEEPS.clear();
        prank = -1;
        HATS.clear();
        SEEN_THROUGH.clear();
        GabrielOverlay.clear();
        GabrielWorldFx.clear();
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            if (ticks > 0 && (flipChannel >= 0 || !HATS.isEmpty() || quizQuestion >= 0)) clear();
            return;
        }
        ticks++;
        GabrielOverlay.tick();
        GabrielWorldFx.tick();
        if (quizQuestion >= 0) {
            boolean over = quizResult >= 0 ? ticks - quizResultAt > QUIZ_RESULT_TICKS : ticks - quizAt > quizLength + 60;
            if (over) quizQuestion = -1;
        }
        if (ticks % 20 == 0) {
            long now = mc.level.getGameTime();
            HATS.values().removeIf(until -> until <= now);
            SEEN_THROUGH.values().removeIf(until -> until <= ticks);
        }
    }
}
