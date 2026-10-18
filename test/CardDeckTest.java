import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Test;

public class CardDeckTest {

    private final ExecutorService background = TestSupport.daemonPool();
    @After
    public void stopBackgroundThreads() {
        background.shutdownNow();
    }

    // draw card as the order than it is added
    @Test
    public void draw_card() {
        CardDeck deck = TestSupport.deck(1, 5, 3, 9, 1);
        assertEquals(5, deck.drawFromTop().getValue());
        assertEquals(3, deck.drawFromTop().getValue());
        assertEquals(9, deck.drawFromTop().getValue());
        assertEquals(1, deck.drawFromTop().getValue());
        assertTrue(deck.isEmpty());
    }

    //checking does the first in first out sequence is working
    @Test
    public void deckFIFO() {
        CardDeck deck = TestSupport.deck(2, 4, 6);
        deck.addToBottom(new Card(8));
        assertEquals(List.of(4, 6, 8), TestSupport.values(deck.getContents()));

        deck.drawFromTop();
        deck.addToBottom(new Card(1));
        assertEquals(List.of(6, 8, 1), TestSupport.values(deck.getContents()));
    }

    //draw from an empty deck will do nothing and no crashes
    @Test
    public void draw_from_empty() {
        CardDeck deck = new CardDeck(1);
        assertThrows(NoSuchElementException.class, deck::drawFromTop);
        assertEquals(0, deck.size());
    }

    //copies of card is actually working and don't effect the actual deck
    @Test
    public void copy_working() {
        CardDeck deck = TestSupport.deck(1, 1, 2);
        List<Card> snapshot = deck.getContents();

        deck.addToBottom(new Card(3));

        assertEquals(List.of(1, 2), TestSupport.values(snapshot));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(new Card(4)));
        assertEquals(3, deck.size());
    }

    //Check does the describe content actually give output in the format want
    @Test
    public void output_format_working() {
        assertEquals("deck2 contents: 1 3 3 7", TestSupport.deck(2, 1, 3, 3, 7).describeContents());
        assertEquals("deck5 contents:", new CardDeck(5).describeContents());
    }

    //Make sure the discard ain't lost in multi-threading
    @Test(timeout = 20_000)
    public void discard_card() throws Exception {
        int threads = 8;
        int cardsEach = 10_000;
        CardDeck deck = new CardDeck(1);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> discarders = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            discarders.add(background.submit(() -> {
                go.await();
                for (int i = 0; i < cardsEach; i++) {
                    deck.addToBottom(new Card(i));
                }
                return null;
            }));
        }
        go.countDown();
        for (Future<?> discarder : discarders) {
            discarder.get();
        }
        assertEquals(threads * cardsEach, deck.size());
    }

// Player will go to sleep if the deck on his left is empty,
// So need to test if player is wake up when the deck he draws is not empty
    @Test(timeout = 10_000)
    public void wake_if_deck_not_empty() throws Exception {
        CardDeck deck = new CardDeck(1);
        Future<Boolean> waiting = background.submit(() -> deck.awaitCard(() -> false));

        Thread.sleep(100);
        assertFalse("still waiting while the deck is empty", waiting.isDone());

        deck.addToBottom(new Card(4));
        assertTrue("a card is now available", waiting.get(5, TimeUnit.SECONDS));
    }

//Check if other player is waken up once a player win
//or else the progeam will run forever
    @Test(timeout = 10_000)
    public void wake_up_loser() throws Exception {
        CardDeck deck = new CardDeck(1);
        AtomicBoolean gameOver = new AtomicBoolean(false);
        Future<Boolean> waiting = background.submit(() -> deck.awaitCard(gameOver::get));

        Thread.sleep(100);
        assertFalse(waiting.isDone());

        gameOver.set(true);
        deck.wakeWaitingPlayers();
        assertFalse("gives up without a card", waiting.get(5, TimeUnit.SECONDS));
    }

// when player makes his turn, he have locked both decks 
// check does decks getting lock properly and they won't wait for each ne
// which turns into a deadlock
    @Test(timeout = 30_000)
    public void Lock_both_decks() throws Exception {
        int n = 5;
        List<CardDeck> ring = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            ring.add(new CardDeck(i));
        }
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> workers = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            CardDeck left = ring.get(i);
            CardDeck right = ring.get((i + 1) % n);
            workers.add(background.submit(() -> {
                go.await();
                for (int turn = 0; turn < 100_000; turn++) {
                    try (CardDeck.HeldLocks held = CardDeck.lockBoth(left, right)) {
                        Thread.onSpinWait(); // where the draw and discard would go
                    }
                }
                return null;
            }));
        }
        go.countDown();
        for (Future<?> worker : workers) {
            worker.get();
        }
    }

//This is testing for 1 player.
//Checking the deck locks itself twice 
//if not, the game will freeze which returns as a timeout 
    @Test(timeout = 10_000)
    public void double_locking() {
        CardDeck only = TestSupport.deck(1, 1, 2);
        try (CardDeck.HeldLocks held = CardDeck.lockBoth(only, only)) {
            only.addToBottom(only.drawFromTop());
        }
        assertEquals(List.of(2, 1), TestSupport.values(only.getContents()));
    }

// Similar to the one above this one checks if the lock gets unlock twice
    @Test(timeout = 10_000)
    public void double_unlocking() throws Exception {
        CardDeck only = TestSupport.deck(1, 1, 2);
        try (CardDeck.HeldLocks held = CardDeck.lockBoth(only, only)) {
            //empty
        }
        Future<Card> otherThread = background.submit(only::drawFromTop);
        assertEquals(1, otherThread.get(5, TimeUnit.SECONDS).getValue());
    }
}
