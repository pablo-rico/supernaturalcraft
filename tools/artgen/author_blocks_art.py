"""The Author's blocks (v0.10): blank paper, ink, burning ink, and his typewriter.

  page_block   -- clean paper with faint ruled lines; tiles seamlessly (the lines repeat every 4 px).
  ink_block    -- glossy wet ink; a slow 4-frame animated sheen (ink_block.png.mcmeta).
  burning_ink  -- ink with orange embers breathing under the surface; 8 frames (the block renders emissive).
  typewriter   -- a real block model: an old black 1940s typewriter. Keyboard toward the north (the typist), four
                  staggered rows of round keys and a space bar, the typebar basket in the front of the body, the
                  carriage with its rubber roller, knobs, return lever and paper bail, and a half-typed sheet leaning
                  back out of the roller. Textures typewriter_{body,front,chrome,rubber,key_0..3,paper}.
"""

import json
import math
import os

from blockmodel import cube, model, write
from common import TEXTURES, save
from pixelkit import Ramp, Tex, fbm, hexc, mix

PAPER = Ramp("#b9b09a", "#d4ccb6", "#e6dfcc", "#f1ebdb", "#f8f4e8", "#fffdf6")
RULE = hexc("#9fb3cf")
INK = Ramp("#020205", "#07060d", "#0e0c19", "#181528", "#262140", "#3a3466")
GLOSS = hexc("#8c88c8")
EMBER = Ramp("#2a0a02", "#5c1603", "#a12e06", "#e2570c", "#ff8f2a", "#ffd27a")
ENAMEL = Ramp("#050506", "#0c0c0f", "#141418", "#1e1e24", "#2c2c35", "#45454f")
CHROME = Ramp("#3a3d44", "#5d626b", "#868c96", "#adb3bc", "#d2d7de", "#f4f6f8")
RUBBER = Ramp("#0a0a0a", "#141414", "#1c1c1c", "#262626", "#303030", "#3c3c3c")
CREAM = hexc("#ece4cf")
GOLD = Ramp("#5a3c0c", "#8a6216", "#b88a24", "#dcb444", "#f2d77a", "#fff0b8")
TYPE = hexc("#1b1a1f")


def _mcmeta(name, frametime, interpolate=True):
    path = os.path.join(TEXTURES, "block", name + ".png.mcmeta")
    with open(path, "w") as f:
        json.dump({"animation": {"frametime": frametime, "interpolate": interpolate}}, f, indent=1)
        f.write("\n")


# --- the arena blocks -----------------------------------------------------------------------

def page_block():
    t = Tex(16, 16, 11)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 11, 16, 16, 3, 4.0)
            fibre = fbm(x * 3, y, 12, 48, 16, 1, 8.0)
            v = 3.6 + (n - 0.5) * 0.9 + (fibre - 0.5) * 0.5
            t.set(x, y, PAPER[int(round(v))])
    for y in (3, 7, 11, 15):
        for x in range(16):
            t.set(x, y, mix(t.get(x, y), RULE, 0.32))
    return t


def ink_block():
    """4 frames stacked: a pool of ink, its wet highlights sliding slowly over the ripples."""
    frames = 4
    t = Tex(16, 16 * frames, 12)
    for f in range(frames):
        for y in range(16):
            for x in range(16):
                n = fbm(x, y, 12, 16, 16, 3, 4.0)
                c = INK[int(round(1.5 + (n - 0.5) * 2.4))]
                # Specular: where the surface rises toward the light (top-left), it shines.
                m = fbm(x + f, y + f, 13, 16, 16, 3, 4.0)
                m2 = fbm(x + 1 + f, y + 1 + f, 13, 16, 16, 3, 4.0)
                spec = (m - m2) * 6.0
                if spec > 0.55:
                    c = mix(c, GLOSS, min(0.3, (spec - 0.55) * 0.7))
                if spec > 1.05:
                    c = mix(c, hexc("#e4e2fa"), 0.3)
                t.set(x, y + 16 * f, c)
    return t


def burning_ink():
    """8 frames: a crust of ink split by glowing fissures that breathe, with the odd spark."""
    frames = 8
    t = Tex(16, 16 * frames, 13)
    for f in range(frames):
        phase = f / frames * 2 * math.pi
        for y in range(16):
            for x in range(16):
                wx = (fbm(x, y, 16, 16, 16, 2, 2.0) - 0.5) * 6.0
                wy = (fbm(x, y, 17, 16, 16, 2, 2.0) - 0.5) * 6.0
                n = fbm(x + wx, y + wy, 13, 16, 16, 3, 4.0)
                crust = fbm(x, y, 15, 16, 16, 2, 4.0)
                c = INK[1 + int(crust * 2.4)]
                vein = abs(n - 0.5)
                pulse = 0.6 + 0.4 * math.sin(phase + fbm(x, y, 14, 16, 16, 1, 2.0) * 12.0)
                if vein < 0.06:
                    k = (1 - vein / 0.06) * pulse
                    c = mix(EMBER[2], EMBER[5], k) if k > 0.35 else mix(c, EMBER[3], k * 2)
                elif vein < 0.095:
                    c = mix(c, EMBER[1], 0.6 * pulse)
                t.set(x, y + 16 * f, c)
        # A couple of sparks per frame.
        for k in range(2):
            sx, sy = (5 + f * 7 + k * 9) % 16, (3 + f * 5 + k * 11) % 16
            t.set(sx, sy + 16 * f, EMBER[5])
    return t


