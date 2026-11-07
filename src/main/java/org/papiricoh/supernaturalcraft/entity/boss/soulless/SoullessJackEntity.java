package org.papiricoh.supernaturalcraft.entity.boss.soulless;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRegistry;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRules;
import org.papiricoh.supernaturalcraft.nephilim.NephilimSounds;
import org.papiricoh.supernaturalcraft.nephilim.Nephilims;
import org.papiricoh.supernaturalcraft.nephilim.boss.SeasonBossEntity;
import org.papiricoh.supernaturalcraft.nephilim.jack.JackAssets;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.util.WorldClock;

import java.util.List;
import java.util.Map;

/**
 * Jack without his soul (v0.41, season 14's finale): he traded it for his power back and came home wrong. He comes down at
 * night where his hunter is ({@link SoullessJackSummoning}); the third phase is in the air. Beaten, black tar rises and takes
 * him into the Empty: every fighter gets a nephilim's grace (the way in, and +mana once taken in) and the road moves on
 * ({@link NephilimRules.Step#JACK_FELL}).
 */
public class SoullessJackEntity extends SeasonBossEntity {

    public SoullessJackEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, VANILLA_BASE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected String rig() {
        return JackAssets.RIG;
    }

    /** His bar, titles and messages are soulless Jack's own ({@code entity.supernaturalcraft.soulless_jack.bar…}). */
    @Override
    protected String key() {
        return JackAssets.SOULLESS;
    }

    @Override
    protected List<String> clips() {
        return JackAssets.TRIGGERED;
    }

    @Override
    protected int theme() {
        return ArenaTheme.NEPHILIM;
    }

    @Override
    protected int phases() {
        return SoullessJackBalance.PHASES;
    }

    @Override
    protected int arenaRadius() {
        return SoullessJackBalance.ARENA_RADIUS;
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> attacks(int phase) {
        return SoullessJackAttacks.pool(phase);
    }

    @Override
    protected int gap(int phase) {
        return SoullessJackBalance.attackGap(phase);
    }

    @Override
    public boolean isAerialPhase() {
        return phase() >= SoullessJackBalance.PHASES;
    }

    @Override
    protected @Nullable String hoverClip() {
        return "float_idle";
    }

    @Override
    protected Map<String, DeferredHolder<SoundEvent, SoundEvent>> sounds() {
        return NephilimSounds.JACK;
    }

    @Override
    protected List<ItemStack> spoils() {
        return List.of(new ItemStack(NephilimRegistry.NEPHILIM_GRACE.get()), new ItemStack(NephilimRegistry.SOULLESS_JACK_TROPHY_ITEM.get()));
    }

    @Override
    protected void afterDefeat(ServerLevel level, List<ServerPlayer> fighters) {
        long day = WorldClock.day(level.getServer()) / 24000L;
        for (ServerPlayer p : fighters) {
            Nephilims.advance(p, NephilimRules.Step.JACK_FELL);
            Nephilims.update(p, n -> n.withVisit(day));
        }
    }

    @Override
    protected @Nullable String transitionClip(int phase) {
        return phase >= SoullessJackBalance.PHASES ? "nova" : "shield";
    }

    @Override
    protected ChatFormatting colour(int phase) {
        return ChatFormatting.YELLOW;
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return AllParticles.GRACE.get();
    }
}
