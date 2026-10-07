"""Dean, Sam and Castiel (v0.10): they step out of the light at the end of the Author's last chapter and hold him.

One bone set for the three (so one animation file serves all of them); each model has its own cubes:
  root -- body -- head, pendant (Dean's amulet, Cas's loose tie; empty on Sam),
                  right_arm -- right_forearm, left_arm -- left_forearm
       -- right_leg, left_leg, coat_tail_r, coat_tail_l (Cas's trench coat skirt; empty on the brothers)

  Dean    -- short spiky dark-blond hair, green eyes, brown leather jacket over a dark plaid shirt, jeans, boots, the
             amulet on its cord.
  Sam     -- built taller (legs and torso 13 px: about 1.07x), long brown hair to the jaw, a blue-brown plaid flannel
             open over a grey henley, jeans, boots.
  Castiel -- dark messy hair, blue eyes, stubble; the beige trench coat (long flared skirt, belt, big collar) over a
             dark suit, white shirt and a loose blue tie askew.

Animations (animation.hunter_ally.*): loop `idle`; one-shots `appear` (steps out of the light), `hold` (reaches and
grips the Author's arm and shoulder in front of him, holds), `talk` and `nod` (both keep the grip, so they can play
while holding), `fade` (lets go, straightens, holds the last pose while the renderer fades him out).
"""

import random

import palette as P
from animkit import AnimFile
from common import ASSETS, save
from geomodel import Rig, box
from paint import cloth, noise, paint_cube, set_face_px, stamp_face
from pixelkit import Ramp, Tex, fbm, hexc, mix

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"
ALLIES = ("dean", "sam", "castiel")
BONES = ("root", "body", "head", "pendant", "right_arm", "right_forearm", "left_arm", "left_forearm",
         "right_leg", "left_leg", "coat_tail_r", "coat_tail_l")

SKIN = Ramp("#5a3a2c", "#8a5f4b", "#b07f67", "#c99a80", "#dcb398", "#ecc9b0")
SKIN_TAN = Ramp("#523426", "#80573f", "#a6765a", "#bf9072", "#d3a98a", "#e4c0a2")
DEAN_HAIR = Ramp("#3a2c18", "#5a4426", "#7a5f36", "#957847", "#ad905a", "#c4a86f")
SAM_HAIR = Ramp("#1f140b", "#332112", "#4a311b", "#5f4024", "#75512f", "#8b633b")
CAS_HAIR = Ramp("#0b0807", "#15100d", "#201813", "#2b2019", "#382a20", "#463428")
LEATHER = Ramp("#22130a", "#3a2214", "#52311d", "#6a4026", "#82512f", "#9a643b")
DENIM = Ramp("#141c2b", "#1f2b40", "#2c3c57", "#3b4d6c", "#4d6083", "#64789a")
BOOT = Ramp("#120c08", "#1e150e", "#2b1e14", "#38281b", "#463222", "#553d2a")
DEAN_SHIRT = Ramp("#1a1d1a", "#262b26", "#323a33", "#3f4840", "#4c574d", "#5b665b")
HENLEY = Ramp("#3a3d42", "#50545a", "#666b72", "#7d828a", "#959aa1", "#adb2b8")
TRENCH = Ramp("#5e4f37", "#7f6c4c", "#9c8762", "#b49e77", "#c8b38c", "#dac7a2")
SUIT = Ramp("#0c0d12", "#14161d", "#1d2029", "#272b36", "#323744", "#3e4453")
WHITE = Ramp("#9a9a9a", "#bdbdbd", "#d6d6d6", "#e6e6e6", "#f2f2f2", "#ffffff")
TIE = Ramp("#0d1b3a", "#142a57", "#1d3a75", "#284c93", "#3a63ad", "#557fc4")
SHOE = Ramp("#050505", "#0d0d0e", "#151517", "#1e1e21", "#29292d", "#35353a")
EYE_WHITE = hexc("#e9e4dc")
MOUTH = hexc("#7a4a40")


# --- geometry -------------------------------------------------------------------------------

