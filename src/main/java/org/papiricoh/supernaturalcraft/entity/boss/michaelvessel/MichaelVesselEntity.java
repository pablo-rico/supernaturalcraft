package org.papiricoh.supernaturalcraft.entity.boss.michaelvessel;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import org.papiricoh.supernaturalcraft.nephilim.michael.MichaelPossession;

import java.util.List;
import java.util.Map;

/**
 * Michael in this world (v0.41, season 14): the Michael of the Apocalypse World, fled through the rift, in a vessel. He made
 * a hunter his offer; out in the open if they said no, or in their own head ({@link #mindscape()}: a projection, the
 * renderer tints it) if they said yes and now throw him out. Three phases ({@link MichaelVesselBalance}). Beaten, he is
 * driven out for good: {@code main/the_spear} for every fighter, and the road moves on ({@link NephilimRules.Step#VESSEL_DOWN}).
 */
public class MichaelVesselEntity extends SeasonBossEntity {

    private static final EntityDataAccessor<Boolean> MINDSCAPE = SynchedEntityData.defineId(MichaelVesselEntity.class, EntityDataSerializers.BOOLEAN);
    public static final String DEFEAT_ADVANCEMENT = "main/the_spear";

    public MichaelVesselEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, VANILLA_BASE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MINDSCAPE, false);
    }

    /** A projection, inside a hunter's head (the yes). */
    public boolean mindscape() {
        return entityData.get(MINDSCAPE);
    }

    public void setMindscape(boolean on) {
        entityData.set(MINDSCAPE, on);
    }

    @Override
    protected String rig() {
        return MichaelVesselAssets.RIG;
    }

    @Override
    protected List<String> clips() {
        return MichaelVesselAssets.TRIGGERED;
    }

    @Override
    protected int theme() {
        return mindscape() ? ArenaTheme.MINDSCAPE : ArenaTheme.VESSEL;
    }

    @Override
    protected int phases() {
        return MichaelVesselBalance.PHASES;
    }

    @Override
    protected int arenaRadius() {
        return MichaelVesselBalance.ARENA_RADIUS;
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> attacks(int phase) {
        return MichaelVesselAttacks.pool(phase);
    }

    @Override
    protected int gap(int phase) {
        return MichaelVesselBalance.attackGap(phase);
    }

    @Override
    public float attackDamageMultiplier() {
        return super.attackDamageMultiplier() * (mindscape() ? MichaelVesselBalance.MINDSCAPE_DAMAGE : 1f);
    }

    @Override
    protected Map<String, DeferredHolder<SoundEvent, SoundEvent>> sounds() {
        return NephilimSounds.MICHAEL_VESSEL;
    }

    @Override
    protected List<ItemStack> spoils() {
        return List.of(new ItemStack(NephilimRegistry.MICHAEL_VESSEL_TROPHY_ITEM.get()));
    }

    @Override
    protected List<String> grantsOnDefeat() {
        return List.of(DEFEAT_ADVANCEMENT);
    }

    @Override
    protected void afterDefeat(ServerLevel level, List<ServerPlayer> fighters) {
        long day = org.papiricoh.supernaturalcraft.util.WorldClock.day(level.getServer()) / 24000L;
        for (ServerPlayer p : fighters) {
            Nephilims.advance(p, NephilimRules.Step.VESSEL_DOWN);
            Nephilims.update(p, n -> n.withVisit(day));
        }
        if (mindscape()) MichaelPossession.expelled(level.getServer(), fighters);
    }

    @Override
    protected @Nullable String transitionClip(int phase) {
        return "wing_flare";
    }

    @Override
    protected ChatFormatting colour(int phase) {
        return phase >= 3 ? ChatFormatting.WHITE : ChatFormatting.AQUA;
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return ParticleTypes.END_ROD;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Mindscape", mindscape());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setMindscape(tag.getBoolean("Mindscape"));
    }
}
