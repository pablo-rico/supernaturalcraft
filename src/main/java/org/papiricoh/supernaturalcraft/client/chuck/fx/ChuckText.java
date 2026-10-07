package org.papiricoh.supernaturalcraft.client.chuck.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * Typewriter text for the Author's fight: the font (the mod's own typewriter font if the art ships one, otherwise
 * Minecraft's monospaced {@code uniform}), the ink and paper colours, and {@link Typist}, which reveals a line letter
 * by letter with the keys' clatter and a bell at the end.
 */
public final class ChuckText {

    public static final int INK = 0x1A1410, INK_BLUE = 0x1E1A3B, PAPER = 0xF2E8D0, PAPER_DARK = 0xD8CBA8, RED_INK = 0x8A1A10;
    private static final ResourceLocation OWN_FONT = SupernaturalCraft.asResource("typewriter");
    private static final ResourceLocation UNIFORM = ResourceLocation.withDefaultNamespace("uniform");
    private static ResourceLocation font;

    private ChuckText() {
    }

    public static ResourceLocation font() {
        if (font == null) {
            boolean own = Minecraft.getInstance().getResourceManager()
                    .getResource(SupernaturalCraft.asResource("font/typewriter.json")).isPresent();
            font = own ? OWN_FONT : UNIFORM;
        }
        return font;
    }

    public static Style style() {
        return Style.EMPTY.withFont(font());
    }

    public static Component typed(String text) {
        return Component.literal(text).withStyle(style());
    }

    public static Component typedItalic(String text) {
        return Component.literal(text).withStyle(style().withItalic(true));
    }

    public static int argb(float alpha, int rgb) {
        return ((int) (Mth.clamp(alpha, 0, 1) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    /** Reveals {@code text} at {@code perTick} letters per tick, starting {@code delay} ticks in; clicks as it goes. */
    public static final class Typist {
        public final String text;
        private final float perTick;
        private final int delay;
        private final boolean bell;
        private int played;
        private boolean rang;

        public Typist(String text, float perTick, int delay, boolean bell) {
            this.text = text;
            this.perTick = perTick;
            this.delay = delay;
            this.bell = bell;
        }

        /** How many letters show at {@code age} ticks (fractional for partial ticks). */
        public int shown(float age) {
            return Mth.clamp((int) ((age - delay) * perTick), 0, text.length());
        }

        public String visible(float age) {
            return text.substring(0, shown(age));
        }

        public boolean done(float age) {
            return shown(age) >= text.length();
        }

        /** Ticks until the whole line is typed. */
        public int length() {
            return delay + (int) Math.ceil(text.length() / perTick);
        }

        /** Call once per tick: the key clicks (one per tick at most, none for spaces) and the bell. */
        public void tick(int age) {
            int n = shown(age);
            if (n > played) {
                boolean key = false;
                for (int i = played; i < n; i++) key |= !Character.isWhitespace(text.charAt(i));
                if (key) sound(AllSounds.CHUCK_TYPE.get(), 0.9f + (float) Math.random() * 0.25f, 0.55f);
                played = n;
            }
            if (bell && !rang && n >= text.length() && !text.isEmpty()) {
                rang = true;
                sound(AllSounds.CHUCK_BELL.get(), 1f, 0.7f);
            }
        }
    }

    public static void sound(net.minecraft.sounds.SoundEvent event, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, pitch, volume));
    }
}
