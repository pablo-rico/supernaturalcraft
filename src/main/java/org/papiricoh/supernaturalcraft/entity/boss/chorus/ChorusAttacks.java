package org.papiricoh.supernaturalcraft.entity.boss.chorus;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTerrain;
import org.papiricoh.supernaturalcraft.chorus.ChoirBellBlock;
import org.papiricoh.supernaturalcraft.chorus.Melody;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.BossStrike;
import org.papiricoh.supernaturalcraft.entity.hazard.FlameTrail;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.network.ChorusFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The Broken Chorus's attacks. Each face is a voice with its own family of attacks (and its own
 * note): break a face and its family falls silent. Telegraph colours follow the house rules:
 * gold for holy light, violet for lightning, red for fire, white for wind, green where it is safe.
 *
 * <pre>
 * P1 faces:  Litany (man), Verdict (man), Lion's Roar, Lion's Pounce, Ox's Trample, Eagle's Stormcall
 * P2 wings:  Wing Gale, Feather Storm, Seraph Dive, Veil of Wings
 * P3 eyes:   Wheel within Wheel, Thousand Eyes, Rolling Wheel, Lightning Gyre
 * Final:     Last Light, Requiem
 * All:       the Hymn — forced every so often; ring the bell it sings to break it
 * </pre>
 */
public final class ChorusAttacks {

    /** Echoes alive at once, at most. */
    public static final int MAX_ECHOES = 6;
    public static final int MAN = 0, LION = 1, OX = 2, EAGLE = 3;
    /** The note each face sings with (its family's audio telegraph). */
    private static final float[] VOICE = {0.8f, 0.55f, 0.45f, 1.3f};

    private ChorusAttacks() {
    }

    private static AttackScheduler.Option<ChorusEntity> opt(Supplier<BossAttack<ChorusEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<ChorusEntity>> P1 = List.of(
            opt(Litany::new, 2), opt(Verdict::new, 2), opt(LionRoar::new, 2), opt(LionPounce::new, 1.5f), opt(OxTrample::new, 2),
            opt(EagleStormcall::new, 2));
    private static final List<AttackScheduler.Option<ChorusEntity>> P2 = List.of(
            opt(WingGale::new, 2), opt(FeatherStorm::new, 2), opt(SeraphDive::new, 2), opt(VeilOfWings::new, 1.5f), opt(Verdict::new, 1));
    private static final List<AttackScheduler.Option<ChorusEntity>> P3 = List.of(
            opt(WheelWithinWheel::new, 2), opt(ThousandEyes::new, 2), opt(RollingWheel::new, 2), opt(LightningGyre::new, 2));
    private static final List<AttackScheduler.Option<ChorusEntity>> P4 = List.of(
            opt(LastLight::new, 3), opt(Requiem::new, 2), opt(LightningGyre::new, 1));

    public static List<AttackScheduler.Option<ChorusEntity>> pool(ChorusEntity boss) {
        return switch (boss.phase()) {
            case 1 -> P1;
            case 2 -> P2;
            case 3 -> P3;
            default -> P4;
        };
    }

    public static @Nullable Supplier<BossAttack<ChorusEntity>> forced(ChorusEntity boss) {
        return boss.hymnDue() ? TheHymn::new : null;
    }

    // --- helpers --------------------------------------------------------------------------

    static ServerLevel level(ChorusEntity boss) {
        return (ServerLevel) boss.level();
    }

    static boolean faceAlive(ChorusEntity boss, int face) {
        return boss.partAlive(ChorusEntity.FIRST_FACE + face);
    }

    /** One of its blows: holy judgment, scaled by the curve and the config and deepened by every echo still singing. */
    static boolean hit(ChorusEntity boss, LivingEntity victim, float amount) {
        if (victim instanceof ChorusEntity || victim instanceof ChoirEchoEntity) return false;
        float k = boss.attackDamageMultiplier() * (1 + 0.15f * Math.min(3, boss.harmony()));
        return BossStrike.deal(boss, victim, AllDamageTypes.JUDGMENT, amount * k);
    }

    static List<LivingEntity> victims(ChorusEntity boss, AABB box) {
        return level(boss).getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !(e instanceof ChorusEntity)
                && !(e instanceof ChoirEchoEntity) && !(e instanceof Player p && (p.isCreative() || p.isSpectator())));
    }

    /** Everyone it fights: its challengers, or anyone living near it when it has none (tests). */
    static List<LivingEntity> targets(ChorusEntity boss) {
        List<LivingEntity> out = new ArrayList<>(boss.challengers());
        if (out.isEmpty()) out.addAll(victims(boss, boss.getBoundingBox().inflate(32, 16, 32)));
        return out;
    }

    static Vec3 core(ChorusEntity boss) {
        return boss.position().add(ChorusGeometry.coreOffset());
    }

    static Vec3 facePos(ChorusEntity boss, int face) {
        return boss.position().add(ChorusGeometry.faceOffset(face, boss.heading()));
    }

    static Vec3 eyePos(ChorusEntity boss, int eye) {
        return boss.position().add(ChorusGeometry.eyeOffset(eye, boss.wheelRot(ChorusGeometry.wheelOf(eye), boss.time(0))));
    }

    /** The arena's floor level, where its ground attacks land. */
    static double floorY(ChorusEntity boss) {
        ArenaController arena = boss.arena();
        return arena != null ? arena.center().getY() : boss.getY();
    }

    static Vec3 centre(ChorusEntity boss) {
        ArenaController arena = boss.arena();
        return arena != null ? arena.centerVec() : new Vec3(boss.getX(), floorY(boss), boss.getZ());
    }

    static Vec3 ground(ChorusEntity boss, Vec3 p) {
        return new Vec3(p.x, floorY(boss), p.z);
    }

    static float floorRadius(ChorusEntity boss) {
        ArenaController arena = boss.arena();
        return arena != null ? arena.floorRadius() : 18;
    }

    static float yawTo(Vec3 from, Vec3 to) {
        return (float) (Mth.atan2(to.z - from.z, to.x - from.x) * Mth.RAD_TO_DEG) - 90f;
    }

