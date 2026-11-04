package org.papiricoh.supernaturalcraft.crypt;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.supernaturalcraft.registry.AllBlockEntities;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Who has taken their Demon Tablet from this pedestal (v0.36). */
public class TabletPedestalBlockEntity extends BlockEntity {

    private final Set<UUID> taken = new HashSet<>();

    public TabletPedestalBlockEntity(BlockPos pos, BlockState state) {
        super(AllBlockEntities.TABLET_PEDESTAL.get(), pos, state);
    }

    /** @return whether {@code player} had not taken theirs yet (and now has) */
    public boolean take(UUID player) {
        if (!taken.add(player)) return false;
        setChanged();
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        taken.forEach(u -> list.add(NbtUtils.createUUID(u)));
        tag.put("Taken", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        taken.clear();
        for (Tag t : tag.getList("Taken", Tag.TAG_INT_ARRAY)) taken.add(NbtUtils.loadUUID(t));
    }
}
