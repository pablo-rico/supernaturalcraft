package org.papiricoh.supernaturalcraft.british.birth;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.pact.PactDialogue;
import org.papiricoh.supernaturalcraft.pact.StoryNpcEntity;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;

/**
 * Lucifer in President Rourke's body (v0.41): he comes for his son at the birth ({@link BirthScene}), reaches for him and is
 * thrown back by the light, through a rift into another world. Staged only: he never talks to anyone and can't be hurt.
 */
public class RourkeLuciferEntity extends StoryNpcEntity {

    public RourkeLuciferEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected String dialogue() {
        return "rourke_lucifer";
    }

    @Override
    protected String prefix() {
        return "animation." + BirthAssets.ROURKE + ".";
    }

    @Override
    protected List<String> actions() {
        return List.of("reach", "thrown");
    }

    @Override
    protected String appearClip() {
        return "none";
    }

    @Override
    protected String leaveClip() {
        return "thrown";
    }

    @Override
    protected @Nullable PactDialogue.Line opening(ServerPlayer hunter) {
        return null;
    }

    @Override
    protected void flourish(ServerLevel level, boolean arriving) {
        level.playSound(null, blockPosition(), AllSounds.LUCIFER_WINGS.get(), net.minecraft.sounds.SoundSource.HOSTILE, 1.2f, arriving ? 0.7f : 0.5f);
        level.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 1.2, getZ(), arriving ? 30 : 60, 0.4, 0.8, 0.4, 0.06);
    }
}
