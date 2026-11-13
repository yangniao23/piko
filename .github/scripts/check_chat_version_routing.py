#!/usr/bin/env python3
"""Check the production chat version hooks using the installed JDK, without an APK."""
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[2]
production = root / "extensions/twitter/src/main/java/app/morphe/extension/twitter/patches/clientversion/SpoofClientVersionPatch.java"
test = root / ".github/tests/SpoofClientVersionTest.java"

with tempfile.TemporaryDirectory(prefix="piko-chat-test-") as output:
    subprocess.run(["javac", "--release", "17", "-d", output, str(production), str(test)], check=True)
    subprocess.run(["java", "-cp", output, "SpoofClientVersionTest"], check=True)
