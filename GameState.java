import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.io.PrintStream;
import java.util.Objects;
import java.util.List;

public final class GameState {

  public static final int noWinner = 0;

  private final AtomicInteger winner = new AtomicInteger(noWinner);
  private final CountDownLatch startGate;
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

  public boolean isOver() {
    return winner.get() != noWinner;
  }

  public int getWinner() {
    return winner.get();
  }

  public boolean declareWinner(int playerIndex) {
    if (playerIndex < 1) {
      throw new IllegalArgumentException(
        "Player index start at 1");
    }
    if (!winner.compareAndSet(noWinner, playerIndex)) {
      return false;
    }
    console.println("player " + playerIndex + " wins!");
    for (CardDeck deck : decks) {
      	deck.wakeWaitingPlayers();
    }
    return true;
  }

  public void playerReady() {
    startGate.countDown();
  }

  public void waitPlayers() throws InterruptedException {
    startGate.await();
  }
}
