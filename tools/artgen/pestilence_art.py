"""Pestilence, the Pale Horseman (v0.11): a sickly, sallow little man with thin grey hair combed over, round
wire glasses over red-rimmed eyes, a runny nose and sores; a rumpled olive tweed jacket over a mustard sweater vest
and a brown tie; a crook-handled cane (`walking_cane`) in his right hand and a handkerchief (`handkerchief`) in his
left; the Ring of Pestilence on his left hand. Rides the pale horse.

Clips (animation.pestilence.*): the common set plus cough, cough_cone, swarm_call, cloud_cast, mounted_spray.
"""

import math

import horsemen_art as H
import steed_art as ST
from chuck_art import cloth, h01, hair_mat, none_on, solid
from hell_art import PESTILENCE_GEM
from horsemen_art import E_BACK, E_IN, E_IO, E_OUT, P
from pixelkit import Ramp, fbm, hexc, mix, shade

SKIN = Ramp("#4a4c32", "#6c7150", "#8c926d", "#a6ab86", "#bcc09d", "#cfd2b2")
HAIR = Ramp("#4a4844", "#636058", "#7c786f", "#949088", "#aaa69e", "#c0bcb4")
TWEED = Ramp("#26281a", "#363a26", "#474c33", "#585e40", "#6b714f", "#7e8460")
VEST = Ramp("#4a3a0e", "#685316", "#86701f", "#a08a2c", "#b8a03e", "#cdb756")
TIE = Ramp("#1e120a", "#2c1b10", "#3c2616", "#4d311d", "#5e3d25", "#70492d")
TROUSERS = Ramp("#1b150f", "#271f17", "#342a1f", "#423528", "#504132", "#5f4d3c")
SHOE = Ramp("#0e0906", "#1a110b", "#261910", "#332216", "#402b1c", "#4e3523")
WIRE = Ramp("#4a3f22", "#6e5e32", "#927d44", "#b39b5a", "#cdb574", "#e2cd94")
CANE = Ramp("#1c0f07", "#2b170b", "#3c2110", "#4e2c16", "#61381d", "#744426")
SILVER = Ramp("#3a3f48", "#626a76", "#8c95a1", "#b5bdc7", "#d9dfe6", "#ffffff")

FACE = [
    "hhhhhcsssschhhhh",
    "hhhsssssssssssshh"[:16],
    "hssssssssssssssh",
    "ssssrrrrrrrrssss",
    "ssssssssssssssss",
    "sbbbbssssssbbbbs",
    "sggggggssggggggs",
    "sgRwipggggpiwRgs",
    "sgRRRRgssgRRRRgs",
    "sggggggnnggggggs",
    "ssssssnNNnssssss",
    "sssssssddsssssss",
    "sssssmmmmmmsssss",
    "sssssssMMsssssss",
    "ssooosssssssooss",
    "ssssssssssssssss",
]


