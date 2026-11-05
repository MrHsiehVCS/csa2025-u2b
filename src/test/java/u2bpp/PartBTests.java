package u2bpp;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withTextFromSystemIn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/*
 * Part B tests. Each test checks one feature, so one missing feature fails only its own test:
 *
 *   - Most tests look at the "message lines": the output with the two prompts removed, so a
 *     println-vs-print mistake only fails the prompts test.
 *   - Only the "Any" test types A. The others type explicit R/P/S picks.
 *   - The computer picks randomly, so every check accepts every possible computer pick.
 *     Tests that need to see a tie and a non-tie play 60 one-round games, so all three
 *     computer picks show up (the chance of missing one is about 1 in 10 billion).
 */
@DisplayName("Part B - play")
@ExtendWith(TimeLimitHint.class)
class PartBTests {

    private static final String WELCOME = "Welcome to Rock Paper Scissors";
    private static final String PICK_PROMPT = "Would you like to pick (R)ock, (P)aper, (S)cissors, or (A)ny: ";
    private static final String AGAIN_PROMPT = "Would you like to play again? (Y)es or (N)o: ";
    private static final String INVALID = "Invalid input, please try again";
    private static final String THANKS = "Thanks for playing!";
    private static final String SCORE_FORMAT = "You have W wins and L losses and T ties";

    private static final Pattern NON_TIE = Pattern.compile(
            "You picked (Rock|Paper|Scissors), and the Computer picked (Rock|Paper|Scissors)\\. You (win|lose)!");
    private static final Pattern TIE = Pattern.compile("You both picked (Rock|Paper|Scissors)\\. It's a tie!");
    private static final Pattern SCORE = Pattern.compile("You have (\\d+) wins and (\\d+) losses and (\\d+) ties");
    private static final Pattern PICK_NAME = Pattern.compile("(Rock|Paper|Scissors)");

    /** Number of one-round games played by tests that must see ties and non-ties. */
    private static final int MANY = 60;

    // ----- tests -----

    @Test
    @DisplayName("prints the welcome line first")
    void printsWelcomeLineFirst() throws Exception {
        Game game = play("r", "n");
        String first = game.rawLines.isEmpty() ? null : game.rawLines.get(0);
        String cause = "Likely cause: play should start with System.out.println(\"" + WELCOME + "\"); before anything else.";
        if (first == null) {
            cause = "Likely cause: play doesn't print anything yet.";
        } else if (first.startsWith(WELCOME) && !first.equals(WELCOME)) {
            cause = "Likely cause: the welcome line must end with a new line - use println, not print, for it.";
        }
        assertEquals(WELCOME, first, "The first line of output should be \"" + WELCOME + "\" but was "
                + quote(first) + ". " + cause + game.transcript());
    }