    static Vec3 dir(float yaw) {
        double r = Math.toRadians(yaw);
        return new Vec3(-Math.sin(r), 0, Math.cos(r));
    }

    static double flat(Vec3 a, Vec3 b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Horizontal distance from p to the segment a-b. */
    static double toSegment(Vec3 p, Vec3 a, Vec3 b) {
        double abx = b.x - a.x, abz = b.z - a.z, len2 = abx * abx + abz * abz;
        double k = len2 < 1e-6 ? 0 : Mth.clamp(((p.x - a.x) * abx + (p.z - a.z) * abz) / len2, 0, 1);
        double dx = p.x - (a.x + abx * k), dz = p.z - (a.z + abz * k);
        return Math.sqrt(dx * dx + dz * dz);
    }

    static void sound(ChorusEntity boss, SoundEvent e, float volume, float pitch) {
        boss.level().playSound(null, boss.getX(), core(boss).y, boss.getZ(), e, SoundSource.HOSTILE, volume, pitch);
    }

    /** A face sings its family's note. */
    static void voice(ChorusEntity boss, int face) {
        sound(boss, AllSounds.CHORUS_SING.get(), 5.0f, VOICE[face]);
        sound(boss, SoundEvents.BELL_RESONATE, 3.0f, VOICE[face] * 1.2f);
    }

    static Vec3 randomFloorPoint(ChorusEntity boss, double minR) {
        Vec3 c = centre(boss);
        double r = Math.max(minR, floorRadius(boss) - 1.5);
        double a = boss.getRandom().nextDouble() * Math.PI * 2, d = minR + boss.getRandom().nextDouble() * (r - minR);
        return new Vec3(c.x + Math.cos(a) * d, c.y, c.z + Math.sin(a) * d);
    }

    /** A bolt from the storm: harmless to look at, it hurts what stands under it. */
    static void bolt(ChorusEntity boss, Vec3 at, float radius, float damage) {
        ServerLevel level = level(boss);
        LightningBolt b = EntityType.LIGHTNING_BOLT.create(level);
        if (b != null) {
            b.moveTo(at.x, at.y, at.z);
            b.setVisualOnly(true);
            level.addFreshEntity(b);
        }
        for (LivingEntity e : victims(boss, new AABB(at, at).inflate(radius, 3, radius))) {
            if (flat(e.position(), at) <= radius) hit(boss, e, damage);
        }
    }

    // --- the gaze and the light -------------------------------------------------------------

    /** Opens the eye with the clearest view of {@code target} (or any eye) and returns it. */
    static int bestEye(ChorusEntity boss, LivingEntity target, Set<Integer> taken) {
        int best = -1;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < ChorusGeometry.EYES; i++) {
            if (taken.contains(i) || !boss.partAlive(ChorusEntity.FIRST_EYE + i)) continue;
            Vec3 e = eyePos(boss, i);
            double d = e.distanceToSqr(target.getEyePosition()) + (ChorusLight.sees(boss, e, target) ? 0 : 400);
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    /** An eye's judgment falls on everyone: full on those who meet its gaze, a burn on those it only sees. */
    static void judge(ChorusEntity boss, int eye) {
        Vec3 from = eyePos(boss, eye);
        ServerLevel level = level(boss);
        level.sendParticles(ParticleTypes.FLASH, from.x, from.y, from.z, 1, 0, 0, 0, 0);
        sound(boss, AllSounds.CHORUS_JUDGMENT.get(), 4.0f, 1.0f);
        for (LivingEntity e : targets(boss)) {
            ChorusBalance.Gaze verdict = ChorusGaze.verdict(boss, from, e);
            if (verdict == ChorusBalance.Gaze.FULL) {
                if (hit(boss, e, 12)) e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                ChorusFx.beam(boss, from, e.getEyePosition(), 0.7f, 8, 0xFFFFFF);
            } else if (verdict == ChorusBalance.Gaze.BURN) {
                if (hit(boss, e, 3)) e.igniteForSeconds(2);
                ChorusFx.beam(boss, from, e.getEyePosition(), 0.25f, 8, 0xFFC860);
            }
        }
    }

    // --- faces, echoes, bells ----------------------------------------------------------------

    /** A face falls silent and its voice escapes as two Echoes. */
    public static void faceBroken(ChorusEntity boss, int face, Vec3 at) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        int alive = echoes(boss).size();
        for (int i = 0; i < 2 && alive + i < MAX_ECHOES; i++) {
            ChoirEchoEntity e = AllEntities.CHOIR_ECHO.get().create(level);
            if (e == null) continue;
            e.moveTo(at.x + (i == 0 ? 1 : -1), at.y, at.z, 0, 0);
            e.bind(boss);
            level.addFreshEntity(e);
        }
        boss.setHarmony(echoes(boss).size());
        sound(boss, SoundEvents.AMETHYST_CLUSTER_BREAK, 5.0f, VOICE[face]);
    }

    public static List<ChoirEchoEntity> echoes(ChorusEntity boss) {
        return boss.level().getEntitiesOfClass(ChoirEchoEntity.class, boss.getBoundingBox().inflate(48),
                e -> e.isAlive() && e.chorus() == boss);
    }

    public static void unmakeEchoes(ChorusEntity boss) {
        for (ChoirEchoEntity e : echoes(boss)) e.unmake();
        boss.setHarmony(0);
    }

    /** A Choir Bell sounded near a Chorus: in a Hymn, the right note brings it to its knees. */
    public static void onBellRung(ServerLevel level, BlockPos bell, int note, @Nullable Player ringer) {
        for (ChorusEntity c : level.getEntitiesOfClass(ChorusEntity.class, new AABB(bell).inflate(64))) {
            if (c.bells().contains(bell) || c.bells().isEmpty()) Hymn.onBell(c, note, ringer);
        }
    }

    // =========================================================================================
    // The Hymn
    // =========================================================================================

    /**
     * Three notes, forty ticks each: the halo takes each note's colour and its bell glows. Ring that
     * bell while the note holds and the choir falls silent and kneels. A wrong bell jars (and hurts
     * the ringer). If the Hymn is sung to the end, its light unmakes everyone not sheltering in the
     * shadow of a pillar.
     */
    public static class TheHymn extends BossAttack<ChorusEntity> {
        public static final int NOTE_TICKS = 40;
        final int[] notes = new int[Melody.LENGTH];
        int current = -1;
        int noteEnds;
        boolean broken, finished;
        final Set<BlockPos> lit = new HashSet<>();

        public TheHymn() {
            super("the_hymn", "hymn", 30, NOTE_TICKS * Melody.LENGTH, 30);
        }

        @Override
        public void onWindup(ChorusEntity boss, LivingEntity target) {
            boss.hymnSung();
            for (int i = 0; i < notes.length; i++) {
                int n;
                do {
                    n = boss.getRandom().nextInt(Melody.NOTES);
                } while (i > 0 && n == notes[i - 1]);
                notes[i] = n;
            }
            sound(boss, AllSounds.CHORUS_HYMN.get(), 6.0f, 1.0f);
            for (ServerPlayer p : boss.challengers()) {
                p.displayClientMessage(Component.translatable("message.supernaturalcraft.chorus.hymn").withStyle(ChatFormatting.GOLD), true);
            }
        }

        @Override
        public void tickActive(ChorusEntity boss, LivingEntity target, int t) {
            int idx = Math.min(Melody.LENGTH - 1, (t - 1) / NOTE_TICKS);
            if (idx != current) sing(boss, idx);
            if (t >= active && !broken && !finished) {
                finished = true;
                unlight(boss);
                boss.setHymnNote(-1);
                unmaking(boss);
            }
        }

        public int note() {
            return current < 0 ? -1 : notes[current];
        }

        public boolean broken() {
            return broken;
        }

        private void sing(ChorusEntity boss, int idx) {
            current = idx;
            int note = notes[idx];
            boss.setHymnNote(note);
            noteEnds = (int) (boss.level().getGameTime() + NOTE_TICKS);
            unlight(boss);
            float pitch = Melody.pitch(note);
            sound(boss, SoundEvents.BELL_BLOCK, 6.0f, pitch);
            sound(boss, SoundEvents.AMETHYST_BLOCK_RESONATE, 6.0f, pitch);
            for (BlockPos b : boss.bells()) {
                BlockState s = boss.level().getBlockState(b);
                if (s.getBlock() instanceof ChoirBellBlock bell && bell.note == note) {
                    boss.level().setBlock(b, s.setValue(ChoirBellBlock.LIT, true), 3);
                    lit.add(b);
                    ChorusFx.send(boss, ChorusFxPayload.NOTE, note, -1, Vec3.atCenterOf(b).add(0, 0.5, 0), Vec3.ZERO, 0, NOTE_TICKS,
                            Melody.RGB[note]);
                }
            }
        }

        /** A bell was struck while it sang. */
        public void bell(ChorusEntity boss, int note, @Nullable Player ringer) {
            if (broken || finished || current < 0) return;
            if (note == notes[current]) {
                broken = true;
                boss.kneelSoon();
                sound(boss, SoundEvents.GLASS_BREAK, 6.0f, 0.5f);
                if (ringer instanceof ServerPlayer sp) {
                    org.papiricoh.supernaturalcraft.reward.ChorusRewards.hymnBroken(sp);
                }
                return;
            }
            sound(boss, SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), 4.0f, 0.5f);
            if (ringer != null) {
                ringer.hurt(AllDamageTypes.source(boss.level(), AllDamageTypes.HYMN, boss), 2);
                ringer.displayClientMessage(Component.translatable("message.supernaturalcraft.choir_altar.discord")
                        .withStyle(ChatFormatting.DARK_RED), true);
            }
        }

        @Override
        public boolean endEarly(ChorusEntity boss, LivingEntity target) {
            return broken;
        }

        @Override
        public void onEnd(ChorusEntity boss) {
            unlight(boss);
            boss.setHymnNote(-1);
            if (broken && !boss.kneeling()) boss.kneelSoon();
        }

        /** The Hymn completed: light from the halo falls on everyone it can see. */
        public static void unmaking(ChorusEntity boss) {
            unmaking(boss, targets(boss));
        }

        public static void unmaking(ChorusEntity boss, List<LivingEntity> targets) {
            Vec3 halo = boss.position().add(0, 100 / ChorusGeometry.PX, 0);
            ChorusFx.send(boss, ChorusFxPayload.PULSE, 0, -1, ground(boss, boss.position()), Vec3.ZERO, 30, 20, 0xFFF4C8);
            sound(boss, SoundEvents.WITHER_SPAWN, 6.0f, 1.8f);
            float dmg = ChorusBalance.hymnDamage(boss.harmony());
            for (LivingEntity e : targets) {
                if (ChorusLight.inShadow(boss, halo, e)) continue;
                if (BossStrike.deal(boss, e, AllDamageTypes.HYMN, dmg * boss.attackDamageMultiplier())) {
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                }
            }
        }

        private void unlight(ChorusEntity boss) {
            for (BlockPos b : lit) {
                BlockState s = boss.level().getBlockState(b);
                if (s.getBlock() instanceof ChoirBellBlock) boss.level().setBlock(b, s.setValue(ChoirBellBlock.LIT, false), 3);
            }
            lit.clear();
        }

        @Override
        public float weight(ChorusEntity boss, LivingEntity target) {
            return 0;
        }
    }

