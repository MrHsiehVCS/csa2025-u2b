#!/usr/bin/env python3
"""Show your test score for this project, per part and in total.

Run from the project folder (in the Codespace terminal):

    python3 grading/score.py

It runs the Maven tests, reads the test reports, and prints a table like the
one your teacher and TAs see. Each part is worth a fixed number of points
(see PARTS below); you earn that part's points times the fraction of its tests
you pass, rounded to a whole number (halves round up).

Other options (mostly for teachers):

    python3 grading/score.py --reports DIR   score existing Surefire reports, skip Maven
    python3 grading/score.py --repo DIR      project folder (default: this script's parent)

Python 3 standard library only.

Layout, for reuse (e.g. by a Classroom50 autograder.py next to this file):
    run_maven(repo)        -> BuildResult   runs the tests (the only step needing Maven)
    read_reports(dir)      -> [TestCase]    parses Surefire TEST-*.xml files
    score(cases, build)    -> Report        pure: maps tests to parts and scores them
    format_report(report)  -> str           the table students see
    grade(repo)            -> Report        all of the above, minus printing
"""

import argparse
import os
import re
import shutil
import signal
import subprocess
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from fractions import Fraction
from pathlib import Path

# ---------------------------------------------------------------------------
# CONFIG: the only block to edit when weights change.
#
# Each part: (label shown in the table, JUnit test class, points).
# A test belongs to a part when its Surefire classname, up to the first "$"
# (so @Nested classes count too), equals the part's test class.
#
# Per-part scoring is a trial. To go back to "1 point per test", set
# POINTS_PER_TEST = 1: each part is then worth (number of its tests) points
# and the points column below is ignored.
# ---------------------------------------------------------------------------
PARTS = [
    ("Part A - results", "u2bpp.PartATests", 2),
    ("Part B - play", "u2bpp.PartBTests", 11),
    ("Part C - isPalindrome", "u2bpp.PartCIsPalindromeTests", 4),
    ("Part C - execute", "u2bpp.PartCExecuteTests", 3),
]
POINTS_PER_TEST = None
# ---------------------------------------------------------------------------

REPO_DEFAULT = Path(__file__).resolve().parent.parent
REPORTS_SUBDIR = Path("target") / "surefire-reports"
MAVEN_TIMEOUT_SECONDS = 600


@dataclass
class TestCase:
    classname: str
    name: str
    passed: bool


@dataclass
class BuildResult:
    """What happened when Maven ran. ok=False means a build error (e.g. compile)."""
    ok: bool
    reports_dir: Path
    errors: list = field(default_factory=list)  # short lines worth showing


@dataclass
class PartScore:
    label: str
    test_class: str
    passed: int
    total: int
    points: int
    max_points: int


@dataclass
class Report:
    parts: list                                   # [PartScore], in PARTS order
    unmapped: list = field(default_factory=list)  # [TestCase] matching no part
    notes: list = field(default_factory=list)     # messages printed above the table

    @property
    def score(self):
        return sum(p.points for p in self.parts)

    @property
    def max_score(self):
        return sum(p.max_points for p in self.parts)


def part_of(classname):
    """Return the PARTS entry a test classname belongs to, or None."""
    top = classname.split("$", 1)[0]
    for part in PARTS:
        if part[1] == top:
            return part
    return None


def part_points(points, passed, total):
    """round(points * passed / total) with halves rounding up; 0 when no tests ran.

    Exact fractions, so 2 * 1/4 = 0.5 really rounds up (Python's round() wouldn't).
    """
    if total == 0:
        return 0
    return int(Fraction(points * passed, total) + Fraction(1, 2))


def read_reports(reports_dir):
    """Parse every TEST-*.xml in reports_dir. Missing folder -> []."""
    cases = []
    reports_dir = Path(reports_dir)
    if not reports_dir.is_dir():
        return cases
    for xml in sorted(reports_dir.glob("TEST-*.xml")):
        try:
            root = ET.parse(xml).getroot()
        except ET.ParseError:
            continue  # half-written report (e.g. the test run was killed)
        for case in root.iter("testcase"):
            failed = any(case.find(tag) is not None for tag in ("failure", "error", "skipped"))
            cases.append(TestCase(case.get("classname", ""), case.get("name", ""), not failed))
    return cases


def score(cases, build=None):
    """Score test cases per part. Pure function: no files, no Maven."""
    report = Report(parts=[])
    by_class = {test_class: [] for _, test_class, _ in PARTS}
    for case in cases:
        part = part_of(case.classname)
        if part is None:
            report.unmapped.append(case)
        else:
            by_class[part[1]].append(case)

    for label, test_class, points in PARTS:
        group = by_class[test_class]
        passed = sum(1 for c in group if c.passed)
        total = len(group)
        if POINTS_PER_TEST is not None:
            max_points = POINTS_PER_TEST * total
            earned = POINTS_PER_TEST * passed
        else:
            max_points = points
            earned = part_points(points, passed, total)
        report.parts.append(PartScore(label, test_class, passed, total, earned, max_points))

    if build is not None and not build.ok:
        if cases:
            report.notes.append("Maven reported a build problem, so some tests may not have run.")
        else:
            report.notes.append("Your code did not compile (or Maven failed), so no tests ran. "
                                "Test score: 0. Fix the errors below, then run this again.")
        report.notes.extend("  " + line for line in build.errors)
    elif not cases:
        report.notes.append("No test reports were found, so no tests ran. Test score: 0.")
    for part in report.parts:
        if part.total == 0 and cases:
            report.notes.append(f"No tests ran for {part.label} ({part.test_class}), "
                                "so it scores 0.")
    if report.unmapped:
        names = ", ".join(sorted({c.classname for c in report.unmapped}))
        report.notes.append(f"Warning: {len(report.unmapped)} test(s) belong to no part and "
                            f"were not scored: {names}")
    return report


