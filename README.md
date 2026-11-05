# Unit 2b Programming Project

For this project, you will be completing the code for two classes: `RockPaperScissors` and `PalindromeTester`. Each of these classes have some code that has already been written for you. You should not change any of the provided code. Instead, you should add in your own code in order to fulfill the project requirements. You will also need to JavaDoc all your classes.

## About the starter code

- **It compiles.** A method that promises to return a value (like `results`, which returns an `int`) won't compile unless it returns something. So instead of being left blank, `results` and `isPalindrome` start with this line:

  ```java
  throw new UnsupportedOperationException("TODO: ... delete this line and write your code");
  ```

  That line lets the project compile and run before you've written anything. `play` and `execute` don't return a value, so they are simply empty.
- **Every test starts red.** Since the starter methods don't do anything yet, every test fails at first. If you run a test on `results` or `isPalindrome` now, it fails with that "not written yet" message. This is expected, not a broken project. A test only turns green when your code makes it pass.
- **Delete the `throw` line** when you start writing a method, then write your code in its place.
- To try your code by hand, open `Main.java` and uncomment **one** block at a time.

## How to write a method

You will learn about writing methods next unit, but this project asks you to write a few small **helper methods** now. Here is a fully worked example that has nothing to do with this project, so you can see every piece. Copy the *shape* of it, not the code.

```java
/**
 * A small program that works out the tip on a restaurant bill.
 */
public class TipCalculator {

    public static void main(String[] args) {
        double tip = calculateTip(40.0, 15);   // call the method; the answer comes back into tip
        System.out.println("Tip: $" + tip);    // prints Tip: $6.0
    }

    /**
     * Calculates the tip for a bill.
     *
     * @param bill    the total of the bill, in dollars
     * @param percent the tip percentage, such as 15 for 15%
     * @return the tip amount, in dollars
     */
    private static double calculateTip(double bill, int percent) {
        double tip = bill * percent / 100.0;
        return tip;
    }
}
```

The parts of the method:

