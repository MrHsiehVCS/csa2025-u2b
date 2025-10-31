package u4pp;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOutNormalized;
import static com.github.stefanbirkner.systemlambda.SystemLambda.withTextFromSystemIn;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Scanner;

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

    @Test
    void execute_whenCalled_executesTheLoopsCorrectly() throws Exception {
        int timesExecuted = 6;
        withTextFromSystemIn("ask;ljasd", "y", "racecar", "Y", "monkey", "peanut", "n").execute(() -> {
            String output = tapSystemOutNormalized(() -> {
                Scanner sc = new Scanner(System.in);
                PalindromeTester.execute(sc);
            });
            assertEquals(timesExecuted, output.split("\n").length);
            // also assert num times "palindrome" is detected. 
        });
    }

    @Test
    void execute_whenCalledWithHappyPathLooping_handlesMultiplePhrases() throws Exception {
        // Test happy path: multiple valid inputs in a row
        withTextFromSystemIn("racecar", "y", "abccba", "Y", "hello", "y", "a", "n").execute(() -> {
            String output = tapSystemOutNormalized(() -> {
                Scanner sc = new Scanner(System.in);
                PalindromeTester.execute(sc);
            });
            String[] tokens = output.split("\n");
            
            System.out.println(output);
            // Should have welcome message + 4 phrases (3 palindromes + 1 non-palindrome) + 3 continuation prompts + final prompt
            assertTrue(tokens.length >= 6, "Should output at least welcome message, 4 phrases, and prompts");
            
            // Count palindrome detections
            int palindromeCount = 0;
            for (String tk : tokens) {
                if (tk.toLowerCase().contains("is a palindrome")) {
                    palindromeCount++;
                }
            }
            assertEquals(3, palindromeCount, "Should detect 3 palindromes: racecar, abccba, and a");
            
            // Count non-palindrome detections
            int nonPalindromeCount = 0;
            for (String tk : tokens) {
                if (tk.toLowerCase().contains("is not a palindrome")) {
                    nonPalindromeCount++;
                }
            }
            assertEquals(1, nonPalindromeCount, "Should detect 1 non-palindrome: hello");
        });
    }

    @Test
    void execute_whenCalledWithInvalidYesNoInput_handlesInvalidInput() throws Exception {
        // Test invalid input for yes/no selection
        withTextFromSystemIn("racecar", "invalid", "xyz", "maybe", "y", "abccba", "n").execute(() -> {
            String output = tapSystemOutNormalized(() -> {
                Scanner sc = new Scanner(System.in);
                PalindromeTester.execute(sc);
            });
            String[] tokens = output.split("\n");
            
            // Check that "Invalid Input" appears for invalid Y/N inputs
            boolean foundInvalidInput = false;
            for (String tk : tokens) {
                if (tk.toLowerCase().contains("invalid input")) {
                    foundInvalidInput = true;
                    break;
                }
            }
            assertTrue(foundInvalidInput, "Should display 'Invalid Input' when user enters invalid Y/N choice");
        });
    }

    @Test
    void execute_whenCalledWithSingleInput_identifiesCorrectly() throws Exception {
        // Test that a palindrome is correctly identified
        withTextFromSystemIn("racecar", "n").execute(() -> {
            String output = tapSystemOutNormalized(() -> {
                Scanner sc = new Scanner(System.in);
                PalindromeTester.execute(sc);
            });
            String[] tokens = output.split("\n");
            
            // Should contain "is a palindrome"
            boolean foundPalindrome = false;
            for (String tk : tokens) {
                if (tk.toLowerCase().contains("racecar") && tk.toLowerCase().contains("is a palindrome")) {
                    foundPalindrome = true;
                    break;
                }
            }
            assertTrue(foundPalindrome, "Should identify 'racecar' as a palindrome");
        });
        
        // Test that a non-palindrome is correctly identified
        withTextFromSystemIn("hello", "n").execute(() -> {
            String output = tapSystemOutNormalized(() -> {
                Scanner sc = new Scanner(System.in);
                PalindromeTester.execute(sc);
            });
            String[] tokens = output.split("\n");
            
            // Should contain "is not a palindrome"
            boolean foundNonPalindrome = false;
            for (String tk : tokens) {
                if (tk.toLowerCase().contains("hello") && tk.toLowerCase().contains("is not a palindrome")) {
                    foundNonPalindrome = true;
                    break;
                }
            }
            assertTrue(foundNonPalindrome, "Should identify 'hello' as not a palindrome");
        });
    }
}