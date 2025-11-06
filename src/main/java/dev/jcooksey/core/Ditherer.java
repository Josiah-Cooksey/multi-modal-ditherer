package dev.jcooksey.core;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class Ditherer
{

    public BufferedImage dither(Map<String, BufferedImage> images)
    {
        ArrayList<Color> paletteColors = getImageColors(images.get("palette"));

        BufferedImage ditheredImage = simpleDither(images.get("inputImage"), paletteColors);

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
    public BufferedImage simpleDither(BufferedImage inputImage, ArrayList<Color> paletteColors)
    {
        BufferedImage outputImage = new BufferedImage(inputImage.getWidth(), inputImage.getHeight(), inputImage.getType());

        BigColor totalErrors = new BigColor(0, 0, 0);
        for (int x = 0; x < inputImage.getWidth(); x++)
        {
            for (int y = 0; y < inputImage.getHeight(); y++)
            {
                Color originalPixelColor = new Color(inputImage.getRGB(x, y));
                BigColor targetColor = new BigColor(originalPixelColor);
                targetColor.addError(totalErrors);
                Color ditherColor = getNearestColor(paletteColors, targetColor);

                outputImage.setRGB(x, y, ditherColor.getRGB());

                totalErrors = targetColor;
                totalErrors.removeColor(ditherColor);
            }
        }

        return outputImage;
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

    private Color getNearestColor(ArrayList<Color> availableColors, BigColor targetColor)
    {

        double leastDistance = Double.MAX_VALUE;
        Color leastColor = null;
        for (Color color : availableColors)
        {
            double newDistance = targetColor.getColorDistance(color);
            if (newDistance >= leastDistance)
            {
                continue;
            }
            leastDistance = newDistance;
            leastColor = color;
        }
        return leastColor;
    }
}