def rig(who):
    """The shared bone set, built at each hunter's proportions. Returns (rig, parts)."""
    tall = who == "sam"
    leg = 13 if tall else 12
    torso = 13 if tall else 12
    hip, shoulder = leg, leg + torso
    elbow = shoulder - (7 if tall else 6)
    r = Rig("hunter_" + who, 128, 128)
    r.bone("root", (0, 0, 0))
    body = r.bone("body", (0, hip, 0), "root")
    head = r.bone("head", (0, shoulder, 0), "body")
    pendant = r.bone("pendant", (0, shoulder - 1, -2), "body")
    arms = {}
    for side, s in (("right", -1), ("left", 1)):
        upper = r.bone(f"{side}_arm", (5 * s, shoulder - 2, 0), "body")
        fore = r.bone(f"{side}_forearm", (6 * s, elbow, 0), f"{side}_arm")
        arms[side] = (upper, fore)
    rl = r.bone("right_leg", (-2, hip, 0), "root")
    ll = r.bone("left_leg", (2, hip, 0), "root")
    tail_r = r.bone("coat_tail_r", (-2, hip, 0), "root")
    tail_l = r.bone("coat_tail_l", (2, hip, 0), "root")

    p = {}
    p["body"] = box(body, (-4, hip, -2), (8, torso, 4), tag="body")
    p["head"] = box(head, (-4, shoulder, -4), (8, 8, 8), tag="head")
    for side, s in (("right", -1), ("left", 1)):
        upper, fore = arms[side]
        x0 = -8 if s < 0 else 4
        p[f"{side}_upper"] = box(upper, (x0, elbow, -2), (4, shoulder - elbow, 4), tag="upper")
        p[f"{side}_fore"] = box(fore, (x0, hip, -2), (4, elbow - hip, 4), tag="fore")
    p["right_leg"] = box(rl, (-4.1, 0, -2), (4, leg, 4), tag="leg")
    p["left_leg"] = box(ll, (0.1, 0, -2), (4, leg, 4), tag="leg")
    for name, bone, x0 in (("boot_r", rl, -4.1), ("boot_l", ll, 0.1)):
        p[name] = box(bone, (x0, 0, -2), (4, 3, 4), inflate=0.25, tag="boot")

    def sleeves(material_tag, inflate=0.3, cuff=True):
        for side, s in (("right", -1), ("left", 1)):
            upper, fore = arms[side]
            x0 = -8 if s < 0 else 4
            p[f"{side}_sleeve_u"] = box(upper, (x0, elbow, -2), (4, shoulder - elbow, 4), inflate=inflate, tag=material_tag)
            p[f"{side}_sleeve_f"] = box(fore, (x0, hip + 2, -2), (4, elbow - hip - 2, 4), inflate=inflate, tag=material_tag)
            if cuff:
                p[f"{side}_cuff"] = box(fore, (x0, hip + 2, -2), (4, 1, 4), inflate=inflate + 0.15, tag=material_tag + "_cuff")

    if who == "dean":
        p["hair"] = box(head, (-4, shoulder + 6, -4), (8, 2, 8), inflate=0.3, tag="hair")
        for i, (x, z, rz, rx) in enumerate(((-3, -3.5, 12, -15), (-1, -3.8, -8, -20), (1, -3.6, 10, -18), (2.5, -2, -14, -6),
                                            (-2.5, -0.5, 16, 6), (0, 0.5, -6, 10), (2, 2, 10, 14), (-1.5, 2.5, -12, 16))):
            p[f"spike_{i}"] = box(head, (x - 0.5, shoulder + 8, z - 0.5), (2, 1, 2), rotation=(rx, 0, rz),
                                  pivot=(x + 0.5, shoulder + 8, z + 0.5), tag="hair")
        p["shirt"] = box(body, (-4, hip, -2), (8, torso, 4), inflate=0.15, tag="shirt")
        p["jacket"] = box(body, (-4, hip, -2), (8, torso, 4), inflate=0.4, tag="jacket")
        p["jacket_hem"] = box(body, (-4, hip - 2, -2), (8, 2, 4), inflate=0.45, tag="jacket_hem")
        p["collar"] = box(body, (-4, shoulder - 2, -2), (8, 2, 4), inflate=0.65, tag="collar")
        sleeves("leather")
        p["amulet"] = box(pendant, (-0.5, shoulder - 6, -2.6), (1, 2, 1), inflate=-0.1, tag="amulet")
    elif who == "sam":
        p["hair"] = box(head, (-4, shoulder + 5, -4), (8, 3, 8), inflate=0.35, tag="hair")
        p["hair_back"] = box(head, (-4, shoulder - 1, 2), (8, 7, 2), inflate=0.3, tag="hair_long")
        p["hair_r"] = box(head, (-4.6, shoulder - 1, -3.6), (1, 7, 6), inflate=0.15, tag="hair_long")
        p["hair_l"] = box(head, (3.6, shoulder - 1, -3.6), (1, 7, 6), inflate=0.15, tag="hair_long")
        p["fringe"] = box(head, (-4, shoulder + 5, -4.4), (8, 2, 1), inflate=0.1, tag="fringe")
        p["henley"] = box(body, (-4, hip, -2), (8, torso, 4), inflate=0.15, tag="henley")
        p["flannel"] = box(body, (-4, hip, -2), (8, torso, 4), inflate=0.4, tag="flannel")
        p["flannel_hem"] = box(body, (-4, hip - 2, -2), (8, 2, 4), inflate=0.45, tag="flannel_hem")
        p["collar"] = box(body, (-4, shoulder - 1, -2), (8, 1, 4), inflate=0.6, tag="flannel_collar")
        sleeves("flannel")
    else:
        p["hair"] = box(head, (-4, shoulder + 6, -4), (8, 2, 8), inflate=0.32, tag="hair")
        for i, (x, z, rz, rx) in enumerate(((-3, -3.2, 25, -25), (0.5, -3.5, -20, -30), (2.8, -1, -30, 5),
                                            (-2.8, 1.5, 28, 15), (1, 2.5, -15, 25))):
            p[f"tuft_{i}"] = box(head, (x - 1, shoulder + 7.5, z - 0.5), (2, 1, 1), rotation=(rx, 0, rz),
                                 pivot=(x, shoulder + 7.5, z), tag="hair")
        p["shirt"] = box(body, (-4, hip, -2), (8, torso, 4), inflate=0.1, tag="cas_shirt")
        p["suit"] = box(body, (-4, hip, -2), (8, torso, 4), inflate=0.3, tag="suit")
        p["coat"] = box(body, (-4, hip, -2), (8, torso, 4), inflate=0.55, tag="trench")
        p["coat_collar"] = box(body, (-4, shoulder - 3, -2), (8, 3, 4), inflate=0.85, tag="trench_collar")
        p["belt"] = box(body, (-4, hip + 1, -2), (8, 1, 4), inflate=0.65, tag="belt")
        p["tie"] = box(pendant, (-0.4, shoulder - 8, -2.5), (1, 7, 0), rotation=(0, 0, -9),
                       pivot=(0, shoulder - 1, -2.5), faces=("north",), tag="tie")
        p["tie_knot"] = box(pendant, (-0.6, shoulder - 2.5, -2.7), (1, 1, 1), tag="tie")
        sleeves("trench", inflate=0.4)
        for name, bone, x0, s in (("skirt_r", tail_r, -4.1, -1), ("skirt_l", tail_l, 0.1, 1)):
            p[name] = box(bone, (x0, 2, -2), (4, 10, 4), inflate=0.75, tag="skirt_" + name[-1])
        tail_r.rotation = (4, 0, -2)
        tail_l.rotation = (4, 0, 2)
    r.pack()
    return r, p


