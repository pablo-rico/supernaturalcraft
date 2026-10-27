package org.papiricoh.supernaturalcraft.chain;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.arena.Stakes;
import org.papiricoh.supernaturalcraft.chain.seat.SeatWorld;
import org.papiricoh.supernaturalcraft.entity.boss.dick.DickRomanEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

/**
 * The Leviathans' first Lord in his own penthouse (v0.28): while Dick Roman holds their throne ({@link Officer#boss()}), he sits
 * at the head of the SucroCorp tower's boardroom table, and the boardroom door opens only to the anointed Righteous Bone. Used
 * on the door (an iron door, which opens to nothing else while he sits), the bone becomes the fight's stake (it comes back if the hunters lose) and his boss fight begins where he
 * sits, the boardroom itself the arena: nothing is summoned and nothing laid ({@link DickRomanEntity#fightInPlace}). His death
 * there is the Lord's ({@code ChainEvents.officerKilled}); after it his rite ({@code summon_dick_roman}) calls him back as a
 * rematch, which the chain no longer feels.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class SucroPenthouse {

    /** The tower's anchors beyond the common contract (see {@code SucroTower}). */
    public static final String BOARDROOM = "boardroom", DOOR = "boardroom_door", SEAT = "dick_seat";
    /** The boardroom's radius: the arena his fight is built for ({@code BoardroomGround}'s default). */
    public static final int RADIUS = 16;
    /** How near the door anchor a click counts as the door (a two-block door, its frame). */
    static final int DOOR_REACH = 2;
    /** Lines he rises with ({@code chain.supernaturalcraft.penthouse.rise.<n>}). */
    static final int RISE_LINES = 3;

    private SucroPenthouse() {
    }

    /** The officer on the Leviathans' throne, if it is the boss himself, alive. */
    public static @Nullable Officer seated(MinecraftServer server) {
        Officer lord = ChainSavedData.get(server).hierarchy(ChainFaction.LEVIATHAN).lord();
        return lord != null && lord.boss() && lord.alive() ? lord : null;
    }

    /** Why his rite cannot call him now (he holds his throne: go to his penthouse), or null. */
    public static @Nullable Component riteRefusal(MinecraftServer server) {
        if (!SNConfig.CHAIN_ENABLED.get() || seated(server) == null) return null;
        return Component.translatable("message.supernaturalcraft.dick_roman.seated").withStyle(ChatFormatting.GRAY);
    }

    @SubscribeEvent
    public static void door(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getHand() != InteractionHand.MAIN_HAND || !SNConfig.CHAIN_ENABLED.get()) return;
        MinecraftServer server = p.server;
        if (SeatWorld.built(server, ChainFaction.LEVIATHAN) == null || p.level() != server.overworld()) return;
        var anchors = SeatWorld.ensure(server.overworld(), ChainFaction.LEVIATHAN);
        BlockPos door = anchors == null ? null : anchors.point(DOOR);
        if (door == null || event.getPos().distManhattan(door) > DOOR_REACH) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        // With no boss in the chair, the boardroom's iron door answers any hand, like a pressure plate would.
        if (seated(server) == null) {
            swing(p.serverLevel(), door, p, null);
            return;
        }
        ItemStack held = p.getMainHandItem();
        if (!held.is(AllItems.ANOINTED_RIGHTEOUS_BONE.get())) {
            p.displayClientMessage(Component.translatable("chain.supernaturalcraft.penthouse.door").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (open(p, anchors) != null && !p.getAbilities().instabuild) held.shrink(1);
    }

    /** Opens the boardroom's iron door ({@code open} null: toggles it). The arena gives its state back when the fight ends. */
    static void swing(ServerLevel level, BlockPos door, @Nullable ServerPlayer by, @Nullable Boolean open) {
        var state = level.getBlockState(door);
        if (!(state.getBlock() instanceof net.minecraft.world.level.block.DoorBlock block)) return;
        boolean now = open != null ? open : !block.isOpen(state);
        block.setOpen(by, level, state, door, now);
    }

    /** Opens his boardroom on {@code p}: the fight begins where he sits. @return him, or null (busy world; nothing spent) */
    public static @Nullable DickRomanEntity open(ServerPlayer p, SeatWorld.Anchors anchors) {
        ServerLevel level = p.serverLevel();
        Officer lord = seated(p.server);
        BlockPos centre = anchors.point(BOARDROOM), seat = anchors.point(SEAT);
        if (lord == null || centre == null) return null;
        if (seat == null) seat = anchors.lord();
        Component busy = LuciferSummoning.refusal(level);
        if (busy != null) {
            p.displayClientMessage(busy, true);
            return null;
        }
        Stakes.offer(Stakes.of(p.getUUID(), List.of(p.getMainHandItem().copyWithCount(1))));
        ArenaController arena = LuciferSummoning.openArena(level, centre, RADIUS, ArenaTheme.BOARDROOM);
        if (arena == null) {
            Stakes.claim();
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.dick_roman.busy").withStyle(ChatFormatting.GRAY), true);
            return null;
        }
        DickRomanEntity boss = AllEntities.DICK_ROMAN.get().create(level);
        if (boss == null) return null;
        BlockPos door = anchors.point(DOOR);
        if (door != null) swing(level, door, p, true);
        float yaw = (float) Math.toDegrees(Math.atan2(-(centre.getX() - seat.getX()), centre.getZ() - seat.getZ()));
        boss.moveTo(seat.getX() + 0.5, seat.getY(), seat.getZ() + 0.5, yaw, 0);
        boss.setYHeadRot(yaw);
        boss.setYBodyRot(yaw);
        boss.fightInPlace();
        boss.bindArena(arena);
        OfficerSpawner.tag(boss, lord);
        level.addFreshEntity(boss);
        boss.beginEmergence();
        int line = (int) Math.floorMod(level.getGameTime(), (long) RISE_LINES);
        Component said = Component.literal(ChainNames.name(lord) + ": ").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.translatable("chain.supernaturalcraft.penthouse.rise." + line).withStyle(ChatFormatting.GRAY));
        for (ServerPlayer near : level.players())
            if (near.distanceToSqr(boss) < (RADIUS + 16) * (RADIUS + 16)) near.sendSystemMessage(said);
        return boss;
    }
}
