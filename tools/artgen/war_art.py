"""War, the Red Horseman (v0.11): a hard-faced man in a blood-red suit and black shirt, cropped dark hair and
stubble, the Ring of War on his left hand and a long sword (`sword`, in his right hand). Rides the red horse.

Clips (animation.war.*): the common set (horsemen_art.COMMON) plus sword_combo, sword_heavy, parried, rage_roar,
illusion_cast, mounted_sweep.
"""

import math

import horsemen_art as H
import steed_art as ST
from chuck_art import cloth, hair_mat, none_on, solid
from hell_art import WAR_GEM
from horsemen_art import E_BACK, E_IN, E_IO, E_OUT, P
from pixelkit import Ramp, hexc, mix, shade

SKIN = Ramp("#4e3226", "#7a5040", "#9c6a54", "#b6826a", "#c99a80", "#dab096")
HAIR = Ramp("#0c0907", "#16100c", "#211812", "#2c2018", "#382a20", "#46342a")
SUIT = Ramp("#2a0507", "#43090c", "#5e0f12", "#7a1619", "#931f20", "#ad2c2a")
SHIRT = Ramp("#060607", "#0d0d10", "#151519", "#1e1e23", "#28282e", "#33333a")
SHOE = Ramp("#050403", "#0c0907", "#15100c", "#1e1711", "#281f17", "#33281e")
STEEL = Ramp("#2a2d33", "#4a4f58", "#6c727c", "#9097a1", "#b7bdc5", "#e4e8ec")
GOLD = Ramp("#4b3108", "#7d5410", "#b07d18", "#d9a92b", "#f2d257", "#fff2a8")
GRIP = Ramp("#120707", "#1e0b0b", "#2b100f", "#3a1614", "#4a1d1a", "#5c2420")

FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhsshhhhhhsshhh",
    "ssssssssssssssss",
    "ssssssssssssssss",
    "sbbbbbssssbbbbbs",
    "sswipwsssswpiwss",
    "ssuuuussssuuuuss",
    "sssssssnnsssssss",
    "ssssssnNNnssssss",
    "SSssssssssssssSS",
    "SSSsssmmmmsssSSS",
    "SSSSSSSMMSSSSSSS",
    "SSSSSSSSSSSSSSSS",
    "sSSSSSSSSSSSSSSs",
]


