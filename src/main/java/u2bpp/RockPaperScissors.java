package u2bpp;

/**
* JAVADOC HERE
*/
import java.util.Scanner;

public class RockPaperScissors {

    /**
     * JAVADOC HERE
     */
    public static void play(Scanner sc) {
        /* PART B: YOUR CODE HERE
         * Follow the Output rules in the README exactly. The two prompts use print (each ends with ": "):
         *   "Would you like to pick (R)ock, (P)aper, (S)cissors, or (A)ny: "
         *   "Would you like to play again? (Y)es or (N)o: "
         * Every other message uses println:
         *   "Welcome to Rock Paper Scissors"
         *   "You both picked Rock. It's a tie!"
         *   "You picked Rock, and the Computer picked Paper. You lose!"
         *   "You have 0 wins and 1 losses and 1 ties"
         *   "Invalid input, please try again"
         *   "Thanks for playing!"
         */
    }

    /**
     * JAVADOC HERE
     */
    public static int results(String playerChoice, String computerChoice) {
        /* PART A: YOUR CODE HERE
         * Each pick is "R", "P" or "S", as a capital or lowercase letter (so "r" vs "P" must work).
         * Return 1 if the player wins, 0 for a tie, -1 if the player loses.
         */
        throw new UnsupportedOperationException("TODO: results is not written yet - delete this line and write your code");
    }

    /*
     * Helper methods for Part B go here. Make each one private static and give it a JavaDoc comment.
     *
     * Recommended helper methods (see the README for details):
     * 1. String getRandomChoice() - returns a random "r", "p", or "s"
     * 2. String getPlayerChoice(Scanner sc) - prompts until the player types R, P, S or A; returns "r", "p", "s" or "a"
     * 3. boolean getYesNoInput(Scanner sc, String prompt) - prompts until the player types Y or N; returns true for yes
     * 4. int playRound(Scanner sc) - plays one round and prints its result line; returns 1 (win), 0 (tie) or -1 (loss)
     * 5. String formatChoice(String choice) - turns "r" into "Rock", "p" into "Paper", and "s" into "Scissors"
     */
}