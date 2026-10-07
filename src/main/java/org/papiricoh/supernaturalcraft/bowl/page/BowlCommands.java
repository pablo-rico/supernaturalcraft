package org.papiricoh.supernaturalcraft.bowl.page;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.BowlSpells;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.network.SNNetworking;

import java.util.List;

/**
 * {@code /supernatural bowl learn <spell|all>} teaches bowl spells, {@code bowl forget} forgets
 * them all, {@code bowl page <spell>} hands over a spell page. For testing (permission level 2).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class BowlCommands {

    private static final SuggestionProvider<CommandSourceStack> SPELLS = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(BowlSpells.allSpells(ctx.getSource().getServer().getRecipeManager()), builder);

    private BowlCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("supernatural").requires(s -> s.hasPermission(2))
                .then(Commands.literal("bowl")
                        .then(Commands.literal("learn")
                                .then(Commands.literal("all").executes(BowlCommands::learnAll))
                                .then(Commands.argument("spell", ResourceLocationArgument.id()).suggests(SPELLS)
                                        .executes(ctx -> learn(ctx, ResourceLocationArgument.getId(ctx, "spell")))))
                        .then(Commands.literal("forget").executes(BowlCommands::forget))
                        .then(Commands.literal("page")
                                .then(Commands.argument("spell", ResourceLocationArgument.id()).suggests(SPELLS)
                                        .executes(ctx -> page(ctx, ResourceLocationArgument.getId(ctx, "spell")))))));
    }

    private static int learn(CommandContext<CommandSourceStack> ctx, ResourceLocation spell) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (BowlSpells.recipesFor(ctx.getSource().getServer().getRecipeManager(), spell).isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("commands.supernaturalcraft.bowl.unknown", spell.toString()));
            return 0;
        }
        ManaManager.get(player).learnRite(spell);
        SNNetworking.syncArcana(player);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.supernaturalcraft.bowl.learned",
                Component.translatable(BowlSpells.nameKey(spell))), true);
        return 1;
    }

    private static int learnAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        List<ResourceLocation> spells = BowlSpells.allSpells(ctx.getSource().getServer().getRecipeManager());
        ArcanaData arcana = ManaManager.get(player);
        spells.forEach(arcana::learnRite);
        SNNetworking.syncArcana(player);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.supernaturalcraft.bowl.learned_all", spells.size()), true);
        return spells.size();
    }

    private static int forget(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ManaManager.get(player).forgetRites();
        SNNetworking.syncArcana(player);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.supernaturalcraft.bowl.forgot"), true);
        return 1;
    }

    private static int page(CommandContext<CommandSourceStack> ctx, ResourceLocation spell) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (!player.getInventory().add(SpellPageItem.of(spell))) player.drop(SpellPageItem.of(spell), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.supernaturalcraft.bowl.page",
                Component.translatable(BowlSpells.nameKey(spell))), true);
        return 1;
    }
}
