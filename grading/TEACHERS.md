# Grading notes for teachers and TAs

Students never need this file. It explains how Classroom50 grades this
assignment and what to do when you change a test.

## How grading works

- Each test file belongs to one part. Each part is worth fixed points:
  **A 2 · B 11 · C-isPalindrome 4 · C-execute 3 (20 total)**. A student earns
  a part's points times the fraction of its tests they pass, rounded to a whole
  number (halves round up). To change the points, edit the `PARTS` block at the
  top of `grading/score.py`, then sync (below).
- Students can run `python3 grading/score.py` in their Codespace and see the
  same numbers you will.
- Classroom50 uses `autograder.py`, which:
  1. compares the student's test files with the official copies,
  2. replaces the student's test files with the official copies,
  3. runs the tests and scores each part with `score.py`,
  4. writes the score (one row per part) and a short summary.
- If a student's test files were different (edited, deleted, or extra files),
  the summary starts with **"⚠ your test files differ from the official ones;
  graded with the official tests"** and lists the files. The score is still
  correct. This also shows up for every student if you changed a test and
  forgot to push the change to student repos - that's harmless, but it's a
  reminder.
- A student whose code doesn't compile gets 0 test points and a summary with
  the compile errors (this is a normal grade, not an error).
- The hand-graded rubric items (compiles/runs, formatting, comments, DRY, no
  extra public methods) are **not** part of this score. TAs still grade those.

## One-time setup

1. Clone your Classroom50 config repo somewhere on your computer, e.g.
   `~/classroom50-config`. Its classroom folders hold an `autograders/` folder.
2. From this project folder, run the sync command (next section). It creates
   `CLASSROOM/autograders/csa2025-u2b/` in the config repo with:
   - `autograder.py` and `score.py` - the grading code,
   - `official-tests/` - a copy of this project's `src/test/java/`,
   - `bundle-info.json` - the classroom and assignment names and when you synced.
3. In the assignment's runtime settings, set **Java 21** (it must match
   `pom.xml` and the Codespace). Python: the default is fine; no extra packages
   are needed.
4. **Maven (not yet verified):** the autograder needs the `mvn` command.
   GitHub's standard Ubuntu runners usually include Maven, but the Classroom50
   docs don't say so. If grading fails with "Maven (mvn) is not installed on
   the grading runner", add Maven to the runtime settings (for example with the
   `apt` option, package `maven`). To check during the try-out (#11).
5. Commit and push the config repo.

If the assignment folder in Classroom50 has a different name, pass it with
`--assignment NAME` every time you sync.

## After changing a test

1. Edit the test in this project and check it (run the tests, and the
   reference-solution checks in the private solution repo).
2. Sync:

   ```text
   python3 grading/sync_classroom50.py ~/classroom50-config --classroom CLASSROOM
   ```

   `--classroom` can be left out if the config repo has only one classroom
   folder. The command lists which test files were added, changed, or removed.
3. Commit and push the config repo.
4. In Classroom50, click **Regrade all**. It re-runs every student's latest
   submission (at the commit they submitted) with the new tests.
5. Optional: push the changed test to student repos too, so their Codespace
   shows the same tests. Until you do, their summaries will show the ⚠ line.

## Caveats

- **The bundle is public.** Everything in the autograder folder can be
  downloaded from the classroom's public Pages site. It is hidden from
  students' repos, not secret. **Never put solutions or answer keys there.**
  The sync command only copies tests and grading code from this repo; the
  reference solution lives in the private `csa2025-u2b-solution` repo.
- **Forged scores are detectable, not prevented.** Grading runs inside the
  student's repo, so a student could, in principle, write code that interferes
  with grading (for example, code that edits the build file or the test
  reports). The ⚠ line catches edited test files, but not everything. If a
  score looks wrong, open the student's repo at the graded commit and run
  `python3 grading/score.py` yourself.
- The autograder deletes the student's `target/` folder before running, so
  old compiled tests can't be reused.

## To verify during the try-out (#11)

Classroom50's docs don't say where some fields of the result come from. The
autograder fills them like this; check that they look right in Classroom50:

| Field | Taken from (first one that is set) |
|---|---|
| `classroom` | `CLASSROOM50_CLASSROOM`, then `bundle-info.json` |
| `assignment` | `CLASSROOM50_ASSIGNMENT`, then `bundle-info.json`, then the folder name |
| `submission` | `CLASSROOM50_SUBMISSION`, then `GITHUB_REPOSITORY`, then the checkout's folder name |
| `commit` | `CLASSROOM50_COMMIT`, then the checkout's git commit, then `GITHUB_SHA` |
| `release` | `CLASSROOM50_RELEASE`, then `GITHUB_REF_NAME`, else empty |
| `review` | `CLASSROOM50_REVIEW`, else empty |

Also check: Maven is available on the runner (above); `release-body.md`
(the per-part table) shows up for students; the one-line `summary=` written to
`$GITHUB_OUTPUT` displays sensibly. The autograder does not write `status=`,
because the allowed values aren't documented.

If any of these need changing, they are in `ENV_FIELDS` and `metadata()` near
the top of `grading/autograder.py`.
