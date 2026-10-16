import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import java.io.BufferedReader;
import java.io.BufferedWriter;

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

