"""The General's armour (v0.12): GeckoLib armour (GeoArmorRenderer) in Michael's white and gold.

Bones are GeckoLib's: armorHead, armorBody, armorRightArm, armorLeftArm, armorRightLeg, armorLeftLeg,
armorRightBoot, armorLeftBoot, at the vanilla player's pivots (the renderer offsets them from those). One extra
child, `visor` (under armorHead), carries the visor slit of light that breathes in animation.general_armor.idle.

Painted to the archangel's standard (michael_art.Plate: gradient, bevel, gold filigree, Enochian script, rivets):
  helm: a close helm with a prow, a brow of gold, a visor slit of light and a crest of feathers swept back;
  chestplate: breastplate with pectorals and a gold ridge, a backplate, a short gold-trimmed cape of light;
    pauldrons of a dome and two lames with a fan of metal feathers;
  leggings: cuisses with knee cops and a short fauld; boots: greaves with a ridge and pointed sabatons.
Also the four flat inventory icons (textures/item/general_helmet|chestplate|leggings|boots.png).
"""

import math

from animkit import AnimFile
from chuck_art import Model, solid
from common import ASSETS, save
from michael_art import E_IO, GOLD, LIGHT, WHITE_METAL, Gilt, MetalFeather, Plate, tone
from pixelkit import Tex, hexc, item_outline, mix, shade

GEO = ASSETS + "/geo/item/armor/"
ANIM = ASSETS + "/animations/item/armor/"
FULL = lambda face, x, y, w, h, c: c
VISOR = hexc("#e9f6ff")
SLIT = hexc("#141a28")


def put(m, bone, origin, size, mat, d=2, **kw):
    return m.cube(bone, origin, size, mat.mat, mat.glow, density=d, **kw)


