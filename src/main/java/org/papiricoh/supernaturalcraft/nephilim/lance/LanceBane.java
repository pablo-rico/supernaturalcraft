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
 * v0.41: the Lance of Michael made whole. Asmodeus kept a shard of the spear Ramiel guarded ({@code archangel_bane_shard});
 * worked into Michael's Lance at the Hellforge (the shard in the shard slot, ascend), it marks the lance
 * ({@code archangel_bane}) and its blows hurt an archangel ({@link #ARCHANGELS}) {@link #MULTIPLIER} times as much.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class LanceBane {

    public static final TagKey<EntityType<?>> ARCHANGELS = TagKey.create(Registries.ENTITY_TYPE, SupernaturalCraft.asResource("archangels"));
    public static final float MULTIPLIER = 1.5f;
    /** Levels the work costs. */
    public static final int COST = 10;

    private LanceBane() {
    }

    public static boolean isShard(ItemStack stack) {
        return stack.is(NephilimRegistry.ARCHANGEL_BANE_SHARD.get());
    }

    public static boolean has(ItemStack weapon) {
        return Boolean.TRUE.equals(weapon.get(NephilimRegistry.ARCHANGEL_BANE.get()));
    }

    /** Why the shard cannot be worked into {@code weapon}, or null (a Hellforge problem key, as {@code Ascension.problem}). */
    public static @Nullable String problem(ItemStack weapon, @Nullable net.minecraft.world.entity.player.Player player) {
        if (weapon.isEmpty()) return "ascend_no_item";
        if (!weapon.is(AllItems.MICHAEL_LANCE.get())) return "bane_lance_only";
        if (has(weapon)) return "bane_done";
        if (player != null && !player.getAbilities().instabuild && player.experienceLevel < COST) return "ascend_no_xp";
        return null;
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
