package org.papiricoh.supernaturalcraft.registry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

public class AllParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, SupernaturalCraft.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HELLFIRE = register("hellfire");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GRACE = register("grace");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SIGIL = register("sigil");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DEMON_SMOKE = register("demon_smoke");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VOID_MOTE = register("void_mote");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FROST = register("frost");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ASH = register("ash");
    /** Azazel's smoke: sulphur-yellow where the demons' is black. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> YELLOW_SMOKE = register("yellow_smoke");
    /** Lilith's light: white, cold, everywhere once her vessel splits. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WHITE_LIGHT = register("white_light");
    /** Metatron's ink: black, edged with gold. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> INK = register("ink");
    /** A loose page, fluttering. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PAGE = register("page");

    /** A bowl spell's smoke, in the spell's own colour. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<net.minecraft.core.particles.ColorParticleOption>> BOWL_SMOKE =
            PARTICLE_TYPES.register("bowl_smoke", () -> new ParticleType<net.minecraft.core.particles.ColorParticleOption>(false) {
                @Override
                public com.mojang.serialization.MapCodec<net.minecraft.core.particles.ColorParticleOption> codec() {
                    return net.minecraft.core.particles.ColorParticleOption.codec(this);
                }

                @Override
                public net.minecraft.network.codec.StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, net.minecraft.core.particles.ColorParticleOption> streamCodec() {
                    return net.minecraft.core.particles.ColorParticleOption.streamCodec(this);
                }
            });

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> register(String name) {
        return PARTICLE_TYPES.register(name, () -> new SimpleParticleType(false));
    }

    public static void init() {
    }
}