    @Test
    @DisplayName("shows the prompts exactly, using print so each answer is typed on the same line")
    void promptsUsePrint() throws Exception {
        Game game = play("r", "y", "p", "n");
        game.assertNoCrash("");
        String shape = "\nEach prompt should start its own line, and the next message should follow it on the same "
                + "line, like this:\n  " + PICK_PROMPT + "You picked Paper, and the Computer picked Rock. You win!\n  "
                + AGAIN_PROMPT + THANKS + "\n";
        List<String> raw = game.rawLines;
        for (int i = 0; i < raw.size(); i++) {
            String line = raw.get(i);
            boolean lastLine = i == raw.size() - 1 && !game.raw.endsWith("\n");
            for (String prompt : new String[] { PICK_PROMPT, AGAIN_PROMPT }) {
                if ((line.equals(prompt) || line.equals(prompt.trim())) && !lastLine) {
                    fail("A prompt is on a line by itself: " + quote(line) + ". Likely cause: the prompts use "
                            + "System.out.println. Prompts use System.out.print, so the player types their answer on "
                            + "the same line as the question (the test's typing is not shown in the output, so the "
                            + "next message continues that line)." + shape + game.transcript());
                }
            }
            // The player's answers are not shown, so a prompt may follow another prompt on the same line,
            // but it must never follow a message.
            String rest = line;
            boolean stripped = true;
            while (stripped) {
                stripped = false;
                for (String prompt : new String[] { PICK_PROMPT, AGAIN_PROMPT }) {
                    if (rest.startsWith(prompt)) {
                        rest = rest.substring(prompt.length());
                        stripped = true;
                    }
                }
            }
            if (rest.contains(PICK_PROMPT.trim()) || rest.contains(AGAIN_PROMPT.trim())) {
                fail("A prompt comes after a message on the same line: " + quote(line) + ". Likely cause: the "
                        + "message before the prompt is printed with print instead of println." + shape
                        + game.transcript());
            }
        }
        assertEquals(2, count(game.raw, PICK_PROMPT), "For the input r, y, p, n the exact pick prompt " + quote(PICK_PROMPT)
                + " should be shown 2 times but was shown " + count(game.raw, PICK_PROMPT) + " times. Likely cause: "
                + "the prompt is missing or its text differs (check spelling, capitals, punctuation and the space after the colon)."
                + game.transcript());
        assertEquals(2, count(game.raw, AGAIN_PROMPT), "For the input r, y, p, n the exact play-again prompt "
                + quote(AGAIN_PROMPT) + " should be shown 2 times but was shown " + count(game.raw, AGAIN_PROMPT)
                + " times. Likely cause: the prompt is missing or its text differs (check spelling, capitals, punctuation and the "
                + "space after the colon)." + game.transcript());
    }

    @Test
    @DisplayName("prints one result line and one score line per round, and nothing extra")
    void oneResultAndScoreLinePerRound() throws Exception {
        Game game = play("r", "y", "p", "n");
        game.assertNoCrash("");
        for (String line : game.rawLines) {
            if (line.trim().isEmpty()) {
                fail("Your output has a blank line. Likely cause: an extra System.out.println() or a \"\\n\" in a "
                        + "message." + game.transcript());
            }
        }
        List<String> middle = new ArrayList<>(game.lines);
        if (!middle.isEmpty() && middle.get(0).trim().equals(WELCOME)) {
            middle.remove(0);
        }
        if (!middle.isEmpty() && middle.get(middle.size() - 1).trim().equals(THANKS)) {
            middle.remove(middle.size() - 1);
        }
        List<String> kinds = new ArrayList<>();
        for (String line : middle) {
            if (line.startsWith("You have")) {
                kinds.add("score line");
            } else if (line.startsWith("You ")) {
                kinds.add("round result");
            } else {
                kinds.add("extra line " + quote(line));
            }
        }
        assertEquals(List.of("round result", "score line", "round result", "score line"), kinds,
                "For 2 rounds (input r, y, p, n), between the welcome line and \"" + THANKS + "\" your program "
                + "should print exactly: a round result, a score line, a round result, a score line (the prompts "
                + "don't count). It printed: " + kinds + ". Likely cause: an extra message (such as a leftover "
                + "debugging print), a missing score line, or a message printed twice." + game.transcript());
    }

    @Test
    @DisplayName("plays a round with the pick you typed (R, P or S, capital or lowercase)")
    void playsRoundWithTypedPick() throws Exception {
        for (String pick : new String[] { "r", "R", "p", "P", "s", "S" }) {
            Game game = play(pick, "n");
            String name = name(pick);
            game.assertNoCrash("Likely cause: " + pick + " was not accepted as a pick. R, P and S must work as "
                    + "capital or lowercase letters.");
            String round = game.firstRoundLine(pick);
            assertTrue(round.startsWith("You picked " + name + ", ") || round.startsWith("You both picked " + name + "."),
                    "After typing " + pick + " the round result should start with \"You picked " + name
                    + ", \" (or \"You both picked " + name + ".\" for a tie) but was " + quote(round)
                    + ". Likely cause: the player's letter is turned into the wrong name, or capital and lowercase "
                    + "letters are not treated the same." + game.transcript());
        }
    }

