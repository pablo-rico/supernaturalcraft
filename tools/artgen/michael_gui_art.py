"""Michael's HUD textures (v0.12), textures/gui/michael/, laid out exactly as the contract (MichaelGui draws them):

  bar_frame 256x32       gold filigree round the bar; the fill's rect x 24..232, y 12..20 is left clear
  bar_fill  208x64       8 frames of 208x8: blue-white light shimmering along (frame = time)
  bar_wing  64x256       8 frames of 64x32: the left wing, folded (0) to open (7); its root at the right edge
  feather   16x16        a small falling feather (tinted by the code)
  enochian  208x16       26 glyphs (A-Z) of 8x16, white on clear (tinted)
  title_rays 256x256     radial rays, white on clear (drawn additive, tinted gold)
  title_wings 256x512    8 frames of 256x64: a pair of wings opening
  title_flourish 256x24  a gold underline flourish (drawn growing from the middle)
  yes_panel 256x160      the "I need your yes" panel: a parchment of light in a gold border
  mark      32x32        Heaven's mark (white: drawn glowing, tinted gold)
  flight    96x16        top 96x8 the frame, bottom 96x8 the fill (white: tinted by the code)
"""

import math

from common import save
from michael_art import GOLD, LIGHT, glyph_bit, tone
from pixelkit import Tex, fbm, hexc, mix

CLEAR = (0, 0, 0, 0)
INK = hexc("#3a2a12")
WHITE = hexc("#ffffff")


def blend(t, x, y, c, a=1.0):
    """Paints c over the pixel with coverage a (works on clear pixels too)."""
    if not (0 <= x < t.w and 0 <= y < t.h) or a <= 0:
        return
    d = t.rows[int(y)][int(x)]
    a = min(1.0, a) * (c[3] / 255 if len(c) > 3 else 1)
    if d[3] == 0:
        t.rows[int(y)][int(x)] = (c[0], c[1], c[2], int(255 * a))
        return
    da = d[3] / 255
    oa = a + da * (1 - a)
    rgb = tuple(int((c[i] * a + d[i] * da * (1 - a)) / oa) for i in range(3))
    t.rows[int(y)][int(x)] = rgb + (int(255 * oa),)


def gold_at(y, h):
    return tone(GOLD, 4.6 - 2.6 * y / max(1, h - 1))


# --- the boss bar -------------------------------------------------------------------------------------------

