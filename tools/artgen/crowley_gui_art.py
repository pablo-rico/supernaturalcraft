"""Crowley's contract bar (v0.35): textures/gui/crowley_bar.png, one 256x128 sheet laid out as client/crowley/CrowleyHud
reads it (the constants there mirror these):

  parchment  (0, 0)    256x40   the contract: aged parchment, deckled and singed edges, foxing, lines of small print, the
                                 signature line (x 40..216, its baseline at y 31) with an "X" before it at x 30
  signature  (0, 40)   176x12   his signature in blood ink (drawn cropped to his health, from the left)
  scorch     (0, 52)   176x12   the same band burnt: char, holes through the paper (drawn where the signature has burnt away)
  burn       (0, 64)   8 frames of 12x20 (x = 12 * frame): the burning edge, flames licking up from it (frame = time)
  seals      (0, 88)   24x24 each: intact (x 0), cracked (x 24), broken (x 48): red wax, a crown pressed into it
"""

import math

from common import save
from pixelkit import Tex, fbm, hexc, mix
from chuck_art import h01

W, H = 256, 128
PARCH = [hexc(c) for c in ("#6e5532", "#8f7346", "#ae915c", "#c8ad75", "#dcc58f", "#ecdcab")]
INK = hexc("#3b2a1a")
BLOOD = [hexc(c) for c in ("#2e0204", "#4e0608", "#700a0c", "#921212", "#b41e1a", "#d83a2c")]
WAX = [hexc(c) for c in ("#2a0204", "#4a0608", "#6c0c0e", "#8e1414", "#b0221c", "#d24a36")]
CHAR = [hexc(c) for c in ("#0c0806", "#1c120c", "#2e1e12", "#4a321c", "#6a4a28")]
FIRE = [hexc(c) for c in ("#7a1406", "#c0320c", "#ff6a1a", "#ffb040", "#ffe8a0")]


def put(t, x, y, c, a=1.0):
    """Paints c over the pixel with coverage a."""
    x, y = int(x), int(y)
    if not (0 <= x < t.w and 0 <= y < t.h) or a <= 0:
        return
    d = t.rows[y][x]
    a = min(1.0, a)
    if d[3] == 0:
        t.rows[y][x] = (c[0], c[1], c[2], int(255 * a))
        return
    da = d[3] / 255
    oa = a + da * (1 - a)
    t.rows[y][x] = tuple(int((c[i] * a + d[i] * da * (1 - a)) / oa) for i in range(3)) + (int(255 * oa),)


def ramp(r, v):
    return r[max(0, min(len(r) - 1, int(round(v))))]


# --- the parchment -------------------------------------------------------------------------------------------------

def parchment(t, ox, oy, w, h, seed):
    for y in range(h):
        for x in range(w):
            # The deckled, singed edge: an irregular margin, browner toward it.
            e = min(x, w - 1 - x, (y) * 2.2, (h - 1 - y) * 2.2)
            wob = 2.2 * fbm(x * 0.7, y * 0.7, seed, 64, 64, 2, 3.0)
            if e < wob:
                continue
            n = fbm(x, y, seed + 1, 128, 128, 3, 18.0)
            v = 3.2 + (n - 0.5) * 2.0
            edge = max(0.0, 1 - (e - wob) / 7.0)
            v -= 2.4 * edge * edge
            c = ramp(PARCH, v)
            # Foxing: small brown spots; a ring stain.
            if fbm(x * 3 + 11, y * 3, seed + 2, 64, 64, 2, 4.0) > 0.78:
                c = mix(c, PARCH[0], 0.35)
            if edge > 0.65:
                c = mix(c, CHAR[2], (edge - 0.65) * 1.8)
            put(t, ox + x, oy + y, c)


def small_print(t, ox, oy, x0, x1, rows, seed):
    """Lines of illegible small print: ink dashes of word lengths."""
    for j, y in enumerate(rows):
        x = x0 + int(h01("indent", seed, j) * 3)
        end = x1 - int(h01("end", seed, j) * (30 if j == len(rows) - 1 else 6))
        while x < end:
            ln = 2 + int(h01("word", seed, j, x) * 6)
            for k in range(ln):
                if x + k >= end:
                    break
                a = 0.45 + 0.3 * h01("ink", seed, j, x + k)
                put(t, ox + x + k, oy + y, INK, a)
                if h01("asc", seed, j, x + k) < 0.18:
                    put(t, ox + x + k, oy + y - 1, INK, a * 0.6)
            x += ln + 2