    @Test
    @DisplayName("says You win! or You lose! with both picks when the picks are different")
    void nonTieWording() throws Exception {
        int checked = 0;
        for (int i = 0; i < MANY; i++) {
            String pick = "rps".substring(i % 3, i % 3 + 1);
            Game game = play(pick, "n");
            game.assertNoCrash("");
            String round = game.firstRoundLine(pick);
            if (looksLikeTie(round)) {
                continue; // ties are checked by the tie test
            }
            checked++;
            String expectedShape = "You picked " + name(pick) + ", and the Computer picked <Rock/Paper/Scissors>. You win!"
                    + " (or You lose!)";
            Matcher m = NON_TIE.matcher(round);
            assertTrue(m.matches(), "When the picks are different the round result should look exactly like \""
                    + expectedShape + "\" but was " + quote(round) + ". Likely cause: a spelling, capital letter, "
                    + "comma or period differs from the sample run." + game.transcript());
            String player = m.group(1);
            String computer = m.group(2);
            String ending = "You " + m.group(3) + "!";
            String correct = outcome(player, computer) == 1 ? "You win!" : "You lose!";
            assertEquals(correct, ending, player + " against " + computer + " should end with \"" + correct
                    + "\" but your line was " + quote(round) + ". Likely cause: the win and lose messages are swapped, "
                    + "the player and computer picks are passed to results in the wrong order, or results (Part A) "
                    + "gives the wrong answer for lowercase or capital letters." + game.transcript());
        }
        assertTrue(checked > 0, "None of the " + MANY + " rounds was a non-tie, which should be almost impossible. "
                + "Likely cause: the computer's pick is not random, or every round is reported as a tie.");
    }

    @Test
    @DisplayName("says It's a tie! when both picks are the same")
    void tieWording() throws Exception {
        int checked = 0;
        for (int i = 0; i < MANY; i++) {
            String pick = "rps".substring(i % 3, i % 3 + 1);
            Game game = play(pick, "n");
            game.assertNoCrash("");
            String round = game.firstRoundLine(pick);
            if (!looksLikeTie(round)) {
                continue; // non-ties are checked by the non-tie test
            }
            checked++;
            String expected = "You both picked " + name(pick) + ". It's a tie!";
            String cause = "Likely cause: a spelling, capital letter, apostrophe or period differs from the sample run "
                    + "(it is \"It's\" with an apostrophe).";
            if (round.startsWith("You picked")) {
                cause = "Likely cause: a tie is printed with the win/lose wording. When results returns 0, print the tie line.";
            }
            assertEquals(expected, round, "When both pick " + name(pick) + " the round result should be exactly \""
                    + expected + "\" but was " + quote(round) + ". " + cause + game.transcript());
        }
        assertTrue(checked > 0, "None of the " + MANY + " rounds was a tie, which should be almost impossible. "
                + "Likely cause: the computer's pick is not random, or ties are not recognised.");
    }

