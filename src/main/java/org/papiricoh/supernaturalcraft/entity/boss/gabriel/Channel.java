package org.papiricoh.supernaturalcraft.entity.boss.gabriel;

/**
 * The four channels of TV Land, one per phase (pure, shared by both sides). Each has its own set (the arena is rewritten on
 * the same footprint, {@code ChannelLayouts}), its own costume for Gabriel ({@link Costume}) and its own rule.
 *
 * <ol>
 *   <li>{@link #SITCOM} ("CH 2"): the laugh track. While the LAUGH sign is lit he can't be touched and plays his gags; when
 *   it goes dark he takes more; the APPLAUSE heals him if nobody hits him.</li>
 *   <li>{@link #GAME_SHOW} ("CH 5"): the quiz. A question on screen, three coloured platforms, a buzzer; wrong or late is
 *   punished, right stuns him.</li>
 *   <li>{@link #HOSPITAL} ("CH 7"): Dr. Sexy, M.D. A heart monitor beeps; a hit on the beep is critical. The defibrillator
 *   shocks zones, the nurse doubles heal him if they reach him.</li>
 *   <li>{@link #COMMERCIAL} ("CH 9"): five spokesmen at five podiums; only the real one casts the six-winged shadow.</li>
 * </ol>
 */
public enum Channel {
    SITCOM(2, Costume.SWEATER),
    GAME_SHOW(5, Costume.TUXEDO),
    HOSPITAL(7, Costume.LAB_COAT),
    COMMERCIAL(9, Costume.SUIT);

    /** The number on the on-screen display ("CH 2"). */
    public final int number;
    /** What Gabriel wears on it. */
    public final Costume costume;

    Channel(int number, Costume costume) {
        this.number = number;
        this.costume = costume;
    }

    /** @return the channel of phase 1–4 (clamped) */
    public static Channel ofPhase(int phase) {
        Channel[] all = values();
        return all[Math.max(0, Math.min(all.length - 1, phase - 1))];
    }

    public int phase() {
        return ordinal() + 1;
    }

    /** Translation key suffix and art name. */
    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * What Gabriel (or a double) wears: one rig, one texture per costume, costume bones shown or hidden by the renderer
     * ({@code GabrielAssets.COSTUME_BONES}). {@link #JACKET} is his own clothes: the summoning, the death, the pranks.
     */
    public enum Costume {
        JACKET, SWEATER, TUXEDO, LAB_COAT, SUIT;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
