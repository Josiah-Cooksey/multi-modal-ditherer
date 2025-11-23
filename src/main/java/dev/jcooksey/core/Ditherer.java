package dev.jcooksey.core;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Ditherer
{
    public static ArrayList<Color> palette;

    public void setPalette(BufferedImage image)
    {
        palette = getImageColors(image);
    }

    public void setPalette(ArrayList<Color> colors)
    {
        palette = colors;
    }

    public BufferedImage dither(Map<String, BufferedImage> images)
    {
        setPalette(images.get("palette"));

        BufferedImage ditheredImage = simpleDither(images.get("inputImage"));

        /*try
        {
            ImageIO.write(images.get("palette"), "png", new File("palette.png"));
            ImageIO.write(images.get("kernel"), "png", new File("kernel.png"));
            ImageIO.write(images.get("inputImage"), "png", new File("inputImage.png"));
            ImageIO.write(ditheredImage, "png", new File("output.png"));
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }*/
        return ditheredImage;
    }

    // picks the closest color and propagates the error to the next pixel
    public BufferedImage simpleDither(BufferedImage inputImage)
    {
        BufferedImage outputImage = new BufferedImage(inputImage.getWidth(), inputImage.getHeight(), inputImage.getType());
        DataBufferInt dataBuffer = (DataBufferInt) inputImage.getRaster().getDataBuffer();
        int[] pixels = dataBuffer.getData();
        BigColor totalErrors = new BigColor(0, 0, 0);
        for (int x = 0; x < inputImage.getWidth(); x++)
        {
            for (int y = 0; y < inputImage.getHeight(); y++)
            {
                BigColor targetColor = new BigColor(pixels[y * inputImage.getWidth() + x]);
                targetColor.addError(totalErrors);
                Color ditherColor = getNearestColor(targetColor);

                outputImage.setRGB(x, y, ditherColor.getRGB());

                totalErrors = targetColor;
                totalErrors.removeColor(ditherColor);
            }
        }

        return outputImage;
    }

    // simpleDither, but follows a hilbert curve instead of left-to-right, top-to-bottom pathing
    public BufferedImage hilbertDither(BufferedImage inputImage)
    {
        BufferedImage outputImage = new BufferedImage(inputImage.getWidth(), inputImage.getHeight(), inputImage.getType());
        DataBufferInt dataBuffer = (DataBufferInt) inputImage.getRaster().getDataBuffer();
        int[] pixels = dataBuffer.getData();
        BigColor totalErrors = new BigColor(0, 0, 0);

        ArrayList<Direction> firstOrderCurve = new ArrayList<>(List.of(Direction.DOWN, Direction.RIGHT, Direction.UP));
        ArrayList<Rotation> secondOrderRotations = new ArrayList<>(List.of(Rotation.LEFT, Rotation.UP, Rotation.UP, Rotation.RIGHT));

        int x = 0;
        int y = 0;
        int hilbertLength = 2;
        int maxOrder = 1;
        int maxSideLength = Math.max(inputImage.getWidth(), inputImage.getHeight());
        // I want the area to encompass the whole image, even if it ends up a bit too large
        while (hilbertLength < maxSideLength)
        {
            hilbertLength *= 2;
            maxOrder += 1;
        }
        int stepLimit = hilbertLength * hilbertLength;

        int step = 0;

        while (step < stepLimit)
        {
            if (x >= 0 && x < outputImage.getWidth() && y >= 0 && y < outputImage.getHeight())
            {
                BigColor targetColor = new BigColor(pixels[y * inputImage.getWidth() + x]);
                targetColor.addError(totalErrors);
                Color ditherColor = getNearestColor(targetColor);

                outputImage.setRGB(x, y, ditherColor.getRGB());

                totalErrors = targetColor;
                totalErrors.removeColor(ditherColor);

                // outputImage.setRGB(x, y, step);
            }

            ArrayList<ArrayList<Direction>> tiers = new ArrayList<>();
            ArrayList<Integer> tierIndices = new ArrayList<>();
            ArrayList<Rotation> rotationsPerTier = new ArrayList<>();
            int tierDivisor = 1;
            int hilbertOrder = 0;

            while (tierDivisor < hilbertLength)
            {
                // TODO: can probably cut the loop short if curveStep is ever equal to 3 because it means that we don't need to calculate any lower orders inside the current order
                // TODO: or we could cache results instead of recalculating them every step
                int curveStep;
                if (hilbertOrder == maxOrder)
                {
                    curveStep = step % 4;
                }
                else
                {
                    // curveStep = Math.abs(Math.floor((((step) / (Math.pow(4, (maxOrder - hilbertOrder))))) - (4 * hilbertOrder)) % 4);
                    // broken curveStep = Math.abs(Math.floor((step / (1 << (2 * (maxOrder - hilbertOrder))) - (4 * hilbertOrder))) % 4);


                    // curveStep = (((step / (Math.pow(4, (maxOrder - hilbertOrder))))) - (4 * hilbertOrder)) % 4;
                    int divisor = (1 << (2 * (maxOrder - hilbertOrder))); // 4 ^ (maxOrder - hilbertOrder)
                    int quotient = step / divisor;
                    curveStep = quotient % 4;
                }

                ArrayList<Direction> currentTierCurve = firstOrderCurve;
                int rotational_depth = 0;

                for(Rotation rotation : rotationsPerTier)
                {
                    if (rotation == Rotation.LEFT || rotation == Rotation.RIGHT)
                    {
                        rotational_depth += 1;
                    }
                    currentTierCurve = rotateDirections90AndReverse(currentTierCurve, rotation);
                }

                tiers.add(currentTierCurve);
                tierIndices.add(curveStep);

                if (rotational_depth % 2 == 0)
                {
                    rotationsPerTier.add(secondOrderRotations.get((int) curveStep));
                }
                else
                {
                    rotationsPerTier.add(secondOrderRotations.get((3 - ((int) curveStep))));
                }

                tierDivisor *= 2;
                hilbertOrder += 1;
            }

            int tierIndex = 0;
            Direction nextDirection = null;
            for (ArrayList<Direction> tier : tiers.reversed())
            {
                int tierStep = (int) ((((step + 1) / (Math.pow(4, tierIndex)))) - 1) % 4;


                if (tierStep == 3)
                {
                    tierIndex += 1;
                    continue;
                }

                nextDirection = tier.get(tierStep);
                break;
            }

            if (nextDirection == null)
            {
                System.out.println("The next direction could not be determined on step " + step);
                return outputImage;
            }

            switch (nextDirection)
            {
                case Direction.UP:
                    y -= 1;
                    break;

                case Direction.DOWN:
                    y += 1;
                    break;

                case Direction.LEFT:
                    x -= 1;
                    break;

                case Direction.RIGHT:
                    x += 1;
                    break;
            }

            step += 1;
        }

        return outputImage;
    }

    private ArrayList<Direction> rotateDirections90AndReverse(ArrayList<Direction> originalDirections, Rotation rotation)
    {
        if (rotation == Rotation.UP)
        {
            return originalDirections;
        }

        ArrayList<Direction> newDirections = new ArrayList<>();
        for (Direction direction : originalDirections.reversed())
        {
            newDirections.add(oppositeDirection(rotateDirection90(direction, rotation)));
        }

        return newDirections;
    }

    private Direction oppositeDirection(Direction direction)
    {
        return switch (direction)
        {
            case Direction.UP -> Direction.DOWN;
            case Direction.DOWN -> Direction.UP;
            case Direction.LEFT -> Direction.RIGHT;
            case Direction.RIGHT -> Direction.LEFT;
        };
    }

    // positive rotations are clockwise
    // negative rotations are counterclockwise
    // zero rotation returns the same direction
    private Direction rotateDirection90(Direction original_direction, Rotation rotation)
    {
        return Direction.from((4 + (original_direction.ordinal() + rotation.ordinal())) % 4);
    }

    private ArrayList<Color> getImageColors(BufferedImage image)
    {
        ArrayList<Color> colors = new ArrayList<>();

        for (int x = 0; x < image.getWidth(); x++)
        {
            for (int y = 0; y < image.getHeight(); y++)
            {
                Color color = new Color(image.getRGB(x, y));
                if (colors.contains(color))
                {
                    continue;
                }
                colors.add(color);
            }
        }

        return colors;
    }

    private Color getNearestColor(BigColor targetColor)
    {

        double leastDistance = Double.MAX_VALUE;
        Color leastColor = null;
        for (Color color : palette)
        {
            double newDistance = targetColor.getColorDistance(color);
            if (newDistance < leastDistance)
            {
                leastDistance = newDistance;
                leastColor = color;
            }
        }
        return leastColor;
    }
}
