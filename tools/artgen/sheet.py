"""Contact sheet: every texture in a folder, upscaled nearest-neighbour, on a checker background,
so a whole set can be reviewed at a glance."""

import os
import sys

from pngio import read_png, write_png


def sheet(paths, out, scale=6, cols=8, pad=4):
    imgs = [read_png(p) for p in paths]
    cell = max(max(w, h) for w, h, _ in imgs) * scale + pad * 2
    rows_n = (len(imgs) + cols - 1) // cols
    W, H = cols * cell, rows_n * cell
    canvas = [[((60, 60, 66, 255) if ((x // 8 + y // 8) % 2) else (78, 78, 86, 255)) for x in range(W)] for y in range(H)]
    for i, (w, h, rows) in enumerate(imgs):
        s = scale
        ox, oy = (i % cols) * cell + pad, (i // cols) * cell + pad
        for y in range(h):
            for x in range(w):
                c = rows[y][x]
                if c[3] == 0:
                    continue
                for dy in range(s):
                    for dx in range(s):
                        X, Y = ox + x * s + dx, oy + y * s + dy
                        if X < W and Y < H:
                            bg = canvas[Y][X]
                            a = c[3] / 255
                            canvas[Y][X] = tuple(int(c[k] * a + bg[k] * (1 - a)) for k in range(3)) + (255,)
    write_png(out, canvas)


if __name__ == "__main__":
    folder, out = sys.argv[1], sys.argv[2]
    names = sorted(f for f in os.listdir(folder) if f.endswith(".png"))
    sheet([os.path.join(folder, n) for n in names], out, scale=int(sys.argv[3]) if len(sys.argv) > 3 else 6)
    print("\n".join(names))
