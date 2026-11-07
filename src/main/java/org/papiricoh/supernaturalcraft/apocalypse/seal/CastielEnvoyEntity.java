package org.papiricoh.supernaturalcraft.apocalypse.seal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.pact.PactDialogue;
import org.papiricoh.supernaturalcraft.pact.StoryNpcEntity;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Castiel at the seals (v0.40, the Seals slice): he warns of the traitor once two angel seals were held, and comes back when Uriel
 * is gone (the Godstiel rig, worn as Castiel). Dialogue {@link #DIALOGUE} ({@link SealEnvoy#answer}); silence is always the last
 * option:
 * <ul>
 *   <li>{@code warn} (listen | later) → {@code traitor} (where | doubt) → {@code garrison} (go): he hands over the way to the
 *   garrison.</li>
 *   <li>{@code after} (go), when Uriel has fallen.</li>
 * </ul>
 */
public class CastielEnvoyEntity extends StoryNpcEntity {

    public static final String DIALOGUE = "castiel_envoy";
    private static final List<String> GO = List.of("go");
    public static final PactDialogue.Line WARN = new PactDialogue.Line("warn", List.of("listen", "later"));
    public static final PactDialogue.Line TRAITOR = new PactDialogue.Line("traitor", List.of("where", "doubt"));
    public static final PactDialogue.Line GARRISON = new PactDialogue.Line("garrison", GO);
    public static final PactDialogue.Line AFTER = new PactDialogue.Line("after", GO);
    /** Every line (lang and tests). */
    public static final List<PactDialogue.Line> LINES = List.of(WARN, TRAITOR, GARRISON, AFTER);

    private static final Map<UUID, UUID> VISITING = new ConcurrentHashMap<>();
    private boolean afterFight;

    public CastielEnvoyEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** He comes to {@code hunter} to warn them of the traitor. Null if he could not. */
    public static @Nullable CastielEnvoyEntity visit(ServerPlayer hunter) {
        return come(hunter, false);
    }

    /** He comes to {@code hunter} after Uriel's fall. */
    public static @Nullable CastielEnvoyEntity after(ServerPlayer hunter) {
        return come(hunter, true);
    }

    private static @Nullable CastielEnvoyEntity come(ServerPlayer hunter, boolean after) {
        if (visiting(hunter)) return null;
        CastielEnvoyEntity c = AllEntities.CASTIEL_ENVOY.get().create(hunter.serverLevel());
        if (c == null) return null;
        c.afterFight = after;
        c = arriveBefore(c, hunter, 3);
        if (c != null) VISITING.put(hunter.getUUID(), c.getUUID());
        return c;
    }

    public static boolean visiting(ServerPlayer hunter) {
        UUID id = VISITING.get(hunter.getUUID());
        if (id == null) return false;
        if (hunter.serverLevel().getEntity(id) instanceof CastielEnvoyEntity c && c.isAlive()) return true;
        VISITING.remove(hunter.getUUID());
        return false;
    }

    public boolean afterFight() {
        return afterFight;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (hunter() != null) VISITING.remove(hunter(), getUUID());
        super.remove(reason);
    }

    @Override
    protected String dialogue() {
        return DIALOGUE;
    }

    @Override
    protected String prefix() {
        return "animation." + org.papiricoh.supernaturalcraft.entity.boss.godstiel.GodstielAssets.RIG + ".";
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
        return afterFight ? AFTER : WARN;
    }

    @Override
    protected void flourish(ServerLevel level, boolean arriving) {
        level.playSound(null, blockPosition(), AllSounds.LUCIFER_WINGS.get(), SoundSource.NEUTRAL, 1.0f, arriving ? 0.7f : 0.8f);
        level.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 1.2, getZ(), arriving ? 30 : 40, 0.4, 0.8, 0.4, 0.06);
    }
}
