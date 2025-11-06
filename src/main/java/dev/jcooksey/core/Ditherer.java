package dev.jcooksey.core;

import javax.imageio.ImageIO;
import javax.xml.crypto.Data;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