class Pestilence(H.Horseman):
    id = "pestilence"
    steed = "pestilence"
    stride = 0.8
    walk_period = 1.3
    hunch = 9.0
    breathe = 1.6
    STANCE = {"right_arm": [-16, 0, 6], "right_forearm": [-22, 0, 0], "left_arm": [-6, 0, -4], "left_forearm": [-30, 0, 0]}
    ARMED_MOUNT = {"right_arm": [8, 0, 0], "right_forearm": [6, 0, 0]}

    def arm_swing(self, side):
        return 8.0 if side == "right" else 18.0

    def build(self, m):
        glow = H.glow_faint(0.15)
        skin = H.skin_mat(SKIN, 3301, 3.0, 0.9, blotch=hexc("#6a5a3a"), blotch_p=0.05, sores=0.08)
        sore_glow = lambda f, x, y, w, h, col: mix(col, hexc("#b8e04a"), 0.6) if col[1] > col[0] + 30 and col[0] > 150 else shade(col, 0.15)
        hair = hair_mat(3302, HAIR)
        legend = {"h": hair, "c": hair, "s": skin, "r": SKIN[2], "b": HAIR[2], "g": lambda f, x, y, w, h: WIRE[3 + (x + y) % 2],
                  "R": mix(SKIN[3], hexc("#c45050"), 0.55), "w": hexc("#e0dccc"), "i": hexc("#5a6a3a"), "p": hexc("#141410"),
                  "n": SKIN[2], "N": SKIN[1], "d": hexc("#b8c48a"), "m": hexc("#4a2a22"), "M": hexc("#8a6a5a"),
                  "o": hexc("#8a3a26")}
        H.head_cube(m, FACE, legend, H.head_sides(skin, hair, top=3, back=10, temple=7, sideburn=7), glow=sore_glow)
        # Combed-over grey hair, wire temples of the glasses, a red nose.
        m.cube("head", (-4, 30.5, -4), (8, 1.5, 8), none_on(("down",), lambda f, x, y, w, h: None if (f == "up" and 6 <= x <= 9 and y < 10)
                                                        or (f == "north" and y > 0) else hair(f, x, y, w, h)),
               glow=glow, inflate=0.2, density=2, tag="hair")
        for s in (-1, 1):
            m.cube("head", (4.05 * s - 0.1, 28.5, -4), (0.2, 0.4, 5), solid(WIRE[3]), glow=glow, density=4, tag="glasses_arm")
        m.cube("head", (-3.5, 27.3, -4.35), (7, 2.5, 0.2), lambda f, x, y, w, h: None if f != "north" else
               ((WIRE[4] if (y in (0, h - 1) or x in (0, 5, 6, 7, 8, 13, 14)) and not (6 <= x <= 7 and y > 1) else
                 (0xdd, 0xee, 0xee, 70))), glow=glow, density=2, faces=("north",), tag="glasses")
        m.cube("head", (-0.5, 26.3, -4.6), (1, 2.2, 0.6), solid(mix(SKIN[3], hexc("#b06050"), 0.45)), glow=glow, density=2, tag="nose")
        m.cube("body", (-1.5, 23.5, -1.5), (3, 1, 3), skin, glow=glow, density=2, tag="neck")
        # Shirt, mustard vest, brown tie; the rumpled tweed jacket hanging open.
        shirt = cloth(Ramp("#6e6a5a", "#8a8672", "#a6a28c", "#bebaa4", "#d2cebc", "#e2dece"), 3303, 3.4, 0.4, 0.1)
        m.cube("body", (-4, 12, -2), (8, 12, 4), shirt, glow=glow, density=2, tag="torso")
        tie = cloth(TIE, 3304, 2.8, 0.4, 0.15)

        def vest(face, x, y, w, h):
            n = fbm(x * 2, y * 2 + len(face), 3305, 64, 64, 2, 10.0)
            c = VEST[2.6 + (n - 0.5) * 0.9 + (0.25 if (x + y) % 2 else 0)]
            if face == "north":
                mid = (w - 1) / 2
                if y < 8 and abs(x - mid) <= (8 - y) * 0.45 + 0.5:
                    return tie(face, x, y, w, h) if abs(x - mid) <= 1 else shirt(face, x, y, w, h)
            return c
        m.cube("body", (-4, 13, -2), (8, 11, 4), none_on(("down", "up"), vest), glow=glow, inflate=0.2, density=2, tag="vest")
        tweed = lambda f, x, y, w, h: TWEED[2.7 + (fbm(x * 3, y * 3 + len(f), 3306, 64, 64, 2, 14.0) - 0.5) * 1.4
                                            + (0.4 if (x * 2 + y) % 5 == 0 else 0) - (0.3 if (x + 2 * y) % 7 == 0 else 0)]

        def jacket(face, x, y, w, h):
            if face == "north":
                mid = (w - 1) / 2
                if abs(x - mid) <= 3.0:
                    return None  # hangs open
                if abs(x - mid) <= 4.0 and y < 14:
                    return TWEED[4]  # lapel
                if y in (15, 16) and (2 <= x <= 3 or w - 4 <= x <= w - 3):
                    return TWEED[1]
            if face == "up" and 3 <= x <= w - 4:
                return None
            return tweed(face, x, y, w, h)
        m.cube("body", (-4, 9, -2), (8, 15, 4), none_on(("down",), jacket), glow=glow, inflate=0.45, density=2, tag="jacket")
        hand = H.skin_mat(SKIN, 3307, 3.0, 0.9, sores=0.06)
        for side in ("right", "left"):
            H.arm_cubes(m, side, tweed, hand, glow=glow, cuff=lambda f, x, y, w, h: shirt(f, x, y, w, h), sleeve_inflate=0.35)
            H.leg_cubes(m, side, cloth(TROUSERS, 3308, 2.7, 0.4, 0.1), lambda f, x, y, w, h: SHOE[2 + (1 if f == "up" else 0)], glow=glow)
        H.ring_cube(m, "left", PESTILENCE_GEM[4], SILVER[3])
        # The cane: a dark wooden shaft from the fist to the ground, a crook over the knuckles, a brass ferrule.
        m.bone("walking_cane", (-6, 12.25, 0), "right_hand", rotation=(38, 0, 0))
        wood = lambda f, x, y, w, h: CANE[2.8 + (0.8 if x == 0 else 0) + (fbm(x, y, 3309, 32, 64, 2, 8.0) - 0.5)]
        m.cube("walking_cane", (-6.5, -0.5, -0.5), (1, 12.5, 1), wood, glow=glow, density=2, tag="cane_shaft")
        m.cube("walking_cane", (-6.5, 12, -0.5), (1, 1, 3), wood, glow=glow, density=2, tag="cane_crook")
        m.cube("walking_cane", (-6.5, 10.5, 1.5), (1, 1.5, 1), wood, glow=glow, density=2, tag="cane_crook_tip")
        m.cube("walking_cane", (-6.6, -0.6, -0.6), (1.2, 1, 1.2), solid(WIRE[3]), glow=glow, density=2, tag="ferrule")
        # A crumpled handkerchief in the left fist.
        m.bone("handkerchief", (6, 12, -1), "left_hand")
        m.cube("handkerchief", (5, 10, -2.5), (2, 2.5, 2), lambda f, x, y, w, h: mix(hexc("#e8e4d4"), hexc("#b8c48a"), 0.2 if (x + y) % 3 else 0),
               glow=glow, density=2, tag="hanky")

    # --- clips -----------------------------------------------------------------------------------

    def cough_fit(self, c, t0, base, n=3, gap=0.22, strength=1.0):
        """A run of coughs from t0: each a sharp forward jerk of the torso and head, the hanky to the mouth."""
        hanky = {"left_arm": [-60, 30, 10], "left_forearm": [-90, 0, 0], "left_hand": [0, 0, 0]}
        t = t0
        for i in range(n):
            k = strength * (1 - i * 0.15)
            c.key(t, P(base, hanky, {"body": [3 * k, 0, 0], "head": [-6 * k, 0, 0]}), E_IO)
            c.key(t + gap * 0.35, P(base, hanky, {"body": [16 * k, 0, 0], "head": [18 * k, 0, 0], "@rider": [0, -0.6 * k, -0.5 * k]}), E_OUT)
            t += gap
        return t

    def clips(self):
        st = self.stance()
        A = self.clip

        c = A("cough", 1.5)
        c.key(0, st)
        t = self.cough_fit(c, 0.15, st, 4, 0.25)
        c.key(1.5, st, E_IO)
        c.done()

        # cough_cone: a long wheezing inhale leaning back, then one huge cough thrown forward, arms flung.
        c = A("cough_cone", 1.8)
        inhale = P(st, {"body": [-18, 0, 0], "head": [-28, 0, 0], "right_arm": [-30, 0, 30], "left_arm": [-30, 0, -30],
                        "right_forearm": [-30, 0, 0], "left_forearm": [-30, 0, 0], "@rider": [0, 0.3, 0.5]})
        hack = P(st, {"body": [34, 0, 0], "head": [26, 0, 0], "right_arm": [10, 0, 30], "left_arm": [10, 0, -30],
                      "right_leg": [-18, 0, 0], "left_leg": [14, 0, 0], "left_shin": [14, 0, 0], "@rider": [0, -1.2, -1.5]})
        c.key(0, st)
        c.key(0.7, inhale, E_IO)
        c.key(0.85, P(inhale, {"head": [-32, 0, 0]}), E_IO)
        c.key(1.0, hack, E_IN)
        c.key(1.25, P(hack, {"body": [30, 0, 0], "head": [20, 0, 0]}), E_IO)
        c.key(1.8, st, E_IO)
        c.done()

        # swarm_call: lifts the cane high like a conductor's baton and circles it; the left hand opens to the sky.
        c = A("swarm_call", 2.2)
        up = P(st, {"body": [-8, 0, 0], "head": [-24, 0, 0], "right_arm": [-165, 0, -10], "right_forearm": [-10, 0, 0],
                    "left_arm": [-110, 0, -40], "left_forearm": [-20, 0, 0], "left_hand": [0, 0, -20]})
        c.key(0, st)
        c.key(0.5, up, E_OUT)
        for i in range(4):
            a = i * math.pi / 2
            c.key(0.7 + i * 0.25, P(up, {"right_arm": [8 * math.sin(a), 0, 12 * math.cos(a)], "right_hand": [15 * math.cos(a), 0, 0]}), E_IO)
        c.key(1.8, P(up, {"head": [-28, 0, 0]}), E_IO)
        c.key(2.2, st, E_IO)
        c.done()

        # cloud_cast: sweeps the cane low in a wide arc in front of him; the hanky hand flicks.
        c = A("cloud_cast", 1.9)
        right = P(st, {"body": [14, 35, 0], "right_arm": [-60, 50, 50], "right_forearm": [-10, 0, 0], "right_hand": [40, 0, 0],
                       "left_arm": [-20, 0, -30]})
        left = P(st, {"body": [16, -35, 0], "right_arm": [-70, -60, 0], "right_forearm": [-10, 0, 0], "right_hand": [40, 0, 0],
                      "left_arm": [-60, 0, -60], "left_forearm": [-20, 0, 0]})
        c.key(0, st)
        c.key(0.5, right, E_IO)
        c.key(1.2, left, E_IO)
        c.key(1.4, P(left, {"left_hand": [-40, 0, 0]}), E_OUT)
        c.key(1.9, st, E_IO)
        c.done()

        # intro: dabs his nose with the handkerchief, pushes his glasses up, coughs, leans on the cane and smiles.
        c = A("intro", 3.4)
        dab = P(st, {"left_arm": [-70, 30, 12], "left_forearm": [-95, 0, 0], "head": [6, -6, 0]})
        glasses = P(st, {"left_arm": [-80, 40, 0], "left_forearm": [-110, 0, 0], "left_hand": [-20, 0, 0], "head": [-6, 0, 0]})
        c.key(0, st)
        c.key(0.5, dab, E_IO)
        c.key(0.7, P(dab, {"head": [10, -10, 0]}), E_IO)
        c.key(0.9, P(dab, {"head": [4, -4, 0]}), E_IO)
        c.key(1.4, glasses, E_IO)
        c.key(1.6, P(glasses, {"head": [-10, 0, 0]}), E_IO)
        t = self.cough_fit(c, 1.9, st, 2, 0.25, 0.8)
        lean = P(st, {"body": [12, 0, -4], "head": [-14, 8, 6], "right_arm": [-26, 0, 4]})
        c.key(2.9, lean, E_IO)
        c.key(3.4, lean)
        c.done()

        # transition: a racking fit that bends him double, then he straightens with a wet laugh.
        c = A("transition", 2.6)
        bent = P(st, {"body": [38, 0, 0], "head": [10, 0, 0], "right_leg": [-12, 0, 0], "left_leg": [-12, 0, 0], "right_shin": [18, 0, 0],
                      "left_shin": [18, 0, 0], "@rider": [0, -1, 0]})
        c.key(0, st)
        t = self.cough_fit(c, 0.1, bent, 4, 0.22, 1.2)
        laugh = P(st, {"body": [-10, 0, 0], "head": [-22, 0, 0], "left_arm": [-30, 0, -30], "right_arm": [-30, 0, 20]})
        c.key(1.5, laugh, E_IO)
        for i, tt in enumerate((1.7, 1.9, 2.1)):
            c.key(tt, P(laugh, {"body": [(-3 if i % 2 else 2), 0, 0], "head": [(-4 if i % 2 else 3), 0, 0]}), E_IO)
        c.key(2.6, st, E_IO)
        c.done()

        # mounted_spray: at the gallop he turns to his left and coughs a long spray over the horse's shoulder.
        turn = {"body": [12, -30, 0], "head": [10, -30, 0], "left_arm": [-60, 30, 10], "left_forearm": [-90, 0, 0]}
        spray = {"body": [24, -40, 0], "head": [24, -36, 0], "left_arm": [-30, 0, -40], "left_forearm": [-20, 0, 0]}
        self.mounted_action("mounted_spray", 1.7, lambda t: ST.gallop_pose(t, 0.6, 0.9), [
            (0, {}, None), (0.4, turn, None), (0.6, spray, None), (0.8, P(spray, {"body": [-6, 0, 0]}), None), (1.0, spray, None),
            (1.2, P(spray, {"body": [-6, 0, 0]}), None), (1.7, {}, None)], lean=8)

        self.death()

    def charge_raise(self):
        return {"right_arm": [-160, 0, 0], "right_forearm": [-10, 0, 0], "right_hand": [-30, 0, 0], "head": [-15, 0, 0]}

    def charge_level(self):
        return {"right_arm": [-90, 0, 10], "right_forearm": [0, 0, 0], "right_hand": [60, 0, 0], "body": [14, 0, 0]}


def generate():
    Pestilence().generate()
