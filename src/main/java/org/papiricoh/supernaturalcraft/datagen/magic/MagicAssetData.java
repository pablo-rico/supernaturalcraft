package org.papiricoh.supernaturalcraft.datagen.magic;

import net.minecraft.core.particles.ParticleType;
import org.papiricoh.supernaturalcraft.magic.MagicAssets;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/**
 * The magic's 2D assets as datagen sees them (v0.21, owned by the art work), called from the shared providers. Each magic
 * particle uses its own sheet drawn by {@code tools/artgen/magic_art.py}: {@code textures/particle/<id>_<n>.png} with
 * {@link MagicAssets#PARTICLE_FRAMES} frames (white/grey, tinted by the particle's options).
 */
public final class MagicAssetData {

    private MagicAssetData() {
    }

    @FunctionalInterface
    public interface SpriteSets {
        void accept(ParticleType<?> type, String baseName, int frames);
    }

    /** Each magic particle's sprite set. */
    public static void particles(SpriteSets out) {
        for (String id : MagicAssets.PARTICLES) {
            out.accept(AllParticles.MAGIC.get(id).get(), id, MagicAssets.PARTICLE_FRAMES.get(id));
        }
    }
}
