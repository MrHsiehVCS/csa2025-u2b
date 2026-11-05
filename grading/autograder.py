#!/usr/bin/env python3
"""Classroom50 autograder for this assignment (teachers only; students use score.py).

Classroom50 runs this file from the teacher's config repo, at
CLASSROOM/autograders/ASSIGNMENT/autograder.py, with the working directory set
to the student's checkout. The folder it sits in (the "bundle") also holds:

    score.py           the same score script students run in their Codespace
    official-tests/    the official copy of src/test/java/ (written by sync_classroom50.py)
    official-pom.xml   the official pom.xml
    bundle-info.json   classroom/assignment names and when the bundle was synced

What it does:
  1. Compares the student's src/test/ files and build files (pom.xml, .mvn/,
     mvnw, mvnw.cmd) with the official ones and notes any file that was added,
     changed, or deleted.
  2. Replaces src/test/ with the official tests and pom.xml with the official
     one, deletes .mvn/, mvnw and mvnw.cmd (student-controlled build settings),
     and deletes target/ so nothing compiled from the student's copies can be
     reused. Symbolic links are removed, never followed.
  3. Runs the runner's own `mvn` (never the repo's mvnw) and scores each part
     with the bundle's score.py (same numbers students see).
  4. Writes ./result.json (Classroom50 result v1, one row per part),
     ./release-body.md (readable summary), and summary= to $GITHUB_OUTPUT.

Exit code 0 whenever grading ran end to end - including compile failures,
failing tests, and a checkout the official files can't be installed into
(that scores 0 with a note). Non-zero only for problems on the teacher/runner
side (bundle missing its official files, Maven not installed), which
Classroom50 treats as an infrastructure error instead of giving the student a 0.

For testing:  python3 autograder.py [--repo DIR] [--bundle DIR] [--mvn CMD]
Everything grading uses - official tests, official pom.xml, and score.py - comes
from the --bundle folder (default: this script's folder), so pointing --bundle
at a synced bundle tests exactly that bundle.
Python 3 standard library only.
"""

import argparse
import datetime
import filecmp
import importlib.util
import json
import os
import shutil
import subprocess
import sys
from pathlib import Path

BUNDLE_DEFAULT = Path(__file__).resolve().parent
sys.path.insert(0, str(BUNDLE_DEFAULT))
import score  # noqa: E402  (score.py next to this file; run() uses the bundle's, see load_score)

SCHEMA = "classroom50/result/v1"
OFFICIAL_DIR = "official-tests"        # mirrors the repo's src/test/java/
OFFICIAL_POM = "official-pom.xml"      # the repo's pom.xml
BUNDLE_INFO = "bundle-info.json"
STUDENT_TESTS = Path("src") / "test"   # everything under here is replaced
STUDENT_JAVA_TESTS = STUDENT_TESTS / "java"
POM = "pom.xml"
# Student-controlled Maven settings/launchers: deleted before grading (none are official).
BUILD_EXTRAS = [".mvn", "mvnw", "mvnw.cmd"]
DIFFER_WARNING = ("⚠ your test or build files differ from the official ones; "
                  "graded with the official copies")
IGNORED_NAMES = {"__pycache__", ".DS_Store"}

# result.json fields Classroom50's docs don't say how to fill. Each is read from
# the first environment variable that is set; see grading/TEACHERS.md.
ENV_FIELDS = {
    "classroom": ["CLASSROOM50_CLASSROOM"],
    "assignment": ["CLASSROOM50_ASSIGNMENT"],
    "submission": ["CLASSROOM50_SUBMISSION", "GITHUB_REPOSITORY"],
    "commit": ["CLASSROOM50_COMMIT"],   # then git HEAD of the checkout, then GITHUB_SHA
    "release": ["CLASSROOM50_RELEASE", "GITHUB_REF_NAME"],
    "review": ["CLASSROOM50_REVIEW"],
}


class InfrastructureError(Exception):
    """Something on the teacher/runner side is broken; not the student's fault."""


class SetupError(Exception):
    """The student's checkout can't take the official files; graded as 0 with a note."""


def load_score(bundle):
    """The score module from the bundle's own score.py (this file's copy if it's the same file)."""
    path = Path(bundle) / "score.py"
    if not path.is_file():
        raise InfrastructureError(f"No score.py in {bundle}. Run grading/sync_classroom50.py "
                                  "and push the Classroom50 config repo.")
    if path.resolve() == Path(score.__file__).resolve():
        return score
    spec = importlib.util.spec_from_file_location("bundle_score", path)
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module  # dataclasses look their module up here
    spec.loader.exec_module(module)
    return module


def list_files(root):
    """Relative paths ('/'-separated) of all files under root. Never follows symbolic links:
    a link (to a file or a folder) is listed as a single entry. A root that is a link -> empty."""
    root = Path(root)
    if root.is_symlink() or not root.is_dir():
        return set()
    found = set()
    for dirpath, dirnames, filenames in os.walk(root):  # followlinks=False
        here = Path(dirpath)
        dirnames[:] = [d for d in dirnames if d not in IGNORED_NAMES]
        links = [d for d in dirnames if (here / d).is_symlink()]
        for name in filenames + links:
            if name not in IGNORED_NAMES:
                found.add((here / name).relative_to(root).as_posix())
    return found


