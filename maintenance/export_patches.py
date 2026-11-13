#!/usr/bin/env python3
"""Export only the committed fork delta, grouped for upstream maintenance."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]


def git(*args, cwd=ROOT):
    return subprocess.check_output(["git", *args], cwd=cwd)


def covered(path, prefixes):
    return any(path == prefix or path.startswith(prefix + "/") for prefix in prefixes)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--source", default="HEAD", help="Committed source ref; working edits are never exported")
    parser.add_argument("--groups", help="Comma-separated groups; default: all")
    parser.add_argument("--verify", action="store_true", help="Reapply to a temporary upstream worktree and compare files")
    args = parser.parse_args()
    source = git("rev-parse", "--verify", f"{args.source}^{{commit}}").decode().strip()
    # Read the baseline and file inventory from the same commit as the source.
    config = json.loads(git("show", f"{source}:maintenance/upstream.json"))
    base = git("rev-parse", "--verify", f'{config["commit"]}^{{commit}}').decode().strip()
    try:
        tagged_base = git("rev-parse", "--verify", f'{config["tag"]}^{{commit}}').decode().strip()
    except subprocess.CalledProcessError:
        raise SystemExit(f'Fetch the recorded official tag {config["tag"]} from {config["repository"]} first') from None
    if tagged_base != base:
        raise SystemExit("Recorded upstream tag and commit do not match")
    if subprocess.run(["git", "merge-base", "--is-ancestor", base, source], cwd=ROOT).returncode:
        raise SystemExit("Recorded upstream commit must be an ancestor of the source")
    changed = git("diff", "--no-renames", "--name-only", base, source).decode().splitlines()
    groups = config["groups"]
    prefixes = [path for group in groups for path in group["paths"]]
    unexpected = [path for path in changed if not covered(path, prefixes)]
    if unexpected:
        raise SystemExit("Unclassified changes; update the baseline/inventory before exporting: " + ", ".join(unexpected))
    for path in changed:
        if sum(covered(path, group["paths"]) for group in groups) != 1:
            raise SystemExit("Change belongs to multiple patch groups: " + path)

    constants = git("show", f"{source}:patches/src/main/kotlin/app/crimera/patches/twitter/utils/Constants.kt").decode()
    versions = re.findall(r'version\s*=\s*"([^"]+)"', constants)
    if versions != config["verified_x_versions"]:
        raise SystemExit("X targets changed: inspect native code and update fixtures before recording new verified versions")
    libraries = git("show", f"{source}:gradle/libs.versions.toml").decode()
    patcher = re.search(r'^morphe-patcher\s*=\s*"([^"]+)"', libraries, re.MULTILINE)
    if not patcher or patcher[1] != config["verified_patcher_version"]:
        raise SystemExit("Patcher version changed: check API compatibility before updating the recorded version")

    selected = set(args.groups.split(",")) if args.groups else {group["name"] for group in groups}
    if not selected or not selected <= {group["name"] for group in groups}:
        raise SystemExit("Unknown or empty group selection")
    output = args.output.resolve()
    if output.is_relative_to(ROOT):
        raise SystemExit("Export outside the repository to keep generated patches out of the source tree")
    output.mkdir(parents=True, exist_ok=True)
    if any(output.iterdir()):
        raise SystemExit("Output directory must be empty; existing exports are never overwritten")

    manifest = {"upstream": config["repository"], "upstream_tag": config["tag"],
                "upstream_commit": base, "source_commit": source, "patches": []}
    for group in groups:
        if group["name"] not in selected:
            continue
        data = git("diff", "--binary", "--full-index", "--no-renames", "--no-color", "--no-ext-diff",
                   "--src-prefix=a/", "--dst-prefix=b/", base, source, "--", *group["paths"])
        if not data:
            continue
        (output / group["file"]).write_bytes(data)
        manifest["patches"].append({"group": group["name"], "file": group["file"],
                                    "sha256": hashlib.sha256(data).hexdigest()})
    if not manifest["patches"]:
        raise SystemExit("No custom changes to export")
    (output / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
    (output / "series").write_text("".join(patch["file"] + "\n" for patch in manifest["patches"]))

    if args.verify:
        patch_files = [str(output / patch["file"]) for patch in manifest["patches"]]
        selected_prefixes = [path for group in groups if group["name"] in selected for path in group["paths"]]
        expected = [path for path in changed if covered(path, selected_prefixes)]
        with tempfile.TemporaryDirectory(prefix="piko-patch-replay-") as temporary:
            checkout = Path(temporary) / "checkout"
            subprocess.run(["git", "worktree", "add", "--detach", str(checkout), base], cwd=ROOT, check=True,
                           stdout=subprocess.DEVNULL)
            try:
                subprocess.run(["git", "apply", "--check", "--index", *patch_files], cwd=checkout, check=True)
                subprocess.run(["git", "apply", "--index", *patch_files], cwd=checkout, check=True)
                actual = git("diff", "--cached", "--no-renames", "--name-only", cwd=checkout).decode().splitlines()
                if actual != expected:
                    raise SystemExit("Replayed file list differs from selected source changes")
                for path in expected:
                    original = subprocess.run(["git", "show", f"{source}:{path}"], cwd=ROOT, capture_output=True)
                    replayed = subprocess.run(["git", "show", f":{path}"], cwd=checkout, capture_output=True)
                    if (original.returncode == 0) != (replayed.returncode == 0) or original.stdout != replayed.stdout:
                        raise SystemExit("Replayed content differs: " + path)
                if selected == {group["name"] for group in groups}:
                    if git("write-tree", cwd=checkout).strip() != git("rev-parse", f"{source}^{{tree}}").strip():
                        raise SystemExit("Replayed tree differs from the committed source")
            finally:
                subprocess.run(["git", "worktree", "remove", "--force", str(checkout)], cwd=ROOT, check=True)
        print("Patch replay verified against committed source")
    print(f'Exported {len(manifest["patches"])} patches from {config["tag"]} to {source[:12]}: {output}')


if __name__ == "__main__":
    main()
