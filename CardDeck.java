import java.util.LinkedList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
A FIFO deck in the game
Only 1 player can draw it and place card in bottom
Adding lock to function to ensure data correctness
*/
public final class CardDeck {

    private final int number;
    private final LinkedList<Card> cards = new LinkedList<>(); // only used while holding the deck lock
    private Thread owner = null; 
    private int holdCount = 0; 

    public CardDeck(int number) {
        if (number < 1) {
            throw new IllegalArgumentException("Deck numbers start at 1, not " + number);
        }
        this.number = number;
    }

    public int getNumber() {
        return number;
    }
    

/*Takes this deck's lock, waiting while another thread holds it. A thread that already
holds the lock can take it again, which is use in a one-player game*/
    private synchronized void lock() {
        Thread me = Thread.currentThread();
        boolean interrupted = false;

        while (owner != null && owner != me) {
            try {
                wait(); // sleep until unlock() calls
            } catch (InterruptedException e) {
                interrupted = true; // keep waiting; restore the interrupt flag below
            }
        }
        owner = me;
        holdCount++;
        if (interrupted) {
            me.interrupt();
        }
    }

/*Gives back one hold of this deck's lock after using it.
the last one frees it will wake up all the waiting threads.*/
    private synchronized void unlock() {
        if (owner != Thread.currentThread()) {
            throw new IllegalMonitorStateException("deck " + number + " is not locked by this thread");
        }
        holdCount--;
        if (holdCount == 0) {
            owner = null;
            notifyAll();
        }
    }

/*Puts a card on the bottom of the deck 
and wakes player up*/
    public void addToBottom(Card card) {
        lock();
        try {
            cards.addLast(card);
        } finally {
            unlock();
        }
    }

/*Removes and returns the top card.*/
    public Card drawFromTop() {
        lock();
        try {
            return cards.removeFirst(); 
        } finally {
            unlock();
        }
    }

    public boolean isEmpty() {
        lock();
        try {
            return cards.isEmpty();
        } finally {
            unlock();
        }
    }

    public int size() {
        lock();
        try {
            return cards.size();
        } finally {
            unlock();
        }
    }

/*A copy of the deck from top to bottom. Later changes to the deck do not affect it.*/
    public List<Card> getContents() {
        lock();
        try {
            return List.copyOf(cards);
        } finally {
            unlock();
        }
    }

/*Use to return a line to display what is inside the deck*/
    public String describeContents() {
        StringBuilder line = new StringBuilder("deck").append(number).append(" contents:");
        for (Card card : getContents()) {
            line.append(' ').append(card.getValue());
        }
        return line.toString();
    }

/*Waits until this deck has a card to draw, or until stopWaiting becomes true.
Whoever makes stopWaiting true must call wakeWaiters() on this deck.
The list is only looked by other functions when no other thread holds the deck.
If it have been hold, this waits, and that thread's unlock() wakes up to look again.
return true if a card is available to draw, false if waiting was abandoned*/
    public synchronized boolean awaitCard(BooleanSupplier stopWaiting) throws InterruptedException {
        Thread me = Thread.currentThread();
        while (true) {
            boolean freeToLook = (owner == null || owner == me);
            if (freeToLook && !cards.isEmpty()) {
                return true;
            }
            if (stopWaiting.getAsBoolean()) {
                return false;
            }
            wait(); // now wait for unlock()
        }
    }

    /** Wakes any player waiting in awaitCard so it re-checks its stop condition. */
    public synchronized void wakeWaitingPlayers() {
        notifyAll();
    }

    public static HeldLocks lockBoth(CardDeck a, CardDeck b) {
            final CardDeck first = a.number <= b.number ? a : b;
            final CardDeck second = (first == a) ? b : a;
            first.lock();
            second.lock();
            return new HeldLocks(first, second);
        }

    public static final class HeldLocks implements AutoCloseable {
        private final CardDeck first;
        private final CardDeck second;
 
        private HeldLocks(CardDeck first, CardDeck second) {
            this.first = first;
            this.second = second;
        }
 
        @Override
        public void close() {      
            second.unlock();   
            first.unlock();
        }
}