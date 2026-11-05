package u2bpp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/*
 * These tests type lines into execute(sc) one at a time, the way a person would. Each typed line
 * is shown in the captured output at the moment your code reads it, followed by a new line, just
 * like on screen. So the output the tests compare looks exactly like the README sample run:
 *
 *     Enter a phrase: racecar
 *     racecar is a palindrome
 */
@DisplayName("Part C - execute")
class PartCExecuteTests {

    private static final String PHRASE_PROMPT = "Enter a phrase: ";
    private static final String KEEP_PROMPT = "Keep testing? (Y)es or (N)o: ";
    private static final String INVALID = "Invalid input, please try again";

    /** The README "Part C Sample Run", character for character. */
    private static final String SAMPLE_RUN = String.join("\n",
            "Welcome to Palindrome Tester",
            "Enter a phrase: racecar",
            "racecar is a palindrome",
            "Keep testing? (Y)es or (N)o: asdfhsa;g",
            "Invalid input, please try again",
            "Keep testing? (Y)es or (N)o: Y",
            "Enter a phrase: ra^124c8098  eCA@R",
            "ra^124c8098  eCA@R is a palindrome",
            "Keep testing? (Y)es or (N)o: y",
            "Enter a phrase: isThisAPalindrome?",
            "isThisAPalindrome? is not a palindrome",
            "Keep testing? (Y)es or (N)o: N") + "\n";

    /** What the user types in the sample run. */
    private static final String[] SAMPLE_INPUT = {
        "racecar", "asdfhsa;g", "Y", "ra^124c8098  eCA@R", "y", "isThisAPalindrome?", "N" };

    @Test
    @DisplayName("matches the README sample run exactly")
    void matchesTheSampleRunExactly() {
        String screen = runToEnd(SAMPLE_INPUT);
        if (screen.equals(SAMPLE_RUN)) {
            return;
        }
        String[] expected = lines(SAMPLE_RUN);
        String[] actual = lines(screen);
        int i = 0;
        while (i < expected.length && i < actual.length && expected[i].equals(actual[i])) {
            i++;
        }
        String where;
        if (i < expected.length && i < actual.length) {
            where = "First difference on line " + (i + 1) + ":\n"
                    + "  expected: \"" + expected[i] + "\"\n"
                    + "  actual:   \"" + actual[i] + "\"\n";
        } else if (i < expected.length) {
            where = "Your output stops after line " + i + ". The next expected line is:\n"
                    + "  \"" + expected[i] + "\"\n";
        } else if (i < actual.length) {
            where = "Your output has extra lines after line " + i + ", starting with:\n"
                    + "  \"" + actual[i] + "\"\n";
        } else {
            where = "";
        }
        assertEquals(SAMPLE_RUN, screen, "With the sample-run input, your output should match the README "
                + "Part C sample run exactly, but it doesn't.\n" + where
                + "Likely cause: using println instead of print for a prompt (the typed answer should be on "
                + "the same line as the prompt), a spelling, capital letter, space or punctuation difference, "
                + "or an extra or missing line.\n");
    }

    @Test
    @DisplayName("prints prompts with print, so the typed answer is on the same line")
    void promptsUsePrint() {
        String screen = runToEnd("racecar", "n");
        assertHasLine(screen, PHRASE_PROMPT + "racecar",
                "the phrase prompt followed by the typed phrase on the same line",
                "Likely cause: printing \"Enter a phrase: \" with println instead of print, "
                + "or a spelling or spacing difference in the prompt.");
        assertHasLine(screen, KEEP_PROMPT + "n",
                "the keep-testing prompt followed by the typed answer on the same line",
                "Likely cause: printing \"Keep testing? (Y)es or (N)o: \" with println instead of print, "
                + "or a spelling or spacing difference in the prompt.");
    }

    @Test
    @DisplayName("tests one phrase and stops when the user answers n")
    void stopsWhenTheUserAnswersN() {
        String screen = runToEnd("racecar", "n");
        assertEquals(1, count(screen, PHRASE_PROMPT.trim()), "After the user typed \"racecar\" and then \"n\", "
                + "\"Enter a phrase:\" should be printed once but was printed " + count(screen, PHRASE_PROMPT.trim())
                + " times. Likely cause: your loop runs the wrong number of times." + show(screen));
    }

    @Test
    @DisplayName("keeps asking for phrases until the user answers n")
    void keepsAskingUntilN() {
        String screen = runToEnd("abba", "y", "level", "Y", "noon", "N");
        int prompts = count(screen, PHRASE_PROMPT.trim());
        assertEquals(3, prompts, "The user typed 3 phrases, answering \"y\", \"Y\" and then \"N\", so "
                + "\"Enter a phrase:\" should be printed 3 times but was printed " + prompts + " times. "
                + "Likely cause: the loop doesn't repeat when the answer is y or Y, or stops too early." + show(screen));
        int results = count(screen, " is a palindrome");
        assertEquals(3, results, "The user typed 3 phrases, so \"... is a palindrome\" should be printed 3 times "
                + "but was printed " + results + " times. Likely cause: the result is not printed every time "
                + "through the loop." + show(screen));
    }

    @Test
    @DisplayName("says \"<phrase> is a palindrome\" for a palindrome")
    void reportsAPalindrome() {
        String screen = runToEnd("racecar", "n");
        assertHasLine(screen, "racecar is a palindrome", "the result line for \"racecar\"",
                "Likely cause: a spelling, spacing or capital letter difference in the message.");
    }

