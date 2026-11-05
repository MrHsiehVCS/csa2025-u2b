package u2bpp;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class PalindromeTesterTests {


    @Test
    void isPalindrome_whenCalledWithAllUpperCaseOddPalindrome_returnsTrue() {
        assertTrue(PalindromeTester.isPalindrome("XYZYX"));
    }

    @Test
    void isPalindrome_whenCalledWithAllUpperCaseEvenPalindrome_returnsTrue() {
        assertTrue(PalindromeTester.isPalindrome("QWERREWQ"));
    }

    @Test
    void isPalindrome_whenCalledWithAllLowerCaseOddPalindrome_returnsTrue() {
        assertTrue(PalindromeTester.isPalindrome("abcdcba"));
    }

    @Test
    void isPalindrome_whenCalledWithAllLowerCaseEvenPalindrome_returnsTrue() {
        assertTrue(PalindromeTester.isPalindrome("abccba"));
    }

    @Test
    void isPalindrome_whenCalledWithAllLowerCaseLongPalindrome_returnsTrue() {
        assertTrue(PalindromeTester.isPalindrome("aaaabbccddeeddeeddggaaddggddaaaeeeeppeeeeaaaddggddaaggddeeddeeddccbbaaaa"));
    }

    @Test
    void isPalindrome_whenCalledWithAllLowerCaseLongLongPalindrome_returnsTrue() {
        String input = "abcdefghijklmnopqrstuvwxyz";
        for(int i = 0; i < 4; i++) {
            input += input;
        }
        input += new StringBuilder(input).reverse().toString();
        // input.length() is 54,525,952
        assertTrue(PalindromeTester.isPalindrome(input));
    }

    @Test
    void isPalindrome_whenCalledWithCapitalizationDifferences_returnsTrue() {
        assertTrue(PalindromeTester.isPalindrome("Never odd or Even"));
        assertTrue(PalindromeTester.isPalindrome("raCecAr"));
    }

    @Test
    void isPalindrome_whenCalledWithAPalindromeThatHasNumbersSpacesAndSymbols_returnsTrue() {
        assertTrue(PalindromeTester.isPalindrome("r98123 ace  137&&109c 00AR"));
    }

    @Test
    void isPalindrome_whenCalledWithNonPalindromes_returnsFalse() {
        assertFalse(PalindromeTester.isPalindrome("thisISnoTApalindrome"));
        assertFalse(PalindromeTester.isPalindrome("thi@@isIsNot       a Palindrome"));
    }
}
