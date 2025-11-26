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

    public BigColor(int rgb)
    {
        int value = 0xff000000 | rgb;
        red = (value >> 16) & 0xFF;
        green = (value >> 8) & 0xFF;
        blue = (value >> 0) & 0xFF;
    }

    public void addColor(Color color)
    {
        setRed(color.getRed() + this.red);
        setGreen(color.getGreen() + this.green);
        setBlue(color.getBlue() + this.blue);
    }

    public void setColor(BigColor color)
    {
        setRed(color.getRed());
        setGreen(color.getGreen());
        setBlue(color.getBlue());
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
        int redDistance = Math.abs(this.red - color.getRed());
        int greenDistance = Math.abs(this.green - color.getGreen());
        int blueDistance = Math.abs(this.blue - color.getBlue());

        return Math.sqrt(Math.pow(redDistance, 2) + Math.pow(greenDistance, 2) + Math.pow(blueDistance, 2));
    }

    public void clampColor()
    {
        // clamping for better dithering appearance
        if (this.red > 255)
        {
            this.red = 255 + ((this.red - 255) / 2);
        }
        else if (this.red < 0)
        {
            this.red = this.red / 2;
        }

        if (this.green > 255)
        {
            this.green = 255 + ((this.green - 255) / 2);
        }
        else if (this.green < 0)
        {
            this.green = this.green / 2;
        }

        if (this.blue > 255)
        {
            this.blue = 255 + ((this.blue - 255) / 2);
        }
        else if (this.blue < 0)
        {
            this.blue = this.blue / 2;
        }
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
