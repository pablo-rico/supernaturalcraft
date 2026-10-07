package org.papiricoh.supernaturalcraft.bowl.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlCast;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlSpellEffect;
import org.papiricoh.supernaturalcraft.network.SmokeTrailPayload;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * The Locating spell: the bowl's smoke rises and drifts toward whoever's blood is in it
 * ({@code target: player}), the pet whose bound collar is among the ingredients ({@code pet}; the
 * collar comes back), or the nearest structure in a tag ({@code structure}). A target in another
 * dimension, or nowhere, leaves the bowl as it was.
 */
public record LocateEffect(Target target, Optional<TagKey<Structure>> structure) implements BowlSpellEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("locate");
    /** Structures are searched this many chunks out, like {@code /locate}. */
    public static final int STRUCTURE_SEARCH_CHUNKS = 100;
    /** Players this close to the bowl see the trail. */
    public static final double TRAIL_AUDIENCE = 64;
    /** Ticks the trail's wisps hang in the air. */
    public static final int TRAIL_LINGER = 600;

    public enum Target implements StringRepresentable {
        PLAYER, PET, STRUCTURE;

        public static final Codec<Target> CODEC = StringRepresentable.fromEnum(Target::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final MapCodec<LocateEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Target.CODEC.fieldOf("target").forGetter(LocateEffect::target),
            TagKey.hashedCodec(Registries.STRUCTURE).optionalFieldOf("structure").forGetter(LocateEffect::structure)
    ).apply(i, LocateEffect::new));

    /** Where the target is: found here (with its position), in another world, or nowhere at all. */
    public record Located(Kind kind, @Nullable Vec3 pos, String name) {
        public enum Kind { FOUND, ELSEWHERE, DEAD, LOST }

        static Located found(Vec3 pos, String name) {
            return new Located(Kind.FOUND, pos, name);
        }

        static Located of(Kind kind, String name) {
            return new Located(kind, null, name);
        }
    }

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Nullable
    @Override
    public String precheck(BowlCast cast) {
        return switch (target) {
            case PLAYER -> cast.bloodOwner().isEmpty() ? "message.supernaturalcraft.locate.no_blood" : null;
            case PET -> collar(cast).isEmpty() ? "message.supernaturalcraft.locate.no_collar" : null;
            case STRUCTURE -> structure.isEmpty() ? "message.supernaturalcraft.locate.not_found" : null;
        };
    }

    @Override
    public boolean perform(BowlCast cast) {
        Located where = resolve(cast);
        ServerPlayer caster = cast.caster();
        switch (where.kind()) {
            case ELSEWHERE -> {
                caster.displayClientMessage(Component.translatable("message.supernaturalcraft.locate.elsewhere", where.name()).withStyle(ChatFormatting.GRAY), false);
                return false;
            }
            case DEAD -> {
                caster.displayClientMessage(Component.translatable("message.supernaturalcraft.locate.dead", where.name()).withStyle(ChatFormatting.GRAY), false);
                return false;
            }
            case LOST -> {
                caster.displayClientMessage(Component.translatable("message.supernaturalcraft.locate.not_found").withStyle(ChatFormatting.GRAY), false);
                return false;
            }
            default -> {
            }
        }
        Vec3 from = cast.surface();
        Vec3 to = where.pos();
        ServerLevel level = cast.level();
        PacketDistributor.sendToPlayersNear(level, null, from.x, from.y, from.z, TRAIL_AUDIENCE,
                new SmokeTrailPayload(from, to, cast.recipe().smokeColor() & 0xFFFFFF, TRAIL_LINGER));
        level.playSound(null, from.x, from.y, from.z, AllSounds.SMOKE_TRAIL.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        if (target == Target.PET) collar(cast).ifPresent(cast::giveBack);
        caster.displayClientMessage(driftLine(from, to), true);
        return true;
    }

    /** "The smoke drifts north-east, far away." */
    public static Component driftLine(Vec3 from, Vec3 to) {
        double dx = to.x - from.x, dz = to.z - from.z;
        Bearing.Band band = Bearing.band(Math.sqrt(dx * dx + dz * dz));
        if (band == Bearing.Band.HERE) {
            return Component.translatable("message.supernaturalcraft.locate.here").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        }
        return Component.translatable("message.supernaturalcraft.locate.drift",
                Component.translatable(Bearing.compass(dx, dz).key()), Component.translatable(band.key()))
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
    }

    /** Where this spell's target is, from the bowl in {@code cast}. Public for tests. */
    public Located resolve(BowlCast cast) {
        return switch (target) {
            case PLAYER -> cast.bloodOwner().map(owner -> findPlayer(cast, owner))
                    .orElse(Located.of(Located.Kind.LOST, ""));
            case PET -> collar(cast).map(c -> findPet(cast, PetCollarItem.bond(c))).orElse(Located.of(Located.Kind.LOST, ""));
            case STRUCTURE -> findStructure(cast);
        };
    }

    private static Located findPlayer(BowlCast cast, UUID owner) {
        String name = cast.blood().flatMap(d -> d.ownerName()).orElse("?");
        Player found = null;
        if (cast.caster().getUUID().equals(owner)) found = cast.caster();
        MinecraftServer server = cast.level().getServer();
        if (found == null) found = server.getPlayerList().getPlayer(owner);
        if (found == null) {
            for (ServerLevel l : server.getAllLevels()) {
                if (l.getEntity(owner) instanceof Player p) {
                    found = p;
                    break;
                }
            }
        }
        if (found == null || found.isRemoved()) return Located.of(Located.Kind.LOST, name);
        if (found.level() != cast.level()) return Located.of(Located.Kind.ELSEWHERE, name);
        return Located.found(found.position().add(0, found.getBbHeight() * 0.6, 0), found.getName().getString());
    }

    private static Located findPet(BowlCast cast, PetBond bond) {
        MinecraftServer server = cast.level().getServer();
        for (ServerLevel l : server.getAllLevels()) {
            Entity e = l.getEntity(bond.pet());
            if (e != null && e.isAlive()) {
                if (l != cast.level()) return Located.of(Located.Kind.ELSEWHERE, bond.name());
                return Located.found(e.position().add(0, e.getBbHeight() * 0.6, 0), bond.name());
            }
        }
        Optional<PetLedger.Entry> entry = PetLedger.get(server).entry(bond.pet());
        if (entry.isEmpty()) return Located.of(Located.Kind.LOST, bond.name());
        PetLedger.Entry e = entry.get();
        if (!e.alive()) return Located.of(Located.Kind.DEAD, bond.name());
        ResourceKey<Level> here = cast.level().dimension();
        if (!e.dimension().equals(here)) return Located.of(Located.Kind.ELSEWHERE, bond.name());
        return Located.found(Vec3.atCenterOf(e.lastPos()), bond.name());
    }

    private Located findStructure(BowlCast cast) {
        if (structure.isEmpty()) return Located.of(Located.Kind.LOST, "");
        ServerLevel level = cast.level();
        BlockPos at = level.findNearestMapStructure(structure.get(), cast.bowl(), STRUCTURE_SEARCH_CHUNKS, false);
        if (at == null) return Located.of(Located.Kind.LOST, "");
        double y = level.isLoaded(at) ? level.getHeight(Heightmap.Types.WORLD_SURFACE, at.getX(), at.getZ()) : cast.surface().y;
        return Located.found(new Vec3(at.getX() + 0.5, y, at.getZ() + 0.5), "");
    }

    /** The first bound collar among the ingredients. */
    static Optional<ItemStack> collar(BowlCast cast) {
        return cast.contents().stacks().stream()
                .filter(s -> s.is(AllItems.PET_COLLAR.get()) && PetCollarItem.bond(s) != null).findFirst();
    }
}
