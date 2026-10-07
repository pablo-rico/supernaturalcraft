package org.papiricoh.supernaturalcraft.weapon.catalyst;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellContext;
import org.papiricoh.supernaturalcraft.network.SNNetworking;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.weapon.curse.CurseLevels;
import org.papiricoh.supernaturalcraft.weapon.curse.CurseState;
import org.papiricoh.supernaturalcraft.weapon.curse.Curses;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * Tier IV, cursed catalyst. Spells cast through it cost no mana: they cost blood and sanity
 * instead, and it grows hungry for what those spells kill. Touch chains, Bolts pierce (L3+), and
 * alone it speaks a Forbidden Word that marks its target for harm.
 */
public class WhisperingCodexItem extends CatalystItem implements GeoItem {

    public static final float MANA_PER_HEART = 12f, SANITY_PER_MANA = 0.25f, POTENCY_PER_LEVEL = 0.05f;
    public static final float LOW_SANITY = 30f, MISFIRE_CHANCE = 0.10f, WORD_SANITY = 10f, WORD_HEALTH = 2f;
    public static final int WORD_TICKS = 100;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public WhisperingCodexItem(Properties properties) {
        super(properties.component(AllDataComponents.CURSE, CurseState.FRESH));
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public boolean paysOwnCost(ItemStack stack) {
        return true;
    }

    @Override
    public void pay(ServerPlayer player, ItemStack stack, float mana) {
        player.hurt(player.damageSources().magic(), mana / MANA_PER_HEART);
        ArcanaData arcana = ManaManager.get(player);
        arcana.setSanity(arcana.sanity() - mana * SANITY_PER_MANA);
        SNNetworking.syncArcana(player);
        if (player.level() instanceof ServerLevel level) triggerAnim(player, GeoItem.getOrAssignId(stack, level), "main", "cast");
    }

    @Override
    public void shape(SpellContext ctx, ItemStack stack, ResolvedSpell spell) {
        CurseState s = Curses.state(stack);
        ctx.potency *= 1f + POTENCY_PER_LEVEL * s.level();
        ctx.traits = ctx.traits.withTouchChain(Math.max(ctx.traits.touchChain(), 2));
        if (s.level() >= 3) ctx.traits = ctx.traits.withBoltPierce(Math.max(ctx.traits.boltPierce(), 2));
        if (ctx.caster instanceof ServerPlayer p && ManaManager.get(p).sanity() < LOW_SANITY && p.getRandom().nextFloat() < MISFIRE_CHANCE) {
            // The words twist: the spell still goes out, but something comes back.
            p.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1));
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.codex.misfire").withStyle(ChatFormatting.DARK_PURPLE), true);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (entity instanceof ServerPlayer player) Curses.tickCarried(player, stack);
    }

    @Override
    protected float ownMana() {
        return 0f;
    }

    @Override
    protected int ownCooldown() {
        return 100;
    }

    /** The Forbidden Word: marks what you look at; it takes a fifth more harm from everything. */
    @Override
    protected boolean ownSpell(ServerPlayer player, ItemStack stack) {
        HitResult hit = CenserOfGraceItem.trace(player);
        if (!(hit instanceof EntityHitResult ehr) || !(ehr.getEntity() instanceof LivingEntity target)) return false;
        target.addEffect(new MobEffectInstance(AllMobEffects.MARKED, WORD_TICKS, 0), player);
        player.hurt(player.damageSources().magic(), WORD_HEALTH);
        ArcanaData arcana = ManaManager.get(player);
        arcana.setSanity(arcana.sanity() - WORD_SANITY);
        SNNetworking.syncArcana(player);
        player.serverLevel().sendParticles(AllParticles.SIGIL.get(), target.getX(), target.getEyeY() + 0.5, target.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
        player.serverLevel().playSound(null, target.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.6f, 1.6f);
        triggerAnim(player, GeoItem.getOrAssignId(stack, player.serverLevel()), "main", "cast");
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        CurseState s = Curses.state(stack);
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.curse.state", s.level(), s.souls(), s.satiation())
                .withStyle(CurseLevels.starving(s.satiation()) ? ChatFormatting.DARK_RED : ChatFormatting.DARK_PURPLE));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("animation.whispering_codex.idle");
        controllers.add(new AnimationController<>(this, "main", 3, st -> st.setAndContinue(idle))
                .triggerableAnim("cast", RawAnimation.begin().thenPlay("animation.whispering_codex.cast")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