    @Test
    @DisplayName("picking A (Any) plays a round with a random pick for you")
    void anyPlaysRoundWithRandomPick() throws Exception {
        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < MANY; i++) {
            String pick = i % 2 == 0 ? "a" : "A";
            Game game = play(pick, "n");
            game.assertNoCrash("Likely cause: " + pick + " (Any) is not accepted as a pick, so your program printed "
                    + "an invalid-input message and asked again. A or a must be accepted.");
            String round = game.firstRoundLine(pick);
            Matcher m = Pattern.compile("You (?:both )?picked (\\w+)").matcher(round);
            String shown = m.lookingAt() ? m.group(1) : null;
            assertTrue("Rock".equals(shown) || "Paper".equals(shown) || "Scissors".equals(shown),
                    "After typing " + pick + " (Any) the round result should start with \"You picked \" and then "
                    + "Rock, Paper or Scissors, but was " + quote(round) + ". Likely cause: Any must be replaced by a "
                    + "random R, P or S (the same way the computer picks) before the round is played." + game.transcript());
            seen.add(shown);
        }
        assertTrue(seen.size() > 1, "In " + MANY + " rounds with A (Any), your pick was always " + seen
                + ". Any should pick randomly each round, so all three picks should show up. Likely cause: A is "
                + "never replaced by a random pick, or the random pick is made only once.");
    }

    @Test
    @DisplayName("an invalid pick prints Invalid input, please try again and asks again")
    void invalidPickAsksAgain() throws Exception {
        Game game = play("x", "hello", "7", "r", "n");
        game.assertNoCrash("Likely cause: after an invalid pick your program did not ask again for a pick.");
        List<String> before = game.linesBeforeFirstRound();
        // Only the invalid-input messages are checked here; other extra lines fail the one-line-per-round test.
        before.removeIf(line -> !line.toLowerCase().contains("invalid"));
        List<String> expected = List.of(INVALID, INVALID, INVALID);
        String cause;
        if (before.isEmpty()) {
            cause = "Likely cause: invalid picks are not checked, so x was played as a round. Use a loop that keeps "
                    + "asking until the player types R, P, S or A.";
        } else if (before.size() == 3) {
            cause = "Likely cause: the message text differs. It must be exactly \"" + INVALID + "\".";
        } else {
            cause = "Likely cause: the message is printed the wrong number of times.";
        }
        assertEquals(expected, before, "For the picks x, hello, 7 and then r, your program should print \"" + INVALID
                + "\" three times (once per invalid pick) before the round. " + cause + game.transcript());
        assertEquals(4, count(game.raw, "(R)ock"), "For the picks x, hello, 7 and then r, the pick prompt should be "
                + "shown 4 times but was shown " + count(game.raw, "(R)ock") + " times. Likely cause: the prompt is "
                + "not printed again inside the loop that re-asks." + game.transcript());
        assertTrue(game.firstRoundLine("r").contains("Rock"), "After the valid pick r the round should use Rock, "
                + "but was " + quote(game.firstRoundLine("r")) + ". Likely cause: the round uses one of the invalid "
                + "inputs instead of the last (valid) one." + game.transcript());
    }

    @Test
    @DisplayName("an invalid Y/N answer prints Invalid input, please try again and asks again")
    void invalidYesNoAsksAgain() throws Exception {
        Game game = play("r", "maybe", "7", "y", "p", "n");
        game.assertNoCrash("Likely cause: after an invalid play-again answer your program did not ask the "
                + "play-again question again.");
        List<String> rounds = game.roundLines();
        assertEquals(2, rounds.size(), "For the input r, maybe, 7, y, p, n your program should play 2 rounds but "
                + "played " + rounds.size() + ". Likely cause: an answer other than Y or N should print \"" + INVALID
                + "\" and ask again, not end the game (or start a round)." + game.transcript());
        List<String> between = game.linesBetween(rounds.get(0), rounds.get(1));
        // Only the invalid-input messages are checked here; other extra lines fail the one-line-per-round test.
        between.removeIf(line -> !line.toLowerCase().contains("invalid"));
        assertEquals(List.of(INVALID, INVALID), between, "For the answers maybe and 7 your program should print \""
                + INVALID + "\" twice between the two rounds (after the score line). Likely cause: the message text "
                + "differs, or it is printed the wrong number of times." + game.transcript());
        assertEquals(4, count(game.raw, "(Y)es"), "For the answers maybe, 7, y and n the play-again prompt should "
                + "be shown 4 times but was shown " + count(game.raw, "(Y)es") + " times. Likely cause: the prompt "
                + "is not printed again inside the loop that re-asks." + game.transcript());
        assertTrue(rounds.get(1).contains("Paper"), "After y the second round should use the pick p (Paper), but was "
                + quote(rounds.get(1)) + "." + game.transcript());
    }

    @Test
    @DisplayName("stops right away when the player answers n or N")
    void quitsOnFirstNo() throws Exception {
        for (String no : new String[] { "n", "N" }) {
            Game game = play("s", no);
            game.assertNoCrash("Likely cause: after " + no + " your program kept going and asked for more input. "
                    + "Both n and N should end the game.");
            assertEquals(1, game.roundLines().size(), "For the input s, " + no + " your program should play exactly 1 "
                    + "round but played " + game.roundLines().size() + ". Likely cause: the loop does not stop when "
                    + "the answer is " + no + "." + game.transcript());
            assertEquals(1, count(game.raw, "(R)ock"), "For the input s, " + no + " the pick prompt should be shown "
                    + "once but was shown " + count(game.raw, "(R)ock") + " times. Likely cause: the loop starts "
                    + "another round after " + no + "." + game.transcript());
        }
    }

    @Test
    @DisplayName("prints the score line exactly: You have W wins and L losses and T ties")
    void scoreLineFormat() throws Exception {
        Game game = play("r", "n");
        game.assertNoCrash("");
        String round = game.firstRoundLine("r");
        List<String> after = game.linesAfter(round);
        String score = after.isEmpty() ? null : after.get(0);
        assertTrue(score != null && SCORE.matcher(score).matches(), "The line right after the round result should be "
                + "the score, exactly in the form \"" + SCORE_FORMAT + "\" (for example \"You have 1 wins and 0 "
                + "losses and 0 ties\"), but was " + quote(score) + ". Likely cause: different words, commas, or "
                + "spacing; or the score is not printed after every round. Always use \"wins\", \"losses\" and "
                + "\"ties\", even for 1." + game.transcript());
    }

    @Test
    @DisplayName("the score counts match the rounds played")
    void scoreCountsMatchRounds() throws Exception {
        int rounds = 30;
        List<String> input = new ArrayList<>();
        for (int i = 0; i < rounds; i++) {
            input.add("rps".substring(i % 3, i % 3 + 1));
            input.add(i < rounds - 1 ? "y" : "n");
        }
        Game game = play(input.toArray(new String[0]));
        game.assertNoCrash("");
        // The score after each round must be the previous score plus 1 win, loss or tie. Either the outcome
        // the result line says (You win!/You lose!/tie) or the outcome of the picks it names is accepted, so a
        // wrong win/lose message or a Part A bug fails only the tests about those.
        List<Integer> previous = List.of(0, 0, 0);
        Set<Integer> allowed = new LinkedHashSet<>();
        String lastRound = null;
        int played = 0;
        int scores = 0;
        for (String line : game.lines) {
            if (line.startsWith("You have")) {
                scores++;
                List<Integer> numbers = new ArrayList<>();
                Matcher m = Pattern.compile("\\d+").matcher(line);
                while (m.find()) {
                    numbers.add(Integer.parseInt(m.group()));
                }
                List<List<Integer>> ok = new ArrayList<>();
                for (int result : allowed) {
                    ok.add(plusOne(previous, result));
                }
                if (!ok.contains(numbers)) {
                    List<Integer> want = ok.isEmpty() ? previous : ok.get(0);
                    fail("After round " + played + " (" + quote(lastRound) + ") the score should go from "
                            + describe(previous) + " to " + describe(want) + ", but your score line was "
                            + quote(line) + ". Likely cause: a counter is not updated, the wrong counter is "
                            + "updated, or the counters are reset each round (create them once, before the loop, "
                            + "in play)." + game.transcript());
                }
                previous = numbers;
                allowed.clear();
            } else if (line.startsWith("You ")) {
                played++;
                lastRound = line;
                allowed.clear();
                for (int result : new int[] { outcomeOf(line), wordingOutcome(line) }) {
                    if (result != 2) {
                        allowed.add(result);
                    }
                }
                if (allowed.isEmpty()) {
                    fail("Could not tell who won from the round result " + quote(line) + ". It should name both "
                            + "picks and say who won, as in the sample run." + game.transcript());
                }
            }
        }
        assertEquals(rounds, played, "For " + rounds + " rounds (answering y " + (rounds - 1) + " times, then n), your "
                + "program should play " + rounds + " rounds but played " + played + ". Likely cause: the loop "
                + "stops too early or too late." + game.transcript());
        assertEquals(rounds, scores, "Your program should print a score line after each of the " + rounds
                + " rounds, but printed " + scores + ". Likely cause: the score is printed only once, or not every "
                + "round." + game.transcript());
    }

    @Test
    @DisplayName("prints Thanks for playing! as the last line")
    void thanksForPlayingLast() throws Exception {
        Game game = play("r", "n");
        game.assertNoCrash("");
        String last = game.lines.isEmpty() ? null : game.lines.get(game.lines.size() - 1);
        String cause = "Likely cause: print \"" + THANKS + "\" with println after the game loop ends.";
        if (last != null && last.trim().equals(THANKS)) {
            cause = "Likely cause: extra spaces around the message. It must be exactly \"" + THANKS
                    + "\" with no space at the end.";
        }
        assertEquals(THANKS, last, "The last line should be exactly \"" + THANKS + "\" but was " + quote(last)
                + ". " + cause + game.transcript());
    }

    // ----- helpers -----

    /** One call of play: what was typed, what was printed, and what (if anything) was thrown. */
    private static final class Game {
        final String[] input;
        final String raw;
        /** The output split into lines, exactly as printed. */
        final List<String> rawLines;
        /** The message lines: output lines with the prompts removed, and empty lines left out. */
        final List<String> lines;
        final Throwable error;

        Game(String[] input, String raw, Throwable error) {
            this.input = input;
            this.raw = raw;
            this.error = error;
            this.rawLines = new ArrayList<>();
            this.lines = new ArrayList<>();
            for (String line : raw.isEmpty() ? new String[0] : raw.split("\n")) {
                rawLines.add(line);
                // Every prompt ends with ": " and no other message has a colon, so the message part of a
                // line is whatever follows the last colon (and the space after it).
                int colon = line.lastIndexOf(':');
                if (colon >= 0) {
                    line = line.substring(colon + 1);
                    if (line.startsWith(" ")) {
                        line = line.substring(1);
                    }
                }
                if (!line.trim().isEmpty()) {
                    lines.add(line);
                }
            }
        }

        /** Fails with a helpful message if play threw an exception or printed far too much. */
        void assertNoCrash(String cause) {
            if (error == null) {
                return;
            }
            String what;
            if (error instanceof NoSuchElementException) {
                what = "Your program asked for more input than the test typed (it typed " + String.join(", ", input)
                        + "), so the Scanner threw " + error.getClass().getSimpleName() + ". ";
            } else if (error instanceof TooMuchOutput) {
                what = "Your program printed more than " + TooMuchOutput.LIMIT + " characters, so the test stopped it. "
                        + "Likely cause: a loop that never ends (for example, one that never reads the next input). ";
            } else if (error instanceof UnsupportedOperationException) {
                what = "Your program threw " + error + ". ";
                cause = "Likely cause: results (Part A) is not written yet. play uses it to decide each round.";
            } else {
                what = "Your program threw " + error + ". ";
            }
            fail(what + cause + transcript());
        }

        /** Message lines that are round results, such as "You picked Rock, and the Computer picked Paper. You lose!". */
        List<String> roundLines() {
            List<String> rounds = new ArrayList<>();
            for (String line : lines) {
                if (line.startsWith("You ") && !line.startsWith("You have")) {
                    rounds.add(line);
                }
            }
            return rounds;
        }

        /** The first round result; fails if there is none. */
        String firstRoundLine(String pick) {
            List<String> rounds = roundLines();
            if (rounds.isEmpty()) {
                fail("After the pick " + pick + " your program should print a round result such as \"You picked Rock, "
                        + "and the Computer picked Paper. You lose!\" or \"You both picked Rock. It's a tie!\", but no "
                        + "line starts with \"You picked\" or \"You both picked\". Likely cause: the round is not played, "
                        + "or its result is worded differently from the sample run." + transcript());
            }
            return rounds.get(0);
        }

        List<String> linesBeforeFirstRound() {
            List<String> rounds = roundLines();
            int end = rounds.isEmpty() ? lines.size() : lines.indexOf(rounds.get(0));
            return new ArrayList<>(lines.subList(0, end));
        }

        List<String> linesAfter(String line) {
            int start = lines.indexOf(line) + 1;
            return new ArrayList<>(lines.subList(start, lines.size()));
        }

        List<String> linesBetween(String first, String second) {
            int start = lines.indexOf(first) + 1;
            int end = lines.subList(start, lines.size()).indexOf(second) + start;
            return new ArrayList<>(lines.subList(start, end));
        }

        /** The input and output, for the end of a failure message. */
        String transcript() {
            StringBuilder sb = new StringBuilder("\n\nThe test typed: " + String.join(", ", input) + "\nYour output was:\n");
            if (raw.isEmpty()) {
                sb.append("  (nothing)\n");
            }
            int shown = 0;
            for (String line : rawLines) {
                if (shown == 40) {
                    sb.append("  ... (" + (rawLines.size() - shown) + " more lines)\n");
                    break;
                }
                sb.append("  ").append(line).append("\n");
                shown++;
            }
            return sb.toString();
        }
    }

    /** Thrown by the output capture when play prints far too much (an endless loop). */
    private static final class TooMuchOutput extends RuntimeException {
        static final int LIMIT = 100_000;

        TooMuchOutput() {
            super("more than " + LIMIT + " characters of output");
        }
    }

    /** Runs play with the given lines as keyboard input and captures what it prints. */
    private static Game play(String... input) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        OutputStream capped = new OutputStream() {
            @Override
            public void write(int b) {
                if (bytes.size() >= TooMuchOutput.LIMIT) {
                    throw new TooMuchOutput();
                }
                bytes.write(b);
            }
        };
        Throwable[] error = new Throwable[1];
        PrintStream original = System.out;
        withTextFromSystemIn(input).execute(() -> {
            System.setOut(new PrintStream(capped, true, StandardCharsets.UTF_8));
            try {
                RockPaperScissors.play(new Scanner(System.in));
            } catch (RuntimeException e) {
                error[0] = e;
            } finally {
                System.out.flush();
                System.setOut(original);
            }
        });
        String raw = bytes.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        return new Game(input, raw, error[0]);
    }

    private static boolean looksLikeTie(String round) {
        if (round.startsWith("You both picked") || round.toLowerCase().contains("tie")) {
            return true;
        }
        List<String> names = names(round);
        return names.size() == 2 && names.get(0).equals(names.get(1));
    }

    /** 1, 0 or -1 for a round result line, worked out from the picks it names; 2 if it can't tell. */
    private static int outcomeOf(String round) {
        List<String> names = names(round);
        if (round.startsWith("You both picked") && names.size() == 1) {
            return 0;
        }
        if (names.size() == 2) {
            return outcome(names.get(0), names.get(1));
        }
        return 2;
    }

    /** 1, 0 or -1 from the words of a round result line (win, lose, tie); 2 if it has none of them. */
    private static int wordingOutcome(String round) {
        String lower = round.toLowerCase();
        if (lower.contains("tie")) {
            return 0;
        } else if (lower.contains("you win")) {
            return 1;
        } else if (lower.contains("you lose")) {
            return -1;
        }
        return 2;
    }

    /** The score [wins, losses, ties] after adding one round with the given result. */
    private static List<Integer> plusOne(List<Integer> score, int result) {
        List<Integer> next = new ArrayList<>(score);
        int index = result == 1 ? 0 : result == -1 ? 1 : 2;
        next.set(index, next.get(index) + 1);
        return next;
    }

    private static String describe(List<Integer> score) {
        return score.get(0) + " wins, " + score.get(1) + " losses, " + score.get(2) + " ties";
    }

    private static List<String> names(String line) {
        List<String> names = new ArrayList<>();
        Matcher m = PICK_NAME.matcher(line);
        while (m.find()) {
            names.add(m.group());
        }
        return names;
    }

    /** The correct result for two pick names: 1 if the player wins, 0 for a tie, -1 if the player loses. */
    private static int outcome(String player, String computer) {
        if (player.equals(computer)) {
            return 0;
        }
        String pair = player + " " + computer;
        if (pair.equals("Rock Scissors") || pair.equals("Paper Rock") || pair.equals("Scissors Paper")) {
            return 1;
        }
        return -1;
    }

    private static String name(String pick) {
        switch (pick.toLowerCase()) {
            case "r":
                return "Rock";
            case "p":
                return "Paper";
            default:
                return "Scissors";
        }
    }

    private static int count(String text, String part) {
        int n = 0;
        int i = text.indexOf(part);
        while (i >= 0) {
            n++;
            i = text.indexOf(part, i + part.length());
        }
        return n;
    }

    private static String quote(String s) {
        return s == null ? "(no line)" : "\"" + s + "\"";
    }
}
