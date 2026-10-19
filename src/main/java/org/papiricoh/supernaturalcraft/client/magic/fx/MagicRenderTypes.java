package org.papiricoh.supernaturalcraft.client.magic.fx;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * The magic's own render types (v0.21): light that adds to what is behind it (source alpha, one), never writes depth and shows
 * both faces. No new shaders: the untextured one runs on vanilla's lightning shader, the textured one on the emissive entity
 * shader. Dark things (smoke, a rift's inside) are drawn with ordinary alpha blending instead ({@code WorldDraw.quads}).
 *
 * <p>Additive light vanishes against a bright daytime sky or sunlit grass, so by day {@link FxDraw} lays a second, ordinarily
 * blended pass ("ink") under the cores and edges: the same colours, painted rather than added ({@link #ink()}).
 */
public final class MagicRenderTypes {

    /** Coloured light, POSITION_COLOR quads. */
    private static final RenderType GLOW = RenderType.create("sn_magic_glow", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 1536, false, true, RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    /** A white-on-transparent texture drawn as light in the vertex colour, NEW_ENTITY quads. */
    private static final Function<ResourceLocation, RenderType> GLOW_TEX = Util.memoize(texture -> RenderType.create(
            "sn_magic_glow_tex", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .createCompositeState(false)));

    /** As {@link #GLOW} but painted (ordinary alpha blending): the daylight pass. */
    private static final RenderType INK = RenderType.create("sn_magic_ink", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 1536, false, true, RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    /** As {@link #GLOW_TEX} but painted: the daylight pass of a textured quad. */
    private static final Function<ResourceLocation, RenderType> INK_TEX = Util.memoize(texture -> RenderType.create(
            "sn_magic_ink_tex", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .createCompositeState(false)));

    private MagicRenderTypes() {
    }

    public static RenderType glow() {
        return GLOW;
    }

    public static RenderType glow(ResourceLocation texture) {
        return GLOW_TEX.apply(texture);
    }

    public static RenderType ink() {
        return INK;
    }

    public static RenderType ink(ResourceLocation texture) {
        return INK_TEX.apply(texture);
    }
}
