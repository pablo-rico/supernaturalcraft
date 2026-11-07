package org.papiricoh.supernaturalcraft.client.apocalypse.seal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealKind;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealWorld;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicOverlay;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicShake;
import org.papiricoh.supernaturalcraft.network.SealFxPayload;
import org.papiricoh.supernaturalcraft.network.UrielFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * Plays the seals' and Uriel's moments on this client (v0.40, the Seals slice): the cards ({@link SealOverlay}), the sounds, the
 * sky's red flash ({@link SealSky#flash}), particles and the white flash of Uriel's grace. Every kind is in its payload's javadoc.
 */
public final class ClientSealFx {

    private ClientSealFx() {
    }

    public static void handle(SealFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;
        RandomSource r = level.random;
        switch (p.kind()) {
            case SealFxPayload.WARNING -> {
                SealKind kind = SealKind.byOrdinal(p.arg());
                SealOverlay.card(Component.translatable("title.supernaturalcraft.seal.warning"), Component.translatable(kind.nameKey()),
                        SealOverlay.RED, p.duration());
                ui(AllSounds.apocalypse("seal.warning"), 1f);
            }
            case SealFxPayload.BROKEN -> {
                SealOverlay.card(Component.translatable("title.supernaturalcraft.seal.broken", p.arg()),
                        Component.translatable(SealWorld.sealName(p.arg())), SealOverlay.RED, 120);
                ui(AllSounds.apocalypse("seal.thunder_far"), 0.8f + r.nextFloat() * 0.2f);
                ui(AllSounds.apocalypse("seal.broken"), 1f);
                SealSky.flash(1f);
            }
            case SealFxPayload.HELD -> {
                SealOverlay.card(Component.translatable("title.supernaturalcraft.seal.held"), Component.translatable(SealKind.byOrdinal(p.arg()).nameKey()),
                        SealOverlay.GOLD, p.duration());
                ui(AllSounds.apocalypse("seal.held"), 1f);
                burst(level, p.point(), AllParticles.GRACE.get(), 40, 0.15);
            }
            case SealFxPayload.PULSE -> {
                Vec3 at = p.point();
                level.playLocalSound(at.x, at.y, at.z, AllSounds.apocalypse("seal.rite_chant"), SoundSource.HOSTILE, 1.2f,
                        0.8f + p.arg() / 250f, false);
                burst(level, at, AllParticles.HEX_MOTE.get(), 12 + p.arg() / 5, 0.05);
                if (p.arg() >= 75) level.playLocalSound(at.x, at.y, at.z, AllSounds.apocalypse("seal.breaking"), SoundSource.HOSTILE, 1f, 1f, false);
            }
            case SealFxPayload.PLAGUE -> {
                Vec3 at = p.point();
                int n = 30;
                for (int i = 0; i < n; i++) {
                    double a = r.nextDouble() * Mth.TWO_PI, d = r.nextDouble() * p.arg();
                    level.addParticle(i % 3 == 0 ? AllParticles.BLOOD_DROP.get() : AllParticles.HEX_MOTE.get(), at.x + Math.cos(a) * d,
                            at.y + 0.3 + r.nextDouble(), at.z + Math.sin(a) * d, 0, 0.02, 0);
                }
            }
            default -> {
            }
        }
    }

    public static void handle(UrielFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;
        Vec3 at = p.point();
        RandomSource r = level.random;
        switch (p.kind()) {
            case UrielFxPayload.TITLE -> {
                String key = switch (p.arg()) {
                    case 2 -> "title.supernaturalcraft.uriel.phase2";
                    case 3 -> "title.supernaturalcraft.uriel.fall";
                    case 4 -> "title.supernaturalcraft.uriel.castiel";
                    default -> "title.supernaturalcraft.uriel.phase1";
                };
                SealOverlay.card(Component.translatable(key), Component.translatable(key + ".sub"), SealOverlay.GOLD, p.duration());
            }
            case UrielFxPayload.SLASH -> burst(level, at.add(0, 1, 0), AllParticles.GRACE.get(), 14, 0.2);
            case UrielFxPayload.PALM -> {
                float radius = p.arg() / 10f;
                for (int i = 0; i < 40; i++) {
                    double a = r.nextDouble() * Mth.TWO_PI, d = r.nextDouble() * radius;
                    level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x + Math.cos(a) * d, at.y + 0.1, at.z + Math.sin(a) * d, 0, 0.03, 0);
                }
                MagicShake.kick(at, 1.2f, 12, 24);
            }
            case UrielFxPayload.SMITE -> {
                float yaw = p.arg(), reach = p.arg2() / 10f;
                Vec3 dir = new Vec3(-Mth.sin(yaw * Mth.DEG_TO_RAD), 0, Mth.cos(yaw * Mth.DEG_TO_RAD));
                for (int i = 0; i < 50; i++) {
                    double d = r.nextDouble() * reach, side = (r.nextDouble() - 0.5) * d * 1.6;
                    Vec3 q = at.add(dir.scale(d)).add(new Vec3(-dir.z, 0, dir.x).scale(side));
                    level.addParticle(AllParticles.GRACE.get(), q.x, q.y + 0.2 + r.nextDouble() * 1.6, q.z, dir.x * 0.1, 0.02, dir.z * 0.1);
                }
            }
            case UrielFxPayload.FLASH -> {
                MagicOverlay.flash(at, 0xFFF6DC, 0.9f, p.arg() / 10f + 4);
                burst(level, at.add(0, 1.2, 0), AllParticles.GRACE.get(), 60, 0.3);
            }
            case UrielFxPayload.WINGS -> burst(level, at.add(0, 1.6, 0), AllParticles.GRACE.get(), 50, 0.25);
            case UrielFxPayload.DEATH -> {
                MagicOverlay.flash(at, 0xFFFFFF, 0.6f, 32);
                burst(level, at.add(0, 1.2, 0), ParticleTypes.END_ROD, 40, 0.2);
            }
            default -> {
            }
        }
    }

    private static void ui(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, 0.9f));
    }

    private static void burst(ClientLevel level, Vec3 at, net.minecraft.core.particles.ParticleOptions p, int n, double speed) {
        RandomSource r = level.random;
        for (int i = 0; i < n; i++) {
            level.addParticle(p, at.x + (r.nextDouble() - 0.5), at.y + r.nextDouble(), at.z + (r.nextDouble() - 0.5),
                    (r.nextDouble() - 0.5) * speed, r.nextDouble() * speed, (r.nextDouble() - 0.5) * speed);
        }
    }
}
