import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.io.PrintStream;
import java.util.Objects;
import java.util.List;

public final class GameState {
  //use in getWinner()
  public static final int noWinner = 0;

  private final AtomicInteger winner = new AtomicInteger(noWinner);
  private final CountDownLatch startGate; //stop every players from playing if not all cards is dealt
  private final List<CardDeck> decks;
  private final PrintStream console;

  /* playerNum: how many players need to be ready before game starts
  * decks:　every decks in the game
  * console: where the win statement is printed
  */
  public GameState(int playerNum, List<CardDeck> decks, PrintStream console) {
    if (playerNum < 1) {
      throw new IllegalArgumentException("Invalid players number");
    }
    this.startGate = new CountDownLatch(playerNum);
    this.decks = List.copyOf(decks);
    this.console = Objects.requireNonNull(console, "console");
  }
  //check if the game is over by looking if a winner exist
  public boolean isOver() {
    return winner.get() != noWinner;
  }
  //get the winner's index
  public int getWinner() {
    return winner.get();
  }
  //declare the winner
  public boolean declareWinner(int playerIndex) {
    if (playerIndex < 1) {
      throw new IllegalArgumentException(
        "Player index start at 1");
    }
    //make sure there's no other winners. If anyone declare their wins after someone already did, It return false.
    if (!winner.compareAndSet(noWinner, playerIndex)) {
      return false;
    }
    console.println("player " + playerIndex + " wins");
    //wake other players who are waiting cards
    for (CardDeck deck : decks) {
      	deck.wakeWaitingPlayers();
    }
    return true;
  }
  //check every players if there card have been dealt
  public void playerReady() {
    startGate.countDown();
  }
  //blocks other players from starting until everyone is ready
  public void waitPlayers() throws InterruptedException {
    startGate.await();
  }
}
