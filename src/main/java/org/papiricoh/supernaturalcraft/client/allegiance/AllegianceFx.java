package org.papiricoh.supernaturalcraft.client.allegiance;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.allegiance.power.Power;
import org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector;
import org.papiricoh.supernaturalcraft.network.AllegianceFxPayload;
import org.papiricoh.supernaturalcraft.network.CameraSequencePayload;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Every {@link AllegianceFxPayload} played out on the client: the ascension (camera sequence {@code ascension_<side>} on
 * the player, then the rank's title card), a cast's sound and flash, a refusal on the HUD, the true form's light, the
 * Angel Radio's marks, an exorcism tearing the smoke or light out, powers silenced or given back, a villager's pact,
 * and the whispers.
 */
public final class AllegianceFx {

    /** {@code WHISPER}'s {@code arg} indexes this ({@code hud.supernaturalcraft.allegiance.whisper.<id>}); append only. */
    public static final List<String> MESSAGES = List.of(
            "bloodlust",        // 0: the Mark thirsts (no kill for a while)
            "bloodlust_feeds",  // 1: it starts feeding on your health
            "starving",         // 2: no Grace/Corruption left
            "prayer",           // 3: Grace gathers while you pray on consecrated ground
            "consecrated",      // 4: a demon on consecrated ground: it burns
            "sense",            // 5: a Veteran senses the supernatural nearby
            "messenger_gone",   // 6: Heaven's messenger leaves
            "pact_refused",     // 7: this villager has already made a pact
            "sworn_blade",      // 8: a blade made for your kind cuts deep
            "trapped_oil",      // 9: an angel inside a ring of holy fire
            "fed"               // 10: a kill slakes the Mark
    );

    /** Ticks an ascension's camera sequence lasts (the card follows it). */
    public static final int ASCENSION_TICKS = 100;
    /** The local player's true form light: entity id → client tick it ends. */
    private static final Map<Integer, Long> TRUE_FORM = new HashMap<>();
    private static long ticks;
    private static int flash, flashColour, flashLength = 1;

    private AllegianceFx() {
    }

    public static void handle(AllegianceFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;
        Entity e = level.getEntity(p.entity());
        Vec3 at = p.point() != null && !p.point().equals(Vec3.ZERO) ? p.point() : e != null ? e.position() : mc.player.position();
        switch (p.kind()) {
            case AllegianceFxPayload.ASCENSION -> ascension(mc, e, Faction.byOrdinal(p.arg()), p.arg2(), at);
            case AllegianceFxPayload.CAST -> cast(mc, e, Power.byOrdinal(p.arg()), at);
            case AllegianceFxPayload.DENIED -> ClientPowers.denied(p.arg());
            case AllegianceFxPayload.TRUE_FORM -> trueForm(mc, e, p.duration());
            case AllegianceFxPayload.RADIO_PING -> {
                AllegianceHud.ping(at, p.arg(), p.duration());
                if (ticks % 2 == 0) sound(at, AllSounds.ALLEGIANCE_RADIO.get(), 0.25f, 1.3f + level.random.nextFloat() * 0.3f, true);
            }
            case AllegianceFxPayload.EXPELLED -> expelled(mc, e, at, p.arg() == 1);
            case AllegianceFxPayload.SUPPRESSED -> {
                boolean on = p.arg() != 0;
                AllegianceHud.whisper(Component.translatable("hud.supernaturalcraft.allegiance." + (on ? "suppressed" : "restored")),
                        on ? 0xD8D0C0 : 0xF4D77E);
                if (on) {
                    AllegianceHud.suppressedFlash();
                    flash(0xFFFFFF, 10);
                }
            }
            case AllegianceFxPayload.PACT -> pact(level, at);
            case AllegianceFxPayload.WHISPER -> {
                String id = p.arg() >= 0 && p.arg() < MESSAGES.size() ? MESSAGES.get(p.arg()) : "unknown";
                AllegianceHud.whisper(Component.translatable("hud.supernaturalcraft.allegiance.whisper." + id), 0xD8B8A8);
            }
            default -> SupernaturalCraft.LOGGER.debug("Unknown allegiance fx {}", p.kind());
        }
    }

    // --- ascension ---------------------------------------------------------------------------------------------------

    private static void ascension(Minecraft mc, Entity e, Faction f, int rank, Vec3 at) {
        if (e == null) return;
        Faction shown = rank <= 0 ? Faction.HUMAN : f;
        SoundEvent s = switch (shown) {
            case ANGEL -> AllSounds.ALLEGIANCE_ASCEND_ANGEL.get();
            case DEMON -> AllSounds.ALLEGIANCE_ASCEND_DEMON.get();
            case HUMAN -> AllSounds.ALLEGIANCE_ASCEND_HUNTER.get();
        };
        sound(e.position(), s, 1.2f, 1f, false);
        burst(mc.level, e.position().add(0, 1, 0), shown, 60);
        if (e != mc.player) return;
        ClientPowers.reset();
        String id = "ascension_" + AllegianceGui.key(shown);
        // The camera sequence (if cinematics are on), the letterbox and a flash; the title card waits for the camera.
        CameraDirector.play(new CameraSequencePayload(SupernaturalCraft.asResource(id), e.getId(), e.position(), e.getYRot(), ASCENSION_TICKS));
        int flashRgb = switch (shown) {
            case ANGEL -> 0xFFF6D8;
            case DEMON -> 0x5A0A0A;
            case HUMAN -> 0xE8DCC0;
        };
        org.papiricoh.supernaturalcraft.client.cinematic.ClientCinematics.play(new CinematicPayload(ASCENSION_TICKS, shown == Faction.DEMON ? 0.4f : 0.15f,
                flashRgb, 0.5f, true, "", ""));
        TitleCard.show(shown, rank, CameraDirector.active() ? ASCENSION_TICKS - 30 : 0);
    }

    // --- casts -------------------------------------------------------------------------------------------------------

    private static void cast(Minecraft mc, Entity e, Power power, Vec3 at) {
        if (power == null) return;
        if (e == mc.player) ClientPowers.confirmed(power);
        ClientLevel level = mc.level;
        Vec3 from = e != null ? e.position().add(0, e.getBbHeight() * 0.6, 0) : at;
        switch (power) {
            case TELEPORT -> {
                sound(at, AllSounds.ALLEGIANCE_TELEPORT.get(), 1, 1, false);
                feathers(level, from, 14);
                feathers(level, at.add(0, 1, 0), 14);
            }
            case HEALING_TOUCH -> {
                sound(at, AllSounds.ALLEGIANCE_HEAL.get(), 1, 1, false);
                spray(level, at.add(0, 1, 0), AllParticles.GRACE.get(), 24, 0.5, 0.02);
            }
            case ANGEL_BLADE -> {
                sound(from, AllSounds.ALLEGIANCE_TELEPORT.get(), 0.6f, 1.6f, false);
                spray(level, from, AllParticles.GRACE.get(), 10, 0.3, 0.02);
            }
            case ANGEL_RADIO -> sound(from, AllSounds.ALLEGIANCE_RADIO.get(), 0.8f, 1, false);
            case SMITE -> {
                sound(at, AllSounds.ALLEGIANCE_SMITE.get(), 1.2f, 1, false);
                spray(level, at.add(0, 1.2, 0), AllParticles.WHITE_LIGHT.get(), 30, 0.4, 0.05);
                if (near(mc, at, 8)) flash(0xFFF4D0, 8);
            }
            case TRUE_FORM -> sound(from, AllSounds.ALLEGIANCE_TRUE_FORM.get(), 1.5f, 1, false);
            case HOST_SQUAD -> {
                sound(from, AllSounds.ALLEGIANCE_TRUE_FORM.get(), 1, 1.4f, false);
                spray(level, at.add(0, 1, 0), AllParticles.WHITE_LIGHT.get(), 40, 2.5, 0.04);
            }
            case LIGHT_LANCE -> {
                sound(from, AllSounds.ALLEGIANCE_LANCE.get(), 1, 1, false);
                trail(level, from, at, AllParticles.GRACE.get());
            }
            case SMOKE -> {
                sound(from, AllSounds.ALLEGIANCE_SMOKE.get(), 1, 1, false);
                trail(level, from, at.add(0, 1, 0), AllParticles.DEMON_SMOKE.get());
                spray(level, from, ParticleTypes.LARGE_SMOKE, 24, 0.5, 0.05);
            }
            case SUMMON_HOUND -> {
                sound(at, AllSounds.ALLEGIANCE_SMOKE.get(), 0.8f, 0.6f, false);
                spray(level, at.add(0, 0.5, 0), AllParticles.HELLFIRE.get(), 30, 0.6, 0.04);
            }
            case TELEKINESIS -> {
                sound(at, AllSounds.ALLEGIANCE_TELEKINESIS.get(), 1, 1, false);
                spray(level, at.add(0, 1, 0), new DustParticleOptions(new Vector3f(0.55f, 0.2f, 0.8f), 1.2f), 20, 0.5, 0.02);
            }
            case POSSESS -> {
                sound(at, AllSounds.ALLEGIANCE_SMOKE.get(), 1, 0.7f, false);
                trail(level, from, at.add(0, 1, 0), AllParticles.DEMON_SMOKE.get());
            }
            case THRONE -> {
                sound(from, AllSounds.ALLEGIANCE_THRONE.get(), 1.5f, 1, false);
                ring(level, from.add(0, -0.4, 0), power.range, AllParticles.HELLFIRE.get());
                if (near(mc, from, power.range)) flash(0x3A0606, 14);
            }
            default -> spray(level, at.add(0, 1, 0), AllParticles.GRACE.get(), 10, 0.4, 0.02);
        }
    }

    private static void trueForm(Minecraft mc, Entity e, int duration) {
        if (e == null) return;
        TRUE_FORM.put(e.getId(), ticks + Math.max(20, duration));
        sound(e.position(), AllSounds.ALLEGIANCE_TRUE_FORM.get(), 1.6f, 1, false);
    }

    /** How bright the true form of {@code e} burns now (0 when not), in and out over half a second. */
    public static float trueFormLight(Entity e, float partial) {
        Long until = TRUE_FORM.get(e.getId());
        boolean flagged = e instanceof Player p && ClientAllegiance.flag(p, org.papiricoh.supernaturalcraft.allegiance.Allegiances.TRUE_FORM);
        if (until == null && !flagged) return 0;
        float left = until == null ? 20 : until - ticks - partial;
        return Math.max(0, Math.min(1, left / 10f));
    }

    /** {@code at} is the eyes; {@code banished} (an angel sent away by the sigil) pours light, an exorcism smoke. */
    private static void expelled(Minecraft mc, Entity e, Vec3 at, boolean banished) {
        boolean angel = banished || e != null && Kin.isAngel(e);
        sound(at, AllSounds.ALLEGIANCE_EXPEL.get(), 1.4f, angel ? 1.3f : 0.8f, false);
        ClientLevel level = mc.level;
        RandomSource r = level.random;
        for (int i = 0; i < 70; i++) {
            ParticleOptions o = angel ? AllParticles.WHITE_LIGHT.get() : i % 3 == 0 ? ParticleTypes.LARGE_SMOKE : AllParticles.DEMON_SMOKE.get();
            level.addParticle(o, at.x + r.nextGaussian() * 0.2, at.y + r.nextDouble() * 0.3, at.z + r.nextGaussian() * 0.2,
                    r.nextGaussian() * 0.08, 0.25 + r.nextDouble() * 0.35, r.nextGaussian() * 0.08);
        }
        if (e == mc.player) {
            flash(angel ? 0xFFFFFF : 0x200000, 20);
            AllegianceHud.whisper(Component.translatable("hud.supernaturalcraft.allegiance.expelled"), 0xE05050);
        }
    }

    private static void pact(ClientLevel level, Vec3 at) {
        sound(at, AllSounds.ALLEGIANCE_SMOKE.get(), 0.7f, 1.4f, false);
        spray(level, at.add(0, 1.4, 0), ParticleTypes.HAPPY_VILLAGER, 10, 0.4, 0.02);
        spray(level, at.add(0, 1, 0), AllParticles.DEMON_SMOKE.get(), 16, 0.3, 0.03);
    }

    // --- screen flash (drawn by TitleCard's overlay) -----------------------------------------------------------------

    static void flash(int rgb, int length) {
        flashColour = rgb;
        flash = flashLength = Math.max(1, length);
    }

    /** 0..1 strength of the flash now, and its colour. */
    static float flashStrength(float partial) {
        return flash <= 0 ? 0 : Math.max(0, (flash - partial) / flashLength);
    }

    static int flashColour() {
        return flashColour;
    }

    static void tick() {
        ticks++;
        if (flash > 0) flash--;
        TRUE_FORM.values().removeIf(until -> until < ticks);
    }

    static void clear() {
        TRUE_FORM.clear();
        flash = 0;
    }

    /** How much of the local player's view a true form floods (looking toward it, near). */
    static float trueFormGlare(Minecraft mc, float partial) {
        if (mc.level == null || mc.player == null) return 0;
        float best = 0;
        Vec3 eye = mc.player.getEyePosition(partial), look = mc.player.getViewVector(partial);
        for (var entry : TRUE_FORM.entrySet()) {
            Entity e = mc.level.getEntity(entry.getKey());
            if (e == null) continue;
            float light = trueFormLight(e, partial);
            if (e == mc.player) {
                best = Math.max(best, light * 0.18f);
                continue;
            }
            Vec3 to = e.position().add(0, 1.2, 0).subtract(eye);
            double d = to.length();
            if (d > 32) continue;
            double facing = Math.max(0, look.dot(to.normalize()));
            best = Math.max(best, (float) (light * facing * facing * (1 - d / 32)));
        }
        return best;
    }

    // --- particles and sounds ----------------------------------------------------------------------------------------

    private static boolean near(Minecraft mc, Vec3 at, double r) {
        return mc.player != null && mc.player.position().distanceToSqr(at) < r * r;
    }

    private static void sound(Vec3 at, SoundEvent s, float volume, float pitch, boolean quiet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        mc.level.playLocalSound(at.x, at.y, at.z, s, SoundSource.PLAYERS, quiet ? volume * 0.6f : volume, pitch, false);
    }

    static void spray(ClientLevel level, Vec3 at, ParticleOptions o, int n, double spread, double speed) {
        RandomSource r = level.random;
        for (int i = 0; i < n; i++) {
            level.addParticle(o, at.x + r.nextGaussian() * spread, at.y + r.nextGaussian() * spread * 0.6, at.z + r.nextGaussian() * spread,
                    r.nextGaussian() * speed, r.nextGaussian() * speed + speed, r.nextGaussian() * speed);
        }
    }

    private static void trail(ClientLevel level, Vec3 from, Vec3 to, ParticleOptions o) {
        Vec3 d = to.subtract(from);
        int n = (int) Math.min(80, d.length() * 3);
        for (int i = 0; i <= n; i++) {
            Vec3 p = from.add(d.scale(i / (double) Math.max(1, n)));
            level.addParticle(o, p.x, p.y, p.z, 0, 0.01, 0);
        }
    }

    private static void ring(ClientLevel level, Vec3 c, double r, ParticleOptions o) {
        for (int i = 0; i < 72; i++) {
            double a = i * Math.PI * 2 / 72;
            level.addParticle(o, c.x + Math.sin(a) * r, c.y, c.z + Math.cos(a) * r, 0, 0.05, 0);
        }
    }

    private static void feathers(ClientLevel level, Vec3 at, int n) {
        spray(level, at, ParticleTypes.WHITE_ASH, n, 0.4, 0.03);
        spray(level, at, AllParticles.GRACE.get(), n / 2, 0.3, 0.02);
    }

    static void burst(ClientLevel level, Vec3 at, Faction f, int n) {
        ParticleOptions o = switch (f) {
            case ANGEL -> AllParticles.GRACE.get();
            case DEMON -> AllParticles.DEMON_SMOKE.get();
            case HUMAN -> ParticleTypes.ENCHANT;
        };
        spray(level, at, o, n, 0.6, 0.06);
        if (f == Faction.DEMON) spray(level, at, AllParticles.HELLFIRE.get(), n / 2, 0.5, 0.05);
        if (f == Faction.ANGEL) spray(level, at, AllParticles.WHITE_LIGHT.get(), n / 3, 0.5, 0.05);
    }
}
