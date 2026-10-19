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
import org.papiricoh.supernaturalcraft.network.Send;

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
    /** How far Trace the Familiar searches (blocks). */
    public static final double FAMILIAR_RANGE = 160;

    /**
     * {@code being} (v0.21, Locate Being): whoever the personal component names, player or creature. {@code familiar} (v0.21,
     * Trace the Familiar): the nearest witch ({@code #supernaturalcraft:witches}) or bound familiar within {@link #FAMILIAR_RANGE}.
     */
    public enum Target implements StringRepresentable {
        PLAYER, PET, STRUCTURE, BEING, FAMILIAR;

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
            case PLAYER -> cast.target().isEmpty() ? "message.supernaturalcraft.locate.no_blood" : null;
            case PET -> collar(cast).isEmpty() ? "message.supernaturalcraft.locate.no_collar" : null;
            case STRUCTURE -> structure.isEmpty() ? "message.supernaturalcraft.locate.not_found" : null;
            case BEING -> cast.target().isEmpty() ? "message.supernaturalcraft.bowl.needs_target" : null;
            case FAMILIAR -> null;
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
        Send.near(level, null, from.x, from.y, from.z, TRAIL_AUDIENCE,
                new SmokeTrailPayload(from, to, cast.recipe().smokeColor() & 0xFFFFFF, TRAIL_LINGER));
        level.playSound(null, from.x, from.y, from.z, AllSounds.SMOKE_TRAIL.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        cast.sendTo(to, null);
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
            case PLAYER -> cast.target().map(t -> findPlayer(cast, t.owner(), t.name()))
                    .orElse(Located.of(Located.Kind.LOST, ""));
            case PET -> collar(cast).map(c -> findPet(cast, PetCollarItem.bond(c))).orElse(Located.of(Located.Kind.LOST, ""));
            case STRUCTURE -> findStructure(cast);
            case BEING -> cast.target().map(t -> findBeing(cast, t.owner(), t.name())).orElse(Located.of(Located.Kind.LOST, ""));
            case FAMILIAR -> findFamiliar(cast);
        };
    }

    private static Located findPlayer(BowlCast cast, UUID owner, String name) {
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
        // A Ward of the Name hides its bearer from the smoke (not from their own bowl).
        if (found != cast.caster() && found.hasEffect(org.papiricoh.supernaturalcraft.registry.AllMobEffects.NAME_WARDED)) {
            return Located.of(Located.Kind.LOST, name);
        }
        if (found.level() != cast.level()) return Located.of(Located.Kind.ELSEWHERE, name);
        return Located.found(found.position().add(0, found.getBbHeight() * 0.6, 0), found.getName().getString());
    }

    /** Any being by id (v0.21): a player as {@link #findPlayer}, else a creature loaded in some world. */
    private static Located findBeing(BowlCast cast, UUID owner, String name) {
        Entity e = org.papiricoh.supernaturalcraft.bowl.witch.NameMagic.anywhere(cast.level(), owner);
        if (e == null || !e.isAlive()) return Located.of(Located.Kind.LOST, name);
        if (e instanceof Player) return findPlayer(cast, owner, name);
        // A Ward of the Name hides its bearer from the smoke.
        if (e instanceof net.minecraft.world.entity.LivingEntity l && l.hasEffect(org.papiricoh.supernaturalcraft.registry.AllMobEffects.NAME_WARDED)) {
            return Located.of(Located.Kind.LOST, name);
        }
        if (e.level() != cast.level()) return Located.of(Located.Kind.ELSEWHERE, name);
        return Located.found(e.position().add(0, e.getBbHeight() * 0.6, 0), e.getName().getString());
    }

    /** The nearest witch or familiar to the bowl (v0.21). */
    private static Located findFamiliar(BowlCast cast) {
        Vec3 c = Vec3.atCenterOf(cast.bowl());
        return cast.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                        new net.minecraft.world.phys.AABB(cast.bowl()).inflate(FAMILIAR_RANGE),
                        e -> e.isAlive() && e.distanceToSqr(c) <= FAMILIAR_RANGE * FAMILIAR_RANGE
                                && (e.getType().is(org.papiricoh.supernaturalcraft.registry.AllTags.Entities.WITCHES)
                                || e.hasData(org.papiricoh.supernaturalcraft.registry.AllAttachments.FAMILIAR) && e.getData(org.papiricoh.supernaturalcraft.registry.AllAttachments.FAMILIAR).isPresent()))
                .stream().min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(c)))
                .map(e -> Located.found(e.position().add(0, e.getBbHeight() * 0.6, 0), e.getName().getString()))
                .orElse(Located.of(Located.Kind.LOST, ""));
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
