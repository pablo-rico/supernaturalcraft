#!/usr/bin/env python3
"""Generates the mod's own sounds -- the Author (v0.10, sounds/chuck) and the Four Horsemen (v0.11, sounds/horsemen) and the Archangel Michael (v0.12, sounds/michael):
pure-stdlib synthesis to WAV, then OGG Vorbis.

Every sound is deterministic (one random.Random per file, seeded from its name) and goes through
synth.finish (DC block, trim, fades, normalise) before encoding. OGGs land in
src/main/resources/assets/supernaturalcraft/sounds/chuck/; ChuckAssetData points the sound events at them.

Mono through GStreamer's libvorbis when available (positional in game); otherwise stereo via ffmpeg (see oggenc).
Usage:  python3 tools/soundgen/generate.py [name ...]     (no names = everything)
"""

import os
import random
import sys
import time
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import music  # noqa: E402
import oggenc  # noqa: E402
import horsemen_sfx as hx  # noqa: E402
import michael_sfx as mx  # noqa: E402
import sfx  # noqa: E402
from synth import finish, stats, write_wav  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
SOUND_ROOT = os.path.join(ROOT, "src", "main", "resources", "assets", "supernaturalcraft", "sounds")
OUT = os.path.join(SOUND_ROOT, "chuck")
WAV = os.environ.get("SN_SOUND_TMP",
                     "/private/tmp/claude-501/-Users-pabloricoal-IdeaProjects/48bfac8a-fc08-4858-bb40-e6e315583706/scratchpad/art2/wav")

SR = 32000          # effects
MUSIC_SR = 22050    # the long piece


def _keys(sr, seed):
    rng = random.Random(seed)
    return [sfx.type_key(sr, rng, i % 4) for i in range(6)]


# name -> (builder(sr, rng), peak dBFS, sample rate)
SOUNDS = {}


def sound(name, peak=-1.5, sr=SR, sub="chuck"):
    def deco(fn):
        if name in SOUNDS:
            raise ValueError("duplicate sound " + name)
        SOUNDS[name] = (fn, peak, sr, sub)
        return fn
    return deco


for _i in range(4):
    sound(f"type_{_i + 1}", -1.5)(lambda sr, rng, i=_i: sfx.type_key(sr, rng, i))
for _i in range(2):
    sound(f"carriage_{_i + 1}", -1.5)(lambda sr, rng, i=_i: sfx.carriage(sr, rng, i))
    sound(f"snap_{_i + 1}", -1.0)(lambda sr, rng, i=_i: sfx.snap(sr, rng, i))
    sound(f"key_impact_{_i + 1}", -1.0)(lambda sr, rng, i=_i: sfx.key_impact(sr, rng, i))
    sound(f"page_tear_{_i + 1}", -2.0)(lambda sr, rng, i=_i: sfx.page_tear(sr, rng, i))
    sound(f"write_{_i + 1}", -3.0)(lambda sr, rng, i=_i: sfx.write(sr, rng, i))
    sound(f"hurt_{_i + 1}", -2.0)(lambda sr, rng, i=_i: sfx.hurt(sr, rng, i))
    sound(f"npc_hum_{_i + 1}", -3.0)(lambda sr, rng, i=_i: sfx.hum(sr, rng, i))
sound("bell", -2.0)(sfx.margin_bell)
sound("backspace", -1.5)(sfx.backspace)
sound("rewrite", -1.5)(lambda sr, rng: sfx.rewrite(sr, rng, _keys(sr, 7)))
sound("laugh", -3.0)(sfx.laugh)
sound("reveal", -1.5)(sfx.reveal)
sound("crack", -1.0)(sfx.crack)
sound("erase", -2.0)(sfx.erase)
sound("echo", -1.5)(sfx.echo)
sound("approve", -2.0)(sfx.approve)
sound("ally_arrive", -1.5)(sfx.ally_arrive)
sound("pen_write", -3.0)(sfx.pen_write)
sound("music", -4.0, MUSIC_SR)(music.compose)


# --- v0.11 the Four Horsemen: sounds/horsemen/<name>.ogg (HorsemenAssetData points the events at them) ----------

def horsemen(name, peak=-1.5):
    return sound(name, peak, SR, "horsemen")


for _i in range(3):
    horsemen(f"war_clash_{_i + 1}", -1.0)(lambda sr, rng, i=_i: hx.sword_clash(sr, rng, i))
    horsemen(f"pestilence_cough_{_i + 1}", -1.5)(lambda sr, rng, i=_i: hx.cough(sr, rng, i))
for _i in range(2):
    for _who, _fn in (("war", hx.war_voice), ("famine", hx.famine_voice), ("pestilence", hx.pestilence_voice),
                      ("death", hx.death_voice)):
        horsemen(f"{_who}_ambient_{_i + 1}", -3.0)(lambda sr, rng, i=_i, fn=_fn: fn(sr, rng, "ambient", i))
        if _who != "pestilence":
            horsemen(f"{_who}_hurt_{_i + 1}", -2.0)(lambda sr, rng, i=_i, fn=_fn: fn(sr, rng, "hurt", i))
    horsemen(f"death_tick_{_i + 1}", -3.0)(lambda sr, rng, i=_i: hx.clock_tick(sr, rng, i))
    horsemen(f"reaper_whisper_{_i + 1}", -3.0)(lambda sr, rng, i=_i: hx.reaper_whisper(sr, rng, i))
    horsemen(f"steed_neigh_{_i + 1}", -1.5)(lambda sr, rng, i=_i: hx.neigh(sr, rng, i))
    horsemen(f"steed_gallop_{_i + 1}", -2.0)(lambda sr, rng, i=_i: hx.gallop(sr, rng, i))
