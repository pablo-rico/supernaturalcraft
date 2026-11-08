package org.papiricoh.supernaturalcraft.hands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;
import org.papiricoh.supernaturalcraft.hands.altar.HandAltarBlockEntity;
import org.papiricoh.supernaturalcraft.hands.reliquary.ReliquaryAnchors;
import org.papiricoh.supernaturalcraft.hands.reliquary.ReliquaryBuilder;

import java.util.Arrays;

/**
 * {@code /supernatural hands …} (v0.42, level 2; debug):
 * <ul>
 *   <li>{@code place <kind> [turn]}: a reliquary of that kind ({@code uboat_wreck}, {@code thule_bunker}, {@code church_crypt},
 *   {@code ark_temple}, {@code snow_monastery}) built here, and the caller set at its way in;</li>
 *   <li>{@code locate}: the nearest reliquary whose Hand the caller has not taken (the tracking rite's search, run at once);</li>
 *   <li>{@code give <hand>}: a Hand ({@code delphines_hand}, {@code nail_of_the_cross}…), without claiming it;</li>
 *   <li>{@code reset}: the caller has taken no Hand (and the altars within a few chunks forget them).</li>
 * </ul>
 * Self-registered under the mod's {@code /supernatural} root (Brigadier merges literals of one name).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class HandsCommands {

    private HandsCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("supernatural").requires(s -> s.hasPermission(2)).then(hands()));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> hands() {
        return Commands.literal("hands").requires(s -> s.hasPermission(2))
                .then(Commands.literal("place")
                        .then(Commands.argument("kind", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(S11Assets.Hand.values()).map(h -> h.reliquary), b))
                                .executes(c -> place(c, 0))
                                .then(Commands.argument("turn", IntegerArgumentType.integer(0, 3))
                                        .executes(c -> place(c, IntegerArgumentType.getInteger(c, "turn"))))))
                .then(Commands.literal("locate").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    HandLocator.forget(p);
                    HandLocator.Located l = HandLocator.complete(p);
                    if (l == null) {
                        c.getSource().sendFailure(Component.translatable("command.supernaturalcraft.hands.locate.none"));
                        return 0;
                    }
                    c.getSource().sendSuccess(() -> Component.translatable("command.supernaturalcraft.hands.locate",
                            Component.translatable("item.supernaturalcraft." + l.hand().item), l.pos().getX(), l.pos().getZ()), false);
                    return 1;
                }))
                .then(Commands.literal("give")
                        .then(Commands.argument("hand", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(S11Assets.Hand.values()).map(h -> h.item), b))
                                .executes(c -> {
                                    S11Assets.Hand h = S11Assets.Hand.byItem(StringArgumentType.getString(c, "hand"));
                                    if (h == null) {
                                        c.getSource().sendFailure(Component.translatable("command.supernaturalcraft.hands.unknown", StringArgumentType.getString(c, "hand")));
                                        return 0;
                                    }
                                    Hands.give(c.getSource().getPlayerOrException(), h);
                                    return 1;
                                })))
                .then(Commands.literal("reset").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Hands.reset(p);
                    int altars = forgetNear(p);
                    c.getSource().sendSuccess(() -> Component.translatable("command.supernaturalcraft.hands.reset", altars), true);
                    return 1;
                }));
    }

    static @Nullable S11Assets.Hand kind(String name) {
        for (S11Assets.Hand h : S11Assets.Hand.values()) if (h.reliquary.equals(name)) return h;
        return null;
    }

    private static int place(CommandContext<CommandSourceStack> c, int turn) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        S11Assets.Hand h = kind(StringArgumentType.getString(c, "kind"));
        if (h == null) {
            c.getSource().sendFailure(Component.translatable("command.supernaturalcraft.hands.unknown", StringArgumentType.getString(c, "kind")));
            return 0;
        }
        ServerLevel level = p.serverLevel();
        BlockPos origin = p.blockPosition();
        BlockPos altar = ReliquaryBuilder.placeAt(level, origin, h, turn);
        BlockPos entry = ReliquaryBuilder.point(origin, h, turn, ReliquaryAnchors.ENTRY);
        if (entry != null) p.teleportTo(level, entry.getX() + 0.5, entry.getY(), entry.getZ() + 0.5, p.getYRot(), p.getXRot());
        c.getSource().sendSuccess(() -> Component.translatable("command.supernaturalcraft.hands.placed",
                Component.translatable("structure.supernaturalcraft." + h.reliquary), altar.getX(), altar.getY(), altar.getZ()), true);
        return 1;
    }

    /** Clears {@code p} off every reliquary altar in the chunks round them. @return how many altars forgot them */
    static int forgetNear(ServerPlayer p) {
        int n = 0;
        ServerLevel level = p.serverLevel();
        int cx = p.blockPosition().getX() >> 4, cz = p.blockPosition().getZ() >> 4;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (!level.hasChunk(cx + dx, cz + dz)) continue;
                LevelChunk chunk = level.getChunk(cx + dx, cz + dz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof HandAltarBlockEntity a && a.claimedBy(p.getUUID())) {
                        a.forgetOne(p.getUUID());
                        n++;
                    }
                }
            }
        }
        return n;
    }
}