- **`private static`** - write this at the start of every helper method in this project. `private` means only this class can use it (so it doesn't count as an extra public method), and `static` means you call it without making an object.
- **`double`** - the *return type*: what kind of value the method gives back. Use `int`, `boolean`, `String`, etc. as needed, or `void` if the method only prints and gives nothing back.
- **`calculateTip`** - the method's name. Start with a verb and use camelCase.
- **`(double bill, int percent)`** - the *parameters*: values the method needs, each with a type and a name. Use `()` if it needs none.
- **`return tip;`** - sends the answer back to whoever called the method. A non-`void` method must always reach a `return`.
- **The call** `calculateTip(40.0, 15)` - runs the method with `bill = 40.0` and `percent = 15`, and the call is replaced by the returned value.

**JavaDoc** is the `/** ... */` comment right above a class or method. This is the commenting the rubric expects. For every method, write one sentence saying what it does, then:

- one `@param name description` line for **each** parameter (skip if there are none), and
- one `@return description` line saying what it gives back (skip for `void` methods).

Every class also gets a JavaDoc comment above it, like the one above `TipCalculator`.

## Part A - RockPaperScissors.java - results()

The method `results` should accept player and computer input (`"R"`, `"P"` or `"S"`, lowercase or capital), and return an integer: `1` if the player wins, `0` if it is a tie, and `-1` if the player loses.

For example, `results("R", "S")` returns `1` (Rock beats Scissors), `results("p", "S")` returns `-1` (Scissors beats Paper), and `results("s", "S")` returns `0`. The first parameter is always the player's pick.

This part has **5 tests**, in the **Part A - results** group of the Testing panel:

| Test | What it checks |
|---|---|
| returns 1 when the player wins (capital letters) | `R` vs `S`, `P` vs `R`, `S` vs `P` all return `1` |
| returns -1 when the player loses (capital letters) | `R` vs `P`, `P` vs `S`, `S` vs `R` all return `-1` |
| returns 0 when both pick the same (capital letters) | `R` vs `R`, `P` vs `P`, `S` vs `S` all return `0` |
| works the same with lowercase letters (all 9 pairs) | the same 9 pairs typed in lowercase, such as `"r"` vs `"s"` |
| works the same with a mix of lowercase and capital letters | one pick lowercase and the other capital, such as `"r"` vs `"P"` |

When a test fails, its message shows the call, what it should return, what your code returned, and a likely cause.

## Part B - RockPaperScissors.java - play()

For **Part B** you are going to complete the method `play` in `RockPaperScissors`. It plays a game of Rock-Paper-Scissors against the computer. The completed method will do the following:

1. Prompt the user to select *Rock*, *Paper*, or *Scissors*.
   - Users can choose *Rock* by inputting either *R* or *r*.
   - Users can choose *Paper* by inputting either *P* or *p*.
   - Users can choose *Scissors* by inputting either *S* or *s*.
   - Users can choose *Any* by inputting either *A* or *a*. If a user selects *Any* then a random selection is made for them, the same way it is done for the computer.
2. Have the computer secretly select one of the three options, randomly.
3. Using the `results` method from Part A, output both selections and whether the user wins, loses, or ties.
4. Keep track of and display how many times the user has won, lost and tied.
5. Keep playing the game until the user chooses to stop.

### Part B - Implementation order

1. Create a helper method that returns a random choice: `"r"`, `"p"`, or `"s"`.
2. Use the Scanner to read the player's pick and the helper method to make the computer's pick, then use the `results` method from Part A to find out who won (`1`, `0` or `-1`).
3. Turn that `int` into the right message, for example:
   - `You both picked Rock. It's a tie!`
   - `You picked Rock, and the Computer picked Paper. You lose!`
4. Add a loop that repeats this over and over, asking the user if they want to continue.
   - Have the loop exit based on the user's answer.
   - The question is `Would you like to play again? (Y)es or (N)o: `
5. Add three `int` variables to keep track of the wins, losses and ties.
   - Print them after every round, in exactly this format: `You have 0 wins and 3 losses and 1 ties`
6. Check for valid input. If the user types something that isn't a valid answer, print `Invalid input, please try again` and ask the same question again.
   - This needs a loop inside your game loop.

### Part B - Code Structure and Helper Methods

When building your `play()` method, break the work into smaller **helper methods** (see [How to write a method](#how-to-write-a-method)). This makes your code easier to read and debug.

A helper method handles one specific, well-defined task. Helper methods should:

- Do **one thing** well
- Be reusable (you might call them more than once)
- Make the main method easier to read and understand

#### Recommended helper methods for Part B

All of these are `private static` methods in `RockPaperScissors`.

1. **`String getRandomChoice()`** - returns a random choice: `"r"`, `"p"`, or `"s"`.
   - Used for the computer's pick, and again when the player picks Any.
2. **`String getPlayerChoice(Scanner sc)`** - prompts for R/P/S/A and returns a valid pick.
   - Loops until the input is valid, printing `Invalid input, please try again` after each invalid input.
   - Returns lowercase `"r"`, `"p"`, `"s"`, or `"a"`.
3. **`boolean getYesNoInput(Scanner sc, String prompt)`** - asks a yes/no question.
   - Loops until the answer is Y or N (capital or lowercase).
   - Returns `true` for yes and `false` for no.
   - The same pattern is useful again in Part C.
4. **`int playRound(Scanner sc)`** - plays one complete round.
   - Gets the player's pick, makes the computer's pick, and prints the round's result line.
   - Returns `1` for a win, `0` for a tie, or `-1` for a loss, so that `play` can update its three counters.
5. **`String formatChoice(String choice)`** - turns `"r"` into `"Rock"`, `"p"` into `"Paper"`, and `"s"` into `"Scissors"`.

#### How to put the helper methods together

The `play()` method should read like an overview of the game:

```java
public static void play(Scanner sc) {
    // Print the welcome line
    // Make the three counters (wins, losses, ties), all starting at 0
    // Game loop:
    //   - Play one round (call playRound) and update a counter using the int it returns
    //   - Print the score line
    //   - Ask if they want to play again (call getYesNoInput)
    // Print the goodbye line
}
```

**Key principle**: if you find yourself writing similar code in more than one place, that's a sign you should make a helper method!

### Part B Sample Run

Once you are done, your output should look exactly like this sample run. It includes what the player typed (after each `: `).

```text
Welcome to Rock Paper Scissors
Would you like to pick (R)ock, (P)aper, (S)cissors, or (A)ny: r
You both picked Rock. It's a tie!
You have 0 wins and 0 losses and 1 ties
Would you like to play again? (Y)es or (N)o: y
Would you like to pick (R)ock, (P)aper, (S)cissors, or (A)ny: a
You picked Rock, and the Computer picked Paper. You lose!
You have 0 wins and 1 losses and 1 ties
Would you like to play again? (Y)es or (N)o: Y
Would you like to pick (R)ock, (P)aper, (S)cissors, or (A)ny: S
You picked Scissors, and the Computer picked Rock. You lose!
You have 0 wins and 2 losses and 1 ties
Would you like to play again? (Y)es or (N)o: y
Would you like to pick (R)ock, (P)aper, (S)cissors, or (A)ny: asdfawefasdf
Invalid input, please try again
Would you like to pick (R)ock, (P)aper, (S)cissors, or (A)ny: p
You picked Paper, and the Computer picked Scissors. You lose!
You have 0 wins and 3 losses and 1 ties
Would you like to play again? (Y)es or (N)o: asdofjas;dfk
Invalid input, please try again
Would you like to play again? (Y)es or (N)o: N
Thanks for playing!
```

### Output rules

The tests check your output exactly, character for character. These are the rules they enforce:

- **Prompts use `print`, not `println`**, so the player types on the same line as the question. Each prompt ends with a colon and a space (`: `):
  - `Would you like to pick (R)ock, (P)aper, (S)cissors, or (A)ny: `
  - `Would you like to play again? (Y)es or (N)o: `
- **Every other message uses `println`**, so it is a line of its own. In the tests, what the player types does not appear in the output, so the message printed after a prompt continues that prompt's line. That's why a `println` prompt breaks the expected line layout.
- **Exactly these lines**, and nothing else (no blank lines, no extra or debugging prints):
  1. `Welcome to Rock Paper Scissors` once, first.
  2. For each round: one result line, then one score line.
  3. `Thanks for playing!` once, last.
- **Result lines**, where X and Y are `Rock`, `Paper` or `Scissors`:
  - a tie: `You both picked X. It's a tie!` (with the apostrophe in `It's`)
  - otherwise: `You picked X, and the Computer picked Y. You win!` or `... You lose!`
- **Score line**: `You have W wins and L losses and T ties`, where W, L and T are the counts so far. Always `wins`, `losses` and `ties`, even for 1.
- **Invalid input**: print `Invalid input, please try again` (lowercase `input`), then ask the same question again. This applies to both questions.
- **Picks and answers** may be typed as capital or lowercase letters: R/r, P/p, S/s, A/a, Y/y and N/n.
- No message has a space at the end (only the two prompts do).

### Part B tests

This part has **13 tests**, in the **Part B - play** group of the Testing panel. Each test checks one feature, so a missing feature fails only its own test. Only the Any test types A; the others type R, P or S. The computer's pick is random, but every test accepts every possible computer pick, so a correct program passes every time. (Some tests play many rounds so that ties and non-ties both show up.)

| Test | What it checks |
|---|---|
| prints the welcome line first | the first line is `Welcome to Rock Paper Scissors` |
| shows the prompts exactly, using print so each answer is typed on the same line | both prompts, exact text, printed with `print` |
| prints one result line and one score line per round, and nothing extra | no blank lines or extra messages |
| plays a round with the pick you typed (R, P or S, capital or lowercase) | `r`/`R` is Rock, `p`/`P` is Paper, `s`/`S` is Scissors |
| says You win! or You lose! with both picks when the picks are different | exact wording, and the right winner |
| says It's a tie! when both picks are the same | exact tie wording |
| picking A (Any) plays a round with a random pick for you | `a`/`A` becomes a random Rock, Paper or Scissors |
| an invalid pick prints Invalid input, please try again and asks again | for example `x`, then asks for a pick again |
| an invalid Y/N answer prints Invalid input, please try again and asks again | for example `maybe`, then asks the question again |
| stops right away when the player answers n or N | no more rounds after `n` or `N` |
| prints the score line exactly: You have W wins and L losses and T ties | exact score-line format |
| the score counts match the rounds played | after each round, exactly one count goes up by 1 |
| prints Thanks for playing! as the last line | exact goodbye line, no space at the end |

When a test fails, its message shows what it expected, what your program printed (with what the test typed), and a likely cause.

## PART C - PalindromeTester.java

For **PART C**, you need to complete the class `PalindromeTester`. The class tests whether or not a given `String` from the user is a palindrome.

For the sake of this project, a palindrome is defined as a `String` that reads the same forward and backwards when spaces, numbers, punctuation, and capitalization are all ignored.

The method `isPalindrome` should take in a string and return a boolean for whether or not the string is a palindrome, as defined in the paragraph above. The input string could have any number of spaces, punctuation, etc. Finishing this function should clear up 10 tests - all the ones starting with `isPalindrome_whenCalledWith...`.

After finishing `isPalindrome`, you will work on `execute()`. You should continually prompt the user for words to test until the user chooses to stop. Your output should look like the **Sample Run** below.

### PART C - Code Structure and Helper Functions

Similar to PART B, you should break down your `execute()` method into smaller helper methods:

#### Recommended Helper Methods for PART C

1. **`getYesNoInput(Scanner sc, String prompt)`** - Gets Y/N input from the user
   - Purpose: Reusable method for asking yes/no questions (you can reuse the same method from RockPaperScissors!)
   - Should validate input and loop until valid
   - Should return `true` for yes, `false` for no
   - Same pattern you used in RockPaperScissors - you can copy that helper method here!

2. **Helper methods for `isPalindrome()`** - Break down the palindrome checking logic
   - Consider creating a method that filters/processes the string (removes spaces, punctuation, etc.)
   - Consider creating a method that checks if a processed string is a palindrome
   - This makes `isPalindrome()` easier to read and test

#### How to Put Helper Methods Together for PART C

The main `execute()` method should:

- Print welcome message
- Main loop:
  - Prompt for phrase
  - Check if palindrome (call `isPalindrome()`)
  - Display result
  - Ask if they want to continue (call `getYesNoInput()` helper)
- Exit when user chooses to stop

**Key Principle**: Notice how `getYesNoInput()` is useful in both Part B and Part C? That's the power of helper methods - they make your code reusable and easier to maintain!

### Part C Sample Run

```java
Welcome to Palindrome Tester
Enter a phrase: racecar
racecar is a palindrome
Keep testing? (Y)es or (N)o: asdfhsa;g
Invalid Input, please try again
Keep testing? (Y)es or (N)o: Y
Enter a phrase: ra^124c8098  eCA@R
ra^124c8098  eCA@R is a palindrome
Keep testing? (Y)es or (N)o: y
Enter a phrase: isThisAPalindrome?
isThisAPalindrome? is not a palindrome
Keep testing? (Y)es or (N)o: N
```

## Grading Breakdown

- Code compiles & runs without errors: 2 pts
- Formatting/indentation: 2 points
- All code commented (JavaDoc on every class and method, like the example in [How to write a method](#how-to-write-a-method)): 2 points
- No public methods/attributes besides the ones specified: 2 point
- All code is DRY (Don't repeat yourself): 2 points
- Passes all test cases: 20 points (20 tests, so 1pt/test)
Total: 30 points


## Extra Credit

Finished early? Try one or more of these. **Extra credit does not affect your test score** - it can't raise or lower your test points - and your required work must still pass every test. Each one only needs things you've already learned (variables, `String` methods, `Math.random`, `if`/`else`, and loops; no arrays).

1. **A smarter computer.** In `RockPaperScissors`, use three counters to keep track of how many times the player has picked Rock, Paper, and Scissors. Each round, have the computer pick the move that *beats* the player's most common pick so far (pick randomly if there's a tie or no picks yet). Keep the printed output exactly the same, so the tests still pass.
2. **Rock Paper Scissors Lizard Spock.** In a **new** class (for example `RockPaperScissorsLizardSpock.java`), write a version of the game with the two extra moves: Scissors cuts Paper, Paper covers Rock, Rock crushes Lizard, Lizard poisons Spock, Spock smashes Scissors, Scissors decapitates Lizard, Lizard eats Paper, Paper disproves Spock, Spock vaporizes Rock, and Rock crushes Scissors. Try it from `Main.java`. Keep it in its own class so `RockPaperScissors` still passes its tests.
3. **Palindrome finder.** In a **new** class, write a method that takes a phrase and prints the longest piece of it (a substring) that is a palindrome, using nested loops and `substring`. For example, in `"my racecar is fast"` the longest palindrome piece is `"racecar"`. You can reuse your `isPalindrome` method to check each piece.
