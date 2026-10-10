package org.papiricoh.supernaturalcraft.client.heaven.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.papiricoh.supernaturalcraft.client.heaven.render.figure.FigureDraw;
import org.papiricoh.supernaturalcraft.client.heaven.render.figure.FigureSpec;

/**
 * A copy raised by Naomi's training test (v0.18). The kneeling ones are friends (Dean, Sam, Castiel, the hunter's lost pet, the
 * hunter themselves): on their knees, in a soft white light. The hostile ones wear a rival hunter's shape, darkened, a blade in
 * hand. Who it copies, and whether it kneels, come from the entity ({@code figure()}/{@code kneeling()}/{@code hostile()}); see
 * {@link FigureDraw} for how each figure is drawn.
 */
public class TrainingCopyRenderer<E extends Entity> extends EntityRenderer<E> {

    /** The washes: a kneeling friend's pale light, a hostile copy's dark. */
    static final int KNEEL_TINT = 0xFFF2F4FF, HOSTILE_TINT = 0xFF8C8088;

    private final FigureDraw figures;

    public TrainingCopyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        figures = new FigureDraw(ctx);
        shadowRadius = 0.45f;
    }

    /** What the copy shows, read off the entity. */
    public static FigureSpec spec(Entity e) {
        boolean kneeling = Accessors.bool(e, false, "kneeling", "isKneeling", "kneels", "friendly", "isFriendly");
        boolean hostile = Accessors.bool(e, !kneeling, "hostile", "isHostile");
        String figure = Accessors.string(e, "", "figure", "copyOf", "copy", "look", "figureId");
        if (figure.isEmpty() || hostile && !figure.startsWith("@")) {
            figure = hostile ? "@rival:" + Math.floorMod(e.getUUID().hashCode(), 3) : "@ally:dean";
        }
        if (hostile && !kneeling) return new FigureSpec(figure, "stand", 1f, HOSTILE_TINT, 0, true);
        return new FigureSpec(figure, "kneel", 1f, KNEEL_TINT, 10, false);
    }

    @Override
    public void render(E e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float body = e instanceof LivingEntity l ? Mth.rotLerp(partial, l.yBodyRotO, l.yBodyRot) : yaw;
        java.util.UUID owner = e instanceof org.papiricoh.supernaturalcraft.entity.boss.naomi.TrainingCopyEntity copy ? copy.ownerId().orElse(null) : null;
        figures.draw(e, spec(e), owner, body, partial, pose, buffers, light);
        super.render(e, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(E e) {
        return ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
    }
}