def rig():
    m = Model("general_armor", 256, 256)
    head = m.bone("armorHead", (0, 24, 0))
    body = m.bone("armorBody", (0, 24, 0))
    ra = m.bone("armorRightArm", (-5, 22, 0))
    la = m.bone("armorLeftArm", (5, 22, 0))
    rl = m.bone("armorRightLeg", (-1.9, 12, 0))
    ll = m.bone("armorLeftLeg", (1.9, 12, 0))
    rb = m.bone("armorRightBoot", (-1.9, 12, 0))
    lb = m.bone("armorLeftBoot", (1.9, 12, 0))

    # --- helm ---
    helm = Plate(6602, 2, engrave=False, rivets=False)

    def helm_m(face, x, y, w, h):
        if face == "north" and y in (8, 9) and 2 <= x < w - 2:
            return SLIT
        return helm.mat(face, x, y, w, h)
    m.cube(head, (-4, 24, -4), (8, 8, 8), helm_m, lambda f, x, y, w, h, c: LIGHT[1] if c == SLIT else helm.glow(f, x, y, w, h, c),
           inflate=1.0, density=2, tag="helm")
    put(m, head, (-4.5, 28.4, -5.6), (9, 1.0, 1.0), Gilt(6620, 2), tag="brow")
    put(m, head, (-0.5, 23.5, -5.6), (1, 4.2, 1), Gilt(6621, 2), tag="face_ridge", rotation=(0, 45, 0), pivot=(0, 25.5, -5.1))
    visor = m.bone("visor", (0, 27.2, -5.1), head.name)
    m.cube(visor, (-3.5, 26.95, -5.35), (7, 0.6, 0.25), solid(VISOR), FULL, density=4, tag="visor")
    for i in range(5):
        z = -3 + i * 1.7
        mf = MetalFeather("crest", -1, 6630 + i, WHITE_METAL)
        L = 3.5 + 2.5 * math.sin(math.pi * (i + 1) / 6)
        m.cube(head, (-0.6, 33, z - 0.25), (1.2, L, 0.5), mf.mat, mf.glow, density=3, tag="crest",
               rotation=(-55 - i * 7, 0, 0), pivot=(0, 33, z))

    # --- chestplate ---
    put(m, body, (-4, 12, -2), (8, 12, 4), Plate(6603, 2, rows=1), inflate=1.0, tag="cuirass")
    for s in (-1, 1):
        put(m, body, (s * 2.1 - 2.1, 17.5, -3.6), (4.2, 5.0, 0.8), Plate(6604 + s, 2, engrave=False, rivets=False),
            tag="pec", rotation=(-6, -14 * s, 0), pivot=(s * 2.1, 20, -3.2))
    put(m, body, (-0.5, 12.5, -3.9), (1, 10, 1), Gilt(6606, 2), tag="ridge", rotation=(0, 45, 0), pivot=(0, 17, -3.4))
    m.cube(body, (-4.5, 8.5, 3.3), (9, 14.5, 0), cape_mat, lambda f, x, y, w, h, c: shade(c, 0.75), density=2,
           faces=("north", "south"), tag="cape", rotation=(8, 0, 0), pivot=(0, 23, 3.3))
    for b, s in ((ra, -1), (la, 1)):
        x0 = -8 if s < 0 else 4
        cx = x0 + 2
        put(m, b, (x0, 12, -2), (4, 12, 4), Plate(6608 + s, 2, engrave=False), inflate=0.9, tag="arm")
        put(m, b, (cx - 3.2, 20.0, -3.2), (6.4, 3.8, 6.4), Plate(6610 + s, 2, engrave=False), tag="pauldron",
            rotation=(0, 0, -14 * s), pivot=(cx, 23.5, 0))
        for k in (1, 2):
            put(m, b, (cx - 3.0 + s * 0.6 * k, 20.0 - 2.0 * k, -3.4), (6.0, 2.2, 6.8), Plate(6612 + k, 2, engrave=False,
                rivets=False), tag="lame", rotation=(0, 0, -(18 + 6 * k) * s), pivot=(cx + s * 0.6 * k, 22.2 - 2.0 * k, 0))
        for i in range(4):  # a fan of metal feathers along the pauldron's crest
            px = cx + s * (0.5 + i * 0.9)
            mf = MetalFeather("crest", s, 6640 + i, WHITE_METAL)
            m.cube(b, (px - 0.6, 23.6, -1.2 + i * 0.7), (1.2, 3.2 + 0.5 * (i % 2), 0.5), mf.mat, mf.glow, density=3,
                   tag="pauldron_feather", rotation=(-25, 0, -s * (28 + i * 14)), pivot=(px, 23.6, i * 0.7 - 1))

    # --- leggings ---
    for b, s in ((rl, -1), (ll, 1)):
        x0 = -3.9 if s < 0 else -0.1
        put(m, b, (x0, 3, -2), (4, 9, 4), Plate(6650 + s, 2, engrave=False), inflate=0.6, tag="cuisse")
        put(m, b, (x0 + 0.5, 4.5, -3.1), (3, 2.5, 1.2), Plate(6652, 2, engrave=False, rivets=False), tag="knee_cop")
        put(m, b, (x0, 9, -2), (4, 3, 4), Gilt(6653, 2, engrave=True), inflate=0.75, tag="fauld")
    # --- boots ---
    for b, s in ((rb, -1), (lb, 1)):
        x0 = -3.9 if s < 0 else -0.1
        put(m, b, (x0, 0, -2), (4, 5, 4), Plate(6660 + s, 2, engrave=False), inflate=1.0, tag="greave")
        put(m, b, (x0 + 1.5, 0.5, -3.2), (1, 4.5, 1), Gilt(6662, 2), tag="shin_ridge", rotation=(0, 45, 0),
            pivot=(x0 + 2, 2.5, -2.7))
        put(m, b, (x0 + 0.6, 0, -4.4), (2.8, 1.4, 1.6), Gilt(6663, 2), tag="sabaton_toe")
    return m


