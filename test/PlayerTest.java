import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Test;

public class PlayerTest {

    private final ExecutorService background = TestSupport.daemonPool();

    @After
    public void stopBackgroundThreads() {
        background.shutdownNow();
    }

    private static Player player(int number, CardDeck left, CardDeck right, GameState game, int... hand) {
        Player player = new Player(number, left, right, game, TestSupport.quietLog());
        for (Card card : TestSupport.cards(hand)) {
            player.receiveCard(card);
        }
        return player;
    }

    private static GameState gameWith(CardDeck... decks) {
        return new GameState(1, Arrays.asList(decks), TestSupport.quietConsole());
    }

    private static int count(List<Card> cards, int value) {
        return Collections.frequency(TestSupport.values(cards), value);
    }

// check does it draw a card from left deck and place it on the bottom of right deck
    @Test
    public void draw_discar_check() {
        CardDeck left = TestSupport.deck(1, 7, 8);
        CardDeck right = TestSupport.deck(2, 9);
        Player player = player(1, left, right, gameWith(left, right), 1, 2, 3, 4);

        Player.Turn turn = player.takeTurn();

        assertEquals(7, turn.getDrawn().getValue());
        assertEquals(2, turn.getDiscarded().getValue());
        assertEquals(List.of(8), TestSupport.values(left.getContents()));
        assertEquals(List.of(9, 2), TestSupport.values(right.getContents()));
        assertEquals(List.of(1, 3, 4, 7), TestSupport.values(player.getHand()));
        assertEquals(player.getHand(), turn.getHandAfter());
    }

// discard a longest holding non perfered card
    @Test
    public void discard_correct_card() {
        CardDeck left = TestSupport.deck(1, 9, 8);
        CardDeck right = new CardDeck(2);
        Player player = player(1, left, right, gameWith(left, right), 5, 1, 6, 7);

        assertEquals(5, player.takeTurn().getDiscarded().getValue()); // 1 6 7 9
        assertEquals(6, player.takeTurn().getDiscarded().getValue()); // 1 7 9 8
    }

    //ensure thread only have 4 cards, never 3 or 5 (stopped midway)
    @Test(timeout = 30_000)
    public void isit_atomic() throws Exception {
        CardDeck ring = new CardDeck(1);
        for (int value = 10; value < 30; value++) {
            ring.addToBottom(new Card(value)); // all different, so nobody can win
        }
        Player player = player(1, ring, ring, gameWith(ring), 2, 3, 4, 5);
        AtomicBoolean finished = new AtomicBoolean(false);
        Future<int[]> observer = background.submit(() -> {
            int looks = 0;
            int wrong = 0;
            while (!finished.get()) {
                looks++;
                if (player.getHand().size() != Player.handSize) {
                    wrong++;
                }
            }
            return new int[] {looks, wrong};
        });

        for (int turn = 0; turn < 200_000; turn++) {
            assertNotNull(player.takeTurn());
        }
        finished.set(true);

        int[] result = observer.get();
        assertTrue("the observer looked at the hand", result[0] > 0);
        assertEquals("hands seen with other than four cards", 0, result[1]);
        assertEquals("no card was created or lost", 24, ring.size() + player.getHand().size());
    }

//player know 4 of the same card wins
    @Test
    public void win_condition() {
        CardDeck deck = new CardDeck(1);
        assertTrue(player(1, deck, deck, gameWith(deck), 1, 1, 1, 1).hasWinningHand());
        assertTrue("a non-preferred four of a kind also wins",
                player(1, deck, deck, gameWith(deck), 6, 6, 6, 6).hasWinningHand());
        assertTrue(player(2, deck, deck, gameWith(deck), 0, 0, 0, 0).hasWinningHand());
    }

//when someone win the player stop and don't take turn
    @Test
    public void game_end_stop() {
        CardDeck left = TestSupport.deck(1, 7);
        CardDeck right = new CardDeck(2);
        GameState game = gameWith(left, right);
        Player player = player(1, left, right, game, 1, 2, 3, 4);

        game.declareWinner(2);

        assertNull(player.takeTurn());
        assertEquals(List.of(7), TestSupport.values(left.getContents()));
        assertTrue(right.isEmpty());
        assertEquals(List.of(1, 2, 3, 4), TestSupport.values(player.getHand()));
    }
}