horsemen("pestilence_hurt_1", -2.0)(lambda sr, rng: hx.pestilence_voice(sr, rng, "hurt"))
for _who, _fn in (("war", hx.war_voice), ("famine", hx.famine_voice), ("pestilence", hx.pestilence_voice), ("death", hx.death_voice)):
    horsemen(f"{_who}_death", -1.5)(lambda sr, rng, fn=_fn: fn(sr, rng, "death"))
horsemen("war_rage", -1.0)(lambda sr, rng: hx.war_voice(sr, rng, "rage"))
horsemen("war_parry", -1.5)(hx.parry)
horsemen("famine_devour", -1.5)(lambda sr, rng: hx.famine_voice(sr, rng, "devour"))
horsemen("famine_hunger", -2.0)(lambda sr, rng: hx.famine_voice(sr, rng, "hunger"))
horsemen("death_reap", -1.5)(hx.reap)
horsemen("death_limbo_bell", -1.0)(hx.limbo_bell)
horsemen("death_world_flip", -1.0)(hx.world_flip)
horsemen("reaper_attack", -2.0)(hx.reaper_attack)
horsemen("fly_buzz", -4.0)(hx.fly_buzz)


# --- v0.12 the Archangel Michael: sounds/michael/<name>.ogg (MichaelAssetData points the events at them) ----------

def michael(name, peak=-1.5):
    return sound(name, peak, SR, "michael")


for _i in range(2):
    michael(f"michael_ambient_{_i + 1}", -4.0)(lambda sr, rng, i=_i: mx.ambient(sr, rng, i))
    michael(f"michael_hurt_{_i + 1}", -2.0)(lambda sr, rng, i=_i: mx.hurt(sr, rng, i))
    michael(f"michael_wings_{_i + 1}", -2.0)(lambda sr, rng, i=_i: mx.wings(sr, rng, i))
    michael(f"host_ambient_{_i + 1}", -4.0)(lambda sr, rng, i=_i: mx.host_ambient(sr, rng, i))
    michael(f"host_hurt_{_i + 1}", -2.0)(lambda sr, rng, i=_i: mx.host_hurt(sr, rng, i))
    michael(f"host_march_{_i + 1}", -3.0)(lambda sr, rng, i=_i: mx.host_march(sr, rng, i))
michael("michael_death", -1.0)(mx.death)
michael("michael_smite", -1.0)(mx.smite)
michael("michael_ask_yes", -2.0)(mx.ask_yes)
michael("michael_trumpet", -1.0)(mx.trumpet_fanfare)
michael("michael_transform", -1.0)(mx.transform)
michael("michael_halo_break", -1.0)(mx.halo_break)
michael("michael_lance_throw", -1.5)(mx.lance_throw)
michael("michael_lance_impact", -1.0)(mx.lance_impact)
michael("michael_lance_recall", -1.5)(mx.lance_recall)
michael("michael_feather_storm", -1.5)(mx.feather_storm)
michael("michael_choir", -1.5)(mx.choir_chord)
michael("michael_dive", -1.5)(mx.dive)
michael("host_death", -1.5)(mx.host_death)
michael("host_shield", -1.5)(mx.host_shield)
michael("general_armor_ward", -2.0)(mx.armor_ward)
michael("grace_flight", -3.0)(mx.grace_flight)


def build(name):
    fn, peak, sr, sub = SOUNDS[name]
    rng = random.Random(zlib.crc32(name.encode()))
    x = fn(sr, rng)
    long_piece = name == "music"
    x = finish(x, sr, peak, fade_in=0.5 if long_piece else 0.0008, fade_out=4.0 if long_piece else 0.04,
               trim=not long_piece)
    os.makedirs(WAV, exist_ok=True)
    wav = os.path.join(WAV, name + ".wav")
    write_wav(wav, x, sr)
    ogg = os.path.join(SOUND_ROOT, sub, name + ".ogg")
    used = oggenc.encode(wav, ogg, name if sub == "chuck" else sub + "/" + name, quality=0.3 if not long_piece else 0.25)
    dur, pk, rms, dc = stats(x, sr)
    return used, dur, pk, rms, dc, os.path.getsize(ogg)


def main():
    names = sys.argv[1:] or list(SOUNDS)
    print(f"encoder: {oggenc.encoder()} ({'mono' if oggenc.encoder() == 'gstreamer' else 'stereo'})")
    for n in names:
        t0 = time.time()
        used, dur, pk, rms, dc, size = build(n)
        print(f"  {n:14s} {dur:6.2f}s peak {pk:.3f} rms {rms:.3f} dc {dc:+.4f} {size / 1024:7.1f} KB  ({time.time() - t0:.1f}s)")
    print(f"wrote {len(names)} sounds")


if __name__ == "__main__":
    main()