    // =========================================================================================
    // Phase 1: the faces
    // =========================================================================================

    /** Its man's face sings three notes: low, middle, high — and light falls on the inner, middle or outer band. */
    public static class Litany extends BossAttack<ChorusEntity> {
        final int[] bands = new int[3];
        static final float[] PITCH = {0.5f, 0.8f, 1.2f};

        public Litany() {
            super("litany", "sing_man", 20, 80, 30);
        }

        @Override
        public float weight(ChorusEntity boss, LivingEntity target) {
            return faceAlive(boss, MAN) ? 1 : 0;
        }

        static float[] band(ChorusEntity boss, int b) {
            float r = floorRadius(boss);
            float w = r / 3;
            return new float[]{b * w, (b + 1) * w};
        }

        @Override
        public void onWindup(ChorusEntity boss, LivingEntity target) {
            for (int i = 0; i < 3; i++) bands[i] = boss.getRandom().nextInt(3);
        }

        @Override
        public void tickActive(ChorusEntity boss, LivingEntity target, int t) {
            for (int i = 0; i < 3; i++) {
                if (t == 1 + i * 22) {
                    sound(boss, SoundEvents.BELL_RESONATE, 6.0f, PITCH[bands[i]]);
                    voice(boss, MAN);
                    float[] b = band(boss, bands[i]);
                    Vec3 c = centre(boss);
                    TelegraphMarker.ring(level(boss), c, b[1], TelegraphMarker.GOLD, 22);
                    if (b[0] > 0.5f) TelegraphMarker.ring(level(boss), c, b[0], TelegraphMarker.GOLD, 22);
                }
                if (t == 22 + i * 22) strike(boss, bands[i]);
            }
        }

