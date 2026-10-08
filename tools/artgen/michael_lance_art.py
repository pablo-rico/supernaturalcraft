"""The Lance of Michael (v0.12): one geometry for the item and both lance entities (Michael's and the player's).

Some 3 blocks long (48 px): a white shaft wound with a gold spiral, gold bands at the grip, a guard with small
wings, and a long leaf blade of light (glowmask). The grip is at the origin and the tip at local +Z (TIP_BLOCKS),
so the projectiles can be drawn pointing along their motion. `lance` (no rest rotation) -> `lance_axis` (turned
-90 degrees about X: its +Y becomes the lance's +Z) holds the cubes, built along +Y like the copies in the bosses'
hands (michael_art.lance_parts).

Textures: textures/item/michael_lance.png, michael_lance_glowmask.png, michael_lance_borrowed.png (brighter: the
stolen lance burns in a mortal's hands). Clip animation.michael_lance.idle: a shimmer on the blade.
"""

from animkit import AnimFile
from chuck_art import Model
from common import ASSETS, save
from michael_art import E_IO, lance_parts
from pixelkit import Tex, mix

GEO = ASSETS + "/geo/item/"
ANIM = ASSETS + "/animations/item/"
LENGTH = 48.0
GRIP = 15.0                      # px from the butt to the grip (the origin)
TIP_BLOCKS = (LENGTH - GRIP) / 16.0
ICON_ANGLE = 135


def rig():
    m = Model("michael_lance", 128, 128)
    m.bone("lance", (0, 0, 0))
    axis = m.bone("lance_axis", (0, 0, 0), "lance", rotation=(-90, 0, 0))
    lance_parts(m, axis, (0, -GRIP, 0), LENGTH, 1.0)
    return m


def anims():
    f = AnimFile()
    idle = f.new("animation.michael_lance.idle", 2.0, loop=True)
    idle.scale("lance", (0, [1, 1, 1]), (1.0, [1.02, 1.02, 1.02], E_IO), (2.0, [1, 1, 1], E_IO))
    return f


def brighter(t, k=0.45):
    out = Tex(t.w, t.h, 0)
    for y in range(t.h):
        for x in range(t.w):
            c = t.rows[y][x]
            out.rows[y][x] = mix(c, (255, 255, 255, c[3]), k) if c[3] else c
    return out


def generate():
    m = rig()
    t, g = m.build(gutter=1, seed=7900)
    m.rig.write(GEO + "michael_lance.geo.json")
    save(t, "item", "michael_lance")
    save(g, "item", "michael_lance_glowmask")
    save(brighter(t), "item", "michael_lance_borrowed")
    # Inventory icons (the item models show these in the GUI): the lance corner to corner, blade up.
    import preview3d
    save(preview3d.icon(m.rig, t, size=16, angle_deg=ICON_ANGLE, view="side"), "item", "michael_lance_icon")
    save(preview3d.icon(m.rig, brighter(t), size=16, angle_deg=ICON_ANGLE, view="side"), "item", "borrowed_lance_icon")
    anims().write(ANIM + "michael_lance.animation.json")


if __name__ == "__main__":
    generate()
