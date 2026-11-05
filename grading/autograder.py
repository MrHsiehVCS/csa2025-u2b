#!/usr/bin/env python3
"""Classroom50 autograder for this assignment (teachers only; students use score.py).

Classroom50 runs this file from the teacher's config repo, at
CLASSROOM/autograders/ASSIGNMENT/autograder.py, with the working directory set
to the student's checkout. The folder it sits in (the "bundle") also holds:

    score.py           the same score script students run in their Codespace
    official-tests/    the official copy of src/test/java/ (written by sync_classroom50.py)
    bundle-info.json   classroom/assignment names and when the bundle was synced

What it does:
  1. Compares the student's src/test/ files with official-tests/ and notes any
     file that was added, changed, or deleted.
  2. Replaces the student's src/test/ with the official tests and deletes
     target/ so nothing compiled from the student's copies can be reused.
  3. Runs Maven and scores each part with score.py (same numbers students see).
  4. Writes ./result.json (Classroom50 result v1, one row per part),
     ./release-body.md (readable summary), and summary= to $GITHUB_OUTPUT.

Exit code 0 whenever grading ran end to end - including compile failures and
failing tests. Non-zero only for problems on the teacher/runner side (bundle
missing its official tests, Maven not installed), which Classroom50 treats as
an infrastructure error instead of giving the student a 0.

For testing:  python3 autograder.py [--repo DIR] [--bundle DIR] [--mvn CMD]
Python 3 standard library only.
"""

import argparse
import datetime
import filecmp
import json
import os
import shutil
import subprocess
import sys
from pathlib import Path

BUNDLE_DEFAULT = Path(__file__).resolve().parent
sys.path.insert(0, str(BUNDLE_DEFAULT))
import score  # noqa: E402  (score.py sits next to this file)

SCHEMA = "classroom50/result/v1"
OFFICIAL_DIR = "official-tests"        # mirrors the repo's src/test/java/
BUNDLE_INFO = "bundle-info.json"
STUDENT_TESTS = Path("src") / "test"   # everything under here is replaced
STUDENT_JAVA_TESTS = STUDENT_TESTS / "java"
DIFFER_WARNING = "⚠ your test files differ from the official ones; graded with the official tests"
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


def list_files(root):
    """Relative paths (as strings, '/'-separated) of all files under root."""
    root = Path(root)
    if not root.is_dir():
        return set()
    return {
        p.relative_to(root).as_posix()
        for p in root.rglob("*")
        if p.is_file() and not (IGNORED_NAMES & set(p.relative_to(root).parts))
    }


def compare_tests(official_java, student_tests):
    """Compare the official tests with the student's whole src/test/ folder.

    official_java mirrors src/test/java/. Returns (added, modified, deleted),
    each a sorted list of paths relative to the repo (e.g. src/test/java/u2bpp/X.java).
    """
    official = {"java/" + p for p in list_files(official_java)}
    student = list_files(student_tests)
    prefix = STUDENT_TESTS.as_posix() + "/"
    added = sorted(prefix + p for p in student - official)
    deleted = sorted(prefix + p for p in official - student)
    modified = sorted(
        prefix + p for p in official & student
        if not filecmp.cmp(Path(official_java) / p[len("java/"):], Path(student_tests) / p,
                           shallow=False)
    )
    return added, modified, deleted


def difference_notes(added, modified, deleted):
    if not (added or modified or deleted):
        return []
    notes = [DIFFER_WARNING]
    for label, paths in (("changed", modified), ("deleted", deleted), ("added", added)):
        notes.extend(f"  {label}: {p}" for p in paths)
    return notes


def install_official_tests(official_java, repo):
    """Replace the student's src/test/ with the official src/test/java/.

    Also deletes target/: otherwise Maven may reuse test classes compiled from
    the student's own copies (it only recompiles sources newer than their .class).
    Files are copied with fresh timestamps for the same reason.
    """
    repo = Path(repo)
    tests = repo / STUDENT_TESTS
    shutil.rmtree(tests, ignore_errors=True)
    shutil.rmtree(repo / "target", ignore_errors=True)
    shutil.copytree(official_java, tests / "java", copy_function=shutil.copyfile,
                    ignore=shutil.ignore_patterns(*IGNORED_NAMES))


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
    passed = sum(p.passed for p in report.parts)
    total = sum(p.total for p in report.parts)
    lines.append(f"| **Total** | **{passed} / {total}** | **{report.score} / {report.max_score}** |")
    if report.notes:
        lines += ["", "### Notes", "", "```text", *report.notes, "```"]
    lines += ["", "Hand-graded rubric items (formatting, comments, DRY, ...) are scored "
              "separately by your teacher/TAs."]
    return "\n".join(lines) + "\n"


def summary_line(report, differed):
    text = f"Test score: {report.score}/{report.max_score}"
    if differed:
        text += " (test files differed from the official ones; graded with the official tests)"
    return text


def run(repo, bundle=BUNDLE_DEFAULT, env=None, mvn=None, timeout=score.MAVEN_TIMEOUT_SECONDS):
    """Grade the student checkout in repo. Returns (report, result dict)."""
    env = os.environ if env is None else env
    repo, bundle = Path(repo), Path(bundle)
    official = bundle / OFFICIAL_DIR
    if not list_files(official):
        raise InfrastructureError(
            f"No official tests found in {official}. Run grading/sync_classroom50.py "
            "and push the Classroom50 config repo.")
    mvn = mvn or score.find_maven(repo)
    if not mvn:
        raise InfrastructureError(
            "Maven (mvn) is not installed on the grading runner. See grading/TEACHERS.md.")

    added, modified, deleted = compare_tests(official, repo / STUDENT_TESTS)
    differed = bool(added or modified or deleted)
    install_official_tests(official, repo)

    report = score.grade(repo, mvn=mvn, timeout=timeout)
    report.notes[:0] = difference_notes(added, modified, deleted)

    result = build_result(report, metadata(repo, bundle, env))
    (repo / "result.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    (repo / "release-body.md").write_text(release_body(report), encoding="utf-8")
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
                    help="folder holding official-tests/ (default: this script's folder)")
    ap.add_argument("--mvn", help="Maven command (default: mvnw or mvn on PATH)")
    args = ap.parse_args(argv)
    try:
        report, _ = run(args.repo, args.bundle, mvn=args.mvn)
    except InfrastructureError as exc:
        print(f"Autograder setup problem: {exc}", file=sys.stderr)
        return 2
    print(score.format_report(report))
    return 0


if __name__ == "__main__":
    sys.exit(main())