# --- textures -------------------------------------------------------------------------------

def plaid(ramp, stripe, line, seed, period=5):
    """Flannel: two crossing dark bands every `period` px, a thin light line between them, woven noise."""
    base = cloth(ramp, seed, 2.8, 0.3)

    def m(face, x, y, w, h):
        c = base(face, x, y, w, h)
        bx, by = x % period < 2, y % period < 2
        if bx and by:
            return mix(c, stripe, 0.75)
        if bx or by:
            return mix(c, stripe, 0.45)
        if x % period == 3 or y % period == 3:
            return mix(c, line, 0.35)
        return c
    return m


class Painter:
    def __init__(self, who):
        self.who = who
        self.r, self.p = rig(who)
        self.t = Tex(128, 128, 300 + ALLIES.index(who))
        self.rng = random.Random(310 + ALLIES.index(who))

    def paint(self):
        getattr(self, self.who)()
        return self.t

    # shared pieces

    def head(self, skin, hair, side_rows=2, back_rows=4, frame=False):
        skin_m, hair_m = noise(skin, 21, 3.2, 0.45), noise(hair, 22, 2.8, 1.3, 7.0)

        def m(face, x, y, w, h):
            if face == "up" or (face == "south" and y < back_rows) or (face == "north" and y < 1):
                return hair_m(face, x, y, w, h)
            if face in ("east", "west"):
                back = x >= w - 3 if face == "west" else x < 3
                if y < side_rows + (2 if back else 0):
                    return hair_m(face, x, y, w, h)
            if frame and face == "north" and (x in (0, 7) and y < 6):
                return hair_m(face, x, y, w, h)
            return skin_m(face, x, y, w, h)
        paint_cube(self.t, self.p["head"], m)

    def face(self, rows, pal):
        stamp_face(self.t, self.p["head"], "north", rows, pal)

    def limbs(self, skin, legs, boots=BOOT, sleeve=None):
        hand = noise(skin, 23, 3.0, 0.45)
        for side in ("right", "left"):
            paint_cube(self.t, self.p[f"{side}_upper"], sleeve or hand)

            def fore(face, x, y, w, h, s=sleeve):
                if face == "down" or (face != "up" and y >= h - 2) or s is None:
                    return hand(face, x, y, w, h)
                return s(face, x, y, w, h)
            paint_cube(self.t, self.p[f"{side}_fore"], fore)
        for leg in ("right_leg", "left_leg"):
            paint_cube(self.t, self.p[leg], lambda f, x, y, w, h: boots[2] if (f == "down" or (f != "up" and y >= h - 3))
                       else legs(f, x, y, w, h))
        boot_m = noise(boots, 24, 2.6, 0.8)
        for b in ("boot_r", "boot_l"):
            def bm(face, x, y, w, h):
                if face == "up":
                    return None
                if face != "down" and y == 0:
                    return boots[4]  # the boot's top edge
                if face == "north" and y == h - 1:
                    return boots[1]
                return boot_m(face, x, y, w, h)
            paint_cube(self.t, self.p[b], bm)

    def sleeves(self, material, cuff=None):
        for side in ("right", "left"):
            for k in ("sleeve_u", "sleeve_f"):
                paint_cube(self.t, self.p[f"{side}_{k}"], lambda f, x, y, w, h, m=material: None if f in ("up", "down") and k == "sleeve_f" else m(f, x, y, w, h))
            if f"{side}_cuff" in self.p:
                paint_cube(self.t, self.p[f"{side}_cuff"], lambda f, x, y, w, h, c=cuff or material:
                           None if f in ("up", "down") else c(f, x, y, w, h))

    def open_front(self, cube, material, gap=(3, 4), lapel=None, hem=None):
        """A garment open down the front: transparent middle columns on the north face (and the top), optional
        lapels either side of the gap and a hem row."""
        def m(face, x, y, w, h):
            if face == "down":
                return None
            if face in ("north", "up") and gap[0] <= x <= gap[1] and (face == "north" or y < 2):
                return None
            if lapel is not None and face == "north" and x in (gap[0] - 1, gap[1] + 1):
                return lapel
            if hem is not None and face in ("north", "south", "east", "west") and y == h - 1:
                return hem
            return material(face, x, y, w, h)
        paint_cube(self.t, cube, m)

    # the hunters

    def dean(self):
        p = self.p
        self.head(SKIN, DEAN_HAIR, side_rows=2, back_rows=4)
        hair_m = noise(DEAN_HAIR, 31, 3.0, 1.4, 8.0)
        paint_cube(self.t, p["hair"], lambda f, x, y, w, h: None if f == "down" or (f == "north" and y > 0 and x % 3)
                   else hair_m(f, x, y, w, h))
        for k, c in p.items():
            if k.startswith("spike_"):
                paint_cube(self.t, c, lambda f, x, y, w, h: DEAN_HAIR[4] if f == "up" else DEAN_HAIR[3])
        self.face([
            "........",
            "........",
            ".bb..bb.",
            ".wg..gw.",
            "........",
            "...nn...",
            "..mmmm..",
            "...ss...",
        ], {"b": DEAN_HAIR[1], "w": EYE_WHITE, "g": hexc("#4f7a3a"), "n": SKIN[2], "m": MOUTH, "s": SKIN[2]})
        shirt = plaid(DEAN_SHIRT, hexc("#541c1c"), hexc("#7a7a6a"), 32, 4)
        paint_cube(self.t, p["body"], shirt)
        paint_cube(self.t, p["shirt"], lambda f, x, y, w, h: None if f in ("up", "down") or f != "north" else
                   (hexc("#1a1a1a") if 3 <= x <= 4 and y < 2 else shirt(f, x, y, w, h)))
        leather = cloth(LEATHER, 33, 2.7, 0.15)

        def worn(face, x, y, w, h):
            c = leather(face, x, y, w, h)
            if fbm(x * 2, y * 2, 34 + len(face), 32, 32, 2, 6.0) > 0.68:
                c = mix(c, LEATHER[4], 0.5)  # scuffed
            return c
        self.open_front(p["jacket"], worn, gap=(2, 5), lapel=LEATHER[1])
        self.open_front(p["jacket_hem"], worn, gap=(2, 5), hem=LEATHER[0])
        paint_cube(self.t, p["collar"], lambda f, x, y, w, h: None if f in ("up", "down") or (f == "north" and 2 <= x <= 5)
                   else (LEATHER[3] if y == 0 else LEATHER[2]))
        self.limbs(SKIN, cloth(DENIM, 35, 2.8, 0.4), sleeve=worn)
        self.sleeves(worn, solid_ramp(LEATHER, 1))
        # The amulet: a little bronze horned face (gold glint) on a dark cord painted down the shirt.
        paint_cube(self.t, p["amulet"], lambda f, x, y, w, h: P.GOLD[4] if y == 0 else P.GOLD[2])
        for (x, y) in ((2, 0), (2, 1), (5, 0), (5, 1), (3, 2), (4, 2)):
            set_face_px(self.t, p["body"], "north", x, y, hexc("#141010"))

    def sam(self):
        p = self.p
        self.head(SKIN_TAN, SAM_HAIR, side_rows=3, back_rows=8, frame=True)
        hair_m = noise(SAM_HAIR, 41, 2.8, 1.4, 7.0)

        def long_hair(face, x, y, w, h):
            if face in ("up", "down") and self.rng.random() < 0.0:
                return None
            if face != "up" and y == h - 1 and (x * 5) % 3 == 0:
                return None  # uneven ends at the jaw
            return hair_m(face, x, y, w, h)
        paint_cube(self.t, p["hair"], lambda f, x, y, w, h: None if f == "down" or (f == "north") else hair_m(f, x, y, w, h))
        for k in ("hair_back", "hair_r", "hair_l"):
            paint_cube(self.t, p[k], long_hair)
        # The fringe swept to the side: a parting at x=5, longer on the right of the face.
        paint_cube(self.t, p["fringe"], lambda f, x, y, w, h: None if (f == "north" and (x == 5 or (y == 1 and x > 4)))
                   else hair_m(f, x, y, w, h))
        self.face([
            "........",
            "........",
            ".bb..bb.",
            ".wh..hw.",
            "........",
            "...nn...",
            "..mmmm..",
            "........",
        ], {"b": SAM_HAIR[1], "w": EYE_WHITE, "h": hexc("#6a6a3a"), "n": SKIN_TAN[2], "m": MOUTH})
        henley = cloth(HENLEY, 42, 2.6, 0.3)
        paint_cube(self.t, p["body"], henley)
        paint_cube(self.t, p["henley"], lambda f, x, y, w, h: None if f != "north" else
                   (HENLEY[1] if (x == 4 and 1 <= y <= 3) else HENLEY[5] if (x == 3 and y in (1, 3)) else henley(f, x, y, w, h)))
        flannel = plaid(Ramp("#22324d", "#2e4466", "#3b5580", "#4b6794", "#5d7aa6", "#738fb8"), hexc("#3a2414"), hexc("#c9b48a"), 43, 5)
        self.open_front(p["flannel"], flannel, gap=(3, 4), lapel=hexc("#2a3a58"))
        self.open_front(p["flannel_hem"], flannel, gap=(3, 4), hem=hexc("#1c2840"))
        paint_cube(self.t, p["collar"], lambda f, x, y, w, h: None if f in ("up", "down") or (f == "north" and 3 <= x <= 4)
                   else flannel(f, x, y, w, h))
        self.limbs(SKIN_TAN, cloth(DENIM, 44, 3.0, 0.4), sleeve=flannel)
        self.sleeves(flannel)

    def castiel(self):
        p = self.p
        self.head(SKIN, CAS_HAIR, side_rows=2, back_rows=4)
        hair_m = noise(CAS_HAIR, 51, 2.8, 1.6, 9.0)
        paint_cube(self.t, p["hair"], lambda f, x, y, w, h: None if f == "down" or (f == "north" and y > 0 and x % 2)
                   else hair_m(f, x, y, w, h))
        for k, c in p.items():
            if k.startswith("tuft_"):
                paint_cube(self.t, c, lambda f, x, y, w, h: CAS_HAIR[3] if f == "up" else CAS_HAIR[2])
        self.face([
            "........",
            "........",
            ".bb..bb.",
            ".wB..Bw.",
            "........",
            "...nn...",
            "...mm...",
            ".s.ss.s.",
        ], {"b": CAS_HAIR[1], "w": EYE_WHITE, "B": hexc("#3f8fe0"), "n": SKIN[2], "m": mix(MOUTH, SKIN[3], 0.3),
            "s": mix(SKIN[2], CAS_HAIR[2], 0.35)})
        # Stubble: a scatter of darker pixels on the jaw and the sides of the face.
        for (x, y) in ((1, 6), (6, 6), (2, 7), (5, 7), (0, 5), (7, 5)):
            set_face_px(self.t, p["head"], "north", x, y, mix(SKIN[2], CAS_HAIR[2], 0.45))
        shirt = cloth(WHITE, 52, 3.8, 0.2)
        paint_cube(self.t, p["body"], shirt)
        paint_cube(self.t, p["shirt"], lambda f, x, y, w, h: None if f != "north" else shirt(f, x, y, w, h))
        suit = cloth(SUIT, 53, 2.6, 0.25)
        self.open_front(p["suit"], suit, gap=(2, 5), lapel=SUIT[4])
        trench = cloth(TRENCH, 54, 3.0, 0.2)

        def coat(face, x, y, w, h):
            c = trench(face, x, y, w, h)
            if face in ("north", "south") and x in (1, w - 2) and face == "north":
                return mix(c, TRENCH[1], 0.35)  # the button line shadows
            return c
        self.open_front(p["coat"], coat, gap=(1, 6), lapel=TRENCH[4])
        paint_cube(self.t, p["coat_collar"], lambda f, x, y, w, h: None if f == "down" or (f in ("north", "up") and 2 <= x <= 5)
                   else (TRENCH[4] if y == 0 else TRENCH[3] if f == "north" else TRENCH[2]))
        paint_cube(self.t, p["belt"], lambda f, x, y, w, h: None if f in ("up", "down") or (f == "north" and 1 <= x <= 6)
                   else TRENCH[1])
        self.limbs(SKIN, cloth(SUIT, 55, 2.4, 0.2), boots=SHOE, sleeve=trench)
        self.sleeves(trench, solid_ramp(TRENCH, 2))
        tie = cloth(TIE, 56, 3.0, 0.3)
        for k in ("tie", "tie_knot"):
            paint_cube(self.t, p[k], lambda f, x, y, w, h: TIE[2] if y == h - 1 and k == "tie" else tie(f, x, y, w, h))
        for side in ("r", "l"):
            def skirt(face, x, y, w, h, s=side):
                if face in ("up", "down"):
                    return None
                # Open at the front: each half leaves the inside edge of its north face clear.
                if face == "north" and ((s == "r" and x >= w - 1) or (s == "l" and x <= 0)):
                    return None
                if y == h - 1:
                    return TRENCH[1]
                c = trench(face, x, y, w, h)
                if (x + (1 if s == "l" else 0)) % 3 == 0 and face in ("south", "east", "west"):
                    c = mix(c, TRENCH[1], 0.25)  # folds
                return c
            paint_cube(self.t, p["skirt_" + side], skirt)


