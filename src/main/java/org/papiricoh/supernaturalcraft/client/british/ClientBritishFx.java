package org.papiricoh.supernaturalcraft.client.british;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.british.BritishFxPayload;
import org.papiricoh.supernaturalcraft.client.apocalypse.seal.SealOverlay;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

import java.util.ArrayList;
import java.util.List;

/**
 * Plays season 12's moments on this client (v0.41): title cards (drawn by the seals' card layer, which shows during camera
 * sequences) and the shots, hexes, wards, telekinesis and hellfire as particles. Every kind is in its payload's javadoc.
 */
public final class ClientBritishFx {

    private static final String[] WHO = {"ketch", "hess", "dagon", "birth"};
    private static final int[] COLOUR = {0xC8CCD2, 0x7FD6E0, 0xD2321E, 0xFFE7A8};

    /** Wards and lingering clouds still being drawn: point, radius, palette, ticks left. */
    private record Lasting(Vec3 at, float radius, int palette, boolean dome, int[] left) {
    }

    private static final List<Lasting> LASTING = new ArrayList<>();

    private ClientBritishFx() {
    }

    public static void handle(BritishFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;
        Vec3 at = p.point();
        switch (p.kind()) {
            case BritishFxPayload.TITLE -> {
                int who = Mth.clamp(p.arg(), 0, WHO.length - 1);
                String key = "title.supernaturalcraft." + WHO[who] + (p.arg2() == 0 ? ".fall" : ".phase" + p.arg2());
                SealOverlay.card(Component.translatable(key), Component.translatable(key + ".sub"), COLOUR[who], p.duration());
            }
            case BritishFxPayload.RING -> ring(level, at, p.arg() / 10f, particle(p.arg2()), Math.max(24, Math.round(p.arg() / 10f * 8)));
            case BritishFxPayload.LANE -> lane(level, at, p.arg(), (p.arg2() % 10000) / 10f, particle(p.arg2() / 10000));
            case BritishFxPayload.CLOUD -> {
                cloud(level, at, p.arg() / 10f, p.arg2(), 60);
                if (p.duration() > 10) LASTING.add(new Lasting(at, p.arg() / 10f, p.arg2(), false, new int[]{p.duration()}));
            }
            case BritishFxPayload.FLASH -> {
                level.addParticle(ParticleTypes.FLASH, at.x, at.y, at.z, 0, 0, 0);
                float size = p.arg() / 10f;
                RandomSource r = level.random;
                for (int i = 0; i < 40; i++) {
                    Vec3 d = new Vec3(r.nextGaussian(), r.nextGaussian(), r.nextGaussian()).normalize().scale(size * 0.1);
                    level.addParticle(particle(p.arg2()), at.x, at.y, at.z, d.x, d.y, d.z);
                }
                if (p.duration() > 10) LASTING.add(new Lasting(at, size * 0.3f, p.arg2(), false, new int[]{p.duration()}));
            }
            case BritishFxPayload.WARD -> LASTING.add(new Lasting(at, p.arg() / 10f, BritishFxPayload.HEX, true, new int[]{p.duration()}));
            default -> {
            }
        }
    }

    /** Client tick (from {@link BritishRenderers}): the wards and clouds that last. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            LASTING.clear();
            return;
        }
        LASTING.removeIf(l -> --l.left()[0] <= 0);
        for (Lasting l : LASTING) {
            if (l.dome()) {
                RandomSource r = level.random;
                for (int i = 0; i < 6; i++) {
                    double a = r.nextDouble() * Mth.TWO_PI, b = r.nextDouble() * Math.PI / 2;
                    level.addParticle(ParticleTypes.ENCHANT, l.at().x + Math.cos(a) * Math.cos(b) * l.radius(), l.at().y + Math.sin(b) * l.radius(),
                            l.at().z + Math.sin(a) * Math.cos(b) * l.radius(), 0, 0, 0);
                }
            } else if (level.getGameTime() % 3 == 0) {
                cloud(level, l.at(), l.radius(), l.palette(), 8);
            }
        }
    }

    static ParticleOptions particle(int palette) {
        return switch (palette) {
            case BritishFxPayload.FIRE -> AllParticles.HELLFIRE.get();
            case BritishFxPayload.HEX -> ParticleTypes.WITCH;
            case BritishFxPayload.FORCE -> ParticleTypes.PORTAL;
            case BritishFxPayload.LIGHT -> ParticleTypes.END_ROD;
            case BritishFxPayload.SMOKE -> ParticleTypes.LARGE_SMOKE;
            default -> ParticleTypes.CRIT;
        };
    }

    private static void cloud(ClientLevel level, Vec3 at, float radius, int palette, int n) {
        RandomSource r = level.random;
        ParticleOptions p = palette == BritishFxPayload.SMOKE ? ParticleTypes.CAMPFIRE_COSY_SMOKE : particle(palette);
        for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Mth.TWO_PI, d = Math.sqrt(r.nextDouble()) * radius;
            level.addParticle(p, at.x + Math.cos(a) * d, at.y + 0.2 + r.nextDouble() * 1.8, at.z + Math.sin(a) * d, 0, 0.01, 0);
        }
    }

    private static void ring(ClientLevel level, Vec3 at, float radius, ParticleOptions particle, int n) {
        for (int i = 0; i < n; i++) {
            double a = Mth.TWO_PI * i / n;
            level.addParticle(particle, at.x + Math.cos(a) * radius, at.y + 0.2, at.z + Math.sin(a) * radius, 0, 0.05, 0);
        }
    }

    private static void lane(ClientLevel level, Vec3 from, float yaw, float length, ParticleOptions particle) {
        Vec3 dir = new Vec3(-Mth.sin(yaw * Mth.DEG_TO_RAD), 0, Mth.cos(yaw * Mth.DEG_TO_RAD));
        int n = Math.max(8, Math.round(length * 2));
        for (int i = 0; i < n; i++) {
            Vec3 q = from.add(dir.scale(length * i / n));
            level.addParticle(particle, q.x, q.y + 1.1, q.z, dir.x * 0.15, 0, dir.z * 0.15);
        }
    }
}
