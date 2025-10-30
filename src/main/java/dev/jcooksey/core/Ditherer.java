package dev.jcooksey.core;

import java.awt.image.BufferedImage;
import java.util.Map;

public class Ditherer
{
    public BufferedImage dither(Map<String, BufferedImage> images)
    {
        BufferedImage ditheredImage = images.get("inputImage");
        return ditheredImage;
    }
}
