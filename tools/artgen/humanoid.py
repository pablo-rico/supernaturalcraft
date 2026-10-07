"""The shared humanoid rig: possessed people, and Lucifer's vessel.

Proportions match the vanilla player (8-px head, 12-px torso and limbs), so the creatures read
at the same scale as the player standing next to them.
"""

from geomodel import Rig, box


def humanoid(identifier, tex_w=64, tex_h=64, slim_arms=False):
    r = Rig(identifier, tex_w, tex_h)
    aw = 3 if slim_arms else 4
    r.bone("root", (0, 0, 0))
    body = r.bone("body", (0, 12, 0), "root")
    head = r.bone("head", (0, 24, 0), "body")
    # Bedrock +X is the entity's LEFT (GeckoLib mirrors X when baking), as in the vanilla
    # Bedrock player geometry where rightArm sits at x = -5.
    right_arm = r.bone("right_arm", (-4 - aw / 2 - 0.5, 22, 0), "body")
    left_arm = r.bone("left_arm", (4 + aw / 2 + 0.5, 22, 0), "body")
    right_leg = r.bone("right_leg", (-2, 12, 0), "root")
    left_leg = r.bone("left_leg", (2, 12, 0), "root")
    parts = {
        "body": box(body, (-4, 12, -2), (8, 12, 4), tag="body"),
        "head": box(head, (-4, 24, -4), (8, 8, 8), tag="head"),
        "right_arm": box(right_arm, (-4 - aw, 12, -2), (aw, 12, 4), tag="right_arm"),
        "left_arm": box(left_arm, (4, 12, -2), (aw, 12, 4), tag="left_arm"),
        "right_leg": box(right_leg, (-4.1, 0, -2), (4, 12, 4), tag="right_leg"),
        "left_leg": box(left_leg, (0.1, 0, -2), (4, 12, 4), tag="left_leg"),
    }
    return r, parts
