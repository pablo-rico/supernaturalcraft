package org.papiricoh.supernaturalcraft.client.magic.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/**
 * The magic's sounds, played by the client from the effects' own payloads (v0.21: {@code magic.<school>.<cast|impact|charge|
 * complete>}, {@code bowl.*}, {@code ritual.rift|implode}). The server sends the moment, not a sound, so every sound arrives in
 * step with what it accompanies.
 */
public final class MagicSounds {

    private MagicSounds() {
    }

    public static void school(School school, String id, Vec3 at, float volume, float pitch) {
        try {
            play(AllSounds.magic(school, id), at, volume, pitch);
        } catch (RuntimeException ignored) {
            // Not registered (an older data pack): stay silent.
        }
    }

    public static void other(String event, Vec3 at, float volume, float pitch) {
        try {
            play(AllSounds.magic(event), at, volume, pitch);
        } catch (RuntimeException ignored) {
        }
    }

    public static void play(SoundEvent sound, Vec3 at, float volume, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        mc.level.playLocalSound(at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch, false);
    }

    /**
     * A looping hum that lives while {@code alive} holds, its volume following {@code volume} (0-1) and fading out when it ends:
     * a ritual's charge.
     */
    public static void loop(School school, String id, Vec3 at, DoubleSupplier volume, BooleanSupplier alive) {
        Minecraft mc = Minecraft.getInstance();
        try {
            mc.getSoundManager().play(new Loop(AllSounds.magic(school, id), at, volume, alive));
        } catch (RuntimeException ignored) {
        }
    }

    private static final class Loop extends AbstractTickableSoundInstance {
        private final DoubleSupplier target;
        private final BooleanSupplier alive;
        private float level;

        Loop(SoundEvent sound, Vec3 at, DoubleSupplier target, BooleanSupplier alive) {
            super(sound, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.target = target;
            this.alive = alive;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01f;
            this.x = at.x;
            this.y = at.y;
            this.z = at.z;
        }

        @Override
        public void tick() {
            float want = alive.getAsBoolean() ? (float) Mth.clamp(target.getAsDouble(), 0, 1) : 0f;
            level += Mth.clamp(want - level, -0.06f, 0.04f);
            volume = Math.max(0.001f, level);
            pitch = 0.85f + 0.3f * level;
            if (!alive.getAsBoolean() && level <= 0.01f) stop();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }
}
