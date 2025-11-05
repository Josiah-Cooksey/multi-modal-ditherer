package dev.jcooksey.core;

import java.awt.*;

public class BigColor
{
    private int red;
    private int green;
    private int blue;

    public BigColor(int r, int g, int b)
    {
        red = r;
        green = g;
        blue = b;
    }

    public BigColor(Color color)
    {
        red = color.getRed();
        green = color.getGreen();
        blue = color.getBlue();
    }

    public void addColor(Color color)
    {
        setRed(color.getRed() + this.red);
        setGreen(color.getGreen() + this.green);
        setBlue(color.getBlue() + this.blue);
    }

    public int getRed()
    {
        return red;
    }
    public void setRed(int red)
    {
        this.red = red;
    }

    public int getGreen()
    {
        return green;
    }
    public void setGreen(int green)
    {
        this.green = green;
    }

    public int getBlue()
    {
        return blue;
    }
    public void setBlue(int blue)
    {
        this.blue = blue;
    }

    // at least for simple error-diffusion dithering, removeColor can calculate a new error
    public void removeColor(Color selectedColor)
    {
        this.red -= selectedColor.getRed();
        this.green -= selectedColor.getGreen();
        this.blue -= selectedColor.getBlue();
    }

    public double getColorDistance(Color color)
    {
        double redDistance = (double) (Math.abs(this.red - color.getRed()));
        double greenDistance = (double) (Math.abs(this.green - color.getGreen()));
        double blueDistance = (double) (Math.abs(this.blue - color.getBlue()));

        double firstHypotenuse = Math.sqrt(Math.pow(redDistance, 2) + Math.pow(greenDistance, 2));
        double resultDistance = Math.sqrt(Math.pow(firstHypotenuse, 2) + Math.pow(blueDistance, 2));
        return resultDistance;
    }

    public Color getBoundedColor()
    {
        return new Color(Math.max(Math.min(this.red, 255), 0),  Math.max(Math.min(this.green, 255), 0), Math.max(Math.min(this.blue, 255), 0));
    }

    public void addError(BigColor totalErrors)
    {
        this.red += totalErrors.getRed();
        this.green += totalErrors.getGreen();
        this.blue += totalErrors.getBlue();
    }
}