        static void strike(ChorusEntity boss, int bandIndex) {
            float[] b = band(boss, bandIndex);
            Vec3 c = centre(boss);
            ChorusFx.ring(boss, c.add(0, 0.1, 0), b[1], 10, 0xFFE38A);
            for (LivingEntity e : victims(boss, new AABB(c, c).inflate(b[1] + 1, 4, b[1] + 1))) {
                double d = flat(e.position(), c);
                if (d >= b[0] && d <= b[1]) hit(boss, e, 7);
            }
            ServerLevel level = level(boss);
            for (int k = 0; k < 10; k++) {
                double a = boss.getRandom().nextDouble() * Math.PI * 2, d = b[0] + boss.getRandom().nextDouble() * (b[1] - b[0]);
                level.sendParticles(ParticleTypes.END_ROD, c.x + Math.cos(a) * d, c.y + 0.3, c.z + Math.sin(a) * d, 2, 0.1, 0.6, 0.1, 0.02);
            }
        }
    }

    /** One eye opens on its target and charges; its judgment falls at the end of the charge. */
    public static class Verdict extends BossAttack<ChorusEntity> {
        int eye = -1;

        public Verdict() {
            super("verdict", "sing_man", 40, 1, 25);
        }

        @Override
        public float weight(ChorusEntity boss, LivingEntity target) {
            return boss.phase() == 1 && !faceAlive(boss, MAN) ? 0 : 1;
        }

        @Override
        public void onWindup(ChorusEntity boss, LivingEntity target) {
            eye = bestEye(boss, target, Set.of());
            if (eye < 0) return;
            boss.forceEyes(boss.forcedEyes() | (1 << eye));
            sound(boss, AllSounds.CHORUS_GAZE.get(), 4.0f, 1.0f);
            for (LivingEntity e : targets(boss)) {
                ChorusFx.send(boss, ChorusFxPayload.GAZE, eye, e.getId(), Vec3.ZERO, e.getEyePosition(), 0, 40, 0xFFE38A);
                if (e instanceof ServerPlayer p) {
                    p.displayClientMessage(Component.translatable("message.supernaturalcraft.chorus.gaze").withStyle(ChatFormatting.YELLOW), true);
                }
            }
        }

        @Override
        public void onActive(ChorusEntity boss, LivingEntity target) {
            if (eye >= 0) judge(boss, eye);
        }

        @Override
        public void onEnd(ChorusEntity boss) {
            if (eye >= 0) boss.forceEyes(boss.forcedEyes() & ~(1 << eye));
        }
    }

    /** The lion roars fire across a wide wedge in front of it; the ground burns after. */
    public static class LionRoar extends BossAttack<ChorusEntity> {
        float yaw;
        Vec3 from;

        public LionRoar() {
            super("lion_roar", "sing_lion", 30, 10, 30);
        }

        @Override
        public float weight(ChorusEntity boss, LivingEntity target) {
            return faceAlive(boss, LION) ? 1 : 0;
        }

        @Override
        public void onWindup(ChorusEntity boss, LivingEntity target) {
            from = ground(boss, facePos(boss, LION));
            yaw = yawTo(from, target.position());
            TelegraphMarker.cone(level(boss), from, yaw, 14, TelegraphMarker.RED, windup + 1);
            voice(boss, LION);
        }

        @Override
        public void onActive(ChorusEntity boss, LivingEntity target) {
            sound(boss, SoundEvents.RAVAGER_ROAR, 6.0f, 0.6f);
            sound(boss, SoundEvents.BLAZE_SHOOT, 4.0f, 0.5f);
            Vec3 d = dir(yaw);
            for (LivingEntity e : victims(boss, new AABB(from, from).inflate(14, 5, 14))) {
                Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
                if (to.length() > 14 || to.length() < 0.01 || to.normalize().dot(d) < 0.5) continue;
                if (hit(boss, e, 9)) {
                    e.igniteForSeconds(4);
                    e.knockback(1.0, -to.x, -to.z);
                }
            }
            ServerLevel level = level(boss);
            for (int i = 2; i <= 13; i += 2) {
                Vec3 p = from.add(d.scale(i));
                level.addFreshEntity(new FlameTrail(level, boss, p.x, p.y, p.z));
                level.sendParticles(ParticleTypes.FLAME, p.x, p.y + 0.5, p.z, 12, 1.2, 0.4, 1.2, 0.05);
            }
        }
    }

    /** The lion lunges low along a line; then it rests there a moment, its faces at the floor. */
    public static class LionPounce extends BossAttack<ChorusEntity> {
        Vec3 from, to;

        public LionPounce() {
            super("lion_pounce", "sing_lion", 35, 6, 10);
        }

        @Override
        public float weight(ChorusEntity boss, LivingEntity target) {
            return faceAlive(boss, LION) ? 1 : 0;
        }