    @Test
    @DisplayName("says \"<phrase> is not a palindrome\", showing the phrase exactly as typed")
    void reportsANonPalindromeWithThePhraseAsTyped() {
        String screen = runToEnd("Hello, World!", "n");
        assertHasLine(screen, "Hello, World! is not a palindrome", "the result line for \"Hello, World!\"",
                "Likely cause: printing a changed copy of the phrase (for example lowercase, or with "
                + "spaces and punctuation removed) instead of the phrase exactly as typed, or a spelling or "
                + "spacing difference in the message.");
    }

    @Test
    @DisplayName("re-asks \"Keep testing?\" after an invalid answer")
    void reasksAfterAnInvalidAnswer() {
        String screen = runToEnd("racecar", "maybe", "yes", "y", "abba", "n");
        int invalid = count(screen, INVALID);
        assertEquals(2, invalid, "The user answered \"maybe\" and then \"yes\" to \"Keep testing?\", so \""
                + INVALID + "\" should be printed 2 times but was printed " + invalid + " times. "
                + "Likely cause: only Y/y and N/n are valid answers; anything else should print \"" + INVALID
                + "\" and ask again, in a loop." + show(screen));
        int asked = count(screen, KEEP_PROMPT.trim());
        assertEquals(4, asked, "\"Keep testing?\" should be asked 4 times (twice after the invalid answers) "
                + "but was asked " + asked + " times. Likely cause: after an invalid answer the question is not "
                + "asked again." + show(screen));
        assertHasLine(screen, "abba is a palindrome", "the result for the second phrase, \"abba\"",
                "Likely cause: after the invalid answers and then \"y\", the program should keep testing.");
    }

    // ----- helpers -----

    /** Thrown when execute prints far more than it should (probably an endless loop). */
    private static class TooMuchOutput extends RuntimeException {
        TooMuchOutput() {
            super("too much output");
        }
    }

    /** Collects printed output, stopping a runaway loop that prints forever. */
    private static class Capture extends ByteArrayOutputStream {
        @Override
        public synchronized void write(int b) {
            check(1);
            super.write(b);
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) {
            check(len);
            super.write(b, off, len);
        }

        private void check(int extra) {
            if (size() + extra > 200_000) {
                throw new TooMuchOutput();
            }
        }
    }

    /** Hands the Scanner one typed line at a time, showing each line in the output when it is read. */
    private static class TypedInput implements Readable {
        private final String[] lines;
        private final PrintStream screen;
        private int used = 0;
        private String pending = "";

        TypedInput(String[] lines, PrintStream screen) {
            this.lines = lines;
            this.screen = screen;
        }

        @Override
        public int read(CharBuffer cb) {
            if (pending.isEmpty()) {
                if (used >= lines.length) {
                    return -1;
                }
                pending = lines[used] + "\n";
                used++;
                screen.print(pending);
                screen.flush();
            }
            int n = Math.min(cb.remaining(), pending.length());
            cb.put(pending, 0, n);
            pending = pending.substring(n);
            return n;
        }
    }

    /**
     * Runs execute with the given typed lines and returns what the screen shows. Fails with a
     * helpful message if execute asks for more input than was typed or throws an exception.
     */
    private static String runToEnd(String... typed) {
        Capture capture = new Capture();
        PrintStream out = new PrintStream(capture, true, StandardCharsets.UTF_8);
        PrintStream oldOut = System.out;
        InputStream oldIn = System.in;
        Throwable problem = null;
        System.setOut(out);
        System.setIn(new ByteArrayInputStream(new byte[0]));
        try {
            PalindromeTester.execute(new Scanner(new TypedInput(typed, out)));
        } catch (RuntimeException e) {
            problem = e;
        } finally {
            out.flush();
            System.setOut(oldOut);
            System.setIn(oldIn);
        }
        String screen = capture.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        String typedList = "\"" + String.join("\", \"", typed) + "\"";
        if (problem instanceof NoSuchElementException) {
            fail("execute tried to read more input after the user typed " + typedList + ". "
                    + "Likely cause: the loop doesn't stop when the user answers n or N, or you made a new "
                    + "Scanner instead of using the sc parameter." + show(screen));
        } else if (problem instanceof TooMuchOutput) {
            fail("execute printed a huge amount of output (probably an endless loop) after the user typed "
                    + typedList + ". Likely cause: a loop that never reads the next answer or never stops.");
        } else if (problem instanceof UnsupportedOperationException) {
            fail("execute stopped with \"" + problem.getMessage() + "\". Likely cause: isPalindrome is not "
                    + "written yet. Finish isPalindrome first." + show(screen));
        } else if (problem != null) {
            fail("execute crashed with " + problem + " after the user typed " + typedList + "." + show(screen),
                    problem);
        }
        return screen;
    }

    private static void assertHasLine(String screen, String line, String what, String cause) {
        for (String actual : lines(screen)) {
            if (actual.equals(line)) {
                return;
            }
        }
        fail("The output should contain " + what + ", exactly:\n  \"" + line + "\"\nbut it doesn't. "
                + cause + show(screen));
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

    private static String[] lines(String text) {
        return text.split("\n", -1);
    }

    private static String show(String screen) {
        return "\nYour output (typed input shown after each prompt):\n" + screen;
    }
}
