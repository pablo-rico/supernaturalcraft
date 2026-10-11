package org.papiricoh.supernaturalcraft.legacy.cases.table;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.papiricoh.supernaturalcraft.legacy.cases.MapTableBlock;
import org.papiricoh.supernaturalcraft.registry.AllBlockEntities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The heart of a formed war table (v0.19), on its centre piece only: the pins stuck in it, shared by everyone who uses the
 * table and lost if it is broken up. The hologram itself is drawn from it on the client.
 */
public class WarTableBlockEntity extends BlockEntity {

    private final List<TablePin> pins = new ArrayList<>();

    public WarTableBlockEntity(BlockPos pos, BlockState state) {
        super(AllBlockEntities.WAR_TABLE.get(), pos, state);
    }

    public List<TablePin> pins() {
        return Collections.unmodifiableList(pins);
    }

    /** @return false if the table already holds {@link TablePin#MAX} pins */
    public boolean addPin(TablePin pin) {
        if (pins.size() >= TablePin.MAX) return false;
        pins.add(pin);
        changed();
        return true;
    }

    public boolean renamePin(int index, String name) {
        if (index < 0 || index >= pins.size()) return false;
        TablePin p = pins.get(index);
        pins.set(index, new TablePin(name, p.x(), p.z()));
        changed();
        return true;
    }

    public boolean removePin(int index) {
        if (index < 0 || index >= pins.size()) return false;
        pins.remove(index);
        changed();
        return true;
    }

    /** A block entity outlives a change of its block's state: this one only belongs to the centre. */
    @Override
    public boolean isValidBlockState(BlockState state) {
        return super.isValidBlockState(state) && state.getValue(MapTableBlock.PART) == TablePart.CENTER;
    }

    /** The hologram spans all nine pieces and its beams rise above them. */
    public AABB renderBox() {
        return new AABB(worldPosition).inflate(1.5, 0, 1.5).expandTowards(0, 3, 0);
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (TablePin p : pins) TablePin.CODEC.encodeStart(NbtOps.INSTANCE, p).result().ifPresent(list::add);
        tag.put("Pins", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pins.clear();
        for (Tag t : tag.getList("Pins", Tag.TAG_COMPOUND)) {
            TablePin.CODEC.parse(NbtOps.INSTANCE, t).result().ifPresent(p -> {
                if (pins.size() < TablePin.MAX) pins.add(p);
            });
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
