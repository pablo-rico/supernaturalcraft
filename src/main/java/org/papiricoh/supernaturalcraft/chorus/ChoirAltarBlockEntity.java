package org.papiricoh.supernaturalcraft.chorus;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusSummoning;
import org.papiricoh.supernaturalcraft.registry.AllBlockEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.UUID;

/**
 * The Choir Altar's memory: its hymn (three notes), the Shattered Hymn laid on it, and how far
 * into the hymn the bells have been rung.
 *
 * <p>To wake the Chorus: in a thunderstorm, lay a Shattered Hymn on the altar, then ring its
 * three notes on the bells in order. A wrong note jars and starts the hymn over; ten seconds of
 * silence lets it fade. In creative, an empty hand reads the hymn and sneaking starts tuning it:
 * the next three bells that player rings become the hymn.
 */
public class ChoirAltarBlockEntity extends BlockEntity {

    /** Silence after which a half-rung hymn fades. */
    public static final int FADE = 200;
    private byte[] melody = new byte[0];
    private ItemStack hymn = ItemStack.EMPTY;
    private int progress;
    private long lastNote;
    private @Nullable UUID tuner;
    private byte[] tuning = new byte[0];

    public ChoirAltarBlockEntity(BlockPos pos, BlockState state) {
        super(AllBlockEntities.CHOIR_ALTAR.get(), pos, state);
    }

    public byte[] melody() {
        return melody;
    }

    public void setMelody(byte[] melody) {
        this.melody = melody.clone();
        progress = 0;
        changed();
    }

    public boolean armed() {
        return !hymn.isEmpty();
    }

    public int progress() {
        return progress;
    }

    /** A hand-placed altar (or one generated without a hymn) makes up its own. */
    public void ensureMelody() {
        if (!Melody.valid(melody) && level != null) setMelody(Melody.generate(level.getRandom()));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel) ensureMelody();
    }

    // --- use ------------------------------------------------------------------------------

    /** @return true if the use was handled */
    public boolean onUse(Player player, InteractionHand hand, ItemStack held) {
        if (!(level instanceof ServerLevel server)) return false;
        ensureMelody();
        if (held.isEmpty() && player.isCreative()) {
            if (player.isShiftKeyDown()) {
                tuner = player.getUUID();
                tuning = new byte[0];
                message(player, Component.translatable("message.supernaturalcraft.choir_altar.tuning").withStyle(ChatFormatting.AQUA));
            } else {
                message(player, Component.translatable("message.supernaturalcraft.choir_altar.hymn", Melody.describe(melody)));
            }
            return true;
        }
        if (held.is(AllItems.SHATTERED_HYMN.get())) {
            if (armed()) {
                message(player, Component.translatable("message.supernaturalcraft.choir_altar.already").withStyle(ChatFormatting.GRAY));
                return true;
            }
            if (!ChorusSummoning.stormy(server)) {
                message(player, Component.translatable("message.supernaturalcraft.choir_altar.no_storm").withStyle(ChatFormatting.GRAY));
                return true;
            }
            hymn = held.copyWithCount(1);
            if (!player.getAbilities().instabuild) held.shrink(1);
            progress = 0;
            server.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.5f, 0.7f);
            message(player, Component.translatable("message.supernaturalcraft.choir_altar.armed").withStyle(ChatFormatting.GOLD));
            changed();
            return true;
        }
        if (held.isEmpty() && armed()) {
            // Take the hymn back down.
            ItemStack back = hymn;
            hymn = ItemStack.EMPTY;
            progress = 0;
            if (!player.getAbilities().instabuild && !player.addItem(back)) player.drop(back, false);
            changed();
            return true;
        }
        return false;
    }

    // --- the bells ------------------------------------------------------------------------

    public void onBellRung(int note, BlockPos bell, @Nullable Player ringer) {
        if (!(level instanceof ServerLevel server)) return;
        ensureMelody();
        if (ringer != null && ringer.getUUID().equals(tuner)) {
            byte[] next = java.util.Arrays.copyOf(tuning, tuning.length + 1);
            next[tuning.length] = (byte) note;
            tuning = next;
            if (tuning.length == Melody.LENGTH) {
                setMelody(tuning);
                tuner = null;
                message(ringer, Component.translatable("message.supernaturalcraft.choir_altar.tuned", Melody.describe(melody)));
            }
            return;
        }
        if (!armed()) return;
        long now = server.getGameTime();
        if (progress > 0 && now - lastNote > FADE) progress = 0;
        lastNote = now;
        if (note != melody[progress]) {
            progress = 0;
            server.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), SoundSource.BLOCKS, 2.0f, 0.5f);
            server.playSound(null, bell, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6f, 1.6f);
            if (ringer != null) {
                message(ringer, Component.translatable("message.supernaturalcraft.choir_altar.discord").withStyle(ChatFormatting.DARK_RED));
            }
            changed();
            return;
        }
        progress++;
        server.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 2.0f, 0.6f + progress * 0.3f);
        if (progress < Melody.LENGTH) {
            changed();
            return;
        }
        progress = 0;
        if (!ChorusSummoning.stormy(server)) {
            if (ringer != null) {
                message(ringer, Component.translatable("message.supernaturalcraft.choir_altar.no_storm").withStyle(ChatFormatting.GRAY));
            }
            changed();
            return;
        }
        if (ChorusSummoning.summon(server, worldPosition, ringer instanceof ServerPlayer sp ? sp : null)) {
            hymn = ItemStack.EMPTY;
        }
        changed();
    }

    private static void message(Player player, Component text) {
        player.displayClientMessage(text, true);
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public ItemStack hymn() {
        return hymn;
    }

    public void dropHymn() {
        if (level != null && !hymn.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, hymn);
            hymn = ItemStack.EMPTY;
        }
    }

    // --- persistence & sync ---------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (Melody.valid(melody)) tag.putByteArray("Melody", melody);
        if (!hymn.isEmpty()) tag.put("Hymn", hymn.save(registries));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        melody = tag.getByteArray("Melody");
        hymn = tag.contains("Hymn") ? ItemStack.parseOptional(registries, tag.getCompound("Hymn")) : ItemStack.EMPTY;
        progress = tag.getInt("Progress");
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChoirAltarBlockEntity altar) {
        if (altar.progress > 0 && level.getGameTime() - altar.lastNote > FADE) {
            altar.progress = 0;
            altar.changed();
        }
    }
}
