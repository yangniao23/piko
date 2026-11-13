#!/usr/bin/env python3
"""Apply both custom patches to the pinned public Piko APK; never sign or install it."""
import argparse
import json
import os
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument("--morphe", type=Path, required=True)
parser.add_argument("--apk", type=Path, required=True)
parser.add_argument("--mpp", type=Path, required=True)
parser.add_argument("--output", type=Path, required=True)
args = parser.parse_args()
root = Path(__file__).resolve().parents[2]
output = args.output.resolve()
output.mkdir(parents=True, exist_ok=True)
result_file = output / "result.json"
patched = output / "patched.apk"
tool = args.morphe.resolve()
subprocess.run([
    "java", "-Xmx3g", "-jar", str(tool), "patch", str(args.apk.resolve()),
    "-p", str(args.mpp.resolve()), "--exclusive", "-e", "Cellular data saver",
    "-e", "Spoof client version", "--unsigned", "--validate", "--bytecode-mode", "STRIP_FAST",
    "-o", str(patched), "-r", str(result_file),
], check=True)
result = json.loads(result_file.read_text())
if not result["success"]:
    raise SystemExit("Custom patch application failed")
test = root / ".github/tests/VerifyCellularPatch.java"
subprocess.run(["javac", "--release", "17", "-cp", str(tool), "-d", str(output), str(test)], check=True)
subprocess.run(["java", "-Xmx2g", "-cp", os.pathsep.join((str(tool), str(output))),
                "VerifyCellularPatch", str(patched)], check=True)
