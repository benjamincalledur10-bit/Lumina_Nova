#!/usr/bin/env python3
"""Summarize every A/B run; do not select the fastest sample or mix fixture densities."""
import argparse
import json
from pathlib import Path
from statistics import median

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("results", nargs="+", type=Path, help="Exported results.json files")
args = parser.parse_args()
print("| Perfil / escena | FPS OFF, mediana [mín–máx] | FPS ON, mediana [mín–máx] | ON/OFF |")
print("| --- | --- | --- | --- |")
for path in args.results:
    result = json.loads(path.read_text())
    for scene in ("enclosed", "exposed"):
        groups = {}
        for mode in ("off", "on"):
            samples = [sample for sample in result["samples"] if sample["name"].startswith(f"{scene}-{mode}-")]
            if not samples:
                raise ValueError(f"Missing {scene}/{mode} in {path}")
            groups[mode] = [sample["fps"] for sample in samples]
        if len(groups["on"]) != len(groups["off"]):
            raise ValueError(f"Unpaired runs in {path}")
        off, on = groups["off"], groups["on"]
        label = f"{path.parent.name} / {scene}"
        print(f"| {label} | {median(off):.1f} [{min(off):.1f}–{max(off):.1f}] | "
              f"{median(on):.1f} [{min(on):.1f}–{max(on):.1f}] | {median(on)/median(off):.2f}× |")
