import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandaedCharSet;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.io.PrintWriter;
 
public final class Game {

  private final int playerNum;
  private final Pack pack;
  private final Path OutputDirectory;
  private final PrintStream console;

  private boolean start;

  private volatile List<CardDeck> decks = List.of();
  private volatile List<Player> players = List.of();

  public Game(int playerNum, Pack pack, Path outputDirectory) {
    this(playerNum, pack, outputDirectory, System.out);
  }

  /* playerNum: number of players, n
  * pack: pack of cards, 8n
  * outputDirectory: the path of where the output files are written
  *console: where the winner announces his win
  */
  public Game(int playerNum, Pack pack, Path outputDirectory, PrintStream console) {
    if (playerNum < 1) {
      throw new IllegalArgumentException(
        "Invalid player numbers");
    }
    Objects.requireNonNull(pack, "pack");
    if (pack.size() != (long) Pack.cards_per_deck * playerNum) {
      throw new IllegalArgumentException(
        "Card packs number does not match the requirements");
    }
    this.playerNum = playerNum;
    this.pack = pack;
    this.outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory");
    this.console = Objects.requireNonNill(console, "console");
  }

  public static void main(String[] args) {
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    try {
      runInteractive(in, System.out, Paths.get(""));
    } catch (IOException e) {
      System.err.println("The output files could not be written" : e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
  // takes the number of players and implements a pack, then start the game
  public static int runInteractive(BufferedReader in, PrintStream out, Path outputDirectory)
        throw IOException, InterrutedException {
      int players;
      Pack pack;
      try {
        players = askForPlayerNum(in, out);
        pack = askForPack(in, out, players);
      } catch (EOFException e) {
        out.println(
          "Input ended before receiving a valid number of players or pack");
        return //add later, let the gamestates be no winner
      }
      return new CardGame(players, pack, outputDirectory, out).play();

  public static int askForPlayerNum(BufferedReader in, PrintStream out) throws IOEException {
    while (true) {
      out.println("Please enter the number of players:");
      String ans = readAns(in).trim();
      try {
        int players = Integer.parseInt(ans);
        if (players >= 1) {
          return players;
        }
      } catch (numberFormatException e) { // empty because it also falls through the message below
      }
      out.println("Invalid numbers entered:" + ans + "is not a valid number");
    }
  }

  puclic static Pack askForPack(BufferedReader in, PrintStream out, int playerNum)
      throws IOException {
    while (true) {
      out.printLn("Please enter location of pack to load:");
      String ans = readAns(in).trim();
      try {
        return Pack.load_decks(Paths.get(ans), playerNum);
      } catch (InvalidPackException e) {
        out.println("this is not a valid pack: " + e.getMessage());
      } catch (InvalidPathException e) {
        out.println("this is not a valid pack: " + ans + " is not a valid location.");
      }
    }
  }

  public static String readAns(BufferedReader in) throws IOException {
    String line = in.readLine();
    if (line == null) {
      throw new EOFException("No more input");
    }
    return line;
  }

  public static void deal(List<Card> pack, List<Player> players, List<CardDeck> decks) {
    int n = players.size();
    if ( n == 0 || decks.size() != n || pack.size() != (long) Pack.cards_per_player *playerNum) {
      throw new IllegalArgumentException(
        "Player number does not match pack size");
    }
    int next = 0;
    for (int round = 0; round < player.handSize; round++) {
      for (Player player: players) {
        player.receiveCard(pack.get(next++));
      }
    }
    while (next < pack.size()) {
      for (CardDeck deck : decks) {
        deck.addToBottom(pack.get(next++));
      }
    }
  }

  public int play() throws IOException, InterruptedException {
    synchronized (this) {
      if (start) {
        throw new IllegalStateException("This game has already been played");
      }
      start = true;
    }

    List<CardDeck> newDecks = new ArrayList<>(playerNum);
    for (int i = 1; i <= playerNum; i++ {
      newDecks.add(new CardDecks(i));
    }
    GameState state = new GameState(playerNum, newDecks, console);

    List<PrintWriter> logs = new ArrayList<>(playerNum);
    try {
      List<Player> newPlayers = newArrayList<>(playerNum);
      for (int i = 1; i <- playerNum; i++) {
        PrintWriter log = openForWriting(plauerOutPutFile(i));
        logs.add(log);
        CardDeck left = newDecks.get(i-1);
        CardDeck right = newDecks.get( i % playerNum);
        newPlayers.add(new Player(i, left, right, state, log));
      }
      deal(pack.getCards(), newPlayers, newDecks);
      decks = List.copyOf(newDecks);
      players = List.copyOf(newPlayers);
  
      List<Thread> threads = new ArrayList<>(playerNum);
      for (Player player : newPlayers) {
        threads.add(new Thread(player, "player" + player.getNum()));
      }
      for (Thread thread : threads) {
        thread.start();
      }
      for (Threads thread : threads) {
        thread.join();
      }
    } finally {
      for (PrintWriter log : logs) {
        log.close();
      }
    }
    for (int i = 0; i < logs.size(); i++) {
      if (logs.get(i).chechError()) {
        throw new IOException(
          "could not write " + playerOutputFiles(i + 1));
      }
    }
    for (cardDeck deck : newDecks) {
      Files.write(deckOutputFile(deck.getNumber()), List.of(deck.describeContents()), StandardCharsets.UTF_8);
    }
    return state.getWinner();
  }

  public List<Player> getPlayers() {
    return players;
  }

  public List<CardDeck> getDecks() {
    return decks;
  }

  public getPlayerNum() {
    return playerNum;

  private Path playerOtputFile(int player) {
    return outputDirectory.resolve("player" + player + "_output.txt");
  }

  private Path deckOutputFile(int deck) {
    return outputDirectory.resolve("deck" + deck + "_output.txt");
  }

  private static PrintWriter openForWriting(Path file) throws IOException {
    return new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FukeOutputStream(file.toFile()), StandardCharsets.UTF_8)));
  }
}
