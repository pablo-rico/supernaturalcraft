package org.papiricoh.supernaturalcraft.reward.raphael;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.magic.item.GrimoireItem;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.weapon.ascension.Ascension;
import org.papiricoh.supernaturalcraft.weapon.catalyst.CatalystItem;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Raphael's Stormcaller (v0.16): a tier IV holy catalyst he leaves. Held opposite a grimoire it steps aside like any catalyst;
 * its own spell is lightning that leaps from the foe it is pointed at to up to {@link #CHAIN_TARGETS} foes in all, each within
 * {@link #CHAIN_REACH} of the last; sneaking, it lays a healing grace on its bearer and the allies near them, once every
 * {@link #HEAL_COOLDOWN} ticks. Both scale with Ascension.
 */
public class StormcallerItem extends CatalystItem implements GeoItem {

    public static final int CHAIN_TARGETS = 3;
    public static final double RANGE = 18, CHAIN_REACH = 6;
    public static final float ZAP_DAMAGE = 8f, ZAP_DECAY = 0.85f;
    public static final float HEAL = 4f, HEAL_RADIUS = 6f, HEAL_MANA = 20f;
    public static final int HEAL_COOLDOWN = 200, REGEN_TICKS = 80;
    /** Where the bearer's last grace is remembered (game time), in their persistent data. */
    private static final String LAST_HEAL = "supernaturalcraft:stormcaller_heal";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public StormcallerItem(Properties properties) {
        super(properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    protected float ownMana() {
        return 8f;
    }

    @Override
    protected int ownCooldown() {
        return 24;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || GrimoireItem.heldHand(player) != null) return super.use(level, player, hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.consume(stack);
        if (!grace(sp, stack)) return InteractionResultHolder.fail(stack);
        sp.swing(hand, true);
        return InteractionResultHolder.success(stack);
    }

    /** Its own spell: the chain lightning. False (no mana spent) if nothing is in reach. */
    @Override
    protected boolean ownSpell(ServerPlayer player, ItemStack stack) {
        LivingEntity first = aim(player);
        if (first == null) return false;
        List<LivingEntity> struck = chain(player, stack, first);
        if (struck.isEmpty()) return false;
        play(player, stack, "zap");
        return true;
    }

    /** The foe it is pointed at, within {@link #RANGE}, or null. */
    public static @Nullable LivingEntity aim(Player player) {
        Vec3 from = player.getEyePosition(), to = from.add(player.getLookAngle().scale(RANGE));
        BlockHitResult block = player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, from, end,
                player.getBoundingBox().expandTowards(player.getLookAngle().scale(RANGE)).inflate(1),
                (Entity e) -> e instanceof LivingEntity l && isFoe(player, l), RANGE * RANGE);
        return hit != null && hit.getEntity() instanceof LivingEntity l ? l : null;
    }

    static boolean isFoe(Player player, LivingEntity e) {
        return e.isAlive() && !(e instanceof ArmorStand) && !ResolvedSpell.isFriend(player, e) && !e.isSpectator();
    }

    /** The lightning leaps from {@code first} to the nearest foes in turn. @return everyone it struck, in order */
    public static List<LivingEntity> chain(ServerPlayer player, ItemStack stack, LivingEntity first) {
        ServerLevel level = player.serverLevel();
        List<LivingEntity> struck = new ArrayList<>();
        LivingEntity at = first;
        Vec3 from = player.getEyePosition().add(player.getLookAngle().scale(0.8)).subtract(0, 0.3, 0);
        float damage = Ascension.scale(stack, ZAP_DAMAGE);
        while (at != null && struck.size() < CHAIN_TARGETS) {
            struck.add(at);
            Vec3 to = at.position().add(0, at.getBbHeight() / 2, 0);
            arc(level, from, to);
            at.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, player, player), Ascension.vsBoss(player, at, damage));
            damage *= ZAP_DECAY;
            from = to;
            LivingEntity next = null;
            double best = CHAIN_REACH * CHAIN_REACH;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at.position(), at.position()).inflate(CHAIN_REACH),
                    e -> isFoe(player, e) && !struck.contains(e))) {
                double d = e.distanceToSqr(at);
                if (d <= best) {
                    best = d;
                    next = e;
                }
            }
            at = next;
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(first.getX(), first.getY(), first.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        level.playSound(null, player.blockPosition(), AllSounds.STORMCALLER_ZAP.get(), SoundSource.PLAYERS, 1.2f, 1.0f);
        return struck;
    }

    private static void arc(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        int n = Math.max(2, (int) (d.length() * 2));
        for (int i = 0; i <= n; i++) {
            Vec3 p = from.add(d.scale(i / (double) n));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.06, 0.06, 0.06, 0.02);
        }
    }

    /**
     * Sneaking: a healing grace on the bearer and every ally within {@link #HEAL_RADIUS} (players, their pets).
     * False if it is still gathering or the bearer lacks the mana. @return whether it was laid
     */
    public static boolean grace(ServerPlayer player, ItemStack stack) {
        long now = player.level().getGameTime();
        long last = player.getPersistentData().getLong(LAST_HEAL);
        if (last > 0 && now - last < HEAL_COOLDOWN && now >= last) {
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.stormcaller.gathering").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        if (!player.getAbilities().instabuild && !ManaManager.tryConsume(player, HEAL_MANA)) {
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.cast.no_mana").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        player.getPersistentData().putLong(LAST_HEAL, now);
        float heal = Ascension.scale(stack, HEAL);
        ServerLevel level = player.serverLevel();
        mend(level, player, heal);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(HEAL_RADIUS),
                e -> e != player && e.isAlive() && ResolvedSpell.isFriend(player, e))) {
            mend(level, e, heal);
        }
        level.playSound(null, player.blockPosition(), AllSounds.STORMCALLER_HEAL.get(), SoundSource.PLAYERS, 1.2f, 1.0f);
        if (stack.getItem() instanceof StormcallerItem item) item.play(player, stack, "heal");
        return true;
    }

    private static void mend(ServerLevel level, LivingEntity e, float heal) {
        e.heal(heal);
        e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_TICKS, 0));
        level.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + 1, e.getZ(), 12, 0.4, 0.6, 0.4, 0.02);
    }

    /** Plays one of its clips on this exact stack for everyone watching. */
    public void play(LivingEntity holder, ItemStack stack, String clip) {
        if (holder.level() instanceof ServerLevel level) triggerAnim(holder, GeoItem.getOrAssignId(stack, level), "main", clip);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("animation.raphaels_stormcaller.idle");
        controllers.add(new AnimationController<>(this, "main", 3, s -> s.setAndContinue(idle))
                .triggerableAnim("zap", RawAnimation.begin().thenPlay("animation.raphaels_stormcaller.zap"))
                .triggerableAnim("heal", RawAnimation.begin().thenPlay("animation.raphaels_stormcaller.heal")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
