package org.papiricoh.supernaturalcraft.weapon.melee;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

import java.util.List;

/**
 * Penumbra, taken from the Darkness. It drinks light: slowly from bright places, quickly from lit
 * foes it strikes. Hold use and let go to pour it all out as an Umbra Wave that blinds and wounds
 * everything ahead. In the dark it bites deeper.
 */
public class PenumbraItem extends GeoSwordItem {

    public static final int MAX = 100, PER_SECOND = 1, PER_HIT = 3, BRIGHT = 10, DARK = 4, CHARGE_TICKS = 20, COOLDOWN = 60;
    public static final float DARK_BONUS = 1.3f, WAVE_RANGE = 9, WAVE_ANGLE = 70;

    public PenumbraItem(Tier tier, Properties properties) {
        super("penumbra", tier, properties.component(AllDataComponents.PENUMBRA_LIGHT, 0), List.of("charge"), List.of("release"));
    }

    public static int light(ItemStack stack) {
        return stack.getOrDefault(AllDataComponents.PENUMBRA_LIGHT, 0);
    }

    public static void setLight(ItemStack stack, int value) {
        stack.set(AllDataComponents.PENUMBRA_LIGHT, Math.max(0, Math.min(MAX, value)));
    }

    public static int lightAt(Entity e) {
        return e.level().getBrightness(LightLayer.BLOCK, BlockPos.containing(e.getEyePosition()));
    }

    /** Damage from a wave carrying {@code light}. */
    public static float waveDamage(int light) {
        return light / 5f;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && selected && level.getGameTime() % 20 == 0 && lightAt(entity) >= BRIGHT) {
            setLight(stack, light(stack) + PER_SECOND);
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (lightAt(target) >= BRIGHT) setLight(stack, light(stack) + PER_HIT);
        return super.hurtEnemy(stack, target, attacker);
    }

    /** In the dark (block light under 4 where the wielder stands) its blows land harder. */
    public static void onIncomingDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getSource().getDirectEntity() instanceof LivingEntity attacker && event.getSource().getEntity() == attacker
                && attacker.getMainHandItem().getItem() instanceof PenumbraItem && lightAt(attacker) < DARK) {
            event.setAmount(event.getAmount() * DARK_BONUS);
        }
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
        if (light(stack) < 5) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        if (player instanceof ServerPlayer sp) play(sp, stack, "charge");
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (!(user instanceof ServerPlayer player)) return;
        stopPlaying(player, stack, "charge");
        if (getUseDuration(stack, user) - timeLeft < CHARGE_TICKS) return;
        wave(player, stack);
    }

    /** Pours out every drop of stored light as a cone of blinding dark. Public for tests. */
    public static int wave(ServerPlayer player, ItemStack stack) {
        int light = light(stack);
        if (light <= 0) return 0;
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        int struck = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, eye).inflate(WAVE_RANGE), e -> e != player && e.isAlive())) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            if (to.length() > WAVE_RANGE || Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, to.normalize().dot(look))))) > WAVE_ANGLE / 2) continue;
            e.hurt(AllDamageTypes.source(level, AllDamageTypes.VOID, player),
                    org.papiricoh.supernaturalcraft.weapon.ascension.Ascension.scale(stack, waveDamage(light)));
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60 + light, 0), player);
            struck++;
        }
        for (int i = 1; i <= 8; i++) {
            Vec3 p = eye.add(look.scale(i));
            level.sendParticles(AllParticles.VOID_MOTE.get(), p.x, p.y, p.z, 6 + i, i * 0.25, i * 0.2, i * 0.25, 0.02);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 1.4f);
        setLight(stack, 0);
        ((PenumbraItem) stack.getItem()).play(player, stack, "release");
        player.getCooldowns().addCooldown(stack.getItem(), COOLDOWN);
        return struck;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.penumbra.light", light(stack), MAX).withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return light(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * light(stack) / MAX);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xB48CFF;
    }
}
