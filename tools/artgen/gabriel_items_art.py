"""Gabriel's items (v0.14): the flat icons (trickster_bait, trickster_candy, candy_wrapper, gabriel_blade), the Trickster's
Remote (a GeckoLib item: a chunky 80s TV remote, upright and centred like weapon_models, with its 16-px inventory icon and a
`click` clip) and the trophy (a vanilla block model facing north: an old wooden CRT television on little legs, rabbit-ear
antenna, his grin on the screen).
"""

import math

import blockmodel as BM
import preview3d
from animkit import AnimFile
from chuck_art import Model, solid
from common import ASSETS, save
from pixelkit import Ramp, Tex, hexc, item_outline, mix, shade

GEO = ASSETS + "/geo/item/"
ANIM = ASSETS + "/animations/item/"
O = hexc("#1E1410")
CANDY = (hexc("#e8283c"), hexc("#fff8ee"), hexc("#ffb21f"), hexc("#3fb0ff"))
GOLD = Ramp("#5a3a0c", "#8a5c14", "#b8851f", "#ddaf38", "#f3d36a", "#fff1b8")
OIL = Ramp("#7a5a14", "#a98222", "#d4ab3c", "#f0cf68", "#fbe7a4", "#fffbe6")
STICKC = Ramp("#a59d8c", "#c6bfae", "#ddd7c8", "#ece7db", "#f6f2ea", "#ffffff")
WRAP = Ramp("#6e0c14", "#981420", "#c21f2c", "#e0384a", "#f06a78", "#fbb0b8")
FOIL = Ramp("#5c6068", "#80858e", "#a5abb3", "#c7ccd2", "#e2e5e9", "#ffffff")
TWIST = Ramp("#7a5410", "#a8761a", "#d29e2c", "#ecc54a", "#f8e08a", "#fff6cf")
GRIP = Ramp("#120c08", "#1f150e", "#2e2016", "#3e2c1f", "#503a2a", "#654a36")


def disc(t, cx, cy, r, fn):
    for y in range(t.h):
        for x in range(t.w):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d <= r:
                t.set(x, y, fn(x + 0.5 - cx, y + 0.5 - cy, d / r))


def thick_line(t, x0, y0, x1, y1, w, fn):
    n = int(max(abs(x1 - x0), abs(y1 - y0)) * 3) + 1
    for i in range(n + 1):
        u = i / n
        x, y = x0 + (x1 - x0) * u, y0 + (y1 - y0) * u
        for yy in range(int(y - w), int(y + w) + 1):
            for xx in range(int(x - w), int(x + w) + 1):
                if math.hypot(xx + 0.5 - x, yy + 0.5 - y) <= w:
                    t.set(xx, yy, fn(u))


def spiral(dx, dy, r):
    a = math.atan2(dy, dx)
    k = int(((a / (2 * math.pi)) * 4 + r * 7.0) % 4)
    c = CANDY[k]
    if r > 0.82:
        c = shade(c, 0.8)
    return c


# --- flat items ---------------------------------------------------------------------------------------------------------

def trickster_bait():
    """A big swirly lollipop in cellophane, glistening with holy oil: the bait for a Trickster."""
    t = Tex(16, 16, 1)
    thick_line(t, 6.2, 9.8, 1.6, 14.6, 0.8, lambda u: STICKC[4 - 2 * u])
    disc(t, 9.5, 6.5, 5.6, spiral)
    # The cellophane: a pale rim round the candy, a gathered twist and a gold ribbon at the stick.
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 9.5, y + 0.5 - 6.5)
            if 5.6 < d <= 6.5:
                t.set(x, y, mix(hexc("#f2f6fa"), OIL[3], 0.25))
    for (x, y) in ((5, 10), (6, 10), (5, 11), (4, 10), (6, 11)):
        t.set(x, y, mix(hexc("#eef3f8"), OIL[2], 0.3))
    for (x, y) in ((4, 11), (5, 12), (3, 11)):
        t.set(x, y, OIL[1] if (x + y) % 2 else OIL[3])
    item_outline(t, O)
    # The holy oil: a golden sheen running over the wrap, a bright glint.
    for (x, y, k) in ((8, 2, 5), (9, 2, 4), (7, 3, 4), (6, 4, 3), (12, 9, 3), (13, 8, 4), (11, 10, 2)):
        t.set(x, y, OIL[k])
    t.set(7, 2, hexc("#ffffff"))
    return t


