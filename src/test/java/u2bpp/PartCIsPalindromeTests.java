package u2bpp;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

@DisplayName("Part C - isPalindrome")
class PartCIsPalindromeTests {

    private static final String CASE_HINT =
            "Likely cause: capital and lowercase letters are treated as different (the same phrase gives the "
            + "right answer when it is all lowercase). Make your comparisons ignore case, for example by calling "
            + "toLowerCase on the phrase first.";

    private static final String NON_LETTER_HINT =
            "Likely cause: spaces, digits, or symbols are not being skipped (the same phrase gives the right "
            + "answer with only its letters left). Only letters count; ignore every other character.";

    private static final String INDEX_HINT =
            "Likely cause: the loop compares the wrong characters. Check that the first letter is compared with "
            + "the last, the second with the second-to-last, and so on, and that no index is off by one.";

    /** Size of the performance test's palindrome: 26 * 2^9 letters, then mirrored. */
    private static final int PERFORMANCE_DOUBLINGS = 9;

    private static final int PERFORMANCE_TIMEOUT_SECONDS = 10;

    @Test
    @DisplayName("returns true for an odd-length palindrome in capital letters")
    void oddLengthPalindromeInCapitalLetters() {
        check("XYZYX", true, INDEX_HINT);
    }

    @Test
    @DisplayName("returns true for an even-length palindrome in capital letters")
    void evenLengthPalindromeInCapitalLetters() {
        check("QWERREWQ", true, INDEX_HINT);
    }

    @Test
    @DisplayName("returns true for an odd-length palindrome in lowercase letters")
    void oddLengthPalindromeInLowercaseLetters() {
        check("abcdcba", true, INDEX_HINT);
    }

    @Test
    @DisplayName("returns true for an even-length palindrome in lowercase letters")
    void evenLengthPalindromeInLowercaseLetters() {
        check("abccba", true, INDEX_HINT);
    }

    @Test
    @DisplayName("returns true for a long palindrome")
    void longPalindrome() {
        check("aaaabbccddeeddeeddggaaddggddaaaeeeeppeeeeaaaddggddaaggddeeddeeddccbbaaaa", true, INDEX_HINT);
    }

    @Test
    @DisplayName("ignores capitalization")
    void ignoresCapitalization() {
        assertAll("palindromes with a mix of capital and lowercase letters",
                () -> check("NeveroddorEven", true, CASE_HINT),
                () -> check("raCecAr", true, CASE_HINT));
    }

    @Test
    @DisplayName("ignores numbers, spaces, and symbols")
    void ignoresNumbersSpacesAndSymbols() {
        assertAll("palindromes with numbers, spaces, and symbols",
                () -> check("r98123 ace  137&&109c 00ar", true, NON_LETTER_HINT),
                () -> check("taco cat!", true, NON_LETTER_HINT));
    }

    @Test
    @DisplayName("returns false for phrases that are not palindromes")
    void returnsFalseForNonPalindromes() {
        String cause = "Likely cause: your code returns true without comparing every pair of letters, or it skips "
                + "letters that should be compared.";
        assertAll("phrases that are not palindromes",
                () -> check("thisISnoTApalindrome", false, cause),
                () -> check("thi@@isIsNot       a Palindrome", false, cause),
                () -> check("abca", false, cause));
    }

    @Test
    @DisplayName("returns true for an empty phrase or a phrase with no letters")
    void emptyOrLettersFreePhraseIsAPalindrome() {
        String cause = "Likely cause: your code returns false when there are no letters. With no letters, nothing "
                + "can be different, so it counts as a palindrome.";
        assertAll("phrases with no letters",
                () -> check("", true, cause),
                () -> check("!! 123 321 !!", true, cause));
    }

    @Test
    @DisplayName("is fast enough on a very long palindrome (performance)")
    void fastEnoughOnAVeryLongPalindrome() {
        String input = "abcdefghijklmnopqrstuvwxyz";
        for (int i = 0; i < PERFORMANCE_DOUBLINGS; i++) {
            input += input;
        }
        input += new StringBuilder(input).reverse().toString();
        String phrase = input;

        boolean actual = assertTimeoutPreemptively(Duration.ofSeconds(PERFORMANCE_TIMEOUT_SECONDS),
                () -> PalindromeTester.isPalindrome(phrase),
                () -> "isPalindrome did not finish within " + PERFORMANCE_TIMEOUT_SECONDS + " seconds on a "
                        + phrase.length() + "-character palindrome. A simple solution takes well under a second. "
                        + "Likely cause: your code repeats a lot of work, for example by building the letters-only "
                        + "string again inside a loop (or inside a loop condition). Build it once, save it in a "
                        + "variable, and then use that variable.");
        assertEquals(true, actual, "isPalindrome(" + show(phrase) + ") should return true but returned false. "
                + INDEX_HINT);
    }

    // ----- helpers -----

    private static void check(String phrase, boolean expected, String cause) {
        boolean actual = PalindromeTester.isPalindrome(phrase);
        if (actual != expected) {
            if (!phrase.equals(phrase.toLowerCase()) && givesAnswer(phrase.toLowerCase(), expected)) {
                cause = CASE_HINT;
            } else if (!phrase.equals(lettersOnly(phrase)) && givesAnswer(lettersOnly(phrase), expected)) {
                cause = NON_LETTER_HINT;
            }
        }
        assertEquals(expected, actual, "isPalindrome(" + show(phrase) + ") should return " + expected
                + " but returned " + actual + ". " + cause);
    }

    /** True if the student's isPalindrome gives the expected answer for this simpler phrase. */
    private static boolean givesAnswer(String phrase, boolean expected) {
        if (phrase.isEmpty()) {
            return false; // the empty phrase has its own test and hint
        }
        try {
            return PalindromeTester.isPalindrome(phrase) == expected;
        } catch (RuntimeException e) {
            return false; // ignore: we only use this to pick a hint
        }
    }

    private static String lettersOnly(String phrase) {
        String letters = "";
        for (int i = 0; i < phrase.length(); i++) {
            char c = phrase.charAt(i);
            if (Character.isLetter(c)) {
                letters += c;
            }
        }
        return letters;
    }

    /** The phrase in quotes, shortened if it is too long to read in a message. */
    private static String show(String phrase) {
        if (phrase.length() <= 80) {
            return "\"" + phrase + "\"";
        }
        return "\"" + phrase.substring(0, 30) + "..." + phrase.substring(phrase.length() - 30) + "\" ("
                + phrase.length() + " characters)";
    }
}
