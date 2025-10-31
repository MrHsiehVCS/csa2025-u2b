package u4pp;

import static com.github.stefanbirkner.systemlambda.SystemLambda.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Scanner;

public class RockPaperScissorsTests {

    @Test
    void displaysScoreCorrectly() throws Exception {
        // Test with 100 games - generate input sequence: 100 games with "a" (random choice) and "y" to continue, then "n" to stop
        StringBuilder inputBuilder = new StringBuilder();
        for (int i = 0; i < 99; i++) {
            inputBuilder.append("a");
            inputBuilder.append(" ");
            inputBuilder.append("y");
            inputBuilder.append(" ");
        }
        inputBuilder.append("a");
        inputBuilder.append(" ");
        inputBuilder.append("n");
        
        withTextFromSystemIn(inputBuilder.toString().split(" ")).execute(() -> {
            String output = tapSystemOutNormalized(() -> {
                Scanner sc = new Scanner(System.in);
                RockPaperScissors.play(sc);
            });
            String[] tokens = output.split("\n");
            int[] winsLossesDraws = { 0, 0, 0 };
            for (String tk : tokens) {
                tk = tk.toLowerCase();
                if (tk.indexOf("you win") > 0) {
                    winsLossesDraws[0]++;
                } else if (tk.contains("you lose")) {
                    winsLossesDraws[1]++;
                } else if (tk.contains("it's a tie")) {
                    winsLossesDraws[2]++;
                }
            }

            // Find the last score line (before "Thanks for playing")
            String[] results = null;
            for (int i = tokens.length - 1; i >= 0; i--) {
                if (tokens[i].contains("wins") && tokens[i].contains("losses") && tokens[i].contains("ties")) {
                    results = tokens[i].split(" ");
                    break;
                }
            }
            
            assert results != null : "Could not find score line in output";
            
            int wins = Integer.parseInt(results[2]);
            int losses = Integer.parseInt(results[5]);
            int ties = Integer.parseInt(results[8]);
            
            assertEquals(winsLossesDraws[0], wins);
            assertEquals(winsLossesDraws[1], losses);
            assertEquals(winsLossesDraws[2], ties);
            
            // Verify that sum of wins/losses/ties equals number of games played (100)
            int totalGames = wins + losses + ties;
            assertEquals(100, totalGames, "Sum of wins, losses, and ties should equal 100 games played");
        });
    }

    @Test
    void gameLoopsCorrectly() throws Exception {
        withTextFromSystemIn("a", "y", "a", "y", "a", "y", "a", "n").execute(() -> {
            String output = tapSystemOutNormalized(() -> {
                Scanner sc = new Scanner(System.in);
                RockPaperScissors.play(sc);
            });
            String[] tokens = output.split("\n");
            assertEquals(10, tokens.length);
        });
    }

    @Test
    void handlesInvalidInputForRPS() throws Exception {
        // Test invalid input for rock/paper/scissors selection
        withTextFromSystemIn("invalid", "x", "z", "999", "r", "y", "badinput", "abc", "p", "n").execute(() -> {
            String output = tapSystemOutNormalized(() -> {
                Scanner sc = new Scanner(System.in);
                RockPaperScissors.play(sc);
            });
            String[] tokens = output.split("\n");
            
            // Check that "Invalid input" appears for invalid R/P/S inputs
            boolean foundInvalidInput = false;
            for (String tk : tokens) {
                if (tk.toLowerCase().contains("invalid input")) {
                    foundInvalidInput = true;
                    break;
                }
            }
            assertTrue(foundInvalidInput, "Should display 'Invalid input' when user enters invalid R/P/S choice");
            
            // Should eventually accept valid input and complete a game
            boolean foundGameResult = false;
            for (String tk : tokens) {
                String lower = tk.toLowerCase();
                if (lower.contains("you win") || lower.contains("you lose") || lower.contains("it's a tie")) {
                    foundGameResult = true;
                    break;
                }
            }
            assertTrue(foundGameResult, "Should eventually accept valid input and play a game");
        });
    }

    @Test
    void handlesInvalidInputForYesNo() throws Exception {
        // Test invalid input for yes/no selection
        withTextFromSystemIn("r", "invalid", "xyz", "maybe", "yes", "a", "Y", "a", "no", "N").execute(() -> {
            String output = tapSystemOutNormalized(() -> {
                Scanner sc = new Scanner(System.in);
                RockPaperScissors.play(sc);
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
    void results_withUppercaseInputs_returnsCorrectValues() {
        assertEquals(-1, RockPaperScissors.results("R", "P"));
        assertEquals(0, RockPaperScissors.results("R", "R"));
        assertEquals(1, RockPaperScissors.results("R", "S"));
        assertEquals(-1, RockPaperScissors.results("S", "R"));
        assertEquals(0, RockPaperScissors.results("S", "S"));
        assertEquals(1, RockPaperScissors.results("S", "P"));
        assertEquals(-1, RockPaperScissors.results("P", "S"));
        assertEquals(0, RockPaperScissors.results("P", "P"));
        assertEquals(1, RockPaperScissors.results("P", "R"));
    }

    @Test
    void results_withLowercaseInputs_returnsCorrectValues() {
        assertEquals(-1, RockPaperScissors.results("r", "p"));
        assertEquals(0, RockPaperScissors.results("r", "r"));
        assertEquals(1, RockPaperScissors.results("r", "s"));
        assertEquals(-1, RockPaperScissors.results("s", "r"));
        assertEquals(0, RockPaperScissors.results("s", "s"));
        assertEquals(1, RockPaperScissors.results("s", "p"));
        assertEquals(-1, RockPaperScissors.results("p", "s"));
        assertEquals(0, RockPaperScissors.results("p", "p"));
        assertEquals(1, RockPaperScissors.results("p", "r"));
    }
}