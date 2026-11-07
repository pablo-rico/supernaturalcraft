package org.papiricoh.supernaturalcraft.british;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.hess.HessAssets;
import org.papiricoh.supernaturalcraft.legacy.Legacies;
import org.papiricoh.supernaturalcraft.pact.PactDialogue;
import org.papiricoh.supernaturalcraft.pact.StoryNpcEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dr. Hess at dawn (v0.41): the head of the British Men of Letters comes in person, once Amara is beaten, with London's
 * ultimatum ({@link BritishDialogue}). She can't be hurt, says her piece and goes. One rig with the boss ({@link HessAssets}).
 */
public class HessEnvoyEntity extends StoryNpcEntity {

    /** Who each Hess came for (one at a time per hunter). */
    private static final Map<UUID, UUID> VISITING = new ConcurrentHashMap<>();

    public HessEnvoyEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** She comes to {@code hunter}, a few steps in front of them. Null if she could not. */
    public static @Nullable HessEnvoyEntity visit(ServerPlayer hunter) {
        if (visiting(hunter)) return null;
        HessEnvoyEntity h = arriveBefore(BritishRegistry.HESS_ENVOY.get().create(hunter.serverLevel()), hunter, 3.5);
        if (h != null) VISITING.put(hunter.getUUID(), h.getUUID());
        return h;
    }

    /** Whether Hess is with {@code hunter} now. */
    public static boolean visiting(ServerPlayer hunter) {
        UUID id = VISITING.get(hunter.getUUID());
        if (id == null) return false;
        if (hunter.serverLevel().getEntity(id) instanceof HessEnvoyEntity h && h.isAlive()) return true;
        VISITING.remove(hunter.getUUID());
        return false;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (hunter() != null) VISITING.remove(hunter(), getUUID());
        super.remove(reason);
    }

    @Override
    protected String dialogue() {
        return BritishDialogue.ULTIMATUM;
    }

    @Override
    protected String prefix() {
        return "animation." + HessAssets.RIG + ".";
    }

    @Override
    protected List<String> actions() {
        return List.of("appear", "leave", "talk");
    }

    @Override
    protected String appearClip() {
        return "appear";
    }

    @Override
    protected String leaveClip() {
        return "leave";
    }

    @Override
    protected @Nullable PactDialogue.Line opening(ServerPlayer hunter) {
        BritishDialogue.Line l = BritishDialogue.opens(BritishOrder.choice(hunter), Legacies.member(hunter));
        return l == null ? null : line(l);
    }

    /** Speaks one of the ultimatum's lines (the shared NPC speaks in {@link PactDialogue.Line}s). */
    public void say(ServerPlayer p, BritishDialogue.Line line) {
        say(p, line(line));
    }

    static PactDialogue.Line line(BritishDialogue.Line l) {
        return new PactDialogue.Line(l.node(), l.options());
    }

    @Override
    protected void flourish(ServerLevel level, boolean arriving) {
        level.playSound(null, blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.NEUTRAL, 1.0f, arriving ? 0.9f : 0.8f);
        level.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 1.2, getZ(), 20, 0.3, 0.6, 0.3, 0.4);
    }
}