# --- the typewriter -------------------------------------------------------------------------

def body_tex():
    """Black enamel with a soft vertical sheen and the odd hairline scratch."""
    t = Tex(16, 16, 21)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 21, 16, 16, 2, 4.0)
            sheen = 0.9 * math.exp(-((x - 5) / 2.2) ** 2)
            t.set(x, y, ENAMEL[int(round(1.6 + (n - 0.5) * 1.2 + sheen))])
    for (x, y) in ((11, 3), (12, 4), (3, 11), (4, 11)):
        t.set(x, y, ENAMEL[4])
    return t


def front_tex():
    """The front of the body (top 15 rows of a 32x32): the typebar basket fanned out in its dark recess, the brand
    plate in gold, a pinstripe."""
    t = Tex(32, 32, 22)
    for y in range(32):
        for x in range(32):
            n = fbm(x, y, 22, 32, 32, 2, 4.0)
            t.set(x, y, ENAMEL[int(round(1.8 + (n - 0.5) * 1.2))])
    cx, cy = 15.5, 12.0
    for y in range(15):
        for x in range(32):
            d = math.hypot((x + 0.5 - cx) / 1.6, y + 0.5 - cy)
            if 3.0 <= d <= 9.6 and y < cy:
                t.set(x, y, ENAMEL[0])
                ang = math.atan2(cy - (y + 0.5), (x + 0.5 - cx) / 1.6)
                # The typebars: thin chrome spokes, one every few degrees.
                if (int(math.degrees(ang) / 5.6)) % 2 == 0 and d > 3.6:
                    t.set(x, y, CHROME[2] if d < 7 else CHROME[3])
            elif 9.6 < d <= 10.6 and y < cy:
                t.set(x, y, ENAMEL[4])
    # The type guide where the bars strike, at the centre of the fan.
    t.rect(14, 10, 17, 11, CHROME[4])
    # Pinstripe and the brand plate.
    t.hline(1, 30, 13, GOLD[2])
    t.rect(11, 1, 20, 2, GOLD[3])
    t.hline(11, 20, 1, GOLD[5])
    for x in (12, 14, 15, 17, 19):
        t.set(x, 2, GOLD[1])
    return t


def chrome_tex():
    t = Tex(16, 16, 23)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y * 4, 23, 16, 64, 2, 6.0)
            t.set(x, y, CHROME[int(round(2.6 + (n - 0.5) * 2.0 + (1.0 if y in (4, 5) else 0)))])
    return t


def rubber_tex():
    t = Tex(16, 16, 24)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 24, 16, 16, 2, 8.0)
            v = 2.0 + (n - 0.5) * 1.0 + (1.2 if y in (5, 6) else 0) - (0.8 if y in (13, 14) else 0)
            t.set(x, y, RUBBER[int(round(v))])
    return t


KEY_GLYPHS = [
    ["#..#", "#..#", "####", "#..#", "#..#"],   # H
    ["####", "#...", "###.", "#...", "####"],   # E
    ["####", ".##.", ".##.", ".##.", ".##."],   # T
    ["###.", "#..#", "#..#", "#..#", "###."],   # D
]


def key_tex(i):
    """A round key cap seen from above: chrome ring, cream face, a typed capital."""
    t = Tex(16, 16, 30 + i)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if d <= 5.4:
                t.set(x, y, mix(CREAM, PAPER[1], max(0.0, (d - 3.5) / 3)))
            elif d <= 7.2:
                t.set(x, y, CHROME[4] if (x + y) < 14 else CHROME[2])
            else:
                t.set(x, y, ENAMEL[1])
    for gy, row in enumerate(KEY_GLYPHS[i]):
        for gx, c in enumerate(row):
            if c == "#":
                t.set(6 + gx, 5 + gy + 1, TYPE)
    return t


def paper_tex():
    """The half-typed sheet: typewritten lines (uneven ink, a word struck out), then the blank rest of the page."""
    import random
    rng = random.Random(25)
    t = Tex(32, 32, 25)
    for y in range(32):
        for x in range(32):
            n = fbm(x, y, 25, 32, 32, 2, 4.0)
            t.set(x, y, PAPER[int(round(4.0 + (n - 0.5) * 0.9))])
    lines = [(3, 27), (3, 25), (3, 28), (3, 22), (3, 26), (3, 14)]
    for i, (x0, x1) in enumerate(lines):
        y = 4 + i * 3
        x = x0
        while x <= x1:
            word = rng.randint(2, 6)
            for k in range(word):
                if x + k > x1:
                    break
                shade = rng.random()
                t.set(x + k, y, mix(TYPE, PAPER[2], 0.15 + shade * 0.35))
                if rng.random() < 0.45:
                    t.set(x + k, y - 1, mix(TYPE, PAPER[2], 0.45 + shade * 0.3))
            x += word + 1
    # A struck-out word on the fourth line, and the cursor where the Author stopped.
    t.hline(9, 14, 12, mix(TYPE, PAPER[2], 0.1))
    t.set(15, 19, TYPE)
    t.set(15, 18, TYPE)
    return t


