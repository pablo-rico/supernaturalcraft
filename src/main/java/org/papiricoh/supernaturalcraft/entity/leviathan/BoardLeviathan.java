package org.papiricoh.supernaturalcraft.entity.leviathan;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * One of SucroCorp's people (v0.28): a Leviathan of the Chain of Command's third faction, wearing no stolen face but the
 * company's own: an executive's suit, a security guard's uniform or a chemist's coat ({@link Skin}). Same body, same maw, same
 * rules as any Leviathan (decapitation, borax); never disguised. Which skin it is follows from its entity type.
 */
public class BoardLeviathan extends LeviathanEntity {

    public enum Skin {
        EXECUTIVE(46.0, 6.5, 0.0),
        GUARD(40.0, 6.0, 4.0),
        CHEMIST(36.0, 5.5, 0.0);

        private final double health, damage, armor;

        Skin(double health, double damage, double armor) {
            this.health = health;
            this.damage = damage;
            this.armor = armor;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** Its entity id ({@code leviathan_<skin>}). */
        public String entityId() {
            return "leviathan_" + id();
        }
    }

    /** The officer it is (its chain's id), -1 for none: synced so the client finds its record (bar, name, voice). */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> OFFICER_ID = net.minecraft.network.syncher.SynchedEntityData
            .defineId(BoardLeviathan.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

    private final Skin skin;

    public BoardLeviathan(EntityType<? extends LeviathanEntity> type, Level level, Skin skin) {
        super(type, level);
        this.skin = skin;
    }

    public static AttributeSupplier.Builder createAttributes(Skin skin) {
        return LeviathanEntity.createAttributes().add(Attributes.MAX_HEALTH, skin.health).add(Attributes.ATTACK_DAMAGE, skin.damage)
                .add(Attributes.ARMOR, skin.armor);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OFFICER_ID, -1);
    }

    public void setOfficerId(int id) {
        entityData.set(OFFICER_ID, id);
    }

    public int officerId() {
        return entityData.get(OFFICER_ID);
    }

    public Skin board() {
        return skin;
    }

    @Override
    public String skin() {
        return skin.id();
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                                  @Nullable SpawnGroupData data) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data);
        setDisguise("");
        return out;
    }
}
