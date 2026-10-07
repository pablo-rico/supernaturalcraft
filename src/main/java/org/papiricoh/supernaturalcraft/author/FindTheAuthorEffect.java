package org.papiricoh.supernaturalcraft.author;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlCast;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlSpellEffect;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * "Find the Author": once every great enemy before him is beaten, the smoke draws the way to his cabin. The world now
 * expects him ({@link AuthorSavedData#spellCast()}: he sits at his desk when hunters come), and the caster is handed a
 * map with his mark on it. Only in the overworld, where the cabin is.
 */
public record FindTheAuthorEffect() implements BowlSpellEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("find_the_author");
    public static final MapCodec<FindTheAuthorEffect> CODEC = MapCodec.unit(new FindTheAuthorEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public @Nullable String precheck(BowlCast cast) {
        if (cast.level().dimension() != Level.OVERWORLD) return "message.supernaturalcraft.author.spell.wrong_world";
        if (!AuthorWorld.readyForHim(cast.caster())) return "message.supernaturalcraft.author.spell.not_yet";
        return null;
    }

    @Override
    public boolean perform(BowlCast cast) {
        return cast(cast.level(), cast.caster(), cast.surface());
    }

    /** The spell's work, apart from the bowl: for the command and tests too. */
    public static boolean cast(ServerLevel level, ServerPlayer caster, Vec3 at) {
        if (level.dimension() != Level.OVERWORLD) return false;
        AuthorSavedData.get(level).setSpellCast(true);
        AuthorSite.Site site = AuthorSite.of(level);
        ItemStack map = AuthorWorld.map(level, site);
        if (!caster.addItem(map)) level.addFreshEntity(new ItemEntity(level, at.x, at.y + 0.6, at.z, map));
        level.playSound(null, at.x, at.y, at.z, AllSounds.CHUCK_TYPE.get(), SoundSource.PLAYERS, 1.0f, 0.8f);
        level.playSound(null, at.x, at.y, at.z, AllSounds.CHUCK_BELL.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
        caster.displayClientMessage(Component.translatable("message.supernaturalcraft.author.spell.found").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
        return true;
    }

    @Override
    public ItemStack displayResult() {
        return AuthorWorld.displayMap();
    }
}
