"""Tests for grading/score.py, using saved Surefire report sets in fixtures/.

Run from the project folder:  python3 -m unittest
No Maven needed: the end-to-end tests use a fake `mvn` that replays saved output.
"""

import contextlib
import io
import os
import shutil
import sys
import tempfile
import textwrap
import unittest
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import score  # noqa: E402

FIXTURES = Path(__file__).resolve().parent / "fixtures"


def points(report):
    return {p.label: (p.passed, p.total, p.points, p.max_points) for p in report.parts}


def score_fixture(name):
    return score.score(score.read_reports(FIXTURES / name))


class ConfigTests(unittest.TestCase):
    def test_weights_total_20(self):
        self.assertEqual([p[2] for p in score.PARTS], [2, 11, 4, 3])
        self.assertEqual(sum(p[2] for p in score.PARTS), 20)

    def test_changing_a_weight_needs_no_other_edit(self):
        parts = [(label, cls, 10) for label, cls, _ in score.PARTS]
        with mock.patch.object(score, "PARTS", parts):
            report = score_fixture("all-pass")
        self.assertEqual((report.score, report.max_score), (40, 40))

    def test_points_per_test_switch(self):
        with mock.patch.object(score, "POINTS_PER_TEST", 1):
            report = score_fixture("partial")
        self.assertEqual((report.score, report.max_score), (22, 35))


class ScoringTests(unittest.TestCase):
    def test_all_pass(self):
        report = score_fixture("all-pass")
        self.assertEqual(points(report), {
            "Part A - results": (5, 5, 2, 2),
            "Part B - play": (13, 13, 11, 11),
            "Part C - isPalindrome": (10, 10, 4, 4),
            "Part C - execute": (7, 7, 3, 3),
        })
        self.assertEqual((report.score, report.max_score), (20, 20))
        self.assertEqual(report.notes, [])

    def test_none_pass(self):
        report = score_fixture("none-pass")
        self.assertEqual((report.score, report.max_score), (0, 20))

    def test_partial_counts_failures_errors_and_skips_as_not_passed(self):
        report = score_fixture("partial")
        self.assertEqual(points(report), {
            "Part A - results": (3, 5, 1, 2),          # 1.2  -> 1
            "Part B - play": (10, 13, 8, 11),          # 8.46 -> 8
            "Part C - isPalindrome": (9, 10, 4, 4),    # 3.6  -> 4
            "Part C - execute": (0, 7, 0, 3),
        })
        self.assertEqual(report.score, 13)

    def test_rounding_boundaries(self):
        report = score_fixture("rounding")
        self.assertEqual(points(report), {
            "Part A - results": (1, 4, 1, 2),          # 0.5  -> 1 (halves up)
            "Part B - play": (1, 2, 6, 11),            # 5.5  -> 6 (halves up)
            "Part C - isPalindrome": (7, 16, 2, 4),    # 1.75 -> 2
            "Part C - execute": (1, 7, 0, 3),          # 0.43 -> 0
        })

    def test_part_points_edges(self):
        self.assertEqual(score.part_points(2, 1, 4), 1)    # exactly .5 rounds up
        self.assertEqual(score.part_points(2, 3, 4), 2)    # 1.5 -> 2
        self.assertEqual(score.part_points(5, 1, 2), 3)    # 2.5 -> 3, where round() gives 2
        self.assertEqual(score.part_points(3, 1, 6), 1)    # 0.5 -> 1
        self.assertEqual(score.part_points(11, 0, 13), 0)
        self.assertEqual(score.part_points(4, 0, 0), 0)    # no tests ran: 0, no crash

    def test_part_with_zero_tests_scores_zero_and_is_reported(self):
        report = score_fixture("part-missing")
        self.assertEqual(points(report)["Part C - execute"], (0, 0, 0, 3))
        self.assertEqual(report.max_score, 20)
        self.assertTrue(any("No tests ran for Part C - execute" in n for n in report.notes))

    def test_nested_class_counts_for_its_part(self):
        report = score_fixture("part-missing")
        self.assertEqual(points(report)["Part B - play"], (12, 13, 10, 11))

    def test_unmapped_test_is_reported_not_scored(self):
        report = score_fixture("part-missing")
        self.assertEqual([c.classname for c in report.unmapped], ["u2bpp.RockPaperScissorsTests"])
        self.assertTrue(any("u2bpp.RockPaperScissorsTests" in n and "no part" in n
                            for n in report.notes))
        self.assertEqual(report.score, 2 + 10 + 4 + 0)

    def test_no_reports_folder_scores_zero(self):
        report = score.score(score.read_reports(FIXTURES / "does-not-exist"))
        self.assertEqual((report.score, report.max_score), (0, 20))
        self.assertIn("No test reports were found", report.notes[0])

    def test_broken_report_file_is_skipped(self):
        with tempfile.TemporaryDirectory() as tmp:
            shutil.copy(FIXTURES / "all-pass" / "TEST-u2bpp.PartATests.xml", tmp)
            Path(tmp, "TEST-u2bpp.PartBTests.xml").write_text("<testsuite><testcase")
            report = score.score(score.read_reports(tmp))
        self.assertEqual(points(report)["Part A - results"], (5, 5, 2, 2))
        self.assertEqual(points(report)["Part B - play"], (0, 0, 0, 11))


