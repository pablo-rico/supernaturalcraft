"""The crossroads demon: a handsome man in a sharp black suit, white shirt and thin black tie, hair
slicked back, and eyes that flash red. Drawn by DemonRenderer as "crossroads_demon".

Reuses the shared humanoid rig and demon_art's helpers and animations (idle, walk, attack, smoke_out,
trapped), and adds `seal` (he steps in and leans down for the kiss that seals the deal) and `offer`
(an open palm, a tilt of the head: name your price). The glowmask lights only the red eyes.
"""

import palette as P
from common import save
from demon_art import ANIM, GEO, SHIRT, SHOE, head_material, humanoid_anims
from geomodel import box
from humanoid import humanoid
from paint import cloth, noise, paint_cube, set_face_px, stamp_face
from pixelkit import Ramp, Tex, fbm, hexc, mix

SUIT = Ramp("#060608", "#0d0d11", "#15151b", "#1e1e26", "#292933", "#363642")
SATIN = Ramp("#101016", "#1c1c25", "#2a2a36", "#3a3a48", "#4c4c5c", "#62627a")  # lapels catch the light
HAIR = Ramp("#070605", "#0f0c0a", "#18130f", "#221b15", "#2e241c", "#3c2f24")
TIE = Ramp("#020203", "#060608", "#0b0b0e", "#121216", "#1b1b21", "#26262e")
SKIN = Ramp("#3a2820", "#634638", "#876553", "#a5806b", "#bf9a84", "#d5b49f")
RED = [hexc("#6e0606"), hexc("#c0140f"), hexc("#ff3a24"), hexc("#ff8a6a")]
A = "animation.crossroads_demon."


def rig():
    r, p = humanoid("crossroads_demon", 128, 128)
    head, body = r.get("head"), r.get("body")
    p["hair"] = box(head, (-4, 29, -4), (8, 3, 8), inflate=0.3, tag="hair")
    p["jacket"] = box(body, (-4, 12, -2), (8, 12, 4), inflate=0.3, tag="jacket")
    p["tails_r"] = box(r.get("right_leg"), (-4.1, 8, -2), (4, 4, 4), inflate=0.4, tag="tails")
    p["tails_l"] = box(r.get("left_leg"), (0.1, 8, -2), (4, 4, 4), inflate=0.4, tag="tails")
    p["cuff_r"] = box(r.get("right_arm"), (-8, 15, -2), (4, 1, 4), inflate=0.25, tag="cuff")
    p["cuff_l"] = box(r.get("left_arm"), (4, 15, -2), (4, 1, 4), inflate=0.25, tag="cuff")
    r.pack()
    return r, p


def tailored(seed, base=2.5):
    """Fine black wool: an even tone, a faint twill, darker hems. No blotches: the suit is sharp."""
    def m(face, x, y, w, h):
        n = fbm(x * 2 + len(face), y * 2, seed, 64, 64, 2, 8.0)
        v = base + (n - 0.5) * 0.8 + (0.35 if (x + y) % 3 == 0 else 0)
        if face in ("north", "south", "east", "west") and y == h - 1:
            v -= 1
        return SUIT[max(0, min(5, int(round(v))))]
    return m


