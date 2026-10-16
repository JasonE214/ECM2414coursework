import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.PrintStream;

public final class Game {

  private final int PlayerNum;
  private final Pack pack;
  private final Path OutputDirectory;
  private final PrintStream console;

  private boolean start;

  private volatile List<cardDeck> decks = List.of();
