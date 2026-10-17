/* Class for card that contains it's fix value */

public final class Card{
    private final int value;

    public Card(int value) throws IllegalArgumentException
    {   
        // value of card can only be greater than -1
        if (value < 0){
            throw new IllegalArgumentException(
                    "A card value " + value + " is a negative integer.");
        }
        else this.value = value;
    }

     public int getValue() {
        return value;
     }

}