def solid_ramp(ramp, i):
    return lambda face, x, y, w, h: ramp[i]


# --- animations -----------------------------------------------------------------------------

A = "animation.hunter_ally."
E_IO = "easeInOutSine"
E_OUT = "easeOutQuad"
E_IN = "easeInQuad"
E_BACK = "easeOutBack"

# The grip, held in front of him: the right hand closes on the Author's arm, the left on his shoulder.
GRIP = {
    "right_arm": [-70, 0, 4],
    "right_forearm": [-14, 0, 0],
    "left_arm": [-95, 0, -6],
    "left_forearm": [-12, 0, 0],
    "body": [8, 0, 0],
    "right_leg": [14, 0, 0],
    "left_leg": [-10, 0, 0],
}


def anims():
    f = AnimFile()
    idle = f.new(A + "idle", 4.0, loop=True)
    idle.pos("body", (0, [0, 0, 0]), (2.0, [0, -0.15, 0], E_IO), (4.0, [0, 0, 0], E_IO))
    idle.rot("head", (0, [0, 0, 0]), (2.0, [-3, 6, 0], E_IO), (4.0, [0, 0, 0], E_IO))
    for side, s in (("right", 1), ("left", -1)):
        idle.rot(f"{side}_arm", (0, [0, 0, 4 * s]), (2.0, [-3, 0, 6 * s], E_IO), (4.0, [0, 0, 4 * s], E_IO))
        idle.rot(f"{side}_forearm", (0, [-8, 0, 0]), (2.0, [-12, 0, 0], E_IO), (4.0, [-8, 0, 0], E_IO))
    for tail, s in (("coat_tail_r", 1), ("coat_tail_l", -1)):
        idle.rot(tail, (0, [0, 0, 0]), (2.0, [2, 0, 1.5 * s], E_IO), (4.0, [0, 0, 0], E_IO))

    appear = f.new(A + "appear", 2.6)
    # Steps forward out of the light: two strides from 10 px behind, head lifting, coat settling.
    appear.pos("root", (0, [0, 0, 10]), (1.6, [0, 0, 0], E_OUT), (2.6, [0, 0, 0]))
    appear.rot("right_leg", (0, [0, 0, 0]), (0.4, [-24, 0, 0], E_IO), (0.8, [20, 0, 0], E_IO), (1.2, [-16, 0, 0], E_IO),
               (1.6, [0, 0, 0], E_IO))
    appear.rot("left_leg", (0, [0, 0, 0]), (0.4, [22, 0, 0], E_IO), (0.8, [-20, 0, 0], E_IO), (1.2, [14, 0, 0], E_IO),
               (1.6, [0, 0, 0], E_IO))
    appear.rot("right_arm", (0, [0, 0, 0]), (0.4, [16, 0, 4], E_IO), (0.8, [-14, 0, 4], E_IO), (1.2, [10, 0, 4], E_IO),
               (1.6, [0, 0, 4], E_IO))
    appear.rot("left_arm", (0, [0, 0, 0]), (0.4, [-16, 0, -4], E_IO), (0.8, [14, 0, -4], E_IO), (1.2, [-10, 0, -4], E_IO),
               (1.6, [0, 0, -4], E_IO))
    appear.rot("head", (0, [20, 0, 0]), (0.9, [8, 0, 0], E_OUT), (1.8, [-4, -8, 0], E_IO), (2.6, [0, 0, 0], E_IO))
    appear.rot("body", (0, [6, 0, 0]), (1.6, [0, 0, 0], E_OUT))
    for tail, s in (("coat_tail_r", 1), ("coat_tail_l", -1)):
        appear.rot(tail, (0, [18, 0, 8 * s]), (0.8, [10, 0, 4 * s], E_OUT), (1.6, [-4, 0, -1 * s], E_IO), (2.6, [0, 0, 0], E_IO))

    hold = f.new(A + "hold", 1.2, hold=True)
    for bone, v in GRIP.items():
        reach = [v[0] * 1.15, v[1], v[2]] if bone.endswith("arm") else v
        hold.rot(bone, (0, [0, 0, 0]), (0.55, reach, E_OUT), (0.8, v, E_BACK), (1.2, v))
    hold.rot("head", (0, [0, 0, 0]), (0.6, [10, 0, 0], E_OUT), (1.2, [6, 0, 0], E_IO))
    hold.pos("root", (0, [0, 0, 0]), (0.55, [0, 0, -2.5], E_OUT), (1.2, [0, 0, -2], E_IO))

    def gripping(a):
        for bone, v in GRIP.items():
            a.rot(bone, (0, v), (a.length, v))
        a.pos("root", (0, [0, 0, -2]), (a.length, [0, 0, -2]))

    talk = f.new(A + "talk", 2.4)
    gripping(talk)
    talk.rot("head", (0, [6, 0, 0]), (0.3, [2, -6, 3], E_IO), (0.7, [8, 4, -2], E_IO), (1.1, [0, -3, 2], E_IO),
             (1.5, [7, 5, 0], E_IO), (1.9, [3, -2, 0], E_IO), (2.4, [6, 0, 0], E_IO))
    talk.rot("body", (0, GRIP["body"]), (0.7, [10, 3, 0], E_IO), (1.5, [7, -3, 0], E_IO), (2.4, GRIP["body"], E_IO))

    nod = f.new(A + "nod", 1.4)
    gripping(nod)
    nod.rot("head", (0, [6, 0, 0]), (0.35, [24, 0, 0], E_OUT), (0.7, [2, 0, 0], E_IO), (1.0, [16, 0, 0], E_IO),
            (1.4, [6, 0, 0], E_IO))

    fade = f.new(A + "fade", 2.0, hold=True)
    for bone, v in GRIP.items():
        fade.rot(bone, (0, v), (0.8, [0, 0, 0], E_IO), (2.0, [0, 0, 0]))
    fade.rot("right_arm", (0, GRIP["right_arm"]), (0.8, [0, 0, 4], E_IO), (2.0, [0, 0, 4]))
    fade.rot("left_arm", (0, GRIP["left_arm"]), (0.8, [0, 0, -4], E_IO), (2.0, [0, 0, -4]))
    fade.rot("head", (0, [6, 0, 0]), (0.8, [-6, -10, 0], E_IO), (1.4, [4, -14, 0], E_IO), (2.0, [-8, -12, 0], E_IO))
    fade.pos("root", (0, [0, 0, -2]), (1.2, [0, 0, 1.5], E_IO), (2.0, [0, 0.6, 1.5], E_IO))
    for tail, s in (("coat_tail_r", 1), ("coat_tail_l", -1)):
        fade.rot(tail, (0, [0, 0, 0]), (1.0, [10, 0, 6 * s], E_IO), (2.0, [16, 0, 9 * s], E_IO))
    return f


def generate():
    for who in ALLIES:
        painter = Painter(who)
        painter.r.write(GEO + f"hunter_{who}.geo.json")
        save(painter.paint(), "entity", f"hunter_{who}")
    anims().write(ANIM + "hunter_ally.animation.json")
