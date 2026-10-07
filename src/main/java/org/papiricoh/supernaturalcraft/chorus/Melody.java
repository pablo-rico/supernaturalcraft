package org.papiricoh.supernaturalcraft.chorus;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;

/**
 * The seven bells of a Choir Altar and the three-note hymn that wakes the Broken Chorus. Each
 * bell is a note of the scale and a colour (the stained glass at its foot, the panes of the
 * temple window that gives its hymn away).
 */
public final class Melody {

    public static final int NOTES = 7, LENGTH = 3;
    public static final String[] NAMES = {"red", "orange", "yellow", "green", "cyan", "blue", "purple"};
    public static final DyeColor[] DYES = {DyeColor.RED, DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.LIME, DyeColor.CYAN,
            DyeColor.BLUE, DyeColor.PURPLE};
    public static final int[] RGB = {0xE8423A, 0xF08A2C, 0xF2D64A, 0x7CD045, 0x46C8D8, 0x4466E0, 0xA050E0};
    private static final ChatFormatting[] CHAT = {ChatFormatting.RED, ChatFormatting.GOLD, ChatFormatting.YELLOW, ChatFormatting.GREEN,
            ChatFormatting.AQUA, ChatFormatting.BLUE, ChatFormatting.LIGHT_PURPLE};
    /** Semitones above the lowest bell: a major scale. */
    private static final int[] SEMITONES = {0, 2, 4, 5, 7, 9, 11};
    private static final float BASE_PITCH = 0.7f;

    private Melody() {
    }

    public static float pitch(int note) {
        return BASE_PITCH * (float) Math.pow(2, SEMITONES[note] / 12.0);
    }

    /** A hymn of three notes; neighbours never repeat, so each note is clearly a new bell. */
    public static byte[] generate(RandomSource random) {
        byte[] m = new byte[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            int n;
            do {
                n = random.nextInt(NOTES);
            } while (i > 0 && n == m[i - 1]);
            m[i] = (byte) n;
        }
        return m;
    }

    public static boolean valid(byte[] m) {
        if (m == null || m.length != LENGTH) return false;
        for (byte b : m) if (b < 0 || b >= NOTES) return false;
        return true;
    }

    public static MutableComponent describe(byte[] m) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < m.length; i++) {
            if (i > 0) out.append(Component.literal(" → ").withStyle(ChatFormatting.GRAY));
            out.append(Component.translatable("color.minecraft." + DYES[m[i]].getSerializedName()).withStyle(CHAT[m[i]]));
        }
        return out;
    }
}
