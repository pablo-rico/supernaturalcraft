"""WAV -> OGG Vorbis, deterministically.

Minecraft only positions MONO sounds. ffmpeg's built-in `vorbis` encoder refuses mono ("only supports 2 channels"),
so the preferred encoder is GStreamer's libvorbis (`vorbisenc`), which writes true mono. If GStreamer is missing we
fall back to ffmpeg in stereo (the sound then plays unpositioned, fine for music and narration).

oggmux picks a random bitstream serial number, so every page's serial is rewritten to a value derived from the file
name and each page CRC recomputed: the same WAV always yields a byte-identical OGG.
"""

import os
import struct
import subprocess
import zlib

GST = "/Library/Frameworks/GStreamer.framework/Versions/1.0/bin/gst-launch-1.0"
FFMPEG = "/opt/homebrew/bin/ffmpeg"


def encoder():
    """'gstreamer' (mono) or 'ffmpeg' (stereo)."""
    return "gstreamer" if os.path.exists(GST) else "ffmpeg"


def _crc_table():
    table = []
    for i in range(256):
        r = i << 24
        for _ in range(8):
            r = ((r << 1) ^ 0x04C11DB7) if r & 0x80000000 else (r << 1)
        table.append(r & 0xFFFFFFFF)
    return table


_TABLE = _crc_table()


def ogg_crc(data):
    """Ogg's CRC-32: polynomial 0x04C11DB7, init 0, unreflected, no final xor."""
    crc = 0
    t = _TABLE
    for b in data:
        crc = ((crc << 8) & 0xFFFFFFFF) ^ t[((crc >> 24) ^ b) & 0xFF]
    return crc


def restamp(path, serial):
    """Rewrites the stream serial of every page and fixes each page's CRC."""
    with open(path, "rb") as f:
        data = bytearray(f.read())
    pos = 0
    pages = 0
    while pos < len(data):
        if data[pos:pos + 4] != b"OggS":
            raise ValueError(f"{path}: lost page sync at {pos}")
        nseg = data[pos + 26]
        body = sum(data[pos + 27:pos + 27 + nseg])
        size = 27 + nseg + body
        struct.pack_into("<I", data, pos + 14, serial)
        struct.pack_into("<I", data, pos + 22, 0)
        struct.pack_into("<I", data, pos + 22, ogg_crc(data[pos:pos + size]))
        pos += size
        pages += 1
    with open(path, "wb") as f:
        f.write(data)
    return pages


def encode(wav, ogg, name, quality=0.3):
    """Encodes and restamps; returns the encoder used."""
    os.makedirs(os.path.dirname(ogg), exist_ok=True)
    if encoder() == "gstreamer":
        cmd = [GST, "-q", "filesrc", "location=" + wav, "!", "wavparse", "!", "audioconvert", "!",
               "vorbisenc", f"quality={quality}", "!", "oggmux", "!", "filesink", "location=" + ogg]
        used = "gstreamer"
    else:
        cmd = [FFMPEG, "-y", "-loglevel", "error", "-i", wav, "-c:a", "vorbis", "-strict", "-2", "-ac", "2",
               "-q:a", str(round(quality * 10)), ogg]
        used = "ffmpeg"
    subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    restamp(ogg, zlib.crc32(name.encode()) & 0x7FFFFFFF)
    return used
