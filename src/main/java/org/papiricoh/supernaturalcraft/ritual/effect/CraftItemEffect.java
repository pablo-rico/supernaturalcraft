package org.papiricoh.supernaturalcraft.ritual.effect;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/** The ritual forges an item, which rises out of the altar. */
public record CraftItemEffect(ItemStack result, boolean bindToRitualist) implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("craft_item");
    public static final MapCodec<CraftItemEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CraftItemEffect::result),
            com.mojang.serialization.Codec.BOOL.optionalFieldOf("bind_to_ritualist", false).forGetter(CraftItemEffect::bindToRitualist)
    ).apply(i, CraftItemEffect::new));

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, ServerPlayer ritualist) {
        ItemStack made = result.copy();
        if (bindToRitualist && ritualist != null) {
            // Cursed weapons answer only to the one who forged them.
            made.set(org.papiricoh.supernaturalcraft.registry.AllDataComponents.CURSE,
                    made.getOrDefault(org.papiricoh.supernaturalcraft.registry.AllDataComponents.CURSE,
                            org.papiricoh.supernaturalcraft.weapon.curse.CurseState.FRESH).boundTo(ritualist.getUUID()));
        }
        ItemEntity item = new ItemEntity(level, altar.getX() + 0.5, altar.getY() + 1.2, altar.getZ() + 0.5, made);
        item.setDeltaMovement(0, 0.15, 0);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
        level.sendParticles(ParticleTypes.END_ROD, altar.getX() + 0.5, altar.getY() + 1.3, altar.getZ() + 0.5, 20, 0.2, 0.3, 0.2, 0.05);
        return true;
    }

    @Override
    public ItemStack displayResult() {
        return result;
    }
}
