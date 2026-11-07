package org.papiricoh.supernaturalcraft.entity.boss.asmodeus;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import org.papiricoh.supernaturalcraft.nephilim.NephilimSounds;
import org.papiricoh.supernaturalcraft.nephilim.boss.SeasonBossEntity;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

import java.util.List;
import java.util.Map;

/**
 * Asmodeus, Prince of Hell (v0.41, season 13, optional): the one who would be King while Crowley was gone, with Gabriel in his
 * cellar milked for grace. Called by his rite ({@code summon_asmodeus}) once Jack is born: he wants the nephilim. Three phases
 * ({@link AsmodeusBalance}); he leaves every fighter the shard of the Lance of Michael he kept ({@code archangel_bane_shard}),
 * worked into Michael's Lance at the Hellforge.
 */
public class AsmodeusEntity extends SeasonBossEntity {

    public AsmodeusEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, VANILLA_BASE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected String rig() {
        return AsmodeusAssets.RIG;
    }

    @Override
    protected List<String> clips() {
        return AsmodeusAssets.TRIGGERED;
    }

    @Override
    protected int theme() {
        return ArenaTheme.PRINCES;
    }

    @Override
    protected int phases() {
        return AsmodeusBalance.PHASES;
    }

    @Override
    protected int arenaRadius() {
        return AsmodeusBalance.ARENA_RADIUS;
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> attacks(int phase) {
        return AsmodeusAttacks.pool(phase);
    }

    @Override
    protected int gap(int phase) {
        return AsmodeusBalance.attackGap(phase);
    }

    @Override
    protected Map<String, DeferredHolder<SoundEvent, SoundEvent>> sounds() {
        return NephilimSounds.ASMODEUS;
    }

    @Override
    protected List<ItemStack> spoils() {
        return List.of(new ItemStack(NephilimRegistry.ARCHANGEL_BANE_SHARD.get()), new ItemStack(NephilimRegistry.ASMODEUS_TROPHY_ITEM.get()));
    }

    @Override
    protected @Nullable String transitionClip(int phase) {
        return phase == 2 ? "grace_drink" : "taunt";
    }

    @Override
    protected ChatFormatting colour(int phase) {
        return phase >= 2 ? ChatFormatting.GOLD : ChatFormatting.WHITE;
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return phase >= 2 ? AllParticles.GRACE.get() : ParticleTypes.SMOKE;
    }
}
