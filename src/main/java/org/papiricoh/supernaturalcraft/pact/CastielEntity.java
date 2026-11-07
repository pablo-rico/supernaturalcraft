package org.papiricoh.supernaturalcraft.pact;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.godstiel.GodstielAssets;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Castiel (v0.39): the angel who comes at dawn with a plan for the war in Heaven (season 6). He can't be hurt, says what he
 * came to say ({@link PactDialogue#castielOpens}) and goes in a flutter of wings. One rig with Godstiel
 * ({@link GodstielAssets}), worn without the glow and with every toggled bone hidden.
 */
public class CastielEntity extends StoryNpcEntity {

    /** Who each Castiel came for (one at a time per hunter). */
    private static final Map<UUID, UUID> VISITING = new ConcurrentHashMap<>();

    public CastielEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** He comes to {@code hunter}, a few steps in front of them. Null if he could not. */
    public static @Nullable CastielEntity visit(ServerPlayer hunter) {
        if (visiting(hunter)) return null;
        CastielEntity c = arriveBefore(AllEntities.CASTIEL.get().create(hunter.serverLevel()), hunter, 3);
        if (c != null) VISITING.put(hunter.getUUID(), c.getUUID());
        return c;
    }

    /** Whether Castiel is with {@code hunter} now. */
    public static boolean visiting(ServerPlayer hunter) {
        UUID id = VISITING.get(hunter.getUUID());
        if (id == null) return false;
        if (hunter.serverLevel().getEntity(id) instanceof CastielEntity c && c.isAlive()) return true;
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
        return PactDialogue.CASTIEL;
    }

    @Override
    protected String prefix() {
        return "animation." + GodstielAssets.RIG + ".";
    }

    @Override
    protected List<String> actions() {
        return List.of("appear", "vanish", "talk", "nod", "turn_away");
    }

    @Override
    protected String appearClip() {
        return "appear";
    }

    @Override
    protected String leaveClip() {
        return "vanish";
    }

    @Override
    protected @Nullable PactDialogue.Line opening(ServerPlayer hunter) {
        Pact p = Pacts.get(hunter);
        return PactDialogue.castielOpens(p.stage(), p.alphas(), p.scenes());
    }

    @Override
    protected void flourish(ServerLevel level, boolean arriving) {
        level.playSound(null, blockPosition(), AllSounds.LUCIFER_WINGS.get(), SoundSource.NEUTRAL, 1.0f, arriving ? 0.7f : 0.8f);
        level.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 1.2, getZ(), arriving ? 30 : 40, 0.4, 0.8, 0.4, 0.06);
    }
}