class OutputTests(unittest.TestCase):
    def run_main(self, *argv):
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            code = score.main(list(argv))
        return code, out.getvalue()

    def test_table(self):
        code, out = self.run_main("--reports", str(FIXTURES / "partial"))
        self.assertEqual(code, 0)
        self.assertEqual(out, textwrap.dedent("""\
            Part                   Tests passed   Points
            --------------------------------------------
            Part A - results              3 / 5    1 / 2
            Part B - play               10 / 13   8 / 11
            Part C - isPalindrome        9 / 10    4 / 4
            Part C - execute              0 / 7    0 / 3
            --------------------------------------------
            Total                       22 / 35  13 / 20
            """))

    def test_warnings_print_above_table(self):
        _, out = self.run_main("--reports", str(FIXTURES / "part-missing"))
        lines = out.splitlines()
        self.assertIn("Warning: 1 test(s) belong to no part", lines[1])
        self.assertEqual(lines[-1].split(), ["Total", "27", "/", "28", "16", "/", "20"])


@unittest.skipIf(os.name == "nt", "fake mvn is a POSIX script")
class MavenRunTests(unittest.TestCase):
    """grade() end to end, with a fake mvn that replays output and copies reports."""

    FAKE_MVN = textwrap.dedent("""\
        #!{python}
        import os, shutil, sys
        from pathlib import Path
        out = os.environ.get("FAKE_MVN_OUTPUT")
        if out:
            sys.stdout.write(Path(out).read_text().replace("{{repo}}", os.getcwd()))
        reports = os.environ.get("FAKE_MVN_REPORTS")
        if reports:
            shutil.copytree(reports, "target/surefire-reports")
        sys.exit(int(os.environ.get("FAKE_MVN_RC", "0")))
        """)

    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.repo = Path(tmp.name) / "repo"
        self.repo.mkdir()
        self.mvn = Path(tmp.name) / "mvn"
        self.mvn.write_text(self.FAKE_MVN.format(python=sys.executable))
        self.mvn.chmod(0o755)

    def grade(self, **env):
        with mock.patch.dict(os.environ, env):
            return score.grade(self.repo, mvn=str(self.mvn))

    def test_tests_ran(self):
        report = self.grade(FAKE_MVN_REPORTS=str(FIXTURES / "all-pass"))
        self.assertEqual(report.score, 20)
        self.assertEqual(report.notes, [])

    def test_compile_failure_scores_zero_with_clear_message(self):
        stale = self.repo / "target" / "surefire-reports"
        shutil.copytree(FIXTURES / "all-pass", stale)  # old reports from an earlier run
        report = self.grade(FAKE_MVN_RC="1",
                            FAKE_MVN_OUTPUT=str(FIXTURES / "compile-failure" / "maven-output.txt"))
        self.assertEqual((report.score, report.max_score), (0, 20))
        self.assertIn("did not compile", report.notes[0])
        self.assertEqual(report.notes[1:], [
            "  src/main/java/u2bpp/RockPaperScissors.java line 36: "
            "incompatible types: java.lang.String cannot be converted to int"])
        text = score.format_report(report)
        self.assertIn("Total", text)

    def test_build_problem_after_some_tests_ran_still_scores_them(self):
        report = self.grade(FAKE_MVN_RC="1", FAKE_MVN_REPORTS=str(FIXTURES / "part-missing"))
        self.assertEqual(report.score, 16)
        self.assertIn("build problem", report.notes[0])

    def test_maven_missing(self):
        with mock.patch.object(score, "find_maven", return_value=None):
            report = score.grade(self.repo)
        self.assertEqual(report.score, 0)
        self.assertTrue(any("Maven (mvn) was not found" in n for n in report.notes))


if __name__ == "__main__":
    unittest.main()
