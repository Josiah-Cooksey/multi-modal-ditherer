package dev.jcooksey.core;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class Ditherer
{
    private static final ArrayList<Integer> secondOrderRotations = new ArrayList<>(List.of(-1, 0, 0, 1));

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

    // TODO: use an inverted index for ArrayLists instead of .reversed to improve performance
    // simpleDither, but follows a hilbert curve instead of left-to-right, top-to-bottom pathing
    public BufferedImage hilbertDither(BufferedImage inputImage)
    {
        BufferedImage outputImage = new BufferedImage(inputImage.getWidth(), inputImage.getHeight(), inputImage.getType());
        DataBufferInt dataBuffer = (DataBufferInt) inputImage.getRaster().getDataBuffer();
        int[] pixels = dataBuffer.getData();
        BigColor totalErrors = new BigColor(0, 0, 0);

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
        maxOrder -= 1;
        int stepLimit = hilbertLength * hilbertLength;

        int step = 0;

        ArrayList<ArrayList<Integer>> tiers = new ArrayList<>(List.of(new ArrayList<Integer>(List.of(2, 1, 0))));
        ArrayList<Integer> tierIndices = new ArrayList<>(List.of(0));
        ArrayList<Integer> rotationsPerTier = new ArrayList<>();

        while (step < stepLimit)
        {
            processNewCurves(tiers, tierIndices, rotationsPerTier, maxOrder, step);

            // we always draw 4 pixels at a time — 3 from the deepest (highest-order) curve and one from the next-deepest curve that isn't on step 3

            int tierIndex = 0;
            for (int tierStep = 0; tierStep < 3; tierStep++)
            {
                int[] updatedCoords = drawHilbertPixel(tiers.getLast().get(tierStep), x, y, outputImage, pixels, inputImage, totalErrors);
                x = updatedCoords[0];
                y = updatedCoords[1];
                // because we're progressing through multiple precalculated steps in this loop, we have to manually increment tierSteps
                // otherwise they perpetually remain at 0
                tierIndices.reversed().set(tierIndex, tierStep + 1);
                step += 1;
            }
            // after those 3 prior steps, we can always safely remove that highest-order curve data
            tiers.removeLast();
            tierIndices.removeLast();
            rotationsPerTier.removeLast();

            tierIndex = 1;
            ArrayList<Integer> tier;
            int completedTiers = 1;
            while (!tiers.isEmpty())
            {
                int tierStep = tierIndices.getLast();

                if (tierStep == 3)
                {
                    completedTiers += 1;
                    tierIndex += 1;

                    tiers.removeLast();
                    tierIndices.removeLast();
                    try
                    {
                        rotationsPerTier.removeLast();
                    } catch (Exception e) {}

                    if (tiers.isEmpty())
                    {
                        return outputImage;
                    }
                    continue;
                }
                int[] updatedCoords = drawHilbertPixel(tiers.getLast().get(tierStep), x, y, outputImage, pixels, inputImage, totalErrors);
                x = updatedCoords[0];
                y = updatedCoords[1];

                // we need to keep track of tier progress because we may not remove it this loop iteration
                tierStep += 1;
                tierIndices.set(tierIndices.size() - 1, tierStep);

                /*if (tierStep == 3)
                {
                    tiers.removeLast();
                    tierIndices.removeLast();
                    try
                    {
                        rotationsPerTier.removeLast();
                    } catch (Exception e) {}

                    if (tiers.isEmpty())
                    {
                        return outputImage;
                    }
                }*/
                break;
            }
            step += 1;
        }

        return outputImage;
    }

    private int[] drawHilbertPixel(int direction, int x, int y, BufferedImage outputImage, int[] pixels, BufferedImage inputImage, BigColor totalErrors)
    {
        switch (direction)
        {
            case 0:
                y -= 1;
                break;

            case 2:
                y += 1;
                break;

            case 3:
                x -= 1;
                break;

            case 1:
                x += 1;
                break;
        }

        if (x >= 0 && x < outputImage.getWidth() && y >= 0 && y < outputImage.getHeight())
        {
            BigColor targetColor = new BigColor(pixels[y * inputImage.getWidth() + x]);
            targetColor.addError(totalErrors);
            Color ditherColor = getNearestColor(targetColor);

            outputImage.setRGB(x, y, ditherColor.getRGB());

            totalErrors.setColor(targetColor);
            totalErrors.removeColor(ditherColor);
        }
        return new int[]{x, y};
    }

    private void processNewCurves(ArrayList<ArrayList<Integer>> tiers, ArrayList<Integer> tierIndices, ArrayList<Integer> rotationsPerTier, int maxOrder, int step)
    {
        // the problem is that if this starts on tiers.size() - 1, then when the first order curve is on step index 1, it duplicates the rotations for some reason, so we end up with two up "rotations" instead of just one
        int hilbertOrder = tiers.size() - 1;
        int rotationalDepth = 0;

        for (int rotation:  rotationsPerTier)
        {
            if (rotation == -1 || rotation == 1)
            {
                rotationalDepth += 1;
            }
        }

        int startingOrder = hilbertOrder;

        int indexGenerationCounter = hilbertOrder + 1;
        while (tierIndices.size() < maxOrder)
        {
            int curveStep;
            int divisor = (1 << (2 * (maxOrder - indexGenerationCounter)));
            int quotient = step / divisor;
            curveStep = quotient % 4;

            // because we update the tier index for tiers that we don't remove, we only need to add an updated tier index if the tier we're on doesn't already have one saved
            if (indexGenerationCounter != maxOrder)
            {
                tierIndices.add(curveStep);
            }
            indexGenerationCounter += 1;
        }
        tierIndices.add(0);

        int rotationGenerationCounter = hilbertOrder;
        while (rotationsPerTier.size() < maxOrder)
        {
            if (rotationalDepth % 2 == 0)
            {
                rotationsPerTier.add(secondOrderRotations.get(tierIndices.get(rotationGenerationCounter)));
            }
            else
            {
                rotationsPerTier.add(secondOrderRotations.get(3 - tierIndices.get(rotationGenerationCounter)));
            }

            if (rotationsPerTier.getLast() == -1 || rotationsPerTier.getLast() == 1)
            {
                rotationalDepth += 1;
            }
            rotationGenerationCounter += 1;
        }

        while (hilbertOrder < maxOrder)
        {
            // we copy the curve from the last tier because we then only need to apply one rotation at most to get the real tier curve
            ArrayList<Integer> currentTierCurve = new ArrayList<>(tiers.getLast());

            try
            {
                int nextRotation = rotationsPerTier.get(hilbertOrder);
                if (nextRotation == 1 || nextRotation == -1)
                {
                    rotateDirections90AndReverse(currentTierCurve, nextRotation);
                    rotationalDepth += 1;
                }
            } catch (NoSuchElementException ignored) { }
            tiers.add(currentTierCurve);

            hilbertOrder += 1;
        }
        return;
    }

    private void rotateDirections90AndReverse(ArrayList<Integer> directions, int rotation)
    {
        if (rotation == 0)
        {
            return;
        }
        int a = directions.get(0);
        int b = directions.get(1);
        int c = directions.get(2);

        directions.set(0, oppositeDirection(rotateDirection90(c, rotation)));
        directions.set(1, oppositeDirection(rotateDirection90(b, rotation)));
        directions.set(2, oppositeDirection(rotateDirection90(a, rotation)));
    }

    private int oppositeDirection(int direction)
    {
        return (direction + 2) % 4;
    }

    // positive rotations are clockwise
    // negative rotations are counterclockwise
    // zero rotation returns the same direction
    private int rotateDirection90(int original_direction, int rotation)
    {
        return (4 + (original_direction + rotation)) % 4;
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
