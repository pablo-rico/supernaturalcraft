package org.papiricoh.supernaturalcraft.weapon.catalyst;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.light.TempLights;
import org.papiricoh.supernaturalcraft.magic.item.GrimoireItem;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellContext;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Tier III catalyst: a censer of burning grace. Holy spells strike harder, a Burst lingers where
 * it went off, a Ward spreads wider. On its own it opens and pours a beam of light: it burns what
 * it touches, harder the longer it holds, costs mana every tick, and lights the ground it hits.
 */
public class CenserOfGraceItem extends CatalystItem implements GeoItem {

    public static final float HOLY_POTENCY = 1.25f, WARD_SCALE = 1.5f, RANGE = 16f, MANA_PER_TICK = 1f;
    public static final int LINGER = 100, LIGHT_LEVEL = 12, LIGHT_TICKS = 40;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CenserOfGraceItem(Properties properties) {
        super(properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void shape(SpellContext ctx, ItemStack stack, ResolvedSpell spell) {
        if (Catalyst.hasEffect(spell, "smite") || Catalyst.hasEffect(spell, "mend") || Catalyst.hasEffect(spell, "exorcise")) {
            ctx.potency *= HOLY_POTENCY;
        }
        ctx.traits = ctx.traits.withBurstLinger(Math.max(ctx.traits.burstLingerTicks(), LINGER))
                .withWardScale(ctx.traits.wardScale() * WARD_SCALE);
    }

    // The beam is channelled, so the CatalystItem one-shot path is not used.
    @Override
    protected float ownMana() {
        return MANA_PER_TICK;
    }

    @Override
    protected int ownCooldown() {
        return 10;
    }

    @Override
    protected boolean ownSpell(ServerPlayer player, ItemStack stack) {
        return false;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (GrimoireItem.heldHand(player) != null) return InteractionResultHolder.pass(stack);
        if (!player.getAbilities().instabuild && ManaManager.get(player).mana() < MANA_PER_TICK * 10) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        if (level instanceof ServerLevel server) triggerAnim(player, GeoItem.getOrAssignId(stack, server), "main", "beam");
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(user instanceof ServerPlayer player)) return;
        int held = getUseDuration(stack, user) - remaining;
        if (!ManaManager.tryConsume(player, MANA_PER_TICK)) {
            player.stopUsingItem();
            return;
        }
        HitResult hit = trace(player);
        ServerLevel server = player.serverLevel();
        if (held % 4 == 0 && hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity target
                && !ResolvedSpell.isFriend(player, target)) {
            target.hurt(AllDamageTypes.source(server, AllDamageTypes.SMITE, player),
                    org.papiricoh.supernaturalcraft.weapon.ascension.Ascension.scale(stack, beamDamage(held)));
        }
        if (held % 5 == 0) {
            BlockPos at = hit instanceof BlockHitResult bhr ? bhr.getBlockPos().relative(bhr.getDirection()) : BlockPos.containing(hit.getLocation());
            TempLights.place(server, at, LIGHT_LEVEL, LIGHT_TICKS);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (level instanceof ServerLevel server) stopTriggeredAnim(user, GeoItem.getOrAssignId(stack, server), "main", "beam");
    }

    /** Damage per pulse: one, rising to three over the first two seconds. */
    public static float beamDamage(int heldTicks) {
        return Math.min(3f, 1f + heldTicks / 20f);
    }

    /** Where the beam lands: the first entity or block along the look, up to RANGE. Shared with the client renderer. */
    public static HitResult trace(Player player) {
        Vec3 from = player.getEyePosition(), to = from.add(player.getLookAngle().scale(RANGE));
        BlockHitResult block = player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
        @Nullable EntityHitResult entity = ProjectileUtil.getEntityHitResult(player, from, end,
                player.getBoundingBox().expandTowards(player.getLookAngle().scale(RANGE)).inflate(1),
                (Entity e) -> e.isPickable() && !e.isSpectator(), RANGE * RANGE);
        return entity != null ? entity : block;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("animation.censer_of_grace.idle");
        controllers.add(new AnimationController<>(this, "main", 3, s -> s.setAndContinue(idle))
                .triggerableAnim("beam", RawAnimation.begin().thenPlayAndHold("animation.censer_of_grace.beam")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
