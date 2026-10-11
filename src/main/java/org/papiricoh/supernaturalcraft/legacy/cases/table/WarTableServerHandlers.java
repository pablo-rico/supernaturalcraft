package org.papiricoh.supernaturalcraft.legacy.cases.table;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.legacy.Legacies;
import org.papiricoh.supernaturalcraft.legacy.Legacy;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseOffice;
import org.papiricoh.supernaturalcraft.network.OpenWarTablePayload;
import org.papiricoh.supernaturalcraft.network.WarTableActionPayload;
import org.papiricoh.supernaturalcraft.network.WarTableTerrainRequestPayload;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * The server's side of a war table (v0.19): opening it, the hunter's actions at it, and the terrain their hologram asks for.
 * Everything is checked again here: the table whole and in reach, a member, the Overworld.
 */
public final class WarTableServerHandlers {

    /** How near the table a hunter must stand to act at it. */
    public static final double REACH = 8;
    /** How near the table its hologram is drawn (and its terrain sent). */
    public static final double SIGHT = 32;

    private WarTableServerHandlers() {
    }

    /** A member used a formed table: open it, or say why not. */
    public static void open(ServerPlayer player, BlockPos centre) {
        if (player.level().dimension() != Level.OVERWORLD) {
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.legacy.map_table.blank")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return;
        }
        if (!WarTable.intact(player.level(), centre)) return;
        if (player.connection != null) PacketDistributor.sendToPlayer(player, new OpenWarTablePayload(centre));
    }

    public static void action(WarTableActionPayload p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer player) act(player, p.centre(), p.action(), p.index(), p.x(), p.z(), p.name());
    }

    public static void terrain(WarTableTerrainRequestPayload p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD || !near(player, p.centre(), SIGHT) || !level.isLoaded(p.centre())
                || !WarTable.intact(level, p.centre())) return;
        TerrainSurvey.request(player, level, p.centre(), p.keys());
    }

    /**
     * An action at the war table centred on {@code centre}, as {@link WarTableActionPayload} describes it.
     *
     * @return whether it was done
     */
    public static boolean act(ServerPlayer player, BlockPos centre, byte action, int index, int x, int z, String name) {
        if (!(player.level() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return false;
        if (!near(player, centre, REACH) || !level.isLoaded(centre) || !WarTable.intact(level, centre)) return false;
        Legacy legacy = Legacies.get(player);
        if (!legacy.member()) return false;
        WarTableBlockEntity table = WarTable.entity(level, centre);
        switch (action) {
            case WarTableActionPayload.REQUEST -> {
                if (CaseOffice.open(legacy) != null) return false;
                if (CaseOffice.waiting(player)) {
                    player.displayClientMessage(Component.translatable("message.supernaturalcraft.legacy.case.cooldown")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                    return false;
                }
                return CaseOffice.issue(player, centre) != null;
            }
            case WarTableActionPayload.ABANDON -> {
                boolean done = CaseOffice.abandon(player);
                if (done) level.playSound(null, centre, AllSounds.LEGACY_PAPER.get(), SoundSource.BLOCKS, 1.0f, 0.6f);
                return done;
            }
            case WarTableActionPayload.COMPASS -> {
                ItemStack compass = compass(player);
                if (compass == null) {
                    player.displayClientMessage(Component.translatable("message.supernaturalcraft.legacy.compass.none")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                    return false;
                }
                if (index < 0) {
                    compass.remove(AllDataComponents.COMPASS_TARGET.get());
                    player.displayClientMessage(Component.translatable("message.supernaturalcraft.legacy.compass.cleared").withStyle(ChatFormatting.GOLD), true);
                } else {
                    if (table == null || index >= table.pins().size()) return false;
                    TablePin pin = table.pins().get(index);
                    compass.set(AllDataComponents.COMPASS_TARGET.get(), pin);
                    player.displayClientMessage(Component.translatable("message.supernaturalcraft.legacy.compass.set", pin.name()).withStyle(ChatFormatting.GOLD), true);
                }
                level.playSound(null, player.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 0.8f, 1.0f);
                return true;
            }
            case WarTableActionPayload.PIN_ADD -> {
                if (table == null || Math.abs(x - centre.getX()) > TerrainTile.RANGE || Math.abs(z - centre.getZ()) > TerrainTile.RANGE) return false;
                String pin = TablePin.clean(name, "Pin " + (table.pins().size() + 1));
                boolean done = table.addPin(new TablePin(pin, x, z));
                if (done) level.playSound(null, centre, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 0.8f, 1.2f);
                return done;
            }
            case WarTableActionPayload.PIN_RENAME -> {
                if (table == null || index < 0 || index >= table.pins().size()) return false;
                return table.renamePin(index, TablePin.clean(name, table.pins().get(index).name()));
            }
            case WarTableActionPayload.PIN_REMOVE -> {
                if (table == null) return false;
                boolean done = table.removePin(index);
                if (done) level.playSound(null, centre, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 0.6f, 0.7f);
                return done;
            }
            default -> {
                return false;
            }
        }
    }

    /** The Letters compass the hunter holds (main hand, then off hand), or the first in their pack; null if none. */
    public static @Nullable ItemStack compass(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.is(AllItems.LETTERS_COMPASS.get())) return held;
        }
        for (ItemStack s : player.getInventory().items) if (s.is(AllItems.LETTERS_COMPASS.get())) return s;
        return null;
    }

    private static boolean near(ServerPlayer player, BlockPos centre, double range) {
        return player.distanceToSqr(centre.getX() + 0.5, centre.getY() + 0.5, centre.getZ() + 0.5) <= range * range;
    }
}
