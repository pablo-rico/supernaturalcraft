package org.papiricoh.supernaturalcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.network.SNNetworking;
import org.papiricoh.supernaturalcraft.registry.SNRegistries;


/**
 * {@code /supernatural} — test and admin tools (permission level 2):
 * boss summon [lucifer|amara|chorus] [pos] | boss phase &lt;n&gt; | boss health &lt;fraction&gt; (any boss, see {@link BossCommands}) | arena restore | mana fill | sigil learn &lt;id|all&gt; | grace &lt;bool&gt;
 * | chorus … (see {@link ChorusCommands}).
 */
public final class SNCommands {

    private SNCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("supernatural").requires(s -> s.hasPermission(2))
                .then(BossCommands.boss())
                .then(Commands.literal("arena").then(Commands.literal("restore").executes(ctx -> {
                    ServerLevel level = ctx.getSource().getLevel();
                    int n = 0;
                    for (ArenaController a : ArenaSavedData.get(level).all()) {
                        a.beginRestore(false);
                        n++;
                    }
                    int count = n;
                    ctx.getSource().sendSuccess(() -> Component.literal("Restoring " + count + " arena(s)."), true);
                    return n;
                })))
                .then(ChorusCommands.chorus())
                .then(ChorusCommands.spire())
                .then(Commands.literal("mana").then(Commands.literal("fill").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    ArcanaData a = ManaManager.get(p);
                    a.setMana(a.maxMana());
                    SNNetworking.syncArcana(p);
                    return 1;
                })))
                .then(Commands.literal("grace").then(Commands.argument("value", com.mojang.brigadier.arguments.BoolArgumentType.bool()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    ManaManager.get(p).setGrace(com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "value"));
                    SNNetworking.syncArcana(p);
                    return 1;
                })))
                .then(Commands.literal("sigil").then(Commands.literal("learn")
                        .then(Commands.literal("all").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            var reg = p.registryAccess().registryOrThrow(SNRegistries.SIGIL);
                            reg.keySet().forEach(id -> ManaManager.get(p).learn(id));
                            SNNetworking.syncArcana(p);
                            return reg.size();
                        }))
                        .then(Commands.argument("sigil", ResourceLocationArgument.id()).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            ResourceLocation id = ResourceLocationArgument.getId(ctx, "sigil");
                            ManaManager.get(p).learn(id);
                            SNNetworking.syncArcana(p);
                            return 1;
                        })))));
    }
}
