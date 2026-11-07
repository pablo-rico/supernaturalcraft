package org.papiricoh.supernaturalcraft.nephilim.michael;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceDialogue;
import org.papiricoh.supernaturalcraft.entity.boss.michaelvessel.MichaelVesselAssets;
import org.papiricoh.supernaturalcraft.entity.boss.michaelvessel.MichaelVesselSummoning;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRegistry;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRules;
import org.papiricoh.supernaturalcraft.nephilim.Nephilims;
import org.papiricoh.supernaturalcraft.pact.PactDialogue;
import org.papiricoh.supernaturalcraft.pact.StoryNpcEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Michael, come through the rift (v0.41, season 14), at dawn: he makes a hunter his offer (dialogue {@code michaels_offer}).
 * Yes: he wears them for a while ({@link MichaelPossession}) and then they must throw him out of their own head. No, or
 * silence (the last option): he takes a vessel of his own and comes for them there and then ({@link MichaelVesselSummoning}).
 * He comes back each dawn until his vessel has fallen.
 */
public class MichaelEnvoyEntity extends StoryNpcEntity {

    public static final String DIALOGUE = "michaels_offer";
    public static final String OFFER = "offer", YES = "yes", NO = "no";

    private static final Map<UUID, UUID> VISITING = new ConcurrentHashMap<>();

    public MichaelEnvoyEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** At mod construction. */
    public static void registerDialogue() {
        AllegianceDialogue.register(DIALOGUE, MichaelEnvoyEntity::answer);
    }

    public static @Nullable MichaelEnvoyEntity visit(ServerPlayer hunter) {
        if (visiting(hunter)) return null;
        MichaelEnvoyEntity m = arriveBefore(NephilimRegistry.MICHAEL_ENVOY.get().create(hunter.serverLevel()), hunter, 4);
        if (m != null) VISITING.put(hunter.getUUID(), m.getUUID());
        return m;
    }

    public static boolean visiting(ServerPlayer hunter) {
        UUID id = VISITING.get(hunter.getUUID());
        if (id == null) return false;
        if (hunter.serverLevel().getEntity(id) instanceof MichaelEnvoyEntity m && m.isAlive()) return true;
        VISITING.remove(hunter.getUUID());
        return false;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (hunter() != null) VISITING.remove(hunter(), getUUID());
        super.remove(reason);
    }

    static void answer(ServerPlayer p, @Nullable Entity speaker, String node, String option) {
        MichaelEnvoyEntity m = speaker instanceof MichaelEnvoyEntity e ? e : null;
        boolean yes = YES.equals(option);
        Nephilims.update(p, n -> n.withAnswer(yes ? NephilimRules.YES : NephilimRules.NO));
        Nephilims.advance(p, NephilimRules.Step.OFFERED);
        if (m != null) m.leave(10);
        if (yes) {
            MichaelPossession.begin(p);
        } else {
            net.minecraft.core.BlockPos at = m != null ? m.blockPosition() : p.blockPosition().relative(p.getDirection(), 4);
            MichaelVesselSummoning.summon(p.serverLevel(), at, p);
        }
    }

    @Override
    protected String dialogue() {
        return DIALOGUE;
    }

    @Override
    protected String prefix() {
        return "animation." + MichaelVesselAssets.RIG + ".";
    }

    @Override
    protected List<String> actions() {
        return List.of("teleport", "snap", "wing_flare");
    }

    @Override
    protected String appearClip() {
        return "teleport";
    }

    @Override
    protected String leaveClip() {
        return "teleport";
    }

    @Override
    protected String talkClip() {
        return "snap";
    }

    @Override
    protected @Nullable PactDialogue.Line opening(ServerPlayer hunter) {
        NephilimRules.Step s = Nephilims.step(hunter);
        if (s != NephilimRules.Step.MICHAEL_FLED && s != NephilimRules.Step.OFFERED) return null;
        // The last option is what silence answers.
        return new PactDialogue.Line(s == NephilimRules.Step.OFFERED ? "again" : OFFER, List.of(YES, NO));
    }

    @Override
    protected void flourish(ServerLevel level, boolean arriving) {
        level.playSound(null, blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.NEUTRAL, 1f, arriving ? 0.6f : 0.8f);
        level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 30, 0.4, 0.9, 0.4, 0.05);
    }
}