        @Override
        public void onWindup(ChorusEntity boss, LivingEntity target) {
            from = ground(boss, boss.position());
            float yaw = yawTo(from, target.position());
            to = from.add(dir(yaw).scale(floorRadius(boss)));
            TelegraphMarker.line(level(boss), from, yaw, 5, floorRadius(boss), TelegraphMarker.VIOLET, windup + 1);
            voice(boss, LION);
        }

        @Override
        public void onActive(ChorusEntity boss, LivingEntity target) {
            sound(boss, SoundEvents.RAVAGER_ATTACK, 6.0f, 0.5f);
            for (LivingEntity e : victims(boss, new AABB(from, to).inflate(3, 4, 3))) {
                if (toSegment(e.position(), from, to) <= 2.5 && hit(boss, e, 10)) {
                    Vec3 push = e.position().subtract(from).multiply(1, 0, 1).normalize();
                    e.setDeltaMovement(push.x * 1.2, 0.5, push.z * 1.2);
                    e.hurtMarked = true;
                }
            }
            ChorusFx.beam(boss, from.add(0, 1, 0), to.add(0, 1, 0), 2.0f, 10, 0xFFB050);
        }

        @Override
        public void onEnd(ChorusEntity boss) {
            if (boss.state() != ChorusEntity.TRANSITION && boss.state() != ChorusEntity.DYING) boss.rest(60);
        }
    }

    /** The ox stamps: three rings race out over the floor (jump them); the last tears pieces from the edge. */
    public static class OxTrample extends BossAttack<ChorusEntity> {
        final Map<UUID, Integer> struck = new HashMap<>();

        public OxTrample() {
            super("ox_trample", "sing_ox", 30, 60, 25);
        }

        @Override
        public float weight(ChorusEntity boss, LivingEntity target) {
            return faceAlive(boss, OX) ? 1 : 0;
        }

        @Override
        public void onWindup(ChorusEntity boss, LivingEntity target) {
            TelegraphMarker.ring(level(boss), centre(boss), floorRadius(boss), TelegraphMarker.WHITE, windup + 1);
            voice(boss, OX);
        }

        @Override
        public void tickActive(ChorusEntity boss, LivingEntity target, int t) {
            Vec3 c = centre(boss);
            float r = floorRadius(boss);
            for (int ring = 0; ring < 3; ring++) {
                int start = ring * 18;
                if (t == start + 1) {
                    sound(boss, SoundEvents.GENERIC_EXPLODE.value(), 4.0f, 0.5f);
                    ChorusFx.ring(boss, c.add(0, 0.1, 0), r, 24, 0xFFFFFF);
                }
                // Each ring sweeps out at r/24 blocks a tick: whoever stands on the ground where it passes is thrown.
                float front = (t - start) * r / 24f;
                if (t <= start || front > r) continue;
                for (LivingEntity e : victims(boss, new AABB(c, c).inflate(r + 1, 3, r + 1))) {
                    double d = flat(e.position(), c);
                    if (Math.abs(d - front) > 1.2 || !e.onGround() || struck.getOrDefault(e.getUUID(), -1) == ring) continue;
                    struck.put(e.getUUID(), ring);
                    if (hit(boss, e, 7)) {
                        e.setDeltaMovement(e.getDeltaMovement().add(0, 0.6, 0));
                        e.hurtMarked = true;
                    }
                }
            }
            if (t == 54) {
                ArenaController arena = boss.arena();
                if (arena != null) {
                    for (int k = 0; k < 3; k++) {
                        double a = boss.getRandom().nextDouble() * Math.PI * 2;
                        BlockPos at = BlockPos.containing(c.x + Math.cos(a) * (r - 1), c.y - 1, c.z + Math.sin(a) * (r - 1));
                        ArenaTerrain.breakChunk(level(boss), arena, at, 2, -1);
                    }
                }
            }
        }
    }

    /** The eagle cries for the storm: lightning falls on each challenger, and a few places besides. */
    public static class EagleStormcall extends BossAttack<ChorusEntity> {
        final List<Vec3> spots = new ArrayList<>();

        public EagleStormcall() {
            super("eagle_stormcall", "sing_eagle", 40, 30, 25);
        }

        @Override
        public float weight(ChorusEntity boss, LivingEntity target) {
            return faceAlive(boss, EAGLE) ? 1 : 0;
        }

        @Override
        public void onWindup(ChorusEntity boss, LivingEntity target) {
            voice(boss, EAGLE);
            sound(boss, SoundEvents.PHANTOM_AMBIENT, 6.0f, 0.5f);
            for (LivingEntity e : targets(boss)) spots.add(ground(boss, e.position()));
            for (int i = 0; i < 2 + boss.harmony(); i++) spots.add(randomFloorPoint(boss, 3));
            for (Vec3 s : spots) TelegraphMarker.circle(level(boss), s, 2.5f, TelegraphMarker.VIOLET, windup + 1);
        }

        @Override
        public void tickActive(ChorusEntity boss, LivingEntity target, int t) {
            int i = (t - 1) / 3;
            if ((t - 1) % 3 == 0 && i < spots.size()) bolt(boss, spots.get(i), 2.5f, 8);
        }
    }

    // =========================================================================================
    // Phase 2: the wings
    // =========================================================================================

    /** A great beat of its wings down a wide corridor: it shoves toward the edge whoever it can see. */
    public static class WingGale extends BossAttack<ChorusEntity> {
        float yaw;
        Vec3 from;

        public WingGale() {
            super("wing_gale", "", 30, 40, 25);
        }

        @Override
        public void onWindup(ChorusEntity boss, LivingEntity target) {
            from = ground(boss, boss.position());
            yaw = yawTo(from, target.position());
            TelegraphMarker.line(level(boss), from, yaw, 12, floorRadius(boss) + 4, TelegraphMarker.WHITE, windup + 1);
            sound(boss, SoundEvents.ENDER_DRAGON_FLAP, 6.0f, 0.5f);
        }