def bar_frame():
    t = Tex(256, 32, 9801)
    x0, x1, y0, y1 = 24, 232, 12, 20
    # The frame round the fill: two gold rails with a bevel, a dark inner line.
    for x in range(x0 - 3, x1 + 3):
        for y in (y0 - 3, y0 - 2, y1 + 1, y1 + 2):
            t.set(x, y, gold_at(y - (y0 - 3), 14) if y < y0 else gold_at(y - y1 + 2, 6))
        t.set(x, y0 - 1, GOLD[1])
        t.set(x, y1, GOLD[1])
    for y in range(y0 - 3, y1 + 3):
        for x in (x0 - 3, x0 - 2, x1 + 1, x1 + 2):
            t.set(x, y, gold_at(y - (y0 - 3), 14))
        t.set(x0 - 1, y, GOLD[1])
        t.set(x1, y, GOLD[1])
    # Filigree along the top rail: little scrolls every 16 px, beads between.
    for i, cx in enumerate(range(x0 + 8, x1 - 4, 16)):
        for a in range(0, 360, 20):
            r = 2.6
            px, py = cx + r * math.cos(math.radians(a)) * (1 if i % 2 else -1), y0 - 6 + r * math.sin(math.radians(a))
            if a < 300:
                blend(t, round(px), round(py), GOLD[4])
        t.set(cx + 6, y0 - 4, GOLD[5])
        t.set(cx + 8, y0 + 10, GOLD[3])
        t.set(cx + 7, y0 + 11, GOLD[4])
        t.set(cx + 9, y0 + 11, GOLD[4])
    # Ends: a pointed cap each side, a gem of light.
    for side in (-1, 1):
        ex = x0 - 4 if side < 0 else x1 + 3
        for k in range(10):
            for dy in range(-(5 - k // 2), 6 - k // 2):
                x = ex + side * k
                y = 16 + dy
                t.set(x, y, gold_at(dy + 5, 11) if abs(dy) < 5 - k // 2 else GOLD[1])
        gx = ex + side * 3
        for dx in range(-1, 2):
            for dy in range(-1, 2):
                t.set(gx + dx, 16 + dy, LIGHT[5] if dx == dy == 0 else LIGHT[3])
    # The centre medallion over the bar: a sunburst in a gold ring, rising above the top rail.
    cx, cy = 128, 6
    for y in range(0, 13):
        for x in range(cx - 7, cx + 8):
            r = math.hypot(x - cx, y - cy)
            if r <= 2.2:
                t.set(x, y, mix(WHITE, LIGHT[3], r / 2.2))
            elif r <= 3.4:
                t.set(x, y, GOLD[5])
            elif r <= 5.8 and (math.degrees(math.atan2(y - cy, x - cx)) % 45) < 14:
                t.set(x, y, GOLD[4])
            elif abs(r - 6.0) < 0.6:
                t.set(x, y, GOLD[2])
    return t


def bar_fill():
    t = Tex(208, 64, 9802)
    for f in range(8):
        for y in range(8):
            for x in range(208):
                # A bright core band, light pulses travelling right, a glittering edge.
                core = 1 - abs(y - 3.5) / 4.0
                wave = 0.5 + 0.5 * math.sin((x - f * 26) * 2 * math.pi / 52)
                n = fbm(x + f * 13, y, 9803, 208, 8, 2, 16.0)
                v = 2.2 + 2.6 * core + 0.8 * wave * core + 0.6 * (n - 0.5)
                c = tone(LIGHT, v)
                if y in (0, 7):
                    c = mix(c, GOLD[4], 0.25)
                if (x * 7 + f * 11 + y * 3) % 41 == 0 and core > 0.5:
                    c = WHITE
                t.set(x, f * 8 + y, c)
    return t


def wing_shape(t, ox, oy, w, h, open_k, side=-1, gold=True, alpha=1.0):
    """A wing drawn into a box: its root at the inner edge (right for side=-1), the arm sweeping out and up as it opens,
    rows of feathers hanging from the arm. open_k 0 = folded, 1 = spread."""
    rx = ox + (w - 4 if side < 0 else 3)
    ry = oy + h * 0.55
    ang = math.radians(-6 - 46 * open_k)   # the arm's angle above horizontal, outward
    reach = w * (0.42 + 0.52 * open_k)
    n = 15
    for i in range(n):
        f = i / (n - 1)
        # Point on the arm.
        ax = rx + side * reach * f * math.cos(ang)
        ay = ry + reach * f * math.sin(ang) * 0.9
        # A feather hanging from it, fanning outward with the opening.
        length = h * (0.28 + 0.5 * f ** 0.8) * (0.55 + 0.45 * open_k)
        fa = math.radians(90 - (10 + 60 * f) * open_k)  # from straight down toward outward
        dx, dy = side * math.cos(fa), math.sin(fa)
        width = 2.8 + 1.6 * f
        for s in range(int(length * 3)):
            q = s / max(1, length * 3 - 1)
            px, py = ax + dx * q * length, ay + dy * q * length
            hw = width * (1 - max(0, q - 0.6) / 0.4 * 0.9)
            for k in range(-int(hw + 1), int(hw + 2)):
                qx, qy = px + (-dy) * k * 0.6, py + dx * k * 0.6
                if abs(k) <= hw:
                    edge = abs(k) > hw - 1
                    c = tone(LIGHT, 4.8 - 1.8 * q) if not edge else (GOLD[4] if gold and q < 0.3 else LIGHT[2])
                    blend(t, round(qx), round(qy), c, alpha)
    # The arm itself: a gold leading edge.
    for s in range(int(reach * 2)):
        f = s / max(1, reach * 2 - 1)
        ax = rx + side * reach * f * math.cos(ang)
        ay = ry + reach * f * math.sin(ang) * 0.9
        for k in (-1, 0):
            blend(t, round(ax), round(ay + k), GOLD[5] if k else GOLD[3], alpha)


def bar_wing():
    t = Tex(64, 256, 9804)
    for f in range(8):
        wing_shape(t, 0, f * 32, 64, 32, f / 7, side=-1)
    return t


def feather():
    t = Tex(16, 16, 9805)
    for s in range(26):
        q = s / 25
        px, py = 3 + q * 10, 13 - q * 10
        hw = 2.2 * (1 - max(0, q - 0.65) / 0.35) if q > 0.08 else 0.6
        for k in range(-3, 4):
            qx, qy = px + k * 0.7, py + k * 0.7
            if abs(k) <= hw:
                blend(t, round(qx), round(qy), WHITE if abs(k) < 0.5 else hexc("#d8e2ee"))
    return t


# --- Enochian ---------------------------------------------------------------------------------------------

def glyph_cell(letter, seed):
    """An 8x16 cell: either a hand-drawn letter or one built from the shared glyph table, scaled to 6x12 with hooks."""
    cell = [[0] * 8 for _ in range(16)]
    g = [[glyph_bit(seed, ord(letter) - 65, 0, cx, cy) for cx in range(3)] for cy in range(5)]
    # Thicken each glyph bit into a 2x2 stroke on a 6x10 grid, then add a tail (Enochian letters hang below the line).
    for cy in range(5):
        for cx in range(3):
            if g[cy][cx]:
                for dy in range(2):
                    for dx in range(2):
                        cell[2 + cy * 2 + dy][1 + cx * 2 + dx] = 1
    tail = (ord(letter) * 7) % 4
    if tail == 1:
        for y in range(12, 15):
            cell[y][2] = 1
        cell[14][3] = 1
    elif tail == 2:
        cell[12][5] = cell[13][6] = 1
    elif tail == 3:
        cell[1][4] = cell[1][5] = 1
    return cell


def enochian():
    t = Tex(208, 16, 9806)
    for i in range(26):
        letter = chr(65 + i)
        cell = glyph_cell(letter, 9807)
        for y in range(16):
            for x in range(8):
                if cell[y][x]:
                    t.set(i * 8 + x, y, WHITE)
    return t


# --- the title card ---------------------------------------------------------------------------------------

def title_rays():
    t = Tex(256, 256, 9808)
    for y in range(256):
        for x in range(256):
            dx, dy = x - 127.5, y - 127.5
            r = math.hypot(dx, dy)
            if r > 127 or r < 4:
                continue
            a = math.degrees(math.atan2(dy, dx)) % 360
            # 24 rays of two widths, softening outward; a glow at the centre.
            k = abs(((a + 7.5) % 15) - 7.5) / 7.5
            width = 0.18 if int((a + 7.5) // 15) % 2 else 0.32
            ray = max(0.0, 1 - k / width)
            fall = (1 - r / 128) ** 1.2
            glow = max(0.0, 1 - r / 46) ** 2
            v = min(1.0, ray * fall + glow * 0.8)
            if v > 0.02:
                t.set(x, y, (255, 255, 255, int(255 * v)))
    return t


def title_wings():
    t = Tex(256, 512, 9809)
    for f in range(8):
        k = f / 7
        wing_shape(t, 0, f * 64, 126, 64, k, side=-1)
        wing_shape(t, 130, f * 64, 126, 64, k, side=1)
    return t


def title_flourish():
    t = Tex(256, 24, 9810)
    cx, cy = 127.5, 12
    for x in range(4, 252):
        d = abs(x - cx) / 124
        th = 1.6 * (1 - d) + 0.4
        y = cy + 2.5 * math.sin((x - cx) / 9) * d
        for yy in range(24):
            if abs(yy - y) <= th:
                t.set(x, yy, GOLD[5] if yy < y else GOLD[3])
    # Curls at the ends and a diamond at the centre.
    for side in (-1, 1):
        ex = cx + side * 112
        for a in range(0, 330, 10):
            r = 4 - a / 120
            blend(t, round(ex + side * r * math.cos(math.radians(a))), round(cy - 3 + r * math.sin(math.radians(a))), GOLD[4])
    for y in range(4, 21):
        for x in range(120, 136):
            if abs(x - cx) + abs(y - cy) <= 7:
                t.set(x, y, LIGHT[5] if abs(x - cx) + abs(y - cy) <= 3 else GOLD[4])
    return t


def yes_panel():
    W, H = 256, 160
    t = Tex(W, H, 9811)
    for y in range(H):
        for x in range(W):
            e = min(x, y, W - 1 - x, H - 1 - y)
            if e < 2:
                c = GOLD[2] if e == 0 else GOLD[4]
            elif e < 5:
                c = gold_at(e - 2, 3)
            elif e == 5:
                c = GOLD[1]
            else:
                # A parchment of light: warm white to pale blue, a soft glow at the centre, faint vellum grain.
                r = math.hypot((x - W / 2) / W, (y - H / 2) / H)
                n = fbm(x, y, 9812, 256, 160, 3, 12.0)
                c = mix(hexc("#fffdf4"), hexc("#e3f0ff"), min(1.0, r * 1.6))
                c = mix(c, hexc("#f2e6c8"), max(0.0, min(1.0, (n - 0.5) * 0.3 + 0.08)))
                c = c[:3] + (246,)
            t.set(x, y, c)
    # Corner filigree and a script line under the top border.
    for cx, cy in ((10, 10), (W - 11, 10), (10, H - 11), (W - 11, H - 11)):
        for a in range(0, 360, 12):
            for r in (3.0, 5.0):
                blend(t, round(cx + r * math.cos(math.radians(a))), round(cy + r * math.sin(math.radians(a))), GOLD[4] if r < 4 else GOLD[3])
        t.set(cx, cy, LIGHT[3])
    for x in range(24, W - 24):
        gx, cx = divmod(x - 24, 4)
        for cy in range(5):
            if cx < 3 and glyph_bit(9813, gx, 0, cx, cy):
                t.set(x, 9 + cy, mix(GOLD[3], LIGHT[2], 0.3))
    return t


def mark():
    t = Tex(32, 32, 9814)
    c = 15.5
    for y in range(32):
        for x in range(32):
            r = math.hypot(x - c, y - c)
            a = math.degrees(math.atan2(y - c, x - c)) % 360
            on = abs(r - 13.5) < 1.0 or abs(r - 10.5) < 0.6
            # A six-pointed star of two triangles and an eye at the centre.
            for k in range(2):
                for j in range(3):
                    a0 = math.radians(90 + k * 60 + j * 120)
                    a1 = math.radians(90 + k * 60 + (j + 1) * 120)
                    p0 = (c + 10 * math.cos(a0), c + 10 * math.sin(a0))
                    p1 = (c + 10 * math.cos(a1), c + 10 * math.sin(a1))
                    vx, vy = p1[0] - p0[0], p1[1] - p0[1]
                    L = math.hypot(vx, vy)
                    tt = max(0, min(1, ((x - p0[0]) * vx + (y - p0[1]) * vy) / (L * L)))
                    if math.hypot(x - p0[0] - vx * tt, y - p0[1] - vy * tt) < 0.7:
                        on = True
            if abs((x - c) / 4.5) ** 2 + abs((y - c) / 2.4) ** 2 < 1.0:
                on = not (r < 1.3)
            if on:
                t.set(x, y, WHITE)
            elif 13.5 < r < 15.5 and int(a) % 30 < 4:
                t.set(x, y, (255, 255, 255, 170))
    return t


def flight():
    t = Tex(96, 16, 9815)
    for y in range(8):
        for x in range(96):
            e = min(x, y, 95 - x, 7 - y)
            if e == 0:
                t.set(x, y, GOLD[2] if y == 7 else GOLD[4])
            elif e == 1:
                t.set(x, y, GOLD[1])
            else:
                t.set(x, y, (16, 16, 32, 200))
            # The fill row: white light (tinted by the code), a bright core and feathered ends.
            if 2 <= y <= 5 and 2 <= x <= 93:
                v = 255 if y in (3, 4) else 205
                t.set(x, 8 + y, (v, v, v, 255))
    for x in range(0, 96, 12):
        t.set(x + 6, 0, GOLD[5])
    return t


TEXTURES = {"bar_frame": bar_frame, "bar_fill": bar_fill, "bar_wing": bar_wing, "feather": feather, "enochian": enochian,
            "title_rays": title_rays, "title_wings": title_wings, "title_flourish": title_flourish, "yes_panel": yes_panel,
            "mark": mark, "flight": flight}


def generate():
    for name, fn in TEXTURES.items():
        save(fn(), "gui/michael", name)


if __name__ == "__main__":
    generate()
