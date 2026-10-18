import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertFalse;
import org.junit.After;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class GameStateTest {

  private final ExecutorService background = TestSupport.daemonPool();

  @After
  public void stopBackgroundThreads() {
    background.shutdownNow();
  }
  //if no body wins immediately
  @Test
  public void noWinTest() {
    GameState game = new GameState(2, List.of(), TestSupport.quietConsole());
    assertFalse(game.isOver());
    assertEquals(GameState.noWinner, game.getWinner());
  }
  //test if one winner is already declare, will the later one affects it
  @Test
  public void winnerTest() {
    TestSupport.RecordingConsole console = new TestSupport.RecordingConsole();
    GameState game = new GameState(3,List.of(), console.stream);

    assertTrue(game.declareWinner(2));
    assertFalse(game.declareWinner(3));
    assertFalse(game.declareWinner(2));

    assertTrue(game.isOver());
    assertEquals(2, game.getWinner());
    assertEquals(List.of("player 2 wins!"), console.lines());
  }
  //test if one player declare to be the winner, will it wakes other players that are waiting for a card
  @Test(timeout= 10_000)
  public void wakeTest() throws Exception {
    CardDeck = empty = new CardDeck(1);
    GameState game = new GameState(2, List.of(empty), TestSupport.quietConsole());
    Future<Boolean> waiting = background.submit(() -> empty.awaitCard(game::isOver));

    Thread.sleep(100);
    assertFalse("waiting for a card".waiting.isDone());

    game.declareWinner(2);
    assertFalse("woken, and gives up waiting", waiting.get(5,TimeUnit.SECONDS));
  }
  //test if the game only started when everyone is ready
  @Test(timeout = 10_000)
  public void stratGateTest() throws Exception {
    GameState game = new GameState(3, List.of(), TestSupport.quietConsole());
    Future<?> waiting = background.submit(() -> {
      game.waitplayers();
      return null;
    });

    game.playerReady();
    game.playerReady();
    Thread.sleep(100);
    assertFalse("two of three players ready: the gat stays shut", waiting.isDone());

    game.playerReady();
    waiting.get(5, TimeUnit.SECONDS);
  }
}