class Painter:
    def __init__(self, rig_, parts):
        self.r, self.p = rig_, parts
        self.t = Tex(rig_.tex_w, rig_.tex_h, 1601)
        self.g = Tex(rig_.tex_w, rig_.tex_h, 1602)

    def glow(self, cube, face, x, y, c, k=1.0):
        set_face_px(self.t, cube, face, x, y, c)
        set_face_px(self.g, cube, face, x, y, tuple(int(v * k) for v in c[:3]) + (255,))

    def paint(self):
        p = self.p
        skin = SKIN
        # Slicked back: hair only on the crown, back and a neat line at the temples.
        paint_cube(self.t, p["head"], head_material(skin, HAIR, back_rows=6, side_rows=2, seed=1603))
        hair_m = noise(HAIR, 1604, 2.4, 1.2, 9.0)

        def slick(face, x, y, w, h):
            if face == "down":
                return None
            if face == "north":
                return hair_m(face, x, y, w, h) if y == 0 else None
            if face == "up":
                return HAIR[4] if x in (2, 3) and y % 2 == 0 else hair_m(face, x, y, w, h)  # comb lines, side parting
            if face in ("east", "west") and y == h - 1 and x % 2:
                return None
            return hair_m(face, x, y, w, h)
        paint_cube(self.t, p["hair"], slick)
        self.face()

        shirt = cloth(SHIRT, 1605, 3.2, 0.15)
        paint_cube(self.t, p["body"], shirt)
        suit = tailored(1606)

        def jacket(face, x, y, w, h):
            if face == "down":
                return None
            if face == "north":
                v = max(0, 4 - y)  # the V of the lapels closes at the button (y = 5)
                if y < 5 and 3 - v < x < 4 + v:
                    return None
                if y < 5 and (x == 3 - v or x == 4 + v):
                    return SATIN[4] if y < 3 else SATIN[3]
                if y < 6 and (x == 2 - v or x == 5 + v) and v > 0:
                    return SATIN[2]
                if x in (3, 4) and y == 6:
                    return hexc("#030304")  # the one button
                if x == 6 and y == 5:
                    return SHIRT[5]  # pocket square
                if y == 8 and x in (1, 6):
                    return SUIT[0]  # pocket flaps
            if face == "up" and 3 <= x <= 4 and y < 2:
                return None
            return suit(face, x, y, w, h)
        paint_cube(self.t, p["jacket"], jacket)
        stamp_face(self.t, p["body"], "north", [
            "..wkkw..",
            "...kk...",
            "...kK...",
            "...kk...",
            "...kK...",
            "...kk...",
        ], {"w": SHIRT[5], "k": TIE[2], "K": TIE[4]})

        sleeve, hand = tailored(1607), noise(skin, 1608, 3.1, 0.4)
        for side in ("right_arm", "left_arm"):
            paint_cube(self.t, p[side], lambda f, x, y, w, h, s=sleeve, hd=hand:
                       hd(f, x, y, w, h) if (f == "down" or (f != "up" and y >= h - 3)) else s(f, x, y, w, h))
        for c in ("cuff_r", "cuff_l"):
            paint_cube(self.t, p[c], lambda f, x, y, w, h: None if f in ("up", "down") else SHIRT[5])
        legs, shoes = tailored(1609, 2.3), noise(SHOE, 1610, 2.2, 0.6)

        def leg(face, x, y, w, h):
            if face == "down" or (face != "up" and y >= h - 2):
                c = shoes(face, x, y, w, h)
                return SHOE[5] if face == "north" and y == h - 2 and x == 1 else c  # a polished toe cap
            if face == "north" and x == 1 and y < h - 2:
                return SUIT[3]  # pressed crease
            return legs(face, x, y, w, h)
        for side in ("right_leg", "left_leg"):
            paint_cube(self.t, p[side], leg)
        for tail in ("tails_r", "tails_l"):
            paint_cube(self.t, p[tail], lambda f, x, y, w, h: None if f in ("up", "down") else suit(f, x, y, w, h))
        return self.t, self.g

    def face(self):
        p, skin = self.p, SKIN
        pal = {"b": HAIR[1], "n": skin[3], "N": skin[2], "m": hexc("#5e3a33"), "c": skin[2]}
        stamp_face(self.t, p["head"], "north", [
            "........",
            "........",
            ".bb..bb.",
            "........",
            "...nN...",
            "........",
            "..mmmc..",
            "........",
        ], pal)
        head = p["head"]
        for (x, c, k) in ((1, RED[1], 0.8), (2, RED[2], 1.0), (5, RED[2], 1.0), (6, RED[1], 0.8)):
            self.glow(head, "north", x, 3, c, k)
        # A faint red cast under the eyes in the glow only.
        for x in (1, 2, 5, 6):
            set_face_px(self.g, head, "north", x, 4, (60, 4, 2, 255))


def anims():
    f = humanoid_anims("crossroads_demon")
    E_IO, E_OUT, E_BACK = "easeInOutSine", "easeOutQuad", "easeOutBack"

    # The kiss: a step in, a hand at the jaw, lean down and hold, then back with a satisfied air.
    seal = f.new(A + "seal", 1.5)
    seal.pos("root", (0, [0, 0, 0]), (0.35, [0, 0, -3], E_OUT), (1.15, [0, 0, -3]), (1.5, [0, 0, 0], E_IO))
    seal.rot("right_leg", (0, [0, 0, 0]), (0.18, [-22, 0, 0], E_OUT), (0.35, [0, 0, 0], E_IO), (1.2, [0, 0, 0]),
             (1.35, [16, 0, 0], E_OUT), (1.5, [0, 0, 0], E_IO))
    seal.rot("body", (0, [0, 0, 0]), (0.45, [16, 0, 0], E_OUT), (1.05, [18, 0, 0]), (1.5, [0, 0, 0], E_IO))
    seal.rot("head", (0, [0, 0, 0]), (0.45, [10, 0, 14], E_OUT), (1.05, [12, 4, 16]), (1.3, [-6, -8, 4], E_IO),
             (1.5, [0, 0, 0], E_IO))
    seal.rot("right_arm", (0, [0, 0, 0]), (0.4, [-68, -20, 8], E_OUT), (1.05, [-72, -22, 10]), (1.5, [0, 0, 0], E_IO))
    seal.rot("left_arm", (0, [0, 0, 0]), (0.4, [-25, 0, -12], E_OUT), (1.05, [-28, 0, -14]), (1.5, [0, 0, 0], E_IO))

    # The offer: one palm opened, head tilted, a slight lean back. Name your price.
    offer = f.new(A + "offer", 1.2)
    offer.rot("right_arm", (0, [0, 0, 0]), (0.35, [-62, 22, 18], E_BACK), (0.9, [-60, 20, 16]), (1.2, [0, 0, 0], E_IO))
    offer.rot("left_arm", (0, [0, 0, 0]), (0.35, [12, 0, -6], E_OUT), (0.9, [12, 0, -6]), (1.2, [0, 0, 0], E_IO))
    offer.rot("head", (0, [0, 0, 0]), (0.35, [-6, 14, -14], E_BACK), (0.9, [-4, 12, -12]), (1.2, [0, 0, 0], E_IO))
    offer.rot("body", (0, [0, 0, 0]), (0.35, [-5, 8, 0], E_OUT), (0.9, [-5, 8, 0]), (1.2, [0, 0, 0], E_IO))
    return f


def generate():
    r, p = rig()
    r.write(GEO + "crossroads_demon.geo.json")
    t, g = Painter(r, p).paint()
    save(t, "entity", "crossroads_demon")
    save(g, "entity", "crossroads_demon_glowmask")
    anims().write(ANIM + "crossroads_demon.animation.json")


if __name__ == "__main__":
    generate()
