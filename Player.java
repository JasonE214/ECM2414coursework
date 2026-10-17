import java.io.PrintWriter;
import java.util.Objects;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

public final class Player implements Runnable {

  public static final int handSize = 4;

  private final int num;
  private final CardDeck leftDeck;
  private final CardDeck rightDeck;
  private final GameState game;
  private final PrintWriter log;

  private final List<Card> hand = new ArrayList<>(handSize + 1);

  /** num: player's index, from 1 to n.
  * leftDeck: Player draws from the top of this deck
  * rightDeck: Player discards to the bottom of this deck
  * log: write down the player's move 
  */
  Player(int num, CardDeck leftDeck, CardDeck rightDeck, GameState game, PrintWriter log) {
    if (num < 1) {
      throw new IllegalArgumentException (
        "Invalid player index"); //index error checking
        }

    this.num = num;
    // null error checking
    this.leftDeck = Objects.requireNonNull(leftDeck, "leftDeck");
    this.rightDeck = Objects.requireNonNull(rightDeck, "rightDeck");
    this.game = Objects.requireNonNull(game,"game");
    this.log = Objects.requireNonNull(log, "log");
  }

  public int getNum() {
    return num;
  }
  
  public int getPreferredValue() {
    return num;
  }
  
  public synchronized List<Card> getHand() {
    return List.copyOf(hand);
  }
  
  public synchronized void receiveCard(Card card) {
    Objects.requireNonNull(card, "card");
    if (hand.size() >= handSize) {
      throw new IllegalStateException (
        "Player(s) hands exceeds the hand limits");
    }
    hand.add(card);
  }

  public synchronized boolean hasWinningHand() {
    if (hand.size() != handSize) {
      return false;
    }

    int value = hand.get(0).getValue();
    for (Card card : hand) {
      if (card.getValue() != value) {
        return false;
      }
    }
    return true;
  }

  @SuppressWarnings("try")
  public synchronized Turn takeTurn() {
    if (hand.size() != handSize) {
       throw new IllegalStateException (
        "Player(s) hands exceeds the hand limits");
    }
    if (hasWinningHand()) {
      throw new IllegalStateException(
        "There is player who has already won");
    }
    try (CardDeck.HeldLocks ignored = CardDeck.lockBoth(leftDeck, rightDeck)) {
      if (game.isOver() || leftDeck.isEmpty()) {
        return null;
      }
      Card drawn = leftDeck.drawFromTop();
      hand.add(drawn);
      Card discarded = removeLongestHeld();
      rightDeck.addToBottom(discarded);
      return new Turn(drawn, discarded, List.copyOf(hand));
    }
  }

  private Card removeLongestHeld() {
    Iterator<Card> cards = hand.iterator();
    while (cards.hasNext()) {
      Card card = cards.next();
      if (card.getValue() != getPreferredValue()) {
        cards.remove();
        return card;
      }
    }
    throw new IllegalStateException("No non-preferred card to discard");
  }

  @Override
    public void run() {
    boolean won = false;
    try {
      try {
        write("player " + num + " initial hand " + describe(getHand()));
        if (hasWinningHand()) {
          won = game.declareWinner(num);
        }
      } finally {
        game.playerReady();
      }
      game.waitPlayers();

      while (!won && !game.isOver()) {
        if (!leftDeck.awaitCard(game::isOver)) {
          break;
        }
        Turn turn = takeTurn();
        if (turn == null) {
          break;
        }
        write("player " + num + " draws a " + turn.getDrawn().getValue() + " from deck " + leftDeck.getNumber());
        write("player " + num + " discards a " + turn.getDiscarded().getValue() + " to deck " + rightDeck.getNumber());
        write("player " + num + " current hand is " + describe(turn.getHandAfter()));
        if(hasWinningHand()) {
          won = game.declareWinner(num);
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      writeEnding(won);
    }
  }

  private void writeEnding(boolean won) {
    String finalHand = describe(getHand());
    if (won) {
      write("Player " + num + " wins!");
      write("Player " + num + " exits");
      write("Player " + num + " final hand: " + finalHand);
    } else {
      int winner = game.getWinner();
      if (winner != GameState.noWinner) {
        write("Player " + winner + " has informed player " + num + "he has won");
      }
      write("player " + num + " exits");
      write("player " + num + "hand: " + finalHand);
    }
    log.flush();
  }

  private void write(String line) {
    log.println(line);
  }

  private static String describe(List<Card> cards) {
    StringBuilder text = new StringBuilder();
    for (Card card : cards) {
      if (text.length() > 0) {
        text.append(' ');
      }
      text.append(card.getValue());
    }
    return text.toString();
  }

  public static final class Turn {
    private final Card drawn;
    private final Card discarded;
    private final List<Card> handAfter;

    Turn(Card drawn, Card discarded, List<Card> handAfter) {
      this.drawn = drawn;
      this.discarded = discarded;
      this.handAfter = handAfter;
    }

    public Card getDrawn() {
      return drawn;
    }

    public Card getDiscarded() {
      return discarded;
    }

    public List<Card> getHandAfter() {
      return handAfter;
    }
  }
}
