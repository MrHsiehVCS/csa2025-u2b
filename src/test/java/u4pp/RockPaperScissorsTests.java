package u4pp;

import static com.github.stefanbirkner.systemlambda.SystemLambda.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Scanner;

public class RockPaperScissorsTests {

    // TODO: extend displaysScoreCorrectly() to be much longer (100 games?) 
    //       also make it check that sum of win/loss/tie == number of games played
    // TODO: test for invalid input for y/n
    // TODO: test for invalid input for r/p/s

    @Test
    void displaysScoreCorrectly() throws Exception {
        withTextFromSystemIn("a", "y", "a", "y", "a", "y", "a", "n").execute(() -> {
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

            String[] results = tokens[tokens.length - 2].split(" ");
            
            assertEquals(winsLossesDraws[0], Integer.parseInt(results[2]));
            assertEquals(winsLossesDraws[1], Integer.parseInt(results[5]));
            assertEquals(winsLossesDraws[2], Integer.parseInt(results[8]));
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
    void results_returnsCorrectValues() {

        assertEquals(-1, RockPaperScissors.results("R", "P"));
        assertEquals(0, RockPaperScissors.results("R", "R"));
        assertEquals(1, RockPaperScissors.results("R", "S"));
        assertEquals(-1, RockPaperScissors.results("S", "R"));
        assertEquals(0, RockPaperScissors.results("S", "S"));
        assertEquals(1, RockPaperScissors.results("S", "P"));
        assertEquals(-1, RockPaperScissors.results("P", "S"));
        assertEquals(0, RockPaperScissors.results("P", "P"));
        assertEquals(1, RockPaperScissors.results("P", "R"));


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