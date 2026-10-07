package org.papiricoh.supernaturalcraft.journal;

import java.util.ArrayList;
import java.util.List;

/**
 * Lays an entry's blocks down the pages ("faces": left page, right page, the next spread's left
 * page...). Pure, so it is tested in JUnit: the book measures each block first (a paragraph as the
 * heights of its wrapped lines, anything else as one height) and draws what this returns.
 *
 * <p>Rules: a paragraph that does not fit flows on to the next face between two lines; anything
 * else never splits, and moves to the next face whole (one taller than a face gets a face to
 * itself); blocks are {@code gap} apart, but a face never starts with a gap; the title takes the
 * top of the first face.
 */
public final class JournalLayout {

    /** One block as measured: a paragraph's line heights, or {@code lines == null} and a fixed height. */
    public record Measured(List<Integer> lines, int height) {

        public static Measured text(List<Integer> lineHeights) {
            return new Measured(List.copyOf(lineHeights), lineHeights.stream().mapToInt(Integer::intValue).sum());
        }

        public static Measured fixed(int height) {
            return new Measured(null, height);
        }

        public boolean isText() {
            return lines != null;
        }
    }

    /**
     * The part of block {@code block} on a face: lines {@code first..last} inclusive of a paragraph,
     * or {@code 0..0} for a block drawn whole. {@code y} is where it starts, from the face's top.
     */
    public record Slice(int block, int first, int last, int y) {
    }

    private JournalLayout() {
    }

    /**
     * @param blocks      the measured blocks, in order
     * @param faceHeight  the height of a face
     * @param titleHeight how much of the first face the title takes (0 for none)
     * @param gap         space between two blocks on a face
     * @return the faces, each the slices on it in order; at least one face (an empty entry still has its title)
     */
    public static List<List<Slice>> layout(List<Measured> blocks, int faceHeight, int titleHeight, int gap) {
        List<List<Slice>> faces = new ArrayList<>();
        List<Slice> face = new ArrayList<>();
        int y = titleHeight;
        // Whether the face holds anything yet (the title counts: a block after it is spaced from it).
        boolean used = titleHeight > 0;
        for (int b = 0; b < blocks.size(); b++) {
            Measured m = blocks.get(b);
            if (m.isText()) {
                if (m.lines().isEmpty()) continue;
                int start = -1, startY = 0;
                for (int i = 0; i < m.lines().size(); i++) {
                    int h = m.lines().get(i);
                    int at = start < 0 && used ? y + gap : y;
                    if (at + h > faceHeight && (used || start >= 0)) {
                        // Close what this paragraph put on this face, and carry on at the next one's top.
                        if (start >= 0) face.add(new Slice(b, start, i - 1, startY));
                        faces.add(face);
                        face = new ArrayList<>();
                        y = 0;
                        used = false;
                        start = -1;
                        at = 0;
                    }
                    if (start < 0) {
                        start = i;
                        startY = at;
                    }
                    y = at + h;
                    used = true;
                }
                face.add(new Slice(b, start, m.lines().size() - 1, startY));
            } else {
                int at = used ? y + gap : y;
                if (at + m.height() > faceHeight && used) {
                    faces.add(face);
                    face = new ArrayList<>();
                    at = 0;
                }
                face.add(new Slice(b, 0, 0, at));
                y = at + m.height();
                used = true;
            }
        }
        if (!face.isEmpty() || faces.isEmpty()) faces.add(face);
        return faces;
    }
}
