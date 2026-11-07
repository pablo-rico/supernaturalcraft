package org.papiricoh.supernaturalcraft.apocalypse.ruby;

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
import org.papiricoh.supernaturalcraft.util.Players;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ruby before the reveal (v0.40): the demon who comes at dawn after Azazel, offers the blood, sends the hunter to Alastair and
 * then the way to Lilith ({@link RubyRules#opens}). She can't be hurt, says her piece and goes in black smoke. The same rig as her
 * fight ({@link RubyAssets}).
 */
public class RubyNpcEntity extends StoryNpcEntity {

    private static final Map<UUID, UUID> VISITING = new ConcurrentHashMap<>();

    public RubyNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** She comes to {@code hunter}, a few steps in front of them. Null if she could not. */
    public static @Nullable RubyNpcEntity visit(ServerPlayer hunter) {
        if (visiting(hunter)) return null;
        RubyNpcEntity r = arriveBefore(AllEntities.RUBY_NPC.get().create(hunter.serverLevel()), hunter, 3);
        if (r != null) VISITING.put(hunter.getUUID(), r.getUUID());
        return r;
    }

    public static boolean visiting(ServerPlayer hunter) {
        UUID id = VISITING.get(hunter.getUUID());
        if (id == null) return false;
        if (hunter.serverLevel().getEntity(id) instanceof RubyNpcEntity r && r.isAlive()) return true;
        VISITING.remove(hunter.getUUID());
        return false;
    }

    public static void forget(UUID hunter) {
        VISITING.remove(hunter);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (hunter() != null) VISITING.remove(hunter(), getUUID());
        super.remove(reason);
    }

    @Override
    protected String dialogue() {
        return RubyRules.DIALOGUE;
    }

    @Override
    protected String prefix() {
        return "animation." + RubyAssets.RIG + ".";
    }

    @Override
    protected List<String> actions() {
        return List.of("appear", "vanish", "talk", "give", "laugh");
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
        RubyRules.Line l = RubyRules.opens(Rubys.step(hunter), Rubys.side(hunter), done(hunter, "main/yellow_eyed"),
                done(hunter, "main/on_the_head_of_a_pin"), done(hunter, "main/lucifer_rising"));
        return l == null ? null : new PactDialogue.Line(l.node(), l.options());
    }

    static boolean done(ServerPlayer p, String adv) {
        return org.papiricoh.supernaturalcraft.author.AuthorWorld.done(p, adv);
    }

    @Override
    protected void flourish(ServerLevel level, boolean arriving) {
        level.playSound(null, blockPosition(), AllSounds.apocalypse(arriving ? "ruby.smoke_in" : "ruby.smoke_out"), SoundSource.NEUTRAL, 1.0f, 1.0f);
        level.sendParticles(AllParticles.DEMON_SMOKE.get(), getX(), getY() + 1, getZ(), arriving ? 40 : 50, 0.4, 0.8, 0.4, 0.04);
    }

    /** Unused helper kept for symmetry with the other visitors (who the visit is for, if they are here). */
    @Nullable ServerPlayer hunterHere() {
        return hunter() == null || !(level() instanceof ServerLevel s) ? null : (ServerPlayer) Players.byUuid(s, hunter());
    }
}
