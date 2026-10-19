import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CardGameTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private final ExecutorService background = TestSupport.daemonPool();

    @After
    public void stopBackgroundThreads() {
        background.shutdownNow();
    }

    private static List<String> read(Path folder, String fileName) throws IOException {
        return Files.readAllLines(folder.resolve(fileName), StandardCharsets.UTF_8);
    }

    private static long outputFileCount(Path folder) throws IOException {
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(file -> file.getFileName().toString().endsWith("_output.txt")).count();
        }
    }

    //deal cards

    @Test
    public void dealTest() {
        CardDeck deck1 = new CardDeck(1);
        CardDeck deck2 = new CardDeck(2);
        GameState game = new GameState(2, List.of(deck1, deck2), TestSupport.quietConsole());
        Player player1 = new Player(1, deck1, deck2, game, TestSupport.quietLog());
        Player player2 = new Player(2, deck2, deck1, game, TestSupport.quietLog());
        List<Card> pack = TestSupport.cards(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);

        CardGame.deal(pack, List.of(player1, player2), List.of(deck1, deck2));

        assertEquals(List.of(1, 3, 5, 7), TestSupport.values(player1.getHand()));
        assertEquals(List.of(2, 4, 6, 8), TestSupport.values(player2.getHand()));
        assertEquals(List.of(9, 11, 13, 15), TestSupport.values(deck1.getContents()));
        assertEquals(List.of(10, 12, 14, 16), TestSupport.values(deck2.getContents()));
    }

    @Test
    public void wrongPackTest() throws Exception {
        Pack eightCards = TestSupport.pack(1, TestSupport.onePlayerPackValues());
        assertThrows(IllegalArgumentException.class,
                () -> new CardGame(2, eightCards, temp.getRoot().toPath()));
    }
    // hand 1 2 3 4, deck 1 1 1 5. Each turn draws a 1 and discards the oldest non-1.
    @Test(timeout = 30_000)
    public void aOnePlayerGameWritesExactlyTheExpectedFiles() throws Exception {
        Path folder = temp.getRoot().toPath();
        TestSupport.RecordingConsole console = new TestSupport.RecordingConsole();
        CardGame game = new CardGame(1, TestSupport.pack(1, TestSupport.onePlayerPackValues()), folder, console.stream);

        assertEquals(1, game.play());

        assertEquals(List.of(
                "player 1 initial hand 1 2 3 4",
                "player 1 draws a 1 from deck 1",
                "player 1 discards a 2 to deck 1",
                "player 1 current hand is 1 3 4 1",
                "player 1 draws a 1 from deck 1",
                "player 1 discards a 3 to deck 1",
                "player 1 current hand is 1 4 1 1",
                "player 1 draws a 1 from deck 1",
                "player 1 discards a 4 to deck 1",
                "player 1 current hand is 1 1 1 1",
                "player 1 wins",
                "player 1 exits",
                "player 1 final hand: 1 1 1 1"), read(folder, "player1_output.txt"));
        assertEquals(List.of("deck1 contents: 5 2 3 4"), read(folder, "deck1_output.txt"));
        assertEquals(List.of("player 1 wins"), console.lines());
        assertEquals(2, outputFileCount(folder));
    }

    // Player 2 starts with 4 9s
    @Test(timeout = 60_000)
    public void immediateWinTest() throws Exception {
        int[] pack = {
            1, 9, 3,    // round 1
            2, 9, 4,    // round 2
            5, 9, 6,    // round 3
            7, 9, 8,    // round 4 hands: player 1 = 1 2 5 7, player 2 = 9 9 9 9, player 3 = 3 4 6 8
            10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21 };    //decks

        for (int repeat = 1; repeat <= 50; repeat++) {
            Path folder = temp.newFolder("run" + repeat).toPath();
            TestSupport.RecordingConsole console = new TestSupport.RecordingConsole();

            int winner = new CardGame(3, TestSupport.pack(3, pack), folder, console.stream).play();

            String run = "run " + repeat;
            assertEquals(run, 2, winner);
            assertEquals(run, List.of("player 2 wins"), console.lines());
            assertEquals(run, List.of(
                    "player 1 initial hand 1 2 5 7",
                    "player 2 has informed player 1 that player 2 has won",
                    "player 1 exits",
                    "player 1 hand: 1 2 5 7"), read(folder, "player1_output.txt"));
            assertEquals(run, List.of(
                    "player 2 initial hand 9 9 9 9",
                    "player 2 wins",
                    "player 2 exits",
                    "player 2 final hand: 9 9 9 9"), read(folder, "player2_output.txt"));
            assertEquals(run, List.of(
                    "player 3 initial hand 3 4 6 8",
                    "player 2 has informed player 3 that player 2 has won",
                    "player 3 exits",
                    "player 3 hand: 3 4 6 8"), read(folder, "player3_output.txt"));
            assertEquals(run, List.of("deck1 contents: 10 13 16 19"), read(folder, "deck1_output.txt"));
            assertEquals(run, List.of("deck2 contents: 11 14 17 20"), read(folder, "deck2_output.txt"));
            assertEquals(run, List.of("deck3 contents: 12 15 18 21"), read(folder, "deck3_output.txt"));
        }
    }

    // display every hands. only 4 4s can win
    @Test(timeout = 60_000)
    public void displayTest() throws Exception {
        List<Integer> values = new ArrayList<>(List.of(4, 4, 4, 4));
        for (int value = 100; values.size() < 32; value++) {
            values.add(value);
        }
        Collections.shuffle(values, new Random(7));
                long looks = 0;
        // a game can very rarely finish before we get a look, so play again (up to 20 games) until we do
        for (int game_no = 1; game_no <= 20 && looks == 0; game_no++) {
            CardGame game = new CardGame(4, TestSupport.pack(4, values), temp.newFolder("game" + game_no).toPath(),
                    TestSupport.quietConsole());

            Future<Integer> result = background.submit(game::play);
            while (!result.isDone()) {
                for (Player player : game.getPlayers()) {
                    assertEquals("player " + player.getNum() + "'s hand",
                            Player.handSize, player.getHand().size());
                    looks++;
                }
            }
            int winner = result.get();
            assertTrue(winner >= 1 && winner <= 4);
        }
        assertTrue("the hands were looked at during the game", looks > 0);
    }
    //test if the game would stop if the output files could not be created
    @Test(timeout = 10_000)
    public void outputTest() throws Exception {
        Path noSuchFolder = temp.getRoot().toPath().resolve("no-such-folder");
        CardGame game = new CardGame(1, TestSupport.pack(1, TestSupport.onePlayerPackValues()),
                noSuchFolder, TestSupport.quietConsole());

        assertThrows(IOException.class, game::play);
        assertTrue("no cards were dealt", game.getPlayers().isEmpty());
    }
    //test if a game can only be played once
    @Test(timeout = 10_000)
    public void sameGameTest() throws Exception {
        CardGame game = new CardGame(1, TestSupport.pack(1, TestSupport.onePlayerPackValues()),
                temp.getRoot().toPath(), TestSupport.quietConsole());
        game.play();
        assertThrows(IllegalStateException.class, game::play);
    }
    //giving invalid playerNum
    @Test
    public void invalidPlayerNumTest() throws Exception {
        TestSupport.RecordingConsole out = new TestSupport.RecordingConsole();

        int players = CardGame.askForPlayerNum(
                TestSupport.input("four", "0", "-2", "2.5", "", " 3 "), out.stream);

        assertEquals(3, players);
        assertEquals(6, TestSupport.count(out.lines(), "Please enter the number of players:"));
        assertEquals(5, TestSupport.count(out.lines(), "is not a valid number"));
    }
    //giving invalid pack
    @Test
    public void invalidPackTest() throws Exception {
        Path folder = temp.getRoot().toPath();
        List<Integer> good = TestSupport.winnableValues(2, 1);
        Path tooShort = TestSupport.writePackFile(folder, "short.txt", good.subList(0, 15));
        List<Integer> withNegative = new ArrayList<>(good);
        withNegative.set(7, -4);
        Path negative = TestSupport.writePackFile(folder, "negative.txt", withNegative);
        Path valid = TestSupport.writePackFile(folder, "valid.txt", good);
        TestSupport.RecordingConsole out = new TestSupport.RecordingConsole();

        Pack pack = CardGame.askForPack(TestSupport.input(
                folder.resolve("missing.txt").toString(), tooShort.toString(),
                negative.toString(), valid.toString()), out.stream, 2);

        assertEquals(good, TestSupport.values(pack.getCards()));
        assertEquals(4, TestSupport.count(out.lines(), "Please enter location of pack to load:"));
        assertEquals(3, TestSupport.count(out.lines(), "this is not a valid pack"));
    }
    //early input ends
    @Test(timeout = 10_000)
    public void inputEndTest() throws Exception {
        Path outputFolder = temp.newFolder("output").toPath();
        TestSupport.RecordingConsole out = new TestSupport.RecordingConsole();

        int winner = CardGame.runInteractive(
                TestSupport.input("2", outputFolder.resolve("missing.txt").toString()), out.stream, outputFolder);

        assertEquals(GameState.noWinner, winner);
        assertEquals(0, outputFileCount(outputFolder));
        assertEquals(1, TestSupport.count(out.lines(), "Input ended"));
    }
    //game simulation with error input
    @Test(timeout = 30_000)
    public void wrongInputTest() throws Exception {
        Path packFolder = temp.newFolder("packs").toPath();
        Path outputFolder = temp.newFolder("output").toPath();
        Path pack = TestSupport.writePackFile(packFolder, "one.txt", TestSupport.onePlayerPackValues());
        TestSupport.RecordingConsole out = new TestSupport.RecordingConsole();

        int winner = CardGame.runInteractive(
                TestSupport.input("zero", "1", "nope.txt", pack.toString()), out.stream, outputFolder);

        assertEquals(1, winner);
        assertEquals(List.of(
                "Please enter the number of players:",
                "Invalid number entered: 'zero' is not a valid number",
                "Please enter the number of players:",
                "Please enter location of pack to load:",
                "this is not a valid pack: There is nothing at 'nope.txt'.",
                "Please enter location of pack to load:",
                "player 1 wins"), out.lines());
        assertEquals(2, outputFileCount(outputFolder));
    }
}
