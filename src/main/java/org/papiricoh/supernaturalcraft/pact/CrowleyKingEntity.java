package org.papiricoh.supernaturalcraft.pact;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleyAssets;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Crowley, King of Hell (v0.39): Castiel's partner in opening Purgatory (season 6). Not the boss of the asylum nor the companion:
 * the story's Crowley, who answers a crossroads call from a hunter Castiel sent him (or, tired of waiting, comes to them at
 * midnight), sets them on the Alphas and hands them his lab. He wears Crowley's rig ({@link CrowleyAssets}) in the King of Hell's
 * skin. He can't be hurt and he never stays.
 */
public class CrowleyKingEntity extends StoryNpcEntity {

    private static final Map<UUID, UUID> VISITING = new ConcurrentHashMap<>();

    public CrowleyKingEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** He comes to {@code hunter} at {@code at} (a crossroads), or before them if {@code at} is null. Null if he could not. */
    public static @Nullable CrowleyKingEntity visit(ServerPlayer hunter, @Nullable Vec3 at) {
        if (visiting(hunter)) return null;
        CrowleyKingEntity c = AllEntities.CROWLEY_KING.get().create(hunter.serverLevel());
        c = at != null ? arriveAt(c, hunter, at) : arriveBefore(c, hunter, 3);
        if (c != null) VISITING.put(hunter.getUUID(), c.getUUID());
        return c;
    }

    public static boolean visiting(ServerPlayer hunter) {
        UUID id = VISITING.get(hunter.getUUID());
        if (id == null) return false;
        if (hunter.serverLevel().getEntity(id) instanceof CrowleyKingEntity c && c.isAlive()) return true;
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
        return PactDialogue.CROWLEY;
    }

    @Override
    protected String prefix() {
        return "animation." + CrowleyAssets.RIG + ".";
    }

    @Override
    protected List<String> actions() {
        return List.of("appear", "vanish", "talk", "snap", "point", "dismiss");
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
        return PactDialogue.crowleyOpens(p.stage(), p.scenes());
    }

    @Override
    protected void flourish(ServerLevel level, boolean arriving) {
        level.playSound(null, blockPosition(), AllSounds.crowley(arriving ? "smoke_in" : "smoke_out"), SoundSource.HOSTILE, 1.0f, 1.0f);
        level.sendParticles(AllParticles.DEMON_SMOKE.get(), getX(), getY() + 1, getZ(), 40, 0.4, 0.9, 0.4, 0.04);
    }
}
