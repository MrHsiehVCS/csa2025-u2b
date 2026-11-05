"""Tests for grading/autograder.py and grading/sync_classroom50.py.

Run from the project folder:  python3 -m unittest
No Maven needed: a fake `mvn` copies saved Surefire reports (see test_score.py).
"""

import contextlib
import datetime
import io
import json
import os
import shutil
import subprocess
import sys
import tempfile
import textwrap
import unittest
from pathlib import Path
from unittest import mock

GRADING = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(GRADING))
import autograder  # noqa: E402
import score  # noqa: E402
import sync_classroom50  # noqa: E402

REPO = GRADING.parent
OFFICIAL_JAVA = REPO / "src" / "test" / "java"
FIXTURES = Path(__file__).resolve().parent / "fixtures"
REQUIRED = ["schema", "classroom", "assignment", "submission", "commit", "release", "review",
            "datetime", "score", "max-score", "tests"]

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


def assert_valid_result(test, result):
    """Check a result.json dict against the Classroom50 result v1 contract."""
    for key in REQUIRED:
        test.assertIn(key, result)
    test.assertEqual(result["schema"], "classroom50/result/v1")
    for key in ("classroom", "assignment", "submission", "commit", "release", "review"):
        test.assertIsInstance(result[key], str, key)
    datetime.datetime.strptime(result["datetime"], "%Y-%m-%dT%H:%M:%SZ")
    test.assertIs(type(result["score"]), int)
    test.assertIs(type(result["max-score"]), int)
    test.assertLessEqual(result["score"], result["max-score"])
    test.assertEqual([t["test-name"] for t in result["tests"]], [p[0] for p in score.PARTS])
    for row in result["tests"]:
        test.assertEqual(set(row), {"test-name", "passed", "score", "max-score"})
        test.assertIsInstance(row["test-name"], str)
        test.assertIs(type(row["passed"]), bool)
        test.assertIs(type(row["score"]), int)
        test.assertIs(type(row["max-score"]), int)
        test.assertLessEqual(0, row["score"])
        test.assertLessEqual(row["score"], row["max-score"])
    test.assertEqual(result["score"], sum(t["score"] for t in result["tests"]))
    test.assertEqual(result["max-score"], sum(t["max-score"] for t in result["tests"]))


