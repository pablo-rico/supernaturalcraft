package org.papiricoh.supernaturalcraft.coven;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.coven.church.ChurchPiece;
import org.papiricoh.supernaturalcraft.coven.covenstead.CovenPiece;
import org.papiricoh.supernaturalcraft.coven.layout.ChurchLayout;
import org.papiricoh.supernaturalcraft.coven.layout.CovenLayout;
import org.papiricoh.supernaturalcraft.coven.layout.RowenaHouseLayout;
import org.papiricoh.supernaturalcraft.coven.rowena.CallingCardItem;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaHouse;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaRelation;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaSavedData;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaServerHandlers;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaSite;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaWorld;

/**
 * {@code /supernatural coven …} (v0.21): {@code church place} and {@code covenstead place} build one (with its creatures) where
 * you stand and register it as the world generator would ({@link #register}: its start in its chunk, a reference in every chunk
 * it touches), so the rite Consecrate Church works in a placed church. Vanilla {@code /place structure
 * supernaturalcraft:abandoned_church} does <b>not</b> register its start (that is vanilla's way): a church placed so cannot be
 * consecrated; use the command here, or a generated one. {@code rowena locate|tp|place|ensure|card|reset}: where her house is (hidden from
 * {@code /locate}), going there, building it where you stand (and making it hers), making sure it stands, a calling card, and
 * forgetting what she thinks of you.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class CovenCommands {

    private CovenCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("supernatural").requires(s -> s.hasPermission(2)).then(coven()));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> coven() {
        return Commands.literal("coven")
                .then(Commands.literal("church").then(Commands.literal("place").executes(CovenCommands::placeChurch)))
                .then(Commands.literal("covenstead").then(Commands.literal("place").executes(CovenCommands::placeCoven)))
                .then(Commands.literal("rowena")
                        .then(Commands.literal("locate").executes(CovenCommands::locate))
                        .then(Commands.literal("tp").executes(CovenCommands::tp))
                        .then(Commands.literal("place").executes(CovenCommands::placeHouse))
                        .then(Commands.literal("ensure").executes(CovenCommands::ensure))
                        .then(Commands.literal("card").executes(CovenCommands::card))
                        .then(Commands.literal("reset").executes(CovenCommands::reset)));
    }

    private static ServerLevel overworld(CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getServer().overworld();
    }

    private static BlockPos ground(ServerLevel level, BlockPos at) {
        level.getChunk(at);
        return new BlockPos(at.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ()), at.getZ());
    }

    private static int placeChurch(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos origin = ground(level, BlockPos.containing(ctx.getSource().getPosition()));
        long seed = level.getSeed() ^ origin.asLong();
        int dir = (int) Math.floorMod(seed ^ (seed >>> 29), 4L);
        ChurchPiece piece = new ChurchPiece(origin, dir, seed);
        BoundingBox box = new BoundingBox(origin.getX() - ChurchLayout.RADIUS - 2, origin.getY() - ChurchLayout.DEPTH - 2,
                origin.getZ() - ChurchLayout.RADIUS - 2, origin.getX() + ChurchLayout.RADIUS + 2, origin.getY() + ChurchLayout.HEIGHT + 2,
                origin.getZ() + ChurchLayout.RADIUS + 2);
        PlanWriter.load(level, box);
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random, box, new ChunkPos(origin), origin);
        register(level, org.papiricoh.supernaturalcraft.coven.church.ChurchStructure.KEY, piece, new ChunkPos(origin));
        ctx.getSource().sendSuccess(() -> Component.literal("Abandoned church placed at " + origin.toShortString()), true);
        return 1;
    }

    private static int placeCoven(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos origin = ground(level, BlockPos.containing(ctx.getSource().getPosition()));
        long seed = level.getSeed() ^ origin.asLong() ^ 0xC0FEL;
        int dir = (int) Math.floorMod(seed ^ (seed >>> 29), 4L);
        CovenPiece piece = new CovenPiece(origin, dir, seed);
        BoundingBox box = new BoundingBox(origin.getX() - CovenLayout.RADIUS - 2, origin.getY() - 8, origin.getZ() - CovenLayout.RADIUS - 2,
                origin.getX() + CovenLayout.RADIUS + 2, origin.getY() + CovenLayout.HEIGHT + 2, origin.getZ() + CovenLayout.RADIUS + 2);
        PlanWriter.load(level, box);
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random, box, new ChunkPos(origin), origin);
        register(level, org.papiricoh.supernaturalcraft.coven.covenstead.CovenStructure.KEY, piece, new ChunkPos(origin));
        ctx.getSource().sendSuccess(() -> Component.literal("Witch coven placed at " + origin.toShortString()), true);
        return 1;
    }

    /**
     * What the world generator leaves for a structure of one {@code piece}: its start in chunk {@code home}, a reference in every
     * chunk the piece touches — so {@code StructureManager} finds it ({@code ConsecrateChurchEffect.church}, {@code /locate}).
     *
     * @return the start, or null if the structure is not in this world's registry
     */
    public static @org.jetbrains.annotations.Nullable net.minecraft.world.level.levelgen.structure.StructureStart register(
            ServerLevel level, net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.structure.Structure> key,
            net.minecraft.world.level.levelgen.structure.StructurePiece piece, ChunkPos home) {
        var structure = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).get(key);
        if (structure == null) return null;
        var start = new net.minecraft.world.level.levelgen.structure.StructureStart(structure, home, 0,
                new net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer(java.util.List.of(piece)));
        BoundingBox pb = piece.getBoundingBox();
        for (int cx = pb.minX() >> 4; cx <= pb.maxX() >> 4; cx++) {
            for (int cz = pb.minZ() >> 4; cz <= pb.maxZ() >> 4; cz++) {
                level.getChunk(cx, cz).addReferenceForStructure(structure, home.toLong());
            }
        }
        var chunk = level.getChunk(home.x, home.z);
        chunk.setStartForStructure(structure, start);
        chunk.setUnsaved(true);
        return start;
    }

    private static int locate(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = overworld(ctx);
        RowenaSite.Site site = RowenaSite.of(level);
        BlockPos h = RowenaWorld.home(site);
        boolean built = RowenaSavedData.get(level).built();
        ctx.getSource().sendSuccess(() -> Component.literal("Rowena's house: " + h.getX() + " " + h.getY() + " " + h.getZ() + " ("
                + (int) Math.round(Math.hypot(h.getX(), h.getZ())) + " blocks from the origin, " + (built ? "built" : "not yet built") + ")"), false);
        return 1;
    }

    private static int tp(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p == null) return 0;
        ServerLevel level = overworld(ctx);
        RowenaSite.Site site = RowenaSite.of(level);
        BlockPos entry = RowenaHouse.at(site, RowenaHouseLayout.ENTRY);
        level.getChunk(entry);
        p.teleportTo(level, entry.getX() + 0.5, Math.max(entry.getY(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                entry.getX(), entry.getZ())), entry.getZ() + 0.5, p.getYRot(), p.getXRot());
        return 1;
    }

    private static int placeHouse(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos at = BlockPos.containing(ctx.getSource().getPosition());
        int turn = ctx.getSource().getPlayer() == null ? 0 : turnFacing(ctx.getSource().getPlayer().getYRot());
        BlockPos origin = RowenaHouse.originOn(level, at.getX(), at.getZ());
        RowenaHouse.placeAt(level, origin, turn);
        if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) RowenaSavedData.get(level).setHouse(origin, turn, true);
        ctx.getSource().sendSuccess(() -> Component.literal("Rowena's house placed at " + origin.toShortString()), true);
        return 1;
    }

    /** The turn whose door looks back at someone facing {@code yaw} (the door looks south at turn 0). */
    static int turnFacing(float yaw) {
        int dir = Math.floorMod(Math.round(yaw / 90f), 4);
        return (dir + 2) % 4;
    }

    private static int ensure(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = overworld(ctx);
        BlockPos origin = RowenaWorld.ensureNow(level, RowenaSavedData.get(level), RowenaSite.of(level));
        ctx.getSource().sendSuccess(() -> Component.literal("Rowena's house stands at " + origin.toShortString()), true);
        return 1;
    }

    private static int card(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p == null) return 0;
        p.getInventory().add(CallingCardItem.pointed(overworld(ctx)));
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p == null) return 0;
        RowenaServerHandlers.setRelation(p, RowenaRelation.NONE);
        ctx.getSource().sendSuccess(() -> Component.literal("Rowena has forgotten you."), true);
        return 1;
    }
}
