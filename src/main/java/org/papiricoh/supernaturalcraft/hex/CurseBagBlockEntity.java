package org.papiricoh.supernaturalcraft.hex;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllBlockEntities;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A hidden curse bag. Every {@link HexBags#PULSE} ticks it afflicts the players and villagers
 * within {@link HexBags#RADIUS} blocks, all but its maker and anyone carrying a protection bag.
 * Remembers its {@link HexBag} (who made it) when picked up and set down again.
 */
public class CurseBagBlockEntity extends BlockEntity {

    @Nullable
    private HexBag bag;

    public CurseBagBlockEntity(BlockPos pos, BlockState state) {
        super(AllBlockEntities.CURSE_BAG.get(), pos, state);
    }

    public Optional<UUID> maker() {
        return bag == null ? Optional.empty() : Optional.of(bag.maker());
    }

    public void setMaker(UUID maker) {
        bag = new HexBag(maker, 0);
        setChanged();
    }

    /**
     * One pulse over {@code candidates} (only those within reach count).
     *
     * @return how many were afflicted
     */
    public int pulse(Collection<? extends LivingEntity> candidates, boolean misfortune) {
        Vec3 center = Vec3.atCenterOf(worldPosition);
        UUID maker = bag == null ? null : bag.maker();
        int n = 0;
        for (LivingEntity e : candidates) {
            if (e.distanceToSqr(center) <= HexBags.RADIUS * HexBags.RADIUS
                    && HexBags.afflict(e, maker, 1, HexBags.JINX_TICKS, misfortune)) n++;
        }
        return n;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CurseBagBlockEntity be) {
        if (Math.floorMod(level.getGameTime() + pos.hashCode(), HexBags.PULSE) != 0) return;
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(HexBags.RADIUS),
                e -> e instanceof Player || e instanceof AbstractVillager);
        if (!near.isEmpty()) be.pulse(near, true);
    }

    // --- components & persistence -----------------------------------------------------------

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        HexBag fromItem = input.get(AllDataComponents.HEX_BAG.get());
        if (fromItem != null) bag = fromItem;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (bag != null) builder.set(AllDataComponents.HEX_BAG.get(), bag);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("bag");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (bag != null) {
            HexBag.CODEC.encodeStart(NbtOps.INSTANCE, bag)
                    .resultOrPartial(e -> SupernaturalCraft.LOGGER.warn("Curse bag not saved: {}", e))
                    .ifPresent(t -> tag.put("bag", t));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        bag = tag.contains("bag") ? HexBag.CODEC.parse(NbtOps.INSTANCE, tag.get("bag")).result().orElse(null) : null;
    }
}
