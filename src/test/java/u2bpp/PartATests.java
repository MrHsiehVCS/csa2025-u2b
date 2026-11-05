package u2bpp;

/*
 * Test grouping convention (followed by every test file and by the score script):
 *
 *   One top-level test class per part. Its @DisplayName is what the Testing panel shows.
 *     Part A           -> class u2bpp.PartATests              "Part A - results"
 *     Part B           -> class u2bpp.PartBTests              "Part B - play"
 *     Part C-isPalindrome -> class u2bpp.PartCIsPalindromeTests "Part C - isPalindrome"
 *     Part C-execute   -> class u2bpp.PartCExecuteTests       "Part C - execute"
 *
 *   The score script maps a Surefire <testcase> to a part by its classname, using the
 *   part before any '$' (so a @Nested class inside PartATests still counts as Part A).
 *   Test methods get a plain-English @DisplayName that reads like a checklist item.
 */

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

@DisplayName("Part A - results")
class PartATests {

    private static final String CASE_HINT =
            "Likely cause: your code only works for capital letters or only for lowercase letters "
            + "(the same picks give the right answer when typed all in capitals or all in lowercase). Make your comparisons ignore case "
            + "(for example with equalsIgnoreCase, or by calling toUpperCase or toLowerCase first).";

    @Test
    @DisplayName("returns 1 when the player wins (capital letters)")
    void playerWinsWithCapitalLetters() {
        checkAll("player wins (capital letters)", new String[][] { { "R", "S" }, { "P", "R" }, { "S", "P" } }, 1,
                "Likely cause: a mistake in the win rules. Rock beats Scissors, Paper beats Rock, "
                + "and Scissors beats Paper.");
    }

    @Test
    @DisplayName("returns -1 when the player loses (capital letters)")
    void playerLosesWithCapitalLetters() {
        checkAll("player loses (capital letters)", new String[][] { { "R", "P" }, { "P", "S" }, { "S", "R" } }, -1,
                "Likely cause: a mistake in the lose rules, or the player and computer picks are swapped. "
                + "The first parameter is the player's pick.");
    }

    @Test
    @DisplayName("returns 0 when both pick the same (capital letters)")
    void tieWithCapitalLetters() {
        checkAll("tie (capital letters)", new String[][] { { "R", "R" }, { "P", "P" }, { "S", "S" } }, 0,
                "Likely cause: a tie (both picks the same) is not returning 0.");
    }

    @Test
    @DisplayName("works the same with lowercase letters (all 9 pairs)")
    void allPairsWithLowercaseLetters() {
        List<Executable> checks = new ArrayList<>();
        for (String[] pair : allPairs()) {
            String player = pair[0].toLowerCase();
            String computer = pair[1].toLowerCase();
            checks.add(() -> check(player, computer, expected(pair[0], pair[1]), CASE_HINT));
        }
        assertAll("results with lowercase letters", checks);
    }

    @Test
    @DisplayName("works the same with a mix of lowercase and capital letters")
    void mixedLowercaseAndCapitalLetters() {
        List<Executable> checks = new ArrayList<>();
        for (String[] pair : allPairs()) {
            String lowerPlayer = pair[0].toLowerCase();
            String lowerComputer = pair[1].toLowerCase();
            int expected = expected(pair[0], pair[1]);
            checks.add(() -> check(lowerPlayer, pair[1], expected, CASE_HINT));
            checks.add(() -> check(pair[0], lowerComputer, expected, CASE_HINT));
        }
        assertAll("results with mixed lowercase and capital letters", checks);
    }

    // ----- helpers -----

    /** All 9 (player, computer) pairs using capital letters. */
    private static String[][] allPairs() {
        String[] picks = { "R", "P", "S" };
        String[][] pairs = new String[9][];
        int i = 0;
        for (String p : picks) {
            for (String c : picks) {
                pairs[i] = new String[] { p, c };
                i++;
            }
        }
        return pairs;
    }

    /** The correct answer for capital-letter picks. */
    private static int expected(String player, String computer) {
        if (player.equals(computer)) {
            return 0;
        }
        String wins = player + computer;
        if (wins.equals("RS") || wins.equals("PR") || wins.equals("SP")) {
            return 1;
        }
        return -1;
    }

    private static void checkAll(String heading, String[][] pairs, int expected, String cause) {
        List<Executable> checks = new ArrayList<>();
        for (String[] pair : pairs) {
            checks.add(() -> check(pair[0], pair[1], expected, cause));
        }
        assertAll(heading, checks);
    }

    private static void check(String player, String computer, int expected, String cause) {
        int actual = RockPaperScissors.results(player, computer);
        if (actual != expected && worksInOtherCase(player, computer, expected)) {
            cause = CASE_HINT;
        }
        assertEquals(expected, actual, "results(\"" + player + "\", \"" + computer + "\") should return "
                + expected + " (" + meaning(expected) + ") but returned " + actual + ". " + cause);
    }

    /** True if the same picks give the right answer when both are all capital or all lowercase letters. */
    private static boolean worksInOtherCase(String player, String computer, int expected) {
        String[][] others = {
            { player.toUpperCase(), computer.toUpperCase() },
            { player.toLowerCase(), computer.toLowerCase() } };
        for (String[] other : others) {
            if (other[0].equals(player) && other[1].equals(computer)) {
                continue;
            }
            try {
                if (RockPaperScissors.results(other[0], other[1]) == expected) {
                    return true;
                }
            } catch (RuntimeException e) {
                // ignore: we only use this to pick a hint
            }
        }
        return false;
    }

    private static String meaning(int result) {
        if (result == 1) {
            return "player wins";
        } else if (result == -1) {
            return "player loses";
        } else if (result == 0) {
            return "tie";
        }
        return "not a valid result";
    }
}
