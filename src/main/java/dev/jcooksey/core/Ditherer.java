package dev.jcooksey.core;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            // starts at one because we always need to remove the highest-order curve
            int tiersToClear = 1;
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

            tierIndex = 1;
            ArrayList<Integer> tier;
            while (true)
            {
                tier = tiers.reversed().get(tierIndex);
                int tierStep = tierIndices.reversed().get(tierIndex);

                if (tierStep == 3)
                {
                    tiersToClear += 1;
                    tierIndex += 1;
                    if (tiersToClear >= tiers.size())
                    {
                        return outputImage;
                    }
                    continue;
                }
                /*tierIndices.reversed().set(tierIndex, tierStep + 1);
                if (tierStep == 2)
                {
                    tiersToClear += 1;
                }*/
                int[] updatedCoords = drawHilbertPixel(tier.get(tierStep), x, y, outputImage, pixels, inputImage, totalErrors);
                x = updatedCoords[0];
                y = updatedCoords[1];
                break;
            }
            step += 1;

            /for (int i = 0; i < tiersToClear; i++)
            {
                tierIndices.removeLast();
                tiers.removeLast();
                try
                {
                    rotationsPerTier.removeLast();
                } catch (Exception e) {}
            }
        }

        return outputImage;
    }

    private int[] drawHilbertPixel(int direction, int x, int y, BufferedImage outputImage, int[] pixels, BufferedImage inputImage, BigColor totalErrors)
    {
        switch (direction)
        {
            case 0:
                y -= 1;
                System.out.println("up");
                break;

            case 2:
                y += 1;
                System.out.println("down");
                break;

            case 3:
                x -= 1;
                System.out.println("left");
                break;

            case 1:
                x += 1;
                System.out.println("right");
                break;
        }

        if (x >= 0 && x < outputImage.getWidth() && y >= 0 && y < outputImage.getHeight())
        {
            BigColor targetColor = new BigColor(pixels[y * inputImage.getWidth() + x]);
            targetColor.addError(totalErrors);
            Color ditherColor = getNearestColor(targetColor);

            outputImage.setRGB(x, y, ditherColor.getRGB());

            totalErrors = targetColor;
            totalErrors.removeColor(ditherColor);
        }
        return new int[]{x, y};
    }

    private void processNewCurves(ArrayList<ArrayList<Integer>> tiers, ArrayList<Integer> tierIndices, ArrayList<Integer> rotationsPerTier, int maxOrder, int step)
    {
        int hilbertOrder = rotationsPerTier.size();
        int rotationalDepth = 0;
        for (int rotation:  rotationsPerTier)
        {
            if (rotation == -1 || rotation == 1)
            {
                rotationalDepth += 1;
            }
        }

        // we always need to add at least one rotation because each four steps there's a new rotation
        if (rotationalDepth % 2 == 0)
        {
            rotationsPerTier.add(secondOrderRotations.get(tierIndices.getLast()));
        } else
        {
            rotationsPerTier.add(secondOrderRotations.get(3 - tierIndices.getLast()));
        }

        int startingOrder = hilbertOrder;

        while (hilbertOrder < maxOrder)
        {
            int curveStep;
            int divisor = (1 << (2 * (maxOrder - hilbertOrder)));
            int quotient = step / divisor;
            curveStep = quotient % 4;

            // we get the curve from the last tier because we then only need to apply one rotation at most to get the real tier curve
            ArrayList<Integer> currentTierCurve = new ArrayList<>(tiers.getLast());
            int nextRotation = rotationsPerTier.getLast();
            if (nextRotation == 1 || nextRotation == -1)
            {
                rotateDirections90AndReverse(currentTierCurve, nextRotation);
                rotationalDepth += 1;
            }

            // because rotationsPerTier will have tiers.size() - 1 elements and each rotation is to go from one order to the next
            // that means that we don't need the last rotation to be added because we don't need to rotate the final curve into yet another higher-order curve
            if (hilbertOrder < maxOrder - 1)
            {
                if (rotationalDepth % 2 == 0)
                {
                    rotationsPerTier.add(secondOrderRotations.get(curveStep));
                } else
                {
                    rotationsPerTier.add(secondOrderRotations.get(3 - curveStep));
                }
            }

            tiers.add(currentTierCurve);

            // because we update the tier index for tiers that we don't remove, we only need to add an updated tier index if the tier we're on doesn't already have one saved
            if (hilbertOrder != startingOrder)
            {
                tierIndices.add(curveStep);
            }

            hilbertOrder += 1;
        }
        // because the improved logic draws pixels in groups of four, when we're done processing the last tier, it is always on the first index (0) of that highest-order curve
        tierIndices.add(0);
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
