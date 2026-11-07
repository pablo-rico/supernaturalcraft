package org.papiricoh.supernaturalcraft.nephilim.boss;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.BossShares;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * v0.41 core: the plumbing the core's four bosses (Asmodeus, Michael's vessel, soulless Jack, the Shadow) share on top of
 * {@link LuciferEntity}: their contract rig ({@code animation.<rig>.<clip>}), the boss bar the season's HUD knows
 * ({@code entity.supernaturalcraft.<rig>.bar.phase<n>}), a title card at each phase
 * ({@code boss.supernaturalcraft.<rig>.phase<n>} and {@code .phase<n>.sub}), their sounds ({@code <rig>.<id>}, see
 * {@code NephilimSounds}), a plain arena of their theme round where they rise, and spoils given to every fighter
 * ({@link BossShares}). Each keeps its numbers in a pure {@code *Balance} and its attacks in {@code *Attacks}.
 */
public abstract class SeasonBossEntity extends LuciferEntity {

    protected SeasonBossEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 600;
    }

    // --- what each boss says about itself ----------------------------------------------------------------------------------

    protected abstract String rig();

    /** The id its text goes by (bar, titles, messages): its rig's, unless two bosses share a rig. */
    protected String key() {
        return rig();
    }

    protected abstract List<String> clips();

    protected abstract int theme();

    protected abstract int phases();

    protected abstract int arenaRadius();

    protected abstract List<AttackScheduler.Option<LuciferEntity>> attacks(int phase);

    protected abstract int gap(int phase);

    /** Its sounds by id ({@code hurt}, {@code death}, …). */
    protected abstract Map<String, DeferredHolder<SoundEvent, SoundEvent>> sounds();

    /** The spoils, one set per fighter (or dropped where it fell if nobody is known to have fought it). */
    protected abstract List<ItemStack> spoils();

    /** Advancements granted by code to every fighter on its defeat (the kill advancement, if it has one, needs none). */
    protected List<String> grantsOnDefeat() {
        return List.of();
    }

    /** What happens after it falls (the road moves on), for every fighter still in the world. */
    protected void afterDefeat(ServerLevel level, List<ServerPlayer> fighters) {
    }

    /** The clip it plays as it moves into {@code phase}, or null. */
    protected @Nullable String transitionClip(int phase) {
        return null;
    }

    protected ChatFormatting colour(int phase) {
        return ChatFormatting.GOLD;
    }

    // --- shared plumbing ---------------------------------------------------------------------------------------------------

    protected SoundEvent sound(String id, SoundEvent fallback) {
        DeferredHolder<SoundEvent, SoundEvent> h = sounds().get(rig() + "." + id);
        return h == null ? fallback : h.get();
    }

    @Override
    public int maxPhase() {
        return phases();
    }

    @Override
    protected float threshold(int phase) {
        return phase >= phases() ? 0f : 1f - phase / (float) phases();
    }

    @Override
    protected double healthPerExtraPlayer() {
        return SNConfig.CROWLEY_HEALTH_PER_PLAYER.get();
    }

    @Override
    protected float mundaneMultiplier() {
        return SNConfig.CROWLEY_MUNDANE_MULTIPLIER.get().floatValue();
    }

    @Override
    protected float damageFactor() {
        return SNConfig.CROWLEY_DAMAGE_FACTOR.get().floatValue();
    }

    @Override
    public boolean isAerialPhase() {
        return false;
    }

    @Override
    public float scale(int phase) {
        return 1f;
    }

    @Override
    protected int emergeTicks() {
        return 40;
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return attacks(phase);
    }

    @Override
    protected int baseGap(int phase) {
        return gap(phase);
    }

    @Override
    protected String animationPrefix() {
        return "animation." + rig() + ".";
    }

    @Override
    protected List<String> triggeredAnimations() {
        return clips();
    }

    /** {@code entity.supernaturalcraft.<rig>.bar.phase<n>}: the season HUD recognises the {@code .bar} prefix. */
    @Override
    protected String bossBarKey(int phase) {
        return "entity.supernaturalcraft." + key() + ".bar.phase" + phase;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return BossEvent.BossBarColor.YELLOW;
    }

    @Override
    protected Component bossBarName(int phase) {
        return Component.translatable(bossBarKey(phase)).withStyle(colour(phase));
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return ParticleTypes.END_ROD;
    }

    @Override
    protected SoundEvent emergeSound() {
        return sound("emerge", SoundEvents.EVOKER_PREPARE_SUMMON);
    }

    @Override
    protected SoundEvent roarSound() {
        return sound("roar", SoundEvents.EVOKER_PREPARE_ATTACK);
    }

    @Override
    protected SoundEvent transformSound() {
        return sound("transform", SoundEvents.BEACON_POWER_SELECT);
    }

    @Override
    protected SoundEvent ambientBossSound() {
        return sound("ambient", SoundEvents.AMBIENT_CAVE.value());
    }

    @Override
    protected SoundEvent dyingSound() {
        return sound("death", SoundEvents.WITHER_DEATH);
    }

    @Override
    protected SoundEvent deflectSound() {
        return sound("hurt", SoundEvents.SHIELD_BLOCK);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return sound("hurt", SoundEvents.PLAYER_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return sound("death", SoundEvents.WITHER_DEATH);
    }

    @Override
    protected void clientEmergenceParticles() {
        level().addParticle(ParticleTypes.END_ROD, getRandomX(0.6), getY() + random.nextDouble() * 2, getRandomZ(0.6), 0, 0.02, 0);
    }

    @Override
    protected void dyingParticles(ServerLevel level, boolean last) {
        if (last) {
            level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 80, 0.5, 1.0, 0.5, 0.12);
            return;
        }
        level.sendParticles(ParticleTypes.END_ROD, getX(), getEyeY(), getZ(), 3, 0.2, 0.2, 0.2, 0.02);
    }

    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        return LuciferSummoning.openArena(level, blockPosition(), arenaRadius(), theme());
    }

    @Override
    protected void playEmergence() {
        title(1);
    }

    @Override
    protected void playTransition(int to) {
        title(to);
        String clip = transitionClip(to);
        if (clip != null) triggerAnim("action", clip);
    }

    @Override
    protected void playDeath() {
    }

    @Override
    protected int transitionTicks(int to) {
        return 40;
    }

    @Override
    protected void tickTransitionMotion(int elapsed, boolean last) {
        setDeltaMovement(0, getDeltaMovement().y, 0);
    }

    @Override
    protected void leaveBehind(ServerLevel level, Vec3 at) {
    }

    @Override
    protected void returnToCage(ServerLevel level, String messageKey) {
        super.returnToCage(level, "message.supernaturalcraft." + key() + ".left");
    }

    /** A title card for {@code phase} to everyone in the fight. */
    protected void title(int phase) {
        Component t = Component.translatable("boss.supernaturalcraft." + key() + ".phase" + phase).withStyle(colour(phase));
        Component s = Component.translatable("boss.supernaturalcraft." + key() + ".phase" + phase + ".sub").withStyle(ChatFormatting.GRAY);
        for (ServerPlayer p : challengers()) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
            p.connection.send(new ClientboundSetTitleTextPacket(t));
            p.connection.send(new ClientboundSetSubtitleTextPacket(s));
        }
    }

    /** Everyone known to have fought it who is in the world now. */
    protected List<ServerPlayer> fighters(ServerLevel level) {
        List<ServerPlayer> out = new java.util.ArrayList<>();
        for (UUID u : BossShares.fighters(level, this, 48)) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(u);
            if (p != null) out.add(p);
        }
        return out;
    }

    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        Vec3 at = position();
        for (ItemStack stack : spoils()) {
            if (!BossShares.share(level, this, stack)) level.addFreshEntity(new ItemEntity(level, at.x, at.y + 0.5, at.z, stack));
        }
        List<ServerPlayer> fighters = fighters(level);
        for (ServerPlayer p : fighters) {
            for (String adv : grantsOnDefeat()) ChorusRewards.award(p, adv);
            p.displayClientMessage(Component.translatable("message.supernaturalcraft." + key() + ".defeated").withStyle(ChatFormatting.GOLD), false);
        }
        afterDefeat(level, fighters);
    }

    // --- animation ---------------------------------------------------------------------------------------------------------

    /** The loop it stands in while aloft ({@code hover_idle}/{@code float_idle}), or null. */
    protected @Nullable String hoverClip() {
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = animationPrefix();
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + "idle"), walk = RawAnimation.begin().thenLoop(pre + "walk");
        RawAnimation hover = hoverClip() == null ? null : RawAnimation.begin().thenLoop(pre + hoverClip());
        controllers.add(new AnimationController<>(this, "base", 5, state -> {
            if (state() == DYING) return PlayState.STOP;
            if (hover != null && isAerialPhase()) return state.setAndContinue(hover);
            return state.setAndContinue(state.isMoving() ? walk : idle);
        }));
        AnimationController<LuciferEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String name : triggeredAnimations()) {
            action.triggerableAnim(name, name.equals("death") || name.equals("expelled")
                    ? RawAnimation.begin().thenPlayAndHold(pre + name)
                    : RawAnimation.begin().thenPlay(pre + name));
        }
        controllers.add(action);
    }
}