        @Override
        public void tickActive(ChorusEntity boss, LivingEntity target, int t) {
            Vec3 d = dir(yaw);
            Vec3 to = from.add(d.scale(floorRadius(boss) + 4));
            if (t % 8 == 1) sound(boss, SoundEvents.ENDER_DRAGON_FLAP, 6.0f, 0.4f);
            for (LivingEntity e : victims(boss, new AABB(from, to).inflate(7, 5, 7))) {
                if (toSegment(e.position(), from, to) > 6) continue;
                if (!ChorusLight.sees(boss, core(boss), e)) continue;   // behind a pillar: sheltered
                e.setDeltaMovement(e.getDeltaMovement().add(d.x * 0.22, 0.02, d.z * 0.22));
                e.hurtMarked = true;
                if (t % 10 == 0) hit(boss, e, 1);
            }
            if (t % 4 == 0) {
                ServerLevel level = level(boss);
                for (int i = 0; i < 6; i++) {
                    Vec3 p = from.add(d.scale(boss.getRandom().nextDouble() * 20)).add((boss.getRandom().nextDouble() - 0.5) * 10, 1, 0);
                    level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 0, d.x, 0.05, d.z, 0.8);
                }
            }
        }
    }

    /** Burning feathers shed in a storm: gold rings mark where they will fall. */
    public static class FeatherStorm extends BossAttack<ChorusEntity> {
        final List<Vec3> spots = new ArrayList<>();

        public FeatherStorm() {
            super("feather_storm", "", 30, 24, 25);
        }

        @Override
        public void onWindup(ChorusEntity boss, LivingEntity target) {
            for (LivingEntity e : targets(boss)) spots.add(ground(boss, e.position()));
            for (int i = 0; i < 9; i++) spots.add(randomFloorPoint(boss, 2));
            for (Vec3 s : spots) TelegraphMarker.circle(level(boss), s, 1.8f, TelegraphMarker.GOLD, windup + 12);
            sound(boss, SoundEvents.ENDER_DRAGON_FLAP, 5.0f, 0.7f);
        }

        @Override
        public void tickActive(ChorusEntity boss, LivingEntity target, int t) {
            int i = (t - 1) * spots.size() / 20;
            if (i >= spots.size() || (t - 1) * spots.size() / 20 == (t - 2) * spots.size() / 20 && t > 1) return;
            Vec3 s = spots.get(i);
            ServerLevel level = level(boss);
            ChorusFx.beam(boss, s.add(0, 14, 0), s.add(0, 0.1, 0), 0.5f, 8, 0xFFA040);
            level.sendParticles(ParticleTypes.FLAME, s.x, s.y + 0.3, s.z, 16, 0.8, 0.2, 0.8, 0.04);
            level.playSound(null, s.x, s.y, s.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.5f, 0.8f);
            for (LivingEntity e : victims(boss, new AABB(s, s).inflate(1.8, 3, 1.8))) {
                if (flat(e.position(), s) <= 1.8 && hit(boss, e, 6)) e.igniteForSeconds(3);
            }
        }
    }

    /** It dives across the summit along a line, then lands: its wings lie within reach. */
    public static class SeraphDive extends BossAttack<ChorusEntity> {
        Vec3 from, to;
        final Set<UUID> struck = new HashSet<>();

        public SeraphDive() {
            super("seraph_dive", "dive", 40, 24, 5);
        }

        @Override
        public void onWindup(ChorusEntity boss, LivingEntity target) {
            Vec3 c = centre(boss);
            float yaw = yawTo(c, target.position());
            float r = floorRadius(boss) - 2;
            from = c.add(dir(yaw).scale(-r));
            to = c.add(dir(yaw).scale(r));
            TelegraphMarker.line(level(boss), from, yaw, 7, 2 * r, TelegraphMarker.VIOLET, windup + 1);
            sound(boss, SoundEvents.PHANTOM_SWOOP, 6.0f, 0.5f);
        }

        @Override
        public void onActive(ChorusEntity boss, LivingEntity target) {
            boss.swoop(from, to, active);
            sound(boss, SoundEvents.ENDER_DRAGON_GROWL, 5.0f, 1.4f);
        }

        @Override
        public void tickActive(ChorusEntity boss, LivingEntity target, int t) {
            Vec3 at = ground(boss, boss.position());
            for (LivingEntity e : victims(boss, new AABB(at, at).inflate(4, 6, 4))) {
                if (flat(e.position(), at) <= 3.5 && struck.add(e.getUUID()) && hit(boss, e, 11)) {
                    Vec3 push = e.position().subtract(at).multiply(1, 0, 1).normalize();
                    e.setDeltaMovement(push.x * 1.3, 0.6, push.z * 1.3);
                    e.hurtMarked = true;
                }
            }
        }

        @Override
        public void onEnd(ChorusEntity boss) {
            if (boss.state() != ChorusEntity.TRANSITION && boss.state() != ChorusEntity.DYING) boss.rest(ChorusEntity.REST_TICKS);
        }
    }

    /** The top wings fold over it like a veil: arrows glance off, and only its low wings can be hurt (twice as much). */
    public static class VeilOfWings extends BossAttack<ChorusEntity> {
        public VeilOfWings() {
            super("veil_of_wings", "", 20, 120, 20);
        }

        @Override
        public float weight(ChorusEntity boss, LivingEntity target) {
            // Only while it still has its top wings to fold.
            return boss.partAlive(ChorusEntity.FIRST_WING) || boss.partAlive(ChorusEntity.FIRST_WING + 1) ? 1 : 0;
        }

        @Override
        public void onWindup(ChorusEntity boss, LivingEntity target) {
            boss.setWingPose(ChorusEntity.VEIL);
            sound(boss, SoundEvents.ELYTRA_FLYING, 4.0f, 0.5f);
        }

        @Override
        public void onActive(ChorusEntity boss, LivingEntity target) {
            boss.setVeiled(true);
        }

        @Override
        public void onEnd(ChorusEntity boss) {
            boss.setVeiled(false);
            if (boss.wingPose() == ChorusEntity.VEIL) boss.setWingPose(ChorusEntity.FLIGHT);
            // The veil opens with a flash.
            Vec3 c = ground(boss, boss.position());
            ChorusFx.ring(boss, c.add(0, 0.1, 0), 9, 12, 0xFFF4C8);
            for (LivingEntity e : victims(boss, new AABB(c, c).inflate(9, 5, 9))) {
                if (flat(e.position(), c) <= 8) hit(boss, e, 6);
            }
            sound(boss, SoundEvents.ENDER_DRAGON_FLAP, 6.0f, 1.2f);
        }
    }

    // =========================================================================================
    // Phase 3: the eyes
    // =========================================================================================

    /** Every eye of one wheel blazes outward: the beams sweep as the wheel turns. Pillars cut them short. */
    public static class WheelWithinWheel extends BossAttack<ChorusEntity> {
        public static final float LENGTH = 24;
        int wheel;
        final Map<UUID, Long> lastHit = new HashMap<>();

        public WheelWithinWheel() {
            super("wheel_within_wheel", "", 30, 100, 25);
        }

        @Override
        public void onWindup(ChorusEntity boss, LivingEntity target) {
            wheel = boss.getRandom().nextInt(3);
            int mask = 0;
            for (int i = 0; i < ChorusGeometry.EYES; i++) if (ChorusGeometry.wheelOf(i) == wheel) mask |= 1 << i;
            boss.forceEyes(boss.forcedEyes() | mask);
            sound(boss, SoundEvents.BEACON_POWER_SELECT, 5.0f, 1.4f);
        }

        @Override
        public void onActive(ChorusEntity boss, LivingEntity target) {
            ChorusFx.send(boss, ChorusFxPayload.EYE_BEAMS, wheel, -1, Vec3.ZERO, Vec3.ZERO, LENGTH, active, 0xFFD27A);
            sound(boss, SoundEvents.BEACON_ACTIVATE, 5.0f, 1.6f);
        }

        @Override
        public void tickActive(ChorusEntity boss, LivingEntity target, int t) {
            Vec3 c = core(boss);
            long now = boss.level().getGameTime();
            for (int i = 0; i < ChorusGeometry.EYES; i++) {
                if (ChorusGeometry.wheelOf(i) != wheel || !boss.partAlive(ChorusEntity.FIRST_EYE + i)) continue;
                Vec3 from = eyePos(boss, i);
                Vec3 to = ChorusLight.reach(boss, from, from.add(from.subtract(c).normalize().scale(LENGTH)));
                for (LivingEntity e : victims(boss, new AABB(from, to).inflate(1.5))) {
                    if (ChorusLight.distanceToBeam(e, from, to) > 1.0) continue;
                    Long last = lastHit.get(e.getUUID());
                    if (last != null && now - last < 10) continue;
                    lastHit.put(e.getUUID(), now);
                    if (hit(boss, e, 5)) e.igniteForSeconds(2);
                }
            }
        }

        @Override
        public void onEnd(ChorusEntity boss) {
            boss.forceEyes(0);
        }
    }

    /** Three to five eyes open at once, each on a different soul: look away, or get behind stone. */
    public static class ThousandEyes extends BossAttack<ChorusEntity> {
        final List<Integer> eyes = new ArrayList<>();

        public ThousandEyes() {
            super("thousand_eyes", "", 50, 1, 25);
        }

        @Override
        public void onWindup(ChorusEntity boss, LivingEntity target) {
            List<LivingEntity> all = targets(boss);
            int n = Mth.clamp(all.size() + 2, 3, 5);
            Set<Integer> taken = new HashSet<>();
            int mask = 0;
            for (int k = 0; k < n && !all.isEmpty(); k++) {
                LivingEntity who = all.get(k % all.size());
                int eye = bestEye(boss, who, taken);
                if (eye < 0) break;
                taken.add(eye);
                eyes.add(eye);
                mask |= 1 << eye;
                for (LivingEntity e : all) ChorusFx.send(boss, ChorusFxPayload.GAZE, eye, e.getId(), Vec3.ZERO, e.getEyePosition(), 0, windup, 0xFFE38A);
            }
            boss.forceEyes(mask);
            sound(boss, AllSounds.CHORUS_GAZE.get(), 6.0f, 1.1f);
            for (ServerPlayer p : boss.challengers()) {
                p.displayClientMessage(Component.translatable("message.supernaturalcraft.chorus.gaze").withStyle(ChatFormatting.YELLOW), true);
            }
        }

        @Override
        public void onActive(ChorusEntity boss, LivingEntity target) {
            for (int eye : eyes) judge(boss, eye);
        }

        @Override
        public void onEnd(ChorusEntity boss) {
            boss.forceEyes(0);
        }
    }

    /** A wheel of burning gold breaks off its rim and rolls across the summit. */
    public static class RollingWheel extends BossAttack<ChorusEntity> {
        public static final float RADIUS = 1.8f;
        Vec3 from, to;
        final Set<UUID> struck = new HashSet<>();

        public RollingWheel() {
            super("rolling_wheel", "", 35, 40, 20);
        }

        @Override
        public void onWindup(ChorusEntity boss, LivingEntity target) {
            Vec3 c = centre(boss);
            float yaw = yawTo(c, target.position()) + (boss.getRandom().nextFloat() - 0.5f) * 30;
            float r = floorRadius(boss);
            from = c.add(dir(yaw).scale(-r));
            to = c.add(dir(yaw).scale(r));
            TelegraphMarker.line(level(boss), from, yaw, 4, 2 * r, TelegraphMarker.RED, windup + 1);
            sound(boss, SoundEvents.ANVIL_PLACE, 4.0f, 0.5f);
        }

        @Override
        public void onActive(ChorusEntity boss, LivingEntity target) {
            ChorusFx.send(boss, ChorusFxPayload.ROLLING, 0, -1, from, to, RADIUS, active, 0xFFA040);
            sound(boss, SoundEvents.MINECART_RIDING, 4.0f, 0.5f);
        }

        @Override
        public void tickActive(ChorusEntity boss, LivingEntity target, int t) {
            Vec3 at = from.lerp(to, t / (double) active);
            for (LivingEntity e : victims(boss, new AABB(at, at).inflate(RADIUS + 0.5, 4, RADIUS + 0.5))) {
                if (flat(e.position(), at) <= RADIUS + 0.3 && struck.add(e.getUUID()) && hit(boss, e, 9)) e.igniteForSeconds(3);
            }
            if (t % 3 == 0) {
                ServerLevel level = level(boss);
                level.addFreshEntity(new FlameTrail(level, boss, at.x, at.y, at.z));
            }
        }
    }

    /** Rings of lightning close in on its heart, then burst outward again: stand between them. */
    public static class LightningGyre extends BossAttack<ChorusEntity> {
        static final float[] RADII = {16, 12, 8, 4, 8, 12, 16};

        public LightningGyre() {
            super("lightning_gyre", "", 30, 70, 25);
        }

        @Override
        public void onWindup(ChorusEntity boss, LivingEntity target) {
            Vec3 c = centre(boss);
            for (int i = 0; i < 4; i++) {
                if (RADII[i] <= floorRadius(boss)) TelegraphMarker.ring(level(boss), c, RADII[i], TelegraphMarker.VIOLET, windup + 10 + i * 10);
            }
            sound(boss, SoundEvents.TRIDENT_THUNDER.value(), 5.0f, 0.6f);
        }

        @Override
        public void tickActive(ChorusEntity boss, LivingEntity target, int t) {
            if ((t - 1) % 10 != 0) return;
            int i = (t - 1) / 10;
            if (i >= RADII.length) return;
            float r = Math.min(RADII[i], floorRadius(boss) - 1);
            Vec3 c = centre(boss);
            int n = Math.max(6, Math.round(r * 0.8f));
            double off = boss.getRandom().nextDouble();
            for (int k = 0; k < n; k++) {
                double a = (k + off) * Math.PI * 2 / n;
                bolt(boss, c.add(Math.cos(a) * r, 0, Math.sin(a) * r), 1.6f, 0);
            }
            for (LivingEntity e : victims(boss, new AABB(c, c).inflate(r + 2, 5, r + 2))) {
                if (Math.abs(flat(e.position(), c) - r) <= 1.4) hit(boss, e, 8);
            }
        }
    }

    // =========================================================================================
    // The last voice: the core
    // =========================================================================================

    /** The core pulses with light that only stone can stop: hide in a pillar's shadow. */
    public static class LastLight extends BossAttack<ChorusEntity> {
        public LastLight() {
            super("last_light", "", 40, 1, 30);
        }

        @Override
        public void onWindup(ChorusEntity boss, LivingEntity target) {
            sound(boss, SoundEvents.BEACON_POWER_SELECT, 6.0f, 0.6f);
            Vec3 c = centre(boss);
            TelegraphMarker.circle(level(boss), c, floorRadius(boss), TelegraphMarker.GOLD, windup + 1);
            for (ServerPlayer p : boss.challengers()) {
                p.displayClientMessage(Component.translatable("message.supernaturalcraft.chorus.shade").withStyle(ChatFormatting.YELLOW), true);
            }
        }

        @Override
        public void onActive(ChorusEntity boss, LivingEntity target) {
            Vec3 from = core(boss);
            ChorusFx.send(boss, ChorusFxPayload.PULSE, 0, -1, ground(boss, boss.position()), Vec3.ZERO, 24, 16, 0xFFF4C8);
            sound(boss, SoundEvents.GENERIC_EXPLODE.value(), 5.0f, 1.6f);
            for (LivingEntity e : targets(boss)) {
                if (!ChorusLight.inShadow(boss, from, e) && hit(boss, e, 9)) e.igniteForSeconds(3);
            }
        }
    }

    /** What is left of its wheels circles the core and fires on the living. */
    public static class Requiem extends BossAttack<ChorusEntity> {
        public Requiem() {
            super("requiem", "", 20, 80, 25);
        }

        @Override
        public void tickActive(ChorusEntity boss, LivingEntity target, int t) {
            if (t % 10 != 5) return;
            List<LivingEntity> all = targets(boss);
            if (all.isEmpty()) return;
            LivingEntity who = all.get(boss.getRandom().nextInt(all.size()));
            Vec3 c = core(boss);
            double a = boss.getRandom().nextDouble() * Math.PI * 2;
            Vec3 from = c.add(Math.cos(a) * 5, boss.getRandom().nextDouble() * 4 - 1, Math.sin(a) * 5);
            Vec3 aim = who.getEyePosition().subtract(0, 0.4, 0);
            Vec3 to = ChorusLight.reach(boss, from, from.add(aim.subtract(from).normalize().scale(40)));
            ChorusFx.beam(boss, from, to, 0.35f, 8, 0xFF8A40);
            sound(boss, SoundEvents.AMETHYST_BLOCK_CHIME, 4.0f, 0.6f + boss.getRandom().nextFloat() * 0.8f);
            for (LivingEntity e : victims(boss, new AABB(from, to).inflate(1))) {
                if (ChorusLight.distanceToBeam(e, from, to) <= 0.8) hit(boss, e, 5);
            }
        }
    }

    // --- ambient storm ----------------------------------------------------------------------

    /** Lightning walks round the edge of the summit while the fight lasts. Only for show. */
    public static void ambientLightning(ChorusEntity boss) {
        ArenaController arena = boss.arena();
        if (arena == null) return;
        double a = boss.getRandom().nextDouble() * Math.PI * 2, r = arena.radius() + 4 + boss.getRandom().nextDouble() * 10;
        Vec3 c = arena.centerVec();
        LightningBolt b = EntityType.LIGHTNING_BOLT.create(level(boss));
        if (b == null) return;
        b.moveTo(c.x + Math.cos(a) * r, c.y - 4, c.z + Math.sin(a) * r);
        b.setVisualOnly(true);
        level(boss).addFreshEntity(b);
    }
}