def format_report(report):
    rows = [("Part", "Tests passed", "Points")]
    for p in report.parts:
        rows.append((p.label, f"{p.passed} / {p.total}", f"{p.points} / {p.max_points}"))
    passed = sum(p.passed for p in report.parts)
    total = sum(p.total for p in report.parts)
    rows.append(("Total", f"{passed} / {total}", f"{report.score} / {report.max_score}"))

    w0 = max(len(r[0]) for r in rows)
    w1 = max(len(r[1]) for r in rows)
    w2 = max(len(r[2]) for r in rows)

    def line(r):
        return f"{r[0]:<{w0}}  {r[1]:>{w1}}  {r[2]:>{w2}}"

    rule = "-" * len(line(rows[0]))
    out = list(report.notes)
    if out:
        out.append("")
    out += [line(rows[0]), rule]
    out += [line(r) for r in rows[1:-1]]
    out += [rule, line(rows[-1])]
    return "\n".join(out)


def find_maven(repo):
    """Return the Maven command to use, or None if Maven isn't installed."""
    wrapper = Path(repo) / ("mvnw.cmd" if os.name == "nt" else "mvnw")
    if wrapper.is_file():
        return str(wrapper)
    return shutil.which("mvn") or shutil.which("mvn.cmd")


COMPILE_ERROR = re.compile(r"^\[ERROR\] (.+\.java):\[(\d+),(\d+)\] (.*)$")


def _error_lines(output, repo, limit=10):
    """Pick the useful [ERROR] lines out of Maven's output (compile errors first)."""
    prefix = str(Path(repo).resolve()) + os.sep
    compile_errors, other = [], []
    for raw in output.splitlines():
        if not raw.startswith("[ERROR]"):
            continue
        raw = raw.replace(prefix, "")
        m = COMPILE_ERROR.match(raw)
        if m:
            text = f"{m.group(1)} line {m.group(2)}: {m.group(4)}"
            if text not in compile_errors:  # Maven prints each compile error twice
                compile_errors.append(text)
        elif raw.strip() != "[ERROR]":
            other.append(raw)
    picked = compile_errors or other
    if len(picked) > limit:
        picked = picked[:limit] + [f"... ({len(picked) - limit} more)"]
    return picked


def run_maven(repo, mvn=None, timeout=MAVEN_TIMEOUT_SECONDS):
    """Run the tests with Maven. Old reports are deleted first so they can't be reused."""
    repo = Path(repo)
    reports_dir = repo / REPORTS_SUBDIR
    shutil.rmtree(reports_dir, ignore_errors=True)
    mvn = mvn or find_maven(repo)
    if not mvn:
        return BuildResult(False, reports_dir, ["Maven (mvn) was not found on this computer."])
    # failure.ignore: failing tests don't fail the build, so a non-zero exit
    # means a real build problem (usually a compile error).
    cmd = [mvn, "-B", "-q", "-Dmaven.test.failure.ignore=true", "test"]
    popen_args = {"start_new_session": True} if os.name != "nt" else {}
    proc = subprocess.Popen(cmd, cwd=repo, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            text=True, errors="replace", **popen_args)
    try:
        output, _ = proc.communicate(timeout=timeout)
    except subprocess.TimeoutExpired:
        if os.name != "nt":
            os.killpg(proc.pid, signal.SIGKILL)  # also stops the forked test JVM
        else:
            proc.kill()
        proc.communicate()
        return BuildResult(False, reports_dir,
                           [f"The tests took longer than {timeout} seconds and were stopped "
                            "(an infinite loop?)."])
    if proc.returncode == 0:
        return BuildResult(True, reports_dir)
    return BuildResult(False, reports_dir,
                       _error_lines(output, repo) or [f"Maven exited with code {proc.returncode}."])


def grade(repo=REPO_DEFAULT, mvn=None, timeout=MAVEN_TIMEOUT_SECONDS):
    """Run the tests in repo and return the scored Report."""
    build = run_maven(repo, mvn=mvn, timeout=timeout)
    return score(read_reports(build.reports_dir), build)


def main(argv=None):
    ap = argparse.ArgumentParser(description="Show your test score per part and in total.")
    ap.add_argument("--repo", type=Path, default=REPO_DEFAULT,
                    help="project folder (default: the folder above grading/)")
    ap.add_argument("--reports", type=Path,
                    help="score existing Surefire reports in this folder instead of running Maven")
    ap.add_argument("--mvn", help="Maven command to use (default: mvnw or mvn on PATH)")
    ap.add_argument("--timeout", type=int, default=MAVEN_TIMEOUT_SECONDS,
                    help="seconds before the test run is stopped (default: %(default)s)")
    args = ap.parse_args(argv)

    if args.reports is not None:
        report = score(read_reports(args.reports))
    else:
        print("Running the tests (this takes a few seconds)...", flush=True)
        report = grade(args.repo, mvn=args.mvn, timeout=args.timeout)
    print(format_report(report))
    return 0


if __name__ == "__main__":
    sys.exit(main())