def contract(t):
    parchment(t, 0, 0, 256, 40, 35901)
    # A ruled border inside the edge, in faded ink.
    for x in range(10, 246):
        if h01("rule", x) > 0.08:
            put(t, x, 4, INK, 0.35)
            put(t, x, 36, INK, 0.35)
    for y in range(4, 37):
        if h01("rule", y, 1) > 0.08:
            put(t, 10, y, INK, 0.35)
            put(t, 245, y, INK, 0.35)
    # The small print, either side of the name's space (the name is drawn by the code over y 6..16).
    small_print(t, 0, 0, 14, 76, (8, 11, 14), 35902)
    small_print(t, 0, 0, 180, 242, (8, 11, 14), 35903)
    small_print(t, 0, 0, 14, 242, (19, 22), 35904)
    # The signature line, dotted, and the X before it.
    for x in range(40, 217):
        if x % 3 != 2:
            put(t, x, 32, INK, 0.7)
    for i in range(5):
        put(t, 29 + i, 27 + i, INK, 0.85)
        put(t, 33 - i, 27 + i, INK, 0.85)


# --- the signature -------------------------------------------------------------------------------------------------

def signature(t, ox, oy, w=176, h=12):
    """A cursive scrawl in blood: a big looped capital, running loops, a long underline flourish."""
    def dot(x, y, r, c, a=1.0):
        for dy in range(-1, 2):
            for dx in range(-1, 2):
                d = math.hypot(dx, dy)
                if d <= r:
                    put(t, ox + x + dx, oy + y + dy, c, a * (1.0 if d < r - 0.4 else 0.55))

    def stroke(points, width):
        for i in range(len(points) - 1):
            (x0, y0), (x1, y1) = points[i], points[i + 1]
            n = max(1, int(math.hypot(x1 - x0, y1 - y0) * 2))
            for k in range(n + 1):
                u = k / n
                x, y = x0 + (x1 - x0) * u, y0 + (y1 - y0) * u
                dot(round(x), round(y), width, BLOOD[2])
    # The capital: a tall loop.
    cap = [(6 + 5 * math.cos(a), 6 - 5 * math.sin(a) * 1.0) for a in [math.pi * 0.2 + i * 0.25 for i in range(26)]]
    stroke(cap, 0.9)
    # The running hand: a prolate cycloid (looping) in two words, the letters' heights wandering, a few tall ones.
    for w0, w1, seed in ((12, 92, 1), (100, w - 22, 2)):
        pts = []
        n = int((w1 - w0) * 6)
        cycles = (w1 - w0) / 5.2
        for i in range(n + 1):
            s = i / n
            tt = s * cycles * 2 * math.pi
            k = int(tt / (2 * math.pi))
            tall = 1.0 + (1.3 if h01("tall", seed, k) < 0.22 else 0.0)
            amp = (2.4 + 0.8 * math.sin(s * 7.0 + seed)) * tall
            x = w0 + s * (w1 - w0) - 1.5 * math.sin(tt)
            y = 7.4 - amp * (0.5 - 0.5 * math.cos(tt)) + 0.6 * math.sin(s * 4 + seed)
            pts.append((x, y))
        stroke(pts, 0.6)
    # The flourish under it, sweeping back.
    fl = [(w - 16 + 10 * math.cos(a), 9 + 2 * math.sin(a)) for a in [i * 0.2 for i in range(0, 16)]]
    fl += [(w - 26 - i * 9, 10.5 + 0.4 * math.sin(i)) for i in range(16)]
    stroke(fl, 0.6)
    # Wet highlights and the darker pooled ink.
    for y in range(h):
        for x in range(w):
            c = t.rows[oy + y][ox + x]
            if c[3] == 0:
                continue
            r = h01("pool", x, y)
            if r < 0.15:
                t.rows[oy + y][ox + x] = BLOOD[1][:3] + (c[3],)
            elif r > 0.9:
                t.rows[oy + y][ox + x] = BLOOD[4][:3] + (c[3],)
    # Blots.
    for x, y in ((8, 11), (70, 2), (150, 11)):
        dot(x, y, 1.2, BLOOD[1])


def scorch(t, ox, oy, w=176, h=12):
    """The burnt band: char with ragged top and bottom edges, holes eaten through, an ember-brown rim."""
    for x in range(w):
        top = 1.5 + 1.5 * fbm(x * 0.9, 0, 35911, 256, 8, 2, 5.0)
        bot = h - 1.5 - 1.5 * fbm(x * 0.9, 3, 35912, 256, 8, 2, 5.0)
        for y in range(h):
            if y < top - 1 or y > bot + 1:
                continue
            rim = y < top or y > bot
            n = fbm(x * 0.7, y * 1.3, 35910, 256, 64, 2, 5.0)
            if not rim and n > 0.6:
                continue                                   # a hole through the paper
            if rim:
                put(t, ox + x, oy + y, CHAR[4], 0.7)
            else:
                put(t, ox + x, oy + y, ramp(CHAR, 0.6 + 2.2 * n), 0.95)


