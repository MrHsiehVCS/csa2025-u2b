# Unit 4 Programming Project

For this project, you will be completing the code for two classes: `RockPaperScissors` and `PalindromeTester`. Each of these classes have some code that has already been written for you. You should not change any of the provided code. Instead, you should add in your own code in order to fulfill the project requirements. You will also need to JavaDoc all your classes.

## Part A - RockPaperScissors.java - results()

The method `results` should accept player and computer input (`"R"`, `"P"` or `"S"`, lowercase or capital), and return an integer: `1` if the player wins, `0` if it is a tie, and `-1` if the player loses.

This part has one test - `doesRockPaperScissorLogicCorrectly`

## PART B - RockPaperScissors.java

For **Part B** you are going to complete the class `RockPaperScissors`. The class simulates a game of Rock-Paper-Scissors against a computer. The completed project will do the following:

1. Prompt the user to select *Rock*, *Paper*, or *Scissors*.
   - Users can choose *Rock* by inputting either *R* or *r*.
   - Users can choose *Paper* by inputting either *P* or *p*.
   - Users can choose *Scissors* by inputting either *S* or *s*.
   - Users can choose *Any* by inputting either *A* or *a*. If a user selects *Any* then a random selection is made for them, the same way it is done for the computer.
2. Have the computer secretly select one of the three options, randomly.
3. Using the results method from part A, output both selections and whether the user wins, loses, or ties.
4. Keep track of and display how many times the user has won, lost and tied
5. Keep playing the game until the user chooses to stop.

### PART B - Implementation order
1. create a helper method that returns a random choice between `"r"`, `"p"`, or `"s"`
2. use the scanner to take in player input, and the helper method to generate a computer choice, then use the `results` method from PART A to output the `int` of who won
3. change the `int` output to show the proper output. 
  - ex: `You both picked Rock. Its a tie!`, or `You picked Rock, and the Computer picked Paper. You lose!`
4. add a loop of some sort that repeats this over and over, asking the user if they want to continue. 
  - Have the loop exit based on the user input
  - ex: `Would you like to play again? (Y)es or (N)o:`
5. add variables to keep track of how many wins/ties/losses the player has experienced. 
  - print these out at the right time, with the correct format
  - ex: `You have 0 wins and 3 losses and 1 ties`
6. add functionality to check for valid input.
  - requires nesting while loops

### PART B - Code Structure and Helper Functions

When building your `play()` method, it's important to break down complex tasks into smaller, reusable helper methods. This makes your code easier to read, test, and debug. Here's how to approach structuring your code:

#### What is a Helper Method?

A helper method is a method that handles a specific, well-defined task. Helper methods should:

- Do **one thing** well
- Be reusable (you might call them multiple times)
- Make the main method easier to read and understand

#### Recommended Helper Methods for PART B

1. **`getRandomChoice()`** - Returns a random choice (`"r"`, `"p"`, or `"s"`)
   - Purpose: Generate the computer's choice
   - Can also be used when the player chooses "Any"
   - This should be a simple method that just returns one random string

2. **`getPlayerChoice(Scanner sc)`** - Prompts the user for R/P/S/A and returns valid input
   - Purpose: Handle all the logic for getting valid player input
   - Should loop until valid input is received
   - Should handle invalid input with error messages
   - Should return lowercase `"r"`, `"p"`, `"s"`, or `"a"`

3. **`getYesNoInput(Scanner sc, String prompt)`** - Gets Y/N input from the user
   - Purpose: Reusable method for asking yes/no questions
   - Should validate input and loop until valid
   - Should return `true` for yes, `false` for no
   - This same pattern can be reused in PalindromeTester!

4. **`playRound(Scanner sc, int[] scores)`** - Handles one complete round of RPS
   - Purpose: Encapsulate all the logic for playing a single round
   - Gets player choice, generates computer choice, determines winner
   - Updates and displays scores
   - Makes the main `play()` loop much simpler

5. **`formatChoice(String choice)`** - Converts `"r"` to `"Rock"`, `"p"` to `"Paper"`, etc.
   - Purpose: Convert the internal representation to user-friendly output
   - Helps with displaying results in a readable format

#### How to Put Helper Methods Together

The main `play()` method should be a high-level overview of what the game does:

```java
public static void play(Scanner sc) {
    // Print welcome message
    // Initialize score variables
    // Main game loop:
    //   - Play one round (call playRound helper)
    //   - Ask if they want to continue (call getYesNoInput helper)
    // Print goodbye message
}
```

Each helper method should handle one specific task. For example, `getPlayerChoice()` should:
- Print the prompt
- Loop until valid input
- Print error messages for invalid input
- Return the valid choice

**Key Principle**: If you find yourself writing similar code in multiple places, that's a sign you should create a helper method!


Once you are done, your output should look like the **Sample Run** below.
The sample output includes player input. 

### PART B Sample Run

```java
Welcome to Rock Paper Scissors
Would you like to pick (R)ock, (P)aper, (S)cissors, or (A)ny: r
You both picked Rock. Its a tie!
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
Invalid Input, please try again
Would you like to play again? (Y)es or (N)o: N
Thanks for playing! 
```

Hint: 
- You might want to make helper methods for contained bits of functionality. For example:
  - a helper method for getting a random choice between R/P/S
  - a helper method that displays/handles one full round of RPS

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
- All code commented: 2 points
- No public methods/attributes besides the ones specified: 2 point
- All code is DRY (Don't repeat yourself): 2 points
- Passes all test cases: 20 points (20 tests, so 1pt/test)
Total: 30 points


## Extra Credit
- make your RPS computer AI smarter.
