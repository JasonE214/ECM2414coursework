import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.Test;

public class CardTest {

    //see if it actuall stores the value
    @Test
    public void keep_value() {
        assertEquals(7, new Card(7).getValue());
    }

    // see if it works on values >= 0
    @Test
    public void works_on_positives() {
        assertEquals(0, new Card(0).getValue());
        assertEquals(1_000_000, new Card(1_000_000).getValue());
    }

    //does it get rid of negatives
    @Test
    public void reject_negative() {
        assertThrows(IllegalArgumentException.class, () -> new Card(-1));
    }

    // Card's thread safety rests on immutability, so check it can't quietly be lost.
    @Test
    public void is_immutable() {
        assertTrue("the class is final", Modifier.isFinal(Card.class.getModifiers()));
        for (Field field : Card.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                assertTrue(field.getName() + " is final", Modifier.isFinal(field.getModifiers()));
            }
        }
    }

    // Does 2 cards with same value works
    @Test
    public void same_value_diff_card() {
        assertNotEquals(new Card(3), new Card(3));
    }
}