def burn_frames(t, ox, oy, fw=12, fh=20, frames=8):
    """The burning edge: a wavering line of embers (x 5..6), flames above it flickering (y 0..8)."""
    for f in range(frames):
        x0 = ox + f * fw
        ph = f / frames * 2 * math.pi
        for y in range(4, fh):
            wob = 0.8 * math.sin(y * 0.9 + ph) + 0.5 * math.sin(y * 2.3 - ph * 2)
            cx = 5.5 + wob
            for x in range(fw):
                d = abs(x - cx)
                if d < 0.8:
                    put(t, x0 + x, oy + y, FIRE[4] if (y + f) % 3 else FIRE[3])
                elif d < 1.8:
                    put(t, x0 + x, oy + y, FIRE[2], 0.95)
                elif d < 3.0 and x > cx:
                    put(t, x0 + x, oy + y, FIRE[0], 0.8 - (d - 1.8) * 0.4)
                elif d < 2.6:
                    put(t, x0 + x, oy + y, CHAR[1], 0.8)
        # Flames: three tongues licking up off the edge, their heights flickering.
        for k, base in enumerate((6, 10, 14)):
            hgt = 3 + 3 * (0.5 + 0.5 * math.sin(ph * (1 + k * 0.5) + k * 2.1))
            for i in range(int(hgt * 2)):
                u = i / (hgt * 2)
                y = oy + base - u * hgt - 2
                x = x0 + 5.5 + 1.2 * math.sin(u * 4 + ph + k) + (0.8 if k == 1 else -0.4)
                col = FIRE[4] if u < 0.3 else FIRE[3] if u < 0.6 else FIRE[2]
                put(t, x, y, col, 1 - u * 0.6)
                if u < 0.5:
                    put(t, x + 1, y, FIRE[2], 0.6 * (1 - u))


# --- the seals -----------------------------------------------------------------------------------------------------

CROWN = ["..........",
         ".x..x..x..",
         ".xx.xx.xx.",
         ".xxxxxxxx.",
         ".xoxxoxxo.",
         ".xxxxxxxx.",
         "..xxxxxx..",
         "..........",
         ]


def seal(t, ox, oy, state, seed):
    """A blob of red wax, 24x24: a raised rim, a crown pressed in; state 0 intact, 1 cracked, 2 broken in two."""
    cx, cy = 11.5, 11.5
    for y in range(24):
        for x in range(24):
            ang = math.atan2(y - cy, x - cx)
            rad = 9.4 + 1.0 * math.sin(ang * 5 + seed) + 0.6 * math.sin(ang * 9 + seed * 2)
            d = math.hypot(x - cx, y - cy)
            drip = (y > cy and abs(x - (cx + 4)) < 1.2 and y < cy + 11.5) or (y > cy and abs(x - (cx - 6)) < 0.9 and y < cy + 10.5)
            if d > rad and not drip:
                continue
            if state == 2:
                # Broken: a jagged split from top-left to bottom-right, the halves pushed apart.
                split = (x - cx) * 0.8 - (y - cy) + 1.6 * math.sin(y * 1.7)
                if abs(split) < 1.3:
                    continue
                if split > 0 and h01("chip", x, y) < 0.04:
                    continue
            # Shading: the rim raised and lit from the top-left, the well inside darker.
            lit = -((x - cx) + (y - cy)) / 14
            v = 3.0 + lit
            if 6.4 < d < rad:
                v += 0.8 if d < rad - 1.2 else -0.4
            elif d <= 6.4:
                v -= 0.6
            if drip and d > rad:
                v = 2.6 - (y - cy - rad) * 0.1
            c = ramp(WAX, v)
            if state == 2:
                c = mix(c, WAX[1], 0.2)
            put(t, ox + x, oy + y, c)
    # The crown pressed in (dark), its edges catching light.
    for j, row in enumerate(CROWN):
        for i, ch in enumerate(row):
            px, py = ox + 7 + i, oy + 7 + j
            if ch == "x" and t.rows[py][px][3]:
                put(t, px, py, WAX[1])
                if j > 0 and CROWN[j - 1][i] != "x" and t.rows[py - 1][px][3]:
                    put(t, px, py - 1, WAX[5], 0.7)
            elif ch == "o" and t.rows[py][px][3]:
                put(t, px, py, WAX[4])
    if state == 1:
        # Cracks running out from the crown.
        for k, ang in enumerate((0.4, 2.2, 3.9, 5.3)):
            x, y = cx, cy
            for s in range(12):
                a = ang + 0.5 * math.sin(s * 1.3 + k)
                x += math.cos(a) * 0.9
                y += math.sin(a) * 0.9
                if t.rows[oy + int(y)][ox + int(x)][3]:
                    put(t, ox + x, oy + y, WAX[0])
                    put(t, ox + x + 1, oy + y, WAX[5], 0.35)


def generate():
    t = Tex(W, H, 35900)
    for y in range(H):
        for x in range(W):
            t.rows[y][x] = (0, 0, 0, 0)
    contract(t)
    signature(t, 0, 40)
    scorch(t, 0, 52)
    burn_frames(t, 0, 64)
    for i in range(3):
        seal(t, i * 24, 88, i, 2.0)
    save(t, "gui", "crowley_bar")


if __name__ == "__main__":
    generate()
