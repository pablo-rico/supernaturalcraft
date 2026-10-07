"""Metatron, the Scribe of God: his GeckoLib geometry, four phase textures (+ glowmasks) and animations;
his two constructs (the Hand with its quill, the Book); his bust, the scripture stone, the Angel Tablet,
and his ink and pages.

  P1 The Scribe         -- the vessel: an older man, glasses, a cardigan, a quill behind his ear.
  P2 The Hand of God    -- the same, ink on his hands.
  P3 Library of Heaven  -- a scribe's robe, wings of written pages; a faint light of his own.
  P4 The Angel Tablet   -- cracks of gold light; the Tablet in his hand.
"""

import random

import palette as P
import scribe_hand_art
from animkit import AnimFile
from azazel_art import ANIM, GEO, write_model
from common import art, save
from geomodel import Rig, box
from humanoid import humanoid
from lilith_art import _tex
from lucifer_art import crack_field
from paint import cloth, noise, paint_cube, set_face_px, stamp_face
from particles import _blob
from pixelkit import Ramp, Tex, fbm, hexc, mix

SKIN = Ramp("#5e4436", "#866250", "#a77e68", "#c09780", "#d4ae97", "#e4c6b1")
HAIR = Ramp("#4a4440", "#635c57", "#7d7670", "#97908a", "#aea8a2", "#c4bfba")
CARDIGAN = Ramp("#3b2a1c", "#523b27", "#6b4e33", "#7f6140", "#93754f", "#a88a62")
SHIRT = Ramp("#5d6f80", "#71849a", "#8799ae", "#9dafc2", "#b4c4d4", "#ccd8e4")
TROUSER = Ramp("#4a4232", "#5c533f", "#6e644c", "#80765a", "#928868", "#a49a78")
SHOE = Ramp("#1e1610", "#2c2018", "#3a2b20", "#493628", "#584231", "#674e3a")
ROBE = Ramp("#b8ab90", "#cfc3a8", "#e1d7bf", "#ede5d2", "#f5efe2", "#fbf8f0")
GOLD = P.GOLD
PAGE = Ramp("#a8977a", "#c4b494", "#d9cbad", "#e7dcc3", "#f1e9d6", "#faf6ea")
LIGHT = [hexc("#ffe7a0"), hexc("#fff3c4"), hexc("#ffffff")]
INK = hexc("#141018")
EYE = hexc("#bfe8ff")


# --- Metatron ---------------------------------------------------------------------------------

def rig():
    r, p = humanoid("metatron", 128, 128)
    head, body = r.get("head"), r.get("body")
    p["hair"] = box(head, (-4, 29, -4), (8, 3, 8), inflate=0.25, tag="hair")
    p["glasses"] = box(head, (-4, 27, -4.35), (8, 3, 0), faces=("north", "south"), tag="glasses")
    quill = r.bone("quill_ear", (-4.5, 28, 0), "head", rotation=(0, 0, -20))
    p["quill"] = box(quill, (-5, 26, -0.5), (1, 7, 0), faces=("east", "west"), tag="quill")
    p["cardigan"] = box(body, (-4, 12, -2), (8, 12, 4), inflate=0.35, tag="cardigan")
    robe = r.bone("robe", (0, 12, 0), "body")
    p["robe"] = box(robe, (-4.5, 1, -2.5), (9, 11, 5), inflate=0.2, tag="robe")
    wings = r.bone("wings", (0, 21, 2), "body")
    for side, s in (("r", -1), ("l", 1)):
        w = r.bone(f"wing_{side}", (2 * s, 21, 2.5), "wings", rotation=(0, 18 * -s, 22 * -s))
        x0 = 2 if s > 0 else -18
        p[f"wing_{side}"] = box(w, (x0, 4, 2.6), (16, 18, 0), faces=("north", "south"), tag=f"wing_{side}")
    tablet = r.bone("tablet", (6, 12, 0), "left_arm")
    p["tablet"] = box(tablet, (8, 7, -3), (1, 7, 6), tag="tablet")
    r.pack()
    return r, p