@unittest.skipIf(os.name == "nt", "fake mvn is a POSIX script")
class AutograderTestBase(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.tmp = Path(tmp.name)
        # The bundle as Classroom50 serves it: autograder.py, score.py, official-tests/.
        self.bundle = self.tmp / "config" / "csa" / "autograders" / "csa2025-u2b"
        self.bundle.mkdir(parents=True)
        for name in ("autograder.py", "score.py"):
            shutil.copy(GRADING / name, self.bundle / name)
        shutil.copytree(OFFICIAL_JAVA, self.bundle / "official-tests")
        # A student checkout whose tests start identical to the official ones.
        self.repo = self.tmp / "student"
        shutil.copytree(OFFICIAL_JAVA, self.repo / "src" / "test" / "java")
        self.mvn = self.tmp / "mvn"
        self.mvn.write_text(FAKE_MVN.format(python=sys.executable))
        self.mvn.chmod(0o755)
        self.env = {"FAKE_MVN_REPORTS": str(FIXTURES / "all-pass")}

    def student_test(self, name):
        return self.repo / "src" / "test" / "java" / "u2bpp" / name

    def run_autograder(self, env=None):
        env = dict(self.env if env is None else env)
        with mock.patch.dict(os.environ, env):
            return autograder.run(self.repo, self.bundle, env=env, mvn=str(self.mvn))

    def result_json(self):
        return json.loads((self.repo / "result.json").read_text(encoding="utf-8"))


class TestFileTests(AutograderTestBase):
    def test_identical_tests_no_warning(self):
        report, result = self.run_autograder()
        self.assertEqual(report.notes, [])
        self.assertEqual((result["score"], result["max-score"]), (20, 20))
        self.assertTrue(all(t["passed"] for t in result["tests"]))
        self.assertEqual(self.result_json(), result)
        assert_valid_result(self, result)

    def test_modified_test_warns_and_official_copy_is_used(self):
        self.student_test("PartATests.java").write_text("// assertTrue(true) for everything\n")
        report, result = self.run_autograder()
        self.assertEqual(report.notes, [autograder.DIFFER_WARNING,
                                        "  changed: src/test/java/u2bpp/PartATests.java"])
        self.assertEqual(self.student_test("PartATests.java").read_bytes(),
                         (OFFICIAL_JAVA / "u2bpp" / "PartATests.java").read_bytes())
        assert_valid_result(self, result)

    def test_stale_compiled_tests_are_deleted(self):
        stale = self.repo / "target" / "test-classes" / "u2bpp" / "PartATests.class"
        stale.parent.mkdir(parents=True)
        stale.write_bytes(b"compiled from the student's edited copy")
        self.run_autograder()
        self.assertFalse(stale.exists())

    def test_deleted_test_warns_and_is_restored(self):
        self.student_test("PartBTests.java").unlink()
        report, _ = self.run_autograder()
        self.assertIn(autograder.DIFFER_WARNING, report.notes)
        self.assertIn("  deleted: src/test/java/u2bpp/PartBTests.java", report.notes)
        self.assertTrue(self.student_test("PartBTests.java").is_file())

    def test_added_test_file_warns_and_is_removed(self):
        extra = self.repo / "src" / "test" / "java" / "other" / "Extra.java"
        extra.parent.mkdir()
        extra.write_text("class Extra {}\n")
        report, _ = self.run_autograder()
        self.assertIn("  added: src/test/java/other/Extra.java", report.notes)
        self.assertFalse(extra.exists())

    def test_whole_test_folder_missing(self):
        shutil.rmtree(self.repo / "src" / "test")
        report, _ = self.run_autograder()
        self.assertEqual(report.notes[0], autograder.DIFFER_WARNING)
        self.assertEqual(len([n for n in report.notes if "deleted:" in n]),
                         len(list((OFFICIAL_JAVA / "u2bpp").glob("*.java"))))

    def test_warning_shows_in_release_body_and_summary(self):
        self.student_test("PartATests.java").write_text("changed\n")
        out = self.tmp / "github_output"
        self.run_autograder({**self.env, "GITHUB_OUTPUT": str(out)})
        body = (self.repo / "release-body.md").read_text(encoding="utf-8")
        self.assertIn(autograder.DIFFER_WARNING, body)
        self.assertIn("| Part B - play | 13 / 13 | 11 / 11 |", body)
        self.assertIn("## Test score: 20 / 20", body)
        self.assertEqual(out.read_text(encoding="utf-8"),
                         "summary=Test score: 20/20 (test files differed from the official "
                         "ones; graded with the official tests)\n")


class ResultTests(AutograderTestBase):
    def test_partial_scores_one_row_per_part(self):
        _, result = self.run_autograder({"FAKE_MVN_REPORTS": str(FIXTURES / "partial")})
        assert_valid_result(self, result)
        self.assertEqual([(t["score"], t["max-score"], t["passed"]) for t in result["tests"]],
                         [(1, 2, False), (8, 11, False), (4, 4, False), (0, 3, False)])
        self.assertEqual(result["score"], 13)

    def test_metadata_from_environment(self):
        env = {**self.env, "CLASSROOM50_CLASSROOM": "csa-2025", "CLASSROOM50_ASSIGNMENT": "u2b",
               "CLASSROOM50_SUBMISSION": "sub-7", "CLASSROOM50_COMMIT": "abc123",
               "CLASSROOM50_RELEASE": "v1", "CLASSROOM50_REVIEW": "r"}
        _, result = self.run_autograder(env)
        self.assertEqual({k: result[k] for k in autograder.ENV_FIELDS},
                         {"classroom": "csa-2025", "assignment": "u2b", "submission": "sub-7",
                          "commit": "abc123", "release": "v1", "review": "r"})

    def test_metadata_fallbacks(self):
        (self.bundle / "bundle-info.json").write_text(
            json.dumps({"classroom": "csa", "assignment": "csa2025-u2b"}))
        env = {**self.env, "GITHUB_REPOSITORY": "org/u2b-alice", "GITHUB_SHA": "f00d"}
        with mock.patch.object(autograder, "git_head", return_value=None):
            _, result = self.run_autograder(env)
        self.assertEqual({k: result[k] for k in autograder.ENV_FIELDS},
                         {"classroom": "csa", "assignment": "csa2025-u2b",
                          "submission": "org/u2b-alice", "commit": "f00d",
                          "release": "", "review": ""})
        assert_valid_result(self, result)

    def test_commit_falls_back_to_git_head(self):
        with mock.patch.object(autograder, "git_head", return_value="deadbeef"):
            _, result = self.run_autograder({**self.env, "GITHUB_SHA": "other"})
        self.assertEqual(result["commit"], "deadbeef")


class ExitCodeTests(AutograderTestBase):
    """Run the bundle's own autograder.py as Classroom50 would: cwd = student checkout."""

    def run_bundle(self, *args, **env):
        return subprocess.run([sys.executable, str(self.bundle / "autograder.py"), *args],
                              cwd=self.repo, capture_output=True, text=True,
                              env={**os.environ, **env})

    def test_all_pass_exits_0(self):
        proc = self.run_bundle("--mvn", str(self.mvn), **self.env)
        self.assertEqual(proc.returncode, 0, proc.stderr)
        assert_valid_result(self, self.result_json())
        self.assertIn("Total", proc.stdout)

    def test_compile_failure_exits_0_with_zero_score(self):
        proc = self.run_bundle("--mvn", str(self.mvn), FAKE_MVN_RC="1",
                               FAKE_MVN_OUTPUT=str(FIXTURES / "compile-failure" /
                                                   "maven-output.txt"))
        self.assertEqual(proc.returncode, 0, proc.stderr)
        result = self.result_json()
        assert_valid_result(self, result)
        self.assertEqual((result["score"], result["max-score"]), (0, 20))
        body = (self.repo / "release-body.md").read_text(encoding="utf-8")
        self.assertIn("did not compile", body)
        self.assertIn("RockPaperScissors.java line 36", body)

    def test_maven_missing_is_an_infrastructure_error(self):
        with mock.patch.object(score, "find_maven", return_value=None):
            err = io.StringIO()
            with contextlib.redirect_stderr(err):
                code = autograder.main(["--repo", str(self.repo), "--bundle", str(self.bundle)])
        self.assertNotEqual(code, 0)
        self.assertIn("Maven", err.getvalue())
        self.assertFalse((self.repo / "result.json").exists())

    def test_bundle_without_official_tests_is_an_infrastructure_error(self):
        shutil.rmtree(self.bundle / "official-tests")
        proc = self.run_bundle("--mvn", str(self.mvn), **self.env)
        self.assertNotEqual(proc.returncode, 0)
        self.assertIn("sync_classroom50.py", proc.stderr)
        self.assertTrue(self.student_test("PartATests.java").is_file())  # untouched


class SyncTests(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.config = Path(tmp.name) / "config"
        (self.config / "csa-2025" / "autograders").mkdir(parents=True)
        (self.config / ".git").mkdir()

    def sync(self, *args):
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            code = sync_classroom50.main([str(self.config), *args])
        return code, out.getvalue(), err.getvalue()

    def test_writes_bundle_at_assignment_folder(self):
        code, out, _ = self.sync()
        self.assertEqual(code, 0)
        bundle = self.config / "csa-2025" / "autograders" / "csa2025-u2b"
        self.assertEqual(sorted(p.name for p in bundle.iterdir()),
                         ["autograder.py", "bundle-info.json", "official-tests", "score.py"])
        self.assertEqual(autograder.list_files(bundle / "official-tests"),
                         autograder.list_files(OFFICIAL_JAVA))
        self.assertEqual((bundle / "score.py").read_bytes(), (GRADING / "score.py").read_bytes())
        info = json.loads((bundle / "bundle-info.json").read_text())
        self.assertEqual((info["classroom"], info["assignment"]), ("csa-2025", "csa2025-u2b"))
        self.assertIn("added: u2bpp/PartATests.java", out)

    def test_resync_reports_changes_and_removes_stale_tests(self):
        self.sync("--classroom", "csa-2025", "--assignment", "u2b")
        official = self.config / "csa-2025" / "autograders" / "u2b" / "official-tests"
        (official / "u2bpp" / "Old.java").write_text("old\n")
        (official / "u2bpp" / "PartATests.java").write_text("edited\n")
        (official / "u2bpp" / "PartBTests.java").unlink()
        code, out, _ = self.sync("--classroom", "csa-2025", "--assignment", "u2b")
        self.assertEqual(code, 0)
        self.assertIn("removed: u2bpp/Old.java", out)
        self.assertIn("changed: u2bpp/PartATests.java", out)
        self.assertIn("added: u2bpp/PartBTests.java", out)
        self.assertFalse((official / "u2bpp" / "Old.java").exists())
        code, out, _ = self.sync("--classroom", "csa-2025", "--assignment", "u2b")
        self.assertIn("unchanged since the last sync", out)

    def test_ambiguous_classroom_needs_flag(self):
        (self.config / "csa-2026" / "autograders").mkdir(parents=True)
        code, _, err = self.sync()
        self.assertEqual(code, 1)
        self.assertIn("--classroom", err)
        self.assertIn("csa-2026", err)

    def test_missing_config_folder(self):
        err = io.StringIO()
        with contextlib.redirect_stderr(err):
            code = sync_classroom50.main([str(self.config / "nope")])
        self.assertEqual(code, 1)
        self.assertIn("not found", err.getvalue())

    @unittest.skipIf(os.name == "nt", "fake mvn is a POSIX script")
    def test_synced_bundle_grades(self):
        self.sync()
        bundle = self.config / "csa-2025" / "autograders" / "csa2025-u2b"
        student = Path(self.config).parent / "student"
        shutil.copytree(OFFICIAL_JAVA, student / "src" / "test" / "java")
        mvn = Path(self.config).parent / "mvn"
        mvn.write_text(FAKE_MVN.format(python=sys.executable))
        mvn.chmod(0o755)
        proc = subprocess.run([sys.executable, str(bundle / "autograder.py"), "--mvn", str(mvn)],
                              cwd=student, capture_output=True, text=True,
                              env={**os.environ, "FAKE_MVN_REPORTS": str(FIXTURES / "all-pass")})
        self.assertEqual(proc.returncode, 0, proc.stderr)
        result = json.loads((student / "result.json").read_text())
        assert_valid_result(self, result)
        self.assertEqual((result["classroom"], result["assignment"]), ("csa-2025", "csa2025-u2b"))
        self.assertEqual(result["score"], 20)


if __name__ == "__main__":
    unittest.main()
