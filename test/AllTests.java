import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    CardTest.class,
    PackTest.class,
    CardDeckTest.class,
    PlayerTest.class,
    GameStateTest.class,
    CardDeckTest.class
})
public class AllTests {
}