def same_file(a, b):
    """True when a and b are both regular files (not links) with the same bytes."""
    a, b = Path(a), Path(b)
    if a.is_symlink() or b.is_symlink() or not (a.is_file() and b.is_file()):
        return False
    return filecmp.cmp(a, b, shallow=False)


def diff_trees(expected_root, actual_root, expected_prefix=""):
    """Compare two folders. Returns (added, changed, removed): sorted paths relative to
    actual_root that are only in actual, in both but different, only in expected.
    expected_prefix is put in front of expected paths (expected_root mirrors a subfolder)."""
    expected = {expected_prefix + p: Path(expected_root) / p for p in list_files(expected_root)}
    actual = {p: Path(actual_root) / p for p in list_files(actual_root)}
    added = sorted(actual.keys() - expected.keys())
    removed = sorted(expected.keys() - actual.keys())
    changed = sorted(p for p in expected.keys() & actual.keys()
                     if not same_file(expected[p], actual[p]))
    return added, changed, removed


def compare_tests(official_java, student_tests):
    """Compare the official tests with the student's whole src/test/ folder.

    official_java mirrors src/test/java/. Returns (added, modified, deleted),
    each a sorted list of paths relative to the repo (e.g. src/test/java/u2bpp/X.java).
    """
    added, modified, deleted = diff_trees(official_java, student_tests, expected_prefix="java/")
    prefix = STUDENT_TESTS.as_posix() + "/"
    added = [prefix + p for p in added]
    if Path(student_tests).is_symlink():
        added.insert(0, STUDENT_TESTS.as_posix())
    return added, [prefix + p for p in modified], [prefix + p for p in deleted]


def compare_build_files(official_pom, repo):
    """Compare the student's build files with the official ones. Same return shape as
    compare_tests: pom.xml changed or deleted; anything in BUILD_EXTRAS added."""
    repo = Path(repo)
    pom = repo / POM
    added, modified, deleted = [], [], []
    if not (pom.exists() or pom.is_symlink()):
        deleted.append(POM)
    elif not same_file(official_pom, pom):
        modified.append(POM)
    for name in BUILD_EXTRAS:
        path = repo / name
        if path.is_dir() and not path.is_symlink():
            added += sorted(f"{name}/{p}" for p in list_files(path)) or [name + "/"]
        elif path.exists() or path.is_symlink():
            added.append(name)
    return added, modified, deleted


def difference_notes(added, modified, deleted):
    if not (added or modified or deleted):
        return []
    notes = [DIFFER_WARNING]
    for label, paths in (("changed", modified), ("deleted", deleted), ("added", added)):
        notes.extend(f"  {label}: {p}" for p in paths)
    return notes


def remove(path):
    """Delete a file, folder, or symbolic link. A link is unlinked, never followed."""
    path = Path(path)
    if path.is_symlink() or (path.exists() and not path.is_dir()):
        path.unlink()
    elif path.is_dir():
        shutil.rmtree(path)  # doesn't follow links inside the folder either


def install_official_files(official_java, official_pom, repo):
    """Put the official tests and pom.xml in the student's checkout.

    Replaces src/test/ with the official src/test/java/ and pom.xml with the
    official one, and deletes BUILD_EXTRAS. Also deletes target/: otherwise Maven
    may reuse test classes compiled from the student's own copies (it only
    recompiles sources newer than their .class). Files are copied with fresh
    timestamps for the same reason. Raises SetupError if this can't be done safely.
    """
    repo = Path(repo)
    if (repo / "src").is_symlink():
        raise SetupError("src is a symbolic link, so the official tests can't be put inside it. "
                         "Make src a normal folder.")
    try:
        for rel in (STUDENT_TESTS, Path("target"), Path(POM), *map(Path, BUILD_EXTRAS)):
            remove(repo / rel)
        shutil.copytree(official_java, repo / STUDENT_JAVA_TESTS, copy_function=shutil.copyfile,
                        ignore=shutil.ignore_patterns(*IGNORED_NAMES))
        shutil.copyfile(official_pom, repo / POM)
    except (OSError, shutil.Error) as exc:
        raise SetupError(f"the official test and build files could not be put in place ({exc}).")


def git_head(repo):
    try:
        out = subprocess.run(["git", "rev-parse", "HEAD"], cwd=repo, capture_output=True,
                             text=True, timeout=30)
    except (OSError, subprocess.SubprocessError):
        return None
    if out.returncode != 0:
        return None
    return out.stdout.strip() or None