def cape_mat(face, x, y, w, h):
    t = y / max(1, h - 1)
    if t > 0.8 + 0.15 * math.sin(x * 1.3):
        return None
    if x in (0, w - 1) or (t > 0.72 + 0.15 * math.sin(x * 1.3)):
        return GOLD[4]
    col = mix(hexc("#eef6ff"), LIGHT[1], t)
    return (col[0], col[1], col[2], 235)


def anims():
    f = AnimFile()
    idle = f.new("animation.general_armor.idle", 3.0, loop=True)
    idle.scale("visor", (0, [1, 1, 1]), (1.5, [1.15, 1.6, 1.0], E_IO), (3.0, [1, 1, 1], E_IO))
    return f


# --- inventory icons: white plate, gold trim, a 1-px dark outline ---------------------------------------------

ICONS = {
    "helmet": [
        "................",
        "......gGGg......",
        "....wwwWWwww....",
        "...wwWWWWWWww...",
        "..wwWWWWWWWWww..",
        "..wWWWWWWWWWWw..",
        "..gggggggggggg..",
        "..wWlllllllllw..",
        "..wwwwwGGwwwww..",
        "..wwwwwGGwwwww..",
        "..wwwwwGGwwwww..",
        "...wwwwGGwwww...",
        "...ss..ss..ss...",
        "................",
        "................",
        "................",
    ],
    "chestplate": [
        "................",
        ".gg..........gg.",
        "gWWg........gWWg",
        "gWwwgg....ggwwWg",
        ".gwwWWwGGwWWwwg.",
        "..gwWWwGGwWWwg..",
        "...wWWwGGwWWw...",
        "...wwwwGGwwww...",
        "...wwlwGGwlww...",
        "...wwwwGGwwww...",
        "....wwwGGwww....",
        "....ggggggggg...",
        "....wwwwwwwww...",
        "....sssssssss...",
        "................",
        "................",
    ],
    "leggings": [
        "................",
        "...gggggggggg...",
        "...GGGGllGGGG...",
        "...wwwwwwwwww...",
        "...wWWww.wWWw...",
        "...wWWw..wWWw...",
        "...gggg..gggg...",
        "...wwww..wwww...",
        "...wWWw..wWWw...",
        "...wwww..wwww...",
        "...wwww..wwww...",
        "...wwww..wwww...",
        "...ssss..ssss...",
        "................",
        "................",
        "................",
    ],
    "boots": [
        "................",
        "................",
        "................",
        "................",
        "..gggg....gggg..",
        "..wWWw....wWWw..",
        "..wWGw....wGWw..",
        "..wWGw....wGWw..",
        "..wwGw....wGww..",
        "..wwww....wwww..",
        ".wwwww....wwwww.",
        "gwwwww....wwwwwg",
        "Gggggs....sggggG",
        "................",
        "................",
        "................",
    ],
}
ICON_PAL = {"w": WHITE_METAL[3], "W": WHITE_METAL[5], "s": WHITE_METAL[1], "g": GOLD[3], "G": GOLD[5], "l": LIGHT[3]}


def icon(kind):
    t = Tex(16, 16, 0)
    for y, row in enumerate(ICONS[kind]):
        for x, ch in enumerate(row):
            if ch in ICON_PAL:
                t.set(x, y, ICON_PAL[ch])
    item_outline(t, (40, 34, 20, 255))
    return t


def generate():
    m = rig()
    t, gl = m.build(gutter=1, seed=6600)
    m.rig.write(GEO + "general_armor.geo.json")
    save(t, "item/armor", "general_armor")
    save(gl, "item/armor", "general_armor_glowmask")
    anims().write(ANIM + "general_armor.animation.json")
    for kind in ("helmet", "chestplate", "leggings", "boots"):
        save(icon(kind), "item", f"general_{kind}")


if __name__ == "__main__":
    generate()
