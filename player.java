import java.io.PrintWriter;
import java.util.Objects;
import java.util

public final class Player implements Runnable {

  public static final int handSize = 4;

  private final int num;
  private final leftDeck;
  private final rightDeck;
  private final PrintWriter log;

  private final List<Card> hand = new ArrayList<>(handSize + 1);

  /** num: player's index, from 1 to n.
  * leftDeck: Player draws from the top of this deck
  * rightDeck: Player discards to the bottom of this deck
  * log: write down the player's move 
  */
  public Player(int num, CardDeck leftDeck, cardDeck rightDeck, PrintWriter log) {
    if (num < 1) {
      throws new IllegalArgumentException (
        "Invalid player index") //index error checking
        }

    this.num = num;
    // null error checking
    this.leftDeck = Objects.requireNonNull(leftDeck, "leftDeck");
    this.rightDeck = Objects.requireNonNull(rightDeck, "rightDeck");
    this.log = Objects.requireNonNull(log, "log");

  public int getNum() {
    return Num;
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

  @suppressWarnings("try")
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
      if (
