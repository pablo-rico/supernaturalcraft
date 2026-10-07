package org.papiricoh.supernaturalcraft.author;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.List;
import java.util.Optional;

/**
 * The Author's Pen: rewrites a small patch of the world. Used while sneaking, it turns to its next {@code PEN_MODE};
 * otherwise, in mode 0 on a block it rewrites the biome around it ({@link PenRewrites#RADIUS} blocks, the next in
 * {@link PenRewrites#BIOMES}), in mode 1 the sky (clear day, rain, storm, clear night), in mode 2 on a creature it
 * rewrites it into another of its tier (never a boss, never a player). Each mode has its own long cooldown.
 */
public class AuthorsPenItem extends Item {

    public AuthorsPenItem(Properties properties) {
        super(properties);
    }

    public static int mode(ItemStack stack) {
        return stack.getOrDefault(AllDataComponents.PEN_MODE.get(), 0);
    }

    public static String modeKey(int mode) {
        return "tooltip.supernaturalcraft.authors_pen.mode." + mode;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || player.isShiftKeyDown() || mode(ctx.getItemInHand()) != PenRewrites.BIOME) return InteractionResult.PASS;
        if (!(ctx.getLevel() instanceof ServerLevel level) || !(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        if (cooling(sp, ctx.getItemInHand())) return InteractionResult.FAIL;
        String to = rewriteBiome(level, ctx.getClickedPos());
        if (to == null) return InteractionResult.FAIL;
        written(sp, ctx.getItemInHand(), Vec3.atCenterOf(ctx.getClickedPos().above()), PenRewrites.BIOME,
                Component.translatable("biome." + to.replace(':', '.')));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                int next = (mode(stack) + 1) % PenRewrites.MODES;
                stack.set(AllDataComponents.PEN_MODE.get(), next);
                player.displayClientMessage(Component.translatable("message.supernaturalcraft.authors_pen.mode",
                        Component.translatable(modeKey(next))).withStyle(ChatFormatting.GRAY), true);
                level.playSound(null, player.blockPosition(), AllSounds.PEN_WRITE.get(), SoundSource.PLAYERS, 0.5f, 1.5f);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (mode(stack) != PenRewrites.SKY) return InteractionResultHolder.pass(stack);
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
            if (cooling(sp, stack)) return InteractionResultHolder.fail(stack);
            PenRewrites.Sky sky = rewriteSky(sl);
            written(sp, stack, player.getEyePosition().add(player.getLookAngle()), PenRewrites.SKY,
                    Component.translatable("message.supernaturalcraft.authors_pen.sky." + sky.name().toLowerCase(java.util.Locale.ROOT)));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.isShiftKeyDown() || mode(stack) != PenRewrites.CREATURE) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        if (cooling(sp, stack)) return InteractionResult.FAIL;
        Mob rewritten = rewriteCreature(target);
        if (rewritten == null) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.authors_pen.refused").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return InteractionResult.FAIL;
        }
        written(sp, stack, rewritten.position().add(0, rewritten.getBbHeight() / 2, 0), PenRewrites.CREATURE, rewritten.getType().getDescription());
        return InteractionResult.SUCCESS;
    }

    private static boolean cooling(ServerPlayer p, ItemStack stack) {
        return p.getCooldowns().isOnCooldown(stack.getItem()) && !p.getAbilities().instabuild;
    }

    private static void written(ServerPlayer p, ItemStack stack, Vec3 at, int mode, Component what) {
        ServerLevel level = p.serverLevel();
        level.playSound(null, at.x, at.y, at.z, AllSounds.PEN_WRITE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        level.sendParticles(AllParticles.INK_LETTER.get(), at.x, at.y, at.z, 24, 0.6, 0.6, 0.6, 0.02);
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.authors_pen.written", what).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), true);
        if (!p.getAbilities().instabuild) p.getCooldowns().addCooldown(stack.getItem(), PenRewrites.COOLDOWN[mode]);
    }

    /** Rewrites the biome around {@code at}; returns the new biome's id, or null if it could not. */
    public static String rewriteBiome(ServerLevel level, BlockPos at) {
        Holder<Biome> now = level.getBiome(at);
        String from = now.unwrapKey().map(k -> k.location().toString()).orElse("");
        String to = PenRewrites.nextBiome(from);
        Optional<Holder.Reference<Biome>> biome = level.registryAccess().registryOrThrow(Registries.BIOME)
                .getHolder(ResourceKey.create(Registries.BIOME, ResourceLocation.parse(to)));
        if (biome.isEmpty()) return null;
        int r = PenRewrites.RADIUS;
        var result = FillBiomeCommand.fill(level, at.offset(-r, -r, -r), at.offset(r, r, r), biome.get());
        return result.left().isPresent() ? to : null;
    }

    /** Turns the sky to its next state; returns it. */
    public static PenRewrites.Sky rewriteSky(ServerLevel level) {
        long time = level.getDayTime() % 24000;
        boolean night = time >= 13000 && time < 23000;
        PenRewrites.Sky next = PenRewrites.Sky.of(level.isRaining(), level.isThundering(), night).next();
        switch (next) {
            case CLEAR_DAY -> {
                level.setWeatherParameters(12000, 0, false, false);
                level.setDayTime(level.getDayTime() - time + 24000 + 1000);
            }
            case RAIN -> level.setWeatherParameters(0, 6000, true, false);
            case STORM -> level.setWeatherParameters(0, 6000, true, true);
            case CLEAR_NIGHT -> {
                level.setWeatherParameters(12000, 0, false, false);
                level.setDayTime(level.getDayTime() - time + 14000);
            }
        }
        return next;
    }

    /** Rewrites {@code target} into the next creature of its tier; null if the Pen may not touch it. */
    @SuppressWarnings("unchecked")
    public static Mob rewriteCreature(LivingEntity target) {
        if (!(target instanceof Mob mob) || target instanceof AuthorNpcEntity) return null;
        String id = EntityType.getKey(target.getType()).toString();
        String to = PenRewrites.rewrite(id, target.getType().is(AllTags.Entities.BOSSES));
        if (to == null) return null;
        Optional<EntityType<?>> type = EntityType.byString(to);
        if (type.isEmpty()) return null;
        float health = mob.getHealth() / mob.getMaxHealth();
        Mob out = mob.convertTo((EntityType<? extends Mob>) type.get(), false);
        if (out == null) return null;
        out.setHealth(Math.max(1, out.getMaxHealth() * health));
        return out;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.authors_pen").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.authors_pen.current", Component.translatable(modeKey(mode(stack))))
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.authors_pen.sneak").withStyle(ChatFormatting.DARK_GRAY));
    }
}
