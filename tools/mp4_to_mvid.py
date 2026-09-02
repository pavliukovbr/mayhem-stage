#!/usr/bin/env python3
"""Converte video em .mvid: sequencia de JPEGs que o mod toca no telao.

Uso: python3 tools/mp4_to_mvid.py <entrada.mp4> <nome> [fps] [largura]
Grava em ~/Library/Application Support/minecraft/mayhem_videos/<nome>.mvid
Formato: 'MVID' u32 | fps u32 | count u32 | count x (len u32 + jpeg)
"""
import os, struct, subprocess, sys, tempfile, glob

src, name = sys.argv[1], sys.argv[2]
fps = int(sys.argv[3]) if len(sys.argv) > 3 else 20
width = int(sys.argv[4]) if len(sys.argv) > 4 else 512

outdir = os.path.expanduser("~/Library/Application Support/minecraft/mayhem_videos")
os.makedirs(outdir, exist_ok=True)
with tempfile.TemporaryDirectory() as td:
    subprocess.run(["ffmpeg","-v","quiet","-i",src,"-vf",f"fps={fps},scale={width}:-2",
                    "-q:v","5", f"{td}/f%05d.jpg"], check=True)
    frames = sorted(glob.glob(f"{td}/f*.jpg"))
    out = open(os.path.join(outdir, name + ".mvid"), "wb")
    out.write(struct.pack("<4sII", b"MVID", fps, len(frames)))
    total = 0
    for f in frames:
        d = open(f, "rb").read()
        out.write(struct.pack("<I", len(d))); out.write(d)
        total += len(d)
    out.close()
print(f"{name}.mvid: {len(frames)} frames @ {fps}fps, {total/2**20:.1f} MB")