def typewriter_model():
    B, F, C, R, P = "body", "front", "chrome", "rubber", "paper"
    els = [
        cube("chassis", (1.5, 0, 3), (14.5, 1.5, 13.5), {"*": B}),
        cube("housing", (2.5, 1.5, 9), (13.5, 6.5, 13.5), {"*": B, "north": F}, uv={"north": [0, 0, 16, 7.27]}),
        cube("top_cover", (3, 6.5, 9.4), (13, 7.1, 12.6), {"*": B}),
        cube("type_guide", (7.4, 6.2, 8.6), (8.6, 7.4, 9.1), {"*": C}),
        cube("spool_l", (3.3, 7.1, 9.5), (5.3, 7.5, 10.6), {"*": C}),
        cube("spool_r", (10.7, 7.1, 9.5), (12.7, 7.5, 10.6), {"*": C}),
        # The carriage: rail, rubber roller (platen), its knobs and the return lever.
        cube("carriage_rail", (1, 7.1, 11), (15, 7.7, 13.6), {"*": C}),
        cube("roller", (1.6, 7.7, 10.9), (14.4, 9.7, 12.9), {"*": R}),
        cube("knob_l", (0.1, 7.9, 11.1), (1.6, 9.5, 12.7), {"*": B, "west": C}),
        cube("knob_r", (14.4, 7.9, 11.1), (15.9, 9.5, 12.7), {"*": B, "east": C}),
        cube("lever_arm", (0.3, 9.3, 8.4), (0.9, 9.8, 11.3), {"*": C}),
        cube("lever_tip", (0.1, 9.1, 7.6), (1.1, 10.1, 8.6), {"*": B}),
        cube("bail", (2.4, 9.5, 10.5), (13.6, 9.9, 10.8), {"*": C}),
        cube("bail_roll_l", (4.4, 9.3, 10.3), (5.4, 10.1, 10.9), {"*": R}),
        cube("bail_roll_r", (10.6, 9.3, 10.3), (11.6, 10.1, 10.9), {"*": R}),
        # The sheet, rising out of the roller and leaning back.
        cube("paper", (4, 9.0, 12.0), (12, 16, 12.0), {"north": P, "south": P},
             uv={"north": [0, 0, 16, 14], "south": [16, 0, 0, 14]}, rot=("x", 22.5, (8, 9.0, 12.0))),
        cube("space_bar", (4.5, 1.9, 2.2), (11.5, 2.5, 3.1), {"*": C}),
    ]
    # The keyboard deck, stepping up toward the body.
    tops = (2.2, 2.8, 3.4, 4.0)
    zs = ((3.2, 4.6), (4.6, 6.0), (6.0, 7.4), (7.4, 9.0))
    for r, (top, (z0, z1)) in enumerate(zip(tops, zs)):
        els.append(cube(f"deck_{r}", (2, 1.5, z0), (14, top, z1), {"*": B, "down": None}))
    # Four staggered rows of round keys on little stems.
    for r, (top, (z0, z1)) in enumerate(zip(tops, zs)):
        n = 10 if r < 3 else 9
        x0 = 2.6 + (0.35 * r if r < 3 else 0.85)
        for i in range(n):
            x = x0 + i * 1.1
            key = f"key_{(r * 3 + i) % 4}"
            els.append(cube(f"key_{r}_{i}", (x, top, z0 + 0.25), (x + 0.9, top + 0.7, z0 + 1.15),
                            {"*": C, "up": key, "down": None}, uv={"up": [0, 0, 16, 16]}))
    return model({B: "typewriter_body", F: "typewriter_front", C: "typewriter_chrome", R: "typewriter_rubber",
                  P: "typewriter_paper", **{f"key_{i}": f"typewriter_key_{i}" for i in range(4)}},
                 els, "typewriter_body")


def generate():
    save(page_block(), "block", "page_block")
    save(ink_block(), "block", "ink_block")
    _mcmeta("ink_block", 10)
    save(burning_ink(), "block", "burning_ink")
    _mcmeta("burning_ink", 3)
    save(body_tex(), "block", "typewriter_body")
    save(front_tex(), "block", "typewriter_front")
    save(chrome_tex(), "block", "typewriter_chrome")
    save(rubber_tex(), "block", "typewriter_rubber")
    for i in range(4):
        save(key_tex(i), "block", f"typewriter_key_{i}")
    save(paper_tex(), "block", "typewriter_paper")
    write("typewriter", typewriter_model())