def metadata(repo, bundle, env):
    """The identifying result.json fields: env vars first, then fallbacks."""
    info = {}
    info_file = Path(bundle) / BUNDLE_INFO
    if info_file.is_file():
        try:
            info = json.loads(info_file.read_text(encoding="utf-8"))
        except ValueError:
            info = {}
    fallbacks = {
        "classroom": info.get("classroom") or "unknown",
        "assignment": info.get("assignment") or Path(bundle).name,
        "submission": Path(repo).resolve().name,
        "commit": git_head(repo) or env.get("GITHUB_SHA") or "unknown",
        "release": "",
        "review": "",
    }
    meta = {}
    for field_name, names in ENV_FIELDS.items():
        meta[field_name] = next((env[n] for n in names if env.get(n)), fallbacks[field_name])
    return meta


def build_result(report, meta, now=None):
    now = now or datetime.datetime.now(datetime.timezone.utc)
    tests = [{
        "test-name": p.label,
        "passed": p.total > 0 and p.passed == p.total,
        "score": int(p.points),
        "max-score": int(p.max_points),
    } for p in report.parts]
    return {
        "schema": SCHEMA,
        **meta,
        "datetime": now.strftime("%Y-%m-%dT%H:%M:%SZ"),
        "score": sum(t["score"] for t in tests),
        "max-score": sum(t["max-score"] for t in tests),
        "tests": tests,
    }


def release_body(report):
    lines = [f"## Test score: {report.score} / {report.max_score}", ""]
    lines += ["| Part | Tests passed | Points |", "|---|---|---|"]
    for p in report.parts:
        lines.append(f"| {p.label} | {p.passed} / {p.total} | {p.points} / {p.max_points} |")
    lines.append(f"| **Total** | **{report.passed} / {report.total}** | "
                 f"**{report.score} / {report.max_score}** |")
    if report.notes:
        lines += ["", "### Notes", "", "```text", *report.notes, "```"]
    lines += ["", "Hand-graded rubric items (formatting, comments, DRY, ...) are scored "
              "separately by your teacher/TAs."]
    return "\n".join(lines) + "\n"


def summary_line(report, differed):
    text = f"Test score: {report.score}/{report.max_score}"
    if differed:
        text += (" (test or build files differed from the official ones; "
                 "graded with the official copies)")
    return text


def write_output(path, text):
    """Write a fresh file: a symbolic link (or anything else) already there is removed first."""
    remove(path)
    Path(path).write_text(text, encoding="utf-8")


def run(repo, bundle=BUNDLE_DEFAULT, env=None, mvn=None, timeout=score.MAVEN_TIMEOUT_SECONDS):
    """Grade the student checkout in repo with the bundle's official files and score.py.
    Returns (report, result dict)."""
    env = os.environ if env is None else env
    repo, bundle = Path(repo), Path(bundle)
    sc = load_score(bundle)
    official, official_pom = bundle / OFFICIAL_DIR, bundle / OFFICIAL_POM
    if not list_files(official) or not official_pom.is_file():
        raise InfrastructureError(
            f"No official tests or pom.xml found in {bundle}. Run grading/sync_classroom50.py "
            "and push the Classroom50 config repo.")
    mvn = mvn or sc.find_maven(repo, use_wrapper=False)
    if not mvn:
        raise InfrastructureError(
            "Maven (mvn) is not installed on the grading runner. See grading/TEACHERS.md.")

    diffs = [compare_tests(official, repo / STUDENT_TESTS), compare_build_files(official_pom, repo)]
    added, modified, deleted = (sum((d[i] for d in diffs), []) for i in range(3))
    differed = bool(added or modified or deleted)
    try:
        install_official_files(official, official_pom, repo)
    except SetupError as exc:
        report = sc.score([])
        report.notes[:] = [f"Could not grade this repository: {exc} No tests ran. Test score: 0."]
    else:
        report = sc.grade(repo, mvn=mvn, timeout=timeout)
    report.notes[:0] = difference_notes(added, modified, deleted)

    result = build_result(report, metadata(repo, bundle, env))
    write_output(repo / "result.json", json.dumps(result, indent=2) + "\n")
    write_output(repo / "release-body.md", release_body(report))
    output = env.get("GITHUB_OUTPUT")
    if output:
        with open(output, "a", encoding="utf-8") as fh:
            fh.write(f"summary={summary_line(report, differed)}\n")
    return report, result


def main(argv=None):
    ap = argparse.ArgumentParser(description="Classroom50 autograder: grade with the official tests.")
    ap.add_argument("--repo", type=Path, default=Path.cwd(),
                    help="student checkout (default: current folder, as Classroom50 runs it)")
    ap.add_argument("--bundle", type=Path, default=BUNDLE_DEFAULT,
                    help="bundle folder: official-tests/, official-pom.xml and score.py all come "
                         "from here (default: this script's folder)")
    ap.add_argument("--mvn", help="Maven command (default: mvn on PATH; the repo's mvnw is "
                                  "never used)")
    args = ap.parse_args(argv)
    try:
        report, _ = run(args.repo, args.bundle, mvn=args.mvn)
        print(load_score(args.bundle).format_report(report))
    except InfrastructureError as exc:
        print(f"Autograder setup problem: {exc}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
