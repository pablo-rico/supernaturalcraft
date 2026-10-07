package org.papiricoh.supernaturalcraft.entity.boss.chuck;

/**
 * What the renderer needs to draw the Author, shared by the boss ({@link ChuckEntity}) and the man at home
 * ({@code author.AuthorNpcEntity}), so one renderer serves both.
 */
public interface ChuckLook {

    /** Which outfit the man wears (ignored while {@link #divine()}). */
    Chapter.Outfit outfit();

    /** Whether he is the light (the divine model) rather than the man. */
    boolean divine();

    /** How far the script has broken, 0-1: cracks of light in the man, fractures in the light. */
    default float crack() {
        return 0f;
    }
}
