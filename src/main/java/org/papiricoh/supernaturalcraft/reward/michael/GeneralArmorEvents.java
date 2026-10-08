package org.papiricoh.supernaturalcraft.reward.michael;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * The General's armour, worn whole: holy harm only lands by half, and a wing of light takes one blow outright every
 * {@link #WARD_TICKS} (its sound and a burst of light say so).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class GeneralArmorEvents {

    public static final int WARD_TICKS = 600;
    public static final float HOLY_SHARE = 0.5f;
    private static final String WARD_READY_AT = "sn_general_ward";

    private GeneralArmorEvents() {
    }

    /** Whether {@code e} wears all four pieces. */
    public static boolean wearsSet(LivingEntity e) {
        return is(e, EquipmentSlot.HEAD, AllItems.GENERAL_HELMET.get()) && is(e, EquipmentSlot.CHEST, AllItems.GENERAL_CHESTPLATE.get())
                && is(e, EquipmentSlot.LEGS, AllItems.GENERAL_LEGGINGS.get()) && is(e, EquipmentSlot.FEET, AllItems.GENERAL_BOOTS.get());
    }

    private static boolean is(LivingEntity e, EquipmentSlot slot, Item item) {
        return e.getItemBySlot(slot).is(item);
    }

    /** Whether the wing of light is ready to take a blow. */
    public static boolean wardReady(LivingEntity e) {
        return e.level().getGameTime() >= e.getPersistentData().getLong(WARD_READY_AT);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide || !wearsSet(e) || event.getAmount() <= 0) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (wardReady(e) && event.getSource().getEntity() != null) {
            e.getPersistentData().putLong(WARD_READY_AT, e.level().getGameTime() + WARD_TICKS);
            event.setCanceled(true);
            e.level().playSound(null, e.blockPosition(), AllSounds.GENERAL_ARMOR_WARD.get(), SoundSource.PLAYERS, 1.2f, 1.0f);
            if (e.level() instanceof net.minecraft.server.level.ServerLevel level) {
                level.sendParticles(AllParticles.GRACE.get(), e.getX(), e.getY() + 1.2, e.getZ(), 40, 0.6, 0.6, 0.6, 0.1);
            }
            if (e instanceof ServerPlayer p) {
                p.displayClientMessage(Component.translatable("message.supernaturalcraft.michael.ward").withStyle(ChatFormatting.GOLD), true);
            }
            return;
        }
        if (event.getSource().is(AllTags.DamageTypes.HOLY)) event.setAmount(event.getAmount() * HOLY_SHARE);
    }
}