class War(H.Horseman):
    id = "war"
    steed = "war"
    stride = 1.1
    walk_period = 1.1
    hunch = -2.0
    # Sword held low in the right hand, the blade angled forward and down; left hand loose, a little out.
    STANCE = {"right_arm": [-12, 0, 6], "right_forearm": [-22, 0, 0], "right_hand": [0, 0, 0],
              "left_arm": [4, 0, -6], "left_forearm": [-8, 0, 0]}
    ARMED_MOUNT = {"right_arm": [-30, 0, 18], "right_forearm": [-30, 0, 0], "right_hand": [20, 0, 0]}

    def build(self, m):
        glow = H.glow_faint(0.12)
        skin = H.skin_mat(SKIN, 3101, 3.0, 0.8)
        hair = hair_mat(3102, HAIR)
        stub = H.stubble(skin, HAIR, 3103, 0.4)
        legend = {"h": hair, "s": skin, "S": stub, "b": HAIR[1], "w": H.EYE_WHITE, "i": hexc("#3a2414"), "p": hexc("#0c0705"),
                  "u": mix(SKIN[2], hexc("#5a3a40"), 0.3), "n": SKIN[2], "N": SKIN[1], "m": hexc("#6e4036"), "M": SKIN[2]}
        H.head_cube(m, FACE, legend, H.head_sides(skin, hair, top=3, back=11, beard=stub, beard_from=11), glow=glow)
        # Cropped hair: a thin shell over the crown and back.
        m.cube("head", (-4, 30, -4), (8, 2, 8), none_on(("down",), lambda f, x, y, w, h: None if f == "north" and y > 1 else hair(f, x, y, w, h)),
               glow=glow, inflate=0.2, density=2, tag="hair")
        m.cube("head", (-0.5, 26.5, -4.5), (1, 2, 0.5), solid(SKIN[3]), glow=glow, density=2, tag="nose")
        m.cube("body", (-1.5, 23.5, -1.5), (3, 1, 3), skin, glow=glow, density=2, tag="neck")
        # Black shirt under a red suit jacket (V front, lapels, two buttons), jacket skirt below the waist.
        shirt = cloth(SHIRT, 3104, 2.6, 0.5, 0.1)
        suit = H.suit_mat(SUIT, 3105, 2.7, pinstripe=SUIT[1], period=5)
        m.cube("body", (-4, 12, -2), (8, 12, 4), shirt, glow=glow, density=2, tag="torso")
        front = H.jacket_front(suit, SUIT[4], shirt, gap=(5, 10), v_depth=11, buttons=GOLD[2], button_rows=(13, 17))
        m.cube("body", (-4, 12, -2), (8, 12, 4), none_on(("down",), front), glow=glow, inflate=0.3, density=2, tag="jacket")
        m.cube("body", (-4, 9.5, -2), (8, 2.5, 4), none_on(("up", "down"), lambda f, x, y, w, h: None if f == "north" and 7 <= x <= 8
                                                         else suit(f, x, y, w, h)), glow=glow, inflate=0.35, density=2, tag="jacket_skirt")
        m.cube("body", (-4, 22, -2), (8, 2, 4), none_on(("down",), lambda f, x, y, w, h: None if f in ("north", "up") and 4 <= x <= 11
                                                     else SHIRT[3]), glow=glow, inflate=0.45, density=2, tag="collar")
        m.cube("body", (-1.5, 18.5, -2.7), (3, 2.5, 0.5), none_on(("up", "down"), solid(SUIT[5])), glow=glow, density=2, tag="pocket_square")
        hand = H.skin_mat(SKIN, 3106, 3.1, 0.6)
        for side in ("right", "left"):
            H.arm_cubes(m, side, suit, hand, glow=glow, cuff=lambda f, x, y, w, h: SHIRT[3], sleeve_inflate=0.25)
            H.leg_cubes(m, side, H.suit_mat(SUIT, 3107, 2.5), lambda f, x, y, w, h: SHOE[2 + (1 if f == "up" else 0)], glow=glow)
        H.ring_cube(m, "left", WAR_GEM[4], GOLD[3])
        # The sword: grip in the fist, a black-and-gold guard, a long bright blade pointing forward.
        m.bone("sword", (-6, 12.25, 0), "right_hand", rotation=(32, 0, 0))
        edge_glow = lambda f, x, y, w, h, col: mix(col, hexc("#ff2a1a"), 0.5) if f in ("up", "down") else shade(col, 0.15)
        m.cube("sword", (-6.5, 11.75, -1.5), (1, 1, 4.5), lambda f, x, y, w, h: GRIP[2 + (y + x) % 2], density=2, tag="grip")
        m.cube("sword", (-6.75, 11.5, 3.0), (1.5, 1.5, 1), solid(GOLD[3]), glow=H.glow_faint(0.3), density=2, tag="pommel")
        m.cube("sword", (-7, 9.75, -2.5), (2, 5, 1), lambda f, x, y, w, h: GOLD[4] if y in (0, h - 1) else SHIRT[3], glow=H.glow_faint(0.2),
               density=2, tag="guard")

        def blade(f, x, y, w, h):
            if f in ("up", "down"):
                return STEEL[5]
            c = STEEL[3.6 - 0.8 * abs(y - (h - 1) / 2) / max(1, h / 2)]
            if f in ("east", "west") and y == h // 2:
                return STEEL[2]  # fuller
            return c
        m.cube("sword", (-6.25, 11.5, -20.5), (0.5, 1.5, 18), blade, glow=edge_glow, density=2, tag="blade")
        m.cube("sword", (-6.25, 11.75, -21.5), (0.5, 1, 1), solid(STEEL[4]), glow=edge_glow, density=2, tag="tip")

    # --- clips -----------------------------------------------------------------------------------

    def clips(self):
        st = self.stance()
        A = self.clip

        # intro: head down, rolls his neck, lifts the sword and points it at you; a slight smile of the shoulders.
        c = A("intro", 3.2)
        c.key(0, P(st, {"head": [22, 0, 0], "body": [6, 0, 0]}))
        c.key(0.8, P(st, {"head": [16, 0, 14], "body": [4, 0, 0]}), E_IO)
        c.key(1.3, P(st, {"head": [10, 0, -14], "body": [2, 0, 0]}), E_IO)
        c.key(1.8, P(st, {"head": [-4, -10, 0]}), E_IO)
        point = P(st, {"right_arm": [-88, -12, 6], "right_forearm": [0, 0, 0], "right_hand": [55, 0, 0], "head": [-2, -8, 0],
                       "body": [0, -12, 0], "left_arm": [6, 0, -14], "right_leg": [-10, 0, 0], "left_leg": [8, 0, 0]})
        c.key(2.4, point, E_BACK)
        c.key(3.2, point, E_IO)
        c.done()

        # transition: plants the sword point-down, breathes in fury, rips it out.
        c = A("transition", 2.4)
        plant = P(st, {"right_arm": [-40, 0, 0], "right_forearm": [-50, 0, 0], "right_hand": [58, 0, 0], "body": [18, 0, 0],
                       "head": [-12, 0, 0], "left_arm": [-30, 0, -10], "left_forearm": [-40, 0, 0], "@rider": [0, -1, 0]})
        c.key(0, st)
        c.key(0.5, plant, E_IN)
        c.key(1.2, P(plant, {"body": [10, 0, 0], "head": [-28, 0, 0], "left_arm": [-10, 0, -40]}), E_IO)
        rip = P(st, {"right_arm": [-150, 0, 10], "right_forearm": [-10, 0, 0], "right_hand": [-20, 0, 0], "body": [-10, 0, 0],
                     "head": [-15, 0, 0]})
        c.key(1.6, rip, E_OUT)
        c.key(2.4, st, E_IO)
        c.done()

        # sword_combo: a flat cut right-to-left, a backhand, an overhead chop.
        c = A("sword_combo", 1.7)
        cut_a = P(st, {"right_arm": [-80, 70, 30], "right_forearm": [-20, 0, 0], "right_hand": [-30, 0, 0], "body": [0, 35, 0],
                       "left_arm": [-30, 0, -30], "right_leg": [10, 0, 0], "left_leg": [-14, 0, 0]})
        cut_b = P(st, {"right_arm": [-85, -70, 10], "right_forearm": [-5, 0, 0], "right_hand": [-30, 0, 0], "body": [6, -35, 0],
                       "left_arm": [10, 0, -30], "right_leg": [-20, 0, 0], "left_leg": [12, 0, 0]})
        cut_c_up = P(st, {"right_arm": [-170, 0, -6], "right_forearm": [-30, 0, 0], "right_hand": [-20, 0, 0], "body": [-12, 0, 0],
                          "left_arm": [-160, 0, 10], "left_forearm": [-30, 0, 0], "head": [-10, 0, 0]})
        cut_c = P(st, {"right_arm": [-45, 0, 0], "right_forearm": [0, 0, 0], "right_hand": [-10, 0, 0], "body": [22, 0, 0],
                       "left_arm": [-40, 0, 10], "left_forearm": [-10, 0, 0], "right_leg": [-26, 0, 0], "left_leg": [18, 0, 0],
                       "left_shin": [20, 0, 0], "@rider": [0, -1, -2]})
        c.key(0, st)
        c.key(0.2, cut_a, E_OUT)
        c.key(0.45, cut_b, E_IN)
        c.key(0.7, P(cut_b, {"body": [4, -40, 0]}), E_OUT)
        c.key(0.95, cut_c_up, E_IO)
        c.key(1.15, cut_c, E_IN)
        c.key(1.35, cut_c)
        c.key(1.7, st, E_IO)
        c.done()

        # sword_heavy: both hands, a long wind-up behind the head, a slam into the ground, heave it free.
        c = A("sword_heavy", 1.9)
        wind = P(st, {"right_arm": [-175, 20, 0], "right_forearm": [-40, 0, 0], "right_hand": [-30, 0, 0],
                      "left_arm": [-170, -20, 0], "left_forearm": [-45, 0, 0], "body": [-18, 0, 0], "head": [-10, 0, 0],
                      "right_leg": [8, 0, 0], "left_leg": [-20, 0, 0], "left_shin": [10, 0, 0]})
        slam = P(st, {"right_arm": [-30, 8, 0], "right_forearm": [0, 0, 0], "right_hand": [20, 0, 0],
                      "left_arm": [-35, -10, 0], "left_forearm": [0, 0, 0], "body": [32, 0, 0], "head": [-20, 0, 0],
                      "right_leg": [-30, 0, 0], "left_leg": [25, 0, 0], "right_shin": [30, 0, 0], "left_shin": [25, 0, 0],
                      "@rider": [0, -2.5, -2]})
        c.key(0, st)
        c.key(0.7, wind, E_OUT)
        c.key(0.85, P(wind, {"body": [-22, 0, 0]}), E_IO)
        c.key(1.05, slam, E_IN)
        c.key(1.45, P(slam, {"body": [28, 0, 0]}))
        c.key(1.9, st, E_IO)
        c.done()

        # parried: the blade is knocked aside, he reels back off balance.
        c = A("parried", 1.1)
        reel = P(st, {"right_arm": [-60, -40, 70], "right_forearm": [-10, 0, 0], "right_hand": [30, 0, 0], "body": [-16, 10, 0],
                      "head": [-20, 15, 0], "left_arm": [-40, 0, -50], "right_leg": [20, 0, 0], "left_leg": [-25, 0, 0],
                      "@rider": [0, 0, 3]})
        c.key(0, P(st, {"right_arm": [-80, 0, 0]}))
        c.key(0.15, reel, E_OUT)
        c.key(0.6, P(reel, {"body": [-10, 6, 0], "@rider": [0, 0, 4]}), E_IO)
        c.key(1.1, st, E_IO)
        c.done()

        # rage_roar: chest out, arms flung wide, head back.
        c = A("rage_roar", 2.2)
        gather = P(st, {"body": [20, 0, 0], "head": [20, 0, 0], "right_arm": [-20, 0, -10], "left_arm": [-20, 0, 10],
                        "right_forearm": [-60, 0, 0], "left_forearm": [-60, 0, 0], "@rider": [0, -1, 0]})
        roar = P(st, {"body": [-16, 0, 0], "head": [-35, 0, 0], "right_arm": [-40, 0, 75], "left_arm": [-40, 0, -75],
                      "right_forearm": [-20, 0, 0], "left_forearm": [-20, 0, 0], "right_leg": [0, 0, 8], "left_leg": [0, 0, -8]})
        c.key(0, st)
        c.key(0.5, gather, E_IO)
        c.key(0.8, roar, E_BACK)
        for i, t in enumerate((1.0, 1.2, 1.4, 1.6)):
            c.key(t, P(roar, {"head": [(-4 if i % 2 else 4), (3 if i % 2 else -3), 0], "body": [(-2 if i % 2 else 2), 0, 0]}), E_IO)
        c.key(2.2, st, E_IO)
        c.done()

        # illusion_cast: lowers the sword, lifts his left hand to his temple, then sweeps it out at the crowd.
        c = A("illusion_cast", 1.7)
        temple = P(st, {"left_arm": [-120, 30, 20], "left_forearm": [-90, 0, 0], "head": [10, 20, 0], "body": [4, 10, 0]})
        sweep = P(st, {"left_arm": [-85, 0, -60], "left_forearm": [-5, 0, 0], "left_hand": [0, 0, -20], "head": [-6, -20, 0],
                       "body": [0, -20, 0]})
        c.key(0, st)
        c.key(0.45, temple, E_IO)
        c.key(0.8, P(temple, {"head": [14, 25, 0]}), E_IO)
        c.key(1.05, sweep, E_OUT)
        c.key(1.3, P(sweep, {"left_arm": [-85, 0, -80]}), E_IO)
        c.key(1.7, st, E_IO)
        c.done()

        # mounted_sweep: at the gallop, the sword drawn far back on his right, then swept low across in front.
        back = {"right_arm": [-60, 80, 60], "right_forearm": [-10, 0, 0], "right_hand": [-20, 0, 0], "body": [0, 30, 0],
                "head": [0, -20, 0]}
        across = {"right_arm": [-70, -60, 20], "right_forearm": [0, 0, 0], "right_hand": [-30, 0, 0], "body": [16, -30, 0],
                  "head": [0, 10, 0]}
        self.mounted_action("mounted_sweep", 1.5, lambda t: ST.gallop_pose(t, 0.6, 1.1), [
            (0, {}, None), (0.45, back, None), (0.75, across, None), (0.95, across, None), (1.5, {}, None)], lean=12)

        self.death()

    def charge_raise(self):
        return {"right_arm": [-165, 0, 6], "right_forearm": [-15, 0, 0], "right_hand": [-30, 0, 0], "head": [-18, 0, 0]}

    def charge_level(self):
        return {"right_arm": [-88, 6, 6], "right_forearm": [0, 0, 0], "right_hand": [50, 0, 0], "body": [14, 0, 0]}


def generate():
    War().generate()
