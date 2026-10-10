package org.papiricoh.supernaturalcraft.entity.boss.naomi;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.bowl.spell.PetLedger;
import org.papiricoh.supernaturalcraft.network.HeavenFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * One of Naomi's training tests (v0.18): on the room's test floor ({@code ReprogrammingRoomLayout.COPY_SPOTS}) kneel copies of the
 * tested hunter's friends (Dean, Sam, Castiel, a pet they lost, themselves) and stand hostile shapes. Kill every hostile within
 * {@link NaomiBalance#TEST_TICKS} without touching a kneeler, and the result is unexpected: she is stunned and open
 * ({@link NaomiEntity#unexpectedResult}). Killing a kneeler conditions the killer and heals her. Held by her and saved with her.
 */
public final class TrainingTest {

    private final UUID subject;
    private final long deadline;
    private final List<UUID> kneelers, hostiles;
    private boolean touched, over;

    private TrainingTest(UUID subject, long deadline, List<UUID> kneelers, List<UUID> hostiles, boolean touched) {
        this.subject = subject;
        this.deadline = deadline;
        this.kneelers = kneelers;
        this.hostiles = hostiles;
        this.touched = touched;
    }

    /** Raises the copies at {@code spots} (kneelers first, shuffled across them) for {@code subject}'s test. */
    public static TrainingTest begin(NaomiEntity naomi, ServerLevel level, LivingEntity subject, List<Vec3> spots) {
        int phase = naomi.phase();
        List<String> friends = friends(level, subject);
        Collections.shuffle(friends, new java.util.Random(naomi.getRandom().nextLong()));
        int k = Math.min(NaomiBalance.kneelers(phase), friends.size());
        int h = NaomiBalance.hostiles(phase);
        List<Vec3> free = new ArrayList<>(spots);
        Collections.shuffle(free, new java.util.Random(naomi.getRandom().nextLong()));
        List<UUID> kneelers = new ArrayList<>(), hostiles = new ArrayList<>();
        for (int i = 0; i < k + h; i++) {
            Vec3 at = i < free.size() ? free.get(i) : naomi.position().add(Math.cos(i) * 4, 0, Math.sin(i) * 4);
            boolean hostile = i >= k;
            String look = hostile ? TrainingCopyEntity.RIVAL + naomi.getRandom().nextInt(3) : friends.get(i);
            TrainingCopyEntity copy = AllEntities.TRAINING_COPY.get().create(level);
            if (copy == null) continue;
            float yaw = (float) Math.toDegrees(Math.atan2(-(subject.getX() - at.x), subject.getZ() - at.z));
            copy.moveTo(at.x, at.y, at.z, yaw, 0);
            copy.setYHeadRot(yaw);
            copy.setYBodyRot(yaw);
            copy.finalizeSpawn(level, level.getCurrentDifficultyAt(copy.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            copy.raise(naomi.getUUID(), look, subject.getUUID(), hostile);
            level.addFreshEntity(copy);
            naomi.minions().add(copy.getUUID());
            (hostile ? hostiles : kneelers).add(copy.getUUID());
            level.sendParticles(hostile ? ParticleTypes.LARGE_SMOKE : ParticleTypes.END_ROD, at.x, at.y + 1, at.z, 20, 0.3, 0.8, 0.3, 0.02);
        }
        naomi.playSound(AllSounds.heaven("naomi.test_bell"), 2f, 1f);
        return new TrainingTest(subject.getUUID(), level.getGameTime() + NaomiBalance.TEST_TICKS, kneelers, hostiles, false);
    }

    /** What the tested hunter's friends look like: the brothers, the angel, a pet they lost, and themselves. */
    static List<String> friends(ServerLevel level, LivingEntity subject) {
        List<String> out = new ArrayList<>(List.of(TrainingCopyEntity.DEAN, TrainingCopyEntity.SAM, TrainingCopyEntity.CASTIEL));
        PetLedger.get(level.getServer()).mostRecentDead(subject.getUUID()).ifPresent(e -> out.add(e.type().toString()));
        if (subject instanceof Player) out.add(TrainingCopyEntity.OWNER);
        return out;
    }

    public UUID subject() {
        return subject;
    }

    public long deadline() {
        return deadline;
    }

    public List<UUID> kneelers() {
        return kneelers;
    }

    public List<UUID> hostiles() {
        return hostiles;
    }

    public boolean touched() {
        return touched;
    }

    public boolean over() {
        return over;
    }

    /** A kneeler was hurt by {@code by}: the test can no longer be passed. */
    void touch(TrainingCopyEntity copy, Player by) {
        if (over || !kneelers.contains(copy.getUUID())) return;
        if (!touched && by instanceof ServerPlayer p) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.naomi.test_touched").withStyle(ChatFormatting.GRAY,
                    ChatFormatting.ITALIC), true);
        }
        touched = true;
    }

    /** One of the copies is down: a kneeler conditions its killer and heals her. */
    void fell(NaomiEntity naomi, TrainingCopyEntity copy, @Nullable Player by) {
        if (over) return;
        if (kneelers.remove(copy.getUUID())) {
            touched = true;
            if (by != null) {
                by.addEffect(new MobEffectInstance(AllMobEffects.CONDITIONED, NaomiBalance.KNEELER_CONDITIONED, 0), naomi);
                by.displayClientMessage(Component.translatable("message.supernaturalcraft.naomi.kneeler_killed")
                        .withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC), true);
            }
            naomi.healTrue(NaomiBalance.KNEELER_HEAL * naomi.trueMaxHealth());
        } else {
            hostiles.remove(copy.getUUID());
        }
    }

    /** Hostiles still standing. */
    public int hostilesLeft(ServerLevel level) {
        hostiles.removeIf(id -> !(level.getEntity(id) instanceof TrainingCopyEntity c) || !c.isAlive());
        return hostiles.size();
    }

    /** Each tick: passed, or out of time. @return whether it is over */
    public boolean tick(NaomiEntity naomi, ServerLevel level) {
        if (over) return true;
        long now = level.getGameTime();
        if (NaomiBalance.testPassed(hostilesLeft(level), touched, now, deadline)) {
            end(naomi, level, true);
        } else if (now > deadline) {
            end(naomi, level, false);
        }
        return over;
    }

    /** The test ends: every copy fades; passed, she is stunned ("Unexpected result."). */
    public void end(NaomiEntity naomi, ServerLevel level, boolean passed) {
        if (over) return;
        over = true;
        for (UUID id : List.copyOf(kneelers)) fade(level, naomi, id);
        for (UUID id : List.copyOf(hostiles)) fade(level, naomi, id);
        naomi.fx(new HeavenFxPayload(naomi.getId(), HeavenFxPayload.TRAINING_TEST, passed ? 1 : 0, 0, naomi.position(), 0, ""));
        if (passed) naomi.unexpectedResult();
    }

    private static void fade(ServerLevel level, NaomiEntity naomi, UUID id) {
        Entity e = level.getEntity(id);
        if (e != null) {
            level.sendParticles(ParticleTypes.CLOUD, e.getX(), e.getY() + 1, e.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
            e.discard();
        }
        naomi.minions().remove(id);
    }

    // --- persistence ------------------------------------------------------------------------------------------------------

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Subject", subject);
        tag.putLong("Deadline", deadline);
        tag.putBoolean("Touched", touched);
        tag.put("Kneelers", uuids(kneelers));
        tag.put("Hostiles", uuids(hostiles));
        return tag;
    }

    public static @Nullable TrainingTest load(CompoundTag tag) {
        if (!tag.hasUUID("Subject")) return null;
        return new TrainingTest(tag.getUUID("Subject"), tag.getLong("Deadline"), read(tag.getList("Kneelers", Tag.TAG_INT_ARRAY)),
                read(tag.getList("Hostiles", Tag.TAG_INT_ARRAY)), tag.getBoolean("Touched"));
    }

    private static ListTag uuids(List<UUID> ids) {
        ListTag list = new ListTag();
        for (UUID id : ids) list.add(NbtUtils.createUUID(id));
        return list;
    }

    private static List<UUID> read(ListTag list) {
        List<UUID> out = new ArrayList<>();
        for (Tag t : list) out.add(NbtUtils.loadUUID(t));
        return out;
    }
}
