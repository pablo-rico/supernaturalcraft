package org.papiricoh.supernaturalcraft.nephilim.lance;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRegistry;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/**
 * v0.41: the Lance of Michael made whole: the spear Ramiel guarded, the one that truly hurts an archangel. Asmodeus took it and
 * leaves it ({@link #whole}): Michael's Lance marked {@code archangel_bane}, whose blows hurt an archangel ({@link #ARCHANGELS})
 * {@link #MULTIPLIER} times as much.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class LanceBane {

    public static final TagKey<EntityType<?>> ARCHANGELS = TagKey.create(Registries.ENTITY_TYPE, SupernaturalCraft.asResource("archangels"));
    public static final float MULTIPLIER = 1.5f;
    /** Granted with it (Asmodeus's fall). */
    public static final String ADVANCEMENT = "main/lance_of_michael";

    private LanceBane() {
    }

    /** A Lance of Michael, already whole. */
    public static ItemStack whole() {
        ItemStack lance = new ItemStack(AllItems.MICHAEL_LANCE.get());
        apply(lance);
        return lance;
    }

    public static boolean has(ItemStack weapon) {
        return Boolean.TRUE.equals(weapon.get(NephilimRegistry.ARCHANGEL_BANE.get()));
    }

    public static void apply(ItemStack weapon) {
        weapon.set(NephilimRegistry.ARCHANGEL_BANE.get(), true);
        weapon.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (!victim.getType().is(ARCHANGELS) || !(event.getSource().getDirectEntity() instanceof LivingEntity attacker)) return;
        if (has(attacker.getMainHandItem())) event.setAmount(event.getAmount() * MULTIPLIER);
    }

    /** Its line under the lance's name (client tooltips call this). */
    public static @Nullable Component tooltip(ItemStack stack) {
        return has(stack) ? Component.translatable("tooltip.supernaturalcraft.archangel_bane").withStyle(ChatFormatting.AQUA) : null;
    }
}