class Painter:
    def __init__(self, rig_, parts, phase):
        self.r, self.p, self.phase = rig_, parts, phase
        self.t = Tex(rig_.tex_w, rig_.tex_h, 110 + phase)
        self.g = Tex(rig_.tex_w, rig_.tex_h, 120 + phase)

    def glow(self, cube, face, x, y, c):
        set_face_px(self.g, cube, face, x, y, c)
        set_face_px(self.t, cube, face, x, y, c)

    def paint(self):
        p, ph = self.p, self.phase
        hair_m, skin_m = noise(HAIR, 41, 2.8, 1.2, 6.0), noise(SKIN, 42, 3.0, 0.45)

        def head(face, x, y, w, h):
            # Thinning grey hair: the crown and the sides, not the brow.
            if face == "up" and (x in (0, 7) or y > 3):
                return hair_m(face, x, y, w, h)
            if face == "south" and y < 6 or (face in ("east", "west") and y < 4 and x > 2):
                return hair_m(face, x, y, w, h)
            return skin_m(face, x, y, w, h)
        paint_cube(self.t, p["head"], head)
        paint_cube(self.t, p["hair"], lambda f, x, y, w, h: None if f in ("down", "north") or (f == "up" and 2 <= x <= 5 and y < 4)
                   or (f != "up" and y > 0) else hair_m(f, x, y, w, h))
        self.face()
        frame = hexc("#2a2420")
        rims = {0: (1, 2, 5, 6), 1: (0, 3, 4, 7), 2: (1, 2, 5, 6)}
        paint_cube(self.t, p["glasses"], lambda f, x, y, w, h: frame if x in rims.get(y, ()) else None, shading=False)
        paint_cube(self.t, p["quill"], lambda f, x, y, w, h: PAGE[5] if y < 5 else PAGE[1], shading=False)

        scribe = ph >= 3
        cloth_m = cloth(ROBE if scribe else CARDIGAN, 43, 3.0 if scribe else 2.6, 0.3)
        paint_cube(self.t, p["body"], cloth(SHIRT, 44, 3.0, 0.2))

        def cardigan(face, x, y, w, h):
            if face == "down":
                return None
            if face == "north" and not scribe and 3 <= x <= 4 and y < 9:
                return None  # open front: the shirt shows
            if face == "north" and x in (2, 5) and not scribe:
                return CARDIGAN[4] if y % 3 == 1 else CARDIGAN[3]  # buttons and edging
            if scribe and (y == h - 1 or x == 0 or x == w - 1) and face != "up":
                return GOLD[4]  # gold trim on the robe
            return cloth_m(face, x, y, w, h)
        paint_cube(self.t, p["cardigan"], cardigan)

        hands = noise(SKIN, 45, 3.0, 0.45)
        for side in ("right_arm", "left_arm"):
            def arm(face, x, y, w, h, hd=hands):
                if face == "down" or (face != "up" and y >= h - 3):
                    if ph == 2 and (x + y * 2) % 5 == 0:
                        return INK
                    return hd(face, x, y, w, h)
                return cloth_m(face, x, y, w, h)
            paint_cube(self.t, p[side], arm)
        legs, shoes = cloth(TROUSER, 46, 2.6, 0.2), noise(SHOE, 47, 2.4, 0.5)
        for side in ("right_leg", "left_leg"):
            paint_cube(self.t, p[side], lambda f, x, y, w, h, l=legs, b=shoes:
                       b(f, x, y, w, h) if (f == "down" or (f != "up" and y >= h - 2)) else l(f, x, y, w, h))
        robe_m = cloth(ROBE, 48, 3.0, 0.3)
        paint_cube(self.t, p["robe"], lambda f, x, y, w, h: None if f in ("up", "down")
                   else GOLD[4] if y == h - 1 else robe_m(f, x, y, w, h))
        self.wings()
        self.tablet()
        if scribe:
            self.inner_light(0.35 if ph == 3 else 0.5)
        if ph >= 4:
            self.cracks()
        return self.t, self.g

    def face(self):
        pal = {"b": HAIR[1], "n": SKIN[2], "m": hexc("#6e4436"), "w": hexc("#e8e2da"), "e": hexc("#3a5068")}
        stamp_face(self.t, self.p["head"], "north", [
            "........",
            "........",
            ".bb..bb.",
            ".we..ew.",
            "........",
            "...nn...",
            "..mmmm..",
            "........",
        ], pal)
        if self.phase >= 3:
            for x in (2, 5):
                self.glow(self.p["head"], "north", x, 3, EYE)

    def wings(self):
        rng = random.Random(51)
        for side in ("wing_r", "wing_l"):
            cube = self.p[side]
            for face, (u, v, w, h) in cube.rects.items():
                for y in range(h):
                    for x in range(w):
                        # Overlapping pages: ragged edges, lines of script.
                        if (x + y) % 6 == 0 and y > h - 3:
                            continue
                        c = PAGE[3 + (x // 4 + y // 5) % 2]
                        if y % 3 == 1 and 1 <= x % 8 <= 6 and rng.random() < 0.7:
                            c = mix(c, INK, 0.55)
                        set_face_px(self.t, cube, face, x, y, c)

    def tablet(self):
        stone = noise(P.STONE, 52, 3.4, 0.6)
        cube = self.p["tablet"]
        for face, (u, v, w, h) in cube.rects.items():
            for y in range(h):
                for x in range(w):
                    c = stone(face, x, y, w, h)
                    if face in ("east", "west") and 0 < x < w - 1 and y % 2 == 1:
                        self.glow(cube, face, x, y, LIGHT[1] if (x + y) % 3 else LIGHT[2])
                        continue
                    set_face_px(self.t, cube, face, x, y, c)

    def inner_light(self, k):
        for y in range(self.t.h):
            for x in range(self.t.w):
                c = self.t.rows[y][x]
                if c[3] and not self.g.rows[y][x][3]:
                    self.g.rows[y][x] = (int(c[0] * k), int(c[1] * k), int(c[2] * k), 255)

    def cracks(self):
        for name, seed in (("head", 61), ("cardigan", 62), ("right_arm", 63), ("left_arm", 64), ("robe", 65)):
            cube = self.p[name]
            for face, (u, v, w, h) in cube.rects.items():
                if face in ("up", "down"):
                    continue
                for (x, y) in crack_field(seed + u * 7 + v, w, h, 1):
                    self.glow(cube, face, x, y, LIGHT[0])


def paint_phase(phase):
    r, p = rig()
    return Painter(r, p, phase).paint()


E_IO, E_OUT, E_IN, E_BACK, E_SNAP = "easeInOutSine", "easeOutQuad", "easeInQuad", "easeOutBack", "easeInQuart"
A = "animation.metatron."


def anims():
    f = AnimFile()
    idle = f.new(A + "idle", 4.0, loop=True)
    idle.pos("body", (0, [0, 0, 0]), (2.0, [0, -0.2, 0], E_IO), (4.0, [0, 0, 0], E_IO))
    idle.rot("head", (0, [4, 0, 0]), (2.0, [6, 8, 2], E_IO), (4.0, [4, 0, 0], E_IO))
    idle.rot("right_arm", (0, [-6, 0, 4]), (2.0, [-10, 0, 6], E_IO), (4.0, [-6, 0, 4], E_IO))
    idle.rot("left_arm", (0, [-6, 0, -4]), (2.0, [-10, 0, -6], E_IO), (4.0, [-6, 0, -4], E_IO))

    walk = f.new(A + "walk", 1.3, loop=True)
    for bone, s in (("right_leg", 1), ("left_leg", -1)):
        walk.rot(bone, (0, [20 * s, 0, 0]), (0.65, [-20 * s, 0, 0], E_IO), (1.3, [20 * s, 0, 0], E_IO))
    for bone, s in (("right_arm", -1), ("left_arm", 1)):
        walk.rot(bone, (0, [12 * s, 0, 0]), (0.65, [-12 * s, 0, 0], E_IO), (1.3, [12 * s, 0, 0], E_IO))

    f.new(A + "idle_fly", 1.0, loop=True).const_rot("root", [0, 0, 0])
    wi = f.new(A + "wings_idle", 4.0, loop=True)
    wi.rot("wing_r", (0, [0, 0, 0]), (2.0, [0, 4, 6], E_IO), (4.0, [0, 0, 0], E_IO))
    wi.rot("wing_l", (0, [0, 0, 0]), (2.0, [0, -4, -6], E_IO), (4.0, [0, 0, 0], E_IO))
    f.new(A + "wings_fly", 1.0, loop=True).const_rot("root", [0, 0, 0])

    emerge = f.new(A + "emerge", 5.0, hold=True)
    emerge.scale("root", (0, [0.05, 1.4, 0.05]), (1.5, [0.05, 1.4, 0.05]), (2.0, [1.1, 1.0, 1.1], E_BACK), (2.4, [1, 1, 1], E_IO), (5.0, [1, 1, 1]))
    emerge.rot("head", (0, [20, 0, 0]), (2.4, [20, 0, 0]), (3.4, [-10, 0, 0], E_IO), (5.0, [4, 0, 0], E_IO))
    emerge.rot("right_arm", (0, [0, 0, 0]), (2.4, [0, 0, 0]), (3.2, [-60, 0, 30], E_OUT), (4.2, [-60, 0, 30]), (5.0, [-6, 0, 4], E_IO))

    def attack(name, length):
        return f.new(A + name, length)

    blade = attack("blade_rush", 2.6)
    blade.rot("body", (0, [0, 0, 0]), (0.7, [20, 0, 0], E_OUT), (1.1, [10, 25, 0], E_SNAP), (1.45, [10, -25, 0], E_SNAP), (2.6, [0, 0, 0], E_IO))
    blade.rot("right_arm", (0, [0, 0, 0]), (0.7, [30, 0, 10]), (1.1, [-110, 0, 30], E_SNAP), (1.45, [-30, 0, -20], E_SNAP), (2.6, [0, 0, 0], E_IO))

    smite = attack("smite", 1.9)
    smite.rot("right_arm", (0, [0, 0, 0]), (0.8, [-40, 0, 20], E_OUT), (0.9, [-90, 0, 0], E_SNAP), (1.3, [-88, 0, 0]), (1.9, [0, 0, 0], E_IO))
    smite.rot("head", (0, [0, 0, 0]), (0.8, [10, 0, 0], E_OUT), (0.9, [-10, 0, 0], E_SNAP), (1.9, [4, 0, 0], E_IO))

    ink = attack("ink_bolt", 1.7)
    ink.rot("right_arm", (0, [0, 0, 0]), (0.7, [-70, 20, 0], E_OUT), (0.8, [-85, -10, 0], E_SNAP), (1.7, [0, 0, 0], E_IO))

    command = attack("command", 2.5)
    command.rot("right_arm", (0, [0, 0, 0]), (0.8, [-150, 0, 20], E_OUT), (1.6, [-145, 0, 25]), (1.8, [-70, 0, 10], E_SNAP), (2.5, [-6, 0, 4], E_IO))
    command.rot("head", (0, [0, 0, 0]), (0.8, [-20, 0, 0], E_OUT), (1.8, [5, 0, 0], E_SNAP), (2.5, [4, 0, 0], E_IO))

    fall = attack("the_fall", 5.2)
    for side, s in (("right_arm", 1), ("left_arm", -1)):
        fall.rot(side, (0, [0, 0, 0]), (1.6, [-170, 0, 15 * s], E_OUT), (4.2, [-172, 0, 18 * s]), (5.2, [0, 0, 0], E_IO))
    fall.rot("head", (0, [0, 0, 0]), (1.6, [-35, 0, 0], E_OUT), (4.2, [-35, 0, 0]), (5.2, [4, 0, 0], E_IO))

    word = attack("the_word", 5.5)
    word.rot("left_arm", (0, [0, 0, 0]), (1.2, [-120, 0, -10], E_OUT), (4.5, [-125, 0, -10]), (5.5, [0, 0, 0], E_IO))
    word.rot("right_arm", (0, [0, 0, 0]), (1.2, [-40, 0, 30], E_OUT), (4.5, [-40, 0, 30]), (5.5, [0, 0, 0], E_IO))
    word.rot("head", (0, [0, 0, 0]), (1.2, [-15, 0, 0], E_OUT), (4.5, [-15, 0, 0]), (5.5, [4, 0, 0], E_IO))

    tp = attack("teleport", 1.0)
    tp.scale("root", (0, [1, 1, 1]), (0.45, [0.1, 1.6, 0.1], E_IN), (0.55, [0.1, 1.6, 0.1]), (1.0, [1, 1, 1], E_BACK))

    for n in (2, 3, 4):
        t = f.new(A + f"transform_{n}", 4.0)
        t.rot("head", (0, [0, 0, 0]), (1.0, [25, 0, 0], E_OUT), (2.0, [25, 0, 0]), (2.4, [-35, 0, 0], E_BACK), (4.0, [4, 0, 0], E_IO))
        for side, s in (("right_arm", 1), ("left_arm", -1)):
            t.rot(side, (0, [0, 0, 0]), (1.0, [-10, 0, -5 * s], E_OUT), (2.0, [-10, 0, -5 * s]), (2.4, [-50, 0, 80 * s], E_BACK),
                  (3.4, [-50, 0, 85 * s]), (4.0, [-6, 0, 4 * s], E_IO))
        if n >= 3:
            for side, s in (("wing_r", 1), ("wing_l", -1)):
                t.scale(side, (0, [0.05, 0.05, 0.05]), (2.0, [0.05, 0.05, 0.05]), (2.8, [1.15, 1.15, 1.15], E_BACK), (3.6, [1, 1, 1], E_IO))
        if n == 4:
            t.scale("tablet", (0, [0, 0, 0]), (2.2, [0, 0, 0]), (3.0, [1.3, 1.3, 1.3], E_BACK), (3.6, [1, 1, 1], E_IO))

    death = f.new(A + "death", 7.0, hold=True)
    death.pos("root", (0, [0, 0, 0]), (2.0, [0, -4, 0], E_IN), (7.0, [0, -4, 0]))
    death.rot("right_leg", (0, [0, 0, 0]), (2.0, [-85, 0, 0], E_OUT), (7.0, [-85, 0, 0]))
    death.rot("left_leg", (0, [0, 0, 0]), (2.0, [-80, 0, 0], E_OUT), (7.0, [-80, 0, 0]))
    death.rot("head", (0, [0, 0, 0]), (2.0, [25, 0, 0], E_OUT), (4.5, [-50, 0, 0], E_IO), (7.0, [40, 0, 0], E_IN))
    death.rot("body", (0, [0, 0, 0]), (2.0, [10, 0, 0], E_OUT), (4.5, [-15, 0, 0], E_IO), (7.0, [35, 0, 0], E_IN))
    for side, s in (("right_arm", 1), ("left_arm", -1)):
        death.rot(side, (0, [0, 0, 0]), (4.5, [-40, 0, 80 * s], E_IO), (7.0, [10, 0, 10 * s], E_IN))
    return f


# --- the Book ------------------------------------------------------------------------------------

def book_rig():
    r = Rig("scribe_book", 128, 128)
    r.bone("root", (0, 0, 0))
    spine = r.bone("spine", (0, 2, 0), "root")
    p = {"spine": box(spine, (-1, 0, -10), (2, 3, 20), tag="spine")}
    for side, s in (("l", 1), ("r", -1)):
        cover = r.bone(f"cover_{side}", (0, 1, 0), "spine")
        x0 = 0 if s > 0 else -14
        p[f"cover_{side}"] = box(cover, (x0, 0, -10), (14, 1.5, 20), tag="cover")
        pages = r.bone(f"pages_{side}", (0, 1.5, 0), f"cover_{side}")
        px0 = 0.5 if s > 0 else -13.5
        p[f"pages_{side}"] = box(pages, (px0, 1.5, -9.5), (13, 2.5, 19), tag="pages")
    r.pack()
    return r, p


def book_textures():
    r, p = book_rig()
    t, g = Tex(r.tex_w, r.tex_h, 140), Tex(r.tex_w, r.tex_h, 141)
    leather = noise(P.LEATHER, 142, 2.0, 0.8)
    for name in ("cover_l", "cover_r", "spine"):
        cube = p[name]
        for face, (u, v, w, h) in cube.rects.items():
            for y in range(h):
                for x in range(w):
                    c = leather(face, x, y, w, h)
                    if face in ("down", "up") and name != "spine" and (x in (1, w - 2) or y in (1, h - 2)):
                        c = GOLD[4]  # gold corner bands
                        set_face_px(g, cube, face, x, y, GOLD[3])
                    set_face_px(t, cube, face, x, y, c)
    rng = random.Random(143)
    for name in ("pages_l", "pages_r"):
        cube = p[name]
        for face, (u, v, w, h) in cube.rects.items():
            for y in range(h):
                for x in range(w):
                    c = PAGE[4] if face == "up" else PAGE[3 - (y % 2)]
                    if face == "up" and y % 2 == 1 and 1 <= x <= w - 2 and rng.random() < 0.75:
                        c = mix(c, INK, 0.6)
                        if rng.random() < 0.15:
                            set_face_px(g, cube, face, x, y, GOLD[4])
                    set_face_px(t, cube, face, x, y, c)
    return r, t, g


def book_anims():
    f = AnimFile()
    B = "animation.scribe_book."
    idle = f.new(B + "idle", 4.0, loop=True)
    idle.pos("root", (0, [0, 0, 0]), (2.0, [0, 1.2, 0], E_IO), (4.0, [0, 0, 0], E_IO))
    idle.rot("cover_l", (0, [0, 0, -8]), (2.0, [0, 0, -14], E_IO), (4.0, [0, 0, -8], E_IO))
    idle.rot("cover_r", (0, [0, 0, 8]), (2.0, [0, 0, 14], E_IO), (4.0, [0, 0, 8], E_IO))
    opened = f.new(B + "open", 0.6)
    opened.rot("cover_l", (0, [0, 0, -85]), (0.6, [0, 0, -8], E_OUT))
    opened.rot("cover_r", (0, [0, 0, 85]), (0.6, [0, 0, 8], E_OUT))
    close = f.new(B + "close", 0.4, hold=True)
    close.rot("cover_l", (0, [0, 0, -8]), (0.25, [0, 0, -88], E_SNAP), (0.4, [0, 0, -88]))
    close.rot("cover_r", (0, [0, 0, 8]), (0.25, [0, 0, 88], E_SNAP), (0.4, [0, 0, 88]))
    slam = f.new(B + "slam", 0.5, hold=True)
    slam.rot("cover_l", (0, [0, 0, -8]), (0.15, [0, 0, -88], E_SNAP), (0.5, [0, 0, -88]))
    slam.rot("cover_r", (0, [0, 0, 8]), (0.15, [0, 0, 88], E_SNAP), (0.5, [0, 0, 88]))
    slam.rot("root", (0, [0, 0, 0]), (0.2, [0, 0, 90], E_SNAP), (0.5, [0, 0, 90]))
    storm = f.new(B + "storm", 2.5)
    storm.rot("root", (0, [0, 0, 0]), (0.5, [-70, 0, 0], E_OUT), (2.0, [-70, 0, 0]), (2.5, [0, 0, 0], E_IO))
    for side, s in (("l", 1), ("r", -1)):
        keys = [(0, [0, 0, 0])]
        for k in range(8):
            keys.append((0.5 + k * 0.2, [0, 0, -8 * s if k % 2 else -2 * s], E_IO))
        keys.append((2.5, [0, 0, 0], E_IO))
        storm.rot(f"pages_{side}", *keys)
    return f


# --- the rest ----------------------------------------------------------------------------------

def trophy():
    cardigan, skin, hair = _tex(CARDIGAN, 901, 2.8, 1.4), _tex(SKIN, 903, 3.0, 1.0), _tex(HAIR, 905, 2.8, 1.6)
    face = Tex(16, 16, 907)
    for y in range(16):
        for x in range(16):
            face.set(x, y, hair.get(x, y) if y < 2 else skin.get(x, y))
    frame = hexc("#2a2420")
    for x in range(3, 13):
        face.set(x, 6, frame)
    for x in (3, 7, 8, 12):
        face.set(x, 7, frame)
    for x in (5, 10):
        face.set(x, 7, hexc("#3a5068"))
    for x in range(6, 10):
        face.set(x, 11, hexc("#6e4436"))
    for name, t in (("cardigan", cardigan), ("face", face), ("hair", hair), ("skin", skin)):
        save(t, "block", "metatron_trophy_" + name)

    def el(name, frm, to, tex, uv=None, per=None):
        faces = {fc: {"uv": uv or [0, 0, 16, 16], "texture": "#" + ((per or {}).get(fc, tex))}
                 for fc in ("north", "south", "east", "west", "up", "down")}
        return {"name": name, "from": frm, "to": to, "faces": faces}
    elements = [
        el("pedestal", [4, 0, 4], [12, 3, 12], "base", [4, 4, 12, 12]),
        el("shoulders", [4, 3, 5.5], [12, 7, 10.5], "cardigan", [2, 2, 10, 6]),
        el("neck", [7, 7, 7], [9, 8, 9], "skin", [4, 4, 6, 6]),
        el("head", [5, 8, 5], [11, 14, 11], "skin", [0, 0, 16, 16], {"north": "face", "up": "hair", "south": "hair"}),
    ]
    write_model("metatron_trophy", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                    "textures": {"particle": "supernaturalcraft:block/trophy_base", "base": "supernaturalcraft:block/trophy_base",
                                                 "cardigan": "supernaturalcraft:block/metatron_trophy_cardigan",
                                                 "face": "supernaturalcraft:block/metatron_trophy_face",
                                                 "hair": "supernaturalcraft:block/metatron_trophy_hair",
                                                 "skin": "supernaturalcraft:block/metatron_trophy_skin"},
                                    "elements": elements})


def scripture_stone():
    t = _tex(Ramp("#b9b2a2", "#cfc8b8", "#ddd7c8", "#e8e3d6", "#f1ede3", "#f9f7f0"), 911, 3.0, 1.0, 3.0)
    rng = random.Random(912)
    for row in (3, 7, 11):
        x = 2
        while x < 14:
            ln = rng.randint(1, 3)
            for k in range(ln):
                if x + k < 14:
                    t.set(x + k, row, GOLD[4] if rng.random() < 0.8 else GOLD[5])
            x += ln + 1
    return t


def angel_tablet():
    pal = {"o": P.OUTLINE, "s": P.STONE[3], "S": P.STONE[4], "d": P.STONE[1], "g": GOLD[4], "G": GOLD[5], "w": hexc("#ffffff")}
    return art([
        "................",
        "...oooooooooo...",
        "..osSSSSSSSSso..",
        "..oSgGgsgGgsSo..",
        "..oSsssssssssSo.",
        "..oSgsGgwgsgSo..",
        "..oSssssssssSo..",
        "..oSggGsgGgsSo..",
        "..oSssssssssSo..",
        "..oSgsgGgwgsSo..",
        "..oSssssssssSo..",
        "..oSgGgsgsGgSo..",
        "..osSSSSSSSSso..",
        "..odddddddddddo.",
        "...oooooooooo...",
        "................",
    ], pal)


def particles():
    for i in range(4):
        t = _blob(Ramp("#08060a", "#141018", "#221a26", "#30243a", "#3e2f4a", "#4c3a5a"), 2.6 - i * 0.4, 950 + i, core=True)
        for y in range(8):
            for x in range(8):
                c = t.rows[y][x]
                if c[3] and (x in (0, 7) or y in (0, 7) or (x + y + i) % 7 == 0):
                    t.rows[y][x] = (GOLD[4][0], GOLD[4][1], GOLD[4][2], 230)
        save(t, "particle", f"ink_{i}")
    for i in range(4):
        t = Tex(8, 8, 960 + i)
        w, h = 5 - (i % 2), 6 - (i // 2)
        x0, y0 = (8 - w) // 2, (8 - h) // 2
        for y in range(h):
            for x in range(w):
                c = PAGE[4] if (y % 2 == 0 or x in (0, w - 1)) else mix(PAGE[3], INK, 0.4)
                t.set(x0 + x, y0 + y, c)
        save(t, "particle", f"page_{i}")


def generate():
    r, _ = rig()
    r.write(GEO + "metatron.geo.json")
    for phase in (1, 2, 3, 4):
        t, g = paint_phase(phase)
        save(t, "entity", f"metatron_p{phase}")
        save(g, "entity", f"metatron_p{phase}_glowmask")
    anims().write(ANIM + "metatron.animation.json")
    scribe_hand_art.generate()
    br, bt, bg = book_textures()
    br.write(GEO + "scribe_book.geo.json")
    save(bt, "entity", "scribe_book")
    save(bg, "entity", "scribe_book_glowmask")
    book_anims().write(ANIM + "scribe_book.animation.json")
    trophy()
    save(scripture_stone(), "block", "scripture_stone")
    save(angel_tablet(), "item", "angel_tablet")
    particles()


if __name__ == "__main__":
    generate()