def trickster_candy():
    """A wrapped candy: a striped red sweet, its twisted gold ends."""
    t = Tex(16, 16, 2)
    for y in range(16):
        for x in range(16):
            u, v = (x + 0.5 - 8) * 0.7071 + (y + 0.5 - 8) * 0.7071, -(x + 0.5 - 8) * 0.7071 + (y + 0.5 - 8) * 0.7071
            if (u / 3.6) ** 2 + (v / 2.6) ** 2 <= 1:
                stripe = int((u + v * 0.5 + 10) // 1.6) % 2
                c = WRAP[3.4 - 1.2 * (v / 2.6)] if stripe else hexc("#fff4ec")
                if v < -1.4 and abs(u) < 2:
                    c = mix(c, hexc("#ffffff"), 0.5)
                t.set(x, y, c)
            elif 3.4 < abs(u) < 6.6 and abs(v) < 0.6 + (abs(u) - 3.4) * 0.55:
                k = 3.6 - 0.9 * abs(v) + (0.6 if int(abs(u) * 2 + v) % 2 else 0)
                t.set(x, y, TWIST[k])
    item_outline(t, O)
    return t


def candy_wrapper():
    """A crumpled, empty wrapper: red foil folded on itself, its silver inside showing, the twisted ends."""
    t = Tex(16, 16, 3)
    pts = [(4, 6), (7, 4), (11, 5), (12, 8), (10, 11), (6, 12), (3, 9)]

    def inside(x, y):
        n, c = len(pts), False
        for i in range(n):
            (x0, y0), (x1, y1) = pts[i], pts[(i + 1) % n]
            if (y0 > y) != (y1 > y) and x < (x1 - x0) * (y - y0) / (y1 - y0) + x0:
                c = not c
        return c
    for y in range(16):
        for x in range(16):
            if inside(x + 0.5, y + 0.5):
                fold = math.sin(x * 1.3 + y * 0.7) + math.sin(x * 0.5 - y * 1.6)
                if fold > 1.1:
                    c = FOIL[4 + (x % 2)]
                elif fold < -1.0:
                    c = WRAP[1]
                else:
                    c = WRAP[3 + 0.8 * math.sin(x * 2.1 + y)]
                t.set(x, y, c)
    for (x, y) in ((2, 7), (2, 8), (1, 6), (1, 9), (13, 7), (14, 6), (14, 8), (13, 9)):
        t.set(x, y, TWIST[3 + (x + y) % 2])
    item_outline(t, O)
    for (x, y) in ((8, 6), (9, 6), (6, 9)):
        t.set(x, y, FOIL[5])
    return t


def gabriel_blade():
    """Gabriel's archangel blade: a long triple-edged blade of gold with a bright edge and an amber heart, a dark wrapped
    grip, a gold guard and pommel (the Archangel Blade is white-gold; this one is all honey and sun)."""
    t = Tex(16, 16, 4)
    thick_line(t, 5.0, 11.0, 14.4, 1.6, 1.25, lambda u: GOLD[3.2 + 1.2 * u])
    thick_line(t, 5.6, 10.0, 14.2, 1.8, 0.45, lambda u: hexc("#fff6d0"))
    thick_line(t, 5.0, 11.6, 13.6, 3.0, 0.4, lambda u: GOLD[2])
    thick_line(t, 6.2, 9.8, 11.5, 4.5, 0.35, lambda u: hexc("#e8892a"))
    thick_line(t, 2.2, 13.8, 5.2, 10.8, 0.9, lambda u: GRIP[2 + (int(u * 8) % 2) * 2])
    thick_line(t, 3.4, 9.4, 6.8, 12.8, 0.7, lambda u: GOLD[3 + (u > 0.5)])
    t.set(1, 14, GOLD[4])
    t.set(2, 14, GOLD[3])
    t.set(1, 15, GOLD[2])
    item_outline(t, O)
    t.set(14, 1, hexc("#ffffff"))
    return t


# --- the remote (GeckoLib item) -------------------------------------------------------------------------------------------

PLASTIC = Ramp("#0e0e10", "#18181b", "#232327", "#2f2f35", "#3c3c44", "#4c4c56")
PANEL = Ramp("#5c5f66", "#7b7f87", "#9da1a9", "#bcc0c6", "#d6d9dd", "#eef0f2")
WOODG = Ramp("#2a170a", "#422511", "#5a3418", "#734420", "#8c562a", "#a46a36")
BUTTONS = (hexc("#e3262f"), hexc("#2fbf4a"), hexc("#f2c418"), hexc("#2f7fe8"))


def remote_rig():
    """Upright (+Y up), centred: GeoItemRenderer already moves it to the block's centre. The buttons face north."""
    m = Model("trickster_remote", 128, 128)
    root = m.bone("remote", (0, 0, 0))

    def body(f, x, y, w, h):
        if f == "north":
            if y < 2 or x in (0, w - 1):
                return PLASTIC[3.6]
            return PANEL[2.6 + 0.6 * ((y // 3) % 2 == 0) * (x % 2)]
        if f in ("east", "west"):
            return WOODG[2.6 + 0.7 * math.sin(y * 0.9 + x)]      # faux-woodgrain sides, very 80s
        if f == "up":
            return PLASTIC[4]
        return PLASTIC[2.4 + 0.4 * ((x + y) % 3 == 0)]
    m.cube(root, (-2.0, -5.0, -0.8), (4.0, 10.0, 1.6), body, density=4, tag="body")
    m.cube(root, (-1.75, -5.4, -0.6), (3.5, 0.5, 1.2), solid(PLASTIC[1]), density=4, tag="foot")
    # The IR eye at the top, the big red power button, a grid of coloured buttons, a white number pad, a rocker.
    m.cube(root, (-1.0, 4.4, -1.05), (2.0, 0.6, 0.3), lambda f, x, y, w, h: hexc("#7a0d16") if f == "north" else hexc("#3a060a"),
           density=4, tag="ir_eye")
    pw = m.bone("power", (0.9, 3.5, -0.9), "remote")
    m.cube(pw, (0.4, 3.0, -1.3), (1.0, 1.0, 0.5), lambda f, x, y, w, h: shade(BUTTONS[0], 1.25 if f == "north" and y == 0 else 1.0),
           density=4, tag="power")
    for i, col in enumerate(BUTTONS):
        m.cube(root, (-1.6 + i * 0.85, 1.8, -1.1), (0.6, 0.6, 0.35), lambda f, x, y, w, h, col=col: shade(col, 1.2 if y == 0 else 0.95),
               density=4, tag="color_button")
    for r in range(3):
        for c in range(3):
            m.cube(root, (-1.45 + c * 1.05, -0.4 - r * 0.95, -1.05), (0.75, 0.6, 0.3),
                   lambda f, x, y, w, h: PANEL[5] if y == 0 else PANEL[4], density=4, tag="num_button")
    m.cube(root, (-0.8, -3.8, -1.15), (1.6, 0.9, 0.4), lambda f, x, y, w, h: PLASTIC[4] if y == 0 else PLASTIC[3], density=4, tag="rocker")
    m.cube(root, (-1.4, -4.6, -1.0), (2.8, 0.4, 0.2), solid(hexc("#d9b343")), density=4, tag="logo_strip")
    return m


def remote_anims():
    f = AnimFile()
    a = f.new("animation.trickster_remote.click", 0.3)
    a.pos("power", (0, [0, 0, 0]), (0.06, [0, 0, 0.3]), (0.3, [0, 0, 0]))
    a.scale("remote", (0, [1, 1, 1]), (0.06, [1.04, 1.04, 1.04]), (0.3, [1, 1, 1]))
    return f


# --- the trophy: an old wooden television --------------------------------------------------------------------------------

def _tex(fn, seed=0):
    t = Tex(16, 16, seed)
    for y in range(16):
        for x in range(16):
            t.set(x, y, fn(x, y))
    return t


SCREEN = [  # his grin on the set: x = 0 is the viewer's left
    "................",
    "....hhhhhhhh....",
    "...hhhHhhhhhh...",
    "...hsssssssssh..",
    "...sbbsssssBBs..",
    "...swEsssssEws..",
    "...sssssnssssS..",
    "...sssssnnsssS..",
    "...ssmsssssmss..",
    "...ssmTTTTTmss..",
    "....ssmmmmmss...",
    ".....ssssssss...",
    "................",
    "................",
    "................",
    "................",
]


def trophy():
    wood = _tex(lambda x, y: WOODG[2.6 + 0.9 * math.sin(x * 0.35 + math.sin(y * 0.8) * 1.6) + (0.4 if (x * 7 + y) % 11 == 0 else 0)])
    dark = _tex(lambda x, y: WOODG[1.2 + 0.5 * math.sin(x * 0.4 + y)])

    face = {"h": hexc("#7a5636"), "H": hexc("#a07a50"), "s": hexc("#e2b591"), "S": hexc("#c8977b"), "b": hexc("#5a3a1c"),
            "B": hexc("#5a3a1c"), "w": hexc("#ffffff"), "E": hexc("#4a3020"), "n": hexc("#c08a6c"), "m": hexc("#7a2f2a"),
            "T": hexc("#ffffff")}

    def screen(x, y):
        # The CRT: a blue-green glow, brighter in the middle, scanlines, and his grin on it.
        cx, cy = x - 7.5, y - 7.5
        r = math.hypot(cx, cy) / 10.6
        bg = mix(hexc("#9fe6ff"), hexc("#1e4a66"), min(1.0, r * 1.2))
        ch = SCREEN[y][x]
        c = face.get(ch, bg)
        if ch != ".":
            c = mix(c, hexc("#bfefff"), 0.18)
        if y % 2:
            c = shade(c, 0.86)
        if r > 0.92:
            c = mix(c, hexc("#0c1820"), 0.6)
        return c
    screen_t = _tex(screen)

    def panel(x, y):
        # Two dials over a speaker grille.
        for (cx, cy) in ((8, 3.5), (8, 8.5)):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d < 2.6:
                if abs(math.atan2(y + 0.5 - cy, x + 0.5 - cx) + 1.2) < 0.25 and d > 0.8:
                    return hexc("#f2e6c8")
                return PANEL[3.6 - d]
        if y >= 12 and x % 2 == 0:
            return WOODG[0]
        return WOODG[1.8]
    panel_t = _tex(panel)
    metal = _tex(lambda x, y: PANEL[3.2 + 1.2 * math.sin(x * 0.9 + y * 0.3)])
    for name, t in (("wood", wood), ("dark", dark), ("screen", screen_t), ("panel", panel_t), ("metal", metal)):
        save(t, "block", f"gabriel_trophy_{name}")

    W, D, S, P, M = "wood", "dark", "screen", "panel", "metal"
    full = [0, 0, 16, 16]
    e = []
    # Little splayed legs.
    for (x, z) in ((3, 4), (12, 4), (3, 11), (12, 11)):
        e.append(BM.cube("leg", (x, 0, z), (x + 1, 3, z + 1), {"*": D}))
    e.append(BM.cube("apron", (2.5, 2.6, 3.5), (13.5, 3.4, 12.5), {"*": D}))
    # The cabinet, its front frame and the inset screen and knob panel.
    e.append(BM.cube("cabinet", (2, 3.4, 3), (14, 13.4, 13), {"*": W}))
    e.append(BM.cube("screen", (3, 4.4, 2.6), (11, 12.4, 3), {"north": S, "*": D}, uv={"north": full}, shade=False))
    e.append(BM.cube("bezel_top", (2.6, 12.2, 2.4), (11.4, 12.8, 3), {"*": D}))
    e.append(BM.cube("bezel_bottom", (2.6, 4.0, 2.4), (11.4, 4.6, 3), {"*": D}))
    e.append(BM.cube("knobs", (11.4, 4.4, 2.7), (13.4, 12.4, 3), {"north": P, "*": D}, uv={"north": [5, 0, 11, 16]}))
    for y in (9.6, 7.0):
        e.append(BM.cube("knob", (11.9, y, 2.2), (12.9, y + 1, 2.7), {"*": M}))
    # Rabbit-ear antenna.
    e.append(BM.cube("antenna_base", (6.5, 13.4, 6.5), (9.5, 14.4, 9.5), {"*": D}))
    e.append(BM.cube("antenna_r", (7.6, 14.0, 7.6), (8.2, 21.0, 8.2), {"*": M}, rot=("z", 22.5, (8, 14, 8))))
    e.append(BM.cube("antenna_l", (7.8, 14.0, 7.8), (8.4, 20.0, 8.4), {"*": M}, rot=("z", -22.5, (8, 14, 8))))
    m = BM.model({W: "gabriel_trophy_wood", D: "gabriel_trophy_dark", S: "gabriel_trophy_screen", P: "gabriel_trophy_panel",
                  M: "gabriel_trophy_metal"}, e, "gabriel_trophy_wood")
    BM.write("gabriel_trophy", m)


ICON_ANGLE = -30


def generate():
    for name, fn in (("trickster_bait", trickster_bait), ("trickster_candy", trickster_candy), ("candy_wrapper", candy_wrapper),
                     ("gabriel_blade", gabriel_blade)):
        save(fn(), "item", name)
    m = remote_rig()
    t, _ = m.build(gutter=1, seed=7800)
    m.rig.write(GEO + "trickster_remote.geo.json")
    save(t, "item", "trickster_remote")
    save(preview3d.icon(m.rig, t, size=16, angle_deg=ICON_ANGLE, view="front"), "item", "trickster_remote_icon")
    remote_anims().write(ANIM + "trickster_remote.animation.json")
    trophy()


if __name__ == "__main__":
    generate()
