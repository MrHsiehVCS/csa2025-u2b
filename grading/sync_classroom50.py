#!/usr/bin/env python3
"""Copy the current official tests and grading code into your Classroom50 config.

Run from this project folder, after changing any test (teachers only):

    python3 grading/sync_classroom50.py PATH/TO/CLASSROOM50-CONFIG --classroom CLASSROOM

It (re)writes PATH/TO/CLASSROOM50-CONFIG/CLASSROOM/autograders/ASSIGNMENT/ with:

    autograder.py      grading/autograder.py
    score.py           grading/score.py
    official-tests/    a fresh copy of src/test/java/ (old copy removed first)
    bundle-info.json   classroom/assignment names and when this sync ran

Other files already in that folder are left alone. It prints which test files
were added, changed, or removed since the last sync. It does not commit or
push: do that yourself in the config repo, then use "Regrade all" if needed.

Options:
    --classroom NAME   classroom folder in the config repo. Can be left out when
                       the config repo has exactly one classroom folder.
    --assignment NAME  assignment folder (default: csa2025-u2b)

Python 3 standard library only.
"""

import argparse
import datetime
import json
import shutil
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import autograder  # noqa: E402

REPO = HERE.parent
ASSIGNMENT_DEFAULT = "csa2025-u2b"
GRADING_FILES = ["autograder.py", "score.py"]


class UsageError(Exception):
    pass


def guess_classroom(config):
    """The only classroom folder in the config repo, preferring ones with autograders/."""
    dirs = [d for d in Path(config).iterdir() if d.is_dir() and not d.name.startswith(".")]
    with_autograders = [d for d in dirs if (d / "autograders").is_dir()]
    for candidates in (with_autograders, dirs):
        if len(candidates) == 1:
            return candidates[0].name
    names = ", ".join(sorted(d.name for d in dirs)) or "(none)"
    raise UsageError(f"Can't tell which classroom to use; pass --classroom NAME. "
                     f"Folders found in {config}: {names}")


def changes(old_dir, new_dir):
    """(added, changed, removed) file lists between two test folders."""
    old, new = autograder.list_files(old_dir), autograder.list_files(new_dir)
    changed = sorted(p for p in old & new
                     if (Path(old_dir) / p).read_bytes() != (Path(new_dir) / p).read_bytes())
    return sorted(new - old), changed, sorted(old - new)


def source_commit(repo):
    head = autograder.git_head(repo)
    if not head:
        return None
    dirty = subprocess.run(["git", "status", "--porcelain", "--", "src/test", "grading"],
                           cwd=repo, capture_output=True, text=True).stdout.strip()
    return head + (" (plus uncommitted changes)" if dirty else "")


def sync(config, classroom=None, assignment=ASSIGNMENT_DEFAULT, repo=REPO, now=None):
    """Write the bundle. Returns (bundle path, (added, changed, removed))."""
    config, repo = Path(config), Path(repo)
    if not config.is_dir():
        raise UsageError(f"Classroom50 config folder not found: {config}")
    official_src = repo / autograder.STUDENT_JAVA_TESTS
    if not autograder.list_files(official_src):
        raise UsageError(f"No tests found in {official_src}")
    classroom = classroom or guess_classroom(config)
    bundle = config / classroom / "autograders" / assignment
    bundle.mkdir(parents=True, exist_ok=True)

    official_dst = bundle / autograder.OFFICIAL_DIR
    diff = changes(official_dst, official_src)
    shutil.rmtree(official_dst, ignore_errors=True)
    shutil.copytree(official_src, official_dst,
                    ignore=shutil.ignore_patterns(*autograder.IGNORED_NAMES))
    for name in GRADING_FILES:
        shutil.copy2(repo / "grading" / name, bundle / name)

    now = now or datetime.datetime.now(datetime.timezone.utc)
    info = {
        "classroom": classroom,
        "assignment": assignment,
        "synced-at": now.strftime("%Y-%m-%dT%H:%M:%SZ"),
        "source-commit": source_commit(repo),
    }
    (bundle / autograder.BUNDLE_INFO).write_text(json.dumps(info, indent=2) + "\n",
                                                 encoding="utf-8")
    return bundle, diff


def main(argv=None):
    ap = argparse.ArgumentParser(
        description="Copy the official tests and grading code into a Classroom50 config repo.",
        epilog="Example: python3 grading/sync_classroom50.py ~/classroom50-config "
               "--classroom csa-2025")
    ap.add_argument("config", type=Path, help="your local checkout of the Classroom50 config repo")
    ap.add_argument("--classroom", help="classroom folder name (default: the only one there)")
    ap.add_argument("--assignment", default=ASSIGNMENT_DEFAULT,
                    help="assignment folder name (default: %(default)s)")
    args = ap.parse_args(argv)
    try:
        bundle, (added, changed, removed) = sync(args.config, args.classroom, args.assignment)
    except UsageError as exc:
        print(f"Error: {exc}", file=sys.stderr)
        return 1
    print(f"Synced the official tests and autograder to {bundle}")
    if added or changed or removed:
        for label, paths in (("added", added), ("changed", changed), ("removed", removed)):
            for p in paths:
                print(f"  {label}: {p}")
    else:
        print("  (test files unchanged since the last sync)")
    print("Next: commit and push the config repo. To re-score submissions already made, "
          "use Regrade all.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
