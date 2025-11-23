package dev.jcooksey.core;

public enum Direction {
    UP(0),
    RIGHT(1),
    DOWN(2),
    LEFT(3);

    Direction(int i)
    {
    }

    public static Direction from(int i)
    {
        for (Direction direction : Direction.values())
        {
            if (direction.ordinal() == i)
            {
                return direction;
            }
        }
        throw new IllegalArgumentException();
    }
}
