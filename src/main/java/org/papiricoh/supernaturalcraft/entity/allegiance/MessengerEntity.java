package org.papiricoh.supernaturalcraft.entity.allegiance;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceAssets;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceDialogue;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceFx;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.MessengerSchedule;
import org.papiricoh.supernaturalcraft.bowl.page.SpellPageItem;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Heaven's messenger (v0.13): an angel in a borrowed vessel (Castiel's model, with shadow wings) who comes at dawn after a
 * hunter's first victory over Azazel and offers them Grace. He can't be hurt, and leaves when the talk is over.
 * Models and clips: {@code AllegianceAssets.MESSENGER_*}.
 *
 * <p>The talk (dialogue {@code "messenger"}): {@code greeting} → {@code listen} | {@code decline}; {@code offer} →
 * {@code accept} | {@code decline}. Accepting hands over a Vial of Grace and the page to call him again, and earns
 * {@code heeded_the_call}; declining sends him away for three dawns ({@link MessengerSchedule#declined}). Silence is a no.
 */
public class MessengerEntity extends PathfinderMob implements GeoEntity {

    public static final String DIALOGUE = "messenger";
    /** Ticks of his entrance before he speaks; of his fade before he is gone; to answer each line. */
    public static final int APPEAR_TICKS = 40, FADE_TICKS = 40, ANSWER_TICKS = 600;
    /** He leaves a hunter who walks this far away. */
    private static final double LEASH = 24;
    private static final String PREFIX = "animation.messenger.";
    /** Who each messenger came for (one at a time per hunter). */
    private static final Map<UUID, UUID> VISITING = new ConcurrentHashMap<>();

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID hunter;
    private int life;
    private int leavingAt = -1;
    private boolean spoke;

    public MessengerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    /** Called once at start-up: his answers come back through the shared dialogue. */
    public static void registerDialogue() {
        AllegianceDialogue.register(DIALOGUE, (player, speaker, node, option) -> {
            if (speaker instanceof MessengerEntity m) m.answer(player, node, option);
            else if ("decline".equals(option)) Allegiances.set(player, MessengerSchedule.declined(Allegiances.get(player), player.serverLevel().getDayTime()));
        });
    }

    /** He comes to {@code hunter}: a few steps in front of them, facing them. Null if he could not. */
    public static @Nullable MessengerEntity visit(ServerPlayer hunter) {
        ServerLevel level = hunter.serverLevel();
        MessengerEntity m = AllEntities.MESSENGER.get().create(level);
        if (m == null) return null;
        Vec3 ahead = hunter.position().add(Vec3.directionFromRotation(0, hunter.getYRot()).scale(3));
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(ahead));
        double y = Math.abs(top.getY() - hunter.getY()) <= 3 ? top.getY() : hunter.getY();
        m.moveTo(ahead.x, y, ahead.z, hunter.getYRot() + 180, 0);
        m.setYHeadRot(hunter.getYRot() + 180);
        m.hunter = hunter.getUUID();
        level.addFreshEntity(m);
        VISITING.put(hunter.getUUID(), m.getUUID());
        level.playSound(null, m.blockPosition(), AllSounds.LUCIFER_WINGS.get(), SoundSource.NEUTRAL, 1.0f, 0.6f);
        level.sendParticles(AllParticles.GRACE.get(), m.getX(), m.getY() + 1.2, m.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
        return m;
    }

    /** Whether a messenger is with {@code hunter} now. */
    public static boolean visiting(ServerPlayer hunter) {
        UUID id = VISITING.get(hunter.getUUID());
        if (id == null) return false;
        if (hunter.serverLevel().getEntity(id) instanceof MessengerEntity m && m.isAlive()) return true;
        VISITING.remove(hunter.getUUID());
        return false;
    }

    public @Nullable UUID hunter() {
        return hunter;
    }

    public boolean leaving() {
        return leavingAt >= 0;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 12f, 1f));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        life++;
        if (life == 1) triggerAnim("action", "appear");
        ServerPlayer p = hunter == null ? null : (ServerPlayer) level().getPlayerByUUID(hunter);
        if (leavingAt < 0 && (p == null || !p.isAlive() || p.distanceToSqr(this) > LEASH * LEASH || !Allegiances.get(p).isHuman())) {
            leave();
        }
        if (p != null) getLookControl().setLookAt(p, 30, 30);
        if (!spoke && life >= APPEAR_TICKS && leavingAt < 0 && p != null) {
            spoke = true;
            triggerAnim("action", "talk");
            AllegianceDialogue.ask(p, this, DIALOGUE, "greeting", List.of("listen", "decline"), ANSWER_TICKS);
        }
        if (leavingAt >= 0 && life == leavingAt) triggerAnim("action", "fade");
        if (leavingAt >= 0 && life >= leavingAt + FADE_TICKS) {
            ((ServerLevel) level()).sendParticles(AllParticles.GRACE.get(), getX(), getY() + 1.2, getZ(), 40, 0.4, 0.8, 0.4, 0.08);
            if (hunter != null) VISITING.remove(hunter, getUUID());
            discard();
        }
        // A safety net: he never lingers more than a few minutes.
        if (life > 20 * 180 && leavingAt < 0) leave();
    }

    /** The hunter's answer to {@code node}. */
    public void answer(ServerPlayer p, String node, String option) {
        if (hunter == null || !hunter.equals(p.getUUID()) || leaving()) return;
        if ("greeting".equals(node) && "listen".equals(option)) {
            triggerAnim("action", "talk");
            AllegianceDialogue.ask(p, this, DIALOGUE, "offer", List.of("accept", "decline"), ANSWER_TICKS);
        } else if ("offer".equals(node) && "accept".equals(option)) {
            accept(p);
        } else {
            Allegiances.set(p, MessengerSchedule.declined(Allegiances.get(p), p.serverLevel().getDayTime()));
            triggerAnim("action", "nod");
            AllegianceFx.tell(p, "message.supernaturalcraft.allegiance.messenger_declined", false, ChatFormatting.GRAY);
            AllegianceFx.toSelf(p, org.papiricoh.supernaturalcraft.network.AllegianceFxPayload.WHISPER,
                    org.papiricoh.supernaturalcraft.allegiance.power.Passives.WHISPER_MESSENGER_GONE, 0, position(), 60);
            leave();
        }
    }

    /** Heeded: the vial, the page, the advancement, and a hint of what the vial is for. */
    void accept(ServerPlayer p) {
        Allegiance a = Allegiances.get(p);
        Allegiances.set(p, MessengerSchedule.heeded(a));
        triggerAnim("action", "offer_vial");
        give(p, new ItemStack(AllItems.VIAL_OF_GRACE.get()));
        give(p, SpellPageItem.of(SupernaturalCraft.asResource("summon_messenger")));
        ChorusRewards.award(p, "main/heeded_the_call");
        AllegianceFx.tell(p, "message.supernaturalcraft.allegiance.messenger_heeded", false, ChatFormatting.GOLD);
        level().playSound(null, blockPosition(), AllSounds.ALLEGIANCE_HEAL.get(), SoundSource.NEUTRAL, 1.0f, 1.2f);
        leave(30);
    }

    private static void give(ServerPlayer p, ItemStack stack) {
        if (!p.getInventory().add(stack) && !stack.isEmpty()) p.drop(stack, false);
    }

    /** Spreads his wings and fades. */
    public void leave() {
        leave(0);
    }

    private void leave(int delay) {
        if (leavingAt >= 0) return;
        // The fade starts on the next tick (or after the vial changes hands).
        leavingAt = life + Math.max(1, delay);
        if (hunter != null && level() instanceof ServerLevel level && level.getPlayerByUUID(hunter) instanceof ServerPlayer p) {
            AllegianceDialogue.close(p, this);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(source, amount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop(PREFIX + "idle");
        controllers.add(new AnimationController<>(this, "base", 5, state -> state.setAndContinue(idle)));
        AnimationController<MessengerEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String clip : AllegianceAssets.MESSENGER_CLIPS) {
            if (clip.equals("idle")) continue;
            action.triggerableAnim(clip, clip.equals("wing_spread") || clip.equals("fade")
                    ? RawAnimation.begin().thenPlayAndHold(PREFIX + clip) : RawAnimation.begin().thenPlay(PREFIX + clip));
        }
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (hunter != null) tag.putUUID("Hunter", hunter);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        hunter = tag.hasUUID("Hunter") ? tag.getUUID("Hunter") : null;
        // Reloaded mid-visit: the talk is lost, so he goes.
        leavingAt = 1;
    }
}
