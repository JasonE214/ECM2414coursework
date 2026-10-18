# ECM2414 Card Game

A multi-threaded card game simulation in Java.

Each player runs in its own thread. Players sit in a ring with a deck between each pair of
neighbours. On each turn a player draws a card from the deck on their left and discards one
to the deck on their right, as one atomic action. The first player to hold four cards of the
same value wins.

## Versions and dependencies

JDK 11 | JUnit 4.13.2 | hamcrest-core 1.3 |

## Folder layout

Path : Contents 
top folder : The game: `Card`, `Pack`, `CardDeck`, `Player`, `GameState`, `CardGame` 
`test/` : JUnit test classes, the `AllTests` suite and the `TestSupport` helper class 
`lib/` : `junit-4.13.2.jar` and `hamcrest-core-1.3.jar` 
`packs/` : Sample pack files, valid and invalid 

All commands below are run from the top folder (the one `CardGame.java` is at).

## Compile and play the game

Copy and paste each line to the terminal one by one:
```
javac --release 11 -d out *.java
java -cp out CardGame
```

The game asks two questions. For example:

```
Please enter the number of players: (Question)
4 (User Input)
Please enter location of pack to load: (Question)
packs/four.txt (User Input)
player 3 wins (Result)
```

If the user input is invalid, game will run into an error, explains the reason and asks again.

When the game ends, it writes the player and deck process to the folder it was started from:

- `player1_output.txt` ... `playerN_output.txt`: each player's initial hand, every draw and
  discard, and how the game ended for them
- `deck1_output.txt` ... `deckN_output.txt`: what was left in each deck

Each new game will overwrites the output files from the previous one.

### Running the jar

```
java -jar cards.jar
```

## Compile and run the tests

Copy and paste each line to the terminal one by one:
```
javac --release 11 -d out *.java
javac --release 11 -d tbin -cp "out;lib\junit-4.13.2.jar;lib\hamcrest-core-1.3.jar" test\*.java
java -cp "out;tbin;lib\junit-4.13.2.jar;lib\hamcrest-core-1.3.jar" org.junit.runner.JUnitCore AllTests
```

1. The first command compiles the game into `out/`.
2. The second compiles the tests into `tbin/`.
3. The third runs the `AllTests` suite. When every test passes, the output ends with `OK`
   and the number of tests run.

To run a single test class, replace `AllTests` with its name, like `CardDeckTest`.

Re-run the first two commands after changing any code; the third only runs what was last
compiled. 

## Sample packs (`packs/`)

A valid pack for n players has exactly 8n rows, each a whole number of 0 or more.

 File                              | Players -> What happens 
  `one.txt`                        | 1 ->  A short one-player game 
`two.txt`, `four.txt`, `eight.txt` | 2, 4, 8 ->  Normal games 
`three_immediate_win.txt`          | 3 ->  A player is dealt four cards of the same value and wins before any card moves 
`invalid_four_too_short.txt`       | 4 ->  Rejected: too few rows 
`invalid_four_negative.txt`        | 4 ->  Rejected: contains a negative number 
`invalid_four_not_a_number.txt`    | 4 ->  Rejected: contains text that is not a number 

## Known limitations

- A valid pack that does not contain four cards of any player's preferred value may never
  produce a winner. The brief allows this.
- A pack file saved with a UTF-8 byte-order mark (an invisible character some Windows editors
  add at the start of a file) will be rejected on row 1. Saving it as plain UTF-8 avoids this.
